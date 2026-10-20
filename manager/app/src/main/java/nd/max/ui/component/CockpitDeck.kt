package nd.max.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.sqrt
import nd.max.R
import nd.max.core.gamespace.QuickApp
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.maxRadius

/*
 * سطح اللعبة العرضي بتصميم «RedMagic Game Space» — مكتوب من الصفر ومستقلّ عن CubeLayoutMath:
 *   • أعلى: ثلاث دوائر (رئيسية · تحرير · طيّ) يسارًا، وثلاثة تطبيقات سريعة + زرّ يطوي الباقي + إضافة يمينًا، بلا خطّ فاصل.
 *   • يسار: بطاقات بروفايل الحرارة (قائمة/شبكة) · وسط: عمودا CPU/GPU (رقم كبير، قرص متوهّج، حبّة وضع، مقياس).
 *   • شريطان شيفرون على كامل الارتفاع (قمّة عند ٣٢٪ نحو الوسط، ١٧ مقطعًا، إطار رفيع) يمتلئان من الأسفل بقدر التردد الحيّ.
 *   • يمين: شبكة بلاطات بعمودين ووسمها تحتها · أسفل الوسط: شريط الحالة وسطر الرسائل.
 * خارج الشريطين أسود صلب كالمرجع، وما بينهما نافذة اللعبة بلا أيّ تعتيم.
 */

internal class DeckMetrics(
    val edge: Float,
    val btn: Float,
    val topY: Float,
    val dividerY: Float,
    val bodyTop: Float,
    val leftW: Float,
    val gaugeW: Float,
    val colW: Float,
    val tile: Float,
    val colGap: Float,
    val gridW: Float,
    val cpuX: Float,
    val gpuX: Float,
    val railW: Float,
    val railTopL: Float,
    val railApexL: Float,
    val railBotL: Float,
    val railTopR: Float,
    val railApexR: Float,
    val railBotR: Float,
    val railOn: Boolean,
    val bottomRoom: Float,
    val meterH: Float,
    val tight: Boolean,
    /** أقصى عرض لصفّ التطبيقات السريعة يمينًا قبل أن يلمس خطّ الشريط الأيمن (dp). */
    val appsMaxW: Float = Float.MAX_VALUE,
    /** كم دائرة (تطبيق · المزيد · الإضافة) تتّسع في [appsMaxW]. */
    val appsSlots: Int = Int.MAX_VALUE
)

internal object DeckMath {
    fun of(w: Float, h: Float, leftOpen: Boolean): DeckMetrics {
        val edge = if (w < 700f) 10f else 16f
        val btn = (h * 0.115f).coerceIn(36f, 50f)
        val topY = 6f
        val dividerY = topY + btn + 8f
        val bodyTop = dividerY + 10f
        val leftW = if (leftOpen) (w * 0.155f).coerceIn(112f, 168f) else 0f
        val colW = (w * 0.075f).coerceIn(54f, 78f)
        val colGap = 6f
        val gridW = colW * 2f + colGap
        val gaugeW = (w * 0.145f).coerceIn(100f, 150f)
        val gap = 10f
        val cpuX = edge + leftW + if (leftOpen) gap else 0f
        val gpuX = w - edge - gridW - gap - gaugeW
        // الشريطان بهندسة المرجع مقيسةً بالبكسل: قمّة عند ٣٢٪ من الارتفاع نحو الوسط وقاعدتان عند الحافتين.
        // المواضع نسبةً إلى مركز كلّ عمود، فتلزمه سواء فُتح اليسار أو أُغلق.
        val cpuC = cpuX + gaugeW / 2f
        val gpuC = gpuX + gaugeW / 2f
        val railW = (w * 0.036f).coerceIn(14f, 44f)
        val keep = railW / 2f + 4f
        val railTopL = cpuC + w * 0.0355f
        val railApexL = cpuC + w * 0.131f
        val railBotL = (cpuC - w * 0.0747f).coerceAtLeast(keep)
        val railTopR = gpuC - w * 0.0355f
        val railApexR = gpuC - w * 0.131f
        val railBotR = (gpuC + w * 0.0747f).coerceAtMost(w - keep)
        val railOn = railApexR - railApexL - railW >= 120f
        val bottomRoom = (railBotR - railBotL - railW - 24f).coerceIn(140f, 420f)
        // مقياس الصوت والإضاءة: ارتفاع ثابت قريب من المرجع، تحت الحبّة مباشرةً
        val meterH = (h * 0.2f).coerceIn(56f, 96f)
        // صفّ التطبيقات يمينًا لا يدخل خطّ الشريط الأيمن: المسافة تُقاس عموديًّا عن الخطّ المائل (لا أفقيًّا) لأن الدائرة
        // تلمس الخطّ بأقرب نقطة منها لا بحافتها اليسرى. ما لا يتّسع يُطوى خلف زرّ «المزيد».
        val appsBase = edge + btn * 3f + APPS_GAP * 2f + gap
        val appsLeft = if (railOn) {
            maxOf(appsBase, circleClearX(topY + btn / 2f, btn / 2f, APPS_RAIL_GAP) { y ->
                railRightEdge(y, h, railTopR, railApexR, railBotR, railW)
            })
        } else appsBase
        val appsMaxW = maxOf(w - edge - appsLeft, btn * 2f + APPS_GAP)
        val appsSlots = maxOf(((appsMaxW + APPS_GAP) / (btn + APPS_GAP)).toInt(), 2)
        return DeckMetrics(
            edge, btn, topY, dividerY, bodyTop, leftW, gaugeW, colW, colW - 12f, colGap, gridW,
            cpuX, gpuX, railW, railTopL, railApexL, railBotL, railTopR, railApexR, railBotR, railOn, bottomRoom, meterH,
            h < 340f, appsMaxW, appsSlots
        )
    }

    /** يمين الشريط الأيمن (إطاره الخارجي مع وهجه) عند الارتفاع [y] بالـdp. */
    fun rightRailEdge(m: DeckMetrics, h: Float, y: Float): Float =
        railRightEdge(y, h, m.railTopR, m.railApexR, m.railBotR, m.railW)
}

/** كم تطبيقًا يُعرض مطويًّا، وهل يلزم زرّ «المزيد»؟ التطبيقات + المزيد + الإضافة تتّسع كلّها في [slots] دائرة. */
internal class DeckAppsFit(val shown: Int, val more: Boolean)

internal fun deckAppsFit(count: Int, slots: Int): DeckAppsFit {
    val room = (slots - 1).coerceAtLeast(0) // ما عدا زرّ الإضافة
    val folded = minOf(count, APPS_FOLDED)
    if (count <= APPS_FOLDED && folded <= room) return DeckAppsFit(count, false)
    return DeckAppsFit(minOf(folded, (room - 1).coerceAtLeast(0)), true)
}

private const val APPS_GAP = 10f
private const val APPS_RAIL_GAP = 6f

/** أيسر موضع تبدأ منه دائرة نصف قطرها [r] ومركزها الرأسي [yc] وبينها وبين الخطّ المائل [gap] على الأقلّ. */
private fun circleClearX(yc: Float, r: Float, gap: Float, edgeAt: (Float) -> Float): Float {
    val slope = (edgeAt(yc + 1f) - edgeAt(yc - 1f)) / 2f
    return edgeAt(yc) + (r + gap) * sqrt(1f + slope * slope) - r
}

/** معدّل الإطارات الحيّ من قياس اللعبة نفسها: آخر عيّنة، أو شرطة إن لم تُقَس (لا تقدير). */
internal fun cockpitFpsText(frames: List<Float>, unit: String): String {
    val fps = frames.lastOrNull()?.takeIf { it > 0f } ?: return MAX_VALUE_UNAVAILABLE
    return "${fps.roundToInt()} $unit"
}

internal class DeckGaugeSpec(
    val label: String,
    val value: String,
    val unit: String,
    val level: Float?,
    val loadPct: Int?,
    val pill: String,
    val pillOpen: Boolean,
    val onPill: () -> Unit,
    val onPillBounds: (Rect) -> Unit,
    val description: String
)
private const val APPS_FOLDED = 3
private val DeckGlass = Brush.verticalGradient(listOf(Color(0xE62B2D33), Color(0xE617181C)))
private val DeckBar = Color(0xB30C0D10)
/** السواد خارج الشريطين: يستخدمه `CockpitRails.kt` أيضًا، فـ`internal` لا `private`. */
internal val DeckInk = Color(0xFF050507)

private fun Modifier.deckEnter(p: () -> Float, dx: Float, dy: Float, delay: Float): Modifier = graphicsLayer {
    val t = ((p() - delay) / (1f - delay)).coerceAtLeast(0f)
    alpha = t.coerceIn(0f, 1f)
    translationX = (1f - t) * dx * density
    translationY = (1f - t) * dy * density
}

/** انكماش خفيف عند الضغط بدل الموجة الافتراضية: ردّ لمس فوري يُرى على الجهاز. */
@Composable
private fun pressScale(source: MutableInteractionSource): State<Float> {
    val pressed by source.collectIsPressedAsState()
    return animateFloatAsState(if (pressed) 0.92f else 1f, tween(90), label = "press")
}

@Composable
private fun DeckCircle(
    size: Dp, description: String, tint: Color, active: Boolean, onClick: () -> Unit, content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    val fill = if (active) {
        Brush.verticalGradient(listOf(lerp(tint, Color.Black, 0.35f), lerp(tint, Color.Black, 0.65f)))
    } else {
        DeckGlass
    }
    Box(
        Modifier.size(size).graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(CircleShape).background(fill)
            .border(MaxSize.hairlineBorder, if (active) tint else Color.White.copy(alpha = 0.12f), CircleShape)
            .clickable(interactionSource = source, indication = null, role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { contentDescription = description; selected = active },
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun DeckTileView(spec: CubeTileSpec, tile: Dp, colW: Dp, tint: Color) {
    val haptic = LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    val shape = RoundedCornerShape(maxRadius(MaxRadius.inset))
    val fill = if (spec.active) {
        Brush.verticalGradient(listOf(lerp(tint, Color.Black, 0.15f), lerp(tint, Color.Black, 0.55f)))
    } else {
        DeckGlass
    }
    val ink = if (spec.active) Color.White else Color(0xFFD3D6DB)
    Column(
        Modifier.width(colW), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            Modifier.size(tile)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                .drawBehind {
                    // هالة خفيفة خارج البلاطة النشطة فقط.
                    if (spec.active) {
                        val g = 3.dp.toPx()
                        drawRoundRect(
                            tint.copy(alpha = 0.22f), Offset(-g, -g), Size(size.width + 2f * g, size.height + 2f * g),
                            CornerRadius(19.dp.toPx())
                        )
                    }
                }
                .then(
                    if (spec.onBounds != null) Modifier.onGloballyPositioned { spec.onBounds.invoke(it.boundsInRoot()) }
                    else Modifier
                )
                .clip(shape).background(fill, shape)
                .border(MaxSize.hairlineBorder, if (spec.active) tint else Color.White.copy(alpha = 0.08f), shape)
                .clickable(interactionSource = source, indication = null, role = Role.Button) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    spec.onClick()
                }
                .semantics { contentDescription = spec.label; selected = spec.active },
            contentAlignment = Alignment.Center
        ) {
            if (spec.icon != null) {
                Icon(spec.icon, null, tint = spec.iconTint ?: ink, modifier = Modifier.size(tile * 0.5f))
            } else {
                Text(
                    spec.text ?: "", color = ink, fontSize = (tile.value * 0.3f).coerceIn(12f, 18f).sp,
                    fontWeight = FontWeight.Black, maxLines = 1
                )
            }
        }
        FitText(spec.label, if (spec.active) CubeInk else CubeDim, maxSp = 10f, minSp = 8f, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DeckProfileCard(
    option: CubeOption, selected: Boolean, busy: Boolean, tint: Color, height: Dp, onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    val shape = RoundedCornerShape(maxRadius(MaxRadius.row))
    val fill = if (selected) {
        Brush.horizontalGradient(listOf(lerp(tint, Color.Black, 0.35f), lerp(tint, Color.Black, 0.70f)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xE62B2D33), Color(0xE61B1C20)))
    }
    Row(
        Modifier.fillMaxWidth().height(height)
            .graphicsLayer {
                scaleX = scale.value; scaleY = scale.value
                alpha = if (option.enabled) 1f else 0.5f
            }
            .clip(shape).background(fill, shape)
            .border(MaxSize.hairlineBorder, if (selected) tint else Color.White.copy(alpha = 0.08f), shape)
            .clickable(interactionSource = source, indication = null, enabled = option.enabled, role = Role.RadioButton) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { this.selected = selected }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(height * 0.58f).clip(CircleShape).background(Color(0x66000000)),
            contentAlignment = Alignment.Center
        ) { Icon(option.icon, null, tint = option.color, modifier = Modifier.size(height * 0.34f)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            FitText(
                option.label, CubeInk, maxSp = 12f, minSp = 9f, weight = FontWeight.Bold,
                align = TextAlign.Left, modifier = Modifier.fillMaxWidth()
            )
            if (option.sub.isNotEmpty()) {
                FitText(option.sub, option.subColor, maxSp = 10f, minSp = 8f, align = TextAlign.Left, modifier = Modifier.fillMaxWidth())
            }
        }
        if (busy) {
            CubeSpinner(CubeInk, 16.dp)
        } else if (selected) {
            Box(
                Modifier.size(7.dp)
                    .drawBehind { drawCircle(tint.copy(alpha = 0.35f), radius = size.minDimension) }
                    .clip(CircleShape).background(lerp(tint, Color.White, 0.5f))
            )
        }
    }
}

/** تبديل قائمة/شبكة: كبسولة بقطعتين، والمختارة تتدرّج بلون الوضع. */
@Composable
private fun DeckViewToggle(
    listView: Boolean, tint: Color, height: Dp, cdList: String, cdGrid: String, onList: () -> Unit, onGrid: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.fillMaxWidth().height(height).clip(shape).background(DeckGlass)
            .border(MaxSize.hairlineBorder, Color.White.copy(alpha = 0.08f), shape).padding(3.dp)
    ) {
        listOf(true to Icons.Rounded.Menu, false to Icons.Rounded.Apps).forEach { (isList, icon) ->
            val on = listView == isList
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(shape)
                    .background(
                        if (on) Brush.verticalGradient(listOf(lerp(tint, Color.Black, 0.2f), lerp(tint, Color.Black, 0.55f)))
                        else SolidColor(Color.Transparent)
                    )
                    .clickable(role = Role.RadioButton) { if (isList) onList() else onGrid() }
                    .semantics { contentDescription = if (isList) cdList else cdGrid; selected = on },
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = if (on) Color.White else CubeDim, modifier = Modifier.size(16.dp)) }
        }
    }
}

/** بطاقة معلومات بنفس لغة البطاقات الأخرى (بدل InfoCard القديمة). */
@Composable
internal fun DeckCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.row))
    Box(
        Modifier.fillMaxWidth().clip(shape).background(DeckGlass)
            .border(MaxSize.hairlineBorder, Color.White.copy(alpha = 0.08f), shape).padding(10.dp)
    ) { content() }
}

private const val METER_W = 34f
private const val METER_CHIP = 28f
private const val METER_GAP = 4f
private const val METER_MAX_BARS = 10

/** عدد مقاطع مقياس الصوت/الإضاءة: يتّسع لارتفاعه بمقطع لا يقلّ عن ٣dp وفاصل ٢dp (٤ على الأقلّ و١٠ على الأكثر). */
internal fun deckMeterBars(meterH: Float): Int {
    val track = meterH - METER_GAP - METER_CHIP
    return ((track + 2f) / 5f).toInt().coerceIn(4, METER_MAX_BARS)
}

/**
 * مقياس عمودي من مقاطع سميكة بحواف مستديرة تضيق نحو الأعلى، وتحته زرّ زجاجيّ دائريّ بأيقونة الوظيفة.
 * لمسة أو سحب على المقاطع يكتب القيمة، ونبضة لمس عند كل مقطع جديد؛ والمقاطع تنزلق بنعومة عند تغيّر القيمة من خارج
 * اللمس وتتبع الإصبع فورًا أثناء السحب. الزرّ يضيء بلون الوضع متى كانت القيمة فوق الصفر، ويعرض الرقم أثناء السحب.
 */
@Composable
private fun DeckMeter(
    value: Float, tint: Color, meterAlpha: Float, icon: ImageVector, label: String, alignEnd: Boolean, bars: Int,
    onChange: (Float) -> Unit, modifier: Modifier
) {
    val haptic = LocalHapticFeedback.current
    val latest by rememberUpdatedState(onChange)
    var dragging by remember { mutableStateOf(false) }
    var local by remember { mutableFloatStateOf(0f) }
    var lastSeg by remember { mutableIntStateOf(-1) }
    val shown = if (dragging) local else value
    val glide = animateFloatAsState(shown, if (dragging) snap<Float>() else tween<Float>(160), label = "meter")
    fun emit(v: Float) {
        local = v
        val seg = (v * bars).toInt()
        if (seg != lastSeg) {
            lastSeg = seg
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        latest(v)
    }
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Box(
            Modifier.weight(1f).width(METER_W.dp)
                .semantics {
                    contentDescription = label
                    stateDescription = "${(shown * 100).roundToInt()}%"
                }
                .pointerInput(Unit) {
                    detectTapGestures { o -> emit((1f - o.y / size.height).coerceIn(0f, 1f)) }
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { o ->
                            dragging = true
                            emit((1f - o.y / size.height).coerceIn(0f, 1f))
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            emit((1f - change.position.y / size.height).coerceIn(0f, 1f))
                        }
                    )
                }
        ) {
            Canvas(Modifier.matchParentSize().graphicsLayer { this.alpha = meterAlpha }) {
                val n = bars
                val gap = 2.dp.toPx()
                val bh = (size.height - gap * (n - 1)) / n
                val v = glide.value.coerceIn(0f, 1f)
                // أيّ قيمة فوق الصفر تُضيء مقطعًا واحدًا على الأقلّ فلا يبدو المقياس فارغًا وهو غير صفريّ
                val lit = if (v > 0.005f) (v * n).roundToInt().coerceIn(1, n) else 0
                for (i in 0 until n) {
                    val frac = i / (n - 1f)
                    val bw = size.width * (1f - 0.36f * frac)
                    val y = size.height - (i + 1) * bh - i * gap
                    val x = if (alignEnd) size.width - bw else 0f
                    val corner = CornerRadius(bh / 2f)
                    if (i < lit) {
                        val top = i == lit - 1
                        if (top) {
                            val halo = 2.dp.toPx()
                            drawRoundRect(
                                tint.copy(alpha = 0.40f), Offset(x - halo, y - halo),
                                Size(bw + 2f * halo, bh + 2f * halo), CornerRadius(bh / 2f + halo)
                            )
                        }
                        val c = if (top) lerp(tint, Color.White, 0.40f)
                        else lerp(Color.White, tint, 0.16f).copy(alpha = 0.55f + 0.35f * (i / lit.coerceAtLeast(1).toFloat()))
                        drawRoundRect(c, Offset(x, y), Size(bw, bh), corner)
                    } else {
                        drawRoundRect(Color.White.copy(alpha = 0.10f), Offset(x, y), Size(bw, bh), corner)
                    }
                }
            }
        }
        Spacer(Modifier.height(METER_GAP.dp))
        DeckMeterChip(icon, tint, shown > 0.005f, dragging, (shown * 100).roundToInt(), meterAlpha)
    }
}

/** الزرّ الزجاجيّ تحت المقياس: أيقونة الوظيفة بلون الوضع متى كان المقياس غير صفريّ، ورقم النسبة أثناء السحب. */
@Composable
private fun DeckMeterChip(icon: ImageVector, tint: Color, active: Boolean, dragging: Boolean, percent: Int, meterAlpha: Float) {
    val pop by animateFloatAsState(if (dragging) 1.1f else 1f, tween(120), label = "chip")
    Box(
        Modifier.width(METER_W.dp).height(METER_CHIP.dp).graphicsLayer { alpha = meterAlpha },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(METER_CHIP.dp).graphicsLayer { scaleX = pop; scaleY = pop }
                .clip(CircleShape).background(DeckBar)
                .border(
                    MaxSize.hairlineBorder,
                    if (dragging) tint.copy(alpha = 0.85f) else if (active) tint.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.10f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (dragging) {
                Text("$percent", color = CubeInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            } else {
                Icon(
                    icon, null, tint = if (active) lerp(CubeInk, tint, 0.25f) else CubeDim,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** رقم كبير بخطّ رقميّ مائل وتوهّج بلون الوضع؛ يصغّر خطّه بخطوات حتى يتّسع. */
@Composable
private fun DeckNumber(text: String, maxSp: Float, minSp: Float, glow: Color, modifier: Modifier) {
    var cur by remember(text, maxSp) { mutableFloatStateOf(maxSp) }
    Text(
        text = text, color = CubeInk, fontSize = cur.sp, fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic, fontFamily = FontFamily.Monospace, letterSpacing = (-1).sp,
        maxLines = 1, softWrap = false,
        style = TextStyle(shadow = Shadow(glow.copy(alpha = 0.65f), blurRadius = 16f)),
        onTextLayout = { if (it.hasVisualOverflow && cur > minSp) cur = (cur - 0.75f).coerceAtLeast(minSp) },
        modifier = modifier
    )
}

/** القرص المتوهّج: نبضة هادئة، وقوس حمل فعليّ على الحلقة الخارجية (حلقة كاملة إن لم يُقرأ الحمل). */
@Composable
private fun DeckDisc(tint: Color, loadPct: Int?, width: Dp, tight: Boolean) {
    val pulse by rememberInfiniteTransition(label = "disc").animateFloat(
        initialValue = 0.80f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val load by animateFloatAsState((loadPct ?: 0) / 100f, tween(450), label = "load")
    Box(
        Modifier.padding(top = 2.dp).width(width * 0.9f).height(width * (if (tight) 0.24f else 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawOval(
                Brush.radialGradient(
                    listOf(tint.copy(alpha = 0.55f * pulse), tint.copy(alpha = 0f)), center = center, radius = size.width / 2f
                )
            )
            val sw = 2.dp.toPx()
            val topLeft = Offset(sw, sw)
            val oval = Size(size.width - 2f * sw, size.height - 2f * sw)
            if (loadPct != null) {
                drawOval(tint.copy(alpha = 0.35f), topLeft, oval, style = Stroke(sw))
                drawArc(
                    lerp(tint, Color.White, 0.35f), 150f, 240f * load.coerceIn(0f, 1f), false, topLeft, oval,
                    style = Stroke(sw * 1.8f, cap = StrokeCap.Round)
                )
            } else {
                drawOval(tint, topLeft, oval, style = Stroke(sw))
            }
            val iw = size.width * 0.14f
            val ih = size.height * 0.14f
            drawOval(
                tint.copy(alpha = 0.55f), Offset(iw, ih), Size(size.width - 2f * iw, size.height - 2f * ih),
                style = Stroke(1.5.dp.toPx())
            )
            val cw = size.width * 0.30f
            val ch = size.height * 0.30f
            drawOval(
                lerp(tint, Color.Black, 0.55f).copy(alpha = 0.9f), Offset(cw, ch),
                Size(size.width - 2f * cw, size.height - 2f * ch)
            )
        }
        loadPct?.let { Text("$it%", color = CubeInk, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun DeckGauge(
    spec: DeckGaugeSpec, tint: Color, width: Dp, tight: Boolean, outerStart: Boolean, freqBar: Boolean,
    meterValue: Float, meterAlpha: Float, meterIcon: ImageVector, meterLabel: String,
    onMeter: (Float) -> Unit, meterH: Dp, modifier: Modifier
) {
    val numSp = (width.value * 0.30f).coerceIn(26f, 42f) * (if (tight) 0.85f else 1f)
    Column(modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.semantics { contentDescription = spec.description },
            verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center
        ) {
            DeckNumber(spec.value, numSp, 20f, tint, Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(3.dp))
            Text(spec.unit, color = CubeDim, fontSize = 11.sp, modifier = Modifier.padding(bottom = 5.dp))
        }
        DeckDisc(tint, spec.loadPct, width, tight)
        Text(
            spec.label, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic, letterSpacing = 1.sp
        )
        Spacer(Modifier.height(if (tight) 3.dp else 6.dp))
        CubePill(spec.pill, tint, width * 0.8f, if (tight) 20.dp else 22.dp, false, spec.onPillBounds, spec.onPill)
        if (!tight) {
            Icon(
                Icons.Rounded.KeyboardArrowDown, null, tint = CubeDim,
                modifier = Modifier.size(16.dp).rotate(if (spec.pillOpen) 180f else 0f)
            )
        }
        if (freqBar) {
            Spacer(Modifier.height(2.dp))
            FrequencyBar(spec.level, tint, Modifier.fillMaxWidth(0.84f).height(12.dp))
        }
        DeckMeter(
            value = meterValue, tint = tint, meterAlpha = meterAlpha, icon = meterIcon, label = meterLabel,
            alignEnd = !outerStart, bars = deckMeterBars(meterH.value), onChange = onMeter,
            modifier = Modifier.padding(top = 12.dp).height(meterH).fillMaxWidth()
        )
    }
}

@Composable
private fun DeckStatusBit(icon: ImageVector?, text: String, iconTint: Color = CubeDim) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Icon(icon, null, tint = iconTint, modifier = Modifier.size(13.dp))
        Text(text, color = CubeInk, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun DeckSep() {
    Box(Modifier.size(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)))
}

@Composable
internal fun BoxScope.CockpitDeck(
    m: DeckMetrics, w: Float, h: Float, enter: () -> Float, tint: Color, leftOpen: Boolean,
    title: String, batteryText: String, charging: Boolean, batteryLow: Boolean,
    fpsText: String, note: String?, hint: String,
    apps: List<QuickApp>, launching: String?, onApp: (QuickApp) -> Unit,
    appsOpen: Boolean, appsEditLabel: String, onAppsEdit: () -> Unit, onAppsBounds: (Rect) -> Unit,
    homeLabel: String, onHome: () -> Unit, sideLabel: String, onToggleLeft: () -> Unit,
    collapseLabel: String, onCollapse: () -> Unit,
    cpu: DeckGaugeSpec, gpu: DeckGaugeSpec,
    brightness: Float, brightnessAllowed: Boolean, brightnessIcon: ImageVector, brightnessLabel: String,
    onBrightness: (Float) -> Unit,
    volume: Float, volumeIcon: ImageVector, volumeLabel: String, onVolume: (Float) -> Unit,
    listView: Boolean, onListView: (Boolean) -> Unit, cdList: String, cdGrid: String,
    thermalTitle: String, thermalOptions: List<CubeOption>, thermalSelected: String, thermalBusy: String?,
    onPickThermal: (String) -> Unit, leftScroll: ScrollState, gridScroll: ScrollState,
    tiles: List<CubeTileSpec>, extra: @Composable ColumnScope.() -> Unit
) {
    val cardH = (h * 0.115f).coerceIn(40f, 52f).dp
    var appsMore by remember { mutableStateOf(false) }
    val moreLabel = stringResource(R.string.game_panel_shelf_more)
    // الدوائر التي لا تتّسع قبل خطّ الشريط الأيمن تُطوى خلف «المزيد»؛ وعند التوسعة يُمرَّر الصفّ داخل المساحة نفسها
    val fit = deckAppsFit(apps.size, m.appsSlots)
    val expanded = appsMore && fit.more
    val appsScroll = rememberScrollState()
    LaunchedEffect(expanded) {
        if (expanded) {
            withFrameNanos { } // يُقاس الصفّ الموسَّع أولًا، ثم يُظهَر طرفه حيث زرّا الطيّ والإضافة
            appsScroll.animateScrollTo(appsScroll.maxValue)
        } else appsScroll.scrollTo(0)
    }

    // ── الخلفية: خارج الشريطين أسود صلب كالمرجع، وما بينهما نافذة اللعبة بلا أيّ تعتيم (بلا خطّ فاصل)
    Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = enter().coerceIn(0f, 1f) }) {
        val wPx = size.width
        val hPx = size.height
        if (m.railOn) {
            val reach = (m.railW / 2f).dp.toPx() + 3.dp.toPx()
            railBlack(m.railTopL.dp.toPx(), m.railApexL.dp.toPx(), m.railBotL.dp.toPx(), reach, true)
            railBlack(m.railTopR.dp.toPx(), m.railApexR.dp.toPx(), m.railBotR.dp.toPx(), reach, false)
        } else {
            // شاشة ضيّقة بلا شريطين: تدرّجات حواف فقط ليُرى اللعب
            val topH = (m.dividerY + 28f).dp.toPx()
            drawRect(
                Brush.verticalGradient(listOf(DeckInk.copy(alpha = 0.80f), Color.Transparent), startY = 0f, endY = topH),
                size = Size(wPx, topH)
            )
            val lw = (m.cpuX + m.gaugeW + 12f).dp.toPx()
            drawRect(
                Brush.horizontalGradient(
                    listOf(DeckInk.copy(alpha = 0.86f), DeckInk.copy(alpha = 0.55f), Color.Transparent), startX = 0f, endX = lw
                ),
                size = Size(lw, hPx)
            )
            val rw = (w - m.gpuX + 12f).dp.toPx()
            drawRect(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, DeckInk.copy(alpha = 0.55f), DeckInk.copy(alpha = 0.86f)), startX = wPx - rw, endX = wPx
                ),
                topLeft = Offset(wPx - rw, 0f), size = Size(rw, hPx)
            )
            val bh = 76.dp.toPx()
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, DeckInk.copy(alpha = 0.70f)), startY = hPx - bh, endY = hPx),
                topLeft = Offset(0f, hPx - bh), size = Size(wPx, bh)
            )
        }
    }

    // ── الشريطان المائلان من أعلى الشاشة إلى أسفلها
    if (m.railOn) DeckRails(m, cpu.level, gpu.level, tint, enter)

    // ── الصفّ العلوي
    Row(
        Modifier.align(Alignment.TopStart).fillMaxWidth()
            .padding(start = m.edge.dp, end = m.edge.dp, top = m.topY.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.deckEnter(enter, -24f, 0f, 0f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DeckCircle(m.btn.dp, homeLabel, tint, false, onHome) {
                Icon(Icons.Rounded.Home, null, tint = CubeInk, modifier = Modifier.size((m.btn * 0.5f).dp))
            }
            DeckCircle(m.btn.dp, sideLabel, tint, leftOpen, onToggleLeft) {
                Icon(Icons.Rounded.Edit, null, tint = CubeInk, modifier = Modifier.size((m.btn * 0.5f).dp))
            }
            DeckCircle(m.btn.dp, collapseLabel, tint, false, onCollapse) {
                CubeLogo(Modifier.size((m.btn * 0.54f).dp))
            }
        }
        Row(
            Modifier.weight(1f, fill = false).widthIn(max = m.appsMaxW.dp).clipToBounds().horizontalScroll(appsScroll)
                .deckEnter(enter, 24f, 0f, 0f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            (if (expanded) apps else apps.take(fit.shown)).forEach { app ->
                val bitmap = remember(app.pkg) { app.icon.asImageBitmap() }
                Box(contentAlignment = Alignment.Center) {
                    DeckCircle(m.btn.dp, app.label, tint, false, { onApp(app) }) {
                        Image(
                            bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size((m.btn * 0.64f).dp).clip(CircleShape)
                                .border(MaxSize.hairlineBorder, Color.White.copy(alpha = 0.16f), CircleShape)
                        )
                    }
                    if (launching == app.pkg) CubeSpinner(CubeInk, (m.btn * 0.5f).dp)
                }
            }
            if (fit.more) {
                DeckCircle(m.btn.dp, moreLabel, tint, expanded, { appsMore = !appsMore }) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown, null, tint = CubeInk,
                        modifier = Modifier.size((m.btn * 0.5f).dp).rotate(if (expanded) 180f else 0f)
                    )
                }
            }
            Box(Modifier.onGloballyPositioned { onAppsBounds(it.boundsInRoot()) }) {
                DeckCircle(m.btn.dp, appsEditLabel, tint, appsOpen, onAppsEdit) {
                    Icon(Icons.Rounded.Add, null, tint = CubeInk, modifier = Modifier.size((m.btn * 0.5f).dp))
                }
            }
        }
    }

    // ── اليسار: بروفايلات الحرارة
    if (leftOpen) {
        Column(
            Modifier.align(Alignment.TopStart)
                .padding(start = m.edge.dp, top = m.bodyTop.dp, bottom = 8.dp)
                .width(m.leftW.dp).fillMaxHeight()
                .swallowTaps().deckEnter(enter, -80f, 0f, 0.05f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DeckViewToggle(listView, tint, 28.dp, cdList, cdGrid, { onListView(true) }, { onListView(false) })
            SectionCaption(Icons.Rounded.Thermostat, thermalTitle)
            FadeScroll(Modifier.weight(1f), leftScroll, 6.dp) {
                if (listView) {
                    thermalOptions.forEach { option ->
                        DeckProfileCard(
                            option, option.id == thermalSelected, option.id == thermalBusy, tint, cardH,
                            onClick = { onPickThermal(option.id) }
                        )
                    }
                } else {
                    val cell = (m.leftW - 6f) / 2f
                    thermalOptions.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pair.forEach { option ->
                                DeckTileView(
                                    CubeTileSpec(
                                        icon = option.icon, text = null, label = option.label,
                                        active = option.id == thermalSelected, iconTint = option.color,
                                        onClick = { onPickThermal(option.id) }
                                    ),
                                    (cell - 10f).dp, cell.dp, tint
                                )
                            }
                        }
                    }
                }
                extra()
            }
        }
    }

    // ── عمودا CPU / GPU
    DeckGauge(
        spec = cpu, tint = tint, width = m.gaugeW.dp, tight = m.tight, outerStart = true, freqBar = !m.railOn,
        meterValue = brightness, meterAlpha = if (brightnessAllowed) 1f else 0.45f,
        meterIcon = brightnessIcon, meterLabel = brightnessLabel, onMeter = onBrightness, meterH = m.meterH.dp,
        modifier = Modifier.align(Alignment.TopStart)
            .padding(start = m.cpuX.dp, top = m.bodyTop.dp, bottom = 8.dp).fillMaxHeight()
            .deckEnter(enter, -60f, 0f, 0.1f)
    )
    DeckGauge(
        spec = gpu, tint = tint, width = m.gaugeW.dp, tight = m.tight, outerStart = false, freqBar = !m.railOn,
        meterValue = volume, meterAlpha = 1f, meterIcon = volumeIcon, meterLabel = volumeLabel, onMeter = onVolume, meterH = m.meterH.dp,
        modifier = Modifier.align(Alignment.TopStart)
            .padding(start = m.gpuX.dp, top = m.bodyTop.dp, bottom = 8.dp).fillMaxHeight()
            .deckEnter(enter, 60f, 0f, 0.1f)
    )

    // ── أسفل الوسط: الحالة وسطر الرسائل
    Column(
        Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).deckEnter(enter, 0f, 20f, 0.3f),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        val room = m.bottomRoom
        Row(
            Modifier.widthIn(max = room.dp).clip(CircleShape).background(DeckBar)
                .border(MaxSize.hairlineBorder, Color.White.copy(alpha = 0.08f), CircleShape)
                .padding(horizontal = 14.dp, vertical = 5.dp).swallowTaps(),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            FitText(
                title, CubeInk, maxSp = 12f, minSp = 9f, weight = FontWeight.Bold, align = TextAlign.Left,
                modifier = Modifier.widthIn(max = (room * 0.45f).dp)
            )
            DeckSep()
            DeckStatusBit(
                Icons.Rounded.BatteryChargingFull, batteryText,
                if (charging) ModeGreen else if (batteryLow) ModeRed else CubeDim
            )
            DeckSep()
            DeckStatusBit(Icons.Rounded.Speed, fpsText)
        }
        Box(
            Modifier.width(minOf(room, 300f).dp).height(26.dp).clip(CircleShape).background(DeckBar).swallowTaps(),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = note ?: hint, label = "status") { text ->
                Row(
                    Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (text != hint) Box(Modifier.size(6.dp).clip(CircleShape).background(tint))
                    FitText(
                        text, if (text == hint) CubeDim else CubeInk, maxSp = 11f, minSp = 8f,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }

    // ── اليمين: شبكة البلاطات
    FadeScroll(
        Modifier.align(Alignment.TopEnd)
            .padding(top = m.bodyTop.dp, end = m.edge.dp, bottom = 8.dp)
            .width(m.gridW.dp).fillMaxHeight()
            .swallowTaps().deckEnter(enter, 60f, 0f, 0.1f),
        gridScroll, 8.dp
    ) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(m.colGap.dp)) {
                row.forEach { DeckTileView(it, m.tile.dp, m.colW.dp, tint) }
            }
        }
    }
}
