/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.ui.theme

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.Contrast
import nd.max.R
import nd.max.ui.design.MaxCardStyle

/**
 * «استوديو السمة» — عقد خيارات التخصيص الخمسة الجديدة في مكان واحد.
 *
 * **ولماذا عقدٌ جديد أصلًا:** كانت شاشة السمة تقرأ ثلاث قيم (`color_mode` · `key_color` ·
 * `color_spec`) وتكتبها في مواضع متفرقة. والخيارات الجديدة (تباين · استدارة · حجم نص ·
 * شكل شريط التنقل · حركة) تحتاج القراءة في **المعمار المركزي** (`MaxManagerTheme`) وفي
 * الشاشة وفي `MainActivity` — فلو كُتب مفتاحٌ نصًّا في ثلاثة مواضع لافترقت النسخ عند أول
 * تصحيح. هنا: المفتاح مرّة، والقراءة مرّة، والافتراضيّ مرّة.
 *
 * **والقراءة متسامحة عمدًا:** قيمة غائبة أو مجهولة (نسخة أقدم، ملفّ تفضيلات معدّل يدويًّا)
 * تعود إلى الافتراضيّ بدل أن تُسقط الشاشة — بلا استثناء ولا `!!`.
 */

/** تباين الألوان — يُمرَّر إلى مولّد materialkolor مباشرةً. */
enum class ThemeContrast(val key: String, val level: Double) {
    /** الافتراضيّ المولَّد من البذرة — ما كان عليه التطبيق قبل وجود هذا الخيار. */
    DEFAULT("DEFAULT", Contrast.Default.value),

    /** تباين أعلى قليلًا: يفيد الشاشات اللامعة بلا أن يصرخ. */
    MEDIUM("MEDIUM", Contrast.Medium.value),

    /** أعلى درجة: نصوص وحوافّ أقرب إلى الأسود/الأبيض. */
    HIGH("HIGH", Contrast.High.value);

    companion object {
        fun fromKey(value: String?): ThemeContrast =
            entries.firstOrNull { it.key == value } ?: DEFAULT
    }
}

/**
 * استدارة الزوايا — معامل يُضرَب في رموز `MaxRadius` كافّة عند نقطة الاستهلاك الواحدة.
 *
 * والقيم الثلاث تُقاس على `MaxCardSpec.radius = 22dp`: 11 · 22 · 33 تقريبًا — أي «حادّة»
 * تبقى متماسكة و«دائريّة» لا تصل إلى حدّ الكبسولة.
 */
enum class ThemeRoundness(val key: String, val scale: Float) {
    SHARP("SHARP", 0.50f),
    STANDARD("STANDARD", 1.00f),
    ROUND("ROUND", 1.50f);

    companion object {
        fun fromKey(value: String?): ThemeRoundness =
            entries.firstOrNull { it.key == value } ?: STANDARD
    }
}

/**
 * حجم النص — معامل يُضرَب في `Typography` (15 نمطًا) عند بناء السمة.
 *
 * وحدود المجرى صغيرة عمدًا (٠٫٩ – ١٫١٢): نسبة أوسع تكسر شبكات الشاشات الضيّقة، ومن
 * يريد أكبر من ذلك عنده تكبير النظام نفسه.
 */
enum class ThemeFontScale(val key: String, val scale: Float) {
    SMALL("SMALL", 0.90f),
    STANDARD("STANDARD", 1.00f),
    LARGE("LARGE", 1.12f);

    companion object {
        fun fromKey(value: String?): ThemeFontScale =
            entries.firstOrNull { it.key == value } ?: STANDARD
    }
}

/**
 * كثافة عرض المعلومات (§٦-ج) — المقياسُ الذي يُضرب به **الحشو الرأسي وفواصل الأقسام وحشو
 * البطاقات**، لا حجم النصّ (له محوره [ThemeFontScale]) ولا أرض اللمس (تبقى ٤٨dp أبدًا).
 *
 * **والافتراضيّ [STANDARD] = سلوك اليوم بالضبط** (مقياس ١)، فلا يتغيّر شيء عند من لم يطلب.
 * وحدود المجرى محسوبة: ٠٫٧٨ تضغط الحشو ١٦dp إلى ١٢٫٥dp فلا ينزل تحت حدّ القراءة،
 * و١٫٢٨ ترفعه إلى ٢٠٫٥dp فلا ينقلب الصفّ إلى فجوة.
 */
enum class ThemeDensity(val key: String, val scale: Float) {
    COMPACT("COMPACT", 0.78f),
    STANDARD("STANDARD", 1.00f),
    SPACIOUS("SPACIOUS", 1.28f);

    companion object {
        fun fromKey(value: String?): ThemeDensity =
            entries.firstOrNull { it.key == value } ?: STANDARD
    }
}

/** نصّ الوجهة تحت أيقونة الشريط السفلي: دائمًا · للعنصر النشط وحده · بلا نصّ. */
enum class NavLabelMode { ALWAYS, ACTIVE_ONLY, NONE }

/**
 * بنية الشريط السفلي — **«خيارات متعددة بتصاميم مستقلة»** (§٦-ب من خطة المستوى): «شريط بسيط
 * تقليدي · شريط عائم أو بتكوين مدمج · شريط يبرز العنصر النشط بأسلوب واضح · شريط ذو مساحة أكبر
 * للأيقونة والنصّ · شريط بخلفية متكيّفة مع المحتوى…». والقواعد **بياناتٌ على النمط نفسه** — كما
 * في [TopBarStyle] — لا فروعٌ متناثرة في الرسم: الشريط يقرؤها، والمعاينة في الاستوديو ترسم بها،
 * واختبار JVM يقيسها.
 *
 * ## والافتراضيّ [FLOATING] — هو شريط التطبيق القائم
 *
 * فلا يتغيّر شكل شاشة لمن لم يطلب، وكل الأنماط الستّة تبقى في نفسها **تنقّلًا كاملًا**: لا نمط
 * يُخفي وجهةً أو يعطّل لمسًا — الأنماط تغيّر الحشو والطباعة وموضع النصّ وتمدّد العنصر النشط،
 * ولا تغيّر مجموعة الوجهات ولا دورها (`Role.Tab` لكل عنصر في كل نمط).
 *
 * ## وأرض اللمس لا تنزل تحت حدّها
 *
 * [itemMinHeight] في كل نمط ≥ `MaxSize.minTouchTarget` (٤٨dp) — حتى «المدمج» الذي يسقط صفّ
 * النصّ: الأيقونة تحتفظ بوصفها لقارئ الشاشة (`contentDescription` من نصّ الوجهة نفسه)، فلا يصير
 * العنصر زرًّا صامتًا. والاختبار يقيس الحدّ لكل نمط لا لنمطٍ واحد.
 */
enum class NavBarStyle(val key: String) {
    /** العائم القائم: كبسولة بهوامش، والنصّ تحت كل أيقونة، والمؤشّر ينزلق خلف العنصر. */
    FLOATING("FLOATING"),

    /** الملتصق القائم: بعرض الشاشة على الحافّة السفلية، بزوايا عليا وحدها، والنصّ دائمًا. */
    DOCKED("DOCKED"),

    /** المدمج: أيقونات وحدها بأهداف أوسع — أعلى مساحة للمحتوى، والوصف لقارئ الشاشة. */
    COMPACT("COMPACT"),

    /** الممتدّ: العنصر النشط وحده يتمدّد ويُظهر نصّه — البقية أيقونات. */
    LABELED("LABELED"),

    /** الواسع: مساحة أكبر للأيقونة والنصّ معًا، وملتصقٌ بالأرض مثل [DOCKED]. */
    LARGE("LARGE"),

    /**
     * المتكيّف مع المحتوى: **يسكن مستوى الصفحة** لا يطفو فوقها — سطحٌ معتم (`surfaceContainerHigh`)
     * بلا شفافية ولا ضباب، وبلا ظلّ، وحوافه على **هوامش الصفحة نفسها** (`MaxSpace.gutter`) وباستدارة
     * البطاقة لا الكبسولة. فهو الشريط الذي «يتبع المحتوى»: حين ينزل المحتوى إليه لا يمرّ من تحته.
     */
    ADAPTIVE("ADAPTIVE");

    /** كبسولة عائمة بهوامش — أم ملتصقة بحافّة الشاشة بعرضها؟ */
    val floating: Boolean get() = this == FLOATING || this == COMPACT || this == LABELED || this == ADAPTIVE

    /** متى يُكتب نصّ الوجهة تحت الأيقونة؟ */
    val labels: NavLabelMode get() = when (this) {
        COMPACT -> NavLabelMode.NONE
        LABELED -> NavLabelMode.ACTIVE_ONLY
        else -> NavLabelMode.ALWAYS
    }

    /** العنصر النشط وحده يتمدّد فيتّسع لنصّه (الفرق البنيويّ في [LABELED]). */
    val expandsActive: Boolean get() = this == LABELED

    /** حجم الأيقونة: القائم ٢٣dp، والمدمج أوسع، والواسع أكبر — والأرض اللمسية تتبعها. */
    val iconSize: Dp get() = when (this) {
        COMPACT -> 28.dp
        LARGE -> 30.dp
        else -> 23.dp
    }

    /** أقلّ ارتفاع للعنصر — أرض اللمس. ولا نمط ينزل تحت `MaxSize.minTouchTarget`. */
    val itemMinHeight: Dp get() = when (this) {
        COMPACT -> 56.dp
        LARGE -> 74.dp
        else -> 64.dp
    }

    /** المسافة بين الأيقونة والنصّ — وبلا نصّ لا فراغ (صفر). */
    val iconTextGap: Dp get() = if (labels == NavLabelMode.NONE) 0.dp else 4.dp

    /** حشو العنصر الرأسي — الواسع يتنفّس أكثر. */
    val itemPadV: Dp get() = if (this == LARGE) 12.dp else 8.dp

    /** نصّ الوجهة بحجم أكبر (في [LARGE] وحده) — «مساحة أكبر للأيقونة والنصّ». */
    val largeLabels: Boolean get() = this == LARGE

    /**
     * **يعيش في مستوى الصفحة** ([ADAPTIVE] وحده): سطحٌ معتم بلا ضباب ولا ظلّ، والحواف على هوامش
     * الصفحة، واستدارة البطاقة لا الكبسولة — فالفرق هنا **مستوًى** لا لونًا: شريطٌ من الصفحة نفسها
     * مقابل كبسولةٍ تطفو فوقها. ولهذا تُهمَل معه بوابة الضباب: ضبابٌ خلف سطحٍ معتم لا يرى شيئًا.
     */
    val pagePlane: Boolean get() = this == ADAPTIVE

    companion object {
        fun fromKey(value: String?): NavBarStyle =
            entries.firstOrNull { it.key == value } ?: FLOATING
    }
}

/**
 * بنية الشريط العلوي — **أنماط تختلف في البنية لا في اللون** (§٦-أ من خطة المستوى): «كل نمط
 * يجب أن يمتلك قواعد واضحة للمسافات والطباعة والأيقونات والعناوين والإجراءات والحركة، وألا يكون
 * مجرّد نسخة أخرى من النمط نفسه».
 *
 * والقواعد **بياناتٌ على النمط نفسه** لا فروعٌ متناثرة في الرسم: الشريط يقرؤها، واختبار JVM
 * يقيسها، والمعاينة في الاستوديو ترسم بها. والافتراضيّ [CLASSIC] — هو شريط التطبيق القائم، فلا
 * يتغيّر شكل شاشة لمن لم يطلب.
 *
 * **وحدّها المعلن:** الأنماط تغيّر بنية الشريط وترتيبه وكثافته؛ ولا تُخفي إجراءً ولا رمزًا — فمن
 * أضاف زرًّا في شريطه يجده في الأنماط الأربعة.
 */
enum class TopBarStyle(val key: String) {
    /** القائم: شريط متوسط، عنوان كبير، ورمز مجال يتقلّص عند الطيّ، والعودة في زرّ مُعبَّأ. */
    CLASSIC("CLASSIC"),

    /** مصغّر: صفّ واحد كثيف، بلا رمز مجال وبلا عنوان كبير — أعلى مساحة للمحتوى. */
    MINIMAL("MINIMAL"),

    /** سينمائيّ: عنوان كبير بمساحة تنفّس، والوصف سطرًا داخل الشريط حتى الطيّ. */
    CINEMATIC("CINEMATIC"),

    /** تقنيّ: صفّان عمليّان — العنوان ثمّ سطر معلومات كثيف؛ لوحات التحكّم قبل غيرها. */
    TECHNICAL("TECHNICAL");

    /** عنوان كبير بقواعد «سينمائيّة»: مساحة تنفّس وتسلسل بصريّ واضح. */
    val heroTitle: Boolean get() = this == CINEMATIC

    /** شريط مُصغَّر بصفّ واحد — لا عنوان كبير ولا رمز مجال. */
    val compactBar: Boolean get() = this == MINIMAL

    /** **الشريط يملك الوصف**: يُرسم داخل الشريط، فلا يكرّره جسم الصفحة. */
    val ownsDescription: Boolean get() = this == CINEMATIC || this == TECHNICAL

    /** رمز المجال يُرسم في الشريط (يتقلّص في [CLASSIC]، وثابتًا في [CINEMATIC]). */
    val showsGlyph: Boolean get() = this == CLASSIC || this == CINEMATIC

    companion object {
        fun fromKey(value: String?): TopBarStyle =
            entries.firstOrNull { it.key == value } ?: CLASSIC
    }
}

/** مستوى الحركة: انتقالات الشاشات كما هي · تلاشٍ هادئ وحده · بلا حركة إطلاقًا. */
enum class ThemeMotion(val key: String) {
    FULL("FULL"),
    CALM("CALM"),
    NONE("NONE");

    companion object {
        fun fromKey(value: String?): ThemeMotion =
            entries.firstOrNull { it.key == value } ?: FULL
    }
}

/**
 * مفاتيح التخزين — مصدرها الوحيد هذا الكائن، ولا نصّ مفتاح خارجها.
 *
 * والأسماء `theme_*` لا `*_new`: الشاشة كلها «سمة»، والنعت يفصلها عن مفاتيح `expressive_*`
 * و`color_*` القائمة التي لم تُمسّ (§ لا هجرات: المفاتيح القديمة تُقرأ كما هي).
 */
object ThemeKeys {
    const val CONTRAST = "theme_contrast"
    const val ROUNDNESS = "theme_roundness"
    const val FONT_SCALE = "theme_font_scale"
    const val NAV_BAR_STYLE = "nav_bar_style"
    const val MOTION = "theme_motion"

    /**
     * تأثيرات التمرير — كشف الكتل عند دخولها الشاشة. **بوليانيّ لا عدديّ**: قيمة واحدة تُقلَب
     * من الشاشة، والقيمة الغائبة تُقرأ «مفعّلة» ([ThemeStudio.scrollEffects]).
     */
    const val SCROLL_EFFECTS = "theme_scroll_effects"

    /**
     * إخفاء الميزات غير المتاحة على الجهاز (§٤-و من خطة المستوى). **بوليانيّ والافتراضيّ
     * «لا إخفاء»**: القيمة الغائبة تُعرض رماديةً مع سببها ([ThemeStudio.hideUnavailableFeatures])،
     * ومن قلّب المفتاح أُخفيت من مصفوفتَي القدرات (العتاد والصوت) — لا من معلومات أساسيّة.
     */
    const val HIDE_UNAVAILABLE = "hide_unavailable_features"

    /**
     * بنية الشريط العلوي (§٦-أ) — **نصّ المفتاح لا الرقم**: النمط يُخزَّن بمفتاحه (`CLASSIC`…)،
     * والقيمة الغائبة تُقرأ [TopBarStyle.CLASSIC] ([ThemeStudio.topBarStyle]).
     */
    const val TOP_BAR = "theme_top_bar"

    /** كثافة الأسطر (§٦-ج) — مفتاح [ThemeDensity]، والغائب يُقرأ `STANDARD`. */
    const val DENSITY = "theme_density"

    /** شكل البطاقات (§٦-ج) — مفتاح [MaxCardStyle]، والغائب يُقرأ `OUTLINED` (شكل اليوم). */
    const val CARD_STYLE = "theme_card_style"
}

/** القراءة الوحيدة لخيارات الاستوديو من تفضيلات `settings` — بافتراضات آمنة دائمًا. */
object ThemeStudio {
    fun contrast(prefs: SharedPreferences): ThemeContrast =
        ThemeContrast.fromKey(prefs.getString(ThemeKeys.CONTRAST, null))

    fun roundness(prefs: SharedPreferences): ThemeRoundness =
        ThemeRoundness.fromKey(prefs.getString(ThemeKeys.ROUNDNESS, null))

    fun fontScale(prefs: SharedPreferences): ThemeFontScale =
        ThemeFontScale.fromKey(prefs.getString(ThemeKeys.FONT_SCALE, null))

    fun navBarStyle(prefs: SharedPreferences): NavBarStyle =
        NavBarStyle.fromKey(prefs.getString(ThemeKeys.NAV_BAR_STYLE, null))

    /** بنية الشريط العلوي — [TopBarStyle.CLASSIC] عند الغياب أو عند مفتاح لا يُعرف. */
    fun topBarStyle(prefs: SharedPreferences): TopBarStyle =
        TopBarStyle.fromKey(prefs.getString(ThemeKeys.TOP_BAR, null))

    /** كثافة الأسطر (§٦-ج) — [ThemeDensity.STANDARD] عند الغياب أو عند مفتاح لا يُعرف. */
    fun density(prefs: SharedPreferences): ThemeDensity =
        ThemeDensity.fromKey(prefs.getString(ThemeKeys.DENSITY, null))

    /** شكل البطاقات (§٦-ج) — [MaxCardStyle.OUTLINED] (شكل اليوم) عند الغياب أو الجهالة. */
    fun cardStyle(prefs: SharedPreferences): MaxCardStyle =
        MaxCardStyle.entries.firstOrNull { it.key == prefs.getString(ThemeKeys.CARD_STYLE, null) }
            ?: MaxCardStyle.OUTLINED

    fun motion(prefs: SharedPreferences): ThemeMotion =
        ThemeMotion.fromKey(prefs.getString(ThemeKeys.MOTION, null))

    /**
     * تأثيرات التمرير (كشف الكتل عند الظهور) — **مفعّلة افتراضيًّا** ومعطّلة بطلب المستخدم.
     *
     * **ولماذا الافتراضيّ «مفعّلة»:** هي سلوك التطبيق القائم أصلًا (`Modifier.maxEnter` كان يعمل
     * بلا مفتاح)، ومعيار القبول ينصّ على أن تكون مفعّلة افتراضيًّا وقابلة للتعطيل. ومن عطّلها
     * يُعرض له المحتوى فورًا كاملًا — لا «حركة أسرع»، وهي حالة تُقرأ ولا تُخمَّن.
     */
    fun scrollEffects(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(ThemeKeys.SCROLL_EFFECTS, true)

    /**
     * إخفاء الميزات غير المتاحة على الجهاز — **الافتراضيّ «لا إخفاء»** (قيمة غائبة = عرض).
     *
     * **ولماذا الافتراضيّ عرضًا لا إخفاءً:** خطة المستوى تنصّ على أن غير المتاح يُعرض رماديًّا
     * «مع توضيح السبب» في الحالتين، والإخفاء قرارٌ يطلبه المستخدم بنفسه («ويكون المستخدم حرًا في
     * تفعيله أو تعطيله») — فقيمةٌ غائبة تُعرض، ولا يُغيّر تحديثٌ سلوكًا لم يطلبه أحد.
     */
    fun hideUnavailableFeatures(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(ThemeKeys.HIDE_UNAVAILABLE, false)
}

/**
 * نمط جاهز: مجموعة متسقة تُطبَّق بضغطة واحدة.
 *
 * **والبذرة من شرائح الشاشة نفسها** (`keyColorOptions` في `CustomThemeScreen`) — لا لون
 * خارج القائمة: فحين يُطبَّق النمط يُعلَّم سواره الشريحةَ المطابقة في صفّ «اللون الأساسي»،
 * ولا يدخل التطبيق لونٌ لا يستطيع المستخدم اختياره بيده.
 *
 * @param id ثابت لا يتغيّر (يُسجَّل في `EventLog`)؛ الاسم يُترجم عبر [nameRes].
 * @param mode المظهر الذي يكمّل البذرة (ليليّ/نهاريّ/أموليد).
 * @param contrast التباين الذي يُبرز النمط.
 * @param roundness الاستدارة التي تُكمّل الطابع (حادّة للنمط الصارم، دائريّة للدافئ).
 */
data class ThemePreset(
    val id: String,
    @StringRes val nameRes: Int,
    val seed: Int,
    val mode: ColorMode,
    val contrast: ThemeContrast,
    val roundness: ThemeRoundness,
) {
    /** هل هذه الخصائص الأربع هي المفروضة الآن؟ (التطابق على القيم الأربع وحدها.) */
    fun matches(
        keyColor: Int,
        colorMode: ColorMode,
        contrast: ThemeContrast,
        roundness: ThemeRoundness,
    ): Boolean =
        seed == keyColor && mode == colorMode && this.contrast == contrast && this.roundness == roundness
}

/**
 * الأنماط الستّة — «الهوية» أولًا (قيم الإقلاع نفسها: البذرة الرمادية المزرقة والنظام
 * والافتراضيّان)، ثم خمسة طوابعه مختلفة: ليليّ · أموليد · غروب · زمرديّ · ياسمين.
 *
 * والتنويع مقصود ومقيس: وضعان فاتحان وثلاثة داكنة ونظام واحد · وتوازن متوسط ثلاثة
 * وعالٍ واحد وافتراضيّان · والاستدارة دائريّة ثلاث وحادّة واحدة وقياسيّة اثنتان —
 * فمن جرّب الأنماط الستّة رأى الخيارات الثلاثة كلها في كل محور.
 */
val ThemeStudioPresets: List<ThemePreset> = listOf(
    ThemePreset(
        id = "identity",
        nameRes = R.string.theme_preset_identity,
        seed = MaxManagerDefaultKeyColor,
        mode = ColorMode.SYSTEM,
        contrast = ThemeContrast.DEFAULT,
        roundness = ThemeRoundness.STANDARD,
    ),
    ThemePreset(
        id = "midnight",
        nameRes = R.string.theme_preset_midnight,
        seed = 0xFF3F51B5.toInt(),
        mode = ColorMode.DARK,
        contrast = ThemeContrast.MEDIUM,
        roundness = ThemeRoundness.STANDARD,
    ),
    ThemePreset(
        id = "amoled",
        nameRes = R.string.theme_preset_amoled,
        seed = 0xFF00BCD4.toInt(),
        mode = ColorMode.DARKAMOLED,
        contrast = ThemeContrast.HIGH,
        roundness = ThemeRoundness.ROUND,
    ),
    ThemePreset(
        id = "sunset",
        nameRes = R.string.theme_preset_sunset,
        seed = 0xFFFF9800.toInt(),
        mode = ColorMode.LIGHT,
        contrast = ThemeContrast.DEFAULT,
        roundness = ThemeRoundness.ROUND,
    ),
    ThemePreset(
        id = "emerald",
        nameRes = R.string.theme_preset_emerald,
        seed = 0xFF009688.toInt(),
        mode = ColorMode.DARK,
        contrast = ThemeContrast.MEDIUM,
        roundness = ThemeRoundness.SHARP,
    ),
    ThemePreset(
        id = "blossom",
        nameRes = R.string.theme_preset_blossom,
        seed = 0xFFE91E63.toInt(),
        mode = ColorMode.LIGHT,
        contrast = ThemeContrast.MEDIUM,
        roundness = ThemeRoundness.ROUND,
    ),
)
