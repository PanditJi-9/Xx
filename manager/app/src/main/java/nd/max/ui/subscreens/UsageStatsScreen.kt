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
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nd.max.R
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
import nd.max.ui.theme.MonoValueStyleSmall
import java.time.LocalDate
import java.time.ZoneId

/**
 * App usage — foreground time as Android itself recorded it.
 *
 * This is the second surface taken from the reference teardown's "device facts"
 * family: Diagnostics explains what the hardware exposes, Device Lab proves that
 * it works, and this screen answers "what has actually been used?". The source
 * is `UsageStatsManager`, not an inference from processes or network counters.
 *
 * Licensing: the reference app's usage screen was read for behaviour only. This
 * file is written against the Android SDK; no reference layout, string or code
 * is reused.
 */

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

@Composable
fun UsageStatsScreen(navController: NavHostController) {
    val context = LocalContext.current
    var period by remember { mutableStateOf(UsagePeriod.TODAY) }
    var permissionGranted by remember { mutableStateOf(hasUsageAccess(context)) }
    var reload by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var snapshot by remember { mutableStateOf<UsageSnapshot?>(null) }
    var failed by remember { mutableStateOf(false) }

    // Returning from the system's usage-access switch must not require a manual
    // refresh. The resume hook re-checks the permission and re-queries only when
    // the screen is actually visible again.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionGranted = hasUsageAccess(context)
        reload++
    }

    LaunchedEffect(period, reload, permissionGranted) {
        if (!permissionGranted) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        failed = false
        readUsage(context, period)
            .onSuccess { snapshot = it }
            .onFailure { failed = true }
        loading = false
    }

    val data = snapshot
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
        failed -> MaxCondition(
            kind = MaxConditionKind.Error,
            title = stringResource(R.string.usage_stats_error_title),
            detail = stringResource(R.string.usage_stats_error_detail)
        )
        loading && data == null -> MaxCondition(
            kind = MaxConditionKind.Loading,
            title = stringResource(R.string.usage_stats_title),
            detail = stringResource(R.string.usage_stats_loading)
        )
        data != null && data.apps.isEmpty() -> MaxCondition(
            kind = MaxConditionKind.Empty,
            title = stringResource(R.string.usage_stats_empty_title),
            detail = stringResource(R.string.usage_stats_empty_detail)
        )
        else -> null
    }

    MaxListScreen(
        title = stringResource(R.string.usage_stats_title),
        onBack = { navController.popBackStack() },
        subtitle = stringResource(R.string.usage_stats_subtitle),
        accentIcon = Icons.Rounded.DataUsage,
        condition = condition,
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
            MaxSection(title = stringResource(R.string.usage_stats_period)) {
                MaxSegmented(
                    options = UsagePeriod.values().map { stringResource(it.labelRes) },
                    selectedIndex = period.ordinal,
                    onSelect = { period = UsagePeriod.values()[it] }
                )
            }
            if (data != null) {
                MaxSection(title = stringResource(R.string.usage_stats_summary)) {
                    MaxGroup {
                        MaxRow(
                            title = stringResource(R.string.usage_stats_total),
                            subtitle = stringResource(R.string.usage_stats_total_detail),
                            icon = Icons.Rounded.Timer,
                            trailing = {
                                Text(
                                    text = formatDuration(data.totalMs),
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
                                    text = data.appCount.toString(),
                                    style = MonoValueStyleSmall
                                )
                            }
                        )
                    }
                }
                MaxSection(
                    title = stringResource(R.string.usage_stats_top_apps),
                    description = stringResource(R.string.usage_stats_top_apps_detail)
                ) { }
            }
        }
    ) {
        val apps = data?.apps.orEmpty()
        items(items = apps, key = { it.packageName }) { app ->
            UsageAppRow(app)
            if (app != apps.last()) MaxGroupDivider()
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

private fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private suspend fun readUsage(
    context: Context,
    period: UsagePeriod
): Result<UsageSnapshot> = withContext(Dispatchers.IO) {
    runCatching {
        val zone = ZoneId.systemDefault()
        val end = System.currentTimeMillis()
        val start = LocalDate.now(zone)
            .minusDays((period.days - 1).toLong())
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

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
