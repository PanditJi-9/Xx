/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * وسم الصانع — «M» بخطّ واحد متّصل، يُرسم ولا يُولَّد.
 *
 * ## لماذا رسمة لا صورة
 * ملاحظة التقييم طلبت لمسة شخصية فريدة بدل «مظهر مولَّد»: الصورة الجاهزة تُنقل وتُنسخ
 * وتفقد أصلها، أمّا الخطّ فأرقامه هنا، ويرسمه التطبيق والشاشة والصورة المصغّرة من
 * المصدر نفسه. ولا يدخل المستودع ملفّ من مولّد صور: هذه القاعدة مكتوبة في `ADR-73`.
 *
 * ## مصدر واحد وثلاثة عارضين
 * الأرقام أدناه هي حرفيًّا أرقام `docs/assets/makers-mark-{dark,light}.svg`، و`tools/makers_mark.py`
 * هو الذي يولّد الملفّين **ويقيس** أنهما لم ينزاحا عن هذه القيم:
 *
 *     python3 tools/makers_mark.py --assert
 *
 * فلا تُعدَّل الأرقام هنا يدويًّا: التعديل من الأداة ثم `--generate`، وإلا حمرّت البوّابة.
 */
package nd.max.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxTone
import nd.max.ui.design.content

/** فضاء الرسم — نفسه في `tools/makers_mark.py` (`VIEW`). */
private const val MARK_VIEW = 100f

/** سُمك القلم في فضاء [MARK_VIEW]؛ ونصفه (3.5) يُبقي المدى المرئيّ داخل اللوحة [5, 95]. */
private const val MARK_STROKE_W = 7f

/**
 * move ثم سبع قطع `cubicTo` — (c1x, c1y, c2x, c2y, x, y) لكل قطعة.
 *
 * والساق اليمنى تنساب يمينًا وهي تهبط (خروج القلم) بدل أن تنزل مستقيمة: هذا الانسياب
 * والوادي الأعمق من مستقيمٍ بين القمّتين هما ما يجعل الخطّ يقرأ «مكتوبًا» لا «مرسومًا بالمسطرة».
 */
private val MARK_SEGMENTS = floatArrayOf(
    8.5f, 86.0f,
    8.6f, 61.0f, 9.4f, 39.0f, 11.5f, 20.5f,
    12.0f, 15.0f, 16.5f, 14.5f, 19.5f, 19.0f,
    29.5f, 43.0f, 40.5f, 59.5f, 48.5f, 69.0f,
    57.0f, 57.0f, 68.0f, 34.0f, 78.5f, 17.5f,
    81.0f, 13.5f, 85.0f, 14.0f, 86.0f, 20.0f,
    87.5f, 39.0f, 89.0f, 63.0f, 91.5f, 87.0f,
)

private fun buildMarkPath(): Path {
    val path = Path()
    path.moveTo(MARK_SEGMENTS[0], MARK_SEGMENTS[1])
    var i = 2
    while (i + 5 < MARK_SEGMENTS.size) {
        path.cubicTo(
            MARK_SEGMENTS[i], MARK_SEGMENTS[i + 1],
            MARK_SEGMENTS[i + 2], MARK_SEGMENTS[i + 3],
            MARK_SEGMENTS[i + 4], MARK_SEGMENTS[i + 5],
        )
        i += 6
    }
    return path
}

/**
 * وسم الصانع: خطٌّ واحد يُكتب أمام العين.
 *
 * @param tint لون القلم — الافتراضيّ «الإيجابي» بعينه في `MaxTokens.kt`، وهو نفسه في ملفّي
 *        الصفحة، فيخرج الوسم في التطبيق وفي الصفحة بالحبر ذاته على الوضعين الداكن والفاتح.
 * @param animated كشفٌ متدرّج عند أول ظهور. ومن أوقف حركات النظام يرى الخطّ كاملًا فورًا
 *        ([rememberAnimationsEnabled]) — «بلا حركة» ليست «حركة أسرع».
 */
@Composable
fun MaxSignatureMark(
    modifier: Modifier = Modifier,
    tint: Color = MaxTone.Positive.content(),
    animated: Boolean = true,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    // البداية عند 1 حين لا حركة: وإلا مرّ إطار فارغ قبل أن يلحق الـLaunchedEffect.
    val reveal = remember { Animatable(if (animated && animationsEnabled) 0f else 1f) }

    LaunchedEffect(animated, animationsEnabled) {
        if (!animated || !animationsEnabled) {
            reveal.snapTo(1f)
        } else {
            reveal.snapTo(0f)
            // ٣× الزمن المتأنّي: الرسمة خطّ واحد يُملى على العين، لا انتقال واجهة.
            reveal.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = MaxDuration.deliberate * 3,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    val full = remember { buildMarkPath() }
    val measure = remember(full) { PathMeasure().apply { setPath(full, false) } }
    val length = remember(full) { measure.length }
    val drawn = remember(full) { Path() }

    Canvas(modifier = modifier) {
        val scale = size.minDimension / MARK_VIEW
        drawn.reset()
        // قراءة `reveal.value` داخل الرسم تُعيد مرحلة الرسم وحدها — لا تركيب الشاشة كلها.
        measure.getSegment(0f, length * reveal.value, drawn, true)
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            drawPath(
                path = drawn,
                color = tint,
                style = Stroke(
                    width = MARK_STROKE_W,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}
