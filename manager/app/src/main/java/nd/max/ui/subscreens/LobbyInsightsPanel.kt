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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import nd.max.R
import nd.max.core.gamespace.CareerBadge
import nd.max.core.gamespace.CareerLedger
import nd.max.core.gamespace.CareerStats
import nd.max.core.gamespace.GameApp
import nd.max.core.gamespace.GamePlayStats
import nd.max.core.gamespace.LobbyInsights
import nd.max.core.gamespace.PlaySession
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
    sessions: List<PlaySession>,
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
    val career = remember(stats, sessions, library, nowMs) {
        CareerLedger.build(stats, sessions, library, nowMs)
    }
    // النافذة الطويلة تُحسب من السجلّ نفسه؛ فإن بلغ السجلّ سقفه قال البطاقة من أين يبدأ.
    val span = remember(sessions, library, nowMs) {
        CareerLedger.span(sessions, library, nowMs)
    }
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

        LobbySectionCard(title = stringResource(R.string.lobby_career_week)) {
            Text(
                text = stringResource(R.string.lobby_career_week_note),
                color = LobbyPalette.Muted,
                fontSize = 12.sp
            )
            val weekMs = career.week.sumOf { it.totalMs }
            if (weekMs > 0L || career.lastWeekMs > 0L) {
                val delta = when {
                    career.lastWeekMs <= 0L ->
                        stringResource(R.string.lobby_career_delta_new) to LobbyPalette.Positive
                    else -> {
                        val pct = ((weekMs - career.lastWeekMs) * 100L / career.lastWeekMs).toInt()
                        when {
                            pct > 0 -> stringResource(R.string.lobby_career_delta_up, pct) to
                                LobbyPalette.Positive
                            pct < 0 -> stringResource(R.string.lobby_career_delta_down, -pct) to
                                LobbyPalette.Muted
                            else -> stringResource(R.string.lobby_career_delta_same) to
                                LobbyPalette.Muted
                        }
                    }
                }
                Text(
                    text = delta.first,
                    color = delta.second,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            val peak = career.week.maxOfOrNull { it.totalMs } ?: 0L
            val today = career.week.lastOrNull()?.date
            val labels = career.week.map { day ->
                stringResource(
                    R.string.lobby_career_bar,
                    day.date.dayOfMonth,
                    lobbyPlayTimeText(day.totalMs)
                        ?: stringResource(R.string.lobby_insights_unknown)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clearAndSetSemantics { contentDescription = labels.joinToString(", ") },
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs),
                verticalAlignment = Alignment.Bottom
            ) {
                career.week.forEach { day ->
                    val ratio = if (peak > 0L) {
                        (day.totalMs.toFloat() / peak).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .clip(RoundedCornerShape(maxRadius(MaxRadius.chip)))
                                .background(LobbyPalette.PanelRaised.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (ratio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(ratio)
                                        .background(
                                            if (day.date == today) {
                                                accent
                                            } else {
                                                LobbyPalette.Muted.copy(alpha = 0.55f)
                                            }
                                        )
                                )
                            }
                        }
                        Spacer(Modifier.height(MaxSpace.xs))
                        Text(
                            text = day.date.dayOfMonth.toString(),
                            color = if (day.date == today) LobbyPalette.Ink else LobbyPalette.Muted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            val bestLabel = career.bestSessionPackage?.let { pkg ->
                games.firstOrNull { it.packageName == pkg }?.label ?: pkg
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
                InsightTile(
                    label = stringResource(R.string.lobby_career_best),
                    value = if (career.bestSessionMs > 0L && bestLabel != null) {
                        stringResource(
                            R.string.lobby_career_best_value,
                            lobbyPlayTimeText(career.bestSessionMs)
                                ?: stringResource(R.string.lobby_insights_unknown),
                            bestLabel
                        )
                    } else {
                        stringResource(R.string.lobby_insights_no_sessions)
                    },
                    modifier = Modifier.weight(1f)
                )
                InsightTile(
                    label = stringResource(R.string.lobby_career_night),
                    value = if (career.nightSessions > 0) {
                        stringResource(R.string.lobby_insights_sessions_count, career.nightSessions)
                    } else {
                        stringResource(R.string.lobby_insights_no_sessions)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LobbySectionCard(title = stringResource(R.string.lobby_span_title)) {
            Text(
                text = stringResource(R.string.lobby_span_note, span.days),
                color = LobbyPalette.Muted,
                fontSize = 12.sp
            )
            if (span.totalMs <= 0L) {
                Text(
                    text = stringResource(R.string.lobby_insights_no_sessions),
                    color = LobbyPalette.Muted,
                    fontSize = 13.sp
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
                    InsightTile(
                        label = stringResource(R.string.lobby_span_active),
                        value = stringResource(
                            R.string.lobby_span_active_value,
                            span.activeDays,
                            span.days
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    InsightTile(
                        label = stringResource(R.string.lobby_span_average),
                        value = lobbyPlayTimeText(span.averageActiveDayMs)
                            ?: stringResource(R.string.lobby_insights_unknown),
                        modifier = Modifier.weight(1f)
                    )
                }
                val locale = LocalConfiguration.current.locales[0]
                val dayFormat = remember(locale) {
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
                }
                InsightTile(
                    label = stringResource(R.string.lobby_span_busiest),
                    value = span.busiest?.let { day ->
                        stringResource(
                            R.string.lobby_span_busiest_value,
                            dayFormat.format(day.date),
                            lobbyPlayTimeText(day.totalMs)
                                ?: stringResource(R.string.lobby_insights_unknown)
                        )
                    } ?: stringResource(R.string.lobby_insights_unknown),
                    modifier = Modifier.fillMaxWidth()
                )
                // السجلّ يحتفظ بآخر ٤٠٠ جلسة؛ فإن لم يغطِّ النافذة كلها يُقال من أين يبدأ لا أكثر.
                span.oldestAtMs?.let { oldest ->
                    Text(
                        text = stringResource(
                            R.string.lobby_span_oldest,
                            dayFormat.format(
                                Instant.ofEpochMilli(oldest)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            )
                        ),
                        color = LobbyPalette.Muted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        LobbySectionCard(title = stringResource(R.string.lobby_career_sessions)) {
            Text(
                text = stringResource(R.string.lobby_career_sessions_note),
                color = LobbyPalette.Muted,
                fontSize = 12.sp
            )
            if (career.recent.isEmpty()) {
                Text(
                    text = stringResource(R.string.lobby_insights_no_sessions),
                    color = LobbyPalette.Muted,
                    fontSize = 13.sp
                )
            } else {
                val locale = LocalConfiguration.current.locales[0]
                val time = remember(locale) {
                    DateTimeFormatter
                        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                        .withLocale(locale)
                        .withZone(ZoneId.systemDefault())
                }
                career.recent.forEach { session ->
                    val label = games.firstOrNull { it.packageName == session.packageName }?.label
                        ?: session.packageName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = MaxSize.minTouchTarget)
                            .semantics(mergeDescendants = true) { },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                color = LobbyPalette.Ink,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = time.format(Instant.ofEpochMilli(session.endedAtMs)),
                                color = LobbyPalette.Muted,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = lobbyPlayTimeText(session.durationMs)
                                ?: stringResource(R.string.lobby_insights_unknown),
                            color = accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
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

        LobbySectionCard(title = stringResource(R.string.lobby_career_milestones)) {
            career.milestones.forEach { milestone ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MaxSize.minTouchTarget)
                        .semantics(mergeDescendants = true) { }
                        .padding(vertical = MaxSpace.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
                ) {
                    Icon(
                        imageVector = if (milestone.unlocked) {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.RadioButtonUnchecked
                        },
                        contentDescription = null,
                        tint = if (milestone.unlocked) accent else LobbyPalette.Muted,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = careerBadgeLabel(milestone.badge),
                        color = LobbyPalette.Ink,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (milestone.unlocked) {
                            stringResource(R.string.lobby_career_unlocked)
                        } else if (milestone.badge == CareerBadge.Marathon) {
                            stringResource(
                                R.string.lobby_career_progress_min,
                                milestone.progress,
                                milestone.target
                            )
                        } else {
                            stringResource(
                                R.string.lobby_career_progress,
                                milestone.progress,
                                milestone.target
                            )
                        },
                        color = if (milestone.unlocked) accent else LobbyPalette.Muted,
                        fontSize = 12.sp
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
private fun careerBadgeLabel(badge: CareerBadge): String = stringResource(
    when (badge) {
        CareerBadge.FirstSession -> R.string.lobby_career_badge_first
        CareerBadge.TenSessions -> R.string.lobby_career_badge_ten
        CareerBadge.Marathon -> R.string.lobby_career_badge_marathon
        CareerBadge.WeekStreak -> R.string.lobby_career_badge_streak
        CareerBadge.FiveGames -> R.string.lobby_career_badge_games
    }
)

/** عمر آخر جلسة كنصّ مترجم؛ `null` يعني «لا جلسة مسجّلة» فلا تُعرض «الآن». */
@Composable
internal fun lobbyPlayAgeText(stats: GamePlayStats?): String? {
    if (stats == null) return null
    return when (val age = PlaySessionLedger.playAge(System.currentTimeMillis(), stats.lastPlayedAtMs)) {
        PlayAge.None -> null
        is PlayAge.Minutes -> stringResource(R.string.lobby_stats_last_minutes, age.minutes)
        is PlayAge.Hours -> stringResource(R.string.lobby_stats_last_hours, age.hours)
        PlayAge.Yesterday -> stringResource(R.string.lobby_stats_last_yesterday)
        is PlayAge.Days -> stringResource(R.string.lobby_stats_last_days, age.days)
    }
}

@Composable
internal fun lobbyPlayLine(stats: GamePlayStats?): String? {
    if (stats == null) return null
    val lastPlayed = lobbyPlayAgeText(stats)
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
                    // صفّ واحد في القارئ: الاسم والوقت يُقرآن معًا، والإجراء معلن زرًّا صريحًا.
                    Modifier
                        .semantics(mergeDescendants = true) { }
                        .clickable(role = Role.Button, onClick = onClick)
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
            .then(
                if (actionable) {
                    // بند التهيئة عقدة واحدة: العنوان وحالته يُسمعان معًا، والإجراء زرّ معلن.
                    Modifier
                        .semantics(mergeDescendants = true) { }
                        .clickable(role = Role.Button, onClick = onCreate)
                } else {
                    Modifier
                }
            )
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
