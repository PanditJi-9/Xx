/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

/**
 * إيقاع حلقة المراقبة السريعة — **ماذا تفعل** كل نبضة لا **متى** فقط.
 *
 * ولماذا وُجد هذا الملف: الحلقة كانت تعمل من `start()` ولا تتوقّف أبدًا، وتنفّذ في كل ثانية
 * لقطةً كاملة (`DeviceStateCollector.collect`: بثّ البطارية، `/proc/stat`، عقدة الحرارة،
 * عدّادات الشبكة)، وتستدعي `PredictorBridge.predictThermal` (JNI إلى Rust)، وتُقيّم الأمان،
 * وتُسجّل حلقة. وكل ذلك **والمحرك مطفأ** كما وهو يعمل: دورة القرار ترجع مبكّرًا، أما الحلقة
 * السريعة فلا تعرف أن AI مطفأ أصلًا. فمن يُطفئ Max AI لا يُوقف عمله الخلفي — وهو نصّ الشكوى
 * «إيقاف AI لم يوقف الحرارة/اللاغ».
 *
 * والقاعدة هنا: **العمل الخلفي يتبع الحالة**. مطفأ ⇒ حاجز الطوارئ وحده (قراءة حرارة واحدة
 * كل ربع دقيقة، بلا لقطة ولا تنبؤ ولا سجل). يعمل وشاشة مضاءة ⇒ ثانية. يعمل وشاشة مطفأة ⇒
 * خمس ثوانٍ (الحرارة لا تتحرّك أسرع، فالتوفير مجاني في الدقة).
 *
 * والدوال نقية بلا وقت داخلي ولا Android، فتُقاس باختبار JVM: قرار «ماذا نعمل الآن» هو
 * أوّل ما يجب أن يثبت أنه لا يتحوّل إلى استطلاع أبديّ.
 */
object MaxAiDutyCycle {

    /** نبضة الحلقة والمحرك يعمل والشاشة مضاءة. */
    const val ACTIVE_MS = 1_000L

    /** نبضة الحلقة والمحرك يعمل والشاشة مطفأة — الحرارة ثابتة على مدى دقائق. */
    const val SCREEN_OFF_MS = 5_000L

    /**
     * نبضة حاجز الطوارئ والمحرك مطفأ.
     *
     * ربع دقيقة: ثابت الحرارة (`EMERGENCY_TEMP_C = 58`) أعلى من كل عتبات الاستخدام، والحرارة
     * الفيزيائية تتحرّك في دقائق لا ثوانٍ — فقراءة كل ثانية وهي مطفأة تُدفع ثمنًا بلا معلومة
     * جديدة. والفرق العملي: ٤ نبضات في الدقيقة بدل ٦٠، أي **٩٣٪ أقل استطلاع** في وضع الإطفاء.
     */
    const val IDLE_GUARD_MS = 15_000L

    /** ما تفعله النبضة، مع سبب قابل للعرض في التشخيص. */
    data class Duty(
        /** true = لقطة كاملة + تنبؤ + تقييم أمان + توقيت. false = حرارة واحدة فقط. */
        val fullSample: Boolean,
        val delayMs: Long,
        val reason: String,
    )

    /** أسباب الإيقاع — ثوابت تُترجم في التشخيص بلا تخمين. */
    object Reason {
        const val ACTIVE = "active"
        const val SCREEN_OFF = "screen_off"
        const val IDLE_GUARD = "idle_guard"
    }

    /**
     * قرار النبضة القادمة.
     *
     * @param aiEnabled هل Max AI مفعّل الآن؟
     * @param screenOn حالة الشاشة من النبضة السابقة (الافتراض `true` قبل أول قياس: الأمان
     *   لا يُبطأ بسبب فشل قراءة).
     */
    fun fast(aiEnabled: Boolean, screenOn: Boolean): Duty = when {
        !aiEnabled -> Duty(fullSample = false, delayMs = IDLE_GUARD_MS, reason = Reason.IDLE_GUARD)
        screenOn -> Duty(fullSample = true, delayMs = ACTIVE_MS, reason = Reason.ACTIVE)
        else -> Duty(fullSample = true, delayMs = SCREEN_OFF_MS, reason = Reason.SCREEN_OFF)
    }
}
