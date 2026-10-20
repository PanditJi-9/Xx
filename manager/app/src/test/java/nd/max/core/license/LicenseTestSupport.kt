/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.license

import nd.max.core.remote.TestKeys
import java.time.Instant
import java.util.Base64

/**
 * أدوات اختبار الترخيص — **بمفاتيح P-256 حقيقية لا بدائل**، على نمط `TestKeys` نفسه.
 *
 * والدعوى الوحيدة التي تستحقّ القياس هنا هي الفرق بين شهادةٍ وقّعها الخادم وأخرى وقّعها مهاجم:
 * فلا فرق بين ما يُقاس هنا وما يعمل على الجهاز — نفس `SHA256withECDSA` ونفس المنحنى.
 */
object LicenseTestSupport {

    const val DEVICE = "0123456789abcdef0123456789abcdef"
    const val OTHER_DEVICE = "fedcba9876543210fedcba9876543210"
    const val LICENSE_ID = "3f1e6f2a-9a4e-4b1c-8f0e-5d2c7a1b4e6f"
    const val NOW = 1_769_000_000_000L
    const val DAY = 24L * 60 * 60 * 1000

    fun iso(millis: Long): String = Instant.ofEpochMilli(millis).toString()

    /** حمولة الشهادة كما يبنيها الخادم بالحرف — حقلًا حقلًا وبالترتيب نفسه. */
    fun payload(
        licenseId: String = LICENSE_ID,
        device: String = DEVICE,
        plan: String = "pro",
        state: String = "active",
        kind: String = "license",
        schema: Int = 1,
        issuedAt: String = iso(NOW - 60_000),
        serverTime: String = iso(NOW - 60_000),
        startsAt: String = iso(NOW - 3_600_000),
        expiresAt: String = iso(NOW + 30L * DAY),
        pausedMs: Long = 0,
        deviceLimit: Int = 1,
        devicesUsed: Int = 1,
    ): String = buildString {
        append("""{"schema":$schema,"kind":"$kind","license_id":"$licenseId","device":"$device"""")
        append(""","plan":"$plan","state":"$state"""")
        append(""","issued_at":"$issuedAt","server_time":"$serverTime"""")
        append(""","starts_at":"$startsAt","expires_at":"$expiresAt"""")
        append(""","paused_ms":$pausedMs,"device_limit":$deviceLimit,"devices_used":$devicesUsed}""")
    }

    /** غلافٌ موقّع **بالفعل** — وما يُوقَّع هو البايتات المعطاة بالضبط (لا إعادة تسلسل). */
    fun envelope(
        payload: String,
        key: TestKeys.Key,
        license: String = LICENSE_ID,
        signature: String? = null,
        payloadOverride: ByteArray? = null,
    ): LicenseEnvelope {
        val bytes = payloadOverride ?: payload.toByteArray(Charsets.UTF_8)
        return LicenseEnvelope(
            schema = 1,
            license = license,
            payload = Base64.getEncoder().encodeToString(bytes),
            signature = signature ?: TestKeys.sign(bytes, key.private),
        )
    }

    /** سجلّ مقبول من غلافٍ صحيح — للمصفوفات التي تحتاج حالةً سارية. */
    fun record(
        payload: String,
        key: TestKeys.Key,
        device: String = DEVICE,
        nowMillis: Long = NOW,
    ): LicenseRecord {
        val envelope = envelope(payload, key)
        val accepted = LicenseCertificates.admit(envelope, key.publicBase64, device, nowMillis)
            as LicenseAdmission.Accepted
        return LicenseStates.recordOf(accepted.payload, envelope, nowMillis)
    }

    /**
     * مُشفِّر اختبار: XOR بمفتاح ثابت — قابل للعكس، ويُثبت أن الملفّ **ليس نصًّا صريحًا**،
     * وأن قراءته بمفتاح آخر تفشل كما تفشل شهادةٌ نُسخت إلى جهاز آخر.
     */
    class XorCipher(private val key: Int = 0x5A) : LicenseCipher {
        override val id: String = "test-xor"
        override fun seal(plain: ByteArray): ByteArray = ByteArray(plain.size) { (plain[it].toInt() xor key).toByte() }
        override fun open(sealed: ByteArray): ByteArray = seal(sealed)
    }
}
