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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/** Static, theme-aware domain identity. Text beside it remains the accessible label. */
@Composable
fun MaxIllustration(
    kind: MaxIllustrationKind,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = MaxVisualSpec.mark,
) {
    val parts = remember(kind) { maxIllustrationParts(kind) }
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier.size(size)) {
        val side = this.size.minDimension
        if (!side.isFinite() || side <= 0f) return@Canvas
        val origin = Offset((this.size.width - side) / 2f, (this.size.height - side) / 2f)
        val stroke = Stroke(width = MaxSize.emphasisBorder.toPx(), cap = StrokeCap.Round)
        fun ink(value: MaxIllustrationInk): Color = when (value) {
            MaxIllustrationInk.Accent -> color
            MaxIllustrationInk.Outline -> outline.copy(alpha = MaxAlpha.supportingText)
            MaxIllustrationInk.Wash -> color.copy(alpha = MaxAlpha.toneContainer)
        }
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
                    drawPath(path, color = ink(part.ink), style = stroke)
                }
                is MaxIllustrationPart.Box -> drawRoundRect(
                    color = ink(part.ink),
                    topLeft = origin + Offset(part.x * side, part.y * side),
                    size = Size(part.width * side, part.height * side),
                    cornerRadius = CornerRadius(part.radius * side),
                    style = if (part.filled) Fill else stroke,
                )
                is MaxIllustrationPart.Ring -> drawCircle(
                    color = ink(part.ink),
                    center = origin + Offset(part.x * side, part.y * side),
                    radius = part.radius * side,
                    style = if (part.filled) Fill else stroke,
                )
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
