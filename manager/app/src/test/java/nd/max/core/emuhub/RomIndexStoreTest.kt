/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.core.emuhub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * فهرس الرفّ يُقاس هنا لأنه **الوحيد الذي يبقى بعد إغلاق التطبيق**: ملفّ في شجرة SAF لا يُعاد مشيه
 * عند كل فتح، فسطر مشوّه واحد يعني لعبةً تختفي بلا رسالة أو حجمًا كاذبًا يظهر في الواجهة.
 *
 * والحالات الثلاث التي تُكسر فيها الصيغة عادةً: اسم فيه فاصل، حجم لم يُقرأ، وسطر تالف.
 */
class RomIndexStoreTest {

    private fun file(
        uri: String = "content://tree/a.rom",
        parent: String = "content://tree",
        name: String = "a.rom",
        sizeBytes: Long? = 1024L,
    ) = RomFile(uri = uri, name = name, parent = parent, sizeBytes = sizeBytes)

    @Test
    fun a_file_survives_a_round_trip() {
        val original = file()

        assertEquals(original, RomIndexStore.decode(RomIndexStore.encode(original)))
    }

    @Test
    fun an_unknown_size_stays_null_instead_of_becoming_zero() {
        val encoded = RomIndexStore.encode(file(sizeBytes = null))

        assertTrue("الحقل الغائب يُكتب فارغًا لا صفرًا", encoded.endsWith("\u0001"))
        assertNull(RomIndexStore.decode(encoded)?.sizeBytes)
    }

    @Test
    fun a_name_containing_the_pipe_character_survives() {
        // الفاصل حرف تحكّم لا `|`: أسماء الألعاب تحمل `|` فعلًا، ولا تحمل `\u0001`.
        val original = file(name = "Sonic | Knuckles (USA).md")

        assertEquals(original, RomIndexStore.decode(RomIndexStore.encode(original)))
    }

    @Test
    fun a_broken_line_is_dropped_without_poisoning_the_rest() {
        val good = RomIndexStore.encode(file(uri = "content://tree/good.rom", name = "good.rom"))

        val decoded = RomIndexStore.decodeAll(
            setOf(good, "not-a-line", "a\u0001b", "\u0001\u0001\u0001\u0001")
        )

        assertEquals(listOf("content://tree/good.rom"), decoded.map { it.uri })
    }

    @Test
    fun duplicates_collapse_by_uri_and_order_is_deterministic() {
        val a = RomIndexStore.encode(file(uri = "content://tree/b.rom", name = "b.rom"))
        val b = RomIndexStore.encode(file(uri = "content://tree/a.rom", name = "a.rom"))
        val duplicateOfA = RomIndexStore.encode(file(uri = "content://tree/a.rom", name = "renamed.rom"))

        val decoded = RomIndexStore.decodeAll(setOf(a, b, duplicateOfA))

        // نفس الرابط مرّتين (اسم قديم واسم جديد) ⇒ عنصر واحد، والترتيب بالرابط لا بترتيب المجموعة.
        assertEquals(listOf("content://tree/a.rom", "content://tree/b.rom"), decoded.map { it.uri })
    }

    @Test
    fun the_cap_is_applied_after_sorting_so_the_result_is_not_order_dependent() {
        val lines = (0..RomIndexStore.MAX_FILES).map { i ->
            RomIndexStore.encode(file(uri = "content://tree/%05d.rom".format(i), name = "$i.rom"))
        }.toSet()

        val decoded = RomIndexStore.decodeAll(lines)

        assertEquals(RomIndexStore.MAX_FILES, decoded.size)
        // السقف بعد الترتيب: أول رابط أبجديًّا يبقى، والزائد يُسقَط حتمًا.
        assertEquals("content://tree/00000.rom", decoded.first().uri)
    }
}
