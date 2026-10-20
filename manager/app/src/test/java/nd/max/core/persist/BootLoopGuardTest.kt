/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.persist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حارس الحلقة يُقاس على النموذج النقيّ: لا أندرويد ولا ساعة داخلية.
 *
 * والفرق الذي تقيسه هذه العقود هو الفرق الذي يجعل الحارس صالحًا: `BOOT_COMPLETED` و
 * `USER_UNLOCKED` في الإقلاع نفسه **لا يُعدّان** إقلاعين، لأن `boot_id` واحد. وهذا هو العطب
 * الذي كان سيُطفئ الاستعادة الآمنة على جهاز سليم لو حُسب الزمن وحده.
 */
class BootLoopGuardTest {

    @Test
    fun `first observation records the boot without counting it`() {
        val decision = BootLoopGuard.evaluate(
            previousBootId = "",
            currentBootId = "boot-a",
            previousBootAtMs = 0L,
            nowMs = 1_000_000L,
            streak = 0,
        )
        assertTrue(decision.newBoot)
        assertEquals(0, decision.streak)
        assertFalse(decision.trip)
    }

    @Test
    fun `the same boot id is the same session and never counts`() {
        val decision = BootLoopGuard.evaluate(
            previousBootId = "boot-a",
            currentBootId = "boot-a",
            previousBootAtMs = 1_000_000L,
            nowMs = 1_010_000L, // USER_UNLOCKED بعد ثوانٍ من BOOT_COMPLETED
            streak = 0,
        )
        assertFalse(decision.newBoot)
        assertEquals(0, decision.streak)
        assertFalse(decision.trip)
    }

    @Test
    fun `a reboot inside the window increments the streak`() {
        val decision = BootLoopGuard.evaluate(
            previousBootId = "boot-a",
            currentBootId = "boot-b",
            previousBootAtMs = 1_000_000L,
            nowMs = 1_060_000L, // ٦٠ ثانية: إقلاع سريع
            streak = 0,
        )
        assertTrue(decision.newBoot)
        assertEquals(1, decision.streak)
        assertFalse(decision.trip)
    }

    @Test
    fun `two fast reboots in a row trip the guard`() {
        val second = BootLoopGuard.evaluate(
            previousBootId = "boot-b",
            currentBootId = "boot-c",
            previousBootAtMs = 2_000_000L,
            nowMs = 2_030_000L,
            streak = 1,
        )
        assertTrue(second.newBoot)
        assertEquals(BootLoopGuard.FAST_REBOOTS_TO_TRIP, second.streak)
        assertTrue(second.trip)
    }

    @Test
    fun `a normal-length boot clears the streak`() {
        val decision = BootLoopGuard.evaluate(
            previousBootId = "boot-a",
            currentBootId = "boot-b",
            previousBootAtMs = 1_000_000L,
            nowMs = 1_000_000L + BootLoopGuard.FAST_REBOOT_WINDOW_MS,
            streak = 1,
        )
        assertTrue(decision.newBoot)
        assertEquals(0, decision.streak)
        assertFalse(decision.trip)
    }

    @Test
    fun `a clock that moved backwards is not evidence of a fast boot`() {
        val decision = BootLoopGuard.evaluate(
            previousBootId = "boot-a",
            currentBootId = "boot-b",
            previousBootAtMs = 9_000_000L,
            nowMs = 1_000_000L,
            streak = 1,
        )
        assertTrue(decision.newBoot)
        assertEquals(0, decision.streak)
        assertFalse(decision.trip)
    }

    @Test
    fun `an unreadable boot id makes no claim`() {
        val decision = BootLoopGuard.evaluate(
            previousBootId = "boot-a",
            currentBootId = "",
            previousBootAtMs = 1_000_000L,
            nowMs = 1_020_000L,
            streak = 1,
        )
        assertFalse(decision.newBoot)
        assertEquals(1, decision.streak)
        assertFalse(decision.trip)
    }
}
