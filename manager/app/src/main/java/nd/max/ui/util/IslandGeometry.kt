/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
/*
 * الجزيرة الديناميكية — **هندسة الكبسولة**: الموضع والمقاس كأرقام، بلا أندرويد وبلا رسم.
 *
 * ### لماذا ملف واحد للأرقام
 *
 * طلب المالك: «تحريكه بأعلى وأسفل أو يمين ويسار لكي يقوم المستخدم بوضعه في المكان المناسب،
 * وتعريضه أو تصغيره، تطويله أو تقصيره». وهذا الطلب يُلمس في **ثلاثة مواضع مختلفة تمامًا**:
 * الشاشة (السحب والمعاينة)، والخدمة (النافذة فوق التطبيقات)، والرسم (الكبسولة نفسها). ولو
 * كُتب المدى في كل موضع لصارت ثلاثة مدايات تتباعد عند أوّل تعديل: الشاشة تسمح بـ٤٠٪ والخدمة
 * تقصّ عند ٣٥٪ والرسم يفترض ٣٠٪ — فيُسحب إلى مكان لا يستقرّ فيه أبدًا.
 *
 * فالقاعدة هنا: **الرقم يُكتب مرّة ويُستدعى ثلاثًا**، والحبس (clamp) يقع على المدخل لا على
 * المخرج، فقيمة محفوظة قديمة أو معدَّلة يدويًّا لا تستطيع أن تدفع الكبسولة خارج الشاشة.
 *
 * ### ومسرح المعاينة ليس صورة
 *
 * المعاينة تعرض **أعلى [PREVIEW_TOP_FRACTION] من الشاشة** وحدها، وهي **نفسها** أقصى إزاحة
 * رأسية — فما يُسحب إلى آخر المسرح هو بالضبط آخر ما يمكن أن يبلغه الحفظ. ولذلك ثابتان
 * بمعنى واحد: لو زاد أحدهما دون الآخر لكذبت المعاينة عند الحافة.
 *
 * ### والعرض بمعامل واحد لا بأرقام مطلقة
 *
 * مضاعف العرض يحرّك **الأرضية والسقف معًا** («أضيق ما تكون» و«أعرض ما تكون»)، لأن الاسمين
 * وحدهما لا يكفيان: مستخدم ضيّق الكبسولة الصغيرة ومستخدم موسّعها الكبيرة يقصدان الشيء نفسه،
 * ولو ثُبِّت أحدهما لصار الاسم كذبةً في أحدهما.
 */
package nd.max.ui.util

import kotlin.math.abs
import kotlin.math.roundToInt
import nd.max.ui.overlay.OverlayGeometry

object IslandGeometry {

    /** أضيق مضاعف عرض يقبله العقل، وأعرضه — وما خرج عنهما يُحبس. */
    const val WIDTH_SCALE_MIN = 0.70f
    const val WIDTH_SCALE_MAX = 1.30f

    /** أنحف مضاعف طول وأسمكه. */
    const val HEIGHT_SCALE_MIN = 0.75f
    const val HEIGHT_SCALE_MAX = 1.50f

    /** أقصى إزاحة أفقية كنسبة من عرض الشاشة، في كل اتجاه. */
    const val OFFSET_X_LIMIT = 0.35f

    /**
     * أعلى نسبة من الشاشة تمثّلها المعاينة — **وهي أقصى إزاحة رأسية بالضبط**.
     *
     * والنزول وحده: الجزيرة تسكن أعلى الشاشة بحكم كونها شريط حالة، ورفعها فوق الحدّ يعني
     * نصف كبسولة خارج الشاشة.
     */
    const val PREVIEW_TOP_FRACTION = 0.30f
    const val OFFSET_Y_LIMIT = PREVIEW_TOP_FRACTION

    /** أضيق عرض للكبسولة كنسبة من الشاشة (كبسولة هادئة بحجم النتوء تقريبًا). */
    const val MIN_WIDTH_FRACTION = 0.34f

    /** أقصى عرض لها، وسقفه المطلق بعد التكبير — لا تبلغ حافة الشاشة أبدًا. */
    const val MAX_WIDTH_FRACTION = 0.72f
    const val MAX_WIDTH_FRACTION_CAP = 0.92f

    fun clampWidthScale(value: Float): Float = value.coerceIn(WIDTH_SCALE_MIN, WIDTH_SCALE_MAX)

    fun clampHeightScale(value: Float): Float = value.coerceIn(HEIGHT_SCALE_MIN, HEIGHT_SCALE_MAX)

    fun clampOffsetX(value: Float): Float = value.coerceIn(-OFFSET_X_LIMIT, OFFSET_X_LIMIT)

    fun clampOffsetY(value: Float): Float = value.coerceIn(0f, OFFSET_Y_LIMIT)

    /** أرضية العرض: لا تنكمش الكبسولة عن [MIN_WIDTH_FRACTION] من مضاعف العرض. */
    fun minWidthFraction(widthScale: Float): Float =
        (MIN_WIDTH_FRACTION * clampWidthScale(widthScale)).coerceAtMost(MAX_WIDTH_FRACTION)

    /** سقف العرض: يمتدّ بمضاعف العرض ولا يبلغ حافة الشاشة. */
    fun maxWidthFraction(widthScale: Float): Float =
        (MAX_WIDTH_FRACTION * clampWidthScale(widthScale))
            .coerceIn(MIN_WIDTH_FRACTION, MAX_WIDTH_FRACTION_CAP)

    /** مضاعف الحشو الرأسي (الطول): ما رُفع من المضاعف يُحبس كما هو. */
    fun verticalPaddingScale(heightScale: Float): Float = clampHeightScale(heightScale)

    /** الإزاحة بالبكسل من النسبة المخزّنة — والنسبة تُحبس قبل الضرب. */
    fun offsetXPx(offsetXf: Float, screenWidthPx: Int): Int =
        (clampOffsetX(offsetXf) * screenWidthPx).roundToInt()

    fun offsetYPx(offsetYf: Float, screenHeightPx: Int): Int =
        (clampOffsetY(offsetYf) * screenHeightPx).roundToInt()

    /**
     * حبس الإزاحة الأفقية عند ما يُبقي الكبسولة **داخل** الشاشة.
     *
     * والعطب الذي يمنعه مقيس: كبسولة موسّعة عند أقصى يمين تخرج حافّتها — والإزاحة المسموحة
     * تعتمد على **عرض الكبسولة اللحظيّ** (يتغيّر مع المحتوى)، فلا يكفي حدّ ثابت في الملفّ.
     */
    fun clampOffsetXPx(offsetXPx: Int, screenWidthPx: Int, islandWidthPx: Int): Int {
        val room = ((screenWidthPx - islandWidthPx) / 2).coerceAtLeast(0)
        return offsetXPx.coerceIn(-room, room)
    }

    /** النسبة نفسها بعد الحبس بعرض الكبسولة — للمعاينة التي تقيس عرضها بنفسها. */
    fun clampOffsetXF(offsetXf: Float, widthPx: Int, islandWidthPx: Int): Float {
        if (widthPx <= 0) return clampOffsetX(offsetXf)
        val clamped = clampOffsetXPx((clampOffsetX(offsetXf) * widthPx).roundToInt(), widthPx, islandWidthPx)
        return clamped.toFloat() / widthPx
    }

    /** موضع الكبسولة داخل المعاينة: ٠ أعلى المسرح و١ أسفله. */
    fun previewYFraction(offsetYf: Float): Float =
        (clampOffsetY(offsetYf) / PREVIEW_TOP_FRACTION).coerceIn(0f, 1f)

    /**
     * نسبة المسرح (عرض ÷ ارتفاع) ليطابق أعلى [PREVIEW_TOP_FRACTION] من شاشة الجهاز نفسه.
     *
     * ولا نسبة ثابتة محفوظة: شاشة ٩:١٩٫٥ ولوح ٤:٣ لا يصحّ لهما إطار واحد، وإطارٌ يفترض
     * نسبةً يجعل الكبسولة تبدو في غير مكانها الذي ستستقرّ فيه فعلًا.
     */
    fun previewAspect(screenWidthDp: Int, screenHeightDp: Int): Float {
        val top = screenHeightDp * PREVIEW_TOP_FRACTION
        if (screenWidthDp <= 0 || top <= 0f) return 1f
        return (screenWidthDp / top).coerceIn(0.6f, 3.0f)
    }

    /** الربع العلوي من الشاشة: القصّة التي تقع أسفله ليست كاميرا. */
    const val CAMERA_ZONE_FRACTION = 0.25f

    /** كاميرا الأعلى تقع في وسط الشاشة تقريبًا: قصّة أبعد من خُمس العرض عن الوسط ليست «تحت الكاميرا». */
    const val CAMERA_CENTER_HALF_WIDTH = 0.20f

    /**
     * القصّة التي تُعدّ كاميرا: الأقرب إلى منتصف الشاشة أفقيًّا، من بين القصّات في الربع العلوي فقط.
     *
     * ولا تُقبل قصّة بعيدة عن الوسط، لأن الكبسولة تُثبَّت في الوسط: قصّة على الجانب لا تُقرأ
     * «تحت الكاميرا» وهي على بُعد منها. و`null` تعني: لا كاميرا في موضع يُعتمد عليه.
     */
    fun cameraCutout(
        cutouts: List<OverlayGeometry.Cutout>,
        screenWidthPx: Int,
        screenHeightPx: Int
    ): OverlayGeometry.Cutout? {
        if (screenWidthPx <= 0 || screenHeightPx <= 0) return null
        val zoneBottom = screenHeightPx * CAMERA_ZONE_FRACTION
        val halfBand = screenWidthPx * CAMERA_CENTER_HALF_WIDTH
        val centerX = screenWidthPx / 2f
        return cutouts
            .filter { it.top < zoneBottom && it.bottom > it.top }
            .map { it to abs((it.left + it.right) / 2f - centerX) }
            .filter { (_, distance) -> distance <= halfBand }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    /** أسفل الكاميرا بالبكسل، أو `null` إن لم تُعدّ قصّة كاميرا. */
    fun cameraBottomPx(cutouts: List<OverlayGeometry.Cutout>, screenWidthPx: Int, screenHeightPx: Int): Int? =
        cameraCutout(cutouts, screenWidthPx, screenHeightPx)?.bottom

    /**
     * أساس الجزيرة الرأسي بالبكسل قبل الإزاحة، لثلاثة مواضع:
     *
     * - [IslandPrefs.Position.Top]: حافّة الشاشة نفسها، فوق شريط الحالة والكاميرا.
     * - [IslandPrefs.Position.BelowStatus]: تحت شريط الحالة كاملًا.
     * - [IslandPrefs.Position.UnderCamera]: أسفل الكاميرا مباشرةً، وإن لم توجد كاميرا فأسفل الشريط.
     */
    fun anchorYPx(position: IslandPrefs.Position, statusBarPx: Int, cameraBottom: Int?): Int =
        when (position) {
            IslandPrefs.Position.Top -> 0
            IslandPrefs.Position.BelowStatus -> statusBarPx.coerceAtLeast(0)
            IslandPrefs.Position.UnderCamera -> (cameraBottom ?: statusBarPx).coerceAtLeast(0)
        }

    /**
     * موضع نسبةٍ من الشاشة داخل المسرح، والمسرح يعرض أعلى [PREVIEW_TOP_FRACTION] منها فقط.
     *
     * الأساس والإزاحة يُجمعان كنسبتين من الشاشة ثم يُسقطان على المسرح مرّة واحدة، فالمعاينة
     * تحسب ما تحسبه الخدمة بالبكسل، لا حسابًا آخر يفترق عنها عند الحافّة.
     */
    fun previewYFractionOf(shareOfScreen: Float): Float =
        (shareOfScreen / PREVIEW_TOP_FRACTION).coerceIn(0f, 1f)
}
