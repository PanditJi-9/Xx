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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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
 * المسرح — تخطيط سينمائيّ كامل للشاشة: لوحة فحمية محايدة ولمسات بنفسجية محدودة.
 *
 * الهندسة كما في اللقطة: شريط أقسام زجاجيّ على الحافة اليسرى، وبطلٌ يمتدّ على عرض المسرح بفنّ
 * اللعبة، وعنوانها، وسطرٌ عربيّ، وكبسولة تشغيل، ونقاط تنقّل. وإلى يمينه عمودٌ عائم من ثلاث
 * بطاقات: حلقات الأداء المجزّأة، ثم البطارية ووضع الأداء، ثم موجة الأداء اللحظيّة. وتحت ذلك
 * رفّ «الألعاب الشائعة» ببطاقات ملصقات رأسيّة و«المزيد»، ثم قاعدة حالة بشريط مائل ومئة في المئة
 * من الجاهزية الحقيقيّة.
 *
 * **القيم كلها حيّة:** المعالج من `PanelCpuLoad`، والرسوميات من `HudDeepReaders.gpuLoadPercent`،
 * والحرارة من `socTempC` (أو حرارة البطارية عند غيابها)، ومعدّل التحديث من شاشة الجهاز نفسها،
 * والموجة من تاريخ عيّنات المعالج الحقيقيّة. وما لم يُقرأ يبقى «—» وحلقةً بلا قوس — ولا تُكتب
 * أرقام اللقطة (٣٨٪ · ٥٢٪ · ٤١° · ١٢٠) لأنها ثابتة في الصورة لا في الجهاز (ADR-07).
 *
 * **والفنّ ليس منقولًا:** البطل أيقونة اللعبة الحقيقيّة من `PackageManager` مكبَّرةً ومذابةً في
 * خلفية مبنيّة من تدرّجات وأضواء، لا صورة من اللقطة ولا شعار لعبةٍ آخر.
 *
 * **والاتجاه مثبّت LTR لهذه الشاشة وحدها** لأن اللقطة تُظهر الشريط يسارًا؛ فالمحاذاة صريحة لا
 * موروثة من العربية.
 */

/** عرض الشريط: ١٥٠dp على العرض الواسع، و٦٤dp على الهاتف. */
private val RailWide = 150.dp
private val RailNarrow = 64.dp

/** عمود الأداء: ٣٠٪ من عرض المسرح، بين ٢٦٨dp و٣٤٨dp. */
private val PanelWidthRatio = 0.27f
private val PanelMinWidth = 252.dp
private val PanelMaxWidth = 340.dp

/** قطر حلقات القياس: أصغر قليلًا في العمود الجانبيّ كي تبقى البطاقات متوازنة. */
private val RingWide = 62.dp
private val RingCompact = 86.dp

/** بطاقات المكتبة أقرب إلى أغلفة الألعاب شبه المربّعة، وتتكيف مع ارتفاع المسرح. */
private val CardHeightRatio = 0.22f
private val CardMinHeight = 82.dp
private val CardMaxHeight = 150.dp
private const val CardAspect = 0.82f

/** شريط الحالة السفلي: ارتفاع مرن مع حد أدنى يحافظ على سهولة القراءة واللمس. */
private val StatusHeightRatio = 0.065f
private val StatusMinHeight = 42.dp

/** أقصى عرض لكتلة نصّ البطل، فلا يمتدّ السطر على عرض المسرح. */
private val HeroCopyMaxWidth = 520.dp

/** زرّ التشغيل: أدنى عرض وأقصى عرض وارتفاعه. */
private val LaunchMinWidth = 160.dp
private val LaunchMaxWidth = 240.dp
private val LaunchHeight = MaxSize.minTouchTarget

/** أقصى طول لموجة الأداء: ٤٨ عيّنة أي نحو ٩٦ ثانية من التاريخ الحقيقيّ. */
private const val WaveHistory = 48

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
    onOpenLibrary: () -> Unit,
    onOpenSessions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // القراءات الحيّة: نفس القارئين اللذين يغذّيان مراقب الجهاز، فلا مقياس ثانٍ.
    // مؤقّت واحد للثلاثة معًا، وتاريخ المعالج يُغذّي موجة الأداء الحقيقيّة.
    val readings by produceState(initialValue = LiveReadings()) {
        val history = mutableListOf<Int>()
        while (true) {
            val cpu = PanelCpuLoad.sample()
            val gpu = HudDeepReaders.gpuLoadPercent()
            val temp = HudDeepReaders.socTempC()
            if (cpu != null) {
                history += cpu
                while (history.size > WaveHistory) history.removeAt(0)
            }
            value = LiveReadings(
                cpu = cpu,
                gpu = gpu,
                tempC = temp,
                cpuHistory = history.toList()
            )
            delay(2_000L)
        }
    }
    val cpu = readings.cpu
    val gpu = readings.gpu
    val heat = readings.tempC ?: batteryTempC

    // معدّل تحديث الشاشة الحقيقيّ: يُقرأ من العرض، لا يُفترض ١٢٠.
    val view = LocalView.current
    val refreshRate = remember(view) {
        runCatching { view.display?.refreshRate?.roundToInt() }.getOrNull()
    }

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
                .background(GameSpacePalette.Void)
        ) {
            StageBackdrop()

            // لا يكفي العرض وحده: نتحقق من نسبة الشاشة والحد الأدنى العملي للارتفاع.
            // عامل النسبة أهم من الارتفاع المطلق: شاشات التلفاز/الأجهزة اللوحية قد تكون
            // عريضة جدًا لكن ارتفاعها المنطقي أقل من 460dp بسبب كثافة العرض وشريط النظام.
            // لا نحولها إلى تخطيط هاتف عموديّ لمجرد نقص الارتفاع؛ نستخدم العرض الأفقي
            // متى توفرت مساحة عملية للمسرح ولوحة الأداء.
            val wide = maxWidth >= 720.dp &&
                maxHeight >= 340.dp &&
                (maxWidth / maxHeight) >= 1.35f
            val panelWidth = (maxWidth * PanelWidthRatio)
                .coerceIn(PanelMinWidth, PanelMaxWidth)
            val cardHeight = if (wide) {
                (maxHeight * CardHeightRatio).coerceIn(CardMinHeight, CardMaxHeight)
            } else {
                (maxHeight * 0.20f).coerceIn(CardMinHeight + MaxSpace.xxl, CardMaxHeight)
            }
            val statusHeight = (maxHeight * StatusHeightRatio)
                .coerceAtLeast(StatusMinHeight)

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        HeroPanel(
                            games = games,
                            selected = selected,
                            selectedIndex = selectedIndex,
                            wide = false,
                            onLaunch = onLaunch,
                            onOptions = onOptions,
                            onOpenLibrary = onOpenLibrary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                        )
                        PerformancePanel(
                            cpu = cpu,
                            gpu = gpu,
                            heat = heat,
                            batteryPercent = batteryPercent,
                            charging = charging,
                            panelEnabled = panelEnabled,
                            refreshRate = refreshRate,
                            cpuHistory = readings.cpuHistory,
                            onOpen = onOptions,
                            wide = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (games.isNotEmpty()) {
                            LibraryStrip(
                                games = games,
                                selected = selected,
                                selectedIndex = selectedIndex,
                                arrows = false,
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
                } else {
                    // العرض الواسع: الهندسة كما في اللقطة — الفنّ يملأ المسرح، والنصّ في أسفله،
                    // وعمود الأداء الزجاجيّ يعوم فوقه في الطرف الأيمن، ثم رفّ الألعاب، ثم القاعدة.
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            HeroPanel(
                                games = games,
                                selected = selected,
                                selectedIndex = selectedIndex,
                                wide = true,
                                onLaunch = onLaunch,
                                onOptions = onOptions,
                                onOpenLibrary = onOpenLibrary,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (games.isNotEmpty() || selected != null) {
                                PerformancePanel(
                                    cpu = cpu,
                                    gpu = gpu,
                                    heat = heat,
                                    batteryPercent = batteryPercent,
                                    charging = charging,
                                    panelEnabled = panelEnabled,
                                    refreshRate = refreshRate,
                                    cpuHistory = readings.cpuHistory,
                                    onOpen = onOptions,
                                    wide = true,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = MaxSpace.xs, end = MaxSpace.md)
                                        // لا يتجاوز ارتفاع البطل: العمود نفسه يتمرّر داخليًّا
                                        // إذا ضاق الارتفاع، ولا يُقصّ شيء بصمت.
                                        .heightIn(max = maxHeight - MaxSpace.md)
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

/** القراءات الحيّة في قراءة واحدة، وتاريخ المعالج لموجة الأداء. */
private data class LiveReadings(
    val cpu: Int? = null,
    val gpu: Int? = null,
    val tempC: Float? = null,
    val cpuHistory: List<Int> = emptyList(),
)

/** خلفية المسرح: ظلامٌ حالك، وهالتان بنفسجيّة وورديّة، وشبكة أفق خفيفة كما في اللقطة. */
@Composable
private fun StageBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(
                listOf(
                    GameSpacePalette.Void,
                    GameSpacePalette.Deep,
                    GameSpacePalette.Void
                )
            )
        )
        val violet = Offset(size.width * 0.55f, size.height * 0.38f)
        val violetR = size.minDimension * 0.95f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(GameSpacePalette.Violet.copy(alpha = 0.085f), Color.Transparent),
                center = violet,
                radius = violetR
            ),
            radius = violetR,
            center = violet
        )
        val pink = Offset(size.width * 0.92f, size.height * 0.05f)
        val pinkR = size.minDimension * 0.60f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(GameSpacePalette.Magenta.copy(alpha = 0.045f), Color.Transparent),
                center = pink,
                radius = pinkR
            ),
            radius = pinkR,
            center = pink
        )
        // خطوط أفق دقيقة أسفل المسرح: عمقٌ سينمائيّ لا صورة.
        val lines = 6
        val startY = size.height * 0.62f
        val step = (size.height - startY) / lines
        for (i in 0 until lines) {
            val y = startY + step * i
            drawLine(
                color = GameSpacePalette.VioletSoft.copy(alpha = 0.025f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }
    }
}

/**
 * الشريط الجانبيّ: شعار «M» في أعلاه، ثم الرئيسية والأقسام، وترس الإعدادات في أسفله.
 * زجاجٌ داكن بحدٍّ بنفسجيّ، والاختيار الحاليّ كبسولة بنفسجيّة شفّافة مضيئة كما في اللقطة.
 */
@Composable
internal fun GameSpaceRail(
    wide: Boolean,
    active: LobbySection?,
    onSelect: (LobbySection?) -> Unit,
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.sheet))
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (wide) RailWide else RailNarrow)
            .padding(start = MaxSpace.xs, end = MaxSpace.sm, top = MaxSpace.sm, bottom = MaxSpace.sm)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 20.dp,
                    shape = shape,
                    ambientColor = GameSpacePalette.Violet.copy(alpha = 0.16f),
                    spotColor = GameSpacePalette.Violet.copy(alpha = 0.22f)
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            GameSpacePalette.Violet.copy(alpha = 0.035f),
                            GameSpacePalette.Deep.copy(alpha = 0.94f),
                            GameSpacePalette.Void.copy(alpha = 0.97f)
                        )
                    )
                )
                .border(MaxSize.hairlineBorder, GameSpacePalette.Edge.copy(alpha = 0.55f), shape)
                .padding(horizontal = MaxSpace.xs, vertical = MaxSpace.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            RailMark(wide = wide)
            Spacer(Modifier.height(MaxSpace.xs))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)
            ) {
                RailItem(
                    labelRes = R.string.gs_nav_home,
                    icon = Icons.Rounded.Home,
                    active = active == null,
                    wide = wide,
                    onClick = { onSelect(null) }
                )
                LobbySection.entries
                    .filter { it != LobbySection.Lobby }
                    .forEach { section ->
                        RailItem(
                            labelRes = section.titleRes,
                            icon = section.icon,
                            active = active == section,
                            wide = wide,
                            onClick = { onSelect(section) }
                        )
                    }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MaxSize.hairlineBorder)
                    .background(GameSpacePalette.Hairline)
            )
            RailGear(
                active = active == LobbySection.Lobby,
                onClick = { onSelect(LobbySection.Lobby) }
            )
        }
    }
}

/** شعار «M» بتدرّج بنفسجيّ وتوهّج، حرًّا على الزجاج لا داخل بطاقة. */
@Composable
private fun RailMark(wide: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (wide) 52.dp else MaxSize.minTouchTarget),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.gs_brand_mark),
            style = TextStyle(
                brush = Brush.linearGradient(
                    listOf(
                        Color.White,
                        GameSpacePalette.VioletSoft,
                        GameSpacePalette.Violet
                    )
                ),
                fontSize = if (wide) 34.sp else 26.sp,
                fontWeight = FontWeight.Black,
                shadow = Shadow(
                    color = GameSpacePalette.VioletSoft.copy(alpha = 0.38f),
                    blurRadius = 22f
                )
            )
        )
    }
}

/**
 * صفّ القسم: أيقونة في أوّله واسمٌ في آخره، والاختيار الحاليّ كبسولة بنفسجيّة شفّافة بحدٍّ مضيء
 * كما في اللقطة — بلا وتدٍ ولا تدرّجٍ صلب.
 */
@Composable
private fun RailItem(
    labelRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    wide: Boolean,
    onClick: () -> Unit,
) {
    val pill = RoundedCornerShape(maxRadius(MaxRadius.row))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaxSize.minTouchTarget)
            .then(
                if (active) {
                    Modifier.shadow(
                        elevation = 12.dp,
                        shape = pill,
                        ambientColor = GameSpacePalette.Violet.copy(alpha = 0.20f),
                        spotColor = GameSpacePalette.VioletSoft.copy(alpha = 0.30f)
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
                                GameSpacePalette.Violet.copy(alpha = 0.20f),
                                GameSpacePalette.VioletDeep.copy(alpha = 0.22f)
                            )
                        )
                    )
                } else {
                    Modifier
                }
            )
            .border(
                MaxSize.hairlineBorder,
                if (active) GameSpacePalette.VioletSoft.copy(alpha = 0.58f) else Color.Transparent,
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
            modifier = Modifier.size(MaxSize.iconGlyph)
        )
        if (wide) {
            Text(
                text = stringResource(labelRes),
                color = if (active) Color.White else GameSpacePalette.Muted,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** ترس الإعدادات في أسفل الشريط، كما في اللقطة. */
@Composable
private fun RailGear(active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaxSize.minTouchTarget)
            .clip(RoundedCornerShape(maxRadius(MaxRadius.row)))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.gs_nav_settings),
            tint = if (active) GameSpacePalette.VioletSoft else GameSpacePalette.Muted,
            modifier = Modifier.size(MaxSize.iconGlyph)
        )
    }
}

/**
 * البطل: فنّ اللعبة الحقيقيّ يملأ المسرح، والنسخ في أسفله — اسم اللعبة، ثم سطرٌ سينمائيّ، ثم
 * كبسولة «العب الآن» و«…»، ثم نقاط التنقّل. والنقر على الكبسولة يفتح اللعبة.
 */
@Composable
private fun HeroPanel(
    games: List<GameApp>,
    selected: GameApp?,
    selectedIndex: Int,
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
    // في العرض الواسع الفنّ يملأ المسرح بلا بطاقة ولا حدّ، كما في اللقطة؛ وعلى الهاتف
    // يبقى لوحًا مستديرًا لأن العمود يتمرّر حوله.
    val shape = if (wide) RectangleShape else RoundedCornerShape(maxRadius(MaxRadius.sheet))
    BoxWithConstraints(
        modifier = modifier.clip(shape)
    ) {
        val short = maxHeight < 240.dp
        // في الواسع يقف النصّ في النصف الأيسر حتى لا يدخل تحت عمود الأداء العائم.
        val copyMaxWidth = if (wide) minOf(HeroCopyMaxWidth, maxWidth * 0.54f) else HeroCopyMaxWidth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(GameSpacePalette.Deep, GameSpacePalette.Void)
                    )
                )
                .then(
                    if (wide) {
                        Modifier
                    } else {
                        Modifier.border(
                            MaxSize.hairlineBorder,
                            GameSpacePalette.Edge.copy(alpha = 0.45f),
                            shape
                        )
                    }
                )
        ) {
            HeroBackdrop()
            HeroArtWash(game = selected)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .widthIn(max = copyMaxWidth)
                    .padding(
                        start = if (wide) MaxSpace.gutter else MaxSpace.md,
                        end = MaxSpace.md,
                        bottom = if (wide) MaxSpace.lg else MaxSpace.xl
                    ),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
            ) {
                Crossfade(
                    targetState = selected.label,
                    animationSpec = tween(durationMillis = 260)
                ) { label ->
                    Text(
                        text = label,
                        // شعار اللعبة أبيض ثم ينزلق إلى البنفسجيّ في آخره — نظير شعار اللوحة.
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                listOf(
                                    GameSpacePalette.Ink,
                                    GameSpacePalette.Ink,
                                    GameSpacePalette.VioletSoft
                                )
                            ),
                            fontSize = when {
                                short -> 26.sp
                                wide -> 40.sp
                                else -> 32.sp
                            },
                            lineHeight = when {
                                short -> 30.sp
                                wide -> 44.sp
                                else -> 36.sp
                            },
                            fontWeight = FontWeight.Black
                        ),
                        maxLines = if (short) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = stringResource(R.string.gs_hero_tagline),
                    color = GameSpacePalette.Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                LaunchRow(
                    enabled = true,
                    onLaunch = onLaunch,
                    onOptions = onOptions
                )
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
 * ضوء البطل: أيقونة اللعبة الحقيقيّة مكبَّرةً ومذابةً في الخلفيّة، لا صورة منقولة. والتكبير ٢٫٢×
 * مقصود: يُقرأ لون اللعبة وملمسها خلفيّةً، ولا تتكرّر أيقونتها العملاقة شعارًا ثانيًا.
 */
@Composable
private fun HeroArtWash(game: GameApp) {
    val icon by rememberLobbyIcon(game.packageName)
    val bitmap = icon ?: return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.50f,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 2.2f
                    scaleY = 2.2f
                }
        )
        // ظلامٌ تامّ تحت النصّ، وانكشافٌ تدريجيّ إلى اليمين حيث يقف الفنّ.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to GameSpacePalette.Void,
                        0.30f to GameSpacePalette.Void.copy(alpha = 0.92f),
                        0.60f to GameSpacePalette.Void.copy(alpha = 0.52f),
                        1f to Color.Transparent
                    )
                )
        )
        // أعلى الفنّ يُذاب في الخلفيّة، وأسفله في رفّ الألعاب — فلا حدّ فاصل بين المنطقتين.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to GameSpacePalette.Void.copy(alpha = 0.78f),
                        0.32f to Color.Transparent,
                        0.58f to Color.Transparent,
                        1f to GameSpacePalette.Void.copy(alpha = 0.96f)
                    )
                )
        )
        // هالة بنفسجيّة تُوحّد لون الفنّ مع لوحة الشاشة.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            GameSpacePalette.Violet.copy(alpha = 0.075f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * كونُ المسرح خلف فنّ اللعبة: نجومٌ ثابتة، وسُحبُ سديمٍ بنفسجيّة، وكوكبٌ ذو حلقةٍ وهالةٍ رقيقة،
 * وشعاعُ ضوءٍ مائل وأرضيّةٌ متدرّجة. لا صورة خارجيّة ولا أصلٌ منقول — كلّه رسمٌ إجرائيّ.
 *
 * النجوم ثابتة لا تتغيّر مع كل إعادة رسم: مولّدٌ بسيط ببذرةٍ ثابتة بدل `Random()`، فلا ترتجف
 * السماء حين تتحدّث القراءات كلّ ثانيتين. الإضاءة البنفسجية خافتة كي تبقى الخلفية فحمية.
 */
@Composable
private fun HeroBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        // سديمٌ مائل: حزامٌ بنفسجيّ عريض وحزامٌ ورديّ رقيق يقطعان السماء قطريًّا.
        val nebula = Path().apply {
            moveTo(size.width * 0.06f, 0f)
            lineTo(size.width * 0.46f, 0f)
            lineTo(size.width * 0.86f, size.height)
            lineTo(size.width * 0.44f, size.height)
            close()
        }
        drawPath(
            nebula,
            Brush.linearGradient(
                listOf(
                    Color.Transparent,
                    GameSpacePalette.Violet.copy(alpha = 0.065f),
                    Color.Transparent
                ),
                start = Offset(size.width * 0.1f, 0f),
                end = Offset(size.width * 0.8f, size.height)
            )
        )
        val haze = Path().apply {
            moveTo(size.width * 0.52f, 0f)
            lineTo(size.width * 0.68f, 0f)
            lineTo(size.width * 0.98f, size.height)
            lineTo(size.width * 0.80f, size.height)
            close()
        }
        drawPath(
            haze,
            Brush.linearGradient(
                listOf(
                    Color.Transparent,
                    GameSpacePalette.Magenta.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                start = Offset(size.width * 0.55f, 0f),
                end = Offset(size.width * 0.95f, size.height)
            )
        )

        // نجومٌ ثابتة: بذرةٌ واحدة، فلا تتغيّر السماء بين إعادة رسمٍ وأخرى.
        var seed = 0x2F6E2B1L
        fun next(): Float {
            seed = (seed * 1103515245L + 12345L) and 0x7FFFFFFFL
            return seed.toFloat() / 0x7FFFFFFF.toFloat()
        }
        repeat(90) {
            val x = next() * size.width
            val y = next() * size.height * 0.92f
            val r = 0.5f + next() * 1.3f
            val a = 0.10f + next() * 0.42f
            drawCircle(
                color = if (next() > 0.7f) {
                    GameSpacePalette.VioletSoft.copy(alpha = a)
                } else {
                    Color.White.copy(alpha = a)
                },
                radius = r,
                center = Offset(x, y)
            )
        }

        // الكوكب: كرةٌ داكنة بحافّةٍ مضيئة وحلقةٍ مائلة — عمقُ السديم في اللوحة.
        val planet = Offset(size.width * 0.72f, size.height * 0.36f)
        val planetR = size.minDimension * 0.17f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    GameSpacePalette.VioletDeep.copy(alpha = 0.28f),
                    GameSpacePalette.Deep.copy(alpha = 0.20f),
                    Color.Transparent
                ),
                center = Offset(planet.x - planetR * 0.35f, planet.y - planetR * 0.35f),
                radius = planetR * 1.4f
            ),
            radius = planetR,
            center = planet
        )
        drawArc(
            color = GameSpacePalette.VioletSoft.copy(alpha = 0.18f),
            startAngle = 168f,
            sweepAngle = 118f,
            useCenter = false,
            topLeft = Offset(planet.x - planetR, planet.y - planetR),
            size = Size(planetR * 2f, planetR * 2f),
            style = Stroke(width = 1.6f)
        )
        rotate(degrees = -16f, pivot = planet) {
            drawOval(
                color = GameSpacePalette.Violet.copy(alpha = 0.13f),
                topLeft = Offset(planet.x - planetR * 1.9f, planet.y - planetR * 0.42f),
                size = Size(planetR * 3.8f, planetR * 0.84f),
                style = Stroke(width = 1.4f)
            )
            drawOval(
                color = GameSpacePalette.Magenta.copy(alpha = 0.065f),
                topLeft = Offset(planet.x - planetR * 1.62f, planet.y - planetR * 0.32f),
                size = Size(planetR * 3.24f, planetR * 0.64f),
                style = Stroke(width = 3.2f)
            )
        }

        val key = Offset(size.width * 0.34f, size.height * 0.52f)
        val keyR = size.minDimension * 0.85f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(GameSpacePalette.Violet.copy(alpha = 0.085f), Color.Transparent),
                center = key,
                radius = keyR
            ),
            radius = keyR,
            center = key
        )
        // شعاع ضوء مائل من أعلى اليمين، كما في ملصقات الألعاب.
        val beam = Path().apply {
            moveTo(size.width * 0.62f, 0f)
            lineTo(size.width * 0.78f, 0f)
            lineTo(size.width * 0.46f, size.height)
            lineTo(size.width * 0.30f, size.height)
            close()
        }
        drawPath(
            beam,
            Brush.verticalGradient(
                listOf(GameSpacePalette.VioletSoft.copy(alpha = 0.055f), Color.Transparent)
            )
        )
        val floor = Path().apply {
            moveTo(0f, size.height * 0.86f)
            lineTo(size.width, size.height * 0.78f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            floor,
            Brush.verticalGradient(
                listOf(Color.Transparent, GameSpacePalette.Violet.copy(alpha = 0.05f))
            )
        )
    }
}

/** الفراغ ليس «٠٪» ولا زرًّا معطّلًا: جملة تقول ما يحدث وخطوة واحدة تفعله. */
@Composable
private fun EmptyHero(onOpenLibrary: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.sheet))
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(GameSpacePalette.Deep, GameSpacePalette.Void)
                )
            )
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
            .padding(MaxSpace.lg)
            .widthIn(max = HeroCopyMaxWidth),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        Box(
            modifier = Modifier
                .size(MaxSize.iconGlyph + MaxSpace.xxl)
                .clip(RoundedCornerShape(maxRadius(MaxRadius.tile)))
                .background(GameSpacePalette.Panel.copy(alpha = 0.9f))
                .border(
                    MaxSize.hairlineBorder,
                    GameSpacePalette.Hairline,
                    RoundedCornerShape(maxRadius(MaxRadius.tile))
                ),
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

/** كبسولة التشغيل الزجاجيّة بحدٍّ بنفسجيّ، ودائرة «…» بجانبها، كما في اللقطة. */
@Composable
private fun LaunchRow(enabled: Boolean, onLaunch: () -> Unit, onOptions: () -> Unit) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.pill))
    Row(
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
                    ambientColor = GameSpacePalette.Violet.copy(alpha = 0.22f),
                    spotColor = GameSpacePalette.VioletSoft.copy(alpha = 0.28f)
                )
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            GameSpacePalette.Violet.copy(alpha = 0.22f),
                            GameSpacePalette.Deep.copy(alpha = 0.72f)
                        )
                    )
                )
                .border(
                    MaxSize.emphasisBorder,
                    GameSpacePalette.VioletSoft.copy(alpha = 0.62f),
                    shape
                )
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
                text = stringResource(R.string.gs_play_now),
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
                .background(GameSpacePalette.Deep.copy(alpha = 0.62f))
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
 * عمود الأداء: ثلاث بطاقات زجاجيّة كما في اللقطة — حلقات القياس، ثم البطارية ووضع الأداء، ثم
 * موجة الأداء اللحظيّة ومعدّل تحديث الشاشة. كل قراءة من مصدرها الحقيقيّ، وما لم يُقرأ يبقى «—».
 */
@Composable
private fun PerformancePanel(
    cpu: Int?,
    gpu: Int?,
    heat: Float?,
    batteryPercent: Int?,
    charging: Boolean,
    panelEnabled: Boolean,
    refreshRate: Int?,
    cpuHistory: List<Int>,
    onOpen: () -> Unit,
    wide: Boolean,
    modifier: Modifier = Modifier,
) {
    val ringSize = if (wide) RingWide else RingCompact
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        PerfSlab {
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
                Box(
                    modifier = Modifier
                        .size(MaxSpace.sm)
                        .clip(CircleShape)
                        .background(GameSpacePalette.Mint)
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
                    toneEnd = GameSpacePalette.Magenta,
                    size = ringSize
                )
                PerformanceRing(
                    progress = gpu?.let { it / 100f },
                    value = gpu?.let { "$it%" } ?: MAX_VALUE_UNAVAILABLE,
                    label = stringResource(R.string.gs_gpu),
                    tone = GameSpacePalette.Cyan,
                    toneEnd = GameSpacePalette.Cyan,
                    size = ringSize
                )
                PerformanceRing(
                    // ميزان الحرارة ٣٠–١٠٠°م: القوس مقياس، والرقم هو الخبر.
                    progress = heat?.let { ((it - 30f) / 70f).coerceIn(0f, 1f) },
                    value = heat?.let { "${it.roundToInt()}°" } ?: MAX_VALUE_UNAVAILABLE,
                    label = stringResource(R.string.gs_heat),
                    tone = GameSpacePalette.Magenta,
                    toneEnd = Color(0xFFFF6B81),
                    size = ringSize
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
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
        WaveCard(history = cpuHistory, refreshRate = refreshRate)
    }
}

/** بطاقة زجاجيّة موحّدة لعمود الأداء: حدٌّ بنفسجيّ خفيف وظلّ. */
@Composable
private fun PerfSlab(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.sheet))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = GameSpacePalette.Violet.copy(alpha = 0.15f),
                spotColor = GameSpacePalette.Violet.copy(alpha = 0.20f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        GameSpacePalette.Panel.copy(alpha = 0.90f),
                        GameSpacePalette.Deep.copy(alpha = 0.94f)
                    )
                )
            )
            .border(MaxSize.hairlineBorder, GameSpacePalette.Edge.copy(alpha = 0.60f), shape)
            .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
        content = content
    )
}

/** بطاقة البطارية: أيقونتها في مربّع ملوّن، والرقم كبير واسمه تحته. */
@Composable
private fun BatteryCard(
    percent: Int?,
    charging: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.row))
    val tint = when {
        percent == null -> GameSpacePalette.Muted
        charging -> GameSpacePalette.Cyan
        else -> GameSpacePalette.Mint
    }
    Row(
        modifier = modifier
            .clip(shape)
            .background(GameSpacePalette.PanelRaised.copy(alpha = 0.60f))
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
            .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Box(
            modifier = Modifier
                .size(MaxSize.rowIconContainer)
                .clip(RoundedCornerShape(maxRadius(MaxRadius.tile)))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.BatteryFull,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(MaxSize.iconGlyph)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = percent?.let { stringResource(R.string.gs_percent_value, it) }
                    ?: MAX_VALUE_UNAVAILABLE,
                color = if (percent == null) GameSpacePalette.Muted else GameSpacePalette.Ink,
                fontSize = 15.sp,
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
    val shape = RoundedCornerShape(maxRadius(MaxRadius.row))
    val tint = if (enabled) GameSpacePalette.VioletSoft else GameSpacePalette.Muted
    Row(
        modifier = modifier
            .clip(shape)
            .background(GameSpacePalette.PanelRaised.copy(alpha = 0.60f))
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Box(
            modifier = Modifier
                .size(MaxSize.rowIconContainer)
                .clip(RoundedCornerShape(maxRadius(MaxRadius.tile)))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(MaxSize.iconGlyph)
            )
        }
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
                color = tint,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * موجة الأداء: تاريخ عيّنات المعالج الحقيقيّة، ومعدّل تحديث الشاشة المقروء من العرض.
 * لا تُكتب ١٢٠ إطارًا لأن اللقطة تقولها — الرقم من `Display.refreshRate` أو «—».
 */
@Composable
private fun WaveCard(history: List<Int>, refreshRate: Int?) {
    val description = stringResource(R.string.gs_realtime)
    PerfSlab {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
        ) {
            Canvas(
                modifier = Modifier
                    .weight(1.2f)
                    .height(MaxSize.sparklineHeight)
                    .semantics { contentDescription = description }
            ) {
                val points = history.takeLast(WaveHistory)
                if (points.size < 2) {
                    drawLine(
                        color = GameSpacePalette.Track,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = MaxSize.activeRing.toPx()
                    )
                    return@Canvas
                }
                val stepX = size.width / (points.size - 1)
                val line = Path()
                val fill = Path()
                points.forEachIndexed { i, value ->
                    val x = i * stepX
                    val y = size.height * (1f - (value / 100f).coerceIn(0f, 1f)) * 0.86f +
                        size.height * 0.07f
                    if (i == 0) {
                        line.moveTo(x, y)
                        fill.moveTo(x, size.height)
                        fill.lineTo(x, y)
                    } else {
                        line.lineTo(x, y)
                        fill.lineTo(x, y)
                    }
                }
                fill.lineTo(size.width, size.height)
                fill.close()
                drawPath(
                    fill,
                    Brush.verticalGradient(
                        listOf(
                            GameSpacePalette.Violet.copy(alpha = 0.32f),
                            Color.Transparent
                        )
                    )
                )
                drawPath(
                    line,
                    Brush.horizontalGradient(
                        listOf(GameSpacePalette.VioletSoft, GameSpacePalette.Magenta)
                    ),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.gs_realtime),
                    color = GameSpacePalette.Muted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = refreshRate?.let { stringResource(R.string.gs_hz_value, it) }
                        ?: MAX_VALUE_UNAVAILABLE,
                    color = if (refreshRate == null) GameSpacePalette.Muted else GameSpacePalette.Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Rounded.BarChart,
                contentDescription = null,
                tint = GameSpacePalette.VioletSoft,
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
        }
    }
}

/** حلقة قياس: مسار مجزّأ، وقوس بتدرّج فوقه، وهالة خفيفة، والرقم في وسطها والاسم تحتها. */
@Composable
private fun PerformanceRing(
    progress: Float?,
    value: String,
    label: String,
    tone: Color,
    toneEnd: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = (progress ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700)
    )
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label $value"
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)
    ) {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size)) {
                // المسار المجزّأ يمتدّ ٣٦٠°، والقوس فوقه يُقصّ عند القيمة المقروءة.
                val glowWidth = this.size.minDimension * 0.20f
                val arcWidth = this.size.minDimension * 0.10f
                val inset = glowWidth / 2f
                val arcSize = Size(
                    this.size.minDimension - inset * 2f,
                    this.size.minDimension - inset * 2f
                )
                val arcOffset = Offset(
                    (this.size.width - arcSize.width) / 2f,
                    (this.size.height - arcSize.height) / 2f
                )
                val dash = PathEffect.dashPathEffect(
                    floatArrayOf(
                        this.size.minDimension * 0.090f,
                        this.size.minDimension * 0.055f
                    )
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
                            center = Offset(this.size.width / 2f, this.size.height / 2f)
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

/**
 * رفّ الألعاب: لوحٌ زجاجيّ بعنوان «الألعاب الشائعة» و«عرض الكل»، وبطاقات ملصقات رأسيّة
 * بسهمين، واللعبة المختارة وحدها مضيئة، ثم بطاقة «المزيد» في النهاية كما في اللقطة.
 */
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
    val shape = RoundedCornerShape(maxRadius(MaxRadius.sheet))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaxSpace.md, vertical = MaxSpace.xs)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        GameSpacePalette.Panel.copy(alpha = 0.62f),
                        GameSpacePalette.Deep.copy(alpha = 0.86f)
                    )
                )
            )
            .border(MaxSize.hairlineBorder, GameSpacePalette.Edge.copy(alpha = 0.45f), shape)
            .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.sm),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
        ) {
            Box(
                modifier = Modifier
                    .width(MaxSpace.xs)
                    .height(MaxSpace.lg)
                    .clip(RoundedCornerShape(maxRadius(MaxRadius.pill)))
                    .background(
                        Brush.verticalGradient(
                            listOf(GameSpacePalette.VioletSoft, GameSpacePalette.Magenta)
                        )
                    )
            )
            Text(
                text = stringResource(R.string.gs_popular_games),
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
                item(key = "more") {
                    MoreCard(height = cardHeight, onClick = onViewAll)
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
 * بطاقة اللعبة غلاف شبه مربّع: أيقونة اللعبة الحقيقيّة تملأ البطاقة،
 * وستارة سفلية تُظهر الاسم، والبطاقة المختارة وحدها بحدّ بنفسجيّ متوهّج وشريط سفليّ مضيء.
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
                        ambientColor = GameSpacePalette.Violet.copy(alpha = 0.22f),
                        spotColor = GameSpacePalette.VioletSoft.copy(alpha = 0.32f)
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
            // ملمس الغلاف: الأيقونة مكبَّرةً وخلف اللون، فيحمل الغلاف لون اللعبة.
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = if (selected) 0.90f else 0.76f,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.6f
                        scaleY = 1.6f
                    }
            )
            // لمعان قطريّ بنفسجيّ يربط الغلاف بلوحة الشاشة.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                GameSpacePalette.Violet.copy(alpha = 0.08f),
                                Color.Transparent,
                                GameSpacePalette.Magenta.copy(alpha = 0.07f)
                            )
                        )
                    )
            )
            // الشعار: الأيقونة كاملةً في وسط الغلاف.
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight(0.56f)
                    .fillMaxWidth(0.70f)
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
                        0.50f to Color.Transparent,
                        1f to GameSpacePalette.Void.copy(alpha = 0.94f)
                    )
                )
        )
        Text(
            text = game.label,
            color = GameSpacePalette.Ink,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 2,
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

/** بطاقة «المزيد» في نهاية الرفّ: أربعة مربّعات بنفسجيّة، كما في اللقطة. */
@Composable
private fun MoreCard(height: Dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.group))
    Box(
        modifier = Modifier
            .height(height)
            .width(height * CardAspect)
            .clip(shape)
            .background(GameSpacePalette.PanelRaised.copy(alpha = 0.45f))
            .border(MaxSize.hairlineBorder, GameSpacePalette.Edge.copy(alpha = 0.55f), shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
                Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
                    MoreSquare()
                    MoreSquare()
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
                    MoreSquare()
                    MoreSquare()
                }
            }
            Text(
                text = stringResource(R.string.gs_more_games),
                color = GameSpacePalette.Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MoreSquare() {
    Box(
        modifier = Modifier
            .size(MaxSpace.sm)
            .clip(RoundedCornerShape(maxRadius(MaxRadius.chip)))
            .background(GameSpacePalette.VioletSoft)
    )
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
    val shape = RoundedCornerShape(maxRadius(MaxRadius.sheet))
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
                ambientColor = GameSpacePalette.Violet.copy(alpha = 0.16f),
                spotColor = GameSpacePalette.Violet.copy(alpha = 0.20f)
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
    }
}
