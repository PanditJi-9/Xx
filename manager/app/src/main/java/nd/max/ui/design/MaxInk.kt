/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * «الحبر» — لغة الرسم الواحدة في MaxManager.
 *
 * ## من أين جاءت
 *
 * ملاحظة التقييم طلبت لمسة شخصية فريدة، وقال المالك إن الوسم وبطاقة «من الصانع» بداية لا تكفي:
 * «أضف تفاصيل مميزة في التفاعلات والرسوم والحالات الحية … اجعله يشعر بأن كل حركة ومعلومة واستجابة
 * لها غرض واضح، وأن MaxManager له أسلوبه الخاص». فهذا الملف هو **الأسلوب نفسه**: القلم الذي كُتب به
 * وسم الصانع ([nd.max.ui.component.MaxSignatureMark]) يصير لغةً تُرسم بها حالات الشاشة.
 *
 * ## وكل رسمة هنا لها غرض واحد مكتوب — لا زخرفة
 *
 * | الرسمة | تُستعمل في | الغرض الذي تحمله |
 * | --- | --- | --- |
 * | [MaxInkGlyph.NotFound] | الحالة الفارغة ([nd.max.ui.component.MaxEmptyState]) | «بحثتُ فلم أجد» — الرسمة **هي** الحالة، بدل أيقونة عامّة |
 * | [MaxInkTick] | حبّة الثقة الحيّة ([MaxTrustChip]) | «وصلت عيّنة جديدة» — تُرسم **فقط** حين تتغيّر القراءة، فالحركة هي إشارة الحداثة |
 * | [MaxInkLoop] | تفعيل الترخيص، والتحميل المدمج | «هناك عمل يجري الآن» — تعيش ما دام العمل جاريًا وتموت بموته |
 *
 * وقاعدتان تمنعان الزخرفة من التسلّل: **لا شيء يتحرّك حين لا يحدث شيء** (لا حركة خاملة ولا لمعان
 * دوريّ)، و**لا تفاعل مزيّف** (الرسمة ليست زرًّا ولا تُنقر). وحالات الخطأ تُركت بأيقوناتها الجادّة:
 * موضع العطب ليس موضع لمسة شخصية.
 *
 * ## ومن أوقف الحركات في نظامه
 *
 * كل كشف هنا يمرّ بـ[rememberAnimationsEnabled]: يرى الرسمة **كاملة فورًا** — «بلا حركة» ليست
 * «حركة أسرع». والانتظار الجاري ([MaxInkLoop]) يقف عند رسمة ثابتة، والنصّ المجاور («جارٍ التفعيل…»)
 * يحمل المعنى وحده.
 *
 * ## الهندسة
 *
 * كل رسمة أرقامها بوحدات إطار مرجعيّ (viewBox) لا بكسل: القلم نفسه بوحدات الإطار
 * ([MaxInkDrawing.strokeWidth]) فيتناسب مع الحجم، ويرافق ارتفاع الرسمة حين تُمطّ عرضًا
 * ([MaxInkDrawingCanvas] مع `stretch`). والمسارات **مؤلفة يدويًّا** لا مولَّدة من أداة رسم،
 * ولا تدخل المستودع صورةٌ من مولّد صور (`ADR-73`).
 *
 * وأرقام **الوسم** وحدها هي المصدر المقيس بـ`tools/makers_mark.py`؛ رسوم هذا الملف رسوم تطبيق
 * داخلية لا تعبر إلى ملفّات الصفحة، فلا بوابة تطابق لها (وهي مذكورة هنا صراحةً كي لا يُظنّ أنها
 * مقيسة بمثل ما يُقاس الوسم).
 */
package nd.max.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import nd.max.ui.component.rememberAnimationsEnabled

/**
 * رسمة حبر واحدة: إطارها المرجعيّ، وسُمك قلمها بوحدات الإطار، وخطوطها.
 *
 * الخطّ الواحد `FloatArray`: نقطة بداية ثم قطع `cubicTo` (c1x, c1y, c2x, c2y, x, y) — الترميز
 * نفسه الذي يقرأه وسم الصانع، فلا يُتعلَّم تمثيلان لشيء واحد.
 */
@Immutable
private class MaxInkDrawing(
    val viewBoxWidth: Float,
    val viewBoxHeight: Float,
    val strokeWidth: Float,
    val strokes: List<FloatArray>,
)

/**
 * «بحثتُ فلم أجد»: عدسة تُرسم بقلم واحد — الدائرة ثم المقبض في خطّ متّصل — ثم شرطة تُشطبها.
 *
 * خطّان لا خطّ واحد، لأن الشطب فعلٌ **بعد** الرسم: يُرسمان بالتسلسل (الدائرة كاملة ثم الشطب)
 * فيقرأ الناظر «بحث … ثم لا نتيجة» لا «شكل هندسي».
 */
private val INK_NOT_FOUND = MaxInkDrawing(
    viewBoxWidth = 100f,
    viewBoxHeight = 100f,
    strokeWidth = 7f,
    strokes = listOf(
        // العدسة: من أسفل اليمين (٤٥°) دورةً كاملة ثم المقبض خارجًا — مدخلٌ ومخرجٌ واحد.
        floatArrayOf(
            60.4f, 60.4f,
            64.6f, 57.4f, 68.0f, 50.2f, 68.0f, 42.0f,
            68.0f, 56.4f, 56.4f, 68.0f, 42.0f, 68.0f,
            27.6f, 68.0f, 16.0f, 56.4f, 16.0f, 42.0f,
            16.0f, 27.6f, 27.6f, 16.0f, 42.0f, 16.0f,
            56.4f, 16.0f, 68.0f, 27.6f, 60.4f, 60.4f,
            66.0f, 66.0f, 78.0f, 78.0f, 88.0f, 88.0f,
        ),
        // الشطب: مستقيم يخرج عن العدسة من الجانبين كي يُقرأ شطبًا لا قطرًا.
        floatArrayOf(24.0f, 84.0f, 44.0f, 64.0f, 64.0f, 44.0f, 84.0f, 24.0f),
    ),
)

/**
 * «وصلت عيّنة»: علامة صحّ بخطّ واحد — هبوطٌ قصير ثم صعودٌ طويل، كيد تكتب على الورق.
 *
 * قلمها أثقل من قلم العدسة (١٢ من ١٠٠ لا ٧): تُعرض في حبّة ثقة بحجم ١٦dp، والقلم النسبيّ الرفيع
 * فيها يصير خيطًا لا يُرى. وهي **لا** تُرسم في حبّة ثابتة (بلا `sampleKey`): الحبّة الثابتة ساكنة.
 */
private val INK_TICK = MaxInkDrawing(
    viewBoxWidth = 100f,
    viewBoxHeight = 100f,
    strokeWidth = 12f,
    strokes = listOf(
        floatArrayOf(
            14.0f, 56.0f,
            20.0f, 60.0f, 27.0f, 68.0f, 34.0f, 74.0f,
            48.0f, 52.0f, 66.0f, 30.0f, 86.0f, 12.0f,
        ),
    ),
)

/**
 * «يجري الآن»: موجة كتابة طويلة — تُمطّ عرضًا ([MaxInkDrawingCanvas] `stretch`).
 *
 * إطارها عريض (٣٠٠×٦٠) لأنها تُرسم بعرض البطاقة: في ٣٦٠dp يصير ارتفاع الموجة ~١٢dp وقلمها ~٢dp.
 * وتُكشف بحركة «ثعبان» ([MaxInkLoop]): رأسٌ يتقدّم وذيلٌ يلحق، فلا تظهر موجة كاملة ثم تُمسح.
 */
private val INK_WAVE = MaxInkDrawing(
    viewBoxWidth = 300f,
    viewBoxHeight = 60f,
    strokeWidth = 6f,
    strokes = listOf(
        floatArrayOf(
            14.0f, 30.0f,
            70.0f, 8.0f, 130.0f, 52.0f, 190.0f, 30.0f,
            232.0f, 15.0f, 262.0f, 22.0f, 286.0f, 30.0f,
        ),
    ),
)

/** الرسوم المتاحة للحالات. تُزاد واحدةً حين تُستعمل واحدة — ولا تُترك رسمة بلا مستهلك. */
enum class MaxInkGlyph { NotFound }

/**
 * رسمة حالة تُكتب أمام العين مرّة عند ظهورها.
 *
 * @param animated كشفٌ متدرّج عند أول ظهور — ومن أوقف حركات النظام يراها كاملة فورًا.
 */
@Composable
fun MaxInkIllustration(
    glyph: MaxInkGlyph,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    animated: Boolean = true,
) {
    val drawing = when (glyph) {
        MaxInkGlyph.NotFound -> INK_NOT_FOUND
    }
    val animationsEnabled = rememberAnimationsEnabled()
    val reveal = remember { Animatable(if (animated && animationsEnabled) 0f else 1f) }

    LaunchedEffect(animated, animationsEnabled) {
        if (!animated || !animationsEnabled) {
            reveal.snapTo(1f)
        } else {
            reveal.snapTo(0f)
            // ٢× الزمن المتأنّي: أطول من انتقال واجهة (الرسمة تُكتب)، وأقصر من الوسم (٣×) لأنها أصغر.
            reveal.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = MaxDuration.deliberate * 2,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    MaxInkDrawingCanvas(
        drawing = drawing,
        modifier = modifier,
        tint = tint,
        start = { 0f },
        end = { reveal.value },
    )
}

/**
 * علامة «وصلت عيّنة جديدة»: تُرسم من جديد **فقط** حين يتغيّر [sampleKey].
 *
 * الغرض هو الفرق كلّه: الحركة هنا ليست زينة تُشغَّل بمؤقّت، بل **إشارة حداثة**. مفتاحها قيمة
 * القراءة المعروضة (`metric.value` في [MaxMetricProvenance])، فإذا تغيّر الرقم على الشاشة
 * تحرّك القلم — وإذا لم يتغيّر فلا شيء يتحرّك، لأن لا شيء حدث. ومفتاح `null` يعني «حبّة ثابتة»
 * (بلا قراءة تتبعها): تُرسم مرّة بلا إعادة، كما كانت النقطة الساكنة قبلها.
 *
 * ولا تُشغَّل عند أول تركيب (المفتاح الأول ليس تغيّرًا) — الحبّة تظهر ساكنة ثم تنبض حين **تتحدّث**
 * القراءة، فلا ترتجف شاشةٌ كاملة عند فتحها.
 */
@Composable
fun MaxInkTick(
    sampleKey: Any?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    animated: Boolean = true,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val reveal = remember { Animatable(1f) }
    var seen by remember { mutableStateOf(sampleKey) }

    LaunchedEffect(sampleKey) {
        if (sampleKey == seen) return@LaunchedEffect
        seen = sampleKey
        if (!animated || !animationsEnabled) {
            reveal.snapTo(1f)
        } else {
            reveal.snapTo(0f)
            reveal.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = MaxDuration.standard,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    MaxInkDrawingCanvas(
        drawing = INK_TICK,
        modifier = modifier,
        tint = tint,
        start = { 0f },
        end = { reveal.value },
    )
}

/**
 * «هناك عمل يجري الآن»: موجة تُكتب بلا نهاية — رأسٌ يتقدّم وذيلٌ يلحق.
 *
 * **عمرها هو عمر العمل**: لا تُستدعى إلا داخل شرط حالة جارية (`activation == Running`، `compact`)،
 * فتموت من الشاشة بموت العمل. ومن أوقف الحركات يرى الموجة كاملة ساكنة، والنصّ المجاور يقول «جارٍ».
 */
@Composable
fun MaxInkLoop(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    if (!animationsEnabled) {
        MaxInkDrawingCanvas(
            drawing = INK_WAVE,
            modifier = modifier,
            tint = tint,
            start = { 0f },
            end = { 1f },
            stretch = true,
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "maxInkLoop")
    // الرأس يتقدّم في ١٠٠٠ms ثم يثبت، والذيل يبدأ بعد ٤٠٠ms ويلحق حتى يدركه عند ١٤٠٠ms:
    // عند لحظة الالتحام يكون المقطع صفرًا فيُعاد من البداية **بلا قطع مرئيّ**.
    val end by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0f at 0
                1f at 1000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "inkLoopHead",
    )
    val start by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0f at 0
                0f at 400
                1f at 1400 using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "inkLoopTail",
    )

    MaxInkDrawingCanvas(
        drawing = INK_WAVE,
        modifier = modifier,
        tint = tint,
        start = { start },
        end = { end },
        stretch = true,
    )
}

/**
 * العارض الواحد: يحوّل أرقام الإطار إلى إحداثيات اللوحة، ويوزّع المقطع المكشوف على الخطوط
 * **بالتسلسل** (خطٌّ واحد يُكتب ثم الذي يليه)، ويرسم بقلم واحد ثابت السماكة.
 *
 * يُبنى المسار في إحداثيات اللوحة مباشرةً (لا مصفوفة تحويل على مسار مرسوم): فسُمك القلم يبقى
 * متساويًا ولو مُطّت الرسمة عرضًا، والقياس ([PathMeasure]) يقع على ما سيُرسم فعلًا لا على أرقام
 * الإطار — وهذا ما يجعل الكشف يتقدّم بسلاسة على أي عرض.
 *
 * ويقرأ `start`/`end` **داخل الرسم** (لامبدات لا قيمًا): قراءة قيمة الحركة في نطاق الرسم تُعيد
 * رسم الطبقة وحدها، لا تركيب الشاشة كلها — وهو الدرس نفسه المكتوب في [MaxSignatureMark].
 */
@Composable
private fun MaxInkDrawingCanvas(
    drawing: MaxInkDrawing,
    modifier: Modifier,
    tint: Color,
    start: () -> Float,
    end: () -> Float,
    stretch: Boolean = false,
) {
    val paths = remember(drawing) { List(drawing.strokes.size) { Path() } }
    val measures = remember(drawing) { List(drawing.strokes.size) { PathMeasure() } }
    val lengths = remember(drawing) { FloatArray(drawing.strokes.size) }
    val segment = remember(drawing) { Path() }

    Canvas(modifier = modifier) {
        val vbWidth = drawing.viewBoxWidth
        val vbHeight = drawing.viewBoxHeight
        val scaleX: Float
        val scaleY: Float
        val offsetX: Float
        val offsetY: Float
        if (stretch) {
            scaleX = size.width / vbWidth
            scaleY = size.height / vbHeight
            offsetX = 0f
            offsetY = 0f
        } else {
            // نسبة واحدة تُبقي الشكل، والوسط هو الحشو حين لا تتساوى النسبتان.
            val scale = minOf(size.width / vbWidth, size.height / vbHeight)
            scaleX = scale
            scaleY = scale
            offsetX = (size.width - vbWidth * scale) / 2f
            offsetY = (size.height - vbHeight * scale) / 2f
        }
        val pen = drawing.strokeWidth * scaleY
        if (pen <= 0f) return@Canvas

        // الأطوال أولًا: المجموع يقسّم المقطع المكشوف، فيُرسم بقلم واحد يمشي على الخطوط بالتسلسل.
        var total = 0f
        drawing.strokes.forEachIndexed { index, points ->
            val path = paths[index]
            path.rewind()
            path.addInkStroke(points, scaleX, scaleY, offsetX, offsetY)
            val measure = measures[index]
            measure.setPath(path, false)
            lengths[index] = measure.length
            total += measure.length
        }
        if (total <= 0f) return@Canvas

        val from = start().coerceIn(0f, 1f) * total
        val to = end().coerceIn(0f, 1f) * total
        if (to <= from) return@Canvas

        var consumed = 0f
        drawing.strokes.forEachIndexed { index, _ ->
            val length = lengths[index]
            val strokeFrom = (from - consumed).coerceIn(0f, length)
            val strokeTo = (to - consumed).coerceIn(0f, length)
            consumed += length
            if (strokeTo <= strokeFrom) return@forEachIndexed
            segment.rewind()
            measures[index].getSegment(strokeFrom, strokeTo, segment, true)
            drawPath(
                path = segment,
                color = tint,
                style = Stroke(width = pen, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/** نقطة البداية ثم قطع `cubicTo` — تُحوَّل إلى إحداثيات اللوحة أثناء البناء. */
private fun Path.addInkStroke(
    points: FloatArray,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float,
    offsetY: Float,
) {
    moveTo(offsetX + points[0] * scaleX, offsetY + points[1] * scaleY)
    var i = 2
    while (i + 5 < points.size) {
        cubicTo(
            offsetX + points[i] * scaleX, offsetY + points[i + 1] * scaleY,
            offsetX + points[i + 2] * scaleX, offsetY + points[i + 3] * scaleY,
            offsetX + points[i + 4] * scaleX, offsetY + points[i + 5] * scaleY,
        )
        i += 6
    }
}
