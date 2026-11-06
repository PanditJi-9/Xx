/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyUsageEventsTest {
    private fun resume(at: Long, pkg: String = "app", activity: String = "Main") =
        DailyUsage.Event(at, DailyUsage.Kind.RESUME, pkg, activity)

    private fun pause(at: Long, pkg: String = "app", activity: String = "Main") =
        DailyUsage.Event(at, DailyUsage.Kind.PAUSE, pkg, activity)

    private fun close(at: Long) = DailyUsage.Event(at, DailyUsage.Kind.CLOSE_ALL)

    @Test
    fun a_closed_session_ends_at_pause_not_at_query_time() {
        assertEquals(
            listOf(DailyUsage.Span(10, 30)),
            DailyUsage.foregroundSpans(listOf(resume(10), pause(30)), 100)
        )
    }

    @Test
    fun an_open_session_ends_at_query_time() {
        assertEquals(listOf(DailyUsage.Span(10, 100)), DailyUsage.foregroundSpans(listOf(resume(10)), 100))
    }

    @Test
    fun a_duplicate_resume_does_not_lose_the_original_start() {
        assertEquals(
            listOf(DailyUsage.Span(10, 50)),
            DailyUsage.foregroundSpans(listOf(resume(10), resume(20), pause(50)), 100)
        )
    }

    @Test
    fun a_stopped_event_after_pause_does_not_create_another_span() {
        assertEquals(
            listOf(DailyUsage.Span(10, 50)),
            DailyUsage.foregroundSpans(listOf(resume(10), pause(50), pause(60)), 100)
        )
    }

    @Test
    fun pausing_the_old_activity_keeps_the_new_activity_open() {
        val events = listOf(resume(10, activity = "A"), resume(20, activity = "B"),
            pause(30, activity = "A"), pause(50, activity = "B"))
        assertEquals(listOf(DailyUsage.Span(10, 50)), DailyUsage.foregroundSpans(events, 100))
    }

    @Test
    fun split_screen_counts_the_union_not_both_apps_twice() {
        val events = listOf(resume(10, "a"), resume(20, "b"), pause(40, "a"), pause(50, "b"))
        val spans = DailyUsage.foregroundSpans(events, 100)
        assertEquals(40L, spans.sumOf { it.endMs - it.startMs })
    }

    @Test
    fun pausing_another_package_with_the_same_activity_name_does_not_close_the_session() {
        assertEquals(
            listOf(DailyUsage.Span(10, 100)),
            DailyUsage.foregroundSpans(listOf(resume(10, "a"), pause(30, "b")), 100)
        )
    }

    @Test
    fun an_unmatched_pause_does_not_invent_a_start_at_midnight() {
        assertTrue(DailyUsage.foregroundSpans(listOf(pause(30)), 100).isEmpty())
    }

    @Test
    fun screen_off_closes_all_activities_without_a_pause_event() {
        val events = listOf(resume(10, "a"), resume(20, "b"), close(40))
        assertEquals(listOf(DailyUsage.Span(10, 40)), DailyUsage.foregroundSpans(events, 100))
    }

    @Test
    fun reboot_downtime_is_never_counted_as_foreground_time() {
        val events = listOf(resume(10), close(20), close(80), resume(90), pause(100))
        assertEquals(
            listOf(DailyUsage.Span(10, 20), DailyUsage.Span(90, 100)),
            DailyUsage.foregroundSpans(events, 120)
        )
    }

    @Test
    fun resuming_after_unlock_needs_a_new_activity_event() {
        val events = listOf(resume(10), close(20), pause(30), resume(50), pause(60))
        assertEquals(
            listOf(DailyUsage.Span(10, 20), DailyUsage.Span(50, 60)),
            DailyUsage.foregroundSpans(events, 100)
        )
    }

    @Test
    fun events_after_query_time_are_ignored() {
        assertEquals(
            listOf(DailyUsage.Span(10, 50)),
            DailyUsage.foregroundSpans(listOf(resume(10), pause(80), resume(90)), 50)
        )
    }

    @Test
    fun unordered_input_is_reduced_in_timestamp_order() {
        assertEquals(
            listOf(DailyUsage.Span(10, 30)),
            DailyUsage.foregroundSpans(listOf(pause(30), resume(10)), 100)
        )
    }

    @Test
    fun blank_package_resume_does_not_create_a_session() {
        assertTrue(DailyUsage.foregroundSpans(listOf(resume(10, " ")), 100).isEmpty())
    }

    @Test
    fun zero_length_and_empty_sessions_are_not_returned() {
        assertTrue(DailyUsage.foregroundSpans(listOf(resume(10), pause(10)), 100).isEmpty())
        assertTrue(DailyUsage.foregroundSpans(emptyList(), 100).isEmpty())
    }
}
