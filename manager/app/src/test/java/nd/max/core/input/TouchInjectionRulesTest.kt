/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * قواعد الحقن تُقاس هنا؛ أما وصول اللمسة إلى اللعبة فيحتاج جهازًا ولا يُدَّعى في JVM.
 */
class TouchInjectionRulesTest {

    @Test
    fun `نقطة داخل الشاشة تُقبل والخارج تُرفض`() {
        assertTrue(TouchInjectionRules.inBounds(100f, 100f, 1080, 2400))
        assertFalse(TouchInjectionRules.inBounds(-1f, 100f, 1080, 2400))
        assertFalse(TouchInjectionRules.inBounds(100f, 2401f, 1080, 2400))
        assertFalse(TouchInjectionRules.inBounds(Float.NaN, 100f, 1080, 2400))
        assertFalse(TouchInjectionRules.inBounds(100f, 100f, 0, 2400))
    }

    @Test
    fun `التقييد يحفظ النقطة داخل الحافة`() {
        assertEquals(0f, TouchInjectionRules.clampX(-50f, 1080), 0f)
        assertEquals(1080f, TouchInjectionRules.clampX(2000f, 1080), 0f)
        assertEquals(2400f, TouchInjectionRules.clampY(9999f, 2400), 0f)
    }

    @Test
    fun `مدة السحبة لا تصير صفرًا ولا تتجاوز الحد`() {
        assertEquals(TouchInjectionRules.MIN_SWIPE_MS, TouchInjectionRules.swipeDuration(0))
        assertEquals(TouchInjectionRules.MAX_SWIPE_MS, TouchInjectionRules.swipeDuration(9_000))
        assertEquals(240, TouchInjectionRules.swipeDuration(240))
    }

    @Test
    fun `الإصبع النازل يرفض الحقنة قبل أي شيء`() {
        assertEquals(
            InjectionRefusal.FINGER_DOWN,
            TouchInjectionRules.refusal(nowMs = 10_000L, lastInjectedAtMs = 0L, finger = FingerReading.DOWN),
        )
    }

    @Test
    fun `الحقنتان المتقاربتان تُرفضان والفاصل يعيد السماح`() {
        assertEquals(
            InjectionRefusal.RATE_LIMITED,
            TouchInjectionRules.refusal(1_000L, 950L, FingerReading.UP),
        )
        assertNull(TouchInjectionRules.refusal(1_100L, 950L, FingerReading.UP))
        assertNull(TouchInjectionRules.refusal(1_000L, 0L, FingerReading.UNKNOWN))
    }

    @Test
    fun `قراءة getevent تُفسَّر نزولًا ورفعًا`() {
        val down = "/dev/input/event3: 0001 014a 00000001\n/dev/input/event3: 0000 0000 00000000"
        val up = "/dev/input/event3: 0001 014a 00000000"
        val touchDown = "/dev/input/event3: 0003 0039 0000000a"
        val touchUp = "/dev/input/event3: 0003 0039 ffffffff"
        assertEquals(FingerReading.DOWN, FingerProbe.interpret(down))
        assertEquals(FingerReading.UP, FingerProbe.interpret(up))
        assertEquals(FingerReading.DOWN, FingerProbe.interpret(touchDown))
        assertEquals(FingerReading.UP, FingerProbe.interpret(touchUp))
        assertEquals(FingerReading.UNKNOWN, FingerProbe.interpret(""))
    }
}
