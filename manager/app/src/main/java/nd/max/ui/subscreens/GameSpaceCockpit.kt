/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import android.animation.ValueAnimator
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.island.engine.dominantColor
import nd.max.ui.component.AppIconCache
import nd.max.ui.component.AppIconImage
import nd.max.ui.component.lobbyPress
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.theme.DisplayFontFamily
import kotlin.math.roundToInt

/**
 * **كوكبيت اللوبي الأفقيّ — مشهد مُضاء لا لوحة أشرطة (جولة ٢٩٨).**
 *
 * سبب إعادة الكتابة: حكم المالك على التركيبات السابقة («بسيطة جدًا كأنها مرسومة بالـCPU لا بالـGPU»)
 * كان صحيحًا تقنيًّا. القياس: كل طبقة فيها كانت `background = تدرّج واحد`، ولوح واحد داخل صفّ ارتفاعه
 * ١٨٤dp، فلا عمق ولا ضوء. والمراجع التي يقصدها تفوز بثلاثة أشياء لا بالزخرفة: **أرضيّة بمنظور**
 * تُثبّت الكاميرا، **هالات إضافيّة** (الضوء يُجمع بـ`Plus` لا يُغطّى)، و**عمق ميدانيّ** (عناصر خارج
 * التركيز في الخلف).
 *
 * فالبنية الآن: `CockpitScene` (قاعدة + شبكة أرضيّة + ٣ هالات + حجب + فينييت + غبار) ← `CockpitBokeh`
 * (أيقونات ألعاب أخرى خارج التركيز: تُصغَّر إلى بكسلات ثم تُكبَّر بـ`ContentScale.Crop`) ←
 * `CockpitSlab` (اللوح ٣٠٠dp يخرج من الحافتين العليا والبداية، مادّته أيقونة اللعبة نفسها) ← الأشرطة.
 *
 * **ولا أصل جديد ولا صورة:** كل ما يُرسم هنا إمّا تدرّج أو أيقونة التطبيق المثبَّتة (`AppIconCache`).
 * وقياس التركيبة في نموذج ١:١ بمقاس ٨٥٠×٣٩٢dp: `scroll 1700×784` بلا تجاوز، اللوح `[−60,−60,600,600]px`,
 * كتلة الاسم `900×102px`، زرّ البدء `254×106px`، التنقّل `1700×112px`.
 *
 * **وقواعد ثابتة:** كل هدف لمس ≥ ٤٨dp · أصغر نصّ ١٢sp · لا كتابة عتاد (ADR-11) · المجهول «—» لا صفر
 * (ADR-07) · الحركة الوحيدة دخول ≤ 360ms ويتوقّف كليًّا إن أوقف النظام المؤثّرات.
 */
internal const val CockpitUnknown = "—"

/** خمسة أبواب في الشريط السفليّ؛ و«الملفّ» و«الإعدادات» في قائمة ⋯ فلا يزدحم الشريط. */
internal val CockpitNavPages = listOf(
    LobbyPage.Home,
    LobbyPage.Library,
    LobbyPage.Performance,
    LobbyPage.Tools,
    LobbyPage.Features,
)

/** ارتفاع شريط التنقّل — يحجزه الغلاف أسفل المشهد في مسار الأقسام أيضًا. */
internal val CockpitNavHeight = 56.dp

internal val CockpitTopBarHeight = MaxSize.minTouchTarget
internal val CockpitShelfHeight = 84.dp
internal val CockpitTileWidth = 68.dp
internal val CockpitTileBox = 64.dp
internal val CockpitTileBoxSelected = 76.dp
internal val CockpitTileIcon = 44.dp
internal val CockpitTileIconSelected = 52.dp
internal val CockpitNavIndicatorWidth = 32.dp
internal val CockpitNavIndicatorHeight = 3.dp
internal val CockpitHeroTextMax = 440.dp
internal val CockpitHeroName = 52.sp
internal val CockpitHeroIndexGap = 18.dp

/** اللوح: ٣٠٠dp يخرج من الحافة كي تُقرأ الشاشة مشهدًا لا صفًّا. */
internal val CockpitSlabSize = 300.dp
internal val CockpitSlabBleed = 30.dp
internal val CockpitSlabRadius = 56.dp
internal val CockpitSlabEmblem = 162.dp
internal val CockpitSlabRim = 2.dp
internal val CockpitSlabHairline = 1.dp
/** نسيج اللوح: الأيقونة تُكبَّر وتخفت فتُقرأ مادّة، لا أيقونةً مكرّرة داخل إطار. */
internal const val CockpitSlabMaterialScale = 1.6f
internal const val CockpitSlabMaterialAlpha = 0.5f

/** الأزرار: أبيض واحد للفعل الأساسيّ، ودائرة زجاجيّة للخيارات. */
internal val CockpitPlayHeight = 52.dp
internal val CockpitPlayDot = 38.dp
internal val CockpitPlayGlyph = 20.dp
internal val CockpitRoundButton = 52.dp
internal val CockpitRoundGlyph = 22.dp
internal val CockpitDividerWidth = 1.dp
internal val CockpitDividerHeight = 26.dp

/** عتبة لون الحرارة: تحت ٤٥° يبقى الحياد، وفوقها تحذير — لا لون لمجرّد الزينة. */
internal const val CockpitHeatWarn = 45f
internal val CockpitHeatTone = Color(0xFFFBBF24)
internal val CockpitEdgeLight = Color(0x33FFFFFF)
internal val CockpitSkyTop = Color(0xFF0B0820)
internal val CockpitSkyMid = Color(0xFF070513)
internal val CockpitFloor = Color(0xFF04030A)
internal val CockpitScrim = Color(0xFF04030A)
internal val CockpitWarm = Color(0xFFFFBE86)
internal val CockpitCool = Color(0xFF28D2EB)
internal val CockpitGlassVeil = Color(0x14FFFFFF)
internal val CockpitSlabShade = Color(0xC405040C)

/** هندسة المشهد: أفق عند ٦٢٪، شبكة تتقارب، وهالات بألفة صغيرة تُجمع بالضوء. */
internal const val CockpitHorizon = 0.62f
internal const val CockpitGridRays = 15
internal const val CockpitGridRows = 11
internal const val CockpitGridReach = 1.9f
internal const val CockpitGridAlpha = 0.10f
internal val CockpitGridStroke = 1.dp
internal const val CockpitHaloAccent = 0.34f
internal const val CockpitHaloCool = 0.12f
internal const val CockpitHaloWarm = 0.10f

/** العمق الميدانيّ: الأيقونات الأخرى تُصغَّر إلى ١٦px ثم تُكبَّر، فالضباب حقيقيّ لا مرشّح. */
internal const val CockpitBokehPx = 16
internal const val CockpitBokehCount = 3
internal const val CockpitDustCount = 54
internal const val CockpitDustSeed = 11

/**
 * مادّة اللوح تُصغَّر إلى ٢٤px ثم تُكبَّر إلى ٣٠٠dp: هذا هو التمويه نفسه، لا مرشّحًا.
 * (كانت الأيقونة تُرسم بحجمها الكامل خلف صبغة، فتقرأ أيقونةً ثانيةً داخل الأولى — وهو عيب «مسطَّح».)
 */
internal const val CockpitSlabMaterialPx = 24

/** حبيبات: نسيج ثابت ٦٤px يُبلَّط، وهو آخر ما يفصل «مُصيَّرًا» عن «مرسومًا» في الشاشة. */
internal const val CockpitGrainPx = 64
internal const val CockpitGrainAlpha = 0.055f
internal const val CockpitGrainMax = 24

/** انحراف لونيّ: الهالة تُكرَّر بإزاحة بكسلين، حمراء وباردة، فتُقرأ عدسةً لا بقعة. */
internal const val CockpitFringePx = 4f
internal const val CockpitFringeWarm = 0.05f
internal const val CockpitFringeCool = 0.05f

/** استخراج لون الأيقونة: عيّنة صغيرة تكفي، ومحاولات قصيرة قبل الرجوع إلى البنفسجي. */
internal const val CockpitAccentTries = 6
internal const val CockpitAccentRetryMs = 140L

/** منظور الرفّ: زاوية الجار، وخطوة العمق، وتتابع الظهور. */
internal const val CockpitTileYaw = 14f
internal const val CockpitShelfCamera = 10f
internal const val CockpitShelfStagger = 0.06f

@Composable
internal fun GameSpaceCockpit(
    data: StageData,
    actions: StageActions,
    @Suppress("UNUSED_PARAMETER") currentPage: LobbyPage,
    onPage: (LobbyPage) -> Unit,
    topActions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    // لون اللعبة يقود الضوء كلّه؛ ويُرجَع إلى البنفسجي بلا وميض إن لم يُقرأ.
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
    val index = data.games.indexOfFirst { it.packageName == data.selected?.packageName }
    Box(modifier.fillMaxSize().clipToBounds()) {
        CockpitScene(accent = accent, ignition = entrance.value, modifier = Modifier.matchParentSize())
        CockpitBokeh(
            games = data.games,
            selected = data.selected?.packageName,
            modifier = Modifier.matchParentSize(),
        )
        CockpitSlab(
            packageName = data.selected?.packageName,
            accent = accent,
            ignition = entrance.value,
            modifier = Modifier.matchParentSize(),
        )
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
                CockpitHeroText(
                    data = data,
                    actions = actions,
                    accent = accent,
                    index = index,
                    total = data.games.size,
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
            color = GameSpacePalette.Ink,
            fontSize = 22.sp,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            modifier = Modifier
                .height(CockpitTopBarHeight * 0.66f)
                .drawBehind {
                    drawCircle(
                        color = GameSpacePalette.VioletSoft.copy(alpha = 0.55f),
                        radius = size.minDimension * 1.4f,
                        blendMode = BlendMode.Plus,
                    )
                },
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
                fontSize = 15.sp,
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
                else GameSpacePalette.Glass.copy(alpha = 0.55f)
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

/**
 * كتلة الاسم والفعل: على الطرف المقابل للوح، فاللوح هو الغلاف والاسم هو العنوان.
 *
 * والاسم `TextStyle(brush = …)` لا لونًا مسطّحًا: التدرّج من الأبيض إلى اللافندر يجعل الحروف
 * تقرأ مُضاءة من أعلى، وهو الفرق بين عنوان في تطبيق وعنوان في لعبة.
 */
@Composable
private fun CockpitHeroText(
    data: StageData,
    actions: StageActions,
    accent: Color,
    index: Int,
    total: Int,
    entrance: Float,
    modifier: Modifier = Modifier,
) {
    val ready = data.readinessPercent.coerceIn(0, 100)
    Box(modifier.padding(horizontal = MaxSpace.xl, vertical = MaxSpace.md)) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .widthIn(max = CockpitHeroTextMax)
                .graphicsLayer {
                    alpha = entrance
                    translationY = (1f - entrance) * MaxSpace.md.toPx() * 2f
                },
            horizontalAlignment = Alignment.End,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = data.statusTitle,
                    color = GameSpacePalette.Ink.copy(alpha = 0.86f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = CockpitHeroTextMax * 0.72f),
                )
                Spacer(modifier = Modifier.width(MaxSpace.sm))
                Box(
                    Modifier
                        .size(MaxSpace.sm)
                        .drawBehind {
                            val r = size.minDimension / 2f
                            drawCircle(GameSpacePalette.Mint.copy(alpha = 0.55f), r)
                            drawCircle(GameSpacePalette.Mint, r * 0.45f)
                        }
                )
                Spacer(modifier = Modifier.width(CockpitHeroIndexGap))
                if (index >= 0) {
                    Text(
                        text = "${(index + 1).toString().padStart(2, '0')} / ${total.toString().padStart(2, '0')}",
                        color = GameSpacePalette.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = data.selected?.label ?: stringResource(R.string.game_space_title),
                style = TextStyle(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White,
                            Color.White,
                            GameSpacePalette.Lavender,
                        )
                    ),
                    fontSize = CockpitHeroName,
                    lineHeight = CockpitHeroName * 1.04f,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.4).sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val tagline = data.tagline
            if (!tagline.isNullOrBlank()) {
                Text(
                    text = tagline,
                    color = GameSpacePalette.Ink.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(MaxSpace.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CockpitPlay(
                    enabled = data.selected != null,
                    launching = data.launching,
                    accent = accent,
                    onLaunch = actions.onLaunch,
                )
                Spacer(modifier = Modifier.width(MaxSpace.sm))
                CockpitRoundAction(
                    icon = Icons.Rounded.Tune,
                    description = stringResource(R.string.gs_more),
                    onClick = actions.onOptions,
                )
                Spacer(modifier = Modifier.width(MaxSpace.md))
                Box(
                    Modifier
                        .width(CockpitDividerWidth)
                        .height(CockpitDividerHeight)
                        .background(CockpitEdgeLight)
                )
                Spacer(modifier = Modifier.width(MaxSpace.md))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.gs_percent_value, ready),
                        color = GameSpacePalette.Ink,
                        fontSize = 24.sp,
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(R.string.gs_ready_title),
                        color = GameSpacePalette.Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * مبدّل الألعاب: صفّ أيقونات يطفو فوق الأرضيّة، والمختارة وحدها تكبر وتُحاط بخيط أبيض.
 *
 * بلا أسماء: الاسم مكتوب فوقها بخطّ ضخم أصلًا — وتكراره في كلّ مربّع هو ما جعل الشاشة السابقة
 * مزدحمة. والاسم باقٍ لقارئ الشاشة عبر `contentDescription` فلا تضيع الوصوليّة بحذف النصّ الظاهر.
 */
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
                        listOf(Color.Transparent, GameSpacePalette.GlassDeep.copy(alpha = 0.72f))
                    )
                )
        )
        LazyRow(
            modifier = Modifier.fillMaxSize().padding(horizontal = MaxSpace.xl),
            state = state,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
            verticalAlignment = Alignment.CenterVertically,
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
    Box(
        modifier = Modifier
            .width(CockpitTileWidth)
            .graphicsLayer {
                // منظور الرفّ: المختارة إلى الأمام، وجاراها يدوران عنها في العمق.
                rotationY = yaw
                cameraDistance = CockpitShelfCamera * density
                val scale = if (selected) 1f else 0.92f + 0.08f * presence
                scaleX = scale
                scaleY = scale
                alpha = presence
                translationY = (1f - presence) * MaxSpace.sm.toPx()
            }
            .semantics { contentDescription = game.label }
            .lobbyPress(interaction, 0.94f)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                        .drawBehind {
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    listOf(GameSpacePalette.VioletSoft.copy(alpha = 0.55f), Color.Transparent),
                                    center = center,
                                    radius = size.minDimension,
                                ),
                                cornerRadius = CornerRadius(MaxRadius.tile.toPx()),
                                blendMode = BlendMode.Plus,
                            )
                        }
                        .border(MaxSize.activeRing, Color.White.copy(alpha = 0.92f), shape)
                } else {
                    Modifier.border(MaxSize.hairlineBorder, CockpitEdgeLight, shape)
                }
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(MaxSpace.xs),
        contentAlignment = Alignment.Center,
    ) {
        AppIconImage(
            packageName = game.packageName,
            size = if (selected) CockpitTileIconSelected else CockpitTileIcon,
            contentDescription = null,
        )
        if (!selected) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(GameSpacePalette.GlassDeep.copy(alpha = 0.42f))
            )
        }
    }
}

@Composable
private fun CockpitSeeAll(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(MaxRadius.tile)
    Box(
        modifier = Modifier
            .width(CockpitTileWidth)
            .height(CockpitTileBox)
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
                .background(
                    if (active) {
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, GameSpacePalette.Lavender, Color.Transparent)
                        )
                    } else {
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                )
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
