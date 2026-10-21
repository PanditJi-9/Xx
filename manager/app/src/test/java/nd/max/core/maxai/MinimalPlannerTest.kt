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
 * عقد المخطِّط: معادلة الجدوى التي تقرر أي مقبض يتحرك.
 *
 * هذه هي أخطر نقطة في العقل — لو رجّحت المقبض الخشن المكلف أو الذي
 * يُتوقع أن يسخّن، عاد Max AI إلى "اقفز إلى السقف الكامل" وهو بالضبط
 * ما فشل في سجل الجهاز الحقيقي.
 */
class MinimalPlannerTest {

    private fun utility(
        impact: Float,
        cost: Float,
        credibility: Float = 1f,
        thermal: Float = 0f,
    ) = MinimalPlanner.utilityOf(impact, cost, credibility, thermal)

    @Test
    fun `cheaper control wins when impact is equal`() {
        val cheap = utility(impact = 0.1f, cost = 0.1f)
        val costly = utility(impact = 0.1f, cost = 0.5f)
        assertTrue("المقبض الأرخص يجب أن يتقدم", cheap > costly)
    }

    @Test
    fun `higher credible control wins when cost and impact are equal`() {
        val trusted = utility(impact = 0.1f, cost = 0.2f, credibility = 0.9f)
        val contested = utility(impact = 0.1f, cost = 0.2f, credibility = 0.1f)
        assertTrue("المقبض الموثوق يجب أن يتقدم", trusted > contested)
    }

    @Test
    fun `predicted heat penalizes utility even for a big impact`() {
        // هذا هو الحارس: مقبض يرفع الأداء كثيرًا لكنه يسخّن بقوة يجب
        // أن يسقط تحت بديل أبرد — لا أن يفوز بمجرد كبر أثره.
        val hot = utility(impact = 0.5f, cost = 0.2f, credibility = 1f, thermal = 5f)
        val cool = utility(impact = 0.2f, cost = 0.2f, credibility = 1f, thermal = 0f)
        assertTrue("البديل البارد يجب أن يتفوق على الساخن رغم أثر أقل", cool > hot)
    }

    @Test
    fun `negative predicted heat is treated as no penalty`() {
        // التبريد المتوقع لا يمنح مكافأة زائدة عن الحد: النموذج لا
        // يجب أن "يكافئ" مقبضًا لمجرد أنه قد يبرّد.
        val cooling = utility(impact = 0.2f, cost = 0.2f, thermal = -3f)
        val neutral = utility(impact = 0.2f, cost = 0.2f, thermal = 0f)
        assertEquals(cooling, neutral, 0.0001f)
    }

    @Test
    fun `zero cost is floored instead of dividing by zero`() {
        val u = utility(impact = 0.1f, cost = 0f)
        assertTrue("يجب أن يبقى منتهيًا لا لا نهائي", u.isFinite())
        assertTrue(u > 0f)
    }

    @Test
    fun `satisfaction threshold marks a healthy device`() {
        // مبدأ "لا تدخل بلا فجوة": العتبة المعلنة هي الحد الفاصل.
        assertEquals(0.75f, MinimalPlanner.SATISFIED_SCORE, 0.0001f)
    }

    // ── أرضية الاستجابة ────────────────────────────────────

    private fun cpuControl(ladderSize: Int = 10): ControlRegistry.Control {
        val ladder = (0 until ladderSize).map { (it * 100_000).toString() }
        return ControlRegistry.Control(
            key = "cpu_limits:policy0",
            feature = nd.max.core.hardware.HardwareFeature.CPU_FREQUENCY,
            label = "سقف policy0",
            ladder = ladder,
            cost = 0.2f,
            read = { ladder.first() },
            apply = { it },
        )
    }

    @Test
    fun `a save-energy step below the responsive floor is rejected`() {
        // **عطب مقيس على rodin:** `cpu_limits:policy0` بلغ `300000:500000` (٥٠٠ ميجاهرتز)
        // لأن السّلّم يسمح بالنزول إلى الأرضية والهدف لا يتحقّق أبدًا. والمنع هنا سياسة
        // لا شكّ في العتاد: نصف المدى يبقى قادرًا على الاستخدام اليومي.
        val control = cpuControl()
        val floorIndex = ((control.ladder.size - 1) * MinimalPlanner.MIN_RESPONSIVE_CPU_FRACTION).toInt()
        assertTrue(
            "موضع $floorIndex من ${control.ladder.last()} تحت الأرضية",
            MinimalPlanner.belowResponsiveFloor(control, control.ladder[floorIndex - 1]),
        )
        assertTrue(
            "والموضع ٩٠٪ فوقها",
            !MinimalPlanner.belowResponsiveFloor(control, control.ladder[9]),
        )
    }

    @Test
    fun `the responsive floor does not apply to boosting or to non-frequency knobs`() {
        val control = cpuControl()
        // الرفع يذهب إلى أعلى السّلّم أبدًا — والأرضية لا تعني شيئًا في اتجاه الرفع
        // (الاستدعاء المقيّد في المخطّط على SAVE_ENERGY وحده).
        assertFalse(MinimalPlanner.belowResponsiveFloor(control, control.ladder[9]))
        val boost = control.copy(
            key = "cpu_boost",
            feature = nd.max.core.hardware.HardwareFeature.CPU_BOOST,
            ladder = listOf("0", "1"),
        )
        assertFalse(
            "مقبض ثنائي لا مدى ترددات له: لا أرضية استجابة",
            MinimalPlanner.belowResponsiveFloor(boost, "0"),
        )
    }

    @Test
    fun `ranking is a total order on distinct utilities`() {
        val a = utility(impact = 0.3f, cost = 0.1f, credibility = 1f, thermal = 0f)
        val b = utility(impact = 0.3f, cost = 0.3f, credibility = 1f, thermal = 0f)
        val c = utility(impact = 0.1f, cost = 0.1f, credibility = 1f, thermal = 3f)
        val ranked = listOf(a, b, c).sortedDescending()
        assertTrue("الترتيب يجب أن يفصل الثلاثة", ranked[0] > ranked[1] && ranked[1] > ranked[2])
    }
}
