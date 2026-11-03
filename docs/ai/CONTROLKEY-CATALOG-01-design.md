# CONTROLKEY-CATALOG-01 — كتالوج القدرات المُعلن: التصميم، البذرة المقيسة، واختبار التطابق

**التاريخ:** 2026-11-03 · **النوع:** تصميم بيانات **بلا تنفيذ** · **صفر تعديل منتج.**
**الأساس:** `MAXMANAGER-TRANSFORM-01-spec.md` §7.١ §8 · `ARCHITECTURE-EVOLUTION-NOV26.md` §2 (المحور الأول).
**الغرض:** تحويل «دعم CPU» من **صنف يعرف مسارات** إلى **مدخل بيانات يُعلن ما يعرفه وبأيّ صلاحية**.

---

## ١. المشكلة المقيسة — لا موصوفة

| القياس (في هذه الجلسة) | القيمة | ماذا يعني |
| --- | --- | --- |
| مسارات `/sys` **متمايزة** داخل `core/hardware` | **٤١** | كل واحد منها مقبض محتمل، ولا سجلّ واحد يجمعها |
| مسارات `/proc` متمايزة | **١٥** | نفس الشيء، ومصادر قراءة لا كتابة |
| ملفّات `core/hardware` تحمل `/sys` حرفيًّا | **٩** من ٦٥ | النثر يوزّع المعرفة، والسجلّ غائب |
| ملفّات `*Backend` | **٦** (`Cpu` · `Gpu` · `MemoryUfs` · `Zram` · `Charging` · `TouchControl`) | القدرة موزّعة على ستة أصناف لا عقدًا |
| `HardwareControlKey` | **`object`** · **١٤ `const val` بادئة** + مصانع مشتقّة · **ليس `enum`** | اختبار التطابق يجب أن يقيس البوادئ والمصانع، لا عدًّا ثابتًا |
| بادئات التجميع (أعلى تكرار) | `/sys/devices/system` ×١٠ · `/sys/block/zram` ×٥ · `/sys/module/ged` ×٤ · `/sys/devices/virtual` ×٤ · `/sys/class/touch` ×٤ | ثلاث عائلات SoC على الأقل (MTK ged، Qualcomm kgsl، generic devfreq) |

⇒ **الخلاصة:** القدرة موجودة، والسجلّ غائب. والكتالوج **لا يضيف قدرة**؛ يجمعها في مكان واحد قابل للقياس.

---

## ٢. العقد — ستة حقول لا سابع

```kotlin
// يُقرأ من data/control_catalog.yaml وقت البناء؛ لا يُكتب بيد في Kotlin.
data class ControlEntry(
    val key: String,                    // مفتاح مُعاير، مشتقّ من HardwareControlKey حرفيًّا
    val kind: ControlKind,              // FREQUENCY_RANGE | ENUM | BOOL | INT | STRING | PERCENT
    val candidates: List<String>,       // مرشّحات تُقاس بالترتيب، لا افتراضات
    val verify: VerifyPolicy,           // READ_BACK_EQUAL | READ_BACK_TOLERANCE | NONE
    val privilege: PrivilegeSpec,       // read: Set<Tier> · write: Set<Tier>
    val danger: Danger,                 // LOW | MEDIUM | HIGH | PROTECTED
)
```

**ولكلٍّ قاعدة واحدة معلنة:**
- `key` **يُشتقّ** من `HardwareControlKey` (بادئة + رمز)، فلا مفتاح ثانٍ لنفس المقبض — وهذا ما يحرسه `ControlPlaneArchitectureTest.canonicalKeysAreNotReinvented` اليوم.
- `candidates` **مرشّحات**: أول مسار موجود **وقابل للقراءة بالطبقة المتاحة** يفوز، ويُسجَّل الفائز مع بصمة الجهاز.
- `verify` **إلزاميّ**: `NONE` لا تُستعمل إلا حيث لا قراءة ممكنة، **وتُعلن** (لا تُسكَت).
- `danger: PROTECTED` ⇒ **الوسيط يرفض** قبل الواجهة: حراريات، watchdog، حماية طاقة.
- `Unsupported(reason)` بأربعة أسباب فقط: `NO_NODE` · `NO_PERMISSION` · `DENIED_BY_POLICY` · `UNKNOWN`.

---

## ٣. البذرة — من المسارات المقيسة، مصنَّفة

هذه **ليست** الكتالوج النهائي؛ هذه **البذرة ٥٦ مسارًا** التي يجب أن تُغطّى أو تُعلن `Unsupported`:

### ٣.١ التردّد والتحكّم — عائلة CPU

| المفتاح المقترح | المرشّحات (مقيسة) | kind | danger |
| --- | --- | --- | --- |
| `cpu_limits:<policy>` | `/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq` · `/sys/devices/system/cpu/$cpu/cpufreq/*` | FREQUENCY_RANGE | MEDIUM |
| `cpu_boost` | `/sys/devices/system/cpu/cpufreq/boost` | BOOL | MEDIUM |
| `cpu.<n>.online` | `/sys/devices/system/cpu/cpu$cpu/online` · `/sys/devices/system/cpu/$it/online` | BOOL | HIGH |
| MTK thermal coupling | `/sys/devices/virtual/thermal/thermal_message/cpu_limits` · `…/sconfig` | STRING | **PROTECTED** (اقرأ فقط) |

### ٣.٢ GPU — ثلاث عائلات في الشجرة نفسها

| المفتاح | المرشّحات | عائلة | ملاحظة |
| --- | --- | --- | --- |
| `gpu_frequency:<dev>` | `/sys/class/kgsl/kgsl-3d0/devfreq/*` | Qualcomm | devfreq قياسيّ |
| `gpu_frequency:<dev>` | `/sys/module/ged/parameters/gpu_cust_upbound_freq` · `gpu_dvfs_enable` · `gpu_idle` | MTK ged | أسماء خاصّة بالمورّد |
| `gpu_frequency:<dev>` | `/proc/gpufreqv2/fix_target_opp_index` · `gpu_working_opp_table` · `/proc/gpufreq/gpufreq_opp_freq` | MTK gpufreq | `/proc` لا `/sys` ⇒ طبقة قراءة مختلفة |
| `gpu.load` | `/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage` · `/sys/kernel/ged/hal/current_freqency` | مراقبة فقط | مصدر المراقب `gpuLoad` |

**وهذه أهمّ نتيجة تصميمية:** نفس **المفتاح المعاير** له ثلاث مرشّحات لعائلات مختلفة. لو كان المفتاح
«صنفًا يعرف المسار» لَلزم ثلاث شاشات؛ وهو مفتاح واحد ⇒ شاشة واحدة.

### ٣.٣ اللمس — أوسع انتشارًا من المتوقَّع (٨ مسارات لسؤال واحد)

`/sys/android_touch/*` · `/sys/class/touch/touch_dev/*` · `/sys/class/touchscreen/*` ·
`/sys/devices/virtual/touch/touch_dev/*` · `/sys/touchpanel/*` · `/proc/touchpanel/*` · `/proc/touch_boost/enable`
⇒ `touch_boost:<path>` موجود كبادئة، والكتالوج يمنحه **قائمة مرشّحات** لا مسارًا واحدًا محفورًا.

### ٣.٤ الذاكرة والتخزين

| المفتاح | المرشّحات | ملاحظة |
| --- | --- | --- |
| `zram.<n>.size` | `/sys/block/zram0` · `/sys/block/zram*` | نمط بدل رقم محفور |
| `mem_frequency:<dev>` | `/sys/class/devfreq/*` | |
| `ufs_frequency:<dev>` | `/sys/class/devfreq/*` | |

### ٣.٥ الطاقة والحرارة (قراءة)

`/sys/class/power_supply/battery` · `/sys/class/thermal` · `/proc/pressure/memory` · `/proc/swaps` · `/proc/loadavg`
⇒ كلها **قراءة فقط**، وتغذّي المراقبات لا التحكّم.

### ٣.٦ ما يُعلن `PROTECTED` صراحةً

`thermal_message/*` · أي `watchdog` · أي `power_safety` · أي عقدة غير معروفة **تحت `thermal`**.
**والقرار:** تُقرأ، ولا تُكتب، ولا تُعرض قابلًة للكتابة — بأيّ طبقة امتياز.

---

## ٤. مسار التوليد — بلا مرآة يدوية

```
data/control_catalog.yaml
        │  tools/gen_control_catalog.py  (بناء)
        ▼
generated/ControlCatalog.kt   (مُولَّد · في .gitignore أو مولَّد في CI)
        │
        ├─► HardwareCapabilityResolver  يقرأه بدل مساراته المحفورة
        ├─► Monitor (M3)  يبني مراقباته منه
        └─► M4: جدول قدرات الجهاز يُولَّد منه للتوثيق
```

**والقاعدة:** المرآة اليدوية **ممنوعة**. إن تعدّدت النسخة انزاحت؛ والانزياح هنا يعني مقبضًا له كتابتان.

---

## ٥. اختبار التطابق — كيف يُقاس «١٠٠٪»

```
test_atlas_jvm::catalogCoversEveryKey
  for each prefix in HardwareControlKey.<14 const val>
      for each sample symbol produced by its factory
          assert catalog has an entry whose key starts with prefix
  assert catalog keys ∪ HardwareControlKey factories == expected set
  assert no catalog key is built outside HardwareControlKey   ← يمنع المفتاح الثاني
```

**وثلاث حالات سلبية إلزامية** (أداة لا تُكسر لا تقيس):
1. حذف مدخل من الكتالوج ⇒ الاختبار يُسمّي البادئة الناقصة.
2. إضافة مفتاح حرفيّ في `core/hardware` لا يمرّ بـ`HardwareControlKey` ⇒ يُسقط الاختبار.
3. مدخل بلا `candidates` ⇒ يُعلن `NO_NODE` ولا يمرّ صامتًا.

**ومعيار القبول الحرفيّ (من المواصفة §7.١):** التطابق **١٠٠٪** بين مفاتيح `HardwareControlKey` (البوادئ
الـ١٤ + المصانع) وبين مداخل الكتالوج — لا «أغلب المفاتيح».

---

## ٦. ما لا يفعله هذا التصميم

1. **لا يُنفَّذ الآن.** صفر ملفّ كتالوج، صفر مولّد، صفر تعديل على `HardwareCapabilityResolver`.
2. **لا ينسخ مسارًا من مرجع كافتراض عامّ.** كل مسار هنا **مقيس من شجرتنا** (`grep` على `core/hardware`)؛
   ومسارات FKM/DevCheck تُضاف **مرشّحًا** يُقاس على الجهاز، لا قيمة افتراضية.
3. **لا يُغيّر `HardwareControlKey`** — يشتقّ منه.
4. **لا يُدخل اعتمادًا** — YAML + stdlib كافيان.
