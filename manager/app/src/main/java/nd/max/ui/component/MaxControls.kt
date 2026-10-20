/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package nd.max.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius

/** Native gestures and semantics are retained; only the thumb responds to drag. */
@Composable
fun MaxSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SliderColors = SliderDefaults.colors(),
    /**
     * `false` for a slider that drives a physical left/right value: the thumb must move the way the
     * finger (and any drag beside it) moves in every locale. Same rule as the frequency axis in
     * `CyberCurveEqualizer`. A magnitude slider keeps the current direction.
     */
    mirrorsInRtl: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val accent = LocalScreenAccent.current ?: scheme.primary
    val haptics = rememberMaxHaptics()
    val dragged by interactionSource.collectIsDraggedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (enabled && (dragged || pressed)) 1.12f else 1f, MaxMotion.controlSpring, label = "sliderThumb")
    val active by animateColorAsState(if (enabled) accent else scheme.onSurface.copy(alpha = .38f), label = "sliderAccent")
    val studioColors = SliderDefaults.colors(
        thumbColor = active,
        activeTrackColor = active,
        inactiveTrackColor = scheme.surfaceContainerHighest,
        activeTickColor = scheme.surface,
        inactiveTickColor = scheme.outline,
        disabledThumbColor = scheme.onSurface.copy(alpha = .38f),
        disabledActiveTrackColor = scheme.onSurface.copy(alpha = .20f),
        disabledInactiveTrackColor = scheme.onSurface.copy(alpha = .10f)
    )
    // A physical-axis control does not mirror in RTL (same rule as the frequency axis in
    // `CyberCurveEqualizer`): its value is a real left/right offset, so the thumb must follow the
    // finger — and any drag beside it — in every locale. Magnitude sliders keep the current
    // direction, so this only overrides when asked.
    val sliderDirection = if (mirrorsInRtl) LocalLayoutDirection.current else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides sliderDirection) {
        Slider(
            value = value,
            onValueChange = { next ->
                // «نقرة» لكل خطوة منفصلة (مواضع محدودة: ملفّات، مستويات، أطوار). والشريط المتّصل أو الكثيف
                // (> ٢٤ خطوة) يبقى صامتًا: ٦٠ نقرة في السحبة الواحدة ضجيجٌ لا معلومة.
                if (steps in 1..24 && next != value) haptics.step()
                onValueChange(next)
            },
            modifier = modifier.heightIn(min = 48.dp),
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            interactionSource = interactionSource,
            colors = studioColors,
            thumb = {
                Surface(
                    modifier = Modifier.size(width = 28.dp, height = 36.dp).scale(scale),
                    shape = RoundedCornerShape(maxRadius(MaxRadius.chip)),
                    color = scheme.surface,
                    border = BorderStroke(2.dp, active),
                    shadowElevation = if (dragged && enabled) 4.dp else 1.dp
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        repeat(2) {
                            // مقبض بعرض 2dp: نصف قطر 1dp لم يكن على أي سلّم، والقصّ الكامل (هيرلاين)
                            // هو ما يجعله يُقرأ مقبضًا مستديرًا لا مستطيلًا. صفر تغيير في المقاس.
                            Box(Modifier.width(MaxSpace.hairline).height(12.dp).clip(RoundedCornerShape(MaxSpace.hairline)).background(active))
                        }
                    }
                }
            },
            track = { state ->
                SliderDefaults.Track(sliderState = state, enabled = enabled, colors = studioColors, modifier = Modifier.height(10.dp))
            }
        )
    }
}

@Composable
fun MaxSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable (() -> Unit))? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SwitchColors = SwitchDefaults.colors()
) {
    val scheme = MaterialTheme.colorScheme
    val accent = LocalScreenAccent.current ?: scheme.primary
    val haptics = rememberMaxHaptics()
    Switch(
        checked = checked,
        // `null` = مفتاح تزييني داخل صفّ يملك اللمسة (`MaxSwitchRow`)، وهو من ينبض. فلا نبضتان.
        onCheckedChange = onCheckedChange?.let { change ->
            { next: Boolean ->
                haptics.toggle(next)
                change(next)
            }
        },
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        thumbContent = {
            if (thumbContent != null) {
                thumbContent()
            } else if (checked) {
                Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
            } else {
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(if (enabled) scheme.surface else scheme.onSurface.copy(alpha = .38f)))
            }
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = scheme.surface,
            checkedTrackColor = accent,
            checkedBorderColor = accent,
            checkedIconColor = accent,
            uncheckedThumbColor = scheme.outline,
            uncheckedTrackColor = scheme.surfaceContainerHighest,
            uncheckedBorderColor = scheme.outlineVariant,
            uncheckedIconColor = scheme.surface,
            disabledCheckedThumbColor = scheme.surface,
            disabledCheckedTrackColor = scheme.onSurface.copy(alpha = .12f),
            disabledUncheckedThumbColor = scheme.onSurface.copy(alpha = .38f),
            disabledUncheckedTrackColor = scheme.onSurface.copy(alpha = .08f)
        )
    )
}
