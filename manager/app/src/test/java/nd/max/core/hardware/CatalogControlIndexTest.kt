/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.core.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * قراءة الكتالوج ودمجه في القدرة (M1 §6 · ADR-87).
 *
 * **ولماذا اختبار نقيّ:** الدمج هو المكان الذي يتحوّل فيه **مسار بيانات** إلى **حكم على الجهاز**.
 * فلو مرّ خطأ واحد هنا لادّعى التطبيق قدرةً لا يملكها — وهو ما يمنعه ADR-07. فكل لمسٍ للنظام
 * يُحقن هنا، ولا يُقرأ ملفّ.
 */
class CatalogControlIndexTest {

    private val json = """
        {
          "version": 1,
          "entries": [
            {
              "key": "cpu_limits:{policy}",
              "transport": "NODE",
              "kind": "FREQUENCY_RANGE",
              "candidates": [
                { "path": "/sys/devices/system/cpu/${'$'}policy/scaling_max_freq", "origin": "MEASURED" },
                { "path": "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "origin": "MEASURED" }
              ],
              "verify": { "policy": "READ_BACK_EQUAL", "tolerance": 0 },
              "privilege": { "read": ["NONE"], "write": ["MAGISK"] },
              "danger": "MEDIUM"
            },
            {
              "key": "gpu_governor_policy:{device}",
              "transport": "NODE",
              "kind": "ENUM",
              "candidates": [
                { "path": "/sys/module/ged/parameters/gpu_dvfs_enable", "origin": "MEASURED" }
              ],
              "verify": { "policy": "READ_BACK_EQUAL", "tolerance": 0 },
              "privilege": { "read": ["NONE"], "write": [] },
              "danger": "PROTECTED",
              "read_only_reason": "EXISTING_WRITER_NOT_ROUTED"
            },
            {
              "key": "thermal_zone",
              "transport": "NODE",
              "kind": "STRING",
              "candidates": [
                { "path": "/sys/class/thermal", "origin": "MEASURED" }
              ],
              "verify": { "policy": "NONE", "tolerance": 0 },
              "privilege": { "read": ["NONE"], "write": [] },
              "danger": "LOW"
            }
          ],
          "unmapped": []
        }
    """.trimIndent()

    private val index = CatalogControlIndex.parse(json)

    private fun capability(feature: HardwareFeature, access: AccessLevel) =
        FeatureCapability(feature, access, "test-backend")

    private fun probe(exists: Set<String>, writable: Set<String>) = CatalogCapabilityOverlay.Probe(
        exists = { it in exists },
        writable = { it in writable },
    )

    @Test
    fun theCatalogIsReadWithItsDeclaredFields() {
        assertEquals(3, index.entries.size)
        val cpu = index.entries.first()
        assertEquals("cpu_limits:{policy}", cpu.key)
        assertEquals("NODE", cpu.transport)
        assertEquals(listOf("MAGISK"), cpu.writeTiers)
        assertEquals(false, cpu.isProtected)
    }

    @Test
    fun templatesAreDeclaredButNeverMeasured() {
        val cpu = index.entries.first()
        assertEquals(
            listOf("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"),
            index.literalNodes(cpu),
        )
        assertEquals(
            "قالب `\$policy` ليس عقدة تُقاس",
            false,
            CatalogControlIndex.isLiteralNode("/sys/devices/system/cpu/\$policy/scaling_max_freq"),
        )
        assertEquals(false, CatalogControlIndex.isLiteralNode("/sys/block/zram*"))
        assertEquals(false, CatalogControlIndex.isLiteralNode("relative/path"))
        assertTrue(CatalogControlIndex.isLiteralNode("/sys/block/zram0/disksize"))
    }

    @Test
    fun anExistingLeafRaisesNoneToReadOnly() {
        val merged = CatalogCapabilityOverlay.apply(
            capability(HardwareFeature.CPU_FREQUENCY, AccessLevel.NONE),
            index,
            probe(
                exists = setOf("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"),
                writable = emptySet(),
            ),
        )

        assertEquals(AccessLevel.READ_ONLY, merged.access)
        assertEquals(listOf("cpu_limits:{policy}"), merged.catalogKeys)
        assertEquals(listOf("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"), merged.evidence)
    }

    @Test
    fun aWritableLeafWithDeclaredWriteRaisesToReadWrite() {
        val merged = CatalogCapabilityOverlay.apply(
            capability(HardwareFeature.CPU_FREQUENCY, AccessLevel.READ_ONLY),
            index,
            probe(
                exists = setOf("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"),
                writable = setOf("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"),
            ),
        )

        assertEquals(AccessLevel.READ_WRITE, merged.access)
    }

    @Test
    fun aProtectedEntryNeverRaisesToWrite() {
        val merged = CatalogCapabilityOverlay.apply(
            capability(HardwareFeature.GPU_GOVERNOR, AccessLevel.NONE),
            index,
            probe(
                exists = setOf("/sys/module/ged/parameters/gpu_dvfs_enable"),
                writable = setOf("/sys/module/ged/parameters/gpu_dvfs_enable"),
            ),
        )

        assertEquals(AccessLevel.READ_ONLY, merged.access)
    }

    @Test
    fun theCatalogNeverLowersAMeasuredAccess() {
        val base = capability(HardwareFeature.CPU_FREQUENCY, AccessLevel.READ_WRITE)
        val merged = CatalogCapabilityOverlay.apply(
            base,
            index,
            probe(exists = emptySet(), writable = emptySet()),
        )

        assertEquals(base, merged)
    }

    @Test
    fun aDirectoryCandidateIsNotEvidence() {
        // الكتالوج يعلن `/sys/class/thermal` مجلّدًا؛ والقياس هنا يرفض المجلّدات (isFile عند المحلّل).
        val merged = CatalogCapabilityOverlay.apply(
            capability(HardwareFeature.THERMAL_ZONES, AccessLevel.NONE),
            index,
            probe(exists = emptySet(), writable = emptySet()),
        )

        assertEquals(AccessLevel.NONE, merged.access)
        assertEquals(emptyList<String>(), merged.catalogKeys)
    }

    @Test
    fun aFeatureWithoutAnEntryIsUntouched() {
        val base = capability(HardwareFeature.DISPLAY_REFRESH, AccessLevel.NONE)
        val merged = CatalogCapabilityOverlay.apply(base, index, probe(emptySet(), emptySet()))

        assertEquals(base, merged)
    }

    @Test
    fun brokenJsonYieldsAnEmptyIndexInsteadOfThrowing() {
        assertEquals(0, CatalogControlIndex.parse("{ not json").entries.size)
        assertEquals(0, CatalogControlIndex.parse("""{"version": 9, "entries": []}""").entries.size)
        assertEquals(0, CatalogControlIndex.parse("""{"version": 1, "entries": [{"transport": "NODE"}]}""").entries.size)
    }

    @Test
    fun anEntryWithoutWriteTiersCannotClaimWriting() {
        val merged = CatalogCapabilityOverlay.apply(
            capability(HardwareFeature.GPU_GOVERNOR, AccessLevel.NONE),
            index,
            probe(
                exists = setOf("/sys/module/ged/parameters/gpu_dvfs_enable"),
                writable = setOf("/sys/module/ged/parameters/gpu_dvfs_enable"),
            ),
        )

        assertEquals("قائمة كتابة فارغة لا ترفع إلى READ_WRITE", AccessLevel.READ_ONLY, merged.access)
    }
}
