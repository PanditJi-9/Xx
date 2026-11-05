/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 */
package nd.max.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حالة الحرارة من المنصّة — التصنيف نقيّ، فيُقاس كل رمز بلا جهاز.
 *
 * **وأخطر خطأ:** تحويل رمز مجهول إلى `NONE` ⇒ الشاشة تقول «لا خنق» بينما المنصّة تخنق.
 */
class ThermalStatusTest {

    @Test
    fun belowAndroid10IsUnknownAndTheReadNeverRuns() {
        var calls = 0

        val sample = ThermalReadout.classify(28) { calls++; 3 }

        assertEquals(ThermalStatus.UNKNOWN, sample.status)
        assertEquals("لا قراءة قبل API 29", 0, calls)
        assertFalse(sample.supported)
    }

    @Test
    fun everyDocumentedCodeMapsToItsName() {
        val expected = listOf(
            0 to ThermalStatus.NONE,
            1 to ThermalStatus.LIGHT,
            2 to ThermalStatus.MODERATE,
            3 to ThermalStatus.SEVERE,
            4 to ThermalStatus.CRITICAL,
            5 to ThermalStatus.EMERGENCY,
            6 to ThermalStatus.SHUTDOWN,
        )

        expected.forEach { (code, name) ->
            val sample = ThermalReadout.classify(34) { code }
            assertEquals(name, sample.status)
            assertEquals(code, sample.raw)
            assertTrue(sample.supported)
        }
    }

    @Test
    fun anUndocumentedCodeIsUnknownNotNone() {
        val sample = ThermalReadout.classify(34) { 9 }

        assertEquals(ThermalStatus.UNKNOWN, sample.status)
        assertFalse("رمز مجهول ليس «لا خنق»", sample.supported)
    }

    @Test
    fun aThrowingReadIsUnknownAndDoesNotCrash() {
        val sample = ThermalReadout.classify(33) { error("vendor") }

        assertEquals(ThermalStatus.UNKNOWN, sample.status)
    }

    @Test
    fun aNullReadingIsUnknown() {
        assertEquals(ThermalStatus.UNKNOWN, ThermalReadout.classify(30) { null }.status)
    }

    @Test
    fun throttlingStartsAtModerateAndIncludesShutdown() {
        assertFalse(ThermalStatus.NONE.isThrottling)
        assertFalse(ThermalStatus.LIGHT.isThrottling)
        assertTrue(ThermalStatus.MODERATE.isThrottling)
        assertTrue(ThermalStatus.SEVERE.isThrottling)
        assertTrue(ThermalStatus.SHUTDOWN.isThrottling)
        assertFalse("مجهول ≠ خنق مؤكّد", ThermalStatus.UNKNOWN.isThrottling)
    }
}
