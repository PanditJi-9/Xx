# NEXT_TASK_REDLAB — 2026-11-21 — كود نملكه ونطوّره

## السياق (لا تعيد اختراعه)
- المستودع: `/storage/emulated/0/red/repo` · remote `PanditJi-9/Xx` · branch `main`.
- العقد: `AGENTS.md` + `docs/ai/ENGINEERING-CONTRACT.md` + `docs/ai/VALIDATION.md` + `docs/ai/REVIEW.md`.
- لا بناء تلقائيًا (§0.1): بوابات `kt_balance`/`code_health`/`i18n_coverage`/`design_tokens`/`resource_compile` تكفي لتعديلات الواجهة؛ `./gradlew :app:compileReleaseKotlin` فقط عند حاجة حقيقية (§0.1-3).
- لا أسرار في الملفات · push مسموح على `PanditJi-9/Xx` · لغات جديدة: EN + AR فقط (§0.2).

## ما أنجز (مقيس)
- `redlab/apps/gamespace/src/ported/java/` = **278 ملفًا** مملوكة ومُتحقَّق منها (git-tracked).
  التشغيل `44870917262` اختار **733** وحدة (زيادة +329 عن 404) لكنه لم يثبّت منها شيئًا بعد:
  الحلقة توقفت بعد 6 جولات والجولة السادسة فاشلة، ثم ثبّتت الأداة 285 وحدة غير مقبولة — عُدّلت
  الحلقة (40 جولة كحدّ أمان، توقف عند `rc=0`، تثبيت مشروط بـ`clean=1`) وأُزيلت الـ285. التشغيل
  `44873094696` فشل عمدًا ولم يثبّت شيئًا لأن الجولة 12 ما زالت فيها خطأ حقيقي؛ العدد الحقيقي
  يُقاس في التشغيل التالي.
- `redlab/apps/gamespace/libraries.txt` = **74 إحداثية Maven** مقيسة من الـAPK، تُمرَّر إلى Gradle
  (`compileOnly`) وإلى `port_slice.py --libraries`؛ فانخفض `library not linked` من 448 إلى 11.
- `org.xmlpull.v1` (في android.jar) و`org.apache.http` (via `useLibrary`) صارا معروفين لـ`port_slice`،
  فلا يُرفض 6 ملفات وأتباعها خطأً.
- `docs/port/FRONTIER.md` يُولَّد آليًا من جولات `javac` المحفوظة، ويعرض أول خطأ لكل وحدة.
- CI `redlab-port.yml`: `assembleDebug` BUILD SUCCESSFUL في `44870917262` قبل التثبيت، APK 152.8MB،
  `res/` 3536 · assets 54 · 3 ABI · `skipped=0`.
- `redlab/apps/gameassist/` = shell يبني APK (main + strings)؛ materials + ported code مؤجَّلان.
- `docs/MASTER_PLAN.md §10` و`docs/REBUILD_LEDGER.md §3.4` محدّثان.

## الخطوة التالية (الأولوية)
1. **شغّل CI وتحقق من نظافة `javac`**: لا تعلن أي عدد مثبّت قبل `clean after round N`. إن لم ينظف
   خلال 12 جولة، فالـrun يفشل عمدًا ويُرفع `FRONTIER.md` بالسبب.
2. **جبهة الـ170 المرفوضة**: أغلِق المراجع داخل الحزمة نفسها (سلاسل `Preference`/`AlertDialog`/
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
