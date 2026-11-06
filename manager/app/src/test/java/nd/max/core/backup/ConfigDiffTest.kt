/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * فرق الاستعادة — يُثبَّت هنا لأن الخطأ الصامت فيه أسوأ من غيابه:
 * فرقٌ يعرض تغييرًا لن يقع يُخيف، وفرقٌ يُسقط تغييرًا سيقع يخدع.
 */
class ConfigDiffTest {

    private val a = "persist.sys.maxmanager.a"
    private val b = "persist.sys.maxmanager.b"

    @Test
    fun an_identical_value_is_not_a_change() {
        val changes = ConfigDiff.diff(mapOf(a to "1"), mapOf(a to "1"))
        assertTrue(changes.isEmpty())
    }

    @Test
    fun a_different_value_is_reported_with_both_sides() {
        val changes = ConfigDiff.diff(mapOf(a to "1"), mapOf(a to "2"))
        assertEquals(1, changes.size)
        assertEquals(a, changes[0].key)
        assertEquals("1", changes[0].from)
        assertEquals("2", changes[0].to)
    }

    @Test
    fun an_unset_property_has_no_previous_value_rather_than_an_empty_one() {
        val changes = ConfigDiff.diff(mapOf(a to ""), mapOf(a to "7"))
        assertEquals(1, changes.size)
        assertNull(changes[0].from)
        assertEquals("7", changes[0].to)
    }

    @Test
    fun a_key_absent_from_the_device_counts_as_a_change() {
        val changes = ConfigDiff.diff(emptyMap(), mapOf(b to "on"))
        assertEquals(1, changes.size)
        assertNull(changes[0].from)
        assertEquals(b, changes[0].key)
    }

    @Test
    fun keys_only_on_the_device_are_not_changes() {
        // الاستعادة تكتب ولا تمحو، فما على الجهاز وحده لا يُمثّل فرقًا يُعرض.
        val changes = ConfigDiff.diff(mapOf(b to "on"), emptyMap())
        assertTrue(changes.isEmpty())
    }

    @Test
    fun an_empty_incoming_value_is_skipped_like_the_restore_skips_it() {
        val changes = ConfigDiff.diff(mapOf(a to "1"), mapOf(a to ""))
        assertTrue(changes.isEmpty())
    }

    @Test
    fun the_result_is_ordered_by_key() {
        val changes = ConfigDiff.diff(
            current = mapOf(a to "1", b to "1"),
            incoming = mapOf(b to "2", a to "2"),
        )
        assertEquals(listOf(a, b), changes.map { it.key })
    }
}
