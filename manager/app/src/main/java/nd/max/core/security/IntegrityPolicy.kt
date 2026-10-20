/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.security

/**
 * السياسة: **أيّ إشارة تُحتسب، ومتى يُرفض التشغيل** — منطق خالص يُقاس في JVM.
 *
 * وفصلها عن [IntegrityProbe] مقصود: هذه هي القرارات التي يُخطئ المطوّر فيها بلا أن يرى
 * (وهو ما يوقع أدوات الحماية في خانة «تعطّل التطبيق على أجهزة سليمة»)، فتُكتب في ملفّ
 * صغير وتُقاس بإثباتين متعاكسين على الأقلّ: أنه **لا يعترض على السليم**، وأنه
 * **يعترض على المعدَّل**.
 *
 * ## القاعدتان المعلنتان
 *
 * 1. **ما لا يمكن أن يقع في نسخة `release` سليمة لا يُحتسب تنقيحًا في نسخة قابلة للتنقيح.**
 *    مصحّحٌ مرتبط بعملية هو **الوضع الطبيعي** في بناء debug (هكذا يُطوَّر التطبيق)، وهو
 *    **المستحيل** في بناء release غير قابل للتنقيح. فإشارةُ «مصحّح مرتبط» تُسقَط كليًّا في
 *    بناء debug، وتُحتسب في بناء release. بدون هذه القاعدة كان كل مطوّر يُشغّل تطبيقه
 *    من Android Studio يرى أربع نقاط تحقّق فيصير «مخترقًا» بنفسه — وهو عطبٌ قِيس فعلًا
 *    في تصميم هذه الطبقة قبل أن تُضاف هذه القاعدة.
 *
 * 2. **الرفض في نسخة الإصدار وحدها.** بناء debug يُرصد ويُسجَّل ولا يرفض — لأن ثمن الرفض
 *    فيه أن يتعذّر تطوير التطبيق نفسه، وهو ثمن لا يقابله مكسب: نسخة debug لا تُوزَّع.
 */
data class IntegrityPolicy(
    /** هل نرفض التشغيل فعلًا عند تأكيد التلاعب؟ (`release` نعم، `debug` لا.) */
    val enforce: Boolean,
    /** هل الحزمة قابلة للتنقيح؟ تُغيّر معنى إشارتَي المصحّح والمتتبّع. */
    val debuggable: Boolean,
    /** عتبة التصديق للإشارات غير القاطعة. */
    val softThreshold: Int = IntegrityLedger.DEFAULT_SOFT_THRESHOLD,
) {

    /**
     * هل تُحتسب هذه الإشارة في بناءٍ بهذه السياسة؟
     *
     * والقاعدة صريحة ومكتوبة بمعادلة واحدة: `debuggable && signal ∈ DEBUG_EXPECTED` ⇒ لا.
     * وما عدا ذلك تُحتسب — فالإشارة لا تُسقَط إلّا بسبب يخصّ نوع البناء نفسه، لا لأنها
     * «مزعجة».
     */
    fun counts(signal: IntegritySignal): Boolean = !(debuggable && signal in DEBUG_EXPECTED)

    /** الحكم الذي تُنتجه هذه السياسة من سجلّ — الرفض على [IntegrityVerdict.TAMPERED] و[enforce] معًا. */
    fun refuses(ledger: IntegrityLedger): Boolean = enforce && ledger.verdict() == IntegrityVerdict.TAMPERED

    companion object {
        /**
         * إشارات هي **الوضع الطبيعي** في بناء قابل للتنقيح، ومستحيلة في بناء غير قابل له.
         * ولذلك معناها ينقلب بانقلاب نوع البناء: عاديّة هناك، وقاطعة هنا.
         */
        val DEBUG_EXPECTED: Set<IntegritySignal> = setOf(
            IntegritySignal.DEBUGGER_ATTACHED,
            IntegritySignal.TRACER_ATTACHED,
        )
    }
}
