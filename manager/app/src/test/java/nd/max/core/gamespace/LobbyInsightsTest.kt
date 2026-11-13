/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حارس مشتقّات «الرؤى» و«الاستكشاف» — نقيّ على JVM، بلا أندرويد.
 *
 * ما يقيسه: أن الأرقام تُشتق من السجلّ القائم وحده، وأن نافذة «هذا الأسبوع» شاملة الطرفين وترفض
 * المستقبل، وأن البحث لا يغيّر الترتيب ولا يفرّق بين حالة الأحرف. وما لا يقيسه: الشكل على الشاشة.
 */
class LobbyInsightsTest {

    private val now = 1_700_000_000_000L
    private val minute = 60_000L

    private fun game(pkg: String, label: String = pkg) = GameApp(pkg, label, detectedGame = true)

    private fun stat(pkg: String, lastMs: Long, minutes: Long, sessions: Int = 1) =
        GamePlayStats(pkg, lastPlayedAtMs = lastMs, totalMs = minutes * minute, sessionCount = sessions)

    @Test
    fun `summary counts library games only and keeps the top three`() {
        val library = setOf("com.a", "com.b", "com.c", "com.d")
        val stats = mapOf(
            "com.a" to stat("com.a", now - minute, 10),
            "com.b" to stat("com.b", now - minute, 5, sessions = 2),
            "com.c" to stat("com.c", now - minute, 20),
            "com.outside" to stat("com.outside", now - minute, 100),
        )

        val summary = LobbyInsights.summary(stats, library)

        assertEquals(35 * minute, summary.totalMs)
        assertEquals(4, summary.sessionCount)
        assertEquals(3, summary.gamesPlayed)
        assertEquals(1, summary.neverPlayed)
        assertEquals(listOf("com.c", "com.a", "com.b"), summary.top.map { it.packageName })
    }

    @Test
    fun `summary of an empty ledger reports zeros without inventing a game`() {
        val summary = LobbyInsights.summary(emptyMap(), setOf("com.a", "com.b"))

        assertEquals(0L, summary.totalMs)
        assertEquals(0, summary.sessionCount)
        assertEquals(0, summary.gamesPlayed)
        assertEquals(2, summary.neverPlayed)
        assertTrue(summary.top.isEmpty())
    }

    @Test
    fun `setup order is fixed and every flag reads its own source`() {
        val empty = LobbyInsights.setup(
            stats = null,
            tweaks = 0,
            panelArmed = false,
            nowMs = now
        )
        assertEquals(
            listOf(SetupKind.Profile, SetupKind.Panel, SetupKind.History, SetupKind.Recent),
            empty.map { it.kind }
        )
        assertTrue(empty.none { it.done })
        assertEquals(0, LobbyInsights.readyCount(empty))

        val ready = LobbyInsights.setup(
            stats = stat("com.a", now - minute, 10),
            tweaks = 2,
            panelArmed = true,
            nowMs = now
        )
        assertTrue(ready.all { it.done })
        assertEquals(LobbyInsights.SETUP_TOTAL, LobbyInsights.readyCount(ready))
    }

    @Test
    fun `recent window is inclusive and the future is not recent`() {
        val recent = stat("com.a", now - LobbyInsights.RECENT_WINDOW_MS, 10)
        val older = stat("com.a", now - LobbyInsights.RECENT_WINDOW_MS - 1, 10)
        val future = stat("com.a", now + 1, 10)
        val never = stat("com.a", 0, 0, sessions = 0)

        assertTrue(LobbyInsights.isRecent(recent, now))
        assertFalse(LobbyInsights.isRecent(older, now))
        assertFalse(LobbyInsights.isRecent(future, now))
        assertFalse(LobbyInsights.isRecent(never, now))
    }

    @Test
    fun `filter searches labels and packages without case and keeps order`() {
        val games = listOf(
            game("com.alpha.one", "Alpha Strike"),
            game("com.beta.two", "Beta"),
        )

        assertEquals(
            listOf("com.alpha.one"),
            LobbyInsights.filter(games, LobbyFilter.All, "alpha", emptySet(), emptyMap(), now)
                .map { it.packageName }
        )
        assertEquals(
            listOf("com.beta.two"),
            LobbyInsights.filter(games, LobbyFilter.All, "COM.BETA", emptySet(), emptyMap(), now)
                .map { it.packageName }
        )
        assertEquals(
            games.map { it.packageName },
            LobbyInsights.filter(games, LobbyFilter.All, "  ", emptySet(), emptyMap(), now)
                .map { it.packageName }
        )
    }

    @Test
    fun `filter narrows by favorites played and recent`() {
        val games = listOf(game("com.a"), game("com.b"), game("com.c"))
        val favorites = setOf("com.a", "com.c")
        val stats = mapOf(
            "com.a" to stat("com.a", now - minute, 10),
            "com.b" to stat("com.b", now - LobbyInsights.RECENT_WINDOW_MS - 1, 10),
        )

        fun packages(filter: LobbyFilter) =
            LobbyInsights.filter(games, filter, "", favorites, stats, now).map { it.packageName }

        assertEquals(listOf("com.a", "com.b", "com.c"), packages(LobbyFilter.All))
        assertEquals(listOf("com.a", "com.c"), packages(LobbyFilter.Favorites))
        assertEquals(listOf("com.a", "com.b"), packages(LobbyFilter.Played))
        assertEquals(listOf("com.a"), packages(LobbyFilter.Recent))
        assertEquals(listOf("com.c"), packages(LobbyFilter.Unplayed))
    }

    @Test
    fun `counts are computed on the whole list before the text query`() {
        val games = listOf(game("com.a"), game("com.b"))
        val stats = mapOf("com.a" to stat("com.a", now - minute, 10))
        val counts = LobbyInsights.counts(games, setOf("com.b"), stats, now)

        assertEquals(2, counts[LobbyFilter.All])
        assertEquals(1, counts[LobbyFilter.Favorites])
        assertEquals(1, counts[LobbyFilter.Played])
        assertEquals(1, counts[LobbyFilter.Recent])
        assertEquals(1, counts[LobbyFilter.Unplayed])
    }
}
