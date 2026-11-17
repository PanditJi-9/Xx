/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import nd.max.core.platform.SessionPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

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

    // ── الثبات وتوزيع زمن الإطارات ──────────────────────────────────────────────

    @Test
    fun `typical fps is the median, so one dip or spike does not move it`() {
        assertEquals(60f, typicalFps(listOf(60f, 60f, 60f, 10f, 120f))!!, 0.001f)
        assertEquals(55f, typicalFps(listOf(50f, 60f))!!, 0.001f)
        assertNull(typicalFps(emptyList()))
        assertNull(typicalFps(listOf(0f, -1f, Float.NaN)))
    }

    @Test
    fun `bands are measured against the game's own rate, not the screen refresh`() {
        // لعبة مقفولة على ٦٠ فوق شاشة ٩٠: ثابتة تمامًا، لا «متعثّرة».
        val locked = List(20) { 60f }
        val lockedBands = fpsBands(locked)!!
        assertEquals(1f, lockedBands.steady, 0.001f)
        assertEquals(0f, lockedBands.rough, 0.001f)
    }

    @Test
    fun `bands split at ninety and seventy percent of the typical rate`() {
        // وسيطها ١٠٠: ثابت ≥٩٠، متذبذب ≥٧٠، متقطّع دون ذلك.
        val frames = listOf(100f, 100f, 100f, 100f, 100f, 95f, 90f, 80f, 70f, 40f)
        val bands = fpsBands(frames)!!
        assertEquals(0.7f, bands.steady, 0.001f)
        assertEquals(0.2f, bands.uneven, 0.001f)
        assertEquals(0.1f, bands.rough, 0.001f)
        assertEquals(1f, bands.steady + bands.uneven + bands.rough, 0.001f)
    }

    @Test
    fun `bands need at least two usable samples`() {
        assertNull(fpsBands(emptyList()))
        assertNull(fpsBands(listOf(60f)))
        assertNull(fpsBands(listOf(0f, 0f, 60f)))
    }

    @Test
    fun `share at or above counts the threshold itself`() {
        val frames = listOf(30f, 45f, 60f, 60f)
        assertEquals(0.75f, shareAtOrAbove(frames, 45f)!!, 0.001f)
        assertNull(shareAtOrAbove(emptyList(), 45f))
    }

    @Test
    fun `spread is zero for a flat run and grows with the swings`() {
        assertEquals(0f, fpsSpread(listOf(60f, 60f, 60f))!!, 0.001f)
        assertEquals(10f, fpsSpread(listOf(50f, 70f))!!, 0.001f)
        assertNull(fpsSpread(listOf(60f)))
    }

    // ── أدنى اللحظات ─────────────────────────────────────────────────────────

    @Test
    fun `worst moments are separated, one long dip is not three moments`() {
        // هبوط واحد طويل في المنتصف + هبوط منفصل قرب النهاية.
        val values = MutableList(40) { 60f }
        values[10] = 20f
        values[11] = 22f
        values[12] = 21f
        values[35] = 30f
        val points = values.mapIndexed { i, v -> point(i * 1_000L, v) }
        val worst = worstMoments(points, count = 3, below = 54f)
        assertEquals(2, worst.size)
        assertEquals(10, worst[0].index)
        assertEquals(35, worst[1].index)
        assertEquals(20f, worst[0].fps, 0.001f)
    }

    @Test
    fun `a flat session has no worst moments`() {
        val points = List(30) { point(it * 1_000L, 60f) }
        assertTrue(worstMoments(points, count = 3, below = 54f).isEmpty())
        assertTrue(worstMoments(emptyList(), count = 3).isEmpty())
        assertTrue(worstMoments(points, count = 0).isEmpty())
    }

    // ── التراكب ───────────────────────────────────────────────────────────────

    @Test
    fun `overlay needs two real readings and zero load is a reading`() {
        val temps = listOf(
            point(0L, 60f).copy(socTempC = 60f),
            point(1_000L, 60f).copy(socTempC = 70f),
        )
        assertTrue(overlayAvailable(temps, ReportOverlay.SocTemp))
        assertFalse(overlayAvailable(temps, ReportOverlay.GpuLoad))

        val idle = listOf(
            point(0L, 60f).copy(gpuLoadPercent = 0),
            point(1_000L, 60f).copy(gpuLoadPercent = 10),
        )
        assertTrue("حِمل ٠٪ قراءة لا غياب", overlayAvailable(idle, ReportOverlay.GpuLoad))
        // الحرارة الصفرية غياب لا قراءة.
        val deadSensor = listOf(
            point(0L, 60f).copy(socTempC = 0f),
            point(1_000L, 60f).copy(socTempC = 0f),
        )
        assertFalse(overlayAvailable(deadSensor, ReportOverlay.SocTemp))
    }

    @Test
    fun `overlay range is fixed for loads and padded for the rest`() {
        val loads = listOf(
            point(0L, 60f).copy(cpu = 20),
            point(1_000L, 60f).copy(cpu = 40),
        )
        assertEquals(0f..100f, overlayRange(loads, ReportOverlay.CpuLoad))

        val temps = listOf(
            point(0L, 60f).copy(socTempC = 50f),
            point(1_000L, 60f).copy(socTempC = 50f),
        )
        val range = overlayRange(temps, ReportOverlay.SocTemp)!!
        assertTrue("نطاق منهار عند التساوي", range.endInclusive > range.start)
        assertTrue(50f in range)
    }

    @Test
    fun `overlay values print with their own digits and unit`() {
        // التنسيق يتبع لغة الجهاز؛ تُثبَّت هنا كي لا يتغيّر الناتج بلغة آلة البناء.
        val saved = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            assertEquals("71 °C", ReportOverlay.SocTemp.format(71.2f))
            assertEquals("52%", ReportOverlay.GpuLoad.format(52f))
            assertEquals("7.4 W", ReportOverlay.Power.format(7.354f))
        } finally {
            Locale.setDefault(saved)
        }
        assertEquals(
            74f,
            overlayAverage(
                listOf(point(0L, 60f).copy(socTempC = 70f), point(1_000L, 60f).copy(socTempC = 78f)),
                ReportOverlay.SocTemp
            )!!,
            0.001f
        )
    }
}
