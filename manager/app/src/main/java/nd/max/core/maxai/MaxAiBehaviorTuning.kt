/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

/**
 * ما يعنيه سلوك واحد بالأرقام التي يستهلكها المحرك.
 *
 * [satisfactionTarget] هي عتبة الرضا: **أخفضها يعني أن الجهاز يُعدّ راضيًا أسهل**،
 * وبالتالي تدخل أقل. ولذلك الهادئ أقل من عتبة المحرك والحاسم أعلى — وهو اتجاه
 * يبدو معكوسًا لمن يقرأ «هادئ = عتبة أعلى»، لكن المعنى المحسوب هنا لا الاسم.
 *
 * وقيم [MaxAiBehavior.BALANCED] **ليست نسخة ثانية** من ثوابت المحرك: تُقرأ من
 * `MinimalPlanner.SATISFIED_SCORE` و`TrustModel.MAX_PROBES_PER_SESSION` و
 * `MAX_PROBE_COST` نفسها، فمن عدّل عتبة المحرك تتغيّر معه القيمة الافتراضية ولا
 * ينفصل الرقمان بصمت.
 */
data class MaxAiTuning(
    val satisfactionTarget: Float,
    val probeBudget: Int,
    val maxProbeCost: Float,
) {
    init {
        require(satisfactionTarget in 0f..1f) { "satisfaction target must be a fraction" }
        require(probeBudget >= 0) { "probe budget cannot be negative" }
        require(maxProbeCost in 0f..1f) { "probe cost ceiling must be a fraction" }
    }

    /** هل يسمح هذا السلوك بتجربة معرفية أصلًا؟ */
    val allowsProbes: Boolean get() = probeBudget > 0
}

object MaxAiBehaviorTuning {

    /**
     * إزاحة ثابتة حول عتبة المحرك: ٧٪.
     *
     * ليست معايَرة على جهاز (design value). الغرض منها أن تكون الإزاحة **صغيرة**
     * بما يكفي لتبقى داخل نطاق القرار نفسه، ومعلنة هنا كي تُصحَّح بالقياس لاحقًا
     * بدل أن تُدفن أرقامها في ثلاثة فروع.
     */
    private const val SATISFACTION_DELTA = 0.07f

    /** الخطوة الثانية من سقف التكلفة للحاسم — ما زالت داخل سقف السلامة. */
    private const val DECISIVE_COST_STEP = 0.10f

    fun of(behavior: MaxAiBehavior): MaxAiTuning = when (behavior) {
        MaxAiBehavior.CALM -> MaxAiTuning(
            satisfactionTarget = (MinimalPlanner.SATISFIED_SCORE - SATISFACTION_DELTA).coerceIn(0f, 1f),
            probeBudget = 0,
            maxProbeCost = 0f,
        )

        MaxAiBehavior.BALANCED -> MaxAiTuning(
            satisfactionTarget = MinimalPlanner.SATISFIED_SCORE,
            probeBudget = TrustModel.MAX_PROBES_PER_SESSION,
            maxProbeCost = TrustModel.MAX_PROBE_COST,
        )

        MaxAiBehavior.DECISIVE -> MaxAiTuning(
            satisfactionTarget = (MinimalPlanner.SATISFIED_SCORE + SATISFACTION_DELTA).coerceIn(0f, 1f),
            probeBudget = TrustModel.MAX_PROBES_PER_SESSION + 1,
            maxProbeCost = (TrustModel.MAX_PROBE_COST + DECISIVE_COST_STEP).coerceIn(0f, 1f),
        )
    }
}
