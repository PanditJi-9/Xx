/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رياضيات موضع التراكب — كانت داخل `OverlayWindow` بلا اختبار، وصارت هنا.
 *
 * أخطر ما يُقاس: نافذة خرجت عن الشاشة **لا تُسحَب عودة** على أغلب الأجهزة، فخطأ في [maxOffset]
 * يعني تراكبًا مفقودًا إلى الأبد بلا أيّ إشارة للمستخدم.
 */
class OverlayGeometryTest {

    // ───────────────────────────── أقصى إزاحة ─────────────────────────────

    @Test fun maxOffsetIsScreenMinusWindow() {
        assertEquals(640, OverlayGeometry.maxOffset(screen = 1080, size = 440))
    }

    @Test fun aWindowWiderThanTheScreenGivesZeroNotANegative() {
        // السالب هنا هو العطب: `coerceIn(0, سالب)` يرمي، ونافذة بإزاحة سالبة تخرج بلا رجعة.
        assertEquals(0, OverlayGeometry.maxOffset(screen = 400, size = 440))
        assertEquals(0, OverlayGeometry.maxOffset(screen = 1080, size = 1080))
    }

    // ───────────────────────────── الحبس ─────────────────────────────

    @Test fun clampKeepsTheWindowOnScreen() {
        assertEquals(0, OverlayGeometry.clamp(-50, screen = 1080, size = 440))
        assertEquals(640, OverlayGeometry.clamp(9999, screen = 1080, size = 440))
        assertEquals(300, OverlayGeometry.clamp(300, screen = 1080, size = 440))
    }

    @Test fun clampAcceptsTheExactEdges() {
        assertEquals(0, OverlayGeometry.clamp(0, screen = 1080, size = 440))
        assertEquals(640, OverlayGeometry.clamp(640, screen = 1080, size = 440))
    }

    @Test fun anUnmeasuredWindowIsLeftWhereItIsNotThrown() {
        // `view.width` صفر قبل أوّل تخطيط — حال حقيقيّ عند الالتقاط المبكر. ولا يُسحب إلى الأصل:
        // جرّ نافذة قبل قياسها يقفز بها إلى الزاوية، وهو أسوأ من تركها. والمهمّ ألّا يرمي
        // (`coerceIn(0, سالب)` يرمي) وأن يبقى المدى صالحًا.
        assertEquals(500, OverlayGeometry.clamp(500, screen = 1080, size = 0))
        assertEquals(0, OverlayGeometry.clamp(-5, screen = 1080, size = 0))
    }

    // ───────────────────────────── الحافة الأقرب ─────────────────────────────

    @Test fun snapGoesToTheNearestEdgeByCenter() {
        // مركز النافذة قبل المنتصف ⇒ يسار.
        assertEquals(0, OverlayGeometry.snapTarget(offset = 100, screen = 1080, size = 440))
        // مركزها بعد المنتصف ⇒ يمين (أقصى إزاحة).
        assertEquals(640, OverlayGeometry.snapTarget(offset = 600, screen = 1080, size = 440))
    }

    @Test fun snapDecidesByTheCenterNotTheLeadingEdge() {
        // حافّتها اليسرى (700) بعد المنتصف، لكن مركزها (920) أيضًا بعده ⇒ يمين. والقرار بالمركز.
        assertEquals(640, OverlayGeometry.snapTarget(offset = 700, screen = 1080, size = 440))
        // ونافذة حافّتها اليسرى في المنتصف تمامًا لكن مركزها بعده ⇒ يمين لا يسار.
        assertEquals(640, OverlayGeometry.snapTarget(offset = 320, screen = 1080, size = 440))
    }

    @Test fun anUnmeasuredWindowIsNotSnappedAnywhere() {
        // بلا عرض لا مركز ⇒ لا تخمين، تبقى حيث هي.
        assertEquals(777, OverlayGeometry.snapTarget(offset = 777, screen = 1080, size = 0))
        assertEquals(0, OverlayGeometry.snapTarget(offset = 0, screen = 1080, size = 0))
    }

    @Test fun snapIsIdempotent() {
        val left = OverlayGeometry.snapTarget(offset = 0, screen = 1080, size = 440)
        assertEquals(left, OverlayGeometry.snapTarget(offset = left, screen = 1080, size = 440))
        val right = OverlayGeometry.snapTarget(offset = 640, screen = 1080, size = 440)
        assertEquals(right, OverlayGeometry.snapTarget(offset = right, screen = 1080, size = 440))
    }

    @Test fun aFullWidthWindowHasNowhereToSnap() {
        // عرض == الشاشة ⇒ أقصى إزاحة صفر ⇒ الحافتان نقطة واحدة، ولا حركة.
        assertEquals(0, OverlayGeometry.snapTarget(offset = 0, screen = 1080, size = 1080))
    }

    // ───────────────────────── تجنّب النتوء (بند §2) ─────────────────────────

    private val topNotch = OverlayGeometry.Cutout(left = 420, top = 0, right = 660, bottom = 90)

    @Test fun withoutCutoutsTheWindowKeepsItsPlace() {
        assertEquals(
            100 to 200,
            OverlayGeometry.avoidCutout(100, 200, 320, 140, 1080, 2340, emptyList())
        )
    }

    @Test fun aClearWindowIsNotMovedAtAll() {
        // يمين النتوء وأسفله — لا تقاطع، فلا حركة (القاعدة: من لا يحتاج لا يُحرَّك).
        assertEquals(
            800 to 200,
            OverlayGeometry.avoidCutout(800, 200, 280, 140, 1080, 2340, listOf(topNotch))
        )
        // وتحت حافّته السفلى تمامًا (y = 90) لا تقاطع: الحافّات المتلاصقة ليست تقاطعًا.
        assertEquals(
            200 to 90,
            OverlayGeometry.avoidCutout(200, 90, 320, 140, 1080, 2340, listOf(topNotch))
        )
    }

    @Test fun aWindowUnderTheTopNotchSlidesDownByTheCheapestStep() {
        // من (380, 10): الإزاحة اليمنى/اليسرى نحو ٢٨٠ بكسل، والسفلى ٨٠ فقط — فالسفلى تفوز.
        assertEquals(
            380 to 90,
            OverlayGeometry.avoidCutout(380, 10, 320, 140, 1080, 2340, listOf(topNotch))
        )
    }

    @Test fun aWindowOnASideNotchSlidesRight() {
        // نتوء على الحافة اليسرى في العرض الأفقي — والنافذة تحته.
        val sideNotch = OverlayGeometry.Cutout(left = 0, top = 400, right = 90, bottom = 680)
        assertEquals(
            90 to 500,
            OverlayGeometry.avoidCutout(10, 500, 320, 140, 2340, 1080, listOf(sideNotch))
        )
    }

    @Test fun aBottomNotchPushesUpWhenNothingElseFits() {
        val bottomNotch = OverlayGeometry.Cutout(left = 420, top = 2250, right = 660, bottom = 2340)
        assertEquals(
            380 to 2110,
            OverlayGeometry.avoidCutout(380, 2140, 320, 140, 1080, 2340, listOf(bottomNotch))
        )
    }

    @Test fun aWindowWithNowhereToGoStaysExactlyWhereItIs() {
        // شريط نتوء بعرض الشاشة، والنافذة أطول من الفراغ تحته: كل الإزاحات تخرج عن الشاشة.
        // القاعدة: إزاحة تُخرج النافذة أسوأ من تداخل جزئيّ مرئيّ — فتُترك مكانها بلا ادّعاء حلّ.
        val band = OverlayGeometry.Cutout(left = 0, top = 0, right = 700, bottom = 200)
        assertEquals(
            0 to 100,
            OverlayGeometry.avoidCutout(0, 100, 700, 900, 700, 1000, listOf(band))
        )
    }

    @Test fun afterAvoidanceNoCutoutIsIntersectedAndNothingLeavesTheScreen() {
        // خاصية لا مثال واحد: من أيّ موضع مبدئيّ، إمّا لا حركة أصلًا أو موضع لا يتقاطع مع أيّ
        // قصّة ويبقى داخل الشاشة — وهذا ما يمنع «إصلاح نتوء بإنشاء نافذة ضائعة».
        val cutouts = listOf(
            OverlayGeometry.Cutout(left = 420, top = 0, right = 660, bottom = 90),
            OverlayGeometry.Cutout(left = 0, top = 0, right = 90, bottom = 200),
            OverlayGeometry.Cutout(left = 0, top = 800, right = 90, bottom = 1080)
        )
        val width = 300
        val height = 120
        for (x in 0..780 step 60) {
            for (y in 0..960 step 60) {
                val (nx, ny) = OverlayGeometry.avoidCutout(x, y, width, height, 1080, 2340, cutouts)
                assertTrue(
                    "خرج عن الشاشة من ($x,$y): $nx,$ny",
                    nx >= 0 && ny >= 0 && nx + width <= 1080 && ny + height <= 2340
                )
                val intersects = cutouts.any { c ->
                    nx < c.right && nx + width > c.left && ny < c.bottom && ny + height > c.top
                }
                val moved = nx != x || ny != y
                assertFalse("تقاطع بعد الحركة من ($x,$y) إلى ($nx,$ny)", moved && intersects)
            }
        }
    }
}
