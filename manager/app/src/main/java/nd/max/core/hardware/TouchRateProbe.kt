/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

/**
 * عيّنة قياس لمعدّل **أحداث اللمس كما تصل إلى التطبيق**.
 *
 * وهذا ليس معدّل أخذ العيّنات في العتاد، ولا معدّل تحديث اللوحة: هو ما يسلّمه Android إلى
 * هذه الشاشة فعلًا. ولذلك يُسمّى باسمه ([eventsPerSecond]) ويُعرض بجانب المعدّل الأصلي،
 * لا بدلًا منه.
 */
data class TouchRateSample(val events: Int, val durationMs: Long) {
    val eventsPerSecond: Double? get() = TouchRateMath.eventsPerSecond(events, durationMs)
}

/**
 * حساب المعدّل وحده — بلا أندرويد وبلا واجهة، فيُقاس على JVM في كل مراجعة.
 *
 * **ولماذا مدّة دنيا:** عيّنة من حدثين على ٥ مللي ثانية تُعطي «٤٠٠ حدث/ث» وهي ليست معدّلًا
 * بل صدفة توقيت. فما دون [MIN_DURATION_MS] **مجهول** (`null`) لا صفر ولا رقم مبالغ.
 */
object TouchRateMath {

    /** أقلّ مدّة تُقبل للقياس — دونها الخطأ النسبي أكبر من الفرق الذي يُقصد قياسه. */
    const val MIN_DURATION_MS = 400L

    fun eventsPerSecond(events: Int, durationMs: Long): Double? =
        if (events <= 0 || durationMs < MIN_DURATION_MS) null
        else events * 1000.0 / durationMs
}

/** قراءات نظام عن مسار اللمس — كلٌّ منها بحدوده المعلنة. */
object TouchRateProbe {

    /**
     * معدّل تحديث اللوحة الحالي (Hz) من `Display.mode` — **قراءة نظام، لا قياس لمس**.
     *
     * ويعود `null` حين لا يجيب النظام: خدمة غائبة أو نمط مجهول. والمجهول لا يُعرض صفرًا
     * (ADR-07) — يُعرض «غير متاح» مع سببه.
     */
    @Suppress("DEPRECATION")
    fun displayRefreshRate(context: Context): Float? = runCatching {
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val mode = manager?.getDisplay(Display.DEFAULT_DISPLAY)?.mode
        mode?.refreshRate?.takeIf { it > 0f }
    }.getOrNull()
}
