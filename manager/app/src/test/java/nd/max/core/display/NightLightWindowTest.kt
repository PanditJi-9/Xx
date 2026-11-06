/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.display

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * قواعد نافذة الوضع الليلي — تُثبَّت هنا لأنها الحساب الوحيد الذي يتكرّر كل يوم.
 *
 * الخطأ الذي تُمسكه هذه الاختبارات هو خطأ منتصف الليل: نافذة ٢٢:٠٠–٠٧:٠٠ ليست
 * `start..end` عاديًّا، ومن يكتبها كذلك **يُطفئ الضوء في منتصف الليل بالضبط** —
 * عطب لا يظهر إلّا على جهاز بعد ساعات.
 */
class NightLightWindowTest {

    private val at22 = 22 * 60   // 1320
    private val at07 = 7 * 60    // 420

    @Test
    fun a_daytime_window_covers_only_its_hours() {
        val start = 9 * 60
        val end = 12 * 60

        assertFalse(NightLightWindow.isActive(8 * 60, start, end))
        assertTrue(NightLightWindow.isActive(10 * 60, start, end))
        assertFalse(NightLightWindow.isActive(13 * 60, start, end))
    }

    @Test
    fun the_start_minute_is_inside_and_the_end_minute_is_outside() {
        val start = 9 * 60
        val end = 12 * 60

        assertTrue(NightLightWindow.isActive(start, start, end))
        // النهاية خارجة: لو كانت داخلة لأُشعل الضوء وأُطفئ في الدقيقة نفسها.
        assertFalse(NightLightWindow.isActive(end, start, end))
    }

    @Test
    fun a_window_across_midnight_is_active_on_both_sides() {
        // قبل البداية بدقيقة واحدة: خارج النافذة. والاختبار الذي كان هنا وقع في
        // هذا الفخّ نفسه: ٢١:٥٩ ليست داخل ٢٢:٠٠–٠٧:٠٠، والذيل لا يُقرأ من الاسم.
        assertFalse(NightLightWindow.isActive(at22 - 1, at22, at07))
        assertTrue(NightLightWindow.isActive(at22 + 60, at22, at07))
        assertTrue(NightLightWindow.isActive(0, at22, at07))
        assertTrue(NightLightWindow.isActive(6 * 60 + 59, at22, at07))
        assertFalse(NightLightWindow.isActive(at07, at22, at07))
        assertFalse(NightLightWindow.isActive(12 * 60, at22, at07))
    }

    @Test
    fun equal_edges_mean_no_window_rather_than_a_whole_day() {
        // البديل (٢٤ ساعة) يُشعل الضوء بلا نهاية صامتًا، فالتفسير المحافظ هو «ملغاة».
        assertFalse(NightLightWindow.isActive(0, 60, 60))
        assertFalse(NightLightWindow.isActive(59, 60, 60))
    }

    @Test
    fun minutes_outside_a_day_are_rotated_into_it() {
        assertEquals(1410, NightLightWindow.normalise(-30))
        assertEquals(60, NightLightWindow.normalise(1500))
        assertEquals(0, NightLightWindow.normalise(NightLightWindow.MINUTES_PER_DAY))
    }

    @Test
    fun the_next_switch_moves_towards_start_when_outside_the_window() {
        // الساعة ١٣:٠٠ والنافذة ٢٢:٠٠–٠٧:٠٠ ⇒ الباقي إلى ٢٢:٠٠ هو ٩ ساعات.
        assertEquals(9 * 60, NightLightWindow.nextSwitch(13 * 60, at22, at07))
    }

    @Test
    fun the_next_switch_moves_towards_end_when_inside_the_window() {
        // الساعة ٢٣:٠٠ داخل النافذة ⇒ الباقي إلى ٠٧:٠٠ هو ٨ ساعات.
        assertEquals(8 * 60, NightLightWindow.nextSwitch(23 * 60, at22, at07))
        // الساعة ٠١:٠٠ داخل النافذة ⇒ الباقي إلى ٠٧:٠٠ هو ٦ ساعات.
        assertEquals(6 * 60, NightLightWindow.nextSwitch(60, at22, at07))
    }

    @Test
    fun the_next_switch_is_never_zero_and_never_more_than_a_day() {
        val edges = listOf(0, 1, 59, 60, 419, 420, 421, 720, 1319, 1320, 1321, 1439)
        edges.forEach { now ->
            val delta = NightLightWindow.nextSwitch(now, at22, at07)
            assertTrue("delta at $now was $delta", delta in 1..NightLightWindow.MINUTES_PER_DAY)
        }
    }
}
