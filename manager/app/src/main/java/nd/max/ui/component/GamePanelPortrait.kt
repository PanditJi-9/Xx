/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.roundToInt
import nd.max.core.gamespace.CockpitModel
import nd.max.core.gamespace.CubeLayoutMath
import nd.max.core.gamespace.PortraitLayout
import nd.max.core.gamespace.QuickApp
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius

/*
 * لوحة اللعبة في **الطولي**: بطاقة عائمة في أسفل الشاشة بدل تخطيط العرضي. كانت اللوحة في الطولي تُرسم بأعمدة
 * العرضي مضغوطة على ٣٦٠dp فتتقاطع الأرقام مع صفحة اللعبة وتُبتر البلاطات (لقطة Chrome)؛ أما هنا
 * فكل بُعد من [PortraitLayout] وكل قراءة حيّة، والبطاقة تتّسع للمحتوى وتُمرَّر إن زاد.
 * الترتيب من الأهمّ إلى الأقلّ: الرأس والتطبيقات، فمعالج/رسّام بتردديهما الحقيقيّين وشريطيهما،
 * فالسطوع والصوت، فبروفايلات الحرارة، فبلاطات التحكّم. الشكل من لغة تصميمنا (`CubeParts`) لا تقليد.
 */

/** ما يلزم لبطاقة معالج أو رسّام في الطولي: النصوص جاهزة، والمستوى من التردد الحيّ فعلًا (`null` ⇒ فارغ). */
internal class PortraitGauge(
    val label: String,
    val value: String,
    val unit: String,
    val level: Float?,
    val loadText: String?,
    val pill: String,
    val onPill: () -> Unit,
    val onPillBounds: (Rect) -> Unit,
    val description: String,
)

/** عتمة خفيفة (٢٥٪): تفصل البطاقة عن اللعبة دون أن تُخفي ما يجري فيها. */
private val PortraitScrim = Color(0x40000000)

@Composable
internal fun BoxScope.PortraitPanel(
    layout: PortraitLayout,
    enter: Animatable<Float, AnimationVector1D>,
    tint: Color,
    title: String,
    statusLine: String,
    homeLabel: String,
    onHome: () -> Unit,
    collapseLabel: String,
    onCollapse: () -> Unit,
    apps: List<QuickApp>,
    launching: String?,
    onApp: (QuickApp) -> Unit,
    appsEditLabel: String,
    appsOpen: Boolean,
    onAppsEdit: () -> Unit,
    onAppsBounds: (Rect) -> Unit,
    cpu: PortraitGauge,
    gpu: PortraitGauge,
    brightness: Float,
    brightnessAllowed: Boolean,
    brightnessLabel: String,
    onBrightness: (Float) -> Unit,
    volume: Float,
    volumeLabel: String,
    onVolume: (Float) -> Unit,
    thermalTitle: String,
    thermalOptions: List<CubeOption>,
    thermalSelected: String,
    thermalBusy: String?,
    onPickThermal: (String) -> Unit,
    tiles: List<CubeTileSpec>,
    note: String,
    noteIsMessage: Boolean,
    extra: @Composable () -> Unit,
) {
    // عتمة خلف البطاقة: تفصل اللوحة عن اللعبة، ولمسة فيها تطوي اللوحة (المعالج في الأعلى).
    Box(Modifier.matchParentSize().graphicsLayer { alpha = enter.value.coerceIn(0f, 1f) }.background(PortraitScrim))
    val shape = RoundedCornerShape(maxRadius(MaxRadius.sheet))
    // سحب المقبض نحو الأسفل يُنزل البطاقة مع الإصبع ثم يطويها عند تجاوز العتبة، أو يرجعها بنعومة.
    val collapse by rememberUpdatedState(onCollapse)
    val dismissAtPx = with(LocalDensity.current) { SHEET_DISMISS_DP.dp.toPx() }
    var drag by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val dragShown by animateFloatAsState(
        drag, if (dragging) snap<Float>() else spring<Float>(dampingRatio = 0.8f, stiffness = 500f), label = "sheetDrag"
    )
    // البطاقة عائمة: ترتفع فوق شريط التنقّل بهامش، فلا تلتصق بالحافّة السفلى، وأطرافها تبقى للعبة.
    Column(
        Modifier.align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = MaxSpace.md)
            .width(layout.cardW.dp)
            .heightIn(max = layout.cardMaxH.dp)
            .graphicsLayer {
                alpha = enter.value.coerceIn(0f, 1f)
                translationY = (1f - enter.value.coerceIn(0f, 1f)) * 56f * density + dragShown
            }
            .clip(shape)
            .background(CubeMenuBg)
            .border(MaxSize.hairlineBorder, tint.copy(alpha = 0.35f), shape)
            .swallowTaps()
    ) {
        Box(
            Modifier.fillMaxWidth().height(MaxSize.iconGlyph + MaxSpace.md)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { dragging = true; drag = 0f },
                        onVerticalDrag = { change, dy ->
                            change.consume()
                            drag = (drag + dy).coerceAtLeast(0f)
                        },
                        onDragCancel = { dragging = false; drag = 0f },
                        onDragEnd = {
                            dragging = false
                            if (drag > dismissAtPx) collapse() else drag = 0f
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.width(36.dp).height(MaxSpace.xs).clip(CircleShape).background(Color.White.copy(alpha = 0.28f)))
        }
        FadeScroll(
            Modifier.weight(1f, fill = false).padding(horizontal = layout.pad.dp, vertical = MaxSpace.md),
            state = rememberScrollState(), spacing = MaxSpace.md
        ) {
            PortraitHeader(layout, title, statusLine, homeLabel, onHome, collapseLabel, onCollapse)
            PortraitApps(layout, tint, apps, launching, onApp, appsEditLabel, appsOpen, onAppsEdit, onAppsBounds)
            Row(horizontalArrangement = Arrangement.spacedBy(layout.gap.dp)) {
                PortraitGaugeCard(cpu, tint, layout, Modifier.weight(1f))
                PortraitGaugeCard(gpu, tint, layout, Modifier.weight(1f))
            }
            PortraitSlider(Icons.Rounded.WbSunny, brightnessLabel, brightness, brightnessAllowed, tint, layout.sliderH.dp, onBrightness)
            PortraitSlider(Icons.AutoMirrored.Rounded.VolumeUp, volumeLabel, volume, true, tint, layout.sliderH.dp, onVolume)
            SectionCaption(Icons.Rounded.Thermostat, thermalTitle)
            thermalOptions.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(layout.gap.dp)) {
                    pair.forEach { option ->
                        OptionRow(
                            option = option, selected = option.id == thermalSelected, busy = option.id == thermalBusy,
                            tint = tint, height = layout.optionH.dp, card = true,
                            onClick = { onPickThermal(option.id) }, modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            tiles.chunked(layout.tileCols).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(layout.gap.dp)) {
                    row.forEach { CubeTileView(it, layout.tile.dp, layout.tileColW.dp, tint, 11f) }
                }
            }
            extra()
            Box(
                Modifier.fillMaxWidth().height(MaxSpace.xxl).clip(CircleShape).background(CubeGlass),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = note, label = "portraitStatus") { text ->
                    // الرسالة الفعلية بلون النصّ، والتلميح الدائم بلون باهت: لا تفوت نتيجةُ فتح تطبيق العين.
                    FitText(text, if (noteIsMessage) CubeInk else CubeDim, maxSp = 11f, minSp = 8f, modifier = Modifier.padding(horizontal = MaxSpace.md))
                }
            }
        }
    }
}

/** مسافة سحب المقبض (dp) التي بعدها تنطوي البطاقة. */
private const val SHEET_DISMISS_DP = 96

@Composable
private fun PortraitHeader(
    layout: PortraitLayout, title: String, statusLine: String,
    homeLabel: String, onHome: () -> Unit, collapseLabel: String, onCollapse: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
        CubeCircle(layout.btn.dp, homeLabel, onHome) {
            Icon(Icons.Rounded.Home, null, tint = CubeInk, modifier = Modifier.size(MaxSpace.xxl))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)) {
            FitText(
                title, CubeInk, maxSp = 16f, minSp = 11f, weight = FontWeight.Bold,
                align = TextAlign.Left, modifier = Modifier.fillMaxWidth()
            )
            FitText(statusLine, CubeDim, maxSp = 12f, minSp = 9f, align = TextAlign.Left, modifier = Modifier.fillMaxWidth())
        }
        CubeCircle(layout.btn.dp, collapseLabel, onCollapse) { CubeLogo(Modifier.size(MaxSpace.xxl)) }
    }
}

/** شريط التطبيقات السريعة: دوائر اللوحة نفسها، ومؤشّر انتظار على الدائرة قيد الفتح، وزرّ التحرير في آخره. */
@Composable
private fun PortraitApps(
    layout: PortraitLayout, tint: Color, apps: List<QuickApp>, launching: String?, onApp: (QuickApp) -> Unit,
    editLabel: String, editOpen: Boolean, onEdit: () -> Unit, onBounds: (Rect) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        apps.forEach { app ->
            Box(contentAlignment = Alignment.Center) {
                CubeCircle(layout.btn.dp, app.label, { onApp(app) }) {
                    Image(
                        bitmap = app.icon.asImageBitmap(), contentDescription = null,
                        modifier = Modifier.size((layout.btn * 0.7f).dp).clip(CircleShape)
                    )
                }
                if (launching == app.pkg) CubeSpinner(CubeInk, (layout.btn * 0.5f).dp)
            }
        }
        Box(Modifier.onGloballyPositioned { onBounds(it.boundsInRoot()) }) {
            CubeCircle(layout.btn.dp, editLabel, onEdit, active = editOpen, tint = tint) {
                Icon(Icons.Rounded.Add, null, tint = CubeInk, modifier = Modifier.size(MaxSpace.xxl))
            }
        }
    }
}

/**
 * بطاقة معالج/رسّام: الوسم وسقفه، ثم التردد الحيّ ووحدته، ثم شريط التردد الأفقيّ (المقاطع نفسها
 * كالشريط العموديّ في العرضي)، ثم الحمل، ثم حبّة الاختيار. كلها قراءات حقيقية، و`null` يُرى «—».
 */
@Composable
private fun PortraitGaugeCard(g: PortraitGauge, tint: Color, layout: PortraitLayout, modifier: Modifier) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.control))
    Column(
        modifier.clip(shape).background(CubeTile).padding(MaxSpace.md)
            .semantics { contentDescription = g.description },
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs + 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
            Text(g.label, color = tint, fontSize = 14.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, maxLines = 1)
            Spacer(Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            FitText(
                g.value, CubeInk, maxSp = layout.numberSp, minSp = 16f, weight = FontWeight.Black,
                style = FontStyle.Italic, align = TextAlign.Left, modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(MaxSpace.xs))
            Text(g.unit, color = CubeDim, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(bottom = MaxSpace.xs))
        }
        FrequencyBar(g.level, tint, Modifier.fillMaxWidth().height(CubeLayoutMath.FREQ_BAR_H.dp + 2.dp))
        FitText(g.loadText ?: " ", CubeDim, maxSp = 11f, minSp = 8f, align = TextAlign.Left, modifier = Modifier.fillMaxWidth())
        CubePill(
            g.pill, tint, (layout.gaugeW - 2f * 12f).dp, 40.dp, false, g.onPillBounds, g.onPill,
            Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/**
 * منزلق أفقيّ للسطوع أو الصوت: السحب محلّي (لا إعادة تركيب للّوحة) والكتابة عند تبدّل المقطع فقط،
 * كالمقياس العموديّ في العرضي. [enabled] = `false` يُخفّت فقط؛ اللمس يبقى لأن المالك يعالج غياب الإذن.
 */
@Composable
private fun PortraitSlider(
    icon: ImageVector, description: String, value: Float, enabled: Boolean, tint: Color, height: Dp,
    onChange: (Float) -> Unit
) {
    val change by rememberUpdatedState(onChange)
    var live by remember { mutableFloatStateOf(value) }
    var grabbed by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (!grabbed) live = value }
    val shown by animateFloatAsState(live, spring(dampingRatio = 1f, stiffness = 900f), label = "portraitSlider")
    val shape = RoundedCornerShape(maxRadius(MaxRadius.control))
    Row(
        Modifier.fillMaxWidth().height(height).clip(shape).background(CubeTile)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(height), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = CubeDim, modifier = Modifier.size(MaxSize.iconGlyph))
        }
        Canvas(
            Modifier.weight(1f).fillMaxHeight().pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    grabbed = true
                    var segment = -1
                    fun push(x: Float) {
                        val v = (x / size.width).coerceIn(0f, 1f)
                        live = v
                        val seg = CockpitModel.segmentOf(v, METER_BARS)
                        if (seg != segment) {
                            segment = seg
                            change(v)
                        }
                    }
                    push(down.position.x)
                    down.consume()
                    while (true) {
                        val move = awaitPointerEvent().changes.firstOrNull() ?: break
                        if (!move.pressed) break
                        push(move.position.x)
                        move.consume()
                    }
                    grabbed = false
                }
            }
        ) {
            val trackH = 10.dp.toPx()
            val top = (size.height - trackH) / 2f
            val radius = CornerRadius(trackH / 2f)
            drawRoundRect(Color.White.copy(alpha = 0.12f), Offset(0f, top), Size(size.width, trackH), radius)
            val fill = size.width * shown.coerceIn(0f, 1f)
            if (fill > 0f) {
                drawRoundRect(
                    Brush.horizontalGradient(
                        listOf(lerp(tint, Color.Black, 0.35f), lerp(tint, Color.White, 0.30f)),
                        startX = 0f, endX = max(fill, 1f)
                    ),
                    Offset(0f, top), Size(fill, trackH), radius
                )
            }
            val knob = 8.dp.toPx()
            drawCircle(Color.White, knob, Offset(fill.coerceIn(knob, max(knob, size.width - knob)), size.height / 2f))
        }
        Box(Modifier.width(MaxSize.minTouchTarget), contentAlignment = Alignment.Center) {
            Text("${(live * 100f).roundToInt()}%", color = CubeInk, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
