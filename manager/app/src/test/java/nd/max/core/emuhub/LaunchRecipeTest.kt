/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.core.emuhub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * وصفة التشغيل — الجزء النقيّ وحده يُقاس هنا (‏`LaunchExecutor` يمسّ Android فلا يدخل JVM).
 *
 * وقاعدة الاختبارات: كل دعوى تقيس **حدًّا مُعلنًا** في `LaunchRecipe.kt` (قائمة مغلقة، نقص يُعلَن،
 * وصفة واحدة مضمونة). وما لا تستطيع هذه البيئة قياسه — الإطلاق الحقيقيّ على جهاز — **لا يُدّعى**.
 */
class LaunchRecipeTest {

    private val values = mapOf(
        LaunchVariable.ROM_PATH to "content://roms/%D8%B9%D8%A8%D8%A9.cue",
        LaunchVariable.ROM_DIR to "content://roms",
        LaunchVariable.ROM_NAME to "لعبة.cue",
        LaunchVariable.ROM_NAME_NO_EXT to "لعبة",
        LaunchVariable.PACKAGE to "com.retroarch.aarch64",
    )

    // ───────────────────────────── حلّ القوالب ─────────────────────────────

    @Test fun theRomPathTokenIsSubstitutedVerbatim() {
        val recipe = LaunchRecipe(
            id = "t",
            packageName = "pkg",
            extras = mapOf("ROM" to "{ROM_PATH}", "DIR" to "{ROM_DIR}"),
        )
        val ready = recipe.resolve(values) as Resolution.Ready
        assertEquals(values.getValue(LaunchVariable.ROM_PATH), ready.extras.getValue("ROM"))
        assertEquals("content://roms", ready.extras.getValue("DIR"))
    }

    @Test fun everyDeclaredVariableHasATokenAndEveryTokenResolves() {
        val template = LaunchVariable.entries.joinToString(" ") { it.token }
        val recipe = LaunchRecipe(id = "t", packageName = "pkg", extras = mapOf("ALL" to template))
        val ready = recipe.resolve(values) as Resolution.Ready
        val resolved = ready.extras.getValue("ALL")
        LaunchVariable.entries.forEach { variable ->
            assertTrue("${variable.token} لم يُحلّ", !resolved.contains(variable.token))
        }
        assertEquals(values.getValue(LaunchVariable.PACKAGE), resolved.substringAfterLast(' '))
    }

    @Test fun aMissingValueIsNamedNotSentLiterally() {
        val recipe = LaunchRecipe(id = "t", packageName = "pkg", extras = mapOf("CORE" to "{CORE_PATH}"))
        assertEquals(Resolution.Missing("{CORE_PATH}"), recipe.resolve(values))
    }

    @Test fun anUnknownBraceTokenIsRefusedRatherThanForwarded() {
        val recipe = LaunchRecipe(id = "t", packageName = "pkg", extras = mapOf("X" to "{SOMETHING_ELSE}"))
        assertTrue(recipe.resolve(values) is Resolution.Missing)
    }

    // ───────────────────────────── الوصفة المُدرجة ─────────────────────────────

    @Test fun retroArchServesEverySystemAndCarriesTheRomExtra() {
        val recipe = LaunchRecipes.retroArch
        assertEquals("com.retroarch.aarch64", recipe.packageName)
        assertEquals("com.retroarch.browser.retroactivity.RetroActivityFuture", recipe.activityName)
        assertEquals("{ROM_PATH}", recipe.extras.getValue("ROM"))
        assertEquals(RomSystem.entries.toSet(), recipe.systems)
        assertEquals(RecipeSource.BUILTIN, recipe.source)
    }

    @Test fun theBuiltinIsTheOnlyDeclaredRecipeAndItIsSelectedBySystem() {
        assertEquals(listOf(LaunchRecipes.retroArch), LaunchRecipes.builtins)
        for (system in RomSystem.entries) {
            assertEquals(LaunchRecipes.retroArch, LaunchRecipes.forSystem(system))
        }
    }

    @Test fun anUnknownSystemGetsNoRecipeInsteadOfAGuess() {
        assertNull(LaunchRecipes.forSystem(null))
    }
}
