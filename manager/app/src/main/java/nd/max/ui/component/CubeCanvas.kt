/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import nd.max.core.gamespace.CubeLayoutMath

/*
 * رسم لوحة «المكعّب» العرضية: لوحان جانبيان معتمان بحافة داخلية مستقيمة ناعمة، تنفتح بينهما نافذة
 * على اللعبة. حافة كل لوح هي موضع شريط التردد الطويل لعموده (CPU يسارًا، GPU يمينًا)، فيكون الشريط
 * عمودَ اللوح الفقريّ لا خطًّا يقطع منتصف الشاشة (كانت الأشرطة المائلة العريضة تفعل ذلك فأُزيلت).
 * حدّا اللوحين يأتيان من [nd.max.core.gamespace.CubeLayout] — موضع واحد للرسم وللاختبار.
 */
/** مسار الشريط المطفأ: داكن شفّاف، فيُقرأ الشكل حتى حين لا يملؤه شيء. */
internal val CubeTrack = Color(0x8C232429)
private val ScrimDeep = Color(0xD8040406)
private val ScrimSoft = Color(0xBB060608)
private const val EDGE_FADE_DP = 16

/**
 * اللوحان المعتمان: من الحافة الخارجية حتى [leftEdgePx] يسارًا ومن [rightEdgePx] حتى الحافة يمينًا.
 * والوسط يُترك شفّافًا إلا من عتمة خفيفة عامة تُبقي النصوص المجاورة مقروءة فوق مشاهد فاتحة
 * (خريطة صفراء مثلًا). الحافة الداخلية تخبو على [EDGE_FADE_DP] فلا يظهر خطّ قاطع حادّ.
 */
internal fun DrawScope.cubePanels(leftEdgePx: Float, rightEdgePx: Float, sweep: Float) {
    val s = sweep.coerceIn(0f, 1f)
    if (s <= 0f) return
    val w = size.width
    val h = size.height
    val fade = EDGE_FADE_DP.dp.toPx()
    drawRect(Color.Black.copy(alpha = 0.18f * s))
    val left = leftEdgePx.coerceIn(0f, w)
    val right = rightEdgePx.coerceIn(left, w)
    if (left > 0f) {
        val brush = Brush.horizontalGradient(
            0f to ScrimDeep,
            ((left - fade) / left).coerceIn(0f, 1f) to ScrimSoft,
            1f to Color.Transparent,
            startX = 0f,
            endX = left,
        )
        drawRect(brush, topLeft = Offset.Zero, size = Size(left, h), alpha = s)
    }
    val rightW = w - right
    if (rightW > 0f) {
        val brush = Brush.horizontalGradient(
            0f to Color.Transparent,
            (fade / rightW).coerceIn(0f, 1f) to ScrimSoft,
            1f to ScrimDeep,
            startX = right,
            endX = w,
        )
        drawRect(brush, topLeft = Offset(right, 0f), size = Size(rightW, h), alpha = s)
    }
}

/** خطّا الفصل تحت صفّ الدوائر: يفصلان الرأس عن الجسم فيقرأ المستخدم بنية الصفحة لا كتلة واحدة. */
internal fun DrawScope.cubeRules(edgePx: Float, y: Float, leftReachPx: Float, rightReachPx: Float, sweep: Float) {
    val a = 0.14f * sweep.coerceIn(0f, 1f)
    if (a <= 0f) return
    val stroke = 1.dp.toPx()
    val color = Color.White.copy(alpha = a)
    if (leftReachPx > edgePx) drawLine(color, Offset(edgePx, y), Offset(leftReachPx, y), stroke)
    if (rightReachPx < size.width - edgePx) drawLine(color, Offset(rightReachPx, y), Offset(size.width - edgePx, y), stroke)
}

/** موجة صدمة أخفّ من موضع المقبض: حلقات متتابعة تتّسع وتخبو بهدوء. */
internal fun DrawScope.cubeWave(origin: Offset, t: Float, tint: Color) {
    if (t <= 0f || t >= 1f) return
    val c = Offset(origin.x * size.width, origin.y * size.height)
    val reach = hypot(size.width, size.height)
    for (k in 0..2) {
        val p = ((t - k * 0.11f) / (1f - k * 0.11f)).coerceIn(0f, 1f)
        if (p <= 0f || p >= 1f) continue
        val r = reach * (1f - (1f - p) * (1f - p))
        val fade = (1f - p) * (1f - p)
        drawCircle(tint.copy(alpha = fade * (0.07f - k * 0.015f)), r, c, style = Stroke((16f - k * 4f).dp.toPx() * (1f - p) + 1.dp.toPx()))
        drawCircle(tint.copy(alpha = fade * (0.40f - k * 0.10f)), r, c, style = Stroke((2f - k * 0.3f).dp.toPx()))
    }
    val flash = (1f - t / 0.35f).coerceIn(0f, 1f)
    if (flash > 0f) {
        val fr = size.height * 0.5f
        drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = 0.35f * flash), Color.Transparent), center = c, radius = fr), fr, c)
    }
}

/**
 * اللوحان المعتمان وحافتاهما تتبعان حزام التردد (زاوية «>» / «<») بدل خطّ عموديّ: العتمة خلف الأعمدة
 * فقط، وما بين ساقي الحزام وقمّته يبقى مفتوحًا على اللعبة كما في المرجع. الحافة مخفيّة تحت الحزام نفسه،
 * فلا حاجة لتخبية ناعمة. كل القيم بكسل؛ [bowPx] = إزاحة القمّة، و[topPx]/[heightPx] مدى الحزام رأسيًّا.
 */
internal fun DrawScope.cubeChevronPanels(
    cpuRailXPx: Float, gpuRailRightPx: Float, railWPx: Float, bowPx: Float,
    topPx: Float, heightPx: Float, sweep: Float
) {
    val s = sweep.coerceIn(0f, 1f)
    if (s <= 0f) return
    val w = size.width
    val h = size.height
    drawRect(Color.Black.copy(alpha = 0.18f * s))
    fun bulge(y: Float) = CubeLayoutMath.railBulge(1f - ((y - topPx) / heightPx.coerceAtLeast(1f))) * bowPx
    val steps = 40
    val ys = FloatArray(steps + 1) { topPx + heightPx * it / steps }
    val left = Path().apply {
        moveTo(0f, 0f)
        lineTo(cpuRailXPx + railWPx + bulge(ys[steps]), 0f)
        for (y in ys.reversed()) lineTo(cpuRailXPx + railWPx + bulge(y), y)
        lineTo(cpuRailXPx + railWPx + bulge(ys[0]), h)
        lineTo(0f, h)
        close()
    }
    val right = Path().apply {
        moveTo(w, 0f)
        lineTo(gpuRailRightPx - railWPx - bulge(ys[steps]), 0f)
        for (y in ys.reversed()) lineTo(gpuRailRightPx - railWPx - bulge(y), y)
        lineTo(gpuRailRightPx - railWPx - bulge(ys[0]), h)
        lineTo(w, h)
        close()
    }
    val lBrush = Brush.horizontalGradient(0f to ScrimDeep, 1f to ScrimSoft, startX = 0f, endX = w * 0.5f)
    val rBrush = Brush.horizontalGradient(0f to ScrimSoft, 1f to ScrimDeep, startX = w * 0.5f, endX = w)
    clipPath(left) { drawRect(lBrush, alpha = s) }
    clipPath(right) { drawRect(rBrush, alpha = s) }
}
