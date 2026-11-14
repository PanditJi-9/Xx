/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** يد ← لمس: القرب يُقرأ كما يعلنه الحسّاس، واللمسة واحدة عند العبور بفاصل يمنع الرعشة. */
class HandTouchMathTest {

    @Test
    fun `القيمة القريبة تُقرأ قريبة`() {
        assertTrue(HandTouchMath.isNear(0f, 10f))
        assertTrue(HandTouchMath.isNear(2.9f, 10f))
        assertTrue(HandTouchMath.isNear(3f, 10f))
        assertFalse(HandTouchMath.isNear(3.1f, 10f))
        assertFalse(HandTouchMath.isNear(9f, 10f))
    }

    @Test
    fun `القراءة غير الصالحة ليست قريبة`() {
        assertFalse(HandTouchMath.isNear(-1f, 10f))
        assertFalse(HandTouchMath.isNear(Float.NaN, 10f))
        assertFalse(HandTouchMath.isNear(Float.POSITIVE_INFINITY, 10f))
    }

    @Test
    fun `حسّاس بلا مدى يستعمل الحدّ الافتراضي`() {
        assertTrue(HandTouchMath.isNear(2f, 0f))
        assertFalse(HandTouchMath.isNear(4f, 0f))
        // المدى الأقصر هو الحاكم: قيمة ٢ لا تُقرأ قريبة على حسّاس مداه ١.
        assertFalse(HandTouchMath.isNear(2f, 1f))
    }

    @Test
    fun `اللمسة تقع عند العبور من بعيد إلى قريب`() {
        assertTrue(HandTouchMath.shouldTap(previousNear = false, nowNear = true, sinceLastTapMs = Long.MAX_VALUE))
        assertFalse(HandTouchMath.shouldTap(previousNear = true, nowNear = true, sinceLastTapMs = Long.MAX_VALUE))
        assertFalse(HandTouchMath.shouldTap(previousNear = true, nowNear = false, sinceLastTapMs = Long.MAX_VALUE))
        assertFalse(HandTouchMath.shouldTap(previousNear = false, nowNear = false, sinceLastTapMs = Long.MAX_VALUE))
    }

    @Test
    fun `التهدئة تمنع رشقة اللمسات`() {
        assertFalse(HandTouchMath.shouldTap(false, true, 0L))
        assertFalse(HandTouchMath.shouldTap(false, true, HandTouchMath.COOLDOWN_MS - 1))
        assertTrue(HandTouchMath.shouldTap(false, true, HandTouchMath.COOLDOWN_MS))
    }
}
