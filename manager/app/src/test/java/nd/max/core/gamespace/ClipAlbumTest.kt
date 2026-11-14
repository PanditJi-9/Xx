/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * ألبوم المقاطع — ما يُخطئ بصمت: ترتيب زمني مجهول، وصفر كاذب، وصياغة رقم.
 *
 * ولا يُختبر الرسم ولا `MediaStore` (يحتاجان جهازًا)؛ يُختبر النموذج الذي يُغذّيهما.
 */
class ClipAlbumTest {

    private var defaultLocale: Locale? = null

    @Before
    fun pinLocale() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun restoreLocale() {
        defaultLocale?.let { Locale.setDefault(it) }
    }

    private fun clip(id: Long, addedAtMs: Long? = id, size: Long? = null, duration: Long? = null) =
        ClipEntry(id = id, name = "Max_$id.mp4", sizeBytes = size, durationMs = duration, addedAtMs = addedAtMs)

    @Test
    fun `الأحدث أولًا والمجهول الوقت في الذيل`() {
        val sorted = ClipAlbumMath.sortNewestFirst(
            listOf(clip(1, addedAtMs = 100L), clip(2, addedAtMs = null), clip(3, addedAtMs = 300L))
        )
        assertEquals(listOf(3L, 1L, 2L), sorted.map { it.id })
    }

    @Test
    fun `الوقت المتساوي يُفصل بالمعرّف لا بترتيب الوصول`() {
        val sorted = ClipAlbumMath.sortNewestFirst(
            listOf(clip(7, addedAtMs = 50L), clip(9, addedAtMs = 50L), clip(8, addedAtMs = 50L))
        )
        assertEquals(listOf(9L, 8L, 7L), sorted.map { it.id })
    }

    @Test
    fun `الحجم المجهول شرطة لا صفر`() {
        assertEquals("—", ClipAlbumMath.sizeText(null))
        assertEquals("—", ClipAlbumMath.sizeText(-1L))
        assertEquals("512 B", ClipAlbumMath.sizeText(512L))
        assertEquals("2 KB", ClipAlbumMath.sizeText(2_048L))
        assertEquals("5.0 MB", ClipAlbumMath.sizeText(5_242_880L))
        assertEquals("1.5 GB", ClipAlbumMath.sizeText(1_610_612_736L))
    }

    @Test
    fun `المدة المجهولة أو الصفرية شرطة`() {
        assertEquals("—", ClipAlbumMath.durationText(null))
        assertEquals("—", ClipAlbumMath.durationText(0L))
        assertEquals("0:59", ClipAlbumMath.durationText(59_000L))
        assertEquals("1:01", ClipAlbumMath.durationText(61_000L))
        assertEquals("1:01:01", ClipAlbumMath.durationText(3_661_000L))
    }

    @Test
    fun `بلا إذن لا يُقال لا مقاطع`() {
        assertEquals(ClipAlbumState.NoPermission, ClipAlbumMath.stateOf(permitted = false, read = false, count = 0))
        assertEquals(ClipAlbumState.NoPermission, ClipAlbumMath.stateOf(permitted = false, read = true, count = 4))
    }

    @Test
    fun `فشل القراءة ليس فراغًا`() {
        assertEquals(ClipAlbumState.Unreadable, ClipAlbumMath.stateOf(permitted = true, read = false, count = 0))
        assertEquals(ClipAlbumState.Empty, ClipAlbumMath.stateOf(permitted = true, read = true, count = 0))
        assertEquals(ClipAlbumState.Ready, ClipAlbumMath.stateOf(permitted = true, read = true, count = 2))
    }
}
