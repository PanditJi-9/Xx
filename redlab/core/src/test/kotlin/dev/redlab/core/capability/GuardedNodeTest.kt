package dev.redlab.core.capability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardedNodeTest {

    /** In-memory sysfs: a map plus a write log, and switches to simulate refusal. */
    private class FakeIo(
        private val nodes: MutableMap<String, String> = mutableMapOf(),
        var refuseWrites: Boolean = false,
    ) : NodeIo {
        val writes = mutableListOf<Pair<String, String>>()

        override fun read(path: String): String? = nodes[path]

        override fun write(path: String, value: String): Boolean {
            if (refuseWrites) return false
            nodes[path] = value
            writes += path to value
            return true
        }
    }

    private val path = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq"

    @Test
    fun `an unreadable node is never written`() {
        val io = FakeIo()
        val node = GuardedNode(io, path)
        val outcome = node.apply("1800000")
        assertTrue(outcome is WriteOutcome.Unsupported)
        assertTrue(io.writes.isEmpty())
    }

    @Test
    fun `a value the kernel does not offer is refused before any read or write`() {
        val io = FakeIo(mutableMapOf(path to "1200000"))
        val node = GuardedNode(io, path)
        val outcome = node.apply("999999999", allowed = setOf("1200000", "1800000", "2400000"))
        assertTrue(outcome is WriteOutcome.Rejected)
        assertTrue((outcome as WriteOutcome.Rejected).reason.contains("999999999"))
        assertTrue(io.writes.isEmpty())
    }

    @Test
    fun `writing the current value is recorded but not performed`() {
        val io = FakeIo(mutableMapOf(path to "1800000"))
        val node = GuardedNode(io, path)
        val outcome = node.apply("1800000", allowed = setOf("1800000"))
        assertEquals(WriteOutcome.Applied("1800000"), outcome)
        assertTrue(io.writes.isEmpty())
        assertTrue(node.isHolding)
    }

    @Test
    fun `restore puts back the FIRST value seen, not an intermediate one`() {
        val io = FakeIo(mutableMapOf(path to "1200000"))
        val node = GuardedNode(io, path)

        node.apply("1800000", allowed = setOf("1200000", "1800000", "2400000"))
        node.apply("2400000", allowed = setOf("1200000", "1800000", "2400000"))

        val restored = node.restore()
        assertEquals(GuardedNode.RestoreOutcome.Restored("1200000"), restored)
        assertEquals("1200000", io.read(path))
        assertFalse(node.isHolding)
    }

    @Test
    fun `a refused write reports UNSUPPORTED and leaves no claim`() {
        val io = FakeIo(mutableMapOf(path to "1200000"), refuseWrites = true)
        val node = GuardedNode(io, path)
        val outcome = node.apply("1800000")
        assertTrue(outcome is WriteOutcome.Unsupported)
        assertTrue((outcome as WriteOutcome.Unsupported).reason.contains("refused"))
        assertFalse(node.isHolding)
        assertEquals(GuardedNode.RestoreOutcome.NothingToDo, node.restore())
    }

    @Test
    fun `restore failure is reported, not swallowed`() {
        val io = FakeIo(mutableMapOf(path to "1200000"))
        val node = GuardedNode(io, path)
        node.apply("1800000")
        io.refuseWrites = true
        assertTrue(node.restore() is GuardedNode.RestoreOutcome.Failed)
    }
}
