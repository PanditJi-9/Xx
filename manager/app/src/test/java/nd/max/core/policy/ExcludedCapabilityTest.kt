/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.policy

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حارس سجلّ المستبعَد — يُقاس بالمصدر لأنه لا يمرّ بمنصّة أندرويد.
 *
 * **ولماذا مصدرًا لا مرجع موارد:** الاختبارات هنا JVM بلا `android.jar`، فقراءة معرّف نصّي
 * تعني ربطًا بأندرويد. والقاعدة التي نحميها نصّية بطبيعتها: **كل بند يُعلن له عنوان وسبب، بالعربية
 * والإنجليزية**، ولا بند يُحذف بصمت ولا معرّف يتكرّر.
 *
 * **والفشل مقصود عند غياب المستودع:** اختبار يتخطّى نفسه عند تعذّر القراءة لا يقيس شيئًا
 * (وهو العطب نفسه الذي وُجدت البوابة لمنعه).
 */
class ExcludedCapabilityTest {

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).canonicalFile
        repeat(8) {
            val candidate = dir ?: return@repeat
            if (File(candidate, "tools/kt_balance.py").isFile) return candidate
            dir = candidate.parentFile
        }
        throw AssertionError("Could not locate the repository root from ${System.getProperty("user.dir")}")
    }

    private fun text(relative: String): String {
        val file = File(repoRoot(), relative)
        assertTrue("Missing source file: ${file.path}", file.isFile)
        return file.readText()
    }

    /** المصدر بلا تعليقات: الفحص عن عنصر واجهة لا عن كلمة في شرح. */
    private fun code(relative: String): String =
        text(relative)
            .replace(Regex("/\\*[\\s\\S]*?\\*/"), " ")
            .replace(Regex("//[^\\n]*"), " ")

    @Test
    fun every_declared_string_exists_in_english_and_arabic() {
        val model = text("manager/app/src/main/java/nd/max/core/policy/ExcludedCapability.kt")
        val names = Regex("""R\.string\.([a-z0-9_]+)""").findAll(model).map { it.groupValues[1] }.toSet()

        assertTrue("The catalogue declares no string resources at all", names.isNotEmpty())
        val english = text("manager/app/src/main/res/values/strings.xml")
        val arabic = text("manager/app/src/main/res/values-ar/strings.xml")
        names.forEach { name ->
            assertTrue("$name is missing from values/strings.xml", english.contains("name=\"$name\""))
            assertTrue("$name is missing from values-ar/strings.xml", arabic.contains("name=\"$name\""))
        }
    }

    @Test
    fun identifiers_are_unique_so_two_rows_cannot_collide() {
        val model = text("manager/app/src/main/java/nd/max/core/policy/ExcludedCapability.kt")
        val ids = Regex("""id = "([a-z0-9_]+)"""").findAll(model).map { it.groupValues[1] }.toList()

        assertTrue("No entries were found", ids.isNotEmpty())
        assertEquals("Duplicate ids in the catalogue: $ids", ids.size, ids.toSet().size)
    }

    @Test
    fun all_three_kinds_are_populated_so_no_group_silently_disappears() {
        val model = text("manager/app/src/main/java/nd/max/core/policy/ExcludedCapability.kt")

        listOf("REFUSED", "UNSUPPORTED", "DANGEROUS").forEach { kind ->
            assertTrue("kind $kind has no entry", model.contains("kind = Kind.$kind"))
        }
    }

    @Test
    fun the_card_offers_no_control_and_is_reached_from_diagnostics() {
        // التعليقات تُنزع قبل الفحص: تعليق يشرح «لماذا لا onClick» ليس onClick.
        val card = code("manager/app/src/main/java/nd/max/ui/component/ExcludedCapabilitiesCard.kt")
        val screen = text("manager/app/src/main/java/nd/max/ui/mainscreens/DiagnosticsScreen.kt")

        // لا مفتاح ولا صفّ قابل للضغط: القائمة تُعلن، والفعل يعيش مع إقراره (ADR-07 · ADR-91).
        listOf("Switch(", "onClick", "clickable", "Button(").forEach { affordance ->
            assertTrue("The excluded list must not render a $affordance", !card.contains(affordance))
        }
        assertTrue("DiagnosticsScreen does not host the excluded list",
            screen.contains("ExcludedCapabilitiesCard"))
    }
}
