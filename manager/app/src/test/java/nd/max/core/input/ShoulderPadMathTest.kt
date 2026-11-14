/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.input

import org.junit.Assert.assertEquals
import org.junit.Test

/** موضع زرّ الكتف يُقاس كنسبة: لا يخرج عن الشاشة ولو تغيّرت الدقّة. */
class ShoulderPadMathTest {

    @Test
    fun `النسبة تُقيَّد بين حدّيها`() {
        assertEquals(ShoulderPadMath.MIN_FRACTION, ShoulderPadMath.clampFraction(-1f), 0f)
        assertEquals(ShoulderPadMath.MAX_FRACTION, ShoulderPadMath.clampFraction(3f), 0f)
        assertEquals(0.5f, ShoulderPadMath.clampFraction(0.5f), 0f)
        assertEquals(ShoulderPadMath.MIN_FRACTION, ShoulderPadMath.clampFraction(Float.NaN), 0f)
    }

    @Test
    fun `المركز يترك الزرّ كاملًا داخل الشاشة`() {
        assertEquals(0, ShoulderPadMath.centerPx(0f, 1080, 168))
        assertEquals(1080 - 168, ShoulderPadMath.centerPx(1f, 1080, 168))
        assertEquals((1080 - 168) / 2, ShoulderPadMath.centerPx(0.5f, 1080, 168))
    }

    @Test
    fun `النسبة والعكس يتحافظان`() {
        val px = ShoulderPadMath.centerPx(0.25f, 1080, 168)
        assertEquals(0.25f, ShoulderPadMath.fractionOf(px, 1080, 168), 0.01f)
    }

    @Test
    fun `مركز النقر وسط النافذة`() {
        assertEquals(200f, ShoulderPadMath.tapX(116, 168), 0f)
        assertEquals(300f, ShoulderPadMath.tapY(216, 168), 0f)
    }
}
