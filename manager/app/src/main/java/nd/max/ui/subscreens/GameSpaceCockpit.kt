/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.ui.component.AppIconImage
import nd.max.ui.component.lobbyPress
import nd.max.ui.design.MaxAlpha
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.theme.DisplayFontFamily
import kotlin.math.roundToInt

/**
 * **كوكبيت اللوبي — التركيبة الأفقية الوحيدة** (اتجاه «A» الذي اختاره المالك، جولة ٢٩٥).
 *
 * ولماذا حلَّ محلّ المسرح المرسوم: المسرح كان لوحةً ثابتة ١٦٧٢×٩٤١ تُقاس إلى الشاشة، فكان
 * النصّ يُصغَّر إلى ما دون ١٢sp ويُقصّ عند `MaxFontScale = 1.15`، وكان اللمس يُضخَّم بحدّ ١.٢٥
 * فلا يبلغ ٤٨dp إلا عناصر كبيرة أصلًا، وكان `LayoutDirection.Ltr` مفروضًا على العربية كلها.
 * هنا كل شيء بوحدات dp حقيقية: لا مقياس، لا قصّ، لا قلب اتجاه، وكل هدف لمس ≥
 * `MaxSize.minTouchTarget`.
 *
 * **وقاعدة المعلومات:** كل قياس يظهر **مرّة واحدة** — الجاهزية والحالة في العدّاد، والمعالج
 * والرسوميات والحرارة في صفّ واحد، والإطارات في سطرها، والبطارية في سطرها. ولا توهّج إلا على
 * زرّ اللعب (`PlayRow`)، فتصير له الأولوية البصرية وحدها.
 *
 * وحدوده المعلنة: قائمة الجلسات تفتح شاشتها (`onOpenSessions`)، وزرّ الخيارات يفتح قسم اللعبة
 * (`onOptions`)، والرفّ يختار اللعبة فقط؛ لا مسار ثانٍ لحفظ التفضيلات أو لتطبيق ملفّ.
 */
private val CockpitInstrumentWidth = 340.dp
private val CockpitTopBarHeight = 56.dp
private val CockpitShelfCardWidth = 84.dp
private val CockpitShelfIcon = 64.dp
private val CockpitArtEdge = 72.dp
private val CockpitArtEdgeTight = 40.dp
private val CockpitHeroTightHeight = 200.dp
private val CockpitBrandRow = 34.dp
private val CockpitSparkHeight = 34.dp
private val CockpitBatteryTrack = 6.dp
private val CockpitBatteryRadius = 3.dp

/** المجهول يُكتب شرطةً لا صفرًا: «لا أعرف» ليس «صفر» (ADR-07). */
private const val CockpitUnknown = "—"

/** خمسة أبواب في الشريط؛ و«الملفّ» و«الإعدادات» في قائمة ⋯ فلا يزدحم الشريط. */
private val CockpitRailPages = listOf(
    LobbyPage.Home,
    LobbyPage.Library,
    LobbyPage.Performance,
    LobbyPage.Tools,
    LobbyPage.Features,
)

@Composable
internal fun GameSpaceCockpit(
    data: StageData,
    actions: StageActions,
    currentPage: LobbyPage,
    onPage: (LobbyPage) -> Unit,
    topActions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaxSpace.xl, vertical = MaxSpace.md),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.md),
    ) {
        CockpitTopBar(data = data, onPage = onPage, topActions = topActions)
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
        ) {
            CockpitRail(current = currentPage, onPage = onPage, modifier = Modifier.fillMaxHeight())
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.md),
            ) {
                if (data.games.isEmpty()) {
                    EmptyHero(
                        onOpenLibrary = actions.onOpenLibrary,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                } else {
                    CockpitHero(data = data, actions = actions, modifier = Modifier.weight(1f).fillMaxWidth())
                }
                CockpitShelf(data = data, actions = actions)
            }
            CockpitInstrument(data = data, actions = actions, modifier = Modifier.fillMaxHeight())
        }
    }
}

@Composable
private fun CockpitTopBar(
    data: StageData,
    onPage: (LobbyPage) -> Unit,
    topActions: @Composable () -> Unit,
) {
    var overflow by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().height(CockpitTopBarHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
    ) {
        val game = data.selected
        if (game != null) {
            AppIconImage(packageName = game.packageName, size = MaxSize.iconContainer)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = game?.label ?: stringResource(R.string.game_space_title),
                color = GameSpacePalette.Ink,
                fontSize = 18.sp,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val tagline = data.tagline
            if (!tagline.isNullOrBlank()) {
                Text(
                    text = tagline,
                    color = GameSpacePalette.Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs),
        ) { topActions() }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = data.clock,
                color = GameSpacePalette.Ink,
                fontSize = 16.sp,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.SemiBold,
            )
            if (data.date.isNotBlank()) {
                Text(text = data.date, color = GameSpacePalette.Muted, fontSize = 11.sp)
            }
        }
        Box {
            CockpitIconButton(
                icon = Icons.Rounded.MoreHoriz,
                description = stringResource(R.string.gs_more),
                selected = overflow,
                onClick = { overflow = true },
            )
            DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(LobbyPage.Profile.labelRes)) },
                    onClick = {
                        overflow = false
                        onPage(LobbyPage.Profile)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(LobbyPage.Settings.labelRes)) },
                    onClick = {
                        overflow = false
                        onPage(LobbyPage.Settings)
                    },
                )
            }
        }
    }
}

@Composable
private fun CockpitIconButton(
    icon: ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(MaxRadius.control)
    Box(
        modifier = Modifier
            .size(MaxSize.minTouchTarget)
            .lobbyPress(interaction, 0.92f)
            .clip(shape)
            .background(if (selected) GameSpacePalette.PanelSelected else GameSpacePalette.Glass)
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (selected) GameSpacePalette.Ink else GameSpacePalette.Muted,
            modifier = Modifier.size(MaxSize.iconGlyph),
        )
    }
}

@Composable
internal fun CockpitRail(
    current: LobbyPage,
    onPage: (LobbyPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(GameSpaceRailWidth)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.gs_brand_mark),
            color = GameSpacePalette.VioletSoft,
            fontSize = 26.sp,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            modifier = Modifier.height(CockpitBrandRow),
        )
        CockpitRailPages.forEach { page ->
            CockpitRailItem(page = page, current = current, onPage = onPage)
        }
    }
}

@Composable
private fun CockpitRailItem(page: LobbyPage, current: LobbyPage, onPage: (LobbyPage) -> Unit) {
    val active = page == current
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(MaxRadius.control)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MaxSize.minTouchTarget)
            .lobbyPress(interaction, 0.94f)
            .clip(shape)
            .background(if (active) GameSpacePalette.PanelSelected else Color.Transparent)
            .selectable(
                selected = active,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = { onPage(page) },
            )
            .padding(horizontal = MaxSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs),
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            tint = if (active) GameSpacePalette.VioletSoft else GameSpacePalette.Muted,
            modifier = Modifier.size(MaxSize.iconGlyph),
        )
        Text(
            text = stringResource(page.labelRes),
            color = if (active) GameSpacePalette.Ink else GameSpacePalette.Muted,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CockpitHero(data: StageData, actions: StageActions, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(MaxRadius.group)
    BoxWithConstraints(modifier = modifier.clip(shape)) {
        val tight = maxHeight < CockpitHeroTightHeight
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(GameSpacePalette.Glass, GameSpacePalette.GlassDeep)
                    )
                )
                .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
                .padding(horizontal = MaxSpace.xl, vertical = MaxSpace.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.xl),
        ) {
            HeroArt(game = data.selected, edge = if (tight) CockpitArtEdgeTight else CockpitArtEdge)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.sm),
            ) {
                Text(
                    text = data.selected?.label ?: data.statusTitle,
                    color = GameSpacePalette.Ink,
                    fontSize = 24.sp,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
                ) {
                    val tagline = data.tagline
                    if (!tagline.isNullOrBlank()) CockpitChip(text = tagline)
                    CockpitChip(
                        text = stringResource(
                            R.string.gs_percent_value,
                            data.readinessPercent.coerceIn(0, 100),
                        )
                    )
                }
                Spacer(modifier = Modifier.height(MaxSpace.hairline))
                PlayRow(
                    enabled = data.selected != null,
                    onLaunch = actions.onLaunch,
                    onOptions = actions.onOptions,
                    modifier = Modifier,
                    launching = data.launching,
                )
            }
        }
    }
}

@Composable
private fun CockpitChip(text: String) {
    val shape = RoundedCornerShape(MaxRadius.chip)
    Text(
        text = text,
        color = GameSpacePalette.Lavender,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(shape)
            .background(GameSpacePalette.Violet.copy(alpha = MaxAlpha.toneContainer))
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
            .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
    )
}

@Composable
private fun CockpitInstrument(data: StageData, actions: StageActions, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(MaxRadius.group)
    val ready = data.readinessPercent.coerceIn(0, 100)
    Column(
        modifier = modifier
            .width(CockpitInstrumentWidth)
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(GameSpacePalette.Glass, GameSpacePalette.GlassDeep))
            )
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
            .verticalScroll(rememberScrollState())
            .padding(MaxSpace.lg),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StageRing(
            progress = ready / 100f,
            value = ready.toString(),
            label = data.statusTitle,
            from = GameSpacePalette.VioletDeep,
            to = GameSpacePalette.VioletSoft,
            warn = false,
        )
        if (data.statusDetail.isNotBlank()) {
            Text(
                text = data.statusDetail,
                color = GameSpacePalette.Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CockpitDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
        ) {
            CockpitMetric(
                label = stringResource(R.string.gs_cpu),
                value = data.cpu?.let { "$it%" },
                modifier = Modifier.weight(1f),
            )
            CockpitMetric(
                label = stringResource(R.string.gs_gpu),
                value = data.gpu?.let { "$it%" },
                modifier = Modifier.weight(1f),
            )
            CockpitMetric(
                label = stringResource(R.string.gs_heat),
                value = data.heat?.let { "${it.roundToInt()}°" },
                modifier = Modifier.weight(1f),
            )
        }
        CockpitDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.gs_fps_unit),
                    color = GameSpacePalette.Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = data.fps.value?.toString() ?: CockpitUnknown,
                        color = GameSpacePalette.Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (data.fps.live) {
                        Spacer(modifier = Modifier.width(MaxSpace.xs))
                        PulseDot(color = GameSpacePalette.Mint)
                    }
                }
            }
            StageSparkline(
                values = data.fps.history,
                live = data.fps.live,
                modifier = Modifier.weight(1.4f).height(CockpitSparkHeight),
            )
        }
        CockpitDivider()
        CockpitBattery(percent = data.batteryPercent, charging = data.charging)
        CockpitDivider()
        CockpitDoor(
            label = stringResource(R.string.lobby_insights_sessions),
            onOpen = actions.onOpenSessions,
        )
    }
}

@Composable
private fun CockpitDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MaxSize.hairlineBorder)
            .background(GameSpacePalette.Hairline)
    )
}

@Composable
private fun CockpitMetric(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)) {
        Text(
            text = label,
            color = GameSpacePalette.Muted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value ?: CockpitUnknown,
            color = if (value == null) GameSpacePalette.Muted else GameSpacePalette.Ink,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun CockpitBattery(percent: Int?, charging: Boolean) {
    val tone = batteryTone(percent, charging)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.gs_battery),
                color = GameSpacePalette.Muted,
                fontSize = 11.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = percent?.let { stringResource(R.string.gs_percent_value, it) } ?: CockpitUnknown,
                color = tone,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CockpitBatteryTrack)
                .clip(RoundedCornerShape(CockpitBatteryRadius))
                .background(GameSpacePalette.Track)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((percent ?: 0).coerceIn(0, 100) / 100f)
                    .fillMaxHeight()
                    .background(tone)
            )
        }
    }
}

@Composable
private fun CockpitDoor(label: String, onOpen: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(MaxRadius.control)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MaxSize.minTouchTarget)
            .lobbyPress(interaction, 0.96f)
            .clip(shape)
            .background(GameSpacePalette.Violet.copy(alpha = MaxAlpha.toneWash))
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onOpen,
            )
            .padding(horizontal = MaxSpace.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
    ) {
        Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = null,
            tint = GameSpacePalette.VioletSoft,
            modifier = Modifier.size(MaxSize.iconGlyphSmall),
        )
        Text(
            text = label,
            color = GameSpacePalette.Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CockpitShelf(data: StageData, actions: StageActions) {
    if (data.games.isEmpty()) return
    val state = rememberLazyListState()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            state = state,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
        ) {
            items(data.games, key = { it.packageName }) { game ->
                CockpitShelfCard(
                    game = game,
                    selected = game.packageName == data.selected?.packageName,
                    onClick = { actions.onSelect(game.packageName) },
                )
            }
        }
        CockpitSeeAll(onClick = actions.onOpenLibrary)
    }
}

@Composable
private fun CockpitShelfCard(game: GameApp, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(MaxRadius.tile)
    Column(
        modifier = Modifier
            .width(CockpitShelfCardWidth)
            .lobbyPress(interaction, 0.94f)
            .clip(shape)
            .background(if (selected) GameSpacePalette.PanelSelected else Color.Transparent)
            .border(
                width = if (selected) MaxSize.activeRing else MaxSize.hairlineBorder,
                color = if (selected) GameSpacePalette.VioletSoft else GameSpacePalette.Hairline,
                shape = shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(MaxSpace.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
    ) {
        AppIconImage(packageName = game.packageName, size = CockpitShelfIcon)
        Text(
            text = game.label,
            color = if (selected) GameSpacePalette.Ink else GameSpacePalette.Muted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CockpitSeeAll(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(MaxRadius.control)
    Box(
        modifier = Modifier
            .height(MaxSize.minTouchTarget)
            .lobbyPress(interaction, 0.94f)
            .clip(shape)
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = MaxSpace.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.gs_view_all),
            color = GameSpacePalette.VioletSoft,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
