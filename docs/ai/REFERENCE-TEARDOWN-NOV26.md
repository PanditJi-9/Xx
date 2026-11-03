# REFERENCE-TEARDOWN-NOV26 — تشريح المراجع الأربعة (قياس لا وصف)

**التاريخ:** 2026-11-03 · **النوع:** تحليل عكسيّ وظيفيّ **فقط** — **صفر سطر كود منقول** (§4).
**المراجع:** `DevCheck 6.61` · `Franco Kernel Manager 7.0.29` · `ZenBubble` · `HyperCool v2.0`.
**المكان:** `/storage/emulated/0/MT2/apks/` — **لم تُنقل ولم تُعدّل**؛ قُرئت في موضعها.

> كل رقم في هذا الملفّ مقيس بأمر مذكور في §12، أو منقول حرفيًّا من مخرَج أداة. وما لم يُقَس
> مكتوب بجانبه **«غير مُتحقَّقة»** ولا يُقرأ كنجاح.

---

## 0. الأمر والحدود

**المطلوب (نصّ المالك):** تحويل MaxManager تحويلًا كبيرًا — معمارية جديدة، اتجاه تقنيّ طويل المدى —
بعد دراسة المراجع الأربعة دراسة عميقة، لكل مرجع غرض تحليليّ مختلف، بأداة REA وبأدوات تحليل ساكن عند الحاجة.

**ما نُفِّذ في هذه الجولة:**

| فُعل | لم يُفعل |
| --- | --- |
| بصمة SHA-256 للأصل، ثم فكّ إلى `/tmp/ref` **خارج الشجرة** | لا نقل إلى المستودع، ولا تعديل للأصل، ولا `chmod` عليه |
| `aapt2 dump xmltree` + `badging` لكل APK (المُصرِّف الرسميّ يحكم على المانيفست) | لا تنفيذ لأي من التطبيقات الأربعة |
| `jadx 1.5.6` على FKM → **٣٨٤٥ ملف Java** في ٨٤ ثانية | لم يُفكّ ZenBubble وDevCheck كاملين (الحجم/الحماية — §5 §7) |
| `refdump.py` (أداة جديدة في `tools/`) على الثلاثة: DEX strings/types/methods/classes + ELF strings | لا تحليل حيّ (بلا جهاز جذر) — كل «يحتاج جهازًا» معلن في §11 |
| قراءة HyperCool v2.0 كاملًا (٤٣ مدخلًا) — **قراءةً فقط، ومفتاح `LICENSE` أولًا** | لم يُشغَّل سطر واحد من سكربتاتها |

**ولا يُدَّعى ما لم يقع:** لم تُشغَّل أي من هذه التطبيقات، ولم يحدث أي اتصال بشبكة من داخل التطبيقات
(نُفِّذ تحليل ساكن فقط)، ولم يُنفَّذ أي شيء ممّا تصفه هذه الوثيقة على جهاز.

---

## 1. البيئة — **ليست Codespaces، وهذا مقيس**

| الحقيقة | القياس |
| --- | --- |
| النواة/العضوية | `Linux localhost 6.17.0-PRoot-Distro … aarch64 GNU/Linux` — **PRoot على الهاتف نفسه** |
| متغيّرات Codespaces | `env \| grep -i codespace` ⇒ **لا نتيجة** |
| مساحة الهاتف | `/storage/emulated/0` مربوط عند `/mnt/sdcard` ⇒ **ملفّاتك مقروءة هنا مباشرة** |
| القرص | ٤٨٠G، المتاح **١١G** (٩٨٪ مستعمل) |
| الذاكرة | ١٠G، المتاح **٢٫٧G** عند القياس |
| الشبكة | `github.com` ⇒ HTTP/2 200 |

**النتيجة العملية:** «نقل الملفّات من الهاتف إلى Codespaces» **لم يكن لازمًا ولن يكون** — الملفّات
مقروءة في مكانها. ولا أملك من هذه الجلسة إنشاء Codespace ولا الوصول إليه؛ وما يلي كلّه نُفِّذ **على
الهاتف** بنجاح. وعليه: أيّ وصف لـ«بيئة سحابية» في طلبك الأصلي غير قابل للتنفيذ من هنا، والبديل المقيس
أفضل لا أسوأ (صفر نقل ⇒ صفر تحقّق نقل).

**والحدّ الحقيقي لهذه البيئة ليس العتاد بل الغياب:** لا `adb` ولا جهازًا متصلًا ⇒ كل سلوك حيّ
(لمس، نافذة نظام، SELinux، إقلاع) **يحتاج جهازًا**، ولا يُستنتج من التحليل الساكن.

---

## 2. الأدوات — ما ثُبّت، وما كان حاضرًا، وما رُفض

### 2.1 الحاضر أصلًا

| الأداة | النسخة | الحكم |
| --- | --- | --- |
| `aapt2` | `2.19-debian` (arm64) | **يعمل**: `dump xmltree` و`dump badging` نجحا على الثلاثة ⇒ المانيفست والموارد محكومان بأداة المنصّة |
| `python3` | `3.14.4` | stdlib فقط — **لا** `androguard`/`lief`/`capstone`/`pyelftools`/`requests` |
| `java` | `25.0.4` | كافٍ لتشغيل JADX (لا للبناء — §9) |
| `unzip` `git` `gradle` | — | حاضر |
| `jadx` `apktool` `baksmali` `d8` | — | **غائبة** |

### 2.2 ما ثُبّت في هذه الجولة (خارج الشجرة)

`jadx 1.5.6` (`jadx-1.5.6.zip` من إصدار skylot الرسميّ) ⇒ فُكّ إلى `/tmp/ref/jadx` وشُغّل على aarch64
بـ`-Xmx2g` بنجاح. **وهو المحرّك نفسه الذي يستدعيه REA**، فالفارق أننا ندعوه مباشرةً بلا طبقة تنسيق.

### 2.3 REA — ما هي فعلًا، وحدودها علينا

**ما هي (من صفحة المشروع الرسمية، مقروءة في هذه الجولة):** خادم **MCP** يربط الوكيل بأدوات تحليل
عكسيّ — ELF/PE/Mach-O عبر Ghidra/Hopper/IDA/Kuna، JavaScript/Electron، .NET، Go، Unity IL2CPP،
kernel modules، SQLite، HAR/PCAP، **وAndroid APK**. التشغيل محلّي، والنتائج تُعاد مع **دليلها وحدودها**.

| السؤال الذي طلبتَه | الجواب المقيس/المقروء |
| --- | --- |
| القدرات | تغطّي ٢٥+ نوع هدف؛ وللـAPK: «Manifest declarations, classes, decompiled methods and references» |
| المتطلّبات | **Node.js 22.23.4+/24.22.1+/26.12.1+** وnpm، والتثبيت `npx rea-agents setup` |
| APK/DEX/الموارد | **JADX بلا واجهة + JDK كامل على Linux/macOS/Windows x64**؛ والموارد تحتاج **Apktool**؛ والجهاز يحتاج **adb** |
| المكتبات الأصلية | تحتاج مزوّدًا خارجيًّا (Ghidra/Hopper/IDA) — لا مفتاح محلّل مدمج لـELF في مسار Android |
| الحدّ الحاكم علينا | **x64 مطلوب لمسار Android**، وهذا المضيف **aarch64**؛ وNode غير حاضر |
| الحدّ الثاني | REA **طبقة تنسيق فوق أدوات**، لا محرّكًا بديلًا: JADX يبقى هو من يفكّ، وApktool من يفكّ الموارد |

**القرار (بسبب تقنيّ، لا تفضيل):** في هذه الجولة نستدعي **JADX مباشرة** — هو محرّك REA نفسه في مسار
APK — مع `aapt2` للمانيفست/الموارد و`refdump.py` لمسح DEX/ELF. ⇒ لا أضيف أداة بلا سبب، ولا أدّعي
تشغيل REA هنا. ويبقى REA خيارًا صحيحًا إن انتقل العمل إلى مضيف x64.

### 2.4 `tools/refdump.py` — أداة المسح التي كُتبت هنا (ولماذا)

`androguard` و`lief` غائبان، وpip في بيئة PRoot بلا ضمان. فكُتب ماسح **stdlib خالص** يقرأ DEX داخل
الـAPK مباشرة (بلا فكّ): جداول `string_ids`/`type_ids`/`method_ids`/`field_ids`/`class_defs`، ويطبع
الأصناف المُعرَّفة، ومراجع الدوال الخارجية (أي **واجهات النظام** تُستدعى)، والسلاسل النصّية، وسلاسل
مقاطع `.so` (ELF strings). **١٢٩ سطرًا، صفر تبعية.** وهو مصدر كل عدد في §5–§7. الاستعمال في §12.

---

## 3. البصمات — الأصل كما هو

```
c9abe793bc6d7cd25e784c8e127555e2dfe68bee82ad630a1c2b9d81c62f2782  DevCheck_6.61_apks.apk   (30,093,939 B)
72ac399f60ea987f53ac0446305600da86842239172065d8f437453b8bfb602e  Franco Kernel Manager_7.0.29.apk (8,032,346 B)
19b2d791059f72cb64eb3c736ef2b0df9e9c02072a9525526b73a1ed999f80af  ZenBubble.apk            (67,082,790 B)
40d4ab719c579d7dd1e19b9af621eb08dd8daf1f4ad041eecdb7326ab64d0927  HyperCool_v2.0.zip       (325,077 B)
f502d434ee9eceae0357ac220d5fd9cfc6812fdcae4c7d7ed055e2b22eb3f8cc  DevCheck_6.61.apks       (حزمة splits — لم تُحلَّل)
```

فُكّت النسخ إلى `/tmp/ref/**`، والأصل بلا كتابة. **إعادة التحقّق:** `sha256sum` على الأصل نفسه بعد
العمل تعطي البصمات ذاتها (لم تُعدَّل).

---

## 4. الترخيص قبل أي سطر — الحكم الرباعي (قاعدة §0.4-1)

| المرجع | الترخيص المقيس | الحكم على الكود |
| --- | --- | --- |
| **HyperCool v2.0** | `LICENSE` = **GNU GPL v3** (٣٥٬١٤٩ بايت) + `SPDX-License-Identifier: GPL-3.0-or-later` في كل سكربت | **صفر كود. لا سطر.** GPL-3 يفرض فتح المصدر — والمستودع مغلق |
| **DevCheck 6.61** | مغلق (Play) + **`libpairipcore.so`** (PairIP) | صفر كود |
| **FKM 7.0.29** | مغلق (Play) | صفر كود |
| **ZenBubble** | مغلق (Play) | صفر كود |

**⇒ المُستعمل في هذا الملفّ وفي خطة §9 هو: أفكار، وآليات منصّة، وأسماء واجهات أندرويد القياسية، وأرقام
مقيسة. لا ملفّ، ولا صنف، ولا دالة، ولا نصّ.** ولذلك **لا صفّ جديد في `THIRD_PARTY_NOTICES.md` ولا في
`Credits`** (محلّهما الكود المنقول — ADR-77).

**وتغييرٌ ملزم على السجل السابق:** `HYPERCOOL-STUDY.md` بُني على «لا ترخيص ⇒ كل الحقوق محفوظة» في
**v1.6**. في **v2.0** صار الترخيص **GPL-3 صريحًا**. الحكم العمليّ لم يتغيّر (صفر كود في الحالين)،
لكن **سببه تغيّر**: من «غير مُثبت» إلى «مُثبت ويرفض». ويُسجَّل هذا التصحيح في `HANDOFF`.

---

## 5. DevCheck 6.61 — المرجع الأوّل: جمع المعلومات

**الهوية:** `flar2.devcheck` · مانيفست ٦٥٢ سطرًا · **٣٩٧٢ صنفًا** · DEX واحد · ٢٥٬٩٠٨ سلسلة · ٢٧٬٧١١ مرجع دالة.
**الأصل:** `libdevcheck.so` · `libbenchmarksuite.so` · `libvulkan_info.so` · **`libpairipcore.so`**.

### 5.1 نموذج الامتياز — ثلاث طبقات معلنة (أهمّ ما يُستفاد)

| الطبقة | الدليل المقيس | لماذا تهمّنا |
| --- | --- | --- |
| (١) واجهات عامّة | `BATTERY_STATS` · `PACKAGE_USAGE_STATS` · `DUMP` · `READ_PHONE_STATE` · `TRANSMIT_IR` | تعمل بلا جذر |
| (٢) **Shizuku** (ADB-level، بلا جذر) | `moe.shizuku.manager.permission.API_V23` · `rikka.shizuku.ShizukuProvider` · `IMyRemoteProcess` · `shizuku:attach-*` · `flar2.devcheck.ui.shizuku.ShizukuProcessService` | **مسار ثالث** لا يملكه MaxManager اليوم بهذا الاتساع |
| (٣) الجذر | سلسلة `RootServerMain` من **libsu** · بحث `su` في ٨ مسارات (`/system/bin`, `/system/xbin`, `/sbin`, `/data/local/xbin` …) · `flar2.devcheck.nativebridge.RootBridge` | نمط `libsu` نفسه القائم عندنا |

**والشاهد المعماريّ:** وجود `systemGraphs.widgets.PrivilegeBadge` — أي أن **درجة الامتياز تُعرض في
الواجهة** لكل رقم. هذا يطابق `PrivilegeLevel` عندنا، ويؤكّد أن الفصل بين «مقروء بلا امتياز/بـShizuku/بالجذر»
**ميزة صدق** لا تفصيلًا تقنيًّا.

### 5.2 الواجهات التي يقرأها فعلًا (مقيسة من جدول السلاسل)

| المجال | ما قُرئ |
| --- | --- |
| `/proc` | `cpuinfo` · `meminfo` · `stat` · `loadavg` · `mounts` · `swaps` · `vmstat` · `version` · `pressure/io` · `pressure/memory` · `asound/cards` · `mtktscpu` (MediaTek) · `PID/{cmdline,stat}` |
| CPU/GPU | `mali/{cur_freq,max_freq,power_policy}` · `devfreq/*/available_frequencies` · `devfreq/*/trans_stat` · `kgsl-3d0` · `/dev/kgsl-3d0` · `/dev/mali0` |
| تخزين | `block/mmcblk*/queue/{scheduler,read_ahead_kb}` · `block/sda/…` · `zram0/{disksize,comp_algorithm,mm_stat}` · `ufshcd-qcom/*/spec_version` · `/dev/block/vnswap0` |
| شاشة | `class/backlight/panel/{brightness,auto_brightness}` · `dumpsys SurfaceFlinger --timestats` |
| بصمات | `fpc1020` · `goodix_fp` · `validity_fingerprint` |
| حرارة | `dumpsys thermalservice` · **`temperature.ThermalHeadroomNative`** (‏ADPF headroom عبر طبقة أصلية) |
| بطارية | `dumpsys batterystats` · `"Last learned battery capacity"` |

**أصناف المجالات (١٥ نافذة مراقبة):** `CPUMonitorWindow` · `GPUMonitorWindow` · `FPSMonitorWindow` ·
`GameFpsMonitorWindow` · `RamMonitorWindow` · `TempMonitorWindow` · `BatteryMonitorWindow` ·
`NetworkMonitorWindow` · `SignalMonitorWindow` · `LoadMonitorWindow` · `CurrentMonitorWindow` ·
`CustomMonitorWindow` (+ `CustomMonitorBuilderActivity`) — **كل نافذة صنف مستقلّ**.

**والباقي المقيس:** `benchmarkSuite` (native bridge + calibration + sanity + history Room) ·
`networkMapper` · `taskmgr`/`taskmgr2` · `opengl` · `manifest.ManifestActivity` · `permissionsSummary` ·
`certificate.CertificateDb` · `hwData.DeviceInfo` · `mydevice.MyDeviceActivity` + `journal` ·
`gpu.{adreno,mali}` · `nativebridge.{GpuBridge,MainBridge,RootBridge,VulkanBridge}` · ٤ قواعد **Room** ·
`WorkManager` · `Firebase` · `PaywallActivity` + `com.android.billingclient` · `TranslateActivity`.

**الحدّ القانونيّ:** `libpairipcore.so` يعني حماية/تشويشًا جزئيًّا (أسماء مختلطة: `QZk.ZTkWByp`, `hwData.a/b/c`,
`opengl.Mn.*`). ⇒ التحليل الوظيفيّ هو السقف، ولا مطاردة لكود محميّ.

---

## 6. Franco Kernel Manager 7.0.29 — المرجع الثاني: عقد العتاد

**الهوية:** `com.franco.kernel` · `minSdk 26` · `targetSdk 36` · **١٧ إذنًا فقط** · **٤١٠٨ أصناف** ·
DEX واحد (٤٫٢MB) · ٢٤٬٤٣٨ مرجع دالة · فُكّ بـJADX إلى **٣٨٤٥ ملف Java في ٨٤ ثانية**.

**الأذون المميّزة:** `DUMP` · `BATTERY_STATS` · `PACKAGE_USAGE_STATS` · `SYSTEM_ALERT_WINDOW` ·
`FOREGROUND_SERVICE_SPECIAL_USE` · `WRITE_SYNC_SETTINGS` · `ACCESS_COARSE_LOCATION`. **ولا إذن جذر** —
الجذر عبر `libsu` (`com.topjohnwu.superuser` موجود في المصادر).

**البنية المقيسة:** `activities/` لكل ميزة (`CoreControlActivity` · `GovernorProfiles` · `KlapseActivity` ·
`Klapse2Activity` · `PpmActivity` · `StuneBoost` · `VoltagesActivity` · `WakelockBlockers` ·
`LowMemoryKiller` · `SocTunablesActivity` · `GedActivity` · `BatteryMonitorActivity` · `SystemMonitorActivity` ·
`CustomTunablesFileManagerActivity` · `ApplicationsListPerAppProfiles` + `EditPerAppProfile`) ·
`broadcasts/` (`BootReceiver` · `Reboot` · `Enable/Disable{Hbm,NightShift}` · `ApplyNightShift`) ·
`color_control/` (‏Klapse لكل جهاز) · `ViewPager2`/`AppCompat` (**لا Compose**) · `DataStore` · `EventBus`.

**عقد العتاد المقيس (عيّنة من مسارات `sysfs` الحرفية):**

```
/sys/class/devfreq/gpufreq/{available_frequencies,available_governors,cur_freq,governor,max_freq,min_freq}
/sys/class/kgsl/kgsl-3d0/{default_pwrlevel,gpu_available_frequencies,gpubusy,gpuclk,max_gpuclk}
/sys/class/kgsl/kgsl-3d0/devfreq/{adrenoboost,available_governors,cur_freq,governor,max_freq,min_freq}
```

**وما يستحقّ الاستعارة (آلية، لا كود):** وجود **`JsonSchemaGenerator` + `JsonDocs` + `OpenJsonUrl`** ⇒
الـtunables عندهم **مُعرَّفة كمخطّط**، والواجهة والتوثيق يُولَّدان منه. أي أن «ماذا يدعم هذا الجهاز»
جوابه **بيانات**، لا شيفرة لكل ميزة. هذه أقوى فكرة معمارية في المراجع الأربعة، وتُقابل عندنا
`HardwareCapabilityResolver` + `HardwareDeviceOverlay` **بلا مخطّط عامّ** (§9).

---

## 7. ZenBubble — المرجع الثالث: طبقة النافذة والتفاعل

**الهوية:** `com.droidbubble.app` · **١٢ DEX** · **٢٩٬٥٥٦ صنفًا** · ٢٦٤٬٩٧٧ سلسلة · **٦٨٧ صنفًا
خاصًّا** · **بلا مكتبة أصلية خاصّة** (فقط `androidx.graphics.path` و`datastore_shared_counter`) ·
**Compose خالص** (‏`androidx.compose.*` يغلب: material/material3/foundation/ui/animation).

**حدّ التحليل:** في الجولة الأولى لم يُفكّ الـAPK كاملًا (٦٤MB/١٢ DEX على ٢٫٧G ⇒ خطر OOM)، فقِيس
خريطة الأصناف والسلاسل. ثم **فُكّت وحدات الجزيرة والخدمات وحدها بنجاح** (§7.1 أدناه) — وهي المقصودة
بالتحليل. وتبقى بقية الأصناف (٢٩٬٥٥٦) غير مفكّكة، ولا حاجة إليها.

**البنية المقيسة (أصناف المشروع نفسه):**

| الوحدة | الأصناف |
| --- | --- |
| `island/` | `IslandStateManager` · `IslandViewKt` · `TeardropIslandShape` · `IslandPriority` · `IslandSettings` · `NotificationActionItem` |
| `model/` | `ActiveCallState` · `FpsInfo` · `MediaInfo` · `NetworkSpeedInfo` · `NotificationInfo` · `ScreenRecordingState` · `WeatherInfo` · `HourlyWeather` · `IslandPriority` |
| `service/` | `IslandOverlayService` · `IslandTouchManager` · `IslandAccessibilityService` · `NotificationReceiverService` · `ScreenRecordingService` · `PersistentWidgetTelemetryManager` · `PowerConnectionReceiver` · `BootReceiver` · `BatteryUtils` · `CustomLifecycleOwner` |
| `util/` | **`RootAccessHelper`** · **`ShizukuHelper`** · `UpdateManager` · `AppReleaseInfo` |
| `ui/main/` | `MainScreenPage` · `MainScreenViewModel` · `CutoutModeOption` · `PermissionHelper` · `PermissionItem` · `GroupPosition` · `FeedbackType` · `AppItem` |
| `data/` | `SettingsRepository` |

**ثلاث حقائق معمارية:** ① **حالة الجزيرة نموذج أوّلي مستقلّ** (`IslandStateManager`) لا منطق داخل composable —
وهو نمطنا المقصود في `island/core`. ② **اللمس مدير مستقلّ** (`IslandTouchManager`) — لأن نافذة فوق
التطبيقات تحتاج فصلًا تامًّا بين اللمس والرسم. ③ **الامتياز طبقتان** (`Root` + `Shizuku`) حتى لتطبيق
واجهة — أي أن Shizuku ليس ترفًا بل **المسار الافتراضي** حين لا جذر.

**`CutoutModeOption` + `TeardropIslandShape`:** كلاهما يدلّ على أن **هندسة القصّة (cutout) تُعامل
كإعداد أوّل الدرجة**، لا كحالة خاصة. وهي الفكرة القابلة للنقل إلى جزيرتنا (§9-د).

---

### ٧٫١ الحفر الثاني — فُكّت وحدات الجزيرة فعلًا (٣ DEX · ٥٧ ملفًّا · ١٣٫٧ث)

**لماذا نجح الآن:** الجزيرة **ليست** في `classes.dex` (٤٠MB) بل في **`classes3.dex` (١٨٠KB)** و
**`classes5.dex` (٧٦٨KB)** و`classes7.dex` (٦٠٨KB) — قيس بـ`grep -oa 'IslandStateManager'` على كل DEX.
فكّها `jadx` بـ`-Xmx1800m` في **١٣٫٧ ثانية** وأنتج **٥٧ ملفًّا**؛ `EXIT=3` (معناه «بعض الأصناف لم تُفكّ»)
و`grep -c 'Failed to decompile'` ⇒ **٠** سطر خطأ صريح.

| الملفّ | الأسطر | ما يثبته |
| --- | --- | --- |
| `IslandStateManager.java` | **١١١٦** | آلة الحالة كاملة |
| `IslandOverlayService.java` | **١٧٠٧** | النافذة ودورة الحياة |
| `NotificationReceiverService.java` | **٣١٧٣** | مصدر الأحداث |
| `IslandTouchManager.java` | **١٤١** | منطقة اللمس |

**(أ) آلة الحالة مستقلّة عن الواجهة تمامًا:** كائن واحد `INSTANCE` يملك **٢٠** زوجًا من
`MutableStateFlow`/`StateFlow` (الشحن · القفل · البلوتوث · نمط الرنين · الوسائط · الإشعار · التقدّم ·
تسجيل الشاشة · المكالمة · التوسّع · FPS · الطقس · سرعة الشبكة · الكشاف…)، **ولا يعرف Compose**: كل
الأفعال (تشغيل/إيقاف · تقديم · ردّ على إشعار · إنهاء مكالمة) **حقول دوالّ تُحقن** من الخارج.
⇒ العقد المقترح في `ARCHITECTURE-EVOLUTION` §5 مُصدَّق بمرجع حقيقيّ، لا مُتخيَّل.

**(ب) الأولوية صريحة ومرتَّبة** (من `getCurrentPriority` حرفًا):
`CALL → MEDIA → FLASHLIGHT → PROGRESS → SCREEN_RECORDING → POP_NOTIFICATION → IDLE`،
و`isOccupied()` = OR لعشر حالات، و`activeExpandedPriority` يرجع إلى `currentPriority` إن لم يُحدَّد.

**(ج) الأحداث العابرة تُنطفئ بنفسها — ثلاثة مؤقّتات مقيسة:** `1500ms` (فتح القفل · البلوتوث · الرنين)،
`3000ms` (اثنان)، `4500ms` — بدل حالة تلتصق بالنافذة. و`setDeviceLocked(true)` **يطوي** الجزيرة إن كانت مفتوحة.

**(د) اللمس: منطقة ديناميكية بالانعكاس على `ViewTreeObserver`** — `addOnComputeInternalInsetsListener`
(انعكاس على الصنف `OnComputeInternalInsetsListener`) ثم `setTouchableInsets` مع `union` لمستطيلات
الكبسولة: مفتوحة ⇒ `0` (الإطار كلّه) · مشغولة ⇒ `3` (منطقة = اتحاد المستطيلات) · وإلّا **منطقة فارغة**
⇒ اللمس يمرّ إلى ما تحت النافذة. **هذا هو المفتاح الذي يجعل نافذةً بعرض الشاشة لا تسرق اللمس.**

**(هـ) النافذة (مقيسة):** `gravity = 49` (أعلى + وسط أفقي) · `windowAnimations = 0` · ضبابية الخلفية
`setBlurBehindRadius` محدودة `0..100` وتُفتح/تُغلق بـ`flags |= 4` / `&= -5` · `layoutInDisplayCutoutMode`
∈ {1, 3} · الطيّ المُتحرّك يتدرّج بالسرعة: `coerceIn(560/speed, 380, 950)ms` ثم `coerceIn(420/speed, 320, 750)ms`.

**وحدّ هذا القسم:** قراءة **بنية** لا نسخ — ولا سطر من هذه الملفّات يدخل MaxManager (التطبيق مغلق المصدر).

---

## 8. HyperCool v2.0 — المرجع الرابع: وحدة الإقلاع (بلا كود)

**الهوية:** `module.prop` ⇒ `id=hypercool` · `v2.0` · `versionCode=200` · مؤلّف `Yudharn` ·
الوصف: «Heat and battery fix … Qualcomm | MediaTek (one-shot at boot, only tiny opt-in keepers). Configure in WebUI, then reboot.»
**٤٣ مدخلًا في الـZIP** · `common/functions.sh` **١٥٥٤ سطرًا** (كان ١١١٢ في v1.6) · `xfeatures.sh` **٧٨٦** ·
`webroot/index.html` **٢٣٥KB** (واجهة WebUI كاملة) · **`LICENSE` = GPL-3** (§4).

**مفاتيح الإعداد: ١١٠** (`default.conf`) بعد **٧١** في v1.6 — أي **+٣٩ مفتاحًا** في نسخة واحدة.

**المفاتيح الجديدة المقيسة (ما لم يكن في v1.6):**
`cpu_cap_little`(+`_lock`) · `gpu_throttle` · `walt_task_util` · `bg_daemons` ·
`soff_prof`/`soff_cpu`/`soff_gpu`/`soff_boost`/`wifi_keep`/`soff_radio`/`soff_radio_min`/`soff_radio_bt` ·
`rr_adapt`/`rr_active`/`rr_idle` · `boot_notify` · `app_disable`(+`_pkgs`) · `loc_throttle` ·
`saver_vib`/`saver_dim` · `smart_px` · `doze_custom` · `x_compact` · `x_zram`(+`_size`/`_alg`/`_custom`) ·
`x_eas` · `x_job_limit` · `x_rpm` · `x_balanced`(+`x_bal_bus`) · `x_kcommon` ·
`x_cache_days`/`x_cache_keep`/`x_cache_keep_pkgs`/`x_cache_notify`.

**نموذج التشغيل المقيس (سطرًا بسطر، قراءةً):**
١. `post-fs-data` **حصريّ ومختصر**: تهيئة المجلّدات، نسخ الإعداد، **لقطة إعداد إقلاع** (`HC_BOOTCFG`) فلا
يُطبَّق تعديل الـWebUI أثناء الإقلاع، ثم **إعادة بناء طبقة `sysconfig` لـGMS Doze** إن فُعّلت.
٢. **حارس حلقة الإقلاع**: عدّاد في `boot_tries`؛ ثلاثة إقلاعات بلا `sys.boot_completed` ⇒ **الوحدة تُعطّل
نفسها** وتعيد كتابة `description` في `module.prop` برسالة واضحة.
٣. `service.sh`: ينتظر `sys.boot_completed` بحدّ ٢٤٠ ثانية، يمسح العدّاد، يعيد بناء zram **قبل** أن تملأه
التطبيقات، ثم **تأخير عشوائي ١٠..١٢٠ث (افتراضيّ ٣٠)** «ليكمل الروم ضبطه أوّلًا»، ثم يطبّق الدوالّ.
٤. **keeprs اختيارية فقط** (حدّ شحن، تنظيف كاش) — وإلا فلا شيء يعمل في الخلفية.

**وثلاث آليات تستحقّ التسجيل (ولم يُنقل منها سطر):**
- **لقطة الإقلاع:** «الإعداد الذي يُطبَّق هو ما كان محفوظًا عند `post-fs-data`» — يقتل سباقًا حقيقيًّا بين
  حفظ المستخدم وتطبيق الإقلاع. نظيرنا المقترح: `PersistRestoreReceiver` + مخزن تجاوز (مهمة قائمة).
- **حارس الحلقة:** إعطاب وحدةٍ نفسها بعد ٣ إقلاعات فاشلة سلوك **دفاعيّ** يستحقّ النقل كفكرة إلى وحدتنا.
- **`apply_delay` عشوائيّ:** يمنع تصادم وحداتٍ متعدّدة في اللحظة نفسها.

**وخلافاتنا معها لا تُخفى:** هي تعمل **دفعةً واحدة عند الإقلاع** ونحن نعمل **عند الطلب عبر arbiter**
(ADR-11)؛ وهي تكتب ملفّات النظام مباشرةً ونحن نحفظ V2 داخل ملفّنا؛ وهي تمسّ مزوّدين محدّدين (Xiaomi/
MediaTek) ونحن نرفض تمرير حلّ مزوّد كحلّ عامّ.

---

## 9. مصفوفة الفجوات — MaxManager مقابل الثلاثة (مقيس على الطرفين)

**حالة MaxManager المقيسة اليوم:** **١١٦ شاشة** (subscreens+mainscreens) · **٤٢٤** ملفّ `ui/*.kt` ·
**٣٥٢** ملفّ `core/*.kt` · **٦٥** ملفًّا في `core/hardware` · **١٩** ملفًّا باسم `*Backend*` ·
**٢٩** ملفًّا في `core/maxai` · **٣٨٣** ملفّ اختبار · **٣٥٤٠** نصًّا إنجليزيًّا · **٨٤** لغة ·
**١٩** ملفًّا في `island/` · `DeviceInfo` **٦ ملفّات / ٤٣٢٣ سطرًا** · **٣٩** ملفّ Kotlin يحمل مسار `/sys/` حرفيًّا.

| القدرة | المرجع | عندنا اليوم (مقيس) | الفجوة الحقيقية |
| --- | --- | --- | --- |
| عقد العتاد كمخطّط بيانات | FKM (`JsonSchemaGenerator`/`JsonDocs`) | `HardwareCapabilityResolver` + `HardwareDeviceOverlay` + ٦٥ ملفًّا **بلا مخطّط عامّ** | **الفجوة الأولى**: القدرات موزّعة على شيفرة، لا مُعلنة في بيانات |
| وسيط امتياز واحد | DevCheck (٣ طبقات معلنة) · ZenBubble (٢) | **٤ مسارات**: `RootIpcManager` + `ipc/RootNodeService` + `privilege/PrivilegeManager` + الـdaemon النصّيّ | توحيد العقد لا المسارات |
| سياق الامتياز معروضًا | DevCheck (`PrivilegeBadge`) | `PrivilegeLevel` موجود | يُعرض في بعض الشاشات فقط — لا في كل رقم |
| مراقبات مستقلة القابلة للتركيب | DevCheck (١٥ نافذة + `CustomMonitorBuilderActivity`) | مراقبات داخل شاشات | **مراقب كصنف أوّليّ** + بناء مراقب مخصّص |
| ARSC/مخطّط موارد في الأداة | FKM (JsonDocs) · DevCheck | `tools/` بلا ماسح DEX | `refdump.py` أُضيف الآن |
| headroom المنصّة (ADPF) | DevCheck (`ThermalHeadroomNative`) | `PredictorBridge` (طبقة أصلية) + `ThermalUtil` | إشارة منصّة ثالثة بلا جذر (XR-08) |
| الجزيرة كآلة حالة + أولوية | ZenBubble (`IslandStateManager`, `IslandPriority`) | `island/core` + `island/engine` (١٩ ملفًّا) | نموذج أولوية صريح + مدير لمس مستقلّ |
| قصّة الشاشة كإعداد أوّل الدرجة | ZenBubble (`CutoutModeOption`, `TeardropIslandShape`) | موضع آمن تحت القصّة (ISLAND-PREMIUM-01) | إعداد هندسة القصّة صريحًا |
| حارس حلقة إقلاع للوحدة | HyperCool (عدّاد ٣ إقلاعات) | **لا نظير مقيس** | فكرة تستحقّ التبنّي في `mainfiles/` |
| لقطة إعداد الإقلاع | HyperCool (`HC_BOOTCFG`) | `PersistRestoreReceiver` + مخزن تجاوز | نظير وظيفيّ قائم — يُقاس سباقه على جهاز |

**والملاحظة الحاكمة:** المراجع الثلاثة **ليست أوسع منّا في الميزات** — FKM ٤١٠٨ صنفًا لعتاد واحد،
وDevCheck ٣٩٧٢ صنفًا للعرض. نحن **١١٦ شاشة و٦٥ ملفّ عتاد**. الفارق ليس في العدد بل في **شكل
الطبقتين**: عندهم عقد بيانات + وسيط واحد؛ وعندنا شيفرة لكل ميزة + أربعة مسارات امتياز. وهذا بالضبط
محور خطة `ARCHITECTURE-EVOLUTION-NOV26.md`.

---

## 10. ما يُؤخذ كآلية، وما يُرفض صراحةً

**يُؤخذ (فكرة/آلية منصّة — لا كود):**
1. مخطّط القدرات: tunables **بيانات** تُولّد الواجهة والتوثيق (FKM).
2. ثلاث طبقات امتياز معلنة، ودرجة الامتياز تُعرض مع كل رقم (DevCheck).
3. المراقب كصنف مستقلّ قابل للتركيب والبناء المخصّص (DevCheck).
4. `IslandStateManager` + `IslandTouchManager` + أولوية صريحة (ZenBubble).
5. حارس حلقة إقلاع ذاتيّ الإعطاب + لقطة إعداد إقلاع + تأخير تطبيق (HyperCool — كفكرة فقط).

**يُرفض صراحةً:**
- جعل وحدة الإقلاع **تكتب ملفّات النظام** لإصلاح Doze (طبقة `sysconfig` تُبنى في `/system`) — يخالف
  نمطنا «لا نعيد كتابة ملفّات النظام»، وGPL-3 يمنع النقل أصلًا.
- **الدفعة الواحدة عند الإقلاع** كبديل عن arbiter عند الطلب.
- تعطيل تتبّع/تشخيص النظام بلا قياس (سياسة §4 في `HYPERCOOL-STUDY`).
- أي حلّ **خاصّ بمزوّد** يُعمَّم على كل الأجهزة.

---

## 11. ما يحتاج جهازًا — صريحًا

سلوك Shizuku الفعليّ عندنا (هل يُمنح `API_V23` على ROM جذريّ؟) · قراءة headroom الحقيقية (`NaN` يعني
غير مدعوم) · زمن استجابة كل مقبض عتاد · صفحة `trans_stat` على kgsl مقابل Mali · إعداد القصّة على
أجهزتنا · تأخير `apply_delay` وأثره · حارس حلقة الإقلاع (يحتاج إقلاعًا فاشلًا متعمّدًا — **لا يُجرَّب
بلا إذن المالك**) · أيّ حكم SELinux · أيّ أثر مرئيّ للجزيرة.

**وحدّ الأداة نفسه:** `refdump.py` يقرأ **الجداول** لا **الأجسام**؛ فهو يجيب «أيّ API استُدعي» ولا يجيب
«بأيّ معامل ولا بأيّ ترتيب». ومن أراد الثاني فـJADX (كما فُعل مع FKM) أو تفكيك `.dex` إلى smali.

---

## 12. إعادة الاشتقاق (كل أمر هنا شُغّل فعلًا)

```sh
# 0) البصمة ثم الفكّ خارج الشجرة (الأصل لا يُمسّ)
cd /storage/emulated/0/MT2/apks && sha256sum DevCheck_6.61_apks.apk \
  "Franco Kernel Manager_7.0.29.apk" ZenBubble.apk HyperCool_v2.0.zip
mkdir -p /tmp/ref && unzip -oq HyperCool_v2.0.zip -d /tmp/ref/hc20

# 1) الترخيص أولًا — GPL-3 ⇒ صفر كود
head -3 /tmp/ref/hc20/LICENSE
grep -rl 'SPDX-License-Identifier: GPL-3.0' /tmp/ref/hc20 | wc -l

# 2) المانيفست والموارد: aapt2 هو الحكم (وليس قارئًا مكتوبًا بيدنا)
aapt2 dump xmltree --file AndroidManifest.xml "Franco Kernel Manager_7.0.29.apk" | head -40
aapt2 dump badging ZenBubble.apk | head -20

# 3) DEX/ELF: الأداة الجديدة
python3 tools/refdump.py "Franco Kernel Manager_7.0.29.apk" summary --top 20
python3 tools/refdump.py DevCheck_6.61_apks.apk search '^/(sys|proc)/' --max 45
python3 tools/refdump.py ZenBubble.apk classes --filter '^com\.droidbubble'
python3 tools/refdump.py DevCheck_6.61_apks.apk native --filter 'thermal|kgsl|mali'

# 4) JADX (نفس محرّك REA في مسار APK) — ٣٨٤٥ ملفًّا في ٨٤ ثانية على aarch64
/tmp/ref/jadx/bin/jadx --no-res --no-imports -j 4 -d /tmp/ref/jadx-fkm \
  "Franco Kernel Manager_7.0.29.apk"
grep -rhoE '"/sys/[a-zA-Z0-9_/.-]*"' /tmp/ref/jadx-fkm/sources | sort -u | head

# 5) مقارنة مع شجرتنا
find manager/app/src/main/java/nd/max/core/hardware -name '*.kt' | wc -l     # ⇒ 65
grep -rl '"/sys/' manager/app/src/main/java/nd/max --include=*.kt | wc -l   # ⇒ 39
```

**الحالة:** `DONE_WITH_CONCERNS` — التحليل الساكن مكتمل ومقيس؛ وكل حكم سلوكيّ يبقى **يحتاج جهازًا**،
وZenBubble لم يُفكّ بـJADX (حدّ ذاكرة البيئة، لا حدّ أداة).
