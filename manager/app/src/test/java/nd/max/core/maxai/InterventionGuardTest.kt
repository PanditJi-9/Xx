/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حارس معدّل التدخل — أثره السابق مقيس لا مفترض: ٣٠١ كتابة سقف في ١٥ ساعة، تأرجح
 * ١٫٠↔١٫٤ جيجاهرتز كل ٤٠–٥٠ ثانية، ونزول إلى ٥٠٠ ميجاهرتز. والعقد المطلوب هنا:
 *
 * 1. بعد خطوة في اتجاه، لا عكسها قبل انقضاء المهلة.
 * 2. لا أكثر من ثلاث خطوات على المقبض في نصف ساعة.
 * 3. الأمان لا يمرّ من هذا الحارس (مستقل في `SafetyEngine`) — والميزانية تُصفّر بالإطفاء.
 */
class InterventionGuardTest {

    private val key = "cpu_limits:policy0"

    @Test
    fun `first step is always allowed`() {
        val guard = InterventionGuard()
        val verdict = guard.check(key, ControlRegistry.Direction.SAVE_ENERGY, nowMs = 1_000L)
        assertFalse(verdict.blocked)
        assertEquals(0L, verdict.retryInMs)
    }

    @Test
    fun `reversing direction is held for the full window`() {
        val guard = InterventionGuard()
        val start = 10_000L
        guard.record(key, ControlRegistry.Direction.SAVE_ENERGY, start)

        // بعد أربعين ثانية — وهو الإيقاع المقيس الذي كان يقلب الاتجاه.
        val tooSoon = guard.check(key, ControlRegistry.Direction.RAISE_PERFORMANCE, start + 40_000L)
        assertTrue("عكس الخطوة بعد ٤٠ ثانية كان يُنتج الكتابة المتأرجحة", tooSoon.blocked)
        assertEquals(InterventionGuard.Reason.DIRECTION_HOLD, tooSoon.reason)
        assertEquals(InterventionGuard.DIRECTION_HOLD_MS - 40_000L, tooSoon.retryInMs)

        val later = guard.check(key, ControlRegistry.Direction.RAISE_PERFORMANCE, start + InterventionGuard.DIRECTION_HOLD_MS)
        assertFalse("بعد انقضاء المهلة لا حجب", later.blocked)
    }

    @Test
    fun `same direction is not held by the direction rule`() {
        val guard = InterventionGuard()
        guard.record(key, ControlRegistry.Direction.SAVE_ENERGY, 5_000L)
        val next = guard.check(key, ControlRegistry.Direction.SAVE_ENERGY, 6_000L)
        assertFalse(next.blocked)
    }

    @Test
    fun `the step budget stops a walk to the ladder floor`() {
        val guard = InterventionGuard()
        var now = 0L
        repeat(InterventionGuard.MAX_STEPS_PER_WINDOW) {
            assertFalse(guard.check(key, ControlRegistry.Direction.SAVE_ENERGY, now).blocked)
            guard.record(key, ControlRegistry.Direction.SAVE_ENERGY, now)
            now += 60_000L
        }
        val blocked = guard.check(key, ControlRegistry.Direction.SAVE_ENERGY, now)
        assertTrue("الخطوة الرابعة بلا بلوغ الهدف ليست تعلّمًا", blocked.blocked)
        assertEquals(InterventionGuard.Reason.STEP_BUDGET, blocked.reason)
        assertTrue(blocked.retryInMs > 0L)
    }

    @Test
    fun `an old step falls out of the window`() {
        val guard = InterventionGuard()
        var now = 0L
        repeat(InterventionGuard.MAX_STEPS_PER_WINDOW) {
            guard.record(key, ControlRegistry.Direction.SAVE_ENERGY, now)
            now += 60_000L
        }
        assertTrue(guard.check(key, ControlRegistry.Direction.SAVE_ENERGY, now).blocked)
        val afterWindow = now + InterventionGuard.STEP_WINDOW_MS
        assertFalse(
            "الميزانية نافذة زمنية لا عمر الجلسة",
            guard.check(key, ControlRegistry.Direction.SAVE_ENERGY, afterWindow).blocked,
        )
    }

    @Test
    fun `budgets are per knob so one knob cannot block another`() {
        val guard = InterventionGuard()
        var now = 0L
        repeat(InterventionGuard.MAX_STEPS_PER_WINDOW) {
            guard.record(key, ControlRegistry.Direction.SAVE_ENERGY, now)
            now += 1_000L
        }
        assertTrue(guard.check(key, ControlRegistry.Direction.SAVE_ENERGY, now).blocked)
        assertFalse(guard.check("cpu_limits:policy4", ControlRegistry.Direction.SAVE_ENERGY, now).blocked)
    }

    @Test
    fun `reset clears the session budget`() {
        val guard = InterventionGuard()
        var now = 0L
        repeat(InterventionGuard.MAX_STEPS_PER_WINDOW) {
            guard.record(key, ControlRegistry.Direction.SAVE_ENERGY, now)
            now += 1_000L
        }
        guard.reset()
        assertFalse(guard.check(key, ControlRegistry.Direction.SAVE_ENERGY, now).blocked)
    }
}
