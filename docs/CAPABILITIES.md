# فهرس القدرات — مولّد من الكتالوج

> **لا تُعدّل هذا الملفّ يدويًّا.** المصدر `manager/app/src/main/assets/control_catalog.json`،
> والتوليد `python3 tools/gen_capabilities_doc.py --write`، والتحقّق في البوابات
> `python3 tools/gen_capabilities_doc.py --assert`.

## الخلاصة

- إصدار الكتالوج: **1**
- المداخل: **88**
- المرشّحون: **162**
- غير المُخطَّط: **19**

## المداخل

| المفتاح | النوع | النقل | الكتابة | الخطر | المرشّح الأول |
| --- | --- | --- | --- | --- | --- |
| `audio_effect:{token}:{param}` | STRING | AUDIO | NONE، SHIZUKU | LOW | `—` |
| `audio_maxfx:{target}` | ENUM | AUDIO | DAEMON | MEDIUM | `—` |
| `audio_mixer:{deviceId}:{attr}` | INT | AUDIO | MAGISK، KSU، APATCH، SU_BARE | LOW | `—` |
| `audio_route:{name}` | STRING | AUDIO | MAGISK، KSU، APATCH، SU_BARE | MEDIUM | `—` |
| `audio_stream:{token}` | PERCENT | AUDIO | NONE، SHIZUKU | LOW | `—` |
| `audio_system:{target}` | ENUM | AUDIO | MAGISK، KSU، APATCH، SU_BARE | LOW | `—` |
| `audio_vendor:{target}` | STRING | AUDIO | MAGISK، KSU، APATCH، SU_BARE | MEDIUM | `—` |
| `battery.charging_enabled` | BOOL | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/sys/class/power_supply/battery/battery_charging_enabled` |
| `battery.cycle_count` | INT | NODE | — | LOW | `/sys/class/power_supply/battery/cycle_count` |
| `battery.input_suspend` | BOOL | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/sys/class/power_supply/battery/input_suspend` |
| `battery.pd_active` | BOOL | NODE | — | LOW | `/sys/class/power_supply/usb/pd_active` |
| `battery.pd_allowed` | BOOL | NODE | — | LOW | `/sys/class/power_supply/usb/pd_allowed` |
| `battery.resistance` | INT | NODE | — | LOW | `/sys/class/power_supply/bms/resistance` |
| `battery.step_charging` | BOOL | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/sys/class/power_supply/battery/step_charging_enabled` |
| `battery.voltage.avg` | INT | NODE | — | LOW | `/sys/class/power_supply/battery/voltage_avg` |
| `battery.voltage.now` | INT | NODE | — | LOW | `/sys/class/power_supply/battery/voltage_now` |
| `battery_health` | STRING | NODE | — | LOW | `/sys/class/power_supply/battery` |
| `cpu.perfserv_freq` | FREQUENCY_RANGE | NODE | — | HIGH | `/proc/powerhal_cpu_ctrl/perfserv_freq` |
| `cpu.qcom.max` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/sys/module/msm_performance/parameters/cpu_max_freq` |
| `cpu.qcom.min` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/sys/module/msm_performance/parameters/cpu_min_freq` |
| `cpu_boost` | BOOL | NODE | MAGISK، KSU، APATCH، SU_BARE | MEDIUM | `/sys/devices/system/cpu/cpufreq/boost` |
| `cpu_core_online:{n}` | BOOL | NODE | — | HIGH | `/sys/devices/system/cpu/cpu$cpu/online` |
| `cpu_limits:{policy}` | FREQUENCY_RANGE | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | MEDIUM | `/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq` |
| `cpu_topology` | STRING | NODE | — | LOW | `/sys/devices/system/cpu` |
| `governor_tunable:{policy}:{governor}:{field}` | INT | NODE | — | HIGH | `/sys/devices/system/cpu/cpufreq/{policy}/{governor}/{field}` |
| `gpu.busy_percentage` | PERCENT | NODE | — | LOW | `/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage` |
| `gpu.clock.current` | FREQUENCY_RANGE | NODE | — | LOW | `/sys/class/kgsl/kgsl-3d0/gpuclk` |
| `gpu.opp_table.{family}` | STRING | PROC | — | LOW | `/proc/gpufreq/gpufreq_opp_dump` |
| `gpu_frequency:{device}` | FREQUENCY_RANGE | NODE | MAGISK، KSU، APATCH، SU_BARE | MEDIUM | `/sys/class/kgsl/kgsl-3d0` |
| `gpu_governor_policy:{device}` | ENUM | NODE | — | MEDIUM | `/sys/module/ged/parameters/gpu_dvfs_enable` |
| `haptic_tuning:{path}` | INT | NODE | — | HIGH | `/sys/class/timed_output/vibrator/{field}` |
| `mem_frequency:{device}` | FREQUENCY_RANGE | NODE | MAGISK، KSU، APATCH، SU_BARE | MEDIUM | `/sys/class/devfreq` |
| `memory.compression` | STRING | NODE | — | LOW | `/sys/kernel/mm/memcompress/stat` |
| `memory.force_reclaim_zram` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/dev/memcg/memory.force_reclaim_zram` |
| `memory.force_swapout` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/dev/memcg/memory.force_swapout` |
| `memory.lmk.adaptive` | BOOL | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/sys/module/lowmemorykiller/parameters/enable_adaptive_lmk` |
| `memory.lmk.minfree` | STRING | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/sys/module/lowmemorykiller/parameters/minfree` |
| `memory.meminfo` | STRING | PROC | — | LOW | `/proc/meminfo` |
| `panel_color:{path}` | STRING | NODE | — | HIGH | `/sys/devices/platform/kcal_ctrl.0/{field}` |
| `panel_feature:{path}` | BOOL | NODE | — | HIGH | `/sys/class/leds/lcd-backlight/hbm` |
| `sched.cpu_shares.top` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/dev/cpuctl/top-app/cpu.shares` |
| `sched.downmigrate` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/proc/sys/kernel/sched_downmigrate` |
| `sched.force_lb_enable` | BOOL | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/proc/sys/kernel/sched_force_lb_enable` |
| `sched.group.downmigrate` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/proc/sys/kernel/sched_group_downmigrate` |
| `sched.group.upmigrate` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/proc/sys/kernel/sched_group_upmigrate` |
| `sched.lib.mask_force` | STRING | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/proc/sys/kernel/sched_lib_mask_force` |
| `sched.stune.boost.top` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/dev/stune/top-app/cpu.schedtune.boost` |
| `sched.stune.prefer_idle.top` | BOOL | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/dev/stune/top-app/cpu.schedtune.prefer_idle` |
| `sched.uclamp.background.max` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/dev/cpuctl/background/cpu.uclamp.max` |
| `sched.uclamp.background.min` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/dev/cpuctl/background/cpu.uclamp.min` |
| `sched.uclamp.foreground.max` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/dev/cpuctl/foreground/cpu.uclamp.max` |
| `sched.uclamp.foreground.min` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/dev/cpuctl/foreground/cpu.uclamp.min` |
| `sched.uclamp.latency_sensitive` | BOOL | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/dev/cpuctl/top-app/cpu.uclamp.latency_sensitive` |
| `sched.uclamp.top.max` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/dev/cpuctl/top-app/cpu.uclamp.max` |
| `sched.uclamp.top.min` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/dev/cpuctl/top-app/cpu.uclamp.min` |
| `sched.upmigrate` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/proc/sys/kernel/sched_upmigrate` |
| `spoof_engine:{target}` | BOOL | PROP | MAGISK، KSU، APATCH، SU_BARE | HIGH | `—` |
| `system.boot_id` | STRING | PROC | — | LOW | `/proc/sys/kernel/random/boot_id` |
| `system.load` | STRING | PROC | — | LOW | `/proc/loadavg` |
| `system.memory_pressure` | STRING | PROC | — | LOW | `/proc/pressure/memory` |
| `system.swaps` | STRING | PROC | — | LOW | `/proc/swaps` |
| `thermal.board_temp` | INT | NODE | — | LOW | `/sys/class/thermal/thermal_message/board_sensor_temp` |
| `thermal.dynamic_tj` | INT | NODE | — | LOW | `/sys/class/thermal/thermal_message/dynamic_tj` |
| `thermal.ttj` | INT | NODE | — | LOW | `/sys/kernel/thermal/ttj` |
| `thermal_message.cpu_limits` | STRING | NODE | — | PROTECTED | `/sys/devices/virtual/thermal/thermal_message/cpu_limits` |
| `thermal_message.sconfig` | STRING | NODE | — | PROTECTED | `/sys/devices/virtual/thermal/thermal_message/sconfig` |
| `storage_queue:{device}:{field}` | INT | NODE | — | HIGH | `/sys/block/{device}/queue/{field}` |
| `thermal_zone` | STRING | NODE | — | LOW | `/sys/class/thermal` |
| `touch_boost:{path}` | BOOL | NODE | MAGISK، KSU، APATCH، SU_BARE | LOW | `/sys/android_touch/game_switch_enable` |
| `touch_device_index` | STRING | NODE | — | LOW | `/sys/android_touch` |
| `touch_gesture:{path}` | BOOL | NODE | — | LOW | `/sys/android_touch/double_tap_enable` |
| `ufs_frequency:{device}` | FREQUENCY_RANGE | NODE | MAGISK، KSU، APATCH، SU_BARE | MEDIUM | `/sys/class/devfreq` |
| `vm.dirty_background_ratio` | PERCENT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | MEDIUM | `/proc/sys/vm/dirty_background_ratio` |
| `vm.dirty_ratio` | PERCENT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | MEDIUM | `/proc/sys/vm/dirty_ratio` |
| `vm.dirty_writeback_centisecs` | INT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | MEDIUM | `/proc/sys/vm/dirty_writeback_centisecs` |
| `vm.extra_free_kbytes` | INT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/proc/sys/vm/extra_free_kbytes` |
| `vm.min_free_kbytes` | INT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/proc/sys/vm/min_free_kbytes` |
| `vm.oom_kill_allocating_task` | BOOL | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | LOW | `/proc/sys/vm/oom_kill_allocating_task` |
| `vm.overcommit_ratio` | PERCENT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | MEDIUM | `/proc/sys/vm/overcommit_ratio` |
| `vm.swappiness` | INT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | LOW | `/proc/sys/vm/swappiness` |
| `vm.vfs_cache_pressure` | INT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | LOW | `/proc/sys/vm/vfs_cache_pressure` |
| `vm.watermark_scale_factor` | INT | PROC | MAGISK، KSU، APATCH، SU_BARE، DAEMON | MEDIUM | `/proc/sys/vm/watermark_scale_factor` |
| `zram.backing_dev` | STRING | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/sys/block/zram0/backing_dev` |
| `zram.comp_algorithm` | ENUM | NODE | — | LOW | `/sys/block/zram0/comp_algorithm` |
| `zram.hybridswap_meminfo` | STRING | NODE | — | LOW | `/sys/block/zram0/hybridswap_meminfo` |
| `zram.smartswap_meminfo` | STRING | NODE | — | LOW | `/sys/block/zram0/smartswap_meminfo` |
| `zram.writeback` | INT | NODE | MAGISK، KSU، APATCH، SU_BARE، DAEMON | HIGH | `/sys/block/zram0/writeback` |
| `zram.{n}.size` | INT | NODE | — | HIGH | `/sys/block/zram0` |

## تفصيل المرشّحين

### `audio_effect:{token}:{param}`

- النوع: `STRING` · النقل: `AUDIO`
- القراءة: NONE
- الكتابة: NONE، SHIZUKU
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **LOW**
- المرشّحون: — (مفتاح غير عقديّ بعد)

### `audio_maxfx:{target}`

- النوع: `ENUM` · النقل: `AUDIO`
- القراءة: NONE
- الكتابة: DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **MEDIUM**
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

### `audio_stream:{token}`

- النوع: `PERCENT` · النقل: `AUDIO`
- القراءة: NONE
- الكتابة: NONE، SHIZUKU
- التحقّق: `READ_BACK_TOLERANCE` · تفاوت 1
- الخطر: **LOW**
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

### `battery.charging_enabled`

- النوع: `BOOL` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/sys/class/power_supply/battery/battery_charging_enabled` — `DERIVED`
  - `/sys/class/power_supply/battery/charging_enabled` — `DERIVED`

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

### `battery.input_suspend`

- النوع: `BOOL` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/sys/class/power_supply/battery/input_suspend` — `DERIVED`
  - `/sys/class/power_supply/bms/input_suspend` — `DERIVED`

### `battery.pd_active`

- النوع: `BOOL` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/class/power_supply/usb/pd_active` — `DERIVED`

### `battery.pd_allowed`

- النوع: `BOOL` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/class/power_supply/usb/pd_allowed` — `DERIVED`

### `battery.resistance`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/class/power_supply/bms/resistance` — `DERIVED`

### `battery.step_charging`

- النوع: `BOOL` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/sys/class/power_supply/battery/step_charging_enabled` — `DERIVED`
  - `/sys/class/power_supply/bms/step_charging_enabled` — `DERIVED`

### `battery.voltage.avg`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/class/power_supply/battery/voltage_avg` — `DERIVED`
  - `/sys/class/power_supply/battery/batt_vol` — `DERIVED`

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

### `battery_health`

- النوع: `STRING` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/sys/class/power_supply/battery` — `MEASURED`

### `cpu.perfserv_freq`

- النوع: `FREQUENCY_RANGE` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/proc/powerhal_cpu_ctrl/perfserv_freq` — `MEASURED`

### `cpu.qcom.max`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/sys/module/msm_performance/parameters/cpu_max_freq` — `DERIVED`
  - `/sys/kernel/msm_performance/parameters/cpu_max_freq` — `DERIVED`

### `cpu.qcom.min`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/sys/module/msm_performance/parameters/cpu_min_freq` — `DERIVED`
  - `/sys/kernel/msm_performance/parameters/cpu_min_freq` — `DERIVED`

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

### `cpu_topology`

- النوع: `STRING` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/sys/devices/system/cpu` — `MEASURED`
  - `/sys/devices/system/cpu/cpufreq` — `MEASURED`

### `governor_tunable:{policy}:{governor}:{field}`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/sys/devices/system/cpu/cpufreq/{policy}/{governor}/{field}` — `DERIVED`
  - `/sys/devices/system/cpu/cpufreq/{governor}/{field}` — `DERIVED`

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

### `haptic_tuning:{path}`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/sys/class/timed_output/vibrator/{field}` — `DERIVED`
  - `/sys/class/leds/vibrator/{field}` — `DERIVED`
  - `/sys/class/leds/vibrator/device/{field}` — `DERIVED`

### `mem_frequency:{device}`

- النوع: `FREQUENCY_RANGE` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **MEDIUM**
- المرشّحون:
  - `/sys/class/devfreq` — `MEASURED`

### `memory.compression`

- النوع: `STRING` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/kernel/mm/memcompress/stat` — `DERIVED`

### `memory.force_reclaim_zram`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/dev/memcg/memory.force_reclaim_zram` — `DERIVED`

### `memory.force_swapout`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/dev/memcg/memory.force_swapout` — `DERIVED`

### `memory.lmk.adaptive`

- النوع: `BOOL` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/sys/module/lowmemorykiller/parameters/enable_adaptive_lmk` — `DERIVED`

### `memory.lmk.minfree`

- النوع: `STRING` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/sys/module/lowmemorykiller/parameters/minfree` — `DERIVED`

### `memory.meminfo`

- النوع: `STRING` · النقل: `PROC`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/proc/meminfo` — `DERIVED`

### `panel_color:{path}`

- النوع: `STRING` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/sys/devices/platform/kcal_ctrl.0/{field}` — `DERIVED`
  - `/sys/devices/platform/kcal_ctrl/{field}` — `DERIVED`

### `panel_feature:{path}`

- النوع: `BOOL` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/sys/class/leds/lcd-backlight/hbm` — `DERIVED`
  - `/sys/devices/platform/mtk-hbm/hbm_enable` — `DERIVED`
  - `/sys/class/backlight/panel0-backlight/hbm_mode` — `DERIVED`
  - `/sys/class/drm/card0-DSI-1/hbm` — `DERIVED`

### `sched.cpu_shares.top`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/dev/cpuctl/top-app/cpu.shares` — `DERIVED`

### `sched.downmigrate`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/proc/sys/kernel/sched_downmigrate` — `DERIVED`

### `sched.force_lb_enable`

- النوع: `BOOL` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/proc/sys/kernel/sched_force_lb_enable` — `DERIVED`

### `sched.group.downmigrate`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/proc/sys/kernel/sched_group_downmigrate` — `DERIVED`

### `sched.group.upmigrate`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/proc/sys/kernel/sched_group_upmigrate` — `DERIVED`

### `sched.lib.mask_force`

- النوع: `STRING` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/proc/sys/kernel/sched_lib_mask_force` — `DERIVED`

### `sched.stune.boost.top`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/dev/stune/top-app/cpu.schedtune.boost` — `DERIVED`

### `sched.stune.prefer_idle.top`

- النوع: `BOOL` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/dev/stune/top-app/cpu.schedtune.prefer_idle` — `DERIVED`

### `sched.uclamp.background.max`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/dev/cpuctl/background/cpu.uclamp.max` — `DERIVED`

### `sched.uclamp.background.min`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/dev/cpuctl/background/cpu.uclamp.min` — `DERIVED`

### `sched.uclamp.foreground.max`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/dev/cpuctl/foreground/cpu.uclamp.max` — `DERIVED`

### `sched.uclamp.foreground.min`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/dev/cpuctl/foreground/cpu.uclamp.min` — `DERIVED`

### `sched.uclamp.latency_sensitive`

- النوع: `BOOL` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/dev/cpuctl/top-app/cpu.uclamp.latency_sensitive` — `DERIVED`

### `sched.uclamp.top.max`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/dev/cpuctl/top-app/cpu.uclamp.max` — `DERIVED`

### `sched.uclamp.top.min`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/dev/cpuctl/top-app/cpu.uclamp.min` — `DERIVED`

### `sched.upmigrate`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/proc/sys/kernel/sched_upmigrate` — `DERIVED`

### `spoof_engine:{target}`

- النوع: `BOOL` · النقل: `PROP`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون: — (مفتاح غير عقديّ بعد)

### `system.boot_id`

- النوع: `STRING` · النقل: `PROC`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/proc/sys/kernel/random/boot_id` — `MEASURED`

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

### `thermal.board_temp`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/class/thermal/thermal_message/board_sensor_temp` — `DERIVED`

### `thermal.dynamic_tj`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/class/thermal/thermal_message/dynamic_tj` — `DERIVED`

### `thermal.ttj`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/kernel/thermal/ttj` — `DERIVED`

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

### `storage_queue:{device}:{field}`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/sys/block/{device}/queue/{field}` — `DERIVED`
  - `/sys/block` — `DERIVED`

### `thermal_zone`

- النوع: `STRING` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/sys/class/thermal` — `MEASURED`

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

### `ufs_frequency:{device}`

- النوع: `FREQUENCY_RANGE` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **MEDIUM**
- المرشّحون:
  - `/sys/class/devfreq` — `MEASURED`

### `vm.dirty_background_ratio`

- النوع: `PERCENT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 1
- الخطر: **MEDIUM**
- المرشّحون:
  - `/proc/sys/vm/dirty_background_ratio` — `MEASURED`

### `vm.dirty_ratio`

- النوع: `PERCENT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 1
- الخطر: **MEDIUM**
- المرشّحون:
  - `/proc/sys/vm/dirty_ratio` — `MEASURED`

### `vm.dirty_writeback_centisecs`

- النوع: `INT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 1
- الخطر: **MEDIUM**
- المرشّحون:
  - `/proc/sys/vm/dirty_writeback_centisecs` — `MEASURED`

### `vm.extra_free_kbytes`

- النوع: `INT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/proc/sys/vm/extra_free_kbytes` — `DERIVED`

### `vm.min_free_kbytes`

- النوع: `INT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 1024
- الخطر: **HIGH**
- المرشّحون:
  - `/proc/sys/vm/min_free_kbytes` — `MEASURED`

### `vm.oom_kill_allocating_task`

- النوع: `BOOL` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/proc/sys/vm/oom_kill_allocating_task` — `MEASURED`

### `vm.overcommit_ratio`

- النوع: `PERCENT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **MEDIUM**
- المرشّحون:
  - `/proc/sys/vm/overcommit_ratio` — `MEASURED`

### `vm.swappiness`

- النوع: `INT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **LOW**
- المرشّحون:
  - `/proc/sys/vm/swappiness` — `MEASURED`

### `vm.vfs_cache_pressure`

- النوع: `INT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 1
- الخطر: **LOW**
- المرشّحون:
  - `/proc/sys/vm/vfs_cache_pressure` — `MEASURED`

### `vm.watermark_scale_factor`

- النوع: `INT` · النقل: `PROC`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 10
- الخطر: **MEDIUM**
- المرشّحون:
  - `/proc/sys/vm/watermark_scale_factor` — `MEASURED`

### `zram.backing_dev`

- النوع: `STRING` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/sys/block/zram0/backing_dev` — `DERIVED`

### `zram.comp_algorithm`

- النوع: `ENUM` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/block/zram0/comp_algorithm` — `DERIVED`

### `zram.hybridswap_meminfo`

- النوع: `STRING` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/block/zram0/hybridswap_meminfo` — `DERIVED`

### `zram.smartswap_meminfo`

- النوع: `STRING` · النقل: `NODE`
- القراءة: NONE
- الكتابة: —
- التحقّق: `NONE`
- الخطر: **LOW**
- المرشّحون:
  - `/sys/block/zram0/smartswap_meminfo` — `DERIVED`

### `zram.writeback`

- النوع: `INT` · النقل: `NODE`
- القراءة: NONE
- الكتابة: MAGISK، KSU، APATCH، SU_BARE، DAEMON
- التحقّق: `READ_BACK_EQUAL` · تفاوت 0
- الخطر: **HIGH**
- المرشّحون:
  - `/sys/block/zram0/writeback` — `DERIVED`

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

## غير المُخطَّط

| المسار | السبب | ملاحظة |
| --- | --- | --- |
| `/proc/$pid/stat` | ROOT_ONLY | AppMonitor يقرأها مباشرة في الرفيق، ولا مقبض تحكّم لها |
| `/sys/block` | READ_ONLY_PROBE | — |
| `/sys/block/zram0/mem_used` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/class/kgsl/kgsl-3d0/gpu_model` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/class/kgsl/kgsl-3d0/max_clock_mhz` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/class/kgsl/kgsl-3d0/min_clock_mhz` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/class/leds/vibrator` | READ_ONLY_PROBE | — |
| `/sys/class/leds/vibrator/device` | READ_ONLY_PROBE | — |
| `/sys/class/power_supply/battery/capacity` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/class/power_supply/battery/status` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/class/thermal/thermal_zone0/temp` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/class/timed_output/vibrator` | READ_ONLY_PROBE | — |
| `/sys/devices/soc0/machine` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/devices/soc0/soc_id` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/devices/system/cpu/cpufreq/$governor` | READ_ONLY_PROBE | — |
| `/sys/devices/system/cpu/cpufreq/policy0/scaling_governor` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
| `/sys/kernel/gpu/gpu_max_clock` | READ_ONLY_PROBE | يُقرأ لكتالوج عُقد التشخيص (قراءة فقط)؛ لا مقبض تحكّم ولا كتابة مقصودة |
