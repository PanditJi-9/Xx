/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * قواعد تحويل التردّد إلى قناة — تُثبَّت هنا لأنها الخطأ الصامت في خريطة القنوات.
 *
 * الإزاحة في ٢٫٤ غيغاهرتز ٢٤٠٧ وفي ٥ و٦ ليست كذلك؛ من يعمّم قاعدة واحدة يضع قناة ٣٦
 * على شبكة ٢٫٤ ويقرأ الخريطة مقلوبة بلا أن يفشل شيء.
 */
class WifiChannelTest {

    @Test
    fun every_two_four_channel_maps_from_its_frequency() {
        assertEquals(1, WifiChannel.channelOf(2412))
        assertEquals(6, WifiChannel.channelOf(2437))
        assertEquals(13, WifiChannel.channelOf(2472))
        assertEquals(14, WifiChannel.channelOf(2484))
    }

    @Test
    fun five_gigahertz_uses_its_own_base() {
        assertEquals(36, WifiChannel.channelOf(5180))
        assertEquals(40, WifiChannel.channelOf(5200))
        assertEquals(149, WifiChannel.channelOf(5745))
        assertEquals(WifiChannel.Band.FiveGhz, WifiChannel.bandOf(5745))
    }

    @Test
    fun six_gigahertz_starts_after_five() {
        assertEquals(1, WifiChannel.channelOf(5955))
        assertEquals(WifiChannel.Band.SixGhz, WifiChannel.bandOf(5955))
    }

    @Test
    fun frequencies_off_the_grid_are_not_rounded_into_a_channel() {
        assertNull(WifiChannel.channelOf(2400))
        assertNull(WifiChannel.channelOf(5183))
        assertNull(WifiChannel.channelOf(0))
        assertNull(WifiChannel.channelOf(7117))
    }

    @Test
    fun the_summary_counts_each_band_and_ranks_by_networks() {
        val summary = WifiChannel.summarise(
            listOf(2412, 2412, 2437, 5180, 5180, 5180, 5955),
            connectedFrequencyMhz = 5180,
        )
        assertEquals(7, summary.total)
        assertEquals(2, summary.twoFour)
        assertEquals(4, summary.five)
        assertEquals(1, summary.six)
        // الأكثر شبكاتٍ أولًا (٣٦ بـ٣)، ثم الأقل؛ وعند التساوي أصغر رقم قناة (١ قبل ٦).
        assertEquals(36, summary.busiest.first().channel)
        assertEquals(3, summary.busiest.first().networks)
        assertEquals(1, summary.busiest[1].channel)
        assertEquals(2, summary.busiest[1].networks)
        assertEquals(5180, summary.connected?.frequencyMhz)
        assertEquals(36, summary.connected?.channel)
    }

    @Test
    fun unreadable_frequencies_do_not_inflate_the_count() {
        val summary = WifiChannel.summarise(listOf(0, 2400, 2412))
        assertEquals(1, summary.total)
        assertNull(summary.connected)
    }
}
