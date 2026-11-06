/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ترميز إقرار المخاطر — يُختبر هنا لأن الخطأ الصامت فيه يعني **إقرارًا ضائعًا** أو **إقرارًا
 * مزيّفًا**، وكلاهما لا يظهر إلّا عند فتح ميزة خطرة على جهاز.
 */
class DangerConsentTest {

    @Test
    fun a_round_trip_preserves_ids_and_times() {
        val original = mapOf(DangerConsent.RESTORE_DANGEROUS to 1_700_000_000_000L, "other" to 42L)

        assertEquals(original, DangerConsent.decode(DangerConsent.encode(original)))
    }

    @Test
    fun encoding_is_sorted_so_the_stored_text_is_stable() {
        val text = DangerConsent.encode(mapOf("b" to 2L, "a" to 1L))

        assertEquals("a=1;b=2", text)
    }

    @Test
    fun blank_and_null_mean_no_consent_at_all() {
        assertTrue(DangerConsent.decode(null).isEmpty())
        assertTrue(DangerConsent.decode("").isEmpty())
        assertTrue(DangerConsent.decode("   ").isEmpty())
    }

    @Test
    fun malformed_entries_are_dropped_rather_than_thrown() {
        // مدخل بلا فاصل · معرّف فارغ · زمن غير رقمي · زمن صفر أو سالب.
        val decoded = DangerConsent.decode("junk;=5;ok=abc;zero=0;neg=-7;good=99")

        assertEquals(mapOf("good" to 99L), decoded)
    }

    @Test
    fun a_repeated_id_keeps_the_first_time_not_the_last() {
        // الإقرار الأول هو الدليل؛ إعادة الكتابة تجعل «أقررت في» يكذب.
        assertEquals(mapOf("x" to 10L), DangerConsent.decode("x=10;x=20"))
    }

    @Test
    fun nothing_invalid_survives_encoding() {
        assertEquals("", DangerConsent.encode(mapOf("" to 5L, " " to 5L, "ok" to 0L, "no" to -1L)))
    }

    @Test
    fun an_encoded_blank_id_is_not_resurrected_by_decoding() {
        val text = "  =12"

        assertFalse(DangerConsent.decode(text).isNotEmpty())
    }
}
