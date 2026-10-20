/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.atlas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The quiet-healing coordinator (`ATLAS-SELFHEAL-01` · S2), without hardware and without a screen.
 *
 * The behaviors pinned here are the ones the owner's ask rests on: nothing runs on construction or
 * without an event (the silent lifecycle G-UX-02 asks for), a proven drift repairs exactly once and
 * silently (G-UX-01), an unknown state surfaces once and halts, and the seam is the only thing that
 * can be called.
 */
class AtlasSessionCoordinatorTest {

    private val target = AtlasControlTarget.CPU_FREQUENCY
    private val route = "cpu-policy0-max"

    private fun drift(expected: String? = "1800000") =
        AtlasQuietEvent.OwnershipDrift(target = target, liveValue = "2400000", expectedValue = expected)

    /** Records every call; returns queued outcomes (default VERIFIED), or throws if asked to. */
    private class FakeRepair(
        private val outcomes: ArrayDeque<AtlasQuietRepairOutcome> = ArrayDeque(),
    ) : AtlasQuietRepair {
        val calls = mutableListOf<Triple<AtlasControlTarget, String, String>>()
        var failure: Throwable? = null

        override fun repair(
            target: AtlasControlTarget,
            routeId: String,
            expectedValue: String,
        ): AtlasQuietRepairOutcome {
            calls += Triple(target, routeId, expectedValue)
            failure?.let { throw it }
            return outcomes.removeFirstOrNull() ?: AtlasQuietRepairOutcome.VERIFIED
        }
    }

    private fun coordinator(
        repair: AtlasQuietRepair,
        proven: Map<AtlasControlTarget, String> = mapOf(target to route),
        clock: () -> Long = { 1_000L },
    ) = AtlasSessionCoordinator(provenOf = { proven }, repair = repair, clockMs = clock)

    // ---- quiet by default -----------------------------------------------------------------------

    @Test
    fun `construction performs no repair, no clock read and leaves the state empty`() {
        var clockReads = 0
        val repair = FakeRepair()

        val session = coordinator(repair, clock = { clockReads += 1; 1_000L })

        assertTrue(repair.calls.isEmpty())
        assertEquals(AtlasQuietState(), session.state())
        assertEquals(0, clockReads)
    }

    @Test
    fun `a proven drift repairs once, with the route and the expected value, and stays quiet`() {
        val repair = FakeRepair()
        val session = coordinator(repair)

        session.onEvent(drift())

        assertEquals(listOf(Triple(target, route, "1800000")), repair.calls)
        assertEquals(1, session.state().silentRepairs)
        assertNull(session.state().escalation)
        assertTrue(session.state().inFlight.isEmpty())
    }

    @Test
    fun `an unproven drift calls nothing and changes nothing`() {
        val repair = FakeRepair()
        val session = coordinator(repair, proven = emptyMap())

        session.onEvent(drift())

        assertTrue(repair.calls.isEmpty())
        assertEquals(AtlasQuietState(), session.state())
    }

    @Test
    fun `foreground change and explicit intent never repair`() {
        val repair = FakeRepair()
        val session = coordinator(repair)

        session.onEvent(AtlasQuietEvent.ForegroundChanged("com.example.game"))
        session.onEvent(
            AtlasQuietEvent.ControlIntent(AtlasControlIntent(target = target, goal = AtlasControlGoal.PERFORMANCE)),
        )

        assertTrue(repair.calls.isEmpty())
        assertEquals(AtlasQuietState(), session.state())
    }

    // ---- failures leave quiet mode once ---------------------------------------------------------

    @Test
    fun `an unverified rollback surfaces once, halts the target and stops repairing`() {
        val repair = FakeRepair(ArrayDeque(listOf(AtlasQuietRepairOutcome.UNKNOWN_STATE)))
        val session = coordinator(repair)

        session.onEvent(drift())

        assertEquals(AtlasQuietRepairOutcome.UNKNOWN_STATE, session.state().escalation?.cause)
        assertTrue(target in session.state().halted)

        session.onEvent(drift())
        assertEquals(1, repair.calls.size)
    }

    @Test
    fun `a repair that throws is folded back as unknown state`() {
        val repair = FakeRepair().apply { failure = IllegalStateException("io") }
        val session = coordinator(repair)

        session.onEvent(drift())

        assertEquals(AtlasQuietRepairOutcome.UNKNOWN_STATE, session.state().escalation?.cause)
        assertTrue(target in session.state().halted)
    }

    // ---- pacing ---------------------------------------------------------------------------------

    @Test
    fun `a second drift inside the interval waits for the clock`() {
        var now = 10_000L
        val repair = FakeRepair()
        val session = coordinator(repair, clock = { now })

        session.onEvent(drift())
        session.onEvent(drift())
        assertEquals(1, repair.calls.size)

        now += AtlasQuietPolicy.REPAIR_MIN_INTERVAL_MS
        session.onEvent(drift())
        assertEquals(2, repair.calls.size)
    }

    @Test
    fun `a repair that re-enters for the same target is refused, not doubled`() {
        var session: AtlasSessionCoordinator? = null
        var reentered = false
        val repair = object : AtlasQuietRepair {
            var calls = 0

            override fun repair(
                target: AtlasControlTarget,
                routeId: String,
                expectedValue: String,
            ): AtlasQuietRepairOutcome {
                calls += 1
                if (!reentered) {
                    reentered = true
                    // حدث يعود من داخل الإصلاح نفسه: يجب أن يُرفض بـIN_FLIGHT ولا يُدخل إصلاحًا ثانيًا.
                    session?.onEvent(drift())
                }
                return AtlasQuietRepairOutcome.VERIFIED
            }
        }
        val subject = coordinator(repair)
        session = subject

        subject.onEvent(drift())

        assertEquals(1, repair.calls)
        assertEquals(1, subject.state().silentRepairs)
    }

    // ---- generation change and acknowledgement --------------------------------------------------

    @Test
    fun `a generation change resets the session without repairing`() {
        val repair = FakeRepair(ArrayDeque(listOf(AtlasQuietRepairOutcome.UNKNOWN_STATE)))
        val session = coordinator(repair)
        session.onEvent(drift())

        session.onEvent(AtlasQuietEvent.GenerationChanged(bootGeneration = 8L, privilegeGeneration = 1L))

        val state = session.state()
        assertNull(state.escalation)
        assertTrue(state.halted.isEmpty())
        assertEquals(1, repair.calls.size)
    }

    @Test
    fun `acknowledgement un-halts and lets quiet repair resume`() {
        var now = 1_000L
        val repair = FakeRepair(
            ArrayDeque(listOf(AtlasQuietRepairOutcome.UNKNOWN_STATE, AtlasQuietRepairOutcome.VERIFIED)),
        )
        val session = coordinator(repair, clock = { now })

        session.onEvent(drift())
        session.onEvent(drift())
        assertEquals(1, repair.calls.size)

        session.acknowledgeEscalation()
        now += AtlasQuietPolicy.REPAIR_MIN_INTERVAL_MS
        session.onEvent(drift())

        assertEquals(2, repair.calls.size)
        assertEquals(1, session.state().silentRepairs)
        assertNull(session.state().escalation)
    }

    // ---- the proven-set helper ------------------------------------------------------------------

    @Test
    fun `provenFrom asks the store with the same lowercase keys MaxAtlas uses`() {
        val asked = mutableListOf<String>()
        val proven = provenFrom(
            lookupRoute = { key ->
                asked += key
                if (key == "cpu_frequency") "cpu-policy0-max" else null
            },
        )

        assertEquals(mapOf(target to "cpu-policy0-max"), proven())
        assertTrue(asked.contains("cpu_frequency"))
        assertTrue(asked.contains("gpu_frequency"))
    }
}
