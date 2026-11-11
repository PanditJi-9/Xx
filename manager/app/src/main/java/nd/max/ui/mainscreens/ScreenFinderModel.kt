/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * فهرس الشاشات — **مُوجِّد واحد لكل وجهة في التطبيق**.
 *
 * ولماذا وُلد: شاشة التحكّم تعرض نحو أربعين صفًّا في تسع مجالات، فمن يريد «مُحرّر القيم» أو
 * `FPSGO` أو «سياسة Doze» يشترط أن **يعرف مجاله أوّلًا** — ثلاث ضغطات ومعرفة سابقة. ومن
 * أراد شاشةً في الإعدادات أو `معلومات الجهاز` فطريقه مختلف أصلًا. والفهرس يجعل الطريق واحدًا:
 * اكتب حرفين من الاسم أو ممّا تضبطه الشاشة، فتُعرض الشاشة ومكانها.
 *
 * **وثلاثة قرارات مقيَّدة هنا لا في الشاشات:**
 *
 * 1. **المصدر هو السجلّ نفسه** (`MaxDestination.Visible`، وهي `All` ناقصَ ما أخفاه المالك) لا
 *    قائمة مكتوبة بيد: وجهة جديدة تُضاف في السجلّ تظهر في الفهرس بلا سطرٍ هنا، ولا يمكن أن يبقى
 *    في الفهرس وجهة محذوفة — ولا أن يعود سطحٌ مخفيٌّ من باب البحث.
 * 2. **والوجهة التي لا تُفتح بلا معرّف تبقى في الفهرس، لكن فعلها يقود إلى بدايتها.**
 *    `AppSettings` مسارها `app_settings/{pkg}` فلا قيمة افتراضية لمعرّفها — وكانت تُسقَط،
 *    فيقرأ من كتب «إعدادات التطبيق» «لا نتيجة» ويخرج بأنّ الشاشة غير موجودة. والصواب: السطر
 *    يبقى ويقول «اختر التطبيق أولًا»، وضغطه يفتح الأب الظاهر (قائمة التطبيقات) حيث يُختار —
 *    لا تفصيل حزمة اسمها فارغ (وهو الوعد الكاذب الذي يمنعه ADR-07)، ولا صفٌّ بلا فعل.
 *    ([ScreenFinderTarget.opensFrom] يحمل الأب، و[screenFinderOpenTarget] تحكم الفعل).
 * 3. **و«أين تسكن» من الشجرة لا من جدول ثانٍ:** أبُ الوجهة في السجلّ هو جواب «أين» (مجالها أو
 *    الإعدادات)، والتبويبات الأربعة تُعلن نفسها، ومن لا أب له يُفتح من الرئيسية. فلا خريطة
 *    موازية تنحرف عن السجلّ يومًا.
 * 3أ. **وما أخفاه المالك (`hidden`) لا يُعرض أيضًا** — وإلا صار البحث بابًا خلفيًّا لسطحٍ
 *    أُخفِي من الإعدادات عمدًا (أمر المالك ٢٠٢٦-١٠-٢١: إحصاءات المجتمع وإضافات الطرف الثالث).
 *    فالاستثناءان من الحقل نفسه: `hidden` ثم `needsLaunchArgument`، لا قائمة أسماء مكتوبة.
 *
 * **والجولة ٢٠٣ أضافت المعلومات الأربع التي يراها المستخدم في السطر نفسه** (طلب المالك:
 * «اعطي لفكرتك معلومات اكثر في الشاشات التي فعلتها جيد»):
 *
 * 4. **ما تضبطه الشاشة** (`role` من كتالوج السجلّ) و**أين تسكن** (`where` من الشجرة) — كانا.
 * 5. **وكم مرة فتحتها أنت** (`usage`): والعدد يُقرأ من `ScreenUsageStore` — السجلّ الواحد الذي
 *    تقرأ منه «منصة التحكم» أيضًا — فلا يقول المُوجِّد «الأكثر استعمالًا» ومنصةٌ أخرى تقول غيره.
 *    والعدد **مقيس لا مُصنَّع**: نحن من سجّله عند فتح الشاشة، ويبقى على الجهاز.
 * 6. **ووسم الخطورة حين تحمله الوجهة** (`riskNote` من `maxRiskLabel` نفسه الذي يوسم به
 *    الإعداد ‹أدوات متقدّمة›، ADR-16) — فلا يفتح المستخدم «مُحرّر القيم» بضغطة وهو يظنّها
 *    شاشة معلومات.
 * 7. **وما تفتحه أكثر يُعرض قبل أن تكتب حرفًا** ([screenFinderSuggestions]): الحقل الفارغ كان
 *    يشرح فقط، وصار يعمل: أربعة صفوف هي شاشاتك، ثم سطر الشرح تحتها.
 *
 * والصافي هنا لا يرسم: `ScreenFinderSheet` هو الذي يرسمه، و`ScreenFinderTest` يقيسه على JVM
 * (الترتيب · الاستثناءات · الطيّ العربي · العدّاد · الوسم).
 */
package nd.max.ui.mainscreens

import androidx.annotation.StringRes
import nd.max.R
import nd.max.ui.navigation.MaxDestination
import nd.max.ui.navigation.MaxRisk
import nd.max.ui.navigation.maxDestinationRole
import nd.max.ui.util.maxSearchFold
import nd.max.ui.util.maxSearchRankFolded

/**
 * وجهة جاهزة للعرض في نتائج البحث: الاسم الذي يراه المستخدم، وما تضبطه، ومكانها، وعدد فتحاتها.
 *
 * والنصوص تُمرَّر **محلولة** لا كمعرّفات مورد: الفهرس يقارن نصوصًا، والقارئ (`stringResource`
 * أو `Context.getString`) يعيش في طبقة الرسم، فيبقى هذا الملفّ صافيًا ومقيسًا بلا منصّة.
 */
data class ScreenFinderTarget(
    val destination: MaxDestination,
    val title: String,
    val role: String,
    val where: String,
    /** عدد فتحات هذه الشاشة على هذا الجهاز؛ و٠ تعني «لم تُفتح بعدُ» لا «غير معروفة». */
    val usage: Int = 0,
    /** وسم الخطورة إن كانت الوجهة موسومة بها، و`null` للوجهة العادية — لا نصّ يُصنَّع لها. */
    val riskNote: String? = null,
    /**
     * كلمات القصد التي ينطق بها المستخدم ولا تحملها أسماء الشاشات («شحن» · «سخونة» · «٦٠»).
     * لا تُعرض أبدًا؛ تُطابَق فقط. والقائمة في [maxDestinationKeywords].
     */
    val keywords: List<String> = emptyList(),
    /**
     * الوجهة التي **تُفتح فعلًا** حين لا يُفتح هذا السطر نفسه.
     *
     * `app_settings/{pkg}` و`max_perms?pkg={pkg}` و`max_backup?pkg={pkg}` لا تُفتح بلا اسم
     * حزمة، فكانت تُسقَط من الفهرس صامتةً: من كتب «إعدادات التطبيق» قرأ «لا نتيجة» وخرج
     * بأنّ الشاشة غير موجودة. والصواب أن يُقال له من أين يبدأ: صفحة التطبيقات (أو الإعدادات)
     * هي المكان الذي يُختار فيه التطبيق. فالسطر يبقى في الفهرس، **وفعله يقود إلى البداية**
     * لا إلى الشاشة نفسها — ولا صفّ بلا فعل، ولا وعد بضغطة تفتح شيئًا آخر.
     */
    val opensFrom: MaxDestination? = null,
)

/**
 * يبني الفهرس من السجلّ. [resolve] يحوّل معرّف مورد إلى نصّه في لغة الواجهة الحالية، و[usage]
 * هو سجلّ الفتحات المحلّي (غائب ⇒ لا نتيجة بعد، وكلّها بصفر).
 */
fun screenFinderTargets(
    resolve: (Int) -> String,
    usage: Map<String, Int> = emptyMap(),
    access: nd.max.ui.navigation.DestinationAccess? = null,
): List<ScreenFinderTarget> {
    val visible = MaxDestination.Visible.filter { access?.visible(it) != false }
    // **المحور ذو الابن الواحد بابٌ لا شاشة.** `GpuHub` · `ThermalHub` · `AudioHub` لكلٍّ ابنٌ
    // واحد، فصفحتها سطرٌ واحد يقود إلى الشاشة الحقيقية. وعرضها في نتائج البحث يعطي نقرةً كاملة
    // مقابل صفر معلومة: تكتب «GPU» فتصل إلى «GPU» ثم إلى «GPU Studio». فيُسقَط الباب ويُعرض ما
    // خلفه مباشرةً — والابن نفسه في الفهرس أصلًا فلا تُفقد وجهة.
    //
    // والشرط ليس «ابن واحد» وحده: لو كان الابن الوحيد يحتاج وسيط مسار (لا يُفتح من الفهرس) لَما
    // جاز إسقاط الأب، وإلّا لاختفى الفرع كلّه من البحث. ولذلك يُشترط أن يكون الابن صالحًا للفهرس.
    val passThrough = visible
        .groupBy { it.parent }
        .filterValues { children -> children.size == 1 && !children.single().needsLaunchArgument }
        .keys
        .filterNotNull()
        .toSet()
    return visible
        // **وما يحتاج معاملًا لا يُسقَط، بل يُذكر من أين يبدأ.** الشرط أن يكون له أبٌ ظاهر
        // يُفتح بلا معامل — وإلا فلا مسار صادقًا، ويبقى الإسقاط أصدق من صفٍّ لا يقود إلى شيء.
        .filter { destination ->
            !destination.needsLaunchArgument ||
                destination.parent?.let { it in visible && !it.needsLaunchArgument } == true
        }
        .filterNot { it in passThrough }
        .map { destination ->
            ScreenFinderTarget(
                destination = destination,
                opensFrom = destination.parent?.takeIf { destination.needsLaunchArgument },
                title = resolve(destination.titleRes),
                role = resolve(maxDestinationRole(destination)),
                where = resolve(screenFinderWhereRes(destination)),
                usage = usage[destination.route] ?: 0,
                riskNote = screenFinderRiskRes(destination)?.let(resolve),
                keywords = maxDestinationKeywords[destination].orEmpty(),
            )
        }
}

/** «أين تسكن هذه الشاشة» — من الشجرة في السجلّ لا من جدول ثانٍ. */
@StringRes
internal fun screenFinderWhereRes(destination: MaxDestination): Int = when {
    destination.isPrimary -> R.string.screen_finder_where_bar
    destination.parent != null -> destination.parent.titleRes
    else -> R.string.max_nav_now
}

/**
 * وسم الخطورة — من الجدول الواحد (`maxRiskLabel` الذي يوسم به الإعداد ‹أدوات متقدّمة›)،
 * ولا جدول ثانٍ للوسم نفسه؛ والوجهة العادية لا وسم لها أصلًا فلا تُكتب «عادي» في كل صفّ.
 */
@StringRes
internal fun screenFinderRiskRes(destination: MaxDestination): Int? =
    if (destination.risk == MaxRisk.Normal) null else maxRiskLabel(destination.risk)

/**
 * نتائج [query] مرتّبةً: اسمُ الشاشة أوّلًا، ثم ما تضبطه، ثم مجالك. وداخل الرتبة نفسها
 * **الأكثر فتحًا أوّلًا** (فمن كتب «الحرارة» مرّتين في يومين يريد شاشته هو قبل شاشةٍ لم يفتحها)،
 * ثم ترتيبٌ أبجديّ مطويّ — لا ترتيبًا يعتمد على ترتيب السجلّ، فيتغيّر بترتيب لا يراه المستخدم.
 *
 * والفراغ ⇒ لا نتائج (لا «كل الشاشات»): القائمة كلها على الشاشة أصلًا، وردُّها هنا تكرار.
 */
fun screenFinderResults(
    query: String,
    targets: List<ScreenFinderTarget>,
    limit: Int = 24,
): List<ScreenFinderTarget> {
    val needle = maxSearchFold(query).trim()
    if (needle.isEmpty()) return emptyList()
    // الكلمات بعد إسقاط النحو: «أوقف الشحن الآن» ⇒ «شحن». ولا تُستعمل إلا حين تعجز العبارة.
    val sentenceTokens = maxSearchTokens(needle)

    return targets
        .mapNotNull { target ->
            val titleRank = maxSearchRankFolded(target.title, needle)
            val keywordRank = if (titleRank < 0) {
                maxSearchRankFolded(target.keywords.joinToString(" "), needle)
            } else {
                -1
            }
            val roleRank = if (titleRank < 0 && keywordRank < 0) {
                maxSearchRankFolded(target.role, needle)
            } else {
                -1
            }
            val whereRank = if (titleRank < 0 && keywordRank < 0 && roleRank < 0) {
                maxSearchRankFolded(target.where, needle)
            } else {
                -1
            }
            val score = when {
                titleRank >= 0 -> titleRank
                keywordRank >= 0 -> KEYWORD_BASE + keywordRank
                roleRank >= 0 -> ROLE_BASE + roleRank
                whereRank >= 0 -> WHERE_BASE + whereRank
                // **والجملة تعمل كما تعمل الكلمة** (فكرة §٣-٥ «أوامر بالقصد»): ما لم تُطابق
                // العبارة كاملةً — والمستخدم يكتب «أوقف الشحن الآن» لا «شحن» — تُطابَق كلماتها
                // بعد إسقاط نحو الكلام وحده. والنتيجة التي وُجدت فيها **كل** الكلمات تتقدّم.
                else -> maxSentenceRank(target, sentenceTokens) ?: return@mapNotNull null
            }
            ScreenFinderHit(target, score, maxSearchFold(target.title))
        }
        .sortedWith(
            compareBy<ScreenFinderHit>({ it.score }, { -it.target.usage }, { it.foldedTitle })
        )
        .take(limit.coerceAtLeast(0))
        .map { it.target }
}

/**
 * **عدد كل المطابقات، بلا سقف — ليُعلَن ما لم يُعرض بدل أن يُقطع صامتًا.**
 *
 * `screenFinderResults` تسقُف ما تعرضه (`limit = 24`) كي تبقى الورقة ورقة. لكن السقف بلا
 * إعلان يعني أن من يكتب «ال» يرى أربعة وعشرين صفًّا ويظنّ أن لا نتيجة غيرها فيتوقّف. فتُقاس
 * المطابقات كلها هنا (الفهرس بضع عشرات، فالحساب رخيص) وتُعلن الورقة الفرق.
 *
 * ولا تُعاد النتائج نفسها مرّتين في الذاكرة: الورقة تعرض المسقوف وتقرأ العدد وحده.
 */
fun screenFinderMatchCount(query: String, targets: List<ScreenFinderTarget>): Int =
    screenFinderResults(query, targets, limit = Int.MAX_VALUE).size

/**
 * **أوّل نتيجة — أي ما يفتحه مفتاح Enter.** لا ترتيب ثانٍ: هي أوّل ما تعرضه الورقة نفسها،
 * فالمفتاح لا يفتح شيئًا لا يراه المستخدم في أعلى القائمة. والفراغ ⇒ `null`، فلا فعل لمفتاح
 * على حقلٍ لم يُكتب فيه.
 */
fun screenFinderTopHit(query: String, targets: List<ScreenFinderTarget>): ScreenFinderTarget? =
    screenFinderResults(query, targets, limit = 1).firstOrNull()

/**
 * **ما يفتحه السطر فعلًا.** سطرٌ لوجهة تحتاج معاملًا يقود إلى أبيه الظاهر (قائمة التطبيقات أو
 * الإعدادات) حيث يُختار التطبيق؛ وما عدا ذلك يقود إلى وجهته. ودالّة واحدة تحكم هذا القرار
 * كي لا يختلف سلوك مفتاح Enter عن سلوك الضغط على الصفّ.
 */
fun screenFinderOpenTarget(target: ScreenFinderTarget): MaxDestination =
    target.opensFrom ?: target.destination

/** كم صفًّا يُقترح قبل أن يكتب المستخدم حرفًا — صفٌّ يُقرأ لا قائمةً تُمرَّر. */
const val SCREEN_FINDER_SUGGESTIONS = 4

/**
 * «ما تفتحه أكثر» — الحقل الفارغ لم يعد يشرح فقط، بل يعمل.
 *
 * **ولا اقتراح بلا سجلّ:** من لم يفتح شيئًا (أو أوّل تشغيل) تُعاد قائمة فارغة، ويُعرض سطر الشرح
 * وحده — ولا تُعرض «اقتراحات» من الصفر هي أعلى الصفر، فهذا ترتيب لا مقياس خلفه.
 *
 * **ولا تُقترح الشاشة التي أنت واقف عليها** (`exclude`): الورقة تُفتح من «التحكّم» ومن
 * «الإعدادات»، وهما أكثر شاشتين تُفتحان في التطبيق أصلًا — فأوّل ما كان يُقترح هو المضيف نفسه،
 * والصفّ لا يفعل شيئًا عند الضغط لأنك فيه. والأسوأ أنه يأكل مقعدًا من أربعة، فيظهر ثلاثة
 * اقتراحات ويُقرأ السقف كأنه كل ما في السجلّ. والاستبعاد **للاقتراح وحده**: من كتب «الإعدادات»
 * يجدها في النتائج، لأن البحث سؤال صريح والاقتراح تخمين.
 */
fun screenFinderSuggestions(
    targets: List<ScreenFinderTarget>,
    limit: Int = SCREEN_FINDER_SUGGESTIONS,
    exclude: Set<String> = emptySet(),
): List<ScreenFinderTarget> = targets
    // **وما لا يُفتح من هنا لا يُقترح:** «أكثر ما تفتحه» يعني الضغط عليه يفتحه. وصفٌّ يفتح
    // قائمة التطبيقات بدلًا منه يُقرأ عطبًا لا اقتراحًا — فيُبقى للبحث وحده، حيث يُشرح سببه.
    .filter { it.usage > 0 && it.opensFrom == null && it.destination.route !in exclude }
    .sortedWith(
        compareByDescending<ScreenFinderTarget> { it.usage }.thenBy { maxSearchFold(it.title) }
    )
    .take(limit.coerceAtLeast(0))

/** رتبةٌ واحدة تُحسب ثم تُرمى — لا تُعرَّض خارج الملفّ. */
private data class ScreenFinderHit(
    val target: ScreenFinderTarget,
    val score: Int,
    val foldedTitle: String,
)

/** مطابقةٌ في سطر الوصف تُرتَّب بعد كل مطابقات الاسم، ومطابقةٌ في المكان بعدها. */
private const val KEYWORD_BASE = 5
private const val ROLE_BASE = 10
private const val WHERE_BASE = 20

/** عقوبة «بعض الكلمات وُجدت»: تُبقي الجملة الكاملة قبل الجملة الناقصة، ولا تُسقطها. */
private const val SENTENCE_PARTIAL_PENALTY = 8

/**
 * كلمات لا تحمل قصدًا في العربية ولا الإنجليزية — تُسقَط قبل المطابقة كلماتٍ كلمات.
 *
 * **ولا تُطبع ولا تُعرض:** لا تظهر في الواجهة ولا تُترجم؛ مكانها هنا لأنها نحْوُ المستخدم
 * لا محتوى الشاشة. والقائمة محصورة في النحو الخالص: من أسقط كلمةً تحمل معنى («شغل» · «حذف»)
 * جعل الجملة تعمل على غير قصدها.
 */
internal val maxSearchFiller: Set<String> = setOf(
    // عربية: طلب ونحو
    "اريد", "ابغى", "ابي", "كيف", "اين", "وين", "هل", "الان", "الان", "من", "في", "عن",
    "الي", "علي", "على", "لي", "له", "و", "او", "ثم", "افتح", "اذهب", "انتقل", "شاشه",
    // إنجليزية
    "open", "go", "to", "the", "a", "an", "how", "do", "does", "i", "can", "please", "show",
    "me", "now", "for", "of", "and", "or", "screen", "settings", "page",
)

/**
 * كلمات الاستعلام بعد الطيّ وإسقاط [maxSearchFiller]، بلا تكرار، وبلا محرف واحد
 * (الحرف الواحد يطابق كل شيء في العربية).
 */
internal fun maxSearchTokens(foldedNeedle: String): List<String> =
    foldedNeedle
        .split(' ', '\t', ',', '.', '،', '؛', ';', '؟', '?', '!', '\n')
        .map { it.trim() }
        .filter { it.length >= 2 && it !in maxSearchFiller }
        .distinct()

/**
 * رتبة الجملة: أفضل مجال تطابقه كلمةٌ منها، وعقوبةٌ إن لم تُوجد كلّها.
 * تعود `null` إن لم تطابق كلمةٌ واحدة — فلا نتيجة من لا شيء.
 */
private fun maxSentenceRank(target: ScreenFinderTarget, tokens: List<String>): Int? {
    if (tokens.isEmpty()) return null
    val fields = listOf(
        0 to target.title,
        KEYWORD_BASE to target.keywords.joinToString(" "),
        ROLE_BASE to target.role,
        WHERE_BASE to target.where,
    )
    var best: Int? = null
    var hits = 0
    tokens.forEach { token ->
        var tokenBest: Int? = null
        // والكلمة العربية تلتصق بها «ال»: «الشحن» في جملة المستخدم هي «شحن» في القاموس.
        val variants = buildList {
            add(token)
            if (token.length >= 4 && token.startsWith("ال")) add(token.removePrefix("ال"))
        }
        variants.forEach { word ->
            fields.forEach { (base, text) ->
                val rank = maxSearchRankFolded(text, word)
                if (rank >= 0) {
                    val score = base + rank
                    if (tokenBest == null || score < tokenBest!!) tokenBest = score
                }
            }
        }
        tokenBest?.let {
            hits++
            if (best == null || it < best!!) best = it
        }
    }
    val found = best ?: return null
    return if (hits == tokens.size) found else found + SENTENCE_PARTIAL_PENALTY
}

/**
 * **كلمات القصد** — الجواب الوحيد المكتوب بيد في الفهرس، لأنه لا يُشتقّ من السجلّ.
 *
 * ولماذا لزمت: أسماء الشاشات تحمل الاسم التقنيّ («شحن التجاوز» · `FPSGO` · «ZRAM»)، والمستخدم
 * يبحث بما يريد أن **يحدث** («شحن سريع» · «سخونة» · «٦٠» · «bypass»). والوسم `role` يشرح
 * الشاشة ولكن بلسان المنتج، لا بلسان من كتب «بطارية».
 *
 * **ولغتان معًا في كل مدخل، لا لغة الواجهة وحدها:** العربية في واجهة إنجليزية تُكتب «شحن»،
 * والإنجليزية في واجهة عربية تُكتب `bypass`. والكلمات هنا **لا تُعرض أبدًا**؛ تُطابَق فقط، فلا
 * تحتاج ترجمة ولا تظهر في أيّ صفّ. ولا تفتح وجهةً مخفيّة: الفهرس يبني من `Visible` أولًا،
 * والمدخل لوجهة غير معروضة يُهمَل.
 *
 * **وحدّها:** لا تُغني عن الاسم، وتأتي في الترتيب بعد مطابقة الاسم وقبل الوصف؛ فمن كتب اسم الشاشة
 * كما هو لا يرى نتيجةً هامشية تسبقها. والاختبار `ScreenFinderTest` يقيس ثلاثتها.
 */
internal val maxDestinationKeywords: Map<MaxDestination, List<String>> = mapOf(
    MaxDestination.Charging to listOf("شحن", "بطارية", "charging", "battery", "power"),
    MaxDestination.BypassCharging to listOf("تجاوز", "شحن مباشر", "bypass", "fast charge"),
    MaxDestination.BypassChargingCheck to listOf("فحص التجاوز", "bypass check"),
    MaxDestination.BatteryHistory to listOf("بطارية", "عمر البطارية", "battery history"),
    MaxDestination.DozeMode to listOf("سكون", "نوم", "doze", "sleep"),
    MaxDestination.ThermalDetail to listOf("حرارة", "سخونة", "تليمت", "thermal", "heat", "throttle"),
    MaxDestination.ZramManager to listOf("رام", "ذاكره", "ضغط", "zram", "ram", "swap"),
    MaxDestination.MemoryUfs to listOf("تخزين", "ذاكره", "ufs", "storage"),
    MaxDestination.Dex2oat to listOf("تجميع", "تسريع", "dex2oat", "compile"),
    MaxDestination.FpsGo to listOf("اطارات", "العاب", "٦٠", "60", "fps", "frames", "game"),
    MaxDestination.FpsOverlay to listOf("عداد", "اطارات", "hud", "fps", "overlay"),
    MaxDestination.GameSessions to listOf("جلسات", "جلسه", "العاب", "تسجيل", "sessions", "session", "record", "game"),
    MaxDestination.Resolution to listOf("دقه", "ابعاد", "شاشه", "resolution", "dpi", "display"),
    MaxDestination.DisplayStudio to listOf("شاشه", "عرض", "سطوع", "display", "screen", "brightness"),
    MaxDestination.NetworkScheduler to listOf("شبكه", "نت", "بينغ", "network", "ping", "latency"),
    MaxDestination.StorageCare to listOf("تنظيف", "مساحه", "storage", "clean", "space"),
    MaxDestination.StorageDetail to listOf("تخزين", "مساحه", "storage", "space"),
    MaxDestination.DebloatFreeze to listOf("تجميد", "تعطيل", "تطبيقات النظام", "debloat", "freeze"),
    MaxDestination.GovernorSettings to listOf("حاكم", "اداء", "governor", "performance"),
    MaxDestination.CpuCoreControl to listOf("تردد", "نواه", "معالج", "cpu", "cores", "frequency"),
    MaxDestination.GpuStudio to listOf("رسوم", "معالج رسومي", "gpu", "graphics"),
    MaxDestination.AudioStudio to listOf("صوت", "سماعات", "مكبر", "audio", "sound", "speaker"),
    MaxDestination.Diagnostics to listOf("تشخيص", "حكام", "ملكيه", "diagnostics", "ownership"),
    MaxDestination.MaxLive to listOf("مراقبه", "بث", "حي", "live", "monitor", "realtime"),
    MaxDestination.Logs to listOf("سجلات", "اخطاء", "logs", "errors"),
    MaxDestination.ModuleHealth to listOf("وحده", "سلامه", "module", "health"),
    MaxDestination.PreferenceTweaks to listOf("قيم", "محرر", "props", "tweaks"),
    MaxDestination.ProcessManager to listOf("عمليات", "خلفيه", "process", "background"),
    MaxDestination.DynamicIsland to listOf("جزيره", "island", "notch"),
    MaxDestination.MaxAi to listOf("ذكاء", "محرك", "ai", "max ai"),
    MaxDestination.ColorScheme to listOf("لون", "سمه", "theme", "color"),
    // ── الباقي (تغطية كاملة): كل وجهة معروضة تُفتح بلا معرّف لها كلمة قصد واحدة على الأقل.
    //    ولو نقصت واحدة لفشل `ScreenFinderTest` — لا تُترك شاشة «لا يعرفها البحث إلا باسمها».
    MaxDestination.Now to listOf("الرئيسيه", "الان", "home", "now", "dashboard"),
    MaxDestination.Apps to listOf("التطبيقات", "تطبيقات", "apps", "installed"),
    MaxDestination.Control to listOf("التحكم", "تحكم", "control", "tools"),
    MaxDestination.Settings to listOf("الاعدادات", "اعدادات", "settings", "preferences"),
    MaxDestination.MaxAiIntro to listOf("الذكاء الاصطناعي", "max ai", "ai intro"),
    MaxDestination.DeviceInfo to listOf("معلومات الجهاز", "مواصفات", "device info", "specs"),
    MaxDestination.GetStarted to listOf("البدء", "دليل", "get started", "onboarding", "guide"),
    MaxDestination.CpuHub to listOf("المعالج", "cpu", "processor"),
    MaxDestination.GpuHub to listOf("الرسوم", "gpu", "graphics"),
    MaxDestination.MemoryHub to listOf("الذاكره", "ram", "memory"),
    MaxDestination.DisplayHub to listOf("الشاشه", "display", "screen"),
    MaxDestination.ResponsivenessHub to listOf("الاستجابه", "السلاسه", "responsiveness", "smooth"),
    MaxDestination.ThermalHub to listOf("الحراره", "thermal", "heat", "temperature"),
    MaxDestination.PowerHub to listOf("الطاقه", "البطاريه", "power", "battery"),
    MaxDestination.StorageHub to listOf("التخزين", "storage", "disk"),
    MaxDestination.NetworkHub to listOf("الشبكه", "network", "wifi", "internet"),
    MaxDestination.AudioHub to listOf("الصوت", "audio", "sound"),
    MaxDestination.Fas to listOf("جدوله", "الاطارات", "fas", "frame", "scheduling"),
    MaxDestination.NetworkDetail to listOf("تفاصيل الشبكه", "network detail", "wifi detail"),
    MaxDestination.DeviceLab to listOf("مختبر", "اختبار الجهاز", "device lab", "test"),
    MaxDestination.UsageStats to listOf("الاستخدام", "وقت الشاشه", "usage", "screen time"),
    MaxDestination.License to listOf("الترخيص", "license", "legal"),
    MaxDestination.ColorPalette to listOf("المظهر", "الالوان", "theme", "palette"),
    MaxDestination.ConfigBackup to listOf("نسخه احتياطيه", "backup", "restore", "config"),
    MaxDestination.MaxBackup to listOf("نسخ احتياطي للتطبيق", "app backup", "backup app"),
    MaxDestination.Permissions to listOf("الصلاحيات", "appops", "permissions"),
    MaxDestination.Privilege to listOf("روت", "امتياز", "root", "su"),
    MaxDestination.About to listOf("حول", "عن التطبيق", "about", "version"),
    MaxDestination.GameLobby to listOf("الالعاب", "لوبي", "games", "game lobby"),
    MaxDestination.EmulatorHub to listOf("المحاكي", "محاكيات", "emulator", "emulators"),
    MaxDestination.HmaCompanion to listOf("اخفاء التطبيقات", "hma", "companion", "hide apps"),
    MaxDestination.SetEdit to listOf("تعديل القيم", "setedit", "properties", "props"),
    MaxDestination.ActivityLauncher to listOf("الانشطه", "activity", "launcher", "shortcut"),
    MaxDestination.FileManager to listOf("الملفات", "مدير الملفات", "files", "file manager"),
)

