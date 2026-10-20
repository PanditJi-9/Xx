/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import nd.max.core.platform.FpsMonitorUtil
import nd.max.core.platform.FpsReadMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ترتيب مصادر الإطارات وتهدئة العطب — الجزء النقيّ من `FpsMonitorUtil.getFps()`.
 *
 * **ولماذا اختبار عليه:** المصدر الثاني (`dumpsys SurfaceFlinger --timestats`) **ثقيل**، وكان يُسأل
 * في **كل ثانية** على جهاز لا تُجيب فيه معاملة SurfaceFlinger القديمة — واللوحة العائمة تعمل بلحظة
 * اللعب تحديدًا. فالجدول هنا يُثبّت: من يُجرَّب أوّلًا، ومن يُستثنى بعد عطب، وأنّ جهازًا أُخفق فيه
 * الجميع **لا يُنفق نداءً واحدًا** حتى تنتهي التهدئة (والواجهة تُعرض شرطة: `null` لا صفر).
 */
class FpsMonitorCooldownTest {

    private fun cooldowns(vararg pairs: Pair<FpsReadMode, Long>): LongArray {
        val arr = LongArray(FpsReadMode.entries.size)
        pairs.forEach { (mode, until) -> arr[mode.ordinal] = until }
        return arr
    }

    @Test
    fun `the configured source comes first when nothing has answered yet`() {
        val order = FpsMonitorUtil.sourcesToTry(
            preferred = null,
            configured = FpsReadMode.KERNEL_NODE,
            retryAtMs = cooldowns(),
            nowMs = 1_000L,
        )
        assertEquals(FpsReadMode.KERNEL_NODE, order.first())
        assertEquals("every source stays reachable as a fallback", FpsReadMode.entries.size, order.size)
    }

    @Test
    fun `the source that answered last time is promoted`() {
        val order = FpsMonitorUtil.sourcesToTry(
            preferred = FpsReadMode.DUMPSYS_TIMESTATS,
            configured = FpsReadMode.SURFACEFLINGER,
            retryAtMs = cooldowns(),
            nowMs = 1_000L,
        )
        assertEquals(FpsReadMode.DUMPSYS_TIMESTATS, order.first())
        assertEquals(FpsReadMode.SURFACEFLINGER, order[1])
    }

    @Test
    fun `a source that just failed is skipped for the whole cooldown`() {
        val order = FpsMonitorUtil.sourcesToTry(
            preferred = FpsReadMode.SURFACEFLINGER,
            configured = FpsReadMode.SURFACEFLINGER,
            retryAtMs = cooldowns(FpsReadMode.SURFACEFLINGER to 20_000L),
            nowMs = 5_000L,
        )
        assertTrue(
            "the failed source is not retried inside its cooldown",
            order.none { it == FpsReadMode.SURFACEFLINGER },
        )
        assertEquals("the remaining sources still run", 2, order.size)
    }

    @Test
    fun `when everything is cooling down no call is made at all`() {
        val order = FpsMonitorUtil.sourcesToTry(
            preferred = FpsReadMode.SURFACEFLINGER,
            configured = FpsReadMode.SURFACEFLINGER,
            retryAtMs = cooldowns(
                FpsReadMode.SURFACEFLINGER to 20_000L,
                FpsReadMode.KERNEL_NODE to 20_000L,
                FpsReadMode.DUMPSYS_TIMESTATS to 20_000L,
            ),
            nowMs = 10_000L,
        )
        assertTrue("an empty list means zero shell commands this second", order.isEmpty())
    }

    @Test
    fun `a cooldown that expired is tried again — the gate is a delay, not a verdict`() {
        val order = FpsMonitorUtil.sourcesToTry(
            preferred = FpsReadMode.SURFACEFLINGER,
            configured = FpsReadMode.SURFACEFLINGER,
            retryAtMs = cooldowns(FpsReadMode.SURFACEFLINGER to 20_000L),
            nowMs = 20_000L,
        )
        assertEquals(FpsReadMode.SURFACEFLINGER, order.first())
    }
}
