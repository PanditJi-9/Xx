/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.ui.component

import android.content.Context
import android.os.PowerManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import nd.max.ui.design.MaxDuration
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** دورة المشهد كاملة. كل حركة فيه مضاعَف صحيح لهذه الدورة، فلا «قفزة» عند إعادتها. */
private const val OrbitPeriodMillis = 14_000L

/**
 * ~٦٠ إطارًا في الثانية (كانت ٣٠). على شاشة ١٢٠Hz كان نصف المعدّل يُرى «تقطّعًا» في حركة المذنّب،
 * وهو أول ما تلتقطه العين في مشهد يُراد له أن يبدو فاخرًا. **الكلفة على البطارية لم تُقَس هنا** — تحتاج
 * جهازًا؛ وللرجوع إلى النصف يكفي `33_000_000L` دون أي تغيير آخر.
 */
private const val OrbitFrameNanos = 16_000_000L

/** وضعية ثابتة مرتّبة تُرسم حين تُوقَف الحركة، فتبدو لقطةً مقصودة لا مشهدًا متجمّدًا في منتصفه. */
private const val OrbitStaticPhase = 0.18f

private const val OrbitStarCount = 22
private const val OrbitTrailSegments = 22
private const val OrbitSatelliteSegments = 9

/** شرائح قوس الشحن: كلما كثرت، تدرّج الذيل بنعومة أكبر (كانت ٢٤ فظهرت حزوزٌ في التدرّج). */
private const val OrbitArcSegments = 56

/** عدد مرّات جريان الطاقة في الدورة: مضاعف صحيح ← الدورة تُغلق بلا قفزة. ١٤s ÷ ٤ = ٣٫٥s للمرّة. */
private const val OrbitFlowPasses = 4

/** أُسّ تسارع الجريان: > ١ ← تبدأ الطاقة بطيئة وتتسارع نحو رأس القوس فتُحسّ «تصبّ فيه». */
private const val OrbitFlowEase = 1.35f

/** خرزات تدريج المدار: كل ٥٪ خرزة، وكل ٢٥٪ أكبر. تظهر في الجزء **غير المشحون** فقط. */
private const val OrbitTickCount = 20

/** أقصى طول لذيل نبضة الطاقة (راديان على المدار). */
private const val OrbitTailSpan = 1.1f

private const val TwoPi = (2.0 * PI).toFloat()
private const val HalfPi = (PI / 2.0).toFloat()
private const val DegToRad = (PI / 180.0).toFloat()

/**
 * «مدار الطاقة»: نواة زجاجية مضيئة عليها صاعقة، ومداران مائلان بعمق، ونبضات طاقة تجري **على قوس
 * الشحن نفسه** وتصبّ في رأسه، وقمرٌ بذيل، ونجوم بثلاث طبقات تومض. **المدار الأكبر هو عدّاد الشحن**:
 * قوسه المضيء يمتدّ بنسبة [fraction]، ونبضات الطاقة لا تدور في فراغ بل تسير من بداية القوس إلى رأسه
 * ثم تنفجر عنده لمعةً — فالحركة نفسها تقول «تدخل طاقة، والمستوى هنا».
 *
 * **ما أُصلح في هذه النسخة (من لقطة الشاشة):**
 *  • المدار الأكبر كان نصف قطره ٠٫٣٤ من العرض، فيلامس حافة البطاقة البعيدة (نحو ١٠dp) ويقترب من
 *    الرقم عند «١٠٠٪» على الشاشات الصغيرة. صار ٠٫٣٠، فيبقى نحو ٢٤–٢٧dp عن الحافتين، ونحو ٢٢dp عن
 *    الرقم حتى على ٣٦٠dp (محسوب على الشكل المائل للمدار). وكل مدار ما زال **يُقاس بصندوقه بعد
 *    الإمالة** ويُصغَّر إن لزم، فلا يخرج من البطاقة على أي عرض.
 *  • قوس الشحن كان يظهر بحزوز (٢٤ شريحة) ويبدأ مكتملًا فجأة: صار ٥٦ شريحة، ويَنمو من الصفر إلى
 *    النسبة بحركة قصيرة ([MaxDuration.deliberate]) ويتبع كل تغيّر في النسبة بدل أن يقفز.
 *  • المذنّب كان يجري على المدار كله بلا علاقة بالنسبة؛ صار نبضتان تجريان على الجزء المشحون وحده.
 *  • النواة كانت كرة بيضاء مسطّحة: صارت كرة مظلَّلة بإضاءة وحافّة ولمعة وحلقات تتّسع منها وصاعقة.
 *  • العمق: الجهة البعيدة من المدارين أخفت وأرقّ، والقريبة أسطع، فيُقرأ المدار حلقةً مائلة لا إهليجًا مسطّحًا.
 *
 * **مرسومٌ بالكامل، بلا صور ولا موارد:** لا ملفّ يُحمَّل ولا ترجمة تُضاف، ويتلوّن بلونَي الشاشة.
 *
 * **كيف يبقى رخيصًا (الحركة المستمرّة هنا هي الاستثناء الوحيد الذي يستحقّ حارسًا):**
 *  • **لا يدور إلا ما يُرى.** الحلقة تقف حين يخرج المشهد من الشاشة (تمرير) وتستأنف من حيث وقفت؛
 *    خارج الشاشة كلفته صفر لا «صغيرة».
 *  • **يقف مع «توفير الطاقة»** وإيقاف الحركة في النظام ([rememberAnimationsEnabled])، ويُرسم ثابتًا.
 *  • **كله في مرحلة الرسم**: الطور وحركة المستوى حالاتٌ تُقرأ داخل `Canvas` فقط، فلا إعادة تركيب ولا
 *    قياس في أي إطار.
 *  • **بلا تخصيص في الإطار الواحد قدر الإمكان**: مسار الصاعقة يُبنى مرة واحدة، وأقلام الرسم تُنشأ
 *    بعدد ثابت لا بعدد الشرائح.
 *
 * @param fraction نسبة الشحن ٠..١ — طول القوس المضيء على المدار.
 * @param flowing جاري ضخّ الطاقة؟ يُجري النبضات ويُطلق حلقات النواة ويُظهر الصاعقة؛ وإلا يهدأ المشهد.
 * @param centerX موضع النواة أفقيًّا كنسبة من العرض، لتترك جهة النص فارغة.
 * @param starTint لون النجوم: أبيض على الداكن، ولون الحبر على الفاتح (الأبيض لا يُرى على سطح فاتح).
 */
@Composable
fun MaxOrbitScene(
    fraction: Float,
    accent: Color,
    accentAlt: Color,
    modifier: Modifier = Modifier,
    flowing: Boolean = true,
    centerX: Float = 0.64f,
    starTint: Color = Color.White,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val context = LocalContext.current
    val powerSave = remember(context) {
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
    }
    val screenHeightPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }
    var onScreen by remember { mutableStateOf(true) }
    val phase = remember { mutableFloatStateOf(OrbitStaticPhase) }
    val running = animationsEnabled && !powerSave && onScreen

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last == 0L) {
                    last = now
                } else if (now - last >= OrbitFrameNanos) {
                    val advance = (now - last) / (OrbitPeriodMillis * 1_000_000f)
                    phase.floatValue = (phase.floatValue + advance) % 1f
                    last = now
                }
            }
        }
    }

    // المستوى المعروض يتبع المستوى الحقيقي بحركة قصيرة (≤ ٣٦٠ms: سقف الحركة في نظام التصميم)،
    // والنمو الأول من الصفر هو «دخول» القوس حين يُوصَل الشاحن.
    val level = fraction.coerceIn(0f, 1f)
    val shownLevel = remember { Animatable(if (animationsEnabled) 0f else level) }
    LaunchedEffect(level, animationsEnabled) {
        if (animationsEnabled) {
            shownLevel.animateTo(level, tween(MaxDuration.deliberate, easing = MaxEase.EmphasizedDecelerate))
        } else {
            shownLevel.snapTo(level)
        }
    }
    // «إشعال» النواة مرّة واحدة عند الظهور: تكبر من ٥٥٪ وتسطع وتُظهر ما حولها.
    val ignite = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(animationsEnabled) {
        if (animationsEnabled) {
            ignite.animateTo(1f, tween(MaxDuration.deliberate, easing = MaxEase.EmphasizedDecelerate))
        } else {
            ignite.snapTo(1f)
        }
    }

    val bolt = remember { boltPath() }

    Canvas(
        modifier = modifier.onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInWindow()
            val visible = bounds.bottom > 0f && bounds.top < screenHeightPx
            if (visible != onScreen) onScreen = visible
        }
    ) {
        val t = phase.floatValue
        val lv = shownLevel.value
        val ig = ignite.value
        val center = Offset(size.width * centerX, size.height * 0.5f)
        val margin = 12.dp.toPx()

        // المداران يُقاسان بصندوقهما بعد الإمالة ويُصغَّران لو خرجا من البطاقة.
        // ٠٫٣٠ لا ٠٫٣٤ من العرض: انظر وصف الصنف عن المسافة من الحافة والرقم.
        val orbitA = fitOrbit(center, size.width * 0.30f, size.height * 0.24f, -12f, margin)
        val orbitB = fitOrbit(center, size.width * 0.16f, size.height * 0.40f, 18f, margin)

        drawStars(t, textOnLeft = centerX >= 0.5f, tint = starTint, reveal = ig)

        drawOrbitRing(orbitA, accent.copy(alpha = 0.24f * ig), 1.2.dp.toPx())
        drawOrbitRing(orbitB, accentAlt.copy(alpha = 0.18f * ig), 1.0.dp.toPx())
        drawTicks(orbitA, lv, accent, ig)

        // نبضتا الطاقة بينهما نصف مرّة: واحدة تصل الرأس بينما الأخرى في منتصف الطريق.
        val uA = flowFraction(t, 0f)
        val uB = flowFraction(t, 0.5f)
        drawChargeArc(orbitA, lv, accent, 2.4.dp.toPx(), flowing, uA.pow(OrbitFlowEase), uB.pow(OrbitFlowEase))
        drawArcHead(orbitA, lv, t, accent, flowing, uA, uB)

        val pulses = flowing && lv > 0.02f
        val sinA = sin(pulseHeadAngle(lv, uA))
        val sinB = sin(pulseHeadAngle(lv, uB))
        // النصف العلوي من الإهليج هو الجهة «البعيدة»: ما يمرّ فيه يُرسم خلف النواة لا أمامها.
        if (pulses) {
            if (sinA < 0f) drawFlowPulse(orbitA, lv, uA, accent)
            if (sinB < 0f) drawFlowPulse(orbitA, lv, uB, accent)
        }
        val satAngle = -TwoPi * 2f * t
        val satBehind = sin(satAngle) < 0f
        if (satBehind) drawSatellite(orbitB, satAngle, accentAlt)
        drawCore(center, t, accent, accentAlt, flowing, ig, bolt)
        if (!satBehind) drawSatellite(orbitB, satAngle, accentAlt)
        if (pulses) {
            if (sinA >= 0f) drawFlowPulse(orbitA, lv, uA, accent)
            if (sinB >= 0f) drawFlowPulse(orbitA, lv, uB, accent)
        }
    }
}

private class Orbit(val center: Offset, val rx: Float, val ry: Float, val tilt: Float)

/** نقطة على مدار مائل عند الزاوية البارامترية [angle]. */
private fun Orbit.pointAt(angle: Float): Offset {
    val x = rx * cos(angle)
    val y = ry * sin(angle)
    val tiltRad = tilt * DegToRad
    val ct = cos(tiltRad)
    val st = sin(tiltRad)
    return Offset(center.x + x * ct - y * st, center.y + x * st + y * ct)
}

/**
 * مدار يدخل البطاقة بكامله. نصفا الصندوق بعد الإمالة: `hx = √(rx²cos²+ry²sin²)` و`hy = √(rx²sin²+ry²cos²)`،
 * والمعامل واحد لكلا المحورين فتبقى النسبة بين القطرين (شكل الإهليج) كما صُمّمت.
 */
private fun DrawScope.fitOrbit(center: Offset, rx: Float, ry: Float, tilt: Float, margin: Float): Orbit {
    val rad = tilt * DegToRad
    val s = sin(rad)
    val c = cos(rad)
    val hx = sqrt(rx * rx * c * c + ry * ry * s * s)
    val hy = sqrt(rx * rx * s * s + ry * ry * c * c)
    val roomX = min(center.x, size.width - center.x) - margin
    val roomY = min(center.y, size.height - center.y) - margin
    val k = min(1f, min(roomX / hx, roomY / hy)).coerceAtLeast(0.2f)
    return Orbit(center, rx * k, ry * k, tilt)
}

/** مرحلة نبضة الطاقة ٠..١ — مضاعف صحيح لدورة المشهد، فلا قفزة عند إعادتها. */
private fun flowFraction(t: Float, offset: Float): Float = (t * OrbitFlowPasses + offset) % 1f

/** زاوية رأس النبضة على المدار: من أعلى المدار (بداية القوس) إلى رأس القوس، متسارعة نحوه. */
private fun pulseHeadAngle(level: Float, u: Float): Float =
    -HalfPi + TwoPi * level * u.pow(OrbitFlowEase)

/** غلاف «وصول» النبضة إلى الرأس: يصعد ثم يهبط في آخر ١٠٪ من مسيرتها. */
private fun arrivalEnvelope(u: Float): Float {
    val a = ((u - 0.90f) / 0.10f).coerceIn(0f, 1f)
    return 4f * a * (1f - a)
}

/** صاعقة بإحداثيات وحدة (≈ ±٠٫٥٢)، تُبنى مرة وتُرسم بإزاحة وتحجيم. */
private fun boltPath(): Path = Path().apply {
    moveTo(0.12f, -0.52f)
    lineTo(-0.30f, 0.06f)
    lineTo(-0.03f, 0.06f)
    lineTo(-0.12f, 0.52f)
    lineTo(0.30f, -0.12f)
    lineTo(0.03f, -0.12f)
    close()
}

/**
 * نجوم بثلاث طبقات عمق: الأبعد أصغر وأخفت وأقلّ حركة، والأقرب أكبر وتومض بشرارة صليبية عند ذروتها.
 * وكل حركة مضاعف صحيح لدورة المشهد.
 */
private fun DrawScope.drawStars(t: Float, textOnLeft: Boolean, tint: Color, reveal: Float) {
    for (i in 0 until OrbitStarCount) {
        val baseX = ((i * 0.6180339f) % 1f) * size.width
        val baseY = ((i * 0.3819660f + 0.17f) % 1f) * size.height
        // النجوم لا تقع خلف الرقم والعنوان: ما يسقط في جهة النص يُنقل إلى الجهة المقابلة.
        val inTextZone = baseY > size.height * 0.5f &&
            (if (textOnLeft) baseX < size.width * 0.45f else baseX > size.width * 0.55f)
        val x0 = if (inTextZone) size.width - baseX else baseX
        val layer = i % 3
        val reach = (0.8f + layer * 0.9f).dp.toPx()
        val x = x0 + sin(TwoPi * (t + i * 0.137f)) * reach
        val y = baseY + cos(TwoPi * (t + i * 0.291f)) * reach * 0.6f
        val harmonic = 1 + (i % 3)
        val twinkle = 0.5f + 0.5f * sin(TwoPi * (t * harmonic + i / OrbitStarCount.toFloat()))
        val alpha = (0.10f + 0.50f * twinkle) * (0.60f + 0.20f * layer) * reveal
        drawCircle(
            color = tint.copy(alpha = alpha),
            radius = (0.6f + layer * 0.45f).dp.toPx(),
            center = Offset(x, y)
        )
        if (layer == 2 && twinkle > 0.9f) {
            val len = 3.2.dp.toPx() * ((twinkle - 0.9f) * 10f)
            val c = tint.copy(alpha = 0.55f * reveal)
            drawLine(c, Offset(x - len, y), Offset(x + len, y), strokeWidth = 0.8.dp.toPx())
            drawLine(c, Offset(x, y - len), Offset(x, y + len), strokeWidth = 0.8.dp.toPx())
        }
    }
}

/**
 * حلقة المدار نصفين: الجهة البعيدة (النصف العلوي) أخفت، والقريبة بكامل لونها — فيُقرأ المدار
 * مائلًا في العمق. (الزاوية ١٨٠..٣٦٠ في إحداثيات الشاشة هي النصف العلوي.)
 */
private fun DrawScope.drawOrbitRing(orbit: Orbit, color: Color, strokeWidth: Float) {
    val style = Stroke(width = strokeWidth)
    val topLeft = Offset(orbit.center.x - orbit.rx, orbit.center.y - orbit.ry)
    val arcSize = Size(orbit.rx * 2f, orbit.ry * 2f)
    rotate(degrees = orbit.tilt, pivot = orbit.center) {
        drawArc(color.copy(alpha = color.alpha * 0.50f), 180f, 180f, false, topLeft, arcSize, style = style)
        drawArc(color, 0f, 180f, false, topLeft, arcSize, style = style)
    }
}

/** خرزات تدريج على الجزء **غير المشحون** من المدار: «ما بقي» يُرى كما يُرى ما امتلأ. */
private fun DrawScope.drawTicks(orbit: Orbit, level: Float, color: Color, reveal: Float) {
    for (i in 1 until OrbitTickCount) {
        val f = i / OrbitTickCount.toFloat()
        if (f <= level + 0.02f) continue
        val major = i % 5 == 0
        drawCircle(
            color = color.copy(alpha = (if (major) 0.34f else 0.16f) * reveal),
            radius = (if (major) 1.7f else 1.0f).dp.toPx(),
            center = orbit.pointAt(-HalfPi + TwoPi * f)
        )
    }
}

/**
 * قوس الشحن: الجزء المضيء من المدار، يبدأ من أعلاه ويمتدّ بنسبة الشحن.
 *
 * **ذيلٌ خافت ورأسٌ ساطع:** شرائح قصيرة تقلّ شفافيتها نحو البداية فيُقرأ اتجاه التقدّم. وتحت الشرائح
 * وهجٌ عريض خافت من ضربة واحدة. ولمعتان تتبعان النبضتين فيبدو الضوء **جاريًا** داخل القوس.
 * والجهة القريبة من العين أسطع من البعيدة (عمق).
 */
private fun DrawScope.drawChargeArc(
    orbit: Orbit,
    level: Float,
    color: Color,
    strokeWidth: Float,
    flowing: Boolean,
    posA: Float,
    posB: Float,
) {
    if (level <= 0.005f) return
    val step = 360f * level / OrbitArcSegments
    val topLeft = Offset(orbit.center.x - orbit.rx, orbit.center.y - orbit.ry)
    val arcSize = Size(orbit.rx * 2f, orbit.ry * 2f)
    val butt = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
    val round = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    rotate(degrees = orbit.tilt, pivot = orbit.center) {
        drawArc(
            color = color.copy(alpha = 0.08f),
            startAngle = -90f,
            sweepAngle = 360f * level,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth * 3.2f, cap = StrokeCap.Round)
        )
        for (i in 0 until OrbitArcSegments) {
            val last = i == OrbitArcSegments - 1
            val pos = (i + 0.5f) / OrbitArcSegments
            val angle = (-90f + step * (i + 0.5f)) * DegToRad
            val depth = 0.5f + 0.5f * sin(angle)
            var glint = 0f
            if (flowing) {
                val da = (pos - posA) / 0.09f
                val db = (pos - posB) / 0.09f
                glint = max(exp(-da * da), exp(-db * db))
            }
            val alpha = ((0.14f + 0.52f * pos) + 0.34f * glint) * (0.66f + 0.34f * depth)
            drawArc(
                color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
                startAngle = -90f + step * i,
                // ٠٫٦ درجة فوق كل شريحة تُخفي الفاصل بينها دون تراكب ظاهر في الشفافية.
                sweepAngle = if (last) step else step + 0.6f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = if (last) round else butt
            )
        }
    }
}

/** رأس القوس: عقدة تتنفّس تدلّ على المستوى بدقّة، وتلمع لمعةً حين تصلها نبضة. */
private fun DrawScope.drawArcHead(
    orbit: Orbit,
    level: Float,
    t: Float,
    color: Color,
    flowing: Boolean,
    uA: Float,
    uB: Float,
) {
    if (level <= 0.005f) return
    val head = orbit.pointAt(-HalfPi + TwoPi * level)
    val breathe = 0.5f + 0.5f * sin(TwoPi * t * 3f)
    drawCircle(color.copy(alpha = 0.14f + 0.10f * breathe), 9.dp.toPx(), head)
    drawCircle(color.copy(alpha = 0.42f), 4.6.dp.toPx(), head)
    drawCircle(lerp(color, Color.White, 0.85f), 2.2.dp.toPx(), head)
    if (flowing) {
        val burst = max(arrivalEnvelope(uA), arrivalEnvelope(uB))
        if (burst > 0.01f) {
            drawCircle(color.copy(alpha = 0.40f * burst), (6f + 12f * burst).dp.toPx(), head)
        }
    }
}

/**
 * نبضة طاقة: رأس ساطع بذيل متّصل يتناقص سماكةً وشفافيةً، تجري من بداية القوس إلى رأسه ثم تذوب في لمعته.
 * الذيل لا يخرج من القوس أبدًا (يُقصّ عند بدايته)، وتظهر النبضة وتختفي بتدرّج فلا «تنبثق».
 */
private fun DrawScope.drawFlowPulse(orbit: Orbit, level: Float, u: Float, color: Color) {
    val headRad = pulseHeadAngle(level, u)
    val depth = 0.72f + 0.28f * (0.5f + 0.5f * sin(headRad))
    val life = (u / 0.08f).coerceIn(0f, 1f) * (1f - ((u - 0.94f) / 0.06f).coerceIn(0f, 1f)) * depth
    if (life <= 0.01f) return
    val span = min(OrbitTailSpan, headRad + HalfPi)
    if (span > 0.02f) {
        val lastIndex = (OrbitTrailSegments - 1).toFloat()
        for (i in OrbitTrailSegments - 1 downTo 1) {
            val far = i / lastIndex
            val near = (i - 1) / lastIndex
            val fade = 1f - (far + near) * 0.5f
            drawLine(
                color = color.copy(alpha = 0.85f * fade * fade * life),
                start = orbit.pointAt(headRad - far * span),
                end = orbit.pointAt(headRad - near * span),
                strokeWidth = (1.2f + 3.2f * fade).dp.toPx() * depth,
                cap = StrokeCap.Butt,
            )
        }
    }
    val head = orbit.pointAt(headRad)
    drawCircle(color.copy(alpha = 0.28f * life), 7.dp.toPx() * depth, head)
    drawCircle(lerp(color, Color.White, 0.85f).copy(alpha = 0.95f * life), 2.4.dp.toPx() * depth, head)
}

/**
 * النواة: كرة مظلَّلة (إضاءة من أعلى اليسار، وجهٌ مظلم بعيد عن الضوء، حافّة لامعة، ولمعة)،
 * بهالتين، وحلقتان تتّسعان منها وتخفتان حين يجري الشحن، وصاعقة تتنفّس في وسطها.
 * وتطفو ببطء وتتنفّس بمضاعفٍ مختلف عن الطفو فلا يتزامنان فيبدوان آليَّين.
 */
private fun DrawScope.drawCore(
    center: Offset,
    t: Float,
    color: Color,
    glow: Color,
    flowing: Boolean,
    ignite: Float,
    bolt: Path,
) {
    val bob = sin(TwoPi * t * 2f) * 1.5.dp.toPx()
    val c = Offset(center.x, center.y + bob)
    val radius = min(size.width, size.height) * 0.15f * (0.55f + 0.45f * ignite)
    val breathe = 0.5f + 0.5f * sin(TwoPi * t * 3f)

    val haloAlpha = (if (flowing) 0.34f + 0.12f * breathe else 0.20f) * ignite
    val outerRadius = radius * 4.2f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glow.copy(alpha = haloAlpha * 0.55f), Color.Transparent),
            center = c,
            radius = outerRadius
        ),
        radius = outerRadius,
        center = c
    )
    val haloRadius = radius * 3.2f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = haloAlpha), Color.Transparent),
            center = c,
            radius = haloRadius
        ),
        radius = haloRadius,
        center = c
    )

    if (flowing) {
        val ripple = Stroke(width = 1.2.dp.toPx())
        for (k in 0 until 2) {
            val r = (t * 3f + k * 0.5f) % 1f
            drawCircle(
                color = color.copy(alpha = (1f - r) * (1f - r) * 0.30f * ignite),
                radius = radius * (1.2f + 2.4f * r),
                center = c,
                style = ripple
            )
        }
    }

    val deep = lerp(color, Color.Black, 0.42f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.95f), color, deep),
            center = Offset(c.x - radius * 0.25f, c.y - radius * 0.30f),
            radius = radius * 1.5f
        ),
        radius = radius,
        center = c
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.16f),
        radius = radius,
        center = c,
        style = Stroke(width = 1.dp.toPx())
    )
    val specCenter = Offset(c.x - radius * 0.36f, c.y - radius * 0.42f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.60f), Color.Transparent),
            center = specCenter,
            radius = radius * 0.30f
        ),
        radius = radius * 0.30f,
        center = specCenter
    )

    if (flowing) {
        withTransform({
            translate(c.x, c.y)
            scale(radius * 0.95f, radius * 0.95f, pivot = Offset.Zero)
        }) {
            drawPath(
                path = bolt,
                color = lerp(color, Color.Black, 0.60f).copy(alpha = (0.55f + 0.20f * breathe) * ignite)
            )
        }
    }
}

/** القمر على المدار الثاني: بذيل قصير، ويكبر ويسطع في الجهة القريبة ويصغر ويخفت في البعيدة. */
private fun DrawScope.drawSatellite(orbit: Orbit, angle: Float, color: Color) {
    val near = 0.5f + 0.5f * sin(angle)
    val depth = 0.55f + 0.45f * near
    val scale = 0.78f + 0.32f * near
    // يسير بزاوية متناقصة، فذيله خلفه في الاتجاه المعاكس (زاوية أكبر).
    val segs = OrbitSatelliteSegments.toFloat()
    for (i in OrbitSatelliteSegments downTo 1) {
        val far = i / segs
        val nearK = (i - 1) / segs
        val fade = 1f - (far + nearK) * 0.5f
        drawLine(
            color = color.copy(alpha = 0.50f * fade * fade * depth),
            start = orbit.pointAt(angle + far * 0.7f),
            end = orbit.pointAt(angle + nearK * 0.7f),
            strokeWidth = (0.8f + 1.6f * fade).dp.toPx(),
            cap = StrokeCap.Butt,
        )
    }
    val p = orbit.pointAt(angle)
    drawCircle(color = color.copy(alpha = 0.30f * depth), radius = 5.dp.toPx() * scale, center = p)
    drawCircle(color = color.copy(alpha = max(depth, 0.7f)), radius = 2.2.dp.toPx() * scale, center = p)
}
