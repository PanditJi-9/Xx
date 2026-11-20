package dev.redlab.core.seat

import dev.redlab.core.capability.Capability
import dev.redlab.core.capability.CapabilityState
import dev.redlab.core.capability.NodeIo
import java.io.File

/**
 * One way of reaching the device. The contract is fixed by this interface, not by the tier, so no
 * screen ever asks "am I rooted?" — it asks for a capability, and the seat either serves it or
 * reports why not (`docs/CONTROL_TIERS.md`).
 */
interface ControlSeat {
    val tier: Tier

    /** Nodes as seen from this seat. A write that this seat cannot perform returns `false`. */
    val nodes: NodeIo

    fun exec(command: List<String>): ShellResult
}

/** Plain app: own process, plain `java.io`, no shell. */
class LocalSeat : ControlSeat {

    override val tier = Tier.LOCAL

    override val nodes: NodeIo = FileNodeIo

    private val shellRefusal = ShellResult(
        exitCode = NEVER_RAN,
        stdout = "",
        stderr = "no shell at LOCAL tier: this app can only touch its own files",
    )

    override fun exec(command: List<String>): ShellResult = shellRefusal

    private companion object {
        const val NEVER_RAN = -1
    }
}

/**
 * Shizuku/ADB or `su`: the seat differs only in the runner handed in, so the capability code above
 * is identical for both and cannot drift.
 */
class ShellSeat(override val tier: Tier, private val shell: Shell) : ControlSeat {

    override val nodes: NodeIo = ShellNodeIo(shell)

    override fun exec(command: List<String>): ShellResult = shell.run(command)

    init {
        require(tier != Tier.LOCAL) { "a shell seat cannot be LOCAL" }
    }
}

/** `java.io.File`, which is exactly the app sandbox plus whatever the OS lets us read. */
object FileNodeIo : NodeIo {
    override fun read(path: String): String? = try {
        val file = File(path)
        if (!file.isFile) null else file.readText().trim().ifEmpty { null }
    } catch (_: Exception) {
        null
    }

    override fun write(path: String, value: String): Boolean = try {
        File(path).writeText("$value\n")
        true
    } catch (_: Exception) {
        false
    }
}

/** What one seat probe found, and the reason for every seat that was refused. */
data class Negotiation(
    val seat: ControlSeat,
    val rejected: Map<Tier, String>,
) {
    val tier: Tier get() = seat.tier
}

/**
 * Picks the strongest seat that actually works, once, and records why the others did not.
 *
 * Rules taken from the failure modes this project has already measured:
 * - **strongest first** (`ROOT`, then `ADB`, then `LOCAL`), because a capability that needs writing
 *   is not served by a weaker seat;
 * - **one attempt per seat at startup** — retrying `su` on a timer drains battery and never starts
 *   working on its own;
 * - **always a seat**: `LOCAL` needs no probe and is the floor, so the app is never seatless;
 * - **reasons are kept**, so a degraded control can tell the user "needs Shizuku" instead of
 *   pretending to be unavailable hardware.
 *
 * A user-chosen tier is honoured even when a stronger one is available (see [preferred]).
 */
class SeatNegotiator(
    private val root: (() -> ControlSeat?)? = null,
    private val adb: (() -> ControlSeat?)? = null,
    private val local: () -> ControlSeat = ::LocalSeat,
) {
    fun negotiate(preferred: Tier? = null): Negotiation {
        val rejected = linkedMapOf<Tier, String>()
        val order = when (preferred) {
            Tier.LOCAL -> listOf(Tier.LOCAL)
            Tier.ADB -> listOf(Tier.ADB, Tier.LOCAL)
            Tier.ROOT -> listOf(Tier.ROOT, Tier.ADB, Tier.LOCAL)
            null -> listOf(Tier.ROOT, Tier.ADB, Tier.LOCAL)
        }
        for (tier in order) {
            when (tier) {
                Tier.LOCAL -> return Negotiation(local(), rejected)
                Tier.ROOT -> {
                    val seat = probe(root, Tier.ROOT, "su", rejected)
                    if (seat != null) return Negotiation(seat, rejected)
                }
                Tier.ADB -> {
                    val seat = probe(adb, Tier.ADB, "shizuku", rejected)
                    if (seat != null) return Negotiation(seat, rejected)
                }
            }
        }
        // Unreachable in practice: LOCAL is in every order above.
        return Negotiation(local(), rejected)
    }

    private fun probe(
        factory: (() -> ControlSeat?)?,
        tier: Tier,
        what: String,
        rejected: MutableMap<Tier, String>,
    ): ControlSeat? {
        if (factory == null) {
            rejected[tier] = "no $what bridge in this build"
            return null
        }
        val seat = try {
            factory()
        } catch (e: Exception) {
            rejected[tier] = "$what bridge failed: ${e.message ?: e::class.simpleName}"
            null
        } ?: run {
            if (!rejected.containsKey(tier)) rejected[tier] = "$what is not installed or was denied"
            null
        }
        if (seat == null) return null
        // A seat that cannot report its identity is not usable: we would be guessing its powers.
        val identity = seat.exec(listOf("id", "-u"))
        if (!identity.ok || identity.stdout.trim().isEmpty()) {
            rejected[tier] = "$what accepted the call but could not run 'id -u'"
            return null
        }
        return seat
    }
}

/**
 * Marks what a capability needs and what this device gives, without inventing a sixth state for
 * every combination: the capability stays [CapabilityState.AVAILABLE] for reading and is reported
 * as [CapabilityState.DEGRADED] when this seat cannot write it.
 */
fun Capability.gradedFor(seatTier: Tier, atMillis: Long): Capability {
    val needed = requiredTier ?: return this
    if (seatTier.rank >= needed.rank) return this
    return remeasured(
        state = CapabilityState.DEGRADED,
        access = if (access == dev.redlab.core.capability.Access.READ_WRITE) {
            dev.redlab.core.capability.Access.READ_ONLY
        } else {
            access
        },
        reason = "needs ${needed.name} access, running at ${seatTier.name}",
        evidence = evidence,
        atMillis = atMillis,
    )
}

private val Tier.rank: Int get() = when (this) {
    Tier.LOCAL -> 0
    Tier.ADB -> 1
    Tier.ROOT -> 2
}
