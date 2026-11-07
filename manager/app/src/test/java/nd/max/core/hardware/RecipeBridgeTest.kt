/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد الوصل: كل كتابة وصفة تمرّ بالـarbiter، ولا يفتح الوصل ملفًا بنفسه.
 *
 * **ولماذا اختبار مصدري لا اختبار سلوكي:** الـarbiter و`RootFileAccess` يحتاجان جهازًا (دفتر الملكية،
 * قناة الجذر، عقد ٠٤٤٤). أمّا ما يمكن إثباته هنا فهو شكل الطريق: من يستدعى، وبأيّ ترتيب، وما الذي
 * يُمنع استعماله — وهو ما يكفي لمنع «كاتب رابع» يتجاوز ADR-11 بلا أن يلاحظه أحد.
 */
class RecipeBridgeTest {

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            if (File(dir, "tools/kt_balance.py").isFile) return dir
            dir = dir.parentFile
        }
        throw AssertionError("لم يُوجد جذر المستودع (tools/kt_balance.py) من ${System.getProperty("user.dir")}")
    }

    /** النصّ بلا تعليقات: التعليقات تذكر أسماء ممنوعة شرحًا لا استعمالًا. */
    private fun code(): String {
        val file = File(
            repoRoot(),
            "manager/app/src/main/java/nd/max/core/hardware/RecipeBridge.kt",
        )
        assertTrue("الملف غير موجود: $file", file.isFile)
        return file.readText()
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("//[^\n]*"), " ")
            .replace(Regex("\\s+"), " ")
    }

    @Test
    fun every_write_goes_through_the_arbiter_and_is_released_afterwards() {
        val text = code()

        assertTrue("لا يستدعي submit", text.contains("arbiter.submit("))
        assertTrue("لا يحرّر الطلب", text.contains("arbiter.release("))
        // الاسترجاع مسؤولية RecipeRun، فالطبقة هنا تُغلق الطلب بلا استرجاع مزدوج.
        assertTrue("الإفراج يجب أن يكون restore = false", text.contains("restore = false"))
        assertTrue("ملكية الكتابة غير معلنة", text.contains("ControlOwnership.Owner.GLOBAL_PROFILE"))
    }

    @Test
    fun reads_and_writes_use_the_existing_access_layer_only() {
        val text = code()

        assertTrue("لا يقرأ عبر RootFileAccess", text.contains("RootFileAccess.read("))
        assertTrue("لا يكتب عبر writeFireAndForget", text.contains("RootFileAccess.writeFireAndForget("))
        // لا كاتب موازٍ: لا ملف، لا صدفة، لا عملية.
        listOf("java.io.File", "File(", "Shell.cmd", "ProcessBuilder", "Runtime.getRuntime").forEach { forbidden ->
            assertFalse("يتجاوز طبقة الوصول: $forbidden", text.contains(forbidden))
        }
    }

    @Test
    fun a_missing_baseline_or_blank_value_never_writes() {
        val text = code()

        // لقطة غير مقروءة ⇒ false قبل أي submit؛ وقيمة فارغة كذلك.
        assertTrue(text.contains("baseline = RootFileAccess.read(path) ?: return false"))
        assertTrue(text.contains("if (desired.isEmpty()) return false"))
    }
}
