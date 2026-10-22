/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
package nd.max.ui.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nd.max.R
import nd.max.core.gamespace.CrosshairGeometry
import nd.max.core.gamespace.CrosshairState
import nd.max.core.gamespace.CrosshairStyle
import nd.max.core.gamespace.crosshairGeometry
import nd.max.service.GamePanelService
import nd.max.ui.component.LobbyPalette
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius
import nd.max.ui.util.CrosshairPrefs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * بطاقة التصويب المخصّص في مركز تحكّم Game Space.
 *
 * **كل تغيير يُحفظ لحظته** (مثل مقبض اللوحة)، و`GamePanelService` يلتقطه بمستمع التفضيلات
 * فيظهر فوق اللعبة بلا زرّ حفظ. والمعاينة هنا تستعمل **هندسة التراكب نفسها**
 * ([crosshairGeometry])، فلا تَعِد بشكلٍ وتُرسم فوق اللعبة بغيره.
 */
@Composable
internal fun CrosshairSettingsCard() {
    val context = LocalContext.current
    var state by remember { mutableStateOf(CrosshairPrefs.load(context)) }

    val update: (CrosshairState) -> Unit = { next ->
        val clean = next.sanitized()
        state = clean
        CrosshairPrefs.save(context, clean)
        // قد تكون الخدمة متوقفة (لا لعبة مُفعَّلة) والتصويب هو السبب الوحيد لتشغيلها.
        GamePanelService.reconcile(context)
    }

    LobbySectionCard(stringResource(R.string.crosshair_title)) {
        LobbySwitchRow(
            title = stringResource(R.string.crosshair_switch),
            checked = state.enabled,
            onChecked = { update(state.copy(enabled = it)) }
        )
        Text(
            text = stringResource(R.string.crosshair_switch_desc),
            color = LobbyPalette.Muted,
            fontSize = 12.sp
        )

        CrosshairPreview(state = state, modifier = Modifier.fillMaxWidth())

        LobbyOptionRow(
            title = stringResource(R.string.crosshair_style),
            options = CrosshairStyle.entries.map { it.key to styleLabel(it) },
            selected = state.style.key,
            onSelect = { update(state.copy(style = CrosshairStyle.fromKey(it))) }
        )

        CrosshairColorRow(
            selected = state.color,
            onSelect = { update(state.copy(color = it)) }
        )

        CrosshairSliderRow(
            label = stringResource(R.string.crosshair_size),
            valueText = "${state.sizeDp} dp",
            value = state.sizeDp.toFloat(),
            range = CrosshairState.MIN_SIZE_DP.toFloat()..CrosshairState.MAX_SIZE_DP.toFloat(),
            steps = CrosshairState.MAX_SIZE_DP - CrosshairState.MIN_SIZE_DP - 1,
            onValue = { update(state.copy(sizeDp = it.roundToInt())) }
        )

        CrosshairSliderRow(
            label = stringResource(R.string.crosshair_thickness),
            valueText = "${state.thicknessDp} dp",
            value = state.thicknessDp.toFloat(),
            range = CrosshairState.MIN_THICKNESS_DP.toFloat()..CrosshairState.MAX_THICKNESS_DP.toFloat(),
            steps = CrosshairState.MAX_THICKNESS_DP - CrosshairState.MIN_THICKNESS_DP - 1,
            onValue = { update(state.copy(thicknessDp = it.roundToInt())) }
        )

        val maxGap = max(0, state.sizeDp / 2 - 1)
        CrosshairSliderRow(
            label = stringResource(R.string.crosshair_gap),
            valueText = "${state.gapDp} dp",
            value = state.gapDp.toFloat(),
            range = 0f..maxGap.toFloat(),
            steps = (maxGap - 1).coerceAtLeast(0),
            onValue = { update(state.copy(gapDp = it.roundToInt())) }
        )

        CrosshairSliderRow(
            label = stringResource(R.string.crosshair_opacity),
            valueText = "${(state.opacity * 100).roundToInt()}%",
            value = state.opacity,
            range = CrosshairState.MIN_OPACITY..CrosshairState.MAX_OPACITY,
            steps = 13,
            onValue = { update(state.copy(opacity = it)) }
        )

        LobbySwitchRow(
            title = stringResource(R.string.crosshair_outline),
            checked = state.outline,
            onChecked = { update(state.copy(outline = it)) }
        )
        Text(
            text = stringResource(R.string.crosshair_outline_desc),
            color = LobbyPalette.Muted,
            fontSize = 12.sp
        )

        Text(
            text = stringResource(R.string.crosshair_position),
            color = LobbyPalette.Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        // الموضع فيزيائيّ: الموجب يمين/أسفل في كل اللغات. فالنافذة تُثبَّت LTR كي لا
        // ينعكس السحب في RTL فيصير «يمين» يسارًا بصريًّا.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            CrosshairSliderRow(
                label = stringResource(R.string.crosshair_horizontal),
                valueText = offsetText(state.offsetX, right = true),
                value = state.offsetX,
                range = -CrosshairState.MAX_OFFSET..CrosshairState.MAX_OFFSET,
                steps = 15,
                onValue = { update(state.copy(offsetX = it)) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.crosshair_left), color = LobbyPalette.Muted, fontSize = 12.sp)
                Text(stringResource(R.string.crosshair_right), color = LobbyPalette.Muted, fontSize = 12.sp)
            }
            CrosshairSliderRow(
                label = stringResource(R.string.crosshair_vertical),
                valueText = offsetText(state.offsetY, right = false),
                value = state.offsetY,
                range = -CrosshairState.MAX_OFFSET..CrosshairState.MAX_OFFSET,
                steps = 15,
                onValue = { update(state.copy(offsetY = it)) }
            )
        }

        OutlinedButton(
            onClick = {
                CrosshairPrefs.reset(context)
                state = CrosshairPrefs.load(context)
                GamePanelService.reconcile(context)
            },
            border = BorderStroke(MaxSize.hairlineBorder, LobbyPalette.Hairline),
            modifier = Modifier.heightIn(min = MaxSize.minTouchTarget)
        ) {
            Text(text = stringResource(R.string.crosshair_reset), color = LobbyPalette.Ink)
        }

        Text(
            text = stringResource(R.string.crosshair_note),
            color = LobbyPalette.Muted,
            fontSize = 12.sp
        )
    }
}

/** معاينة حيّة بنفس هندسة التراكب — الخلفية داكنة كي تُرى الألوان الفاتحة والداكنة. */
@Composable
private fun CrosshairPreview(state: CrosshairState, modifier: Modifier = Modifier) {
    val density = LocalDensity.current.density
    Canvas(
        modifier
            .height(140.dp)
            .clip(RoundedCornerShape(maxRadius(MaxRadius.group)))
            .background(LobbyPalette.Black)
    ) {
        val grid = Color.White.copy(alpha = 0.06f)
        for (i in 1..3) {
            val x = size.width * i / 4f
            val y = size.height * i / 4f
            drawLine(grid, Offset(x, 0f), Offset(x, size.height), 1f)
            drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f)
        }
        val geometry = crosshairGeometry(state, size.width, size.height, density)
        val ink = Color(state.color).copy(alpha = state.opacity)
        if (state.outline) {
            drawCrosshair(
                geometry = geometry,
                color = Color.Black.copy(alpha = 0.55f * state.opacity),
                extra = 2.5f * density
            )
        }
        drawCrosshair(geometry = geometry, color = ink, extra = 0f)
    }
}

/** الرسم نفسه في المعاينة والتراكب: خطوط بحدود مستديرة، ودوائر مُعبَّأة أو مجرَّدة. */
private fun DrawScope.drawCrosshair(
    geometry: CrosshairGeometry,
    color: Color,
    extra: Float
) {
    geometry.lines.forEach {
        drawLine(
            color = color,
            start = Offset(it.startX, it.startY),
            end = Offset(it.endX, it.endY),
            strokeWidth = geometry.strokeWidth + extra,
            cap = StrokeCap.Round
        )
    }
    geometry.circles.forEach {
        val center = Offset(it.centerX, it.centerY)
        if (it.filled) {
            drawCircle(color = color, radius = it.radius + extra / 2f, center = center)
        } else {
            drawCircle(
                color = color,
                radius = it.radius,
                center = center,
                style = Stroke(width = geometry.strokeWidth + extra)
            )
        }
    }
}

@Composable
private fun CrosshairColorRow(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
        Text(
            text = stringResource(R.string.crosshair_color),
            color = LobbyPalette.Ink,
            fontSize = 16.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
        ) {
            CrosshairState.COLORS.forEachIndexed { index, argb ->
                val isSelected = selected == argb
                val label = stringResource(colorLabel(index))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) LobbyPalette.Ink else LobbyPalette.Hairline,
                            shape = CircleShape
                        )
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelect(argb) }
                        )
                        .semantics { contentDescription = label }
                )
            }
        }
    }
}

@Composable
private fun CrosshairSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValue: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = LobbyPalette.Ink,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            Text(text = valueText, color = LobbyPalette.Muted, fontSize = 13.sp)
        }
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = LobbyPalette.Ink,
                activeTrackColor = LobbyPalette.Red,
                inactiveTrackColor = LobbyPalette.Hairline,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun offsetText(value: Float, right: Boolean): String {
    val percent = (value * 100).roundToInt()
    return when {
        percent == 0 -> stringResource(R.string.crosshair_center)
        percent > 0 -> {
            val direction = stringResource(if (right) R.string.crosshair_right else R.string.crosshair_down)
            "$direction $percent%"
        }
        else -> {
            val direction = stringResource(if (right) R.string.crosshair_left else R.string.crosshair_up)
            "$direction ${-percent}%"
        }
    }
}

@Composable
private fun styleLabel(style: CrosshairStyle): String = stringResource(
    when (style) {
        CrosshairStyle.CROSS -> R.string.crosshair_style_cross
        CrosshairStyle.DOT -> R.string.crosshair_style_dot
        CrosshairStyle.CIRCLE -> R.string.crosshair_style_circle
        CrosshairStyle.TEE -> R.string.crosshair_style_tee
    }
)

private fun colorLabel(index: Int): Int = when (index) {
    0 -> R.string.crosshair_color_white
    1 -> R.string.crosshair_color_green
    2 -> R.string.crosshair_color_cyan
    3 -> R.string.crosshair_color_red
    4 -> R.string.crosshair_color_amber
    else -> R.string.crosshair_color_magenta
}
