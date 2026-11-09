/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import nd.max.core.display.PanelColorTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KernelTuningDiscoveryTest {
    private class Io(val values: MutableMap<String, String> = linkedMapOf(), val names: Map<String, List<String>> = emptyMap()) : NodeTuning.Io {
        override fun read(path: String): String? = values[path]
        override fun write(path: String, value: String): Boolean { throw AssertionError("Discovery tried to write $path") }
        override fun listNames(path: String): List<String> = names[path].orEmpty()
    }

    private fun policy(name: String = "policy0", governor: String = "schedutil") = CpuHardwareBackend.Policy(
        path = "/sys/devices/system/cpu/cpufreq/$name", name = name, governor = governor,
        governors = listOf(governor), minKHz = 300000, maxKHz = 2000000,
        hwMinKHz = 300000, hwMaxKHz = 2000000, availableFrequenciesKHz = listOf(300000, 1000000, 2000000),
    )

    @Test
    fun schedulerParseRequiresExactlyOneLiveSelection() {
        assertEquals(listOf("none", "mq-deadline", "kyber") to "mq-deadline", StorageTuning.parseSchedulers("none [mq-deadline] kyber\n"))
        listOf(null, "", "none kyber", "[none] [kyber]", "none [bad;cmd]", "none [none]").forEach { assertNull(StorageTuning.parseSchedulers(it)) }
    }

    @Test
    fun physicalDevicesAreDiscoveredWithoutStackedOrSwapDevices() {
        val io = Io(names = mapOf(StorageTuning.ROOT to listOf("sda", "sdb", "dm-0", "loop0", "zram0", "mmcblk0", "../../bad")))
        for (device in listOf("sda", "sdb", "mmcblk0", "dm-0", "loop0", "zram0")) {
            io.values["/sys/block/$device/queue/scheduler"] = "none [mq-deadline]"
        }
        io.values["/sys/block/mmcblk0/removable"] = "1"
        val found = StorageTuning.discover(io)
        assertEquals(listOf("mmcblk0", "sda", "sdb"), found.map { it.name })
        assertEquals(true, found.first().removable)
        assertNull(found.last().removable)
    }

    @Test
    fun eachDeviceKeepsItsOwnQueueCapabilitiesAndCurrentValue() {
        val io = Io(names = mapOf(StorageTuning.ROOT to listOf("sda", "sdb")))
        io.values["/sys/block/sda/queue/scheduler"] = "[none] mq-deadline"
        io.values["/sys/block/sdb/queue/scheduler"] = "none [kyber]"
        io.values["/sys/block/sda/queue/read_ahead_kb"] = "128"
        val devices = StorageTuning.discover(io)
        assertEquals("none", devices[0].controls.first().value)
        assertEquals("kyber", devices[1].controls.first().value)
        assertEquals(2, devices[0].controls.size)
        assertEquals(1, devices[1].controls.size)
        assertTrue(devices[0].controls.none { it.key in devices[1].controls.map(NodeControl::key) })
    }

    @Test
    fun invalidNumbersDoNotBecomeEditableFallbacks() {
        val io = Io(names = mapOf(StorageTuning.ROOT to listOf("sda")))
        io.values["/sys/block/sda/queue/read_ahead_kb"] = "999999999"
        io.values["/sys/block/sda/queue/nr_requests"] = "abc"
        assertTrue(StorageTuning.discover(io).isEmpty())
    }

    @Test
    fun governorDiscoveryReadsOnlySupportedKnownParameters() {
        val cpu = policy()
        val io = Io()
        io.values["${cpu.path}/schedutil/up_rate_limit_us"] = "5000"
        io.values["${cpu.path}/schedutil/not_reviewed"] = "1"
        val groups = GovernorTuning.discover(listOf(cpu), io)
        assertEquals(1, groups.size)
        assertEquals(listOf("up_rate_limit_us"), groups.single().controls.map(NodeControl::label))
        assertEquals(cpu.governor, groups.single().controls.single().contextValue)
        assertTrue(groups.single().controls.all(NodeTuning::validControl))
    }

    @Test
    fun globalGovernorNodesAreNotAliasedForEveryPolicy() {
        val io = Io()
        io.values["/sys/devices/system/cpu/cpufreq/schedutil/rate_limit_us"] = "2000"
        val groups = GovernorTuning.discover(listOf(policy(), policy("policy4")), io)
        assertEquals(1, groups.flatMap { it.controls }.size)
        assertTrue(groups.flatMap { it.controls }.all(NodeTuning::validControl))
    }

    @Test
    fun frequencyTunablesRequireProvenDeviceLimits() {
        val cpu = policy()
        val io = Io()
        io.values["${cpu.path}/schedutil/hispeed_freq"] = "3000000"
        assertTrue(GovernorTuning.discover(listOf(cpu), io).isEmpty())
        io.values["${cpu.path}/schedutil/hispeed_freq"] = "1000000"
        val control = GovernorTuning.discover(listOf(cpu), io).single().controls.single()
        assertFalse(control.accepts("3000000"))
    }

    @Test
    fun savedSnapshotCannotInventPathsOrWrongGovernorKeys() {
        val format = NodeValueFormat.Number(100, 10000)
        val control = NodeControl(HardwareControlKey.governorTunable("policy0", "schedutil", "rate_limit_us"), "/sys/example/rate_limit_us", "rate_limit_us", "1000", format)
        assertNull(GovernorTuning.matchSnapshot(listOf(control), mapOf("/sys/evil" to "1")))
        assertNull(GovernorTuning.matchSnapshot(listOf(control), mapOf(control.key to "100000000")))
        assertEquals(listOf(control to "2000"), GovernorTuning.matchSnapshot(listOf(control), mapOf(control.key to "2000")))
    }

    @Test
    fun hapticStrengthRequiresDriverReportedMinAndMax() {
        val io = Io()
        val root = "/sys/class/timed_output/vibrator"
        io.values["$root/vtg_level"] = "80"
        assertTrue(HapticTuning.discover(io).isEmpty())
        io.values["$root/vtg_min"] = "32"
        io.values["$root/vtg_max"] = "127"
        assertEquals("80", HapticTuning.discover(io).single().value)
        assertTrue(NodeTuning.validControl(HapticTuning.discover(io).single()))
        assertFalse(HapticTuning.discover(io).single().accepts("128"))
    }

    @Test
    fun splitHapticChannelsRequireEnabledOverrides() {
        val io = Io()
        val root = "/sys/class/leds/vibrator"
        io.values["$root/vtg_min"] = "100"
        io.values["$root/vtg_max"] = "1000"
        io.values["$root/vmax_mv_user"] = "500"
        assertTrue(HapticTuning.discover(io).isEmpty())
        io.values["$root/vmax_override"] = "1"
        assertTrue(HapticTuning.discover(io).isEmpty())
        io.values["$root/vmax_min"] = "100"
        io.values["$root/vmax_max"] = "1000"
        val control = HapticTuning.discover(io).single()
        assertEquals("touch", control.label)
        assertEquals("$root/vmax_override", control.contextPath)
    }

    @Test
    fun hapticBoundsAreNeverSynthesizedFromTheCurrentLevel() {
        val io = Io()
        val root = "/sys/class/timed_output/vibrator"
        io.values["$root/vtg_min"] = "127"
        io.values["$root/vtg_max"] = "32"
        io.values["$root/vtg_level"] = "80"
        assertTrue(HapticTuning.discover(io).isEmpty())
    }

    @Test
    fun disabledKcalHasNoPretendColourControls() {
        val io = Io()
        val root = "/sys/devices/platform/kcal_ctrl.0"
        io.values["$root/kcal_enable"] = "0"
        io.values["$root/kcal_sat"] = "255"
        assertTrue(PanelColorTuning.discover(io).isEmpty())
    }

    @Test
    fun rgbRequiresKnownMinimumAndPreservesReadableLowerBound() {
        val io = Io()
        val root = "/sys/devices/platform/kcal_ctrl.0"
        io.values["$root/kcal_enable"] = "1"
        io.values["$root/kcal"] = "256 256 256"
        assertTrue(PanelColorTuning.discover(io).isEmpty())
        io.values["$root/kcal_min"] = "35"
        val control = PanelColorTuning.discover(io).single()
        assertFalse(control.accepts("0 0 0"))
        assertTrue(control.accepts("128 200 256"))
        assertFalse(control.accepts("128 200"))
        assertTrue(NodeTuning.validControl(control))
    }

    @Test
    fun scalarColourBoundsAvoidTheSaturationGap() {
        val io = Io()
        val root = "/sys/devices/platform/kcal_ctrl.0"
        io.values["$root/kcal_enable"] = "1"
        io.values["$root/kcal_sat"] = "255"
        val control = PanelColorTuning.discover(io).single()
        assertFalse(control.accepts("200"))
        assertTrue(control.accepts("224"))
        assertTrue(control.accepts("383"))
    }
}
