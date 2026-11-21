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
- `redlab/apps/gameassist/` = shell يبني APK (main + strings)؛ materials + ported code مؤجَّلان.

## الخطوة التالية (الأولوية)
1. **ثبّت بناء الـ573**: شغّل CI بلا تغيير كود؛ يجب أن يبني `assembleDebug` الـ573، ثم يمرّ `javac`
   على الـ165 المتبقية ويفشل في تثبيت أي جديد (`already sealed`). لا تعلن الشاشات عاملة قبل ذلك.
2. **جبهة الـ165 المرفوضة**: صنّف `com/zte/shared/wrapper` كواجهات Android مخفية بدل إعادة تغذيتها،
   وأغلِق سلاسل `Preference`/`AlertDialog` داخل الحزمة، وأصلح آثار jadx الحقيقية فقط بعد قراءة الملف.
3. **وقت التشغيل**: `compileOnly` لا يضع الأصناف في الـAPK. قبل تشغيل أي شاشة أصلية، أثبت وجود
   الصنف في `classes.dex` أو أضف المكتبة كـ`implementation` مع استبعاد مواردها المتعارضة.
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
