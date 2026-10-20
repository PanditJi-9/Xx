package com.maxmanager.control

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/** Every failure the UI can see. [authExpired] means the owner must sign in again. */
class ApiException(message: String, val code: Int = 0, val authExpired: Boolean = false) : Exception(message)

class Session(var accessToken: String, var refreshToken: String)

data class CountryStat(val code: String, val total: Long, val active: Long)

/** صفّ توافق (§٦.٤) — شركة/طراز/إصدار/بيئة بعددها، بلا معرّف تثبيت. */
data class CompatStat(
    val manufacturer: String,
    val model: String,
    val sdk: Int,
    val env: String,
    val total: Long,
    val active: Long,
)

/** صفّ صحّة تحكّم (§٦.٣) — أعداد مجمَّعة لكل مجال: أُثبت · ارتدّ · مجهول. */
data class HealthStat(
    val domain: String,
    val confirmed: Long,
    val rolledBack: Long,
    val unknown: Long,
    val installs: Long,
)

/** صفّ سجلّ التدقيق — المراجعة ومَن نشرها وأيّ مفاتيح تغيّرت (لا diff كامل). */
data class AuditRow(
    val revision: Long,
    val actor: String,
    val at: String,
    val changed: List<String>,
)

object Api {
    private const val SESSION_EXPIRED = "انتهت الجلسة، سجّل الدخول من جديد"

    private fun request(method: String, path: String, body: String? = null, token: String? = null): String {
        val conn = URL(Config.SUPABASE_URL + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("apikey", Config.SUPABASE_ANON_KEY)
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Cache-Control", "no-cache")
            if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw ApiException(describe(code, text, path), code)
            return text
        } catch (e: ApiException) {
            throw e
        } catch (e: UnknownHostException) {
            throw ApiException("لا يوجد اتصال بالإنترنت، أو عنوان Supabase غير صحيح")
        } catch (e: SocketTimeoutException) {
            throw ApiException("انتهت مهلة الاتصال، حاول مرة أخرى")
        } catch (e: IOException) {
            throw ApiException("تعذّر الاتصال بالخادم")
        } finally {
            conn.disconnect()
        }
    }

    private fun describe(code: Int, text: String, path: String): String {
        val json = runCatching { JSONObject(text) }.getOrNull()
        val raw = json
            ?.let { j -> listOf("msg", "message", "error_description").map { j.optString(it) }.firstOrNull { it.isNotEmpty() } }
            ?: text
        val errCode = json?.optString("error_code").orEmpty()
        // `error` هو رمز دالّة `maxctl` (‏`unauthorized` · `forbidden` · `bad_request`…) — وهي
        // تردّ بأجسام لا بأكواد Supabase، فبلا هذا السطر يرى المالك «خطأ 401: …» بدل السبب.
        val fnError = json?.optString("error").orEmpty()
        val fnMessage = json?.optString("message").orEmpty()
        return when {
            errCode == "invalid_credentials" || raw.contains("Invalid login credentials", true) ->
                "البريد الإلكتروني أو كلمة المرور غير صحيحة"
            errCode == "email_not_confirmed" ->
                "البريد غير مؤكَّد. فعّل «Auto Confirm User» للمستخدم في Supabase"
            raw.contains("Invalid API key", true) ->
                "مفتاح Supabase غير صحيح، راجع SUPABASE_ANON_KEY"
            fnError == "unauthorized" ->
                "الجلسة غير صالحة. سجّل الدخول من جديد"
            fnError == "forbidden" || raw.equals("forbidden", true) ->
                "هذا الحساب ليس مشرفًا. أضِفه إلى جدول admins في Supabase"
            fnError == "not_found" -> "المفتاح غير موجود"
            code == 429 -> "محاولات كثيرة، انتظر قليلًا ثم أعد المحاولة"
            raw.contains("LICENSE_SIGNING_KEY_PKCS8", true) ->
                "سرّ التوقيع LICENSE_SIGNING_KEY_PKCS8 غير مضبوط في Supabase. راجع supabase/functions/maxctl/README.md"
            code == 404 && path.startsWith("/functions/v1/") ->
                "دالّة maxctl غير منشورة في مشروع Supabase هذا. انشرها بأمر: supabase functions deploy maxctl --no-verify-jwt"
            code == 404 ->
                "الجدول أو الدالة غير موجود في قاعدة البيانات. نفّذ ملف supabase/schema.sql كاملًا في SQL Editor"
            fnError.isNotEmpty() -> "رفض الخادم الطلب ($fnError)${if (fnMessage.isNotEmpty()) ": $fnMessage" else ""}"
            else -> "خطأ $code: ${raw.take(160)}"
        }
    }

    private fun parseSession(text: String): Session = try {
        val j = JSONObject(text)
        Session(j.getString("access_token"), j.getString("refresh_token"))
    } catch (e: JSONException) {
        throw ApiException("استجابة غير متوقعة من الخادم")
    }

    fun signIn(email: String, password: String): Session {
        val body = JSONObject().put("email", email).put("password", password).toString()
        return parseSession(request("POST", "/auth/v1/token?grant_type=password", body))
    }

    /** Best effort: revokes the refresh token on the server. */
    fun signOut(s: Session) {
        request("POST", "/auth/v1/logout", "{}", s.accessToken)
    }

    private fun refresh(s: Session) {
        val fresh = try {
            parseSession(
                request("POST", "/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", s.refreshToken).toString())
            )
        } catch (e: ApiException) {
            if (e.code in 400..499) throw ApiException(SESSION_EXPIRED, e.code, authExpired = true)
            throw e
        }
        s.accessToken = fresh.accessToken
        s.refreshToken = fresh.refreshToken
    }

    /** Runs [block] with the access token; on 401 refreshes once and retries. */
    private fun <T> authed(s: Session, block: (String) -> T): T {
        try {
            return block(s.accessToken)
        } catch (e: ApiException) {
            if (e.code != 401) throw e
        }
        refresh(s)
        try {
            return block(s.accessToken)
        } catch (e: ApiException) {
            if (e.code == 401) throw ApiException(SESSION_EXPIRED, 401, authExpired = true)
            throw e
        }
    }

    fun loadConfig(): JSONObject {
        val text = request("GET", "/rest/v1/app_config?key=eq.main&select=value")
        try {
            val rows = JSONArray(text)
            if (rows.length() == 0) throw ApiException("لا يوجد إعداد main في app_config. نفّذ supabase/schema.sql")
            return rows.getJSONObject(0).getJSONObject("value")
        } catch (e: JSONException) {
            throw ApiException("بنية الإعداد المخزَّن غير صالحة")
        }
    }

    fun countryStats(s: Session): List<CountryStat> {
        val text = authed(s) { request("POST", "/rest/v1/rpc/admin_country_stats", "{}", it) }
        try {
            val rows = JSONArray(text)
            return (0 until rows.length()).map {
                val r = rows.getJSONObject(it)
                CountryStat(r.optString("country_code", "XX"), r.optLong("total_count"), r.optLong("active_count"))
            }
        } catch (e: JSONException) {
            throw ApiException("استجابة الإحصاءات غير صالحة")
        }
    }

    /** توزيع التوافق (§٦.٤) — دالّة `admin_compat_stats` على الخادم، وهي التي تجمّع. */
    fun compatStats(s: Session): List<CompatStat> {
        val text = authed(s) { request("POST", "/rest/v1/rpc/admin_compat_stats", "{}", it) }
        try {
            val rows = JSONArray(text)
            return (0 until rows.length()).map {
                val r = rows.getJSONObject(it)
                CompatStat(
                    manufacturer = r.optString("manufacturer"),
                    model = r.optString("model"),
                    sdk = r.optInt("android_sdk"),
                    env = r.optString("control_env", "none"),
                    total = r.optLong("total_count"),
                    active = r.optLong("active_count"),
                )
            }
        } catch (e: JSONException) {
            throw ApiException("استجابة التوافق غير صالحة")
        }
    }

    /** صحّة التحكّم (§٦.٣) — مجموع عدّادات `health` لكل مجال عبر التثبيتات. */
    fun healthStats(s: Session): List<HealthStat> {
        val text = authed(s) { request("POST", "/rest/v1/rpc/admin_health_stats", "{}", it) }
        try {
            val rows = JSONArray(text)
            return (0 until rows.length()).map {
                val r = rows.getJSONObject(it)
                HealthStat(
                    domain = r.optString("domain"),
                    confirmed = r.optLong("confirmed"),
                    rolledBack = r.optLong("rolled_back"),
                    unknown = r.optLong("unknown_count"),
                    installs = r.optLong("installs"),
                )
            }
        } catch (e: JSONException) {
            throw ApiException("استجابة صحّة التحكّم غير صالحة")
        }
    }

    /** آخر نشرات الإعداد (§١٣ بند ٢/٧) — بلا القيم الكاملة، فقط ما تغيّر. */
    fun auditPage(s: Session): List<AuditRow> {
        val text = authed(s) { request("POST", "/rest/v1/rpc/admin_audit_page", "{}", it) }
        try {
            val rows = JSONArray(text)
            return (0 until rows.length()).map {
                val r = rows.getJSONObject(it)
                AuditRow(
                    revision = r.optLong("revision"),
                    actor = r.optString("actor"),
                    at = r.optString("at"),
                    changed = changedKeys(r.optJSONObject("old_value"), r.optJSONObject("new_value")),
                )
            }
        } catch (e: JSONException) {
            throw ApiException("استجابة سجلّ التدقيق غير صالحة")
        }
    }

    /** أسماء المفاتيح العليا التي تغيّرت بين نشرتين — والجديد وحده يعني «إضافة». */
    private fun changedKeys(old: JSONObject?, new: JSONObject?): List<String> {
        if (new == null) return emptyList()
        val names = mutableSetOf<String>()
        old?.keys()?.forEach { names.add(it) }
        new.keys().forEach { names.add(it) }
        return names.filter { key -> old?.opt(key)?.toString() != new.opt(key)?.toString() }.sorted()
    }

    /**
     * نداء دالّة `maxctl` — الطريق **الوحيد** لعمليات المفاتيح والنشر الموقّع.
     *
     * ولماذا دالّة لا كتابة مباشرة إلى الجداول: التوقيع يقع على الخادم بمفتاح خاص لا يدخل
     * التطبيق ولا المستودع، وجدولا `licenses`/`app_config` محجوبان عن الكتابة المباشرة (RLS).
     * ولو كتبت اللوحة في `app_config.value` لما رآها تطبيق MaxManager أصلًا: هو يقرأ العرض
     * الموقّع `config_signed` وحده — فكان النشر «ينجح» ويبقى المستخدمون على إعداد قديم.
     *
     * وردود الرفض تصل بترميز 200 مع `ok=false` (لأنها تحمل `server_time` حتى في الرفض)،
     * فالفحص هنا لا في `request`.
     */
    private fun call(s: Session, action: String, vararg fields: Pair<String, Any?>): JSONObject {
        val body = JSONObject().put("action", action)
        fields.forEach { (key, value) -> if (value != null) body.put(key, value) }
        val text = authed(s) { request("POST", "/functions/v1/maxctl", body.toString(), it) }
        val json = runCatching { JSONObject(text) }.getOrNull()
            ?: throw ApiException("استجابة غير متوقعة من دالّة التحكّم")
        if (!json.optBoolean("ok", false)) {
            val error = json.optString("error").ifEmpty { "unknown" }
            val message = json.optString("message")
            throw ApiException("رفض الخادم الطلب ($error)${if (message.isNotEmpty()) ": $message" else ""}")
        }
        return json
    }

    /** نشر الإعداد **موقّعًا** من الخادم. والردّ يحمل رقم الإصدار الجديد. */
    fun publishConfig(s: Session, value: JSONObject): JSONObject = call(s, "config.publish", "value" to value)

    /** إنشاء مفاتيح: الردّ يحمل نصّ المفتاح **مرّة واحدة** — ولا يُخزَّن خامًّا في أي جدول. */
    fun licenseCreate(
        s: Session,
        plan: String,
        days: Int,
        deviceLimit: Int,
        count: Int,
        note: String,
    ): List<Pair<String, String>> {
        val json = call(
            s,
            "license.create",
            "plan" to plan,
            "days" to days,
            "device_limit" to deviceLimit,
            "count" to count,
            "note" to note,
        )
        val keys = json.optJSONArray("keys") ?: JSONArray()
        return (0 until keys.length()).map {
            val row = keys.optJSONObject(it) ?: JSONObject()
            row.optString("key") to row.optString("hint")
        }
    }

    fun licenseList(s: Session, query: String): List<JSONObject> {
        val json = call(s, "license.list", "query" to query)
        val rows = json.optJSONArray("licenses") ?: JSONArray()
        return (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
    }

    fun licenseDetail(s: Session, id: String): JSONObject = call(s, "license.detail", "id" to id)

    fun licenseRevoke(s: Session, id: String, revoked: Boolean): Boolean =
        call(s, "license.revoke", "id" to id, "revoked" to revoked).optBoolean("revoked", revoked)

    fun licenseExtend(s: Session, id: String, days: Int): Int =
        call(s, "license.extend", "id" to id, "days" to days).optInt("duration_days")

    fun licenseResetDevices(s: Session, id: String) {
        call(s, "license.resetDevices", "id" to id)
    }
}
