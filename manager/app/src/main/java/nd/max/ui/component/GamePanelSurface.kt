package nd.max.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nd.max.R
import nd.max.ui.overlay.HandleFx
import nd.max.core.gamespace.GamePanelMode
import nd.max.core.gamespace.BypassState
import nd.max.core.gamespace.PanelClocks
import nd.max.core.gamespace.PanelControlState
import nd.max.core.gamespace.PanelSide
import nd.max.core.gamespace.ThermalPanelState
import nd.max.core.platform.HudField
import nd.max.core.platform.HudReading
import nd.max.core.platform.HudTally
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius

private val PanelGraph = 40.dp
private const val RING_SPAN = 270f
private const val FPS_REFERENCE_HZ = 120

internal val PanelInk = Color(0xFFF1F3F5)
internal val PanelMuted = Color(0xFF98A2AA)
internal val PanelTile = Color(0x14FFFFFF)
internal val PanelDanger = Color(0xFFFF4D5E)

/**
 * لوحة اللعبة: مقبض صغير مطويّ، ولوحة قيادة مفتوحة (حلقتا إطارات/معالج + قياسات + ثلاث أدوات
 * تعمل فعلًا). كل رقم من `HudSampler` الحقيقي، ولا أداة لا تعمل (لا لقطة، لا DND وهميّ).
 * المحتوى LTR دائمًا لأن الموضع فيزيائي (`PanelSide`) لا يتبع اتجاه اللغة.
 */
@Composable
fun GamePanelSurface(
    mode: GamePanelMode,
    gameLabel: String,
    reading: HudReading?,
    tally: HudTally?,
    fields: List<HudField>,
    accent: Color,
    side: PanelSide,
    recording: Boolean,
    refreshRateHz: Int?,
    clocks: PanelClocks,
    controls: PanelControlState,
    frames: List<Float>,
    onOpen: () -> Unit,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    onCycleRefresh: () -> Unit,
    onToggleRecording: () -> Unit,
    onOpenControls: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onToggleBypass: () -> Unit,
    thermal: ThermalPanelState = ThermalPanelState(),
    onSelectThermal: (String) -> Unit = {},
    refreshChoices: List<Int> = emptyList(),
    onSelectRefresh: (Int?) -> Unit = {},
    handleFx: HandleFx? = null,
    handleScale: Float = 1f,
    crosshairEnabled: Boolean = false,
    onToggleCrosshair: () -> Unit = {},
    origin: Offset = Offset(1f, 0.5f)
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        when (mode) {
            GamePanelMode.Hidden -> Unit
            GamePanelMode.Handle -> {
                val hot = (reading?.heat ?: 0f) >= HEAT_WARN_C
                GamePanelHandle(
                    side = side,
                    gameLabel = gameLabel,
                    accent = ThermalTint.of(thermal) ?: accent,
                    hot = hot,
                    fx = handleFx,
                    scale = handleScale,
                    onOpen = onOpen
                )
            }
            GamePanelMode.Open -> GamePanelCockpit(
                gameLabel = gameLabel,
                reading = reading,
                tally = tally,
                fields = fields,
                accent = accent,
                side = side,
                recording = recording,
                refreshRateHz = refreshRateHz,
                clocks = clocks,
                controls = controls,
                frames = frames,
                onCollapse = onCollapse,
                onClose = onClose,
                onCycleRefresh = onCycleRefresh,
                onToggleRecording = onToggleRecording,
                onOpenControls = onOpenControls,
                onSelectProfile = onSelectProfile,
                onToggleBypass = onToggleBypass,
                crosshairEnabled = crosshairEnabled,
                onToggleCrosshair = onToggleCrosshair,
                thermal = thermal,
                onSelectThermal = onSelectThermal,
                refreshChoices = refreshChoices,
                onSelectRefresh = onSelectRefresh,
                origin = origin
            )
        }
    }
}

/** اسم اللعبة المقروء بدل اسم الحزمة؛ إن لم يُقرأ يبقى اسم الحزمة. */
@Composable
internal fun rememberGameTitle(packageName: String): String {
    val context = LocalContext.current
    return remember(packageName) {
        @Suppress("DEPRECATION")
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: packageName
    }
}

@Composable
/** قياسات ثانوية بحسب الحقول التي اختارها المستخدم في طبقة FPS؛ كل واحدة من قراءة فعلية. */
@Composable
internal fun PanelReadings(reading: HudReading?, fields: List<HudField>) {
    val items = buildList {
        if (HudField.Cpu in fields) {
            add(stringResource(R.string.game_panel_chip_load) to (reading?.cpu?.let { "$it%" } ?: MAX_VALUE_UNAVAILABLE))
        }
        if (HudField.Ram in fields) {
            add(stringResource(R.string.game_panel_chip_ram) to (reading?.ramMb?.let(::ramText) ?: MAX_VALUE_UNAVAILABLE))
        }
        if (HudField.Power in fields) {
            add(stringResource(R.string.game_panel_chip_power) to (reading?.watt?.let { "%.1f W".format(it) } ?: MAX_VALUE_UNAVAILABLE))
        }
        if (HudField.Heat in fields) {
            add(stringResource(R.string.game_panel_chip_heat) to (reading?.heat?.let { "%.0f°C".format(it) } ?: MAX_VALUE_UNAVAILABLE))
        }
        if (HudField.Renderer in fields) {
            add(stringResource(R.string.game_panel_chip_renderer) to (reading?.renderer ?: MAX_VALUE_UNAVAILABLE))
        }
    }
    if (items.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
        items.forEach { (label, value) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(PanelTile, RoundedCornerShape(maxRadius(MaxRadius.control)))
                    .padding(vertical = MaxSpace.xs, horizontal = MaxSpace.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = label, color = PanelMuted, fontSize = 9.sp, maxLines = 1)
                Text(
                    text = value,
                    color = PanelInk,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun ramText(mb: Int): String = if (mb >= 1024) "%.1f GB".format(mb / 1024f) else "$mb MB"

@Composable
internal fun GamePanelSession(tally: HudTally?) {
    if (tally == null || tally.isEmpty) {
        Text(text = stringResource(R.string.game_panel_no_session), color = PanelMuted, fontSize = 10.sp)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
        PanelStat(stringResource(R.string.game_panel_stat_samples), tally.samples.toString())
        PanelStat(stringResource(R.string.game_panel_stat_average), tally.framesAverage?.let { "%.0f".format(it) } ?: MAX_VALUE_UNAVAILABLE)
        PanelStat(stringResource(R.string.game_panel_stat_low), tally.framesLow?.let { "%.0f".format(it) } ?: MAX_VALUE_UNAVAILABLE)
        PanelStat(stringResource(R.string.game_panel_stat_heat), tally.heatPeak?.let { "%.0f°C".format(it) } ?: MAX_VALUE_UNAVAILABLE)
    }
    if (tally.droppedSamples > 0) {
        Text(text = stringResource(R.string.game_panel_dropped, tally.droppedSamples), color = PanelMuted, fontSize = 9.sp)
    }
}

@Composable
private fun PanelStat(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)) {
        Text(text = label, color = PanelMuted, fontSize = 9.sp, maxLines = 1)
        Text(
            text = value,
            color = PanelInk,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
internal fun PanelButton(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = PanelInk) {
    Box(
        modifier = Modifier
            .size(PanelTouch)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(MaxSpace.xl))
    }
}

/** ثلاثة أوضاع أداء بنقرة؛ في وضع الذكاء تُعرض الشارة فقط لأن الذكاء هو المالك. */
@Composable
/** منحنى الإطارات الحيّ من تاريخ `HudSampler`؛ أقل من نقطتين ⇒ انتظار، لا خط مرسوم من العدم. */
@Composable
internal fun FrameGraph(history: List<Float>, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PanelGraph)
            .background(PanelTile, RoundedCornerShape(maxRadius(MaxRadius.control)))
            .padding(MaxSpace.xs),
        contentAlignment = Alignment.Center
    ) {
        if (history.size < 2) {
            Text(text = stringResource(R.string.game_panel_graph_empty), color = PanelMuted, fontSize = 10.sp)
            return@Box
        }
        Canvas(Modifier.fillMaxSize()) {
            val top = maxOf(history.max(), 30f) * 1.1f
            val dx = this.size.width / (history.size - 1)
            val line = Path()
            val fill = Path()
            history.forEachIndexed { i, v ->
                val x = i * dx
                val y = this.size.height * (1f - (v / top).coerceIn(0f, 1f))
                if (i == 0) {
                    line.moveTo(x, y)
                    fill.moveTo(x, this.size.height)
                    fill.lineTo(x, y)
                } else {
                    line.lineTo(x, y)
                    fill.lineTo(x, y)
                }
            }
            fill.lineTo(this.size.width, this.size.height)
            fill.close()
            drawPath(fill, Brush.verticalGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent)))
            drawPath(line, accent, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}
