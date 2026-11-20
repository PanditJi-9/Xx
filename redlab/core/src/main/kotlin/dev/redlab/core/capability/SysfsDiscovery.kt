package dev.redlab.core.capability

/**
 * Turns "does this device have that node, and can it be written?" into a [Capability], with the
 * evidence written down instead of guessed.
 *
 * Access is decided by **trying**, never by the vendor or the model name: existence of a path is
 * not proof of anything (`docs/CAPABILITY_REGISTRY.md` §1). A readable node is `READ_ONLY`; a node
 * that accepts a write of the value it already holds is `READ_WRITE`. The no-op write is the
 * cheapest honest test there is: it cannot change a value, and a refused one is exactly the
 * SELinux/permission answer we need.
 */
class SysfsDiscovery(
    private val io: NodeIo,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /**
     * @param declared the capability as declared by its owner; id/owner are preserved.
     * @param candidates paths to try in order, most specific first.
     * @param writeProbe whether to test writability (false for read-only telemetry).
     */
    fun probe(
        declared: Capability,
        candidates: List<String>,
        writeProbe: Boolean = false,
    ): Capability {
        val at = clock()
        val readable = candidates.firstNotNullOfOrNull { path ->
            io.read(path)?.let { path to it }
        }
        if (readable == null) {
            return declared.remeasured(
                state = CapabilityState.UNSUPPORTED,
                access = Access.NONE,
                reason = "none readable: ${candidates.joinToString()}",
                evidence = null,
                atMillis = at,
            )
        }
        val (path, value) = readable
        val base = "read $path = '$value'"
        if (!writeProbe) {
            return declared.remeasured(
                state = CapabilityState.AVAILABLE,
                access = Access.READ_ONLY,
                evidence = base,
                atMillis = at,
            )
        }
        // Writing the current value back is a no-op by construction; its acceptance is the probe.
        val writable = io.write(path, value)
        return declared.remeasured(
            state = CapabilityState.AVAILABLE,
            access = if (writable) Access.READ_WRITE else Access.READ_ONLY,
            evidence = if (writable) "$base; no-op write accepted" else "$base; write refused (read-only)",
            atMillis = at,
        )
    }
}
