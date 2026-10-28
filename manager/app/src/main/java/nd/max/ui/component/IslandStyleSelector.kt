/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root.
 *
 * مكون اختيار النمط البصري وخصائص الفلاجشيب للجزيرة الديناميكية (HyperOS 4 / ColorOS 17)
 */
package nd.max.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.FlipToFront
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import nd.max.R
import nd.max.island.core.IslandSettings
import nd.max.island.engine.IslandThemeStyle
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSegmented
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxSwitchRow

/**
 * قسم اختيار نمط الجزيرة البصري (HyperOS 4 / ColorOS 17 / iOS / Material).
 */
@Composable
fun IslandStyleSelectorSection() {
    val currentStyle by IslandSettings.islandThemeStyle.collectAsState()

    MaxSection(
        title = stringResource(R.string.island_style_title),
        description = stringResource(R.string.island_style_desc)
    ) {
        MaxGroup {
            IslandThemeStyle.entries.forEachIndexed { index, style ->
                if (index > 0) MaxGroupDivider()
                val selected = currentStyle == style.id
                val shape = RoundedCornerShape(MaxRadius.row)
                val bg = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(bg)
                        .clickable {
                            IslandSettings.islandThemeStyle.value = style.id
                            IslandSettings.save()
                        }
                        .padding(
                            horizontal = MaxSpace.rowPaddingHorizontal,
                            vertical = MaxSpace.rowPaddingVertical
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(MaxSpace.xl)
                    )
                    Spacer(Modifier.width(MaxSpace.md))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(style.titleRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * قسم المزايا المتقدمة وتموضع الكاميرا والجزيرة المزدوجة.
 */
@Composable
fun IslandFlagshipFeaturesSection() {
    val splitActive by IslandSettings.splitIsland.collectAsState()
    val cutoutAnchor by IslandSettings.cutoutAnchor.collectAsState()
    val hyperChargeOn by IslandSettings.hyperCharge.collectAsState()
    val waveformOn by IslandSettings.interactiveWaveform.collectAsState()

    MaxSection(
        title = stringResource(R.string.island_dual_split_title),
        description = stringResource(R.string.island_dual_split_desc)
    ) {
        MaxGroup {
            MaxSwitchRow(
                title = stringResource(R.string.island_dual_split_title),
                subtitle = stringResource(R.string.island_dual_split_desc),
                icon = Icons.Rounded.FlipToFront,
                checked = splitActive,
                onCheckedChange = {
                    IslandSettings.splitIsland.value = it
                    IslandSettings.save()
                }
            )
            MaxGroupDivider()
            MaxSwitchRow(
                title = stringResource(R.string.island_turbo_charge_title),
                subtitle = stringResource(R.string.island_turbo_charge_desc),
                icon = Icons.Rounded.ElectricBolt,
                checked = hyperChargeOn,
                onCheckedChange = {
                    IslandSettings.hyperCharge.value = it
                    IslandSettings.save()
                }
            )
            MaxGroupDivider()
            MaxSwitchRow(
                title = stringResource(R.string.island_interactive_waveform_title),
                subtitle = stringResource(R.string.island_interactive_waveform_desc),
                icon = Icons.Rounded.GraphicEq,
                checked = waveformOn,
                onCheckedChange = {
                    IslandSettings.interactiveWaveform.value = it
                    IslandSettings.save()
                }
            )
            MaxGroupDivider()
            Column(
                modifier = Modifier.padding(
                    horizontal = MaxSpace.rowPaddingHorizontal,
                    vertical = MaxSpace.sm
                ),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
            ) {
                Text(
                    text = stringResource(R.string.island_cutout_anchor_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val cutoutLabels = listOf(
                    stringResource(R.string.island_cutout_center),
                    stringResource(R.string.island_cutout_left),
                    stringResource(R.string.island_cutout_right),
                    stringResource(R.string.island_cutout_top)
                )
                MaxSegmented(
                    options = cutoutLabels,
                    selectedIndex = cutoutAnchor.coerceIn(0, 3),
                    onSelect = {
                        IslandSettings.cutoutAnchor.value = it
                        IslandSettings.save()
                    }
                )
            }
        }
    }
}
