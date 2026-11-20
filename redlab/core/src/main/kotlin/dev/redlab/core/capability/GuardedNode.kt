package dev.redlab.core.capability

/** Minimal seam over a sysfs-like file so the safety rules can be tested without a device. */
interface NodeIo {
    /** Current value, or null when the node cannot be read (missing or permission denied). */
    fun read(path: String): String?

    /** True when the write was accepted by the kernel/SELinux. */
    fun write(path: String, value: String): Boolean
}

/**
 * How to decide whether a value counts as reached. A ceiling knob (`scaling_max_freq`) is
 * satisfied by any value at or below the request, not only by an exact match — measuring that
 * difference is what stopped a thermal cap from being read as a failure and rolled back.
 */
fun interface ValuePolicy {
    fun satisfied(requested: String, actual: String?): Boolean

    companion object {
        /** Literal equality. Use for enums/strings (`scaling_governor`). */
        val Exact = ValuePolicy { requested, actual -> actual == requested }

        /** Numeric ceiling: live value at or below the request satisfies it. */
        val MaxCeiling = ValuePolicy { requested, actual ->
            val want = requested.toLongOrNull()
            val got = actual?.toLongOrNull()
            if (want == null || got == null) actual == requested else got <= want
        }

        /** Numeric floor: live value at or above the request satisfies it. */
        val MinFloor = ValuePolicy { requested, actual ->
            val want = requested.toLongOrNull()
            val got = actual?.toLongOrNull()
            if (want == null || got == null) actual == requested else got >= want
        }
    }
}

sealed interface WriteOutcome {
    /** We wrote it and the re-read confirmed it; [previous] must be restored later. */
    data class Applied(val previous: String?) : WriteOutcome

    /**
     * The node already satisfied the request before we touched it — a platform cap tighter than
     * ours, or an earlier write. Reporting this as "applied" is how a device quietly stays at the
     * wrong value while the UI says success.
     */
    data class AlreadySatisfied(val actual: String) : WriteOutcome

    /** Refused before touching the node: value not in the kernel's own list. */
    data class Rejected(val reason: String) : WriteOutcome

    /** Could not be done here: unreadable node or refused write. Never retried per session. */
    data class Unsupported(val reason: String) : WriteOutcome

    /** The write was accepted but the node did not reach the value; [restored] says if we undid it. */
    data class NotRealized(
        val requested: String,
        val actual: String?,
        val restored: Boolean,
    ) : WriteOutcome
}

/**
 * A single hardware node with the safety rules from `docs/HARDWARE_CONTROL_SAFETY.md` §3:
 *
 * - **read before write** — no write happens on an unreadable node;
 * - **verify after write** — success means the re-read satisfied [ValuePolicy], not that
 *   `write()` returned true;
 * - **roll back a write that did not land** — otherwise the device keeps a value the caller
 *   believes it never set;
 * - **restore on release** — the first value seen is written back, not the last one seen;
 * - **only values the kernel offers** — anything outside `allowed` is refused, so no invented
 *   frequency can reach the hardware;
 * - **no false credit** — a value already in place is reported as [WriteOutcome.AlreadySatisfied].
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
     *   Empty means "the node has no enumerable list".
     * @param policy how to judge the result; equality unless the knob is a ceiling/floor.
     */
    fun apply(
        value: String,
        allowed: Set<String> = emptySet(),
        policy: ValuePolicy = ValuePolicy.Exact,
    ): WriteOutcome {
        if (allowed.isNotEmpty() && value !in allowed) {
            return WriteOutcome.Rejected("'$value' is not one of ${allowed.sorted()}")
        }
        val previous = io.read(path)
            ?: return WriteOutcome.Unsupported("$path is not readable")

        if (policy.satisfied(value, previous)) {
            // Hold nothing: we did not change the node, so there is nothing to restore.
            return WriteOutcome.AlreadySatisfied(previous)
        }
        if (!io.write(path, value)) {
            return WriteOutcome.Unsupported("write to $path was refused")
        }
        val actual = io.read(path)
        if (!policy.satisfied(value, actual)) {
            // Accepted by the kernel but not reached: undo our own attempt, do not linger.
            val restored = io.write(path, previous)
            return WriteOutcome.NotRealized(value, actual, restored)
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
