/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.display

import nd.max.core.hardware.HardwareControlKey
import nd.max.core.hardware.NodeControl
import nd.max.core.hardware.NodeTuning
import nd.max.core.hardware.NodeValueFormat

/** KCAL's scalar/RGB ABI only. OEM-specific colour blobs and framebuffer values are not guessed. */
object PanelColorTuning {
    private val roots = listOf("/sys/devices/platform/kcal_ctrl.0", "/sys/devices/platform/kcal_ctrl")

    fun discover(io: NodeTuning.Io = NodeTuning.SystemIo): List<NodeControl> {
        for (root in roots) {
            // Disabled KCAL may read successfully without affecting the panel, so no false control.
            if (io.read("$root/kcal_enable")?.trim() != "1") continue
            val controls = buildList {
                val minimum = NodeValueFormat.parseNumber(io.read("$root/kcal_min"))
                if (minimum != null && minimum in 0..256) {
                    // Keep enough light to read the restore action even with all channels lowered.
                    val format = NodeValueFormat.Vector(3, minimum.coerceAtLeast(64), 256)
                    val value = format.normalise(io.read("$root/kcal"))
                    if (value != null) add(NodeControl(
                        HardwareControlKey.panelColor("$root/kcal"), "$root/kcal", "rgb", value, format,
                        contextPath = "$root/kcal_enable", contextValue = "1",
                    ))
                }
                val fields = listOf(
                    "kcal_sat" to NodeValueFormat.Number(224, 383, 8),
                    "kcal_cont" to NodeValueFormat.Number(128, 383, 8),
                    "kcal_val" to NodeValueFormat.Number(128, 383, 8),
                )
                for ((field, format) in fields) {
                    val path = "$root/$field"
                    val value = format.normalise(io.read(path)) ?: continue
                    add(NodeControl(HardwareControlKey.panelColor(path), path, field, value, format,
                        contextPath = "$root/kcal_enable", contextValue = "1"))
                }
            }
            if (controls.isNotEmpty()) return controls
        }
        return emptyList()
    }
}
