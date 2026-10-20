/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.max
import nd.max.core.gamespace.CubeLayoutMath
import nd.max.core.gamespace.PanelClockMath
import nd.max.ui.design.MaxAlpha

/*
 * شريط التردد الطويل للوحة «المكعّب»: قوس «V» كالمرجع، بغمد داكن وحافّتين فضّيّتين، ومقاطع مستطيلة مائلة.
 * الهندسة كلها من [CubeLayoutMath]، والتشكيل (المقاطع والألوان) هنا وحدها، وكل ما يُحسب عن القياس في النموذج.
 */

/**
 * شريط التردد الطويل: حزام بشكل زاوية «>» (CPU) أو «<» (GPU) يتّبع حافة اللوح، لا خطٌّ مستقيم يقطع الشاشة.
 * مقاطعه تمتلئ من القاع بقدر التردد **الحيّ**، ويتوهّج مقطعه الأعلى فيُرى رأس القيمة. بلا وسم نصّيّ عائم:
 * القيمة تظهر في رقم العمود فوقه.
 * [bow] = إزاحة القمّة نحو اللعبة بالـdp (صفر ⇒ مستقيم). [level] = `null` ⇒ مسار فارغ.
 * [railOnLeft] ⇒ الشريط عند يسار الحاوية (CPU)، وإلا عند يمينها (GPU).
 */

/** انحناء الحزام عند [t] من القاع؛ الحساب كله في [CubeLayoutMath] ليُقاس ويُشارَك مع الألواح الجانبية. */
private fun railBulge(t: Float): Float = CubeLayoutMath.railBulge(t)

/** مركز الشريط أفقيًّا داخل حاويته [lane] عند نسبة الارتفاع [t]؛ القوس يتّجه نحو اللعبة. */
private fun railCenterX(t: Float, lane: Float, railW: Float, bow: Float, onLeft: Boolean): Float {
    val b = railBulge(t) * bow
    return if (onLeft) railW / 2f + b else lane - railW / 2f - b
}


/** غمد الشريط الداكن وحافّتاه الفضّيّتان الرفيعتان، والبرتقالي الذي يبدأ منه تدرّج الإضاءة من القاع. */
private val RailSleeve = Color(0xF2080A0D)
private val RailHairline = Color(0x99B8BFCC)
private val RailEmber = Color(0xFFE08A3C)
/** سُمك الغمد وحافّته ومسافة الفجوة بين المقاطع، نسبةً إلى سُمك الشريط أو خطوته كما في المرجع. */
private const val RAIL_SLEEVE_RATIO = 0.16f
private const val RAIL_HAIR_RATIO = 0.035f
private const val RAIL_GAP_RATIO = 0.14f

/**
 * يرسم القوس كالمرجع: غمد داكن محاط بحافّتين فضّيّتين رفيعتين، وداخله مقاطع مستطيلة مائلة على امتداد
 * الساق، مضاءة من القاع بتدرّج الوضع، ومقطع الرأس متوهّج. «أعلى قيمة حديثة» تُرى مقطعًا باهتًا فوق المضاء.
 */
private fun DrawScope.curvedRail(
    level: Float, peak: Float, tint: Color, railWpx: Float, bowPx: Float, onLeft: Boolean, segments: Int
) {
    val h = size.height
    if (h <= 0f) return
    val lane = size.width
    // القوس ساقان مستقيمتان تلتقيان عند القمّة: تكفي ثلاث نقاط (القاع، القمّة، الطرف العلوي).
    val knots = floatArrayOf(0f, CubeLayoutMath.RAIL_APEX, 1f)
    val spine = Path()
    for ((i, t) in knots.withIndex()) {
        val x = railCenterX(t, lane, railWpx, bowPx, onLeft)
        val y = h * (1f - t)
        if (i == 0) spine.moveTo(x, y) else spine.lineTo(x, y)
    }
    // الإطار: حافّة خارجية بخطّ أوسع من الغمد فيظهر خطّاها الفضّيّان على جانبيه، ثم الغمد الداكن فوقها.
    val sleeve = railWpx * RAIL_SLEEVE_RATIO
    val hair = max(1.dp.toPx(), railWpx * RAIL_HAIR_RATIO)
    drawPath(
        spine, RailHairline,
        style = Stroke(width = railWpx + 2f * (sleeve + hair), cap = StrokeCap.Butt, join = StrokeJoin.Round)
    )
    drawPath(
        spine, RailSleeve,
        style = Stroke(width = railWpx + 2f * sleeve, cap = StrokeCap.Butt, join = StrokeJoin.Round)
    )

    val n = segments.coerceAtLeast(2)
    val pitch = h / n
    val seg = pitch * (1f - RAIL_GAP_RATIO)
    val lit = level.coerceIn(0f, 1f) * n
    val head = if (lit > 0f) kotlin.math.ceil(lit).toInt() - 1 else -1
    val dark = lerp(lerp(tint, RailEmber, 0.30f), Color.Black, 0.22f)
    val bright = lerp(tint, Color.White, 0.45f)
    val off = Color.White.copy(alpha = 0.10f)
    val peakIdx = if (peak > 0.05f) (peak.coerceIn(0f, 1f) * n).toInt().coerceIn(0, n - 1) else -1
    val corner = CornerRadius(railWpx * 0.08f)
    for (k in 0 until n) {
        val mid = h - (k + 0.5f) * pitch
        val t = (h - mid) / h
        val x = railCenterX(t, lane, railWpx, bowPx, onLeft)
        // x(t) = c ± bow·bulge(t) و dt/dy = −1/h ⇒ ميل ثابت على كل ساق، فيُدار المقطع بزاوية ساقه.
        // ميل كل ساق من مشتقّة الانحناء نفسها (ساق سفلى أحدّ من العليا).
        val slope = (if (onLeft) -1.0 else 1.0) * bowPx * CubeLayoutMath.railBulgeSlope(t).toDouble() / h
        val angle = (-atan(slope) * 180.0 / PI).toFloat()
        // طول المقطع على امتداد الساق: خطوته الرأسية مقسومة على جيب تمام الميل، فتبقى الفجوات متساوية.
        val along = seg * kotlin.math.sqrt(1f + (slope * slope).toFloat())
        val on = lerp(dark, bright, k / (n - 1f))
        val f = (lit - k).coerceIn(0f, 1f)
        val base = lerp(off, on, f)
        val color = if (k == peakIdx && lit <= k) bright.copy(alpha = 0.55f) else base
        rotate(angle, pivot = Offset(x, mid)) {
            if (f > 0f) {
                // هالة خفيفة لكل مقطع مضاء: الضوء يمتدّ قليلًا خارج المقطع دون أي blur.
                val pad = 3.5f.dp.toPx()
                drawRoundRect(
                    tint.copy(alpha = MaxAlpha.edgeLight * f),
                    Offset(x - railWpx / 2f - pad, mid - along / 2f - pad),
                    Size(railWpx + 2f * pad, along + 2f * pad),
                    CornerRadius(corner.x + pad)
                )
            }
            if (k == head) {
                // وهج الرأس بطبقتين من لون الوضع وبشفافيتين معتمدتين: مقطع حيّ يضيء لا مطليّ فقط.
                val outer = 6.dp.toPx()
                val inner = 2.dp.toPx()
                drawRoundRect(
                    tint.copy(alpha = MaxAlpha.edgeLight),
                    Offset(x - railWpx / 2f - outer, mid - along / 2f - outer),
                    Size(railWpx + 2f * outer, along + 2f * outer),
                    CornerRadius(corner.x + outer)
                )
                drawRoundRect(
                    tint.copy(alpha = 0.55f),
                    Offset(x - railWpx / 2f - inner, mid - along / 2f - inner),
                    Size(railWpx + 2f * inner, along + 2f * inner),
                    CornerRadius(corner.x + inner)
                )
            }
            drawRoundRect(color, Offset(x - railWpx / 2f, mid - along / 2f), Size(railWpx, along), corner)
            if (f > 0f) {
                // خطّ لمعان رفيع في أعلى كل مقطع مضاء: يعطيه حجمًا زجاجيًّا بدل لون مسطّح.
                drawRoundRect(
                    Color.White.copy(alpha = 0.34f * f),
                    Offset(x - railWpx * 0.38f, mid - along * 0.40f), Size(railWpx * 0.76f, along * 0.2f), corner
                )
            }
        }
    }
}

@Composable
internal fun FrequencyRail(
    level: Float?, tint: Color, railW: Dp, bow: Dp,
    railOnLeft: Boolean, description: String, segments: Int = PanelClockMath.SEGMENTS, modifier: Modifier = Modifier
) {
    val shown by animateFloatAsState(level ?: 0f, tween(450), label = "rail")
    val peak = rememberPeak(level)
    // الشريط وحده بلا وسم: الرقم موجود فوق العمود نفسه، والوسم كان يكرّره ويخرج عن حدود اللوح.
    Canvas(modifier.semantics { contentDescription = description }.swallowTaps()) {
        curvedRail(if (level != null) shown.coerceIn(1f / segments, 1f) else 0f, peak, tint, railW.toPx(), bow.toPx(), railOnLeft, segments)
    }
}
