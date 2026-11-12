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
 * **كل شاشة رئيسية تسمّي نوعها قبل أن تُخطَّط.**
 *
 * `DESIGN.md` §Screen Composition يقرّر أن الكائن يقرّر الشكل، ويشترط تسمية النوع
 * قبل أول خطوة. وهذا الحارس يمنع أن يعود التخطيط إلى «قالب واحد لكل شيء»: شاشة بلا
 * نوع مسمّى هي شاشة تُنسخ من جارتها لا من كائنها.
 *
 * وحدّه المُعلَن: فحص نصّي على البطاقة، لا حكمًا على الشكل المعروض — الشكل يحتاج جهازًا.
 */
class ScreenArchetypeTest {

    /** شاشة M2: النوع المقصود، وحاملها الواحد، والملفّ الذي يستدعيه. */
    private data class M2Screen(
        val path: String,
        val archetype: String,
        val carrierCall: String,
        val carrierPath: String = path,
    )

    /** الأنواع السبعة كما في `DESIGN.md` §Screen Composition. */
    private val archetypes = setOf(
        "Instrument", "Ledger", "Console", "Matrix", "Studio", "Flow", "Library",
    )

    private val screens = listOf(
        "HomeScreen.kt",
        "ControlScreen.kt",
        "SettingsScreen.kt",
        "ApplistScreen.kt",
        "MaxAiScreen.kt",
        "DiagnosticsScreen.kt",
    )

    /** شاشات M2 الخمس كما في `PRODUCT-MAP.md` §٥ — النوع والحامل الواحد. */
    private val m2Screens = listOf(
        M2Screen(
            path = "ui/mainscreens/HomeScreen.kt",
            archetype = "Matrix",
            carrierCall = "CommandDeck",
            carrierPath = "ui/mainscreens/LegendaryHomeDashboard.kt",
        ),
        M2Screen("ui/mainscreens/ControlScreen.kt", "Matrix", "MaxListScreen"),
        M2Screen("ui/subscreens/FpsOverlayScreen.kt", "Instrument", "HudSurface"),
        M2Screen("ui/subscreens/GameSessionsScreen.kt", "Ledger", "SessionChartSection"),
        M2Screen("ui/subscreens/ThermalDetailScreen.kt", "Instrument", "ThermalTrend"),
    )

    private fun sourceRoot(): File? = listOf(
        File("src/main/java/nd/max"),
        File("app/src/main/java/nd/max"),
        File("../app/src/main/java/nd/max"),
        File("manager/app/src/main/java/nd/max"),
    ).firstOrNull { it.isDirectory }

    @Test
    fun `every primary screen names one of the seven archetypes`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        val missing = mutableListOf<String>()
        val unknown = mutableListOf<String>()
        screens.forEach { name ->
            val file = File(root, "ui/mainscreens/$name")
            if (!file.isFile) {
                missing += "$name (الملف غير موجود)"
                return@forEach
            }
            val text = file.readText()
            val archetype = Regex("archetype: (\\w+)").find(text)?.groupValues?.get(1)
            when {
                archetype == null -> missing += "$name (بلا بطاقة نوع)"
                archetype !in archetypes -> unknown += "$name → $archetype"
            }
        }
        assertEquals("شاشات بلا نوع مسمّى: $missing", emptyList<String>(), missing)
        assertEquals("أنواع ليست من السبعة في DESIGN.md: $unknown", emptyList<String>(), unknown)
    }

    @Test
    fun `the archetype is declared before the composable it describes`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        screens.forEach { name ->
            val text = File(root, "ui/mainscreens/$name").readText()
            val card = text.indexOf("archetype:")
            val screen = text.indexOf("fun ${name.removeSuffix(".kt")}(")
            assertTrue(
                "$name: البطاقة بعد الشاشة أو غائبة",
                card >= 0 && screen > card,
            )
        }
    }

    @Test
    fun `M2 screens name their intended archetype and render one carrier`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)

        m2Screens.forEach { screen ->
            val screenText = File(root, screen.path).readText()
            val archetype = Regex("archetype: (\\w+)").find(screenText)?.groupValues?.get(1)
            assertEquals("${screen.path}: نوع M2 تغيّر", screen.archetype, archetype)

            val carrierText = File(root, screen.carrierPath).readText()
            val calls = Regex("(?m)^\\s*${Regex.escape(screen.carrierCall)}\\(")
                .findAll(carrierText)
                .count()
            assertEquals(
                "${screen.path}: الحامل ${screen.carrierCall} يجب أن يُستدعى مرة واحدة",
                1,
                calls,
            )
        }
    }

    @Test
    fun `M2 carriers lead their dependent rows and static blocks`() {
        val root = sourceRoot()
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)

        // ترتيب `Now` محفوظ كما اعتمده المالك (`HomeShapeContractTest`): الهوية أولًا،
        // فمنصة Matrix لا تتقدّم البطل. وهذا الحارس يمنع أن «تُقدَّم» المنصة لاحقًا باسم M2.
        val now = File(root, "ui/mainscreens/LegendaryHomeDashboard.kt").readText()
        assertTrue(
            "Now: منصة Matrix تقدّمت بطاقة الهوية المعتمدة",
            callIndex(now, "HomeHeroCard") < callIndex(now, "CommandDeck"),
        )

        val fps = File(root, "ui/subscreens/FpsOverlayScreen.kt").readText()
        assertTrue(
            "FpsOverlay: أدوات الضبط تسبق الآلة",
            callIndex(fps, "HudSurface") < callIndex(fps, "MaxSwitchRow"),
        )

        val sessions = File(root, "ui/subscreens/GameSessionsScreen.kt").readText()
        assertTrue(
            "GameSessions: المنحنى عاد داخل صفّ جلسة",
            callIndex(sessions, "SessionChartSection") < sessions.indexOf("sessions.forEach"),
        )

        val thermal = File(root, "ui/subscreens/ThermalDetailScreen.kt").readText()
        assertTrue(
            "ThermalDetail: الصفوف تسبق منحنى الآلة",
            callIndex(thermal, "ThermalTrend") < callIndex(thermal, "MaxRow"),
        )
    }

    private fun callIndex(source: String, call: String): Int =
        Regex("(?m)^\\s*${Regex.escape(call)}\\(").find(source)?.range?.first ?: Int.MAX_VALUE
}
