/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.atlas

/**
 * The quirk base (`P11`): what a device family is **known** to do, as reviewed data rather than as
 * conditionals scattered through the shared code.
 *
 * This is the answer to the complaint that started the whole track — "a fix works on one phone and
 * breaks another". Every such fix so far has been a branch somewhere in shared code, and each branch is
 * invisible to every device it was not written for. A quirk moves that knowledge out of the control flow
 * and into an entry with a provenance, a revision and a *bounded* effect.
 *
 * Four rules, and the fourth is the one that makes the rest safe:
 *
 * 1. **Provenance is part of the type.** An entry without a source and an evidence link is not a quirk,
 *    it is a rumour with a name — so [AtlasQuirk] refuses to exist without both. Most of the entries in
 *    [AtlasQuirkBase] cite a refusal measured in a real log bundle; the rest cite a reviewed platform
 *    policy. None cite an impression.
 * 2. **Matching is explicit and narrow.** A quirk names the SoC models, API range, kernel prefix or path
 *    fragments it applies to, and refuses to exist if it names nothing (a rule that matches everything
 *    is a global change wearing a table's clothes).
 * 3. **A quirk cannot remove coverage.** It never suppresses an attempt: a measured refusal on one
 *    device is not proof that the interface is unreachable on another, and the app has root-read layers
 *    precisely because the app context is not the only reader. Only a reviewed *platform* closure
 *    ([AtlasClosedSurfaces]) may stop an attempt, because that one is a statement about Android rather
 *    than about one phone.
 * 4. **An effect can only lower a claim, and this is enforced by the shape of the type.** Every member of
 *    [AtlasQuirkEffect] contributes at most [AtlasAvailability.DEVICE_DEPENDENT]; there is no member that
 *    can contribute [AtlasAvailability.EXPECTED] and none that can contribute
 *    [AtlasAvailability.EXPECTED_DENIED]. "Prefer another source" and "the unit is not established here"
 *    are restrictions; "this is granted" and "this is closed" are not expressible. So a quirk can make a
 *    reading *less* trusted and can never make one *more* trusted, whatever a future entry looks like —
 *    the type refuses the mistake rather than a review having to catch it.
 *
 * One measured phenomenon is deliberately **not** an effect here: a node that answers with whole-device
 * state rather than the field that was written (the `perfserv_freq` shape, which produced the bulk of one
 * bundle's false verdicts). It is already implemented, with its own measured derivation, in
 * [nd.max.core.hardware.WriteVerification.comparePowerhalField]. Expressing it a second time as data would
 * create the parallel mechanism this project keeps refusing; when it becomes data it must *replace* that
 * call site, which is a separate, evidence-driven change.
 */

/**
 * Which devices a quirk applies to. At least one criterion is required, and every criterion that is
 * present must match — a quirk is a conjunction, never a list of alternatives.
 */
data class AtlasQuirkMatch(
    /** SoC models as the platform declares them, compared case-insensitively. */
    val socModels: Set<String> = emptySet(),
    /** A prefix of the kernel release string, e.g. `4.19` or `6.1.` */
    val kernelPrefix: String? = null,
    /** API levels this applies to. `null` means "any". */
    val apiLevels: IntRange? = null,
    /** Substrings that must appear in the interface path. */
    val pathFragments: Set<String> = emptySet(),
) {
    init {
        require(socModels.isNotEmpty() || kernelPrefix != null || apiLevels != null || pathFragments.isNotEmpty()) {
            "a quirk that names nothing applies to everything, which is not a quirk"
        }
        require(socModels.none { it.isBlank() }) { "an SoC model is a name, not a blank" }
        require(kernelPrefix == null || kernelPrefix.isNotBlank()) { "a kernel prefix is not blank" }
        require(pathFragments.none { it.isBlank() }) { "a path fragment is not blank" }
    }

    /** True when this quirk's criteria all hold for the given device and interface. */
    fun matches(identity: AtlasDeviceIdentity, path: String): Boolean {
        if (socModels.isNotEmpty()) {
            val model = identity.socModel?.lowercase() ?: return false
            if (socModels.none { it.lowercase() == model }) return false
        }
        val release = identity.kernelRelease
        val prefix = kernelPrefix
        if (prefix != null && (release == null || !release.startsWith(prefix))) return false
        val levels = apiLevels
        if (levels != null && identity.apiLevel !in levels) return false
        if (pathFragments.isNotEmpty() && pathFragments.none { it in path }) return false
        return true
    }
}

/**
 * What a quirk does. Every member is a restriction, and the type cannot express a promotion.
 *
 * [contributes] is the availability a matched effect may add to the fold. It is fixed at
 * [AtlasAvailability.DEVICE_DEPENDENT] for the whole type — the interface has one implementation of the
 * property and it is not overridable per member, so no future effect can quietly become a grant or a
 * closure without changing this file *and* failing the test that pins it.
 */
sealed interface AtlasQuirkEffect {

    /** Why the effect is here, in the words a report needs. Never blank. */
    val reason: String

    /** What this effect may contribute to the availability axis: never a stronger claim than this. */
    val contributes: AtlasAvailability

    /**
     * Expect a refusal here on these devices.
     *
     * An expectation, not a closure: the attempt still happens, so a device where the refusal does not
     * occur still gets its reading and nothing is lost.
     */
    data class ExpectDenied(override val reason: String) : AtlasQuirkEffect {
        init { require(reason.isNotBlank()) { "an expectation says why" } }
        override val contributes: AtlasAvailability = AtlasAvailability.DEVICE_DEPENDENT
    }

    /** Expect the interface not to exist on these devices. Absence is unsurprising, not a finding. */
    data class ExpectAbsent(override val reason: String) : AtlasQuirkEffect {
        init { require(reason.isNotBlank()) { "an expectation says why" } }
        override val contributes: AtlasAvailability = AtlasAvailability.DEVICE_DEPENDENT
    }

    /**
     * The unit is not established on these devices.
     *
     * The honest verdict for a value whose scale this project has not proven: the number is reported
     * with its raw text and **no** converted unit, which is the only reading that does not invent a
     * divisor.
     */
    data class UnitUnestablished(override val reason: String) : AtlasQuirkEffect {
        init { require(reason.isNotBlank()) { "an unestablished unit says why" } }
        override val contributes: AtlasAvailability = AtlasAvailability.DEVICE_DEPENDENT
    }

    /** Answer this interface from the named source in preference to the others. A preference, not a grant. */
    data class PreferSource(val sourceId: String, override val reason: String) : AtlasQuirkEffect {
        init {
            require(sourceId.isNotBlank()) { "a preferred source is named" }
            require(reason.isNotBlank()) { "a preference says why" }
        }

        override val contributes: AtlasAvailability = AtlasAvailability.DEVICE_DEPENDENT
    }
}

/**
 * One reviewed quirk.
 *
 * [revision] exists so an entry can be *changed* rather than re-interpreted: a report quotes the
 * revision it used, and a later change to the effect is visible as a change of revision instead of as an
 * unexplained difference between two runs.
 */
data class AtlasQuirk(
    val id: String,
    val match: AtlasQuirkMatch,
    val effect: AtlasQuirkEffect,
    val sourceId: String,
    val evidenceLink: String,
    val revision: Int = 1,
) {
    init {
        require(ID.matches(id)) { "a quirk id is a stable slug: $id" }
        require(sourceId.isNotBlank()) { "a quirk cites its source" }
        require(evidenceLink.isNotBlank()) { "a quirk cites its evidence" }
        require(revision >= 1) { "a quirk revision starts at 1" }
    }

    companion object {
        private val ID = Regex("^[a-z][a-z0-9-]{2,47}$")
    }
}

/**
 * The reviewed entries.
 *
 * Every entry here came from a measurement that is already recorded in this repository: the refusals
 * counted in a real log bundle, or a reviewed AOSP policy. The distinction is carried in [AtlasQuirk.sourceId]
 * and spelled out in the evidence line, because "one phone answered this" and "Android says this" are
 * different strengths of claim and the difference must survive into a report.
 *
 * Nothing here is inferred from a name, and no entry was added because a device *sounded* similar.
 */
object AtlasQuirkBase {

    /**
     * The device refusals measured in `MaxManager_Logs_20261014_202637.tar.gz` (Xiaomi manet · SM8650 ·
     * build `343-2bd677c`), recorded in HANDOFF `GPU-DEVFREQ-BLIND-01`: 40 refusals for the app's
     * `u:r:untrusted_app` context while the root companion was running.
     */
    private const val MANET_EVIDENCE =
        "HANDOFF GPU-DEVFREQ-BLIND-01: 40 avc denials measured for the app context " +
            "(manet/SM8650, build 343-2bd677c)"

    val ALL: List<AtlasQuirk> = listOf(
        AtlasQuirk(
            id = "devfreq-search-denied-app-context",
            match = AtlasQuirkMatch(
                socModels = setOf("sm8650"),
                pathFragments = setOf("/sys/class/devfreq"),
            ),
            effect = AtlasQuirkEffect.ExpectDenied(
                "the app SELinux context cannot even search /sys/class/devfreq on this ROM, so a " +
                    "listing failure here is the context and not the device — the root companion is " +
                    "what answers",
            ),
            sourceId = "S18b",
            evidenceLink = MANET_EVIDENCE,
        ),
        AtlasQuirk(
            id = "power-supply-search-denied-app-context",
            match = AtlasQuirkMatch(
                socModels = setOf("sm8650"),
                pathFragments = setOf("/sys/class/power_supply"),
            ),
            effect = AtlasQuirkEffect.ExpectDenied(
                "the app context is refused directory search under /sys/class/power_supply on this " +
                    "ROM, so the battery nodes are read through the authorized path or not at all",
            ),
            sourceId = "S18b",
            evidenceLink = MANET_EVIDENCE,
        ),
        AtlasQuirk(
            id = "gpu-busy-percentage-unreadable-app-context",
            match = AtlasQuirkMatch(
                socModels = setOf("sm8650"),
                pathFragments = setOf("gpu_busy_percentage"),
            ),
            effect = AtlasQuirkEffect.ExpectDenied(
                "GPU busy percentage is refused to the app context on this ROM, so an absent reading " +
                    "there says nothing about the GPU",
            ),
            sourceId = "S18b",
            evidenceLink = MANET_EVIDENCE,
        ),
        AtlasQuirk(
            id = "soc-machine-node-denied-app-context",
            match = AtlasQuirkMatch(
                socModels = setOf("sm8650"),
                pathFragments = setOf("/sys/devices/soc0/machine"),
            ),
            effect = AtlasQuirkEffect.ExpectDenied(
                "the SoC identity node is refused under this context, which is why the declared " +
                    "platform identity is the primary source and this file is only a cross-check",
            ),
            sourceId = "S18b",
            evidenceLink = MANET_EVIDENCE,
        ),
    )

    /**
     * The quirks that apply to one interface on one device, in a deterministic order.
     *
     * Order is by id, not by declaration, so a report's list is comparable between runs and the
     * addition of an entry cannot reshuffle the output of the others.
     */
    fun matched(identity: AtlasDeviceIdentity, path: String): List<AtlasQuirk> =
        ALL.filter { it.match.matches(identity, path) }.sortedBy { it.id }

    /**
     * The availability the reviewed knowledge supports for this path, given the base tier.
     *
     * The result is the **strictest** of the base tier and the reviewed platform closure — the same
     * minimum the axis itself uses — so this function can only ever lower the base. A quirk deliberately
     * does not appear here: [matched] is quoted as an explanation, and an explanation must not be able to
     * change what the app is willing to read.
     */
    fun availabilityOf(path: String, base: AtlasAvailability = AtlasAvailability.DEVICE_DEPENDENT): AtlasAvailability =
        AtlasAvailability.strictestOf(base, AtlasClosedSurfaces.availabilityOf(path) ?: base)

    /**
     * One line per applicable quirk, for a report.
     *
     * `id@revision effect reason` and nothing else — no path, no value, no device identifier. A quirk line
     * is knowledge about a family, and a family is not a person.
     */
    fun annotationsFor(identity: AtlasDeviceIdentity, path: String): List<String> =
        matched(identity, path).map { quirk ->
            val kind = when (val effect = quirk.effect) {
                is AtlasQuirkEffect.ExpectDenied -> "expect-denied"
                is AtlasQuirkEffect.ExpectAbsent -> "expect-absent"
                is AtlasQuirkEffect.UnitUnestablished -> "unit-unestablished"
                is AtlasQuirkEffect.PreferSource -> "prefer-source:${effect.sourceId}"
            }
            "${quirk.id}@${quirk.revision} $kind"
        }
}
