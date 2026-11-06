/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import nd.max.ui.util.ConfigBackupInventory.PrefValue
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * عدّ مفاتيح التفضيلات المختلفة — الحساب الذي يقوله الحوار قبل الدمج.
 *
 * الخطأ الذي يُمسك هنا: عدّ الملفّ كاملًا فيُخيف بعدد لا يعني شيئًا، أو إسقاط
 * ملفّ جديد على الجهاز فتُعلن إضافته استبدالًا.
 */
class PrefsDeltaTest {

    @Test
    fun identical_bundles_have_no_difference() {
        val bundle = mapOf("a" to mapOf("k" to PrefValue.Int32(1)))
        assertEquals(0, PrefsDelta.count(bundle, bundle))
    }

    @Test
    fun a_changed_value_is_one_difference() {
        val incoming = mapOf("a" to mapOf("k" to PrefValue.Int32(2)))
        val current = mapOf("a" to mapOf("k" to PrefValue.Int32(1)))
        assertEquals(1, PrefsDelta.count(incoming, current))
    }

    @Test
    fun a_key_missing_from_the_device_counts_as_a_difference() {
        val incoming = mapOf("a" to mapOf("k" to PrefValue.Flag(true), "j" to PrefValue.Text("x")))
        val current = mapOf("a" to mapOf("k" to PrefValue.Flag(true)))
        assertEquals(1, PrefsDelta.count(incoming, current))
    }

    @Test
    fun an_unreadable_device_counts_only_the_incoming_keys() {
        val incoming = mapOf("a" to mapOf("k" to PrefValue.Text("x"), "j" to PrefValue.Whole(2L)))
        assertEquals(2, PrefsDelta.count(incoming, null))
    }

    @Test
    fun keys_only_on_the_device_are_not_counted() {
        // الدمج لا يمحو: ما على الجهاز وحده لا يتغيّر بالاستعادة.
        val incoming = mapOf("a" to mapOf("k" to PrefValue.Text("x")))
        val current = mapOf("a" to mapOf("k" to PrefValue.Text("x"), "extra" to PrefValue.Int32(9)))
        assertEquals(0, PrefsDelta.count(incoming, current))
    }

    @Test
    fun a_file_missing_on_the_device_counts_its_keys_once() {
        val incoming = mapOf(
            "a" to mapOf("k" to PrefValue.Text("x")),
            "b" to mapOf("j" to PrefValue.Int32(1), "i" to PrefValue.Decimal(1.5)),
        )
        val current = mapOf("a" to mapOf("k" to PrefValue.Text("x")))
        assertEquals(2, PrefsDelta.count(incoming, current))
    }
}
