/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LobbyDeckMathTest {
    private val eps = 0.0001f

    @Test
    fun `the focused card has an identity pose at every depth`() {
        for (depth in LobbyDepth.entries) {
            val pose = LobbyDeckMath.pose(0f, depth)
            assertEquals(1f, pose.scale, eps)
            assertEquals(1f, pose.alpha, eps)
            assertEquals(0f, pose.rotationY, eps)
            assertEquals(0f, pose.tuck, eps)
        }
    }

    @Test
    fun `neighbours shrink fade and tuck monotonically with distance`() {
        val near = LobbyDeckMath.pose(1f, LobbyDepth.Deep)
        val mid = LobbyDeckMath.pose(2f, LobbyDepth.Deep)
        val far = LobbyDeckMath.pose(3f, LobbyDepth.Deep)
        assertTrue(near.scale > mid.scale && mid.scale > far.scale)
        assertTrue(near.alpha > mid.alpha && mid.alpha > far.alpha)
        assertTrue(near.tuck < mid.tuck && mid.tuck < far.tuck)
    }

    @Test
    fun `the pose is mirrored across the centre`() {
        val left = LobbyDeckMath.pose(-1.4f, LobbyDepth.Deep)
        val right = LobbyDeckMath.pose(1.4f, LobbyDepth.Deep)
        assertEquals(left.scale, right.scale, eps)
        assertEquals(left.alpha, right.alpha, eps)
        assertEquals(left.tuck, right.tuck, eps)
        assertEquals(-left.rotationY, right.rotationY, eps)
        assertTrue(right.rotationY > 0f)
    }

    @Test
    fun `flat depth never tilts or tucks`() {
        for (slot in listOf(-3f, -1f, -0.4f, 0.4f, 1f, 2.5f)) {
            val pose = LobbyDeckMath.pose(slot, LobbyDepth.Flat)
            assertEquals(0f, pose.rotationY, eps)
            assertEquals(0f, pose.tuck, eps)
        }
    }

    @Test
    fun `soft sits between flat and deep`() {
        val flat = LobbyDeckMath.pose(1f, LobbyDepth.Flat)
        val soft = LobbyDeckMath.pose(1f, LobbyDepth.Soft)
        val deep = LobbyDeckMath.pose(1f, LobbyDepth.Deep)
        assertTrue(flat.rotationY < soft.rotationY && soft.rotationY < deep.rotationY)
        assertTrue(flat.tuck < soft.tuck && soft.tuck < deep.tuck)
        assertTrue(flat.scale > soft.scale && soft.scale > deep.scale)
    }

    @Test
    fun `tilt is fully reached one card away and not beyond`() {
        val one = LobbyDeckMath.pose(1f, LobbyDepth.Deep)
        val two = LobbyDeckMath.pose(2f, LobbyDepth.Deep)
        assertEquals(one.rotationY, two.rotationY, eps)
        assertTrue(LobbyDeckMath.pose(0.5f, LobbyDepth.Deep).rotationY < one.rotationY)
    }

    @Test
    fun `cards beyond the reach keep the last pose and are nearly invisible`() {
        val reach = LobbyDeckMath.pose(LobbyDeckMath.REACH, LobbyDepth.Deep)
        val beyond = LobbyDeckMath.pose(9f, LobbyDepth.Deep)
        assertEquals(reach, beyond)
        assertTrue(beyond.alpha < 0.15f)
    }

    @Test
    fun `emphasis and draw order favour the centre`() {
        assertEquals(1f, LobbyDeckMath.emphasis(0f), eps)
        assertEquals(0.5f, LobbyDeckMath.emphasis(-0.5f), eps)
        assertEquals(0f, LobbyDeckMath.emphasis(1f), eps)
        assertEquals(0f, LobbyDeckMath.emphasis(4f), eps)
        assertTrue(LobbyDeckMath.zOrder(0f) > LobbyDeckMath.zOrder(1f))
        assertEquals(LobbyDeckMath.zOrder(-2f), LobbyDeckMath.zOrder(2f), eps)
    }

    private fun game(pkg: String, label: String) = GameApp(pkg, label, detectedGame = true)

    @Test
    fun `favorites sort puts favourites first then names`() {
        val games = listOf(game("c.pkg", "Cat"), game("a.pkg", "apple"), game("b.pkg", "Bee"))
        val sorted = LobbyOrder.sort(games, setOf("c.pkg"), LobbySort.Favorites, emptyMap())
        assertEquals(listOf("c.pkg", "a.pkg", "b.pkg"), sorted.map { it.packageName })
    }

    @Test
    fun `name sort ignores favourites and letter case`() {
        val games = listOf(game("c.pkg", "Cat"), game("a.pkg", "apple"), game("b.pkg", "Bee"))
        val sorted = LobbyOrder.sort(games, setOf("c.pkg"), LobbySort.Name, emptyMap())
        assertEquals(listOf("a.pkg", "b.pkg", "c.pkg"), sorted.map { it.packageName })
    }

    @Test
    fun `newest sort uses install age and puts unknown ages last`() {
        val games = listOf(game("old", "Old"), game("new", "New"), game("unknown", "Unknown"))
        val ages = mapOf("old" to 400, "new" to 2)
        val sorted = LobbyOrder.sort(games, emptySet(), LobbySort.Newest, ages)
        assertEquals(listOf("new", "old", "unknown"), sorted.map { it.packageName })
    }

    @Test
    fun `ties fall back to the package name so the order is stable`() {
        val games = listOf(game("z.pkg", "Same"), game("a.pkg", "Same"))
        val sorted = LobbyOrder.sort(games, emptySet(), LobbySort.Name, emptyMap())
        assertEquals(listOf("a.pkg", "z.pkg"), sorted.map { it.packageName })
    }
}
