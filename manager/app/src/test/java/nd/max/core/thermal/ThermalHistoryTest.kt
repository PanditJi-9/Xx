/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.thermal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * قواعد تاريخ الحرارة — الحدود لا الشكل السعيد وحده.
 *
 * **ولماذا:** هذا الملفّ يُنتج CSV يُفتح في جدول، فإن انقلب عمود صامتًا صار الرقم في غير
 * معناه، وإن نُسي القصّ كبر الملفّ بلا سقف. وكل قاعدة هنا تقيس شيئًا لا يراه المُصرّف.
 */
class ThermalHistoryTest {

    private fun sample(atMs: Long, zone: String = "cpu-0", celsius: Int = 50) =
        ThermalSessionSample(atMs = atMs, zone = zone, celsius = celsius)

    @Test
    fun `push keeps the newest samples and drops the oldest`() {
        var history = emptyList<ThermalSessionSample>()
        repeat(ThermalHistory.CAPACITY + 5) { index ->
            history = ThermalHistory.push(history, sample(atMs = index * 3_000L, celsius = index))
        }
        assertEquals(ThermalHistory.CAPACITY, history.size)
        assertEquals(5, history.first().celsius)
        assertEquals(ThermalHistory.CAPACITY + 4, history.last().celsius)
    }

    @Test
    fun `empty session yields the header alone`() {
        assertEquals(ThermalHistory.CSV_HEADER + "\n", ThermalHistory.csv(emptyList()))
    }

    @Test
    fun `elapsed is relative to the first sample`() {
        val csv = ThermalHistory.csv(
            listOf(
                sample(atMs = 1_000_000L, celsius = 40),
                sample(atMs = 1_003_000L, celsius = 41),
                sample(atMs = 1_009_500L, celsius = 42)
            )
        )
        val rows = csv.trim().lines()
        assertEquals(ThermalHistory.CSV_HEADER, rows[0])
        assertTrue(rows[1].startsWith("0,cpu-0,40"))
        assertTrue(rows[2].startsWith("3000,cpu-0,41"))
        assertTrue(rows[3].startsWith("9500,cpu-0,42"))
    }

    @Test
    fun `a zone containing a comma is quoted so columns cannot shift`() {
        val csv = ThermalHistory.csv(listOf(sample(zone = "mtk-ts,0", celsius = 61)))
        assertTrue(csv.lines()[1].endsWith(",\"mtk-ts,0\",61"))
    }

    @Test
    fun `a quote inside a zone is doubled`() {
        val csv = ThermalHistory.csv(listOf(sample(zone = "soc \"main\"", celsius = 55)))
        assertTrue(csv.lines()[1].endsWith(",\"soc \"\"main\"\"\",55"))
    }

    @Test
    fun `a missing zone leaves an empty field rather than a placeholder`() {
        val csv = ThermalHistory.csv(listOf(sample(zone = "", celsius = 44)))
        assertEquals("0,,44", csv.lines()[1])
    }

    @Test
    fun `window is measured from the samples, not assumed from their count`() {
        assertEquals(0, ThermalHistory.windowSeconds(emptyList(), 3_000L))
        assertEquals(3, ThermalHistory.windowSeconds(listOf(sample(0)), 3_000L))
        // ثلاث عيّنات لكن الدورة تأخّرت: ٤٧ ثانية لا ٩.
        assertEquals(
            47,
            ThermalHistory.windowSeconds(
                listOf(sample(0), sample(20_000L), sample(47_400L)),
                3_000L
            )
        )
    }
}
