package com.maxmanager.control.data

data class GameSessionBenchmark(
    val sessionId: String,
    val packageName: String,
    val profileLabel: String,
    val durationSec: Long,
    val avgWatts: Double,
    val peakWatts: Double,
    val minFreeRamMb: Int,
    val avgJitterMs: Double,
    val bypassDutyCyclePct: Double
)

data class SessionComparisonDiff(
    val baselineId: String,
    val candidateId: String,
    val wattsDelta: Double,
    val jitterDeltaMs: Double,
    val minRamDeltaMb: Int,
    val summaryVerdict: String
)

class SessionBenchmarkStore {
    private val sessions = mutableListOf<GameSessionBenchmark>()

    fun record(session: GameSessionBenchmark) {
        sessions.removeAll { it.sessionId == session.sessionId }
        sessions += session
    }

    fun listForPackage(packageName: String): List<GameSessionBenchmark> =
        sessions.filter { it.packageName == packageName }

    fun compare(baselineId: String, candidateId: String): SessionComparisonDiff? {
        val base = sessions.find { it.sessionId == baselineId } ?: return null
        val cand = sessions.find { it.sessionId == candidateId } ?: return null

        val wattsDelta = cand.avgWatts - base.avgWatts
        val jitterDelta = cand.avgJitterMs - base.avgJitterMs
        val ramDelta = cand.minFreeRamMb - base.minFreeRamMb

        val wins = mutableListOf<String>()
        if (wattsDelta < -0.15) wins += "lower power"
        if (jitterDelta < -1.0) wins += "smoother network"
        if (ramDelta > 128) wins += "more RAM headroom"

        val verdict = if (wins.isNotEmpty()) {
            "Candidate improved: ${wins.joinToString(", ")}"
        } else {
            "Comparable or baseline stronger"
        }

        return SessionComparisonDiff(
            baselineId = baselineId,
            candidateId = candidateId,
            wattsDelta = wattsDelta,
            jitterDeltaMs = jitterDelta,
            minRamDeltaMb = ramDelta,
            summaryVerdict = verdict
        )
    }

    fun exportJson(): String = buildString {
        append('[')
        sessions.forEachIndexed { i, s ->
            if (i > 0) append(',')
            append('{')
            append("\"id\":\"${s.sessionId}\",")
            append("\"package\":\"${s.packageName}\",")
            append("\"profile\":\"${s.profileLabel}\",")
            append("\"duration_sec\":${s.durationSec},")
            append("\"avg_watts\":%.2f,".format(s.avgWatts))
            append("\"peak_watts\":%.2f,".format(s.peakWatts))
            append("\"min_ram_mb\":${s.minFreeRamMb},")
            append("\"avg_jitter_ms\":%.2f,".format(s.avgJitterMs))
            append("\"bypass_pct\":%.1f".format(s.bypassDutyCyclePct))
            append('}')
        }
        append(']')
    }
}
