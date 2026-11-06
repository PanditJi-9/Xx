/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import nd.max.core.policy.DangerConsent
import nd.max.ui.design.MaxConfirmDialog

/**
 * بوابة الإقرار للميزات الخطرة — تنفيذ `ADR-90` في مكان واحد بدل حوارٍ محلّي في كل شاشة.
 *
 * **الاستعمال:** تُستدعى مرة في الشاشة، وتُعيد دالةً تُنفّذ الفعل الخطير: يُمرَّر `id` من
 * [DangerConsent]، ونصوصٌ من `stringResource` تُسمّي ما سيُكتب وأثره، ثم الفعل نفسه في `onAllowed`.
 *
 * **ولماذا دالةٌ عائدة لا حوارًا يُركّب يدويًّا:** الشاشة التي تنسى تركيب الحوار لا تنسى استدعاء
 * الفعل؛ فالبوابة تسكن **على مسار الفعل نفسه**، ويستحيل تجاوزها سهوًا كما يحدث حين يُكتب
 * `MaxConfirmDialog` في مكان و`onClick` في مكان آخر.
 *
 * **وحدودها:** لا تمنح صلاحية نظامية، ولا تتجاوز الـarbiter (`ADR-11`)، ولا تُسقط الحاجز التقنيّ:
 * ما يرفضه النظام يبقى مرفوضًا. وأول إقرار فقط يُسجَّل؛ ومن أراد التراجع يستدعي
 * [DangerConsent.revoke] صريحًا — لا يُلغى تلقائيًّا بالخروج من الشاشة.
 */
@Composable
fun rememberDangerGate(
    id: String,
    title: String,
    message: String,
    confirmLabel: String,
    technicalDetail: String? = null,
    onAllowed: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    var pending by remember(id) { mutableStateOf(false) }
    // الفعل قد يتغيّر بين إعادة التركيب (قيم السلايدر مثلًا)، فنقرأ أحدثه لا الذي وُلد مع الحوار.
    val currentOnAllowed by rememberUpdatedState(onAllowed)

    MaxConfirmDialog(
        visible = pending,
        title = title,
        message = message,
        confirmLabel = confirmLabel,
        technicalDetail = technicalDetail,
        onConfirm = {
            DangerConsent.acknowledge(context, id)
            pending = false
            currentOnAllowed()
        },
        onDismiss = { pending = false },
        icon = Icons.Rounded.Warning,
        destructive = true
    )

    return {
        if (DangerConsent.isAcknowledged(context, id)) onAllowed() else pending = true
    }
}
