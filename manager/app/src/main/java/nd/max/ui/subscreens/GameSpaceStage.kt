/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.core.gamespace.PanelCpuLoad
import nd.max.core.platform.HudDeepReaders
import nd.max.ui.component.rememberLobbyIcon
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import nd.max.ui.design.MaxAlpha
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius

/*
 * المسرح — تصميم المالك نفسه، لا محاكاةً لشكل تطبيق آخر.
 *
 * اللقطة (١٦٧٢×٩٤١) تقرأ هكذا: شريط أقسام على الحافة اليسرى بشعار في أعلاه، وشعار الكلمة وساعة
 * الجهاز في الشريط العلويّ، وبطل اللعبة في الوسط مع زرّ تشغيل كبير ونقاط تنقّل، ولوحة أداء على
 * اليمين بثلاث حلقات (معالج · رسوميات · حرارة) وصفّي بطارية ووضع أداء، ورفّ ألعاب أفقيّ، وقاعدة
 * حالة في الأسفل بشريط نسبة مجزّأ.
 *
 * **والاتجاه مثبّت LTR لهذه الشاشة وحدها** لأن اللقطة تُظهر الشريط يسارًا والشعار يسارًا والبطاقة
 * يمينًا مع نصوص عربيّة. وقلب الاتجاه في العربية يبدّل الهندسة كلها فتخرج الشاشة غير المرسومة.
 *
 * ### ما لا يُدّعى (ADR-07)
 *
 * لا شعار لعبة ولا فنّ لها: اللقطة تحمل فنّ PUBG وهو ليس ملكنا ولا يأتي مع `PackageManager`؛
 * فالمكان يأخذ اسم اللعبة ونقشة التطبيق المموّهة القائمة. والأرقام تُقرأ فعلًا: حمل المعالج من
 * `PanelCpuLoad`، وحمل الرسوميات من `HudDeepReaders.gpuLoadPercent`، والحرارة من `socTempC`
 * (أو حرارة البطارية عند غيابها). وما لم يُقرأ يبقى «—» وحلقةً بلا قوس، لا صفرًا.
 */

/** عرض الشريط: ٩٢dp على العرض الواسع (≈ ٢٢٨px في اللقطة)، و٦٨dp على الهاتف فلا يأكل المسرح. */
private val RailWide = 92.dp
private val RailNarrow = 68.dp

/** لوحة الأداء: ٣٢٠dp ≈ ٣٨٤px في اللقطة. */
private val BoardWidth = 320.dp

/** قطر الحلقة: ٨٨dp ≈ ١٠٥px. */
private val RingSize = 88.dp

/** بطاقة الرفّ: ١٢٤dp × ١٢٤dp للنقشة. */
private val ShelfCard = 124.dp

@Composable
internal fun GameSpaceStage(
    games: List<GameApp>,
    selected: GameApp?,
    readinessPercent: Int,
    statusTitle: String,
    statusDetail: String,
    panelEnabled: Boolean,
    batteryPercent: Int?,
    batteryTempC: Float?,
    charging: Boolean,
    onSelect: (String) -> Unit,
    onLaunch: () -> Unit,
    onOptions: () -> Unit,
    onHome: () -> Unit,
    onOpenSection: (LobbySection) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSessions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // القراءات الحيّة: نفس القارئين اللذين يغذّيان مراقب الجهاز، فلا مقياس ثانٍ.
    // مؤقّت واحد للثلاثة معًا: حلقة واحدة تُقاس مرّة كل ثانيتين، لا ثلاث حلقات تتصارع.
    val readings by produceState(initialValue = LiveReadings()) {
        while (true) {
            value = LiveReadings(
                cpu = PanelCpuLoad.sample(),
                gpu = HudDeepReaders.gpuLoadPercent(),
                tempC = HudDeepReaders.socTempC()
            )
            delay(2_000L)
        }
    }
    val cpu = readings.cpu
    val gpu = readings.gpu
    val heat = readings.tempC ?: batteryTempC

    val selectedIndex = remember(games, selected) {
        games.indexOfFirst { it.packageName == selected?.packageName }.coerceAtLeast(0)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier.fillMaxSize()) {
            val wide = maxWidth >= 720.dp
            val sideBoard = maxWidth >= 1000.dp
            val compactInset = MaxSpace.gutter
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .displayCutoutPadding()
                    .padding(top = TopBarHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameSpaceRail(wide = wide, onHome = onHome, onSection = onOpenSection)
                if (!wide) {
                    // الهاتف: عمود واحد يتمرّر رأسيًّا. اللوحة والرفّ والقاعدة تحت البطل، لا حوله.
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = MaxSpace.sm),
                        verticalArrangement = Arrangement.spacedBy(MaxSpace.md)
                    ) {
                        HeroBlock(
                            games = games,
                            selected = selected,
                            selectedIndex = selectedIndex,
                            wide = false,
                            onLaunch = onLaunch,
                            onOptions = onOptions,
                            onOpenLibrary = onOpenLibrary,
                            modifier = Modifier.padding(horizontal = compactInset)
                        )
                        PerformanceBoard(
                            cpu = cpu,
                            gpu = gpu,
                            heat = heat,
                            batteryPercent = batteryPercent,
                            charging = charging,
                            panelEnabled = panelEnabled,
                            onOpen = onOptions,
                            fullWidth = true,
                            modifier = Modifier.padding(horizontal = compactInset)
                        )
                        if (games.isNotEmpty()) {
                            GameSpaceShelf(
                                games = games,
                                selected = selected,
                                selectedIndex = selectedIndex,
                                arrows = false,
                                onSelect = onSelect,
                                onNext = { games.getOrNull(selectedIndex + 1)?.let { onSelect(it.packageName) } },
                                onPrev = { games.getOrNull(selectedIndex - 1)?.let { onSelect(it.packageName) } },
                                onViewAll = onOpenLibrary
                            )
                        }
                        GameSpaceBase(
                            title = statusTitle,
                            detail = statusDetail,
                            percent = readinessPercent,
                            onOpen = onOpenSessions,
                            inset = compactInset
                        )
                    }
                } else {
                    // الجهاز الواسع: الهندسة كما في اللقطة — البطل يسارًا واللوحة يمينًا، والقاعدة ملتصقة.
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            HeroGlow(
                                letter = selected?.label?.trim()?.take(1).orEmpty(),
                                wide = true
                            )
                            HeroBlock(
                                games = games,
                                selected = selected,
                                selectedIndex = selectedIndex,
                                wide = true,
                                onLaunch = onLaunch,
                                onOptions = onOptions,
                                onOpenLibrary = onOpenLibrary,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = MaxSpace.xxl)
                            )
                            if (sideBoard) {
                                PerformanceBoard(
                                    cpu = cpu,
                                    gpu = gpu,
                                    heat = heat,
                                    batteryPercent = batteryPercent,
                                    charging = charging,
                                    panelEnabled = panelEnabled,
                                    onOpen = onOptions,
                                    fullWidth = false,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = MaxSpace.md, end = MaxSpace.xxl)
                                )
                            }
                        }
                        if (!sideBoard) {
                            PerformanceBoard(
                                cpu = cpu,
                                gpu = gpu,
                                heat = heat,
                                batteryPercent = batteryPercent,
                                charging = charging,
                                panelEnabled = panelEnabled,
                                onOpen = onOptions,
                                fullWidth = true,
                                modifier = Modifier.padding(horizontal = MaxSpace.xxl)
                            )
                        }
                        if (games.isNotEmpty()) {
                            GameSpaceShelf(
                                games = games,
                                selected = selected,
                                selectedIndex = selectedIndex,
                                arrows = true,
                                onSelect = onSelect,
                                onNext = { games.getOrNull(selectedIndex + 1)?.let { onSelect(it.packageName) } },
                                onPrev = { games.getOrNull(selectedIndex - 1)?.let { onSelect(it.packageName) } },
                                onViewAll = onOpenLibrary
                            )
                        }
                        GameSpaceBase(
                            title = statusTitle,
                            detail = statusDetail,
                            percent = readinessPercent,
                            onOpen = onOpenSessions,
                            inset = MaxSpace.xxl
                        )
                    }
                }
            }
        }
    }
}

/** القراءات الثلاث في قراءة واحدة، فالمؤقّت واحد. */
private data class LiveReadings(
    val cpu: Int? = null,
    val gpu: Int? = null,
    val tempC: Float? = null,
)

/**
 * البطل: اسم اللعبة الكبير أو — إن لم تكن هناك لعبة — بطاقة فراغ تقود إلى المكتبة.
 * الفراغ هنا ليس «٠٪» ولا زرًّا معطّلًا: جملة تقول ما يحدث وخطوة واحدة تفعله.
 */
@Composable
private fun HeroBlock(
    games: List<GameApp>,
    selected: GameApp?,
    selectedIndex: Int,
    wide: Boolean,
    onLaunch: () -> Unit,
    onOptions: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (games.isEmpty()) {
        EmptyHero(onOpenLibrary = onOpenLibrary, modifier = modifier)
    } else {
        HeroCopy(
            games = games,
            selected = selected,
            selectedIndex = selectedIndex,
            wide = wide,
            onLaunch = onLaunch,
            onOptions = onOptions,
            modifier = modifier
        )
    }
}

@Composable
private fun HeroCopy(
    games: List<GameApp>,
    selected: GameApp?,
    selectedIndex: Int,
    wide: Boolean,
    onLaunch: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        Crossfade(
            targetState = selected?.label.orEmpty(),
            animationSpec = tween(durationMillis = 260)
        ) { label ->
            Text(
                text = label,
                color = GameSpacePalette.Ink,
                fontSize = if (wide) 44.sp else 32.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        LaunchRow(
            enabled = selected != null,
            onLaunch = onLaunch,
            onOptions = onOptions
        )
        GameSpaceDots(
            count = games.size,
            index = if (selected == null) -1 else selectedIndex
        )
    }
}

@Composable
private fun EmptyHero(onOpenLibrary: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.group))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(shape)
                .background(GameSpacePalette.Panel.copy(alpha = 0.9f))
                .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.VideogameAsset,
                contentDescription = null,
                tint = GameSpacePalette.VioletSoft,
                modifier = Modifier.size(MaxSize.iconGlyph)
            )
        }
        Text(
            text = stringResource(R.string.gs_empty_title),
            color = GameSpacePalette.Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.gs_empty_body),
            color = GameSpacePalette.Muted,
            fontSize = 13.sp
        )
        val cta = RoundedCornerShape(maxRadius(MaxRadius.pill))
        Row(
            modifier = Modifier
                .heightIn(min = MaxSize.minTouchTarget)
                .shadow(
                    elevation = 8.dp,
                    shape = cta,
                    ambientColor = GameSpacePalette.Violet.copy(alpha = 0.30f),
                    spotColor = GameSpacePalette.Violet.copy(alpha = 0.40f)
                )
                .clip(cta)
                .background(
                    Brush.horizontalGradient(
                        listOf(GameSpacePalette.Violet, GameSpacePalette.VioletDeep)
                    )
                )
                .border(MaxSize.hairlineBorder, GameSpacePalette.VioletSoft, cta)
                .clickable(role = Role.Button, onClick = onOpenLibrary)
                .padding(horizontal = MaxSpace.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            Icon(
                imageVector = Icons.Rounded.VideogameAsset,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
            Text(
                text = stringResource(R.string.gs_empty_action),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/** هالة خلفيّة وحرف اللعبة شبه شفّاف: يملأ مكان الفنّ الذي لا نملكه بلا ادّعاء أنه فنّها. */
@Composable
private fun HeroGlow(letter: String, wide: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GameSpacePalette.Violet.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.22f, size.height * 0.5f),
                        radius = size.minDimension * 0.95f
                    ),
                    radius = size.minDimension * 0.95f,
                    center = Offset(size.width * 0.22f, size.height * 0.5f)
                )
            }
    ) {
        if (letter.isNotEmpty()) {
            Text(
                text = letter,
                color = GameSpacePalette.VioletSoft.copy(alpha = 0.06f),
                fontSize = if (wide) 240.sp else 150.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = MaxSpace.md)
            )
        }
    }
}

/** الشريط الجانبيّ: شعار، ثم الأقسام، وآخرها سهم فتح مركز التحكّم. */
@Composable
private fun GameSpaceRail(
    wide: Boolean,
    onHome: () -> Unit,
    onSection: (LobbySection) -> Unit,
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.group))
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (wide) RailWide else RailNarrow)
            .padding(horizontal = MaxSpace.xs, vertical = MaxSpace.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Box(
            modifier = Modifier
                .size(if (wide) 56.dp else 44.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(GameSpacePalette.Violet, GameSpacePalette.VioletDeep)
                    )
                )
                .border(MaxSize.hairlineBorder, GameSpacePalette.VioletSoft.copy(alpha = MaxAlpha.borderStrong), shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.gs_brand_mark),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.height(MaxSpace.xs))
        GameSpaceRailItem(
            labelRes = R.string.gs_nav_home,
            icon = Icons.Rounded.Home,
            active = true,
            wide = wide,
            onClick = onHome
        )
        RailSection.entries.forEach { entry ->
            GameSpaceRailItem(
                labelRes = entry.labelRes,
                icon = entry.section.icon,
                active = false,
                wide = wide,
                onClick = { onSection(entry.section) }
            )
        }
        Spacer(Modifier.weight(1f))
        // السهم في اللقطة يفتح مركز التحكّم؛ وهو الفعل نفسه هنا، لا زخرفة.
        Box(
            modifier = Modifier
                .heightIn(min = MaxSize.minTouchTarget)
                .fillMaxWidth()
                .clip(RoundedCornerShape(maxRadius(MaxRadius.pill)))
                .clickable(role = Role.Button) { onSection(LobbySection.Lobby) }
                .padding(vertical = MaxSpace.xs),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.lobby_tab_control),
                tint = GameSpacePalette.Muted,
                modifier = Modifier.size(MaxSize.iconGlyph)
            )
        }
    }
}

/**
 * الأقسام الخمسة كما سمّاها التصميم. 「المزايا」 هو ملفّ اللعبة (التعديلات)، و「الأدوات」 هو لوحة
 * الجانب، و「الإعدادات」 هو إعدادات اللوبي — لا أقسام جديدة، بل أسماء الشاشة نفسها.
 */
private enum class RailSection(val labelRes: Int, val section: LobbySection) {
    Library(R.string.gs_nav_games, LobbySection.Library),
    Performance(R.string.gs_nav_perf, LobbySection.Insights),
    Tools(R.string.gs_nav_tools, LobbySection.Panel),
    Features(R.string.gs_nav_features, LobbySection.Loadout),
    Settings(R.string.gs_nav_settings, LobbySection.Lobby)
}

@Composable
private fun GameSpaceRailItem(
    labelRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    wide: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.control))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaxSize.minTouchTarget)
            .clip(shape)
            .background(
                if (active) {
                    Brush.horizontalGradient(
                        listOf(GameSpacePalette.Violet, GameSpacePalette.VioletDeep)
                    )
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
            .border(
                MaxSize.hairlineBorder,
                if (active) GameSpacePalette.VioletSoft else Color.Transparent,
                shape
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (wide) MaxSpace.sm else MaxSpace.xs, vertical = MaxSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) Color.White else GameSpacePalette.Muted,
            modifier = Modifier.size(MaxSize.iconGlyphSmall)
        )
        if (wide) {
            Text(
                text = stringResource(labelRes),
                color = if (active) Color.White else GameSpacePalette.Muted,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** زرّ التشغيل الكبير + «...» كما في اللقطة. */
@Composable
private fun LaunchRow(enabled: Boolean, onLaunch: () -> Unit, onOptions: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        val shape = RoundedCornerShape(maxRadius(MaxRadius.pill))
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = shape,
                    ambientColor = GameSpacePalette.Violet.copy(alpha = 0.35f),
                    spotColor = GameSpacePalette.Violet.copy(alpha = 0.45f)
                )
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(GameSpacePalette.Violet, GameSpacePalette.VioletDeep)
                    )
                )
                .border(MaxSize.hairlineBorder, GameSpacePalette.VioletSoft, shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onLaunch)
                .padding(horizontal = MaxSpace.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(MaxSize.iconGlyph)
            )
            Text(
                text = stringResource(R.string.gs_launch),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(MaxSize.minTouchTarget)
                .clip(CircleShape)
                .background(GameSpacePalette.Panel.copy(alpha = 0.9f))
                .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, CircleShape)
                .clickable(role = Role.Button, onClick = onOptions),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = stringResource(R.string.gs_more),
                tint = GameSpacePalette.Ink,
                modifier = Modifier.size(MaxSize.iconGlyph)
            )
        }
    }
}

@Composable
private fun GameSpaceDots(count: Int, index: Int) {
    if (count <= 1) return
    val window = 7
    val start = (index - window / 2).coerceIn(0, (count - window).coerceAtLeast(0))
    val end = (start + window).coerceAtMost(count)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        for (i in start until end) {
            val active = i == index
            Box(
                modifier = Modifier
                    .height(MaxSpace.xs)
                    .width(if (active) MaxSpace.lg else MaxSpace.xs)
                    .clip(RoundedCornerShape(maxRadius(MaxRadius.pill)))
                    .background(if (active) GameSpacePalette.VioletSoft else GameSpacePalette.Track)
            )
        }
        if (count > window) {
            Text(
                text = "${(index + 1).coerceAtLeast(1)}/$count",
                color = GameSpacePalette.Muted,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

/** لوحة الأداء: ثلاث حلقات، ثم بطارية ووضع الأداء. */
@Composable
private fun PerformanceBoard(
    cpu: Int?,
    gpu: Int?,
    heat: Float?,
    batteryPercent: Int?,
    charging: Boolean,
    panelEnabled: Boolean,
    onOpen: () -> Unit,
    fullWidth: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.group))
    Column(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier.width(BoardWidth))
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = GameSpacePalette.Violet.copy(alpha = 0.25f),
                spotColor = GameSpacePalette.Violet.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(GameSpacePalette.Panel.copy(alpha = 0.88f))
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
            .padding(MaxSpace.md),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(vertical = MaxSpace.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = GameSpacePalette.VioletSoft,
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
            Text(
                text = stringResource(R.string.gs_device_perf),
                color = GameSpacePalette.Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = GameSpacePalette.Muted,
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
        ) {
            PerformanceRing(
                progress = cpu?.let { it / 100f },
                value = cpu?.let { "$it%" } ?: MAX_VALUE_UNAVAILABLE,
                label = stringResource(R.string.gs_cpu),
                tone = GameSpacePalette.VioletSoft,
                modifier = Modifier.weight(1f)
            )
            PerformanceRing(
                progress = gpu?.let { it / 100f },
                value = gpu?.let { "$it%" } ?: MAX_VALUE_UNAVAILABLE,
                label = stringResource(R.string.gs_gpu),
                tone = GameSpacePalette.Cyan,
                modifier = Modifier.weight(1f)
            )
            PerformanceRing(
                // ميزان الحرارة ٣٠–١٠٠°م: القوس مقياس، والرقم هو الخبر.
                progress = heat?.let { ((it - 30f) / 70f).coerceIn(0f, 1f) },
                value = heat?.let { "${it.roundToInt()}°" } ?: MAX_VALUE_UNAVAILABLE,
                label = stringResource(R.string.gs_heat),
                tone = GameSpacePalette.Magenta,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            Icon(
                imageVector = Icons.Rounded.BatteryFull,
                contentDescription = stringResource(R.string.gs_battery),
                tint = if (charging) GameSpacePalette.Cyan else GameSpacePalette.Muted,
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
            Text(
                text = batteryPercent?.let { "$it%" } ?: MAX_VALUE_UNAVAILABLE,
                color = GameSpacePalette.Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = stringResource(R.string.gs_perf_mode),
                tint = if (panelEnabled) GameSpacePalette.Cyan else GameSpacePalette.Muted,
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
            Text(
                text = if (panelEnabled) stringResource(R.string.gs_perf_mode_on) else MAX_VALUE_UNAVAILABLE,
                color = if (panelEnabled) GameSpacePalette.Cyan else GameSpacePalette.Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PerformanceRing(
    progress: Float?,
    value: String,
    label: String,
    tone: Color,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = (progress ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700)
    )
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Box(modifier = Modifier.size(RingSize), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(RingSize)) {
                val dash = PathEffect.dashPathEffect(
                    floatArrayOf(size.minDimension * 0.080f, size.minDimension * 0.050f)
                )
                val stroke = Stroke(width = size.minDimension * 0.085f, cap = StrokeCap.Round, pathEffect = dash)
                drawArc(
                    color = GameSpacePalette.Track,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke
                )
                if (progress != null) {
                    drawArc(
                        color = tone,
                        startAngle = -90f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        style = stroke
                    )
                }
            }
            Text(
                text = value,
                color = if (value == MAX_VALUE_UNAVAILABLE) GameSpacePalette.Muted else GameSpacePalette.Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Text(
            text = label,
            color = GameSpacePalette.Muted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** رفّ الألعاب: بطاقات أفقيّة بسهمين، واللعبة المختارة وحدها تحمل حدًّا مضيئًا. */
@Composable
private fun GameSpaceShelf(
    games: List<GameApp>,
    selected: GameApp?,
    selectedIndex: Int,
    arrows: Boolean,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onViewAll: () -> Unit,
) {
    val listState = rememberLazyListState()
    // اختيارٌ بالأسهم أو النقاط يجب أن يُظهر بطاقته، لا أن يتركها خارج الشاشة.
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in games.indices) listState.animateScrollToItem(selectedIndex)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaxSpace.sm),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (arrows) MaxSpace.xxl else MaxSpace.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            Icon(
                imageVector = Icons.Rounded.VideogameAsset,
                contentDescription = null,
                tint = GameSpacePalette.VioletSoft,
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
            Text(
                text = stringResource(R.string.gs_shelf),
                color = GameSpacePalette.Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Row(
                modifier = Modifier
                    .heightIn(min = MaxSize.minTouchTarget)
                    .clip(RoundedCornerShape(maxRadius(MaxRadius.pill)))
                    .clickable(role = Role.Button, onClick = onViewAll)
                    .padding(horizontal = MaxSpace.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
            ) {
                Text(
                    text = stringResource(R.string.gs_view_all),
                    color = GameSpacePalette.Muted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = GameSpacePalette.Muted,
                    modifier = Modifier.size(MaxSize.iconGlyphSmall)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (arrows) {
                ShelfArrow(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    description = stringResource(R.string.gs_prev),
                    enabled = selectedIndex > 0,
                    onClick = onPrev
                )
            }
            LazyRow(
                modifier = Modifier.weight(1f),
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
                contentPadding = PaddingValues(vertical = MaxSpace.xs)
            ) {
                items(games, key = { it.packageName }) { game ->
                    ShelfCard(
                        game = game,
                        selected = game.packageName == selected?.packageName,
                        onClick = { onSelect(game.packageName) }
                    )
                }
            }
            if (arrows) {
                ShelfArrow(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    description = stringResource(R.string.gs_next),
                    enabled = selectedIndex < games.lastIndex,
                    onClick = onNext
                )
            }
        }
    }
}

@Composable
private fun ShelfArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(MaxSize.minTouchTarget)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(GameSpacePalette.Panel.copy(alpha = 0.85f))
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = GameSpacePalette.Ink,
            modifier = Modifier.size(MaxSize.iconGlyph)
        )
    }
}

@Composable
private fun ShelfCard(game: GameApp, selected: Boolean, onClick: () -> Unit) {
    val icon by rememberLobbyIcon(game.packageName)
    val shape = RoundedCornerShape(maxRadius(MaxRadius.group))
    val lift by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = tween(durationMillis = 220)
    )
    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = lift
                scaleY = lift
            }
            .width(ShelfCard)
            .clip(shape)
            .background(GameSpacePalette.Panel.copy(alpha = 0.75f))
            .border(
                if (selected) 2.dp else MaxSize.hairlineBorder,
                if (selected) GameSpacePalette.VioletSoft else GameSpacePalette.Hairline,
                shape
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(MaxSpace.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ShelfCard)
                .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
                .background(GameSpacePalette.Deep),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                val bitmap = icon
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Text(
                    text = game.label.take(1),
                    color = GameSpacePalette.Ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = game.label,
            color = if (selected) GameSpacePalette.Ink else GameSpacePalette.Muted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** قاعدة الحالة: حكم آخر جلسة أو الجاهزية، وشريط نسبة مجزّأ — النسبة هي الجاهزية الحقيقيّة. */
@Composable
private fun GameSpaceBase(
    title: String,
    detail: String,
    percent: Int,
    onOpen: () -> Unit,
    inset: Dp = MaxSpace.md,
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.control))
    val target = percent.coerceIn(0, 100)
    val animated by animateFloatAsState(
        targetValue = target.toFloat(),
        animationSpec = tween(durationMillis = 700)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = inset, vertical = MaxSpace.sm)
            .heightIn(min = GameSpaceStatusHeight)
            .clip(shape)
            .background(GameSpacePalette.Deep.copy(alpha = 0.92f))
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = MaxSpace.md, vertical = MaxSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        Icon(
            imageVector = Icons.Rounded.Bolt,
            contentDescription = null,
            tint = GameSpacePalette.VioletSoft,
            modifier = Modifier.size(MaxSize.iconGlyph)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)
        ) {
            Text(
                text = title,
                color = GameSpacePalette.Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                color = GameSpacePalette.Muted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Canvas(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 260.dp)
                .height(MaxSpace.sm)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(target / 100f, 0f..1f)
                }
        ) {
            val count = 28
            val gap = size.width * 0.006f
            val segW = (size.width - gap * (count - 1)) / count
            val filled = (count * animated / 100f).roundToInt()
            val fill = Brush.horizontalGradient(
                listOf(GameSpacePalette.VioletSoft, GameSpacePalette.Cyan)
            )
            for (i in 0 until count) {
                val corner = CornerRadius(size.height / 2f)
                val topLeft = Offset(i * (segW + gap), 0f)
                if (i < filled) {
                    drawRoundRect(
                        brush = fill,
                        topLeft = topLeft,
                        size = Size(segW, size.height),
                        cornerRadius = corner
                    )
                } else {
                    drawRoundRect(
                        color = GameSpacePalette.Track,
                        topLeft = topLeft,
                        size = Size(segW, size.height),
                        cornerRadius = corner
                    )
                }
            }
        }
        Text(
            text = "$target%",
            color = GameSpacePalette.Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = GameSpacePalette.Muted,
            modifier = Modifier.size(MaxSize.iconGlyphSmall)
        )
    }
}
