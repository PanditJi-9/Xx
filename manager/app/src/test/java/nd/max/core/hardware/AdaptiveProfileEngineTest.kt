/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * محرّك السياسة التكيفية يُقاس هنا لأنه **قرار** لا أثر: خطؤه لا يرمي استثناءً، بل يختار
 * بروفايلًا خاطئًا فيسخّن الجهاز أو يخنقه — ولا أحد يرى السبب.
 *
 * وأهمّ اختبار فيه هو `high_load_while_charging_reaches_performance`: كان `PERFORMANCE`
 * **غير قابل للبلوغ** لأن شرطه أضيق من `GAMING` وقد جاء بعده، فلا يصدر أعلى بروفايل أبدًا.
 */
class AdaptiveProfileEngineTest {

    private fun input(
        t: Float? = null,
        battery: Int? = null,
        charging: Boolean = false,
        load: Int? = null,
    ) = AdaptiveProfileEngine.Input(t, battery, charging, load)

    @Test
    fun a_hot_temperature_forces_power_before_anything_else() {
        val decision = AdaptiveProfileEngine().decide(
            input(t = 49f, battery = 80, charging = true, load = 95),
        )

        assertEquals(AdaptiveProfileEngine.Profile.POWER, decision.profile)
        assertEquals("temperature-hot", decision.reason)
        assertTrue(decision.changed)
    }

    @Test
    fun a_critical_battery_off_the_charger_forces_power() {
        val decision = AdaptiveProfileEngine().decide(input(battery = 8, charging = false, load = 90))

        assertEquals(AdaptiveProfileEngine.Profile.POWER, decision.profile)
        assertEquals("battery-critical", decision.reason)
    }

    @Test
    fun the_same_battery_on_the_charger_does_not_force_power() {
        val decision = AdaptiveProfileEngine().decide(input(battery = 8, charging = true, load = 60))

        assertEquals(AdaptiveProfileEngine.Profile.GAMING, decision.profile)
    }

    @Test
    fun warmth_wins_over_load_so_the_device_is_not_pushed_while_hot() {
        val decision = AdaptiveProfileEngine().decide(input(t = 45f, battery = 90, charging = true, load = 95))

        assertEquals(AdaptiveProfileEngine.Profile.BALANCED, decision.profile)
        assertEquals("temperature-warm", decision.reason)
    }

    @Test
    fun high_load_while_charging_reaches_performance() {
        val decision = AdaptiveProfileEngine().decide(input(t = 34f, battery = 90, charging = true, load = 85))

        // كان هذا الاختبار يفشل قبل الإصلاح: `GAMING` (حمل ≥ ٥٥) كان يسبق `PERFORMANCE` (حمل ≥ ٨٠).
        assertEquals(AdaptiveProfileEngine.Profile.PERFORMANCE, decision.profile)
        assertEquals("high-load", decision.reason)
    }

    @Test
    fun high_load_on_a_healthy_battery_reaches_performance_without_the_charger() {
        val decision = AdaptiveProfileEngine().decide(input(battery = 50, charging = false, load = 85))

        assertEquals(AdaptiveProfileEngine.Profile.PERFORMANCE, decision.profile)
    }

    @Test
    fun high_load_on_a_low_battery_stops_at_gaming_instead_of_performance() {
        val decision = AdaptiveProfileEngine().decide(input(battery = 20, charging = false, load = 85))

        assertEquals(AdaptiveProfileEngine.Profile.GAMING, decision.profile)
        assertEquals("medium-load", decision.reason)
    }

    @Test
    fun a_medium_load_is_gaming_not_performance() {
        val decision = AdaptiveProfileEngine().decide(input(battery = 80, charging = true, load = 60))

        assertEquals(AdaptiveProfileEngine.Profile.GAMING, decision.profile)
        assertEquals("medium-load", decision.reason)
    }

    @Test
    fun an_unknown_input_holds_the_last_profile_instead_of_flapping() {
        val engine = AdaptiveProfileEngine()
        engine.decide(input(t = 50f))

        val decision = engine.decide(input())

        assertEquals(AdaptiveProfileEngine.Profile.POWER, decision.profile)
        assertEquals("stable", decision.reason)
        assertFalse(decision.changed)
    }

    @Test
    fun reset_forgets_the_last_profile() {
        val engine = AdaptiveProfileEngine()
        engine.decide(input(t = 50f))
        engine.reset()

        // بلا لقطة سابقة وبلاه دخل، الافتراض توازن لا POWER موروث.
        val decision = engine.decide(input())

        assertEquals(AdaptiveProfileEngine.Profile.BALANCED, decision.profile)
        assertTrue(decision.changed)
    }
}
