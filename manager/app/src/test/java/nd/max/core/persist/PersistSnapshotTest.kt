/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.persist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ترميز اللقطة وفكّه — الجزء الوحيد الذي يُختبر بلا جهاز، وهو أيضًا الجزء الذي
 * يفسد صامتًا: لقطة لا تُقرأ تعني زرّ تراجع لا يفعل شيئًا، أو أسوأ: يكتب قيمًا
 * من لقطة قديمة على جهاز تغيّر.
 */
class PersistSnapshotTest {

    @Test
    fun a_snapshot_round_trips_with_its_time() {
        val values = linkedMapOf(
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor" to "schedutil",
            "/proc/sys/vm/swappiness" to "100"
        )
        val decoded = PersistSnapshot.decode(PersistSnapshot.encode(values, 1_700_000_000_000L))
        assertEquals(1_700_000_000_000L, decoded!!.atMs)
        assertEquals(values, decoded.values)
    }

    @Test
    fun another_format_version_is_rejected_not_guessed() {
        val text = """{"v":99,"at":1700000000000,"values":{"/a":"1"}}"""
        assertNull(PersistSnapshot.decode(text))
    }

    @Test
    fun only_absolute_paths_with_a_value_survive_decoding() {
        val text = """{"v":1,"at":1700000000000,"values":{"/a":"1","relative":"2","/blank":"  "}}"""
        val decoded = PersistSnapshot.decode(text)!!
        assertEquals(mapOf("/a" to "1"), decoded.values)
    }

    @Test
    fun an_empty_bag_is_no_snapshot_at_all() {
        val text = """{"v":1,"at":1700000000000,"values":{}}"""
        assertNull(PersistSnapshot.decode(text))
        assertNull(PersistSnapshot.decode(null))
        assertNull(PersistSnapshot.decode(""))
    }

    @Test
    fun a_garbage_string_returns_null_instead_of_throwing() {
        assertNull(PersistSnapshot.decode("not json at all"))
        assertNull(PersistSnapshot.decode("""{"v":1}"""))
    }

    @Test
    fun encoding_is_capped_so_the_preference_cannot_grow_without_bound() {
        val big = (1..(PersistSnapshot.MAX_ENTRIES + 20)).associate { "/node/$it" to "v$it" }
        val decoded = PersistSnapshot.decode(PersistSnapshot.encode(big, 1L))!!
        assertEquals(PersistSnapshot.MAX_ENTRIES, decoded.values.size)
        assertTrue(decoded.values.containsKey("/node/1"))
    }
}
