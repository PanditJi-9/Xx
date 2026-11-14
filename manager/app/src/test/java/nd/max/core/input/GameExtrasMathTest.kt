/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** نقاط الإيماءات تُقاس بلا جهاز: الحدود، والتنظيف، والتخزين، وكل إيماءة. */
class GameExtrasMathTest {

    @Test
    fun `النسبة تُقيَّد ولا تخرج عن الشاشة`() {
        assertEquals(GameExtrasMath.MAX_FRACTION, GameExtrasMath.clampFraction(-1f), 0f)
        assertEquals(GameExtrasMath.MAX_FRACTION, GameExtrasMath.clampFraction(3f), 0f)
        assertEquals(GameExtrasMath.MIN_FRACTION, GameExtrasMath.clampFraction(0f), 0f)
        assertEquals(0.5f, GameExtrasMath.clampFraction(Float.NaN), 0f)
        assertEquals(0.5f, GameExtrasMath.fractionOf(500f, 1000), 0f)
        assertEquals(0.5f, GameExtrasMath.fractionOf(10f, 0), 0f)
    }

    @Test
    fun `التحويل إلى بكسل يطابق النسبة`() {
        assertEquals(500f, GameExtrasMath.px(0.5f, 1000), 0f)
        assertEquals(500f, GameExtrasMath.px(0.5f, 1000), 0f)
    }

    @Test
    fun `التنظيف يحذف التكرار ويحدّ العدد`() {
        val duplicated = (0..9).map { id -> ExtraPoint(id = id % 3, x = 2f, y = -1f) }
        val clean = GameExtrasMath.sanitize(duplicated)
        assertEquals(3, clean.size)
        assertEquals(listOf(0, 1, 2), clean.map { it.id })
        assertTrue(clean.all { it.x == GameExtrasMath.MAX_FRACTION && it.y == GameExtrasMath.MIN_FRACTION })

        val many = (0..11).map { id -> ExtraPoint(id = id) }
        assertEquals(GameExtrasMath.MAX_POINTS, GameExtrasMath.sanitize(many).size)
    }

    @Test
    fun `أول معرّف فارغ يُعاد والباقي سالب`() {
        assertEquals(0, GameExtrasMath.nextId(emptyList()))
        assertEquals(1, GameExtrasMath.nextId(listOf(ExtraPoint(0))))
        assertEquals(-1, GameExtrasMath.nextId((0 until GameExtrasMath.MAX_POINTS).map { ExtraPoint(it) }))
    }

    @Test
    fun `كل إيماءة تُحوَّل إلى لمسة صحيحة`() {
        val tap = GameExtrasMath.actionFor(ExtraPoint(0, ExtraGesture.TAP, 0.5f, 0.5f), 1000, 2000)
        assertEquals(500f, tap.startX, 0f)
        assertEquals(1000f, tap.startY, 0f)
        assertEquals(tap.startX, tap.endX, 0f)
        assertEquals(GameExtrasMath.TAP_MS, tap.durationMs)
        assertEquals(1, tap.repeatCount)

        val double = GameExtrasMath.actionFor(ExtraPoint(0, ExtraGesture.DOUBLE_TAP, 0.5f, 0.5f), 1000, 2000)
        assertEquals(2, double.repeatCount)
        assertEquals(GameExtrasMath.DOUBLE_TAP_GAP_MS, double.repeatGapMs)

        val longPress = GameExtrasMath.actionFor(ExtraPoint(0, ExtraGesture.LONG_PRESS, 0.5f, 0.5f), 1000, 2000)
        assertEquals(GameExtrasMath.LONG_PRESS_MS, longPress.durationMs)

        val up = GameExtrasMath.actionFor(ExtraPoint(0, ExtraGesture.SWIPE_UP, 0.5f, 0.5f), 1000, 2000)
        assertTrue(up.endY < up.startY)
        val down = GameExtrasMath.actionFor(ExtraPoint(0, ExtraGesture.SWIPE_DOWN, 0.5f, 0.5f), 1000, 2000)
        assertTrue(down.endY > down.startY)
        val left = GameExtrasMath.actionFor(ExtraPoint(0, ExtraGesture.SWIPE_LEFT, 0.5f, 0.5f), 1000, 2000)
        assertTrue(left.endX < left.startX)
        val right = GameExtrasMath.actionFor(ExtraPoint(0, ExtraGesture.SWIPE_RIGHT, 0.5f, 0.5f), 1000, 2000)
        assertTrue(right.endX > right.startX)
    }

    @Test
    fun `السحب عند الحافة لا يخرج عن الشاشة`() {
        val top = GameExtrasMath.actionFor(ExtraPoint(0, ExtraGesture.SWIPE_UP, 0.5f, 0.04f), 1000, 2000)
        assertEquals(0f, top.endY, 0f)
        val bottom = GameExtrasMath.actionFor(ExtraPoint(0, ExtraGesture.SWIPE_DOWN, 0.5f, 0.96f), 1000, 2000)
        assertEquals(2000f, bottom.endY, 0f)
        val left = GameExtrasMath.actionFor(ExtraPoint(0, ExtraGesture.SWIPE_LEFT, 0.04f, 0.5f), 1000, 2000)
        assertEquals(0f, left.endX, 0f)
        val right = GameExtrasMath.actionFor(ExtraPoint(0, ExtraGesture.SWIPE_RIGHT, 0.96f, 0.5f), 1000, 2000)
        assertEquals(1000f, right.endX, 0f)
    }

    @Test
    fun `التخزين يعيد نفس النقاط ويحتمل المعطوب`() {
        val points = listOf(
            ExtraPoint(0, ExtraGesture.DOUBLE_TAP, 0.2f, 0.8f),
            ExtraPoint(3, ExtraGesture.SWIPE_UP, 0.5f, 0.5f),
        )
        assertEquals(points, GameExtrasMath.decode(GameExtrasMath.encode(points)))
        assertEquals(emptyList<ExtraPoint>(), GameExtrasMath.decode(null))
        assertEquals(emptyList<ExtraPoint>(), GameExtrasMath.decode("garbage"))
        assertEquals(emptyList<ExtraPoint>(), GameExtrasMath.decode("0,NOPE,0.2,0.8"))
        assertEquals(emptyList<ExtraPoint>(), GameExtrasMath.decode("9,TAP,0.2,0.8"))
    }

    @Test
    fun `الإيماءة التالية تدور على القائمة`() {
        var gesture = ExtraGesture.TAP
        repeat(ExtraGesture.entries.size) { gesture = gesture.next() }
        assertEquals(ExtraGesture.TAP, gesture)
        assertFalse(ExtraGesture.entries.isEmpty())
    }
}
