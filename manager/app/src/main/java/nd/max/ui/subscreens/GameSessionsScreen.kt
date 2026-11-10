/*
 * Copyright (C) 2026-2027 Zexshia
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */

package nd.max.ui.subscreens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.platform.GameSessionStore
import nd.max.core.platform.HudChartRenderer
import nd.max.core.platform.HudLive
import nd.max.core.platform.HudRecorder
import nd.max.core.platform.HudSessionRules
import nd.max.core.platform.HudSessionSettings
import nd.max.core.platform.HudTally
import nd.max.core.platform.SessionPoint
import nd.max.core.platform.SessionStopReason
import nd.max.core.platform.StoredGameSession
import nd.max.core.platform.sessionFrameBounds
import nd.max.core.platform.storedSessionCsv
import nd.max.ui.design.MaxDataTrust
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxMetric
import nd.max.ui.design.MaxMetricLine
import nd.max.ui.design.MaxNavigationRow
import nd.max.ui.design.MaxScreen
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSliderRow
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxSwitchRow
import nd.max.ui.design.MaxTone
import nd.max.ui.component.hudFieldLabel

/**
 * جلسات اللعب — سطحٌ واحد يجمع ما كان مبعثرًا: تسجيل حيّ، وقواعد إيقاف وتحذير، وسجلّ
 * دائم، ومنحنى، وتصدير CSV وصورة.
 *
 * والمسجّل هو [HudRecorder] نفسه الذي يعمل فوق اللعبة؛ فلا يوجد مسار تسجيل ثانٍ يختلف
 * في الأرقام أو في معنى الجلسة. الفرق الوحيد أن هذه الشاشة تسمّي الجلسة وتقرأ السجلّ
 * وتضبط قواعدها.
 */
@Composable
fun GameSessionsScreen(
    navController: NavController,
    viewModel: GameSessionsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snapshot by HudLive.snapshot.collectAsState()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme

    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var tally by remember { mutableStateOf(HudRecorder.tally()) }
    var series by remember { mutableStateOf(HudRecorder.series()) }
    var clockNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var rules by remember { mutableStateOf(HudSessionSettings.load(context)) }
    var remarkOpen by remember { mutableStateOf(false) }
    var remarkSessionId by remember { mutableStateOf<String?>(null) }
    var remarkDraft by remember { mutableStateOf("") }

    // السجلّ يُقرأ عند فتح الشاشة، ثم يُعاد بعد كل إيقاف: الحفظ يقع على IO بعد `stop`،
    // فقراءة فورية قد تسبقه. والمهلة القصيرة تُعطي الكتابة الذرّية وقتها بلا انتظار المستخدم.
    LaunchedEffect(Unit) { viewModel.refresh(context) }
    LaunchedEffect(snapshot.recording) {
        if (!snapshot.recording) {
            delay(350)
            viewModel.refresh(context)
        }
    }
    // نبضة العرض الحيّة: الإحصاء والمنحنى يُقرآن من الحائز كل ثانية، والمدّة من ساعة الجلسة
    // لا من `spanMs` وحده — فالجلسة التي تعمل الآن مدّتها تكبر أمام العين.
    LaunchedEffect(snapshot.recording) {
        while (true) {
            clockNowMs = System.currentTimeMillis()
            tally = HudRecorder.tally()
            series = HudRecorder.series()
            delay(1_000L)
        }
    }

    val recording = snapshot.recording
    val startedAtMs = HudRecorder.sessionStartedAtMs
    val elapsedMs = when {
        recording && startedAtMs > 0L -> (clockNowMs - startedAtMs).coerceAtLeast(0L)
        else -> tally?.spanMs ?: 0L
    }

    MaxScreen(
        title = stringResource(R.string.game_sessions_title),
        onBack = { navController.popBackStack() },
        subtitle = stringResource(R.string.game_sessions_subtitle),
        accentIcon = Icons.Rounded.History,
        accent = scheme.tertiary
    ) {
        MaxSection(
            title = stringResource(R.string.game_sessions_live_title),
            description = stringResource(R.string.game_sessions_live_desc)
        ) {
            MaxGroup {
                MaxSwitchRow(
                    title = if (recording) {
                        stringResource(R.string.hud_session_stop)
                    } else {
                        stringResource(R.string.hud_session_start)
                    },
                    subtitle = stringResource(R.string.hud_session_record_note),
                    checked = recording,
                    onCheckedChange = { on ->
                        if (on) {
                            HudRecorder.start(context)
                        } else {
                            HudRecorder.stop(context)
                        }
                        tally = HudRecorder.tally()
                        series = HudRecorder.series()
                    }
                )
            }

            val live = tally
            if (live == null) {
                Text(
                    text = stringResource(R.string.game_sessions_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
                )
            } else {
                MaxGroup {
                    MaxMetricLine(
                        MaxMetric(
                            label = stringResource(R.string.game_sessions_game),
                            value = HudRecorder.sessionLabel
                                ?: HudRecorder.sessionPackage
                                ?: stringResource(R.string.game_sessions_unknown_game),
                            trust = if (recording) MaxDataTrust.Live else MaxDataTrust.Snapshot
                        )
                    )
                    HudRecorder.sessionAppVersion?.let { version ->
                        MaxMetricLine(
                            MaxMetric(
                                label = stringResource(R.string.game_sessions_app_version),
                                value = version,
                                trust = MaxDataTrust.Snapshot
                            )
                        )
                    }
                    HudRecorder.sessionViewSize?.let { size ->
                        MaxMetricLine(
                            MaxMetric(
                                label = stringResource(R.string.game_sessions_view_size),
                                value = size,
                                trust = MaxDataTrust.Snapshot
                            )
                        )
                    }
                    MaxMetricLine(
                        MaxMetric(
                            label = stringResource(R.string.game_sessions_mode),
                            value = if (HudRecorder.sessionPowerSave) {
                                stringResource(R.string.game_sessions_mode_saver)
                            } else {
                                stringResource(R.string.game_sessions_mode_normal)
                            },
                            trust = MaxDataTrust.Snapshot
                        )
                    )
                    LiveSessionMetrics(live, elapsedMs, recording)
                }

                // تحذيرات العتبات: تُعرض هنا لأنها حالة الجلسة، ولونها تحذيريّ لا حياديّ.
                if (snapshot.warnings.isNotEmpty()) {
                    MaxGroup {
                        snapshot.warnings.forEach { warning ->
                            MaxMetricLine(
                                MaxMetric(
                                    label = stringResource(R.string.game_sessions_warning_active, hudFieldLabel(warning.field)),
                                    value = "%.1f".format(warning.value),
                                    unit = warningUnit(warning.field),
                                    trust = MaxDataTrust.Live,
                                    note = "≥ %.1f · %d s".format(
                                        warning.threshold,
                                        warning.durationMs / 1000
                                    )
                                )
                            )
                        }
                    }
                }

                MaxGroup {
                    MaxNavigationRow(
                        title = stringResource(R.string.game_sessions_remark),
                        subtitle = HudRecorder.remark ?: stringResource(R.string.game_sessions_remark_empty),
                        icon = Icons.Rounded.Edit,
                        onClick = {
                            remarkSessionId = null
                            remarkDraft = HudRecorder.remark.orEmpty()
                            remarkOpen = true
                        }
                    )
                }

                SessionChartSection(
                    points = series,
                    emptyText = stringResource(R.string.game_sessions_chart_empty)
                )

                MaxGroup {
                    MaxNavigationRow(
                        title = stringResource(R.string.game_sessions_export),
                        subtitle = stringResource(R.string.game_sessions_export_note),
                        onClick = { exportLive(context, scope) }
                    )
                    MaxGroupDivider()
                    MaxNavigationRow(
                        title = stringResource(R.string.game_sessions_export_image),
                        subtitle = stringResource(R.string.game_sessions_export_image_note),
                        icon = Icons.Rounded.Image,
                        onClick = {
                            exportChart(
                                context = context,
                                scope = scope,
                                points = series,
                                title = HudRecorder.sessionLabel ?: HudRecorder.sessionPackage
                                    ?: context.getString(R.string.game_sessions_title),
                                subtitle = sessionSubtitleText(elapsedMs, series.size)
                            )
                        }
                    )
                    MaxGroupDivider()
                    MaxNavigationRow(
                        title = stringResource(R.string.hud_session_clear),
                        icon = Icons.Rounded.Delete,
                        iconTone = MaxTone.Caution,
                        onClick = {
                            HudRecorder.clear()
                            tally = null
                            series = emptyList()
                        }
                    )
                }
            }
        }

        MaxSection(
            title = stringResource(R.string.game_sessions_settings_title),
            description = stringResource(R.string.game_sessions_settings_desc)
        ) {
            MaxGroup {
                MaxSwitchRow(
                    title = stringResource(R.string.game_sessions_autostop_foreground),
                    subtitle = stringResource(R.string.game_sessions_stop_foreground_note),
                    checked = rules.stopOnForegroundChange,
                    onCheckedChange = { on ->
                        rules = rules.copy(stopOnForegroundChange = on)
                        HudSessionSettings.save(context, rules)
                    }
                )
                MaxGroupDivider()
                MaxSwitchRow(
                    title = stringResource(R.string.game_sessions_autostop_power),
                    subtitle = stringResource(R.string.game_sessions_stop_power_note),
                    checked = rules.stopOnPowerChange,
                    onCheckedChange = { on ->
                        rules = rules.copy(stopOnPowerChange = on)
                        HudSessionSettings.save(context, rules)
                    }
                )
                MaxGroupDivider()
                MaxSwitchRow(
                    title = stringResource(R.string.game_sessions_autostop_screen),
                    subtitle = stringResource(R.string.game_sessions_stop_screen_note),
                    checked = rules.stopOnScreenOff,
                    onCheckedChange = { on ->
                        rules = rules.copy(stopOnScreenOff = on)
                        HudSessionSettings.save(context, rules)
                    }
                )
                MaxGroupDivider()
                MaxSwitchRow(
                    title = stringResource(R.string.game_sessions_clip),
                    subtitle = stringResource(R.string.game_sessions_clip_note),
                    checked = rules.clipTransitions,
                    onCheckedChange = { on ->
                        rules = rules.copy(clipTransitions = on)
                        HudSessionSettings.save(context, rules)
                    }
                )
            }
            MaxGroup {
                MaxSliderRow(
                    title = stringResource(R.string.game_sessions_warn_soc),
                    value = rules.socTempThresholdC,
                    valueText = "%.0f°C".format(rules.socTempThresholdC),
                    valueRange = 50f..100f,
                    steps = 49,
                    onValueChange = { rules = rules.copy(socTempThresholdC = it) },
                    onValueChangeFinished = { HudSessionSettings.save(context, rules) }
                )
                MaxGroupDivider()
                MaxSliderRow(
                    title = stringResource(R.string.game_sessions_warn_battery),
                    value = rules.batteryTempThresholdC,
                    valueText = "%.0f°C".format(rules.batteryTempThresholdC),
                    valueRange = 30f..60f,
                    steps = 29,
                    onValueChange = { rules = rules.copy(batteryTempThresholdC = it) },
                    onValueChangeFinished = { HudSessionSettings.save(context, rules) }
                )
                MaxGroupDivider()
                MaxSliderRow(
                    title = stringResource(R.string.game_sessions_warn_gpu),
                    value = rules.gpuLoadThresholdPercent,
                    valueText = "%.0f%%".format(rules.gpuLoadThresholdPercent),
                    valueRange = 40f..100f,
                    steps = 59,
                    onValueChange = { rules = rules.copy(gpuLoadThresholdPercent = it) },
                    onValueChangeFinished = { HudSessionSettings.save(context, rules) }
                )
                MaxGroupDivider()
                MaxSliderRow(
                    title = stringResource(R.string.game_sessions_warn_seconds),
                    value = rules.warningSeconds.toFloat(),
                    valueText = stringResource(
                        R.string.game_sessions_warn_seconds_value,
                        rules.warningSeconds
                    ),
                    valueRange = 0f..30f,
                    steps = 29,
                    onValueChange = { rules = rules.copy(warningSeconds = it.toInt()) },
                    onValueChangeFinished = { HudSessionSettings.save(context, rules) }
                )
            }
        }

        MaxSection(
            title = stringResource(R.string.game_sessions_history_title),
            description = stringResource(R.string.game_sessions_history_desc)
        ) {
            when {
                loading -> Text(
                    text = stringResource(R.string.game_sessions_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
                )

                sessions.isEmpty() -> Text(
                    text = stringResource(R.string.game_sessions_history_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
                )

                else -> {
                    sessions.forEach { session ->
                        MaxGroup {
                            MaxNavigationRow(
                                title = session.displayName,
                                subtitle = sessionSubtitle(session),
                                valueText = session.framesAverage?.let { "%.1f FPS".format(it) },
                                onClick = {
                                    expandedId = if (expandedId == session.id) null else session.id
                                }
                            )
                            if (expandedId == session.id) {
                                MaxMetricLine(
                                    MaxMetric(
                                        label = stringResource(R.string.game_sessions_package),
                                        value = session.packageName,
                                        trust = MaxDataTrust.Snapshot
                                    )
                                )
                                session.appVersion?.let { version ->
                                    MaxMetricLine(
                                        MaxMetric(
                                            label = stringResource(R.string.game_sessions_app_version),
                                            value = version,
                                            trust = MaxDataTrust.Snapshot
                                        )
                                    )
                                }
                                session.viewSize?.let { size ->
                                    MaxMetricLine(
                                        MaxMetric(
                                            label = stringResource(R.string.game_sessions_view_size),
                                            value = size,
                                            trust = MaxDataTrust.Snapshot
                                        )
                                    )
                                }
                                StoredSessionMetrics(session)
                                session.remark?.let { note ->
                                    MaxMetricLine(
                                        MaxMetric(
                                            label = stringResource(R.string.game_sessions_remark),
                                            value = note,
                                            trust = MaxDataTrust.Snapshot
                                        )
                                    )
                                }
                                SessionChartSection(
                                    points = session.series,
                                    emptyText = stringResource(R.string.game_sessions_chart_empty)
                                )
                                MaxGroupDivider()
                                MaxNavigationRow(
                                    title = stringResource(R.string.game_sessions_remark),
                                    subtitle = session.remark
                                        ?: stringResource(R.string.game_sessions_remark_empty),
                                    icon = Icons.Rounded.Edit,
                                    onClick = {
                                        remarkSessionId = session.id
                                        remarkDraft = session.remark.orEmpty()
                                        remarkOpen = true
                                    }
                                )
                                MaxGroupDivider()
                                MaxNavigationRow(
                                    title = stringResource(R.string.game_sessions_export),
                                    subtitle = stringResource(R.string.game_sessions_export_note),
                                    onClick = { exportStored(context, scope, session) }
                                )
                                MaxGroupDivider()
                                MaxNavigationRow(
                                    title = stringResource(R.string.game_sessions_export_image),
                                    subtitle = stringResource(R.string.game_sessions_export_image_note),
                                    icon = Icons.Rounded.Image,
                                    onClick = {
                                        exportChart(
                                            context = context,
                                            scope = scope,
                                            points = session.series,
                                            title = session.displayName,
                                            subtitle = sessionSubtitle(session)
                                        )
                                    }
                                )
                                MaxGroupDivider()
                                MaxNavigationRow(
                                    title = stringResource(R.string.game_sessions_delete),
                                    icon = Icons.Rounded.Delete,
                                    iconTone = MaxTone.Caution,
                                    onClick = {
                                        viewModel.delete(context, session.id)
                                        expandedId = null
                                    }
                                )
                            }
                        }
                    }
                    MaxGroup {
                        MaxNavigationRow(
                            title = stringResource(R.string.game_sessions_clear_history),
                            subtitle = stringResource(R.string.game_sessions_clear_history_note),
                            icon = Icons.Rounded.Delete,
                            iconTone = MaxTone.Caution,
                            onClick = { viewModel.clear(context) }
                        )
                    }
                }
            }
        }
    }

    if (remarkOpen) {
        AlertDialog(
            onDismissRequest = { remarkOpen = false },
            title = { Text(stringResource(R.string.game_sessions_remark_title)) },
            text = {
                OutlinedTextField(
                    value = remarkDraft,
                    onValueChange = { remarkDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.game_sessions_remark_hint)) },
                    minLines = 3,
                    maxLines = 6
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = remarkSessionId
                        if (target == null) {
                            HudRecorder.setRemark(remarkDraft)
                        } else {
                            viewModel.updateRemark(context, target, remarkDraft)
                        }
                        remarkOpen = false
                    }
                ) {
                    Text(stringResource(R.string.game_sessions_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { remarkOpen = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

/** وحدة التحذير بحسب الحقل — تُقرأ مع الرقم، لا تُترك للسياق. */
private fun warningUnit(field: nd.max.core.platform.HudField): String = when (field) {
    nd.max.core.platform.HudField.SocTemp, nd.max.core.platform.HudField.Heat -> "°C"
    nd.max.core.platform.HudField.GpuLoad -> "%"
    else -> ""
}

/** منحنى الإطارات: رسم بسيط بلا مكتبة، والغائب يكسر الخطّ لا يُوصل بصفر. */
@Composable
private fun SessionChartSection(points: List<SessionPoint>, emptyText: String) {
    val bounds = sessionFrameBounds(points)
    val lineColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    MaxGroup {
        Text(
            text = stringResource(R.string.game_sessions_chart),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
        )
        if (bounds == null) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
            )
        } else {
            val (low, high) = bounds
            val firstAt = points.first().atMs
            val lastAt = points.last().atMs
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .padding(horizontal = MaxSpace.groupPadding)
            ) {
                repeat(3) { step ->
                    val y = size.height * step / 2f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }
                val path = Path()
                var drawing = false
                points.forEach { point ->
                    val frames = point.frames
                    if (frames == null || !frames.isFinite() || frames <= 0f) {
                        drawing = false
                        return@forEach
                    }
                    val x = HudChartRenderer.horizontalFraction(point.atMs, firstAt, lastAt) * size.width
                    val fraction = HudChartRenderer.verticalFraction(frames, low, high)
                    val y = size.height - fraction * size.height
                    if (drawing) path.lineTo(x, y) else path.moveTo(x, y)
                    drawing = true
                }
                drawPath(path = path, color = lineColor, style = Stroke(width = 3f))
            }
        }
    }
}

/** مقاييس الجلسة الحيّة — كل صفّ يبقى شرطةً حتى تصل قراءته. */
@Composable
private fun LiveSessionMetrics(tally: HudTally, elapsedMs: Long, recording: Boolean) {
    val trust = if (recording) MaxDataTrust.Live else MaxDataTrust.Snapshot
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_duration),
            value = sessionClock(elapsedMs),
            trust = trust
        )
    )
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_samples),
            value = tally.samples.toString(),
            trust = trust
        )
    )
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_fps_avg),
            value = tally.framesAverage?.let { "%.1f".format(it) },
            unit = "FPS",
            trust = trust
        )
    )
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_fps_range),
            value = if (tally.framesLow != null && tally.framesHigh != null) {
                "%.0f – %.0f".format(tally.framesLow, tally.framesHigh)
            } else {
                null
            },
            unit = "FPS",
            trust = trust
        )
    )
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_heat_peak),
            value = tally.heatPeak?.let { "%.1f".format(it) },
            unit = "°C",
            trust = trust
        )
    )
    tally.socTempPeak?.let { peak ->
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.game_sessions_soc_peak),
                value = "%.1f".format(peak),
                unit = "°C",
                trust = trust
            )
        )
    }
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_power_avg),
            value = tally.powerAverage?.let { "%.2f".format(it) },
            unit = "W",
            trust = trust
        )
    )
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_energy),
            value = tally.energyWh?.let { "%.2f".format(it) },
            unit = "Wh",
            trust = trust,
            note = stringResource(R.string.game_sessions_energy_note)
        )
    )
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_jank_avg),
            value = tally.jankAverage?.toString(),
            unit = "%",
            trust = trust
        )
    )
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_p99),
            value = tally.frameP99AverageMs?.let { "%.1f".format(it) },
            unit = "ms",
            trust = trust
        )
    )
    tally.gpuLoadAverage?.let { load ->
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.game_sessions_gpu_load_avg),
                value = "%.0f".format(load),
                unit = "%",
                trust = trust
            )
        )
    }
    tally.cpuFreqAverageMhz?.let { mhz ->
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.game_sessions_cpu_freq_avg),
                value = mhz.toString(),
                unit = "MHz",
                trust = trust
            )
        )
    }
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_battery),
            value = batterySpan(tally.batteryStart, tally.batteryEnd),
            trust = trust
        )
    )
    tally.batteryVoltageMinMv?.let { mv ->
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.game_sessions_voltage_min),
                value = "%.2f".format(mv / 1000f),
                unit = "V",
                trust = trust
            )
        )
    }
    tally.renderer?.let { renderer ->
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.game_sessions_renderer),
                value = renderer,
                trust = trust
            )
        )
    }
    if (tally.droppedSamples > 0) {
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.game_sessions_dropped),
                value = tally.droppedSamples.toString(),
                trust = MaxDataTrust.Snapshot,
                note = stringResource(R.string.hud_session_dropped_note)
            )
        )
    }
}

/** مقاييس جلسة محفوظة — نفس ترتيب الحيّة، والمصدر لقطة لا بثّ. */
@Composable
private fun StoredSessionMetrics(session: StoredGameSession) {
    val trust = MaxDataTrust.Snapshot
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_duration),
            value = sessionClock(session.spanMs),
            trust = trust
        )
    )
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_samples),
            value = session.samples.toString(),
            trust = trust
        )
    )
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_fps_avg),
            value = session.framesAverage?.let { "%.1f".format(it) },
            unit = "FPS",
            trust = trust
        )
    )
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_fps_range),
            value = if (session.framesLow != null && session.framesHigh != null) {
                "%.0f – %.0f".format(session.framesLow, session.framesHigh)
            } else {
                null
            },
            unit = "FPS",
            trust = trust
        )
    )
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_heat_peak),
            value = session.heatPeak?.let { "%.1f".format(it) },
            unit = "°C",
            trust = trust
        )
    )
    session.socTempPeak?.let { peak ->
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.game_sessions_soc_peak),
                value = "%.1f".format(peak),
                unit = "°C",
                trust = trust
            )
        )
    }
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_power_avg),
            value = session.powerAverage?.let { "%.2f".format(it) },
            unit = "W",
            trust = trust
        )
    )
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_energy),
            value = session.energyWh?.let { "%.2f".format(it) },
            unit = "Wh",
            trust = trust
        )
    )
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_jank_avg),
            value = session.jankAverage?.toString(),
            unit = "%",
            trust = trust
        )
    )
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_p99),
            value = session.frameP99AverageMs?.let { "%.1f".format(it) },
            unit = "ms",
            trust = trust
        )
    )
    session.gpuLoadAverage?.let { load ->
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.game_sessions_gpu_load_avg),
                value = "%.0f".format(load),
                unit = "%",
                trust = trust
            )
        )
    }
    session.cpuFreqAverageMhz?.let { mhz ->
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.game_sessions_cpu_freq_avg),
                value = mhz.toString(),
                unit = "MHz",
                trust = trust
            )
        )
    }
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_battery),
            value = batterySpan(session.batteryStart, session.batteryEnd),
            trust = trust
        )
    )
    session.batteryVoltageMinMv?.let { mv ->
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.game_sessions_voltage_min),
                value = "%.2f".format(mv / 1000f),
                unit = "V",
                trust = trust
            )
        )
    }
    session.renderer?.let { renderer ->
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.game_sessions_renderer),
                value = renderer,
                trust = trust
            )
        )
    }
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_mode),
            value = if (session.powerSave) {
                stringResource(R.string.game_sessions_mode_saver)
            } else {
                stringResource(R.string.game_sessions_mode_normal)
            },
            trust = trust
        )
    )
    session.stopReason?.let { reason ->
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.game_sessions_stop_reason),
                value = stopReasonText(reason),
                trust = trust
            )
        )
    }
    if (session.clipRemoved > 0) {
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.game_sessions_clipped),
                value = session.clipRemoved.toString(),
                trust = trust
            )
        )
    }
    if (session.clipRejected) {
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.game_sessions_clip_rejected),
                value = null,
                trust = MaxDataTrust.Unreadable
            )
        )
    }
    if (session.droppedSamples > 0) {
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.game_sessions_dropped),
                value = session.droppedSamples.toString(),
                trust = MaxDataTrust.Snapshot,
                note = stringResource(R.string.hud_session_dropped_note)
            )
        )
    }
}

@Composable
private fun stopReasonText(id: String): String = when (SessionStopReason.of(id)) {
    SessionStopReason.Manual -> stringResource(R.string.game_sessions_stop_manual)
    SessionStopReason.ForegroundChanged -> stringResource(R.string.game_sessions_stop_foreground)
    SessionStopReason.PowerChanged -> stringResource(R.string.game_sessions_stop_power)
    SessionStopReason.ScreenOff -> stringResource(R.string.game_sessions_stop_screen)
    SessionStopReason.Cleared -> stringResource(R.string.game_sessions_stop_manual)
}

/** سطر الجلسة: التاريخ ثم المدّة — يُقارَن رأسيًّا بلا قراءة اسمين. */
private fun sessionSubtitle(session: StoredGameSession): String {
    val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        .format(Date(session.startedAtMs))
    return "$date · ${sessionClock(session.spanMs)}"
}

private fun sessionSubtitleText(elapsedMs: Long, samples: Int): String =
    "${sessionClock(elapsedMs)} · $samples samples"

private fun sessionClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun batterySpan(start: Int?, end: Int?): String? = when {
    start != null && end != null -> "$start% → $end%"
    start != null -> "$start%"
    end != null -> "$end%"
    else -> null
}

private fun exportLive(context: Context, scope: CoroutineScope) {
    val body = HudRecorder.csv()
    if (body == null) {
        Toast.makeText(context, context.getString(R.string.hud_session_empty), Toast.LENGTH_SHORT)
            .show()
        return
    }
    shareFile(context, scope, body.toByteArray(), "maxmanager-session", "text/csv", "csv")
}

private fun exportStored(context: Context, scope: CoroutineScope, session: StoredGameSession) {
    shareFile(
        context = context,
        scope = scope,
        bytes = storedSessionCsv(session).toByteArray(),
        prefix = "maxmanager-session-${session.id}",
        mime = "text/csv",
        extension = "csv"
    )
}

/**
 * صورة الجلسة: الرسم على خيط خلفيّ ثم مشاركة PNG.
 *
 * **والرسم على خيط آخر ليس ترفًا:** 1080×540 مع مئات النقاط يجمّد الواجهة إن وقع في
 * خيط الرسم، والمستخدم يرى «لم يحدث شيء» فيضغط مرّتين.
 */
private fun exportChart(
    context: Context,
    scope: CoroutineScope,
    points: List<SessionPoint>,
    title: String,
    subtitle: String?
) {
    scope.launch {
        val share = withContext(Dispatchers.Default) {
            runCatching {
                val bitmap = HudChartRenderer.render(points, title, subtitle)
                val folder = File(context.cacheDir, "hud").apply { mkdirs() }
                val file = File(folder, "maxmanager-session-${System.currentTimeMillis()}.png")
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                bitmap.recycle()
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    context.getString(R.string.game_sessions_export_image)
                )
            }.getOrNull()
        }
        if (share == null) {
            Toast.makeText(
                context,
                context.getString(R.string.game_sessions_image_failed),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            context.startActivity(share)
        }
    }
}

/**
 * الملفّ ثم حصّة مشاركة. يُكتب في `cacheDir` لا في التخزين المشترك: قياسٌ مؤقّت
 * لا يستحقّ أن يُلوّث التنزيلات، و`file_paths.xml` يصرّح بـ`cache-path`.
 */
private fun shareFile(
    context: Context,
    scope: CoroutineScope,
    bytes: ByteArray,
    prefix: String,
    mime: String,
    extension: String
) {
    scope.launch {
        val share = withContext(Dispatchers.IO) {
            runCatching {
                val folder = File(context.cacheDir, "hud").apply { mkdirs() }
                val file = File(folder, "$prefix-${System.currentTimeMillis()}.$extension")
                file.writeBytes(bytes)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = mime
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    context.getString(R.string.game_sessions_export)
                )
            }.getOrNull()
        }
        if (share == null) {
            Toast.makeText(
                context,
                context.getString(R.string.game_sessions_export_failed),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            context.startActivity(share)
        }
    }
}
