/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.ui.license

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nd.max.R

/**
 * **شجرة قدرات Max AI** — صورة واحدة بدل النصوص (طلب المالك ٢٠٢٦-١٠-٢٢).
 *
 * الشجرة مرسومة على [Canvas] لا صورةً مخبوزة: فتتبع ألوان الثيم (فاتح/داكن) وتُترجَم عناوينها
 * من الموارد، وتُقرأ بقارئ الشاشة عبر وصفٍ واحد يسرد العُقَد بترتيبها.
 *
 * **كلّ عقدة لها أصلٌ في المحرّك** (لا قدرة مُدَّعاة):
 * - الجذر: Max AI، ومنه «الوضع» (أداء/متوازن/بطارية) و«القراءات اللحظية» (الحالة قبل القرار).
 * - المقابض: سقف GPU، سقف CPU والتعزيز، والحماية الحرارية (`ControlRegistry` + `SafetyGovernor`).
 * - التعلّم: نموذج الاستجابة (`ResponseModel`) يتعلّم أثر كل مقبض على الجهاز نفسه.
 * - القرار: المخطّط (`MinimalPlanner`) داخل الحدود الآمنة، والتحقّق بقراءة القيمة بعد الكتابة.
 * - النتيجتان هما هدفا الوضع (الأداء · توفير البطارية)، ومنهما تعود الحلقة إلى التعلّم.
 *
 * الرسم LTR دائمًا (الأسهم والحلقة لا تنعكس)، والنصّ العربي داخل العقد يُشكَّل كالمعتاد.
 */
@Composable
internal fun MaxAiCapabilityTree(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val baseStyle = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
    )

    val root = stringResource(R.string.maxai_tree_root)
    val mode = stringResource(R.string.maxai_tree_mode)
    val readings = stringResource(R.string.maxai_tree_readings)
    val gpu = stringResource(R.string.maxai_tree_gpu)
    val thermal = stringResource(R.string.maxai_tree_thermal)
    val cpu = stringResource(R.string.maxai_tree_cpu)
    val learning = stringResource(R.string.maxai_tree_learning)
    val safety = stringResource(R.string.maxai_tree_safety)
    val decide = stringResource(R.string.maxai_tree_decide)
    val verify = stringResource(R.string.maxai_tree_verify)
    val perf = stringResource(R.string.maxai_tree_perf)
    val battery = stringResource(R.string.maxai_tree_battery)
    val description = listOf(root, mode, readings, gpu, thermal, cpu, learning, safety, decide, verify, perf, battery)
        .joinToString(separator = " · ")

    val line = cs.onSurfaceVariant
    val rootFill = cs.primary
    val rootInk = cs.onPrimary
    val sideFill = cs.surfaceContainerHighest
    val sideInk = cs.onSurface
    val gpuFill = cs.tertiaryContainer
    val gpuInk = cs.onTertiaryContainer
    val thermalFill = cs.secondaryContainer
    val thermalInk = cs.onSecondaryContainer
    val cpuFill = cs.primaryContainer
    val cpuInk = cs.onPrimaryContainer
    val learnFill = cs.secondary
    val learnInk = cs.onSecondary
    val decideFill = cs.tertiary
    val decideInk = cs.onTertiary
    val resultPerfFill = cs.errorContainer
    val resultPerfInk = cs.onErrorContainer

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Canvas(
            modifier = modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .aspectRatio(TREE_W / TREE_H)
                .semantics { contentDescription = description },
        ) {
            val u = size.width / TREE_W // بكسل لكل وحدة منطقية
            val unitDp = size.width / density / TREE_W
            val textStyle = baseStyle.copy(fontSize = (11.5f * unitDp.coerceIn(0.85f, 1.25f)).sp)
            val stroke = 1.6.dp.toPx()
            val head = 6.5f * u

            fun p(x: Float, y: Float) = Offset(x * u, y * u)

            // ── الخطوط أولًا (تحت العقد) ───────────────────────────────────────────────
            // الجذر ↔ القراءات (ذهابًا وإيابًا) ، والجذر → الوضع
            link(p(100f, 32f), p(126f, 32f), line, stroke, head, both = true)
            link(p(234f, 32f), p(260f, 32f), line, stroke, head, both = false)
            // الجذر → ناقل أفقي → المقابض الثلاثة
            seg(p(180f, 58f), p(180f, 84f), line, stroke)
            seg(p(57f, 84f), p(303f, 84f), line, stroke)
            link(p(57f, 84f), p(57f, 106f), line, stroke, head, both = false)
            link(p(180f, 84f), p(180f, 98f), line, stroke, head, both = false)
            link(p(303f, 84f), p(303f, 106f), line, stroke, head, both = false)
            // المقابض ↔ الحماية الحرارية
            link(p(100f, 128f), p(124f, 128f), line, stroke, head, both = true)
            link(p(236f, 128f), p(260f, 128f), line, stroke, head, both = true)
            // المقابض → التعلّم
            seg(p(57f, 150f), p(57f, 188f), line, stroke)
            seg(p(303f, 150f), p(303f, 188f), line, stroke)
            seg(p(180f, 158f), p(180f, 188f), line, stroke)
            seg(p(57f, 188f), p(303f, 188f), line, stroke)
            link(p(180f, 188f), p(180f, 224f), line, stroke, head, both = false)
            // التعلّم → القرار
            link(p(180f, 304f), p(180f, 338f), line, stroke, head, both = false)
            // القرار ↔ الحدود الآمنة ↔ التحقّق
            link(p(100f, 360f), p(118f, 360f), line, stroke, head, both = true)
            link(p(242f, 360f), p(260f, 360f), line, stroke, head, both = true)
            // القرار → النتيجتان
            seg(p(180f, 382f), p(180f, 400f), line, stroke)
            seg(p(93f, 400f), p(267f, 400f), line, stroke)
            link(p(93f, 400f), p(93f, 418f), line, stroke, head, both = false)
            link(p(267f, 400f), p(267f, 418f), line, stroke, head, both = false)
            // حلقتا العودة من النتيجتين إلى التعلّم
            seg(p(36f, 440f), p(6f, 440f), line, stroke)
            seg(p(6f, 440f), p(6f, 264f), line, stroke)
            link(p(6f, 264f), p(118f, 264f), line, stroke, head, both = false)
            seg(p(324f, 440f), p(354f, 440f), line, stroke)
            seg(p(354f, 440f), p(354f, 264f), line, stroke)
            link(p(354f, 264f), p(242f, 264f), line, stroke, head, both = false)

            // ── العقد ────────────────────────────────────────────────────────────────
            pill(p(14f, 106f), Size(86f * u, 44f * u), gpuFill)
            nodeText(measurer, gpu, textStyle, gpuInk, p(57f, 128f), 74f * u)

            hexagon(p(124f, 98f), Size(112f * u, 60f * u), thermalFill)
            nodeText(measurer, thermal, textStyle, thermalInk, p(180f, 128f), 84f * u)

            pill(p(260f, 106f), Size(86f * u, 44f * u), cpuFill)
            nodeText(measurer, cpu, textStyle, cpuInk, p(303f, 128f), 74f * u)

            roundBox(p(126f, 8f), Size(108f * u, 50f * u), 12f * u, rootFill)
            nodeText(measurer, root, textStyle.copy(fontWeight = FontWeight.Bold), rootInk, p(180f, 33f), 92f * u)

            pill(p(14f, 12f), Size(86f * u, 40f * u), sideFill)
            nodeText(measurer, readings, textStyle, sideInk, p(57f, 32f), 74f * u)

            pill(p(260f, 12f), Size(86f * u, 40f * u), sideFill)
            nodeText(measurer, mode, textStyle, sideInk, p(303f, 32f), 74f * u)

            diamond(p(118f, 224f), Size(124f * u, 80f * u), learnFill)
            nodeText(measurer, learning, textStyle, learnInk, p(180f, 264f), 66f * u)

            roundBox(p(14f, 338f), Size(86f * u, 44f * u), 10f * u, sideFill)
            nodeText(measurer, safety, textStyle, sideInk, p(57f, 360f), 74f * u)

            roundBox(p(118f, 338f), Size(124f * u, 44f * u), 10f * u, decideFill)
            nodeText(measurer, decide, textStyle, decideInk, p(180f, 360f), 108f * u)

            roundBox(p(260f, 338f), Size(86f * u, 44f * u), 10f * u, sideFill)
            nodeText(measurer, verify, textStyle, sideInk, p(303f, 360f), 74f * u)

            pill(p(36f, 418f), Size(114f * u, 44f * u), resultPerfFill)
            nodeText(measurer, perf, textStyle, resultPerfInk, p(93f, 440f), 98f * u)

            pill(p(210f, 418f), Size(114f * u, 44f * u), cpuFill)
            nodeText(measurer, battery, textStyle, cpuInk, p(267f, 440f), 98f * u)
        }
    }
}

private const val TREE_W = 360f
private const val TREE_H = 470f

// ── أدوات الرسم ──────────────────────────────────────────────────────────────────────────

private fun DrawScope.seg(a: Offset, b: Offset, color: Color, width: Float) {
    drawLine(color = color, start = a, end = b, strokeWidth = width, cap = StrokeCap.Round)
}

/** خطّ بسهم عند [b]، وعند [a] أيضًا إن كان [both] (علاقة ذهابٍ وإيّاب). */
private fun DrawScope.link(
    a: Offset,
    b: Offset,
    color: Color,
    width: Float,
    head: Float,
    both: Boolean,
) {
    seg(a, b, color, width)
    val dx = b.x - a.x
    val dy = b.y - a.y
    val len = kotlin.math.sqrt(dx * dx + dy * dy)
    if (len <= 0f) return
    val dir = Offset(dx / len, dy / len)
    arrowHead(b, dir, head, color)
    if (both) arrowHead(a, Offset(-dir.x, -dir.y), head, color)
}

private fun DrawScope.arrowHead(tip: Offset, dir: Offset, size: Float, color: Color) {
    val back = Offset(tip.x - dir.x * size, tip.y - dir.y * size)
    val perp = Offset(-dir.y, dir.x)
    val half = size * 0.55f
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(back.x + perp.x * half, back.y + perp.y * half)
        lineTo(back.x - perp.x * half, back.y - perp.y * half)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.pill(topLeft: Offset, size: Size, color: Color) {
    drawRoundRect(color = color, topLeft = topLeft, size = size, cornerRadius = CornerRadius(size.height / 2f))
}

private fun DrawScope.roundBox(topLeft: Offset, size: Size, radius: Float, color: Color) {
    drawRoundRect(color = color, topLeft = topLeft, size = size, cornerRadius = CornerRadius(radius))
}

private fun DrawScope.hexagon(topLeft: Offset, size: Size, color: Color) {
    val w = size.width
    val h = size.height
    val inset = w * 0.2f
    val path = Path().apply {
        moveTo(topLeft.x, topLeft.y + h / 2f)
        lineTo(topLeft.x + inset, topLeft.y)
        lineTo(topLeft.x + w - inset, topLeft.y)
        lineTo(topLeft.x + w, topLeft.y + h / 2f)
        lineTo(topLeft.x + w - inset, topLeft.y + h)
        lineTo(topLeft.x + inset, topLeft.y + h)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.diamond(topLeft: Offset, size: Size, color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(topLeft.x + w / 2f, topLeft.y)
        lineTo(topLeft.x + w, topLeft.y + h / 2f)
        lineTo(topLeft.x + w / 2f, topLeft.y + h)
        lineTo(topLeft.x, topLeft.y + h / 2f)
        close()
    }
    drawPath(path, color)
}

/** نصّ مُوسَّط في العقدة: سطران كحدٍّ أقصى، ويُقطَع بعلامة حذف لا أن يفيض خارج الشكل. */
private fun DrawScope.nodeText(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    color: Color,
    center: Offset,
    maxWidth: Float,
) {
    val w = maxWidth.toInt().coerceAtLeast(1)
    val layout = measurer.measure(
        text = text,
        style = style.copy(color = color),
        overflow = TextOverflow.Ellipsis,
        maxLines = 2,
        constraints = Constraints(minWidth = w, maxWidth = w),
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f),
    )
}
