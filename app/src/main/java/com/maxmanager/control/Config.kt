package com.maxmanager.control

import android.util.Base64

/**
 * Supabase connection.
 *
 * Preferred: set the repository secrets SUPABASE_URL and SUPABASE_ANON_KEY (GitHub → Settings →
 * Secrets and variables → Actions); the workflow passes them in at build time.
 * Alternative: replace the two placeholders below. The publishable / anon key is public by design
 * (RLS protects the data). NEVER use the service_role / secret key here — [problem] refuses it.
 */
object Config {
    private const val PLACEHOLDER_URL = "https://ibnlpwayycpttxphagpr.supabase.co"
    private const val PLACEHOLDER_KEY = "sb_publishable_LLLB6VTTOiluL7AuukYTkA_FQADkEsZ"

    val SUPABASE_URL: String = BuildConfig.SUPABASE_URL.ifBlank { PLACEHOLDER_URL }.trimEnd('/')
    val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY.ifBlank { PLACEHOLDER_KEY }

    /** Null when the connection settings are usable, otherwise an Arabic explanation for the owner. */
    fun problem(): String? = when {
        SUPABASE_URL.contains("YOUR-") || SUPABASE_ANON_KEY.contains("YOUR-") ->
            "لم تُضبط بيانات Supabase بعد. أضِف SUPABASE_URL و SUPABASE_ANON_KEY كـ Secrets في GitHub ثم أعد البناء، أو عدّل Config.kt."
        !SUPABASE_URL.startsWith("https://") ->
            "عنوان Supabase يجب أن يبدأ بـ https://"
        isSecretKey(SUPABASE_ANON_KEY) ->
            "المفتاح المضبوط هو مفتاح سرّي (service_role). لا تضعه في التطبيق أبدًا — استخدم مفتاح anon / publishable."
        else -> null
    }

    private fun isSecretKey(key: String): Boolean {
        if (key.startsWith("sb_secret_")) return true
        val payload = key.split('.').getOrNull(1) ?: return false
        return runCatching {
            val bytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            String(bytes, Charsets.UTF_8).contains("service_role")
        }.getOrDefault(false)
    }
}
