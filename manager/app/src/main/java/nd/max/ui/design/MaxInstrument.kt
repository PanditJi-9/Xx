/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection

/** A frequency is a position on a scale, not a capacity to fill. No time-based animation. */
@Composable
fun MaxFrequencyTrack(
    fraction: Float?,
    modifier: Modifier = Modifier,
    limit: Float? = null,
) {
    val value = maxMeasuredFraction(fraction, 1f) ?: return
    val ceiling = maxMeasuredFraction(limit, 1f)
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.outlineVariant
    val marker = MaterialTheme.colorScheme.onSurface
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(
        modifier = modifier.fillMaxWidth().height(MaxVisualSpec.trackHeight)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f) },
    ) {
        val radius = MaxSpace.xs.toPx()
        val usable = (size.width - radius * 2f).coerceAtLeast(0f)
        if (usable <= 0f || size.height <= 0f) return@Canvas
        val y = size.height / 2f
        fun x(position: Float) = radius + usable * (if (rtl) 1f - position else position)
        drawLine(muted, Offset(x(0f), y), Offset(x(1f), y), MaxSize.emphasisBorder.toPx(), StrokeCap.Round)
        for (index in 0..4) {
            val tick = x(index / 4f)
            drawLine(muted, Offset(tick, y - radius), Offset(tick, y + radius), MaxSize.hairlineBorder.toPx())
        }
        if (ceiling != null) {
            val tick = x(ceiling)
            drawLine(marker, Offset(tick, radius), Offset(tick, size.height - radius), MaxSize.emphasisBorder.toPx())
        }
        drawLine(primary, Offset(x(0f), y), Offset(x(value), y), MaxSize.activeRing.toPx(), StrokeCap.Round)
        drawCircle(primary, radius, Offset(x(value), y))
    }
}

/**
 * حصة من كلّ (وقت، طاقة) كشريط يمتلئ من الصفر — عكس [MaxFrequencyTrack].
 *
 * والفرق بين الشكلين هو نوع البيانات: التردد **موضع** على سلّم، والحصة **مقدار** من مجموع.
 * فرسمُ حصةٍ كمؤشّر على خطّ يقول «٠٫٣ من سقف» وهو ليس سقفًا، ورسمُ ترددٍ كشريط يمتلئ يوهم
 * بأن الحالة الأعلى «ممتلئة» — فلا يُخلط الشكلان.
 */
@Composable
fun MaxShareTrack(
    fraction: Float?,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val share = maxMeasuredFraction(fraction, 1f) ?: return
    val track = MaterialTheme.colorScheme.outlineVariant
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(
        modifier = modifier.fillMaxWidth().height(MaxVisualSpec.capacityHeight)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(share, 0f..1f) },
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(track.copy(alpha = MaxAlpha.toneContainerStrong), size = size, cornerRadius = radius)
        val filled = (size.width * share).coerceAtLeast(if (share > 0f) size.height else 0f)
        if (filled <= 0f) return@Canvas
        drawRoundRect(
            accent,
            topLeft = Offset(if (rtl) size.width - filled else 0f, 0f),
            size = Size(filled, size.height),
            cornerRadius = radius,
        )
    }
}

/** An actual battery level. Unknown values produce no empty battery pretending to be zero. */
@Composable
fun MaxBatteryLevel(fraction: Float?, modifier: Modifier = Modifier) {
    val level = maxMeasuredFraction(fraction, 1f) ?: return
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = modifier.size(MaxVisualSpec.instrument)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(level, 0f..1f) },
    ) {
        val side = size.minDimension
        if (side <= 0f) return@Canvas
        val origin = Offset((size.width - side * 0.52f) / 2f, (size.height - side * 0.76f) / 2f)
        val body = Size(side * 0.52f, side * 0.76f)
        val corner = CornerRadius(MaxSpace.sm.toPx())
        drawRoundRect(outline, origin, body, corner, style = Stroke(MaxSize.emphasisBorder.toPx()))
        drawRoundRect(
            outline,
            origin + Offset(side * 0.16f, -side * 0.065f),
            Size(side * 0.20f, side * 0.04f),
            CornerRadius(MaxSpace.hairline.toPx()),
        )
        val inset = MaxSpace.xs.toPx()
        val inner = Size((body.width - inset * 2f).coerceAtLeast(0f), (body.height - inset * 2f).coerceAtLeast(0f))
        if (inner.width <= 0f || inner.height <= 0f || level == 0f) return@Canvas
        drawRoundRect(
            primary.copy(alpha = MaxAlpha.toneContainerStrong),
            origin + Offset(inset, inset),
            inner,
            CornerRadius(MaxSpace.xs.toPx()),
        )
        drawRoundRect(
            primary,
            origin + Offset(inset, inset + inner.height * (1f - level)),
            Size(inner.width, inner.height * level),
            CornerRadius(MaxSpace.xs.toPx()),
        )
    }
}
