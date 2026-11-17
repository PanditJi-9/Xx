/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp

/**
 * سماكة الخط **نسبة من ضلع الرمز** لا dp ثابت: ٦٪ من الضلع في كل الأحجام (أثقل من ٥٫٥٪ السابقة
 * فيبقى الرمز مقروءًا عند ٤٠dp)، والوصلات والنهايات دائرية. كل جزء يضرب هذه السماكة بـ`weight`.
 */
private const val IllustrationStrokeFraction = 0.06f

/**
 * Static, theme-aware domain identity. Text beside it remains the accessible label.
 *
 * العائلة «زجاج ثنائي اللون»: الجسم تعبئة شفّافة بتدرّج رأسي مع حدّ، والقلب (Accent) تدرّج صلب
 * من لون أفتح إلى لون التمييز، والتفاصيل المثقوبة (Cutout) بلون سطح البطاقة، والثانوية (Soft)
 * بنصف شفافية. ألفا [color] يُحترم في كل الأحبار (صفوف معطّلة تبهت كلها معًا).
 */
@Composable
fun MaxIllustration(
    kind: MaxIllustrationKind,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = MaxVisualSpec.mark,
) {
    val parts = remember(kind) { maxIllustrationParts(kind) }
    val scheme = MaterialTheme.colorScheme
    val neutral = scheme.onSurfaceVariant
    val knockout = scheme.surfaceContainerLow
    Canvas(modifier = modifier.size(size)) {
        val side = this.size.minDimension
        if (!side.isFinite() || side <= 0f) return@Canvas
        val origin = Offset((this.size.width - side) / 2f, (this.size.height - side) / 2f)
        val baseStroke = side * IllustrationStrokeFraction
        val top = origin.y
        val bottom = origin.y + side

        fun scaled(value: Color, factor: Float): Color = value.copy(alpha = value.alpha * factor)

        val accentBrush = Brush.verticalGradient(
            colors = listOf(lerp(color, Color.White, 0.24f).copy(alpha = color.alpha), color),
            startY = top,
            endY = bottom,
        )
        val washBrush = Brush.verticalGradient(
            colors = listOf(scaled(color, 0.34f), scaled(color, 0.12f)),
            startY = top,
            endY = bottom,
        )
        val washStroke = SolidColor(scaled(color, MaxAlpha.toneContainerStrong))
        val softBrush = SolidColor(scaled(color, 0.5f))
        val outlineBrush = SolidColor(
            scaled(lerp(color, neutral, 0.45f).copy(alpha = color.alpha), 0.92f),
        )
        val cutoutBrush = SolidColor(knockout.copy(alpha = 0.96f))

        fun brush(ink: MaxIllustrationInk, filled: Boolean): Brush = when (ink) {
            MaxIllustrationInk.Accent -> accentBrush
            MaxIllustrationInk.Outline -> outlineBrush
            MaxIllustrationInk.Wash -> if (filled) washBrush else washStroke
            MaxIllustrationInk.Soft -> softBrush
            MaxIllustrationInk.Cutout -> cutoutBrush
        }

        fun strokeOf(weight: Float) = Stroke(
            width = baseStroke * weight,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )

        for (part in parts) {
            when (part) {
                is MaxIllustrationPart.Line -> {
                    val path = Path()
                    part.points.forEachIndexed { index, point ->
                        val x = origin.x + point.x * side
                        val y = origin.y + point.y * side
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    if (part.closed) path.close()
                    drawPath(
                        path = path,
                        brush = brush(part.ink, part.filled),
                        style = if (part.filled) Fill else strokeOf(part.weight),
                    )
                }
                is MaxIllustrationPart.Box -> {
                    drawRoundRect(
                        brush = brush(part.ink, part.filled),
                        topLeft = origin + Offset(part.x * side, part.y * side),
                        size = Size(part.width * side, part.height * side),
                        cornerRadius = CornerRadius(part.radius * side),
                        style = if (part.filled) Fill else strokeOf(1f),
                    )
                }
                is MaxIllustrationPart.Ring -> {
                    drawCircle(
                        brush = brush(part.ink, part.filled),
                        radius = part.radius * side,
                        center = origin + Offset(part.x * side, part.y * side),
                        style = if (part.filled) Fill else strokeOf(1f),
                    )
                }
            }
        }
    }
}

/** A screen-panel diagram: aspect comes only from declared dimensions; no value means no diagram. */
@Composable
fun MaxDisplaySchematic(
    width: Float?,
    height: Float?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = MaxVisualSpec.hero,
) {
    val proportion = remember(width, height) { maxDisplayProportion(width, height) } ?: return
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier.size(size)) {
        val side = this.size.minDimension * MaxVisualSpec.panelFit
        if (!side.isFinite() || side <= 0f) return@Canvas
        val panel = Size(side * proportion.width, side * proportion.height)
        val origin = Offset((this.size.width - panel.width) / 2f, (this.size.height - panel.height) / 2f)
        drawRoundRect(color.copy(alpha = MaxAlpha.toneContainer), origin, panel, CornerRadius(MaxSpace.xs.toPx()))
        drawRoundRect(color, origin, panel, CornerRadius(MaxSpace.xs.toPx()), style = Stroke(MaxSize.emphasisBorder.toPx()))
        val start = origin + Offset(panel.width * 0.22f, panel.height * 0.78f)
        val end = origin + Offset(panel.width * 0.78f, panel.height * 0.22f)
        drawLine(outline, start, end, MaxSize.hairlineBorder.toPx(), cap = StrokeCap.Round)
    }
}
