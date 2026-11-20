package dev.redlab.core.capability

/**
 * What a capability is on *this* device. Four values, not a boolean:
 *
 * - [AVAILABLE] measured and it worked here.
 * - [UNSUPPORTED] measured and it failed for a known reason; [Capability.reason] says why.
 * - [NEEDS_PERMISSION] possible, but its permission has not been granted yet.
 * - [UNKNOWN] never measured. This is the default on purpose: an unmeasured capability
 *   must not be shown as working, and must not be shown as broken either.
 *
 * See `docs/CAPABILITY_REGISTRY.md` §1 and §5.
 */
enum class CapabilityState {
    AVAILABLE,
    UNSUPPORTED,
    NEEDS_PERMISSION,
    UNKNOWN,
}

/**
 * What can be done with a capability that exists. Kept separate from [CapabilityState] because
 * "readable" is a real result: a diagnostic row that can be read is not "unavailable", and a
 * control that can only be read must never look like a control that can be moved.
 *
 * Mirrors the access ladder MaxManager ships (`AccessLevel`), which came from measuring that a
 * single "supported / not supported" flag hid read-only controls.
 */
enum class Access { NONE, READ_ONLY, READ_WRITE }

/**
 * One capability, owned by exactly one module.
 *
 * The constructor rejects the two lies this model exists to prevent:
 * claiming [CapabilityState.AVAILABLE] without recorded [evidence], and declaring a
 * capability without a single named [owner] (`docs/CAPABILITY_REGISTRY.md` §3).
 */
data class Capability(
    val id: String,
    val owner: String,
    val state: CapabilityState = CapabilityState.UNKNOWN,
    val access: Access = Access.NONE,
    val reason: String? = null,
    val evidence: String? = null,
    val measuredAtMillis: Long? = null,
) {
    init {
        require(id.isNotBlank() && id.none { it.isWhitespace() }) { "capability id must be non-blank, no spaces: '$id'" }
        require(owner.isNotBlank()) { "capability '$id' needs an owner module" }
        require(state != CapabilityState.AVAILABLE || !evidence.isNullOrBlank()) {
            "capability '$id' cannot be AVAILABLE without evidence of a real measurement"
        }
        require(state != CapabilityState.UNSUPPORTED || !reason.isNullOrBlank()) {
            "capability '$id' cannot be UNSUPPORTED without a literal reason"
        }
    }

    /** A copy with a newer measurement, keeping id and owner fixed. */
    fun remeasured(
        state: CapabilityState,
        access: Access = this.access,
        reason: String? = null,
        evidence: String? = null,
        atMillis: Long,
    ): Capability = copy(
        state = state,
        access = access,
        reason = reason,
        evidence = evidence,
        measuredAtMillis = atMillis,
    )
}
