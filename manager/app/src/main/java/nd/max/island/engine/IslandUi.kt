/*
 * Ported from Islandify (https://github.com/bughunter11/Islandify) — MIT License,
 * Copyright (c) 2026 bughunter11. Full license text: THIRD_PARTY_NOTICES.md.
 * Adapted for MaxManager (packages, resources, integration). Modifications (C) 2026 Nader Magdy.
 */
package nd.max.island.engine

import nd.max.R
import nd.max.island.core.IslandSettings
import nd.max.island.core.KindStyle
import nd.max.island.core.SwipeDir
import nd.max.island.core.kindOf
import nd.max.island.core.seedScheme
import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

internal val Green = Color(0xFF30D158)
internal val Orange = Color(0xFFFF9F0A)
internal val Red = Color(0xFFFF453A)
internal val Track = Color(0xFF2C2C2E)
internal val Muted = Color(0xFF8E8E93)
internal val LiveBlue = Color(0xFF0A84FF)
internal val DndPurple = Color(0xFF7D5CFF)

internal fun IslandMode.Toggle.icon(): ImageVector = when (kind) {
    ToggleKind.Silent -> if (on) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp
    ToggleKind.Vibrate -> Icons.Rounded.Vibration
    ToggleKind.Dnd -> if (on) Icons.Rounded.DoNotDisturbOn else Icons.Rounded.NotificationsActive
    ToggleKind.Flashlight -> if (on) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff
}

// نصوصها من الموارد (كانت إنجليزية صلبة في الأصل المنقول) فتُترجم مع الواجهة.
@Composable
internal fun IslandMode.Toggle.title(): String = when (kind) {
    ToggleKind.Silent -> stringResource(if (on) R.string.ify_toggle_silent else R.string.ify_toggle_sound_on)
    ToggleKind.Vibrate -> stringResource(R.string.ify_toggle_vibrate)
    ToggleKind.Dnd -> stringResource(if (on) R.string.ify_toggle_dnd else R.string.ify_toggle_dnd_off)
    ToggleKind.Flashlight -> stringResource(if (on) R.string.ify_toggle_flash_on else R.string.ify_toggle_flash_off)
}

@Composable
internal fun IslandMode.Toggle.subtitle(): String = when (kind) {
    ToggleKind.Silent -> stringResource(if (on) R.string.ify_toggle_sub_silent else R.string.ify_toggle_sub_sound)
    ToggleKind.Vibrate -> stringResource(R.string.ify_toggle_sub_vibrate)
    ToggleKind.Dnd -> stringResource(if (on) R.string.ify_toggle_sub_dnd_on else R.string.ify_toggle_sub_dnd_off)
    ToggleKind.Flashlight -> stringResource(if (on) R.string.ify_toggle_sub_torch_on else R.string.ify_toggle_sub_torch_off)
}

internal fun IslandMode.Toggle.tint(): Color = when (kind) {
    ToggleKind.Silent, ToggleKind.Vibrate -> if (on) Orange else Green
    ToggleKind.Dnd -> if (on) DndPurple else Green
    ToggleKind.Flashlight -> if (on) Color(0xFFFFD60A) else Muted
}

internal fun IslandMode.Live.icon(): ImageVector {
    if (kind == LiveKind.Navigation) return Icons.Rounded.Navigation
    if (kind == LiveKind.Download) return Icons.Rounded.Download
    val a = app.lowercase()
    val ride = listOf("uber", "ola", "rapido", "taxi", "cab").any { a.contains(it) }
    return if (ride) Icons.Rounded.LocalTaxi else Icons.Rounded.DeliveryDining
}

@Composable
fun IslandTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val source by IslandSettings.accentSource.collectAsState()
    val seed by IslandSettings.accentSeed.collectAsState()
    val scheme = if (source == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        dynamicDarkColorScheme(ctx) else seedScheme(seed, true)
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Callbacks for the buttons inside the island, kept in one place. */
internal class IslandActions(
    val onPrev: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onAnswer: () -> Unit,
    val onDecline: () -> Unit,
    val onMarkRead: () -> Unit,
    val onReply: () -> Unit,
    val onSendReply: (String) -> Unit,
    val onCancelReply: () -> Unit,
    val replying: Boolean,
    val onOpenMedia: () -> Unit,
    val queued: Int,
)

/**
 * [scale] resizes the whole island (dp and sp together).
 *
 * Gestures (all handled in one pointerInput below):
 *  - tap                    -> onClick (compact <-> card)
 *  - long-press             -> onLongPress (large card)
 *  - swipe up               -> onDismiss
 */
@Composable
fun DynamicIslandUi(
    mode: IslandMode,
    level: IslandLevel,
    scale: Float = 1f,
    widthScale: Float = 1f,
    heightScale: Float = 1f,
    cornerFactor: Float = 1f,
    animSpeed: Float = 1f,
    glow: Boolean = true,
    haptics: Boolean = true,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onPrev: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onAnswer: () -> Unit = {},
    onDecline: () -> Unit = {},
    onMarkRead: () -> Unit = {},
    onReply: () -> Unit = {},
    onSendReply: (String) -> Unit = {},
    onCancelReply: () -> Unit = {},
    replying: Boolean = false,
    onSwipeSide: (Int) -> Unit = {},
    appAccent: Color? = null,
    onOpenMedia: () -> Unit = {},
    secondary: IslandMode.Media? = null,   // music next to a timer / navigation
    queued: Int = 0,                       // banners waiting behind the current one
    onSwipe: ((SwipeDir) -> Unit)? = null, // null = old behaviour (up = onDismiss, left / right = onSwipeSide)
) {
    val base = LocalDensity.current
    // Text size of this kind (banners scale their own text, cards / pills scale the font here)
    val kindStyles by IslandSettings.kindStyles.collectAsState()
    val textMul = if (level == IslandLevel.Banner) 1f else (kindOf(mode)?.let { kindStyles[it.key]?.text } ?: 1f)
    val actions = IslandActions(
        onPrev, onPlayPause, onNext, onSeek, onAnswer, onDecline,
        onMarkRead, onReply, onSendReply, onCancelReply, replying, onOpenMedia, queued
    )
    CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale * textMul)) {
        IslandBody(
            mode, level, widthScale, heightScale, cornerFactor, animSpeed, glow, haptics,
            onClick, onLongPress, onDismiss, actions,
            onSwipeSide, appAccent, secondary, queued, onSwipe
        )
    }
}

@Composable
internal fun IslandBody(
    mode: IslandMode,
    level: IslandLevel,
    widthScale: Float,
    heightScale: Float,
    cornerFactor: Float,
    animSpeed: Float,
    glow: Boolean,
    haptics: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDismiss: () -> Unit,
    actions: IslandActions,
    onSwipeSide: (Int) -> Unit,
    appAccent: Color?,
    secondary: IslandMode.Media?,
    queued: Int,
    onSwipe: ((SwipeDir) -> Unit)?,
) {
    val lvl = level
    val kindStyles by IslandSettings.kindStyles.collectAsState()
    // Each banner type (message, charging, unplugged, ...) has its own size
    val kind = kindOf(mode)
    val bs = kind?.let { kindStyles[it.key] } ?: KindStyle()
    val isPopup = kind?.popup == true
    // Music is only the second part while a timer / navigation is the main thing
    val sec = if (mode is IslandMode.Live || mode is IslandMode.Timer) secondary else null
    val haptic = LocalHapticFeedback.current
    val hapticOn by rememberUpdatedState(haptics)

    val dClick by rememberUpdatedState(onClick)
    val dLong by rememberUpdatedState(onLongPress)
    val dDismiss by rememberUpdatedState(onDismiss)
    val dSide by rememberUpdatedState(onSwipeSide)
    val dSwipe by rememberUpdatedState(onSwipe)

    // Light haptic on expand / collapse (not on the first composition)
    var firstLevel by remember { mutableStateOf(true) }
    LaunchedEffect(lvl) {
        if (firstLevel) firstLevel = false
        else if (hapticOn) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val baseW = when (lvl) {
        IslandLevel.Compact -> when (mode) {
            IslandMode.Idle -> 118.dp
            is IslandMode.Charging -> 150.dp
            is IslandMode.Timer -> if (sec != null) 214.dp else 160.dp
            is IslandMode.Notification -> 224.dp
            is IslandMode.Media -> 224.dp
            is IslandMode.Call -> 210.dp
            is IslandMode.Device -> 200.dp
            is IslandMode.LowBattery -> 150.dp
            is IslandMode.Toggle -> 190.dp
            is IslandMode.Live -> if (sec != null) 256.dp else 224.dp
        }
        IslandLevel.Banner -> bannerWidth(mode)
        else -> 368.dp
    }
    val baseH = when (lvl) {
        IslandLevel.Compact -> 38.dp
        IslandLevel.Banner -> 48.dp
        IslandLevel.Expanded -> expandedHeight(mode)
        IslandLevel.Large -> when (mode) {
            is IslandMode.Media -> 236.dp
            is IslandMode.Notification -> if (mode.canReply || mode.canMarkRead) 224.dp else 200.dp
            is IslandMode.Live -> 168.dp
            else -> expandedHeight(mode)
        }
    }

    // Never wider than the screen (density is already scaled, so derive dp from it)
    val screenPx = LocalContext.current.resources.displayMetrics.widthPixels
    val maxW = (screenPx / LocalDensity.current.density).dp - 8.dp
    val targetW = (baseW * (when (lvl) {
        IslandLevel.Banner -> bs.w
        IslandLevel.Compact -> widthScale * (if (isPopup) 1f else bs.w)
        else -> widthScale * bs.cardW
    })).coerceAtMost(maxW)
    // The small pill keeps a sane height; the height slider mainly shapes the cards
    // The mini player adds a row under the card
    val miniExtra = if (sec != null && (lvl == IslandLevel.Expanded || lvl == IslandLevel.Large)) 52.dp else 0.dp
    val targetH = (baseH + miniExtra) * (when (lvl) {
        IslandLevel.Compact -> heightScale.coerceIn(0.8f, 1.2f) * (if (isPopup) 1f else bs.h)
        IslandLevel.Banner -> bs.h   // own height for each banner type
        else -> heightScale * bs.cardH
    })

    // Higher animSpeed = faster spring
    val spec = spring<Dp>(
        dampingRatio = 0.8f,
        stiffness = (Spring.StiffnessMedium * 1.6f * animSpeed).coerceIn(300f, 6000f)
    )
    val width by animateDpAsState(targetW, spec, label = "w")
    val height by animateDpAsState(targetH, spec, label = "h")
    val cf = if (bs.corner >= 0f) bs.corner else cornerFactor
    val corner = (height / 2 * cf).coerceIn(12.dp, 46.dp)
    val shape = RoundedCornerShape(corner)

    // Glow: accent color from the album art / current mode
    val glowTarget: Color = if (!glow) Color.Transparent else when (mode) {
        is IslandMode.Media -> appAccent ?: mode.accent?.let { Color(it) } ?: Color.Transparent
        is IslandMode.Charging -> Green
        is IslandMode.Call -> Green
        is IslandMode.Timer -> Orange
        is IslandMode.LowBattery -> Red
        is IslandMode.Live -> LiveBlue
        is IslandMode.Toggle -> mode.tint()
        else -> Color.Transparent
    }
    val glowColor by animateColorAsState(glowTarget, tween(200), label = "glow")

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(Color.Black)
            .drawBehind {
                if (glowColor.alpha > 0f) {
                    drawRect(
                        Brush.radialGradient(
                            listOf(glowColor.copy(alpha = glowColor.alpha * 0.32f), Color.Transparent),
                            center = Offset(size.width * 0.16f, size.height * 0.5f),
                            radius = size.width * 0.75f
                        )
                    )
                }
            }
            .border(
                0.6.dp,
                if (glowColor.alpha > 0f) glowColor.copy(alpha = glowColor.alpha * 0.55f) else Color(0xFF1F1F21),
                shape
            )
            // One gesture handler for everything: tap, long-press and swipe.
            // (No separate clickable: two handlers fighting over the same touch is what made taps unreliable.)
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Default requireUnconsumed = true: a touch that starts on an inner button
                    // (prev / play / next / answer...) belongs to that button, not to us.
                    val down = awaitFirstDown()
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    val slop = viewConfiguration.touchSlop
                    var accum = Offset.Zero
                    var swipe = false
                    var tap = false

                    // Phase 1: watch until the long-press timeout.
                    // Finger moved = swipe, finger lifted = tap, still down = long-press.
                    val timedOut = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.id == down.id }
                            // Lost, or an inner button consumed it: not our gesture
                            if (ch == null || ch.isConsumed) return@withTimeoutOrNull
                            if (ch.changedToUpIgnoreConsumed()) {
                                tap = true
                                return@withTimeoutOrNull
                            }
                            accum += ch.positionChange()
                            tracker.addPosition(ch.uptimeMillis, ch.position)
                            if (accum.getDistance() > slop) {
                                swipe = true
                                return@withTimeoutOrNull
                            }
                        }
                    } == null

                    if (timedOut) {
                        // Long press = large card (no dragging)
                        if (hapticOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        dLong()
                        // Swallow the rest of the touch until the finger lifts
                        drag(down.id) { ch -> ch.consume() }
                        currentEvent.changes.forEach { it.consume() }
                    } else if (swipe) {
                        var dx = accum.x
                        var dy = accum.y
                        drag(down.id) { ch ->
                            val d = ch.positionChange()
                            dx += d.x
                            dy += d.y
                            tracker.addPosition(ch.uptimeMillis, ch.position)
                            ch.consume()
                        }
                        currentEvent.changes.forEach { it.consume() }
                        // FIX: the old rule needed a long drag (18dp, and it shrank with the island scale).
                        // Now a short quick flick counts too, and the distance is not tied to the scale.
                        val velo = tracker.calculateVelocity()
                        val vertical = abs(dy) >= abs(dx)
                        val dist = if (vertical) abs(dy) else abs(dx)
                        val speed = if (vertical) abs(velo.y) else abs(velo.x)
                        if (dist >= slop * 2f || speed >= 500f) {
                            val dir = if (vertical) (if (dy < 0) SwipeDir.Up else SwipeDir.Down)
                                      else (if (dx < 0) SwipeDir.Left else SwipeDir.Right)
                            val cb = dSwipe
                            if (cb != null) cb(dir)
                            else when (dir) {
                                SwipeDir.Up -> dDismiss()
                                SwipeDir.Left -> dSide(1)
                                SwipeDir.Right -> dSide(-1)
                                SwipeDir.Down -> Unit
                            }
                        }
                    } else if (tap) {
                        dClick()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            targetState = mode to lvl,
            // A timer ticking every second must not re-trigger the transition
            contentKey = { it.first::class to it.second },
            transitionSpec = {
                (fadeIn(tween(120)) + scaleIn(tween(140), initialScale = 0.9f)) togetherWith
                    fadeOut(tween(60))
            },
            contentAlignment = Alignment.Center,
            label = "content"
        ) { (m, l) ->
            if (l == IslandLevel.Compact) {
                Compact(m, queued, sec)
            } else if (l == IslandLevel.Banner) {
                Banner(m, queued)
            } else if (sec != null && (m is IslandMode.Live || m is IslandMode.Timer)) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) { Expanded(m, l, actions) }
                    MiniPlayer(sec, actions)
                }
            } else {
                Expanded(m, l, actions)
            }
        }
    }
}

internal fun expandedHeight(mode: IslandMode): Dp = when (mode) {
    IslandMode.Idle -> 96.dp
    is IslandMode.Charging -> 132.dp
    is IslandMode.Timer -> 132.dp
    is IslandMode.Notification -> if (mode.canReply || mode.canMarkRead) 156.dp else 136.dp
    is IslandMode.Media -> 156.dp
    is IslandMode.Call -> 120.dp
    is IslandMode.Device -> 108.dp
    is IslandMode.LowBattery -> 132.dp
    is IslandMode.Toggle -> 108.dp
    is IslandMode.Live -> 132.dp
}

internal fun IslandMode.Media.accentColor(fallback: Color): Color = accent?.let { Color(it) } ?: fallback

internal fun IslandMode.Media.currentPosition(): Long {
    if (!playing || updatedAt == 0L) return positionMs
    val p = positionMs + ((SystemClock.elapsedRealtime() - updatedAt) * speed).toLong()
    return if (durationMs > 0) p.coerceIn(0L, durationMs) else p
}

/* ------------------------------ COMPACT ------------------------------ */

/** Download ring: filled ring when the progress is known, spinner otherwise. */
@Composable
internal fun DownloadRing(progress: Int, size: Dp) {
    if (progress >= 0) {
        CircularProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.size(size),
            color = LiveBlue, trackColor = LiveBlue.copy(alpha = 0.18f),
            strokeWidth = 2.dp, strokeCap = StrokeCap.Round
        )
    } else {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = LiveBlue, trackColor = LiveBlue.copy(alpha = 0.18f),
            strokeWidth = 2.dp, strokeCap = StrokeCap.Round
        )
    }
}

@Composable
internal fun Compact(m: IslandMode, queued: Int, sec: IslandMode.Media?) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxSize().padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when (m) {
            IslandMode.Idle -> Unit
            is IslandMode.Charging -> {
                val c = if (m.connected) Green else Orange
                Icon(
                    if (m.connected) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                    null, tint = c, modifier = Modifier.size(20.dp)
                )
                Label("${m.percent}%", color = c, size = 14, weight = FontWeight.Bold)
            }
            is IslandMode.Notification -> {
                Avatar(m.app, 24.dp)
                Label(
                    m.title.ifBlank { m.app },
                    Modifier.weight(1f).padding(horizontal = 9.dp),
                    size = 13
                )
                if (queued > 0) CountBadge(queued, primary)
                else Box(Modifier.size(7.dp).clip(CircleShape).background(primary))
            }
            is IslandMode.Media -> {
                Art(m, 25.dp, 7.dp)
                Waveform(m.playing, m.accentColor(primary), Modifier.height(16.dp))
            }
            is IslandMode.Timer -> {
                if (sec == null) {
                    Icon(Icons.Rounded.Timer, null, tint = Orange, modifier = Modifier.size(20.dp))
                    Label(formatTime(m.remainingSec), color = Orange, size = 14, weight = FontWeight.Bold, tabular = true)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, null, tint = Orange, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Label(formatTime(m.remainingSec), color = Orange, size = 14, weight = FontWeight.Bold, tabular = true)
                    }
                    MiniMusic(sec, primary)
                }
            }
            is IslandMode.Call -> {
                Icon(Icons.Rounded.Call, null, tint = Green, modifier = Modifier.size(19.dp))
                Label(m.name, Modifier.weight(1f).padding(horizontal = 9.dp), size = 13)
                if (m.incoming) {
                    Waveform(true, Green, Modifier.height(14.dp))
                } else {
                    val sec = rememberElapsedSec(m.since)
                    Label(formatTime(sec), color = Green, size = 13, weight = FontWeight.Bold, tabular = true)
                }
            }
            is IslandMode.Device -> {
                Icon(
                    if (m.headphones) Icons.Rounded.Headphones else Icons.Rounded.Bluetooth,
                    null, tint = Color(0xFF0A84FF), modifier = Modifier.size(19.dp)
                )
                Label(m.name, Modifier.weight(1f).padding(start = 9.dp), size = 13)
            }
            is IslandMode.LowBattery -> {
                Icon(Icons.Rounded.BatteryAlert, null, tint = Red, modifier = Modifier.size(20.dp))
                Label("${m.percent}%", color = Red, size = 14, weight = FontWeight.Bold)
            }
            is IslandMode.Toggle -> {
                Icon(m.icon(), null, tint = m.tint(), modifier = Modifier.size(19.dp))
                Label(m.title(), Modifier.weight(1f).padding(start = 9.dp), size = 13)
            }
            is IslandMode.Live -> {
                Icon(m.icon(), null, tint = LiveBlue, modifier = Modifier.size(20.dp))
                Label(
                    m.title.ifBlank { m.app },
                    Modifier.weight(1f).padding(horizontal = 9.dp),
                    size = 13
                )
                if (m.kind == LiveKind.Download) {
                    DownloadRing(m.progress, 18.dp)
                    if (sec != null) {
                        Spacer(Modifier.width(6.dp))
                        MiniMusic(sec, primary)
                    }
                } else if (sec != null) MiniMusic(sec, primary)
                else Box(Modifier.size(7.dp).clip(CircleShape).background(LiveBlue))
            }
        }
    }
}

/* ------------------------------ BANNER (quick popups) ------------------------------ */

/** Width of the small banner used for notification / charging / device / toggle popups. */
internal fun bannerWidth(mode: IslandMode): Dp = when (mode) {
    is IslandMode.Charging -> 228.dp
    is IslandMode.LowBattery -> 228.dp
    is IslandMode.Toggle -> 228.dp
    is IslandMode.Device -> 240.dp
    else -> 258.dp
}

/** One slim row: [lead] title / subtitle [trail]. */
@Composable
internal fun BannerRow(
    lead: @Composable () -> Unit,
    title: String,
    subtitle: String,
    trail: (@Composable () -> Unit)? = null,
    ts: Float = 1f,
) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        lead()
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Label(title, size = (13 * ts).roundToInt(), weight = FontWeight.SemiBold)
            if (subtitle.isNotBlank()) Label(subtitle, color = Muted, size = (11 * ts).roundToInt(), weight = FontWeight.Normal)
        }
        if (trail != null) {
            Spacer(Modifier.width(8.dp))
            trail()
        }
    }
}

@Composable
internal fun BannerRing(percent: Int, color: Color, icon: ImageVector, ts: Float = 1f) {
    Box(Modifier.size((28 * ts).dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier.fillMaxSize(),
            color = color, trackColor = color.copy(alpha = 0.18f),
            strokeWidth = 2.5.dp, strokeCap = StrokeCap.Round
        )
        Icon(icon, null, tint = color, modifier = Modifier.size((14 * ts).dp))
    }
}

@Composable
internal fun BannerBadge(icon: ImageVector, tint: Color, bg: Color, ts: Float = 1f) {
    Box(
        Modifier.size((28 * ts).dp).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size((15 * ts).dp))
    }
}

@Composable
internal fun Banner(m: IslandMode, queued: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val kindStyles by IslandSettings.kindStyles.collectAsState()
    val ts = kindOf(m)?.let { kindStyles[it.key]?.text } ?: 1f
    when (m) {
        is IslandMode.Charging -> {
            val c = if (m.connected) Green else Orange
            BannerRow(
                lead = {
                    BannerRing(
                        m.percent, c,
                        if (m.connected) Icons.Rounded.Bolt else Icons.Rounded.BatteryStd, ts
                    )
                },
                title = stringResource(if (m.connected) R.string.ify_demo_charging else R.string.ify_live_charger_removed),
                subtitle = stringResource(if (m.connected) R.string.ify_live_power_connected else R.string.ify_live_unplugged),
                trail = { Label("${m.percent}%", color = c, size = (14 * ts).roundToInt(), weight = FontWeight.Bold) },
                ts = ts
            )
        }
        is IslandMode.LowBattery -> BannerRow(
            lead = { BannerRing(m.percent, Red, Icons.Rounded.BatteryAlert, ts) },
            title = stringResource(if (m.percent <= 10) R.string.ify_live_battery_very_low else R.string.ify_live_battery_low),
            subtitle = stringResource(R.string.ify_live_plug_in),
            trail = { Label("${m.percent}%", color = Red, size = (14 * ts).roundToInt(), weight = FontWeight.Bold) },
            ts = ts
        )
        is IslandMode.Notification -> BannerRow(
            lead = { Avatar(m.app, (28 * ts).dp) },
            title = m.title.ifBlank { m.app },
            subtitle = m.text.ifBlank { m.app },
            trail = {
                if (queued > 0) CountBadge(queued, primary)
                else Box(Modifier.size((7 * ts).dp).clip(CircleShape).background(primary))
            },
            ts = ts
        )
        is IslandMode.Device -> BannerRow(
            lead = {
                BannerBadge(
                    if (m.headphones) Icons.Rounded.Headphones else Icons.Rounded.BluetoothConnected,
                    Color(0xFF0A84FF), Color(0xFF0A2540), ts
                )
            },
            title = stringResource(R.string.ify_live_connected),
            subtitle = m.name,
            trail = { Icon(Icons.Rounded.CheckCircle, null, tint = Green, modifier = Modifier.size((17 * ts).dp)) },
            ts = ts
        )
        is IslandMode.Toggle -> BannerRow(
            lead = { BannerBadge(m.icon(), m.tint(), m.tint().copy(alpha = 0.18f), ts) },
            title = m.title(),
            subtitle = m.subtitle(),
            ts = ts
        )
        else -> Compact(m, queued, null)
    }
}
