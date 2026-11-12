/*
 * Copyright (C) 2026-2027 Zexshia
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */
package nd.max.ui.subscreens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nd.max.R
import nd.max.core.platform.SessionPoint
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import nd.max.ui.design.MaxCardSpec
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxVisualSpec
import nd.max.ui.theme.MonoValueStyleLarge
import nd.max.ui.theme.MonoValueStyleSmall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * **تقرير الجلسة — العنصر الحامل في شاشة جلسات اللعب.**
 *
 * بنية التقرير مأخوذة من تقرير Scene الذي أرسله المالك كمرجع (معلومات لا شكل):
 * رأس (اللعبة · التاريخ · الحزمة · سطر المدّة)، ثم ثمانية أرقام في شبكة عمودين، ثم
 * منحنى الإطارات مع خطّي المتوسّط والأدنى ودقّة العرض، ويُسحب المنحنى لقراءة أي لحظة.
 *
 * **ولا يُنسخ شكل المرجع**: الخطوط والألوان والمسافات من `MaxTokens.kt` و`MonoValueStyle`،
 * والحاوية `MaxGroup` من نظام التصميم، والسحب والرسم داخل هذه الشاشة لا في مكتبة خارجية.
 *
 * يظهر التقرير العلوي لبيانات المسجّل الحالية، وتستعمل الجلسة المحفوظة التركيب نفسه حين يفتح
 * المستخدم صفّها. لا تُرسم إلا جلسة محفوظة واحدة في الوقت نفسه، ولا يتبدّل مصدر التقرير
 * العلوي بصمت.
 */
@Composable
fun GameSessionReport(
    title: String,
    packageName: String?,
    dateMs: Long,
    meta: String,
    stats: GameReportStats,
    points: List<SessionPoint>,
    live: Boolean,
    accent: Color,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    MaxGroup(modifier = modifier) {
        if (stats.isEmpty && points.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodySmall,
                color = variant,
                modifier = Modifier.padding(
                    horizontal = MaxSpace.rowPaddingHorizontal,
                    vertical = MaxSpace.md
                )
            )
        } else {
            Column(
                modifier = Modifier.padding(
                    horizontal = MaxSpace.rowPaddingHorizontal,
                    vertical = MaxSpace.md
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (live) {
                        LiveBadge(accent)
                    }
                }
                Text(
                    text = SimpleDateFormat("d MMM yyyy · HH:mm", Locale.getDefault())
                        .format(Date(dateMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = variant
                )
                packageName?.takeIf { it.isNotBlank() }?.let { pkg ->
                    Text(
                        text = pkg,
                        style = MonoValueStyleSmall,
                        color = variant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = variant
                    )
                }
            }

            MaxGroupDivider()
            ReportStatGrid(stats = stats, accent = accent)
            MaxGroupDivider()
            ReportChartSection(stats = stats, points = points, accent = accent)
        }
    }
}

/** وسم «مباشر» — نقطة ونصّ، لا حبّة ملوّنة ولا وميض. */
@Composable
private fun LiveBadge(accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(accent, androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.game_sessions_report_live),
            style = MaterialTheme.typography.labelMedium,
            color = accent
        )
    }
}

/**
 * ثمانية أرقام: الإطارات أولًا، ثم التقطيع والحرارة، ثم الطاقة. الشبكة تتكيّف مع
 * العرض وتكبير الخط؛ والقيمة الغائبة تُطبع `—` لا `0`. المتوسط بلون منحناه، والقراءات
 * المساندة بلونها الثالث؛ الحرارة والتقطيع ليسا خطأً لمجرّد وجود قراءة بلا حكم خطر.
 */
@Composable
private fun ReportStatGrid(stats: GameReportStats, accent: Color) {
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    val scheme = MaterialTheme.colorScheme
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = MaxSpace.rowPaddingHorizontal,
            vertical = MaxSpace.md,
        )
    ) {
        val stacked = fontScale > MaxVisualSpec.largeFontScale ||
            maxWidth.value < MaxCardSpec.minColumnWidth.value * 2 * fontScale.coerceAtLeast(1f) +
                MaxSpace.md.value
        Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
            StatPair(
                stacked = stacked,
                first = StatText(
                    label = stringResource(R.string.game_sessions_fps_avg),
                    value = stats.averageFps?.let { "%.0f".format(it) },
                    unit = "FPS",
                    tone = accent
                ),
                second = StatText(
                    label = stringResource(R.string.game_sessions_report_min),
                    value = stats.minFps?.let { "%.0f".format(it) },
                    unit = "FPS"
                )
            )
            StatPair(
                stacked = stacked,
                first = StatText(
                    label = stringResource(R.string.game_sessions_report_low1),
                    value = stats.onePercentLowFps?.let { "%.0f".format(it) },
                    unit = "FPS",
                    tone = scheme.tertiary
                ),
                second = StatText(
                    label = stringResource(R.string.game_sessions_p99),
                    value = stats.frameP99Ms?.let { "%.1f".format(it) },
                    unit = "ms"
                )
            )
            StatPair(
                stacked = stacked,
                first = StatText(
                    label = stringResource(R.string.game_sessions_jank_avg),
                    value = stats.jankPercent?.let { "%.0f".format(it) },
                    unit = "%"
                ),
                second = StatText(
                    label = stringResource(R.string.game_sessions_soc_peak),
                    value = stats.maxSocTempC?.let { "%.0f".format(it) },
                    unit = "°C"
                )
            )
            StatPair(
                stacked = stacked,
                first = StatText(
                    label = batteryLabel(stats.batteryTempDeltaC),
                    value = heatSpan(stats.batteryTempStartC, stats.batteryTempEndC),
                    unit = if (stats.batteryTempStartC != null || stats.batteryTempEndC != null) "°C" else null
                ),
                second = StatText(
                    label = stringResource(R.string.game_sessions_power_avg),
                    value = stats.averagePowerMw?.toString(),
                    unit = "mW",
                    tone = scheme.tertiary
                )
            )
            if (stats.samples > 0 || stats.droppedSamples > 0) {
                Text(
                    text = stringResource(
                        R.string.game_sessions_report_samples,
                        stats.samples,
                        stats.droppedSamples
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = variant
                )
            }
        }
    }
}

/** بطاقة رقم واحدة داخل الشبكة: تسمية صغيرة فوق قيمة أحادية. */
private data class StatText(
    val label: String,
    val value: String?,
    val unit: String?,
    /** لون التركيز أو القراءة المساندة، لا حكم سلامة يُستنتج من اللون وحده. */
    val tone: Color? = null,
)

@Composable
private fun StatPair(first: StatText, second: StatText, stacked: Boolean) {
    if (stacked) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.md),
        ) {
            StatCell(text = first, modifier = Modifier.fillMaxWidth())
            StatCell(text = second, modifier = Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
        ) {
            StatCell(text = first, modifier = Modifier.weight(1f))
            StatCell(text = second, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatCell(text: StatText, modifier: Modifier = Modifier) {
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier) {
        Text(
            text = text.label,
            style = MaterialTheme.typography.labelMedium,
            color = variant
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs),
        ) {
            Text(
                text = text.value ?: MAX_VALUE_UNAVAILABLE,
                style = MonoValueStyleLarge,
                color = if (text.value == null) variant else text.tone ?: MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Bottom),
            )
            text.unit?.let { unit ->
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = variant,
                    modifier = Modifier.align(Alignment.Bottom)
                )
            }
        }
    }
}

/** «٣٨ ← ٤٠» أو الواحد منهما وحده؛ وما لا يُقرأ يبقى `—` في الخانة. */
private fun heatSpan(start: Float?, end: Float?): String? = when {
    start == null && end == null -> null
    start == null -> "%.0f".format(end)
    end == null -> "%.0f".format(start)
    else -> "%.0f ← %.0f".format(start, end)
}

@Composable
private fun batteryLabel(deltaC: Float?): String {
    val base = stringResource(R.string.game_sessions_report_battery)
    val delta = deltaC ?: return base
    val signed = if (delta >= 0f) "+%.0f".format(delta) else "%.0f".format(delta)
    return stringResource(R.string.game_sessions_report_battery_delta, base, signed)
}

/** منحنى الإطارات: متوسّط متّصل، وأدنى الجلسة متقطّع، ودقّة العرض متقطّعة أخفّ. */
@Composable
private fun ReportChartSection(
    stats: GameReportStats,
    points: List<SessionPoint>,
    accent: Color,
) {
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val frames = remember(points) { points.mapNotNull { it.frames?.takeIf { f -> f > 0f } } }
    val spanMs = remember(points) {
        val first = points.firstOrNull()?.atMs ?: 0L
        val last = points.lastOrNull()?.atMs ?: first
        (last - first).coerceAtLeast(0L)
    }

    Column(
        modifier = Modifier.padding(
            horizontal = MaxSpace.rowPaddingHorizontal,
            vertical = MaxSpace.md
        )
    ) {
        Text(
            text = stringResource(R.string.game_sessions_report_fps),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.game_sessions_report_hint),
            style = MaterialTheme.typography.bodySmall,
            color = variant
        )
        Spacer(modifier = Modifier.height(MaxSpace.sm))
        Row {
            LegendItem(
                color = accent,
                label = stringResource(R.string.game_sessions_report_legend_avg),
                value = stats.averageFps?.let { "%.0f FPS".format(it) },
                modifier = Modifier.weight(1f)
            )
            LegendItem(
                color = variant,
                label = stringResource(R.string.game_sessions_report_legend_min),
                value = stats.minFps?.let { "%.0f FPS".format(it) },
                modifier = Modifier.weight(1f)
            )
        }
        stats.refreshHz?.let { hz ->
            LegendItem(
                color = outline,
                label = stringResource(R.string.game_sessions_report_legend_refresh),
                value = "$hz Hz",
                            modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(MaxSpace.md))

        if (frames.size < 2) {
            Text(
                text = stringResource(R.string.game_sessions_chart_empty),
                style = MaterialTheme.typography.bodySmall,
                color = variant
            )
        } else {
            val range = remember(points, stats.minFps, stats.maxFps, stats.refreshHz) {
                fpsAxisRange(points, stats.minFps, stats.maxFps, stats.refreshHz)
            }
            var scrubIndex by remember(points) { mutableStateOf<Int?>(null) }
            var chartWidth by remember { mutableStateOf(1f) }
            val selected = scrubIndex?.let { points.getOrNull(it) }
            val selectedValue = selected?.frames

            // سطر القراءة يظهر حين تُمسك لحظة فقط. النصّ الثابت فوق المنحنى يقول كيف يُقرأ،
            // فإعادته هنا كانت تكرارًا يدفع المنحنى إلى الأسفل بلا معلومة جديدة.
            if (selectedValue != null) {
                Text(
                    text = stringResource(
                        R.string.game_sessions_report_scrub,
                        "%.0f".format(selectedValue),
                        reportClock((selected?.atMs ?: 0L) - (points.firstOrNull()?.atMs ?: 0L))
                    ),
                    style = MonoValueStyleSmall,
                    color = accent,
                    modifier = Modifier.padding(bottom = MaxSpace.xs)
                )
            }

            Row {
                // النصوص في متغيّرات لا في وسيط `Text("…")`: الحارس يعدّ الحرفي المباشر
                // نصًّا صلبًا، والقيمة هنا محسوبة لا مكتوبة.
                val highLabel = "%.0f".format(range.endInclusive)
                val midLabel = "%.0f".format((range.start + range.endInclusive) / 2f)
                val lowLabel = "%.0f".format(range.start)
                Column(
                    modifier = Modifier.height(168.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(highLabel, style = MonoValueStyleSmall, color = variant)
                    Text(midLabel, style = MonoValueStyleSmall, color = variant)
                    Text(lowLabel, style = MonoValueStyleSmall, color = variant)
                }
                Spacer(modifier = Modifier.width(MaxSpace.sm))
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(168.dp)
                        .onSizeChanged { chartWidth = it.width.toFloat().coerceAtLeast(1f) }
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                scrubIndex = indexAt(offset.x, chartWidth, points.size)
                            }
                        }
                        .pointerInput(points) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    scrubIndex = indexAt(offset.x, chartWidth, points.size)
                                },
                                onDrag = { change, _ ->
                                    scrubIndex = indexAt(change.position.x, chartWidth, points.size)
                                }
                            )
                        }
                ) {
                    val origin = points.firstOrNull()?.atMs ?: 0L
                    val span = spanMs.coerceAtLeast(1L)
                    fun xOf(atMs: Long): Float =
                        ((atMs - origin).toFloat() / span.toFloat()).coerceIn(0f, 1f) * size.width
                    fun yOf(value: Float): Float {
                        val fraction = (value - range.start) / (range.endInclusive - range.start)
                        return size.height - fraction.coerceIn(0f, 1f) * size.height
                    }

                    // خطوط المحور الثلاثة كما في المرجع: أعلى، وسط، أسفل.
                    listOf(0f, 0.5f, 1f).forEach { fraction ->
                        val y = size.height * fraction
                        drawLine(
                            color = outline.copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }

                    stats.refreshHz?.takeIf { it.toFloat() in range }?.let { hz ->
                        val y = yOf(hz.toFloat())
                        drawLine(
                            color = outline,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        )
                    }
                    stats.minFps?.takeIf { it in range }?.let { low ->
                        val y = yOf(low)
                        drawLine(
                            color = variant.copy(alpha = 0.7f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
                        )
                    }

                    val path = Path()
                    var started = false
                    points.forEach { point ->
                        val value = point.frames?.takeIf { it > 0f } ?: return@forEach
                        val x = xOf(point.atMs)
                        val y = yOf(value)
                        if (started) path.lineTo(x, y) else {
                            path.moveTo(x, y)
                            started = true
                        }
                    }
                    if (started) {
                        drawPath(
                            path = path,
                            color = accent,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 3f,
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    selected?.frames?.takeIf { it > 0f }?.let { value ->
                        val x = xOf(selected.atMs)
                        drawLine(
                            color = accent.copy(alpha = 0.4f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 2f
                        )
                        drawCircle(color = accent, radius = 7f, center = Offset(x, yOf(value)))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaxSpace.xs),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0:00", style = MonoValueStyleSmall, color = variant)
                Text(reportClock(spanMs / 2), style = MonoValueStyleSmall, color = variant)
                Text(reportClock(spanMs), style = MonoValueStyleSmall, color = variant)
            }
        }
    }
}

/** عنصر مفتاح المنحنى: شرطة بلون الخطّ ونصّان — التسمية ثم القيمة. */
@Composable
private fun LegendItem(
    color: Color,
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(3.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value ?: MAX_VALUE_UNAVAILABLE,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** مدّة تُقرأ بالدقائق:الثواني — نسخة التقرير من منسّق الشاشة نفسه. */
internal fun reportClock(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val minutes = total / 60L
    val seconds = total % 60L
    return "%d:%02d".format(minutes, seconds)
}

/** موضع العيّنة تحت الإصبع: نسبة العرض × آخر موضع — بلا بحث ثنائي على ١٨٠ نقطة. */
private fun indexAt(x: Float, width: Float, size: Int): Int? {
    if (size == 0) return null
    val fraction = (x / width).coerceIn(0f, 1f)
    return (fraction * (size - 1)).roundToInt().coerceIn(0, size - 1)
}
