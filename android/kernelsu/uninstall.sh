#!/system/bin/sh
# Copyright (C) 2026 Nader Magdy. All rights reserved.
# Proprietary and confidential — not licensed for use, copying, or distribution
# without prior written permission from the copyright holder.

MODDIR=${0%/*}
MAXMANAGER_ROOT=/data/adb/maxmanager
MAXMANAGER_WATCHDOG_LOCK="$MAXMANAGER_ROOT/watchdog.lock"
MAXMANAGER_WATCHDOG_PID="$(cat "$MAXMANAGER_ROOT/watchdog.pid" 2>/dev/null)"
MAXMANAGER_PACKAGE=nd.max
MAXMANAGER_NATIVE_MODE=0

[ -f "$MODDIR/rom-native-mode" ] && MAXMANAGER_NATIVE_MODE=1

# Kill watchdog and daemon
case "$MAXMANAGER_WATCHDOG_PID" in
    ''|*[!0-9]*) ;;
    *) kill "$MAXMANAGER_WATCHDOG_PID" 2>/dev/null ;;
esac

for MAXMANAGER_PROCESS_NAME in sys.maxmanager-service sys.maxmanager-rianixiathermalcore sys.maxmanager-appmonitoring; do
    for MAXMANAGER_PID in $(pidof "$MAXMANAGER_PROCESS_NAME" 2>/dev/null); do
        kill "$MAXMANAGER_PID" 2>/dev/null
    done
done

sleep 1
for MAXMANAGER_PROCESS_NAME in sys.maxmanager-service sys.maxmanager-rianixiathermalcore sys.maxmanager-appmonitoring; do
    for MAXMANAGER_PID in $(pidof "$MAXMANAGER_PROCESS_NAME" 2>/dev/null); do
        kill -9 "$MAXMANAGER_PID" 2>/dev/null
    done
done

# Clean up state files
rm -f "$MAXMANAGER_ROOT/watchdog.pid" "$MAXMANAGER_WATCHDOG_LOCK/pid" "$MAXMANAGER_WATCHDOG_LOCK/boot_id"
rmdir "$MAXMANAGER_WATCHDOG_LOCK" 2>/dev/null
rm -f "$MAXMANAGER_ROOT/daemon.log" "$MAXMANAGER_ROOT/daemon.log.1"

if [ "$MAXMANAGER_NATIVE_MODE" -eq 1 ]; then
    # ROM-native mode: revert system app updates
    /system/bin/cmd package uninstall-system-updates "$MAXMANAGER_PACKAGE" \
        >"$MAXMANAGER_ROOT/native-app-rollback.log" 2>&1 || true
    /system/bin/pm enable --user 0 "$MAXMANAGER_PACKAGE" >/dev/null 2>&1 || true
fi

# ── الحالة المحفوظة: تُمسح كما في إزالة الوحدة القياسية ──
#
# بلاغ SHARK: بعد إزالة الوحدة من KernelSU ظلّت إعدادات النسخة السابقة تُطبَّق عند الإقلاع.
# والسبب أن هذا المسار كان يقتل الخدمة وحدها ويترك خصائص `persist.sys.maxmanager*`
# و`/data/adb/.config/MaxManager` (وفيه `API/current_profile` و`API/max_ai_mode`)،
# فيقرأها الخادم والملفّ التنفيذيّ للخطط فيعيد الوضع القديم. الإزالة تعني الإزالة.
if command -v resetprop >/dev/null 2>&1; then
    resetprop | awk -F'[][]' '/persist\.sys\.maxmanager/ {print $2}' | while read -r MAXMANAGER_PROP; do
        [ -n "$MAXMANAGER_PROP" ] && resetprop -p --delete "$MAXMANAGER_PROP"
    done
fi
if command -v resetprop >/dev/null 2>&1; then
    for MAXMANAGER_PROP in persist.sys.rianixia.learning_enabled persist.sys.rianixia.thermalcore-bigdata.path; do
        resetprop -p --delete "$MAXMANAGER_PROP"
    done
fi

rm -rf \
    "/data/adb/.config/MaxManager" \
    "/data/MaxManager" \
    "/data/data/nd.max" \
    "$MAXMANAGER_ROOT"

# والثنائيات التي قد تُشغَّل من خارج مجلّد الوحدة تبقى بعده — فتُزال أيضًا.
for MAXMANAGER_BIN_DIR in /data/adb/ap/bin /data/adb/ksu/bin; do
    [ -d "$MAXMANAGER_BIN_DIR" ] || continue
    rm -f "$MAXMANAGER_BIN_DIR/zx"
    for MAXMANAGER_BIN in "$MAXMANAGER_BIN_DIR"/sys.maxmanager-*; do
        [ -e "$MAXMANAGER_BIN" ] && rm -f "$MAXMANAGER_BIN"
    done
done

exit 0