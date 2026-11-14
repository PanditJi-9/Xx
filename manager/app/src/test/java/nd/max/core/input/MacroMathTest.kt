/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** الماكرو: تصنيف الخطوة، وحدود الفاصل، والتخزين، وسقف الخطوات. */
class MacroMathTest {

    @Test
    fun `الفاصل يُقيَّد بين حدّيه`() {
        assertEquals(MacroMath.MIN_DELAY_MS, MacroMath.clampDelay(-5L))
        assertEquals(MacroMath.MIN_DELAY_MS, MacroMath.clampDelay(0L))
        assertEquals(MacroMath.MAX_DELAY_MS, MacroMath.clampDelay(99_999L))
        assertEquals(500L, MacroMath.clampDelay(500L))
    }

    @Test
    fun `الحركة الصغيرة نقرة والكبيرة سحبة`() {
        assertTrue(MacroMath.isTap(0.5f, 0.5f, 0.5001f, 0.5001f))
        assertFalse(MacroMath.isTap(0.5f, 0.5f, 0.7f, 0.5f))
        assertFalse(MacroMath.isTap(0.5f, 0.5f, 0.5f, 0.2f))
    }

    @Test
    fun `التسجيل يصنّف الخطوة ويقيّدها`() {
        val tap = MacroMath.recorded(0.3f, 0.4f, 0.3001f, 0.4f, 120L)
        assertEquals(MacroStepKind.TAP, tap.kind)
        assertEquals(120L, tap.delayMs)
        val swipe = MacroMath.recorded(0.1f, 0.2f, 0.8f, 0.9f, 1L)
        assertEquals(MacroStepKind.SWIPE, swipe.kind)
        assertEquals(MacroMath.MIN_DELAY_MS, swipe.delayMs)
        val outside = MacroMath.recorded(-1f, 2f, 3f, -4f, 500L)
        assertEquals(0f, outside.x1, 0f)
        assertEquals(1f, outside.y1, 0f)
        assertEquals(1f, outside.x2, 0f)
        assertEquals(0f, outside.y2, 0f)
    }

    @Test
    fun `التنظيف يحدّ الخطوات بالسقف`() {
        val many = (0..60).map { MacroStep(MacroStepKind.TAP, 0.5f, 0.5f, 0.5f, 0.5f, 350L) }
        assertEquals(MacroMath.MAX_STEPS, MacroMath.sanitize(many).size)
    }

    @Test
    fun `التخزين يعيد نفس الخطوات ويحتمل المعطوب`() {
        val steps = listOf(
            MacroStep(MacroStepKind.TAP, 0.1f, 0.2f, 0.1f, 0.2f, 400L),
            MacroStep(MacroStepKind.SWIPE, 0.3f, 0.4f, 0.7f, 0.8f, 120L),
        )
        assertEquals(steps, MacroMath.decode(MacroMath.encode(steps)))
        assertEquals(emptyList<MacroStep>(), MacroMath.decode(null))
        assertEquals(emptyList<MacroStep>(), MacroMath.decode("garbage"))
        assertEquals(emptyList<MacroStep>(), MacroMath.decode("NOPE,0.1,0.2,0.1,0.2,400"))
        assertEquals(emptyList<MacroStep>(), MacroMath.decode("TAP,0.1,0.2,0.1,0.2"))
    }

    @Test
    fun `الفاصل الافتراضي داخل حدوده`() {
        assertTrue(MacroMath.DEFAULT_DELAY_MS in MacroMath.MIN_DELAY_MS..MacroMath.MAX_DELAY_MS)
    }
}
