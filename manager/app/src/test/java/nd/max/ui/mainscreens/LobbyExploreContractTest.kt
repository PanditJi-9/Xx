/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.mainscreens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * حارس عقد «استكشف» و«الرؤى» — فحص نصّي على المصدر، لا حكمًا على الشكل المعروض.
 *
 * يثبت ثلاثة أشياء يسهل نقضها لاحقًا بلا اختبار: أن الطبقة تُركَّب فعلًا فوق المسرح، وأن أوّل رجوع
 * يغلقها قبل أي تنقّل، وأن الرؤى صارت قسمًا سادسًا في مركز التحكّم — مع بقاء باب السجل single-top.
 */
class LobbyExploreContractTest {

    private fun sourceRoot(): File? = listOf(
        File("src/main/java/nd/max"),
        File("app/src/main/java/nd/max"),
        File("../app/src/main/java/nd/max"),
        File("manager/app/src/main/java/nd/max"),
    ).firstOrNull { it.isDirectory }

    private fun lobby(): String = File(sourceRoot(), "ui/subscreens/GameLobbyScreen.kt").readText()

    private fun control(): String = File(sourceRoot(), "ui/subscreens/LobbyControlCenter.kt").readText()

    private fun explore(): String = File(sourceRoot(), "ui/subscreens/LobbyExplore.kt").readText()

    private fun insights(): String = File(sourceRoot(), "ui/subscreens/LobbyInsightsPanel.kt").readText()

    @Test
    fun `explore is composed over the stage and the first back closes it`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        val text = lobby()

        assertTrue("Explore overlay is not composed", text.contains("LobbyExplore("))
        assertTrue("Explore overlay is not animated", text.contains("AnimatedVisibility("))

        val controlBack = text.indexOf("BackHandler(enabled = rootState.currentPage == 1)")
        val exploreBack = text.indexOf("BackHandler(enabled = exploreOpen)")
        assertTrue("Control back handler is missing", controlBack >= 0)
        assertTrue("Explore back handler is missing", exploreBack >= 0)
        assertTrue("Explore back handler must be composed last so it wins", exploreBack > controlBack)
    }

    @Test
    fun `insights is a sixth section with its own body`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        val text = control()

        assertTrue(
            "Insights section is missing from the sidebar enum",
            text.contains("Insights(R.string.lobby_insights_title, Icons.Rounded.Insights)")
        )
        assertTrue("Insights body is not rendered", text.contains("LobbyInsightsBody("))
        assertTrue(
            "Short screens must get the two-column sidebar tiles",
            text.contains("LobbySection.entries.chunked(2)")
        )
    }

    @Test
    fun `explore and insights keep every visible string in resources`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        listOf("LobbyExplore.kt" to explore(), "LobbyInsightsPanel.kt" to insights()).forEach { (name, text) ->
            assertEquals(
                "$name: visible copy leaked as a literal",
                0,
                Regex("Text\\(\\s*\"").findAll(text).count()
            )
            assertTrue("$name: no string resources used", text.contains("stringResource("))
        }
    }

    @Test
    fun `the sessions door stays single top from both lobby entries`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        val text = lobby()

        assertEquals(
            "The lobby must open the sessions log single-top from the top bar and Insights",
            2,
            Regex("launchSingleTop = true").findAll(text).count()
        )
    }

    @Test
    fun `explore answers the remote and gives the stage its focus back`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        val exploreText = explore()

        assertTrue("Explore does not listen to keys", exploreText.contains("onKeyEvent"))
        assertTrue("Explore does not own a focus requester", exploreText.contains("focusRequester(focus)"))
        assertTrue("Arrow navigation is missing", exploreText.contains("Key.DirectionRight"))
        assertTrue("Grid row movement is missing", exploreText.contains("Key.DirectionDown"))
        assertTrue(
            "Selecting from the pad is missing",
            exploreText.contains("Key.DirectionCenter, Key.Enter, Key.ButtonA")
        )
        assertTrue("Escape does not close Explore", exploreText.contains("Key.Escape"))
        assertTrue(
            "The grid must follow the remote cursor or the focused card leaves the viewport",
            exploreText.contains("rememberLazyGridState()") &&
                exploreText.contains("state = gridState") &&
                exploreText.contains("gridState.animateScrollToItem(cursor)")
        )
        assertTrue(
            "The focus ring must appear only after the remote moved, not for touch users",
            exploreText.contains("focused = cursorActive && index == cursor")
        )
        assertTrue(
            "Explore must open on the game the stage already shows",
            exploreText.contains("visible.indexOfFirst { it.packageName == currentPackage }")
        )
    }

    @Test
    fun `most played rows open their game through one shared selector`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        val panel = insights()

        assertTrue(
            "A most-played row must be clickable",
            panel.contains(".clickable(onClick = onClick)")
        )
        assertTrue(
            "A most-played row must open its game, not just display it",
            panel.contains("onOpenGame(stat.packageName)")
        )
        assertTrue(
            "The control center must pass the opener down to Insights",
            control().contains("onOpenGame = onOpenGame")
        )
        assertTrue(
            "The lobby must route Insights through the shared game selector",
            lobby().contains("onOpenGame = { pkg ->")
        )
        assertTrue(
            "The armed counter must intersect the library, or removed games overcount",
            panel.contains("armed intersect library") &&
                panel.contains("libraryArmed.size") &&
                panel.contains("app.packageName in libraryArmed")
        )
        assertTrue(
            "The most-played value must follow the stage tone, not a fixed red",
            panel.contains("accent = accent") &&
                !panel.contains("accent = LobbyPalette.Red")
        )
    }
}
