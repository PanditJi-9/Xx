/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * مقارنة الأسبوع بالذي قبله — تُختبر هنا لأن الخطأ فيها **يجعل الأسبوع يبدو تحسّنًا وهو تراجع**،
 * أو يخترع نسبةً من صفر. والقائمة تُقرأ من النهاية، فأي قلبٍ للترتيب يقلب المعنى بلا فشل ظاهر.
 */
class DailyUsageWeekOverWeekTest {

    private fun days(vararg millis: Long): List<DailyUsage.Day> =
        millis.mapIndexed { index, value -> DailyUsage.Day(20_000L + index, value) }

    @Test
    fun thirteen_days_are_not_enough_for_a_comparison() {
        val short = days(*LongArray(13) { 1_000L })

        assertNull(DailyUsage.weekOverWeek(short))
    }

    @Test
    fun exactly_fourteen_days_compare_the_last_seven_with_the_seven_before_them() {
        val fortnight = days(*LongArray(7) { 1_000L }, *LongArray(7) { 2_000L })

        val result = DailyUsage.weekOverWeek(fortnight)

        assertEquals(14_000L, result?.currentMillis)
        assertEquals(7_000L, result?.previousMillis)
        assertEquals(7_000L, result?.deltaMillis)
        assertEquals(100, result?.percentChange)
    }

    @Test
    fun equal_weeks_report_no_change_rather_than_a_false_improvement() {
        val flat = days(*LongArray(14) { 3_000L })

        val result = DailyUsage.weekOverWeek(flat)

        assertEquals(0L, result?.deltaMillis)
        assertEquals(0, result?.percentChange)
    }

    @Test
    fun a_quiet_week_is_reported_as_a_drop_not_as_an_absence() {
        val falling = days(*LongArray(7) { 2_000L }, *LongArray(7) { 1_000L })

        val result = DailyUsage.weekOverWeek(falling)

        assertEquals(-7_000L, result?.deltaMillis)
        assertEquals(-50, result?.percentChange)
    }

    @Test
    fun an_empty_previous_week_has_no_percentage_but_still_reports_both_totals() {
        val fresh = days(*LongArray(7) { 0L }, *LongArray(7) { 4_000L })

        val result = DailyUsage.weekOverWeek(fresh)

        assertEquals(28_000L, result?.currentMillis)
        assertEquals(0L, result?.previousMillis)
        assertNull(result?.percentChange)
        assertTrue((result?.deltaMillis ?: 0L) > 0L)
    }

    @Test
    fun the_oldest_days_are_ignored_when_more_than_fourteen_are_supplied() {
        val twentyDays = days(
            *LongArray(6) { 9_000L },
            *LongArray(7) { 1_000L },
            *LongArray(7) { 2_000L }
        )

        val result = DailyUsage.weekOverWeek(twentyDays)

        assertEquals(14_000L, result?.currentMillis)
        assertEquals(7_000L, result?.previousMillis)
        assertEquals(100, result?.percentChange)
    }

    @Test
    fun tail_keeps_the_requested_window_from_the_end() {
        val all = days(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

        assertEquals(listOf(8L, 9L, 10L), DailyUsage.tail(all, 3).map { it.millis })
    }

    @Test
    fun tail_returns_the_whole_list_when_it_is_shorter_than_the_window() {
        val all = days(1, 2, 3)

        assertEquals(3, DailyUsage.tail(all, 30).size)
    }

    @Test
    fun a_non_positive_window_is_empty_rather_than_a_crash() {
        assertTrue(DailyUsage.tail(days(1, 2), 0).isEmpty())
        assertTrue(DailyUsage.tail(days(1, 2), -5).isEmpty())
    }
}
