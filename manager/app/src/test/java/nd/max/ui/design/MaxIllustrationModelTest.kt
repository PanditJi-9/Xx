/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MaxIllustrationModelTest {
    @Test
    fun `every domain has distinct bounded artwork`() {
        val drawings = MaxIllustrationKind.entries.map(::maxIllustrationParts)
        assertEquals(MaxIllustrationKind.entries.size, drawings.toSet().size)
        drawings.forEach { assertTrue(it.size in 2..32) }
        assertNotEquals(maxIllustrationParts(MaxIllustrationKind.Cpu), maxIllustrationParts(MaxIllustrationKind.Gpu))
        assertNotEquals(maxIllustrationParts(MaxIllustrationKind.Memory), maxIllustrationParts(MaxIllustrationKind.Storage))
    }

    @Test
    fun `all artwork fits its viewport with finite dimensions`() {
        fun coordinate(value: Float) { assertTrue("Invalid coordinate $value", value.isFinite() && value in 0f..1f) }
        MaxIllustrationKind.entries.flatMap(::maxIllustrationParts).forEach { part ->
            when (part) {
                is MaxIllustrationPart.Line -> {
                    assertTrue(part.points.size in 2..8)
                    if (part.closed) assertTrue(part.points.size >= 3)
                    part.points.forEach { coordinate(it.x); coordinate(it.y) }
                }
                is MaxIllustrationPart.Box -> {
                    coordinate(part.x); coordinate(part.y)
                    assertTrue(part.width > 0f && part.height > 0f)
                    coordinate(part.x + part.width); coordinate(part.y + part.height)
                    assertTrue(part.radius >= 0f && part.radius <= minOf(part.width, part.height) / 2f)
                }
                is MaxIllustrationPart.Ring -> {
                    assertTrue(part.radius > 0f)
                    coordinate(part.x - part.radius); coordinate(part.x + part.radius)
                    coordinate(part.y - part.radius); coordinate(part.y + part.radius)
                }
            }
        }
    }

    @Test
    fun `image vector styles do not change their domain`() {
        listOf("Filled", "Outlined", "Rounded", "AutoMirrored.Rounded").forEach { style ->
            assertEquals(MaxIllustrationKind.Network, maxIllustrationKind("$style.Wifi"))
            assertEquals(MaxIllustrationKind.Audio, maxIllustrationKind("$style.VolumeUp"))
            assertEquals(MaxIllustrationKind.Cpu, maxIllustrationKind("$style.Memory"))
        }
    }

    @Test
    fun `unknown icons retain the original glyph`() {
        assertNull(maxIllustrationKind(null))
        assertNull(maxIllustrationKind(""))
        assertNull(maxIllustrationKind("Rounded.Check"))
        assertNull(maxIllustrationKind("Rounded.Warning"))
        assertNull(maxIllustrationKind("Rounded.ArrowBack"))
    }

    @Test
    fun `zero is a measured value not an unreadable fill`() {
        assertEquals(0f, maxMeasuredFraction(0f, 100f)!!, 0f)
        assertEquals(0.22f, maxMeasuredFraction(22f, 100f)!!, 0.00001f)
        assertEquals(1f, maxMeasuredFraction(120f, 100f)!!, 0f)
    }

    @Test
    fun `missing or invalid readings do not produce a fraction`() {
        assertNull(maxMeasuredFraction(null, 100f))
        assertNull(maxMeasuredFraction(22f, null))
        assertNull(maxMeasuredFraction(-1f, 100f))
        assertNull(maxMeasuredFraction(22f, 0f))
        assertNull(maxMeasuredFraction(22f, -1f))
        assertNull(maxMeasuredFraction(Float.NaN, 100f))
        assertNull(maxMeasuredFraction(22f, Float.POSITIVE_INFINITY))
    }

    @Test
    fun `frequency fractions remain bounded for extreme finite readings`() {
        assertEquals(1f, maxMeasuredFraction(Float.MAX_VALUE, Float.MIN_VALUE)!!, 0f)
        assertEquals(0f, maxMeasuredFraction(Float.MIN_VALUE, Float.MAX_VALUE)!!, 0f)
    }

    @Test
    fun `a panel retains its declared portrait proportions`() {
        val panel = maxDisplayProportion(1220f, 2712f)!!
        assertEquals(1220f / 2712f, panel.width, 0.00001f)
        assertEquals(1f, panel.height, 0f)
    }

    @Test
    fun `landscape panels are not rotated into a generic phone`() {
        val panel = maxDisplayProportion(2712f, 1220f)!!
        assertEquals(1f, panel.width, 0f)
        assertEquals(1220f / 2712f, panel.height, 0.00001f)
    }

    @Test
    fun `unreadable panel dimensions produce no made up outline`() {
        assertNull(maxDisplayProportion(null, 2712f))
        assertNull(maxDisplayProportion(1220f, null))
        assertNull(maxDisplayProportion(0f, 2712f))
        assertNull(maxDisplayProportion(-1f, 2712f))
        assertNull(maxDisplayProportion(Float.NaN, 2712f))
        assertNull(maxDisplayProportion(1220f, Float.POSITIVE_INFINITY))
    }

    @Test
    fun `finite subnormal dimensions do not overflow during fitting`() {
        val tiny = maxDisplayProportion(Float.MIN_VALUE, Float.MIN_VALUE)!!
        assertEquals(MaxDisplayProportion(1f, 1f), tiny)
        val extreme = maxDisplayProportion(Float.MIN_VALUE, Float.MAX_VALUE)!!
        assertTrue(extreme.width.isFinite() && extreme.height.isFinite())
    }
}
