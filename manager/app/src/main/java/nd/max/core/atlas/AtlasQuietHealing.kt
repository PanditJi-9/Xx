/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.atlas

/**
 * Quiet self-healing (`ATLAS-SELFHEAL-01` · S2) — the whole decision, without a device.
 *
 * The complaint this answers is «ميزة تعمل على جهاز ولا تعمل على آخر، وأُعيد الإصلاح بيدي» — and the
 * safety boundary for the automatic half was chosen with the owner: **silent repair only for a route
 * that was already verified**, everything else stays a click. Three consequences, each held here as a
 * property of the types rather than as a logging habit:
 *
 * 1. **Nothing here can invent a value.** A drift event carries the expected value that was observed;
 *    an event without one is refused with [AtlasQuietSilence.NO_EXPECTED_VALUE]. The policy restores
 *    what was already asked for — it never decides what the value should be.
 * 2. **Nothing here can invent a route.** A target with no route verified in this boot/privilege
 *    generation is refused with [AtlasQuietSilence.NOT_PROVEN]. The proven set is derived from
 *    [nd.max.core.hardware.AtlasRouteMemory.preferredRoute] by the caller — this file reads no store.
 * 3. **The log stays small by construction.** [AtlasQuietState] is counters plus the last escalation;
 *    a successful silent repair exists as a count and is never an append-only event line. That is the
 *    «صغر حجم السجل» requirement expressed as a type, not as a convention someone must remember.
 *
 * The file is pure: no I/O, no reader, no clock of its own (the caller passes `nowMs`), no Android
 * import. The one way quiet healing can reach a device is the injected repair port in
 * [AtlasSessionCoordinator] — and that port is what the hardware layer fills, not this file.
 *
 * One safety rule deserves its own name: an outcome of [AtlasQuietRepairOutcome.UNKNOWN_STATE] — a
 * repair whose rollback could not be confirmed — **halts** the target. A halted target is never
 * auto-repaired again until a generation change or an acknowledgement, because after an unverified
 * rollback the physical state is not known, and retrying into an unknown state is the one thing this
 * layer must never do.
 */

/** Why Atlas stayed quiet. Stable codes: the decision owns the reason, the screen owns the wording. */
enum class AtlasQuietSilence {
    /** Not a repair trigger at all (foreground change, explicit intent, generation change). */
    NOT_ACTIONABLE,

    /** The target has no route verified in this boot/privilege generation. */
    NOT_PROVEN,

    /** Drift was seen but no expected value was carried, so there is nothing to restore. */
    NO_EXPECTED_VALUE,

    /** A repair for this target is already running. */
    IN_FLIGHT,

    /** A silent repair for this target ran too recently — see [AtlasQuietPolicy.REPAIR_MIN_INTERVAL_MS]. */
    RATE_LIMITED,

    /** The target hit an unverified rollback; auto-repair is off until a generation change or acknowledgement. */
    HALTED,
}

/** What one quiet repair reported back. Honest states, not a boolean. */
enum class AtlasQuietRepairOutcome {
    /** The value was written and held through the confirmation window. The one success. */
    VERIFIED,

    /** The write did not hold, but the baseline was restored cleanly. Retrying is safe. */
    NOT_TAKEN_ROLLED_BACK,

    /** The route or the arbiter refused before any write (ownership, safety policy, no route). */
    BLOCKED,

    /** A rollback could not be confirmed: the physical state is unknown, so the target halts. */
    UNKNOWN_STATE,
}

/** A drift event is a signal, not a data channel: both values are bounded like [AtlasControlIntent]. */
private const val MAX_VALUE_CHARS = 128

/**
 * An event the quiet coordinator reacts to. Four facts, four sources, one repairable.
 *
 * - [OwnershipDrift] — an external writer moved a value away from what was last verified. This is the
 *   event the whole file is about; it must carry the expected value or it contains nothing to do.
 *   `liveValue` is context for a report, never written back.
 * - [ForegroundChanged] — a scan trigger, not evidence of drift. Recognized and refused as
 *   [AtlasQuietSilence.NOT_ACTIONABLE]: the per-app re-apply it would drive needs its profile store
 *   wired first, and that is a device step, not a decision step.
 * - [ControlIntent] — an explicit request. It runs on the normal `MaxAtlas.execute` path; the quiet
 *   coordinator must never double-apply what the user just asked for.
 * - [GenerationChanged] — a new boot or privilege generation voids everything proven. It resets the
 *   coordinator rather than triggering a repair.
 */
sealed interface AtlasQuietEvent {

    data class OwnershipDrift(
        val target: AtlasControlTarget,
        val liveValue: String?,
        val expectedValue: String?,
    ) : AtlasQuietEvent {
        init {
            require(liveValue == null || liveValue.length <= MAX_VALUE_CHARS) { "live value exceeds the bounded size" }
            require(expectedValue == null || expectedValue.length <= MAX_VALUE_CHARS) { "expected value exceeds the bounded size" }
        }
    }

    data class ForegroundChanged(val packageName: String) : AtlasQuietEvent {
        init {
            require(packageName.isNotBlank()) { "a foreground change names a package" }
        }
    }

    data class ControlIntent(val intent: AtlasControlIntent) : AtlasQuietEvent

    data class GenerationChanged(
        val bootGeneration: Long,
        val privilegeGeneration: Long,
    ) : AtlasQuietEvent {
        init {
            require(bootGeneration >= 0L && privilegeGeneration >= 0L) { "generations are non-negative" }
        }
    }
}

/** The decision for one event. */
sealed interface AtlasQuietDecision {

    /** Stay quiet, with the stable reason a report or a screen can quote. */
    data class StaySilent(val reason: AtlasQuietSilence) : AtlasQuietDecision

    /** Re-apply [expectedValue] for [target] through the already-proven [routeId]. */
    data class ProposeRepair(
        val target: AtlasControlTarget,
        val routeId: String,
        val expectedValue: String,
    ) : AtlasQuietDecision {
        init {
            require(routeId.isNotBlank()) { "a proposed repair names a proven route" }
            require(expectedValue.isNotBlank()) { "a proposed repair carries the expected value" }
        }
    }
}

/**
 * The one thing that leaves quiet mode: a repeated failure, or an unverified rollback.
 *
 * It is a *notice*, not a dialog — the compact one-line state G-UX-01 asks for. Success never produces
 * one, and an escalation is replaced by a later one for a different target rather than accumulated.
 */
data class AtlasQuietEscalation(
    val target: AtlasControlTarget,
    val cause: AtlasQuietRepairOutcome,
    val consecutive: Int,
    val whenMs: Long,
) {
    init {
        require(cause != AtlasQuietRepairOutcome.VERIFIED) { "an escalation is a failure state" }
        require(consecutive >= 1) { "an escalation counts at least one failure" }
        require(whenMs >= 0L) { "clock readings are non-negative" }
    }
}

/**
 * What quiet mode remembers: counters and the last escalation — deliberately not a log.
 *
 * - `lastRepairAtMs` is the rate-limit memory (per target, monotonic clock readings).
 * - `inFlight` is the single-flight guard; a target is in it only while its repair runs.
 * - `consecutiveFailures` is what thresholds act on; a success removes its target.
 * - `halted` is the UNKNOWN_STATE stop; only [AtlasQuietPolicy.regenerate] and
 *   [AtlasQuietPolicy.acknowledge] clear it.
 * - `silentRepairs` counts verified silent repairs. There is no per-event history beyond this —
 *   a failure is visible through `escalation`, which is why the log cannot grow with success.
 */
data class AtlasQuietState(
    val lastRepairAtMs: Map<AtlasControlTarget, Long> = emptyMap(),
    val inFlight: Set<AtlasControlTarget> = emptySet(),
    val consecutiveFailures: Map<AtlasControlTarget, Int> = emptyMap(),
    val halted: Set<AtlasControlTarget> = emptySet(),
    val silentRepairs: Int = 0,
    val escalation: AtlasQuietEscalation? = null,
)

/**
 * The quiet-healing rules, as pure functions over snapshots the caller owns.
 *
 * [decide] answers one question per event — repair or stay quiet, and why — with refusals checked in a
 * fixed order so one event always yields one reason: **halt first** (a hazard outranks everything),
 * then **proven route** (without one there is nothing to write through, whatever the value), then the
 * missing value, then in-flight, then the rate limit.
 *
 * [reduce] folds a repair outcome back in and is the single place escalation thresholds live. The
 * thresholds are **design values, unmeasured** ([PROVENANCE]): they bound how much quiet healing may
 * try before it asks for attention, and no device evidence exists yet to tune them.
 */
object AtlasQuietPolicy {

    /**
     * Minimum spacing between two silent repairs of the same target.
     *
     * Not a performance knob: a vendor daemon that republishes on a seconds-long timer and a quiet loop
     * that reacts instantly would fight forever. A minute of restraint turns a fight into a repair.
     */
    const val REPAIR_MIN_INTERVAL_MS: Long = 60_000L

    /** After this many consecutive clean-blocked attempts, the target stops being quiet. */
    const val ESCALATE_AFTER_BLOCKED: Int = 3

    /** After this many consecutive rolled-back attempts, the target stops being quiet. */
    const val ESCALATE_AFTER_ROLLBACK: Int = 2

    /** Provenance, so nobody mistakes these thresholds for measured ones. */
    const val PROVENANCE: String = "design values; unmeasured; ATLAS-SELFHEAL-01 S2"

    fun decide(
        event: AtlasQuietEvent,
        proven: Map<AtlasControlTarget, String>,
        state: AtlasQuietState,
        nowMs: Long,
    ): AtlasQuietDecision = when (event) {
        is AtlasQuietEvent.OwnershipDrift -> decideDrift(event, proven, state, nowMs)
        is AtlasQuietEvent.ForegroundChanged -> AtlasQuietDecision.StaySilent(AtlasQuietSilence.NOT_ACTIONABLE)
        is AtlasQuietEvent.ControlIntent -> AtlasQuietDecision.StaySilent(AtlasQuietSilence.NOT_ACTIONABLE)
        is AtlasQuietEvent.GenerationChanged -> AtlasQuietDecision.StaySilent(AtlasQuietSilence.NOT_ACTIONABLE)
    }

    private fun decideDrift(
        event: AtlasQuietEvent.OwnershipDrift,
        proven: Map<AtlasControlTarget, String>,
        state: AtlasQuietState,
        nowMs: Long,
    ): AtlasQuietDecision {
        if (event.target in state.halted) return AtlasQuietDecision.StaySilent(AtlasQuietSilence.HALTED)
        val routeId = proven[event.target] ?: return AtlasQuietDecision.StaySilent(AtlasQuietSilence.NOT_PROVEN)
        val expectedValue = event.expectedValue ?: return AtlasQuietDecision.StaySilent(AtlasQuietSilence.NO_EXPECTED_VALUE)
        if (event.target in state.inFlight) return AtlasQuietDecision.StaySilent(AtlasQuietSilence.IN_FLIGHT)
        val last = state.lastRepairAtMs[event.target]
        // A clock that moved backwards computes a negative difference and stays rate-limited: an
        // unmeasurable interval is not a short one (same direction as `AtlasFreshness.UNMEASURABLE_CLOCK`).
        if (last != null && nowMs - last < REPAIR_MIN_INTERVAL_MS) {
            return AtlasQuietDecision.StaySilent(AtlasQuietSilence.RATE_LIMITED)
        }
        return AtlasQuietDecision.ProposeRepair(event.target, routeId, expectedValue)
    }

    /**
     * Folds one repair outcome into the state. This is the only writer of the escalation slot.
     *
     * A verified repair removes the target's failure streak and **clears an escalation that belongs to
     * that same target** — a warning that outlived the condition it warned about is a false alarm, and
     * quiet mode hides details, not truth. An escalation for a different target is kept.
     */
    fun reduce(
        state: AtlasQuietState,
        target: AtlasControlTarget,
        outcome: AtlasQuietRepairOutcome,
        nowMs: Long,
    ): AtlasQuietState {
        val consecutive = (state.consecutiveFailures[target] ?: 0) + 1
        val escalates = when (outcome) {
            AtlasQuietRepairOutcome.VERIFIED -> false
            AtlasQuietRepairOutcome.UNKNOWN_STATE -> true
            AtlasQuietRepairOutcome.NOT_TAKEN_ROLLED_BACK -> consecutive >= ESCALATE_AFTER_ROLLBACK
            AtlasQuietRepairOutcome.BLOCKED -> consecutive >= ESCALATE_AFTER_BLOCKED
        }
        return state.copy(
            lastRepairAtMs = state.lastRepairAtMs + (target to nowMs),
            inFlight = state.inFlight - target,
            consecutiveFailures =
                if (outcome == AtlasQuietRepairOutcome.VERIFIED) {
                    state.consecutiveFailures - target
                } else {
                    state.consecutiveFailures + (target to consecutive)
                },
            halted =
                if (outcome == AtlasQuietRepairOutcome.UNKNOWN_STATE) state.halted + target else state.halted,
            silentRepairs = state.silentRepairs + if (outcome == AtlasQuietRepairOutcome.VERIFIED) 1 else 0,
            escalation = when {
                escalates -> AtlasQuietEscalation(target, outcome, consecutive, nowMs)
                outcome == AtlasQuietRepairOutcome.VERIFIED -> state.escalation?.takeIf { it.target != target }
                else -> state.escalation
            },
        )
    }

    /**
     * A generation change voids provenance and any hazard recorded under the old one.
     *
     * `lastRepairAtMs` is deliberately kept: rate-limiting across a reboot is harmless and
     * conservative. `silentRepairs` is a lifetime count and is kept too — it is a counter, not a claim.
     */
    fun regenerate(state: AtlasQuietState): AtlasQuietState = state.copy(
        inFlight = emptySet(),
        consecutiveFailures = emptyMap(),
        halted = emptySet(),
        escalation = null,
    )

    /**
     * The user has seen the escalation, so retry may resume.
     *
     * Failure counters are *not* cleared: a target that fails again right after acknowledgement reaches
     * its threshold sooner, which is the conservative direction — acknowledgement un-halts, it does not
     * grant a fresh streak of attempts.
     */
    fun acknowledge(state: AtlasQuietState): AtlasQuietState = state.copy(
        halted = emptySet(),
        escalation = null,
    )
}
