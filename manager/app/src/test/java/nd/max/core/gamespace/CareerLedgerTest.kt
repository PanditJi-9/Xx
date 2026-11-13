/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * مسيرة المكتبة على JVM: النافذة، وأطول جلسة، وما بعد منتصف الليل، والمحطّات.
 * كل التواريخ مثبّتة، فالحكم لا يتغيّر مع ساعة الجهاز.
 */
class CareerLedgerTest {
    private val zone = ZoneOffset.UTC
    private val now = Instant.parse("2026-11-13T12:00:00Z").toEpochMilli()
    private val alpha = "com.alpha.game"
    private val beta = "com.beta.game"

    private fun session(pkg: String, start: String, end: String) = PlaySession(
        packageName = pkg,
        startedAtMs = Instant.parse(start).toEpochMilli(),
        endedAtMs = Instant.parse(end).toEpochMilli(),
    )

    private fun stats(pkg: String, last: String, totalMs: Long, count: Int) = GamePlayStats(
        packageName = pkg,
        lastPlayedAtMs = Instant.parse(last).toEpochMilli(),
        totalMs = totalMs,
        sessionCount = count,
    )

    @Test
    fun `empty ledger reads seven honest empty days and locked milestones`() {
        val career = CareerLedger.build(emptyMap(), emptyList(), setOf(alpha), now, zone)

        assertEquals(0L, career.totalMs)
        assertEquals(0, career.sessionCount)
        assertEquals(0, career.gamesPlayed)
        assertEquals(0, career.streakDays)
        assertEquals(7, career.week.size)
        assertEquals(LocalDate.parse("2026-11-07"), career.week.first().date)
        assertEquals(LocalDate.parse("2026-11-13"), career.week.last().date)
        assertTrue(career.week.all { it.totalMs == 0L })
        assertEquals(0L, career.lastWeekMs)
        assertEquals(0L, career.bestSessionMs)
        assertNull(career.bestSessionPackage)
        assertEquals(0, career.nightSessions)
        assertTrue(career.recent.isEmpty())
        assertTrue(career.top.isEmpty())
        assertEquals(CareerBadge.entries.size, career.milestones.size)
        assertTrue(career.milestones.none { it.unlocked })
    }

    @Test
    fun `totals and ranking count library games only`() {
        val stats = mapOf(
            alpha to stats(alpha, "2026-11-13T09:00:00Z", 3_600_000L, 2),
            beta to stats(beta, "2026-11-12T09:00:00Z", 7_200_000L, 1),
            "com.removed.game" to stats("com.removed.game", "2026-11-13T10:00:00Z", 9_000_000L, 4),
        )
        val career = CareerLedger.build(stats, emptyList(), setOf(alpha, beta), now, zone)

        assertEquals(10_800_000L, career.totalMs)
        assertEquals(3, career.sessionCount)
        assertEquals(2, career.gamesPlayed)
        assertEquals(listOf(beta, alpha), career.top.map { it.packageName })
    }

    @Test
    fun `week buckets by end date and ignores future removed or invalid sessions`() {
        val sessions = listOf(
            session(alpha, "2026-11-13T08:00:00Z", "2026-11-13T09:00:00Z"),
            session(alpha, "2026-11-08T23:30:00Z", "2026-11-09T00:30:00Z"),
            session(alpha, "2026-11-01T10:00:00Z", "2026-11-01T11:00:00Z"),
            session("com.removed.game", "2026-11-12T10:00:00Z", "2026-11-12T11:00:00Z"),
            session(alpha, "2026-11-14T10:00:00Z", "2026-11-14T11:00:00Z"),
            session(alpha, "2026-11-12T10:00:00Z", "2026-11-12T10:00:02Z"),
        )
        val career = CareerLedger.build(emptyMap(), sessions, setOf(alpha), now, zone)

        assertEquals(7, career.week.size)
        assertEquals(3_600_000L, career.week.last().totalMs)
        // جلسة عبرت منتصف الليل تُنسب إلى يوم انتهائها (٩ نوفمبر) لا إلى يوم بدايتها.
        assertEquals(3_600_000L, career.week.first { it.date == LocalDate.parse("2026-11-09") }.totalMs)
        assertEquals(0L, career.week.first { it.date == LocalDate.parse("2026-11-08") }.totalMs)
        assertEquals(2, career.week.count { it.totalMs > 0L })
        assertEquals(3_600_000L, career.bestSessionMs)
    }

    @Test
    fun `previous week is the seven days before the current window`() {
        val sessions = listOf(
            session(alpha, "2026-11-06T10:00:00Z", "2026-11-06T11:00:00Z"),
            session(alpha, "2026-10-31T10:00:00Z", "2026-10-31T11:00:00Z"),
            session(alpha, "2026-10-30T10:00:00Z", "2026-10-30T11:00:00Z"),
            session(alpha, "2026-11-07T10:00:00Z", "2026-11-07T11:00:00Z"),
            session(alpha, "2026-11-13T10:00:00Z", "2026-11-13T11:00:00Z"),
        )
        val career = CareerLedger.build(emptyMap(), sessions, setOf(alpha), now, zone)

        // ٦ و٣١ أكتوبر داخل الأسبوع السابق؛ ٣٠ أكتوبر خارجه؛ ٧ و١٣ نوفمبر في الحالي.
        assertEquals(7_200_000L, career.lastWeekMs)
        assertEquals(7_200_000L, career.week.sumOf { it.totalMs })
    }

    @Test
    fun `recent sessions are the newest valid library sessions only`() {
        val sessions = listOf(
            session(alpha, "2026-11-13T08:00:00Z", "2026-11-13T09:00:00Z"),
            session(beta, "2026-11-12T08:00:00Z", "2026-11-12T09:00:00Z"),
            session(alpha, "2026-11-11T08:00:00Z", "2026-11-11T09:00:00Z"),
            session(alpha, "2026-11-10T08:00:00Z", "2026-11-10T09:00:00Z"),
            session("com.removed.game", "2026-11-12T10:00:00Z", "2026-11-12T11:00:00Z"),
            session(alpha, "2026-11-14T10:00:00Z", "2026-11-14T11:00:00Z"),
            session(alpha, "2026-11-12T10:00:00Z", "2026-11-12T10:00:02Z"),
        )
        val career = CareerLedger.build(emptyMap(), sessions, setOf(alpha, beta), now, zone)

        assertEquals(3, career.recent.size)
        assertEquals(listOf(alpha, beta, alpha), career.recent.map { it.packageName })
        assertEquals(
            listOf(
                Instant.parse("2026-11-13T09:00:00Z").toEpochMilli(),
                Instant.parse("2026-11-12T09:00:00Z").toEpochMilli(),
                Instant.parse("2026-11-11T09:00:00Z").toEpochMilli()
            ),
            career.recent.map { it.endedAtMs }
        )
    }

    @Test
    fun `night sessions start after ten or before five`() {
        val sessions = listOf(
            session(alpha, "2026-11-12T22:30:00Z", "2026-11-12T23:00:00Z"),
            session(alpha, "2026-11-12T04:00:00Z", "2026-11-12T05:00:00Z"),
            session(alpha, "2026-11-12T20:00:00Z", "2026-11-12T21:00:00Z"),
        )
        val career = CareerLedger.build(emptyMap(), sessions, setOf(alpha), now, zone)

        assertEquals(2, career.nightSessions)
        assertEquals(3_600_000L, career.bestSessionMs)
        assertEquals(alpha, career.bestSessionPackage)
    }

    @Test
    fun `milestones clamp progress and unlock at their real targets`() {
        val stats = mapOf(
            alpha to stats(alpha, "2026-11-13T09:00:00Z", 2L * 3_600_000L, 12),
            beta to stats(beta, "2026-11-12T09:00:00Z", 600_000L, 1),
        )
        val sessions = buildList {
            for (day in 0 until 8) {
                val d = LocalDate.parse("2026-11-13").minusDays(day.toLong())
                add(session(alpha, "${d}T10:00:00Z", "${d}T12:10:00Z"))
            }
            add(session(beta, "2026-11-11T23:00:00Z", "2026-11-11T23:30:00Z"))
        }
        val career = CareerLedger.build(stats, sessions, setOf(alpha, beta), now, zone)

        val byBadge = career.milestones.associateBy { it.badge }
        assertEquals(1, byBadge.getValue(CareerBadge.FirstSession).progress)
        assertEquals(10, byBadge.getValue(CareerBadge.TenSessions).progress)
        assertEquals(120, byBadge.getValue(CareerBadge.Marathon).progress)
        assertEquals(7, byBadge.getValue(CareerBadge.WeekStreak).progress)
        assertEquals(2, byBadge.getValue(CareerBadge.FiveGames).progress)
        assertTrue(byBadge.getValue(CareerBadge.FirstSession).unlocked)
        assertTrue(byBadge.getValue(CareerBadge.TenSessions).unlocked)
        assertTrue(byBadge.getValue(CareerBadge.Marathon).unlocked)
        assertTrue(byBadge.getValue(CareerBadge.WeekStreak).unlocked)
        assertTrue(!byBadge.getValue(CareerBadge.FiveGames).unlocked)
    }

    @Test
    fun `streak stays alive from yesterday and dies after a gap`() {
        val sessions = listOf(
            session(alpha, "2026-11-12T10:00:00Z", "2026-11-12T11:00:00Z"),
            session(alpha, "2026-11-11T10:00:00Z", "2026-11-11T11:00:00Z"),
        )
        assertEquals(2, CareerLedger.streak(sessions, setOf(alpha), now, zone))
        assertEquals(
            0,
            CareerLedger.streak(
                listOf(session(alpha, "2026-11-10T10:00:00Z", "2026-11-10T11:00:00Z")),
                setOf(alpha),
                now,
                zone
            )
        )
    }
}
