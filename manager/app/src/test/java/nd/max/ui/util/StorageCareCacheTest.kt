/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ذاكرة القياس — **العمر وحده يُقاس هنا**: متى تُعرض أرقام الجولة السابقة ومتى تُرفض.
 *
 * وهي ثالثة عقد الفتح السريع في `StorageCareViewModel`: الفتح الثاني يعرض ما قيس قبل
 * دقائق بلا إعادة مسح، ولا يعرض رقمًا تقادم. فاختبار الحدّين — الداخل والخارج — هو
 * الاختبار الذي يعنيه هذا الملف.
 */
class StorageCareCacheTest {

    private fun entry(atMs: Long) = StorageCareCache.Entry(
        atMs = atMs,
        rootAvailable = false,
        measurements = CleanMeasurementSet(),
    )

    @Test
    fun `entry inside the window is fresh`() {
        val at = 1_000_000L
        assertTrue(StorageCareCache.isFresh(at, entry(at)))
        assertTrue(StorageCareCache.isFresh(at + StorageCareCache.TTL_MS, entry(at)))
        assertTrue(StorageCareCache.isFresh(at + StorageCareCache.TTL_MS / 2, entry(at)))
    }

    @Test
    fun `entry past the window is stale`() {
        val at = 1_000_000L
        assertFalse(StorageCareCache.isFresh(at + StorageCareCache.TTL_MS + 1, entry(at)))
    }

    @Test
    fun `a clock that went backwards does not vouch for a future reading`() {
        val at = 1_000_000L
        assertFalse(StorageCareCache.isFresh(at - 1, entry(at)))
    }

    @Test
    fun `store then load returns the same entry, and invalidate empties it`() {
        StorageCareCache.invalidate()
        assertNull(StorageCareCache.load())

        val fresh = entry(2_000_000L)
        StorageCareCache.store(fresh)
        assertSame(fresh, StorageCareCache.load())

        StorageCareCache.invalidate()
        assertNull(StorageCareCache.load())
    }
}
