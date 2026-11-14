/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حساب القصّ فقط — لا `MediaExtractor` ولا `MediaStore`.
 *
 * والقصّ الفعلي يحتاج جهازًا (فكّ ترميز وكتابة معرّض)، لكن الحدود التي تمنع ملفًّا فارغًا
 * أو أقصر من ثانية هي حساب نقيّ يُقاس هنا.
 */
class ClipTrimMathTest {

    @Test
    fun `المدة المجهولة أو القصيرة لا تُقصّ`() {
        assertFalse(ClipTrimMath.canTrim(null))
        assertFalse(ClipTrimMath.canTrim(0L))
        assertFalse(ClipTrimMath.canTrim(1_999L))
        assertTrue(ClipTrimMath.canTrim(2_000L))
    }

    @Test
    fun `البداية تُقصّ داخل المدى ولا تتجاوز المدى ناقص ثانية`() {
        assertEquals(0L, ClipTrimMath.clampStart(-500L, 60_000L))
        assertEquals(0L, ClipTrimMath.clampStart(0L, 60_000L))
        // أقصى بداية = ٣٠٠٠ − ١٠٠٠ = ٢٠٠٠، حتى لو جرّ المستخدم إلى ٥٠٠٠.
        assertEquals(2_000L, ClipTrimMath.clampStart(5_000L, 3_000L))
        assertEquals(1_500L, ClipTrimMath.clampStart(1_500L, 10_000L))
    }

    @Test
    fun `النهاية لا تسبق البداية بأقل من ثانية`() {
        assertEquals(6_000L, ClipTrimMath.clampEnd(1_000L, 5_000L, 60_000L))
        assertEquals(60_000L, ClipTrimMath.clampEnd(90_000L, 5_000L, 60_000L))
        assertEquals(60_000L, ClipTrimMath.clampEnd(60_000L, 5_000L, 60_000L))
    }

    @Test
    fun `المحفوظ لا يكون سالبًا حتى لو جاءت الأطراف معكوسة`() {
        assertEquals(0L, ClipTrimMath.keptMs(9_000L, 1_000L))
        assertEquals(4_000L, ClipTrimMath.keptMs(1_000L, 5_000L))
    }

    @Test
    fun `الكسر ذهابًا وعودة على شريط المدة`() {
        assertEquals(30_000L, ClipTrimMath.fromFraction(0.5f, 60_000L))
        assertEquals(0.5f, ClipTrimMath.fraction(30_000L, 60_000L))
        // المدة غير الصالحة تعطي صفرًا لا قسمة على صفر.
        assertEquals(0f, ClipTrimMath.fraction(5_000L, 0L))
        assertEquals(0L, ClipTrimMath.fromFraction(0.5f, 0L))
        assertEquals(1f, ClipTrimMath.fraction(90_000L, 60_000L))
    }

    @Test
    fun `المقابض تُقرَّب لأقرب نصف ثانية`() {
        assertEquals(1_000L, ClipTrimMath.clampStart(1_240L, 10_000L))
        assertEquals(1_500L, ClipTrimMath.clampStart(1_300L, 10_000L))
        assertEquals(2_000L, ClipTrimMath.clampStart(1_800L, 10_000L))
    }
}
