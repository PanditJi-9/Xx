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
| `cn.nubia.gamelauncher` | `res/` + `assets/` + `lib/` كاملة | resource-set | — | `SCAFFOLDED` | تشغيل `44870917262` (BUILD SUCCESSFUL) · APK 152.8MB | 3536 موردًا · 54 أصلًا · 3 ABI؛ 41 اسمًا أعيد تسميته فقط، **skipped = 0**. تصحيح مقيس: الأربعة `raw/*.he|mp4` كانت تُنسخ وتُحزم فعلًا في `44850020107`، لكن تقرير `MATERIALS.md` كان يعدّها «مستبعدة» بقائمة امتدادات خاطئة؛ التقرير صُحّح ولم يُفقد أي مورد. |
| `cn.nubia.gamelauncher` | `dev/redlab/gamespace/MainActivity.java` | activity | 0 | `REBUILT` | نفس التشغيل | شاشة تقرأ `port/inventory.json`؛ تزول حين يُبنى الردهة الأصلي |
| `cn.nubia.gameassist` | `dev/redlab/gameassist/MainActivity.java` | activity | 0 | `SCAFFOLDED` | تشغيل `44865094295` (BUILD SUCCESSFUL) | shell: يبني APK (main + strings)؛ materials + ported code مؤجَّلان |

**لماذا `SCAFFOLDED` لا أعلى:** الموارد تُترجم وتُحزَّم، ولم يُقس سلوكها على جهاز. ولا صفّ يتجاوز
`SCAFFOLDED` بلا `evidence` جهاز.

**تحويل لازم سُجّل:** apktool يفكّ أجزاء AnimatedVectorDrawable بأسماء تبدأ بـ`$` يرفضها aapt2
(41 ملفًا)، فتُعاد تسميتها إلى `rl_*` وتُعاد كتابة مراجعها ميكانيكيًّا — لا سلوك تغيّر، والقائمة منشورة.

## 3.2 جبهة الترجمة — أول قياس للكود (تشغيل `44853528267`)

| المقياس | القيمة | الأداة |
| --- | --- | --- |
| ملفات `sources/` كلها | 6280 | `port_slice.py` |
| كود مكتبات مضمّنة (kotlin · androidx · gson …) | 5217 | نفسه — **لا يُبورت**، يُضاف كتبعية |
| كود التطبيق الأصلي | 1063 | نفسه |
| **قابل للترجمة الآن بلا `:compat`** | **404** | `port_slice.py` (إغلاق على الاستيرادات) |
| قَبِله المترجم فعلًا | **352** | `:apps:gamespace:compileDebugJavaWithJavac` |
| رفضه المترجم | 52 | 31 رمزًا داخليًّا · 8 `androidx.preference` · 1 `org.apache.http` |
| يحتاج تبعية (استيراد مكتبة) | 448 | `port_slice.py` |
| إطار مورّد (`cn.nubia.*` غير حزمته) | 7 | نفسه |
| يعتمد على وحدة مرفوضة | 197 | نفسه |

### 3.3 الكود صار ملكنا (تشغيل `44860316090`)

| المقياس | القيمة |
| --- | --- |
| وحدات مثبّتة في `apps/gamespace/src/ported/java/` | **278** (3.2 م.ب، متتبَّعة في git) |
| ترتيب التثبيت | `port_slice` ← `stage_sources` ← **javac يقبل** ← `seal_ported.py` |
| شجرة البناء | `:apps:gamespace:assembleDebug` = **BUILD SUCCESSFUL** بالمصادر المثبّتة |
| الجولة التالية | 126 وحدة باقية رفضها المترجم كلها ⇒ 0 مضافة، والتثبيت **إضافي فقط** |
| الوثيقة | `apps/gamespace/PORTED.md` + `SEALED.json` |

**درس مقيس:** التثبيت لا يحذف. في تشغيل `44859682746` كان `materials/sources` فارغًا (رفض المترجم
كل الباقي)، فحذف «التقليم» الـ278 المثبتة — فأُعيدت وأُزيل التقليم كليًّا.

**حدّ الأداة معلن:** الإغلاق على **الاستيرادات** لا على المراجع داخل الحزمة نفسها؛ ولهذا بقي 31 خطأ
«cannot find symbol» داخل وحدات مختارة. الخطوة التالية المقيسة: إغلاق على مستوى الحزمة، ثم إضافة
التبعيات الحقيقية (`androidx.preference`, `org.apache.http.legacy`).

### 3.4 التبعيات الحقيقية والجبهة المقيسة (تشغيل `44870917262`)

| المقياس | قبل (44860316090) | بعد | الدليل |
| --- | ---: | ---: | --- |
| وحدات مختارة بواسطة `port_slice` | 404 | **733** | `slice.json` + `--libraries` |
| مرفوضة | 5876 | 5547 | نفسه |
| `library not linked` | 448 | **11** | بقيت `com.android.*` (systemui/inputEventTool) وليست تبعية Maven |
| `depends on an excluded unit` | 197 | 301 | زادت لأن وحدات أكثر صارت مختارة |
| `missing library` | 7 | 11 | نفسه |
| وحدات مثبّتة بعد قبول `javac` | 278 | **278** (تصحيح — انظر أدناه) | `SEALED.json` |
| ملفات `res/` | 3536 | 3536 | لم يُفقد شيء؛ `skipped=0` |
| `assembleDebug` | success | success | APK 152,827,031 بايت |

- `redlab/apps/gamespace/libraries.txt` = 48 بادئة استيراد / 74 إحداثية Maven مقيسة من `META-INF` وdex،
  كلها `compileOnly` حتى لا تدمج مواردها وتُعيد تعارض `androidx.preference` المقيس.
- `docs/port/FRONTIER.md` يُولَّد من سجلات `javac-round-*.log` ويعرض أول خطأ لكل وحدة.
- **تصحيح مقيس (خطأ في خط الأنابيب):** الحلقة توقفت بعد 6 جولات بينما الجولة السادسة ما زالت فاشلة
  (200 سطر خطأ)، ثم ثبّتت `seal_ported.py` الوحدات الـ285 المتبقية رغم أن `javac` لم يقبلها. السبب:
  شرط `steps.ported.outcome == 'success'` لم يمنع التثبيت. أُزيلت الـ285 من `src/ported/java` (لم تكن
  مقبولة قط)، وأُعيد `SEALED.json` إلى 278. أُصلحت الحلقة: 40 جولة كحدّ أمان، وتتوقف فقط عند `rc=0`،
  والتثبيت مشروط بـ`clean=1`. التشغيل `44873094696` أثبت الحاجة: نظافة `javac` لم تتحقق حتى الجولة 12
  (خطأ حقيقي واحد في `ClipImageView` ورّث سلسلة)، ففشل الـrun عمدًا ولم يثبّت شيئًا.
- **تحذير صريح:** `compileOnly` يشتري الترجمة فقط؛ أصناف هذه المكتبات **ليست في الـAPK بعد**. أي شاشة
  تستدعيها وقت التشغيل ستنهار حتى تُضاف كـ`implementation` أو يُثبت وجودها في dex الأصلي.
- الجولة التالية: 170 وحدة رفضها المترجم (أولها سلاسل `Preference`/`AlertDialog`/`Anim3DHelper`)؛
  وإغلاق المراجع داخل الحزمة نفسها؛ ثم GameAssist materials + appcompat.

## 4. ما يُولَّد آليًا وما يُكتب بيد

| المحتوى | المصدر |
| --- | --- |
| قائمة الوحدات وأعداد المراجع | `tools/jadx_index.py` (⏳) |
| صفوف الموارد وأنواعها | `tools/apk_resources.py` + `tools/asset_parity.py` (⏳) |
| الحالة و`evidence` | تُكتب في الالتزام الذي ينفّذ الوحدة، وتمنعها بوابة `rebuild_ledger.py --assert` إن نقصت |
