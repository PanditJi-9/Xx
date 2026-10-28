/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned. Modifications and architecture (C) 2026 Nader Magdy.
 *
 * نماذج وهندسة الأنماط البصرية للجزيرة الديناميكية (HyperOS 4 / ColorOS 17 / iOS / Material)
 */
package nd.max.island.engine

import androidx.annotation.StringRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nd.max.R

/**
 * أنماط تصميم الجزيرة الديناميكية المستوحاة من أفضل واجهات الفلاجشيب:
 *  - HyperOS 4: كبسولة أوبسيديان فخمة مع إضاءة نيونية وانتقالات فائقة الاستجابة.
 *  - ColorOS 17 (Aqua Dynamics): زجاج سائل شبه شفاف (Aqua Liquid Glass) مع حافة لمعان علوية وانقسام سائل.
 *  - iOS Modern: أسود أوليد عميق كلاسيكي بحواف سكويركل منحنية ولمعان فضي رقيق.
 *  - Dynamic Material: ألوان نيونية متفاعلة مع ألوان النظام الديناميكية (Material You).
 */
enum class IslandThemeStyle(
    val id: Int,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int
) {
    HyperOS4(0, R.string.island_style_hyperos, R.string.island_style_desc),
    ColorOS17(1, R.string.island_style_coloros, R.string.island_style_desc),
    iOSModern(2, R.string.island_style_ios, R.string.island_style_desc),
    DynamicMaterial(3, R.string.island_style_material, R.string.island_style_desc);

    companion object {
        fun fromId(id: Int): IslandThemeStyle = entries.firstOrNull { it.id == id } ?: HyperOS4
    }
}

/** لوحة المظهر البصري المتكاملة للكبسولة. */
data class IslandVisualPalette(
    val backgroundBrush: Brush,
    val specularRimBrush: Brush,
    val borderColor: Color,
    val borderWidth: Dp,
    val glowColor: Color,
    val glowAlpha: Float,
    val springDamping: Float,
    val springStiffness: Float,
    val isGlassmorphic: Boolean,
)

object IslandStyleFactory {

    val HyperNeonCyan = Color(0xFF00F0FF)
    val HyperNeonGreen = Color(0xFF10F284)
    val AquaDropletBlue = Color(0xFF00A2FF)
    val AquaSpecularWhite = Color(0x3564D2FF)
    val ObsidianDeep = Color(0xF2080A0D)
    val ObsidianPure = Color(0xFA040507)

    fun resolvePalette(
        styleId: Int,
        appAccent: Color?,
        mode: IslandMode,
        glowEnabled: Boolean,
    ): IslandVisualPalette {
        val style = IslandThemeStyle.fromId(styleId)

        val modeAccent: Color = when (mode) {
            is IslandMode.Media -> appAccent ?: mode.accent?.let { Color(it) } ?: AquaDropletBlue
            is IslandMode.Charging -> HyperNeonGreen
            is IslandMode.Call -> HyperNeonGreen
            is IslandMode.Timer -> Color(0xFFFF9F0A)
            is IslandMode.LowBattery -> Color(0xFFFF453A)
            is IslandMode.Live -> AquaDropletBlue
            is IslandMode.Toggle -> mode.tint()
            else -> Color.Transparent
        }

        return when (style) {
            IslandThemeStyle.HyperOS4 -> IslandVisualPalette(
                backgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xF50D0F14), Color(0xFC06080B))
                ),
                specularRimBrush = Brush.verticalGradient(
                    listOf(
                        Color(0x38FFFFFF),
                        Color(0x0CFFFFFF),
                        Color.Transparent
                    )
                ),
                borderColor = if (modeAccent != Color.Transparent) {
                    modeAccent.copy(alpha = 0.55f)
                } else {
                    Color(0xFF242730)
                },
                borderWidth = 0.75.dp,
                glowColor = modeAccent,
                glowAlpha = if (glowEnabled) 0.38f else 0f,
                springDamping = 0.76f,
                springStiffness = 2200f,
                isGlassmorphic = true,
            )

            IslandThemeStyle.ColorOS17 -> IslandVisualPalette(
                backgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xEB0A1420), Color(0xF5050A10))
                ),
                specularRimBrush = Brush.verticalGradient(
                    listOf(
                        AquaSpecularWhite,
                        Color(0x0E00A2FF),
                        Color.Transparent
                    )
                ),
                borderColor = if (modeAccent != Color.Transparent) {
                    modeAccent.copy(alpha = 0.48f)
                } else {
                    Color(0x3300A2FF)
                },
                borderWidth = 0.8.dp,
                glowColor = if (modeAccent != Color.Transparent) modeAccent else AquaDropletBlue,
                glowAlpha = if (glowEnabled) 0.34f else 0f,
                springDamping = 0.68f,  // ارتداد سائل كقطرات الماء (Aqua droplet spring)
                springStiffness = 1450f,
                isGlassmorphic = true,
            )

            IslandThemeStyle.iOSModern -> IslandVisualPalette(
                backgroundBrush = SolidColor(Color.Black),
                specularRimBrush = Brush.verticalGradient(
                    listOf(
                        Color(0x28FFFFFF),
                        Color(0x06FFFFFF),
                        Color.Transparent
                    )
                ),
                borderColor = if (modeAccent != Color.Transparent) {
                    modeAccent.copy(alpha = 0.45f)
                } else {
                    Color(0xFF1C1C1E)
                },
                borderWidth = 0.6.dp,
                glowColor = modeAccent,
                glowAlpha = if (glowEnabled) 0.28f else 0f,
                springDamping = 0.85f,
                springStiffness = 2400f,
                isGlassmorphic = false,
            )

            IslandThemeStyle.DynamicMaterial -> IslandVisualPalette(
                backgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xF2121318), Color(0xFA0A0B0E))
                ),
                specularRimBrush = Brush.verticalGradient(
                    listOf(
                        Color(0x25FFFFFF),
                        Color(0x08FFFFFF),
                        Color.Transparent
                    )
                ),
                borderColor = (appAccent ?: modeAccent).copy(alpha = 0.50f),
                borderWidth = 0.75.dp,
                glowColor = appAccent ?: modeAccent,
                glowAlpha = if (glowEnabled) 0.40f else 0f,
                springDamping = 0.80f,
                springStiffness = 1900f,
                isGlassmorphic = true,
            )
        }
    }
}

/**
 * محاكي رسم المظهر البصري الزجاجي المتقدم:
 *  - هالة تنفس نيونية/مائية خلف وأسفل الكبسولة.
 *  - حافة اللمعان الضوئي العلوية (Specular Rim Highlight).
 */
fun Modifier.drawIslandSurface(
    palette: IslandVisualPalette,
    cornerRadius: Dp,
): Modifier = this.drawBehind {
    // 1. رسم الهالة التفاعلية المضيئة
    if (palette.glowAlpha > 0f && palette.glowColor != Color.Transparent) {
        val glowBrush = Brush.radialGradient(
            colors = listOf(
                palette.glowColor.copy(alpha = palette.glowAlpha),
                palette.glowColor.copy(alpha = palette.glowAlpha * 0.4f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.20f, size.height * 0.5f),
            radius = size.width * 0.80f
        )
        drawRect(brush = glowBrush)
    }

    // 2. رسم لمعان الحافة العلوية (Specular Rim Light) لمحاكاة عدسة الكاميرا المصقولة
    if (palette.isGlassmorphic) {
        val rimHeight = (size.height * 0.35f).coerceAtMost(22.dp.toPx())
        drawRect(
            brush = palette.specularRimBrush,
            topLeft = Offset.Zero,
            size = Size(size.width, rimHeight)
        )
    }
}
