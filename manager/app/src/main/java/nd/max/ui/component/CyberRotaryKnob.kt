/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nd.max.ui.design.MaxCardSpec
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import kotlin.math.atan2
import nd.max.ui.design.maxRadius

/**
 * قرص التحكم الدائري النيوني (Cyber Rotary HUD Dial).
 *
 * قرص قوسي دائري سيبراني يتيح سحب وضبط المعامل بنعومة:
 * - قوس محيطي بـ 270 درجة بتدرج لوني ناعم.
 * - قراءة رقمية دقيقة في المركز مع أيقونة المؤثر.
 */
@Composable
fun CyberRotaryKnob(
    value: Int,
    range: IntRange,
    title: String,
    unit: String,
    icon: ImageVector,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    /** يُعلن بداية السحب ونهايته، فالشاشة الحاوية توقف تمريرها طوال السحب. */
    onInteractionChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // لون التمييز من مخطّط Material 3 النشط في الإعدادات، لا أحمر ثابت
    val (accent, accentDeep) = cyberPalette()
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

    val fraction = (value - range.first).toFloat() / (range.last - range.first).coerceAtLeast(1)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(maxRadius(MaxCardSpec.radius)))
            .border(
                MaxSize.hairlineBorder,
                if (enabled && value > 0) accent.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                RoundedCornerShape(maxRadius(MaxCardSpec.radius)),
            ),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(MaxSpace.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .pointerInput(enabled, range) {
                        if (!enabled) return@pointerInput
                        detectDragGestures(
                            onDragStart = { onInteractionChange(true) },
                            onDragEnd = { onInteractionChange(false) },
                            onDragCancel = { onInteractionChange(false) },
                        ) { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchVec = change.position - center
                            // زاوية اللمس
                            var angle = Math.toDegrees(atan2(touchVec.y.toDouble(), touchVec.x.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            // تحويل الزاوية بالنسبة لمجال القوس (135 إلى 405 = 270 درجة)
                            var mapped = (angle - 135f)
                            if (mapped < 0) mapped += 360f
                            if (mapped <= 270f) {
                                val newFraction = (mapped / 270f).coerceIn(0f, 1f)
                                val newVal = range.first + (newFraction * (range.last - range.first)).toInt()
                                onValueChange(newVal.coerceIn(range.first, range.last))
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                // رسم مسار القوس الدائري وقيمته
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 8f
                    val arcRadius = (size.minDimension / 2f) - strokeW - 4f
                    val arcSize = Size(arcRadius * 2, arcRadius * 2)
                    val topLeft = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f)

                    // 1. مسار القوس الخلفي (270 درجة)
                    drawArc(
                        color = trackColor,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round),
                    )

                    // 2. مسار القوس الممتلئ المتوهج
                    if (enabled && fraction > 0f) {
                        val sweep = fraction * 270f
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(accentDeep, accent),
                                center = Offset(size.width / 2f, size.height / 2f),
                            ),
                            startAngle = 135f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW + 2f, cap = StrokeCap.Round),
                        )
                    }
                }

                // المركز الدائري المخصص
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(72.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(MaxSize.iconGlyph),
                            tint = if (enabled && value > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (value > 0) "$value$unit" else "OFF",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (enabled && value > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(MaxSpace.xs))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
