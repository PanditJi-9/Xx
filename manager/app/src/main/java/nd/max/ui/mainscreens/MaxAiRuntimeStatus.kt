/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.mainscreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nd.max.R
import nd.max.core.maxai.MaxAiCycleStatus

/**
 * زرّ صغير لدورة واحدة — لا بطاقة.
 *
 * **ولماذا تغيّر (طلب المالك ٢٠٢٦-١٠-٢٠):** كان صفًّا كاملًا بعنوان وسطر شرح في أعلى الشاشة،
 * فيقرأ المستخدم «إجراء تشغيليّ» قبل أن يرى ما يفعله المحرّك. صار زرًّا هادئًا في آخر صفّ
 * الرأس: وجوده كافٍ، وشرحه لا يزاحم المحتوى. وحالات المحرّك الثلاث بقيت في نصّه نفسه.
 */
@Composable
internal fun MaxAiRuntimeStatus(status: MaxAiCycleStatus, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(
            onClick = onRefresh,
            enabled = !status.inFlight,
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(
                    when {
                        status.inFlight -> R.string.max_ai_cycle_running
                        status.lastFailureAtMs != null -> R.string.max_ai_cycle_failed
                        else -> R.string.max_ai_refresh
                    },
                ),
            )
        }
    }
}
