/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **السقف لا يُقرأ نهاية.** شاشة الذاكرة تعرض ستة أسطر رفض ثم تسكت — ومن رأى ستة وظنّها
 * الملفّ كلّه اعتبر ملفّه سليمًا وهو ليس كذلك: الملفّ يُقرأ سطرًا سطرًا ويُرفض كل سطر خاطئ.
 *
 * فالفصل بين «المعروض» و«الباقي» دالّة صافية تُقاس هنا بلا جهاز ولا Compose، ويُقاس معها
 * الحدّ نفسه: ستة، لا خمسة ولا سبعة.
 */
class OverlayRejectionSplitTest {

    private val lines = (1..10).map { "line-$it" }

    @Test
    fun `a short file shows everything and announces nothing`() {
        val (shown, rest) = overlayRejectionSplit(lines.take(4))
        assertEquals(lines.take(4), shown)
        assertEquals(0, rest)
    }

    @Test
    fun `exactly at the ceiling is still nothing hidden`() {
        val (shown, rest) = overlayRejectionSplit(lines.take(6))
        assertEquals(6, shown.size)
        assertEquals(0, rest)
    }

    @Test
    fun `the ceiling is six and the remainder is counted, not dropped`() {
        val (shown, rest) = overlayRejectionSplit(lines)
        assertEquals(lines.take(6), shown)
        assertEquals(4, rest)
        assertEquals(
            "المعروض + الباقي = الكل، فلا سطر يضيع في الحساب",
            lines.size,
            shown.size + rest,
        )
    }

    @Test
    fun `a zero ceiling shows nothing and still counts the whole file`() {
        val (shown, rest) = overlayRejectionSplit(lines, limit = 0)
        assertTrue(shown.isEmpty())
        assertEquals(lines.size, rest)
    }

    @Test
    fun `a negative ceiling is treated as zero, never as a crash`() {
        val (shown, rest) = overlayRejectionSplit(lines, limit = -3)
        assertTrue(shown.isEmpty())
        assertEquals(lines.size, rest)
    }

    @Test
    fun `an empty list yields an empty pair`() {
        val (shown, rest) = overlayRejectionSplit(emptyList<String>())
        assertTrue(shown.isEmpty())
        assertEquals(0, rest)
    }
}
