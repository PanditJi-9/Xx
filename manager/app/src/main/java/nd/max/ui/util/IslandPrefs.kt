/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.ui.util

import android.content.Context

/**
 * ما تحفظه الجزيرة الديناميكية. تُقرأ من الشاشة ومن الخدمة معًا، فتغيير وأنت خارج التطبيق
 * يظهر في الكبسولة بلا إعادة تشغيل.
 *
 * ### وأسماء الأنواع لا أرقامها
 *
 * كما في `FpsOverlayPrefs` بالحرف: الموضع والمقاس يُخزَّنان **باسم النوع**، وقراءة اسم لا
 * يُعرف تُسقط إلى الأصل. فرقم `0/1` في المخزن يُقرأ خطأً بمجرّد إعادة ترتيب الاختيارات،
 * والرجوع إلى الافتراضيّ حالة سيئة واحدة ممكنة — لا إلى شكل آخر بالغلط.
 *
 * ### والافتراضيّات مُعلنة هنا لا في الشاشة
 *
 * مفتاح لكل مصدر من الأربعة، وافتراضيّها: الإشعارات والوسائط والشحن **مفتوحة** (هي ما جاء
 * المستخدم لأجله، وكلّها بلا كلفة قراءة إضافية)، والمؤشرات **مغلقة** — لأنّها تُشغّل قارئ
 * اللوحة (`HudSampler`) على جهاز يُقاس أداؤه، فتفتح بقرار لا بحُسن نيّة.
 */
object IslandPrefs {

    private const val PREFS_NAME = "maxmanager_island"

    private const val KEY_ENABLED = "island_enabled"
    private const val KEY_POSITION = "island_position"
    private const val KEY_SIZE = "island_size"
    private const val KEY_COLOR = "island_color_hex"
    private const val KEY_NOTICES = "island_notices"
    private const val KEY_MEDIA = "island_media"
    private const val KEY_CHARGING = "island_charging"
    private const val KEY_INDICATORS = "island_indicators"
    private const val KEY_IDLE_DOT = "island_idle_dot"
    private const val KEY_SHAPE = "island_shape"
    private const val KEY_OPACITY = "island_opacity"
    private const val KEY_DURATION = "island_duration"
    private const val KEY_ANIMATE = "island_animate"
    private const val KEY_OFFSET_X = "island_offset_x"
    private const val KEY_OFFSET_Y = "island_offset_y"
    private const val KEY_WIDTH_SCALE = "island_width_scale"
    private const val KEY_HEIGHT_SCALE = "island_height_scale"
    private const val KEY_VERSION = "island_prefs_version"

    /**
     * نسخة الملفّ — وُجدت لنقلٍ واحد صريح لا لترقيمٍ للترقيم.
     *
     * العطب الذي تُصلحه: لون الأصل كان `#00E5FF` (سماوي)، وطلب المالك أن تكون **سوداء**
     * افتراضيًّا. ولو غُيِّر الافتراضيّ وحده لبقي اللون القديم في أجهزة من فعّل الجزيرة قبل
     * التبديل (المفتاح يُحفظ كاملًا عند أوّل تغيير)، فيرى المستخدم سماويًّا ويقول «لم يتغيّر».
     * فالنسخة ٢ تنقل **الافتراضيّ غير الملموس** وحده، ومن اختار لونًا بنفسه لا يُمسّ اختياره.
     */
    private const val PREFS_VERSION = 2

    /** الافتراضيّ الأسود: كبسولة المصنع سوداء لا ملوّنة. */
    const val DEFAULT_COLOR_HEX = "#000000"

    /** الافتراضيّ القديم — يُنقل مرّة واحدة عند الترقية، ولا يُستعمل افتراضًا في أي موضع. */
    const val LEGACY_DEFAULT_COLOR_HEX = "#00E5FF"

    /**
     * نقل اللون القديم عند الترقية — **دالّة نقيّة تُقاس** (`IslandGeometryTest`).
     *
     * والقاعدة: ما يساوي الافتراضيّ القديم بالحرف يُنقل إلى الأسود، وما خالفه (لون اختاره
     * المستخدم بنفسه) يبقى كما هو. والعودة `null` تعني «لا لون محفوظ» ⇒ الافتراضيّ الجديد.
     */
    fun migrateColorOnUpgrade(storedHex: String?): String? = when {
        storedHex == null -> null
        storedHex.equals(LEGACY_DEFAULT_COLOR_HEX, ignoreCase = true) -> DEFAULT_COLOR_HEX
        else -> storedHex
    }

    /**
     * موضع الكبسولة الأساسي. الإزاحة تُضاف إليه كما هي.
     *
     * - [Top]: حافّة الشاشة العليا، فوق شريط الحالة وفوق الكاميرا إن وُجدت.
     * - [BelowStatus]: تحت شريط الحالة كاملًا.
     * - [UnderCamera]: أسفل الكاميرا مباشرةً — الافتراضيّ لمن لم يختر موضعًا بعد.
     */
    enum class Position { Top, BelowStatus, UnderCamera }

    /** مقاس الكبسولة كمعامل يُضرب في مقاسات المحتوى، فلا رقم يُكتب مرّتين. */
    enum class Size(val scale: Float) {
        Compact(0.85f),
        Normal(1f),
        Large(1.15f)
    }

    /** شكل الكبسولة: وسادة كاملة، أو حوافّ ناعمة، أو زوايا صغيرة. */
    enum class Shape { Pill, Soft, Square }

    /**
     * شفافية خلفية الكبسولة.
     *
     * والعتمة ليست قيمة حرّة لسبب مقيس: الكبسولة تجلس فوق شريط حالة وربما فوق لعبة، وشفافية
     * حرة قد تجعل النصّ غير مقروء على خلفية فاتحة. فالثلاث درجات كلها مقروءة والأخفّ لا يُخفي
     * ما تحته إخفاءً تامًّا.
     */
    enum class Opacity(val alpha: Float) {
        Solid(1f),
        Soft(0.92f),
        Glass(0.8f)
    }

    /** مضاعف مدّة الإشعار العابر: أقصر للعين المستعجلة، وأطول لمن يقرأ على مهل. */
    enum class Duration(val scale: Float) {
        Short(0.6f),
        Normal(1f),
        Long(1.6f)
    }

    data class State(
        /**
         * **قرار «الجزيرة مفعَّلة»** — منفصل عن «الخدمة تعمل الآن» عن قصد.
         *
         * الخدمة تعمل أو تتوقف بأسباب كثيرة لا تعني تراجع المستخدم (قتلها النظام، سحب الصلاحية
         * مؤقتًا، إعادة تشغيل)، فلو كان المفتاح هو «هل تعمل؟» لانقلب مطفأً بعد كل قتل، ولما
         * عرف بابُ الاستعادة بعد الإقلاع (§3) هل يعيدها أم لا. فالمخزون **نيّة المستخدم**،
         * والمقروء من الخدمة حقيقتها اللحظية، والشاشة تعرضان معًا بصدق.
         */
        val enabled: Boolean = false,
        val position: Position = Position.UnderCamera,
        val size: Size = Size.Normal,
        val colorHex: String = DEFAULT_COLOR_HEX,
        val showNotices: Boolean = true,
        val showMedia: Boolean = true,
        val showCharging: Boolean = true,
        val showIndicators: Boolean = false,
        val showIdleDot: Boolean = true,
        val shape: Shape = Shape.Pill,
        val opacity: Opacity = Opacity.Solid,
        val duration: Duration = Duration.Normal,
        val animate: Boolean = true,
        /**
         * إزاحة حرّة عن الموضع المختار: أفقية بنسبة من عرض الشاشة (يمينًا موجبًا، ويسارًا
         * سالبًا)، ورأسية بنسبة من ارتفاعها نزولًا وحده.
         *
         * ولأنها **نسبة لا بكسل**: تدوير الجهاز أو اختلاف مقاسه يُبقي الكبسولة في نسبتها من
         * الشاشة، ولو حُفظت بكسل لظهرت في مكان آخر على شاشة أخرى. والشاشة تسمّيها «الموضع».
         */
        val offsetXf: Float = 0f,
        val offsetYf: Float = 0f,
        /** مضاعفا العرض والطول (١ = الأصل) — يُحبسان في `IslandGeometry`. */
        val widthScale: Float = 1f,
        val heightScale: Float = 1f
    )

    fun load(context: Context): State {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val upgraded = p.getInt(KEY_VERSION, 1) < PREFS_VERSION
        val storedColor = p.getString(KEY_COLOR, null)
        return State(
            enabled = p.getBoolean(KEY_ENABLED, false),
            position = enumOr(p.getString(KEY_POSITION, null), Position.entries, Position.UnderCamera),
            size = enumOr(p.getString(KEY_SIZE, null), Size.entries, Size.Normal),
            // النقل عند الترقية يعمل على المخزون وحده: قراءةٌ على نسخة حديثة لا تعيد كتابة لون.
            colorHex = (if (upgraded) migrateColorOnUpgrade(storedColor) else storedColor)
                ?: DEFAULT_COLOR_HEX,
            showNotices = p.getBoolean(KEY_NOTICES, true),
            showMedia = p.getBoolean(KEY_MEDIA, true),
            showCharging = p.getBoolean(KEY_CHARGING, true),
            showIndicators = p.getBoolean(KEY_INDICATORS, false),
            // النقطة الهادئة **مفتوحة افتراضيًّا**: كبسولة لا يظهر منها شيء في لحظة هادئة
            // تُقرأ «الجزيرة لا تعمل» — وهو أسوأ من نقطة صغيرة تقول «حاضرة».
            showIdleDot = p.getBoolean(KEY_IDLE_DOT, true),
            shape = enumOr(p.getString(KEY_SHAPE, null), Shape.entries, Shape.Pill),
            opacity = enumOr(p.getString(KEY_OPACITY, null), Opacity.entries, Opacity.Solid),
            duration = enumOr(p.getString(KEY_DURATION, null), Duration.entries, Duration.Normal),
            animate = p.getBoolean(KEY_ANIMATE, true),
            // الحبس عند القراءة: قيمة محفوظة قديمة أو معدَّلة من نسخة سابقة لا تدفع الكبسولة
            // خارج الشاشة — المدى في `IslandGeometry` وحده، فلا رقم ثانٍ يُخترع هنا.
            offsetXf = IslandGeometry.clampOffsetX(p.getFloat(KEY_OFFSET_X, 0f)),
            offsetYf = IslandGeometry.clampOffsetY(p.getFloat(KEY_OFFSET_Y, 0f)),
            widthScale = IslandGeometry.clampWidthScale(p.getFloat(KEY_WIDTH_SCALE, 1f)),
            heightScale = IslandGeometry.clampHeightScale(p.getFloat(KEY_HEIGHT_SCALE, 1f))
        )
    }

    fun save(context: Context, state: State) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putBoolean(KEY_ENABLED, state.enabled)
            putString(KEY_POSITION, state.position.name)
            putString(KEY_SIZE, state.size.name)
            putString(KEY_COLOR, state.colorHex)
            putBoolean(KEY_NOTICES, state.showNotices)
            putBoolean(KEY_MEDIA, state.showMedia)
            putBoolean(KEY_CHARGING, state.showCharging)
            putBoolean(KEY_INDICATORS, state.showIndicators)
            putBoolean(KEY_IDLE_DOT, state.showIdleDot)
            putString(KEY_SHAPE, state.shape.name)
            putString(KEY_OPACITY, state.opacity.name)
            putString(KEY_DURATION, state.duration.name)
            putBoolean(KEY_ANIMATE, state.animate)
            putFloat(KEY_OFFSET_X, IslandGeometry.clampOffsetX(state.offsetXf))
            putFloat(KEY_OFFSET_Y, IslandGeometry.clampOffsetY(state.offsetYf))
            putFloat(KEY_WIDTH_SCALE, IslandGeometry.clampWidthScale(state.widthScale))
            putFloat(KEY_HEIGHT_SCALE, IslandGeometry.clampHeightScale(state.heightScale))
            // الكتابة ترفع النسخة: النقل لا يعود على لون اختاره المستخدم بعد الترقية.
            putInt(KEY_VERSION, PREFS_VERSION)
            apply()
        }
    }

    private fun <T> enumOr(raw: String?, values: List<T>, fallback: T): T where T : Enum<T> =
        values.firstOrNull { it.name == raw } ?: fallback
}
