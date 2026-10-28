/*
 * Ported from Islandify (https://github.com/bughunter11/Islandify) — MIT License,
 * Copyright (c) 2026 bughunter11. Full license text: THIRD_PARTY_NOTICES.md.
 * Adapted for MaxManager (packages, resources, integration). Modifications (C) 2026 Nader Magdy.
 *
 * الجزء الثاني من `IslandUi.kt` — قُسِم عند `Expanded` لسقف حجم الملف في `code_health`
 * (≤ 1000 سطر). و`private` على مستوى الملف صار `internal` عبر الحدّ: في Kotlin هي
 * خاصّة بالملف الواحد، والجزآن يتبادلان الاستدعاء (البنية التشعبية تسمّي أدوات الجزء
 * الآخر والعكس). لا تغيير سلوك — نقلٌ وتوسيع رؤية فقط.
 */
package nd.max.island.engine

import nd.max.R
import nd.max.island.core.IslandSettings
import nd.max.island.core.IslandText
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/* ------------------------------ EXPANDED / LARGE ------------------------------ */
@Composable
internal fun Expanded(m: IslandMode, level: IslandLevel, a: IslandActions) {
    val primary = MaterialTheme.colorScheme.primary
    val large = level == IslandLevel.Large
    when (m) {
        IslandMode.Idle -> IdleCard()

        is IslandMode.Charging -> Row(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { m.percent / 100f },
                    modifier = Modifier.size(76.dp),
                    color = IslandStyleFactory.HyperNeonGreen, trackColor = Color(0xFF16301D),
                    strokeWidth = 8.dp, strokeCap = StrokeCap.Round
                )
                Label("${m.percent}", size = 22, weight = FontWeight.Bold)
            }
            Spacer(Modifier.width(20.dp))
            Column {
                val turboTitle = if (m.connected) {
                    if (IslandSettings.hyperCharge.value) "HyperCharge"
                    else stringResource(R.string.ify_demo_charging)
                } else stringResource(R.string.ify_live_charger_removed)
                val turboSub = if (m.connected) {
                    if (m.watts > 0) "${m.watts}W • " + stringResource(R.string.ify_live_power_connected)
                    else stringResource(R.string.ify_live_power_connected)
                } else stringResource(R.string.ify_live_unplugged)
                Label(turboTitle, size = 20, weight = FontWeight.SemiBold)
                Label(turboSub, color = Muted, size = 14)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.Bolt, null, tint = IslandStyleFactory.HyperNeonGreen, modifier = Modifier.size(30.dp))
        }

        is IslandMode.Notification -> {
            val actionable = m.canReply || m.canMarkRead
            val typing = a.replying && m.canReply
            Column(
                Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = if (actionable) 14.dp else 20.dp)
            ) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                    Avatar(m.app, if (actionable && !large) 40.dp else if (large) 52.dp else 46.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Label(
                                m.app.uppercase(), Modifier.weight(1f),
                                color = primary, size = 11, weight = FontWeight.Bold, spacing = 0.8f
                            )
                            if (a.queued > 0) CountBadge(a.queued, primary)
                        }
                        Spacer(Modifier.height(3.dp))
                        Label(
                            m.title.ifBlank { m.app }, size = 17, weight = FontWeight.SemiBold,
                            lines = if (large && !typing) 2 else 1
                        )
                        Spacer(Modifier.height(2.dp))
                        Label(
                            m.text, color = Color(0xFFD1D1D6), size = 14,
                            lines = if (typing) 1 else if (large) 6 else 2, weight = FontWeight.Normal
                        )
                    }
                }
                if (actionable) {
                    Spacer(Modifier.height(8.dp))
                    if (typing) {
                        ReplyBar(a.onSendReply, a.onCancelReply)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (m.canMarkRead) PillButton(stringResource(R.string.ify_act_mark_read), a.onMarkRead)
                            if (m.canReply) PillButton(stringResource(R.string.ify_act_reply), a.onReply)
                        }
                    }
                }
            }
        }

        is IslandMode.Media -> {
            val accent = m.accentColor(primary)
            Column(
                Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Art(m, if (large) 64.dp else 54.dp, if (large) 16.dp else 14.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Label(m.title.ifBlank { stringResource(R.string.ify_unknown) }, size = if (large) 18 else 17, weight = FontWeight.SemiBold)
                        Label(m.artist, color = Muted, size = 14, weight = FontWeight.Normal)
                    }
                    if (!large) {
                        Spacer(Modifier.width(10.dp))
                        Waveform(m.playing, accent, Modifier.height(24.dp))
                    }
                }
                if (large) {
                    Waveform(
                        m.playing, accent,
                        Modifier.height(28.dp).align(Alignment.CenterHorizontally),
                        bars = 24, barWidth = 4.dp, gap = 4.dp
                    )
                    SeekBar(m, accent, a.onSeek)
                }
                // ترتيب التحكّم (السابق · تشغيل · التالي) ثابت في كل لغة، ومجموعة في الوسط.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoundIcon(Icons.Rounded.SkipPrevious, 46.dp, Color.Transparent, Color.White, a.onPrev)
                        RoundIcon(
                            if (m.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            54.dp, Color.White, Color.Black, a.onPlayPause
                        )
                        RoundIcon(Icons.Rounded.SkipNext, 46.dp, Color.Transparent, Color.White, a.onNext)
                    }
                }
            }
        }

        is IslandMode.Timer -> Row(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { m.remainingSec / m.totalSec.toFloat() },
                    modifier = Modifier.size(76.dp),
                    color = Orange, trackColor = Color(0xFF3A2A10),
                    strokeWidth = 8.dp, strokeCap = StrokeCap.Round
                )
                Icon(Icons.Rounded.Timer, null, tint = Orange, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Label(formatTime(m.remainingSec), color = Orange, size = 34, weight = FontWeight.Light, tabular = true)
                Label(stringResource(if (m.external) R.string.ify_live_clock_timer else R.string.ify_demo_timer), color = Muted, size = 14, weight = FontWeight.Normal)
            }
            // In-app timer: the button really stops it. Clock app timer: it only hides the island
            // (the timer keeps running in Clock), so it must not look like a stop button.
            if (m.external) {
                RoundIcon(Icons.Rounded.VisibilityOff, 46.dp, Color(0xFF2C2C2E), Color.White, onClick = { IslandController.stopTimer() })
            } else {
                RoundIcon(Icons.Rounded.Close, 46.dp, Red, Color.White, onClick = { IslandController.stopTimer() })
            }
        }

        is IslandMode.Call -> Row(
            Modifier.fillMaxSize().padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(m.name, 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Label(m.name, size = 18, weight = FontWeight.SemiBold)
                if (m.incoming) {
                    Label(stringResource(R.string.ify_live_incoming_call), color = Green, size = 14, weight = FontWeight.Normal)
                } else {
                    val sec = rememberElapsedSec(m.since)
                    Label(formatTime(sec), color = Green, size = 14, weight = FontWeight.Normal, tabular = true)
                }
            }
            RoundIcon(Icons.Rounded.CallEnd, 52.dp, Red, Color.White, a.onDecline)
            if (m.incoming && m.canAnswer) {
                Spacer(Modifier.width(12.dp))
                RoundIcon(Icons.Rounded.Call, 52.dp, Green, Color.White, a.onAnswer)
            }
        }

        is IslandMode.Device -> Row(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF0A2540)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (m.headphones) Icons.Rounded.Headphones else Icons.Rounded.BluetoothConnected,
                    null, tint = Color(0xFF0A84FF), modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Label(stringResource(R.string.ify_live_connected), size = 20, weight = FontWeight.SemiBold)
                Label(m.name, color = Muted, size = 14, weight = FontWeight.Normal)
            }
            Icon(Icons.Rounded.CheckCircle, null, tint = Green, modifier = Modifier.size(28.dp))
        }

        is IslandMode.LowBattery -> Row(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { m.percent / 100f },
                    modifier = Modifier.size(76.dp),
                    color = Red, trackColor = Color(0xFF3A1512),
                    strokeWidth = 8.dp, strokeCap = StrokeCap.Round
                )
                Label("${m.percent}", size = 22, weight = FontWeight.Bold)
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Label(
                    stringResource(if (m.percent <= 10) R.string.ify_live_battery_very_low else R.string.ify_live_battery_low),
                    size = 20, weight = FontWeight.SemiBold
                )
                Label(stringResource(R.string.ify_live_plug_in), color = Muted, size = 14, weight = FontWeight.Normal)
            }
            Icon(Icons.Rounded.BatteryAlert, null, tint = Red, modifier = Modifier.size(30.dp))
        }

        is IslandMode.Toggle -> Row(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(m.tint().copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(m.icon(), null, tint = m.tint(), modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Label(m.title(), size = 20, weight = FontWeight.SemiBold)
                Label(m.subtitle(), color = Muted, size = 14, weight = FontWeight.Normal)
            }
        }

        is IslandMode.Live -> Row(
            Modifier.fillMaxSize().padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(if (large) 56.dp else 50.dp).clip(CircleShape)
                    .background(LiveBlue.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(m.icon(), null, tint = LiveBlue, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Label(m.app.uppercase(), color = LiveBlue, size = 11, weight = FontWeight.Bold, spacing = 0.8f)
                Spacer(Modifier.height(3.dp))
                Label(
                    // A download keeps its extension visible; a navigation title can be cut at a word.
                    if (m.kind == LiveKind.Download) IslandText.fitMiddle(m.title.ifBlank { m.app }, if (large) 42 else 30)
                    else IslandText.fit(m.title.ifBlank { m.app }, if (large) 48 else 32),
                    size = 17, weight = FontWeight.SemiBold, lines = if (large) 2 else 1
                )
                Spacer(Modifier.height(2.dp))
                val isDl = m.kind == LiveKind.Download
                Label(
                    m.text, color = Color(0xFFD1D1D6), size = 14,
                    lines = if (isDl) 1 else if (large) 3 else 2, weight = FontWeight.Normal
                )
                if (isDl) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (m.progress >= 0) {
                            LinearProgressIndicator(
                                progress = { m.progress / 100f },
                                modifier = Modifier.weight(1f).height(5.dp).clip(CircleShape),
                                color = LiveBlue, trackColor = LiveBlue.copy(alpha = 0.2f),
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(Modifier.width(8.dp))
                            Label("${m.progress}%", color = LiveBlue, size = 12, weight = FontWeight.Bold)
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                color = LiveBlue, trackColor = LiveBlue.copy(alpha = 0.2f),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------ PIECES ------------------------------ */

/**
 * مقبض الاستعادة بعد إخفاء المستخدم: شريط صغير شبه شفّاف. لمسة تُعيد الجزيرة، وسحب أفقي
 * (تكرار الإيماءة التي أخفتها) يُعيدها أيضًا — فلا يبقى الإخفاء بلا رجوع.
 */
@Composable
internal fun IslandRestoreHandle(onRestore: () -> Unit) {
    val desc = stringResource(R.string.ify_island_restore)
    Box(
        Modifier
            .width(64.dp)
            .height(22.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.45f))
            .border(0.6.dp, Color(0xFF2C2C2E), RoundedCornerShape(50))
            .semantics { contentDescription = desc }
            .pointerInput(Unit) { detectTapGestures { onRestore() } }
            .pointerInput(Unit) { detectHorizontalDragGestures(onDragStart = { onRestore() }) { _, _ -> } },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(24.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF8E8E93))
        )
    }
}

/** "+2": how many more banners are waiting. */
@Composable
internal fun CountBadge(n: Int, color: Color) {
    Box(
        Modifier
            .height(18.dp)
            .widthIn(min = 18.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.35f))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Label("+$n", color = Color.White, size = 10, weight = FontWeight.Bold)
    }
}

/**
 * The second activity as a tiny indicator: music art, timer count, or live-app icon. It is always
 * tappable and promotes the companion to the main slot — that is the "switch" in two activities.
 */
@Composable
internal fun MiniActivity(m: IslandMode, fallback: Color, onClick: () -> Unit) {
    // «اجعل الموسيقى الأساس» صحيح للموسيقى وحدها؛ ولمؤقّت أو تنزيل يُقال الإجراء العامّ.
    val promote = stringResource(
        if (m is IslandMode.Media) R.string.ify_promote_music else R.string.ify_switch_activity
    )
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .semantics { contentDescription = promote }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (m) {
            is IslandMode.Media -> {
                Art(m, 22.dp, 6.dp)
                Spacer(Modifier.width(5.dp))
                Waveform(m.playing, m.accentColor(fallback), Modifier.height(14.dp))
            }
            is IslandMode.Timer -> {
                Icon(Icons.Rounded.Timer, null, tint = Orange, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Label(formatTime(m.remainingSec), color = Orange, size = 12, weight = FontWeight.Bold, tabular = true)
            }
            is IslandMode.Live -> {
                Icon(m.icon(), null, tint = LiveBlue, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Label(IslandText.fit(m.title.ifBlank { m.app }, 12), size = 11)
            }
            is IslandMode.Notification -> {
                Avatar(m.app, 18.dp)
                Spacer(Modifier.width(4.dp))
                Label(IslandText.fit(m.title.ifBlank { m.app }, 14), size = 11)
            }
            else -> Icon(Icons.Rounded.Bolt, null, tint = fallback, modifier = Modifier.size(15.dp))
        }
    }
}

/**
 * The companion under an expanded card: a full mini player for music, a compact row for any other
 * activity. Tapping it promotes it; it never steals the main card's space.
 */
@Composable
internal fun CompanionRow(m: IslandMode, a: IslandActions) {
    if (m is IslandMode.Media) {
        MiniPlayer(m, a)
        return
    }
    val title = when (m) {
        is IslandMode.Timer -> formatTime(m.remainingSec)
        is IslandMode.Live -> m.title.ifBlank { m.app }
        is IslandMode.Notification -> m.title.ifBlank { m.app }
        // لا اسم عامّ هنا: تسمية زرّ «تبديل» كعنوان نشاط وهمٌ. النشاط المجهول بلا عنوان أفضل.
        else -> ""
    }
    val subtitle = when (m) {
        is IslandMode.Live -> m.app
        is IslandMode.Notification -> m.text
        is IslandMode.Timer -> stringResource(R.string.ify_demo_timer)
        else -> ""
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 18.dp)
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { a.onPromoteSecondary() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (m) {
            is IslandMode.Live -> Icon(m.icon(), null, tint = LiveBlue, modifier = Modifier.size(20.dp))
            is IslandMode.Notification -> Avatar(m.app, 26.dp)
            else -> Icon(Icons.Rounded.Timer, null, tint = Orange, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Label(IslandText.fit(title, 30), size = 13, weight = FontWeight.SemiBold)
            if (subtitle.isNotBlank()) Label(IslandText.fit(subtitle, 34), color = Muted, size = 11, weight = FontWeight.Normal)
        }
        Icon(Icons.Rounded.SwapVert, stringResource(R.string.ify_promote_music), tint = Muted, modifier = Modifier.size(18.dp))
    }
}

/** Music under a timer / navigation card: song (tap = open the music app) + prev / play / next. */
@Composable
internal fun MiniPlayer(m: IslandMode.Media, a: IslandActions) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 18.dp)
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { a.onOpenMedia() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Art(m, 34.dp, 9.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Label(IslandText.fit(m.title.ifBlank { stringResource(R.string.ify_unknown) }, 26), size = 13, weight = FontWeight.SemiBold)
                Label(IslandText.fit(m.artist, 30), color = Muted, size = 12, weight = FontWeight.Normal)
            }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(
                    Icons.Rounded.SwapVert, 36.dp, Color.Transparent, Muted, a.onPromoteSecondary,
                    desc = stringResource(R.string.ify_promote_music)
                )
                RoundIcon(Icons.Rounded.SkipPrevious, 36.dp, Color.Transparent, Color.White, a.onPrev)
                RoundIcon(
                    if (m.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    36.dp, Color.White, Color.Black, a.onPlayPause
                )
                RoundIcon(Icons.Rounded.SkipNext, 36.dp, Color.Transparent, Color.White, a.onNext)
            }
        }
    }
}

/** Dark-teal pill button (Mark as read / Reply). Its own click, so the card's tap gesture is not triggered. */
@Composable
internal fun PillButton(text: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(c.copy(alpha = 0.22f))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Label(text, color = Color.White, size = 13, weight = FontWeight.SemiBold)
    }
}

/** Inline reply: text box + send + cancel. The keyboard opens by itself. */
@Composable
internal fun ReplyBar(onSend: (String) -> Unit, onCancel: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // The window turns focusable a moment after Reply is tapped, so wait a little before asking for focus
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { focus.requestFocus() }
        keyboard?.show()
    }
    fun send() { if (text.isNotBlank()) onSend(text.trim()) }

    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (text.isEmpty()) Label(stringResource(R.string.ify_reply_hint), color = Muted, size = 14, weight = FontWeight.Normal)
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        }
        Spacer(Modifier.width(8.dp))
        RoundIcon(Icons.AutoMirrored.Rounded.Send, 40.dp, primary, Color.White, onClick = { send() })
        Spacer(Modifier.width(6.dp))
        RoundIcon(Icons.Rounded.Close, 40.dp, Color(0xFF2C2C2E), Color.White, onCancel)
    }
}

/** Shown when nothing is active and the island is tapped: time, date, battery. */
@Composable
internal fun IdleCard() {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(10_000)
        }
    }
    // (level %, charging) from the sticky battery broadcast: one read, no receiver to leak
    val (battery, charging) = remember(now) {
        runCatching {
            val i = ctx.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val level = i?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = i?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = i?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) level * 100 / scale else 0
            pct to (status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                status == android.os.BatteryManager.BATTERY_STATUS_FULL)
        }.getOrDefault(0 to false)
    }
    val batteryColor = when {
        charging -> Green
        battery <= 15 -> Red
        battery <= 30 -> Orange
        else -> Green
    }
    val batteryIcon = when {
        charging -> Icons.Rounded.BatteryChargingFull
        battery <= 15 -> Icons.Rounded.BatteryAlert
        battery <= 50 -> Icons.Rounded.Battery3Bar
        else -> Icons.Rounded.BatteryFull
    }
    val time = remember(now) {
        // Follow the phone's 12 / 24-hour setting
        val pattern = if (android.text.format.DateFormat.is24HourFormat(ctx)) "HH:mm" else "h:mm"
        java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(java.util.Date(now))
    }
    val date = remember(now) {
        java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale.getDefault()).format(java.util.Date(now))
    }
    Row(
        Modifier.fillMaxSize().padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Label(time, size = 34, weight = FontWeight.Light, tabular = true)
            Label(date, color = Muted, size = 14, weight = FontWeight.Normal)
        }
        Icon(batteryIcon, null, tint = batteryColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Label("$battery%", size = 16, weight = FontWeight.SemiBold)
    }
}

@Composable
internal fun rememberElapsedSec(since: Long): Int {
    var sec by remember(since) { mutableIntStateOf(((SystemClock.elapsedRealtime() - since) / 1000).toInt()) }
    LaunchedEffect(since) {
        while (true) {
            delay(1000)
            sec = ((SystemClock.elapsedRealtime() - since) / 1000).toInt()
        }
    }
    return sec
}

/** Tap to seek. The position advances by itself every 250 ms. */
@Composable
internal fun SeekBar(m: IslandMode.Media, accent: Color, onSeek: (Long) -> Unit) {
    var pos by remember { mutableLongStateOf(m.currentPosition()) }
    LaunchedEffect(m.playing, m.positionMs, m.updatedAt, m.title) {
        pos = m.currentPosition()
        while (m.playing) {
            delay(250)
            pos = m.currentPosition()
        }
    }
    val dur = m.durationMs
    val frac = if (dur > 0) (pos / dur.toFloat()).coerceIn(0f, 1f) else 0f
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(18.dp)
                .pointerInput(dur, rtl) {
                    detectTapGestures { off ->
                        if (dur > 0) {
                            // In RTL the start of the bar is on the right; map the touch accordingly.
                            val f = (off.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            onSeek(((if (rtl) 1f - f else f) * dur).toLong().coerceIn(0L, dur))
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Track))
            Box(Modifier.fillMaxWidth(frac).height(4.dp).clip(CircleShape).background(accent))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Label(formatClock(pos), color = Muted, size = 11, tabular = true)
            Label(if (dur > 0) formatClock(dur) else "--:--", color = Muted, size = 11, tabular = true)
        }
    }
}

/** Canvas waveform. Audio is not captured (that would need RECORD_AUDIO); the bars are animated with a sine wave. */
@Composable
internal fun Waveform(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    bars: Int = 4,
    barWidth: Dp = 3.dp,
    gap: Dp = 3.dp,
) {
    // The loop only runs while playing. Paused music = no frames are scheduled, so no battery use
    // (an always-on infinite transition kept a 60 fps animation going even for a paused pill).
    val phase = remember { Animatable(0f) }
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        val full = (2 * PI).toFloat()
        while (true) {
            val ms = ((full - phase.value) / full * 1600f).toInt().coerceAtLeast(1)
            phase.animateTo(full, tween(ms, easing = LinearEasing))
            phase.snapTo(0f)
        }
    }
    val amount by animateFloatAsState(if (playing) 1f else 0f, spring(), label = "amount")

    Canvas(modifier.width((barWidth + gap) * bars - gap)) {
        val bw = barWidth.toPx()
        val g = gap.toPx()
        for (i in 0 until bars) {
            val s = (sin(phase.value * (1 + i % 3) + i * 1.3f) + 1f) / 2f
            val frac = 0.18f + 0.82f * s * amount
            val h = size.height * frac
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (bw + g), (size.height - h) / 2f),
                size = Size(bw, h),
                cornerRadius = CornerRadius(bw / 2f)
            )
        }
    }
}

@Composable
internal fun RoundIcon(
    icon: ImageVector,
    size: Dp,
    bg: Color,
    tint: Color,
    onClick: () -> Unit,
    desc: String? = null,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // An icon inside a button needs a name: without one a screen reader says only «زر».
        Icon(icon, desc, tint = tint, modifier = Modifier.size(size * 0.58f))
    }
}

@Composable
internal fun Art(m: IslandMode.Media, size: Dp, radius: Dp) {
    val s = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(radius)
    if (m.art != null) {
        Image(
            bitmap = m.art.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(shape)
        )
    } else {
        Box(
            Modifier
                .size(size)
                .clip(shape)
                .background(Brush.linearGradient(listOf(s.primary, s.tertiary))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
        }
    }
}

/** First user-visible character. take(1) cut emoji (surrogate pairs, ZWJ / flag sequences) in half. */
internal fun firstGrapheme(name: String): String {
    val t = name.trim()
    if (t.isEmpty()) return "?"
    val it = android.icu.text.BreakIterator.getCharacterInstance()
    it.setText(t)
    val end = it.next()
    return t.substring(0, if (end == android.icu.text.BreakIterator.DONE) t.length else end).uppercase()
}

@Composable
internal fun Avatar(name: String, size: Dp) {
    val palette = listOf(0xFF0A84FF, 0xFF30D158, 0xFFFF9F0A, 0xFFFF375F, 0xFFBF5AF2, 0xFF64D2FF)
    val c = Color(palette[(name.hashCode() and 0x7fffffff) % palette.size])
    Box(
        Modifier.size(size).clip(CircleShape).background(c),
        contentAlignment = Alignment.Center
    ) {
        Text(
            firstGrapheme(name),
            color = Color.White,
            fontSize = (size.value * 0.46f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun Label(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Int = 13,
    lines: Int = 1,
    weight: FontWeight = FontWeight.Medium,
    tabular: Boolean = false,
    spacing: Float = 0f,
) = Text(
    text,
    modifier = modifier,
    color = color,
    fontSize = size.sp,
    fontWeight = weight,
    letterSpacing = spacing.sp,
    maxLines = lines,
    overflow = TextOverflow.Ellipsis,
    style = if (tabular) TextStyle(fontFeatureSettings = "tnum") else LocalTextStyle.current
)

internal fun formatTime(s: Int) =
    if (s >= 3600) "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    else "%02d:%02d".format(s / 60, s % 60)

internal fun formatClock(ms: Long): String {
    val s = (ms / 1000).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}
