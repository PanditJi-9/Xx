/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/** Never infer motor voltage limits from the current value; the driver must publish its bounds. */
object HapticTuning {
    private val roots = listOf(
        "/sys/class/timed_output/vibrator",
        "/sys/class/leds/vibrator",
        "/sys/class/leds/vibrator/device",
    )

    fun discover(io: NodeTuning.Io = NodeTuning.SystemIo): List<NodeControl> {
        for (root in roots) {
            fun limits(minPath: String, maxPath: String): NodeValueFormat.Number? {
                val minimum = NodeValueFormat.parseNumber(io.read(minPath)) ?: return null
                val maximum = NodeValueFormat.parseNumber(io.read(maxPath)) ?: return null
                if (maximum <= minimum || maximum > 100000) return null
                return NodeValueFormat.Number(minimum, maximum, ((maximum - minimum) / 20).coerceAtLeast(1))
            }
            val format = limits("$root/vtg_min", "$root/vtg_max")
            val controls = buildList {
                val common = format?.normalise(io.read("$root/vtg_level"))
                if (common != null && format != null) {
                    val path = "$root/vtg_level"
                    add(NodeControl(HardwareControlKey.hapticTuning(path), path, "intensity", common, format))
                }
                // Split-channel overrides are meaningful only when the kernel has already enabled them.
                if (io.read("$root/vmax_override")?.trim() == "1") {
                    for ((field, label) in listOf("vmax_mv_user" to "touch", "vmax_mv_strong" to "notification", "vmax_mv_call" to "call")) {
                        val path = "$root/$field"
                        // vtg_level may be amplitude units, not mV. Split channels need mV bounds of their own.
                        val channelFormat = limits("${path}_min", "${path}_max")
                            ?: limits("$root/vmax_min", "$root/vmax_max") ?: continue
                        val value = channelFormat.normalise(io.read(path)) ?: continue
                        add(NodeControl(HardwareControlKey.hapticTuning(path), path, label, value, channelFormat,
                            contextPath = "$root/vmax_override", contextValue = "1"))
                    }
                }
            }
            if (controls.isNotEmpty()) return controls
        }
        return emptyList()
    }
}
