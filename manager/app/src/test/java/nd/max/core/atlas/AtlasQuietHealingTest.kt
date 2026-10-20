/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.atlas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The quiet-healing policy (`ATLAS-SELFHEAL-01` · S2), decided without a device.
 *
 * Every rule the coordinator acts on is a pure function here, so the owner-chosen safety boundary —
 * proven route only, value carried by the event, unknown state halts — is enforced by tests before
 * any hardware exists in the picture.
 */
class AtlasQuietHealingTest {

    private val target = AtlasControlTarget.GPU_FREQUENCY
    private val other = AtlasControlTarget.CPU_FREQUENCY

    private fun drift(expected: String? = "490000000", live: String? = "305000000") =
        AtlasQuietEvent.OwnershipDrift(target = target, liveValue = live, expectedValue = expected)

    private val proven = mapOf(target to "gpu-devfreq-max")

    // ---- decide ---------------------------------------------------------------------------------

    @Test
    fun `a drift on a proven target carries the proven route and the expected value`() {
        val decision = AtlasQuietPolicy.decide(drift(), proven, AtlasQuietState(), nowMs = 1_000L)

        assertEquals(AtlasQuietDecision.ProposeRepair(target, "gpu-devfreq-max", "490000000"), decision)
    }

    @Test
    fun `a drift without a proven route stays a click`() {
        assertEquals(
            AtlasQuietDecision.StaySilent(AtlasQuietSilence.NOT_PROVEN),
            AtlasQuietPolicy.decide(drift(), emptyMap(), AtlasQuietState(), nowMs = 1_000L),
        )
    }

    @Test
    fun `a drift without an expected value contains nothing to restore`() {
        assertEquals(
            AtlasQuietDecision.StaySilent(AtlasQuietSilence.NO_EXPECTED_VALUE),
            AtlasQuietPolicy.decide(drift(expected = null), proven, AtlasQuietState(), nowMs = 1_000L),
        )
    }

    @Test
    fun `a halted target is refused before every other question`() {
        // No expected value and no proven route either: HALTED must still be the reason, because it is
        // the one that must never be mistaken for "just needs a value".
        val halted = AtlasQuietState(halted = setOf(target))

        assertEquals(
            AtlasQuietDecision.StaySilent(AtlasQuietSilence.HALTED),
            AtlasQuietPolicy.decide(drift(expected = null), emptyMap(), halted, nowMs = 1_000L),
        )
    }

    @Test
    fun `a target already in flight is not repaired twice`() {
        val state = AtlasQuietState(inFlight = setOf(target))

        assertEquals(
            AtlasQuietDecision.StaySilent(AtlasQuietSilence.IN_FLIGHT),
            AtlasQuietPolicy.decide(drift(), proven, state, nowMs = 1_000L),
        )
    }

    @Test
    fun `the rate limit blocks a second repair inside the interval and releases it at the boundary`() {
        val state = AtlasQuietState(lastRepairAtMs = mapOf(target to 1_000L))

        assertEquals(
            AtlasQuietDecision.StaySilent(AtlasQuietSilence.RATE_LIMITED),
            AtlasQuietPolicy.decide(
                drift(),
                proven,
                state,
                nowMs = 1_000L + AtlasQuietPolicy.REPAIR_MIN_INTERVAL_MS - 1,
            ),
        )
        assertTrue(
            AtlasQuietPolicy.decide(drift(), proven, state, nowMs = 1_000L + AtlasQuietPolicy.REPAIR_MIN_INTERVAL_MS)
                is AtlasQuietDecision.ProposeRepair,
        )
    }

    @Test
    fun `a clock that moved backwards keeps the target rate limited`() {
        val state = AtlasQuietState(lastRepairAtMs = mapOf(target to 5_000L))

        assertEquals(
            AtlasQuietDecision.StaySilent(AtlasQuietSilence.RATE_LIMITED),
            AtlasQuietPolicy.decide(drift(), proven, state, nowMs = 4_000L),
        )
    }

    @Test
    fun `foreground, explicit intent and generation change are recognized but not actionable`() {
        val events = listOf(
            AtlasQuietEvent.ForegroundChanged("com.example.game"),
            AtlasQuietEvent.ControlIntent(
                AtlasControlIntent(target = target, goal = AtlasControlGoal.PERFORMANCE),
            ),
            AtlasQuietEvent.GenerationChanged(bootGeneration = 7L, privilegeGeneration = 1L),
        )

        events.forEach { event ->
            assertEquals(
                AtlasQuietDecision.StaySilent(AtlasQuietSilence.NOT_ACTIONABLE),
                AtlasQuietPolicy.decide(event, proven, AtlasQuietState(), nowMs = 1_000L),
            )
        }
    }

    // ---- reduce ---------------------------------------------------------------------------------

    @Test
    fun `a verified repair counts once, clears its streak and never escalates`() {
        val before = AtlasQuietState(
            inFlight = setOf(target),
            consecutiveFailures = mapOf(target to 2),
        )

        val after = AtlasQuietPolicy.reduce(before, target, AtlasQuietRepairOutcome.VERIFIED, nowMs = 9_000L)

        assertEquals(1, after.silentRepairs)
        assertNull(after.escalation)
        assertTrue(after.inFlight.isEmpty())
        assertFalse(after.consecutiveFailures.containsKey(target))
        assertEquals(9_000L, after.lastRepairAtMs[target])
    }

    @Test
    fun `a verified repair clears the escalation it outlived, and only its own`() {
        val escalated = AtlasQuietState(
            escalation = AtlasQuietEscalation(
                target = target,
                cause = AtlasQuietRepairOutcome.BLOCKED,
                consecutive = 3,
                whenMs = 1_000L,
            ),
        )

        assertNull(
            AtlasQuietPolicy.reduce(escalated, target, AtlasQuietRepairOutcome.VERIFIED, nowMs = 2_000L).escalation,
        )
        assertEquals(
            escalated.escalation,
            AtlasQuietPolicy.reduce(escalated, other, AtlasQuietRepairOutcome.VERIFIED, nowMs = 2_000L).escalation,
        )
    }

    @Test
    fun `two rolled-back attempts stop being quiet, not one`() {
        val first = AtlasQuietPolicy.reduce(
            AtlasQuietState(),
            target,
            AtlasQuietRepairOutcome.NOT_TAKEN_ROLLED_BACK,
            nowMs = 1_000L,
        )
        assertNull(first.escalation)

        val second = AtlasQuietPolicy.reduce(
            first,
            target,
            AtlasQuietRepairOutcome.NOT_TAKEN_ROLLED_BACK,
            nowMs = 2_000L,
        )
        assertEquals(
            AtlasQuietEscalation(target, AtlasQuietRepairOutcome.NOT_TAKEN_ROLLED_BACK, consecutive = 2, whenMs = 2_000L),
            second.escalation,
        )
    }

    @Test
    fun `three clean blocks stop being quiet, not two`() {
        var state = AtlasQuietState()
        state = AtlasQuietPolicy.reduce(state, target, AtlasQuietRepairOutcome.BLOCKED, nowMs = 1_000L)
        state = AtlasQuietPolicy.reduce(state, target, AtlasQuietRepairOutcome.BLOCKED, nowMs = 2_000L)
        assertNull(state.escalation)

        state = AtlasQuietPolicy.reduce(state, target, AtlasQuietRepairOutcome.BLOCKED, nowMs = 3_000L)
        assertEquals(AtlasQuietRepairOutcome.BLOCKED, state.escalation?.cause)
        assertEquals(3, state.escalation?.consecutive)
    }

    @Test
    fun `an unverified rollback halts the target at once`() {
        val state = AtlasQuietPolicy.reduce(
            AtlasQuietState(),
            target,
            AtlasQuietRepairOutcome.UNKNOWN_STATE,
            nowMs = 1_000L,
        )

        assertTrue(target in state.halted)
        assertEquals(AtlasQuietRepairOutcome.UNKNOWN_STATE, state.escalation?.cause)
        assertEquals(
            AtlasQuietDecision.StaySilent(AtlasQuietSilence.HALTED),
            AtlasQuietPolicy.decide(drift(), proven, state, nowMs = 999_000L),
        )
    }

    // ---- regenerate / acknowledge ---------------------------------------------------------------

    @Test
    fun `a generation change voids provenance and hazard but keeps the rate-limit memory and the count`() {
        val state = AtlasQuietState(
            lastRepairAtMs = mapOf(target to 5_000L),
            inFlight = setOf(target),
            consecutiveFailures = mapOf(target to 2),
            halted = setOf(target),
            silentRepairs = 4,
            escalation = AtlasQuietEscalation(target, AtlasQuietRepairOutcome.UNKNOWN_STATE, 1, 5_000L),
        )

        val fresh = AtlasQuietPolicy.regenerate(state)

        assertTrue(fresh.halted.isEmpty())
        assertNull(fresh.escalation)
        assertTrue(fresh.inFlight.isEmpty())
        assertTrue(fresh.consecutiveFailures.isEmpty())
        assertEquals(5_000L, fresh.lastRepairAtMs[target])
        assertEquals(4, fresh.silentRepairs)
    }

    @Test
    fun `acknowledgement un-halts, clears the escalation, and the next failure escalates sooner`() {
        val halted = AtlasQuietState(
            consecutiveFailures = mapOf(target to 1),
            halted = setOf(target),
            escalation = AtlasQuietEscalation(target, AtlasQuietRepairOutcome.UNKNOWN_STATE, 1, 5_000L),
        )

        val acknowledged = AtlasQuietPolicy.acknowledge(halted)

        assertNull(acknowledged.escalation)
        assertTrue(acknowledged.halted.isEmpty())
        val failed = AtlasQuietPolicy.reduce(
            acknowledged,
            target,
            AtlasQuietRepairOutcome.NOT_TAKEN_ROLLED_BACK,
            nowMs = 6_000L,
        )
        assertEquals(2, failed.escalation?.consecutive)
    }

    // ---- construction guards --------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `a proposed repair refuses a blank route`() {
        AtlasQuietDecision.ProposeRepair(target, "  ", "490000000")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a proposed repair refuses a blank value`() {
        AtlasQuietDecision.ProposeRepair(target, "gpu-devfreq-max", " ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an escalation refuses to represent a success`() {
        AtlasQuietEscalation(target, AtlasQuietRepairOutcome.VERIFIED, consecutive = 1, whenMs = 1_000L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a drift value beyond the bounded size is refused`() {
        AtlasQuietEvent.OwnershipDrift(target, liveValue = null, expectedValue = "x".repeat(129))
    }
}
