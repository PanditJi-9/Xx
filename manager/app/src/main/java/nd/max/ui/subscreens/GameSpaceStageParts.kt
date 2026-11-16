/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.first
import nd.max.R
import nd.max.ui.component.FitText
import nd.max.ui.component.lobbyIntro
import nd.max.ui.component.lobbyPress
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import nd.max.ui.theme.DisplayFontFamily

/*
 * أجزاء اللوحة الثابتة — الشريط الجانبيّ، شريط الحالة، لوحات الأداء الثلاث، وقاعدة الحالة.
 *
 * **كل رقم هنا بكسل من اللقطة** (١٦٧٢×٩٤١): انظر `GameSpaceStage.kt`. فلا رموز `MaxSpace`/`MaxSize` هنا
 * (قياساتها بالـdp الفعليّ لا بوحدة اللقطة)، بل ثوابت مسمّاة تحمل المقاس المرسوم. وبوّابة `design_tokens`
 * تعدّ `RoundedCornerShape(N.dp)` و`.border(N.dp` و`spacedBy(N.dp)` وتمنع زيادتها، فكل قياس منها اسمٌ.
 *
 * اللغة البصريّة: ألواح زجاجيّة داكنة بحدّ أبيض خافت، والبنفسجيّ للتفاعل وحده (العنصر النشط، الأقواس،
 * المنحنى). ما لم يُقرأ يبقى «—» وحلقةً بلا قوس لا صفرًا (ADR-07).
 *
 * ### الحياة (جولة ٢٠٢٦-١١-١٥)
 *
 *  - **دخول متعاقب**: كل لوح ينساب إلى مكانه بتأخّر نسبيّ على [LocalStageIntro] (يبدأ بعد غطاء الدخول).
 *  - **عدّ تصاعديّ**: الحلقات والبطارية وشريط الجاهزيّة تملأ من الصفر إلى القراءة الحقيقيّة عند ظهور اللوح
 *    ([rememberCountUp])، ثم تتبع كل قراءة جديدة بسلاسة. الرقم المكتوب دائمًا هو القراءة نفسها لا الحركة.
 *  - **ضغط زنبركيّ** على كل ما يُلمس، وبند الشريط يضيء بانتقال لا بقفزة.
 *  - ما يتحرّك باستمرار يقرأ [LocalStageClock] في مرحلة الرسم وحدها (نبض «Live»، لمعة الشريط السفليّ،
 *    أعمدة الإطارات)، فلا إعادة تركيب أثناء الحركة؛ وكلّه ساكن إن أوقف النظام الأنيميشن.
 */

private val RadiusRail = 40.dp
private val RadiusItem = 16.dp
private val RadiusCard = 24.dp
private val StrokeFine = 1.dp
private val StrokeFirm = 1.5.dp
private val GapXS = 6.dp
private val GapS = 10.dp
private val GapM = 16.dp
private val GapL = 24.dp
private val PadCard = 22.dp
private val PadWide = 22.dp
private val AvatarSize = 36.dp
private val GearIcon = 40.dp
private val GlyphRail = 30.dp
private val GlyphRailBig = 34.dp
private val GlyphStatus = 30.dp
private val RingSize = 106.dp
private val RingLabelIcon = 26.dp
private val BatteryIcon = 54.dp
private val ModeIcon = 44.dp
private val ModeGlyph = 28.dp
private val ChevronIcon = 28.dp
private val BaseIcon = 30.dp
private val BarWidth = 393.dp
private val BarHeight = 18.dp
private val MemoryBarHeight = 12.dp
private val PulseSize = 10.dp

private const val BarCells = 25
private const val MemoryCells = 24
private const val BaseGlintCycles = 4
private const val CountUpMs = 900
private const val CountUpGate = 0.30f
private const val DegToRad = 0.017453292f

/** أقصى وأدنى حجم لاسم بند الشريط (sp): يصغر داخل هذا المدى حتى يبقى في سطر واحد. */
private const val RailLabelMax = 17f
private const val RailLabelMin = 12f

/** شدّة اللمعة العلويّة على الزجاج: وهج أبيض ينطفئ قبل منتصف اللوح فيبدو سطحًا مصقولًا. */
private const val GlassSheen = 0.07f

internal val GlassBorder = Brush.linearGradient(
    listOf(
        LuxGold.copy(alpha = 0.55f),
        Color.White.copy(alpha = 0.07f),
        Color.White.copy(alpha = 0.04f),
        LuxGold.copy(alpha = 0.26f)
    )
)
internal val GlassFill = Brush.verticalGradient(
    listOf(Color(0xFF1B1840).copy(alpha = 0.80f), GameSpacePalette.GlassDeep.copy(alpha = 0.90f))
)
private val RailFill = Brush.verticalGradient(
    listOf(GameSpacePalette.Glass.copy(alpha = 0.86f), GameSpacePalette.GlassDeep.copy(alpha = 0.93f))
)
internal val ActiveFill = Brush.horizontalGradient(
    listOf(GameSpacePalette.VioletDeep.copy(alpha = 0.65f), GameSpacePalette.Plum.copy(alpha = 0.92f))
)
private val BrandBrush = Brush.linearGradient(
    listOf(LuxIvory, LuxGold, LuxGoldDeep)
)
private val AvatarBrush = Brush.linearGradient(
    listOf(GameSpacePalette.VioletSoft, GameSpacePalette.VioletDeep)
)
private val AvatarRing = Brush.sweepGradient(
    listOf(LuxGold, LuxIvory, LuxGoldDeep, LuxGold)
)
private val ModeOnFill = Brush.linearGradient(
    listOf(GameSpacePalette.Violet.copy(alpha = 0.60f), GameSpacePalette.VioletDeep.copy(alpha = 0.40f))
)
private val ModeOffFill = Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.04f))
)

/** قاعدة الحالة: مستطيل مستدير الزوايا بقطع مائل في الزاوية السفلى اليمنى كما في اللقطة. */
private val BaseShape = GenericShape { size, _ ->
    val r = size.height * 0.28f
    val c = size.height * 0.52f
    moveTo(r, 0f)
    lineTo(size.width - r, 0f)
    arcTo(Rect(size.width - 2f * r, 0f, size.width, 2f * r), -90f, 90f, false)
    lineTo(size.width, size.height - c)
    lineTo(size.width - c, size.height)
    lineTo(r, size.height)
    arcTo(Rect(0f, size.height - 2f * r, 2f * r, size.height), 90f, 90f, false)
    lineTo(0f, r)
    arcTo(Rect(0f, 0f, 2f * r, 2f * r), 180f, 90f, false)
    close()
}

/** سطح زجاجيّ داكن بحدّ أبيض خافت — لغة كل اللوحات في اللقطة. */
internal fun Modifier.stageGlass(shape: Shape): Modifier =
    this
        .clip(shape)
        .background(GlassFill)
        .drawBehind {
            drawRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = GlassSheen),
                    0.42f to Color.Transparent
                )
            )
        }
        .border(StrokeFine, GlassBorder, shape)

/**
 * عدّ تصاعديّ 0 → [target]: يبدأ حين يتجاوز تقدّم الدخول [CountUpGate] (أي حين يظهر اللوح فعلًا لا خلف
 * غطاء الدخول)، وكل [target] جديد بعدها ينتقل إليه بالحركة نفسها. يُقرأ في الرسم فقط.
 */
@Composable
private fun rememberCountUp(target: Float): State<Float> {
    val intro = LocalStageIntro.current
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) {
        if (intro.value < CountUpGate) snapshotFlow { intro.value }.first { it >= CountUpGate }
        anim.animateTo(target, tween(durationMillis = CountUpMs, easing = FastOutSlowInEasing))
    }
    return anim.asState()
}

/** نقطة نابضة بلون [color]؛ تقرأ الساعة في الرسم فقط. */
@Composable
internal fun PulseDot(color: Color, modifier: Modifier = Modifier) {
    val clock = LocalStageClock.current
    Box(
        modifier.drawBehind {
            val pulse = stageWave(clock.value, 30)
            drawCircle(
                color.copy(alpha = 0.22f + 0.18f * pulse),
                size.minDimension * (0.62f + 0.38f * pulse)
            )
            drawCircle(color.copy(alpha = 0.60f + 0.40f * pulse), size.minDimension * 0.34f)
        }
    )
}

/** نبرة البطارية: الأخضر عاديّ، البرتقاليّ منخفض، الأحمر حرج، «—» رماديّ حين لا قراءة. */
private fun batteryTone(percent: Int?, charging: Boolean): Color = when {
    percent == null -> GameSpacePalette.Muted
    charging -> GameSpacePalette.Mint
    percent <= 10 -> GameSpacePalette.Magenta
    percent <= 20 -> GameSpacePalette.Orange
    else -> GameSpacePalette.Mint
}

/**
 * الشريط الجانبيّ بإحداثيات اللقطة: لوح (٠،٢٤، ١٦٨×٨٩٢)، شعار عند y≈٨٥، «Home» في (١٤،١٥٢)
 * بارتفاع ٦٣، ثم خمسة أقسام بارتفاع ٦٥ لكلٍّ، والترس مركزه (٨٠،٨٧٥).
 *
 * ثابت فوق كل الصفحات: [current] الصفحة الظاهرة فيُضاء بندها بانتقال، ولمس أي بند ينقل المُصفِّح الرأسيّ
 * إلى صفحته — السحب واللمس طريقان إلى المكان نفسه.
 */
@Composable
internal fun StageRail(current: LobbyPage, onPage: (LobbyPage) -> Unit) {
    val intro = LocalStageIntro.current
    val clock = LocalStageClock.current
    val drop = LocalStageDrop.current
    val railBottom = StageGeo.height(drop)
    val frame = RoundedCornerShape(RadiusRail)
    Box(
        Modifier
            .at(0, 24, 168, railBottom - 49)
            .lobbyIntro(intro, fromStart = 60.dp)
            .clip(frame)
            .background(RailFill)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(GameSpacePalette.Violet.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.5f, 0f),
                        radius = size.width * 1.4f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(GameSpacePalette.Magenta.copy(alpha = 0.09f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height),
                        radius = size.width * 1.3f
                    )
                )
            }
            .border(StrokeFine, GlassBorder, frame)
    )
    Box(
        modifier = Modifier
            .at(0, 40, 168, 90)
            .lobbyIntro(intro, scaleFrom = 0.7f, start = 0.04f)
            .drawBehind {
                val breath = stageWave(clock.value, 3)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GameSpacePalette.VioletSoft.copy(alpha = 0.26f + 0.18f * breath),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * (0.66f + 0.08f * breath)
                    ),
                    radius = size.minDimension * 0.76f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.gs_brand_mark),
            style = TextStyle(
                brush = BrandBrush,
                fontSize = 52.sp,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Black
            ),
            maxLines = 1,
            modifier = Modifier.stageTextShine(clock, cycles = 2)
        )
    }
    // التخطيط المضغوط: بنود الشريط تتّسع لما يتيحه ارتفاع الشاشة (حتى ٩٢) فيسهل لمسها؛ المرجعيّ ٧٦/٧٨ كما هو.
    val sectionCount = LobbyPage.entries.count { it != LobbyPage.Home && it != LobbyPage.Settings }
    val itemH = if (drop == 0) 78 else ((railBottom - 258 - 18) / (sectionCount + 1)).coerceIn(78, 92)
    val homeH = if (drop == 0) 76 else itemH - 2
    Column(Modifier.at(14, 152, 154, maxOf(490, homeH + 18 + sectionCount * itemH))) {
        StageRailItem(
            labelRes = LobbyPage.Home.labelRes,
            icon = LobbyPage.Home.icon,
            active = current == LobbyPage.Home,
            height = homeH,
            order = 0,
            onClick = { onPage(LobbyPage.Home) }
        )
        Spacer(Modifier.height(18.dp))
        LobbyPage.entries
            .filter { it != LobbyPage.Home && it != LobbyPage.Settings }
            .forEachIndexed { index, page ->
                StageRailItem(
                    labelRes = page.labelRes,
                    icon = page.icon,
                    active = current == page,
                    height = itemH,
                    order = index + 1,
                    onClick = { onPage(page) }
                )
            }
    }
    val gearActive = current == LobbyPage.Settings
    val gearInteraction = remember { MutableInteractionSource() }
    val gearSelection by animateFloatAsState(
        targetValue = if (gearActive) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "railGearSelection"
    )
    Box(
        modifier = Modifier
            .at(48, railBottom - 98, 64, 64)
            .lobbyIntro(intro, fromBelow = 30.dp, start = 0.36f)
            .lobbyPress(gearInteraction, 0.90f)
            .clip(CircleShape)
            .drawBehind {
                if (gearSelection > 0.01f) {
                    val rim = StrokeFirm.toPx()
                    drawCircle(brush = ActiveFill, alpha = gearSelection)
                    drawCircle(
                        color = GameSpacePalette.VioletSoft.copy(alpha = 0.85f * gearSelection),
                        radius = size.minDimension / 2f - rim / 2f,
                        style = Stroke(width = rim)
                    )
                }
            }
            .selectable(
                selected = gearActive,
                interactionSource = gearInteraction,
                indication = null,
                role = Role.Tab,
                onClick = { onPage(LobbyPage.Settings) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = LobbyPage.Settings.icon,
            contentDescription = stringResource(R.string.gs_nav_settings),
            tint = lerp(GameSpacePalette.Ink.copy(alpha = 0.80f), Color.White, gearSelection),
            modifier = Modifier
                .size(GearIcon)
                .graphicsLayer { rotationZ = 90f * gearSelection }
        )
    }
}

@Composable
private fun StageRailItem(
    labelRes: Int,
    icon: ImageVector,
    active: Boolean,
    height: Int,
    order: Int,
    onClick: () -> Unit,
) {
    val intro = LocalStageIntro.current
    val interaction = remember { MutableInteractionSource() }
    val selection by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "railSelection"
    )
    val shape = RoundedCornerShape(RadiusItem)
    val tint = lerp(GameSpacePalette.Ink.copy(alpha = 0.80f), LuxGold, selection)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .lobbyIntro(intro, fromStart = 44.dp, start = 0.08f + 0.05f * order)
            .lobbyPress(interaction, 0.95f)
            .clip(shape)
            .drawBehind {
                if (selection > 0.01f) {
                    val rim = StrokeFirm.toPx()
                    val radius = RadiusItem.toPx()
                    drawRoundRect(
                        brush = ActiveFill,
                        cornerRadius = CornerRadius(radius),
                        alpha = selection
                    )
                    // وهج داخليّ من جهة الأيقونة: يعطي البند النشط عمقًا بدل تعبئة مسطّحة.
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                LuxGold.copy(alpha = 0.20f * selection),
                                Color.Transparent
                            ),
                            center = Offset(size.height * 0.55f, size.height * 0.5f),
                            radius = size.width * 0.85f
                        ),
                        cornerRadius = CornerRadius(radius)
                    )
                    drawRoundRect(
                        color = LuxGold.copy(alpha = 0.80f * selection),
                        topLeft = Offset(rim / 2f, rim / 2f),
                        size = Size(size.width - rim, size.height - rim),
                        cornerRadius = CornerRadius(radius - rim / 2f),
                        style = Stroke(width = rim)
                    )
                    val barHeight = size.height * 0.46f * selection
                    // توهّج نيون خلف الشريط: نطاق يخبو نحو الداخل، فيبدو الشريط مضيئًا لا مرسومًا.
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                LuxGold.copy(alpha = 0.55f * selection),
                                LuxGold.copy(alpha = 0f)
                            ),
                            startX = 0f,
                            endX = rim * 10f
                        ),
                        topLeft = Offset(0f, (size.height - barHeight * 1.7f) / 2f),
                        size = Size(rim * 10f, barHeight * 1.7f),
                        cornerRadius = CornerRadius(rim * 2f)
                    )
                    drawRoundRect(
                        color = LuxGold,
                        topLeft = Offset(0f, (size.height - barHeight) / 2f),
                        size = Size(rim * 2.4f, barHeight),
                        cornerRadius = CornerRadius(rim)
                    )
                }
            }
            .selectable(
                selected = active,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GapS)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(if (LocalStageDrop.current > 0) GlyphRailBig else GlyphRail)
                .graphicsLayer {
                    val k = 1f + 0.08f * selection
                    scaleX = k
                    scaleY = k
                }
        )
        // سطر واحد دائمًا: «مكتبة الألعاب» و«ملف اللعبة» كانا ينكسران فيختلف إيقاع البنود، فيصغر الاسم حتى يتّسع.
        FitText(
            text = stringResource(labelRes),
            color = tint,
            maxSp = RailLabelMax,
            minSp = RailLabelMin,
            modifier = Modifier.weight(1f),
            weight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            align = TextAlign.Start
        )
    }
}

/**
 * شريط الحالة العلويّ: الساعة والتاريخ يسارًا (x=٢٠٠)، واتّصال + بطارية + نسبة + صورة «M» يمينًا.
 * أزرار اللوبي (استكشاف/مفضّلة/سجلّ/إغلاق) تُوضع بين الطرفين بمقاسها **الفعليّ** — بإعادة
 * `LocalDensity` الأصلي داخل منطقتها — فتبقى قابلة للّمس مهما صغّرت اللوحة.
 */
@Composable
internal fun StageStatusBar(
    data: StageData,
    onProfile: () -> Unit,
    realDensity: Density,
    topActions: @Composable () -> Unit,
) {
    val link by rememberLobbyLink()
    val intro = LocalStageIntro.current
    val extra = LocalStageExtra.current
    Row(
        modifier = Modifier
            .at(200, 22, 480, 40)
            .lobbyIntro(intro, fromBelow = (-24).dp, start = 0.06f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GapM)
    ) {
        Text(
            text = data.clock,
            color = LuxIvory,
            fontSize = 23.sp,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        if (data.clock.isNotBlank() && data.date.isNotBlank()) {
            Box(
                Modifier
                    .size(PulseSize / 2)
                    .clip(CircleShape)
                    .background(LuxGold.copy(alpha = 0.85f))
            )
        }
        Text(
            text = data.date,
            color = LuxGold.copy(alpha = 0.90f),
            fontSize = 19.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    Box(
        modifier = Modifier
            .at(700, 0, 720 + extra, 86)
            .lobbyIntro(intro, fromBelow = (-24).dp, start = 0.08f),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalDensity provides realDensity) {
            Row(Modifier.wrapContentSize(align = Alignment.Center, unbounded = true)) {
                topActions()
            }
        }
    }
    Row(
        modifier = Modifier
            .at(1426 + extra, 22, 226, 44)
            .lobbyIntro(intro, fromBelow = (-24).dp, start = 0.10f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GapM, Alignment.End)
    ) {
        Icon(
            imageVector = when (link) {
                LobbyLink.Wifi -> Icons.Rounded.Wifi
                LobbyLink.Cellular -> Icons.Rounded.SignalCellularAlt
                LobbyLink.Offline -> Icons.Rounded.WifiOff
            },
            contentDescription = null,
            tint = if (link == LobbyLink.Offline) GameSpacePalette.Muted else GameSpacePalette.Ink,
            modifier = Modifier.size(GlyphStatus)
        )
        StageBatteryGlyph(percent = data.batteryPercent, charging = data.charging)
        Text(
            text = data.batteryPercent?.let { stringResource(R.string.gs_percent_value, it) }
                ?: MAX_VALUE_UNAVAILABLE,
            color = GameSpacePalette.Ink,
            fontSize = 19.sp,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        val profile = stringResource(R.string.lobby_tile_profile)
        val avatarInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(AvatarSize)
                .lobbyPress(avatarInteraction, 0.90f)
                .clip(CircleShape)
                .background(AvatarBrush)
                .border(StrokeFirm, AvatarRing, CircleShape)
                .semantics { contentDescription = profile }
                .clickable(
                    interactionSource = avatarInteraction,
                    indication = null,
                    role = Role.Button,
                    onClick = onProfile
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.gs_brand_mark),
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/** بطارية أفقيّة كما في اللقطة: إطار مجوّف بحلمة، وتعبئة بنسبة الشحن الحقيقيّة (سماويّة عند الشحن، حمراء عند الحرج). */
@Composable
private fun StageBatteryGlyph(percent: Int?, charging: Boolean) {
    val fillTone = when {
        charging -> GameSpacePalette.Cyan
        percent != null && percent <= 15 -> GameSpacePalette.Magenta
        else -> GameSpacePalette.Ink
    }
    Canvas(Modifier.size(width = 38.dp, height = 20.dp)) {
        val stroke = size.height * 0.11f
        val body = size.width * 0.92f
        val tone = GameSpacePalette.Ink.copy(alpha = 0.92f)
        drawRoundRect(
            color = tone,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(body - stroke, size.height - stroke),
            cornerRadius = CornerRadius(size.height * 0.28f),
            style = Stroke(width = stroke)
        )
        drawRoundRect(
            color = tone,
            topLeft = Offset(body + stroke, size.height * 0.32f),
            size = Size(size.width - body - stroke, size.height * 0.36f),
            cornerRadius = CornerRadius(stroke)
        )
        if (percent != null) {
            val fill = (body - stroke * 3f) * (percent / 100f).coerceIn(0f, 1f)
            drawRoundRect(
                color = fillTone,
                topLeft = Offset(stroke * 1.5f, stroke * 1.5f),
                size = Size(fill, size.height - stroke * 3f),
                cornerRadius = CornerRadius(size.height * 0.14f)
            )
        }
    }
}

/** أيقونة النبض في رأس لوحة الأداء. */
@Composable
private fun PulseLine(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, h * 0.5f)
            lineTo(w * 0.22f, h * 0.5f)
            lineTo(w * 0.34f, h * 0.08f)
            lineTo(w * 0.50f, h * 0.94f)
            lineTo(w * 0.62f, h * 0.28f)
            lineTo(w * 0.72f, h * 0.5f)
            lineTo(w, h * 0.5f)
        }
        drawPath(
            path = path,
            color = GameSpacePalette.VioletSoft.copy(alpha = 0.35f),
            style = Stroke(width = h * 0.26f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = path,
            color = GameSpacePalette.VioletSoft,
            style = Stroke(width = h * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * اللوحات الثلاث بمستطيلاتها في اللقطة: الأداء (١٢٣٦،٨٧، ٤١٠×٢٢٩)، البطارية ووضع الأداء
 * (١٢٣٦،٣٣٠، ٤١٠×٩١)، والإطارات اللحظيّة (١٢٣٦،٤٣٣، ٤١٠×٨٥). تنساب من يمين اللوحة واحدةً بعد أخرى.
 */
@Composable
internal fun StageBoard(data: StageData, onOpen: () -> Unit, onOpenSessions: () -> Unit) {
    val intro = LocalStageIntro.current
    val extra = LocalStageExtra.current
    val drop = LocalStageDrop.current
    // العرض الإضافيّ: الحافة اليمنى ثابتة، واللوحات تتّسع جهة اليسار بحصّةٍ محدودة ليبقى النصّ الأكبر فيها.
    val grow = stageRightGrow(extra)
    val x = stageBoardX(extra)
    val w = 410 + grow
    if (drop == 0) {
        PerformanceCard(
            data = data,
            onOpen = onOpen,
            modifier = Modifier
                .at(x, 87, w, 229)
                .lobbyIntro(intro, fromStart = (-70).dp, start = 0.18f)
        )
        StatusCard(
            data = data,
            modifier = Modifier
                .at(x, 330, w, 91)
                .lobbyIntro(intro, fromStart = (-70).dp, start = 0.24f),
            onOpen = onOpen
        )
        MemoryCard(
            ram = data.ram,
            modifier = Modifier
                .at(x, 433, w, 85)
                .lobbyIntro(intro, fromStart = (-70).dp, start = 0.30f)
        )
        return
    }
    // التخطيط المضغوط (هاتف عريض): العمود يمتدّ إلى أسفل الشاشة، واللوحات الثلاث تتمدّد رأسيًّا بمعامل واحد،
    // وتسكن تحتها قاعدة الحالة بدل شريطها السفليّ العريض.
    val col = stageColumn(drop)
    val statusY = 87 + col.perf + 14
    val fpsY = statusY + col.status + 12
    CompositionLocalProvider(LocalStageStretch provides col.kv) {
        PerformanceCard(
            data = data,
            onOpen = onOpen,
            modifier = Modifier
                .at(x, 87, w, col.perf)
                .lobbyIntro(intro, fromStart = (-70).dp, start = 0.18f)
        )
        StatusCard(
            data = data,
            modifier = Modifier
                .at(x, statusY, w, col.status)
                .lobbyIntro(intro, fromStart = (-70).dp, start = 0.24f),
            onOpen = onOpen
        )
        MemoryCard(
            ram = data.ram,
            modifier = Modifier
                .at(x, fpsY, w, col.fps)
                .lobbyIntro(intro, fromStart = (-70).dp, start = 0.30f)
        )
    }
    StageBase(
        title = data.statusTitle,
        detail = data.statusDetail,
        percent = data.readinessPercent,
        onOpen = onOpenSessions,
        compact = true,
        modifier = Modifier
            .at(x, col.baseTop, w, col.baseH)
            .lobbyIntro(intro, fromBelow = 40.dp, start = 0.36f)
    )
}

@Composable
internal fun PerformanceCard(data: StageData, onOpen: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(RadiusCard)
    val interaction = remember { MutableInteractionSource() }
    val live = data.cpu != null || data.gpu != null || data.heat != null
    Column(
        modifier = modifier
            .lobbyPress(interaction, 0.97f)
            .stageGlass(shape)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(GameSpacePalette.Violet.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.18f, 0f),
                        radius = size.width * 0.75f
                    )
                )
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onOpen
            )
            .padding(start = PadCard, top = 16.dp, end = 10.dp),
        verticalArrangement = if (LocalStageStretch.current > 1f) Arrangement.SpaceEvenly else Arrangement.Top
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GapM)
        ) {
            PulseLine(Modifier.size(width = 26.dp, height = 22.dp))
            Text(
                text = stringResource(R.string.gs_device_perf),
                color = GameSpacePalette.Ink,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (live) LivePill()
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth()) {
            StageRing(
                progress = data.cpu?.let { it / 100f },
                value = data.cpu?.let { stringResource(R.string.gs_percent_value, it) } ?: MAX_VALUE_UNAVAILABLE,
                label = stringResource(R.string.gs_cpu),
                from = GameSpacePalette.Violet,
                to = GameSpacePalette.VioletSoft,
                modifier = Modifier.weight(1f)
            )
            StageRing(
                progress = data.gpu?.let { it / 100f },
                value = data.gpu?.let { stringResource(R.string.gs_percent_value, it) } ?: MAX_VALUE_UNAVAILABLE,
                label = stringResource(R.string.gs_gpu),
                from = GameSpacePalette.Blue,
                to = GameSpacePalette.Cyan,
                modifier = Modifier.weight(1f)
            )
            StageRing(
                // مقياس الحرارة ٢٥–٦٥°م: القوس مقياس بصريّ، والرقم هو الخبر.
                progress = data.heat?.let { ((it - 25f) / 40f).coerceIn(0f, 1f) },
                value = data.heat?.let { "${it.roundToInt()}°C" } ?: MAX_VALUE_UNAVAILABLE,
                label = stringResource(R.string.gs_heat),
                from = GameSpacePalette.Magenta,
                to = GameSpacePalette.Orange,
                labelIcon = Icons.Rounded.Thermostat,
                warn = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** كبسولة «Live» بنقطة نابضة: لا تظهر إلا إن وُجدت قراءة واحدة على الأقل. */
@Composable
private fun LivePill() {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(GameSpacePalette.Mint.copy(alpha = 0.12f))
            .border(StrokeFine, GameSpacePalette.Mint.copy(alpha = 0.35f), CircleShape)
            .padding(start = 9.dp, end = 11.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GapXS)
    ) {
        PulseDot(GameSpacePalette.Mint, Modifier.size(PulseSize))
        Text(
            text = stringResource(R.string.gs_tele_live),
            color = GameSpacePalette.Mint,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/**
 * حلقة قراءة (١٠٦): مسار خافت، وقوس متدرّج بتوهّج يمتلئ بعدّ تصاعديّ ويحمل «خرزة» عند طرفه، والرقم في المركز
 * بوحدة أصغر. فوق ٨٢٪ يميل اللون نحو الأحمر/البرتقاليّ ([warn]). الغائب بلا قوس (ADR-07).
 */
@Composable
private fun StageRing(
    progress: Float?,
    value: String,
    label: String,
    from: Color,
    to: Color,
    modifier: Modifier = Modifier,
    labelIcon: ImageVector? = null,
    warn: Boolean = true,
) {
    val sweepState = rememberCountUp((progress ?: 0f).coerceIn(0f, 1f))
    val number = value.takeWhile { it.isDigit() }
    val unit = value.drop(number.length)
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(RingSize), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(RingSize)) {
                val shown = sweepState.value
                val strokeWidth = size.minDimension * 0.075f
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val hot = if (warn) ((shown - 0.82f) / 0.18f).coerceIn(0f, 1f) else 0f
                val c1 = lerp(from, GameSpacePalette.Magenta, hot)
                val c2 = lerp(to, GameSpacePalette.Orange, hot)
                if (progress != null) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(c2.copy(alpha = 0.14f), Color.Transparent),
                            center = center,
                            radius = size.minDimension / 2f
                        ),
                        radius = size.minDimension / 2f
                    )
                }
                // بلا قراءة: مسار متقطّع هادئ بدل حلقة ميّتة، فيُقرأ «لا قياس» لا «عطل».
                drawArc(
                    color = Color.White.copy(alpha = if (progress == null) 0.16f else 0.08f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = if (progress == null) {
                            PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 0.9f, strokeWidth * 1.5f))
                        } else {
                            null
                        }
                    )
                )
                if (progress != null && shown > 0.005f) {
                    val sweep = 360f * shown
                    val stop = shown.coerceIn(0.02f, 1f)
                    val brush = Brush.sweepGradient(0f to c1, stop to c2, 1f to c2)
                    rotate(degrees = -90f) {
                        drawArc(
                            brush = brush,
                            startAngle = 0f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            alpha = 0.25f,
                            style = Stroke(width = strokeWidth * 2.0f, cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = brush,
                            startAngle = 0f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    // خرزة عند طرف القوس: تحدّد القراءة بدقّة وتعطي الحلقة عمقًا.
                    val angle = (sweep - 90f) * DegToRad
                    val orbit = (size.minDimension - strokeWidth) / 2f
                    val knob = Offset(center.x + orbit * cos(angle), center.y + orbit * sin(angle))
                    drawCircle(
                        color = Color.White.copy(alpha = 0.30f),
                        radius = strokeWidth * 1.4f,
                        center = knob
                    )
                    drawCircle(color = Color.White, radius = strokeWidth * 0.46f, center = knob)
                }
            }
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 30.sp)) {
                        append(if (number.isEmpty()) value else number)
                    }
                    if (number.isNotEmpty() && unit.isNotEmpty()) {
                        withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal)) {
                            append(unit)
                        }
                    }
                },
                color = if (value == MAX_VALUE_UNAVAILABLE) GameSpacePalette.Muted else GameSpacePalette.Ink,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Row(
            modifier = Modifier.height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GapS)
        ) {
            if (labelIcon != null) {
                Icon(
                    imageVector = labelIcon,
                    contentDescription = null,
                    tint = to,
                    modifier = Modifier.size(RingLabelIcon)
                )
            }
            Text(
                text = label,
                color = GameSpacePalette.Ink.copy(alpha = 0.92f),
                fontSize = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StageDivider(height: Dp) {
    Box(
        Modifier
            .width(StrokeFine)
            .height(height)
            .background(Color.White.copy(alpha = 0.12f))
    )
}

/**
 * لوحة البطارية ووضع الأداء: نصفان يفصلهما خطّ رفيع. البطارية مرسومة بمستواها الحقيقيّ (تمتلئ بعدّ تصاعديّ
 * وتتلوّن بحسب الشحن)، وبطاقة الوضع تُضاء بنفسجيًّا حين تكون اللوحة مفعّلة. إن مُرّر [onOpen] صارت اللوحة
 * كلّها زرًّا يفتح الخيارات.
 */
@Composable
internal fun StatusCard(data: StageData, modifier: Modifier, onOpen: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(RadiusCard)
    val interaction = remember { MutableInteractionSource() }
    val tone = batteryTone(data.batteryPercent, data.charging)
    Row(
        modifier = modifier
            .lobbyPress(interaction, 0.97f)
            .stageGlass(shape)
            .then(
                if (onOpen != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = onOpen
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = PadWide),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GapS)
    ) {
        // البرق داخل البطارية يظهر عند الشحن فقط — لا يُرسم ادّعاءً.
        Box(modifier = Modifier.size(BatteryIcon), contentAlignment = Alignment.Center) {
            StageBatteryTile(
                percent = data.batteryPercent,
                tone = tone,
                modifier = Modifier.size(BatteryIcon)
            )
            if (data.charging) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = GameSpacePalette.GlassDeep,
                    modifier = Modifier.size(ModeGlyph)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.batteryPercent?.let { stringResource(R.string.gs_percent_value, it) }
                    ?: MAX_VALUE_UNAVAILABLE,
                color = GameSpacePalette.Ink,
                fontSize = 27.sp,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.gs_battery),
                color = GameSpacePalette.Ink.copy(alpha = 0.75f),
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        StageDivider(BatteryIcon)
        Box(
            modifier = Modifier
                .size(ModeIcon)
                .clip(CircleShape)
                .background(if (data.panelEnabled) ModeOnFill else ModeOffFill)
                .border(
                    StrokeFine,
                    GameSpacePalette.VioletSoft.copy(alpha = if (data.panelEnabled) 0.55f else 0.14f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = if (data.panelEnabled) Color.White else GameSpacePalette.Muted,
                modifier = Modifier.size(ModeGlyph)
            )
        }
        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = stringResource(R.string.gs_perf_mode),
                color = GameSpacePalette.VioletSoft,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (data.panelEnabled) stringResource(R.string.gs_perf_mode_on) else MAX_VALUE_UNAVAILABLE,
                color = if (data.panelEnabled) GameSpacePalette.Ink else GameSpacePalette.Muted,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = GameSpacePalette.Ink,
            modifier = Modifier.size(ChevronIcon)
        )
    }
}

/** بطارية عموديّة بمستواها الحقيقيّ: إطار بحلمة وتعبئة تصعد من الأسفل بعدّ تصاعديّ؛ بلا قراءة = إطار فارغ رماديّ. */
@Composable
private fun StageBatteryTile(percent: Int?, tone: Color, modifier: Modifier = Modifier) {
    val level = rememberCountUp(((percent ?: 0) / 100f).coerceIn(0f, 1f))
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val bodyW = w * 0.56f
        val bodyH = h * 0.88f
        val left = (w - bodyW) / 2f
        val top = h - bodyH
        val stroke = w * 0.075f
        if (percent != null) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tone.copy(alpha = 0.20f), Color.Transparent),
                    center = center,
                    radius = w * 0.78f
                ),
                radius = w * 0.78f
            )
        }
        drawRoundRect(
            color = tone,
            topLeft = Offset(w / 2f - bodyW * 0.19f, h * 0.02f),
            size = Size(bodyW * 0.38f, top + stroke),
            cornerRadius = CornerRadius(stroke)
        )
        drawRoundRect(
            color = tone,
            topLeft = Offset(left + stroke / 2f, top + stroke / 2f),
            size = Size(bodyW - stroke, bodyH - stroke),
            cornerRadius = CornerRadius(w * 0.12f),
            style = Stroke(width = stroke)
        )
        if (percent != null) {
            val inner = stroke * 2f
            val room = bodyH - inner * 2f
            val fillH = room * level.value
            if (fillH > 1f) {
                val fillTop = top + inner + room - fillH
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(tone, tone.copy(alpha = 0.62f)),
                        startY = fillTop,
                        endY = top + inner + room
                    ),
                    topLeft = Offset(left + inner, fillTop),
                    size = Size(bodyW - inner * 2f, fillH),
                    cornerRadius = CornerRadius(w * 0.05f)
                )
            }
        }
    }
}

/**
 * لوحة الذاكرة: النسبة المستعملة (رقم كبير) وشريط شرائح يمتلئ بعدّ تصاعديّ، وتحتهما المستعمل/الكلّي بالجيجا.
 * مكان لوحة «الإطارات اللحظية» التي لا تُقاس إلا داخل اللعبة فتبقى «—» ميّتة في اللوبي؛ الذاكرة قراءة حقيقيّة
 * من النظام حاضرة دائمًا، ولونها يسخن مع الامتلاء (نعناعيّ ثم برتقاليّ ثم ورديّ). بلا قراءة: «—» وشريط فارغ.
 */
@Composable
internal fun MemoryCard(ram: StageRam?, modifier: Modifier) {
    val shape = RoundedCornerShape(RadiusCard)
    val level = rememberCountUp(((ram?.percent ?: 0) / 100f).coerceIn(0f, 1f))
    val tone = when {
        ram == null -> GameSpacePalette.Muted
        ram.percent >= 88 -> GameSpacePalette.Magenta
        ram.percent >= 72 -> GameSpacePalette.Orange
        else -> GameSpacePalette.Mint
    }
    Row(
        modifier = modifier
            .stageGlass(shape)
            .padding(horizontal = PadCard),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GapM)
    ) {
        Box(
            modifier = Modifier
                .size(ModeIcon)
                .clip(CircleShape)
                .background(ModeOffFill)
                .border(StrokeFine, tone.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Memory,
                contentDescription = null,
                tint = tone,
                modifier = Modifier.size(ModeGlyph)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.gs_memory),
                    color = GameSpacePalette.Ink.copy(alpha = 0.85f),
                    fontSize = 19.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = ram?.let { stringResource(R.string.gs_percent_value, it.percent) } ?: MAX_VALUE_UNAVAILABLE,
                    color = if (ram == null) GameSpacePalette.Muted else GameSpacePalette.Ink,
                    fontSize = 27.sp,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(GapXS))
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(MemoryBarHeight)
            ) {
                val gap = size.width * 0.012f
                val cell = (size.width - gap * (MemoryCells - 1)) / MemoryCells
                val filled = level.value * MemoryCells
                for (i in 0 until MemoryCells) {
                    val x = i * (cell + gap)
                    val corner = CornerRadius(cell * 0.32f)
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.09f),
                        topLeft = Offset(x, 0f),
                        size = Size(cell, size.height),
                        cornerRadius = corner
                    )
                    val part = (filled - i).coerceIn(0f, 1f)
                    if (part > 0.02f) {
                        drawRoundRect(
                            color = tone.copy(alpha = 0.35f + 0.65f * part),
                            topLeft = Offset(x, 0f),
                            size = Size(cell, size.height),
                            cornerRadius = corner
                        )
                    }
                }
            }
            if (ram != null) {
                Spacer(Modifier.height(GapXS))
                Text(
                    text = stringResource(R.string.gs_memory_detail, ram.usedGb, ram.totalGb),
                    color = GameSpacePalette.Ink.copy(alpha = 0.70f),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** لوحة الإطارات اللحظيّة: منحنى آخر العيّنات يسارًا (١٥٤×٤٨)، والرقم الكبير، ثم أعمدة تتراقص حين يكون القياس حيًّا. */
@Composable
internal fun FpsCard(fps: StageFps, modifier: Modifier) {
    val shape = RoundedCornerShape(RadiusCard)
    val shown = fps.value != null && fps.live
    Row(
        modifier = modifier
            .stageGlass(shape)
            .padding(horizontal = PadCard),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GapL)
    ) {
        StageSparkline(
            values = fps.history,
            live = shown,
            modifier = Modifier.size(width = 124.dp, height = 48.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GapS)
            ) {
                Text(
                    text = stringResource(R.string.gs_fps_title),
                    color = GameSpacePalette.Ink.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    maxLines = 1
                )
                if (shown) PulseDot(GameSpacePalette.Mint, Modifier.size(PulseSize))
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(GapS)
            ) {
                Text(
                    text = fps.value?.toString() ?: MAX_VALUE_UNAVAILABLE,
                    color = if (shown) GameSpacePalette.Ink else GameSpacePalette.Muted,
                    fontSize = 37.sp,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.gs_fps_unit),
                    color = GameSpacePalette.Ink.copy(alpha = 0.78f),
                    fontSize = 17.sp,
                    maxLines = 1
                )
            }
        }
        StageBars(active = shown, modifier = Modifier.size(width = 28.dp, height = 26.dp))
    }
}

/**
 * منحنى بنفسجيّ ناعم مملوء من عيّنات حقيقيّة بتوهّج، ونقطة نابضة عند آخر عيّنة حين يكون القياس حيًّا.
 * أقلّ من عيّنتين = خطّ أساس متقطّع لا منحنى مخترع.
 */
@Composable
private fun StageSparkline(values: List<Float>, live: Boolean, modifier: Modifier = Modifier) {
    val clock = LocalStageClock.current
    Canvas(modifier) {
        if (values.size < 2) {
            drawLine(
                color = GameSpacePalette.VioletSoft.copy(alpha = 0.35f),
                start = Offset(0f, size.height * 0.75f),
                end = Offset(size.width, size.height * 0.75f),
                strokeWidth = size.height * 0.04f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(size.height * 0.2f, size.height * 0.2f))
            )
            return@Canvas
        }
        val peak = (values.maxOrNull() ?: 1f).coerceAtLeast(1f) * 1.15f
        val step = size.width / (values.size - 1)
        val line = Path()
        val fill = Path()
        var previous = Offset.Zero
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = size.height - (v / peak).coerceIn(0f, 1f) * size.height * 0.90f - size.height * 0.05f
            if (i == 0) {
                line.moveTo(x, y)
                fill.moveTo(x, size.height)
                fill.lineTo(x, y)
            } else {
                val mid = (previous.x + x) / 2f
                line.cubicTo(mid, previous.y, mid, y, x, y)
                fill.cubicTo(mid, previous.y, mid, y, x, y)
            }
            previous = Offset(x, y)
        }
        fill.lineTo(size.width, size.height)
        fill.close()
        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                listOf(GameSpacePalette.VioletSoft.copy(alpha = 0.38f), Color.Transparent)
            )
        )
        drawPath(
            path = line,
            color = GameSpacePalette.VioletSoft.copy(alpha = 0.30f),
            style = Stroke(width = size.height * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = line,
            brush = Brush.horizontalGradient(
                listOf(GameSpacePalette.Violet, GameSpacePalette.VioletSoft, GameSpacePalette.Lavender)
            ),
            style = Stroke(width = size.height * 0.055f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (live) {
            val pulse = stageWave(clock.value, 30)
            drawCircle(
                color = GameSpacePalette.Lavender.copy(alpha = 0.20f + 0.20f * pulse),
                radius = size.height * (0.12f + 0.08f * pulse),
                center = previous
            )
            drawCircle(color = Color.White, radius = size.height * 0.055f, center = previous)
        }
    }
}

private val BarFractions = floatArrayOf(0.45f, 0.75f, 1f)

/** ثلاثة أعمدة صغيرة — رمز اللوحة كما في التصميم؛ تتراقص حين يكون القياس حيًّا وتسكن بدونه. زينة صريحة لا قراءة. */
@Composable
private fun StageBars(active: Boolean, modifier: Modifier = Modifier) {
    val clock = LocalStageClock.current
    Canvas(modifier) {
        val bar = size.width * 0.24f
        val gap = (size.width - bar * 3f) / 2f
        val t = if (active) clock.value else 0f
        for (i in BarFractions.indices) {
            val fraction = if (active) {
                BarFractions[i] * (0.76f + 0.24f * stageWave(t, 10 + i, i * 0.31f))
            } else {
                BarFractions[i]
            }
            drawRoundRect(
                color = GameSpacePalette.VioletSoft,
                topLeft = Offset(i * (bar + gap), size.height * (1f - fraction)),
                size = Size(bar, size.height * fraction),
                cornerRadius = CornerRadius(bar / 3f)
            )
        }
    }
}

/**
 * قاعدة الحالة (١٩٥،٨٥٠، ١٤٥١×٦٢): برق متوهّج، فاصل، حكم آخر جلسة (أو الجاهزية)، وشريط نسبة بشرائح مائلة
 * تمتلئ بعدّ تصاعديّ وتعبرها لمعة. [detail] سطر ثانٍ **اختياريّ**: يظهر حين يكون هناك شرط ناقص يُقال،
 * ويُسقط حين تكتمل التهيئة. النسبة هي الجاهزية الحقيقيّة لا رقمًا مرسومًا.
 * [compact]: في التخطيط المضغوط (هاتف عريض) تسكن أسفل عمود اللوحات اليمنى بسطرين: البرق والحكم والنسبة فوق،
 * والشريط بعرض اللوح تحتها.
 */
@Composable
internal fun StageBase(
    title: String,
    detail: String,
    percent: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val target = percent.coerceIn(0, 100)
    val level = rememberCountUp(target / 100f)
    val interaction = remember { MutableInteractionSource() }
    if (compact) {
        val card = RoundedCornerShape(RadiusCard)
        Column(
            modifier = modifier
                .lobbyPress(interaction, 0.985f)
                .clip(card)
                .background(GlassFill)
                .drawBehind {
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(GameSpacePalette.Violet.copy(alpha = 0.14f), Color.Transparent)
                        )
                    )
                }
                .border(StrokeFine, GlassBorder, card)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onOpen
                )
                .padding(start = PadCard, end = PadCard),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GapM)
            ) {
                BaseBolt()
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = GameSpacePalette.Ink,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (detail.isNotBlank()) {
                        Text(
                            text = detail,
                            color = GameSpacePalette.VioletSoft,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.gs_percent_value, target),
                    color = GameSpacePalette.Ink,
                    fontSize = 26.sp,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(GapM))
            BaseBar(target = target, level = level, modifier = Modifier.fillMaxWidth().height(BarHeight))
        }
        return
    }
    Row(
        modifier = modifier
            .lobbyPress(interaction, 0.985f)
            .clip(BaseShape)
            .background(GlassFill)
            .drawBehind {
                drawRect(
                    Brush.horizontalGradient(
                        listOf(GameSpacePalette.Violet.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
            }
            .border(StrokeFine, GlassBorder, BaseShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onOpen
            )
            .padding(start = 47.dp, end = 132.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BaseBolt()
        Spacer(Modifier.width(33.dp))
        StageDivider(BaseIcon)
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = GameSpacePalette.Ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    color = GameSpacePalette.VioletSoft,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        BaseBar(target = target, level = level, modifier = Modifier.size(width = BarWidth, height = BarHeight))
        Spacer(Modifier.width(21.dp))
        Text(
            text = stringResource(R.string.gs_percent_value, target),
            color = GameSpacePalette.Ink,
            fontSize = 26.sp,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/** البرق المتوهّج أمام الحكم، مشترك بين التخطيطين. */
@Composable
private fun BaseBolt() {
    Icon(
        imageVector = Icons.Rounded.Bolt,
        contentDescription = null,
        tint = GameSpacePalette.VioletSoft,
        modifier = Modifier
            .size(BaseIcon)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GameSpacePalette.Violet.copy(alpha = 0.40f), Color.Transparent),
                        center = center,
                        radius = size.minDimension * 1.1f
                    ),
                    radius = size.minDimension * 1.1f
                )
            }
    )
}

/** شريط الجاهزية بشرائحه المائلة: يمتلئ بعدّ تصاعديّ وتعبره لمعة؛ عرضه وارتفاعه من [modifier]. */
@Composable
private fun BaseBar(target: Int, level: State<Float>, modifier: Modifier) {
    val clock = LocalStageClock.current
    Box(
        modifier = modifier
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(target / 100f, 0f..1f)
            }
            .drawWithCache {
                val gap = size.width * 0.006f
                val skew = size.height * 0.55f
                val segment = (size.width - skew - gap * (BarCells - 1)) / BarCells
                val cells = Array(BarCells) { i ->
                    val x = i * (segment + gap)
                    Path().apply {
                        moveTo(x + skew, 0f)
                        lineTo(x + skew + segment, 0f)
                        lineTo(x + segment, size.height)
                        lineTo(x, size.height)
                        close()
                    }
                }
                val fill = Brush.horizontalGradient(
                    colors = listOf(LuxGoldDeep, LuxGold, LuxIvory),
                    startX = 0f,
                    endX = size.width
                )
                onDrawBehind {
                    val filledF = BarCells * level.value
                    val whole = filledF.toInt()
                    val frac = filledF - whole
                    val glint = ((clock.value * BaseGlintCycles) % 1f) * (whole + 8) - 4f
                    for (i in 0 until BarCells) {
                        if (i < whole) {
                            drawPath(path = cells[i], brush = fill)
                            val w = 1f - abs(i - glint) / 3f
                            if (w > 0f) {
                                drawPath(path = cells[i], color = Color.White.copy(alpha = 0.30f * w))
                            }
                        } else {
                            drawPath(path = cells[i], color = Color.White.copy(alpha = 0.08f))
                            if (i == whole && frac > 0.04f) {
                                drawPath(path = cells[i], brush = fill, alpha = frac)
                            }
                        }
                    }
                }
            }
    )
}
