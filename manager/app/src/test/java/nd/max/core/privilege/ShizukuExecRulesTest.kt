/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.privilege

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حدود تنفيذ Shizuku — نقيّة، فتُقاس على JVM بلا جهاز.
 *
 * وكلُّ اختبار هنا يقابل عطبًا حقيقيًّا في النوع لا في الشكل: خَرْج مقتطع بلا علامة يوهم
 * بالكمال، وردّ ناقص يُقرأ صفرًا يوهم بالنجاح، وأمر أطول من الحدّ كان سيُدفع ثمنه في binder
 * قبل أن يُرفض. ولا ادّعاء أعلى من ذلك: السلوك على جهاز حقيقيّ (ربط الخدمة نفسها) يحتاج جهازًا.
 */
class ShizukuExecRulesTest {

    @Test
    fun `output within the limit passes through untouched`() {
        val raw = "package:nd.max\n"
        assertEquals(raw, ShizukuExecRules.capOutput(raw))
    }

    @Test
    fun `output exactly at the limit is not marked as truncated`() {
        val raw = "x".repeat(ShizukuExecRules.MAX_OUTPUT_CHARS)
        assertEquals(raw, ShizukuExecRules.capOutput(raw))
    }

    @Test
    fun `output beyond the limit is cut and marked once`() {
        val raw = "x".repeat(ShizukuExecRules.MAX_OUTPUT_CHARS + 10)
        val capped = ShizukuExecRules.capOutput(raw)
        assertTrue(
            "a truncated output must say so; silent truncation reads as a complete answer",
            capped.endsWith(ShizukuExecRules.TRUNCATION_MARK),
        )
        assertEquals(
            ShizukuExecRules.MAX_OUTPUT_CHARS,
            capped.removeSuffix(ShizukuExecRules.TRUNCATION_MARK).length,
        )
    }

    @Test
    fun `capping already-capped text keeps exactly one mark`() {
        val once = ShizukuExecRules.capOutput("y".repeat(ShizukuExecRules.MAX_OUTPUT_CHARS + 1))
        assertEquals(once, ShizukuExecRules.capOutput(once))
    }

    @Test
    fun `a two element reply becomes a declared verdict with its exit code`() {
        assertEquals(ShizukuExec.Ran(0, "ok"), ShizukuExecRules.parseReply(arrayOf("0", "ok")))
        assertEquals(ShizukuExec.Ran(127, "not found"), ShizukuExecRules.parseReply(arrayOf("127", "not found")))
    }

    @Test
    fun `a null or short or non numeric reply is a failure, never an invented success`() {
        assertTrue(ShizukuExecRules.parseReply(null) is ShizukuExec.Failed)
        assertTrue(ShizukuExecRules.parseReply(arrayOf()) is ShizukuExec.Failed)
        assertTrue(ShizukuExecRules.parseReply(arrayOf("not-a-code", "out")) is ShizukuExec.Failed)
    }

    @Test
    fun `a missing output is empty text, not a missing verdict`() {
        assertEquals(ShizukuExec.Ran(0, ""), ShizukuExecRules.parseReply(arrayOf("0")))
    }

    @Test
    fun `blank and over-long commands are refused before the wire`() {
        assertEquals("empty-command", ShizukuExecRules.rejectionFor("   "))
        assertEquals(
            "command-too-long",
            ShizukuExecRules.rejectionFor("x".repeat(ShizukuExecRules.MAX_COMMAND_CHARS + 1)),
        )
        assertNull(ShizukuExecRules.rejectionFor("pm list packages"))
        assertNull(ShizukuExecRules.rejectionFor("x".repeat(ShizukuExecRules.MAX_COMMAND_CHARS)))
    }
}
