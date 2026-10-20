/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import nd.max.core.atlas.AtlasControlGoal
import nd.max.core.atlas.AtlasControlIntent
import nd.max.core.atlas.AtlasControlTarget
import nd.max.core.atlas.AtlasControlTransport
import nd.max.core.atlas.AtlasSafetyVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * مُلاءِمو الذاكرة وUFS — يُقاس هنا العقد لا الأسماء: أي هدف، وأي مفتاح قياسي، وأي رمز رفض،
 * وأن الكتابة **لا تقع داخل الملاءِم** بل عبر عقد المعاملة.
 *
 * والاختبار يعمل على جهاز مصطنع (`FakeAccess`) لأن السؤال «هل يصل القرار إلى الكتابة الصحيحة؟»
 * سؤالُ منطق لا سؤالُ عتاد — وأما «هل يقبل كيرنل هاتفك الكتابة؟» فجوابه على هاتف.
 */
class AtlasMemoryUfsAdaptersTest {

    @Test
    fun `the memory adapter owns the memory target and the ufs adapter the storage target`() {
        val memory = MemoryDevfreqCeilingAdapter()
        val ufs = UfsDevfreqCeilingAdapter()

        assertEquals(AtlasControlTarget.MEMORY, memory.target)
        assertEquals(AtlasControlTarget.STORAGE, ufs.target)
        assertEquals(AtlasControlTransport.ARBITER_SYSFS, memory.transport)
        // ومعرّفان مختلفان وإلا صار سجلّ الملاءِمين نفسه غير صالح (المعرّف هو مفتاح الاختيار).
        assertFalse(memory.id == ufs.id)
    }

    @Test
    fun `a plan builds one canonical route per writable device`() {
        val access = FakeAccess()
        val context = context(
            memory = listOf(
                device(name = "dvfsrc", path = "/sys/class/devfreq/dvfsrc"),
                device(name = "memlat-cpu4", path = "/sys/class/devfreq/memlat4"),
            ),
            access = access,
        )

        val plan = MemoryDevfreqCeilingAdapter().plan(context, memoryCeilings(mapOf("dvfsrc" to 1_200_000_000L, "memlat-cpu4" to 600_000_000L)))

        assertEquals("memory-devfreq-ceiling", plan.adapterId)
        assertEquals(
            listOf("mem_frequency:dvfsrc", "mem_frequency:memlat-cpu4"),
            plan.bindings.map { it.request.key },
        )
        assertTrue(plan.skipped.isEmpty())
    }

    @Test
    fun `a device with no published ceiling for the request is skipped by name`() {
        val access = FakeAccess()
        val context = context(memory = listOf(device(name = "dvfsrc")), access = access)

        // طلبٌ دون أدنى درجة مُعلنة (٣٠٠MHz) لا مسار له: الجهاز لا يقبل قيمة لا وجود لها.
        val plan = MemoryDevfreqCeilingAdapter().plan(context, memoryCeilings(mapOf("dvfsrc" to 100_000_000L)))

        assertTrue(plan.bindings.isEmpty())
        assertEquals(listOf("mem_frequency:dvfsrc" to "no-proven-range"), plan.skipped)
    }

    @Test
    fun `a request with no value for a device produces no route`() {
        val access = FakeAccess()
        val context = context(memory = listOf(device(name = "dvfsrc")), access = access)

        val plan = MemoryDevfreqCeilingAdapter().plan(context, memoryCeilings(mapOf("dvfsrc" to null)))

        assertTrue(plan.bindings.isEmpty())
        assertEquals(listOf("mem_frequency:dvfsrc" to "no-requested-value"), plan.skipped)
    }

    @Test
    fun `an unproven unit is skipped instead of written`() {
        val access = FakeAccess()
        val context = context(
            memory = listOf(device(name = "dvfsrc", unit = GpuHardwareBackend.FrequencyUnit.AMBIGUOUS)),
            access = access,
        )

        val plan = MemoryDevfreqCeilingAdapter().plan(context, memoryCeilings(mapOf("dvfsrc" to 1_200_000_000L)))

        assertTrue(plan.bindings.isEmpty())
        assertEquals(listOf("mem_frequency:dvfsrc" to "frequency-unit-unproven"), plan.skipped)
    }

    @Test
    fun `a read-only node is skipped with its own code`() {
        val access = FakeAccess()
        val context = context(memory = listOf(device(name = "dvfsrc", minWritable = false)), access = access)

        val plan = MemoryDevfreqCeilingAdapter().plan(context, memoryCeilings(mapOf("dvfsrc" to 1_200_000_000L)))

        assertEquals(listOf("mem_frequency:dvfsrc" to "devfreq-range-not-writable"), plan.skipped)
    }

    @Test
    fun `a node on a never-touch path is dropped before any route is built`() {
        val access = FakeAccess()
        // `/sys/power` عائلة ممنوعة في سياسة السلامة (مداخل التعليق والإسبات لا مقابض أداء).
        val context = context(
            memory = listOf(device(name = "dvfsrc", path = "/sys/power/devfreq/dvfsrc")),
            access = access,
        )

        val plan = MemoryDevfreqCeilingAdapter().plan(context, memoryCeilings(mapOf("dvfsrc" to 1_200_000_000L)))

        assertTrue(plan.bindings.isEmpty())
        assertEquals(listOf("mem_frequency:dvfsrc" to "never-touch:power-state"), plan.skipped)
        assertEquals(AtlasSafetyVerdict.Allowed, nd.max.core.atlas.AtlasSafetyPolicy.verdictFor("dvfsrc", "/sys/class/devfreq/dvfsrc"))
    }

    @Test
    fun `without a write seam no write route is built at all`() {
        val context = context(memory = listOf(device(name = "dvfsrc")), access = null)

        val plan = MemoryDevfreqCeilingAdapter().plan(context, memoryCeilings(mapOf("dvfsrc" to 1_200_000_000L)))

        assertTrue(plan.bindings.isEmpty())
        assertEquals(listOf("devfreq-ceiling" to "no-write-seam-available"), plan.skipped)
    }

    @Test
    fun `a request of the wrong shape is refused rather than coerced`() {
        val access = FakeAccess()
        val context = context(memory = listOf(device(name = "dvfsrc")), access = access)

        val plan = MemoryDevfreqCeilingAdapter().plan(
            context,
            AtlasControlRequest.StorageCeilings(
                intent = AtlasControlIntent(AtlasControlTarget.STORAGE, AtlasControlGoal.SUSTAINED_PERFORMANCE),
                owner = ControlOwnership.Owner.MAX_AI,
                token = "adapter-test",
                ceilingByKnob = mapOf("dvfsrc" to 1_200_000_000L),
            ),
        )

        assertEquals(listOf("devfreq-ceiling" to "wrong-request-shape"), plan.skipped)
    }

    @Test
    fun `the representative probe builds a route and writes nothing`() {
        val access = FakeAccess()
        val context = context(memory = listOf(device(name = "dvfsrc")), access = access)

        val plan = MemoryDevfreqCeilingAdapter().probe(context)

        assertEquals(1, plan.bindings.size)
        assertEquals("mem-frequency-dvfsrc", plan.bindings.single().candidate.id)
        assertEquals(0, access.writes)
        // والتمثيل يجيب بإشارة **مقيسة** لا مُدّعاة: الوحدة مُثبتة، وخط الأساس مقروء (أو لا)،
        // والمراجعة من المفتاح القياسي — وهي التي تحدّد هل يُعدّ المسار مؤهّلًا في الخريطة.
        val evidence = plan.bindings.single().candidate.evidence
        assertTrue(evidence.unitProven)
        assertTrue(evidence.readable)
        assertTrue(evidence.reviewed)
        assertEquals("memory-ufs-devfreq", evidence.providerId)
    }

    @Test
    fun `executing the planned binding writes through the seam and never around it`() {
        val access = FakeAccess()
        val path = "/sys/class/devfreq/dvfsrc"
        val context = context(memory = listOf(device(name = "dvfsrc", path = path)), access = access)
        val plan = MemoryDevfreqCeilingAdapter().plan(context, memoryCeilings(mapOf("dvfsrc" to 1_200_000_000L)))

        val binding = plan.bindings.single()
        val applied = binding.request.apply(binding.request.desired)

        assertTrue(applied)
        assertEquals(1, access.writes)
        // وما كُتب هو **نصّ الطلب نفسه** لا صورة محفوظة منه — وهو ما يمنع انزلاق المُنفَّذ عن المفحوص.
        assertEquals(binding.request.desired, access.nodes[path])
        // وطلبٌ بلا أرضية يُرمَّز بخانة أرضية فارغة لا بصفر: «لا قيمة» ≠ «قيمة صفرية».
        assertEquals(":1200000000|-", binding.request.desired)
    }

    @Test
    fun `a requested floor is clipped into the ceiling inside the built route`() {
        val access = FakeAccess()
        val context = context(memory = listOf(device(name = "dvfsrc")), access = access)

        val plan = MemoryDevfreqCeilingAdapter().plan(
            context,
            memoryCeilings(
                ceilings = mapOf("dvfsrc" to 600_000_000L),
                floors = mapOf("dvfsrc" to 1_500_000_000L),
            ),
        )

        val desired = plan.bindings.single().request.desired
        val floor = DevfreqEncoding.floor(desired)
        val ceiling = DevfreqEncoding.ceiling(desired)
        assertEquals(600_000_000L, ceiling)
        assertTrue("الأرضية لا تعلو السقف", floor != null && floor <= ceiling!!)
    }

    @Test
    fun `a node we saw but could not name is a different gap from no node at all`() {
        val adapter = MemoryDevfreqCeilingAdapter()

        // عقدة تُجيب بسلّم ولا يعرف صنفها المحرّك: الفجوة ليست «لا شيء هنا» بل «صنفٌ ينقصه اسم».
        val unexplained = AtlasAdapterContext(
            privileged = true,
            devfreqAccess = FakeAccess(),
            unclassifiedDevfreq = listOf(
                MemoryUfsHardwareBackend.Unclassified(
                    directory = "mtk-memnoc",
                    declared = "mtk-memnoc",
                    ladderSize = 5,
                    writable = true,
                ),
            ),
        )

        assertEquals(
            AtlasAdapterAssessment.NotApplicable(MemoryUfsHardwareBackend.UNCLASSIFIED_GAP_CODE),
            adapter.assess(unexplained, null),
        )
        assertEquals(
            AtlasAdapterAssessment.NotApplicable("no-devfreq-device-seen"),
            adapter.assess(AtlasAdapterContext(privileged = true, devfreqAccess = FakeAccess()), null),
        )
    }

    @Test
    fun `assess refuses when no device of the kind was seen`() {
        val adapter = MemoryDevfreqCeilingAdapter()

        assertEquals(
            AtlasAdapterAssessment.NotApplicable("no-devfreq-device-seen"),
            adapter.assess(AtlasAdapterContext(privileged = true, devfreqAccess = FakeAccess()), null),
        )
        assertEquals(
            AtlasAdapterAssessment.NotApplicable("devfreq-range-not-writable"),
            adapter.assess(
                AtlasAdapterContext(
                    privileged = true,
                    memoryDevices = listOf(device(name = "dvfsrc", minWritable = false, maxWritable = false)),
                    devfreqAccess = FakeAccess(),
                ),
                null,
            ),
        )
    }

    @Test
    fun `the storage adapter reads the storage list and not the memory list`() {
        val access = FakeAccess()
        val context = AtlasAdapterContext(
            privileged = true,
            memoryDevices = listOf(device(name = "dvfsrc")),
            storageDevices = listOf(device(name = "112b0000.ufshci", kind = MemoryUfsHardwareBackend.Kind.STORAGE)),
            devfreqAccess = access,
        )

        val plan = UfsDevfreqCeilingAdapter().plan(
            context,
            AtlasControlRequest.StorageCeilings(
                intent = AtlasControlIntent(AtlasControlTarget.STORAGE, AtlasControlGoal.SUSTAINED_PERFORMANCE),
                owner = ControlOwnership.Owner.MAX_AI,
                token = "adapter-test",
                ceilingByKnob = mapOf("112b0000.ufshci" to 1_200_000_000L),
            ),
        )

        assertEquals(listOf("ufs_frequency:112b0000.ufshci"), plan.bindings.map { it.request.key })
    }

    @Test
    fun `the chosen seam is asked once per plan, not once per device`() {
        val access = CountingAccess()
        val context = context(
            memory = listOf(device(name = "a"), device(name = "b"), device(name = "c")),
            access = access,
        )

        MemoryDevfreqCeilingAdapter().plan(context, memoryCeilings(mapOf("a" to 600_000_000L, "b" to 600_000_000L, "c" to 600_000_000L)))

        // `privileged` في الثقب الإنتاجي يعدّد `/sys/class/devfreq` كاملًا؛ سؤالُها لكل جهاز
        // يضاعف القراءة بعدد العقد بلا معلومة جديدة.
        assertEquals(1, access.privilegeQueries)
    }

    @Test
    fun `the devfreq keys are the ones this review covers, and gpu keys are not claimed`() {
        // إعلانُ مراجعةٍ مكتوبٌ كاختبار: مفاتيح الذاكرة وUFS تسلك معاملة مُحكِّم كاملة عبر
        // `MemoryUfsHardwareBackend`، ومفتاح GPU له مالكه الآخر ولا يُدَّعى هنا.
        assertTrue(AtlasDiscoveredControl.isReviewedControlRoute(HardwareControlKey.memoryFrequency("dvfsrc")))
        assertTrue(AtlasDiscoveredControl.isReviewedControlRoute(HardwareControlKey.ufsFrequency("112b0000.ufshci")))
        assertTrue(AtlasDiscoveredControl.isReviewedControlRoute(HardwareControlKey.cpuLimits("policy0")))
        assertFalse(AtlasDiscoveredControl.isReviewedControlRoute(HardwareControlKey.gpuFrequency("13000000.mali")))
        assertFalse(AtlasDiscoveredControl.isReviewedControlRoute("audio_stream:music"))
    }

    // ── أدوات الاختبار ─────────────────────────────────────────────────────────────────────────

    private fun memoryCeilings(
        ceilings: Map<String, Long?>,
        floors: Map<String, Long?> = emptyMap(),
    ) = AtlasControlRequest.MemoryCeilings(
        intent = AtlasControlIntent(AtlasControlTarget.MEMORY, AtlasControlGoal.SUSTAINED_PERFORMANCE),
        owner = ControlOwnership.Owner.MAX_AI,
        token = "adapter-test",
        ceilingByKnob = ceilings,
        floorByKnob = floors,
    )

    private fun context(
        memory: List<MemoryUfsHardwareBackend.Device> = emptyList(),
        storage: List<MemoryUfsHardwareBackend.Device> = emptyList(),
        access: AtlasDevfreqCeilingAccess?,
    ) = AtlasAdapterContext(
        privileged = true,
        memoryDevices = memory,
        storageDevices = storage,
        devfreqAccess = access,
    )

    private fun device(
        name: String,
        path: String = "/sys/class/devfreq/$name",
        kind: MemoryUfsHardwareBackend.Kind = MemoryUfsHardwareBackend.Kind.MEMORY,
        unit: GpuHardwareBackend.FrequencyUnit = GpuHardwareBackend.FrequencyUnit.HZ,
        minWritable: Boolean = true,
        maxWritable: Boolean = true,
    ) = MemoryUfsHardwareBackend.Device(
        path = path,
        name = name,
        kind = kind,
        family = MemoryUfsHardwareBackend.Family.GENERIC,
        governor = "simple_ondemand",
        governors = listOf("simple_ondemand", "performance"),
        minFreq = LADDER.first(),
        maxFreq = LADDER.last(),
        currentFreq = LADDER.first(),
        frequencies = LADDER,
        frequencyUnit = unit,
        minWritable = minWritable,
        maxWritable = maxWritable,
        governorWritable = true,
        evidence = listOf("devfreq-device"),
    )

    private open class FakeAccess : AtlasDevfreqCeilingAccess {
        override val privileged: Boolean = true
        val nodes = mutableMapOf<String, String>()
        var writes: Int = 0

        override fun readBaseline(devicePath: String): String? =
            nodes[devicePath] ?: "300000000:2000000000|simple_ondemand"

        override fun writeRange(devicePath: String, encoded: String): Boolean {
            writes += 1
            nodes[devicePath] = encoded
            return true
        }

        override fun restoreBaseline(devicePath: String, encoded: String): Boolean {
            nodes[devicePath] = encoded
            return true
        }
    }

    private class CountingAccess : AtlasDevfreqCeilingAccess {
        var privilegeQueries: Int = 0
        override val privileged: Boolean get() {
            privilegeQueries += 1
            return true
        }

        override fun readBaseline(devicePath: String): String = "300000000:2000000000|simple_ondemand"

        override fun writeRange(devicePath: String, encoded: String): Boolean = true

        override fun restoreBaseline(devicePath: String, encoded: String): Boolean = true
    }

    private companion object {
        val LADDER = listOf(300_000_000L, 600_000_000L, 1_200_000_000L, 1_500_000_000L, 2_000_000_000L)
    }
}
