/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nd.max.R
import nd.max.core.diagnostics.HardwareRouteHealth
import nd.max.core.hardware.AccessLevel
import nd.max.core.hardware.HardwareCapabilitySnapshot
import nd.max.core.hardware.capabilityRows
import nd.max.ui.mainscreens.IconBadge
import nd.max.ui.theme.MaxTextRole

/**
 * كم دليلًا يُعرض في سطر القدرة قبل أن يُختصر.
 *
 * **والسقف يُعلَن ولا يُسكت عنه:** كان `evidence.take(3)` يقطع بلا كلمة، فيقرأ المستخدم
 * ثلاثة أدلّة ويظنّها كلّ ما عُرف عن القدرة — وهو نفس العطب الذي عالجه
 * `unavailable_hidden_count` أسفل البطاقة: الإخفاء يُقال. والسقف هنا رقم مسمّى لأن
 * «ثلاثة» في موضعين تتباعد صامتة.
 */
private const val EVIDENCE_PREVIEW = 3

/**
 * يفصل ما يُعرض من الأدلّة عن عدده الباقي — **دالّة صافية تُقاس في JVM**.
 *
 * ولماذا خرجت من `Text`: القرار «كم أُخفي» يجب أن يُقاس باختبار لا أن يُقرأ بالعين في
 * شاشة. والعدّ هنا لا يمكن أن يكون سالبًا، ولا أن يعرض السطر أكثر من [limit] عنصرًا —
 * وهما الشرطان الوحيدان اللذان يحتاجهما السطر.
 */
internal fun capabilityEvidenceSplit(
    evidence: List<String>,
    limit: Int = EVIDENCE_PREVIEW,
): Pair<List<String>, Int> {
    val shown = evidence.take(limit)
    return shown to (evidence.size - shown.size)
}

/**
 * The capability matrix, with each control's **activation verdict** under it.
 *
 * The access level answers "is this interface readable or writable"; the verdict answers the different
 * question the user is actually asking when a control does nothing — "is there a route this build can
 * prove for applying it". Both are shown because they fail differently: a control can be perfectly
 * writable and still have no verified route that survives a vendor daemon, and it can be readable with
 * no route at all.
 *
 * Moved out of `DiagnosticsScreen` on its own merits (a screen should not own a 55-line card) and
 * because keeping it inline had pushed that file past the project's file-size ceiling.
 */
@Composable
fun CapabilityMatrixCard(
    snapshot: HardwareCapabilitySnapshot?,
    routes: List<HardwareRouteHealth.Verdict>,
    onRefresh: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    // البوابة الواحدة لخيار §٤-و: «هل تُعرض الميزات غير المتاحة؟» — تُقرأ عند التركيب كقراءة
    // تفضيل الحركة نفسه، ومن قلّب المفتاح يعيد فتح الشاشة فيُعاد التركيب.
    val showUnavailable = rememberUnavailableVisible()
    MaxSurface(modifier = Modifier.padding(top = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(Icons.Outlined.Memory, MaterialTheme.colorScheme.primary, 36)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.diagnostics_capability_matrix),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    snapshot?.let { "${it.vendor} • ${it.platform}" }
                        ?: stringResource(R.string.diagnostics_detecting_interfaces),
                    style = MaxTextRole.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StudioTextButton(onClick = onRefresh) { Text(stringResource(R.string.diagnostics_refresh)) }
        }
        Spacer(Modifier.height(10.dp))
        // **والصفوف تُبنى من دالّة نقيّة** (`capabilityRows`) لا بشرطٍ مكتوب هنا: نفس الطيّ
        // يُقاس في اختبار JVM (`UnavailableFeaturesTest`) — فالسياسة تُقاس والرسم يُرى.
        val rows = snapshot?.features?.values?.let { capabilityRows(it, showUnavailable) }.orEmpty()
        val unavailable = snapshot?.features?.values?.count { it.access == AccessLevel.NONE } ?: 0
        val hidden = if (showUnavailable) 0 else unavailable
        rows.forEach { capability ->
            val label = capability.feature.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
            val status = when (capability.access) {
                AccessLevel.READ_WRITE -> stringResource(R.string.diagnostics_access_read_write)
                AccessLevel.READ_ONLY -> stringResource(R.string.diagnostics_access_read_only)
                AccessLevel.NONE -> stringResource(R.string.diagnostics_access_none)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    // **والرمادي الهادئ لقدرة غير متاحة** (§٤-و): نفس نصّها، ولونها أخفت — فتُقرأ
                    // «موجودة ولا تعمل هنا» لا «محذوفة»، ولا «تعمل مثل غيرها من الصفوف».
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (capability.access == AccessLevel.NONE) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            Color.Unspecified
                        },
                    )
                    Text(
                        "${capability.backend} • $status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RouteVerdictLabel(routes, capability.feature)
                    if (capability.evidence.isNotEmpty()) {
                        val (shown, more) = capabilityEvidenceSplit(capability.evidence)
                        // **وما قُطع يُقال بعددِه:** «+2» في آخر السطر يُعلن أنّ هناك دليلًا
                        // آخر لم يُقرأ، بدل سطر يبدو كاملًا وهو ناقص.
                        val text = if (more > 0) {
                            shown.joinToString("  ·  ") + "  ·  " +
                                stringResource(R.string.diagnostics_capability_evidence_more, more)
                        } else {
                            shown.joinToString("  ·  ")
                        }
                        Text(
                            text,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        )
                    }
                }
            }
        }
        // **والإخفاء يُقال ولا يُسكت عنه:** من أخفى صفوفًا يُخبره العدد المخفيّ — فلا يظنّ
        // المصفوفة كاملة وهي ناقصةٌ بتفضيله هو، ولا يُضطرّ ليفتح المفتاح ليعرف ما أخفاه.
        if (hidden > 0) {
            Text(
                stringResource(R.string.unavailable_hidden_count, hidden),
                style = MaxTextRole.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Text(
            stringResource(R.string.diagnostics_capability_matrix_hint),
            style = MaxTextRole.description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        // **والباب في آخر البطاقة لا في صفّ عنوانها:** صفُّ العنوان يحمل عنوانًا ووصفًا وزرّ
        // تحديث، فضغطةٌ ثالثة فيه تزاحم القراءة؛ وهنا يُقرأ **إجراءً على البطاقة** بعد أن
        // تُقرأ بياناتها. وهو سطر رابط لا كبسولة (`MaxDeviceInfoShortcut`)، ويُمرَّر له صفر
        // حاشية لأن هذه البطاقة تحشو نفسها أصلًا.
        trailing?.invoke()
    }
}
