/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * دور عقدة الرسوميات: ساعةٌ أم **مراقب** — الفصل الذي كان غيابه يُغلق كل مقابض الـGPU.
 *
 * والعطب المقيس (POCO X6 Pro · SM8735 · Adreno 825 · حزمة ٢٠٢٦-١٠-١٥): `/sys/class/devfreq` يحمل
 * أكثر من عقدة في عائلة الرسوميات، والدرجة كانت تُمنح باحتوِاء الاسم `kgsl` — فيتعادل جهاز الـGPU
 * (`3d00000.qcom,kgsl-3d0`) مع مراقب عرض النطاق (`kgsl-busmon`)، ويُقرأ التعادل
 * `multiple-equally-proven-gpu-providers` فلا يُختار مزوّد. ونتيجة ذلك في حزمة المستخدم سطر واحد
 * تكرّر ست مرات: `knob=gpu_profile outcome=unsupported reason=no-gpu-provider:…`.
 *
 * وهذه المجموعة تقيس الفصل نفسه: أن المراقب **يُرى** ولا **يُرشَّح**، وأن الساعة تُختار، وأن
 * الاسمين يظهران في اللقطة والتشخيص.
 */
class GpuNodeRoleTest {

    private class FakeIo(
        private val values: MutableMap<String, String>,
        private val writable: Set<String> = emptySet(),
    ) : GpuHardwareBackend.Io {
        override fun exists(path: String) = path in values || values.keys.any { it.startsWith("$path/") }
        override fun writable(path: String) = path in writable
        override fun read(path: String) = values[path]
        override fun write(path: String, value: String): Boolean {
            if (path !in writable) return false
            values[path] = value
            return true
        }

        override fun listDirectories(path: String): List<String> = when (path) {
            "/sys/class/devfreq" -> values.keys.mapNotNull {
                it.takeIf { key -> key.startsWith("$path/") }
                    ?.removePrefix("$path/")?.substringBefore('/')?.takeIf(String::isNotBlank)
            }.distinct()
            else -> emptyList()
        }

        fun put(path: String, value: String) { values[path] = value }
    }

    /**
     * عقدة كما تنشرها نواة سنابدراجون: `kgsl-3d0` ساعة، و`kgsl-busmon` مراقب عرض نطاق — وكلتاهما
     * تُعلن سلّمًا وترددًا جاريًا، وهو ما جعلهما **متعادلتين بالدرجة نفسها** في الحساب القديم.
     *
     * والزوج (مجلّد `devfreq` ← `device_name`) مطلوب لأن الدور يُقرأ من **الاثنين** مع المسار.
     */
    private fun qualcommIo(vararg nodes: Pair<String, String>): FakeIo {
        val values = mutableMapOf<String, String>()
        val writable = mutableSetOf<String>()
        nodes.forEach { (dir, declared) ->
            val base = "/sys/class/devfreq/$dir"
            values["$base/device_name"] = declared
            values["$base/available_frequencies"] = "220000000 320000000 565000000 719000000"
            values["$base/cur_freq"] = "320000000"
            values["$base/min_freq"] = "220000000"
            values["$base/max_freq"] = "719000000"
            values["$base/governor"] = "msm-adreno-tz"
            values["$base/available_governors"] = "msm-adreno-tz performance powersave"
            writable += listOf("$base/min_freq", "$base/max_freq", "$base/governor")
        }
        return FakeIo(values, writable)
    }

    private val gpuClock = "3d00000.qcom,kgsl-3d0" to "kgsl-3d0"
    private val busMonitor = "3d00000.qcom,kgsl-busmon" to "kgsl-busmon"

    @Test fun aBandwidthMonitorDoesNotTieWithTheClockNode() {
        // الاسم الحقيقي لمراقب النطاق على سنابدراجون يبدأ بمعرّف جهاز الـGPU نفسه
        // (`3d00000.qcom,kgsl-busmon`) — وهذا هو ما يجعل الفصل على `kgsl-3d` لا على `kgsl`.
        val fake = qualcommIo(gpuClock, busMonitor)
        val selection = GpuHardwareBackend.selection(fake)

        assertEquals(GpuHardwareBackend.SelectionState.READY, selection.state)
        assertEquals("kgsl-3d0", selection.device?.name)
        assertEquals("verified-gpu-provider", selection.reason)
        // والمراقب يبقى **مرئيًّا**: يُعرض في المرشّحات، ويُسمّى في ملخّص الاختيار، ويُعرف دوره —
        // فمن يقرأ اللقطة أو السجل يرى ما رُصد كاملًا، وليس ما اختير وحده.
        val monitor = selection.candidates.first { it.name == "kgsl-busmon" }
        assertEquals(GpuHardwareBackend.NodeRole.BANDWIDTH_MONITOR, monitor.role)
        assertTrue(selection.candidateSummary.contains("kgsl-busmon:bandwidth-monitor"))
        assertEquals("kgsl-busmon", selection.roleExcludedSummary)
    }

    @Test fun monitorsOnlyIsItsOwnAbsenceReasonNotSilence() {
        // «لا عقدة تُرصد» و«عُقَد رُصدت وليست مزوّدًا» حالتان مختلفتان: الأولى قد يُصلحها تثبيت
        // الوحدة، والثانية لا شيء في التطبيق يفتحها. فلكلٍّ رمزه، ولا يُعلَن أحدهما بالآخر.
        val fake = qualcommIo(busMonitor)
        val selection = GpuHardwareBackend.selection(fake)

        assertEquals(GpuHardwareBackend.SelectionState.UNAVAILABLE, selection.state)
        assertNull(selection.device)
        assertEquals(GpuHardwareBackend.BANDWIDTH_MONITOR_REASON, selection.reason)
        assertEquals(listOf("kgsl-busmon"), selection.candidates.map { it.name })
    }

    @Test fun noGraphicsNodeAtAllKeepsThePlainAbsenceReason() {
        val fake = FakeIo(
            mutableMapOf(
                "/sys/class/devfreq/dmc/device_name" to "dmc",
                "/sys/class/devfreq/dmc/cur_freq" to "1000000",
            ),
        )
        val selection = GpuHardwareBackend.selection(fake)

        assertEquals(GpuHardwareBackend.SelectionState.UNAVAILABLE, selection.state)
        assertEquals("no-gpu-provider", selection.reason)
        assertTrue("عقدة ليست رسومية لا تُعرض باسم مزوّد", selection.candidates.isEmpty())
    }

    @Test fun anExistingRootThatCannotBeListedIsAMeasurementGapNotAbsence() {
        // مقيس في حزمة سنابدراجون (manet · SM8650 · ٢٠٢٦-١٠-١٤): `/sys/class/devfreq` موجود على
        // النواة و`avc denied { search }` يحجبه عن uid التطبيق، فيقرأ الاكتشاف صفر أسماء ويقول
        // «لا مزوّد». والرمز الجديد يفصل الحالتين: جذر محجوب إجراؤه الجذر، ولا مزوّد إجراؤه جهاز
        // آخر — وهو رمز عدّ الذاكرة/التخزين نفسه، فلا يتعلّم قارئ الحزمة رمزين لنقص واحد.
        val fake = FakeIo(mutableMapOf("/sys/class/devfreq" to ""))
        val selection = GpuHardwareBackend.selection(fake)

        assertEquals(GpuHardwareBackend.SelectionState.UNAVAILABLE, selection.state)
        assertEquals(MemoryUfsHardwareBackend.REASON_ROOT_UNREADABLE, selection.reason)
    }

    @Test fun theClockSignatureBeatsABareFamilyToken() {
        // `kgsl` المجرّد توقيعُ عائلة لا توقيع ساعة: كان يأخذ ١٠٠ كما تأخذها `kgsl-3d0`، فيتعادلان
        // على جهاز يحمل الاثنين (وهو تعادل آخر بالسبب نفسه). والترتيب الجديد يُسمّي الساعة.
        val fake = qualcommIo(gpuClock, "kgsl" to "kgsl")
        val selection = GpuHardwareBackend.selection(fake)

        assertEquals(GpuHardwareBackend.SelectionState.READY, selection.state)
        assertEquals("kgsl-3d0", selection.device?.name)
    }

    @Test fun theEvidenceNamesTheRoleOfEveryNode() {
        // الدور يُكتب في الدليل لا يُستنتج عند القارئ: من يفتح البصمة أو الحزمة يعرف أيّ عقدة
        // وُصفت بأيّ دور، وأيّها استُبعد ولِمَ.
        val fake = qualcommIo(gpuClock, busMonitor)
        val selection = GpuHardwareBackend.selection(fake)

        assertTrue(selection.device!!.evidence.contains("role-gpu-clock"))
        val monitor = selection.candidates.first { it.name == "kgsl-busmon" }
        assertTrue(monitor.evidence.contains("role-bandwidth-monitor"))
        assertTrue(monitor.evidence.contains("not-a-gpu-clock-owner"))
    }

    @Test fun aTieBetweenRealProvidersStillNamesBothNodes() {
        // التعادل بين مزوّدين حقيقيين يبقى مُعلَنًا (لا «الأول يفوز»)، لكنه صار **مقروءًا**:
        // الاسم والدور في اللقطة والسجل، فلا تُشخَّص حزمة تقول `multiple-equally-proven-gpu-providers`
        // بلا أن تقول أيّ عقدتين تعادلتا.
        val values = mutableMapOf<String, String>()
        listOf("test-gpu", "other-gpu").forEach { node ->
            val base = "/sys/class/devfreq/$node"
            values["$base/device_name"] = "mali-gpu"
            values["$base/available_frequencies"] = "100000000 200000000 400000000 800000000"
            values["$base/cur_freq"] = "200000000"
            values["$base/min_freq"] = "100000000"
            values["$base/max_freq"] = "800000000"
            values["$base/governor"] = "simple_ondemand"
            values["$base/available_governors"] = "simple_ondemand performance"
        }
        val selection = GpuHardwareBackend.selection(FakeIo(values))

        assertEquals(GpuHardwareBackend.SelectionState.AMBIGUOUS, selection.state)
        assertEquals("multiple-equally-proven-gpu-providers", selection.reason)
        assertTrue(selection.candidateSummary.contains("test-gpu"))
        assertTrue(selection.candidateSummary.contains("other-gpu"))
    }

    @Test fun thePublishedSnapshotCarriesTheRoleWithoutANewField() {
        // اللقطة تنقل الاسم والمسار ولا تنقل الدور (١٥ حقلًا ثابتة)، والدور يُعاد اشتقاقه عند الفكّ:
        // فلا تُغيَّر صيغة ولا نسخة، ولقطة قديمة فيها المراقب بدرجة مزوّد تُقرأ بدورها الصحيح.
        val fake = qualcommIo(gpuClock, busMonitor)
        val entries = GpuHardwareBackend.selection(fake).candidates
            .map { GpuDiscoverySnapshot.Entry(it, score = 100) }
        val parsed = GpuDiscoverySnapshot.parse(GpuDiscoverySnapshot.encode(1_000L, entries))

        assertNotNull(parsed)
        val monitor = parsed!!.devices.first { it.name == "kgsl-busmon" }
        assertEquals(GpuHardwareBackend.NodeRole.BANDWIDTH_MONITOR, monitor.role)
        val clock = parsed.devices.first { it.name == "kgsl-3d0" }
        assertEquals(GpuHardwareBackend.NodeRole.GPU_CLOCK, clock.role)
    }
}
