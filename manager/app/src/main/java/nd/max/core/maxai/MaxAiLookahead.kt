/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

/**
 * نظرة استباقية خطوة واحدة — هل الحالة **بعد** الخطوة أسوأ من الحالة الآن؟
 *
 * المشكلة التي تحلها: [MinimalPlanner] يرتّب المرشحين بأثرهم المتوقع، لكنه كان
 * يقبل الخطوة إذا كان أثرها المتوقع إيجابيًا، حتى لو كان التنبؤ نفسه يقول إن
 * الحالة التالية ستكون أسوأ. خطوة تُحسّن رقمًا الآن ثم تُسقطه بعدها ليست
 * تدخلًا ذكيًا، بل تأجيلًا للضرر.
 *
 * **وهي ليست محاكاة كاملة:** لا تبحث عن الخطوة الثانية، ولا تعيد تشغيل
 * المخطّط على حالة مُعدَّلة. إنها تقدير أثر الخطوة الحالية على الحالة، من نفس
 * تنبؤ [ResponseModel] الذي استُخدم في الترتيب. هذا حدّها المعلن، لا يُخفى.
 *
 * **والحرارة المجهولة ليست صفرًا آمنًا:** إذا كانت القراءة الحالية صفرًا
 * (لا مستشعر أو لا قراءة)، يُتخطّى فحص السقف الحراري بدل أن يُفهم الصفر
 * كأنه برودة مثالية. والسلامة تبقى فوق هذا الفحص دائمًا.
 */
object MaxAiLookahead {

    /** سبب رفض استباقي — يُترجم إلى ثابت [MaxAiRejection] في المخطّط. */
    enum class Rejection {
        /** الدرجة المتوقعة بعد الخطوة أقل من الحالة الآن بفارق يستحق الرفض. */
        NEXT_STATE_WORSE,

        /** الخطوة ترفع الأداء لكنها تتجاوز هامش الحرارة المتوقع. */
        THERMAL_CEILING,
    }

    /** الحالة الآن والمتوقعة بعد الخطوة — أرقام صريحة، لا حكم مخفي. */
    data class Projection(
        val currentScore: Float,
        val nextScore: Float,
        val currentThermalC: Float,
        val nextThermalC: Float,
    )

    /** حكم واحد: مسموح، أو مرفوض بسبب مسمّى، مع الإسقاط الذي بُني عليه. */
    data class Verdict(
        val allowed: Boolean,
        val rejection: Rejection?,
        val projection: Projection,
    )

    /**
     * يقيس أثر خطوة واحدة على الدرجة والحرارة.
     *
     * @param currentScore درجة الهدف الآن (0..1).
     * @param currentThermalC الحرارة المقيسة الآن بالدرجة، أو 0 إن كانت مجهولة.
     * @param predictedGain تغيّر الدرجة المتوقع من [ResponseModel].
     * @param predictedThermalC تغيّر الحرارة المتوقع بالدرجة.
     * @param raisingPerformance هل الخطوة ترفع الأداء؟ سقف الحرارة لا يُطبَّق
     *        على خطوة ترشيد، لأنها ليست مصدر حرارة متوقع.
     */
    fun assess(
        currentScore: Float,
        currentThermalC: Float,
        predictedGain: Float,
        predictedThermalC: Float,
        raisingPerformance: Boolean,
    ): Verdict {
        val safeCurrent = currentScore.coerceIn(0f, 1f)
        val nextScore = (safeCurrent + predictedGain).coerceIn(0f, 1f)
        val nextThermalC = currentThermalC + predictedThermalC
        val projection = Projection(
            currentScore = safeCurrent,
            nextScore = nextScore,
            currentThermalC = currentThermalC,
            nextThermalC = nextThermalC,
        )

        // فرق صغير جدًا ضجيج تنبؤ لا قرار: لا نرفض خطوة مفيدة بسبب 0.001.
        if (safeCurrent - nextScore > NEXT_SCORE_REGRESSION) {
            return Verdict(false, Rejection.NEXT_STATE_WORSE, projection)
        }

        if (raisingPerformance && currentThermalC > 0f &&
            nextThermalC >= NEXT_THERMAL_CEILING_C
        ) {
            return Verdict(false, Rejection.THERMAL_CEILING, projection)
        }

        return Verdict(true, null, projection)
    }

    /**
     * أقل فرق درجة يستحق الرفض — 0.01.
     *
     * قيمة تصميم غير معايَرة على جهاز: أصغر من فجوة القرار المعتادة، وأكبر من
     * ضجيج التنبؤ، فلا تحوّل الخطأ الصغير إلى شلل.
     */
    const val NEXT_SCORE_REGRESSION = 0.01f

    /**
     * هامش الحرارة للخطوات الرافعة — 46°م، أي درجتان تحت عتبة تدخل السلامة
     * (48°م). ليس سياسة سلامة ثانية: [SafetyEngine] يبقى صاحب القرار الأخير،
     * وهذا فحص استباقي يمنع الوصول إليه أصلًا. وقيمته تصميمية غير معايَرة.
     */
    const val NEXT_THERMAL_CEILING_C = 46f
}
