/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * نافذة الإطارات تُقاس على حدودها لأنها تُغذّي رقمًا يُعرض للمستخدم: «الثبات».
 *
 * والقاعدة التي يثبّتها هذا الملفّ: **الفارغ ليس صفرًا**. نافذة لم تُسجَّل بعد يجب أن تُعطي `null`
 * كي يعرض السطح «غير متاح» بدل أن يدّعي أن كل الإطارات سقطت.
 */
class FrameWindowTest {

    @Test
    fun an_empty_window_answers_null_not_zero() {
        val window = FrameWindow()

        assertEquals(0, window.samples)
        assertNull(window.mean())
        assertNull(window.percentile(0.5))
        assertNull(window.stableShare(60f))
    }

    @Test
    fun impossible_samples_are_ignored_instead_of_dragging_the_average_down() {
        val window = FrameWindow()

        window.record(null)
        window.record(Float.NaN)
        window.record(0f)
        window.record(-1f)

        assertEquals("لا عيّنة صالحة بعد", 0, window.samples)
        window.record(60f)
        assertEquals(1, window.samples)
        assertEquals(60f, window.mean()!!, 0.001f)
    }

    @Test
    fun the_oldest_sample_leaves_when_the_window_is_full() {
        val window = FrameWindow(capacity = 3)

        (1..4).forEach { window.record(it.toFloat()) }

        assertEquals(3, window.samples)
        assertEquals(2f, window.percentile(0.0)!!, 0.001f)
        assertEquals(4f, window.percentile(1.0)!!, 0.001f)
    }

    @Test
    fun the_mean_and_the_percentiles_describe_the_same_window() {
        val window = FrameWindow()
        (1..10).forEach { window.record(it * 10f) }

        assertEquals(55f, window.mean()!!, 0.001f)
        // الرتبة = ceil(p * n) مقيّدة ١..n: النصف عند ٥، والتاسع والتسعون عند ١٠.
        assertEquals(50f, window.percentile(0.5)!!, 0.001f)
        assertEquals(100f, window.percentile(0.95)!!, 0.001f)
    }

    @Test
    fun stability_counts_the_samples_near_the_target_not_the_average() {
        val window = FrameWindow()
        // الأرضية ٠٫٩ × ٦٠ = ٥٤: ثلاث عيّنات فوقها وواحدة تحتها.
        listOf(60f, 60f, 55f, 30f).forEach { window.record(it) }

        assertEquals(0.75f, window.stableShare(60f)!!, 0.001f)
    }

    @Test
    fun reset_empties_the_window_so_the_next_session_starts_clean() {
        val window = FrameWindow()
        window.record(60f)

        window.reset()

        assertEquals(0, window.samples)
        assertNull(window.mean())
    }
}
