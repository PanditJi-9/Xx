/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * الحصص تُقاس هنا لأن الشكل يعتمد عليها اعتمادًا كاملًا: شريط يبدو «٤٠٪» وليس ٤٠٪ يكذب على
 * المستخدم في أوضح صورة — ولا استثناء يرميه، ولا أحد يلاحظه.
 */
class CpuStateDistributionTest {

    private fun state(khz: Long, ticks: Long) = CpuHardwareBackend.StateTime(khz, ticks)

    @Test
    fun no_counter_is_absence_not_zero_shares() {
        assertNull(CpuStateDistribution.shares(emptyList()))
        assertNull(CpuStateDistribution.shares(listOf(state(1_800_000, 0), state(1_200_000, 0))))
    }

    @Test
    fun shares_sum_to_one_and_divide_by_the_measured_total() {
        val shares = CpuStateDistribution.shares(
            listOf(state(1_800_000, 25), state(1_200_000, 75)),
        )!!

        assertEquals(2, shares.size)
        assertEquals(1f, shares.sumOf { it.share.toDouble() }.toFloat(), 0.0001f)
        assertEquals(0.25f, shares.first { it.frequencyKHz == 1_800_000L }.share, 0.0001f)
    }

    @Test
    fun rows_are_ordered_from_the_highest_frequency_down() {
        val shares = CpuStateDistribution.shares(
            listOf(state(1_200_000, 1), state(2_400_000, 1), state(400_000, 1)),
        )!!

        assertEquals(listOf(2_400_000L, 1_200_000L, 400_000L), shares.map { it.frequencyKHz })
    }

    @Test
    fun a_duplicated_frequency_row_is_merged_not_shown_twice() {
        val shares = CpuStateDistribution.shares(
            listOf(state(1_800_000, 10), state(1_800_000, 10), state(1_200_000, 20)),
        )!!

        assertEquals(2, shares.size)
        assertEquals(0.5f, shares.first { it.frequencyKHz == 1_800_000L }.share, 0.0001f)
    }

    @Test
    fun a_single_state_is_the_whole_time() {
        val shares = CpuStateDistribution.shares(listOf(state(2_000_000, 7)))!!

        assertEquals(1f, shares.single().share, 0f)
    }

    @Test
    fun noise_below_the_threshold_is_counted_and_declared_not_hidden_silently() {
        val shares = CpuStateDistribution.shares(
            listOf(
                state(2_000_000, 990),
                state(1_500_000, 5),
                state(1_000_000, 5),
            ),
        )!!
        val split = CpuStateDistribution.split(shares)

        assertEquals(1, split.visible.size)
        assertEquals(2, split.hiddenCount)
        assertEquals(0.01f, split.hiddenShare, 0.0001f)
    }

    @Test
    fun an_entirely_flat_distribution_reports_every_row_visible() {
        val shares = CpuStateDistribution.shares(
            listOf(state(2_000_000, 1), state(1_000_000, 1)),
        )!!
        val split = CpuStateDistribution.split(shares)

        assertEquals(2, split.visible.size)
        assertEquals(0, split.hiddenCount)
        assertTrue(split.hiddenShare == 0f)
    }

    @Test
    fun the_dominant_state_is_the_largest_share_not_the_highest_frequency() {
        val shares = CpuStateDistribution.shares(
            listOf(state(2_400_000, 10), state(1_200_000, 90)),
        )!!

        assertEquals(1_200_000L, CpuStateDistribution.dominant(shares)!!.frequencyKHz)
    }

    @Test
    fun a_state_never_visited_is_not_a_row_at_all() {
        // جدول OPP قد يُعلن ترددًا لم يُستخدم: صفّه بصفر ليس حالة، وإدخاله يضخّم «المخفيّ».
        val shares = CpuStateDistribution.shares(
            listOf(state(2_400_000, 0), state(1_200_000, 30), state(600_000, 10)),
        )!!

        assertEquals(listOf(1_200_000L, 600_000L), shares.map { it.frequencyKHz })
        assertEquals(1f, shares.sumOf { it.share.toDouble() }.toFloat(), 0.0001f)
    }

    @Test
    fun a_share_exactly_at_the_threshold_stays_visible() {
        val shares = CpuStateDistribution.shares(
            listOf(state(2_000_000, 99), state(1_000_000, 1)),
        )!!
        val split = CpuStateDistribution.split(shares)

        assertEquals(2, split.visible.size)
        assertEquals(0, split.hiddenCount)
    }

    @Test
    fun splitting_nothing_reports_nothing_rather_than_dividing() {
        val split = CpuStateDistribution.split(emptyList())

        assertEquals(0, split.visible.size)
        assertEquals(0, split.hiddenCount)
        assertEquals(0f, split.hiddenShare, 0f)
    }
}
