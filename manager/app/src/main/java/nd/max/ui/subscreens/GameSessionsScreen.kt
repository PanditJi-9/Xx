/*
 * Copyright (C) 2026-2027 Zexshia
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */

package nd.max.ui.subscreens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import nd.max.core.platform.HudLive
import nd.max.core.platform.HudRecorder
import nd.max.core.platform.HudTally
import nd.max.core.platform.StoredGameSession
import nd.max.core.platform.storedSessionCsv
import nd.max.ui.design.MaxDataTrust
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxMetric
import nd.max.ui.design.MaxMetricLine
import nd.max.ui.design.MaxNavigationRow
import nd.max.ui.design.MaxScreen
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxSwitchRow
import nd.max.ui.design.MaxTone

/**
 * جلسات اللعب — سطحٌ واحد يجمع ما كان مبعثرًا: تسجيل حيّ، وسجلّ دائم، وتصدير.
 *
 * والمسجّل هو [HudRecorder] نفسه الذي يعمل فوق اللعبة؛ فلا يوجد مسار تسجيل ثانٍ يختلف
 * في الأرقام أو في معنى الجلسة. الفرق الوحيد أن هذه الشاشة تسمّي الجلسة وتقرأ السجلّ.
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
    var clockNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // السجلّ يُقرأ عند فتح الشاشة، ثم يُعاد بعد كل إيقاف: الحفظ يقع على IO بعد `stop`،
    // فقراءة فورية قد تسبقه. والمهلة القصيرة تُعطي الكتابة الذرّية وقتها بلا انتظار المستخدم.
    LaunchedEffect(Unit) { viewModel.refresh(context) }
    LaunchedEffect(snapshot.recording) {
        if (!snapshot.recording) {
            delay(350)
            viewModel.refresh(context)
        }
    }
    // نبضة العرض الحيّة: الإحصاء يُقرأ من الحائز كل ثانية، والمدّة من ساعة الجلسة لا من
    // `spanMs` وحده — فالجلسة التي تعمل الآن مدّتها تكبر أمام العين.
    LaunchedEffect(snapshot.recording) {
        while (true) {
            clockNowMs = System.currentTimeMillis()
            tally = HudRecorder.tally()
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
                    LiveSessionMetrics(live, elapsedMs, recording)
                }
                MaxGroup {
                    MaxNavigationRow(
                        title = stringResource(R.string.game_sessions_export),
                        subtitle = stringResource(R.string.game_sessions_export_note),
                        onClick = { exportLive(context, scope) }
                    )
                    MaxGroupDivider()
                    MaxNavigationRow(
                        title = stringResource(R.string.hud_session_clear),
                        icon = Icons.Rounded.Delete,
                        iconTone = MaxTone.Caution,
                        onClick = {
                            HudRecorder.clear()
                            tally = null
                        }
                    )
                }
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
                                StoredSessionMetrics(session)
                                MaxGroupDivider()
                                MaxNavigationRow(
                                    title = stringResource(R.string.game_sessions_export),
                                    subtitle = stringResource(R.string.game_sessions_export_note),
                                    onClick = { exportStored(context, scope, session) }
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
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_battery),
            value = batterySpan(tally.batteryStart, tally.batteryEnd),
            trust = trust
        )
    )
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
    MaxMetricLine(
        MaxMetric(
            label = stringResource(R.string.game_sessions_battery),
            value = batterySpan(session.batteryStart, session.batteryEnd),
            trust = trust
        )
    )
    session.renderer?.let { renderer ->
        MaxMetricLine(
            MaxMetric(
                label = stringResource(R.string.game_sessions_renderer),
                value = renderer,
                trust = trust
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

/** سطر الجلسة: التاريخ ثم المدّة — يُقارَن رأسيًّا بلا قراءة اسمين. */
private fun sessionSubtitle(session: StoredGameSession): String {
    val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        .format(Date(session.startedAtMs))
    return "$date · ${sessionClock(session.spanMs)}"
}

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
    shareCsv(context, scope, body, "maxmanager-session")
}

private fun exportStored(context: Context, scope: CoroutineScope, session: StoredGameSession) {
    shareCsv(context, scope, storedSessionCsv(session), "maxmanager-session-${session.id}")
}

/**
 * الجلسة ملفًّا ثم حصّة مشاركة. تُكتب في `cacheDir` لا في التخزين المشترك: قياسٌ مؤقّت
 * لا يستحقّ أن يُلوّث التنزيلات، و`file_paths.xml` يصرّح بـ`cache-path`.
 */
private fun shareCsv(
    context: Context,
    scope: CoroutineScope,
    body: String,
    prefix: String
) {
    scope.launch {
        val share = withContext(Dispatchers.IO) {
            runCatching {
                val folder = File(context.cacheDir, "hud").apply { mkdirs() }
                val file = File(folder, "$prefix-${System.currentTimeMillis()}.csv")
                file.writeText(body)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
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
