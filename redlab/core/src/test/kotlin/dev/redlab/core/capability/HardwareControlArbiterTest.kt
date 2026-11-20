package dev.redlab.core.capability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HardwareControlArbiterTest {

    private val capabilityId = "gamespace.perf.cpu_max_freq"
    private lateinit var registry: CapabilityRegistry
    private lateinit var arbiter: HardwareControlArbiter
    private val granted = mutableListOf<ClaimToken>()
    private val preempted = mutableListOf<ClaimToken>()

    @Before
    fun setUp() {
        registry = CapabilityRegistry()
        registry.declare(Capability(capabilityId, owner = "gamespace"))
        registry.record(
            Capability(
                capabilityId,
                owner = "gamespace",
                state = CapabilityState.AVAILABLE,
                evidence = "/sys/.../scaling_max_freq read and written",
            )
        )
        arbiter = HardwareControlArbiter(registry)
        arbiter.onGranted = { granted += it }
        arbiter.onPreempted = { preempted += it }
    }

    @Test
    fun `an UNKNOWN capability cannot be claimed`() {
        registry.declare(Capability("gamespace.perf.gpu_freq", owner = "gamespace"))
        val result = arbiter.claim(HardwareRequest("gamespace.perf.gpu_freq", "gamespace", Priority.GAME))
        assertTrue(result is ClaimResult.Unsupported)
        assertTrue((result as ClaimResult.Unsupported).reason.contains("UNKNOWN"))
        assertTrue(granted.isEmpty())
    }

    @Test
    fun `one owner at a time - second equal priority is denied and named`() {
        val first = arbiter.claim(HardwareRequest(capabilityId, "gamespace", Priority.GAME)) as ClaimResult.Granted
        val second = arbiter.claim(HardwareRequest(capabilityId, "widget", Priority.GAME))
        assertTrue(second is ClaimResult.Denied)
        assertEquals("gamespace", (second as ClaimResult.Denied).holder)
        assertEquals(first.token, arbiter.holders()[capabilityId])
    }

    @Test
    fun `higher priority preempts and the loser is told`() {
        val game = (arbiter.claim(HardwareRequest(capabilityId, "gamespace", Priority.GAME)) as ClaimResult.Granted).token
        val user = arbiter.claim(HardwareRequest(capabilityId, "user-panel", Priority.USER))
        assertTrue(user is ClaimResult.Granted)
        assertEquals(listOf(game), preempted)
        assertEquals("user-panel", arbiter.holders()[capabilityId]!!.requester)
    }

    @Test
    fun `SAFETY is not preemptable by anything`() {
        arbiter.claim(HardwareRequest(capabilityId, "thermal", Priority.SAFETY))
        val challenger = arbiter.claim(HardwareRequest(capabilityId, "gamespace", Priority.USER))
        assertTrue(challenger is ClaimResult.Denied)
        assertEquals("thermal", arbiter.holders()[capabilityId]!!.requester)
    }

    @Test
    fun `a tripped safety ceiling freezes every non-safety claim`() {
        arbiter.claim(HardwareRequest(capabilityId, "gamespace", Priority.GAME))
        arbiter.tripSafety("battery 46C")
        assertTrue(arbiter.claim(HardwareRequest(capabilityId, "user-panel", Priority.USER)) is ClaimResult.Unsupported)
        assertTrue(arbiter.claim(HardwareRequest(capabilityId, "thermal", Priority.SAFETY)) is ClaimResult.Granted)
    }

    @Test
    fun `only the current token can release, and release hands back to the preempted holder`() {
        val game = (arbiter.claim(HardwareRequest(capabilityId, "gamespace", Priority.GAME)) as ClaimResult.Granted).token
        arbiter.claim(HardwareRequest(capabilityId, "user-panel", Priority.USER))

        assertFalse(arbiter.release(game))
        assertTrue(arbiter.holders().containsKey(capabilityId))

        val user = arbiter.holders()[capabilityId]!!
        assertTrue(arbiter.release(user))
        assertEquals("gamespace", arbiter.holders()[capabilityId]!!.requester)
    }

    @Test
    fun `a manual user lock blocks automated owners`() {
        arbiter.lockByUser(capabilityId)
        val blocked = arbiter.claim(HardwareRequest(capabilityId, "gamespace", Priority.GAME))
        assertTrue(blocked is ClaimResult.BlockedByManualLock)
        assertEquals("user", (blocked as ClaimResult.BlockedByManualLock).lockedBy)

        arbiter.unlock(capabilityId)
        assertTrue(arbiter.claim(HardwareRequest(capabilityId, "gamespace", Priority.GAME)) is ClaimResult.Granted)
    }

    @Test
    fun `SAFETY is never blocked by a manual lock`() {
        arbiter.lockByUser(capabilityId)
        assertTrue(arbiter.claim(HardwareRequest(capabilityId, "thermal", Priority.SAFETY)) is ClaimResult.Granted)
    }

    @Test
    fun `a stale token cannot release a newer claim`() {
        val old = (arbiter.claim(HardwareRequest(capabilityId, "gamespace", Priority.GAME)) as ClaimResult.Granted).token
        arbiter.release(old)
        arbiter.claim(HardwareRequest(capabilityId, "gamespace", Priority.GAME))
        assertFalse(arbiter.release(old))
        assertTrue(arbiter.holders().containsKey(capabilityId))
    }
}
