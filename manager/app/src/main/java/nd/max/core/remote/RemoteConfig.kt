/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nd.max.core.access.PromoWindow
import nd.max.core.access.Promos
import java.time.Instant

/**
 * عقد الإعداد البعيد بين لوحة المالك وMaxManager — **وموضع الحكم كله في هذا الملفّ**.
 *
 * ## أولًا: لماذا وُجد أصلًا
 *
 * قِيس في `MAX-CONTROL-01` §٢.٣ أن `app_config` في Supabase **يُكتب ولا يُقرأ**: لا ملفّ واحد في
 * `nd.max` يقرأه، وقناة الإصدار الفعّالة قناة أخرى (`docs/update/manifest.json`). فهذا الملفّ
 * يُغلق حلقة مفتوحة: مفتاح ميزة يُطفأ من اللوحة **يجب** أن يصل إلى جهاز المستخدم، وإلّا فالمفتاح
 * زرٌّ يحرّك شيئًا في اللوحة وحدها — وهي بعينها «الواجهة الوهمية» التي نهى عنها المالك.
 *
 * ## ثانيًا: لماذا التوقيع على **البايتات** لا على كائن JSON
 *
 * الشكل البديهي أن يصل الـpayload ككائن JSON ويُوقَّع «تمثيله القياسيّ». وهذا يفتح بابًا معروفًا
 * من الأعطال: ترتيب المفاتيح، والمسافات، و`1` مقابل `1.0` — فيُوقَّع نصّ ويُتحقَّق من نصّ آخر
 * **مختلفين حرفًا وواحدًا في المعنى**، فيُرفض إعداد سليم ولا يقول أحدٌ لماذا. ولذلك هنا:
 *
 * ```
 * payload   : base64( بايتات UTF-8 بالضبط كما وُقّعت )
 * signature : base64( توقيع ECDSA P-256/SHA-256 على تلك البايتات )
 * ```
 *
 * فالتحقّق يصير **تشغيليًّا لا حسابيًّا**: بايتاتٌ دخلت، بايتاتٌ خرجت، ولا إعادة تسلسل في الطريق.
 * (والـjsonb في قاعدة البيانات يبقى **مصدر التحرير** في اللوحة؛ ودالّة التوقيع تُنتج النصّ القياسيّ
 * مرّة واحدة وتخزّنه، فما يُوقَّع هو ما يُخزَّن بالحرف.)
 *
 * ## ثالثًا: ترتيب الفحص — وأول فشل يُنهي
 *
 * ١ التوقيع (بالبايتات) → ٢ الصيغة تُقرأ → ٣ `schema` ≤ المدعوم → ٤ `revision` **أكبر** من المسجّل
 * → ٥ `revision` في الغلاف يساوي الذي **داخل** التوقيع → ٦ `issued_at` زمنٌ معقول لا مستقبليّ
 * ولا من عمر الكون. **ولا يُقبل شيء بأقلّ من هذا كله.**
 *
 * والترتيب مقصود: التوقيع أولًا لأن ما بعده **قراءة لبيانات لم تُثبت بعد**؛ ولو قُرئت أولًا لأمكن
 * لمدخلٍ مجهول أن يُشغل قارئ JSON بأشكال غريبة قبل أي تحقّق.
 *
 * ## رابعًا: المفاتيح غير المعروفة تُهمَل ولا تُسقط الإعداد (§٥.٤ حالة ٧)
 *
 * اللوحة تُنشر مستقلة عن دورات إصدار التطبيق: مفتاح يُضاف غدًا يجب ألّا يكسر النسخ المثبَّتة اليوم.
 * لكن «الإهمال» ليس صمتًا: الأسماء الغريبة تُعاد في [RemoteConfig.unknownFeatureKeys] لتُسجَّل.
 *
 * ## خامسًا: ما **لا** يملكه هذا العقد — بالبناء لا بالفحص (§٥.٤ حالة ٩)
 *
 * لا حقل هنا يفتح مسار كتابة عتاد، ولا يمنح قدرة جديدة. المفاتيح **تُقيّد أو تُخفي فقط**، وكل
 * كتابة عتاد تبقى خلف `HardwareControlArbiter` وحده (ADR-11). وهذا مُثبَّت في
 * `RemoteConfigArchitectureTest` بقائمة أسماء حقولٍ مسموحة: إضافة حقلٍ يمنح قدرة **تُسقط الاختبار**.
 */

/**
 * معنى مفتاح الميزة — **ثلاث حالات بأسماء المالك في لوحته حرفيًّا**، ولا رابعة:
 *
 * * `on` — تعمل للجميع (سلوك اليوم).
 * * `subscribe` — سطحٌ ظاهر **مقفلٌ بمفتاح**: يعمل لمن فعّل مفتاحًا سارٍ، ومَن لا مفتاح له يرى
 *   شاشة «يلزم مفتاح» مع رابط الشراء ومدخل التفعيل. وهذه هي «الميزة المدفوعة» بعينها.
 * * `off` — مطفأ: لا يفتح (وإخفاء مداخله في الواجهات تفصيلٌ يُبنى في جولة الواجهة).
 */
enum class FeatureState {
    ON,
    SUBSCRIBE,
    OFF,
}

/** مفتاح ميزة واحد. `min_version_code` يمنع بيع مفتاح لتطبيقٍ لا يعرف معناه. */
@Serializable
data class RemoteFeature(
    val enabled: Boolean = false,
    @SerialName("min_version_code") val minVersionCode: Int = 0,
    val state: String = "",
    val title: String = "",
    @SerialName("message_ar") val messageAr: String = "",
    @SerialName("message_en") val messageEn: String = "",
) {
    /** هل يُقرأ هذا المفتاح أصلًا في هذه النسخة من التطبيق؟ */
    fun understoodBy(versionCode: Int): Boolean = versionCode >= minVersionCode

    /**
     * الحالة كما تعنيها اللوحة — وتقبل الشكل القديم (`enabled` وحده) فلا يتعطّل إعدادٌ قديم
     * صالح: نصّ الحالة يسبق، وغيابه يسقط على `enabled` المكتوبة.
     */
    fun state(): FeatureState = when (state.trim().lowercase()) {
        "on" -> FeatureState.ON
        "subscribe", "subscription" -> FeatureState.SUBSCRIBE
        "off" -> FeatureState.OFF
        else -> if (enabled) FeatureState.ON else FeatureState.OFF
    }
}

/**
 * قناة التواصل التجاريّ: **رابط الشراء/الاشتراك الذي يكتبه المالك بيده** ونصوصه، ورابط تنزيل
 * التطبيق. ولا شيء هنا يُشتقّ تلقائيًّا من مستودعٍ أو إصدار: ما كتبه المالك هو ما يُعرض.
 */
@Serializable
data class RemoteSubscription(
    val url: String = "",
    @SerialName("payment_text_ar") val paymentTextAr: String = "",
    @SerialName("payment_text_en") val paymentTextEn: String = "",
    @SerialName("download_url") val downloadUrl: String = "",
)

/**
 * نافذة «مجّاني للجميع» كما تُنشر في الإعداد الموقّع — واللحظة بالصيغة ISO-8601.
 * وترتيب النوافذ قبل الحلّ في [RemoteConfigs.admit]: غير الصالح يُهمَل ولا يُسقط الإعداد.
 */
@Serializable
data class RemotePromo(
    val from: String = "",
    val to: String = "",
    val note: String = "",
)

/**
 * إصدار واحد في سجلّ التغييرات الذي تكتبه اللوحة — **نصوص فقط**: رقمٌ وتاريخٌ وملاحظات.
 * ولا حقل فيه يمنح قدرة: كلّه يُعرض سطورًا في نافذة التحديث («الجديد»).
 *
 * وكان هذا السجلّ يُنشر من اللوحة ثم **يُهمَل** في التطبيق (حقل غير معروف في العقد)، فبقيت
 * نافذة التحديث بلا «الجديد» أبدًا. قراءته هنا تُغلق الحلقة: ما يكتبه المالك هو ما يُعرض.
 */
@Serializable
data class RemoteRelease(
    val version: String = "",
    val date: String = "",
    @SerialName("notes_ar") val notesAr: List<String> = emptyList(),
    @SerialName("notes_en") val notesEn: List<String> = emptyList(),
)

/**
 * إعلان داخل التطبيق — «تحديث داخليّ» بمعنى المالك: نصّ يظهر للمستخدمين بلا إصدار نسخة جديدة.
 * `until` حدّ زمنيّ اختياريّ بعده يسقط الإعلان من نفسه وإن نسي المالك إطفاءه.
 */
@Serializable
data class RemoteAnnouncement(
    val enabled: Boolean = false,
    @SerialName("text_ar") val textAr: String = "",
    @SerialName("text_en") val textEn: String = "",
    val until: String? = null,
)

/**
 * الإصدار المعروض في اللوحة — **ومنذ جولة إصلاح التحديث هو الذي يفتح نافذة التحديث**:
 * المالك يكتب الرقم ورابط التنزيل في لوحته، والنافذة تعرض الجديد ثم تفتح الرابط في المتصفّح.
 * وقناة المانيفست بقيت كما هي (الحجب والتعليق)، لكن قناة اللوحة هي التي يملكها المالك.
 *
 * و`download_page` هو الاسم الذي **تكتبه اللوحة فعلًا** في `latest` — وكان يصل ويُهمَل لأن
 * الحقل هنا اسمه `page` بلا `@SerialName`، فيقرأ التطبيق فراغًا دائمًا. قِيس على الحمولة
 * الحيّة للمشروع: المفتاح الموقّع يحمل `download_page` حرفًا، و`page` لا يأتي من اللوحة أبدًا.
 */
@Serializable
data class RemoteLatest(
    val version: String = "",
    val released: String = "",
    @SerialName("download_page") val page: String = "",
    /**
     * `required` (أو `mandatory`/`force` كمرادفات لوحةٍ قديمة) ⇒ نافذة لا تُغلق حتى يُثبَّت
     * الإصدار، وغيابه أو `optional` ⇒ نافذة إرشاديّة تُتخطّى وتعود بعد [reminderHours].
     * والقيمة تُطبَّع هنا لا في الواجهة: حكمٌ واحد يقرأه كل مستهلك.
     */
    val mode: String = MODE_OPTIONAL,
    /**
     * فاصل التذكير بالساعات للإصدار الاختياري (١ ساعة .. ٣٠ يومًا). و`0` تعني «مرّة واحدة»
     * في اللوحة القديمة، وتُقرأ كـ٢٤ ساعة لا ككتمان أبديّ.
     */
    @SerialName("reminder_hours") val reminderHours: Int = DEFAULT_REMINDER_HOURS,
) {
    /** هل هذا التحديث إلزاميّ؟ */
    val required: Boolean
        get() = mode.trim().lowercase() in REQUIRED_MODES

    /** الفاصل الفعلي بعد التقييد — لا يثق بعنوانٍ من الخادم بلا سقف. */
    val effectiveReminderHours: Int
        get() = reminderHours.coerceIn(MIN_REMINDER_HOURS, MAX_REMINDER_HOURS)

    companion object {
        const val MODE_OPTIONAL = "optional"
        const val MODE_REQUIRED = "required"
        const val DEFAULT_REMINDER_HOURS = 24
        const val MIN_REMINDER_HOURS = 1
        const val MAX_REMINDER_HOURS = 24 * 30

        private val REQUIRED_MODES = setOf(MODE_REQUIRED, "mandatory", "force", "forced", "blocking")
    }
}

/**
 * سياسة الإحصاءات كما تُضبط من الخادم. **ولا تُلغي موافقة المستخدم أبدًا**: الجمع يقع إذا
 * تحقّق الشرطان معًا — موافقة صريحة على الجهاز **و** `collect = true` هنا. فإطفاؤه من اللوحة
 * يوقف الجمع، وتشغيله لا يُشغّله في جهازٍ لم يوافق.
 */
@Serializable
data class RemoteStats(
    @SerialName("collect") val collect: Boolean = false,
    @SerialName("sample_rate") val sampleRate: Int = 100,
    @SerialName("ttl_hours") val ttlHours: Int = 12,
) {
    /** النسبة تُقيَّد داخل ١..١٠٠ فلا يصير `sample_rate` بوابةً لسلوك غير متوقّع. */
    val effectiveSampleRate: Int get() = sampleRate.coerceIn(1, 100)

    /** سقف الـTTL: يومٌ كامل لا أكثر، فلا يُخزَّن إعدادٌ شهرًا ويُقال «طازج». */
    val effectiveTtlHours: Int get() = ttlHours.coerceIn(1, 24)
}

/**
 * سياسة نوافذ التحديث كما ينشرها المالك من لوحة التحكّم (`com.maxmanager.control`).
 *
 * **ولماذا في الإعداد الموقّع لا في مفتاحٍ محليّ:** كانت في التطبيق مفتاحًا للمستخدم في شاشة
 * التحكّم، والمستخدم هو من يُسكِت إشعارات التحديث — وهي قدرة مالكٍ لا قدرة مستخدم (أمر المالك
 * ٢٠٢٦-١٠-٢١: «أريده في `com.maxmanager.control` حتى أتمكّن من تعطيل إشعارات التحديث
 * للمستخدمين»). والقيمة تصل موقّعة، وتُطبَّق عند اعتماد الإعداد، فلا تعتمد على شبكةٍ لحظةَ
 * عرض النافذة.
 */
@Serializable
data class RemoteUpdates(
    /** `true` ⇒ لا نوافذ تحديث تلقائية. والفحص اليدويّ والحجب الأمنيّ لا يمسّهما هذا العلَم. */
    @SerialName("disabled") val disabled: Boolean = false,
)

/** الـpayload الموقّع نفسه — هذا هو ما يُخزَّن كبايتات في الغلاف أعلاه. */
@Serializable
data class RemotePayload(
    val schema: Int = 0,
    val revision: Long = 0,
    @SerialName("issued_at") val issuedAt: String = "",
    @SerialName("expires_at") val expiresAt: String? = null,
    val features: Map<String, RemoteFeature> = emptyMap(),
    val announcement: RemoteAnnouncement? = null,
    val latest: RemoteLatest? = null,
    val stats: RemoteStats? = null,
    val subscription: RemoteSubscription? = null,
    val promos: List<RemotePromo> = emptyList(),
    val changelog: List<RemoteRelease> = emptyList(),
    val updates: RemoteUpdates? = null,
)

/**
 * الأربعة التي تعبر الشبكة: `schema` و`revision` إضافةً إلى البايتات الموقّعة وتوقيعها.
 * و`schema`/`revision` هنا **ليسا مصدر حكم**: حكمهما من الذي داخل التوقيع، وهما خارجها
 * لسببين هندسيّين فقط: يقرأ العميل `schema` قبل أن يفكّ الترميز (فيعلن «صيغة أحدث مني»
 * برسالة صحيحة)، و`revision` مكرّر لأن المساواة بين الاثنين فحصٌ مجانيّ يمسك غلافًا مبدَّلًا.
 */
@Serializable
data class SignedEnvelope(
    val schema: Int = 0,
    val revision: Long = 0,
    val payload: String = "",
    val signature: String = "",
)

/** الإعداد المقبول بعد كل الفحوص — عرضٌ **محلول** لا بايتات. */
data class RemoteConfig(
    val revision: Long,
    val issuedAtMillis: Long,
    val expiresAtMillis: Long?,
    val features: Map<String, RemoteFeature>,
    val announcement: RemoteAnnouncement?,
    val latest: RemoteLatest?,
    val stats: RemoteStats?,
    /** قناة التواصل التجاريّ — روابط المالك ونصوصه، أو `null` إن لم تُنشر بعد. */
    val subscription: RemoteSubscription? = null,
    /** نوافذ «مجّاني للجميع» **محلولة**: المللي ثانية لا نصوص ISO (الحلّ في البوّابة). */
    val promos: List<PromoWindow> = emptyList(),
    /** سجلّ التغييرات كما كتبه المالك — تُقرأ منه ملاحظات الإصدار في نافذة التحديث. */
    val changelog: List<RemoteRelease> = emptyList(),
    /**
     * سياسة نوافذ التحديث كما نشرها المالك — `null` أي «لم تُنشر بعد» فتبقى النوافذ تظهر
     * (السلوك السابق بالضبط)، فلا يكتم إعدادٌ ناقص إشعارًا لم يقصده المالك.
     */
    val updates: RemoteUpdates? = null,
    /** أسماء مفاتيح لم يعرفها هذا الإصدار — تُسجَّل ولا تُسقط الإعداد (توافقًا أماميًّا). */
    val unknownFeatureKeys: List<String>,
) {
    /**
     * الإعلان الصالح **الآن**: مفعَّل، ونصّه غير فارغ، ولم ينقضِ `until`.
     * والاختبار على الوقت يقع هنا لا في الواجهة، فمهلة الإعلان ليست قرار عرض.
     */
    fun announcementAt(nowMillis: Long): RemoteAnnouncement? {
        val current = announcement ?: return null
        if (!current.enabled) return null
        if (current.textAr.isBlank() && current.textEn.isBlank()) return null
        val until = current.until?.let(RemoteConfigs::instantMillis) ?: return current
        return current.takeIf { nowMillis < until }
    }

    /** نافذة المجّاني السارية الآن — حكمٌ واحد يُنادى من البوّابة وشاشة الترخيص معًا. */
    fun activePromoAt(nowMillis: Long): PromoWindow? = Promos.active(promos, nowMillis)

    /**
     * ملاحظات إصدار بعينه باللغة المطلوبة، وإلا فاللغة الأخرى، وإلا فلا شيء.
     * ولا تُخلط لغتان في عرض واحد: من كتب ملاحظات عربية فقط يقرؤها العربي وحده.
     */
    fun releaseNotes(version: String, language: String): List<String> {
        val entry = changelog.firstOrNull { it.version == version.trim() } ?: return emptyList()
        val arabic = language.lowercase().startsWith("ar")
        return if (arabic) entry.notesAr.ifEmpty { entry.notesEn } else entry.notesEn.ifEmpty { entry.notesAr }
    }
}

/** أسباب الرفض — كلٌّ باسمه، فلا يُدمج عطبٌ في «فشل». والاسم يُكتب في السجلّ كما هو. */
enum class RemoteRejection(val code: String) {
    /** لا مفتاح عامّ مضبوط في هذا البناء: لا توقيع يمكن التحقّق منه. */
    NOT_CONFIGURED("remote_not_configured"),

    /** غلاف أو حمولة لا تُقرأ (base64 مكسور، JSON مقطوع، حقل مفقود). */
    MALFORMED("remote_malformed"),

    /** التوقيع باطل: حمولة مبدَّلة، أو مفتاح مزوّر، أو خادم لا يملك المفتاح. */
    SIGNATURE("remote_signature"),

    /** صيغة أحدث من التي يفهمها هذا الإصدار → يُرفض **كاملًا** والبقاء على الافتراضي. */
    SCHEMA("remote_schema"),

    /** `revision` ليس أكبر من المسجَّل → **مانع الرجوع**. */
    REVISION("remote_revision"),

    /** `revision` في الغلاف لا يساوي الذي داخل التوقيع → غلاف مبدَّل. */
    REVISION_MISMATCH("remote_revision_mismatch"),

    /** `issued_at` مستقبليّ بفارق غير معقول، أو خارج أي عمر ممكن. */
    ISSUED_INVALID("remote_issued_invalid"),
}

/** نتيجة المحاولة: إعدادٌ مقبول، أو رفضٌ **بسببه**. ولا ثالث («تجاهل الفشل والمتابعة»). */
sealed interface Admission {
    data class Admitted(val config: RemoteConfig) : Admission
    data class Rejected(val reason: RemoteRejection, val detail: String) : Admission
}

object RemoteConfigs {

    /** أعلى صيغة يفهمها هذا الكود. الأعلى منها يُرفض كاملًا (§٥.٤ حالة ٨). */
    const val SCHEMA = 1

    /** سقف حمولة الإعداد: عقدٌ صغير، والرابط المكسور لا يُحمّل megabytes. */
    const val MAX_PAYLOAD_BYTES = 64 * 1024

    /** فرق الساعات المسموح به للجهاز — ساعة جهاز متأخّرة دقائق ليست عطبًا. */
    const val CLOCK_SKEW_MILLIS = 15 * 60 * 1000L

    /** أقصى عمر معقول لـ`issued_at`: عشر سنوات. ما بعدها ليس «إعدادًا قديمًا» بل زمنٌ مكسور. */
    const val MAX_ISSUED_AGE_MILLIS = 3650L * 24 * 60 * 60 * 1000

    /** سقف نوافذ المجّاني في الإعداد الواحد — عقدٌ صغير، وسجلٌّ لا ملفّ. */
    const val MAX_PROMOS = 64

    /** سقف سجلّ التغييرات: ٣٢ إصدارًا تكفي عرضًا، وما زاد قِصاصه بلا ضجيج. */
    const val MAX_CHANGELOG = 32

    /** سقف ملاحظات الإصدار الواحد: سطرًا وطولًا — النصّ المعروض لا ملفّ. */
    const val MAX_NOTE_LINES = 20
    const val MAX_NOTE_CHARS = 160

    /**
     * `ignoreUnknownKeys` **مقصود** (كما في `UpdateManifests` وبنفس السبب): الخادم يُنشر مستقلًا
     * عن دورات الإصدار، فحقلٌ يُضاف غدًا لا يجوز أن يكسر النسخ المثبَّتة اليوم.
     * و`explicitNulls = false` لأن اللوحة ترسل `"until": null` و`"expires_at": null` بمعنى
     * «غير محدَّد»؛ وقراءتها كغياب لا كخطأ هي الصواب.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        explicitNulls = false
    }

    /** قراءة الغلاف — `null` عند أي خلل، ولا نموذج نصفه أصفار. */
    fun parseEnvelope(raw: String): SignedEnvelope? =
        runCatching { json.decodeFromString<SignedEnvelope>(raw) }.getOrNull()

    /** قراءة الحمولة من **بايتاتها** (لا من نصّ مُعاد تسلسله) — `null` عند أي خلل. */
    fun parsePayload(bytes: ByteArray): RemotePayload? = runCatching {
        json.decodeFromString<RemotePayload>(bytes.toString(Charsets.UTF_8))
    }.getOrNull()

    /** تاريخٌ وحده (`2026-10-14`) — يُقرأ منتصف ليل UTC. */
    private val DATE_ONLY = Regex("""\d{4}-\d{2}-\d{2}""")

    /**
     * قراءة ISO-8601 إلى ميلي ثانية — `null` عند نصّ لا يُقرأ.
     *
     * وتُقبل **التاريخ وحده** (`2026-10-14`) كمنتصف ليل UTC. ولماذا: لوح التحكّم يكتب تواريخ
     * بشرية، و`Instant.parse` وحدها ترفضها بينما الخادم (`Date.parse` في دالّة `maxctl`) يقبلها
     * — فكان الإعداد الموقّع الواحد يُحسب في الخادم (إيقاف عدّاد المدفوع) ويُهمَل في التطبيق
     * (بلا فتح نافذة). طرفان يفترقان على نفس البايتات، والقبول هنا يُلغي الفرق من أصله.
     */
    fun instantMillis(text: String): Long? {
        val trimmed = text.trim()
        val normalized = if (DATE_ONLY.matches(trimmed)) "${trimmed}T00:00:00Z" else trimmed
        return runCatching { Instant.parse(normalized).toEpochMilli() }.getOrNull()
    }

    /**
     * الحكم الكامل على غلاف: التوقيع، ثم الصيغة، ثم الإصدار، ثم الزمن. **وأول فشل يُنهي.**
     *
     * @param floorRevision أعلى `revision` مقبول سابقًا على هذا الجهاز (صفر إن لم يُقبل شيء).
     */
    fun admit(
        envelope: SignedEnvelope,
        publicKeyBase64: String,
        floorRevision: Long,
        nowMillis: Long,
    ): Admission {
        val key = ConfigSignature.decodePublicKey(publicKeyBase64)
            ?: return Admission.Rejected(
                RemoteRejection.NOT_CONFIGURED,
                if (publicKeyBase64.isBlank()) "no public key in this build" else "public key is not an EC P-256 SPKI",
            )

        // ① التوقيع على البايتات بالضبط — قبل أي قراءة.
        val payloadBytes = runCatching { ConfigSignature.decodeBase64(envelope.payload) }.getOrNull()
            ?: return Admission.Rejected(RemoteRejection.MALFORMED, "payload is not base64")
        if (payloadBytes.isEmpty()) return Admission.Rejected(RemoteRejection.MALFORMED, "empty payload")
        if (payloadBytes.size > MAX_PAYLOAD_BYTES) {
            return Admission.Rejected(RemoteRejection.MALFORMED, "payload over $MAX_PAYLOAD_BYTES bytes")
        }
        if (!ConfigSignature.verify(payloadBytes, envelope.signature, key)) {
            return Admission.Rejected(RemoteRejection.SIGNATURE, "ECDSA P-256 verification failed")
        }

        // ② الصيغة تُقرأ بعد أن ثبت أن البايتات موقّعة.
        val payload = parsePayload(payloadBytes)
            ?: return Admission.Rejected(RemoteRejection.MALFORMED, "payload is not the config contract")

        // ③ صيغة أعلى من المدعوم ⇒ رفض كامل (لا قراءة جزئية لمفتاحٍ نفهمه).
        if (payload.schema > SCHEMA) {
            return Admission.Rejected(RemoteRejection.SCHEMA, "schema ${payload.schema} > $SCHEMA")
        }

        // ④ الغلاف المبدَّل: الإصدار الظاهر يجب أن يساوي الموقَّع.
        if (envelope.revision != payload.revision) {
            return Admission.Rejected(
                RemoteRejection.REVISION_MISMATCH,
                "envelope revision ${envelope.revision} != signed ${payload.revision}",
            )
        }

        // ⑤ مانع الرجوع: إعداد أقدم صالح التوقيع يُرفض صراحةً.
        if (payload.revision <= floorRevision) {
            return Admission.Rejected(
                RemoteRejection.REVISION,
                "revision ${payload.revision} <= $floorRevision",
            )
        }

        // ⑥ الزمن: لا مستقبليّ بفارق معقول، ولا خارج أي عمر ممكن.
        val issuedAt = instantMillis(payload.issuedAt)
            ?: return Admission.Rejected(RemoteRejection.ISSUED_INVALID, "issued_at is not ISO-8601")
        if (issuedAt > nowMillis + CLOCK_SKEW_MILLIS) {
            return Admission.Rejected(
                RemoteRejection.ISSUED_INVALID,
                "issued_at is ${(issuedAt - nowMillis) / 1000}s in the future",
            )
        }
        if (nowMillis - issuedAt > MAX_ISSUED_AGE_MILLIS) {
            return Admission.Rejected(RemoteRejection.ISSUED_INVALID, "issued_at is over ten years old")
        }
        val expiresAt = payload.expiresAt?.let(::instantMillis)
        if (payload.expiresAt != null && expiresAt == null) {
            return Admission.Rejected(RemoteRejection.ISSUED_INVALID, "expires_at is not ISO-8601")
        }
        if (expiresAt != null && expiresAt <= issuedAt) {
            return Admission.Rejected(RemoteRejection.ISSUED_INVALID, "expires_at is not after issued_at")
        }

        // ⑦ مفاتيح لا يعرفها هذا الإصدار: تُسجَّل ولا تُسقط الإعداد.
        val unknown = payload.features.keys.filterNot(RemoteFlags::isKnown)

        // ⑧ نوافذ المجّاني تُحلّ إلى مللي ثانية: نافذة لا تُقرأ أو منقلبة تُهمَل (لا تُسقط
        // الإعداد) — فالملفّ كلّه موقّع، والحرص هنا على الحساب لا على الثقة.
        val promos = payload.promos.take(MAX_PROMOS).mapNotNull { promo ->
            val from = instantMillis(promo.from) ?: return@mapNotNull null
            val to = instantMillis(promo.to) ?: return@mapNotNull null
            if (to <= from) null else PromoWindow(from, to, promo.note.take(160))
        }.sortedBy(PromoWindow::fromMillis)

        // ⑨ سجلّ التغييرات: يُقرأ للعرض فقط، وحدوده تُقصّ هنا (نصٌّ لا ملفّ) — وإصدارٌ بلا
        // رقم يُهمَل لأن ملاحظاتٍ بلا إصدار لا تُنسب في النافذة إلى شيء.
        val changelog = payload.changelog.take(MAX_CHANGELOG).mapNotNull { release ->
            val version = release.version.trim().take(24)
            if (version.isEmpty()) return@mapNotNull null
            fun lines(notes: List<String>) = notes
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(MAX_NOTE_LINES)
                .map { it.take(MAX_NOTE_CHARS) }
            RemoteRelease(
                version = version,
                date = release.date.trim().take(16),
                notesAr = lines(release.notesAr),
                notesEn = lines(release.notesEn),
            )
        }

        return Admission.Admitted(
            RemoteConfig(
                revision = payload.revision,
                issuedAtMillis = issuedAt,
                expiresAtMillis = expiresAt,
                features = payload.features,
                announcement = payload.announcement,
                latest = payload.latest,
                stats = payload.stats,
                subscription = payload.subscription,
                promos = promos,
                changelog = changelog,
                updates = payload.updates,
                unknownFeatureKeys = unknown.sorted(),
            ),
        )
    }
}
