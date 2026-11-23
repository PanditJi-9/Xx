/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.mainscreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.text.format.DateUtils
import kotlinx.coroutines.launch
import nd.max.R
import nd.max.core.hardware.PlatformDefaultsReset
import nd.max.core.persist.PersistRestorer
import nd.max.core.persist.PersistStore
import nd.max.core.policy.DangerConsent
import nd.max.ui.component.ExpressiveList
import nd.max.ui.component.ExpressiveListItem
import nd.max.ui.component.ExpressiveSwitchItem
import nd.max.ui.component.LeadingIcon
import nd.max.ui.design.MaxConfirmDialog
import nd.max.ui.design.MaxSpace

/**
 * قسم التطبيق التلقائي عند الإقلاع في Settings.
 */
@Composable
fun PersistRestoreItem() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(PersistStore.enabled(context)) }
    var busy by remember { mutableStateOf(false) }
    var dangerousAuto by remember { mutableStateOf(PersistStore.dangerousAutoEnabled(context)) }
    var dangerousReason by remember { mutableStateOf(PersistStore.dangerousAutoOffReason(context)) }
    var lastRunAtMs by remember { mutableStateOf(PersistStore.lastRunAtMs(context)) }
    var pendingDangerConfirm by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf(false) }
    var pendingResetConfirm by remember { mutableStateOf(false) }
    var resetFailure by remember { mutableStateOf<String?>(null) }
    var resetDone by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
        ExpressiveList(
            content = listOf(
                {
                    ExpressiveSwitchItem(
                        icon = Icons.Rounded.PowerSettingsNew,
                        title = stringResource(R.string.persist_enable_title),
                        summary = stringResource(R.string.persist_enable_desc),
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            PersistStore.setEnabled(context, it)
                        }
                    )
                },
                {
                    ExpressiveSwitchItem(
                        icon = Icons.Rounded.WarningAmber,
                        title = stringResource(R.string.persist_dangerous_title),
                        summary = stringResource(R.string.persist_dangerous_desc),
                        checked = dangerousAuto,
                        enabled = enabled,
                        onCheckedChange = { wanted ->
                            // الفتح فقط يحتاج إقرارًا: الإطفاء تراجعٌ لا مخاطرة.
                            if (wanted && !DangerConsent.isAcknowledged(context, DangerConsent.RESTORE_DANGEROUS)) {
                                pendingDangerConfirm = true
                            } else {
                                dangerousAuto = wanted
                                PersistStore.setDangerousAutoEnabled(context, wanted)
                                dangerousReason = PersistStore.dangerousAutoOffReason(context)
                            }
                        }
                    )
                },
                {
                    ExpressiveListItem(
                        onClick = if (enabled && !busy) {
                            {
                                busy = true
                                scope.launch {
                                    PersistRestorer.restoreNow(context)
                                    lastRunAtMs = PersistStore.lastRunAtMs(context)
                                    busy = false
                                }
                            }
                        } else null,
                        headlineContent = { Text(stringResource(R.string.persist_apply_now)) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    if (busy) R.string.persist_apply_running else R.string.persist_apply_now_desc
                                )
                            )
                        },
                        leadingContent = { LeadingIcon(icon = Icons.Rounded.Refresh, contentDescription = null) },
                        trailingContent = {
                            if (busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                            }
                        }
                    )
                }
            )
        )

        // آخر جولة: قراءة فقط، فلا زرّ يصفّر سجلًّا وقع.
        ExpressiveList(
            content = listOf(
                {
                    ExpressiveListItem(
                        headlineContent = { Text(stringResource(R.string.persist_last_run_title)) },
                        supportingContent = {
                            Text(
                                if (lastRunAtMs == 0L) {
                                    stringResource(R.string.persist_last_run_never)
                                } else {
                                    DateUtils.getRelativeTimeSpanString(
                                        lastRunAtMs,
                                        System.currentTimeMillis(),
                                        DateUtils.MINUTE_IN_MILLIS
                                    ).toString()
                                }
                            )
                        },
                        leadingContent = {
                            LeadingIcon(icon = Icons.Rounded.History, contentDescription = null)
                        }
                    )
                },
            )
        )

        // **استعادة افتراضي المنصّة** (بلاغ SHARK): إزالة الوحدة وحدها قد تُبقي مفاتيح
        // `persist.sys.maxmanager*` و`API/current_profile`، فيعيد الخادم الوضع القديم عند الإقلاع.
        // وهذا الفعل يفكّ قيود المصنّع والملفات المحفوظة ويعيد التخطيط المتوازن — لا يمسّ
        // اختيارات التطبيق ولا ملفاتك، ولا يُعيد ضبط أندرويد نفسه.
        ExpressiveList(
            content = listOf(
                {
                    ExpressiveListItem(
                        onClick = if (!resetting) {
                            { pendingResetConfirm = true }
                        } else null,
                        headlineContent = { Text(stringResource(R.string.persist_reset_platform_title)) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    if (resetting) R.string.persist_reset_platform_running
                                    else R.string.persist_reset_platform_desc
                                )
                            )
                        },
                        leadingContent = {
                            LeadingIcon(
                                icon = Icons.Rounded.SettingsBackupRestore,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            if (resetting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                            }
                        }
                    )
                },
            )
        )

        // النتيجة تُعرض كما قِيست: نجاح كامل، أو أسماء الخطوات التي لم تُثبت.
        resetFailure?.let { failed ->
            Text(
                text = stringResource(R.string.persist_reset_platform_partial, failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
            )
        }
        if (resetDone) {
            Text(
                text = stringResource(R.string.persist_reset_platform_done),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
            )
        }

        // إقرار المخاطر مرة واحدة لكل ميزة: يُعرض مرة، ويُسجَّل بزمنه، ولا يُلغى تلقائيًّا.
        // والرسالة تقول المسؤولية صريحةً — لا «هل أنت متأكد؟» التي لا تقول شيئًا.
        MaxConfirmDialog(
            visible = pendingDangerConfirm,
            title = stringResource(R.string.persist_danger_confirm_title),
            message = stringResource(R.string.persist_danger_confirm_body),
            confirmLabel = stringResource(R.string.persist_danger_confirm_action),
            onConfirm = {
                DangerConsent.acknowledge(context, DangerConsent.RESTORE_DANGEROUS)
                dangerousAuto = true
                PersistStore.setDangerousAutoEnabled(context, true)
                dangerousReason = PersistStore.dangerousAutoOffReason(context)
                pendingDangerConfirm = false
            },
            onDismiss = { pendingDangerConfirm = false },
            icon = Icons.Rounded.Warning,
            destructive = true
        )

        MaxConfirmDialog(
            visible = pendingResetConfirm,
            title = stringResource(R.string.persist_reset_platform_confirm_title),
            message = stringResource(R.string.persist_reset_platform_confirm_body),
            confirmLabel = stringResource(R.string.persist_reset_platform_confirm_action),
            onConfirm = {
                pendingResetConfirm = false
                resetting = true
                resetDone = false
                resetFailure = null
                scope.launch {
                    val outcome = PlatformDefaultsReset.reset()
                    // الخطوات تُسمّى بلغة الواجهة لا بمفاتيحها البرمجية (`cpu-ranges`)،
                    // و`0` يعني خطوة بلا اسم مترجم فيُعرض مفتاحها كما هو بدل أن يختفي.
                    resetFailure = outcome.failed
                        .joinToString(", ") { step ->
                            val label = stepLabel(step.name)
                            if (label == 0) step.name else context.getString(label)
                        }
                        .takeIf { it.isNotEmpty() }
                    resetDone = outcome.allOk
                    resetting = false
                }
            },
            onDismiss = { pendingResetConfirm = false },
            icon = Icons.Rounded.SettingsBackupRestore,
            destructive = true
        )

        if (!dangerousAuto && dangerousReason == PersistStore.REASON_BOOTLOOP) {
            Text(
                text = stringResource(R.string.persist_dangerous_bootloop),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
            )
        }
    }
}

/**
 * اسم مقروء لخطوة في [PlatformDefaultsReset]: مفتاح الخطوة برمجيّ بالإنجليزية
 * (`cpu-ranges`)، وعرضه خامًا داخل جملة عربية يجعل الرسالة تبدو خطأً لا تفسيرًا.
 * و`0` تعني «لا ترجمة معلومة» فيعرض المنادي المفتاح نفسه بدل أن يبتلع الخطوة.
 */
private fun stepLabel(name: String): Int = when (name) {
    "locks" -> R.string.persist_reset_step_locks
    "cpu-ranges" -> R.string.persist_reset_step_cpu_ranges
    "manual-session" -> R.string.persist_reset_step_manual_session
    "saved-overrides" -> R.string.persist_reset_step_saved_overrides
    "gpu" -> R.string.persist_reset_step_gpu
    "gpu-saved" -> R.string.persist_reset_step_gpu_saved
    "profile" -> R.string.persist_reset_step_profile
    else -> 0
}
