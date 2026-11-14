/*
 * Copyright (C) 2026-2027 Zexshia
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */
/*
 * **Screen card — archetype: Ledger · object: one session ·
 * carrying element: the session report.** The page opens on a record action —
 * pick the game, then record — and the report below it is the page: name, date,
 * package, duration line, eight measured numbers and one frame curve that can
 * be dragged. Rows are entries in a record, newest first: opening a row unfolds
 * that session's own report directly underneath it, so the numbers appear where
 * the finger is. The live report at the top never changes source under the reader.
 *
 * States: nothing recorded → the record action plus the report's empty text ·
 * current recorder data → the report shows its running or last stopped session · row open →
 * that session's own report under the row, folded again by the first Back · a
 * session without samples → the chart empty text inside its own report.
 */

package nd.max.ui.subscreens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.gamespace.GameLibraryAccess
import nd.max.core.platform.HudChartRenderer
import nd.max.core.platform.HudLive
import nd.max.core.platform.HudRecorder
import nd.max.core.platform.HudSessionSettings
import nd.max.core.platform.SessionPoint
import nd.max.core.platform.InsightRules
import nd.max.core.platform.SessionAction
import nd.max.core.platform.SessionCause
import nd.max.core.platform.SessionVerdict
import nd.max.core.platform.sessionInsight
import nd.max.core.platform.SessionStopReason
import nd.max.core.platform.StoredGameSession
import nd.max.core.platform.storedLedgerJson
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
import nd.max.ui.navigation.MaxDestination

/**
 * جلسات اللعب — سطحٌ واحد يجمع ما كان مبعثرًا: تسجيل حيّ، وقواعد إيقاف وتحذير، وسجلّ
 * دائم، ومنحنى، وتصدير CSV وصورة.
 *
 * والمسجّل هو [HudRecorder] نفسه الذي يعمل فوق اللعبة؛ فلا يوجد مسار تسجيل ثانٍ يختلف
 * في الأرقام أو في معنى الجلسة. الفرق الوحيد أن هذه الشاشة تسمّي الجلسة وتقرأ السجلّ
 * وتضبط قواعدها.
 *
 * الشكل: Ledger — الكائن: جلسة واحدة — العنصر الحامل: تقرير الجلسة (رأس، ثمانية أرقام،
 * منحنى يُسحب). والزرّ الأعلى يسجّل **لعبة بعينها**: تختارها فتبدأ الجلسة بحزمتها.
 * والتقرير العلوي لبيانات المسجّل الحالية؛ وصفّ السجلّ يحمل حكمه، وفَتْحُه يُنزل **تقريره هو**
 * تحته (تكملة ٨٨) — فلا يُقرأ الرقم في غير موضع إصبعه، ولا يُفتح تقريرا صفّين معًا.
 */
@Composable
fun GameSessionsScreen(
    navController: NavController,
    viewModel: GameSessionsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // M4: السجلّ لا يقرأ اللقطة الحيّة كاملةً. `HudLive.snapshot` يتغيّر مع كل عيّنة إطارات
    // (كل ثانية)، وكانت الشاشة — بصفوف عتباتها وسجلّها المفتوح — تُعاد تركيبها مع كل عيّنة وهي
    // لا تحتاج من اللقطة إلا حالتين: أيجري تسجيل؟ وهل هناك تحذير؟ فاشتُقّتا هنا، ولا تُعاد
    // التركيبة إلا عند تغيّرهما فعلًا.
    val recording by remember { derivedStateOf { HudLive.snapshot.value.recording } }
    val warnings by remember { derivedStateOf { HudLive.snapshot.value.warnings } }
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme

    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    // الرجوع يطوي التقرير المحلي أولًا ويحفظ موضع القارئ في السجلّ؛ والرجوع التالي يخرج.
    BackHandler(enabled = expandedId != null) { expandedId = null }
    var tally by remember { mutableStateOf(HudRecorder.tally()) }
    var series by remember { mutableStateOf(HudRecorder.series()) }
    var clockNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var rules by remember { mutableStateOf(HudSessionSettings.load(context)) }
    var remarkOpen by remember { mutableStateOf(false) }
    var remarkSessionId by remember { mutableStateOf<String?>(null) }
    var remarkDraft by remember { mutableStateOf("") }
    var pickerOpen by remember { mutableStateOf(false) }

    // السجلّ يُقرأ عند فتح الشاشة، ثم يُعاد بعد كل إيقاف: الحفظ يقع على IO بعد `stop`،
    // فقراءة فورية قد تسبقه. والمهلة القصيرة تُعطي الكتابة الذرّية وقتها بلا انتظار المستخدم.
    LaunchedEffect(Unit) { viewModel.refresh(context) }
    LaunchedEffect(recording) {
        if (!recording) {
            delay(350)
            viewModel.refresh(context)
        }
    }
    // نبضة العرض الحيّة: الإحصاء والمنحنى يُقرآن من الحائز كل ثانية، والمدّة من ساعة الجلسة
    // لا من `spanMs` وحده — فالجلسة التي تعمل الآن مدّتها تكبر أمام العين.
    //
    // M4: **لا نبضة بلا تسجيل.** كانت الحلقة تدور دائمًا فتكتب `clockNowMs` كل ثانية، فيُعاد
    // تركيب السجلّ كله — صفوفه المحفوظة وقواعد التسجيل وشرائح العتبات — ولا رقم منها يتغيّر.
    // الآن تُقرأ القيم مرة عند الفتح، وتنبض أثناء التسجيل وحده، فتسكن الشاشة حين لا شيء يجري.
    LaunchedEffect(recording) {
        clockNowMs = System.currentTimeMillis()
        tally = HudRecorder.tally()
        series = HudRecorder.series()
        while (recording) {
            delay(1_000L)
            clockNowMs = System.currentTimeMillis()
            tally = HudRecorder.tally()
            series = HudRecorder.series()
        }
    }

    val startedAtMs = HudRecorder.sessionStartedAtMs
    val elapsedMs = when {
        recording && startedAtMs > 0L -> (clockNowMs - startedAtMs).coerceAtLeast(0L)
        else -> tally?.spanMs ?: 0L
    }
    // الحامل العلوي لبيانات المسجّل الحالية؛ وتقرير الجلسة المحفوظة ينزل تحت صفّها —
    // تقرير محفوظ واحد مفتوح: صفٌّ مغلق لا يرسم، والتقرير العلوي لا يتبدّل مصدره.
    MaxScreen(
        title = stringResource(R.string.game_sessions_title),
        onBack = {
            if (expandedId != null) expandedId = null else navController.popBackStack()
        },
        subtitle = stringResource(R.string.game_sessions_subtitle),
        accentIcon = Icons.Rounded.History,
        accent = scheme.tertiary
    ) {
        // ── ١. سجّل لعبة بعينها ────────────────────────────────────────────────
        // طلب المالك: زرّ يسجّل **لعبة معيّنة**. فالاختيار يسبق الجلسة، ويبدأ التسجيل
        // بحزمة اللعبة المختارة (`HudRecorder.start(context, pkg)`) ثم تُفتح — لا «بما
        // يصادف أنه في المقدمة». والتقرير يحمل الجلسة الجارية وحدها بعد ذلك.
        MaxSection(title = stringResource(R.string.game_sessions_live_title)) {
            MaxGroup {
                if (recording) {
                    MaxNavigationRow(
                        title = HudRecorder.sessionLabel
                            ?: HudRecorder.sessionPackage
                            ?: stringResource(R.string.game_sessions_unknown_game),
                        subtitle = stringResource(
                            R.string.game_sessions_record_elapsed,
                            reportClock(elapsedMs)
                        ),
                        icon = Icons.Rounded.Stop,
                        iconTone = MaxTone.Caution,
                        showChevron = false,
                        onClick = {
                            HudRecorder.stop(context)
                            tally = HudRecorder.tally()
                            series = HudRecorder.series()
                        }
                    )
                } else {
                    MaxNavigationRow(
                        title = stringResource(R.string.game_sessions_record_game),
                        subtitle = stringResource(R.string.game_sessions_record_game_desc),
                        icon = Icons.Rounded.FiberManualRecord,
                        onClick = { pickerOpen = true }
                    )
                }
            }

            // تحذيرات العتبات: حالة الجلسة لا بطاقة إعداد.
            if (warnings.isNotEmpty()) {
                MaxGroup {
                    warnings.forEach { warning ->
                        MaxMetricLine(
                            MaxMetric(
                                label = stringResource(
                                    R.string.game_sessions_warning_active,
                                    hudFieldLabel(warning.field)
                                ),
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

            // أفعال الجلسة الحيّة في بطاقة واحدة: صفّ لكلٍّ كان يُقرأ فراغًا.
            if (tally != null) {
                MaxGroup {
                    MaxNavigationRow(
                        title = stringResource(R.string.game_sessions_remark),
                        subtitle = HudRecorder.remark
                            ?: stringResource(R.string.game_sessions_remark_empty),
                        icon = Icons.Rounded.Edit,
                        onClick = {
                            remarkSessionId = null
                            remarkDraft = HudRecorder.remark.orEmpty()
                            remarkOpen = true
                        }
                    )
                    MaxGroupDivider()
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
                        showChevron = false,
                        onClick = {
                            HudRecorder.clear()
                            tally = null
                            series = emptyList()
                        }
                    )
                }
            }
        }

        // ── ٢. تقرير بيانات المسجّل الحالية: الحامل الأول ────────────────────
        // الجلسة المفتوحة من السجلّ لا تبدّله: تقريرها ينزل تحت صفّها في قسم السجلّ،
        // فما يقرأه القارئ هنا لا يتغيّر تحت إصبعه.
        val liveStats = remember(tally, series, elapsedMs) {
            liveReportStats(tally, series, elapsedMs)
        }
        // **التقرير العلوي للمسجّل وحده (تكملة ٨٨):** كان يتغيّر بلا إعلان
        // حين يُفتح صفّ في السجلّ — فيقرأ المستخدم أرقامًا لا يعرف من أين جاءت.
        // والصفّ الآن يفتح تقريره تحته، فلم يبقَ للتقرير العلويّ مصدر ثانٍ.
        val reportStats = liveStats
        val reportPoints = series
        val reportLive = recording
        val reportTitle = HudRecorder.sessionLabel
            ?: HudRecorder.sessionPackage
            ?: stringResource(R.string.game_sessions_unknown_game)
        val reportPackage = HudRecorder.sessionPackage
        val reportDate = HudRecorder.sessionStartedAtMs
        val reportMode = if (reportStats.powerSave) {
            stringResource(R.string.game_sessions_mode_saver)
        } else {
            stringResource(R.string.game_sessions_mode_normal)
        }
        val reportMeta = buildList {
            add(stringResource(R.string.game_sessions_report_meta_duration, reportClock(reportStats.durationMs)))
            add(stringResource(R.string.game_sessions_report_meta_profile, reportMode))
            reportStats.refreshHz?.let { add(stringResource(R.string.game_sessions_report_meta_refresh, it)) }
            if (!reportLive) {
                reportStats.stopReasonId?.let {
                    add(stringResource(R.string.game_sessions_report_meta_stopped, stopReasonText(it)))
                }
            }
        }.joinToString(" · ")

        GameSessionReport(
            title = reportTitle,
            packageName = reportPackage,
            dateMs = reportDate,
            meta = reportMeta,
            stats = reportStats,
            points = reportPoints,
            live = reportLive,
            accent = scheme.tertiary,
            emptyText = stringResource(R.string.game_sessions_empty)
        )

        // ── حكم أحدث جلسة محفوظة: الرقم وحده لا يقول «جيد أم سيئ» ──
        // ويُسمّى باسمها في أول سطر، ولا يظهر فوق جلسة جارية لم تُحفظ بعد — فمقارنة
        // جارية بأرشيف سابق تُقرأ حكمًا على ما لم ينتهِ.
        val lastSession = sessions.firstOrNull()
        if (lastSession != null && !reportLive) {
            val insight = remember(lastSession.id, sessions.size) {
                sessionInsight(lastSession, sessions)
            }
            MaxSection(
                title = stringResource(R.string.game_sessions_insight_title),
                description = if (insight.baselineCount >= 2) {
                    stringResource(R.string.game_sessions_insight_note, insight.baselineCount)
                } else {
                    null
                }
            ) {
                MaxGroup {
                    MaxMetricLine(
                        MaxMetric(
                            label = lastSession.displayName,
                            value = stringResource(verdictText(insight.verdict)),
                            trust = MaxDataTrust.Snapshot
                        )
                    )
                    insight.framesDeltaPercent?.let { delta ->
                        val signed = if (delta > 0) "+$delta" else delta.toString()
                        MaxMetricLine(
                            MaxMetric(
                                label = stringResource(R.string.game_sessions_fps_avg),
                                value = stringResource(R.string.game_sessions_vs_baseline, signed),
                                trust = MaxDataTrust.Snapshot
                            )
                        )
                    }
                    insight.worstPoint?.let { point ->
                        val frames = point.frames
                        if (frames != null) {
                            // `atMs` طابع زمني مطلق (epoch) من المسجّل، والقراءة هنا مدة من بدء الجلسة:
                            // طرح أول عيّنة هو ما يجعل «عند 0:07» صحيحًا بدل «عند 498422:15:16».
                            val origin = lastSession.series.firstOrNull()?.atMs ?: point.atMs
                            MaxMetricLine(
                                MaxMetric(
                                    label = stringResource(R.string.game_sessions_worst_moment),
                                    value = stringResource(
                                        R.string.game_sessions_worst_value,
                                        frames.roundToInt(),
                                        sessionClock(point.atMs - origin)
                                    ),
                                    trust = MaxDataTrust.Snapshot
                                )
                            )
                        }
                    }
                }
                when (insight.verdict) {
                    SessionVerdict.FirstRun -> Text(
                        text = stringResource(R.string.game_sessions_insight_first),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
                    )
                    SessionVerdict.Unusable -> Text(
                        text = stringResource(
                            R.string.game_sessions_insight_unusable,
                            InsightRules().minSamples
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
                    )
                    else -> if (insight.cause != SessionCause.None) {
                        Text(
                            text = stringResource(causeText(insight.cause)),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
                        )
                    }
                }
                val actionRoute = when (insight.action) {
                    SessionAction.Thermal -> MaxDestination.ThermalHub.route
                    SessionAction.Display -> MaxDestination.DisplayHub.route
                    SessionAction.Power -> MaxDestination.PowerHub.route
                    SessionAction.Renderer -> MaxDestination.DisplayHub.route
                    SessionAction.None -> null
                }
                if (actionRoute != null) {
                    MaxGroupDivider()
                    MaxNavigationRow(
                        title = stringResource(actionText(insight.action)),
                        onClick = { navController.navigate(actionRoute) }
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
                    MaxGroup {
                        MaxNavigationRow(
                            title = stringResource(R.string.game_sessions_export_all),
                            subtitle = stringResource(R.string.game_sessions_export_all_note, sessions.size),
                            onClick = { exportLedger(context, scope, sessions) }
                        )
                    }
                    sessions.forEach { session ->
                        val rowInsight = remember(session.id, sessions.size) {
                            sessionInsight(session, sessions)
                        }
                        MaxGroup {
                            MaxNavigationRow(
                                title = session.displayName,
                                subtitle = sessionSubtitle(session) + " · " +
                                    stringResource(verdictText(rowInsight.verdict)),
                                valueText = session.framesAverage?.let { "%.1f FPS".format(it) },
                                icon = Icons.Rounded.History,
                                iconTone = when (rowInsight.verdict) {
                                    SessionVerdict.Better -> MaxTone.Positive
                                    SessionVerdict.Worse, SessionVerdict.Unusable -> MaxTone.Caution
                                    else -> MaxTone.Neutral
                                },
                                onClick = {
                                    expandedId = if (expandedId == session.id) null else session.id
                                }
                            )
                        }
                        if (expandedId == session.id) {
                            // التقرير بطاقة مستقلة مباشرة تحت صفّه؛ لا بطاقة داخل بطاقة، ولا
                            // تغيير صامت لتقرير الجلسة الجارية في أعلى الصفحة.
                            val stored = remember(session) { storedReportStats(session) }
                            val storedMeta = buildList {
                                add(stringResource(
                                    R.string.game_sessions_report_meta_duration,
                                    reportClock(stored.durationMs),
                                ))
                                add(stringResource(
                                    R.string.game_sessions_report_meta_profile,
                                    stringResource(
                                        if (stored.powerSave) R.string.game_sessions_mode_saver
                                        else R.string.game_sessions_mode_normal
                                    ),
                                ))
                                stored.refreshHz?.let {
                                    add(stringResource(R.string.game_sessions_report_meta_refresh, it))
                                }
                                stored.stopReasonId?.let {
                                    add(stringResource(
                                        R.string.game_sessions_report_meta_stopped,
                                        stopReasonText(it),
                                    ))
                                }
                            }.joinToString(" · ")
                            GameSessionReport(
                                title = session.displayName,
                                packageName = session.packageName,
                                dateMs = session.startedAtMs,
                                meta = storedMeta,
                                stats = stored,
                                points = session.series,
                                live = false,
                                accent = scheme.tertiary,
                                emptyText = stringResource(R.string.game_sessions_empty),
                            )
                            MaxGroup {
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
                                    showChevron = false,
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
                            showChevron = false,
                            onClick = {
                                expandedId = null
                                viewModel.clear(context)
                            }
                        )
                    }
                }
            }
        }

        // الإعدادات آخر الصفحة ومطويّة: تقرير اللعبة هو السؤال، والقواعد تُضبط مرّة.
        MaxSection(
            title = stringResource(R.string.game_sessions_settings_title),
            description = stringResource(R.string.game_sessions_settings_desc),
            collapsible = true
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
                MaxGroupDivider()
                MaxSwitchRow(
                    title = stringResource(R.string.game_sessions_auto_start),
                    subtitle = stringResource(R.string.game_sessions_auto_start_note),
                    checked = rules.autoStart,
                    onCheckedChange = { on ->
                        rules = rules.copy(autoStart = on)
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
                    // بلا `steps`: مئات النقاط على المسار تُقرأ خطًّا منقّطًا لا مقياسًا.
                    // الصحيح يبقى صحيحًا لأن القيمة العائدة تُدوَّر قبل الحفظ.
                    steps = 0,
                    onValueChange = { rules = rules.copy(socTempThresholdC = it.roundToInt().toFloat()) },
                    onValueChangeFinished = { HudSessionSettings.save(context, rules) }
                )
                MaxGroupDivider()
                MaxSliderRow(
                    title = stringResource(R.string.game_sessions_warn_battery),
                    value = rules.batteryTempThresholdC,
                    valueText = "%.0f°C".format(rules.batteryTempThresholdC),
                    valueRange = 30f..60f,
                    steps = 0,
                    onValueChange = { rules = rules.copy(batteryTempThresholdC = it.roundToInt().toFloat()) },
                    onValueChangeFinished = { HudSessionSettings.save(context, rules) }
                )
                MaxGroupDivider()
                MaxSliderRow(
                    title = stringResource(R.string.game_sessions_warn_gpu),
                    value = rules.gpuLoadThresholdPercent,
                    valueText = "%.0f%%".format(rules.gpuLoadThresholdPercent),
                    valueRange = 40f..100f,
                    steps = 0,
                    onValueChange = { rules = rules.copy(gpuLoadThresholdPercent = it.roundToInt().toFloat()) },
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
                    steps = 0,
                    onValueChange = { rules = rules.copy(warningSeconds = it.roundToInt()) },
                    onValueChangeFinished = { HudSessionSettings.save(context, rules) }
                )
            }
        }
    }

    if (pickerOpen) {
        GamePickerSheet(
            visible = true,
            onDismiss = { pickerOpen = false },
            onPick = { app ->
                pickerOpen = false
                // الترتيب مقصود: التسجيل بالحزمة أولًا ثم فتح اللعبة، فلا تضيع أول ثانية
                // من الجلسة قبل أن يعرف المسجّل صاحبها.
                HudRecorder.start(context, app.packageName)
                tally = HudRecorder.tally()
                series = HudRecorder.series()
                GameLibraryAccess.launch(context, app.packageName)
            }
        )
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

private fun exportLive(context: Context, scope: CoroutineScope) {
    val body = HudRecorder.csv()
    if (body == null) {
        Toast.makeText(context, context.getString(R.string.hud_session_empty), Toast.LENGTH_SHORT)
            .show()
        return
    }
    shareFile(context, scope, body.toByteArray(), "maxmanager-session", "text/csv", "csv")
}

/**
 * السجلّ كاملًا ملفًّا واحدًا: لمشاركة ما عندك أو قراءته بأداة، لا لرسم اللوحة.
 *
 * والحدّ نفسه الذي في [storedLedgerJson]: أحدث جلسات السقف فقط، والظرف يقول إن كان قد قُصّ.
 */
private fun exportLedger(context: Context, scope: CoroutineScope, sessions: List<StoredGameSession>) {
    if (sessions.isEmpty()) {
        Toast.makeText(context, context.getString(R.string.game_sessions_history_empty), Toast.LENGTH_SHORT)
            .show()
        return
    }
    val body = storedLedgerJson(sessions, System.currentTimeMillis())
    shareFile(context, scope, body.toByteArray(), "max-sessions", "application/json", "json")
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

@StringRes
private fun verdictText(verdict: SessionVerdict): Int = when (verdict) {
    SessionVerdict.Better -> R.string.game_sessions_verdict_better
    SessionVerdict.Similar -> R.string.game_sessions_verdict_similar
    SessionVerdict.Worse -> R.string.game_sessions_verdict_worse
    SessionVerdict.FirstRun -> R.string.game_sessions_verdict_first
    SessionVerdict.Unusable -> R.string.game_sessions_verdict_unusable
}

@StringRes
private fun causeText(cause: SessionCause): Int = when (cause) {
    SessionCause.Thermal -> R.string.session_cause_thermal
    SessionCause.Jank -> R.string.session_cause_jank
    SessionCause.Energy -> R.string.session_cause_energy
    SessionCause.PowerSave -> R.string.session_cause_power_save
    SessionCause.Refresh -> R.string.session_cause_refresh
    SessionCause.Renderer -> R.string.session_cause_renderer
    SessionCause.None -> 0
}

@StringRes
private fun actionText(action: SessionAction): Int = when (action) {
    SessionAction.Thermal -> R.string.game_sessions_action_thermal
    SessionAction.Display -> R.string.game_sessions_action_display
    SessionAction.Power -> R.string.game_sessions_action_power
    SessionAction.Renderer -> R.string.game_sessions_action_renderer
    SessionAction.None -> 0
}
