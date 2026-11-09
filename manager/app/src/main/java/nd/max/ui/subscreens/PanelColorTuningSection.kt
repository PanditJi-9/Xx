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
fun PanelColorTuningSection(viewModel: KernelTuningViewModel = hiltViewModel()) {
    LifecycleResumeEffect(viewModel) {
        viewModel.load(KernelTuningViewModel.Domain.COLOR, force = true)
        onPauseOrDispose { }
    }
    if (viewModel.colors.isEmpty() && viewModel.feedback(KernelTuningViewModel.Domain.COLOR) == null) return
    val action = remember { arrayOfNulls<() -> Unit>(1) }
    val gate = rememberDangerGate(
        id = DangerConsent.PANEL_COLOR_TUNING,
        title = stringResource(R.string.panel_color_tuning_title),
        message = stringResource(R.string.panel_color_tuning_warning),
        confirmLabel = stringResource(R.string.node_tuning_apply),
    ) {
        val allowed = action[0]
        action[0] = null
        allowed?.invoke()
    }
    MaxSection(title = stringResource(R.string.panel_color_tuning_title), description = stringResource(R.string.panel_color_tuning_desc)) {
        MaxGroup {
            NodeTuningControls(
                controls = viewModel.colors,
                busy = viewModel.busy || viewModel.scanning,
                titleOf = { control -> stringResource(when (control.label) {
                    "rgb" -> R.string.panel_color_tuning_rgb
                    "kcal_sat" -> R.string.panel_color_tuning_saturation
                    "kcal_cont" -> R.string.panel_color_tuning_contrast
                    else -> R.string.panel_color_tuning_value
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
        NodeTuningFeedback(viewModel.feedback(KernelTuningViewModel.Domain.COLOR))
        StudioTextButton(onClick = { viewModel.load(KernelTuningViewModel.Domain.COLOR, force = true) }, enabled = !viewModel.busy) {
            Text(stringResource(R.string.max_action_recheck))
        }
    }
}
