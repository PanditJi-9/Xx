/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelTogglesTest {
    @Test
    fun reminderCyclesThroughStepsThenStops() {
        var current = 0
        val seen = mutableListOf<Int>()
        repeat(REMINDER_STEPS.size + 1) {
            current = nextReminderMinutes(current)
            seen += current
        }
        assertEquals(REMINDER_STEPS + 0, seen)
    }

    @Test
    fun unknownReminderValueRestartsFromTheFirstStep() {
        assertEquals(REMINDER_STEPS.first(), nextReminderMinutes(45))
    }

    /**
     * مصدر الحقيقة للمقبض: الحيّة أولًا. وكانت اللوحة تقرأ `wifi_on`/`mobile_data` المخزَّنين
     * وحدهما، فيقول المفتاح «مغلق» وWi-Fi يعمل — ثم يكتب عكس ما يراه المستخدم.
     */
    @Test
    fun liveReadingWinsOverTheStoredMirror() {
        assertTrue(PanelStateRules.resolve(live = true, mirror = 0, fallback = false))
        assertFalse(PanelStateRules.resolve(live = false, mirror = 1, fallback = true))
    }

    @Test
    fun aMissingLiveReadingFallsBackToTheMirrorThenTheDefault() {
        assertTrue(PanelStateRules.resolve(live = null, mirror = 1, fallback = false))
        assertFalse(PanelStateRules.resolve(live = null, mirror = 0, fallback = true))
        assertFalse(PanelStateRules.resolve(live = null, mirror = null, fallback = false))
        assertTrue(PanelStateRules.resolve(live = null, mirror = null, fallback = true))
    }

    /** ولا نجاح بلا قراءة مطابقة — والغياب ليس مطابقة. */
    @Test
    fun verificationNeverTreatsAMissingReadingAsSuccess() {
        assertTrue(PanelWriteRules.honoured(desired = true, observed = true))
        assertTrue(PanelWriteRules.honoured(desired = false, observed = false))
        assertFalse(PanelWriteRules.honoured(desired = true, observed = false))
        assertFalse(PanelWriteRules.honoured(desired = true, observed = null))
    }

    @Test
    fun verificationIsBoundedInTimeAndAttempts() {
        assertTrue(PanelWriteRules.VERIFY_DELAYS_MS.isNotEmpty())
        assertTrue(
            "المجموع يجب أن يبقى قصيرًا فلا تُعلَّق ضغطة المستخدم",
            PanelWriteRules.VERIFY_DELAYS_MS.sum() <= 3_000L,
        )
    }
}
