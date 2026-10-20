/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * لقطة اكتشاف GPU المنشورة — الترميز والفكّ والطزاجة بلا جذر وبلا قرص.
 *
 * والقاعدة المُثبَّتة: ما يُكتب يُقرأ حرفيًا (درجات · حدود · وحدة · نتيجة)، والمعطوب
 * يُسقَط سطره وحده، والغامض (إصدار/زمن/فراغ) يُهمَل كله — فلا تخمين يعبر القناة.
 */
class GpuDiscoverySnapshotTest {

    private fun fullDevice() = GpuHardwareBackend.Device(
        path = "/sys/class/devfreq/kgsl-3d0",
        name = "kgsl-3d0",
        governor = "msm_adreno_tz",
        governors = listOf("msm_adreno_tz", "performance"),
        minFreq = 315000000L,
        maxFreq = 1000000000L,
        currentFreq = 500000000L,
        frequencies = listOf(315000000L, 500000000L, 1000000000L),
        family = GpuHardwareBackend.Family.QUALCOMM,
        frequencyUnit = GpuHardwareBackend.FrequencyUnit.HZ,
        minWritable = true,
        maxWritable = true,
        governorWritable = true,
        mtkFixedIndexPath = null,
        mtkLockWritable = false,
        mtkOppIndexByFrequency = emptyMap(),
        evidence = listOf("qualcomm-gpu-identity", "devfreq-opp-table"),
    )

    private fun shellDevice() = GpuHardwareBackend.Device(
        path = "/sys/class/devfreq/kgsl-3d0",
        name = "kgsl-3d0",
        governor = null,
        governors = emptyList(),
        minFreq = null,
        maxFreq = null,
        currentFreq = null,
        frequencies = emptyList(),
        family = GpuHardwareBackend.Family.QUALCOMM,
        frequencyUnit = GpuHardwareBackend.FrequencyUnit.AMBIGUOUS,
        evidence = listOf("qualcomm-gpu-identity", "clocks-unreadable-at-discovery"),
    )

    @Test fun `a full device roundtrips with its score`() {
        val at = 1_700_000_000_000L
        val text = GpuDiscoverySnapshot.encode(at, listOf(GpuDiscoverySnapshot.Entry(fullDevice(), 125)))
        val snapshot = GpuDiscoverySnapshot.parse(text)!!
        assertEquals(at, snapshot.atMs)
        assertEquals(1, snapshot.entries.size)
        val (device, score) = snapshot.entries.single()
        assertEquals(125, score)
        assertEquals("/sys/class/devfreq/kgsl-3d0", device.path)
        assertEquals(GpuHardwareBackend.Family.QUALCOMM, device.family)
        assertEquals(listOf(315000000L, 500000000L, 1000000000L), device.frequencies)
        assertEquals(315000000L, device.minFreq)
        assertEquals(1000000000L, device.maxFreq)
        assertEquals(500000000L, device.currentFreq)
        assertEquals("msm_adreno_tz", device.governor)
        assertEquals(GpuHardwareBackend.FrequencyUnit.HZ, device.frequencyUnit)
        // الصلاحية لا تُنقل: تُثبت في عملية الاستعمال دائمًا.
        assertFalse(device.minWritable)
        assertFalse(device.maxWritable)
        // والوسم يُرافق: متبنّى لا مقيس هنا.
        assertTrue(device.evidence.contains("companion-published"))
        assertTrue(device.evidence.contains("qualcomm-gpu-identity"))
    }

    @Test fun `a shell device roundtrips without inventing clocks`() {
        val text = GpuDiscoverySnapshot.encode(99L, listOf(GpuDiscoverySnapshot.Entry(shellDevice(), 100)))
        val device = GpuDiscoverySnapshot.parse(text)!!.entries.single().device
        assertTrue(device.frequencies.isEmpty())
        assertNull(device.minFreq)
        assertNull(device.maxFreq)
        assertNull(device.currentFreq)
        assertNull(device.governor)
        assertEquals(GpuHardwareBackend.FrequencyUnit.AMBIGUOUS, device.frequencyUnit)
        assertTrue(device.evidence.contains("clocks-unreadable-at-discovery"))
        assertTrue(device.evidence.contains("companion-published"))
    }

    @Test fun `unknown version time or emptiness discards the whole snapshot`() {
        val good = GpuDiscoverySnapshot.encode(7L, listOf(GpuDiscoverySnapshot.Entry(shellDevice(), 100)))
        assertNull(GpuDiscoverySnapshot.parse(good.replace("v=1", "v=2")))
        assertNull(GpuDiscoverySnapshot.parse(good.lines().filterNot { it.startsWith("at=") }.joinToString("\n")))
        assertNull(GpuDiscoverySnapshot.parse("v=1\nat=7\n"))
        assertNull(GpuDiscoverySnapshot.parse(""))
        assertNull(GpuDiscoverySnapshot.parse("garbage\n"))
    }

    @Test fun `a corrupt device line drops alone and keeps its siblings`() {
        val good = GpuDiscoverySnapshot.encode(7L, listOf(GpuDiscoverySnapshot.Entry(shellDevice(), 100)))
        val mixed = "$good\ndevice broken\ndevice /a /b\n"
        val snapshot = GpuDiscoverySnapshot.parse(mixed)!!
        assertEquals(1, snapshot.entries.size)
    }

    @Test fun `unknown family and unit fall back instead of crashing`() {
        val good = GpuDiscoverySnapshot.encode(7L, listOf(GpuDiscoverySnapshot.Entry(shellDevice(), 100)))
        val tampered = good
            .replace("QUALCOMM", "BOGUS")
            .replace("AMBIGUOUS", "BOGUS")
        val device = GpuDiscoverySnapshot.parse(tampered)!!.entries.single().device
        assertEquals(GpuHardwareBackend.Family.UNKNOWN, device.family)
        assertEquals(GpuHardwareBackend.FrequencyUnit.AMBIGUOUS, device.frequencyUnit)
    }

    @Test fun `freshness is a fifteen minute window and the future is stale`() {
        val ttl = GpuDiscoverySnapshot.SNAPSHOT_TTL_MS
        assertTrue(GpuDiscoverySnapshot.isFresh(1_000_000L, 1_000_000L + ttl - 1))
        assertFalse(GpuDiscoverySnapshot.isFresh(1_000_000L, 1_000_000L + ttl))
        assertFalse(GpuDiscoverySnapshot.isFresh(1_000_000L, 1_000_000L + ttl + 1))
        assertFalse(GpuDiscoverySnapshot.isFresh(1_000_000L, 999_999L))
    }

    @Test fun `an mtk index table roundtrips`() {
        val mtk = fullDevice().copy(
            mtkFixedIndexPath = "/proc/gpufreqv2/fix_target_opp_index",
            mtkOppIndexByFrequency = mapOf(1300000000L to "0", 754000000L to "1"),
        )
        val text = GpuDiscoverySnapshot.encode(7L, listOf(GpuDiscoverySnapshot.Entry(mtk, 130)))
        val device = GpuDiscoverySnapshot.parse(text)!!.entries.single().device
        assertEquals("/proc/gpufreqv2/fix_target_opp_index", device.mtkFixedIndexPath)
        assertEquals(mapOf(1300000000L to "0", 754000000L to "1"), device.mtkOppIndexByFrequency)
    }
}
