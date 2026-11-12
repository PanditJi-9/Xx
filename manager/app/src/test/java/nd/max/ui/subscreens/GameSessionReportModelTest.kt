/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import nd.max.core.platform.SessionPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **أرقام التقرير تُقاس هنا لا على الشاشة.**
 *
 * التقرير الجديد (طلب المالك: تقرير كتقرير Scene) يعرض أدنى ١٪ والقيمة بالملّي واط
 * ومدى المحور. وكلها حسابات صافية، فتُختبر على JVM بلا جهاز: الخطأ فيها لا يُرى بالعين —
 * «أدنى ١٪» يُقرأ رقمًا معقولًا حتى لو اختيرت العيّنة الخطأ.
 *
 * وحدّ مُعلَن: العيّنات في السجلّ مُختزلة إلى ١٨٠ نقطة، فالنسبة هنا **مئين من العيّنات
 * المحفوظة** لا من كل إطار رُسم — وهذا مكتوب في KDoc النموذج، ولا يُقدَّم كرقم Scene.
 */
class GameSessionReportModelTest {

    private fun point(atMs: Long, frames: Float?) = SessionPoint(
        atMs = atMs,
        frames = frames,
        cpu = null,
        ramMb = null,
        watt = null,
        battery = null,
        heat = null,
        jankPercent = null,
        frameP99Ms = null,
        cpuFreqMhz = null,
        socTempC = null,
        gpuLoadPercent = null,
        batteryVoltageMv = null,
        foreground = "game.pkg",
    )

    @Test
    fun `low percentile picks the sample at the tail, not the average`() {
        val frames = (1..100).map { it.toFloat() }
        assertEquals(1f, lowPercentileFps(frames, 1.0)!!, 0.001f)
        assertEquals(5f, lowPercentileFps(frames, 5.0)!!, 0.001f)
        assertEquals(100f, lowPercentileFps(frames, 100.0)!!, 0.001f)
        assertNull(lowPercentileFps(emptyList(), 1.0))
    }

    @Test
    fun `low percentile tolerates a short or unsorted series`() {
        assertEquals(40f, lowPercentileFps(listOf(90f, 40f, 80f), 1.0)!!, 0.001f)
        assertEquals(40f, lowPercentileFps(listOf(40f), 1.0)!!, 0.001f)
    }

    @Test
    fun `watts are shown in milliwatts and unknown stays unknown`() {
        assertEquals(7354, wattToMw(7.354f))
        assertEquals(900, wattToMw(0.9f))
        assertNull(wattToMw(null))
        assertNull(wattToMw(0f))
        assertNull(wattToMw(-1f))
    }

    @Test
    fun `the axis contains the session and the refresh line`() {
        val points = listOf(point(0L, 92f), point(1_000L, 80f), point(2_000L, 90f))
        val range = fpsAxisRange(points, minFps = 80f, maxFps = 92f, refreshHz = 90)
        assertTrue("أدنى الجلسة خارج المحور", range.start <= 80f)
        assertTrue("أعلى الجلسة خارج المحور", range.endInclusive >= 92f)
        assertTrue("دقّة العرض خارج المحور", 90f in range)
        assertTrue("محور سالب", range.start >= 0f)
    }

    @Test
    fun `battery delta follows the series, not a rounded tally`() {
        val series = listOf(
            point(0L, 60f).copy(heat = 38f),
            point(1_000L, 60f).copy(heat = 40f),
        )
        val stats = liveReportStats(tally = null, series = series, elapsedMs = 60_000L)
        assertEquals(38f, stats.batteryTempStartC!!, 0.001f)
        assertEquals(40f, stats.batteryTempEndC!!, 0.001f)
        assertEquals(2f, stats.batteryTempDeltaC!!, 0.001f)
    }
}
