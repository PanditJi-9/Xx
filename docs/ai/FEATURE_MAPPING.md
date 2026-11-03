# FEATURE_MAPPING — مصفوفة المقارنة: كل ميزة، آليتها، قرارها، وحدّها

**التاريخ:** 2026-11-03 · **النوع:** مصفوفة قرار **بلا تنفيذ** · **صفر تعديل منتج.**
**الأساس:** `REFERENCE-TEARDOWN-NOV26.md` (كل آلية هنا مقيسة هناك) · `MAXMANAGER-TRANSFORM-01-spec.md` §5 §7
· `CONTROLKEY-CATALOG-01-design.md`.

> **قاعدة الصفّ:** لا صفّ بلا **آلية تقنية مقيسة** في المرجع، ولا صفّ بلا **مسار تنفيذ عندنا** أو **سبب رفض**.
> والعمود الأخير هو التصنيف السباعيّ الذي يطلبه الأمر: عامّ · جذر · Shizuku · مُهايئ جهاز · واجهة مورّد ·
> يحتاج بحثًا · **غير ملائم**.

---

## ٠. التصنيفات — تعريف السبع

| رمز | التصنيف | المعنى العمليّ |
| --- | --- | --- |
| **P** | Public API | يُنفَّذ بواجهات Android العامة، بلا امتياز |
| **R** | Root | يحتاج كتابة `/sys` أو `/proc` أو تنفيذًا بدرجة الجذر |
| **S** | Shizuku | يحتاج ADB-level؛ قد يعمل بلا جذر إن مُنح |
| **D** | Device adapter | يحتاج مُهايئًا خاصًّا لعائلة SoC/روم |
| **V** | Vendor interface | يحتاج واجهة مورّد (HAL/AIDL/بروبريتري) |
| **?** | Needs investigation | لا دليل قاطع بعد؛ يُقاس قبل القرار |
| **X** | Not appropriate | لا يُنسخ: ترخيص، أو مخالف لنمطنا، أو غير آمن |

---

## ١. DevCheck 6.61 — جمع المعلومات والرصد

| الميزة (مقيسة) | الآلية في المرجع | مسار التنفيذ عندنا | المتطلّبات | الحدّ | التصنيف |
| --- | --- | --- | --- | --- | --- |
| درجة الامتياز لكل رقم | `systemGraphs.widgets.PrivilegeBadge` + ٣ طبقات معلنة | `PrivilegeLevel` قائم + `Observation.tier` (M3) | عقد بيانات واحد | يُعرض في كل شارة، لا في شاشات مختارة | **P** |
| ١٥ نافذة مراقبة مستقلّة | `CPUMonitorWindow`…`CustomMonitorWindow` (١٥ صنفًا) | `Monitor` مقعدًا جديدًا + `Sensor` registry (M3) | واجهة `Sensor` | يجب أن تُركَّب بلا شاشة جديدة | **P** |
| مراقب مخصّص يبنيه المستخدم | `CustomMonitorBuilderActivity` | **لا** في v1 | — | سطح تصميم كامل؛ مؤجّل صراحةً | **X (v1)** |
| headroom الحرارة (ADPF) | `temperature.ThermalHeadroomNative` + `getThermalHeadroom` | `thermalHeadroom` مراقبًا (M3) | `PowerManager` عامّ | `NaN` ⇒ `Unsupported` | **P** |
| حرارة النظام | `dumpsys thermalservice` | `getCurrentThermalStatus` + `class/thermal` | — | `dumpsys` عبر shell ⇒ **S** إن لزم | **P / S** |
| بطارية + سعة متعلَّمة | `dumpsys batterystats` + `"Last learned battery capacity"` | `BatteryManager` + `BatteryHealthUtil` قائم | — | الإحصاءات تحتاج `BATTERY_STATS` | **S** |
| CPU: تردّدات/حكام/أنوية | `/proc/cpuinfo` · `/sys/devices/system/cpu/*` | كتالوج `cpu_limits` + `cpu_boost` (M1) | قراءة `/proc` مسموحة | الكتابة تحتاج جذرًا | **P→R** |
| GPU Adreno + Mali | `kgsl-3d0` · `mali/{cur,max}_freq` | مرشّحات `gpu_frequency` الثلاثة | — | ثلاث عائلات ⇒ مُهايئ لكل عائلة | **R / D** |
| FPS لكل لعبة | `GameFpsMonitorWindow` | `FpsOverlayService` قائم | `SYSTEM_ALERT_WINDOW` | القياس داخل اللعبة يحتاج جهازًا | **P** |
| ذاكرة + ضغط I/O | `meminfo` · `vmstat` · `pressure/io` · `pressure/memory` | `memoryStall` مراقبًا | — | الحرارة/الضغط يُقاسان على جهاز | **P** |
| شبكة + إشارة | `NetworkMonitorWindow` · `SignalMonitorWindow` | `NetworkHub` قائم | `READ_PHONE_STATE` | الإشارة تحتاج `TelephonyManager` وROM | **P** |
| تخزين: جدولة + UFS | `block/*/queue/scheduler` · `ufshcd-qcom/*/spec_version` | `StorageHub` + `ufs_frequency` | — | `scheduler` كتابةٌ خطرة ⇒ `HIGH` | **R / D** |
| شاشة: سطوع + SurfaceFlinger timestats | `class/backlight/panel/*` · `dumpsys SurfaceFlinger --timestats` | `DisplayStudioViewModel` قائم | — | `dumpsys` يحتاج shell | **S** |
| بصمة (نوع/موضع) | `fpc1020` · `goodix_fp` · `validity_fingerprint` | **لا** — `PackageManager` يكفي للتعرّف | — | قراءة عقد بصمة لا تضيف قيمة للمستخدم | **X** |
| معايرة/معايير (benchmark) | `benchmarkSuite` + Room history | **لا** في v1 | — | نتائج معايير ليست هدف هذه المرحلة | **X (v1)** |
| Firebase + Paywall + Translate | `PaywallActivity` · `billingclient` · `Firebase` | **لا** | — | ADR-07 (لا تيليمتري) + لا مدفوعات في هذه المرحلة | **X** |
| كتالوج شهادات/مانيفست | `certificate.CertificateDb` · `manifest.ManifestActivity` | `Diagnostics` قائم | — | — | **P** |

---

## ٢. Franco Kernel Manager 7.0.29 — عقد العتاد والتحكّم

| الميزة (مقيسة) | الآلية في المرجع | مسار التنفيذ عندنا | المتطلّبات | الحدّ | التصنيف |
| --- | --- | --- | --- | --- | --- |
| **tunables كمخطّط بيانات** | `JsonSchemaGenerator` + `JsonDocs` + `OpenJsonUrl` | **كتالوج `ControlKey`** (M1) يُولِّد Kotlin والتوثيق | YAML + مولّد stdlib | لا مرآة يدوية | **P (فكرة)** |
| `libsu` واحد للامتياز | `com.topjohnwu.superuser` | **وسيط واحد** يغلّف ٥ مسارات (M2) | — | لا دمج في v1 | **R** |
| CPU: تردّدات + حكام | `/sys/devices/system/cpu/*cpufreq*` | `CpuHub` + كتالوج `cpu_limits` | جذر للكتابة | الحكام تختلف بين النوى | **R / D** |
| GPU: `default_pwrlevel`/`adrenoboost` | `kgsl-3d0/*` · `devfreq/{cur,max,min}_freq` | `gpu_frequency` مرشّحًا | جذر | Adreno ≠ Mali ≠ ged | **R / D** |
| Klapse / color control | `KlapseActivity` · `Klapse2Activity` | **?** — يحتاج بحثًا: هل عقدنا يدعمه؟ | عقدة مورّد | قد لا توجد على MTK | **?** |
| Voltages | `VoltagesActivity` | **X** — جهد CPU بلا حماية جهد = خطر عتاد | — | مرفوض صراحةً بلا قياس جهاز | **X** |
| Wakelock blockers | `WakelockBlockers` | **?** — يحتاج بحثًا عن `/sys/.../wakeup*` عندنا | جذر | كسر وصولية النظام محتمل | **?** |
| Low memory killer | `LowMemoryKiller` | `ZramManager` + `MemoryHub` قائمان | جذر | `lmk` نواة قديمة؛ `psi` أنظف | **R / D** |
| PPM / SoC tunables | `PpmActivity` · `SocTunablesActivity` | `?` — يحتاج قياس عقد MTK `proc/mtk*` | جذر | عقد مورّد | **? / V** |
| Stune / boost | `StuneBoost` | `cpu_boost` قائم | جذر | `schedtune` غير موجود على كل نواة | **R / D** |
| بروفايلات لكل تطبيق | `ApplicationsListPerAppProfiles` + `EditPerAppProfile` | `PerAppControlRegistry` + `ProfileApplier` قائمان | — | يحتاج تحقّقًا على جهاز | **P / R** |
| أتمتة عند الإقلاع | `BootReceiver` + `broadcasts/*` | `PersistRestoreReceiver` قائم | — | السباق يُقاس على جهاز | **P** |
| بيانات + حدث | `DataStore` + `EventBus` | `DataStore`-نظير قائم (`MaxManagerPaths`) | — | EventBus غير مستخدم عندنا | **P** |

**وحدّ صريح:** `Klapse`/`Voltages`/`PPM` **لا تُنقل** من المراجع؛ العقد يُقاس على جهازنا، وإن لم يوجد ⇒ `NO_NODE`.

---

## ٣. ZenBubble — طبقة النافذة والتفاعل

| الميزة (مقيسة) | الآلية في المرجع | مسار التنفيذ عندنا | المتطلّبات | الحدّ | التصنيف |
| --- | --- | --- | --- | --- | --- |
| آلة حالة مستقلّة عن الواجهة | `IslandStateManager` (١١١٦ سطرًا · ٢٠ `StateFlow` · دوالّ تُحقن) | `island/core` — يحتاج **تأكيد الفصل بالقياس** | — | نموذجنا موجود؛ يُقاس لا يُفترض | **P** |
| أولوية صريحة مرتَّبة | `CALL → MEDIA → FLASHLIGHT → PROGRESS → SCREEN_RECORDING → POP_NOTIFICATION → IDLE` | أولوية معلنة ومُختبَرة نقيًّا (M5) | — | ترتيب Bedrock: يُعتمد أو يُعدَّل بقرار مالك | **P** |
| مدير لمس مستقلّ | `IslandTouchManager` + `addOnComputeInternalInsetsListener` + `setTouchableInsets` | `island/engine` — يحتاج تدقيقًا إن كان قائمًا | نافذة نظام | اللمس يمرّ لما تحت النافذة | **P** |
| هندسة القصّة كإعداد | `CutoutModeOption` · `TeardropIslandShape` | `cutoutAnchor` موجود فعلًا في `IslandLayout` | — | يُصبح إعدادًا أوّلَ الدرجة | **P** |
| أحداث عابرة تُطفأ ذاتيًّا | مؤقّتات ١٥٠٠/٣٠٠٠/٤٥٠٠ms | منطق حالة دقيق | — | يحتاج جهازًا للتحقّق من الإحساس | **P** |
| طبقتان للامتياز (Root+Shizuku) | `RootAccessHelper` · `ShizukuHelper` | `PrivilegeBroker` (M2) | Shizuku حيّ | — | **S** |
| إعدادات كـRepository واحد | `SettingsRepository` | `SettingsAccess` + مخزن الإعدادات | — | — | **P** |
| خدمة وصولية | `IslandAccessibilityService` | خدمة الوصولية قائمة | إذن وصولية | — | **P** |
| تسجيل شاشة | `ScreenRecordingService` | `MediaProjection` قائم | موافقة مستخدم | — | **P** |

---

## ٤. HyperCool v2.0 — وحدة الإقلاع (**GPL-3 ⇒ صفر كود**)

| الميزة (مقيسة) | الآلية | مسار التنفيذ عندنا | المتطلّبات | الحدّ | التصنيف |
| --- | --- | --- | --- | --- | --- |
| حارس حلقة إقلاع | عدّاد `boot_tries`؛ ٣ إقلاعات بلا `sys.boot_completed` ⇒ إعطاب ذاتيّ + رسالة في `module.prop` | فكرة في `mainfiles/` **بإذن المالك** | إقلاع فاشل متعمَّد للاختبار | خطر قفل الجهاز | **X→? (بإذن)** |
| لقطة إعداد الإقلاع | `HC_BOOTCFG` في `post-fs-data` | `PersistRestoreReceiver` + مخزن تجاوز | — | يُقاس السباق على جهاز | **P** |
| تأخير تطبيق عشوائيّ ١٠..١٢٠ث | `apply_delay` | **?** — هل يقع تصادم وحدات عندنا؟ | قياس | بلا قياس = لا يُضاف | **?** |
| إعادة بناء zram قبل التطبيقات | `service.sh` | `ZramHardwareBackend` قائم — التوقيت فقط | جذر | الترتيب يُقاس | **R** |
| ١١٠ مفاتيح إعداد | `default.conf` | **X** — لا نتبنّى مجموعة مفاتيح مورّد كقائمة | — | حلّ مورّد يُعمَّم = مرفوض | **X** |
| طبقة `sysconfig` لـGMS Doze | بناء ملفّات في `/system` | **X** — مخالف لنمطنا (لا نعيد كتابة ملفّات النظام) | — | + GPL-3 | **X** |
| دفع واحد عند الإقلاع بدل arbiter | — | **X** — يخالف ADR-11 | — | — | **X** |
| WebUI للإعداد | `webroot/index.html` (٢٣٥KB) | **X** — لا واجهة ويب | — | شكل/كود محميّ | **X** |

---

## ٥. الحصيلة — عدّ الصفوف

| التصنيف | عدد الصفوف | ملاحظة |
| --- | --- | --- |
| **P** — Public API | **٢٦** | الأغلبية: الرصد والتنقّل والإعدادات |
| **R** — Root | **٩** | كل كتابة `/sys`؛ تحتاج وسيطًا |
| **S** — Shizuku / shell | **٤** | `dumpsys`/`batterystats`/`SurfaceFlinger` |
| **D** — Device adapter | **٧** | GPU (٣ عائلات) · schedtune · lmk · ufs |
| **V** — Vendor interface | **١** | عقد MTK (`mtk*`) يحتاج تحقّقًا |
| **?** — يحتاج بحثًا | **٤** | Klapse · wakelock · SoC tunables · `apply_delay` |
| **X** — غير ملائم | **٩** | ترخيص · تيليمتري · جهد/محفظة · شكل مورّد |

⇒ **٦٠ صفًّا.** ولا صفّ منها «نقل كود»: كل صفّ قرار تنفيذ عندنا.

**وثلاث نتائج تُغيّر الترتيب:**
1. **`P` هي الأغلبية (٢٦/٦٠)** — أي أن معظم قيمة المراجع تُنفَّذ **بلا جذر**، وهو ما يطابق قرار المالك
   «لتشغيل أغلب التطبيق بدون روت».
2. **`D` ليست استثناءً (٧ صفوف)** — المُهايئ لكل عائلة SoC **قاعدة** لا حالة خاصة. وهذا سبب وجود
   `candidates[]` في الكتالوج.
3. **`X` تسعة** — والرفض **مُعلن** لا صامت، حتى لا يُعاد اقتراحه من الصفر.

---

## ٦. ما لا تقوله هذه المصفوفة

- **لا تقول إن كل `P` يعمل على جهازك.** تقول إن مساره العامّ موجود؛ والتحقّق يحتاج جهازًا.
- **لا تقيس الأداء.** «مراقب مستقلّ» ليس «أخفّ»؛ الوتيرة تُقاس في M3.
- **لا تحكم على الشكل.** أي شكل من المراجع **مرفوض** (ADR-18 §0.4-4) — والمصفوفة آليات فقط.
- **لا تُثبت الترخيص.** الأحكام في `REFERENCE-TEARDOWN §4`؛ و`license_audit --assert` هو الحكم الآليّ.
