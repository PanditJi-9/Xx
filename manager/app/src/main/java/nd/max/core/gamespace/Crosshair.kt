/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import kotlin.math.max

/**
 * التصويب المخصّص في Game Turbo — **القواعد والهندسة بلا أندرويد وبلا Compose**.
 *
 * ### لماذا هنا لا في الخدمة
 *
 * شكل التصويب وموضعه قراران يُخطئان في حالات حقيقية: شاشة ضيّقة، قصّة، وحافة قريبة.
 * ولو حُسبا داخل `onDraw` لما أمكن قياسهما إلا بالعين. فالحساب كلّه في [crosshairGeometry]
 * يعطي خطوطًا ودوائرَ بإحداثيّات البكسل، ويستهلكه **رسم التراكب** و**معاينة الاستوديو**
 * معًا — فلا يرى المستخدم شكلًا في الإعدادات ويجد غيره فوق اللعبة.
 *
 * ### والحدّ المعلن
 *
 * لا لمس هنا: التصويب علامة لا زرّ. ولا كتابة عتاد. والتراكب نفسه هو من يضيف النافذة
 * (`CrosshairOverlay` في طبقة الخدمة) بعد صلاحية «الرسم فوق التطبيقات» القائمة.
 */
enum class CrosshairStyle(val key: String) {
    /** أربعة أذرع بفراغ مركزي — الشكل الكلاسيكيّ للألعاب. */
    CROSS("CROSS"),

    /** نقطة مركزية وحدها — أخفّ شكل وأقلّه حجبًا. */
    DOT("DOT"),

    /** حلقة رقيقة حول نقطة — واضحة على الخلفيات المزدحمة. */
    CIRCLE("CIRCLE"),

    /** ثلاثة أذرع (فوق ويمين ويسار) بلا ذراع سفلي — شكل T. */
    TEE("TEE");

    companion object {
        /** مفتاح لا يُعرف يعود إلى الصليب، ولا يُسقط الرسم. */
        fun fromKey(value: String?): CrosshairStyle =
            entries.firstOrNull { it.key == value } ?: CROSS
    }
}

/**
 * حالة التصويب المحفوظة. كل الأبعاد بـdp، والموضع نسبةٌ من مركز الشاشة:
 * `offsetX > 0` يمين، و`offsetY > 0` أسفل. والقيم الخارجة تُقصّ في [sanitized]، فلا
 * يكتب التفضيلَ إلا قيمةٌ صالحة حتى لو عُدّل الملفّ يدويًّا.
 */
data class CrosshairState(
    val enabled: Boolean = false,
    val style: CrosshairStyle = CrosshairStyle.CROSS,
    val color: Int = DEFAULT_COLOR,
    val sizeDp: Int = DEFAULT_SIZE_DP,
    val thicknessDp: Int = DEFAULT_THICKNESS_DP,
    val gapDp: Int = DEFAULT_GAP_DP,
    val opacity: Float = DEFAULT_OPACITY,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val outline: Boolean = true
) {
    /** نسخة صالحة للرسم والحفظ: كل قيمة داخل مداها المعلَن. */
    fun sanitized(): CrosshairState {
        val cleanSize = sizeDp.coerceIn(MIN_SIZE_DP, MAX_SIZE_DP)
        // الفراغ لا يأكل الذراع كلها: يبقى منها بكسل واحد على الأقل في أصغر حجم.
        val maxGap = max(MIN_GAP_DP, cleanSize / 2 - 1)
        return copy(
            color = color or 0xFF000000.toInt(),
            sizeDp = cleanSize,
            thicknessDp = thicknessDp.coerceIn(MIN_THICKNESS_DP, MAX_THICKNESS_DP),
            gapDp = gapDp.coerceIn(MIN_GAP_DP, maxGap),
            opacity = opacity.coerceIn(MIN_OPACITY, MAX_OPACITY),
            offsetX = offsetX.coerceIn(-MAX_OFFSET, MAX_OFFSET),
            offsetY = offsetY.coerceIn(-MAX_OFFSET, MAX_OFFSET)
        )
    }

    companion object {
        const val MIN_SIZE_DP = 12
        const val MAX_SIZE_DP = 64
        const val DEFAULT_SIZE_DP = 28

        const val MIN_THICKNESS_DP = 1
        const val MAX_THICKNESS_DP = 6
        const val DEFAULT_THICKNESS_DP = 2

        const val MIN_GAP_DP = 0
        const val DEFAULT_GAP_DP = 6

        const val MIN_OPACITY = 0.3f
        const val MAX_OPACITY = 1f
        const val DEFAULT_OPACITY = 0.9f

        /** أقصى إزاحة من المركز: ٤٠٪ — تبقى العلامة قريبة من الشاشة لا تهرب منها. */
        const val MAX_OFFSET = 0.4f

        /** أبيض افتراضيًّا: يُقرأ على الفاتح والداكن. */
        val DEFAULT_COLOR: Int = 0xFFFFFFFF.toInt()

        /**
         * لوحة ألوان ثابتة لا تتبع سمة التطبيق: التصويب يقع فوق **لعبة**، فالتباين هو
         * المعيار لا الانسجام مع لون الواجهة. والترتيب هو ترتيب العرض في المنتقي.
         */
        val COLORS: List<Int> = listOf(
            0xFFFFFFFF.toInt(), // أبيض
            0xFF39FF14.toInt(), // أخضر نيون
            0xFF00E5FF.toInt(), // سماوي
            0xFFFF3B30.toInt(), // أحمر
            0xFFFFC400.toInt(), // كهرماني
            0xFFFF2D95.toInt()  // ماجنتا
        )
    }
}

/** خطّ في هندسة التصويب — إحداثيّات بكسل جاهزة للرسم. */
data class CrosshairLine(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
)

/** دائرة في هندسة التصويب: مُعبَّأة للنقطة، ومجرَّدة للحلقة. */
data class CrosshairCircle(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val filled: Boolean
)

/**
 * شكل التصويب النهائي بلا أي قرار رسم آخر: الخطوط والدوائر والسماكة والمركز.
 * `strokeWidth` بالبكسل، والمركز مقصوص داخل الشاشة كي لا تُقطع العلامة عند الحافة.
 */
data class CrosshairGeometry(
    val centerX: Float,
    val centerY: Float,
    val strokeWidth: Float,
    val lines: List<CrosshairLine>,
    val circles: List<CrosshairCircle>
)

/**
 * يحسب الشكل من الحالة ومقاس اللوحة وكثافة الشاشة.
 *
 * **والمركز يُقصّ إلى `[half + stroke, width - half - stroke]`**، فلا تخرج العلامة ولو
 * اختار المستخدم أقصى إزاحة. وعند لوحة أصغر من العلامة نفسها يبقى المركز عند الحدّ
 * (ترسم العلامة مقصوصةً) بدل أن تختفي.
 */
fun crosshairGeometry(
    state: CrosshairState,
    width: Float,
    height: Float,
    density: Float
): CrosshairGeometry {
    val clean = state.sanitized()
    val d = density.coerceAtLeast(0.1f)
    val half = (clean.sizeDp * d) / 2f
    val stroke = (clean.thicknessDp * d).coerceAtLeast(1f)
    val gap = (clean.gapDp * d).coerceIn(0f, (half - 1f).coerceAtLeast(0f))
    val margin = half + stroke

    val maxX = (width - margin).coerceAtLeast(margin)
    val maxY = (height - margin).coerceAtLeast(margin)
    val cx = (width * (0.5f + clean.offsetX)).coerceIn(margin, maxX)
    val cy = (height * (0.5f + clean.offsetY)).coerceIn(margin, maxY)

    val lines = buildList {
        fun arm(startDx: Float, startDy: Float, endDx: Float, endDy: Float) {
            add(
                CrosshairLine(
                    startX = cx + startDx,
                    startY = cy + startDy,
                    endX = cx + endDx,
                    endY = cy + endDy
                )
            )
        }
        when (clean.style) {
            CrosshairStyle.CROSS -> {
                arm(0f, -gap, 0f, -half) // فوق
                arm(0f, gap, 0f, half)   // تحت
                arm(-gap, 0f, -half, 0f) // يسار
                arm(gap, 0f, half, 0f)   // يمين
            }
            CrosshairStyle.TEE -> {
                arm(0f, -gap, 0f, -half)
                arm(-gap, 0f, -half, 0f)
                arm(gap, 0f, half, 0f)
            }
            CrosshairStyle.DOT, CrosshairStyle.CIRCLE -> Unit
        }
    }

    val circles = buildList {
        when (clean.style) {
            CrosshairStyle.DOT -> add(
                CrosshairCircle(
                    centerX = cx,
                    centerY = cy,
                    radius = max(stroke * 1.35f, half * 0.24f),
                    filled = true
                )
            )
            CrosshairStyle.CIRCLE -> {
                add(CrosshairCircle(centerX = cx, centerY = cy, radius = half, filled = false))
                add(
                    CrosshairCircle(
                        centerX = cx,
                        centerY = cy,
                        radius = max(stroke, half * 0.18f),
                        filled = true
                    )
                )
            }
            CrosshairStyle.CROSS, CrosshairStyle.TEE -> Unit
        }
    }

    return CrosshairGeometry(
        centerX = cx,
        centerY = cy,
        strokeWidth = stroke,
        lines = lines,
        circles = circles
    )
}
