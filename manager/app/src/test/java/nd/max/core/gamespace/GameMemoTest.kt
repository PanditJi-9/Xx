/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * المذكّرة المثبّتة — ما يُخطئ بصمت: فراغٌ يُعرض إطارًا، وسطرٌ جديد يفتح تراكبًا متعدّد الأسطر،
 * ونصٌّ طويل يغطّي نصف الشاشة، وإيموجي مقطوع يُرسم مربّعًا.
 *
 * ولا يُختبر التراكب ولا `WindowManager` (يحتاجان جهازًا)؛ يُختبر النموذج الذي يُغذّيهما.
 */
class GameMemoTest {

    @Test
    fun `الأسطر الجديدة والجدولة تُطوى إلى مسافة واحدة`() {
        assertEquals("المرحلة 4 — الباب الأيسر", GameMemo.sanitize("المرحلة 4\n—\tالباب\r\nالأيسر"))
        assertEquals("a b", GameMemo.sanitize("a \n\n  b"))
    }

    @Test
    fun `الفراغات الطرفية لا تُحفظ`() {
        assertEquals("مرحلة 2", GameMemo.sanitize("   مرحلة 2   "))
        assertEquals("", GameMemo.sanitize("   \n\t "))
        assertEquals("", GameMemo.sanitize(null))
    }

    @Test
    fun `الحد الأقصى مقيس بالحرف لا بالتقدير`() {
        assertEquals(120, GameMemo.MAX_LENGTH)
        assertEquals(120, GameMemo.sanitize("x".repeat(500)).length)
        assertEquals("x".repeat(120), GameMemo.sanitize("x".repeat(120)))
    }

    @Test
    fun `القصّ لا يترك نصف إيموجي`() {
        // ١١٩ حرفًا ثم إيموجي (وحدتان): القصّ عند ١٢٠ يقع في منتصفه، فيُسقط الحرف الأخير.
        val raw = "x".repeat(119) + "\uD83C\uDFAE"
        val clean = GameMemo.sanitize(raw)
        assertEquals(119, clean.length)
        assertFalse(clean.last().isHighSurrogate())
    }

    @Test
    fun `نص فارغ يعني مذكّرة غير موجودة لا إطارًا فارغًا`() {
        val memo = GameMemo.of("   ", visible = true)
        assertEquals("", memo.text)
        assertFalse(memo.visible)
        assertEquals("", memo.shownText)
    }

    @Test
    fun `المخفيّة لا ترسم نصًّا وإن حملت نصًّا`() {
        val memo = GameMemo.of("المرحلة 4", visible = false)
        assertEquals("المرحلة 4", memo.text)
        assertFalse(memo.visible)
        assertEquals("", memo.shownText)
    }

    @Test
    fun `المرئية ترسم النص المنظَّف نفسه`() {
        val memo = GameMemo.of("  المرحلة\n4  ", visible = true)
        assertTrue(memo.visible)
        assertEquals("المرحلة 4", memo.text)
        assertEquals("المرحلة 4", memo.shownText)
    }
}
