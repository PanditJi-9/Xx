package dev.redlab.core.capability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CapabilityRegistryTest {

    private val registry = CapabilityRegistry()

    @Test
    fun `undeclared capability is UNKNOWN, not broken and not working`() {
        assertEquals(CapabilityState.UNKNOWN, registry.stateOf("gamespace.perf.cpu_max_freq"))
        assertNull(registry.get("gamespace.perf.cpu_max_freq"))
    }

    @Test
    fun `declared capability starts UNKNOWN and keeps its owner`() {
        registry.declare(Capability("gamespace.perf.cpu_max_freq", owner = "gamespace"))
        val capability = registry.get("gamespace.perf.cpu_max_freq")!!
        assertEquals(CapabilityState.UNKNOWN, capability.state)
        assertEquals("gamespace", capability.owner)
    }

    @Test
    fun `AVAILABLE without evidence is refused by the model`() {
        try {
            Capability("x.y", owner = "x", state = CapabilityState.AVAILABLE)
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("evidence"))
        }
    }

    @Test
    fun `UNSUPPORTED without a reason is refused by the model`() {
        try {
            Capability("x.y", owner = "x", state = CapabilityState.UNSUPPORTED)
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("reason"))
        }
    }

    @Test
    fun `recording keeps the failure reason verbatim`() {
        registry.declare(Capability("gamespace.perf.gpu_freq", owner = "gamespace"))
        val measured = registry.record(
            Capability(
                id = "gamespace.perf.gpu_freq",
                owner = "gamespace",
                state = CapabilityState.UNSUPPORTED,
                reason = "/sys/class/kgsl/kgsl-3d0/max_gpuclk: EACCES",
                measuredAtMillis = 1_700_000_000_000,
            )
        )
        assertEquals("/sys/class/kgsl/kgsl-3d0/max_gpuclk: EACCES", registry.get(measured.id)!!.reason)
    }

    @Test
    fun `a second owner cannot take over a capability`() {
        registry.declare(Capability("floatwindow.power.panel_ui", owner = "floatwindow"))
        try {
            registry.declare(Capability("floatwindow.power.panel_ui", owner = "gamespace"))
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("floatwindow"))
        }
    }

    @Test
    fun `recording an undeclared capability is refused instead of invented`() {
        try {
            registry.record(Capability("ghost.capability", owner = "ghost", state = CapabilityState.AVAILABLE, evidence = "made up"))
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("never declared"))
        }
    }

    @Test
    fun `observers see recorded measurements and stop after close`() {
        registry.declare(Capability("notes.store.write", owner = "notes"))
        val seen = mutableListOf<String>()
        val subscription = registry.observe { seen += it.id }

        registry.record(Capability("notes.store.write", owner = "notes", state = CapabilityState.AVAILABLE, evidence = "row written and read back"))
        subscription.close()
        registry.record(Capability("notes.store.write", owner = "notes", state = CapabilityState.AVAILABLE, evidence = "row written and read back"))

        assertEquals(listOf("notes.store.write"), seen)
    }
}
