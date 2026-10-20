/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.atlas

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The store is the last step between a captured run and a file the user may hand over, so each test here
 * pins one way that step could lie: an artifact that does not read back unchanged, a file that lands past
 * the ceiling the format declares, a share whose file is deleted out from under it, and a temporary file
 * that survives a write.
 */
class AtlasFixtureStoreTest {

    @Test
    fun `a written fixture reads back unchanged and is listed`() {
        val store = AtlasFixtureStore(temporaryDirectory())
        val text = """{"schema":1,"origin":"DEVICE","catalog":"atlas-1","entries":[]}"""

        val file = store.write(text)

        assertTrue("a bounded artifact is written", file != null)
        assertEquals("and is handed over whole", text, store.read(file!!))
        assertEquals(listOf(file), store.fixtures())
        assertTrue("no temporary file survives the write", store.temporaryFiles().isEmpty())
    }

    @Test
    fun `an artifact past the format ceiling is refused before a file exists`() {
        val store = AtlasFixtureStore(temporaryDirectory())
        val oversize = "x".repeat(AtlasFixture.MAX_BYTES + 1)

        assertNull("a bigger artifact than the format allows is not written", store.write(oversize))
        assertTrue("and nothing is left behind", store.fixtures().isEmpty())
        assertTrue(store.temporaryFiles().isEmpty())
    }

    @Test
    fun `an oversize file already on disk is refused instead of read`() {
        val directory = temporaryDirectory()
        val store = AtlasFixtureStore(directory)
        val planted = java.io.File(directory, "atlas-fixture-1.json")
        planted.writeText("x".repeat(AtlasFixture.MAX_BYTES + 1))

        assertNull("reading it would cost a bound the format already declared", store.read(planted))
    }

    @Test
    fun `cleanup keeps the file a share may still be reading`() {
        var now = 1_000L
        val store = AtlasFixtureStore(temporaryDirectory(), clockMs = { now })
        val first = store.write("""{"schema":1}""")!!
        first.setLastModified(1_000L)
        now = 100_000L
        val second = store.write("""{"schema":1}""")!!
        second.setLastModified(100_000L)
        now = 200_000L

        assertEquals("only the unprotected one goes", 1, store.cleanup(maxAgeMs = 1_000L, protected = second))
        assertEquals(listOf(second), store.fixtures())
    }

    @Test
    fun `a fixture that cannot be aged is kept`() {
        val store = AtlasFixtureStore(temporaryDirectory(), clockMs = { 0L })
        val file = store.write("""{"schema":1}""")!!
        // A clock that moved backwards: the age is negative, so it is not "old" and must not be deleted.
        file.setLastModified(50_000L)

        assertEquals(0, store.cleanup(maxAgeMs = 1_000L))
        assertEquals(listOf(file), store.fixtures())
    }

    @Test
    fun `the directory is declared once`() {
        val root = temporaryDirectory()
        assertEquals("atlas-fixtures", AtlasFixtureStore.directoryFor(root).name)
    }

    private fun temporaryDirectory(): java.io.File =
        Files.createTempDirectory("atlas-fixture-store-").toFile().also { it.deleteOnExit() }
}
