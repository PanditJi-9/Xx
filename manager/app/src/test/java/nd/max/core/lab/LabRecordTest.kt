/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.lab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * **الترميز الذي يُحفظ في التفضيلات — يُقاس بلا Android.**
 *
 * والحدّ المُعلَن: هذه الاختبارات تقيس التحويل وحده؛ بقاء القيمة بعد إغلاق الشاشة
 * يحتاج جهازًا (SharedPreferences الحقيقية)، فلا يُدّعى هنا.
 */
class LabRecordTest {

    @Test
    fun `a record survives a round trip`() {
        val record = LabRecord("PASSED", 1_760_000_000_000L)
        assertEquals(record, decodeLabRecord(encodeLabRecord(record)))
    }

    @Test
    fun `nothing saved reads as nothing, not as a verdict`() {
        assertNull(decodeLabRecord(null))
    }

    @Test
    fun `a value without a separator is refused`() {
        assertNull(decodeLabRecord("PASSED"))
    }

    @Test
    fun `a time that is not a number is refused`() {
        assertNull(decodeLabRecord("PASSED|abc"))
    }

    @Test
    fun `an empty outcome is refused`() {
        // «|123» تعني نتيجة بلا كلمة — عرضها كحكم يعني كلمة لا معنى لها.
        assertNull(decodeLabRecord("|123"))
    }

    @Test
    fun `a zero time is refused`() {
        assertNull(decodeLabRecord("PASSED|0"))
    }

    @Test
    fun `extra separators are refused instead of silently truncated`() {
        assertNull(decodeLabRecord("PASSED|123|456"))
    }
}
