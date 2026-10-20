/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.license

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * عطبٌ مصنَّف في مسار الترخيص — والاسم نفسه يُكتب في السجلّ. ولا يُدمج «لا شبكة» في «خادم
 * يرفض»: الأول يُعاد عليه بلا تدخّل، والثاني يعني مشروعًا متوقفًا أو رابطًا خاطئًا.
 */
enum class LicenseFault(val code: String) {
    /** لا `SUPABASE_URL` في هذا البناء — إعداد بناء ناقص، والشاشة تقولها صراحةً. */
    NOT_CONFIGURED("license_not_configured"),

    /** لا اتصال: لا شبكة، أو مضيف لا يُحلّ. */
    NETWORK("license_network"),

    /** وصلنا ومرّ الوقت. */
    TIMEOUT("license_timeout"),

    /** ‏4xx: طلب سيّئ أو دالّة غير منشورة (`404` يعني غالبًا `functions deploy` ناقص). */
    CLIENT("license_http_4xx"),

    /** ‏5xx: الخادم نفسه. */
    SERVER("license_http_5xx"),

    /** جسم لا يُقرأ كعقد الاستجابة (‏JSON مكسور، أو غلاف شهادة غائب مع `ok=true`). */
    FORMAT("license_malformed"),
}

/** عطبٌ مصنَّف: النوع يُترجَم إلى قرار ورمز، والرسالة التقنية للسجلّ لا للمستخدم. */
class LicenseFetchException(
    val fault: LicenseFault,
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

/**
 * نتيجة النداء: **مُنِح** (فالشهادة الموقّعة في اليد) أو **رُفض** بسببٍ مسمّى من الخادم
 * (`not_found` · `revoked` · `expired` · `device_limit` · …). ولا ثالث: عطب الشبكة **استثناء**
 * لا نتيجة، لأن ردّ فعل المستخدم عليه مختلف («جرّب بعد قليل» لا «مفتاحك مرفوض»).
 */
sealed interface LicenseCallOutcome {
    /** الشهادة كما وصلت — **ولا تُقرأ هنا**: تُمرَّر إلى `LicenseCertificates.admit` فورًا. */
    data class Granted(val envelope: LicenseEnvelope, val serverMillis: Long) : LicenseCallOutcome

    /**
     * رفضٌ مسمّى من الخادم — **ومعه زمن الخادم إن أرسله** (`serverMillis`).
     *
     * ولماذا الزمن في الرفض أيضًا: هو مرساة منع رجوع الساعة. جهازٌ انتهى مفتاحه ثم أرجع ساعته
     * ليُعيده لا يُنقذه الرجوع **بعد أول اتصال**: المرساة تُثبَّت على زمن الرفض نفسه فتصير بعد
     * لحظة الانتهاء (`LicenseStore.noteAttempt` → `LicenseStates.anchored`).
     */
    data class Denied(val code: String, val serverMillis: Long = 0L) : LicenseCallOutcome
}

/**
 * العميل الوحيد الذي يتكلّم مع دالّة `maxctl` — بلا SDK ولا تبعية، على نمط `ConfigClient` نفسه.
 *
 * ## قاعدتان لا تُخترقان
 *
 * * **`POST` إلى دالّة واحدة** بعقد `{action, ...}` — لا صفوف جداول، ولا مفاتيح خدمة في الحزمة.
 * * **المفتاح الخامّ لا يعبر الشبكة**: يُرسل `key_hash` (بصمة SHA-256) كما يُخزَّن على الخادم.
 *   فحتى اعتراض الاتصال (TLS مقشور على جهاز مروّت) لا يعطي المفتاح صالحًا للاستعمال مرة أخرى
 *   من طرف آخر — والبصمة وحدها لا تُفعّل شيئًا بلا الجهاز المسجَّل.
 */
class LicenseClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val userAgent: String = USER_AGENT,
    private val connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = DEFAULT_READ_TIMEOUT_MS,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /** تفعيل مفتاح لأول مرة (أو بعد فقده) — الخادم يقرّر: صالح؟ مقاعد؟ منتهٍ؟ */
    fun redeem(keyHash: String, device: String, label: String, appVersion: String): LicenseCallOutcome =
        call(
            action = "license.redeem",
            fields = mapOf(
                "key_hash" to keyHash,
                "device" to device,
                "label" to label,
                "app_version" to appVersion,
            ),
        )

    /** تجديد شهادة جهاز **مسجَّل** — يكشف الإلغاء ويبني الإيقاف المؤقّت المستجدّ. */
    fun validate(licenseId: String, device: String, appVersion: String): LicenseCallOutcome =
        call(
            action = "license.validate",
            fields = mapOf(
                "license_id" to licenseId,
                "device" to device,
                "app_version" to appVersion,
            ),
        )

    private fun call(action: String, fields: Map<String, String>): LicenseCallOutcome {
        val endpoint = endpoint() ?: throw LicenseFetchException(
            LicenseFault.NOT_CONFIGURED,
            "SUPABASE_URL is not set in this build",
        )
        val body = buildJsonObject {
            put("action", action)
            fields.forEach { (name, value) -> put(name, value) }
        }.toString()

        val connection = open(endpoint)
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code !in 200..299) {
                val fault = if (code >= 500) LicenseFault.SERVER else LicenseFault.CLIENT
                throw LicenseFetchException(fault, "HTTP $code")
            }
            val text = read(connection)
            val wire = runCatching { json.decodeFromString<Wire>(text) }.getOrNull()
                ?: throw LicenseFetchException(LicenseFault.FORMAT, "response is not the wire contract")

            // الزمن يُقرأ من الردّ **قبل** الحكم على `ok`: الرفض أيضًا يثبّت المرساة.
            val serverMillis = wire.serverTime?.let(LicenseTime::instantMillis)
                ?: LicenseTime.instantMillis(wire.issuedAt ?: "")
                ?: 0L

            if (!wire.ok) return LicenseCallOutcome.Denied(wire.error.ifBlank { "unknown" }, serverMillis)

            val envelope = wire.cert
                ?: throw LicenseFetchException(LicenseFault.FORMAT, "ok response without a certificate")
            return LicenseCallOutcome.Granted(envelope, if (serverMillis > 0) serverMillis else System.currentTimeMillis())
        } finally {
            connection.disconnect()
        }
    }

    private fun endpoint(): String? {
        val base = baseUrl.trim().trimEnd('/')
        if (base.isEmpty()) return null
        return "$base/functions/v1/$FUNCTION"
    }

    private fun open(endpoint: String): HttpURLConnection {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", userAgent)
        if (apiKey.isNotBlank()) {
            connection.setRequestProperty("apikey", apiKey)
            // الدالّة تُنشر بـ`--no-verify-jwt` (المفتاح العامّ ليس JWT في المشاريع الحديثة)،
            // و`Authorization` هنا للتوافق مع بوّابة Supabase إن فُعّلت لاحقًا — لا للتصديق.
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
        }
        return connection
    }

    private fun read(connection: HttpURLConnection): String {
        val stream = connection.inputStream
        return stream.use { input ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(8 * 1024)
            while (true) {
                val count = input.read(chunk)
                if (count < 0) break
                buffer.write(chunk, 0, count)
                if (buffer.size() > MAX_BODY_BYTES) {
                    throw LicenseFetchException(LicenseFault.FORMAT, "response over $MAX_BODY_BYTES bytes")
                }
            }
            buffer.toString(Charsets.UTF_8.name())
        }
    }

    /** عقد الاستجابة كما تكتبه دالّة `maxctl` بالحرف. */
    @Serializable
    private data class Wire(
        val ok: Boolean = false,
        val error: String = "",
        val cert: LicenseEnvelope? = null,
        @SerialName("server_time") val serverTime: String? = null,
        @SerialName("issued_at") val issuedAt: String? = null,
    )

    companion object {
        /** اسم دالّة Supabase — في `supabase/functions/maxctl/`. */
        const val FUNCTION = "maxctl"

        const val USER_AGENT = "MaxManager-License/1"

        const val DEFAULT_CONNECT_TIMEOUT_MS = 10_000
        const val DEFAULT_READ_TIMEOUT_MS = 20_000

        const val MAX_BODY_BYTES = 256 * 1024
    }
}
