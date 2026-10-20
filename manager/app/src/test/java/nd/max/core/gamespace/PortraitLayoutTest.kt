/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** هندسة اللوحة الطولية: دالة في (عرض، ارتفاع) لا ترمي ولا تتقاطع أعمدتها على أي هاتف. */
class PortraitLayoutTest {

    private val widths = (280..1000 step 20).map { it.toFloat() }
    private val heights = (360..2400 step 60).map { it.toFloat() }

    @Test
    fun `portrait is a height greater than width`() {
        assertTrue(CockpitModel.isPortrait(360f, 800f))
        assertFalse(CockpitModel.isPortrait(872f, 393f))
        // مربّع تمامًا ⇒ العرضي، فلا يتأرجح التخطيط على الحدّ.
        assertFalse(CockpitModel.isPortrait(600f, 600f))
    }

    @Test
    fun `the card fits the screen and the two gauges tile its width exactly`() {
        var cases = 0
        for (w in widths) {
            for (h in heights) {
                cases++
                val l = PortraitLayoutMath.of(w, h)
                val where = "${w.toInt()}x${h.toInt()}"
                assertTrue("$where: cardW=${l.cardW}", l.cardW <= maxOf(w, 280f) && l.cardW <= PortraitLayoutMath.MAX_CARD_W)
                assertTrue("$where: cardMaxH=${l.cardMaxH}", l.cardMaxH < maxOf(h, 360f))
                assertEquals("$where: gauges", l.inner, l.gaugeW * 2f + l.gap, 0.01f)
                assertTrue("$where: gaugeW=${l.gaugeW}", l.gaugeW >= 100f)
            }
        }
        assertTrue(cases > 500)
    }

    @Test
    fun `tiles fit their columns and every touch target is at least 40dp`() {
        for (w in widths) {
            for (h in heights) {
                val l = PortraitLayoutMath.of(w, h)
                val where = "${w.toInt()}x${h.toInt()}"
                assertEquals("$where: columns", l.inner, l.tileColW * l.tileCols + l.gap * (l.tileCols - 1), 0.01f)
                assertTrue("$where: tile=${l.tile} col=${l.tileColW}", l.tile <= l.tileColW && l.tile >= 40f)
                assertTrue("$where: btn=${l.btn}", l.btn >= 48f)
                assertTrue("$where: option=${l.optionH} slider=${l.sliderH}", l.optionH >= 40f && l.sliderH >= 40f)
                assertTrue("$where: numberSp=${l.numberSp}", l.numberSp in 20f..32f)
            }
        }
    }

    @Test
    fun `a wide portrait tablet does not stretch the card`() {
        val l = PortraitLayoutMath.of(900f, 1400f)
        assertEquals(PortraitLayoutMath.MAX_CARD_W, l.cardW, 0.01f)
    }

    @Test
    fun `the card leaves the game visible above it and beside it`() {
        for (w in widths) {
            for (h in heights) {
                val l = PortraitLayoutMath.of(w, h)
                val where = "${w.toInt()}x${h.toInt()}"
                assertTrue(
                    "$where: cardMaxH=${l.cardMaxH}",
                    l.cardMaxH <= maxOf(h, 360f) * PortraitLayoutMath.HEIGHT_FRACTION + 0.01f
                )
                assertTrue(
                    "$where: cardW=${l.cardW}",
                    l.cardW <= maxOf(w, 280f) * PortraitLayoutMath.WIDTH_FRACTION + 0.01f ||
                        l.cardW >= PortraitLayoutMath.MAX_CARD_W - 0.01f
                )
            }
        }
    }
}
