/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.ui.component

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * **حارس الوصول — «مكوّن لا يفتحه أحد» يمرّ من كل البوابات.**
 *
 * القياس الذي وُلد هذا الملفّ لأجله: مسحٌ لكل تصريح في `core` و`ui` بعدد مرّات ظهوره في الشجرة
 * كلّها أعطى **٩٧ تصريحًا يظهر اسمها مرّة واحدة فقط** (سطر تعريفه). ومنها ملفّان بكاملهما ميتان:
 * `PulseArt.kt` (٩١ سطرًا) و`AmbientGlowCycle.kt` (٥٩ سطرًا) — لا مرجع واحد في `main` ولا في `test`
 * ولا في `debug`. والمُصرّف لا يقول شيئًا عن هذا، والاختبارات لا تراه، والبوابات تمرّ.
 *
 * فالقاعدة هنا **على مستوى الملفّ لا التصريح**: الملفّ حيّ إذا ذُكر **تصريح واحد** منه خارج نفسه.
 * وهذا هو الحدّ الذي يمنع عودة ملفّ زخرفيّ كامل بلا مستهلك، بلا أن يفرض حذف كل تصريح داخليّ غير
 * مستعمل — وهو نمط مشروع في هذا المستودع (كل شاشة تُعرّف مكوّناتها المحلّية علنًا).
 */
class ComponentReachabilityTest {

    @Test
    fun `no component file is entirely unreachable`() {
        val root = sourceRoot() ?: return
        val texts = mainSources(root)
        assumeTrue("no kotlin sources found", texts.isNotEmpty())

        // التعليقات تُجرَّد قبل العدّ: اسمٌ مذكور في تعليق ليس مستهلكًا.
        val stripped = texts.mapValues { (_, text) -> stripComments(text) }
        val corpus = stripped.values.joinToString("\n")

        val unreachable = stripped.keys
            .filter { it.path.contains("/ui/component/") }
            .filter { file ->
                val names = declarationsOf(stripped.getValue(file))
                names.isNotEmpty() && names.none { name ->
                    Regex("\\b${Regex.escape(name)}\\b").findAll(corpus).count() > 1
                }
            }
            .map { it.path.substringAfter("/ui/component/") }
            .sorted()

        assertEquals(
            "ملفّات في ui/component لا يذكر أحدٌ أيّ تصريح فيها — إمّا تُوصَل وإمّا تُحذف:\n" +
                unreachable.joinToString("\n"),
            emptyList<String>(),
            unreachable,
        )
    }

    @Test
    fun `the two decoration files removed for being unreachable stay removed`() {
        val root = sourceRoot() ?: return
        listOf("PulseArt.kt", "AmbientGlowCycle.kt").forEach { name ->
            assertFalse(
                "$name was unreachable and was removed; re-adding it needs a consumer first",
                File(root, "ui/component/$name").exists(),
            )
        }
    }

    @Test
    fun `no source claims the recommendation bridge feeds Max AI`() {
        val root = sourceRoot() ?: return
        val claim = "الذي يغذي محرك MAX AI"
        val offenders = mainSources(root)
            .filterValues { it.contains(claim) }
            .keys
            .map { it.path.substringAfter("/nd/max/") }
            .sorted()
        assertTrue("الادّعاء الذي كذّبه القياس عاد: $offenders", offenders.isEmpty())
    }

    /** يجرّد تعليقات الكتلة والسطر حتى لا يُحسب ذكرٌ في شرح كمستهلك. */
    private fun stripComments(text: String): String =
        text.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("//[^\n]*"), " ")

    /** أسماء التصريحات العليا في ملفّ — بلا تعليقات، وبلا أسماء قصيرة تُشبه كلمات شائعة. */
    private fun declarationsOf(text: String): List<String> {
        val pattern = Regex(
            "^(?:@\\w+(?:\\([^)]*\\))?\\s+)*" +
                "(?:internal |public |private |protected )?" +
                "(?:fun|val|var|object|class|interface|enum class|sealed class|data class)\\s+" +
                "(?:<[^>]+>\\s*)?" +
                "(?:[\\w.]+\\.)?" +
                "(\\w+)",
            RegexOption.MULTILINE,
        )
        return pattern.findAll(text)
            .map { it.groupValues[1] }
            .filter { it.length >= 5 }
            .distinct()
            .toList()
    }

    private fun mainSources(root: File): Map<File, String> =
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .associateWith { it.readText() }

    /** يجد جذر المصادر من مجلّد تشغيل الاختبار، ويُعلن التخطّي حين لا يُوجد. */
    private fun sourceRoot(): File? {
        val root = listOf(
            "src/main/java/nd/max",
            "app/src/main/java/nd/max",
            "../app/src/main/java/nd/max",
            "manager/app/src/main/java/nd/max",
        ).map(::File).firstOrNull { it.isDirectory }
        assumeTrue("source tree not available in this runtime", root != null)
        return root
    }
}
