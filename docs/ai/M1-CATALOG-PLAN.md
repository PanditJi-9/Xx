# M1-CATALOG-PLAN — خطّة تنفيذ كتالوج `ControlKey` (بذرة كاملة + مولّد + اختبار تطابق)

**التاريخ:** 2026-11-03 · **المرحلة:** **M1** (بعد M0 أخضر) · **النوع:** خطّة تنفيذ **بلا كود الآن**.
**الأساس:** `CONTROLKEY-CATALOG-01-design.md` (التصميم) · `MAXMANAGER-TRANSFORM-01-spec.md` §7.١ §8
· `FEATURE_MAPPING.md` (مصدر كل مسار) · ADR-87 (مقترح).

> **حدّ هذا الملفّ:** كل مسار فيه **مقيس** من شجرة اليوم بـ`grep` على `core/hardware` (الأمر في §8).
> والمسارات **المشتقّة** (نمط مجلّد لم يُرَ حرفيًّا) تُعلَن `origin: DERIVED` ولا تُعامَل مقيسةً.

---

## ١. الهدف ومعيار القبول

| البند | المعيار |
| --- | --- |
| الهدف | كل مقبض عتاد **مدخل بيانات** يُعلن: نوعه، مرشّحاته، تحقّقه، امتيازه، خطره |
| القبول ١ | كل بادئة من **بوادئ `HardwareControlKey` الـ١٤** لها مدخل واحد على الأقل |
| القبول ٢ | كل مسار **مقيس** في `core/hardware` (**٤١** `/sys` + **١٥** `/proc`) ظاهر في `candidates` أو مُعلَن في `unmapped` بسببه |
| القبول ٣ | **صفر** مدخل بـ`candidates` فارغة بلا `reason: NO_NODE` |
| القبول ٤ | **صفر** مدخل `danger: PROTECTED` يقبل الكتابة في `privilege.write` |
| القبول ٥ | اختبار نقيّ يمرّ **ويُكسر** بالحالات السلبية الثلاث (§7.3) |

---

## ٢. المخرجات — أربعة ملفّات، لا خامس

| # | الملفّ | النوع | في git؟ |
| --- | --- | --- | --- |
| ١ | `manager/app/src/main/assets/control_catalog.yaml` | مصدر البيانات | **نعم** (بيانات، لا كود) |
| ٢ | `tools/gen_control_catalog.py` | المولّد (stdlib) | نعم |
| ٣ | `manager/app/src/main/java/nd/max/core/hardware/generated/ControlCatalog.kt` | مُولَّد | **لا** (في `.gitignore`) |
| ٤ | `manager/app/src/test/java/nd/max/core/hardware/ControlCatalogParityTest.kt` | اختبار التطابق | نعم |

**ولماذا المُولَّد غير ملتزم:** المرآة الملزَمة تنزاح. وأداة `code_health` ترى ملفّات `.kt` — فمُولَّدٌ
بـ٣٠٠ مدخل يضخّم عدّ الملفّات ويُشوّه سقوف الدَّين. ⇒ يُولَّد في البناء (واختبار التطابق يُولّده في `setUp` إن غاب).

---

## ٣. المخطّط — سبعة حقول كاملة

```yaml
version: 1
entries:
  - key: "cpu_limits:{policy}"     # يطابق بادئة HardwareControlKey حرفيًّا
    transport: NODE                 # NODE | PROC | PROP | AUDIO | SETTINGS
    kind: FREQUENCY_RANGE           # FREQUENCY_RANGE | ENUM | BOOL | INT | STRING | PERCENT
    candidates:
      - { path: "/sys/…", origin: MEASURED }   # MEASURED | DERIVED
    verify:
      policy: READ_BACK_EQUAL       # READ_BACK_EQUAL | READ_BACK_TOLERANCE | NONE
      tolerance: 0
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE, DAEMON] }
    danger: MEDIUM                  # LOW | MEDIUM | HIGH | PROTECTED
unmapped:                           # المسارات المقيسة التي لا مقبض لها بعد
  - { path: "/proc/$pid/stat", reason: ROOT_ONLY, note: "AppMonitor يقرأها مباشرة" }
```

**وأربع قواعد في المخطّط:**
1. `key` **يُشتقّ** من `HardwareControlKey` (بادئة + رمز). المولّد **يرفض** مفتاحًا لا يبدأ ببادئة معلنة.
2. `transport: AUDIO | SETTINGS | PROP` للمفاتيح التي **ليست عقدة**: مستويات الصوت، المؤثّر، التمويه، تجاوزات الإقلاع. (هذا كشفته البذرة: **البوادئ الـ١٤ ليست كلها `/sys`**.)
3. `privilege.write: []` يعني **قراءة فقط**، ولا يُخفي ذلك. و`danger: PROTECTED` يُجبره على `[]`.
4. `tolerance: 0` هو الافتراض؛ أي قيمة أخرى تحتاج `settle_ms` معلنة.

---

## ٤. البذرة — ٥٦ مسارًا مصنَّفًا (YAML كامل)

```yaml
version: 1

entries:
  # ─────────────────────────── CPU ───────────────────────────
  - key: "cpu_limits:{policy}"
    transport: NODE
    kind: FREQUENCY_RANGE
    candidates:
      - { path: "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", origin: MEASURED }
      - { path: "/sys/devices/system/cpu/{cpu}/cpufreq",                   origin: MEASURED }
      - { path: "/sys/devices/system/cpu/$cpu/cpufreq",                    origin: MEASURED }
      - { path: "/sys/devices/system/cpu/cpufreq/policy{policy}/scaling_max_freq", origin: DERIVED }
      - { path: "/sys/devices/system/cpu/cpufreq/policy{policy}/scaling_min_freq", origin: DERIVED }
    verify: { policy: READ_BACK_EQUAL, tolerance: 0 }
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE, DAEMON] }
    danger: MEDIUM

  - key: "cpu_boost"
    transport: NODE
    kind: BOOL
    candidates:
      - { path: "/sys/devices/system/cpu/cpufreq/boost", origin: MEASURED }
    verify: { policy: READ_BACK_EQUAL, tolerance: 0 }
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE] }
    danger: MEDIUM

  - key: "cpu_core_online:{n}"         # مشتقّ من cpu.<n>.online في الشجرة
    transport: NODE
    kind: BOOL
    candidates:
      - { path: "/sys/devices/system/cpu/cpu{cpu}/online",  origin: MEASURED }
      - { path: "/sys/devices/system/cpu/cpu$cpu/online",    origin: MEASURED }
      - { path: "/sys/devices/system/cpu/{it}/online",      origin: MEASURED }
      - { path: "/sys/devices/system/cpu/$it/online",        origin: MEASURED }
    verify: { policy: READ_BACK_EQUAL, tolerance: 0 }
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE] }
    danger: HIGH                        # إطفاء نواة على جهاز ضعيف = تجميد محتمل

  - key: "cpu_topology"                 # قراءة فقط: تجميع النوى والحكام
    transport: NODE
    kind: STRING
    candidates:
      - { path: "/sys/devices/system/cpu",           origin: MEASURED }
      - { path: "/sys/devices/system/cpu/cpufreq",   origin: MEASURED }
    verify: { policy: NONE }
    privilege: { read: [NONE], write: [] }
    danger: LOW

  # ─────────────────────────── GPU: ثلاث عائلات ───────────────────────────
  - key: "gpu_frequency:{device}"
    transport: NODE
    kind: FREQUENCY_RANGE
    candidates:
      # Qualcomm (kgsl)
      - { path: "/sys/class/kgsl/kgsl-3d0",           origin: MEASURED }
      - { path: "/sys/class/kgsl/kgsl-3d0/devfreq",   origin: MEASURED }
      # MTK ged (sysfs)
      - { path: "/sys/module/ged/parameters/gpu_cust_boost_freq",   origin: MEASURED }
      - { path: "/sys/module/ged/parameters/gpu_cust_upbound_freq", origin: MEASURED }
      - { path: "/sys/kernel/ged/hal/custom_upbound_gpu_freq",      origin: MEASURED }
      # MTK gpufreq (proc)
      - { path: "/proc/gpufreq/gpufreq_opp_freq",          origin: MEASURED }
      - { path: "/proc/gpufreqv2/fix_target_opp_index",    origin: MEASURED }
      # عامّ (devfreq)
      - { path: "/sys/class/devfreq",                      origin: MEASURED }
    verify: { policy: READ_BACK_EQUAL, tolerance: 0 }
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE] }
    danger: MEDIUM

  - key: "gpu_governor_policy:{device}"
    transport: NODE
    kind: ENUM
    candidates:
      - { path: "/sys/module/ged/parameters/gpu_dvfs_enable", origin: MEASURED }
      - { path: "/sys/module/ged/parameters/gpu_idle",        origin: MEASURED }
    verify: { policy: READ_BACK_EQUAL, tolerance: 0 }
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE] }
    danger: MEDIUM

  # ─────────────────────────── اللمس: ثمانية عائلات ───────────────────────────
  - key: "touch_boost:{path}"
    transport: NODE
    kind: BOOL
    candidates:
      - { path: "/sys/android_touch/game_switch_enable",              origin: MEASURED }
      - { path: "/sys/class/touch/touch_dev/game_switch_enable",      origin: MEASURED }
      - { path: "/sys/class/touchscreen/game_switch_enable",          origin: MEASURED }
      - { path: "/sys/devices/virtual/touch/touch_dev/game_switch_enable", origin: MEASURED }
      - { path: "/sys/touchpanel/game_switch_enable",                 origin: MEASURED }
      - { path: "/proc/touchpanel/game_switch_enable",                origin: MEASURED }
      - { path: "/proc/touch_boost/enable",                           origin: MEASURED }
    verify: { policy: READ_BACK_EQUAL, tolerance: 0 }
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE] }
    danger: LOW

  - key: "touch_gesture:{path}"         # النقر المزدوج/الإيقاظ
    transport: NODE
    kind: BOOL
    candidates:
      - { path: "/sys/android_touch/double_tap_enable",              origin: MEASURED }
      - { path: "/sys/android_touch/doubletap2wake",                 origin: MEASURED }
      - { path: "/sys/class/touch/touch_dev/double_tap_enable",      origin: MEASURED }
      - { path: "/sys/class/touchscreen/double_tap_enable",          origin: MEASURED }
      - { path: "/sys/touchpanel/double_tap_enable",                 origin: MEASURED }
      - { path: "/proc/touchpanel/double_tap_enable",                origin: MEASURED }
    verify: { policy: READ_BACK_EQUAL, tolerance: 0 }
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE] }
    danger: LOW

  - key: "touch_device_index"
    transport: NODE
    kind: STRING
    candidates:
      - { path: "/sys/android_touch",                       origin: MEASURED }
      - { path: "/sys/class/touch",                          origin: MEASURED }
      - { path: "/sys/class/touch/touch_dev",                origin: MEASURED }
      - { path: "/sys/class/touchscreen",                    origin: MEASURED }
      - { path: "/sys/devices/virtual/touch/touch_dev",      origin: MEASURED }
      - { path: "/sys/touchpanel",                           origin: MEASURED }
      - { path: "/proc/touchpanel",                          origin: MEASURED }
    verify: { policy: NONE }
    privilege: { read: [NONE], write: [] }
    danger: LOW

  # ─────────────────────────── الذاكرة والتخزين ───────────────────────────
  - key: "zram.{n}.size"
    transport: NODE
    kind: INT
    candidates:
      - { path: "/sys/block/zram0",     origin: MEASURED }
      - { path: "/sys/block/zram*",     origin: MEASURED }
      - { path: "/sys/block/zram",      origin: MEASURED }
      - { path: "/sys/block/",          origin: MEASURED }
    verify: { policy: READ_BACK_EQUAL, tolerance: 0 }
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE] }
    danger: HIGH                          # حجم zram خاطئ = OOM أو swap storm

  - key: "mem_frequency:{device}"
    transport: NODE
    kind: FREQUENCY_RANGE
    candidates:
      - { path: "/sys/class/devfreq", origin: MEASURED }
    verify: { policy: READ_BACK_EQUAL, tolerance: 0 }
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE] }
    danger: MEDIUM

  - key: "ufs_frequency:{device}"
    transport: NODE
    kind: FREQUENCY_RANGE
    candidates:
      - { path: "/sys/class/devfreq", origin: MEASURED }
    verify: { policy: READ_BACK_EQUAL, tolerance: 0 }
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE] }
    danger: MEDIUM

  # ─────────────────────────── الطاقة والحرارة ───────────────────────────
  - key: "battery_health"
    transport: NODE
    kind: STRING
    candidates:
      - { path: "/sys/class/power_supply/battery", origin: MEASURED }
    verify: { policy: NONE }
    privilege: { read: [NONE], write: [] }
    danger: LOW

  - key: "thermal_zone"
    transport: NODE
    kind: STRING
    candidates:
      - { path: "/sys/class/thermal", origin: MEASURED }
    verify: { policy: NONE }
    privilege: { read: [NONE], write: [] }
    danger: LOW

  # ⛔ PROTECTED — تُقرأ ولا تُكتب، بأيّ طبقة
  - key: "thermal_message.cpu_limits"
    transport: NODE
    kind: STRING
    candidates:
      - { path: "/sys/devices/virtual/thermal/thermal_message/cpu_limits", origin: MEASURED }
    verify: { policy: NONE }
    privilege: { read: [NONE, SHIZUKU, MAGISK, KSU, APATCH, DAEMON], write: [] }
    danger: PROTECTED

  - key: "thermal_message.sconfig"
    transport: NODE
    kind: STRING
    candidates:
      - { path: "/sys/devices/virtual/thermal/thermal_message/sconfig", origin: MEASURED }
    verify: { policy: NONE }
    privilege: { read: [NONE, SHIZUKU, MAGISK, KSU, APATCH, DAEMON], write: [] }
    danger: PROTECTED

  # ─────────────────────────── قراءات النظام (PROC) ───────────────────────────
  - key: "system.load"
    transport: PROC
    kind: STRING
    candidates: [ { path: "/proc/loadavg", origin: MEASURED } ]
    verify: { policy: NONE }
    privilege: { read: [NONE], write: [] }
    danger: LOW

  - key: "system.memory_pressure"
    transport: PROC
    kind: STRING
    candidates: [ { path: "/proc/pressure/memory", origin: MEASURED } ]
    verify: { policy: NONE }
    privilege: { read: [NONE], write: [] }
    danger: LOW

  - key: "system.swaps"
    transport: PROC
    kind: STRING
    candidates: [ { path: "/proc/swaps", origin: MEASURED } ]
    verify: { policy: NONE }
    privilege: { read: [NONE], write: [] }
    danger: LOW

  - key: "system.boot_id"
    transport: PROC
    kind: STRING
    candidates: [ { path: "/proc/sys/kernel/random/boot_id", origin: MEASURED } ]
    verify: { policy: NONE }
    privilege: { read: [NONE], write: [] }
    danger: LOW

  - key: "gpu.opp_table.{family}"
    transport: PROC
    kind: STRING
    candidates:
      - { path: "/proc/gpufreq/gpufreq_opp_dump",            origin: MEASURED }
      - { path: "/proc/gpufreqv2/gpu_working_opp_table",     origin: MEASURED }
      - { path: "/proc/gpufreqv2/stack_signed_opp_table",    origin: MEASURED }
    verify: { policy: NONE }
    privilege: { read: [NONE], write: [] }
    danger: LOW

  - key: "cpu.perfserv_freq"
    transport: PROC
    kind: FREQUENCY_RANGE
    candidates: [ { path: "/proc/powerhal_cpu_ctrl/perfserv_freq", origin: MEASURED } ]
    verify: { policy: READ_BACK_EQUAL, tolerance: 0 }
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE] }
    danger: HIGH                          # powerhal: تصادم مع سياسة النظام

  - key: "gpu.busy_percentage"
    transport: NODE
    kind: PERCENT
    candidates:
      - { path: "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage", origin: MEASURED }
      - { path: "/sys/kernel/ged/hal/current_freqency",         origin: MEASURED }
    verify: { policy: NONE }
    privilege: { read: [NONE], write: [] }
    danger: LOW

  # ─────────────────────────── مفاتيح غير عقديّة (البوادئ ١٤ ليست كلها /sys) ───────────────────────────
  - key: "audio_stream:{token}"
    transport: AUDIO
    kind: PERCENT
    candidates: []                        # ليست عقدة: تمرّ بـAudioBackend
    verify: { policy: READ_BACK_TOLERANCE, tolerance: 1 }
    privilege: { read: [NONE], write: [NONE, SHIZUKU] }
    danger: LOW

  - key: "audio_effect:{token}:{param}"
    transport: AUDIO
    kind: STRING
    candidates: []
    verify: { policy: READ_BACK_EQUAL, tolerance: 0 }
    privilege: { read: [NONE], write: [NONE, SHIZUKU] }
    danger: LOW

  - key: "audio_mixer:{deviceId}:{attr}"
    transport: AUDIO
    kind: INT
    candidates: []
    verify: { policy: READ_BACK_EQUAL, tolerance: 0 }
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE] }
    danger: LOW

  - key: "audio_route:{name}"
    transport: AUDIO
    kind: STRING
    candidates: []
    verify: { policy: NONE }
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE] }
    danger: MEDIUM

  - key: "audio_system:{target}"
    transport: AUDIO
    kind: ENUM
    candidates: []
    verify: { policy: READ_BACK_EQUAL, tolerance: 0 }
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE] }
    danger: LOW

  - key: "audio_vendor:{target}"
    transport: AUDIO
    kind: STRING
    candidates: []
    verify: { policy: NONE }
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE] }
    danger: MEDIUM

  - key: "audio_maxfx:{target}"
    transport: AUDIO
    kind: ENUM
    candidates: []
    verify: { policy: READ_BACK_EQUAL, tolerance: 0 }
    privilege: { read: [NONE], write: [DAEMON] }   # عبر maxfx فقط
    danger: MEDIUM

  - key: "spoof_engine:{target}"
    transport: PROP
    kind: BOOL
    candidates: []
    verify: { policy: READ_BACK_EQUAL, tolerance: 0 }
    privilege: { read: [NONE], write: [MAGISK, KSU, APATCH, SU_BARE] }
    danger: HIGH

# ─────────────────────────── مقيس ولا مقبض له بعد ───────────────────────────
unmapped:
  - { path: "/proc/{pid}/stat",                   reason: ROOT_ONLY,  note: "AppMonitor يقرأها في الرفيق" }
```

---

## ٥. عقد المولّد — `tools/gen_control_catalog.py`

| البند | القاعدة |
| --- | --- |
| المدخل | `manager/app/src/main/assets/control_catalog.yaml` + قائمة بوادئ `HardwareControlKey` |
| المخرج | `generated/ControlCatalog.kt` — `internal object` بحقول `const`، **بلا منطق** |
| الرفض | بادئة غير معلنة · مفتاح مكرّر · `PROTECTED` بقائمة كتابة · `candidates` فارغة بلا `transport` غير عقديّ |
| الحتمية | نفس المدخل ⇒ ملفّ بايت-ببايت متطابق (لا طوابع زمن، لا `HashMap` بلا ترتيب) |
| الفشل | `exit 1` + تسمية المدخل وسطره؛ **لا** إخراج ملفّ نصف صالح |
| البوّابة | `python3 tools/gen_control_catalog.py --check` يقارن المولَّد بالملفّ الحاليّ بلا كتابة |

**والحدّ المعلن:** المولّد **لا يُصرّف Kotlin**؛ صحّة المخرَج النحويّة تُقاس بـ`kt_balance` وبالـCI.
ومحليًّا يُعلن «غير مُتحقَّق» إن غاب المُصرّف.

---

## ٦. توصيل المحلّل — إضافيّ لا استبداليّ

```
HardwareCapabilityResolver.readCapability(key)
    ├── الكتالوج (M1)  ⇒ candidates + privilege + danger      ← جديد
    └── المسار القديم  ⇒ يبقى مرجعًا احتياطيًّا                 ← يُحذف في M2 فقط
```

**القاعدة:** لا يُحذف مسار من المحلّل في M1. الاختبار القائم `HardwareCapabilityResolver` يبقى أخضر،
والجديد **يُركَّب فوقه**. وإن اختلف الحكمان على نفس المفتاح ⇒ **الكتالوج يفوز ويُسجَّل الاختلاف**،
والاختلاف نفسه دليل على مسار محفور يجب أن يزول.

---

## ٧. اختبار التطابق

### ٧.١ الموضع والشكل

`manager/app/src/test/java/nd/max/core/hardware/ControlCatalogParityTest.kt`

```kotlin
// كل الدوالّ نقيّة: تأخذ القائمة بمعامل ولا تقرأ ملفًّا.
object CatalogGaps {
    fun missingPrefixes(entries: List<Entry>, prefixes: List<String>): List<String>
    fun keysOutsideControlKey(entries: List<Entry>): List<String>
    fun protectedWithWrite(entries: List<Entry>): List<String>
    fun emptyCandidates(entries: List<Entry>): List<String>
    fun unmeasuredUnmapped(measured: List<String>, entries: List<Entry>, unmapped: List<String>): List<String>
}
```

### ٧.٢ الحالات الموجبة (على البذرة الحقيقية)

| الحالة | المتوقَّع |
| --- | --- |
| بوادئ `HardwareControlKey` الـ١٤ | `missingPrefixes == []` |
| مفاتيح خارج `HardwareControlKey` | `[]` |
| `PROTECTED` بقائمة كتابة | `[]` |
| `candidates` فارغة بلا `transport` غير عقديّ | `[]` |
| المسارات الـ٥٦ | كلها في `candidates` أو `unmapped` |

### ٧.٣ الحالات السلبية الثلاث (إلزامية — أداة لا تُكسر لا تقيس)

| الحقن | المتوقَّع |
| --- | --- |
| حذف مدخل `gpu_frequency:*` من قائمة مُصغَّرة | `missingPrefixes == ["gpu_frequency:"]` — **بالاسم** |
| إضافة مفتاح `"my.custom.knob"` | `keysOutsideControlKey == ["my.custom.knob"]` |
| مدخل `danger: PROTECTED` بقائمة كتابة | `protectedWithWrite == [<key>]` |

---

## ٨. الخطوات — خمسة التزامات صغيرة، كلٌّ ببوّابته

| # | العمل | البوّابة قبل الالتزام |
| --- | --- | --- |
| ١ | `assets/control_catalog.yaml` بالبذرة (§4) — **بلا كود** | `python3 -c "import yaml,sys;…"` غير متاح (لا PyYAML) ⇒ **يُقرأ بمحلّل stdlib** صغير يُضاف للمولّد؛ أو يُكتب JSON ويُقرأ بـ`kotlinx.serialization` |
| ٢ | `tools/gen_control_catalog.py` + `--check` | نفس المدخل ⇒ مخرج متطابق مرّتين (`cmp`) |
| ٣ | كائن مُولَّد + ربطه بـ`HardwareCapabilityResolver` (قراءة فقط) | `kt_balance` · `code_health` · `design_tokens` |
| ٤ | `ControlCatalogParityTest` + الحالات السلبية | الاختبار **يُشغَّل**: ٥ موجب + ٣ سالب. بلا Gradle ⇒ «غير مُتحقَّق» ولا يُدّعى |
| ٥ | تحديث `CURRENT_ARCHITECTURE.md` §5 و`CONTROLKEY-CATALOG-01-design.md` بالأرقام بعد التنفيذ | `code_health --assert` |

**وسؤال تقنيّ يُحسم قبل الخطوة ٢ (لا يُترك للتخمين):** هل يُقرأ YAML في Kotlin بتبعية؟ **لا** —
`PyYAML` غائب في بيئة PRoot، و`snakeyaml` تبعية جديدة. ⇒ **القرار: JSON** (`control_catalog.json`) يُقرأ
بـ`kotlinx.serialization` **الموجود أصلًا** في `libs.versions.toml` (`1.11.0`). والـYAML في هذا الملفّ
**صيغة عرض**؛ المصدر المُلتزم JSON. **وهذا تصحيح مُعلَن لبند في التصميم.**

---

## ٩. الأوامر التي أنتجت كل مسار في البذرة

```sh
cd manager/app/src/main/java/nd/max
grep -rhoE '"/sys/[^"]+"' core/hardware | sort -u          # 41
grep -rhoE '"/proc/[^"]+"' core/hardware | sort -u         # 15
grep -rlE  '"/sys/' core/hardware | wc -l                  # 9
grep -cE 'const val [A-Z0-9_]+' core/hardware/HardwareControlKey.kt   # 14
ls core/hardware/*Backend*.kt                              # 6
```

---

## ١٠. خارج النطاق (لا يُنفَّذ في M1)

1. **لا حذف** لأيّ مسار محفور من `core/hardware` (M2).
2. **لا كتابة** عبر الكتالوج — M1 **قراءة وإعلان** فقط.
3. **لا مسار من مرجع كافتراضي** — المرشّحات من شجرتنا وحدها؛ ومسارات FKM/DevCheck تُضاف بعد قياس جهاز.
4. **لا واجهة جديدة** — `Monitor` هو M5، وM1 يُنتج بيانات لا شاشة.
5. **لا `danger` بلا دليل** — `HIGH` و`PROTECTED` تُبنى على سابقة قائمة (`cpu.perfserv_freq` يتصادم مع powerhal، و`zram.*.size` يخاطر بـOOM).

---

## ١١. المخاطر وقراراتها

| الخطر | القرار |
| --- | --- |
| البذرة تُكرّر معرفة موجودة في `HardwareDeviceOverlay` | الكتالوج **يشتقّ** من البادئات ويُسجّل التجاوزات، ولا يُنشئ نظامًا ثانيًا |
| المولَّد يضخّم `code_health` | في `.gitignore`، ويُولَّد في البناء فقط |
| اختبار التطابق يمرّ دائمًا (دالة لا تُكسر) | **الحالات السلبية الثلاث (§7.3) شرط قبول، لا تحسين** |
| مسارات `DERIVED` تُعامَل مقيسة | الحقل `origin` إلزاميّ، و`DERIVED` يُعرض في `Unknown` حتى يُقاس |
| JSON بديلًا عن YAML يُفقد سهولة التحرير | يُحرَّر YAML ثم يُحوَّل بأداة واحدة (خطوة ٢)، ويُلتزم **المُحوَّل فقط** |
