/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.viewmodel

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §10 — **خيار ضبابية النوافذ** المدمج من تحليل الوحدة المرجعية يقفل عقدَه هنا:
 * النصّان المكتوبان بالحرف، والإطفاء يعيد الحالة (لا يعلق الخيار)،
 * و**وحدة مصدر الكتابة** في التطبيق كلّه.
 *
 * ولماذا الأخير بالذات: الخطر المعتاد بعد أي دمج خارجيّ ليس الخطأ في السطر، بل **نسخة ثانية**
 * منه — خدمة أو util ثانٍ يكتب المفتاح نفسه بقيمة معارضة. والمسح يُفشل النسخة الثانية لحظة
 * كتابتها في المصدر، لا على جهاز بعد شهور.
 */
class DisplayStudioBlurTest {

    @Test
    fun `disabling writes one to the persisted setting and to the live command`() {
        assertEquals(
            listOf(
                "settings put global disable_window_blurs 1",
                "cmd window disable-blur 1",
            ),
            DisplayStudioViewModel.blurWriteCommands(disabled = true),
        )
    }

    @Test
    fun `enabling writes zero to both so nothing stays stuck off`() {
        val commands = DisplayStudioViewModel.blurWriteCommands(disabled = false)
        assertEquals("settings put global disable_window_blurs 0", commands[0])
        assertEquals("cmd window disable-blur 0", commands[1])
    }

    @Test
    fun `no second component writes the window-blur setting`() {
        val root = sourceRoot()
        assertTrue("تعذّر العثور على جذر المصادر — اختبار لا يجد ما يفحصه لا يثبت شيئًا", root != null)
        val writers = root!!.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readText().contains(DisplayStudioViewModel.WINDOW_BLUR_KEY) }
            .map { it.name }
            .toSortedSet()
        assertTrue("المسح نفسه معطوب: لا ملفَّ يكتب المفتاح أصلًا", writers.isNotEmpty())
        assertEquals(setOf("DisplayStudioViewModel.kt"), writers)
    }

    /** يبحث عن جذر مصادر التطبيق صاعدًا من مجلد تشغيل الاختبار، فلا يعتمد على مسار ثابت. */
    private fun sourceRoot(): File? {
        val candidates = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
            File("manager/app/src/main/java"),
        )
        candidates.firstOrNull { it.isDirectory }?.let { return it }

        var dir: File? = File("").absoluteFile
        repeat(6) {
            val current = dir ?: return null
            listOf("app/src/main/java", "manager/app/src/main/java")
                .map { File(current, it) }
                .firstOrNull { it.isDirectory }
                ?.let { return it }
            dir = current.parentFile
        }
        return null
    }
}
