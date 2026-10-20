/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * قواعد اللوحة الجانبية — **تُقاس بلا شاشة ولا أندرويد**.
 *
 * والأربعة التي تحكمها: (١) ما لم يُقرأ ليس «غير لعبة»، (٢) ما فُعِّل له أثر يُفتح،
 * (٣) الفتح لا ينجو من مغادرة اللعبة، (٤) الشرائح لا تُخفى تحت شريحة الحالة.
 */
class GameSessionPanelTest {

    private val library = setOf("com.game.one", "com.game.two")

    @Test fun unreadableForegroundIsUnknownNotNotAGame() {
        // `getForegroundPackage()` تُعيد "" حين لا يجيب المصدر — وقراءتها «ليست لعبة»
        // تُطوي اللوحة عن لعبة قائمة.
        assertEquals(PanelSubject.Unknown, panelSubject("", library))
        assertEquals(PanelSubject.Unknown, panelSubject(null, library))
        assertEquals(PanelSubject.Unknown, panelSubject("bad;pkg", library))
        assertEquals(PanelSubject.Unknown, panelSubject("com", library))
    }

    @Test fun readableForegroundIsEitherTrackedOrOther() {
        assertEquals(PanelSubject.Tracked("com.game.one"), panelSubject("com.game.one", library))
        assertEquals(PanelSubject.Other("com.chat.app"), panelSubject("com.chat.app", library))
    }

    @Test fun enabledTrackedGameAlwaysGetsAtLeastAHandle() {
        val state = reconcileGamePanel(GamePanelState(), PanelSubject.Tracked("com.game.one"), enabled = true, openByUser = false)
        assertEquals(GamePanelMode.Handle, state.mode)
        assertTrue(state.visible)
        assertFalse(state.coversGame)
        assertTrue(state.canOpen)
    }

    @Test fun openingIsTheOnlyModeThatTakesGameSpace() {
        val state = reconcileGamePanel(GamePanelState(), PanelSubject.Tracked("com.game.one"), enabled = true, openByUser = true)
        assertEquals(GamePanelMode.Open, state.mode)
        assertTrue(state.coversGame)
    }

    @Test fun nothingIsDrawnWhenDisabledOrWhenTheGameIsNotTracked() {
        val disabled = reconcileGamePanel(GamePanelState(), PanelSubject.Tracked("com.game.one"), enabled = false, openByUser = true)
        assertEquals(GamePanelMode.Hidden, disabled.mode)
        assertFalse(disabled.visible)

        val other = reconcileGamePanel(GamePanelState(), PanelSubject.Other("com.chat.app"), enabled = true, openByUser = true)
        assertEquals(GamePanelMode.Hidden, other.mode)
        assertFalse(other.canOpen)

        val unknown = reconcileGamePanel(GamePanelState(), PanelSubject.Unknown, enabled = true, openByUser = true)
        assertEquals(GamePanelMode.Hidden, unknown.mode)
    }

    @Test fun anUnreadableSampleCollapsesAnOpenPanelButKeepsTheUsersChoice() {
        // قراءة مفقودة لحظة واحدة (إشعار، مكالمة) لا تُلغي ما اختاره المستخدم: الحالة تُطوى
        // وتعود بلمسة حين تُقرأ اللعبة مرّة أخرى.
        val open = reconcileGamePanel(GamePanelState(), PanelSubject.Tracked("com.game.one"), enabled = true, openByUser = true)
        val blind = reconcileGamePanel(open, PanelSubject.Unknown, enabled = true, openByUser = true)
        assertEquals(GamePanelMode.Hidden, blind.mode)
        assertEquals(GamePanelMode.Open, reconcileGamePanel(blind, PanelSubject.Tracked("com.game.one"), enabled = true, openByUser = true).mode)
    }

    @Test fun leavingTheGameResetsTheOpenFlag() {
        val open = reconcileGamePanel(GamePanelState(), PanelSubject.Tracked("com.game.one"), enabled = true, openByUser = true)
        val left = reconcileGamePanel(open, PanelSubject.Tracked("com.game.two"), enabled = true, openByUser = false)
        assertEquals(GamePanelMode.Handle, left.mode)
    }

    @Test fun theSideSurvivesCollapsing() {
        val state = GamePanelState(side = PanelSide.Start)
        assertEquals(PanelSide.Start, reconcileGamePanel(state, PanelSubject.Tracked("com.game.one"), true, false).side)
        assertEquals(PanelSide.Start, reconcileGamePanel(state, PanelSubject.Unknown, true, false).side)
    }

    @Test fun placementRespectsSystemInsets() {
        // شاشة عرضية ٢٤٠٠×١٠٨٠، لوحة ٣٦٠×٧٠٠، شريط حالة ٦٠، شريط تنقّل ٤٨.
        val placement = panelPlacement(2400, 1080, 360, 700, 60, 48, PanelSide.End)
        assertEquals(2040, placement.x)
        assertTrue("اللوحة تبدأ أسفل شريط الحالة", placement.y >= 60)
        assertTrue("وتنتهي أعلى شريط التنقّل", placement.y + 700 <= 1080 - 48)
    }

    @Test fun placementStaysInsideANarrowScreen() {
        // شاشة أضيق من اللوحة: تُقصّ أفقيًّا إلى ما تبقّى، ورأسها لا يختفي تحت شريط الحالة.
        val start = panelPlacement(320, 480, 360, 700, 0, 0, PanelSide.Start)
        assertEquals(0, start.x)
        assertEquals(0, start.y)
        val end = panelPlacement(320, 480, 360, 700, 24, 24, PanelSide.End)
        assertEquals(0, end.x)
        assertEquals(24, end.y)
    }

    @Test fun anEnabledGameInFrontIsWhatNeedsTheFastPoll() {
        // «يحتاج الاستطلاع السريع» ⟺ لعبة مُفعَّلة أمامية الآن. أمّا بقاء الخدمة نفسها فقاعدة أخرى.
        val enabled = setOf("com.game.one")
        assertTrue(panelServiceNeeded(enabled, "com.game.one"))
        // لعبة غير مُفعَّلة، أو غير موجودة، أو لا قراءة: لا تحتاج الاستطلاع السريع.
        assertFalse(panelServiceNeeded(enabled, "com.chat.app"))
        assertFalse(panelServiceNeeded(enabled, "com.game.two"))
        assertFalse(panelServiceNeeded(enabled, ""))
        assertFalse(panelServiceNeeded(enabled, null))
    }

    @Test fun theRefreshCycleEndsAtNoOverrideRatherThanSixty() {
        // 60 ⟶ 90 ⟶ 120 ⟶ بلا فرض ⟶ 60…، والقيمة المجهولة تبدأ من أوّل الخيارات.
        assertEquals(90, nextRefreshRate(60))
        assertEquals(120, nextRefreshRate(90))
        assertNull(nextRefreshRate(120))
        assertEquals(60, nextRefreshRate(null))
        assertEquals(60, nextRefreshRate(45))
        assertNull(nextRefreshRate(60, emptyList()))
    }

    @Test fun theServiceStaysArmedWhileAnyGameIsEnabled() {
        // لا لعبة مُفعَّلة ⟹ لا خدمة، سواء أمامية كانت اللعبة أم لا.
        assertEquals(PanelLifetime.Stop, panelServiceLifetime(emptySet()))
        // لعبة مُفعَّلة واحدة تكفي: لم نرَها بعد، أو رأيناها ثم غادرت. الخدمة تبقى تراقب.
        assertEquals(PanelLifetime.Continue, panelServiceLifetime(setOf("com.game.one")))
        assertEquals(PanelLifetime.Continue, panelServiceLifetime(setOf("com.game.one", "com.game.two")))
    }

    @Test fun thePollSlowsWhenNothingIsDrawnAndStopsWithTheScreen() {
        assertEquals(1_500L, panelPollDelayMs(interactive = true, gameInFront = true))
        assertEquals(2_000L, panelPollDelayMs(interactive = true, gameInFront = false))
        assertEquals(10_000L, panelPollDelayMs(interactive = false, gameInFront = true))
        assertEquals(10_000L, panelPollDelayMs(interactive = false, gameInFront = false))
    }

    @Test fun closingThePanelHidesItForThisSessionWithoutDisarmingTheGame() {
        val tracked = PanelSubject.Tracked("com.game.one")
        val closed = reconcileGamePanel(GamePanelState(), tracked, enabled = true, openByUser = true, dismissed = true)
        assertEquals(GamePanelMode.Hidden, closed.mode)
        assertTrue(closed.dismissed)
        // الإغلاق لا يُعطّل اللعبة، والجلسة التالية تُرسم مقبضًا من جديد.
        assertTrue(closed.enabled)
        val nextSession = reconcileGamePanel(closed, tracked, enabled = true, openByUser = false, dismissed = false)
        assertEquals(GamePanelMode.Handle, nextSession.mode)
        assertFalse(nextSession.dismissed)
    }

    @Test fun tilesDeclareWhyTheyCannotActInsteadOfPretending() {
        assertEquals(GamePanelTileState.Ready, gamePanelTileState(GamePanelTile.RefreshRate))
        assertEquals(GamePanelTileState.ControlledElsewhere, gamePanelTileState(GamePanelTile.DoNotDisturb))
        assertEquals(GamePanelTileState.NotAvailableYet, gamePanelTileState(GamePanelTile.Capture))
    }

    @Test fun aPanelShorterThanTheScreenStaysFullyInsideIt() {
        val placement = panelPlacement(2400, 1080, 360, 420, 60, 48, PanelSide.End)
        assertTrue(placement.y >= 60)
        assertTrue(placement.y + 420 <= 1080 - 48)
        assertEquals(2040, placement.x)
    }
}
