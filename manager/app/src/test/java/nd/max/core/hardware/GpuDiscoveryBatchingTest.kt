/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §4 — **دفعات القراءة في الاكتشاف**: العقد الصغيرة المتقاربة تُقرأ في نداء واحد.
 *
 * وما يُقاس هنا ليس الساعة بل **عدد الرحلات** — وهو الجزء الحتميّ الذي يُقاس بلا جهاز: مسار
 * واحد من [GpuHardwareBackend.refresh] كان يقرأ عقدةً عقدة (ستّ ساعات للمرشّح بعد الهوية،
 * وثلاث عقد حِمل، و`type` لكل منطقة حرارية ثم درجة المطابقة) — ثلاث عشرة رحلة في العيّنة
 * الواحدة، وصارت **أربع دفعات** صفرًا فيها من القراءات الفرديّة. وأمّا الأثر الزمني فيُقاس
 * على جهاز وهو مُعلن في التقرير لا مُدَّعى.
 *
 * والوهميّ هنا يحفظ دلالة القارئ الحقيقيّ: المقصوص غير الفارغ أو `null` — فلا يمرّ اختبار
 * لقارئ أرحم من القارئ الذي يعمل على الجهاز، ويُفشل الدفعةَ إن أرحمَت.
 */
class GpuDiscoveryBatchingTest {

    private class CountingIo(
        private val directories: Map<String, List<String>>,
        private val files: Map<String, String>,
    ) : GpuHardwareBackend.Io {
        val singleReads = mutableListOf<String>()
        val batches = mutableListOf<List<String>>()
        val writableChecks = mutableListOf<String>()

        override fun exists(path: String): Boolean =
            files.containsKey(path) || directories.containsKey(path)

        override fun read(path: String): String? {
            singleReads += path
            return files[path]?.trim()?.takeIf(String::isNotEmpty)
        }

        override fun readMany(paths: List<String>): List<String?> {
            batches += paths
            return paths.map { files[it]?.trim()?.takeIf(String::isNotEmpty) }
        }

        override fun listDirectories(path: String): List<String> = directories[path].orEmpty()

        override fun writable(path: String): Boolean {
            writableChecks += path
            return false
        }

        override fun write(path: String, value: String): Boolean = false
    }

    private val root = "/sys/class/devfreq"
    private val thermalRoot = "/sys/class/thermal"
    private val node = "$root/gpu0"

    /** شجرة قزمة تحمل ما يلزم مرشّحًا واحدًا: هوية، وساعات، وثلاث مناطق حرارية بإحداها `gpu`. */
    private fun fakeTree(): CountingIo = CountingIo(
        directories = mapOf(
            root to listOf("gpu0"),
            thermalRoot to listOf("thermal_zone0", "thermal_zone1", "thermal_zone2"),
        ),
        files = mapOf(
            "$node/device_name" to "kgsl-3d0\n",
            "$node/available_frequencies" to "300000000 600000000\n",
            "$node/min_freq" to "300000000\n",
            "$node/max_freq" to "600000000\n",
            "$node/cur_freq" to "600000000\n",
            "$node/governor" to "performance\n",
            "$node/available_governors" to "performance simple_ondemand\n",
            "$thermalRoot/thermal_zone0/type" to "battery\n",
            "$thermalRoot/thermal_zone1/type" to "gpu\n",
            "$thermalRoot/thermal_zone2/type" to "quiet_therm\n",
            "$thermalRoot/thermal_zone1/temp" to "47000\n",
        )
    )

    @Test
    fun `the candidate clock reads arrive as one batch after the identity read`() {
        val io = fakeTree()
        assertNotNull(GpuHardwareBackend.refresh(node, io))

        // الفرديّ الوحيد الباقي هو سؤال الهوية الذي يسبق أيّ دفعة: من لا يعرف اسمه لا يُقرأ له.
        assertEquals(listOf("$node/device_name"), io.singleReads)

        val clocks = io.batches.firstOrNull { "$node/available_frequencies" in it }
        assertNotNull(clocks)
        assertEquals(6, clocks!!.size)
        assertTrue("$node/governor" in clocks)
        assertTrue("$node/available_governors" in clocks)
    }

    @Test
    fun `the load read tries its nodes in one batch instead of a chain`() {
        val io = fakeTree()
        assertNotNull(GpuHardwareBackend.refresh(node, io))

        // عائلة `kgsl` تجعل مسار الحِمل ثلاثة: عقدتان عامّتان وسطح Qualcomm — كلّها في نداء واحد.
        val load = io.batches.firstOrNull { "$node/load" in it }
        assertNotNull(load)
        assertTrue("$node/device/load" in load!!)
        assertTrue("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage" in load)
        assertTrue(io.singleReads.none { it.endsWith("/load") })
    }

    @Test
    fun `the thermal scan reads every zone type in one batch and only the matched temperature in another`() {
        val io = fakeTree()
        val device = GpuHardwareBackend.refresh(node, io)
        assertNotNull(device)
        // الرقم نفسه لم يتغيّر: ٤٧٠٠٠ → 47°، والمناطق غير المطابقة لا تُقرأ درجتها أصلًا.
        assertEquals(47, device!!.thermalC!!)

        val types = io.batches.firstOrNull { batch -> batch.all { it.endsWith("/type") } }
        assertNotNull(types)
        assertEquals(3, types!!.size)
        val temps = io.batches.firstOrNull { batch -> batch.any { it.endsWith("/temp") } }
        assertEquals(listOf("$thermalRoot/thermal_zone1/temp"), temps)
        assertTrue(io.singleReads.none { it.startsWith(thermalRoot) })
    }

    @Test
    fun `no node is read one by one when a batch can carry it`() {
        // الحصيلة الكاملة للعيّنة: أربع دفعات (ساعات · حِمل · أنواع · درجات) وصفر فرديّات
        // عدا الهوية — وهو الفرق المقيس بين «ثلاث عشرة رحلة» و«أربع».
        val io = fakeTree()
        assertNotNull(GpuHardwareBackend.refresh(node, io))
        assertEquals(4, io.batches.size)
        assertEquals(listOf("$node/device_name"), io.singleReads)
    }

    @Test
    fun `the mediaTek opp surfaces are read in one batch`() {
        val io = fakeTree()
        // لا أسطح OPP في الشجرة: العائد فراغ — والقياس أن الأسطح الثلاثة سُئلت في نداء واحد.
        val discovery = MtkGpuOppTable.discover(io)
        assertEquals(0, discovery.frequencies.size)
        val opp = io.batches.firstOrNull { batch -> MtkGpuOppTable.OPP_TABLES.all { it in batch } }
        assertNotNull(opp)
        assertEquals(MtkGpuOppTable.OPP_TABLES.size, opp!!.size)
        assertTrue(io.singleReads.none { it in MtkGpuOppTable.OPP_TABLES })
    }

    @Test
    fun `the read-only adapter hands the batch through instead of unrolling it`() {
        // `ReadOnlyIo` هو ما يمرّ به Atlas: لو فكّ الدفعة إلى فرديّات لَضاع ما يمنحه §4 على
        // مسار كامل — والوسيط يجب أن يكون شفّافًا لا مُفسِّرًا.
        val io = fakeTree()
        val readOnly = GpuHardwareBackend.ReadOnlyIo(io)
        assertEquals(listOf<String?>(null, null), readOnly.readMany(listOf("/a", "/b")))
        assertEquals(1, io.batches.size)
        assertEquals(emptyList<String>(), io.singleReads)
    }
}
