/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.license

import nd.max.core.remote.TestKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/**
 * قبول الشهادة — كل سبب رفضٍ يُقاس باسمه، ولا يُدمج تلاعبٌ في «فشل».
 *
 * والمفاتيح حقيقية في كل نداء: زوجٌ للخادم وزوجٌ للمهاجم، والتوقيع بـ`SHA256withECDSA` نفسها
 * التي تُشحن في التطبيق. فالدعوى «توقيعٌ صحيح يمرّ وتوقيعٌ مزوَّر يرسب» تُقاس لا تُدَّعى.
 */
class LicenseCertificateTest {

    private val server = TestKeys.generate()
    private val attacker = TestKeys.generate()

    private fun admit(envelope: LicenseEnvelope, key: TestKeys.Key = server): LicenseAdmission =
        LicenseCertificates.admit(envelope, key.publicBase64, LicenseTestSupport.DEVICE, LicenseTestSupport.NOW)

    private fun rejection(envelope: LicenseEnvelope, key: TestKeys.Key = server): LicenseRejection =
        (admit(envelope, key) as LicenseAdmission.Rejected).reason

    @Test
    fun `a server-signed certificate for this device is admitted`() {
        val accepted = admit(LicenseTestSupport.envelope(LicenseTestSupport.payload(), server))
            as LicenseAdmission.Accepted
        assertEquals(LicenseTestSupport.LICENSE_ID, accepted.payload.licenseId)
        assertEquals(LicenseTestSupport.DEVICE, accepted.payload.device)
        assertEquals("pro", accepted.payload.plan)
        assertTrue("زمن الخادم المحفوظ هو زمن الإصدار أو أحدث", accepted.serverMillis >= LicenseTestSupport.NOW - 60_000)
    }

    @Test
    fun `a single flipped payload byte is refused by name`() {
        val original = LicenseTestSupport.payload().toByteArray(Charsets.UTF_8)
        val signed = LicenseTestSupport.envelope("", server, payloadOverride = original)
        val tampered = original.copyOf()
        tampered[10] = (tampered[10].toInt() xor 0x01).toByte()
        val envelope = signed.copy(payload = Base64.getEncoder().encodeToString(tampered))
        assertEquals(LicenseRejection.SIGNATURE, rejection(envelope))
    }

    @Test
    fun `a certificate signed by another key is refused`() {
        assertEquals(
            LicenseRejection.SIGNATURE,
            rejection(LicenseTestSupport.envelope(LicenseTestSupport.payload(), attacker)),
        )
    }

    @Test
    fun `a certificate copied to another device is refused by name`() {
        val envelope = LicenseTestSupport.envelope(LicenseTestSupport.payload(), server)
        val rejected = LicenseCertificates.admit(
            envelope,
            server.publicBase64,
            LicenseTestSupport.OTHER_DEVICE,
            LicenseTestSupport.NOW,
        ) as LicenseAdmission.Rejected
        assertEquals(LicenseRejection.BOUND_OTHER_DEVICE, rejected.reason)
    }

    @Test
    fun `a build without a public key says so instead of blaming the signature`() {
        val envelope = LicenseTestSupport.envelope(LicenseTestSupport.payload(), server)
        val rejected = LicenseCertificates.admit(envelope, "", LicenseTestSupport.DEVICE, LicenseTestSupport.NOW)
        assertEquals(LicenseRejection.NOT_CONFIGURED, (rejected as LicenseAdmission.Rejected).reason)
    }

    @Test
    fun `the envelope identity must match the signed identity`() {
        val envelope = LicenseTestSupport.envelope(
            LicenseTestSupport.payload(),
            server,
            license = LicenseTestSupport.OTHER_DEVICE,
        )
        assertEquals(LicenseRejection.LICENSE_ID, rejection(envelope))
    }

    @Test
    fun `a newer schema is refused, not guessed`() {
        val envelope = LicenseTestSupport.envelope(LicenseTestSupport.payload(schema = 2), server)
        assertEquals(LicenseRejection.SCHEMA, rejection(envelope))
    }

    @Test
    fun `another kind of signed document is not a licence`() {
        val envelope = LicenseTestSupport.envelope(LicenseTestSupport.payload(kind = "config"), server)
        assertEquals(LicenseRejection.KIND, rejection(envelope))
    }

    @Test
    fun `a signed state this build does not know is refused`() {
        val envelope = LicenseTestSupport.envelope(LicenseTestSupport.payload(state = "revoked"), server)
        assertEquals(LicenseRejection.STATE, rejection(envelope))
    }

    @Test
    fun `an expiry that is not after issuance is refused`() {
        val same = LicenseTestSupport.payload(expiresAt = LicenseTestSupport.iso(LicenseTestSupport.NOW - 60_000))
        assertEquals(LicenseRejection.EXPIRES_INVALID, rejection(LicenseTestSupport.envelope(same, server)))

        val backwards = LicenseTestSupport.payload(expiresAt = LicenseTestSupport.iso(LicenseTestSupport.NOW - 120_000))
        assertEquals(LicenseRejection.EXPIRES_INVALID, rejection(LicenseTestSupport.envelope(backwards, server)))

        val broken = LicenseTestSupport.payload(expiresAt = "ليس وقتًا")
        assertEquals(LicenseRejection.EXPIRES_INVALID, rejection(LicenseTestSupport.envelope(broken, server)))
    }

    @Test
    fun `an issuance from the future is refused`() {
        val future = LicenseTestSupport.payload(issuedAt = LicenseTestSupport.iso(LicenseTestSupport.NOW + 60 * 60 * 1000))
        assertEquals(LicenseRejection.ISSUED_INVALID, rejection(LicenseTestSupport.envelope(future, server)))

        val broken = LicenseTestSupport.payload(issuedAt = "غدًا")
        assertEquals(LicenseRejection.ISSUED_INVALID, rejection(LicenseTestSupport.envelope(broken, server)))

        val brokenServer = LicenseTestSupport.payload(serverTime = "بعد قليل")
        assertEquals(LicenseRejection.ISSUED_INVALID, rejection(LicenseTestSupport.envelope(brokenServer, server)))
    }

    @Test
    fun `a validity span beyond any real plan is refused`() {
        val absurd = LicenseTestSupport.payload(
            expiresAt = LicenseTestSupport.iso(LicenseTestSupport.NOW + 41L * 365 * LicenseTestSupport.DAY),
        )
        assertEquals(LicenseRejection.EXPIRES_INVALID, rejection(LicenseTestSupport.envelope(absurd, server)))
    }

    @Test
    fun `malformed and oversized payloads are refused before any parsing`() {
        val notBase64 = LicenseEnvelope(schema = 1, license = LicenseTestSupport.LICENSE_ID, payload = "!!!", signature = "AA==")
        assertEquals(LicenseRejection.MALFORMED, rejection(notBase64))

        val empty = LicenseEnvelope(schema = 1, license = LicenseTestSupport.LICENSE_ID, payload = "", signature = "AA==")
        assertEquals(LicenseRejection.MALFORMED, rejection(empty))

        val huge = LicenseEnvelope(
            schema = 1,
            license = LicenseTestSupport.LICENSE_ID,
            payload = Base64.getEncoder().encodeToString(ByteArray(LicenseCertificates.MAX_PAYLOAD_BYTES + 1)),
            signature = "AA==",
        )
        assertEquals(LicenseRejection.MALFORMED, rejection(huge))
    }

    @Test
    fun `the envelope round-trips through its wire form`() {
        val envelope = LicenseTestSupport.envelope(LicenseTestSupport.payload(), server)
        val text = """{"schema":${envelope.schema},"license":"${envelope.license}","payload":"${envelope.payload}","signature":"${envelope.signature}"}"""
        val parsed = LicenseCertificates.parseEnvelope(text)
        assertNotNull(parsed)
        assertEquals(envelope.signature, parsed!!.signature)
    }
}
