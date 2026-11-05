/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.battery

import android.os.BatteryManager
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The CSV is a file contract, not a screen detail: a spreadsheet reads the
 * header by name, so these tests pin the header, the column order and the
 * "missing reading is an empty field" rule.
 */
class BatteryHistoryCsvTest {

    private val header =
        "timestamp_iso,epoch_ms,level_percent,voltage_mv,temperature_c,status,plugged,current_ua,discharging"

    @Test
    fun headerOnlyWhenThereAreNoSamples() {
        assertEquals("$header\n", BatteryHistoryStore.csv(emptyList()))
    }

    @Test
    fun completeSampleWritesEveryColumnInOrder() {
        val sample = BatterySample(
            atMillis = 1_700_000_000_000L,
            levelPercent = 87,
            voltageMv = 4_120,
            temperatureC = 32.5f,
            status = BatteryManager.BATTERY_STATUS_DISCHARGING,
            plugged = 0,
            currentUa = -450_000L
        )

        assertEquals(
            "$header\n2023-11-14T22:13:20Z,1700000000000,87,4120,32.5," +
                "${BatteryManager.BATTERY_STATUS_DISCHARGING},0,-450000,1\n",
            BatteryHistoryStore.csv(listOf(sample))
        )
    }

    @Test
    fun missingReadingsStayEmptyInsteadOfZero() {
        val sample = BatterySample(
            atMillis = 0L,
            levelPercent = 5,
            voltageMv = null,
            temperatureC = null,
            status = BatteryManager.BATTERY_STATUS_FULL,
            plugged = BatteryManager.BATTERY_PLUGGED_USB,
            currentUa = null
        )

        assertEquals(
            "$header\n1970-01-01T00:00:00Z,0,5,,,${BatteryManager.BATTERY_STATUS_FULL}," +
                "${BatteryManager.BATTERY_PLUGGED_USB},,0\n",
            BatteryHistoryStore.csv(listOf(sample))
        )
    }

    @Test
    fun dischargingFlagFollowsPluggedStateNotStatusAlone() {
        val unpluggedFull = BatterySample(
            atMillis = 1L,
            levelPercent = 100,
            voltageMv = null,
            temperatureC = null,
            status = BatteryManager.BATTERY_STATUS_FULL,
            plugged = 0,
            currentUa = null
        )
        val pluggedWhileDischarging = BatterySample(
            atMillis = 2L,
            levelPercent = 40,
            voltageMv = null,
            temperatureC = null,
            status = BatteryManager.BATTERY_STATUS_DISCHARGING,
            plugged = BatteryManager.BATTERY_PLUGGED_AC,
            currentUa = null
        )

        val rows = BatteryHistoryStore.csv(listOf(unpluggedFull, pluggedWhileDischarging))
            .lines()
            .drop(1)

        assertEquals(0, rows[0].substringAfterLast(',').toInt())
        assertEquals(0, rows[1].substringAfterLast(',').toInt())
    }

    @Test
    fun oneLinePerSamplePlusHeader() {
        val samples = (1..3).map { index ->
            BatterySample(
                atMillis = index * 900_000L,
                levelPercent = 50 + index,
                voltageMv = 4_000,
                temperatureC = 30f,
                status = BatteryManager.BATTERY_STATUS_DISCHARGING,
                plugged = 0,
                currentUa = -100L
            )
        }

        val lines = BatteryHistoryStore.csv(samples).trimEnd('\n').lines()
        assertEquals(4, lines.size)
        assertEquals(header, lines.first())
    }
}
