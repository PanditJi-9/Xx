# أطلس — القديم ↔ الجديد

> **ما هذه الوثيقة:** مقارنة قياس بين منظومة Atlas قبل موجة `ATLAS-SELFHEAL-01` وبعدها.
> كل رقم هنا مشتقّ من `git` ومن قراءة الكود الحالي مباشرةً — لا من ذاكرة ولا من تخمين. وأوامر إعادة الاشتقاق في §١٤.
> **اللغة:** لا وصف لسلوك لم يُقَس: ما يحتاج مُصرّفًا أو جهازًا مُعلَن صراحةً في §١٣.

| البند | القيمة |
| --- | --- |
| **القديم (خطّ الأساس)** | `16ce7f43` — 2026-10-12 12:43 (+0300) · آخر إيداع لمس `core/atlas` قبل الموجة |
| **الجديد (المقيس)** | `91f6c049` — 2026-10-16 21:48 (+0300) · الرأس، شجرة نظيفة لحظة القياس |
| **إيداعات الموجة** | `26009f99` (S4+S1 · 16 ملفًا · +1714/−45) · `0fc82f69` (S2 · ملفّات Atlas وحدها: 5 ملفّات · 953 سطرًا، ضمن إيداع أوسع) · `91f6c049` (تصحيحات + توثيق) |
| **النطاق** | `manager/app/src/main/java/nd/max/core/atlas/**` + ربطه في `ui/viewmodel/AtlasViewModel.kt` و`ui/mainscreens/AtlasDiagnosticsSection.kt` + نصوص `max_screen_strings.xml` (en+ar) |
| **الطريقة** | فرق الإيداعات · إحصاء ملفّات/أسطر بين الأساسين · قراءة الملفّات الستة الجديدة كاملةً · فحوص موضعية (`grep`) · **ولا بناء أُجري لهذه الوثيقة** |

---

## ١) الخلاصة

Atlas قبل الموجة كان **دورة قياس مكتملة** (استكشاف → فهم → خريطة → تكييف → تنفيذ → إثبات → تذكّر) بثلاث فجوات معلنة:

1. **لا مُنتِج دليل داخل التطبيق** — كل قالب كان بأصل `HOST` كتبه اختبار؛ `AtlasDoctor` موجود في `core/diagnostics` لكن **بلا مستدعٍ في مسار التطبيق** (كل ذكره في المصدر: تعريفه + توثيق — مقيس)، ولهذا بقيت «كل ادّعاءات التوافق تنتهي بـ: تحتاج هاتفًا».
2. **لا تمييز بين رفض متوقّع ورفض مكتشَف** — فلا نموذج يفرّق بين «المنصّة لا تمنح هذا لأي تطبيق» و«شيء ما تغيّر هنا».
3. **كل شيء بفتح الشاشة** — لا مسار صامت يعالج انحرافًا مُتحقَّقًا (G-UX-01/G-UX-02 في مراجعة الفجوات).

والموجة أغلقت الثلاثة **بإضافة طبقات فوق الدورة، لا باستبدالها**: محذوفات `core/atlas` بين الأساسين = **صفر** (مقيس)، ولم يتغيّر مالك أي مرحلة (§١٢). الجديد كله **إضافي وقابل للعزل**، وقد صُنِّفت الملفّات النقيّة الجديدة تحت حرس معماري يمنعها من مسار القراءة.

| المحور | القديم `16ce7f43` | الجديد `91f6c049` |
| --- | --- | --- |
| **دليل الجهاز (S4)** | لا مسار في التطبيق: `HOST` فقط من الاختبارات | التقاط من الهاتف بلمسة → ملفّ واحد → مشاركة يدوية عبر منتقي النظام؛ بلا رفع |
| **التوفّر والعيوب (S1)** | لا تصنيف ولا قواعد | `AtlasAvailability` (٣ حالات · خفض فقط) + ٩ أسطح منصّية مغلقة بدليل لكل سطح + `AtlasQuirk` (٤ تأثيرات · ٤ مدخلات بمنشأ) |
| **المنع قبل المحاولة (S1)** | لا يوجد | `blockedReasonOf` في المُجدوِل: تخطٍّ **مُعلَن** برمز `expected-denied:<rule>` بدل محاولة تُسجَّل فشلًا وتُضيف ضجيجًا |
| **الإصلاح الصامت (S2)** | كل شيء بفتح الشاشة | نواة قرار + تنسيق كاملة (٤ أحداث · ٦ أصوات رفض · ٤ نتائج · قفل واحد) — **والربط بالعتاد لم يُنفَّذ بعد (معلَن)** |
| **الكشف لا التنفيذ (S3)** | موجود سلفًا | **لم يتغيّر** — `AtlasFailureRetryPolicy` و`suppressionFor` موجودان في `16ce7f43` (مقيس)، ولم تُضَف طبقة موازية |

---

## ٢) الأرقام المقيسة

| القياس | القديم `16ce7f43` | الجديد `91f6c049` | الفرق |
| --- | --- | --- | --- |
| ملفّات `core/atlas` (مصدر) | 24 | 30 | **+6 ملفّات** |
| أسطرها | 7,021 | 8,284 | **+1,263** — منها 1,180 = الملفّات الستة الجديدة وحدها |
| أصناف اختبار Atlas (+ أدوات دعم) | 21 (+5) | 26 (+5) | **+5 أصناف** |
| أسطر الاختبارات | 6,137 | 7,159 | **+1,022** |

لا تشمل الأرقام `ui/**` و`res/**` — وإضافاتهما في §٨.

---

## ٣) أين تقع هذه الموجة في عمر Atlas

| التاريخ | الإيداعات | ماذا أضافت (موجز) |
| --- | --- | --- |
| 2026-09-20 → 09-26 | جولات التأسيس الأولى | كل ما يعود إليه §١٢ («ما لم يتغيّر»): الاستكشاف، الهوية، الخريطة، السلامة، القوالب، الطزاجة، السجل السلبي، البنك — تفاصيل كل جولة في `HANDOFF` الخاص بها |
| 2026-10-04 | `9402245f` | فصل حالة القدرة: `AtlasCapabilityState.kt` **جديد** (‎+22) واقتطاع مُقابِل من `AtlasCapabilityMap` (‎−22) — في إيداع مستودعي ضخم لا يخصّ Atlas وحده |
| 2026-10-12 | `f476fd7f` · `a9f034e6` · `16ce7f43` | الكتالوج (‎+150 سطرًا) · خريطة القدرة · ثم `MaxAtlas` — **وهنا يقف «القديم»** |
| 2026-10-16 | `26009f99` · `0fc82f69` · `91f6c049` | **موجة `ATLAS-SELFHEAL-01`** — موضوع هذه الوثيقة |

---

## ٤) دليل الجهاز (S4): من «الاختبارات فقط» إلى «يُقرأ من الهاتف»

### قبل
- لا مسار إنتاجي ينتج قالبًا من الجهاز: الطبيب `AtlasDoctor` دالّته الوحيدة (`compare`) لا يستدعيها شيء في مسار التطبيق — اختبارها `AtlasDoctorTest` وحده؛ وكل التقاط كان بأصل `HOST` من اختبار.
- النتيجة التي تسمّيها وثائق الكود نفسها: منظومة إعادة التشغيل حقيقية لكن **غير قابلة للوصول** من الهاتف.

### بعد

| الملفّ | الحجم | الدور (من الكود ووثائقه) |
| --- | --- | --- |
| `AtlasRecordingTransport.kt` (جديد) | 131 سطرًا | زخرفة `AtlasReadTransport`: تمرّر كل جواب كما هو، والجواب الذي لا يسعه التنسيق **يُعدّ ولا يُرمى** (`unrecordable`)، والأصل افتراضه `DEVICE` — والاختبارات وحدها تستعمل `HOST` «حتى لا يُستشهد بالقالب لاحقًا كدليل على هاتف» |
| `AtlasFixtureStore.kt` (جديد) | 137 سطرًا | مخزن الجهاز: مجلد `atlas-fixtures` في `cacheDir` (لا تنسخه النسخ الاحتياطي)، كتابة ذرّية (`tmp` → rename)، حدّ `AtlasFixture.MAX_BYTES` قبل الكتابة، تنظيف بالعمر (افتراضيًا ٧ أيام) مع حماية الملفّ النشط — **ولا رفع تلقائي في أي مكان** |
| `AtlasFixtureRecorder.kt` (معدَّل) | 104 أسطر متغيّرة | أُعيد تشكيل البوابة (`throughGate`) وأُضيفت `recordRead` / `recordListing` / `recordResolution` — ترجمة جواب النقل إلى جواب قالب |
| `AtlasDiscovery.kt` (معدَّل) | 24 سطرًا متغيّرًا | `newJob(..., readTransport: AtlasReadTransport = transport)` — الشرط نفسه بنقل مختلف، بلا فرع موازٍ |
| `AtlasViewModel.kt` (معدَّل) | 177 سطرًا متغيّرًا | `captureDeviceEvidence()`: ترفض الجولة غير المنتهية/الملغاة/المحدودة بحدّ («قاعدة الطبيب الأولى عند المصدر»)، تُسلّح التسجيل وتُشغّل الجولة نفسها. `finishCapture()`: يفرّغ `recording` أولًا (فلا كتابة ثانية لنفس الحالة النهائية)، يُشفّر `AtlasFixtureCodec`، **ثم يفكّ ترميز ما كتبه** — الملفّ يفحص نفسه — ثم يُخزّن. و`shareEvidence()` / `consumeEvidenceRequest()`: طلب مشاركة أحادي المرّة |
| `AtlasDiagnosticsSection.kt` (معدَّل) | 119 سطرًا متغيّرًا | صفّ الالتقاط في شاشة التشخيص: `onCapture` + `onShareEvidence`، و`LaunchedEffect` يسلّم الملفّ لمنتقي النظام عند الطلب |
| نصوص `max_screen_strings.xml` (en + ar) | ‎+6 / +6 | `max_atlas_capture_title` · `_desc` · `_done_title` · `_done_detail` (‎%1$s/%2$s/%3$s) · `_share` · `_blocked_title` |

وملخّص `AtlasCapture` (سطر 103): `origin` · `values` · `failures` · `listings` · `resolutions` · `skipped` · `truncated` · `unrecordable` · `replayedBack` — «عدّادات لا حكم: الالتقاط يقول كم جوابًا سُجّل، ولا يقول إن الجهاز مدعوم».

---

## ٥) التوفّر والرفض المتوقّع والعيوب (S1)

### قبل
- لا تصنيف للتوقّع: رفض `/proc/stat` ورفض عقدة شريحة بعينها شيء واحد في السجل.
- لا قواعد عيوب (Quirks): «يعمل هنا ولا يعمل هناك» معرفة مشتّتة.
- رمز `expected-denied` غير موجود — أُضيف في الموجة (مقيس من الفرق).

### بعد — الطبقات الأربع

**(أ) محور التوقّع — `AtlasAvailability.kt` (196 سطرًا، جديد):**
- `EXPECTED` (ما يمنحه كل بناء) · `DEVICE_DEPENDENT` (الحقيقة لمعظم واجهات الشرائح) · `EXPECTED_DENIED` (المنصّة لا تمنحه لأي تطبيق).
- الطيّ **حدٌّ أدنى** (`strictestOf`): لا قيد ولا عيب ولا كتالوج **يرفع** حالةً على هذا المحور — الخفض فقط، بنيويًّا.
- الأسطح المغلقة لا تكون `EXPECTED` (قاعدة نوعية).

**(ب) الأسطح المغلقة — `AtlasClosedSurfaces.ALL`: ٩ أسطح، ولكل سطح دليل قاعدة رفض منصّية في حقله `evidence`:**

| المعرّف | المسارات | لماذا مغلق |
| --- | --- | --- |
| `R-01` | `/sys/fs/cgroup` | كل ملفّات cgroup ممنوعة (neverallow) على التطبيقات غير الموثوقة — لا قراءة ضغط/ذاكرة لكل تطبيق |
| `R-02` | `/sys/kernel/debug` | debugfs ممنوع قراءةً على التطبيقات؛ يُكتب فيه وسَم التتبّع وحده |
| `R-04` | `/proc/stat` · `/proc/uptime` · `/proc/version` · `/proc/vmstat` · `/proc/swaps` · `/proc/loadavg` | تسميات `/proc` العامة ممنوعة؛ والبديل من واجهات المنصّة العامة (مثل `ActivityManager` لمعلومات العمليات) |
| `R-05` | `/sys/fs/selinux` | حالة SELinux لا تُقرأ من تطبيق؛ «متساهل» يُستدلّ من **نمط الرفض**، ولا يُشاهد من هنا |

**(ج) العيوب — `AtlasQuirk.kt` (282 سطرًا، جديد):**
- أربعة آثار: `ExpectDenied` · `ExpectAbsent` · `UnitUnestablished` · `PreferSource` — **كلها تسهم بـ`DEVICE_DEPENDENT`**: بنيويًّا لا يستطيع عيب أن يرفع ادّعاءً.
- القاعدة: **العيب لا يمنع محاولة أبدًا** — يقول «لا تعتبر هذا عطبًا»، لا «لن يعمل هذا». المنع محصور بالإغلاق المنصّي المُدقَّق (التالي).
- أربع مدخلات في `AtlasQuirkBase` (مقيسة): `devfreq-search-denied-app-context` · `power-supply-search-denied-app-context` · `gpu-busy-percentage-unreadable-app-context` · `soc-machine-node-denied-app-context`.
- المنشأ جزء من النوع: مطابقة ضيّقة ترفض الفارغ ودليل لازم — عيبٌ بلا منشأ لا يُبنى أصلًا.

**(د) المنع قبل المحاولة — `AtlasProbeSchedule.kt` (+23) و`AtlasDiscovery.kt` (الربط):**
- `AtlasSkipReasons.EXPECTED_DENIED = "expected-denied"`، و`blockedReasonOf: (String) -> String?` **دالّة محقونة، لا حقلًا على المسبار** — بتعليقه: «قصد التصميم يجب ألّا يستطيع جعل مسبار يبدو مقيسًا».
- الربط: `AtlasClosedSurfaces.match(path)?.let { "${AtlasSkipReasons.EXPECTED_DENIED}:${it.id}" }`.
- النتيجة: تخطٍّ **مُعلَن** في `AtlasProbePlan.skipped` (مثل `expected-denied:R-04`) — قرار مرئي لا صمت — ووفرة: المحاولة كانت ستكلّف على بعض الأنوية سطر `avc: denied` في سجل النظام بلا فائدة.

---

## ٦) الإصلاح الصامت (S2): النواة كاملة، والربط معلَن أنه لم يُنفَّذ

### قبل
- لا كاشف حيّ ولا مسار صامت؛ كل معالجة تبدأ بفتح الشاشة (G-UX-01: «أداة تشخيص لا خدمة»؛ G-UX-02: «لا lifecycle صامت»).

### بعد

**السياسة النقيّة — `AtlasQuietHealing.kt` (305 أسطر، جديد):** بلا I/O ولا ساعة ذاتية ولا Android.
- الأحداث أربعة: `OwnershipDrift` (يحمل `liveValue`/`expectedValue` محدودين بـ128 حرفًا) · `ForegroundChanged` · `ControlIntent` · `GenerationChanged`.
- أصوات الرفض ستّة، وبترتيب بوابات واحد ثابت (مقيس): `HALTED → NOT_PROVEN → NO_EXPECTED_VALUE → IN_FLIGHT → RATE_LIMITED` — و`ForegroundChanged`/`ControlIntent` خارج جيل الفعل تُرفض `NOT_ACTIONABLE` (والأولى حتى يُربط مخزن الأنماط).
- `reduce` هو **الكاتب الوحيد** لخانة التصعيد، ونجاح يمحو تصعيد **نفس الهدف** فقط؛ `regenerate` (تغيّر جيل) يمحو السلامة والمخاطر ويُبقي ذاكرة الوتيرة؛ `acknowledge` يرفع الإيقاف ويُبقي العدّاد.
- الثوابت المعلنة: `REPAIR_MIN_INTERVAL_MS = 60_000` · `ESCALATE_AFTER_BLOCKED = 3` · `ESCALATE_AFTER_ROLLBACK = 2` — و`PROVENANCE = "design values; unmeasured; ATLAS-SELFHEAL-01 S2"`.

**التنسيق — `AtlasSessionCoordinator.kt` (129 سطرًا، جديد):**
- قفل واحد؛ القرار **ووسم `inFlight` في القفل نفسه** (فلا إصلاحان لهدف واحد أبدًا)، والنداء **خارجه**؛ حدثٌ يعود من داخل الإصلاح يُرفض `IN_FLIGHT`؛ ومن رمى يُطوى `UNKNOWN_STATE` (يوقف الهدف)؛ و`GenerationChanged` يُعيد التهيئة ولا يُصلح.
- **منفذ الكتابة الوحيد واجهة محقونة**: `AtlasQuietRepair` — لا شيء غيرها يمسّ جهازًا. وتعيين النتيجة إلى `HardwareRepairState` الفعلي: `CONFIRMED_WINDOW → VERIFIED` · `APPLY_FAILED_ROLLED_BACK` / `DRIFT_ROLLED_BACK → NOT_TAKEN_ROLLED_BACK` · `BLOCKED → BLOCKED` · `ROLLBACK_UNVERIFIED → UNKNOWN_STATE` (وأسماء الحالة صُحِّحت في `91f6c049` لتطابق التعدّد الفعلي مقيسًا).

**«المُثبَت» ليس بوابة موازية:** `provenFrom` يقرأ `AtlasRouteMemory.preferredRoute(target.name.lowercase())` — **نفس مفتاح `MaxAtlas`** (مقيس: `MaxAtlas.kt:302`)، ويُعيد التقييم مع كل حدث.

**الحدّ المعلن:** لا DI ولا مستدعٍ إنتاجي بعد؛ الربط الآتي يملأ الواجهة حصرًا عبر مسار `HardwareRepairExecutor → HardwareControlArbiter` — خطوة جهاز + مراجعة مستقلة، وهي معلنة في `NEXT_TASK`.

---

## ٧) الكشف لا التنفيذ (S3): سبق الموجة (مقيس)

- في `16ce7f43` نفسه: `AtlasFailureRetryPolicy` في `AtlasFailureLedger.kt` (سطر 25) ويُستعمل في `AtlasRepository` (سطور 166/212)، و`suppressionFor` (سطور 235/274)، وخانة الاستبعاد في المسبار (`suppressionOf`).
- فـ S3 **ليست من إضافات الموجة**، والموجة لم تكرّرها: لا `RetryPolicy` ثانية ولا سجلّ سلبي موازٍ.

---

## ٨) جدول الملفّات

**الجديدة (٦ ملفّات · 1,180 سطرًا):**

| الملفّ | الأسطر | الإيداع |
| --- | --- | --- |
| `AtlasAvailability.kt` | 196 | `26009f99` |
| `AtlasQuirk.kt` | 282 | `26009f99` |
| `AtlasRecordingTransport.kt` | 131 | `26009f99` |
| `AtlasFixtureStore.kt` | 137 | `26009f99` |
| `AtlasQuietHealing.kt` | 305 | `0fc82f69` |
| `AtlasSessionCoordinator.kt` | 129 | `0fc82f69` — ومُرِّر تعليقه في `91f6c049` |

**المعدَّلة في `core/atlas`:** `AtlasDiscovery.kt` (24) · `AtlasFixtureRecorder.kt` (104) · `AtlasProbeSchedule.kt` (+23) — كلها في `26009f99`.

**خارج `core/atlas`:**

| الملفّ | الفرق | الإيداع |
| --- | --- | --- |
| `ui/viewmodel/AtlasViewModel.kt` | 177 سطرًا متغيّرًا | `26009f99` |
| `ui/mainscreens/AtlasDiagnosticsSection.kt` | 119 سطرًا متغيّرًا | `26009f99` |
| `res/values/max_screen_strings.xml` + `values-ar/…` | ‎+6 / +6 | `26009f99` |
| `test/.../AtlasArchitectureTest.kt` | ‎+13 ثم +9 | `26009f99` · `0fc82f69` |
| `test/.../ui/viewmodel/AtlasPresentationTest.kt` | ‎+53 | `26009f99` |
| `test/.../AtlasFreshnessTest.kt` | 7 أسطر متغيّرة | `91f6c049` (إصلاح ربط تسمية من `CI-FIX-02`) |

**الإيداعات الثلاثة كاملةً (للسياق):** `26009f99` = 16 ملفًا · ‎+1714/−45 (يشمل Atlas + الواجهة + النصوص + الاختبارات) · `0fc82f69` = 25 ملفًا · ‎+1251/−266 (وملفّات Atlas وحدها 5 ملفّات · 953 سطرًا؛ والباقي توثيق وأعمال مستودعية خارج مقارنة Atlas) · `91f6c049` = 8 ملفّات (منها إغلاق تعليق `AtlasSessionCoordinator` وإصلاح `AtlasFreshnessTest`، والبقيّة وثائق).

---

## ٩) الواجهات والعقود — قبل ↔ بعد

| الموضع | قبل | بعد |
| --- | --- | --- |
| `AtlasProbeSchedule.plan(...)` | آخر وسيط: `suppressionOf: (String) -> AtlasSuppression? = { null }` | أُضيف آخرًا: `blockedReasonOf: (String) -> String? = { null }` — وهو بالضبط ما كسر الربط الذيلي في `AtlasFreshnessTest` (اللامبدا القديمة التصقت بالوسيط الجديد)؛ أُصلح بتسمية `suppressionOf = { … }` (`CI-FIX-02`) |
| `AtlasDiscovery.newJob(...)` | بلا نقل مخصّص | `newJob(..., readTransport: AtlasReadTransport = transport)` |
| `AtlasFixtureRecorder` | بوابة قبول + دوالّ قديمة | بوابة `throughGate` + `recordRead` / `recordListing` / `recordResolution` |
| `AtlasViewModel` | لا التقاط | `captureDeviceEvidence()` · `shareEvidence()` · `consumeEvidenceRequest()` + حالة (`capture` · `captureRefusal` · `evidenceFile` · `shareEvidenceRequest`) و`finishCapture()` خاصّة |
| رموز التخطّي | `NOT_DUE` · `OVER_BUDGET` · استبعاد | ‎+ `AtlasSkipReasons.EXPECTED_DENIED = "expected-denied"` |
| الإصلاح الصامت | لا يوجد | `AtlasQuietHealing.decide/reduce/regenerate/acknowledge` · `AtlasSessionCoordinator.onEvent` · `AtlasQuietRepair` (واجهة حقن) |

---

## ١٠) سلوك — سيناريوهات (قديم ↔ جديد)

| السيناريو | القديم | الجديد |
| --- | --- | --- |
| قراءة `/proc/stat` أو `cgroup` | تُجرَّب وتُسجَّل نتيجتها كأي رفض — بلا معرفة أن المنصّة تغلقها دائمًا، وقد تُضاف سطور `avc: denied` بلا فائدة | تُمنع قبل المحاولة، ويُسجَّل `expected-denied:R-04` — تخطٍّ مرئي بقرار لا صمت |
| رفض قراءة عقدة شريحة في سياق التطبيق | يُسجَّل رفضًا عاديًّا | عيب مُصنَّف **يخفض الادّعاء** إلى `DEVICE_DEPENDENT` — وبلا منع للمحاولة: العيب لا يقول «لن يعمل» |
| انحراف قيمة كانت مُتحقَّقة | لا شيء يلاحظه حتى يفتح المستخدم الشاشة | `OwnershipDrift` → قرار؛ مسار مُثبت في هذا الجيل → إصلاح صامت ⏳؛ غير مُثبت → يبقى بنقرة (`NOT_PROVEN`) |
| فشل إصلاح متكرّر | يعود للمستخدم كل مرة بلا ذاكرة عدّ | تصعيد بعد تراجُعَين (`ESCALATE_AFTER_ROLLBACK=2`) أو ثلاث إعاقات (`ESCALATE_AFTER_BLOCKED=3`)؛ و`UNKNOWN_STATE` يوقف الهدف حتى مراجعة صريحة (`acknowledge`) |
| دليل جهاز للدعم | غير موجود في التطبيق (يُجمع بالاختبارات فقط، أصل `HOST`) | لمسة → جولة تسجّل → ملفّ واحد يُتحقَّق من قراءته → مشاركة يدوية عبر المنتقي؛ لا رفع |
| جولة منقطعة أو مقصوصة | لا التقاط أصلًا | الالتقاط يرفض الجولة غير المنتهية («قالب من جولة مقاطَعة سيُعيد إنتاج جهاز صمته ليس صمته»)؛ والقصّ يُوسَم `truncated` |
| نقص في التنسيق | لا شيء يكشفه | العدّادات تفضحه: `unrecordable` · `skipped` — «الإغفال لا يكون غير مرئي» |

⏳ صفوف الإصلاح الصامت تصف **السلوك عند الربط**؛ الربط الإنتاجي غير منفَّذ في هذه الموجة (§١٣).

---

## ١١) الاختبارات والتحقق

**أصناف جديدة:** `AtlasQuirkTest` (+237) · `AtlasRecordingTransportTest` (+162) · `AtlasFixtureStoreTest` (+89) · `AtlasQuietHealingTest` (+279 · **19 عقدًا**) · `AtlasSessionCoordinatorTest` (+232 · **11 عقدًا**).

**حرّاس قائمون تعزّزوا:** `AtlasArchitectureTest` (+13 ثم +9) — رفض الملفّين النقيّين الجديدين حتى صُنِّفا في `declaredOffReadPath` («الحرس عمل قبل أن يُطاء عليه» — تكملة ٢٧٩)؛ و`AtlasPresentationTest` (+53) لتدفّق الالتقاط.

**قياس محلّي بمحرّك JUnit فعلي (تكملة ٢٧٩):** `:app:compileReleaseKotlin` = `BUILD SUCCESSFUL` في **21m59s** · ثم `testReleaseUnitTest` (مصفّاة على الأصناف الثلاثة) = `BUILD SUCCESSFUL` في **7m12s** — وتقرير XML: `tests=39 failures=0 errors=0 skipped=0`. وفي هذا التشغيل نفسه قِيس إصلاح ربط `AtlasFreshnessTest` من جولة `CI-FIX-02`، فسقط وسم «غير مُتحقَّق بمُصرّف».

**حدّ قائم:** الطقم الكامل (آخر مرجع: 3072 اختبارًا في CI) **لم يُشغَّل بعد على هذا الرأس** — الـCI هو الحكم، وهو الخطوة التالية المعلنة في `HANDOFF` و`NEXT_TASK`.

---

## ١٢) ما لم يتغيّر (مقصود)

- **الدورة السبعية ومالكوها**: الاستكشاف (BackendProvider/Discovery) · الهوية (DeviceIdentity) · الخريطة (CapabilityMap) · التكييف (Resolver/RoutePlanner) · التنفيذ (Repository + arbiter) · الإثبات (Freshness/EvidenceStore) · التذكّر (FailureLedger/RouteMemory) — لا مكوّن استُبدل أو حُذف في الموجة.
- **مسار الكتابة الواحد وسياسة السلامة**: لم تُلمسا — والطبقة الجديدة **لا تكتب أصلًا**: سياستها نقيّة ومنفذ كتابتها الوحيد واجهة محقونة.
- **`AtlasDoctor`**: يبقى كما كان — بلا مستدعٍ في مسار التطبيق (والجديد لا يستدعيه: التقاط التطبيق يمشي بجولة العرض الحقيقية نفسها).
- **الحدّ مع Max AI** بلا تغيير: Atlas يقرّر «كيف»، وMax AI «ماذا/متى» (كما تفصله `docs/max-atlas.md`).
- **وثيقة `docs/max-atlas.md` (138 سطرًا)**: تصف الدورة والحدود والتصدير وإثباتًا «بلا جهاز» — لكنها **لم تُحدَّث بعد** بنصف الأدلة الجديد (الالتقاط من الجهاز S4 · طبقات التوفّر/العيوب S1 · الإصلاح الصامت S2). تحديثها **قرار المالك** (لم تُلمس في الموجة).

---

## ١٣) الحدود الصادقة

1. **هذه الوثيقة وصفٌ مقيس للمصدر، وليست قياس سلوك**: لا شيء فيها قيس على هاتف — الحذف الفعلي والانحراف الحقيقي وعتبات 60s/3/2 كلها **تحتاج جهازًا**.
2. **الربط الإنتاجي للإصلاح الصامت غير منفَّذ** (معلن): لا DI ولا مستدعٍ؛ و`ForegroundChanged` تبقى `NOT_ACTIONABLE` حتى يُربط مخزن الأنماط.
3. **العتبات تصميمية غير مقيسة** — `PROVENANCE` في الملفّ يسمّيها كذلك حرفيًّا.
4. **الاقتباسات الكمّية بوسومها**: 39·0 وزمن 21m59s/7m12s من **تكملة ٢٧٩** (تشغيل الجولة وتقريره XML)، لا من إعادة تشغيل جديدة؛ وحدّ «الطقم الكامل» ما زال معلّقًا على CI.
5. **لا بناء أُجري لهذه الوثيقة**؛ أرقامها البنيوية من `git` وحده (والأوامر أدناه تعيد اشتقاقها).

---

## ١٤) إعادة الاشتقاق والمرجعية

```sh
# أحجام الأساسين (مصدر + اختبار)
git ls-tree -r 16ce7f43 --name-only -- manager/app/src/main/java/nd/max/core/atlas/ | wc -l   # 24
git ls-tree -r 16ce7f43 --name-only -- manager/app/src/main/java/nd/max/core/atlas/ \
  | while read f; do git show 16ce7f43:$f | wc -l; done | awk '{s+=$1} END {print s}'          # 7021
find manager/app/src/main/java/nd/max/core/atlas -name '*.kt' | wc -l                          # 30
find manager/app/src/main/java/nd/max/core/atlas -name '*.kt' -print0 | xargs -0 cat | wc -l  # 8284

# الإيداعات (الفواصل)
git show --stat 26009f99 | tail -1 ; git show --stat 0fc82f69 | tail -1 ; git show --stat 91f6c049 | tail -1

# الأسطح التسعة والعيوب الأربعة
grep -n 'id = "R-' manager/app/src/main/java/nd/max/core/atlas/AtlasAvailability.kt
grep -n 'id = "' manager/app/src/main/java/nd/max/core/atlas/AtlasQuirk.kt
```

**المراجع الداخلية:** `HANDOFF` (تكملة ٢٧٨ = `CI-FIX-02` · تكملة ٢٧٩ = S2 والدفعة) · `NEXT_TASK` (بلوك `ATLAS-SELFHEAL-01`) · `docs/max-atlas.md` (وثيقة الميزة) · `REVIEW.md` §2.
