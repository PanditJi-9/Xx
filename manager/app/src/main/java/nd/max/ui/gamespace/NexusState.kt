/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.gamespace

import nd.max.core.gamespace.CareerLedger
import nd.max.core.gamespace.GameApp
import nd.max.core.gamespace.GamePlayStats
import nd.max.core.gamespace.PlaySession
import java.time.ZoneId

/**
 * لقطة Nexus: كل رقم فيها مشتقٌّ من سجلّ الجلسات والمكتبة القائمين، ولا مخزن ثالث.
 *
 * `readyTotal` ثلاثة مقابض يملكها المستخدم فعلًا: ملفّ اللعبة، ولوحة الجانب، وصلاحية التراكب.
 * و`streakDays` أيام محليّة متتالية فيها جلسة مرصودة، تنتهي اليوم أو أمس؛ وما انقطع لا يُمدّ.
 */
data class NexusSnapshot(
    val totalMs: Long = 0L,
    val sessionCount: Int = 0,
    val gamesPlayed: Int = 0,
    val streakDays: Int = 0,
    val readyDone: Int = 0,
    val readyTotal: Int = READY_TOTAL,
    val recent: GamePlayStats? = null,
) {
    companion object {
        const val READY_TOTAL = 3
    }
}

/** اشتقاق Nexus — نقيّ بالكامل: لا أندرويد ولا وقت حقيقي، فيُقاس على JVM. */
object NexusState {

    fun snapshot(
        games: List<GameApp>,
        stats: Map<String, GamePlayStats>,
        sessions: List<PlaySession>,
        armed: Set<String>,
        overlayGranted: Boolean,
        tweaks: Map<String, Int>,
        selected: GameApp?,
        nowMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): NexusSnapshot {
        val library = games.mapTo(HashSet()) { it.packageName }
        val played = games
            .mapNotNull { stats[it.packageName] }
            .filter { it.sessionCount > 0 && it.totalMs > 0L }
        val readyDone = if (selected == null) {
            0
        } else {
            listOf(
                (tweaks[selected.packageName] ?: 0) > 0,
                selected.packageName in armed,
                overlayGranted,
            ).count { it }
        }
        return NexusSnapshot(
            totalMs = played.sumOf { it.totalMs },
            sessionCount = played.sumOf { it.sessionCount },
            gamesPlayed = played.size,
            streakDays = streak(sessions, library, nowMs, zone),
            readyDone = readyDone,
            readyTotal = NexusSnapshot.READY_TOTAL,
            recent = played.maxByOrNull { it.lastPlayedAtMs },
        )
    }

    /**
     * أيام متتالية محليّة. تُحسب من اليوم إلى الخلف إن كان فيه جلسة، أو من أمس إن كان اليوم
     * ما زال فارغًا (السلسلة حيّة حتى نهاية اليوم). المستقبل لا يُحتسب، والانقطاع يصفّرها.
     */
    fun streak(
        sessions: List<PlaySession>,
        library: Set<String>,
        nowMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Int = CareerLedger.streak(sessions, library, nowMs, zone)
}
