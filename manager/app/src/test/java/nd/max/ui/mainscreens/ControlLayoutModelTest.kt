/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.mainscreens

import nd.max.ui.navigation.MaxDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد ربط الوضعين: الصفحة المختصرة والصفحة الموسّعة تُبنيان من نموذج واحد
 * ([controlLayoutModel]) مشتقّ من السجل [MaxDestination]، فلا شيء يُكتب مرتين.
 *
 * هذه الاختبارات هي ما يمنع الانحدار: أي وجهة جديدة تُضاف تحت Control ويُنسى
 * إظهارها في النموذج تُسقط الاختبار، وأي ازدواج في الصفوف كذلك.
 */
class ControlLayoutModelTest {

    private val bands = controlLayoutModel()

    private val hubs = bands.flatMap { band -> band.hubs }

    @Test
    fun `every destination parented by Control is reachable from the page`() {
        // والقياس على ما يراه المستخدم: `hidden` يخرج من الصفوف بقرار المالك (ADR-18)
        // ولا يجوز أن يُطالب النموذج بعرضه.
        val fromRegistry = MaxDestination.Visible.filter { it.parent == MaxDestination.Control }.toSet()
        val hubsInModel = hubs.map { it.hub }.toSet()
        // شريط واحد للأدوات بعد M1c: «فحص الجهاز» أُفرغ ونُقلت أدواته إلى شاشة التشخيص،
        // فالمجموع = المحاور + صفوف الصندوق، ويغطّي كل وجهة أبوّتها `Control` بلا تكرار.
        val toolsInModel = controlToolEntries().map { it.destination }.toSet()

        assertEquals(fromRegistry, hubsInModel + toolsInModel)
        assertEquals(hubsInModel.size + toolsInModel.size, (hubsInModel + toolsInModel).size)
    }

    @Test
    fun `the tool box is split into named groups without losing a row`() {
        // العقد: النطاقات **تقسّم** صفوف الصندوق، لا تنسخها ولا تُسقط منها. فلو أُضيفت أداة
        // إلى `ControlToolDestinations` ونُسيت من نطاق، سقط هذا الاختبار بدل أن تختفي بصمت.
        val entries = controlToolEntries()
        val groups = controlToolGroups()
        val listed = groups.flatMap { group -> group.entries }

        assertEquals(entries.size, listed.size)
        assertEquals(
            entries.map { it.destination }.toSet(),
            listed.map { it.destination }.toSet(),
        )
        assertEquals(
            "a tool is listed in more than one group: $listed",
            listed.size,
            listed.map { it.destination }.toSet().size,
        )
        assertTrue("a group renders empty", groups.all { it.entries.isNotEmpty() })
        assertTrue("the box must read as more than one band", groups.size >= 2)
    }

    @Test
    fun `each hub lists every screen the registry gives it`() {
        hubs.forEach { spec ->
            // `Visible` لا `All`: وجهةٌ أخفاها المالك (`hidden`) تبقى في السجلّ ومسارها صالح،
            // لكنها ليست صفًّا في الصفحة — ومطالبة النموذج بها تُسقط الاختبار على قرارٍ مقصود.
            val owned = MaxDestination.Visible.filter { it.parent == spec.hub }.map { it.route }
            val listed = spec.features.map { it.destination.route }
            assertTrue(
                "${spec.hub.route} is missing ${owned - listed.toSet()}",
                listed.containsAll(owned),
            )
        }
    }

    @Test
    fun `no screen is listed twice within the same hub`() {
        hubs.forEach { spec ->
            val listed = spec.features.map { it.destination.route }
            assertEquals(
                "${spec.hub.route} lists a screen more than once: $listed",
                listed.distinct(),
                listed,
            )
        }
    }

    @Test
    fun `bands and hubs are listed exactly once`() {
        assertEquals(bands.map { it.key }.distinct().size, bands.size)
        assertEquals(hubs.map { it.hub }.distinct().size, hubs.size)
        assertTrue("empty bands must be dropped", bands.all { it.hubs.isNotEmpty() })
    }

    @Test
    fun `memory hub also surfaces the shared preference editor`() {
        val memory = hubs.first { it.hub == MaxDestination.MemoryHub }
        assertTrue(memory.features.map { it.destination }.contains(MaxDestination.PreferenceTweaks))
    }

    @Test
    fun `every row carries a sub-title resource`() {
        hubs.forEach { spec ->
            assertTrue(spec.hubSubtitleRes != 0)
            spec.features.forEach { feature -> assertTrue(feature.subtitleRes != 0) }
        }
        controlToolEntries().forEach { tool -> assertTrue(tool.subtitleRes != 0) }
    }
}
