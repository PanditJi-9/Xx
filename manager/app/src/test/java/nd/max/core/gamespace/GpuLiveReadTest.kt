/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import nd.max.core.hardware.GpuHardwareBackend
import nd.max.core.hardware.GpuHardwareBackend.Family
import nd.max.core.hardware.GpuHardwareBackend.FrequencyUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رقم الرسّام في اللوحة يُشتقّ من ثلاث عقد حيّة فقط، بنفس دلالة الاكتشاف الكامل،
 * وفشل قراءة واحدة لا يُفرغ العدّاد.
 */
class GpuLiveReadTest {

    private val path = "/sys/class/devfreq/gpu0"

    private fun generic(unit: FrequencyUnit, family: Family = Family.QUALCOMM) = GpuHardwareBackend.Device(
        path = path, name = "gpu0", governor = "msm-adreno-tz", governors = emptyList(),
        minFreq = 260_000L, maxFreq = 1_300_000L, currentFreq = 754_000L,
        frequencies = listOf(260_000L, 754_000L, 1_300_000L),
        family = family, frequencyUnit = unit
    )

    private fun mtk(lockPath: String? = "/proc/gpufreq/gpufreq_opp_freq") = GpuHardwareBackend.Device(
        path = path, name = "mali", governor = "dummy", governors = emptyList(),
        minFreq = null, maxFreq = null, currentFreq = null,
        frequencies = listOf(260_000_000L, 754_000_000L, 1_300_000_000L),
        family = Family.MALI, frequencyUnit = FrequencyUnit.HZ,
        mtkFixedIndexPath = lockPath,
        mtkOppIndexByFrequency = mapOf(260_000_000L to "0", 754_000_000L to "1", 1_300_000_000L to "2")
    )

    @Test
    fun `generic device converts node language to mhz`() {
        val live = GpuLiveRead.derive(generic(FrequencyUnit.KHZ), GpuLiveRead.Raw(754_000L, 1_300_000L, null))
        assertEquals(754, live.curMhz)
        assertEquals(1300, live.dynamicMaxMhz)
    }

    @Test
    fun `generic device with an untrusted unit never invents a number`() {
        val live = GpuLiveRead.derive(generic(FrequencyUnit.AMBIGUOUS), GpuLiveRead.Raw(754_000L, 1_300_000L, null))
        assertNull(live.curMhz)
        assertNull(live.dynamicMaxMhz)
    }

    @Test
    fun `an unread node stays null`() {
        val live = GpuLiveRead.derive(generic(FrequencyUnit.KHZ), GpuLiveRead.Raw(null, null, null))
        assertNull(live.curMhz)
        assertNull(live.dynamicMaxMhz)
    }

    @Test
    fun `mtk without a lock reads the nodes against the advertised ladder`() {
        val live = GpuLiveRead.derive(mtk(), GpuLiveRead.Raw(754_000_000L, 754_000_000L, "-1"))
        assertEquals(754, live.curMhz)
        assertEquals(754, live.dynamicMaxMhz)
    }

    @Test
    fun `mtk node in khz is accepted only when it matches an advertised step`() {
        val ok = GpuLiveRead.derive(mtk(), GpuLiveRead.Raw(754_000L, 1_300_000L, null))
        assertEquals(754, ok.curMhz)
        assertEquals(1300, ok.dynamicMaxMhz)
        val bogus = GpuLiveRead.derive(mtk(), GpuLiveRead.Raw(123L, 123L, null))
        assertNull(bogus.curMhz)
        // الحدّ الأعلى يعود لآخر درجة معلنة حين لا تُقرأ العقدة قراءة صالحة.
        assertEquals(1300, bogus.dynamicMaxMhz)
    }

    @Test
    fun `mtk active lock wins over the nodes`() {
        val live = GpuLiveRead.derive(mtk(), GpuLiveRead.Raw(754_000_000L, 1_300_000_000L, "2"))
        assertEquals(1300, live.curMhz)
        assertEquals(1300, live.dynamicMaxMhz)
    }

    @Test
    fun `mtk released lock text is treated as no lock`() {
        val live = GpuLiveRead.derive(
            mtk(), GpuLiveRead.Raw(754_000_000L, 754_000_000L, "fix GPU/STACK OPP index is disabled")
        )
        assertEquals(754, live.curMhz)
        assertEquals(754, live.dynamicMaxMhz)
    }

    @Test
    fun `load is parsed and gpu idle is inverted`() {
        assertEquals(37, GpuLiveRead.loadPercent("$path/load", "37"))
        assertEquals(50, GpuLiveRead.loadPercent("$path/load", "1500 3000"))
        assertEquals(20, GpuLiveRead.loadPercent("/sys/module/ged/parameters/gpu_idle", "80"))
        assertNull(GpuLiveRead.loadPercent("$path/load", "n/a"))
        assertNull(GpuLiveRead.loadPercent("$path/load", null))
    }

    @Test
    fun `load candidates follow the family`() {
        val q = GpuLiveRead.loadCandidates(generic(FrequencyUnit.KHZ, Family.QUALCOMM))
        assertTrue(q.contains("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"))
        val m = GpuLiveRead.loadCandidates(mtk())
        assertTrue(m.contains("/sys/module/ged/parameters/gpu_idle"))
        val u = GpuLiveRead.loadCandidates(generic(FrequencyUnit.KHZ, Family.UNKNOWN))
        assertEquals(listOf("$path/load", "$path/device/load"), u)
    }

    // ── PanelClockHold ──────────────────────────────────────────────────────────

    private val good = PanelClocks(gpuMhz = 700, gpuCeilingMhz = 754, gpuLoadPct = 40, gpuFloorMhz = 260, gpuCapabilityMaxMhz = 1300)
    private val blank = PanelClocks(cpuMhz = 1400, cpuLoadPct = 12)

    @Test
    fun `a good gpu reading is taken as is and resets the miss count`() {
        val held = PanelClockHold.merge(PanelClocks(), good, misses = 3)
        assertEquals(good, held.clocks)
        assertEquals(0, held.misses)
    }

    @Test
    fun `a single failed read keeps the last good gpu numbers but takes fresh cpu`() {
        val held = PanelClockHold.merge(good, blank, misses = 0)
        assertEquals(754, held.clocks.gpuShownMhz)
        assertEquals(1300, held.clocks.gpuCapabilityMaxMhz)
        assertEquals(40, held.clocks.gpuLoadPct)
        assertEquals(1400, held.clocks.cpuMhz)
        assertEquals(1, held.misses)
    }

    @Test
    fun `after too many consecutive misses the blank is shown honestly`() {
        var state = good
        var misses = 0
        repeat(PanelClockHold.MAX_MISSES) {
            val held = PanelClockHold.merge(state, blank, misses)
            assertEquals(754, held.clocks.gpuShownMhz)
            state = held.clocks
            misses = held.misses
        }
        val released = PanelClockHold.merge(state, blank, misses)
        assertNull(released.clocks.gpuShownMhz)
    }

    @Test
    fun `nothing is held when there was never a gpu reading`() {
        val held = PanelClockHold.merge(PanelClocks(), blank, misses = 0)
        assertNull(held.clocks.gpuShownMhz)
    }
}
