/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LobbyPrefsModelTest {
    @Test
    fun `unknown or missing stored values fall back to the default`() {
        assertEquals(LobbyDepth.Deep, lobbyEnumOr(null, LobbyDepth.entries, LobbyDepth.Deep))
        assertEquals(LobbyDepth.Deep, lobbyEnumOr("Gigantic", LobbyDepth.entries, LobbyDepth.Deep))
        assertEquals(LobbySort.Name, lobbyEnumOr("Name", LobbySort.entries, LobbySort.Favorites))
        assertEquals(LobbyDepth.Flat, lobbyEnumOr("Flat", LobbyDepth.entries, LobbyDepth.Deep))
    }

    @Test
    fun `the 3D layout is on but decorative effects stay off by default`() {
        val d = LobbyPrefsState()
        assertEquals(LobbyDepth.Deep, d.depth)
        assertEquals(LobbySort.Favorites, d.sort)
        assertTrue(d.gameColors && d.showStatus && d.haptics && d.resumeLast)
        assertFalse(d.ambient)
        assertFalse(d.launchFlash)
        assertFalse(d.keepScreenOn)
        assertNull(d.lastPackage)
    }

    @Test
    fun `sounds and the cinematic entrance are on by explicit owner request`() {
        // استثناء مقصود من «الزينة مُطفأة» (ADR-12): طلب المالك ٢٠٢٦-١٠-٢١ الأصوات والدخول صراحةً.
        val d = LobbyPrefsState()
        assertTrue(d.sounds)
        assertTrue(d.entrance)
    }

    @Test
    fun `depth strengths are ordered flat soft deep`() {
        assertTrue(LobbyDepth.Flat.strength < LobbyDepth.Soft.strength)
        assertTrue(LobbyDepth.Soft.strength < LobbyDepth.Deep.strength)
        assertEquals(0f, LobbyDepth.Flat.strength, 0f)
    }
}
