# مجلّد مؤقّت فريد لكل تصدير: كان `/data/local/tmp/az_logs_tmp` ثابتًا (وباسم AZenith القديم)،
# فتصديران متزامنان يمحو أحدهما الآخر، و`rm -rf` على مسار ثابت خطر. و`trap` يضمن التنظيف
# حتى لو فشل السكربت في منتصفه.
TMP_DIR=$(mktemp -d /data/local/tmp/maxmanager-logs.XXXXXX 2>/dev/null) || \
    TMP_DIR="/data/local/tmp/maxmanager-logs.$$"
mkdir -p "$TMP_DIR" || exit 1
OUT_FILE="/data/local/tmp/${OUT_NAME##*/}"
trap 'rm -rf "$TMP_DIR"; rm -f "$OUT_FILE"' EXIT INT TERM

mkdir -p "$TMP_DIR/MaxManagerConfig" "$TMP_DIR/log"
LOG_DIR="$TMP_DIR/log"
CONFIG_DIR="/data/adb/.config/MaxManager"

# **لا يُنسخ `sysmon.log` كاملًا:** قِيس ١٥٦ م.ب منه في حزمة ٣٫٤ م.ب (٩٦٪ من الحجم)؛
# ويُؤخذ آخره هو ونسخته المدوّرة أدناه. وما عدا ذلك من الإعداد يُنسخ كما هو.
for entry in "$CONFIG_DIR"/*; do
    [ -e "$entry" ] || continue
    case "${entry##*/}" in
        sysmon.log|sysmon.log.1) continue ;;
    esac
    cp -r "$entry" "$TMP_DIR/MaxManagerConfig/" 2>/dev/null
done

# آخر ١ م.ب من كل جيل — والدليل الذي كان يضيع هو `sysmon.log.1` (يدوّره الحارس عند ٤ م.ب
# ثم لا ينسخه المصدر، فتبقى أسباب موت الرفيق في ملفّ لا يصل).
for source in "$CONFIG_DIR/sysmon.log" "$CONFIG_DIR/sysmon.log.1"; do
    [ -f "$source" ] || continue
    name="${source##*/}"
    {
        echo "--- tail of $source (last 1048576 bytes) ---"
        tail -c 1048576 "$source" 2>/dev/null
        echo ""
    } > "$TMP_DIR/MaxManagerConfig/$name.tail" 2>/dev/null
done
[ -n "${BLUEPRINT_FILE:-}" ] && cp "$BLUEPRINT_FILE" "$LOG_DIR/device_blueprint.txt" 2>/dev/null

INFO_FILE="$LOG_DIR/MaxManager.log"

MODULE_VER=$(grep '^version=' /data/adb/modules/MaxManager/module.prop 2>/dev/null | cut -d= -f2)
[ -z "$MODULE_VER" ] && MODULE_VER="Unknown"

KERNEL_INFO=$(uname -r -m)
ANDROID_VER=$(getprop ro.build.version.release)
API_LEVEL=$(getprop ro.build.version.sdk)
FINGERPRINT=$(getprop ro.build.fingerprint)
SELINUX_STATUS=$(getenforce 2>/dev/null)

echo "################################################################" > "$INFO_FILE"
echo "                       MaxManager Diagnostics                      " >> "$INFO_FILE"
echo "################################################################" >> "$INFO_FILE"
echo "" >> "$INFO_FILE"
echo "  [Device Information]" >> "$INFO_FILE"
echo "    Device Name   : $DEVICE_NAME" >> "$INFO_FILE"
echo "    Chipset       : $CHIPSET_NAME" >> "$INFO_FILE"
echo "    Android Ver   : $ANDROID_VER (API $API_LEVEL)" >> "$INFO_FILE"
echo "    Kernel        : $KERNEL_INFO" >> "$INFO_FILE"
echo "    SELinux Status: $SELINUX_STATUS" >> "$INFO_FILE"
echo "    Fingerprint   : $FINGERPRINT" >> "$INFO_FILE"
echo "" >> "$INFO_FILE"
echo "  [MaxManager]" >> "$INFO_FILE"
echo "    App Version   : $APP_VERSION" >> "$INFO_FILE"
echo "    Build Mark    : $BUILD_MARK" >> "$INFO_FILE"
echo "    Module Version: $MODULE_VER" >> "$INFO_FILE"
echo "" >> "$INFO_FILE"
echo "################################################################" >> "$INFO_FILE"
echo "" >> "$INFO_FILE"

# Every generation of both streams, oldest first: MaxManager.log.2 -> .1 -> current, then
# the verbose stream. Rotation keeps history; a dump that omits .1/.2 would lose it.
for live in "$CONFIG_DIR/debug/MaxManager.log" "$CONFIG_DIR/debug/MaxManagerVerbose.log"; do
    for gen_file in "${live}.2" "${live}.1" "$live"; do
        if [ -f "$gen_file" ]; then
            echo "--- START OF ${gen_file##*/} ---" >> "$INFO_FILE"
            cat "$gen_file" >> "$INFO_FILE"
            echo "" >> "$INFO_FILE"
        fi
    done
done

# DiagnosticCenter: مشاكل هذه الجلسة — تُدمج دائمًا بلا أي مفتاح،
# فيصل الأثر حتى لو لم يفعّل المستخدم أي سجل تفصيلي قبل المشكلة.
echo "" >> "$INFO_FILE"
echo "--- DIAGNOSTIC CENTER ---" >> "$INFO_FILE"
[ -n "${DIAG_FILE:-}" ] && cat "$DIAG_FILE" >> "$INFO_FILE" 2>/dev/null

#
# إثبات الوحدة: الأربعة التي كانت **غائبة** من الحزمة التي وصلت في 2026-10-01، فبقي
# «لماذا مات الرفيق» بلا جواب في أيّ ملفّ يرسله المستخدم:
#
# 1. `sysmon.log` — صفر بايت في تلك الحزمة (مات الرفيق قبل أن يكتب حرفًا)، وهو اليوم
#    يُكتب من الرفيق نفسه أوّل شيء ([`AppMonitorLogger.persist`]) — فيُقرأ في مكانه.
# 2. عمليّاتنا **بسياقها** (`ps -Z`): «من كان يعمل» و«بأيّ هويّة SELinux» — وهو ما يفرّق
#    بين رفضٍ سببه الهوية وبين غيابٍ سببه الموت.
# 3. رفض SELinux الذي يخصّنا وحدنا — لا ٢ ميغابايت من ضجيج النواة.
# 4. الوحدات الأخرى المثبّتة: التعايش سؤال مشروع في كل عطب عتاد (قِيس في تلك الحزمة
#    أربع وحدات أخرى بينها محرّك صوت يكتب في العقد نفسها).
#
echo "" >> "$INFO_FILE"
echo "--- MAXMANAGER EVIDENCE ---" >> "$INFO_FILE"
echo "[sysmon.log tail]" >> "$INFO_FILE"
tail -n 40 "$CONFIG_DIR/sysmon.log" >> "$INFO_FILE" 2>/dev/null
echo "[sysmon.log.1 tail]" >> "$INFO_FILE"
tail -n 40 "$CONFIG_DIR/sysmon.log.1" >> "$INFO_FILE" 2>/dev/null
echo "[our processes with contexts]" >> "$INFO_FILE"
ps -AZ 2>/dev/null | grep -E 'maxmanager|nd[.]max' >> "$INFO_FILE"
ps -A 2>/dev/null | grep -E 'maxmanager|nd[.]max' >> "$INFO_FILE"
echo "[selinux denials for nd.max / maxmanager]" >> "$INFO_FILE"
dmesg 2>/dev/null | grep -E 'avc:.*(app=nd[.]max|maxmanager|comm="DefaultDispatch")' | tail -n 40 >> "$INFO_FILE"
echo "[module properties]" >> "$INFO_FILE"
getprop 2>/dev/null | grep -E 'persist[.]sys[.]maxmanager' | sort >> "$INFO_FILE"
# ── الوضع المستقلّ / التثبيت في الروم (بلا موديول): ما يحتاجه من يضبط sepolicy من جهاز حقيقي ──
# كل سطر هنا يجيب عن سؤال واحد: هل وُجد الثنائيّ؟ هل بدأته init؟ بأيّ نطاق/مجموعات/قدرات؟ هل القناة مفتوحة؟
# هل كتب المراقب `app_status`؟ هل ملفّات cgroup المقروءة موجودة؟ وأيّ رفض SELinux يخصّ الخادم؟
echo "[rom install: flag, init state, binary]" >> "$INFO_FILE"
echo "ro.maxmanager.sepolicy=$(getprop ro.maxmanager.sepolicy 2>/dev/null)" >> "$INFO_FILE"
echo "init.svc.sys.maxmanager-service=$(getprop init.svc.sys.maxmanager-service 2>/dev/null)" >> "$INFO_FILE"
ls -lZ /system/bin/sys.maxmanager-service /system_ext/bin/sys.maxmanager-service /product/bin/sys.maxmanager-service /vendor/bin/sys.maxmanager-service 2>&1 | grep -v 'No such' >> "$INFO_FILE"
ls -lZ /product/priv-app/MaxManager /system/priv-app/MaxManager /system_ext/priv-app/MaxManager 2>&1 | grep -v 'No such' >> "$INFO_FILE"
echo "[ipc socket @maxmanager.sock]" >> "$INFO_FILE"
grep -a maxmanager /proc/net/unix 2>/dev/null >> "$INFO_FILE"
echo "[daemon identity: uid/gid/groups/caps/domain]" >> "$INFO_FILE"
DPID=$(pidof sys.maxmanager-service 2>/dev/null | cut -d' ' -f1)
if [ -n "$DPID" ]; then
    grep -E '^(Uid|Gid|Groups|CapEff|CapBnd):' "/proc/$DPID/status" >> "$INFO_FILE" 2>/dev/null
    cat "/proc/$DPID/attr/current" >> "$INFO_FILE" 2>/dev/null
    echo "" >> "$INFO_FILE"
else
    echo "daemon not running" >> "$INFO_FILE"
fi
echo "[app_status written by the daemon or the companion]" >> "$INFO_FILE"
ls -l "$CONFIG_DIR/app_status" >> "$INFO_FILE" 2>&1
cat "$CONFIG_DIR/app_status" >> "$INFO_FILE" 2>/dev/null
echo "[top-app cgroup files used by the native foreground observer]" >> "$INFO_FILE"
for cg in /dev/cpuset/top-app/cgroup.procs /dev/cpuset/top-app/tasks /dev/cpuctl/top-app/cgroup.procs /dev/cpuctl/top-app/tasks; do
    if [ -e "$cg" ]; then
        echo "$cg: $(head -n 8 "$cg" 2>/dev/null | tr '\n' ' ')" >> "$INFO_FILE"
    fi
done
echo "[selinux denials involving the maxmanager domain: dmesg + logcat, ready for make_sepolicy.py]" >> "$INFO_FILE"
# **المرشّح القديم كان يبحث عن `maxmanager` وحده** فلم يلتقط أهمّ ما في الحزمة: رفض
# `u:r:untrusted_app` و`app=nd.max` (قراءات sysfs من عملية التطبيق). وهذه هي الأسطر
# التي تُثبت تعارض «READ_WRITE» المعلن مع صلاحيات النطاق.
{ dmesg 2>/dev/null; logcat -d -b all 2>/dev/null; } | \
    grep -aE 'avc:.*(app=nd[.]max|scontext=u:r:maxmanager|comm="DefaultDispatch")' | \
    sort -u > "$LOG_DIR/avc_maxmanager.txt"
tail -n 80 "$LOG_DIR/avc_maxmanager.txt" >> "$INFO_FILE"
# القواعد جاهزة من الهاتف نفسه (بلا بايثون): تُنسخ إلى مجلّد sepolicy في الروم. فشلها (خادم قديم) لا يضرّ.
[ -x "$SERVICE_BIN" ] && \
    "$SERVICE_BIN" --gen-sepolicy "$LOG_DIR/avc_maxmanager.txt" -o "$LOG_DIR/maxmanager_generated.te" >/dev/null 2>&1
echo "[other installed modules]" >> "$INFO_FILE"
ls -1 /data/adb/modules 2>/dev/null >> "$INFO_FILE"
# سبب الإقلاع وحداثة pstore: حزمة ٢٠٢٦-١٠-١٥ أقلعت مرتين `kernel_panic/AP_WDT`
# و`console-ramoops` فيها قديم (٢٠ سبتمبر) فلا مكدّس للانهيار — فبهذين السطرين
# تُقرأ في الحزمة القادمة مباشرة: هل كان الإقلاع watchdog؟ وهل pstore طازج أم معاد؟
echo "[boot reason]" >> "$INFO_FILE"
echo "ro.boot.bootreason=$(getprop ro.boot.bootreason 2>/dev/null)" >> "$INFO_FILE"
echo "sys.boot.reason=$(getprop sys.boot.reason 2>/dev/null)" >> "$INFO_FILE"
echo "boot.history=$(getprop persist.sys.boot.reason.history 2>/dev/null)" >> "$INFO_FILE"
echo "[pstore freshness]" >> "$INFO_FILE"
ls -l /sys/fs/pstore 2>/dev/null >> "$INFO_FILE"

cp -r /sys/fs/pstore "$TMP_DIR/" 2>/dev/null

# ── **الحيّ الآن** ────────────────────────────────────────────────────────────
# الحزمة السابقة حملت `logcat.log` و`dmesg.log` فقط، وهما **سجلّا KernelSU وقت الإقلاع**
# (نافذة ٣٧–٤١ ثانية بعد الإقلاع)، لا مخزنَي النظام الحيّين. فمن قرأها ظنّ أن لا شيء يحدث
# بعد الإقلاع، بينما العطب يقع الساعة ١٥:١٨. هذه الأربعة تُغلق ذلك الفرق.
logcat -d -b all -v threadtime 2>/dev/null | tail -n 20000 > "$LOG_DIR/logcat-live.log"
dmesg 2>/dev/null | tail -n 5000 > "$LOG_DIR/dmesg-live.log"
dumpsys package nd.max > "$LOG_DIR/dumpsys-package.txt" 2>/dev/null
dumpsys meminfo nd.max > "$LOG_DIR/dumpsys-meminfo.txt" 2>/dev/null
dumpsys activity services nd.max > "$LOG_DIR/dumpsys-services.txt" 2>/dev/null
dumpsys notification --noredact 2>/dev/null | grep -a -B2 -A10 'nd\.max' | head -n 600 > "$LOG_DIR/dumpsys-notification.txt"
getprop > "$LOG_DIR/getprop.txt" 2>/dev/null

# سجلّا الإقلاع من KernelSU/APatch يبقيان (يفيدان في تحليل الإقلاع)، لكن بعنوان صريح
# يمنع قراءتهما كسجلّ حيّ.
{
    echo "--- KernelSU/APatch boot log (written around boot; NOT the live buffer) ---"
    cat /data/adb/ksu/log/logcat.log 2>/dev/null
    cat /data/adb/ap/log/logcat.log 2>/dev/null
} > "$LOG_DIR/logcat.log"
{
    echo "--- KernelSU/APatch boot logcat (written around boot; NOT the live buffer) ---"
    cat /data/adb/ksu/log/logcat.old.log 2>/dev/null
    cat /data/adb/ap/log/logcat.old.log 2>/dev/null
} > "$LOG_DIR/logcat.old.log"
{
    echo "--- KernelSU/APatch boot dmesg ---"
    cat /data/adb/ksu/log/dmesg.log 2>/dev/null
    cat /data/adb/ap/log/dmesg.log 2>/dev/null
} > "$LOG_DIR/dmesg.log"
{
    echo "--- KernelSU/APatch boot dmesg (previous) ---"
    cat /data/adb/ksu/log/dmesg.old.log 2>/dev/null
    cat /data/adb/ap/log/dmesg.old.log 2>/dev/null
} > "$LOG_DIR/dmesg.old.log"

cat /cache/magisk.log > "$LOG_DIR/magisk.log" 2>/dev/null
cat /data/adb/magisk.log >> "$LOG_DIR/magisk.log" 2>/dev/null
cat /data/adb/apatch/log/* >> "$LOG_DIR/apatch.log" 2>/dev/null

# **الخروج من `$TMP_DIR` لا من مسار ثابت:** وكان `tar -C az_logs_tmp` يعتمد على `cd`
# سابق، فلو تغيّر المجلّد خرجت الحزمة فارغة أو خاطئة. والمخرَج خارج المجلّد المؤقّت
# حتى لا تُدرج الحزمة نفسها.
tar -czf "$OUT_FILE" -C "$TMP_DIR" . || exit 1

mkdir -p "$(dirname "$TARGET_PATH")"
cp "$OUT_FILE" "$TARGET_PATH" || exit 1

if [ "$SAVE_TO_DOWNLOADS" = "true" ]; then
    chmod 666 "$TARGET_PATH"
else
    chown "$APP_UID:$APP_UID" "$TARGET_PATH"
    chmod 600 "$TARGET_PATH"
    restorecon "$TARGET_PATH" 2>/dev/null
fi
