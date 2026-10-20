/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** هويّة الإحصاءات: عشوائيّة، ثابتة بين تشغيلين، وتُبدَّل عند السحب — ولا تُستنتج من العتاد. */
class InstallIdentityTest {

    private fun tempFile(): File =
        File(Files.createTempDirectory("install-id").toFile(), InstallIdentities.FILE_NAME)

    @Test
    fun `the identifier is a uuid the schema can cast`() {
        val id = InstallIdentities.newId()
        assertTrue(InstallIdentities.isValid(id))
        assertEquals(36, id.length)
        assertNotEquals(id, InstallIdentities.newId())
    }

    @Test
    fun `the identifier persists across restarts`() {
        val file = tempFile()
        val first = InstallIdentities.loadOrCreate(file)
        assertTrue(first.persisted)
        val second = InstallIdentities.loadOrCreate(file)
        assertEquals(first.id, second.id)
        assertEquals(first.id, file.readText().trim())
    }

    @Test
    fun `garbage on disk is replaced not trusted`() {
        val file = tempFile()
        file.writeText("not-a-uuid")
        val loaded = InstallIdentities.loadOrCreate(file)
        assertTrue(InstallIdentities.isValid(loaded.id))
        assertTrue(loaded.persisted)
        assertEquals(loaded.id, file.readText().trim())
    }

    @Test
    fun `withdrawing removes the file so a new consent starts fresh`() {
        val file = tempFile()
        val first = InstallIdentities.loadOrCreate(file)
        assertTrue(InstallIdentities.remove(file))
        assertFalse(file.exists())
        val second = InstallIdentities.loadOrCreate(file)
        assertNotEquals(first.id, second.id)
    }

    @Test
    fun `a missing file still returns a usable identifier for this session`() {
        val file = tempFile()
        assertFalse(file.exists())
        assertTrue(InstallIdentities.isValid(InstallIdentities.loadOrCreate(file).id))
    }
}
