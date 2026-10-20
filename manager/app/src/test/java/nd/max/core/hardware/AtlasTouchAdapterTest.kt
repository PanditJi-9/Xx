/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import nd.max.core.atlas.AtlasControlGoal
import nd.max.core.atlas.AtlasControlIntent
import nd.max.core.atlas.AtlasControlTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * مُلاءِم اللمس: لا يُبنى مسار إلا لعقدة **قرأَت** قيمةً ثنائية وقبلت الكتابة، والمسار نفسه
 * يُنفَّذ عبر عقد أطلس (المخطِّط ← المُنفِّذ) لا بكاتب ثانٍ.
 */
class AtlasTouchAdapterTest {

    private class FakeAccess(
        private val values: MutableMap<String, String> = mutableMapOf(),
        private val writable: Set<String> = emptySet(),
    ) : TouchControlBackend.Access {
        val writes = mutableListOf<Pair<String, String>>()

        override fun read(path: String): String? = values[path]
        override fun exists(path: String): Boolean = path in values
        override fun writable(path: String): Boolean = path in writable
        override fun writeVerified(path: String, value: String): WriteVerification.Outcome {
            writes += path to value
            if (path !in writable) return WriteVerification.Outcome.WRITE_FAILED
            values[path] = value
            return WriteVerification.Outcome.MATCHED
        }
    }

    private val game = TouchControlBackend.Node(
        path = "/proc/touchpanel/game_switch_enable",
        onValue = "1",
        offValue = "0",
        kind = TouchControlBackend.Kind.GAME_MODE,
    )

    private fun context(access: TouchControlBackend.Access, vararg nodes: TouchControlBackend.Node) =
        AtlasAdapterContext(
            privileged = true,
            touchNodes = nodes.toList(),
            touchAccess = access,
        )

    private fun request(enabled: Boolean) = AtlasControlRequest.TouchBoost(
        intent = AtlasControlIntent(
            target = AtlasControlTarget.TOUCH,
            goal = AtlasControlGoal.PERFORMANCE,
        ),
        owner = ControlOwnership.Owner.GLOBAL_PROFILE,
        token = "test-touch",
        enabled = enabled,
    )

    @Test
    fun `a readable writable boolean node becomes a reviewed route`() {
        val access = FakeAccess(mutableMapOf(game.path to "0"), writable = setOf(game.path))
        val plan = TouchSysfsAdapter().plan(context(access, game), request(enabled = true))

        assertEquals(1, plan.bindings.size)
        val binding = plan.bindings.single()
        assertEquals(HardwareControlKey.touchBoost(game.path), binding.request.key)
        assertEquals("1", binding.request.desired)
        assertEquals("0", binding.request.baseline)
        assertTrue(binding.candidate.evidence.reviewed)
        assertEquals(AtlasControlTarget.TOUCH, binding.candidate.evidence.target)
    }

    @Test
    fun `a node that does not answer with zero or one is not a control`() {
        val access = FakeAccess(mutableMapOf(game.path to "2"), writable = setOf(game.path))
        val plan = TouchSysfsAdapter().plan(context(access, game), request(enabled = true))

        assertTrue(plan.bindings.isEmpty())
        assertTrue(plan.skipped.any { it.second == "touch-node-not-boolean-writable" })
    }

    @Test
    fun `the production reconciler applies the node through the atlas transaction`() {
        val access = FakeAccess(mutableMapOf(game.path to "0"), writable = setOf(game.path))
        val port = object : AtlasRepairPort {
            override fun execute(request: HardwareRepairRequest): HardwareRepairResult {
                val applied = request.apply(request.desired)
                return HardwareRepairResult(
                    routeId = request.routeId,
                    key = request.key,
                    state = if (applied) HardwareRepairState.CONFIRMED_WINDOW else HardwareRepairState.BLOCKED,
                    requested = request.desired,
                    actual = request.read(),
                    applied = applied,
                    verified = applied,
                    stabilitySamples = if (applied) 1 else 0,
                    rollbackAttempted = false,
                    rollbackVerified = null,
                )
            }
        }

        val attempt = TouchControlBackend.Reconciler(
            access = access,
            vendor = { TouchControlBackend.NoVendorBridge },
            atlas = port,
        ).reconcile(true)

        assertEquals(TouchControlBackend.Result.APPLIED, attempt.result)
        assertEquals(game.path, attempt.path)
        assertEquals("write-verified", attempt.reason)
        assertEquals(listOf(game.path to "1"), access.writes)
    }
}
