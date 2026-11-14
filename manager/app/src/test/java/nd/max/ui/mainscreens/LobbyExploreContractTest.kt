/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.mainscreens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        val controlBack = text.indexOf("BackHandler(enabled = spineIndex > 0)")
        val exploreBack = text.indexOf("BackHandler(enabled = exploreOpen)")
        assertTrue("Spine back handler is missing", controlBack >= 0)
        assertTrue("Explore back handler is missing", exploreBack >= 0)
        assertTrue("Explore back handler must be composed last so it wins", exploreBack > controlBack)
    }

    @Test
    fun `the lobby is one spine, not a hidden settings page`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        val text = lobby()

        assertFalse("The hidden vertical pager must not return", text.contains("VerticalPager("))
        assertFalse("The settings page state must not return", text.contains("rememberPagerState("))
        assertTrue("The fixed spine is missing", text.contains("GameSpaceRail("))
        assertTrue("The section bodies are not composed in the spine", text.contains("LobbySectionContent("))
        assertTrue("The spine is not scroll-spied", text.contains("verticalScroll(homeScroll)"))
        assertTrue("The active section is not derived from the scroll", text.contains("spineIndex by remember"))
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
    fun `every sessions door in the lobby is single top`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        val text = lobby()

        // البوابة على **الفعل** لا على عدده: كل نداء يفتح سجلّ الجلسات يجب أن يكون single-top،
        // فإضافة بابٍ مشروع جديد (قاعدة المسرح، مركز التحكّم، الشريط العلوي، لوحة الأوامر) لا
        // تُسقط العقد، وبابٌ جديد ينسى `launchSingleTop` يُسقطه. والعدّ الثابت كان **أضعف** من
        // الشرط الذي يحرسه: بابٌ خامس غير single-top لم يكن يُغيّر العدّ فيمرّ.
        val doors = Regex(
            "navigate\\(MaxDestination\\.GameSessions\\.route\\)\\s*\\{([^}]*)\\}"
        ).findAll(text).toList()

        assertTrue("The lobby has no door to the sessions log", doors.isNotEmpty())
        doors.forEachIndexed { index, door ->
            assertTrue(
                "Sessions door #${index + 1} is not single-top: a second tap would stack the screen",
                door.groupValues[1].contains("launchSingleTop = true")
            )
        }

        // وكل سطح يعلن الباب يظلّ يعلنه — لا يُحذف بابٌ بصمت إصلاحًا لعدّ.
        listOf(
            "onOpenSessions" to "the stage base",
            "onOpenHistory" to "the control centre",
            "PaletteActionId.Sessions" to "the command palette",
            "R.string.game_sessions_lobby_open" to "the top bar",
        ).forEach { (needle, surface) ->
            assertTrue("The sessions door disappeared from $surface", text.contains(needle))
        }
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
            "A most-played row must be one button node, not a clickable with unlabeled children",
            panel.contains(".semantics(mergeDescendants = true)") &&
                panel.contains(".clickable(role = Role.Button, onClick = onClick)")
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

    @Test
    fun `an empty result names its filter instead of claiming an empty library`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        val exploreText = explore()

        assertTrue(
            "A filtered empty result must name its own filter",
            exploreText.contains("R.string.lobby_explore_empty_recent") &&
                exploreText.contains("R.string.lobby_explore_empty_played") &&
                exploreText.contains("R.string.lobby_explore_empty_unplayed")
        )
        assertTrue(
            "A filter-only empty result must offer a filter reset",
            exploreText.contains("R.string.lobby_explore_show_all") &&
                exploreText.contains("onClick = { filter = LobbyFilter.All }")
        )
        assertTrue(
            "The search hint must stay bound to a real query",
            exploreText.contains("if (query.isNotBlank())")
        )
    }

    @Test
    fun `one play line serves the stage and the explore card`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)

        assertTrue(
            "The explore card must read the shared play line, and the lobby must not grow a copy",
            explore().contains("lobbyPlayLine(stats)") &&
                !lobby().contains("private fun playHistoryText")
        )
        assertTrue(
            "The explore card must read the same play line",
            explore().contains("lobbyPlayLine(stats)")
        )
        assertTrue(
            "The shared play line must live with the insights panel",
            insights().contains("internal fun lobbyPlayLine(")
        )
    }

    @Test
    fun `a short landscape screen compresses the header instead of the cards`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        val text = explore()

        assertTrue(
            "Explore must measure its height before laying the header out",
            text.contains("val compact = maxHeight < ExploreCompactHeight") &&
                text.contains("private val ExploreCompactHeight")
        )
        assertTrue(
            "The compact header must drop the count line and keep the light page title",
            text.contains("if (!compact)") &&
                text.contains("fontSize = if (compact)") &&
                text.contains("fontWeight = FontWeight.Light")
        )
        assertTrue(
            "The compact card must shrink its padding and skip the redundant dots",
            text.contains("compact = compact") &&
                text.contains("padding(if (compact) MaxSpace.md else MaxSpace.lg)") &&
                text.contains("if (!compact) {")
        )
    }

    @Test
    fun `the remote can reach the search field and come back to the grid`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        val text = explore()

        assertTrue(
            "The search field must own a focus requester",
            text.contains("val searchFocus = remember { FocusRequester() }") &&
                text.contains(".focusRequester(searchFocus)")
        )
        assertTrue(
            "Up from the top row must hand focus to search instead of clamping",
            text.contains("if (cursor < columns) {") &&
                text.contains("searchFocus.requestFocus()")
        )
    }

    @Test
    fun `the overlay root is not a phantom button and the chips can wrap`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        val text = explore()

        assertTrue(
            "Empty taps must be swallowed without publishing button semantics",
            text.contains("detectTapGestures") && text.contains(".pointerInput(Unit)")
        )
        assertFalse(
            "The overlay root must not declare an unlabeled empty click",
            text.contains("onClick = {}")
        )
        assertTrue(
            "Meta chips must wrap instead of clipping at large font scale",
            text.contains("FlowRow(") && text.contains("verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)")
        )
    }

    @Test
    fun `the grid scrolls only when the cursor leaves the viewport`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        val text = explore()

        assertTrue(
            "The grid must know whether the cursor is already visible",
            text.contains("visibleItemsInfo.any { it.index == cursor }")
        )
        assertTrue(
            "A visible cursor must not force a scroll on a horizontal step",
            text.contains("if (!onScreen) gridState.animateScrollToItem(cursor)")
        )
    }

    @Test
    fun `the bumpers cycle the filters so the remote can reach them`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        val text = explore()

        assertTrue(
            "L1/R1 must cycle the filter row",
            text.contains("Key.ButtonR1, Key.ButtonL1 ->") &&
                text.contains("if (event.key == Key.ButtonR1) 1 else -1")
        )
        assertTrue(
            "The filter cycle must wrap around the five filters",
            text.contains("filters.indexOf(filter) + step + filters.size")
        )
    }

    @Test
    fun `each card is one button for the screen reader`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        val text = explore()

        assertTrue(
            "The card must merge its icon, name, play line and chips into one node",
            text.contains("mergeDescendants = true")
        )
        assertTrue(
            "Opening a game from the card must be announced as a button",
            text.contains("role = Role.Button")
        )
    }

    @Test
    fun `insights rows are single buttons too`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        val text = insights()

        assertTrue(
            "Most-played and setup rows must merge their text for the screen reader",
            text.contains("mergeDescendants = true")
        )
        assertTrue(
            "Clickable insights rows must declare the button role",
            text.contains("role = Role.Button")
        )
    }
}
