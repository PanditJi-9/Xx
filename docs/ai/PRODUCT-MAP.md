# خريطة المنتج — حصر مقيس وإعادة هندسة مرحلية

> هذا الملفّ ليس اقتراحًا. أرقامه مستخرجة من الشجرة نفسها بالأمر الموصوف في §٢، وكل عيب فيه
> مسنود إلى عدد يمكن إعادة اشتقاقه. وما لا يُقاس هنا يُعلن في §٧ ولا يُدّعى.

## ١. الخلاصة المقيسة

| المقياس | القيمة |
| --- | --- |
| وجهات مسجّلة (`data object … : MaxDestination`) | **٦٩** |
| جذور بلا أب | **٨** |
| جذور أساسية (لها تبويب) | **٤** — `Now` · `Apps` · `Control` · `Settings` |
| جذور غير أساسية | **٤** — `MaxAi` · `DeviceInfo` · `GetStarted` · `HmaCompanion` |
| شاشات في العمق ١ | **٣٤** |
| شاشات في العمق ٢ | **٢٧** |
| أعمق مسار | **٢** (لا شاشة في العمق ٣) |
| أبناء `Control` المباشرون | **١٦** |
| مخفية بأمر المالك | **٣** — `TouchBoost` · `CommunityStats` · `Plugins` |
| مصنّفة `MaxRisk.Advanced` | **٧** |
| وجهات بوسيط مسار | **٤** — `DeviceInfo` · `AppSettings` · `MaxBackup` · `Permissions` |

**وقاعدة العدّ:** `grep -c "data object .* : MaxDestination"` يعطي ٦٩، وهو نفس ما تعدّه بوابة
`readme_assets.py`. وأي قراءة تعطي ٧٠ تكون قد حسبت تعريف الصنف الأب أو سطرًا معلّقًا — لا شاشة.

## ٢. كيف تُعاد هذه الأرقام

الحصر لا يُكتب من الذاكرة. يُشتقّ بثلاث خطوات على `manager/app/src/main/java/nd/max/ui/navigation/MaxDestinations.kt`:

1. التقط كل `data object <Name> : MaxDestination(` ثم وازِن الأقواس من موضع الفتح حتى الإغلاق
   المطابق — لا بتعبير سطري، لأن بعض التعريفات تحمل تعليقات داخلها.
2. فكّك الوسائط على الفواصل في المستوى الصفري (مع تجاهل التعليقات): الأولى هي `route`، وأول
   وسيط يطابق اسم وجهة معروفة هو `parent`، والباقي أعلام (`risk` · `isPrimary` · `hidden`).
3. العمق = عدد خطوات المشي في `parent` حتى الجذر. والترتيب في الجدول = ترتيب الشجرة: الجذور
   الأساسية أولًا ثم أبناؤها بالعمق، وكل أب قبل أبنائه.

## ٣. الشجرة كاملة — ٦٦ وجهة بترتيب التطبيق

> الجدول مقيس من السجلّ مباشرة: `data object \w+ : MaxDestination` = ٦٦ بعد حذف شاشات
> التشخيص وصحة الوحدة وHMA بأمر المالك (تكملة ٨٥). والترتيب: الجذور ثم أبناؤها بالعمق.

| # | الشاشة | المسار | الأب | العمق | النوع | ملاحظة مقيسة |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Now | `now` | — | 0 | Matrix |  |
| 2 | Apps | `apps` | — | 0 | Library |  |
| 3 | Control | `control` | — | 0 | Matrix | **٢٣ ابنًا مباشرًا** — ١٠ محاور و١٣ أداة في أربعة نطاقات بأمر المالك (تكملة ٨٥) |
| 4 | Settings | `settings` | — | 0 | Library |  |
| 5 | MaxAi | `max_ai` | — | 0 | Console | جذر غير أساسي — لا تبويب له في الشريط السفلي |
| 6 | DeviceInfo | `device_info?section={section}` | — | 0 | Matrix | جذر غير أساسي بوسيط `{section}` |
| 7 | GetStarted | `get_started` | — | 0 | Flow | جذر غير أساسي |
| 8 | DebloatFreeze | `debloatfreeze` | Control | 1 | Library |  |
| 9 | AppSettings | `app_settings/{pkg}` | Apps | 1 | Console |  |
| 10 | GameLobby | `gamelobby` | Apps | 1 | Library |  |
| 11 | EmulatorHub | `emulatorhub` | Apps | 1 | Library |  |
| 12 | CpuHub | `hub_cpu` | Control | 1 | Console |  |
| 13 | GpuHub | `hub_gpu` | Control | 1 | Console |  |
| 14 | MemoryHub | `hub_memory` | Control | 1 | Console |  |
| 15 | DisplayHub | `hub_display` | Control | 1 | Console |  |
| 16 | ResponsivenessHub | `hub_responsiveness` | Control | 1 | Console |  |
| 17 | ThermalHub | `hub_thermal` | Control | 1 | Console |  |
| 18 | PowerHub | `hub_power` | Control | 1 | Console |  |
| 19 | StorageHub | `hub_storage` | Control | 1 | Console |  |
| 20 | NetworkHub | `hub_network` | Control | 1 | Console |  |
| 21 | AudioHub | `hub_audio` | Control | 1 | Console |  |
| 22 | FpsOverlay | `fpsoverlay` | Control | 1 | Instrument |  |
| 23 | GameSessions | `gamesessions` | Control | 1 | Ledger |  |
| 24 | DynamicIsland | `dynamic_island` | Control | 1 | Studio |  |
| 25 | DeviceLab | `device_lab` | Control | 1 | Matrix | **اختبار عتاد خالص** (شاشة · لمس · أزرار · صوت · مستشعرات · بطارية · شبكة) — **نُقل في تكملة ٨٥** إلى التحكّم بأمر المالك |
| 26 | UsageStats | `usage_stats` | Control | 1 | Ledger | إحصاء الاستخدام — **نُقل في تكملة ٨٥** إلى التحكّم بأمر المالك |
| 27 | ColorScheme | `colorscheme` | Control | 1 | Console | نظام الألوان — **نُقل في تكملة ٨٥** إلى التحكّم بجوار `ColorPalette` |
| 28 | Logs | `logsviewer` | Control | 1 | Ledger | سجلّ الأحداث — **نُقل في تكملة ٨٥** إلى التحكّم بأمر المالك |
| 29 | MaxBackup | `max_backup?pkg={pkg}` | Control | 1 | Flow | نسخ التطبيقات واستعادتها — **نُقل في تكملة ٨٥** إلى التحكّم · `MaxRisk.Advanced` |
| 30 | Permissions | `max_perms?pkg={pkg}` | Control | 1 | Library | صلاحيات التطبيقات وAppOps — **نُقل في تكملة ٨٥** إلى التحكّم · `MaxRisk.Advanced` |
| 31 | SetEdit | `setedit` | Control | 1 | Console | محرّر إعدادات — أداة متقدّمة بلا محور · `MaxRisk.Advanced` |
| 32 | ActivityLauncher | `activitylauncher` | Control | 1 | Library | مشغّل أنشطة — أداة متقدّمة بلا محور · `MaxRisk.Advanced` |
| 33 | License | `license` | Settings | 1 | Flow |  |
| 34 | ColorPalette | `color_palette` | Control | 1 | Studio | **نُقل في تكملة ٨٥** إلى التحكّم بأمر المالك |
| 35 | ConfigBackup | `config_backup` | Settings | 1 | Flow |  |
| 36 | CommunityStats | `community_stats` | Settings | 1 | Matrix | مخفي بأمر المالك · مخفي |
| 37 | Plugins | `plugins` | Settings | 1 | Library | مخفي بأمر المالك · مخفي |
| 38 | About | `aboutscreen` | Settings | 1 | Flow |  |
| 39 | Privilege | `privilege` | Settings | 1 | Console |  |
| 40 | MaxLive | `max_live` | MaxAi | 1 | Instrument |  |
| 41 | MaxAiIntro | `max_ai_intro` | MaxAi | 1 | Flow |  |
| 42 | CpuCoreControl | `cpucorecontrol` | CpuHub | 2 | Matrix |  |
| 43 | GovernorSettings | `governorsettings` | CpuHub | 2 | Console |  |
| 44 | PreferenceTweaks | `preferenced` | CpuHub | 2 | Console |  |
| 45 | GpuStudio | `gpustudio` | GpuHub | 2 | Studio |  |
| 46 | ZramManager | `zrammanager` | MemoryHub | 2 | Instrument |  |
| 47 | MemoryUfs | `memory_ufs` | MemoryHub | 2 | Matrix | `MaxRisk.Advanced` |
| 48 | DisplayStudio | `displaystudio` | DisplayHub | 2 | Studio |  |
| 49 | Resolution | `resolutionscreen` | DisplayHub | 2 | Console |  |
| 50 | TouchBoost | `touchboost` | ResponsivenessHub | 2 | Console | مخفي بأمر المالك — يبقى في السجلّ ولا يُعرض · مخفي |
| 51 | FpsGo | `fpsgoscreen` | ResponsivenessHub | 2 | Console |  |
| 52 | Fas | `FasScreen` | ResponsivenessHub | 2 | Console |  |
| 53 | ThermalDetail | `thermal_detail` | ThermalHub | 2 | Instrument |  |
| 54 | Charging | `chargingscreen` | PowerHub | 2 | Instrument |  |
| 55 | BypassCharging | `bypasschg` | PowerHub | 2 | Console |  |
| 56 | BypassChargingCheck | `bypasschg_check` | PowerHub | 2 | Flow |  |
| 57 | DozeMode | `dozemode` | PowerHub | 2 | Console |  |
| 58 | Dex2oat | `dex2oat` | StorageHub | 2 | Console |  |
| 59 | StorageDetail | `storage_detail` | StorageHub | 2 | Matrix |  |
| 60 | StorageCare | `storage_care` | StorageHub | 2 | Console | `MaxRisk.Advanced` |
| 61 | NetworkScheduler | `networkscheduler` | NetworkHub | 2 | Console |  |
| 62 | NetworkDetail | `network_detail` | NetworkHub | 2 | Instrument |  |
| 63 | AudioStudio | `audio_studio` | AudioHub | 2 | Studio |  |
| 64 | ProcessManager | `processmanager` | MemoryHub | 2 | Library | قائمة عمليات حيّة — **نُقلت في M1a** إلى محور الذاكرة |
| 65 | BatteryHistory | `battery_history` | PowerHub | 2 | Ledger | تاريخ الطاقة — **نُقلت في M1a** إلى محور الطاقة |
| 66 | FileManager | `filemanager` | StorageHub | 2 | Library | مدير ملفات — **نُقل في تكملة ٨٥** إلى محور التخزين حيث تُدار المساحة · `MaxRisk.Advanced` |

## ٤. العيوب النظامية — خمسة، كلٌّ بعدد

### ع١ — `Control` صار الباب الواحد للأدوات بأمر المالك
**٢٣ ابنًا مباشرًا** من ٦٦ وجهة (٣٥٪): عشرة محاور (`CpuHub` … `AudioHub`) وثلاثة عشر أداة لا محور
لها. وكان العدد ١٦ قبل **تكملة ٨٥**، فنقل المالك إليها أدوات التشخيص والألوان والنسخ والصلاحيات
والتنظيف ووضع المصرف. فالكثافة هنا **قرار مالك لا سهو**، والفرق بين ١٦ و٢٣ ليس انحدارًا بل تنفيذًا.
**وما يُدار بها:** أربعة نطاقات معنونة (`control_tools_live` · `control_tools_advanced` ·
`control_tools_system` · `control_tools_appearance`) تُبنى من السجلّ لا من قائمة ثانية.
**والحدّ الباقي:** ارتفاع الصفحة على ٣٦٠dp يحتاج جهازًا — لا يُدّعى أنه مقبول بلا قياس بصري.
ومحاولة تفريغ `Control` بإحالة أدواته إلى `DeviceLab` **رُفضت بأمر المالك**: المختبر اختبار عتاد
لا صندوق أدوات.

### ع٢ — العائلة الواحدة في مكان واحد
- ~~الطاقة~~: أُعيدت في **M1a** — `BatteryHistory` تحت `PowerHub` مع `Charging` · `BypassCharging` · `BypassChargingCheck` · `DozeMode`.
- ~~العمليات~~: أُعيدت في **M1a** — `ProcessManager` تحت `MemoryHub` مع `ZramManager`/`MemoryUfs`.
- ~~السجلّات والتشخيص~~: كانت في M1c تحت `Diagnostics`، ثم **حُذفت الشاشة ونُقلت الثلاثة إلى `Control`** في تكملة ٨٥.
- ~~اللون~~: `ColorPalette` و`ColorScheme` صارا **تحت `Control`** في تكملة ٨٥ (كانا في `Settings`).
- ~~النسخ والأذونات~~: `MaxBackup` و`Permissions` صارا **تحت `Control`** في تكملة ٨٥ (كانا في `Settings`).
**الأثر:** كل عائلة أُعيدت إلى أب واحد — الطاقة في `PowerHub`، والعمليات في `MemoryHub`، وأدوات
التشخيص والألوان والنسخ والصلاحيات في `Control`. و`ConfigBackup`/`Privilege` بقيا في `Settings`
لأنهما نسخ الإعدادات ومنح الامتياز، لا نسخ التطبيقات وأذوناتها. وصفر عائلة مفصولة على أبين.

### ع٣ — ثلاثة جذور بلا باب
`MaxAi` · `DeviceInfo` · `GetStarted` جذور كاملة (parent = null) لكن الشريط السفلي يحمل أربعة
تبويبات فقط. أي أن **ثلاثة من سبعة جذور** يُبلغ إليها بمسار أو زرّ داخل شاشة، لا بموقع ثابت.
`MaxAi` خصوصًا — وهو أعلى أولوية بعد السلامة وملفّات التطبيقات — ليس له باب دائم. و`HmaCompanion`
خرج من القائمة بحذف شاشته بأمر المالك، فسقط بابه وسقط الجذر معه.

### ع٤ — العمق يتوقّف عند ٢ والكثافة تنفجر في ١
٣٤ شاشة في العمق ١ مقابل ٢٥ في العمق ٢، وسبعة جذور، وصفر في العمق ٣. فالشجرة ليست عميقة — بل
**عريضة عند المحاور ومسطّحة تحتها**. والنتيجة أن `Control` يحمل العبء الأكبر (ع١) بينما المحاور
أوراق بلا أبناء إلا في مواضع قليلة.

### ع٥ — حالة خاصة بلا موضع مراجعة
`hidden` (٣) و`MaxRisk.Advanced` (٧) ووسائط المسار (٤) = **١٤ وجهة من ٦٦ (٢١٪)** تحمل حالة
استثنائية. لا شاشة واحدة تجمعها للمراجعة، ولا بوابة تمنع ظهور مخفيّة في قائمة جديدة. الحالة
موجودة في الكود ومتفرّقة في السلوك.

## ٥. خريطة الهجرة — M0 إلى M4 + تسجيل الجلسات

كل مرحلة تُقبل أو تُرفض بعدد، لا برأي. وما لم تُقَس مرحلته لا تُعلَن منجزة.

| المرحلة | التغيير | معيار القبول المقيس |
| --- | --- | --- |
| **M0** (هذه) | هذه الخريطة + أنواع الشاشات في `DESIGN.md` + حارس الأبواب `DestinationDoorTest` | ٦٦ وجهة في الجدول (بعد حذف ثلاث شاشات بأمر المالك) · صفر وجهة بلا نوع · صفر وجهة بلا أب معروف · **صفر وجهة مرئية بلا باب** (الاختبار يسقط إن نُقلت وجهة إلى أبٍ لا يذكرها) |
| **M1** (منجزة على مرحلتين) | تفكيك `Control`: المحاور العشرة + الأسطح الحيّة (`FpsOverlay` · `GameSessions` · `DynamicIsland`) + ثلاثة أدوات متقدّمة بلا محور؛ والعائلات إلى محاورها | أبناء `Control` **١٦** يومها (تاريخيّ — صار ٢٣ بأمر المالك في تكملة ٨٥) · **صفر عائلة مفصولة** ✓ · ٦٦ وجهة بعد الحذف |
| **M1a** (منجزة) | نقل العائلتين اللتين أثبت القياس أن بابها الجديد يعرض أبناءه من السجلّ: `BatteryHistory→PowerHub` · `ProcessManager→MemoryHub` | أبناء `Control` **٢٤ ← ٢٢** · العمق ٣٩/٢٢ ← **٣٧/٢٤** · صفر تكرار في صفحة التحكّم (حُذف الاثنان من قائمتيها) |
| **M1b** (**مرفوضة بأمر المالك**) | كانت: `SetEdit` · `ActivityLauncher` · `FileManager` → `DeviceLab` وقسم «أدوات المختبر» يقرأها من السجلّ | **السبب المقيس:** `DeviceLabScreen` اختبار عتاد (شاشة · لمس · أزرار · اهتزاز · صوت · مشعل · ميكروفون · مستشعرات · بطارية · ترميز · Wi-Fi)، لا صندوق أدوات. أُعيدت الثلاثة إلى `Control` وحُذف القسم والاختبار والنصوص |
| **M1c** (منجزة ثم نسخها أمر تكملة ٨٥ — تاريخيّة) | `Logs` · `UsageStats` · `DeviceLab` → `Diagnostics` · `ColorScheme` · `MaxBackup` · `Permissions` → `Settings` · وإضافة قسم «أدوات التشخيص» الذي يقرأ أبناءه من السجلّ | أبناء `Control` **٢٢ ← ١٦** · العمق ٣٧/٢٤ ← **٣٤/٢٧** · `Settings` ٩ ← **١٢** · `Diagnostics` ٠ ← **٣** · صندوق الأدوات ١٠ ← **٦** · صفر عائلة مفصولة · حرّاس محدَّثون (`DestinationDoorTest` يقبل الباب المولَّد · `LaunchRouteTest` · `OwnerDecisionsGuardTest` · `ControlLayoutModelTest`) |
| **M2** (منجزة بنيويًا) | إعادة تركيب أعلى خمس شاشات حسب نوعها: `Now` (Matrix) · `Control` (Matrix) · `FpsOverlay` (Instrument) · `GameSessions` (Ledger) · `ThermalDetail` (Instrument) | كل شاشة تحمل بطاقة الشاشة في KDoc · عنصر حامل واحد لكل شاشة · لا صفّ يحمل منحنى — **مقيس:** البطاقات الخمس قائمة · حامل واحد لكل شاشة يحرسه `ScreenArchetypeTest` (النوع + استدعاء واحد + سبق الحامل للصفوف) · `GameSessions` صار حاملًا واحدًا يتبدّل مصدره بدل منحنيين · `Now` **لم يُعَد ترتيبه**: `HomeShapeContractTest` أسقط محاولة تقديم المنصة (تشغيل `43133186010`) فبقي ترتيب المالك · ترجمة `1f11d42e` **مُتحقّقة في CI** (الاختبارات جرت) والحكم البصري/اللمس/RTL **يحتاج جهازًا** |
| **M3** (منجزة بنيويًا) | سلوك تكيّفي وإتاحة: عمق اللمس · RTL · أحجام الشاشات · حالات الفراغ والخطأ | فحص `rtl_guard` + قائمة حالات لكل شاشة جديدة · لا نصّ يُقتطع في ٣٦٠dp — **مقيس:** `rtl_guard` صفر مخالفة (٤٦٢ ملفًا) · سطر `States:` في بطاقات الخمس كلها يحرسه `ScreenArchetypeTest` · صفر عرض ثابت > ٣٢٠dp في الخمس (حارس نصّي) · `CommandDeck` يقول حالته الفارغة بنصّ en/ar بدل صندوق صامت · **قصّ النصّ فعليًّا في ٣٦٠dp يحتاج جهازًا** |
| **M4** (منجزة بنيويًا) | تقليل الاستهلاك: عدد إعادات التركيب في الأسطح الحيّة · فواصل التحديث | قياس قبل/بعد لعدد إعادات التركيب في `FpsOverlay` و`GameSessions`، يُسجَّل في `HANDOFF` — **مقيس بنيويًّا:** الشاشتان لم تعودا تقرآن `HudLive.snapshot` على الجذر (صفر `collectAsState` لقطة، و`derivedStateOf` للقيم الثلاث/الاثنتين) فيحرسه `ScreenArchetypeTest` · الخدمة تمرّر `intervalMs` من التفضيلات أصلًا (`FpsOverlayService.kt:221` · `gate.updateInterval` :310) فلا مسار ثانٍ · المعاينة والحقول تُعاد تركيبهما كل عيّنة وهو مقصودهما · **عدّ إعادات التركيب نفسه يحتاج جهازًا** |
| **التهيئة (Onboarding)** (أُعيد تركيبها بأمر المالك) | صفحة واحدة للوصول: اختيار الوضع (تلقائي · Root · Shizuku · أساسي) + `PrivilegePanel` + GitHub بزر نجمة ودليل مصوّر + قناة تيلجرام بشارة «جديد»؛ وصفحة للأذونات (فوق التطبيقات · إشعارات · استخدام · بطارية) بحالة حيّة ومنح صريح | دمج الطلب: لا سطح امتياز ثانٍ (`PrivilegePanel`/`ExecutionModeStore` مُعادان) · الإنهاء داخل العملية بلا `am start -S` ولا غطاء `isFinalizing` (مقيس: `grep` = 0، وحارس `ScreenArchetypeTest`) · `MainActivity` تسمع تغيّر العلم · نصوص en/ar فقط · الشكل/اللمس/RTL يحتاج جهازًا · **«حول» تحمل المجتمع كله** (GitHub · القناة · جروب `@MaxManagerChat`) والجروب ليس في التهيئة، يحرسه `ScreenArchetypeTest` |
| **أدوات التحكّم** (نُقلت وحُذفت بأمر المالك) | `MaxBackup` · `Permissions/AppOps` · نظام الألوان · `DeviceLab` · `UsageStats` · `Logs` · وضع الشحن المصرفي · تنظيف التطبيقات → **`Control`** بأربعة نطاقات؛ `FileManager` → **`StorageHub`**؛ وحُذفت شاشات `Diagnostics` و`ModuleHealth` و`HmaCompanion` نهائيًّا | أبناء `Control` تستقبل الأدوات من سجلّها لا من قائمة ثانية · `control_tools_system` و`control_tools_appearance` في en/ar · صفر مرجع إلى الوجهات المحذوفة في الشيفرة أو الاختبارات (حارس `AtlasReadOnlyEntryTest` يشهد) · بطاقة النشاط تفتح إعداداتها من زرّ في رأسها · **بطاقة النشاط أُعيد تصميمها من الصفر (شريط حالة ثلاثي + نقطة حالة لكل سطر)، و«إعادة كل شيء إلى الافتراضي» تغطّي اختيارات التطبيق لا `persist.sys.*` ولا بيانات المستخدم** · الشكل/اللمس/RTL يحتاج جهازًا |

**ولا تُنفَّذ مرحلة فوق مرحلة لم تُقبل.** فـM1 تغيّر مسارات، وM2 تغيّر تخطيطات — وخلطهما يجعل
الفشل بلا سبب واحد.

## ٦. ما لا يُلمس في أي مرحلة

- مسار الكتابة الواحد (المحكّم) و`ADR-11`: لا كتابة `sysfs` من `ui/**`.
- ترتيب الأولويات `SAFETY > APP PROFILE > MAX AI > MANUAL` — إعادة التركيب لا تعيد ترتيب السلطة.
- المحرّك الحراري والعتاد والإقلاع: أي تغيير فيها يبقى `يحتاج جهازًا` ولا يُغلق ببناء ناجح.
- الميزات المخفية بأمر المالك تبقى في السجلّ ولا تعود إلى أي قائمة.

## ٧. حدود هذه الوثيقة

- الحصر **بنَيَوي**: يقرأ التسجيلات في الكود، لا ما يظهر فعلًا على الشاشة. فشاشة قد تكون مسجّلة
  ولا يصلها زرّ — والعكس. وهذا يبقى **يحتاج جهازًا**.
- الأنواع (Archetype) إسناد تصميمي بيد، لا قياس آلي. تُراجع في M2 شاشةً شاشة.
- أرقام الاستهلاك (M4) غير مقيسة بعد — ولا يُكتب لها رقم قبل قياس.
- `٢طلبي.txt` و`طلبي.txt` قُرئا كتوجيه؛ ما فيهما من مراجع خارجية لم يُنقل منه كود، ولا يُسمّى
  مرجعٌ فيهما في هذه الوثيقة (§0.4).
