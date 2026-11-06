/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/**
 * كتالوج العُقد التي يعرفها التطبيق — **قراءةً فقط**، تعريفًا لا كتابةً.
 *
 * **ولماذا وُجد:** محرّك الوصفات لا يجوز أن يكتب إلى مسار لم يُقرأ ويتأكّد وجوده أولًا؛ فكان لا بدّ
 * من قائمة واحدة تقول: هذه هي العُقد التي نعرفها، وهذا نطاقها، وهذه حالتها على هذا الجهاز. وليست
 * هذه القائمة «العُقد الممكنة» في العالم — هي **ما في هذا المستودع فعلًا**: كل مسار هنا مأخوذ من
 * كود قائم (`PersistRestorer` وواجهات العتاد)، لا من دليل جهاز نظري.
 *
 * **وما لا يفعله:** لا يكتب ولا يفتح ملفًا للكتابة، ولا يمنح ملكية، ولا يتجاوز `HardwareControlArbiter`
 * (ADR-11). ولا يحوّل غياب عقدة إلى صفر: الغياب والجهل حالتان مختلفتان عن القيمة (ADR-07).
 */
object NodeCatalog {

    enum class Scope { CPU, GPU, THERMAL, MEMORY, POWER, PLATFORM }

    /**
     * عقدة واحدة بمعرّف ثابت.
     *
     * المعرّف **مستقرّ** لأنه يُخزَّن في السجلات ويُقارن بين جولتين؛ أمّا [path] فقابل للتغيّر بين
     * الأجهزة، ولهذا لا يُستعمل مفتاحًا.
     */
    data class Node(val id: String, val path: String, val scope: Scope)

    val nodes: List<Node> = listOf(
        Node("cpu_scaling_min", "/sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq", Scope.CPU),
        Node("cpu_scaling_max", "/sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq", Scope.CPU),
        Node("cpu_scaling_governor", "/sys/devices/system/cpu/cpufreq/policy0/scaling_governor", Scope.CPU),
        Node("cpu_boost", "/sys/devices/system/cpu/cpufreq/boost", Scope.CPU),
        Node("gpu_min_clock", "/sys/class/kgsl/kgsl-3d0/min_clock_mhz", Scope.GPU),
        Node("gpu_max_clock", "/sys/class/kgsl/kgsl-3d0/max_clock_mhz", Scope.GPU),
        Node("gpu_busy", "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage", Scope.GPU),
        Node("gpu_model", "/sys/class/kgsl/kgsl-3d0/gpu_model", Scope.GPU),
        Node("kernel_gpu_max", "/sys/kernel/gpu/gpu_max_clock", Scope.GPU),
        Node("thermal_zone0", "/sys/class/thermal/thermal_zone0/temp", Scope.THERMAL),
        Node("zram_disksize", "/sys/block/zram0/disksize", Scope.MEMORY),
        Node("zram_mem_used", "/sys/block/zram0/mem_used", Scope.MEMORY),
        Node("battery_capacity", "/sys/class/power_supply/battery/capacity", Scope.POWER),
        Node("battery_status", "/sys/class/power_supply/battery/status", Scope.POWER),
        Node("touch_game_switch", "/sys/touchpanel/game_switch_enable", Scope.PLATFORM),
        Node("touch_double_tap", "/sys/touchpanel/double_tap_enable", Scope.PLATFORM),
        Node("ged_gpu_dvfs", "/sys/module/ged/parameters/gpu_dvfs_enable", Scope.PLATFORM),
        Node("soc_machine", "/sys/devices/soc0/machine", Scope.PLATFORM),
        Node("soc_id", "/sys/devices/soc0/soc_id", Scope.PLATFORM),
    )

    /** العُقد مجمَّعةً بترتيب [Scope] الثابت، ليعرضها سطح القراءة بلا ترتيبٍ محلّي مختلف في كل شاشة. */
    fun byScope(): List<Pair<Scope, List<Node>>> =
        Scope.entries.map { scope -> scope to nodes.filter { it.scope == scope } }
            .filter { it.second.isNotEmpty() }
}
