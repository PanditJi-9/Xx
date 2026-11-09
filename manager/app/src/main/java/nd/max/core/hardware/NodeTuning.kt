/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import java.util.UUID

/** Values are constrained before the writer is reached; a path is never supplied by editable text. */
data class NodeControl(
    val key: String,
    val path: String,
    val label: String,
    val value: String,
    val format: NodeValueFormat,
    val contextPath: String? = null,
    val contextValue: String? = null,
    val guards: Map<String, String> = emptyMap(),
) {
    fun accepts(text: String): Boolean = format.normalise(text) != null
}

sealed interface NodeValueFormat {
    fun normalise(text: String?): String?

    data class Number(val min: Long, val max: Long, val step: Long = 1L) : NodeValueFormat {
        override fun normalise(text: String?): String? =
            if (min < 0 || max < min || step <= 0) null else parseNumber(text)?.takeIf { it in min..max }?.toString()
    }

    data class Choice(val options: List<String>, val bracketedRead: Boolean = false) : NodeValueFormat {
        override fun normalise(text: String?): String? {
            val raw = text?.trim() ?: return null
            if (raw.length > MAX_VALUE_LENGTH || options.isEmpty() || options.size > 32 || options.distinct().size != options.size) return null
            val value = if (bracketedRead && '[' in raw) {
                val matches = Regex("\\[([A-Za-z0-9_-]+)]").findAll(raw).toList()
                if (matches.size != 1) return null
                matches.single().groupValues[1]
            } else {
                raw
            }
            return value.takeIf { it in options }
        }
    }

    data class Vector(val size: Int, val min: Long, val max: Long) : NodeValueFormat {
        override fun normalise(text: String?): String? {
            val raw = text?.trim() ?: return null
            if (size !in 2..6 || min < 0 || max < min || raw.isEmpty() || raw.length > MAX_VALUE_LENGTH) return null
            val parts = raw.split(Regex("\\s+"))
            if (parts.size != size) return null
            val values = parts.map { parseNumber(it)?.takeIf { value -> value in min..max } ?: return null }
            return values.joinToString(" ")
        }
    }

    companion object {
        const val MAX_VALUE_LENGTH = 256
        fun parseNumber(text: String?): Long? {
            val raw = text?.trim() ?: return null
            if (raw.isEmpty() || raw.length > 20 || raw.any { it !in '0'..'9' }) return null
            return raw.toLongOrNull()
        }
    }
}

enum class NodeTuningOutcome { APPLIED, INVALID, UNREADABLE, BLOCKED, WRITE_FAILED, UNVERIFIED }

data class NodeTuningResult(
    val outcome: NodeTuningOutcome,
    val actual: String? = null,
    val rollbackVerified: Boolean? = null,
)

/** Manual, one-shot mutations reuse the process singleton and leave no deferred intent behind. */
class NodeTuning(
    private val arbiter: HardwareControlArbiter,
    private val io: Io = SystemIo,
) {
    interface Io {
        fun read(path: String): String?
        fun write(path: String, value: String): Boolean
        fun listNames(path: String): List<String>
    }

    object SystemIo : Io {
        override fun read(path: String): String? = RootFileAccess.read(path)
        override fun write(path: String, value: String): Boolean = RootFileAccess.write(path, value)
        override fun listNames(path: String): List<String> = RootFileAccess.listNames(path)
    }

    fun apply(control: NodeControl, requested: String, expected: String = control.value): NodeTuningResult {
        val desired = control.format.normalise(requested) ?: return NodeTuningResult(NodeTuningOutcome.INVALID)
        val before = control.format.normalise(expected) ?: return NodeTuningResult(NodeTuningOutcome.INVALID)
        if (!validControl(control)) return NodeTuningResult(NodeTuningOutcome.INVALID)
        if (control.contextPath?.let { !safePath(it) } == true || control.guards.keys.any { !safePath(it) }) return NodeTuningResult(NodeTuningOutcome.INVALID)
        if (!SharedHardwareOwnershipStore.isConfigured()) return NodeTuningResult(NodeTuningOutcome.BLOCKED)
        val token = "manual-node-${UUID.randomUUID()}"
        var baselineChanged = false
        fun read(): String? = runCatching { control.format.normalise(io.read(control.path)) }.getOrNull()
        fun contextMatches(): Boolean = (control.contextPath?.let { context ->
            runCatching { io.read(context)?.trim() == control.contextValue }.getOrDefault(false)
        } ?: true) && control.guards.all { (path, value) ->
            runCatching { io.read(path)?.trim() == value }.getOrDefault(false)
        }
        val live = read() ?: return NodeTuningResult(NodeTuningOutcome.UNREADABLE)
        if (live != before || !contextMatches()) return NodeTuningResult(NodeTuningOutcome.BLOCKED, live)
        return try {
            val result = arbiter.submit(
                key = control.key,
                owner = ControlOwnership.Owner.GLOBAL_PROFILE,
                token = token,
                desired = desired,
                read = ::read,
                baseline = before,
                apply = { value ->
                    if (read() != before || !contextMatches()) {
                        baselineChanged = true
                        false
                    } else {
                        io.write(control.path, value)
                    }
                },
                restore = { value ->
                    // A changed governor, removed device or external write is not ours to overwrite.
                    if (!contextMatches()) false else {
                        val now = read()
                        now == value || (now == desired && io.write(control.path, value))
                    }
                },
                realized = { _, _ -> contextMatches() && read() == before },
            )
            val actual = read()
            NodeTuningResult(
                outcome = when {
                    result.blocked || baselineChanged -> NodeTuningOutcome.BLOCKED
                    result.verified && actual == desired -> NodeTuningOutcome.APPLIED
                    result.rollbackAttempted && result.rollbackVerified != true -> NodeTuningOutcome.UNVERIFIED
                    !result.applied -> NodeTuningOutcome.WRITE_FAILED
                    else -> NodeTuningOutcome.UNVERIFIED
                },
                actual = actual,
                rollbackVerified = result.rollbackVerified,
            )
        } catch (_: Exception) {
            NodeTuningResult(NodeTuningOutcome.UNVERIFIED, read())
        } finally {
            runCatching { arbiter.release(control.key, token, restore = false) }
        }
    }

    /** A saved preset is indivisible: validate all values first, then unwind confirmed writes on failure. */
    fun applyPreset(requests: List<Pair<NodeControl, String>>): NodeTuningResult {
        if (requests.isEmpty() || requests.size > 128 || requests.map { it.first.key }.distinct().size != requests.size) {
            return NodeTuningResult(NodeTuningOutcome.INVALID)
        }
        if (requests.any { (control, value) -> !control.accepts(value) || control.format.normalise(control.value) == null }) {
            return NodeTuningResult(NodeTuningOutcome.INVALID)
        }
        val completed = arrayListOf<Pair<NodeControl, String>>()
        for ((control, desired) in requests) {
            val result = apply(control, desired)
            if (result.outcome != NodeTuningOutcome.APPLIED) {
                var restored = true
                for ((previous, applied) in completed.asReversed()) {
                    val rollback = apply(previous.copy(value = applied), previous.value, expected = applied)
                    if (rollback.outcome != NodeTuningOutcome.APPLIED) restored = false
                }
                return if (restored) result else NodeTuningResult(NodeTuningOutcome.UNVERIFIED, rollbackVerified = false)
            }
            completed += control to desired
        }
        return NodeTuningResult(NodeTuningOutcome.APPLIED)
    }

    companion object {
        fun validControl(control: NodeControl): Boolean {
            if (!safePath(control.path) || control.key.length > 240) return false
            val path = control.path
            val field = path.substringAfterLast('/')
            return when {
                control.key.startsWith(HardwareControlKey.STORAGE_QUEUE_PREFIX) -> {
                    val match = Regex("^/sys/block/(mmcblk\\d+|sd[a-z]+|nvme\\d+n\\d+|vd[a-z]+)/queue/(scheduler|read_ahead_kb|nr_requests|rq_affinity)$").matchEntire(path)
                        ?: return false
                    control.key == HardwareControlKey.storageQueue(match.groupValues[1], field)
                }
                control.key.startsWith(HardwareControlKey.GOVERNOR_TUNABLE_PREFIX) -> {
                    val match = Regex("^/sys/devices/system/cpu/(?:cpufreq/(?:(policy\\d+)/)?|(?:(cpu\\d+)/cpufreq/))([A-Za-z0-9_-]{1,40})/([A-Za-z0-9_]+)$").matchEntire(path)
                        ?: return false
                    val allowed = setOf("up_rate_limit_us", "down_rate_limit_us", "rate_limit_us", "sampling_rate", "sampling_down_factor", "up_threshold", "go_hispeed_load", "min_sample_time", "timer_rate", "hispeed_freq")
                    val policy = match.groupValues[1].ifEmpty { match.groupValues[2].ifEmpty { "global" } }
                    val governor = match.groupValues[3]
                    field in allowed && control.contextPath != null && control.contextValue == governor &&
                        control.key == HardwareControlKey.governorTunable(policy, governor, field)
                }
                control.key.startsWith(HardwareControlKey.HAPTIC_TUNING_PREFIX) ->
                    path.matches(Regex("^/sys/class/(?:timed_output/vibrator|leds/vibrator(?:/device)?)/(?:vtg_level|vmax_mv_user|vmax_mv_strong|vmax_mv_call)$")) &&
                        control.key == HardwareControlKey.hapticTuning(path)
                control.key.startsWith(HardwareControlKey.PANEL_COLOR_PREFIX) ->
                    path.matches(Regex("^/sys/devices/platform/kcal_ctrl(?:\\.0)?/(?:kcal|kcal_sat|kcal_cont|kcal_val)$")) &&
                        control.key == HardwareControlKey.panelColor(path)
                control.key.startsWith(HardwareControlKey.PANEL_FEATURE_PREFIX) ->
                    path in setOf("/sys/class/leds/lcd-backlight/hbm", "/sys/devices/platform/mtk-hbm/hbm_enable", "/sys/class/backlight/panel0-backlight/hbm_mode", "/sys/class/drm/card0-DSI-1/hbm") &&
                        control.key == HardwareControlKey.panelFeature(path)
                else -> false
            }
        }

        fun safePath(path: String): Boolean = path.startsWith("/sys/") &&
            path.length <= 240 && path.split('/').none { it == ".." || it == "." } &&
            path.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it in "/_-.:" }
    }
}
