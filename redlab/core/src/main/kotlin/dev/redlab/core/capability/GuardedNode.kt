package dev.redlab.core.capability

/** Minimal seam over a sysfs-like file so the safety rules can be tested without a device. */
interface NodeIo {
    /** Current value, or null when the node cannot be read (missing or permission denied). */
    fun read(path: String): String?

    /** True when the write was accepted by the kernel/SELinux. */
    fun write(path: String, value: String): Boolean
}

sealed interface WriteOutcome {
    /** Written; [previous] is the value that must be restored later (null if unreadable). */
    data class Applied(val previous: String?) : WriteOutcome

    /** Refused before touching the node: value not in the kernel's own list. */
    data class Rejected(val reason: String) : WriteOutcome

    /** Could not be done here: unreadable node or refused write. Never retried per session. */
    data class Unsupported(val reason: String) : WriteOutcome
}

/**
 * A single hardware node with the safety rules from `docs/HARDWARE_CONTROL_SAFETY.md` §3:
 *
 * - **read before write** — no write happens on an unreadable node;
 * - **restore on release** — the first value seen is written back, not the last one seen;
 * - **only values the kernel offers** — anything outside `allowed` is refused, so no invented
 *   frequency can reach the hardware;
 * - writing the value that is already there is recorded but not performed.
 */
class GuardedNode(
    private val io: NodeIo,
    val path: String,
) {
    private var original: String? = null
    private var applied: String? = null

    val isHolding: Boolean get() = applied != null

    /**
     * @param allowed the kernel's own list (`scaling_available_frequencies`, governors, …).
     *   Empty means "the node has no enumerable list" — then only [value] sanity is applied.
     */
    fun apply(value: String, allowed: Set<String> = emptySet()): WriteOutcome {
        if (allowed.isNotEmpty() && value !in allowed) {
            return WriteOutcome.Rejected("'$value' is not one of ${allowed.sorted()}")
        }
        val previous = io.read(path)
            ?: return WriteOutcome.Unsupported("$path is not readable")
        if (previous == value) {
            if (original == null) original = previous
            applied = value
            return WriteOutcome.Applied(previous)
        }
        if (!io.write(path, value)) {
            return WriteOutcome.Unsupported("write to $path was refused")
        }
        // Keep the *first* value seen, so nested claims cannot lose the stock setting.
        if (original == null) original = previous
        applied = value
        return WriteOutcome.Applied(previous)
    }

    sealed interface RestoreOutcome {
        data object NothingToDo : RestoreOutcome
        data class Restored(val value: String) : RestoreOutcome
        data class Failed(val reason: String) : RestoreOutcome
    }

    /** Put the stock value back. Called on release/end of session, not as a nicety. */
    fun restore(): RestoreOutcome {
        val target = original ?: return RestoreOutcome.NothingToDo
        if (!io.write(path, target)) return RestoreOutcome.Failed("restore of $path to '$target' was refused")
        applied = null
        original = null
        return RestoreOutcome.Restored(target)
    }
}
