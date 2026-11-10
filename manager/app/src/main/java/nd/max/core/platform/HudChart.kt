/*
 * Copyright (C) 2026-2027 Zexshia
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */

package nd.max.core.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.max
import kotlin.math.min

/**
 * رسم الجلسة صورةً: منحنى الإطارات على خلفية داكنة، ومعه الأرقام التي تُقرأ بلا تطبيق.
 *
 * **ولماذا صورة لا CSV فقط:** الملفّ الجدولي يُقرأ بجدول بيانات؛ والصورة تُرسل في محادثة
 * أو تُلصق في تقرير. والاثنان من الجلسة نفسها، لا من حسابين.
 *
 * والرسم **لا يخترع قيمة**: النقاط الغائبة تُكسر الخطّ لا تُوصل بقيمة صفر، والمحور يبدأ
 * من أدنى قراءة مقيسة لا من الصفر — لأن الصفر ليس حدًّا فيزيائيًّا للإطارات.
 */
object HudChartRenderer {

    private const val BG = 0xFF12181C.toInt()
    private const val GRID = 0xFF2A3339.toInt()
    private const val INK = 0xFFE8EEF2.toInt()
    private const val MUTED = 0xFF9AA6AE.toInt()
    private const val ACCENT = 0xFF4FD1C5.toInt()

    /** أقصى عرض للصورة: 1080 يكفي لقراءة على الهاتف وأيضًا في مستند. */
    private const val WIDTH = 1080
    private const val HEIGHT = 540

    fun render(
        points: List<SessionPoint>,
        title: String,
        subtitle: String? = null
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BG)

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK
            textSize = 30f
            isFakeBoldText = true
        }
        canvas.drawText(title.take(48), 36f, 54f, text)

        if (!subtitle.isNullOrBlank()) {
            val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = MUTED
                textSize = 22f
            }
            canvas.drawText(subtitle.take(80), 36f, 88f, sub)
        }

        val left = 72f
        val top = 130f
        val right = WIDTH - 36f
        val bottom = HEIGHT - 64f
        val values = points.mapNotNull { it.frames?.takeIf { v -> v.isFinite() && v > 0f } }
        if (values.isEmpty()) {
            val empty = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = MUTED
                textSize = 26f
            }
            canvas.drawText("—", left, (top + bottom) / 2f, empty)
            return bitmap
        }

        val low = values.min()
        val high = values.max()
        val span = max(high - low, 1f)
        val minY = low - span * 0.12f
        val maxY = high + span * 0.12f
        val scale = (maxY - minY).coerceAtLeast(1f)

        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GRID
            strokeWidth = 2f
        }
        repeat(4) { step ->
            val y = top + (bottom - top) * step / 3f
            canvas.drawLine(left, y, right, y, grid)
        }

        val axis = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MUTED
            textSize = 20f
        }
        canvas.drawText("%.0f".format(high), 12f, top + 8f, axis)
        canvas.drawText("%.0f".format(low), 12f, bottom, axis)

        val firstAt = points.first().atMs
        val lastAt = points.last().atMs
        val timeSpan = (lastAt - firstAt).coerceAtLeast(1L).toFloat()

        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ACCENT
            strokeWidth = 4f
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        val path = Path()
        var drawing = false
        for (point in points) {
            val frames = point.frames
            if (frames == null || !frames.isFinite() || frames <= 0f) {
                drawing = false
                continue
            }
            val x = left + (point.atMs - firstAt) / timeSpan * (right - left)
            val y = bottom - (frames - minY) / scale * (bottom - top)
            if (drawing) path.lineTo(x, y) else path.moveTo(x, y)
            drawing = true
        }
        canvas.drawPath(path, line)

        val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MUTED
            textSize = 20f
        }
        val average = values.average()
        canvas.drawText(
            "avg %.1f  ·  low %.0f  ·  high %.0f  ·  %d samples".format(
                average, low, high, values.size
            ),
            36f,
            HEIGHT - 26f,
            footer
        )
        return bitmap
    }

    /** أبعاد الصورة الثابتة — تُقرأ في الاختبار وفي المشاركة. */
    fun dimensions(): Pair<Int, Int> = WIDTH to HEIGHT

    /**
     * إسقاط قيمة على المحور الرأسي، مفصولة عن الرسم لتُختبر وحدها:
     * النطاق يُوسَّع بهامش ١٢٪ حتى لا يلتصق الخطّ بالحدّ.
     */
    fun verticalFraction(value: Float, low: Float, high: Float): Float {
        val span = max(high - low, 1f)
        val minY = low - span * 0.12f
        val maxY = high + span * 0.12f
        return ((value - minY) / (maxY - minY)).coerceIn(0f, 1f)
    }

    /** حصّة الأفق: موضع النقطة من الزمن الكلّي. */
    fun horizontalFraction(atMs: Long, firstAtMs: Long, lastAtMs: Long): Float {
        val span = (lastAtMs - firstAtMs).coerceAtLeast(1L)
        return ((atMs - firstAtMs).toFloat() / span).coerceIn(0f, 1f)
    }
}

/** حدّ أعلى/أدنى للرسم في الشاشة — نفس القاعدة، بلا bitmap. */
fun sessionFrameBounds(points: List<SessionPoint>): Pair<Float, Float>? {
    val values = points.mapNotNull { it.frames?.takeIf { v -> v.isFinite() && v > 0f } }
    if (values.isEmpty()) return null
    return min(values.min(), values.max()) to values.max()
}
