/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.license

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** هويّة الجهاز: عشوائية، تُستقرّ على قرص، وتُستبدل إن كان المحتوى غير صالح. */
class DeviceIdentityTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun file(): File = File(folder.root, DeviceIdentities.FILE_NAME)

    @Test
    fun `a fresh install gets a random 128-bit id and it persists`() {
        val file = file()
        val first = DeviceIdentities.loadOrCreate(file)
        assertTrue(DeviceIdentities.isValid(first.id))
        assertTrue(first.persisted)
        assertEquals("المعرّف مكتوب على القرص", first.id, file.readText(Charsets.UTF_8).trim())
        assertEquals("ويُقرأ نفسه في المرة القادمة", first.id, DeviceIdentities.loadOrCreate(file).id)
    }

    @Test
    fun `two installs never share an id`() {
        val a = DeviceIdentities.loadOrCreate(file())
        val b = DeviceIdentities.loadOrCreate(File(folder.root, "second/${DeviceIdentities.FILE_NAME}"))
        assertNotEquals(a.id, b.id)
        assertTrue(DeviceIdentities.isValid(a.id) && DeviceIdentities.isValid(b.id))
    }

    @Test
    fun `garbage content is replaced, not trusted`() {
        val file = file()
        file.writeText("ليس معرّفًا", Charsets.UTF_8)
        val identity = DeviceIdentities.loadOrCreate(file)
        assertTrue(DeviceIdentities.isValid(identity.id))
        assertEquals(identity.id, file.readText(Charsets.UTF_8).trim())
    }

    @Test
    fun `an uppercase id on disk is normalised to the canonical form`() {
        val file = file()
        val upper = "ABCDEF0123456789ABCDEF0123456789"
        file.writeText(upper, Charsets.UTF_8)
        assertEquals(upper.lowercase(), DeviceIdentities.loadOrCreate(file).id)
    }

    @Test
    fun `only 32 lowercase hex characters are a valid id`() {
        assertTrue(DeviceIdentities.isValid("0123456789abcdef0123456789abcdef"))
        assertFalse(DeviceIdentities.isValid("0123456789abcdef0123456789abcde"))
        assertFalse(DeviceIdentities.isValid("0123456789abcdef0123456789abcdeZ"))
        assertFalse(DeviceIdentities.isValid(""))
    }

    @Test
    fun `the preview keeps the ends and hides the middle`() {
        assertEquals(
            "012345…cdef",
            DeviceIdentities.preview("0123456789abcdef0123456789abcdef"),
        )
    }
}
