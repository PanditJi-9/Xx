package nd.max.ui.subscreens

import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewSidebar
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.core.platform.SessionCause
import nd.max.core.platform.SessionInsight
import nd.max.core.platform.SessionVerdict
import nd.max.core.platform.StoredGameSession
import nd.max.service.FpsOverlayService
import nd.max.ui.component.AppIconImage
import nd.max.ui.component.LobbyPalette
import nd.max.ui.component.NeuralSparkline
import nd.max.ui.design.MaxAlpha
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius
import nd.max.ui.util.AppConfig
import nd.max.ui.util.customizedFieldCount
import kotlin.math.roundToInt

/**
 * «الساحة»: أوّل قسم في مركز التحكّم، وأقرب شيء إلى «ماذا أفتح الآن؟».
 *
 * ليست مخزنًا ثانيًا ولا كاتبًا ثانيًا: تقرأ ملف اللعبة، وتقرأ سجلّ الجلسات المحفوظ، وتفتح
 * الشاشات القائمة. كل كتابة تبقى في مالكها ([AppConfig] عبر `onUpdate`، ومقبض اللوحة عبر
 * `onPanelEnabled`). ويد التحكم تُقرأ من [InputDevice] فقط — لا إعداد لها من هنا.
 */
@Composable
internal fun LobbyArena(
    app: GameApp?,
    config: AppConfig,
    panelEnabled: Boolean,
    onPanelEnabled: (Boolean) -> Unit,
    sessions: List<StoredGameSession>,
    insight: SessionInsight?,
    accent: Color,
    onSessions: () -> Unit,
    onHud: () -> Unit,
    onThermal: () -> Unit,
    onDisplay: () -> Unit,
    onGameProfile: () -> Unit,
) {
    val controllerName = rememberControllerName()
    val hudRunning by produceState(initialValue = FpsOverlayService.isRunning) {
        while (true) {
            value = FpsOverlayService.isRunning
            delay(1500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaxSpace.xl, vertical = MaxSpace.lg),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
            Text(
                text = stringResource(R.string.lobby_cc_arena),
                color = LobbyPalette.Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.lobby_arena_scope),
                color = LobbyPalette.Muted,
                fontSize = 12.sp
            )
        }

        if (app == null) {
            ArenaNotice(stringResource(R.string.lobby_arena_no_game))
        } else {
            ArenaHero(
                app = app,
                config = config,
                panelEnabled = panelEnabled,
                controllerName = controllerName,
                accent = accent
            )
            ArenaSessionCard(
                session = sessions.firstOrNull(),
                insight = insight,
                sessionCount = sessions.size,
                accent = accent,
                onSessions = onSessions
            )

            Text(
                text = stringResource(R.string.lobby_arena_tools),
                color = LobbyPalette.Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
                ArenaToolCard(
                    icon = Icons.Rounded.History,
                    title = stringResource(R.string.lobby_arena_sessions),
                    detail = if (sessions.isEmpty()) {
                        stringResource(R.string.lobby_arena_sessions_none)
                    } else {
                        stringResource(R.string.lobby_arena_sessions_detail, sessions.size)
                    },
                    accent = LobbyPalette.Cyan,
                    onClick = onSessions,
                    modifier = Modifier.weight(1f)
                )
                ArenaToolCard(
                    icon = Icons.Rounded.Speed,
                    title = stringResource(R.string.lobby_arena_hud),
                    detail = stringResource(
                        if (hudRunning) R.string.lobby_arena_hud_on else R.string.lobby_arena_hud_off
                    ),
                    accent = if (hudRunning) LobbyPalette.Positive else LobbyPalette.Muted,
                    onClick = onHud,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
                ArenaToggleCard(
                    icon = Icons.Rounded.ViewSidebar,
                    title = stringResource(R.string.lobby_arena_panel),
                    detail = stringResource(
                        if (panelEnabled) R.string.lobby_arena_panel_armed else R.string.lobby_arena_panel_off
                    ),
                    checked = panelEnabled,
                    onChange = onPanelEnabled,
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
                ArenaToolCard(
                    icon = Icons.Rounded.Thermostat,
                    title = stringResource(R.string.lobby_arena_thermal),
                    detail = stringResource(R.string.lobby_arena_thermal_detail),
                    accent = LobbyPalette.Caution,
                    onClick = onThermal,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
                ArenaToolCard(
                    icon = Icons.Rounded.Speed,
                    title = stringResource(R.string.lobby_arena_display),
                    detail = stringResource(R.string.lobby_arena_display_detail),
                    accent = LobbyPalette.BlueBright,
                    onClick = onDisplay,
                    modifier = Modifier.weight(1f)
                )
                ArenaToolCard(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.lobby_arena_profile),
                    detail = stringResource(R.string.lobby_arena_profile_detail),
                    accent = accent,
                    onClick = onGameProfile,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = stringResource(R.string.lobby_arena_note),
                color = LobbyPalette.Muted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ArenaHero(
    app: GameApp,
    config: AppConfig,
    panelEnabled: Boolean,
    controllerName: String?,
    accent: Color,
) {
    val tweaks = remember(config) { config.customizedFieldCount() }
    val shape = remember { RoundedCornerShape(maxRadius(MaxRadius.group)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = MaxAlpha.toneContainerStrong), LobbyPalette.Panel.copy(alpha = .55f))
                )
            )
            .border(MaxSize.hairlineBorder, accent.copy(alpha = MaxAlpha.borderStrong), shape)
            .padding(MaxSpace.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        Box(
            modifier = Modifier
                .size(MaxSize.iconContainer + MaxSpace.lg)
                .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
                .background(accent.copy(alpha = MaxAlpha.toneContainer)),
            contentAlignment = Alignment.Center
        ) {
            AppIconImage(packageName = app.packageName, size = MaxSize.iconContainer, contentDescription = null)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            Text(
                text = app.label,
                color = LobbyPalette.Ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (tweaks > 0) {
                    stringResource(R.string.lobby_arena_tweaks, tweaks)
                } else {
                    stringResource(R.string.lobby_arena_defaults)
                },
                color = LobbyPalette.Steel,
                fontSize = 12.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Gamepad,
                    contentDescription = null,
                    tint = if (controllerName != null) LobbyPalette.Cyan else LobbyPalette.Muted,
                    modifier = Modifier.size(MaxSize.iconGlyphSmall)
                )
                Text(
                    text = if (controllerName != null) {
                        stringResource(R.string.lobby_arena_controller_ready, controllerName)
                    } else {
                        stringResource(R.string.lobby_arena_controller_none)
                    },
                    color = LobbyPalette.Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ArenaStatusChip(
                text = stringResource(
                    if (panelEnabled) R.string.lobby_arena_panel_armed else R.string.lobby_arena_panel_off
                ),
                tone = if (panelEnabled) LobbyPalette.Cyan else LobbyPalette.Muted
            )
        }
    }
}

@Composable
private fun ArenaSessionCard(
    session: StoredGameSession?,
    insight: SessionInsight?,
    sessionCount: Int,
    accent: Color,
    onSessions: () -> Unit,
) {
    val shape = remember { RoundedCornerShape(maxRadius(MaxRadius.group)) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LobbyPalette.Panel.copy(alpha = .50f))
            .border(MaxSize.hairlineBorder, LobbyPalette.Hairline, shape)
            .clickable(role = Role.Button, onClick = onSessions)
            .padding(MaxSpace.lg),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
        ) {
            Text(
                text = stringResource(R.string.lobby_arena_last_session),
                color = LobbyPalette.Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.lobby_arena_sessions_detail, sessionCount),
                color = LobbyPalette.Muted,
                fontSize = 11.sp
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = LobbyPalette.Muted,
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
        }

        if (session == null) {
            Text(
                text = stringResource(R.string.lobby_arena_no_session),
                color = LobbyPalette.Muted,
                fontSize = 13.sp
            )
            Text(
                text = stringResource(R.string.lobby_arena_record),
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
            ) {
                Text(
                    text = session.framesAverage?.let {
                        stringResource(R.string.lobby_arena_fps, it)
                    } ?: "—",
                    color = LobbyPalette.Ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (insight != null) {
                    val tone = verdictTone(insight.verdict)
                    ArenaStatusChip(text = stringResource(verdictText(insight.verdict)), tone = tone)
                }
            }

            val frames = remember(session.id) { session.series.mapNotNull { it.frames } }
            if (frames.size >= 2) {
                NeuralSparkline(
                    samples = frames,
                    accent = verdictTone(insight?.verdict ?: SessionVerdict.FirstRun),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ArenaSparklineHeight)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
                session.heatPeak?.let {
                    ArenaMetric(stringResource(R.string.lobby_arena_temp, it.roundToInt()))
                }
                session.jankAverage?.let {
                    ArenaMetric(stringResource(R.string.lobby_arena_jank, it))
                }
                session.spanMs.takeIf { it >= 60_000L }?.let {
                    ArenaMetric(stringResource(R.string.lobby_arena_minutes, (it / 60_000L).toInt()))
                }
            }

            val cause = insight?.cause
            if (cause != null && cause != SessionCause.None) {
                Text(
                    text = stringResource(causeText(cause)),
                    color = LobbyPalette.Caution,
                    fontSize = 12.sp
                )
            }
            val note = when {
                insight == null -> null
                insight.baselineCount >= 2 ->
                    stringResource(R.string.game_sessions_insight_note, insight.baselineCount)
                else -> stringResource(R.string.lobby_arena_last_session_note, sessionCount)
            }
            if (note != null) {
                Text(text = note, color = LobbyPalette.Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ArenaMetric(text: String) {
    Text(text = text, color = LobbyPalette.Steel, fontSize = 12.sp)
}

@Composable
private fun ArenaStatusChip(text: String, tone: Color) {
    val shape = remember { RoundedCornerShape(maxRadius(MaxRadius.pill)) }
    Row(
        modifier = Modifier
            .clip(shape)
            .background(tone.copy(alpha = MaxAlpha.toneContainer))
            .border(MaxSize.hairlineBorder, tone.copy(alpha = MaxAlpha.borderStrong), shape)
            .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Box(Modifier.size(MaxSpace.xs).clip(CircleShape).background(tone))
        Text(
            text = text,
            color = tone,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun ArenaToolCard(
    icon: ImageVector,
    title: String,
    detail: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = remember { RoundedCornerShape(maxRadius(MaxRadius.tile)) }
    Row(
        modifier = modifier
            .heightIn(min = ArenaToolMinHeight)
            .clip(shape)
            .background(LobbyPalette.PanelRaised.copy(alpha = .45f))
            .border(MaxSize.hairlineBorder, LobbyPalette.Hairline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(MaxSpace.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        ArenaToolIcon(icon = icon, accent = accent)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            Text(
                text = title,
                color = LobbyPalette.Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                color = LobbyPalette.Muted,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = LobbyPalette.Muted,
            modifier = Modifier.size(MaxSize.iconGlyphSmall)
        )
    }
}

@Composable
private fun ArenaToggleCard(
    icon: ImageVector,
    title: String,
    detail: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val shape = remember { RoundedCornerShape(maxRadius(MaxRadius.tile)) }
    Row(
        modifier = modifier
            .heightIn(min = ArenaToolMinHeight)
            .clip(shape)
            .background(LobbyPalette.PanelRaised.copy(alpha = .45f))
            .border(
                MaxSize.hairlineBorder,
                if (checked) accent.copy(alpha = MaxAlpha.borderStrong) else LobbyPalette.Hairline,
                shape
            )
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(MaxSpace.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        ArenaToolIcon(icon = icon, accent = accent)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            Text(
                text = title,
                color = LobbyPalette.Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                color = LobbyPalette.Muted,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LobbyPalette.Red,
                uncheckedTrackColor = LobbyPalette.PanelRaised,
                uncheckedThumbColor = LobbyPalette.Muted
            )
        )
    }
}

@Composable
private fun ArenaToolIcon(icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier
            .size(MaxSize.rowIconContainer)
            .clip(RoundedCornerShape(maxRadius(MaxRadius.chip)))
            .background(accent.copy(alpha = MaxAlpha.toneContainer)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(MaxSize.iconGlyph)
        )
    }
}

@Composable
private fun ArenaNotice(message: String) {
    val shape = remember { RoundedCornerShape(maxRadius(MaxRadius.group)) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LobbyPalette.Panel.copy(alpha = .50f))
            .border(MaxSize.hairlineBorder, LobbyPalette.Hairline, shape)
            .padding(MaxSpace.lg)
    ) {
        Text(text = message, color = LobbyPalette.Muted, fontSize = 13.sp)
    }
}

/**
 * يد التحكم المقروءة الآن. المستمع يعيد المسح عند وصلها أو فصلها، فالحالة لا تبقى قديمة
 * بعد إعادة توصيلها واللوبي مفتوح. لا شيء يُكتب هنا.
 */
@Composable
private fun rememberControllerName(): String? {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(InputManager::class.java) }
    var name by remember { mutableStateOf(scanControllerName()) }

    DisposableEffect(manager) {
        if (manager == null) {
            onDispose { }
        } else {
            val handler = Handler(Looper.getMainLooper())
            val listener = object : InputManager.InputDeviceListener {
                override fun onInputDeviceAdded(deviceId: Int) {
                    name = scanControllerName()
                }

                override fun onInputDeviceRemoved(deviceId: Int) {
                    name = scanControllerName()
                }

                override fun onInputDeviceChanged(deviceId: Int) {
                    name = scanControllerName()
                }
            }
            manager.registerInputDeviceListener(listener, handler)
            onDispose { manager.unregisterInputDeviceListener(listener) }
        }
    }
    return name
}

private fun scanControllerName(): String? = InputDevice.getDeviceIds()
    .asSequence()
    .mapNotNull { InputDevice.getDevice(it) }
    .filter { device ->
        !device.isVirtual &&
            (device.sources and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK)) != 0
    }
    .mapNotNull { device -> device.name?.takeIf { it.isNotBlank() } }
    .firstOrNull()

internal fun verdictTone(verdict: SessionVerdict): Color = when (verdict) {
    SessionVerdict.Better -> LobbyPalette.Positive
    SessionVerdict.Similar -> LobbyPalette.Muted
    SessionVerdict.Worse, SessionVerdict.Unusable -> LobbyPalette.Caution
    SessionVerdict.FirstRun -> LobbyPalette.Cyan
}

internal fun verdictText(verdict: SessionVerdict): Int = when (verdict) {
    SessionVerdict.Better -> R.string.game_sessions_verdict_better
    SessionVerdict.Similar -> R.string.game_sessions_verdict_similar
    SessionVerdict.Worse -> R.string.game_sessions_verdict_worse
    SessionVerdict.FirstRun -> R.string.game_sessions_verdict_first
    SessionVerdict.Unusable -> R.string.game_sessions_verdict_unusable
}

private fun causeText(cause: SessionCause): Int = when (cause) {
    SessionCause.Thermal -> R.string.session_cause_thermal
    SessionCause.Jank -> R.string.session_cause_jank
    SessionCause.Energy -> R.string.session_cause_energy
    SessionCause.PowerSave -> R.string.session_cause_power_save
    SessionCause.Refresh -> R.string.session_cause_refresh
    SessionCause.Renderer -> R.string.session_cause_renderer
    SessionCause.None -> 0
}

private val ArenaSparklineHeight = MaxSize.sparklineHeight * 2
private val ArenaToolMinHeight = MaxSize.minTouchTarget + MaxSpace.xxl
