/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * اللقطة (١٦٧٢×٩٤١) ثلاثة أعمدة رأسية تحت رأس واحد: شريط أقسام على الحافة اليسرى بشعار «M»
 * في أعلاه، ثم عمود المحتوى: رأس بالشعار والساعة، ثم بطل بفنّ اللعبة وزرّ تشغيل، ولوحة أداء
 * على اليمين بثلاث حلقات وبطاقتين، ثم رفّ ألعاب بخمس بطاقات، ثم قاعدة حالة بشريط مجزّأ.
 *
 * **والاتجاه مثبّت LTR لهذه الشاشة وحدها** لأن اللقطة تُظهر الشريط يسارًا والشعار يسارًا
 * والبطاقة يمينًا مع نصوص عربيّة. وقلب الاتجاه في العربية يبدّل الهندسة كلها فتخرج الشاشة غير
 * المرسومة؛ فالمحاذاة هنا صريحة (`TextAlign.End` في الشريط) لا موروثة من الاتجاه.
 *
 * **القياسات نسبيّة لا مطلقة:** الشريط ١٣٫٥٪ من العرض، ولوحة الأداء ٢٤٪، وبطاقة الرفّ بنسبة
 * ١٫٤٣ عرضًا لارتفاع، وقاعدة الحالة ٧٪ من الارتفاع. فاللقطة على أي هاتف أفقيّ تحفظ النِسَب،
 * ولا تُنسخ بكسلاتها كـdp.
 *
 * ### ما لا يُدّعى (ADR-07)
 *
 * لا فنّ منقول: البطل أيقونة اللعبة الحقيقية من `PackageManager`، لا صورة من اللقطة. والأرقام
 * التي تظهر في اللقطة (٣٨٪ و٥٢٪ و٤١°) قيمٌ ثابتة في الصورة، فلا تُكتب هنا لأن الشاشة تقرأ
 * الجهاز مباشرة: المعالج من `PanelCpuLoad`، والرسوميات من `HudDeepReaders.gpuLoadPercent`،
 * والحرارة من `socTempC` (أو حرارة البطارية عند غيابها). وما لم يُقرأ يبقى «—» وحلقةً بلا قوس.
 */

/** عرض الشريط: ١٣٫٥٪ من العرض كما في اللقطة، وحدوده من الهدف اللمسي وحتى ١٢٨dp. */
private val RailWide = 112.dp
private val RailNarrow = 60.dp

/** لوحة الأداء: ٢٤٪ من عرض المسرح، لا تزيد على ٣٢٠dp ولا تنقص عن ٢٣٢dp. */
private val PanelWidthRatio = 0.24f
private val PanelMinWidth = 232.dp
private val PanelMaxWidth = 320.dp

/** قطر الحلقة وحلقة التدريج تحتها. */
private val RingSize = 62.dp

/** ارتفاع بطاقة المكتبة: ٢٠٪ من الارتفاع، بين ٧٢dp و١١٢dp (نسبة اللقطة ١٫٤٣). */
private val CardHeightRatio = 0.20f
private val CardMinHeight = 72.dp
private val CardMaxHeight = 112.dp
private const val CardAspect = 1.43f

/** قاعدة الحالة: ٧٪ من الارتفاع، ولا تنزل عن الهدف اللمسي. */
private val StatusHeightRatio = 0.07f
private val StatusMinHeight = MaxSize.minTouchTarget

/** أدنى عرض لمنطقة البطل تتّسع فيه بطاقة الغلاف دون أن تلامس النصّ. */
private val HeroCoverMinWidth = 420.dp

/** أقصى عرض لكتلة نصّ البطل، فلا يمتدّ السطر على عرض المسرح. */
private val HeroCopyMaxWidth = 460.dp

/** زرّ التشغيل: أدنى عرض، وأقصى عرض، وارتفاعه. */
private val LaunchMinWidth = 190.dp
private val LaunchMaxWidth = 280.dp
private val LaunchHeight = MaxSize.minTouchTarget

/** شعار «M» في أعلى الشريط. */
private val RailMarkSize = 46.dp

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
    header: @Composable () -> Unit = {},
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

    val nextGame: () -> Unit = {
        games.getOrNull(selectedIndex + 1)?.let { onSelect(it.packageName) }
    }
    val prevGame: () -> Unit = {
        games.getOrNull(selectedIndex - 1)?.let { onSelect(it.packageName) }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            GameSpacePalette.Void,
                            GameSpacePalette.Deep,
                            GameSpacePalette.Void
                        )
                    )
                )
        ) {
            // هالتان ثابتتان: بنفسجيّة في وسط المسرح وورديّة أعلى اليمين، كضوء الفنّ في اللقطة.
            Canvas(Modifier.fillMaxSize()) {
                val violet = Offset(size.width * 0.52f, size.height * 0.34f)
                val violetR = size.minDimension * 0.85f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(GameSpacePalette.Violet.copy(alpha = 0.20f), Color.Transparent),
                        center = violet,
                        radius = violetR
                    ),
                    radius = violetR,
                    center = violet
                )
                val pink = Offset(size.width * 0.86f, size.height * 0.06f)
                val pinkR = size.minDimension * 0.55f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(GameSpacePalette.Magenta.copy(alpha = 0.10f), Color.Transparent),
                        center = pink,
                        radius = pinkR
                    ),
                    radius = pinkR,
                    center = pink
                )
            }

            val wide = maxWidth >= 720.dp
            val panelWidth = (maxWidth * PanelWidthRatio)
                .coerceIn(PanelMinWidth, PanelMaxWidth)
            val cardHeight = (maxHeight * CardHeightRatio)
                .coerceIn(CardMinHeight, CardMaxHeight)
            val statusHeight = (maxHeight * StatusHeightRatio)
                .coerceAtLeast(StatusMinHeight)

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .displayCutoutPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameSpaceRail(
                    wide = wide,
                    onHome = onHome,
                    onSection = onOpenSection
                )
                if (!wide) {
                    // الهاتف الطولي: عمود واحد يتمرّر رأسيًّا — نفس العناصر، لا نسخة ثانية منها.
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = MaxSpace.xs),
                        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
                    ) {
                        header()
                        HeroPanel(
                            games = games,
                            selected = selected,
                            selectedIndex = selectedIndex,
                            subtitle = statusDetail,
                            wide = false,
                            onLaunch = onLaunch,
                            onOptions = onOptions,
                            onOpenLibrary = onOpenLibrary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        PerformancePanel(
                            cpu = cpu,
                            gpu = gpu,
                            heat = heat,
                            batteryPercent = batteryPercent,
                            charging = charging,
                            panelEnabled = panelEnabled,
                            onOpen = onOptions,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (games.isNotEmpty()) {
                            LibraryStrip(
                                games = games,
                                selected = selected,
                                selectedIndex = selectedIndex,
                                arrows = false,
                                cardHeight = CardMinHeight,
                                onSelect = onSelect,
                                onNext = nextGame,
                                onPrev = prevGame,
                                onViewAll = onOpenLibrary
                            )
                        }
                        StatusStrip(
                            title = statusTitle,
                            detail = statusDetail,
                            percent = readinessPercent,
                            height = statusHeight,
                            onClick = onOpenSessions
                        )
                    }
                } else {
                    // الهاتف الأفقيّ والجهاز الواسع: الهندسة كما في اللقطة — بطل ولوحة يمينه،
                    // ثم الرفّ بعرض المسرح، ثم القاعدة ملتصقة بالأسفل.
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        header()
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(
                                    start = MaxSpace.md,
                                    end = MaxSpace.md,
                                    top = MaxSpace.xs
                                ),
                            verticalAlignment = Alignment.Top
                        ) {
                            HeroPanel(
                                games = games,
                                selected = selected,
                                selectedIndex = selectedIndex,
                                subtitle = statusDetail,
                                wide = true,
                                onLaunch = onLaunch,
                                onOptions = onOptions,
                                onOpenLibrary = onOpenLibrary,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            if (games.isNotEmpty() || selected != null) {
                                Spacer(Modifier.width(MaxSpace.sm))
                            }
                            if (games.isNotEmpty() || selected != null) {
                                PerformancePanel(
                                    cpu = cpu,
                                    gpu = gpu,
                                    heat = heat,
                                    batteryPercent = batteryPercent,
                                    charging = charging,
                                    panelEnabled = panelEnabled,
                                    onOpen = onOptions,
                                    modifier = Modifier
                                        .width(panelWidth)
                                )
                            }
                        }
                        if (games.isNotEmpty()) {
                            LibraryStrip(
                                games = games,
                                selected = selected,
                                selectedIndex = selectedIndex,
                                arrows = true,
                                cardHeight = cardHeight,
                                onSelect = onSelect,
                                onNext = nextGame,
                                onPrev = prevGame,
                                onViewAll = onOpenLibrary
                            )
                        }
                        StatusStrip(
                            title = statusTitle,
                            detail = statusDetail,
                            percent = readinessPercent,
                            height = statusHeight,
                            onClick = onOpenSessions
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
 * الشريط الجانبيّ: شعار «M» في أعلاه، ثم الأقسام الستّة، ثم زرّ مركز التحكّم مثبّتًا في أسفله.
 * الاختيار الحاليّ هو «الرئيسية» لأن المسرح هو الشاشة المعروضة، ولا صفّ يُظنّ زرًّا وهو لا يفعل شيئًا.
 */
@Composable
private fun GameSpaceRail(
    wide: Boolean,
    onHome: () -> Unit,
    onSection: (LobbySection) -> Unit,
) {
    val shape = CutCornerShape(topEnd = MaxSpace.xl, bottomEnd = MaxSpace.xl)
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (wide) RailWide else RailNarrow)
            .padding(end = MaxSpace.xs, top = MaxSpace.xs, bottom = MaxSpace.xs)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 18.dp,
                    shape = shape,
                    ambientColor = GameSpacePalette.Violet.copy(alpha = 0.45f),
                    spotColor = GameSpacePalette.Violet.copy(alpha = 0.55f)
                )
                .clip(shape)
                .background(GameSpacePalette.Deep.copy(alpha = 0.94f))
                .border(MaxSize.hairlineBorder, GameSpacePalette.Edge.copy(alpha = 0.55f), shape)
                .padding(horizontal = MaxSpace.xs, vertical = MaxSpace.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            RailMark(wide = wide)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
            ) {
                RailItem(
                    labelRes = R.string.gs_nav_home,
                    icon = Icons.Rounded.Home,
                    active = true,
                    wide = wide,
                    onClick = onHome
                )
                RailSection.entries.forEach { entry ->
                    RailItem(
                        labelRes = entry.labelRes,
                        icon = entry.section.icon,
                        active = false,
                        wide = wide,
                        onClick = { onSection(entry.section) }
                    )
                }
            }
            RailCollapse(wide = wide, onClick = { onSection(LobbySection.Lobby) })
        }
    }
}

/** شعار «M» في بطاقة مشطوفة بتدرّج بنفسجيّ وهالة، كما في أعلى اللقطة. */
@Composable
private fun RailMark(wide: Boolean) {
    val shape = CutCornerShape(topStart = MaxSpace.sm, bottomEnd = MaxSpace.sm)
    Box(
        modifier = Modifier
            .size(if (wide) RailMarkSize else MaxSize.minTouchTarget)
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = GameSpacePalette.Violet.copy(alpha = 0.8f),
                spotColor = GameSpacePalette.VioletSoft.copy(alpha = 0.9f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        GameSpacePalette.VioletSoft,
                        GameSpacePalette.Violet,
                        GameSpacePalette.VioletDeep
                    )
                )
            )
            .border(
                MaxSize.hairlineBorder,
                GameSpacePalette.VioletSoft.copy(alpha = MaxAlpha.borderStrong),
                shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.gs_brand_mark),
            color = Color.White,
            fontSize = if (wide) 22.sp else 18.sp,
            fontWeight = FontWeight.Black
        )
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

/**
 * صفّ القسم: أيقونة في أوّله واسمٌ في آخره، والاختيار الحاليّ كبسولة بتدرّج بنفسجيّ وحدّ مضيء
 * ووتد مثلّث على حافّتها اليسرى كما في اللقطة.
 */
@Composable
private fun RailItem(
    labelRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    wide: Boolean,
    onClick: () -> Unit,
) {
    val pill = RoundedCornerShape(maxRadius(MaxRadius.pill))
    Box(modifier = Modifier.fillMaxWidth()) {
        if (active) {
            // الوتد المثلّث على حافّة الكبسولة اليسرى، كما في اللقطة. حجمه ثابت فلا يمدّ الصفّ.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(MaxSpace.sm)
                    .graphicsLayer { rotationZ = 45f }
                    .background(GameSpacePalette.VioletSoft)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (active) MaxSpace.xs else 0.dp)
                .heightIn(min = MaxSize.minTouchTarget)
                .then(
                    if (active) {
                        Modifier.shadow(
                            elevation = 10.dp,
                            shape = pill,
                            ambientColor = GameSpacePalette.Violet.copy(alpha = 0.55f),
                            spotColor = GameSpacePalette.VioletSoft.copy(alpha = 0.7f)
                        )
                    } else {
                        Modifier
                    }
                )
                .clip(pill)
                .then(
                    if (active) {
                        Modifier.background(
                            Brush.horizontalGradient(
                                listOf(
                                    GameSpacePalette.Violet,
                                    GameSpacePalette.VioletDeep
                                )
                            )
                        )
                    } else {
                        Modifier
                    }
                )
                .border(
                    MaxSize.hairlineBorder,
                    if (active) GameSpacePalette.VioletSoft else Color.Transparent,
                    pill
                )
                .clickable(role = Role.Tab, onClick = onClick)
                .padding(horizontal = MaxSpace.xs),
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
                    fontSize = 10.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** زرّ طيّ الشريط وفتح مركز التحكّم، كما في أسفل اللقطة. */
@Composable
private fun RailCollapse(wide: Boolean, onClick: () -> Unit) {
    val shape = CutCornerShape(topStart = MaxSpace.sm, bottomEnd = MaxSpace.sm)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaxSize.minTouchTarget)
            .clip(shape)
            .background(GameSpacePalette.Panel.copy(alpha = 0.9f))
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (wide) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = null,
                tint = GameSpacePalette.VioletSoft,
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = stringResource(R.string.lobby_tab_control),
            tint = GameSpacePalette.Ink,
            modifier = Modifier.size(MaxSize.iconGlyphSmall)
        )
    }
}

/**
 * البطل: فنّ اللعبة الحقيقيّ (أيقونتها من `PackageManager`) على يمينه هالة، والنسخ على يساره —
 * اسم اللعبة، ثم سطر الحالة الحقيقيّ، ثم زرّ التشغيل و«…»، ثم نقاط التنقّل.
 */
@Composable
private fun HeroPanel(
    games: List<GameApp>,
    selected: GameApp?,
    selectedIndex: Int,
    subtitle: String,
    wide: Boolean,
    onLaunch: () -> Unit,
    onOptions: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (games.isEmpty() || selected == null) {
        EmptyHero(onOpenLibrary = onOpenLibrary, modifier = modifier)
        return
    }
    BoxWithConstraints(modifier = modifier.heightIn(min = 200.dp)) {
        // الغلاف لا يُرسم إلا حين يتّسع له عرض المسرح؛ وإلا بقي الفنّ ضوءًا خلف النصّ وحده.
        val cover = minOf(maxHeight * 0.82f, maxWidth * 0.30f, 230.dp)
        val showCover = wide && maxWidth >= HeroCoverMinWidth && cover >= 96.dp
        Box(modifier = Modifier.fillMaxSize()) {
            HeroArtWash(game = selected)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (wide) MaxSpace.md else MaxSpace.xs,
                        end = MaxSpace.xs
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = HeroCopyMaxWidth),
                    verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
                ) {
                    Crossfade(
                        targetState = selected.label,
                        animationSpec = tween(durationMillis = 260)
                    ) { label ->
                        Text(
                            text = label,
                            color = GameSpacePalette.Ink,
                            fontSize = if (wide) 38.sp else 30.sp,
                            lineHeight = if (wide) 42.sp else 34.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            color = GameSpacePalette.Muted,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(MaxSpace.xs))
                    LaunchRow(
                        enabled = true,
                        onLaunch = onLaunch,
                        onOptions = onOptions
                    )
                }
                if (showCover) {
                    HeroCover(
                        game = selected,
                        size = cover,
                        modifier = Modifier.padding(start = MaxSpace.md, end = MaxSpace.xs)
                    )
                }
            }
            GameSpaceDots(
                count = games.size,
                index = selectedIndex,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = MaxSpace.xs)
            )
        }
    }
}

/**
 * ضوء فنّ اللعبة: أيقونتها الحقيقيّة مكبَّرةً إلى يمين المسرح بشفافية، ثم ستارة تدرّج تُذيبها في
 * الخلفيّة. لا صورة منقولة ولا رسم بديل — ما لم تُقرأ الأيقونة لا يُرسم شيء.
 */
@Composable
private fun HeroArtWash(game: GameApp) {
    val icon by rememberLobbyIcon(game.packageName)
    val bitmap = icon ?: return
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.34f,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.62f)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to GameSpacePalette.Void,
                        0.42f to GameSpacePalette.Void.copy(alpha = 0.82f),
                        0.74f to GameSpacePalette.Void.copy(alpha = 0.30f),
                        1f to Color.Transparent
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, GameSpacePalette.Void.copy(alpha = 0.55f))
                    )
                )
        )
    }
}

/** غلاف اللعبة: أيقونتها الحقيقيّة في بطاقة مشطوفة بحدّ بنفسجيّ متوهّج. */
@Composable
private fun HeroCover(game: GameApp, size: Dp, modifier: Modifier = Modifier) {
    val icon by rememberLobbyIcon(game.packageName)
    val bitmap = icon
    val shape = CutCornerShape(topStart = MaxSpace.md, bottomEnd = MaxSpace.md)
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 26.dp,
                shape = shape,
                ambientColor = GameSpacePalette.Violet.copy(alpha = 0.6f),
                spotColor = GameSpacePalette.VioletSoft.copy(alpha = 0.75f)
            )
            .clip(shape)
            .background(GameSpacePalette.Deep)
            .border(
                MaxSize.activeRing,
                GameSpacePalette.VioletSoft.copy(alpha = MaxAlpha.borderStrong),
                shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // لمعة زاويّة خفيفة أعلى الغلاف، كحدّ الضوء في اللقطة.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MaxSize.activeRing)
                .background(GameSpacePalette.VioletSoft.copy(alpha = 0.7f))
                .align(Alignment.TopCenter)
        )
    }
}

/** الفراغ ليس «٠٪» ولا زرًّا معطّلًا: جملة تقول ما يحدث وخطوة واحدة تفعله. */
@Composable
private fun EmptyHero(onOpenLibrary: () -> Unit, modifier: Modifier = Modifier) {
    val shape = CutCornerShape(topStart = MaxSpace.md, bottomEnd = MaxSpace.md)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = HeroCopyMaxWidth),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        Box(
            modifier = Modifier
                .size(MaxSize.iconGlyph + MaxSpace.xxl)
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
            fontSize = 12.sp
        )
        val cta = RoundedCornerShape(maxRadius(MaxRadius.pill))
        Row(
            modifier = Modifier
                .heightIn(min = MaxSize.minTouchTarget)
                .shadow(
                    elevation = 10.dp,
                    shape = cta,
                    ambientColor = GameSpacePalette.Violet.copy(alpha = 0.5f),
                    spotColor = GameSpacePalette.Violet.copy(alpha = 0.6f)
                )
                .clip(cta)
                .background(
                    Brush.horizontalGradient(
                        listOf(GameSpacePalette.VioletSoft, GameSpacePalette.VioletDeep)
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

/** زرّ التشغيل: كبسولة بتدرّج بنفسجيّ وهالة، و«…» دائريّ بجانبها. */
@Composable
private fun LaunchRow(enabled: Boolean, onLaunch: () -> Unit, onOptions: () -> Unit) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.pill))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = LaunchMinWidth, max = LaunchMaxWidth)
                .heightIn(min = LaunchHeight)
                .shadow(
                    elevation = 16.dp,
                    shape = shape,
                    ambientColor = GameSpacePalette.Violet.copy(alpha = 0.55f),
                    spotColor = GameSpacePalette.VioletSoft.copy(alpha = 0.7f)
                )
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            GameSpacePalette.VioletSoft,
                            GameSpacePalette.Violet,
                            GameSpacePalette.VioletDeep
                        )
                    )
                )
                .border(MaxSize.hairlineBorder, GameSpacePalette.VioletSoft, shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onLaunch)
                .padding(horizontal = MaxSpace.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(MaxSize.iconGlyph)
            )
            Spacer(Modifier.width(MaxSpace.xs))
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
                .background(GameSpacePalette.Panel.copy(alpha = 0.92f))
                .border(MaxSize.hairlineBorder, GameSpacePalette.Edge, CircleShape)
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

/** نقاط تنقّل الألعاب: النشطة كبسولة مضيئة، والباقي نقاط باهتة كما في اللقطة. */
@Composable
private fun GameSpaceDots(count: Int, index: Int, modifier: Modifier = Modifier) {
    if (count <= 1) return
    val window = 7
    val start = (index - window / 2).coerceIn(0, (count - window).coerceAtLeast(0))
    val end = (start + window).coerceAtMost(count)
    Row(
        modifier = modifier,
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

/**
 * لوحة الأداء كما في اللقطة: صفّ عنوان قابل للنقر، وثلاث حلقات، ثم صفّان: البطارية ووضع الأداء.
 * كل قراءة تأتي من مصدرها الحقيقيّ، وما لم يُقرأ يبقى «—».
 */
@Composable
private fun PerformancePanel(
    cpu: Int?,
    gpu: Int?,
    heat: Float?,
    batteryPercent: Int?,
    charging: Boolean,
    panelEnabled: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = CutCornerShape(topStart = MaxSpace.md, bottomEnd = MaxSpace.xl)
    Column(
        modifier = modifier
            .shadow(
                elevation = 20.dp,
                shape = shape,
                ambientColor = GameSpacePalette.Violet.copy(alpha = 0.4f),
                spotColor = GameSpacePalette.Violet.copy(alpha = 0.55f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        GameSpacePalette.Panel.copy(alpha = 0.94f),
                        GameSpacePalette.Deep.copy(alpha = 0.96f)
                    )
                )
            )
            .border(MaxSize.hairlineBorder, GameSpacePalette.Edge, shape)
            .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
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
                imageVector = Icons.Rounded.Timeline,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaxSpace.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PerformanceRing(
                progress = cpu?.let { it / 100f },
                value = cpu?.let { "$it%" } ?: MAX_VALUE_UNAVAILABLE,
                label = stringResource(R.string.gs_cpu),
                tone = GameSpacePalette.VioletSoft,
                toneEnd = GameSpacePalette.Magenta
            )
            PerformanceRing(
                progress = gpu?.let { it / 100f },
                value = gpu?.let { "$it%" } ?: MAX_VALUE_UNAVAILABLE,
                label = stringResource(R.string.gs_gpu),
                tone = GameSpacePalette.Cyan,
                toneEnd = GameSpacePalette.Cyan
            )
            PerformanceRing(
                // ميزان الحرارة ٣٠–١٠٠°م: القوس مقياس، والرقم هو الخبر.
                progress = heat?.let { ((it - 30f) / 70f).coerceIn(0f, 1f) },
                value = heat?.let { "${it.roundToInt()}°" } ?: MAX_VALUE_UNAVAILABLE,
                label = stringResource(R.string.gs_heat),
                tone = GameSpacePalette.Magenta,
                toneEnd = Color(0xFFFF6B81)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            BatteryCard(
                percent = batteryPercent,
                charging = charging,
                modifier = Modifier.weight(1f)
            )
            ModeCard(
                enabled = panelEnabled,
                onOpen = onOpen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** بطاقة البطارية: الرقم واسمه، وأيقونتها خضراء ما دامت مقروءة، وسماويّة عند الشحن. */
@Composable
private fun BatteryCard(
    percent: Int?,
    charging: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = CutCornerShape(bottomEnd = MaxSpace.sm)
    Row(
        modifier = modifier
            .clip(shape)
            .background(GameSpacePalette.PanelRaised.copy(alpha = 0.55f))
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
            .padding(horizontal = MaxSpace.xs, vertical = MaxSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Icon(
            imageVector = Icons.Rounded.BatteryFull,
            contentDescription = null,
            tint = when {
                percent == null -> GameSpacePalette.Muted
                charging -> GameSpacePalette.Cyan
                else -> GameSpacePalette.Mint
            },
            modifier = Modifier.size(MaxSize.iconGlyphSmall)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = percent?.let { stringResource(R.string.gs_percent_value, it) }
                    ?: MAX_VALUE_UNAVAILABLE,
                color = if (percent == null) GameSpacePalette.Muted else GameSpacePalette.Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.gs_battery),
                color = GameSpacePalette.Muted,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** بطاقة وضع الأداء: «Gaming» حين تكون اللوحة مفعّلة، و«—» حين لا تكون، ونقرها يفتح الإعدادات. */
@Composable
private fun ModeCard(
    enabled: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = CutCornerShape(bottomEnd = MaxSpace.sm)
    Row(
        modifier = modifier
            .clip(shape)
            .background(GameSpacePalette.PanelRaised.copy(alpha = 0.55f))
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = MaxSpace.xs, vertical = MaxSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Icon(
            imageVector = Icons.Rounded.Bolt,
            contentDescription = null,
            tint = if (enabled) GameSpacePalette.Cyan else GameSpacePalette.Muted,
            modifier = Modifier.size(MaxSize.iconGlyphSmall)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.gs_perf_mode),
                color = GameSpacePalette.Muted,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (enabled) stringResource(R.string.gs_perf_mode_on) else MAX_VALUE_UNAVAILABLE,
                color = if (enabled) GameSpacePalette.Cyan else GameSpacePalette.Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** حلقة قياس: مسار مجزّأ، وقوس بتدرّج فوقه، وهالة خفيفة، والرقم في وسطها. */
@Composable
private fun PerformanceRing(
    progress: Float?,
    value: String,
    label: String,
    tone: Color,
    toneEnd: Color,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = (progress ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700)
    )
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)
    ) {
        Box(modifier = Modifier.size(RingSize), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(RingSize)) {
                // المسار المجزّأ يمتدّ ٣٦٠°، والقوس فوقه يُقصّ عند القيمة المقروءة.
                val glowWidth = size.minDimension * 0.20f
                val arcWidth = size.minDimension * 0.10f
                val inset = glowWidth / 2f
                val arcSize = Size(size.minDimension - inset * 2f, size.minDimension - inset * 2f)
                val arcOffset = Offset(
                    (size.width - arcSize.width) / 2f,
                    (size.height - arcSize.height) / 2f
                )
                val dash = PathEffect.dashPathEffect(
                    floatArrayOf(size.minDimension * 0.090f, size.minDimension * 0.055f)
                )
                val arcStroke = Stroke(width = arcWidth, cap = StrokeCap.Round, pathEffect = dash)
                val haloStroke = Stroke(width = glowWidth, cap = StrokeCap.Round, pathEffect = dash)
                drawArc(
                    color = GameSpacePalette.Track,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = arcStroke
                )
                if (progress != null) {
                    drawArc(
                        color = tone.copy(alpha = 0.20f),
                        startAngle = -90f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        topLeft = arcOffset,
                        size = arcSize,
                        style = haloStroke
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to tone,
                            1f to toneEnd,
                            center = Offset(size.width / 2f, size.height / 2f)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        topLeft = arcOffset,
                        size = arcSize,
                        style = arcStroke
                    )
                }
            }
            Text(
                text = value,
                color = if (value == MAX_VALUE_UNAVAILABLE) GameSpacePalette.Muted else GameSpacePalette.Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
        Text(
            text = label,
            color = GameSpacePalette.Muted,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** رفّ الألعاب: عنوان و«عرض الكل»، ثم بطاقات أفقيّة بسهمين، واللعبة المختارة وحدها مضيئة. */
@Composable
private fun LibraryStrip(
    games: List<GameApp>,
    selected: GameApp?,
    selectedIndex: Int,
    arrows: Boolean,
    cardHeight: Dp,
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
            .padding(horizontal = MaxSpace.md, vertical = MaxSpace.xs),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            Icon(
                imageVector = Icons.Rounded.VideogameAsset,
                contentDescription = null,
                tint = GameSpacePalette.Magenta,
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
            Text(
                text = stringResource(R.string.gs_shelf),
                color = GameSpacePalette.Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
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
                    color = GameSpacePalette.VioletSoft,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = GameSpacePalette.VioletSoft,
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
                contentPadding = PaddingValues(vertical = MaxSpace.hairline)
            ) {
                items(games, key = { it.packageName }) { game ->
                    GameCard(
                        game = game,
                        height = cardHeight,
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
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(GameSpacePalette.Deep.copy(alpha = 0.9f))
            .border(MaxSize.hairlineBorder, GameSpacePalette.Edge, CircleShape)
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

/**
 * بطاقة اللعبة بنسبة اللقطة (١٫٤٣ عرضًا لارتفاع): أيقونة اللعبة الحقيقيّة تملأ البطاقة، وستارة
 * سفلية تُظهر الاسم، والبطاقة المختارة وحدها بحدّ بنفسجيّ متوهّج وشريط سفليّ مضيء.
 */
@Composable
private fun GameCard(
    game: GameApp,
    height: Dp,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val icon by rememberLobbyIcon(game.packageName)
    val shape = RoundedCornerShape(maxRadius(MaxRadius.group))
    val width = height * CardAspect
    Box(
        modifier = Modifier
            .height(height)
            .width(width)
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 18.dp,
                        shape = shape,
                        ambientColor = GameSpacePalette.Violet.copy(alpha = 0.6f),
                        spotColor = GameSpacePalette.VioletSoft.copy(alpha = 0.75f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(GameSpacePalette.Panel.copy(alpha = 0.9f))
            .border(
                if (selected) MaxSize.activeRing else MaxSize.hairlineBorder,
                if (selected) GameSpacePalette.VioletSoft else GameSpacePalette.Hairline,
                shape
            )
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        val bitmap = icon
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = if (selected) 1f else 0.86f,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = game.label.take(1),
                    color = GameSpacePalette.Ink,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        // ستارة الاسم: تدرّج إلى أسفل البطاقة فلا يقرأ النصّ فوق صورة فاتحة.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to GameSpacePalette.Void.copy(alpha = 0.92f)
                    )
                )
        )
        Text(
            text = game.label,
            color = GameSpacePalette.Ink,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = MaxSpace.xs, vertical = MaxSpace.xs)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(MaxSize.activeRing)
                    .background(GameSpacePalette.VioletSoft)
            )
        }
    }
}

/**
 * قاعدة الحالة أسفل المسرح: حكم آخر جلسة أو الجاهزية، وشريط نسبة مجزّأ بخطوط مائلة كما في اللقطة.
 * النسبة هي الجاهزية الحقيقيّة، والضغط على القاعدة يفتح سجلّ الجلسات.
 */
@Composable
private fun StatusStrip(
    title: String,
    detail: String,
    percent: Int,
    height: Dp,
    onClick: () -> Unit,
) {
    val shape = CutCornerShape(topStart = MaxSpace.md, bottomEnd = MaxSpace.md)
    val target = percent.coerceIn(0, 100)
    val animated by animateFloatAsState(
        targetValue = target.toFloat(),
        animationSpec = tween(durationMillis = 700)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaxSpace.md, vertical = MaxSpace.xs)
            .heightIn(min = height)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = GameSpacePalette.Violet.copy(alpha = 0.4f),
                spotColor = GameSpacePalette.Violet.copy(alpha = 0.5f)
            )
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        GameSpacePalette.Panel.copy(alpha = 0.96f),
                        GameSpacePalette.Deep.copy(alpha = 0.96f)
                    )
                )
            )
            .border(MaxSize.hairlineBorder, GameSpacePalette.Edge, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        Icon(
            imageVector = Icons.Rounded.Bolt,
            contentDescription = null,
            tint = GameSpacePalette.VioletSoft,
            modifier = Modifier.size(MaxSize.iconGlyphSmall)
        )
        Column(
            modifier = Modifier.weight(1.1f),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)
        ) {
            Text(
                text = title,
                color = GameSpacePalette.Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                color = GameSpacePalette.Muted,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(MaxSpace.md)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(target / 100f, 0f..1f)
                }
        ) {
            // ٣٢ قطعة مائلة: المملوء بتدرّج بنفسجيّ والباقي بمسار باهت، كشريط اللقطة.
            val segments = 32
            val slot = size.width / segments
            val gap = slot * 0.30f
            val barWidth = slot - gap
            val lean = size.height * 0.34f
            val filled = segments * animated / 100f
            val brush = Brush.horizontalGradient(
                listOf(GameSpacePalette.Violet, GameSpacePalette.Magenta)
            )
            for (i in 0 until segments) {
                val x = i * slot
                val path = Path().apply {
                    moveTo(x + lean, 0f)
                    lineTo(x + barWidth + lean, 0f)
                    lineTo(x + barWidth, size.height)
                    lineTo(x, size.height)
                    close()
                }
                if (i < filled) {
                    drawPath(path, brush)
                } else {
                    drawPath(path, GameSpacePalette.Track)
                }
            }
        }
        Text(
            text = stringResource(R.string.gs_percent_value, target),
            color = GameSpacePalette.Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
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
