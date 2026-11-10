# جرد MaxManager — من المستودع فقط

> قياسٌ بتاريخ 2026-11-08 على شجرة `/storage/emulated/0/MaxManger/optmize-main`.
> كل رقم هنا أُخرج بأمر على الشجرة؛ وما لم يُقَس مكتوب «غير موجود» أو «غير مقيس».
> لا رأي تصميمي في هذا الملف — التشخيص في `diagnosis.md`.

---

## 0. حقائق القسم 0 (المطلوبة قبل أي رأي)

| السؤال | الجواب المقيس | الدليل |
| --- | --- | --- |
| مسار الجذر | `/storage/emulated/0/MaxManger/optmize-main` (نفسه `/sdcard/MaxManger/optmize-main`) | `pwd` |
| أمر البناء | `cd manager && ./gradlew :app:assembleDebug` (وأمر الاختبارات `:app:testReleaseUnitTest`) | `AGENTS.md` §5 · `manager/gradlew` |
| أمر معاينة | **لا أمر ولا خادم معاينة**. توجد `@Preview` واحدة في الشجرة (`grep -rn "@Preview"` = ١)، ومعاينة «الجزيرة» شاشةٌ داخل التطبيق (`IslandPreview.kt`) لا معاينة Compose سلكية | `grep -rn "@Preview"` · `find -iname '*preview*'` = ١٢٨ (معظمها أصول ودجات) |
| إطار الواجهة | **Compose** (Material 3) في نشاطين: `MainActivity` و`GameLobbyEntryActivity`؛ Views فقط في `RemoteViews` للودجات | `MainActivity.kt` · `ui/**` |
| اسم الثيم | `MaxManagerTheme` في `ui/theme/Theme.kt`، وفوقه طبقة رموز `ui/design/MaxTokens.kt` | `Theme.kt` · `MaxTokens.kt` |
| مكوّنات مشتركة | `MaxScreenScaffold` · `MaxListScreen` · `MaxCard`/`MaxCardShell` · `MaxControlRows` (`MaxSwitchRow`/`MaxSliderRow`/`MaxNavigationRow`/`MaxChoiceRow`) · `MaxMetric`+`MaxTrustChip` · `ContentStateComponents` (`MaxLoadingState`/`MaxEmptyState`/`MaxErrorState`) · `ExpressiveList` · `MaxNavBar` · `PrimaryDeck` · `ScreenFinderSheet` | `ui/design/` · `ui/component/` |
| اللغات | ٨٤ مجلّد `values-*`؛ الإنجليزية `values/` والعربية `values-ar/` كاملتان تقريبًا (٦٢١١ نصًّا مقابل ٦١٩٩) | `ls -d res/values-*` · عدّ `<string ` |
| العربية وRTL | **جزء من المنتج**: العربية لغة أولى، والتخطيطات تُقرأ RTL، والاختبارات تذكر RTL | `AGENTS.md` ADR-14 · `ScreensSweepTest` |
| بوابة الكتابة الحسّاسة | `HardwareControlArbiter.submit(...)` — عقد واحد: قراءة خط أساس ← كتابة `apply` ← تحقق `verify`/`realized` ← استرجاع `restore` عند الفشل | `core/hardware/HardwareControlArbiter.kt` |
| مَن يحكم | `ControlOwnership` بسلّم `RECOVERY(100) > SAFETY(80) > PER_APP(60) > MAX_AI(40) > GLOBAL_PROFILE(20) > SYSTEM(0)`، و`ManualControlLocks` تمنع الآلي عن مقبض لمسه المستخدم | `core/hardware/ControlOwnership.kt` |
| المداخل الخارجية | ٣ بلاطات إعدادات سريعة · ٢٢ ودجة · اختصار واحد (Game Lobby) · خدمة استماع إشعارات · خدمة إتاحة · `nd.max.ACTION_MANAGE` · `OPEN_ROUTE` extras | `AndroidManifest.xml` · `res/xml/shortcuts.xml` |
| العدد الكلي | ٨٦٨ ملف Kotlin · ٤٢٨ منها في `ui/` · ٦١ ملف `*Screen.kt` · **٦٩ وجهة مسجّلة في `MaxDestination.All`** = ٦٩ كائن `data object` (صفر فرق، مُقاس بعد M1a) | `find` · `grep data object` · موازنة أقواس `All` |

**ما ليس موجودًا صراحةً:** لا deep links من نوع `VIEW`/`BROWSABLE` (صفر تطابق في المانيفست)، لا خادم معاينة، لا قياس استخدام حقيقي، لا اختبارات مستخدمين في الشجرة.

---

## 1. الشاشات الحيّة

### 1.1 الأقسام الأربعة (سطح `PrimaryDeck`)

ليست مسارات تُنتقل، بل صفحات `HorizontalPager` جاهزة قبل السحب، والمسار يُكتب عند الاستقرار:

| القسم | الملف | ملاحظة |
| --- | --- | --- |
| الآن | `ui/mainscreens/HomeScreen.kt` → `LegendaryHomeDashboard` | لوحة الجهاز (البطل + شبكة القيم + بطاقات القدرة) |
| التطبيقات | `ui/mainscreens/ApplistScreen.kt` | قائمة + بحث + مرشّح |
| التحكّم | `ui/mainscreens/ControlScreen.kt` | ٩ نطاقات + أدوات متقدّمة، عرضان (مختصر/موسّع) |
| الإعدادات | `ui/mainscreens/SettingsScreen.kt` | ٧ أقسام |

### 1.2 وجهات مسجّلة (`MaxDestination.All` = ٦٩)

- **عشرة نطاقات تحكّم** (`MaxDomainHubScreen`): CPU · GPU · الذاكرة · العرض · الاستجابة · الحرارة · الطاقة · التخزين · الشبكة · الصوت.
- **شاشات النطاقات**: `CpuCoreControl` · `GovernorSettings` · `PreferenceTweaks` · `GpuStudio` · `ZramManager` · `MemoryUfs` · `DisplayStudio` · `Resolution` · `TouchBoost` · `FpsGo` · `Fas` · `FpsOverlay` · `DynamicIsland` · `ThermalDetail` · `Charging` · `BypassCharging` · `BypassChargingCheck` · `DozeMode` · `Dex2oat` · `StorageDetail` · `StorageCare` · `NetworkScheduler` · `NetworkDetail` · `AudioStudio`.
- **التطبيقات والنظام**: `ProcessManager` (تحت `MemoryHub` بعد M1a) · `DebloatFreeze` · `AppSettings` · `ActivityLauncher` · `UsageStats` · `Permissions` · `HmaCompanion` · `Plugins` · `FileManager` · `SetEdit` (تحت `Control`: رُفض إحالتهما إلى المختبر لأنه اختبار عتاد لا صندوق أدوات) · `DeviceInfo` · `DeviceLab` · `MaxLive` · `Diagnostics` · `Logs` · `ConfigBackup` · `MaxBackup` · `BatteryHistory` (تحت `PowerHub` بعد M1a) · `CommunityStats` · `ModuleHealth` · `Privilege` · `License` · `About`.
- **الأسطح الخاصة**: `GetStarted` (أول تشغيل) · `MaxAi` · `MaxAiIntro` · `GameLobby` · `EmulatorHub` · `ColorPalette` · `ColorScheme`.

### 1.3 ما هو ميّت فعلًا (لا يُحسب ضمن الجرد الحيّ)

- `ui/mainscreens/StoryboardHome.kt` (+`StoryboardModel.kt`/`StoryboardSources.kt`) — **صفر مستدعٍ** خارج ملفاته؛ الشاشة الحيّة هي `LegendaryHomeDashboard`.
- مسارات `Now`/`Control`/`Apps`/`Settings` في `MaxNavGraph` مقاعد فارغة بقصد — يرسمها `PrimaryDeck` فوقها (تعليق صريح في `MaxNavGraph.kt`).

---

## 2. الميزات مربوطًة بالشاشات (الشاشة ليست ميزة)

| المجموعة | الميزات | حيث تُرى |
| --- | --- | --- |
| المعالجات | ترددات النوى، الحاكم، مقابض الأولوية، الالتزام | `CpuCoreControl` · `GovernorSettings` · `PreferenceTweaks` |
| الرسوم | السقوف، التردد، مسار المزوّد | `GpuStudio` |
| الذاكرة | ZRAM، ضغط/سواب، UFS | `ZramManager` · `MemoryUfs` |
| العرض | الدقة، معدّل التحديث، HDR/سطوع | `DisplayStudio` · `Resolution` |
| الاستجابة | لمس، FPS Go، FAS | `TouchBoost` · `FpsGo` · `Fas` |
| الحرارة | مناطق حرارية، سقوف، تليمت | `ThermalDetail` |
| الطاقة | شحن، تجاوز، Doze، تاريخ البطارية | `Charging` · `BypassCharging` · `BypassChargingCheck` · `DozeMode` · `BatteryHistory` |
| التخزين | تفصيل، عناية، dex2oat، ملفات، نسخ | `StorageDetail` · `StorageCare` · `Dex2oat` · `FileManager` · `MaxBackup` |
| الشبكة | جدولة، تفصيل | `NetworkScheduler` · `NetworkDetail` |
| الصوت | أجهزة، دفقات، مؤثرات | `AudioStudio` |
| التطبيقات | قائمة، إعدادات تطبيق، تجميد، عمليات، إتاحة، استخدام، أذونات | `Applist` · `AppSettings` · `DebloatFreeze` · `ProcessManager` · `HmaCompanion` · `UsageStats` · `Permissions` |
| النظام | تشخيص، معلومات جهاز، مختبر، سجلات، نسخ إعداد، مجتمع، صحة وحدة، امتياز، ترخيص، حول | `Diagnostics` · `DeviceInfo` · `DeviceLab` · `Logs` · `ConfigBackup` · `CommunityStats` · `ModuleHealth` · `Privilege` · `License` · `About` |
| المظهر | لوحة ألوان، سمة | `ColorPalette` · `ColorScheme` |
| MAX AI | محرّك، تمهيد، بثّ حيّ | `MaxAi` · `MaxAiIntro` · `MaxLive` |
| بوّابات عن بُعد | ٧ رايات (`RemoteFlags`) تُقفل أسطحًا: MAXAI · AUDIO · GAMESPACE · EMUHUB · BYPASS_CHARGE · LICENSE · … | `FeatureGate(...)` في `MaxNavGraph` |

---

## 3. التنقّل

| نمط | الوجود | الدليل |
| --- | --- | --- |
| رئيسي | ٤ أقسام في `HorizontalPager` + `MaxNavBar` يتبع `targetPage` | `PrimaryDeck.kt` · `MaxNavBar.kt` |
| ثانوي | `MaxDomainHubScreen` لكل نطاق؛ بنية أب/ابن في `MaxDestinations` | `MaxDestinations.kt` |
| متقدّم | قسم «أدوات متقدّمة» في `ControlScreen`، ومقفل الأصل في الإعدادات (ADR-16) | `ControlScreen.kt` سطر ١٨٠ |
| بحث | `ScreenFinderSheet` داخل التحكّم فقط (بحث بالاسم لا بالقصد) | `ScreenFinderSheet.kt` |
| بحث ثانٍ | حقل بحث في قائمة التطبيقات | `ApplistScreen.kt` |
| إيماءات | `SwipeGestureArbiter` بين الأقسام + رجوع النظام | `ui/navigation/Swipe*` |

---

## 4. نقاط الربط بالعتاد (حسّاسة — لا تُمس إلا بتغليف بصري)

| النقطة | الملف | الحساسية |
| --- | --- | --- |
| المراجِع الوحيد | `core/hardware/HardwareControlArbiter.kt` | قراءة خط أساس، كتابة، تحقق، تراجع، منع بالتفويض |
| سلّم الحكم | `core/hardware/ControlOwnership.kt` | من يملك المفتاح الآن |
| قفل اليدوي | `core/hardware/ManualControlLocks.kt` | يمنع الآلي عن مقبض مسّه المستخدم |
| التحقق | `core/hardware/HardwareVerification.kt` | سقف/مدى لا تساوٍ حرفي |
| الظلال/القراءة | `HardwareRuntime`, `DeviceStateCollector`, `Atlas*` | قراءة فقط في معظمها |
| شاشات تكتب | `CpuCoreControl` · `GpuStudio` · `MemoryUfs` … عبر ViewModel ← المراجِع | كلها `owner = GLOBAL_PROFILE` في `viewmodel/` |
| تشخيص الحكّام | `DiagnosticsScreen.OwnershipDiagnosticsCard` (سطر ٨٦٢) | عرض `ControlOwnership.snapshot()` كما هو |

**قاعدة قائمة يجب ألا تُخترق:** لا يوجد في `ui/` مسار كتابة sysfs مباشر — الكتابة عبر الـViewModel إلى المراجِع. (يُعاد فحصه في كل شريحة.)
**والاستثناء الوحيد المقيس:** `FpsOverlayScreen.exportSession` يكتب ملفّ CSV في `cacheDir` للمشاركة — ليس عتادًا ولا sysfs، ولا يمسّ بوابة الكتابة.

---

## 5. المداخل الخارجية

| المدخل | العدد | الوجهة/السلوك |
| --- | --- | --- |
| بلاطات QS | ٣ | `BypassChgTileService` · `ProfileTileService` · `StatusBarTileService` |
| ودجات | ٢٢ مزوّدًا | تُفتح عبر `OPEN_ROUTE`؛ أكثرها يعرض قيمة ويقود إلى شاشة |
| اختصار مثبّت | ١ | `nd.max.action.OPEN_GAME_LOBBY` → `GameLobby` عبر `RemoteFlags.GAMESPACE` |
| نيّة إدارة | ١ | `nd.max.ACTION_MANAGE` على `MainActivity` |
| خدمة إشعارات | ٢ | استماع/إدارة (الجزيرة وغيرها) |
| خدمة إتاحة | ١ | لمس/تحكّم آليّ |
| Deep links http | **صفر** | لا `VIEW`/`BROWSABLE` في المانيفست |

---

## 6. التكرار والأنماط البصرية المتعارضة

| التكرار | القياس | الحكم |
| --- | --- | --- |
| عرضان لصفحة التحكّم | `compact` و`expanded` من نفس النموذج، مفتاح في الشريط | مقصود ومُوثَّق؛ لكنه يُضيف «وضعًا» يحفظه المستخدم في التفضيلات |
| شاشات النطاق تكرّر صفوف التحكّم | `MaxDomainHubScreen` يعرض `maxHubRows` نفسها؛ التعليق في `ControlScreen` يعترف بذلك | ازدواج مقصود للوصول العميق، لكنه بابان لنفس الصف |
| نظاما بطاقات | `MaxCard` (أيقونة/عنوان/وصف) + `MaxCardShell` (هندسة) | مكمّلان بقصد، لا بديلان |
| لغتا تحذير | `WarningBanner` يستعمل `colorScheme.error`، والرموز تعرّف `MaxTone.Critical` | لونان دلاليّان لنفس المعنى |
| نصوص إنجليزية مثبّتة | ٢٢ موضع `Text("…")` إنجليزي في `ui/`، منها «CURRENT STATE» في `DecisionHierarchy` و«No policy currently owns…» في التشخيص | خرق لقاعدة العربية/الإنجليزية (ADR-14) ولـ«لا مصطلح كود في الواجهة» |
| مفردات آلية في الواجهة | `lease.key` و`lease.ownerToken` تُطبع خامًّا؛ `map:read-only` في `RouteVerdictLabel` (بقصد توثيقي) | الأول عطب، الثاني مُعلَن |
| شرائح الثقة | `MaxTrustChip` في ٣ ملفات فقط من ٤٢٨ | النظام موجود، التغطية غير متساوية |
| مسجّلو شاشات | `StoryboardHome` ميّت؛ `LegendaryHomeDashboard` حيّ | ملف ميّت يضاعف كلفة القراءة |

---

## 7. ما يجب إبقاؤه حرفيًّا

- الأقسام الأربعة والـPager (لا تُعاد كتابتها).
- `MaxDestination` سجلًّا وحيدًا للمسارات (ADR-02) — لا مسار حرفي في شاشة.
- `HardwareControlArbiter` وبوّاباته — لا مسار كتابة ثانٍ.
- سلّم `ControlOwnership` وأولوياته — يُعرض ولا يُغيَّر.
- `ManualControlLocks` وسلوك الخدمات الخلفية والحماية الحرارية.
- المداخل الخارجية الثلاثة والعشرين — تُحال ولا تُكسر.
- رموز `MaxTokens` والثيم — تُوسَّع لا تُستبدل.
