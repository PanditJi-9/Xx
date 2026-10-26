/*
 * Copyright (C) 2026-2027 Zexshia
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */

package nd.max.core.daemon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حارس إيقاع دورة رفيق الجذر.
 *
 * **ولماذا اختبار على ثوابت تبدو بسيطة:** الإيقاع هو **الأثر المقيس** لهذه الجولة على حرارة
 * الجهاز وبطاريته — وهو أول ما يُعاد «تبسيطًا» إلى ٥٠٠ م.ث في مراجعة قادمة بلا قياس. فهنا يُثبَّت:
 * كل حالة وإيقاعها، وأنّ لحظة اللعب **لم** تتغيّر، وأنّ الفرق بالأرقام لا بالانطباع.
 */
class CompanionPollPolicyTest {

    @Test
    fun `the gaming moment keeps the responsive cadence`() {
        assertEquals(
            "a managed app in front with the screen on keeps 500ms — this path must not slow down",
            CompanionPollPolicy.ACTIVE_MS,
            CompanionPollPolicy.intervalMs(screenInteractive = true, managedAppOwnsForeground = true),
        )
    }

    @Test
    fun `screen on without a managed app relaxes to one second`() {
        assertEquals(
            CompanionPollPolicy.IDLE_MS,
            CompanionPollPolicy.intervalMs(screenInteractive = true, managedAppOwnsForeground = false),
        )
    }

    @Test
    fun `screen off backs off tenfold`() {
        assertEquals(
            CompanionPollPolicy.SCREEN_OFF_MS,
            CompanionPollPolicy.intervalMs(screenInteractive = false, managedAppOwnsForeground = false),
        )
        // وحتى لو بقي تطبيق مُدار «مالكًا» للحالة، فالشاشة المطفأة تُغلب: لا مقدّمة تُطبَّق عليها.
        assertEquals(
            CompanionPollPolicy.SCREEN_OFF_MS,
            CompanionPollPolicy.intervalMs(screenInteractive = false, managedAppOwnsForeground = true),
        )
    }

    @Test
    fun `the measured win is stated as numbers, not as a feeling`() {
        val screenOffBefore = CompanionPollPolicy.wakeupsPerHour(500L)
        val screenOffAfter = CompanionPollPolicy.wakeupsPerHour(CompanionPollPolicy.SCREEN_OFF_MS)
        assertEquals(7_200L, screenOffBefore)
        assertEquals(720L, screenOffAfter)
        assertEquals("the screen-off backoff is exactly one tenth of the wakeups", screenOffBefore / 10, screenOffAfter)

        val idleBefore = CompanionPollPolicy.wakeupsPerHour(500L)
        val idleAfter = CompanionPollPolicy.wakeupsPerHour(CompanionPollPolicy.IDLE_MS)
        assertEquals(3_600L, idleAfter)
        assertEquals("an idle screen-on device halves its wakeups", idleBefore / 2, idleAfter)
    }

    @Test
    fun `the config watcher relaxes with the same rule and never goes below a single stat per second`() {
        assertEquals(CompanionPollPolicy.CONFIG_WATCH_ACTIVE_MS, CompanionPollPolicy.configWatchMs(true))
        assertEquals(CompanionPollPolicy.CONFIG_WATCH_IDLE_MS, CompanionPollPolicy.configWatchMs(false))
        assertTrue(
            "the active watcher is coarser than the old 120ms — that is the point, and it stays sub-second",
            CompanionPollPolicy.CONFIG_WATCH_ACTIVE_MS in 200L..999L,
        )
    }

    @Test
    fun `the config watcher backs off while the screen is off`() {
        assertEquals(
            CompanionPollPolicy.CONFIG_WATCH_SCREEN_OFF_MS,
            CompanionPollPolicy.configWatchMs(managedAppOwnsForeground = true, screenInteractive = false),
        )
        assertTrue(
            "screen off is the slowest watcher, and still a single stat every two seconds",
            CompanionPollPolicy.CONFIG_WATCH_SCREEN_OFF_MS >= CompanionPollPolicy.CONFIG_WATCH_IDLE_MS,
        )
    }

    @Test
    fun `battery saver slows the idle loop but never the gaming moment`() {
        assertEquals(
            "a managed app in front keeps 500ms even in battery saver — the session path is not optional",
            CompanionPollPolicy.ACTIVE_MS,
            CompanionPollPolicy.intervalMs(true, managedAppOwnsForeground = true, powerSave = true),
        )
        assertEquals(
            CompanionPollPolicy.IDLE_POWER_SAVE_MS,
            CompanionPollPolicy.intervalMs(true, managedAppOwnsForeground = false, powerSave = true),
        )
        assertEquals(
            "screen off is already slower than the power-save idle interval",
            CompanionPollPolicy.SCREEN_OFF_MS,
            CompanionPollPolicy.intervalMs(false, managedAppOwnsForeground = false, powerSave = true),
        )
    }

    @Test
    fun `power save halving the idle wakeups is arithmetic, not a claim`() {
        val idle = CompanionPollPolicy.wakeupsPerHour(
            CompanionPollPolicy.intervalMs(true, managedAppOwnsForeground = false, powerSave = false),
        )
        val saving = CompanionPollPolicy.wakeupsPerHour(
            CompanionPollPolicy.intervalMs(true, managedAppOwnsForeground = false, powerSave = true),
        )
        assertEquals(3_600L, idle)
        assertEquals(1_800L, saving)
        assertEquals(idle / 2, saving)
    }
}
