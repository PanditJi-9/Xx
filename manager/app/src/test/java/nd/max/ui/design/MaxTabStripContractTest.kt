/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.design

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * حرّاس شكل الشريط: يقرؤون الكود لا اللقطة، فيمنعون عودة الحبّات الكبسولية والتبويبات المتفرّقة
 * الحدود، ويحفظون حدّ اللمس وصفة التبويب وطريقة الاهتزاز المشتركة.
 *
 * **حدّها المُعلَن:** انزلاق العدسة ولونها على الجهاز يحتاجان جهازًا؛ هذا يحرس البنية فقط.
 */
class MaxTabStripContractTest {

    private lateinit var strip: String

    @Before
    fun locateStrip() {
        val found = listOf(
            File("src/main/java/nd/max/ui/design/MaxTabStrip.kt"),
            File("app/src/main/java/nd/max/ui/design/MaxTabStrip.kt"),
            File("../app/src/main/java/nd/max/ui/design/MaxTabStrip.kt"),
            File("manager/app/src/main/java/nd/max/ui/design/MaxTabStrip.kt"),
        ).firstOrNull { it.isFile }
        assumeTrue("Cannot locate MaxTabStrip.kt; guard not evaluated", found != null)
        strip = found!!.readText()
    }

    @Test
    fun `tabs are not capsules because a pill means a status`() {
        assertFalse(
            "التبويب عاد كبسولة",
            strip.contains("RoundedCornerShape(MaxRadius.pill)"),
        )
    }

    @Test
    fun `the selection is one sliding lens rather than a fill on every tab`() {
        assertTrue("العدسة المنزلقة غائبة", strip.contains(".tabSelectionLens("))
        assertTrue("زاوية العدسة ليست من الرموز", strip.contains("cornerRadius = MaxRadius.control"))
    }

    @Test
    fun `every tab keeps the 48dp touch floor and the tab role`() {
        assertTrue("حدّ اللمس غائب", strip.contains("heightIn(min = MaxSize.minTouchTarget)"))
        assertTrue("دور التبويب غائب", strip.contains("role = Role.Tab"))
    }

    @Test
    fun `haptics go through the shared helper`() {
        assertTrue("الاهتزاز ليس من المساعد المشترك", strip.contains("rememberMaxHaptics()"))
        assertFalse("نداء اهتزاز خام عاد", strip.contains("LocalHapticFeedback"))
    }
}
