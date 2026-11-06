/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nd.max.R
import nd.max.core.policy.ExcludedCapability
import nd.max.ui.mainscreens.IconBadge
import nd.max.ui.theme.MaxTextRole

/**
 * سجلّ المستبعَد مع تحذيره: ما لا يقدّمه التطبيق، ولماذا — في شاشة التشخيص وحدها.
 *
 * **ولماذا بطاقة مستقلّة لا صفوفًا داخل [CapabilityMatrixCard]:** مصفوفة القدرات تصف **ما
 * وجده الجهاز** (مسار، صلاحية، حالة)، وهذه تصف **قرارنا نحن** (رفضٌ، أو منصّة لا تقدّم، أو
 * خطرٌ مشحون خلف إقرار). خلطهما يجعل «مرفوض بقرارٍ» يُقرأ كأنه «غير مدعوم هنا» — وهو كذبٌ على
 * المستخدم وعلى الكود معًا.
 *
 * **ولا مفتاح هنا إطلاقًا** — لا `Switch` مطفأ ولا صفّ قابل للضغط (§0.5 من `AGENTS.md` ·
 * `ADR-07`): القائمة تُعلن، والفعل يعيش في مكانه مع إقراره (`ADR-90`). ولذلك لا `onClick`
 * في توقيعها.
 */
@Composable
fun ExcludedCapabilitiesCard(modifier: Modifier = Modifier) {
    MaxSurface(modifier = modifier.padding(top = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // أحمر لا رماديّ: هذه منطقة «لن يُفعل»، والرماديّ يوحي بأنه مطفأ فحسب.
            IconBadge(Icons.Rounded.Warning, MaterialTheme.colorScheme.error, 36)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(
                    stringResource(R.string.diagnostics_excluded_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.diagnostics_excluded_desc),
                    style = MaxTextRole.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        ExcludedCapability.grouped().forEach { (kind, rows) ->
            if (rows.isEmpty()) return@forEach
            Text(
                stringResource(kind.label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
            rows.forEach { capability ->
                Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text(
                        stringResource(capability.title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        stringResource(capability.reason),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // **والتحذير نفسه صريح:** القائمة ليست قائمة انتظار. من قرأها يجب ألّا يخرج بظنّ أن
        // إقرارًا أو جذرًا سيُشغّل ما رُفض، ولا أن ما لا تُظهره النواة صار متاحًا بجهدٍ أكثر.
        Text(
            stringResource(R.string.diagnostics_excluded_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
