/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.atlas

import nd.max.core.atlas.support.AtlasSourceGuard
import nd.max.core.hardware.AtlasReadBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The quirk base's whole safety argument is that it can only ever *lower* a claim, so these tests attack
 * that argument from four directions rather than checking a behaviour: the shape of the effect type, the
 * source text of the file that declares it, the fold that combines tiers, and the one place a closure is
 * allowed to stop a probe.
 *
 * There is also one guard that has no trigger today and is asserted anyway: **no catalog entry may address
 * a closed family**. The community bank is accumulated vocabulary, so an entry naming `/proc/stat` could
 * arrive later, be attempted on every pass, and add an `avc: denied` to a device's system log every time.
 * A test that fails at review is cheaper than a device that pays for it.
 */
class AtlasQuirkTest {

    private val manet = AtlasDeviceIdentity(
        socManufacturer = "Qualcomm",
        socModel = "SM8650",
        hardware = "qcom",
        board = "kalama",
        supportedAbis = listOf("arm64-v8a"),
        apiLevel = 35,
        kernelRelease = "6.1.25-android14-11-g1234abcd",
    )

    private val unknown = AtlasDeviceIdentity(
        supportedAbis = listOf("arm64-v8a"),
        apiLevel = 33,
        kernelRelease = null,
    )

    // ---- the effect type can only lower -----------------------------------------------------------------

    @Test
    fun `every effect contributes at most the device-dependent tier`() {
        val effects: List<AtlasQuirkEffect> = listOf(
            AtlasQuirkEffect.ExpectDenied("denied here"),
            AtlasQuirkEffect.ExpectAbsent("absent here"),
            AtlasQuirkEffect.UnitUnestablished("unit unproven here"),
            AtlasQuirkEffect.PreferSource("another-bank", "better evidence"),
        )

        effects.forEach { effect ->
            // The `when` is exhaustive on purpose: a fifth effect cannot be added without this test
            // failing to compile, which forces whoever adds it to state what it may claim.
            val described = when (effect) {
                is AtlasQuirkEffect.ExpectDenied -> "deny"
                is AtlasQuirkEffect.ExpectAbsent -> "absent"
                is AtlasQuirkEffect.UnitUnestablished -> "unit"
                is AtlasQuirkEffect.PreferSource -> "prefer"
            }
            assertTrue(described.isNotBlank())
            assertEquals(
                "an effect must not be able to raise a claim above device-dependent",
                AtlasAvailability.DEVICE_DEPENDENT.claim,
                effect.contributes.claim,
            )
        }
    }

    @Test
    fun `the quirk source cannot name the tiers a quirk may not reach`() {
        // Comments are stripped by the guard, so the file is free to *explain* which tiers it refuses to
        // reach; what it must not do is name them in code. This is the same guard shape the read path uses.
        val code = AtlasSourceGuard.code("src/main/java/nd/max/core/atlas/AtlasQuirk.kt")

        assertTrue("a quirk must not be able to claim a platform grant in code", !code.contains("EXPECTED"))
        assertTrue("and it declares an effect type", code.contains("sealed interface AtlasQuirkEffect"))
    }

    @Test
    fun `the strictest tier wins and a grant can never override a closure`() {
        assertEquals(
            AtlasAvailability.EXPECTED_DENIED,
            AtlasAvailability.strictestOf(AtlasAvailability.EXPECTED, AtlasAvailability.EXPECTED_DENIED),
        )
        assertEquals(
            AtlasAvailability.DEVICE_DEPENDENT,
            AtlasAvailability.strictestOf(AtlasAvailability.EXPECTED, AtlasAvailability.DEVICE_DEPENDENT),
        )
        assertEquals(
            "no contribution means the default expectation, never a grant",
            AtlasAvailability.DEVICE_DEPENDENT,
            AtlasAvailability.strictestOf(),
        )
    }

    // ---- a quirk explains and never suppresses ----------------------------------------------------------

    @Test
    fun `a measured quirk changes no policy, only the explanation`() {
        val path = "/sys/class/devfreq/gpu/cur_freq"

        val matched = AtlasQuirkBase.matched(manet, path)
        assertTrue("the measured refusal is known for this device", matched.isNotEmpty())
        assertTrue(matched.all { it.evidenceLink.isNotBlank() })
        assertEquals(
            "a refusal measured on one phone must not become a closure that removes coverage",
            AtlasAvailability.DEVICE_DEPENDENT,
            AtlasQuirkBase.availabilityOf(path),
        )
    }

    @Test
    fun `an unknown device matches no quirk and keeps every generic interface`() {
        assertTrue(AtlasQuirkBase.matched(unknown, "/sys/class/devfreq/gpu/cur_freq").isEmpty())
        assertEquals(AtlasAvailability.DEVICE_DEPENDENT, AtlasQuirkBase.availabilityOf("/sys/class/thermal/thermal_zone0/temp"))
        assertTrue(AtlasQuirkBase.annotationsFor(unknown, "/proc/pressure/memory").isEmpty())
    }

    @Test
    fun `an annotation is knowledge about a family, not about a machine`() {
        val lines = AtlasQuirkBase.annotationsFor(manet, "/sys/class/devfreq/gpu/cur_freq")

        assertTrue(lines.isNotEmpty())
        assertTrue(lines.all { it.contains("@") })
        assertTrue("no path, no value and no device identifier belongs in an annotation", lines.none { it.contains("/sys") })
        assertEquals("and the order is stable", lines, AtlasQuirkBase.annotationsFor(manet, "/sys/class/devfreq/gpu/cur_freq"))
    }

    // ---- provenance is part of the type -----------------------------------------------------------------

    @Test
    fun `every reviewed quirk cites a source, an evidence link and a revision`() {
        assertTrue(AtlasQuirkBase.ALL.isNotEmpty())
        assertTrue(AtlasQuirkBase.ALL.all { it.sourceId.isNotBlank() })
        assertTrue(AtlasQuirkBase.ALL.all { it.evidenceLink.isNotBlank() })
        assertTrue(AtlasQuirkBase.ALL.all { it.revision >= 1 })
        assertEquals("ids are unique", AtlasQuirkBase.ALL.size, AtlasQuirkBase.ALL.map { it.id }.distinct().size)
    }

    @Test
    fun `a quirk without a source or without a match criterion cannot be built`() {
        assertThrows(IllegalArgumentException::class.java) {
            AtlasQuirk(
                id = "no-source",
                match = AtlasQuirkMatch(pathFragments = setOf("/sys/class/devfreq")),
                effect = AtlasQuirkEffect.ExpectDenied("because"),
                sourceId = "",
                evidenceLink = "somewhere",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            AtlasQuirkMatch()
        }
    }

    @Test
    fun `a match criterion that is present must hold`() {
        val qualcomm = AtlasQuirkMatch(socModels = setOf("sm8650"), pathFragments = setOf("/sys/class/devfreq"))

        assertTrue(qualcomm.matches(manet, "/sys/class/devfreq/gpu/cur_freq"))
        assertTrue("the model comparison is not case-sensitive", qualcomm.matches(manet.copy(socModel = "sm8650"), "/sys/class/devfreq/x"))
        assertTrue("a device that does not declare the model does not match", !qualcomm.matches(unknown, "/sys/class/devfreq/x"))
        assertTrue("and a different interface does not match", !qualcomm.matches(manet, "/sys/class/thermal/x"))
    }

    // ---- the closure list, and the one skip it authorises ------------------------------------------------

    @Test
    fun `a reviewed closure is the only tier that reaches expected-denied`() {
        assertEquals(AtlasAvailability.EXPECTED_DENIED, AtlasClosedSurfaces.availabilityOf("/proc/stat"))
        assertEquals(AtlasAvailability.EXPECTED_DENIED, AtlasClosedSurfaces.availabilityOf("/sys/fs/cgroup/memory.pressure"))
        assertEquals(AtlasAvailability.EXPECTED_DENIED, AtlasQuirkBase.availabilityOf("/sys/fs/selinux/enforce"))
        assertEquals(
            "an open question is answered by the device, not by a table",
            null,
            AtlasClosedSurfaces.availabilityOf("/sys/class/thermal/thermal_zone0/temp"),
        )
    }

    @Test
    fun `a closed surface is skipped with its rule id instead of attempted`() {
        val scheduler = AtlasProbeScheduler(AtlasReadBudget.DEFAULT, clockMs = { 0L })
        val probes = listOf(
            AtlasScheduledProbe("closed.probe", "/proc/stat", AtlasProbeCost(1, 64), freshness = null),
            AtlasScheduledProbe("open.probe", "/proc/pressure/memory", AtlasProbeCost(1, 64), freshness = null),
        )

        val plan = scheduler.plan(
            probes = probes,
            bootGeneration = 0L,
            privilegeGeneration = 0L,
            blockedReasonOf = { path ->
                AtlasClosedSurfaces.match(path)?.let { surface -> "${AtlasSkipReasons.EXPECTED_DENIED}:${surface.id}" }
            },
        )

        assertEquals(listOf("open.probe"), plan.due.map { it.id })
        assertEquals("${AtlasSkipReasons.EXPECTED_DENIED}:R-04", plan.reasonFor("closed.probe"))
        assertTrue("a skipped probe costs no budget", plan.plannedOperations == 1)
    }

    @Test
    fun `no reviewed or candidate entry addresses a closed family`() {
        val entries = AtlasReviewedSeeds.entries() + AtlasCommunityBank.entries()

        assertTrue(entries.isNotEmpty())
        entries.forEach { entry ->
            // The address an entry produces, in the shape the discovery path builds: root plus attribute.
            val address = "${entry.parentRoot}/${entry.attribute}"
            assertEquals(
                "a catalog entry must not address a family the platform closes: ${entry.id}",
                null,
                AtlasClosedSurfaces.match(address),
            )
        }
    }

    @Test
    fun `the closed families are path-shaped and each cites its review`() {
        assertTrue(AtlasClosedSurfaces.ALL.isNotEmpty())
        AtlasClosedSurfaces.ALL.forEach { surface ->
            assertTrue("a family is a path prefix: ${surface.prefix}", surface.prefix.startsWith("/"))
            assertTrue(surface.evidence.isNotBlank())
            assertTrue(surface.id.startsWith("R-"))
        }
        assertNotEquals(
            "the list must not be trivially empty of the entries the review established",
            0,
            AtlasClosedSurfaces.ALL.size,
        )
    }
}
