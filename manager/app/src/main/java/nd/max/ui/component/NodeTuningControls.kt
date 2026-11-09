/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import nd.max.R
import nd.max.core.hardware.NodeControl
import nd.max.core.hardware.NodeTuningOutcome
import nd.max.core.hardware.NodeTuningResult
import nd.max.core.hardware.NodeValueFormat
import nd.max.ui.design.MaxChoiceRow
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxTone
import nd.max.ui.design.content

/** The draft never masquerades as a live value; writes require a distinct action and consent. */
@Composable
fun NodeTuningControls(
    controls: List<NodeControl>,
    busy: Boolean,
    titleOf: @Composable (NodeControl) -> String,
    canRestore: (NodeControl) -> Boolean,
    onApply: (NodeControl, String) -> Unit,
    onRestore: (NodeControl) -> Unit,
) {
    controls.forEachIndexed { index, control ->
        if (index > 0) MaxGroupDivider()
        MaxRow(
            title = titleOf(control),
            subtitle = stringResource(R.string.node_tuning_current, control.value),
        )
        when (val format = control.format) {
            is NodeValueFormat.Choice -> format.options.forEach { option ->
                MaxChoiceRow(
                    title = option,
                    selected = control.value == option,
                    enabled = !busy,
                    onSelect = { if (option != control.value) onApply(control, option) },
                )
            }
            else -> {
                var draft by remember(control.key, control.value) { mutableStateOf(control.value) }
                Column(
                    modifier = Modifier.padding(horizontal = MaxSpace.rowPaddingHorizontal, vertical = MaxSpace.sm),
                    verticalArrangement = Arrangement.spacedBy(MaxSpace.sm),
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { if (it.length <= 64) draft = it },
                        label = { Text(stringResource(R.string.node_tuning_requested)) },
                        supportingText = {
                            Text(when (format) {
                                is NodeValueFormat.Number -> stringResource(R.string.node_tuning_range, format.min, format.max)
                                is NodeValueFormat.Vector -> stringResource(R.string.node_tuning_vector_range, format.size, format.min, format.max)
                                else -> ""
                            })
                        },
                        isError = draft.isNotBlank() && !control.accepts(draft),
                        enabled = !busy,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = if (format is NodeValueFormat.Number) KeyboardType.Number else KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
                        StudioTonalButton(
                            onClick = { onApply(control, draft) },
                            enabled = !busy && draft != control.value && control.accepts(draft),
                        ) { Text(stringResource(R.string.node_tuning_apply)) }
                        StudioTextButton(
                            onClick = { onRestore(control) },
                            enabled = !busy && canRestore(control),
                        ) { Text(stringResource(R.string.node_tuning_restore)) }
                    }
                }
            }
        }
        if (control.format is NodeValueFormat.Choice) {
            StudioTextButton(onClick = { onRestore(control) }, enabled = !busy && canRestore(control)) {
                Text(stringResource(R.string.node_tuning_restore))
            }
        }
    }
}

@Composable
fun NodeTuningFeedback(result: NodeTuningResult?) {
    if (result == null) return
    Text(
        text = stringResource(when (result.outcome) {
            NodeTuningOutcome.APPLIED -> R.string.node_tuning_applied
            NodeTuningOutcome.INVALID -> R.string.node_tuning_invalid
            NodeTuningOutcome.UNREADABLE -> R.string.node_tuning_unreadable
            NodeTuningOutcome.BLOCKED -> R.string.node_tuning_blocked
            NodeTuningOutcome.WRITE_FAILED -> R.string.node_tuning_failed
            NodeTuningOutcome.UNVERIFIED -> R.string.node_tuning_unverified
        }),
        style = MaterialTheme.typography.bodySmall,
        color = if (result.outcome == NodeTuningOutcome.APPLIED) MaxTone.Positive.content() else MaxTone.Caution.content(),
        modifier = Modifier.padding(horizontal = MaxSpace.rowPaddingHorizontal, vertical = MaxSpace.sm),
    )
}
