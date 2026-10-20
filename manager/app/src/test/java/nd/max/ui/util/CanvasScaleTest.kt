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
 * §9.5 — **التحويل يُقاس هنا**: «720p» ليست 0.50 دائمًا، بل حاصل قسمة على ضلع الشاشة القصير.
 *
 * والقياس على النموذج النقيّ وحده مقصود: ما تبقّى بعد ذلك — أن واجهة Game Mode في Android
 * تقبل المعامل وتُصغّر فعلًا — **يحتاج جهازًا** ولا يُدَّعى هنا («لا تعتبر تغيير قيمة داخل
 * الواجهة دليلًا على أن خفض الدقة يعمل» — نصّ الطلب).
 */
class CanvasScaleTest {

    @Test
    fun `720p is computed from the screen, not a fixed 0_50`() {
        // شاشة 1080: 720/1080 = 0.67
        val on1080 = canvasScaleOptions(1080).first { it.label == CanvasScaleLabel.ShortSide(720) }
        assertEquals("0.67", on1080.stored)
        // شاشة 1440: 720/1440 = 0.50 — الاسم نفسه، ومعامل مختلف
        val on1440 = canvasScaleOptions(1440).first { it.label == CanvasScaleLabel.ShortSide(720) }
        assertEquals("0.50", on1440.stored)
        // وهذا هو الافتراض الذي نفاه الطلب صريحًا (‏§9.5-5)
        assertTrue(on1080.stored != on1440.stored)
    }

    @Test
    fun `a resolution matching the screen is excluded with a reason, never shown dead`() {
        val on1080 = canvasScaleOptions(1080)
        assertEquals(
            CanvasScaleSkip.MatchesScreen,
            on1080.first { it.label == CanvasScaleLabel.ShortSide(1080) }.skip
        )
        // وعلى شاشة 720: المقترحان 720 و1080 كلاهما «الأصلية أو أكبر» — يُعلَن السبب لا يُخفى الخيار
        val on720 = canvasScaleOptions(720)
        assertEquals(CanvasScaleSkip.MatchesScreen, on720.first { it.label == CanvasScaleLabel.ShortSide(720) }.skip)
        assertEquals(CanvasScaleSkip.MatchesScreen, on720.first { it.label == CanvasScaleLabel.ShortSide(1080) }.skip)
        assertTrue(on720.first { it.label == CanvasScaleLabel.ShortSide(480) }.supported)
        // و«الأصلية» دائمًا مدعومة وهي القيمة `default` نفسها التي يقرؤها مسار التطبيق
        assertEquals("default", on1080.first { it.label == CanvasScaleLabel.Native }.stored)
    }

    @Test
    fun `an old stored factor keeps its row and its meaning`() {
        // قيمة من الواجهة القديمة (٠٫٥٥) لا تسقط من القائمة، وتُعرض نسبتها لا رقمها المجرّد
        val custom = canvasScaleOptions(1080, includeStored = "0.55").last()
        assertEquals("0.55", custom.stored)
        assertEquals(CanvasScaleLabel.Percent(55), custom.label)
        assertTrue(custom.supported)
        // وقيمة مطابقة لمقترح قائم تُدمج فيه — لا صفّ مكرّر بمعنيين
        assertEquals(1, canvasScaleOptions(1080, includeStored = "0.67").count { it.stored == "0.67" })
    }

    @Test
    fun `below-range downscale is marked, and an unknown screen invents nothing`() {
        // شاشة عريضة جدًّا: 480p تهبط تحت نطاق الدعم (0.30) فتُعلَّم بالسبب
        assertEquals(
            CanvasScaleSkip.BelowRange,
            canvasScaleOptions(2000).first { it.label == CanvasScaleLabel.ShortSide(480) }.skip
        )
        // ودقة شاشة غير معروفة: «الأصلية» وحدها — لا مقترحٌ حسابُه باطل يُخترع
        assertEquals(listOf("default"), canvasScaleOptions(0).map { it.stored })
    }
}
