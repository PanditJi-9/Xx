/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.atlas

/**
 * Quiet self-healing (`ATLAS-SELFHEAL-01` · S2) — the orchestration half.
 *
 * G-UX-01 («Atlas ظاهر كأداة تشخيص لا كخدمة ذكية») condemned the shape this class removes: the only
 * way anything happened was a person opening a screen. G-UX-02 («لا نملك بعد lifecycle صامتًا») names
 * what was missing: a silent lifecycle. This coordinator is that lifecycle's seam, event-driven by
 * construction — it performs **nothing** at construction, does **not** sweep, and wakes only on the
 * four facts of [AtlasQuietEvent]: with no event there is no work.
 *
 * The write boundary is the injected [AtlasQuietRepair] port. This class never builds a hardware
 * context, never reads a node and never touches an arbiter; in production the port is filled by the
 * hardware layer with the existing `MaxAtlas.execute` → `HardwareRepairExecutor` →
 * `HardwareControlArbiter` transaction — the same single write path everything else uses. Wiring the
 * port to real hardware is deliberately **not** done here: its behavior on a phone cannot be verified
 * in a build-only environment, and guessing at it is exactly the class of change the repository
 * refuses to make.
 *
 * Threading contract: callable from any thread. All state is guarded by one lock; the repair call runs
 * **outside** the lock (it may read and write hardware and must not hold anyone else back), and the
 * target is marked in-flight **inside** the same lock as the decision that proposed it — so two racing
 * events cannot produce two repairs for one target. A repair that throws is folded back as
 * [AtlasQuietRepairOutcome.UNKNOWN_STATE]: an exception mid-transaction leaves a physical state nobody
 * has confirmed, which is the definition of unknown.
 *
 * There is no observable flow and no event stream out of this class on purpose: quiet mode is read-on-
 * open — the control page calls [state] when it opens (G-UX-01) — and nothing here can grow a log as a
 * side effect of working.
 */
class AtlasSessionCoordinator(
    /**
     * The proven set for the current boot/privilege generation. Production derives it with
     * [provenFrom] over the real [nd.max.core.hardware.AtlasRouteMemory]; tests pass a map.
     */
    private val provenOf: () -> Map<AtlasControlTarget, String>,
    /** The hardware seam. The only thing in this class that can change a device. */
    private val repair: AtlasQuietRepair,
    /** Monotonic clock in milliseconds. Required, not defaulted: a wall clock here would leak. */
    private val clockMs: () -> Long,
) {

    private val lock = Any()
    private var state: AtlasQuietState = AtlasQuietState()

    /** The compact status the control page reads when it opens. */
    fun state(): AtlasQuietState = synchronized(lock) { state }

    /**
     * The user has seen the surfaced escalation. Clears it and un-halts its target
     * ([AtlasQuietPolicy.acknowledge]) so quiet repair may resume where the user allows it.
     */
    fun acknowledgeEscalation() {
        synchronized(lock) { state = AtlasQuietPolicy.acknowledge(state) }
    }

    /**
     * One event, one decision, at most one repair.
     *
     * A [AtlasQuietEvent.GenerationChanged] is intercepted before the policy: it means everything
     * previously proven is void, so the coordinator resets and repairs nothing.
     */
    fun onEvent(event: AtlasQuietEvent) {
        if (event is AtlasQuietEvent.GenerationChanged) {
            synchronized(lock) { state = AtlasQuietPolicy.regenerate(state) }
            return
        }
        val proven = provenOf()
        val proposal = synchronized(lock) {
            when (val decision = AtlasQuietPolicy.decide(event, proven, state, clockMs())) {
                is AtlasQuietDecision.StaySilent -> null

                is AtlasQuietDecision.ProposeRepair -> {
                    // في القفل نفسه مع القرار: لولاه أمكن لإشارتين متزامنتين أن تُنتجا إصلاحين لهدف واحد.
                    state = state.copy(inFlight = state.inFlight + decision.target)
                    decision
                }
            }
        } ?: return

        val outcome = runCatching {
            repair.repair(proposal.target, proposal.routeId, proposal.expectedValue)
        }.getOrElse { AtlasQuietRepairOutcome.UNKNOWN_STATE }

        synchronized(lock) { state = AtlasQuietPolicy.reduce(state, proposal.target, outcome, clockMs()) }
    }
}

/**
 * The mutation seam of quiet healing. In production this is filled with `MaxAtlas.execute` — the port
 * exists so this package stays free of a hardware context, and so the mapping below has exactly one
 * implementation site.
 *
 * The implementation maps the execution verdict (`nd.max.core.hardware.HardwareRepairState`) onto the
 * four honest outcomes:
 *
 * | repair state | quiet outcome |
 * | --- | --- |
 * | `CONFIRMED_WINDOW` | [AtlasQuietRepairOutcome.VERIFIED] |
 * | `APPLY_FAILED_ROLLED_BACK` · `DRIFT_ROLLED_BACK` | [AtlasQuietRepairOutcome.NOT_TAKEN_ROLLED_BACK] |
 * | `BLOCKED` | [AtlasQuietRepairOutcome.BLOCKED] |
 * | `ROLLBACK_UNVERIFIED` | [AtlasQuietRepairOutcome.UNKNOWN_STATE] |
 *
 * [expectedValue] is the value that was already verified once; the implementation writes **that**, not
 * anything it derives on its own.
 */
fun interface AtlasQuietRepair {
    fun repair(target: AtlasControlTarget, routeId: String, expectedValue: String): AtlasQuietRepairOutcome
}

/**
 * Derives the proven set from the real learning store, so quiet healing uses the gate that already
 * exists instead of a parallel one.
 *
 * The lookup key is the target's lowercase name — the same key `MaxAtlas.profile` passes to
 * `AtlasRouteMemory.preferredRoute`. The returned snapshot re-reads on every call: provenance is
 * re-evaluated per event, never cached into this layer.
 */
fun provenFrom(
    lookupRoute: (String) -> String?,
    targets: List<AtlasControlTarget> = AtlasControlTarget.values().toList(),
): () -> Map<AtlasControlTarget, String> = {
    targets.mapNotNull { target -> lookupRoute(target.name.lowercase())?.let { target to it } }.toMap()
}
