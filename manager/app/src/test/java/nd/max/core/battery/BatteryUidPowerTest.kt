/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * المحلّل يُقاس على نصٍّ **من شكل التفريغ نفسه**: UID بصيغة `u0a123`، وأسماء أقسام قبل القسم
 * المطلوب وبعده. والخطأ في هذا المحلّل لا يرمي استثناءً — يعرض تطبيقًا بالاستهلاك الأعلى وهو
 * ليس كذلك، فيُحذف تطبيق سليم بدل أن يُقرأ رقمه.
 */
class BatteryUidPowerTest {

    private val dump = """
        Battery Stats:
          Time on battery: 6h 12m
        Per-PID Stats:
          PID 1234: 120 ms
        Estimated power use (mAh):
          Capacity: 5000, Computed drain: 412
          Screen: 210.0
          UID u0a123: 82.40 ( cpu=40.1 wifi=1.2 )
          UID u0a177: 41.20 ( cpu=20.0 )
          UID u0a123: 12.10 ( sensors=12.1 )
          UID 1000: 9.50 ( cpu=9.5 )
          UID u10a55: 3.00 ( cpu=3.0 )
          Unaccounted: 46.0

        Other section:
          UID u0a99999: 999.0
    """.trimIndent()

    @Test
    fun only_the_power_section_is_parsed() {
        val reading = BatteryUidPower.parse(dump)!!

        assertNull(reading.entries.firstOrNull { it.uid == 109_999 })
        assertEquals(148.2f, reading.totalMilliampHours, 0.001f)
    }

    @Test
    fun a_repeated_uid_within_the_section_is_summed() {
        val reading = BatteryUidPower.parse(dump)!!
        val entry = reading.entries.first { it.uid == 10123 }

        assertEquals(94.5f, entry.milliampHours, 0.001f)
    }

    @Test
    fun entries_are_ordered_by_estimated_drain() {
        val reading = BatteryUidPower.parse(dump)!!

        assertEquals(listOf(10123, 10177, 1000, 1_010_055), reading.entries.map { it.uid })
    }

    @Test
    fun the_total_is_the_sum_of_what_was_read_not_the_device_drain() {
        val reading = BatteryUidPower.parse(dump)!!

        assertEquals(148.2f, reading.totalMilliampHours, 0.001f)
    }

    @Test
    fun work_profile_apps_are_decoded_with_their_user_offset() {
        assertEquals(10123, BatteryUidPower.decodeUid("u0a123"))
        assertEquals(1_010_055, BatteryUidPower.decodeUid("u10a55"))
        assertEquals(1000, BatteryUidPower.decodeUid("1000"))
        assertNull(BatteryUidPower.decodeUid("u_x"))
    }

    @Test
    fun a_dump_without_a_power_section_is_absence_not_zero() {
        assertNull(BatteryUidPower.parse("Battery Stats:\n  nothing here\n"))
        assertNull(BatteryUidPower.parse("Estimated power use (mAh):\n  Screen: 120.0\n"))
    }
}
