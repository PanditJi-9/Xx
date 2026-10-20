/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * الجزيرة الديناميكية — **قرار العرض وحده**، بلا أندرويد وبلا نافذة وبلا خدمة.
 *
 * العائلة كلها تحلّ محلّ ما لا يوجد: كبسولة حيّة واحدة تجمع ما يهمّ **الآن**. وقرارها هنا
 * ليُقاس على JVM بلا جهاز: أيّ مصدر يُعرض، ومن يسقط لمن، ومتى يسقط الإشعار العابر.
 *
 * **وأربعة عقود مثبّتة بالنصّ:**
 *
 * 1. **الأولوية مرتّبة لا مصادفة:** إشعار عابر ← وسائط ← شحن ← مؤشرات ← مخفي. والعابر أولًا
 *    لأنه **حدث** (وقعت الآن، ويُنسى)، وما بعده **حالات** (ما زالت قائمة). وحدث يقع يجب أن
 *    يُرى فورًا، وحالة لا تُخفي حدثًا وقع بعده.
 * 2. **العابر يسقط بمؤقّت:** مدّته بحسب أهميته (٨/٥/٣ ثوانٍ)، فما يُعرض ثانيةً واحدة ويبقى
 *    ثانيةً واحدة. والحالات لا مؤقّت لها: تنتهي بانتهاء مصدرها (الشحن يُنزع، الوسائط تُوقف).
 * 3. **الترتيب ليس تفضيلًا:** لا يُختار من الشاشة — الأولوية عقد في هذا الملف. وما تختاره
 *    الشاشة **أيّ مصادر تُقرأ أصلًا** (مفتاح لكل مصدر) — إسكات مصدر لا تفضيل مصدر.
 * 4. **الغائب غائب:** مصدر لا قراءة له لا يُخترع له محتوى؛ ومحتوى بلا معلومة (`Indicators`
 *    بثلاث قيم `null`) لا يُعرض — وإلا عرضت الجزيرة كبسولةً فارغة تُقرأ عطبًا.
 */
package nd.max.ui.util

/** ما تعرضه الجزيرة الآن — والخفيّ حالة صريحة لا غياب قيمة. */
sealed interface IslandContent {

    /** إشعار عابر وصل من المستمع، باسم تطبيقه ونصّه ونطاقه (لشعاره). */
    data class Transient(
        val appLabel: String?,
        val title: String,
        val text: String?,
        val packageName: String? = null
    ) : IslandContent

    /** ما يُشغَّل الآن — من أيّ تطبيق يملك جلسة وسائط. */
    data class Media(
        val title: String,
        val artist: String?
    ) : IslandContent

    /** الشحن: النسبة وحدها (الواط في لوحة الأداء لمن أراد الرقم الكامل). */
    data class Charging(val percent: Int) : IslandContent

    /**
     * مؤشرات حيّة من قارئ اللوحة المشترك. وكل قيمة قابلة للغياب على حِدة — من قرأ الإطارات
     * ولم يقرأ الحمل يُعرض له ما قيس، لا صفر مكان ما لم يُقس.
     */
    data class Indicators(
        val frames: Int?,
        val cpuPercent: Int?,
        val heatC: Float?
    ) : IslandContent

    /** لا شيء يُعرض: الكبسولة تُطوى ولا يُرسم لها شيء. */
    data object Hidden : IslandContent
}

/**
 * إشعار عابر كما وصل من المستمع.
 *
 * @param postedAtMs زمن النشر كما رآه النظام (`StatusBarNotification.postTime`) لا زمن
 *   وصوله إلى هنا — فإشعار نشرته المنصّة قبل لحظة يبقى له من عمره ما بقي، لا عمر جديد.
 * @param importance أهمية الإشعار الخام (`NotificationManager.IMPORTANCE_*`) — تُقرأ لتحديد
 *   مدّة العرض، وتُقرأ في `IslandModel` بأرقامها المعروفة لا باستيراد أندرويد (الملف نقيّ).
 * @param packageName حزمة الناشر — **نصّ لا أيقونة**: النموذج يبقى نقيًّا يُقاس على JVM،
 *   والأيقونة تُقرأ من الحزمة **لحظة الرسم** (طبقة العرض) فتبدو البطاقة كما تبدو في رومات
 *   المصانع: شعار التطبيق لا اسمه وحده. وحزمة لا تُقرأ ⇒ شعار بحرف الاسم الأول، لا فراغ.
 */
data class IslandNotice(
    val appLabel: String?,
    val title: String,
    val text: String?,
    val postedAtMs: Long,
    val importance: Int,
    val packageName: String? = null
)

/** المصادر الأربعة كما هي الآن — ومنها يُبنى قرار واحد. */
data class IslandInputs(
    val notice: IslandNotice? = null,
    val media: IslandContent.Media? = null,
    val charging: IslandContent.Charging? = null,
    val indicators: IslandContent.Indicators? = null
)

/**
 * @param expiresAtMs متى يسقط هذا المحتوى بذاته، أو `null` لِما لا يسقط بمؤقّت (حالة تنتهي
 *   بانتهاء مصدرها). والفارق مقصود: العابر **يجب** أن يسقط ولو بقي مصدره، والحالة لا تسقط
 *   وهي قائمة.
 */
data class IslandState(
    val content: IslandContent = IslandContent.Hidden,
    val expiresAtMs: Long? = null
)

object IslandModel {

    /**
     * عتبات الأهمية بأرقامها الخام.
     *
     * **ولماذا لا تُستورد من أندرويد:** هذا الملف يُقاس على JVM، واستيراد `NotificationManager`
     * كان سيجرّ أندرويد إلى اختبار لا يحتاجه. والقيم ثابتة في المنصّة منذ API 24 ولم تتغيّر.
     */
    const val IMPORTANCE_DEFAULT = 3
    const val IMPORTANCE_HIGH = 4

    /** مدّة عرض الإشعار العابر بحسب أهميته: ٨ ثوانٍ للمهمّ، ٥ للعادي، ٣ لما دونه. */
    fun ttlMsForImportance(importance: Int): Long = when {
        importance >= IMPORTANCE_HIGH -> 8_000L
        importance == IMPORTANCE_DEFAULT -> 5_000L
        else -> 3_000L
    }

    /**
     * القرار: أعلى مصدر فعّال، أو [IslandContent.Hidden].
     *
     * والعابر يُفحص بعمره: إشعار نشر قبل انتهاء مدّته يُعرض ببقيّتها، وإشعار منتهٍ لا يحجب
     * ما بعده — فوسائط تعمل الآن أهمّ من إشعار نُسي.
     *
     * @param ttlScale مضاعف مدّة العابر من تفضيل المستخدم (قصير/عادي/طويل). ومدّة **الحالات**
     *   لا تتأثّر به: الحالة تنتهي بانتهاء مصدرها لا بمؤقّت يُطال أو يُقصّ. وما دون الصفر أو
     *   يساويه يُطرح (يعود إلى ١) فلا يُنتج إشعارًا ميّتًا في لحظة عرضه.
     */
    fun decide(inputs: IslandInputs, nowMs: Long, ttlScale: Float = 1f): IslandState {
        val scale = ttlScale.takeIf { it > 0f } ?: 1f
        inputs.notice?.let { notice ->
            val expiresAtMs = notice.postedAtMs + (ttlMsForImportance(notice.importance) * scale).toLong()
            if (nowMs < expiresAtMs) {
                return IslandState(
                    content = IslandContent.Transient(
                        appLabel = notice.appLabel,
                        title = notice.title,
                        text = notice.text,
                        packageName = notice.packageName
                    ),
                    expiresAtMs = expiresAtMs
                )
            }
        }
        inputs.media?.let { return IslandState(it) }
        inputs.charging?.let { return IslandState(it) }
        inputs.indicators?.takeIf { it.hasAnyValue }?.let { return IslandState(it) }
        return IslandState()
    }

    /** مؤشرات كل قيمها غائبة ليست محتوى — كبسولة بلا رقم تُقرأ عطبًا لا هدوءًا. */
    private val IslandContent.Indicators.hasAnyValue: Boolean
        get() = frames != null || cpuPercent != null || heatC != null
}
