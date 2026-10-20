/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.ui.subscreens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.navigation.NavHostController
import nd.max.R
import nd.max.core.access.CommunityStats
import nd.max.ui.component.ExpressiveSwitchItem
import nd.max.ui.design.MaxBullets
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxScreen
import nd.max.ui.design.MaxSection

/**
 * **شاشة الموافقة الواحدة** (§١٠.٣ من `MAX-CONTROL-01`): تفعل ثلاثة أشياء لا رابع — تشرح،
 * وتعرض **الرسالة نفسها حرفيًّا**، وتضع مفتاحًا واحدًا. ولا نقرةُ «موافقة» في نافذة منبثقة
 * ولا خياران متداخلان: من يقرأ الشاشة يعرف بالضبط ما يخرج من جهازه.
 *
 * ## ولماذا تُعرض الرسالة نفسها
 *
 * `CommunityStats.preview()` تنادي بناء البناءَ الذي يُرسل فعلًا (`StatsRules.trackPayload`) —
 * لا وصفًا موازيًا له يمكن أن يتباعد عنه في تحديث لاحق. فما تراه العين هو البايتات، وهذا
 * ما يجعل الشاشة حجّةً لا وعدًا. (ويحرسه الاختبار: العارض والمرسل دالّة واحدة.)
 *
 * ## والسحب بضغطة واحدة
 *
 * إطفاء المفتاح فعلٌ له أثر لا رسالة: يحذف سجلّنا المحلّيّ كاملًا ويرسل طلب حذف واحدًا
 * (`forget_install`)، ثم تُحذف الهويّة المحلّيّة إن وصل الطلب — فمن يعود فيوافق يبدأ بهويّة
 * جديدة. والملخّص تحت المفتاح يقول ذلك بالنصّ لا بالإيماء.
 *
 * **والحدّ المُعلَن:** «هل يفهم المستخدم الشاشة من أول قراءة؟» — UAT على جهاز؛ ولا يُدّعى هنا
 * وضوحٌ ولا ذوق.
 */
@Composable
fun CommunityStatsScreen(navController: NavHostController) {
    val context = LocalContext.current
    var consent by remember { mutableStateOf(CommunityStats.consent(context)) }
    val payload = remember { CommunityStats.preview() }

    MaxScreen(
        title = stringResource(R.string.max_stats_title),
        subtitle = stringResource(R.string.max_stats_subtitle),
        onBack = { navController.popBackStack() },
        accentIcon = Icons.Rounded.DataUsage,
        accent = MaterialTheme.colorScheme.secondary,
    ) {
        MaxSection(
            title = stringResource(R.string.max_stats_collect_title),
            description = stringResource(R.string.max_stats_collect_desc),
        ) {
            MaxBullets(
                lines = listOf(
                    stringResource(R.string.max_stats_collect_country),
                    stringResource(R.string.max_stats_collect_version),
                    stringResource(R.string.max_stats_collect_device),
                    stringResource(R.string.max_stats_collect_android),
                    stringResource(R.string.max_stats_collect_env),
                    stringResource(R.string.max_stats_id_note),
                ),
            )
        }

        MaxSection(title = stringResource(R.string.max_stats_never_title)) {
            MaxBullets(
                lines = listOf(
                    stringResource(R.string.max_stats_never_identifiers),
                    stringResource(R.string.max_stats_never_content),
                    stringResource(R.string.max_stats_never_location),
                    stringResource(R.string.max_stats_ip_note),
                ),
            )
        }

        if (payload.isNotBlank()) {
            MaxSection(
                title = stringResource(R.string.max_stats_payload_title),
                description = stringResource(R.string.max_stats_payload_desc),
            ) {
                MaxGroup {
                    Text(
                        text = payload,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        MaxGroup {
            ExpressiveSwitchItem(
                icon = Icons.Rounded.DataUsage,
                title = stringResource(R.string.max_stats_switch_title),
                summary = stringResource(R.string.max_stats_switch_summary),
                checked = consent,
                onCheckedChange = { next ->
                    consent = next
                    CommunityStats.setConsent(context, next)
                },
            )
        }
    }
}
