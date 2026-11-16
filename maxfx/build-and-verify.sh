set -e
set -e
# ── رؤوس AIDL أوّلًا: غلافُ العقد المزدوج يُترجم بها ──
#
# `aidl` من `build-tools` في SDK هذه البيئة، والتوليد **مُعلَن نتيجته**
# (`GEN=ok …` أو `GEN=skipped reason=…`) — ولا يُسكِت الفشل: البوابة أدناه تسقط
# إن غابت الرموز الأربعة الجديدة. والمسارات والنسخ المجمَّدة في `maxfx/aidl/README.md`.
sh maxfx/aidl/gen.sh
# والرأس مطلوبٌ بالاسم، والمصادر **تُبحَث ولا تُفترض**: المولِّد يكتب الرؤوس
# ببادئة `aidl/` (‏`gen/aidl/android/…`) والمصادر من `GetOutputFilePath` التي تبني
# من اسم الحزمة وحده (‏`gen/android/…`) — قِيس في `37034110669`: الفحص بحث في
# `gen/aidl/…/IEffect.cpp` فلم يجده. وتركُ المصادر عطبُ **ربطٍ** لا ترجمة
# (‏`37031374179`: `undefined symbol: BnEffect::createBinder()`).
if [ ! -s maxfx/gen/aidl/android/hardware/audio/effect/BnEffect.h ]; then
  echo "::error::رأس الواجهة المُولَّد غائب ⇒ لا يُبنى غلاف createEffect/queryEffect/destroyEffect"
  exit 1
fi
echo "--- أين صارت المصادر المُولَّدة؟ ---"
find maxfx/gen -name '*.cpp' | head -5 || true
aidl_srcs="$(find maxfx/gen -name '*.cpp' | wc -l | tr -d ' ')"
echo "عدد مصادر AIDL المُولَّدة: $aidl_srcs"
if [ "$aidl_srcs" -eq 0 ]; then
  echo "::error::لم يُولَّد أيّ مصدر ⇒ رابطٌ بلا تعريف"
  exit 1
fi
# ── بيانٌ مقيس لأعيان الـNDK: هل لا تزال رؤوس الـC++ غائبة؟ ──
#
# الكود المُولَّد يُضمِّن رؤوس `android/binder_*.h` لطبقة الـC++، والـNDK يُشحَن
# واجهة C فقط — وذاك سبب وجود `maxfx/third_party/libbinder_ndk_cpp` المنسوخة.
# وهذا السطر ليس تشخيصًا للعطب بل **بيانًا يُثبّت الحاجة**: يومَ يُشحَن الرأس في
# الـNDK يُطبع اسم مساره، فيصير نسخنا مرشّحًا للإزالة بقرار لا بتخمين.
vendored="maxfx/third_party/libbinder_ndk_cpp"
for h in binder_interface_utils.h binder_auto_utils.h binder_stability.h \
         binder_enums.h binder_internal_logging.h binder_parcel_utils.h \
         binder_parcelable_utils.h binder_to_string.h; do
  if [ ! -s "$vendored/android/$h" ]; then
    echo "::error::رأسٌ منسوخ غائب أو فارغ: $vendored/android/$h"
    exit 1
  fi
done
echo "✅ رؤوس binder_ndk المنسوخة كاملة في $vendored"
found="$(find "$ANDROID_NDK_HOME" -name 'binder_interface_utils.h' -print -quit 2>/dev/null || true)"
if [ -n "$found" ]; then
  echo "⚠️ ملاحظة: الـNDK صار يُشحَن $found — يُراجَع نسخنا الرسوميّ (لا يُحذف تلقائيًّا)."
fi
# ── تصريف سطحيّ لبدائل `compat/`: ثوانٍ هنا بدل دورة بناء كاملة ──
#
# بدائلنا تُقلّد سطح `libbase`/`libutils` بقدر ما تستعمله `libfmq`، وسطحٌ ناقص
# فيها لا يُكشف إلا **داخل** بناء المكتبة — بعد التوليد ودقائق من الترجمة. ووقع
# فعلًا: التشغيل `37030025157` سقط بـ«use of undeclared identifier 'ERROR'» في
# `FmqInternal.cpp` — وهو عطبُ سطحٍ يُمسك بتصريف سطرٍ واحد.
# والحاضنة تُحاكي موضع الاستعمال حرفيًّا (‏`namespace android::hardware::details`)
# وتُصرَّف بمُصرّف الـNDK نفسه (فسياق `android/log.h` حقيقيّ لا مُستبدَل) وبـ`-Werror`
# كما في `Android.mk` — فلا يُقاس سطحٌ بغير ما يُبنى به. وهي لا تدخل المكتبة:
# ليست في `LOCAL_SRC_FILES`، و`ndk-build` لا يُجمّع الملفّات تلقائيًّا.
ndk_cxx="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android31-clang++"
if [ -x "$ndk_cxx" ]; then
  "$ndk_cxx" -std=c++20 -Wall -Wextra -Werror -fsyntax-only \
    -Imaxfx/third_party/libfmq/compat \
    maxfx/third_party/libfmq/compat/logging_surface_test.cpp
  echo "✅ سطح بدائل compat يُصرَّف (logging)"
else
  echo "::warning::مُصرّف الـNDK غير موجود ⇒ سطح بدائل compat **غير مُتحقَّق** — لا يُدَّعى"
fi
export NDK_CCACHE="$(command -v ccache || true)"
(cd maxfx && ndk-build)
readelf_bin="$(command -v llvm-readelf || command -v readelf)"
# الرموز الخمسة: `AELI` (HIDL ≤11) والثلاثة (AIDL 12+) — و`libmaxfx.so` الواحدة
# تُصدِّرها معًا فيعمل الجهاز على الجيلين بلا نسختين ولا تفريع.
for abi in arm64-v8a armeabi-v7a; do
  so="maxfx/libs/$abi/libmaxfx.so"
  if [ ! -s "$so" ]; then
    echo "::error::libmaxfx.so غائب في $so — البناء لم يُنتج العمود"
    exit 1
  fi
  symbols="$("$readelf_bin" --dyn-syms -W "$so" | awk '$7 != "UND" { print $8 }')"
  missing=""
  for sym in AELI createEffect queryEffect destroyEffect; do
    if ! printf '%s\n' "$symbols" | grep -qx "$sym"; then
      missing="$missing $sym"
    fi
  done
  if [ -n "$missing" ]; then
    echo "::error::رموز ناقصة في $so:$missing — المكتبة ستبني ولا تُحمَّل على الجهاز"
    echo "   مصنع AIDL يبحث بـdlsym عن createEffect/queryEffect/destroyEffect،"
    echo "   وAELI هو عقد الجيل الأقدم. غياب أيٍّ منها = «لا تغيّر في الصوت» بلا سطر عطل."
    printf '%s\n' "$symbols" | grep -E '^(AELI|createEffect|queryEffect|destroyEffect)$' || true
    "$readelf_bin" --dyn-syms -W "$so" | head -25
    exit 1
  fi
  echo "✅ $so · الخمسة مُصدَّرة"
  printf '%s\n' "$symbols" | grep -E '^(AELI|createEffect|queryEffect|destroyEffect)$' | sed 's/^/     /'
done

# والخمسة الباقية تُشغَّل في الخلفية وتُحصَد بعد Gradle (الشرح أعلاه).
# ولا `wait` بين الخطوتين: كل خطوة صدفة shell مستقلة، فالشهادة هنا **ملفٌ تكتبه الخلفية**
# بصدفة خروجها — وهذا ما تقرأه خطوة الحصاد.
#
# ⚠️ و`set -e` **داخل صدفة داخلية** لا في المجموعة `{ }` مباشرة: `{ }` تُنفَّذ في الصدفة
# الحالية، فـ`set -e` يُسرّب إلى ما بعدها ويُقفل الصدفة عند أوّل فشل **قبل** كتابة ملف
# الشهادة — فينتظر الحصاد عشر دقائق على ملف لن يُكتب. مُقيس في محاكاة محليّة (تكملة ٨٦) لا مفترض.
