/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import android.media.AudioDeviceInfo
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nd.max.R
import nd.max.ui.design.MaxCardSpec
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius

/** مقاسات مسرح الزر الرئيسي: منطقة الرسم ونصف قطرها، وقطر الزر ونصف قطره. */
private val VortexStageSize = 168.dp
private val VortexStageRadius = 84.dp
private val VortexOrbSize = 100.dp
private val VortexOrbRadius = 50.dp

/**
 * زرّ التشغيل الرئيسي (Master)، وهو بطاقة التحكّم بالمعالجة الصوتية كلها.
 *
 * - كل الألوان من مخطّط Material 3 النشط في الإعدادات، فلا يبقى أيّ أحمر ثابت.
 * - انتقالٌ واحد يقود الحالة كلها: حلقة الشحن تمتلئ، والزر ينتقل من السطح الرمادي
 *   إلى تدرّج الـ primary، ويضيء الجزء العلوي من البطاقة تدريجيًّا.
 * - الضغط يعطي نبضة زنبركية مع اهتزازٍ خفيف، والهالة والموجة لا تعملان إلا والجهاز مُشغَّل.
 */
@Composable
fun CyberAudioVortex(
    enabled: Boolean,
    activeSessionsCount: Int,
    outputDeviceName: String,
    deviceType: Int?,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val (accent, accentDeep, accentCool) = cyberPalette()
    val live = enabled && activeSessionsCount > 0

    // تقدّم واحد من 0 (متوقّف) إلى 1 (يعمل) يقود كل الألوان والحلقة معًا
    val power by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "vortex_power",
    )

    // نبضة الضغط: زنبرك قصير يعيد الزر إلى حجمه الطبيعي بعد الإفلات
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow),
        label = "vortex_press",
    )
    val haptic = LocalHapticFeedback.current

    val deviceIcon = when (deviceType) {
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> Icons.Rounded.Headphones
        else -> Icons.Rounded.Speaker
    }
    val statusColor = when {
        live -> accent
        enabled -> accentCool
        else -> scheme.onSurfaceVariant
    }
    val orbCore = lerp(scheme.surfaceContainerHighest, accent, power)
    val orbEdge = lerp(scheme.surfaceContainerHigh, accentDeep, power)
    val orbContent = lerp(scheme.onSurfaceVariant, scheme.onPrimary, power)
    val rim = lerp(scheme.outlineVariant.copy(alpha = 0.3f), accent.copy(alpha = 0.6f), power)
    val cardShape = RoundedCornerShape(maxRadius(MaxCardSpec.radius))

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(MaxSize.hairlineBorder, rim, cardShape),
        color = scheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.16f * power), Color.Transparent)))
                .padding(vertical = MaxSpace.lg, horizontal = MaxSpace.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // شريط الحالة: جهاز الإخراج، وعدّاد الجلسات الحيّة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHigh)
                        .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
                ) {
                    Icon(
                        imageVector = deviceIcon,
                        contentDescription = null,
                        modifier = Modifier.size(MaxSize.iconGlyphSmall),
                        tint = if (enabled) accentCool else scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(MaxSpace.xs))
                    Text(
                        text = outputDeviceName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (enabled) statusColor.copy(alpha = 0.16f) else scheme.surfaceContainerHigh)
                        .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
                ) {
                    Box(
                        modifier = Modifier
                            .size(MaxSpace.xs)
                            .clip(CircleShape)
                            .background(statusColor),
                    )
                    Spacer(Modifier.width(MaxSpace.xs))
                    Text(
                        text = when {
                            !enabled -> stringResource(R.string.audio_vortex_chip_standby)
                            live -> stringResource(R.string.audio_vortex_chip_live, activeSessionsCount)
                            else -> stringResource(R.string.audio_vortex_chip_ready)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                    )
                }
            }

            Spacer(Modifier.height(MaxSpace.md))

            // المسرح الدائري: الهالة والموجة عند التشغيل، ثم حلقة الشحن، ثم الزر نفسه
            Box(
                modifier = Modifier.size(VortexStageSize),
                contentAlignment = Alignment.Center,
            ) {
                if (power > 0.01f) {
                    VortexLivePulse(
                        accent = accent,
                        strength = power,
                        orbRadius = VortexOrbRadius,
                        stageRadius = VortexStageRadius,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 3.dp.toPx()
                    val ringRadius = size.minDimension / 2f - stroke * 2f
                    val ringTopLeft = Offset(center.x - ringRadius, center.y - ringRadius)
                    val ringSize = Size(ringRadius * 2f, ringRadius * 2f)
                    // مسار الحلقة الساكن
                    drawCircle(
                        color = scheme.outlineVariant.copy(alpha = 0.3f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = stroke),
                    )
                    // قوس الشحن: يمتلئ عند التشغيل ويتراجع عند الإيقاف
                    if (power > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(accentDeep, accent, accentCool, accentDeep),
                                center = center,
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * power,
                            useCenter = false,
                            topLeft = ringTopLeft,
                            size = ringSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                }

                // الزر الرئيسي
                Box(
                    modifier = Modifier
                        .size(VortexOrbSize)
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                        .shadow(
                            elevation = (16f * power).dp,
                            shape = CircleShape,
                            ambientColor = accent,
                            spotColor = accent,
                        )
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(orbCore, orbEdge)))
                        .clickable(interactionSource = interaction, indication = null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggle()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            tint = orbContent,
                        )
                        Spacer(Modifier.height(MaxSpace.xs))
                        Text(
                            text = stringResource(if (enabled) R.string.audio_vortex_orb_on else R.string.audio_vortex_orb_off),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = orbContent,
                        )
                    }
                }
            }

            Spacer(Modifier.height(MaxSpace.md))

            // سطر الحالة السفلي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(MaxSize.iconGlyphSmall),
                    tint = if (enabled) accent else scheme.outlineVariant,
                )
                Spacer(Modifier.width(MaxSpace.xs))
                Text(
                    text = stringResource(if (enabled) R.string.audio_status_universal_active else R.string.audio_status_universal_disabled),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** هالة تتنفّس خلف الزر، وموجة تتّسع من حافته وتتلاشى. تُرسم فقط والجهاز مُشغَّل. */
@Composable
private fun VortexLivePulse(
    accent: Color,
    strength: Float,
    orbRadius: Dp,
    stageRadius: Dp,
    modifier: Modifier = Modifier,
) {
    // نبضٌ وغلافٌ حول زرّ التشغيل: يُقرآن فقط والجهاز يعمل، ويتوقّفان كلّيًّا حين
    // يطلب النظام تقليل الحركة. الحالة الساكنة تحمل المعنى نفسه (هالة + حلقة).
    val animationsEnabled = rememberAnimationsEnabled()
    val breathe: Float
    val wave: Float
    if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "vortex_live")
        breathe = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "vortex_breathe",
        ).value
        wave = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "vortex_wave",
        ).value
    } else {
        breathe = 0.5f
        wave = 0f
    }

    Canvas(modifier = modifier) {
        val orbPx = orbRadius.toPx()
        val stagePx = stageRadius.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = (0.14f + 0.12f * breathe) * strength), Color.Transparent),
                center = center,
                radius = stagePx,
            ),
            radius = stagePx,
            center = center,
        )
        drawCircle(
            color = accent.copy(alpha = 0.5f * (1f - wave) * strength),
            radius = orbPx + (stagePx - orbPx) * wave,
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}
