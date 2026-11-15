/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * قياس توزيع الخيوط عبر الجلسة بلا جهاز: القراءة نصّية والفرق حسابيّ، فكلاهما يُقاس هنا.
 *
 * وحدّه: منحنى التوقيت الحقيقي لا يُقاس باختبار — القراءتان تحتاجان `su` وعملية حيّة.
 */
class SessionThreadProbeTest {

    // ── قراءة المخرَج ─────────────────────────────────────────────────────────

    @Test
    fun `snapshot reads pid and thread table from one output`() {
        val snapshot = SessionThreadProbe.parseSnapshot(
            listOf(
                "pid=4242",
                "4242 (RenderThread) S 1 4242 0 0 -1 0 0 0 0 0 500 200 0 0 20 0",
                "4243 (UnityMain) S 1 4242 0 0 -1 0 0 0 0 0 900 100 0 0 20 0",
            )
        )
        assertEquals(4242, snapshot?.pid)
        assertEquals(2, snapshot?.threads?.size)
        assertEquals(1000L, snapshot?.threads?.first()?.ticks)
    }

    @Test
    fun `snapshot keeps a comm with spaces and parens`() {
        val snapshot = SessionThreadProbe.parseSnapshot(
            listOf(
                "pid=7",
                "77 (Job Worker (gfx)) S 1 7 0 0 -1 0 0 0 0 0 10 20 0 0 20 0",
            )
        )
        assertEquals("Job Worker (gfx)", snapshot?.threads?.single()?.name)
        assertEquals(30L, snapshot?.threads?.single()?.ticks)
    }

    @Test
    fun `snapshot without a pid line is unknown not empty`() {
        assertNull(SessionThreadProbe.parseSnapshot(emptyList()))
        assertNull(
            SessionThreadProbe.parseSnapshot(
                listOf("4242 (RenderThread) S 1 4242 0 0 -1 0 0 0 0 0 500 200 0 0 20 0")
            )
        )
    }

    @Test
    fun `snapshot with a pid but no readable thread is unknown`() {
        assertNull(SessionThreadProbe.parseSnapshot(listOf("pid=4242", "", "garbage")))
    }

    // ── الفرق بين اللقطتين ────────────────────────────────────────────────────

    @Test
    fun `loads are the delta per thread highest first`() {
        val loads = SessionThreadProbe.loads(
            snap(4242, thread(1, 1_000), thread(2, 500)),
            snap(4242, thread(1, 1_600), thread(2, 2_500)),
        )
        assertEquals(listOf(2, 1), loads?.map { it.tid })
        assertEquals(listOf(2_000L, 600L), loads?.map { it.ticks })
    }

    @Test
    fun `a thread born mid-session is left out`() {
        val loads = SessionThreadProbe.loads(
            snap(4242, thread(1, 100)),
            snap(4242, thread(1, 200), thread(9, 5_000)),
        )
        assertEquals(listOf(1), loads?.map { it.tid })
    }

    @Test
    fun `a thread that ended before the stop is left out`() {
        val loads = SessionThreadProbe.loads(
            snap(4242, thread(1, 100), thread(2, 100)),
            snap(4242, thread(1, 300)),
        )
        assertEquals(listOf(1), loads?.map { it.tid })
    }

    @Test
    fun `a different pid is unknown not a difference`() {
        assertNull(SessionThreadProbe.loads(snap(4242, thread(1, 1)), snap(9999, thread(1, 9))))
    }

    @Test
    fun `a missing snapshot is unknown`() {
        assertNull(SessionThreadProbe.loads(null, snap(4242, thread(1, 9))))
        assertNull(SessionThreadProbe.loads(snap(4242, thread(1, 9)), null))
    }

    @Test
    fun `no growth is unknown rather than zeros`() {
        assertNull(SessionThreadProbe.loads(snap(4242, thread(1, 100)), snap(4242, thread(1, 100))))
    }

    @Test
    fun `a backward reading is dropped not displayed`() {
        assertNull(SessionThreadProbe.loads(snap(4242, thread(1, 900)), snap(4242, thread(1, 100))))
    }

    @Test
    fun `only the busiest threads are kept`() {
        val start = (1..12).map { thread(it, 0) }
        val end = (1..12).map { thread(it, it * 10L) }
        val loads = SessionThreadProbe.loads(snap(1, *start.toTypedArray()), snap(1, *end.toTypedArray()))
        assertEquals(SessionThreadProbe.MAX_THREADS, loads?.size)
        assertEquals(listOf(12, 11, 10, 9, 8, 7), loads?.map { it.tid })
        assertTrue(loads!!.all { it.ticks > 0L })
    }

    private fun snap(pid: Int, vararg threads: ThreadTime) = ThreadsSnapshot(pid, threads.toList())

    private fun thread(tid: Int, ticks: Long) = ThreadTime(tid = tid, name = "t$tid", ticks = ticks)
}
