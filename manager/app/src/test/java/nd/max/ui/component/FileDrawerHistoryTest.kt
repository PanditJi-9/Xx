/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **سجلّ المجلدات لا يُقطع صامتًا.**
 *
 * العطب الذي وُلد منه هذا الاختبار: درج الملفّات كان يعرض `history.take(12)` **بلا كلمة
 * عن الباقي**. فمن زار ثلاثين مجلّدًا يرى آخر اثني عشر، ويظنّ أنّ ما قبلها لا سبيل إليه —
 * وأقرب زرّ إلى القسم هو «مسح السجل»، فيمسح ما لم يُرَ أصلًا.
 *
 * والاختصار ليس عيبًا يُزال: الدرج ليس صفحة سجلّ. العيب أن يُقرأ الاختصار كأنه الحقيقة.
 * فتُقاس هنا قسمة القائمة: ما يُعرض، وكم بقي.
 */
class FileDrawerHistoryTest {

    private fun entries(count: Int): List<String> = (1..count).map { "folder-$it" }

    @Test
    fun `a short history is shown whole and claims nothing older`() {
        val (shown, older) = fileHistorySplit(entries(3))
        assertEquals(entries(3), shown)
        assertEquals(0, older)
    }

    @Test
    fun `exactly the limit still claims nothing older`() {
        // الحدّ نفسه ليس اختصارًا: ١٢ من ١٢ كلّها معروضة، فإعلان «٠ أقدم» كذبٌ صغير.
        val (shown, older) = fileHistorySplit(entries(12))
        assertEquals(12, shown.size)
        assertEquals(0, older)
    }

    @Test
    fun `one past the limit hides exactly one, and says so`() {
        val (shown, older) = fileHistorySplit(entries(13))
        assertEquals(12, shown.size)
        assertEquals(1, older)
        assertEquals(13, shown.size + older)
    }

    @Test
    fun `what is hidden is counted, and the newest are the ones kept`() {
        val all = entries(30)
        val (shown, older) = fileHistorySplit(all)
        assertEquals(all.take(12), shown)
        assertEquals(18, older)
        assertEquals(all.size, shown.size + older)
    }

    @Test
    fun `an empty history shows nothing and hides nothing`() {
        val (shown, older) = fileHistorySplit(emptyList<String>())
        assertTrue(shown.isEmpty())
        assertEquals(0, older)
    }

    @Test
    fun `the count is never negative and always adds up`() {
        listOf(0, 1, 11, 12, 13, 40).forEach { size ->
            listOf(0, 1, 12, 20).forEach { limit ->
                val (shown, older) = fileHistorySplit(entries(size), limit = limit)
                assertTrue("older سالب عند size=$size limit=$limit", older >= 0)
                assertEquals("المعروض والمخفيّ لا يساويان الكل عند size=$size", size, shown.size + older)
                assertTrue("عُرِض أكثر من السقف عند limit=$limit", shown.size <= limit)
            }
        }
    }
}
