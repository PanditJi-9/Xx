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
 * المنفّذ يُقاس بواجهة مزيفة: لا ملفّ حقيقي ولا جذر.
 *
 * **ولماذا هذا هو الاختبار الصحيح:** الشرط الذي يُثبت هنا ليس «هل تقبل النواة الكتابة؟» — ذاك
 * يحتاج جهازًا — بل «هل نتراجع عن كتابة لم تُقرأ كما أردنا، وهل نتوقّف عند أول فشل، وهل نُعلن
 * فشل الاسترجاع بدل ادّعاء النجاح؟».
 */
class RecipeRunTest {

    private class FakeIo(vararg initial: Pair<String, String>) : RecipeRun.NodeIo {
        val values = LinkedHashMap<String, String>().apply { putAll(initial.toMap()) }
        var writes = 0
        val refuseWrite = mutableSetOf<String>()
        var ignoreWrites = false

        override fun read(path: String): String? = values[path]

        override fun write(path: String, value: String): Boolean {
            writes++
            if (path in refuseWrite) return false
            if (!ignoreWrites) values[path] = value
            return true
        }
    }

    private fun ready(key: String, value: String, path: String) =
        RecipePlan.Step(key, value, path, RecipePlan.Verdict.READY)

    private fun planOf(vararg steps: RecipePlan.Step) = RecipePlan.Plan(steps.toList())

    @Test
    fun every_ready_step_is_written_and_read_back() {
        val io = FakeIo("/cpu/min" to "100", "/cpu/max" to "200")
        val plan = planOf(ready("cpu_min", "300", "/cpu/min"), ready("cpu_max", "400", "/cpu/max"))

        val report = RecipeRun.run(plan, io)

        assertTrue(report.ok)
        assertEquals(2, report.applied)
        assertEquals("300", io.values["/cpu/min"])
        assertEquals("400", io.values["/cpu/max"])
    }

    @Test
    fun a_write_that_does_not_read_back_is_restored_to_the_previous_value() {
        val io = FakeIo("/cpu/min" to "100").apply { ignoreWrites = true }
        val plan = planOf(ready("cpu_min", "300", "/cpu/min"))

        val report = RecipeRun.run(plan, io)

        assertFalse(report.ok)
        assertEquals(RecipeRun.Outcome.MISMATCH_RESTORED, report.results.single().outcome)
        assertEquals("100", report.results.single().before)
        // الكتابة الثانية هي محاولة الاسترجاع — لا قيمة جديدة تُدَّعى.
        assertEquals(2, io.writes)
    }

    @Test
    fun an_unreadable_previous_value_is_not_pretended_to_be_restored() {
        val io = FakeIo().apply { ignoreWrites = true }
        val plan = planOf(ready("cpu_min", "300", "/cpu/min"))

        val report = RecipeRun.run(plan, io)

        assertEquals(RecipeRun.Outcome.RESTORE_FAILED, report.results.single().outcome)
        assertEquals(1, report.failed)
    }

    @Test
    fun a_refused_write_stops_the_recipe_and_rolls_back_what_was_applied() {
        val io = FakeIo("/cpu/min" to "100", "/cpu/max" to "200").apply { refuseWrite += "/cpu/max" }
        val plan = planOf(ready("cpu_min", "300", "/cpu/min"), ready("cpu_max", "400", "/cpu/max"))

        val report = RecipeRun.run(plan, io)

        assertFalse(report.ok)
        assertEquals(
            listOf(
                RecipeRun.Outcome.APPLIED,
                RecipeRun.Outcome.WRITE_FAILED,
                RecipeRun.Outcome.ROLLED_BACK,
            ),
            report.results.map { it.outcome },
        )
        // الخطوة الأولى عادت إلى 100، والثانية لم تُكتب أصلًا.
        assertEquals("100", io.values["/cpu/min"])
        assertEquals("200", io.values["/cpu/max"])
    }

    @Test
    fun a_rollback_that_the_io_refuses_is_reported_as_failed_not_hidden() {
        val io = FakeIo("/cpu/min" to "100", "/cpu/max" to "200").apply {
            refuseWrite += setOf("/cpu/max", "/cpu/min")
        }
        val plan = planOf(ready("cpu_min", "300", "/cpu/min"), ready("cpu_max", "400", "/cpu/max"))

        val report = RecipeRun.run(plan, io)

        assertFalse(report.ok)
        // فشل كتابة + فشل استرجاع: كلاهما يُعدّ فشلًا معلنًا، ولا أحدهما يُخفي الآخر.
        assertEquals(2, report.failed)
        assertTrue(report.results.any { it.outcome == RecipeRun.Outcome.WRITE_FAILED })
        assertTrue(report.results.any { it.outcome == RecipeRun.Outcome.ROLLBACK_FAILED })
    }

    @Test
    fun a_plan_with_a_refused_step_touches_nothing_at_all() {
        val io = FakeIo("/cpu/min" to "100")
        val plan = RecipePlan.Plan(
            listOf(
                ready("cpu_min", "300", "/cpu/min"),
                RecipePlan.Step("cpu_max", "400", "/cpu/max", RecipePlan.Verdict.NO_NODE),
            )
        )

        val report = RecipeRun.run(plan, io)

        assertFalse(report.ok)
        assertEquals(0, io.writes)
        assertEquals(listOf(RecipeRun.Outcome.BLOCKED, RecipeRun.Outcome.BLOCKED), report.results.map { it.outcome })
    }

    @Test
    fun an_empty_plan_is_not_a_success() {
        val report = RecipeRun.run(RecipePlan.Plan(emptyList<RecipePlan.Step>()), FakeIo())

        assertFalse(report.ok)
        assertTrue(report.results.isEmpty())
    }
}
