/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import nd.max.core.gamespace.GameCapabilities.Availability
import nd.max.core.gamespace.GameCapabilities.Cause
import nd.max.core.privilege.ExecutionMode
import nd.max.core.privilege.PrivilegeLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** إتاحة ميزات اللعب بحسب الوضع والامتياز: ما يُقال للمستخدم «بدّل الوضع» أو «امنح الامتياز» يُقاس هنا. */
class GameCapabilitiesTest {

    private fun av(f: GameFeature, m: ExecutionMode, root: Boolean, shizuku: Boolean) =
        GameCapabilities.availability(f, m, root, shizuku)

    @Test
    fun `root auto unlocks every feature`() {
        assertTrue(GameCapabilities.all(ExecutionMode.AUTO, true, true).values.all { it == Availability.Available })
    }

    @Test
    fun `shizuku mode keeps the lane-routed features and drops the root-only ones`() {
        val all = GameCapabilities.all(ExecutionMode.SHIZUKU, rootGranted = false, shizukuReady = true)
        for (feature in listOf(GameFeature.SCREENSHOT, GameFeature.SCREEN_RECORD, GameFeature.WIFI_TOGGLE, GameFeature.DISPLAY_SCALE)) {
            assertEquals("$feature", Availability.Available, all[feature])
        }
        for (feature in listOf(GameFeature.CLOCK_CONTROL, GameFeature.BYPASS_CHARGING, GameFeature.FLOATING_WINDOW)) {
            assertEquals("$feature", Availability.Unavailable(PrivilegeLevel.ROOT, Cause.NOT_GRANTED), all[feature])
        }
    }

    @Test
    fun `a mode that excludes granted root says to switch the mode, not to grant root`() {
        val shot = av(GameFeature.CLOCK_CONTROL, ExecutionMode.SHIZUKU, root = true, shizuku = true)
        assertEquals(Availability.Unavailable(PrivilegeLevel.ROOT, Cause.MODE_EXCLUDES), shot)
        val basic = av(GameFeature.SCREENSHOT, ExecutionMode.BASIC, root = true, shizuku = false)
        assertEquals(Availability.Unavailable(PrivilegeLevel.SHIZUKU, Cause.MODE_EXCLUDES), basic)
    }

    @Test
    fun `without any privilege only unprivileged features remain`() {
        val all = GameCapabilities.all(ExecutionMode.AUTO, rootGranted = false, shizukuReady = false)
        val available = all.filterValues { it == Availability.Available }.keys
        assertEquals(GameFeature.entries.filter { it.requires == PrivilegeLevel.NONE }.toSet(), available)
        assertTrue(GameFeature.CROSSHAIR_OVERLAY in available)
    }

    @Test
    fun `the requested level of a feature is never above what its row claims`() {
        // الجدول يعكس المسار الفعلي: لا ميزة تُعلَن Shizuku وهي ما زالت بصدفة جذر مباشرة (يحرسه GameSpaceExecutionGuardTest).
        assertEquals(GameFeature.entries.size, GameFeature.entries.map { it.name }.toSet().size)
        assertEquals(PrivilegeLevel.ROOT, GameFeature.FLOATING_WINDOW.requires)
        assertEquals(PrivilegeLevel.SHIZUKU, GameFeature.SCREEN_RECORD.requires)
    }

    @Test
    fun `the mode status tells a missing grant apart from having no privilege at all`() {
        assertEquals(GameModeStatus.ACTIVE_ROOT, GameModeStatus.of(ExecutionMode.AUTO, true, true))
        assertEquals(GameModeStatus.ACTIVE_SHIZUKU, GameModeStatus.of(ExecutionMode.SHIZUKU, true, true))
        assertEquals(GameModeStatus.NEEDS_ROOT, GameModeStatus.of(ExecutionMode.ROOT, false, true))
        assertEquals(GameModeStatus.NEEDS_SHIZUKU, GameModeStatus.of(ExecutionMode.SHIZUKU, true, false))
        assertEquals(GameModeStatus.NO_PRIVILEGE, GameModeStatus.of(ExecutionMode.AUTO, false, false))
        assertEquals(GameModeStatus.ACTIVE_BASIC, GameModeStatus.of(ExecutionMode.BASIC, true, true))
    }
}
