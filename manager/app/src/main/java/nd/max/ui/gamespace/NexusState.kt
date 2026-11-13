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
 * الشرط الأول الناقص في الجاهزية. الرقم وحده يقول «٢/٣» ولا يقول أيّها؛ وهذا يقول.
 * الترتيب مقصود: الملف أولًا لأنه الأثرى، ثم اللوحة، ثم الإذن الذي يحتاج شاشة نظام.
 */
enum class ReadyGap { Profile, Panel, Overlay }

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
    /** الشرط الأول الناقص، أو `null` إن كانت التهيئة كاملة. */
    val gap: ReadyGap? = null,
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
            readiness(selected, armed, overlayGranted, tweaks).count { it }
        }
        return NexusSnapshot(
            totalMs = played.sumOf { it.totalMs },
            sessionCount = played.sumOf { it.sessionCount },
            gamesPlayed = played.size,
            streakDays = streak(sessions, library, nowMs, zone),
            readyDone = readyDone,
            readyTotal = NexusSnapshot.READY_TOTAL,
            recent = played.maxByOrNull { it.lastPlayedAtMs },
            gap = firstGap(selected, armed, overlayGranted, tweaks),
        )
    }

    private fun readiness(
        selected: GameApp,
        armed: Set<String>,
        overlayGranted: Boolean,
        tweaks: Map<String, Int>,
    ): List<Boolean> = listOf(
        (tweaks[selected.packageName] ?: 0) > 0,
        selected.packageName in armed,
        overlayGranted,
    )

    /**
     * الشرط الأول الناقص، بالترتيب نفسه الذي تُعدّ به الحلقة. الشرط والعدد من دالة واحدة،
     * فلا يمكن أن يقول الرقم «٣/٣» بينما السطر يشتكي من نقص.
     */
    fun firstGap(
        selected: GameApp?,
        armed: Set<String>,
        overlayGranted: Boolean,
        tweaks: Map<String, Int>,
    ): ReadyGap? {
        if (selected == null) return null
        val done = readiness(selected, armed, overlayGranted, tweaks)
        return when {
            !done[0] -> ReadyGap.Profile
            !done[1] -> ReadyGap.Panel
            !done[2] -> ReadyGap.Overlay
            else -> null
        }
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
