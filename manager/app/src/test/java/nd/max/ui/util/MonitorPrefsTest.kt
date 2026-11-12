/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import nd.max.core.platform.HudArrangement
import nd.max.core.platform.HudField
import nd.max.core.platform.HudForm
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حدود القراءة في [MonitorPrefs] — لا الشكل السعيد وحده.
 *
 * **ولماذا هذه الاختبارات:** هذا المخزن هو الشيء الوحيد الذي يبقى بين تشغيلين، وهو يُقرأ من نصّ
 * كتبه إصدار أقدم أو مستخدم بيد. والفشل هنا لا يُرى في الترجمة: مراقب يختفي صامتًا، أو ترتيب
 * حقول يُقلب، أو موضع يعود إلى الوسط. فكل قاعدة في `decode` تُثبَّت بسطر يقيسها.
 */
class MonitorPrefsTest {

    private fun spec(
        id: String = "m1",
        fields: Set<HudField> = setOf(HudField.Frames, HudField.Cpu),
        form: HudForm = HudForm.Pane,
        arrangement: HudArrangement = HudArrangement.Stack,
        colorHex: String = "#FF5252",
        textSizeSp: Float = 18f,
        backgroundAlpha: Float = 0.7f,
        widthScale: Float = 1.4f,
        showGraph: Boolean = true,
        graphSpan: Int = 40,
        snapEdges: Boolean = false,
        posX: Int? = 120,
        posY: Int? = 640,
        intervalMs: Int = 2000,
        filter: HudFilter = HudFilter.Chart
    ) = MonitorSpec(
        id = id,
        fields = fields,
        form = form,
        arrangement = arrangement,
        colorHex = colorHex,
        textSizeSp = textSizeSp,
        backgroundAlpha = backgroundAlpha,
        widthScale = widthScale,
        showGraph = showGraph,
        graphSpan = graphSpan,
        snapEdges = snapEdges,
        posX = posX,
        posY = posY,
        intervalMs = intervalMs,
        filter = filter
    )

    @Test
    fun `absent key means one monitor, not a broken one`() {
        assertTrue(MonitorPrefs.decode(null).isEmpty())
        assertTrue(MonitorPrefs.decode("").isEmpty())
        assertTrue(MonitorPrefs.decode("   ").isEmpty())
    }

    @Test
    fun `unreadable text gives an empty list`() {
        assertTrue(MonitorPrefs.decode("not json at all").isEmpty())
        assertTrue(MonitorPrefs.decode("[1,2,3]").isEmpty())
    }

    @Test
    fun `everything written comes back unchanged`() {
        val original = spec()
        val decoded = MonitorPrefs.decode(MonitorPrefs.encode(listOf(original))).single()

        assertEquals(original.id, decoded.id)
        assertEquals(original.fields, decoded.fields)
        assertEquals(original.form, decoded.form)
        assertEquals(original.arrangement, decoded.arrangement)
        assertEquals(original.colorHex, decoded.colorHex)
        assertEquals(original.textSizeSp, decoded.textSizeSp, 0.0001f)
        assertEquals(original.backgroundAlpha, decoded.backgroundAlpha, 0.0001f)
        assertEquals(original.widthScale, decoded.widthScale, 0.0001f)
        assertEquals(original.showGraph, decoded.showGraph)
        assertEquals(original.graphSpan, decoded.graphSpan)
        assertEquals(original.snapEdges, decoded.snapEdges)
        assertEquals(original.posX, decoded.posX)
        assertEquals(original.posY, decoded.posY)
        assertEquals(original.intervalMs, decoded.intervalMs)
        assertEquals(original.filter, decoded.filter)
    }

    @Test
    fun `a row written before the filter existed keeps both and shows everything`() {
        val decoded = MonitorPrefs.decode("""[{"id":"m1","graph":true}]""").single()
        assertEquals(FpsOverlayPrefs.DEFAULT_INTERVAL_MS, decoded.intervalMs)
        assertEquals(HudFilter.All, decoded.filter)
    }

    @Test
    fun `an interval outside the offered list falls back, an unknown filter too`() {
        val raw = """[{"id":"m1","interval_ms":137,"filter":"Vaporwave"}]"""
        val decoded = MonitorPrefs.decode(raw).single()
        assertEquals(FpsOverlayPrefs.DEFAULT_INTERVAL_MS, decoded.intervalMs)
        assertEquals(HudFilter.All, decoded.filter)
    }

    @Test
    fun `an unknown field name is dropped, not guessed`() {
        val raw = """[{"id":"m1","fields":["Frames","Teleport","Cpu"]}]"""
        val decoded = MonitorPrefs.decode(raw).single()
        assertEquals(listOf(HudField.Frames, HudField.Cpu), decoded.orderedFields)
    }

    @Test
    fun `a row without an id is skipped while its neighbour survives`() {
        val raw = """[{"fields":["Cpu"]},{"id":"m2","fields":["Heat"]}]"""
        val decoded = MonitorPrefs.decode(raw)
        assertEquals(1, decoded.size)
        assertEquals("m2", decoded.single().id)
        assertEquals(listOf(HudField.Heat), decoded.single().orderedFields)
    }

    @Test
    fun `a blank id is not an id`() {
        assertTrue(MonitorPrefs.decode("""[{"id":"   "}]""").isEmpty())
    }

    @Test
    fun `a graph span outside the offered list falls back to twenty`() {
        val raw = """[{"id":"m1","graph_span":17},{"id":"m2","graph_span":40}]"""
        val decoded = MonitorPrefs.decode(raw)
        assertEquals(20, decoded[0].graphSpan)
        assertEquals(40, decoded[1].graphSpan)
    }

    @Test
    fun `never more than the two extras`() {
        val raw = (1..5).joinToString(prefix = "[", postfix = "]") { """{"id":"m$it"}""" }
        assertEquals(MonitorPrefs.MAX_EXTRA, MonitorPrefs.decode(raw).size)
    }

    @Test
    fun `unknown enum names and absurd numbers fall back instead of throwing`() {
        val raw = """[{"id":"m1","form":"Bubble","arrangement":"Diagonal","text_size_sp":99,"background_alpha":-4,"width_scale":0.01}]"""
        val decoded = MonitorPrefs.decode(raw).single()
        assertEquals(HudForm.Strip, decoded.form)
        assertEquals(HudArrangement.Line, decoded.arrangement)
        assertEquals(24f, decoded.textSizeSp, 0.0001f)
        assertEquals(0f, decoded.backgroundAlpha, 0.0001f)
        assertEquals(0.6f, decoded.widthScale, 0.0001f)
    }

    @Test
    fun `half a position is not a position`() {
        val raw = """[{"id":"m1","pos_x":60}]"""
        val decoded = MonitorPrefs.decode(raw).single()
        assertEquals(60, decoded.posX)
        assertNull(decoded.posY)
    }

    @Test
    fun `writing more than the cap keeps only what can be read back`() {
        val five = (1..5).map { spec(id = "m$it") }
        val encoded = JSONArray(MonitorPrefs.encode(five))
        assertEquals(MonitorPrefs.MAX_EXTRA, encoded.length())
        assertEquals(MonitorPrefs.MAX_EXTRA, MonitorPrefs.decode(encoded.toString()).size)
    }

    @Test
    fun `a missing fields key means the defaults, not an empty panel`() {
        val decoded = MonitorPrefs.decode("""[{"id":"m1"}]""").single()
        assertEquals(FpsOverlayPrefs.DEFAULT_FIELDS, decoded.fields)
    }

    @Test
    fun `an explicitly empty fields list stays empty`() {
        val raw = """[{"id":"m1","fields":[]}]"""
        assertTrue(MonitorPrefs.decode(raw).single().fields.isEmpty())
    }

    @Test
    fun `a colour that is not a colour is replaced`() {
        val raw = JSONObject().apply { put("id", "m1"); put("color_hex", "red") }
        assertEquals("#00E676", MonitorPrefs.decode("[$raw]").single().colorHex)
    }

    @Test
    fun `ordered fields follow the type order whatever the storage order`() {
        val decoded = MonitorPrefs.decode("""[{"id":"m1","fields":["Heat","Frames","Power"]}]""").single()
        assertEquals(listOf(HudField.Frames, HudField.Heat, HudField.Power), decoded.orderedFields)
    }
}
