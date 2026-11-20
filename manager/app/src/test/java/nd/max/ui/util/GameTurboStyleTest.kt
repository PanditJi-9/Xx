/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * مظهر لوحة Game Turbo — يُقاس في JVM عادي.
 *
 * الوعد الذي تحرسه هذه الاختبارات: **الجديد هو الافتراضي**، وأي قيمة مخزَّنة لا تُعرف تسقط إليه
 * ولا تُسقط اللوحة، والمفتاحان المخزَّنان **لا يُعاد تسميتهما** بصمت (إعادة التسمية تُنسي كل مستخدم اختياره).
 */
class GameTurboStyleTest {

    @Test
    fun newDesignIsTheDefault() {
        assertEquals(GameTurboStyle.Neon, GameTurboStyle.DEFAULT)
    }

    @Test
    fun missingOrUnknownValueFallsBackToDefault() {
        assertEquals(GameTurboStyle.DEFAULT, GameTurboStyle.of(null))
        assertEquals(GameTurboStyle.DEFAULT, GameTurboStyle.of(""))
        assertEquals(GameTurboStyle.DEFAULT, GameTurboStyle.of("garbage"))
        assertEquals(GameTurboStyle.DEFAULT, GameTurboStyle.of("NEON "))
    }

    @Test
    fun everyStoredKeyRoundTrips() {
        for (style in GameTurboStyle.entries) assertEquals(style, GameTurboStyle.of(style.key))
    }

    @Test
    fun storedKeysAreStableAndDistinct() {
        assertEquals("neon", GameTurboStyle.Neon.key)
        assertEquals("classic", GameTurboStyle.Classic.key)
        assertNotEquals(GameTurboStyle.Neon.key, GameTurboStyle.Classic.key)
    }

    @Test
    fun classicRemainsSelectable() {
        assertEquals(GameTurboStyle.Classic, GameTurboStyle.of("classic"))
    }

    @Test
    fun everyChooserRequestIsAFreshValue() {
        val before = GameTurboStylePrefs.chooserRequests.value
        GameTurboStylePrefs.requestChooser()
        GameTurboStylePrefs.requestChooser()
        assertEquals(before + 2, GameTurboStylePrefs.chooserRequests.value)
    }
}
