/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.privilege

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * فصل الأوضاع: كل وضع يحصر المسار ولا يتنازل صامتًا إلى غيره. والسطر الحاسم هنا أن Shizuku لا يرجع
 * إلى الجذر وإن كان الجذر ممنوحًا — فمن اختار Shizuku يريد ألّا يُستدعى `su`.
 */
class ExecutionModeRulesTest {
    private val root = PrivilegedShellRules.Lane.ROOT
    private val shizuku = PrivilegedShellRules.Lane.SHIZUKU

    @Test
    fun `auto keeps the previous behaviour`() {
        assertEquals(root, ExecutionModeRules.lane(ExecutionMode.AUTO, rootGranted = true, shizukuReady = true))
        assertEquals(shizuku, ExecutionModeRules.lane(ExecutionMode.AUTO, rootGranted = false, shizukuReady = true))
        assertNull(ExecutionModeRules.lane(ExecutionMode.AUTO, rootGranted = false, shizukuReady = false))
    }

    @Test
    fun `root mode never falls back to shizuku`() {
        assertEquals(root, ExecutionModeRules.lane(ExecutionMode.ROOT, rootGranted = true, shizukuReady = true))
        assertNull(ExecutionModeRules.lane(ExecutionMode.ROOT, rootGranted = false, shizukuReady = true))
    }

    @Test
    fun `shizuku mode never uses root even when root is granted`() {
        assertEquals(shizuku, ExecutionModeRules.lane(ExecutionMode.SHIZUKU, rootGranted = true, shizukuReady = true))
        assertNull(ExecutionModeRules.lane(ExecutionMode.SHIZUKU, rootGranted = true, shizukuReady = false))
    }

    @Test
    fun `basic mode refuses every privileged command`() {
        for (r in listOf(true, false)) for (s in listOf(true, false)) {
            assertNull(ExecutionModeRules.lane(ExecutionMode.BASIC, rootGranted = r, shizukuReady = s))
        }
    }

    @Test
    fun `the effective level is the lower of the chosen mode and what is granted`() {
        assertEquals(PrivilegeLevel.ROOT, ExecutionModeRules.level(ExecutionMode.AUTO, true, true))
        assertEquals(PrivilegeLevel.SHIZUKU, ExecutionModeRules.level(ExecutionMode.SHIZUKU, true, true))
        assertEquals(PrivilegeLevel.NONE, ExecutionModeRules.level(ExecutionMode.ROOT, false, true))
        assertEquals(PrivilegeLevel.NONE, ExecutionModeRules.level(ExecutionMode.BASIC, true, true))
    }

    @Test
    fun `each mode refuses with its own stable reason`() {
        val reasons = ExecutionMode.entries.map { ExecutionModeRules.refusalReason(it) }
        assertEquals("every mode needs a distinct reason", reasons.size, reasons.toSet().size)
        assertEquals(PrivilegedShell.REASON_NO_PRIVILEGE, ExecutionModeRules.refusalReason(ExecutionMode.AUTO))
    }

    @Test
    fun `only auto and root may warm the su shell at startup`() {
        assertTrue(ExecutionModeRules.mayUseRoot(ExecutionMode.AUTO))
        assertTrue(ExecutionModeRules.mayUseRoot(ExecutionMode.ROOT))
        assertFalse(ExecutionModeRules.mayUseRoot(ExecutionMode.SHIZUKU))
        assertFalse(ExecutionModeRules.mayUseRoot(ExecutionMode.BASIC))
    }

    @Test
    fun `ids round trip and an unknown id falls back to auto`() {
        for (mode in ExecutionMode.entries) assertEquals(mode, ExecutionMode.fromId(mode.id))
        assertEquals(ExecutionMode.AUTO, ExecutionMode.fromId(null))
        assertEquals(ExecutionMode.AUTO, ExecutionMode.fromId("something-else"))
        assertEquals("every id is unique", ExecutionMode.entries.size, ExecutionMode.entries.map { it.id }.toSet().size)
    }
}
