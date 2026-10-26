/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import nd.max.core.privilege.ExecutionMode
import nd.max.ui.util.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * البروفايل الداخلي (بلا رفيق الجذر): الدلالات منقولة من `AppMonitor`، والقاعدة الحاكمة أن لا كتابة بلا
 * لقطة قابلة للاسترجاع، وأن «عدم الإزعاج» لا يخفّض نمطًا أقوى اختاره المستخدم.
 */
class InAppProfileTest {
    private val zen = InAppProfileRules.ZEN
    private val haptic = InAppProfileRules.HAPTIC
    private val refresh = InAppProfileRules.REFRESH_REFS

    // ── ما المطلوب ──────────────────────────────────────────────────────────────────
    @Test
    fun `wants reads only the three in-app knobs and ignores default`() {
        assertFalse(InAppProfileRules.wants(AppConfig(), listOf(60, 90, 120)).any)
        val wants = InAppProfileRules.wants(
            AppConfig(dnd_on_gaming = "true", refresh_rate = "100", haptic_feedback = "true"),
            listOf(60, 90, 120)
        )
        assertTrue(wants.dnd)
        assertTrue(wants.reduceHaptics)
        assertEquals("100 rounds to the nearest supported rate", 90, wants.refreshHz)
    }

    @Test
    fun `refresh rate normalisation follows the controller`() {
        assertEquals(120, InAppProfileRules.normalizeHz(144, listOf(60, 90, 120)))
        assertEquals(144, InAppProfileRules.normalizeHz(144, emptyList()))
        assertNull(InAppProfileRules.normalizeHz(0, listOf(60)))
        assertNull(InAppProfileRules.wants(AppConfig(refresh_rate = "abc"), emptyList()).refreshHz)
    }

    // ── عدم الإزعاج ──────────────────────────────────────────────────────────────────
    @Test
    fun `dnd is written only when zen mode is off right now`() {
        val wants = InAppWants(dnd = true, refreshHz = null, reduceHaptics = false)
        val on = InAppProfileRules.plan(wants, mapOf(zen to "0"))
        assertEquals(listOf(SettingWrite(zen, "1")), on.writes)
        assertEquals(listOf(SavedSetting(zen, "0")), on.saved)

        val silent = InAppProfileRules.plan(wants, mapOf(zen to "2"))
        assertTrue("a stronger mode must never be lowered", silent.writes.isEmpty())
        assertEquals("zen-mode-already-on", silent.skipped[InAppKnob.DND])
    }

    @Test
    fun `an unset zen mode counts as off and is restored by deleting the key`() {
        val wants = InAppWants(dnd = true, refreshHz = null, reduceHaptics = false)
        val plan = InAppProfileRules.plan(wants, mapOf(zen to null))
        assertEquals(listOf(SettingWrite(zen, "1")), plan.writes)
        assertEquals(listOf("settings delete global 'zen_mode'"), InAppProfileRules.restoreCommands(plan.saved))
    }

    @Test
    fun `an unreadable zen mode is never guessed`() {
        val plan = InAppProfileRules.plan(InAppWants(true, null, false), emptyMap())
        assertTrue(plan.writes.isEmpty())
        assertEquals("zen-unreadable", plan.skipped[InAppKnob.DND])
    }

    // ── معدّل التحديث ─────────────────────────────────────────────────────────────────
    @Test
    fun `refresh rate writes all four keys and snapshots all four`() {
        val before = refresh.associateWith<SettingRef, String?> { null } + (refresh[2] to "120")
        val plan = InAppProfileRules.plan(InAppWants(false, 90, false), before)
        assertEquals(4, plan.writes.size)
        assertTrue(plan.writes.all { it.value == "90" })
        assertEquals(refresh, plan.saved.map { it.ref })
        assertEquals("120", plan.saved.first { it.ref == refresh[2] }.value)
    }

    @Test
    fun `refresh rate is skipped when any key cannot be snapshotted`() {
        val partial = mapOf<SettingRef, String?>(refresh[0] to null)
        val plan = InAppProfileRules.plan(InAppWants(false, 90, false), partial)
        assertTrue(plan.writes.isEmpty())
        assertEquals("snapshot-unreadable", plan.skipped[InAppKnob.REFRESH_RATE])
    }

    @Test
    fun `a snapshot value that cannot be quoted safely blocks the knob`() {
        val before = refresh.associateWith<SettingRef, String?> { "1'; rm -rf /" }
        val plan = InAppProfileRules.plan(InAppWants(false, 60, false), before)
        assertTrue(plan.writes.isEmpty())
        assertEquals("snapshot-unreadable", plan.skipped[InAppKnob.REFRESH_RATE])
    }

    // ── اللمس الاهتزازي ───────────────────────────────────────────────────────────────
    @Test
    fun `haptics are reduced once and skipped when already off`() {
        val wants = InAppWants(false, null, true)
        assertEquals(listOf(SettingWrite(haptic, "0")), InAppProfileRules.plan(wants, mapOf(haptic to "1")).writes)
        assertEquals("already-off", InAppProfileRules.plan(wants, mapOf(haptic to "0")).skipped[InAppKnob.HAPTICS])
    }

    // ── اللقطة المحفوظة ──────────────────────────────────────────────────────────────
    @Test
    fun `the saved snapshot survives a round trip including unset values`() {
        val saved = listOf(SavedSetting(zen, "0"), SavedSetting(refresh[0], null), SavedSetting(haptic, "1"))
        val decoded = InAppProfileRules.decodeSaved(InAppProfileRules.encodeSaved("com.game.one", saved))
        assertEquals("com.game.one" to saved, decoded)
    }

    @Test
    fun `a tampered snapshot is rejected instead of being executed`() {
        assertNull(InAppProfileRules.decodeSaved("pkg\tcom.x\ns\tglobal\tzen_mode\t+0; reboot"))
        assertNull(InAppProfileRules.decodeSaved("pkg\tcom.x\ns\tevil\tzen_mode\t+0"))
        assertNull(InAppProfileRules.decodeSaved("pkg\tcom.x\ns\tglobal\tzen mode\t+0"))
        assertNull(InAppProfileRules.decodeSaved("garbage"))
    }

    @Test
    fun `commands quote the key and value`() {
        assertEquals("settings put system 'peak_refresh_rate' '90'", InAppProfileRules.putCommand(refresh[2], "90"))
        assertEquals("settings get secure 'miui_refresh_rate'", InAppProfileRules.getCommand(refresh[1]))
        assertNull(InAppProfileRules.readValue("null"))
        assertEquals("90", InAppProfileRules.readValue(" 90 "))
    }

    // ── من المسؤول ───────────────────────────────────────────────────────────────────
    @Test
    fun `the in-app applier yields to the root companion only on the root lane`() {
        assertFalse(InAppProfileRules.isActive(ExecutionMode.AUTO, rootGranted = true))
        assertFalse(InAppProfileRules.isActive(ExecutionMode.ROOT, rootGranted = true))
        assertTrue(InAppProfileRules.isActive(ExecutionMode.AUTO, rootGranted = false))
        assertTrue(InAppProfileRules.isActive(ExecutionMode.SHIZUKU, rootGranted = true))
        assertTrue(InAppProfileRules.isActive(ExecutionMode.BASIC, rootGranted = true))
    }
}
