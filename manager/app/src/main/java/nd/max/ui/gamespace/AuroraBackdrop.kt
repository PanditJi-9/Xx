/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.gamespace

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/*
 * خلفية MONOLITH: أفق واحد، وهج لعبة واحد، وغبار نجمي ثابت.
 *
 * لا تدرّجات نيون متحرّكة ولا جزيئات تُعاد رسمها كل إطار. الحركة الوحيدة
 * تنفّس بطيء للوهج (٢٤ ثانية) وتتوقف تمامًا حين يطلب النظام تقليل الحركة.
 * الرسم كله في مرحلة الرسم، فلا يُعاد تركيب الشجرة مع كل إطار.
 */
@Composable
fun AuroraBackdrop(
    accent: Color,
    animate: Boolean,
    modifier: Modifier = Modifier
) {
    val drift: State<Float> = if (animate) {
        rememberInfiniteTransition(label = "aurora").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing)),
            label = "auroraDrift"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val phase by drift
    Canvas(modifier = modifier) {
        drawBase(phase)
        drawGlow(accent, phase)
        drawConstellation(phase)
    }
}

private fun DrawScope.drawBase(phase: Float) {
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Monolith.Obsidian, Monolith.Basalt, Monolith.Obsidian)
        )
    )
    // أفق منخفض: خطّ شعر واحد يفصل «السماء» عن «الأرض» بلا صندوق.
    val horizon = size.height * 0.66f
    drawLine(
        color = Color.White.copy(alpha = 0.05f),
        start = Offset(0f, horizon),
        end = Offset(size.width, horizon),
        strokeWidth = 1f
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.32f)),
            startY = horizon,
            endY = size.height
        ),
        topLeft = Offset(0f, horizon),
        size = androidx.compose.ui.geometry.Size(size.width, size.height - horizon)
    )
}

private fun DrawScope.drawGlow(accent: Color, phase: Float) {
    val breath = 0.82f + 0.18f * phase
    val center = Offset(size.width * 0.86f, size.height * 0.12f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.16f * breath), Color.Transparent),
            center = center,
            radius = size.minDimension * 0.92f
        ),
        radius = size.minDimension * 0.92f,
        center = center
    )
    val emberCenter = Offset(size.width * 0.08f, size.height * 0.88f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Monolith.Ember.copy(alpha = 0.07f * breath), Color.Transparent),
            center = emberCenter,
            radius = size.minDimension * 0.7f
        ),
        radius = size.minDimension * 0.7f,
        center = emberCenter
    )
}

/**
 * غبار ثابت محسوب من مواضع نسبية لا من عشوائية وقت التشغيل: الشكل نفسه في كل إطار
 * وفي كل جهاز، وصفر كلفة على الذاكرة.
 */
private fun DrawScope.drawConstellation(phase: Float) {
    val nodes = 14
    for (index in 0 until nodes) {
        val fx = ((index * 37) % 97) / 97f
        val fy = ((index * 61) % 89) / 89f
        val x = size.width * (0.06f + fx * 0.88f)
        val y = size.height * (0.05f + fy * 0.58f)
        val twinkle = 0.04f + 0.03f * ((index + phase * 4f) % 3f) / 3f
        drawCircle(
            color = Color.White.copy(alpha = twinkle),
            radius = if (index % 4 == 0) 1.6.dp.toPx() else 1.dp.toPx(),
            center = Offset(x, y)
        )
    }
}
