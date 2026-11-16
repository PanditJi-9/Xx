/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MaxAiLookaheadTest {

    @Test
    fun `a projected improvement is allowed`() {
        val verdict = MaxAiLookahead.assess(
            currentScore = 0.60f,
            currentThermalC = 40f,
            predictedGain = 0.05f,
            predictedThermalC = 1.0f,
            raisingPerformance = true,
        )

        assertTrue(verdict.allowed)
        assertNull(verdict.rejection)
        assertEquals(0.65, verdict.projection.nextScore.toDouble(), 0.0001)
    }

    @Test
    fun `a projected score regression is rejected`() {
        val verdict = MaxAiLookahead.assess(
            currentScore = 0.60f,
            currentThermalC = 40f,
            predictedGain = -0.02f,
            predictedThermalC = 0f,
            raisingPerformance = true,
        )

        assertFalse(verdict.allowed)
        assertEquals(MaxAiLookahead.Rejection.NEXT_STATE_WORSE, verdict.rejection)
    }

    @Test
    fun `a raise that would cross the thermal margin is rejected`() {
        val verdict = MaxAiLookahead.assess(
            currentScore = 0.60f,
            currentThermalC = 45f,
            predictedGain = 0.05f,
            predictedThermalC = 1.5f,
            raisingPerformance = true,
        )

        assertFalse(verdict.allowed)
        assertEquals(MaxAiLookahead.Rejection.THERMAL_CEILING, verdict.rejection)
    }

    @Test
    fun `an unknown thermal reading does not invent safety`() {
        val verdict = MaxAiLookahead.assess(
            currentScore = 0.60f,
            currentThermalC = 0f,
            predictedGain = 0.05f,
            predictedThermalC = 2f,
            raisingPerformance = true,
        )

        assertTrue(verdict.allowed)
        assertNull(verdict.rejection)
    }

    @Test
    fun `a saving step is not blocked by the raise ceiling`() {
        val verdict = MaxAiLookahead.assess(
            currentScore = 0.60f,
            currentThermalC = 47f,
            predictedGain = -0.005f,
            predictedThermalC = -1f,
            raisingPerformance = false,
        )

        assertTrue(verdict.allowed)
        assertNull(verdict.rejection)
    }

    @Test
    fun `small negative noise is not a regression`() {
        val verdict = MaxAiLookahead.assess(
            currentScore = 0.60f,
            currentThermalC = 40f,
            predictedGain = -0.005f,
            predictedThermalC = 0f,
            raisingPerformance = true,
        )

        assertTrue(verdict.allowed)
        assertNull(verdict.rejection)
    }
}
