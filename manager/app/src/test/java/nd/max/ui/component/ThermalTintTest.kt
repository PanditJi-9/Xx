/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.ui.graphics.Color
import nd.max.core.gamespace.GameThermalProfiles
import nd.max.core.gamespace.ThermalPanelState
import nd.max.core.gamespace.ThermalPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** لون اللوحة يتبع بروفايل الحرارة المختار، والافتراضي والمرفوض يبقيان المظهر المعتاد. */
class ThermalTintTest {

    private fun state(id: String, phase: ThermalPhase = ThermalPhase.Applied) =
        ThermalPanelState(selected = id, phase = phase)

    @Test
    fun `each profile has its own settings color`() {
        assertEquals(Color(0xFF36C77B), ThermalTint.of(state("power")))
        assertEquals(Color(0xFF3D8EFF), ThermalTint.of(state("balanced")))
        assertEquals(Color(0xFFFF9800), ThermalTint.of(state("gaming")))
        assertEquals(Color(0xFFFF4444), ThermalTint.of(state("performance")))
        assertEquals(Color(0xFFB07CFF), ThermalTint.of(state("custom")))
    }

    @Test
    fun `the profiles are visually distinct from each other`() {
        val colors = GameThermalProfiles.ids.filter { it != GameThermalProfiles.DEFAULT }
            .map { ThermalTint.colorOf(it) }
        assertEquals(colors.size, colors.toSet().size)
    }

    @Test
    fun `default keeps the usual look`() {
        assertNull(ThermalTint.of(state(GameThermalProfiles.DEFAULT)))
        assertNull(ThermalTint.of(ThermalPanelState()))
    }

    @Test
    fun `an unknown stored value is treated as default and the old powersave name as power`() {
        assertNull(ThermalTint.of(state("something-else")))
        assertEquals(ThermalTint.colorOf("power"), ThermalTint.of(state("powersave")))
    }

    @Test
    fun `a rejected request is not painted as if it were active`() {
        assertNull(ThermalTint.of(state("gaming", ThermalPhase.Failed)))
    }

    @Test
    fun `a request in flight already takes the profile color`() {
        listOf(ThermalPhase.Idle, ThermalPhase.Applying, ThermalPhase.Pending, ThermalPhase.Applied).forEach { phase ->
            assertNotNull("phase $phase", ThermalTint.of(state("performance", phase)))
        }
    }
}
