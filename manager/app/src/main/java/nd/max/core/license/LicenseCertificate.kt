/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.license

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nd.max.core.security.P256Signatures

/**
 * شهادة الترخيص — **حمولةٌ موقّعة تُثبت: هذا المفتاح لهذا الجهاز إلى هذا الوقت**، ويُتحقّق منها
 * على الجهاز بلا اتصال. وهي نفس فكرة غلاف الإعداد البعيد حرفيًّا (بايتات موقّعة لا كائن JSON
 * يُعاد تسلسله)، ومفتاح واحد يوقّع القناتين.
 *
 * ## ماذا تكسب بـ«التحقّق بلا اتصال» — وما لا تكسبه
 *
 * تكسب: جهازًا بلا شبكة يعمل بمفتاحه كامل مدّته، بلا صفٍّ واحد في قاعدة البيانات عند كل فتح
 * (وهذا فرقٌ حقيقي في فاتورة Supabase المجّانية). ولا تكسب «استحالة الكسر»: صاحب الجهاز المروّت
 * يستطيع تعديل الحزمة — ولهذا فالمانع الفعليّ **الإلغاءُ عند التجديد + حدّ الأجهزة على الخادم**،
 * والبيان الصادق كاملٌ في `docs/ai/LICENSE-KEYS-01-spec.md` §5.
 *
 * ## وترتيب الفحص — وأول فشل يُنهي (نفس ترتيب `RemoteConfigs.admit`)
 *
 * ١ المفتاح العامّ موجود → ٢ التوقيع على البايتات **قبل أي قراءة** → ٣ الصيغة تُقرأ →
 * ٤ `schema` ≤ المدعوم → ٥ النوع `kind = license` → ٦ الهوية والجهاز (الشهادة **لهذا الجهاز**؛
 * نسخةٌ منسوخة إلى جهاز آخر تُرفض باسمها `BOUND_OTHER_DEVICE` لا كعطب غامض) → ٧ الأزمنة.
 */

/** الغلاف المعروض على السلك: أربعة حقول، ولا خامس. */
@Serializable
data class LicenseEnvelope(
    val schema: Int = 0,
    val license: String = "",
    val payload: String = "",
    val signature: String = "",
)

/** الحمولة الموقّعة نفسها — تُقرأ من **بايتاتها** لا من إعادة تسلسل. */
@Serializable
data class LicensePayload(
    val schema: Int = 0,
    val kind: String = "",
    @SerialName("license_id") val licenseId: String = "",
    val device: String = "",
    val plan: String = "",
    val state: String = "",
    @SerialName("issued_at") val issuedAt: String = "",
    @SerialName("server_time") val serverTime: String = "",
    @SerialName("starts_at") val startsAt: String = "",
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("paused_ms") val pausedMs: Long = 0,
    @SerialName("device_limit") val deviceLimit: Int = 0,
    @SerialName("devices_used") val devicesUsed: Int = 0,
)

/** أسباب الرفض — كلٌّ باسمه، فلا يُدمج تلاعبٌ في «فشل». والاسم يُكتب في السجلّ كما هو. */
enum class LicenseRejection(val code: String) {
    /** لا مفتاح عامّ في هذا البناء: لا توقيع يمكن التحقّق منه. */
    NOT_CONFIGURED("license_not_configured"),

    /** غلاف أو حمولة لا تُقرأ (base64 مكسور، JSON مقطوع، حمولة فارغة أو متضخّمة). */
    MALFORMED("license_malformed"),

    /** التوقيع باطل: حمولة مبدَّلة، أو مفتاح مزوّر، أو شهادة ليست من الخادم. */
    SIGNATURE("license_signature"),

    /** صيغة أحدث من التي يفهمها هذا الإصدار. */
    SCHEMA("license_schema"),

    /** ليس شهادة ترخيص (`kind` آخر). */
    KIND("license_kind"),

    /** الشهادة **لجهاز آخر**: نسخها من جهاز إلى آخر لا يُفعّلها. */
    BOUND_OTHER_DEVICE("license_other_device"),

    /** هوية المفتاح غائبة أو مشوّهة. */
    LICENSE_ID("license_id_invalid"),

    /** `issued_at`/`server_time` مستقبليّان بفارق غير معقول، أو خارج أي عمر ممكن. */
    ISSUED_INVALID("license_issued_invalid"),

    /** `expires_at` لا يُقرأ، أو ليس لاحقًا للإصدار. */
    EXPIRES_INVALID("license_expires_invalid"),

    /** حالة غير `active` موقّعة — عقد لا يعرفه هذا الإصدار. */
    STATE("license_state_unknown"),
}

/** نتيجة القبول: شهادةٌ صالحة البناء، أو رفضٌ بسببه. ولا ثالث. */
sealed interface LicenseAdmission {
    /** الشهادة سليمة البناء ولهذا الجهاز — والانتهاء يُحكم عليه في [LicenseStates] لا هنا. */
    data class Accepted(val payload: LicensePayload, val serverMillis: Long) : LicenseAdmission

    data class Rejected(val reason: LicenseRejection, val detail: String) : LicenseAdmission
}

object LicenseCertificates {

    /** أعلى صيغة يفهمها هذا الكود. */
    const val SCHEMA = 1

    /** سقف حمولة الشهادة — شهادة، لا ملفّ. */
    const val MAX_PAYLOAD_BYTES = 8 * 1024

    /** فرق الساعات المسموح به — ساعة جهاز متأخّرة دقائق ليست عطبًا. */
    const val CLOCK_SKEW_MILLIS = 15 * 60 * 1000L

    /** أقصى عمر معقول للإصدار: ثلاثون سنة (المدّة القصوى للمفتاح عشر سنوات + هامش الإيقاف). */
    const val MAX_ISSUED_AGE_MILLIS = 30L * 365 * 24 * 60 * 60 * 1000

    /** أقصى مدى ممكن `issued→expires`: أربعون سنة — حرسُ زمنٍ مكسور لا حرسُ ميزة. */
    const val MAX_VALIDITY_SPAN_MILLIS = 40L * 365 * 24 * 60 * 60 * 1000

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        explicitNulls = false
    }

    fun parseEnvelope(raw: String): LicenseEnvelope? =
        runCatching { json.decodeFromString<LicenseEnvelope>(raw) }.getOrNull()

    /**
     * الحكم الكامل على غلاف شهادة — يُنادى في **كل إقلاع** على ما في الذاكرة، فشهادة سطح
     * المكتب تُعدَّل بجذر ليست شهادة: البايتات الموقّعة هي التي تُقرأ، لا الحقائق المخزَّنة.
     */
    fun admit(
        envelope: LicenseEnvelope,
        publicKeyBase64: String,
        deviceId: String,
        nowMillis: Long,
    ): LicenseAdmission {
        val key = P256Signatures.decodePublicKey(publicKeyBase64)
            ?: return LicenseAdmission.Rejected(
                LicenseRejection.NOT_CONFIGURED,
                if (publicKeyBase64.isBlank()) "no public key in this build" else "public key is not an EC P-256 SPKI",
            )

        // ① التوقيع على البايتات بالضبط — قبل أي قراءة.
        val payloadBytes = P256Signatures.decodeBase64(envelope.payload)
            ?: return LicenseAdmission.Rejected(LicenseRejection.MALFORMED, "payload is not base64")
        if (payloadBytes.isEmpty()) {
            return LicenseAdmission.Rejected(LicenseRejection.MALFORMED, "empty payload")
        }
        if (payloadBytes.size > MAX_PAYLOAD_BYTES) {
            return LicenseAdmission.Rejected(LicenseRejection.MALFORMED, "payload over $MAX_PAYLOAD_BYTES bytes")
        }
        if (!P256Signatures.verify(payloadBytes, envelope.signature, key)) {
            return LicenseAdmission.Rejected(LicenseRejection.SIGNATURE, "ECDSA P-256 verification failed")
        }

        // ② الصيغة تُقرأ بعد أن ثبت أن البايتات موقّعة.
        val payload = runCatching {
            json.decodeFromString<LicensePayload>(payloadBytes.toString(Charsets.UTF_8))
        }.getOrNull() ?: return LicenseAdmission.Rejected(LicenseRejection.MALFORMED, "payload is not the license contract")

        if (payload.schema > SCHEMA) {
            return LicenseAdmission.Rejected(LicenseRejection.SCHEMA, "schema ${payload.schema} > $SCHEMA")
        }
        if (payload.kind != "license") {
            return LicenseAdmission.Rejected(LicenseRejection.KIND, "kind is not license")
        }
        if (payload.licenseId.isBlank() || payload.licenseId != envelope.license) {
            return LicenseAdmission.Rejected(LicenseRejection.LICENSE_ID, "envelope id != signed id")
        }
        if (payload.device != deviceId) {
            return LicenseAdmission.Rejected(LicenseRejection.BOUND_OTHER_DEVICE, "certificate belongs to another device")
        }
        if (payload.state != "active") {
            return LicenseAdmission.Rejected(LicenseRejection.STATE, "state is not active")
        }

        // ③ الزمن: لا مستقبليّ بفارق معقول، ولا خارج أي عمر ممكن.
        val issuedAt = LicenseTime.instantMillis(payload.issuedAt)
            ?: return LicenseAdmission.Rejected(LicenseRejection.ISSUED_INVALID, "issued_at is not ISO-8601")
        if (issuedAt > nowMillis + CLOCK_SKEW_MILLIS) {
            return LicenseAdmission.Rejected(
                LicenseRejection.ISSUED_INVALID,
                "issued_at is ${(issuedAt - nowMillis) / 1000}s in the future",
            )
        }
        val serverAt = if (payload.serverTime.isBlank()) issuedAt else {
            LicenseTime.instantMillis(payload.serverTime)
                ?: return LicenseAdmission.Rejected(LicenseRejection.ISSUED_INVALID, "server_time is not ISO-8601")
        }
        val trustedServer = maxOf(serverAt, issuedAt)
        if (nowMillis - issuedAt > MAX_ISSUED_AGE_MILLIS) {
            return LicenseAdmission.Rejected(LicenseRejection.ISSUED_INVALID, "issued_at is over thirty years old")
        }
        val expiresAt = LicenseTime.instantMillis(payload.expiresAt)
            ?: return LicenseAdmission.Rejected(LicenseRejection.EXPIRES_INVALID, "expires_at is not ISO-8601")
        if (expiresAt <= issuedAt) {
            return LicenseAdmission.Rejected(LicenseRejection.EXPIRES_INVALID, "expires_at is not after issued_at")
        }
        if (expiresAt - issuedAt > MAX_VALIDITY_SPAN_MILLIS) {
            return LicenseAdmission.Rejected(LicenseRejection.EXPIRES_INVALID, "validity span is over forty years")
        }

        return LicenseAdmission.Accepted(payload, trustedServer)
    }
}

/** قراءة ISO-8601 إلى مللي ثانية — مصدر واحد لتحويل الزمن في هذه الحزمة. */
internal object LicenseTime {
    fun instantMillis(text: String): Long? =
        runCatching { java.time.Instant.parse(text).toEpochMilli() }.getOrNull()
}
