/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **الحدّ بين سجلّ الاستعمال وإعدادات المستخدم.**
 *
 * `ScreenUsageStore` يقرأ من ملفّ التفضيلات `"settings"` — وهو الملفّ نفسه الذي يحمل وضع
 * MAX AI والثيم وكل اختيار آخر. فلا يوجد ملفّ منفصل يحمي أحدهما من الآخر: **الحاجز الوحيد
 * هو البادئة `screen_uses_`**. ولو انزلق هذا الحاجز يومًا، ظهر «max_ai_mode» في «أكثر ما
 * تفتحه» — أي أن إعدادًا يُقرأ كشاشة، ويُفتح بالنقر عليه مسارًا لا وجود له.
 *
 * ولهذا تُقاس الفلترة هنا كدالّة صافية ([ScreenUsageStore.countsFrom]) بلا `SharedPreferences`
 * ولا Android: الحارس الذي يحتاج جهازًا لا يُشغَّل في كل دفعة.
 */
class ScreenUsageStoreTest {

    @Test
    fun `only prefixed keys become routes`() {
        val counts = ScreenUsageStore.countsFrom(
            mapOf(
                "screen_uses_gpustudio" to 7,
                "screen_uses_hub_thermal" to 2,
            ),
        )
        assertEquals(mapOf("gpustudio" to 7, "hub_thermal" to 2), counts)
    }

    @Test
    fun `a settings key never reaches the suggestions`() {
        // هذان مفتاحان حقيقيّان في الملفّ نفسه — ولن يظهرا أبدًا كشاشتين.
        val counts = ScreenUsageStore.countsFrom(
            mapOf(
                "max_ai_mode" to 3,
                "theme_mode" to 1,
                "screen_uses_zram" to 4,
            ),
        )
        assertEquals(mapOf("zram" to 4), counts)
        assertTrue("مفتاح إعدادات تسرّب إلى سجلّ الشاشات", "max_ai_mode" !in counts)
        assertTrue("مفتاح إعدادات تسرّب إلى سجلّ الشاشات", "theme_mode" !in counts)
    }

    @Test
    fun `the prefix alone is not a route`() {
        // `screen_uses_` بلا مسار: مفتاح ناقص أو مكتوب بيد — لا يُعرض كشاشة اسمها فارغ.
        val counts = ScreenUsageStore.countsFrom(mapOf("screen_uses_" to 9))
        assertTrue("بادئة بلا مسار صارت وجهة: $counts", counts.isEmpty())
    }

    @Test
    fun `a value that is not a count is not read as one`() {
        // القيمة غير العددية لا تُقرأ صفرًا ولا تُقرأ عددًا: تُهمَل، لأن قراءتها اختراع.
        val counts = ScreenUsageStore.countsFrom(
            mapOf(
                "screen_uses_broken" to "12",
                "screen_uses_flag" to true,
                "screen_uses_null" to null,
                "screen_uses_real" to 5,
            ),
        )
        assertEquals(mapOf("real" to 5), counts)
    }

    @Test
    fun `counts are read as stored, with no ceiling applied on the way out`() {
        // السقف (`CEILING = 10_000`) يخصّ الكتابة لا القراءة: ما كُتب يُقرأ كما هو، وإلا
        // صار العدّاد يكذب على من يعرضه.
        val counts = ScreenUsageStore.countsFrom(
            mapOf(
                "screen_uses_top" to 10_000,
                "screen_uses_beyond" to 12_345,
                "screen_uses_zero" to 0,
            ),
        )
        assertEquals(10_000, counts["top"])
        assertEquals(12_345, counts["beyond"])
        assertEquals(0, counts["zero"])
    }

    @Test
    fun `an empty or unrelated store yields nothing at all`() {
        assertTrue(ScreenUsageStore.countsFrom(emptyMap()).isEmpty())
        assertTrue(ScreenUsageStore.countsFrom(mapOf("other_key" to 1)).isEmpty())
    }
}
