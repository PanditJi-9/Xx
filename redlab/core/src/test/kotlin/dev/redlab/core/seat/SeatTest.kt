package dev.redlab.core.seat

import dev.redlab.core.capability.Access
import dev.redlab.core.capability.Capability
import dev.redlab.core.capability.CapabilityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeatTest {

    /** A shell that pretends to be `su`: answers `id -u` with 0 and records what it was asked. */
    private class FakeShell(
        private val uid: String,
        private val failOn: String? = null,
    ) : Shell {
        val calls = mutableListOf<List<String>>()

        override fun run(command: List<String>): ShellResult {
            calls += command
            if (failOn != null && command.contains(failOn)) {
                return ShellResult(1, "", "not found")
            }
            return when (command.firstOrNull()) {
                "id" -> ShellResult(0, uid + "\n", "")
                "cat" -> ShellResult(0, "1800000\n", "")
                else -> ShellResult(0, "", "")
            }
        }
    }

    private fun root(uid: String = "0", failOn: String? = null): ControlSeat =
        ShellSeat(Tier.ROOT, FakeShell(uid, failOn))

    @Test
    fun `the strongest working seat wins and refusals keep their reason`() {
        val negotiation = SeatNegotiator(root = { root() }, adb = { root("2000") }).negotiate()
        assertEquals(Tier.ROOT, negotiation.tier)
        assertTrue(negotiation.rejected.isEmpty())
    }

    @Test
    fun `a root bridge that cannot answer id is rejected, not trusted`() {
        val negotiation = SeatNegotiator(
            root = { root(uid = "", failOn = null) },
            adb = { root("2000") },
        ).negotiate()
        assertEquals(Tier.ADB, negotiation.tier)
        assertTrue(negotiation.rejected[Tier.ROOT]!!.contains("id -u"))
    }

    @Test
    fun `a missing shizuku bridge is named, and local is the floor`() {
        val negotiation = SeatNegotiator(root = null, adb = null).negotiate()
        assertEquals(Tier.LOCAL, negotiation.tier)
        assertEquals("no shizuku bridge in this build", negotiation.rejected[Tier.ADB])
        assertEquals("no su bridge in this build", negotiation.rejected[Tier.ROOT])
    }

    @Test
    fun `a preferred weaker tier is honoured`() {
        val negotiation = SeatNegotiator(root = { root() }, adb = { root("2000") }).negotiate(Tier.LOCAL)
        assertEquals(Tier.LOCAL, negotiation.tier)
    }

    @Test
    fun `the local seat refuses to run a shell instead of pretending`() {
        val result = LocalSeat().exec(listOf("cat", "/sys/x"))
        assertFalse(result.ok)
        assertEquals(-1, result.exitCode)
        assertTrue(result.stderr.contains("LOCAL"))
    }

    @Test
    fun `a value that would inject a command is refused before it reaches the shell`() {
        val shell = FakeShell("0")
        val io = ShellNodeIo(shell)
        assertFalse(io.write("/sys/class/kgsl/kgsl-3d0/max_gpuclk", "100; rm -rf /"))
        assertFalse(io.write("/sys/class/kgsl/kgsl-3d0/max_gpuclk", "100\nreboot"))
        assertTrue(shell.calls.isEmpty())
        assertTrue(io.write("/sys/class/kgsl/kgsl-3d0/max_gpuclk", "680000000"))
        assertEquals(1, shell.calls.size)
    }

    @Test
    fun `a quoted path is never written`() {
        assertNull(ShellNodeIo(FakeShell("0")).read("/sys/x'; reboot; '"))
    }

    @Test
    fun `a capability needing root degrades on a local seat with the reason kept`() {
        val cap = Capability(
            id = "gpu.max_clock",
            owner = "gameassist",
            state = CapabilityState.AVAILABLE,
            access = Access.READ_WRITE,
            evidence = "wrote 680000000 and re-read it",
            requiredTier = Tier.ROOT,
        )
        val graded = cap.gradedFor(Tier.LOCAL, atMillis = 5)
        assertEquals(CapabilityState.DEGRADED, graded.state)
        assertEquals(Access.READ_ONLY, graded.access)
        assertEquals("needs ROOT access, running at LOCAL", graded.reason)
        assertEquals(5, graded.measuredAtMillis)
    }

    @Test
    fun `a capability is untouched when the seat is strong enough`() {
        val cap = Capability(
            id = "cpu.governor",
            owner = "gamespace",
            state = CapabilityState.AVAILABLE,
            access = Access.READ_WRITE,
            evidence = "governor write accepted and re-read",
            requiredTier = Tier.ADB,
        )
        assertEquals(cap, cap.gradedFor(Tier.ROOT, atMillis = 9))
    }

    @Test
    fun `changes that omit the seat model keep working`() {
        val cap = Capability(id = "screen.refresh", owner = "gamespace")
        assertNull(cap.requiredTier)
        assertEquals(cap, cap.gradedFor(Tier.LOCAL, atMillis = 1))
    }
}
