/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.component

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * **الوضع المسمّى يجب أن يُرى لا أن يُلوَّن فقط.**
 *
 * `ProfileStrip` كُتب في `GamePanelSurface.kt` وظلّ بلا مستدعٍ، فكانت اللوحة تعرض الوضع الحالي
 * لونًا (`ModeRed`/`ModeAmber`/`ModeGreen`) بلا اسم ولا مالك. هذا الحارس يمنع عودة القطعة إلى
 * «مكتوبة وغير موصولة»: يعدّ الاستدعاءات في التخطيطين، ويتحقّق أنها تقرأ الحالة نفسها التي
 * يكتبها المقبض الواحد.
 *
 * وحدّه المُعلَن: فحص نصّي على التوصيل، لا حكمًا على الشكل — الشكل على 320/360dp يحتاج جهازًا.
 */
class ProfileStripWiringTest {
    private lateinit var sourceRoot: File

    @Before
    fun locate() {
        val found = listOf(
            File("src/main/java/nd/max"), File("app/src/main/java/nd/max"),
            File("../app/src/main/java/nd/max"), File("manager/app/src/main/java/nd/max")
        ).firstOrNull { it.isDirectory }
        assumeTrue("Cannot locate nd.max sources", found != null)
        sourceRoot = found!!
    }

    private fun text(relative: String) = File(sourceRoot, relative).readText()

    @Test
    fun `both cockpit layouts reach the mode strip`() {
        val cockpit = text("ui/component/GamePanelCockpit.kt")
        val calls = Regex("ProfileStrip\\(").findAll(cockpit).count()
        assertTrue("the strip must be reachable from both layouts, found $calls", calls >= 2)
        assertTrue(cockpit.contains("selected = controls.profile"))
        assertTrue(cockpit.contains("auto = controls.auto"))
        assertTrue(cockpit.contains("onSelect = onSelectProfile"))
    }

    @Test
    fun `the strip names the same three values the control state reads`() {
        val strip = text("ui/component/GamePanelSurface.kt")
        assertTrue(strip.contains("\"1\" to R.string.profile_performance"))
        assertTrue(strip.contains("\"2\" to R.string.profile_balanced"))
        assertTrue(strip.contains("\"3\" to R.string.profile_powersave"))
        assertTrue("auto must say who owns the file", strip.contains("R.string.str_auto_mode"))
        val controls = text("core/gamespace/PanelControls.kt")
        assertTrue(
            "the same three ids are the ones read back from the file",
            controls.contains("takeIf { it in setOf(\"1\", \"2\", \"3\") }")
        )
    }

    @Test
    fun `the write goes through the one applier and refuses when the AI owns the profile`() {
        val controls = text("core/gamespace/PanelControls.kt")
        assertTrue(controls.contains("return ProfileApplier.apply(id)"))
        assertTrue(controls.contains("if (autoKnown ?: read().auto) return false"))
        val service = text("service/GamePanelService.kt")
        assertTrue(
            "the panel writes through PanelControls, not a second path",
            service.contains("PanelControls.applyProfile(id, autoKnown = false)")
        )
        assertTrue(
            "and re-reads so a refusal is shown, not hidden",
            service.contains("controls = PanelControls.read()")
        )
    }
}
