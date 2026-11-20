package dev.redlab.core.capability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SysfsDiscoveryTest {

    private class FakeIo(
        private val nodes: MutableMap<String, String> = mutableMapOf(),
        private val writable: Set<String> = emptySet(),
    ) : NodeIo {
        val writes = mutableListOf<Pair<String, String>>()
        override fun read(path: String): String? = nodes[path]
        override fun write(path: String, value: String): Boolean {
            if (path !in writable) return false
            nodes[path] = value
            writes += path to value
            return true
        }
    }

    private val cpuNode = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq"
    private val gpuCandidates = listOf(
        "/sys/class/kgsl/kgsl-3d0/max_gpuclk",
        "/sys/class/devfreq/gpufreq/max_freq",
    )
    private val declared = Capability(
        "gamespace.perf.gpu_freq",
        owner = "gamespace",
        state = CapabilityState.UNKNOWN,
    )
    private val discovery = SysfsDiscovery(FakeIo(), clock = { 1_700_000_000_000 })

    @Test
    fun `no readable candidate is UNSUPPORTED with the tried paths as reason`() {
        val result = discovery.probe(declared, gpuCandidates)
        assertEquals(CapabilityState.UNSUPPORTED, result.state)
        assertEquals(Access.NONE, result.access)
        assertTrue(result.reason.orEmpty().contains("kgsl"))
        assertTrue(result.reason.orEmpty().contains("devfreq"))
    }

    @Test
    fun `a readable node without a write probe is AVAILABLE and READ_ONLY`() {
        val io = FakeIo(mutableMapOf(cpuNode to "1800000"))
        val result = SysfsDiscovery(io, clock = { 7 }).probe(declared, listOf(cpuNode))
        assertEquals(CapabilityState.AVAILABLE, result.state)
        assertEquals(Access.READ_ONLY, result.access)
        assertEquals("read $cpuNode = '1800000'", result.evidence)
        assertEquals(7L, result.measuredAtMillis)
        assertTrue(io.writes.isEmpty())
    }

    @Test
    fun `the first readable candidate wins even when later ones exist`() {
        val io = FakeIo(mutableMapOf(gpuCandidates[1] to "670000000"))
        val result = SysfsDiscovery(io, clock = { 1 }).probe(declared, gpuCandidates, writeProbe = true)
        assertTrue(result.evidence!!.contains("devfreq"))
        assertEquals(Access.READ_ONLY, result.access) // FakeIo does not mark it writable
    }

    @Test
    fun `an accepted no-op write proves READ_WRITE - and it writes the current value only`() {
        val io = FakeIo(mutableMapOf(cpuNode to "1800000"), writable = setOf(cpuNode))
        val result = SysfsDiscovery(io, clock = { 1 }).probe(declared, listOf(cpuNode), writeProbe = true)
        assertEquals(Access.READ_WRITE, result.access)
        assertEquals(listOf(cpuNode to "1800000"), io.writes)
        assertTrue(result.evidence!!.contains("no-op write accepted"))
    }

    @Test
    fun `a refused no-op write keeps the capability AVAILABLE but READ_ONLY with the reason`() {
        val io = FakeIo(mutableMapOf(cpuNode to "1800000"))
        val result = SysfsDiscovery(io, clock = { 1 }).probe(declared, listOf(cpuNode), writeProbe = true)
        assertEquals(CapabilityState.AVAILABLE, result.state)
        assertEquals(Access.READ_ONLY, result.access)
        assertTrue(result.evidence!!.contains("write refused"))
    }
}
