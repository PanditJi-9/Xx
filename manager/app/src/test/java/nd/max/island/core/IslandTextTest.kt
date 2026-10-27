/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.island.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * قصّ النصّ — العيوب التي يمنعها هذا الملفّ تُرى في لقطات حقيقية:
 * كلمة مقطوعة في منتصفها، إيموجي مقطوع إلى نصفين، امتداد ملفّ ضائع، وسطر يبدأ بمسافة.
 */
class IslandTextTest {

    @Test
    fun `normalize collapses whitespace and trims`() {
        assertEquals("Hello world", IslandText.normalize("  Hello   world \n"))
        assertEquals("", IslandText.normalize("   "))
        assertEquals("مرحبا بالعالم", IslandText.normalize("مرحبا   بالعالم"))
    }

    @Test
    fun `short text is returned unchanged`() {
        assertEquals("Ahmed", IslandText.fit("Ahmed", 20))
        assertEquals("Ahmed", IslandText.fit("  Ahmed  ", 5))
    }

    @Test
    fun `truncation lands on a word boundary when one is close enough`() {
        val out = IslandText.fit("Downloading a very large file", 20)
        assertEquals("Downloading a very…", out)
        assertFalse(out.endsWith(" ver…"))
    }

    @Test
    fun `truncation never splits a surrogate pair`() {
        val out = IslandText.fit("Family 👨‍👩‍👧‍👦 photo", 9)
        assertTrue(IslandText.codePoints(out) <= 9)
        // آخر محرف قبل النقاط ليس نصف زوج بديل
        val beforeDots = out.dropLast(1)
        if (beforeDots.isNotEmpty()) {
            val last = beforeDots.last()
            assertFalse(Character.isHighSurrogate(last))
        }
        assertTrue(out.endsWith("…"))
    }

    @Test
    fun `middle truncation keeps both the name and the extension`() {
        val out = IslandText.fitMiddle("MaxManager-2026-10-27-release-signed.apk", 24)
        assertTrue(out.startsWith("MaxManager"))
        assertTrue(out.endsWith(".apk"))
        assertEquals(24, IslandText.codePoints(out))
    }

    @Test
    fun `zero or negative budget produces no text`() {
        assertEquals("", IslandText.fit("anything", 0))
        assertEquals("", IslandText.fitMiddle("anything", -3))
    }

    @Test
    fun `first grapheme keeps an accent with its letter`() {
        assertEquals("É", IslandText.firstGrapheme("Élodie"))
        assertEquals("A", IslandText.firstGrapheme("ahmed"))
        assertEquals("?", IslandText.firstGrapheme("   "))
        // إيموجي لا يُقطع إلى نصفين
        assertEquals("👨", IslandText.firstGrapheme("👨‍👩‍👧 family"))
    }
}
