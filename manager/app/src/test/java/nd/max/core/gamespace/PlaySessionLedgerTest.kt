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

class PlaySessionLedgerTest {

    private val hour = 3_600_000L
    private val minute = 60_000L

    private fun session(pkg: String, start: Long, end: Long) = PlaySession(pkg, start, end)

    @Test
    fun `invalid sessions are ignored instead of entering the ledger`() {
        val valid = session("com.example.game", 1_000_000L, 1_000_000L + 10 * minute)
        assertEquals(listOf(valid), PlaySessionLedger.append(emptyList(), valid))
        assertEquals(listOf(valid), PlaySessionLedger.append(listOf(valid), valid))

        assertTrue(PlaySessionLedger.append(emptyList(), session("bad", 1_000L, 2_000L + 10 * minute)).isEmpty())
        assertTrue(PlaySessionLedger.append(emptyList(), session("com.example.game", 0L, 10 * minute)).isEmpty())
        assertTrue(
            PlaySessionLedger.append(
                emptyList(),
                session("com.example.game", 1_000_000L, 1_000_000L + PlaySessionLedger.MIN_DURATION_MS - 1L)
            ).isEmpty()
        )
        assertFalse(
            PlaySessionLedger.valid(
                session(
                    "com.example.game",
                    1_000_000L,
                    1_000_000L + PlaySessionLedger.MAX_DURATION_MS + 1L
                )
            )
        )
    }

    @Test
    fun `the ledger keeps the newest bounded sessions`() {
        var sessions = emptyList<PlaySession>()
        repeat(PlaySessionLedger.MAX_SESSIONS + 5) { index ->
            sessions = PlaySessionLedger.append(
                sessions,
                session("com.example.game", 1_000_000L + index * hour, 1_000_000L + index * hour + 10 * minute)
            )
        }
        assertEquals(PlaySessionLedger.MAX_SESSIONS, sessions.size)
        assertEquals(
            1_000_000L + (PlaySessionLedger.MAX_SESSIONS + 4) * hour + 10 * minute,
            sessions.last().endedAtMs
        )
    }

    @Test
    fun `round trip preserves valid sessions and skips malformed lines`() {
        val a = session("com.example.one", 1_000_000L, 1_000_000L + 30 * minute)
        val b = session("com.example.two", 2_000_000L, 2_000_000L + 45 * minute)
        val encoded = PlaySessionLedger.encode(listOf(b, a))
        assertTrue(encoded.startsWith("v1\n"))
        assertEquals(listOf(a, b), PlaySessionLedger.parse(encoded))
        assertEquals(emptyList<PlaySession>(), PlaySessionLedger.parse("garbage\ncom.example.one\tx\ty\n"))
        assertEquals(listOf(a), PlaySessionLedger.parse(PlaySessionLedger.encode(listOf(a)) + "broken\n"))
    }

    @Test
    fun `stats aggregate total time last play and session count`() {
        val first = session("com.example.one", 1_000_000L, 1_000_000L + 10 * minute)
        val second = session("com.example.one", 2_000_000L, 2_000_000L + 20 * minute)
        val other = session("com.example.two", 3_000_000L, 3_000_000L + 5 * minute)
        val stats = PlaySessionLedger.stats(listOf(first, second, other))
        assertEquals(2, stats.getValue("com.example.one").sessionCount)
        assertEquals(30 * minute, stats.getValue("com.example.one").totalMs)
        assertEquals(2_000_000L + 20 * minute, stats.getValue("com.example.one").lastPlayedAtMs)
        assertEquals(5 * minute, stats.getValue("com.example.two").totalMs)
    }

    @Test
    fun `relative age never invents a session and buckets the known ones`() {
        val now = 10 * hour
        assertEquals(PlayAge.None, PlaySessionLedger.playAge(now, null))
        assertEquals(PlayAge.None, PlaySessionLedger.playAge(now, now + 1L))
        assertEquals(PlayAge.Minutes(1), PlaySessionLedger.playAge(now, now - 1_000L))
        assertEquals(PlayAge.Minutes(59), PlaySessionLedger.playAge(now, now - 59 * minute))
        assertEquals(PlayAge.Hours(1), PlaySessionLedger.playAge(now, now - hour))
        assertEquals(PlayAge.Hours(23), PlaySessionLedger.playAge(now, now - 23 * hour))
        assertEquals(PlayAge.Yesterday, PlaySessionLedger.playAge(now, now - 24 * hour))
        assertEquals(PlayAge.Days(2), PlaySessionLedger.playAge(now, now - 48 * hour))
    }

    @Test
    fun `play minutes are absent below the minimum and rounded up after it`() {
        assertNull(PlaySessionLedger.playMinutes(0L))
        assertNull(PlaySessionLedger.playMinutes(PlaySessionLedger.MIN_DURATION_MS - 1L))
        assertEquals(1, PlaySessionLedger.playMinutes(PlaySessionLedger.MIN_DURATION_MS))
        assertEquals(61, PlaySessionLedger.playMinutes(61 * minute))
    }

    @Test
    fun `tracker closes on another app starts on a new game and ignores unreadable samples`() {
        val tracker = PlaySessionTracker()
        assertNull(tracker.onForeground("com.example.one", 1_000_000L))
        assertEquals("com.example.one", tracker.currentPackage())

        val switched = tracker.onForeground("com.example.two", 1_000_000L + 10 * minute)
        assertEquals("com.example.one", switched?.packageName)
        assertEquals("com.example.two", tracker.currentPackage())

        val leftGame = tracker.onForeground(null, 1_000_000L + 20 * minute)
        assertEquals("com.example.two", leftGame?.packageName)
        assertNull(tracker.currentPackage())

        tracker.onForeground("com.example.one", 2_000_000L)
        val closed = tracker.close(2_000_000L + 15 * minute)
        assertEquals("com.example.one", closed?.packageName)
        assertEquals(15 * minute, closed?.durationMs)
        assertNull(tracker.close(3_000_000L))
    }
}
