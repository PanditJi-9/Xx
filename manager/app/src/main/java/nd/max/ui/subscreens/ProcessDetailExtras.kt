/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

/*
 * مدير العمليات — **بقيّة تفاصيل العملية** التي كانت تُقرأ ولا تُعرض.
 *
 * `ProcessFeed` كان يقرأ `/proc/<pid>/status` كاملًا ثم يعرض ستة حقول، فتبقى العملية الأمّ
 * والحالة والمستخدم والأنوية وسطر الأمر في النصّ المقروء بلا سطح يقرأها فيه المستخدم. وهي
 * **لا تحتاج قياسًا جديدًا ولا نداء جذر إضافيًّا** (سطر الأمر وحده يُلحق بالنداء نفسه)، فإضافةُ
 * صفوفها تصحيحُ عرضٍ لا بناءُ قدرة ثانية — وهذا حدّها (§0.5).
 *
 * وسطر الأمر **يُنتقى لا يُقصّ**: هو النصّ الذي ينسخه المستخدم ليبحث أو ليكرّر تشخيصًا، فحُوِّل
 * إلى `SelectionContainer`. والحالة تُترجم رموز النواة إلى كلمة قارئها يفهمها، بدل حرف لاتيني
 * واحد يُقرأ لغزًا؛ والرمز يبقى ظاهرًا لأنّه ما تعلنه النواة.
 *
 * وما لم يُقرأ يبقى «—» ولا يُخترع له صفر ولا نصّ (ADR-07).
 */
package nd.max.ui.subscreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import nd.max.R
import nd.max.core.platform.ProcessDetails
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import nd.max.ui.design.MaxSpace

@Composable
internal fun ProcessDetailExtras(details: ProcessDetails) {
    DetailLine(
        stringResource(R.string.processmgr_detail_ppid),
        details.ppid?.toString() ?: MAX_VALUE_UNAVAILABLE
    )
    DetailLine(
        stringResource(R.string.processmgr_detail_state),
        kernelStateText(details.state)
    )
    DetailLine(
        stringResource(R.string.processmgr_detail_uid),
        details.uid?.toString() ?: MAX_VALUE_UNAVAILABLE
    )
    DetailLine(
        stringResource(R.string.processmgr_detail_cpus),
        details.cpusAllowed ?: MAX_VALUE_UNAVAILABLE
    )
    val command = details.cmdline ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaxSpace.xs),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Text(
            text = stringResource(R.string.processmgr_detail_command),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SelectionContainer {
            Text(
                text = command,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * رمز حالة العملية عند النواة مع كلمته: الرمز يبقى لأنّه المُعلن، والكلمة تُفهم.
 * وما لا نعرفه يُعرض رمزه كما هو بدل تخمين معنى.
 */
@Composable
private fun kernelStateText(state: String?): String {
    if (state == null) return MAX_VALUE_UNAVAILABLE
    return when (state) {
        "R" -> stringResource(R.string.processmgr_state_running)
        "S" -> stringResource(R.string.processmgr_state_sleeping)
        "D" -> stringResource(R.string.processmgr_state_disk)
        "Z" -> stringResource(R.string.processmgr_state_zombie)
        "T", "t" -> stringResource(R.string.processmgr_state_stopped)
        "I" -> stringResource(R.string.processmgr_state_idle)
        else -> state
    }
}
