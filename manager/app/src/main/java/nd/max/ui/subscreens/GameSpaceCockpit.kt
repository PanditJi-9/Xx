/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
 * **كوكبيت اللوبي الأفقيّ — تركيبة الأشرطة (جولة ٢٩٦).**
 *
 * ولا أقول إنها «بريميم» ولا إنها تطابق مرجعًا: المرجع الذي رسم المالك على شاشته يعيش ٩٠٪ منه
 * على فنّ اللعبة (ملصق يغطّي الشاشة)، وهذه الشاشة لا تملك ملصقات — كل ما تملكه هو أيقونة التطبيق
 * المثبَّت. فبُنيت على ما هو حقيقيّ: الأيقونة كبيرةً ومُضاءة كمرساة، وخلفها هالة لونيّة ساكنة،
 * وعلى ذلك تُركَّب الأشرطة. وأي «ملصق» مُصنَّع بتدرّج مُموَّه قِيس في نماذج سابقة فقرأ ضبابًا أو
 * لوحًا فارغًا — فلم يُشحن.
 *
 * و**القياس** الذي وُلدت منه هذه التركيبة (نموذج ١:١ بمقاس ٨٥٠×٣٩٢dp، مقيس في متصفّح حقيقيّ):
 * `‏48dp رأس · المشهد 1fr · 104dp رفّ · 56dp تنقّل` ⇒ **صفر تجاوز، صفر تمرير** على هاتف أفقيّ.
 * والتركيبة السابقة (شريط جانبيّ + آلة ٣٤٠dp + رفّ) كانت تحتاج ٤٥٠dp داخل ٣٩٢ ⇒ تمرير داخليّ
 * أو خطّ تحت ١٢sp، وكلاهما مرفوض؛ فحُذفت.
 *
 * **وقواعد ثابتة:** كل هدف لمس ≥ `MaxSize.minTouchTarget` (الرفّ ٨٠dp، عناصر التنقّل ٥٦dp،
 * زرّ القائمة ٤٨dp) · أصغر نصّ ١٢sp · لا كتابة عتاد (ADR-11) · المجهول «—» لا صفر (ADR-07) ·
 * ولا توهّج إلا على زرّ التشغيل. والحركة الوحيدة المسموحة في ملفّات أخرى (`lobbyPress`) ساكنة
 * النهاية ولا تعمل إن أوقف النظام الأنيميشن.
 */
private const val CockpitUnknown = "—"

/** خمسة أبواب في الشريط السفليّ؛ و«الملفّ» و«الإعدادات» في قائمة ⋯ فلا يزدحم الشريط. */
private val CockpitNavPages = listOf(
    LobbyPage.Home,
    LobbyPage.Library,
    LobbyPage.Performance,
    LobbyPage.Tools,
    LobbyPage.Features,
)

/** ارتفاع شريط التنقّل — يحجزه الغلاف أسفل المشهد في مسار الأقسام أيضًا. */
internal val CockpitNavHeight = 56.dp

private val CockpitTopBarHeight = MaxSize.minTouchTarget
private val CockpitShelfHeight = 104.dp
private val CockpitTileSize = 80.dp
private val CockpitTileIcon = 64.dp
private val CockpitTileWidth = 92.dp
private val CockpitHeroIcon = 96.dp
private val CockpitHeroIconInner = 76.dp
private val CockpitBrandRow = 32.dp
private val CockpitNavIndicatorWidth = 28.dp
private val CockpitNavIndicatorHeight = 3.dp

/** عتبة لون الحرارة: تحت ٤٥° يبقى الحياد، وفوقها تحذير — لا لون لمجرّد الزينة. */
private const val CockpitHeatWarn = 45f
private val CockpitHeatTone = Color(0xFFFBBF24)
private val CockpitEdgeLight = Color(0x33FFFFFF)
private val CockpitGloss = Color(0x1AFFFFFF)
private val CockpitBase = Color(0xFF0B0718)
private val CockpitFloor = Color(0xFF05040D)

@Composable
internal fun GameSpaceCockpit(
    data: StageData,
    actions: StageActions,
    @Suppress("UNUSED_PARAMETER") currentPage: LobbyPage,
    @Suppress("UNUSED_PARAMETER") onPage: (LobbyPage) -> Unit,
    topActions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        CockpitBloom(Modifier.matchParentSize())
        Column(modifier = Modifier.fillMaxSize()) {
            CockpitTopBar(
                data = data,
                onSelect = actions.onSelect,
                onOpenSessions = actions.onOpenSessions,
                onPage = onPage,
                topActions = topActions,
            )
            if (data.games.isEmpty()) {
                EmptyHero(
                    onOpenLibrary = actions.onOpenLibrary,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            } else {
                CockpitHero(
                    data = data,
                    actions = actions,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
            CockpitShelf(data = data, actions = actions)
            // شريط التنقّل يملكه الغلاف (`GameLobbyScreen`) فيُرسم مرّة واحدة لكل الصفحات،
            // ويحجز له المشهد ارتفاع `CockpitNavHeight` أسفله — فلا شريط مزدوج ولا تراكب.
        }
    }
}

/**
 * خلفيّة المشهد: قاعدة شبه سوداء + هالة بنفسجيّة ساكنة في زاوية البداية + وهج أرضيّة.
 * كلها `drawBehind` — تُرسم مرّة مع إعادة الرسم ولا تكلّف إطارًا.
 */
@Composable
private fun CockpitBloom(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.drawBehind {
            drawRect(
                Brush.verticalGradient(listOf(CockpitBase, GameSpacePalette.GlassDeep, CockpitFloor))
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(
                        GameSpacePalette.Violet.copy(alpha = MaxAlpha.toneContainerStrong),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.22f, size.height * 0.34f),
                    radius = size.maxDimension * 0.78f,
                )
            )
        }
    )
}

@Composable
private fun CockpitTopBar(
    data: StageData,
    onSelect: (String) -> Unit,
    onOpenSessions: () -> Unit,
    onPage: (LobbyPage) -> Unit,
    topActions: @Composable () -> Unit,
) {
    var overflow by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CockpitTopBarHeight)
            .padding(horizontal = MaxSpace.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
    ) {
        Text(
            text = stringResource(R.string.gs_brand_mark),
            color = GameSpacePalette.VioletSoft,
            fontSize = 22.sp,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            modifier = Modifier.height(CockpitBrandRow),
        )
        CockpitStat(label = stringResource(R.string.gs_cpu), value = data.cpu?.let { "$it%" })
        CockpitStat(label = stringResource(R.string.gs_gpu), value = data.gpu?.let { "$it%" })
        CockpitStat(
            label = stringResource(R.string.gs_heat),
            value = data.heat?.let { "${it.roundToInt()}°" },
            tone = if ((data.heat ?: 0f) >= CockpitHeatWarn) CockpitHeatTone else null,
        )
        CockpitStat(
            label = stringResource(R.string.gs_fps_unit),
            value = data.fps.value?.toString(),
            live = data.fps.live,
        )
        CockpitStat(
            label = stringResource(R.string.gs_battery),
            value = data.batteryPercent?.let { stringResource(R.string.gs_percent_value, it) },
        )
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = data.clock,
                color = GameSpacePalette.Ink,
                fontSize = 15.sp,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (data.date.isNotBlank()) {
                Text(
                    text = data.date,
                    color = GameSpacePalette.Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
        }
        topActions()
        Box {
            CockpitIconButton(
                icon = Icons.Rounded.MoreHoriz,
                description = stringResource(R.string.gs_more),
                selected = overflow,
                onClick = { overflow = true },
            )
            DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(LobbyPage.Profile.labelRes), fontSize = 14.sp) },
                    onClick = {
                        overflow = false
                        onPage(LobbyPage.Profile)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(LobbyPage.Settings.labelRes), fontSize = 14.sp) },
                    onClick = {
                        overflow = false
                        onPage(LobbyPage.Settings)
                    },
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.lobby_insights_sessions),
                            fontSize = 14.sp,
                        )
                    },
                    onClick = {
                        overflow = false
                        onOpenSessions()
                    },
                )
            }
        }
    }
}

@Composable
private fun CockpitStat(
    label: String,
    value: String?,
    tone: Color? = null,
    live: Boolean = false,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value ?: CockpitUnknown,
                color = tone ?: if (value == null) GameSpacePalette.Muted else GameSpacePalette.Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (live) {
                Spacer(modifier = Modifier.width(MaxSpace.xs))
                PulseDot(color = GameSpacePalette.Mint)
            }
        }
        Text(
            text = label,
            color = GameSpacePalette.Muted,
            fontSize = 12.sp,
            maxLines = 1,
        )
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
            .background(
                if (selected) GameSpacePalette.PanelSelected
                else GameSpacePalette.Glass.copy(alpha = MaxAlpha.toneContainer)
            )
            .border(MaxSize.hairlineBorder, CockpitEdgeLight, shape)
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

/** المشهد: الأيقونة مرساةً، والاسم والجاهزية وزرّ اللعب — كلّ حقيقة مرّة واحدة. */
@Composable
private fun CockpitHero(data: StageData, actions: StageActions, modifier: Modifier = Modifier) {
    val ready = data.readinessPercent.coerceIn(0, 100)
    Row(
        modifier = modifier.padding(horizontal = MaxSpace.xl, vertical = MaxSpace.md),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.lg),
    ) {
        CockpitHeroIcon(
            packageName = data.selected?.packageName,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
        ) {
            Text(
                text = data.statusTitle,
                color = GameSpacePalette.VioletSoft,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = data.selected?.label ?: stringResource(R.string.game_space_title),
                color = GameSpacePalette.Ink,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val tagline = data.tagline
            if (!tagline.isNullOrBlank()) {
                Text(
                    text = tagline,
                    color = GameSpacePalette.Muted,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        StageRing(
            progress = ready / 100f,
            value = ready.toString(),
            label = stringResource(R.string.gs_ready_title),
            from = GameSpacePalette.VioletDeep,
            to = GameSpacePalette.VioletSoft,
            warn = false,
        )
        PlayRow(
            enabled = data.selected != null,
            onLaunch = actions.onLaunch,
            onOptions = actions.onOptions,
            modifier = Modifier,
            launching = data.launching,
        )
    }
}

/** الأيقونة كمرساة مادّيّة: لوح داكن، حافة مُضاءة أعلى، ولمعان زجاجيّ ساكن. */
@Composable
private fun CockpitHeroIcon(packageName: String?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(MaxRadius.tile)
    Box(
        modifier = modifier
            .size(CockpitHeroIcon)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(GameSpacePalette.VioletDeep, GameSpacePalette.Glass, CockpitFloor)
                )
            )
            .border(MaxSize.hairlineBorder, CockpitEdgeLight, shape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.46f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(listOf(CockpitGloss, Color.Transparent))
                )
        )
        if (packageName != null) {
            AppIconImage(packageName = packageName, size = CockpitHeroIconInner)
        }
    }
}

/** رفّ الألعاب: مسار اختيار اللعبة الوحيد، وآخره «كل الألعاب». */
@Composable
private fun CockpitShelf(data: StageData, actions: StageActions) {
    if (data.games.isEmpty()) return
    val state = rememberLazyListState()
    Box(modifier = Modifier.fillMaxWidth().height(CockpitShelfHeight)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, GameSpacePalette.GlassDeep.copy(alpha = 0.86f))
                    )
                )
        )
        LazyRow(
            modifier = Modifier.fillMaxSize().padding(horizontal = MaxSpace.xl),
            state = state,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
            verticalAlignment = Alignment.Top,
        ) {
            items(data.games, key = { it.packageName }) { game ->
                CockpitShelfTile(
                    game = game,
                    selected = game.packageName == data.selected?.packageName,
                    onClick = { actions.onSelect(game.packageName) },
                )
            }
            item {
                CockpitSeeAll(onClick = actions.onOpenLibrary)
            }
        }
    }
}

@Composable
private fun CockpitShelfTile(game: GameApp, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(MaxRadius.tile)
    Column(
        modifier = Modifier
            .width(CockpitTileWidth)
            .lobbyPress(interaction, 0.94f)
            .clip(shape)
            .background(if (selected) GameSpacePalette.PanelSelected else Color.Transparent)
            .border(
                width = if (selected) MaxSize.activeRing else MaxSize.hairlineBorder,
                color = if (selected) GameSpacePalette.Lavender else CockpitEdgeLight,
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
        Box(
            modifier = Modifier
                .size(CockpitTileSize - MaxSpace.sm)
                .clip(RoundedCornerShape(MaxRadius.control))
                .background(
                    Brush.linearGradient(
                        listOf(
                            GameSpacePalette.Violet.copy(alpha = MaxAlpha.toneContainer),
                            GameSpacePalette.Glass,
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            AppIconImage(packageName = game.packageName, size = CockpitTileIcon - MaxSpace.md)
        }
        Text(
            text = game.label,
            color = if (selected) GameSpacePalette.Ink else GameSpacePalette.Muted,
            fontSize = 12.sp,
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
    val shape = RoundedCornerShape(MaxRadius.tile)
    Box(
        modifier = Modifier
            .width(CockpitTileWidth)
            .height(CockpitTileSize)
            .lobbyPress(interaction, 0.94f)
            .clip(shape)
            .border(MaxSize.hairlineBorder, CockpitEdgeLight, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.gs_view_all),
            color = GameSpacePalette.VioletSoft,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

/** شريط التنقّل السفليّ: مسار واحد، مؤشّر ساكن على النشط، وهدف ٥٦dp لكل باب. */
@Composable
internal fun CockpitNav(
    current: LobbyPage,
    onPage: (LobbyPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(CockpitNavHeight)
            .background(
                Brush.verticalGradient(
                    listOf(GameSpacePalette.PanelSelected, GameSpacePalette.GlassDeep)
                )
            )
            .padding(horizontal = MaxSpace.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CockpitNavPages.forEach { page ->
            CockpitNavItem(page = page, current = current, onPage = onPage)
        }
    }
}

@Composable
private fun RowScope.CockpitNavItem(
    page: LobbyPage,
    current: LobbyPage,
    onPage: (LobbyPage) -> Unit,
) {
    val active = page == current
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .lobbyPress(interaction, 0.94f)
            .selectable(
                selected = active,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = { onPage(page) },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(CockpitNavIndicatorWidth)
                .height(CockpitNavIndicatorHeight)
                .clip(RoundedCornerShape(MaxRadius.chip))
                .background(if (active) GameSpacePalette.VioletSoft else Color.Transparent)
        )
        Spacer(modifier = Modifier.height(MaxSpace.xs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs),
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = if (active) GameSpacePalette.VioletSoft else GameSpacePalette.Muted,
                modifier = Modifier.size(MaxSize.iconGlyphSmall),
            )
            Text(
                text = stringResource(page.labelRes),
                color = if (active) GameSpacePalette.Ink else GameSpacePalette.Muted,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}
