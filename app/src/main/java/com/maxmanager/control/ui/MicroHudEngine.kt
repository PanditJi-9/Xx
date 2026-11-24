package com.maxmanager.control.ui

import kotlin.math.abs

data class MicroHudFrame(
    val watts: Double,
    val batterySoc: Int,
    val tempCelsius: Double,
    val ramAvailMb: Int,
    val pingMs: Int,
    val jitterMs: Double,
    val pinnedThreads: Int,
    val frozenApps: Int,
    val bypassActive: Boolean,
    val qosActive: Boolean
)

class MicroHudEngine {
    private var lastPingMs: Int? = null
    private var ewmaJitterMs: Double = 0.0

    fun recordPingSample(pingMs: Int): Double {
        val prev = lastPingMs
        if (prev != null) {
            val delta = abs(pingMs - prev).toDouble()
            ewmaJitterMs = ewmaJitterMs + (delta - ewmaJitterMs) / 8.0
        }
        lastPingMs = pingMs
        return ewmaJitterMs
    }

    fun parseDaemonHudJson(json: String, currentPingMs: Int = 0, bypassActive: Boolean = false): MicroHudFrame {
        val powerMw = extractLong(json, "power_mw") ?: 0L
        val soc = extractInt(json, "battery_soc") ?: 0
        val tempDc = extractInt(json, "battery_temp_dc") ?: 0
        val ramMb = extractInt(json, "ram_avail_mb") ?: 0
        val pinned = extractInt(json, "pinned_threads") ?: 0
        val frozen = extractInt(json, "frozen_apps") ?: 0
        val qos = extractBool(json, "qos_active") ?: false

        if (currentPingMs > 0) recordPingSample(currentPingMs)

        return MicroHudFrame(
            watts = powerMw / 1000.0,
            batterySoc = soc,
            tempCelsius = tempDc / 10.0,
            ramAvailMb = ramMb,
            pingMs = currentPingMs,
            jitterMs = ewmaJitterMs,
            pinnedThreads = pinned,
            frozenApps = frozen,
            bypassActive = bypassActive,
            qosActive = qos
        )
    }

    fun formatCompactStrip(frame: MicroHudFrame): String {
        val bp = if (frame.bypassActive) "BYPASS" else "CHG"
        val qos = if (frame.qosActive) "QoS" else "-"
        return "%.2fW | %.1f°C | %d%% %s | RAM %dM | %dms (±%.1f) | Pin:%d Frz:%d [%s]".format(
            frame.watts, frame.tempCelsius, frame.batterySoc, bp,
            frame.ramAvailMb, frame.pingMs, frame.jitterMs,
            frame.pinnedThreads, frame.frozenApps, qos
        )
    }

    private fun extractLong(json: String, key: String): Long? =
        Regex("\"$key\"\\s*:\\s*(-?\\d+)").find(json)?.groupValues?.get(1)?.toLongOrNull()

    private fun extractInt(json: String, key: String): Int? =
        extractLong(json, key)?.toInt()

    private fun extractBool(json: String, key: String): Boolean? =
        Regex("\"$key\"\\s*:\\s*(true|false)").find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
}
