# NEXT_TASK_REDLAB — 2026-11-21 — كود نملكه ونطوّره

## السياق (لا تعيد اختراعه)
- المستودع: `/storage/emulated/0/red/repo` · remote `PanditJi-9/Xx` · branch `main`.
- العقد: `AGENTS.md` + `docs/ai/ENGINEERING-CONTRACT.md` + `docs/ai/VALIDATION.md` + `docs/ai/REVIEW.md`.
- لا بناء تلقائيًا (§0.1): بوابات `kt_balance`/`code_health`/`i18n_coverage`/`design_tokens`/`resource_compile` تكفي لتعديلات الواجهة؛ `./gradlew :app:compileReleaseKotlin` فقط عند حاجة حقيقية (§0.1-3).
- لا أسرار في الملفات · push مسموح على `PanditJi-9/Xx` · لغات جديدة: EN + AR فقط (§0.2).

## ما أنجز (مقيس)
- `redlab/apps/gamespace/src/ported/java/` = **534 ملفًا** مملوكة ومُتحقَّق منها (git-tracked).
  التشغيل `44874120151` اختار **738** وحدة، رفض `javac` منها **204** عبر 15 جولة، ثم قبل **256** وحدة
  في الجولة 15 (`BUILD SUCCESSFUL`)؛ ثُبّتت الـ256 فأصبح الإجمالي **534** (`SEALED.json` + `PORTED.md`).
  التثبيتات السابقة: 278 في `44860316090`، ثم تصحيح كاذب في `44870917262` أُزيل في `b1fcf6c`.
- **خطأ تقرير صُحّح:** `port_frontier.py` كان يرتّب أسماء السجلات أبجديًّا فيقرأ الجولة 9 كـ«الأخيرة»
  ويكتب `BUILD FAILED` رغم أن الجولة 15 `BUILD SUCCESSFUL`. صار الترتيب حسب رقم الجولة، وأُعيد توليد
  `docs/port/FRONTIER.md` من سجلات `44874120151`؛ الاختبار الذاتي 16 حالة.
- `redlab/apps/gamespace/libraries.txt` = **74 إحداثية Maven** مقيسة من الـAPK، تُمرَّر إلى Gradle
  (`compileOnly`) وإلى `port_slice.py --libraries`؛ فانخفض `library not linked` من 448 إلى 11.
- `org.xmlpull.v1` (في android.jar) و`org.apache.http` (via `useLibrary`) صارا معروفين لـ`port_slice`،
  فلا يُرفض 6 ملفات وأتباعها خطأً.
- CI `redlab-port.yml`: `assembleDebug` BUILD SUCCESSFUL في `44874120151`، APK 152.8MB،
  `res/` 3536 · assets 54 · 3 ABI · `skipped=0` — لكن هذا الـAPK بُني **قبل** تثبيت الـ256،
  فالتشغيل التالي هو أول بناء يضم الـ534.
- `redlab/apps/gameassist/` = shell يبني APK (main + strings)؛ materials + ported code مؤجَّلان.

## الخطوة التالية (الأولوية)
1. **أعِد البناء بـ534 مثبّتة وتحقّق من الـAPK**: شغّل CI؛ المطلوب `assembleDebug` أخضر بعد التثبيت،
   و`javac` نظيف، وألّا يزيد عدد مثبّت فوق 534 بلا قبول صريح.
2. **جبهة الـ204 المرفوضة**: أغلِق المراجع داخل الحزمة نفسها (سلاسل `Preference`/`AlertDialog`/
   `Anim3DHelper`/`ToastUtil`)؛ أصلح آثار jadx الحقيقية فقط بعد قراءة الملف.
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
