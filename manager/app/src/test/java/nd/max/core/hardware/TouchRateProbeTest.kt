/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد قياس معدّل اللمس (معيار القبول ٤: **الفصل بين المعدل الأصلي ومعدل أحداث Android**).
 *
 * والقياس هنا هو الحساب وحده — لأن ما قبله (وصول الأحداث) وما بعده (معدّل اللوحة) يحتاج
 * جهازًا. وهذا هو الحدّ المُعلَن، لا ادّعاء أن القياس مُتحقَّق على عتاد.
 */
class TouchRateProbeTest {

    @Test
    fun `the rate is events over elapsed time`() {
        assertEquals(120.0, TouchRateMath.eventsPerSecond(120, 1_000)!!, 0.001)
        assertEquals(60.0, TouchRateMath.eventsPerSecond(30, 500)!!, 0.001)
        assertEquals(90.0, TouchRateMath.eventsPerSecond(45, 500)!!, 0.001)
    }

    @Test
    fun `a sample too short to mean anything is unknown, not zero and not a rate`() {
        assertNull(
            "حدثان على ٥ مللي ليست «٤٠٠ حدث/ث» بل صدفة توقيت",
            TouchRateMath.eventsPerSecond(2, 5),
        )
        assertNull(
            "المدّة الدنيا نفسها حدّ أدنى شامل، وما دونها مجهول",
            TouchRateMath.eventsPerSecond(1, TouchRateMath.MIN_DURATION_MS - 1),
        )
        assertEquals(
            "وعند الحدّ الأدنى يُقبل الحساب",
            2.5,
            TouchRateMath.eventsPerSecond(1, TouchRateMath.MIN_DURATION_MS)!!,
            0.001,
        )
    }

    @Test
    fun `no events and impossible durations are unknown`() {
        assertNull(TouchRateMath.eventsPerSecond(0, 1_000))
        assertNull(TouchRateMath.eventsPerSecond(-3, 1_000))
        assertNull(TouchRateMath.eventsPerSecond(50, 0))
        assertNull(TouchRateMath.eventsPerSecond(50, -1_000))
    }

    @Test
    fun `the sample carries the same judgement as the math`() {
        assertEquals(120.0, TouchRateSample(events = 120, durationMs = 1_000).eventsPerSecond!!, 0.001)
        assertNull(TouchRateSample(events = 1, durationMs = 10).eventsPerSecond)
        assertTrue(
            "العيّنة القصيرة تُعلن نفسها مجهولة — لا رقمًا يُعرض بجانب المعدل الأصلي",
            TouchRateMath.MIN_DURATION_MS >= 400L,
        )
    }
}
