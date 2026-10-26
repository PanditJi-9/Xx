/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * حرّاس اختصار «مساحة الألعاب». كلّها بنيويّة (تقرأ المصدر): ما يحرسونه سلسلةُ ربطٍ عبر ملفّات لا
 * يراها المُصرّف — مسار في `WidgetIntents` ← ترجمة في `MainActivity` ← وجهة في السجلّ ← نصوص — وانكسار
 * أيّ حلقة منها يعني أيقونة على سطح الهاتف تفتح التطبيق ولا تصل إلى اللوبي، أو تظهر بلا نصّ.
 */
class GameSpaceShortcutTest {
    private lateinit var sourceRoot: File
    private lateinit var resRoot: File

    @Before
    fun locate() {
        val found = listOf(
            File("src/main/java/nd/max"),
            File("app/src/main/java/nd/max"),
            File("../app/src/main/java/nd/max"),
            File("manager/app/src/main/java/nd/max")
        ).firstOrNull { it.isDirectory }
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", found != null)
        sourceRoot = found!!
        resRoot = File(sourceRoot, "../../../res").canonicalFile
    }

    /** الكود دون تعليقاته: تُقاس البنية لا صياغة التعليق (والتعليق هنا يشرح العطب بنفس الكلمات). */
    private fun code(path: String): String = File(sourceRoot, path).readText()
        .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("//[^\\n]*"), "")

    private fun squash(text: String) = text.replace(Regex("\\s+"), " ")

    @Test
    fun `the shortcut id and action are stable and well formed`() {
        // تغيير المعرّف يترك على سطح المستخدم أيقونة يتيمة؛ فيُجمَّد هنا ويحتاج تغييرُه قرارًا.
        assertEquals("game_space_lobby", GameSpaceShortcut.SHORTCUT_ID)
        assertTrue(Regex("[a-z0-9_]+").matches(GameSpaceShortcut.SHORTCUT_ID))
        assertTrue(GameSpaceShortcut.ACTION_OPEN_LOBBY.startsWith("nd.max."))
    }

    @Test
    fun `every open-route value is unique so one request cannot be read as another`() {
        val routes = Regex("const val (ROUTE_\\w+) = \"([^\"]+)\"")
            .findAll(code("widget/core/WidgetActions.kt"))
            .associate { it.groupValues[1] to it.groupValues[2] }
        assertTrue("ROUTE_GAME_LOBBY missing: $routes", routes.containsKey("ROUTE_GAME_LOBBY"))
        assertEquals("duplicate route values: $routes", routes.size, routes.values.toSet().size)
    }

    @Test
    fun `main activity maps the lobby route to the registered lobby destination`() {
        val activity = squash(code("MainActivity.kt"))
        assertTrue(
            "MainActivity no longer maps ROUTE_GAME_LOBBY to the lobby route",
            activity.contains("WidgetIntents.ROUTE_GAME_LOBBY -> MaxDestination.GameLobby.route")
        )
        val registry = code("ui/navigation/MaxDestinations.kt")
        assertTrue(
            "the lobby destination is not registered as gamelobby",
            Regex("data object GameLobby : MaxDestination\\(\"gamelobby\"").containsMatchIn(registry)
        )
    }

    /**
     * العطب المقيس بالتحليل: النشاط بلا `configChanges`، واللوبي يقلب الاتجاه عند الدخول ويعيده عند
     * الخروج، فيُعاد إنشاء النشاط والنيّة الأصلية ما زالت فيه. قراءة المسار في كل `onCreate` تعني أن
     * إغلاق اللوبي يعيدك إليه. فالقراءة في `onCreate` **مشروطة بإطلاق فعليّ**، و`onNewIntent` غير مشروط.
     */
    @Test
    fun `a recreated activity never replays the launch route but a new intent always does`() {
        val activity = squash(code("MainActivity.kt"))
        assertTrue(
            "onCreate must read the route only when savedInstanceState == null",
            activity.contains(
                "if (savedInstanceState == null) { " +
                    "widgetRouteState.value = intent.getStringExtra(WidgetIntents.EXTRA_OPEN_ROUTE) }"
            )
        )
        val onNewIntent = activity.substringAfter("override fun onNewIntent")
            .substringBefore("override fun onStart")
        assertTrue(onNewIntent.contains("intent.getStringExtra(WidgetIntents.EXTRA_OPEN_ROUTE)"))
        assertTrue("onNewIntent must bump the sequence", onNewIntent.contains("widgetRouteSeq.intValue++"))
        assertTrue("onNewIntent must not be guarded", !onNewIntent.contains("savedInstanceState"))
    }

    @Test
    fun `the pinner targets main activity with the action and the lobby route`() {
        val pinner = squash(code("ui/util/GameSpaceShortcutPinner.kt"))
        assertTrue(pinner.contains("Intent(context.applicationContext, MainActivity::class.java)"))
        assertTrue(pinner.contains(".setAction(GameSpaceShortcut.ACTION_OPEN_LOBBY)"))
        assertTrue(pinner.contains(".putExtra(WidgetIntents.EXTRA_OPEN_ROUTE, WidgetIntents.ROUTE_GAME_LOBBY)"))
        assertTrue(pinner.contains("ShortcutInfo.Builder(app, GameSpaceShortcut.SHORTCUT_ID)"))
    }

    @Test
    fun `the icon is drawn with the entrance emblem so the two cannot drift apart`() {
        assertTrue(squash(code("ui/util/GameSpaceShortcutPinner.kt")).contains("drawLobbyEmblem(LobbyPalette.Blue)"))
        assertTrue(code("ui/component/LobbyEntrance.kt").contains("internal fun DrawScope.drawLobbyEmblem("))
    }

    @Test
    fun `every shortcut string exists in english and arabic`() {
        val keys = Regex("R\\.string\\.(lobby_(?:head_)?shortcut\\w*)")
            .findAll(code("ui/subscreens/LobbyControlCenter.kt"))
            .map { it.groupValues[1] }
            .toSet()
        assertTrue("no shortcut strings referenced: $keys", keys.size >= 7)
        for (folder in listOf("values", "values-ar")) {
            val file = File(resRoot, "$folder/max_gaming_strings.xml")
            assertNotNull("missing $file", file.takeIf { it.isFile })
            val declared = Regex("<string name=\"([^\"]+)\"").findAll(file.readText()).map { it.groupValues[1] }.toSet()
            for (key in keys) assertTrue("$key missing from $folder", key in declared)
        }
    }

    private fun manifest(): String = File(sourceRoot, "../../../AndroidManifest.xml").canonicalFile.readText()

    private fun widgetConst(name: String): String =
        Regex("const val $name = \"([^\"]+)\"").find(code("widget/core/WidgetActions.kt"))
            ?.groupValues?.get(1) ?: error("$name not found in WidgetActions.kt")

    /**
     * اختصارات الضغط المطوّل لا تمرّ بإذن «اختصارات الشاشة الرئيسية» عند المشغّل. وحلقاتها ثلاث لا يراها
     * المُصرّف: `meta-data` على نشاط `LAUNCHER` ← `shortcuts.xml` ← نيّة مطابقة لنيّة الاختصار المثبَّت.
     */
    @Test
    fun `the static shortcut carries the same action and lobby route as the pinned one`() {
        val launcher = manifest().substringAfter("<activity-alias").substringBefore("</activity-alias>")
        assertTrue(
            "the launcher alias must declare android.app.shortcuts",
            launcher.contains("android:name=\"android.app.shortcuts\"") &&
                launcher.contains("android:resource=\"@xml/shortcuts\"")
        )
        val xml = File(resRoot, "xml/shortcuts.xml").takeIf { it.isFile }?.readText()
        assertNotNull("res/xml/shortcuts.xml is missing", xml)
        xml!!
        assertTrue(xml.contains("android:action=\"${GameSpaceShortcut.ACTION_OPEN_LOBBY}\""))
        assertTrue(xml.contains("android:targetClass=\"nd.max.MainActivity\""))
        assertTrue(xml.contains("android:targetPackage=\"nd.max\""))
        assertTrue(
            "the static shortcut must carry the open-route extra",
            Regex(
                "<extra\\s+android:name=\"${Regex.escape(widgetConst("EXTRA_OPEN_ROUTE"))}\"\\s+" +
                    "android:value=\"${Regex.escape(widgetConst("ROUTE_GAME_LOBBY"))}\""
            ).containsMatchIn(xml)
        )
        for (label in listOf("shortcutShortLabel", "shortcutLongLabel")) {
            assertTrue("$label must use a string resource", xml.contains("android:$label=\"@string/game_lobby_title\""))
        }
    }

    /**
     * الأيقونة المستقلّة: مدخل `LAUNCHER` مُعطَّل افتراضيًّا (لا تظهر إلا بطلب المستخدم)، شفّاف، يُطلق النيّة
     * نفسها التي يطلقها الاختصار المثبَّت ثم ينتهي في `onCreate` (شرط `Theme.NoDisplay`).
     */
    @Test
    fun `the standalone lobby icon is an opt-in transparent launcher entry`() {
        val block = manifest().substringAfter("android:name=\".GameLobbyEntryActivity\"").substringBefore("</activity>")
        assertTrue("must be disabled by default", block.contains("android:enabled=\"false\""))
        assertTrue("must be exported for the launcher", block.contains("android:exported=\"true\""))
        assertTrue("must be a LAUNCHER entry", block.contains("android.intent.category.LAUNCHER"))
        assertTrue("must be transparent", block.contains("@android:style/Theme.NoDisplay"))
        assertTrue("must not leave an empty task behind", block.contains("android:excludeFromRecents=\"true\""))
        val entry = squash(code("GameLobbyEntryActivity.kt"))
        assertTrue(entry.contains("startActivity(GameSpaceShortcutPinner.lobbyIntent(this))"))
        assertTrue("NoDisplay must finish before onResume", entry.contains("finish()"))
        val icon = squash(code("ui/util/GameLobbyIcon.kt"))
        assertTrue(icon.contains("GameLobbyEntryActivity::class.java"))
        assertTrue(icon.contains("PackageManager.DONT_KILL_APP"))
        for (res in listOf("mipmap-anydpi-v26/ic_game_lobby.xml", "drawable/ic_game_lobby_background.xml", "drawable/ic_game_lobby_foreground.xml")) {
            assertTrue("$res is missing", File(resRoot, res).isFile)
        }
    }
}
