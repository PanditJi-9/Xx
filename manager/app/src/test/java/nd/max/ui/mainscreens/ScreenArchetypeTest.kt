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
}
