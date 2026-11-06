/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Process
import android.provider.Settings
import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.net.NetworkUsageReader
import nd.max.core.net.NetworkUsageSnapshot
import nd.max.core.stats.DailyUsage
import nd.max.ui.design.MaxCondition
import nd.max.ui.design.MaxConditionKind
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxListScreen
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSegmented
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.util.DailyUsageReader
import nd.max.ui.theme.MonoValueStyleSmall
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * App usage — two views of the same question, both read from Android's own
 * databases.
 *
 * **Screen time** is `UsageStatsManager`: foreground milliseconds per app.
 * **Data usage** is `NetworkStatsManager`: bytes per uid, split by transport.
 * Neither number is inferred from the other; a device that reports one and not
 * the other shows exactly that.
 *
 * The two share one permission (Usage access), one period selector and one
 * list, so the screen stays a single destination instead of two lookalikes.
 *
 * Licensing: the reference app's usage screens were read for behaviour only.
 * This file is written against the Android SDK; no reference layout, string or
 * code is reused.
 */

/** The two measurable views. Both answer the same period selector. */
private enum class UsageMode(@StringRes val labelRes: Int) {
    TIME(R.string.usage_stats_mode_time),
    NETWORK(R.string.usage_stats_mode_network)
}

/** The three windows a usage query can answer without guessing. */
private enum class UsagePeriod(val days: Int, @StringRes val labelRes: Int) {
    TODAY(1, R.string.usage_stats_period_today),
    WEEK(7, R.string.usage_stats_period_week),
    MONTH(30, R.string.usage_stats_period_month)
}

private data class UsageApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val foregroundMs: Long
)

private data class UsageSnapshot(
    val totalMs: Long,
    val appCount: Int,
    val apps: List<UsageApp>
)

private data class NetworkAppRow(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val wifiBytes: Long,
    val mobileBytes: Long
) {
    val totalBytes: Long get() = wifiBytes + mobileBytes
}

@Composable
fun UsageStatsScreen(navController: NavHostController) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(UsageMode.TIME) }
    var period by remember { mutableStateOf(UsagePeriod.TODAY) }
    var permissionGranted by remember { mutableStateOf(hasUsageAccess(context)) }
    var reload by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var snapshot by remember { mutableStateOf<UsageSnapshot?>(null) }
    var timeFailed by remember { mutableStateOf(false) }
    var daily by remember { mutableStateOf<List<DailyUsage.Day>>(emptyList()) }
    var dailyFailed by remember { mutableStateOf(false) }
    var network by remember { mutableStateOf<NetworkUsageSnapshot?>(null) }
    var networkApps by remember { mutableStateOf<List<NetworkAppRow>>(emptyList()) }
    var networkFailed by remember { mutableStateOf(false) }

    // Returning from the system's usage-access switch must not require a manual
    // refresh. The resume hook re-checks the permission and re-queries only when
    // the screen is actually visible again.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionGranted = hasUsageAccess(context)
        reload++
    }

    LaunchedEffect(mode, period, reload, permissionGranted) {
        if (!permissionGranted) {
            daily = emptyList()
            snapshot = null
            loading = false
            return@LaunchedEffect
        }
        loading = true
        when (mode) {
            UsageMode.TIME -> {
                timeFailed = false
                daily = emptyList()
                dailyFailed = false
                readUsage(context, period)
                    .onSuccess { snapshot = it }
                    .onFailure { timeFailed = true }
                // الرسم أسبوعي دائمًا: الفترة المختارة تخصّ القائمة، لا المحور الزمني.
                withContext(Dispatchers.IO) { DailyUsageReader.read(context, 14) }
                    .onSuccess { daily = it }
                    .onFailure { dailyFailed = true }
            }
            UsageMode.NETWORK -> {
                networkFailed = false
                readNetwork(context, period)
                    .onSuccess {
                        network = it.first
                        networkApps = it.second
                    }
                    .onFailure { networkFailed = true }
            }
        }
        loading = false
    }

    val timeData = snapshot
    val networkData = network
    val failed = if (mode == UsageMode.TIME) timeFailed else networkFailed
    val loaded = if (mode == UsageMode.TIME) timeData != null else networkData != null

    val condition = when {
        !permissionGranted -> MaxCondition(
            kind = MaxConditionKind.PermissionRequired,
            title = stringResource(R.string.usage_stats_permission_title),
            detail = stringResource(R.string.usage_stats_permission_detail),
            primaryActionLabel = stringResource(R.string.usage_stats_permission_action),
            onPrimaryAction = {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            }
        )
        failed -> if (mode == UsageMode.TIME) {
            MaxCondition(
                kind = MaxConditionKind.Error,
                title = stringResource(R.string.usage_stats_error_title),
                detail = stringResource(R.string.usage_stats_error_detail)
            )
        } else {
            MaxCondition(
                kind = MaxConditionKind.Error,
                title = stringResource(R.string.usage_network_error_title),
                detail = stringResource(R.string.usage_network_error_detail)
            )
        }
        loading && !loaded -> MaxCondition(
            kind = MaxConditionKind.Loading,
            title = stringResource(R.string.usage_stats_title),
            detail = stringResource(R.string.usage_stats_loading)
        )
        mode == UsageMode.TIME && timeData != null && timeData.apps.isEmpty() &&
            daily.none { it.millis > 0L } && !dailyFailed -> MaxCondition(
            kind = MaxConditionKind.Empty,
            title = stringResource(R.string.usage_stats_empty_title),
            detail = stringResource(R.string.usage_stats_empty_detail)
        )
        mode == UsageMode.NETWORK && networkData != null &&
            networkData.perAppAvailable && networkData.totalBytes == 0L -> MaxCondition(
            kind = MaxConditionKind.Empty,
            title = stringResource(R.string.usage_network_empty_title),
            detail = stringResource(R.string.usage_network_empty_detail)
        )
        else -> null
    }

    // A single combined counter is a real answer, not a failure: the totals are
    // shown and the missing breakdown is named in the banner instead of being
    // rendered as an empty list.
    val banner = if (
        mode == UsageMode.NETWORK && networkData != null && !networkData.perAppAvailable
    ) {
        MaxCondition(
            kind = MaxConditionKind.Unsupported,
            title = stringResource(R.string.usage_network_totals_only_title),
            detail = stringResource(R.string.usage_network_totals_only_detail)
        )
    } else {
        null
    }

    MaxListScreen(
        title = stringResource(R.string.usage_stats_title),
        onBack = { navController.popBackStack() },
        subtitle = stringResource(R.string.usage_stats_subtitle),
        accentIcon = Icons.Rounded.DataUsage,
        condition = condition,
        banner = banner,
        actions = {
            IconButton(
                onClick = { reload++ },
                enabled = permissionGranted && !loading
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.usage_stats_refresh)
                )
            }
        },
        header = {
            MaxSection(title = stringResource(R.string.usage_stats_mode)) {
                MaxSegmented(
                    options = UsageMode.entries.map { stringResource(it.labelRes) },
                    selectedIndex = mode.ordinal,
                    onSelect = { mode = UsageMode.entries[it] }
                )
            }
            MaxSection(title = stringResource(R.string.usage_stats_period)) {
                MaxSegmented(
                    options = UsagePeriod.entries.map { stringResource(it.labelRes) },
                    selectedIndex = period.ordinal,
                    onSelect = { period = UsagePeriod.entries[it] }
                )
            }
            if (mode == UsageMode.TIME && timeData != null) {
                MaxSection(title = stringResource(R.string.usage_stats_summary)) {
                    MaxGroup {
                        MaxRow(
                            title = stringResource(R.string.usage_stats_total),
                            subtitle = stringResource(R.string.usage_stats_total_detail),
                            icon = Icons.Rounded.Timer,
                            trailing = {
                                Text(
                                    text = formatDuration(timeData.totalMs),
                                    style = MonoValueStyleSmall
                                )
                            }
                        )
                        MaxGroupDivider()
                        MaxRow(
                            title = stringResource(R.string.usage_stats_app_count),
                            subtitle = stringResource(period.labelRes),
                            icon = Icons.Rounded.Apps,
                            trailing = {
                                Text(
                                    text = timeData.appCount.toString(),
                                    style = MonoValueStyleSmall
                                )
                            }
                        )
                    }
                }
                if (permissionGranted && !loading && !timeFailed) {
                    MaxSection(
                        title = stringResource(R.string.usage_stats_daily_title),
                        description = stringResource(R.string.usage_stats_daily_detail)
                    ) {
                        if (dailyFailed) {
                            Text(
                                text = stringResource(R.string.usage_stats_daily_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // الرسم سبعةٌ دائمًا، والمقارنة تحتاج سبعةً ثانية خلفها.
                            DailyUsageBars(days = daily.takeLast(7))
                            DailyUsageComparison(
                                compare = DailyUsage.weekOverWeek(daily),
                                modifier = Modifier.padding(top = MaxSpace.sm)
                            )
                        }
                    }
                }
                MaxSection(
                    title = stringResource(R.string.usage_stats_top_apps),
                    description = stringResource(R.string.usage_stats_top_apps_detail)
                ) { }
            }
            if (mode == UsageMode.NETWORK && networkData != null) {
                MaxSection(
                    title = stringResource(R.string.usage_network_summary),
                    description = stringResource(R.string.usage_network_summary_detail)
                ) {
                    MaxGroup {
                        MaxRow(
                            title = stringResource(R.string.usage_network_wifi),
                            subtitle = if (networkData.wifiAvailable) {
                                stringResource(period.labelRes)
                            } else {
                                stringResource(R.string.usage_network_transport_unavailable)
                            },
                            trailing = {
                                Text(
                                    text = if (networkData.wifiAvailable) {
                                        formatBytes(context, networkData.wifiBytes)
                                    } else {
                                        stringResource(R.string.device_lab_value_unavailable)
                                    },
                                    style = MonoValueStyleSmall
                                )
                            }
                        )
                        MaxGroupDivider()
                        MaxRow(
                            title = stringResource(R.string.usage_network_mobile),
                            subtitle = if (networkData.mobileAvailable) {
                                stringResource(period.labelRes)
                            } else {
                                stringResource(R.string.usage_network_transport_unavailable)
                            },
                            trailing = {
                                Text(
                                    text = if (networkData.mobileAvailable) {
                                        formatBytes(context, networkData.mobileBytes)
                                    } else {
                                        stringResource(R.string.device_lab_value_unavailable)
                                    },
                                    style = MonoValueStyleSmall
                                )
                            }
                        )
                        MaxGroupDivider()
                        MaxRow(
                            title = stringResource(R.string.usage_network_total),
                            trailing = {
                                Text(
                                    text = formatBytes(context, networkData.totalBytes),
                                    style = MonoValueStyleSmall
                                )
                            }
                        )
                    }
                }
                if (networkData.perAppAvailable) {
                    MaxSection(
                        title = stringResource(R.string.usage_network_apps_title),
                        description = stringResource(R.string.usage_network_apps_detail)
                    ) { }
                }
            }
        }
    ) {
        when (mode) {
            UsageMode.TIME -> {
                val apps = timeData?.apps.orEmpty()
                items(items = apps, key = { it.packageName }) { app ->
                    UsageAppRow(app)
                    if (app != apps.last()) MaxGroupDivider()
                }
            }
            UsageMode.NETWORK -> {
                items(items = networkApps, key = { it.packageName }) { app ->
                    NetworkAppRowItem(app)
                    if (app != networkApps.last()) MaxGroupDivider()
                }
            }
        }
    }
}

@Composable
private fun UsageAppRow(app: UsageApp) {
    val iconBitmap = remember(app.packageName) { app.icon?.toBitmap()?.asImageBitmap() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaxSize.minTouchTarget)
            .padding(
                horizontal = MaxSpace.rowPaddingHorizontal,
                vertical = MaxSpace.rowPaddingVertical
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier.size(MaxSize.rowIconContainer)
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Android,
                contentDescription = null,
                modifier = Modifier.size(MaxSize.rowIconContainer),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)
        ) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatDuration(app.foregroundMs),
            style = MonoValueStyleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NetworkAppRowItem(app: NetworkAppRow) {
    val context = LocalContext.current
    val iconBitmap = remember(app.packageName) { app.icon?.toBitmap()?.asImageBitmap() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaxSize.minTouchTarget)
            .padding(
                horizontal = MaxSpace.rowPaddingHorizontal,
                vertical = MaxSpace.rowPaddingVertical
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier.size(MaxSize.rowIconContainer)
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Android,
                contentDescription = null,
                modifier = Modifier.size(MaxSize.rowIconContainer),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)
        ) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = networkSplit(context, app),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatBytes(context, app.totalBytes),
            style = MonoValueStyleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun networkSplit(context: Context, app: NetworkAppRow): String {
    val parts = buildList {
        if (app.wifiBytes > 0L) {
            add(
                stringResource(
                    R.string.usage_network_split_wifi,
                    formatBytes(context, app.wifiBytes)
                )
            )
        }
        if (app.mobileBytes > 0L) {
            add(
                stringResource(
                    R.string.usage_network_split_mobile,
                    formatBytes(context, app.mobileBytes)
                )
            )
        }
    }
    return parts.ifEmpty { listOf(app.packageName) }.joinToString(" · ")
}

@Composable
private fun formatDuration(ms: Long): String {
    val totalMinutes = (ms / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L -> stringResource(R.string.usage_stats_duration_hm, hours, minutes)
        totalMinutes > 0L -> stringResource(R.string.usage_stats_duration_m, totalMinutes)
        else -> stringResource(
            R.string.usage_stats_duration_s,
            (ms / 1_000L).coerceAtLeast(0L)
        )
    }
}

/** Platform-localised, so `١٢٫٣ ميغابايت` reads right without a second table. */
private fun formatBytes(context: Context, bytes: Long): String =
    Formatter.formatFileSize(context, bytes)

private fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun periodStart(period: UsagePeriod): Long {
    val zone = ZoneId.systemDefault()
    return LocalDate.now(zone)
        .minusDays((period.days - 1).toLong())
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
}

private suspend fun readUsage(
    context: Context,
    period: UsagePeriod
): Result<UsageSnapshot> = withContext(Dispatchers.IO) {
    runCatching {
        val end = System.currentTimeMillis()
        val start = periodStart(period)

        val manager = context.getSystemService(UsageStatsManager::class.java)
            ?: error("UsageStatsManager unavailable")
        val stats = manager.queryAndAggregateUsageStats(start, end)
        val packageManager = context.packageManager

        val totalMs = stats.values.sumOf { it.totalTimeInForeground }
        val apps = stats.values.asSequence()
            .filter { it.totalTimeInForeground > 0L }
            .sortedByDescending { it.totalTimeInForeground }
            .mapNotNull { stat ->
                val packageName = stat.packageName ?: return@mapNotNull null
                val info = runCatching {
                    packageManager.getApplicationInfo(packageName, 0)
                }.getOrNull() ?: return@mapNotNull null
                val label = runCatching {
                    packageManager.getApplicationLabel(info).toString()
                }.getOrDefault(packageName)
                val icon = runCatching {
                    packageManager.getApplicationIcon(info)
                }.getOrNull()
                UsageApp(
                    packageName = packageName,
                    label = label,
                    icon = icon,
                    foregroundMs = stat.totalTimeInForeground
                )
            }
            .toList()

        UsageSnapshot(
            totalMs = totalMs,
            appCount = stats.values.count { it.totalTimeInForeground > 0L },
            apps = apps.take(40)
        )
    }
}

/**
 * Network counters in the same window.
 *
 * The reader returns platform totals; this function only attaches the icon and
 * label the row needs. An app whose package disappeared still contributes to
 * the platform total but has no row, which is stated in the section
 * description instead of being silently dropped.
 */
private suspend fun readNetwork(
    context: Context,
    period: UsagePeriod
): Result<Pair<NetworkUsageSnapshot, List<NetworkAppRow>>> = withContext(Dispatchers.IO) {
    runCatching {
        val end = System.currentTimeMillis()
        val snapshot = NetworkUsageReader.read(context, periodStart(period), end).getOrThrow()
        val packageManager = context.packageManager
        val rows = snapshot.apps.map { app ->
            val icon = runCatching {
                packageManager.getApplicationIcon(app.packageName)
            }.getOrNull()
            NetworkAppRow(
                packageName = app.packageName,
                label = app.label,
                icon = icon,
                wifiBytes = app.wifiBytes,
                mobileBytes = app.mobileBytes
            )
        }
        snapshot to rows
    }
}

/**
 * أعمدة الأيام السبعة — الرسم نفسه لا يخترع أرقامًا: أعلى عمود هو أكثر يوم استعمالًا
 * في هذا الأسبوع، وليس هدفًا ثابتًا. الصفر يعني أن الأحداث المتاحة لم تعطِ زمنًا،
 * لا دليلًا أن الجهاز لم يُستعمل. وصف الوصول يضمّ التاريخ الكامل والمدة.
 */
@Composable
private fun DailyUsageBars(days: List<DailyUsage.Day>) {
    val peak = days.maxOfOrNull { it.millis } ?: 0L
    if (peak <= 0L) {
        Text(
            text = stringResource(R.string.usage_stats_daily_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val fraction = (day.millis.toFloat() / peak).coerceIn(0f, 1f)
            val description = stringResource(
                R.string.usage_stats_daily_value,
                LocalDate.ofEpochDay(day.epochDay).format(dateFormat),
                formatDuration(day.millis)
            )
            Column(
                modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {
                    contentDescription = description
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MaxSize.iconContainer * 2),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Text(
                    text = LocalDate.ofEpochDay(day.epochDay).dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * سطر المقارنة: يقول الفرق بلغته، ويمتنع حين لا أساس له.
 *
 * **ولماذا لا يعرض سهمًا ملوّنًا وحده:** اللون وحده لا يُقرأ بقارئ الشاشة ولا يُفهم بلا نظر؛
 * والنصّ يقول «أعلى/أقلّ» صريحًا ثم يضع المدة والنسبة. وحين يكون الأسبوع السابق صفرًا لا تُخترع
 * نسبة، بل يُقال إنه لا أساس للمقارنة بعد.
 */
@Composable
private fun DailyUsageComparison(compare: DailyUsage.WeekOverWeek?, modifier: Modifier = Modifier) {
    if (compare == null) return
    val percent = compare.percentChange
    val text = when {
        percent == null -> stringResource(R.string.usage_stats_daily_compare_none)
        percent == 0 -> stringResource(R.string.usage_stats_daily_compare_same)
        percent > 0 -> stringResource(
            R.string.usage_stats_daily_compare_up,
            formatDuration(compare.deltaMillis),
            percent
        )
        else -> stringResource(
            R.string.usage_stats_daily_compare_down,
            formatDuration(-compare.deltaMillis),
            -percent
        )
    }
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
