/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * يقيس الحالات الثلاث على شجرة مؤقّتة — لا على جهاز المصدر.
 *
 * **ولماذا شجرة:** `node.path` مطلق، ولو قرأنا `/` في الاختبار لصار الحكم «حسب نواة المصدر»، وهو
 * أسوأ من لا اختبار. الجذر معامل، فالقياس حتميّ.
 */
class NodeCatalogReaderTest {

    private fun tempRoot(): File = Files.createTempDirectory("node-catalog").toFile()

    private fun node(id: String, path: String) = NodeCatalog.Node(id, path, NodeCatalog.Scope.CPU)

    private fun write(root: File, relative: String, text: String): File {
        val file = File(root, relative)
        file.parentFile?.mkdirs()
        file.writeText(text)
        return file
    }

    @Test
    fun a_readable_node_is_present_with_its_own_text() {
        val root = tempRoot()
        write(root, "sys/class/thermal/thermal_zone0/temp", "  42000 \n")

        val readings = NodeCatalogReader.read(root, listOf(node("thermal", "/sys/class/thermal/thermal_zone0/temp")))

        assertEquals(NodeCatalogReader.State.PRESENT, readings.single().state)
        assertEquals("42000", readings.single().value)
    }

    @Test
    fun a_missing_node_is_absent_and_carries_no_value() {
        val root = tempRoot()

        val reading = NodeCatalogReader.read(root, listOf(node("x", "/sys/does/not/exist"))).single()

        assertEquals(NodeCatalogReader.State.ABSENT, reading.state)
        assertNull(reading.value)
    }

    @Test
    fun a_directory_in_place_of_a_node_is_unreadable_not_present() {
        // مقبض أو مجلّد ليس عقدة: لا نقرؤه ولا نعدّه موجودًا للكتابة.
        val root = tempRoot()
        File(root, "sys/module/ged/parameters/gpu_dvfs_enable").mkdirs()

        val reading = NodeCatalogReader.read(root, listOf(node("ged", "/sys/module/ged/parameters/gpu_dvfs_enable"))).single()

        assertEquals(NodeCatalogReader.State.UNREADABLE, reading.state)
        assertNull(reading.value)
    }

    @Test
    fun an_empty_node_is_unreadable_rather_than_zero() {
        val root = tempRoot()
        write(root, "sys/block/zram0/mem_used", "   \n")

        val reading = NodeCatalogReader.read(root, listOf(node("zram", "/sys/block/zram0/mem_used"))).single()

        assertEquals(NodeCatalogReader.State.UNREADABLE, reading.state)
        assertNull(reading.value)
    }

    @Test
    fun the_summary_counts_each_state_once() {
        val root = tempRoot()
        write(root, "a", "1")
        write(root, "b", "")
        File(root, "c").mkdirs()
        val nodes = listOf(node("a", "/a"), node("b", "/b"), node("c", "/c"), node("d", "/d"))

        val summary = NodeCatalogReader.summarise(NodeCatalogReader.read(root, nodes))

        assertEquals(4, summary.total)
        assertEquals(1, summary.present)
        assertEquals(1, summary.absent)
        assertEquals(2, summary.unreadable)
        assertEquals(1, summary.usable)
    }

    @Test
    fun the_catalog_itself_is_well_formed() {
        val ids = NodeCatalog.nodes.map { it.id }
        assertEquals("المعرّفات تُخزَّن في السجلات فيجب أن تكون فريدة", ids.size, ids.toSet().size)
        assertTrue(NodeCatalog.nodes.all { it.path.startsWith("/") })
        assertEquals("كل نطاق يجب أن يُعرض", NodeCatalog.Scope.entries.toSet(), NodeCatalog.nodes.map { it.scope }.toSet())
        assertEquals(NodeCatalog.nodes.size, NodeCatalog.byScope().sumOf { it.second.size })
    }

    @Test
    fun an_empty_root_reports_every_catalog_node_as_absent() {
        val summary = NodeCatalogReader.summarise(NodeCatalogReader.read(tempRoot()))

        assertEquals(NodeCatalog.nodes.size, summary.total)
        assertEquals(NodeCatalog.nodes.size, summary.absent)
        assertEquals(0, summary.present)
    }
}
