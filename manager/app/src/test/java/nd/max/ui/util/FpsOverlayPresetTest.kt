/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import nd.max.core.platform.HudArrangement
import nd.max.core.platform.HudField
import nd.max.core.platform.HudForm
import nd.max.core.platform.HudPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §2 — الاختصاران المصغّر/الموسّع: **لقطة إعداد لا نمطًا مفروضًا**.
 *
 * أخطر ما يُقاس هنا هو «الاستنتاج»: هل يعرف المنتقي أيّ اختصار قائم **بحرفه**؟ تظليل اختصار
 * غير مطبَّق يعِد بما ليس قائمًا؛ والرجوع إلى «مخصّص» (بلا تظليل) بعد أي تعديل فردي هو الفرق
 * بين واجهة صادقة وأخرى تُوهم بأنها في نمط لم تعد فيه.
 *
 * ولا `Context` هنا: الدوالّ كلها على [FpsOverlayPrefs.State] وحدها، فتُقاس على JVM بلا أندرويد.
 */
class FpsOverlayPresetTest {

    private val base = FpsOverlayPrefs.State(
        enabled = true,
        arrangement = HudArrangement.Stack,
        colorHex = "#112233",
        backgroundAlpha = 0.3f,
        widthScale = 1.4f,
        graphSpan = 40,
        snapEdges = false
    )

    @Test
    fun `compact sets the four components it owns`() {
        val compact = base.withPreset(HudPreset.Compact)
        assertEquals(HudForm.Strip, compact.form)
        assertEquals(setOf(HudField.Frames, HudField.Cpu, HudField.Heat), compact.fields)
        assertEquals(12f, compact.textSizeSp, 0.001f)
        assertFalse(compact.showGraph)
    }

    @Test
    fun `expanded carries every field there is`() {
        val expanded = base.withPreset(HudPreset.Expanded)
        assertEquals(HudForm.Pane, expanded.form)
        assertEquals(HudField.entries.toSet(), expanded.fields)
        assertEquals(16f, expanded.textSizeSp, 0.001f)
        assertTrue(expanded.showGraph)
    }

    @Test
    fun `a preset never touches what it does not claim to set`() {
        // الشكل والحقول والحجم والمنحنى فقط — وأمّا اللون والشفافية ومعامل العرض ومدّة المنحنى
        // والالتصاق والتفعيل فهوية اختارها المستخدم ولا يمسّها اختصار «عرض سريع».
        for (preset in HudPreset.entries) {
            val after = base.withPreset(preset)
            assertEquals(base.enabled, after.enabled)
            assertEquals(base.arrangement, after.arrangement)
            assertEquals(base.colorHex, after.colorHex)
            assertEquals(base.backgroundAlpha, after.backgroundAlpha, 0.001f)
            assertEquals(base.widthScale, after.widthScale, 0.001f)
            assertEquals(base.graphSpan, after.graphSpan)
            assertEquals(base.snapEdges, after.snapEdges)
        }
    }

    @Test
    fun `presetOrNull recognises the exact state and nothing else`() {
        assertEquals(HudPreset.Compact, base.withPreset(HudPreset.Compact).presetOrNull())
        assertEquals(HudPreset.Expanded, base.withPreset(HudPreset.Expanded).presetOrNull())
        // تعديل فردي واحد بعد الاختصار ⇒ «مخصّص» بصدق: لا تظليل اختصار لم يُطبَّق.
        assertNull(
            base.withPreset(HudPreset.Compact)
                .copy(fields = setOf(HudField.Frames))
                .presetOrNull()
        )
        assertNull(base.withPreset(HudPreset.Compact).copy(textSizeSp = 13f).presetOrNull())
        assertNull(base.withPreset(HudPreset.Compact).copy(showGraph = true).presetOrNull())
        assertNull(base.withPreset(HudPreset.Expanded).copy(form = HudForm.Badge).presetOrNull())
        // والحالة الابتدائية (المخصّصة أصلًا) لا تطابق شيئًا.
        assertNull(base.presetOrNull())
    }
}
