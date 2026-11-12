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

## ٣. الشجرة كاملة — ٦٩ وجهة بترتيب التطبيق

| # | الشاشة | المسار | الأب | العمق | النوع | ملاحظة مقيسة |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Now | `now` | — | 0 | Matrix |  |
| 2 | Apps | `apps` | — | 0 | Library |  |
| 3 | DebloatFreeze | `debloatfreeze` | Apps | 1 | Library |  |
| 4 | AppSettings | `app_settings/{pkg}` | Apps | 1 | Console |  |
| 5 | GameLobby | `gamelobby` | Apps | 1 | Library |  |
| 6 | EmulatorHub | `emulatorhub` | Apps | 1 | Library |  |
| 7 | Control | `control` | — | 0 | Matrix | **١٦ ابنًا مباشرًا** — ١٠ محاور و٦ أسطح بلا محور |
| 8 | CpuHub | `hub_cpu` | Control | 1 | Console |  |
| 9 | CpuCoreControl | `cpucorecontrol` | CpuHub | 2 | Matrix |  |
| 10 | GovernorSettings | `governorsettings` | CpuHub | 2 | Console |  |
| 11 | PreferenceTweaks | `preferenced` | CpuHub | 2 | Console |  |
| 12 | GpuHub | `hub_gpu` | Control | 1 | Console |  |
| 13 | GpuStudio | `gpustudio` | GpuHub | 2 | Studio |  |
| 14 | MemoryHub | `hub_memory` | Control | 1 | Console |  |
| 15 | ZramManager | `zrammanager` | MemoryHub | 2 | Instrument |  |
| 16 | MemoryUfs | `memory_ufs` | MemoryHub | 2 | Matrix | `MaxRisk.Advanced` |
| 17 | DisplayHub | `hub_display` | Control | 1 | Console |  |
| 18 | DisplayStudio | `displaystudio` | DisplayHub | 2 | Studio |  |
| 19 | Resolution | `resolutionscreen` | DisplayHub | 2 | Console |  |
| 20 | ResponsivenessHub | `hub_responsiveness` | Control | 1 | Console |  |
| 21 | TouchBoost | `touchboost` | ResponsivenessHub | 2 | Console | مخفي بأمر المالك — يبقى في السجلّ ولا يُعرض · مخفي |
| 22 | FpsGo | `fpsgoscreen` | ResponsivenessHub | 2 | Console |  |
| 23 | Fas | `FasScreen` | ResponsivenessHub | 2 | Console |  |
| 24 | ThermalHub | `hub_thermal` | Control | 1 | Console |  |
| 25 | ThermalDetail | `thermal_detail` | ThermalHub | 2 | Instrument |  |
| 26 | PowerHub | `hub_power` | Control | 1 | Console |  |
| 27 | Charging | `chargingscreen` | PowerHub | 2 | Instrument |  |
| 28 | BypassCharging | `bypasschg` | PowerHub | 2 | Console |  |
| 29 | BypassChargingCheck | `bypasschg_check` | PowerHub | 2 | Flow |  |
| 30 | DozeMode | `dozemode` | PowerHub | 2 | Console |  |
| 31 | StorageHub | `hub_storage` | Control | 1 | Console |  |
| 32 | Dex2oat | `dex2oat` | StorageHub | 2 | Console |  |
| 33 | StorageDetail | `storage_detail` | StorageHub | 2 | Matrix |  |
| 34 | StorageCare | `storage_care` | StorageHub | 2 | Console | `MaxRisk.Advanced` |
| 35 | NetworkHub | `hub_network` | Control | 1 | Console |  |
| 36 | NetworkScheduler | `networkscheduler` | NetworkHub | 2 | Console |  |
| 37 | NetworkDetail | `network_detail` | NetworkHub | 2 | Instrument |  |
| 38 | AudioHub | `hub_audio` | Control | 1 | Console |  |
| 39 | AudioStudio | `audio_studio` | AudioHub | 2 | Studio |  |
| 40 | FpsOverlay | `fpsoverlay` | Control | 1 | Instrument |  |
| 41 | GameSessions | `gamesessions` | Control | 1 | Ledger |  |
| 42 | DynamicIsland | `dynamic_island` | Control | 1 | Studio |  |
| 43 | ProcessManager | `processmanager` | MemoryHub | 2 | Library | قائمة عمليات حيّة — **نُقلت في M1a** إلى محور الذاكرة |
| 44 | DeviceLab | `device_lab` | Diagnostics | 2 | Matrix | **اختبار عتاد خالص**: شاشة · لمس · أزرار · صوت · مستشعرات · بطارية · شبكة — **نُقل في M1c** إلى التشخيص |
| 45 | UsageStats | `usage_stats` | Diagnostics | 2 | Ledger | إحصاء الاستخدام — **نُقل في M1c** إلى التشخيص مع السجلّ والمختبر |
| 46 | BatteryHistory | `battery_history` | PowerHub | 2 | Ledger | تاريخ الطاقة — **نُقلت في M1a** إلى محور الطاقة |
| 47 | ColorScheme | `colorscheme` | Settings | 1 | Console | **نُقل في M1c** بجوار `ColorPalette` — العائلة صارت في مكان واحد |
| 48 | Logs | `logsviewer` | Diagnostics | 2 | Ledger | **نُقل في M1c** إلى التشخيص — العائلة صارت في مكان واحد |
| 49 | MaxBackup | `max_backup?pkg={pkg}` | Settings | 1 | Flow | **نُقل في M1c** بجوار `ConfigBackup` · `MaxRisk.Advanced` |
| 50 | Permissions | `max_perms?pkg={pkg}` | Settings | 1 | Library | **نُقل في M1c** بجوار `Privilege` · `MaxRisk.Advanced` |
| 51 | SetEdit | `setedit` | Control | 1 | Console | محرّر إعدادات — أداة متقدّمة بلا محور · `MaxRisk.Advanced` |
| 52 | ActivityLauncher | `activitylauncher` | Control | 1 | Library | مشغّل أنشطة — أداة متقدّمة بلا محور · `MaxRisk.Advanced` |
| 53 | FileManager | `filemanager` | Control | 1 | Library | مدير ملفات — أداة متقدّمة بلا محور · `MaxRisk.Advanced` |
| 54 | Settings | `settings` | — | 0 | Library |  |
| 55 | Diagnostics | `diagnostics` | Settings | 1 | Matrix |  |
| 56 | License | `license` | Settings | 1 | Flow |  |
| 57 | ColorPalette | `color_palette` | Settings | 1 | Studio |  |
| 58 | ConfigBackup | `config_backup` | Settings | 1 | Flow |  |
| 59 | CommunityStats | `community_stats` | Settings | 1 | Matrix | مخفي بأمر المالك · مخفي |
| 60 | Plugins | `plugins` | Settings | 1 | Library | مخفي بأمر المالك · مخفي |
| 61 | About | `aboutscreen` | Settings | 1 | Flow |  |
| 62 | Privilege | `privilege` | Settings | 1 | Console |  |
| 63 | ModuleHealth | `module_health` | Settings | 1 | Instrument |  |
| 64 | MaxAi | `max_ai` | — | 0 | Console | جذر غير أساسي — لا تبويب له في الشريط السفلي |
| 65 | MaxLive | `max_live` | MaxAi | 1 | Instrument |  |
| 66 | MaxAiIntro | `max_ai_intro` | MaxAi | 1 | Flow |  |
| 67 | DeviceInfo | `device_info?section={section}` | — | 0 | Matrix | جذر غير أساسي بوسيط `{section}` |
| 68 | GetStarted | `get_started` | — | 0 | Flow | جذر غير أساسي |
| 69 | HmaCompanion | `hma_companion` | — | 0 | Flow | جذر غير أساسي (parent = null) |

## ٤. العيوب النظامية — خمسة، كلٌّ بعدد

### ع١ — `Control` صار مخزنًا لا محورًا
**١٦ ابنًا مباشرًا** من ٦٩ وجهة (٢٣٪) بعد M1a وM1c. عشرة منها محاور (`CpuHub` … `AudioHub`) وستّة
أسطح لا تنتمي إلى محور: `FpsOverlay` · `GameSessions` · `DynamicIsland` · `SetEdit` ·
`ActivityLauncher` · `FileManager`.
**الأثر:** كانت ٢٤ قبل الجولتين. والستّة الباقية ليست تنوّعًا سائبًا: ثلاثة أسطح حيّة تُرى فوق
التطبيقات، وثلاثة أدوات متقدّمة لا محور لها. ومحاولة تفريغها بإحالة الأدوات إلى `DeviceLab`
**رُفضت بأمر المالك**: المختبر اختبار عتاد لا صندوق أدوات.
**وما تحسّن بلا نقل:** صندوق الأدوات صار **نطاقات معنونة** بدل صفوف متتالية بلا جامع، ثمّ قصُر
إلى نطاقين بعد M1c (`control_tools_live` · `control_tools_advanced`). والعيب **انخفض انخفاضًا
مقيسًا (٢٤ ← ١٦) ولم ينتهِ**؛ وما بقي محكوم بأن الثلاثة المتقدّمة أداة بلا محور، لا بأن أحدًا نسيها.

### ع٢ — العائلة الواحدة مفصولة على أبين
- ~~الطاقة~~: أُعيدت في **M1a** — `BatteryHistory` صارت تحت `PowerHub` مع `Charging` · `BypassCharging` · `BypassChargingCheck` · `DozeMode`.
- ~~العمليات~~: أُعيدت في **M1a** — `ProcessManager` صارت تحت `MemoryHub` مع `ZramManager`/`MemoryUfs`.
- ~~السجلّات والتشخيص~~: أُعيدت في **M1c** — `Logs` · `UsageStats` · `DeviceLab` صارت تحت `Diagnostics`، والشاشة تعرض أبناءها من السجلّ عبر `maxHubRows`.
- ~~اللون~~: أُعيدت في **M1c** — `ColorScheme` صارت تحت `Settings` بجوار `ColorPalette`.
- ~~النسخ والأذونات~~: أُعيدت في **M1c** — `MaxBackup` بجوار `ConfigBackup`، و`Permissions` بجوار `Privilege`.
**الأثر بعد M1c:** **العائلات الأربع كلها في مكان واحد**، وصفر عائلة مفصولة على أبين. وهذا كان معيار قبول M1.

### ع٣ — أربعة جذور بلا باب
`MaxAi` · `DeviceInfo` · `GetStarted` · `HmaCompanion` جذور كاملة (parent = null) لكن الشريط
السفلي يحمل أربعة تبويبات فقط. أي أن **نصف الجذور** يُبلغ إليها بمسار أو زرّ داخل شاشة، لا بموقع
ثابت. `MaxAi` خصوصًا — وهو أعلى أولوية بعد السلامة وملفّات التطبيقات — ليس له باب دائم.

### ع٤ — العمق يتوقّف عند ٢ والكثافة تنفجر في ١
٣٧ شاشة في العمق ١ مقابل ٢٤ في العمق ٢، وصفر في العمق ٣. فالشجرة ليست عميقة — بل **عريضة عند
المحاور ومسطّحة تحتها**. والنتيجة أن `Control` يحمل العبء كله (ع١) بينما المحاور أوراق بلا أبناء
إلا في ثمانية مواضع.

### ع٥ — حالة خاصة بلا موضع مراجعة
`hidden` (٣) و`MaxRisk.Advanced` (٧) ووسائط المسار (٤) = **١٤ وجهة من ٦٩ (٢٠٪)** تحمل حالة
استثنائية. لا شاشة واحدة تجمعها للمراجعة، ولا بوابة تمنع ظهور مخفيّة في قائمة جديدة. الحالة
موجودة في الكود ومتفرّقة في السلوك.

## ٥. خريطة الهجرة — M0 إلى M4 + تسجيل الجلسات

كل مرحلة تُقبل أو تُرفض بعدد، لا برأي. وما لم تُقَس مرحلته لا تُعلَن منجزة.

| المرحلة | التغيير | معيار القبول المقيس |
| --- | --- | --- |
| **M0** (هذه) | هذه الخريطة + أنواع الشاشات في `DESIGN.md` + حارس الأبواب `DestinationDoorTest` | ٦٩ وجهة في الجدول · صفر وجهة بلا نوع · صفر وجهة بلا أب معروف · **صفر وجهة مرئية بلا باب** (الاختبار يسقط إن نُقلت وجهة إلى أبٍ لا يذكرها) |
| **M1** (منجزة على مرحلتين) | تفكيك `Control`: المحاور العشرة + الأسطح الحيّة (`FpsOverlay` · `GameSessions` · `DynamicIsland`) + ثلاثة أدوات متقدّمة بلا محور؛ والعائلات إلى محاورها | أبناء `Control` **١٦** (الهدف ≤ ١٣ لم يُبلَغ، والسبب مُعلَن: الثلاثة أداة بلا محور) · **صفر عائلة مفصولة** ✓ · ٦٩ وجهة كما هي ✓ |
| **M1a** (منجزة) | نقل العائلتين اللتين أثبت القياس أن بابها الجديد يعرض أبناءه من السجلّ: `BatteryHistory→PowerHub` · `ProcessManager→MemoryHub` | أبناء `Control` **٢٤ ← ٢٢** · العمق ٣٩/٢٢ ← **٣٧/٢٤** · صفر تكرار في صفحة التحكّم (حُذف الاثنان من قائمتيها) |
| **M1b** (**مرفوضة بأمر المالك**) | كانت: `SetEdit` · `ActivityLauncher` · `FileManager` → `DeviceLab` وقسم «أدوات المختبر» يقرأها من السجلّ | **السبب المقيس:** `DeviceLabScreen` اختبار عتاد (شاشة · لمس · أزرار · اهتزاز · صوت · مشعل · ميكروفون · مستشعرات · بطارية · ترميز · Wi-Fi)، لا صندوق أدوات. أُعيدت الثلاثة إلى `Control` وحُذف القسم والاختبار والنصوص |
| **M1c** (منجزة بأمر المالك) | `Logs` · `UsageStats` · `DeviceLab` → `Diagnostics` · `ColorScheme` · `MaxBackup` · `Permissions` → `Settings` · وإضافة قسم «أدوات التشخيص» الذي يقرأ أبناءه من السجلّ | أبناء `Control` **٢٢ ← ١٦** · العمق ٣٧/٢٤ ← **٣٤/٢٧** · `Settings` ٩ ← **١٢** · `Diagnostics` ٠ ← **٣** · صندوق الأدوات ١٠ ← **٦** · صفر عائلة مفصولة · حرّاس محدَّثون (`DestinationDoorTest` يقبل الباب المولَّد · `LaunchRouteTest` · `OwnerDecisionsGuardTest` · `ControlLayoutModelTest`) |
| **M2** (منجزة بنيويًا) | إعادة تركيب أعلى خمس شاشات حسب نوعها: `Now` (Matrix) · `Control` (Matrix) · `FpsOverlay` (Instrument) · `GameSessions` (Ledger) · `ThermalDetail` (Instrument) | كل شاشة تحمل بطاقة الشاشة في KDoc · عنصر حامل واحد لكل شاشة · لا صفّ يحمل منحنى — **مقيس:** البطاقات الخمس قائمة · حامل واحد لكل شاشة يحرسه `ScreenArchetypeTest` (النوع + استدعاء واحد + سبق الحامل للصفوف) · `GameSessions` صار حاملًا واحدًا يتبدّل مصدره بدل منحنيين · `Now` **لم يُعَد ترتيبه**: `HomeShapeContractTest` أسقط محاولة تقديم المنصة (تشغيل `43133186010`) فبقي ترتيب المالك · ترجمة `1f11d42e` **مُتحقّقة في CI** (الاختبارات جرت) والحكم البصري/اللمس/RTL **يحتاج جهازًا** |
| **M3** (منجزة بنيويًا) | سلوك تكيّفي وإتاحة: عمق اللمس · RTL · أحجام الشاشات · حالات الفراغ والخطأ | فحص `rtl_guard` + قائمة حالات لكل شاشة جديدة · لا نصّ يُقتطع في ٣٦٠dp — **مقيس:** `rtl_guard` صفر مخالفة (٤٦٢ ملفًا) · سطر `States:` في بطاقات الخمس كلها يحرسه `ScreenArchetypeTest` · صفر عرض ثابت > ٣٢٠dp في الخمس (حارس نصّي) · `CommandDeck` يقول حالته الفارغة بنصّ en/ar بدل صندوق صامت · **قصّ النصّ فعليًّا في ٣٦٠dp يحتاج جهازًا** |
| **M4** (منجزة بنيويًا) | تقليل الاستهلاك: عدد إعادات التركيب في الأسطح الحيّة · فواصل التحديث | قياس قبل/بعد لعدد إعادات التركيب في `FpsOverlay` و`GameSessions`، يُسجَّل في `HANDOFF` — **مقيس بنيويًّا:** الشاشتان لم تعودا تقرآن `HudLive.snapshot` على الجذر (صفر `collectAsState` لقطة، و`derivedStateOf` للقيم الثلاث/الاثنتين) فيحرسه `ScreenArchetypeTest` · الخدمة تمرّر `intervalMs` من التفضيلات أصلًا (`FpsOverlayService.kt:221` · `gate.updateInterval` :310) فلا مسار ثانٍ · المعاينة والحقول تُعاد تركيبهما كل عيّنة وهو مقصودهما · **عدّ إعادات التركيب نفسه يحتاج جهازًا** |

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
