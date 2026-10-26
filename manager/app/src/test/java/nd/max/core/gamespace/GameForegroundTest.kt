/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import java.io.File
import nd.max.core.privilege.ExecutionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * كشف اللعبة الأمامية بلا جذر: اختيار المصدر بحسب الوضع، واختزال أحداث الاستخدام إلى «من في المقدّمة».
 * وأهمّ سطر: وضع Shizuku لا يقرأ بالصدفة الجذرية وإن كان الجذر ممنوحًا — فيعتمد على أحداث الاستخدام.
 */
class GameForegroundTest {

    private fun choose(mode: ExecutionMode, root: Boolean, usage: Boolean) =
        ForegroundSourceRules.choose(mode, root, usage)

    @Test
    fun `root auto and root mode keep reading the foreground through the shell`() {
        assertEquals(ForegroundSource.SHELL, choose(ExecutionMode.AUTO, root = true, usage = false))
        assertEquals(ForegroundSource.SHELL, choose(ExecutionMode.AUTO, root = true, usage = true))
        assertEquals(ForegroundSource.SHELL, choose(ExecutionMode.ROOT, root = true, usage = false))
    }

    @Test
    fun `shizuku and basic modes never use the root shell even when root is granted`() {
        assertEquals(ForegroundSource.USAGE_STATS, choose(ExecutionMode.SHIZUKU, root = true, usage = true))
        assertEquals(ForegroundSource.NONE, choose(ExecutionMode.SHIZUKU, root = true, usage = false))
        assertEquals(ForegroundSource.USAGE_STATS, choose(ExecutionMode.BASIC, root = true, usage = true))
        assertEquals(ForegroundSource.NONE, choose(ExecutionMode.BASIC, root = true, usage = false))
    }

    @Test
    fun `without root the source is usage access or nothing`() {
        assertEquals(ForegroundSource.USAGE_STATS, choose(ExecutionMode.AUTO, root = false, usage = true))
        assertEquals(ForegroundSource.NONE, choose(ExecutionMode.AUTO, root = false, usage = false))
        assertEquals(ForegroundSource.USAGE_STATS, choose(ExecutionMode.ROOT, root = false, usage = true))
    }

    private fun ev(pkg: String, resumed: Boolean, at: Long) = ForegroundEvent(pkg, resumed, at)

    @Test
    fun `the most recent resume is the foreground app`() {
        val events = listOf(ev("a", true, 10), ev("a", false, 20), ev("b", true, 21))
        assertEquals("b", UsageForegroundRules.reduce("", events))
    }

    @Test
    fun `pausing the current app leaves no foreground until another resumes`() {
        assertEquals("", UsageForegroundRules.reduce("game", listOf(ev("game", false, 5))))
    }

    @Test
    fun `pausing some other app does not change the foreground`() {
        assertEquals("game", UsageForegroundRules.reduce("game", listOf(ev("other", false, 5))))
    }

    @Test
    fun `events are applied in time order whatever order they arrive in`() {
        val shuffled = listOf(ev("b", true, 21), ev("a", false, 20), ev("a", true, 10))
        assertEquals("b", UsageForegroundRules.reduce("", shuffled))
    }

    @Test
    fun `re-applying events from an overlapping window gives the same answer`() {
        val window = listOf(ev("a", false, 20), ev("b", true, 21))
        val once = UsageForegroundRules.reduce("a", window)
        assertEquals(once, UsageForegroundRules.reduce(once, window))
        val closed = listOf(ev("b", true, 30), ev("b", false, 31))
        val after = UsageForegroundRules.reduce(once, closed)
        assertEquals(after, UsageForegroundRules.reduce(after, closed))
    }

    /** الخدمة تقرأ المصدر من المدخل الواحد، ومراجعة نافذة التركيز تبقى لمسار الصدفة وحده. */
    @Test
    fun `the panel service reads the foreground through GameForeground`() {
        val root = listOf(
            File("src/main/java/nd/max"), File("app/src/main/java/nd/max"),
            File("../app/src/main/java/nd/max"), File("manager/app/src/main/java/nd/max")
        ).firstOrNull { it.isDirectory }
        assumeTrue("Cannot locate nd.max sources", root != null)
        val service = File(root, "service/GamePanelService.kt").readText()
        assertTrue(service.contains("GameForeground.source(applicationContext)"))
        assertTrue(service.contains("GameForeground.current(applicationContext, source)"))
        assertTrue(
            "the focus-window check reads through the shell, so it must stay behind the SHELL source",
            service.contains("source == ForegroundSource.SHELL && resumed != null")
        )
        assertTrue(!service.contains("runCatching { FpsMonitorUtil.getForegroundPackage() }"))
    }
}
