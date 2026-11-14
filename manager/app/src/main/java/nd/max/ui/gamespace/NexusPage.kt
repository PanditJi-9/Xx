/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.gamespace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.core.gamespace.GamePlayStats
import nd.max.core.gamespace.PlaySession
import nd.max.core.platform.HudLive
import nd.max.core.platform.SessionCause
import nd.max.core.platform.SessionVerdict
import nd.max.core.platform.StoredGameSession
import nd.max.core.platform.sessionInsight
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
    /** سجلّ الجلسات الغنيّ: منه وحده يُشتقّ حكم آخر جلسة. */
    gameSessions: List<StoredGameSession>,
    onSelect: (String) -> Unit,
    onLaunch: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenLoadout: () -> Unit,
    onOpenCareer: () -> Unit,
    onOpenSearch: () -> Unit,
    onClearFavorites: () -> Unit,
    onOpenLibrary: () -> Unit,
    /** يكمل الشرط الناقص مباشرةً (ملفّ · لوحة · تراكب). */
    onFixGap: (ReadyGap) -> Unit,
    chromeAccent: Color? = null,
    modifier: Modifier = Modifier
) {
    // اللقطة تُعاد حين يتغيّر السجلّ أو المكتبة أو المقابض؛ الوقت وحده لا يحرّكها.
    // أول فتح بلا اختيار محفوظ: الأولى هي البطلة، لا هيكل بلا أيقونة أو لون.
    val focus = selected ?: games.firstOrNull()
    val nowMs = remember(stats, sessions) { System.currentTimeMillis() }
    // حكم آخر جلسة لهذه اللعبة وحدها: الأساس لا يخلط لعبة ثقيلة بأخرى خفيفة.
    val history = remember(gameSessions, focus) {
        gameSessions.filter { it.packageName == focus?.packageName }.sortedByDescending { it.endedAtMs }
    }
    val lastInsight = remember(history, gameSessions) {
        history.firstOrNull()?.let { sessionInsight(it, gameSessions) }
    }
    // القراءة الحيّة من تدفّق HUD نفسه؛ والتقادم يُقاس عند كل إصدار لا بمؤقّت ثانٍ.
    val hud by HudLive.snapshot.collectAsState()
    val telemetry = remember(hud, focus) {
        nexusTelemetry(hud, focus?.packageName, System.currentTimeMillis())
    }
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
        recentLine = recentLine,
        lastSessionLine = lastInsight?.let { insight ->
            val verdict = stringResource(
                when (insight.verdict) {
                    SessionVerdict.Better -> R.string.game_sessions_verdict_better
                    SessionVerdict.Similar -> R.string.game_sessions_verdict_similar
                    SessionVerdict.Worse -> R.string.game_sessions_verdict_worse
                    SessionVerdict.FirstRun -> R.string.game_sessions_verdict_first
                    SessionVerdict.Unusable -> R.string.game_sessions_verdict_unusable
                }
            )
            val cause = insight.cause.takeIf { it != SessionCause.None }?.let { cause ->
                stringResource(
                    when (cause) {
                        SessionCause.Thermal -> R.string.session_cause_thermal
                        SessionCause.Jank -> R.string.session_cause_jank
                        SessionCause.Energy -> R.string.session_cause_energy
                        SessionCause.PowerSave -> R.string.session_cause_power_save
                        SessionCause.Refresh -> R.string.session_cause_refresh
                        SessionCause.Renderer -> R.string.session_cause_renderer
                        SessionCause.None -> R.string.session_cause_thermal
                    }
                )
            }
            listOfNotNull(verdict, cause).joinToString(" · ")
        },
        lastSessionTone = when (lastInsight?.verdict) {
            SessionVerdict.Better -> Monolith.Mint
            SessionVerdict.Worse, SessionVerdict.Unusable -> Monolith.Amber
            SessionVerdict.FirstRun -> Monolith.Ice
            SessionVerdict.Similar, null -> Monolith.Muted
        },
        gap = snapshot.gap,
        telemetry = telemetry
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
            ),
            fixNow = stringResource(R.string.gs_fix_now)
        ),
        onSelect = { index -> entries.getOrNull(index)?.let { onSelect(it.packageName) } },
        onLaunch = onLaunch,
        onOpenVault = onOpenVault,
        onOpenLoadout = onOpenLoadout,
        onOpenCareer = onOpenCareer,
        onOpenSearch = onOpenSearch,
        onEmptyAction = if (favoritesOnly) onClearFavorites else onOpenLibrary,
        onFixGap = onFixGap,
        chromeAccent = chromeAccent,
        modifier = modifier
    )
}
