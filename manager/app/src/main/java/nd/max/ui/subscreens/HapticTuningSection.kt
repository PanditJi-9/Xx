/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import nd.max.R
import nd.max.core.policy.DangerConsent
import nd.max.ui.component.NodeTuningControls
import nd.max.ui.component.NodeTuningFeedback
import nd.max.ui.component.StudioTextButton
import nd.max.ui.component.rememberDangerGate
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxSection
import nd.max.ui.viewmodel.KernelTuningViewModel

@Composable
fun HapticTuningSection(viewModel: KernelTuningViewModel = hiltViewModel()) {
    LifecycleResumeEffect(viewModel) {
        viewModel.load(KernelTuningViewModel.Domain.HAPTIC, force = true)
        onPauseOrDispose { }
    }
    if (viewModel.haptics.isEmpty() && viewModel.feedback(KernelTuningViewModel.Domain.HAPTIC) == null) return
    val action = remember { arrayOfNulls<() -> Unit>(1) }
    val gate = rememberDangerGate(
        id = DangerConsent.HAPTIC_TUNING,
        title = stringResource(R.string.haptic_tuning_title),
        message = stringResource(R.string.haptic_tuning_warning),
        confirmLabel = stringResource(R.string.node_tuning_apply),
    ) {
        val allowed = action[0]
        action[0] = null
        allowed?.invoke()
    }
    MaxSection(title = stringResource(R.string.haptic_tuning_title), description = stringResource(R.string.haptic_tuning_desc)) {
        MaxGroup {
            NodeTuningControls(
                controls = viewModel.haptics,
                busy = viewModel.busy || viewModel.scanning,
                titleOf = { control -> stringResource(when (control.label) {
                    "touch" -> R.string.haptic_tuning_touch
                    "notification" -> R.string.haptic_tuning_notification
                    "call" -> R.string.haptic_tuning_call
                    else -> R.string.haptic_tuning_intensity
                }) },
                canRestore = viewModel::canRestore,
                onApply = { control, value ->
                    viewModel.prepare(control, value)
                    action[0] = viewModel::applyPending
                    gate()
                },
                onRestore = { control -> action[0] = { viewModel.restore(control) }; gate() },
            )
        }
        NodeTuningFeedback(viewModel.feedback(KernelTuningViewModel.Domain.HAPTIC))
        StudioTextButton(onClick = { viewModel.load(KernelTuningViewModel.Domain.HAPTIC, force = true) }, enabled = !viewModel.busy) {
            Text(stringResource(R.string.max_action_recheck))
        }
    }
}
