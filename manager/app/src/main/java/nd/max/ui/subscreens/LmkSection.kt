/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.platform.LmkTuning
import nd.max.core.policy.DangerConsent
import nd.max.ui.component.rememberDangerGate
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxTone
import nd.max.ui.design.content
import nd.max.ui.util.LmkPendingAction

/**
 * عتبات قاتل الذاكرة — **ميزة تعمل فعلًا**، لا بطاقة تعريف.
 *
 * **ما تفعله:** تكتب متجه `minfree` في النواة بعد إقرار خطر صريح، وتتحقّق بالقراءة المرتجعة.
 * **وما لا تفعله:** لا تظهر أصلًا إن لم تنشر النواة الجدول (نوى حديثة كثيرة لا تنشره)، فلا وعد
 * بميزة غير موجودة، ولا مفتاح يُضغط بلا أثر.
 *
 * الاختيار محسوب من لقطة فتح القسم، لا من آخر كتابة؛ فلا تتضاعف العتبات بتكرار الضغط.
 * كل كتابة تعيد فحص القيمة الحية قبل التنفيذ، والاسترجاع لقيمة الجلسة نفسها.
 */
@Composable
fun LmkSection(io: LmkTuning.Io) {
    val scope = rememberCoroutineScope()

    var table by remember(io) { mutableStateOf<List<Int>?>(null) }
    var opened by remember(io) { mutableStateOf<List<Int>?>(null) }
    var pageSizeBytes by remember(io) { mutableStateOf<Long?>(null) }
    var outcome by remember(io) { mutableStateOf<LmkTuning.Outcome?>(null) }
    var busy by remember(io) { mutableStateOf(false) }
    val pendingAction = remember(io) { LmkPendingAction() }
    var resumeGeneration by remember(io) { mutableStateOf(0) }

    LifecycleResumeEffect(io) {
        resumeGeneration++
        onPauseOrDispose { }
    }

    LaunchedEffect(io, resumeGeneration) {
        val live = withContext(Dispatchers.IO) { io.current() }
        table = live
        if (opened == null && live != null) opened = live
        pageSizeBytes = withContext(Dispatchers.IO) { io.pageSizeBytes() }
    }

    fun apply(target: List<Int>, expected: List<Int>) {
        if (busy) return
        busy = true
        outcome = null
        scope.launch {
            try {
                outcome = withContext(Dispatchers.IO) { io.apply(target, expected) }
                table = withContext(Dispatchers.IO) { io.current() }
            } finally {
                busy = false
            }
        }
    }

    val gate = rememberDangerGate(
        id = DangerConsent.MEMORY_LMK,
        title = stringResource(R.string.lmk_danger_title),
        message = stringResource(R.string.lmk_danger_body),
        confirmLabel = stringResource(R.string.lmk_danger_confirm),
        technicalDetail = LmkTuning.MINFREE,
        onAllowed = {
            pendingAction.take()?.let { request -> apply(request.target, request.expected) }
        },
    )

    val current = table
    // لا وميض لقسم عن ميزة غائبة أثناء الاكتشاف. يبقى آخر حكم كتابة ظاهرًا إن تعذرت القراءة بعدها.
    if (current == null && outcome == null) return

    val size = pageSizeBytes
    val megabyteValues = if (current != null && size != null) {
        current.map { LmkTuning.pageToMegabytes(it, size) }
    } else {
        null
    }
    val megabytes = megabyteValues?.takeIf { values -> values.all { it != null } }?.joinToString(" · ")
    val pages = current?.joinToString(",")
    val softer = opened?.let { LmkTuning.scale(it, 75) }
    val harder = opened?.let { LmkTuning.scale(it, 125) }
    val canAct = current != null && !busy

    MaxSection(
        title = stringResource(R.string.lmk_title),
        description = stringResource(R.string.lmk_desc),
    ) {
        MaxGroup {
            MaxRow(
                title = stringResource(R.string.lmk_current),
                subtitle = when {
                    megabytes != null && pages != null -> stringResource(R.string.lmk_current_value, megabytes, pages)
                    pages != null -> stringResource(R.string.lmk_current_pages, pages)
                    else -> stringResource(R.string.lmk_outcome_not_present)
                },
                icon = Icons.Rounded.Memory,
                iconTone = MaxTone.Neutral,
            )
            MaxGroupDivider()
            MaxRow(
                title = stringResource(R.string.lmk_soften),
                subtitle = stringResource(R.string.lmk_soften_desc),
                icon = Icons.Rounded.ArrowDownward,
                iconTone = MaxTone.Caution,
                enabled = canAct && softer != null && softer != current,
                onClick = {
                    val target = softer
                    if (target != null && current != null) {
                        pendingAction.set(target, current)
                        gate()
                    }
                },
            )
            MaxGroupDivider()
            MaxRow(
                title = stringResource(R.string.lmk_harden),
                subtitle = stringResource(R.string.lmk_harden_desc),
                icon = Icons.Rounded.ArrowUpward,
                iconTone = MaxTone.Caution,
                enabled = canAct && harder != null && harder != current,
                onClick = {
                    val target = harder
                    if (target != null && current != null) {
                        pendingAction.set(target, current)
                        gate()
                    }
                },
            )
            MaxGroupDivider()
            MaxRow(
                title = stringResource(R.string.lmk_restore),
                subtitle = stringResource(R.string.lmk_restore_desc),
                icon = Icons.Rounded.RestartAlt,
                iconTone = MaxTone.Accent,
                enabled = canAct && opened != null && opened != current,
                onClick = {
                    val target = opened
                    if (target != null && current != null) {
                        pendingAction.set(target, current)
                        gate()
                    }
                },
            )
            MaxGroupDivider()
            MaxRow(
                title = stringResource(R.string.max_action_recheck),
                icon = Icons.Rounded.Refresh,
                enabled = !busy,
                onClick = {
                    outcome = null
                    resumeGeneration++
                },
            )
        }

        outcome?.let { result ->
            Text(
                text = stringResource(
                    when (result) {
                        LmkTuning.Outcome.APPLIED -> R.string.lmk_outcome_applied
                        LmkTuning.Outcome.NOT_PRESENT -> R.string.lmk_outcome_not_present
                        LmkTuning.Outcome.INVALID -> R.string.lmk_outcome_invalid
                        LmkTuning.Outcome.BLOCKED -> R.string.lmk_outcome_blocked
                        LmkTuning.Outcome.NOT_WRITABLE -> R.string.lmk_outcome_not_writable
                        LmkTuning.Outcome.UNVERIFIED -> R.string.lmk_outcome_unverified
                    }
                ),
                style = MaterialTheme.typography.labelSmall,
                color = if (result == LmkTuning.Outcome.APPLIED) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaxTone.Caution.content()
                },
                modifier = Modifier.padding(horizontal = MaxSpace.groupPadding, vertical = MaxSpace.xs),
            )
        }
    }
}
