# DECISIONS

Binding decisions from the architect pass (2026-09-15). Executors follow these unless a decision is explicitly revised here. Format: decision → why → consequence.

## ADR-01 — One navigation model; the pager mode is deleted
**Why:** `use_scroll_animation` produces two runtime navigation architectures for the same four screens, doubling every nav concern (D-01), and it is invisible to users.
**Consequence:** delete the `main` pager route, the flag read in `MainActivity`, and its settings toggle. Bottom bar + nav rail is the only model.
**Amended (2026-10-21, `SWIPE-NAV-01`):** horizontal swipe is an **input layer on this same model**, not a second one. One root detector in `MainActivity` calls the existing `navigateToPrimary` / `NavHost` / `OnBackPressedDispatcher`; in-screen tabs register a handler and only the `RESUMED` destination owns the gesture. No second pager and no duplicated screens. It is enabled by default with an explicit Settings switch, and horizontal controls (pagers, rows, sliders) keep their own drag because the root only acts on unconsumed movement.

## ADR-02 — Routes become a typed registry outside `MainActivity`
**Why:** 40+ string-literal routes inline in an 817-line activity produced dead aliases and untestable navigation (D-03).
**Consequence:** `ui/navigation/MaxDestinations.kt` is the single source of truth (route id, title res, icon, parent, risk level, deep-link key). `navigate("literal")` outside that package becomes a static-test violation.

## ADR-03 — Primary destinations are Now / Control / Apps / Max AI; Settings leaves the bottom bar
**Why:** Settings held a primary slot while navigating to two destinations, and Max AI — the product differentiator — had no primary entry (D-02, D-09).
**Consequence:** Settings is reached from the Now top bar and aggregates appearance, module/update, diagnostics, logs, advanced tools, about.

## ADR-04 — Control is organized by device domain, not by screen name
**Why:** ~9 real domains were split across 19 launcher rows with overlapping scope (D-03, D-04).
**Consequence:** 8–9 domain hubs (CPU, GPU, Memory, Display, Responsiveness, Thermal, Power, Storage & compiler, Network). A dedicated screen survives only for deep workspaces (CPU core grid, GPU ladder, per-app editor, kernel flasher).
**Amended (round 226, `AU-01`):** the hub count is **ten**, not nine — **Audio** joins as the tenth domain (output devices, declared effects, then streams in its own stage; `SOUND-SCREEN-PLAN`). It sits under `Control` because audio is a system world like power and display, and it is read-first. The "nine worlds" figure was quoted in comments and older plans; the registry (`MaxDestination.All`) was and remains the one source, so this line is annotated rather than rewritten (ADR-18).

## ADR-05 — Merge by absorption, never by rewrite
**Why:** six screens were just rebuilt on the new language; re-doing them for the sake of the new IA would burn work and risk regressions (explicit user constraint).
**Consequence:** hubs host existing screen bodies as sections; migration of a body to `ui/design/` happens in its own later task, not in the IA task.

## ADR-06 — `ui/design/` is the only design system; `ui/component/` is legacy in runoff
**Why:** two parallel systems, the better one covering 13% of screens (D-05).
**Consequence:** new UI imports `nd.max.ui.design` only. No new `Scaffold(` in screen files. Legacy components are deleted in the task that orphans them. `ui/components/` is folded into `ui/component/` and the duplicate package name disappears.

## ADR-07 — Data trust is a product rule, not a screen detail
**Why:** `MaxDataTrust` + source labelling is the credible differentiator in a category full of fake telemetry (D-06).
**Consequence:** any surfaced number must carry freshness + source; unknown renders as `status_unknown`, never as a plausible value. Synthesizing or interpolating telemetry is a blocking defect.

## ADR-08 — Capability and state must be visually separate
**Why:** the TouchBoost rebuild proved the old pattern conflated "device supports it" with "it is on".
**Consequence:** every control surface shows a providers/capability section (with node path + trust) distinct from its state switches; every disabled control supplies `lockedReason`.

## ADR-09 — Ownership of every knob is user-visible
**Why:** the arbiter, manual locks, and cross-process ownership journal already exist but are invisible where they matter (D-02).
**Consequence:** `MaxOwnerChip` (You / Max AI / Per-app / Module / Kernel default) renders on control rows and hub summaries and can release or lock, backed by `HardwareControlArbiter` + `ManualControlLocks`. No new ownership concept is invented in the UI layer.

## ADR-10 — Max AI must be auditable: ledger over dashboard
**Why:** trust in autonomy comes from history, not from status cards.
**Consequence:** a change journal (actor, knob, before→after, verified/rolled back, measurement) is a first-class surface in Now and in Max AI. `MaxAiScreen`'s generic cards are replaced by: objective, ownership map, journal, safety, learning credibility.

## ADR-11 — UI never writes hardware directly
**Why:** bypassing the control plane breaks leases, baselines, rollback, and safety veto.
**Consequence:** no `RootFileAccess`/shell writes from composables or screen ViewModels; all writes go through the arbiter/control-plane APIs.

## ADR-12 — Efficiency is part of the brand: decoration is opt-in and off by default
**Why:** blur, ambient glow, motif overlays, weather effects, and video wallpaper cost frames and battery in an app that promises to save both (D-07).
**Consequence:** one Appearance → Ambient effects switch, default off, with an honest cost note; effect engines are not composed when off; unreferenced engines are deleted.

## ADR-13 — Semantic tones stay hard-coded, not dynamic
**Why:** Positive/Caution/Critical must survive any wallpaper-derived palette; a "critical" that renders pastel green is a safety bug.
**Consequence:** keep `MaxTone`'s fixed hues and the light/dark surface derivation in `MaxTokens.kt`; dynamic colour only drives Neutral/Accent.

## ADR-14 — Localization parity is part of "done"
**Why:** new screens added 188 English-only keys in an RTL-first, ~100-locale app (D-10).
**Consequence:** every task that adds user-visible copy updates `values/` **and** `values-ar/` in the same change; no string literals in Compose.

## ADR-15 — Static verification is the gate; compilation is best-effort
**Why:** no usable Gradle/JDK toolchain combination here (D-11).
**Consequence:** each task ships with grep/python contract checks (`VALIDATION.md`) and, where cheap, a JVM source-scan test in the existing architecture-test style. Reports must say "compilation unverified in this environment" rather than implying a build passed.

## ADR-16 — High-risk tools are gated, not featured
**Why:** Terminal, SetEdit, ActivityLauncher, KernelFlasher can brick or wedge a device and currently sit beside ordinary tweaks.
**Consequence:** they live under Settings → Advanced tools with an explicit risk gate and `MaxRiskDialog` on destructive actions; risk level is declared in the destination registry (ADR-02).

## ADR-17 — Root/module status becomes one app-level session state
**Why:** root is re-probed on every navigation event and each screen invents its own unsupported story (D-08).
**Consequence:** a single observable session state (root, module, SELinux, capability snapshot) provided once; screens read it and render `MaxCondition` from it. Not in NT-01's scope, but no new per-screen probes may be added.

## ADR-18 — Nothing already-built is redone for aesthetic reasons
**Why:** explicit user constraint and respect for sunk, good work.
**Consequence:** the six migrated screens and the control-plane work are treated as foundations; they change only when a functional rule above (ownership chip, journal, localization) requires an additive change.

## ADR-19 — دفتر الحلقات هو الذاكرة السردية، لا العدّادات
العدّادات الأربعة و"آخر إجراء" كانت تُهدر سلسلة السبب/النتيجة كل ٣٠ ثانية. صار كل قرار حلقة دائمة مُسلسلة حقلًا بحقل.

## ADR-20 — إظهار المرشحين المرفوضين
القرار غير مفهوم بدون البدائل. `planWithTrace` يُخرج كل مرشح مع سبب الاستبعاد (`measured_harm`/`predicted_harm`/`no_step`/`unreadable`).

## ADR-21 — `NO_ACTION` حدث يُسجَّل
المراقبة الواعية ليست خمولًا؛ تُسجل حلقة بلا تنفيذ (بخنق ٥ دقائق) كي لا يبدو النظام ميتًا حين يقرر ألا يتدخل.

## ADR-22 — التنبؤ يُقارن بالقياس دائمًا
كل حلقة تحفظ `predictedGain` و`predictionErrorGain = |تنبؤ − مقيس|`، فتُعرض دقة النموذج بدل ادعائها.

## ADR-23 — ما لا يُقاس يُعرض كغير متوفر
لا صفر افتراضي ولا رسم وهمي: `after == null` ⇒ "تعذر القياس"، والخط الزمني فارغ قبل أول حلقة حقيقية.

## ADR-24 — شريط التطور في الذاكرة فقط
عيّنات الجلسة الحالية (120) لا تُحفظ على القرص كي لا يُعرض رسم "حي" من جلسة سابقة.

## ADR-26 — كل ملف نصوص يُسجَّل في Crowdin صراحةً
**القرار:** `crowdin.yml` يسرد في `files:` كل ملف في `values/` يحمل نصًّا مرئيًا للمستخدم، وملف نصوص جديد يُسجَّل في نفس التغيير. التجاهل المقصود يُكتب لا يُترك ضمنيًا.
**لماذا:** كانت ٤٧١ نصًّا في ٥ ملفات (`max_ai` ٢٢٠، `max_screen` ١٧٧، `max_navigation` ٥٤، `max_design` ١١، `studio` ٩) خارج خط الترجمة لأن `crowdin.yml` كان مربوطًا بـ`strings.xml` وحده — فبقيت إنجليزية في تطبيق من ~١٠٠ لغة.
**النتيجة:** أي ملف نصوص غير مسجَّل = عيب. المرجع للتحقق: `.planning/codebase/INTEGRATIONS.md` → «Localization pipeline». وملفات `values-ar` المصاحبة تبقى مصدر الحقيقة المتزامن (ADR-14).

## ADR-27 — ثلاثة أشياء تصف اللغات الـ٨٥، وأداة واحدة تمنع تباعدها
**القرار:** عدد اللغات ومصدرها واحد — مجلدات `res/values-*` الفعلية. منها تُشتقّ `AppLanguage.CODES` (منتقي داخل التطبيق) و`res/xml/locales_config.xml` (ما يعرضه النظام)، و`tools/i18n_coverage.py --check-codes` يقارن الثلاثة ويفشل عند أي اختلاف.
**لماذا:** `setApplicationLocales` يُمرّر الوسم إلى نظام أندرويد، ومنذ API 33 يرفض النظام الطلب ما لم يُعرَّف `android:localeConfig` في البيان — فبدون الملف يعمل التبديل على الإصدارات الأقدم ويفشل صامتًا على الحديثة. ووصف اللغات في مكانين بلا أداة يعني أن يتباعدا بلا أن يلاحظ أحد.
**النتيجة:** لا تُضاف لغة في الكود قبل أن يوجد مجلدها، ولا يُحرّر `locales_config.xml` يدويًا. أكواد المجلدات القديمة (`values-in`, `values-iw`, `values-tl`) تُكتب في الكود بصيغتها الحديثة (`id`, `he`, `fil`) لأن النظام يعرض الحديثة و`aapt2` يوفّق بينهما. التفاصيل والبوابة: `VALIDATION.md` §3.1.

**مسلكان للتطبيق لا مسلك واحد:** `AppCompatDelegate.setApplicationLocales` مسالك النظام على API 33+، لكنه يمرّ عبر `AppCompatActivity` ونشاط التطبيق `ComponentActivity` بسمة منصّية — فهو لا يُطبّق اللغة على أندرويد ١٠–١٢ (`minSdk = 29`). لذلك `MainActivity.attachBaseContext` يلفّ السياق بـ`AppLanguage.wrap` (لغة + اتجاه تخطيط). حذف أحدهما يُسقط نسخة من نسخات أندرويد المدعومة.

## ADR-28 — الترجمة تُجمع وتُتحقق آليًا، ولا تُخترع
**القرار:** لا تُكتب ترجمة آلية داخل المستودع من نموذج لغوي. `tools/i18n_coverage.py` يجمع المفاتيح الناقصة لكل لغة في CSV، ويكتب العائد منها بأسلوب **الإضافة فقط** بعد التحقق من الوسائط، والبشر أو Crowdin هم من يترجم.
**لماذا:** ٨٤ لغة ناقصة ‎١٤٤٠٤٣‎ مفتاحًا. الاختراع الصامت لترجمة بهذا الحجم يعطي نصًّا غير مُراجَع يبدو مكتملًا وينقل خطأً حقيقيًا في تطبيق يتحكم بمسارات الجذر — والصف الذي يطلب وسيطًا لا يمرّره الكود يُسقط التطبيق لا يُظهر نصًّا رديئًا.
**النتيجة:** النقص مُقاس ومعلَن (اللغة غير المترجمة تُظهر الإنجليزية تلقائيًا، وهذا سلوك صحيح لا عيب)، وكل صف مرفوض يُطبع مع سببه.

**تعديل 2026-09-18 — قرار المالك: الـ٨٥ لغة تُكتمل بالترجمة الآلية.** منعُ الاختراع الصامت لا يعني منع الترجمة: يُسمح بالترجمة الآلية عبر **مزوّد مُعلَن** بمفتاح يضعه المالك (`tools/i18n_translate.py`)، وبثلاث ضمانات لا يُتازل عنها: ① الوسائط والمصطلحات تُحجب قبل الإرسال بصور حارسة وتُستعاد بعده (فلا مزوّد يستطيع قلب `%1$s` أو حذفه)، ② المخرج **لا يُكتب في الموارد**: يمرّ على `--apply-csv` بأسلوب الإضافة فقط، ③ مخرج مزوّد `stub` لا يُدمج أبدًا. ما يبقى مسؤولية المالك: اختيار المزوّد وتكلفته (٤٣٥٩٤٦٣ حرفًا مصدرًا: ~٧٧–٩٦ $ مرة واحدة، أو ٩ أشهر على الحصة المجانية، والذاكرة المؤقتة تمنع إعادة الفاتورة)، ومراجعة لغوية لاحقة إن أراد جودة أعلى من الآلة.

## ADR-29 — قائمة اللغات تُكتب يدويًا، وتوليد AGP الآلي مُعطَّل
**القرار:** `androidResources { generateLocaleConfig = false }` مع إبقاء `android:localeConfig="@xml/locales_config"` وقائمة اللغات الصريحة في `res/xml/locales_config.xml`.
**لماذا:** المشروع كان يحمل `generateLocaleConfig = true` (2026-09-17) بينما جولة اللغة (2026-09-18) أضافت قائمة صريحة وربطها بالبيان — وهذا مزيج يرفضه AGP: «Locale config generation was requested but user locale config is present in manifest» فيسقط `:app:processDebugMainManifest` ويفشل البناء كله (لم يُكشف إلا ببناء حقيقي في جولة ١٠).
**النتيجة:** اختيار واحد من الاثنين لا كليهما. اختيرت القائمة الصريحة لأنها المصدر الوحيد الذي يضمن ظهور **اللغات الـ٨٥ كاملة** في منتقي النظام — ومنها ما لا يستنتجه التوليد الآلي من أسماء المجلدات (`values-b+sr+Latn`, `values-zh-rHK`, `values-pt-rPT`) — ولأنها الوجهة التي يتحقق منها `tools/i18n_coverage.py --check-codes` (ADR-27) مقابل المجلدات والمنتقي. التوليد الآلي يُسقط هذا الضمان بلا أن يشتكي أحد.
**إثبات:** `:app:testDebugUnitTest :app:assembleDebug` → BUILD SUCCESSFUL، والبيان المدمج في الـAPK يحمل `android:localeConfig`، والحزمة تضم `res/xml/locales_config.xml` (`VALIDATION.md` §0).

## ADR-25 — بدائل سينمائية بلا تبعية للنواة
`MaxAiCinematics.kt` لا يستورد `nd.max.core.*` ولا التنقل، فتبقى لغة التصميم قابلة لإعادة الاستخدام والاختبار.

## ADR-30 — الوتيرة تُشتق من إشارات مجانية، بحد أدنى معلن (2026-09-18)

**القرار:** إعادة التخطيط المبكر (`MaxAiCadence`) تُقرَّر من ثلاث حقائق فقط، ولا شيء غيرها: حالة الشاشة، ونوع الاستخدام المقيس في `DeviceStateCollector` (لعبة/عادي/مطفأ)، ومستوى حاكم الأمان. الإشارة تُبنى من اللقطة التي تُجمَع كل ثانية أصلًا للحلقة السريعة، وبدون أي قراءة جذرية جديدة (اسم الحزمة يُقرأ في دورة القرار عند تنفيذها فقط). ولا يُسمح بدورتين متتاليتين بسبب تغيّر حقيقي قبل `MIN_ADAPTIVE_INTERVAL_MS = 10_000`، وهو مساوٍ لنافذة قياس الأثر في المحرك.

**لماذا:** كان المحرك يفكر كل ٣٠ ثانية، والدورة الفورية لا تقع إلا بطلب المستخدم — فمن فتح لعبة كان التاريخي أن ينتظر حتى ٣٠ ثانية كي يلاحظ Max AI تغيّر السياق. لكن إزالة الانتظار بإطلاق حلقة استطلاع سريعة كانت ستقلب المبدأ المعاكس (تحكم لا يتأرجح، ADR-23/CYCLE_MS): عشر ثوانٍ من عمر البطارية لأجل طمأنينة بصرية ليست ذكاءً. الحل بالتفريق: **الهدوء يبقى، والاستثناء مؤهَّل ومحدود**. والحرارة والحمل مستبعدان عمدًا من الإشارة لأن لكل منهما مسارًا أسرع (حاكم الأمان كل ثانية)، وإدخالهما كان يجعل المحرك يستيقظ كل ثانية بلا سبب جديد.

**النتيجة:** لا سلطة جديدة ولا كتابة عتاد جديدة — الدورة المستيقَظة هي `cycle()` نفسها بكل سقوفها. والحد الأدنى لا يُنسي التغيّر: الإشارة السابقة لا تُحدَّث حين تأجيل الاستيقاظ، فيبقى الفرق قائمًا ويُعرض في الواجهة كعدّاد تنازلي، ثم يقع الاستيقاظ في أول ثانية تنتهي فيها المهلة. وكل استيقاظ يُوثَّق في `DiagnosticCenter` بسببه.

## ADR-32 — السقف الحراري منحنى لا عتبتين، وكل فرق عن القديم زيادةُ حماية (2026-09-18)

**القرار:** يُستبدل السقفان الثابتان (`ENGAGE_CAP_FRACTION = 0.55` داخل نطاق التدخل، `CRITICAL_CAP_FRACTION = 0.35` عند الحرجة) بمنحنى `MaxAiThermalCurve`: سقف متناسب مع مقدار التجاوز، وبند تكاملي مُقيَّد يشدّد ولا يرخي، وشرط تبريد متتالٍ قبل الاسترجاع، وتقريب إلى خطوات ٥٪ مع حد كتابة واحد كل ١٥ ثانية. **وتاكيداً على عدم المساس بقواعد السلامة**: منحنى المقدار لا يقرر **مستوى** الحماية — آلة الحالة في `SafetyEngine` (هستيريسيس ٥ درجات + تنبؤ أمامي + إعادة محاولة بتراجع أُسّي) تبقى كما هي، والمنحنى يترجم مستواها إلى رقم.

**لماذا:** أربع درجات من التجاوز كانت تُعالج بنفس القسوة، والدرجة الخامسة بشيء. والتحكم المُكمَّم (سلّم ترددات العتاد) لا يصلحه البند التناسبي وحده — وهو ما تعالجه نواة لينكس في حاكم `power_allocator` بثابت مخصوص (`integral_cutoff`) ومُعامِل مختلف للتجاوز عن ما دونه (`k_po` مقابل `k_pu`)، وما تعالجه المتحكمات العملية بلا تماثل مقصود (تصعيد فوري، ترخية بعد قياسات مستقرة). وهذا كله مذكور بدليله في `docs/ai/EXTERNAL-RESEARCH.md` §١ — لا واحد منه منقول، وإنما أعيد تصميمه على قيودنا.

**الدعوى القابلة للتكذيب (شرط القبول):** `MaxAiThermalCurve` **لا يعطي سقفًا أخفّ من السقف القديم في أي نقطة**، والبند التكاملي لا يُضاف أبدًا. `MaxAiThermalCurveTest` يفحص ذلك شاملةً على النطاق ٤٨°–٦٤° بخطوة ربع درجة، وعند الحدّين بالضبط، وبالتراكم الطويل، وبالإعداد الفاسد. أي أن كل فرق مالحظ عن السلوك السابق هو كتابة في اتجاه الأمان، وهذا ما يجعل التغيير قابلًا للتسليم قبل تجربته على جهاز.

**الحدود المُعلنة (لا تُدّعى):** كسب البند التكاملي (`0.0015`) ومدى الأرضية (`0.25`) قيم تصميمية غير معايَرة على جهاز — وهي مذكورة كخطر متبقٍّ في `HANDOFF.md`. وأي تغيير يمسّ هذا المنحنى لاحقًا يُثبت بنفس الاختبار أو يُرفض.

## ADR-31 — نطاق التعلّم لكل سياق، ولا يُخلط بعمود واحد (2026-09-18)

**القرار:** حكم المقبض يُحسب ويُعرض **داخل سياقه**: `Snapshot.knobs` تبقى أحكام السياق العام `*` (دلالة لم تتغير لأي شاشة قائمة)، و`knobsByContext` يحمل حكم كل سياق بمفاتيحه، و`contexts` مرتّبة: العام أولًا ثم الأغزر عيناتٍ ثم الأبجدي كسرًا للتعادل. والواجهة تُختار نطاقًا واحدًا وتقرأ أرقامه وحده — بلا دمج ولا معدّل مرجّح بين التطبيق والجهاز.

**لماذا:** نواة التعلّم كانت تقيس الأثر بمفتاح `(مقبض، اتجاه، سياق)` وتُغذّي السياقين معًا (الخاص والعام) من كل قياس، ثم يقرأ الاستخلاص **السياق العام وحده** ويعرضه للمستخدم. أي أن أدقّ ما يعرفه المحرك — «هذا المقبض ينفع في هذه اللعبة» — كان يُقاس ثم يُخفى، ويُستبدل بمتوسط عام يمكن أن يصفر بحق. وعرض المعدّل واحدًا كان سيكذب في الاتجاهين: مقبض نافع في تطبيق واحد يبدو فاشلًا بعد خلطه بسياق خامل، ومقبض عادل يبدو نافعًا بعد خلطه بسياق استثنائي.

**النتيجة:** `contextLabel` يحوّل اسم الحزمة إلى اسم التطبيق حين يسمح النظام، ويعود إلى اسم الحزمة حين لا يسمح — لا يُخترع اسم غير معروف (ADR-07/ADR-23). ومعه **تدقيق المحرك على نفسه**: صدق التنبؤ (`PREDICTION_TOLERANCE_GAIN = 5 × HELPFUL_GAIN`، مشتقّة لا مُختارة، و`hitRate` هي الحكم) وحاصل التجارب ونسب التجاوز والقياس، وكلها بمقام صفري يُعرض كغير متوفر لا كصفر (ADR-23). القاعدة الجامعة: **الرقم الذي يقيس المحرك نفسه يُعرض كما يقيسه الآخرون، لا كما يريد أن يُقاس.**

## ADR-33 — يُقاس ما هو زمن لا ما هو حالة، والغياب يُكتب غيابًا لا صفرًا (2026-09-18)

**القرار:** يُضاف مقياس زمني واحد هو **ميزانية الحرارة** (`MaxAiThermalBudget`): الزمن من بدء حمل ثقيل مقيس إلى أول خنق، يُشتق من نفس قياسات الحلقة السريعة بلا عتاد جديد، والمحرك يستدعي `observe(...)` مرة كل ثانية و`statusOf(...)` للعرض. وثلاث قواعد قبول ملزمة: ① الجلسة التي تنتهي **بلا خنق** تُعرض «جلسة نظيفة» لا رقمًا مُقدّرًا؛ ② التدخل الثاني في الجلسة نفسها لا يُعيد قفل الميزانية (الرقم عمر الجلسة حتى **أول** خنق لا آخر خنق)؛ ③ إنهاء الجلسة بإطفاء الشاشة مع قيام الحمل **لا يُحتسب صمودًا** — لا نعرف هل صمد العتاد أم توقف الطلب. ومعها **ذيل الأثر** في `MaxAiInsights.DeltaSpread` (`worst/p10/median/best`) للقرارات المقيسة وحدها، بترتيب الأقرب لا بالاستقراء، وباستبعاد التجارب المعرفية وحلقات السلامة من التوزيع.

**لماذا:** كل ما يعرضه Max AI كان يصف **حالة** (حرارة، حمل، عدد تدخلات)، وسؤال اللاعب الحقيقي زمني: «هل أعتمد على هذا الجهاز ساعة كاملة أم يهبط بعد عشر دقائق؟». وأدبيات التخفيف الحراري في AOSP تقترح هذا القياس صراحةً (تسجيل `t0` عند `THERMAL_STATUS_NONE`، وقياس المنقضي إلى أول خفض)، وهو XR-05 في `docs/ai/EXTERNAL-RESEARCH.md`. وأما الذيل فلأن المتوسط يخفي الذيل: مكبسب متوسطه `+0.02` قد يأتي من عشرة نجاحات وخسارة كبيرة واحدة، والمتوسط وحده يقول «نجحنا».

**النتيجة (درس عيب حقيقي):** كشفت الاختبارات أن `sessionStartedAtMs = 0L` كان يُستعمل كعلامة «لا جلسة»، والصفر لحظة زمنية صحيحة (بدء التشغيل) — فقراءة ساعة عند الصفر كانت تلغي الجلسة صامتةً بلا خطأ ظاهر. أُبدلت العلامة بحقول `Long?`. **القاعدة المستخلصة:** استعمال قيمة من صميم مدى المُدخل كعلامة حالة ممنوع؛ الغياب يُكتب `null` (أو حقل منطقي صريح) حتى لو كان المُدخل في الإنتاج لا يساوي الصفر عمليًا — لأن العقد لا يُبنى على ما تصادف أن المُستدعي يمرّره.

**الخوارزميات المُعاد تصميمها (لا منقولة):** ترتيب الأقرب لـ`p10` (لا استقراء خطي بين عينتين — لا يُعرض رقم لم يُقس)، ووسيط حقيقي لحجم زوجي، وتوزيع مبني من `kind == DECISION` لا من كل حلقة. والمصادر في §١ و§٣ من `EXTERNAL-RESEARCH.md`.

**الحدود المُعلنة:** `MIN_SESSION_MS = 30s` و`DEMANDING_INTENT = 0.75` قيم تصميمية **غير معايَرة على جهاز**؛ و«حمل ثقيل» مشتق من `appIntent` المُقدّر لا من إطارات حقيقية — فجلسة مُصنَّفة خطأً تُقاس خطأً. مُسجَّلة في I-60/I-61.

## ADR-34 — ضغط الذاكرة يُقاس بPSI لا بنسبة الامتلاء، والخنق مانع لا مشجّع (2026-09-18)

**القرار:** تُضاف إشارة `memoryStall` من `/proc/pressure/memory` (`full avg10`)، مقروءة بـ`MemoryPressureReader` ومحلَّلة بـ`MemoryStall` النقي. وتُستخدم في اتجاه واحد فقط: **تمنع رفع الأداء** حين يتجاوز الخنق [STALL_FRACTION]. ومعه قاعدتان: ① غير المدعوم (`UNSUPPORTED`) **لا يمنع شيئًا** — الجهل ليس خنقًا؛ ② الاستقصاء مُخزَّن سلبيًا: نواة أعلنت عدم الدعم لا تُسأل إلا كل [RETRY_AFTER_MS]، لأن `RootFileAccess.read` يجرّب قشرة عند فشل القراءة المباشرة — فاستقصاء كل ثانية كان يعني عملية قشرة كل ثانية مقابل قياس لا يُنتج رقمًا.

**لماذا:** `DeviceStateCollector.memoryUsage` يقيس **الامتلاء** لا **الأثر**؛ جهاز بـ٩٠٪ ذاكرة مستخدمة قد يكون سريعًا تمامًا وآخر بـ٦٠٪ في خنق شديد. والمقياس الصحيح موجود في النواة أصلًا: `some` (توقف بعض العمل) و`full` (توقف كل العمل غير الخامل = thrashing)، وعليه بُني `lmkd`. واستعماله في اتجاه واحد مقصود: رفع الترددات على جهاز يتوقف منتظرًا الذاكرة لا يعالج السبب بل يزيد الطاقة على نفس العمل المتوقف.

**الدعوى القابلة للتكذيب:** المنع **لا يستطيع إلا أن يقلب رفعًا إلى توفير**، ولا يقلب شيئًا في الاتجاه المعاكس، ولا يعمل بلا قياس. `ObjectiveTest` يفحص ذلك بتسلسل خنق تصاعدي (null → 1.0): رفع عند الأول، ثم منع دائم لا يعود إلى رفع. و`MemoryStallTest` يفحص الرفض: نواة تصدّر `some` وحدها تُعلن `UNSUPPORTED` (عرضها مكان `full` كان سيقلب «انتظار عادي» إلى «خنق»)، والأرقام المشوّهة (`NaN`/`Infinity`/نص) تُرفض لا تُقصّ، وما خرج عن المدى يُقصّ إلى حدّه.

**عبء إضافي مُعلن:** إشارة ثامنة في اللقطة، **لا تُمرَّر إلى موصل RL الأصلي** (عقده سبع إحداثيات ثابتة — زيادتها كانت ستقلب معنى كل وزن في النموذج).

**الحدود المُعلنة:** `STALL_FRACTION = 0.10` غير معايَرة على جهاز، ومسار القراءة الفعلي (صلاحية `/proc/pressure/memory` بلا جذر على إصدارات مختلفة) لم يُجرَّب على جهاز — مُسجَّل في I-65.

## ADR-35 - Observable intent and evidence-first workspaces (2026-09-19)

Max AI remains one local control plane. UI preferences reflect observable engine state, not a remembered imperative getter. Request progress is distinct from verified hardware state; a tap never establishes success. Presentation freshness expires independently of engine emissions, rejects future timestamps as live, and never invents a reading before the first sample. A presentation clock does not request hardware reads.

The Max AI destination separates overview, journal, learning and controls while retaining the causal evidence and existing live command centre. Journal queries operate on recorded fields, preserve order, and do not mutate learning. Learning contexts are selected by stable key so new samples cannot silently switch the selected app. All changes reuse `ui/design/`, paired EN/AR strings, existing arbiter ownership and existing safety policy.

## ADR-36 — إعادة بناء واجهة مدير الملفات على طلب المالك، والنموذج لا يُمَس (2026-09-19)

**القرار:** أُعيد بناء **عرض** شاشة مدير الملفات (`FM-02`) استجابةً لطلب المالك الصريح بأنها «عديمة الفائدة
وغير متناسقة وصعبة الاستخدام»، وبمرجع سبع لقطات لمدير ملفات آخر قُرئت بنصوصها (‏`tools/read_image_text.py`).
والتغيير مقيَّد بحدّين: ① طبقة العرض — والنموذج الخالص يُعدَّل **إضافةً** لا إبدالًا (`EntryCounts` · `DiskSpace`
· `isHidden/withoutHidden/counts` · `showHidden`/`tabs` · `FileSystemEngine.diskSpace`)، و② كل ما يُثبت
السلامة يبقى كما هو: الحرس قبل الـshell، والأحكام الثلاثة للنتيجة، والإعلان عن المجهول («غير مقروءة» لا صفرًا).

**لماذا لا يخالف `ADR-18`:** ذاك يمنع إعادة ما بُني **لأسباب جمالية**. والطلب هنا استعمالي مُعلن، والتشخيص
مقيس لا مُدَّعى: أربعة أشرطة قبل أول صفّ في كل لوح، وأفعال موزّعة على ثلاثة أشرطة بأيقونات بلا أسماء،
وصفوف بسطرين تحمل الصلاحيات في كل صفّ. فالقاعدة تُطبَّق على «أعيدوا بنائه ليبدو أحدث» لا على «لم يعد صالحًا
للاستعمال».

**النتيجة الملزمة للجولات القادمة:**

1. **بنية الشاشة معلنة**: شريط أوامر الشاشة في الأعلى · **شريط سفلي واحد** يتبدّل دوره (أدوات اللوح النشط،
   أو إجراءات التحديد) · وداخل كل لوح: تبويباته · مساره · سطر حالته · قائمته — ولا شريط خامس يُضاف.
2. **لكل فعل اسمه**: لا زرّ أيقونة في هذه الشاشة بلا `contentDescription`، ولا إجراء في قائمة إلا بنصّه.
3. **الإخفاء يُعلَن**: كل ترشيح أو إخفاء يقصّ ما يُرى يجب أن يُصرّح بعدد ما أخفى في سطر الحالة — «مخفيّ
   ومُعلَن» غير «مفقود».
4. **ما لا يُقاس لا يُعرض**: لا حجم لمجلد غير مقيس، ولا إجراء لا ينطبق (`FileActionSet`)، ولا مساحة تُكتب
   صفرًا حين لم تُقرأ.
5. **التبويبات تخصّ اللوح** وتُحفظ مع حالته (`PaneSaver`)، ولا تُبنى من نصّ محفوظ في مكان آخر.

**الحدود المُعلنة:** لم تُرَ الشاشة على جهاز؛ وعدد التبويبات الكبير في لوح بعرض ~١٧٠ نقطة لم يُقس
(‏`HANDOFF` تكملة ٤١ §٦).

## ADR-37 — أطلس: بنك معرفة ثانٍ من مشاريع مفتوحة المصدر — **مفردات واجهات لا جداول مسارات** (2026-09-20)

**السياق.** أمر المالك: «وخذ من SmartPack وغيرها من مشاريع مفتوحة المصدر معرفتهم، لأنهم يجمعون هذه
الأشياء لسنوات، فإذا ما هو مخزّن عندنا لا يعمل يقوم أطلس بإكماله تلقائيًا». وهذا يقع على `D-02`
و`ATLAS-01` اللذين يمنعان «نسخ الأكواد والسكربتات وقواعد المسارات»، وعلى حكم `S01` نفسه: «Do not import
path maps, scripts, custom controls, or generic governor defaults».

**القرار.** يُضاف `AtlasCommunityBank` كبنك ثانٍ، و`AtlasCompletion` كمرحلة ثالثة في المسح، بهذه الحدود:

1. **ما يُنقل مفردات الواجهات**: اسم عقدة ↔ وحدة موثّقة ↔ جذر مُعدَد يمكن السير فيه. **ولا** جدول مسارات
   لكل جهاز، ولا سكربت، ولا معامل تحويل منسوخ، ولا سطر كود.
2. **كل مدخل يحمل `provenance`** (مصدر + مرجع + إصدار + سجلّ ترخيص)، ويمرّ **بنفس** مدقّق الكتالوج المراجَع:
   جذر مُعتمد، اسم أساس آمن، لا مفاتيح مكرّرة، ولا تعارض وحدات.
3. **الثقة لا تتجاوز `CLAIMED`/`FETCHED`**: ولا مدخل واحد بـ`SOURCE_VERIFIED`، لأنّ لا تنفيذ فُحص في هذه الجولة.
4. **القراءة منه لا تُرقّى**: `semanticStatus = INFERRED` ومرحلة `CANDIDATE_INTERFACE` — واسم واجهة معروف
   الوجود ليس معنًى مُراجَعًا.
5. **لا يُسأل إلا عن نقص**: النطاقات التي لم يُجب عنها البنك الأول، ولا تُعاد قراءة واجهة قُرئت، بحدّ ٣٢ مرشّحًا
   في المسح مضافًا إلى ميزانية النقل نفسها.
6. **لا يحرّك بوابة التقرير**: `reviewedUnresolved` يحصي نقص المراجَع وحده؛ غياب اسم مرشّح هو الحال المتوقَّعة.

**البديل المرفوض.** نسخ خريطة مسارات SmartPack كما هي: تخلط GPL-3.0 بـApache-2.0، وتُدخل «تخمينًا عند
الفشل» (قائمة حكام ثابتة عند تعذّر القراءة)، وتحوّل غياب الاسم إلى غياب الواجهة — وهو ما يرفضه أطلس.

**العاقبة.** `D-02` تبقى قائمة للكود والسكربتات وجداول المسارات، ويُستدرك عليها بالمعرفة بصيغة المفردات
مع الإسناد. وهذا الاستدراك **لا يوسّع الصلاحيات**: البنك الثاني يقرأ من نفس الأسطح المعتمدة، وبلا جذر،
وبلا كتابة، وبميزانية واحدة مع البنك الأول.

## ADR-38 — سقف الحرارة لكل تطبيق: مرجعه **قدرة الجهاز**، وبروفايلا القوّة يرفعان ولا ينزلان (2026-09-21)

**السياق.** قياس على جهاز صاحبه، ووصفه: «الحرارة في شاشة التطبيقات لا تزيد أبدًا عن القيمة الافتراضية،
وإنما تنقص». والسبب مُثبَت في الكود: النسبة كانت تُحسب من **السقف الحيّ** (`scaling_max_freq` كما هو
الآن). فجهاز سياسة الـvendor فيه تخفض السقف الحيّ إلى جزء من قدرته صار فيه «Gaming ٨٥٪» **أدنى من
الجهاز غير الممسوس**، و«Performance» لا يجد ما ينزله فيمرّ بلا أي كتابة على CPU.

**القرار.** يُصبح مرجع النسبة **القدرة المكتشفة** (`cpuinfo_max_freq`، أو أعلى درجة في سلّم السائق
المُعلن)، وللسقف نمطان لا نمط واحد:

```
بروفايلا القوّة (performance · gaming): target = max(النسبة من القدرة، السقف الحيّ)  ⇒ رفع مشروع، ولا خفض أبدًا
بروفايلات التبريد (balanced · power · custom): target = النسبة من القدرة، وتُكتب فقط إذا كانت أدنى من الحيّ
```

**الحدود غير القابلة للتفاوض.** الرفع متعهّد في المسار المحكوم القائم: ملكية `HardwareControlKey.cpuLimits`
· `CpuHardwareBackend.setPolicyLimits` (وقدّم `min <= max` والقصّ إلى الحدود المُثبتة) · `HardwareVerification`
· الاسترجاع إلى خط الأساس عند مغادرة التطبيق · وقفل المستخدم اليدوي يوقف المنحنى على تلك السياسة
(`cpu_policy_controls` لا يُمَسّ). ولا يُرفع إلا إلى **قيمة أعلنها السائق** — لا ثابت في الكود ولا رقم جهاز.

**الدعوى القابلة للتكذيب.** لا حالة واحدة يخرج فيها بروفايل قوّة **أدنى** من السقف الحيّ، ولا حالة يخرج
فيها بروفايل تبريد **أعلى** منه، ولا سقف مكتوب **فوق** النسبة المطلوبة من القدرة، ولا درجة غير مُعلنة
إلا حين لا يُعلن السائق سلّمًا (فعندها القدرة وحدها من `cpuinfo_max_freq`). وهذا مُثبَت بـ**١٥ حالة**
JVM في `ThermalCurveTest` — منها حالة القياس نفسها (قدرة ٢٤٠٠ تخفضها المنصة إلى ١٥٠٠ ⇒ Gaming يطلب
٢٠٠٠، لا ١٢٧٥).

**الحدود المُعلنة (لا تُدّعى).** أن المنصّة قد تعيد فرض سقفها بعد الكتابة (يُكشفه `direction=raise` مع
`live` أقل من الهدف في السطر نفسه) · وأن ارتفاع سقف CPU معنى حراري (يزيد الحرارة) ولذلك تبقى الطبقات
الأدنى مسؤولة عن الردّ: `ThermalGuard` التفاعليّ + `MaxAiThermalCurve` + خنق النواة. وأي تغيير يمسّ هذه
القاعدة يُثبت بنفس الاختبار أو يُرفض.

## ADR-39 — الإصدار **64-بت وحده**: `arm64-v8a` يفقد 32-بت من السلسلة كلها (2026-09-22)

> ⚠️ **أُبطل هذا القرار بـADR-44 (2026-09-24، تكملة ١١٠):** عاد العمودان إلى السلسلة كلها بأمر
> المالك («دعم كامل لـarmeabi-v7a»)، ولم يبقَ من ADR-39 إلا صيغته: لا استثناء صامت — الحرس
> انقلب من «امنع 32-بت» إلى «اطلب العمودين»، فأصير كل ما هنا تاريخًا موثّقًا لا حكمًا ساريًا.

**السياق.** طلب المالك تسريع البناء مرارًا، وعُرضت عليه مقايضات صريحة. واختار إسقاط `armeabi-v7a`
مع علمه بالثمن: **اثنا عشر بناء هدفًا في كل دفع** (خادم + preload بـ`ndk-build` عبر هدفين، وأربعة
crates Rust عبر هدفين) تصير **ستة**، ومقابلها تفقد الأجهزة 32-بت الدعم. **والثمن مُعلَن لا مسكوت عنه.**
وعند التدقيق: `terminal-emulator` لا يبني أصلًا (`externalNativeBuild` مُزال منه بقرار سابق)، و`libtermux.so`
يُشحن مبنيًّا لـarm64 وحده — فلم يكن له سهم في الحساب.

**القرار.** `arm64-v8a` هو المعمار الوحيد من أول السلسلة إلى آخرها، ومصادر الحقيقة صارت واحدة في كل موضع:
`archdaemon/jni/Application.mk` و`preloadbin/jni/Application.mk` (`APP_ABI`) · `:app` و`:kernel-flasher`
(`abiFilters` + استثناء تغليف في `:app`) · `dtolnay/rust-toolchain` + كل نداء `cargo ndk` ·
`compile_zip.sh` (دليل `libs/` الوحيد).

**والحدّ الذي لا يُتجاوَز.** الحزمة ترفض 32-بت **رفضًا صريحًا** لا تركيبًا ناقصًا: `mainfiles/customize.sh`
صار يرفض `"arm"` بـ`abort_arch` مع رسالة تسمّي المدعوم وحده، وبعده يُثبِت CI العقد من الطرفين:
الحزمة لا تحمل `libs/armeabi-v7a/`، والـAPK لا يحمل `lib/armeabi-v7a/` (وإن حمل يفشل البناء بأسماء
الملفات)، ويُتحقَّق من الأسماء الخمسة التي يستخرجها المنصّب (كان يُتحقَّق من اسم واحد).

**الدعوى القابلة للتكذيب.** لا سطر في مسار البناء يشير إلى `armv7`/`armeabi-v7a` إلا لحظره، ولا حزمة
تُنتج ثنائيًّا 32-بت، ولا جهاز 32-بت يُركّب ناقصًا. وهذا مُتحقَّق منه ثابتًا (`grep` على مسار البناء +
`yaml.safe_load` + `bash -n`)، و**يحتاج تشغيل CI واحد** ليُثبت الطرفين معًا.

**الحدود المُعلنة.** لم يُقَس شيء على جهاز: لا تركيب على جهاز 32-بت (يُتوقَّع رسالة الإجهاض)، ولا تشغيل
حزمة 64-بت على الجهاز. وملفات 32-بت المتبقّية في المستودع (`kernel-flasher/src/main/jniLibs/armeabi-v7a`)
**لم تُحذف**: حُرسها في التغليف بدل حذفها، فإن أُريد إعادة الدعم يومًا فالمصادر سليمة.

## ADR-40 — بطاقة المصفوفة تعرض **سعة** لا **ضغطًا**، والمدى الحقيقي للترددات يُقرأ من مسار واحد (2026-09-23)

**السياق.** طلب المالك الصريح: «أضف بطاقة تحت بطاقة التخزين… بأفضل تصميم وتناسق مع الشاشة الرئيسية»،
ووقع الاختيار على **إعادة مصفوفة الذاكرة** (RAM · ZRAM · التخزين الداخلي) — وهي التي حذفتها المرحلة ٤ من
خطة `storyboard-home`، وبقيت نصوصها في الموارد **بلا مستهلك** (`home_memory_storage` · `_desc` ·
`home_available_memory` · `home_available_swap` · `home_available_storage` · `home_internal_storage`
— ستة مفاتيح، مترجمة في ٨٤ لغة، واستخدام صفرًا).

**القرار.** ① تُعاد البطاقة **كحقيقة سعة** لا كحُكم: تُعرض المستخدم من الإجمالي والمتاح وأشرطة النسبة،
**بلا عتبات امتلاء وبلا ألوان إنذار**؛ وألوان الصفوف هوية (الابتدائي/الثالثي/الثانوي) لا حكم. والسبب
نصّ `ADR-34`: الامتلاء لا يقيس الضغط (جهاز بـ٩٠٪ سريع وآخر بـ٦٠٪ مخنوق)، فإدخال عتبة امتلاء ملونة هنا
يعني إدخال الحكم المحرّم من الباب الخلفي. ويبقى الحكم في موضعه: `FocusCard` تقول «يكاد يمتلئ»، والمحرك
يقيس PSI. ② وتُستهلك **المفاتيح الستة النائمة** كما هي بلا نصّ جديد، فلا دين ترجمة على إعادة البطاقة.
③ والاستثناء الوحيد: مفتاح مسمّى واحد `home_memory_swap_label` بـ`translatable="false"` (‏`ZRAM` اسم
وحدة لا كلمة)، وهو **استثناء معلن على ADR-14** (لا نظير له في `values-ar` لأنه لا يُترجم أصلًا).

**وأصلح معها عطب مُبلَّغ عنه.** «رسم GPU لا يعمل»: كان الرسم يُبنى من المدى المعلن وحده، فجهاز لا تُعلن
نواته سقفًا (أو لا يردّ مسار البائع) يُنتج `graphCeiling = 0` ⇒ **رسم فارغ** بينما تردداته المقيسة موجودة.
فصار ① الاحتياط `max(ceiling, current, observed)` لا سقفًا مصنوعًا (كل حدّ فيه رقم مقيس أو معلن)،
② وتردّد الرسوم الحالي يُقرأ — عند صمت مسار البائع — من `GpuHardwareBackend` **نفسه** الذي ترسم به شاشة GPU
(مسار واحد لفكرة واحدة)، ③ ومدى الرسوم (أدنى/أعلى) يُحلّ مرة واحدة لكل إقلاع من الجهاز نفسه، ويُعرض كحدّين
تحت الرسم مع خطّ أرضية على مستواه الحقيقي. وأرضية المعالج تُقرأ مع سقفه من عقدة العنقود (`cpuinfo_min_freq`)
في الدورة نفسها — لا نداء جديد في كل دورة قياس. و**ما لا يُعلن يُترك فارغًا**: لا أرضية مصنوعة من أدنى عيّنة.

**الدعوى القابلة للتكذيب.** لا رقم في البطاقة يأتي من مصدر ثانٍ لمصدر الشاشة المالكة له، ولا عتبة امتلاء
تُلوّن شيئًا في هذه البطاقة، ولا سقف/أرضية يُخترعان عند الغياب. والمفاتيح الستة خرجت من قائمة «نصوص بلا
مستهلك»، وواحد فقط أُضيف.

**الحدود المُعلنة.** لم يُبنَ ولم يُشاهد على جهاز: هل تُقرأ عقدة الرسوم فعلًا على الجهاز المُبلّغ عنه (وحدها
تجعل الرسم غير فارغ) **لم يُجرَّب هنا**، ومطابقة `LocaleList.getDefault()` إلى المدعوم لم تُختبر على جهاز
متعدد اللغات. وشارة «نسبة اكتمال الترجمة» داخل منتقي اللغة **لم تُشحن**: تحتاج توليدًا وقت البناء من
`tools/i18n_coverage.py` (علامة `--json` + مهمة Gradle)، وقُدّم تأجيلها على رمي رقم غير مقيس.

## ADR-41 — السطح الأساسي: **أربع وجهات بترتيب المالك**، وMax AI مدخلٌ من البطاقة الأولى لا مقعدٌ خامس (2026-09-24)

**السياق.** أمر المالك الصريح: ترتيب الشريط السفلي **الرئيسية ← التطبيقات ← التحكم ← الإعدادات**، ومدخل
**Max AI** في **البطاقة الأولى** من الرئيسية بدلًا من كلمة «النظام مستقر»، و**السمة** في الإعدادات فوق صفّ
اللغة. وهذا يخالف ما بُنيت عليه `NT-01` («الشريط = Now · Control · Apps · Max AI»)، فالتعارض **مُعلَن لا
مسكوت عنه**: قرار المالك اللاحق ينسخ ترتيب `NT-01`، والرخصة نفسها باقية (السجل `MaxDestination` هو المصدر
الوحيد؛ لا مسار يُكتب مرتين).

**القرار.**
1. **الترتيب مصدره واحد:** `MaxDestination.PrimaryDestinations = listOf(Now, Apps, Control, Settings)`
   هو نفسه ما يُبنى منه الشريط (`MainActivity`) وما يُقاس به ظهوره (`PrimaryRoutes`) — لا قائمة ثانية.
2. **الإعدادات مقعد دائم** (`isPrimary = true`)، وكانت تُفتح من الشريط العلوي وحده (عيب قابلية وصول).
3. **Max AI يُخرج من المقاعد** ويبقى مدخلًا من **البطاقة الأولى** في الرئيسية (وسم `filled` بنجمة واسم،
   `onClick`)، ومن موضعه الثاني القائم (`MaxAiActiveBanner`) — فالخروج من الشريط لم يُيتمّ شاشة.
4. **والسمة تُعاد إلى الإعدادات** (`ColorPalette` أبوه `Settings`، وصفّها أول صفوف مجموعة الرأس فوق اللغة)،
   ولا تُكرَّر في `Control → Tools`: تكرارها كان يعني موضعين يفعلان شيئًا واحدًا.

**الدعوى القابلة للتكذيب.** لا وجهة تُفتح من مكانين مختلفين في الواجهة نفسها، ولا وجهة تُخرج من الشريط
إلا ولها مدخل معلوم في الشجرة (Max AI: البطاقة الأولى + البانر). و`ControlLayoutModelTest` يبقى صادقًا: يربط
كل وجهة أبوها `Control` بالنموذج، فوجهة نقل أبوها إلى `Settings` تخرج من مقارنته لا تُكسره.

**الحدود المُعلنة.** لم يُبنَ ولم يُرَ على شاشة: ترتيب الشريط وثبات المدخل البصري **يحتاجان لقطة من المالك**؛
والشريط يظهر بشرط (`rootStatus && moduleInstalled && currentRoute in primaryRoutes`) فلم يُجرَّب ظهوره بلا
وحدة مثبَّتة.

## ADR-42 — الجذر وShizuku **سطحٌ واحد** بكشفٍ تلقائي وفعلٍ صريح، لا ثلاثة أبواب لموضوع واحد (2026-09-24)

**السياق.** أمر المالك: «دمج صفحة الروت و شيزوكو في شاشة واحدة… بعد تحسين التصميم وايضا امكانية التغيير
في شاشة الاعدادات وان يكون الاكتشاف تلقائي وليس يدوي او الاثنين معا تلقائي ويدوي لا فرق».
والمقيس في الشجرة قبل القرار: للجذر بابان ظاهران (`GetStartedScreen` صفحة ١ + صفّ `root_grant_title` في
`SettingsScreen`)، وللشيزوكو بابٌ ثالث (`PrivilegePanel`)، واللوحة المشتركة كانت **أصلًا** تعرض الاثنين —
فالتَّكرار في الأسطح لا في المنطق، وعلاجه حذفُ الأسطح لا كتابة منطق جديد.

**القرار.**
1. **سطحٌ واحد للامتياز** هو `ui/component/PrivilegePanel.kt`، يُزرع في شاشة البداية وفي الإعدادات معًا —
   ولا يُنسخ نصٌّ من طبقة الامتياز في أي شاشة أخرى (مصدر حقيقة واحد للعبارة والحالة).
2. **القراءة تلقائية، والطلب ضغطة.** الفحص يجري عند فتح السطح وعند العودة إليه، والقراءة **سلبية**:
   `PrivilegeManager.cachedRootGranted()` يقرأ الصدفة المُخزَّنة ولا يستدعي `su`، وShizuku يُستعلَم بـ
   `pingBinder`. ولا يُطلب امتياز عند الإقلاع. وإعادة الفحص اليدوية تبقى متاحة (تلقائي + يدوي معًا).
3. **لا نتيجة عابرة لِما يُقاس:** نتيجة طلب الجذر تُعرض في موضعها داخل السطح (`root_grant_ok/denied`)
   لا في snackbar يختفي — الشرح والنتيجة يبقيان مقروءين.
4. **من الإعدادات يُغيَّر لا يُشاهد فقط:** صفّ الإعدادات الواحد يعرض الطبقة الحالية ويفتح السطح؛ وصفُّ
   طلب الجذر المستقلّ أُزيل مع بقاء نصوصه مستعملة داخل السطح.

**الدعوى القابلة للتكذيب.** لكل طبقة امتياز موضعٌ واحد في الواجهة يُقرأ منه ويُغيَّر؛ ولا يوجد مفتاح
نصّي لطبقة الامتياز مستعملًا في شاشتين مستقلّتين — فأي `PrivilegePanel()` آخر هو نقضٌ لهذا القرار.

**الحدود المُعلنة.** البناء (`:app:compileReleaseKotlin`) أثبت الترجمة لا السلوك: شكل السطح وRTL وحلقة
نافذة الجذر عند فتح صفحة الامتياز تحتاج جهازًا. وتبقى نقطة قرارٍ للمالك: الطلب التلقائي للجذر مرّةً واحدة
عند الوصول إلى الصفحة أُبقي كما كان، وحذفه موضعان معلومان في `GetStartedScreen.kt`.

## ADR-43 — Max Atlas نظام الذكاء والتكيف المركزي: دورة سبع مراحل، وخريطة قدرة بسبع حالات، ومواءِمون لا قلبٌ يُعاد (2026-09-24)

**السياق.** أمر المالك: «أعد هندسة وتطوير Max Atlas ليكون نظام الذكاء والتكيف المركزي في MaxManager، وليس
مجرد نسخة أقوى من Max AI… Atlas = كيف أجعل MaxManager يعمل على هذا الجهاز؟ Max AI = ماذا يجب أن أفعل الآن
لتحسين هذا الهاتف؟ لا تنقل مسؤوليات Max AI إلى Atlas، ولا تجعل Atlas مجرد محرك Performance آخر»، ودورة
`Discover → Understand → Map → Adapt → Execute → Verify → Learn`، وأعمدة `Capability Discovery + Adapters +
Device Profiles + Runtime Verification` حتى يُضاف جهاز/كيرنل جديد **دون إعادة كتابة القلب**. والمقيس قبل
القرار: أطلس كان يملك خمس مراحل من الدورة (اكتشاف محدود الميزانية · مفردات دلالية · تنفيذ بمعاملة مُتحقَّقة ·
تحقق قراءة مرتجعة · تعلّم لكل جهاز) وياله من **Map** ولا **Adapt** ولا ملف جهاز ولا واجهة مركزية — وكل مقبض
كتابة له مُنتِج واحد مكتوب بيده.

**القرار.**
1. **الدورة ملكٌ واحد لكل مرحلة، ومواقعها في الشيفرة معلومة:** Discover/Understand = `core/atlas`
   (`AtlasDiscovery`/`AtlasBackendProvider`/`AtlasModels`) · **Map = `AtlasCapabilityMap` + `AtlasSafetyPolicy`** ·
   **Adapt = `core/hardware/AtlasAdapters`** (عقد مُلاءِم + سجلّ حتميّ + مُلاءِم مُتحقَّق واحد على الأقل) ·
   Execute = `AtlasAdaptiveExecutor` ← `HardwareRepairExecutor` ← المُحكِّم (حدّ المعاملة الوحيد كما كان) ·
   Verify = `HardwareVerification`/`WriteVerification` + نافذة التأكيد · Learn = `AtlasRouteMemory`/`AtlasEvidenceStore`.
   و`MaxAtlas` واجهةٌ تركيبية تجمع الدورة وتكتب **جدول الفصل Atlas ≠ Max AI** في صدرها: أطلس لا هدف ولا
   أولوية ولا «متى»، وMAX AI لا يكتشف عتادًا ولا يختار مسارًا ولا يكتب مباشرة.
2. **خريطة القدرة بسبع حالات وأسبقية ثابتة:** `NEVER_TOUCH` (السلامة فوق الكل) ← `SUPPORTED` (مسار مؤهل
   **و**نتيجة مُثبتة على هذا الجهاز — وحدها تدّعي نجاحًا) ← `WRITABLE` ← `NEEDS_ADAPTER` (الجهاز يُظهر سطحًا
   ولا مسار لنا عليه) ← `UNAVAILABLE` (**غياب مُثبت** بجردٍ لا فارغ، لا فشل قراءة) ← `READ_ONLY` ← `UNKNOWN`
   («لم يُقَس» و«قِيس فلم يُجب» برمزين مختلفين). وجدول الاشتقاق **واحد** (`AtlasCapabilityRules`) يخدم مَن
   التمساه: التشخيص والتنفيذ والتمثيل — فلا يُجيب سطحان بجوابين على سؤال واحد.
3. **قائمة عدم اللمس مُراجَعة فوق الجميع:** نقاط الحرارة ومحاكاتها · panic/sysrq · watchdog · `/sys/power` ·
   uevent — بمطابقة مغلقة الفشل على المفتاح والمسار معًا، يرفضها المُلاءِم قبل البناء و`MaxAtlas` قبل التنفيذ،
   ولا يرفعها نجاحٌ قديم ولا مُلاءِم ولا ذاكرة. ويبقى `AtlasSafetyClass` في كتالوجَي القراءة **`READ_ONLY` كما
   كان**: حالات الخريطة أحكامٌ على مسارات لا سلطة على العتاد، والسلطة تبقى للمُحكِّم وحده.
4. **ملف الجهاز يُبنى ولا يُخزَّن:** الهوية + الخريطة + ما تعلّمته الذاكرة، بصلاحيات صريحة (جيل إقلاع · جيل
   امتياز) — جيلٌ آخر يبطل المعرفة فلا تتحوّل إلى افتراض ثابت، وإعادة البناء هي التحديث. (`STATIC` لا تنتهي
   بالساعة — قِيس لا فُرِض.)
5. **التوسعة = مُلاءِم جديد لا تعديل قلب:** أي جهاز/كيرنل/طريقة تُضاف بتسجيل `AtlasControlAdapter` في
   `AtlasAdapterRegistry` (assess مُقاس · plan بمعاملة · probe لا يُنفَّذ)، وغيابُ الجميع فجوة `Gap` مُسمّاة
   السبب لا صمت ولا مسار مُختلَق. وكتبٌ جديدة تُصنَّف في `AtlasArchitectureTest.declaredOffReadPath` بأسبابها.

**الدعوى القابلة للتكذيب.** (١) لا مسار كتابة في المشروع يتجاوز حدّ المعاملة المُتحقَّق، و`CpuCeilingAdapter`
يلفّ `AtlasDiscoveredControl` ولا يملك كاتبًا. (٢) لا حالة `SUPPORTED` بلا نتيجة مُثبتة على الجهاز نفسه — يُقاس
في `MaxAtlasTest` («مدعوم» بعد `VERIFIED` وحده). (٣) عقدة ممنوعة تُرفض **قبل** فعل الكتابة في موضعين
(ملاءِم + واجهة). (٤) جهاز لا ملاءِم له يقول `NEEDS_ADAPTER` لا يصمت ولا يدّعي. (٥) إضافة ملف إلى
`core/atlas` بلا تصنيف تُسقط `AtlasArchitectureTest`.

**الحدود المُعلنة.** مراجعة السلامة المستقلة **علَقت ولم تُغلق** (الـdiff يمسّ `core/hardware`/`core/maxai`، ولا أداة
استدعاء في الجلسة)؛ والبناء (`:app:testReleaseUnitTest` = 1474 ناجحًا) يثبت الترجمة ومنطق JVM لا سلوك عتاد؛
وملاءِما GPU/Thermal وسطح عرض ملف الجهاز مُسجَّلون في `NEXT_TASK` كتوسعة لا ك חוב.

### ADR-43.1 — عقدا GPU المُقيسان (2026-09-24، `ATLAS-ADAPT-02`)

أُضيف للمواءمة ملاءِما GPU (`GpuDevfreqCeilingAdapter` · `GpuOppPinCeilingAdapter` في `AtlasGpuAdapters`،
و`ControlRegistry.gpuCeilingControl` صار مُستهلكهما الإنتاجي: «ماذا» لـMAX AI و«كيف» لأطلس)، وأثبتت
الاختبارات عقدين مُلزمانَين لكل مُلاءِم قادم — وكلاهما خرج من مُكذِّبة لا من رأي:

1. **عقد `realized` = «بلغت» لا «تحتوي»** — `ceilingSatisfied` معناها `node ≤ desired`، وبوصفه عقد
   التخطّي (`HardwareRepairRequest.realized`) قُرئ سقفٌ أدنى من الطلب «مُبلَّغًا فعلًا» فتُتخطّى
   الكتابة فلا تُكتب قيمة أعلى أبدًا (ظهر في الاختبار: `writes=[]` مع `VERIFIED`). فكل عقد `realized`
   يُثبت **تساوي القيمة** (أو تحريرًا مُثبتًا عند القدرة) — وهي قاعدة `HardwareVerification.ceilingReached`
   نفسها في مقبض CPU.
2. **التحرير فعلٌ لا يُنفَّذ من داخل `apply`** — عند القدرة يُعاد خط الأساس بعد المطابقة الفاشلة فيُمحى
   التحرير («Performance بلا أثر» المقيس). فالتحرير (`releaseVendorCeiling`) يُنفَّذ **بلا كتابة تردد**،
   والرفع فوق السقف الحيّ يُحرَّر أولًا (`releaseRequiredForRetarget` بـ`plannedRelease=false`:
   القرار يُقاس لا يُخطَّط).

ووحدتا الأرقام محسومتان في العقد: نصوص المعاملة بوحدة العقدة كما تحفظها `GpuHardwareBackend.Device`،
و`hzMultiplier` هو المحوِّل الوحيد إلى لغة `GpuCeilingPolicy` (Hz) — فلا تُقارَن أرقامٌ بوحدتين أبدًا.

**دعاواهما القابلتان للتكذيب.** (١) ملاءِمٌ يجعل `realized` «يحتوي» يُسقط اختبار «الكتابة تقع وتُقرأ»
(`writes=[…]` لا `[]` مع `VERIFIED`). (٢) تحريرٌ يُنفَّذ من داخل `apply` يُسقط اختبار «الطلب عند القدرة
يُعلن رقم ما لا نملكه ولا يفشل مُكرَّرًا».

### ADR-43.2 — عقدا صِدق الخريطة وتبعُد التقرير (2026-09-24، `ATLAS-MAP-02`)

1. **«يحتاج مُلاءِمًا» ≠ «للقراءة فقط»** — مدخل `writeExpected` في جدول الاشتقاق، وحكمه المُلزم:
   **طلب تحكم وصل ⇒ كتابة متوقّعة دائمًا** (فغياب الطريقة فجوةٌ تُصلَّح)، والخريطة وحدها تسأل
   «هل يدّعي هذا البناء طريقة؟» (حضور ملاءِم مُسجَّل) — فمراقبةٌ تقرأ ولا تكتب تُقرأ `READ_ONLY`،
   ولا يَعِد صفٌّ بمسار لا يحتاجه أحد.
2. **تقرير الدعم يحمل الخريطة رموزًا لا جملًا** (`map:<state>:<reason>` + معرّف ملاءِم + `verified`)
   — والطرح يبقى صفرًا: لا حقل يحتمل مسارًا أو رقمًا أو سرّ. و`SCHEMA 2` مع **قبول v1 صراحةً**
   في `decode`: مرفق الأمس يُقرأ ولا يُرفض، وإلا صار الإصدار الموثَّق امتناعًا عن إعادة القراءة.

**دعاواهما القابلتان للتكذيب.** (١) مراقبةٌ تقرأ وتُصنَّف «يحتاج مُلاءِمًا» يُسقط اختبار
`a monitoring-only surface that reads is read-only…`. (٢) رفض مرفق v1 يُسقوط اختبار
`a report written before capability rows existed is still readable`.

## ADR-44 — **Android 17 (API 37) و32-بت معًا**: إعادة العمودين إلى السلسلة كلها (2026-09-24)

**السياق.** طلب المالك: «اجعل تطبيقي يدعم أندرويد 17 ويدعم هواتف 32-بت» — وهو عكس ADR-39 حرفًا،
ومبرّره صريح: «أريد أن يعمل على كل أندرويد دون مشاكل»، وأرفق مشروعه القديم (`Dragon`) الذي كان يحمل
الخط الكامل لـ`arm64-v8a` و`armeabi-v7a` معًا. **وقبل التنفيذ قِيس الواقع لا ذُكِر:** 
`platforms;android-37.0` و`build-tools;37.0.0` **منشوران ومستقرّان** اليوم (فالتقييد القديم «API 37
غير منشور في مستودع SDK» كان صحيحًا وقته، وقد انتهى)، و`armeabi-v7a` يملك **ثنائيات جاهزة في المستودع
ذاته** (`kernel-flasher/src/main/jniLibs/armeabi-v7a`) كان الحرس القديم يحجبها عن جهازها بالاستثناء.

**القرار.** يُبنى العمودان ويُشحنان معًا، وAPI 37 يُعلَن: `compileSdk = 37` و`targetSdk = 37` في `:app`
و`:terminal-emulator` و`:terminal-view` و`:kernel-flasher`؛ و`abiFilters` = `arm64-v8a` + `armeabi-v7a`
في `:app` و`:kernel-flasher`؛ و`APP_ABI := arm64-v8a armeabi-v7a` في `archdaemon` و`preloadbin`؛ وهدفا
Rust (`aarch64-linux-android` + `armv7-linux-androideabi`) في كل نداء `cargo ndk` وفي `dtolnay/rust-toolchain`؛
و`compile_zip.sh` يملأ `libs/armeabi-v7a/` بالمعيار **المشدَّد** نفسه (`copy_binary` يرفض المفقود بالاسم
والمسار — لا `|| true` الذي كان في المشروع القديم)؛ و`customize.sh` يختار `ARCH_TMP` بحسب `ARCH`.

**والحُرّاس انقلبت لا أُزيلت** — وهذا جوهر الفرق عن ADR-39: كان CI يمنع وجود 32-بت عمدًا، وصار يطالب
بالعمودين عمدًا: `lib/arm64-v8a/` **و**`lib/armeabi-v7a/` في الـAPK، والخمسة أسماء المستخرجة في
`libs/<abi>/` لكل عمود، و`libtermux.so` يُبنى للعمودين **قبل Gradle** (كان مُلتزمًا لـ64-بت وحده، فكان
`System.loadLibrary("termux")` يسقط على هاتف 32-بت عند فتح الطرفية).

**وحجبت الترقية بأمان:** `compose-bom` و`material3` و`navigation` بقيت على إصداراتها المُثبَّتة رغم أن
مانعها (سقف SDK 36) زال — لأن رفع سطر Compose/Material كله جولة مقيسة مستقلة، لا أثرًا جانبيًّا لتغيير SDK.

**الدعوى القابلة للتكذيب.** (١) `:app:compileReleaseKotlin` يفشل إن لم يقبل AGP مستوى API 37.
(٢) اختبارات `:app:testReleaseUnitTest` تسقط إن غيّر الـSDK سلوكًا مقيسًا. (٣) فحص `aapt2 dump badging`
يُظهر `targetSdkVersion` غير `37` أو `native-code` بعمود واحد — فيُكذَّب القرار بأمر واحد.

**الحدّ المُعلن.** المُقاس هنا: الترجمة على API 37، و**1495 اختبارًا** جَرَت على الإعداد الجديد، و**APK
مُفحوص المحتوى** (`targetSdkVersion:'37'` · `compileSdkVersionCodename:'17'` · `native-code: 'arm64-v8a'
'armeabi-v7a'` · ٥ مكتبات v7a داخل الحزمة). **وما لا يُقاس محليًّا:** الثنائيات الأصلية للعمودين
(`ndk-build`/`cargo ndk` — تحتاج NDK، وCI هو من يبنيها)، وتركيب على هاتف 32-بت حقيقي.

## ADR-45 — **قاعدة النقل إلى Rust**: ما يُنقل وما لا يُنقل، ولماذا (2026-09-24)

**السياق.** أمر المالك: «حوّل كل شيء يحتاج إلى ويستحسن إلى Rust بعد وضع خطة… وإذا كان النقل يحسّن ولو بنسبة
بسيطة فلا تتردد». والتحقيق العميق أنتج أن **الاختناق ليس اللغة**: التطبيق يفتح **صدفة جذر لكل قراءة** في
٢١٠ موضعًا، ويعيد قراءة الأنواع الثابتة كل دورتين، ويُقرأ كل عقدة عبر **معاملة IPC منفردة**، وخدمة المراقبة
تنبض كل **٥٠٠ مللي** بكتابتين و`fsync`.

**القرار — قاعدة الحدّ:** يُنقل إلى Rust **ما هو قراءة أو معالجة كثيفة على حدود النظام**:
قراءة sysfs/`/proc` مجمَّعة · مسح المناطق الحرارية · عيّنات العتاد الدورية · تحليل نصوص كبيرة ·
ضغط/تجزئة/أرشيف. **ولا يُنقل:** واجهة Compose · سياسة أطلس وأحكامه · المُحكِّم والملكية (ADR-11) ·
وما يُختبَر بعقود نصية في JVM. والصيغة المختصرة: **ما كان قرارًا يبقى Kotlin، وما كان قراءة/معالجة كثيفة يصير Rust.**

**والمسار الموجي** (التفصيل ومعايير القبول في `NEXT_TASK` §`RUST-PLAN-01`):
**٠** المعمارية أولًا — دفعات IPC + كاش بجيل الإقلاع + قراءات مباشرة بدل الصدَف (**منجزة، Kotlin**، وهي ما
سيصير لاحقًا تنفيذًا بلغة أخرى خلف العقد نفسه) · **١** قارئ مجمَّع بلغة Rust داخل عملية الجذر ·
**٢** خادم عيّنات دائم يُخرج حلقة ٥٠٠ مللي من عملية التطبيق · **٣** محلّل السجلات والتجزئة/الأرشيف.

**ولماذا الموجة ٠ بلغة Kotlin ليست تأخيرًا:** العقد (دفعة واحدة لكل دورة، وكاش إقلاع) هو ما يجعل نقل التنفيذ
لاحقًا **تغييرًا موضعيًّا بلا مساس بالمتصلين**؛ لو نُقلت القراءة إلى Rust قبل ذلك لكانت النتيجة نفس عدد
الرحلات بلغة أخرى — أي أن اللغة لا تُصلح عطبًا معماريًّا.

**الدعوى القابلة للتكذيب.** كل موجة تُثبت «قبل/بعد» برقم مقيس، أو تُعلن «غير مُتحقَّقة في هذه البيئة»:
(١) دفعة الحرارة: `IMtkService.readNodes` تجعل قراءات N منطقة **معاملة واحدة** — يُمتحن بمراجعة
`RootFileAccess.readMany` وسقوطه الحرفي إلى `read()`؛ (٢) الموجات ١-٣ تُقاس بـ`cargo test` على شجرة sysfs
وهمية + زمن دورة على جهاز — وما لم يُثبَّت يُعلن.

**وحدّ لا يُتجاوَز:** **لا يُنقل أي مسار كتابة عتاد** إلى Rust قبل حكم سلامة (ADR-07/11/16)، ولا تُبنى مكتبة
جديدة بلا مُستدعٍ إنتاجي — «الكود بلا مستهلك دَين».

## ADR-46 — **الموجة ١ من النقل إلى Rust منفَّذة**: قارئ دفعات داخل العملية، وسلّم سقوط يحفظ القدرة (2026-09-24)

**القرار:** يُضاف في `manager/src/main/rust` وحدة `probe` (مُصدَّرة عبر JNI إلى
`nd.max.core.jni.ProbeBridge`) تقرأ **عدة عقد في نداء واحد** داخل العملية: قيمًا (`read_many`)، ووجودًا
(`existing`)، وأسماء مجلد (`list_names`). وتُصبح **الطبقة الأولى** في `RootFileAccess` قبل IPC الجذر
والصدفة. والوحدة **قراءة فقط**: لا كتابة ولا `chmod` ولا حذف — فلا تمسّ ADR-11 ولا تفتح بابًا جانبيًّا للامتياز.

**السلّم (وهو جوهر القرار):** قارئ أصلي ← **ما لم يُقرأ وحده** يُسأل عبر معاملة IPC واحدة إلى
`MtkRootService` ← ثم `read()` لكل عقدة (صدفة الجذر آخرًا). فالسرعة **لا تُشترى بقدرة**: كل عقدة كانت
تُقرأ بالجذر تبقى تُقرأ بالجذر، ولا تُخفى عقدة ولا تُفترض قيمة، والعقدة الغائبة من كل الطبقات تبقى `null`.

**عقد الصيغة بين لغتين — يُحرَس من الجانبين:** `paths` سطر لكل مسار · `values` سطر لكل مسار بالترتيب
(الفارغ = لم تُقرأ) · `\u0001` = سطر جديد داخل القيمة · `flags` = `1`/`0` لكل مسار. والاختبارات على
**نفس المُدخلات حرفيًّا** في Rust (`probe::tests::same_vectors`) وKotlin (`ProbePacketTest`)، فأي انحراف
على أي جانب يُسقط اختبارًا — لا يمرّ صامتًا. والمحاذاة تُرفض عند أي اختلاف في عدد الأسطر: **حزمة مشوَّهة
أخطر من غيابها**، لأنها تنسب قيمة عقدة إلى عقدة أخرى.

**وأخبار مقيسة تُضاف إلى القاعدة (لا استثناءات مُعلَنة بلا رقم):**

* **`AppMonitor` لا يحتاج إصلاح إهدار:** كاتبا الحالة وتطبيقات الخلفية **يُحجمان بالتبدّل أصلًا**
  (`if (currentStatus == lastStatus) return`) — فلا تُكتب الآن ملفات كل ٥٠٠ مللي بلا تغيّر، ولا عطب يُدَّعى.
* **نقل أطلس لم يُنفَّذ، وهذا صواب:** نقله الفعلي (`AtlasFileReadTransport`) يقرأ بـ`java.nio.file` **داخل
  العملية** بلا صدفة ولا binder، مع تعيين دقيق لكل `errno` (ABSENT ≠ PERMISSION_DENIED ≠ UNKNOWN_CAUSE)
  وحدّ بايتات وميزانية روابط معدودة. دفعة Rust ستُفقد **أسباب الفشل الدقيقة** التي تفرضها مواصفة أطلس
  («إن لم يجد طريقة موثوقة يقول غير مدعوم») مقابل ربح لا يقيسه شيء — **فلا يُنقل**.
* **اختبار Rust متعفّن وُجد عند أول تشغيل حقيقي:** `power_predictor::tests::nan_never_enters_history` كان
  يسأل عن المحور الخطأ ويفشل. وسببه بنيوي: **لا خط CI واحد كان يشغّل `cargo test`** لـcrate التطبيق (كان
  يُبنى للـABIs فقط). فأُصلح الاختبار **وأُضيف خط `cargo test` (مضيف) في CI** — قاعدة تكتب نفسها: **كل ما
  يُبنى بلا اختبار يُشغَّل يتعفّن**.

**الدعوى القابلة للتكذيب:** عدد الرحلات في مسار الدفعة = **نداء واحد** لكل مجموعة عقد (مُثبَّت بالبنية
واختبارات الجانبين)، وزمن الدورة/الفتحة قبل-بعد على جهاز **يحتاج جهازًا** — ولا يُدَّعى هنا.

## ADR-47 — **الموجة ٣**: تحليل السجلّات **دفعةً** في Rust، والموجة ١.ب **لا تُنفَّذ** لسبب مقيس (2026-09-24)

**القرار (أ):** يُنقل تحليل سطور السجلّات إلى Rust في وحدة `logparse`، لكن **دفعةً لا سطرًا سطرًا**:
سطور logcat تُخزَّن خامًا في نافذة الإفراغ (٣٠٠ مللي) ثم تُحلَّل كلها في **نداء JNI واحد**، فيصير
عدد العبور إلى المترجم مرّتين إلى ثلاث في الثانية بدل نداء لكل سطر.

**وهذا يقلب قرارًا سابقًا بالقياس لا بالمزاج:** كان ADR-45 يقول «محلّل السجلات: يحتاج قياسًا قبل
النقل». والقياس جرى هنا على نفس الآلة ونفس الأسطر (٢٠٠ ألف سطر نمطي):

| الجانب | الزمن | الدليل |
| --- | --- | --- |
| Kotlin/JVM (النمط المرجعي، بعد تسخين JIT) | **٢٧٦ مللي** | قياس مؤقت ثم **حُذف** (لا يبقى في المستودع) |
| Rust (`parse_batch_packed`، release) | **١٨٠ مللي** | ‏**×1.5**، وقيمة `cargo test` نفسه تشغيلًا |

وربح النقل ليس في النسبة وحدها: مطابقة Kotlin تُنتج لكل سطر `MatchResult` وسبع مجموعات وثمانية نصوص
جزئية ⇒ **مليون وستمائة ألف كائن** لكل ٢٠٠ ألف سطر على كومة التطبيق، والعمل بهذا الشكل يعود إلى JVM
في كل نافذة. **وحدّه المعلن:** الرقم على مضيف x86 بـHotSpot؛ وزمن ART على جهاز **يحتاج جهازًا** — ولا
يُدَّعى.

**القرار (ب):** **الموجة ١.ب (نقل القراءة إلى داخل عملية الجذر عبر `.so`) لا تُنفَّذ.** وسببها مُقاس من
الكود: `MtkRootService` (وراثة `libsu`'s `RootService`) ينفّذ `readNode` بـ`File(path).readText()`
**داخل عملية الجذر** أصلًا — أي قراءة ملف محلية بلا صدفة وبلا معاملة إضافية. فنقلها إلى Rust لا يوفر
رحلةً واحدة، ويقايض ذلك بوضع فشل جديد: تحميل مكتبة أصلية في عملية `app_process` مطلقة بالجذر (مسارات
المكتبات فيها ليست مسارات الحزمة). والقاعدة التي تحكم: **لا يُقايض سقوطٌ محتمل بربحٍ غير مقيس**.

**السلوك لا يتغيّر في الحالتين:** `logparse` **محاكاة حرفية** لنمطَي Kotlin (بما فيهما `\s` الـASCII)،
وفرع الاحتياط والإسقاط منقولان كما هما. وأمّا `isBlank()` فقد **صحّحت قياسًا لا اجتهادًا**: Kotlin تبنيها على
`Character.isWhitespace(c) || Character.isSpaceChar(c)` — فالاتّحاد يجعل **U+00A0 فراغًا** (خلافًا لـJava's
isWhitespace وحدها)، ويُخرج U+0085 (NEL) من الفراغ، ويشمل `U+001C..U+001F`. وكانت محاكاتي الأولى تستثني
U+00A0 خطأً، فأمسك الانحراف **اختبار JVM على الجانبين** (رسب عند أول تشغيل) قبل أن يظهر فرق في تحليل
سجلّات جهاز حقيقي — وهذا هو سبب وجود الاختبارات العابرة للّغات أصلًا. ويُحرَس
التطابق بحارسين: حارس **رموز JNI** (كل `external fun` له تصدير `Java_...` بالاسم نفسه، وكل تصدير له
مستدعٍ — فلا `UnsatisfiedLinkError` على جهاز ولا سطح ميت)، وحارس **تطابق النمطين** (النمطان الحرفيان
في `LogsViewerViewModel` يجب أن يكونا مذكورين في وحدة Rust نفسها). والمحلّل المرجعي الكامل يبقى في
الشجرة مسارَ سقوط عند غياب المكتبة — فيُختبر هو في JVM كما كان.

## ADR-48 — **بوابة رموز JNI**: ما يقرأ الثنائية فعلًا، وما يُعلن عجزه (2026-09-24)

**القرار:** تُضاف أداة `tools/jni_symbols.py` بثلاث طبقات، تُشغَّل في CI مرّتين — قبل البناء (طبقة المصدر،
ثوانٍ) وبعد بناء ثنائيي المكتبتين (الطبقة الفعلية). وصارت **البوابة الرابعة** إلى جانب `kt_balance`
و`code_health` و`i18n_coverage`.

**لماذا وُجدت (وما لا تكفيه البوابات القائمة):** `external fun nativeReadManyPacked` مع رمز غائب في `.so`
**لا يُسقط ترجمةً ولا بوابة بنية ولا فحص موارد** — بل `UnsatisfiedLinkError` عند أول نداء على الجهاز،
وهو المكان الوحيد الذي لا نقيس فيه. وصنفه وقع هنا فعلًا: `libtermux.so` كانت ٦٤-بت وحدها.

**الطبقات وحدّ كل واحدة:** (١) المصدر: كل `external fun`/`native` في Kotlin/Java ↔ `Java_*` في Rust —
تعمل بلا مُصرّف وبلا جهاز. (٢) الثنائيات: `nm -D --defined-only` لكل `.so` مُشحونة: نواقص · يتامى ·
**انحراف ABIs** · **تغطية الأعمدة**. (٣) ما لم يُبنَ (لا NDK محليًّا) يُعلن «غير مُتحقَّق» ولا يُدّعى.

**خمسة قرارات ملزمة داخل الأداة:**

1. **التصريح يُنسب إلى ثنائيته من `System.loadLibrary`** في ملفه نفسه — لا من جدول مخفي يُصان يدويًّا؛
   وملف بلا نداء تحميل يُعلن «مكتبة غير معروفة» ويُسقط البوابة بدل التخمين، ونطاق الطبقة ١ محصور
   بمكتبات Rust المستخرَجة من `Cargo.toml` و`Android.mk` (وإلا نُسب تنفيذ C إلى Rust فصار عطبًا وهميًّا دائمًا).
2. **الرمز اليتيم تحذيرٌ لا عطب** (ADR-18): كود ميت لا يضرّ، وإسقاط إصدار من أجله حذفٌ لعمل. و`--strict`
   لمن أراد عكسه صريحًا.
3. **`--require-binaries` هي التي تجعل غياب الثنائية عطبًا — ووضع CI وحده.** فتشغيل المالك محليًّا (بلا NDK)
   يبقى ناجحًا ويقول بصوت مسموع: «هذه الطبقة غير مُتحقّقة».
4. **تغطية الأعمدة** (مكتبة لها عقد في عمود وغائبة في آخر) عطبٌ — **وهي صنف عطب `libtermux` نفسه** —
   ولا تنشط إلا إذا كان في الشجرة أكثر من عمود، فلا عطب وهمي في بيئة بعمود واحد.
5. **الأداة تُقاس على نفسها (‏27 حالة) وتُصرَّف ثنائية C حقيقية ثم تُقرأ بـ`nm`** — لأن قياس تحليل نصّ
   لا يقيس استدعاء الأداة. وهذا ليس تفصيلًا: **أربعة عيوب في الأداة نفسها** أمسكها هذا القياس وهي تُبنى
   (‏`U` تُقرأ مُعرَّفة · أعمار Rust تبتلع سطورًا · نقطة الحزمة تُشفَّر `_0002e` فتفرغ قائمة الأيتام
   وتُخفي عطبًا حقيقيًّا · وقيمة افتراضية مُجمَّدة تُعطّل اختبار التكذيب) — وكلّها كانت **تشهد زورًا**.

**ودرس يعمّ كل بوابة (‏`code_health`):** أداة تسأل عن بيئتها وتقرأ الفشل عطبًا **تُدرَّب الناس على
تجاهلها**. `stray_root_file` كان يسقط في بيئة بلا git على ملف **مذكور في `.gitignore`** (سؤال git يعود
128 ⇒ «غير متعقّب وغير متجاهَل»). فصار السؤال الأول «هل git صالح؟»، وحين لا، يُبلَّغ الملف
`stray_root_file_unverified` — **لا يُسقط الصحة، ولا يُخفى**.

**الدعوى القابلة للتكذيب:** كل رمز يُسقطه أحد الجانبين يُمسك من الجانبين — مُثبت باختبارات تكذيب على
نسخ من ملفات المستودع الحقيقية (تصدير Rust مسقَط ⇒ «ناقص مصدر» · تصريح Kotlin مسقَط ⇒ «يتيم مصدر» ·
رمز مسقَط في ثنائية مُصرَّفة ⇒ «ناقص ثنائية»)، وبمحاكاة CI بأربع ثنائيات في العمودين (خضراء) وبثنائية في
عمود واحد (حمراء).**وحدّها:** `nm` يعطي **الأسماء لا التوقيعات** — فعدد المعاملات وأنواعها وحال `static` تحتاج مُصرّفًا، ولا يُدّعى فحصها.

## ADR-49 — **الموجة ٣.ب**: ضغط zip إلى Rust **مشروطًا بالمُحرّك**، والفكّ يبقى Kotlin بحكم مقيس (2026-09-24)

**القرار (أ) — الضغط يُنقل:** تُضاف وحدة `archive` في `manager/src/main/rust` (تُصدَّر عبر
`ArchiveBridge.nativeCreateZip`) وتصير **الطبقة الأولى** في `FileArchiveEngine.createZip`، ويبقى تنفيذ Kotlin
**مسار سقوط معلنًا** كما في بقية الجسور.

**والنقل مشروط بمُحرّك بعينه، لا بـ«Rust»:** التبعية مُثبَّتة على `zip` + `flate2/zlib-rs` **عمدًا**، لأن
المُحرّك الافتراضي النقيّ ‏(miniz_oxide) **أبطأ من zlib الأصلية** — أي أن نقلًا بغير هذا الشرط كان سيُبطئ
الميزة. والقياس على شجرتين حقيقيتين من المستودع نفسه، وبنفس دلالات `FileArchiveEngine` (ترتيب أبجدي
بوحدات UTF-16 · مجلد فارغ بشرطة أخيرة · استثناء الأرشيف نفسه · سقف عمق ٤٠) وبنفس الدفعة ٦٤ ك ونفس
المستوى ٦ (وهو افتراضي `Deflater` عند JDK):

| العملية | الشجرة | JDK `java.util.zip` | Rust + miniz_oxide | Rust + **zlib-rs** |
| --- | --- | --- | --- | --- |
| ضغط | ٥.٣ ميجا · ٤٢١ ملفًا (نصّ) | 211ms | 217ms (**×0.97 — أبطأ**) | **107ms (×1.97)** |
| ضغط | ٣٧.١ ميجا · ١١٥١ ملفًا (صور/خطوط) | 1321ms | 1387ms (**×0.95 — أبطأ**) | **706ms (×1.87)** |
| فكّ | ٤٢١ مدخلًا | 96ms | 44ms (×2.2) | **34ms (×2.8)** |
| فكّ | ١١٥١ مدخلًا | 261ms | 183ms (×1.4) | **173ms (×1.5)** |

**والصحّة مقيسة لا مُفترضة:** أرشيف Rust فكّه Java مطابق للشجرة الأصلية بايتًا ببايت، وأرشيف Java فكّه
Rust كذلك، و**قائمة المداخل متطابقة بالاسم والترتيب** (٤٢١ = ٤٢١) — فالتبادل بين المكدّسين ليس تخمينًا.

**القرار (ب) — الفكّ لا يُنقل، وهذا هو الأهم:** ربحه المقيس ‎٠.١–٠.٤‎ ثانية، ومقابله تنفيذ **ثانٍ** لقاعدة
أمنية قائمة — `zip-slip`: رفض مدخل غير آمن (`../` · مسار مطلق · `C:`) **قبل** أول بايت يُكتب، وهو ما
يُختبَر في Kotlin باختبار يفحص **أين كُتب** لا «هل نجح الفكّ». ومسار الفكّ المحور يبقى موضعًا واحدًا
تُحرَس فيه تلك القاعدة (ونصف جدول القياس أعلاه لا يزال مُسجَّلًا، فللرجوع عنه عُدّة أرقام لا مزاج).
و**حارسه:** `ArchiveBridgeSymbolTest.extraction deliberately has no native bridge` — إضافة جسر فكّ يومًا
تُسقط اختبارًا يُسأل صاحبه عن حكم سلامة.

**الثمن معلن رقمًا:** المكتبة الأصلية ‎729,904‎ بايت ← **‎1,097,280‎** (+367KB · +50٪) بـzlib-rs،
و‎1,009,136‎ (+279KB) بـminiz — أي ≈‎+0.73‎ ميجا في الحزمة للعمودين (arm64-v8a + armeabi-v7a). وهذا ثمن
يُدفع من حجم الإصدار مقابل ٢× في عملية يلمسها المستخدم مباشرة (ضغط مجلد) — وهو قرار مالك لا قرار تنفيذ.

**ثلاث قواعد سلوكية لا تتغيّر بالنقل:**

1. **التقدّم:** المسار الأصلي يكتب في نداء واحد فلا يُبلّغ تقدّمًا، فمن طلب تقدّمًا حقيقيًّا
   (‏`progress !== NO_PROGRESS`) أخذ مسار Kotlin بنفس دلالته السابقة — فلا شريطًا يتجمّد ولا تقدّمًا مُختلقًا.
2. **`NoSources` يُحكم في Kotlin بلا رحلة** — حكم يعرفه الطرفان ولا يحتاج مكتبة أصلية.
3. **سبب مجهول لا يُبتلع:** ما لا نعرفه من الأسباب الرمزية يُعلن `WriteFailed`، ولا يصير «نجاحًا» ولا
   «لا مصادر»؛ وردّ مشوَّه يُرفض كاملًا فيعود المتصل إلى Kotlin (لا محاذاة مخمّنة).

**الدعوى القابلة للتكذيب:** الضغط بالمسار الأصلي أسرع من JDK بنحو **×١.٩** على الشجرتين — ويُبطلها أن
يُقاس أبطأ أو مساويًا على جهاز حقيقي. **وحدودها:** الأرقام على مضيف x86-64 بـJDK 17/5 HotSpot؛ وزمن ART
على هاتف **يحتاج جهازًا** · وبناء ثنائيات Android للعمودين **في CI وحده** (لا NDK محليًّا) · ومقارنة الفكّ
جزئيًّا بين نمطين مختلفين للإدخال (‏JDK يبثّ تسلسليًّا بعد مسح مركزي، وRust يقرأ الفهرس ثم يقفز)
— وهذا مُعلن لا مستور.

## ADR-50 — **الموجة ٣.ج**: مسح المساحة إلى Rust — §مُسجَّل متأخرًا (§0.3: التوثيق آخرًا)

**القرار:** تمشية شجرة `StorageUtil.scan` تُنقل إلى وحدة `scan` في `manager/src/main/rust`، وتُصدَّر عبر
`ScanBridge` بثلاثة رموز لا رمز واحد (نداء يعمل · قراءة عدّاد التقدّم الحيّ · طلب إلغاء)، وتبقى حلقة
Kotlin **مسار سقوط معلنًا** في `StorageUtil.scan`.

**والقياس قبل النقل** (شجرتان حقيقيتان · ٢٣,٢٢٤ و١٨,٩٦٥ ملفًا · نفس التصنيف ونفس الترتيب):
**JDK 447ms ← Rust 209ms (×2.1)** و**JDK 285ms ← Rust 101ms (×2.8)**، والنتيجة مطابقة بالحرف (نفس
البايتات وعدد المداخل والمصارف). وسبب الفرق بنيوي: كل مدخل كان في JVM يكلّف كائن `File` وسؤالَي نظام
(`isDirectory()`/`length()`) وصندوق `runCatching`؛ ومسح ١٢٠ ألف مدخل يعني ملايين الكائنات الوسيطة.

**ثلاثة عقود تُحفظ كما هي لأنها الميزة لا التجميل:** السقف يُعلن (`truncated` — فلا يُقدَّم الناتج كأنه
الجهاز كله) · المتخطّى يُعدّ (`skipped`) · وملف بحجم غير موجب يُعدّ ولا يُجمع فيبقى «المسح» أوسع من «المجموع».

**عيب تصميم أمسكته اختبارات الوحدة نفسها (يُسجَّل لأنه لم يُشاهد في أي أداة أخرى):** علم الإلغاء **عام**
مع مسحَين متوازيين = تشويش متبادل (مسح يبدأ فيلغي طلب غيره، أو يمسح علم إلغاء غيره). أُصلح بقفل مسح
واحد + إبطال الطلب الأقدم **بعد** القفل لا قبله، وبأن بدء مسح لا يمحو طلب إلغاء من خيط آخر. وكان أول
إصلاح أنقص: «امسح العلم عند البداية» **كان يبتلع الإلغاء** — أي أن الاختبار كشف الإصلاح الخاطئ أيضًا.

**الحراسة:** ١٠ اختبارات Rust في الوحدة + `ScanPacketTest` (٧) + `StorageScanNativeMappingTest` (٤)،
وبوابة `jni_symbols.py` تحرس الرموز الثلاثة في المصدر وفي الثنائيات (ADR-48).
**والحدود:** الأرقام على مضيف x86-64 بأقراص محلية؛ وزمن ART وI/O الهاتف **يحتاج جهازًا**.

## ADR-51 — **الموجة ٢ (`sampled`) تُغلق بالقياس**: المكسب سقفه ٠٫٢ مللي، والثمن يزيد عليه (2026-09-24)

**ما كانت تطلبه الخطة:** خادم عيّنات دائم يقرأ العتاد كل ٥٠٠ مللي ويكتب **لقطة**، والتطبيق يقرأ اللقطة —
«يُخرج الحلقة والحمل من عملية التطبيق؛ ودورة اللوحة تصير صفر صدفة/نداء».

**وقبل بناء خدمة، قِيست الدعوى على ثلاث طبقات** (مسطرة خارج المستودع، ومضيف x86-64):

| الحدّ | المقيس | معناه |
| --- | --- | --- |
| دفعة داخل العملية: ١٢ عقدة حقيقية | **٢٣٤ ميكرو** | هذا **سقف** كل ما يمكن أن يوفّره نقل القراءة خارج العملية |
| قراءة لقطة مكتوبة مسبقًا | **٩–١٢ ميكرو** | التطبيق يوفّر ≈٢٢٢ ميكرو لكل دورة (بدل ٢٣٤) |
| نشر ذرّي (tmp+fsync+rename) | **٥٢٤ ميكرو** (وبلا fsync ١٣٦) | وكل خصيصة غير مضبوطة تُدفع من طرف المُنتِج |
| ولادة عملية واحدة | **١٣٤٥ ميكرو** | أرضية أي مُنتِج يُنادى بدل القراءة |

**والنتيجة البنيوية، وهي الأهم:** اللقطة **لا تُنقص عمل النواة ولا سؤالًا واحدًا** — التطبيق لا بدّ أن يحصل
على القيم ليُعرضها؛ فما يوفّره النقل هو **زمن القراءة نفسه** الذي سقفه ٢٣٤ ميكرو لكل دورة (≈٠٫٠١٪ حِمل)،
في حين يدفع المُنتِج **٥٢٤ ميكرو × ٤ مرّات** لكل دورتين (لقطة كل ٥٠٠ مللي ودورة كل ٢ ثانية) — أي **خسارة
صافية بمعامل ٢٫٥–٩×**، قبل خدمة جديدة (وحدة/إقلاع/SELinux) تحتاج جهازًا وحكم سلامة.

**وقد كان المعيار الثاني للقبول («صفر صدفة في دورة اللوحة») هو الحمل الحقيقي — وقد تحقّق في الموجة ٤**
لا بخدمة: دورة اللوحة كانت **٣٠٧٠٢ ميكرو** (صدفة تفرّخ `cat` لكل عنقود!) وصارت **٨٢ ميكرو** = **×٣٧٤**.
فالحمل لم يكن في **موضع** القراءة بل في **نمط إفراخ عملية لكل عقدة** — وهذا يُنتزع بالتحويل لا بخدمة.

**القرار:** (أ) **لا يُبنى خادم عيّنات** — لا في التطبيق ولا خارجًا. (ب) ولا تُبنى «لقطة داخل العملية»
للتوفير بين مستهلكين: سقف توفيرها هو تكرار القراءات، وهو أقلّ من ٠٫٠٤٪ حِمل (مقيس بالحدّ الأعلى).
(ج) **والاستعمال الوحيد الذي يُبرّر مُنتِجًا خارج العملية هو عيّنات التاريخ أثناء نوم التطبيق** — وهي
ميزة أخرى لها مستهلكها وقرارها، وليست «دورة اللوحة». (د) وصفة تكذيب على جهاز (٣ دقائق) في `VALIDATION` §11.

**الدعوى القابلة للتكذيب:** لا يوجد جهاز يُظهر أن نقل دورة اللوحة خارج العملية يوفّر أكثر من **٠٫٣ مللي**
لكل دورة، ولا أن الخدمة تُلغي **أي** سؤال نواة (لأن التطبيق يقرأ ما يعرضه في الحالتين).

## ADR-52 — **الموجة ٤**: الخصائص تُقرأ أصلًا، و«صدفة لكل عقدة» تُمحى (2026-09-24)

**القرار (أ) — قراءة الخصائص إلى bionic:** وحدة `sysprop` (`__system_property_get`) وتُصدَّر عبر
`PropBridge` (`nativeGetProp` · `nativeGetPropsPacked`)، و`PropertyUtils.get` صارت **الأصلي أولًا**
والانعكاس احتياطًا. والمقيس: `getprop` عبر صدفة **٢٣٦٦ ميكرو** ← **١٢٫٦ ميكرو** (**×١٨٧**) — وخصيصتان
في مسار مفتاح Max AI، وأربع في فحص إصدار HyperOS تُقرأ الآن **في نداء واحد**.

**والأهم أن الأصلي أوثق لا أسرع فقط:** انعكاس `android.os.SystemProperties` سطح مخفي غير مضمون عبر
الإصدارات، وحين يُحجب يرجع `def` **صامتًا** — أي قيمة تبدو مقروءة وهي ليست كذلك. و`__system_property_get`
سطح ثابت منذ Android 1.0، و`PropertyUtils.setAndConfirm` (تحقّق الكتابة) صار يقرأ الحقيقة منه مباشرة.

**القرار (ب) — «صدفة لكل عقدة» تُمحى:** `HomeDashboardViewModel.readCores` كان يبني صدفة تُفرّخ `cat`
لكل عقدة (١٦ عملية فرعية لدورة من ٨ أنوية). والقياس: **٣٠٧٠٢ ميكرو ← ٨٢ ميكرو** (**×٣٧٤**) لكل دورة كل
ثانيتين. ونفس النمط أُزيل من `readSwap` (`/proc/meminfo`) ومن `ZramHardwareBackend.isActiveSwap`
(`/proc/swaps`) ومن حكام mali (`glob` ← `listNames` + قراءة دفعة).

**والقراءة الأصلية لا تشتري قدرةً:** عقدة لا يقرأها uid التطبيق تعود `null` فيسألها سلّم السقوط المعلن
(IPC الجذر ثم الصدفة) كما كان — ولا تُخفى عقدة لصالح السرعة.

**والكتابة تبقى كما هي عمدًا:** `setprop` وعقد sysfs تحتاج جذرًا وسياسة SELinux، وثمنها **الولادة**
(١٣٤٥ ميكرو مقيسة) لا الكتابة — و‏Rust لا يُلغي ولادة عملية (ADR-47)، فالنقل هناك بلا مكسب يُقاس.

**وحدود معلنة:** أرقام المضيف (x86-64) وزمن bionic على ARM **يحتاج جهازًا** · والثنائيات لعمودَي أندرويد
تُبنى في CI وحده · و**عطبَان أمسكهما المُصرّف** في هذه الجولة (`raw.uppercase` على عنصر `String?` بعد
بندل nullable، و`$it` مقروءة استيفاءً في نصّ اختبار) — وهذا هو نفس مبرّر تشغيله عند كل تغيير Kotlin.

## ADR-53 — **ترتيب بناء `jniLibs` عقدٌ لا مصادفة**: `ndk-build` يحذف `lib*.so` في مجلد خروجه (2026-09-25)

**العطب الذي أطلقه سجل CI (لا شكوى):** بوابة `ADR-48` أسقطت التشغيل بـ
`⚠️ مكتبة معلنة بلا أي ثنائية: 1  maxmanager_native — معلنة في Kotlin ولا ثنائية لها في الشجرة`
و`ثنائيات مقروءة 2`: أي أن المقروءتين هما `libtermux.so` في العمودين، **ولا وجود لـ`libmaxmanager_native.so`**.

**والسبب الجذري قاعدة في `ndk-build` نفسه، لا في البوابة ولا في مسار `cargo ndk`:**
`ndk-build` يحذف **كل** `lib*.so` في مجلد خروج المكتبات لكل ABI **قبل** أن يثبّت وحداته — قاعدة
`clean-installed-binaries` في `build/core/` من الـNDK (`rm -f <libs>/<abi>/lib*.so`؛ وُجدت لتفادي المكتبات
البائتة، وتحذف ما لا تعرفه). وترتيب الخطوتين في `build.yml` كان `cargo ndk` ثم `ndk-build`، فالأخيرة
**محت** ما بنته الأولى قبل ثوانٍ — وهو تحديدًا ما رآه المُشغّل: ثنائيتان لا أربع.

**والأثر الحقيقي أخطر من فشل بوابة — وهذا ما يجعل القرار عقدًا لا تجميلًا:** في الفترة نفسها كان الـAPK
**يُحزَّم بلا الطبقة الأصلية**، فيسقط `System.loadLibrary("maxmanager_native")` ويعمل التطبيق في الاحتياط
الدائم (`nativeAvailable=false`) بلا وكيل ولا متنبّئ ولا توأم ولا محرك سياق — **في صمت**، لأن حرس الـAPK
القديم كان يقيس **وجود العمود** (`lib/<abi>/`) لا وجود **المكتبة** بالاسم، ووجودُ `libtermux.so` كان يكفيه.
فالبوابة الجديدة لم تكن عقبةً بل أوّل ما صدح بالحقيقة.

**القرار — ثلاثة أجزاء، كل جزء في موضعه الذي يقيس فيه:**

1. **الترتيب:** `ndk-build` (الطرفية) أولًا، ثم `cargo ndk` (التطبيق) فلا يمسّها ماسح. ولا يهمّ أيّهما قبل
   الآخر لـGradle: هو يحزم `jniLibs` بعد الاثنين، والمقصود ألّا تمسح خطوة ما بنته أختها.
2. **توكيد قبل البوابة** («Assert jniLibs holds both libraries in both ABIs»): يقيس الأربعة بالمسار،
   ويسمّي الغائب فيها **قبل** البوابة، ويقول السبب المتوقّع (قاعدة الحذف) فلا يُعاد تشخيصه من الصفر.
3. **حرس الـAPK يقيس المكتبتين بالاسم** في كل عمود، لا وجود العمود وحده — فيقيس **الحزمة النهائية**
   نفسها بعد التوقيع، وهي الحقيقة التي وصلت المستخدم.

**وحدود معلنة:** لا NDK في هذه البيئة، فلا مصدر r29 مقروءًا هنا ولا `ndk-build` يُشغَّل: القاعدة مأخوذة من
مصادر الـNDK (`build/core/`، وهي في الشجرة الرسمية وقاعدة قديمة مستقرّة)، ودليلها القاطع الملازم هو مطابقة
ما رآه المُشغّل لما تُنتجه القاعدة حرفيًّا: **ثنائيتان لا أربع**، وهما بالضبط `libtermux.so` في العمودين — أي
الباقي بعد حذف `lib*.so`، لا ناتج بناء فاشل (ولو فشل `cargo ndk` لسقطت الخطوة قبله). وتنفيذ الترتيب الجديد
يُقاس في CI وحده. والمقيس هنا اثنان:
**(أ)** تمثيل حالة ما بعد الإصلاح (ثنائيتا المضيف في مجلدَي ABI) ⇒ `jni_symbols --assert --require-binaries`
**خروج 0** بـ«ثنائيات مقروءة 4» و«تحقّقت الثنائيات لعقود: maxmanager_native, termux»، وبإزالتها ⇒ **خروج 1**
(فالبوابة تفصل الحالتين فعلًا، ولم تُرخَّ). **(ب)** منطق حرس الـAPK بالتكذيب على أرشيفين مبنيّين: الكامل ⇒ 0،
والناقص ⇒ «missing lib/armeabi-v7a/libmaxmanager_native.so» و1.

**ولماذا لم يُحذف الرمز اليتيم:** `Java_com_termux_terminal_JNI_setPtyUTF8Mode` مُعرَّف في `termux.c:191`
ولا مُعلن له في `JNI.java` (أربعة مُعلنات: `createSubprocess` · `setPtyWindowSize` · `waitFor` · `close`)
ولا مستدعي له في الكود، ووظيفته (`IUTF8`) تُؤدّى **أصلًا** عند إنشاء الـpty (`termux.c:57`). فهو كود ميت
لا يضرّ ⇒ يُعلَن ولا يُسقط الإصدار (ADR-18)، ولا يُحذف من ملفّ موروث لتحسين منظر.

## ADR-54 — **الفضل يُعلن في `README`، والإشعارات تبقى ملزمة في `THIRD_PARTY_NOTICES.md`** (2026-09-28)

**الأمر:** «أريدك أن تضيف في اقرأني.md credit» + «قم فقط بإضافة credit للمشاريع الخارجية التي عندنا
**بعد التحقق حقًّا**» + «لا تذكر مبنيّ على AZenith — أضف الفضل فقط».

**والعقد الذي كان قائمًا:** `THIRD_PARTY_NOTICES.md` كان ينصّ: «ولا يُعضَّد بذكرٍ دعائي **ولا يُعرض في
الواجهة ولا في `README`**». فأمر المالك يخالف حرف المبدأ — **والتعارض يُعلَن ولا يُسكت عنه**.

**القرار:** يُعدَّل المبدأ: يُحفظ القدر الذي تفرضه الرخصة، ولا يُعرض في الواجهة، **ويُذكر أصحاب المكوّنات
القائمة في `README` باسم العمل وحقّ النشر ورخصته (قسم `Credits`)** — إعلانًا للفضل لا نقلًا للإشعار؛
وملفّ الإشعارات يبقى **وحده** المرجع الملزم ووحده موضع النصّ الكامل للرخصة.

**ولماذا لا يناقض ذلك القاعدة:** الجدول ثلاثة أعمدة (العمل · صاحب الحقوق · الرخصة)، بلا وصف تسويقي
وبلا نصّ رخصة منسوخ ولا شعار. ولا واحد من المذكورين يشترط العرض في `README` — **إلّا AZenith** فهو
يطلب الفضل صراحةً («you may use it but give credits!») ويوزّع `NOTICE.md`، ومادّة Apache-2.0 §4(d)
تُوجب حمل إشعارات `NOTICE` في العمل المشتق، فالجدول هو النسخة المقروءة المطلوبة.

**وحدّ القياس (لا الذاكرة):** كل اسم في الجدول يقابله ملفّ في الشجرة — AZenith (٢٤٧ ملفًا بترويسة
Zexshia) · Encore Tweaks (٢٠ ملفًا) · Rianixia-ThermalCore (١٦ ملف Rust) · VMTouch
(`preloadbin/jni/main.c` المُضمَّن). **وأُزيل `AnyKernel3` من ملفّ الإشعارات ومن `license_audit.py`**
لأنه **لا ملفّ له في الشجرة** — ادّعاء بلا ملفّ يُحذف ولا يُبرَّر (والأسلوب الذي في `mainfiles/customize.sh`
أسلوب تثبيت لا كود منقول). **ولا بوابة جديدة** (قرار المالك: «أنا أفضل عدم ذلك») — فالرقابة على
القائمة مراجعة، وهي مشروطة بـ«بعد التحقق حقًّا» لا بأداة.

## ADR-55 — **الترخيص المتسامح يُنقل باعتماد، والمفروض لا يُنقل إطلاقًا** (2026-09-30)

**الأمر:** «لا تكون تقليد ونسخ من الآخرين بل مرجع، ولا تنسخ حتى الشكل لأن تطبيقي مغلق المصدر … لا
أمانع المشاريع صاحبة الترخيص التي تسمح بجعل المفتوح مغلق المصدر، ويمكنك حتى إضافتهم في credit … وإذا
كان الأمر صعبًا جدًا صنعه من الصفر للتعقيد … أنه أفضل نسخه بعض الإمكانات ووضعه في credit.»

**القرار:** ثلاث فئات لا رابعة:

1. **متسامح** (`MIT` · `Apache-2.0` · `BSD-3-Clause`) ⇒ **يجوز النقل ولو جزئيًّا**، مع الإشعار في
   `THIRD_PARTY_NOTICES.md` والفضل في `README` ← `Credits`.
2. **مفروض** (`GPL` · `AGPL` · أي ترخيص يوجب فتح مصدرنا) ⇒ **صفر كود، ولو سطرًا**؛ والمشروع مغلق ولا
   يُفتح أبدًا، والبوّابة `tools/license_audit.py --assert` تمنع الانزلاق.
3. **غير مُثبت** ⇒ **لا يُنقل منه شيء** حتى يُقرأ نصّ ترخيصه.

**السبب:** الترخيص المفروض يجعل «استخدام شيء» مساويًا «فتح المشروع»، وهو ما يرفضه المالك رفضًا مطلقًا —
فالحدّ قانونيّ لا ذوقيّ. وفي المقابل، رفضُ النقل من المتسامح مطلقًا يُكلّف إعادة اختراع بلا مقابل في
مواضع يستحيل فيها أو تكلف أكثر من اللازم.

**النتيجة:** (١) **الشكل/التصميم لا يُقلَّد** — الاستعارة معلومة ومنهج، والرسم من لغة التصميم عندنا
(`ui/design/`)، لا من شكل تطبيق مرجعيّ؛ (٢) **الفضل يُعلن حتى حين تُؤخذ الفكرة بلا كود** إذا كان
المشروع فريدًا (تعضيد لـADR-54)؛ (٣) كل خطة مراجع تُصنَّف مراجعها بهذه الفئات في **قسم صريح**؛
(٤) `license_audit.py --assert` تبقى البوّابة المقيسة، وكل ادّعاء اعتماد يقابله ملفّ أو مصدر مقيس.

**وسندها المقيس:** `license_audit.py --assert` ⇒ **exit 0** («لا مكوّن GPL في مسار الإصدار»)، والفضل
في `README` ← `## Credits`، والإشعارات في `THIRD_PARTY_NOTICES.md` (المكوّنات المشمولة كلها متسامحة:
Apache-2.0 · BSD-3-Clause · MIT).

## ADR-56 — **`AR-R1` يُعدَّل بأمر المالك: عزل الجذر لكل تطبيق مأذون به** (2026-09-30)

**الأمر:** «ابحث عن مشاريع مثل hma oos **يخفي الروت عن التطبيق نفسه الذي يختاره** … يدخل تطبيقي ويحدد
علامة على تطبيق البنك فيخفي ويعزله حتي يعمل التطبيقي البنكي ولا يكتشف شيئًا».

**والتعارض مُعلَن لا مسكوت عنه:** `AR-R1` («دمج إخفاء الجذر (SUSFS/Zygisk/DenyList)») كان **مرفوضًا**
بعلّتين: «خارج الهوية» و«سباق كشف لا ينتهي». وأمر المالك **يُعدّله**، و**العلّتان تبقَيان مكتوبتين** في
هذا القرار ولم تُمحَيا — لأن الكلفة عُرضت عليه ثلاث مرّات وقَبِلها صراحةً، والقرار قراره.

**القرار الجديد:** يُبنى «عزل الجذر لكل تطبيق» **بوصفنا مديرًا لِما ثبّته المستخدم**، لا مُنفّذًا للإخفاء:

1. **صفر كود** من `Magisk` · `Shamiko` · `ZygiskNext` · `ReZygisk` · `NoHello` · `SUSFS` (§0.4)؛ ونستدعي
   **واجهة** مدير الجذر — وهو **استعمال أداة لا اقتباس كود**.
2. **لا تثبيت وحدات** — `AR-R2` **يبقى كما هو**؛ نكشف الناقص ونُرشد، والتثبيت بيد المستخدم.
3. **كاتب واحد + journal + تحقّق بالقراءة** (انضباط `HardwareControlArbiter`) بمفتاح لكل تطبيق.
4. **لا وعد بأن بنكًا سيعمل** (ADR-07): الحكم يُقاس على جهاز المستخدم وتطبيقه.
5. **`DenyList` وحدها ليست إخفاءً** بنصّ مشرف Magisk نفسه (`#7418`)، وتعارُض `Enforce DenyList` مع
   `Shamiko`/`Zygisk-Assistant` **يُعلن في الواجهة** قبل أيّ علامة.

**النتيجة:** `AR-R1` **معدَّل لا محذوف**؛ والخطّة `docs/ai/ROOT-ISOLATION-PLAN.md`؛ ونطاق ملفّي
`HMA-INTEGRATION-PLAN.md` (§19) و`APP-LIST-HIDING-PLAN.md` (القرار ٢) **يُصحَّح بالإحالة إليه** فلا يُقرأ
أحدهما مناقضًا للآخر.

## ADR-57 — **المحرّك الصوتيّ: الشامل أساسًا وDolby اكتشافًا — والمستحيل بالـAPI العامّ لا يُوعد** (2026-10-01)

**الأمر:** «أنشئ `Audio Engine` لا يعتمد على Dolby أصلًا، ويستخدم **أفضل `Audio Backend` حقيقيّ متاح على**
الجهاز… ويتمّ اختيار الـBackend بناءً على **capability discovery + verification** وليس اسم الشركة المصنّعة»،
و«اجعل MaxManager يبحث **تلقائيًّا** عن Dolby… وإذا تمّ اكتشافه لكنّه غير قابل للوصول فيجب تسجيل
**`DETECTED_BUT_UNAVAILABLE`** بدل إظهار أنّه يعمل». وعرض المالك ثلاثة خيارات — (أ) Dolby كأساس · (ب) محرّك
شامل مستقلّ · (ج) هجين — وطلب تنفيذًا حقيقيًّا يشمل discovery · abstraction · adapter · runtime selection ·
verification · rollback · graceful fallback · حالة واجهة صادقة · اختبارات.

**القياس الذي حسم القرار (لا تفضيل):** على **نفس** `android.jar` الذي يُصرَّف عليه المشروع (`compileSdk 37`)
بـ`javap`، ومقابَلًا بمصدر AOSP (`frameworks/base`، فرع `main`):

| الواجهة | في سطح SDK العامّ؟ | الموضع |
| --- | --- | --- |
| `AudioEffect.queryEffects()` | **نعم** (`public static Descriptor[]`) | `javap` |
| `AudioEffect.Descriptor.uuid` | **نعم** (`public UUID uuid`) — معرّف **التنفيذ** | `javap` |
| `AudioEffect.EFFECT_TYPE_NULL` | **لا** (`@hide` · `ec7178ec-e5e1-4432-a3f4-4657e6795210`) | AOSP سطر ١٦٣ |
| `AudioEffect(UUID,UUID,int,int)` | **لا** (`@hide`) | AOSP سطر ٤٧٢ |
| `setParameter(byte[],byte[])` / `getParameter(byte[],byte[])` | **لا** (`@hide`) | AOSP ٧٣٨ / ٨٤٨ |

⇒ **الـSDK العامّ «يرى» مؤثّر المصنّع ولا «يلمسه».** فـ(أ) كأساس تعني وعدًا لا يُوفى على أكثر الأجهزة (الشاشة
كلها تصير «Dolby موجود ولا يعمل»)، و(ب) وحدها تُسقط ما هو حقيقيّ على أجهزة تُعلنه.

**القرار:** **(ج) هجين** — المحرّك الشامل (`AudioBackendLadder`: `VENDOR_EFFECT` → `PLATFORM_DYNAMICS` →
`PLATFORM_EQUALIZER` → `PLATFORM_SIMPLE` → `SYSTEM_LAYER`) هو **الطبقة الأساس**، وDolby **اكتشافٌ اختياريّ**
يُقاس في اللحظة ويُعرض بحالته، **ولا يقود إلّا إن ثبت أنّه قابل للقيادة**. والترتيب **بالقدرة لا بالمصنّع**.

1. **الحالة قياسٌ لا أمنيّة.** `AVAILABLE` = «قِيست وأمكن قيادتها»؛ `NEEDS_ADAPTER` = «حقيقيّة وتحتاج مسارًا»؛
   `DETECTED_BUT_UNAVAILABLE` = «موجودة ولا مسار» (والمطلوب صراحةً)؛ و**ثلاثيّات لا ثنائيّات** (ADR-07): قائمة
   مؤثّرات غير مقروءة ⇒ `UNKNOWN` ولا تُقرأ «غير موجود».
2. **الاختيار لا يرقّي الحالة** — الحالة مقياسٌ ثابت، و`primary` قرارٌ، فلا يُخلطان في حقل واحد، والبدائل تُقال.
3. **صفر كود من التطبيقات المرجعيّة (§0.4/ADR-55):** `RootlessJamesDSP`/`wecho` **GPL-3.0** ⇒ لا سطر منها؛
   وما نُقل هو **معرّف مؤثّر Dolby DAP** وحده من `DolbyUI` (**Apache-2.0** بنصّ ترويساتها) بترويسة نسبةٍ
   إنجليزية **أولى** في أوّل الملفّ (يشترطها `license_audit.py`، وإلّا صُنّف `DERIVED_UNDER_PROPRIETARY_HEADER`).
4. **لا رِفادة `SoftwareDSPBackend`** — وهذا **منعُ ادّعاء لا رفضُ فكرة**: هذا تطبيق تحكّم لا مشغّل وسائط، ومن
   يعالج الصوت داخل عمليّته يحتاج أن **يملك مسار الصوت** (التقاط `AudioPlaybackCapture`/`MediaProjection`) —
   مرفوضٌ صراحةً في `AUDIO-ADVANCED-PLAN` §4. فالمسار الشرعيّ الوحيد لمحرّك DSP حقيقيّ = **طبقة نظاميّة**
   (`AQ-09`/`AS-06`) = رِفادة `SYSTEM_LAYER` في السلّم، مسمّاةً لا موعودة.
5. **كل كتابة عبر المحكِّم** (`HardwareControlArbiter`) بمفتاح `audio_vendor:<effect>` (ADR-11)، وقراءةٌ بعد كل
   كتابة، و**لا مقبض قبل جلسة نملكها** (زرٌّ يعرف أنّه سيفشل أسوأ من زرٍّ غائب)، وتحريرٌ في اللحظة نفسها.
6. **تفريق `NO_BACKEND_AVAILABLE` عن `NO_BACKEND_MEASURED`** — «لا محرّك» يُقال فقط بعد قياسٍ فعليّ.
7. **الاختيار وقت التشغيل يُربط بالمقابض ولا يُعزل عنها:** `AudioBackendRole` (`PRIMARY`/`FALLBACK`/`OTHER`)
   يُحسب من الاختيار نفسه، ويُقال في تبويب المحرّك وفي قسم مؤثّر المصنّع — فما يُقاس هو ما يُعرض موضعه.
8. **الاكتشاف من مصدرين لا من واحد:** `AudioEffect.queryEffects()` تقول ما **حمّلته** المنصّة، ووثيقة
   `audio_effects.xml` تقول **أين** وبأيّ مكتبة. و**مُعرَّفٌ في التهيئة ولم يُحمَّل ⇒ `DETECTED_BUT_UNAVAILABLE`**
   (لا «غير موجود» ولا «يعمل») — **والجردة تُقدَّم على التهيئة**، فلا يُرقّى تعريفٌ إلى تحكّم بلا قياسٍ حقيقيّ.

**ولا تعارض مع تكملة ٢٣٠:** قولها «لا كشفَ DAP صُنع» معناه **لا حزمة Dolby نُقلت ولا كشفٌ بامتياز نظام**؛ وهذه
الموجة تضيف **اكتشافًا بالـSDK العامّ وحده** + بروتوكول DAP **نموذجًا صافيًا** — فالتصريح يُحدَّد لا يُنقض.

**وسندها المقيس:** `OK (163 tests)` (٣٦ ملفًّا · +٤٩ اختبارًا) · **٤٩٧ ملفًّا · ٢٩٩٠ صنفًا · `total errors: 0`**
(ترجمة أندرويد محلّية) · **`ran=32 FAIL=0`** لأوامر «Contract gates» محلّيًّا · **٨٤ مفتاحًا** في `values/`
و`values-ar/` وحدهما (§0.2) متطابقةً و`--prune` = صفر يتيم · و`license_audit --assert` ⇒ exit 0.
**وحدّه المعلن:** هل تُقبل جلسة ٠ · وهل تسمح سياسة الـAPI المخفيّة بالانعكاس · وهل يمنع محرّكٌ آخر ملكيّة المؤثّر ·
وهل يقبل `audioserver` الطبقة النظاميّة بعد الإقلاع — **يحتاج جهازًا**، ولا يُدَّعى خلاف ذلك.

## ADR-58 — **MaxFx: محرّكٌ صوتيٌّ مملوك كمؤثّرٍ نظاميّ، يُقاد بخاصيّاتٍ لا بـAPI مخفيّ** (2026-10-01)

**السبب:** أمر المالك: «إذا كان الأمر شبه مستحيل… فقط تنقّب في ١٠٠ مشروع مفتوح المصدر لنحقق فلسفة
التطبيق عن خيارات متقدّمة للصوت وتحسينات عميقة… مميزات صوت رائعة و**تعمل بشكل عام مهما كان** مثل
ViPER4Android-RE-Fork». والقياس على جهاز المالك يقول إنّ الطريق القائم (الـAPI المخفيّ + مؤثّرات المصنّع)
**لا يعمَّم**: `hidden-api-blocked-or-absent` على ROMه، وDolby = `DETECTED_BUT_UNAVAILABLE`. والطريق الذي
«يعمل بشكل عام» هو مؤثّرٌ **نظاميٌّ مملوك لنا** يحمّله `audioserver` على أيّ جهاز — هذا هو ما يفعله ViPER.

**القرار:** محرّك **MaxFx** في `maxfx/` — مؤثّرٌ نظاميٌّ (`libmaxfx.so`) يُثبَّت بطبقة `AQ-09`، ونواة DSP
نقّية مملوكة، وقِياسُه على المضيف قبل أيّ جهاز. وتسع قواعد:

1. **صفر كود من ViPER4Android-RE (`iscle/ViPER4Android-RE` = GPL-3.0)** (ADR-55) — الفكرة تُنسب والكود
   يُكتب ملكيًّا سطرًا سطرًا؛ ولا يُنقل من GPL/AGPL حرفٌ واحد. والمنهجيّات (فواصل النطاقات · ضاغط ·
   محدّد · tube) مفاهيمُ عامّة لا كود.
2. **الموقع: مؤثّرٌ نظاميّ يحمّله `audioserver`** — لا تطبيقٌ يعالج داخل عمليّته (وذلك يحتاج ملكيّة مسار
   الصوت = مرفوضٌ سلفًا). والوحدة (`AudioSystemModuleModel`) تضع المكتبة في `system/lib64/soundfx/`
   والتهرية في المسار الذي يقرأه الجهاز بالاسمين (`audio_effects.xml`/`audio_effects_config.xml`).
3. **قناة التحكّم: خاصيّات النظام** (`persist.audio.maxfx.<key>`) — لأنّ `AudioEffect.setParameter`
   والبانيّ **`@hide` ومحجوبٌ مقيسًا**، والخاصية يقرأها المؤثّر بلا صلاحيّة وبلا سياقٍ خاصّ **على أيّ ROM**.
   والقراءة دوريّة (~250ms من `process`) — تأخيرٌ مُعلن لضبطٍ لا يطلب سرعة؛ و`EFFECT_CMD_SET_PARAM`
   مطبَّقٌ أيضًا لمن يملك المقبض. والغياب **ليس صفرًا** (ADR-07) على الطرفين.
4. **العقد مصدرٌ واحد لا يُعاد:** `fixtures/contracts/maxfx_{params,identity}.tsv` يُقرأ من **طرفَي القياس**
   (`maxfx/tests/dsp_test.c` و`MaxFxModelTest.kt`) ويُقارَن بالنموذجين — فلا رقمَ مكتوبٌ من الذاكرة،
   و`id` المعاملات ثابتة لا تُعاد استخدامها بعد الحذف (المُستهلك على الجهاز قد يكون أقدم من الجدول).
5. **النواة نقّية ومضبوطة على المضيف:** `maxfx_dsp` بلا أندرويد وبلا عقد مؤثّرات، وتُقاس بأدّعاءاتٍ رقميّة
   قابلة للتكذيب **وطفراتٍ يجب أن تُسقطها**؛ وحالةُ كلّ قناةٍ مستقلّة (عطبٌ أمسكه القياس: حالةٌ مشتركة
   بين القنوات أمالت استجابة المعادل +1.95dB بدل +6dB)، والدخول المُشوَّه (NaN/Inf/ضخامة) يُخرج محدودًا
   سليمًا، و`process()` بلا `malloc`/`lock`/`I/O` (زمنٌ حقيقيّ).
6. **رؤوس ABI من AOSP بنسبة الفضل** (Apache-2.0 — ADR-55): `maxfx_effect_abi.h` مُجزَّأ حرفيًّا من رؤوس
   `audio_effect` الثلاثة لأنّه **عقدٌ ثنائيّ** مع `audioserver` — قيمةٌ مُغيَّرة = مكتبةٌ لا تُحمَّل أو ذاكرةٌ
   تُفسد؛ والإشعار في الترويسة + صفّ AOSP في `THIRD_PARTY_NOTICES.md`.
7. **الهويّة مقيسة لا مُنتقاة:** uuid النوع/التنفيذ والاسم والمكتبة وبادئة الخاصيّات كلّها في
   `maxfx_identity.tsv` — وتعديلُ هويّةٍ بعد تثبيت الوحدة يعني مؤثّرًا قديمًا لا يُربط بمصدره، والعقد يمنع
   ذلك بصمت (اختبارٌ يقرأه ويقارن، والقارئ يُسقط الصفوف الفائتة بلا سكوت).
8. **التركيب بالأداة الوحيدة:** `MaxFxModel.installAddition()` يولّد سطر الإضافة الذي يقبله محلّل الشاشة
   ويكتبه مولّد الوحدة — والتهريةُ في التهيئة هي **اسم الملفّ مجرّدًا** (صيغة AOSP) فيبحث عنه الحمّال في
   مجلّدات `soundfx`، والمعرّفُ يُشتقّ من اسم الملفّ (`libmaxfx.so` ⇐ `maxfx`).
9. **الحدّ المُعلن:** ما مُثبَت هنا أنّ العقد سليمٌ والمعالجة قياسها صحيح **على المضيف** — وأمّا «الصوت
   يتغيّر فعلًا على الهاتف» ف**يحتاج جهازًا**، ولا يُدَّعى قبل قياسٍ فيه. وبناء `libmaxfx.so` يحتاج NDK
   غير متوفّر محليًّا — فالتراجمة «غير مُتحقَّقة في هذه البيئة» لا «مُتحقَّقة».

**وسندها المقيس:** `maxfx/tests` = **246 ناجحة · 0 فاشلة** + **ثلاث طفراتٍ سقطت المجموعة كلّها**
(`self-check`) · `MaxFxModelTest` = 5 اختبارات · **`OK (171 tests)`** (١٦ ملفًّا + الجديد) · **499 ملفًا ·
2995 صنفًا · `total errors: 0`** · **`ran=32 FAIL=0`** · `license_audit --assert` exit 0 بعد صفّ AOSP المُحدَّث.

## ADR-59 — **`audio_maxfx:`: مقبضُ الخاصيّة مفتاحًا مُفرَّقًا، وكل كتابةٍ تُقرأ بعدها** (2026-10-02)

**السبب:** ربطُ MaxFx بالواجهة (تكملة ٢٣٥) أوجَب حسمَ ما كان مفتوحًا في تكملة ٢٣٤: أيّ مفتاحٍ لمحكِّم
يحمل معاملات MaxFx؟ بين `audio_vendor:<uuid>:<param>` (مفتاح مؤثّرٍ يُفتح بجلسة) ومفتاحٍ جديد. وثوابت
المقابض في `HardwareControlKey` تقول: **الاختلاف في الوصول هو ما يجب أن يُفرّق بين المقابض في السجلّ**
— فشلُ كتابةِ خاصيةٍ ليس فشلَ جلسةٍ، والعكس، وعلاجهما مختلف — وهي القاعدة نفسها التي فصّلت قديمًا
`audio_vendor:` عن `audio_effect:`.

**القرار:** مفتاحٌ رابع `audio_maxfx:<param>` + تحكّمٌ بالخاصيّات عبر المحكِّم، وسبع قواعد:

1. **المفتاح يُبنى في `HardwareControlKey` وحده** — والوسيط هو **مفتاح العقد نفسه** (`maxfx_params.tsv`)
   فلا تسميةٌ ثانية لاسمٍ واحد؛ وبوّابة `canonicalKeysAreNotReinvented` تحرس الموضع الواحد.
2. **كل كتابة تمرّ بالمحكِّم** (`Owner.GLOBAL_PROFILE`) بـ`apply = PropertyUtils.setAndConfirm`، وكلّ
   `apply` يقابله `read` من المنصّة نفسها و`restore` بالقيمة القديمة (ADR-11) — ولا كتابةٌ من `ui`.
3. **خطّ الأساس للخاصية غير المكتوبة فراغٌ مكتوب لا `null`:** المحكِّم لا يبني خطّ أساس من غير قراءة
   (`baseline-unreadable`)، والفراغ يُسترجع فراغًا فيعود افتراض العقد (`maxfx_config_default`).
   **والغياب ليس صفرًا** (ADR-07): ما يُعرض لغير المكتوب هو افتراض العقد موسومًا «افتراضي».
4. **الصياغة من العقد وحده** (`MaxFxModel.propValue`: اقتصاصٌ عند الحدود + تقريبُ عرضٍ للمنزلق) — فما
   يُعرض حرفيًّا هو ما يُكتب في الخاصية بالضبط، ولا صياغةٌ ثانية في الشاشة.
5. **القسم يُبنى من `MaxFxModel.params`** فلا جدول معاملاتٍ ثانٍ في الواجهة؛ ومعاملٌ بلا تسميةٍ معلومة
   **يُعرض باسمه في العقد** لا يُلحق بأقرب شبيه فيُقرأ باسمٍ ليس له.
6. **التثبيت فعلٌ واحد فنصّه واحد:** زرٌّ يولّد سطر الإضافة من `MaxFxModel.installAddition()` وينادي
   الطبقة النظاميّة (`AQ-09`) — **بنصّ التحذير ونافذة التأكيد نفسهما**، لا نسخةٌ ثانية تفترق مع الوقت.
7. **`libmaxfx.so` تُبنى في CI للعمودين ويُقاس رمز `AELI` على الثنائية** — ومكتبةٌ بلا رمزٍ مُصدَّر
   تُبنى «ناجحةً» ثم لا تُحمَّل أبدًا؛ و`maxfx/**` دخل مسارات التفعيل فتعديلٌ فيها يُشغّل القياس.

**وسندها المقيس:** ترجمة **502 ملفًا · 3005 صنفًا · `total errors: 0`** · `OK (171 tests)` ·
`maxfx/tests` **246 ناجحة · 0 فاشلة** + ثلاث طفراتٍ سقطت · **`ran=34 FAIL=0`** · `aapt2` على النصوص
الجديدة · `source_manifest --write` بعد الإضافة. **وأمّا «الصوت يتغيّر فعلًا» فكما كان: يحتاج جهازًا**
— والخاصية تصل المؤثّر خلال ~250ms (مُعلن من تكملة ٢٣٤)، وقبولُ `audioserver` للمؤثّر وسماعُ الفرق
يبقيان خارج ما يُقاس هنا.

---

## ADR-60 — **عقد الطبقة النظاميّة صار AIDL، و`type` على `<effect>` شرطُ تحميلٍ لا زينة** (2026-10-02)

**السبب — أمر المالك ثمّ قياسٌ نقض الافتراض:** أمر المالك: «اخبرتك أن تستسلم وتصنع شيئًا مشابهًا
لـ`AndroidAudioMods/ViPER4Android` وغيرها من المشاريع المشابهة» + مراجع AIDL الرسميّة. وكان الافتراض
السائد أنّ V4A «حلٌّ مثبتٌ يعمل على الأجهزة الحديثة» فيكفي تقليده. **والقياس نقض الافتراض من طرفيه:**

1. **الرخصة:** `AndroidAudioMods/ViPER4Android` **ليس فيه كود** («*The ViPER4Android apk source code is
   currently not open source*»)، والمصدر المفتوح `ViPERFX_RE` `module/LICENSE` = **GPL-3.0** (18092
   بايت) و`src/` بلا رخصة ⇒ **صفر كود** (ADR-55)، معماريّةٌ فقط.
2. **العقد المشحون فعلًا:** نُزّلت الوحدة الرسميّة `V4A_Magisk_Module_0.6.1.zip` وقِيست ثنائيّتها:
   `readelf --dyn-syms libv4a_re_arm64-v8a.so` ⇒ **`AELI` كائنٌ مُصدَّر (48 بايت)** و**صفر** من
   `createEffect`/`queryEffect`/`destroyEffect`؛ و`NEEDED` = `liblog`/`libm`/`libdl`/`libc` فقط.
   **أي أنّ `maxfx` عندنا ليس شاذًّا: هو العقد نفسه الذي يُشحنه V4A.**
3. **ومصنع AIDL لا يقرأه:** `EffectFactory.cpp` (AOSP `audio/aidl/default`، ٣٠٠ سطر، مقروء) يبني واجهته
   بـ`dlsym(h,"createEffect")`/`"queryEffect"`/`"destroyEffect"` حصرًا، وبغيابها يكتب
   `… not exist in library` — و**لا مسار `legacy`/`AELI` في المصنع ولا في `EffectConfig`/`EffectImpl`/
   `EffectThread`** (فحص الأربعة: صفر مطابقة لـ`aeli`/`AUDIO_EFFECT_LIBRARY`). ودليله الاجتماعيّ: تغيير
   LineageOS **مُدمَج 2026-09-05** عنوانه «*Add an AIDL wrapper for legacy effect libraries*» — أي أنّ
   دعم `AELI` **يُضاف** لأنّه غير موجود. **⇒ وحدة V4A المشحونة لا تُحمَّل على جهاز المالك أيضًا.**
4. **وجهاز المالك AIDL خالص (من لوقه):** كلّ مكتبةٍ تُفتح فعلًا `*aidl.so`؛ والمكتبات القديمة
   (`libbassboostsw.so` · `libequalizersw.so` · `libvolumesw.so` · …) تظهر في `parseLibrary`
   **ولا تُفتح بـ`openEffectLibrary` إطلاقًا**، و**`not exist in library` = 0 مرّة**.
5. **والسبب الأخير مقروء في المصدر:** `EffectConfig::parseLibrary` (سطرا ١٩٨ و٣٤١) يقرأ سمة **`type`**
   **من عنصر `<effect>`** ويخزّنها في `library.type`، و`findUuid` **يُعيد `false` بغيابها** ⇒
   `Factory::loadEffectLibs` يطبع `skipping` ولا يُنشئ هويّةً ولا يفتح المكتبة. **وهو تفسير ما في البند
   ٤ بالضبط** — مفعولات `*sw` في التهيئة بلا `type`، فلم تُحمَّل ولم تُفتح.

**القرار — أربعة بنود:**

1. **عقد الطبقة النظاميّة = عقد AIDL** (`createEffect`/`queryEffect`/`destroyEffect`، توقيعها المقيس في
   `EffectTypes.h`: `binder_exception_t (const AudioUuid*, std::shared_ptr<IEffect>*)` و…).
   ويبقى `AELI` **مبنيًّا كصيغةٍ بديلة** للأجهزة ذات الـHAL القديم (HIDL) — لا يُحذف ما يعمل على غيره.
   **والبناء يحتاج ترويسات AIDL من AOSP** (‏`aidl --lang=ndk` على `aidl_api/android.hardware.audio.effect`
   + `android.media.audio.common` + `android.hardware.common.fmq`) — وهي **Apache-2.0** فتُستعمل بنسبة
   الفضل لا تُنقل من GPL (ADR-55). وقياس البناء في فرع V4A غير المشحون
   (`Android.bp` · `BnEffect` · `EffectThread` · `android.hardware.audio.effect-V{1,2,3}-ndk`) — **يُقرأ
   كخريطةٍ لا يُنقل ككودّ.**
2. **`type` في `<effect>` شرطُ تحميل:** يُكتب بقيمة **`type_uuid` من عقد الهويّة نفسه** — وهي **مطابقةٌ
   قائمةٌ أصلًا**: `maxfx_descriptor()` تضبط `out->type = MAXFX_TYPE_UUID`، فالتهيئة والوصف يُعلنان
   النوع نفسه، واختلافهما كان سيجعل المؤثّر غير قابلٍ للربط. **وسطر الإضافة في الواجهة يحمله الآن
   حقلًا سادسًا** (`…|uuid|الأجهزة|النوع`) — في الذيل لا في الوسط، فسطرٌ قديم بخمسة حقول لا يتغيّر معناه.
3. **وما لا يُصلَح صامتًا يُرفض معلنًا:** إضافةٌ إلى مؤثّرٍ **قائم بلا `type`** تُرفض بـ`addition-effect-conflict`
   — لأنّ الطبقة **تُلحق ولا تُعدّل** عقدةً كتبها غيرنا (ADR-18)، فلا سبيل إلى إسناد سمةٍ لعنصرٍ قائم.
4. **و`libmaxfx.so` تُشحن** (الفجوة المقيسة: مولّد الوحدة يُخرج أربعة ملفّات نصّيّة ولا ينسخ المكتبة
   إطلاقًا — فلا وجود لمسار `cp` كما في وحدة V4A) — وتُوضع حيث يقرؤها الـHAL، ويبقى `path` في التهيئة
   **اسمَ الملفّ مجرّدًا** فيبحث عنه الحمّال في مجلّدات `soundfx`.

**وسنده المقيس (كلّه في هذه الجلسة):** `readelf` على ثنائيّة V4A · `EffectFactory.cpp` و`EffectConfig.cpp`
و`EffectTypes.h` و`EffectImpl.cpp` و`EffectThread.cpp` من googlesource (`?format=TEXT`) · `parseLibrary`
و`openEffectLibrary` و`not exist in library` من **لوق جهاز المالك** · وتغيير LineageOS 501218 عبر
`/changes/501218/detail` (`status: MERGED`) · ثمّ تعديل الكود: **`OK (177 tests)`** (كانت ١٧١ ⇒ +٦)
بعد ترجمة **38 ملفًا · 125 صنفًا · 0 خطأ** في `kverify-audio` · وستّ بوّابات: `kt_balance` · `code_health` ·
`i18n_coverage` · `--prune all` · `jni_symbols` · `resource_compile` — **كلّها PASS**.

**وحدوده المعلنة:** غلاف AIDL **غير مكتوب بعد** (لا تُبنى ترويسات AIDL في هذه البيئة) · و`libmaxfx.so` لم
تُنقل بعد إلى الوحدة · و**قبول `audioserver` للمؤثّر وسماعُ فرقٍ في الصوت يحتاجان جهازًا** (§0.1) —
فلا يُدَّعى صوتٌ لم يُسمَع.

## ADR-61 — إرفاق `DynamicsProcessing` **سلّمٌ يقيس نفسه**، لا محاولةٌ واحدة بهندسةٍ نفترضها

**الحالة:** ملزم · **السياق:** تكملة ٢٣٩ (تشخيص `effect-attach-refused` في سجلّ المالك).

**المشكلة المقيسة:** كان `AudioEffectBackend.createEffect` يُنشئ محرّك الديناميّ **مرّة واحدة** بهندسةٍ
**اخترعناها**: `Config.Builder(VARIANT_FAVOR_FREQUENCY_RESOLUTION, channels, true, 6, true, 4, true, 6, true)`
— أي أنّنا كنّا **نفرض هندسةً على عتادٍ لم نسأله عن هندسته**، وأيُّ فشلٍ في أيّ خطوةٍ يُطوى في رمزٍ واحد
`effect-attach-refused`. ونتيجةً لذلك: سجلّ المالك يقول `effect-attach-refused` **بلا تفصيل**، ولا محاولةَ
ثانية، فيبقى المحرّك — وهو موجودٌ في المنصّة أصلًا — **مُغلقًا على جهازه**.

**ومقيسٌ في مصدر AOSP:** `DynamicsProcessing(int session)` = `(0, session, null)` — أي **بلا `Config`**،
فتُبنى المحرّكة على **هندسة المنصّة نفسها**، ولا عددَ نطلب. وهذا هو الطريق الذي تسلكه المشاريع العاملة
(`allEQ` نمطًا)، ولم يكن في كودنا إطلاقًا.

**القرار:**

1. **السلّم مُعرَّفٌ ومرتَّبٌ في نواةٍ نقيّة** (`DYNAMICS_ATTACH_LADDER`): (١) **هندسة المنصّة** بلا `Config`،
   (٢) **دقّة التردّد** بأعدادٍ مقيسة إن وُجد قياس وإلا فطلبنا المُعلَن، (٣) **زمن الاستقرار**. والترتيب
   **ثابتٌ من التعريف لا من ترتيب وصول قياس**، فالشاشة والسجلّ يتّفقان.
2. **المقيس أولى من المطلوب** (`dynamicsBandCounts`): ما أعلنته المنصّة بعد إرفاقٍ ناجح يُبنى به في أيّ
   خطوةٍ تالية؛ وقياسٌ صفريّ **لا يُصدَّق** (يُستعمل المطلوب)، وعدد النطاقات لا يقلّ عن واحد.
3. **كل خطوة تُسمّى برمزها للتشخيص** (`dynamicsAttachRefusal`) وتُسجَّل في `audioOp` بـ`gated=false`،
   **والحكم المعروض واحدٌ صريح**: `effect-attach-refused-after-all-steps` — فلا يُظنّ أنّ محاولةً تُركت،
   ولا يُخلط تشخيصٌ بحكم (وهو نفس درس `AudioCapabilitySection`: النفي يُعلن عن شجرتنا لا عن الجهاز).
4. **لا قراءةَ من `Config` لما لا تُعلنه:** مقيسٌ بـ`javap` أنّ `DynamicsProcessing.Config` تُعلن أربعة
   قارئات فقط (`getVariant` · `getPreEqBandCount` · `getMbcBandCount` · `getPostEqBandCount`) **ولا
   `getChannelCount`**؛ فعدد القنوات يُمرَّر من الخطة (وهي مصدره المقيس من `AudioManager`)، لا من `config`.

**الأثر:** صار الفشل **يُشخَّص من السجلّ وحده**، وصار الإرفاق **يحاول ما تعرفه المنصّة قبل ما نظنّه**.
**وحدّه المُعلَن:** هل يُقبل الإرفاق فعلًا على جهازٍ ما — **يحتاج جهازًا** (§0.1)؛ ولا صوتَ موعود قبل سماع فرق.

## ADR-62 — **مكتبة المؤثّر تُنسخ إلى مجلّد مكتبات المنصّة**، ومساراتها وسمُها وصلاحيّتها تُقاس

**الحالة:** ملزم · **السياق:** تكملة ٢٤٠ (الفجوة المقيسة: «صفر سطرٍ ينسخ مكتبة»).

**المقيس الذي أنتج القرار — ثلاثة مصادر لا رأي:**

1. `EffectConfig::parseLibrary` (AOSP، مقروء في هذه الجولة): يقرأ **سمة `path` من `<library>`** ثمّ
   `resolveLibrary` يبني `directory + '/' + path` ويختبر `access()` على كل مجلّد في `kEffectLibPath`
   (وقبلها `apex/<vendor>/…`). ⇒ **موضع المكتبة ليس موضع ملفّ التهيئة**، وقد يقرأ الأخير من `/odm/etc`
   والمكتبة من `/vendor/lib64` — ولا نجاحَ إن لم تكن المكتبة في مجلّد يبحث فيه المصنع.
2. ملفّ `audio_effects_config.xml` المرجعيّ في AOSP: `<library name="reverb" path="libreverbaidl.so"/>`
   — **`path` اسمُ ملفّ مجرَّد** لا مسار. (وهو ما يفسّر `…soundfx//lib<name>.so` في اللوق: الشرطة
   المزدوجة من `directory + '/' + path`.) ويُضاف: **سمة `type` اختياريّة على `<effect>`** «*can be used
   to add any customized effect type*» — وهو تأكيدٌ صريح لإصلاح تكملة ٢٣٧.
3. لوق جهاز المالك: `parseLibrary <name> : /vendor/lib64/soundfx//lib<name>.so` — المسار المحلول
   **بالحرف**، فالمجلّد على عتاده معلوم لا مُخمَّن.

**القرار:**

1. **خطّة مكتبة نقيّة** ([`audioEffectLibraryPlan`]): تُبنى من (مجلّد مكتبات التطبيق · العمود · اسم
   الملفّ) وتُخرج المصدر، ومسار الجهاز (`/vendor/lib64|lib/soundfx/<name>.so`)، والمسار داخل الوحدة
   (`system/vendor/...`)، والوسم، والصلاحية. وتُرجع `null` بدل مسارٍ مظنون (عمود مجهول · اسمٌ فيه `/` ·
   مسارٌ لا يُركَّب). والعمود يحدّد `lib64` أو `lib` — فلا تُنسخ مكتبة ٦٤-بت إلى مجلّد ٣٢-بت.
2. **وسمٌ مغايرٌ لوسم التهيئة:** `/vendor/lib64/…` تُوسم `vendor_file`، و`/vendor/etc/…` تُوسم
   `vendor_configs_file`. وخلطُهما يمنع التحميل بـ`avc denied`. **وتقديرٌ مُعلَن** كأخيه (§0.1).
3. **نفس عطب الصلاحية بنفس الأثر:** المكتبة تُنسخ بصلاحية **٠٦٤٤** — و٠٦٠٠ `atomicWrite` تُركّب ولا
   يقرؤها المصنع. و`post-fs-data.sh` يُصلح الملكيّة والصلاحية والوسم عند كل إقلاع (كتلةٌ **لا تُكتب
   أصلًا** بلا مكتبة، فلا سطر ميّت يوهم بقارئ).
4. **النسخ داخل معاملة المحكِّم** لا بسكربت: ذرّيٌّ (`cp` لمسارٍ مؤقّت ثمّ `mv`)، **وفشله يُسقط الكتابة
   كلها فيُحذف المجلّد** (الرجوع التامّ). و**لا `cp` في سكربت Magisk** أبدًا.
5. **ثلاث رفضات مُعلَنة بدل «نجحتْ»:** `effect-library-not-shipped-in-app` (لا شيء يُنسخ) ·
   `effect-library-not-installed` (الطبقة مكتوبة والمكتبة غائبة) · `effect-library-not-world-readable`.
   والحكم يُخفض إليها **بعد قياس الوجود ثمّ الصلاحية**، **ولا يُخفض بجهلٍ** (تعذّر قراءة الصلاحية
   يُبقي الحكم — فالجهل ليس نفيًا).

**الأثر:** صار للمكتبة مسارٌ ووسمٌ وصلاحيةٌ وخطوةُ نسخ، وصار غيابُها **يُقال** لا يُخفي. **وحدّه المُعلَن:**
(أ) **لا يُقاد الآن من الشحنة الحقيقيّة:** `libmaxfx.so` ليس داخل الـAPK بعد (لا `jniLibs`)، ولو أُدخل
فإنّ `extractNativeLibs=false` يجعل `nativeLibraryDir` مسارًا داخل الحزمة لا مجلّدًا حقيقيًّا — فالشحنة
تحتاج خطوةً مكتوبة (استخراج المدخل `lib/<abi>/<name>.so` من حزمة التطبيق إلى ملفّ مؤقّت يقرؤه الجذر، أو
`android:extractNativeLibs="true"`). (ب) **هل يجد المصنع المكتبة ويُحمّلها فعلًا: يحتاج جهازًا** (§0.1)،
ولا صوتَ موعود قبل سماع فرق.

## ADR-63 — **المكتبة تُخرَج من حزمة التطبيق** (`nativeLibraryDir` ليس مجلّدًا)، وتُشحَن في الحزمة مع بوابة تُقيس المدخل

**الحالة:** ملزم · **السياق:** تكملة ٢٤١ (استكمال ADR-62؛ الفجوة: `libmaxfx.so` لم يكن مشحونًا أصلًا).

**المشكلة المقيسة:** مسار `audioEffectLibraryPlan` في ADR-62 كان يُبنى من
`applicationInfo.nativeLibraryDir` — **وهو ليس مجلّدًا حقيقيًّا** على الأجهزة الحديثة: `extractNativeLibs`
يساوي `false` افتراضيًّا، فتبقى المكتبات **داخل الحزمة** (`base.apk!/lib/…`) ولا تُفكّ إلى قرص.
فـ`File(nativeLibraryDir + "/libmaxfx.so").exists()` = `false`، و`cp` يفشل، **فتُثبَّت الطبقة بلا مكتبة**
— وهو «صفر تغيير» في لوق المالك بسببٍ **رابع** لم يكن معروفًا حين كُتب ADR-62.

**القرار:**

1. **الإخراج من الحزمة لا القراءة من مجلّد:** [`AudioEffectLibraryStaging`] تقرأ الحزمة بـ`ZipFile`،
   وتستخرج المدخل `lib/<abi>/<اسم>.so` (وهو عُرف AGP، لا تخمين) إلى ملفٍّ في مجلّد التطبيق.
   **ويعمل في الحالتين** (`extractNativeLibs` صحيحًا أو خاطئًا) **وبلا تغيير في بيان التطبيق** — فلا
   نطلب إعدادًا يُنقص الأداء (فكّ كلّ المكتبات عند التثبيت) من أجل ملفّ واحد.
2. **اسم المدخل يُحسب حصرًا:** لا بحثٌ جزئيّ ولا أوّل مدخل شبيه — **مكتبةٌ أخرى في الحزمة ليست
   مكتبتنا**. ومدخلٌ فارغ (`size == 0`) ليس مكتبة؛ وملفٌ ليس حزمةً يعود `null` لا استثناء.
3. **الكتابة ذرّيّة والترتيب محسوب:** استخراج إلى `<name>.tmp` ثمّ `renameTo` — فلا يظهر ملفّ نصف
   مكتوب قطّ، ودورةٌ قُوطعت لا تترك «مكتبة» مبتورة. **والصلاحية 0600 مقصودة:** الملفّ في مجلّد التطبيق
   الخاصّ، **والجذر يقرأ ما لا يقرؤه غيره** (`su` بـ`uid 0`).
4. **الترتيب: الحزمة أوّلًا ثمّ المجلّد المفكوك** احتياطًا (أجهزة `extractNativeLibs=true`)، ومن
   يُرجع `null` يعني **«المكتبة غير مشحونة»** — وهي حالةٌ **تُقال** (`effect-library-not-shipped-in-app`)
   ولا تُتجاهل بتثبيت طبقةٍ بلا مكتبة.
5. **والشحن في الحزمة:** `sourceSets["main"].jniLibs.srcDir(maxfx/libs)` **بشرط وجود المجلّد وقت
   التهيئة** — فالبناء المحليّ بلا `ndk-build` لا يُدخل مصدرًا غائبًا، والـCI يضيفه دائمًا (يُبني بـ
   `ndk-build` في **المهمّة نفسها** قبل Gradle).
6. **وبوابة CI تقيس المدخل:** حرس «Validate Manager APK» صار يشترط `lib/<abi>/libmaxfx.so` للعمودين
   مع `libmaxmanager_native.so` — **فمكتبةٌ خارج الحزمة = نسخٌ يفشل = زرّ يُرفض**، وهو عطبٌ كان صامتًا
   في كل الحالات السابقة.

**الأثر:** صارت السلسلة كاملة: مدخلٌ في الحزمة ⇐ إخراجٌ مقيس ⇐ خطّةٌ بمسار البحث الصحيح ⇐ نسخٌ داخل
معاملة المحكِّم ⇐ صلاحيةٌ ووسمٌ مضبوطان. **وحدّه المُعلَن:** تغليف المكتبة وتقييم إعداد Gradle **لم
يُقيَما في هذه البيئة** (لا Android SDK ولا NDK ولا JDK 17 فيها) — والقياس الحقيقيّ للحزم في CI؛
**وقبول المصنع للمكتبة وسماع فرق: يحتاج جهازًا** (§0.1).

## ADR-64 — **حالة المكتبة تُحكَم في نواةٍ نقيّة** وتُعرض في صفّها، فلا يُقرأ «نجحتْ» عن مكتبةٍ ممنوعة

**الحالة:** ملزم · **السياق:** تكملة ٢٤٢ (إظهار السلسلة كاملة في الشاشة).

**المشكلة:** ADR-62/63 بنتا مسارًا ووسمًا وصلاحيةً ونسخًا ورفضًا صريحًا — **ولم يكن شيء من ذلك مرئيًّا**:
الشاشة تعرض حالة الطبقة (`installed` · `moduleWritable` · `sourcePath`) ولا تعرض **المكتبة** أصلًا.
فالمستخدم (والمالك) لا يفرّق بين «الطبقة رُكّبت ومكتبتها مقروءة» و«الطبقة رُكّبت ومكتبتها ٠٦٠٠ فلن
يقرأها المصنع» — **وهما في السجلّ سطران مختلفان وفي الشاشة صفر فرق**. وهذا بالضبط نمط «نجحتْ ولا يُسمع
فرق» الذي جئنا نُزيله.

**القرار:**

1. **خمس حالات مُعرَّفة في نواةٍ نقيّة** ([`AudioEffectLibraryState`] + [`audioEffectLibraryState`]):
   `NOT_SHIPPED` (لا مصدر معلَن ⇒ لا شيء يُنسخ) · `NOT_INSTALLED` (غائبة عن مسار البحث) · `READABLE`
   (٠٦٤٤ — الشرط الكامل) · `NOT_READABLE` (موجودة وغير مقروءة ⇒ يُطبع «can't find» والعلّة صلاحية) ·
   `UNMEASURED`. **والحكم في الطبقة النقيّة لا في `Compose`** — فشرطٌ مكتوب في الواجهة لا يُختبر أبدًا،
   وهذه هي حالات العطب نفسها.
2. **والترتيب محسوب:** غيابُ الخطّة أسبق (لا يُقال «غائبة» عن مكتبةٍ لم نشحنها) · ثمّ **جهلُ الوجود**
   (‏`installed == null` ⇒ `UNMEASURED` — **الجهل ليس نفيًا**، ADR-07) · ثمّ الغياب المقيس · ثمّ
   الصلاحية؛ و`mode == null` لا يُقرأ قراءةً ولا منعًا.
3. **والواجهة تُترجم ولا تحكم:** صفٌّ واحد يسمّي الحالة والمسار الذي يبحث فيه المصنع، **ووسمٌ لونيّ
   واحدٌ إيجابيّ** للحالة `READABLE` وحدها؛ وما عداها تحذيرٌ أو حياد **لا نجاح**.
4. **والمسار يُعرض في الحالتين** — القارئ يعرف **أين يُبحث** لا أنّ شيئًا «مفقود» فقط؛ وأوّل ما يُفعَل
   عند عطبٍ هو مقارنة هذا المسار بما يطبعه `EffectConfig` في اللوق.

**الأثر:** صار الفرق بين «الطبقة موجودة» و«المكتبة محمَّلة فعلًا» **مقروءًا على الشاشة**، ومعه المسار
والصلاحية. **وحدّه المُعلَن:** يُقاس هذا كله على الجهاز فقط (§0.1)؛ وما يُقاس هنا هو **الحكم** (خمس حالات
بترتيبها) والترجمة.

## ADR-65 — **حارس التحكّم يستمع ولا يستطلع فقط**، ويُفرّق «لا نملكه» عن «الجهاز عطّله»

**الحالة:** ملزم · **السياق:** تكملة ٢٤٣ (البند ٣ح-أ، معلَّق منذ تكملة ٢٣١).

**المشكلة المقيسة — وهي جوهر شكوى المالك «مهما غيّرت لا يوجد تغيير»:** كان `AudioEffectSession` يستطلع
`hasControl()` **عند الكتابة فقط**، ولا يستمع لشيء. فحالتان تُقرأان «مطبَّق» وليس فيهما أثر مسموع:

1. **فقد التحكّم بين الكتابتين:** تطبيق آخر يأخذ المؤثّر بعد أن فتحناه ⇒ المقابض المعروضة ميتة ولا شيء
   يُعلن ذلك حتى يلمس المستخدم مقبضًا.
2. **مؤثّرٌ نملكه والجهاز معطّله** (`onEnableStatusChange(enabled=false)`): الكتابة عليه **تُقبل وتُقرأ
   مطابقةً** (`expected == actual`) ثمّ **لا تُسمع** — «مطبَّق» و«صفر تغيير» في السطر نفسه، بلا سبب
   يُقال. وهذه أخطر من الأولى لأنّ كل قياس لدينا يؤكّد النجاح.

**والواجهة مقيسة بالـ`javap` قبل الكتابة:** `setControlStatusListener` و`setEnableStatusListener`
**عامّتان** (`public void`)، وكذلك `hasControl()` و`getEnabled()` — فلا حاجة لـAPI مخفيّة (ADR-07/AQ-02).

**القرار:**

1. **حالةٌ نقيّة** ([`AudioEffectControlState`]) في **ملفٍّ نقيّ** (`AudioKnobVerdict.kt`) لا في ملفّ
   الأندرويد — **فالانتقال هو ما يُختبر، والخيط في أندرويد لا.** وهي ثلاثة مدخلات معلَنة: `controlled` ·
   `enabledByEngine` (كلٌّ `null` قبل أن تُعلن المنصّة = «لم تُقَس»).
2. **والجلسة تستمع عند الفتح** (`init`) لكلا الحدثين — **وعدمُ قبول المستمع لا يُسقط الجلسة**: يُبقى
   `null` أي «لم تُبلَّغ»، ولا يُدّعى ملكٌ ولا نفي.
3. **والحراسة طبقتان في `knob()` لا واحدة:** ما **أُعلن** بين الكتابتين أوّلًا (`blockReason`)، ثمّ
   الاستطلاع الحيّ `hasControl()` (وهو المرجع في لحظة الكتابة). **بلا الثانية تُعرض مقابض ميتة.**
4. **وسببان متمايزان لا سببٌ واحد:** `control-not-owned-by-us` (لا نملكه) · `effect-disabled-by-engine`
   (نملكه والجهاز عطّله) — لكلٍّ نصّه في `values/`+`values-ar/`. **ولا يُقال للمستخدم سببان لعلّةٍ
   واحدة:** عند فقد التحكّم يُنسب الإبلاغ إليه (الأسبق) ولا يُذكر التعطيل.
5. **ومصدرُ الرمز واحد:** `disabledReason` مشتقٌّ من `disabledByEngine` نفسه لا من شرطٍ ثانٍ مكرّر —
   فحكمٌ يُقال للمستخدم لا يجوز أن يكون له مصدران ينحرفان.

**الأثر:** يُمتنع أن تُكتب قيمةٌ على مؤثّرٍ لا يُسمع أثرُها، ويُقال السبب **قبل** الكتابة لا بعدها.
**وحدّه المُعلَن:** التخفيض **اللحظيّ** على الشاشة بلا لمس مقبض يحتاج أن يُعيد الـ`ViewModel` قراءة
أسباب المحرّك من الجلسات عند كل قياس (سقف الملفّ المجمَّد يمنع إضافتها الآن) — فالانتقال يُكتشف ويُمنع
عند أوّل كتابة، **ويُقاس على جهاز** (§0.1).

## ADR-66 — **العقد المزدوج: خمسة رموز في مكتبة واحدة** (وعدمٌ لا يُصلحه إلا مكتبةٌ تصدّر الاثنين)

**الحالة:** ملزم · **السياق:** تكملة ٢٤٤ (البند ٣ي، وهو أكبر بند معلّق في الملفّ).

**المشكلة المقيسة — وهي السبب الجذريّ لشكوى «مهما غيّرت لا يوجد تغيير في الصوت»:**
مكتبتنا `libmaxfx.so` تُصدِّر رمزًا واحدًا: **`AELI`** (كائن `audio_effect_library_t`،
`maxfx/src/maxfx_effect.c:414`). وهذا هو عقد الجيل **HIDL** (أندرويد ≤11). وأما الجيل **AIDL**
(12+، وجهاز المالك Android 16) فمصنعُه `EffectFactory.cpp` لا يعرف `AELI` أصلًا: يُجري `dlsym`
على **ثلاثة أسماء حرفيّة** — `createEffect` · `queryEffect` · `destroyEffect` — وإن غابت قال
في اللوق `create (0), query (0), or destroy (0) not exist in library` و**رمى المكتبة**.

ومكتبةٌ تُرفض في المصنع **لا تُحمَّل أبدًا**: لا خطأ يصل للتطبيق، ولا سطرُ عطبٍ واحد — «نجحتْ ولا
يُسمع فرق». وهذا مُثبتٌ من ثلاثة مواضع مستقلّة، لا مستنتَج:

1. **`EffectTypes.h`** (AOSP): توقيعات الدوالّ الثلاث بالحرف — وهي المنقولة في `maxfx_aidl.cpp`.
2. **`EffectsFactoryHalAidl.cpp`** (إطار `frameworks/av`): **صفر** `dlopen`/`dlsym`/مسار
   `legacy` فيه؛ فليس في الإطار طريقٌ ثانٍ يحمّل مؤثّرًا محليًّا على جهاز AIDL. (و`EffectsXmlConfigLoader`
   الذي يحمّل `AELI` بـ`dlopen` ما زال في الشجرة، لكن لا يمرّ منه جهازٌ بعقد AIDL.)
3. **تغيير LineageOS MERGED** (٥٠١٢٢٨، `lineage-24.0`): «Add an AIDL wrapper for legacy effect
   libraries» — كُتب **لنفس السبب**: «the HIDL effect HAL (which was itself only a wrapper around this
   C API) … a device moving to an AIDL core HAL would otherwise lose every prebuilt effect it ships».
   أي أنّ الصناعة نفسها اضطرّت لبناء غلافٍ لأنّ العقد القديم مات — وهو ما نبنيه هنا داخل مكتبتنا.

**القرار:**

1. **المكتبة الواحدة تصدّر الخمسة:** `AELI` **و**`createEffect`/`queryEffect`/`destroyEffect`
   معًا في `libmaxfx.so`. لا رمزَ يُناقض آخر (كائنٌ `<name>` ودوالُّ بأسماء أخرى)، ولذلك يعمل
   الجهاز على الجيلين بلا نسختين ولا تفريع في الشحن. **و`AELI` لا تُحذف** (ADR-18): أجهزة
   ≤11 قائمة، وهي تعمل بها اليوم.
2. **ولا نواة ثانية:** المعالجة كلّها في `maxfx_dsp` نفسها، وقناة التحكّم تبقى **الخاصيّات**
   (`persist.audio.maxfx.*`). الغلاف الجديد **مُحوِّلٌ لعقد**، لا نسخةٌ ثانية من المؤثّر.
3. **والنسخ المجمَّدة تُثبَّت بأرقامها لا بـ«current»:** `audio/aidl/Android.bp` يعلن للإصدار **1** من
   `android.hardware.audio.effect` استيراد `android.media.audio.common.types-V2`، والحزمة تُصدِّر
   `android.hardware.common-V2` و`android.hardware.common.fmq-V1`. فهذه النسخ الأربع هي التي كُتب
   بها الإطارُ الذي يقرأ ما نُرسل — وتخطيطُ الـparcelable هو العقد السلكيّ، ونسخةٌ أحدث تقرأ بايتاتٍ
   في غير مواضعها **بلا خطأ ظاهر**.
4. **و`libfmq` تُبنى معنا لا تُخترع:** عقد AIDL لا يقبل مؤثّرًا بلا مقابض دخلٍ وخرج (ثلاثة FMQ في
   `OpenEffectReturn`)، و`libfmq` **ليست في الـNDK**؛ فمصدرها منسوخٌ في `maxfx/third_party/libfmq`
   (Apache-2.0) مع بدائل `compat/` لأنّ `libbase`/`libcutils`/`libutils` ليست في الـNDK أيضًا.
   **والبديل المرفوض صراحةً:** كتابة تخطيط الذاكرة المشتركة يدويًّا — عطبُ صوتٍ صامت مقابل كودٍ منسوخ
   يُقاس.
5. **والبوابة على الرموز لا على «نجح البناء»:** CI يقيس `readelf --dyn-syms` على `libmaxfx.so`
   **للعمودين** ويُسقط إن غاب أيٌّ من الخمسة — فمكتبةٌ تُبنى ولا تُحمَّل هي بالضبط ما نسدّه.
6. **وحدودٌ مُعلنة في الغلاف الجديد:** `float` حصرًا (كما في `EffectImpl` المرجعيّ)، وقناتان أو
   واحدة وما عداهما مرورٌ حرفيّ، و`setParameter` يقبل ولا يُطبِّق (التحكّم من الخاصيّات) — فرقٌ
   مقصود عن المرجعيّ الذي يرفض ما لا يعرفه.

7. **واللقطات المجمَّدة لا تُنسخ أعمى وتُقاس بلا مُصرّف:** الجلبُ الأول لمجموعة الـ٩٤ ملفًّا
   **فشل لعشرةٍ منها صامتًا** (ملفّات فارغة أو ببقايا base64)، ولم يكشفه `kt_balance` (ملفٌّ
   فارغ متوازن) ولا `code_health` (ولا نصّ فيه). فصار للقطةٍ حكُمُ قبولٍ مُعلَن (حجم · سطر
   `package` واحد · علامة `IMMUTABLE` · قوس إغلاق) في `tools/fetch_maxfx_aidl.py`، **ويُستورد
   حكمه ولا يُعاد كتابته** في `tools/maxfx_contract.py` — مصدرٌ واحد، إذ مصدران ينحرفان =
   حكمان. وتقيس البوابةُ — **بلا مُصرّف** — الرموزَ الثلاثة وتوقيعاتها وبقاءَ `AELI` وسلامةَ
   اللقطات وإغلاقَ تضمين `libfmq` من أبوابنا الفعليّة واتساقَ البناء، مع `--self-test` يقيسها
   على أعطابٍ مغروسة. **وهذا قرارٌ لا تفصيل:** كل ما لا يُقاس إلا بعد بناءٍ يُقاس متأخّرًا.

**الأثر:** لم تبق ثغرةُ عقدٍ بيننا وبين أي جهاز: «مكتبةٌ تصدّر الخمسة يقبلها الجيلان». **وحدّه المُعلن:**
هذا يُزيل **رفض المصنع**، ولا يُثبت أن الصوت يتغيّر — فقبول `audioserver` وتحميل المكتبة وسماع الفرق
**يحتاج جهازًا** (§0.1). وما قِيس هنا هو البنية والتوقيعات والرموز، لا الصوت.

## ADR-67 — **ما يفرضه الـNDK على بناء عقد AIDL**: رؤوسٌ منسوخة · منصّةٌ `31` · ومصادرُ الرزم الأربع

**الحالة:** ملزم · **السياق:** تكملة ٢٤٥ (بناء `libmaxfx.so` أوّل مرّة، ثمّ نجاحه في CI).

ثلاثة قيود اكتُشفت **بالقياس في مُصرّف حقيقيّ** بعد أن كان الغلاف ٥٩٤ سطرًا لم تُترجم مرّة واحدة.
وكلٌّ منها كان يُكلّف دورة CI كاملة لو خُمِّن، فلذلك تُكتب هنا بأدلّتها:

1. **رؤوس الـC++ لـ`libbinder_ndk` منسوخةٌ عندنا** (`maxfx/third_party/libbinder_ndk_cpp`، ٨ رؤوس
   Apache-2.0 · ٣٢٠٤ سطرًا). السبب مقيسٌ على حزمة **NDK r29 نفسها** (`android-ndk-r29-linux.zip`
   · `Pkg.Revision = 29.0.14206865`، وهو بالحرف ما يستعمله CI) بقراءة فهرسها بنطاقات HTTP بلا
   تنزيلها: `sysroot/usr/include/android/` فيه **واجهة C فقط**، وكلّ رؤوس الـC++ غائبة، و**لا مجلّد
   `platforms/` في الحزمة إطلاقًا** — أي أنّ مسار `platforms/android-31/optional/libbinder_ndk_cpp`
   الذي وصفته مسألة NDK `android/ndk#2130` (المُغلقة «كما هو مقصود») **لا وجود له في r29**. والمسلك
   هو مسلك `libfmq` نفسه: تُنسخ رقعةٌ لا تُخترع، وتُحرس ببوابة.
2. **والرأس المنسوخ لا يُخلط بالنظام:** نُنسخ **ما غاب فقط**، ويبقى `binder_ibinder.h` و`binder_status.h`
   و`binder_parcel.h` من الـNDK — فرأسٌ زائد عندنا يُظلّل رأس الـNDK (نسخُ طبقةٍ بدل رقعة).
   والباقي ([`binder_shell.h`](../maxfx/third_party/libbinder_ndk_cpp/README.md)) **يُترك عن قصد**:
   يُضمَّن بـ`__has_include` وغيابُه يُطفئ مسار `dumpsys` لخدمةٍ لسنا هي.
3. **والمنصّة `31` لا `29`:** من جهة الوظيفة (ولادة مصنع AIDL و`ParcelableHolder`)، ومن جهة الحصانة
   (‏`libbinder_ndk.so` مشحونٌ في الـr29 للإصدارات **٢٩–٣٥ فقط**، فلا يُقفز إلى ما لا يُشحَن).
   **وثمنه مُعلَن:** المكتبة تُوسم بالجيل 12+، فمسار `AELI` على أجهزة ≤11 يبقى مُصدَّرًا لكنّ تحميله
   عليها لم يعد مضمونًا كالسابق — وهذا **حدّ عتادٍ يُحتاج جهازًا لقيسه**.
4. **و"الرؤوس تكفي" غير صحيح: المصادر تُصرَّف أيضًا — وللرزم الأربع.** قِيس ذلك بثلاث سقطات متتالية
   في CI، وكلّها **ربطٌ** لا ترجمة: `BnEffect::createBinder()` (تعريفه في مصدر النوع المُولَّد)، ثمّ
   `NativeHandle::readFromParcel` و`GrantorDescriptor::writeToParcel` و`AudioUuid::…` (تعريفاتها في
   مصادر رزم `hardware/common` و`hardware/common/fmq` و`media/audio/common`). والسبب بنيويّ: رأس
   المولِّد يضمّ هذه الأصناف **كنماذج** في `binder_parcel_utils.h`، فيُصدِر المُصرّف نداءً بلا تعريف.
   والمُصرَّف هو **مصادر رزمنا الأربع كلّها**، في **موضعين** لكلٍّ منها (`gen/android` للمصادر و
   `gen/aidl` للرؤوس) — وهما مجلّدان مختلفان لأنّ المولِّد يبني مسار المصدر من اسم الحزمة وحده.
5. **والبوابة تمتدّ إلى مسار الربط:** `tools/maxfx_contract.py` صار **٨ فحوص** (كانت ٦): فحصٌ
   للرؤوس المنسوخة (وجود · حجم · `#pragma once` · ترخيص Apache · **لا ظلّ على الـNDK** · إغلاق
   تضمينها الفعّال بوعي حرّاس `__has_include`)، وفحصٌ لنطاقات الأسماء (`::android::media::` و
   `::android::hardware::audio::` بلا `aidl` = عطب)، مع `--self-test` يُثبت الحالتين لكلّ قاعدة.

**وحدّ مُعلن:** كلّ ما تقدم يُثبت أنّ المكتبة **تُبنى وتُصدَّر**؛ ولا يُثبت أنّها **تُحمَّل ويُسمع أثرها**
— فذلك يحتاج جهازًا (§0.1)، وهو ما بقي.

## ADR-68 — أنماط الصوت: إعدادات مقروءة ومقارنة قابلة للرجوع، لا وعد بمعالجة مسموعة

**السياق:** أمر المالك «اكمل وحسن اكثر شاشة الصوت واجعله متطور ورائع»؛ التنفيذ `AUDIO-PRESETS-01`.

1. الأنماط الستة وصفات أصلية فوق مؤثرات Android الحالية، عبر `AudioEffectBackend` والمحكّم نفسه؛ لا root shell جديد ولا تجاوز للملكية ولا كود من المشاريع المرجعية.
2. النجاح يعني كتابة كل معاملات الخطة وقراءتها؛ الفشل يوقف الخطة ويجرب استعادة كل المقابض الملموسة، بما فيها المقبض الذي فشل. رفض الاسترجاع يُعلن ولا يُخفى.
3. «قبل» يستعيد إعدادات ما قبل أول نمط، و«بعد» يعيد آخر خطة ناجحة؛ المقارنة نقرٌ واضح قابل للوصول، لا ضغط مطوّل يعتمد على توقيت coroutine. الإيقاف يستعيد الإعدادات ولا يكتب أصفارًا اعتباطًا.
4. EQ يستوفي لوغاريتميًا عند مراكز الجهاز المقروءة (Android يعيد milli-Hz)، ويطرح أعلى رفع من المنحنى ليكتب خفضًا فقط ضمن حدود الجهاز. `preAmpDb` وصف نظري **لا** preamp مطبّق ولا limiter؛ BassBoost/Loudness قد يسببان تشويهًا، فلا ضمان نقاء.
5. الميزة الأساسية تستلزم جلسة وقراءات صالحة؛ الإضافات الثانوية غير المتاحة تُتخطّى ويُقال اسمها. تغيير النمط يعطّل المؤثرات غير المستخدمة من المجموعة المُدارة فقط. لا يُغيّر Dolby/MaxFx/Dynamics تلقائيًا.
6. التحكّم اليدوي مطويّ ومحجوب أثناء النمط كيلا يفسد خط أساس المقارنة. MaxFx يبقى تجريبيًا داخل الأدوات ولا يُحذف ولا يوصف بأنه يعمل على كل هاتف.
7. تمكين المؤثّر نفسه مستثنى من حارس «المحرك عطّله»، **لا** من حارس الملكية أو المحكّم: وإلا يستحيل Off→On. ترتيب الرجوع: تمكين مؤقت، قيم، ثم حالة التمكين الأصلية.
8. لا خدمة خلفية أو Shizuku جديدة في هذه الجولة؛ الجلسات تعيش مع ViewModel الملاحة وتُحرّر في `onCleared`. أثر مسموع، حياة الخلفية وتعارض التطبيقات وRTL/اللمس تحتاج جهازًا، ومراجعة السلامة المستقلة معلّقة.

### متابعة ADR-68 — AUDIO-UX-02 (2026-10-02)

- اختيار البلاطة صار **معاينة دون كتابة**، ويحتاج زر «طبّق»؛ هذا تغيير متعمد بطلب معاينة النمط قبل تطبيقه. النمط المعروض في بطاقة الحالة هو آخر تطبيق ناجح، لا البلاطة الجاري فحصها.
- القوة `0/50/100` تعني `0.35/1.0/1.4` من الوصفة، بمنحنى متصل ومتزايد؛ الافتراضي50. لا ادعاء أن0 إيقاف أو أن شدة المعالجة تساوي الجهارة المدركة.
- التشخيص قراءة صريحة فقط، بلا فتح جلسات أو تمكين أو كتابة. يقرأ الاتصال والتمكين وملكية التحكم والقراءات ويطابق الخطة المحفوظة للجانب الحالي من A/B. خطّة غائبة أو قيمة غائبة = مجهول، لا نجاح.
- التشخيص **لقطة غير مستمرة** تُمحى عند محاولة تغيير النمط/المقارنة وعند إعادة تحميل جرد الجهاز. نتيجة التطبيق السابقة تُسمى «آخر تطبيق» لا حالة حيّة.
- أولsink فيAudioInventory مخرج مكتشف، وليس مسار التشغيل النشط المثبت؛ بطاقة الحالة تقول ذلك صراحةً. المعاينة تقترح محاولة فقط، ولا تضمن بقاء الملكية بعد القراءة.
- حفظ أنماط لكل مخرج، منع قفزات الجهارة بقياسPCM، ومحركDSP أعمق ليست ضمن هذه المتابعة ولم تُنفّذ.

### مراجعة ADR-68 — AUDIO-REVIEW-03

الرجوع عند الفشل يستخدم تمكينًا مؤقتًا قبل معاملات المؤثّر وحالة التمكين الأصلية بعده، حتى إن لم تصل الكتابة بعد إلى مفتاحenabled. خطةAfterتضمقيمخطالأساسللمؤثراتالمستخدمةفيأنماطسابقة، فلايرجعA/Bبقيمناقصة. خطةcut-onlyترفضمدىEQالموجبفقطوالفهارسالمكررة. لاتعطيللمؤثرغيرمستخدمإذاكانتملكيتهغيرمقروءةtrue؛ الحارسالمركزييبقىمرجعكلعملية. الاختبارات تثبتترتيبالقراراتوالقيملاالصوتالمسموعأوتوافقROM.

## ADR-69 — **سجلّ الأدلّة يشترط التصديقَ لا التكرار، والقراءة مرايا بدل سؤال، ومفتاح من البناء لا من الورش**

**السياق:** طلب المالك «حماية من إعادة بناء التطبيق» مع أمرين ثابتين: لا يُمنع المستخدم المتجذّر من تشغيل التطبيق على جهازه (جذور/KernelSU/روم/‏SELinux/خيارات مطوّر ليست أدلّة أبدًا)، ولا تُطوَّر إمكانية بلا قياس هنا.

### القرارات

1. **الحكم في سجلّ ثابت يُقاس في JVM** (`IntegrityLedger`/`IntegrityModel`): دليل قاطع واحد يكفي (اختلاف موقّع · أصل مختوم معدَّل · مرساة أصلية مكسورة · محتوى حزمة مرفوض · مكتبة native مستبدلة)، والإشارات غير القاطعة تحتاج **إشارتين مختلفتين** لا إشارتين مكررتين. الإشارة الواحدة في مواضع متعددة = دليل واحد — عدّ المواضع كان يرفض مستخدمًا سطحه يحمل مكتبة هوك لتطبيق آخر. الرفض على `TAMPERED` في release وحده؛ debug يرصد ولا يرفض (`IntegrityPolicy`).
2. **القراءة مرايا مستقلة، لا سؤال واحد موثوق:** بصمة الموقّع عبر `PackageManager` **ومباشرة من ملف الحزمة** (`ApkSigningBlock`: v2/v3/‏v3.1 — فـAGP لا يُنتج v1 عند minSdk≥24، والمسار الثاني بمكتبة `CertificateFactory` كان مكتوبًا ولا يعمل). والمكتبة الأصلية المستخرجة تقارن بمحرّكين (JDK + native)، وتحقيق **محتوى** الحزمة كاملًا عبر `com.android.apksig` (`ApkContentVerification`) — الفحص الوحيد الذي يرى تعديل DEX على جهاز يُبطل فيه تحقّق النظام ويبقى التوقيع سليمًا. ومرّة الإقلاع وحدها (حزمة كاملة؛ مقاس ≤ 110 ms على حزمة مصنوعة).
3. **`setMinCheckedPlatformVersion` من منصّة التثبيت نفسها** (`applicationInfo.minSdkVersion`) لا ثابت: الفحص بثابت يرفض حزمة سليمة على منصّة قديمة تطلب v1 أو يمرّر ما لا يُطلب عليها.
4. **البصمة مشتقّة زمن البناء من مخزن المفاتيح** (`SignerPinValueSource` → `BuildConfig.SECURITY_PIN_*` مُقنَّعة XOR)، لا مكتوبة بيد. لا مزلق، ولا سرّ (الشهادة مرئية في كل حزمة). غياب البصمة ⇒ وضع رصد لا رفض.
5. **أصول الملكية تُختم بمفتاح مشتقّ من البصمة** (`SealAssetsTask` ⇒ `MMVAULT1‖salt‖iv‖AES-GCM`)، وتُسحب من `assets/` إلى `assets-internal/` كي لا يُشحن الأصل الصريح مع المختوم. الفشل في الفك **ليس دليل تلاعب** — الحكم من قائمة الختوم وحدها. **وقياسها هنا:** الخاتم والفك (كودان مستقلّان) يعودان بالأصل الأصلي بايتًا ببايت (`SealedAssetsTest` — 3 أصول، والقائمة تُقاس). ولم يُشحن APK إصدار بعد، فقياس «ما داخل الحزمة الشاحنة» **غير مُتحقَّق في هذه البيئة** ولا يُدّعى.
6. **المرساة الأصلية** (`security.rs` عبر `SecurityBridge` بتّ): تراكم `TracerPid`/خريطة هوك/منفذ تصحيح من كود مُصرَّف. غياب المكتبة يُسجَّل ولا يُحتسب دليلًا — بناء محلي بلا NDK يجب أن يعمل.
7. **الرفض إعلان لا انهيار** (`SecurityEnforcement`): حوار غير قابل للإلغاء في النشاط، `stopSelf` في الخدمة، صمت في المستقبِل؛ والنص لا يذكر أي فحص رُصد. بوابات: النشاط، الإقلاع (العملية)، كل خدمة، كل مستقبِل.
8. **R8:** `-repackageclasses 'o'` + `-optimizationpasses 5` + إخفاء أسماء ملفات المصدر (بإبقاء `LineNumberTable`) — يُعلن حدّه: إعادة تسمية لا تعمية. وحارس السلامة `core/security/**` **بلا قاعدة إبقاء** عن قصد (كل نداءاته مباشرة). و`-keepclasseswithmembernames native <methods>` يأتي من الإعداد الافتراضي فلا تكرار.
9. **`CompileSdk=37`**: لأن من في القياس التزم أحدث واجهة (`platform 37` مثبّت محليًّا وفي CI).

### ما قِيس فعليًّا — يُشار إليه لا يُعاد

- **البوابة الوحيدة للأنواع:** `:app:compileReleaseKotlin` → **BUILD SUCCESSFUL** (11m 14s؛ بعد إصلاح 4 عطب ترجمة: مرجع `IntegrityVerdict` اليتيم، قارئ `Long`/`Int`، `byteInputStream`→`inputStream()`).
- **اختبارات نواة الأمان (JVM، بلا Gradle): `tools/test_security_jvm.py` → 29/29 ناجحة**، وفيها قياس حقيقي: حزمة **موقّعة فعليًّا** بـapksig 9.2 → VERIFIED (61–107 ms)؛ نفس الحزمة بقلب **بايت واحد** في `resources.arsc` → REJECTED (48 ms)؛ وفحصها كحزمة لمنصّة تطلب v1 → REJECTED (إثبات أن المنصّة معاملٌ مقيس لا تفصيل). الأداة على نمط `test_maxai_jvm.py`: بلا تنزيل، ولا تعرف Gradle.
- **سقف هذه الأداة (معلن):** تستبدل `DiagnosticCenter` و`BuildConfig` ببذرة، فقيمها للقياس فقط؛ البوابة الحقيقية للأنواع تبقى `:app:compileReleaseKotlin`.
- **Gradle `testReleaseUnitTest`/`assembleDebug` غير مكتملة في هذه البيئة:** `aapt2` من AGP 9.2 x86-64 فقط، و`aapt2` المثبّت مُقادم (`2.19-debian`) ⇒ يفشل عند `mergeReleaseResources`/`processReleaseResources` (عطب بيئة موثّق، جرى تشغيله واقيًع). البوابات الخفيفة كلها خضراء: `kt_balance` (2297 ملفًا·0) · `code_health` (نظيف) · `i18n_coverage --assert` و`--prune all --assert` (0) · `jni_symbols --assert` (23·0 نواقص) · `cargo test` (64 ناجحة من `security.rs`).
- عطبان اختباريان تقطعهما وأُصلحا **قبل** الحكم: تكديس الأطوال في بنية المصنوعة (`ApkSigningBlockTest`) نقيض المواصفة، وقراءة الطول القصير بـ`uint32` في الأداة. الاثنان لم يعيقا تحكّم `:app:compileReleaseKotlin`.

---

## ADR-70 — **التصنيف الذي يكتبه صاحب الجهاز: ملفّ تعليم لا جدول أجهزة، وفحص قبل الخزن، وفجوةٌ تسمّي فعلها**

**السياق:** طلب المالك ميزة «Memory & UFS» مستوحاة من مشروع مرجعيّ مغلق (Rodin Essential 1.18.5) **بفارق ساحق** وأن تعمل «على أغلب الأجهزة» ومرتبطة بـMax Atlas. والمشروع المرجعيّ يحقّق ذلك بكتابة مسارات بطاقات بعينها في سياسته (`/devices/platform/soc/1c00f000.dvfsrc/…` و`112b0000.ufshci`)، وبملفّ `--labels labels.json` يكتبه خبير حين يقصّ المعنى الأصليّ (`policy/rodin-essential.cil`). ودرسُ الحزمة الصالح عندنا ليس المسارات (رقمٌ على ناقل SoC واحد)، بل **آلية الاحتياط حين يجهل المحرّك الاسم**: قائمةٌ تقول ما لم يُعرف، وملفُّ تعليم يملأ النقص بلا تعديل شفرة.

### القرارات

1. **المنسّق لا ينمو بجدول أجهزة.** مفردات التصنيف في `MemoryUfsHardwareBackend` مرصودة من سائقات معلومة (`dvfsrc` · `emi` · `cpubw` · `llccbw` · `mif` · `int` · `ufs` · `nvme`…)، والمطابقة **على حدود الرمز** (`int` لا تُطابق `interconnect`)، وما لا يُعرف **لا يُسقط صامتًا** بل يُعرض ([Discovery.unclassified]) كما هو الوصول في المشروع المرجعيّ («الأداة تسرد الوسوم الناقصة»).
2. **ملفّ تعليم نصّي في مجلّد التطبيق الخاصّ** (`hardware-overlay.txt`) لا قاعدة بيانات ولا SharedPreferences: نصف قيمة الفكرة أنها تُنسخ إلى جهاز آخر. الصيغة `اسم الجهاز ثم الصنف`، وتُرفض الأسطر بأسبابها (`overlay-line-shape` · `overlay-name-invalid` · `overlay-kind-unknown` · `overlay-graphics-owned` · `overlay-device-conflict` · `overlay-entry-limit`) وسطرٌ خاطئ **لا يُسقط الملفّ**؛ ويُقرأ في ثلاث نقاط معلنة (إقلاع العملية · فتح شاشة الذاكرة وUFS · بعد كل تعليم/نسيان).
3. **ثلاثة حدود لا تُجتاز:** القاعدة المبنية تفوز دائمًا (التعليم لا يغيّر صنف جهاز معلوم)، واسمٌ يحمل رمزًا رسوميًّا يُرفض (لأن `gpu_frequency:<device>` له مالكه، ومقبضٌ له مالكَان هو العطب المحظور)، وصنفان فقط (ذاكرة/تخزين) فلا تُبنى بها ملكية ثالثة.
4. **الفحص قبل الخزن** (`probe`): لا يُكتب في الملفّ اسمٌ لعقدة لا يُقرأ منها سلّم تردّد، ورمز الرفض يُعرض بجمله. وهو نقل مبدأ «لا مخرجات صالحة بلا اجتياز التحقق» من أداة الدمج عندهم إلى مقياس جهاز هنا.
5. **الرمز يقول الفعل.** حين تُرصد عقدة بلا صنف، فجوة خريطة أطلس تقول `unclassified-devfreq-node-seen` — لا `adapter-gap` ولا «لم يُرصَد» — لأن الفرق عمليّ: الأول يُغلق بنقرة تعليم، والثاني لا شيء فيه يُعلَم. والقراءة تُحتسب قراءةً حين تجيب عقدة مجهولة (وإلا كان الحكم مخالفًا لما قيس).
6. **التحقق احتواءً لا تساويًا في مقبض المدى.** `satisfied` تقبل مدى أضيق ممّا طُلب (حرارة/طاقة/سقف مصنّع يضيّقه) وترفض قيمةً تجاوزت الطلب أو نزلت تحت الأرضية أو قراءةً صفرية/غائبة. والتساوي الحرفيّ كان يقرأ ضبطًا ناجحًا فشلًا فيُعيد خط الأساس — أي يُلغي ما طلبه المستخدم ويقول له «فشل».

### ما قِيس فعليًّا — يُشار إليه لا يُعاد

- **الفحص النوعي:** `643` مصدرًا في `manager/app/src/main/java` تمرّ على `kotlinc 2.3.10` الحقيقيّ مع إضافة Compose و`android.jar` 37 والرزمة المخزنة ⇒ `KOTLIN_EXIT=0` وصفر خطأ (الأداة في `/tmp/kverify-memufs.sh` على أسلوب `/tmp/kverify-compose.sh`؛ ومنه الانكشاف الحقيقي: صنف عامّ يرث صنفًا داخليًّا، و`Modifier` بلا استيراد).
- **التشغيل:** JUnit 4.13.2 على JVM بمدخلات مصطنعة ⇒ `OK (72 tests)` في أربع مجموعات (`MemoryUfsHardwareBackendTest` · `AtlasMemoryUfsAdaptersTest` · `HardwareDeviceOverlayTest` · `AtlasCapabilityMapTest`). وأول تشغيل كشف **٧ إخفاقات** = **٥ أعطاب إنتاجية** (`Device.level` يُعلن READY بلا مسار مدى · `satisfied` تساوٍ حرفيّ · `requestForFraction` يُسقط الأرضية · `assess` يُنطبق بحاكم قابل للكتابة وحدها · عائلة `memlat` تُنسب لمصنّع لا يُعرف) و**٣ اختبارات متناقضة** كُتبت بلا تشغيل فصُحّحت.
- **بوابات CI السبعة عشر:** كلها خضراء بعد ثلاثة إصلاحات حقيقية: فاصلة عليا غير مهرَّبة في نصّ إنجليزيّ (`code_health`)، وعدد الشاشات المتقادم ٦٠ ← ٦١ في الصفحتين و`docs/features.md` (`readme_assets`)، وملفّان جديدان في `kt_balance` (2313·0). و`resource_compile` بـ`aapt2` الحقيقيّ ✓، و`license_audit` ✓، و`source_manifest --assert` ✓ (بلا تحديث).
- **الحدود:** لا Gradle ولا جهاز في هذه البيئة (البناء المُصرَّف الكامل غير مُنفَّذ)، والشاشة واللمس وRTL وPSI على جهاز حقيقي لم تُرَ، وعتبة الخنق `10٪` غير معايَرة (I-65 قائم).

---

## ADR-71 — **السبب المُعلَن هو السبب المقيس: ثلاثة أحكام لا حكمان، و«لا أقرأ» ليست «لا يوجد»** (MEMORY-UFS-TEACH-01ب)

**السياق:** في تحديثات الحزمة المرجعية (Rodin Essential 1.18.5) اعترافٌ بعطبٍ وقع لهم: قسم بطارية ظهر فارغًا، وبقي التشخيص «غير مدعوم» حتى اكتُشف أن السبب **عبور مجلّد أب** في vendor غير ممنوح (`sysfs_batteryinfo:dir search`؛ ونصّهم: «Missing parent traversal was the cause of the reported blank battery section»). أي أن العطب لم يكن في الميزة بل في **سببٍ أُعلن خطأً**، فبحث صاحب الجهاز في غير موضعه. وفي مستودعنا كان هذا الخلط نفسه قائمًا في موضعين: (أ) `discover` يُعيد سببًا واحدًا `devfreq-root-unreadable` حين يكون التعديد فارغًا — صحيحٌ إن كان الجذر محجوبًا، **خطأٌ** إن كانت النواة لا تنشر صنف `devfreq` أصلًا (فيُطلب من المستخدم جذرٌ لا يُغيّر شيئًا)؛ (ب) `read` يحكم `NotAnInterface` على عقدة **لم يُقرأ منها سطر** — فيُقال «ليست واجهة تردّد» وهي محجوبة فقط.

### القرارات

1. **الجذر ثلاثة أحوال لا حالان:** التعديد أجاب · الجذر موجود ولا يُعدَّد (`REASON_ROOT_UNREADABLE`، نقصُ قياس) · الجذر غير موجود (`REASON_ROOT_ABSENT`، غيابٌ **مقيس** فتُعلن `scanned = true` ولا تُطلب صلاحية).
2. **والعقدة ثلاثة أحكام:** `Classified` · **`Unreadable`** (رُصدت ولم يُقرأ منها شيء ⇐ `Discovery.unreadable` ⇐ رمز فجوة `devfreq-node-unreadable`) · `NotAnInterface` (**قُرئت** ولم تنشر تردّدًا). ومعيار الفصل **مقيس**: أي قراءة أجابت (ومعها `device_name` الذي تنشره عُقَد devfreq كلّها) ⇒ المجلّد مقروء.
3. **و«لم تُقرأ» ليست «ليست واجهة تردّد»** — لا في العرض ولا في فحص التعليم: `PROBE_UNREADABLE` رمزٌ ثانٍ، و`NotAnInterface` تبقى للعقدة التي **أجابت** بشيء. وكان الرمز واحدًا فيُقرأ اسمٌ مطبوعٌ خطأً «العقدة ليست واجهة» وهي لم تُقرأ.
4. **والعقدة المحجوبة لا تُعرَض للتعليم:** لا شيء فيها قيس ليُعلَّم، فتظهر في قسم «ما رُصد ولم يُقرأ» بفعلها (صلاحيةُ عبور أو مسارٌ مصحَّح) لا بزرّ تعليم — وهذا نصّ ما يجعل القسمين مختلفين لا متكرّرين.
5. **ورموز الفجوة لا تُدمج:** `unclassified-devfreq-node-seen` يقول «سمِّ الصنف» (نقرةٌ واحدة)، و`devfreq-node-unreadable` يقول «لم يُقرأ شيء» (صلاحية/مسار) — ورمزٌ واحد للاثنين يطلب الفعل الخطأ.

### ما لا نأخذه من الحزمة (وبيانُه)

- **خدمة الجذر وتهيئة init والسياستين** (`rodin_daemon.rc` · `rodin-essential.cil` · دمج XML/contexts/شهادة التوقيع): لا نظير لها في تطبيقٍ يُثبَّت كمستخدم ولا يبني ROM — والحاجة التي تخدمها (الكتابة على عقد sysfs) عندنا تُقضى بالمُحكِّم `HardwareControlArbiter` ومفتاحٍ قياسيّ لكل مقبض، لا بخدمةٍ دائمة بصلاحية الجذر.
- **مسارات بطاقة بعينها** (`1c00f000.dvfsrc` · `112b0000.ufshci`): رقمٌ على ناقل SoC واحد وعقدةٌ غائبة على الكيرنل التالي (ADR-70).
- **وملفّ الاعتماد:** لم يُنسخ كودٌ من الحزمة (لا Rust ولا Kotlin ولا CIL) — والأفكار المستعارة (قائمة النقص · ملفّ التعليم · الفحص قبل الخزن · رمز الفجوة · فصلُ السبب المقيس) مُعلَنة باسمها في `HANDOFF` تكملة ٢٧٤/٢٧٥، وهي منهجٌ لا رسم، وفق ADR-54.

---

## ADR-72 — **المفتاح يُولَّد على الخادم ولا يُخزَّن، والشهادة تُربط بجهاز، والبوّابة عند مدخل الوجهة** (LICENSE-KEYS-01)

**السياق:** طلب المالك منظومة مفاتيح كاملة: ينشئها من لوحة التحكّم ويبيعها، يدخلها المستخدم في
MaxManager، مع تتبّع ومنع تلاعب وحدّ أجهزة ومنع مشاركة ومدّة محدّدة لكل مفتاح، وفترة مجّاني
للجميع **يتوقّف داخلها عدّاد المدفوع**، ورابط تنزيل يكتبه بيده. والمستودع فيه بالفعل: خادم
Supabase، وأوّليّات توقيع P-256 في `core/security`، وعقد إعداد موقّع في `core/remote` **بلا مستهلك**،
ولوحة مالك تنشر إلى `app_config`. فالسؤال المعماري كان: أين يقع كل حكم، وما الذي يُخزَّن.

### القرارات

1. **نصّ المفتاح لا يُخزَّن في أي جدول.** الخادم يولّد (‏`MAXM` + ٢٠ محرف Crockford + محرفا تحقّق)
   ويُعيده **مرّة واحدة**، ويحفظ `sha256` + بصمة عرض (`key_hint`) فقط. والتطبيق يرسل البصمة لا النصّ.
   والنتيجة المقيسة: جدول مسروق لا يحتوي مفتاحًا صالحًا واحدًا.
2. **شهادة موقّعة تعمل بلا إنترنت، ومرساةُ زمنٍ لا تُرجَع.** التفاعل الشبكي مرّة عند التفعيل، ثم
   `SHA256withECDSA` على الجهاز. وزمن الخادم يُثبَّت أماميًّا في **كل** ردّ — ومنه ردود الرفض
   (`denial` تحمل `server_time`، و`LicenseStates.anchored` + `noteAttempt(serverMillis)`) — فباب
   «انتهى المفتاح ← رجّع الساعة» يُغلق بعد أول اتصال. وحدّه المُعلن: من لم يتصل قطّ بعد الانتهاء
   يبقى على حكم ساعته، ولا يُدَّعى غير ذلك.
3. **الربط بمعرّف تثبيت عشوائي، لا بمعرّف عتاد.** ٣٢ محرف hex من `SecureRandom` في تخزين التطبيق
   الخاصّ. فسياسة الخصوصية لا تُخرق، ونسخ بيانات التطبيق إلى هاتف آخر يُرفض بـ`bound_other_device`
   (وهو «منع المشاركة» بمعناه القابل للقياس)، وتصفير المقاعد يُبقي الأثر في السجلّ.
4. **نوافذ المجّاني في الإعداد الموقّع، والخادم هو من يحسب الإيقاف.** `promos` داخل البايتات
   الموقّعة، والمدّة تُعاد حسابها عند **كل** تفعيل/تحقّق بدالّة `addDaysSkipping` (لا وقتٌ محسوبٌ
   مرّة فيتقادم)، فالمدفوع لا يخسر أيام النافذة. ولو كانت النوافذ في التطبيق لمنح كل جهاز نفسه عطلةً أبدية.
5. **البوّابة عند مدخل الوجهة لا عند كل زرّ.** حكم واحد (`FeatureAccessRules.access`) وثلاث حالات
   (`on`/`subscribe`/`off`): ميزةٌ تُشترى **تبقى ظاهرة ومقفلة** (وإلا صار الشراء سرًّا)، وميزةٌ
   أُطفئت تختفي. ومعرّفات المفاتيح ثوابت في `RemoteFlags` تُنادى بها البوّابة، لأن معرّفًا منزاحًا
   يُعيد `OFF` للمجهول ⇒ اختفاء السطح بلا رسالة.
6. **اللوحة تنادي الدالّة ولا تكتب في الجداول.** تطبيق MaxManager لا يقرأ إلا العرض الموقّع
   `config_signed`؛ فالكتابة المباشرة في `app_config.value` كانت «تنجح» ويبقى كل مستخدم على إعداد
   قديم — عطبٌ صامت أُغلق بجعل `config.publish` الطريق الوحيد، **ورابط التنزيل** معه (يكتبه المالك
   بيده، ويُرفض الفارغ لأن غيابه يُفقد زرّ التنزيل عند الجميع).

### ما قِيس فعليًّا — يُشار إليه لا يُعاد

- **نواة الترخيص:** `tools/test_license_jvm.py` ⇒ `OK (62 tests)` على ٢١ ملفًّا (مفاتيح بمتجهات
  مرجعية من سكربت Python مستقلّ · شهادات بتوقيعات حقيقية · خادم HTTP حقيقيّ على 127.0.0.1).
- **نواة الإعداد البعيد:** `tools/test_remote_jvm.py` ⇒ `OK (64 tests)` — منها اختبارٌ جديد يثبّت
  أن **التاريخ وحده** (`2026-10-14`) يُقرأ منتصف ليل UTC: كان الخادم (`Date.parse`) يقبله والتطبيق
  (`Instant.parse`) يرفضه، فيُحسب الإيقاف في طرف ويُهمَل في الطرف الآخر على نفس البايتات.
- **البوّابات الخمس:** `kt_balance` (2894·0) · `code_health` (exit 0) · `i18n_coverage --assert` (عوائق 0)
  · `--prune all` (يتامى 0) · `resource_compile` بـ`aapt2` الحقيقيّ ✓.
- **أداة المفتاح:** `tools/license_tool.py --self-test` ⇒ ستّ خطوات تفصل الصحيح من الخطأ
  (زوج صحيح ✓ · زوجان مختلفان ✗ · base64 مكسور ✗ · منحنى P-384 ✗ · نقطة كلها `0x03` ✓ ·
  أربعة أزواج عشوائية ✓)، وفي خمس تشغيلات متتالية. **وقد أمسكت عطبًا في نفسها:** قراءة SPKI كانت
  تبحث عن رأس BIT STRING بـ`rfind` عن البايت `0x03`، وهو يظهر داخل إحداثيات النقطة (احتمال
  ‏~٢٢٪ لكل مفتاح) فترفض مفاتيح سليمة؛ فصارت القراءة بنيوية (‏SEQUENCE ← AlgorithmIdentifier
  ← BIT STRING) والمواضع محسوبة، والحالة ⑤ تثبّت العطب ميتًا حتميًّا.
- **الترجمة الحقيقية:** `:app:compileReleaseKotlin` ⇒ **BUILD SUCCESSFUL ‏(16m58s)** بعد أن كشف المُصرّف
  **استيرادًا ناقصًا واحدًا** (`nd.max.core.license.LicenseClient` في `RemoteAccess.kt`) كان يُسقط
  الـ`when` والنداءات — ولم تكن أي حزمة JVM لتراه (الملفّ يعتمد أندرويد فلا يُصرَّف فيها). فالمُصرّف
  هنا ليس رفاهية: هو **القياس الوحيد** الذي يجيب «هل الأنواع سليمة في المسار الكامل».
- **الحدود:** اللوحة تُترجم محليًّا (`:app:compileDebugKotlin`) لكن اختباراتها لا تُشغَّل هنا
  (‏`aapt2` x86-64 على مضيف aarch64)، وسلوك الترخيص على جهاز حقيقي وجولة على خادم حيّ **لم تُقَس**
  (تحتاج جهازًا ونشرًا)، وربط المحرّكات (‏`maxai` يعمل عند `subscribe`) **لم يُنجَز** ويُقال.

---

## ADR-73 — **الصور تُرسم من لغة التطبيق ولا تُولَّد: عقدُ الصور المصغّرة، ووسمُ الصانع مصدرًا واحدًا** (REVIEW-NOTES-01)

**السياق:** ملاحظة التقييم الأولى كانت «استخدام الذكاء الاصطناعي مفرط»، وأوضح المالك أن المقصود
**مظهر المشروع وصوره**؛ والثالثة: «الصور المصغّرة: اعتمد تصميمًا خاصًّا بالتطبيق بدلًا من أدوات توليد
الصور». وقيدُ المالك القائم (`§0.4`) يمنع نقل الشكل والتقليد، والتطبيق مغلق المصدر. فالسؤال: كيف
تُبنى صورة متجر **يشبه صاحبها** بلا مولّد صور وبلا نقلٍ من أحد.

### القرارات

1. **لا يدخل المستودع ملفّ من مولّد صور.** كل صورة في `docs/store/` تُبنى من أربعة مصادر يملكها
   المشروع: لقطة شاشة حقيقية من `docs/screenshots/` (بإطار جهازها)، ولوحة `DARK` في
   `tools/gen_readme_assets.py` (وهي مقيسة بدورها على `MaxTokens.kt`)، وهندسة الوسم في
   `tools/makers_mark.py`، وخطوط التطبيق نفسها. ولا يُرسم شيء من ذاكرة أحد.
2. **الوسم هندسةٌ واحدة بثلاثة عارضين** — الصفحة (`docs/assets/makers-mark-*.svg`) والتطبيق
   (`MaxSignatureMark.kt`) والصورة المصغّرة (‏PIL) — و`tools/makers_mark.py --assert` يقيس أن
   الثلاثة لم تنزح. **وهذا هو الجواب المعماري للسؤال:** اللمسة الشخصية ليست صورة تُنقل، بل خطٌّ
   أرقامه في المستودع ويُقاس كل عارض عليها.
3. **الادّعاء يُنشر ولا يُخترع.** مضمون كل شريحة في البطاقة (‏«بلا قياس عن بُعد · بلا حسابات · بلا
   رفع صامت») يجب أن يكون منشورًا في `README.md`/`README.ar.md` — تقيسه البوّابة بالبحث في الصفحتين.
   فالمتجر لا يقول إلا ما يقول المشروع في العلن، وأي تحرير في الصفحتين يسقط معه التشغيل حتى يُضبط.
4. **الأرقام تُقاس وتُستبدَل، لا تُكتب.** `{screens}` و`{locales}` في نصّ المتجر يُقاسان من الشجرة
   (‏٦٢ من `MaxDestinations.kt` · ٨٤ من `values-*`) ويُستبدَلان عند التوليد — فادّعاء «٦٢ شاشة» لا
   يتقادم بصمت.
5. **البوّابة بلا Pillow، والحتمية ببصمة.** `--assert` (stdlib) يقيس بصمات البايتات مقابل
   `manifest.json`، ولوحة المستودع مقابل المعلَن، وبصمة هندسة الوسم، وأن `listing.md` حرفيًّا ما
   يولّده العقد، وأن بصمة `spec.json` لم تتغيّر — فيعمل في CI بلا مكتبة صور. و`--reproduce` يقيس
   أن تشغيلين من مدخلات واحدة يعطيان البايتات نفسها، فيُراجع المراجع الصورة ببصمتها لا بعينه.
6. **الخطوط لا تُشحن في المستودع، وغيابها يوقف التوليد بصوت عالٍ.** التطبيق ينزّل خطوطه على
   الجهاز (`Fonts.kt`)، فتُثبَّت هنا **خارجه** على عُرف `tessdata`/`EasyOCR`. والعربية بـ`Noto`
   (Kufi للعنوان · Sans Arabic للمتن) — وهو ما يسقط إليه النصّ العربي في أندرويد، فلا يُسمّى
   تبديلًا باسم اختيار.
7. **محرف بلا شكل يمنع حفظ الصورة.** وُلد هذا الشرط من عيبٍ وقع فعلًا في أول توليد: شرائح العربية
   رُسمت مربّعات فارغة (الخطّ الأحاديّ بلا محارف عربية) والنقطة النهائية مربّعًا (Kufi بلا `.`).
   فصار المولّد يقيس كل محرف مقابل محرف غائب حتمًا (‏U+E000) ويرفض الحفظ — لأن عيبًا يُرى بالعين
   مرّة لا يُترك ليُرى مرّة أخرى.
8. **وحدٌّ مُعلَن لا مسكوت عنه:** البطاقات العربية تحمل لقطات بواجهة إنجليزية، لأن لقطات RTL العربية
   **غير موجودة بعد** في `docs/screenshots/` — وعقد الأسماء يقولها بنفسه. فالعنوان والشرائح والتخطيط
   عربية ومن اليمين إلى اليسار، واللقطة وحدها ليست كذلك؛ وحين تُلتقط تُشار إليها في `spec.json`
   ويُعاد التوليد.

### ما قِيس فعليًّا — يُشار إليه لا يُعاد

- **البوّابة:** `tools/store_thumbnails.py --assert` ⇒ ٨ صور · عوائق ٠. و`--self-test` ⇒ **١١/١١**
  (عشر حالات مكسورة معلومة النتيجة، كلٌّ باسمها، **وحالة سليمة تمرّ** — أداة تفشل دائمًا لا تفرّق
  بين عطب وسلامة).
- **الحتمية:** `--reproduce` ⇒ «إعادة التوليد تعطى البايتات نفسها في ٨ صورة ✓».
- **عيبان أمسكتهما العين قبل البوّابة، وأُصلحا وأُعيد النظر:** إطارٌ مزدوج (اللقطة تحمل إطار جهازها،
  فإطارٌ من عندنا فوقه يفضح التركيب) — وحُذف؛ ومربّعات فارغة — وأُغلق بابها بقرار (٧).
- **الترجمة:** `:app:compileReleaseKotlin` ⇒ **BUILD SUCCESSFUL (26m26s)** — وهو القياس الوحيد
  المتاح لتغييرات Compose (لا حزمة JVM تصرّفها).
- **الحدّ:** جمال الصورة **لا يقيسه شيء** في هذا المستودع، ويُقال في `docs/store/README.md` صراحةً:
  «التركيب مقصود ليُنظر إليه — افتح بطاقة». ولم يُنشر شيء إلى متجر، ولا يوجد متجر أُنشئ بعد.

---

## ADR-74 — **«الخطّ الواحد» يصير يدَ التطبيق: كل حركة لها غرض مكتوب، ولا زخرفة** (REVIEW-NOTES-01)

**السياق:** بعد الوسم وبطاقة «من الصانع» كتب المالك: «أضف تفاصيل مميزة في التفاعلات والرسوم والحالات
الحيّة … اجعله يشعر بأن كل حركة ومعلومة واستجابة لها غرض واضح، وأن MaxManager له أسلوبه الخاص».
والخطر في طلب كهذا معكوسٌ عمّا يبدو: أن يُملأ التطبيق بحركات لا معنى لها، فيصير «مظهرًا» بدل أن
يصير «أسلوبًا» — وهو العطب نفسه الذي جاءت الملاحظة الأولى تشتكي منه.

### القرارات

1. **قلم الوسم يصير لغة الرسم.** `ui/design/MaxInk.kt` يرسم بخطّ واحد متّصل، أرقامه بوحدات إطار
   مرجعيّ (viewBox) لا بكسل، وقلمه بوحدات الإطار فيتناسب مع أي حجم، والكشف يتقدّم على الخطوط
   **بالتسلسل** (خطٌّ كامل ثم الذي يليه) كما تكتب اليد — لا خطوطًا متوازية تنمو معًا.
2. **ثلاث رسمات، ثلاثة أغراض — ولا رابعة:**

   | الرسمة | موضعها | غرضها |
   | --- | --- | --- |
   | `MaxInkGlyph.NotFound` | الحالة الفارغة (٣ مواضع) | «بحثتُ فلم أجد» — العدسة تُكتب ثم تُشطب، والرسمة **هي** الحالة |
   | `MaxInkTick` | حبّة الثقة الحيّة (٦٥ موضع قراءة) | «وصلت عيّنة» — تُرسم **فقط** حين تتغيّر القراءة، فالحركة إشارةُ حداثةٍ لا مؤقّت |
   | `MaxInkLoop` | تفعيل الترخيص · التحميل المدمج | «هناك عمل يجري الآن» — عمرها عمر العمل، وتموت بموته |

3. **ولا زخرفة بلا غرض.** رُفض في التصميم: فاصلٌ مزيّن (`MaxInkDivider`) لأنه لا يحمل معلومة، ونقرٌ
   يعيد رسم الوسم لأنه تفاعل مزيّف. ولا حركة خاملة: **لا شيء يتحرّك حين لا يحدث شيء**.
4. **حالات الخطأ بقيت جادّة.** الحالة الفارغة وحدها أخذت رسمًا (‏`SearchOff` ← عدسة الحبر، المعنى
   نفسه)، وأيقونات الخطأ والانتظار غير المدمج بقيت كما هي: موضع العطب ليس موضع لمسة شخصية.
5. **من أوقف حركات النظام يرى الرسمة كاملة فورًا** — «بلا حركة» ليست «حركة أسرع». كل رسمة تمرّ
   بـ`rememberAnimationsEnabled`، وموجة الانتظار تقف ساكنة والنصّ المجاور («جارٍ التفعيل…») يحمل
   المعنى وحده.
6. **لا لغة تصميم ثانية:** الرسمة تُرسم بلون `colorScheme.primary` داخل بلاطة الحالة القائمة
   (٥٦dp، بالحجم نفسه)، والحبّة تُرسم بلون نصّها. وعقد الشريحة القائم («أيقونة أو نقطة **مع** نصّ،
   لا لون وحده») لم يُكسر: النقطة صارت علامة صحّ — ما زال شكلًا **مع** النصّ.
7. **حدّ مُعلَن:** رسوم هذا الملفّ رسوم تطبيق داخلية، **ولا توأم SVG لها** في `docs/assets/` — فلا
   تشملها بوابة `makers_mark` (التي تحرس هندسة **الوسم** وحدها). ولا تُستعمل في الصور المصغّرة،
   فتُترك الهندسة مصدرًا واحدًا لكل عارض ولا تُنسخ أرقامها إلى Python.

### ما قِيس فعليًّا — يُشار إليه لا يُعاد

- **البوّابات:** `kt_balance --assert` (٢٨٩٦ ملفًّا · عوائق ٠) · `code_health --assert` (صحّة نظيفة)
  · `design_tokens --assert` (المجموع ٢٧١ من سقف ٣٠٠ — ولم يُضف حرفيّ جديد: استُعملت
  `MaxSpace.sm` و`MaxSize.iconGlyph*` و`MaxSize.iconContainer`).
- **الترجمة:** `:app:compileReleaseKotlin` ⇒ **BUILD SUCCESSFUL (26m26s)** — وفيه تُصرَّف الرسمات
  الثلاث وربطها (الحالة الفارغة · الحبّة · الموجة في شاشة الترخيص والتحميل المدمج).
- **والهندسة رُسمت ونُظر إليها قبل اعتمادها** (معاينة PIL للعدسة والعلامة والموجة)، ولم تُقبل رسمة
  لم تُرَ. وحدّها المُعلَن: **سلوك اللمس وRTL والأداء على جهاز حقيقي يحتاج جهازًا** — لم يُقَس هنا.

## ADR-75 — **«العناية بالتخزين» تحلّ محلّ «التنظيف الفائق»: شاشة شاملة واحدة، والاسم القديم لا يبقى** (STORAGE-CARE-01)

**الأمر:** «احذف شاشة ultra clean وابنِ شاشة شاملة (تنظيف + تحليل تخزين + إدارة تطبيقات) بمرجعية
أفكار — دون نسخ كود». ومعها أمرٌ لاحق في الجولة نفسها: **المراجع لا تُسمّى** (ADR-77).

**السياق:** «التنظيف الفائق» أجاب سؤالًا واحدًا (ما الذي أحرّره الآن؟) وأهمل سؤالين يقفان معه: أين
ذهبت المساحة، وكيف أدير ما بقي. والدمج في شاشة واحدة ليس تجميع أقسام بل **توحيد مصدر قياس**: رقمان
لسعة واحدة يفترقان حتمًا.

### القرارات

1. **إعادة تسمية كاملة بلا بقايا.** العائلة `UltraClean*` ⇒ `StorageCare*` (‏Model · Engine ·
   ViewModel · Screen)، والوجهة `ultra_cleaner` ⇒ `storage_care`، والمفاتيح `ultra_cleaner_*` ⇒
   `storage_care_*`، والدور `max_role_ultra_cleaner` ⇒ `max_role_storage_care`، والعنوان
   «Storage Care»/«العناية بالتخزين». **وبقيت أسماء القياس كما هي** (`CleanMeasurement` ·
   `CleanMeasurementSet` · `CleanTotal` · `CleanOutcome`) لأنها تصف قياسًا لا شاشة، و`StorageDetail`
   كما هي، و`HomeDeckEntry.key = "clean"` لم يُغيَّر (ثابت تخزين لا يُقرأ منه نصّ).
2. **ستة أقسام بهذا الترتيب:** السعة (من `StorageUtil.readMounts` نفسه — لا حساب ثانٍ) · النطاق (أي
   طبقة امتياز، وما يوسّعه الجذر بالاسم) · التنظيف الذكي (صفّ لكل فئة مرئية، والزرّ يحمل مجموع
   المختار) · تحليل المساحة (مسح مضغوط وباب `StorageDetail`) · أكبر التطبيقات (بصلاحية Usage Access
   أرقام كاملة، وبدونها حجم الحزمة وحده — والدرجة معلنة) · أدوات (`FileManager` · `Dex2oat` ·
   `DebloatFreeze` — شاشة تجمع ما تفرّق).
3. **فئة جديدة `CorpseData` — ومُشّرّة افتراضيًّا.** بقايا التطبيقات المُزالة: مجلدات تحت
   `Android/data` و`Android/obb` أسماؤها حزم لم تعد مثبّتة. وهي **ليست ثالثة** الفئتين المحفوظتين
   (`InstallerFiles` · `EmptyFolders` — مطفأتان لأن كلًّا منهما قد يكون ملك المستخدم): التطبيق أُزيل
   فعلًا وبقيت بياناته، فالتأشير عليها **افتراض** لا تجاوز.
4. **وثلاثة قيود على كشف البقايا:** المثبَّت يُقرأ من `pm list packages` بالجذر — لا من
   `getInstalledPackages` الذي يحجبه `<queries>` في البيان فيرى كل حزمة «مُزالة»؛ **ولا يُحذف مجلد
   بلا نقطة في اسمه** (اسم حزمة أندرويد يحمل نقطة، وما لا نقطة له بيانات مشروعة لا حزمة)؛ و
   `Android/media` **مستثنى قصدًا** (وسائط المستخدم لا كاش تطبيق).
5. **وفئات الجذر الأربع تُخفى عند غياب الجذر** (`visibleAt`: `AppCaches` · `SharedCaches` ·
   `CorpseData` · `SystemLogs`) — مساراتها خارج ما يقرؤه uid التطبيق أصلًا، فعرضها كصفوف معطّلة
   **دعوة دائمة لتحذير لا يمتلئ أبدًا**. وتبقى الثلاث بلا جذر (`Thumbnails` · `InstallerFiles` ·
   `EmptyFolders`)، وسطر النطاق يقول ما يوسّعه الجذر بالاسم.
6. **`null` ليست صفرًا** (عقد سابق مُثبَّت): فئة فشل قياسها لا تدخل المجموع ويُعلَن النقص
   (`CleanTotal.hasUnmeasured`)، وفئة مطفأة لا تدخل المجموع ولو قيست — الزرّ يقول ما سيُحذف لا ما هو
   موجود.
7. **المستبعد المُعلن:** Deduplicator · Scheduler · MediaSqueeze — جولة لاحقة، **لا نصف تنفيذ**.
8. **والحرس ثلاثة:** `MergedDestinationCleanupTest` يمنع عودة `UltraClean`/`ultra_cleaner` إلى أي
   ملفّ Kotlin (بتجريد التعليقات — توثيق «كان كذا» مشروع) · `StorageCareModelTest` يحرس عقود القرار
   · و`ControlLayoutModelTest` يحرس أبوّة `Control` للوجهة الجديدة.
9. **والتنقية اللغوية جزء من القرار:** ‏**٢٧٧٢ مفتاحًا يتيمًا** أُزيلت بـ`i18n_coverage.py --prune
   all` — **٣٣ مفتاحًا في كل لغة من ٨٤ لغة بالضبط** (`ultra_cleaner_*` ×٣٠ · `home_cleaner_*` ×٢ ·
   `max_role_ultra_cleaner`) — ولو بقيت لأسقط lint الإصدار (`ExtraTranslation` خطأ قاتل). والعدد
   يُقاس من الـcommit نفسه لا من الذاكرة: `git show` على ملفات اللغات أثبت min=max=33.

### ما قِيس فعليًّا — يُشار إليه لا يُعاد

- **البوّابات:** `kt_balance --assert` **٢٩٠٦ ملفًا · ٠ عوائق** · `code_health --assert` صحّة نظيفة ·
  `i18n_coverage --assert` ‏٠ عوائق و`--prune all --assert` ‏**٠ يتيم** · `resource_compile --assert`
  ✓ (aapt2 الحقيقي) · `readme_assets --assert` أصول سليمة · `license_audit --assert` ‏٠ GPL.
- **الترجمة:** `:app:compileReleaseKotlin` ⇒ **BUILD SUCCESSFUL (22m17s)** — بعد عطب أمسكه
  المُصرّف وأُصلح بقياس (تفصيله في ADR-76).
- **وحدّها المُعلن:** سلوك الحذف على جهاز حقيقي (بقايا فعلية · `pm list packages` على روم مخصّص ·
  Usage Access) **يحتاج جهازًا** — لم يُقَس هنا.

## ADR-76 — **الجزيرة الديناميكية: كبسولة حيّة واحدة — لا تلمس، ولا تسرق قارئ اللوحة** (DYNAMIC-ISLAND-01)

**الأمر:** «شاشة Dynamic Island أفضل مليار مرة» — بمرجعية أفكار خارجية (والمراجع لا تُسمّى: ADR-77).

### القرارات

1. **القرار في نموذج نقيّ (`IslandModel`) لا في الرسم.** الأولوية مرتّبة: **إشعار عابر > وسائط >
   شحن > مؤشرات > مخفي**، والمؤقّت بالأهمية: `importance ≥ 4` ⇒ ٨ ثوانٍ · `== 3` ⇒ ٥ · دونها ⇒ ٣ —
   فالحدث يبقى بقدر ما يستحقّ. والمنتهي **لا يحجب ما بعده** (وسائط تعمل الآن أهمّ من إشعار نُسي)،
   ومؤشرات بلا قيمة **ليست محتوى** (كبسولة بلا رقم تُقرأ عطبًا لا هدوءًا).
2. **`IslandLive` بعدّاد مراجع — مالكان لا مالك واحد.** المعاينة في الشاشة والخدمة فوق التطبيقات
   تشتركان في الإشارات نفسها، فالمالك الأوّل يسجّل والمالك الأخير ينظّف؛ والنطاق داخلي
   (`SupervisorJob + Main.immediate`) **لا يُستعار** من مستدعٍ.
3. **أربعة مصادر، وكلّها مقروءة لا مكتوبة:** البطارية (`ACTION_BATTERY_CHANGED`) · الوسائط
   (`MediaSessionManager.getActiveSessions` بمستمعنا — والفعّالة وحدها: `playbackState.isActive`
   وعنوان غير فارغ) · الإشعار العابر من `MaxNotificationListener` جديد (مرشّحات المصدر: حزمة
   التطبيق · `isOngoing` · ملخّص المجموعة · وسائط · بلا عنوان) · المؤشرات من `HudLive` المشترك.
4. **والمؤشرات لا تسرق قارئ اللوحة الواحد:** `HUD_OWNER_ISLAND` — تُبدأ **فقط إن كان
   `HudSampler.currentOwner == null`**، وتُوقف عند مغادرة الملك، وتُخفى إن كان القارئ بلا مالك (لا
   أرقام قديمة كاذبة).
5. **والكبسولة لا تستقبل اللمس** — `touchable = false` ⇒ `FLAG_NOT_TOUCHABLE` في `OverlayWindow`
   (معامل جديد): لا تفعل شيئًا عند النقر، فتمرّ اللمسة إلى ما تحتها **ولا تُسقط سحب شريط الإشعارات
   من أعلى الشاشة**. والموضع عبر معامل `windowGravity` جديد (`TOP|CENTER_HORIZONTAL` أو تحت شريط
   الحالة بارتفاع مقيس) و`setGravity` لا يمسّ المحور الأفقي.
6. **والشكل من لغتنا:** المقاس Compact 0.85 / Normal 1 / Large 1.15 عبر override `LocalDensity` على
   الرسم وحده · واللون من `AccentSwatches` **المشترك** (استُخرج من شاشة لوحة الأداء إلى
   `ui/component/` فلا نسختان لألوان واحدة) · والتغيير يُنشر عبر `IslandLive.config` فيُطبَّق بلا
   إعادة تشغيل الخدمة.
7. **البيان:** خدمة تراكب بنفس عقد أخواتها (`specialUse` + إشعار أمامي) ومستمع إشعارات بصلاحية
   `BIND_NOTIFICATION_LISTENER_SERVICE` — **بلا منح صريح من المستخدم لا يستلم المستمع شيئًا**،
   والشاشة تسمّي المنح ووجهته. ونصوص en+ar فقط.
8. **والوجهة تحت `Control`** (`ControlToolDestinations` — عقد `ControlLayoutModelTest` حرفيًّا).

### عطب أمسكه المُصرّف — يُسجَّل لأنه الدرس

التشغيل الأول لـ`:app:compileReleaseKotlin` فشل بثلاثة أخطاء في `MaxNotificationListener`:
`RankingMap` **لا يملك** إلا النسخة بمعلَّمَي الإخراج `getRanking(String, Ranking)` — والنسخة بمعلّم
واحد التي بُنيت عليها الكتابة **غير موجودة في `android.jar` أصلًا**. والقرار كان بالقياس: `javap` على
`android-36` و`android-37.0` أثبت التوقيع، فصار الاستدعاء `Ranking()` تُملأ و`== true` تعني «وجد
ترتيبًا». ⇒ هذا هو نصّ §0.1 (ب): «مرجع غير محلول ⇒ المُصرّف هو القياس الوحيد المتاح». وأُعيد
التشغيل بعد الإصلاح ⇒ **BUILD SUCCESSFUL 22m17s**.

### ما قِيس فعليًّا

- `IslandModelTest`: ٧ عقود خالصة (الأولوية · انتهاء TTL · حدّ الانتهاء صارم · الشحن يسبق المؤشرات ·
  مؤشرات بلا قيمة ليست محتوى · المخفي بلا مؤقّت) — تُقاس في CI (JUnit لا يعمل محليًّا: aapt2 x86-64
  على مضيف aarch64).
- البوّابات الخفيفة كلها خضراء (كما في ADR-75)، والترجمة: `:app:compileReleaseKotlin` ⇒
  **BUILD SUCCESSFUL (22m17s)**.
- **وحدّها المُعلن:** اللمس الحقيقي والوسائط والإشعارات على جهاز **يحتاج جهازًا**؛ والشاشة والخدمة
  والمعاينة لم تُرَ على هاتف بعد.

## ADR-77 — **الأفكار والمراجع لا تُذكر؛ الاعتماد بكودٍ منقولٍ أو أمرٍ من المالك** (2026-10-16)

**الأمر:** «ضيف قاعدة أن الأفكار أو المراجع لا تُذكر أبدًا إلا فقط من تم أخذ منه أكواد فعليًّا أو
طلبت أنا بنفسي هذا».

**التناقض المُعلَن:** ADR-54 قرّر أن الفضل يُعلن في `Credits` **حتى بلا كود** («إعلانًا للفضل لا نقلًا
للإشعار»)، وADR-55 §2 عضّده («الفضل يُعلن حتى حين تُؤخذ الفكرة بلا كود إذا كان المشروع فريدًا»)،
وكان نصّ `AGENTS.md` §0.4 يقول مثل ذلك. **وهذا الأمر يخالفها** — والمالك صاحب القرار، فالتعديل يُعلَن
ولا يُسكَت عنه: يبقى ADR-54/55 ساريين في كل شيء **إلا هذا الشقّ**.

### القرار — ثلاث حالات لا رابعة

1. **كود فعلي منقول** (من ترخيص متسامح: `MIT` · `Apache-2.0` · `BSD-3-Clause`) ⇒ الإشعار في
   `THIRD_PARTY_NOTICES.md` (المرجع الملزم) والفضل في `README` ← `Credits` — كما هو.
2. **أمر المالك بذكرٍ بعينه** ⇒ يُذكر كما أمر.
3. **فكرة أو منهج أو مرجع قُرئ** ⇒ يُقرأ، **وتُفتح رخصته كاملةً قبل الاقتراب منه** (ADR-55 §3 —
   القياس لا يُمحى)، ثم **لا يُسمّى ولا يُعلن**: لا في `Credits` ولا `THIRD_PARTY_NOTICES` ولا في
   تعليقات الكود. والمفروض (`GPL`/`AGPL`…) يبقى صفر كود كما كان — ولم يغيّر هذا الأمر شيئًا فيه.

### والأثر المقيس على الجولة نفسها

- كانت قد أُضيفت **صفوف مراجعِ أفكارٍ** إلى `Credits` في الـREADMEين، وقسم تصنيف في
  `THIRD_PARTY_NOTICES.md`، وسطر في `StorageCareModel` — **وأُزيلت كلها** بموجب هذا الأمر، وثُبّتت
  القاعدة في `AGENTS.md` §0.4 (القاعدة ٣ نصًّا ناسخًا).
- **وحدّه:** يُقاس بمراجعة، لا بأداة (لا بوابة جديدة — قرار المالك في ADR-54 قائم). ومن أضاف لاحقًا
  اسمًا بلا نقل كود يخالف النصّ صراحةً، لا بالتأويل.

## ADR-78 — **لا تُبنى بوابة على اسم نموذج غير مُتحقَّق: «Luna» أُزيلت من الفريق كله** (2026-10-16)

**الأمر (المالك):** «قم بإزالة Luna فهي شيء لا وجود له».

**القياس الذي يؤكّده:** الكتالوج الحيّ (`tools.opencode.models`) يعطي **صفر نتيجة** لكلمة `Luna`؛
وسبق أن سُجّل في `.planning/STATE.md` أن «الأسماء التاريخية المُهيَّأة غير قابلة للاختيار في هذا
التشغيل». أي أن كل بوابة «حكم سلامة Luna» كانت معلّقة على اسم لا يمكن اختياره أصلًا — قفلٌ لا يُفتح.

**القرار:** أُزيل ذكر Luna من الوثائق الحيّة كلها — `AGENTS.md` (قواعد §2 وروستر §3 وبروتوكول §4)،
وملفات الأدوار الخمسة، و`REVIEW.md`، و`REVIEW-REQUEST.md`، و`KNOWN_ISSUES.md`، و`REBUILD-BASELINE.md`،
و`PER-APP-SPOOF-PLAN.md`، وسجلات `HANDOFF`/`NEXT_TASK` — وحلّت محلها صيغة الدور: **«مراجعة مستقلة»
يختار لها المالك نموذجًا غير المنفّذ من المتاح.** وما تبقّى من ذكرها في السجل هو توثيق الإزالة نفسها
لا أكثر.

**الأثر:** بوابات «حكم Luna» صارت «مراجعة مستقلة معلّقة» — الحالة نفسها، بلا اسم لا وجود له. والمتطلَّب
لم يتغيّر: أي تغيير في `core/hardware`/`core/maxai` لا يُغلق إلا بمراجعة من نموذج بعائلة مختلفة عن
المنفّذ (§2 القاعدة ٤) — لكن لا يُعلَّق إغلاق على اسمٍ لا وجود له.

**ومُعلَنٌ معه (قِيسَ ولا يُدَّعى):** القياس نفسه أعطى **صفر نتيجة** لـ`Solar Pro 4`، وأن الاسمين
الحاليين في الكتالوج `Muse Spark 1.3` و`MiMo V2.6 Flash`؛ لم تُعدَّل صفوفهما في هذه الجولة — تُصحَّح
بأمر منفصل.

## ADR-78 — **وضع واحد: الملفّ العام هو وضع Max AI، والخادم منفّذ لا مالك** (2026-10-20)

**الأمر:** «لا أقصد حذف demon يا غبي بل حذف ارتباط البروفايل العام به ودمج البروفايل العام معا
max ai». ثم: «نفّذ». وسبقه قرار المالك: «فقط إذا تم تفعيل max ai يُطبَّق عند الإقلاع اختيار المستخدم
فيه وخلاص».

**العطب المقيس الذي وُلد منه القرار (حزمة 2026-10-16، Moto G86 · MT6878):** كان التطبيق يعرض
مبدّلين — «الملفّ العام» (1/2/3) ومبدّل «هدف» Max AI (`performance/balanced/battery`) — بينما
الخادم يفرض `Balanced` عند الإقلاع وعند كل تشغيل لـAI (`PROFILE_APPLY BALANCED` في 07:39:34
و07:53:52)، ويكتب `current_profile` بالوضع المؤقّت للعبة/البطارية. فصار `current_profile=2`
(متوازن) بينما `AIobjective=battery`، والعتاد نفسه انتهى إلى `policy0 2000000..2000000` و
`policy4 2500000..2500000` (تثبيت الأرضية عند السقف) بعد 58 تراجعًا مسجّلًا.

**القرار — نيّة واحدة، ومنفّذ واحد:**

1. **`API/max_ai_mode`** هو نيّة المستخدم الواحدة: `1` أداء · `2` متوازن · `3` بطارية. يكتبه
   التطبيق من مبدّل Max AI، ويقرؤه الخادم عند الإقلاع وعند تغييره.
2. **`API/current_profile` يبقى الوضع الفاعل** الذي يكتبه الخادم عند كل تطبيق (لعب/بطارية منخفضة/
   أساس). لا يُقرأ على أنه نيّة المستخدم، ولا تُكتب به النيّة.
3. **عند الإقلاع:** إن كان `AIenabled=1` يُطبَّق `max_ai_mode`؛ وإن كان `0` فلا يُطبَّق ملفّ أساس
   (يبقى `run_profiler(PERFCOMMON)` للتهيئة فقط). ولا `Balanced` مفروض.
4. **عند تبديل AI:** لا يُفرض `Balanced`. عند التشغيل يُقرأ `max_ai_mode` ويُطبَّق؛ وعند الإطفاء
   يبقى الفاعل ثم يعيد التطبيقُ نيّةَه عبر `standDownToControlPlane`.
5. **اللعب والبطارية المنخفضة تبديلات مؤقّتة**، تعود إلى `base_mode` لا إلى `Balanced`. وأُضيف
   `apply_base_profile()` في `SystemProfiles.c` ليكون مرجع العودة الوحيد.
6. **مبدّل Max AI هو الملفّ العام:** `setObjectivePreference()` يكتب `max_ai_mode` ويضبط
   `AIobjective` **ويطبّق الملفّ** (`--from-ai` حين AI مفعّل). والاختيار اليدوي عبر CLI يكتب
   `max_ai_mode` أيضًا، فلا يبقى ملفّ عام خارج العقل.
7. **الترحيل:** عند غياب `max_ai_mode` تُؤخذ النيّة من `AIobjective` القديم (لا من
   `current_profile`، لأنه قد يكون وضع لعبة)، ويُكتب الملفّ مرة واحدة. و`AIobjective` يبقى مرآة
   للتوافق والإشعارات، لا خيارًا ثانيًا.
8. **وضع الأداء صار أساسًا بلا لعبة**، فحُصّنت مسارات اللعبة في `apply_performance_profile`
   بـ`gamestart != NULL` (DND، التحميل المسبق، اسم الإشعار)، و`run_profiler(1)` يكتب `NULL`
   بدل تمرير مؤشّر فارغ إلى `%s`، ومنع `opts` الباقية من آخر لعبة أن تُقرأ كتجاوز صريح
   (وإلا تُخطّي `run_profiler(PERFORMANCE)` في وضع الأساس).

**الملفّات:** `manager/.../maxai/MaxAiMode.kt` (جديد) · `MaxAiEngine.kt` · `MaxAiModels.kt` ·
`MaxAiScreen.kt` · `MaxManagerPaths.kt` · `AppMonitor.kt` (أرضية per-app) · `System.c` ·
`SystemProfiles.c` · `InotifyWatcher.c` · `CLIUtility.c` · `MaxAiMode.c` (جديد) · `MaxManager.h` ·
`DaemonContext.c` · اختبارات `MaxAiModeTest.kt` و`parity_test.c` و`cli_profile.c` · `docs/max-ai.md`.

**الحدود المُعلنة:** هذا يثبت القرار في الشيفرة والاختبارات المضيفة، ولا يثبت سلوكًا على هاتف:
الإقلاع الحقيقي، وكتابة sysfs، وترتيب أحداث inotify، وواجهة RTL تحتاج جهازًا. والبناء الكامل
(APK) لا يُشغَّل كعادة (§0.1)؛ ما يُقاس هنا بوابات البنية والاختبارات المستهدفة. وقِيس في هذه
الجولة `:app:compileReleaseKotlin` و`:app:testReleaseUnitTest` لثلاث فئات: **٢٩ اختبارًا · ٠ فشل**
(`MaxAiModeTest` ٤ · `ControlRegistryTest` ١٥ · `CpuAvailableFrequencySnapTest` ١٠)، و**٣١٨/٣١٨**
في harness المضيف (مع تجاوز مجموعة كتابة واحدة على مضيف يُعلن `policyN` حيّة).

## ADR-79 — **سطحان يُخفيان بعلامةٍ واحدة في السجلّ: «إحصاءات المجتمع» و«إضافات الطرف الثالث»** (2026-10-21)

**الأمر:** «قم بإخفاء إحصاءات المجتمع — *ما يُشارَك، والرسالة نفسها، ومفتاح واحد* — [و]إضافات الطرف
الثالث — *العقد المنشور: ما يجوز للإضافة أن تعلنه، وما قُبل أو رُفض على هذا الجهاز*.» ثم اختار المالك
في التوضيح صراحةً: **«من الإعدادات وفهرس الشاشات معًا»**، و**«القناة تبقى كما هي»**.

**لماذا قرار لا سطر حذف:** الصفّان كانا مقصودين ومُعلَّلين في مكانهما — `CommunityStats` مكتوب عليها
«تبقى ظاهرةً دائمًا كالترخيص» (`MAX-CONTROL-01` §١٠.٣)، و`Plugins` وُلدت من `GAP-14` لأن الشاشة
بُنيت بلا مدخل. فالإخفاء **يعكس** القرارين، ويجب أن يُقرأ في السجلّ لا أن يُستنتج من غياب صفّ.

**القرار:**

1. **حالةٌ واحدة في السجلّ:** `MaxDestination.hidden: Boolean = false`، ومعها
   `MaxDestination.Visible = All.filterNot { it.hidden }`. أي سطح يعرض «كل الشاشات» يقرأ `Visible`
   لا `All`، فلا يعود السطح المخفيّ من بابٍ نُسي.
2. **`CommunityStats` و`Plugins` بـ`hidden = true`:** لا صفّ في `SettingsScreen`، ولا صفّ في
   `screenFinderTargets`، ولا صفّ في `maxHubRows`.
3. **لا حذف:** الشاشتان والمساران والرسم والنصوص كما هي؛ الإرجاع **بضغطة واحدة** على العلامة.
4. **قناة الإحصاءات لم تُوقَف بهذا الإخفاء** (اختيار المالك): الإرسال يبقى مشروطًا بالموافقة
   المحفوظة، والافتراضيّ بلا موافقة = لا إرسال. فلا يفتح الإخفاء بابًا ولا يغلقه.
5. **الحارس ينقلب ولا يُسقَط:** `SettingsDestinationReachabilityTest` يشترط للمرئيّ مدخلًا،
   ويشترط للمخفيّ **ألّا** يكون له مدخل، والمجموعة المخفيّة مكتوبة فيه صراحةً (`CommunityStats`,
   `Plugins`) فلا يُخفي أحدٌ سطحًا بالصمت.

**الملفّات:** `ui/navigation/MaxDestinations.kt` · `ui/mainscreens/SettingsScreen.kt` ·
`ui/mainscreens/ScreenFinderModel.kt` · `ui/navigation/MaxDestinationCatalog.kt` ·
`test/.../SettingsDestinationReachabilityTest.kt` · `test/.../ScreenFinderTest.kt`.

**الحدود المُعلنة:** الإخفاء بنيويّ (شيفرة ونصوص)، لا يُثبت أن الصفّين لا يظهران على جهاز. ولم أبنِ
محليًّا في هذه الجولة (§0.1: تعديل واجهة/سجلّ) — الحكم للمُصرّف في CI على الالتزام الذي يحمله.

## ADR-80 — **التبويب المختار يستقرّ في منتصف الشريط، لا عند حافّته** (2026-10-21)

**الأمر:** «أريد أن يكون التمرير بين التبويبات كالفيديو الأول — سواء في Device Info أو Per App أو أي
شيء سيكون به تبويب — حيث عندما تصل إلى المنتصف وتكمل التقليب يكون ثابتًا في المنتصف، مثل جميع
التطبيقات التي بها تبويب احترافي.» والفيديو المرجعي يُظهر التبويب النشط في **مركز** الشريط، وله جاران
ظاهران على الجانبين، بينما كان الشريط عندنا يكتفي بإظهاره عند الحافة.

**العطب المقيس في الفيديو الثاني (الوضع القديم):** عند الانتقال إلى «المستشعرات» يتوقّف الشريط وقد
التصق التبويب النشط بالحافة اليسرى (آخر الشريط في RTL)، وعند «نظرة عامة» يلتصق بالحافة اليمنى. والسبب
بنيوي: `MaxTabStrip` كان يستعمل `BringIntoViewRequester` — أي «أظهِره»، لا «وسّطه».

**القرار:**

1. **المركز هو الحالة الافتراضية:** عند كل تغيّر اختيار يُحسب مركز التبويب من الحدود المقيسة
   (`positionInParent`) ومن `ScrollState.viewportSize`، ويُسحب الشريط إلى
   `مركز المحتوى − منتصف نافذة العرض`، ثم يُثبَّت هناك.
2. **RTL بلا فرع في القياس:** المسافة تُقاس من **بداية التمرير** كما يقيسها `ScrollState` نفسه؛ في
   العربية بدايتها اليمنى. الحساب واحد والاتجاه يحدّد طرف المسافة فقط.
3. **الحدّ الطبيعي لا يُخفى:** أول تبويب وآخره لا يُجاوز بهما المحتوى، فيُثبَّتان عند الحافة (`coerceIn`).
   هذا سلوك الشريط المرجعي نفسه، وليس نقصًا.
4. **الحركة:** أول تثبيت بعد القياس **يقفز** (الشريط يفتح واختياره في وسطه، فلا حركة بلا سبب)، وتغيير
   الاختيار **ينزلق** بنابض بلا ارتداد (`tabCenterSpring`). وبلا حركة النظام يقفز دائمًا. والعدسة
   (`tabSelectionLens`) تبقى في إحداثيات المحتوى كما هي، فالاختيار يُفهم بالحركة واللون معًا.
5. **سطح واحد لكل الشاشات:** التغيير في `MaxTabStrip` المشترك، فيسري على معلومات الجهاز، وتفاصيل
   التطبيق (Per App)، وإلغاء التجميد، وسجلّات النظام، ومركز المحاكيات — بلا شريط ثانٍ ولا نمط مختلف.
6. **الحارس ينقلب مع السلوك:** `MaxTabStripContractTest` يرفض عودة `bringIntoView` ويشترط حساب المركز
   وفرع RTL وسحب الشريط إلى الهدف، فلا يرجع السلوك القديم صامتًا.

**الملفّات:** `ui/design/MaxTabStrip.kt` · `test/.../ui/design/MaxTabStripContractTest.kt`.

**الحدود المُعلنة:** التوسيط **بصريّ**؛ صحّته على جهاز (خصوصًا RTL واللمس) تحتاج جهازًا. والمقيس هنا
تصريف Kotlin وبوابات البنية، لا لقطة شاشة.

## ADR-81 — **«مُدار» في الإيقاع تعني «مُدرج في قائمة اللعب»، وجلسة العتاد تعني «حقل صريح» — والاسمان لا يتبادلان** (PERF-AUDIT-01-REVIEW-FIX · 2026-10-26)

**السبب:** مراجعة `PERF-AUDIT-01` كشفت أن `AppMonitor.managedAppOwnsForeground()` يطابق **اسم الحزمة في قائمة اللعب**، بينما التقرير يقول «إدخال إعدادات فعّال»؛ وأن قائمة المستخدم فيها 13 مدخلًا، 12 منها كلها `default`. فالسؤال ليس تجميليًّا: أيّهما يقود الإيقاع؟

**القرار:**

1. **«مُدار» = مُدرج في قائمة اللعب.** وجود الحزمة في `maxmanagerApplist.json` إعلانُ المستخدم أنها لعبة، حتى لو كانت كل حقولها `default`. ولهذا يبقى إيقاع المراقبة 500 م.ث أمامها، ويُبنى `background_apps` كل دورة لتحديث PIDs التي يقرأها الخادم.
2. **«جلسة عتاد per-app» = حقل صريح واحد على الأقل.** `PerAppSessionRules.hasExplicitOverride` وحدها تفتح التقاط خط الأساس/الاستعادة. الإدخال `default` لا يفتح جلسة ولا يلتقط ولا يتراجع.
3. **لا يُستبدل أحد التعريفين بالآخر** في تعليق أو تقرير أو جدول. الإيقاع يتبع (1)، والجلسة تتبع (2)؛ وخلطهما أنتج الادعاء الخاطئ في `PERF-AUDIT-01`.
4. **الأثر المعلن:** 12 من 13 إدخالًا في قائمة المستخدم `default` ⇒ الإيقاع يبقى 500 م.ث أمامها، وقائمة العمليات تُبنى كل دورة (7200/س) — وهذا **مقصود** لتحديث PIDs. أمام تطبيق غير مُدرج: 1000 م.ث، والقائمة تُبنى مرة عند تغيّر الحزمة.
5. **الحارس:** `CompanionPollPolicy.isManagedForeground()` دالّة نقيّة + اختباراتها، و`AppMonitor` يفوّض إليها؛ فأي تغيير صامت للدلالة يُسقط الاختبار.

**الملفّات:** `core/daemon/CompanionPollPolicy.kt` · `AppMonitor.kt` · `test/.../CompanionPollPolicyTest.kt` · `docs/ai/HANDOFF.md`.

**الحدود:** لا جهاز؛ الأثر على الحرارة/البطارية يبقى غير مقيس، والأرقام أعلاه عدّ نداءات لا قياس طاقة. والالتزام الأصلي لم يُعدَّل تاريخه؛ التصحيح في سجلّ جديد.

## ADR-82 — **`prediction.rs` يُبعَث على قياسٍ حقيقيّ، لا يُحذف ولا يُترَك ميتًا** (PERF-AUDIT-01-CLOSE · 2026-10-27)

**السبب:** `thermalcore/src/prediction.rs` كان **خارج شجرة الوحدات** (لا `mod prediction` في `lib.rs`)، فيقرؤه
`dead_modules` ملفًّا لا يُصرَّف أصلًا — ويقرأ **سبعة حقول لا وجود لها** في `learning::ThermalEvent`
(`temp_delta` · `core_load_max` · `is_charging` · `current_now_abs` · `hour_of_day` · `avg_gradient` …).
فلو وُصل لَما ترجم. وبقي في منطقة رمادية: `ADR-18` يمنع حذف عمل منجز لأسباب جمالية، وإصلاحه
قرار سلوك يحتاج جهازًا. والنتيجة أن الملفّ **لا يموت ولا يعمل** — أسوأ الحالتين.

**القرار:**

1. **يُعاد بناؤه على ما نقيسه فعلًا فقط:** `(at_secs, deci_c)` — الحرارة المفلترة والزمن الرتيب. ولا
   يُخترع حمل ولا شحن ولا ساعة، لأن ما لا يقيسه الخيط الحالي لا يصحّ أن يُدَّعى في نموذج.
2. **الرياضيات خطّ واحد بالمربعات الصغرى** على نافذة متحرّكة:
   `slope = Σ(tᵢ−t̄)(vᵢ−v̄) / Σ(tᵢ−t̄)²` ثم `v(t+h) = v_last + slope × h`. وهذا يحلّ محلّ ثمانية أوزان
   ثابتة تُحدَّث بمعدّل تعلّم بلا معايرة — أي بلا معنى عدديّ.
3. **ثلاثة حرّاس صريحة:** أقلّ من `PREDICTION_MIN_SAMPLES` ⇒ `None` لا صفر مخترع (`ADR-07`) · تباين
   زمني صفري ⇒ `None` لا `NaN` · وحبس الناتج بين `PREDICTION_MIN_TEMP_DECI_C` و
   `PREDICTION_MAX_TEMP_DECI_C` فلا يُطبع رقم بمئات الدرجات من عيّنة قافزة.
4. **الوصل بالمراقب للسطر التشخيصي وحده:** `TrendPredictor` يُغذّى في `handle_temperature_change` بعد
   الـEWMA، ويُقرأ فقط داخل `logger.debug` (لاحقة `Pred:{:.1}°C/45s`). **ولا يدخل `apply_intensity`**
   ولا أي قرار تبريد. والسبب: نموذج يؤثر في التحكّم قبل بلوغ العتبة يحتاج معايرة على جهاز؛ أما تنبؤ
   في السجلّ فيكشف الاتجاه بلا مخاطرة.
5. **النافذة ١٢ عيّنة** (≈ دقيقة عند الإيقاع الاحتياطيّ): تكفي لصعود جارٍ، ولا تُخفي ارتفاعًا بدأ الآن
   خلف هبوط قديم، ولا تنمو بلا حدّ في خدمة تعمل دائمًا.
6. **`RUST_DEAD_BASELINE` صار فارغًا** — لا صفّ استثناء بعد اليوم؛ وأي ملفّ Rust خارج الشجرة يُسقط
   `dead_modules --assert` بلا مذكرة.

**الملفّات:** `thermalcore/src/prediction.rs` (إعادة كتابة + ٧ اختبارات) · `thermalcore/src/monitor.rs` ·
`thermalcore/src/constants.rs` · `thermalcore/src/lib.rs` · `tools/dead_modules.py`.

**الحدود المعلنة:** الرياضيات مقيسة بـ`cargo test` (١٤/١٤ في الحزمة، منها ٧ للتنبؤ). **ولا يُدَّعى** أن
التنبؤ «يتوقّع الحرارة» على جهاز: هو استقراء خطّي لا يعرف الشحن ولا حمل المعالج ولا منحنى التبريد،
وفائدته التشخيصية لم تُقَس على عتاد. ومن أراد أن يقود التنبؤ قرارًا فليُعاير على جهاز ويُسجّل ADR جديدًا.

## ADR-83 — **`presentation_hw_writes` يقيس «تجاوز الطبقة» لا «ذكر أمر كتابة»، وسقفه صفر** (NT-13 · 2026-10-27)

**السبب:** البوابة كانت تعدّ **موضع الاستدعاء** بتعبيرين نصّيين: `echo/printf/tee` مع إعادة توجيه،
و`RootFileAccess.write`. فسجّلت ١٢ «مخالفة» ليست كلها مخالفة: **٧ كتابات صدفة خام حقيقية**، و**٣**
استدعاءات لِـ`RootFileAccess` (المسار المصرَّح بنصّ ADR-11 نفسه)، و**٢** إيجابيتان كاذبتان —
`technicalDetail = "echo 1 > /proc/sys/vm/compact_memory"` نصٌّ يُعرض للمستخدم، و`CrosshairStyle.TEE ->`
طابق كلمة `tee` ثم سهْم `->`. وبوابة تُبلّغ عن الطبقة المصرَّحة وعن نصّ معروض **تُعلّم المستخدم تجاهلها**،
وهذا أخطر من غيابها.

**القرار:**

1. **المواضع الحقيقية السبعة رُحّلت إلى `RootFileAccess`:** `BypassChargeScreen` (ملفّا الحالة والعتبة) ·
   `DisplayStudioViewModel` · `NetworkSchedulerViewModel` · `TweakViewmodel` (الاستعادة والحفظ) ·
   `ZramViewModel` (‏`compact_memory` عبر `writeFireAndForget` لأن العقدة **لا تُقرأ**، والتحقّق عليها
   يُنتج `unreadable` كاذبًا). فصار المسار واحدًا: اقتباس داخلي · IPC الجذر أولًا · رقصة `chmod` على
   HyperOS · قراءة تحقّق بعد الكتابة (`writeOutcome`).
2. **عتبة الشحن لا تُكتب صدفةً لكل خطوة إصبع:** الخاصية تُحدَّث أثناء السحب (نداء أصليّ رخيص)، والملفّ
   يُكتب **مرة واحدة عند `onValueChangeFinished`** — فيقلّ عدد أوامر الجذر ويتحقق آخر ما استقرّ عليه
   المؤشّر.
3. **تعريف البوابة المصحَّح:** تُحتسب الكتابة فقط إذا اجتمع **أمر كتابة صدفة** (`echo/printf/tee` مع
   إعادة توجيه إلى عقدة) **ونداء يشغّل عملية** على السطر نفسه (`Shell.cmd/sh` · `Runtime.exec` ·
   `ProcessBuilder`) — أو كان السطر سطرَ متابعة يبدأ بعلامة تنصيص داخل نداء متعدّد الأسطر.
   و`RootFileAccess.*` **ليس مخالفة**؛ هو الطبقة المصرَّحة.
4. **السقف ١٢ ← ٠** في `code_health_baseline.json`: أي كتابة خام جديدة تُسقط البوابة فورًا، لا تُسجَّل
   دَينًا يُجمَّد.
5. **الفحص الذاتي غُطّي بثلاثة أسباب الخطأ التاريخية كلها**: النداء متعدّد الأسطر (يُمسك)، والنصّ
   المعروض و`TEE ->` (لا يُمسكان)، و`RootFileAccess` (لا يُحتسب) — ٣١ تأكيدًا متعاكسًا.

**الملفّات:** `tools/code_health.py` (القاعدة + التجهيز + ٣١ تأكيدًا) · `tools/code_health_baseline.json` ·
الملفّات الخمسة في `ui/**` · `docs/ai/NEXT_TASK.md` (NT-13).

**الحدود — وهي حدّ الإغلاق لا هامشه:** الجهاز غائب هنا، فسلوك كل كتابة محوَّلة (هل تثبت على HyperOS 3؟
هل يراها `inotify` في الخادم؟ هل `compact_memory` يقبل رقصة `chmod`؟) **غير مُتحقَّق**. ولم تُجرَ مراجعة
السلامة المستقلة التي يشترطها §2 لتغيير يمسّ العتاد؛ ولا تُشترى بالبوابات الخفيفة. ولذلك: **الكود
مُسلَّم، والإغلاق النهائي معلَّق على جهاز + حكم `safety-reviewer`**، ولا يُقال «يعمل» بغيرهما.

## ADR-84 — **الجزيرة الأصلية في ROM تُكشف ولا تُتحكَّم: لا مفتاح مخمَّن ولا مساس بـSystemUI** (ISLAND-PREMIUM-01 · 2026-10-28)

**السبب:** الطلب تضمّن «تعطيل الجزيرة الأصلية مؤقّتًا (HyperIsland/ColorOS) اختياريًّا، افتراضيًّا مغلق،
مع حفظ الحالة واستعادتها، وبلا حذف/قتل SystemUI». والبحث في واجهات Android لم يُظهر **أي** API عامّ
يوقف الجزيرة الأصلية، ولا مفتاح `Settings` موثّقًا لها. والبدائل المتاحة كلّها مرفوضة: صفوف `Settings`
تخمينية تُكتب بلا أثر (وتُعيد نفس عطب `zen_mode` الذي رُفض في `FIVE-FIXES-01`)، وتعطيل مكوّن SystemUI
أو قتله يهدم شريط الحالة والإشعارات ويخالف الطلب نفسه.

**القرار:**

1. **لا كتابة عمياء ولا تعطيل لمكوّن نظام.** التطبيق لا يكتب أيّ مفتاح للجزيرة الأصلية، ولا يعطّل حزمة
   `systemui`، ولا يقتل عمليّة — لا في الإقلاع ولا عند الطلب ولا في أيّ فرع.
2. **القدرة تُقاس والحدّ يُقال:** `NativeIslandRules.classify` تأخذ حقائق (`Build.MANUFACTURER`/`BRAND`،
   `SDK_INT`، وجود حزم نظام معروفة) وتُعيد `nativeLikely` و`controllable = false` دائمًا مع سبب صريح.
   و`NativeIslandProbe` يفحص ستّ حزم نظام معروفة عند فتح الشاشة (`<queries>` في البيان كي لا يخفي
   Android 11+ الحزم)، بلا مسح جهاز كامل.
3. **البديل المتوافق:** تحكّم المستخدم في **ما تعرضه جزيرتنا** لكل نشاط (مفاتيح الأنشطة)، فيُمنع التكرار
   من طرفنا بلا لمس النظام. والمفتاح المعروض **مقفل** ويشرح السبب في مكانه، لا يظهر كأنه يعمل.
4. **أيّ محاولة لجعل `controllable = true` بلا واجهة نظام موثّقة تُسقط `NativeIslandTest`** (ستّة
   تأكيدات، منها أن Xiaomi وOPPO/OnePlus/realme وحزمة `com.oplus.systemui` تُعيد `false`). القرار
   محميّ باختبار لا بتعليق.
5. **إن ظهر على جهازٍ مفتاحٌ موثّق** (اسمه من الروم نفسه، أو تجربة على جهاز المالك): يُضاف بقراءة تحقّق
   بعده (`write → read → report`)، ويبقى **افتراضيًّا مغلقًا**، ويُستعاد ما كان قبل التجربة عند الإطفاء
   أو الإقلاع — ولا يُعمَّم على رومٍ آخر.

**الملفّات:** `island/core/NativeIsland.kt` · `island/core/NativeIslandProbe.kt` ·
`ui/subscreens/DynamicIslandTabs.kt` (بطاقة «الجزيرة الأصلية» في تبويب متقدّم) ·
`AndroidManifest.xml` (`<queries>`) · `src/test/.../NativeIslandTest.kt`.

**الحدود:** ما لا يُقاس هنا هو ما تعرضه الجزيرة الأصلية فعلًا على جهاز — ولأنّنا **لا نتحكّم فيها**،
فحدّنا المُعلن هو الكشف والشرح، ولا يُقال «عُطّلت» في أيّ نصّ للمستخدم.

## ADR-85 — **النقل إلى Rust/Zig/Mojo: ما يستحقّ قد نُقل، والباقي لا يُنقل بلا قياس** (FOOTPRINT-01 · 2026-10-28)

**السبب:** أمر المالك بعد جولة تقليل استهلاك MaxManager نفسه: «وانقل ما يستحسن نقله الي لغة rust او
zig او mojo». والسؤال ليس «أيّ لغة أسرع» بل **أيّ عمل يستحقّ عبور JNI أصلًا** — فالنقل الذي يُضيف
رحلة JNI لكل نبضة يُبطئ ولا يُسرّع.

**ما قِيس أولًا (لا مفترض):**

1. **العمل الثقيل في Kotlin اليوم يعبر إلى Rust فعلًا:** قراءة sysfs/proc بالجملة (`probe.rs`)،
   الخصائص (`sysprop.rs`)، تحليل السجلّات (`logparse.rs`)، مسح الملفّات (`scan.rs`)، ZIP
   (`archive.rs`)، الوكيل/التوقّع/التوأم الرقميّ (`rl_agent.rs` · `power_predictor.rs` ·
   `digital_twin.rs`)، المحرّك السياقيّ (`contextual_engine.rs`)، وبصمات السلامة (`security.rs`).
   وكلّها خلف جسور قائمة (`ProbeBridge` · `PropBridge` · `ScanBridge` · `ArchiveBridge` ·
   `SecurityBridge` · `PredictorBridge` · `ContextBridge`)، و`thermalcore` خدمة Rust مستقلّة.
   وبناء `.so` للـABIs موصول في CI بـ`cargo ndk` — أي أنّ مسار Rust **إنتاجيّ قائم** لا تجربة.
2. **`cargo test` على المضيف: ٦٤/٦٤ ناجح، ٠ فشل** (بـ`CARGO_TARGET_DIR` خارج بطاقة `sdcard`،
   لأن مجلّد `target` الافتراضي على حامل `noexec`). و`tools/jni_symbols.py` يطابق ٢٣ رمزًا.
3. **ما بقي في Kotlin ليس مرشّحًا:** إمّا يمسّ إطار أندرويد (Binder · Compose · Service ·
   `FileObserver` · `WindowManager` · مستمع الإشعارات) فلا يعبر JNI إلا بتمرير كائنات أو استدعاءات
   راجعة أغلى من العمل نفسه؛ وإمّا رياضيات صغيرة نقيّة (`IslandLayout` · `IslandGeometry` ·
   `IslandText` · `CompanionPollPolicy`) مغطّاة باختبارات JVM — ونداء JNI لكل نبضة أبطأ من الحساب
   داخل العملية؛ وإمّا تحليل سطر واحد (`/proc/stat`) مكسبه صفر وعبوره أغلى.

**القرار:**

1. **لا نقل جديد بلا ثلاثة شروط مقيسة:** (أ) مسار ساخن مقيس فعلًا (لا «يُرجَّح»)، (ب) مدخلاته
   ومخرجاته بايتات/مصفوفات لا كائنات إطار، (ج) يمرّ اختبارات المضيف **وبوّابة `jni_symbols`**.
   وما لا يحقّقها يبقى Kotlin — وهذا قرار، لا تهاون.
2. **Zig مرفوض:** لا سلسلة أدوات أندرويد في المستودع، ولا NDK محليًّا، ولا مسار JNI موثّق هنا؛
   وإدخال toolchain ثالثة يضيف كلفة بناء وCI وصيانة **بلا مكسب مقيس** — وهو نقيض قاعدة القياس.
3. **Mojo مرفوض:** لا مسار JNI/NDK إنتاجيّ موثّق، ولا سلسلة أدوات مثبَّتة في هذه البيئة؛ وإدخاله
   يضيف اعتمادًا خارجيًّا لا نتحكّم بترخيصه، مقابل صفر مكسب مقيس على مسار قائم يعمل.
4. **Rust هو المسار الأصليّ الوحيد** — لأنه موجود ومُقاس ومربوط بالبناء، ولأنّ إضافة لغة ثانية
   تعني نسختَي منطق تتباعدان (وهو عطب أكبر من أي مكسب أداء نظريّ).

**الحدود:** لا NDK في هذه البيئة ⇒ `.so` لا يُبنى هنا، والطبقة الثانية من `jni_symbols` (الرموز في
الثنائيات) **غير مُتحقَّقة محليًّا** وتُتحقَّق في CI. وزمن الاستجابة والحرارة على جهاز حقيقيّ يبقى
**يحتاج جهازًا** — ولا يُدَّعى من المضيف.

**الملفّات:** `manager/src/main/rust/**` · `thermalcore/**` · `.github/workflows/build.yml` (خطوات
`cargo ndk` و`cargo test`) · `tools/jni_symbols.py`.
