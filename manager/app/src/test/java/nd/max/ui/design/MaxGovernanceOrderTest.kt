/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.design

import nd.max.core.hardware.ControlOwnership
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The governance ladder is the only *rule* the UX slice introduced, and it must not drift
 * from `ControlOwnership`.
 *
 * It is a pure JVM measurement: no device, no Compose runtime. If someone adds a seventh
 * owner, or reorders the enum priorities, this fails instead of the UI quietly showing an
 * order the arbiter does not enforce.
 */
class MaxGovernanceOrderTest {

    @Test
    fun ladder_lists_every_owner_exactly_once() {
        assertEquals(
            ControlOwnership.Owner.values().toSet(),
            maxGovernanceLadder.toSet(),
        )
        assertEquals(
            ControlOwnership.Owner.values().size,
            maxGovernanceLadder.size,
        )
    }

    @Test
    fun ladder_is_sorted_by_declared_priority_descending() {
        assertEquals(
            maxGovernanceLadder.sortedByDescending { it.priority },
            maxGovernanceLadder,
        )
        assertEquals(ControlOwnership.Owner.RECOVERY, maxGovernanceLadder.first())
        assertEquals(ControlOwnership.Owner.SYSTEM, maxGovernanceLadder.last())
    }

    /**
     * The two rungs that must never be outvoted by a user/UI choice stay at the top.
     * This is the contract the ladder exists to surface, not a new policy.
     */
    @Test
    fun safety_and_recovery_outrank_every_other_owner() {
        val topTwo = maxGovernanceLadder.take(2).toSet()
        assertEquals(
            setOf(ControlOwnership.Owner.RECOVERY, ControlOwnership.Owner.SAFETY),
            topTwo,
        )
    }

    /**
     * شريط «من يحكم الآن» يعرض **أعلى** حاكم نشط لا أوّل عنصر في اللقطة: `snapshot()` مرتّبة
     * بمفاتيحها، فلو عُرض أوّلها لقيل «MAX AI يحكم» وحمايةٌ حرارية تمنعه. وهذه هي القاعدة نفسها
     * التي يطبّقها الـarbiter عند التنازع، مقيسةً على JVM.
     */
    @Test
    fun strip_reports_the_highest_priority_active_owner() {
        val leases = listOf(
            lease("cpu.max_freq", ControlOwnership.Owner.MAX_AI),
            lease("gpu.max_freq", ControlOwnership.Owner.SAFETY),
            lease("vm.swappiness", ControlOwnership.Owner.GLOBAL_PROFILE),
        )
        assertEquals(ControlOwnership.Owner.SAFETY, maxGovernorTop(leases))
        assertEquals(null, maxGovernorTop(emptyList()))
    }

    private fun lease(key: String, owner: ControlOwnership.Owner) =
        ControlOwnership.Lease(
            key = key,
            owner = owner,
            ownerToken = "token-$key",
            desired = "value",
            acquiredAt = 0L,
        )
}
