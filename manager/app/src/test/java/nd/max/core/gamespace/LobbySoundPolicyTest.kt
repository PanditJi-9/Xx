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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class LobbySoundPolicyTest {
    @Test
    fun `a disabled switch silences every cue`() {
        for (cue in LobbyCue.entries) {
            assertFalse("$cue", LobbySoundPolicy.allow(cue, nowMs = 10_000L, lastPlayedMs = null, enabled = false))
            assertFalse("$cue", LobbySoundPolicy.allow(cue, nowMs = 10_000L, lastPlayedMs = 0L, enabled = false))
        }
    }

    @Test
    fun `the first play of a cue is always allowed`() {
        for (cue in LobbyCue.entries) {
            assertTrue("$cue", LobbySoundPolicy.allow(cue, nowMs = 0L, lastPlayedMs = null, enabled = true))
        }
    }

    @Test
    fun `a repeat inside the cue gap is dropped and one after it plays`() {
        val cue = LobbyCue.Tick
        val last = 5_000L
        assertFalse(LobbySoundPolicy.allow(cue, last + cue.minGapMs - 1, last, enabled = true))
        assertTrue(LobbySoundPolicy.allow(cue, last + cue.minGapMs, last, enabled = true))
        assertTrue(LobbySoundPolicy.allow(cue, last + cue.minGapMs + 500L, last, enabled = true))
    }

    @Test
    fun `gaps are per cue so a tap does not swallow the tick that follows`() {
        // السياسة تُسأل لكل نوع بوقته الأخير؛ نوعان مختلفان لا يحجب أحدهما الآخر.
        assertTrue(LobbySoundPolicy.allow(LobbyCue.Tick, nowMs = 1_010L, lastPlayedMs = null, enabled = true))
        assertFalse(LobbySoundPolicy.allow(LobbyCue.Tap, nowMs = 1_010L, lastPlayedMs = 1_000L, enabled = true))
    }

    @Test
    fun `a clock that moved backwards never blocks sound forever`() {
        assertTrue(LobbySoundPolicy.allow(LobbyCue.Start, nowMs = 100L, lastPlayedMs = 9_000L, enabled = true))
    }

    @Test
    fun `the intro has no gap and every other cue has a positive one`() {
        assertEquals(0L, LobbyCue.Intro.minGapMs)
        for (cue in LobbyCue.entries - LobbyCue.Intro) assertTrue("$cue", cue.minGapMs > 0L)
    }

    @Test
    fun `volumes are audible but the frequent tick is the quietest ui cue`() {
        for (cue in LobbyCue.entries) assertTrue("$cue", cue.volume > 0f && cue.volume <= 1f)
        val others = LobbyCue.entries - LobbyCue.Tick
        assertTrue(others.all { LobbyCue.Tick.volume < it.volume })
    }

    /** كل قيمة صوت لها ملفّ `res/raw/lobby_<الاسم>.ogg`؛ حذف ملف أو إضافة قيمة دونه يسقط هنا لا على الجهاز. */
    @Test
    fun `every cue has its ogg resource`() {
        val raw = File(ContractFixtures.repoRoot, "manager/app/src/main/res/raw")
        assumeTrue("res/raw not reachable from ${File("").absolutePath}", raw.isDirectory)
        for (cue in LobbyCue.entries) {
            val file = File(raw, "lobby_${cue.name.lowercase()}.ogg")
            assertTrue("missing ${file.name}", file.isFile && file.length() > 0L)
            // ترويسة OGG: «OggS».
            val magic = file.inputStream().use { input -> ByteArray(4).also { input.read(it) } }
            assertEquals("OggS", String(magic, Charsets.US_ASCII))
        }
    }
}
