/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.license

import nd.max.core.remote.TestKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * ذاكرة الشهادة على ملفّ واحد — بنفس قواعد `ConfigStore` المقيسة:
 * **التلف = الغياب** · **الكتابة ذرّية** · **مفتاحٌ آخر لا يفكّ** · **المحاولة لا تمسح الشهادة**.
 *
 * والمُشفِّر هنا XOR بسيط **مقصود**: ما يُقاس هو منطق المخزن لا قوّة التشفير (تلك تُقاس في
 * الأندرويد بمخزن المفاتيح نفسه)، وXOR يكفي لإثبات أن الملفّ ليس نصًّا صريحًا وأن مفتاحًا آخر
 * يُنتج عطبًا لا شهادةً.
 */
class LicenseStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val server = TestKeys.generate()

    private fun store(cipher: LicenseCipher = LicenseTestSupport.XorCipher()): Pair<LicenseStore, File> {
        val file = File(folder.root, LicenseStore.FILE_NAME)
        return LicenseStore(file, cipher) to file
    }

    private fun record(): LicenseRecord = LicenseTestSupport.record(LicenseTestSupport.payload(), server)

    @Test
    fun `write and read round-trips the certificate`() {
        val (licenseStore, _) = store()
        val original = record()
        assertTrue(licenseStore.write(original))
        val read = licenseStore.read()
        assertEquals(LicenseTestSupport.LICENSE_ID, read?.licenseId)
        assertEquals(LicenseTestSupport.DEVICE, read?.device)
        assertEquals(original.certificate.signature, read?.certificate?.signature)
    }

    @Test
    fun `the file on disk is sealed, not plaintext`() {
        val (licenseStore, file) = store()
        licenseStore.write(record())
        val text = file.readBytes().toString(Charsets.UTF_8)
        assertFalse("لا يظهر معرّف المفتاح في ملفّ مشفَّر", text.contains("licenseId"))
        assertFalse(text.contains(LicenseTestSupport.LICENSE_ID))
    }

    @Test
    fun `a corrupted file is treated as absent, never as a crash`() {
        val (licenseStore, file) = store()
        licenseStore.write(record())
        file.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        assertNull(licenseStore.read())
    }

    @Test
    fun `a file read with another key does not open`() {
        val (writer, file) = store(LicenseTestSupport.XorCipher(0x5A))
        writer.write(record())
        val stranger = LicenseStore(file, LicenseTestSupport.XorCipher(0x11))
        assertNull("شهادةٌ نُسخت إلى جهاز آخر لا تُفكّ", stranger.read())
    }

    @Test
    fun `an unknown format is treated as absent`() {
        val (licenseStore, _) = store()
        licenseStore.write(record().copy(format = 99))
        assertNull(licenseStore.read())
    }

    @Test
    fun `a record without a certificate is treated as absent`() {
        val (licenseStore, _) = store()
        licenseStore.write(record().copy(licenseId = ""))
        assertNull(licenseStore.read())
    }

    @Test
    fun `noteAttempt keeps the certificate and records the outcome`() {
        val (licenseStore, _) = store()
        licenseStore.write(record())
        assertTrue(licenseStore.noteAttempt(LicenseTestSupport.NOW + 5000, "license_network"))
        val read = licenseStore.read()
        assertEquals("license_network", read?.lastOutcome)
        assertEquals(LicenseTestSupport.NOW + 5000, read?.lastAttemptMillis)
        assertEquals("والشهادة لم تُمَسّ", LicenseTestSupport.LICENSE_ID, read?.licenseId)
    }

    @Test
    fun `noteAttempt anchors the server clock forward only`() {
        val (licenseStore, _) = store()
        val original = record()
        licenseStore.write(original)
        licenseStore.noteAttempt(LicenseTestSupport.NOW, "expired", serverMillis = LicenseTestSupport.NOW + LicenseTestSupport.DAY)
        assertEquals(
            "المرساة تُقدَّم من ردّ الخادم",
            LicenseTestSupport.NOW + LicenseTestSupport.DAY,
            licenseStore.read()?.serverMillis,
        )
        licenseStore.noteAttempt(LicenseTestSupport.NOW, "expired", serverMillis = 1L)
        assertEquals(
            "ولا تُرجَع للوراء أبدًا",
            LicenseTestSupport.NOW + LicenseTestSupport.DAY,
            licenseStore.read()?.serverMillis,
        )
    }

    @Test
    fun `noteAttempt on an empty store writes nothing`() {
        val (licenseStore, file) = store()
        assertFalse(licenseStore.noteAttempt(LicenseTestSupport.NOW, "license_network"))
        assertFalse("ولا يُنشئ ملفًّا من العدم", file.exists())
    }

    @Test
    fun `clear removes the file and is true when there is nothing to remove`() {
        val (licenseStore, file) = store()
        licenseStore.write(record())
        assertTrue(licenseStore.clear())
        assertFalse(file.exists())
        assertTrue("والحذف مرّتين ليس عطبًا", licenseStore.clear())
    }
}
