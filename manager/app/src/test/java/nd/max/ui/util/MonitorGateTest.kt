/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import nd.max.core.platform.HudChannelProbe
import nd.max.core.platform.HudChannelStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حدود بوّابة الإيقاع ودلالة الفلتر — القواعد الصافية التي تُقاس بلا شاشة ولا جهاز.
 *
 * **ولماذا هنا:** الفاصل والفلتر يعيشان في التفضيلات، لكن ما يقرّرهما هو هاتان الدالّتان.
 * ولو لم تُختبرا لصار «الفاصل» رقمًا محفوظًا لا أثر له، وصار `Chart` بلا حقل إطارات
 * صندوقًا فارغًا على الشاشة.
 */
class MonitorGateTest {

    @Test
    fun `the first sample always passes`() {
        val gate = MonitorGate(1000)
        assertTrue(gate.accept(5_000))
    }

    @Test
    fun `a sample before the interval is refused, one on the interval passes`() {
        val gate = MonitorGate(1000)
        assertTrue(gate.accept(1_000))
        assertFalse(gate.accept(1_500))
        assertFalse(gate.accept(1_999))
        assertTrue(gate.accept(2_000))
    }

    @Test
    fun `zero and negative intervals never block`() {
        val gate = MonitorGate(0)
        assertTrue(gate.accept(10))
        assertTrue(gate.accept(10))
        gate.updateInterval(-500)
        assertTrue(gate.accept(11))
    }

    @Test
    fun `changing the interval applies to the next gap, not to the past`() {
        val gate = MonitorGate(3000)
        assertTrue(gate.accept(0))
        gate.updateInterval(500)
        assertFalse(gate.accept(400))
        assertTrue(gate.accept(500))
    }

    @Test
    fun `reset makes the next sample pass regardless of the interval`() {
        val gate = MonitorGate(3000)
        assertTrue(gate.accept(0))
        assertFalse(gate.accept(100))
        gate.reset()
        assertTrue(gate.accept(100))
    }
}

class HudFilterTest {

    @Test
    fun `chart filter hides text only when a chart is possible`() {
        assertFalse(hudTextVisible(HudFilter.Chart, chartPossible = true))
        assertTrue(hudTextVisible(HudFilter.Chart, chartPossible = false))
        assertTrue(hudTextVisible(HudFilter.Text, chartPossible = true))
        assertTrue(hudTextVisible(HudFilter.All, chartPossible = true))
    }

    @Test
    fun `chart filter forces the chart, all follows the switch, text never draws it`() {
        assertTrue(hudChartVisible(HudFilter.Chart, showGraph = false, chartPossible = true))
        assertTrue(hudChartVisible(HudFilter.All, showGraph = true, chartPossible = true))
        assertFalse(hudChartVisible(HudFilter.All, showGraph = false, chartPossible = true))
        assertFalse(hudChartVisible(HudFilter.Text, showGraph = true, chartPossible = true))
    }

    @Test
    fun `no frames field means no chart, whatever the filter asks`() {
        assertFalse(hudChartVisible(HudFilter.Chart, showGraph = true, chartPossible = false))
        assertTrue(hudTextVisible(HudFilter.Chart, chartPossible = false))
    }
}

class HudChannelProbeTest {

    @Test
    fun `classify reports three states and never guesses`() {
        assertEquals(HudChannelStatus.Missing, HudChannelProbe.classify(exists = false, readable = false))
        assertEquals(HudChannelStatus.Missing, HudChannelProbe.classify(exists = false, readable = true))
        assertEquals(HudChannelStatus.Denied, HudChannelProbe.classify(exists = true, readable = false))
        assertEquals(HudChannelStatus.Readable, HudChannelProbe.classify(exists = true, readable = true))
    }
}
