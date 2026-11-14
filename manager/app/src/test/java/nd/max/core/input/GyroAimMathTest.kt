/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حساب الجيروسكوب نقيّ: المدخل راديان/ثانية والخرج بكسل، والحدود مقيسة هنا.
 */
class GyroAimMathTest {

    private val settings = GyroAimSettings(enabled = true, sensitivity = 1f, deadZone = 1.5f, smoothing = 0f)

    @Test
    fun `المنطقة الميتة تُصفّر الحركة الصغيرة وتطرح نفسها من الكبيرة`() {
        assertEquals(0f, GyroAimMath.deadband(1.4f, 1.5f), 0f)
        assertEquals(0f, GyroAimMath.deadband(-1.5f, 1.5f), 0f)
        assertEquals(3.5f, GyroAimMath.deadband(5f, 1.5f), 0.001f)
        assertEquals(-3.5f, GyroAimMath.deadband(-5f, 1.5f), 0.001f)
    }

    @Test
    fun `الخطوة تتبع الزمن لا عدد العيّنات`() {
        val one = GyroAimMath.step(1f, 0f, 16L, settings)
        val two = GyroAimMath.step(1f, 0f, 32L, settings)
        assertEquals(one.dx * 2f, two.dx, 0.5f)
    }

    @Test
    fun `سقف الخطوة يمنع القفزة`() {
        val delta = GyroAimMath.step(50f, -50f, 250L, settings)
        assertEquals(GyroAimMath.MAX_STEP_PX, delta.dx, 0f)
        assertEquals(-GyroAimMath.MAX_STEP_PX, delta.dy, 0f)
    }

    @Test
    fun `فاصل أقل من الحد لا يولّد خطوة`() {
        val delta = GyroAimMath.step(5f, 5f, 4L, settings)
        assertTrue(delta.isStill)
    }

    @Test
    fun `التنعيم يخلط الحالية بالسابقة`() {
        assertEquals(5f, GyroAimMath.smooth(10f, 0f, 0.5f), 0.001f)
        assertEquals(10f, GyroAimMath.smooth(10f, 10f, 0.5f), 0.001f)
    }

    @Test
    fun `الحساسية والمنطقة الميتة تُقيَّدان`() {
        assertEquals(GyroAimMath.MIN_SENSITIVITY, GyroAimMath.sensitivityAfter(0.3f, -5f), 0f)
        assertEquals(GyroAimMath.MAX_SENSITIVITY, GyroAimMath.sensitivityAfter(2.9f, 5f), 0f)
        assertEquals(0f, GyroAimMath.deadZoneAfter(0.2f, -1f), 0f)
        assertEquals(GyroAimMath.MAX_DEAD_ZONE, GyroAimMath.deadZoneAfter(7.5f, 2f), 0f)
    }
}
