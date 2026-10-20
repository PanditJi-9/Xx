/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** شريط التردد: المستوى من التردد الحيّ فعلًا، وما لا يُقرأ يبقى `null` لا صفرًا. */
class PanelClockMathTest {

    @Test
    fun `level runs between the announced floor and ceiling`() {
        assertEquals(0f, PanelClockMath.level(300, 300, 3000)!!, 0.0001f)
        assertEquals(1f, PanelClockMath.level(3000, 300, 3000)!!, 0.0001f)
        assertEquals(0.5f, PanelClockMath.level(1650, 300, 3000)!!, 0.0001f)
    }

    @Test
    fun `without a floor the level is measured from zero`() {
        assertEquals(0.5f, PanelClockMath.level(1500, null, 3000)!!, 0.0001f)
        // أرضية غير منطقية (≥ السقف) تُهمَل ولا تقسم على صفر.
        assertEquals(0.5f, PanelClockMath.level(1500, 3000, 3000)!!, 0.0001f)
        assertEquals(0.5f, PanelClockMath.level(1500, 4000, 3000)!!, 0.0001f)
    }

    @Test
    fun `level is clamped and a missing input is null never zero`() {
        assertEquals(1f, PanelClockMath.level(5000, 300, 3000)!!, 0.0001f)
        assertEquals(0f, PanelClockMath.level(100, 300, 3000)!!, 0.0001f)
        assertNull(PanelClockMath.level(null, 300, 3000))
        assertNull(PanelClockMath.level(1500, 300, null))
        assertNull(PanelClockMath.level(0, 300, 3000))
        assertNull(PanelClockMath.level(1500, 300, 0))
    }

    @Test
    fun `lit segments follow the level and an unread level lights nothing`() {
        assertEquals(0, PanelClockMath.litSegments(null))
        assertEquals(0, PanelClockMath.litSegments(0f))
        assertEquals(10, PanelClockMath.litSegments(0.5f))
        assertEquals(PanelClockMath.SEGMENTS, PanelClockMath.litSegments(1f))
        assertEquals(PanelClockMath.SEGMENTS, PanelClockMath.litSegments(7f))
        assertEquals(0, PanelClockMath.litSegments(-1f))
    }

    @Test
    fun `texts come from the live reading only`() {
        assertEquals("3.51", PanelClockMath.ghz(3510))
        assertEquals("342", PanelClockMath.mhz(342))
        assertEquals("3.51 GHz", PanelClockMath.cpuTag(3510))
        assertEquals("342 MHz", PanelClockMath.gpuTag(342))
        assertEquals("MAX 3.25", PanelClockMath.cpuMax(3250))
        // سقف الرسّام لا دالّة له هنا عمدًا، فلا يُختبر في هذا الملف.
        assertNull(PanelClockMath.ghz(null))
        assertNull(PanelClockMath.ghz(0))
        assertNull(PanelClockMath.mhz(-5))
        assertNull(PanelClockMath.cpuTag(null))
        assertNull(PanelClockMath.gpuTag(null))
    }

    @Test
    fun `the clocks model exposes the level of each processor`() {
        // الرسّام: السقف الديناميكي الحيّ (٦٨٤) على المسطرة الفيزيائية (٠ ← ١٣٦٨ بلا أرضية) ⟹ ٠٫٥.
        val clocks = PanelClocks(
            cpuMhz = 1650, cpuCeilingMhz = 3000, cpuFloorMhz = 300,
            gpuMhz = 342, gpuCeilingMhz = 684, gpuCapabilityMaxMhz = 1368
        )
        assertEquals(0.5f, clocks.cpuLevel!!, 0.0001f)
        assertEquals(0.5f, clocks.gpuLevel!!, 0.0001f)
        // رسّام بلا سقف فيزيائي: شريط فارغ، لا مستوى مختلق (السقف الديناميكي وحده لا مسطرة له).
        assertNull(PanelClocks(gpuMhz = 342).gpuLevel)
        assertNull(PanelClocks(gpuMhz = 342, gpuCeilingMhz = 684).gpuLevel)
        assertNotNull(PanelClocks(cpuMhz = 1000, cpuCeilingMhz = 2000).cpuLevel)
        assertTrue(PanelClocks().cpuLevel == null && PanelClocks().gpuLevel == null)
    }

    @Test
    fun `gpu bar lights by the dynamic max across the physical range`() {
        // أرضية فيزيائية ٢٦٠، سقف فيزيائي ١٣٠٠، سقف ديناميكي حيّ ٧٥٤ ⇒ ≈ ٤٨٪ ⇒ ٩ مقاطع.
        val clocks = PanelClocks(gpuMhz = 260, gpuCeilingMhz = 754, gpuFloorMhz = 260, gpuCapabilityMaxMhz = 1300)
        assertEquals(754, clocks.gpuShownMhz)
        assertEquals(0.475f, clocks.gpuLevel!!, 0.001f)
        assertEquals(9, PanelClockMath.litSegments(clocks.gpuLevel))
    }

    @Test
    fun `cpu shows and lights by its dynamic max, falling back to the live clock`() {
        val capped = PanelClocks(cpuMhz = 3250, cpuDynamicMaxMhz = 2000, cpuFloorMhz = 500, cpuCeilingMhz = 3250)
        assertEquals(2000, capped.cpuShownMhz)
        assertEquals(1500f / 2750f, capped.cpuLevel!!, 0.001f)
        val noDynamic = PanelClocks(cpuMhz = 1650, cpuFloorMhz = 300, cpuCeilingMhz = 3000)
        assertEquals(1650, noDynamic.cpuShownMhz)
        assertEquals(0.5f, noDynamic.cpuLevel!!, 0.0001f)
        assertNull(PanelClocks().cpuLevel)
        assertNull(PanelClocks().gpuLevel)
    }
}
