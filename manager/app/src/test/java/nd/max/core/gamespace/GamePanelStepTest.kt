/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ما يراه المستخدم عبر **استطلاعات متتالية**، بلا أندرويد.
 *
 * كل خطوة هنا تمرّ من `panelStep`، وهي الدالّة نفسها التي تستدعيها الخدمة في كل استطلاع. فالاختبار يقيس ما يحدث
 * فعلًا: تفعيل، ثم فتح اللعبة من خارج التطبيق، ثم الفتح والإغلاق والمغادرة والعودة، ثم الشاشة المطفأة.
 */
class GamePanelStepTest {

    private val game = "com.game.one"
    private val otherGame = "com.game.two"
    private val chat = "com.chat.app"
    private val library = setOf(game, otherGame)
    private val armed = setOf(game)

    private fun poll(
        previous: GamePanelState,
        enabled: Set<String> = armed,
        foreground: String? = game,
        interactive: Boolean = true
    ): PanelStep = panelStep(
        previous = previous,
        enabledPackages = enabled,
        side = PanelSide.End,
        interactive = interactive,
        foreground = foreground,
        library = library
    )

    @Test fun anArmedGameOpenedFromOutsideTheAppShowsItsHandleAtOnce() {
        // لم يُفتح شيء من داخل التطبيق: أوّل استطلاع يرى اللعبة في المقدّمة فيرسم المقبض.
        val step = poll(GamePanelState(), foreground = game)
        assertEquals(GamePanelMode.Handle, step.state.mode)
        assertTrue(step.samplerWanted)
        assertEquals(PanelLifetime.Continue, step.lifetime)
        assertEquals(PANEL_ACTIVE_POLL_MS, step.nextDelayMs)
    }

    @Test fun aGameThatIsNotArmedNeverShowsAndNeverSamples() {
        val step = poll(GamePanelState(), enabled = emptySet(), foreground = game)
        assertEquals(GamePanelMode.Hidden, step.state.mode)
        assertFalse(step.samplerWanted)
        // لا لعبة مُفعَّلة ⟹ الخدمة تنتهي حتى لو كانت اللعبة أمامية.
        assertEquals(PanelLifetime.Stop, step.lifetime)
    }

    @Test fun leavingTheArmedGameFoldsThePanelWhileTheServiceStaysArmed() {
        val inGame = poll(GamePanelState(), foreground = game).state
        val left = poll(inGame, foreground = chat)
        assertEquals(GamePanelMode.Hidden, left.state.mode)
        assertFalse(left.samplerWanted)
        // الخلل الذي أغلقته هذه الدفعة: مغادرة اللعبة كانت تُوقف الخدمة فلا تعود اللوحة لاحقًا.
        assertEquals(PanelLifetime.Continue, left.lifetime)
        assertEquals(PANEL_IDLE_POLL_MS, left.nextDelayMs)
    }

    @Test fun returningToTheArmedGameShowsTheHandleAgainWithoutAnyManagerUi() {
        val inGame = poll(GamePanelState(), foreground = game).state
        val left = poll(inGame, foreground = chat).state
        val back = poll(left, foreground = game)
        assertEquals(GamePanelMode.Handle, back.state.mode)
        assertTrue(back.samplerWanted)
    }

    @Test fun openingThenClosingHidesForTheRestOfTheSessionAndTheNextSessionShowsAHandle() {
        val handle = poll(GamePanelState(), foreground = game).state
        // ما تفعله `openPanel()` في الخدمة: لمسة على المقبض تفتح اللوحة.
        val opened = handle.copy(mode = GamePanelMode.Open, openByUser = true)
        assertEquals(GamePanelMode.Open, poll(opened, foreground = game).state.mode)
        // الإغلاق من داخل اللوحة: تُطوى لبقية الجلسة، واللعبة تبقى مُفعَّلة.
        val closed = dismissGamePanel(opened)
        val stillClosed = poll(closed, foreground = game)
        assertEquals(GamePanelMode.Hidden, stillClosed.state.mode)
        assertFalse(stillClosed.samplerWanted)
        assertTrue(stillClosed.state.enabled)
        assertEquals(PanelLifetime.Continue, stillClosed.lifetime)
        // مغادرة اللعبة ثم عودتها جلسة جديدة: المقبض يعود.
        val nextSession = poll(poll(stillClosed.state, foreground = chat).state, foreground = game)
        assertEquals(GamePanelMode.Handle, nextSession.state.mode)
    }

    @Test fun leavingWhileOpenReturnsAsAHandleNotAsAnOpenPanel() {
        val handle = poll(GamePanelState(), foreground = game).state
        val opened = handle.copy(mode = GamePanelMode.Open, openByUser = true)
        val left = poll(opened, foreground = chat).state
        val back = poll(left, foreground = game)
        // اللمسة فتحت اللوحة في تلك الجلسة فقط، فالعودة تبدأ بمقبض لا بلوحة مفتوحة.
        assertEquals(GamePanelMode.Handle, back.state.mode)
        assertFalse(back.state.openByUser)
    }

    @Test fun aScreenThatIsOffFoldsThePanelAndNeverProbesForAGame() {
        val inGame = poll(GamePanelState(), foreground = game).state
        val off = poll(inGame, foreground = game, interactive = false)
        assertEquals(GamePanelMode.Hidden, off.state.mode)
        assertFalse(off.samplerWanted)
        assertEquals(PANEL_SCREEN_OFF_POLL_MS, off.nextDelayMs)
        assertEquals(PanelLifetime.Continue, off.lifetime)
        // عند إضاءة الشاشة والعبة ما زالت أمامية: المقبض يعود.
        assertEquals(GamePanelMode.Handle, poll(off.state, foreground = game).state.mode)
    }

    @Test fun anUnreadableSampleFoldsThePanelButDoesNotDisarmTheService() {
        val inGame = poll(GamePanelState(), foreground = game).state
        val blind = poll(inGame, foreground = "")
        assertEquals(GamePanelMode.Hidden, blind.state.mode)
        assertEquals(PanelLifetime.Continue, blind.lifetime)
    }

    @Test fun disablingTheLastArmedGameStopsTheServiceEvenWhileItIsInFront() {
        val inGame = poll(GamePanelState(), foreground = game).state
        assertEquals(PanelLifetime.Stop, poll(inGame, enabled = emptySet(), foreground = game).lifetime)
    }

    @Test fun disablingOneOfTwoArmedGamesKeepsTheServiceAndTheOtherStillShows() {
        val onlySecond = setOf(otherGame)
        val disabledOne = poll(GamePanelState(), enabled = onlySecond, foreground = game)
        assertEquals(GamePanelMode.Hidden, disabledOne.state.mode)
        assertEquals(PanelLifetime.Continue, disabledOne.lifetime)
        assertEquals(GamePanelMode.Handle, poll(GamePanelState(), enabled = onlySecond, foreground = otherGame).state.mode)
    }

    @Test fun theLibraryIsReadAgainWhenTheArmedSetChangesOrWhenItAges() {
        assertTrue(panelLibraryStale(loadedFor = emptySet(), enabled = armed, ageMs = 0))
        assertFalse(panelLibraryStale(loadedFor = armed, enabled = armed, ageMs = 1_000))
        assertTrue(panelLibraryStale(loadedFor = armed, enabled = armed, ageMs = PANEL_LIBRARY_TTL_MS))
        assertTrue(panelLibraryStale(loadedFor = armed, enabled = setOf(game, otherGame), ageMs = 0))
    }

    @Test fun aCockpitLeftOpenIsPolledEverySecondAndFoldsWhenTheGameIsLeft() {
        // اللوحة المفتوحة تُستطلع كل ثانية، فمغادرة اللعبة تُطوى خلال ثانية لا بعد ثانيتين.
        val openedState = GamePanelState(
            mode = GamePanelMode.Open,
            subject = PanelSubject.Tracked(game),
            enabled = true,
            openByUser = true
        )
        val stillIn = poll(openedState, foreground = game)
        assertEquals(GamePanelMode.Open, stillIn.state.mode)
        assertEquals(PANEL_OPEN_POLL_MS, stillIn.nextDelayMs)
        val left = poll(stillIn.state, foreground = chat)
        assertEquals(GamePanelMode.Hidden, left.state.mode)
        assertEquals(PANEL_IDLE_POLL_MS, left.nextDelayMs)
    }

    @Test
    fun `a readable focus window overrides a stale resumed game`() {
        // اللعبة ما زالت «مقيمة» لكن التركيز للشاشة الرئيسية ⟹ الأمامي هو الشاشة الرئيسية.
        assertEquals("com.miui.home", preferFocusedPackage("com.tencent.ig", "com.miui.home"))
        assertEquals("com.tencent.ig", preferFocusedPackage("com.tencent.ig", "com.tencent.ig"))
    }

    @Test
    fun `an unreadable focus window never vetoes the resumed package`() {
        assertEquals("com.tencent.ig", preferFocusedPackage("com.tencent.ig", null))
        assertEquals("com.tencent.ig", preferFocusedPackage("com.tencent.ig", ""))
        assertEquals("com.tencent.ig", preferFocusedPackage("com.tencent.ig", "0.85"))
    }

    @Test
    fun `leaving the game for the launcher hides the panel`() {
        val library = setOf("com.tencent.ig")
        val enabled = setOf("com.tencent.ig")
        val inGame = panelStep(GamePanelState(), enabled, PanelSide.End, true, "com.tencent.ig", library)
        assertEquals(GamePanelMode.Handle, inGame.state.mode)
        val foreground = preferFocusedPackage("com.tencent.ig", "com.miui.home")
        val away = panelStep(inGame.state, enabled, PanelSide.End, true, foreground, library)
        assertEquals(GamePanelMode.Hidden, away.state.mode)
        val back = panelStep(away.state, enabled, PanelSide.End, true, "com.tencent.ig", library)
        assertEquals(GamePanelMode.Handle, back.state.mode)
    }
}
