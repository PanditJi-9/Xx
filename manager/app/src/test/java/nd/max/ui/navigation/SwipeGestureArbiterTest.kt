/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
package nd.max.ui.navigation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حكم ملكية السحب — يقاس على JVM بلا جهاز وبلا Compose.
 *
 * العطب الذي يمنعه: أن تُلغي حركة رأسية لقائمة التنقّل الأفقي، أو أن يُسحب صفّ أفقي فينتقل الجذر معه.
 */
class SwipeGestureArbiterTest {

    private val arbiter = SwipeGestureArbiter()
    private val connection = arbiter.nestedScrollConnection

    @Test
    fun fresh_gesture_is_not_owned_by_content() {
        arbiter.beginGesture()
        assertFalse(arbiter.isTakenByContent)
    }

    @Test
    fun horizontal_row_that_consumed_takes_the_gesture() {
        arbiter.beginGesture()
        connection.onPostScroll(Offset(-40f, 0f), Offset.Zero, NestedScrollSource.UserInput)
        assertTrue(arbiter.isTakenByContent)
    }

    @Test
    fun horizontal_row_at_its_end_does_not_take_the_gesture() {
        arbiter.beginGesture()
        connection.onPostScroll(Offset.Zero, Offset(-40f, 0f), NestedScrollSource.UserInput)
        assertFalse(arbiter.isTakenByContent)
    }

    @Test
    fun vertical_list_consumption_does_not_block_a_horizontal_swipe() {
        arbiter.beginGesture()
        connection.onPostScroll(Offset(0f, -12f), Offset.Zero, NestedScrollSource.UserInput)
        arbiter.onPointerEvent(consumedPastSlop = true, dx = -120f, dy = -12f)
        assertFalse(arbiter.isTakenByContent)
    }

    @Test
    fun non_scroll_consumer_on_a_horizontal_drag_takes_the_gesture() {
        arbiter.beginGesture()
        arbiter.onPointerEvent(consumedPastSlop = true, dx = -60f, dy = 4f)
        assertTrue(arbiter.isTakenByContent)
    }

    @Test
    fun consumption_on_a_vertical_drag_does_not_take_the_gesture() {
        arbiter.beginGesture()
        arbiter.onPointerEvent(consumedPastSlop = true, dx = 3f, dy = -80f)
        assertFalse(arbiter.isTakenByContent)
    }

    @Test
    fun consumption_before_slop_is_ignored() {
        arbiter.beginGesture()
        arbiter.onPointerEvent(consumedPastSlop = false, dx = -60f, dy = 4f)
        assertFalse(arbiter.isTakenByContent)
    }

    @Test
    fun fling_source_is_ignored() {
        arbiter.beginGesture()
        connection.onPostScroll(Offset(-40f, 0f), Offset.Zero, NestedScrollSource.Fling)
        assertFalse(arbiter.isTakenByContent)
    }

    @Test
    fun begin_gesture_clears_the_previous_gesture() {
        arbiter.beginGesture()
        connection.onPostScroll(Offset(-40f, 0f), Offset.Zero, NestedScrollSource.UserInput)
        arbiter.beginGesture()
        assertFalse(arbiter.isTakenByContent)
    }
}
