/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.remote

/** من أين جاء الإعداد المعتمد — **وهذا ما يُعرض للمستخدم**، لا `revision = 0` المشوِّش. */
enum class ConfigOrigin {
    /** خادمٌ وردَ الآن وتحقّق توقيعه. */
    SERVER,

    /** ذاكرة محلية تحقّق توقيعها الآن — الإعداد الأخير الصالح. */
    MEMORY,

    /** الافتراضي المكتوب في الكود: لا خادم ولا ذاكرة. */
    DEFAULT,
}

/** ما حدث في محاولة الشبكة عند الإقلاع — **يُقاس لا يُفترض**. */
sealed interface FetchAttempt {
    /** لم تُحاول: عند الإقلاع لا يُنتظر خادم (لا شبكة في مسار الإقلاع). */
    data object NotAttempted : FetchAttempt

    /** حاولت وفشلت، والرمز اسم العطب لا وصفه. */
    data class Failed(val code: String) : FetchAttempt

    /** ورد غلافٌ جديد — والتحقّق منه يقع في البوّابة لا في العميل. */
    data class Received(val envelope: SignedEnvelope, val etag: String?) : FetchAttempt
}

/**
 * حكم الإقلاع الكامل: الإعداد المعتمد، ومصدره، وعمره، ورمز تشخيصيّ إن كان في الطريق عطب.
 *
 * و[refreshInBackground] صريح لأن مسار الإقلاع **لا ينتظر شبكة أبدًا**: القرار يُتّخذ من الذاكرة
 * ثم يُحدَّث في الخلفية (§٩). و«بلا حجب للشاشة» ليس تفصيلًا جماليًّا: تطبيقٌ يفتح على دوّارةٍ
 * تنتظر خادمًا لا يردّ أسوأ من تطبيقٍ يفتح فورًا على شيء يعمل.
 */
data class BootDecision(
    val config: RemoteConfig,
    val origin: ConfigOrigin,
    val freshness: ConfigFreshness,
    /** `revision` المعتمد — وهو أرضية منع الرجوع في المحاولة القادمة. */
    val revision: Long,
    /** رمز العطب إن وقع (من [RemoteFault] أو [RemoteRejection]) — وإلّا `null`. */
    val code: String?,
    val refreshInBackground: Boolean,
    val etag: String?,
) {
    val serverBacked: Boolean get() = origin == ConfigOrigin.SERVER
}

/**
 * الحكم عند الإقلاع: **(خادمٌ صالح) أو (ذاكرةٌ صالحة) أو (الافتراضي المحلي)** — ولا رابع.
 *
 * ## ولماذا هذه الدالّة **نقيّة** بلا شبكة ولا Context
 *
 * لأن كل ما يجب أن يُثبت في §٥.٤ (١ · ٣ · ٤ · ٥ · ٦ · ٨ · ١١ · ١٢) **قرارٌ** لا اتصال: أيّ
 * مصدر يُعتمد، وبأيّ عمر، وبأيّ رمز. وبتقديم [FetchAttempt] كوسيط تصير كل حالة مضادّة سطرًا في
 * اختبار على JVM عادية — بلا خادم ولا محاكاة.
 *
 * ## و«الافتراضي المحلي» ليس «لا شيء»
 *
 * [RemoteFlags.defaultConfig] هي سلوك التطبيق اليوم: مفاتيحه مفعَّلة كما هي في الحزمة. فغياب
 * الخادم (أو وقوف المشروع المجّاني، §٤.٣) **لا يُطفئ ميزة ولا يفتح أخرى** — وهذا الفرق بين
 * «افتراضي معلن» و«فراغ»: الأول حالةٌ يعرفها الكود، والثاني سبقُ استعمالٍ يُعطّل التطبيق صامتًا.
 */
object ConfigBootGate {

    /** الـTTL المستعمل حين لا يُذكر في الإعداد نفسه (وهو مذكور في العقد، §٨). */
    const val DEFAULT_TTL_HOURS = 12

    fun decide(
        nowMillis: Long,
        fetched: FetchAttempt,
        stored: StoredConfig?,
        publicKeyBase64: String,
    ): BootDecision {
        val floor = stored?.envelope?.revision ?: 0L

        if (fetched is FetchAttempt.Received) {
            when (val admitted = RemoteConfigs.admit(fetched.envelope, publicKeyBase64, floor, nowMillis)) {
                is Admission.Admitted -> return BootDecision(
                    config = admitted.config,
                    origin = ConfigOrigin.SERVER,
                    freshness = ConfigFreshness.FRESH,
                    revision = admitted.config.revision,
                    code = null,
                    refreshInBackground = false,
                    etag = fetched.etag,
                )
                // رفضٌ صريح: يُسجَّل برمزه، ثم تُجرَّب الذاكرة — **لا** يُهمل الفشل ويُتابع.
                is Admission.Rejected -> return fromMemory(
                    nowMillis = nowMillis,
                    stored = stored,
                    publicKeyBase64 = publicKeyBase64,
                    code = admitted.reason.code,
                )
            }
        }

        val failureCode = (fetched as? FetchAttempt.Failed)?.code
        return fromMemory(nowMillis, stored, publicKeyBase64, failureCode)
    }

    /**
     * الذاكرة: يُعاد التحقّق من توقيعها **الآن** (‏§٥.١: تبديل ملفّ محلي يُمنع)، فإن صلحت فهي
     * الحكم بعمرها المعلن، وإلّا فالافتراضي وحده.
     */
    private fun fromMemory(
        nowMillis: Long,
        stored: StoredConfig?,
        publicKeyBase64: String,
        code: String?,
    ): BootDecision {
        if (stored != null) {
            // الأرضية صفر عن قصد: الذاكرة هي «آخر ما تُقبل»، فلا تُسقط نفسها بفحص الرجوع.
            val admitted = RemoteConfigs.admit(stored.envelope, publicKeyBase64, 0L, nowMillis)
            if (admitted is Admission.Admitted) {
                val ttl = admitted.config.stats?.effectiveTtlHours ?: DEFAULT_TTL_HOURS
                return BootDecision(
                    config = admitted.config,
                    origin = ConfigOrigin.MEMORY,
                    freshness = stored.freshness(nowMillis, ttl),
                    revision = admitted.config.revision,
                    code = code,
                    // الذاكرة لا تُغني عن الطلب: الخادم قد يحمل إصدارًا أحدث — والطلب في الخلفية.
                    refreshInBackground = true,
                    etag = null,
                )
            }
            // ذاكرة مفسودة أو معدَّلة: تُهمل، ويُسجَّل رمزها إن لم يكن في الطريق عطب أسبق.
            return defaultDecision(code ?: deliberateCode(admitted))
        }
        return defaultDecision(code)
    }

    private fun deliberateCode(admission: Admission): String =
        (admission as? Admission.Rejected)?.reason?.code ?: RemoteRejection.MALFORMED.code

    private fun defaultDecision(code: String?) = BootDecision(
        config = RemoteFlags.defaultConfig(),
        origin = ConfigOrigin.DEFAULT,
        freshness = ConfigFreshness.ABSENT,
        revision = 0,
        code = code,
        refreshInBackground = true,
        etag = null,
    )
}
