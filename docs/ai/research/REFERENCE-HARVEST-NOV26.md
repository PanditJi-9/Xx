# حصاد المراجع — DevCheck · FKM · ZenBubble · HyperCool

**التاريخ:** 2026-11-05 · **المرجع الملزم:** `docs/ai/FEATURE_MAPPING.md` (٦٠ صفًّا) و`REFERENCE-EXTRACTION-NOV26.md` (الفجوات العشر).
هذا الملفّ **يوسّع** الخريطة القائمة ولا يستبدلها: كل ما هنا إمّا **صفّ جديد** لم يُقيَّم، أو **تفصيل تنفيذيّ** لصفّ قائم.

## ٠) الحدود والمنهج — تُقرأ قبل أي بند

- **لا كود ولا أصول ولا نصوص منقولة.** DevCheck وFKM وZenBubble **ملكية خاصة، كل الحقوق محفوظة**، وHyperCool **GPL-3**. ما يلي **وصف آليّات وأنماط** مقابل `path:line`، ثم إعادة بناء نظيفة بـKotlin/Compose من SDK العام. لا صفّ اعتماد جديد في `THIRD_PARTY_NOTICES` لأنّ لا كود نُقل (ADR-77).
- **المصادر المقيسة:** `src-devcheck/sources` (٣٬٧٥٦ ملفًا)، `src-fkm/sources` (٣٬٨٤٥)، `src-zenbubble/sources` (١٩٬٢٩٨، أكثره مكتبات)، `hypercool/` (٣٧ ملفًا)؛ الموارد والمانيفستات في `/tmp/opencode/ref-res/*`؛ السلاسل الإنجليزية المفصولة في `/tmp/opencode/{devcheck,fkm}.strings.tsv` (١٥٨٣ و٩٠١ صفًّا بعد نزع مكتبات AndroidX/Material).
- **حدّ الصدق:** ما لم يُشغَّل على جهاز يبقى «غير مُتحقَّق على العتاد»؛ وما لم يُصرَّف في CI يبقى «غير مُتحقَّق ترجمةً». الأرقام هنا **مقيسة من الشجرة**، لا مُدّعاة.

## ١) لقطة مقيسة

| التطبيق | الحزمة | الأنشطة | الخدمات | المستقبلات | ما يهمّنا |
|---|---|---:|---:|---:|---|
| DevCheck | `flar2.devcheck` | ٥٨ | ٢٠ (١٢ مراقبًا) | ١٢ | مراقبات عائمة · اختبارات عتاد · تاريخ بطارية · إحصاء استخدام/شبكة · مخططات |
| FKM | `com.franco.kernel` | ٢٨ | ١٩ | ١٢ | توابل كمخطط بيانات · ملفات لكل تطبيق · بطارية · wakelocks/LMK/stune · بلاطات |
| ZenBubble | `com.droidbubble.app` | ٣ | ٤ | ٣ | آيلاند: أولوية، مؤقّتات، لمس، قطة، خصوصية |
| HyperCool | وحدة Magisk | — | — | — | حارس إقلاع · لقطة إقلاع · keepers · تأخير تطبيق |

---

## ٢) DevCheck — ما يستحقّ الأخذ

### ٢.١ الشاشات والأدوات (٥٨ نشاطًا)
- **تشخيص الجهاز:** `MyDeviceActivity`، `SensorActivity`، `ManifestActivity` (عارض مانيفست)، `PermissionsSummaryActivity`، `VulkanInfoActivity`، `OpenGLInfoActivity`، `CPUTimeActivity`، `GpuTimeActivity`.
- **أدوات:** `ToolsActivity`، `BluetoothActivity`، `USBActivity`، `WifiActivity`، `GpsActivity`، `CPUActivity`، `LogcatReaderActivity`، `TaskManagerActivity`، `TaskManager2Activity`، `SystemGraphsActivity`، `NetworkMapperActivity`.
- **بطارية/استخدام:** `BatteryActivity`، `BatteryMonitorService`، `NetworkUsageActivity`، `ScreentimeActivity`.
- **اختبارات عتاد (١٤):** Screen · MultiTouch · Button · Vib · Speakers · Earpiece · Mic · Flashlight · Proximity · Accelerometer · LightSensor · Headset · Brightness · Charging.
- **مراقبات عائمة (١٢ خدمة):** CPU · Temp · GPU · Load · Battery · Ram · Current · Network · Signal · FPS · GameFps · CustomMonitor. كلها `MonitorWindow` مستقلة، مع `MonitorActivity` (١٦٨٩ سطرًا) و`CustomMonitorBuilderActivity` (٢٨٧) و`PauseResumeReceiver`.
- **ودجات:** DashWidget · BatteryWidget · BarDetailWidget، ولكلٍّ `ConfigureActivity`.
- **مرفوض:** Paywall/Translate/Announcements/BenchmarkSuite (ترتيب تنافسي) — لا يُنقل.

### ٢.٢ أنماط UI/UX المستخلصة
1. **بطاقة القيمة الحيّة:** عنوان + قيمة أحادية المسافة + وحدة + مصدر/شارة امتياز + آخر تحديث. الشارة تجيب «من أين جاء الرقم؟» قبل أن يسأل المستخدم.
2. **لوحة مراقبة قابلة للتخصيص:** المراقب وحدة مستقلة (نافذة عائمة) لا قسمًا داخل شاشة؛ الموضع والحجم والمظهر يُحفظان، والإيقاف/الاستئناف عامّ.
3. **مخططات نظام بتبويبات مجال** (CPU/GPU/RAM/ZRAM/تخزين/حراري/شبكة/أنظمة فرعية) ونوافذ زمنية، لا رسم واحد مزدحم.
4. **اختبار تفاعلي كامل الشاشة:** تعليمات قصيرة، نتيجة فورية، زر إنهاء واضح، ولا حفظ لنتيجة إن لم تُطلب.
5. **فهرس أدوات مُصنَّف** (اختبار/شبكة/حساس/نظام) بدل قائمة مسطّحة طويلة.
6. **حالة الصلاحية ككائن** لا كنصّ: `Public/Shizuku/Root` مع تفسير ما يفقده المستخدم عند كل طبقة.
7. **«لا بيانات» ليست خطأً:** تمييز Empty عن Error عن Unsupported، مع إجراء تالٍ.

### ٢.٣ تقنيات
- `UsageStatsManager.queryAndAggregateUsageStats` + `AppOpsManager.OPSTR_GET_USAGE_STATS` + `ACTION_USAGE_ACCESS_SETTINGS` (نُفِّذ عندنا الآن).
- `NetworkStatsManager` لإحصاء كل تطبيق (لم يُنقل بعد).
- `BatteryManager` + مستقبل `ACTION_BATTERY_CHANGED` + تخزين تاريخي (SQLite/Room) لعيّنات البطارية.
- `CameraManager.setTorchMode` للفلاش، `VibratorManager`/`VibratorEffect` للاهتزاز، `AudioTrack` للنغمة، `SensorManager` للمستشعرات (نُفِّذت كلها في Device Lab).
- `UsageStats`/`NetworkStats` تحتاج إذن وصول خاص لا جذر — نمط «امنح مرّة، واشرح لماذا».
- مراقب عائم = خدمة أمامية + `TYPE_APPLICATION_OVERLAY` + حالة موضع محفوظة + إيقاف تلقائي عند انخفاض البطارية.
- Socket/HTTP لاختبار الشبكة (latency/jitter/DNS) — يحتاج قياسًا لا تخمينًا.

### ٢.٤ فجواتنا مقابل DevCheck
| المجال | حالتنا | الفجوة |
|---|---|---|
| اختبارات العتاد | **Device Lab shipped** (شاشة/لمس/أزرار/اهتزاز/سماعة/فلاش/مستشعرات/بطارية) | لا ميكروفون/سماعة أذن/Bluetooth/USB/GPS/Brightness/Charging التفصيلي |
| استخدام التطبيقات | **مشحون**: زمن لكل تطبيق، Wi-Fi/بيانات لكل تطبيق، ورسم يومي لآخر ٧ أيام باتحاد الأحداث وتقسيمها عند منتصف الليل المحلي | لا رسم ٣٠ يومًا أو مقارنة أسابيع متعددة؛ أحداث Android قد تكون ناقصة والمقياس نسبي |
| تاريخ البطارية | **مشحون**: عيّنات تاريخية، استنزاف مضيء/مطفأ مفصولًا، تصدير CSV، و`ChargeLedger` بجلسات و`verdict` معروض في التشخيص | لا مقارنة بين دورات شحن متعددة في رسم واحد |
| مراقبات عائمة | HUD داخل اللعب فقط | لا مراقب مستقل قابل للسحب والتخصيص |
| مخططات النظام | بطاقات اتجاه في الرئيسية | لا شاشة مخططات بنوافذ زمنية وتصدير |
| عارض المانيفست/الأذونات | **مشحون لحزمة التطبيق نفسه** (`ManifestFacts`)؛ و`QUERY_ALL_PACKAGES` **مضاف** في المانيفست (سطر ٨٢) | لا واجهة تعرض مانيفست **حِزم أخرى** بعد، رغم توفّر الإذن |
| Vulkan/OpenGL | **مشحون**: إصدار GLES يُقرأ من `dumpsys SurfaceFlinger`، ونسخة Vulkan ومستواها من `getSystemAvailableFeatures()`، وتُعرض بمصادرها | `DeviceInfoCollect.kt` (`glEsVersion` · `readVulkanVersion` · `readVulkanLevel`) · `DeviceInfoFacts.kt:170/188/189` |
| إحصاء الشبكة | **مشحون**: استهلاك Wi-Fi/بيانات لكل تطبيق داخل Usage Stats، وخريطة قنوات Wi-Fi في Device Lab | لا خريطة حرارية لتغطية الشبكات |

---

## ٣) FKM — ما يستحقّ الأخذ

### ٣.١ الشاشات (٢٨ نشاطًا)
`Main` · `GovernorProfiles` · `Settings` · `Help` · `Voltages` · `SocTunables` · `DisplayProfiles` · `NightShift` · `HighBrightnessMode` · `EditPerAppProfile` · `CoreControl` · `SystemMonitor` · `CustomKernelSettingsEdit` · `BatteryMonitor` · `WakelockBlockers` · `ApplicationsListPerAppProfiles` · `LowMemoryKiller` · `StuneBoost` · `Ged` · `Ppm` · `Klapse/Klapse2` · `JsonSchemaGenerator` · `JsonDocs` · `OpenJsonUrl` · `CustomTunablesFileManager`.
**خدمات:** `ForegroundAppDetectionService` (مفتاح الملفات التلقائية)، `CpuTempService`، `AmbientLightSensorService`، `NightShiftService`، `NewBatteryMonitorService`، `FlashNotifService`، `PerfMon`، وبلاطات NightShift/HBM/KLapse/Script1–3/PerfMon.

### ٣.٢ أنماط UI/UX
1. **التوابل كمخطط (schema) لا كشاشات مكتوبة بيد:** كل عقدة تُعلن نوعها ومداها ووحدتها ووصفها، والواجهة تُولد منها. هذا يجعل إضافة جهاز جديد **بيانات** لا كودًا.
2. **وضع متقدّم:** إظهار كل الخيارات بلا شروح لمن يعرف، مع بقاء الوضع البسيط افتراضيًا.
3. **ملف لكل تطبيق:** اختيار التطبيق ← ملف (حفظ/توازن/أداء) ← تطبيق تلقائي عند المقدّمة ← رجوع عند الخروج. `ForegroundAppDetectionService` هو المحرّك.
4. **تأكيد الكتابة والتحقق:** «طُبِّق» لا تُقال إلا بعد قراءة القيمة مرة أخرى؛ وفشل جزئي يُسمّى.
5. **بطارية بلغة المستخدم:** active drain · idle drain · شحن/تفريغ · إشعار دوري.
6. **بلاطات سريعة للأفعال المتكرّرة** لا لكل إعداد.
7. **مستندات مولّدة من المخطط** (`JsonDocs`/`JsonSchemaGenerator`) — مرجع القدرات بدل وصف يدوي يتقادم.

### ٣.٣ تقنيات
- `JsonSchemaGenerator` + `JsonDocs`: مخطط ← JSON ← مستند؛ نفس فكرة `control_catalog.json` عندنا، لكن مع **مخرج مستندات**.
- `libsu`/جذر موحّد، `DataStore` + `EventBus`، `WorkManager` للمراقبة الدورية، receivers للإقلاع/الوضع الليلي.
- `ForegroundAppDetectionService` = UsageStats/Accessibility حسب المتاح، مع احتياط.
- كتابة sysfs مع تحقق قراءة-بعد-كتابة (نملكه في `WriteVerification`/`WriteReceipt`).
- توابل خاصة بالـSoC (GED/PPM/KLapse/فولتية) تُكتشف ولا تُفترض.

### ٣.٤ فجواتنا مقابل FKM
| المجال | حالتنا | الفجوة |
|---|---|---|
| مخطط التوابل | `control_catalog.json` (٤١ مدخلًا/٦٩ مرشّحًا) | **شُحنت المستندات** (`docs/CAPABILITIES.md`) و**شُحن JSON Schema** (`docs/control-catalog.schema.json` من `tools/gen_catalog_schema.py`)؛ والفجوة الباقية بعدها: عرض «كل ما تعلنه الحزمة من features» كقائمة داخل شاشة قائمة (§٩) |
| ملفات لكل تطبيق | **عامّ ومشحون لأي حزمة**: `AppMonitor.applyPerAppConfig` (سطر ٢٢٥١) و`revertPerAppConfig` (٣١٤٨) عبر `PerAppControlRegistry` (٢٨١ سطرًا، يمرّ بالـarbiter)، و`AppSettingsScreen` تُفتح من قائمة التطبيقات لأي حزمة، و`PerAppSessionRules.hasExplicitOverride` يمنع جلسة بلا إعداد صريح | الفارق: المُطبِّق بلا جذر `InAppProfileApplier` يقبل ثلاثة مقابض فقط (`InAppKnob {DND, REFRESH_RATE, HAPTICS}`) ولحزم مكتبة الألعاب وحدها |
| البطارية | بطاقة صحة + سجل شحن | لا مراقب تاريخي/إشعار active-idle |
| wakelocks/LMK/stune/PPM | غير موجودة | تحتاج جذرًا ومسارات مُكتشفة؛ ليست واجهة عامة |
| البلاطات | ٣ أنواع | لا NightShift/HBM/مراقبة/ملف لعب/تجميد |
| مستندات القدرات | لا | الأولوية التالية بعد البطارية |
| النسخ/الاستعادة | `MaxBackup` | لا تصدير ملف جهاز بمخطط إصدار ومقارنة قبل التطبيق |

---

## ٤) ZenBubble — ما يستحقّ الأخذ

### ٤.١ المكوّنات
`MainActivity` واحد + `IslandOverlayService` (الآيلاند) + `ScreenRecordingService` + `NotificationReceiverService` (مُصدَّر) + `IslandAccessibilityService` (مُصدَّر) + `BootReceiver` + `PowerConnectionReceiver`.
**لا شاشات إعدادات كثيرة:** الإعدادات داخل MainActivity/Compose، والنصّ غالبًا مضمّن لا في الموارد — لذلك الخريطة من الشيفرة والمانيفست لا من `strings.xml`.

### ٤.٢ أنماط UI/UX
1. **آلة حالة واحدة للآيلاند** بأولوية صريحة: مكالمة ← وسائط ← فلاش ← تقدّم ← تسجيل شاشة ← إشعار منبثق ← خمول. لا تعرض حدثين متسابقين.
2. **مؤقّتات عابرة** (١٫٥/٣/٤٫٥ ث) تُعاد ضبطها بالحدث، وتُلغى عند الأولوية الأعلى.
3. **منطقة لمس أوسع من الرسم** مع تمرير اللمس خارج الشكل، ومنع تعارض مع شريط الحالة.
4. **أوضاع القطة/الثقب** (teardrop/punch/notch) بمعايرة موضع، لا إحداثيات ثابتة.
5. **إخفاء المحتوى الحسّاس** في الإشعارات (نصّ بديل) خيارًا لا افتراضًا.
6. **مؤشّر تسجيل شاشة** ثابت مع إيقاف من الآيلاند نفسه.
7. **سلوك الشحن/فصل الشاحن** كحدث مرئي قصير لا إشعار دائم.

### ٤.٣ تقنيات
- `StateFlow` + sealed state للآيلاند، `Overlay` بنافذة `TYPE_APPLICATION_OVERLAY`، `WindowInsets`/cutout APIs، `AccessibilityService` للمسّ الموسّع، `NotificationListenerService` لالتقاط المحتوى، `MediaSessionManager` للوسائط، `MediaProjection` للتسجيل.
- إعدادات عبر repository واحد؛ استعادة على الإقلاع.
- **لا تليمتري**: أي مدير «إحصاءات» في المرجع يُقرأ كفكرة ولا يُنقل كسلوك.

### ٤.٤ فجواتنا مقابل ZenBubble
| المجال | حالتنا | الفجوة |
|---|---|---|
| الآيلاند | ١٩ ملفًا مبنيًا على Islandify (MIT) | أولوية/مؤقّتات/قطة تحتاج مقارنة سلوكية على جهاز |
| القطة | إعداد موجود | لا معايرة لكل جهاز، لا أوضاع notch/teardrop صريحة |
| اللمس | إدارة لمس قائمة | لا منطقة لمس أوسع من الرسم ولا اختبار تعارض |
| الخصوصية | لا | لا إخفاء محتوى الإشعارات الحسّاس في الآيلاند |
| تسجيل الشاشة | خدمة تسجيل موجودة | لا مؤشّر آيلاند بإيقاف فوري |
| الشحن | شاشة شحن | لا حدث آيلاند قصير عند الوصل/الفصل |

---

## ٥) HyperCool — ما يستحقّ الأخذ

### ٥.١ المكوّنات المقيسة
١١٠ مفتاحًا في `default.conf` (شبكة/طاقة/حرارة/ذاكرة/تخزين/تطبيقات/نظام)، `service.sh` و`post-fs-data.sh`، `webroot/`، لقطة إقلاع `HC_BOOTCFG`، حارس `boot_tries`، `apply_delay` ١٠–١٢٠ ث، keepers (شحن/شاشة/كاش/CPU/معدّل تحديث)، واستعادة عند التعطيل.
**تفاصيلها في `HYPERCOOL-STUDY.md`**؛ لا نعيدها هنا.

### ٥.٢ ما يُنقل كفكرة
1. **حارس إقلاع بعدّاد ومحاولة تراجع** — عندنا `BootLoopGuard` في `PersistStore`؛ الفجوة هي **سطح استعادة** يشرح آخر لقطة ويسمح بالرجوع قبل الإقلاع التالي. **وقد شُحن:** السطح يعرض آخر جولة وعدّاد الإقلاعات السريعة، والرجوع الفعليّ عن آخر تطبيق عبر `PersistSnapshot` (§٨ · §٩).
2. **لقطة إقلاع** لقيَم محدّدة قبل التطبيق، لا نسخ ملفات نظام.
3. **تأخير تطبيق** بعد الإقلاع لأنّ الخدمات لم تجهز — يجب أن يكون قرارًا معلنًا لا صامتًا.
4. **keepers بحدود**: شحن/تنظيف كاش عند الشاشة المطفأة/معدّل تحديث — كلها عبر arbiter وليس كتابة مباشرة.
5. **واجهة تشرح الافتراضي والمخاطرة** لكل مفتاح؛ لا «مفتاح سحري» بلا وصف.

### ٥.٣ مرفوض قطعًا
- أي كتابة إلى `/system` أو sysconfig أو GMS Doze عبر overlay.
- تعطيل تليمتري بلا قياس أو بلا خيار صريح.
- توابل SoC مثبّتة (KLapse/GED/PPM) كشاشات عامة.
- تطبيق دفعة واحدة عند الإقلاع يستبدل arbiter أو يمنع الرجوع.

---

## ٦) خلفية الأولويات — بعد ما شُحن

**شُحن هذه الجولة:** Device Lab (٨ اختبارات تفاعلية) · Usage Stats (زمن مقدّمة **وحركة شبكة** لكل تطبيق) · **تاريخ البطارية** (عيّنات ١٥ دقيقة + منحنى ٢٤ س/٧ي/٣٠ي) · **مستندات القدرات المولّدة** (`tools/gen_capabilities_doc.py` + بوابة CI) · **خمس بلاطات سريعة** (تجاوز الشحن · البروفايل · شريط نظيف · تسجيل البطارية · السطوع العالي) · إصلاح `PowerManager.currentThermalStatus` · مزامنة عدّ الشاشات ٦٨ في README/الأصول/المتجر · إصلاح بوابة الكتالوج (١٩ بادئة).

**وقيد التحقق في هذه الجولة:** توسيع Device Lab (سماعة الأذن + مخارج الصوت الحيّة) — مكتوب ويُصرَّف، ولا يُقال «شُحن» حتى يمرّ الخطّ الكامل.

### ما أُغلق من القائمة
- **P0/١ تاريخ البطارية** — شُحن (`BatteryHistoryStore` + `BatteryHistoryScreen`).
- **P0/٢ مستندات القدرات** — شُحن (`docs/CAPABILITIES.md` ٥٠٤ أسطر من ٤١ مدخلًا).
- **P0/٤ البلاطات** — شُحن منها: تسجيل البطارية والسطوع العالي (HBM) **والوضع الليلي** (`NightLightTileService` على مفتاح المنصّة نفسه `secure/night_display_activated`) **وبلاطة «إدارة التجميد»** (`FreezeManagerTileService`: مدخل يفتح الشاشة القائمة ولا يجمّد شيئًا بنفسه؛ R-CLOSE-13). الباقي (مراقبة/ملف) لم يُبنَ بعد.
- **P1/٧ إحصاء الشبكة لكل تطبيق** — شُحن داخل Usage Stats بلا إذن جديد (إذن الاستخدام وحده).

### P0 — بلا جذر، أثر مرئي، خطر منخفض
1. **تاريخ البطارية** — عيّنات دورية محلية + رسم ٧/٣٠ يومًا + active/idle drain + Charge Ledger زمني. (DevCheck BatteryMonitor · FKM NewBatteryMonitor)
2. **مستندات القدرات المولّدة** — `tools/gen_capabilities_doc.py` من `control_catalog.json` إلى Markdown/JSON، مع `--assert`؛ لا تغيير في الشاشات. (FKM JsonDocs)
3. **توسيع Device Lab** — ميكروفون/سماعة أذن/Bluetooth/USB/GPS/السطوع/الشحن التفصيلي. (DevCheck tests)
4. **بلاطات سريعة** — HBM/الوضع الليلي/إيقاف المراقبة/ملف اللعب/تجميد الحزم. (FKM tiles)
5. **تقرير تشخيص قابل للمشاركة** — تصدير مُنقّح بلا أسرار. (DevCheck Backup/Manifest)

### P1 — متوسط، يحتاج جهازًا أو صلاحية
6. **استوديو المراقبة العائم** — ٦–٨ مراقبات، مصدر بيانات، موضع/مظهر محفوظ، تكلفة وإيقاف تلقائي. (DevCheck monitors)
7. **إحصاء الشبكة لكل تطبيق** — `NetworkStatsManager` + إذن. (DevCheck NetworkUsage)
8. **ملفات لكل تطبيق مع مشغّل** — كشف مقدّمة + تطبيق/رجوع، مع arbiter. (FKM per-app)
9. **مخططات النظام التاريخية** — نوافذ زمنية وتصدير. (DevCheck SystemGraphs)
10. **عارض مانيفست/ملخّص أذونات** — للجهاز والحزم. (DevCheck Manifest/Permissions)
11. **تحسينات الآيلاند** — أولوية/مؤقّتات/قطة/خصوصية/مؤشّر تسجيل. (ZenBubble)
12. **معايرة اللمس/الحرارة/البطارية** — ملف جهاز بدل أرقام عامة.

### P2 — لاحقًا أو مشروط
13. **محرّك وصفات** («لعب/بطارية/برودة/ليل») مع تحقق ورجوع.
14. **استيراد/تصدير ملف الجهاز** بمخطط إصدار وdiff قبل التطبيق.
15. **ميزانية تكلفة المراقبة** (بطارية/CPU لكل مراقب).
16. ~~**Vulkan/OpenGL capability report**.~~ — **مشحون** في شاشة معلومات الجهاز: GLES من `dumpsys SurfaceFlinger`، وVulkan version/level من قائمة خصائص النظام، كلٌّ بمصدره.
17. **خريطة شبكة** (channel/latency/DNS) — قياس لا تخمين.

### لا يُنقل
ترتيب البنشمارك التنافسي · تفليش الأنوية · الفولتية/KLapse/GED/PPM كشاشات عامة · تعديل sysconfig/GMS Doze · تعطيل تليمتري بلا قياس · أي كود/أصل/نصّ من المراجع · Firebase/Paywall/Translate/Announcements.

---

## ٧) الدليل والتحقق
- البوابات الخفيفة بعد التغييرات: `kt_balance` (٣٢٣٠ ملفًا، ٠ عوائق) · `code_health` ٠ · `i18n_coverage` ٠ · `design_tokens` ٠ · `resource_compile` كل المجلّدات · `license_audit` نظيف · `gen_capabilities_doc` ٤١ مدخلًا.
- CI: Quick Verify نجح على `631972a3` (٣٨٠١ اختبارًا، ٥ متجاوزة)؛ البناء الكامل على `3b17c588` تجاوز بوابات العقد ثم تعثّر في **تنزيل Gradle** (مهلة ١٠ ثوانٍ) لا في الكود — أُعيد تشغيله.
- **دفعات هذه الجولة ونتيجتها في CI:** `1adcf63b` (بلاطة FPS + خطة الاستوديو) ✓ · `89721194` (خدمة تعدّد المراقبات) ✓ · `a1636900` (تحكّم مظهر كل مراقب) ✓ · `246604a0` (منحنى حرارة البطارية) ✓ · `a18b227c` (قياس جودة الوصلة) ✗ **`padding` غير مستورد** · `d7066f1b` (تصدير CSV) أُلغي بالتزامن · `3ca1046e` (إصلاح الاستيراد ومعه الدفعتان) ✓ · `4c21a823` (مستوى الميكروفون) ✓ · `5faba64d` (اختبارات جودة الوصلة) أُلغي بالتزامن ومحتواه داخل ما بعده · `02949467` (قدرات العرض والـHDR) ✓ ضمن ما بعده · `2028fd1b` (مُفكّكات الفيديو) ✗ **اقتباس غير مهروب** · `36edf695` (هروب الاقتباس ومعه الدفعة) ✓ · `ada50f5c` (اختبارات `MonitorPrefs`) ✓ · `a67aa9a6` (ملخّص المانيفست) ✗ **`MonoValueStyleSmall` من `ui.design` و`INSTALL_LOCATION_*` من `PackageManager`** · `eef6dacb` (إصلاح الاستيراد) ✗ **`installLocation` قُرئ من `ApplicationInfo`** · `f2c6572e` (قراءتها من `PackageInfo`) ✓ · `a4c9b104` (منحنى جلسة الحرارة) ✓ · `77f45ba8` (سجل الحصاد) مستندات · `c94000dd` (بلاطة الوضع الليلي) ✓ · `0b534070` (سجلها) مستندات · `da2553a6` (تصدير جلسة الحرارة CSV) ✗ **مُساعد الاختبار يفتقد `atMs` افتراضيًا** · `ed54a942` (إصلاح المُساعد) ✓ · `2ec1979f` و`912a334e` (إغلاق الجولة) مستندات. **وبهذا كل التزام شُحن في هذه الجولة مُتحقَّق في CI.**
- **غير مُتحقَّق:** سلوك Device Lab/Usage Stats على جهاز حقيقي (لمس، شاشة، سماعة، فلاش، ميكروفون، صلاحية Usage Access). لا يُكتب «يعمل» بلا جهاز.

## ٨) ما أُنجز بعد القائمة الأولى (يُقرأ معها لا بدلًا منها)

| البند | الحالة | أين |
| --- | --- | --- |
| P1/6 استوديو المراقبة | **مشحون** (٣ مراقبات، خدمة واحدة، قارئ واحد) | `ui/util/MonitorPrefs.kt` · `service/FpsOverlayService.kt` · `FpsOverlayScreen.kt` |
| P1/9 تصدير المخططات | **مشحون للبطارية** (CSV من النافذة المعروضة عبر ورقة المشاركة) | `BatteryHistoryStore.csv/writeCsv` + `BatteryHistoryCsvTest` |
| P1/9 مخططات النظام التاريخية | **مشحون للجلسة**: منحنى الحرارة يرسم أسخن منطقة مقروءة كل ٣ ثوانٍ (نافذة ٥ دقائق، ١٠٠ عيّنة) بمقياس مشتقّ من المقيس، مع الذروة والأدنى، وفي الذاكرة وحدها — لا يُكتب شيء على التخزين قبل طلبك. **وتصدير الجلسة**: CSV بالعيّنات المعروضة نفسها (زمن منقضي، منطقة، درجة) يُشارَك عبر ورقة المشاركة | `core/thermal/ThermalHistory.kt` + `ThermalHistoryTest` · `ThermalDetailScreen.kt` (`ThermalTrend` · `exportThermalSession`) |
| P2/17 خريطة الشبكة | **شُحن القياس لا الخريطة**: تحليل اسم + أزمنة اتصال TCP + تذبذب + تقدير الوصلة، عند الطلب فقط | `core/net/LinkProbe.kt` · `LinkQualitySection` |
| P0/3 توسيع Device Lab | **مشحون**: ميكروفون (مستوى dBFS حيّ + ذروة، ولا يُسجَّل شيء) · قدرات العرض والـHDR · مُفكّكات الفيديو المعلنة | `MicrophoneTest` · `DisplayCapabilities` · `CodecCapabilities` في `DeviceLabScreen.kt` |
| P0/4 بلاطة الوضع الليلي | **مشحون**: بلاطة إعدادات سريعة تقلب `secure/night_display_activated` نفسه الذي تكتبه شاشة العرض، وتقرأ المفتاح بعد الكتابة — فحين تُرفض الكتابة تقول «لم يُطبَّق» لا حالة لم تقبلها المنصّة | `TileService/NightLightTileService.kt` · `res/drawable/ic_tile_night.xml` |
| P1/10 عارض المانيفست/الأذونات | **مشحون لأي حزمة** (لا لحزمة التطبيق وحده): الشاشة تُفتح بحزمة محدّدة فتقرأ مانيفستها. وتُضيف الآن **الميزات المعلَنة**: ما تطلبه الحزمة (`<uses-feature>`) مقابل ما يعلنه الجهاز، والغائب يُعرض صريحًا | `ui/util/ManifestFacts.kt` · `core/platform/PackageFeatures.kt` (+`PackageFeaturesTest`) · `PermissionsScreen.kt` |
| مولّد JSON Schema | **مشحون**: مخطط الشكل (draft 2020-12) يُشتقّ من مفردات المدقّق نفسها، مع مدقّق داخلي بلا حزم خارجية، ويُقاس في «Contract gates» | `tools/gen_catalog_schema.py` · `docs/control-catalog.schema.json` |
| P1 جدولة الوضع الليلي | **مشحون**: نافذة ساعة بداية/نهاية (تعبر منتصف الليل) تكتب المفتاح نفسه `secure/night_display_activated` عبر `SettingsAccess`، بمنبّه واحد غير دقيق يُعاد جدولته بعد كل تبديلة، ويُعاد ضبطه بعد الإقلاع/التحديث/تغيّر الساعة | `core/display/NightLightWindow.kt` (+`NightLightWindowTest`) · `core/display/NightLightSchedule.kt` · `receiver/NightLightScheduleReceiver.kt` |
| P1 سطح استعادة الإقلاع | **مشحون**: يعرض آخر جولة استعادة (زمن نسبيّ) وعدّاد الإقلاعات السريعة مع تحذير عند ٢ — قراءة فقط بلا زر تصفير، والحارس نفسه قائم في `BootLoopGuard`/`PersistStore` | `PersistStore.fastBootStreak` · `ui/mainscreens/PersistRestoreItem.kt` |

**وما بقي بلا تنفيذ، وسببه مُعلن:** P1/11 تحسينات الآيلاند وP1/12 المعايرة يحتاجان جهازًا وحكمًا بصريًّا/حسّيًّا لا يُثبته مُصرّف؛ P2/13 محرّك الوصفات يكتب على العتاد (يحتاج arbiter وحكم سلامة مستقل)؛ P2/15 ميزانية تكلفة المراقبة لا تُقاس بلا جهاز. وتصدير السجلّات والتشخيص موجود أصلًا فلم يُعد بناؤه.

---

## ٩) تصحيح الحالة — صفوف قديمة في §٢.٤ و§٣.٤ تجاوزها §٨

هذه الجداول كُتبت **قبل** الدفعات اللاحقة، فبقيت صفوفها تعلن فجوات أُغلقت بعدها. لا تُحذف
الصفوف (التاريخ يُقرأ)، بل يُصحَّح حكمها هنا، و§٨ هو المرجع الملزم للأين:

| الصفّ القديم | ما كان يقول | الحقيقة اليوم | الدليل |
| --- | --- | --- | --- |
| §٢.٤ اختبارات العتاد | «لا ميكروفون» | شُحن الميكروفون (مستوى حيّ + ذروة، ولا يُسجَّل شيء) وقدرات العرض/HDR | `DeviceInfoCollect` · Device Lab |
| §٢.٤ استخدام التطبيقات | «لا تفصيل يومي/أسبوعي برسم، ولا إحصاء شبكة لكل تطبيق» | شُحنا داخل Usage Stats بإذن الاستخدام القائم: شبكة لكل تطبيق ورسم يومي ثابت لآخر ٧ أيام؛ لا إذن جديد، وفشل قراءة الأحداث يُعلن لا يتحول إلى صفر | `UsageStatsScreen` · `core/stats/DailyUsage.kt` · `ui/util/DailyUsageReader.kt` (+٢٩ دالة اختبار) |
| §٢.٤ تاريخ البطارية | «لا عيّنات تاريخية، لا active/idle drain» | شُحنت العيّنات والاستنزاف مفصولًا بين شاشة مضيئة ومطفأة، وتصدير CSV | `BatteryHistoryStore` · `BatteryDrain` |
| §٢.٤ عارض المانيفست/الأذونات | «لا عارض مانيفست جهاز/حزم» | شُحن **ملخّص حزمة التطبيق نفسه** قبل قائمة الأذونات؛ و`QUERY_ALL_PACKAGES` **موجود فعلًا** (سطر ٨٢) فعارض حِزم أخرى صار ممكنًا ولم يُبنَ بعد | `ui/util/ManifestFacts.kt` |
| §٢.٤ مراقبات عائمة | «لا مراقب مستقل قابل للسحب والتخصيص» | شُحن استوديو بثلاث مراقبات في نافذة واحدة، مع موضع ومظهر محفوظين وخدمة واحدة | `ui/util/MonitorPrefs.kt` · `service/FpsOverlayService.kt` |
| §٢.٤ مخططات النظام | «لا شاشة مخططات بنوافذ زمنية وتصدير» | شُحن **للبطارية وجلسة الحرارة** (نوافذ + CSV)؛ ولا تزال المخططات العامة غير مشحونة | سجل البطارية · `ThermalHistory` |
| §٣.٤ مستندات القدرات | «لا · الأولوية التالية» | شُحنت مستندات مولّدة من الكتالوج، مع `--assert` في البوابات | `tools/gen_capabilities_doc.py` |
| §٣.٤ البلاطات | «لا NightShift/HBM/مراقبة/ملف لعب/تجميد» | شُحنت: الوضع الليلي · السطوع العالي · تاريخ البطارية · مراقبة FPS · ملف اللعب. **والتجميد موجود فعلًا** كشاشة كاملة (`DebloatFreezeScreen` + `DebloatFreezeUtil` + `PrivilegeFeature("freeze_disable", SHIZUKU)`) **وبلاطة مدخل** (`FreezeManagerTileService` + مسار `ACTION_QS_TILE_PREFERENCES` في `MainActivity`؛ لا تجميد تلقائي ولا حزمة مخمّنة) | `ui/subscreens/DebloatFreezeScreen.kt` · `ui/util/DebloatFreezeUtil.kt` · `TileService/FreezeManagerTileService.kt` |
| §٣.٤ النسخ/الاستعادة | «لا تصدير ملف جهاز بمخطط إصدار ومقارنة قبل التطبيق» | شُحنا: مخطط إصدار للتفضيلات، وفرق قبل التطبيق في ثلاثة محاور (خصائص · قائمة تطبيقات · تفضيلات) | `ConfigBackupInventory` · `ConfigDiff` · `PrefsDelta` |
| §٣.٤ مخطط التوابل (سطر ٩١) | «لا عرض كل ما يعلنه الجهاز» | **شُحن كاملًا**: الكتالوج والمخطط والمستندات، وقائمة `features` المعلنة داخل شاشة الأذونات | `docs/control-catalog.schema.json` · `PackageFeatures` |
| §٥.٢ حارس الإقلاع (سطر ١٤٠) | «الفجوة سطح استعادة» | شُحن السطح (آخر جولة + عدّاد الإقلاعات) والرجوع عبر `PersistSnapshot` | `PersistRestoreItem.kt` · `PersistSnapshot.kt` |
| خريطة قناة Wi-Fi | «تحتاج `ACCESS_FINE_LOCATION`، لا إذن جديد بلا أمر» | **أُذن وأُضيف** (‏`ACCESS_FINE_LOCATION` + `NEARBY_WIFI_DEVICES`): قناة الاتصال وعدد الشبكات والقناة الأكثر ازدحامًا داخل Device Lab، ولا موقع يُقرأ أو يُحفظ؛ والقياس على جهاز حقيقي «يحتاج جهازًا» | `core/net/WifiChannel.kt` (+`WifiChannelTest`) · `ui/util/WifiChannelReader.kt` · `DeviceLabScreen` |

**وما بقي حقًّا بلا تنفيذ، وسببه مُعلن:** P1/8 المشغّل الأمامي **مشحون للألعاب** (`InAppProfileApplier`
+ `InAppProfileRules` يقرآن الحزمة الأمامية من استطلاع `AppMonitor`، ويكتبان `settings put` بالمسار الموحَّد،
بلقطة تُسترجع عند الخروج وعند الإقلاع التالي) — والناقص هو **بروفايل لتطبيق اعتباطي** لا لعبة مرخّصة في
`GameLibrary`، وهو وصلٌ لا بناء. P1/11 الآيلاند وP1/12 المعايرة (جهاز وحكم بصري/حسّي) · P2/13 محرّك
الوصفات (يحتاج كتالوج عقد مُتحقَّق قبل أي كتابة) · P2/15 ميزانية المراقبة (قياس على جهاز). وخريطة قناة
Wi-Fi صارت مشحونة بإذن المالك، لكن قياسها على شبكة حقيقية «يحتاج جهازًا».

**وسياسة الخطير (ADR-90):** كل ما كان مرفوضًا **لسلامة** صار يُشحن بوسم خطر + إقرار مرة واحدة يُسجَّل
بزمنه (`core/policy/DangerConsent.kt` + `ui/component/DangerGate.kt`)، وأول مواضعه: إعادة التطبيق التلقائي
الخطير · كتابة حدود تردد المعالج · كتابة نافذة ترددات الرسوميات. وما تمنعه **الترخيص** أو **المنصّة** أو
يحتاج **جهازًا** لا يفتحه هذا الإقرار.

**تصحيح لاحق (جولة Wi-Fi):** سطران في §٢.٤ كانا يعدّان شيئًا شُحن فعلًا: `ChargeLedger` (يُعرض في `DiagnosticsScreen`) و`QUERY_ALL_PACKAGES` (سطر ٨٢ في المانيفست). صُحّحا أعلاه، وما بقي هو **واجهة** عارض حِزم أخرى لا الإذن.
