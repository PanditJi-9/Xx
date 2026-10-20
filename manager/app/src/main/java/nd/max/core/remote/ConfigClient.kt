/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.remote

import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * عطبٌ مصنَّف في مسار الإعداد — **والاسم نفسه يُكتب في السجلّ**. ولا يُدمج نوعان في «فشل»:
 * «الخادم موقوف» و«لا شبكة» و«التوقيع باطل» ثلاثة أحكام مختلفة، ولو صارت كلمة واحدة لصارت
 * التشخيص الذي يطلبه المالك مستحيلًا.
 */
enum class RemoteFault(val code: String) {
    /** لا `SUPABASE_URL` في هذا البناء — إعداد بناء ناقص، والافتراضي المحلي هو الحكم. */
    NOT_CONFIGURED("remote_not_configured"),

    /** لا اتصال: لا شبكة، أو مضيف لا يُحلّ. */
    NETWORK("remote_network"),

    /** وصلنا ومرّ الوقت — خادم بطيء أو شبكة متقطّعة. */
    TIMEOUT("remote_timeout"),

    /** ‏401: مفتاح النشر خطأ أو غاب. */
    UNAUTHORIZED("remote_http_401"),

    /** ‏403: سياسة RLS تمنع القراءة. */
    FORBIDDEN("remote_http_403"),

    /** ‏404: العرض/الجدول غير منشور. */
    NOT_FOUND("remote_http_404"),

    /** ‏5xx: الخادم نفسه، أو مشروع مجّاني في وقفة سكون (§٤.٣). */
    SERVER("remote_http_5xx"),

    /** ‏4xx أخرى. */
    CLIENT("remote_http_4xx"),

    /** الجسم أكبر من سقف العقد — رابط خاطئ أو ملفّ ليس عقدًا. */
    TOO_LARGE("remote_too_large"),

    /** وصل شيء لا يُقرأ كغلاف (صفٌّ فارغ، JSON مكسور، حقول غائبة). */
    FORMAT("remote_malformed"),
}

/** عطبٌ مصنَّف: النوع يُترجَم إلى قرار ورمز، والرسالة التقنية للسجلّ لا للمستخدم. */
class RemoteFetchException(
    val fault: RemoteFault,
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

/** نتيجة الطلب: تغيّر شيء (فمعنا غلافٌ جديد)، أو لم يتغيّر (‏304 ⇒ **صفر egress**، §٤.٢ قاعدة ٢). */
sealed interface FetchOutcome {
    data class Changed(val envelope: SignedEnvelope, val etag: String?) : FetchOutcome
    data object Unchanged : FetchOutcome
}

/**
 * الطلب الواحد للإعداد الموقّع — بلا SDK ولا تبعية، على نمط `UpdateTransport` نفسه.
 *
 * ## القواعد المُلزمة هنا (‏§٤.٢ من `MAX-CONTROL-01`)
 *
 * * **قراءة واحدة** لكل تشغيل: لا قراءة لكل شاشة ولا لكل تنقّل. وهذا الطلب الوحيد.
 * * **`If-None-Match`**: إن كان الإصدار المخزَّن يحمل ETag، فالخادم يردّ ‏`304` بلا جسم —
 *   وذلك **صفر egress**، وهو الفرق بين خطّة تكفي آلافًا وأخرى تنفد بأول ألفين.
 * * **`GET` فقط، ولا كتابة من التطبيق إطلاقًا**: كل الكتابات الإدارية من اللوحة وحدها، وRLS
 *   يمنع غيره أصلًا. ولا مسار في هذا الملفّ يرسل جسمًا إلى الخادم — بالبناء لا بالفحص.
 * * **مهلة لا انتظار أبديّ**: ١٠ ثوانٍ اتصالًا و٢٠ قراءةً. تطبيق يُفتح ويجد معلّقًا على خادم
 *   لا يردّ أسوأ من تطبيق يعمل من الذاكرة.
 */
class ConfigClient(
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

    /**
     * يقرأ العرض المنشور: صفٌّ واحد فيه `payload` و`signature` و`schema` و`revision`.
     * و`etag` السابق (إن وُجد) يمنع تنزيل جسمٍ لا حاجة له.
     */
    fun fetch(etag: String? = null): FetchOutcome {
        val endpoint = endpoint() ?: throw RemoteFetchException(
            RemoteFault.NOT_CONFIGURED,
            "SUPABASE_URL is not set in this build",
        )
        val connection = open(endpoint, etag)
        try {
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_NOT_MODIFIED) return FetchOutcome.Unchanged
            if (code !in 200..299) throw RemoteFetchException(classify(code), "HTTP $code")

            val body = read(connection)
            val rows = runCatching { json.decodeFromString<List<SignedEnvelope>>(body) }.getOrNull()
                ?: throw RemoteFetchException(RemoteFault.FORMAT, "response is not a config row array")
            val envelope = rows.firstOrNull()
                ?: throw RemoteFetchException(RemoteFault.FORMAT, "the published config row is missing")
            if (envelope.payload.isBlank() || envelope.signature.isBlank()) {
                throw RemoteFetchException(RemoteFault.FORMAT, "payload or signature is empty")
            }
            return FetchOutcome.Changed(envelope, connection.getHeaderField("ETag"))
        } catch (fault: RemoteFetchException) {
            throw fault
        } catch (timeout: SocketTimeoutException) {
            throw RemoteFetchException(RemoteFault.TIMEOUT, "read timed out after ${readTimeoutMs}ms", timeout)
        } catch (dns: UnknownHostException) {
            throw RemoteFetchException(RemoteFault.NETWORK, "host not resolved", dns)
        } catch (error: IOException) {
            throw RemoteFetchException(RemoteFault.NETWORK, "transport failed: ${error.message}", error)
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    private fun classify(code: Int): RemoteFault = when {
        code == HttpURLConnection.HTTP_UNAUTHORIZED -> RemoteFault.UNAUTHORIZED
        code == HttpURLConnection.HTTP_FORBIDDEN -> RemoteFault.FORBIDDEN
        code == HttpURLConnection.HTTP_NOT_FOUND -> RemoteFault.NOT_FOUND
        code >= 500 -> RemoteFault.SERVER
        else -> RemoteFault.CLIENT
    }

    private fun endpoint(): String? {
        if (baseUrl.isBlank()) return null
        return baseUrl.trimEnd('/') + PUBLISHED_VIEW
    }

    /** قراءة بحدٍّ أعلى — والحدّ يُقاس أثناء القراءة لا من ترويسة قد تكذب. */
    private fun read(connection: HttpURLConnection): String {
        val declared = connection.contentLengthLong
        if (declared > MAX_BODY_BYTES) {
            throw RemoteFetchException(RemoteFault.TOO_LARGE, "declared $declared bytes")
        }
        val sink = ByteArrayOutputStream(INITIAL_BUFFER_BYTES)
        val buffer = ByteArray(READ_BUFFER_BYTES)
        connection.inputStream.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                sink.write(buffer, 0, read)
                if (sink.size() > MAX_BODY_BYTES) {
                    throw RemoteFetchException(RemoteFault.TOO_LARGE, "over $MAX_BODY_BYTES bytes")
                }
            }
        }
        return sink.toString(Charsets.UTF_8.name())
    }

    private fun open(endpoint: String, etag: String?): HttpURLConnection {
        val url = try {
            URL(endpoint)
        } catch (error: MalformedURLException) {
            throw RemoteFetchException(RemoteFault.NOT_CONFIGURED, "bad url: $endpoint", error)
        }
        return try {
            (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                instanceFollowRedirects = true
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", userAgent)
                if (!etag.isNullOrBlank()) setRequestProperty("If-None-Match", etag)
            }
        } catch (error: IOException) {
            throw RemoteFetchException(RemoteFault.NETWORK, "connect failed: ${error.message}", error)
        }
    }

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MS = 10_000
        const val DEFAULT_READ_TIMEOUT_MS = 20_000

        /** سقف الجسم: العقد صغير، والـ`payload` نفسه محدود بـ64 كيلوبايت في [RemoteConfigs]. */
        const val MAX_BODY_BYTES = 256 * 1024

        const val USER_AGENT = "MaxManager-Config"

        /**
         * العرض المنشور — صفٌّ واحد من `app_config` مع توقيعه. وعرضٌ لا دالّة RPC عن قصد:
         * القراءة العامّة إعداد موجود أصلًا في المخطّط (‏`create policy "config is public read"`)،
         * ولا تُضاف صلاحية جديدة لأي جهة (§١٣ البند ١٠).
         */
        const val PUBLISHED_VIEW =
            "/rest/v1/config_signed?select=schema,revision,payload,signature&limit=1"

        private const val READ_BUFFER_BYTES = 16 * 1024
        private const val INITIAL_BUFFER_BYTES = 4 * 1024
    }
}
