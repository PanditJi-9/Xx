/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.gamespace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.core.gamespace.GamePlayStats
import nd.max.core.gamespace.PlaySession
import nd.max.ui.component.rememberLobbyIcon
import nd.max.ui.component.rememberLobbyTone
import nd.max.ui.subscreens.lobbyPlayAgeText
import nd.max.ui.subscreens.lobbyPlayTimeText
import nd.max.ui.subscreens.rememberLobbyInstalledAge

/**
 * يربط Nexus بالمصادر القائمة وحدها: المكتبة، سجلّ الجلسات، ملفّات `AppConfig`،
 * وحالة اللوحة الجانبية وصلاحية التراكب. لا يقرأ ولا يكتب شيئًا بنفسه.
 */
@Composable
internal fun NexusPage(
    games: List<GameApp>,
    selected: GameApp?,
    stats: Map<String, GamePlayStats>,
    sessions: List<PlaySession>,
    tweaks: Map<String, Int>,
    armed: Set<String>,
    overlayGranted: Boolean,
    favoritesOnly: Boolean,
    onSelect: (String) -> Unit,
    onLaunch: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenLoadout: () -> Unit,
    onOpenCareer: () -> Unit,
    onOpenSearch: () -> Unit,
    onClearFavorites: () -> Unit,
    onOpenLibrary: () -> Unit,
    chromeAccent: Color? = null,
    modifier: Modifier = Modifier
) {
    // اللقطة تُعاد حين يتغيّر السجلّ أو المكتبة أو المقابض؛ الوقت وحده لا يحرّكها.
    // أول فتح بلا اختيار محفوظ: الأولى هي البطلة، لا هيكل بلا أيقونة أو لون.
    val focus = selected ?: games.firstOrNull()
    val nowMs = remember(stats, sessions) { System.currentTimeMillis() }
    val snapshot = remember(games, stats, sessions, armed, overlayGranted, tweaks, focus, nowMs) {
        NexusState.snapshot(
            games = games,
            stats = stats,
            sessions = sessions,
            armed = armed,
            overlayGranted = overlayGranted,
            tweaks = tweaks,
            selected = focus,
            nowMs = nowMs
        )
    }

    val accent = rememberLobbyTone(focus?.packageName)
    val icon by rememberLobbyIcon(focus?.packageName)
    val selectedStats = focus?.let { stats[it.packageName] }
    val recentLine = lobbyPlayAgeText(selectedStats)
    val installedAge = if (focus != null) rememberLobbyInstalledAge(focus.packageName) else null
    val unknown = stringResource(R.string.lobby_insights_unknown)

    val entries = games.map { game ->
        val isSelected = game.packageName == focus?.packageName
        NexusGame(
            packageName = game.packageName,
            label = game.label,
            accent = if (isSelected) accent else Monolith.Faint,
            icon = if (isSelected) icon else null,
            lastPlayed = if (isSelected) recentLine else null,
            installedDays = if (isSelected) installedAge else null,
            monogram = game.label.take(1).uppercase()
        )
    }
    val selectedIndex = entries.indexOfFirst { it.packageName == focus?.packageName }
    val model = NexusModel(
        games = entries,
        selectedIndex = selectedIndex.coerceAtLeast(0),
        totalPlaytime = lobbyPlayTimeText(snapshot.totalMs) ?: unknown,
        sessionCount = if (snapshot.sessionCount > 0) snapshot.sessionCount.toString() else unknown,
        streak = if (snapshot.streakDays > 0) snapshot.streakDays.toString() else unknown,
        readiness = if (snapshot.readyTotal > 0) {
            snapshot.readyDone.toFloat() / snapshot.readyTotal
        } else {
            0f
        },
        readinessValue = "${snapshot.readyDone}/${snapshot.readyTotal}",
        readinessGap = snapshot.gap?.let { gap ->
            stringResource(
                when (gap) {
                    ReadyGap.Profile -> R.string.lobby_nexus_gap_profile
                    ReadyGap.Panel -> R.string.lobby_nexus_gap_panel
                    ReadyGap.Overlay -> R.string.lobby_nexus_gap_overlay
                }
            )
        },
        recentLine = recentLine
    )

    NexusScreen(
        model = model,
        copy = NexusCopy(
            wordmark = stringResource(R.string.game_space_title),
            nowLabel = stringResource(R.string.lobby_nexus_now),
            launch = stringResource(R.string.lobby_start),
            vault = stringResource(R.string.lobby_cc_library),
            loadout = stringResource(R.string.lobby_cc_loadout),
            career = stringResource(R.string.lobby_insights_career),
            search = stringResource(R.string.lobby_nexus_search),
            playtimeLabel = stringResource(R.string.lobby_insights_total_time),
            gamesLabel = stringResource(R.string.lobby_nexus_games),
            sessionsLabel = stringResource(R.string.lobby_insights_sessions),
            streakLabel = stringResource(R.string.lobby_nexus_streak),
            readyLabel = stringResource(R.string.lobby_nexus_ready),
            reviewSetup = stringResource(R.string.lobby_nexus_review),
            lastPlayedLabel = stringResource(R.string.lobby_nexus_last_played),
            installedLabel = stringResource(R.string.lobby_nexus_installed),
            emptyTitle = stringResource(R.string.lobby_nexus_empty_title),
            emptyBody = stringResource(
                if (favoritesOnly) R.string.lobby_empty_favorites else R.string.lobby_empty
            ),
            emptyAction = stringResource(
                if (favoritesOnly) R.string.lobby_vault_all else R.string.lobby_manage_title
            )
        ),
        onSelect = { index -> entries.getOrNull(index)?.let { onSelect(it.packageName) } },
        onLaunch = onLaunch,
        onOpenVault = onOpenVault,
        onOpenLoadout = onOpenLoadout,
        onOpenCareer = onOpenCareer,
        onOpenSearch = onOpenSearch,
        onEmptyAction = if (favoritesOnly) onClearFavorites else onOpenLibrary,
        chromeAccent = chromeAccent,
        modifier = modifier
    )
}
