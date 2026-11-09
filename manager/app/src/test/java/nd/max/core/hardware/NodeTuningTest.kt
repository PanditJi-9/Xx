/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NodeTuningTest {
    private lateinit var directory: File
    private val path = "/sys/block/sda/queue/read_ahead_kb"
    private val control = NodeControl(HardwareControlKey.storageQueue("sda", "read_ahead_kb"), path, "read_ahead_kb", "128", NodeValueFormat.Number(0, 4096))

    private class Io(var live: String? = "128", var writeSucceeds: Boolean = true) : NodeTuning.Io {
        val writes = arrayListOf<String>()
        var externalAfterWrite: String? = null
        var context: String = "schedutil"
        override fun read(path: String): String? = if (path.endsWith("scaling_governor")) context else live
        override fun write(path: String, value: String): Boolean {
            writes += value
            if (!writeSucceeds) return false
            live = externalAfterWrite ?: value
            return true
        }
        override fun listNames(path: String): List<String> = emptyList()
    }

    @Before
    fun configure() {
        directory = Files.createTempDirectory("node-tuning-test").toFile()
        SharedHardwareOwnershipStore.configure(directory, 0, ProcessHandle.current().pid().toInt())
        ManualControlLocks.configure(directory)
        ManualControlLocks.clearAll()
        ControlOwnership.snapshot().forEach { ControlOwnership.release(it.key) }
    }

    @After
    fun cleanup() {
        ManualControlLocks.clearAll()
        ControlOwnership.snapshot().forEach { ControlOwnership.release(it.key) }
        directory.deleteRecursively()
    }

    @Test
    fun numberParsingRejectsInjectedOrOutOfBoundsValues() {
        listOf("", "-1", "+1", "1;id", "abc", "4097", "999999999999999999999").forEach { assertFalse(control.accepts(it)) }
        assertTrue(control.accepts(" 256\n"))
    }

    @Test
    fun malformedVectorsAndMultipleActiveChoicesAreRejected() {
        val vector = NodeValueFormat.Vector(3, 64, 256)
        assertNull(vector.normalise("256 256"))
        assertNull(vector.normalise("0 0 0"))
        assertEquals("128 200 256", vector.normalise("128\t200 256"))
        assertNull(NodeValueFormat.Choice(listOf("none", "kyber"), true).normalise("[none] [kyber]"))
    }

    @Test
    fun validWriteIsReadBackAndTemporaryLeaseIsReleased() {
        val arbiter = HardwareControlArbiter()
        val io = Io()
        assertEquals(NodeTuningOutcome.APPLIED, NodeTuning(arbiter, io).apply(control, "256").outcome)
        assertEquals("256", io.live)
        assertTrue(arbiter.snapshot().isEmpty())
        assertNull(ControlOwnership.winner(control.key))
    }

    @Test
    fun invalidValuesNeverReachTheWriter() {
        val io = Io()
        assertEquals(NodeTuningOutcome.INVALID, NodeTuning(HardwareControlArbiter(), io).apply(control, "-1").outcome)
        assertTrue(io.writes.isEmpty())
    }

    @Test
    fun staleReadIsBlockedInsteadOfOverwritingAnotherWriter() {
        val io = Io(live = "256")
        assertEquals(NodeTuningOutcome.BLOCKED, NodeTuning(HardwareControlArbiter(), io).apply(control, "512").outcome)
        assertTrue(io.writes.isEmpty())
    }

    @Test
    fun missingBaselineDoesNotBecomeZero() {
        val io = Io(live = null)
        assertEquals(NodeTuningOutcome.UNREADABLE, NodeTuning(HardwareControlArbiter(), io).apply(control, "256").outcome)
        assertTrue(io.writes.isEmpty())
    }

    @Test
    fun manualLockPreventsTheWrite() {
        val io = Io()
        ManualControlLocks.lock(control.key, "other", "128", "128")
        assertEquals(NodeTuningOutcome.BLOCKED, NodeTuning(HardwareControlArbiter(), io).apply(control, "256").outcome)
        assertTrue(io.writes.isEmpty())
    }

    @Test
    fun refusedWriteIsReportedWithoutFabricatedSuccess() {
        val io = Io(writeSucceeds = false)
        assertEquals(NodeTuningOutcome.WRITE_FAILED, NodeTuning(HardwareControlArbiter(), io).apply(control, "256").outcome)
        assertEquals("128", io.live)
    }

    @Test
    fun externalDriftIsNotOverwrittenByRollback() {
        val io = Io()
        io.externalAfterWrite = "1024"
        val result = NodeTuning(HardwareControlArbiter(), io).apply(control, "256")
        assertEquals(NodeTuningOutcome.UNVERIFIED, result.outcome)
        assertEquals("1024", io.live)
        assertEquals(listOf("256"), io.writes)
    }

    @Test
    fun governorContextChangesInvalidateTheControl() {
        val io = Io()
        io.context = "performance"
        val guarded = control.copy(contextPath = "/sys/devices/system/cpu/cpufreq/policy0/scaling_governor", contextValue = "schedutil")
        assertEquals(NodeTuningOutcome.BLOCKED, NodeTuning(HardwareControlArbiter(), io).apply(guarded, "256").outcome)
        assertTrue(io.writes.isEmpty())
    }

    @Test
    fun pathTraversalIsRefused() {
        val io = Io()
        val unsafe = control.copy(path = "/sys/../system/evil")
        assertEquals(NodeTuningOutcome.INVALID, NodeTuning(HardwareControlArbiter(), io).apply(unsafe, "256").outcome)
        assertTrue(io.writes.isEmpty())
    }

    @Test
    fun compoundPresetIsValidatedBeforeTheFirstWrite() {
        val io = Io()
        val second = control.copy(key = HardwareControlKey.storageQueue("sdb", "read_ahead_kb"))
        val result = NodeTuning(HardwareControlArbiter(), io).applyPreset(listOf(control to "256", second to "999999"))
        assertEquals(NodeTuningOutcome.INVALID, result.outcome)
        assertTrue(io.writes.isEmpty())
    }

    @Test
    fun duplicateKnobInPresetIsRejected() {
        val io = Io()
        val result = NodeTuning(HardwareControlArbiter(), io).applyPreset(listOf(control to "256", control to "512"))
        assertEquals(NodeTuningOutcome.INVALID, result.outcome)
        assertTrue(io.writes.isEmpty())
    }

    @Test
    fun compoundFailureRestoresOnlyConfirmedEarlierWrites() {
        val values = mutableMapOf(path to "128", "/sys/block/sdb/queue/read_ahead_kb" to "128")
        val writes = arrayListOf<Pair<String, String>>()
        val io = object : NodeTuning.Io {
            override fun read(path: String): String? = values[path]
            override fun write(path: String, value: String): Boolean {
                writes += path to value
                if (path.contains("/sdb/")) return false
                values[path] = value
                return true
            }
            override fun listNames(path: String) = emptyList<String>()
        }
        val second = control.copy(key = HardwareControlKey.storageQueue("sdb", "read_ahead_kb"), path = "/sys/block/sdb/queue/read_ahead_kb")
        val result = NodeTuning(HardwareControlArbiter(), io).applyPreset(listOf(control to "256", second to "256"))
        assertEquals(NodeTuningOutcome.WRITE_FAILED, result.outcome)
        assertEquals("128", values[path])
        assertEquals(listOf(path to "256", second.path to "256", path to "128"), writes)
    }

    @Test
    fun malformedFormatLimitsFailClosed() {
        assertNull(NodeValueFormat.Number(-1, 100).normalise("10"))
        assertNull(NodeValueFormat.Number(100, 1).normalise("10"))
        assertNull(NodeValueFormat.Vector(1000, 0, 100).normalise("1 2"))
        assertNull(NodeValueFormat.Choice(emptyList()).normalise("1"))
    }

    @Test
    fun canonicalKeyMustMatchThePhysicalStorageDevice() {
        val io = Io()
        val wrong = control.copy(key = HardwareControlKey.storageQueue("sdb", "read_ahead_kb"))
        assertEquals(NodeTuningOutcome.INVALID, NodeTuning(HardwareControlArbiter(), io).apply(wrong, "256").outcome)
        assertTrue(io.writes.isEmpty())
    }
}
