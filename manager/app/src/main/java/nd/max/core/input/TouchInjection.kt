/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.input

/**
 * نتيجة محاولة حقن لمسة واحدة — ثلاثة أحكام لا رابع لها.
 *
 * **والفرق بين [Refused] و[Failed] مقصود:** الرفض قرار سبق التنفيذ (إصبع على الشاشة، أو
 * مساران غير متاحين)، والفشل يعني أن المسار نُفِّذ ولم يكتمل. ولا يُقال «نجح» إلا بـ
 * [Delivered]، ولا يُخمَّن نجاح من غياب الخطأ (ADR-07).
 */
sealed interface InjectionOutcome {
    data class Delivered(val lane: InjectionLane) : InjectionOutcome
    data class Refused(val reason: InjectionRefusal) : InjectionOutcome
    data class Failed(val lane: InjectionLane, val detail: String) : InjectionOutcome
}

/** المسار الذي نفّذ الحقن فعلًا — يُعرض للمستخدم لأنّ لكل مسار شرطه. */
enum class InjectionLane {
    /** `AccessibilityService.dispatchGesture` — بلا جذر، بشرط تفعيل الخدمة. */
    ACCESSIBILITY,

    /** `input tap/swipe` عبر `PrivilegedShell` (جذر أو Shizuku). */
    PRIVILEGED_SHELL,
}

/** أسباب الرفض — مفاتيح تقنيّة ثابتة، والنصّ العربيّ يعيش في الواجهة. */
enum class InjectionRefusal {
    /** إصبع حقيقيّ على الشاشة الآن: الحقن يُرفض بدل أن يتسابق معه. */
    FINGER_DOWN,

    /** حقنتان متقاربتان أكثر من الحدّ: منع تراكم يبدو كاهتزاز. */
    RATE_LIMITED,

    /** لا خدمة وصول ولا صدفة مميّزة. */
    NO_LANE,

    /** إحداثيات خارج الشاشة. */
    OUT_OF_BOUNDS,
}

/** قراءة الإصبع — والمجهول يبقى مجهولًا ولا يُقرأ «لا إصبع». */
enum class FingerReading { DOWN, UP, UNKNOWN }

/** قراءة المجسّ مع سببها حين تكون [FingerReading.UNKNOWN]. */
data class FingerProbeResult(
    val reading: FingerReading,
    val detail: String? = null,
)

/**
 * قواعد الحقن النقيّة — تُقاس في JVM بلا جهاز.
 *
 * **وحدودها المعلنة:** هذه القواعد تمنع النقر المزدوج المتقارب وتكلّف المجسّ قراءة الإصبع؛
 * أمّا «هل وصلت اللمسة إلى اللعبة فعلًا؟» فليس هنا — يُجيب عنه [InjectionLane] لا هذه القواعد.
 */
object TouchInjectionRules {

    /** أدنى فاصل بين حقنتين من نفس المصدر (~١١ هرتز سقفًا). */
    const val MIN_GAP_MS: Long = 90L

    /** مدّة أعلى لسحبة واحدة يقبلها `input swipe` بلا أن تصير «سحبًا بطيئًا» في الألعاب. */
    const val MAX_SWIPE_MS: Int = 1_500

    /** مدّة أدنى — صفر يجعل السحبة نقرة، وهو ليس ما طُلب. */
    const val MIN_SWIPE_MS: Int = 40

    /** هل النقطة داخل الشاشة بهامش يمنع الحقن على حافة النظام؟ */
    fun inBounds(x: Float, y: Float, widthPx: Int, heightPx: Int, marginPx: Int = 0): Boolean {
        if (widthPx <= 0 || heightPx <= 0 || !x.isFinite() || !y.isFinite()) return false
        val safe = marginPx.coerceAtLeast(0)
        return x >= safe && y >= safe && x <= widthPx - safe && y <= heightPx - safe
    }

    /** تقييد نقطة داخل الشاشة بدل رفض سحبة تنتهي عند الحافة. */
    fun clampX(x: Float, widthPx: Int): Float = x.coerceIn(0f, widthPx.toFloat().coerceAtLeast(0f))

    fun clampY(y: Float, heightPx: Int): Float = y.coerceIn(0f, heightPx.toFloat().coerceAtLeast(0f))

    /** مدّة السحبة بعد التقييد — لا صفر ولا أطول من الحدّ. */
    fun swipeDuration(durationMs: Int): Int = durationMs.coerceIn(MIN_SWIPE_MS, MAX_SWIPE_MS)

    /**
     * الحكم قبل الحقن: `null` يعني «امضِ»، وغيرُه سبب الرفض.
     *
     * **والإصبع المجهول لا يوقف الحقن** — وإلّا لتعطّلت الميزة على جهاز لا يُقرأ فيه `/dev/input`
     * بلا جذر. لكنّه لا يُدَّعى محروسًا: الواجهة تقول «حارس الإصبع غير متاح» بصدق، والقاعدة
     * الوحيدة المضمونة هنا هي منع الحقن المتقارب.
     */
    fun refusal(nowMs: Long, lastInjectedAtMs: Long, finger: FingerReading): InjectionRefusal? = when {
        finger == FingerReading.DOWN -> InjectionRefusal.FINGER_DOWN
        lastInjectedAtMs > 0L && nowMs - lastInjectedAtMs in 0 until MIN_GAP_MS -> InjectionRefusal.RATE_LIMITED
        else -> null
    }
}
