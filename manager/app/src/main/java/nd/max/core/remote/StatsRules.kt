/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.remote

/**
 * **حالة قناة الإحصاءات** — تُكتب في ملفّ واحد ([StatsStore]) وتُقرأ قبل كلّ حكم.
 *
 * ليست إعدادًا للمستخدم (تلك موافقةٌ منفصلة في `settings`)، بل ذاكرة تشغيل: متى أُرسل آخر
 * إشارة، وبأيّ إصدارٍ وبيئة، وكم فشلت المحاولات المتتالية. وبها وحدها يُحسب الإيقاع المطلوب
 * في `MAX-CONTROL-01` §١٠.٤ بلا طابورٍ ولا خدمة خلفيّة جديدة.
 */
data class StatsState(
    /** وقت آخر إرسال **ناجح** (٠ = لم يُرسل شيء بعد). */
    val lastSentAt: Long = 0,
    /** الإصدار الذي رافق آخر إرسال — تغيّره حدثٌ حقيقيّ يسمح بإشارة إضافية. */
    val lastSentVersion: String = "",
    /** بيئة التحكّم التي رافقت آخر إرسال — تغيّرها حدثٌ حقيقيّ كذلك. */
    val lastSentEnv: String = "",
    /**
     * عدّادات الصحّة التي رافقت آخر إرسال (بنصّها كما بُنيت) — تغيّرها حدثٌ حقيقيّ كذلك:
     * «تخطّي ما لا يتغيّر» (§١٠.٤) يقارن الإصدار والبيئة **والعدّادات**، فإن جرى أيّ إجراء
     * تحكّم بين الإشارتين فالأعداد تغيّرت ويرجع الإرسال اليوميّ بدل انتظار الأسبوع.
     */
    val lastSentHealth: String = "",
    /** محاولات فاشلة متتالية منذ آخر نجاح (بلا طابور: تسقط بعد [StatsRules.MAX_ATTEMPTS]). */
    val attempts: Int = 0,
    /** وقت آخر محاولة (نجحت أو فشلت) — عليه يُحسب التباعد التصاعديّ. */
    val lastAttemptAt: Long = 0,
)

/**
 * سبب تخطّي الإشارة — **يُكتب في السجلّ بنصّه**، فلا يُخلط «المستخدم لم يوافق بعد» بـ«الخادم
 * لم يطلب» بـ«مرّ أسبوع بلا تغيير»: تشخيصٌ يطلبه المالك (ADR-77) ولا تعطيه كلمة «تجاوزنا».
 */
enum class StatsSkip(val code: String) {
    /** لم يوافق المستخدم بعد (الافتراضيّ: لا). */
    CONSENT_OFF("stats_no_consent"),

    /** الخادم لم يُفعّل الجمع (`stats.collect=false`) — والغياب مطفأ. */
    COLLECT_OFF("stats_no_collect"),

    /** ثلاث محاولات فاشلة في اليوم نفسه — لا طابور يتضخّم (§١٠.٤). */
    EXHAUSTED("stats_exhausted"),

    /** داخل التباعد التصاعديّ (١ · ٤ · ١٢ ساعة). */
    BACKOFF("stats_backoff"),

    /** أُرسلت اليوم ولا جديد. */
    RECENT("stats_recent"),

    /** مرّ يومها ولم يتغيّر شيء، وأسبوع لم يمضِ بعد (§١٠.٤: «تخطّي ما لا يتغيّر»). */
    UNCHANGED("stats_unchanged"),
}

/** حكم الإشارة: تُرسل أو تُتخطّى لسببٍ معلَن. */
sealed interface StatsDue {
    data object Send : StatsDue
    data class Skip(val reason: StatsSkip) : StatsDue
}

/**
 * **قواعد الإشارة اليومية — نقيّة بالكامل** (`MAX-CONTROL-01` §١٠.٣–§١٠.٤).
 *
 * هنا يُحتجز كلّ منطق الموافقة والإيقاع والرسالة، لا في الواجهة ولا في الشبكة: فتُختبر على
 * JVM في أجزاء الثانية، ويبقى موضعُ الإرسال رقيقًا. ولا يُبنى نصّ الإرسال في مكانين أبدًا —
 * [`trackPayload`] وحدها تبني ما يُعرض في الشاشة **وما يُرسل في الشبكة** (بايتات واحدة).
 */
object StatsRules {

    const val DAY_MS: Long = 24L * 60 * 60 * 1000
    const val WEEK_MS: Long = 7 * DAY_MS

    /** ثلاث محاولات ثم إسقاط — لا نموّ للطابور (لا طابور أصلًا). */
    const val MAX_ATTEMPTS = 3

    /**
     * التباعد التصاعديّ: ١ ساعة · ٤ ساعات · ١٢ ساعة (§١٠.٤).
     *
     * **وملاحظة صدق:** المدخلان الأوّلان حيّان (فشلٌ أول ⇒ ساعة، وثانٍ ⇒ ٤ ساعات)؛ وأمّا ١٢ ساعة
     * فقد شملها حارس الإسقاط: ثلاث محاولات فاشلة تُسقط الإشارة لليوم كاملًا، واليوم أطول من ١٢
     * ساعة — فلا يبقى للقيمة الثالثة مسارٌ عمليّ. أُبقيت لأنها نصّ الجدول، لا لأنها تمرّ.
     */
    val BACKOFF_MS: LongArray = longArrayOf(60L * 60 * 1000, 4L * 60 * 60 * 1000, 12L * 60 * 60 * 1000)

    const val MAX_VERSION_CHARS = 20
    const val MAX_TEXT_CHARS = 40
    const val DEFAULT_COUNTRY = "XX"

    /** القيم التي يقبلها قيد المخطّط — ولا يخرج غيرها من هنا. */
    val ALLOWED_ENVS = setOf("none", "shizuku", "magisk", "kernelsu", "apatch", "root_other")

    /** رمز الدولة من لغة الجهاز — حرفان لاتينيّان أو `XX`، لا شيء غير ذلك. */
    fun countryOf(raw: String?): String {
        val text = raw?.trim().orEmpty()
        return if (text.length == 2 && text.all { it.isLetter() }) text.uppercase() else DEFAULT_COUNTRY
    }

    /**
     * بيئة التحكّم كما نعرفها فعلًا — **ولا نقول أكثر ممّا نقيس** (ADR-07).
     *
     * `PrivilegeLevel` في هذا التطبيق ثلاثُ طبقات لا سادسة: `NONE` · `SHIZUKU` · `ROOT`. وتفصيل
     * السادسة (`magisk · kernelsu · apatch`) يحتاج فحص جذرٍ لا نملكه بعد؛ فـ`ROOT` تُعلَن
     * `root_other` بأمانة، والقيم المحجوزة تنتظر كاشفًا حقيقيًّا — لا تخمينًا.
     */
    fun envOf(level: String?): String = when (level?.trim()?.uppercase()) {
        "SHIZUKU" -> "shizuku"
        "ROOT" -> "root_other"
        else -> "none"
    }

    /**
     * **الحكم الوحيد**: هل تُرسل الآن، أم لا ولماذا؟
     *
     * الترتيب مقصود: الموافقة قبل كل شيء (ولا إشارة بلا «نعم» صريحة)، ثم إذنٌ الخادم، ثم
     * ذاكرة الفشل، ثم الإيقاع. وتغيُّر الإصدار أو البيئة **حدثٌ حقيقيّ** يعبر حاجز الأسبوع
     * — وتبقى كلمةُ اليوم الأخيرة للخادم ([`track_install`] يرفض إشارة ثانية في اليوم نفسه).
     */
    fun due(
        state: StatsState,
        consent: Boolean,
        collect: Boolean,
        version: String,
        env: String,
        now: Long,
        health: String = "{}",
    ): StatsDue {
        if (!consent) return StatsDue.Skip(StatsSkip.CONSENT_OFF)
        if (!collect) return StatsDue.Skip(StatsSkip.COLLECT_OFF)

        if (state.attempts >= MAX_ATTEMPTS && now - state.lastAttemptAt < DAY_MS) {
            return StatsDue.Skip(StatsSkip.EXHAUSTED)
        }
        if (state.attempts > 0) {
            val wait = BACKOFF_MS[(state.attempts - 1).coerceIn(0, BACKOFF_MS.lastIndex)]
            if (now - state.lastAttemptAt < wait) return StatsDue.Skip(StatsSkip.BACKOFF)
        }

        val changed = version != state.lastSentVersion ||
            env != state.lastSentEnv ||
            health != state.lastSentHealth
        if (state.lastSentAt > 0 && !changed) {
            val age = now - state.lastSentAt
            if (age < DAY_MS) return StatsDue.Skip(StatsSkip.RECENT)
            if (age < WEEK_MS) return StatsDue.Skip(StatsSkip.UNCHANGED)
        }
        return StatsDue.Send
    }

    /**
     * **الرسالة نفسها** — بناء واحد للعرض والإرسال معًا: حقولٌ بأسماء دالّة الخادم حرفيًّا
     * (`p_*`)، بترتيب ثابت، وتهريب آمن. ومن يعرضها في الشاشة يعرض هذه الدالّة بعينها،
     * فلا يوصف payload ويُرسل آخر (وهذا هو شرط §١٠.٣ حرفيًّا).
     *
     * والحقل التاسع `p_health` — عدّادات الصحّة المصنَّفة (§١٠.١) — يُبنى في
     * [nd.max.core.hardware.HealthModel] ويُمرَّر هنا **كائنًا خامًّا** لا سلسلةً مهرَّبة
     * (الخادم يقرؤه `jsonb`)، ويحرسه [safeHealth] من أيّ نصّ آخر.
     */
    fun trackPayload(
        installId: String,
        country: String,
        version: String,
        manufacturer: String,
        model: String,
        sdk: Int,
        release: String,
        env: String,
        health: String = "{}",
    ): String {
        val safeEnv = if (env in ALLOWED_ENVS) env else "none"
        val healthJson = safeHealth(health)
        return buildString {
            append('{')
            append("\"p_install\":").append(jsonString(installId)).append(',')
            append("\"p_country\":").append(jsonString(countryOf(country))).append(',')
            append("\"p_version\":").append(jsonString(version.take(MAX_VERSION_CHARS))).append(',')
            append("\"p_manufacturer\":").append(jsonString(manufacturer.take(MAX_TEXT_CHARS))).append(',')
            append("\"p_model\":").append(jsonString(model.take(MAX_TEXT_CHARS))).append(',')
            append("\"p_sdk\":").append(sdk).append(',')
            append("\"p_release\":").append(jsonString(release.take(MAX_VERSION_CHARS))).append(',')
            append("\"p_env\":").append(jsonString(safeEnv)).append(',')
            append("\"p_health\":").append(healthJson)
            append('}')
        }
    }

    /**
     * سقف حجم رسالة العدّادات — **نفس سقف قيد الخادم** (`installs_health_shape`)، فما يُبنى
     * هنا يمرّ هناك بلا رفض.
     */
    const val MAX_HEALTH_CHARS = 4096

    /**
     * حرس شكل العدّادات: كائن JSON واحد بسقف حجم — لا سلسلة ولا مصفوفة ولا نصّ حرّ.
     *
     * **ولا يُفسَّر JSON هنا:** الباني الوحيد هو [nd.max.core.hardware.HealthModel.encode]
     * (نقيّ ومُختبر)، وهذا الحرس يمنع أيّ نصّ آخر من العبور إلى الحقل فحسب. ومنحه صلاحية
     * «تصحيح» بنية مجهولة كان سيعني مُفسِّرًا ثانيًا يخالف الأول — وهو صنف العطب الذي يمنعه
     * هذا المستودع.
     */
    private fun safeHealth(raw: String): String {
        val text = raw.trim()
        return if (text.length in 2..MAX_HEALTH_CHARS && text.startsWith("{") && text.endsWith("}")) {
            text
        } else {
            "{}"
        }
    }

    /** طلب السحب: معرّف واحد لا غير — لا حقل آخر يُضاف إليه أبدًا. */
    fun forgetPayload(installId: String): String = "{\"p_install\":${jsonString(installId)}}"

    private fun jsonString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}
