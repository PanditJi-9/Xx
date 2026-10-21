/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

import nd.max.core.hardware.DeviceStateCollector.DeviceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد الهدف: الأوزان تترجم إلى قرارات، والشاشة المطفأة هدف مختلف لا
 * "إيقاف". هذه الطبقة تحل محل الأوضاع الثابتة (قرار #2 و#11)، فأي
 * انحدار فيها يعني عودة "مبدّل الملفات" من الباب الخلفي.
 */
class ObjectiveTest {

    private fun snapshot(
        cpuLoad: Float = 0.5f,
        thermal: Float = 0.3f,
        battery: Float = 0.6f,
        appIntent: Float = 0.5f,
        screenOn: Float = 1f,
    ) = DeviceSnapshot(
        cpuLoad = cpuLoad, thermal = thermal, battery = battery,
        appIntent = appIntent, screenOn = screenOn,
        memoryUsage = 0.5f, networkSpeed = 0.1f,
    )

    @Test
    fun `performance objective prefers raising under real load with thermal room`() {
        val direction = Objective.PERFORMANCE.preferredDirection(
            snapshot(cpuLoad = 0.85f, thermal = 0.35f)
        )
        assertEquals(ControlRegistry.Direction.RAISE_PERFORMANCE, direction)
    }

    @Test
    fun `eco objective always saves energy even under load`() {
        val direction = Objective.ECO.preferredDirection(
            snapshot(cpuLoad = 0.95f, thermal = 0.2f)
        )
        assertEquals(ControlRegistry.Direction.SAVE_ENERGY, direction)
    }

    @Test
    fun `hot device is never told to raise performance`() {
        val direction = Objective.PERFORMANCE.preferredDirection(
            snapshot(cpuLoad = 0.9f, thermal = 0.7f)
        )
        assertEquals(ControlRegistry.Direction.SAVE_ENERGY, direction)
    }

    @Test
    fun `screen off objective carries zero performance weight`() {
        assertEquals(0f, Objective.SCREEN_OFF.performance, 0.0001f)
        assertTrue(Objective.SCREEN_OFF.battery > 0f)
        assertTrue(Objective.SCREEN_OFF.thermalHeadroom > 0f)
    }

    @Test
    fun `score rewards low load cool device and full battery`() {
        val ideal = Objective.BALANCED.score(
            snapshot(cpuLoad = 0f, thermal = 0f, battery = 1f)
        )
        val stressed = Objective.BALANCED.score(
            snapshot(cpuLoad = 1f, thermal = 1f, battery = 0f)
        )
        assertTrue("الجهاز المثالي يجب أن يتفوق", ideal > stressed)
        assertEquals(1f, ideal, 0.0001f)
    }

    @Test
    fun `eco can be satisfied above the battery comfort line`() {
        // **العطب المقيس (سجل جهاز + لقطات ٢٠٢٦-١٠-٢١):** كان وزن البطارية هو نسبة الشحن
        // نفسها، فسقف الدرجة في وضع الترشيد عند ١٥٪ = 0.253 — و`SATISFIED_SCORE = 0.75`
        // غير قابل للبلوغ رياضيًّا. فالمحرك بلا راحة: يهبط بسقف CPU درجة كل ٣٠ ثانية إلى
        // أرضية السلّم. الدعوى القابلة للتكذيب: عند شحن ٦٠٪ وحمل منخفض وجهاز بارد يجب أن
        // يبلغ الجهاز عتبة الرضا فلا يتدخل.
        val score = Objective.ECO.score(
            snapshot(cpuLoad = 0.4f, thermal = 0.35f, battery = 0.6f)
        )
        assertTrue("درجة $score يجب أن تبلغ ${MinimalPlanner.SATISFIED_SCORE}", score >= MinimalPlanner.SATISFIED_SCORE)
    }

    @Test
    fun `eco still sees a gap when the battery is genuinely low`() {
        val score = Objective.ECO.score(
            snapshot(cpuLoad = 0.57f, thermal = 0.441f, battery = 0.15f)
        )
        assertTrue("الشحن المنخفض يبقى فجوة تُبرّر خطوة", score < MinimalPlanner.SATISFIED_SCORE)
    }

    @Test
    fun `battery headroom saturates at the comfort line and never exceeds one`() {
        assertEquals(0f, Objective.batteryHeadroom(0f), 0.0001f)
        assertEquals(0.5f, Objective.batteryHeadroom(Objective.BATTERY_COMFORT_FRACTION / 2f), 0.0001f)
        assertEquals(1f, Objective.batteryHeadroom(Objective.BATTERY_COMFORT_FRACTION), 0.0001f)
        assertEquals(1f, Objective.batteryHeadroom(1f), 0.0001f)
    }

    @Test
    fun `performance never raises on a low battery`() {
        // رفع السقف عند شحن منخفض نقيضُ طلب المستخدم ومصدر حرارةٍ لا يُبرّره وضع الأداء.
        val direction = Objective.PERFORMANCE.preferredDirection(
            snapshot(cpuLoad = 0.95f, thermal = 0.3f, battery = 0.2f)
        )
        assertEquals(ControlRegistry.Direction.SAVE_ENERGY, direction)
    }

    @Test
    fun `preference round-trips through labels`() {
        listOf(
            "performance" to Objective.PERFORMANCE,
            "battery" to Objective.ECO,
            "balanced" to Objective.BALANCED,
        ).forEach { (label, objective) ->
            assertEquals(label, Objective.labelFor(objective))
            assertEquals(objective, Objective.fromPreference(label))
        }
    }

    @Test
    fun `unknown preference falls back to balanced`() {
        assertEquals(Objective.BALANCED, Objective.fromPreference(null))
        assertEquals(Objective.BALANCED, Objective.fromPreference("nonsense"))
    }
}
