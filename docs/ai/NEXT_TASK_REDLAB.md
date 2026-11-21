# NEXT_TASK_REDLAB — 2026-11-21 — كود نملكه ونطوّره

## السياق (لا تعيد اختراعه)
- المستودع: `/storage/emulated/0/red/repo` · remote `PanditJi-9/Xx` · branch `main`.
- العقد: `AGENTS.md` + `docs/ai/ENGINEERING-CONTRACT.md` + `docs/ai/VALIDATION.md` + `docs/ai/REVIEW.md`.
- لا بناء تلقائيًا (§0.1): بوابات `kt_balance`/`code_health`/`i18n_coverage`/`design_tokens`/`resource_compile` تكفي لتعديلات الواجهة؛ `./gradlew :app:compileReleaseKotlin` فقط عند حاجة حقيقية (§0.1-3).
- لا أسرار في الملفات · push مسموح على `PanditJi-9/Xx` · لغات جديدة: EN + AR فقط (§0.2).

## ما أنجز (مقيس)
- `redlab/apps/gamespace/src/ported/java/` = **573 ملفًا** مملوكة ومُتحقَّق منها (git-tracked).
  التشغيل `44902867016` طبّق الإصلاحات الأربعة، فرفض `javac` **165** بدل 204 وقبل **39** وحدة جديدة
  في الجولة 16 (`Final javac state: clean`)؛ فثُبّتت وأصبح الإجمالي **573** (`SEALED.json` + `PORTED.md`).
  التثبيتات السابقة: 278 في `44860316090`، 534 في `44874120151`، وتصحيح كاذب في `44870917262` أُزيل في `b1fcf6c`.
- **خطأ تقرير صُحّح:** `port_frontier.py` كان يرتّب أسماء السجلات أبجديًّا فيقرأ الجولة 9 كـ«الأخيرة»
  ويكتب `BUILD FAILED` رغم أن الجولة 15 `BUILD SUCCESSFUL`. صار الترتيب حسب رقم الجولة، وأُعيد توليد
  `docs/port/FRONTIER.md` من سجلات `44874120151`؛ الاختبار الذاتي 16 حالة.
- `redlab/apps/gamespace/libraries.txt` = **74 إحداثية Maven** مقيسة من الـAPK، تُمرَّر إلى Gradle
  (`compileOnly`) وإلى `port_slice.py --libraries`؛ فانخفض `library not linked` من 448 إلى 11.
- `org.xmlpull.v1` (في android.jar) و`org.apache.http` (via `useLibrary`) صارا معروفين لـ`port_slice`،
  فلا يُرفض 6 ملفات وأتباعها خطأً.
- CI `redlab-port.yml`: `assembleDebug` BUILD SUCCESSFUL في `44898305585` (أول بناء يضم الـ534)،
  artifact APK 137,583,914 بايت مضغوط، `res/` 3536 · assets 54 · 3 ABI · `skipped=0`؛
  ثم مرّ `javac` على الـ204 غير المثبّتة ورفضها جميعًا (`units left: 0`) ولم يثبّت جديدًا.
- التشغيل `44904667579` على `1cf63f2`: `assembleDebug` **BUILD SUCCESSFUL** للـ573، ثم رفض `javac`
  الـ165 غير المثبّتة عبر 15 جولة، والجولة 16 نظفت الـ573، ولم يُثبّت جديد (`already sealed`).
  APK الـartifact 137,625,295 بايت مضغوط؛ لم يُنزَّل محليًا بسبب انقطاع الشبكة.
- **جهاز حقيقي متصل:** `adb connect 192.168.1.3:41325` → `24129RT7CC` (Android 16، `arm64-v8a`).
  ثُبّت APK أقدم (278 مختومة) وعملت `MainActivity`؛ الشاشة قرأت `res 3536 · assets 54 · 3 ABI`
  و28 activities/11 services/14 receivers/13 providers. هذا يثبت الموارد وقت التشغيل، **لا** الـ573
  ولا أي شاشة أصلية.
- **أصناف المكتبات صارت في الـAPK:** تحويل `AAR→classes.jar` باسم `portRuntime`، ثم
  `implementation(portRuntime.incoming.files)`؛ البناء المحلي `build15` أنتج APK بحجم **168MB و19 dex**
  بعد استبعاد `annotations` و`kotlin-stdlib(-jdk7/-jdk8/-common)` لتفادي التكرار مع Kotlin 2.3.10.
- **معرّفات الموارد مثبّتة:** `public.xml` الأصلي 12,633 مدخلًا؛ يُحوَّل إلى `--stable-ids` بمفاتيح
  مؤهّلة بالحزمة (`dev.redlab.gamespace:id/name = 0x...`). أُعيد الربط فصار
  `id/tgk_case_modify_name=0x7f0a0643` و`string/gcs_game_refresh_rate=0x7f1202b6` و
  `xml/gcs_game_refresh_rate=0x7f150006` مطابقة للأصل. `styleables.xml` يعيد 11 `declare-styleable`.
- **أول واجهة أصلية تعمل:** `TgkCaseRenameDialog` ظهر على الجهاز بعنوانه العربي وحقل `EditText`
  بلا crash، عبر `ProofActivity` (يعيد نوع النافذة إلى `TYPE_APPLICATION` قبل `show`).
- **`GameRefreshRateActivity` ميتة في الأصل نفسه على Android 16:** الانهيار `InflateException` →
  `Fragment$InstantiationException` يتكرر حرفيًا عند تشغيل `cn.nubia.gamelauncher`؛ لا تُصلَح ولا تُعلَن.
  `GameCenterActivity` تعمل في الأصل وتُظهر الإعدادات العربية، وهي الهدف التالي (سلسلة كبيرة).
- `redlab/apps/gameassist/` = shell يبني APK (main + strings)؛ materials + ported code مؤجَّلان.

## الخطوة التالية (الأولوية)
1. **أول نشاط أصلي كامل السلسلة**: الحوار أثبت التضخيم والموارد. الهدف التالي `GameCenterActivity`
   لأنها تعمل في الأصل وتُظهر الإعدادات العربية؛ أقِس سلسلتها (17 `Fragment` ومساعداتها)، وأضِف
   الناقص كوحدات مختومة، ولا تُعلنها في المانيفست قبل نجاح `am start` بلا crash.
2. **تثبيت عمل اليوم**: معرّفات `stable-ids`، و`portRuntime`، و`styleables.xml`، و`ProofActivity`
   موجودة محليًا فقط. راجع `git diff` ثم ادفعها في التزام واحد ليصبح CI مرجعًا مقيسًا؛ لا تُدفع
   `materials/` (gitignored) ولا أي نشاط يعتمد على كود غير متعقّب.
3. **جبهة الـ165 المرفوضة**: صنّف `com/zte/shared/wrapper` كواجهات Android مخفية بدل إعادة تغذيتها،
   وأغلِق سلاسل `Preference`/`AlertDialog` داخل الحزمة، وأصلح آثار jadx الحقيقية فقط بعد قراءة الملف.
4. **GameAssist**: staging للمواد + حلّ appcompat (`abc_search_view`/`edit_query`/`overflow_button`)
   إما بتبعية مع استبعاد الموارد المكرّرة أو بتشذيب أدنى للمراجع.
5. لا تنسَ: كل نص جديد في `values/` + `values-ar/` فقط (§0.2)، التثبيت add-only (لا حذف).

## أوامر مفيدة
```sh
gh workflow run redlab-port.yml -R PanditJi-9/Xx --ref main
gh run view <id> -R PanditJi-9/Xx --json status,conclusion
python3 tools/port_slice.py --sources work/port/GameSpace/sources --package cn.nubia.gamelauncher --app GameSpace --json work/port/GameSpace/slice.json
python3 tools/seal_ported.py --self-test
python3 tools/port_materials.py --self-test
```

## لا تفعل
- لا تُعد تثبيت (seal) أي شيء حُذف (add-only).
- لا تقليد شكل/أيقونات من تطبيقات مرجعية (§0.4).
- لا تدّعِ «١٠٠٪ تغطية» لغات (فقط EN/AR).
- لا تبني شاشة معدلة لمجرد الاطمئنان (§0.1).

## تحديث 2026-11-21 — طبقة الأوضاع الثلاثة (أول شريحة قابلة للقياس)

- أُضيف `ControlMode` (`ROOT`/`SHIZUKU`/`NONE`) و`ModeProbe` داخل `apps/gamespace`، ويُعرض الوضع
  في `MainActivity` بعد فحص غير متزامن. النصوص في `values/mode.xml` و`values-ar/mode.xml` فقط (§0.2).
- الفحص للقراءة فقط: Root يُقبل حين يعيد `su -c id` قيمة `uid=0` خلال 3 ثوانٍ؛ Shizuku يُقبل حين
  توجد حزمته **و** يعمل binder الخدمة `shizuku`؛ وإلا `NONE` (واجهة للقراءة).
- التحقق: `:apps:gamespace:compileDebugJavaWithJavac` = **success** (6 تحذيرات من كود الأصل فقط)،
  والمفاتيح الأربعة موجودة في `R.txt`. السلوك على جهاز لم يُقس بعد.
- التالي: واجهة `PrivilegedTransport` واحدة بتنفيذين (root/shizuku) تُمرَّر خلف `HardwareControlArbiter`،
  ثم شاشة قائمة الألعاب من تخطيطات الأصل. لا تُعلن أي كتابة قبل قياسها على الجهاز.

## تحديث 2026-11-21 (٢) — شاشة قائمة الألعاب من تخطيطات الأصل

- `GameListActivity`: تعيد استخدام `game_space_main` و`game_item_medium` الأصليين حرفيًا، وتُعبّئ البطاقة
  بأيقونة التطبيق واسمه من `queryIntentActivities(ACTION_MAIN/CATEGORY_LAUNCHER)` — بلا خدمة Nubia وبلا
  صلاحية حزم، فتعمل على أي هاتف. النقر يفتح التطبيق عبر `getLaunchIntentForPackage`.
- الزر في `MainActivity` من `rl_open_game_list` (EN + AR فقط).
- التحقق: `:apps:gamespace:compileDebugJavaWithJavac` = **success**. السلوك المرئي لم يُقس: لا جهاز متصل
  حاليًا (`adb devices` فارغ).
