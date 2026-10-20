/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.license

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * صيغة المفتاح: التجزيء والبصمة والصيغة المعروضة — **بمتجهات مرجعية محسوبة خارج Kotlin**.
 *
 * والمتجهات المثبَّتة هنا (`1T` · `BT` · بصمتا SHA-256) حُسبت بتطبيق Python مستقلّ للخوارزمية
 * نفسها التي في `maxctl/index.ts`. فإذا انحرف أحد الطرفين عن العقد، يسقط أحد الجانبين — وهذا
 * أقوى من اختبار يقارن الكود بنفسه.
 */
class LicenseKeyTest {

    private val canonical1 = "MAXMA1B2C3D4E5F6G7H8J9K01T"
    private val canonical2 = "MAXMZ9Y8X7W6V5T4S3R2Q1P0BT"

    @Test
    fun `the checksum matches an independent implementation of the same algorithm`() {
        assertEquals("1T", LicenseKey.checksum(canonical1))
        assertEquals("BT", LicenseKey.checksum(canonical2))
    }

    @Test
    fun `the fingerprint is sha256 of the canonical form`() {
        assertEquals(
            "56a4e107d113d2874ba23e27d2f6a2c775bbf22035378b6458ce9e4873db5247",
            LicenseKey.sha256Hex(canonical1),
        )
        assertEquals(
            "f5369b5ae5d940a490e2634ebcf0e97e62b3a2259a0d9deb500eb0345b741736",
            LicenseKey.sha256Hex(canonical2),
        )
    }

    @Test
    fun `display form groups five by five with the checksum last`() {
        assertEquals("MAXM-A1B2C-3D4E5-F6G7H-8J9K0-1T", LicenseKey.format(canonical1))
        assertEquals("MAXM-Z9Y8X-7W6V5-T4S3R-2Q1P0-BT", LicenseKey.format(canonical2))
    }

    @Test
    fun `pasted text is normalised however the customer types it`() {
        assertEquals(canonical1, LicenseKey.normalize("MAXM-A1B2C-3D4E5-F6G7H-8J9K0-1T"))
        assertEquals(canonical1, LicenseKey.normalize("maxm a1b2c 3d4e5 f6g7h 8j9k0 1t"))
        assertEquals(canonical1, LicenseKey.normalize("  MAXM-A1B2C-3D4E5-F6G7H-8J9K0-1T  \n"))
        assertEquals(canonical1, LicenseKey.normalize("A1B2C3D4E5F6G7H8J9K01T"))
    }

    @Test
    fun `the classic look-alike confusions are read, not rejected`() {
        assertEquals(canonical1, LicenseKey.normalize("MAXM-A1B2C-3D4E5-F6G7H-8J9KO-1T"))
        assertEquals(canonical1, LicenseKey.normalize("MAXM-AIB2C-3D4E5-F6G7H-8J9K0-1T"))
        assertEquals(canonical1, LicenseKey.normalize("MAXM-A1B2C-3D4E5-F6G7H-8J9K0-1T".replace("1", "l")))
    }

    @Test
    fun `a mistyped checksum is refused before any network call`() {
        assertNull(LicenseKey.normalize("MAXM-A1B2C-3D4E5-F6G7H-8J9K0-1X"))
        assertNull(LicenseKey.normalize("MAXM-A1B2C-3D4E5-F6G7H-8J9K0-99"))
        assertNull(LicenseKey.normalize("MAXM-A1B2C-3D4E5-F6G7H-8J9K1-1T"))
    }

    @Test
    fun `wrong lengths are refused`() {
        assertNull(LicenseKey.normalize(""))
        assertNull(LicenseKey.normalize("MAXM-A1B2C"))
        assertNull(LicenseKey.normalize("MAXM-A1B2C-3D4E5-F6G7H-8J9K0-1T-EXTRA"))
    }

    @Test
    fun `a body that itself begins with the prefix is not mis-stripped`() {
        val body = "MAXM" + "23456789ABCDEFGH"
        val canonical = "MAXM" + body + LicenseKey.checksum("MAXM" + body)
        assertEquals(canonical, LicenseKey.normalize(canonical))
        assertEquals("وبلا البادئة أيضًا", canonical, LicenseKey.normalize(body + LicenseKey.checksum("MAXM" + body)))
    }
}
