/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * المحلّل يُقاس على الصيغة التي تُفسد الرقم بصمت: قسم غائب، عمود ناقص، وإطار بطيء واحد.
 *
 * والقاعدة: **ما لم يُقرأ لا يُخترع**. غياب `---PROFILEDATA---` أو أعمدة الطوابع يعيد `null`،
 * والإطار الذي لا يبدأ أو ينتهي بطابع صالح يُتخطّى ولا يُحسب صفرًا.
 */
class FrameStatsTest {

    private val header = "Flags,IntendedVsync,Vsync,OldestInputEvent,NewestInputEvent," +
        "HandleInputStart,AnimationStart,PerformTraversalsStart,DrawStart,SyncQueued," +
        "SyncStart,IssueDrawCommandsStart,SwapBuffers,FrameCompleted"

    private fun frame(startNs: Long, durationNs: Long): String {
        val columns = MutableList(14) { "0" }
        columns[1] = startNs.toString()
        columns[13] = (startNs + durationNs).toString()
        return columns.joinToString(",")
    }

    private fun dump(vararg frames: String): String = buildString {
        appendLine("Applications Graphics Acceleration Info:")
        appendLine("---PROFILEDATA---")
        appendLine(header)
        frames.forEach { appendLine(it) }
        appendLine("---PROFILEDATA---")
    }

    @Test
    fun a_16ms_and_a_20ms_frame_at_60hz_are_half_janky() {
        val stats = FrameStatsParser.parse(
            dump(
                frame(1_000_000_000L, 16_000_000L),
                frame(1_020_000_000L, 16_000_000L),
                frame(1_040_000_000L, 16_000_000L),
                frame(1_060_000_000L, 20_000_000L),
                frame(1_080_000_000L, 20_000_000L),
                frame(1_100_000_000L, 20_000_000L),
            ),
            refreshHz = 60f,
        )

        assertNotNull(stats)
        stats!!
        assertEquals(6, stats.frames)
        assertEquals(50, stats.jankPercent)
        assertEquals(3, stats.slowFrames)
        assertEquals(16f, stats.p50Ms, 0.001f)
        assertEquals(20f, stats.p90Ms, 0.001f)
        assertEquals(20f, stats.p99Ms, 0.001f)
    }

    @Test
    fun a_dump_without_the_profiledata_section_is_unknown_not_zero() {
        assertNull(FrameStatsParser.parse("Applications Graphics Acceleration Info:\nNo data\n", 60f))
        assertNull(FrameStatsParser.parse(null, 60f))
        assertNull(FrameStatsParser.parse("", 60f))
    }

    @Test
    fun a_header_without_the_timestamp_columns_is_rejected() {
        val raw = """
            ---PROFILEDATA---
            Flags,Vsync,FrameCompleted
            0,1,2
            ---PROFILEDATA---
        """.trimIndent()

        assertNull(FrameStatsParser.parse(raw, 60f))
    }

    @Test
    fun frames_with_an_invalid_span_are_skipped_and_all_invalid_is_null() {
        val skipped = FrameStatsParser.parse(
            dump(
                frame(0L, 16_000_000L),
                frame(1_000_000_000L, -16_000_000L),
                frame(1_020_000_000L, 0L),
            ),
            refreshHz = 60f,
        )
        assertNull(skipped)

        val oneValid = FrameStatsParser.parse(
            dump(
                frame(0L, 16_000_000L),
                frame(1_000_000_000L, 16_000_000L),
            ),
            refreshHz = 60f,
        )
        assertNotNull(oneValid)
        assertEquals(1, oneValid!!.frames)
        assertEquals(0, oneValid.jankPercent)
    }

    @Test
    fun a_missing_refresh_rate_falls_back_to_60hz() {
        val stats = FrameStatsParser.parse(
            dump(frame(1_000_000_000L, 20_000_000L)),
            refreshHz = 0f,
        )

        assertNotNull(stats)
        assertEquals(100, stats!!.jankPercent)
        assertEquals(1, stats.slowFrames)
    }
}
