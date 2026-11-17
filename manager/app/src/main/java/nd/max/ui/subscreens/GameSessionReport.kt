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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import nd.max.ui.theme.MonoValueStyleMedium
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
 * **ما يزيده على المرجع** (طلب المالك: «أفضل من تقارير الأدوات الأخرى»): حلقة **الثبات** وتوزيع
 * زمن الجلسة (ثابت/متذبذب/متقطّع) نسبةً إلى معدّل اللعبة المعتاد لا إلى دقّة الشاشة؛ وأرقام
 * مساندة (زمن ≥٤٥ FPS، تذبذب الإطارات، حِمل GPU، تردّد المعالج، الطاقة، أدنى جهد)؛ وتراكب
 * اختياري (حرارة/حِمل/طاقة/زمن الإطار) على محور ثانٍ فوق المنحنى نفسه؛ و«أدنى اللحظات» التي
 * تقفز بالقراءة إلى موضع الهبوط. كلها نِسَب من نقاط المنحنى المصغّرة لا من كل إطار.
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
            val frames = remember(points) { positiveFrames(points) }
            val bands = remember(frames) { fpsBands(frames) }
            val typical = remember(frames) { typicalFps(frames) }
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
                // المُصيّر ومقاس العرض قيمتان ذاتيتا الدلالة («Vulkan · 1600×720»): تُطبعان
                // بلا تسميات، ولا تظهر إلا إن قِيستا (ADR-07).
                val facts = listOfNotNull(
                    stats.renderer?.takeIf { it.isNotBlank() },
                    stats.viewSize?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (facts.isNotBlank()) {
                    Text(
                        text = facts,
                        style = MonoValueStyleSmall,
                        color = variant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            MaxGroupDivider()
            if (bands != null && typical != null) {
                ReportStabilityHero(bands = bands, typical = typical, accent = accent)
                MaxGroupDivider()
            }
            ReportStatGrid(stats = stats, accent = accent)
            ReportExtraStats(stats = stats, frames = frames)
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
 * **الثبات أولًا.** حلقة بنسبة الزمن «الثابت» وشريط بثلاث فئات، نسبةً إلى **معدّل اللعبة المعتاد
 * في هذه الجلسة** ([typicalFps]) لا إلى دقّة الشاشة: لعبة مقفولة على ٦٠ فوق شاشة ٩٠ ليست
 * متعثّرة. الألوان درجات اللون المميّز نفسه ثم لون الإطار — لا أحمر: الفئة الأخيرة وصفٌ لا حكم
 * خطر (قاعدة هذا التقرير: قراءة بلا حكم).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportStabilityHero(bands: FpsBands, typical: Float, accent: Color) {
    val scheme = MaterialTheme.colorScheme
    val variant = scheme.onSurfaceVariant
    val track = scheme.outlineVariant
    val unevenTone = accent.copy(alpha = 0.5f)
    val roughTone = scheme.outline
    val ringLabel = "%d%%".format((bands.steady * 100f).roundToInt())
    Row(
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = MaxSpace.rowPaddingHorizontal,
            vertical = MaxSpace.md,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.lg),
    ) {
        Box(modifier = Modifier.size(MaxVisualSpec.compact), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = size.minDimension * 0.12f
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(inset, inset)
                drawArc(
                    color = track.copy(alpha = 0.6f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )
                if (bands.steady > 0.005f) {
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * bands.steady.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            }
            Text(text = ringLabel, style = MonoValueStyleMedium, color = accent)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.game_sessions_report_stability),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface
            )
            Text(
                text = stringResource(R.string.game_sessions_report_stability_note, "%.0f".format(typical)),
                style = MaterialTheme.typography.bodySmall,
                color = variant
            )
            Spacer(modifier = Modifier.height(MaxSpace.sm))
            BandBar(bands = bands, steady = accent, uneven = unevenTone, rough = roughTone)
            Spacer(modifier = Modifier.height(MaxSpace.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
            ) {
                LegendItem(
                    color = accent,
                    label = stringResource(R.string.game_sessions_report_band_steady),
                    value = sharePercent(bands.steady),
                )
                LegendItem(
                    color = unevenTone,
                    label = stringResource(R.string.game_sessions_report_band_uneven),
                    value = sharePercent(bands.uneven),
                )
                LegendItem(
                    color = roughTone,
                    label = stringResource(R.string.game_sessions_report_band_rough),
                    value = sharePercent(bands.rough),
                )
            }
        }
    }
}

/** شريط الفئات الثلاث بعرضٍ يتناسب مع الحصص؛ الحصّة الصفرية لا تُرسم (الوزن يجب أن يكون موجبًا). */
@Composable
private fun BandBar(bands: FpsBands, steady: Color, uneven: Color, rough: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MaxSpace.sm)
            .clip(CircleShape),
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.hairline),
    ) {
        if (bands.steady > 0f) Box(Modifier.weight(bands.steady).fillMaxHeight().background(steady))
        if (bands.uneven > 0f) Box(Modifier.weight(bands.uneven).fillMaxHeight().background(uneven))
        if (bands.rough > 0f) Box(Modifier.weight(bands.rough).fillMaxHeight().background(rough))
    }
}

/** حصّة ٠..١ كنسبة مئوية بلا كسر («٩٣٪» لا «٠٫٩٣»). */
private fun sharePercent(share: Float): String = "%.0f%%".format(share * 100f)

/**
 * ثمانية أرقام: الإطارات أولًا، ثم التقطيع والحرارة، ثم الطاقة. الشبكة تتكيّف مع
 * العرض وتكبير الخط؛ والقيمة الغائبة تُطبع `—` لا `0`. المتوسط بلون منحناه، والقراءات
 * المساندة بلونها الثالث؛ الحرارة والتقطيع ليسا خطأً لمجرّد وجود قراءة بلا حكم خطر.
 */
@Composable
private fun ReportStatGrid(stats: GameReportStats, accent: Color) {
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

/** ضيق الخانة أو تكبير الخط ⇒ تُرصّ القيمتان عموديًّا. الصيغة نفسها التي في شبكة الأرقام. */
private fun isStackedLayout(maxWidth: Dp, fontScale: Float): Boolean =
    fontScale > MaxVisualSpec.largeFontScale ||
        maxWidth.value < MaxCardSpec.minColumnWidth.value * 2 * fontScale.coerceAtLeast(1f) +
        MaxSpace.md.value

/** زوج قيم يُطبع إن قِيست إحداهما على الأقل؛ كلتاهما غائبتان ⇒ لا صفّ فارغ من `—`. */
@Composable
private fun OptionalStatPair(first: StatText, second: StatText, stacked: Boolean) {
    if (first.value != null || second.value != null) {
        StatPair(first = first, second = second, stacked = stacked)
    }
}

/**
 * الأرقام المساندة تحت الثمانية: **زمن ≥٤٥ FPS** (المقياس الذي تعرضه أدوات التقارير الأخرى، فتسهل
 * المقارنة) وتذبذب الإطارات، ثم حِمل GPU وتردّد المعالج، ثم الطاقة وأدنى جهد. كل زوج يختفي إن لم
 * يُقَس أيّ طرفيه، ولا يُطبع صفرٌ مكان غياب. وسطر العيّنات في آخرها.
 */
@Composable
private fun ReportExtraStats(stats: GameReportStats, frames: List<Float>) {
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    val tertiary = MaterialTheme.colorScheme.tertiary
    val fontScale = LocalDensity.current.fontScale
    val smoothShare = remember(frames) { shareAtOrAbove(frames, SMOOTH_FPS_THRESHOLD) }
    val spread = remember(frames) { fpsSpread(frames) }
    val hasFootnote = stats.samples > 0 || stats.droppedSamples > 0
    val hasPairs = smoothShare != null || spread != null || stats.gpuLoadAverage != null ||
        stats.cpuFreqAverageMhz != null || stats.energyWh != null || stats.voltageMinMv != null
    if (!hasFootnote && !hasPairs) return

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(
            start = MaxSpace.rowPaddingHorizontal,
            end = MaxSpace.rowPaddingHorizontal,
            bottom = MaxSpace.md,
        )
    ) {
        val stacked = isStackedLayout(maxWidth, fontScale)
        Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
            OptionalStatPair(
                stacked = stacked,
                first = StatText(
                    label = stringResource(R.string.game_sessions_report_share_smooth),
                    value = smoothShare?.let { "%.0f".format(it * 100f) },
                    unit = "%",
                    tone = tertiary
                ),
                second = StatText(
                    label = stringResource(R.string.game_sessions_report_variation),
                    value = spread?.let { "%.1f".format(it) },
                    unit = "FPS"
                )
            )
            OptionalStatPair(
                stacked = stacked,
                first = StatText(
                    label = stringResource(R.string.game_sessions_gpu_load_avg),
                    value = stats.gpuLoadAverage?.let { "%.0f".format(it) },
                    unit = "%"
                ),
                second = StatText(
                    label = stringResource(R.string.game_sessions_cpu_freq_avg),
                    value = stats.cpuFreqAverageMhz?.toString(),
                    unit = "MHz"
                )
            )
            OptionalStatPair(
                stacked = stacked,
                first = StatText(
                    label = stringResource(R.string.game_sessions_energy),
                    value = stats.energyWh?.let { "%.2f".format(it) },
                    unit = "Wh"
                ),
                second = StatText(
                    label = stringResource(R.string.game_sessions_voltage_min),
                    value = stats.voltageMinMv?.toString(),
                    unit = "mV"
                )
            )
            if (hasFootnote) {
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

@Composable
private fun overlayLabel(overlay: ReportOverlay): String = stringResource(
    when (overlay) {
        ReportOverlay.SocTemp -> R.string.game_sessions_overlay_soc
        ReportOverlay.BatteryTemp -> R.string.game_sessions_report_battery
        ReportOverlay.GpuLoad -> R.string.game_sessions_overlay_gpu
        ReportOverlay.CpuLoad -> R.string.game_sessions_overlay_cpu
        ReportOverlay.Power -> R.string.game_sessions_overlay_power
        ReportOverlay.FrameTime -> R.string.game_sessions_overlay_frame_time
    }
)

/**
 * منحنى الإطارات: متوسّط متّصل، وأدنى الجلسة متقطّع، ودقّة العرض متقطّعة أخفّ.
 *
 * وفوقه **تراكب اختياري** (حرارة SoC/البطارية، حِمل GPU/CPU، الطاقة، زمن الإطار) بخطّ ثانٍ على
 * محور أيمن بلون الخط نفسه — فيُرى سبب الهبوط في اللحظة ذاتها. لا يُعرض إلا ما قُرئ مرّتين على
 * الأقل في الجلسة. ثم «أدنى اللحظات»: أسوأ ثلاث هبوطات منفصلة، والمسّ على واحدة ينقل قارئ السحب
 * إليها. وسطر القراءة يحجز ارتفاعه دائمًا كي لا ينزلق المنحنى من تحت الإصبع عند أول لمسة.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportChartSection(
    stats: GameReportStats,
    points: List<SessionPoint>,
    accent: Color,
) {
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val overlayTone = MaterialTheme.colorScheme.tertiary
    val chartHeight = 168.dp
    val frames = remember(points) { points.mapNotNull { it.frames?.takeIf { f -> f > 0f } } }
    val spanMs = remember(points) {
        val first = points.firstOrNull()?.atMs ?: 0L
        val last = points.lastOrNull()?.atMs ?: first
        (last - first).coerceAtLeast(0L)
    }
    val overlays = remember(points) { ReportOverlay.entries.filter { overlayAvailable(points, it) } }
    var overlayChoice by remember { mutableStateOf<ReportOverlay?>(null) }
    val overlay = overlayChoice?.takeIf { it in overlays }
    val overlayBounds = remember(points, overlay) { overlay?.let { overlayRange(points, it) } }
    val overlayAvg = remember(points, overlay) { overlay?.let { overlayAverage(points, it) } }
    val typical = remember(frames) { typicalFps(frames) }
    val worst = remember(points, typical) {
        worstMoments(points, count = 3, below = (typical ?: 0f) * BAND_STEADY_FRACTION)
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
        if (overlay != null) {
            LegendItem(
                color = overlayTone,
                label = overlayLabel(overlay),
                value = overlayAvg?.let {
                    stringResource(R.string.game_sessions_report_overlay_avg, overlay.format(it))
                },
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

            if (overlays.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.game_sessions_report_overlay_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = variant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
                    overlays.forEach { option ->
                        FilterChip(
                            selected = option == overlay,
                            onClick = { overlayChoice = if (option == overlay) null else option },
                            label = {
                                Text(
                                    text = overlayLabel(option),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(MaxSpace.sm))
            }

            // سطر القراءة يحجز ارتفاعه حتى حين لا لحظة ممسوكة: ظهوره عند اللمس كان يدفع المنحنى
            // إلى الأسفل من تحت الإصبع فتقفز القراءة. والنصّ الثابت فوق المنحنى يقول كيف يُقرأ،
            // فلا تكرار هنا — الخانة فارغة حتى تُمسك لحظة.
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = MaxSpace.xl),
                contentAlignment = Alignment.CenterStart
            ) {
                if (selectedValue != null) {
                    val base = stringResource(
                        R.string.game_sessions_report_scrub,
                        "%.0f".format(selectedValue),
                        reportClock((selected?.atMs ?: 0L) - (points.firstOrNull()?.atMs ?: 0L))
                    )
                    val extra = overlay?.let { o -> selected?.let { o.read(it) }?.let { o.format(it) } }
                    val readout = if (extra != null) "$base · $extra" else base
                    Text(
                        text = readout,
                        style = MonoValueStyleSmall,
                        color = accent
                    )
                }
            }

            Row {
                // النصوص في متغيّرات لا في وسيط `Text("…")`: الحارس يعدّ الحرفي المباشر
                // نصًّا صلبًا، والقيمة هنا محسوبة لا مكتوبة.
                val highLabel = "%.0f".format(range.endInclusive)
                val midLabel = "%.0f".format((range.start + range.endInclusive) / 2f)
                val lowLabel = "%.0f".format(range.start)
                Column(
                    modifier = Modifier.height(chartHeight),
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
                        .height(chartHeight)
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
                    // محور التراكب مستقلّ عن محور الإطارات: كل خطّ يملأ ارتفاعه بنطاقه.
                    fun overlayY(value: Float): Float {
                        val bounds = overlayBounds ?: return size.height
                        val fraction = (value - bounds.start) / (bounds.endInclusive - bounds.start)
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

                    // التراكب يُرسم تحت خطّ الإطارات: الخطّ الرئيسي هو ما يُقرأ أولًا.
                    if (overlay != null && overlayBounds != null) {
                        val overlayPath = Path()
                        var overlayStarted = false
                        points.forEach { point ->
                            val reading = overlay.read(point) ?: return@forEach
                            val x = xOf(point.atMs)
                            val y = overlayY(reading)
                            if (overlayStarted) overlayPath.lineTo(x, y) else {
                                overlayPath.moveTo(x, y)
                                overlayStarted = true
                            }
                        }
                        if (overlayStarted) {
                            drawPath(
                                path = overlayPath,
                                color = overlayTone.copy(alpha = 0.85f),
                                style = Stroke(width = 3f, cap = StrokeCap.Round)
                            )
                        }
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
                            style = Stroke(
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
                        if (overlay != null && overlayBounds != null) {
                            overlay.read(selected)?.let { reading ->
                                drawCircle(color = overlayTone, radius = 6f, center = Offset(x, overlayY(reading)))
                            }
                        }
                        drawCircle(color = accent, radius = 7f, center = Offset(x, yOf(value)))
                    }
                }
                if (overlay != null && overlayBounds != null) {
                    Spacer(modifier = Modifier.width(MaxSpace.sm))
                    val overlayHigh = "%.${overlay.decimals}f".format(overlayBounds.endInclusive)
                    val overlayMid = "%.${overlay.decimals}f".format((overlayBounds.start + overlayBounds.endInclusive) / 2f)
                    val overlayLow = "%.${overlay.decimals}f".format(overlayBounds.start)
                    Column(
                        modifier = Modifier.height(chartHeight),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(overlayHigh, style = MonoValueStyleSmall, color = overlayTone)
                        Text(overlayMid, style = MonoValueStyleSmall, color = overlayTone)
                        Text(overlayLow, style = MonoValueStyleSmall, color = overlayTone)
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

            if (worst.isNotEmpty()) {
                Spacer(modifier = Modifier.height(MaxSpace.md))
                Text(
                    text = stringResource(R.string.game_sessions_report_worst),
                    style = MaterialTheme.typography.labelMedium,
                    color = variant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
                    worst.forEach { moment ->
                        val chipLabel = stringResource(
                            R.string.game_sessions_report_scrub,
                            "%.0f".format(moment.fps),
                            reportClock(moment.atMs - (points.firstOrNull()?.atMs ?: 0L))
                        )
                        FilterChip(
                            selected = scrubIndex == moment.index,
                            onClick = {
                                scrubIndex = if (scrubIndex == moment.index) null else moment.index
                            },
                            label = { Text(text = chipLabel, style = MonoValueStyleSmall) }
                        )
                    }
                }
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
