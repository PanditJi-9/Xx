/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
/*
 * الجزيرة الديناميكية — **هندسة الموضع الآمن**: القصّة، وشريط الحالة، والاتجاه، وحجم الشاشة.
 *
 * ### العطب الذي يمنعه
 *
 * كانت النافذة تُوضع بـ`y = offsetY` وحده، وتُوسَّع إلى `screenWidth - 8dp`. فالمحتوى يُرسم
 * **تحت الكاميرا** (القصّة في وسط أعلى الشاشة) وقد يتجاوز شريط الحالة على الأجهزة الضيّقة،
 * ولا يوجد أيّ فرق بين الوضع الطولي والعرضي. وكلّها عيوب لا تُرى في المحاكي لأن القصّة غائبة.
 *
 * ### ولماذا حساب نقيّ هنا
 *
 * الموضع يُلمس من ثلاثة مواضع: الخدمة (النافذة)، والمعاينة (الشاشة)، والاختبار. وثلاثة حسابات
 * لشيء واحد تفترق عند أوّل تعديل. فالحساب كلّه في هذا الملفّ: مدخلات فيزيائية (بكسل)، ومخرج
 * موضع وحجم. ولا أندرويد هنا، فيُقاس على JVM بلا جهاز.
 *
 * ### والقاعدة الحاكمة
 *
 * **لا محتوى تحت القصّة ولا خارج المنطقة الآمنة، إلّا إن طلب المستخدم ذلك صراحةً.** أي أن
 * `avoidSystemUi = false` هو الخروج الوحيد عن الحماية، وهو خيار مستخدم لا افتراض.
 */
package nd.max.island.core

import kotlin.math.abs
import kotlin.math.roundToInt
import nd.max.island.engine.IslandLevel

/**
 * الوصف الفيزيائي للمساحة المتاحة، بالبكسل — من [android.view.WindowInsets] ومن مقاس النافذة.
 *
 * [safeTopPx] هو `DisplayCutout.safeInsetTop` (أو ارتفاع شريط الحالة إن لم تُعلن قصّة)،
 * و[statusBarPx] ارتفاع شريط الحالة المعلن من موارد النظام. ولا يُفترض أيّهما صفرًا: الغياب
 * يعني «لم يُقرأ»، ولذلك يُؤخذ الأكبر لا الأخير.
 */
data class IslandInsets(
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val density: Float,
    val safeLeftPx: Int = 0,
    val safeTopPx: Int = 0,
    val safeRightPx: Int = 0,
    val safeBottomPx: Int = 0,
    val statusBarPx: Int = 0,
    val cutoutLeftPx: Int = -1,
    val cutoutTopPx: Int = -1,
    val cutoutRightPx: Int = -1,
    val cutoutBottomPx: Int = -1,
    val landscape: Boolean = false,
    val rtl: Boolean = false,
) {
    /** مدخلات مقروءة من نظام قد يعطي أرقامًا سالبة أو صفرية؛ لا يُبنى عليها موضع. */
    fun sanitized(): IslandInsets = copy(
        screenWidthPx = screenWidthPx.coerceAtLeast(1),
        screenHeightPx = screenHeightPx.coerceAtLeast(1),
        density = if (density.isFinite() && density > 0f) density else 1f,
        safeLeftPx = safeLeftPx.coerceAtLeast(0),
        safeTopPx = safeTopPx.coerceAtLeast(0),
        safeRightPx = safeRightPx.coerceAtLeast(0),
        safeBottomPx = safeBottomPx.coerceAtLeast(0),
        statusBarPx = statusBarPx.coerceAtLeast(0),
    )
}

/** نتيجة الحساب: أين تُوضع النافذة، وأقصى عرض يقبله المحتوى. */
data class IslandPlacement(
    val xPx: Int,
    val yPx: Int,
    val maxWidthPx: Int,
    val anchorTopPx: Int,
    val belowSystemUi: Boolean,
    val cutoutSpanPx: Int?,
) {
    /** عرض «الجناح» الواحد حول قصّة وسطية — يُستعمل لتوزيع النصّ يمينًا ويسارًا. */
    fun wingPx(islandWidthPx: Int): Int {
        val span = cutoutSpanPx ?: return (islandWidthPx / 2).coerceAtLeast(0)
        return ((islandWidthPx - span) / 2).coerceAtLeast(0)
    }
}

/**
 * ميزانية النصّ: كم سطرًا وكم حرفًا يتحمّلهما العرض الفعليّ.
 *
 * **ولماذا لا يكفي `TextOverflow.Ellipsis`:** القصّ يحدث **بعد** أن يُرسم السطر، فإن كان
 * العنصر أعرض من المتاح يختفي الذيل كلّه أو يُرسم تحت الأيقونة. والميزانية تُقرأ **قبل**
 * البناء: عدد الأسطر، وهل يُعرض السطر الثاني أصلًا، وهل يتّسع لشارة في الطرف.
 */
data class IslandTextBudget(
    val titleChars: Int,
    val titleLines: Int,
    val bodyLines: Int,
    val showBody: Boolean,
    val showTrailing: Boolean,
)

/** زنبرك الحركة: سرعة الفليك تُغيّر الصلابة والتخميد، لا مدّة ثابتة. */
data class IslandSpringSpec(val dampingRatio: Float, val stiffness: Float)

/** انتقال الظهور/الإخفاء: الفليك السريع أقصر وأجرأ، والسحب البطيء أهدأ. */
data class IslandTransitionSpec(
    val durationMs: Int,
    val startScale: Float,
    val slideFraction: Float,
)

object IslandLayout {
    /** هامش الحافّة: الكبسولة لا تلمس حافّة الشاشة أبدًا. */
    const val EDGE_MARGIN_DP = 6f

    /** فراغ بين أسفل القصّة/شريط الحالة وأعلى الكبسولة. */
    const val CUTOUT_GAP_DP = 4f

    /** سقف العرض بلا نظام آمن: لا تبلغ الحافّة. */
    const val MAX_WIDTH_FRACTION = 0.94f

    /** أقصى ارتفاع منطقيّ للكبسولة كنسبة من الشاشة — يمنع بطاقةً تخرج أسفل الشاشة. */
    const val MAX_HEIGHT_FRACTION = 0.42f

    /** أقلّ عرض مقروء؛ ما دونه يُعرض رمزًا واحدًا لا نصًّا مقصوصًا بلا معنى. */
    const val MIN_READABLE_WIDTH_DP = 72f

    fun maxWidthPx(insets: IslandInsets): Int {
        val i = insets.sanitized()
        val margin = (EDGE_MARGIN_DP * i.density).roundToInt()
        val available = i.screenWidthPx - i.safeLeftPx - i.safeRightPx - margin * 2
        val cap = (i.screenWidthPx * MAX_WIDTH_FRACTION).roundToInt()
        return available.coerceIn(1, cap)
    }

    /**
     * أعلى نقطة مسموحة للكبسولة.
     *
     * [avoidSystemUi] = false يعيد إزاحة المستخدم كما هي (اختيار صريح للتراكب)، و[avoidSystemUi]
     * = true يرفعها إلى ما تحت القصّة/شريط الحالة. ولا يُرفع إلا إلى الأدنى: من وضعها أسفل
     * منهما لا تُسحب إلى أعلى.
     */
    fun preferredTopPx(insets: IslandInsets, offsetYf: Float, avoidSystemUi: Boolean): Int {
        val i = insets.sanitized()
        val desired = (finite(offsetYf, 0f, 10_000f) * i.density).roundToInt()
        if (!avoidSystemUi) return desired
        val systemTop = maxOf(i.safeTopPx, i.statusBarPx)
        val gap = (CUTOUT_GAP_DP * i.density).roundToInt()
        val limit = i.screenHeightPx / 3
        return maxOf(desired, systemTop + gap).coerceAtMost(limit)
    }

    /** عرض القصّة **فقط** إن كانت وسطية (الكاميرا في الوسط) — وإلا فليست حاجزًا للكبسولة الوسطية. */
    fun centeredCutoutSpanPx(insets: IslandInsets): Int? {
        val i = insets.sanitized()
        if (i.cutoutLeftPx < 0 || i.cutoutRightPx <= i.cutoutLeftPx) return null
        if (i.cutoutTopPx < 0 || i.cutoutBottomPx <= i.cutoutTopPx) return null
        val center = (i.cutoutLeftPx + i.cutoutRightPx) / 2f
        val tolerance = i.screenWidthPx * 0.22f
        if (abs(center - i.screenWidthPx / 2f) > tolerance) return null
        return i.cutoutRightPx - i.cutoutLeftPx
    }

    /**
     * الحساب الكامل. العرض والارتفاع قد يكونان صفرًا في أول تركيب (قبل أن يقيس Compose)
     * فلا يُنتج موضع سالب ولا قسمة على صفر.
     */
    fun place(
        insets: IslandInsets,
        offsetXf: Float,
        offsetYf: Float,
        islandWidthPx: Int,
        islandHeightPx: Int,
        avoidSystemUi: Boolean,
    ): IslandPlacement {
        val i = insets.sanitized()
        val margin = (EDGE_MARGIN_DP * i.density).roundToInt()
        val w = islandWidthPx.coerceAtLeast(0)
        val h = islandHeightPx.coerceAtLeast(0)
        val maxW = maxWidthPx(i)
        val anchor = preferredTopPx(i, offsetYf, avoidSystemUi)

        val minX = i.safeLeftPx + margin
        val maxX = (i.screenWidthPx - i.safeRightPx - margin - w).coerceAtLeast(minX)
        // في الوضع العرضي قد تكون القصّة على أحد الجانبين: التوسيط داخل المساحة الآمنة لا داخل
        // الشاشة كلّها، وإلا بدت الكبسولة منزاحةً نحو الجانب الذي بلا قصّة.
        val area = (maxX - minX).coerceAtLeast(0)
        val centered = minX + area / 2
        val x = (centered + (finite(offsetXf, -10_000f, 10_000f) * i.density).roundToInt()).coerceIn(minX, maxX)

        val bottomRoom = (i.screenHeightPx - i.safeBottomPx - margin - h).coerceAtLeast(0)
        val y = anchor.coerceIn(0, bottomRoom)

        val cutoutBottom = if (i.cutoutBottomPx > 0) i.cutoutBottomPx else 0
        val systemTop = maxOf(i.safeTopPx, i.statusBarPx)
        return IslandPlacement(
            xPx = x,
            yPx = y,
            maxWidthPx = maxW,
            anchorTopPx = anchor,
            belowSystemUi = y >= maxOf(systemTop, cutoutBottom),
            cutoutSpanPx = centeredCutoutSpanPx(i),
        )
    }

    /** ميزانية النصّ من عرض فعليّ. الأرقام تقديرية لكنها تُقاس مقابل الحشو الحقيقيّ. */
    fun textBudget(widthPx: Int, density: Float, level: IslandLevel): IslandTextBudget {
        val d = if (density.isFinite() && density > 0f) density else 1f
        val widthDp = widthPx.coerceAtLeast(0) / d
        val reserved = when (level) {
            IslandLevel.Compact -> 66f
            IslandLevel.Banner -> 92f
            IslandLevel.Expanded -> 118f
            IslandLevel.Large -> 136f
        }
        val inner = (widthDp - reserved).coerceAtLeast(24f)
        val charDp = 6.6f
        val chars = (inner / charDp).roundToInt().coerceIn(4, 160)
        return when (level) {
            IslandLevel.Compact -> IslandTextBudget(
                titleChars = chars,
                titleLines = 1,
                bodyLines = 0,
                showBody = false,
                showTrailing = inner >= 132f,
            )
            IslandLevel.Banner -> IslandTextBudget(
                titleChars = chars,
                titleLines = 1,
                bodyLines = 1,
                showBody = inner >= 96f,
                showTrailing = inner >= 128f,
            )
            IslandLevel.Expanded -> IslandTextBudget(
                titleChars = chars,
                titleLines = if (inner < 190f) 1 else 2,
                bodyLines = if (inner < 150f) 1 else 2,
                showBody = inner >= 84f,
                showTrailing = inner >= 120f,
            )
            IslandLevel.Large -> IslandTextBudget(
                titleChars = chars,
                titleLines = 2,
                bodyLines = if (inner < 220f) 3 else 5,
                showBody = inner >= 84f,
                showTrailing = inner >= 120f,
            )
        }
    }

    /**
     * زنبرك يتفاعل مع سرعة الإيماءة.
     *
     * الفليك السريع (‏≥ ~1200px/s) يزيد الصلابة ويقلّل التخميد قليلًا: الوصول أسرع مع ارتداد
     * خفيف. السحب البطيء يبقى مطيعًا بلا ارتداد مفاجئ. و[animSpeed] مفتاح المستخدم يضاعف
     * الأساس لا النتيجة، فحدوده محفوظة.
     */
    fun springFor(velocityPxPerSec: Float, animSpeed: Float): IslandSpringSpec {
        val speed = if (animSpeed.isFinite()) animSpeed.coerceIn(0.5f, 2f) else 1f
        val v = if (velocityPxPerSec.isFinite()) abs(velocityPxPerSec) else 0f
        val norm = (v / 1200f).coerceIn(0f, 2.5f)
        val stiffness = (2_400f * speed + norm * 900f).coerceIn(300f, 6_000f)
        val damping = (0.86f - norm * 0.07f + (speed - 1f) * 0.03f).coerceIn(0.62f, 0.95f)
        return IslandSpringSpec(damping, stiffness)
    }

    /** مدّة انتقال الظهور/الإخفاء: أقصر مع الفليك السريع، وأطول مع السحب البطيء. */
    fun transitionFor(velocityPxPerSec: Float): IslandTransitionSpec {
        val v = if (velocityPxPerSec.isFinite() && abs(velocityPxPerSec) >= 1f) abs(velocityPxPerSec) else 0f
        val norm = (v / 6_000f).coerceIn(0f, 1f)
        return IslandTransitionSpec(
            durationMs = (190f - 110f * norm).roundToInt().coerceIn(70, 190),
            startScale = (0.72f - 0.14f * norm).coerceIn(0.55f, 0.72f),
            slideFraction = (0.28f + 0.22f * norm).coerceIn(0.28f, 0.50f),
        )
    }

    /** `roundToInt` يرمي على NaN؛ وقياس نظام غير مكتمل ليس سببًا لسقوط الخدمة. */
    private fun finite(value: Float, min: Float, max: Float): Float =
        if (value.isFinite()) value.coerceIn(min, max) else 0f
}
