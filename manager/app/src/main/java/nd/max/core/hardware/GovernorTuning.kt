/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/** Only reviewed scalar governor parameters are editable; discovery is not a generic sysfs editor. */
object GovernorTuning {
    data class Group(val policy: String, val governor: String, val controls: List<NodeControl>)

    private val formats = linkedMapOf(
        "up_rate_limit_us" to NodeValueFormat.Number(100, 1_000_000, 1000),
        "down_rate_limit_us" to NodeValueFormat.Number(100, 1_000_000, 1000),
        "rate_limit_us" to NodeValueFormat.Number(100, 1_000_000, 1000),
        "sampling_rate" to NodeValueFormat.Number(1000, 1_000_000, 1000),
        "sampling_down_factor" to NodeValueFormat.Number(1, 100),
        "up_threshold" to NodeValueFormat.Number(50, 100),
        "go_hispeed_load" to NodeValueFormat.Number(50, 100),
        "min_sample_time" to NodeValueFormat.Number(1000, 1_000_000, 1000),
        "timer_rate" to NodeValueFormat.Number(1000, 1_000_000, 1000),
        "hispeed_freq" to NodeValueFormat.Number(1, Long.MAX_VALUE, 100000),
    )

    fun discover(
        policies: List<CpuHardwareBackend.Policy> = CpuHardwareBackend.policies(),
        io: NodeTuning.Io = NodeTuning.SystemIo,
    ): List<Group> {
        val seenPaths = hashSetOf<String>()
        return policies.take(32).mapNotNull { policy ->
            val governor = policy.governor?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,40}")) } ?: return@mapNotNull null
            val candidates = listOf("${policy.path}/$governor", "/sys/devices/system/cpu/cpufreq/$governor")
            val controls = buildList {
                for ((field, initialFormat) in formats) {
                    val format = if (field == "hispeed_freq") {
                        val min = policy.provenMinKHz ?: continue
                        val max = policy.provenMaxKHz ?: continue
                        NodeValueFormat.Number(min, max, 100000)
                    } else initialFormat
                    for (directory in candidates) {
                        val path = "$directory/$field"
                        if (path in seenPaths) break
                        val value = format.normalise(io.read(path)) ?: continue
                        seenPaths += path
                        val identity = if (directory == candidates.last()) "global" else policy.name
                        val guards = if (identity == "global") {
                            policies.filter { it.governor == governor }.associate { "${it.path}/scaling_governor" to governor }
                        } else emptyMap()
                        add(NodeControl(
                            key = HardwareControlKey.governorTunable(identity, governor, field),
                            path = path,
                            label = field,
                            value = value,
                            format = format,
                            contextPath = "${policy.path}/scaling_governor",
                            contextValue = governor,
                            guards = guards,
                        ))
                        break
                    }
                }
            }
            if (controls.isEmpty()) null else Group(policy.name, governor, controls)
        }
    }

    /** Stored snapshots refer to known controls, never paths that an imported document can invent. */
    fun matchSnapshot(controls: List<NodeControl>, saved: Map<String, String>): List<Pair<NodeControl, String>>? {
        if (saved.isEmpty() || saved.size > 128) return null
        val indexed = controls.associateBy(NodeControl::key)
        val values = saved.map { (key, value) ->
            val control = indexed[key] ?: return null
            val normalized = control.format.normalise(value) ?: return null
            control to normalized
        }
        return values
    }
}
