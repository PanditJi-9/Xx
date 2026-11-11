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
 * **ما قُطع من الأدلّة يُعلَن بعدده، ولا يُدَّعى إعلانٌ كاذب.**
 *
 * العطب الذي وُلد منه هذا الاختبار: سطر القدرة في `CapabilityMatrixCard` كان يعرض
 * `evidence.take(3)` **بلا كلمة عن الباقي** — فمن يقرأ «three probes agreed» لا يعرف أن
 * هناك أربعة غيرها. والعطب ليس في العرض بل في **الصمت**: الشاشة تقول «هذه هي الأدلّة»
 * وهي تعرض ثلثها.
 *
 * والقطع نفسه مشروع (سطر القدرة لا يحتمل عشرة أدلّة)، فالمطلوب ليس عرض الكل بل **قول
 * العدد**. وهذه الدالّة هي الحساب وحده، مفصولة عن الرسم لتُقاس بلا منصّة: `shown` ما
 * يُعرض، و`more` ما بقي — والثاني هو الذي يُترجَم إلى «+2 أخرى».
 */
class CapabilityMatrixEvidenceTest {

    private val five = listOf("one", "two", "three", "four", "five")

    @Test
    fun `evidence that fits is shown whole and claims nothing more`() {
        val (shown, more) = capabilityEvidenceSplit(listOf("one", "two", "three"))
        assertEquals(listOf("one", "two", "three"), shown)
        // ولا «+0 أخرى»: الإعلان عن صفر إخفاء كذبٌ صغير لكنه كذب.
        assertEquals(0, more)
    }

    @Test
    fun `a short list is not padded and is not counted as hidden`() {
        val (shown, more) = capabilityEvidenceSplit(listOf("only"))
        assertEquals(listOf("only"), shown)
        assertEquals(0, more)
    }

    @Test
    fun `what was cut is counted, not guessed`() {
        val (shown, more) = capabilityEvidenceSplit(five)
        assertEquals(3, shown.size)
        assertEquals(listOf("one", "two", "three"), shown)
        // العدد المخفيّ حقيقيّ: خمسة أدلّة، ثلاثة معروضة، اثنان لم يُقرآ.
        assertEquals(2, more)
        assertEquals(five.size, shown.size + more)
    }

    @Test
    fun `an empty list has nothing to show and nothing to hide`() {
        val (shown, more) = capabilityEvidenceSplit(emptyList())
        assertTrue(shown.isEmpty())
        assertEquals(0, more)
    }

    @Test
    fun `the hidden count is never negative, whatever the limit`() {
        // الحارس على الحساب نفسه: `evidence.size - shown.size` لا يصير سالبًا لأن `take`
        // لا تُعيد أكثر من المُدخل — لكن لو غُيّر الحساب يومًا إلى `limit - size` لانقلبت
        // الإشارة، فيُقاس هنا صراحةً.
        listOf(0, 1, 3, 5, 10).forEach { size ->
            val evidence = (1..size).map { "e$it" }
            listOf(0, 1, 3, 7).forEach { limit ->
                val (shown, more) = capabilityEvidenceSplit(evidence, limit = limit)
                assertTrue("more سالب عند size=$size limit=$limit", more >= 0)
                assertEquals("مجموع المعروض والمخفيّ لا يساوي الكل عند size=$size", size, shown.size + more)
                assertTrue("عُرِض أكثر من السقف عند limit=$limit", shown.size <= limit)
            }
        }
    }

    @Test
    fun `the limit is a parameter, so a caller with one line of room can ask for one`() {
        val (shown, more) = capabilityEvidenceSplit(five, limit = 1)
        assertEquals(listOf("one"), shown)
        assertEquals(4, more)
    }
}
