/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * بطاقة قدرة **غير مدعومة** — عرضٌ لا مفتاح.
 *
 * ولا تُبنى من [MaxRow] لأن الصفّ يوحي بأن خلفه فعلًا: صفٌّ ذو عنوان يُضغط، أو مفتاح يُقلب.
 * هذه بطاقة تقول ثلاثة أشياء بالترتيب نفسه في كل شاشة: **ما هي**، **لماذا خطر**، **ولماذا لا
 * تُضاف** — ثم سطر المسؤولية. ولا زرّ ولا مفتاح ولا «قريبًا»: وعدٌ بدعم غير موجود أسوأ من
 * الصمت، لأنه يجعل المستخدم ينتظره بدل أن يبحث عن طريق آمن.
 *
 * @param danger سطر واحد يبدأ بنوع الخطر — لا يُخفَّف إلى «تحذير» عام.
 * @param responsibility سطر المسؤولية: الاختيار خارج التطبيق مسؤولية من اختاره.
 */
@Composable
fun MaxBlockedFeatureCard(
    title: String,
    danger: String,
    reason: String,
    responsibility: String,
    modifier: Modifier = Modifier,
) {
    val tone = MaxTone.Caution
    MaxCardShell(
        modifier = modifier
            .fillMaxWidth()
            .semantics { stateDescription = danger },
        borderColor = tone.border(),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
        ) {
            Surface(
                shape = RoundedCornerShape(maxRadius(MaxRadius.row)),
                color = tone.container(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = tone.content(),
                    modifier = Modifier
                        .padding(MaxSpace.xs)
                        .size(MaxSize.iconGlyph)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = danger,
                    style = MaterialTheme.typography.labelSmall,
                    color = tone.content()
                )
            }
        }
        Text(
            text = reason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = responsibility,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
