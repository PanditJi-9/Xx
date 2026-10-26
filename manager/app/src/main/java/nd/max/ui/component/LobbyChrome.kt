/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewSidebar
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import nd.max.R
import nd.max.core.gamespace.LobbyCue
import nd.max.core.gamespace.GameModeStatus
import nd.max.core.gamespace.LobbyModel
import nd.max.core.privilege.ExecutionModeStore
import nd.max.core.privilege.PrivilegeManager
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import nd.max.ui.design.MaxAlpha
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace

/*
 * عناصر إطار اللوبي: تبويبات علوية بمؤشّر منزلق، وكبسولة حالة حيّة، وأزرار دائرية، وعمود معلومات
 * اللعبة مع زرّ «ابدأ» المتوهّج، وتلميح السحب، وإشعار. كل لون يتبع لون اللعبة عبر دالّة `tone` تُقرأ
 * في مرحلة الرسم حيث أمكن، فلا إعادة تركيب أثناء انتقال اللون.
 */

private val TabWidth = 104.dp
private val TabInset = 3.dp
private val StartHeight = 56.dp
private val StartGlowStep = 7.dp
private const val START_GLOW_ALPHA = 0.20f
private const val START_GLOW_STEPS = 3
private const val CHROME_FILL_ALPHA = 0.50f
private const val HINT_BOB_DP = 4f
private const val HINT_ALPHA = 0.70f

// ─────────────────────────────── التبويبات ───────────────────────────────

/**
 * تبويبات علويّة (ألعاب | تحكّم) بمؤشّر ينزلق مع موضع السحب الرأسيّ الفعليّ: يُرسم في مرحلة الرسم
 * فيتبع الإصبع لا الحالة المستقرّة. نصّ كل تبويب يُرسم مرّتين (خافت + أبيض) والأبيض تُحرَّك شفافيّته
 * في الطبقة فتنتقل الألوان بسلاسة بلا إعادة تركيب.
 */
@Composable
internal fun LobbyTopTabs(
    labels: List<String>,
    selected: Int,
    position: () -> Float,
    tone: () -> Color,
    description: String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val shape = remember { RoundedCornerShape(MaxRadius.pill) }
    val sounds = LocalLobbySounds.current
    Row(
        modifier = modifier
            .semantics { contentDescription = description }
            .height(MaxSize.minTouchTarget)
            .clip(shape)
            .background(LobbyPalette.Black.copy(alpha = CHROME_FILL_ALPHA))
            .border(MaxSize.hairlineBorder, LobbyPalette.Hairline, shape)
            .drawBehind {
                val count = labels.size
                if (count == 0) return@drawBehind
                val slotW = size.width / count
                val index = position().coerceIn(0f, (count - 1).toFloat())
                val x = if (rtl) size.width - (index + 1f) * slotW else index * slotW
                val accent = tone()
                val inset = TabInset.toPx()
                val h = size.height - inset * 2f
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(accent, lerp(accent, LobbyPalette.Black, 0.35f)),
                        startX = x,
                        endX = x + slotW
                    ),
                    topLeft = Offset(x + inset, inset),
                    size = Size(slotW - inset * 2f, h),
                    cornerRadius = CornerRadius(h / 2f)
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { i, label ->
            Box(
                modifier = Modifier
                    .width(TabWidth)
                    .fillMaxHeight()
                    .selectable(
                        selected = i == selected,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = {
                            if (i != selected) sounds.play(LobbyCue.Tap)
                            onSelect(i)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = LobbyPalette.Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier
                        .clearAndSetSemantics { }
                        .graphicsLayer { alpha = 1f - abs(position() - i).coerceIn(0f, 1f) }
                )
            }
        }
    }
}

// ─────────────────────────────── كبسولة الحالة ───────────────────────────────

/**
 * كبسولة حالة الجهاز: الساعة وشحن البطارية وحرارتها والذاكرة الحرّة. قراءات حقيقيّة فقط؛ ما لم يُقرأ
 * يُعرض «—» ولا يُختلق رقم (ADR-07). القراءات تُجدَّد داخلها هي فلا تُعيد تركيب الشاشة.
 */
@Composable
internal fun LobbyStatusCapsule(modifier: Modifier = Modifier) {
    val clock by rememberLobbyClock()
    val battery by rememberLobbyBattery()
    val memory by rememberLobbyMemory()
    val unavailable = stringResource(R.string.lobby_ready_unavailable)
    val batteryText = battery?.let { "${it.percent}%" }
    val tempText = LobbyModel.temperatureText(battery?.tempTenthsC)
    val ramText = LobbyModel.freeMemoryText(memory?.availBytes)
    val charging = battery?.charging == true
    val shape = remember { RoundedCornerShape(MaxRadius.pill) }
    Row(
        modifier = modifier
            .clip(shape)
            .background(LobbyPalette.Black.copy(alpha = CHROME_FILL_ALPHA))
            .border(MaxSize.hairlineBorder, LobbyPalette.Hairline, shape)
            .padding(horizontal = MaxSpace.md, vertical = MaxSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        if (clock.isNotEmpty()) {
            Text(
                text = clock,
                color = LobbyPalette.Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        StatusReading(
            icon = if (charging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
            text = batteryText ?: MAX_VALUE_UNAVAILABLE,
            description = stringResource(R.string.lobby_ready_battery_cd, batteryText ?: unavailable),
            tint = if (charging) LobbyPalette.Positive else LobbyPalette.Ink
        )
        StatusReading(
            icon = Icons.Rounded.Thermostat,
            text = tempText ?: MAX_VALUE_UNAVAILABLE,
            description = stringResource(R.string.lobby_ready_temp_cd, tempText ?: unavailable),
            tint = LobbyPalette.Cyan
        )
        StatusReading(
            icon = Icons.Rounded.Memory,
            text = ramText ?: MAX_VALUE_UNAVAILABLE,
            description = stringResource(R.string.lobby_ready_ram_cd, ramText ?: unavailable),
            tint = LobbyPalette.Cyan
        )
        // المسار الذي تنفَّذ به أوامر مساحة الألعاب الآن (جذر · Shizuku · أساسي): يُقرأ من الحالة المُخزَّنة بلا
        // نافذة صلاحية، ويُعرض نصًّا فلا يُعتمد على اللون وحده.
        val privilege by PrivilegeManager.snapshot.collectAsState()
        val lane = GameModeStatus.of(
            ExecutionModeStore.current(),
            privilege.rootGranted || PrivilegeManager.cachedRootGranted(),
            privilege.shizuku.ready
        )
        val laneText = stringResource(lane.badgeRes())
        Text(
            text = laneText,
            color = if (lane.isCautionBadge()) LobbyPalette.Caution else LobbyPalette.Positive,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.semantics { contentDescription = laneText }
        )
    }
}

private fun GameModeStatus.badgeRes(): Int = when (this) {
    GameModeStatus.ACTIVE_ROOT -> R.string.lobby_lane_root
    GameModeStatus.ACTIVE_SHIZUKU -> R.string.lobby_lane_shizuku
    GameModeStatus.ACTIVE_BASIC -> R.string.lobby_lane_basic
    GameModeStatus.NEEDS_ROOT, GameModeStatus.NEEDS_SHIZUKU, GameModeStatus.NO_PRIVILEGE -> R.string.lobby_lane_none
}

private fun GameModeStatus.isCautionBadge(): Boolean =
    this == GameModeStatus.NEEDS_ROOT || this == GameModeStatus.NEEDS_SHIZUKU || this == GameModeStatus.NO_PRIVILEGE

@Composable
private fun StatusReading(icon: ImageVector, text: String, description: String, tint: Color) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(MaxSize.iconGlyphSmall)
        )
        Text(text = text, color = LobbyPalette.Ink, fontSize = 12.sp, maxLines = 1)
    }
}

// ─────────────────────────────── أزرار ───────────────────────────────

/**
 * زرّ أيقونة دائريّ؛ هدف اللمس 48dp من `minimumInteractiveComponentSize` والدائرة المرئيّة أصغر.
 * عند [selected] يمتلئ بلون [tone] وتشتدّ حافّته (مثلًا: مرشّح المفضّلة مفعّل).
 */
@Composable
internal fun LobbyRoundButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tone: Color = LobbyPalette.Red,
    cue: LobbyCue? = LobbyCue.Tap
) {
    val interaction = remember { MutableInteractionSource() }
    val sounds = LocalLobbySounds.current
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .lobbyPress(interaction)
            .clip(CircleShape)
            .background(if (selected) tone.copy(alpha = 0.28f) else LobbyPalette.Black.copy(alpha = CHROME_FILL_ALPHA))
            .border(MaxSize.hairlineBorder, if (selected) tone else LobbyPalette.Hairline, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    if (cue != null) sounds.play(cue)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (selected) LobbyPalette.Ink else LobbyPalette.Steel,
            modifier = Modifier
                .padding(MaxSpace.sm)
                .size(MaxSize.iconGlyph)
        )
    }
}

/**
 * زرّ «ابدأ»: حبّة طويلة بتدرّج لون اللعبة وإضاءة ناعمة متعدّدة الحلقات تتنفّس مع [breathe]، وتصغر
 * قليلًا عند الضغط. اللون والتوهّج يُقرآن في مرحلة الرسم.
 */
@Composable
internal fun LobbyStartButton(
    label: String,
    tone: () -> Color,
    breathe: State<Float>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = remember { RoundedCornerShape(MaxRadius.pill) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(StartHeight)
            .lobbyPress(interaction)
            .drawBehind {
                val accent = tone()
                val step = StartGlowStep.toPx()
                val b = breathe.value
                for (i in 1..START_GLOW_STEPS) {
                    val spread = step * i * (0.8f + 0.5f * b)
                    drawRoundRect(
                        color = accent.copy(alpha = START_GLOW_ALPHA / i),
                        topLeft = Offset(-spread, -spread),
                        size = Size(size.width + spread * 2f, size.height + spread * 2f),
                        cornerRadius = CornerRadius(size.height / 2f + spread)
                    )
                }
            }
            .clip(shape)
            .drawBehind {
                val accent = tone()
                drawRect(
                    Brush.horizontalGradient(listOf(accent, lerp(accent, LobbyPalette.Black, 0.40f)))
                )
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.22f),
                        0.5f to Color.Transparent
                    )
                )
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(MaxSize.iconGlyph)
        )
        Spacer(Modifier.width(MaxSpace.sm))
        Text(
            text = label,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/** بلاطة صغيرة بأيقونة وعنوان وقيمة (ملف اللعبة / اللوحة الجانبية)؛ النشطة تتلوّن بلون اللعبة. */
@Composable
internal fun LobbyInfoTile(
    icon: ImageVector,
    label: String,
    value: String,
    active: Boolean,
    tone: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cue: LobbyCue? = LobbyCue.Tap
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = remember { RoundedCornerShape(MaxRadius.control) }
    val sounds = LocalLobbySounds.current
    Row(
        modifier = modifier
            .lobbyPress(interaction)
            .clip(shape)
            .background(LobbyPalette.Black.copy(alpha = CHROME_FILL_ALPHA))
            .border(
                MaxSize.hairlineBorder,
                if (active) tone.copy(alpha = MaxAlpha.borderStrong) else LobbyPalette.Hairline,
                shape
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    if (cue != null) sounds.play(cue)
                    onClick()
                }
            )
            .padding(horizontal = MaxSpace.md, vertical = MaxSpace.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) tone else LobbyPalette.Steel,
            modifier = Modifier.size(MaxSize.iconGlyph)
        )
        Column {
            Text(
                text = label,
                color = LobbyPalette.Muted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = LobbyPalette.Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────── عمود معلومات اللعبة ───────────────────────────────

/**
 * عمود معلومات اللعبة المحدَّدة (بداية الاتّجاه): الاسم والمعلومات تتلاشى بين الألعاب، يليهما زرّ
 * «ابدأ» ثم بلاطتا «ملف اللعبة» و«اللوحة الجانبية». نقر ملف اللعبة يفتح مركز التحكّم، ونقر اللوحة يبدّلها.
 */
@Composable
internal fun LobbyInfoPanel(
    title: String,
    meta: String,
    favorite: Boolean,
    onFavorite: () -> Unit,
    profileValue: String,
    onProfile: () -> Unit,
    panelValue: String,
    panelOn: Boolean,
    onPanel: () -> Unit,
    startLabel: String,
    onStart: () -> Unit,
    tone: () -> Color,
    breathe: State<Float>,
    modifier: Modifier = Modifier
) {
    val accent = tone()
    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.Top) {
            Crossfade(
                targetState = title to meta,
                animationSpec = tween(MaxDuration.quick),
                label = "lobbyInfoTitle",
                modifier = Modifier.weight(1f)
            ) { (name, detail) ->
                Column {
                    Text(
                        text = name,
                        color = LobbyPalette.Ink,
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (detail.isNotBlank()) {
                        Text(
                            text = detail,
                            color = LobbyPalette.Muted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            LobbyRoundButton(
                icon = if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                description = stringResource(
                    if (favorite) R.string.gaming_unfavorite else R.string.gaming_favorite
                ),
                onClick = onFavorite,
                selected = favorite,
                tone = accent,
                cue = if (favorite) LobbyCue.Off else LobbyCue.On
            )
        }
        Spacer(Modifier.height(MaxSpace.lg))
        LobbyStartButton(label = startLabel, tone = tone, breathe = breathe, onClick = onStart)
        Spacer(Modifier.height(MaxSpace.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
        ) {
            LobbyInfoTile(
                icon = Icons.Rounded.Tune,
                label = stringResource(R.string.lobby_tile_profile),
                value = profileValue,
                active = false,
                tone = accent,
                onClick = onProfile,
                modifier = Modifier.weight(1f)
            )
            LobbyInfoTile(
                icon = Icons.Rounded.ViewSidebar,
                label = stringResource(R.string.lobby_tile_panel),
                value = panelValue,
                active = panelOn,
                tone = accent,
                onClick = onPanel,
                modifier = Modifier.weight(1f),
                cue = if (panelOn) LobbyCue.Off else LobbyCue.On
            )
        }
    }
}

// ─────────────────────────────── تلميح وإشعار ───────────────────────────────

/** سهم صغير وسطر يدعوان لسحب المسرح إلى الأعلى؛ يطفو بخفّة مع [bob] ولا يتحرّك دون الحركة المحيطة. */
@Composable
internal fun LobbyPullHint(text: String, bob: State<Float>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.graphicsLayer {
            translationY = -bob.value * HINT_BOB_DP * density
            alpha = HINT_ALPHA
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowUp,
            contentDescription = null,
            tint = LobbyPalette.Muted,
            modifier = Modifier.size(MaxSize.iconGlyph)
        )
        Text(text = text, color = LobbyPalette.Muted, fontSize = 11.sp, maxLines = 1)
    }
}

/** شريط إشعار صغير: معلومة (رمادي) أو تنبيه ([caution]) مع زرّ إجراء اختياريّ. */
@Composable
internal fun LobbyNotice(
    message: String,
    modifier: Modifier = Modifier,
    caution: Boolean = false,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    val shape = remember { RoundedCornerShape(MaxRadius.control) }
    val accent = if (caution) LobbyPalette.Caution else LobbyPalette.Steel
    Row(
        modifier = modifier
            .clip(shape)
            .background(LobbyPalette.Black.copy(alpha = 0.55f))
            .border(MaxSize.hairlineBorder, accent.copy(alpha = MaxAlpha.border), shape)
            .padding(horizontal = MaxSpace.md, vertical = MaxSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        Icon(
            imageVector = if (caution) Icons.Rounded.Warning else Icons.Rounded.Info,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(MaxSize.iconGlyphSmall)
        )
        Text(
            text = message,
            color = LobbyPalette.Ink,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (action != null) {
            Text(
                text = action,
                color = LobbyPalette.RedBright,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(role = Role.Button, onClick = onAction)
            )
        }
    }
}
