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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.automirrored.rounded.Undo
import kotlinx.coroutines.launch
import nd.max.R
import nd.max.core.persist.PersistRestorer
import nd.max.core.persist.PersistSnapshot
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
    var fastBoots by remember { mutableStateOf(PersistStore.fastBootStreak(context)) }
    var pendingDangerConfirm by remember { mutableStateOf(false) }
    var snapshotCount by remember { mutableStateOf(PersistSnapshot.load(context)?.values?.size ?: 0) }

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
                        icon = Icons.Rounded.Shield,
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
                                    fastBoots = PersistStore.fastBootStreak(context)
                                    snapshotCount = PersistSnapshot.load(context)?.values?.size ?: 0
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
                        leadingContent = { LeadingIcon(icon = Icons.Filled.Refresh, contentDescription = null) },
                        trailingContent = {
                            if (busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                            }
                        }
                    )
                }
            )
        )

        // وقائع الحارس: آخر جولة، وكم إقلاعًا سريعًا رآه. قراءة فقط — لا زرّ يصفّر
        // عدّادًا، لأن الصفر الكاذب يُخفي إقلاعًا وقع.
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
                {
                    ExpressiveListItem(
                        headlineContent = { Text(stringResource(R.string.persist_fast_boots_title)) },
                        supportingContent = {
                            Text(stringResource(R.string.persist_fast_boots_desc, fastBoots))
                        },
                        leadingContent = {
                            LeadingIcon(icon = Icons.Rounded.RestartAlt, contentDescription = null)
                        }
                    )
                }
            )
        )

        // تراجع عن آخر قيم كُتبت: فعل لا سجلّ. يظهر فقط إن كانت هناك لقطة،
        // لأن زرًّا لا يفعل شيئًا أسوأ من غيابه.
        if (snapshotCount > 0) {
            ExpressiveList(
                content = listOf(
                    {
                        ExpressiveListItem(
                            onClick = {
                                if (!busy) {
                                    busy = true
                                    scope.launch {
                                        PersistRestorer.restoreSnapshot(context)
                                        snapshotCount = PersistSnapshot.load(context)?.values?.size ?: 0
                                        lastRunAtMs = PersistStore.lastRunAtMs(context)
                                        busy = false
                                    }
                                }
                            },
                            headlineContent = { Text(stringResource(R.string.persist_snapshot_title)) },
                            supportingContent = {
                                Text(stringResource(R.string.persist_snapshot_desc, snapshotCount))
                            },
                            leadingContent = {
                                LeadingIcon(icon = Icons.AutoMirrored.Rounded.Undo, contentDescription = null)
                            }
                        )
                    }
                )
            )
        }

        if (fastBoots >= 2) {
            Text(
                text = stringResource(R.string.persist_fast_boots_tripped),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
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
