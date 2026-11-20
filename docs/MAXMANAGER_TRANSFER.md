# MAXMANAGER_TRANSFER — كود MaxManager المنقول إلى RedLab

> **أمر المالك (2026-11-20):** «انقل كود MaxManager الذي يخدم الهدف. هذا يوفّر وقتًا ويستفيد من عملك
> السابق.» وهو مشروعه، فنقلُه مشروع، ويُسجَّل هنا بدقّة بدل أن يبقى في الذاكرة.

---

## 1. ما نُقل (27 ملفًا)

المصدر: `optmize-main/manager/app/src/main/java/nd/max/core/hardware/` → الهدف:
`redlab/hardware/src/main/kotlin/dev/redlab/hardware/`.

**الطريقة:** حُسب إغلاق الاعتماديات داخل مجلّد `core/hardware` انطلاقًا من ستة ملفات محورية
(`HardwareControlArbiter` · `HardwareCapability` · `ManualControlLocks` · `DriftGuard` ·
`GpuFrequencyWindow` · `CpuStateDistribution`) فنتج **37 ملفًا (8,945 سطرًا)**، ثم بقي منها **27**
بعد استبعاد ما يعتمد على طبقات أخرى (§2).

**التغييرات المُدخلة — ثلاثة فقط، وكلها ميكانيكية:**
1. `package nd.max.core.hardware` ← `dev.redlab.hardware` (وتنظيف الاستيرادات الداخلية).
2. حذف `@Inject`/`@Singleton` (تعليقان لا منطق) — لأن DI عندنا ليس Dagger.
3. ترويسة في كل ملف تذكر المصدر والتاريخ والتغييرات.

**ما لم يتغيّر:** منطق القرار، أسماء الأنواع، الثوابت، التعليقات المقيسة (مثل تعليق `chmod 0666`
وسبب `owners.lock`). ولهذا تبقى قياسات MaxManager دليلًا صالحًا لهذا الكود.

| المجموعة | أمثلة | الأسطر |
| --- | --- | --- |
| المحرّك | `HardwareControlArbiter` · `ControlOwnership` · `HardwareControlKey` | ~780 |
| كتابة متحقَّقة | `WriteVerification` · `WriteReceipt` · `VerifiedControl` · `HardwareVerification` | ~400 |
| سياسات وسقوف | `GpuCeilingPolicy` · `PlatformCeilingAuthority` · `GpuFrequencyWindow` · `DriftGuard` | ~1,100 |
| واجهات عتاد | `CpuHardwareBackend` · `GpuHardwareBackend` · `MemoryUfsHardwareBackend` | ~2,660 |
| MediaTek | `MtkGpuFixedIndex` · `MtkGpuOppTable` | ~270 |
| سجلّات وحالة | `PerAppControlRegistry` · `PerAppRecoveryStore` · `HealthModel` · `ManualControlLocks` | ~870 |
| مسارات/فهرس | `CatalogControlIndex` · `RouteEvidenceFacts` · `HardwareDeviceOverlay` · `HardwareOverlayStore` · `HardwareRepairExecutor` | ~770 |

## 2. ما لم يُنقل بعد — وسببه (لا استبعاد بلا دليل)

عشرة ملفات أُخرجت من النسخة الأولى لأنها تستورد طبقات ليست في `core/hardware`:

| الملف | يعتمد على | الأثر |
| --- | --- | --- |
| `RootFileAccess` | `core.daemon.DaemonIpc` · `core.jni.ProbeBridge` · `core.platform.EventLog` · `core.ipc.RootNodeChannel` · `core.privilege.RootCommands` | يُستدعى من 7 ملفات منقولة ⇒ **مانع ترجمة** |
| `SharedHardwareOwnershipStore` | `android.system.Os` (flock/chown/kill) | يحتاج مقعدًا بديلًا في JVM |
| `HealthLedger` | `android.content.Context` (filesDir) | سهل: يُستبدل بجذر `File` |
| `PerAppHardwareStatus` · `GpuDiscoverySnapshot` | `MaxManagerPaths` | مسارات تطبيق Android |
| `ThermalGuard` · `ThermalCeilingRouter` | `PowerManager` | يُستبدل بمصدر ضغط محقون |
| `AtlasRouteMemory` · `AtlasRouteMemoryFactory` · `AtlasAdaptiveExecutor` | `core.atlas.*` (6 أنواع) | حزمة كاملة لم تُنقل |

**لهذا لا تُضاف `:hardware` إلى `settings.gradle.kts` بعد:** وحدة لا تُترجم لا تُشحن كتقدّم، والأسوأ أن
تكسر CI. تبقى الملفات في الشجرة مع هذه الوثيقة، وتُفعَّل الوحدة عند اكتمال إغلاقها.

## 3. الخطوة التالية المحدَّدة (بالترتيب)

1. نقل `core/privilege` (‏`RootCommands`, `PrivilegedShell`, `ExecutionMode`) + `core/atlas` — لأنهما
   السببان الوحيدان لبقاء 7 ملفات معطّلة.
2. استبدال ثلاث لمسات Android بثلاثة مقاعد/واجهات: `Context→File`، `PowerManager→ThermalStatusSource`،
   `Os.flock→FileChannel.lock` (وهذا بالضبط ما وُجدت `ControlSeat` من أجله).
3. تفعيل `:hardware` في `settings.gradle.kts` مع نقل اختباراته الأصلية (‏`DriftGuardTest`,
   `GpuFrequencyWindowTest`, `CpuStateDistributionTest`, `HardwareControlArbiterTest`, `ManualControlLocksTest`).
4. إضافة `python3 -m ...`؟ لا: البوابة هي `./gradlew :hardware:test` في CI — ولا ادّعاء قبل خضرتها.

## 4. ما لا يُدَّعى

- الملفات المنقولة **لم تُترجم بعد** في هذه البيئة، ولم تُشغَّل اختباراتها ⇒ حالتها `PORTED_NOT_BUILT`
  في `REBUILD_LEDGER`، لا `REBUILT`.
- لم يُنقل أي كود إلى MaxManager، ولم يُعدّل مستودعه.
