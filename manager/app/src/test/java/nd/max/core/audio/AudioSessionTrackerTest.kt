/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * قياس متتبع جلسات الصوت ومحلل مخرجات مسارات الصوت (AudioFlinger).
 *
 * المنصات الحديثة تعزل مشغلات الوسائط بجلسات موجبة مستقلة؛ هذا الاختبار يضمن:
 * 1. حصر الجلسات الموجبة واستبعاد الصفر والقيم السالبة.
 * 2. إشعار المستمعين بتغير الجلسات.
 * 3. دقة استخراج أرقام الجلسات من سجلات النظام (dumpsys media.audio_flinger).
 */
class AudioSessionTrackerTest {

    @Test
    fun `opening positive session adds to active sessions and notifies listeners`() {
        val tracker = AudioSessionTracker()
        val opened = mutableListOf<Pair<Int, String?>>()
        val listener = object : AudioSessionTracker.SessionListener {
            override fun onSessionOpened(sessionId: Int, packageName: String?) {
                opened.add(sessionId to packageName)
            }
            override fun onSessionClosed(sessionId: Int) {}
        }
        tracker.addListener(listener)

        tracker.onSessionOpened(42, "com.spotify.music")

        assertEquals(1, tracker.activeSessions.value.size)
        assertEquals("com.spotify.music", tracker.activeSessions.value[42])
        assertEquals(listOf(42 to "com.spotify.music"), opened)
    }

    @Test
    fun `sessions with zero or negative id are strictly ignored`() {
        val tracker = AudioSessionTracker()
        tracker.onSessionOpened(0, "global")
        tracker.onSessionOpened(-1, "invalid")

        assertTrue(tracker.activeSessions.value.isEmpty())
    }

    @Test
    fun `closing session removes it and notifies listener`() {
        val tracker = AudioSessionTracker()
        val closed = mutableListOf<Int>()
        val listener = object : AudioSessionTracker.SessionListener {
            override fun onSessionOpened(sessionId: Int, packageName: String?) {}
            override fun onSessionClosed(sessionId: Int) {
                closed.add(sessionId)
            }
        }
        tracker.addListener(listener)

        tracker.onSessionOpened(101, "com.google.android.youtube")
        assertEquals(1, tracker.activeSessions.value.size)

        tracker.onSessionClosed(101)
        assertTrue(tracker.activeSessions.value.isEmpty())
        assertEquals(listOf(101), closed)
    }

    @Test
    fun `removing listener stops event delivery`() {
        val tracker = AudioSessionTracker()
        var eventCount = 0
        val listener = object : AudioSessionTracker.SessionListener {
            override fun onSessionOpened(sessionId: Int, packageName: String?) { eventCount++ }
            override fun onSessionClosed(sessionId: Int) { eventCount++ }
        }
        tracker.addListener(listener)
        tracker.onSessionOpened(50, "app1")
        assertEquals(1, eventCount)

        tracker.removeListener(listener)
        tracker.onSessionOpened(60, "app2")
        tracker.onSessionClosed(50)
        assertEquals(1, eventCount)
    }

    @Test
    fun `parseAudioFlingerDump extracts session ids across diverse formats`() {
        val tracker = AudioSessionTracker()
        val dump = listOf(
            "Output thread 0xb4000072: active tracks 2",
            "  Track 1: session: 124, stream: 3, state: ACTIVE",
            "  Track 2: audio_session_t 89, stream: 3, format: 0x1",
            "  Track 3: Session Id: 42, flags: 0x4",
            "  Track 4: session 0, stream: 1 (global, ignored)",
            "  Track 5: sessionId=55, format: pcm",
            "Non-matching line: sampleRate 48000 channels 2",
        )

        val result = tracker.parseAudioFlingerDump(dump)

        assertEquals(listOf(124, 89, 42, 55), result)
    }

    @Test
    fun `singleton getter returns valid instance`() {
        val instance = AudioSessionTracker.get()
        assertNotNull(instance)
    }
}
