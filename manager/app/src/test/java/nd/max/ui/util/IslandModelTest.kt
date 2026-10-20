/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * الجزيرة الديناميكية — **القرار وحده**، مقيس على JVM بلا أندرويد وبلا نافذة.
 *
 * والعقود الأربعة التي تحكم `IslandModel` محفورة هنا فلا يحرّكها أحد بضغطة عابرة:
 * **الأولوية مرتّبة** (عابر ← وسائط ← شحن ← مؤشرات) · **العابر يسقط بمؤقّت والحالة لا تسقط**
 * · **المنتهي لا يحجب ما بعده** · **مؤشرات بلا قيمة ليست محتوى**.
 *
 * والقياس على النموذج النقيّ وحده مقصود: الكبسولة فوق التطبيقات لا تُختبر بلمسة، وقرارها
 * يُختبر هنا — وما يبقى بعد ذلك (نافذة، مستمع، بطارية) له حدّه المعلن «يحتاج جهازًا».
 */
class IslandModelTest {

    private val base = 1_000_000L

    private fun notice(
        title: String = "مكالمة فائتة",
        importance: Int = IslandModel.IMPORTANCE_DEFAULT,
        postedAtMs: Long = base
    ) = IslandNotice(
        appLabel = "الهاتف",
        title = title,
        text = "تفصيل",
        postedAtMs = postedAtMs,
        importance = importance
    )

    @Test
    fun `a fresh notice outranks every live source`() {
        val inputs = IslandInputs(
            notice = notice(),
            media = IslandContent.Media("مقطع", "فنان"),
            charging = IslandContent.Charging(42),
            indicators = IslandContent.Indicators(87, 23, 38f)
        )
        val state = IslandModel.decide(inputs, base)
        assertTrue("العابر حدث وقع الآن، والباقي حالات قائمة", state.content is IslandContent.Transient)
        assertEquals(base + 5_000L, state.expiresAtMs)
    }

    @Test
    fun `when the notice expires the live source below it takes over`() {
        val inputs = IslandInputs(notice = notice(), media = IslandContent.Media("مقطع", "فنان"))
        val expired = IslandModel.decide(inputs, base + 5_000L)
        assertEquals(
            "المنتهي لا يحجب ما بعده: الوسائط تعمل الآن وهي أهمّ من إشعار نُسي",
            IslandContent.Media("مقطع", "فنان"),
            expired.content
        )
        assertNull("والحالة لا مؤقّت لها: تنتهي بانتهاء مصدرها", expired.expiresAtMs)
    }

    @Test
    fun `ttl follows importance — 8s high, 5s default, 3s below`() {
        assertEquals(8_000L, IslandModel.ttlMsForImportance(5))
        assertEquals(8_000L, IslandModel.ttlMsForImportance(4))
        assertEquals(5_000L, IslandModel.ttlMsForImportance(3))
        assertEquals(3_000L, IslandModel.ttlMsForImportance(2))
        assertEquals(3_000L, IslandModel.ttlMsForImportance(1))
        assertEquals("وغير المصنّف يُعامل كالأدنى لا يُسقط", 3_000L, IslandModel.ttlMsForImportance(-1000))
    }

    @Test
    fun `the expiry boundary is strict — a notice dies at its deadline`() {
        val inputs = IslandInputs(notice = notice(importance = 2))
        assertTrue(IslandModel.decide(inputs, base + 2_999L).content is IslandContent.Transient)
        assertEquals(
            "عند اللحظة نفسها يسقط: لا يعيش إشعار ثانيةً بلا مدّة معلَنة",
            IslandContent.Hidden,
            IslandModel.decide(inputs, base + 3_000L).content
        )
    }

    @Test
    fun `charging outranks indicators, and indicators come last`() {
        val withCharging = IslandInputs(
            charging = IslandContent.Charging(99),
            indicators = IslandContent.Indicators(60, 12, 31f)
        )
        assertEquals(IslandContent.Charging(99), IslandModel.decide(withCharging, base).content)

        val onlyIndicators = IslandInputs(indicators = IslandContent.Indicators(60, 12, 31f))
        assertEquals(
            IslandContent.Indicators(60, 12, 31f),
            IslandModel.decide(onlyIndicators, base).content
        )
    }

    @Test
    fun `indicators with no reading at all are not content`() {
        val empty = IslandInputs(indicators = IslandContent.Indicators(null, null, null))
        assertEquals(
            "كبسولة بلا رقم تُقرأ عطبًا لا هدوءًا — فالمصدر بلا قيمة يسقط",
            IslandContent.Hidden,
            IslandModel.decide(empty, base).content
        )
        val partial = IslandInputs(indicators = IslandContent.Indicators(null, 23, null))
        assertEquals(
            "ومن قيس له بعض القيم يُعرض بقيّة ما قيس لا أكثر",
            IslandContent.Indicators(null, 23, null),
            IslandModel.decide(partial, base).content
        )
    }

    @Test
    fun `nothing at all means hidden, and hidden carries no deadline`() {
        val state = IslandModel.decide(IslandInputs(), base)
        assertEquals(IslandContent.Hidden, state.content)
        assertNull(state.expiresAtMs)
        assertEquals("والافتراضيّ نفسه", IslandState(), state)
    }

    @Test
    fun `the stay multiplier stretches and shortens the transient only`() {
        val inputs = IslandInputs(
            notice = notice(importance = 3),
            media = IslandContent.Media("مقطع", "فنان")
        )
        // عادي: 5 ثوانٍ من وقت النشر.
        assertEquals(base + 5_000L, IslandModel.decide(inputs, base, 1f).expiresAtMs)
        // طويل: ×1.6 ⇒ 8 ثوانٍ، والوسائط خلفه لا تزال في مكانها.
        val long = IslandModel.decide(inputs, base, IslandPrefs.Duration.Long.scale)
        assertEquals(base + 8_000L, long.expiresAtMs)
        assertEquals("والوسائط لا مؤقّت لها: مضاعف العابر لا يمسّ الحالات", null, IslandModel.decide(IslandInputs(media = inputs.media), base, 1.6f).expiresAtMs)
        // قصير: ×0.6 ⇒ 3 ثوانٍ — وعند اللحظة نفسها يسقط إلى الوسائط.
        val short = IslandPrefs.Duration.Short.scale
        assertEquals(base + 3_000L, IslandModel.decide(inputs, base, short).expiresAtMs)
        assertEquals(
            inputs.media,
            IslandModel.decide(inputs, base + 3_000L, short).content
        )
        // ومضاعف صفر أو سالب يُطرح كأنه غير موجود: لا إشعار ميّت في لحظة عرضه.
        assertEquals(base + 5_000L, IslandModel.decide(inputs, base, 0f).expiresAtMs)
        assertEquals(base + 5_000L, IslandModel.decide(inputs, base, -3f).expiresAtMs)
    }
}
