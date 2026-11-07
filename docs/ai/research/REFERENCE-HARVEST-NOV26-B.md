# حصاد المراجع — `fde_ai` · `scene_n1`

**التاريخ:** 2026-11-07 · **الشجرتان:** `/tmp/opencode/ref-corpus/fde_ai` (٥٬٠٧٧ ملفًا) و`scene_n1` (٤٬٦٠٠ ملفًا).
**المرجع الملزم:** `docs/ai/FEATURE_MAPPING.md` و`REFERENCE-HARVEST-NOV26.md` — وهذا الملفّ **ملحق** لهما، لا بديل.

## ٠) الحدود والمنهج — تُقرأ قبل أي بند

- **صفر كود · صفر أصل · صفر نصّ · صفر تخطيط.** لا ملفّ ترخيص مقروء في الشجرتين، و`scene_n1` معروفة بترخيص
  يفرض الفتح (copyleft). فالمأخوذ هو **جدول مسارات ومعانٍ** أُعيد بناؤه من الصفر بـKotlin/Compose، ولا صفّ
  اعتماد جديد في `THIRD_PARTY_NOTICES` لأنّ لا كود نُقل (ADR-77).
- **ما قيس يُذكر بـ`path:line`**؛ وما لم يُقَس على جهاز يبقى «غير مُتحقَّق على العتاد».
- **كل مسار جديد دخل الكتالوج `origin: DERIVED`** — أي مشتقّ من جدول مرجعيّ لا مقيس على جهاز المالك،
  ويُجرَّب بالترتيب حتى ينجح أو تنفد المرشّحات.

## ١) لقطة مقيسة

| الشجرة | الملفّات | ما فيها | الحكم |
|---|---:|---|---|
| `fde_ai` | ٥٬٠٧٧ | `src/main/java/con/**` (Java مفكوك)، `src/main/lib/{arm64-v8a,armeabi-v7a}/*.so`، `smali/`، `assets/dexopt` | **جداول مسارات فقط.** لا مكتبة، ولا smali، ولا ضبط تلقائي |
| `scene_n1` | ٤٬٦٠٠ | `assets/{kr-script,toolkit,custom-command,addin}`، `assets/*.json` (`perf_controller` · `categories` · `custom`) | **جداول مقابض فقط.** لا سكربت، ولا `kr-script`، ولا busybox |

## ٢) ما أُخذ فعلًا — خمس دفعات

### ٢.١ كتالوج المسارات — الدفعة ١ (`030968db`)

ثمانية مداخل · ٤٠ مرشّحًا · الكتالوج ٤١ ← ٤٨ مدخلًا و٦٩ ← ١٠٩ مرشّحًا.

| المفتاح | المرشّحات | المصدر المقيس | ملاحظة |
|---|---:|---|---|
| `gpu.clock.current` | ٢١ | `fde_ai/src/main/java/con/SNr2.java:23` | kgsl · mali · devfreq · tegra · omap — بوحدات مختلفة تُقاس ولا تُفترض |
| `gpu.busy` | ٤ | `fde_ai/src/main/java/con/SNr2.java:21` | رُفض `/d/mali/utilization_pp` (debugfs لا مسار `sysfs` معلن) |
| `battery.cycle_count` | ٦ | `fde_ai/src/main/java/con/RV4.java:47` | `battery` · `bms` · `maxfg` · `pmic` · `fg_cycle_count` · `charge_full_cycles` |
| `battery.resistance` | ١ | `fde_ai/src/main/java/con/jlgI.java:38` | `/sys/class/power_supply/bms/resistance` |
| `battery.voltage.now` | ٤ | `fde_ai/src/main/java/con/RV4.java:45` | أربع عائلات `power_supply` |
| `battery.voltage.avg` | ٢ | `fde_ai/src/main/java/con/RV4.java:45` | `voltage_avg` و`batt_vol` |
| `memory.meminfo` | ١ | `fde_ai/src/main/java/con/erM0a.java` | `/proc/meminfo` — قراءة فقط |
| `zram.comp_algorithm` | ١ | `scene_n1/src/main/java/a/nf.java:123` · `a/v20.java:110` | `/sys/block/zram0/comp_algorithm` — قراءة معلنة |

### ٢.٢ نافذة ثبات الإطارات — الدفعة ٢ (`fa176e15`)

`core/platform/FrameWindow.kt`: نافذة متدحرجة (١٢٠ عيّنة) تحسب المتوسط والمئين و**حصة العيّنات القريبة من
هدف الشاشة** (`stableShare`, أرضية ٠٫٩). و`HudField.Stability` بجانب `Frames` لأنّه يجيب السؤال نفسه
(«هل هي سلسة؟») بمدى لا بلحظة. **لا مصدر قياس جديد:** يُغذّى من معدّل الإطارات المُعايَن أصلًا.

### ٢.٣ منحنى الشحن — الدفعة ٣ (`fc2b2ea8`)

`core/battery/ChargeCurve.kt`: القدرة = الجهد × التيار (بالقيمة المطلقة — إشارة `CURRENT_NOW` تختلف بين
النوى)، ونافذة الشحن (أعلى/متوسط/مدى/زمن متبقٍّ) تُحسب **من العيّنات المسجّلة سلفًا** في
`BatteryHistoryStore`. لا خدمة خلفية ولا صلاحية جديدة؛ ونافذة أقصر من دقيقة تُرفض، والممتلئ لا يُعطي
«وقتًا متبقّيًا». يُعرض قسمًا داخل شاشة تاريخ البطارية (لا وجهة جديدة).

### ٢.٤ عائلة المجدول — الدفعة ٤ (`c11d2c75`)

١٢ مدخلًا حرفيًّا تحت بادئة معلنة واحدة `HardwareControlKey.SCHED_PREFIX = "sched."` — الكتالوج
٤٨ ← ٦٠ مدخلًا و١٠٩ ← ١٢١ مرشّحًا، والبوادئ ١٩ ← ٢٠:

| المجموعة | المسارات | النوع |
|---|---|---|
| `uclamp` | `/dev/cpuctl/{top-app,foreground,background}/cpu.uclamp.{min,max}` | `INT` |
| `uclamp` | `/dev/cpuctl/top-app/cpu.uclamp.latency_sensitive` | `BOOL` |
| `shares` | `/dev/cpuctl/top-app/cpu.shares` | `INT` |
| `schedtune` | `/dev/stune/top-app/cpu.schedtune.{boost,prefer_idle}` | `INT` · `BOOL` |
| `migrate` | `/proc/sys/kernel/sched_{up,down}migrate` | `INT` |

كلّها `danger: HIGH` · `verify: READ_BACK_EQUAL` · كتابة عبر الـarbiter وحدها وتحتاج إقرارًا (ADR-90).

### ٢.٥ إتمام العائلة — الدفعة ٥ (`ff71a55d`)

أربعة مداخل من `scene_n1/src/main/res/raw/perf_controller:21-54` — الكتالوج ٦٠ ← ٦٤ مدخلًا
و١٢١ ← ١٢٥ مرشّحًا:

| المفتاح | المسار | النوع |
|---|---|---|
| `sched.group.upmigrate` | `/proc/sys/kernel/sched_group_upmigrate` | `INT` |
| `sched.group.downmigrate` | `/proc/sys/kernel/sched_group_downmigrate` | `INT` |
| `sched.force_lb_enable` | `/proc/sys/kernel/sched_force_lb_enable` | `BOOL` |
| `sched.lib.mask_force` | `/proc/sys/kernel/sched_lib_mask_force` | `STRING` |

**وحدّ `sched.lib.mask_force` معلن:** دلالته تخصّ نواة البائع ولم تُقَس؛ الكتالوج يعلن المسار لا الصيغة،
والقيمة الخاطئة تُقرأ وتُرجَع (`READ_BACK_EQUAL`) لكنّها قد تُغيّر سلوك المجدول — لذلك `HIGH`.

## ٣) ما رُفض صراحةً — ولن يُنفَّذ

| البند | من أين | لماذا |
|---|---|---|
| أيّ `.so` أو daemon أو `.o` BPF | `fde_ai/src/main/lib/*/libfcore.so` | كود مُصرَّف بلا مصدر ولا ترخيص ⇒ صفر نقل |
| ضبط النواة التلقائي | `fde_ai` (خدمة الضبط) | لا كتابة بلا فعل مستخدم — ADR-11 |
| كتم خفض التردد (throttle suppression) | `fde_ai` | يُخفي حماية العتاد بدل قياسها — ADR-07/18 |
| `kr-script` كمنفّذ أوامر | `scene_n1/assets/kr-script` + `toolkit/run` | منفّذ أوامر عامّ = سطح كتابة خارج الـarbiter |
| سكربتات `assets/addin/*.sh` | `scene_n1` | `force_compact` · `zram_control` · `swap_control` — تُنفَّذ بـshell مباشرة |
| `custom-command/*.sh` | `scene_n1` | Perfetto/Simpleperf — أدوات خارجية لا تُشحن |
| `dexopt` · `ksu.zip` · `busybox` | الشجرتان | ثنائيات/حزم جاهزة لا تُنسخ |
| التخطيط والأيقونات واللوحة والحركة | الشجرتان | لا تقليد شكل — التصميم من `ui/design/` (ADR-77) |
| `categories.json` · `custom.json` · `outside.json` | `scene_n1` | تصنيفات واجهة؛ أُخذت **الفكرة** لا الملفّ |

## ٤) ما بقي على الجهاز — لا يُدّعى هنا

- هل تقبل النواة الكتابة إلى `cpu.uclamp.*` / `cpu.schedtune.*` / `sched_*migrate` / `sched_group_*migrate`
  أم تُرجع `EACCES` رغم الجذر — **يحتاج جهازًا**.
- أيّ مسار GPU يظهر فعلًا من الـ٢١ مرشّحًا، وبأيّ وحدة (Hz/MHz) — **يحتاج جهازًا**.
- إشارة `currentUa` على نواة المالك، وشكل منحنى الشحن الحقيقي — **يحتاج جهازًا**.
- `sched.lib.mask_force`: صيغة القيمة المقبولة عند البائع — **يحتاج جهازًا**.

## ٥) حالة التحقق

- **البوابات المحلية:** التسع `--assert` + `--prune all` + `rtl_guard --assert` + `git diff --check` خضراء
  في كل دفعة (`y3-*` · `z4-*` · `t5-*` · `u5-*`).
- **الاختبارات:** `FrameWindowTest` (٦) و`ChargeCurveTest` (٧) مكتوبتان ضمن `6e040134`.
- **CI:** `MaxManager` و`MaxManager docs` تلقائيان على كل دفع؛ حكم كل تشغيل يُقرأ من `gh run view`
  ولا يُدّعى قبل قراءته.
