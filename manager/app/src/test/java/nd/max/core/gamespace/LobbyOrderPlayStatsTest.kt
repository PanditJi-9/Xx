/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Test

class LobbyOrderPlayStatsTest {

    private fun game(pkg: String, label: String) = GameApp(pkg, label, detectedGame = true)

    @Test
    fun `most played sorts by total time and keeps favorites as the tie break`() {
        val games = listOf(
            game("com.example.a", "Alpha"),
            game("com.example.b", "Beta"),
            game("com.example.c", "Gamma"),
            game("com.example.d", "Delta"),
        )
        val stats = mapOf(
            "com.example.a" to GamePlayStats("com.example.a", 1L, 10_000L, 1),
            "com.example.b" to GamePlayStats("com.example.b", 2L, 90_000L, 1),
            "com.example.c" to GamePlayStats("com.example.c", 3L, 10_000L, 1),
        )
        val order = LobbyOrder.sort(
            games = games,
            favorites = setOf("com.example.c"),
            order = LobbySort.MostPlayed,
            installedDays = emptyMap(),
            playStats = stats,
        )
        assertEquals(listOf("com.example.b", "com.example.c", "com.example.a", "com.example.d"), order.map { it.packageName })
    }

    @Test
    fun `most played with no records falls back to label order`() {
        val games = listOf(game("com.example.z", "Zulu"), game("com.example.a", "Alpha"))
        val order = LobbyOrder.sort(
            games = games,
            favorites = emptySet(),
            order = LobbySort.MostPlayed,
            installedDays = emptyMap(),
        )
        assertEquals(listOf("com.example.a", "com.example.z"), order.map { it.packageName })
    }
}
