/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * نواة الوصفات تُقاس هنا لأنها **حكم قبل الكتابة**: خطأها لا يُظهر استثناءً، بل يكتب قيمة خاطئة
 * إلى عقدة نواة. فكل رفض له اختبار، وكل قبول له سبب.
 */
class RecipePlanTest {

    private fun entry(
        key: String,
        kind: String = "INT",
        candidates: List<String> = listOf("/sys/x"),
        write: List<String> = listOf("MAGISK"),
        danger: String = "MEDIUM",
    ) = RecipePlan.Entry(key, "NODE", candidates, write, danger, kind)

    private val catalog = listOf(
        entry("cpu.min", kind = "INT"),
        entry("cpu.max", kind = "INT"),
        entry("gpu.adreno", kind = "ENUM"),
        entry("display.hbm", kind = "BOOL"),
        entry("battery.limit", kind = "PERCENT"),
        entry("thermal.profile", kind = "STRING"),
        entry("readonly.key", write = emptyList()),
        entry("protected.key", danger = "PROTECTED"),
        entry("nopath.key", candidates = emptyList()),
    )

    @Test
    fun a_fully_valid_recipe_is_applicable() {
        val plan = RecipePlan.plan("cpu.min=300000\ncpu.max=1800000", catalog)

        assertTrue(plan.applicable)
        assertEquals(2, plan.ready.size)
        assertEquals("/sys/x", plan.ready.first().path)
    }

    @Test
    fun comments_and_blank_lines_are_not_steps() {
        val parsed = RecipePlan.parse("# comment\n\n  \ncpu.min=300000\nbroken line\n=5\nk=\n")

        assertEquals(listOf("cpu.min" to "300000"), parsed)
    }

    @Test
    fun an_unknown_key_is_reported_not_guessed() {
        val plan = RecipePlan.plan("cpu.unknown=1", catalog)

        assertEquals(RecipePlan.Verdict.UNKNOWN_KEY, plan.steps.single().verdict)
        assertFalse(plan.applicable)
    }

    @Test
    fun a_protected_entry_is_never_planned() {
        assertEquals(
            RecipePlan.Verdict.PROTECTED,
            RecipePlan.plan("protected.key=1", catalog).steps.single().verdict,
        )
    }

    @Test
    fun an_entry_without_write_privilege_is_refused() {
        assertEquals(
            RecipePlan.Verdict.NOT_WRITABLE,
            RecipePlan.plan("readonly.key=1", catalog).steps.single().verdict,
        )
    }

    @Test
    fun an_entry_without_candidates_is_refused() {
        assertEquals(
            RecipePlan.Verdict.NO_NODE,
            RecipePlan.plan("nopath.key=1", catalog).steps.single().verdict,
        )
    }

    @Test
    fun a_value_outside_its_kind_is_refused_before_any_write() {
        assertEquals(RecipePlan.Verdict.BAD_VALUE, RecipePlan.plan("display.hbm=2", catalog).steps.single().verdict)
        assertEquals(RecipePlan.Verdict.BAD_VALUE, RecipePlan.plan("battery.limit=101", catalog).steps.single().verdict)
        assertEquals(RecipePlan.Verdict.BAD_VALUE, RecipePlan.plan("cpu.min=abc", catalog).steps.single().verdict)
        assertEquals(RecipePlan.Verdict.READY, RecipePlan.plan("display.hbm=true", catalog).steps.single().verdict)
        assertEquals(RecipePlan.Verdict.READY, RecipePlan.plan("battery.limit=80", catalog).steps.single().verdict)
    }

    @Test
    fun one_bad_step_blocks_the_whole_recipe() {
        // نصف وصفة قد يترك الجهاز في حالة لم تُقصَد؛ فالرفض كامل لا جزئي.
        val plan = RecipePlan.plan("cpu.min=300000\ncpu.unknown=1", catalog)

        assertFalse(plan.applicable)
        assertEquals(1, plan.ready.size)
        assertEquals(1, plan.blocked.size)
    }

    @Test
    fun an_empty_recipe_is_not_applicable() {
        assertFalse(RecipePlan.plan("", catalog).applicable)
        assertFalse(RecipePlan.plan("# only a comment", catalog).applicable)
    }

    @Test
    fun the_last_wins_nothing_duplicate_keys_stay_separate_steps() {
        // التكرار لا يُدمج بصمت: يظهر خطوتين ليقرّر المستخدم، ولا تُطبَّق إحداهما بدل الأخرى.
        val plan = RecipePlan.plan("cpu.min=1\ncpu.min=2", catalog)

        assertEquals(2, plan.steps.size)
        assertEquals(listOf("1", "2"), plan.steps.map { it.value })
    }
}
