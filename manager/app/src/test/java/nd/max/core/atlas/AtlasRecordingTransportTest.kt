/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.atlas

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import nd.max.core.hardware.AtlasReadBudget
import nd.max.core.hardware.AtlasReadTransport
import nd.max.core.hardware.AtlasTransportList
import nd.max.core.hardware.AtlasTransportRead
import nd.max.core.hardware.ReadOnlyProbeAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The recorder is only worth anything if it can be pointed at a **real run**, so these tests decorate a
 * real filesystem transport, drive the shipped boundary through the decorator, and then replay what was
 * captured. Three properties are asserted, and each one has a specific falsehood behind it:
 *
 * 1. **An answer is passed through untouched.** If recording could rewrite a value, the fixture would
 *    describe a device that was never measured — and the reading the user saw would be a different one
 *    than the reading on disk.
 * 2. **The capture reproduces the run.** Same requests, same results, object by object: the same
 *    equivalence `AtlasFixtureRecorderTest` asserts, reached here through the decorator a scan uses.
 * 3. **A refusal or an unaddressable answer is counted, never thrown.** A diagnostic feature must not be
 *    able to break a scan, and it must not hide its own omissions either.
 *
 * The origin is [AtlasFixtureOrigin.HOST] wherever a JVM filesystem is decorated: these are this host's
 * answers, and the label is what stops them being quoted later as evidence about a phone.
 */
class AtlasRecordingTransportTest {

    private val version = "atlas-test-1"
    private val thermalTemp = "/sys/class/thermal/thermal_zone0/temp"
    private val devfreqRoot = "/sys/class/devfreq"
    private val devfreqValue = "/sys/class/devfreq/gpu/cur_freq"
    private val missingChild = "/sys/class/devfreq/absent_freq"

    @Test
    fun `the decorator returns exactly what the transport it wraps answered`() {
        val live = AtlasFileReadTransport(deviceTree())
        val recording = AtlasRecordingTransport(live, version, origin = AtlasFixtureOrigin.HOST)

        listOf(thermalTemp, devfreqValue, missingChild).forEach { path ->
            assertEquals(
                "recording must not change a reading at $path",
                live.readText(path, 4096),
                recording.readText(path, 4096),
            )
        }
        assertEquals(live.listNames(devfreqRoot, 64), recording.listNames(devfreqRoot, 64))
        assertEquals(live.canonicalPath(thermalTemp, 8), recording.canonicalPath(thermalTemp, 8))
    }

    @Test
    fun `a decorated run replays into the run it decorated`() {
        val live = AtlasFileReadTransport(deviceTree())
        val recording = AtlasRecordingTransport(live, version, origin = AtlasFixtureOrigin.HOST)

        val liveResults = drive(live)
        val recordedResults = drive(recording)
        val replayed = drive(AtlasFixtureTransport(recording.result().fixture))

        assertEquals("a decorated run must answer what the undecorated one answered", liveResults, recordedResults)
        assertEquals("and the capture must replay into the same answers", liveResults, replayed)
    }

    @Test
    fun `the capture is labelled with the machine it came from`() {
        val recording = AtlasRecordingTransport(AtlasFileReadTransport(deviceTree()), version, origin = AtlasFixtureOrigin.HOST)
        drive(recording)

        val result = recording.result()

        assertEquals(AtlasFixtureOrigin.HOST, result.fixture.origin)
        assertEquals(version, result.fixture.catalogVersion)
        assertTrue("a capture taken from a run is readable back", AtlasFixtureCodec.decode(AtlasFixtureCodec.encode(result.fixture)).isReadable)
        assertTrue("and it holds the answers the run received", result.fixture.entries.isNotEmpty())
    }

    @Test
    fun `a path the recorder refuses is still answered, and named as skipped`() {
        val live = AtlasFileReadTransport(deviceTree())
        val recording = AtlasRecordingTransport(live, version, origin = AtlasFixtureOrigin.HOST)
        val unapproved = "/data/local/tmp/dump"

        val answer = recording.readText(unapproved, 4096)

        assertEquals("the caller must still receive the transport's own answer", live.readText(unapproved, 4096), answer)
        assertTrue("the refusal is named, not dropped", recording.result().skipped.any { it.path == unapproved })
        assertTrue("and the refused path is not in the fixture", unapproved !in recording.result().fixture.paths())
    }

    @Test
    fun `a resolution the format cannot address is counted instead of thrown`() {
        // A transport that hands back a relative path: the format requires an absolute addressable one,
        // so the answer can be returned to the caller but cannot be recorded.
        val odd = OddTransport(resolved = "not/absolute")
        val recording = AtlasRecordingTransport(odd, version, origin = AtlasFixtureOrigin.HOST)

        val resolved = recording.canonicalPath(thermalTemp, 8)

        assertEquals("the caller still gets what the transport said", "not/absolute", resolved)
        assertEquals("and the omission is counted rather than hidden", 1, recording.result().unrecordable)
        assertTrue("nothing was recorded for it", thermalTemp !in recording.result().fixture.paths())
    }

    @Test
    fun `an unresolvable path is a recorded fact and not an omission`() {
        val recording = AtlasRecordingTransport(OddTransport(resolved = null), version, origin = AtlasFixtureOrigin.HOST)

        assertEquals(null, recording.canonicalPath(thermalTemp, 8))
        assertEquals(0, recording.result().unrecordable)
        assertEquals(AtlasFixtureAnswer.Unresolvable, recording.result().fixture.answerFor(thermalTemp))
    }

    /** The same requests in the same order, so two runs are comparable at all. */
    private fun drive(transport: AtlasReadTransport): List<AtlasReadResult> {
        val access = ReadOnlyProbeAccess(transport, AtlasReadBudget.DEFAULT, clockMs = { 0L })
        access.list(devfreqRoot)
        return listOf(
            access.read(request(thermalTemp)),
            access.read(request(devfreqValue)),
            access.read(request(missingChild)),
        )
    }

    private fun request(path: String): AtlasProbeRequest = AtlasProbeRequest(
        id = "cpu.cpufreq.cur",
        domain = AtlasDomain.CPU,
        providerId = "local",
        catalogVersion = version,
        sourceId = "reviewed-1",
        path = path,
        unit = AtlasUnit.KILO_HERTZ,
    )

    private fun deviceTree(): Path {
        val root = Files.createTempDirectory("atlas-recording-").also { it.toFile().deleteOnExit() }
        root.resolve("sys/class/thermal/thermal_zone0").createDirectories().resolve("temp").writeText("42000\n")
        val devfreq = root.resolve("sys/class/devfreq").createDirectories()
        devfreq.resolve("gpu").createDirectories().resolve("cur_freq").writeText("500000000\n")
        return root
    }

    /** A transport whose only interesting behaviour is the resolution it reports. */
    private class OddTransport(private val resolved: String?) : AtlasReadTransport {
        override fun readText(path: String, maxBytes: Int): AtlasTransportRead =
            AtlasTransportRead.Text("17\n", truncated = false)

        override fun listNames(path: String, limit: Int): AtlasTransportList = AtlasTransportList(listOf("gpu"))

        override fun canonicalPath(path: String, maxHops: Int): String? = resolved
    }
}
