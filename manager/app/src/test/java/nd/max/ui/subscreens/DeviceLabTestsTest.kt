/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import java.io.File
import nd.max.core.diagnostics.DeviceFacts
import nd.max.core.lab.LabRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun `an untouched lab starts at the first card of the table`() {
        assertEquals(LabTestId.entries.first(), nextUntried(emptyMap()))
    }

    @Test
    fun `a judged card is skipped, whatever the verdict`() {
        val outcomes = mapOf(
            LabTestId.SCREEN to LabOutcome.PASSED,
            LabTestId.TOUCH to LabOutcome.FAILED,
        )
        assertEquals(LabTestId.entries[2], nextUntried(outcomes))
    }

    @Test
    fun `a finished lab has no next test at all`() {
        val all = LabTestId.entries.associateWith { LabOutcome.PASSED }
        assertNull(nextUntried(all))
    }

    @Test
    fun `the report reads the verdicts before the ones that were never tried`() {
        // والمدخل مُرتَّب بالعكس عن قصد: التقرير لا يتبع ترتيب الإدخال بل ترتيب الحكم.
        val records = mapOf(
            LabTestId.WIFI to LabRecord("FAILED", 1L),
            LabTestId.SCREEN to LabRecord("PASSED", 1L),
        )
        val report = labReport(records, { "G:${it.name}" }, { it.name })
        val passed = report.indexOf("G:PASSED")
        val failed = report.indexOf("G:FAILED")
        val untried = report.indexOf("G:NOT_RUN")
        assertTrue("لا مجموعة «يعمل»", passed >= 0)
        assertTrue("«يعمل» ليس قبل «معطوب»", passed < failed)
        assertTrue("«لم يُجرَّب» ليس آخرًا", failed < untried)
    }

    @Test
    fun `every test is named exactly once, judged or not`() {
        val records = mapOf(LabTestId.TOUCH to LabRecord("FAILED", 1L))
        val report = labReport(records, { it.name }, { "T:${it.name}" })
        LabTestId.entries.forEach { test ->
            val hits = Regex(Regex.escape("T:${test.name}")).findAll(report).count()
            assertEquals("${test.name} مذكور $hits مرّة في التقرير", 1, hits)
        }
    }

    @Test
    fun `an untouched lab reports everything as untested, with no empty headers`() {
        val report = labReport(emptyMap(), { "G:${it.name}" }, { "T:${it.name}" })
        // ولا عنوان لمجموعة فارغة: «يعمل» بلا شيء تحته يُقرأ نتيجةً لم تُقل.
        assertFalse(report.contains("G:PASSED"))
        assertFalse(report.contains("G:FAILED"))
        assertEquals(1, report.split("G:NOT_RUN").size - 1)
        LabTestId.entries.forEach { test ->
            assertTrue("${test.name} غائب عن تقرير مختبر بكر", report.contains("T:${test.name}"))
        }
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

    @Test
    fun `a judged test carries the moment it was judged, when the caller can format it`() {
        val records = mapOf(LabTestId.TOUCH to LabRecord("PASSED", 1_700_000_000_000L))
        val report = labReport(records, { it.name }, { "T:${it.name}" }, stamp = { "AT:${it.atMs}" })
        assertTrue(
            "الحكم مذكور بلا زمنه — والتقرير يُشارَك خارج الجهاز، فقارئه لا يعرف عمر الحكم",
            report.contains("T:TOUCH — AT:1700000000000"),
        )
    }

    @Test
    fun `no date is invented when there is none to print`() {
        val records = mapOf(LabTestId.TOUCH to LabRecord("PASSED", 1_700_000_000_000L))
        // (١) مستدعٍ لا يملك منسّقًا: التقرير يبقى كما كان، بلا شرطة معلّقة ولا زمن مخترع.
        val plain = labReport(records, { it.name }, { "T:${it.name}" })
        assertFalse("التقرير اخترع فاصلًا لزمنٍ لم يُمرَّر", plain.contains("—"))
        assertFalse("التقرير اخترع زمنًا لم يُمرَّر", plain.contains("AT:"))
        // (٢) سجلّ بزمن صفر (كتبه مخزن قديم أو مقطوع): لا يُطبع 1970.
        val zero = labReport(
            mapOf(LabTestId.TOUCH to LabRecord("PASSED", 0L)),
            { it.name },
            { "T:${it.name}" },
            stamp = { "AT:${it.atMs}" },
        )
        assertFalse("زمن صفر طُبع كأنه تاريخ حقيقي", zero.contains("AT:"))
    }

    @Test
    fun `the device line names the device, the chip, the API and the kernel in that order`() {
        val line = labDeviceLine(
            DeviceFacts(
                appVersion = "3.1.0",
                moduleVersion = "v42",
                socModel = "MT6789",
                socManufacturer = "MediaTek",
                hardware = "rodin",
                apiLevel = 34,
                kernel = "5.10.136-android13",
                rooted = true,
                manufacturer = "Xiaomi",
                model = "Redmi Note 10",
            ),
        )
        // من الأعمّ إلى الأخصّ: الجهاز الذي يمسكه القارئ، ثم الشريحة، ثم النظام، ثم النواة.
        assertEquals(
            "Xiaomi Redmi Note 10 · MediaTek/MT6789 · API 34 · kernel 5.10.136-android13",
            line,
        )
    }

    @Test
    fun `the device line says a dash for what it does not know`() {
        val line = labDeviceLine(
            DeviceFacts(
                appVersion = null,
                moduleVersion = null,
                socModel = null,
                socManufacturer = null,
                hardware = null,
                apiLevel = 34,
                kernel = null,
                rooted = false,
            ),
        )
        // والمجهول يبقى مجهولًا: شرطة، لا "unknown" ولا صفر يُقرأ قيمةً.
        assertEquals("- · -/- · API 34 · kernel -", line)
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
