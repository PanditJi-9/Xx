/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.platform

import java.io.File
import java.nio.file.Files
import nd.max.core.hardware.ControlOwnership
import nd.max.core.hardware.HardwareControlArbiter
import nd.max.core.hardware.HardwareControlKey
import nd.max.core.hardware.ManualControlLocks
import nd.max.core.hardware.SharedHardwareOwnershipStore
import nd.max.core.hardware.WriteVerification
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** المحكّم الحقيقي بملف ملكية مؤقت؛ ناقل العقدة وحده مصنوع بدل الكتابة على جهاز. */
class LmkArbiterTest {
    private lateinit var root: File
    private val original = listOf(10240, 20480, 30720)
    private val target = listOf(7680, 15360, 23040)
    private val key = HardwareControlKey.MEMORY_LMK_MINFREE

    @Before
    fun configureStores() {
        root = Files.createTempDirectory("lmk-arbiter-test").toFile()
        SharedHardwareOwnershipStore.configure(root, appUid = 0, processId = ProcessHandle.current().pid().toInt())
        ManualControlLocks.configure(root)
        ManualControlLocks.clearAll()
        ControlOwnership.snapshot().forEach { ControlOwnership.release(it.key) }
    }

    @After
    fun cleanStores() {
        ManualControlLocks.clearAll()
        ControlOwnership.snapshot().forEach { ControlOwnership.release(it.key) }
        root.deleteRecursively()
    }

    @Test
    fun successfulTransactionIsVerifiedAndReleasesItsTemporaryLease() {
        val arbiter = HardwareControlArbiter()
        var live = LmkTuning.format(original)
        val io = LmkTuning.Io.transactionIo(arbiter, read = { live }, write = { path, value ->
            assertEquals(LmkTuning.MINFREE, path)
            live = value
            WriteVerification.Outcome.MATCHED
        })
        assertEquals(LmkTuning.Outcome.APPLIED, io.apply(target, original))
        assertEquals(LmkTuning.format(target), live)
        assertTrue(arbiter.snapshot().isEmpty())
        assertFalse(SharedHardwareOwnershipStore.snapshot().any { it.key == key })
        assertNull(ControlOwnership.winner(key))
    }

    @Test
    fun manualLockBlocksBeforeAnyNodeWrite() {
        val arbiter = HardwareControlArbiter()
        var writes = 0
        ManualControlLocks.lock(key, "other-manual-owner", LmkTuning.format(original), LmkTuning.format(original))
        val io = LmkTuning.Io.transactionIo(arbiter, read = { LmkTuning.format(original) }, write = { _, _ ->
            writes++
            WriteVerification.Outcome.MATCHED
        })
        assertEquals(LmkTuning.Outcome.BLOCKED, io.apply(target, original))
        assertEquals(0, writes)
        assertTrue(arbiter.snapshot().isEmpty())
    }

    @Test
    fun safetyOwnerWinsAndBlockedIntentDoesNotApplyAfterItsRelease() {
        val arbiter = HardwareControlArbiter()
        var live = LmkTuning.format(original)
        var writes = 0
        assertTrue(arbiter.submit(key, ControlOwnership.Owner.SAFETY, "safety", live, apply = { live = it; true }, read = { live }).verified)
        val io = LmkTuning.Io.transactionIo(arbiter, read = { live }, write = { _, value ->
            writes++
            live = value
            WriteVerification.Outcome.MATCHED
        })
        assertEquals(LmkTuning.Outcome.BLOCKED, io.apply(target, original))
        arbiter.release(key, "safety", restore = false)
        assertEquals(0, writes)
        assertEquals(LmkTuning.format(original), live)
        assertTrue(arbiter.snapshot().isEmpty())
    }

    @Test
    fun aWriteRefusalIsNotCalledApplied() {
        val arbiter = HardwareControlArbiter()
        val io = LmkTuning.Io.transactionIo(arbiter, read = { LmkTuning.format(original) }, write = { _, _ ->
            WriteVerification.Outcome.WRITE_FAILED
        })
        assertEquals(LmkTuning.Outcome.NOT_WRITABLE, io.apply(target, original))
        assertTrue(arbiter.snapshot().isEmpty())
    }

    @Test
    fun unverifiedWriteRestoresItsOwnStillVisibleTarget() {
        val arbiter = HardwareControlArbiter()
        var live = LmkTuning.format(original)
        val writes = mutableListOf<String>()
        val io = LmkTuning.Io.transactionIo(arbiter, read = { live }, write = { _, value ->
            writes += value
            live = value
            if (value == LmkTuning.format(target)) WriteVerification.Outcome.READBACK_UNAVAILABLE else WriteVerification.Outcome.MATCHED
        })
        assertEquals(LmkTuning.Outcome.UNVERIFIED, io.apply(target, original))
        assertEquals(listOf(LmkTuning.format(target), LmkTuning.format(original)), writes)
        assertEquals(LmkTuning.format(original), live)
        assertTrue(arbiter.snapshot().isEmpty())
    }

    @Test
    fun rollbackDoesNotOverwriteAnUnrelatedExternalChange() {
        val arbiter = HardwareControlArbiter()
        var live = LmkTuning.format(original)
        var writes = 0
        val external = "12000,24000,48000"
        val io = LmkTuning.Io.transactionIo(arbiter, read = { live }, write = { _, _ ->
            writes++
            live = external
            WriteVerification.Outcome.DIFFERS
        })
        assertEquals(LmkTuning.Outcome.UNVERIFIED, io.apply(target, original))
        assertEquals(1, writes)
        assertEquals(external, live)
        assertTrue(arbiter.snapshot().isEmpty())
    }

    @Test
    fun changedBaselineInsideTheArbiterLockIsNotOverwritten() {
        val arbiter = HardwareControlArbiter()
        var reads = 0
        var writes = 0
        val external = "12000,24000,48000"
        val io = LmkTuning.Io.transactionIo(arbiter, read = {
            reads++
            if (reads == 1) LmkTuning.format(original) else external
        }, write = { _, _ -> writes++; WriteVerification.Outcome.MATCHED })
        assertEquals(LmkTuning.Outcome.BLOCKED, io.apply(target, original))
        assertEquals(0, writes)
        assertTrue(arbiter.snapshot().isEmpty())
    }

    @Test
    fun malformedReadbackAndFailedRollbackRemainUnverified() {
        val arbiter = HardwareControlArbiter()
        var live = LmkTuning.format(original)
        val io = LmkTuning.Io.transactionIo(arbiter, read = { live }, write = { _, _ ->
            live = "not-a-table"
            WriteVerification.Outcome.DIFFERS
        })
        assertEquals(LmkTuning.Outcome.UNVERIFIED, io.apply(target, original))
        assertTrue(arbiter.snapshot().isEmpty())
        assertNull(ControlOwnership.winner(key))
    }
}
