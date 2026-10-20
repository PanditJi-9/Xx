/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.mainscreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import nd.max.R
import nd.max.core.persist.PersistRestorer
import nd.max.core.persist.PersistStore
import nd.max.ui.component.ExpressiveList
import nd.max.ui.component.ExpressiveListItem
import nd.max.ui.component.ExpressiveSwitchItem
import nd.max.ui.component.LeadingIcon
import nd.max.ui.design.MaxSpace

/**
 * قسم «حفظ خياراتك وتطبيقها بعد إعادة التشغيل» في Settings.
 *
 * ### ثلاث طبقات، ولا سجلّ
 *
 * 1. **المفتاح المركزي** — مفعَّل افتراضيًّا، ومعه سطرٌ واحد يشرح ما يفعله فعلًا.
 * 2. **الخطير التلقائي** — مفتاح ثانٍ يخصّ zram والذاكرة/UFS ومقاس الشاشة واللمس. يُغلق
 *    **تلقائيًّا** حين يرصد [nd.max.core.persist.BootLoopGuard] إقلاعين سريعين متتاليين، ويُعرض
 *    السبب تحته. وإعادة فتحه قرارٌ صريح من صاحب الجهاز.
 * 3. **«استعادة الآن»** — نفس الجولة بلا انتظار إقلاع، وتتجاوز حارس الخطر لأنها طلبٌ صريح.
 *
 * و«آخر استعادة» **أُزيل بأمر المالك**: القسم يقول ما يفعله، ودليلُه سطرٌ واحد في
 * `MaxManager.log` (`EVENT=PERSIST_RESTORE`) لا جدولٌ في الشاشة.
 */
@Composable
fun PersistRestoreItem() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(PersistStore.enabled(context)) }
    var busy by remember { mutableStateOf(false) }
    var dangerousAuto by remember { mutableStateOf(PersistStore.dangerousAutoEnabled(context)) }
    var dangerousReason by remember { mutableStateOf(PersistStore.dangerousAutoOffReason(context)) }

    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
        ExpressiveList(
            content = listOf(
                {
                    ExpressiveSwitchItem(
                        icon = Icons.Filled.Restore,
                        title = stringResource(R.string.persist_enable_title),
                        summary = stringResource(R.string.persist_enable_desc),
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            PersistStore.setEnabled(context, it)
                        }
                    )
                },
                {
                    ExpressiveSwitchItem(
                        icon = Icons.Filled.Warning,
                        title = stringResource(R.string.persist_dangerous_title),
                        summary = stringResource(R.string.persist_dangerous_desc),
                        checked = dangerousAuto,
                        enabled = enabled,
                        onCheckedChange = {
                            dangerousAuto = it
                            PersistStore.setDangerousAutoEnabled(context, it)
                            dangerousReason = PersistStore.dangerousAutoOffReason(context)
                        }
                    )
                }
            )
        )

        if (!dangerousAuto && dangerousReason == PersistStore.REASON_BOOTLOOP) {
            Text(
                text = stringResource(R.string.persist_dangerous_bootloop),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
            )
        }

        Text(
            text = stringResource(R.string.persist_coverage),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
        )

        ExpressiveList(
            content = listOf(
                {
                    ExpressiveListItem(
                        // الزرّ يُعطَّل بمفتاح الميزة نفسه: من أطفأها لا يُدنَدن له زرّها،
                        // والخدمة ترفض الجولة أصلًا إن نُوديت — فالواجهة تقول ما يقوله المنفّذ.
                        onClick = if (enabled && !busy) {
                            {
                                busy = true
                                scope.launch {
                                    PersistRestorer.restoreNow(context)
                                    busy = false
                                }
                            }
                        } else {
                            null
                        },
                        headlineContent = { Text(stringResource(R.string.persist_apply_now)) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    if (busy) R.string.persist_apply_running else R.string.persist_apply_now_desc
                                )
                            )
                        },
                        leadingContent = { LeadingIcon(icon = Icons.Filled.Refresh, contentDescription = null) }
                    )
                }
            )
        )

        Text(
            text = stringResource(R.string.persist_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
        )
    }
}
