/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned. Architecture and modifications (C) 2026 Nader Magdy.
 *
 * الجزيرة المنقسمة (Dual Capsule / Split Island):
 * هندسة انقسام الجزيرة بفيزيائية سائلة إلى كبسولتين مستقلتين (مستوحى من ColorOS 17 Aqua Dynamics و HyperOS)
 */
package nd.max.island.engine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * راسم الكبسولة المنفصلة الثانوية (Secondary Floating Droplet Bubble).
 *
 * تنفصل بجانب الكبسولة الأساسية عند وجود نشاطين، وتستجيب للنقر المستقل للتبديل أو التوسيع.
 */
@Composable
fun SecondaryFloatingBubble(
    secondary: IslandMode,
    palette: IslandVisualPalette,
    height: Dp,
    onClick: () -> Unit,
) {
    val cornerRadius = height / 2
    val shape = RoundedCornerShape(cornerRadius)

    val bubbleWidth by animateDpAsState(
        targetValue = when (secondary) {
            is IslandMode.Timer -> 48.dp
            is IslandMode.Media -> 52.dp
            is IslandMode.Charging -> 50.dp
            is IslandMode.Live -> 44.dp
            else -> height
        },
        animationSpec = spring(
            dampingRatio = palette.springDamping,
            stiffness = palette.springStiffness
        ),
        label = "bubble_w"
    )

    Box(
        modifier = Modifier
            .width(bubbleWidth)
            .height(height)
            .clip(shape)
            .background(palette.backgroundBrush)
            .drawIslandSurface(palette, cornerRadius)
            .border(palette.borderWidth, palette.borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        when (secondary) {
            is IslandMode.Timer -> {
                val sec = secondary.remainingSec
                val mm = (sec / 60).coerceAtLeast(0)
                val ss = (sec % 60).coerceAtLeast(0)
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                    CircularProgressIndicator(
                        progress = { 0.75f },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFFFF9F0A),
                        trackColor = Color(0x33FF9F0A),
                        strokeWidth = 2.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Label(
                        text = if (mm > 0) "${mm}m" else "${ss}s",
                        size = 10,
                        weight = FontWeight.Bold,
                        color = Color(0xFFFF9F0A)
                    )
                }
            }

            is IslandMode.Media -> {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Art(secondary, 20.dp, 6.dp)
                    Spacer(Modifier.width(4.dp))
                    DynamicWaveform(
                        playing = secondary.playing,
                        color = secondary.accentColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.height(12.dp)
                    )
                }
            }

            is IslandMode.Charging -> {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.BatteryChargingFull,
                        contentDescription = null,
                        tint = IslandStyleFactory.HyperNeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Label(
                        text = "${secondary.percent}%",
                        size = 11,
                        weight = FontWeight.Bold,
                        color = IslandStyleFactory.HyperNeonGreen
                    )
                }
            }

            is IslandMode.Live -> {
                when (secondary.kind) {
                    LiveKind.Navigation -> {
                        Icon(
                            Icons.Rounded.Navigation,
                            contentDescription = null,
                            tint = Color(0xFF00A2FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    LiveKind.Download -> {
                        DownloadRing(secondary.progress, 20.dp)
                    }
                    LiveKind.Tracking -> {
                        Icon(
                            Icons.Rounded.DirectionsRun,
                            contentDescription = null,
                            tint = Color(0xFF00A2FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            else -> {
                Icon(
                    Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = palette.glowColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * عارض الموجات الحركي التفاعلي (Dynamic 4-Bar Equalizer):
 * يتذبذب بـ 4 أعمدة إيقاعية ديناميكية استجابة لتشغيل الموسيقى.
 */
@Composable
fun DynamicWaveform(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Waveform(
        playing = playing,
        color = color,
        modifier = modifier
    )
}
