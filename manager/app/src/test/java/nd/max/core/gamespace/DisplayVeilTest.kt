/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ستارة العرض — الحساب النقيّ وحده: الدورة واللون.
 *
 * **ولماذا هنا:** «اللمسة الثالثة تُطفئ» قاعدة تُنسى بسهولة لو كُتبت في الواجهة؛ وهي هنا دالّة
 * واحدة تُقاس بلا جهاز. أمّا ما تظهر عليه فعلًا على شاشة لعبة، فذاك يحتاج جهازًا ولا يُدَّعى.
 */
class DisplayVeilTest {

    @Test
    fun `دورة اللمس تُشعل دافئة ثم تُدوّر ثم تُطفئ`() {
        val off = DisplayVeilState()
        assertFalse(off.enabled)

        val warm = off.cycle()
        assertTrue(warm.enabled)
        assertEquals(DisplayVeilTone.WARM, warm.tone)

        val night = warm.cycle()
        assertTrue(night.enabled)
        assertEquals(DisplayVeilTone.NIGHT, night.tone)

        val dim = night.cycle()
        assertTrue(dim.enabled)
        assertEquals(DisplayVeilTone.DIM, dim.tone)

        val backOff = dim.cycle()
        assertFalse(backOff.enabled)
        assertEquals(DisplayVeilTone.WARM, backOff.tone)
    }

    @Test
    fun `المطفأة لا تُرسم ولونها صفر`() {
        assertEquals(0, DisplayVeilState(enabled = false).argb)
        assertNotEquals(0, DisplayVeilState(enabled = true).argb)
    }

    @Test
    fun `لكل لون تغطية موجبة وألوان مختلفة`() {
        val tones = DisplayVeilTone.entries
        tones.forEach { tone ->
            val alpha = tone.argb ushr 24
            assertTrue("$tone must be translucent, not opaque", alpha in 1..254)
        }
        assertEquals(tones.size, tones.map { it.argb }.distinct().size)
    }

    @Test
    fun `مفتاح مجهول يعود إلى الدافئ ولا يُسقط الحالة`() {
        assertEquals(DisplayVeilTone.WARM, DisplayVeilTone.fromKey(null))
        assertEquals(DisplayVeilTone.WARM, DisplayVeilTone.fromKey("SEPIA"))
        assertEquals(DisplayVeilTone.NIGHT, DisplayVeilTone.fromKey("NIGHT"))
    }
}
