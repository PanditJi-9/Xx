/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.input

/** إيماءة يمكن ربطها بنقطة على الشاشة — كلها تُنفَّذ عبر [TouchInjector] لا بتقليد شكل. */
enum class ExtraGesture {
    TAP,
    DOUBLE_TAP,
    LONG_PRESS,
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT;

    /** الدوران على القائمة في واجهة الربط — بلا قائمة ثانية في الواجهة. */
    fun next(): ExtraGesture = entries[(ordinal + 1) % entries.size]
}

/**
 * نقطة ربط واحدة: إيماءة وموضع كنسبة من الشاشة.
 *
 * النسبة لا البكسل — فالموضع يبقى صحيحًا بعد تبدّل الدقّة أو الدوران، كما في أزرار الكتف.
 */
data class ExtraPoint(
    val id: Int,
    val gesture: ExtraGesture = ExtraGesture.TAP,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
)

/** لمسة واحدة محسوبة بالبكسل — تُسلَّم إلى [TouchInjector] كما هي. */
data class ExtraAction(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val durationMs: Int,
    /** التكرار للنقر المزدوج: مرّتان بفاصل قصير. */
    val repeatCount: Int = 1,
    val repeatGapMs: Long = 0L,
)

/**
 * حساب نقاط الإيماءات — **نقيّ بالكامل** كي يُقاس بلا جهاز.
 *
 * ### حدوده المعلنة
 *
 * - ثماني نقاط سقفًا: أكثر من ذلك يصير شاشة أزرار فوق اللعبة، ويستهلك كل لمسة تمرّ.
 * - الإيماءة تُنفَّذ عند النقطة عبر مسار الحقن نفسه؛ فاللعبة التي ترفض الإدخال المُولَّد
 *   ترفضها أيضًا، وهذا يُقال في الإقرار ولا يُخفى.
 * - «خريطة أزرار» هنا يدويّة: المستخدم يضع النقطة فوق زرّ اللعبة. لا نخمّن مواضع أزرار لعبة.
 */
object GameExtrasMath {

    const val MAX_POINTS = 8
    const val MIN_FRACTION = 0.04f
    const val MAX_FRACTION = 0.96f
    const val TAP_MS = 60
    const val LONG_PRESS_MS = 520
    const val SWIPE_MS = 180
    const val DOUBLE_TAP_GAP_MS = 90L
    const val SWIPE_LENGTH_FRACTION = 0.12f

    fun clampFraction(value: Float): Float =
        if (!value.isFinite()) 0.5f else value.coerceIn(MIN_FRACTION, MAX_FRACTION)

    fun px(fraction: Float, size: Int): Float =
        (clampFraction(fraction) * size.coerceAtLeast(1)).toInt().toFloat()

    fun fractionOf(positionPx: Float, size: Int): Float =
        if (size <= 0) 0.5f else clampFraction(positionPx / size)

    /** تنظيف القائمة: لا تكرار معرّف، ولا تجاوز السقف، وكل نسبة داخل الشاشة. */
    fun sanitize(points: List<ExtraPoint>): List<ExtraPoint> {
        val seen = HashSet<Int>(points.size)
        val out = ArrayList<ExtraPoint>(points.size)
        for (point in points) {
            if (out.size >= MAX_POINTS) break
            if (!seen.add(point.id)) continue
            out += ExtraPoint(
                id = point.id,
                gesture = point.gesture,
                x = clampFraction(point.x),
                y = clampFraction(point.y),
            )
        }
        return out
    }

    /** أول معرّف فارغ — الواجهة تضيف به بلا عدّاد ثانٍ. */
    fun nextId(points: List<ExtraPoint>): Int =
        (0 until MAX_POINTS).firstOrNull { id -> points.none { it.id == id } } ?: -1

    fun actionFor(point: ExtraPoint, width: Int, height: Int): ExtraAction {
        val x = px(point.x, width)
        val y = px(point.y, height)
        val dx = width.coerceAtLeast(0) * SWIPE_LENGTH_FRACTION
        val dy = height.coerceAtLeast(0) * SWIPE_LENGTH_FRACTION
        return when (point.gesture) {
            ExtraGesture.TAP -> ExtraAction(x, y, x, y, TAP_MS)
            ExtraGesture.DOUBLE_TAP ->
                ExtraAction(x, y, x, y, TAP_MS, repeatCount = 2, repeatGapMs = DOUBLE_TAP_GAP_MS)
            ExtraGesture.LONG_PRESS -> ExtraAction(x, y, x, y, LONG_PRESS_MS)
            ExtraGesture.SWIPE_UP -> ExtraAction(x, y, x, (y - dy).coerceAtLeast(0f), SWIPE_MS)
            ExtraGesture.SWIPE_DOWN -> ExtraAction(x, y, x, (y + dy).coerceAtMost(height.toFloat()), SWIPE_MS)
            ExtraGesture.SWIPE_LEFT -> ExtraAction(x, y, (x - dx).coerceAtLeast(0f), y, SWIPE_MS)
            ExtraGesture.SWIPE_RIGHT -> ExtraAction(x, y, (x + dx).coerceAtMost(width.toFloat()), y, SWIPE_MS)
        }
    }

    /** تخزين واحد يفهمه الاتّجاهان: `id,GESTURE,x,y;…` — والقراءة لا ترمي على مدخل معطوب. */
    fun encode(points: List<ExtraPoint>): String =
        sanitize(points).joinToString(";") { point ->
            "${point.id},${point.gesture.name},${point.x},${point.y}"
        }

    fun decode(raw: String?): List<ExtraPoint> {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return emptyList()
        val out = ArrayList<ExtraPoint>()
        for (chunk in text.split(';')) {
            if (out.size >= MAX_POINTS) break
            val parts = chunk.split(',')
            if (parts.size != 4) continue
            val id = parts[0].trim().toIntOrNull() ?: continue
            if (id < 0 || id >= MAX_POINTS) continue
            val gesture = ExtraGesture.entries.firstOrNull { it.name == parts[1].trim() } ?: continue
            val x = parts[2].trim().toFloatOrNull() ?: continue
            val y = parts[3].trim().toFloatOrNull() ?: continue
            out += ExtraPoint(id, gesture, clampFraction(x), clampFraction(y))
        }
        return sanitize(out)
    }
}

/** خطوة ماكرو: لمسة أو سحبة، بفاصل قبلها. */
enum class MacroStepKind { TAP, SWIPE }

data class MacroStep(
    val kind: MacroStepKind,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val delayMs: Long,
)

/**
 * الماكرو — تسلسل لمسات يعيد تشغيله المستخدم.
 *
 * **حدوده المعلنة:** الماكرو يعيد **لمسًا** لا قرارًا؛ لعبة تعتمد على زمن عشوائي أو على
 * موضع شخصية متغيّر قد لا تنفّذه كما توقّع صاحبه. ولذلك يُعاد تشغيله بيد المستخدم لا تلقائيًّا
 * في الخلفية: هو أداة تكرار لا بوت.
 */
object MacroMath {

    const val MAX_STEPS = 40
    const val MIN_DELAY_MS = 40L
    const val MAX_DELAY_MS = 3_000L
    const val DEFAULT_DELAY_MS = 350L
    const val TAP_SLOP_FRACTION = 0.008f

    fun clampDelay(value: Long): Long = value.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)

    fun sanitize(steps: List<MacroStep>): List<MacroStep> =
        steps.take(MAX_STEPS).map { step ->
            MacroStep(
                kind = step.kind,
                x1 = step.x1.coerceIn(0f, 1f),
                y1 = step.y1.coerceIn(0f, 1f),
                x2 = step.x2.coerceIn(0f, 1f),
                y2 = step.y2.coerceIn(0f, 1f),
                delayMs = clampDelay(step.delayMs),
            )
        }

    /** حركة أقل من عُشر بالمئة تُقرأ نقرة لا سحبة — والفرق يغيّر ما تفهمه اللعبة. */
    fun isTap(x1: Float, y1: Float, x2: Float, y2: Float): Boolean =
        kotlin.math.abs(x1 - x2) < TAP_SLOP_FRACTION && kotlin.math.abs(y1 - y2) < TAP_SLOP_FRACTION

    fun recorded(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        elapsedSincePreviousMs: Long,
    ): MacroStep = MacroStep(
        kind = if (isTap(x1, y1, x2, y2)) MacroStepKind.TAP else MacroStepKind.SWIPE,
        x1 = x1.coerceIn(0f, 1f),
        y1 = y1.coerceIn(0f, 1f),
        x2 = x2.coerceIn(0f, 1f),
        y2 = y2.coerceIn(0f, 1f),
        delayMs = clampDelay(elapsedSincePreviousMs),
    )

    fun encode(steps: List<MacroStep>): String =
        sanitize(steps).joinToString(";") { step ->
            "${step.kind.name},${step.x1},${step.y1},${step.x2},${step.y2},${step.delayMs}"
        }

    fun decode(raw: String?): List<MacroStep> {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return emptyList()
        val out = ArrayList<MacroStep>()
        for (chunk in text.split(';')) {
            if (out.size >= MAX_STEPS) break
            val parts = chunk.split(',')
            if (parts.size != 6) continue
            val kind = MacroStepKind.entries.firstOrNull { it.name == parts[0].trim() } ?: continue
            val x1 = parts[1].trim().toFloatOrNull() ?: continue
            val y1 = parts[2].trim().toFloatOrNull() ?: continue
            val x2 = parts[3].trim().toFloatOrNull() ?: continue
            val y2 = parts[4].trim().toFloatOrNull() ?: continue
            val delay = parts[5].trim().toLongOrNull() ?: continue
            out += MacroStep(kind, x1, y1, x2, y2, clampDelay(delay))
        }
        return sanitize(out)
    }
}

/**
 * يد ← لمس: اقتراب اليد من الشاشة يُرسل لمسة واحدة في المركز.
 *
 * **ما يقرؤه وما لا يقرؤه:** حسّاس القرب يعطي مسافة تقريبية (سنتيمترات على بعض الأجهزة،
 * وقيمة ٠/بعيد على غيرها)، فلا يُدّعى أنه يعرف «يدًا» بل «اقترابًا» حسب ما يعلنه الجهاز.
 * واللمسة واحدة عند العبور من بعيد إلى قريب، بفاصل ٧٠٠ ملّي ثانية يمنع الرعشة من توليد رشقة.
 */
object HandTouchMath {

    const val NEAR_CM = 3.0f
    const val COOLDOWN_MS = 700L

    fun isNear(value: Float, maxRange: Float): Boolean {
        if (!value.isFinite() || value < 0f) return false
        val ceiling = if (maxRange > 0f) minOf(maxRange, NEAR_CM) else NEAR_CM
        return value <= ceiling
    }

    fun shouldTap(previousNear: Boolean, nowNear: Boolean, sinceLastTapMs: Long): Boolean =
        nowNear && !previousNear && sinceLastTapMs >= COOLDOWN_MS
}
