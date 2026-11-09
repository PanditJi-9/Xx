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

/**
 * عتبات قاتل الذاكرة — **ميزة تعمل فعلًا**، لا بطاقة تعريف.
 *
 * **ما تفعله:** تكتب متجه `minfree` في النواة بعد إقرار خطر صريح، وتتحقّق بالقراءة المرتجعة.
 * **وما لا تفعله:** لا تظهر أصلًا إن لم تنشر النواة الجدول (نوى حديثة كثيرة لا تنشره)، فلا وعد
 * بميزة غير موجودة، ولا مفتاح يُضغط بلا أثر.
 *
 * **والخطورة مقيسة بالرقم لا بالوسم:** عتبة أعلى تقتل تطبيقات في الخلفية، وأدنى تترك الذاكرة
 * تمتلئ؛ فالمعامل محدود ٥٠–٢٠٠٪، وقيمة لحظة الفتح محفوظة في الجلسة للرجوع إليها بضغطة.
 */
@Composable
fun LmkSection() {
    val scope = rememberCoroutineScope()
    val io = remember { LmkTuning.Io.system() }

    var table by remember { mutableStateOf<List<Int>?>(null) }
    var opened by remember { mutableStateOf<List<Int>?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<LmkTuning.Outcome?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pendingTarget by remember { mutableStateOf<List<Int>?>(null) }

    LaunchedEffect(Unit) {
        val live = withContext(Dispatchers.IO) { io.current() }
        table = live
        opened = live
        loaded = true
    }

    fun apply(target: List<Int>) {
        if (busy) return
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { io.apply(target) }
            outcome = result
            table = withContext(Dispatchers.IO) { io.current() }
            busy = false
        }
    }

    val gate = rememberDangerGate(
        id = DangerConsent.MEMORY_LMK,
        title = stringResource(R.string.lmk_danger_title),
        message = stringResource(R.string.lmk_danger_body),
        confirmLabel = stringResource(R.string.lmk_danger_confirm),
        technicalDetail = LmkTuning.MINFREE,
        onAllowed = {
            pendingTarget?.let { target -> apply(target) }
            pendingTarget = null
        }
    )

    val current = table
    // النواة لا تنشر الجدول ⇒ لا قسم ولا سطر. الغياب يُعلن بعدم الظهور، لا ببطاقة عن ميزةٍ ليست هنا.
    if (loaded && current == null) return

    val megabytes = current?.joinToString(" · ") { LmkTuning.pageToMegabytes(it).toString() }
    val pages = current?.joinToString(",")
    val canAct = current != null && !busy

    MaxSection(
        title = stringResource(R.string.lmk_title),
        description = stringResource(R.string.lmk_desc),
    ) {
        MaxGroup {
            MaxRow(
                title = stringResource(R.string.lmk_current),
                subtitle = if (megabytes != null && pages != null) {
                    stringResource(R.string.lmk_current_value, megabytes, pages)
                } else {
                    stringResource(R.string.vm_loading)
                },
                icon = Icons.Rounded.ArrowDownward,
                iconTone = MaxTone.Neutral,
            )
            MaxGroupDivider()
            MaxRow(
                title = stringResource(R.string.lmk_soften),
                subtitle = stringResource(R.string.lmk_soften_desc),
                icon = Icons.Rounded.ArrowDownward,
                iconTone = MaxTone.Caution,
                enabled = canAct,
                onClick = {
                    val target = current?.let { LmkTuning.scale(it, 75) }
                    if (target != null) {
                        pendingTarget = target
                        gate()
                    }
                }
            )
            MaxGroupDivider()
            MaxRow(
                title = stringResource(R.string.lmk_harden),
                subtitle = stringResource(R.string.lmk_harden_desc),
                icon = Icons.Rounded.ArrowUpward,
                iconTone = MaxTone.Caution,
                enabled = canAct,
                onClick = {
                    val target = current?.let { LmkTuning.scale(it, 125) }
                    if (target != null) {
                        pendingTarget = target
                        gate()
                    }
                }
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
                    if (target != null) {
                        pendingTarget = target
                        gate()
                    }
                }
            )
        }

        outcome?.let { result ->
            Text(
                text = stringResource(
                    when (result) {
                        LmkTuning.Outcome.APPLIED -> R.string.lmk_outcome_applied
                        LmkTuning.Outcome.NOT_PRESENT -> R.string.lmk_outcome_not_present
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
