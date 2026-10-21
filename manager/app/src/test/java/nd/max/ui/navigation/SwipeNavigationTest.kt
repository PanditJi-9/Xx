/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
package nd.max.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * حكم التمرير — يقاس على JVM بلا جهاز وبلا Compose.
 *
 * العطب الذي يمنعه: أن يُقلب الاتجاه في العربية (RTL) فيذهب السحب إلى القسم الخطأ،
 * أو أن يتحوّل تمرير قائمة رأسيّة إلى تنقّل لأن الإصبع مال قليلًا في نهايته.
 */
class SwipeNavigationTest {

    private val minDistance = 100f
    private val minVelocity = 500f

    private fun resolve(
        dx: Float,
        dy: Float = 0f,
        velocityX: Float = 0f,
        isRtl: Boolean = false,
    ) = resolveSwipeDirection(
        dx = dx,
        dy = dy,
        velocityX = velocityX,
        minDistancePx = minDistance,
        minVelocityPxPerSecond = minVelocity,
        isRtl = isRtl,
    )

    @Test
    fun `ltr swipe left is next`() {
        assertEquals(SwipeDirection.NEXT, resolve(dx = -200f, velocityX = -900f))
    }

    @Test
    fun `ltr swipe right is previous`() {
        assertEquals(SwipeDirection.PREVIOUS, resolve(dx = 200f, velocityX = 900f))
    }

    @Test
    fun `rtl swipe right is next`() {
        assertEquals(SwipeDirection.NEXT, resolve(dx = 200f, velocityX = 900f, isRtl = true))
    }

    @Test
    fun `rtl swipe left is previous`() {
        assertEquals(SwipeDirection.PREVIOUS, resolve(dx = -200f, velocityX = -900f, isRtl = true))
    }

    @Test
    fun `vertical drag is not navigation`() {
        assertEquals(SwipeDirection.NONE, resolve(dx = 90f, dy = 400f, velocityX = 100f))
    }

    @Test
    fun `short slow drag is not navigation`() {
        assertEquals(SwipeDirection.NONE, resolve(dx = 40f, velocityX = 120f))
    }

    @Test
    fun `short fast fling is navigation`() {
        assertEquals(SwipeDirection.NEXT, resolve(dx = -40f, velocityX = -1200f))
    }

    @Test
    fun `long slow drag is navigation`() {
        assertEquals(SwipeDirection.NEXT, resolve(dx = -160f, velocityX = 0f))
    }

    @Test
    fun `diagonal drag keeps the vertical list`() {
        assertEquals(SwipeDirection.NONE, resolve(dx = -120f, dy = 100f, velocityX = -200f))
    }

    @Test
    fun `zero horizontal displacement is not navigation`() {
        assertEquals(SwipeDirection.NONE, resolve(dx = 0f, dy = 0f, velocityX = 900f))
    }

    @Test
    fun `controller exposes the last registered screen and removes only its own token`() {
        val controller = SwipeNavigationController()
        val firstHandlers = SwipeNavigationHandlers(onNext = { true })
        val secondHandlers = SwipeNavigationHandlers(onNext = { true })
        val first = controller.register(firstHandlers)
        val second = controller.register(secondHandlers)

        assertSame(secondHandlers, controller.current)
        controller.unregister(first)
        assertSame(secondHandlers, controller.current)
        controller.unregister(second)
        assertNull(controller.current)
    }
}
