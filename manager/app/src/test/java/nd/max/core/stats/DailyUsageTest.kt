/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * تقطيع الاستخدام على الأيام — يُختبر هنا لأن الخطأ فيه **صامت**: رسم بيانيّ يقبل
 * أي أرقام، فلا يكشف يومًا ضائعًا أو ساعةً محسوبةً مرّتين. الاختبارات تثبّت الحدود
 * (منتصف الليل) والقصّ (ما قبل النافذة) والمجموع (لا يُخلَق زمن).
 */
class DailyUsageTest {

    private val day = LocalDate.of(2026, 11, 6).toEpochDay()
    private val zone: ZoneId = ZoneOffset.UTC

    private fun at(text: String): Long = Instant.parse(text).toEpochMilli()

    @Test
    fun a_span_inside_one_day_stays_in_that_day() {
        val start = at("2026-11-06T10:00:00Z")

        val days = DailyUsage.bucket(
            listOf(DailyUsage.Span(start, start + 90 * 60_000L)),
            day, day, zone
        )

        assertEquals(listOf(DailyUsage.Day(day, 90 * 60_000L)), days)
    }

    @Test
    fun a_span_across_midnight_is_split_at_the_local_boundary() {
        val start = at("2026-11-06T23:00:00Z")
        val end = at("2026-11-07T01:30:00Z")

        val days = DailyUsage.bucket(listOf(DailyUsage.Span(start, end)), day, day + 1, zone)

        assertEquals(60 * 60_000L, days[0].millis)
        assertEquals(90 * 60_000L, days[1].millis)
    }

    @Test
    fun a_span_ending_exactly_at_midnight_does_not_open_the_next_day() {
        val start = at("2026-11-06T22:00:00Z")
        val end = at("2026-11-07T00:00:00Z")

        val days = DailyUsage.bucket(listOf(DailyUsage.Span(start, end)), day, day + 1, zone)

        assertEquals(2 * 60 * 60_000L, days[0].millis)
        assertEquals(0L, days[1].millis)
    }

    @Test
    fun what_happened_before_the_window_is_clipped_not_dropped() {
        // يبدأ قبل النافذة بساعة، وينتهي داخلها بساعة ⇒ يوم واحد، ساعة واحدة.
        val start = at("2026-11-05T23:00:00Z")
        val end = at("2026-11-06T01:00:00Z")

        val days = DailyUsage.bucket(listOf(DailyUsage.Span(start, end)), day, day, zone)

        assertEquals(listOf(DailyUsage.Day(day, 60 * 60_000L)), days)
    }

    @Test
    fun what_happened_after_the_window_is_ignored_entirely() {
        val start = at("2026-11-08T10:00:00Z")

        val days = DailyUsage.bucket(
            listOf(DailyUsage.Span(start, start + 60_000L)),
            day, day, zone
        )

        assertEquals(listOf(DailyUsage.Day(day, 0L)), days)
    }

    @Test
    fun empty_and_backwards_spans_are_ignored_rather_than_counted() {
        val start = at("2026-11-06T10:00:00Z")

        val days = DailyUsage.bucket(
            listOf(
                DailyUsage.Span(start, start),
                DailyUsage.Span(start, start - 5_000L)
            ),
            day, day, zone
        )

        assertEquals(0L, days.single().millis)
    }

    @Test
    fun a_reversed_window_is_empty_and_a_single_day_is_still_filled() {
        assertTrue(DailyUsage.bucket(emptyList(), day + 3, day, zone).isEmpty())

        val single = DailyUsage.bucket(emptyList(), day, day, zone)

        assertEquals(1, single.size)
        assertEquals(day, single.single().epochDay)
    }

    @Test
    fun the_days_come_back_contiguous_in_order() {
        val days = DailyUsage.bucket(emptyList(), day, day + 6, zone)

        assertEquals(7, days.size)
        assertEquals((0L..6L).toList(), days.map { it.epochDay - day })
    }

    @Test
    fun one_long_span_is_conserved_across_every_day_it_touches() {
        // فترة تبدأ قبل النافذة: يُقصّ ما قبلها ولا يُخلَق زمن ⇒ ٢٤ ساعة في أول يوم فقط.
        val start = at("2026-11-05T01:00:00Z")
        val end = at("2026-11-07T00:00:00Z")

        val days = DailyUsage.bucket(listOf(DailyUsage.Span(start, end)), day, day + 2, zone)

        assertEquals(end - at("2026-11-06T00:00:00Z"), days.sumOf { it.millis })
    }

    @Test
    fun the_split_follows_the_zone_not_greenwich() {
        // ٢٢:٣٠ بتوقيت غرينتش = ٠١:٣٠ من الغد في الرياض ⇒ يُحسب على يومه المحلّي.
        val riyadh = ZoneId.of("Asia/Riyadh")
        val start = at("2026-11-06T22:30:00Z")
        val end = at("2026-11-06T23:30:00Z")

        val days = DailyUsage.bucket(listOf(DailyUsage.Span(start, end)), day, day + 1, riyadh)

        assertEquals(0L, days[0].millis)
        assertEquals(60 * 60_000L, days[1].millis)
    }

    @Test
    fun a_real_multiday_span_is_conserved_without_clipping() {
        val start = at("2026-11-06T12:00:00Z")
        val end = at("2026-11-08T06:00:00Z")
        val days = DailyUsage.bucket(listOf(DailyUsage.Span(start, end)), day, day + 2, zone)
        assertEquals(listOf(12L, 24L, 6L), days.map { it.millis / 3_600_000L })
        assertEquals(end - start, days.sumOf { it.millis })
    }

    @Test
    fun the_spring_clock_change_has_a_twenty_three_hour_day() {
        val newYork = ZoneId.of("America/New_York")
        val springDay = LocalDate.of(2026, 3, 8).toEpochDay()
        val start = DailyUsage.startOfDay(springDay, newYork)
        val end = DailyUsage.startOfDay(springDay + 1, newYork)
        val days = DailyUsage.bucket(listOf(DailyUsage.Span(start, end)), springDay, springDay, newYork)
        assertEquals(23 * 3_600_000L, days.single().millis)
    }

    @Test
    fun the_autumn_clock_change_has_a_twenty_five_hour_day() {
        val newYork = ZoneId.of("America/New_York")
        val autumnDay = LocalDate.of(2026, 11, 1).toEpochDay()
        val start = DailyUsage.startOfDay(autumnDay, newYork)
        val end = DailyUsage.startOfDay(autumnDay + 1, newYork)
        val days = DailyUsage.bucket(listOf(DailyUsage.Span(start, end)), autumnDay, autumnDay, newYork)
        assertEquals(25 * 3_600_000L, days.single().millis)
    }

    @Test
    fun lookback_events_are_clipped_to_the_visible_days() {
        val events = listOf(
            DailyUsage.Event(at("2026-11-05T23:00:00Z"), DailyUsage.Kind.RESUME, "app", "Main"),
            DailyUsage.Event(at("2026-11-06T01:00:00Z"), DailyUsage.Kind.PAUSE, "app", "Main")
        )
        val spans = DailyUsage.foregroundSpans(events, at("2026-11-06T10:00:00Z"))
        assertEquals(3_600_000L, DailyUsage.bucket(spans, day, day, zone).single().millis)
    }
}
