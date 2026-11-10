/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * جدول اختبارات المختبر ونتائجه: مصدر واحد للبطاقات، ونتيجة لا تُخترع.
 *
 * العطب الذي يمنعه هذا الاختبار هو ما وقع فعلًا في الشاشة: قائمة اختبارات مكتوبة بيد في
 * ثلاثة مواضع (صفوف، وأقسام، وحوارات) فأضيف اختبار في موضع ونُسي في آخر. فالبطاقات تُبنى من
 * [LabTestId.entries] وحدها، وهذا الاختبار يمنع أن تُضاف بطاقة ثانية بمعزل عن الجدول.
 */
class DeviceLabTestsTest {

    @Test
    fun `every test declares a title and an icon`() {
        LabTestId.entries.forEach { test ->
            assertTrue("${test.name} has no title", test.titleRes != 0)
            assertNotNull("${test.name} has no icon", test.icon)
        }
    }

    @Test
    fun `no two tests share a title`() {
        val titles = LabTestId.entries.map { it.titleRes }
        assertEquals(titles.distinct(), titles)
    }

    @Test
    fun `the outcomes are exactly three and each has its own label and tone`() {
        assertEquals(3, LabOutcome.entries.size)
        assertEquals(LabOutcome.entries.map { it.labelRes }.distinct(), LabOutcome.entries.map { it.labelRes })
        assertEquals(LabOutcome.entries.map { it.tone }.distinct(), LabOutcome.entries.map { it.tone })
        // ولم يُجرَّب ليست «يعمل»: الحالة الافتراضية محايدة لا خضراء.
        assertEquals(LabOutcome.NOT_RUN, LabOutcome.entries.first())
    }

    @Test
    fun `the cards are built from the table and not from a second list`() {
        // فحص بنيوي على النصّ: البطاقات تُشتقّ من الجدول، فلا قائمة ثانية تُنسى.
        val source = labScreenSource()
        assertTrue(
            "DeviceLabScreen must build its cards from LabTestId.entries",
            source.contains("LabTestId.entries.map"),
        )
    }

    /** يقرأ ملف الشاشة من شجرة المصادر، ويُعلن التخطّي حين لا تُوجد (تشغيل من حزمة APK). */
    private fun labScreenSource(): String {
        val root = listOf(
            File("src/main/java/nd/max"),
            File("app/src/main/java/nd/max"),
            File("../app/src/main/java/nd/max"),
            File("manager/app/src/main/java/nd/max"),
        ).firstOrNull { it.isDirectory }
        assumeTrue("Cannot locate nd.max sources; guard not evaluated", root != null)
        return File(root, "ui/subscreens/DeviceLabScreen.kt").readText()
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("//[^\\n]*"), "")
    }
}
