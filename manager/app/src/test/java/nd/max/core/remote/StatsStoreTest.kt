/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** ذاكرة القناة على القرص: تدور كاملةً، وتتحمّل ملفًّا تالفًا بلا استثناء. */
class StatsStoreTest {

    private fun tempFile(): File =
        File(Files.createTempDirectory("stats-store").toFile(), "stats.state")

    @Test
    fun `an absent file reads as the empty state`() {
        assertEquals(StatsState(), StatsStore(tempFile()).read())
    }

    @Test
    fun `the state round trips through the file`() {
        val store = StatsStore(tempFile())
        val state = StatsState(
            lastSentAt = 123,
            lastSentVersion = "42",
            lastSentEnv = "shizuku",
            lastSentHealth = "{\"cpu\":{\"confirmed\":1,\"rolled_back\":0,\"unknown\":0}}",
            attempts = 2,
            lastAttemptAt = 456,
        )
        assertTrue(store.write(state))
        assertEquals(state, store.read())
    }

    @Test
    fun `a stray newline in the health line never breaks the file`() {
        val store = StatsStore(tempFile())
        store.write(StatsState(lastSentHealth = "a\nb"))
        // السطر يُطوى عند الكتابة، فلا يُقرأ الملفّ سطرين في تشغيلٍ تالٍ.
        assertEquals("a b", store.read().lastSentHealth)
    }

    @Test
    fun `a corrupt file reads as the empty state never a crash`() {
        val file = tempFile()
        file.writeText("sent_at=not-a-number\nwhat is this\n")
        assertEquals(StatsState(), StatsStore(file).read())
    }

    @Test
    fun `clear removes the record`() {
        val store = StatsStore(tempFile())
        store.write(StatsState(lastSentAt = 9))
        assertTrue(store.clear())
        assertEquals(StatsState(), store.read())
    }
}
