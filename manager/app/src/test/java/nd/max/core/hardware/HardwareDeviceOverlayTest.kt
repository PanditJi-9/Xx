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
import org.junit.Test

/**
 * ملفّ **تصنيف المستخدم** — يُقاس هنا ما يجوز أن يُعلَّم وما لا يجوز، لغةً واحدة.
 *
 * والقاعدة التي تحكم كل اختبار في هذا الملفّ: ما يُرفض يُرفض **برمزه**، والملفّ لا يسقط بسطر.
 * وهذا هو الفرق بين ملفّ يُصلَّح وبين ملفّ يُهمَل بعد أول خطأ مطبعيّ.
 */
class HardwareDeviceOverlayTest {

    private val temps = mutableListOf<File>()

    @After
    fun cleanUp() {
        temps.forEach { it.deleteRecursively() }
        temps.clear()
    }

    @Test
    fun `a taught line names a device and one of two kinds`() {
        val parsed = HardwareDeviceOverlay.parse(
            """
            # تعليم جهازي
            mtk-memnoc   memory
            112b0000.ufshci storage   # تعليق في نهاية السطر
            """.trimIndent(),
        )

        assertEquals(emptyList<HardwareDeviceOverlay.Rejection>(), parsed.rejections)
        assertEquals(
            listOf(
                HardwareDeviceOverlay.Entry("mtk-memnoc", MemoryUfsHardwareBackend.Kind.MEMORY),
                HardwareDeviceOverlay.Entry("112b0000.ufshci", MemoryUfsHardwareBackend.Kind.STORAGE),
            ),
            parsed.entries,
        )
        assertEquals(MemoryUfsHardwareBackend.Kind.MEMORY, parsed.kindOf("mtk-memnoc"))
        assertNull(parsed.kindOf("unknown"))
    }

    @Test
    fun `the synonyms of a kind are accepted and everything else is refused`() {
        assertEquals(MemoryUfsHardwareBackend.Kind.MEMORY, HardwareDeviceOverlay.kindOfToken("RAM"))
        assertEquals(MemoryUfsHardwareBackend.Kind.MEMORY, HardwareDeviceOverlay.kindOfToken("ddr"))
        assertEquals(MemoryUfsHardwareBackend.Kind.STORAGE, HardwareDeviceOverlay.kindOfToken("NVMe"))
        assertEquals(MemoryUfsHardwareBackend.Kind.STORAGE, HardwareDeviceOverlay.kindOfToken("emmc"))
        assertNull(HardwareDeviceOverlay.kindOfToken("gpu"))
        assertNull(HardwareDeviceOverlay.kindOfToken("cpu"))
        assertNull(HardwareDeviceOverlay.kindOfToken("thermal"))
    }

    @Test
    fun `a bad line is refused with its own code and never drops the rest of the file`() {
        val parsed = HardwareDeviceOverlay.parse(
            """
            one-token-only
            /sys/class/devfreq/foo memory
            good-one memory
            bad-kind cpu
            mali storage
            good-two storage
            """.trimIndent(),
        )

        assertEquals(
            listOf("good-one", "good-two"),
            parsed.entries.map { it.device },
        )
        assertEquals(
            listOf(
                HardwareDeviceOverlay.REASON_SHAPE,
                HardwareDeviceOverlay.REASON_NAME_INVALID,
                HardwareDeviceOverlay.REASON_UNKNOWN_KIND,
                HardwareDeviceOverlay.REASON_GRAPHICS_OWNED,
            ),
            parsed.rejections.map { it.reason },
        )
        assertEquals(listOf(1, 2, 4, 5), parsed.rejections.map { it.line })
    }

    @Test
    fun `a name is a single path segment and nothing else`() {
        assertTrue(HardwareDeviceOverlay.isCanonicalName("dvfsrc"))
        assertTrue(HardwareDeviceOverlay.isCanonicalName("112b0000.ufshci"))
        assertFalse(HardwareDeviceOverlay.isCanonicalName("sys/class/devfreq/x"))
        assertFalse(HardwareDeviceOverlay.isCanonicalName("*.ufs"))
        assertFalse(HardwareDeviceOverlay.isCanonicalName("-leading"))
        assertFalse(HardwareDeviceOverlay.isCanonicalName(""))
        assertFalse(HardwareDeviceOverlay.isCanonicalName("a".repeat(HardwareDeviceOverlay.MAX_NAME_LENGTH + 1)))
    }

    @Test
    fun `a device given two kinds keeps the first and reports the conflict`() {
        val parsed = HardwareDeviceOverlay.parse(
            """
            mtk-memnoc memory
            mtk-memnoc storage
            mtk-memnoc memory
            """.trimIndent(),
        )

        assertEquals(listOf(HardwareDeviceOverlay.REASON_CONFLICT), parsed.rejections.map { it.reason })
        // وتكرارٌ بنفس الصنف ليس خطأً: ملفّ يُنسخ بيد يتكرّر، والرفض هنا كان سيعاقب النسخ.
        assertEquals(1, parsed.entries.size)
        assertEquals(MemoryUfsHardwareBackend.Kind.MEMORY, parsed.kindOf("mtk-memnoc"))
    }

    @Test
    fun `the file has a ceiling and past it the line is refused, not trimmed`() {
        val lines = (1..HardwareDeviceOverlay.MAX_ENTRIES + 3).joinToString("\n") { "node$it memory" }

        val parsed = HardwareDeviceOverlay.parse(lines)

        assertEquals(HardwareDeviceOverlay.MAX_ENTRIES, parsed.entries.size)
        assertEquals(
            listOf(
                HardwareDeviceOverlay.REASON_LIMIT,
                HardwareDeviceOverlay.REASON_LIMIT,
                HardwareDeviceOverlay.REASON_LIMIT,
            ),
            parsed.rejections.map { it.reason },
        )
    }

    @Test
    fun `render and parse are the same text both ways`() {
        val entries = listOf(
            HardwareDeviceOverlay.Entry("b-node", MemoryUfsHardwareBackend.Kind.STORAGE),
            HardwareDeviceOverlay.Entry("a-node", MemoryUfsHardwareBackend.Kind.MEMORY),
        )

        val rendered = HardwareDeviceOverlay.render(entries)
        val parsed = HardwareDeviceOverlay.parse(rendered)

        // والترتيب ليس جزءًا من المعنى: المقارنة على المجموعة، والترتيب يُقاس في السطر التالي.
        assertEquals(entries.toSet(), parsed.entries.toSet())
        assertEquals(emptyList<HardwareDeviceOverlay.Rejection>(), parsed.rejections)
        // والترتيب مُقنَّن: ملفّ التصدير نفسه لا يختلف بحسب ترتيب إضافات المستخدم.
        assertTrue(rendered.indexOf("a-node") < rendered.indexOf("b-node"))
        assertEquals(rendered, HardwareDeviceOverlay.render(parsed.entries))
    }

    @Test
    fun `with and without keep one line per device`() {
        val start = emptyList<HardwareDeviceOverlay.Entry>()
        val added = HardwareDeviceOverlay.with(start, HardwareDeviceOverlay.Entry("x1", MemoryUfsHardwareBackend.Kind.MEMORY))
        val replaced = HardwareDeviceOverlay.with(added, HardwareDeviceOverlay.Entry("x1", MemoryUfsHardwareBackend.Kind.STORAGE))

        assertEquals(1, replaced.size)
        assertEquals(MemoryUfsHardwareBackend.Kind.STORAGE, replaced.single().kind)
        assertTrue(HardwareDeviceOverlay.without(replaced, "x1").isEmpty())
        // ونسيان جهاز لم يُعلَّم ليس خطأً.
        assertEquals(replaced, HardwareDeviceOverlay.without(replaced, "never-taught"))
    }

    @Test
    fun `an entry cannot be built for a graphics name or a foreign kind`() {
        val canonical = runCatching { HardwareDeviceOverlay.Entry("gpu0", MemoryUfsHardwareBackend.Kind.MEMORY) }
        assertTrue("الاسم ليس في محظور البناء: المنع يكون عند القراءة والفحص", canonical.isSuccess)

        assertTrue(runCatching { HardwareDeviceOverlay.Entry("a/b", MemoryUfsHardwareBackend.Kind.MEMORY) }.isFailure)
    }

    @Test
    fun `the store writes what it can read back`() {
        val store = HardwareOverlayStore.inDirectory(tempDir())

        assertTrue(store.save(listOf(HardwareDeviceOverlay.Entry("mtk-memnoc", MemoryUfsHardwareBackend.Kind.MEMORY))))

        val reloaded = store.load()
        assertEquals(listOf("mtk-memnoc"), reloaded.entries.map { it.device })
        assertTrue(store.teach(HardwareDeviceOverlay.Entry("ufs-x", MemoryUfsHardwareBackend.Kind.STORAGE)))
        assertEquals(listOf("mtk-memnoc", "ufs-x"), store.load().entries.map { it.device })
        assertTrue(store.forget("mtk-memnoc"))
        assertEquals(listOf("ufs-x"), store.load().entries.map { it.device })
    }

    @Test
    fun `a missing file is an empty overlay, not an error`() {
        val parsed = HardwareOverlayStore.inDirectory(tempDir()).load()

        assertEquals(emptyList<HardwareDeviceOverlay.Rejection>(), parsed.rejections)
        assertFalse(parsed.usable)
    }

    @Test
    fun `an oversized file is refused with its reason instead of being parsed`() {
        val directory = tempDir()
        val file = File(directory, HardwareOverlayStore.FILE_NAME)
        file.writeText("x".repeat((HardwareOverlayStore.MAX_BYTES + 1).toInt()))

        val parsed = HardwareOverlayStore.inDirectory(directory).load()

        assertEquals(listOf(HardwareDeviceOverlay.REASON_TOO_LARGE), parsed.rejections.map { it.reason })
        assertEquals(0, parsed.rejections.single().line)
        assertTrue(parsed.entries.isEmpty())
    }

    @Test
    fun `a write that cannot be read back is reported as not written`() {
        val directory = tempDir()
        // مسار الملفّ مشغول بمجلّد غير فارغ: التبديل لا يمرّ، ولا يجوز أن نُعلن كتابةً لم تقع.
        val blocker = File(directory, HardwareOverlayStore.FILE_NAME)
        blocker.mkdirs()
        File(blocker, "occupied").writeText("x")

        val store = HardwareOverlayStore.inDirectory(directory)

        assertFalse(store.save(listOf(HardwareDeviceOverlay.Entry("mtk-memnoc", MemoryUfsHardwareBackend.Kind.MEMORY))))
        assertTrue(store.load().entries.isEmpty())
    }

    private fun tempDir(): File = Files.createTempDirectory("overlay-test").toFile().also { temps += it }
}
