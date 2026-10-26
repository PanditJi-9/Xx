#!/usr/bin/env bash
# مسبار أوامر Game Space بصلاحية uid الـshell — يجيب عن أكبر مجهول في وضع Shizuku: «أيّ الأوامر يقبلها الـshell فعلًا؟».
#
# لماذا يصحّ بديلًا عن تجربة Shizuku: خدمة Shizuku تعمل بـuid الـshell (2000) وهو uid `adb shell` نفسه. فما يقبله
# `adb shell` هنا يقبله `PrivilegedShell.exec` تحت Shizuku (إن بدأتَ Shizuku عبر adb). وإن كان Shizuku مشغَّلًا بالجذر
# فالـuid صفر والنتيجة لا تمثّل وضع Shizuku — والمسبار يطبع الـuid ليُعرف.
#
# **غير مدمِّر:** كل كتابة إمّا إعادة نفس القيمة الحالية، أو مفتاح اختبار خاص يُحذف في الحال، أو ملفّ مؤقّت يُحذف.
# الاستعمال:
#   bash tools/gamespace_shell_probe.sh [-s SERIAL] [-p PACKAGE] [-n N] [-o REPORT.txt] [--no-record] [--no-svc]
#     -s  رقم الجهاز لـadb          -p  حزمة التطبيق (الافتراضي nd.max)
#     -n  عدد مرّات قياس الإقلاع البارد (0 = بلا قياس، الافتراضي 0)
#     -o  يكتب التقرير في ملفّ أيضًا
#     --no-record  يتخطّى `screenrecord` (٢ ثانية)      --no-svc  يتخطّى `svc wifi/data` (إعادة نفس الحالة، لكن قد يقطع ADB اللاسلكي)
# الخروج: 0 كل شيء نجح أو تُخطّي، 1 إن فشل مسبار واحد على الأقل، 2 خطأ استعمال/اتصال.
set -u

SERIAL=""; PKG="nd.max"; COLD=0; REPORT=""; DO_RECORD=1; DO_SVC=1
while [ $# -gt 0 ]; do
  case "$1" in
    -s) SERIAL="${2:-}"; shift 2 ;;
    -p) PKG="${2:-}"; shift 2 ;;
    -n) COLD="${2:-0}"; shift 2 ;;
    -o) REPORT="${2:-}"; shift 2 ;;
    --no-record) DO_RECORD=0; shift ;;
    --no-svc) DO_SVC=0; shift ;;
    -h|--help) sed -n '2,17p' "$0"; exit 0 ;;
    *) echo "خيار غير معروف: $1" >&2; exit 2 ;;
  esac
done
case "$COLD" in ''|*[!0-9]*) echo "-n يجب أن يكون عددًا" >&2; exit 2 ;; esac

ADB=(adb); [ -n "$SERIAL" ] && ADB=(adb -s "$SERIAL")

RC=0; OUT=""
# ينفّذ أمرًا على الجهاز ويعيد رمز الخروج الحقيقي في RC والخَرْج في OUT (adb القديم لا ينقل الرمز، فنحمله نحن).
run() {
  local raw
  raw=$("${ADB[@]}" shell "$1; echo __RC=\$?" 2>&1 | tr -d '\r')
  RC=$(printf '%s\n' "$raw" | sed -n 's/^__RC=\([0-9][0-9]*\)$/\1/p' | tail -n 1)
  [ -z "$RC" ] && RC=255
  OUT=$(printf '%s\n' "$raw" | sed '/^__RC=/d')
}
trim() { printf '%s' "$1" | tr -d '\r' | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//'; }
lastline() { printf '%s\n' "$1" | sed '/^[[:space:]]*$/d' | tail -n 1 | tr -d '\r'; }

RESULTS=(); FAILS=0
record() { RESULTS+=("$1|$2|$3"); [ "$1" = "FAIL" ] && FAILS=$((FAILS + 1)); return 0; }
pass() { record PASS "$1" "${2:-}"; }
fail() { record FAIL "$1" "${2:-}"; }
skip() { record SKIP "$1" "${2:-}"; }

if ! "${ADB[@]}" get-state >/dev/null 2>&1; then
  echo "لا جهاز متصل بـadb (أو أكثر من جهاز: استعمل -s)." >&2; exit 2
fi

run "id -u"; UID_NOW=$(trim "$(lastline "$OUT")")
run "getprop ro.build.version.sdk"; SDK=$(trim "$(lastline "$OUT")")
run "getprop ro.product.model"; MODEL=$(trim "$(lastline "$OUT")")
if [ "$UID_NOW" = "2000" ]; then pass "uid" "2000 (shell) — يمثّل Shizuku المبدوء عبر adb"
else skip "uid" "uid=$UID_NOW لا 2000: النتائج لا تمثّل وضع Shizuku"; fi

# ── ١) صلاحية الكتابة في الفضاءات الثلاثة، بمفتاح اختبار يُحذف (لا نمسّ مفتاحًا حقيقيًّا) ─────────────────────────────
for ns in global secure system; do
  key="maxmanager_probe_${ns}"
  run "settings put $ns $key 1"; put_rc=$RC; put_out=$OUT
  run "settings get $ns $key"; got=$(trim "$(lastline "$OUT")")
  run "settings delete $ns $key"
  if [ "$put_rc" = "0" ] && [ "$got" = "1" ]; then pass "settings put $ns" "كتابة وقراءة وحذف"
  else fail "settings put $ns" "rc=$put_rc get='$got' ${put_out:0:80}"; fi
done

# ── ٢) عدم الإزعاج: إعادة القيمة الحالية نفسها (لا يغيّر النمط) ──────────────────────────────────────────────────────
run "settings get global zen_mode"; zen=$(trim "$(lastline "$OUT")")
case "$zen" in
  [0-9]) run "settings put global zen_mode $zen"
         if [ "$RC" = "0" ]; then pass "zen_mode write-back" "القيمة $zen"; else fail "zen_mode write-back" "rc=$RC ${OUT:0:80}"; fi ;;
  *) skip "zen_mode write-back" "غير مضبوط ($zen)" ;;
esac

# ── ٣) مفاتيح معدّل التحديث والاهتزاز: إعادة قيمها الحالية إن وُجدت ──────────────────────────────────────────────────
writeback() {  # ns key label
  run "settings get $1 $2"; local cur; cur=$(trim "$(lastline "$OUT")")
  if [ -z "$cur" ] || [ "$cur" = "null" ]; then skip "$3" "غير مضبوط على هذا الجهاز"; return; fi
  case "$cur" in *[!A-Za-z0-9_.,:-]*) skip "$3" "قيمة لا تُقتبس بأمان: $cur"; return ;; esac
  run "settings put $1 $2 '$cur'"
  if [ "$RC" = "0" ]; then pass "$3" "$cur"; else fail "$3" "rc=$RC ${OUT:0:80}"; fi
}
writeback secure user_refresh_rate "refresh secure user_refresh_rate"
writeback secure miui_refresh_rate "refresh secure miui_refresh_rate"
writeback system peak_refresh_rate "refresh system peak_refresh_rate"
writeback system min_refresh_rate "refresh system min_refresh_rate"
writeback system haptic_feedback_enabled "haptic_feedback_enabled"

# ── ٤) لقطة الشاشة ───────────────────────────────────────────────────────────────────────────────────────────────────
PIC="/sdcard/Pictures/MaxManagerProbe"
run "mkdir -p $PIC && screencap -p $PIC/probe.png; stat -c %s $PIC/probe.png 2>/dev/null || echo 0"; size=$(trim "$(lastline "$OUT")")
run "rm -rf $PIC"
case "$size" in ''|*[!0-9]*) size=0 ;; esac
if [ "$size" -gt 0 ]; then pass "screencap" "${size} بايت"; else fail "screencap" "ملفّ صفريّ أو لم يُكتب (محتوى محمي؟ شاشة مطفأة؟)"; fi

# ── ٥) تسجيل الشاشة + بقاء الحلقة الخلفية بعد عودة الأمر (تقريبي لمسار Shizuku) ─────────────────────────────────────────
if [ "$DO_RECORD" = "1" ]; then
  MOV="/sdcard/Movies/.mm_probe.mp4"
  run "screenrecord --time-limit 2 $MOV; stat -c %s $MOV 2>/dev/null || echo 0"; msize=$(trim "$(lastline "$OUT")")
  run "rm -f $MOV"
  case "$msize" in ''|*[!0-9]*) msize=0 ;; esac
  if [ "$msize" -gt 0 ]; then pass "screenrecord" "${msize} بايت"; else fail "screenrecord" "لم يُكتب ملفّ"; fi

  FLAG="/data/local/tmp/.mm_probe_flag"
  run "touch $FLAG && ((while [ -f $FLAG ]; do screenrecord --time-limit 2 /sdcard/Movies/.mm_probe_bg.mp4 || { rm -f $FLAG; break; }; done) >/dev/null 2>&1 &)"
  sleep 3
  run "[ -f $FLAG ] && (pidof screenrecord >/dev/null 2>&1 && echo alive || echo dead) || echo flag-gone"; state=$(trim "$(lastline "$OUT")")
  run "rm -f $FLAG; pkill -2 screenrecord || killall -2 screenrecord; sleep 1; rm -f /sdcard/Movies/.mm_probe_bg.mp4"
  case "$state" in
    alive) pass "screenrecord background loop" "الحلقة تعيش بعد عودة الصدفة (تقريبي: Shizuku قد يختلف)" ;;
    flag-gone) fail "screenrecord background loop" "حُذف العلم: فشل screenrecord داخل الحلقة" ;;
    *) fail "screenrecord background loop" "الحلقة ماتت بعد عودة الصدفة ($state)" ;;
  esac
else
  skip "screenrecord" "--no-record"
fi

# ── ٦) wm: إعادة المقاس والكثافة الفعّالين أنفسهما ────────────────────────────────────────────────────────────────────
run "wm size; wm density"; wm_out="$OUT"
phys=$(printf '%s\n' "$wm_out" | sed -n 's/^Physical size: *\([0-9]*x[0-9]*\).*/\1/p' | head -n 1)
over=$(printf '%s\n' "$wm_out" | sed -n 's/^Override size: *\([0-9]*x[0-9]*\).*/\1/p' | head -n 1)
dphys=$(printf '%s\n' "$wm_out" | sed -n 's/^Physical density: *\([0-9]*\).*/\1/p' | head -n 1)
dover=$(printf '%s\n' "$wm_out" | sed -n 's/^Override density: *\([0-9]*\).*/\1/p' | head -n 1)
if [ -z "$phys" ] || [ -z "$dphys" ]; then
  fail "wm size/density read" "لم يُفهم الخَرْج: ${wm_out:0:80}"
else
  pass "wm size/density read" "$phys @ ${dphys}dpi"
  size_now="${over:-$phys}"; dens_now="${dover:-$dphys}"
  run "wm size $size_now; wm density $dens_now"
  if [ "$RC" = "0" ]; then pass "wm write-back" "$size_now @ ${dens_now}dpi"; else fail "wm write-back" "rc=$RC ${OUT:0:80}"; fi
  # الحالة الأصلية: إن لم يكن هناك تجاوز أصلًا فأعدها إلى «لا تجاوز».
  [ -z "$over" ] && run "wm size reset"
  [ -z "$dover" ] && run "wm density reset"
fi

# ── ٧) إذن وصول الاستخدام (نفس أمر التطبيق): إعادة وضعه الحالي ───────────────────────────────────────────────────────────
run "pm list packages $PKG | grep -x package:$PKG"
if [ -z "$(trim "$OUT")" ]; then
  skip "appops GET_USAGE_STATS" "الحزمة $PKG غير مثبّتة"
else
  run "appops get $PKG GET_USAGE_STATS"; mode_line=$(trim "$(lastline "$OUT")")
  mode=$(printf '%s' "$mode_line" | sed -n 's/^GET_USAGE_STATS: *\([a-z_]*\).*/\1/p')
  [ -z "$mode" ] && mode="default"
  run "appops set $PKG GET_USAGE_STATS $mode"; set_rc=$RC
  run "appops get $PKG GET_USAGE_STATS"; after=$(printf '%s' "$(lastline "$OUT")" | sed -n 's/^GET_USAGE_STATS: *\([a-z_]*\).*/\1/p'); [ -z "$after" ] && after="default"
  if [ "$set_rc" = "0" ] && [ "$after" = "$mode" ]; then pass "appops set GET_USAGE_STATS" "الوضع الحالي: $mode"
  else fail "appops set GET_USAGE_STATS" "rc=$set_rc قبل=$mode بعد=$after"; fi
fi

# ── ٨) svc: إعادة الحالة الحالية نفسها ─────────────────────────────────────────────────────────────────────────────────
if [ "$DO_SVC" = "1" ]; then
  run "settings get global wifi_on"; w=$(trim "$(lastline "$OUT")")
  case "$w" in 1) run "svc wifi enable" ;; 0) run "svc wifi disable" ;; *) RC=-1 ;; esac
  if [ "$RC" = "0" ]; then pass "svc wifi" "إعادة الحالة ($w)"; elif [ "$RC" = "-1" ]; then skip "svc wifi" "حالة غير معروفة ($w)"; else fail "svc wifi" "rc=$RC ${OUT:0:80}"; fi
  run "settings get global mobile_data"; d=$(trim "$(lastline "$OUT")")
  case "$d" in 1) run "svc data enable" ;; 0) run "svc data disable" ;; *) RC=-1 ;; esac
  if [ "$RC" = "0" ]; then pass "svc data" "إعادة الحالة ($d)"; elif [ "$RC" = "-1" ]; then skip "svc data" "حالة غير معروفة ($d)"; else fail "svc data" "rc=$RC ${OUT:0:80}"; fi
else
  skip "svc wifi/data" "--no-svc"
fi

# ── ٩) معلومة: Game Mode ─────────────────────────────────────────────────────────────────────────────────────────────
run "cmd -l | grep -x game"
if [ -n "$(trim "$OUT")" ]; then pass "cmd game (معلومة)" "الخدمة موجودة — ميزة Game Mode لكل لعبة ممكنة"; else skip "cmd game (معلومة)" "غير موجودة (أندرويد < 12؟)"; fi

# ── ١٠) الإقلاع البارد (اختياري) ───────────────────────────────────────────────────────────────────────────────────────
median() { sort -n | awk '{a[NR]=$1} END{ if(NR==0){print "-"} else if(NR%2){print a[(NR+1)/2]} else {print int((a[NR/2]+a[NR/2+1])/2)} }'; }
COLD_APP=""; COLD_LOBBY=""
if [ "$COLD" -gt 0 ]; then
  run "pm list packages $PKG | grep -x package:$PKG"
  if [ -z "$(trim "$OUT")" ]; then
    skip "cold start" "الحزمة $PKG غير مثبّتة"
  else
    apps=""; lobbies=""
    for _ in $(seq 1 "$COLD"); do
      run "am force-stop $PKG"; sleep 1
      run "am start -W -n $PKG/nd.max.MainActivity"
      t=$(printf '%s\n' "$OUT" | sed -n 's/^TotalTime: *\([0-9]*\).*/\1/p' | head -n 1); [ -n "$t" ] && apps="$apps$t\n"
      run "am force-stop $PKG"; sleep 1
      run "am start -W -a nd.max.action.OPEN_GAME_LOBBY -n $PKG/nd.max.MainActivity --es nd.max.widget.OPEN_ROUTE game_lobby"
      t=$(printf '%s\n' "$OUT" | sed -n 's/^TotalTime: *\([0-9]*\).*/\1/p' | head -n 1); [ -n "$t" ] && lobbies="$lobbies$t\n"
    done
    COLD_APP=$(printf "$apps" | median); COLD_LOBBY=$(printf "$lobbies" | median)
    pass "cold start (التطبيق)" "وسيط TotalTime = ${COLD_APP}ms من $COLD تشغيلات"
    pass "cold start (اختصار اللوبي)" "وسيط TotalTime = ${COLD_LOBBY}ms من $COLD تشغيلات — أول إطار للنشاط لا جاهزية اللوبي"
    run "am force-stop $PKG"
  fi
fi

# ── التقرير ──────────────────────────────────────────────────────────────────────────────────────────────────────────
emit() {
  echo "Game Space shell probe — $(date '+%Y-%m-%d %H:%M')"
  echo "الجهاز: ${MODEL:-?} · SDK ${SDK:-?} · uid ${UID_NOW:-?} · الحزمة $PKG"
  echo "------------------------------------------------------------------------"
  local row status name detail
  for row in "${RESULTS[@]}"; do
    status="${row%%|*}"; row="${row#*|}"; name="${row%%|*}"; detail="${row#*|}"
    printf '%-5s %-34s %s\n' "$status" "$name" "$detail"
  done
  echo "------------------------------------------------------------------------"
  echo "فشل: $FAILS"
}
if [ -n "$REPORT" ]; then emit | tee "$REPORT"; else emit; fi
[ "$FAILS" -eq 0 ] || exit 1
exit 0
