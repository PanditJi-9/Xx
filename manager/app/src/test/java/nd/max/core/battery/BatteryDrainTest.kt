/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * يثبّت قواعد قسم الاستنزاف: أيّ زوج يُحسب، وأيّه يُهمَل، ومتى يصير الرقم مجهولًا.
 *
 * كل حالة هنا عطب حقيقي كان سيظهر كرقم معقول على الشاشة — وهذا أخطر من خطأ صريح.
 */
class BatteryDrainTest {

    private val start = 1_700_000_000_000L
    private val hour = 3_600_000L

    private fun sample(
        atHours: Float,
        level: Int,
        screen: Boolean? = null,
        discharging: Boolean = true
    ) = BatterySample(
        atMillis = start + (atHours * hour).toLong(),
        levelPercent = level,
        voltageMv = 4_000,
        temperatureC = 30f,
        status = if (discharging) 3 else 2,
        plugged = if (discharging) 0 else 1,
        currentUa = -500_000L,
        screenOn = screen
    )

    @Test
    fun `one sample has no rate at all`() {
        val summary = BatteryDrain.summarise(listOf(sample(0f, 90, screen = true)))
        assertNull(summary.overallPerHour)
        assertNull(summary.activePerHour)
        assertNull(summary.idlePerHour)
        assertTrue(!summary.hasReading)
    }

    @Test
    fun `two hours losing ten percent is five per hour`() {
        val summary = BatteryDrain.summarise(listOf(sample(0f, 90, true), sample(2f, 80, true)))
        assertEquals(5f, summary.overallPerHour!!, 0.001f)
        assertEquals(5f, summary.activePerHour!!, 0.001f)
        assertNull(summary.idlePerHour)
        assertEquals(2f, summary.activeSpanHours, 0.001f)
    }

    @Test
    fun `screen-on and screen-off are kept apart`() {
        val summary = BatteryDrain.summarise(
            listOf(
                sample(0f, 90, true),
                sample(1f, 78, true),   // 12% in one screen-on hour
                sample(2f, 76, false),  // then 2% in one screen-off hour
                sample(4f, 72, false)   // then 4% in two screen-off hours
            )
        )
        assertEquals(12f, summary.activePerHour!!, 0.001f)
        assertEquals(2f, summary.idlePerHour!!, 0.001f)
        assertEquals(1f, summary.activeSpanHours, 0.001f)
        assertEquals(3f, summary.idleSpanHours, 0.001f)
        // 18% over four hours overall.
        assertEquals(4.5f, summary.overallPerHour!!, 0.001f)
    }

    @Test
    fun `a charging gap between two discharging points is not a drop`() {
        val summary = BatteryDrain.summarise(
            listOf(
                sample(0f, 80, true),
                sample(1f, 95, true, discharging = false),
                sample(2f, 78, true)
            )
        )
        // The 80 -> 95 pair is skipped (charging), the 95 -> 78 pair too.
        assertNull(summary.overallPerHour)
    }

    @Test
    fun `unknown screen state counts overall but in neither bucket`() {
        val summary = BatteryDrain.summarise(listOf(sample(0f, 90), sample(2f, 80)))
        assertEquals(5f, summary.overallPerHour!!, 0.001f)
        assertNull(summary.activePerHour)
        assertNull(summary.idlePerHour)
        assertEquals(0f, summary.activeSpanHours, 0.001f)
        assertEquals(0f, summary.idleSpanHours, 0.001f)
    }

    @Test
    fun `a mixed pair belongs to neither bucket`() {
        val summary = BatteryDrain.summarise(listOf(sample(0f, 90, true), sample(2f, 80, false)))
        assertEquals(5f, summary.overallPerHour!!, 0.001f)
        assertNull(summary.activePerHour)
        assertNull(summary.idlePerHour)
    }

    @Test
    fun `rising level during discharge is not a negative drain`() {
        val summary = BatteryDrain.summarise(listOf(sample(0f, 80, true), sample(2f, 82, true)))
        assertNull(summary.overallPerHour)
    }

    @Test
    fun `a span under a quarter hour is too short to be a rate`() {
        val summary = BatteryDrain.summarise(
            listOf(sample(0f, 90, true), sample(0.1f, 89, true))
        )
        assertNull(summary.overallPerHour)
        assertNull(summary.activePerHour)
    }

    @Test
    fun `out of order samples are skipped rather than computed backwards`() {
        val summary = BatteryDrain.summarise(
            listOf(sample(0f, 90, true), sample(2f, 80, true), sample(1f, 85, true))
        )
        assertEquals(5f, summary.overallPerHour!!, 0.001f)
    }
}
