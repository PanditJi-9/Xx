/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.mainscreens

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * **سطر الصفّ يُقرأ بنظرة، لا يُدرَس.**
 *
 * أمر المالك: «التفاصيل والكلام زيادة عن اللزوم … قلّل الـInfo … مش لازم شرح مطوّل».
 * والموضع الذي يتكرّر فيه الشرح أكثر من أي غيره هو **وصف الصفّ** (`max_role_*`): يظهر تحت
 * اسم كل شاشة في التحكّم، وفي كل مجال، وفي كل نتيجة بحث — فسطرٌ من ١٠٠ حرف هناك يعني مئة
 * حرف في كل مرّة يمرّ عليها الإصبع.
 *
 * فالسقف مكتوب هنا: **٤٠ حرفًا** لكل وصف في اللغتين. لا يُقاس «هل الشرح مفيد» — هذا حكم
 * تحريري؛ بل يُقاس ما يمكن قياسه: **الطول**. والوصف الطويل ليس خطأً نحويًّا بل عطب واجهة:
 * يُلَفّ إلى سطرين، ويخرج الصفّ عن إيقاع القائمة.
 *
 * وحدّه المُعلَن: يقيس الطول لا المعنى. وصفٌ من ٣٩ حرفًا قد يكون غامضًا — وهذا يُراجَع
 * بالعين، لا بهذا الاختبار.
 */
class RowSubtitleBrevityTest {

    private lateinit var resRoot: File

    @Before
    fun locateResources() {
        val found = listOf(
            File("src/main/res"),
            File("../res"),
            File("app/src/main/res"),
            File("manager/app/src/main/res"),
        ).firstOrNull { it.isDirectory }
        assumeTrue("Cannot locate res/; brevity not evaluated", found != null)
        resRoot = found!!
    }

    private fun roles(file: File): Map<String, String> {
        val text = file.readText()
        return Regex("""<string name="(max_role_[a-z0-9_]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(text)
            .associate { it.groupValues[1] to it.groupValues[2] }
    }

    /** ٤٠ حرفًا: ما زاد يلتفّ سطرًا ثانيًا في الصفّ، والصفحة تفقد إيقاعها. */
    @Test
    fun `every row subtitle fits on one line, in both languages`() {
        val ceiling = 40
        val arabic = roles(File(resRoot, "values-ar/strings.xml"))
        val english = roles(File(resRoot, "values/strings.xml"))

        assertTrue("لا وصوف صفوف في الشجرة — الاختبار لم يقس شيئًا", arabic.isNotEmpty())

        val offenders = arabic.filter { it.value.length > ceiling }.map { (key, value) ->
            "ar/$key (${value.length}): $value"
        } + english.filter { it.value.length > ceiling }.map { (key, value) ->
            "en/$key (${value.length}): $value"
        }

        assertTrue(
            "وصف صفّ أطول من $ceiling حرفًا — يلتفّ سطرًا ثانيًا في القائمة:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    /** واللغتان تُسمّيان الصفوف نفسها: مفتاحٌ في العربية وبلا مقابل لا يُترجم، بل يُختلق. */
    @Test
    fun `the row table is the same shape in Arabic and English`() {
        val arabic = roles(File(resRoot, "values-ar/strings.xml")).keys
        val english = roles(File(resRoot, "values/strings.xml")).keys
        val missingInArabic = (english - arabic).sorted()
        val missingInEnglish = (arabic - english).sorted()
        assertTrue("بلا مقابل عربي: $missingInArabic", missingInArabic.isEmpty())
        assertTrue("بلا مقابل إنجليزي: $missingInEnglish", missingInEnglish.isEmpty())
    }
}
