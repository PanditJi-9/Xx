/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * الإيقاع يُقاس على حدوده لأنه يُغذّي رقمًا يُعرض للمستخدم: «كم هرتز وصل فعلًا».
 *
 * والقاعدة التي يثبّتها هذا الملفّ: **الطلب ليس القياس**. تسجيل حدثين لا يُنتج معدّلًا، وقيمة
 * سالبة أو متساوية لا تُنتج هرتزًا، والفاصل القديم يخرج من النافذة كما تخرج عيّناته.
 */
class SensorCadenceTest {

    private fun steady(cadence: SensorCadence, count: Int, periodNs: Long) {
        for (i in 0 until count) cadence.record(i * periodNs)
    }

    @Test
    fun `no events means no rate and no gap`() {
        val snapshot = SensorCadence().snapshot()

        assertEquals(0L, snapshot.received)
        assertNull(snapshot.hz)
        assertNull(snapshot.longestGapMs)
    }

    @Test
    fun `two events are one interval, not a measured rate`() {
        val cadence = SensorCadence()

        cadence.record(0L)
        cadence.record(16_000_000L)

        assertEquals(2L, cadence.snapshot().received)
        assertNull(cadence.snapshot().hz)
        assertNull(cadence.snapshot().longestGapMs)
    }

    @Test
    fun `steady sixty hertz measures sixty and its real gap`() {
        val cadence = SensorCadence()

        steady(cadence, 61, 16_666_667L)

        val snapshot = cadence.snapshot()
        assertEquals(61L, snapshot.received)
        assertEquals(60f, snapshot.hz!!, 0.5f)
        assertEquals(16L, snapshot.longestGapMs)
    }

    @Test
    fun `the longest gap stays visible after the sensor calms down`() {
        val cadence = SensorCadence()
        cadence.record(0L)
        cadence.record(250_000_000L)
        for (i in 1..20) cadence.record(250_000_000L + i * 16_000_000L)

        val snapshot = cadence.snapshot()
        assertEquals(250L, snapshot.longestGapMs)
        assertNotNull(snapshot.hz)
    }

    @Test
    fun `a gap that fell out of the window stops being reported`() {
        val cadence = SensorCadence(capacity = 4)
        cadence.record(0L)
        cadence.record(1_000_000_000L)
        for (i in 1..10) cadence.record(1_000_000_000L + i * 16_000_000L)

        val snapshot = cadence.snapshot()
        assertEquals(16L, snapshot.longestGapMs)
        assertTrue(snapshot.received > 4)
    }

    @Test
    fun `an out of order stamp is ignored instead of inventing a negative gap`() {
        val cadence = SensorCadence()
        cadence.record(0L)
        cadence.record(100_000_000L)
        cadence.record(50_000_000L)

        val snapshot = cadence.snapshot()
        assertEquals(2L, snapshot.received)
        assertNull(snapshot.hz)
        assertNull(snapshot.longestGapMs)
    }

    @Test
    fun `identical stamps give a gap but no invented rate`() {
        val cadence = SensorCadence()
        repeat(4) { cadence.record(7_000_000L) }

        val snapshot = cadence.snapshot()
        assertEquals(1L, snapshot.received)
        assertNull(snapshot.hz)
        assertNull(snapshot.longestGapMs)
    }

    @Test
    fun `a capacity below three still needs three samples`() {
        val cadence = SensorCadence(capacity = 1)

        steady(cadence, 3, 10_000_000L)

        val snapshot = cadence.snapshot()
        assertEquals(100f, snapshot.hz!!, 1f)
        assertEquals(10L, snapshot.longestGapMs)
    }
}
