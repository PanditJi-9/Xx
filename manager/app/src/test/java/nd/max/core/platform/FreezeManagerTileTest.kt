/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.platform

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.w3c.dom.Element

/** Source-contract guards, not a claim that a ROM's shade or keyguard was tested. */
class FreezeManagerTileTest {
    private lateinit var sourceRoot: File
    private lateinit var resRoot: File

    @Before
    fun locateSources() {
        sourceRoot = checkNotNull(listOf(
            File("src/main/java/nd/max"),
            File("app/src/main/java/nd/max"),
            File("../app/src/main/java/nd/max"),
            File("manager/app/src/main/java/nd/max")
        ).firstOrNull { it.isDirectory }) { "Cannot locate sources; tile contract was not evaluated" }
        resRoot = File(sourceRoot, "../../../res").canonicalFile
    }

    private fun code(path: String): String = File(sourceRoot, path).readText()
        .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("//[^\\n]*"), "")
        .replace(Regex("\\s+"), " ")

    private fun tileCode() = code("TileService/FreezeManagerTileService.kt")

    @Test
    fun the_tile_is_registered_once_and_protected_by_the_system_binding_permission() {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val manifest = factory.newDocumentBuilder().parse(File(sourceRoot, "../../../AndroidManifest.xml"))
        val services = manifest.getElementsByTagName("service")
        val android = "http://schemas.android.com/apk/res/android"
        val matches = (0 until services.length).map { services.item(it) as Element }
            .filter { it.getAttributeNS(android, "name") == ".TileService.FreezeManagerTileService" }
        assertEquals(1, matches.size)
        val service = matches.single()
        assertEquals("android.permission.BIND_QUICK_SETTINGS_TILE", service.getAttributeNS(android, "permission"))
        assertEquals("true", service.getAttributeNS(android, "exported"))
        assertEquals("@string/tile_freeze_manager", service.getAttributeNS(android, "label"))
        assertEquals("@drawable/widget_ic_freeze", service.getAttributeNS(android, "icon"))
        val actions = service.getElementsByTagName("action")
        assertEquals(1, actions.length)
        assertEquals("android.service.quicksettings.action.QS_TILE", (actions.item(0) as Element).getAttributeNS(android, "name"))
        assertTrue(File(resRoot, "drawable/widget_ic_freeze.xml").isFile)
    }

    @Test
    fun the_launch_is_explicit_and_uses_the_existing_freezer_route() {
        val tile = tileCode()
        assertTrue(tile.contains("Intent(this, MainActivity::class.java)"))
        assertTrue(tile.contains(".putExtra(WidgetIntents.EXTRA_OPEN_ROUTE, WidgetIntents.ROUTE_APP_FREEZER)"))
        assertTrue(tile.contains(".setAction(ACTION_OPEN_FREEZER)"))
        assertTrue(tile.contains("const val ACTION_OPEN_FREEZER = \"nd.max.action.OPEN_FREEZE_MANAGER\""))
        assertTrue(tile.contains("Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP"))
        assertFalse(tile.contains("FLAG_ACTIVITY_CLEAR_TASK"))
    }

    @Test
    fun android_14_uses_an_immutable_pending_intent_and_older_versions_keep_the_intent_overload() {
        val tile = tileCode()
        assertTrue(tile.contains("Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE"))
        assertTrue(tile.contains("startActivityAndCollapse( PendingIntent.getActivity("))
        assertTrue(tile.contains("PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT"))
        assertTrue(tile.contains("startActivityAndCollapse(intent)"))
        assertFalse(tile.contains("FLAG_MUTABLE"))
    }

    @Test
    fun the_tile_unlocks_before_opening_and_never_mutates_a_package_or_invents_a_frozen_state() {
        val tile = tileCode()
        assertTrue(tile.contains("if (isLocked) { unlockAndRun { openManager() } } else { openManager() }"))
        assertTrue(tile.contains("tile.state = Tile.STATE_INACTIVE"))
        assertFalse(tile.contains("Tile.STATE_ACTIVE"))
        for (forbidden in listOf("DebloatFreezeUtil", "toggleFreeze(", "debloatApp(", "PrivilegedShell", "Shizuku", "Runtime.getRuntime", "Shell.")) {
            assertFalse("Tile must only open the manager: $forbidden", tile.contains(forbidden))
        }
        assertTrue(tile.contains(".onFailure {"))
        assertTrue(tile.contains("tile.subtitle = getString(R.string.tile_freeze_open_failed)"))
    }

    @Test
    fun cold_repeated_and_long_press_launches_reach_the_same_existing_destination() {
        val activity = code("MainActivity.kt")
        assertTrue(activity.contains("if (savedInstanceState == null) { widgetRouteState.value = intent.getStringExtra(WidgetIntents.EXTRA_OPEN_ROUTE) }"))
        assertTrue(activity.contains("if (savedInstanceState == null && widgetRouteState.value == null) { widgetRouteState.value = freezeTilePreferencesRoute(intent) }"))
        val newIntent = activity.substringAfter("override fun onNewIntent").substringBefore("private fun freezeTilePreferencesRoute")
        assertTrue(newIntent.contains("?: freezeTilePreferencesRoute(intent) ?: return"))
        assertTrue(newIntent.contains("widgetRouteSeq.intValue++"))
        val preferences = activity.substringAfter("private fun freezeTilePreferencesRoute").substringBefore("override fun onStart")
        assertTrue(preferences.contains("incoming.action != android.service.quicksettings.TileService.ACTION_QS_TILE_PREFERENCES"))
        assertTrue(preferences.contains("component?.className == FreezeManagerTileService::class.java.name"))
        assertTrue(preferences.contains("WidgetIntents.ROUTE_APP_FREEZER"))
        assertTrue(activity.contains("WidgetIntents.ROUTE_APP_FREEZER -> MaxDestination.DebloatFreeze.route"))
        assertTrue(code("ui/navigation/MaxNavGraph.kt").contains("composable(MaxDestination.DebloatFreeze.route) { DebloatFreezeScreen(navController) }"))
        // The entrance must preserve the existing consent gate, not implement another freeze path.
        val screen = code("ui/subscreens/DebloatFreezeScreen.kt")
        assertTrue(screen.contains("id = DangerConsent.PACKAGE_FREEZE"))
        assertTrue(screen.contains("if (app.isEnabled) freezeGate() else viewModel.toggleFreeze(context, app)"))
    }

    @Test
    fun all_tile_labels_and_failure_copy_exist_in_english_and_arabic() {
        val keys = setOf("tile_freeze_manager", "tile_freeze_choose_app", "tile_freeze_open_failed")
        for (locale in listOf("values", "values-ar")) {
            val strings = File(resRoot, "$locale/strings.xml").readText()
            for (key in keys) assertTrue("Missing $key in $locale", strings.contains("<string name=\"$key\">"))
        }
    }
}
