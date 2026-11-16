/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.navigation

import nd.max.ui.design.MaxIllustrationKind
import nd.max.ui.design.maxIllustrationKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** The control page and the screen chrome both read the destination's mark, not a local guess. */
class MaxDestinationIllustrationsTest {
    @Test
    fun `tool screens keep their own marks on control and in the top bar`() {
        val expected = mapOf(
            MaxDestination.DynamicIsland to MaxIllustrationKind.Island,
            MaxDestination.FpsOverlay to MaxIllustrationKind.Overlay,
            MaxDestination.ClipAlbum to MaxIllustrationKind.Clips,
            MaxDestination.GameSessions to MaxIllustrationKind.Sessions,
            MaxDestination.ManifestInspector to MaxIllustrationKind.Manifest,
            MaxDestination.NetworkScanner to MaxIllustrationKind.Scan,
            MaxDestination.UsageStats to MaxIllustrationKind.Usage,
            MaxDestination.BatteryHistory to MaxIllustrationKind.BatteryHistory,
        )

        expected.forEach { (destination, kind) ->
            assertEquals(destination.route, kind, destination.illustrationKind)
        }
        assertEquals(
            "each tool must own a distinct silhouette, not share the generic Controls mark",
            expected.size,
            expected.values.toSet().size,
        )
    }

    @Test
    fun `manifest inspector no longer shares app settings mark`() {
        assertNotEquals(
            MaxDestination.AppSettings.illustrationKind,
            MaxDestination.ManifestInspector.illustrationKind,
        )
    }

    @Test
    fun `material icon names for the new marks resolve`() {
        assertEquals(MaxIllustrationKind.Clips, maxIllustrationKind("Rounded.VideoLibrary"))
        assertEquals(MaxIllustrationKind.Sessions, maxIllustrationKind("Rounded.History"))
        assertEquals(MaxIllustrationKind.Island, maxIllustrationKind("Rounded.SmartButton"))
        assertEquals(MaxIllustrationKind.Manifest, maxIllustrationKind("Rounded.Policy"))
        assertEquals(MaxIllustrationKind.Usage, maxIllustrationKind("Rounded.QueryStats"))
    }
}
