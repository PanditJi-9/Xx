/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.hardware.NodeCatalogReader
import nd.max.ui.mainscreens.IconBadge
import nd.max.ui.theme.MaxTextRole

/**
 * بطاقة **قراءة فقط** تقول أيّ عُقد النظام موجودة على هذا الجهاز.
 *
 * **ولماذا تظهر أصلًا:** محرّك الوصفات لا يُبنى على مسار غير موجود؛ وغياب العقدة هنا يفسّر لماذا
 * تبقى ميزة من ميزات تطبيقٍ مرجعيّ بلا أثر على جهاز بعينه — وهو تفسير لا ادّعاء. والباقة لا تكتب
 * ولا تمنح ملكية ولا تتجاوز الـarbiter (ADR-11)، والقراءة تجري خارج خيط الواجهة لأن ملفات sysfs
 * قد تتباطأ.
 *
 * **وما لا تقوله:** لا تقول «مدعومة» لمجرّد وجود العقدة — الوجود شرط لازم لا كافٍ، والكتابة
 * تحتاج صلاحية ومراجعة سلامة. ولذلك لا يوجد هنا مفتاح، ولا صفّ قابل للضغط.
 */
@Composable
fun NodeCatalogCard(modifier: Modifier = Modifier) {
    var summary by remember { mutableStateOf<NodeCatalogReader.Summary?>(null) }
    LaunchedEffect(Unit) {
        summary = withContext(Dispatchers.IO) {
            NodeCatalogReader.summarise(NodeCatalogReader.read())
        }
    }
    MaxSurface(modifier.padding(top = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Memory, MaterialTheme.colorScheme.primary, 36)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(
                    text = stringResource(R.string.diagnostics_nodes_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(R.string.diagnostics_nodes_desc),
                    style = MaxTextRole.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        val counts = summary
        if (counts != null) {
            Text(
                text = stringResource(R.string.diagnostics_nodes_present, counts.present, counts.total),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.diagnostics_nodes_absent, counts.absent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.diagnostics_nodes_unreadable, counts.unreadable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
