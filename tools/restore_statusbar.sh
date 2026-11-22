#!/system/bin/sh
#
# restore_statusbar.sh — عكس تأثير بلاط «شريط حالة نظيف» (Clean status bar)
#
# يزيل رموز شريط الحالة **المعروفة** (wifi, cell, battery, clock, ...) من
# Settings.Secure.icon_blacklist، مع **الحفاظ** على الرموز المجهولة/الخاصة بالروم
# (nfc, headset, rotate, ...) التي كانت مخفيةً قبل التفعيل.
#
# الاستخدام:
#   sh /sdcard/restore_statusbar.sh          # على الجهاز (root أو WRITE_SECURE_SETTINGS)
#   adb shell sh /sdcard/restore_statusbar.sh
#
# رموز «الشريط النظيف» (StatusBarIcon.entries) — هي ما يُضاف عند التفعيل:
KNOWN_TOKENS="wifi cell battery clock alarm_clock bluetooth volume mute vibrate zen dnd location gps cast hotspot vpn airplane"

get_blacklist() {
    settings get secure icon_blacklist 2>/dev/null
}

put_blacklist() {
    settings put secure icon_blacklist "$1" 2>/dev/null
}

current="$(get_blacklist)"
case "$current" in ""|"null"|"NULL") current="" ;; esac

# بناء القائمة الجديدة: احتفظ بالرموز غير المعروفة فقط.
new=""
old_ifs="$IFS"
IFS=','
for tok in $current; do
    IFS="$old_ifs"
    t="$(printf '%s' "$tok" | tr '[:upper:]' '[:lower:]' | tr -d '[:space:]')"
    [ -z "$t" ] && continue
    skip=0
    for k in $KNOWN_TOKENS; do
        if [ "$t" = "$k" ]; then
            skip=1
            break
        fi
    done
    if [ "$skip" = "0" ]; then
        if [ -z "$new" ]; then
            new="$t"
        else
            new="$new,$t"
        fi
    fi
done
IFS="$old_ifs"

ok=0
if put_blacklist "$new"; then
    ok=1
else
    # fallback: جرّب عبر root (su) إن كان متاحًا.
    if command -v su >/dev/null 2>&1; then
        if su -c "settings put secure icon_blacklist '$new'" >/dev/null 2>&1; then
            ok=1
        fi
    fi
fi

if [ "$ok" = "1" ]; then
    printf 'تمت استعادة أيقونات شريط الحالة. icon_blacklist الآن: %s\n' "'${new}'"
    exit 0
else
    printf 'فشل كتابة الإعداد. شغّل كـroot أو امنح WRITE_SECURE_SETTINGS.\n' >&2
    exit 1
fi
