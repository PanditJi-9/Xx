/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewSidebar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.core.gamespace.GamePlayStats
import nd.max.core.gamespace.LobbyInsights
import nd.max.core.gamespace.PlayAge
import nd.max.core.gamespace.PlaySessionLedger
import nd.max.core.gamespace.SetupKind
import nd.max.core.gamespace.SetupStep
import nd.max.ui.component.AppIconImage
import nd.max.ui.component.LobbyPalette
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius

/**
 * «الرؤى»: مسيرة المكتبة وتهيئة اللعبة الحالية — كلها مشتقّة من `PlaySessionLedger` و`AppConfig`
 * و`GamePanelPrefs` القائمة. لا مخزن ثانٍ ولا رقم مُختلق: ما لم يُرصد يُقال «لا جلسة بعد».
 *
 * البنود الأربعة قابلة للفعل: الملف يفتح قسمه، واللوحة تُقلَب من هنا، والسجل يفتح الشاشة؛
 * و«آخر أسبوع» معلومة لا زرّ لها.
 */
@Composable
internal fun LobbyInsightsBody(
    app: GameApp?,
    games: List<GameApp>,
    stats: Map<String, GamePlayStats>,
    tweaks: Map<String, Int>,
    armed: Set<String>,
    accent: Color,
    onSection: (LobbySection) -> Unit,
    onPanelEnabled: (Boolean) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenGame: (String) -> Unit,
) {
    val nowMs = remember { System.currentTimeMillis() }
    val library = remember(games) { games.map { it.packageName }.toSet() }
    // حزمة أُزيلت من المكتبة قد تبقى في تفضيلات اللوحة؛ العدّاد يحسب ما يُعرض هنا فقط.
    val libraryArmed = remember(armed, library) { armed intersect library }
    val summary = remember(stats, library) { LobbyInsights.summary(stats, library) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaxSpace.xl, vertical = MaxSpace.lg),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.lg)
    ) {
        Text(
            text = stringResource(R.string.lobby_insights_head),
            color = LobbyPalette.Ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        LobbySectionCard(title = stringResource(R.string.lobby_insights_career)) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
                InsightTile(
                    label = stringResource(R.string.lobby_insights_total_time),
                    value = lobbyPlayTimeText(summary.totalMs)
                        ?: stringResource(R.string.lobby_insights_unknown),
                    modifier = Modifier.weight(1f)
                )
                InsightTile(
                    label = stringResource(R.string.lobby_insights_sessions),
                    value = summary.sessionCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
                InsightTile(
                    label = stringResource(R.string.lobby_insights_games_played),
                    value = summary.gamesPlayed.toString(),
                    modifier = Modifier.weight(1f)
                )
                InsightTile(
                    label = stringResource(R.string.lobby_insights_never_played),
                    value = summary.neverPlayed.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = if (libraryArmed.isEmpty()) {
                    stringResource(R.string.lobby_insights_armed_none)
                } else {
                    stringResource(R.string.lobby_insights_armed_count, libraryArmed.size)
                },
                color = LobbyPalette.Muted,
                fontSize = 12.sp
            )
        }

        LobbySectionCard(title = stringResource(R.string.lobby_insights_most_played)) {
            if (summary.top.isEmpty()) {
                Text(
                    text = stringResource(R.string.lobby_insights_no_stats),
                    color = LobbyPalette.Muted,
                    fontSize = 13.sp
                )
            } else {
                summary.top.forEach { stat ->
                    val ranked = games.firstOrNull { it.packageName == stat.packageName }
                    MostPlayedRow(
                        label = ranked?.label ?: stat.packageName,
                        packageName = stat.packageName,
                        value = lobbyPlayTimeText(stat.totalMs)
                            ?: stringResource(R.string.lobby_insights_unknown),
                        accent = accent,
                        onClick = if (ranked != null) {
                            { onOpenGame(stat.packageName) }
                        } else {
                            null
                        }
                    )
                }
            }
        }

        LobbySectionCard(
            title = if (app == null) {
                stringResource(R.string.lobby_insights_setup_none)
            } else {
                stringResource(R.string.lobby_insights_setup_title, app.label)
            }
        ) {
            if (app == null) {
                Text(
                    text = stringResource(R.string.lobby_insights_pick_game),
                    color = LobbyPalette.Muted,
                    fontSize = 13.sp
                )
            } else {
                val tweakCount = tweaks[app.packageName] ?: 0
                val panelArmed = app.packageName in libraryArmed
                val steps = remember(stats, app.packageName, tweakCount, panelArmed, nowMs) {
                    LobbyInsights.setup(
                        stats = stats[app.packageName],
                        tweaks = tweakCount,
                        panelArmed = panelArmed,
                        nowMs = nowMs
                    )
                }
                Text(
                    text = stringResource(
                        R.string.lobby_explore_ready,
                        LobbyInsights.readyCount(steps)
                    ),
                    color = LobbyPalette.Muted,
                    fontSize = 12.sp
                )
                steps.forEach { step ->
                    SetupRow(
                        step = step,
                        tweakCount = tweakCount,
                        stats = stats[app.packageName],
                        onCreate = {
                            when (step.kind) {
                                SetupKind.Profile -> onSection(LobbySection.Game)
                                SetupKind.Panel -> onPanelEnabled(!panelArmed)
                                SetupKind.History -> onOpenHistory()
                                SetupKind.Recent -> Unit
                            }
                        }
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.lobby_insights_scope),
            color = LobbyPalette.Muted,
            fontSize = 12.sp
        )
    }
}

/**
 * سطر سجلّ اللعب للعبة واحدة: «آخر لعب … · لُعبت …» من الجلسات المحفوظة محليًّا. غياب الجلسة
 * يُعرض غيابًا لا صفرًا (ADR-07)، ولا يُدَّعى وقتٌ لم يُسجَّل.
 *
 * مصدر واحد للمعنى نفسه: المسرح وبطاقة «استكشف» يقرآن هذا السطر، فلا يظهر الرقم بصيغتين.
 */
@Composable
internal fun lobbyPlayLine(stats: GamePlayStats?): String? {
    if (stats == null) return null
    val age = PlaySessionLedger.playAge(System.currentTimeMillis(), stats.lastPlayedAtMs)
    val lastPlayed = when (age) {
        PlayAge.None -> null
        is PlayAge.Minutes -> stringResource(R.string.lobby_stats_last_minutes, age.minutes)
        is PlayAge.Hours -> stringResource(R.string.lobby_stats_last_hours, age.hours)
        PlayAge.Yesterday -> stringResource(R.string.lobby_stats_last_yesterday)
        is PlayAge.Days -> stringResource(R.string.lobby_stats_last_days, age.days)
    }
    val played = lobbyPlayTimeText(stats.totalMs)
    return listOfNotNull(lastPlayed, played).takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/** الدقائق/الساعات من المدة نفسها التي تعرضها بطاقة المسرح؛ `null` لما دون خمس ثوانٍ. */
@Composable
internal fun lobbyPlayTimeText(totalMs: Long): String? {
    val minutes = PlaySessionLedger.playMinutes(totalMs) ?: return null
    return if (minutes < 60) {
        stringResource(R.string.lobby_stats_play_minutes, minutes)
    } else {
        stringResource(R.string.lobby_stats_play_hours, minutes / 60, minutes % 60)
    }
}

@Composable
private fun InsightTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(maxRadius(MaxRadius.tile)))
            .background(LobbyPalette.PanelRaised.copy(alpha = 0.45f))
            .padding(MaxSpace.lg),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Text(text = label, color = LobbyPalette.Muted, fontSize = 11.sp, maxLines = 1)
        Text(
            text = value,
            color = LobbyPalette.Ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun MostPlayedRow(
    label: String,
    packageName: String,
    value: String,
    accent: Color,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaxSize.minTouchTarget)
            .clip(RoundedCornerShape(maxRadius(MaxRadius.chip)))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = MaxSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        AppIconImage(
            packageName = packageName,
            size = MaxSize.rowIconContainer,
            contentDescription = null
        )
        Text(
            text = label,
            color = LobbyPalette.Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, color = accent, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun SetupRow(
    step: SetupStep,
    tweakCount: Int,
    stats: GamePlayStats?,
    onCreate: () -> Unit,
) {
    val icon: ImageVector = when (step.kind) {
        SetupKind.Profile -> Icons.Rounded.Tune
        SetupKind.Panel -> Icons.Rounded.ViewSidebar
        SetupKind.History -> Icons.Rounded.History
        SetupKind.Recent -> Icons.Rounded.Schedule
    }
    val title = when (step.kind) {
        SetupKind.Profile -> stringResource(R.string.lobby_insights_step_profile)
        SetupKind.Panel -> stringResource(R.string.lobby_insights_step_panel)
        SetupKind.History -> stringResource(R.string.lobby_insights_step_history)
        SetupKind.Recent -> stringResource(R.string.lobby_insights_step_recent)
    }
    val detail = when (step.kind) {
        SetupKind.Profile -> if (tweakCount > 0) {
            stringResource(R.string.lobby_tile_profile_value, tweakCount)
        } else {
            stringResource(R.string.lobby_tile_profile_none)
        }
        SetupKind.Panel -> if (step.done) {
            stringResource(R.string.lobby_state_on)
        } else {
            stringResource(R.string.lobby_state_off)
        }
        SetupKind.History -> {
            val count = stats?.sessionCount ?: 0
            if (count > 0) {
                stringResource(R.string.lobby_insights_sessions_count, count)
            } else {
                stringResource(R.string.lobby_insights_no_sessions)
            }
        }
        SetupKind.Recent -> if (step.done) {
            stringResource(R.string.lobby_insights_recent_done)
        } else {
            stringResource(R.string.lobby_insights_recent_todo)
        }
    }
    val actionable = step.kind != SetupKind.Recent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaxSize.minTouchTarget)
            .clip(RoundedCornerShape(maxRadius(MaxRadius.row)))
            .then(if (actionable) Modifier.clickable(onClick = onCreate) else Modifier)
            .padding(horizontal = MaxSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (step.done) LobbyPalette.Positive else LobbyPalette.Muted
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = LobbyPalette.Ink, fontSize = 14.sp, maxLines = 1)
            Text(text = detail, color = LobbyPalette.Muted, fontSize = 12.sp, maxLines = 1)
        }
        Icon(
            imageVector = if (step.done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (step.done) LobbyPalette.Positive else LobbyPalette.Muted
        )
    }
}
