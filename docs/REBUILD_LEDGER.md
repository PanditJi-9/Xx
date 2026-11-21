# REBUILD_LEDGER — دفتر إعادة البناء (ملف بملف)

> **لماذا هذا الملف موجود:** الأمر الحاكم يطلب إعادة بناء **كل** ملف وClass ومكوّن ومورد، ومنع الادّعاء
> قبل البناء والاختبار. بلا دفتر، يصبح «أُعيد بناؤه» رأيًا. هذا الملف هو السجل الوحيد للحالة.
>
> **الحالة الحالية: لا صفوف حقيقية بعد.** الجدول أدناه يوضّح الشكل فقط. أول صفّ يُكتب عندما يُبنى
> `tools/jadx_index.py` و`tools/rebuild_ledger.py` (§4.1 من `MASTER_PLAN.md`) — لأن الصفوف تُولَّد من
> الفهرس، لا تُكتب باليد، وإلا صارت أمنيات.

---

## 1. الأعمدة

| العمود | معناه |
| --- | --- |
| `app` | الحزمة الأصلية (`cn.nubia.gamelauncher` …) |
| `unit` | مسار الوحدة في الأصل (`cn/nubia/gamelauncher/activity/GameSpaceActivity.java`) |
| `kind` | `class` · `activity` · `service` · `receiver` · `provider` · `layout` · `drawable` · `anim` · `raw` · `string-set` |
| `origin_refs` | عدد مراجع `cn.nubia.*`/`com.zte.*` التي يستعملها (يحدّد عمل `:compat`) |
| `status` | سلّم §8: `INVENTORIED` → `SCAFFOLDED` → `REBUILT` → `TESTED_HOST` → `TESTED_DEVICE` → `PARITY` |
| `evidence` | رابط تشغيل CI أو سجل جهاز أو بصمة مقارنة — **فارغ = لا ادّعاء** |
| `note` | سبب التأجيل أو الاستثناء (`UNREACHABLE` بدليل، `UNSUPPORTED` بسبب) |

## 2. قواعد لا تُخترق

1. **صفّ بلا `evidence` لا يتجاوز `SCAFFOLDED`.** البناء الأخضر يثبت الترجمة فقط.
2. **`UNREACHABLE` يحتاج دليلًا** (لا مرجع من أي مكوّن معلن ولا من أي أثر تشغيل) — لا «يبدو ميتًا».
3. **`UNSUPPORTED` يحتاج سببًا حرفيًا** من قياس (`SysfsDiscovery`/`GameManager`/`SELinux`)، لا تقديرًا.
4. **لا حذف صفّ.** ما يُلغى يُعلَّم `DROPPED` مع سببه؛ الحذف الصامت هو ما يسمح بالادّعاء الكاذب.
5. **عدّادات الإغلاق** تُحسب من هذا الملف: كم `REBUILT`، كم `TESTED_DEVICE`، كم بلا عقد توافق.

## 3. شكل الصفّ (مثال توضيحي — ليس إنجازًا)

| app | unit | kind | origin_refs | status | evidence | note |
| --- | --- | --- | --- | --- | --- | --- |
| `cn.nubia.gamelauncher` | `.../activity/GameSpaceActivity.java` | activity | 37 | `INVENTORIED` | — | بانتظار `jadx_index` |

## 3.1 أول صفّ حقيقي (P1 — مقيس)

| app | unit | kind | origin_refs | status | evidence | note |
| --- | --- | --- | --- | --- | --- | --- |
| `cn.nubia.gamelauncher` | `res/` + `assets/` + `lib/` كاملة | resource-set | — | `SCAFFOLDED` | تشغيل `44850020107` (BUILD SUCCESSFUL) · APK 130MB | 3536 موردًا · 54 أصلًا · 3 ABI؛ 41 اسمًا أعيد تسميته، 4 استُبعدت — القائمة في `MATERIALS.md` المرفوعة مع الـAPK |
| `cn.nubia.gamelauncher` | `dev/redlab/gamespace/MainActivity.java` | activity | 0 | `REBUILT` | نفس التشغيل | شاشة تقرأ `port/inventory.json`؛ تزول حين يُبنى الردهة الأصلي |

**لماذا `SCAFFOLDED` لا أعلى:** الموارد تُترجم وتُحزَّم، ولم يُقس سلوكها على جهاز. ولا صفّ يتجاوز
`SCAFFOLDED` بلا `evidence` جهاز.

**تحويل لازم سُجّل:** apktool يفكّ أجزاء AnimatedVectorDrawable بأسماء تبدأ بـ`$` يرفضها aapt2
(41 ملفًا)، فتُعاد تسميتها إلى `rl_*` وتُعاد كتابة مراجعها ميكانيكيًّا — لا سلوك تغيّر، والقائمة منشورة.

## 4. ما يُولَّد آليًا وما يُكتب بيد

| المحتوى | المصدر |
| --- | --- |
| قائمة الوحدات وأعداد المراجع | `tools/jadx_index.py` (⏳) |
| صفوف الموارد وأنواعها | `tools/apk_resources.py` + `tools/asset_parity.py` (⏳) |
| الحالة و`evidence` | تُكتب في الالتزام الذي ينفّذ الوحدة، وتمنعها بوابة `rebuild_ledger.py --assert` إن نقصت |
