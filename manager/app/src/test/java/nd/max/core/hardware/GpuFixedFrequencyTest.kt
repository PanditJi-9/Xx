/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression for fixed/range controls and measured clocks, using the real backend with a driver transport. */
class GpuFixedFrequencyTest {
    private val base = "/sys/class/devfreq/13000000.mali"
    private val lock = "/proc/gpufreqv2/fix_target_opp_index"
    private val gedClock = "/sys/kernel/ged/hal/current_freqency"

    private inner class DriverIo(
        val unit: GpuHardwareBackend.FrequencyUnit = GpuHardwareBackend.FrequencyUnit.HZ,
        indexed: Boolean = true,
        val clockMoves: Boolean = true,
        val clockDelayReads: Int = 0,
    ) : GpuHardwareBackend.Io {
        private val multiplier = unit.hzMultiplier
        val values = mutableMapOf(
            "$base/device_name" to if (indexed) "mali-gpu" else "kgsl-3d0",
            "$base/available_frequencies" to listOf(260L, 520L, 650L, 754L, 1300L)
                .joinToString(" ") { (it * 1_000_000L / multiplier).toString() },
            "$base/min_freq" to (260_000_000L / multiplier).toString(),
            "$base/max_freq" to (754_000_000L / multiplier).toString(),
            "$base/cur_freq" to (260_000_000L / multiplier).toString(),
            "$base/governor" to "dummy",
            "$base/available_governors" to "dummy performance",
        )
        val writablePaths = mutableSetOf("$base/min_freq", "$base/max_freq", "$base/governor")
        val writes = mutableListOf<Pair<String, String>>()
        val ceilingCalls = mutableListOf<Pair<Boolean, Boolean>>()
        val pauses = mutableListOf<Long>()
        val refuseOnce = mutableSetOf<String>()
        var refuseUnlock = false
        var refuseCeilingRestore = false
        private var pendingClock: String? = null

        init {
            if (indexed) {
                values[lock] = "-1"
                values[PlatformCeilingAuthority.GED_DVFS_ENABLE] = "1"
                values["/proc/gpufreqv2/stack_signed_opp_table"] =
                    "[0] freq=1300 MHz\n[1] freq=754 MHz\n[2] freq=650 MHz\n[3] freq=520 MHz\n[4] freq=260 MHz"
                writablePaths += lock
            }
        }

        override fun exists(path: String): Boolean = path in values || values.keys.any { it.startsWith("$path/") }
        override fun read(path: String): String? = values[path]
        override fun writable(path: String): Boolean = path in writablePaths
        override fun listDirectories(path: String): List<String> =
            if (path == "/sys/class/devfreq") listOf("13000000.mali") else emptyList()

        override fun write(path: String, value: String): Boolean {
            writes += path to value
            if (path !in writablePaths || refuseOnce.remove(path) || (path == lock && value == "-1" && refuseUnlock)) return false
            if (path == "$base/min_freq" && value.toLong() > values.getValue("$base/max_freq").toLong()) return false
            if (path == "$base/max_freq" && value.toLong() < values.getValue("$base/min_freq").toLong()) return false
            values[path] = value
            // Deliberately do not move the clock on an index write: an index echo
            // cannot establish which clock the running driver actually chose.
            if (path == "$base/min_freq" || path == "$base/max_freq") {
                val min = values.getValue("$base/min_freq")
                val max = values.getValue("$base/max_freq")
                pendingClock = if (min == max && clockMoves && values[lock] != "2" &&
                    values[PlatformCeilingAuthority.GED_DVFS_ENABLE] != "0"
                ) min else null
                if (clockDelayReads == 0) moveClock()
            }
            return true
        }

        private fun moveClock() {
            pendingClock?.let { values["$base/cur_freq"] = it }
        }

        override fun pauseBeforeReadback(delayMs: Long) {
            pauses += delayMs
            if (pauses.size >= clockDelayReads) moveClock()
        }

        override fun permitVendorCeiling(lock: Boolean, release: Boolean): Boolean {
            ceilingCalls += lock to release
            if (PlatformCeilingAuthority.GED_DVFS_ENABLE in values) {
                values[PlatformCeilingAuthority.GED_DVFS_ENABLE] = if (lock) "0" else "1"
            }
            return false
        }

        override fun captureVendorCeiling(): PlatformCeilingAuthority.CeilingCapture =
            PlatformCeilingAuthority.CeilingCapture(null, null, null, values[PlatformCeilingAuthority.GED_DVFS_ENABLE], null, null)

        override fun restoreVendorCeiling(capture: PlatformCeilingAuthority.CeilingCapture): Boolean {
            if (refuseCeilingRestore) return false
            capture.dvfsEnable?.let { values[PlatformCeilingAuthority.GED_DVFS_ENABLE] = it }
            return true
        }

        fun device(): GpuHardwareBackend.Device = GpuHardwareBackend.selection(this).device!!
        fun fixed(mhz: Long = 650L): GpuHardwareBackend.Request =
            GpuHardwareBackend.Request(mhz * 1_000_000L / multiplier, mhz * 1_000_000L / multiplier, releaseVendorCeiling = false)
    }

    @Test fun `fixed frequency uses the proven devfreq bounds instead of a vendor index`() {
        val io = DriverIo()
        val device = io.device()
        assertNotNull(device.mtkFixedIndexPath)
        assertTrue(device.rangeWritable)
        val result = GpuHardwareBackend.applyValidated(device, io.fixed(), io)
        assertTrue(result.verified)
        assertEquals("650000000", io.values["$base/min_freq"])
        assertEquals("650000000", io.values["$base/max_freq"])
        assertEquals(650_000_000L, GpuHardwareBackend.currentFrequencyHz(result.actual!!, io))
        assertTrue(io.writes.none { it.first == lock })
        assertEquals(listOf(false to false), io.ceilingCalls)
        assertEquals("1", io.values[PlatformCeilingAuthority.GED_DVFS_ENABLE])
    }

    @Test fun `a delayed clock is re-read without repeating the writes`() {
        val io = DriverIo(clockDelayReads = 2)
        val result = GpuHardwareBackend.applyValidated(io.device(), io.fixed(), io)
        assertTrue(result.verified)
        assertEquals(listOf(40L, 80L), io.pauses)
        assertEquals(1, io.writes.count { it.first == "$base/min_freq" })
        assertEquals(1, io.writes.count { it.first == "$base/max_freq" })
    }

    @Test fun `an unchanged measured clock cannot turn an echoed range into success`() {
        val io = DriverIo(clockMoves = false)
        val result = GpuHardwareBackend.applyValidated(io.device(), io.fixed(), io)
        assertFalse(result.verified)
        assertTrue(result.rollbackAttempted)
        assertEquals(true, result.rollbackVerified)
        assertEquals("pin-clock-mismatch", result.error)
        assertEquals("260000000", io.values["$base/min_freq"])
        assertEquals("754000000", io.values["$base/max_freq"])
    }

    @Test fun `an unreadable measured clock is not a verified fixed frequency`() {
        val io = DriverIo(clockMoves = false)
        io.values.remove("$base/cur_freq")
        val result = GpuHardwareBackend.applyValidated(io.device(), io.fixed(), io)
        assertFalse(result.verified)
        assertEquals("pin-unreadable", result.error)
        assertEquals(true, result.rollbackVerified)
        assertNull(GpuHardwareBackend.measuredFrequency(result.actual!!, io))
    }

    @Test fun `partial range failure restores both original bounds`() {
        val io = DriverIo()
        io.refuseOnce += "$base/max_freq"
        val result = GpuHardwareBackend.applyValidated(io.device(), io.fixed(), io)
        assertFalse(result.verified)
        assertEquals(true, result.rollbackVerified)
        assertEquals("260000000", io.values["$base/min_freq"])
        assertEquals("754000000", io.values["$base/max_freq"])
    }

    @Test fun `a held index is released before an equal-bounds request`() {
        val io = DriverIo()
        io.values[lock] = "2"
        val device = io.device()
        assertEquals(754_000_000L, device.maxFreq)
        assertEquals(260_000_000L, device.currentFreq)
        val result = GpuHardwareBackend.applyValidated(device, io.fixed(), io)
        assertTrue(result.verified)
        assertEquals(lock to "-1", io.writes.first())
        assertEquals("-1", io.values[lock])
    }

    @Test fun `a rejected unlock never reports a range write as applied`() {
        val io = DriverIo()
        io.values[lock] = "2"
        io.refuseUnlock = true
        val result = GpuHardwareBackend.applyValidated(io.device(), io.fixed(), io)
        assertFalse(result.verified)
        assertFalse(result.writeSucceeded)
        assertEquals("2", io.values[lock])
        assertTrue(io.writes.none { it.first == "$base/min_freq" && it.second == "650000000" })
    }

    @Test fun `unreadable vendor baseline blocks a hybrid write before any side effects`() {
        val io = DriverIo()
        io.values[lock] = "unknown"
        val result = GpuHardwareBackend.applyValidated(io.device(), io.fixed(), io)
        assertFalse(result.verified)
        assertEquals("baseline-unreadable", result.error)
        assertTrue(io.writes.isEmpty())
        assertTrue(io.ceilingCalls.isEmpty())
    }

    @Test fun `failed verification restores the captured dvfs value too`() {
        val io = DriverIo(clockMoves = false)
        io.values[PlatformCeilingAuthority.GED_DVFS_ENABLE] = "0"
        val result = GpuHardwareBackend.applyValidated(io.device(), io.fixed(), io)
        assertFalse(result.verified)
        assertEquals(true, result.rollbackVerified)
        assertEquals("0", io.values[PlatformCeilingAuthority.GED_DVFS_ENABLE])
    }

    @Test fun `failed vendor restoration is not called a verified rollback`() {
        val io = DriverIo(clockMoves = false)
        io.refuseCeilingRestore = true
        val result = GpuHardwareBackend.applyValidated(io.device(), io.fixed(), io)
        assertFalse(result.verified)
        assertEquals(false, result.rollbackVerified)
        assertEquals("apply-and-rollback-failed", result.error)
    }

    @Test fun `fixed requests compare measured hz using the provider unit`() {
        for (unit in listOf(GpuHardwareBackend.FrequencyUnit.HZ, GpuHardwareBackend.FrequencyUnit.KHZ, GpuHardwareBackend.FrequencyUnit.MHZ)) {
            val io = DriverIo(unit = unit, indexed = false)
            val result = GpuHardwareBackend.applyValidated(io.device(), io.fixed(), io)
            assertTrue(unit.name, result.verified)
            assertEquals(unit.name, 650_000_000L, GpuHardwareBackend.measuredFrequency(result.actual!!, io)?.hz)
        }
    }

    @Test fun `mtk range writes use their raw khz unit even with an hz opp table`() {
        val io = DriverIo(unit = GpuHardwareBackend.FrequencyUnit.KHZ)
        val device = io.device()
        assertEquals(GpuHardwareBackend.FrequencyUnit.KHZ, device.frequencyUnit)
        val result = GpuHardwareBackend.applyValidated(device, io.fixed(), io)
        assertTrue(result.verified)
        assertEquals("650000", io.values["$base/max_freq"])
        assertEquals(650_000_000L, GpuHardwareBackend.measuredFrequency(result.actual!!, io)?.hz)
    }

    @Test fun `an index is not substituted for the measured current clock or limits`() {
        val io = DriverIo()
        io.values[lock] = "2"
        val device = io.device()
        assertEquals(650_000_000L, GpuHardwareBackend.currentExactLockFrequency(device, io))
        assertEquals(260_000_000L, device.currentFreq)
        assertEquals(260_000_000L, device.minFreq)
        assertEquals(754_000_000L, device.maxFreq)
        assertEquals(260_000_000L, GpuHardwareBackend.measuredFrequency(device, io)?.hz)
    }

    @Test fun `an idle clock below the first opp is still a real measurement`() {
        val io = DriverIo()
        io.values[gedClock] = "26000"
        val measured = GpuHardwareBackend.measuredFrequency(io.device(), io)!!
        assertEquals(26_000_000L, measured.hz)
        assertEquals(gedClock, measured.source)
        assertTrue(measured.observedAtMs > 0L)
    }

    @Test fun `a qualcomm clock never adopts a global mtk ged measurement`() {
        val io = DriverIo(indexed = false)
        io.values[gedClock] = "520000"
        val measured = GpuHardwareBackend.measuredFrequency(io.device(), io)!!
        assertEquals(260_000_000L, measured.hz)
        assertEquals("$base/cur_freq", measured.source)
    }

    @Test fun `unrecognized lock text is neither an index nor proof of release`() {
        assertNull(MtkGpuOppTable.parseIndex(""))
        assertNull(MtkGpuOppTable.parseIndex("unknown"))
        assertNull(MtkGpuOppTable.parseIndex("GPU index=2 STACK index=3"))
        assertEquals("-1", MtkGpuOppTable.parseIndex("fix GPU/STACK OPP index is disabled"))
        assertEquals("2", MtkGpuOppTable.parseIndex("fixed index = 2"))
    }

    @Test fun `a missing live ceiling is not replaced by the capability maximum`() {
        val io = DriverIo()
        io.values.remove("$base/max_freq")
        assertNull(GpuHardwareBackend.nodeCeilingHz(io.device()))
    }
}
