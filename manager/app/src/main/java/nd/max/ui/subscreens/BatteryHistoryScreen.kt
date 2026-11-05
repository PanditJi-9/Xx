/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import android.app.AlarmManager
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.battery.BatteryHistoryStore
import nd.max.core.battery.BatterySample
import nd.max.ui.design.MaxCondition
import nd.max.ui.design.MaxConditionKind
import nd.max.ui.design.MaxConditionNotice
import nd.max.ui.design.MaxConfirmDialog
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxScreen
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSegmented
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxSwitchRow
import nd.max.ui.theme.MonoValueStyleSmall
import java.util.Date
import java.util.Locale

/**
 * Battery history — the time axis that Diagnostics was missing.
 *
 * Diagnostics answers "what is the battery's health right now?"; this screen
 * answers "how did the level move over the last day, week or month?".
 *
 * **Source honesty:** every point is the platform's own sticky battery
 * broadcast plus `BATTERY_PROPERTY_CURRENT_NOW`. No value is modelled,
 * interpolated or inferred. The screen states how many points it actually has,
 * and refuses to draw a trend line from a single point.
 *
 * **Opt-in:** background recording is off until the switch is enabled. Turning
 * it on writes one point immediately and schedules an inexact 15-minute alarm;
 * turning it off cancels the alarm. Clearing deletes the local file only.
 */
@Composable
fun BatteryHistoryScreen(navController: NavHostController) {
    val context = LocalContext.current
    val timeFormat = remember { android.text.format.DateFormat.getTimeFormat(context) }
    val dateFormat = remember { android.text.format.DateFormat.getMediumDateFormat(context) }

    var period by remember { mutableStateOf(HistoryPeriod.DAY) }
    var samples by remember { mutableStateOf<List<BatterySample>>(emptyList()) }
    var current by remember { mutableStateOf<BatterySample?>(null) }
    var loading by remember { mutableStateOf(true) }
    var enabled by remember { mutableStateOf(BatteryHistoryStore.isEnabled(context)) }
    var reload by remember { mutableIntStateOf(0) }
    var showClear by remember { mutableStateOf(false) }
    var exportFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        enabled = BatteryHistoryStore.isEnabled(context)
        reload++
    }

    LaunchedEffect(period, reload) {
        loading = true
        val result = withContext(Dispatchers.IO) {
            if (BatteryHistoryStore.isEnabled(context)) {
                BatteryHistoryStore.recordNow(context)
                delay(120)
            }
            val cutoff = System.currentTimeMillis() - period.windowMs
            BatteryHistoryStore.read(context).filter { it.atMillis >= cutoff } to
                BatteryHistoryStore.readCurrent(context)
        }
        samples = result.first
        current = result.second
        loading = false
    }

    val latest = current ?: samples.lastOrNull()
    val levels = samples.map { it.levelPercent }
    val average = levels.takeIf { it.isNotEmpty() }?.average()
    val lowest = levels.minOrNull()
    val highest = levels.maxOrNull()
    val drain = dischargeRate(samples)
    val lastAt = samples.lastOrNull()?.atMillis
    val lastText = lastAt?.let {
        "${dateFormat.format(Date(it))} · ${timeFormat.format(Date(it))}"
    }

    MaxScreen(
        title = stringResource(R.string.battery_history_title),
        onBack = { navController.popBackStack() },
        subtitle = stringResource(R.string.battery_history_subtitle),
        accentIcon = Icons.Rounded.BatteryFull,
        actions = {
            IconButton(onClick = { reload++ }) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.battery_history_refresh)
                )
            }
        }
    ) {
        MaxSection(title = stringResource(R.string.battery_history_period)) {
            MaxSegmented(
                options = HistoryPeriod.entries.map { stringResource(it.labelRes) },
                selectedIndex = period.ordinal,
                onSelect = { period = HistoryPeriod.entries[it] }
            )
        }

        if (!loading && samples.isEmpty()) {
            MaxConditionNotice(
                condition = MaxCondition(
                    kind = MaxConditionKind.Empty,
                    title = stringResource(R.string.battery_history_empty_title),
                    detail = stringResource(R.string.battery_history_empty_detail)
                )
            )
        }

        if (samples.size >= 2) {
            MaxSection(
                title = stringResource(R.string.battery_history_chart_title),
                description = stringResource(R.string.battery_history_chart_desc)
            ) {
                MaxGroup {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MaxSpace.md, vertical = MaxSpace.md),
                        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
                    ) {
                        val chartDescription = stringResource(R.string.battery_history_chart_desc)
                        BatteryChart(
                            samples = samples,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .semantics {
                                    contentDescription = chartDescription
                                }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.battery_history_chart_low),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.battery_history_chart_high),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (samples.size >= 2) {
            MaxSection(
                title = stringResource(R.string.battery_history_temp_chart_title),
                description = stringResource(R.string.battery_history_temp_chart_desc)
            ) {
                val temperatureDescription =
                    stringResource(R.string.battery_history_temp_chart_desc)
                MaxGroup {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MaxSpace.md, vertical = MaxSpace.md),
                        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
                    ) {
                        TemperatureChart(
                            samples = samples,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .semantics {
                                    contentDescription = temperatureDescription
                                }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.battery_history_temp_chart_low),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.battery_history_temp_chart_high),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        MaxSection(
            title = stringResource(R.string.battery_history_summary_title),
            description = stringResource(R.string.battery_history_summary_desc)
        ) {
            MaxGroup {
                MaxRow(
                    title = stringResource(R.string.battery_history_current),
                    subtitle = latest?.let { statusText(it.status) },
                    trailing = {
                        Text(
                            text = latest?.let {
                                stringResource(R.string.battery_history_percent, it.levelPercent)
                            } ?: stringResource(R.string.device_lab_value_unavailable),
                            style = MonoValueStyleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                MaxGroupDivider()
                MaxRow(
                    title = stringResource(R.string.battery_history_average),
                    trailing = {
                        Text(
                            text = average?.let {
                                stringResource(
                                    R.string.battery_history_percent,
                                    it.toInt().coerceIn(0, 100)
                                )
                            } ?: stringResource(R.string.device_lab_value_unavailable),
                            style = MonoValueStyleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                MaxGroupDivider()
                MaxRow(
                    title = stringResource(R.string.battery_history_min_max),
                    trailing = {
                        Text(
                            text = if (lowest != null && highest != null) {
                                "${stringResource(R.string.battery_history_percent, lowest)} / " +
                                    stringResource(R.string.battery_history_percent, highest)
                            } else {
                                stringResource(R.string.device_lab_value_unavailable)
                            },
                            style = MonoValueStyleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                MaxGroupDivider()
                MaxRow(
                    title = stringResource(R.string.battery_history_drain),
                    subtitle = stringResource(R.string.battery_history_drain_desc),
                    trailing = {
                        Text(
                            text = drain?.let {
                                stringResource(
                                    R.string.battery_history_drain_value,
                                    String.format(Locale.getDefault(), "%.1f", it)
                                )
                            } ?: stringResource(R.string.device_lab_value_unavailable),
                            style = MonoValueStyleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                MaxGroupDivider()
                MaxRow(
                    title = stringResource(R.string.battery_history_samples),
                    trailing = {
                        Text(
                            text = samples.size.toString(),
                            style = MonoValueStyleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                MaxGroupDivider()
                MaxRow(
                    title = stringResource(R.string.battery_history_last_sample),
                    trailing = {
                        Text(
                            text = lastText ?: stringResource(R.string.device_lab_value_unavailable),
                            style = MonoValueStyleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                latest?.voltageMv?.let { voltage ->
                    MaxGroupDivider()
                    MaxRow(
                        title = stringResource(R.string.battery_history_voltage),
                        trailing = {
                            Text(
                                text = stringResource(
                                    R.string.battery_history_voltage_value,
                                    voltage.toString()
                                ),
                                style = MonoValueStyleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
                latest?.temperatureC?.let { temperature ->
                    MaxGroupDivider()
                    MaxRow(
                        title = stringResource(R.string.battery_history_temperature),
                        trailing = {
                            Text(
                                text = stringResource(
                                    R.string.battery_history_temperature_value,
                                    String.format(Locale.getDefault(), "%.1f", temperature)
                                ),
                                style = MonoValueStyleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }

        MaxSection(
            title = stringResource(R.string.battery_history_recording_title),
            description = stringResource(R.string.battery_history_recording_desc)
        ) {
            MaxGroup {
                MaxSwitchRow(
                    title = stringResource(R.string.battery_history_recording_switch),
                    subtitle = stringResource(R.string.battery_history_recording_switch_desc),
                    checked = enabled,
                    onCheckedChange = { next ->
                        enabled = next
                        BatteryHistoryStore.setEnabled(context, next)
                        reload++
                    }
                )
                MaxGroupDivider()
                MaxRow(
                    title = stringResource(R.string.battery_history_export),
                    subtitle = stringResource(R.string.battery_history_export_desc),
                    icon = Icons.Rounded.Share,
                    enabled = samples.isNotEmpty(),
                    onClick = {
                        exportFailed = false
                        scope.launch {
                            val file = withContext(Dispatchers.IO) {
                                BatteryHistoryStore.writeCsv(context, samples)
                            }
                            val uri = file?.let {
                                runCatching {
                                    FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        it
                                    )
                                }.getOrNull()
                            }
                            if (uri == null) {
                                exportFailed = true
                                return@launch
                            }
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(
                                        send,
                                        context.getString(R.string.battery_history_export_chooser)
                                    )
                                )
                            }.onFailure { exportFailed = true }
                        }
                    }
                )
                if (exportFailed) {
                    MaxGroupDivider()
                    MaxRow(
                        title = stringResource(R.string.battery_history_export_failed),
                        subtitle = stringResource(R.string.battery_history_export_failed_desc)
                    )
                }
                MaxGroupDivider()
                MaxRow(
                    title = stringResource(R.string.battery_history_clear),
                    subtitle = stringResource(R.string.battery_history_clear_desc),
                    icon = Icons.Rounded.DeleteOutline,
                    onClick = { showClear = true }
                )
            }
        }
    }

    MaxConfirmDialog(
        visible = showClear,
        title = stringResource(R.string.battery_history_clear_title),
        message = stringResource(R.string.battery_history_clear_message),
        confirmLabel = stringResource(R.string.battery_history_clear_confirm),
        onConfirm = {
            BatteryHistoryStore.clear(context)
            showClear = false
            reload++
        },
        onDismiss = { showClear = false },
        destructive = true,
        icon = Icons.Rounded.DeleteOutline
    )
}

private enum class HistoryPeriod(val windowMs: Long, val labelRes: Int) {
    DAY(AlarmManager.INTERVAL_DAY, R.string.battery_history_period_day),
    WEEK(7L * AlarmManager.INTERVAL_DAY, R.string.battery_history_period_week),
    MONTH(30L * AlarmManager.INTERVAL_DAY, R.string.battery_history_period_month)
}

/**
 * Percent per hour across the discharging points only.
 *
 * A charging point between two discharge points would make the slope
 * meaningless, so it is excluded. The function also refuses spans shorter than
 * fifteen minutes and negative drops (the level rose): both produce no number
 * rather than a confident wrong one.
 */
private fun dischargeRate(samples: List<BatterySample>): Float? {
    val discharging = samples.filter { it.discharging }
    if (discharging.size < 2) return null
    val first = discharging.first()
    val last = discharging.last()
    val hours = (last.atMillis - first.atMillis) / 3_600_000.0
    val drop = first.levelPercent - last.levelPercent
    if (hours < 0.25 || drop <= 0) return null
    return (drop / hours).toFloat()
}

@Composable
private fun statusText(status: Int): String = when (status) {
    BatteryManager.BATTERY_STATUS_CHARGING -> stringResource(R.string.device_lab_status_charging)
    BatteryManager.BATTERY_STATUS_DISCHARGING -> stringResource(R.string.device_lab_status_discharging)
    BatteryManager.BATTERY_STATUS_FULL -> stringResource(R.string.device_lab_status_full)
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> stringResource(R.string.device_lab_status_not_charging)
    else -> stringResource(R.string.device_lab_value_unavailable)
}

@Composable
private fun BatteryChart(samples: List<BatterySample>, modifier: Modifier = Modifier) {
    SeriesChart(
        values = samples.map { it.levelPercent.toFloat() },
        min = 0f,
        max = 100f,
        modifier = modifier
    )
}

/**
 * حرارة البطارية عبر النافذة نفسها.
 *
 * **والحدّان من البيانات لا من ثابت:** نطاق الحرارة يختلف بين جهاز وآخر؛ تثبيت ٠–١٠٠ كان
 * سيحوّل كل منحنى إلى خطٍّ مستقيم في وسطه، وهو رسم لا يقول شيئًا. فالحدّان هنا أدنى/أعلى ما
 * قِيس فعلًا، معامَدان بهامش بسيط.
 */
@Composable
private fun TemperatureChart(samples: List<BatterySample>, modifier: Modifier = Modifier) {
    val known = samples.mapNotNull { it.temperatureC }
    if (known.size < 2) return
    val low = known.minOrNull() ?: 0f
    val high = known.maxOrNull() ?: 0f
    val padding = ((high - low) * 0.15f).coerceAtLeast(1f)
    SeriesChart(
        values = samples.map { it.temperatureC },
        min = low - padding,
        max = high + padding,
        modifier = modifier,
        lineColor = MaterialTheme.colorScheme.tertiary
    )
}

/**
 * رسم خطّي لسلسلة قد تحوي فراغات.
 *
 * **والفراغ يُقطع لا يُوصل:** عيّنة بلا قراءة ليست صفرًا ولا تقفز إلى جارتها؛ الخطّ ينقطع
 * عندها ويُستأنف بعدها — فالرسم لا يدّعي قياسًا لم يُقَس.
 */
@Composable
private fun SeriesChart(
    values: List<Float?>,
    min: Float,
    max: Float,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val span = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val path = Path()
        var started = false
        values.forEachIndexed { index, value ->
            if (value == null) {
                started = false
                return@forEachIndexed
            }
            val x = index * stepX
            val y = size.height * (1f - ((value - min) / span).coerceIn(0f, 1f))
            if (started) path.lineTo(x, y) else path.moveTo(x, y)
            started = true
        }
        listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
            val y = size.height * fraction
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
