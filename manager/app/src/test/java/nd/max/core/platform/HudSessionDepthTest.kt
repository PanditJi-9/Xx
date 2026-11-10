/*
 * Copyright (C) 2026-2027 Zexshia
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */

package nd.max.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات الطبقة العميقة: القصّ، والعتبات، وقارئات العتاد.
 *
 * وكلها دوالّ صافية بلا أندرويد — لأن ما لا يُختبر بلا جهاز يجب أن يُختبر هكذا،
 * وإلا بقي «يعمل على جهازي» هو الدليل الوحيد.
 */
class HudSessionDepthTest {

    private fun point(at: Long, pkg: String?, frames: Float = 60f) = SessionPoint(
        atMs = at,
        frames = frames,
        foreground = pkg
    )

    @Test
    fun `clip keeps the longest in-game run and drops the edges`() {
        val points = buildList {
            repeat(10) { add(point(it * 1000L, "com.loading")) }
            repeat(40) { add(point((10 + it) * 1000L, "com.game")) }
            repeat(10) { add(point((50 + it) * 1000L, "com.menu")) }
        }
        val result = clipTransitions(points)
        assertFalse(result.rejected)
        assertEquals(40, result.points.size)
        assertEquals(20, result.removed)
        assertTrue(result.points.all { it.foreground == "com.game" })
    }

    @Test
    fun `clip is refused when transitions dominate the session`() {
        val points = buildList {
            repeat(10) { add(point(it * 1000L, "com.game")) }
            repeat(10) { add(point((10 + it) * 1000L, "com.loading")) }
            repeat(10) { add(point((20 + it) * 1000L, "com.menu")) }
        }
        val result = clipTransitions(points)
        assertTrue(result.rejected)
        assertEquals(30, result.points.size)
        assertEquals(0, result.removed)
    }

    @Test
    fun `clip is refused with too few samples`() {
        val points = (0 until 5).map { point(it * 1000L, "com.game") }
        assertTrue(clipTransitions(points).rejected)
    }

    @Test
    fun `clip is refused when the foreground package is never reported`() {
        val points = (0 until 40).map { point(it * 1000L, null) }
        assertTrue(clipTransitions(points).rejected)
    }

    @Test
    fun `warnings wait for the configured duration`() {
        val tracker = HudWarningTracker()
        val rules = HudSessionRules(socTempThresholdC = 80f, warningSeconds = 10)
        val hot = HudReading(socTempC = 85f)

        assertTrue(tracker.update(hot, rules, nowMs = 1_000L).isEmpty())
        assertTrue(tracker.update(hot, rules, nowMs = 5_000L).isEmpty())
        val fired = tracker.update(hot, rules, nowMs = 11_000L)
        assertEquals(1, fired.size)
        assertEquals(HudField.SocTemp, fired.first().field)
        assertEquals(10_000L, fired.first().durationMs)
    }

    @Test
    fun `a warning window closes when the value falls back`() {
        val tracker = HudWarningTracker()
        val rules = HudSessionRules(socTempThresholdC = 80f, warningSeconds = 2)
        assertTrue(tracker.update(HudReading(socTempC = 85f), rules, 0L).isEmpty())
        assertTrue(tracker.update(HudReading(socTempC = 70f), rules, 5_000L).isEmpty())
        // عاد فوق العتبة: النافذة تبدأ من جديد لا تُستأنف.
        assertTrue(tracker.update(HudReading(socTempC = 85f), rules, 6_000L).isEmpty())
        assertEquals(1, tracker.update(HudReading(socTempC = 85f), rules, 9_000L).size)
    }

    @Test
    fun `a disabled threshold never fires`() {
        val tracker = HudWarningTracker()
        val rules = HudSessionRules(socTempThresholdC = 0f, warningSeconds = 0)
        assertTrue(tracker.update(HudReading(socTempC = 99f), rules, 0L).isEmpty())
    }

    @Test
    fun `disk stats sum base devices only and skip partitions and loop devices`() {
        val text = """
             259       0 mmcblk0 100 0 8000 0 50 0 4000 0 0 0 0
             259       1 mmcblk0p1 100 0 8000 0 50 0 4000 0 0 0 0
               7       0 loop0 10 0 80 0 0 0 0 0 0 0 0
             253       0 dm-0 5 0 400 0 5 0 800 0 0 0 0
        """.trimIndent()
        val (read, write) = DiskIoSampler.parseDiskSectors(text)
        assertEquals(8_400L, read)
        assertEquals(4_800L, write)
    }

    @Test
    fun `gpu busy node is a pair of counters, not a percentage`() {
        assertEquals(25, DiskIoSampler.parseGpuBusyPercent("25 100"))
        assertEquals(100, DiskIoSampler.parseGpuBusyPercent("100 100"))
        assertNull(DiskIoSampler.parseGpuBusyPercent("0 0"))
        assertNull(DiskIoSampler.parseGpuBusyPercent("garbage"))
    }

    @Test
    fun `gpu busy node may already be a percentage`() {
        assertEquals(73, DiskIoSampler.parseGpuBusyPercent("73"))
    }

    @Test
    fun `clock units are told apart, not guessed`() {
        assertEquals(587, hzToMhz(587_000_000L))
        assertEquals(1_800, hzToMhz(1_800_000L))
        assertEquals(240, khzToMhz(240_000L))
        assertNull(hzToMhz(0L))
    }

    @Test
    fun `tally carries the deep channels and leaves absent ones null`() {
        val points = listOf(
            SessionPoint(
                atMs = 0L,
                frames = 60f,
                socTempC = 71f,
                gpuLoadPercent = 40,
                cpuFreqMhz = 1_800,
                batteryVoltageMv = 4_200
            ),
            SessionPoint(
                atMs = 2_000L,
                frames = 50f,
                socTempC = 75f,
                gpuLoadPercent = 60,
                cpuFreqMhz = 2_000,
                batteryVoltageMv = 3_900
            )
        )
        val tally = tallyPoints(points, droppedSamples = 3, renderer = "Adreno")
        assertEquals(2, tally.samples)
        assertEquals(3, tally.droppedSamples)
        assertEquals(75f, tally.socTempPeak)
        assertEquals(50f, tally.gpuLoadAverage)
        assertEquals(1_900, tally.cpuFreqAverageMhz)
        assertEquals(3_900, tally.batteryVoltageMinMv)
        assertEquals("Adreno", tally.renderer)
    }

    @Test
    fun `tally of empty points reports nothing rather than zero`() {
        val tally = tallyPoints(emptyList())
        assertEquals(0, tally.samples)
        assertNull(tally.framesAverage)
        assertNull(tally.socTempPeak)
        assertNull(tally.energyWh)
        assertTrue(tally.isEmpty)
    }

    @Test
    fun `an unknown stop reason falls back to manual instead of inventing one`() {
        assertEquals(SessionStopReason.Manual, SessionStopReason.of("nonsense"))
        assertEquals(SessionStopReason.ScreenOff, SessionStopReason.of("screen"))
        assertEquals(SessionStopReason.Manual, SessionStopReason.of(null))
    }
}