package dev.redlab.core.capability

/**
 * The one place that answers "what is the state of capability X on this device".
 *
 * Deliberately narrow: no Android types, no I/O, no device-name guessing. Probes live in
 * the owning module and report results here; this class only holds and broadcasts them.
 *
 * Thread-safe because probes finish on background threads while the UI reads snapshots.
 */
class CapabilityRegistry {

    private val lock = Any()
    private val capabilities = LinkedHashMap<String, Capability>()
    private val listeners = mutableListOf<(Capability) -> Unit>()

    /**
     * Declare a capability before it is measured. Re-declaring the same id from a different
     * owner is refused: one capability has one owner (`docs/CAPABILITY_REGISTRY.md` §3).
     */
    fun declare(capability: Capability): Capability = synchronized(lock) {
        val existing = capabilities[capability.id]
        require(existing == null || existing.owner == capability.owner) {
            "capability '${capability.id}' is owned by '${existing?.owner}', not '${capability.owner}'"
        }
        capabilities[capability.id] = existing?.copy(state = capability.state, reason = capability.reason) ?: capability
        capability
    }

    /** Record a measurement. Unknown ids are refused rather than silently invented. */
    fun record(capability: Capability): Capability {
        val toNotify: List<(Capability) -> Unit>
        synchronized(lock) {
            val existing = capabilities[capability.id]
                ?: throw IllegalArgumentException("capability '${capability.id}' was never declared")
            require(existing.owner == capability.owner) {
                "capability '${capability.id}' is owned by '${existing.owner}', not '${capability.owner}'"
            }
            capabilities[capability.id] = capability
            toNotify = listeners.toList()
        }
        // Listeners run outside the lock: one slow observer must not block recording.
        toNotify.forEach { it(capability) }
        return capability
    }

    /** [CapabilityState.UNKNOWN] for anything not declared — never a guessed state. */
    fun stateOf(id: String): CapabilityState = synchronized(lock) {
        capabilities[id]?.state ?: CapabilityState.UNKNOWN
    }

    fun get(id: String): Capability? = synchronized(lock) { capabilities[id] }

    fun snapshot(): List<Capability> = synchronized(lock) { capabilities.values.toList() }

    fun observe(listener: (Capability) -> Unit): AutoCloseable {
        synchronized(lock) { listeners += listener }
        return AutoCloseable { synchronized(lock) { listeners -= listener } }
    }
}
