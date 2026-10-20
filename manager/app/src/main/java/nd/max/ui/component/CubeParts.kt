/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.roundToInt
import nd.max.core.gamespace.CockpitModel
import nd.max.core.gamespace.CubeLayout
import nd.max.core.gamespace.CubeLayoutMath
import nd.max.core.gamespace.PanelClockMath
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import nd.max.ui.design.MaxAlpha
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius

/*
 * أجزاء لوحة «المكعّب» القابلة لإعادة الاستعمال. القاعدة: كل جزء يأخذ مقاساته من [CubeLayout]
 * أو من معاملاته، ولا يضع حدًّا أدنى ثابتًا يغلب مقياس الشاشة؛ وكل نصّ يمرّ بـ[FitText] فيصغر
 * حتى يتّسع بدل أن يُبتر («الوضع التلق…» كانت العلّة المرئية الأولى).
 */

internal val CubeInk = Color(0xFFF4F5F7)
internal val CubeDim = Color(0xFFA9AEB6)
internal val CubeTile = Color(0xCC2A2B2F)
internal val CubeGlass = Color(0x99111318)
internal val CubeMenuBg = Color(0xFA17181C)
internal val ModeRed = Color(0xFFE8475A)
internal val ModeAmber = Color(0xFFFFC53D)
internal val ModeGreen = Color(0xFF3DDC97)
internal val ModeAuto = Color(0xFF4DD0FF)

/** حبّة كبسولية ناعمة بدل السداسية القديمة — أنظف وأحدث كما في المراجع. */
internal val PillShape = RoundedCornerShape(50)

/** حبّة سداسية الطرفين («‹ نص ›») كما في المرجع: الطرفان يُشطبان بزاوية بدل نصف دائرة. */
internal val HexPillShape = GenericShape { size, _ ->
    val c = size.height / 2f
    moveTo(c, 0f); lineTo(size.width - c, 0f); lineTo(size.width, c)
    lineTo(size.width - c, size.height); lineTo(c, size.height); lineTo(0f, c); close()
}

/** خيار في قائمة منسدلة أو بطاقة: المعرّف، والاسم، وسطر فرعي (تردد/حالة)، وأيقونته ولونها. */
@Immutable
internal class CubeOption(
    val id: String,
    val label: String,
    val sub: String,
    val icon: ImageVector,
    val color: Color,
    val enabled: Boolean = true,
    val subColor: Color = CubeDim,
)

/** وصف بلاطة في الشبكة: أيقونة أو نصّ قصير، ووسم، وحالة، وإجراء، وتقارير موضعها إن لزم. */
internal class CubeTileSpec(
    val icon: ImageVector?,
    val text: String?,
    val label: String,
    val active: Boolean,
    val iconTint: Color? = null,
    val onBounds: ((Rect) -> Unit)? = null,
    val onClick: () -> Unit,
)

internal fun Modifier.swallowTaps(): Modifier = pointerInput(Unit) { detectTapGestures { } }

/** نصّ سطر واحد يصغّر خطّه بخطوات حتى يتّسع لعرضه؛ عند الحدّ الأدنى يُقتطع بنقاط. */
@Composable
internal fun FitText(
    text: String,
    color: Color,
    maxSp: Float,
    minSp: Float,
    modifier: Modifier = Modifier,
    weight: FontWeight? = null,
    style: FontStyle? = null,
    align: TextAlign = TextAlign.Center,
) {
    var current by remember(text, maxSp) { mutableFloatStateOf(maxSp) }
    Text(
        text = text,
        color = color,
        fontSize = current.sp,
        fontWeight = weight,
        fontStyle = style,
        textAlign = align,
        maxLines = 1,
        softWrap = false,
        overflow = if (current <= minSp) TextOverflow.Ellipsis else TextOverflow.Clip,
        onTextLayout = { layout ->
            if (layout.hasVisualOverflow && current > minSp) current = (current - 0.75f).coerceAtLeast(minSp)
        },
        modifier = modifier,
    )
}

/** مؤشّر انتظار صغير: قوس يدور. يُرى فقط حين يكون هناك تنفيذ جارٍ فعلًا. */
@Composable
internal fun CubeSpinner(color: Color, diameter: Dp) {
    val angle by rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(850, easing = LinearEasing)), label = "spinnerAngle"
    )
    Canvas(Modifier.size(diameter)) {
        val stroke = 2.dp.toPx()
        drawArc(
            color = color, startAngle = angle, sweepAngle = 270f, useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(this.size.width - stroke, this.size.height - stroke),
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
internal fun CubeCircle(
    diameter: Dp, description: String, onClick: () -> Unit,
    active: Boolean = false, tint: Color = Color.Transparent, content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier.size(diameter).clip(CircleShape)
            .background(if (active) lerp(tint, Color.Black, 0.5f) else CubeTile)
            .border(MaxSize.hairlineBorder, if (active) tint.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.10f), CircleShape)
            .clickable(role = Role.Button) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) { content() }
}

/** شعار المكعّب: سداسيّ بثلاثة أضلاع داخلية (منظور متساوي القياس). */
@Composable
internal fun CubeLogo(modifier: Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        fun p(deg: Float) = Offset(
            c.x + r * kotlin.math.cos(Math.toRadians(deg.toDouble())).toFloat(),
            c.y + r * kotlin.math.sin(Math.toRadians(deg.toDouble())).toFloat()
        )
        val hex = Path().apply {
            moveTo(p(-90f).x, p(-90f).y)
            listOf(-30f, 30f, 90f, 150f, 210f).forEach { lineTo(p(it).x, p(it).y) }
            close()
        }
        drawPath(hex, CubeInk, style = Stroke(2.dp.toPx()))
        listOf(-30f, 90f, 210f).forEach { drawLine(CubeInk, c, p(it), 2.dp.toPx()) }
    }
}

@Composable
internal fun StatusBit(icon: ImageVector?, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Icon(icon, null, tint = CubeDim, modifier = Modifier.size(13.dp))
        Text(text, color = CubeInk, fontSize = 12.sp, maxLines = 1)
    }
}

/** قرص متوهّج مبسّط: حلقة واحدة مع قوس حمل ونبضة خفيفة — أقل ضجيجًا بصريًّا. */
@Composable
internal fun CubeOrb(tint: Color, loadPct: Int? = null, modifier: Modifier = Modifier) {
    val spin by rememberInfiniteTransition(label = "orb").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "orbSpin"
    )
    val animatedLoad by animateFloatAsState(
        targetValue = (loadPct ?: 0) / 100f,
        animationSpec = tween(450),
        label = "orbLoad"
    )
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        // لبّ مضيء يخبو نحو الحافة: هو ما يجعل القرص يبدو متوهّجًا لا مرسومًا بخطّ.
        drawOval(
            Brush.radialGradient(
                0f to tint.copy(alpha = 0.55f), 0.55f to tint.copy(alpha = 0.2f), 1f to Color.Transparent,
                center = Offset(cx, cy), radius = size.width / 2f
            ),
            topLeft = Offset.Zero, size = size
        )
        // حلقتان متداخلتان (خارجية وداخلية) تعطيان القرص عمقًا ومنظورًا.
        drawOval(tint.copy(alpha = 0.55f), Offset.Zero, size, style = Stroke(1.5f.dp.toPx()))
        val inX = size.width * 0.17f
        val inY = size.height * 0.17f
        val inTop = Offset(inX, inY)
        val inSize = Size(size.width - inX * 2f, size.height - inY * 2f)
        drawOval(tint.copy(alpha = 0.35f), inTop, inSize, style = Stroke(1.dp.toPx()))
        // قوسان متعاكسان يدوران: الخارجي ساطع، والداخلي أقصر وأخفّ.
        drawArc(tint, spin, 110f, false, Offset.Zero, size, style = Stroke(2.5f.dp.toPx(), cap = StrokeCap.Round))
        drawArc(tint.copy(alpha = 0.7f), -spin * 1.4f, 70f, false, inTop, inSize, style = Stroke(1.5f.dp.toPx(), cap = StrokeCap.Round))
        // قوس التحميل (إن وُجد)
        if (loadPct != null) {
            val strokeW = 3.dp.toPx()
            val pad = strokeW / 2f + 1.dp.toPx()
            val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
            val arcTopLeft = Offset(pad, pad)
            drawArc(
                color = tint.copy(alpha = 0.15f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = arcTopLeft, size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(tint.copy(alpha = 0.5f), tint)),
                startAngle = -90f,
                sweepAngle = (animatedLoad * 360f).coerceIn(0f, 360f),
                useCenter = false,
                topLeft = arcTopLeft, size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * الحبّة السداسية تحت العدّاد: تُظهر الاختيار القائم وتفتح قائمته. تُبلّغ بحدودها كي تُثبَّت
 * القائمة تحتها بدقّة، وتُظهر مؤشّر انتظار حين يكون هناك تنفيذ جارٍ.
 */
@Composable
internal fun CubePill(
    text: String, tint: Color, width: Dp, height: Dp, busy: Boolean,
    onBounds: (Rect) -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier.width(width).height(height)
            .onGloballyPositioned { onBounds(it.boundsInRoot()) }
            .clip(HexPillShape)
            .background(Brush.verticalGradient(listOf(lerp(tint, Color.Black, 0.55f), lerp(tint, Color.Black, 0.82f))))
            .border(MaxSize.emphasisBorder, tint, HexPillShape)
            .clickable(role = Role.Button) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() },
        contentAlignment = Alignment.Center
    ) {
        val inset = height * 0.5f
        Row(Modifier.padding(horizontal = inset), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (busy) {
                CubeSpinner(CubeInk, 12.dp)
                Spacer(Modifier.width(4.dp))
            }
            FitText(text, CubeInk, maxSp = 13f, minSp = 8f, weight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
        }
    }
}

/**
 * مقياس مقاطع يتّسع نحو الأعلى؛ الحافة الخارجية مستقيمة. السحب محلي (لا إعادة تركيب للّوحة)
 * والكتابة عند تبدّل المقطع فقط. وأثناء السحب تظهر النسبة مكان الأيقونة فيعرف المستخدم أين وصل.
 */
@Composable
internal fun CubeMeter(
    value: Float, icon: ImageVector, startAnchored: Boolean, onChange: (Float) -> Unit,
    meterW: Dp, meterH: Dp, modifier: Modifier = Modifier
) {
    val change by rememberUpdatedState(onChange)
    var live by remember { mutableFloatStateOf(value) }
    var grabbed by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (!grabbed) live = value }
    val shown = animateFloatAsState(live, spring(dampingRatio = 1f, stiffness = 900f), label = "meter")
    Column(
        modifier, horizontalAlignment = if (startAnchored) Alignment.Start else Alignment.End,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Canvas(
            Modifier.width(meterW).height(meterH).pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    grabbed = true
                    var segment = -1
                    fun push(y: Float) {
                        val v = (1f - y / size.height).coerceIn(0f, 1f)
                        live = v
                        val seg = CockpitModel.segmentOf(v, METER_BARS)
                        if (seg != segment) {
                            segment = seg
                            change(v)
                        }
                    }
                    push(down.position.y)
                    down.consume()
                    while (true) {
                        val move = awaitPointerEvent().changes.firstOrNull() ?: break
                        if (!move.pressed) break
                        push(move.position.y)
                        move.consume()
                    }
                    grabbed = false
                }
            }
        ) {
            // أشرطة أسمك بفجوات أضيق وزوايا شبه حادّة، والمطفأ رماديّ ظاهر كما في المرجع.
            val pitch = size.height / METER_BARS
            val bar = pitch * 0.64f
            val radius = CornerRadius(1.dp.toPx())
            val level = shown.value.coerceIn(0f, 1f) * METER_BARS
            val full = level.toInt()
            val frac = level - full
            for (k in 0 until METER_BARS) {
                val bw = size.width * (1f - 0.55f * k / (METER_BARS - 1f))
                val x = if (startAnchored) 0f else size.width - bw
                val m2 = METER_BARS - 1 - k
                val a2 = if (m2 < full) 0.92f else if (m2 == full) 0.30f + 0.62f * frac else 0.30f
                drawRoundRect(Color.White.copy(alpha = a2), Offset(x, k * pitch + (pitch - bar) / 2f), Size(bw, bar), radius)
            }
        }
        Box(Modifier.height(16.dp), contentAlignment = Alignment.Center) {
            if (grabbed) {
                Text("${(live * 100f).roundToInt()}%", color = CubeInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            } else {
                Icon(icon, contentDescription = null, tint = CubeDim, modifier = Modifier.size(16.dp))
            }
        }
    }
}

internal const val METER_BARS = 14

/**
 * عمود العدّاد: رقم التردد ووحدته على سطر واحد (الوحدة بجانب الرقم لا فوق القرص — كانت حلقة القرص
 * تقطع «GHz» في اللقطة)، فقرص متوهّج بحمله، فوسم، فحبّة اختيار، فمقياس.
 * [level] مستوى التردد الحيّ 0..1 (`null` ⇒ فارغ)؛ ويُرسم شريطًا أفقيًّا هنا فقط حين [freqBar]
 * (الوسط ضيّق فلا مكان للشريط العموديّ الطويل). كل المقاسات من [CubeLayout].
 */
@Composable
internal fun CubeGauge(
    value: String, unit: String, label: String, tint: Color,
    pill: String, pillBusy: Boolean, pillOpen: Boolean,
    onPill: () -> Unit, onPillBounds: (Rect) -> Unit,
    meter: Float, meterIcon: ImageVector, meterAlpha: Float, onMeter: (Float) -> Unit,
    outerLeft: Boolean, layout: CubeLayout, modifier: Modifier,
    loadPct: Int? = null,
    maxCeiling: String? = null,
    level: Float? = null,
    freqBar: Boolean = false
) {
    Column(modifier.width(layout.gaugeW.dp).swallowTaps(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            FitText(
                value, CubeInk, maxSp = layout.numberSp, minSp = 14f, weight = FontWeight.Black,
                style = FontStyle.Italic, modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(MaxSpace.xs))
            Text(
                unit, color = CubeDim, fontSize = layout.unitSp.sp, maxLines = 1,
                modifier = Modifier.padding(bottom = MaxSpace.xs)
            )
        }
        Box(Modifier.fillMaxWidth().height(layout.orbH.dp), contentAlignment = Alignment.Center) {
            CubeOrb(tint, loadPct, Modifier.size((layout.gaugeW * 0.82f).dp, (layout.orbH * 0.9f).dp))
            if (loadPct != null) {
                Text("$loadPct%", color = CubeInk, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            Text(label, color = tint, fontSize = layout.labelSp.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
            if (maxCeiling != null) {
                FitText(
                    maxCeiling, CubeDim, maxSp = 9f, minSp = 8f, weight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
        if (freqBar) {
            Spacer(Modifier.height(MaxSpace.xs))
            FrequencyBar(
                level, tint,
                Modifier.fillMaxWidth().padding(horizontal = (layout.gaugeW * 0.08f).dp).height(CubeLayoutMath.FREQ_BAR_H.dp)
            )
        }
        Spacer(Modifier.height((layout.u * 1.2f).dp))
        CubePill(pill, tint, layout.pillW.dp, layout.pillH.dp, pillBusy, onPillBounds, onPill)
        Spacer(Modifier.height((layout.u * 1f).dp))
        Box(
            Modifier.fillMaxWidth().padding(horizontal = (layout.gaugeW * 0.15f).dp).graphicsLayer { alpha = meterAlpha },
            contentAlignment = if (outerLeft) Alignment.CenterStart else Alignment.CenterEnd
        ) { CubeMeter(meter, meterIcon, outerLeft, onMeter, layout.meterW.dp, layout.meterH.dp) }
    }
}

/**
 * مقاطع الشريط من الأدنى إلى الأعلى: المضاء بتدرّج لون الوضع من الأعمق إلى الأفتح، والمقطع الجزئيّ
 * يمتزج بقدر كسره فلا يقفز الشريط مقطعًا كاملًا. [vertical] ⇒ الأدنى عند القاع، وإلا عند اليسار.
 */
private fun DrawScope.freqSegments(level: Float, tint: Color, vertical: Boolean, peak: Float = 0f) {
    val n = PanelClockMath.SEGMENTS
    val gap = 2.dp.toPx()
    val along = if (vertical) size.height else size.width
    val seg = ((along - gap * (n - 1)) / n).coerceAtLeast(1f)
    val lit = level.coerceIn(0f, 1f) * n
    val corner = CornerRadius((if (vertical) size.width else size.height) * 0.25f)
    val dark = lerp(tint, Color.Black, 0.35f)
    val bright = lerp(tint, Color.White, 0.30f)
    val off = Color.White.copy(alpha = 0.10f)
    // مقطع «أعلى قيمة حديثة»: يظهر فوق المضاء فقط، وبقدر نصف سطوعه فلا يُظنّ تردّدًا حاليًّا.
    val peakIdx = if (peak > 0.05f) (peak.coerceIn(0f, 1f) * n).toInt().coerceIn(0, n - 1) else -1
    for (k in 0 until n) {
        val start = k * (seg + gap)
        val on = lerp(dark, bright, k / (n - 1f))
        val base = lerp(off, on, (lit - k).coerceIn(0f, 1f))
        val color = if (k == peakIdx && lit <= k) bright.copy(alpha = 0.55f) else base
        if (vertical) {
            drawRoundRect(color, Offset(0f, size.height - start - seg), Size(size.width, seg), corner)
        } else {
            drawRoundRect(color, Offset(start, 0f), Size(seg, size.height), corner)
        }
    }
}

/** مدّة بقاء «أعلى قيمة حديثة» على الشريط قبل أن تهبط إلى المستوى الحالي. */
private const val PEAK_HOLD_MS = 1_800L

/**
 * أعلى مستوى بلغه الشريط حديثًا، يُمسَك [PEAK_HOLD_MS] ثم يهبط بنعومة إلى الحالي — كمؤشّرات القياس
 * الصوتية، فيُرى «بلغ المعالج ٣٫٢ ثم هدأ» ولو لم تكن العين على الشريط لحظة الذروة.
 * لا يحسب إلا من مستويات حقيقية؛ و`null` يُنزله إلى صفر فلا يبقى مؤشّر لقراءة اختفت.
 */
@Composable
internal fun rememberPeak(level: Float?): Float {
    var peak by remember { mutableFloatStateOf(0f) }
    var at by remember { mutableLongStateOf(0L) }
    LaunchedEffect(level) {
        val now = android.os.SystemClock.uptimeMillis()
        val current = level ?: 0f
        if (current >= peak || now - at > PEAK_HOLD_MS) {
            peak = current
            at = now
        } else {
            delay(PEAK_HOLD_MS - (now - at))
            peak = current
            at = android.os.SystemClock.uptimeMillis()
        }
    }
    val shown by animateFloatAsState(peak, tween(300), label = "peak")
    return shown
}

/** الشريط الأفقيّ نفسه (المقاطع نفسها) لعمود قياس ضيّق أو بطاقة الطولي. */
@Composable
internal fun FrequencyBar(level: Float?, tint: Color, modifier: Modifier = Modifier) {
    val shown by animateFloatAsState(level ?: 0f, tween(450), label = "freqBar")
    val peak = rememberPeak(level)
    Canvas(modifier) {
        drawRoundRect(CubeTrack, cornerRadius = CornerRadius(size.height / 2f))
        freqSegments(shown, tint, vertical = false, peak = peak)
    }
}

@Composable
internal fun ViewToggle(
    listView: Boolean, tint: Color, height: Dp, cdList: String, cdGrid: String, onList: () -> Unit, onGrid: () -> Unit
) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(maxRadius(MaxRadius.chip))).background(CubeTile)) {
        listOf(true to Icons.Rounded.Menu, false to Icons.Rounded.Apps).forEach { (isList, icon) ->
            Box(
                Modifier.weight(1f).height(height)
                    .background(if (listView == isList) lerp(tint, Color.Black, 0.5f) else Color.Transparent)
                    .clickable(role = Role.RadioButton) { if (isList) onList() else onGrid() }
                    .semantics { contentDescription = if (isList) cdList else cdGrid },
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = CubeInk, modifier = Modifier.size(20.dp)) }
        }
    }
}

/** عنوان قسم صغير فوق قائمة البطاقات: يسمّي ما تفعله القائمة بدل أن تُترك بلا اسم. */
@Composable
internal fun SectionCaption(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = CubeDim, modifier = Modifier.size(12.dp))
        FitText(text, CubeDim, maxSp = 11f, minSp = 8f, weight = FontWeight.Bold, align = TextAlign.Left, modifier = Modifier.weight(1f))
    }
}

/**
 * صفّ خيار: قرص أيقونة، واسم، وسطر فرعي، وعلامة الاختيار أو مؤشّر الانتظار.
 * [card] يعطيه خلفية بطاقة (القائمة اليسرى)، وبدونها يكون صفًّا شفّافًا داخل قائمة منسدلة.
 */
@Composable
internal fun OptionRow(
    option: CubeOption, selected: Boolean, busy: Boolean, tint: Color, height: Dp,
    card: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(maxRadius(MaxRadius.control))
    val fill = when {
        selected && card -> Brush.verticalGradient(listOf(lerp(tint, Color.Black, 0.35f), lerp(tint, Color.Black, 0.6f)))
        selected -> SolidColor(tint.copy(alpha = 0.22f))
        card -> SolidColor(CubeTile)
        else -> SolidColor(Color.Transparent)
    }
    Row(
        modifier.fillMaxWidth().height(height).graphicsLayer { alpha = if (option.enabled) 1f else 0.5f }
            .clip(shape).background(fill, shape)
            .then(if (selected && card) Modifier.border(MaxSize.hairlineBorder, tint, shape) else Modifier)
            .clickable(enabled = option.enabled, role = Role.RadioButton) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = if (card) MaxSpace.xs + 2.dp else MaxSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (card) MaxSpace.xs + 2.dp else MaxSpace.sm)
    ) {
        Box(Modifier.size(height * 0.66f).clip(CircleShape).background(CubeGlass), contentAlignment = Alignment.Center) {
            Icon(option.icon, null, tint = option.color, modifier = Modifier.size(height * 0.4f))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            FitText(option.label, CubeInk, maxSp = 13f, minSp = 9f, weight = FontWeight.Bold, align = TextAlign.Left, modifier = Modifier.fillMaxWidth())
            if (option.sub.isNotEmpty()) {
                FitText(option.sub, option.subColor, maxSp = 10.5f, minSp = 8f, align = TextAlign.Left, modifier = Modifier.fillMaxWidth())
            }
        }
        if (busy) {
            CubeSpinner(CubeInk, 16.dp)
        } else if (selected && !card) {
            // علامة الاختيار للقوائم المنسدلة فقط؛ في بطاقات العمود الضيّق تأكل عرض الاسم، ولون البطاقة وإطارها يكفيان.
            Icon(Icons.Rounded.Check, null, tint = CubeInk, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
internal fun InfoCard(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(maxRadius(MaxRadius.control))).background(CubeTile).padding(MaxSpace.sm)) { content() }
}

/** بلاطة الشبكة: مربّع بأيقونة (أو نصّ قصير) ووسمه تحته، والنشطة تتدرّج بلون الوضع. */
@Composable
internal fun CubeTileView(spec: CubeTileSpec, tile: Dp, colW: Dp, tint: Color, labelSp: Float) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(maxRadius(MaxRadius.control))
    val fill = if (spec.active) Brush.verticalGradient(listOf(lerp(tint, Color.Black, 0.35f), lerp(tint, Color.Black, 0.6f))) else SolidColor(CubeTile)
    val ink = if (spec.active) Color.White else Color(0xFFD0D3D8)
    Column(Modifier.width(colW), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(
            Modifier.size(tile)
                .then(if (spec.onBounds != null) Modifier.onGloballyPositioned { spec.onBounds.invoke(it.boundsInRoot()) } else Modifier)
                .clip(shape).background(fill, shape)
                .border(MaxSize.hairlineBorder, if (spec.active) tint.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f), shape)
                .clickable(role = Role.Button) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); spec.onClick() }
                .semantics { contentDescription = spec.label },
            contentAlignment = Alignment.Center
        ) {
            if (spec.icon != null) {
                Icon(spec.icon, null, tint = spec.iconTint ?: ink, modifier = Modifier.size(tile * 0.5f))
            } else {
                Text(
                    spec.text ?: "", color = ink, fontSize = max(13f, tile.value * 0.34f).coerceAtMost(20f).sp,
                    fontWeight = FontWeight.Black, maxLines = 1
                )
            }
        }
        FitText(spec.label, CubeInk, maxSp = labelSp, minSp = 8f, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * قائمة منسدلة **داخل الشجرة نفسها** لا نافذة ثانية: نافذة اللوحة تراكب بلا رمز تطبيق، وأي
 * `Popup` فوقها يسقط بـ`BadTokenException` على بعض الأجهزة. تُثبَّت تحت الحبّة (أو فوقها إن
 * ضاق ما تحتها)، ولمسة خارجها تُغلقها دون أن تُطوى اللوحة كلها.
 */
@Composable
internal fun CubeDropdown(
    title: String, options: List<CubeOption>, selectedId: String, busyId: String?, tint: Color,
    anchor: Rect, screenW: Dp, screenH: Dp, topLimit: Dp, density: Float,
    onSelect: (String) -> Unit, onDismiss: () -> Unit
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(150, easing = FastOutSlowInEasing)) }
    val rowH = 42f
    val titleH = 26f
    val pad = 6f
    val dropW = 188f
    val sw = screenW.value
    val sh = screenH.value
    val centerX = anchor.center.x / density
    val x = (centerX - dropW / 2f).coerceIn(8f, max(8f, sw - dropW - 8f))
    // تختار الجهة الأوسع (أسفل الحبّة أو فوقها)، وتُقصّ القائمة إلى ما يتّسع لها بالتمرير: لا خيار يخرج من الشاشة.
    val below = anchor.bottom / density + 6f
    val spaceBelow = sh - 8f - below
    val spaceAbove = anchor.top / density - 6f - topLimit.value
    val fullH = titleH + rowH * options.size + pad * 2f
    val placeBelow = fullH <= spaceBelow || spaceBelow >= spaceAbove
    val space = max(if (placeBelow) spaceBelow else spaceAbove, titleH + rowH + pad * 2f)
    val rows = minOf(options.size.toFloat(), kotlin.math.floor((space - titleH - pad * 2f) / rowH))
    val listH = rowH * rows
    val dropH = titleH + listH + pad * 2f
    val y = if (placeBelow) below else max(topLimit.value, anchor.top / density - dropH - 6f)
    val scroll = rememberScrollState()
    val shape = RoundedCornerShape(maxRadius(MaxRadius.control))
    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onDismiss() } }) {
        Column(
            Modifier.offset(x.dp, y.dp).width(dropW.dp)
                .graphicsLayer {
                    alpha = appear.value
                    scaleX = 0.94f + 0.06f * appear.value
                    scaleY = 0.94f + 0.06f * appear.value
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .clip(shape).background(CubeMenuBg).border(MaxSize.hairlineBorder, tint.copy(alpha = 0.55f), shape)
                .swallowTaps().padding(pad.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            FitText(
                title, CubeDim, maxSp = 11f, minSp = 8f, weight = FontWeight.Bold, align = TextAlign.Left,
                modifier = Modifier.fillMaxWidth().height(titleH.dp).padding(horizontal = 6.dp)
            )
            if (rows < options.size) {
                FadeScroll(Modifier.height(listH.dp), scroll, 0.dp) {
                    CubeOptionRows(options, selectedId, busyId, tint, rowH, onSelect)
                }
            } else {
                CubeOptionRows(options, selectedId, busyId, tint, rowH, onSelect)
            }
        }
    }
}

/** صفوف خيارات القائمة المنسدلة، تُعرض داخل عمود القائمة أو داخل منطقة التمرير عند الحاجة. */
@Composable
private fun ColumnScope.CubeOptionRows(
    options: List<CubeOption>, selectedId: String, busyId: String?, tint: Color, rowH: Float,
    onSelect: (String) -> Unit
) {
    options.forEach { option ->
        OptionRow(
            option = option, selected = option.id == selectedId, busy = option.id == busyId, tint = tint,
            height = rowH.dp, card = false, onClick = { onSelect(option.id) }
        )
    }
}

/**
 * عمود قابل للتمرير بتلاشٍ سفليّ ومؤشّر موضع: ما بقي تحت الحافة يُرى أنه «يتبع» بدل أن يظهر
 * صفّ مبتور بلا إشارة (علّة الشبكة اليمنى في اللقطة).
 */
@Composable
internal fun FadeScroll(
    modifier: Modifier, state: ScrollState, spacing: Dp, content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // القراءة داخل الرسم لا التركيب: التمرير لا يُعيد تركيب القائمة كل إطار.
                    if (state.value < state.maxValue) {
                        drawRect(
                            Brush.verticalGradient(0.78f to Color.Black, 1f to Color.Transparent),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
                .verticalScroll(state),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content
        )
        Canvas(Modifier.matchParentSize()) {
            val range = state.maxValue
            if (range in 1 until Int.MAX_VALUE) {
                val track = size.height
                val barH = (track * track / (track + range)).coerceAtLeast(14.dp.toPx())
                val top = (track - barH) * (state.value.toFloat() / range)
                val w = 2.dp.toPx()
                drawRoundRect(
                    Color.White.copy(alpha = 0.35f), Offset(size.width - w, top), Size(w, barH), CornerRadius(w / 2f)
                )
            }
        }
    }
}
