/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.island.core

import nd.max.island.engine.IslandLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * الموضع الآمن — العيوب التي يمنعها هذا الملفّ كلّها مرئية على جهاز حقيقي:
 * نصّ تحت الكاميرا، كبسولة تحت شريط الحالة، خروج في الوضع العرضي، وميزانية نصّ لا تتّسع.
 */
class IslandLayoutTest {

    private val portrait = IslandInsets(
        screenWidthPx = 1080,
        screenHeightPx = 2400,
        density = 3f,
        safeTopPx = 96,
        statusBarPx = 72,
        safeBottomPx = 48,
        cutoutLeftPx = 400,
        cutoutTopPx = 0,
        cutoutRightPx = 680,
        cutoutBottomPx = 96,
    )

    @Test
    fun `the island wraps the camera cutout at the top by default`() {
        // مركز الكاميرا 48px، الكبسولة تُطوّق الكاميرا في الأعلى + إزاحة المستخدم 10dp (30px) = 30px.
        assertEquals(30, IslandLayout.preferredTopPx(portrait, 10f, avoidSystemUi = true))
        // وعند زيادة إزاحة المستخدم إلى 100dp (300px) تنزل بسلاسة.
        assertEquals(300, IslandLayout.preferredTopPx(portrait, 100f, avoidSystemUi = true))
    }

    @Test
    fun `user offset is respected when wrapping the camera`() {
        assertEquals(30, IslandLayout.preferredTopPx(portrait, 10f, avoidSystemUi = false))
    }

    @Test
    fun `the width respects the safe sides and never reaches the screen edge`() {
        // 1080 - 0 - 0 - 2*18 = 1044، وسقف 94٪ = 1015.
        assertEquals(1015, IslandLayout.maxWidthPx(portrait))
        val landscape = portrait.copy(safeLeftPx = 200, safeRightPx = 0, landscape = true)
        assertEquals(1080, landscape.screenWidthPx) // الشاشة لم تُقصّ في هذا الاختبار
        assertEquals((1080 - 200 - 36), IslandLayout.maxWidthPx(landscape))
    }

    @Test
    fun `a centered cutout is the only one that reserves a wing`() {
        assertEquals(280, IslandLayout.centeredCutoutSpanPx(portrait))
        val side = portrait.copy(cutoutLeftPx = 20, cutoutRightPx = 120)
        assertNull(IslandLayout.centeredCutoutSpanPx(side))
        assertNull(IslandLayout.centeredCutoutSpanPx(portrait.copy(cutoutLeftPx = -1, cutoutRightPx = -1)))
    }

    @Test
    fun `placement centers the window and clamps a user offset inside the safe area`() {
        val center = IslandLayout.place(portrait, 0f, 10f, 300, 120, avoidSystemUi = true)
        assertEquals(390, center.xPx)
        assertEquals(108, center.yPx)
        assertTrue(center.belowSystemUi)
        assertEquals(1015, center.maxWidthPx)
        assertEquals(10, center.wingPx(300))

        val farRight = IslandLayout.place(portrait, 400f, 10f, 300, 120, avoidSystemUi = true)
        assertEquals(1080 - 18 - 300, farRight.xPx)
        val farLeft = IslandLayout.place(portrait, -400f, 10f, 300, 120, avoidSystemUi = true)
        assertEquals(18, farLeft.xPx)
    }

    @Test
    fun `a tall card cannot be placed below the visible screen`() {
        val nearlyFull = IslandLayout.place(portrait, 0f, 10f, 500, 2350, avoidSystemUi = true)
        assertTrue(nearlyFull.yPx >= 0)
        assertTrue(nearlyFull.yPx + 2350 <= 2400)
        // ولا قسمة على صفر ولا موضع سالب قبل أن يقيس Compose.
        val unmeasured = IslandLayout.place(portrait, 0f, 10f, 0, 0, avoidSystemUi = true)
        assertTrue(unmeasured.xPx >= 0 && unmeasured.yPx >= 0)
    }

    @Test
    fun `landscape keeps the island inside the cutout safe insets`() {
        val landscape = IslandInsets(
            screenWidthPx = 2400,
            screenHeightPx = 1080,
            density = 2f,
            safeLeftPx = 220,
            safeTopPx = 0,
            safeRightPx = 80,
            statusBarPx = 0,
            landscape = true,
        )
        val p = IslandLayout.place(landscape, 0f, 8f, 600, 120, avoidSystemUi = true)
        assertTrue(p.xPx >= 220 + 12)
        assertTrue(p.xPx + 600 <= 2400 - 80 - 12)
        assertEquals(8 * 2, p.yPx)
        assertEquals(2400 - 220 - 80 - 24, p.maxWidthPx)
    }

    @Test
    fun `text budget shrinks with the real width instead of clipping after drawing`() {
        val wide = IslandLayout.textBudget(widthPx = 500, density = 2f, level = IslandLevel.Compact)
        val narrow = IslandLayout.textBudget(widthPx = 180, density = 2f, level = IslandLevel.Compact)
        assertTrue(wide.titleChars > narrow.titleChars)
        assertTrue(wide.showTrailing)
        assertFalse(narrow.showTrailing)
        assertFalse(narrow.showBody)

        val card = IslandLayout.textBudget(widthPx = 700, density = 2f, level = IslandLevel.Large)
        assertEquals(2, card.titleLines)
        assertTrue(card.bodyLines >= 3)
    }

    @Test
    fun `a faster flick gives a stiffer and slightly bouncier spring`() {
        val slow = IslandLayout.springFor(0f, 1f)
        val fast = IslandLayout.springFor(6_000f, 1f)
        assertTrue(fast.stiffness > slow.stiffness)
        assertTrue(fast.dampingRatio < slow.dampingRatio)
        assertTrue(fast.dampingRatio >= 0.62f)
        assertTrue(fast.stiffness <= 6_000f)
        // السرعة المضاعفة تصل إلى السقف لا تتجاوزه.
        assertTrue(IslandLayout.springFor(99_000f, 2f).stiffness <= 6_000f)
    }

    @Test
    fun `transition duration and start scale follow the gesture velocity`() {
        val slow = IslandLayout.transitionFor(0f)
        val fast = IslandLayout.transitionFor(6_000f)
        assertTrue(fast.durationMs < slow.durationMs)
        assertTrue(fast.startScale < slow.startScale)
        assertTrue(fast.slideFraction > slow.slideFraction)
        // سرعة سالبة أو شبه ساكنة = لا حركة: نفس المدّة ونفس البداية (بفارق float).
        val idle = IslandLayout.transitionFor(-1f)
        assertEquals(slow.durationMs, idle.durationMs)
        assertEquals(slow.startScale, idle.startScale, 0.001f)
        assertEquals(slow.slideFraction, idle.slideFraction, 0.001f)
    }

    @Test
    fun `negative and NaN measurements never produce a negative placement`() {
        val broken = IslandInsets(0, 0, Float.NaN, -50, -90, -20, -10, -5)
        val p = IslandLayout.place(broken, Float.NaN, -20f, -100, -100, avoidSystemUi = true)
        assertTrue(p.xPx >= 0)
        assertTrue(p.yPx >= 0)
        assertTrue(p.maxWidthPx >= 1)
    }
}
