# فهرس القدرات — مولّد من الكتالوج

> **لا تُعدّل هذا الملفّ يدويًّا.** المصدر `manager/app/src/main/assets/control_catalog.json`،
> والتوليد `python3 tools/gen_capabilities_doc.py --write`، والتحقّق في البوابات
> `python3 tools/gen_capabilities_doc.py --assert`.

## الخلاصة

- إصدار الكتالوج: **1**
- المداخل: **48**
- المرشّحون: **109**
- غير المُخطَّط: **14**

## المداخل

| المفتاح | النوع | النقل | الكتابة | الخطر | المرشّح الأول |
| --- | --- | --- | --- | --- | --- |
| `cpu_limits:{policy}` | FREQUENCY_RANGE | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | MEDIUM | `/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq` |
| `cpu_boost` | BOOL | NODE | MAGISK، KSU، APATCH، SU_BARE | MEDIUM | `/sys/devices/system/cpu/cpufreq/boost` |
| `cpu_core_online:{n}` | BOOL | NODE | — | HIGH | `/sys/devices/system/cpu/cpu$cpu/online` |
| `cpu_topology` | STRING | NODE | — | LOW | `/sys/devices/system/cpu` |
| `gpu_frequency:{device}` | FREQUENCY_RANGE | NODE | MAGISK، KSU، APATCH، SU_BARE | MEDIUM | `/sys/class/kgsl/kgsl-3d0` |
| `gpu_governor_policy:{device}` | ENUM | NODE | — | MEDIUM | `/sys/module/ged/parameters/gpu_dvfs_enable` |
| `gpu.busy_percentage` | PERCENT | NODE | — | LOW | `/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage` |
| `gpu.opp_table.{family}` | STRING | PROC | — | LOW | `/proc/gpufreq/gpufreq_opp_dump` |
| `touch_boost:{path}` | BOOL | NODE | MAGISK، KSU، APATCH، SU_BARE | LOW | `/sys/android_touch/game_switch_enable` |
| `touch_gesture:{path}` | BOOL | NODE | — | LOW | `/sys/android_touch/double_tap_enable` |
| `touch_device_index` | STRING | NODE | — | LOW | `/sys/android_touch` |
| `zram.{n}.size` | INT | NODE | — | HIGH | `/sys/block/zram0` |
| `mem_frequency:{device}` | FREQUENCY_RANGE | NODE | MAGISK، KSU، APATCH، SU_BARE | MEDIUM | `/sys/class/devfreq` |
| `ufs_frequency:{device}` | FREQUENCY_RANGE | NODE | MAGISK، KSU، APATCH، SU_BARE | MEDIUM | `/sys/class/devfreq` |
| `battery_health` | STRING | NODE | — | LOW | `/sys/class/power_supply/battery` |
| `thermal_zone` | STRING | NODE | — | LOW | `/sys/class/thermal` |
| `thermal_message.cpu_limits` | STRING | NODE | — | PROTECTED | `/sys/devices/virtual/thermal/thermal_message/cpu_limits` |
| `thermal_message.sconfig` | STRING | NODE | — | PROTECTED | `/sys/devices/virtual/thermal/thermal_message/sconfig` |
| `system.load` | STRING | PROC | — | LOW | `/proc/loadavg` |
| `system.memory_pressure` | STRING | PROC | — | LOW | `/proc/pressure/memory` |
| `system.swaps` | STRING | PROC | — | LOW | `/proc/swaps` |
| `system.boot_id` | STRING | PROC | — | LOW | `/proc/sys/kernel/random/boot_id` |
| `cpu.perfserv_freq` | FREQUENCY_RANGE | NODE | — | HIGH | `/proc/powerhal_cpu_ctrl/perfserv_freq` |
| `audio_stream:{token}` | PERCENT | AUDIO | NONE، SHIZUKU | LOW | `—` |
| `audio_effect:{token}:{param}` | STRING | AUDIO | NONE، SHIZUKU | LOW | `—` |
| `audio_mixer:{deviceId}:{attr}` | INT | AUDIO | MAGISK، KSU، APATCH، SU_BARE | LOW | `—` |
| `audio_route:{name}` | STRING | AUDIO | MAGISK، KSU، APATCH، SU_BARE | MEDIUM | `—` |
| `audio_system:{target}` | ENUM | AUDIO | MAGISK، KSU، APATCH، SU_BARE | LOW | `—` |
| `audio_vendor:{target}` | STRING | AUDIO | MAGISK، KSU، APATCH، SU_BARE | MEDIUM | `—` |
| `audio_maxfx:{target}` | ENUM | AUDIO | DAEMON | MEDIUM | `—` |
| `spoof_engine:{target}` | BOOL | PROP | MAGISK، KSU، APATCH، SU_BARE | HIGH | `—` |
| `vm.swappiness` | INT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | LOW | `/proc/sys/vm/swappiness` |
| `vm.dirty_ratio` | PERCENT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | MEDIUM | `/proc/sys/vm/dirty_ratio` |
| `vm.dirty_background_ratio` | PERCENT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | MEDIUM | `/proc/sys/vm/dirty_background_ratio` |
| `vm.dirty_writeback_centisecs` | INT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | MEDIUM | `/proc/sys/vm/dirty_writeback_centisecs` |
| `vm.vfs_cache_pressure` | INT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | LOW | `/proc/sys/vm/vfs_cache_pressure` |
| `vm.min_free_kbytes` | INT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/proc/sys/vm/min_free_kbytes` |
| `vm.extra_free_kbytes` | INT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/proc/sys/vm/extra_free_kbytes` |
| `vm.overcommit_ratio` | PERCENT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | MEDIUM | `/proc/sys/vm/overcommit_ratio` |
| `vm.watermark_scale_factor` | INT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | MEDIUM | `/proc/sys/vm/watermark_scale_factor` |
| `vm.oom_kill_allocating_task` | BOOL | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | LOW | `/proc/sys/vm/oom_kill_allocating_task` |
| `gpu.clock.current` | FREQUENCY_RANGE | NODE | — | LOW | `/sys/class/kgsl/kgsl-3d0/gpuclk` |
| `battery.cycle_count` | INT | NODE | — | LOW | `/sys/class/power_supply/battery/cycle_count` |
| `battery.resistance` | INT | NODE | — | LOW | `/sys/class/power_supply/bms/resistance` |
| `battery.voltage.now` | INT | NODE | — | LOW | `/sys/class/power_supply/battery/voltage_now` |
| `battery.voltage.avg` | INT | NODE | — | LOW | `/sys/class/power_supply/battery/voltage_avg` |
| `memory.meminfo` | STRING | PROC | — | LOW | `/proc/meminfo` |
| `zram.comp_algorithm` | ENUM | NODE | — | LOW | `/sys/block/zram0/comp_algorithm` |

## تفصيل المرشّحين

### `cpu_limits:{policy}`

- النوع: `FREQUENCY_RANGE` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **MEDIUM**
- المرشّحون:
  - `/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq` — `MEASURED`
  - `/sys/devices/system/cpu/$cpu/cpufreq` — `MEASURED`
  - `/sys/devices/system/cpu/cpufreq` — `MEASURED`

### `cpu_boost`

- النوع: `BOOL` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **MEDIUM**
- المرشّحون:
  - `/sys/devices/system/cpu/cpufreq/boost` — `MEASURED`

### `cpu_core_online:{n}`

- النوع: `BOOL` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/sys/devices/system/cpu/cpu$cpu/online` — `MEASURED`
  - `/sys/devices/system/cpu/$it/online` — `MEASURED`

### `cpu_topology`

- النوع: `STRING` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/sys/devices/system/cpu` — `MEASURED`
  - `/sys/devices/system/cpu/cpufreq` — `MEASURED`

### `gpu_frequency:{device}`

- النوع: `FREQUENCY_RANGE` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **MEDIUM**
- المرشّحون:
  - `/sys/class/kgsl/kgsl-3d0` — `MEASURED`
  - `/sys/class/kgsl/kgsl-3d0/devfreq` — `MEASURED`
  - `/sys/module/ged/parameters/gpu_cust_boost_freq` — `MEASURED`
  - `/sys/module/ged/parameters/gpu_cust_upbound_freq` — `MEASURED`
  - `/sys/kernel/ged/hal/custom_upbound_gpu_freq` — `MEASURED`
  - `/proc/gpufreq/gpufreq_opp_freq` — `MEASURED`
  - `/proc/gpufreqv2/fix_target_opp_index` — `MEASURED`
  - `/sys/class/devfreq` — `MEASURED`

### `gpu_governor_policy:{device}`

- النوع: `ENUM` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **MEDIUM**
- المرشّحون:
  - `/sys/module/ged/parameters/gpu_dvfs_enable` — `MEASURED`
  - `/sys/module/ged/parameters/gpu_idle` — `MEASURED`

### `gpu.busy_percentage`

- النوع: `PERCENT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage` — `MEASURED`
  - `/sys/kernel/ged/hal/current_freqency` — `MEASURED`
  - `/sys/class/kgsl/kgsl-3d0/gpubusy` — `DERIVED`
  - `/sys/class/misc/mali0/device/utilization` — `DERIVED`
  - `/sys/class/misc/mali0/device/devfreq/gpufreq/gpu_scene_aware/utilisation` — `DERIVED`
  - `/sys/kernel/gpu/gpu_busy` — `DERIVED`

### `gpu.opp_table.{family}`

- النوع: `STRING` · النقل: `PROC`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/proc/gpufreq/gpufreq_opp_dump` — `MEASURED`
  - `/proc/gpufreqv2/gpu_working_opp_table` — `MEASURED`
  - `/proc/gpufreqv2/stack_signed_opp_table` — `MEASURED`

### `touch_boost:{path}`

- النوع: `BOOL` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/sys/android_touch/game_switch_enable` — `MEASURED`
  - `/sys/class/touch/touch_dev/game_switch_enable` — `MEASURED`
  - `/sys/class/touchscreen/game_switch_enable` — `MEASURED`
  - `/sys/devices/virtual/touch/touch_dev/game_switch_enable` — `MEASURED`
  - `/sys/touchpanel/game_switch_enable` — `MEASURED`
  - `/proc/touchpanel/game_switch_enable` — `MEASURED`
  - `/proc/touch_boost/enable` — `MEASURED`

### `touch_gesture:{path}`

- النوع: `BOOL` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/sys/android_touch/double_tap_enable` — `MEASURED`
  - `/sys/android_touch/doubletap2wake` — `MEASURED`
  - `/sys/class/touch/touch_dev/double_tap_enable` — `MEASURED`
  - `/sys/class/touchscreen/double_tap_enable` — `MEASURED`
  - `/sys/touchpanel/double_tap_enable` — `MEASURED`
  - `/proc/touchpanel/double_tap_enable` — `MEASURED`

### `touch_device_index`

- النوع: `STRING` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/sys/android_touch` — `MEASURED`
  - `/sys/class/touch` — `MEASURED`
  - `/sys/class/touch/touch_dev` — `MEASURED`
  - `/sys/class/touchscreen` — `MEASURED`
  - `/sys/devices/virtual/touch/touch_dev` — `MEASURED`
  - `/sys/touchpanel` — `MEASURED`
  - `/proc/touchpanel` — `MEASURED`

### `zram.{n}.size`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/sys/block/zram0` — `MEASURED`
  - `/sys/block/zram*` — `MEASURED`
  - `/sys/block/zram` — `MEASURED`
  - `/sys/block/` — `MEASURED`
  - `/sys/block/zram0/disksize` — `MEASURED`

### `mem_frequency:{device}`

- النوع: `FREQUENCY_RANGE` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **MEDIUM**
- المرشّحون:
  - `/sys/class/devfreq` — `MEASURED`

### `ufs_frequency:{device}`

- النوع: `FREQUENCY_RANGE` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **MEDIUM**
- المرشّحون:
  - `/sys/class/devfreq` — `MEASURED`

### `battery_health`

- النوع: `STRING` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/sys/class/power_supply/battery` — `MEASURED`

### `thermal_zone`

- النوع: `STRING` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/sys/class/thermal` — `MEASURED`

### `thermal_message.cpu_limits`

- النوع: `STRING` · النقل: `NODE`
- القراءة: NONE، SHIZUKU، MAGISK، KSU، APATCH، DAEMON
- الكتابة: —
- التحقّق: `NONE` · تفاوت 0
- الخطر: **PROTECTED**
- المرشّحون:
  - `/sys/devices/virtual/thermal/thermal_message/cpu_limits` — `MEASURED`

### `thermal_message.sconfig`

- النوع: `STRING` · النقل: `NODE`
- القراءة: NONE، SHIZUKU، MAGISK، KSU، APATCH، DAEMON
- الكتابة: —
- التحقّق: `NONE` · تفاوت 0
- الخطر: **PROTECTED**
- المرشّحون:
  - `/sys/devices/virtual/thermal/thermal_message/sconfig` — `MEASURED`

### `system.load`

- النوع: `STRING` · النقل: `PROC`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/proc/loadavg` — `MEASURED`

### `system.memory_pressure`

- النوع: `STRING` · النقل: `PROC`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/proc/pressure/memory` — `MEASURED`

### `system.swaps`

- النوع: `STRING` · النقل: `PROC`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/proc/swaps` — `MEASURED`

### `system.boot_id`

- النوع: `STRING` · النقل: `PROC`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/proc/sys/kernel/random/boot_id` — `MEASURED`

### `cpu.perfserv_freq`

- النوع: `FREQUENCY_RANGE` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/proc/powerhal_cpu_ctrl/perfserv_freq` — `MEASURED`

### `audio_stream:{token}`

- النوع: `PERCENT` · النقل: `AUDIO`
- القراءة: NONE
- الكتابة: NONE، SHIZUKU
- التحقّق: `READ_BACK_TOLERANCE` · تفاوت 1
- الخطر: **LOW**
- المرشّحون: — (مفتاح غير عقديّ بعد)

### `audio_effect:{token}:{param}`

- النوع: `STRING` · النقل: `AUDIO`
- القراءة: NONE
- الكتابة: NONE، SHIZUKU
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **LOW**
- المرشّحون: — (مفتاح غير عقديّ بعد)

### `audio_mixer:{deviceId}:{attr}`

- النوع: `INT` · النقل: `AUDIO`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **LOW**
- المرشّحون: — (مفتاح غير عقديّ بعد)

### `audio_route:{name}`

- النوع: `STRING` · النقل: `AUDIO`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE
- التحقّق: `NONE` · تفاوت 0
- الخطر: **MEDIUM**
- المرشّحون: — (مفتاح غير عقديّ بعد)

### `audio_system:{target}`

- النوع: `ENUM` · النقل: `AUDIO`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **LOW**
- المرشّحون: — (مفتاح غير عقديّ بعد)

### `audio_vendor:{target}`

- النوع: `STRING` · النقل: `AUDIO`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE
- التحقّق: `NONE` · تفاوت 0
- الخطر: **MEDIUM**
- المرشّحون: — (مفتاح غير عقديّ بعد)

### `audio_maxfx:{target}`

- النوع: `ENUM` · النقل: `AUDIO`
- القراءة: NONE
- الكتابة: DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **MEDIUM**
- المرشّحون: — (مفتاح غير عقديّ بعد)

### `spoof_engine:{target}`

- النوع: `BOOL` · النقل: `PROP`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون: — (مفتاح غير عقديّ بعد)

### `vm.swappiness`

- النوع: `INT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/proc/sys/vm/swappiness` — `MEASURED`

### `vm.dirty_ratio`

- النوع: `PERCENT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 1
- الخطر: **MEDIUM**
- المرشّحون:
  - `/proc/sys/vm/dirty_ratio` — `MEASURED`

### `vm.dirty_background_ratio`

- النوع: `PERCENT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 1
- الخطر: **MEDIUM**
- المرشّحون:
  - `/proc/sys/vm/dirty_background_ratio` — `MEASURED`

### `vm.dirty_writeback_centisecs`

- النوع: `INT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 1
- الخطر: **MEDIUM**
- المرشّحون:
  - `/proc/sys/vm/dirty_writeback_centisecs` — `MEASURED`

### `vm.vfs_cache_pressure`

- النوع: `INT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 1
- الخطر: **LOW**
- المرشّحون:
  - `/proc/sys/vm/vfs_cache_pressure` — `MEASURED`

### `vm.min_free_kbytes`

- النوع: `INT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 1024
- الخطر: **HIGH**
- المرشّحون:
  - `/proc/sys/vm/min_free_kbytes` — `MEASURED`

### `vm.extra_free_kbytes`

- النوع: `INT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/proc/sys/vm/extra_free_kbytes` — `DERIVED`

### `vm.overcommit_ratio`

- النوع: `PERCENT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **MEDIUM**
- المرشّحون:
  - `/proc/sys/vm/overcommit_ratio` — `MEASURED`

### `vm.watermark_scale_factor`

- النوع: `INT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 10
- الخطر: **MEDIUM**
- المرشّحون:
  - `/proc/sys/vm/watermark_scale_factor` — `MEASURED`

### `vm.oom_kill_allocating_task`

- النوع: `BOOL` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/proc/sys/vm/oom_kill_allocating_task` — `MEASURED`

### `gpu.clock.current`

- النوع: `FREQUENCY_RANGE` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/class/kgsl/kgsl-3d0/gpuclk` — `DERIVED`
  - `/sys/class/kgsl/kgsl-3d0/clock_mhz` — `DERIVED`
  - `/sys/class/misc/mali0/device/clock` — `DERIVED`
  - `/sys/class/misc/mali0/device/clk_rate` — `DERIVED`
  - `/sys/module/mali/parameters/gpu_freq_cur` — `DERIVED`
  - `/sys/class/devfreq/gpufreq/cur_freq` — `DERIVED`
  - `/sys/class/devfreq/ffa30000.mali/cur_freq` — `DERIVED`
  - `/sys/class/devfreq/57000000.gpu/cur_freq` — `DERIVED`
  - `/sys/class/devfreq/50000000.gpu/cur_freq` — `DERIVED`
  - `/sys/class/devfreq/1c00000.qcom,kgsl-3d0/cur_freq` — `DERIVED`
  - `/sys/class/devfreq/0.qcom,kgsl-3d0/cur_freq` — `DERIVED`
  - `/sys/class/devfreq/17000000.g3d/cur_freq` — `DERIVED`
  - `/sys/class/devfreq/11800000.g3d/cur_freq` — `DERIVED`
  - `/sys/devices/platform/kgsl-3d0.0/kgsl/kgsl-3d0/gpuclk` — `DERIVED`
  - `/sys/devices/platform/kgsl-3d0.0/devfreq/kgsl-3d0/cur_freq` — `DERIVED`
  - `/sys/devices/platform/mali.0/devfreq/mali/cur_freq` — `DERIVED`
  - `/sys/devices/platform/11800000.g3d/devfreq/11800000.g3d/cur_freq` — `DERIVED`
  - `/sys/kernel/tegra_gpu/gpu_rate` — `DERIVED`
  - `/sys/devices/platform/omap/pvrsrvkm.0/sgxfreq/frequency` — `DERIVED`
  - `/proc/mali/cur_freq` — `DERIVED`
  - `/proc/gpufreq/gpufreq_cur` — `DERIVED`

### `battery.cycle_count`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/class/power_supply/battery/cycle_count` — `DERIVED`
  - `/sys/class/power_supply/battery/fg_cycle_count` — `DERIVED`
  - `/sys/class/power_supply/battery/charge_full_cycles` — `DERIVED`
  - `/sys/class/power_supply/bms/cycle_count` — `DERIVED`
  - `/sys/class/power_supply/maxfg/cycle_count` — `DERIVED`
  - `/sys/class/power_supply/pmic/cycle_count` — `DERIVED`

### `battery.resistance`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/class/power_supply/bms/resistance` — `DERIVED`

### `battery.voltage.now`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/class/power_supply/battery/voltage_now` — `DERIVED`
  - `/sys/class/power_supply/bms/voltage_now` — `DERIVED`
  - `/sys/class/power_supply/maxfg/voltage_now` — `DERIVED`
  - `/sys/class/power_supply/pmic/voltage_now` — `DERIVED`

### `battery.voltage.avg`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/class/power_supply/battery/voltage_avg` — `DERIVED`
  - `/sys/class/power_supply/battery/batt_vol` — `DERIVED`

### `memory.meminfo`

- النوع: `STRING` · النقل: `PROC`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/proc/meminfo` — `DERIVED`

### `zram.comp_algorithm`

- النوع: `ENUM` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/block/zram0/comp_algorithm` — `DERIVED`

## غير المُخطَّط

| المسار | السبب | ملاحظة |
| --- | --- | --- |
| `/proc/$pid/stat` | ROOT_ONLY | AppMonitor يقرأها مباشرة في الرفيق، ولا مقبض تحكّم لها |
| `/sys/block/zram0/mem_used` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/class/kgsl/kgsl-3d0/gpu_model` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/class/kgsl/kgsl-3d0/max_clock_mhz` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/class/kgsl/kgsl-3d0/min_clock_mhz` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/class/power_supply/battery/capacity` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/class/power_supply/battery/status` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/class/thermal/thermal_zone0/temp` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/devices/soc0/machine` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/devices/soc0/soc_id` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/devices/system/cpu/cpufreq/policy0/scaling_governor` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/kernel/gpu/gpu_max_clock` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
