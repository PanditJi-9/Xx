/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/** Each domain has its own composition; the existing snapshot remains the only source of values. */
package nd.max.ui.subscreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import nd.max.R
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import nd.max.ui.design.MaxBatteryLevel
import nd.max.ui.design.MaxCapsule
import nd.max.ui.design.MaxCardShell
import nd.max.ui.design.MaxCardSpec
import nd.max.ui.design.MaxDataTrust
import nd.max.ui.design.MaxDisplaySchematic
import nd.max.ui.design.MaxFrequencyTrack
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxIllustration
import nd.max.ui.design.MaxIllustrationKind
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxTone
import nd.max.ui.design.MaxUsageBar
import nd.max.ui.design.MaxVisualSpec
import nd.max.ui.design.content
import nd.max.ui.design.maxDisplayProportion
import nd.max.ui.design.maxTrustLabel
import nd.max.ui.design.visual

/**
 * رأس القسم كاملًا — يُبنى من [DeviceInfoHero] الصافي (المقيس في `DeviceInfoFactsTest`).
 */
@Composable
internal fun DeviceInfoHeroView(
    hero: DeviceInfoHero,
    pending: Boolean,
    section: DeviceInfoSection = DeviceInfoSection.Overview,
) {
    if (hero.title == null && hero.subtitle == null && hero.gauges.isEmpty() && hero.tiles.isEmpty()) return
    MaxCardShell(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.lg),
    ) {
        when (section) {
            DeviceInfoSection.Overview -> IdentityHero(hero, pending)
            DeviceInfoSection.Cpu -> InstrumentHero(hero, pending, MaxIllustrationKind.Cpu)
            DeviceInfoSection.Gpu -> InstrumentHero(hero, pending, MaxIllustrationKind.Gpu)
            DeviceInfoSection.Memory -> CapacityHero(hero, pending, MaxIllustrationKind.Memory)
            DeviceInfoSection.Storage -> CapacityHero(hero, pending, MaxIllustrationKind.Storage)
            DeviceInfoSection.Battery -> BatteryHero(hero, pending)
            DeviceInfoSection.Display -> PanelHero(hero, pending)
            DeviceInfoSection.Network -> TrafficHero(hero, pending)
            DeviceInfoSection.System -> ProfileHero(hero, pending, MaxIllustrationKind.System, R.string.android_version)
            DeviceInfoSection.Thermal -> ProfileHero(hero, pending, MaxIllustrationKind.Thermal, R.string.devinfo_temp_cpu)
            DeviceInfoSection.Sensors -> ProfileHero(hero, pending, MaxIllustrationKind.Sensors, R.string.devinfo_sensor_count)
            DeviceInfoSection.Camera -> ProfileHero(hero, pending, MaxIllustrationKind.Camera, R.string.devinfo_camera_megapixels)
            DeviceInfoSection.Audio -> ProfileHero(hero, pending, MaxIllustrationKind.Audio, R.string.max_audio_sample_rate_caption)
        }
    }
}

@Composable
private fun IdentityHero(hero: DeviceInfoHero, pending: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
            hero.title?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            hero.subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HeroMark(MaxIllustrationKind.Device)
    }
    hero.gauges.forEach {
        GaugeReadout(it, large = false)
        MaxUsageBar(it.fraction)
    }
    HeroStats(hero.tiles, pending)
}

@Composable
private fun InstrumentHero(hero: DeviceInfoHero, pending: Boolean, kind: MaxIllustrationKind) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            hero.gauges.forEach { GaugeReadout(it, large = true) }
        }
        HeroMark(kind)
    }
    hero.gauges.forEach { MaxFrequencyTrack(it.fraction, limit = it.markerFraction) }
    HeroStats(hero.tiles, pending)
}

@Composable
private fun CapacityHero(hero: DeviceInfoHero, pending: Boolean, kind: MaxIllustrationKind) {
    hero.gauges.forEach { gauge ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
        ) {
            MaxIllustration(kind, size = MaxVisualSpec.card)
            Column(Modifier.weight(1f)) { GaugeReadout(gauge, large = true) }
        }
        MaxUsageBar(gauge.fraction, height = MaxVisualSpec.capacityHeight)
    }
    HeroStats(hero.tiles, pending)
}

@Composable
private fun BatteryHero(hero: DeviceInfoHero, pending: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val gauge = hero.gauges.firstOrNull()
        if (gauge == null) HeroMark(MaxIllustrationKind.Power) else MaxBatteryLevel(gauge.fraction)
        Column(Modifier.weight(1f)) {
            if (gauge != null) GaugeReadout(gauge, large = true)
            else {
                Text(stringResource(R.string.devinfo_battery_level), style = MaterialTheme.typography.labelMedium)
                HeroValue(MAX_VALUE_UNAVAILABLE, null, large = true)
            }
        }
    }
    HeroStats(hero.tiles, pending)
}

@Composable
private fun PanelHero(hero: DeviceInfoHero, pending: Boolean) {
    val resolution = hero.tiles.firstOrNull { it.captionRes == R.string.devinfo_resolution }
    val refresh = hero.tiles.firstOrNull { it.captionRes == R.string.devinfo_refresh }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
    ) {
        val width = hero.panelWidth?.toFloat()
        val height = hero.panelHeight?.toFloat()
        if (maxDisplayProportion(width, height) != null) {
            MaxDisplaySchematic(width, height, size = heroGraphicSize())
        } else HeroMark(MaxIllustrationKind.Display)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
            resolution?.let { HeroStat(it, pending, large = true) }
            refresh?.let { HeroStat(it, pending, large = true) }
        }
    }
    HeroStats(hero.tiles.filterNot { it === resolution || it === refresh }, pending)
}

@Composable
private fun TrafficHero(hero: DeviceInfoHero, pending: Boolean) {
    MaxIllustration(MaxIllustrationKind.Network, size = MaxVisualSpec.card)
    HeroStats(hero.tiles.take(2), pending, large = true)
    MaxGroupDivider(inset = false)
    HeroStats(hero.tiles.drop(2), pending)
}

@Composable
private fun ProfileHero(
    hero: DeviceInfoHero,
    pending: Boolean,
    kind: MaxIllustrationKind,
    primaryCaption: Int,
) {
    val primary = hero.tiles.firstOrNull { it.captionRes == primaryCaption }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
    ) {
        Column(Modifier.weight(1f)) { primary?.let { HeroStat(it, pending, large = true) } }
        HeroMark(kind)
    }
    HeroStats(hero.tiles.filterNot { it === primary }, pending)
}

@Composable
private fun heroGraphicSize() = if (LocalDensity.current.fontScale > MaxVisualSpec.largeFontScale) {
    MaxVisualSpec.mark
} else MaxVisualSpec.instrument

@Composable
private fun HeroMark(kind: MaxIllustrationKind) {
    MaxIllustration(kind, size = heroGraphicSize())
}

@Composable
private fun GaugeReadout(gauge: DeviceInfoGauge, large: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
        Text(
            stringResource(gauge.titleRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HeroValue(gauge.valueText, gauge.unitText, large)
        gauge.subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Plain readouts replace the nested square cards. Width and enlarged text decide the column count. */
@Composable
private fun HeroStats(tiles: List<DeviceInfoTile>, pending: Boolean, large: Boolean = false) {
    if (tiles.isEmpty()) return
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (fontScale > MaxVisualSpec.largeFontScale || maxWidth < MaxCardSpec.minColumnWidth * 2) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.lg)) {
            tiles.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaxSpace.lg),
                ) {
                    row.forEach { tile ->
                        Column(Modifier.weight(1f)) { HeroStat(tile, pending, large) }
                    }
                    if (row.size < columns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeroStat(tile: DeviceInfoTile, pending: Boolean, large: Boolean = false) {
    val trust = tile.trust.asMaxTrust(pending)
    val showsValue = trust.visual().showsValue && (tile.value != null || tile.valueRes != null)
    val valueText = when {
        !showsValue -> MAX_VALUE_UNAVAILABLE
        tile.valueRes != null -> stringResource(tile.valueRes)
        else -> tile.value.orEmpty()
    }
    Column(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
    ) {
        Text(
            stringResource(tile.captionRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HeroValue(valueText, if (showsValue) tile.unit else null, large)
        if (!showsValue) {
            Text(maxTrustLabel(trust), style = MaterialTheme.typography.bodySmall, color = trust.visual().tone.content())
        }
    }
}

@Composable
private fun HeroValue(value: String, unit: String?, large: Boolean) {
    val text = listOfNotNull(value, unit?.takeIf { it.isNotBlank() }).joinToString(" ")
    val typography = MaterialTheme.typography
    val style = when {
        value.length > 20 -> typography.bodyMedium
        large -> typography.headlineMedium
        else -> typography.titleMedium
    }
    Text(
        text = text,
        style = style.copy(fontFeatureSettings = "tnum"),
        fontWeight = FontWeight.SemiBold,
        color = if (large) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    )
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
