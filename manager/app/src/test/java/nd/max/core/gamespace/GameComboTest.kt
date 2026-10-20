/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import nd.max.ui.subscreens.ComboKeysBySection
import nd.max.ui.util.AppConfig
import nd.max.ui.util.comboSnapshot
import nd.max.ui.util.comboValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §1.1 — **طبقة المجموعات تُقاس هنا** حيث يمكن: الترميز وفكّه، والعرض على الجهاز، وتغطية
 * المحرّر. وما لا يُقاس هنا — أن واجهات النظام تُصغّر فعلًا عند تشغيل اللعبة — يبقى حدًّا
 * معلنًا «يحتاج جهازًا»، ولا يُدَّعى لا في الطلب ولا هنا.
 */
class GameComboTest {

    @Test
    fun `the codec round-trips combos with names and entries`() {
        val combos = listOf(
            GameCombo("a1", "Racing", mapOf("gpu_profile" to "gaming", "touch_boost" to "true")),
            GameCombo("b2", "Battery", mapOf("perf_lite_mode" to "true", "refresh_rate" to "60")),
        )
        assertEquals(combos, GameComboCodec.decode(GameComboCodec.encode(combos)))
    }

    @Test
    fun `a corrupted line drops alone and the rest survives`() {
        val text = buildString {
            append("c\tone\tName one\n")
            append("k\tone\tgpu_profile\tgaming\n")
            append("garbage line without tabs\n")
            append("k\tone\ttouch_boost\ttrue\n")
            append("k\tghost\tperf_lite_mode\ttrue\n") // بلا سطر ترويسة: لا يُخترع له اسم
        }
        val decoded = GameComboCodec.decode(text)
        assertEquals(1, decoded.size)
        assertEquals("Name one", decoded.first().name)
        assertEquals(mapOf("gpu_profile" to "gaming", "touch_boost" to "true"), decoded.first().entries)
        // وملفّ فارغ أو تالف كليًّا: قائمة فارغة لا انفجار
        assertTrue(GameComboCodec.decode(null).isEmpty())
        assertTrue(GameComboCodec.decode("nonsense\nnonsense").isEmpty())
    }

    @Test
    fun `decode enforces its bounds on hand-edited storage`() {
        val long = "n".repeat(400)
        val capped = GameComboCodec.decode("c\tid\t$long")
        assertEquals(GameComboCodec.MAX_NAME_LENGTH, capped.single().name.length)

        val many = (1..40).associate { "k$it" to "v$it" }
        val encoded = GameComboCodec.encode(listOf(GameCombo("x", "Big", many)))
        assertEquals(GameComboCodec.MAX_ENTRIES, GameComboCodec.decode(encoded).single().entries.size)
    }

    @Test
    fun `vetting accepts known keys with known values`() {
        val combo = GameCombo("a", "x", mapOf("gpu_profile" to "gaming", "kill_bg_apps" to "true"))
        val verdict = GameComboVetting.vet(combo, emptyMap())
        assertEquals(2, verdict.acceptedCount)
        assertEquals(2, verdict.totalCount)
        assertTrue(verdict.unsupported.isEmpty())
        assertTrue(verdict.rejected.isEmpty())
    }

    @Test
    fun `vetting rejects unknown keys and values outside a key's own domain`() {
        // ثلاثة رفض بأسباب مختلفة: قيمة خارج مجال مفتاح معروف، ومفتاح خارج الكتالوج مرّتين
        // (أحدهما مفتاح `AppConfig` حقيقي لا تقبله المجموعة — الحدّ عند الكتالوج لا عند الملفّ).
        val combo = GameCombo(
            "a", "x",
            mapOf("gpu_profile" to "turbo", "teleport" to "true", "renderer" to "vulkan")
        )
        val verdict = GameComboVetting.vet(combo, emptyMap())
        assertEquals(0, verdict.acceptedCount)
        assertEquals(setOf("gpu_profile", "teleport", "renderer"), verdict.rejected.toSet())
        assertTrue(verdict.unsupported.isEmpty())
    }

    @Test
    fun `device keys are judged against the device and never accepted blind`() {
        val combo = GameCombo("a", "x", mapOf("refresh_rate" to "120", "resolution_downscale" to "0.67"))

        // ١) بلا قائمة قدرات معروفة: لا يُقبل شيء أعمى — يُعلَن غير مدعوم
        val blind = GameComboVetting.vet(combo, emptyMap())
        assertEquals(0, blind.acceptedCount)
        assertEquals(setOf("refresh_rate", "resolution_downscale"), blind.unsupported.toSet())

        // ٢) ومع قوائم الجهاز: المقبول منهما وحدهما
        val aware = GameComboVetting.vet(
            combo,
            mapOf(
                "refresh_rate" to setOf("60", "120", "default"),
                "resolution_downscale" to setOf("0.67", "0.44")
            )
        )
        assertEquals(2, aware.acceptedCount)

        // ٣) وجهاز يدعم أحدهما: النصف الآخر يُعلَن ولا يُسكَت عنه
        val partial = GameComboVetting.vet(combo, mapOf("refresh_rate" to setOf("120")))
        assertEquals(1, partial.acceptedCount)
        assertEquals(listOf("resolution_downscale"), partial.unsupported)
    }

    @Test
    fun `the editor mapping covers every catalog key exactly once`() {
        val covered = ComboKeysBySection.values.flatten()
        // لا مفتاح مرتين (وإلا كُتب مرّتين بنيّتين متضاربتين)، ولا مفتاح بلا موضع في المحرّر
        assertEquals(covered.size, covered.toSet().size)
        assertEquals(GameComboCatalog.ALL_KEYS.toSet(), covered.toSet())
        // وكل أقسام الملفّ موجودة، فلا يسقط قسم كامل من المحرّر صامتًا
        assertEquals(nd.max.ui.subscreens.GameProfileSection.entries.toSet(), ComboKeysBySection.keys)
    }

    @Test
    fun `snapshot takes only the fields the user actually customized`() {
        val config = AppConfig(gpu_profile = "gaming", touch_boost = "true")
        assertEquals(
            mapOf("gpu_profile" to "gaming", "touch_boost" to "true"),
            config.comboSnapshot(GameComboCatalog.ALL_KEYS)
        )
        // والقيمة المطفأة تُقرأ `default` من موضعها ولا تُخترع
        assertEquals("default", config.comboValue("kill_bg_apps"))
        assertEquals(emptyMap<String, String>(), AppConfig().comboSnapshot(GameComboCatalog.ALL_KEYS))
    }
}
