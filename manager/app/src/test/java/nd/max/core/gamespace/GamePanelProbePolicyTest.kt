/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حارس إيقاع خدمة اللوحة — **الحالة شيء، وسؤال النظام شيء آخر**.
 *
 * **العطب المقيس الذي وُلد منه الفصل:** `poll()` كان يسأل عن التطبيق المقدّمة في كل دورة. وفي مسار
 * الجذر السؤال يفتح صدفة تُنفّذ `dumpsys activity activities` (أثقل تفريغ في النظام). فكانت
 * الخدمة الخاملة تدفع ثمنه **١٨٠٠ مرة في الساعة** بلا لعبة في المقدّمة. وهذه الدعاوى تُثبّت:
 * لحظة اللعب تبقى على إيقاعها، والخمول يبطؤ، والشاشة المطفأة لا سؤال أصلًا، وتوفير البطارية
 * يخفض الاثنين — ولا يُعطّل مسار اللعب.
 */
class GamePanelProbePolicyTest {

    @Test
    fun `the gaming moment keeps its cadence`() {
        assertEquals(PANEL_ACTIVE_POLL_MS, panelPollDelayMs(interactive = true, gameInFront = true))
        assertEquals(
            "battery saver must not slow the state loop while a game is in front",
            PANEL_ACTIVE_POLL_MS,
            panelPollDelayMs(interactive = true, gameInFront = true, powerSave = true),
        )
        assertEquals(
            "the probe keeps up with the state loop only while a game is in front",
            PANEL_ACTIVE_POLL_MS,
            panelForegroundProbeMs(interactive = true, gameInFront = true),
        )
    }

    @Test
    fun `an idle panel probes far less often than it ticks`() {
        val tick = panelPollDelayMs(interactive = true, gameInFront = false)
        val probe = panelForegroundProbeMs(interactive = true, gameInFront = false)
        assertEquals(PANEL_IDLE_POLL_MS, tick)
        assertTrue(
            "the expensive shell probe must not run on every idle tick (tick=$tick probe=$probe)",
            probe >= tick * 2,
        )
    }

    @Test
    fun `battery saver slows the idle panel and the probe together`() {
        assertEquals(
            PANEL_IDLE_POWER_SAVE_POLL_MS,
            panelPollDelayMs(interactive = true, gameInFront = false, powerSave = true),
        )
        assertEquals(
            PANEL_OPEN_POWER_SAVE_POLL_MS,
            panelPollDelayMs(interactive = true, gameInFront = false, cockpitOpen = true, powerSave = true),
        )
        assertEquals(
            20_000L,
            panelForegroundProbeMs(interactive = true, gameInFront = false, powerSave = true),
        )
        assertTrue(
            "the opened cockpit stays usable in battery saver — it is a user-facing surface",
            PANEL_OPEN_POWER_SAVE_POLL_MS <= 2_000L,
        )
    }

    @Test
    fun `a dark screen asks the system nothing`() {
        assertEquals(PANEL_SCREEN_OFF_POLL_MS, panelPollDelayMs(interactive = false, gameInFront = false))
        assertEquals(Long.MAX_VALUE, panelForegroundProbeMs(interactive = false, gameInFront = false))
        assertEquals(
            "even a tracked game is not probed while the screen is off",
            Long.MAX_VALUE,
            panelForegroundProbeMs(interactive = false, gameInFront = true),
        )
    }

    @Test
    fun `panelStep hands the power-save decision to the delay, visibly`() {
        val step = panelStep(
            previous = GamePanelState(),
            enabledPackages = setOf("com.game"),
            side = PanelSide.End,
            interactive = true,
            foreground = "com.game",
            library = setOf("com.game"),
            powerSave = true,
        )
        assertEquals(
            "a game in front keeps the responsive tick regardless of power save",
            PANEL_ACTIVE_POLL_MS,
            step.nextDelayMs,
        )
    }
}
