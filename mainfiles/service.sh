#!/system/bin/sh

#
# Copyright (C) 2026-2027 Zexshia
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

readonly MODDIR="${0%/*}"
readonly MODULE_CONFIG="/data/adb/.config/MaxManager"
readonly BIN_SVC="$MODDIR/system/bin/sys.maxmanager-service"
readonly APK_COMP="$MODDIR/app/MaxManager.apk"

# shellcheck source=props.sh
. "$MODDIR/props.sh"

# Wait boot to complete
until [ "$(getprop sys.boot_completed)" = "1" ]; do 
    sleep 1
done

# Remove Single Instance since we dont need it anymore
rm -f /dev/.maxmanagerSingleInstance

# Reset anti bootloop
echo "BOOTCOUNT=0" > "$MODDIR/count.sh"

# Clear Old Logs
"$BIN_SVC" --clearlogs

readonly LAUNCHER_STATE="$MODULE_CONFIG/launcher_visibility"
readonly LEGACY_LAUNCHER_MARKER="$MODULE_CONFIG/.launcher_enabled"
readonly RECOVERY_LOG="$MODULE_CONFIG/package-recovery.log"

log_recovery() {
    printf '%s %s\n' "$(date '+%Y-%m-%dT%H:%M:%S%z')" "$*" >> "$RECOVERY_LOG"
}

package_installed() {
    pm path nd.max >/dev/null 2>&1
}

# versionCode the installed package reports (`dumpsys` prints `versionCode=<n> minSdk=...`).
installed_code() {
    dumpsys package nd.max 2>/dev/null | sed -n 's/.*versionCode=\([0-9][0-9]*\).*/\1/p' | head -n 1
}

# versionCode of this module build (CI writes it from the commit count; the app carries the same number).
module_code() {
    sed -n 's/^versionCode=\([0-9][0-9]*\).*/\1/p' "$MODDIR/module.prop" 2>/dev/null | head -n 1
}

# Installs the module's APK as a user app for user 0. `pm install -r` refuses a downgrade
# without `-d`, so an already newer install is never replaced by an older build.
install_user_app() {
    if [ ! -f "$APK_COMP" ]; then
        log_recovery "module APK missing: $APK_COMP"
        return 1
    fi
    cp "$APK_COMP" /data/local/tmp/MaxManager.apk || return 1
    chmod 644 /data/local/tmp/MaxManager.apk
    out=$(pm install -r --user 0 /data/local/tmp/MaxManager.apk 2>&1)
    rm -f /data/local/tmp/MaxManager.apk
    printf '%s\n' "$out" >> "$RECOVERY_LOG"
    case "$out" in
        *Success*) return 0 ;;
        *) return 1 ;;
    esac
}

# Makes the user-data copy match this module: missing or older -> install; equal or newer -> keep.
ensure_user_app() {
    have=$(installed_code)
    want=$(module_code)
    if [ -n "$have" ] && { [ -z "$want" ] || [ "$have" -ge "$want" ]; }; then
        log_recovery "user app current: installed=$have module=${want:-unknown}"
        return 0
    fi
    log_recovery "installing user app: installed=${have:-none} module=${want:-unknown}"
    install_user_app
}

# PackageManager must answer before we install or resolve anything (it may still be starting).
attempt=0
until pm path android >/dev/null 2>&1 || [ "$attempt" -ge 30 ]; do
    attempt=$((attempt + 1))
    sleep 2
done

PACKAGE_READY=0
attempt=0
while [ "$attempt" -lt 3 ]; do
    ensure_user_app
    if package_installed; then
        PACKAGE_READY=1
        break
    fi
    attempt=$((attempt + 1))
    sleep 3
done

if [ "$PACKAGE_READY" -eq 1 ]; then
    log_recovery "package available path=$(pm path nd.max 2>/dev/null | tr '\n' ' ')"

    # A user app cannot hold WRITE_SECURE_SETTINGS; runtime grants and appops still apply.
    pm grant nd.max android.permission.POST_NOTIFICATIONS >/dev/null 2>&1
    pm grant nd.max android.permission.READ_EXTERNAL_STORAGE >/dev/null 2>&1
    pm grant nd.max android.permission.READ_MEDIA_IMAGES >/dev/null 2>&1
    appops set nd.max SYSTEM_ALERT_WINDOW allow >/dev/null 2>&1
    appops set nd.max WRITE_SETTINGS allow >/dev/null 2>&1

    # الأيقونة تبقى ظاهرة دائمًا — بلا حالة محفوظة تُقلَب.
    #
    # وهذا ليس تفضيلًا: التطبيق هو الطريق الوحيد إلى مدير الروت بعد التفليش (منح الإذن يتمّ من
    # داخل التطبيق أو من قائمة مدير الروت)، وإخفاؤه من المشغّل كان يقطع الطريق على من أراد منحه
    # الإذن ثم يقول «تطبيقي لا يظهر». وأي إخفاء سابق محفوظ في `PackageManager` يُلغى هنا عند كل
    # إقلاع، لأن حالة المكوّن المحفوظة تسبق قيمة البيان الافتراضية.
    if pm enable --user 0 nd.max/.Launcher >/dev/null 2>&1; then
        printf '%s\n' shown > "$LAUNCHER_STATE"
        chmod 600 "$LAUNCHER_STATE" 2>/dev/null
        rm -f "$LEGACY_LAUNCHER_MARKER"
    else
        log_recovery "launcher enable failed (component state may still be hidden)"
    fi

    log_recovery "package state: user_app=yes (installed from the module app/ folder, listed with normal apps)"
else
    log_recovery "package recovery failed; app companion not started"
fi

# Remove reboot flag
if [ -f "$MODDIR/reboot" ]; then
    rm -f "$MODDIR/reboot"
fi

# Refresh MaxManager daemon state
STATE=$(getprop "$PROP_STATE")
{ [ -z "$STATE" ] || { [ "$STATE" = "running" ] && [ -z "$(/system/bin/toybox pidof sys.maxmanager-service)" ]; }; } && {
    setprop "$PROP_STATE" stopped
    setprop "$PROP_SERVICE" ""
}

readonly COMPANION_NAME="sys.maxmanager-appmonitoring"

launch_companion() {
    # والمخرَج **يُضاف** لا يُقتطع: كان `>` يمحو ما كتبه رفيق سابق في الإقلاع نفسه —
    # والحالة التي دفعت الثمن هي التي تُشفى: موت الرفيق في الإقلاع يُتبع بإعادة تشغيل
    # (من الحارس في التطبيق، أو من شاشة فحص الشحن) — فلو مُحي السابق لم يبق دليل يُرسل.
    nohup app_process -Djava.class.path="$APK_COMP" / \
        --nice-name="$COMPANION_NAME" nd.max.AppMonitor \
        "$MODULE_CONFIG/app_status" \
        "$MODULE_CONFIG/background_apps" \
        "$MODULE_CONFIG/java.lock" >>"$MODULE_CONFIG/sysmon.log" 2>&1 &
}

# اسم الرفيق يضعه `--nice-name`، فيُقاس حضوره **بالاسم** لا بالتخمين.
companion_alive() {
    pids=$(/system/bin/toybox pidof "$COMPANION_NAME" 2>/dev/null) || return 1
    [ -n "$pids" ]
}

# Exec Java Companion Daemon only when PackageManager can resolve the app.
COMPANION_READY=0
if [ "$PACKAGE_READY" -eq 1 ]; then
    launch_companion
    # **ولا يُبدأ الخادم على ظنّ أنّ الرفيق حيّ:** الخادم ينتظر قفله ١٢٠ ثانية كاملة ثم يُغلق الوحدة
    # كلها برسالة «Java companion daemon crashed or failed to start» — وهذا يقع فعلًا حين يموت الرفيق
    # في أوّل ثانية من عمره (عطب مقيس: تكملة ٢٢٧، حيث مُنع تخزين المستخدم قبل فتح الشاشة). والمهلة
    # هنا قصيرة عن قصد: الرفيق الذي لا يستطيع التهيئة **يقولها في سجلّه ويخرج**، فلا معنى لانتظار طويل.
    if ! /system/bin/toybox pidof init >/dev/null 2>&1; then
        # وغياب أداة الفحص **لا يُسقط الوحدة**: يُعلن أنه لم يُقس، ويُعاد السلوك السابق (الخادم يفحص بنفسه).
        log_recovery "pidof unavailable; companion liveness not measured"
        COMPANION_READY=1
    else
        attempt=0
        while [ "$attempt" -lt 15 ]; do
            if companion_alive; then
                COMPANION_READY=1
                break
            fi
            attempt=$((attempt + 1))
            sleep 1
        done

        if [ "$COMPANION_READY" -eq 1 ]; then
            log_recovery "java companion running pid=$(/system/bin/toybox pidof "$COMPANION_NAME" 2>/dev/null | tr ' ' ',')"
        else
            # ومحاولة ثانية واحدة: أوّل ثانية من الإقلاع قد تسبق جاهزية `app_process`، والثانية تُنقذ الجلسة.
            log_recovery "java companion not alive after ${attempt}s; retrying once"
            launch_companion
            attempt=0
            while [ "$attempt" -lt 10 ]; do
                if companion_alive; then
                    COMPANION_READY=1
                    break
                fi
                attempt=$((attempt + 1))
                sleep 1
            done
            if [ "$COMPANION_READY" -eq 1 ]; then
                log_recovery "java companion running after retry"
            else
                log_recovery "java companion failed to start; see sysmon.log (service not started)"
            fi
        fi
    fi
fi

# Run MaxManager service — **فقط** إذا كان الرفيق حيًّا.
# وليس الشرط وجود الحزمة وحده: انظر تعليق الفحص أعلاه — الرسالة المُضلّلة («crashed or failed to start»)
# تُستبدل بسطر واحد صادق في `package-recovery.log` يسمّي ما جرى.
if [ "$COMPANION_READY" -eq 1 ]; then
    sleep 1 && exec "$BIN_SVC" --run
else
    log_recovery "service not started; java companion unavailable"
    exit 0
fi
