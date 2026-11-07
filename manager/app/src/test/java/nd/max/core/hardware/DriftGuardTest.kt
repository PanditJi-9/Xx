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
 * حارس الانحراف يُقاس بكتّابه وقرّائه المُستعاضين لا بجهاز: العقود الثلاثة التي يخفيها
 * التعميم هي التي تُكسر صامتة — ألّا يكتب شيئًا وهو مطابق، وأن يُصلح حين ينحرف، وأن يُسمّي
 * المفتاح حين يفشل الإصلاح (‏`VerificationResult` لا يحمل المفتاح، فالخطأ وحده هو الدليل).
 */
class DriftGuardTest {

    @Test
    fun a_matching_value_is_neither_written_nor_reported() {
        var writes = 0
        val guard = DriftGuard<Int>()
        guard.own("cpu.max", desired = 7, writer = { writes++; true }, reader = { 7 })

        assertTrue(guard.checkAndRepair().isEmpty())
        assertEquals(0, writes)
    }

    @Test
    fun a_drifted_value_is_written_back_and_reported() {
        var live = 0
        val guard = DriftGuard<Int>()
        guard.own("cpu.max", desired = 7, writer = { value -> live = value; true }, reader = { live })

        val results = guard.checkAndRepair()

        assertEquals(1, results.size)
        assertTrue(results.single().successful)
        assertEquals(7, live)
    }

    @Test
    fun a_failed_repair_names_the_key_it_could_not_repair() {
        val guard = DriftGuard<String>()
        guard.own("gpu.governor", desired = "performance", writer = { false }, reader = { "simple" })

        val result = guard.checkAndRepair().single()

        assertFalse(result.successful)
        assertTrue(
            "الخطأ لا يحمل المفتاح: ${result.error}",
            result.error.orEmpty().startsWith("gpu.governor: "),
        )
    }

    @Test
    fun an_unreadable_node_still_attempts_one_repair_and_reports_the_failure() {
        val guard = DriftGuard<String>()
        guard.own("thermal.sconfig", desired = "0", writer = { true }, reader = { null })

        val result = guard.checkAndRepair().single()

        // قراءة `null` ليست «مطابقًا»: تُحاول الكتابة، فإن بقيت غير مقروءة فالحكم فشل لا سكوت.
        assertFalse(result.successful)
        assertTrue(result.error.orEmpty().startsWith("thermal.sconfig: "))
    }

    @Test
    fun releasing_a_key_stops_it_being_checked() {
        var writes = 0
        val guard = DriftGuard<Int>()
        guard.own("cpu.max", desired = 7, writer = { writes++; true }, reader = { 0 })
        guard.release("cpu.max")

        assertTrue(guard.checkAndRepair().isEmpty())
        assertEquals(0, writes)
    }

    @Test
    fun clearing_drops_every_owned_key() {
        val guard = DriftGuard<Int>()
        guard.own("a", desired = 1, writer = { true }, reader = { 0 })
        guard.own("b", desired = 2, writer = { true }, reader = { 0 })

        guard.clear()

        assertTrue(guard.checkAndRepair().isEmpty())
    }
}
