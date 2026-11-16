/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * الضبط هو ما يستهلكه المحرك فعلًا، فيُقاس هنا:
 *
 * - الافتراضي هو ثوابت المحرك نفسها (لا نسخة ثانية تنفصل بصمت).
 * - الهادئ يرضى أسهل (عتبة أدنى) ولا يجرّب.
 * - الحاسم يطلب رضا أعلى ويسمح بميزانية أوسع، ويبقى سقف تكلفته داخل حدّ.
 */
class MaxAiBehaviorTuningTest {

    @Test
    fun `balanced keeps the engine constants`() {
        val tuning = MaxAiBehaviorTuning.of(MaxAiBehavior.BALANCED)

        assertEquals(MinimalPlanner.SATISFIED_SCORE.toDouble(), tuning.satisfactionTarget.toDouble(), 0.0)
        assertEquals(TrustModel.MAX_PROBES_PER_SESSION, tuning.probeBudget)
        assertEquals(TrustModel.MAX_PROBE_COST.toDouble(), tuning.maxProbeCost.toDouble(), 0.0)
        assertTrue(tuning.allowsProbes)
    }

    @Test
    fun `calm is easier to satisfy and never probes`() {
        val calm = MaxAiBehaviorTuning.of(MaxAiBehavior.CALM)
        val balanced = MaxAiBehaviorTuning.of(MaxAiBehavior.BALANCED)

        assertTrue(calm.satisfactionTarget < balanced.satisfactionTarget)
        assertFalse(calm.allowsProbes)
        assertEquals(0.0, calm.maxProbeCost.toDouble(), 0.0)
    }

    @Test
    fun `decisive demands more and allows one wider reversible probe`() {
        val decisive = MaxAiBehaviorTuning.of(MaxAiBehavior.DECISIVE)
        val balanced = MaxAiBehaviorTuning.of(MaxAiBehavior.BALANCED)

        assertTrue(decisive.satisfactionTarget > balanced.satisfactionTarget)
        assertTrue(decisive.probeBudget > balanced.probeBudget)
        assertTrue(decisive.maxProbeCost > balanced.maxProbeCost)
        assertTrue(decisive.maxProbeCost <= 1f)
    }
}
