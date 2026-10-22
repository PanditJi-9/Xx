/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import java.io.File
import nd.max.contract.ContractFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class LobbyEntranceTimelineTest {
    private val impact = LobbyEntranceTimeline.IMPACT_MS.toFloat()
    private val total = LobbyEntranceTimeline.TOTAL_MS.toFloat()

    @Test
    fun `the first frame is a black veil with nothing flashing or revealed`() {
        val f = LobbyEntranceTimeline.frame(0f)
        assertEquals(0f, f.warp, 0f)
        assertEquals(1f, f.tunnel, 0f)
        assertEquals(0f, f.flash, 0f)
        assertEquals(0f, f.emblem, 0f)
        assertEquals(1f, f.veil, 0f)
    }

    @Test
    fun `the last frame has handed the screen back to the lobby`() {
        val f = LobbyEntranceTimeline.frame(total)
        assertEquals(0f, f.veil, 1e-6f)
        assertEquals(0f, f.tunnel, 0f)
        assertEquals(1f, f.emblem, 1e-6f)
        assertTrue("flash must have decayed away", f.flash < 0.001f)
    }

    @Test
    fun `warp accelerates into the impact and never moves backwards`() {
        var last = -1f
        var t = 0f
        while (t <= impact) {
            val w = LobbyEntranceTimeline.warp(t)
            assertTrue("warp went backwards at $t", w >= last)
            last = w
            t += 25f
        }
        assertEquals(1f, LobbyEntranceTimeline.warp(impact), 1e-6f)
        // تسارع: النصف الأول من الزمن يقطع أقل من نصف الشوط.
        assertTrue(LobbyEntranceTimeline.warp(impact / 2f) < 0.5f)
    }

    @Test
    fun `the flash peaks exactly at the impact and is dark before it starts to rise`() {
        assertEquals(1f, LobbyEntranceTimeline.flash(impact), 1e-6f)
        assertEquals(0f, LobbyEntranceTimeline.flash(impact - 400f), 0f)
        assertTrue(LobbyEntranceTimeline.flash(impact - 80f) in 0.01f..0.99f)
        assertTrue(LobbyEntranceTimeline.flash(impact + 100f) < 1f)
        assertTrue("the flash must be gone well before the veil lifts", LobbyEntranceTimeline.flash(impact + 700f) < 0.05f)
    }

    @Test
    fun `the tunnel is cut behind the flash and the emblem only appears after it`() {
        assertEquals(1f, LobbyEntranceTimeline.tunnel(impact - 1f), 0f)
        assertEquals(0f, LobbyEntranceTimeline.tunnel(impact + 140f), 0f)
        assertEquals(0f, LobbyEntranceTimeline.emblem(impact), 0f)
        var last = 0f
        var t = impact
        while (t <= total) {
            val e = LobbyEntranceTimeline.emblem(t)
            assertTrue("emblem went backwards at $t", e >= last)
            last = e
            t += 25f
        }
    }

    @Test
    fun `the veil holds until the fade-out start then lifts linearly`() {
        val start = LobbyEntranceTimeline.VEIL_OUT_START_MS.toFloat()
        assertEquals(1f, LobbyEntranceTimeline.veil(start), 0f)
        assertEquals(0.5f, LobbyEntranceTimeline.veil((start + total) / 2f), 1e-6f)
        assertTrue("the emblem must be (almost) fully shown before the veil lifts", LobbyEntranceTimeline.emblem(start) >= 0.99f)
    }

    @Test
    fun `out-of-range times are clamped and every field stays within 0 to 1`() {
        for (t in listOf(-500f, 0f, 1234f, impact, total, total + 9999f)) {
            val f = LobbyEntranceTimeline.frame(t)
            for (v in listOf(f.warp, f.tunnel, f.flash, f.emblem, f.veil)) {
                assertTrue("value $v out of range at $t", v in 0f..1f)
            }
        }
        assertEquals(LobbyEntranceTimeline.frame(0f), LobbyEntranceTimeline.frame(-500f))
        assertEquals(LobbyEntranceTimeline.frame(total), LobbyEntranceTimeline.frame(total + 9999f))
    }

    @Test
    fun `the skip fade is shorter than the intro so it never feels slower than waiting`() {
        assertTrue(LobbyEntranceTimeline.SKIP_FADE_MS in 100..400)
        assertTrue(LobbyEntranceTimeline.IMPACT_MS < LobbyEntranceTimeline.VEIL_OUT_START_MS)
        assertTrue(LobbyEntranceTimeline.VEIL_OUT_START_MS < LobbyEntranceTimeline.TOTAL_MS)
    }

    /**
     * الصوت مولَّد بسكربت بايثون بثوابت زمنيّة خاصّة به؛ لو اختلفت عن هذه لانفصلت الضربة عن الوميض
     * دون أن يلاحظ أحد في المراجعة. نقرأ السكربت ونقارن (لا يُتجاوَز بصمت إن غاب: يُعلن السبب).
     */
    @Test
    fun `the sound generator uses the same impact and total as the timeline`() {
        val script = File(ContractFixtures.repoRoot, "tools/gen_lobby_sounds.py")
        assumeTrue("tools/gen_lobby_sounds.py not reachable from ${File("").absolutePath}", script.isFile)
        val text = script.readText()
        fun constant(name: String): Int =
            Regex("""(?m)^$name\s*=\s*(\d+)""").find(text)?.groupValues?.get(1)?.toInt()
                ?: error("$name not found in gen_lobby_sounds.py")
        assertEquals(LobbyEntranceTimeline.IMPACT_MS, constant("IMPACT_MS"))
        assertEquals(LobbyEntranceTimeline.TOTAL_MS, constant("TOTAL_MS"))
    }
}
