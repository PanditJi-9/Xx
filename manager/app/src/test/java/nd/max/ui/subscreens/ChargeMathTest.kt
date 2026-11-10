/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * عقد تحويل عدّاد الشحنة إلى تيار: الرقم والاتجاه والحدود.
 *
 * الخطأ الذي يمنعه هذا الاختبار هو الأسهل في هذا النوع من الحساب: نسيان معامل
 * `3.6` (فيُقرأ ٣٦٠ mA على أنها ١٠٠)، أو قلب الاتجاه فيُقال «تشحن» والبطارية
 * تفقد. والثاني هو العطب الذي يجعل اختبار الشحن يكذب على صاحبه.
 */
class ChargeMathTest {

    @Test
    fun `one microamp-hour per second is three point six milliamps`() {
        // 1000 µAh في 10 ثوانٍ = 100 µAh/s = 360 mA
        assertEquals(360.0, chargeMilliamps(1_000, 10)!!, 1e-9)
    }

    @Test
    fun `a falling counter is a negative current, so the direction is not guessed`() {
        assertEquals(-360.0, chargeMilliamps(-1_000, 10)!!, 1e-9)
    }

    @Test
    fun `a still counter is zero, not missing`() {
        // الصفر قراءةٌ صحيحة (لا تدفّق مقيس) وليست غياب عدّاد — والفرق يظهر في الواجهة.
        assertEquals(0.0, chargeMilliamps(0, 6)!!, 1e-9)
    }

    @Test
    fun `a six second window of one milliamp-hour is six hundred milliamps`() {
        // 1000 µAh في 6 ثوانٍ = 166.7 µAh/s = 600 mA
        assertEquals(600.0, chargeMilliamps(1_000, 6)!!, 1e-9)
    }

    @Test
    fun `no window means no answer`() {
        assertNull(chargeMilliamps(1_000, 0))
        assertNull(chargeMilliamps(1_000, -5))
    }
}
