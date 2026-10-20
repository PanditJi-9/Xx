/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import kotlin.math.abs
import kotlin.math.max
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * هندسة لوحة «المكعّب» — تُقاس بلا جهاز لأن [CubeLayoutMath] رياضيات خالصة.
 *
 * ووُجدت أولًا لتثبيت **عطب مقيس**: `meterH = room.coerceIn(64f, u * 24f)` كان يرمي
 * `IllegalArgumentException: Cannot coerce value to an empty range` عند `u = 2.4`
 * (أي عرض أقل من ٥٠٧dp تقريبًا ⇒ `u * 24 = 57.6 < 64`) — فينهار **أول رسم** على أكثر الهواتف.
 * فأول ما يُقاس هنا ليس شكلًا ولا ترتيبًا بل **عدم الرمي** على شبكة أحجام كاملة.
 *
 * والقاعدة الباقية هي التي وُعد بها الملف نفسه: «الأعمدة تُوزَّع بعرض مضمون لا يتقاطع» —
 * فتُقاس المسافات بالأرقام (أضيق ما قِيس عبر الشبكة: ٦٫٤dp بين المقياسين، و٦dp بين العمود
 * الأيسر ومقياس CPU حين لا تكون الشاشة ضيّقة، و١٢dp بين الشبكة والحافة).
 */
class CubeLayoutTest {

    private val widths: List<Float> = (320..1400 step 60).map { it.toFloat() }
    private val heights: List<Float> = (240..2400 step 80).map { it.toFloat() }

    private inline fun eachLayout(
        body: (w: Float, h: Float, left: Boolean, wide: Boolean, layout: CubeLayout) -> Unit,
    ): Int {
        var cases = 0
        for (w in widths) {
            for (h in heights) {
                for (left in listOf(true, false)) {
                    for (wide in listOf(true, false)) {
                        cases++
                        body(w, h, left, wide, CubeLayoutMath.of(w, h, left, wide))
                    }
                }
            }
        }
        return cases
    }

    @Test
    fun `layout never throws and stays ordered across the full size sweep`() {
        val cases = eachLayout { w, h, left, wide, l ->
            val where = "${w.toInt()}x${h.toInt()} left=$left wide=$wide"
            val clampedW = max(w, 320f)

            // العطب المدفون الذي وُجد بهذا الاختبار: `u = 2.4` يجعل سقف المقياس ٥٧٫٦ تحت أرضيته ٦٤.
            assertTrue("$where: meterH=${l.meterH} u=${l.u}", l.meterH >= 64f && l.meterH <= max(64f, l.u * 24f))
            assertTrue("$where: u=${l.u}", l.u in 2.4f..8f)
            assertTrue("$where: topPad=${l.topPad} divider=${l.dividerY} top=${l.bodyTop} bottom=${l.bodyBottom}",
                l.topPad < l.dividerY && l.dividerY < l.bodyTop && l.bodyTop < l.bodyBottom)
            assertTrue("$where: bodyHeight=${l.bodyHeight}", l.bodyHeight > 0f)
            assertTrue("$where: gridX=${l.gridX} gridW=${l.gridW}", l.gridX >= 0f && l.gridX + l.gridW <= clampedW)
            assertTrue("$where: compact=${l.compact}", l.compact == CockpitModel.isCompact(clampedW))
        }
        assertTrue("the sweep must actually run: $cases", cases >= 1000)
    }

    @Test
    fun `cpu and gpu gauges never intersect`() {
        var narrowest = Float.MAX_VALUE
        eachLayout { w, h, left, wide, l ->
            val gap = l.gpuRange.start - l.cpuRange.endInclusive
            narrowest = minOf(narrowest, gap)
            assertTrue("gauges intersect at ${w.toInt()}x${h.toInt()} left=$left wide=$wide: gap=$gap",
                gap > 0f)
        }
        // مُقاس عبر الشبكة: أضيق فراغ ٦٫٤dp (عند ٣٢٠×٢٤٠) — فالحدّ هنا سقفُ انحدار لا وصفُ اليوم.
        assertTrue("narrowest=$narrowest", narrowest >= 4f)
    }

    @Test
    fun `the open left column never covers the cpu gauge unless the screen is compact`() {
        eachLayout { w, h, left, wide, l ->
            if (left && !l.compact) {
                assertTrue(
                    "left column covers cpu gauge at ${w.toInt()}x${h.toInt()} wide=$wide: leftRight=${l.leftRight} cpuStart=${l.cpuRange.start}",
                    l.cpuRange.start >= l.leftRight,
                )
            }
        }
    }

    @Test
    fun `the gpu gauge never enters the tile grid`() {
        // كان GPU مرآة CPU فيتراكب مع الشبكة بحتى ٢٨dp تحت ٨٠٠dp.
        eachLayout { w, h, left, wide, l ->
            if (!l.compact) {
                assertTrue(
                    "gpu gauge overlaps the grid at ${w.toInt()}x${h.toInt()} left=$left wide=$wide: " +
                        "gpuEnd=${l.gpuRange.endInclusive} gridX=${l.gridX}",
                    l.gpuRange.endInclusive <= l.gridX,
                )
            }
        }
    }

    @Test
    fun `the frequency rails stay between the gauges and never meet in the middle`() {
        var railCases = 0
        eachLayout { w, h, left, wide, l ->
            val where = "${w.toInt()}x${h.toInt()} left=$left wide=$wide"
            assertTrue("$where: compact must not draw rails", !(l.compact && l.railOn))
            assertTrue("$where: rail=${l.railW}", l.railW >= 10f)
            if (l.railOn) {
                railCases++
                // الشريط يلي عموده مباشرة، وحاويتا الشريطين بينهما وسط حرّ لا يقلّ عن ٤٠dp.
                assertTrue("$where: cpu rail inside its gauge", l.cpuRailX >= l.cpuRange.endInclusive)
                assertTrue("$where: gpu rail inside its gauge", l.gpuRailX + l.railBoxW <= l.gpuRange.start)
                assertTrue("$where: free middle", l.gpuRailX - (l.cpuRailX + l.railBoxW) >= 40f)
                assertTrue("$where: panels", l.leftPanelEdge < l.rightPanelEdge)
                assertTrue("$where: rail height", l.railH > 0f && l.railTop + l.railH <= l.bodyBottom + 0.01f)
            }
        }
        assertTrue("the sweep must include layouts with rails: $railCases", railCases > 100)
    }

    @Test
    fun `a phone in landscape gets the long rails and a wide enough left column`() {
        // هاتف عرضيّ شائع (٨٧٢×٣٩٣dp): الشريطان ظاهران، والعمود الأيسر يتّسع لاسم عربيّ وسطره الفرعي.
        val l = CubeLayoutMath.of(872f, 393f, leftOpen = true, wideLeft = false)
        assertTrue(l.railOn)
        assertTrue("leftW=${l.leftW}", l.leftW >= 116f)
        assertTrue("cardH=${l.cardH}", l.cardH >= 40f)
    }

    @Test
    fun `a phone in landscape gets a wide chevron rail that still leaves the middle open`() {
        // الشريط حزام عريض بقمّة نحو اللعبة (لا نقاط صغيرة على خطّ شبه مستقيم)، ويبقى بين حاويتيه وسطٌ حرّ.
        val l = CubeLayoutMath.of(872f, 393f, leftOpen = true, wideLeft = false)
        assertTrue(l.railOn)
        assertTrue("railW=${l.railW}", l.railW >= 18f)
        assertTrue("railBow=${l.railBow}", l.railBow >= 30f)
        val free = l.gpuRailX - (l.cpuRailX + l.railBoxW)
        assertTrue("free=$free", free >= CubeLayout.RAIL_MIN_FREE - 0.01f)
    }

    @Test
    fun `a bowed rail keeps the minimum free middle across the whole sweep`() {
        var bowed = 0
        eachLayout { w, h, left, wide, l ->
            if (l.railOn && l.railBow > 0f) {
                bowed++
                val free = l.gpuRailX - (l.cpuRailX + l.railBoxW)
                assertTrue(
                    "${w.toInt()}x${h.toInt()} left=$left wide=$wide: bow=${l.railBow} free=$free",
                    l.railBow >= CubeLayout.RAIL_MIN_BOW && free >= CubeLayout.RAIL_MIN_FREE - 0.01f,
                )
            }
        }
        assertTrue("the sweep must include bowed rails: $bowed", bowed > 100)
    }

    @Test
    fun `the stats lane sits under the buttons and above the divider across the sweep`() {
        eachLayout { _, _, _, _, l ->
            assertTrue("statsTop=${l.statsTop} buttonsEnd=${l.topPad + l.btn}", l.statsTop >= l.topPad + l.btn)
            assertTrue("statsEnd=${l.statsTop + l.statsH} divider=${l.dividerY}", l.statsTop + l.statsH <= l.dividerY)
        }
    }

    @Test
    fun `the status lane fits between the rail feet and the apex gap stays open`() {
        var checked = 0
        eachLayout { w, h, left, wide, l ->
            if (l.railOn) {
                checked++
                val where = "${w.toInt()}x${h.toInt()} left=$left wide=$wide"
                val apexGap = l.gpuRailX - (l.cpuRailX + l.railBoxW)
                // القدم أعرض من القمّة بقدر قوسَي الشريطين، فشريط الحالة السفلي لا يرتطم بقدمَي الشريطين.
                assertTrue("$where: foot=${l.bottomFreeW} apex=$apexGap bow=${l.railBow}",
                    abs(l.bottomFreeW - (apexGap + 2f * l.railBow)) < 0.01f)
                if (l.railBow > 0f) {
                    assertTrue("$where: apex=$apexGap", apexGap >= w * CubeLayout.APEX_GAP_RATIO - 0.01f)
                }
            }
        }
        assertTrue("the sweep must include rails: $checked", checked > 100)
        val phone = CubeLayoutMath.of(872f, 393f, leftOpen = true, wideLeft = false)
        assertTrue("foot=${phone.bottomFreeW}", phone.bottomFreeW >= 230f)
    }
}
