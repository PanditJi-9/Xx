/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import android.animation.ValueAnimator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import nd.max.island.engine.dominantColor
import nd.max.ui.component.AppIconCache
import nd.max.ui.design.MaxDuration

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
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

/** تحت هذا الارتفاع يُصغَّر الأيقون والاسم ويُخفى الوصف: لا شيء يُدفع خارج الشاشة. */
private val CockpitHeroTight = 150.dp
private val CockpitHeroIconTight = 72.dp

/** أقصى عرض لكتلة الاسم: يبقيها ملتصقة بلوح الأيقونة بدل أن تتوزّع الشاشة كلها. */
private val CockpitHeroTextMax = 360.dp
private val CockpitHeroNameTight = 30.sp
private val CockpitHeroNameWide = 40.sp
private const val CockpitHeroRise = 14f
private const val CockpitShelfDrop = 10f

/** استخراج لون الأيقونة: عيّنة صغيرة تكفي، ومحاولات قصيرة قبل الرجوع إلى البنفسجي. */
private const val CockpitAccentTries = 6
private const val CockpitAccentRetryMs = 140L

/** منظور الرفّ: زاوية الجار، وخطوة العمق، وتتابع الظهور. */
private const val CockpitTileYaw = 14f
private const val CockpitShelfCamera = 10f
private const val CockpitShelfStagger = 0.06f

@Composable
internal fun GameSpaceCockpit(
    data: StageData,
    actions: StageActions,
    @Suppress("UNUSED_PARAMETER") currentPage: LobbyPage,
    @Suppress("UNUSED_PARAMETER") onPage: (LobbyPage) -> Unit,
    topActions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    // لون اللعبة يقود الهالة والحلقة ووجه الأيقونة؛ ويُرجَع إلى البنفسجي بلا وميض إن لم يُقرأ.
    val rawAccent = rememberCockpitAccent(data.selected?.packageName)
    val accent by animateColorAsState(
        targetValue = rawAccent,
        animationSpec = tween(MaxDuration.standard),
        label = "cockpitAccent",
    )
    // دخول واحد فقط: يُلغى بالكامل إن أوقف النظام المؤثّرات، فلا شاشة عالقة في منتصف حركة.
    val animatorsOn = remember { ValueAnimator.areAnimatorsEnabled() }
    val entrance = remember { Animatable(if (animatorsOn) 0f else 1f) }
    LaunchedEffect(entrance) {
        if (entrance.value < 1f) {
            entrance.animateTo(
                targetValue = 1f,
                animationSpec = tween(MaxDuration.deliberate, easing = FastOutSlowInEasing),
            )
        }
    }
    Box(modifier.fillMaxSize()) {
        CockpitBloom(accent = accent, ignition = entrance.value, modifier = Modifier.matchParentSize())
        Column(Modifier.fillMaxSize()) {
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
                    accent = accent,
                    entrance = entrance.value,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
            CockpitShelf(data = data, actions = actions, entrance = entrance.value)
        }
    }
}

/**
 * اللون السائد لأيقونة اللعبة: يُستخرج مرّة عند تبديل اللعبة، ويُرجع البنفسجي إن لم يُقرأ.
 *
 * ويُعاد استخدام الموجود لا يُبنى ثانيةً (ADR-05/§0.5): الأيقونة من `AppIconCache` نفسها التي
 * ترسمها، والمستخرج `dominantColor` الموجود في المشروع. لا قراءة متزامنة من القرص: المحاولات
 * القصيرة تنتظر الأيقونة التي تُحمَّل أصلًا للعرض، وإن لم تصل بقي البنفسجي — لا فراغ ولا وميض.
 */
@Composable
private fun rememberCockpitAccent(packageName: String?): Color {
    val accent by produceState(GameSpacePalette.Violet, packageName) {
        if (packageName == null) return@produceState
        var icon = AppIconCache.get(packageName)
        var tries = 0
        while (icon == null && tries < CockpitAccentTries) {
            delay(CockpitAccentRetryMs)
            icon = AppIconCache.get(packageName)
            tries++
        }
        val bitmap = icon?.asAndroidBitmap() ?: return@produceState
        val argb = withContext(Dispatchers.Default) { runCatching { dominantColor(bitmap) }.getOrNull() }
            ?: return@produceState
        value = Color(argb)
    }
    return accent
}

/**
 * خلفيّة المشهد: قاعدة شبه سوداء + هالة بلون اللعبة في زاوية البداية، تشتعل مع الدخول ثم تستقرّ.
 * كلها `drawBehind` — تُرسم مرّة مع إعادة الرسم ولا تكلّف إطارًا بعدها.
 */
@Composable
private fun CockpitBloom(accent: Color, ignition: Float, modifier: Modifier) {
    Box(
        modifier = modifier.drawBehind {
            drawRect(
                Brush.verticalGradient(
                    listOf(CockpitBase, GameSpacePalette.GlassDeep, CockpitFloor)
                )
            )
            // هالة واحدة تُشتعل مع الدخول وتستقرّ: رسم ثابت لا يكلف إطارًا بعدها.
            drawRect(
                Brush.radialGradient(
                    listOf(
                        accent.copy(alpha = MaxAlpha.toneContainerStrong * ignition),
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
private fun CockpitHero(
    data: StageData,
    actions: StageActions,
    accent: Color,
    entrance: Float,
    modifier: Modifier = Modifier,
) {
    val ready = data.readinessPercent.coerceIn(0, 100)
    BoxWithConstraints(modifier = modifier) {
        val tight = maxHeight < CockpitHeroTight
        val nameSize = if (tight) CockpitHeroNameTight else CockpitHeroNameWide
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaxSpace.xl, vertical = MaxSpace.md),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.lg),
        ) {
            CockpitHeroIcon(
                packageName = data.selected?.packageName,
                accent = accent,
                size = if (tight) CockpitHeroIconTight else CockpitHeroIcon,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            Column(
                modifier = Modifier
                    .widthIn(max = CockpitHeroTextMax)
                    .graphicsLayer {
                        alpha = entrance
                        translationY = (1f - entrance) * CockpitHeroRise
                    },
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
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            listOf(GameSpacePalette.Ink, GameSpacePalette.Lavender)
                        ),
                        fontSize = nameSize,
                        lineHeight = nameSize * 1.08f,
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.8).sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val tagline = data.tagline
                if (!tight && !tagline.isNullOrBlank()) {
                    Text(
                        text = tagline,
                        color = GameSpacePalette.Muted,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(MaxSpace.sm))
                PlayRow(
                    enabled = data.selected != null,
                    onLaunch = actions.onLaunch,
                    onOptions = actions.onOptions,
                    modifier = Modifier,
                    launching = data.launching,
                )
            }
            StageRing(
                progress = ready / 100f * entrance,
                value = (ready * entrance).roundToInt().toString(),
                label = stringResource(R.string.gs_ready_title),
                from = GameSpacePalette.VioletDeep,
                to = accent,
                warn = false,
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/** الأيقونة كمرساة مادّيّة: لوح داكن، حافة مُضاءة أعلى، ولمعان زجاجيّ ساكن. */
@Composable
private fun CockpitHeroIcon(
    packageName: String?,
    accent: Color,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(MaxRadius.tile)
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                // هالة اللون تحت الأيقونة: تُرسم مرّة واحدة، ولا ظلّ متحرّك ولا ضباب.
                val radius = this.size.minDimension * 0.78f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.45f), Color.Transparent),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                )
            }
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.55f), GameSpacePalette.Glass, CockpitFloor)
                )
            )
            .border(MaxSize.hairlineBorder, CockpitEdgeLight, shape),
        contentAlignment = Alignment.Center,
    ) {
        // وجه علويّ لامع وضلع سفليّ داكن: تُقرأ جسمًا لا بطاقة مسطّحة.
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.46f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(listOf(CockpitGloss, Color.Transparent))
                )
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(MaxSpace.xs)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0x66000000)))
                )
        )
        if (packageName != null) {
            AppIconImage(packageName = packageName, size = size - MaxSpace.md)
        }
    }
}

/** رفّ الألعاب: مسار اختيار اللعبة الوحيد، وآخره «كل الألعاب». */
@Composable
private fun CockpitShelf(data: StageData, actions: StageActions, entrance: Float) {
    if (data.games.isEmpty()) return
    val state = rememberLazyListState()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val focus = data.games.indexOfFirst { it.packageName == data.selected?.packageName }
    Box(Modifier.fillMaxWidth().height(CockpitShelfHeight).clipToBounds()) {
        Box(
            Modifier
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
            itemsIndexed(data.games, key = { _, game -> game.packageName }) { index, game ->
                val depth = (index - focus).coerceIn(-2, 2)
                val base = index * CockpitShelfStagger
                val presence = ((entrance - base) / (1f - base).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
                CockpitShelfTile(
                    game = game,
                    selected = index == focus,
                    yaw = -depth * CockpitTileYaw * (if (rtl) -1f else 1f),
                    presence = presence,
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
private fun CockpitShelfTile(
    game: GameApp,
    selected: Boolean,
    yaw: Float,
    presence: Float,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(MaxRadius.tile)
    Column(
        modifier = Modifier
            .width(CockpitTileWidth)
            .graphicsLayer {
                // منظور الرفّ: البطاقة المختارة إلى الأمام، وجاراها يدوران عنها في العمق.
                rotationY = yaw
                cameraDistance = CockpitShelfCamera * density
                val scale = 0.94f + 0.06f * presence + if (selected) 0.04f else 0f
                scaleX = scale
                scaleY = scale
                alpha = presence
                translationY = (1f - presence) * CockpitShelfDrop
            }
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
            AppIconImage(game.packageName, CockpitTileIcon - MaxSpace.md)
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
