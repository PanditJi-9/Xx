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

    @Test
    fun `a saved record becomes a card verdict when both names are known`() {
        val mapped = labOutcomes(mapOf("TOUCH" to "PASSED", "SCREEN" to "FAILED"))
        assertEquals(LabOutcome.PASSED, mapped[LabTestId.TOUCH])
        assertEquals(LabOutcome.FAILED, mapped[LabTestId.SCREEN])
    }

    @Test
    fun `a test this build no longer has is dropped, not shown as a verdict`() {
        val mapped = labOutcomes(mapOf("TELEPORT" to "PASSED", "TOUCH" to "PASSED"))
        assertEquals(setOf(LabTestId.TOUCH), mapped.keys)
    }

    @Test
    fun `an outcome this build does not know is dropped, not guessed`() {
        val mapped = labOutcomes(mapOf("TOUCH" to "MAYBE", "SCREEN" to "PASSED"))
        assertEquals(setOf(LabTestId.SCREEN), mapped.keys)
    }

    @Test
    fun `an untouched lab counts every test as untried`() {
        val tally = labTally(emptyMap())
        assertEquals(0, tally.works)
        assertEquals(0, tally.faulty)
        assertEquals(LabTestId.entries.size, tally.untried)
    }

    @Test
    fun `the tally adds up to the table and never beyond it`() {
        val tally = labTally(
            mapOf(
                LabTestId.SCREEN to LabOutcome.PASSED,
                LabTestId.TOUCH to LabOutcome.FAILED,
            ),
        )
        assertEquals(1, tally.works)
        assertEquals(1, tally.faulty)
        assertEquals(LabTestId.entries.size - 2, tally.untried)
        // والعقد: المجموع = عدد البطاقات، فلا بطاقة تُعدّ مرّتين ولا تُسقَط.
        assertEquals(LabTestId.entries.size, tally.works + tally.faulty + tally.untried)
    }

    @Test
    fun `an explicit not-tried verdict counts as untried, not as a result`() {
        val tally = labTally(mapOf(LabTestId.WIFI to LabOutcome.NOT_RUN))
        assertEquals(0, tally.works)
        assertEquals(0, tally.faulty)
        assertEquals(LabTestId.entries.size, tally.untried)
    }

    @Test
    fun `every tool dialog knows which card it belongs to`() {
        // العطب الذي يمنعه: أداة تُفتح من بطاقة ثم لا يُعرف إلى أيّ بطاقة تعود، فيضيع الحكم
        // أو يُكتب على اختبار آخر. والدالّتان معكوستان إحداهما للأخرى — وهذا ما يُقاس هنا.
        LabTest.entries.forEach { tool ->
            assertEquals(
                "أداة بلا بطاقة تعود إليها: $tool",
                tool,
                tool.verdictId().dialog(),
            )
        }
    }

    @Test
    fun `only the three full-screen tests claim a tool`() {
        // الحدّ المُعلَن: عشرة اختبارات أدواتها صفوف في الشاشة لا حوارات كاملة، فلا يُدّعى
        // أن لكلّ الثلاثة عشر حوارًا. وهذا الاختبار يثبّت القائمة فلا تزيد صامتة ولا تنقص.
        val withTool = LabTestId.entries.filter { it.dialog() != null }
        assertEquals(
            listOf(LabTestId.SCREEN, LabTestId.TOUCH, LabTestId.BUTTONS),
            withTool,
        )
    }

    @Test
    fun `closing a test returns to the verdict it came from`() {
        val source = labScreenSource()
        assertTrue(
            "الشاشة لا تحتفظ بالبطاقة التي جاء منها التشغيل",
            source.contains("pendingVerdict"),
        )
        assertTrue(
            "إغلاق الأداة لا يعيد حوار الحكم",
            source.contains("labTarget = pendingVerdict"),
        )
    }

    /** يقرأ ملف الشاشة من شجرة المصادر، ويُعلن التخطّي حين لا تُوجد (تشغيل من حزمة APK). */
    @Test
    fun `every tool section has at least one card pointing at it`() {
        // العطب المقصود: زرّ «اذهب إلى الاختبار» ينزل إلى قسم. فقسمٌ لا بطاقة تشير إليه
        // نزولٌ إلى عنوانٍ لا أداة تحته — وهو وعدٌ كاذب في واجهة.
        assertEquals(
            "أقسام أدوات لا تشير إليها أيّ بطاقة",
            LabToolSection.entries.toSet(),
            LabTestId.entries.map { it.toolSection() }.toSet(),
        )
    }

    @Test
    fun `the three dialog tests point at the sections that hold them`() {
        assertEquals(LabToolSection.Screen, LabTestId.SCREEN.toolSection())
        assertEquals(LabToolSection.Screen, LabTestId.TOUCH.toolSection())
        assertEquals(LabToolSection.Controls, LabTestId.BUTTONS.toolSection())
    }

    @Test
    fun `every tool section is anchored in the page`() {
        val source = labScreenSource()
        LabToolSection.entries.forEach { section ->
            assertTrue(
                "$section بلا مرساة — الزرّ لا ينزل إلى شيء",
                source.contains("toolAnchors.getValue(LabToolSection.$section)"),
            )
        }
    }

    @Test
    fun `every tool section offers to record the verdict it sent the user to`() {
        // الحلقة كانت مقطوعة في طرفها الثاني: البطاقة تُنزل إلى القسم، ثم لا طريق للحكم من
        // هناك إلا بالتمرير عائدًا إلى أعلى الصفحة. الحارس يمنع عودة القطع: لكل قسم زرّه.
        val source = labScreenSource()
        LabToolSection.entries.forEach { section ->
            assertTrue(
                "section $section has no LabSectionTrailing, so its card cannot be judged from it",
                source.contains("LabSectionTrailing(LabToolSection.$section, pendingCard)"),
            )
        }
        assertEquals(
            "a section is wired twice — one card, one record button",
            LabToolSection.entries.size,
            Regex("LabSectionTrailing\\(LabToolSection\\.").findAll(source).count(),
        )
    }

    @Test
    fun `recording from a section consumes the pending card`() {
        // بعد أن يُفتح الحوار من رأس القسم تُفرَّغ البطاقة، فلا يبقى الزرّ يفتح حوارًا ثانيًا
        // بعد تسجيل الحكم أو إغلاقه.
        val source = labScreenSource()
        assertTrue(
            "the section record callback does not clear pendingCard",
            source.contains("pendingCard = null"),
        )
        assertTrue(
            "opening a card from the grid does not clear the stale pending card",
            Regex("pendingCard = null\\s*\\n\\s*labTarget = test").containsMatchIn(source),
        )
    }

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
