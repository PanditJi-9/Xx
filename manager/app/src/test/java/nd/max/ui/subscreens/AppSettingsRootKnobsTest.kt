/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.subscreens

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * شاشة بروفايل اللعبة في الوضعين غير الجذريين: الخيارات التي لا يطبّقها غير رفيق الجذر **تُعطَّل** بدل أن تُحفظ
 * وتبدو فعّالة. والثلاثة التي يطبّقها `InAppProfileApplier` تبقى فعّالة. هذا الحارس يمنع أن يُضاف صفّ جذريّ
 * جديد بلا بوّابة، أو أن يُعطَّل بالخطأ أحد الثلاثة.
 */
class AppSettingsRootKnobsTest {
    private val inAppFields = setOf("refresh_rate", "haptic_feedback", "dnd_on_gaming")

    private fun screen(): String {
        val root = listOf(
            File("src/main/java/nd/max"), File("app/src/main/java/nd/max"),
            File("../app/src/main/java/nd/max"), File("manager/app/src/main/java/nd/max")
        ).firstOrNull { it.isDirectory }
        assumeTrue("Cannot locate nd.max sources", root != null)
        return File(root, "ui/subscreens/AppSettingsScreen.kt").readText()
    }

    private val field = Regex("updateSetting\\(it, \"(\\w+)\"")

    @Test
    fun `every profile row that edits a root-only field is gated by rootKnobs`() {
        val text = screen()
        val offenders = Regex("enabled = localMasterOn,\\n").findAll(text).mapNotNull { match ->
            val name = field.find(text.substring(match.range.first, minOf(text.length, match.range.first + 1_800)))
                ?.groupValues?.get(1) ?: return@mapNotNull null
            name.takeIf { it !in inAppFields }
        }.toList()
        assertTrue(
            "root-only profile rows must use `enabled = localMasterOn && rootKnobs`: $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun `the three in-app fields stay editable in every mode`() {
        val text = screen()
        // لكل صفّ مُعطَّل بالبوّابة: أوّل حقل يحرّره هو الذي يُحسب (لا أيّ حقل لاحق في المدى).
        val gatedFields = Regex("enabled = localMasterOn && rootKnobs,\\n").findAll(text).mapNotNull { match ->
            field.find(text.substring(match.range.first, minOf(text.length, match.range.first + 1_800)))
                ?.groupValues?.get(1)
        }.toSet()
        assertTrue("these must never be gated: ${gatedFields intersect inAppFields}", (gatedFields intersect inAppFields).isEmpty())
        assertTrue("the gate must actually be applied somewhere", gatedFields.isNotEmpty())
    }

    @Test
    fun `the screen derives rootKnobs from the same rule the applier uses`() {
        val text = screen()
        assertTrue(text.contains("InAppProfileRules.isActive("))
        assertTrue(text.contains("val rootKnobs = !inAppProfile"))
    }
}
