/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root.
 *
 * تبويب الأنشطة (Activities) — التحكم الكامل في محفزات الجزيرة الـ 12.
 */
package nd.max.ui.subscreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import nd.max.R
import nd.max.island.core.IslandKind
import nd.max.island.core.IslandSettings
import nd.max.island.core.Trigger
import nd.max.ui.component.hint
import nd.max.ui.component.icon
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxNavigationRow
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxSwitchRow

/**
 * محتوى تبويب «الأنشطة»: التنبيهات، الشحن، البطارية، المكالمات، الوسائط، المؤقت، الملاحة، التتبع، التنزيلات.
 */
@Composable
internal fun ActivitiesTabContent(
    triggers: Set<String>,
    kindSheetAllSetter: (Boolean) -> Unit,
    scheme: ColorScheme
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.section)
    ) {
        /* ── ١. التنبيهات والأجهزة الأساسية ────────────────────────── */
        MaxSection(
            title = stringResource(R.string.ify_test_group_basic),
            description = stringResource(R.string.ify_act_footer)
        ) {
            MaxGroup {
                val basicTriggers = listOf(
                    Trigger.Notifications,
                    Trigger.Charging,
                    Trigger.Battery,
                    Trigger.Devices
                )
                basicTriggers.forEachIndexed { i, t ->
                    if (i > 0) MaxGroupDivider()
                    MaxSwitchRow(
                        title = stringResource(t.label),
                        subtitle = stringResource(t.hint()),
                        checked = t.key in triggers,
                        onCheckedChange = { IslandSettings.setTrigger(t, it) },
                        icon = t.icon()
                    )
                }
            }
        }

        /* ── ٢. المكالمات والموسيقى والمؤقت ────────────────────────── */
        MaxSection(title = stringResource(R.string.ify_kind_group_island)) {
            MaxGroup {
                val ongoingTriggers = listOf(
                    Trigger.Calls,
                    Trigger.Media,
                    Trigger.Timer
                )
                ongoingTriggers.forEachIndexed { i, t ->
                    if (i > 0) MaxGroupDivider()
                    MaxSwitchRow(
                        title = stringResource(t.label),
                        subtitle = stringResource(t.hint()),
                        checked = t.key in triggers,
                        onCheckedChange = { IslandSettings.setTrigger(t, it) },
                        icon = t.icon()
                    )
                }
            }
        }

        /* ── ٣. الأنشطة المباشرة والتتبع ───────────────────────────── */
        MaxSection(title = stringResource(R.string.ify_test_group_live)) {
            MaxGroup {
                val liveTriggers = listOf(
                    Trigger.Navigation,
                    Trigger.Tracking,
                    Trigger.Downloads
                )
                liveTriggers.forEachIndexed { i, t ->
                    if (i > 0) MaxGroupDivider()
                    MaxSwitchRow(
                        title = stringResource(t.label),
                        subtitle = stringResource(t.hint()),
                        checked = t.key in triggers,
                        onCheckedChange = { IslandSettings.setTrigger(t, it) },
                        icon = t.icon()
                    )
                }
            }
        }

        /* ── ٤. تبديلات النظام والهدوء ────────────────────────────── */
        MaxSection(title = stringResource(R.string.ify_trigger_toggles)) {
            MaxGroup {
                val systemTriggers = listOf(
                    Trigger.Toggles,
                    Trigger.Quiet
                )
                systemTriggers.forEachIndexed { i, t ->
                    if (i > 0) MaxGroupDivider()
                    MaxSwitchRow(
                        title = stringResource(t.label),
                        subtitle = stringResource(t.hint()),
                        checked = t.key in triggers,
                        onCheckedChange = { IslandSettings.setTrigger(t, it) },
                        icon = t.icon()
                    )
                }
            }
        }

        /* ── ٥. تخصيص مظهر النوافذ والأنشطة ────────────────────────── */
        MaxSection(
            title = stringResource(R.string.ify_cust_banner),
            description = stringResource(R.string.ify_cust_banner_sub)
        ) {
            MaxGroup {
                MaxNavigationRow(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.ify_banner_all),
                    subtitle = stringResource(R.string.ify_banner_all_sub),
                    onClick = { kindSheetAllSetter(true) }
                )
            }
        }
    }
}
