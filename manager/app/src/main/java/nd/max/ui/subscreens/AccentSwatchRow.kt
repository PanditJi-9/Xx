/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.ui.subscreens

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.materialkolor.dynamiccolor.ColorSpec
import kotlin.math.roundToInt
import nd.max.R
import nd.max.core.platform.EventLog
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxBleed
import nd.max.ui.design.maxEdgeFade
import nd.max.ui.design.maxRadius
import nd.max.ui.theme.MaxManagerDefaultKeyColor

/** مقاسات لوحة الضبط مُسمّاة هنا، لا أرقام مبعثرة في الواجهة. */
private val AccentTrackHeight = 12.dp
private val AccentThumbSize = 24.dp
private val AccentThumbBorder = 3.dp
private val AccentSwatchSize = 68.dp
private val AccentPreviewSize = 40.dp

/** طيف الألوان كاملًا (0° إلى 360°): مسار الصبغة، وخلفية شريحة «مخصّص». */
private val AccentHueSpectrum: List<Color> = (0..6).map { step -> accentPaint(step * 60f, 1f, 1f) }

/** لونٌ بصيغة HSV ليسهل ضبطه بالمنزلقات، ويُحوَّل إلى ARGB عند الحفظ. */
private data class AccentHsv(val hue: Float, val saturation: Float, val value: Float) {
    fun toArgb(): Int = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
}

private fun accentHsvOf(argb: Int): AccentHsv {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    return AccentHsv(hsv[0], hsv[1], hsv[2])
}

private fun accentPaint(hue: Float, saturation: Float, value: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))

/**
 * صفّ التمييز اللوني: الشرائح الجاهزة، ثم شريحة «مخصّص» تفتح لوحة الضبط أسفله.
 *
 * ولماذا صفٌّ مستقلّ: الحالة المفتوحة/المغلقة تخصّ هذا الصفّ وحده، فتبقى داخله ولا تُمرَّر
 * عبر قائمة الإعدادات كلها. والمنزلقات تُحدّث اللون لحظيًّا (فتتحرّك المعاينة معها) وتكتب
 * التفضيلات عند رفع الإصبع فقط، فلا يُعاد بناء السمة مع كل خطوة سحب.
 */
@Composable
internal fun AccentSwatchRow(
    palette: List<Int>,
    swatchStartBleed: Dp,
    currentKeyColor: Int,
    currentColorSpec: ColorSpec.SpecVersion,
    isDark: Boolean,
    prefs: SharedPreferences,
    onKeyColorChange: (Int) -> Unit,
) {
    val isCustomSelected = currentKeyColor != 0 && currentKeyColor !in palette
    // يبقى اللوحة مفتوحة تلقائيًّا إن كان اللون المحفوظ مخصّصًا (ليس من الشرائح).
    var customOpen by rememberSaveable { mutableStateOf(isCustomSelected) }

    Column {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .maxBleed(start = swatchStartBleed, end = MaxSpace.gutter)
                .maxEdgeFade(),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
            contentPadding = PaddingValues(horizontal = MaxSpace.gutter),
        ) {
            // "Dynamic / Wallpaper" swatch
            item {
                ColorSwatch(
                    color = Color.Unspecified,
                    isSelected = currentKeyColor == 0,
                    isDark = isDark,
                    colorSpec = currentColorSpec,
                    label = stringResource(R.string.theme_accent_auto),
                    onClick = {
                        onKeyColorChange(0)
                        EventLog.userAction(screen = "CustomTheme", field = "key_color", old = currentKeyColor.toString(), new = "0")
                        prefs.edit { putInt("key_color", 0) }
                    }
                )
            }
            items(palette) { colorInt ->
                ColorSwatch(
                    color = Color(colorInt),
                    isSelected = currentKeyColor == colorInt,
                    isDark = isDark,
                    colorSpec = currentColorSpec,
                    onClick = {
                        onKeyColorChange(colorInt)
                        EventLog.userAction(screen = "CustomTheme", field = "key_color", old = currentKeyColor.toString(), new = colorInt.toString())
                        prefs.edit { putInt("key_color", colorInt) }
                    }
                )
            }
            item {
                CustomAccentSwatch(
                    isSelected = isCustomSelected || customOpen,
                    onClick = { customOpen = !customOpen }
                )
            }
        }

        AnimatedVisibility(
            visible = customOpen,
            enter = expandVertically(tween(MaxDuration.standard)) + fadeIn(tween(MaxDuration.standard)),
            exit = shrinkVertically(tween(MaxDuration.standard)) + fadeOut(tween(MaxDuration.standard)),
        ) {
            CustomAccentPanel(
                currentKeyColor = currentKeyColor,
                onLiveChange = onKeyColorChange,
                onCommit = { color ->
                    EventLog.userAction(screen = "CustomTheme", field = "key_color", old = currentKeyColor.toString(), new = color.toString())
                    prefs.edit { putInt("key_color", color) }
                }
            )
        }
    }
}

/** شريحة «مخصّص»: طيف ألوان دائريّ يدلّ على أنها تفتح منتقي الألوان لا لونًا بعينه. */
@Composable
private fun CustomAccentSwatch(isSelected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(maxRadius(MaxRadius.group))
    val borderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(MaxDuration.standard),
        label = "accent_custom_border"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = scheme.surfaceContainer,
            modifier = Modifier
                .size(AccentSwatchSize)
                .border(width = MaxSize.swatchRing, color = scheme.primary.copy(alpha = borderAlpha), shape = shape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(AccentPreviewSize)) {
                    drawCircle(brush = Brush.sweepGradient(AccentHueSpectrum))
                }
                Icon(
                    imageVector = Icons.Outlined.Colorize,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(MaxSize.iconGlyph)
                )
            }
        }
        Text(
            text = stringResource(R.string.theme_accent_custom),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/** لوحة الضبط: صبغة وتشبّع وسطوع. تُحدّث اللون أثناء السحب وتُحفظ عند رفع الإصبع. */
@Composable
private fun CustomAccentPanel(
    currentKeyColor: Int,
    onLiveChange: (Int) -> Unit,
    onCommit: (Int) -> Unit,
) {
    var hsv by remember {
        mutableStateOf(accentHsvOf(if (currentKeyColor == 0) MaxManagerDefaultKeyColor else currentKeyColor))
    }
    // اختيار لونٍ من خارج اللوحة (شريحة أخرى أو نمط جاهز) يُعاد إلى المنزلقات، أمّا السحب نفسه فلا.
    LaunchedEffect(currentKeyColor) {
        if (currentKeyColor != 0 && currentKeyColor != hsv.toArgb()) {
            hsv = accentHsvOf(currentKeyColor)
        }
    }
    val argb = hsv.toArgb()

    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
        Text(
            text = stringResource(R.string.theme_accent_custom_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
        ) {
            Box(
                modifier = Modifier
                    .size(AccentPreviewSize)
                    .background(Color(argb), CircleShape)
                    .border(BorderStroke(MaxSize.swatchRing, MaterialTheme.colorScheme.outlineVariant), CircleShape)
            )
            Text(
                text = String.format("#%06X", argb and 0xFFFFFF),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        AccentHsvSlider(
            label = stringResource(R.string.theme_accent_hue),
            valueText = hsv.hue.roundToInt().toString(),
            value = hsv.hue,
            range = 0f..360f,
            track = Brush.horizontalGradient(AccentHueSpectrum),
            onValueChange = { hue ->
                hsv = hsv.copy(hue = hue)
                onLiveChange(hsv.toArgb())
            },
            onValueChangeFinished = { onCommit(hsv.toArgb()) }
        )
        AccentHsvSlider(
            label = stringResource(R.string.theme_accent_saturation),
            valueText = "${(hsv.saturation * 100).roundToInt()}%",
            value = hsv.saturation,
            range = 0f..1f,
            track = Brush.horizontalGradient(
                listOf(accentPaint(hsv.hue, 0f, hsv.value), accentPaint(hsv.hue, 1f, hsv.value))
            ),
            onValueChange = { sat ->
                hsv = hsv.copy(saturation = sat)
                onLiveChange(hsv.toArgb())
            },
            onValueChangeFinished = { onCommit(hsv.toArgb()) }
        )
        AccentHsvSlider(
            label = stringResource(R.string.theme_accent_brightness),
            valueText = "${(hsv.value * 100).roundToInt()}%",
            value = hsv.value,
            range = 0f..1f,
            track = Brush.horizontalGradient(
                listOf(accentPaint(hsv.hue, hsv.saturation, 0f), accentPaint(hsv.hue, hsv.saturation, 1f))
            ),
            onValueChange = { bright ->
                hsv = hsv.copy(value = bright)
                onLiveChange(hsv.toArgb())
            },
            onValueChangeFinished = { onCommit(hsv.toArgb()) }
        )
    }
}

/** صفّ منزلق واحد: عنوان وقيمة فوقه، ومساره مُلوَّن بالتدرّج الذي يمثّل ما يضبطه. */
@Composable
private fun AccentHsvSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    track: Brush,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
            Text(text = valueText, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            onValueChangeFinished = onValueChangeFinished,
            thumb = { AccentSliderThumb() },
            track = { _ ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AccentTrackHeight)
                        .clip(RoundedCornerShape(maxRadius(MaxRadius.chip)))
                        .background(track)
                )
            }
        )
    }
}

/** مقبض المنزلق: دائرة بحدّ بلون التمييز، على نسق مقابض باقي الشاشات. */
@Composable
private fun AccentSliderThumb() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(AccentThumbSize)
            .background(scheme.surface, CircleShape)
            .border(BorderStroke(AccentThumbBorder, scheme.primary), CircleShape)
    )
}
