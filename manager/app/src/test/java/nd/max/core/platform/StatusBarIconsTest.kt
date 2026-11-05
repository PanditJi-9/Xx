/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 */
package nd.max.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * شريط الحالة بلا جذر — القائمة تُقاس بقراءة/كتابة مزيّفة، فلا أندرويد ولا جهاز.
 *
 * **وأخطر خطأ هنا هو محو إخفاءات المستخدم:** روم يضيف رمزًا لا نعرفه، ثم نكتب القائمة من جديد
 * بالمعروف وحده ⇒ يختفي إخفاؤه. الدعاوى السبع تحرس هذه الحالة قبل غيرها.
 */
class StatusBarIconsTest {

    private class Fake {
        val store = HashMap<String, String>()
        var writes = 0

        fun icons() = StatusBarIcons(
            read = { ns, key -> store["$ns:$key"] },
            write = { ns, key, value -> writes++; store["$ns:$key"] = value; true },
        )
    }

    @Test
    fun anEmptyOrMissingSettingParsesToNothing() {
        assertTrue(StatusBarIcons.parse(null).isEmpty)
        assertTrue(StatusBarIcons.parse("").isEmpty)
        assertTrue(StatusBarIcons.parse("  , ,").isEmpty)
    }

    @Test
    fun knownTokensAreRecognisedInAnyOrderAndCase() {
        val state = StatusBarIcons.parse(" Clock , wifi ,BATTERY ")

        assertEquals(setOf(StatusBarIcon.CLOCK, StatusBarIcon.WIFI, StatusBarIcon.BATTERY), state.known)
        assertTrue(state.unknown.isEmpty())
    }

    @Test
    fun unknownTokensFromTheRomArePreserved() {
        val state = StatusBarIcons.parse("wifi,nfc,headset")

        assertEquals(setOf(StatusBarIcon.WIFI), state.known)
        assertEquals(listOf("nfc", "headset"), state.unknown)
    }

    @Test
    fun hidingOneIconKeepsTheRomTokensAndTheOthers() {
        val fake = Fake()
        fake.store["secure:icon_blacklist"] = "clock,nfc"

        assertTrue(fake.icons().hide(setOf(StatusBarIcon.WIFI)))

        assertEquals("wifi,clock,nfc", fake.store["secure:icon_blacklist"])
    }

    @Test
    fun encodeIsDeterministicAndDropsDuplicates() {
        val state = IconBlacklist(
            known = setOf(StatusBarIcon.BATTERY, StatusBarIcon.WIFI),
            unknown = listOf("nfc", "wifi"),
        )

        assertEquals("wifi,battery,nfc", StatusBarIcons.encode(state))
    }

    @Test
    fun cleanBarHidesEverythingKnownAndStillKeepsRomTokens() {
        val fake = Fake()
        fake.store["secure:icon_blacklist"] = "nfc"
        val icons = fake.icons()

        assertTrue(icons.setAllHidden(true))
        assertTrue(icons.allHidden())
        assertTrue(fake.store["secure:icon_blacklist"]!!.endsWith(",nfc"))

        assertTrue(icons.setAllHidden(false))
        assertFalse(icons.allHidden())
        assertEquals("nfc", fake.store["secure:icon_blacklist"])
    }

    @Test
    fun toggleFlipsAndTheOtherKeysUseTheirOwnNamespace() {
        val fake = Fake()
        val icons = fake.icons()

        assertTrue(icons.toggle(StatusBarIcon.CLOCK))
        assertTrue(icons.isHidden(StatusBarIcon.CLOCK))
        assertTrue(icons.toggle(StatusBarIcon.CLOCK))
        assertFalse(icons.isHidden(StatusBarIcon.CLOCK))

        assertTrue(icons.immersive(true))
        assertEquals(StatusBarIcons.IMMERSIVE_FULL, fake.store["global:policy_control"])
        assertTrue(icons.notificationBadging(false))
        assertEquals("0", fake.store["secure:notification_badging"])
        assertTrue(icons.notificationIcon(true))
        assertEquals("1", fake.store["secure:show_notification_icon"])
    }
}
