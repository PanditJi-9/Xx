/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.gamespace

import nd.max.core.gamespace.GameApp
import nd.max.core.gamespace.GamePlayStats
import nd.max.core.gamespace.PlaySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class NexusStateTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val now = Instant.parse("2026-11-13T12:00:00Z").toEpochMilli()

    private fun ago(days: Long, hours: Long = 0): Long =
        now - days * 24L * 60L * 60L * 1_000L - hours * 60L * 60L * 1_000L

    private fun game(pkg: String, label: String = pkg) = GameApp(pkg, label, detectedGame = true)

    private fun stats(pkg: String, lastAt: Long, totalMs: Long, count: Int) =
        GamePlayStats(pkg, lastAt, totalMs, count)

    @Test
    fun `empty library reads nothing, not zero`() {
        val snapshot = NexusState.snapshot(
            games = emptyList(),
            stats = emptyMap(),
            sessions = emptyList(),
            armed = emptySet(),
            overlayGranted = false,
            tweaks = emptyMap(),
            selected = null,
            nowMs = now,
            zone = zone,
        )

        assertEquals(0L, snapshot.totalMs)
        assertEquals(0, snapshot.sessionCount)
        assertEquals(0, snapshot.gamesPlayed)
        assertEquals(0, snapshot.streakDays)
        assertEquals(0, snapshot.readyDone)
        assertEquals(NexusSnapshot.READY_TOTAL, snapshot.readyTotal)
        assertNull(snapshot.recent)
    }

    @Test
    fun `sums only library games and keeps the latest one`() {
        val library = listOf(game("a.game"), game("b.game"))
        val stats = mapOf(
            "a.game" to stats("a.game", ago(1), 3_600_000L, 2),
            "b.game" to stats("b.game", ago(3), 1_800_000L, 1),
            "removed.game" to stats("removed.game", ago(0), 9_000_000L, 7),
        )

        val snapshot = NexusState.snapshot(
            games = library,
            stats = stats,
            sessions = emptyList(),
            armed = emptySet(),
            overlayGranted = false,
            tweaks = emptyMap(),
            selected = library.first(),
            nowMs = now,
            zone = zone,
        )

        assertEquals(5_400_000L, snapshot.totalMs)
        assertEquals(3, snapshot.sessionCount)
        assertEquals(2, snapshot.gamesPlayed)
        assertEquals("a.game", snapshot.recent?.packageName)
    }

    @Test
    fun `readiness counts only the selected game and real handles`() {
        val selected = game("a.game")
        val other = game("b.game")
        val snapshot = NexusState.snapshot(
            games = listOf(selected, other),
            stats = emptyMap(),
            sessions = emptyList(),
            armed = setOf("b.game"),
            overlayGranted = true,
            tweaks = mapOf("b.game" to 4),
            selected = selected,
            nowMs = now,
            zone = zone,
        )

        assertEquals(1, snapshot.readyDone)
        assertEquals(3, snapshot.readyTotal)
    }

    @Test
    fun `full readiness needs profile panel and overlay together`() {
        val selected = game("a.game")
        val snapshot = NexusState.snapshot(
            games = listOf(selected),
            stats = emptyMap(),
            sessions = emptyList(),
            armed = setOf("a.game"),
            overlayGranted = true,
            tweaks = mapOf("a.game" to 2),
            selected = selected,
            nowMs = now,
            zone = zone,
        )

        assertEquals(3, snapshot.readyDone)
    }

    @Test
    fun `the ring names the first missing handle in order`() {
        val selected = game("a.game")

        fun gapOf(armed: Set<String>, overlay: Boolean, tweaks: Map<String, Int>) = NexusState.snapshot(
            games = listOf(selected),
            stats = emptyMap(),
            sessions = emptyList(),
            armed = armed,
            overlayGranted = overlay,
            tweaks = tweaks,
            selected = selected,
            nowMs = now,
            zone = zone,
        ).gap

        assertEquals(ReadyGap.Profile, gapOf(emptySet(), false, emptyMap()))
        assertEquals(ReadyGap.Panel, gapOf(emptySet(), true, mapOf("a.game" to 3)))
        assertEquals(ReadyGap.Overlay, gapOf(setOf("a.game"), false, mapOf("a.game" to 3)))
        assertNull(gapOf(setOf("a.game"), true, mapOf("a.game" to 3)))
    }

    @Test
    fun `a game whose profile is empty counts as missing, not saved`() {
        val selected = game("a.game")
        val snapshot = NexusState.snapshot(
            games = listOf(selected),
            stats = emptyMap(),
            sessions = emptyList(),
            armed = setOf("a.game"),
            overlayGranted = true,
            tweaks = mapOf("a.game" to 0),
            selected = selected,
            nowMs = now,
            zone = zone,
        )

        assertEquals(ReadyGap.Profile, snapshot.gap)
        assertEquals(2, snapshot.readyDone)
    }

    @Test
    fun `no selected game has no gap to name`() {
        val snapshot = NexusState.snapshot(
            games = emptyList(),
            stats = emptyMap(),
            sessions = emptyList(),
            armed = emptySet(),
            overlayGranted = false,
            tweaks = emptyMap(),
            selected = null,
            nowMs = now,
            zone = zone,
        )

        assertNull(snapshot.gap)
        assertEquals(0, snapshot.readyDone)
    }

    @Test
    fun `streak counts consecutive local days ending today`() {
        val sessions = listOf(
            PlaySession("a.game", ago(2, 2), ago(2)),
            PlaySession("a.game", ago(1, 2), ago(1)),
            PlaySession("a.game", ago(0, 2), ago(0)),
        )

        assertEquals(3, NexusState.streak(sessions, setOf("a.game"), now, zone))
    }

    @Test
    fun `streak stops at the first missing day and dies after yesterday`() {
        val alive = listOf(
            PlaySession("a.game", ago(2, 2), ago(2)),
            PlaySession("a.game", ago(1, 2), ago(1)),
        )
        val gap = listOf(
            PlaySession("a.game", ago(3, 2), ago(3)),
            PlaySession("a.game", ago(1, 2), ago(1)),
        )
        val dead = listOf(PlaySession("a.game", ago(4, 2), ago(4)))

        assertEquals(2, NexusState.streak(alive, setOf("a.game"), now, zone))
        assertEquals(1, NexusState.streak(gap, setOf("a.game"), now, zone))
        assertEquals(0, NexusState.streak(dead, setOf("a.game"), now, zone))
    }

    @Test
    fun `streak ignores removed games and future sessions`() {
        val sessions = listOf(
            PlaySession("removed.game", ago(1, 2), ago(1)),
            PlaySession("a.game", now + 60_000L, now + 120_000L),
        )

        assertEquals(0, NexusState.streak(sessions, setOf("a.game"), now, zone))
    }
}
