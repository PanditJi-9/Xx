/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandPaletteModelTest {

    private val games = listOf(
        PaletteGame("com.tencent.ig", "PUBG Mobile"),
        PaletteGame("com.mojang.minecraftpe", "Minecraft"),
        PaletteGame("com.riotgames.league.wildrift", "Wild Rift")
    )

    private val actions = listOf(
        PaletteAction(PaletteActionId.Launch, "Launch", "Start the selected game"),
        PaletteAction(PaletteActionId.Vault, "Library", "Browse every game"),
        PaletteAction(PaletteActionId.Close, "Close", "Leave Game Space")
    )

    @Test
    fun `empty query shows games first and keeps actions visible`() {
        val hits = PaletteSearch.filter("", games, actions)
        assertEquals(6, hits.size)
        assertTrue(hits.take(3).all { it is PaletteHit.Game })
        assertTrue(hits.drop(3).all { it is PaletteHit.Action })
    }

    @Test
    fun `empty query caps a long game list before the actions`() {
        val many = (1..20).map { PaletteGame("com.example.$it", "Game $it") }
        val hits = PaletteSearch.filter("", many, actions)
        assertEquals(9, hits.size)
        assertEquals(6, hits.count { it is PaletteHit.Game })
        assertEquals(3, hits.count { it is PaletteHit.Action })
    }

    @Test
    fun `query matches a label regardless of case`() {
        val hits = PaletteSearch.filter("mine", games, actions)
        assertEquals(1, hits.size)
        assertEquals("com.mojang.minecraftpe", (hits.single() as PaletteHit.Game).game.packageName)
    }

    @Test
    fun `query matches a package name`() {
        val hits = PaletteSearch.filter("tencent", games, actions)
        assertEquals("PUBG Mobile", (hits.single() as PaletteHit.Game).game.label)
    }

    @Test
    fun `query matches an action detail`() {
        val hits = PaletteSearch.filter("leave", games, actions)
        assertEquals(PaletteActionId.Close, (hits.single() as PaletteHit.Action).action.id)
    }

    @Test
    fun `query trims surrounding whitespace`() {
        val hits = PaletteSearch.filter("  rift  ", games, actions)
        assertEquals("Wild Rift", (hits.single() as PaletteHit.Game).game.label)
    }

    @Test
    fun `no match returns an honest empty list`() {
        assertTrue(PaletteSearch.filter("zzzz", games, actions).isEmpty())
    }

    @Test
    fun `limit never returns more than asked and rejects zero`() {
        assertEquals(2, PaletteSearch.filter("", games, actions, limit = 2).size)
        assertTrue(PaletteSearch.filter("", games, actions, limit = 0).isEmpty())
    }
}
