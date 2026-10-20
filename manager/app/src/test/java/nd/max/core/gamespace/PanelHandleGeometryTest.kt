/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Test

class PanelHandleGeometryTest {

    @Test
    fun handleLeftOfCenterDocksToStart() {
        assertEquals(PanelSide.Start, PanelHandleGeometry.sideFor(300f, 2400f, PanelSide.End))
    }

    @Test
    fun handleRightOfCenterDocksToEnd() {
        assertEquals(PanelSide.End, PanelHandleGeometry.sideFor(1900f, 2400f, PanelSide.Start))
    }

    @Test
    fun unknownWidthKeepsCurrentSide() {
        assertEquals(PanelSide.End, PanelHandleGeometry.sideFor(100f, 0f, PanelSide.End))
        assertEquals(PanelSide.Start, PanelHandleGeometry.sideFor(100f, -5f, PanelSide.Start))
    }

    @Test
    fun inwardOffsetIsZeroWhenFlushWithTheEdge() {
        assertEquals(0f, PanelHandleGeometry.inwardFrom(PanelSide.Start, 0f, 60f, 2400f), 0f)
        assertEquals(0f, PanelHandleGeometry.inwardFrom(PanelSide.End, 2340f, 60f, 2400f), 0f)
    }

    @Test
    fun inwardOffsetGrowsTowardTheScreenCenter() {
        assertEquals(500f, PanelHandleGeometry.inwardFrom(PanelSide.Start, 500f, 60f, 2400f), 0f)
        assertEquals(500f, PanelHandleGeometry.inwardFrom(PanelSide.End, 1840f, 60f, 2400f), 0f)
    }

    @Test
    fun sensitivityNamesRoundTripAndHigherMeansShorterPull() {
        assert(PanelSensitivity.High.distanceFactor < PanelSensitivity.Balanced.distanceFactor)
        assert(PanelSensitivity.Balanced.distanceFactor < PanelSensitivity.Low.distanceFactor)
        assertEquals(PanelSensitivity.Balanced, PanelSensitivity.entries.first { it.name == "Balanced" })
    }
}
