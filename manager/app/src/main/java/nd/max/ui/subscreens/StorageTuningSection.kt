/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import nd.max.R
import nd.max.core.hardware.NodeControl
import nd.max.core.policy.DangerConsent
import nd.max.ui.component.NodeTuningControls
import nd.max.ui.component.NodeTuningFeedback
import nd.max.ui.component.StudioTextButton
import nd.max.ui.component.rememberDangerGate
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxSection
import androidx.compose.material3.Text
import nd.max.ui.viewmodel.KernelTuningViewModel

@Composable
fun StorageTuningSection(viewModel: KernelTuningViewModel = hiltViewModel()) {
    LifecycleResumeEffect(viewModel) {
        viewModel.load(KernelTuningViewModel.Domain.STORAGE, force = true)
        onPauseOrDispose { }
    }
    if (viewModel.storage.isEmpty() && viewModel.feedback(KernelTuningViewModel.Domain.STORAGE) == null) return
    val action = remember { arrayOfNulls<() -> Unit>(1) }
    val gate = rememberDangerGate(
        id = DangerConsent.STORAGE_TUNING,
        title = stringResource(R.string.storage_tuning_title),
        message = stringResource(R.string.storage_tuning_warning),
        confirmLabel = stringResource(R.string.node_tuning_apply),
    ) {
        val allowed = action[0]
        action[0] = null
        allowed?.invoke()
    }
    MaxSection(title = stringResource(R.string.storage_tuning_title), description = stringResource(R.string.storage_tuning_desc)) {
        viewModel.storage.forEach { device ->
            MaxSection(
                title = device.name,
                description = stringResource(when (device.removable) {
                    true -> R.string.storage_tuning_removable
                    false -> R.string.storage_tuning_fixed
                    null -> R.string.storage_tuning_location_unknown
                }),
            ) {
                MaxGroup {
                    NodeTuningControls(
                        controls = device.controls,
                        busy = viewModel.busy || viewModel.scanning,
                        titleOf = { storageControlTitle(it) },
                        canRestore = viewModel::canRestore,
                        onApply = { control, value ->
                            viewModel.prepare(control, value)
                            action[0] = viewModel::applyPending
                            gate()
                        },
                        onRestore = { control -> action[0] = { viewModel.restore(control) }; gate() },
                    )
                }
            }
        }
        NodeTuningFeedback(viewModel.feedback(KernelTuningViewModel.Domain.STORAGE))
        StudioTextButton(onClick = { viewModel.load(KernelTuningViewModel.Domain.STORAGE, force = true) }, enabled = !viewModel.busy) {
            Text(stringResource(R.string.max_action_recheck))
        }
    }
}

@Composable
private fun storageControlTitle(control: NodeControl): String = stringResource(when (control.label) {
    "scheduler" -> R.string.io_scheduler
    "read_ahead_kb" -> R.string.storage_tuning_read_ahead
    "nr_requests" -> R.string.storage_tuning_requests
    "rq_affinity" -> R.string.storage_tuning_affinity
    else -> R.string.storage_tuning_title
})
