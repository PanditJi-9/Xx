/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.display

import nd.max.core.hardware.RootFileAccess
import nd.max.core.hardware.WriteVerification

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
        DisplayFeatureNode("/sys/devices/platform/mtk-hbm/hbm_enable", "1", "0")
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

    /** First candidate whose node actually exists, or `null` when none does. */
    fun resolve(candidates: List<DisplayFeatureNode>): DisplayFeatureNode? =
        RootFileAccess.firstExisting(candidates) { it.path }

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
        return raw == node.onValue
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
}
