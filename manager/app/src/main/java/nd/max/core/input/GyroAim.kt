/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.input

/**
 * إعدادات مساعد التصويب الجيروسكوبيّ.
 *
 * [sensitivity] مضاعِف حساسية (٠٫٢٥–٣٫٠)، و[deadZone] منطقة ميتة بالدرجات/ثانية تمنع
 * الاهتزاز الدقيق من تحريك الكاميرا، و[smoothing] نسبة من الخطوة السابقة تُضاف للحالية
 * (٠–٠٫٨) فتُنعّم الحركة بدل أن تتقافز.
 */
data class GyroAimSettings(
    val enabled: Boolean = false,
    val sensitivity: Float = 1.0f,
    val deadZone: Float = 1.5f,
    val smoothing: Float = 0.35f,
) {
    fun normalized(): GyroAimSettings = copy(
        sensitivity = sensitivity.coerceIn(GyroAimMath.MIN_SENSITIVITY, GyroAimMath.MAX_SENSITIVITY),
        deadZone = deadZone.coerceIn(0f, GyroAimMath.MAX_DEAD_ZONE),
        smoothing = smoothing.coerceIn(0f, GyroAimMath.MAX_SMOOTHING),
    )
}

/** خطوة تصويب واحدة بالبكسل — موجبة لليمين/الأسفل. */
data class GyroAimDelta(val dx: Float, val dy: Float) {
    val isStill: Boolean get() = kotlin.math.abs(dx) < 0.5f && kotlin.math.abs(dy) < 0.5f
}

/**
 * حساب خطوة التصويب — **نقيّ بالكامل** كي يُقاس بلا جهاز ولا حسّاس.
 *
 * ### الوحدة والاتجاه
 *
 * مدخلات الجيروسكوب راديان/ثانية (كما يعطيها Android). تُحوَّل هنا إلى درجات، ثم تُضرب
 * بالزمن الفعليّ بين عيّنتين — فالخطوة تتبع الزمن لا عدد العيّنات، ولا تتضاعف على شاشة
 * ١٢٠ هرتز. والدوران حول محور الشاشة (z) يحريك أفقيًّا، وحول x يحريك رأسيًّا.
 *
 * ### لماذا لا يُدّعى «تصويب دقيق»
 *
 * نُولّد سحبات صغيرة في مركز الشاشة؛ اللعبة التي تفهم السحب كتحريك كاميرا تستجيب، والتي
 * تحتاج عصا تحكّم افتراضيّة قد تتجاهلها. هذا **حدّ يُعلن**، لا عيب يُخفى.
 */
object GyroAimMath {

    /** بكسل لكل درجة دوران — قيمة وسطى قابلة للمعايرة بالمضاعِف. */
    const val PIXELS_PER_DEGREE = 6f

    /** سقف الخطوة الواحدة: قفزة أكبر تُقرأ في اللعبة كنبضة لا كتصويب. */
    const val MAX_STEP_PX = 48f

    /** أقلّ فرق زمنيّ يُحسب؛ ما دونه ضجيج مستشعر. */
    const val MIN_DT_MS = 8L

    const val MIN_SENSITIVITY = 0.25f
    const val MAX_SENSITIVITY = 3.0f
    const val MAX_DEAD_ZONE = 8.0f
    const val MAX_SMOOTHING = 0.8f

    private const val RAD_TO_DEG = 57.295_78f

    fun step(
        yawRadPerSec: Float,
        pitchRadPerSec: Float,
        dtMs: Long,
        settings: GyroAimSettings,
        previousDx: Float = 0f,
        previousDy: Float = 0f,
    ): GyroAimDelta {
        val safe = settings.normalized()
        val dt = dtMs.coerceAtLeast(0L).coerceAtMost(250L)
        if (dt < MIN_DT_MS) return GyroAimDelta(0f, 0f)
        val yawRate = deadband(yawRadPerSec * RAD_TO_DEG, safe.deadZone)
        val pitchRate = deadband(pitchRadPerSec * RAD_TO_DEG, safe.deadZone)
        val seconds = dt / 1000f
        val rawDx = yawRate * seconds * PIXELS_PER_DEGREE * safe.sensitivity
        val rawDy = pitchRate * seconds * PIXELS_PER_DEGREE * safe.sensitivity
        val dx = smooth(rawDx, previousDx, safe.smoothing).coerceIn(-MAX_STEP_PX, MAX_STEP_PX)
        val dy = smooth(rawDy, previousDy, safe.smoothing).coerceIn(-MAX_STEP_PX, MAX_STEP_PX)
        return GyroAimDelta(dx, dy)
    }

    /** المنطقة الميتة تُطرح لا تُصفَّر فجأة: الخروج منها يبدأ من الصفر بسلاسة. */
    fun deadband(value: Float, deadZone: Float): Float {
        val zone = deadZone.coerceIn(0f, MAX_DEAD_ZONE)
        if (zone <= 0f) return value
        return if (kotlin.math.abs(value) <= zone) {
            0f
        } else {
            value - kotlin.math.sign(value) * zone
        }
    }

    fun smooth(raw: Float, previous: Float, smoothing: Float): Float {
        val s = smoothing.coerceIn(0f, MAX_SMOOTHING)
        return raw * (1f - s) + previous * s
    }

    fun sensitivityAfter(current: Float, delta: Float): Float =
        (current + delta).coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY)

    fun deadZoneAfter(current: Float, delta: Float): Float =
        (current + delta).coerceIn(0f, MAX_DEAD_ZONE)
}
