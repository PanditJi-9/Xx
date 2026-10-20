/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/** حكم طلب الإشارة: وصل، أو فشل بعطبٍ **مصنَّف بالاسم** (ولا كلمة «فشل» واحدة). */
sealed interface SignalOutcome {
    data object Sent : SignalOutcome
    data class Failed(val fault: RemoteFault) : SignalOutcome
}

/**
 * **الطرف الشبكيّ لقناة الإحصاءات** — دالتان لا ثالثة لهما، وثلاثة رؤوس لا أكثر.
 *
 * ## القواعد المُلزمة هنا (‏`MAX-CONTROL-01` §١٠)
 *
 * * **دالّتان فقط**: `track_install` (إشارة يوميّة) و`forget_install` (سحب الموافقة). ولا
 *   مسار يقرأ صفًّا ولا يكتب في جدول مباشرةً — الكتابة تمرّ بالدالّتين الحارستين، وفيهما
 *   الفحص الخادميّ الذي لا يُتجاوز من عميل (§٤.٢ قاعدة ٧).
 * * **لا يرمي استثناءً أبدًا**: كلّ عطبٍ يُعاد حكمًا مصنَّفًا [`SignalOutcome.Failed`] —
 *   فقناةٌ ثانويّة لا يجوز أن تُسقط نداءً من أجله (نمط `React` في الواجهة لا `throw`).
 * * **زمنان محدودان** كما في [ConfigClient]: ١٠ ثوانٍ اتصالًا و٢٠ قراءةً.
 * * **جسم الطلب يُبنى في [StatsRules] وحدها** — العارض والمرسل بايتات واحدة (§١٠.٣).
 */
class TrackClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val userAgent: String = USER_AGENT,
    private val connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = DEFAULT_READ_TIMEOUT_MS,
) {

    /** إشارة التثبيت — جسمها من [StatsRules.trackPayload]. */
    fun track(body: String): SignalOutcome = post(TRACK_PATH, body)

    /** طلب السحب — جسمه من [StatsRules.forgetPayload]. */
    fun forget(body: String): SignalOutcome = post(FORGET_PATH, body)

    private fun post(path: String, body: String): SignalOutcome {
        val endpoint = endpoint(path) ?: return SignalOutcome.Failed(RemoteFault.NOT_CONFIGURED)
        val connection = try {
            URL(endpoint).openConnection() as HttpURLConnection
        } catch (error: MalformedURLException) {
            return SignalOutcome.Failed(RemoteFault.NOT_CONFIGURED)
        } catch (error: IOException) {
            return SignalOutcome.Failed(RemoteFault.NETWORK)
        }
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", userAgent)
            if (apiKey.isNotBlank()) {
                connection.setRequestProperty("apikey", apiKey)
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
            }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            return if (code in 200..299) SignalOutcome.Sent else SignalOutcome.Failed(classify(code))
        } catch (timeout: SocketTimeoutException) {
            return SignalOutcome.Failed(RemoteFault.TIMEOUT)
        } catch (dns: UnknownHostException) {
            return SignalOutcome.Failed(RemoteFault.NETWORK)
        } catch (error: IOException) {
            return SignalOutcome.Failed(RemoteFault.NETWORK)
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

    private fun endpoint(path: String): String? {
        val base = baseUrl.trim().trimEnd('/')
        if (base.isEmpty()) return null
        return base + path
    }

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MS = 10_000
        const val DEFAULT_READ_TIMEOUT_MS = 20_000

        const val USER_AGENT = "MaxManager-Stats"

        /**
         * مسارا الدالّتين في PostgREST — **وهما عقدُ المخطّط نفسه**:
         * `supabase/schema.sql` يعرّف `track_install` و`forget_install` ويمنح `anon` تنفيذهما.
         * ويحرس التطابقَ اختبارُ العقد `MaxControlProtocolContractTest` من المصدرين معًا.
         */
        const val TRACK_PATH = "/rest/v1/rpc/track_install"
        const val FORGET_PATH = "/rest/v1/rpc/forget_install"
    }
}
