/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.atlas

import nd.max.core.hardware.AtlasReadBudget
import nd.max.core.hardware.AtlasReadTransport
import nd.max.core.hardware.AtlasTransportList
import nd.max.core.hardware.AtlasTransportRead

/**
 * Records a **real device run** as a fixture, by decorating the transport the run already uses.
 *
 * This is the half of the evidence loop that was missing: [AtlasFixtureRecorder] could record, and
 * [AtlasFixtureTransport] could replay, but nothing in the app ever produced a fixture whose origin is
 * `DEVICE`. Every capture that existed was a `HOST` capture written by a test, which is why every
 * compatibility claim in this repository still ends in "needs a phone". A decorator closes that gap
 * without adding a second read path: the scan drives this object exactly as it drove the real
 * transport, and the fixture falls out of the run that actually happened.
 *
 * Three rules define it, and each one exists to stop a specific way of corrupting the evidence:
 *
 * 1. **The answer is passed through untouched.** Whatever the inner transport returned is what the
 *    caller receives — the same object, not a re-derived one. Recording a value must never change a
 *    reading, because then the fixture would describe a device that was never measured.
 * 2. **Recording cannot fail the run.** A fixture that cannot hold an answer (a resolved path outside
 *    the addressable form, a value past the format's ceiling) is counted in [Result.unrecordable] and
 *    left unrecorded. The alternative — letting a formatting limit throw out of `readText` — would turn
 *    a diagnostic feature into a way to break a scan, and swallowing it silently would hide a real
 *    omission. It is neither: the omission is counted, and the run continues.
 * 3. **No second read.** The answer recorded is the answer already obtained, so a node that changed
 *    between two reads cannot make the fixture disagree with the run it claims to be.
 *
 * It is a read transport like any other: the interface it implements has no mutating member, it opens
 * nothing itself, and the gate, budgets and anchor rules above it are unchanged — a decorated transport
 * is not a privileged one.
 *
 * @param origin which machine is being recorded. Defaults to [AtlasFixtureOrigin.DEVICE] because the
 *   app only ever runs on an Android device; a test that decorates a JVM filesystem must pass
 *   [AtlasFixtureOrigin.HOST] so the artifact cannot later be cited as evidence about a phone.
 */
class AtlasRecordingTransport(
    private val inner: AtlasReadTransport,
    catalogVersion: String,
    private val origin: AtlasFixtureOrigin = AtlasFixtureOrigin.DEVICE,
    readAllowance: Int = AtlasReadBudget.DEFAULT.maxScalarBytes,
    listAllowance: Int = AtlasFixtureRecorder.DEFAULT_LIST_LIMIT,
    hopAllowance: Int = AtlasReadBudget.DEFAULT.maxSymlinkHops,
) : AtlasReadTransport {

    private val recorder = AtlasFixtureRecorder(inner, readAllowance, listAllowance, hopAllowance)

    /**
     * Reads may be driven from more than one thread (the boundary admits a bounded concurrency), so the
     * recording state is guarded. The lock is held for a map insertion and nothing else — no transport
     * call happens inside it, so a slow node cannot serialise the scan behind the recorder.
     */
    private val lock = Any()

    private var state: AtlasFixtureRecorder.State = recorder.start(origin, catalogVersion)

    /** Answers received but not kept, because the fixture format cannot hold them. */
    private var unrecordable: Int = 0

    /** What the recording produced so far. Safe to call at any time. */
    fun result(): Result = synchronized(lock) {
        Result(
            fixture = state.fixture,
            skipped = state.skipped,
            truncated = state.truncated,
            unrecordable = unrecordable,
        )
    }

    /** What one recorded run produced. `unrecordable` is never folded into `skipped`: one is a refusal
     *  by policy, the other a limit of the format, and a reader must be able to tell them apart. */
    data class Result(
        val fixture: AtlasFixture,
        val skipped: List<AtlasFixtureRecorder.Skipped>,
        val truncated: Boolean,
        val unrecordable: Int,
    )

    override fun readText(path: String, maxBytes: Int): AtlasTransportRead {
        val answer = inner.readText(path, maxBytes)
        record { current -> recorder.recordRead(current, path, answer) }
        return answer
    }

    override fun listNames(path: String, limit: Int): AtlasTransportList {
        val listing = inner.listNames(path, limit)
        record { current -> recorder.recordListing(current, path, listing) }
        return listing
    }

    override fun canonicalPath(path: String, maxHops: Int): String? {
        val resolved = inner.canonicalPath(path, maxHops)
        // A resolution the format cannot address is skipped before it can throw. `recordResolution`
        // would refuse it at construction time (a fixture key/value must be addressable), and the
        // replay's documented default for an unrecorded resolution is the path itself — which claims
        // no link and no absence, so nothing is invented by the omission.
        if (resolved == null || AtlasIds.isSafeAbsolutePath(resolved)) {
            record { current -> recorder.recordResolution(current, path, resolved) }
        } else {
            synchronized(lock) { unrecordable += 1 }
        }
        return resolved
    }

    /**
     * Applies one recording step, counting instead of throwing.
     *
     * Any refusal here (a format ceiling, an unaddressable key) is a fact about the fixture, never about
     * the device, so it must not reach the scan.
     */
    private fun record(step: (AtlasFixtureRecorder.State) -> AtlasFixtureRecorder.State) {
        synchronized(lock) {
            state = try {
                step(state)
            } catch (_: IllegalArgumentException) {
                unrecordable += 1
                state
            } catch (_: IllegalStateException) {
                unrecordable += 1
                state
            }
        }
    }
}
