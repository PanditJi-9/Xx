/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد «عدم الإزعاج» بلا جهاز: الخرائط نقيّة، والكتابة تُحكم بقراءة، والمسارات الغائبة تُعلَن.
 *
 * والسبب الذي يجعل هذا الاختبار ضروريًّا: العطب الأصلي كان **نجاحًا بلا قراءة** — رمز خروج صفر
 * من `settings put global zen_mode` بينما `ZenModeHelper` يردّ القيمة. فأي إصلاح لا يُسقط هذا
 * الاختبار لو عاد إلى «أعلن النجاح بمجرّد التنفيذ» ليس إصلاحًا.
 */
class DndControlTest {

    @Test
    fun `every zen mode maps to the word the shell command accepts`() {
        assertEquals("all", DndControl.shellModeForZen(0))
        assertEquals("priority", DndControl.shellModeForZen(1))
        assertEquals("none", DndControl.shellModeForZen(2))
        assertEquals("alarms", DndControl.shellModeForZen(3))
        // قيمة غير معروفة لا تُترجم إلى نمط أقوى: `all` = متوقّف.
        assertEquals("all", DndControl.shellModeForZen(99))
    }

    @Test
    fun `the framework filter and the stored mode agree in both directions`() {
        for (zen in 0..3) {
            assertEquals(zen, DndControl.zenForFilter(DndControl.filterForZen(zen)))
        }
        // `UNKNOWN` ليس حالة؛ لا يُقرأ «متوقّفًا» ولا «مفعّلًا».
        assertEquals(null, DndControl.zenForFilter(0))
    }

    @Test
    fun `availability needs either a granted policy or the android eleven command`() {
        assertFalse("Android 10 بلا صلاحية سياسة: لا مسار أصلًا", DndControl.availableOn(29, policyAccess = false))
        assertTrue("صلاحية السياسة تكفي على أي إصدار", DndControl.availableOn(29, policyAccess = true))
        assertTrue("الأمر موجود من 30", DndControl.availableOn(30, policyAccess = false))
    }

    @Test
    fun `a shell write is applied only after the read back matches`() = runBlocking {
        var state = 0
        val commands = mutableListOf<String>()
        val verdict = DndControl.apply(
            zen = 1,
            policy = null,
            shell = { command ->
                commands += command
                state = 1
                DndControl.ShellVerdict.RAN
            },
            read = { state },
            sdkInt = 30,
            pause = {},
        )
        assertEquals(listOf("cmd notification set_dnd priority"), commands)
        assertEquals(DndControl.Verdict.APPLIED, verdict)
    }

    @Test
    fun `an accepted command that changes nothing is not success`() = runBlocking {
        val verdict = DndControl.apply(
            zen = 1,
            policy = null,
            shell = { DndControl.ShellVerdict.RAN },
            read = { 0 },
            sdkInt = 30,
            pause = {},
        )
        assertEquals(DndControl.Verdict.NOT_HONOURED, verdict)
    }

    @Test
    fun `a refused lane never claims the mode was set`() = runBlocking {
        var shellCalls = 0
        val verdict = DndControl.apply(
            zen = 1,
            policy = null,
            shell = {
                shellCalls++
                DndControl.ShellVerdict.REFUSED
            },
            read = { 0 },
            sdkInt = 30,
            pause = {},
        )
        assertEquals(1, shellCalls)
        assertEquals(DndControl.Verdict.REFUSED, verdict)
    }

    @Test
    fun `android ten without policy access is unsupported and does not run a shell command`() = runBlocking {
        var shellCalls = 0
        val verdict = DndControl.apply(
            zen = 1,
            policy = null,
            shell = {
                shellCalls++
                DndControl.ShellVerdict.RAN
            },
            read = { 0 },
            sdkInt = 29,
            pause = {},
        )
        assertEquals(0, shellCalls)
        assertEquals(DndControl.Verdict.UNSUPPORTED, verdict)
    }

    @Test
    fun `verification retries are bounded and stop at the first match`() = runBlocking {
        var state = 0
        var reads = 0
        val pauses = mutableListOf<Long>()
        val verdict = DndControl.apply(
            zen = 1,
            policy = null,
            shell = { DndControl.ShellVerdict.RAN },
            read = {
                reads++
                if (reads >= 3) state = 1
                state
            },
            sdkInt = 30,
            pause = { pauses += it },
        )
        assertEquals(DndControl.Verdict.APPLIED, verdict)
        assertEquals("قراءتان فاشلتان ثم ثالثة ناجحة = انتظاران", 2, pauses.size)
        assertEquals("لا انتظار بلا حدّ", 4, DndControl.VERIFY_DELAYS_MS.size)
    }

    @Test
    fun `an impossible mode is refused rather than written as something else`() = runBlocking {
        var shellCalls = 0
        val verdict = DndControl.apply(
            zen = 9,
            policy = null,
            shell = {
                shellCalls++
                DndControl.ShellVerdict.RAN
            },
            read = { 9 },
            sdkInt = 30,
            pause = {},
        )
        assertEquals(0, shellCalls)
        assertEquals(DndControl.Verdict.UNSUPPORTED, verdict)
    }
}
