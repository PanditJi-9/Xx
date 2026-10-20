/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** الإشعار يقرأ الهدف **الفاعل** لا التفضيل، ولا يدّعي «يعمل» والمحرك مطفأ. */
class ActiveObjectiveNoticeTest {

    @Test
    fun `encode and decode round trip`() {
        val raw = ActiveObjectiveNotice.encode("performance", "learned")
        assertEquals(ActiveObjectiveNotice("performance", "learned"), ActiveObjectiveNotice.decode(raw))
    }

    @Test
    fun `empty or malformed value is no announcement, not a guessed balanced`() {
        assertNull(ActiveObjectiveNotice.decode(null))
        assertNull(ActiveObjectiveNotice.decode(""))
        assertNull(ActiveObjectiveNotice.decode("turbo|user"))
    }

    @Test
    fun `engine switching to performance reaches the notice even if the saved preference is balanced`() {
        val notice = ActiveObjectiveNotice.resolve(
            aiEnabledRaw = "1",
            activeRaw = ActiveObjectiveNotice.encode("performance", "learned"),
            preferenceRaw = "balanced",
        )
        assertEquals("performance", notice?.label)
    }

    @Test
    fun `before the engine has published, the saved preference is shown`() {
        assertEquals("battery", ActiveObjectiveNotice.resolve("1", "", "battery")?.label)
        assertEquals("balanced", ActiveObjectiveNotice.resolve("1", "", "")?.label)
    }

    @Test
    fun `ai off never claims to be running even with a stale published objective`() {
        assertNull(ActiveObjectiveNotice.resolve("0", ActiveObjectiveNotice.encode("performance", "user"), "performance"))
        assertNull(ActiveObjectiveNotice.resolve(null, "", ""))
    }
}
