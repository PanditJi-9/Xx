package dev.redlab.core.capability

import java.util.concurrent.atomic.AtomicLong

/** Lower ordinal wins. `SAFETY` is absolute: it preempts, and nothing preempts it. */
enum class Priority { SAFETY, USER, GAME, SYSTEM }

data class HardwareRequest(
    val id: String,
    val requester: String,
    val priority: Priority = Priority.SYSTEM,
    /** Desired value; null means "claim the control without changing it". */
    val value: String? = null,
)

data class ClaimToken(
    val id: String,
    val requester: String,
    val priority: Priority,
    val sequence: Long,
) {
    override fun toString(): String = "ClaimToken($id by $requester, $priority, #$sequence)"
}

sealed interface ClaimResult {
    data class Granted(val token: ClaimToken) : ClaimResult

    data class Denied(val id: String, val holder: String, val holderPriority: Priority) : ClaimResult {
        val reason: String get() = "'$id' is held by '$holder' ($holderPriority)"
    }

    /** The capability itself is not usable here — never retried in the same session. */
    data class Unsupported(val id: String, val reason: String) : ClaimResult
}

/**
 * Arbitration for hardware controls. One owner per control at a time, and the contract is
 * `docs/HARDWARE_CONTROL_SAFETY.md` §3:
 *
 * 1. an `UNKNOWN`/`UNSUPPORTED` capability cannot be claimed at all;
 * 2. when a safety ceiling has tripped, only [Priority.SAFETY] may claim;
 * 3. a strictly higher priority preempts the current holder; equal or lower is denied;
 * 4. a preempted holder is **not** restored automatically — it must re-claim, because
 *    silently re-granting a control the user just overrode is how fights start;
 * 5. only the current token can release.
 */
class HardwareControlArbiter(
    private val registry: CapabilityRegistry,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private data class Active(val token: ClaimToken, val previous: Active?)

    private val lock = Any()
    private val active = HashMap<String, Active>()
    private val sequence = AtomicLong(0)
    private var safetyTripped: String? = null

    /** Called when a claim is accepted; the owner performs the write here. */
    var onGranted: ((ClaimToken) -> Unit)? = null

    /** Called for a holder that lost the control to a higher priority. */
    var onPreempted: ((ClaimToken) -> Unit)? = null

    /** Trip the safety ceiling (e.g. battery temperature), which freezes non-safety claims. */
    fun tripSafety(reason: String) = synchronized(lock) { safetyTripped = reason }

    fun clearSafety() = synchronized(lock) { safetyTripped = null }

    fun safetyReason(): String? = synchronized(lock) { safetyTripped }

    fun claim(request: HardwareRequest): ClaimResult {
        val current: Active?
        val grant: ClaimToken
        synchronized(lock) {
            val state = registry.stateOf(request.id)
            if (state != CapabilityState.AVAILABLE) {
                return ClaimResult.Unsupported(request.id, "state=$state, not AVAILABLE on this device")
            }
            safetyTripped?.let { why ->
                if (request.priority != Priority.SAFETY) {
                    return ClaimResult.Unsupported(request.id, "safety ceiling: $why")
                }
            }
            current = active[request.id]
            if (current != null) {
                val held = current.token
                val challengerWins = request.priority.ordinal < held.priority.ordinal
                        && held.priority != Priority.SAFETY
                if (!challengerWins) {
                    return ClaimResult.Denied(request.id, held.requester, held.priority)
                }
            }
            grant = ClaimToken(request.id, request.requester, request.priority, sequence.incrementAndGet())
            active[request.id] = Active(grant, current)
        }
        current?.let { onPreempted?.invoke(it.token) }
        onGranted?.invoke(grant)
        return ClaimResult.Granted(grant)
    }

    /** True only for the token that currently holds the control. Releases are not chained. */
    fun release(token: ClaimToken): Boolean {
        synchronized(lock) {
            val entry = active[token.id] ?: return false
            if (entry.token.sequence != token.sequence) return false
            val previous = entry.previous
            if (previous == null) active.remove(token.id) else active[token.id] = previous
            return true
        }
    }

    /** Diagnostics: who holds what right now. */
    fun holders(): Map<String, ClaimToken> = synchronized(lock) {
        active.mapValues { it.value.token }
    }
}
