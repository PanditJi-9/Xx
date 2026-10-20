/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.atlas

/**
 * How surprising a refusal on this interface is — the `EXPECTED` / `DEVICE_DEPENDENT` / `EXPECTED_DENIED`
 * axis from the gap review (`G-09`, `I-02`).
 *
 * This axis exists because the same event means two completely different things depending on where it
 * happens. A refusal on an interface **every** Android build grants is a finding: something changed, and
 * it is worth a report. A refusal on an interface the platform **never** grants an app is the platform
 * working as designed, and treating it as a defect is how a support loop opens over a non-event — the
 * mistake `AtlasDoctor` already guards against for the candidate bank, applied here to whole families of
 * interfaces.
 *
 * Three rules keep the axis honest:
 *
 * 1. **It lowers a claim and never raises one.** The fold is a minimum over contributions
 *    ([strictestOf]), so no quirk, profile or catalog entry can move an interface *up* this axis. A
 *    reading is evidence; this is a prior about the reading, and a prior never outweighs evidence.
 * 2. **`EXPECTED` is a strong word, so almost nothing gets it.** It means the platform grants this to
 *    every app, which today is true of exactly the surfaces AOSP grants in `domain.te`/`app.te`. A
 *    vendor interface on some ROMs is [DEVICE_DEPENDENT], which is the truthful answer.
 * 3. **A closed surface cannot be `EXPECTED`.** [AtlasClosedSurfaces] is the reviewed list of families
 *    the platform denies to untrusted apps; [strictestOf] refuses to let anything claim `EXPECTED` about
 *    them, and the refusal is a type-level fact rather than a lint rule (see `AtlasQuirkTest`).
 *
 * Nothing here reads a device. It is a classification of *our* expectations, which is why it is
 * vocabulary and not part of the read path.
 */
enum class AtlasAvailability {
    /**
     * The platform grants this to an ordinary app, so a refusal is a finding.
     *
     * The strongest claim on the axis, and the rarest. Reachable only for surfaces with a reviewed
     * platform grant.
     */
    EXPECTED,

    /**
     * Some devices expose it and some do not — vendor interfaces, optional kernel subsystems, anything
     * whose existence is a property of a ROM rather than of Android.
     *
     * The default, and deliberately so: an unknown device lands here, which means nothing is refused
     * pre-emptively on the strength of not being recognised. Being unknown must never cost coverage.
     */
    DEVICE_DEPENDENT,

    /**
     * The platform denies this to an application, on every device, by policy.
     *
     * A refusal here is expected, so it is not a finding and not worth a support report. It is still
     * *recorded* — the report names the rule and its source — because "we did not look" and "we were
     * never allowed to look" are different facts, and only one of them belongs in a coverage number.
     */
    EXPECTED_DENIED,
    ;

    /** Higher is a stronger claim. [strictestOf] compares these, never the names. */
    val claim: Int get() = when (this) {
        EXPECTED -> 2
        DEVICE_DEPENDENT -> 1
        EXPECTED_DENIED -> 0
    }

    companion object {
        /**
         * The weakest claim among the inputs.
         *
         * It is a minimum because this axis can only ever be lowered: a fold that could raise a claim
         * would let a table of assumptions override a measurement, which is the one thing this vocabulary
         * exists to prevent.
         */
        fun strictestOf(vararg values: AtlasAvailability): AtlasAvailability =
            values.minByOrNull { it.claim } ?: DEVICE_DEPENDENT
    }
}

/**
 * One family of interfaces the platform closes to an application, with the policy that closes it.
 *
 * The data is from the gap review's section 3 (`R-01`…`R-09`), whose sources are AOSP
 * `system/sepolicy` blobs already recorded there. Nothing was added from memory: each entry carries the
 * rule id and the evidence line, so a maintainer can re-check one entry without re-reading the review.
 */
data class AtlasClosedSurface(
    /** Stable rule id (`R-04`). A report quotes this and nothing else. */
    val id: String,
    /** A path prefix that identifies the family. Matched as a prefix of an absolute path. */
    val prefix: String,
    /** Why the platform closes it, in the words a reader needs. */
    val reason: String,
    /** Where the closure was reviewed. Never blank: an unprovenanced rule is a rumour. */
    val evidence: String,
) {
    init {
        require(id.isNotBlank()) { "a closed surface needs its rule id" }
        require(prefix.startsWith("/")) { "a closed surface is a path family: $prefix" }
        require(reason.isNotBlank()) { "a closed surface says why it is closed" }
        require(evidence.isNotBlank()) { "a closed surface cites where it was reviewed" }
    }

    /** True when [path] is inside this family. Family membership, not an exact path. */
    fun covers(path: String): Boolean = path == prefix || path.startsWith("$prefix/")
}

/**
 * The reviewed closed families.
 *
 * Only families that are **path-shaped** are here. Several entries of the gap review are not: a netlink
 * subscription (`R-06`), a sysfs write (`R-08`) and a whole permission class (`R-03`) are not prefixes,
 * and inventing a prefix for them would manufacture a match that the review never established. Those
 * stay documented in the review and out of this list, because a rule that matches the wrong thing is
 * worse than a rule that is missing.
 */
object AtlasClosedSurfaces {

    val ALL: List<AtlasClosedSurface> = listOf(
        AtlasClosedSurface(
            id = "R-01",
            prefix = "/sys/fs/cgroup",
            reason = "cgroup files are neverallows for untrusted apps, so no per-app pressure or " +
                "memory.stat reading exists for an ordinary application",
            evidence = "AOSP private/app_neverallows.te (blob 434fb132e7ad) — all_untrusted_apps cgroup:file *",
        ),
        AtlasClosedSurface(
            id = "R-02",
            prefix = "/sys/kernel/debug",
            reason = "debugfs is denied for reading to untrusted apps; only the trace marker is writable",
            evidence = "AOSP private/app_neverallows.te (blob 434fb132e7ad) — { debugfs_type -debugfs_kcov }:file read",
        ),
        AtlasClosedSurface(
            id = "R-04",
            prefix = "/proc/stat",
            reason = "the generic /proc labels are neverallows; a device-specific label is required and " +
                "the app has none",
            evidence = "AOSP private/app_neverallows.te (blob 434fb132e7ad) — proc_stat:file *",
        ),
        AtlasClosedSurface(
            id = "R-04",
            prefix = "/proc/uptime",
            reason = "closed with the rest of the generic /proc summaries",
            evidence = "AOSP private/app_neverallows.te (blob 434fb132e7ad) — proc_uptime:file *",
        ),
        AtlasClosedSurface(
            id = "R-04",
            prefix = "/proc/version",
            reason = "closed to apps; the kernel release comes from the platform API instead",
            evidence = "AOSP private/app_neverallows.te (blob 434fb132e7ad) — proc_version:file *",
        ),
        AtlasClosedSurface(
            id = "R-04",
            prefix = "/proc/vmstat",
            reason = "closed with the rest of the generic /proc summaries",
            evidence = "AOSP private/app_neverallows.te (blob 434fb132e7ad) — proc_vmstat:file *",
        ),
        AtlasClosedSurface(
            id = "R-04",
            prefix = "/proc/swaps",
            reason = "swap accounting is not readable by an app; there is no public replacement for it",
            evidence = "AOSP private/app_neverallows.te (blob 434fb132e7ad) — proc_swaps:file *",
        ),
        AtlasClosedSurface(
            id = "R-04",
            prefix = "/proc/loadavg",
            reason = "closed to apps; process-level information needs the activity manager instead",
            evidence = "AOSP private/app_neverallows.te (blob 434fb132e7ad) — proc_loadavg:file *",
        ),
        AtlasClosedSurface(
            id = "R-05",
            prefix = "/sys/fs/selinux",
            reason = "the SELinux state cannot be read by an app; a permissive state is inferred from the " +
                "pattern of refusals, never observed here",
            evidence = "AOSP private/app_neverallows.te (blob 434fb132e7ad) — selinuxfs:file no_rw_file_perms",
        ),
    )

    /** The family [path] belongs to, or `null` when it is not a closed surface. */
    fun match(path: String): AtlasClosedSurface? = ALL.firstOrNull { it.covers(path) }

    /** True when the platform closes this path to an application on every device. */
    fun covers(path: String): Boolean = match(path) != null

    /**
     * The availability the reviewed closure implies for this path, or `null` when the platform says
     * nothing about it.
     *
     * A `null` answer is the important one: it means the question is open, and an open question is
     * answered by reading the device, not by a table.
     */
    fun availabilityOf(path: String): AtlasAvailability? =
        if (covers(path)) AtlasAvailability.EXPECTED_DENIED else null
}
