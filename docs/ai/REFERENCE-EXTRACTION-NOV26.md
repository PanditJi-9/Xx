# تشريح المراجع — الاستخراج الكامل (2026-11-05)

> مصدر الأرقام هنا **قياسٌ جرى في هذه الجلسة**، لا نقلًا عن المستندات السابقة. المستودع الدائم
> للمخرجات: `/storage/emulated/0/MT2/ref-analysis/` (فهرسه في `INDEX.md` هناك، وبصمات الأصل في
> `MANIFEST.txt`). والأصل في `/storage/emulated/0/MT2/apks/` بلا مساس.

## 1. ما فُكّ — بالأرقام

| الهدف | ملفات Java | حجم | مسارات sysfs/proc | أوامر shell/API | أذونات | أخطاء jadx |
| --- | --- | --- | --- | --- | --- | --- |
| FKM 7.0.29 | 3,845 | 25 MB | 351 | 28 | 6 | 27 |
| ZenBubble | 19,124 | 184 MB | 10 | 22 | 4 | خرج=1 |
| DevCheck 6.61 | 3,756 | 35 MB | 227 | 17 | 94 | خرج=3 |
| HyperCool 2.0 | — (37 ملفًا) | — | — | سكربتات كاملة | — | — |

**الأداة:** `jadx 1.5.6` على `openjdk 17.0.20.1` (aarch64)، `--no-res --no-imports`، `-Xmx1600m`.
زمن الفكّ: FKM ١د٤٨ث · ZenBubble ١٤د٥٠ث · DevCheck ٣د٣٩ث.

**والمحرّك هو نفسه الذي يستدعيه REA للـAPK**؛ REA نفسها خادم MCP يحتاج مضيف x64 + Node 22+، والمضيف
هنا aarch64 — فالاستدعاء المباشر هو البديل الموثَّق لا التفاف.

## 2. تغطية مسارات العتاد — الفجوة الحقيقية

عندنا **١٨٨** مسارًا حرفيًّا في `manager/app/src/main/java`. المقارنة المباشرة:

| المرجع | مشترك معنا | عنده فقط |
| --- | --- | --- |
| FKM | **٢٢** من ٣٥١ | **٣٢٩** |
| DevCheck | **٢٣** من ٢٢٧ | **٢٠٤** |

**وأين يتكدّس الناقص** (أعلى المجموعات، بعد استثناء القوالب):

```text
FKM   77 /sys/module        ← معاملات موديولات (kernel modules) — لا نقرأها إطلاقًا
      75 /sys/class         ← leds · backlight · power_supply · sound · mdnie …
      63 /sys/devices       ← مسارات SoC محدّدة (msm/mediatek/exynos)
      19 /sys/kernel        ← debugfs-like knobs
      17 /sys/klapse        ← KLapse (نواة مخصّصة)
      17 /proc/sys          ← VM / sched / net — عندنا ٦ منها فقط
      16 /dev/stune         ← cgroups schedtune (EAS)
      14 /proc/ppm          ← MTK PPM (userlimit/hard_userlimit)
DevCheck 111 /sys/devices  ·  46 /sys/class  ·  15 /sys/kernel  ·  8 /sys/power  ·  8 /sys/block
```

**القراءة الصحيحة لهذه الأرقام:** العدد ليس هدفًا. FKM يستهدف نوى مخصّصة (KLapse، `schedtune`،
`ppm`) بأسماء تتغيّر بين الأجهزة؛ نسخ ٣٢٩ مسارًا حرفيًّا يعني **٣٢٩ مسارًا ميتًا** على معظم الهواتف.
ما يصلح لنا هو **المجموعات العامّة** التي توجد على كل نواة لينكس: `/proc/sys/vm/*`،
`/proc/sys/net/*`، `/sys/block/*/queue/*`، `/sys/class/leds/*`.

## 3. الأوامر — ما يُدار بلا جذر

مستخرج FKM (٢٢ أمرًا، أهمّها):

```text
wm size | wm density | reset            ← دقّة/كثافة الشاشة (عندنا: DISPLAY_RESOLUTION)
svc wifi <on|off> · svc power reboot
settings put system screen_brightness · accelerometer_rotation
settings put global low_power
settings put secure location_providers_allowed
dumpsys batterystats --charged · dumpsys activity recents · dumpsys display
```

مستخرج ZenBubble (٢٢ أمرًا، **كلها بلا جذر** إذا مُنح `WRITE_SECURE_SETTINGS`):

```text
settings put secure icon_blacklist '<csv>'          ← إخفاء رموز شريط الحالة
settings put global  policy_control 'immersive.status=*'
settings put secure  notification_badging 0|1
settings put {secure|system} show_notification_icon 0|1
settings put {secure|system} status_bar_show_notification_icon 0|1
settings put secure  status_bar_show_badges 0|1
pm grant <pkg> <perm>
```

**وهذا آخر سطر هو درس عمليّ:** ZenBubble يكتب **المفتاح نفسه بأربعة أسماء** (`secure`/`system` ×
`show_notification_icon`/`status_bar_show_notification_icon`) لأن الرومات تختلف في أيّها يُقرأ. كتابة
اسم واحد فقط تفشل صامتة على نصف الأجهزة.

## 4. DevCheck — ما فيه فعلًا

أصناف المراقبة المقيسة (بعد حذف المكتبات):

```text
BatteryMonitorService · CPUMonitorWindow · GPUMonitorWindow · RamMonitorWindow
NetworkMonitorWindow · TempMonitorWindow · CurrentMonitorWindow · SignalMonitorWindow
FPSMonitorWindow · GameFpsMonitorWindow · LoadMonitorWindow · CustomMonitorWindow
DashboardSensorsActivity · SensorActivity · LightSensorActivity · NetworkMapperActivity
CPUTimeActivity · GpuTimeActivity · BatteryMonitor2Db (سجلّ تاريخي في SQLite)
ThermalHeadroomNative · PowerManager$OnThermalStatusChangedListener
```

**ثلاث آليات تستحقّ الأخذ (بلا كود):**

1. **`OnThermalStatusChangedListener`** — حالة `PowerManager.getThermalStatus()` (API 29) بدل قراءة
   `sysfs`. عندنا **صفر** استخدام (`grep`)، والهامش (ADPF) أُضيف في هذه الجولة وحده.
2. **`CustomMonitorWindow` + `CustomMonitorBuilderActivity`** — مراقب يبنيه المستخدم من قائمة
   مقاييس. مرفوض في v1 بقرار سابق، ويُعاد النظر فيه فقط إن صار عندنا سجلّ مقاييس موحّد.
3. **`BatteryMonitor2Db`** — تاريخ في SQLite لا رسم لحظيّ. عندنا لقطات في الذاكرة؛ الفارق أن
   الاتجاه (تدهور البطارية عبر أسابيع) يحتاج تخزينًا.

## 5. ZenBubble — UX

المصادر مُشوَّشة بـR8 (`a/b/c`)، فما يُقرأ منها هو **السلوك** لا التصميم: مفاتيح `settings` أعلاه،
و`androidx.window.extensions` (١٥٢ مرجعًا — أكبر استيراد غير Android SDK)، و`USE_FULL_SCREEN_INTENT`
و`SYSTEM_ALERT_WINDOW`. **والشكل البصريّ لا يُستخرج من الكود** — يحتاج موارد (`--no-res` مستخدم هنا)،
ولا يُنقل شكلٌ أصلًا (ADR-54 §4).

## 6. HyperCool — الآليات (GPL-3: صفر كود)

ثلاث آليات مقروءة من السكربتات، وكلها **مفاهيم** تُعاد كتابتها:

| الآلية | كيف تعمل | عندنا |
| --- | --- | --- |
| **وضع الأمان ضدّ حلقة الإقلاع** | عدّاد `boot_tries` يزيد في `post-fs-data.sh`، ويُصفَّر عند `sys.boot_completed=1`؛ تجاوز الحدّ ⇒ تعطيل الموديول وكتابة `safe_mode` | **غائب** — يحتاج إقلاعًا فاشلًا متعمّدًا للتحقّق |
| **لقطة إعداد عند الإقلاع** | `service.sh` ينسخ `default.conf` إلى `boot.conf` ويطبّق **اللقطة**، فتعديل المستخدم أثناء الإقلاع ينتظر الإقلاع التالي | غائب (إعداداتنا تُطبَّق فورًا) |
| **حارس عند التعطيل** | `hc_guard.sh` يعيد قيم الروم من `backup/rom.bak` عند ظهور ملفّ `disable` | عندنا `PersistRestoreReceiver` — قريب لا مطابق |

## 7. Backlog — مرتّب بما يستحقّ فعلًا

| # | البند | المصدر | الحالة | الحائل |
| --- | --- | --- | --- | --- |
| 1 | **إخفاء رموز شريط الحالة + الوضع الغامر** | ZenBubble | **شُحن الآن** (`StatusBarIcons` + بلاط) | — |
| 2 | مجموعة `proc/sys/vm` كاملة (dirty_*, vfs_cache_pressure, min_free_kbytes, overcommit) | FKM | **شُحن الآن** (عشرة مقابض + كتالوج `vm.*` + صفّ `KERNEL_VM`) | واجهة الشاشة لاحقًا |
| 3 | `PowerManager.getThermalStatus()` (حالة الحرارة) | DevCheck | **شُحن الآن** (`ThermalStatus` + صفّ `THERMAL_STATUS`) | — |
| 4 | تعدّد أسماء مفتاح الإشعار (٤ أسماء) | ZenBubble | **مأخوذ** في `StatusBarIcons` | — |
| 5 | `wm size/density` كتحكّم دائم | FKM | عندنا `DISPLAY_RESOLUTION` | يحتاج تحقّق جهاز |
| 6 | مجموعة `/sys/class/leds` | FKM | غائب | مسارات آمنة للقراءة، كتابة على جهاز |
| 7 | عدّاد إقلاع آمن | HyperCool | غائب | **إذن المالك + إقلاع فاشل متعمّد** |
| 8 | سجلّ بطارية SQLite (اتجاه طويل) | DevCheck | غائب | تصميم تخزين + هجرة |
| 9 | باني مراقب مخصّص | DevCheck | مرفوض v1 | قرار المالك |
| 10 | `schedtune`/`ppm`/`KLapse` | FKM | **مرفوض** | أسماء نوى مخصّصة: مسارات ميتة على ٩٠٪ من الأجهزة |

## 8. ما شُحن في هذه الدفعة

| الملف | المحتوى |
| --- | --- |
| `core/platform/StatusBarIcons.kt` | تعداد ١٧ رمزًا · `parse`/`encode` نقيّان **يحفظان رموز الروم المجهولة** · `hide`/`show`/`toggle`/`setAllHidden` · `immersive` · `notificationBadging` · `notificationIcon` · حقن قراءة/كتابة للقياس |
| `TileService/StatusBarTileService.kt` | بلاط إعدادات سريعة يقلب «الشريط النظيف» |
| `res/drawable/ic_statusbar_clean.xml` | أيقونة البلاط |
| `values{,‑ar}/strings.xml` | ٤ نصوص (إنجليزي + عربي فقط) |
| `AndroidManifest.xml` | خدمة البلاط بصلاحية `BIND_QUICK_SETTINGS_TILE` |
| `StatusBarIconsTest` | ٧ دعاوى (منها: الإخفاء لا يمحو رموز الروم) |
| `core/platform/VmTuning.kt` | عشرة مقابض `/proc/sys/vm` بحدود معلنة · `parse` صارم · كتابة بتحقّق قراءة-بعد-كتابة · خمس نتائج معلنة · عبر `RootFileAccess` وحده |
| `core/platform/ThermalStatus.kt` | حالة الخنق من `PowerManager.getThermalStatus` · سبعة رموز + `UNKNOWN` · `isThrottling` |
| `assets/control_catalog.json` | **+١٠ مداخل `vm.*`** (٤١ مدخلًا · ٦٩ مرشّحًا · ١٥ بادئة) |
| `HardwareFeature` | `KERNEL_VM` (تُشتقّ من الكتالوج وحده) · `THERMAL_STATUS` |
| `VmTuningTest` · `ThermalStatusTest` | ٨ + ٦ دعاوى |
| `ui/subscreens/VmTuningSection.kt` | قسم «مقابض نواة VM» داخل `MemoryUfsScreen` · قراءة أوّلية على `Dispatchers.IO` · `±step` بحدّي المدى · رمز النتيجة كما هو |
| `core/platform/VmTuning.kt` (تحديث) | `VmTunable.step` لكل مفتاح (١ · ٥ · ١٠ · ١٠٠ · ١٠٢٤) — الخطوة مشتقّة من مقياس المعامل |
| `core/hardware/ZramHardwareBackend.kt` · `core/persist/PersistRestorer.kt` | **كاتب واحد لـ`swappiness`**: `echo` و`writeAndRead` المحلّيان حُلّا محلّهما `VmTuning.system().set(...)` بالقراءة المرتجعة |
| `core/hardware/HardwareCapabilityResolver.kt` | نسبة `getThermalHeadroom(10)` تُلحق بـ`evidence` ⇒ تُعرض في بطاقة المصفوفة بلا لمس Compose |
| `values{,‑ar}/strings.xml` | ١٣ نصًّا (القسم · التحميل · الغياب · المدى · الزرّان · النتائج الخمس) |

**والواجهة بلا route جديدة عمدًا:** القسم يُدرج في `MemoryUfsScreen` لأنها الشاشة التي تقيس ضغط الذاكرة
أصلًا؛ وroute مستقلّة كانت تكلّف مدخل تنقّل + دور + كتالوج لأجل عشرة صفوف. **والانحراف المعلن:** القسم
ينادي `VmTuning.system()` بلا ViewModel، والكتابة تبقى عبر `RootFileAccess` وحده.
**وما بقي خارج المسار الواحد:** `PanelDevice.kt:116-117` (`drop_caches`/`compact_memory`) — مُشغِّلان
لا مقبضان، مسجَّلان في `NEXT_TASK`.

**وحدود البيانات معلنة:** `origin: MEASURED` تعني «الوجود قيس على نواة القياس هنا»؛ و`vm.extra_free_kbytes`
وُسم `DERIVED` لأنه **غير موجود** على هذه النواة (مقبض أندرويد خاصّ) — فالوسم يقول الحقيقة لا التمنّي.

**القياس:** منطق القائمة أُعيد تنفيذه في بايثون على الحالات السبع ⇒ **٧/٧**؛ `kt_balance` ٣٢٠٤/٠ ·
`code_health` ✓ · `i18n_coverage` ٠ عوائق · `design_tokens` ✓ · الكتالوج ٠.
**والترجمة غير مُتحقَّقة في هذه البيئة** (لا JVM/SDK)، و`resource_compile` غير مُتحقَّقة (لا `aapt2`).

## 9. إعادة الاشتقاق

```sh
python3 /storage/emulated/0/MT2/ref-analysis/mine.py fkm
comm -23 /storage/emulated/0/MT2/ref-analysis/extract/fkm.paths.txt /tmp/ours.paths.txt
```
