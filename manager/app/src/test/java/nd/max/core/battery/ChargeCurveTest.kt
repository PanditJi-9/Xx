/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.battery

import android.os.BatteryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * منحنى الشحن يُقاس هنا على الحالات التي تُفسد الرقم بصمت: إشارة التيار، نافذة قصيرة، ومستوى ممتلئ.
 *
 * والقاعدة: **المجهول ليس صفرًا**. عيّنة بلا جهد أو تيار تُعطي `null` للقدرة، ولا تُحسب صفرًا في
 * المتوسط — وإلا بدا الشاحن ضعيفًا لأنه لم يُقرأ.
 */
class ChargeCurveTest {

    private fun sample(
        atMillis: Long = 0L,
        level: Int = 50,
        voltageMv: Int? = 4000,
        currentUa: Long? = 2_000_000L,
        plugged: Int = 1,
        status: Int = BatteryManager.BATTERY_STATUS_CHARGING,
    ) = BatterySample(
        atMillis = atMillis,
        levelPercent = level,
        voltageMv = voltageMv,
        temperatureC = 30f,
        status = status,
        plugged = plugged,
        currentUa = currentUa,
    )

    @Test
    fun power_is_voltage_times_current_even_when_the_core_reports_a_negative_current() {
        // الإشارة تختلف بين النوى؛ القدرة تُؤخذ بالقيمة المطلقة.
        val negative = sample(voltageMv = 4000, currentUa = -1_000_000L)
        val positive = sample(voltageMv = 4000, currentUa = 1_000_000L)

        assertEquals(4f, ChargeCurve.watts(negative)!!, 0.001f)
        assertEquals(4f, ChargeCurve.watts(positive)!!, 0.001f)
    }

    @Test
    fun a_missing_or_zero_reading_gives_null_not_zero_watts() {
        assertNull(ChargeCurve.watts(sample(voltageMv = null)))
        assertNull(ChargeCurve.watts(sample(currentUa = null)))
        assertNull(ChargeCurve.watts(sample(voltageMv = 0)))
        assertNull(ChargeCurve.watts(sample(currentUa = 0L)))
    }

    @Test
    fun a_window_needs_two_samples_and_at_least_a_minute() {
        assertNull(ChargeCurve.window(emptyList()))
        assertNull(ChargeCurve.window(listOf(sample(atMillis = 0L))))
        // ٥٩ ثانية أقل من الحدّ: معدّلٌ من عيّنتين متلاصقتين ليس معدّلًا.
        assertNull(
            ChargeCurve.window(
                listOf(sample(atMillis = 0L, level = 20), sample(atMillis = 59_000L, level = 21))
            )
        )
        assertNotNull(
            ChargeCurve.window(
                listOf(sample(atMillis = 0L, level = 20), sample(atMillis = 60_000L, level = 21))
            )
        )
    }

    @Test
    fun discharging_samples_never_enter_the_charging_window() {
        val window = ChargeCurve.window(
            listOf(
                sample(atMillis = 0L, level = 20),
                // عيّنة على البطارية: ليست شحنًا ولو جاءت في المنتصف.
                sample(atMillis = 30_000L, level = 60, plugged = 0, status = BatteryManager.BATTERY_STATUS_DISCHARGING),
                sample(atMillis = 600_000L, level = 30),
            )
        )

        assertNotNull(window)
        assertEquals(20, window!!.startLevel)
        assertEquals(30, window.endLevel)
        assertEquals(2, window.points.size)
    }

    @Test
    fun the_window_reports_levels_span_and_power_over_the_same_points() {
        val window = ChargeCurve.window(
            listOf(
                sample(atMillis = 0L, level = 20, currentUa = 1_000_000L),
                sample(atMillis = 300_000L, level = 25, currentUa = 2_000_000L),
                sample(atMillis = 600_000L, level = 30, currentUa = 3_000_000L),
            )
        )!!

        assertEquals(20, window.startLevel)
        assertEquals(30, window.endLevel)
        assertEquals(10f, window.spanMinutes, 0.001f)
        assertEquals(12f, window.peakWatts!!, 0.001f)
        assertEquals(8f, window.meanWatts!!, 0.001f)
    }

    @Test
    fun a_full_battery_has_no_estimate_and_a_rising_one_extends_the_measured_rate() {
        val full = ChargeCurve.window(
            listOf(sample(atMillis = 0L, level = 100), sample(atMillis = 600_000L, level = 100))
        )!!
        assertNull("ممتلئ: لا وقت متبقٍّ", full.minutesToFull)

        val rising = ChargeCurve.window(
            listOf(sample(atMillis = 0L, level = 20), sample(atMillis = 600_000L, level = 30))
        )!!
        // نقطة كل دقيقة ⇒ ٧٠ نقطة متبقية إلى ١٠٠.
        assertEquals(70f, rising.minutesToFull!!, 0.001f)
    }

    @Test
    fun a_long_history_is_cut_to_the_last_points_not_the_first() {
        val samples = (0..300).map { i -> sample(atMillis = i * 60_000L, level = 10 + i / 10) }

        val window = ChargeCurve.window(samples)!!

        assertEquals(ChargeCurve.MAX_POINTS, window.points.size)
        // القطع من البداية: آخر عيّنة تبقى، وأول عيّنة لا.
        assertEquals(300L * 60_000L, window.points.last().atMillis)
    }

    @Test
    fun energy_and_charge_integrate_the_whole_window_with_trapezoids() {
        // 4 V × 1 A = 4 W ثابتة لعشر دقائق ⇒ 4 × (10/60) = 0.6667 Wh، و1000 mA × (10/60) = 166.67 mAh.
        val window = ChargeCurve.window(
            listOf(
                sample(atMillis = 0L, level = 20, voltageMv = 4000, currentUa = 1_000_000L),
                sample(atMillis = 300_000L, level = 25, voltageMv = 4000, currentUa = 1_000_000L),
                sample(atMillis = 600_000L, level = 30, voltageMv = 4000, currentUa = 1_000_000L),
            )
        )!!

        assertEquals(0.6667f, window.energyWh!!, 0.001f)
        assertEquals(166.67f, window.chargeMah!!, 0.01f)
    }

    @Test
    fun an_unknown_endpoint_skips_its_interval_and_does_not_become_zero() {
        // الجهد مجهول في الوسط: لا فاصل طاقة صالحًا ⇒ null، لكن التيار معروف ⇒ الشحنة تُحسب.
        val window = ChargeCurve.window(
            listOf(
                sample(atMillis = 0L, level = 20, voltageMv = 4000, currentUa = 1_000_000L),
                sample(atMillis = 300_000L, level = 25, voltageMv = null, currentUa = 1_000_000L),
                sample(atMillis = 600_000L, level = 30, voltageMv = 4000, currentUa = 1_000_000L),
            )
        )!!

        assertNull(window.energyWh)
        assertEquals(166.67f, window.chargeMah!!, 0.01f)
    }
}
