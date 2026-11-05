/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * هامش الحرارة (ADPF) — التصنيف يقاس بالحقن، بلا أندرويد وبلا جهاز.
 *
 * **ولماذا كل هذه الحالات:** الإشارة موجودة على بعض الأجهزة، وترمي على بعضها، وتُرجع `NaN` على
 * ثالث. وأخطر خطأ ممكن هو تحويل الغياب إلى صفر ⇒ «الحرارة ممتازة» على جهازٍ لا يقيسها (ADR-07).
 */
class ThermalHeadroomTest {

    @Test
    fun belowApi30IsDeclaredAndTheReadIsNeverCalled() {
        var calls = 0

        val sample = ThermalHeadroom.classify(29) { calls++; 0.2f }

        assertEquals(HeadroomStatus.NO_API, sample.status)
        assertEquals("قراءة غير متاحة على API 29", 0, calls)
        assertNull(sample.percent)
        assertFalse(sample.supported)
    }

    @Test
    fun api30IsReadAndTheValueIsKept() {
        val sample = ThermalHeadroom.classify(30) { 0.42f }

        assertEquals(HeadroomStatus.LIVE, sample.status)
        assertEquals(0.42f, sample.percent)
        assertTrue(sample.supported)
        assertEquals(ThermalHeadroom.HORIZON_SECONDS, sample.horizonSeconds)
    }

    @Test
    fun nanIsUnsupportedNotZero() {
        val sample = ThermalHeadroom.classify(34) { Float.NaN }

        assertEquals(HeadroomStatus.UNSUPPORTED, sample.status)
        assertNull("NaN لا تصير ٠٪", sample.percent)
        assertFalse(sample.supported)
    }

    @Test
    fun aThrowingReadIsReportedSeparatelyFromUnsupported() {
        val sample = ThermalHeadroom.classify(33) { error("vendor ROM threw") }

        assertEquals(HeadroomStatus.THREW, sample.status)
        assertNull(sample.percent)
        assertFalse(sample.supported)
    }

    @Test
    fun anAbsentServiceIsItsOwnStatus() {
        val sample = ThermalHeadroom.classify(31) { null }

        assertEquals(HeadroomStatus.NO_SERVICE, sample.status)
        assertNull(sample.percent)
    }

    @Test
    fun extremeValuesAreClampedButStillLive() {
        val high = ThermalHeadroom.classify(32) { 9.5f }
        assertEquals(ThermalHeadroom.MAX_PERCENT, high.percent)
        assertEquals(HeadroomStatus.LIVE, high.status)

        val negative = ThermalHeadroom.classify(32) { -3f }
        assertEquals(0f, negative.percent)
    }

    @Test
    fun theHorizonIsTenSeconds() {
        assertEquals("XR-08: أفق ١٠ ثوانٍ هو ما تطلبه المنصّة", 10, ThermalHeadroom.HORIZON_SECONDS)
        assertEquals(30, ThermalHeadroom.MIN_API)
    }
}
