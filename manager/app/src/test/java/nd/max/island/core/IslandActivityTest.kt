/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.island.core

import nd.max.island.engine.IslandMode
import nd.max.island.engine.LiveKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * نشاطان معًا — القواعد التي كانت مكتوبة داخل الواجهة (`mode is Live || mode is Timer`) صارت
 * هنا، فتُقاس: هل يُنافَس المؤقّت الملاحة؟ هل تُرقّى الموسيقى فوق مكالمة؟ وهل يبقى الرفيق
 * حين يعرض الأساس إشعارًا؟
 */
class IslandActivityTest {

    private fun media(title: String = "Song") =
        IslandMode.Media(title = title, artist = "Artist", playing = true)

    private fun nav() = IslandMode.Live(LiveKind.Navigation, "Maps", "Turn left", "200 m")

    private fun timer() = IslandMode.Timer(remainingSec = 120, totalSec = 300)

    @Test
    fun `higher ranked activity becomes the primary`() {
        val s = IslandActivityRules.select(
            mediaPromoted = false,
            timer = timer(),
            live = nav(),
            download = null,
            media = media(),
        )
        assertEquals(nav(), s.primary)
        assertEquals(media(), s.companion)
    }

    @Test
    fun `media is alone when it is the only activity`() {
        val s = IslandActivityRules.select(false, null, null, null, media())
        assertEquals(media(), s.primary)
        assertNull(s.companion)
    }

    @Test
    fun `an old timer cannot hide a more important navigation`() {
        val s = IslandActivityRules.select(false, timer(), nav(), null, null)
        assertEquals(nav(), s.primary)
    }

    @Test
    fun `a download is ranked below navigation but above a timer`() {
        val dl = IslandMode.Live(LiveKind.Download, "Browser", "file.zip", "40%", progress = 40)
        val only = IslandActivityRules.select(false, timer(), null, dl, media())
        assertEquals(dl, only.primary)
        assertEquals(media(), only.companion)
        // والترتيب بين الملاحة والتنزيل يُقاس لا يُفترض: تعليمة انعطاف تسبق شريط تقدّم.
        val both = IslandActivityRules.select(false, timer(), nav(), dl, media())
        assertEquals(nav(), both.primary)
        assertEquals(media(), both.companion)
    }

    @Test
    fun `promoting media swaps it with the previous primary`() {
        val s = IslandActivityRules.select(true, timer(), null, null, media())
        assertEquals(media(), s.primary)
        assertEquals(timer(), s.companion)
    }

    @Test
    fun `media is never offered as a companion to a call`() {
        val call = IslandMode.Call("Nader", incoming = true, since = 0L)
        assertNull(IslandActivityRules.companionFor(call, media()))
        assertNull(IslandActivityRules.companionFor(media(), media()))
        assertNull(IslandActivityRules.companionFor(IslandMode.Idle, media()))
        assertEquals(media(), IslandActivityRules.companionFor(timer(), media()))
    }

    @Test
    fun `a call cannot be displaced by promotion`() {
        val call = IslandMode.Call("Nader", incoming = true, since = 0L)
        assertFalse(IslandActivityRules.canPromote(call, media()))
        assertFalse(IslandActivityRules.canPromote(IslandMode.Idle, media()))
        assertFalse(IslandActivityRules.canPromote(null, media()))
        assertFalse(IslandActivityRules.canPromote(timer(), null))
        assertTrue(IslandActivityRules.canPromote(timer(), media()))
    }

    @Test
    fun `activity ranks are stable and media ranks below live activities`() {
        assertTrue(activityOf(timer()).rank > activityOf(media()).rank)
        assertTrue(activityOf(nav()).rank > activityOf(media()).rank)
        assertEquals(IslandActivity.Media, activityOf(media()))
        assertEquals(IslandActivity.Idle, activityOf(null))
        assertEquals(IslandActivity.Idle, activityOf(IslandMode.Idle))
    }
}
