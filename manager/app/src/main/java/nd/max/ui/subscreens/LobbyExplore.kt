/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.subscreens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.core.gamespace.GamePlayStats
import nd.max.core.gamespace.LobbyFilter
import nd.max.core.gamespace.LobbyInsights
import nd.max.ui.component.AppIconImage
import nd.max.ui.component.LobbyPalette
import nd.max.ui.component.LobbyRoundButton
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxVisualSpec
import nd.max.ui.design.maxRadius

/** أقلّ عرض لعمود البطاقة؛ تحته تصير الشبكة عمودًا واحدًا تلقائيًا. */
private val ExploreColumnMin = MaxVisualSpec.hero * 2f

/**
 * «استكشف»: مكتبة كاملة فوق المسرح بلا مغادرة اللوبي.
 *
 * لماذا طبقة لا شاشة: المسرح يحمل الاختيار والتشغيل، والبحث في عشرات الألعاب كان يعني سحب الحلقة
 * سحبًا طويلًا أو فتح إدارة الألعاب (شاشة إدراج لا اختيار). هذه الطبقة تعرض **كل** الألعاب مع
 * وقت اللعب والتهيئة وحالة اللوحة، وتُعيد اللعبة المختارة إلى المسرح مباشرةً.
 *
 * كل ما تعرضه مشتقٌّ من مصادر قائمة: `GameApp` من المكتبة، والأرقام من `PlaySessionLedger`،
 * والتهيئة من `AppConfig` و`GamePanelPrefs`. ولا زرّ هنا يكتب عتادًا.
 */
@Composable
internal fun LobbyExplore(
    games: List<GameApp>,
    favorites: Set<String>,
    stats: Map<String, GamePlayStats>,
    tweaks: Map<String, Int>,
    armed: Set<String>,
    currentPackage: String?,
    accent: Color,
    animations: Boolean,
    onSelect: (GameApp) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nowMs = remember { System.currentTimeMillis() }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(LobbyFilter.All) }
    val counts = remember(games, favorites, stats, nowMs) {
        LobbyInsights.counts(games, favorites, stats, nowMs)
    }
    val visible = remember(games, filter, query, favorites, stats, nowMs) {
        LobbyInsights.filter(games, filter, query, favorites, stats, nowMs)
    }
    var shown by remember { mutableStateOf(!animations) }
    LaunchedEffect(Unit) { shown = true }
    val entrance by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(MaxDuration.standard),
        label = "exploreEntrance"
    )
    // الريموت/الكيبورد: تركيز واحد على الطبقة، ومؤشّر يتنقّل الشبكة بلا لمس. المسرح يستعمل
    // النمط نفسه، فتبقى الأسهم وEnter تعمل هنا كما تعمل هناك.
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val gridState = rememberLazyGridState()
    // الحلقة البصرية تخصّ الريموت وحده: مستخدم اللمس لا يرى مؤشّرًا اختاره غيره.
    var cursorActive by remember { mutableStateOf(false) }
    // يبدأ المؤشّر على اللعبة المعروضة، ويعود إلى أول نتيجة عند تغيير البحث أو المرشّح.
    var cursor by remember(filter, query) {
        val target = visible.indexOfFirst { it.packageName == currentPackage }
        mutableStateOf(if (target >= 0) target else 0)
    }
    // المؤشّر يقود الشبكة: كل حركة تُمرّر البطاقة إلى المرأى، فلا يضيع التركيز خارج الشاشة.
    LaunchedEffect(cursor, visible.size) {
        if (cursor > visible.lastIndex) cursor = visible.lastIndex.coerceAtLeast(0)
        if (cursor in visible.indices) gridState.animateScrollToItem(cursor)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LobbyPalette.Black.copy(alpha = 0.92f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .graphicsLayer {
                alpha = entrance
                translationY = (1f - entrance) * 24.dp.toPx()
            }
            .displayCutoutPadding()
            .padding(top = MaxSpace.xxl)
    ) {
        // عدد الأعمدة كما تحسبه الشبكة نفسها (المساحة ناقص الحافتين، والفجوة بين عمودين).
        val columns = ((maxWidth - MaxSpace.xl * 2 + MaxSpace.md) / (ExploreColumnMin + MaxSpace.md))
            .toInt()
            .coerceAtLeast(1)
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        false
                    } else {
                        cursorActive = true
                        val last = visible.lastIndex
                        when (event.key) {
                            Key.DirectionRight -> if (last >= 0) {
                                cursor = (cursor + if (rtl) -1 else 1).coerceIn(0, last)
                                true
                            } else false
                            Key.DirectionLeft -> if (last >= 0) {
                                cursor = (cursor + if (rtl) 1 else -1).coerceIn(0, last)
                                true
                            } else false
                            Key.DirectionDown -> if (last >= 0) {
                                cursor = (cursor + columns).coerceAtMost(last)
                                true
                            } else false
                            Key.DirectionUp -> if (last >= 0) {
                                cursor = (cursor - columns).coerceAtLeast(0)
                                true
                            } else false
                            Key.DirectionCenter, Key.Enter, Key.ButtonA -> {
                                val choice = visible.getOrNull(cursor)
                                if (choice == null) {
                                    false
                                } else {
                                    onSelect(choice)
                                    true
                                }
                            }
                            Key.Escape -> {
                                onClose()
                                true
                            }
                            else -> false
                        }
                    }
                }
                .focusRequester(focus)
                .focusable()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaxSpace.xl, vertical = MaxSpace.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.lobby_explore_title),
                    color = LobbyPalette.Ink,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.lobby_explore_count, games.size, visible.size),
                    color = LobbyPalette.Muted,
                    fontSize = 12.sp
                )
            }
            LobbyRoundButton(
                icon = Icons.Rounded.Close,
                description = stringResource(R.string.lobby_explore_close),
                onClick = onClose,
                tone = accent
            )
        }

        val searchShape = RoundedCornerShape(maxRadius(MaxRadius.row))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaxSpace.xl)
                .heightIn(min = MaxSize.minTouchTarget)
                .clip(searchShape)
                .background(LobbyPalette.Panel)
                .border(MaxSize.hairlineBorder, accent.copy(alpha = 0.35f), searchShape)
                .padding(horizontal = MaxSpace.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = LobbyPalette.Muted
            )
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.lobby_explore_search),
                        color = LobbyPalette.Muted,
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = LobbyPalette.Ink, fontSize = 15.sp),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        // الأسهم داخل الحقل للكتابة؛ والنزول منه يعيد التحكّم للشبكة بلا لمس.
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) {
                                false
                            } else when (event.key) {
                                Key.DirectionDown, Key.DirectionUp -> {
                                    focus.requestFocus()
                                    true
                                }
                                Key.Enter -> {
                                    val first = visible.firstOrNull()
                                    if (first == null) {
                                        false
                                    } else {
                                        onSelect(first)
                                        true
                                    }
                                }
                                else -> false
                            }
                        }
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(MaxSize.minTouchTarget)
                        .clip(CircleShape)
                        .clickable { query = "" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.lobby_explore_clear),
                        tint = LobbyPalette.Muted
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = MaxSpace.xl, vertical = MaxSpace.md),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
        ) {
            LobbyFilter.entries.forEach { item ->
                FilterChip(
                    label = filterLabel(item),
                    count = counts[item] ?: 0,
                    selected = filter == item,
                    accent = accent,
                    onClick = { filter = item }
                )
            }
        }

        if (visible.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MaxSpace.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SearchOff,
                    contentDescription = null,
                    tint = LobbyPalette.Muted
                )
                val emptyTitle = when {
                    query.isNotBlank() -> stringResource(R.string.lobby_explore_no_match, query.trim())
                    filter == LobbyFilter.Favorites -> stringResource(R.string.lobby_empty_favorites)
                    filter == LobbyFilter.Recent -> stringResource(R.string.lobby_explore_empty_recent)
                    filter == LobbyFilter.Played -> stringResource(R.string.lobby_explore_empty_played)
                    filter == LobbyFilter.Unplayed -> stringResource(R.string.lobby_explore_empty_unplayed)
                    else -> stringResource(R.string.lobby_empty)
                }
                Text(
                    text = emptyTitle,
                    color = LobbyPalette.Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = MaxSpace.md)
                )
                // التلميح وزرّ المسح يخصّان بحثًا لم يطابق؛ مكتبة فارغة تمامًا لا تُقرأ كأنها بحث خاطئ.
                // وحين يكون البحث فارغًا والمرشّح وحده يُخفي النتائج، يُقال ذلك بالمرشّح لا بنصّ البحث.
                if (query.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.lobby_explore_no_match_hint),
                        color = LobbyPalette.Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = MaxSpace.xs)
                    )
                    OutlinedButton(
                        onClick = {
                            query = ""
                            filter = LobbyFilter.All
                        },
                        modifier = Modifier.padding(top = MaxSpace.lg)
                    ) {
                        Text(text = stringResource(R.string.lobby_explore_clear), color = LobbyPalette.Ink)
                    }
                } else if (filter != LobbyFilter.All) {
                    OutlinedButton(
                        onClick = { filter = LobbyFilter.All },
                        modifier = Modifier.padding(top = MaxSpace.lg)
                    ) {
                        Text(text = stringResource(R.string.lobby_explore_show_all), color = LobbyPalette.Ink)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = ExploreColumnMin),
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                contentPadding = PaddingValues(horizontal = MaxSpace.xl, vertical = MaxSpace.md),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.md),
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
            ) {
                itemsIndexed(visible, key = { _, game -> game.packageName }) { index, game ->
                    ExploreCard(
                        game = game,
                        stats = stats[game.packageName],
                        tweaks = tweaks[game.packageName] ?: 0,
                        panelArmed = game.packageName in armed,
                        selected = game.packageName == currentPackage,
                        focused = cursorActive && index == cursor,
                        accent = accent,
                        nowMs = nowMs,
                        onClick = {
                            cursor = index
                            onSelect(game)
                        }
                    )
                }
            }
        }
            }
        }
    }
}

@Composable
private fun filterLabel(filter: LobbyFilter): String = when (filter) {
    LobbyFilter.All -> stringResource(R.string.lobby_explore_filter_all)
    LobbyFilter.Favorites -> stringResource(R.string.lobby_explore_filter_favorites)
    LobbyFilter.Recent -> stringResource(R.string.lobby_explore_filter_recent)
    LobbyFilter.Played -> stringResource(R.string.lobby_explore_filter_played)
    LobbyFilter.Unplayed -> stringResource(R.string.lobby_explore_filter_unplayed)
}

@Composable
private fun FilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.pill))
    Row(
        modifier = Modifier
            .heightIn(min = MaxSize.minTouchTarget)
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.22f) else LobbyPalette.PanelRaised.copy(alpha = 0.55f))
            .border(
                MaxSize.hairlineBorder,
                if (selected) accent.copy(alpha = 0.55f) else LobbyPalette.Hairline,
                shape
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = MaxSpace.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        Text(
            text = label,
            color = if (selected) LobbyPalette.Ink else LobbyPalette.Muted,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
        Text(
            text = count.toString(),
            color = if (selected) accent else LobbyPalette.Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun ExploreCard(
    game: GameApp,
    stats: GamePlayStats?,
    tweaks: Int,
    panelArmed: Boolean,
    selected: Boolean,
    focused: Boolean,
    accent: Color,
    nowMs: Long,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(MaxDuration.instant),
        label = "exploreCardScale"
    )
    val ready = remember(stats, tweaks, panelArmed, nowMs) {
        LobbyInsights.readyCount(
            LobbyInsights.setup(
                stats = stats,
                tweaks = tweaks,
                panelArmed = panelArmed,
                nowMs = nowMs
            )
        )
    }
    val shape = RoundedCornerShape(maxRadius(MaxRadius.tile))
    val playLine = stats?.let { lobbyPlayTimeText(it.totalMs) }
        ?: stringResource(R.string.lobby_explore_never)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (focused) LobbyPalette.PanelRaised.copy(alpha = 0.72f) else LobbyPalette.Panel)
            .border(
                if (focused || selected) MaxSize.activeRing else MaxSize.hairlineBorder,
                when {
                    focused -> accent
                    selected -> accent.copy(alpha = 0.55f)
                    else -> LobbyPalette.Hairline
                },
                shape
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(MaxSpace.lg),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
        ) {
            AppIconImage(
                packageName = game.packageName,
                size = MaxSize.iconContainer,
                contentDescription = null
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.label,
                    color = LobbyPalette.Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playLine,
                    color = LobbyPalette.Muted,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            if (selected) {
                Text(
                    text = stringResource(R.string.lobby_explore_current),
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
            MetaChip(text = stringResource(R.string.lobby_explore_ready, ready))
            MetaChip(
                text = if (tweaks > 0) {
                    stringResource(R.string.lobby_tile_profile_value, tweaks)
                } else {
                    stringResource(R.string.lobby_tile_profile_none)
                }
            )
            MetaChip(
                text = if (panelArmed) {
                    stringResource(R.string.lobby_explore_panel_on)
                } else {
                    stringResource(R.string.lobby_explore_panel_off)
                }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
            repeat(LobbyInsights.SETUP_TOTAL) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (index < ready) accent else LobbyPalette.PanelRaised)
                )
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Text(
        text = text,
        color = LobbyPalette.Muted,
        fontSize = 11.sp,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(maxRadius(MaxRadius.pill)))
            .background(LobbyPalette.PanelRaised.copy(alpha = 0.55f))
            .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.hairline)
    )
}
