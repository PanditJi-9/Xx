/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    val cpu by produceState<Int?>(null) {
        while (true) {
            value = PanelCpuLoad.sample()
            delay(2_000L)
        }
    }
    val gpu by produceState<Int?>(null) {
        while (true) {
            value = HudDeepReaders.gpuLoadPercent()
            delay(2_000L)
        }
    }
    val socTemp by produceState<Float?>(null) {
        while (true) {
            value = HudDeepReaders.socTempC()
            delay(2_000L)
        }
    }
    val heat = socTemp ?: batteryTempC

    val selectedIndex = remember(games, selected) {
        games.indexOfFirst { it.packageName == selected?.packageName }.coerceAtLeast(0)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier.fillMaxSize()) {
            val wide = maxWidth >= 720.dp
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .displayCutoutPadding()
                    .padding(top = TopBarHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameSpaceRail(
                    wide = wide,
                    selected = selected,
                    onHome = onHome,
                    onSection = onOpenSection
                )
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        // بطل اللعبة: الاسم الكبير، ثم زرّ التشغيل وخياراته، ثم نقاط الاختيار.
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = MaxSpace.xxl)
                                .widthIn(max = 420.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(MaxSpace.md)
                        ) {
                            Text(
                                text = selected?.label.orEmpty(),
                                color = GameSpacePalette.Ink,
                                fontSize = if (wide) 44.sp else 32.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            LaunchRow(
                                enabled = selected != null,
                                onLaunch = onLaunch,
                                onOptions = onOptions
                            )
                            GameSpaceDots(count = games.size.coerceAtMost(9), index = selectedIndex)
                        }
                        if (wide) {
                            PerformanceBoard(
                                cpu = cpu,
                                gpu = gpu,
                                heat = heat,
                                batteryPercent = batteryPercent,
                                charging = charging,
                                panelEnabled = panelEnabled,
                                onOpen = onOptions,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = MaxSpace.md, end = MaxSpace.xxl)
                            )
                        }
                    }
                    if (!wide) {
                        PerformanceBoard(
                            cpu = cpu,
                            gpu = gpu,
                            heat = heat,
                            batteryPercent = batteryPercent,
                            charging = charging,
                            panelEnabled = panelEnabled,
                            onOpen = onOptions,
                            modifier = Modifier.padding(horizontal = MaxSpace.gutter)
                        )
                    }
                    GameSpaceShelf(
                        games = games,
                        selected = selected,
                        onSelect = onSelect,
                        onNext = { games.getOrNull(selectedIndex + 1)?.let { onSelect(it.packageName) } },
                        onPrev = { games.getOrNull(selectedIndex - 1)?.let { onSelect(it.packageName) } },
                        onViewAll = onOpenLibrary
                    )
                    GameSpaceBase(
                        title = statusTitle,
                        detail = statusDetail,
                        percent = readinessPercent,
                        onOpen = onOpenSessions
                    )
                }
            }
        }
    }
}

/** الشريط الجانبيّ: شعار، ثم الأقسام، وآخرها سهم فتح مركز التحكّم. */
@Composable
private fun GameSpaceRail(
    wide: Boolean,
    selected: GameApp?,
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
                fontSize = 11.sp,
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        val shape = RoundedCornerShape(maxRadius(MaxRadius.pill))
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .widthIn(min = 220.dp)
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(GameSpacePalette.Violet, GameSpacePalette.VioletDeep)
                    )
                )
                .border(MaxSize.hairlineBorder, GameSpacePalette.VioletSoft, shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onLaunch)
                .padding(horizontal = MaxSpace.xl),
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
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
    Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
        repeat(count) { i ->
            val active = i == index
            Box(
                modifier = Modifier
                    .height(MaxSpace.xs)
                    .width(if (active) MaxSpace.lg else MaxSpace.xs)
                    .clip(RoundedCornerShape(maxRadius(MaxRadius.pill)))
                    .background(if (active) GameSpacePalette.VioletSoft else GameSpacePalette.Track)
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
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.group))
    Column(
        modifier = modifier
            .widthIn(max = BoardWidth)
            .width(BoardWidth)
            .clip(shape)
            .background(GameSpacePalette.Panel.copy(alpha = 0.88f))
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
            .padding(MaxSpace.md),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onOpen),
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
        Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
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
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
        ) {
            Icon(
                imageVector = Icons.Rounded.BatteryFull,
                contentDescription = null,
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
            Text(
                text = stringResource(R.string.gs_battery),
                color = GameSpacePalette.Muted,
                fontSize = 11.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = GameSpacePalette.Magenta,
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
            Text(
                text = stringResource(R.string.gs_perf_mode),
                color = GameSpacePalette.Muted,
                fontSize = 11.sp,
                maxLines = 1
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
    Column(
        modifier = modifier,
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
                        sweepAngle = 360f * progress.coerceIn(0f, 1f),
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
            fontSize = 10.sp,
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
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onViewAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaxSpace.sm),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaxSpace.xxl),
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
                    .clip(RoundedCornerShape(maxRadius(MaxRadius.pill)))
                    .clickable(role = Role.Button, onClick = onViewAll)
                    .padding(horizontal = MaxSpace.xs, vertical = MaxSpace.xs),
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
            ShelfArrow(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                description = stringResource(R.string.gs_prev),
                onClick = onPrev
            )
            LazyRow(
                modifier = Modifier.weight(1f),
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
            ShelfArrow(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                description = stringResource(R.string.gs_next),
                onClick = onNext
            )
        }
    }
}

@Composable
private fun ShelfArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(MaxSize.minTouchTarget)
            .clip(CircleShape)
            .background(GameSpacePalette.Panel.copy(alpha = 0.85f))
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
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
    Column(
        modifier = Modifier
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
            fontSize = 10.sp,
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
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.control))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaxSpace.xxl, vertical = MaxSpace.sm)
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
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Canvas(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .width(220.dp)
                .height(MaxSpace.sm)
        ) {
            val count = 28
            val gap = size.width * 0.006f
            val segW = (size.width - gap * (count - 1)) / count
            val filled = (count * percent / 100f).roundToInt()
            for (i in 0 until count) {
                drawRoundRect(
                    color = if (i < filled) GameSpacePalette.VioletSoft else GameSpacePalette.Track,
                    topLeft = Offset(i * (segW + gap), 0f),
                    size = Size(segW, size.height),
                    cornerRadius = CornerRadius(size.height / 2f)
                )
            }
        }
        Text(
            text = "$percent%",
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
