/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

/**
 * عقد التصويب المخصّص: حالةٌ آمنة، وهندسةٌ لا تخرج من الشاشة، وأشكالٌ مختلفة فعلًا.
 *
 * والقياس هنا على القيم والحساب وحدهما — لا Android ولا Compose — فما يُقاس في JVM
 * هو ما يرسمه التراكب نفسه ([crosshairGeometry]) لا نسخة موازية منه.
 */
class CrosshairTest {

    @Test
    fun `defaults are safe and disabled`() {
        val state = CrosshairState()
        assertFalse("التصويب لا يظهر بلا طلب المالك", state.enabled)
        assertEquals(CrosshairStyle.CROSS, state.style)
        assertEquals(CrosshairState.DEFAULT_COLOR, state.color)
        assertEquals(CrosshairState.DEFAULT_SIZE_DP, state.sizeDp)
        assertEquals(CrosshairState.DEFAULT_THICKNESS_DP, state.thicknessDp)
        assertEquals(CrosshairState.DEFAULT_GAP_DP, state.gapDp)
        assertEquals(0f, state.offsetX, 0f)
        assertEquals(0f, state.offsetY, 0f)
        assertTrue("الحدود الداكنة افتراضية لتباين أعلى", state.outline)
    }

    @Test
    fun `unknown style key falls back to cross`() {
        assertEquals(CrosshairStyle.CROSS, CrosshairStyle.fromKey(null))
        assertEquals(CrosshairStyle.CROSS, CrosshairStyle.fromKey("NOPE"))
        CrosshairStyle.entries.forEach { style ->
            assertEquals(style, CrosshairStyle.fromKey(style.key))
        }
    }

    @Test
    fun `sanitized clamps every value into its published range`() {
        val wild = CrosshairState(
            style = CrosshairStyle.CIRCLE,
            color = 0x123456,
            sizeDp = 9_999,
            thicknessDp = -4,
            gapDp = 9_999,
            opacity = 4f,
            offsetX = 3f,
            offsetY = -3f
        ).sanitized()

        assertEquals(CrosshairState.MAX_SIZE_DP, wild.sizeDp)
        assertEquals(CrosshairState.MIN_THICKNESS_DP, wild.thicknessDp)
        assertTrue("الفراغ لا يأكل نصف الحجم كله", wild.gapDp <= wild.sizeDp / 2 - 1)
        assertEquals(CrosshairState.MAX_OPACITY, wild.opacity, 0f)
        assertEquals(CrosshairState.MAX_OFFSET, wild.offsetX, 0f)
        assertEquals(-CrosshairState.MAX_OFFSET, wild.offsetY, 0f)
        assertEquals("اللون يُجبر على العتامة، والشفافية في opacity وحدها", 0xFF123456.toInt(), wild.color)
        assertEquals("الشكل لا يتغيّر بالتقصيب", CrosshairStyle.CIRCLE, wild.style)
    }

    @Test
    fun `geometry is centered on an empty offset and moves with it`() {
        val state = CrosshairState(sizeDp = 20, gapDp = 4, thicknessDp = 2)
        val center = crosshairGeometry(state, width = 1_000f, height = 2_000f, density = 2f)
        assertEquals(500f, center.centerX, 0.01f)
        assertEquals(1_000f, center.centerY, 0.01f)
        assertEquals("الصليب أربعة أذرع", 4, center.lines.size)

        val moved = crosshairGeometry(
            state.copy(offsetX = 0.25f, offsetY = -0.25f),
            width = 1_000f,
            height = 2_000f,
            density = 2f
        )
        assertEquals(750f, moved.centerX, 0.01f)
        assertEquals(500f, moved.centerY, 0.01f)
    }

    @Test
    fun `geometry never lets the marker leave the screen`() {
        val state = CrosshairState(
            sizeDp = CrosshairState.MAX_SIZE_DP,
            thicknessDp = CrosshairState.MAX_THICKNESS_DP,
            offsetX = CrosshairState.MAX_OFFSET,
            offsetY = CrosshairState.MAX_OFFSET
        )
        // لوحة 200x320 dp بكثافة 3 (أي 600x960 بكسل): يجب أن تتّسع فيها العلامة القصوى،
        // وإلا طلب الاختبار المستحيل. اللوحة الأضيق من العلامة تُرسم مقصوصةً عمدًا (راجع KDoc في Crosshair.kt).
        val width = 600f
        val height = 960f
        val geometry = crosshairGeometry(state, width = width, height = height, density = 3f)
        val half = CrosshairState.MAX_SIZE_DP * 3f / 2f
        val stroke = CrosshairState.MAX_THICKNESS_DP * 3f
        assertTrue("الحافة اليمنى", geometry.centerX + half + stroke <= width + 0.01f)
        assertTrue("الحافة السفلى", geometry.centerY + half + stroke <= height + 0.01f)
        assertTrue("لا يخرج من اليسار", geometry.centerX - half - stroke >= -0.01f)
        assertTrue("لا يخرج من الأعلى", geometry.centerY - half - stroke >= -0.01f)
    }

    @Test
    fun `each style draws a different primitive set`() {
        val size = 32
        val cross = crosshairGeometry(
            CrosshairState(style = CrosshairStyle.CROSS, sizeDp = size),
            width = 1_000f,
            height = 1_000f,
            density = 1f
        )
        assertEquals(4, cross.lines.size)
        assertTrue(cross.circles.isEmpty())

        val tee = crosshairGeometry(
            CrosshairState(style = CrosshairStyle.TEE, sizeDp = size),
            width = 1_000f,
            height = 1_000f,
            density = 1f
        )
        assertEquals("T ثلاثة أذرع بلا سفلي", 3, tee.lines.size)
        assertTrue("ولا ذراع سفلية", tee.lines.none { it.startY > tee.centerY })

        val dot = crosshairGeometry(
            CrosshairState(style = CrosshairStyle.DOT, sizeDp = size),
            width = 1_000f,
            height = 1_000f,
            density = 1f
        )
        assertTrue(dot.lines.isEmpty())
        assertEquals(1, dot.circles.size)
        assertTrue("النقطة مُعبَّأة", dot.circles.single().filled)

        val circle = crosshairGeometry(
            CrosshairState(style = CrosshairStyle.CIRCLE, sizeDp = size),
            width = 1_000f,
            height = 1_000f,
            density = 1f
        )
        assertTrue(circle.lines.isEmpty())
        assertEquals("حلقة + نقطة", 2, circle.circles.size)
        assertTrue("الحلقة مجرَّدة", circle.circles.any { !it.filled })
        assertTrue("وفيها نقطة", circle.circles.any { it.filled })
    }

    @Test
    fun `arms keep a visible length inside the center gap`() {
        val state = CrosshairState(sizeDp = 24, gapDp = 40, thicknessDp = 2)
        val geometry = crosshairGeometry(state, width = 1_000f, height = 1_000f, density = 1f)
        geometry.lines.forEach { line ->
            val length = abs(line.endY - line.startY) + abs(line.endX - line.startX)
            assertTrue("الذراع ليست سالبة ولا صفرية", length > 0f)
        }
        // الفراغ بعد التقصيب لا يتجاوز نصف الحجم، فلا تنقلب الأذرع.
        val half = state.sanitized().sizeDp / 2f
        assertEquals(max(0f, half - 1f), state.sanitized().gapDp.toFloat(), 0f)
    }

    @Test
    fun `palette is opaque and has no duplicates`() {
        assertTrue("لوحة الألوان لا تُفرَّغ", CrosshairState.COLORS.size >= 4)
        CrosshairState.COLORS.forEach { color ->
            assertEquals("كل لون معتم: ${color.toUInt().toString(16)}", 0xFF, color ushr 24)
        }
        assertEquals(
            "لا لون مكرّر في المنتقي",
            CrosshairState.COLORS.size,
            CrosshairState.COLORS.toSet().size
        )
    }

    @Test
    fun `no rings leaves the shape exactly as it was`() {
        val plain = CrosshairState(style = CrosshairStyle.CROSS)
        val geometry = crosshairGeometry(plain, width = 1_000f, height = 1_000f, density = 1f)

        assertEquals(0, plain.rings)
        assertTrue("الصليب بلا حلقات لا يرسم دوائر", geometry.circles.isEmpty())
    }

    @Test
    fun `rings sit outside the shape and grow outwards`() {
        val one = crosshairGeometry(
            CrosshairState(style = CrosshairStyle.CROSS, rings = 1),
            width = 1_000f, height = 1_000f, density = 1f
        )
        val two = crosshairGeometry(
            CrosshairState(style = CrosshairStyle.CROSS, rings = 2),
            width = 1_000f, height = 1_000f, density = 1f
        )

        assertEquals(1, one.circles.size)
        assertEquals(2, two.circles.size)
        val half = CrosshairState().sizeDp / 2f
        two.circles.forEach { ring ->
            assertFalse("الحلقة مجرَّدة لا معبَّأة", ring.filled)
            assertTrue("الحلقة خارج نصف الحجم", ring.radius > half)
            assertEquals(two.centerX, ring.centerX, 0f)
            assertEquals(two.centerY, ring.centerY, 0f)
        }
        assertTrue("الحلقة الثانية أبعد", two.circles[1].radius > two.circles[0].radius)
    }

    @Test
    fun `rings stay inside the window even at the farthest offset`() {
        val geometry = crosshairGeometry(
            CrosshairState(style = CrosshairStyle.CROSS, rings = CrosshairState.MAX_RINGS, offsetX = 1f, offsetY = 1f),
            width = 400f, height = 320f, density = 1f
        )
        val nearestEdge = minOf(
            geometry.centerX,
            geometry.centerY,
            400f - geometry.centerX,
            320f - geometry.centerY
        )

        assertTrue("الحلقات موجودة عند أقصى إزاحة", geometry.circles.isNotEmpty())
        geometry.circles.forEach { ring ->
            assertTrue("الحلقة داخل النافذة: ${ring.radius} <= $nearestEdge", ring.radius <= nearestEdge)
        }
    }

    @Test
    fun `ring count is clamped into its published range`() {
        assertEquals(CrosshairState.MAX_RINGS, CrosshairState(rings = 9).sanitized().rings)
        assertEquals(CrosshairState.MIN_RINGS, CrosshairState(rings = -4).sanitized().rings)
    }
}
