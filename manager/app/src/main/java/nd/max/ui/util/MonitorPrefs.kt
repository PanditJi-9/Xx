/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import android.content.Context
import nd.max.core.platform.HudArrangement
import nd.max.core.platform.HudField
import nd.max.core.platform.HudForm
import org.json.JSONArray
import org.json.JSONObject

/**
 * إعدادات مراقب عائم إضافي — مستقلّ تمامًا عن الأوّل.
 *
 * **ولماذا لم يُدمج الأوّل هنا:** المراقب الأوّل له مفاتيحه القديمة (`hud_*`) منذ ما قبل
 * تعدّد المراقبات، و`PersistRestorer` والاختبارات تقرؤها. فتحويلها إلى هذا الملف يهجر
 * إعدادًا محفوظًا بلا سبب. المراقب الأوّل يبقى في [FpsOverlayPrefs]، وهذا الملف يحفظ
 * **الإضافات فقط**، والغياب يعني «مراقب واحد» كما كان.
 */
data class MonitorSpec(
    val id: String,
    val fields: Set<HudField> = FpsOverlayPrefs.DEFAULT_FIELDS,
    val form: HudForm = HudForm.Strip,
    val arrangement: HudArrangement = HudArrangement.Line,
    val colorHex: String = "#00E676",
    val textSizeSp: Float = 14f,
    val backgroundAlpha: Float = 0.5f,
    val widthScale: Float = 1f,
    val showGraph: Boolean = false,
    val graphSpan: Int = 20,
    val snapEdges: Boolean = true,
    val posX: Int? = null,
    val posY: Int? = null
) {
    /** ترتيب العرض من ترتيب النوع — لا يُرتَّب حقل هنا على خلاف بقية الأسطح. */
    val orderedFields: List<HudField> get() = HudField.entries.filter { it in fields }
}

/**
 * حفظ المراقبات الإضافية في ملف تفضيلات التراكب نفسه.
 *
 * **والحدّ ثلاثة مراقبات كاملة (الأوّل + اثنان)، ويُرفض الزائد ولا يُقتطع صامتًا:**
 * اقتطاع قائمة إلى اثنين بينما يظنّ المستخدم أنه أضاف ثلاثة هو كذب على الواجهة. فالإضافة
 * تُعطَّل مع سبب ظاهر، والحفظ لا يقبل أكثر من [MAX_EXTRA].
 */
object MonitorPrefs {

    /** السقف الكامل: المراقب الأوّل + [MAX_EXTRA]. */
    const val MAX_MONITORS = 3

    /** ما يمكن إضافته فوق المراقب الأوّل. */
    const val MAX_EXTRA = MAX_MONITORS - 1

    /** ملف التفضيلات مشترك مع [FpsOverlayPrefs] — مفتاح واحد إضافي فيه. */
    private const val PREFS_NAME = "maxmanager_fps_overlay"
    private const val KEY = "hud_monitors"

    private const val K_ID = "id"
    private const val K_FIELDS = "fields"
    private const val K_FORM = "form"
    private const val K_ARRANGEMENT = "arrangement"
    private const val K_COLOR = "color_hex"
    private const val K_TEXT_SIZE = "text_size_sp"
    private const val K_ALPHA = "background_alpha"
    private const val K_WIDTH = "width_scale"
    private const val K_GRAPH = "graph"
    private const val K_GRAPH_SPAN = "graph_span"
    private const val K_SNAP = "snap_edges"
    private const val K_POS_X = "pos_x"
    private const val K_POS_Y = "pos_y"

    /** معرّف جديد — الوقت بالستّ عشريّ يكفي لمراقب يُضاف بيد إنسان. */
    fun newId(): String = "m" + System.currentTimeMillis().toString(36)

    /**
     * القائمة المحفوظة، أو قائمة فارغة.
     *
     * **والقراءة متسامحة عن قصد:** صفّ تالف لا يُسقط الصفوف السليمة معه، والاسم المجهول
     * لحقل يُهمَل (حقل أُعيد تسميته لا يُخمَّن). وملفّ قديم بلا هذا المفتاح يعطي فارغة —
     * أي «مراقب واحد» — بلا هجرة ولا فقدان.
     */
    fun load(context: Context): List<MonitorSpec> = decode(prefs(context).getString(KEY, null))

    /**
     * القائمة من نصّ JSON خام — دالّة صافية بلا `Context`، فتُقاس بالاختبار مباشرة.
     *
     * **والصفّ التالف لا يُسقط جاره:** كل صفّ يُفكّ وحده، وفشله يعني تخطّيه لا إلغاء القائمة.
     * والزيادة على [MAX_EXTRA] تُقتطع عند القراءة لأن ملفًّا كُتب بيد أو من نسخة أقدم قد يحمل أكثر.
     */
    fun decode(raw: String?): List<MonitorSpec> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val result = ArrayList<MonitorSpec>(minOf(array.length(), MAX_EXTRA))
        for (index in 0 until array.length()) {
            if (result.size >= MAX_EXTRA) break
            val row = runCatching { array.getJSONObject(index) }.getOrNull() ?: continue
            decodeRow(row)?.let(result::add)
        }
        return result
    }

    /** النصّ المحفوظ — صافٍ كذلك، فيُقاس الذهاب والعودة بلا جهاز. */
    fun encode(monitors: List<MonitorSpec>): String {
        val array = JSONArray()
        monitors.take(MAX_EXTRA).forEach { array.put(encodeRow(it)) }
        return array.toString()
    }

    /** يحفظ القائمة كاملةً، ويقتطع عند السقف بدل كتابة ما لا يُقرأ. */
    fun save(context: Context, monitors: List<MonitorSpec>) {
        prefs(context).edit().putString(KEY, encode(monitors)).apply()
    }

    /** يضيف مراقبًا إن كان هناك موضع؛ ويعيد القائمة الناتجة. */
    fun add(context: Context, spec: MonitorSpec): List<MonitorSpec> {
        val next = load(context).toMutableList()
        if (next.size < MAX_EXTRA) next.add(spec)
        save(context, next)
        return next
    }

    fun remove(context: Context, id: String): List<MonitorSpec> {
        val next = load(context).filterNot { it.id == id }
        save(context, next)
        return next
    }

    fun update(context: Context, spec: MonitorSpec): List<MonitorSpec> {
        val next = load(context).map { if (it.id == spec.id) spec else it }
        save(context, next)
        return next
    }

    /** موضع نافذة واحدة فقط — لا يمسّ إحداثيات غيرها. */
    fun savePosition(context: Context, id: String, x: Int, y: Int) {
        val monitors = load(context)
        if (monitors.none { it.id == id }) return
        save(
            context,
            monitors.map { if (it.id == id) it.copy(posX = x, posY = y) else it }
        )
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun encodeRow(spec: MonitorSpec): JSONObject = JSONObject().apply {
        put(K_ID, spec.id)
        put(K_FIELDS, JSONArray(spec.orderedFields.map { it.name }))
        put(K_FORM, spec.form.name)
        put(K_ARRANGEMENT, spec.arrangement.name)
        put(K_COLOR, spec.colorHex)
        put(K_TEXT_SIZE, spec.textSizeSp.toDouble())
        put(K_ALPHA, spec.backgroundAlpha.toDouble())
        put(K_WIDTH, spec.widthScale.toDouble())
        put(K_GRAPH, spec.showGraph)
        put(K_GRAPH_SPAN, spec.graphSpan)
        put(K_SNAP, spec.snapEdges)
        spec.posX?.let { put(K_POS_X, it) }
        spec.posY?.let { put(K_POS_Y, it) }
    }

    private fun decodeRow(row: JSONObject): MonitorSpec? {
        val id = runCatching { row.getString(K_ID) }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: return null
        val fields = if (row.has(K_FIELDS)) {
            val array = runCatching { row.getJSONArray(K_FIELDS) }.getOrNull()
            if (array == null) {
                FpsOverlayPrefs.DEFAULT_FIELDS
            } else {
                val names = (0 until array.length()).mapNotNull { index ->
                    runCatching { array.getString(index) }.getOrNull()
                }
                HudField.entries.filter { it.name in names }.toSet()
            }
        } else {
            FpsOverlayPrefs.DEFAULT_FIELDS
        }
        return MonitorSpec(
            id = id,
            fields = fields,
            form = enumOr(row.optString(K_FORM), HudForm.entries, HudForm.Strip),
            arrangement = enumOr(
                row.optString(K_ARRANGEMENT),
                HudArrangement.entries,
                HudArrangement.Line
            ),
            colorHex = row.optString(K_COLOR).takeIf { it.startsWith("#") } ?: "#00E676",
            textSizeSp = row.optDouble(K_TEXT_SIZE, 14.0).toFloat().coerceIn(10f, 24f),
            backgroundAlpha = row.optDouble(K_ALPHA, 0.5).toFloat().coerceIn(0f, 1f),
            widthScale = row.optDouble(K_WIDTH, 1.0).toFloat().coerceIn(0.6f, 2.5f),
            showGraph = row.optBoolean(K_GRAPH, false),
            graphSpan = row.optInt(K_GRAPH_SPAN, 20).let { span ->
                if (span in FpsOverlayPrefs.GRAPH_SPANS) span else 20
            },
            snapEdges = row.optBoolean(K_SNAP, true),
            posX = if (row.has(K_POS_X)) row.optInt(K_POS_X) else null,
            posY = if (row.has(K_POS_Y)) row.optInt(K_POS_Y) else null
        )
    }

    private fun <T : Enum<T>> enumOr(raw: String, values: List<T>, fallback: T): T =
        values.firstOrNull { it.name == raw } ?: fallback
}
