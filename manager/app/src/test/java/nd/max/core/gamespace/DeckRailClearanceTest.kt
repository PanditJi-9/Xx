/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import nd.max.ui.component.DeckMath
import nd.max.ui.component.deckAppsFit
import nd.max.ui.component.deckMeterBars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * هندسة الصفّ العلوي الأيمن في سطح اللعبة العرضي — رياضيات خالصة تُقاس بلا جهاز.
 *
 * العطب الذي وُجد بلقطة جهاز: خمس دوائر تطبيقات بعرضها الكامل تدخل خطّ الشريط الأيمن بنحو ٣٠dp، فتعلو
 * أولى الدوائر الشريط وإطاره. فالمقاس هنا هو المسافة الفعلية بين محيط أولى الدوائر وخطّ الشريط عبر شبكة أحجام كاملة.
 */
class DeckRailClearanceTest {

    private val widths: List<Float> = (480..1500 step 20).map { it.toFloat() }
    private val heights: List<Float> = (280..520 step 20).map { it.toFloat() }

    @Test
    fun `quick apps never touch the right rail line across the size sweep`() {
        var checked = 0
        for (w in widths) for (h in heights) for (leftOpen in listOf(true, false)) {
            val m = DeckMath.of(w, h, leftOpen)
            if (!m.railOn) continue
            checked++
            val r = m.btn / 2f
            val rowW = m.appsSlots * m.btn + (m.appsSlots - 1) * 10f
            val xc = w - m.edge - rowW + r
            val yc = m.topY + r
            var gap = Float.MAX_VALUE
            for (k in 0 until 120) {
                val a = 2.0 * PI * k / 120
                val px = xc + r * cos(a).toFloat()
                val py = yc + r * sin(a).toFloat()
                gap = minOf(gap, px - DeckMath.rightRailEdge(m, h, py))
            }
            assertTrue("${w.toInt()}x${h.toInt()} leftOpen=$leftOpen gap=$gap", gap >= 5f)
        }
        assertTrue("no layout with rails was measured", checked > 0)
    }

    @Test
    fun `apps row always leaves room for the more and add buttons`() {
        for (w in widths) for (h in heights) for (leftOpen in listOf(true, false)) {
            val m = DeckMath.of(w, h, leftOpen)
            assertTrue("${w.toInt()}x${h.toInt()}: slots=${m.appsSlots}", m.appsSlots >= 2)
            assertTrue("${w.toInt()}x${h.toInt()}: maxW=${m.appsMaxW}", m.appsMaxW >= m.btn * 2f)
        }
    }

    @Test
    fun `apps fit folds only what does not fit and never overflows the slots`() {
        for (slots in 2..9) for (count in 0..9) {
            val f = deckAppsFit(count, slots)
            val circles = f.shown + (if (f.more) 1 else 0) + 1
            assertTrue("count=$count slots=$slots circles=$circles", circles <= slots)
            assertTrue("count=$count slots=$slots shown=${f.shown}", f.shown in 0..count)
            assertEquals("count=$count slots=$slots", f.shown < count, f.more)
        }
    }

    @Test
    fun `apps fit keeps the wide screen behaviour and folds on the narrow one`() {
        // شاشة عريضة: ثلاثة تطبيقات + المزيد + الإضافة كما كان
        assertEquals(3, deckAppsFit(5, 5).shown)
        assertTrue(deckAppsFit(5, 5).more)
        // ثلاثة تطبيقات أو أقلّ تتّسع مع الإضافة: لا زرّ «المزيد»
        assertFalse(deckAppsFit(3, 4).more)
        assertEquals(3, deckAppsFit(3, 4).shown)
        assertFalse(deckAppsFit(0, 2).more)
        // أربع دوائر فقط (حال لقطة الجهاز): تطبيقان + المزيد + الإضافة
        assertEquals(2, deckAppsFit(5, 4).shown)
        assertTrue(deckAppsFit(5, 4).more)
    }

    @Test
    fun `meter bars stay thick enough to read and within the design range`() {
        for (meterH in 56..96) {
            val n = deckMeterBars(meterH.toFloat())
            assertTrue("meterH=$meterH n=$n", n in 4..10)
            val track = meterH - 4f - 28f
            val bar = (track - 2f * (n - 1)) / n
            assertTrue("meterH=$meterH bar=$bar", bar >= 3f - 1e-3f)
        }
    }
}
