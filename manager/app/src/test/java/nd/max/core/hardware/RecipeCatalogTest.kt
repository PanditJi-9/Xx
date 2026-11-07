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
 * تحويل الكتالوج إلى مدخلات المحرّك — يُقاس هنا لأن خطأه **صامت**: مرشّح واحد غير حرفيّ يمرّ إلى
 * `RecipeRun` يعني كتابةً إلى مجلّد أو إلى قالب لم يُحلّ، ولا يفشل شيء في الترجمة.
 */
class RecipeCatalogTest {

    private val json = """
        {
          "version": 1,
          "entries": [
            {
              "key": "vm.swappiness",
              "transport": "NODE",
              "kind": "INT",
              "danger": "LOW",
              "candidates": [
                { "path": "/proc/sys/vm/swappiness", "origin": "MEASURED" },
                { "path": "/proc/sys/vm/${'$'}knob", "origin": "TEMPLATE" }
              ],
              "privilege": { "read": ["NONE"], "write": ["MAGISK", "DAEMON"] }
            },
            {
              "key": "cpu_limits:{policy}",
              "transport": "NODE",
              "kind": "FREQUENCY_RANGE",
              "danger": "MEDIUM",
              "candidates": [
                { "path": "/sys/devices/system/cpu/cpu0/cpufreq", "origin": "MEASURED" }
              ],
              "privilege": { "read": ["NONE"], "write": ["MAGISK"] }
            },
            {
              "key": "thermal_message.sconfig",
              "transport": "NODE",
              "kind": "INT",
              "danger": "HIGH",
              "candidates": [
                { "path": "/sys/devices/virtual/thermal/thermal_message/sconfig", "origin": "MEASURED" }
              ],
              "privilege": { "read": ["NONE"], "write": [] }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun entries_keep_the_catalog_order_and_copy_the_declared_kind() {
        val entries = RecipeCatalog.fromJson(json)

        assertEquals(listOf("vm.swappiness", "cpu_limits:{policy}", "thermal_message.sconfig"), entries.map { it.key })
        assertEquals("INT", entries.first().kind)
        assertEquals(listOf("MAGISK", "DAEMON"), entries.first().writePrivilege)
    }

    @Test
    fun non_literal_candidates_are_dropped_not_carried_into_a_plan() {
        val swappiness = RecipeCatalog.fromJson(json).first()

        // `$knob` قالب لا عقدة: يُسقَط ويبقى المسار الحرفيّ وحده.
        assertEquals(listOf("/proc/sys/vm/swappiness"), swappiness.candidates)
    }

    @Test
    fun a_literal_key_on_a_real_node_becomes_ready() {
        val plan = RecipePlan.plan("vm.swappiness=60", RecipeCatalog.fromJson(json))

        assertTrue(plan.applicable)
        assertEquals("/proc/sys/vm/swappiness", plan.ready.single().path)
    }

    @Test
    fun the_templated_family_is_refused_rather_than_pointed_at_a_guessed_node() {
        val plan = RecipePlan.plan("cpu_limits:{policy}=1800000", RecipeCatalog.fromJson(json))

        assertEquals(RecipePlan.Verdict.TEMPLATED, plan.steps.single().verdict)
        assertFalse(plan.applicable)
    }

    @Test
    fun an_entry_without_write_privilege_stays_refused_even_with_a_real_path() {
        val plan = RecipePlan.plan("thermal_message.sconfig=5", RecipeCatalog.fromJson(json))

        assertEquals(RecipePlan.Verdict.NOT_WRITABLE, plan.steps.single().verdict)
    }

    @Test
    fun broken_json_yields_an_empty_catalog_instead_of_throwing() {
        assertTrue(RecipeCatalog.fromJson("{ not json").isEmpty())
        assertTrue(RecipeCatalog.fromJson("").isEmpty())
    }
}
