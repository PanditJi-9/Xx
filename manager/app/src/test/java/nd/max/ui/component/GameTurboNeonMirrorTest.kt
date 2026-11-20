/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.component

import androidx.compose.ui.text.style.TextAlign
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * انعكاس لوحة Game Turbo النيونية عند الالتصاق باليمين — يُقاس في JVM عادي.
 *
 * الوعد: قائمة التبويبات تلاصق حافة الهاتف في الجهتين؛ فعند الانعكاس تتبدّل محاذاة النصوص اليمنى واليسرى
 * (لتلاصق أيقوناتها المنعكسة) ويبقى الوسط، ومن دون انعكاس لا يتغيّر شيء عمّا في الصورة الأصل.
 */
class GameTurboNeonMirrorTest {

    @Test
    fun unmirroredPanelKeepsEveryAlignment() {
        assertEquals(TextAlign.Start, neonEdge(TextAlign.Start, mirror = false))
        assertEquals(TextAlign.End, neonEdge(TextAlign.End, mirror = false))
        assertEquals(TextAlign.Center, neonEdge(TextAlign.Center, mirror = false))
    }

    @Test
    fun mirroredPanelSwapsStartAndEnd() {
        assertEquals(TextAlign.End, neonEdge(TextAlign.Start, mirror = true))
        assertEquals(TextAlign.Start, neonEdge(TextAlign.End, mirror = true))
    }

    @Test
    fun mirroredPanelKeepsCentre() {
        assertEquals(TextAlign.Center, neonEdge(TextAlign.Center, mirror = true))
    }

    @Test
    fun mirroringTwiceIsTheIdentity() {
        for (a in listOf(TextAlign.Start, TextAlign.End, TextAlign.Center)) {
            assertEquals(a, neonEdge(neonEdge(a, mirror = true), mirror = true))
        }
    }
}
