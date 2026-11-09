/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import nd.max.R
import nd.max.core.policy.DangerConsent
import nd.max.ui.component.NodeTuningControls
import nd.max.ui.component.NodeTuningFeedback
import nd.max.ui.component.StudioTextButton
import nd.max.ui.component.rememberDangerGate
import nd.max.ui.design.MaxChoiceRow
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSpace
import nd.max.ui.util.ProfileSharing
import nd.max.ui.viewmodel.KernelTuningViewModel

@Composable
fun GovernorTuningSection(viewModel: KernelTuningViewModel = hiltViewModel()) {
    LifecycleResumeEffect(viewModel) {
        viewModel.load(KernelTuningViewModel.Domain.GOVERNOR, force = true)
        onPauseOrDispose { }
    }
    if (viewModel.governors.isEmpty() && viewModel.feedback(KernelTuningViewModel.Domain.GOVERNOR) == null) return
    val action = remember { arrayOfNulls<() -> Unit>(1) }
    val gate = rememberDangerGate(
        id = DangerConsent.GOVERNOR_TUNING,
        title = stringResource(R.string.governor_tuning_title),
        message = stringResource(R.string.governor_tuning_warning),
        confirmLabel = stringResource(R.string.node_tuning_apply),
    ) {
        val allowed = action[0]
        action[0] = null
        allowed?.invoke()
    }
    MaxSection(title = stringResource(R.string.governor_tuning_title), description = stringResource(R.string.governor_tuning_desc)) {
        viewModel.governors.forEach { group ->
            MaxSection(title = stringResource(R.string.governor_tuning_group, group.policy, group.governor)) {
                MaxGroup {
                    NodeTuningControls(
                        controls = group.controls,
                        busy = viewModel.busy || viewModel.scanning,
                        titleOf = { it.label },
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
        var profile by remember { mutableStateOf("custom") }
        val labels = listOf(
            stringResource(R.string.profile_label_power), stringResource(R.string.Profile_Balanced),
            stringResource(R.string.profile_label_gaming), stringResource(R.string.Profile_Performance),
            stringResource(R.string.profile_label_custom),
        )
        if (viewModel.governors.isNotEmpty()) MaxSection(title = stringResource(R.string.governor_tuning_presets), description = stringResource(R.string.governor_tuning_presets_desc)) {
            MaxGroup {
                ProfileSharing.KNOWN_PROFILES.forEachIndexed { index, name ->
                    MaxChoiceRow(title = labels[index], selected = profile == name, onSelect = { profile = name }, enabled = !viewModel.busy && !viewModel.scanning)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
                StudioTextButton(onClick = { viewModel.saveGovernorSnapshot(profile) }, enabled = !viewModel.busy && !viewModel.scanning) {
                    Text(stringResource(R.string.governor_tuning_save_current))
                }
                StudioTextButton(
                    onClick = { action[0] = { viewModel.applyGovernorSnapshot(profile) }; gate() },
                    enabled = !viewModel.busy && !viewModel.scanning && viewModel.hasGovernorSnapshot(profile),
                ) { Text(stringResource(R.string.node_tuning_apply)) }
            }
            viewModel.snapshotSaved?.let { saved ->
                Text(stringResource(if (saved) R.string.governor_tuning_saved else R.string.governor_tuning_save_failed))
            }
        }
        NodeTuningFeedback(viewModel.feedback(KernelTuningViewModel.Domain.GOVERNOR))
        StudioTextButton(onClick = { viewModel.load(KernelTuningViewModel.Domain.GOVERNOR, force = true) }, enabled = !viewModel.busy) {
            Text(stringResource(R.string.max_action_recheck))
        }
    }
}
