/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد «هل تبقى القيمة بعد الإقلاع؟» — بلا جهاز.
 *
 * والعطب الذي يحرسه هذا الملف مقيس من الكود نفسه: `setprop` لا يحفظ إلا `persist.*`، وشاشة
 * المحرّر كانت تعدّ رمز خروج صفر حفظًا. فكل اختبار هنا يُسقط إصلاحًا يعود إلى «أعلن الحفظ
 * بمجرّد تنفيذ الأمر» أو إلى «أعد كل مفتاح بعد الإقلاع» (فيعيد كتابة ما تملكه خدمة حيّة).
 */
class SetEditBootTest {

    private val global = SetEditCategory.GLOBAL
    private val prop = SetEditCategory.ANDROID_PROP

    @Test
    fun `settings and persist properties are already stored by the system`() {
        assertEquals(SetEditPersistence.PERSISTENT, setEditPersistenceOf(global, "some_key"))
        assertEquals(SetEditPersistence.PERSISTENT, setEditPersistenceOf(SetEditCategory.SECURE, "some_key"))
        assertEquals(SetEditPersistence.PERSISTENT, setEditPersistenceOf(SetEditCategory.SYSTEM, "some_key"))
        assertEquals(SetEditPersistence.PERSISTENT, setEditPersistenceOf(prop, "persist.sys.maxmanager.debugmode"))
    }

    @Test
    fun `read-only and control properties are never replayed`() {
        assertEquals(SetEditPersistence.READ_ONLY, setEditPersistenceOf(prop, "ro.build.version.sdk"))
        assertEquals(SetEditPersistence.READ_ONLY, setEditPersistenceOf(prop, "init.svc.adbd"))
        assertEquals(SetEditPersistence.CONTROL, setEditPersistenceOf(prop, "ctl.start"))
        assertEquals(SetEditPersistence.CONTROL, setEditPersistenceOf(prop, "ctl.restart"))
    }

    @Test
    fun `an ordinary property is the only kind that needs a boot replay`() {
        assertEquals(SetEditPersistence.UNTIL_REBOOT, setEditPersistenceOf(prop, "debug.hwui.renderer"))
        assertEquals(SetEditPersistence.UNTIL_REBOOT, setEditPersistenceOf(prop, "vendor.debug.probe"))
        assertEquals(SetEditPersistence.UNTIL_REBOOT, setEditPersistenceOf(prop, "sys.some_toggle"))
    }

    @Test
    fun `the plan survives tabs newlines and backslashes`() {
        val entries = listOf(
            SetEditBootOverride("debug.a", "line1\nline2"),
            SetEditBootOverride("debug.b", "tab\there"),
            SetEditBootOverride("debug.c", "back\\slash"),
            SetEditBootOverride("debug.d", "قيمة عربية"),
        )
        assertEquals(entries, SetEditBootCodec.decode(SetEditBootCodec.encode(entries)))
    }

    @Test
    fun `decoding drops keys that must not run at boot`() {
        // خطة قديمة/تالفة قد تحمل ro أو persist أو ctl؛ إعادتها أخطر من إسقاطها.
        val raw = "ro.build.type\tuserdebug\npersist.sys.x\t1\nctl.start\tservice\ndebug.keep\t1"
        val decoded = SetEditBootCodec.decode(raw)
        assertEquals(listOf("debug.keep"), decoded.map { it.key })
    }

    @Test
    fun `a duplicate key keeps the last value exactly once`() {
        val raw = "debug.dup\tfirst\ndebug.dup\tsecond"
        val decoded = SetEditBootCodec.decode(raw)
        assertEquals(1, decoded.size)
        assertEquals("second", decoded[0].value)
    }

    @Test
    fun `a malformed line is dropped instead of guessed`() {
        val raw = "\nno-tab-here\ndebug.good\tok\n\torphan"
        val decoded = SetEditBootCodec.decode(raw)
        assertEquals(listOf("debug.good"), decoded.map { it.key })
    }

    @Test
    fun `the plan keeps the newest entries under the cap`() {
        val entries = (1..(SetEditBootCodec.MAX_ENTRIES + 10)).map {
            SetEditBootOverride("debug.key$it", "v$it")
        }
        val decoded = SetEditBootCodec.decode(SetEditBootCodec.encode(entries))
        assertEquals(SetEditBootCodec.MAX_ENTRIES, decoded.size)
        assertEquals("debug.key${SetEditBootCodec.MAX_ENTRIES + 10}", decoded.last().key)
    }

    @Test
    fun `keys outside the property charset are refused before a shell line exists`() {
        assertTrue(isValidSetEditKey(prop, "debug.hwui.renderer"))
        assertTrue(isValidSetEditKey(prop, "persist.sys.maxmanagerconf.AIenabled"))
        assertTrue(isValidSetEditKey(global, "development_settings_enabled"))
        assertFalse(isValidSetEditKey(prop, "debug.x; reboot"))
        assertFalse(isValidSetEditKey(prop, "debug.x y"))
        assertFalse(isValidSetEditKey(global, "key\$(reboot)"))
        assertFalse(isValidSetEditKey(global, ""))
        assertFalse(isValidSetEditKey(prop, "-starts-with-dash"))
    }

    @Test
    fun `shell quoting keeps dollar and backticks literal`() {
        assertEquals("'plain'", setEditShellQuote("plain"))
        assertEquals("'a b'", setEditShellQuote("a b"))
        assertEquals("'it'\\''s'", setEditShellQuote("it's"))
        assertEquals("'\$(reboot)'", setEditShellQuote("\$(reboot)"))
        assertEquals("'`reboot`'", setEditShellQuote("`reboot`"))
    }

    @Test
    fun `the write command carries the quote on both stores`() {
        assertEquals(
            "setprop debug.probe 'a b\$c'",
            SetEditUtil.writeCommand(prop, "debug.probe", "a b\$c"),
        )
        assertEquals(
            "settings put global probe 'x; reboot'",
            SetEditUtil.writeCommand(global, "probe", "x; reboot"),
        )
    }

    @Test
    fun `a missing reading is never a match`() {
        assertTrue(SetEditWriteRules.honoured("1", " 1 "))
        assertTrue(SetEditWriteRules.honoured("text", "text"))
        assertFalse(SetEditWriteRules.honoured("1", "0"))
        assertFalse(SetEditWriteRules.honoured("1", null))
        assertFalse(SetEditWriteRules.honoured("1", ""))
    }
}
