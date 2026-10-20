/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import nd.max.core.atlas.AtlasStoreIo
import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Locale

/** What the last attempt of one route did. Three facts, not two: see the class documentation. */
enum class AtlasRouteOutcome {
    /** The write was read back unchanged for the bounded confirmation window. */
    VERIFIED,

    /** The attempt did not take, but the baseline was restored. Retrying another route is safe. */
    FAILED,

    /**
     * The attempt did not take **and** the baseline could not be confirmed.
     *
     * This is deliberately not folded into [FAILED]: a route that failed cleanly teaches us nothing
     * about the device's safety, while a route that left an unknown state must not be tried again in
     * the same boot generation.
     */
    ROLLBACK_FAILED,
}

/** One route's remembered state, for one target, in one boot/privilege generation. */
data class AtlasRouteMemoryEntry(
    val target: String,
    val routeId: String,
    val lastOutcome: AtlasRouteOutcome,
    val lastVerifiedElapsedMs: Long?,
    /**
     * When this route was last *attempted*, successful or not.
     *
     * Separate from [lastVerifiedElapsedMs] because a quarantine never carries a verification, so
     * ordering quarantines by verification time put every one of them at the same value and left
     * the tie to the file name. That is not a detail: `pruneIfNeeded` promises that the oldest
     * quarantine yields to the newest, and a name ordering can discard the newest one — the only
     * record describing the state the device is in right now.
     *
     * Null for legacy records written before this field existed; callers fall back to
     * [lastVerifiedElapsedMs] and then to the oldest-possible value, which keeps an unaged
     * quarantine as the first to yield rather than the last.
     */
    val lastAttemptElapsedMs: Long? = null,
    val bootGeneration: Long,
    val privilegeGeneration: Long,
    val successes: Int,
    val failures: Int,
    val rollbackFailures: Int,
) {
    /** An unknown boot cannot prove that a previous unsafe state has gone away. */
    fun isQuarantinedIn(bootGeneration: Long, privilegeGeneration: Long): Boolean =
        rollbackFailures > 0 &&
            (this.bootGeneration == 0L || bootGeneration == 0L ||
                (this.bootGeneration == bootGeneration && this.privilegeGeneration == privilegeGeneration))

    fun isVerifiedIn(bootGeneration: Long, privilegeGeneration: Long): Boolean =
        lastOutcome == AtlasRouteOutcome.VERIFIED &&
            successes > 0 &&
            rollbackFailures == 0 &&
            bootGeneration > 0L &&
            this.bootGeneration == bootGeneration &&
            this.privilegeGeneration == privilegeGeneration

    /**
     * When this record was last touched, for ageing one record against another.
     *
     * Falls back to the verification stamp for records written before [lastAttemptElapsedMs]
     * existed, and to [Long.MIN_VALUE] for a record that has neither — a legacy quarantine is then
     * the first to yield, never the last. Reading the raw field directly is what made "the oldest
     * quarantine yields" an alphabetical accident.
     */
    fun recencyElapsedMs(): Long = lastAttemptElapsedMs ?: lastVerifiedElapsedMs ?: Long.MIN_VALUE
}

/**
 * Learns which route actually worked on **this** device, and remembers which routes must not be
 * retried.
 *
 * The point is that Atlas does not rediscover the same device every scan: once a route has been
 * verified on this phone, the next attempt can prefer it. Three rules keep that from becoming a
 * liability:
 *
 * 1. **Learning never overrides safety ordering.** The stored preference only breaks ties *inside* one
 *    transport tier (`PLATFORM_HINT` before `VENDOR_BRIDGE` before `ROOT_DAEMON` before
 *    `ARBITER_SYSFS`). A remembered sysfs route can never outrank a platform route.
 * 2. **A rollback failure is remembered as such, and expires with the boot.** Within one boot the
 *    route is quarantined, because the device was left in an unknown state. After a reboot the
 *    evidence is void — the vendor's own state was rebuilt — so it is retried rather than banned
 *    forever.
 * 3. **Nothing here is a claim about a device.** An entry records what *we* observed; it carries no
 *    value, no unit and no capability, so it cannot be promoted into a measurement.
 *
 * The store is bounded and self-validating: an entry this build cannot read is deleted and treated as
 * absent, never half-trusted.
 */
class AtlasRouteMemory(
    private val io: AtlasStoreIo,
    private val clockMs: () -> Long,
    private val bootGeneration: () -> Long = { 0L },
    private val privilegeGeneration: () -> Long = { 0L },
    private val maxEntries: Int = MAX_ENTRIES,
) {

    init {
        require(maxEntries > 0) { "the route memory bound must be positive" }
    }

    /**
     * Every read and write of the store is a read-modify-write over one directory.
     *
     * Two callers in this process — the UI's scan and a background transaction — would otherwise
     * both read the same counters, both add one, and the second write would erase the first. The
     * lock makes one process's updates atomic. It says nothing about a second process: `AppMonitor`
     * and the app share the directory by design (`AtlasRouteMemoryFactory`), and only a file lock
     * could serialize them. Until one exists, the counter a crossed process loses is a counter, not
     * a verdict: quarantine is written with the same operation and a lost update can only keep a
     * route blocked longer, never release it.
     */
    private val lock = Any()

    /** The last route verified on this device for [target], or `null` when there is none. */
    fun preferredRoute(target: String): String? = synchronized(lock) {
        val boot = bootGeneration()
        val privilege = privilegeGeneration()
        entries()
            .filter { it.target == target && it.isVerifiedIn(boot, privilege) }
            .maxByOrNull { it.lastVerifiedElapsedMs ?: Long.MIN_VALUE }
            ?.routeId
    }

    /** Routes that must not be attempted again in this boot generation. */
    fun quarantinedRoutes(target: String): Set<String> = synchronized(lock) {
        val boot = bootGeneration()
        val privilege = privilegeGeneration()
        entries()
            .filter { it.target == target && it.isQuarantinedIn(boot, privilege) }
            .onEach { entry ->
                if (entry.bootGeneration == 0L && boot > 0L) {
                    // Legacy/unknown failures remain blocked now. Anchor them to this known boot so
                    // a later real reboot can release them without a manual memory reset.
                    store(entry.copy(bootGeneration = boot, privilegeGeneration = privilege))
                }
            }
            .map { it.routeId }
            .toSet()
    }

    fun noteVerified(target: String, routeId: String) = synchronized(lock) {
        val entry = currentEntry(target, routeId)
        store(copy(entry, AtlasRouteOutcome.VERIFIED, successes = entry.successes + 1))
    }

    /**
     * Records a failed attempt. [rollbackVerified] is required rather than optional because "we could
     * not restore the baseline" is the one failure that changes what may be attempted next.
     */
    fun noteFailed(target: String, routeId: String, rollbackVerified: Boolean) = synchronized(lock) {
        val entry = currentEntry(target, routeId)
        store(
            copy(
                entry,
                if (rollbackVerified) AtlasRouteOutcome.FAILED else AtlasRouteOutcome.ROLLBACK_FAILED,
                failures = entry.failures + 1,
                rollbackFailures = entry.rollbackFailures + if (rollbackVerified) 0 else 1,
            ),
        )
    }

    /**
     * Drops one target's memory. Used when a device config change invalidates what was learned.
     *
     * The match is on the decoded target, never on the file name: `slug` is lossy on purpose (it
     * lowercases and folds punctuation), and a `contains` over the encoded name would also drop
     * `cpu` when asked to forget `cpu_frequency`, or `cpu_frequency` when asked to forget `cpu`.
     * An entry that cannot be decoded is not this target's memory and is left for [pruneIfNeeded]
     * and the corrupt-entry path to remove.
     */
    fun forget(target: String): Int = synchronized(lock) {
        entries()
            .filter { it.target == target }
            .count { entry -> io.delete(nameOf(entry.target, entry.routeId)) }
    }

    fun clear(): Int = synchronized(lock) {
        io.list().count { it.startsWith(NAME_PREFIX) && it.endsWith(NAME_SUFFIX) && io.delete(it) }
    }

    fun entries(): List<AtlasRouteMemoryEntry> = io.list()
        .filter { it.startsWith(NAME_PREFIX) && it.endsWith(NAME_SUFFIX) }
        .mapNotNull { name -> io.read(name)?.let(::decode) }
        .sortedWith(compareBy({ it.target }, { it.routeId }))

    // ---- helpers -----------------------------------------------------------------------------------

    private fun currentEntry(target: String, routeId: String): AtlasRouteMemoryEntry {
        val boot = bootGeneration()
        val privilege = privilegeGeneration()
        val previous = load(target, routeId)
        if (previous != null && (
                (previous.bootGeneration == boot && previous.privilegeGeneration == privilege) ||
                    previous.isQuarantinedIn(boot, privilege)
            )) {
            return previous.copy(bootGeneration = boot, privilegeGeneration = privilege)
        }
        // Counters describe one generation. Copying an old rollback failure here would quarantine
        // the route again immediately after its first successful recovery in the new boot.
        return AtlasRouteMemoryEntry(
            target = target,
            routeId = routeId,
            lastOutcome = AtlasRouteOutcome.FAILED,
            lastVerifiedElapsedMs = null,
            bootGeneration = boot,
            privilegeGeneration = privilege,
            successes = 0,
            failures = 0,
            rollbackFailures = 0,
        )
    }

    private fun copy(
        entry: AtlasRouteMemoryEntry,
        outcome: AtlasRouteOutcome,
        successes: Int = entry.successes,
        failures: Int = entry.failures,
        rollbackFailures: Int = entry.rollbackFailures,
    ): AtlasRouteMemoryEntry {
        // One reading for the whole update: a verification is also an attempt, and both stamps
        // describing the same event must not disagree.
        val now = clockMs()
        return entry.copy(
            lastOutcome = outcome,
            lastVerifiedElapsedMs =
                if (outcome == AtlasRouteOutcome.VERIFIED) now else entry.lastVerifiedElapsedMs,
            // Stamped for every outcome: this is what orders quarantines against each other.
            lastAttemptElapsedMs = now,
            // currentEntry captured one context for the whole update, including counter reset.
            successes = successes,
            failures = failures,
            rollbackFailures = rollbackFailures,
        )
    }

    private fun load(target: String, routeId: String): AtlasRouteMemoryEntry? {
        val text = io.read(nameOf(target, routeId)) ?: return null
        val decoded = decode(text) ?: run {
            io.delete(nameOf(target, routeId))
            return null
        }
        return decoded.takeIf { it.target == target && it.routeId == routeId }
            ?: run {
                io.delete(nameOf(target, routeId))
                null
            }
    }

    private fun store(entry: AtlasRouteMemoryEntry) {
        if (!pruneIfNeeded(entry)) return
        io.write(nameOf(entry.target, entry.routeId), encode(entry))
    }

    /**
     * Keeps the store bounded, and **never buys one entry's room with a quarantine**.
     *
     * The bound used to be enforced by deleting every other name once the store was full, which
     * meant that learning one new route silently wiped every learned route and — the part that
     * matters — every quarantine. A quarantine is the record that a route left the device in an
     * unknown physical state in this boot; dropping it does not make the device safe, it makes the
     * route eligible again, so the retry the quarantine existed to prevent would happen.
     *
     * Four ordered steps instead:
     *
     * 1. **Undecodable files go first.** They carry no learning at all, and because [entries] skips
     *    them they would otherwise occupy capacity forever without ever appearing in a report.
     * 2. **The cheapest-to-lose entries go next**, ranked: a failure that was never verified is
     *    worth less than a route verified in an older generation, which is worth less than one
     *    verified in this generation.
     * 3. **A quarantined entry is not evicted for a mere counter**, so a full store of quarantines
     *    makes an ordinary update fail rather than forget one.
     * 4. **An incoming quarantine still gets in.** Step 3 must not turn "we could not restore the
     *    baseline" into a discarded fact, so when the entry being written is itself a quarantine and
     *    the store is full of them, the oldest quarantine yields to the newest — the newest is the one
     *    describing the attempt that just left the device in an unknown state.
     *
     * @return `true` when there is room to write [entry], `false` when the store refused.
     */
    private fun pruneIfNeeded(entry: AtlasRouteMemoryEntry): Boolean {
        val keep = nameOf(entry.target, entry.routeId)
        fun present(): List<String> =
            io.list().filter { it.startsWith(NAME_PREFIX) && it.endsWith(NAME_SUFFIX) }

        val current = present()
        // Refreshing an existing entry consumes no capacity and must not evict anything.
        if (keep in current || current.size < maxEntries) return true

        val boot = bootGeneration()
        val privilege = privilegeGeneration()

        val readable = current.mapNotNull { name -> io.read(name)?.let { name to decode(it) } }
            .mapNotNull { (name, decoded) -> decoded?.let { name to it } }
            .toMap()
        current.filter { it !in readable }.forEach { io.delete(it) }
        var remaining = present()
        if (remaining.size < maxEntries) return true

        // Ascending value: the first deleted is the record that would be least missed.
        val evictable = remaining
            .mapNotNull { name -> readable[name]?.let { name to it } }
            .filterNot { (_, known) -> known.isQuarantinedIn(boot, privilege) }
            .sortedWith(
                compareBy(
                    { (_, known) -> evictionValue(known, boot, privilege) },
                    { (_, known) -> known.recencyElapsedMs() },
                    { (name, _) -> name },
                ),
            )

        for ((name, _) in evictable) {
            if (remaining.size < maxEntries) break
            if (io.delete(name)) remaining = remaining - name
        }
        if (remaining.size < maxEntries) return true

        // The incoming fact is a quarantine and every slot holds one: the newest must replace the
        // oldest, because only the newest describes the state the device is in now.
        val incomingIsQuarantine = entry.isQuarantinedIn(boot, privilege)
        if (!incomingIsQuarantine) return false
        val oldest = remaining
            .mapNotNull { name -> readable[name]?.let { name to it } }
            .filter { (_, known) -> known.isQuarantinedIn(boot, privilege) }
            .minWithOrNull(
                compareBy(
                    { (_, known) -> known.recencyElapsedMs() },
                    { (name, _) -> name },
                ),
            )
            ?: return false
        return io.delete(oldest.first)
    }

    /** Lower is cheaper to lose. Quarantine is not rankable: [pruneIfNeeded] removes it from the list. */
    private fun evictionValue(entry: AtlasRouteMemoryEntry, boot: Long, privilege: Long): Int = when {
        entry.isVerifiedIn(boot, privilege) -> 3
        entry.successes > 0 || entry.lastOutcome == AtlasRouteOutcome.VERIFIED -> 2
        else -> 1
    }

    // ---- explicit-shape serialization ---------------------------------------------------------------

    private fun encode(entry: AtlasRouteMemoryEntry): String {
        val root = JSONObject()
        root.put(KEY_SCHEMA, SCHEMA)
        root.put(KEY_TARGET, entry.target)
        root.put(KEY_ROUTE, entry.routeId)
        root.put(KEY_OUTCOME, entry.lastOutcome.name)
        root.put(KEY_VERIFIED_AT, entry.lastVerifiedElapsedMs ?: JSONObject.NULL)
        root.put(KEY_ATTEMPTED_AT, entry.lastAttemptElapsedMs ?: JSONObject.NULL)
        root.put(KEY_BOOT, entry.bootGeneration)
        root.put(KEY_PRIVILEGE, entry.privilegeGeneration)
        root.put(KEY_SUCCESSES, entry.successes)
        root.put(KEY_FAILURES, entry.failures)
        root.put(KEY_ROLLBACK_FAILURES, entry.rollbackFailures)
        return root.toString()
    }

    private fun decode(text: String): AtlasRouteMemoryEntry? {
        if (text.length > MAX_ENTRY_CHARS) return null
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return null
        if (root.optInt(KEY_SCHEMA, -1) != SCHEMA) return null
        if (!REQUIRED_KEYS.all { root.has(it) }) return null
        return runCatching {
            AtlasRouteMemoryEntry(
                target = root.getString(KEY_TARGET),
                routeId = root.getString(KEY_ROUTE),
                lastOutcome = AtlasRouteOutcome.valueOf(root.getString(KEY_OUTCOME)),
                lastVerifiedElapsedMs = if (root.isNull(KEY_VERIFIED_AT)) null else root.getLong(KEY_VERIFIED_AT),
                // Optional on purpose: a record written before this key existed must still load, or
                // upgrading the app would drop every quarantine and make blocked routes retryable.
                lastAttemptElapsedMs = if (!root.has(KEY_ATTEMPTED_AT) || root.isNull(KEY_ATTEMPTED_AT)) {
                    null
                } else {
                    root.getLong(KEY_ATTEMPTED_AT)
                },
                bootGeneration = root.getLong(KEY_BOOT),
                privilegeGeneration = root.getLong(KEY_PRIVILEGE),
                successes = root.getInt(KEY_SUCCESSES).coerceAtLeast(0),
                failures = root.getInt(KEY_FAILURES).coerceAtLeast(0),
                rollbackFailures = root.getInt(KEY_ROLLBACK_FAILURES).coerceAtLeast(0),
            )
        }.getOrNull()
    }

    private fun nameOf(target: String, routeId: String): String =
        "$NAME_PREFIX${slug(target)}$NAME_SEPARATOR${slug(routeId)}$NAME_SUFFIX"

    /** Entry names are generated here, so an unusual target cannot name a file outside this store. */
    private fun slug(value: String): String {
        val cleaned = value.lowercase().map { character ->
            if (character.isLetterOrDigit() || character == '.' || character == '-' || character == '_') {
                character
            } else {
                '-'
            }
        }.joinToString("").trim('-', '.').replace(Regex("-{2,}"), "-")
        return cleaned.take(MAX_SLUG_CHARS).ifEmpty { "unnamed" }
    }

    companion object {
        const val SCHEMA: Int = 1

        /**
         * A stable token for a validated kernel boot UUID; zero means unknown, including legacy records.
         * A digest collision can only retain quarantine too long, never release it inside one boot.
         * The raw boot identity is neither stored nor reported.
         */
        fun generationForBootId(bootId: String?): Long {
            val normalized = bootId?.trim()?.lowercase(Locale.ROOT) ?: return 0L
            if (!BOOT_UUID.matches(normalized) || normalized == "00000000-0000-0000-0000-000000000000") return 0L
            val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.US_ASCII))
            return (ByteBuffer.wrap(digest).long and Long.MAX_VALUE).coerceAtLeast(1L)
        }

        const val NAME_PREFIX: String = "route."
        const val NAME_SUFFIX: String = ".json"
        const val NAME_SEPARATOR: String = "."

        private const val MAX_ENTRIES = 64
        private const val MAX_ENTRY_CHARS = 2 * 1024
        private const val MAX_SLUG_CHARS = 48

        private val BOOT_UUID = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

        private const val KEY_SCHEMA = "schema"
        private const val KEY_TARGET = "target"
        private const val KEY_ROUTE = "route"
        private const val KEY_OUTCOME = "outcome"
        private const val KEY_VERIFIED_AT = "verifiedAt"
        private const val KEY_ATTEMPTED_AT = "attemptedAt"
        private const val KEY_BOOT = "boot"
        private const val KEY_PRIVILEGE = "privilege"
        private const val KEY_SUCCESSES = "successes"
        private const val KEY_FAILURES = "failures"
        private const val KEY_ROLLBACK_FAILURES = "rollbackFailures"

        private val REQUIRED_KEYS: List<String> = listOf(
            KEY_SCHEMA,
            KEY_TARGET,
            KEY_ROUTE,
            KEY_OUTCOME,
            KEY_VERIFIED_AT,
            KEY_BOOT,
            KEY_PRIVILEGE,
            KEY_SUCCESSES,
            KEY_FAILURES,
            KEY_ROLLBACK_FAILURES,
        )
    }
}
