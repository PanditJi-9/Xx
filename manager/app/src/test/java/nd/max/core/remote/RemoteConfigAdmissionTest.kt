/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/**
 * الحالات المضادّة ٣ · ٤ · ٧ · ٨ من §٥.٤ — **بالرمز لا بالوصف**: كل دعوى تفحص `RemoteRejection`
 * الذي وقع بعينه، لأن «رُفض شيء ما» ليست دعوى يمكن تكذيبها.
 */
class RemoteConfigAdmissionTest {

    private val server = TestKeys.generate()
    private val attacker = TestKeys.generate()

    private fun admit(
        envelope: SignedEnvelope,
        key: String = server.publicBase64,
        floor: Long = 0L,
        now: Long = TestConfigs.NOW,
    ): Admission = RemoteConfigs.admit(envelope, key, floor, now)

    @Test
    fun `a real signature is admitted and the flags arrive`() {
        val payload = TestConfigs.payloadJson(
            revision = 5,
            features = """{"gamespace":{"enabled":false},"maxai":{"enabled":true}}""",
        )
        val admitted = admit(TestConfigs.envelope(payload, server))
        assertTrue("إعداد صالح يجب أن يُقبل", admitted is Admission.Admitted)
        val config = (admitted as Admission.Admitted).config
        assertEquals("الإصدار", 5L, config.revision)
        assertEquals("مفتاح مطفأ من الخادم", false, config.features.getValue("gamespace").enabled)
        assertEquals("مفتاح مشغَّل من الخادم", true, config.features.getValue("maxai").enabled)
    }

    @Test
    fun `a forged key never gets in`() {
        val payload = TestConfigs.payloadJson(revision = 9)
        // الحمولة موقّعة بتوقيع صحيح — لكن بمفتاح آخر، وهذا كل ما يفعله الخصم.
        val forged = TestConfigs.envelope(payload, attacker)
        val rejected = admit(forged)
        assertEquals(
            "توقيع بمفتاح غير مفتاحنا يجب أن يُرفض برمزه",
            RemoteRejection.SIGNATURE,
            (rejected as Admission.Rejected).reason,
        )
    }

    @Test
    fun `turning a flag on without re-signing is refused`() {
        val signed = TestConfigs.payloadJson(
            revision = 11,
            features = """{"gamespace":{"enabled":false}}""",
        )
        val envelope = TestConfigs.envelope(signed, server)
        // التعديل الذي يخيف فعلًا: `false` ⇒ `true` في الحمولة، بلا لمس التوقيع.
        val tampered = String(TestConfigs.bytes(envelope), Charsets.UTF_8)
            .replace("\"enabled\":false", "\"enabled\":true")
        assertFalse("التعديل يجب أن يُغيّر البايتات فعلًا", tampered == signed)
        val resignedFree = envelope.copy(
            payload = Base64.getEncoder().encodeToString(tampered.toByteArray(Charsets.UTF_8)),
        )
        val rejected = admit(resignedFree)
        assertEquals(
            "بايت مقلوب داخل حمولة موقّعة يُسقط التوقيع",
            RemoteRejection.SIGNATURE,
            (rejected as Admission.Rejected).reason,
        )
    }

    @Test
    fun `a lower revision is refused, an equal one too, a higher one passes`() {
        val payload = TestConfigs.payloadJson(revision = 5)
        val envelope = TestConfigs.envelope(payload, server)
        assertEquals(
            "إصدار أدنى من المسجَّل = رجوع ممنوع",
            RemoteRejection.REVISION,
            (admit(envelope, floor = 9L) as Admission.Rejected).reason,
        )
        assertEquals(
            "إصدار يساوي المسجَّل لا يُقبل مرّتين",
            RemoteRejection.REVISION,
            (admit(envelope, floor = 5L) as Admission.Rejected).reason,
        )
        assertTrue("أعلى من المسجَّل يُقبل", admit(envelope, floor = 4L) is Admission.Admitted)
    }

    @Test
    fun `an envelope whose revision disagrees with the signed one is refused`() {
        val payload = TestConfigs.payloadJson(revision = 5)
        val lying = TestConfigs.envelope(payload, server, revision = 99L)
        assertEquals(
            "إصدار الغلاف يجب أن يساوي الموقَّع",
            RemoteRejection.REVISION_MISMATCH,
            (admit(lying, floor = 4L) as Admission.Rejected).reason,
        )
    }

    @Test
    fun `a schema newer than this build is refused whole`() {
        val payload = TestConfigs.payloadJson(revision = 5, schema = RemoteConfigs.SCHEMA + 1)
        val envelope = TestConfigs.envelope(payload, server)
        assertEquals(
            "صيغة أحدث ⇒ رفض كامل، لا قراءة جزئية لما نفهمه",
            RemoteRejection.SCHEMA,
            (admit(envelope) as Admission.Rejected).reason,
        )
    }

    @Test
    fun `a payload that is not the contract is refused after the signature`() {
        val envelope = TestConfigs.envelope("not json at all", server, revision = 1L)
        assertEquals(
            "حمولة لا تُقرأ ⇒ MALFORMED لا SIGNATURE (التوقيع كان صالحًا)",
            RemoteRejection.MALFORMED,
            (admit(envelope) as Admission.Rejected).reason,
        )
    }

    @Test
    fun `an unset public key is a build gap, not a forged signature`() {
        val envelope = TestConfigs.envelope(TestConfigs.payloadJson(revision = 1), server)
        assertEquals(
            "بلا مفتاح: رمز إعداد ناقص لا رمز تلاعب",
            RemoteRejection.NOT_CONFIGURED,
            (admit(envelope, key = "") as Admission.Rejected).reason,
        )
    }

    @Test
    fun `a future issued_at is refused beyond the clock skew and tolerated inside it`() {
        val far = TestConfigs.payloadJson(revision = 2, issuedAt = TestConfigs.iso(TestConfigs.HOUR))
        assertEquals(
            "‏`issued_at` أمامنا بساعة ⇒ زمن مكسور",
            RemoteRejection.ISSUED_INVALID,
            (admit(TestConfigs.envelope(far, server)) as Admission.Rejected).reason,
        )
        val near = TestConfigs.payloadJson(revision = 2, issuedAt = TestConfigs.iso(5 * TestConfigs.MINUTE))
        assertTrue(
            "ساعة جهاز متأخّرة خمس دقائق ليست عطبًا",
            admit(TestConfigs.envelope(near, server)) is Admission.Admitted,
        )
    }

    @Test
    fun `an expiry that precedes issuance is refused`() {
        val payload = TestConfigs.payloadJson(
            revision = 3,
            issuedAt = TestConfigs.iso(-TestConfigs.HOUR),
            expiresAt = TestConfigs.iso(-2 * TestConfigs.HOUR),
        )
        assertEquals(
            "انتهاء قبل الإصدار ⇒ زمن مقلوب",
            RemoteRejection.ISSUED_INVALID,
            (admit(TestConfigs.envelope(payload, server)) as Admission.Rejected).reason,
        )
    }

    @Test
    fun `a key this build does not know is ignored and reported, never fatal`() {
        val payload = TestConfigs.payloadJson(
            revision = 7,
            features = """{"gamespace":{"enabled":false},"lab_x":{"enabled":true}}""",
        )
        val admitted = admit(TestConfigs.envelope(payload, server))
        assertTrue("مفتاح غريب لا يُسقط الإعداد", admitted is Admission.Admitted)
        val config = (admitted as Admission.Admitted).config
        assertEquals("المفاتيح الغريبة تُسمّى", listOf("lab_x"), config.unknownFeatureKeys)
        assertEquals(
            "والمعروف يُقرأ كما هو",
            false,
            config.features.getValue("gamespace").enabled,
        )
    }

    @Test
    fun `an announcement stops by itself when its window closes`() {
        val announcement = """{"enabled":true,"text_ar":"صيانة","text_en":"maintenance","until":"${TestConfigs.iso(2 * TestConfigs.HOUR)}"}"""
        val payload = TestConfigs.payloadJson(revision = 4, announcement = announcement)
        val admitted = admit(TestConfigs.envelope(payload, server)) as Admission.Admitted
        assertNotNull("قبل الموعد يُعرض", admitted.config.announcementAt(TestConfigs.NOW))
        assertTrue(
            "بعده يسقط من نفسه وإن نسي المالك إطفاءه",
            admitted.config.announcementAt(TestConfigs.NOW + 3 * TestConfigs.HOUR) == null,
        )
    }

    @Test
    fun `an enabled announcement with no text is not shown`() {
        val announcement = """{"enabled":true,"text_ar":"","text_en":""}"""
        val payload = TestConfigs.payloadJson(revision = 4, announcement = announcement)
        val admitted = admit(TestConfigs.envelope(payload, server)) as Admission.Admitted
        assertTrue(
            "مفتاح إعلان بلا نصّ = لا شيء يُعرض، فلا نافذة فارغة",
            admitted.config.announcementAt(TestConfigs.NOW) == null,
        )
    }

    @Test
    fun `the stats policy is clamped inside sane bounds`() {
        val payload = TestConfigs.payloadJson(
            revision = 6,
            stats = """{"collect":true,"sample_rate":900,"ttl_hours":9999}""",
        )
        val admitted = admit(TestConfigs.envelope(payload, server)) as Admission.Admitted
        val stats = admitted.config.stats
        assertNotNull(stats)
        assertEquals("نسبة خارج المعقول تُقيَّد", 100, stats!!.effectiveSampleRate)
        assertEquals("‏TTL خارج المعقول يُقيَّد", 24, stats.effectiveTtlHours)
    }
}
