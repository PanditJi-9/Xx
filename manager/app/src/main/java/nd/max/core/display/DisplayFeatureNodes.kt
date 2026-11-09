/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.display

import nd.max.core.hardware.RootFileAccess
import nd.max.core.hardware.WriteVerification
import nd.max.core.hardware.HardwareControlKey
import nd.max.core.hardware.NodeControl
import nd.max.core.hardware.NodeTuning
import nd.max.core.hardware.NodeValueFormat
import nd.max.core.hardware.NodeTuningOutcome

/**
 * A vendor display feature exposed through a sysfs node.
 *
 * Panel features (sunlight/HBM, silky brightness, video enhancement, HDR) are
 * not a common kernel API: every OEM names the node differently. A candidate
 * carries the path it may live at and the exact values that mean on and off,
 * so probing stays data rather than a branch per device.
 */
data class DisplayFeatureNode(
    val path: String,
    val onValue: String,
    val offValue: String
)

/**
 * The single list of known display feature nodes.
 *
 * Both the Display Studio screen and the Quick Settings tile resolve their
 * nodes here. They used to be private to the screen's view model, which meant a
 * tile would have had to keep a second copy of the same paths — two lists that
 * drift apart, and a tile that claims support for a node the screen never sees.
 *
 * **A missing node is a supported answer, not an error:** [resolve] returns
 * `null`, and callers report "not supported" instead of writing to a guessed
 * path.
 */
object DisplayFeatureNodes {

    val SUNLIGHT: List<DisplayFeatureNode> = listOf(
        DisplayFeatureNode("/sys/class/leds/lcd-backlight/hbm", "1", "0"),
        DisplayFeatureNode("/sys/devices/platform/mtk-hbm/hbm_enable", "1", "0"),
        DisplayFeatureNode("/sys/class/backlight/panel0-backlight/hbm_mode", "1", "0"),
        DisplayFeatureNode("/sys/class/drm/card0-DSI-1/hbm", "1", "0"),
    )

    val SILKY: List<DisplayFeatureNode> = listOf(
        DisplayFeatureNode("/sys/class/graphics/fb0/aod_brightness_smooth", "1", "0"),
        DisplayFeatureNode("/proc/silky_brightness/enable", "1", "0")
    )

    val VIDEO_ENHANCE: List<DisplayFeatureNode> = listOf(
        DisplayFeatureNode("/sys/class/graphics/fb0/video_enhance", "1", "0"),
        DisplayFeatureNode("/proc/mtk_video_enhance/enable", "1", "0")
    )

    val HDR: List<DisplayFeatureNode> = listOf(
        DisplayFeatureNode("/sys/class/graphics/fb0/hdr_enable", "1", "0"),
        DisplayFeatureNode("/proc/dolby_vision/enable", "1", "0")
    )

    /** First candidate with a readable recognised state; mere file existence is not a working control. */
    fun resolve(candidates: List<DisplayFeatureNode>): DisplayFeatureNode? =
        candidates.firstOrNull { node -> isEnabled(node) != null }

    /** Raw node contents, or `null` when the node cannot be read. */
    fun read(node: DisplayFeatureNode): String? = RootFileAccess.read(node.path)

    /**
     * Live state from the node itself.
     *
     * `null` means *unreadable*, never "off": a node whose read fails must not
     * be rendered as a confident off state.
     */
    fun isEnabled(node: DisplayFeatureNode): Boolean? {
        val raw = read(node)?.trim() ?: return null
        return when (raw) { node.onValue -> true; node.offValue -> false; else -> null }
    }

    /**
     * Writes the feature and reads it back before claiming anything.
     *
     * Returns the verification outcome so callers can distinguish "written and
     * confirmed" from "write failed" or "readback differs".
     */
    fun write(node: DisplayFeatureNode, enabled: Boolean): WriteVerification.Outcome =
        RootFileAccess.writeVerified(
            node.path,
            if (enabled) node.onValue else node.offValue
        )

    /** Guarded manual variant used by the screen and tile; both share the same canonical identity. */
    fun writeGuarded(node: DisplayFeatureNode, enabled: Boolean, tuning: NodeTuning): WriteVerification.Outcome {
        val format = NodeValueFormat.Choice(listOf(node.offValue, node.onValue))
        val live = format.normalise(read(node)) ?: return WriteVerification.Outcome.READBACK_UNAVAILABLE
        val control = NodeControl(HardwareControlKey.panelFeature(node.path), node.path, "panel", live, format)
        return when (tuning.apply(control, if (enabled) node.onValue else node.offValue).outcome) {
            NodeTuningOutcome.APPLIED -> WriteVerification.Outcome.MATCHED
            NodeTuningOutcome.UNVERIFIED -> WriteVerification.Outcome.DIFFERS
            NodeTuningOutcome.UNREADABLE -> WriteVerification.Outcome.READBACK_UNAVAILABLE
            else -> WriteVerification.Outcome.WRITE_FAILED
        }
    }
}
