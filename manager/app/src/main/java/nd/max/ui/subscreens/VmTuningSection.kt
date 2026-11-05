/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.ui.subscreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.platform.VmTunable
import nd.max.core.platform.VmTuning
import nd.max.core.platform.VmWriteOutcome
import nd.max.ui.component.StudioTextButton
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSpace

/**
 * قسم «مقابض نواة VM» داخل شاشة الذاكرة (FKM: `/proc/sys/vm`).
 *
 * **وموضعها هنا لا في شاشة مستقلّة:** هذه الشاشة تقيس ضغط الذاكرة أصلًا، ومقابض النواة هي اليد التي
 * تُحرّك ما تقيسه. وroute جديدة كانت تحتاج مدخلًا في التنقّل والكتالوج والأدوار لأجل عشرة صفوف.
 *
 * **وحدودها المعلنة:** لا تُخفى القيمة خارج مداها، ولا يُدَّعى نجاحٌ بلا قراءة مرتجعة — كل رفض
 * يظهر برمزه كما هو من [VmTuning]. والكتابة نفسها تمرّ بـ`RootFileAccess` وحده.
 */
@Composable
fun VmTuningSection() {
    val scope = rememberCoroutineScope()
    var values by remember { mutableStateOf<Map<VmTunable, Long?>>(emptyMap()) }
    var loaded by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<VmWriteOutcome?>(null) }

    LaunchedEffect(Unit) {
        values = withContext(Dispatchers.IO) { VmTuning.system().snapshot() }
        loaded = true
    }

    fun commit(tunable: VmTunable, direction: Long) {
        val current = values[tunable] ?: return
        val next = (current + direction * tunable.step).coerceIn(tunable.min, tunable.max)
        if (next == current) return
        scope.launch {
            val result = withContext(Dispatchers.IO) { VmTuning.system().set(tunable, next) }
            outcome = result
            values = withContext(Dispatchers.IO) { VmTuning.system().snapshot() }
        }
    }

    MaxSection(
        title = stringResource(R.string.vm_section_title),
        description = stringResource(R.string.vm_section_desc),
    ) {
        MaxGroup {
            when {
                !loaded -> MaxRow(title = stringResource(R.string.vm_loading))

                values.values.all { it == null } -> MaxRow(
                    title = stringResource(R.string.vm_absent_title),
                    subtitle = stringResource(R.string.vm_absent_desc),
                )

                else -> VmTunable.entries
                    .filter { values[it] != null }
                    .forEach { tunable ->
                        MaxRow(
                            title = tunable.path.substringAfterLast('/'),
                            subtitle = stringResource(
                                R.string.vm_range,
                                tunable.min.toString(),
                                tunable.max.toString(),
                            ),
                            trailing = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
                                ) {
                                    StudioTextButton(
                                        onClick = { commit(tunable, -1) },
                                        enabled = (values[tunable] ?: 0L) > tunable.min,
                                    ) {
                                        Text(stringResource(R.string.vm_step_down))
                                    }
                                    Text(
                                        text = values[tunable]?.toString().orEmpty(),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    StudioTextButton(
                                        onClick = { commit(tunable, 1) },
                                        enabled = (values[tunable] ?: 0L) < tunable.max,
                                    ) {
                                        Text(stringResource(R.string.vm_step_up))
                                    }
                                }
                            },
                        )
                    }
            }

            outcome?.let { result ->
                MaxRow(
                    title = stringResource(
                        when (result) {
                            VmWriteOutcome.APPLIED -> R.string.vm_outcome_applied
                            VmWriteOutcome.REJECTED_RANGE -> R.string.vm_outcome_range
                            VmWriteOutcome.REJECTED_INVALID -> R.string.vm_outcome_invalid
                            VmWriteOutcome.REJECTED_NOT_WRITABLE -> R.string.vm_outcome_unwritable
                            VmWriteOutcome.UNVERIFIED -> R.string.vm_outcome_unverified
                        },
                    ),
                )
            }
        }
    }
}
