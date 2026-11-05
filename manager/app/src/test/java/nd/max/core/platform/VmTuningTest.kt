/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 */
package nd.max.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * مقابض `/proc/sys/vm` — الحدود والتحقّق يُقاسان بقراءة/كتابة مزيّفة، بلا نواة ولا جهاز.
 *
 * **والعطب الذي تحرسه هذه الدعاوى:** كتابة قيمة خارج المدى ثم «نجاح» لأن الملفّ قَبِلها.
 * `dirty_ratio = 0` يُلغي خنق الكتابة القذرة، و`min_free_kbytes` صغير جدًّا يجعل النواة تقتل
 * العمليّات تحت الضغط — والاثنان يبدوان كسطرٍ ناجح في السجلّ.
 */
class VmTuningTest {

    private class Fake {
        val nodes = HashMap<String, String>()
        var writes = 0
        var acceptWrites = true
        var ignoreWrites = false

        fun tuning() = VmTuning(
            read = { path -> nodes[path] },
            write = { path, value ->
                writes++
                if (!acceptWrites) false else {
                    if (!ignoreWrites) nodes[path] = value
                    true
                }
            },
        )
    }

    @Test
    fun kernelTextIsParsedStrictly() {
        assertEquals(60L, VmTuning.parse("60\n"))
        assertEquals(-5L, VmTuning.parse(" -5 "))
        assertEquals(0L, VmTuning.parse("0"))
        assertNull("نصّ مختلط لا يُفهم", VmTuning.parse("12abc"))
        assertNull(VmTuning.parse(""))
        assertNull(VmTuning.parse(null))
        assertNull(VmTuning.parse("  "))
        assertNull("تجاوز ٣٢ بت", VmTuning.parse("99999999999999999999"))
    }

    @Test
    fun aValueAboveTheRangeIsRejectedBeforeAnyWrite() {
        val fake = Fake()
        fake.nodes[VmTunable.SWAPPINESS.path] = "60"

        assertEquals(VmWriteOutcome.REJECTED_RANGE, fake.tuning().set(VmTunable.SWAPPINESS, 201))
        assertEquals("لا كتابة على الرفض", 0, fake.writes)
        assertEquals("60", fake.nodes[VmTunable.SWAPPINESS.path])
    }

    @Test
    fun theEdgesOfTheRangeAreAccepted() {
        val fake = Fake()
        fake.nodes[VmTunable.SWAPPINESS.path] = "60"

        assertEquals(VmWriteOutcome.APPLIED, fake.tuning().set(VmTunable.SWAPPINESS, 200))
        assertEquals("200", fake.nodes[VmTunable.SWAPPINESS.path])
        assertEquals(VmWriteOutcome.APPLIED, fake.tuning().set(VmTunable.SWAPPINESS, 0))
        assertEquals("0", fake.nodes[VmTunable.SWAPPINESS.path])
    }

    @Test
    fun aMissingNodeIsNotWritable() {
        val fake = Fake()

        assertEquals(
            VmWriteOutcome.REJECTED_NOT_WRITABLE,
            fake.tuning().set(VmTunable.EXTRA_FREE_KBYTES, 4096),
        )
        assertEquals(0, fake.writes)
    }

    @Test
    fun aWriteTheKernelIgnoresIsNotSuccess() {
        val fake = Fake()
        fake.nodes[VmTunable.DIRTY_RATIO.path] = "20"
        fake.ignoreWrites = true

        assertEquals(VmWriteOutcome.UNVERIFIED, fake.tuning().set(VmTunable.DIRTY_RATIO, 30))
        assertEquals("20", fake.nodes[VmTunable.DIRTY_RATIO.path])
    }

    @Test
    fun aRefusedWriteIsReportedAsNotWritable() {
        val fake = Fake()
        fake.nodes[VmTunable.VFS_CACHE_PRESSURE.path] = "100"
        fake.acceptWrites = false

        assertEquals(
            VmWriteOutcome.REJECTED_NOT_WRITABLE,
            fake.tuning().set(VmTunable.VFS_CACHE_PRESSURE, 150),
        )
    }

    @Test
    fun theSnapshotReadsEveryKnobAndMissingOnesAreNull() {
        val fake = Fake()
        fake.nodes[VmTunable.SWAPPINESS.path] = "100"
        fake.nodes[VmTunable.DIRTY_RATIO.path] = "20"

        val snapshot = fake.tuning().snapshot()

        assertEquals(VmTunable.entries.size, snapshot.size)
        assertEquals(100L, snapshot[VmTunable.SWAPPINESS])
        assertEquals(20L, snapshot[VmTunable.DIRTY_RATIO])
        assertNull(snapshot[VmTunable.MIN_FREE_KBYTES])
    }

    @Test
    fun everyKnobHasItsOwnDocumentedRange() {
        VmTunable.entries.forEach { tunable ->
            assertTrue("${tunable.path}: مدى مقلوب", tunable.min <= tunable.max)
            assertTrue("${tunable.path}: خارج شجرة vm", tunable.path.startsWith("/proc/sys/vm/"))
        }
        assertEquals(
            "dirty_ratio = 0 يُلغي خنق الكتابة القذرة — الحدّ الأدنى ١",
            1L,
            VmTunable.DIRTY_RATIO.min,
        )
    }
}
