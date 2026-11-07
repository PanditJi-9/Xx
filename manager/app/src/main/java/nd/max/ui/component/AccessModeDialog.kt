/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.privilege.AccessFilter
import nd.max.core.privilege.AccessFilterStore
import nd.max.core.privilege.ExecutionMode
import nd.max.core.privilege.ExecutionModeStore
import nd.max.core.privilege.PrivilegeLevel
import nd.max.core.privilege.PrivilegeManager
import nd.max.core.privilege.RootManager
import nd.max.core.privilege.RootManagerDetector
import nd.max.core.privilege.SelinuxMode
import nd.max.core.privilege.SelinuxStatus
import nd.max.ui.design.MaxSpace

/**
 * نافذة وضع الوصول: الطبقة الفعلية، ومن يدير الجذر، وحالة SELinux، ومحدّد الوضع والفلتر.
 *
 * **ولماذا نافذة لا شاشة:** هذا قرار يُتَّخذ من مكانين (الرئيسية والإعدادات) ولا يستحقّ
 * `MaxDestination` جديدة؛ والنافذة تُغلق وتعيد المستخدم حيث كان.
 *
 * **وحدّ الصدق:** «Root» قابل للاختيار ولو لم يُمنح — لأنه نيّةٌ تُطلب عند أوّل أمر؛ والشرح يقول
 * إنه غير ممنوح الآن. ولا نُخفي خيارًا لنجمّل الحالة.
 */
@Composable
fun AccessModeDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val snapshot by PrivilegeManager.snapshot.collectAsState()

    var mode by remember { mutableStateOf(ExecutionModeStore.current()) }
    var filter by remember { mutableStateOf(AccessFilterStore.current()) }
    var rootManager by remember { mutableStateOf(RootManager.NONE) }
    var selinux by remember { mutableStateOf(SelinuxMode.UNKNOWN) }

    LaunchedEffect(Unit) {
        PrivilegeManager.refresh()
        selinux = withContext(Dispatchers.IO) { SelinuxStatus.read() }
    }
    LaunchedEffect(snapshot.rootGranted) {
        rootManager = withContext(Dispatchers.IO) { RootManagerDetector.detect(snapshot.rootGranted) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.access_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
            ) {
                FactRow(stringResource(R.string.access_status_level), stringResource(snapshot.level.labelRes))
                FactRow(stringResource(R.string.access_status_root_manager), stringResource(rootManager.labelRes))
                FactRow(stringResource(R.string.access_status_selinux), stringResource(selinux.labelRes))

                SectionTitle(stringResource(R.string.access_mode_section))
                ExecutionMode.entries.forEach { entry ->
                    ChoiceRow(
                        title = stringResource(entry.labelRes()),
                        subtitle = stringResource(entry.descriptionRes(snapshot.level, snapshot.shizuku.ready)),
                        selected = mode == entry,
                        onSelect = {
                            mode = entry
                            ExecutionModeStore.save(context, entry)
                            PrivilegeManager.refresh()
                        }
                    )
                }

                SectionTitle(stringResource(R.string.access_filter_section))
                AccessFilter.entries.forEach { entry ->
                    ChoiceRow(
                        title = stringResource(entry.labelRes()),
                        subtitle = stringResource(entry.descriptionRes()),
                        selected = filter == entry,
                        onSelect = {
                            filter = entry
                            AccessFilterStore.save(context, entry)
                        }
                    )
                }

                Text(
                    text = stringResource(R.string.access_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.access_close)) }
        }
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = MaxSpace.sm)
    )
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(MaxSpace.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun ExecutionMode.labelRes(): Int = when (this) {
    ExecutionMode.AUTO -> R.string.lobby_mode_auto
    ExecutionMode.ROOT -> R.string.lobby_mode_root
    ExecutionMode.SHIZUKU -> R.string.lobby_mode_shizuku
    ExecutionMode.BASIC -> R.string.lobby_mode_basic
}

private fun ExecutionMode.descriptionRes(level: PrivilegeLevel, shizukuReady: Boolean): Int = when (this) {
    ExecutionMode.AUTO -> R.string.access_mode_auto_desc
    ExecutionMode.ROOT -> if (level == PrivilegeLevel.ROOT) {
        R.string.access_mode_root_desc_granted
    } else {
        R.string.access_mode_root_desc_missing
    }
    ExecutionMode.SHIZUKU -> if (shizukuReady) {
        R.string.access_mode_shizuku_desc_ready
    } else {
        R.string.access_mode_shizuku_desc_missing
    }
    ExecutionMode.BASIC -> R.string.access_mode_basic_desc
}

private fun AccessFilter.labelRes(): Int = when (this) {
    AccessFilter.SUPPORTED_ONLY -> R.string.access_filter_supported
    AccessFilter.EVERYTHING -> R.string.access_filter_everything
}

private fun AccessFilter.descriptionRes(): Int = when (this) {
    AccessFilter.SUPPORTED_ONLY -> R.string.access_filter_supported_desc
    AccessFilter.EVERYTHING -> R.string.access_filter_everything_desc
}

private fun RootManager.labelRes(): Int = when (this) {
    RootManager.NONE -> R.string.access_root_manager_none
    RootManager.MAGISK -> R.string.access_root_manager_magisk
    RootManager.KERNEL_SU -> R.string.access_root_manager_kernel_su
    RootManager.APATCH -> R.string.access_root_manager_apatch
    RootManager.OTHER -> R.string.access_root_manager_other
}

private fun SelinuxMode.labelRes(): Int = when (this) {
    SelinuxMode.ENFORCING -> R.string.access_selinux_enforcing
    SelinuxMode.PERMISSIVE -> R.string.access_selinux_permissive
    SelinuxMode.UNKNOWN -> R.string.access_selinux_unknown
}
