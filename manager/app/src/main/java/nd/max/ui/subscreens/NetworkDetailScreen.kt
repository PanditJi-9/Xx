/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * شاشة الشبكة — **آخر شاشة كانت تُبنى بلغة لوحة البداية**، وهذه إعادة بنائها بلغة التطبيق.
 *
 * **الشكوى وأين قِيست:** «شكل شاشة الشبكة غير متناسق مع باقي التطبيق». والقياس لا الذوق: هذه
 * الشاشة كانت الوحيدة في التطبيق التي تحمل شريطها الخاص (`DetailTopBar`)، وبطاقاتها الخاصة
 * (`DetailStatCard` فوق `DashCardWrapper`)، وحبّة خاصة (`DetailPill` على `Surface`)، وقائمة
 * `LazyColumn` بحشو خاص (`DetailListPadding`) — أي **أربعة** أنظمة بديلة بينما ٢٨ شاشة تقرأ من
 * `MaxListScreen` + `MaxSection`/`MaxGroup`/`MaxRow`. وهي أيضًا الشاشة التي وصفها تعليق الملفّ
 * القديم بأنها «تبقى هنا حتى تُعاد بناؤها بنفس الطريقة» بعد أن نُقلت شاشتا الحرارة والتخزين
 * وأُعيد بناؤهما. وهذه هي تلك الإعادة.
 *
 * **وما يقرأه المستخدم وقيمةُ كل رقم لم تتغيّر** — القياس نفسه (`TrafficStats`) والفاصل نفسه
 * (ثانية واحدة) وأربعة أرقام حيّة ونافذة ٣٦ عيّنة للرسم الزمني. وما تغيّر هو اللغة التي تُعرض بها.
 *
 * **وأُضيف حال واحد كان مسكوتًا عنه:** `TrafficStats` يُعيد `-1` (`UNSUPPORTED`) على جهاز لا
 * يعرض العدّادات، وكانت الشاشة تُمرّره عبر `coerceAtLeast(0)` فتعرض **صفرًا** وتقول للشاشة
 * «الشبكة هادئة» — أي تُخبر عن جهل كأنه نتيجة، وهو عكس `ADR-07` نصًّا. الآن يُعلن غير المدعوم
 * بحالة صريحة تحمل مصدرها (`TrafficStats`)، كما تفعل شاشة الحرارة مع جهاز بلا مناطق حرارية.
 */
package nd.max.ui.subscreens

import android.net.TrafficStats
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nd.max.R
import nd.max.core.net.LinkProbe
import nd.max.core.net.LinkReport
import nd.max.ui.component.LivePulseDot
import nd.max.ui.component.MaxDeviceInfoShortcut
import nd.max.ui.design.MaxAlpha
import nd.max.ui.design.MaxCardShell
import nd.max.ui.design.MaxCondition
import nd.max.ui.design.MaxConditionKind
import nd.max.ui.design.MaxDataTrust
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxHelpAction
import nd.max.ui.design.MaxListScreen
import nd.max.ui.design.MaxMetric
import nd.max.ui.design.MaxMetricLine
import nd.max.ui.design.MaxNavigationRow
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.navigation.MaxDestination
import nd.max.ui.mainscreens.formatNetSpeed
import nd.max.ui.theme.MonoValueStyleSmall

/** عدّادات أندرويد تُقرأ مرة كل ثانية — الفاصل لم يتغيّر عن الشاشة السابقة. */
private const val SAMPLE_INTERVAL_MS = 1_000L

/** نافذة الرسم الزمني: آخر ٣٦ عيّنة (٣٦ ثانية) — الرقم نفسه الذي كان محفوظًا في الشاشة السابقة. */
private const val HISTORY_CAPACITY = 36

/** مسار الاشتقاق كما يُعلن للمستخدم؛ `TrafficStats` هو المصدر الحقيقي الوحيد في هذه الشاشة. */
private const val TRAFFIC_SOURCE = "TrafficStats"

/** مصدر قسم جودة الوصلة: قياس حقيقي عند الطلب، لا عدّاد منصّة. */
private const val PROBE_SOURCE = "LinkProbe"

/** ارتفاع الرسم الزمني بالضبط كما كان، فلا يصغر السطح بعد إعادة البناء. */
private val SparklineHeight = 168.dp

@Composable
fun NetworkDetailScreen(navController: NavHostController) {
    var history by remember { mutableStateOf<List<Long>>(emptyList()) }
    var downloadKbps by remember { mutableLongStateOf(0L) }
    var uploadKbps by remember { mutableLongStateOf(0L) }
    var peakDownloadKbps by remember { mutableLongStateOf(0L) }
    var peakUploadKbps by remember { mutableLongStateOf(0L) }
    // مجهول حتى يثبت العكس: `-1` من `TrafficStats` يعني «هذا الجهاز لا يعرضها»، لا «صفر».
    var countersReadable by remember { mutableStateOf(true) }

    // قياس جودة الوصلة: لا يبدأ من نفسه. `null` = لم يُقس بعد، وهو حالة لا رقم.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var linkReport by remember { mutableStateOf<LinkReport?>(null) }
    var probing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        while (true) {
            delay(SAMPLE_INTERVAL_MS)
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            if (rx < 0L || tx < 0L) {
                countersReadable = false
                continue
            }
            countersReadable = true
            downloadKbps = if (rx > lastRx) (rx - lastRx) / 1024L else 0L
            uploadKbps = if (tx > lastTx) (tx - lastTx) / 1024L else 0L
            lastRx = rx
            lastTx = tx
            peakDownloadKbps = maxOf(peakDownloadKbps, downloadKbps)
            peakUploadKbps = maxOf(peakUploadKbps, uploadKbps)
            history = (history + downloadKbps).takeLast(HISTORY_CAPACITY)
        }
    }

    val accent = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val totalRx = TrafficStats.getTotalRxBytes()
    val totalTx = TrafficStats.getTotalTxBytes()
    val live = downloadKbps > 0L || uploadKbps > 0L

    val condition = if (!countersReadable) {
        MaxCondition(
            kind = MaxConditionKind.Unsupported,
            title = stringResource(R.string.detail_network),
            detail = stringResource(R.string.detail_trafficstats_unavailable),
            technicalDetail = TRAFFIC_SOURCE
        )
    } else {
        null
    }

    MaxListScreen(
        title = stringResource(R.string.detail_network),
        onBack = { navController.popBackStack() },
        accentIcon = Icons.Rounded.NetworkCheck,
        accent = accent,
        condition = condition,
        actions = {
            MaxHelpAction(
                title = stringResource(R.string.detail_network),
                body = listOf(stringResource(R.string.detail_live_traffic), stringResource(R.string.detail_trafficstats_subtitle)).joinToString(separator = "\n\n")
            )
        },
        header = {
            NetworkHeadline(
                live = live,
                speed = formatNetSpeed(downloadKbps),
                accent = accent
            )
        }
    ) {
        item(key = "network_live") {
            // وزرّ قسم الشبكة في «معلومات الجهاز» في آخر بطاقة الحركة الحيّة — أوّل بطاقة
            // في الشاشة — كبسولة بأيقونة وكلمة لا أيقونة مجرّدة (أمر المالك).
            MaxSection(
                title = stringResource(R.string.detail_live_traffic_label),
                description = stringResource(R.string.detail_trafficstats_subtitle)
            ) {
                MaxGroup {
                    MaxMetricLine(
                        metric = MaxMetric(
                            label = stringResource(R.string.detail_download),
                            value = formatNetSpeed(downloadKbps),
                            trust = MaxDataTrust.Live,
                            source = TRAFFIC_SOURCE
                        )
                    )
                    MaxGroupDivider()
                    MaxMetricLine(
                        metric = MaxMetric(
                            label = stringResource(R.string.detail_upload),
                            value = formatNetSpeed(uploadKbps),
                            trust = MaxDataTrust.Live,
                            source = TRAFFIC_SOURCE
                        )
                    )
                    MaxGroupDivider()
                    MaxDeviceInfoShortcut(navController, MaxDestination.NetworkDetail)
                }
            }
        }

        // ── جودة الوصلة: قياس عند الطلب لا استنتاج ─────────────────────────
        item(key = "network_quality") {
            LinkQualitySection(
                report = linkReport,
                probing = probing,
                onMeasure = {
                    if (!probing) {
                        scope.launch {
                            probing = true
                            linkReport = runCatching { LinkProbe.measure(context) }.getOrNull()
                            probing = false
                        }
                    }
                }
            )
        }

        item(key = "network_timeline") {
            MaxSection(
                title = stringResource(R.string.detail_download_timeline),
                description = if (history.size >= 2) {
                    stringResource(R.string.detail_last_samples, history.size)
                } else {
                    stringResource(R.string.detail_collecting_samples)
                },
                trailing = { LivePulseDot(accent) }
            ) {
                MaxCardShell(borderColor = accent.copy(alpha = MaxAlpha.borderStrong)) {
                    if (history.size >= 2) {
                        NetworkSparkline(history = history, accent = accent, secondary = secondary)
                    } else {
                        Text(
                            text = stringResource(R.string.detail_collecting_live_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "network_totals") {
            MaxSection(
                title = stringResource(R.string.detail_traffic_totals),
                description = stringResource(R.string.detail_android_counters)
            ) {
                MaxGroup {
                    MaxMetricLine(
                        metric = MaxMetric(
                            label = stringResource(R.string.detail_total_download),
                            value = if (countersReadable) formatByteTotal(totalRx) else null,
                            trust = if (countersReadable) MaxDataTrust.Live else MaxDataTrust.Unreadable,
                            source = TRAFFIC_SOURCE
                        )
                    )
                    MaxGroupDivider()
                    MaxMetricLine(
                        metric = MaxMetric(
                            label = stringResource(R.string.detail_total_upload),
                            value = if (countersReadable) formatByteTotal(totalTx) else null,
                            trust = if (countersReadable) MaxDataTrust.Live else MaxDataTrust.Unreadable,
                            source = TRAFFIC_SOURCE
                        )
                    )
                }
            }
        }

        item(key = "network_peaks") {
            MaxSection(title = stringResource(R.string.detail_peaks)) {
                MaxGroup {
                    MaxMetricLine(
                        metric = MaxMetric(
                            label = stringResource(R.string.detail_peak_download),
                            value = formatNetSpeed(peakDownloadKbps),
                            trust = MaxDataTrust.Live,
                            source = TRAFFIC_SOURCE
                        )
                    )
                    MaxGroupDivider()
                    MaxMetricLine(
                        metric = MaxMetric(
                            label = stringResource(R.string.detail_peak_upload),
                            value = formatNetSpeed(peakUploadKbps),
                            trust = MaxDataTrust.Live,
                            source = TRAFFIC_SOURCE
                        )
                    )
                }
            }
        }
    }
}

/**
 * رأس الصفحة: الحالة بكلمة، والرقم الذي يجيب أوّلًا («كم الآن؟»).
 *
 * والكلمتان «نشطة/هادئة» لم تُحذفا في إعادة البناء: هما الجواب القابل للقراءة في ثانية، والرقم
 * وحده يطلب من المستخدم أن يقارنه بنفسه.
 */
@Composable
private fun NetworkHeadline(
    live: Boolean,
    speed: String,
    accent: Color
) {
    MaxCardShell(borderColor = accent.copy(alpha = MaxAlpha.borderStrong)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
        ) {
            Icon(
                imageVector = Icons.Rounded.NetworkCheck,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(MaxSize.iconGlyph)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.detail_live_traffic_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Text(
                    text = stringResource(
                        if (live) R.string.detail_network_active else R.string.detail_network_quiet
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            LivePulseDot(accent)
        }
        Text(
            text = stringResource(R.string.detail_trafficstats_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.detail_download_value),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = speed,
            style = MonoValueStyleSmall.copy(fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
            color = accent
        )
    }
}

/**
 * الرسم الزمني للتنزيل: تعبئة متدرّجة وخطّ بنفس سماكة الشاشة السابقة (`2.5dp`)، ونقطة عند آخر عيّنة.
 *
 * والبصمة واحدة وما تغيّر إلا السطح: كان الرسم يحمل خلفيته الخاصة (`clip` + `background`) وهو
 * **سطح ثانٍ داخل بطاقة**، والآن السطح من `MaxCardShell` وحده والرسم خطوط فقط.
 */
@Composable
private fun NetworkSparkline(history: List<Long>, accent: Color, secondary: Color) {
    val maxValue = history.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val baselineColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(SparklineHeight)
    ) {
        val width = size.width
        val height = size.height
        val baseline = height - 18.dp.toPx()
        if (history.size >= 2) {
            val step = width / (history.size - 1).toFloat()
            val fillPath = Path()
            val linePath = Path()
            history.forEachIndexed { index, value ->
                val x = index * step
                val y = baseline - (value.toFloat() / maxValue * (baseline - 16.dp.toPx()))
                if (index == 0) {
                    fillPath.moveTo(x, baseline)
                    fillPath.lineTo(x, y)
                    linePath.moveTo(x, y)
                } else {
                    fillPath.lineTo(x, y)
                    linePath.lineTo(x, y)
                }
            }
            fillPath.lineTo(width, baseline)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(listOf(accent.copy(alpha = 0.34f), Color.Transparent)),
                style = Fill
            )
            drawPath(path = linePath, color = accent, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            val lastY = baseline - (history.last().toFloat() / maxValue * (baseline - 16.dp.toPx()))
            drawCircle(secondary, radius = 4.dp.toPx(), center = Offset(width - 1.dp.toPx(), lastY))
        }
        drawLine(
            color = baselineColor,
            start = Offset(0f, baseline),
            end = Offset(width, baseline),
            strokeWidth = 1.dp.toPx()
        )
    }
}

/** إجمالي بالبايت بوحدة مقروءة — التقسيم نفسه الذي كانت تجريه الشاشة السابقة. */
private fun formatByteTotal(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> String.format(java.util.Locale.US, "%.2f GB", bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1_048_576.0)
    else -> "${bytes.coerceAtLeast(0L) / 1024L} KB"
}

/**
 * جودة الوصلة — قياس لا استنتاج.
 *
 * **ولماذا قسم مستقلّ لا صفّ داخل «الحركة الحيّة»:** حركة `TrafficStats` تُقاس كل ثانية بلا
 * إذن ولا اتصال خارجي. أمّا هذا فيفتح اتصالًا حقيقيًا بعنوان مذكور، فلابد أن يكون **بضغطة**،
 * وأن يقول قبلها إلى أين يتصل. خلطهما كان سيجعل الشاشة تتصل بالشبكة لمجرّد فتحها.
 *
 * **والفشل يُعرض فشلًا:** محاولة لم تصل تُحسب في «محاولات فاشلة» ويُسمّى نوع العطب، ولا تُحوّل
 * إلى `0 ms` — الصفر يعني «فوري» وهو أنجح نتيجة ممكنة، لا أسوأها.
 */
@Composable
private fun LinkQualitySection(
    report: LinkReport?,
    probing: Boolean,
    onMeasure: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    MaxSection(
        title = stringResource(R.string.detail_link_quality),
        description = stringResource(R.string.detail_link_quality_desc)
    ) {
        MaxGroup {
            MaxNavigationRow(
                title = stringResource(
                    if (probing) R.string.detail_probe_running else R.string.detail_probe_run
                ),
                subtitle = stringResource(
                    R.string.detail_probe_target,
                    LinkProbe.DEFAULT_HOST,
                    LinkProbe.DEFAULT_PORT
                ),
                enabled = !probing,
                onClick = onMeasure
            )
        }

        if (report == null) {
            Text(
                text = stringResource(R.string.detail_probe_never),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaxSpace.groupPadding, vertical = MaxSpace.sm)
            )
            return@MaxSection
        }

        MaxGroup {
            MaxMetricLine(
                metric = MaxMetric(
                    label = stringResource(R.string.detail_probe_dns),
                    value = report.dnsMs?.let { stringResource(R.string.detail_probe_ms, it) },
                    trust = MaxDataTrust.Snapshot,
                    source = PROBE_SOURCE
                )
            )
            MaxGroupDivider()
            MaxMetricLine(
                metric = MaxMetric(
                    label = stringResource(R.string.detail_probe_best),
                    value = report.bestMs?.let { stringResource(R.string.detail_probe_ms, it) },
                    trust = MaxDataTrust.Snapshot,
                    source = PROBE_SOURCE
                )
            )
            MaxGroupDivider()
            MaxMetricLine(
                metric = MaxMetric(
                    label = stringResource(R.string.detail_probe_average),
                    value = report.averageMs?.let { stringResource(R.string.detail_probe_ms, it) },
                    trust = MaxDataTrust.Snapshot,
                    source = PROBE_SOURCE
                )
            )
            MaxGroupDivider()
            MaxMetricLine(
                metric = MaxMetric(
                    label = stringResource(R.string.detail_probe_jitter),
                    value = report.jitterMs?.let { stringResource(R.string.detail_probe_ms, it) },
                    trust = MaxDataTrust.Snapshot,
                    source = PROBE_SOURCE
                )
            )
            if (report.failures > 0) {
                MaxGroupDivider()
                MaxMetricLine(
                    metric = MaxMetric(
                        label = stringResource(R.string.detail_probe_failures),
                        value = "${report.failures} / ${report.attempts}",
                        trust = MaxDataTrust.Snapshot,
                        source = report.lastFailure ?: PROBE_SOURCE
                    )
                )
            }
        }

        MaxGroup {
            MaxRow(
                title = stringResource(R.string.detail_probe_validated),
                trailing = {
                    Text(
                        text = when (report.validated) {
                            true -> stringResource(R.string.yes)
                            false -> stringResource(R.string.no)
                            null -> stringResource(R.string.detail_probe_unknown)
                        },
                        style = MonoValueStyleSmall,
                        color = scheme.onSurface
                    )
                }
            )
            if (report.captivePortal == true) {
                MaxGroupDivider()
                MaxRow(
                    title = stringResource(R.string.detail_probe_captive),
                    trailing = {
                        Text(
                            text = stringResource(R.string.yes),
                            style = MonoValueStyleSmall,
                            color = scheme.onSurface
                        )
                    }
                )
            }
            MaxGroupDivider()
            MaxRow(
                title = stringResource(R.string.detail_probe_transport),
                trailing = {
                    Text(
                        text = report.transports.takeIf { it.isNotEmpty() }?.joinToString(", ")
                            ?: stringResource(R.string.detail_probe_unknown),
                        style = MonoValueStyleSmall,
                        color = scheme.onSurface
                    )
                }
            )
            MaxGroupDivider()
            MaxRow(
                title = stringResource(R.string.detail_probe_metered),
                trailing = {
                    Text(
                        text = when (report.metered) {
                            true -> stringResource(R.string.yes)
                            false -> stringResource(R.string.no)
                            null -> stringResource(R.string.detail_probe_unknown)
                        },
                        style = MonoValueStyleSmall,
                        color = scheme.onSurface
                    )
                }
            )
            MaxGroupDivider()
            MaxRow(
                title = stringResource(R.string.detail_probe_link_down),
                trailing = {
                    Text(
                        text = report.downstreamKbps?.let { formatNetSpeed(it.toLong()) }
                            ?: stringResource(R.string.detail_probe_unknown),
                        style = MonoValueStyleSmall,
                        color = scheme.onSurface
                    )
                }
            )
            MaxGroupDivider()
            MaxRow(
                title = stringResource(R.string.detail_probe_link_up),
                trailing = {
                    Text(
                        text = report.upstreamKbps?.let { formatNetSpeed(it.toLong()) }
                            ?: stringResource(R.string.detail_probe_unknown),
                        style = MonoValueStyleSmall,
                        color = scheme.onSurface
                    )
                }
            )
        }

        Text(
            text = stringResource(R.string.detail_probe_not_icmp),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MaxSpace.groupPadding, vertical = MaxSpace.sm)
        )
        Text(
            text = stringResource(R.string.detail_probe_cached_note),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
        )
    }
}
