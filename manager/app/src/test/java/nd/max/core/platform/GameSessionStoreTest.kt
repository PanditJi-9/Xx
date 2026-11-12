/*
 * Copyright (C) 2026-2027 Zexshia
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */

package nd.max.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * مخزن جلسات اللعب — الدوال الصافية وحدها.
 *
 * والملفّ والقرص لا يُختبَران هنا (يحتاجان `Context`)؛ ما يُقاس هو ما يمكن أن يفسد
 * صامتًا: الترتيب، والسقف، والتكرار، وشبه المنحرف، والتصغير، وأعمدة CSV.
 */
class GameSessionStoreTest {

    private fun session(
        id: String,
        startedAtMs: Long,
        packageName: String? = "com.example.game"
    ) = StoredGameSession(
        id = id,
        packageName = packageName,
        appLabel = "Example",
        startedAtMs = startedAtMs,
        endedAtMs = startedAtMs + 60_000L,
        spanMs = 60_000L,
        samples = 60
    )

    private fun point(
        atMs: Long,
        watt: Float? = null,
        jank: Int? = null,
        p99: Float? = null
    ) = SessionPoint(atMs = atMs, watt = watt, jankPercent = jank, frameP99Ms = p99)

    @Test
    fun `merge keeps newest first and removes the same id`() {
        val existing = listOf(session("a", 100L), session("b", 300L))
        val merged = merge(existing, session("a", 500L))

        assertEquals(listOf("a", "b"), merged.map { it.id })
        assertEquals(500L, merged.first().startedAtMs)
        assertEquals(2, merged.size)
    }

    @Test
    fun `merge refuses to grow past the cap and keeps the newest`() {
        val existing = (1..5).map { session("s$it", it.toLong()) }
        val merged = merge(existing, session("new", 99L), max = 3)

        assertEquals(3, merged.size)
        assertEquals(listOf("new", "s5", "s4"), merged.map { it.id })
    }

    @Test
    fun `merge with a zero cap has nowhere to put anything`() {
        assertTrue(merge(listOf(session("a", 1L)), session("b", 2L), max = 0).isEmpty())
    }

    @Test
    fun `downsample keeps both ends and never exceeds the cap`() {
        val samples = (0 until 1_000).map { point(it.toLong()) }
        val reduced = downsampleSamples(samples, maxPoints = 180)

        assertEquals(180, reduced.size)
        assertEquals(samples.first(), reduced.first())
        assertEquals(samples.last(), reduced.last())
    }

    @Test
    fun `downsample returns the list untouched when it already fits`() {
        val samples = (0 until 10).map { point(it.toLong()) }
        assertEquals(samples, downsampleSamples(samples, maxPoints = 180))
    }

    @Test
    fun `downsample to one point keeps the last reading not the first`() {
        val samples = (0 until 10).map { point(it.toLong()) }
        assertEquals(listOf(samples.last()), downsampleSamples(samples, maxPoints = 1))
    }

    @Test
    fun `energy is the trapezoid between two consecutive watts`() {
        // 2W ثم 4W على ثانية واحدة: (2+4)/2 × 1/3600 = 0.000833Wh
        val energy = sessionEnergyWh(listOf(point(0L, watt = 2f), point(1_000L, watt = 4f)))
        assertEquals(0.000833f, energy ?: 0f, 0.000001f)
    }

    @Test
    fun `energy refuses a gap it cannot integrate across`() {
        assertNull(sessionEnergyWh(listOf(point(0L, watt = 2f), point(20_000L, watt = 4f))))
    }

    @Test
    fun `energy without a pair of readings is absent not zero`() {
        assertNull(sessionEnergyWh(listOf(point(0L, watt = 2f))))
        assertNull(sessionEnergyWh(emptyList()))
    }

    @Test
    fun `jank and p99 average only what was actually measured`() {
        val samples = listOf(
            point(0L, jank = 10, p99 = 16f),
            point(1_000L, jank = 20, p99 = 20f),
            point(2_000L, jank = 31)
        )
        assertEquals(20, sessionJankAverage(samples))
        assertEquals(18f, sessionP99Average(samples) ?: 0f, 0.0001f)
        assertNull(sessionJankAverage(listOf(point(0L))))
        assertNull(sessionP99Average(listOf(point(0L))))
    }

    @Test
    fun `stored csv writes the header, every column, and empty absent cells`() {
        val session = StoredGameSession(
            id = "x",
            packageName = "com.example.game",
            startedAtMs = 1_000L,
            endedAtMs = 3_000L,
            spanMs = 2_000L,
            samples = 2,
            renderer = "Vulkan",
            series = listOf(
                SessionPoint(atMs = 1_000L, frames = 59.5f),
                SessionPoint(atMs = 3_000L, frames = 61f, watt = 3.25f, jankPercent = 4)
            )
        )
        val lines = storedSessionCsv(session).trim().lines()

        assertEquals(SESSION_CSV_HEADER, lines.first())
        assertEquals(3, lines.size)
        lines.drop(1).forEach { assertEquals(SESSION_CSV_HEADER.split(",").size, it.split(',').size) }
        assertEquals("0,59.5,,,,,,,Vulkan,,,,", lines[1])
        assertEquals("2000,61.0,,,3.25,,4,,Vulkan,,,,", lines[2])
    }
}
