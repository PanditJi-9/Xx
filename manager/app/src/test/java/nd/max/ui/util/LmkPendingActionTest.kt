/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LmkPendingActionTest {
    @Test
    fun aPreviouslyAcknowledgedGateGetsTheJustClickedRequest() {
        val pending = LmkPendingAction()
        val target = listOf(7500, 15000)
        val baseline = listOf(10000, 20000)
        // callback صُنع قبل النقرة، مثل rememberDangerGate قبل إعادة التركيب.
        val allowed = { pending.take() }
        pending.set(target, baseline)
        assertEquals(LmkPendingAction.Request(target, baseline), allowed())
    }

    @Test
    fun theFirstAcknowledgementKeepsTheSnapshotUntilConfirmation() {
        val pending = LmkPendingAction()
        val target = mutableListOf(7500, 15000)
        val baseline = mutableListOf(10000, 20000)
        pending.set(target, baseline)
        target[0] = 9999
        baseline[0] = 9999
        assertEquals(LmkPendingAction.Request(listOf(7500, 15000), listOf(10000, 20000)), pending.take())
    }

    @Test
    fun confirmingTwiceCannotRunTheWriteTwice() {
        val pending = LmkPendingAction()
        pending.set(listOf(7500, 15000), listOf(10000, 20000))
        pending.take()
        assertNull(pending.take())
    }

    @Test
    fun aNewClickReplacesAnyDismissedDialogRequest() {
        val pending = LmkPendingAction()
        pending.set(listOf(7500, 15000), listOf(10000, 20000))
        pending.set(listOf(12500, 25000), listOf(10000, 20000))
        assertEquals(LmkPendingAction.Request(listOf(12500, 25000), listOf(10000, 20000)), pending.take())
    }
}
