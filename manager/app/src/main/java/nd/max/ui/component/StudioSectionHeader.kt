/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import nd.max.ui.design.MaxSectionSpec

/**
 * Compact, direction-neutral hierarchy marker used by the rebuilt screens.
 *
 * **والعلامة شرطةٌ لا نجمة.** كانت كل رأس قسم يحمل `AutoAwesome` — وهو **رمز Max AI نفسه**
 * المستعمل في الرئيسية والسجلّ — فصار كل قسمٍ في التطبيق يشبه إعلانًا للذكاء الاصطناعي، وهي
 * بعينها ملاحظة التقييم التي عالجتها §٩: «AI seems to be overused». والآن العلامة هي موتيف
 * المشروع نفسه: **الشرطة الرأسية** بلون الشاشة بأرقام `MaxSectionSpec.accent*` — نفس أرقام
 * `NeuralSectionHeader` ووصف الشاشة (`MaxScreenDescription`) — فصارت لغة العلامات واحدة في
 * التطبيق: شرطةٌ تُعلّم العنوان، ولا رمزٌ مزخرف يُكرَّر في كل مكان.
 *
 * ولغة التصميم لا تفقد شيئًا: الشرطة **معلومةٌ** (هذا قسم، وهذا لون بابه) والنجمة كانت
 * زخرفةً لا تحمل شيئًا — والعقد القائم (`ADR-74`) ينصّ: «كل حركة ومعلومة لها غرض، ولا زخرفة».
 */
@Composable
fun StudioSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    // Static marker: section hierarchy must not compete with live telemetry.
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            Modifier
                .width(MaxSectionSpec.accentWidth)
                .height(MaxSectionSpec.accentHeight)
                .clip(RoundedCornerShape(MaxSectionSpec.accentRadius))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(MaxSectionSpec.accentGap))
        androidx.compose.foundation.layout.Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
