/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The summary numbers the network screen shows are derived here, not stored:
 * a wrong rule would print a confident number for something that was never
 * measured. These tests pin the rules that carry a judgement:
 *
 *  - jitter needs two samples (one sample has no difference, and `0 ms` would
 *    claim a stability nobody measured);
 *  - failures never hide behind a successful average;
 *  - an empty list has no fastest/average/jitter at all.
 */
class LinkProbeTest {

    private fun report(
        connects: List<Long> = emptyList(),
        failures: Int = 0,
        attempts: Int = connects.size + failures,
        dnsMs: Long? = null
    ) = LinkReport(
        host = "example.com",
        port = 443,
        dnsMs = dnsMs,
        connectsMs = connects,
        failures = failures,
        attempts = attempts,
        lastFailure = if (failures > 0) "SocketTimeoutException" else null,
        validated = true,
        captivePortal = false,
        metered = false,
        transports = listOf("Wi-Fi"),
        downstreamKbps = 100_000,
        upstreamKbps = 20_000
    )

    @Test
    fun `empty list has no derived numbers`() {
        val sample = report()
        assertNull(sample.bestMs)
        assertNull(sample.averageMs)
        assertNull(sample.jitterMs)
        assertFalse(sample.allReached)
    }

    @Test
    fun `single sample reports best and average but no jitter`() {
        val sample = report(connects = listOf(42L))
        assertEquals(42L, sample.bestMs)
        assertEquals(42L, sample.averageMs)
        assertNull(sample.jitterMs)
        assertTrue(sample.allReached)
    }

    @Test
    fun `best is the minimum and average is the truncated mean`() {
        val sample = report(connects = listOf(40L, 30L, 50L))
        assertEquals(30L, sample.bestMs)
        // 120 / 3 = 40 exactly; a mean that rounded up would need 121.
        assertEquals(40L, sample.averageMs)
    }

    @Test
    fun `average truncates instead of rounding`() {
        val sample = report(connects = listOf(10L, 11L))
        // 21 / 2 = 10.5 — reported as 10, never as 11, so the number never
        // claims a millisecond that was not measured.
        assertEquals(10L, sample.averageMs)
    }

    @Test
    fun `jitter is the mean absolute step between consecutive samples`() {
        val sample = report(connects = listOf(10L, 30L, 20L))
        // |30-10| = 20 and |20-30| = 10 ⇒ 15.
        assertEquals(15L, sample.jitterMs)
    }

    @Test
    fun `steady samples have zero jitter because they were measured`() {
        val sample = report(connects = listOf(25L, 25L, 25L))
        assertEquals(0L, sample.jitterMs)
    }

    @Test
    fun `a failure disqualifies allReached even when samples succeeded`() {
        val sample = report(connects = listOf(20L, 21L), failures = 2, attempts = 4)
        assertEquals(2, sample.failures)
        assertFalse(sample.allReached)
        // The successful samples still produce numbers: they were measured.
        assertEquals(20L, sample.bestMs)
    }

    @Test
    fun `dns absence is preserved and not turned into zero`() {
        val sample = report(connects = listOf(12L), dnsMs = null)
        assertNull(sample.dnsMs)
    }
}
