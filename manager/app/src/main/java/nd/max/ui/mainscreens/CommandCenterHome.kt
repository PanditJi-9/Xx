/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * **Screen card — archetype: Instrument · object: the device as one live core ·
 * carrying element: the reactor gauge.**
 *
 * This is a deliberate break from the card feed that preceded it. The first question is not
 * "which card do I read first"; it is "is the device under pressure, and what can I do now".
 * So the page opens on identity, drops immediately into one instrument that fuses CPU, GPU,
 * RAM and heat, then gives the three actions that answer pressure, and only then lists the
 * rest as a flat telemetry fabric and a dock.
 *
 * States: live · a reading the device does not report is an em dash, never zero · engine
 * offline says so in the header · focus alert appears only for a real heat/RAM/storage
 * problem · deck empty names its settings path · reduced-motion turns the gauge snap.
 *
 * The old card dashboard stays in `LegendaryHomeDashboard.kt` as the detailed fallback and
 * for the source-level guards; this file owns the surface the person actually sees.
 */
package nd.max.ui.mainscreens

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import nd.max.R
import nd.max.core.maxai.MaxAiState
import nd.max.core.privilege.PrivilegeLevel
import nd.max.ui.component.MaxAiEntryButton
import nd.max.ui.component.NeuralIconChip
import nd.max.ui.component.NeuralPill
import nd.max.ui.component.NeuralTile
import nd.max.ui.component.NeuralValue
import nd.max.ui.component.neuralClickable
import nd.max.ui.component.neuralPalette
import nd.max.ui.component.rememberAnimationsEnabled
import nd.max.ui.design.MaxAlpha
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.content
import nd.max.ui.design.maxRadius
import nd.max.ui.navigation.MaxDestination
import nd.max.ui.theme.MonoValueStyleLarge
import nd.max.ui.theme.MonoValueStyleMedium
import nd.max.ui.viewmodel.DashboardState
import nd.max.ui.viewmodel.HomeUiState
import nd.max.ui.viewmodel.MemoryBoostState

@Composable
internal fun CommandCenterHome(
    timeLeftMinutes: Int?,
    ui: HomeUiState,
    dashboard: DashboardState,
    maxAi: MaxAiState,
    deviceName: String,
    deckEntries: List<HomeDeckEntry>,
    onOpenDeck: (HomeDeckEntry) -> Unit,
    onConfigureDeck: () -> Unit,
    onNavigate: (String) -> Unit,
    onReboot: () -> Unit,
    onSettings: () -> Unit,
    accessLevel: PrivilegeLevel,
    boost: MemoryBoostState,
    onBoost: () -> Unit,
    onShowGuide: () -> Unit,
    storageCardVisible: Boolean,
    onHideStorage: () -> Unit,
    onAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val online = (ui.rootStatus || ui.daemonOnline) && ui.moduleInstalled
    val heat = deviceHeatC(dashboard)
    val ramFraction = fractionOf(dashboard.ramUsedMb, dashboard.ramTotalMb)
    val focusKind = HomeFocusModel.focusKind(
        heatC = heat ?: 0,
        ramFraction = ramFraction,
        storageVisible = storageCardVisible,
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.section),
    ) {
        CommandHeader(
            online = online,
            onShowGuide = onShowGuide,
            onReboot = onReboot,
            onSettings = onSettings,
        )

        HomeTourAnchor(HomeTourTarget.Hero) {
            CommandIdentity(
                deviceName = deviceName,
                chipsetName = dashboard.chipsetName,
                profile = stringResource(ui.currentProfileRes),
                accessLevel = accessLevel,
                heat = heat,
                cpuLoad = dashboard.cpuLoadPercent,
                aiEnabled = maxAi.aiEnabled,
                onAccess = onAccess,
                onOverview = { onNavigate(MaxDestination.DeviceInfo.route) },
                onMaxAi = { onNavigate(MaxDestination.MaxAi.route) },
            )
        }

        HomeTourAnchor(HomeTourTarget.Pulse) {
            ReactorDeck(
                dashboard = dashboard,
                heat = heat,
                ramFraction = ramFraction,
                onOpen = { onNavigate(MaxDestination.ThermalDetail.route) },
                onCpu = { onNavigate(MaxDestination.CpuCoreControl.route) },
                onGpu = { onNavigate(MaxDestination.GpuStudio.route) },
                onRam = { onNavigate(MaxDestination.ZramManager.route) },
            )
        }

        if (focusKind != null) {
            CommandAlert(
                kind = focusKind,
                onOpen = { route -> onNavigate(route) },
                onHideStorage = onHideStorage,
            )
        }

        HomeTourAnchor(HomeTourTarget.Actions) {
            CommandActions(
                boost = boost,
                aiEnabled = maxAi.aiEnabled,
                onBoost = onBoost,
                onControl = { onNavigate(MaxDestination.Control.route) },
                onGameSpace = { onNavigate(MaxDestination.GameLobby.route) },
                onMaxAi = { onNavigate(MaxDestination.MaxAi.route) },
            )
        }

        HomeTourAnchor(HomeTourTarget.Vitals) {
            TelemetryDeck(
                dashboard = dashboard,
                onNavigate = onNavigate,
            )
        }

        HomeTourAnchor(HomeTourTarget.Deck) {
            CommandDock(
                entries = deckEntries,
                onOpen = onOpenDeck,
                onConfigure = onConfigureDeck,
            )
        }

        timeLeftMinutes?.let { minutes ->
            Text(
                stringResource(
                    R.string.home_time_left,
                    stringResource(R.string.home_duration_hm, minutes / 60, minutes % 60),
                ),
                color = neuralPalette().muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
    }
}

/**
 * Brand and one state. The wordmark is the note door it has always been; the three icons are
 * the same three commitments (guide, power, settings), not a new toolbar to learn.
 */
@Composable
private fun CommandHeader(
    online: Boolean,
    onShowGuide: () -> Unit,
    onReboot: () -> Unit,
    onSettings: () -> Unit,
) {
    val p = neuralPalette()
    val noteTitle = stringResource(R.string.home_note_title)
    var noteOpen by rememberSaveable { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .heightIn(min = MaxSize.minTouchTarget)
                    .semantics { contentDescription = noteTitle }
                    .neuralClickable({ noteOpen = true }, role = Role.Button),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(MaxSpace.xxl)
                        .clip(RoundedCornerShape(maxRadius(MaxRadius.chip)))
                        .background(p.accent.copy(alpha = MaxAlpha.toneContainerStrong)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.max_wordmark),
                        color = p.accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                }
            }
            Spacer(Modifier.width(MaxSpace.sm))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    stringResource(R.string.max_home_command_center),
                    color = p.text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.max_home_dashboard_eyebrow),
                    color = p.muted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
                    .background((if (online) p.ok else p.danger).copy(alpha = MaxAlpha.toneContainer))
                    .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(MaxSpace.xs)
                            .clip(CircleShape)
                            .background(if (online) p.ok else p.danger)
                    )
                    Spacer(Modifier.width(MaxSpace.xs))
                    Text(
                        stringResource(if (online) R.string.home_active else R.string.home_idle),
                        color = p.text,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
            HeaderDoor(Icons.Rounded.HelpOutline, stringResource(R.string.home_banner_restore), onShowGuide)
            HeaderDoor(Icons.Rounded.PowerSettingsNew, stringResource(R.string.max_home_power), onReboot)
            HeaderDoor(Icons.Rounded.Settings, stringResource(R.string.max_home_settings), onSettings)
        }
    }
    if (noteOpen) {
        HomeNoteWindow(onClose = { noteOpen = false })
    }
}

@Composable
private fun HeaderDoor(icon: ImageVector, description: String, onClick: () -> Unit) {
    val p = neuralPalette()
    IconButton(onClick = onClick, modifier = Modifier.size(MaxSize.minTouchTarget)) {
        Icon(icon, description, Modifier.size(MaxSize.iconGlyph), tint = p.muted)
    }
}

/** Identity is typographic, not a card: the loudest thing on the screen is the device itself. */
@Composable
private fun CommandIdentity(
    deviceName: String,
    chipsetName: String,
    profile: String,
    accessLevel: PrivilegeLevel,
    heat: Int?,
    cpuLoad: Int,
    aiEnabled: Boolean,
    onAccess: () -> Unit,
    onOverview: () -> Unit,
    onMaxAi: () -> Unit,
) {
    val p = neuralPalette()
    val attention = (heat ?: 0) >= 45 || cpuLoad >= 90
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
        ) {
            Text(
                stringResource(R.string.max_nav_now).uppercase(),
                color = p.accent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp,
            )
            Text(
                deviceName,
                color = p.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
            ) {
                Text(
                    chipsetName,
                    color = p.muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text("·", color = p.muted, style = MaterialTheme.typography.bodySmall)
                Text(
                    profile,
                    color = p.muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
            ) {
                NeuralPill(
                    text = stringResource(accessLevel.labelRes),
                    accent = when (accessLevel) {
                        PrivilegeLevel.ROOT -> p.ok
                        PrivilegeLevel.SHIZUKU -> p.accent
                        PrivilegeLevel.NONE -> p.muted
                    },
                    filled = true,
                    dot = true,
                    icon = if (accessLevel == PrivilegeLevel.ROOT) Icons.Rounded.Shield else null,
                    navigates = true,
                    onClick = onAccess,
                )
                Text(
                    stringResource(if (attention) R.string.home_system_attention else R.string.home_system_stable),
                    color = if (attention) p.warn else p.ok,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(MaxSpace.sm),
        ) {
            MaxAiEntryButton(
                text = stringResource(R.string.max_nav_max_ai),
                active = aiEnabled,
                onClick = onMaxAi,
            )
            Row(
                modifier = Modifier
                    .heightIn(min = MaxSize.minTouchTarget)
                    .neuralClickable(onOverview, role = Role.Button)
                    .padding(horizontal = MaxSpace.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.home_hero_open_overview),
                    color = p.muted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    null,
                    Modifier.size(MaxSize.iconGlyphSmall),
                    tint = p.muted,
                )
            }
        }
    }
}

/**
 * The reactor: one instrument instead of three cards. The outer arc is CPU, the middle GPU,
 * the inner RAM; the centre is the same heat reading the old hero used. When a reading is
 * absent its arc is empty and its legend says "—", never zero.
 */
@Composable
private fun ReactorDeck(
    dashboard: DashboardState,
    heat: Int?,
    ramFraction: Float,
    onOpen: () -> Unit,
    onCpu: () -> Unit,
    onGpu: () -> Unit,
    onRam: () -> Unit,
) {
    val p = neuralPalette()
    val animations = rememberAnimationsEnabled()
    val spec: AnimationSpec<Float> = remember(animations) {
        if (animations) tween(360) else snap()
    }
    val hasReading = dashboard.readingsAtMs > 0
    val ramKnown = dashboard.ramTotalMb > 0
    val cpuTarget = if (hasReading) dashboard.cpuLoadPercent.coerceIn(0, 100) / 100f else 0f
    val gpuTarget = ((dashboard.gpuLoadPercent ?: 0).coerceIn(0, 100) / 100f)
    val cpuArc by animateFloatAsState(cpuTarget, spec, label = "reactor-cpu")
    val gpuArc by animateFloatAsState(gpuTarget, spec, label = "reactor-gpu")
    val ramArc by animateFloatAsState(
        if (ramKnown) ramFraction.coerceIn(0f, 1f) else 0f,
        spec,
        label = "reactor-ram",
    )
    val shape = RoundedCornerShape(maxRadius(MaxRadius.group))
    val heatAccent = temperatureAccent(heat)

    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.max_home_live_hardware),
                color = p.muted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .heightIn(min = MaxSize.minTouchTarget)
                    .neuralClickable(onOpen, role = Role.Button),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.home_action_thermal),
                    color = heatAccent,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    null,
                    Modifier.size(MaxSize.iconGlyphSmall),
                    tint = heatAccent,
                )
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(p.panelTop.copy(alpha = .72f), p.panel.copy(alpha = .48f))
                    )
                )
                .border(MaxSize.hairlineBorder, p.border, shape)
                .neuralClickable(onOpen, role = Role.Button)
                .padding(vertical = MaxSpace.lg, horizontal = MaxSpace.sm),
        ) {
            val gauge = minOf(maxWidth - MaxSpace.xxl, 260.dp).coerceAtLeast(200.dp)
            Box(
                modifier = Modifier
                    .size(gauge)
                    .align(Alignment.Center),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val base = size.minDimension / 2f
                    val outer = base - 14.dp.toPx()
                    val line = if (size.minDimension < 520f) 5.dp.toPx() else 6.dp.toPx()
                    val start = 135f
                    val sweep = 270f
                    // Ticks: one every six degrees, longer every thirty. They make the
                    // instrument readable before any label is scanned.
                    var tick = 0
                    while (tick <= 45) {
                        val angle = (start + sweep * tick / 45f) * PI / 180.0
                        val major = tick % 5 == 0
                        val inner = outer + if (major) 2.dp.toPx() else 6.dp.toPx()
                        val end = outer + 12.dp.toPx()
                        drawLine(
                            color = if (major) p.muted.copy(alpha = .55f) else p.grid,
                            start = Offset(
                                center.x + cos(angle).toFloat() * inner,
                                center.y + sin(angle).toFloat() * inner,
                            ),
                            end = Offset(
                                center.x + cos(angle).toFloat() * end,
                                center.y + sin(angle).toFloat() * end,
                            ),
                            strokeWidth = if (major) 1.6.dp.toPx() else 1.dp.toPx(),
                        )
                        tick++
                    }
                    drawReactorRing(cpuArc, outer - 10.dp.toPx(), p.accent, line, p.grid, start, sweep)
                    drawReactorRing(gpuArc, outer - 24.dp.toPx(), p.accentAlt, line * .84f, p.grid, start, sweep)
                    drawReactorRing(ramArc, outer - 38.dp.toPx(), p.ok, line * .68f, p.grid, start, sweep)
                    // The core: a quiet filled centre, not a glow or a glass ball.
                    drawCircle(
                        color = p.panel.copy(alpha = .88f),
                        radius = outer - 54.dp.toPx(),
                        center = center,
                    )
                    drawCircle(
                        color = heatAccent.copy(alpha = .18f),
                        radius = outer - 54.dp.toPx(),
                        center = center,
                        style = Stroke(1.dp.toPx()),
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
                ) {
                    Text(
                        stringResource(R.string.home_temperature_short).uppercase(),
                        color = p.muted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        NeuralValue(
                            heat?.toString() ?: "—",
                            style = MonoValueStyleLarge,
                            color = p.text,
                        )
                        if (heat != null) {
                            Spacer(Modifier.width(MaxSpace.xs))
                            Text(
                                stringResource(R.string.home_unit_celsius),
                                color = p.muted,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(bottom = MaxSpace.xs),
                            )
                        }
                    }
                    Text(
                        stringResource(heatWordRes(heat)),
                        color = heatAccent,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
        ) {
            ReactorLegend(
                label = stringResource(R.string.max_home_cpu_label),
                value = if (hasReading) "${dashboard.cpuLoadPercent.coerceIn(0, 100)}%" else "—",
                accent = p.accent,
                onClick = onCpu,
                modifier = Modifier.weight(1f),
            )
            ReactorLegend(
                label = stringResource(R.string.max_home_gpu_label),
                value = dashboard.gpuLoadPercent?.let { "${it.coerceIn(0, 100)}%" } ?: "—",
                accent = p.accentAlt,
                onClick = onGpu,
                modifier = Modifier.weight(1f),
            )
            ReactorLegend(
                label = stringResource(R.string.max_home_memory),
                value = if (ramKnown) "${(ramFraction * 100).toInt().coerceIn(0, 100)}%" else "—",
                accent = p.ok,
                onClick = onRam,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun DrawScope.drawReactorRing(
    fraction: Float,
    radius: Float,
    color: Color,
    stroke: Float,
    track: Color,
    start: Float,
    sweep: Float,
) {
    val topLeft = Offset(center.x - radius, center.y - radius)
    val extent = Size(radius * 2, radius * 2)
    drawArc(
        color = track,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = topLeft,
        size = extent,
        style = Stroke(stroke, cap = StrokeCap.Round),
    )
    if (fraction > 0f) {
        drawArc(
            color = color,
            startAngle = start,
            sweepAngle = sweep * fraction.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = extent,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun ReactorLegend(
    label: String,
    value: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = neuralPalette()
    Column(
        modifier = modifier
            .heightIn(min = MaxSize.minTouchTarget)
            .neuralClickable(onClick, role = Role.Button),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(MaxSpace.xs).clip(CircleShape).background(accent))
            Spacer(Modifier.width(MaxSpace.xs))
            Text(
                label,
                color = p.muted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        NeuralValue(value, style = MonoValueStyleMedium, color = p.text, maxLines = 1)
    }
}

/**
 * Flat telemetry fabric: no four cards, one ruled surface. Each cell is a door to the screen
 * that owns the number. RAM carries the tour memory anchor because it is the capacity readout.
 */
@Composable
private fun TelemetryDeck(
    dashboard: DashboardState,
    onNavigate: (String) -> Unit,
) {
    val p = neuralPalette()
    val shape = RoundedCornerShape(maxRadius(MaxRadius.group))
    val hasReading = dashboard.readingsAtMs > 0
    val battery = dashboard.batteryPercent.takeIf { it > 0 }
    val ramPercent = if (dashboard.ramTotalMb > 0) {
        (fractionOf(dashboard.ramUsedMb, dashboard.ramTotalMb) * 100).toInt().coerceIn(0, 100)
    } else null
    val freeRam = (dashboard.ramTotalMb - dashboard.ramUsedMb).coerceAtLeast(0)
    val usedStoragePercent = if (dashboard.storageTotalGb > 0f) {
        (dashboard.storageUsedGb / dashboard.storageTotalGb * 100f).coerceIn(0f, 100f).toInt()
    } else null
    val freeStorage = (dashboard.storageTotalGb - dashboard.storageUsedGb).coerceAtLeast(0f)
    val network = if (hasReading) {
        netSpeed(dashboard.downloadSpeedKbps + dashboard.uploadSpeedKbps)
    } else {
        "—"
    }

    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
        Text(
            stringResource(R.string.home_system_vitals).uppercase(),
            color = p.muted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(p.tile)
                .border(MaxSize.hairlineBorder, p.border, shape),
        ) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                TelemetryCell(
                    icon = if (dashboard.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                    label = stringResource(R.string.max_home_battery),
                    value = battery?.let { "$it%" } ?: "—",
                    support = if (dashboard.isCharging) {
                        stringResource(R.string.max_home_charging)
                    } else {
                        stringResource(R.string.max_home_battery_desc)
                    },
                    accent = if (dashboard.isCharging) p.ok else p.accentAlt,
                    onClick = { onNavigate(MaxDestination.Charging.route) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                Hairline(vertical = true)
                HomeTourAnchor(HomeTourTarget.Memory, Modifier.weight(1f).fillMaxHeight()) {
                    TelemetryCell(
                        icon = Icons.Rounded.Memory,
                        label = stringResource(R.string.max_home_memory),
                        value = ramPercent?.let { "$it%" } ?: "—",
                        support = if (dashboard.ramTotalMb > 0) {
                            stringResource(R.string.home_available_memory, gigabytes(freeRam))
                        } else {
                            stringResource(R.string.max_home_unavailable)
                        },
                        accent = p.accent,
                        onClick = { onNavigate(MaxDestination.ZramManager.route) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Hairline(vertical = false)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                TelemetryCell(
                    icon = Icons.Rounded.Storage,
                    label = stringResource(R.string.max_home_storage),
                    value = usedStoragePercent?.let { "$it%" } ?: "—",
                    support = if (dashboard.storageTotalGb > 0f) {
                        stringResource(R.string.home_available_storage, freeStorage.oneDecimal())
                    } else {
                        stringResource(R.string.max_home_unavailable)
                    },
                    accent = p.accentAlt,
                    onClick = { onNavigate(MaxDestination.StorageDetail.route) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                Hairline(vertical = true)
                TelemetryCell(
                    icon = Icons.Rounded.NetworkCheck,
                    label = stringResource(R.string.max_home_network),
                    value = network,
                    support = stringResource(R.string.home_network_status),
                    accent = p.ok,
                    onClick = { onNavigate(MaxDestination.NetworkHub.route) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun Hairline(vertical: Boolean) {
    val p = neuralPalette()
    if (vertical) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(MaxSize.hairlineBorder)
                .background(p.border)
        )
    } else {
        Box(
            Modifier
                .fillMaxWidth()
                .height(MaxSize.hairlineBorder)
                .background(p.border)
        )
    }
}

@Composable
private fun TelemetryCell(
    icon: ImageVector,
    label: String,
    value: String,
    support: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = neuralPalette()
    Column(
        modifier = modifier
            .neuralClickable(onClick, role = Role.Button)
            .padding(horizontal = MaxSpace.md, vertical = MaxSpace.md),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(MaxSize.iconGlyphSmall), tint = accent)
            Spacer(Modifier.width(MaxSpace.xs))
            Text(
                label,
                color = p.muted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        NeuralValue(value, style = MonoValueStyleMedium, color = p.text, maxLines = 1)
        Text(
            support,
            color = p.muted,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Three immediate answers, one hierarchy: Boost is the full-width action because it is the
 * only one that changes memory and reports what it freed; the other three are equal doors.
 */
@Composable
private fun CommandActions(
    boost: MemoryBoostState,
    aiEnabled: Boolean,
    onBoost: () -> Unit,
    onControl: () -> Unit,
    onGameSpace: () -> Unit,
    onMaxAi: () -> Unit,
) {
    val p = neuralPalette()
    val freedMb = boost.outcome?.freedMb?.takeIf { it > 0 }
    val accent = when {
        boost.blocked -> p.warn
        freedMb != null -> p.ok
        else -> p.accent
    }
    val onAccent = when {
        boost.blocked -> MaterialTheme.colorScheme.onTertiary
        freedMb != null -> MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.onPrimary
    }
    val support = when {
        boost.running -> stringResource(R.string.home_memory_boost_running)
        boost.blocked -> stringResource(R.string.home_memory_boost_blocked)
        boost.outcome != null && freedMb == null -> stringResource(R.string.home_memory_boost_none)
        else -> stringResource(R.string.home_memory_boost_desc)
    }
    val shape = RoundedCornerShape(maxRadius(MaxRadius.group))

    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
        Text(
            stringResource(R.string.home_section_actions).uppercase(),
            color = p.muted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = .84f))))
                .neuralClickable(onBoost, role = Role.Button)
                .padding(horizontal = MaxSpace.lg, vertical = MaxSpace.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(MaxSize.iconContainer)
                    .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
                    .background(onAccent.copy(alpha = .16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Memory, null, Modifier.size(MaxSize.iconGlyph), tint = onAccent)
            }
            Spacer(Modifier.width(MaxSpace.md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.home_memory_boost),
                    color = onAccent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    support,
                    color = onAccent.copy(alpha = .78f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (freedMb != null) {
                Text(
                    stringResource(R.string.home_memory_boost_freed, "$freedMb MB"),
                    color = onAccent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                Modifier.size(MaxSize.iconGlyph),
                tint = onAccent,
            )
        }
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
        ) {
            CommandKey(
                icon = MaxDestination.Control.icon,
                label = stringResource(MaxDestination.Control.titleRes),
                accent = p.accentAlt,
                onClick = onControl,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            CommandKey(
                icon = Icons.Rounded.Gamepad,
                label = stringResource(R.string.game_lobby_title),
                accent = p.accent,
                onClick = onGameSpace,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            CommandKey(
                icon = MaxDestination.MaxAi.icon,
                label = stringResource(R.string.max_nav_max_ai),
                accent = if (aiEnabled) p.ok else p.muted,
                onClick = onMaxAi,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun CommandKey(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = neuralPalette()
    NeuralTile(
        modifier = modifier,
        accent = accent,
        onClick = onClick,
        verticalSpacing = MaxSpace.sm,
    ) {
        NeuralIconChip(icon, accent, size = MaxSize.rowIconContainer)
        Text(
            label,
            color = p.text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A real problem gets one alert row, not a card that pretends to be another dashboard. */
@Composable
private fun CommandAlert(
    kind: HomeFocusKind,
    onOpen: (String) -> Unit,
    onHideStorage: () -> Unit,
) {
    val p = neuralPalette()
    val title: String
    val body: String
    val accent: Color
    val icon: ImageVector
    val route: String
    when (kind) {
        HomeFocusKind.HEAT -> {
            title = stringResource(R.string.home_focus_heat)
            body = stringResource(R.string.home_focus_heat_desc)
            accent = p.danger
            icon = Icons.Rounded.Thermostat
            route = MaxDestination.ThermalDetail.route
        }
        HomeFocusKind.STORAGE -> {
            title = stringResource(R.string.home_focus_storage)
            body = stringResource(R.string.home_focus_storage_desc)
            accent = p.warn
            icon = Icons.Rounded.Storage
            route = MaxDestination.StorageDetail.route
        }
        HomeFocusKind.MEMORY -> {
            title = stringResource(R.string.home_focus_memory)
            body = stringResource(R.string.home_focus_memory_desc)
            accent = p.accentAlt
            icon = Icons.Rounded.Speed
            route = MaxDestination.ZramManager.route
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(maxRadius(MaxRadius.row)))
            .background(accent.copy(alpha = MaxAlpha.toneWash))
            .border(MaxSize.hairlineBorder, accent.copy(alpha = MaxAlpha.border), RoundedCornerShape(maxRadius(MaxRadius.row)))
            .neuralClickable({ onOpen(route) }, role = Role.Button)
            .padding(MaxSpace.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NeuralIconChip(icon, accent, size = MaxSize.rowIconContainer)
        Spacer(Modifier.width(MaxSpace.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                color = p.text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            Text(
                body,
                color = p.muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (kind == HomeFocusKind.STORAGE) {
            Spacer(Modifier.width(MaxSpace.sm))
            NeuralPill(
                text = stringResource(R.string.home_focus_hide),
                accent = p.muted,
                compact = true,
                onClick = onHideStorage,
            )
        }
    }
}

/** The dock is the only horizontal movement: it is a shelf of doors, not another list page. */
@Composable
private fun CommandDock(
    entries: List<HomeDeckEntry>,
    onOpen: (HomeDeckEntry) -> Unit,
    onConfigure: () -> Unit,
) {
    val p = neuralPalette()
    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.home_quick_actions),
                    color = p.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    stringResource(R.string.home_quick_actions_desc),
                    color = p.muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                )
            }
            IconButton(onClick = onConfigure) {
                Icon(
                    Icons.Rounded.Settings,
                    stringResource(R.string.home_deck_settings_cd),
                    Modifier.size(MaxSize.iconGlyph),
                    tint = p.muted,
                )
            }
        }
        if (entries.isEmpty()) {
            Text(
                stringResource(R.string.home_deck_empty),
                color = p.muted,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
            ) {
                items(entries, key = { it.key }) { entry ->
                    DockTile(entry = entry, onClick = { onOpen(entry) })
                }
            }
        }
    }
}

@Composable
private fun DockTile(entry: HomeDeckEntry, onClick: () -> Unit) {
    val p = neuralPalette()
    val accent = entry.tone.content()
    val shape = RoundedCornerShape(maxRadius(MaxRadius.group))
    Column(
        modifier = Modifier
            .width(MaxSpace.xxl * 4)
            .height(MaxSpace.xxl * 4)
            .clip(shape)
            .background(p.tile)
            .border(MaxSize.hairlineBorder, accent.copy(alpha = MaxAlpha.border), shape)
            .neuralClickable(onClick, role = Role.Button)
            .padding(MaxSpace.md),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        NeuralIconChip(entry.icon, accent, size = MaxSize.rowIconContainer)
        Text(
            stringResource(entry.titleRes),
            color = p.text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
