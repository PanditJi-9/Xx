/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * Device Info — **رأس القسم**: ملخص الهوية، ومقياس الاستخدام الرشيق، وبلاطات الإحصاء.
 *
 * أُعيد تطوير هذا الرأس ليكون متناسقاً وأنيقاً ضمن لغة التصميم الموحدة (MaxManager):
 * - إزالة اللوحة المظلمة (NeuralPanel) والمقياس الدائري الضخم (RadialGaugeCard) الذي كان يستهلك
 *   معظم مساحة الشاشة ويسبب عدم اتساق بين الأقسام.
 * - استبدالها بقشرة بطاقة موحدة (MaxCardShell) ومقياس استخدام شريطي رشيق (MaxUsageBar).
 * - تصميم بلاطات إحصاء مضغوطة متناسقة وأنيقة تحفظ كثافة المعلومات وسهولة القراءة.
 */
package nd.max.ui.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nd.max.R
import nd.max.ui.component.LivePulseDot
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import nd.max.ui.design.MaxCapsule
import nd.max.ui.design.MaxCardShell
import nd.max.ui.design.MaxDataTrust
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxTone
import nd.max.ui.design.MaxUsageBar
import nd.max.ui.design.content
import nd.max.ui.design.maxTrustLabel
import nd.max.ui.design.visual
import nd.max.ui.design.maxRadius

/**
 * رأس القسم كاملًا — يُبنى من [DeviceInfoHero] الصافي (المقيس في `DeviceInfoFactsTest`).
 */
@Composable
internal fun DeviceInfoHeroView(hero: DeviceInfoHero, pending: Boolean) {
    if (hero.title == null && hero.subtitle == null && hero.gauges.isEmpty() && hero.tiles.isEmpty()) return

    MaxCardShell(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm),
    ) {
        // هوية الجهاز في قسم النظرة العامة
        hero.title?.takeIf { it.isNotBlank() }?.let { title ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Rounded.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    hero.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // مقياس الاستخدام الحي الرشيق (بديل المقياس الدائري الضخم)
        hero.gauges.forEach { gauge ->
            DeviceInfoGaugeView(gauge)
        }

        // شبكة البلاطات الإحصائية المصغرة
        if (hero.tiles.isNotEmpty()) {
            DeviceInfoTileGrid(hero.tiles, pending)
        }
    }
}

/**
 * مقياس استخدام رشيق يظهر النسبة والقيمة الحيّة بشريط أفقي متناسق بدلاً من الدائرة الضخمة.
 */
@Composable
private fun DeviceInfoGaugeView(gauge: DeviceInfoGauge) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(maxRadius(MaxRadius.chip)))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f))
            .padding(horizontal = MaxSpace.md, vertical = MaxSpace.sm),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (gauge.live) {
                    LivePulseDot(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(MaxSpace.xs))
                }
                Text(
                    text = stringResource(gauge.titleRes),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = gauge.valueText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (gauge.unitText.isNotBlank()) {
                    Spacer(Modifier.width(MaxSpace.hairline))
                    Text(
                        text = gauge.unitText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        MaxUsageBar(
            fraction = gauge.fraction,
            modifier = Modifier.fillMaxWidth(),
            tone = if (gauge.fraction > 0.88f && gauge.live) MaxTone.Caution else MaxTone.Accent,
            height = 5.dp,
        )
        if (!gauge.subtitle.isNullOrBlank()) {
            Text(
                text = gauge.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
    }
}

/**
 * شبكة البلاطات: صفّان في كل صفّ — متوازنة ورشيقة.
 */
@Composable
private fun DeviceInfoTileGrid(tiles: List<DeviceInfoTile>, pending: Boolean) {
    tiles.chunked(2).forEach { rowTiles ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
        ) {
            rowTiles.forEach { tile ->
                Box(modifier = Modifier.weight(1f)) {
                    DeviceInfoTileView(tile, pending)
                }
            }
            if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/**
 * بلاطة إحصائية مصغرة وأنيقة بتنسيق متناسق وهوامش مريحة.
 */
@Composable
private fun DeviceInfoTileView(tile: DeviceInfoTile, pending: Boolean) {
    val trust = tile.trust.asMaxTrust(pending)
    val showsValue = trust.visual().showsValue && (tile.value != null || tile.valueRes != null)
    val valueText = when {
        tile.valueRes != null -> stringResource(tile.valueRes)
        showsValue -> tile.value.orEmpty()
        else -> MAX_VALUE_UNAVAILABLE
    }
    val accentColor = trust.visual().tone.content()

    Surface(
        shape = RoundedCornerShape(maxRadius(MaxRadius.chip)),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        border = BorderStroke(MaxSize.hairlineBorder, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = MaxSpace.sm, vertical = MaxSpace.sm),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(tile.captionRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!showsValue && !pending) {
                    Box(
                        modifier = Modifier
                            .size(MaxSpace.xs)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.7f)),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.hairline),
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (showsValue) MaterialTheme.colorScheme.onSurface else accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val supportText = if (showsValue) tile.unit else maxTrustLabel(trust)
                if (!supportText.isNullOrBlank()) {
                    Text(
                        text = supportText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * شرائح القدرات — صفٌّ لكل خاصية، ووسمٌ يقول حكم المنصّة لا انطباعنا.
 */
@Composable
internal fun DeviceInfoChipGroup(chips: List<DeviceInfoChipRow>, title: String) {
    if (chips.isEmpty()) return
    MaxSection(title = title) {
        MaxGroup {
            chips.forEachIndexed { position, chip ->
                if (position > 0) MaxGroupDivider()
                MaxRow(
                    title = stringResource(chip.labelRes),
                    trailing = {
                        MaxCapsule(
                            text = chipStateLabel(chip.state),
                            tone = chipStateTone(chip.state),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun chipStateLabel(state: CapabilityState): String = when (state) {
    CapabilityState.Supported -> stringResource(R.string.devinfo_cap_supported)
    CapabilityState.NotDeclared -> stringResource(R.string.devinfo_cap_not_declared)
    CapabilityState.Unreadable -> maxTrustLabel(MaxDataTrust.Unreadable)
}

/** واللون **يُقوّي النصّ لا يُغني عنه**: الوسم يظلّ مكتوبًا في كل الحالات. */
private fun chipStateTone(state: CapabilityState): MaxTone = when (state) {
    CapabilityState.Supported -> MaxTone.Positive
    CapabilityState.NotDeclared -> MaxTone.Inactive
    CapabilityState.Unreadable -> MaxTone.Caution
}
