/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * أدوات الرسم للّوحة النيونية ([GamePanelTurboNeon]).
 *
 * ### فضاء التصميم
 *
 * اللوحة مرسومة في **فضاء تصميم ثابت** بإحداثيات الصورة المرجعية نفسها (١ وحدة = ١ بكسل صورة)، ثم تُكبَّر
 * أو تُصغَّر دفعة واحدة لتناسب الشاشة. وداخل هذا الفضاء تكون `Density = 1` و`fontScale = 1`، فتصبح `X.dp`
 * و`X.sp` و`DrawScope` كلّها بوحدة واحدة: الإحداثي الذي يُكتب هنا هو الإحداثي في الصورة.
 *
 * ### والتوهّج
 *
 * التمويه (`blur`) غير متاح قبل API 31 وminSdk هنا ٢٩، فالتوهّج **طبقات خطوط متراكبة** تتناقص شفافيتها
 * مع اتّساعها، وهالات إشعاعية خلف الأيقونات: يعمل على كل الإصدارات بلا `RenderEffect`.
 */
internal object NeonColors {
    val Ink = Color(0xFFF4F6FA)
    val InkDim = Color(0xFFB4BED3)
    val Void = Color(0xFF02030A)
    val Navy = Color(0xFF060B1C)
    val NavyHi = Color(0xFF0C1736)
    val Red = Color(0xFFFF2A35)
    val RedHot = Color(0xFFFF0A1C)
    val RedSoft = Color(0xFFFF8A94)
    val RedDeep = Color(0xFF4A0008)
    val Blue = Color(0xFF1E7BFF)
    val BlueHi = Color(0xFF38B6FF)
    val BlueLine = Color(0xFF2A5FD0)
    val BlueDim = Color(0xFF16336E)
    val Purple = Color(0xFF7B2FE8)
    val Green = Color(0xFF22C55E)
    val Amber = Color(0xFFF5B800)
    val Cyan = Color(0xFF2FD4FF)
}

/** يضع عنصرًا بإحداثي ومقاس مطلقين في فضاء التصميم. */
internal fun Modifier.at(x: Float, y: Float, w: Float, h: Float): Modifier =
    offset { IntOffset(x.roundToInt(), y.roundToInt()) }.size(w.dp, h.dp)

/** مضلّع من أزواج x,y متتالية. */
internal fun poly(vararg p: Float): Path = Path().apply {
    moveTo(p[0], p[1])
    var i = 2
    while (i < p.size - 1) {
        lineTo(p[i], p[i + 1])
        i += 2
    }
    close()
}

/** مستطيل بزوايا مقطوعة بقدر [c] في الزوايا الأربع. */
internal fun chamfer(x: Float, y: Float, w: Float, h: Float, c: Float): Path = poly(
    x + c, y, x + w - c, y, x + w, y + c, x + w, y + h - c,
    x + w - c, y + h, x + c, y + h, x, y + h - c, x, y + c
)

/** متوازي أضلاع مائل بمقدار [skew] (للمقاطع المضيئة في شريط الحالة). */
internal fun slanted(x: Float, y: Float, w: Float, h: Float, skew: Float): Path =
    poly(x + skew, y, x + w + skew, y, x + w, y + h, x, y + h)

/** خطّ نيون: طبقات هالة متسعة خافتة تحت الخطّ الحادّ. */
internal fun DrawScope.glowStroke(
    path: Path,
    brush: Brush,
    width: Float,
    glow: Float = 1f,
    spread: Float = 18f,
    alpha: Float = 1f
) {
    val steps = 4
    for (i in steps downTo 1) {
        val k = i / steps.toFloat()
        drawPath(
            path = path,
            brush = brush,
            alpha = 0.075f * glow * (1.7f - k) * alpha,
            style = Stroke(width = width + spread * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
    drawPath(
        path = path,
        brush = brush,
        alpha = alpha,
        style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

internal fun DrawScope.glowStroke(
    path: Path,
    color: Color,
    width: Float,
    glow: Float = 1f,
    spread: Float = 18f,
    alpha: Float = 1f
) = glowStroke(path, Brush.linearGradient(listOf(color, color)), width, glow, spread, alpha)

/** هالة إشعاعية ناعمة خلف عنصر مضيء. */
internal fun DrawScope.halo(center: Offset, radius: Float, color: Color, alpha: Float = 0.5f) {
    drawCircle(
        brush = Brush.radialGradient(
            0f to color.copy(alpha = alpha),
            0.55f to color.copy(alpha = alpha * 0.35f),
            1f to Color.Transparent,
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/**
 * أيقونة مادّية بتدرّج لوني وهالة مضيئة: الأيقونة تُرسم في طبقة منفصلة ثم يُستبدل لونها بالتدرّج
 * (`SrcIn`) فتبقى حوافّها وشفافيتها كما هي.
 *
 * (x, y) = مركز الأيقونة، [size] = ضلعها. [dim] يخفّض الإضاءة كلّها (للعنصر غير المتاح).
 */
@Composable
internal fun GlowIcon(
    vector: ImageVector,
    cx: Float,
    cy: Float,
    size: Float,
    top: Color,
    bottom: Color,
    glow: Color,
    haloAlpha: Float = 0.45f,
    dim: Float = 1f,
    framed: Boolean = false
) {
    val painter = rememberVectorPainter(vector)
    val box = size * 2.2f
    Box(
        Modifier
            .at(cx - box / 2f, cy - box / 2f, box, box)
            .drawBehind {
                halo(center, size * 1.0f, glow, haloAlpha * dim)
                val iconRect = Rect(center.x - size / 2f, center.y - size / 2f, center.x + size / 2f, center.y + size / 2f)
                val tint = Brush.verticalGradient(
                    colors = listOf(top.copy(alpha = dim), bottom.copy(alpha = dim)),
                    startY = iconRect.top,
                    endY = iconRect.bottom
                )
                if (framed) {
                    val fw = size * 1.2f
                    val fh = size * 1.0f
                    val frame = Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                center.x - fw / 2f, center.y - fh / 2f, center.x + fw / 2f, center.y + fh / 2f,
                                CornerRadius(size * 0.2f)
                            )
                        )
                    }
                    glowStroke(frame, tint, size * 0.09f, glow = 0.8f, spread = size * 0.2f)
                }
                val inner = if (framed) size * 0.62f else size
                val innerRect = Rect(center.x - inner / 2f, center.y - inner / 2f, center.x + inner / 2f, center.y + inner / 2f)
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(innerRect, Paint())
                    translate(innerRect.left, innerRect.top) {
                        with(painter) { draw(Size(inner, inner)) }
                    }
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(top.copy(alpha = dim), bottom.copy(alpha = dim)),
                            startY = innerRect.top,
                            endY = innerRect.bottom
                        ),
                        topLeft = innerRect.topLeft,
                        size = innerRect.size,
                        blendMode = BlendMode.SrcIn
                    )
                    canvas.restore()
                }
            }
    )
}

/** رسم أيقونة مخصّصة داخل صندوق 100×100 مُكبَّر إلى [size] ومركزه ([cx], [cy]). */
@Composable
internal fun GlowShape(
    cx: Float,
    cy: Float,
    size: Float,
    glow: Color,
    haloAlpha: Float = 0.45f,
    dim: Float = 1f,
    draw: DrawScope.() -> Unit
) {
    val box = size * 2.2f
    Box(
        Modifier
            .at(cx - box / 2f, cy - box / 2f, box, box)
            .drawBehind {
                halo(center, size * 1.0f, glow, haloAlpha * dim)
                translate(center.x - size / 2f, center.y - size / 2f) {
                    scale(size / 100f, pivot = Offset.Zero) { draw() }
                }
            }
    )
}

// ───────────────────────── أيقونات مخصّصة (صندوق ١٠٠×١٠٠) ─────────────────────────

internal fun DrawScope.iconRocket(body: Brush, hole: Color, flame: Color) {
    rotate(45f, pivot = Offset(50f, 50f)) {
        val shell = Path().apply {
            moveTo(50f, 4f)
            cubicTo(66f, 18f, 76f, 44f, 66f, 72f)
            lineTo(34f, 72f)
            cubicTo(24f, 44f, 34f, 18f, 50f, 4f)
            close()
        }
        drawPath(poly(42f, 74f, 50f, 99f, 58f, 74f), flame)
        drawPath(poly(36f, 52f, 15f, 76f, 18f, 91f, 38f, 74f), body)
        drawPath(poly(64f, 52f, 85f, 76f, 82f, 91f, 62f, 74f), body)
        drawPath(shell, body)
        drawCircle(hole, radius = 8.5f, center = Offset(50f, 36f))
        drawCircle(hole, radius = 5.5f, center = Offset(50f, 56f))
    }
}

internal fun DrawScope.iconBolt(brush: Brush) {
    drawPath(poly(58f, 2f, 22f, 54f, 46f, 54f, 36f, 98f, 80f, 40f, 56f, 40f, 72f, 2f), brush)
}

internal fun DrawScope.iconCube(brush: Brush) {
    val hex = poly(50f, 5f, 91f, 28f, 91f, 72f, 50f, 95f, 9f, 72f, 9f, 28f)
    drawPath(hex, brush, alpha = 0.22f)
    val st = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(hex, brush, style = st)
    val y = Path().apply {
        moveTo(9f, 28f); lineTo(50f, 50f); lineTo(91f, 28f)
        moveTo(50f, 50f); lineTo(50f, 95f)
    }
    drawPath(y, brush, style = st)
}

internal fun DrawScope.iconBag(brush: Brush, cut: Color) {
    val handle = Path().apply {
        moveTo(34f, 34f); lineTo(34f, 22f)
        cubicTo(34f, 8f, 66f, 8f, 66f, 22f); lineTo(66f, 34f)
    }
    drawPath(handle, brush, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    val body = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(12f, 32f, 88f, 94f, CornerRadius(12f)))
    }
    drawPath(body, brush)
    val y = Path().apply {
        moveTo(36f, 48f); lineTo(50f, 66f); lineTo(64f, 48f)
        moveTo(50f, 66f); lineTo(50f, 82f)
    }
    drawPath(y, cut, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

internal fun DrawScope.iconThermometer(color: Color) {
    val st = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val tube = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(38f, 4f, 62f, 66f, CornerRadius(12f)))
    }
    drawPath(tube, color, style = st)
    drawCircle(color, radius = 19f, center = Offset(50f, 77f), style = st)
    drawCircle(color, radius = 10f, center = Offset(50f, 77f))
    drawLine(color, Offset(50f, 26f), Offset(50f, 70f), strokeWidth = 8f, cap = StrokeCap.Round)
}

internal fun DrawScope.iconBattery(rim: Color, fill: Brush, plus: Color) {
    val st = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val body = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(6f, 26f, 84f, 74f, CornerRadius(10f)))
    }
    drawPath(body, rim, style = st)
    drawRoundRect(rim, topLeft = Offset(87f, 41f), size = Size(8f, 18f), cornerRadius = CornerRadius(3f))
    drawRoundRect(fill, topLeft = Offset(14f, 34f), size = Size(62f, 32f), cornerRadius = CornerRadius(5f))
    drawLine(plus, Offset(45f, 40f), Offset(45f, 60f), strokeWidth = 6f, cap = StrokeCap.Round)
    drawLine(plus, Offset(35f, 50f), Offset(55f, 50f), strokeWidth = 6f, cap = StrokeCap.Round)
}

/** نبض قلب (ECG) في صندوق 44×20. */
internal fun DrawScope.iconPulse(color: Color) {
    val p = Path().apply {
        moveTo(0f, 10f); lineTo(11f, 10f); lineTo(15f, 2f); lineTo(21f, 18f)
        lineTo(26f, 5f); lineTo(30f, 10f); lineTo(44f, 10f)
    }
    glowStroke(p, color, 3f, glow = 1.2f, spread = 9f)
}
