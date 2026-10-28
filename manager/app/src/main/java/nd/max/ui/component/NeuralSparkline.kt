/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * موجة قياس مشتركة — **نُقلت لا نُسخت**.
 *
 * كانت `FrequencySparkline` خاصةً بـ`LegendaryHomeDashboard`، وشاشة CPU تحتاج الموجة نفسها
 * لكل نواة. ونسخةٌ ثانية من دالة رسم تعني **موجتين تفترقان عند أول تعديل**: من يغيّر التعبئة
 * أو سمك الخط في إحداهما يظنّ أنه غيّر «شكل الموجة» في التطبيق. فنُقلت إلى كومبوننت، والملفّان
 * يرسمان من هذا الملفّ وحده — والقيمة البصرية لم تتغيّر حرفيًّا (نقل لا إعادة تصميم).
 *
 * **والوحدات مقصودة:** `samples` **نسب** من ٠ إلى ١ لا ميغاهرتز. القرار «ما مدى هذه النواة»
 * يبقى في الشاشة التي تعرف سقفها المعلن، والرسم يعرف الشكل فقط — فلا تصنع الموجة مقياسًا.
 */
package nd.max.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import nd.max.ui.design.MaxDuration

/** Vertical inset of the plot, shared by the line and the beacon so the halo sits on the last point. */
private const val PLOT_INSET_PX = 5f

/**
 * Where the newest sample sits inside the canvas. One function for both layers: the line draws its
 * dot here and the beacon draws its halo here, so the two can never drift apart.
 */
private fun DrawScope.newestPoint(samples: List<Float>): Offset {
    val top = PLOT_INSET_PX
    val bottom = size.height - PLOT_INSET_PX
    val usable = (bottom - top).coerceAtLeast(1f)
    return Offset(size.width, top + usable * (1f - samples.last().coerceIn(0f, 1f)))
}

/**
 * Measurement wave shared by the home cards and the CPU core screen.
 *
 * **The beacon is a ping per fresh reading, not a loop.** It used to be an infinite transition read
 * while composing: on a 120 Hz screen that recomposed the sparkline and rebuilt its paths about
 * 120 times a second for as long as the screen was open (two sparklines on the home screen), so the
 * CPU and GPU never went idle between the two-second readings. Now the line is drawn only when the
 * samples change, and a short halo (`MaxDuration.deliberate`) plays once per new reading on its own
 * layer, read in the draw phase only. The beacon still carries the reading: a ping means a new
 * value arrived, and a feed that stops stops pinging. A series whose values are identical from one
 * reading to the next does not ping.
 */
@Composable
fun NeuralSparkline(
    samples: List<Float>,
    accent: Color,
    modifier: Modifier = Modifier,
    /**
     * خطّ الأرضية المعلنة داخل المدى، أو `null` إن لم تُعلن أرضية.
     *
     * ويُرسم على مستواه الحقيقي فيصير المدى مقروءًا من الرسم نفسه لا مِن نصّ مجاور — ويُشتقّ
     * من الرقم الذي أعلنته النواة، لا من أدنى عيّنة.
     */
    floorFraction: Float? = null,
) {
    val p = neuralPalette()
    val grid = p.muted.copy(alpha = .07f)
    val floorLine = p.muted.copy(alpha = .22f)
    val animationsEnabled = rememberAnimationsEnabled()

    // 1f = at rest. Restarted by every new set of samples; nothing runs between readings.
    val ping = remember { Animatable(1f) }
    LaunchedEffect(samples, animationsEnabled) {
        if (!animationsEnabled || samples.size < 2) {
            ping.snapTo(1f)
            return@LaunchedEffect
        }
        ping.snapTo(0f)
        ping.animateTo(1f, tween(MaxDuration.deliberate, easing = FastOutSlowInEasing))
    }

    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            if (samples.size < 2) return@Canvas
            val w = size.width
            val h = size.height
            val top = PLOT_INSET_PX
            val bottom = h - PLOT_INSET_PX
            val usable = (bottom - top).coerceAtLeast(1f)
            val step = w / (samples.size - 1).toFloat()
            val points = samples.mapIndexed { index, value ->
                Offset(step * index, top + usable * (1f - value.coerceIn(0f, 1f)))
            }

            drawLine(grid, Offset(0f, top), Offset(w, top), 1f)
            drawLine(grid, Offset(0f, h / 2f), Offset(w, h / 2f), 1f)
            drawLine(grid, Offset(0f, bottom), Offset(w, bottom), 1f)

            floorFraction?.let { fraction ->
                val y = top + usable * (1f - fraction)
                drawLine(floorLine, Offset(0f, y), Offset(w, y), 1.dp.toPx())
            }

            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.lastIndex) {
                    val a = points[i]
                    val b = points[i + 1]
                    quadraticTo(a.x, a.y, (a.x + b.x) / 2f, (a.y + b.y) / 2f)
                }
                lineTo(points.last().x, points.last().y)
            }
            val fill = Path().apply {
                addPath(path)
                lineTo(points.last().x, bottom)
                lineTo(points.first().x, bottom)
                close()
            }
            drawPath(
                fill,
                brush = Brush.verticalGradient(
                    listOf(accent.copy(alpha = .20f), accent.copy(alpha = .015f)),
                    startY = top,
                    endY = bottom
                )
            )
            drawPath(path, color = accent.copy(alpha = .14f), style = Stroke(width = 7.dp.toPx()))
            drawPath(
                path,
                color = accent,
                style = Stroke(
                    width = 2.2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            drawCircle(accent, radius = 3.dp.toPx(), center = newestPoint(samples))
        }

        // The halo lives on its own layer: while it plays, only this small layer is redrawn, not the
        // line or the card around it. `ping.value` is read here, in the draw phase, so no frame of
        // the ping recomposes anything.
        if (animationsEnabled && samples.size >= 2) {
            Canvas(Modifier.matchParentSize().graphicsLayer { }) {
                val pulse = ping.value
                if (pulse < 1f) {
                    drawCircle(
                        accent.copy(alpha = (1f - pulse) * 0.45f),
                        radius = 3.dp.toPx() + pulse * 5.dp.toPx(),
                        center = newestPoint(samples)
                    )
                }
            }
        }
    }
}
