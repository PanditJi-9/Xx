/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import nd.max.R
import nd.max.core.privilege.AccessFilter
import nd.max.core.privilege.AccessFilterStore
import nd.max.ui.design.MaxCardSpec
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace

/**
 * **مفتاح الكشف: «إظهار الميزات التي تحتاج صلاحية أعلى».**
 *
 * حين يكون وضع التشغيل Shizuku أو Basic، يُخفي [AccessFilter.SUPPORTED_ONLY] كل وجهة تحتاج
 * صلاحية أعلى — أدوات في التحكّم، وبطاقات في الرئيسية، وشاشات في المُوجِّد. فيرى المستخدم
 * تطبيقًا ناقصًا ولا يعرف أن ما ينقصه موجودٌ ومنتظرٌ صلاحية. وهذا المفتاح يقلب الفلتر إلى
 * [AccessFilter.EVERYTHING] فتظهر الأدوات والبطاقات والشاشات موسومةً بمستواها.
 *
 * **ولماذا بطاقةٌ خاصة لا `ExpressiveSwitchItem`:** النصّ الطويل كان محصورًا بين أيقونةٍ في
 * الطرف ومفتاحٍ في الطرف الآخر، فينكسر إلى أربعة أسطر قصيرة ويُقرأ كعمودٍ في وسط البطاقة —
 * وهو العطب الظاهر في لقطة المالك. فالترتيب هنا: الأيقونة، ثم عمود نصّ يأخذ العرض، ثم المفتاح
 * في طرفه. ولا تتراكم الأيقونتان على النصّ.
 *
 * **والحدّ:** الكشف لا يمنح صلاحية ولا يشغّل شيئًا. الوجهةُ الظاهرة تعمل فقط إذا كان مستواها
 * متاحًا؛ وما دون ذلك يُعرض بوسمه كما يفعل [PrivilegePanel]. ولذلك يبقى المصدر واحدًا:
 * [AccessFilterStore] — لا تفضيل ثانٍ يُكتب بجواره فينفصل الحالان.
 */
@Composable
fun AccessRevealSwitch() {
    val context = LocalContext.current
    val filter by AccessFilterStore.state.collectAsState()
    val checked = filter == AccessFilter.EVERYTHING
    val interaction = remember { MutableInteractionSource() }

    // The caller already supplies the grouped-list card (`ExpressiveList`). This is the row,
    // not a second card: nested shells were the same "card inside a card" defect the design
    // system removed elsewhere.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Switch,
                onValueChange = { reveal ->
                    AccessFilterStore.save(
                        context,
                        if (reveal) AccessFilter.EVERYTHING else AccessFilter.SUPPORTED_ONLY,
                    )
                },
            )
            .padding(horizontal = MaxCardSpec.padding, vertical = MaxSpace.sm),
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
        verticalAlignment = Alignment.Top,
    ) {
            Icon(
                imageVector = if (checked) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = MaxSpace.hairline)
                    .size(MaxSize.iconGlyph),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
            ) {
                Text(
                    text = stringResource(R.string.access_reveal_title),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.access_reveal_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            MaxSwitch(
                checked = checked,
                onCheckedChange = null,
                enabled = true,
                interactionSource = interaction,
                modifier = Modifier.padding(top = MaxSpace.hairline),
            )
    }
}
