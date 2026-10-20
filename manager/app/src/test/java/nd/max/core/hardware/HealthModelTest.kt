/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **نموذج عدّادات الصحّة** (`MAX-CONTROL-01` §١٠.١) — يُقاس على JVM في أجزاء الثانية:
 * المجال من المفتاح، والتصنيف من نتيجة التحكّم، والترميز الحتميّ، والسقوف.
 *
 * وأهمّ ما يحرسه هذا الملفّ: **ألّا يُصنَّف عملٌ مجهول نجاحًا** — وهي القاعدة التي يقوم عليها
 * معنى «صحّة التحكّم» كلّه (ADR-07: لا نقول نجحنا ولا نقول فشلنا).
 */
class HealthModelTest {

    @Test
    fun `a knob key lands in its own domain`() {
        assertEquals("cpu", HealthModel.domainOf("cpu_limits:policy0"))
        assertEquals("cpu", HealthModel.domainOf("cpu_boost"))
        assertEquals("gpu", HealthModel.domainOf("gpu_frequency:kgsl-3d0"))
        assertEquals("memory", HealthModel.domainOf("mem_frequency:dmc"))
        assertEquals("storage", HealthModel.domainOf("ufs_frequency:1d84000.ufshc"))
        assertEquals("audio", HealthModel.domainOf("audio_stream:0"))
        assertEquals("charging", HealthModel.domainOf("charging_limit"))
        assertEquals("spoof", HealthModel.domainOf("spoof_engine:model"))
        assertEquals("thermal", HealthModel.domainOf("thermal_zone:cpu"))
        assertEquals("display", HealthModel.domainOf("display_refresh"))
        assertEquals("touch", HealthModel.domainOf("touch_rate"))
        // وما لا يُعرف يُنسب إلى `other` ولا يُسقط — ولا يُخترع له مجال.
        assertEquals("other", HealthModel.domainOf("mystery_knob"))
        assertEquals("other", HealthModel.domainOf(""))
    }

    @Test
    fun `an uppercase key never invents a second domain`() {
        assertEquals("cpu", HealthModel.domainOf("CPU_LIMITS:policy0"))
        assertEquals("gpu", HealthModel.domainOf("GPU_frequency:x"))
    }

    @Test
    fun `the rollback verdict beats a successful write`() {
        // كتابة نجحت ثم أُعيد خط الأساس: إلغاء لا تنفيذ — والترتيب مقصود لا صدفة.
        assertEquals(
            HealthModel.Outcome.ROLLED_BACK,
            HealthModel.classify(applied = true, verified = true, rollbackAttempted = true),
        )
        assertEquals(
            HealthModel.Outcome.ROLLED_BACK,
            HealthModel.classify(applied = false, verified = false, rollbackAttempted = true),
        )
    }

    @Test
    fun `an unverified write is unknown never success`() {
        assertEquals(
            HealthModel.Outcome.CONFIRMED,
            HealthModel.classify(applied = true, verified = true, rollbackAttempted = false),
        )
        assertEquals(
            HealthModel.Outcome.UNKNOWN,
            HealthModel.classify(applied = true, verified = false, rollbackAttempted = false),
        )
        assertEquals(
            HealthModel.Outcome.UNKNOWN,
            HealthModel.classify(applied = false, verified = false, rollbackAttempted = false),
        )
    }

    @Test
    fun `the message is deterministic and drops empty domains`() {
        val counts = mapOf(
            "gpu" to HealthModel.Counters(confirmed = 2),
            "cpu" to HealthModel.Counters(confirmed = 1, rolledBack = 1),
            "other" to HealthModel.Counters(),
        )
        val encoded = HealthModel.encode(counts)
        assertEquals(
            "{\"cpu\":{\"confirmed\":1,\"rolled_back\":1,\"unknown\":0}," +
                "\"gpu\":{\"confirmed\":2,\"rolled_back\":0,\"unknown\":0}}",
            encoded,
        )
        // الترتيب أبجديّ والحتميّة قائمة: نفس المدخل ⇒ نفس البايتات بالضبط.
        assertEquals(encoded, HealthModel.encode(counts))
    }

    @Test
    fun `no counters means an empty object not a fake zero`() {
        assertEquals("{}", HealthModel.encode(emptyMap()))
        assertEquals("{}", HealthModel.encode(mapOf("cpu" to HealthModel.Counters())))
    }

    @Test
    fun `an unknown domain never crosses the wire`() {
        // مجال غير معلوم في الذاكرة يُسقط عند الترميز — وقائمة الخادم هي الحدّ نفسه.
        assertFalse(HealthModel.encode(mapOf("evil" to HealthModel.Counters(confirmed = 5))).contains("evil"))
    }

    @Test
    fun `counters are capped before leaving the device`() {
        val encoded = HealthModel.encode(
            mapOf(
                "cpu" to HealthModel.Counters(
                    confirmed = Long.MAX_VALUE,
                    rolledBack = -3,
                    unknown = HealthModel.MAX_COUNT + 10,
                ),
            ),
        )
        assertEquals(
            "{\"cpu\":{\"confirmed\":${HealthModel.MAX_COUNT}," +
                "\"rolled_back\":0,\"unknown\":${HealthModel.MAX_COUNT}}}",
            encoded,
        )
    }

    @Test
    fun `a sum of extreme counters saturates instead of wrapping negative`() {
        // العطب الذي كشفه تشغيل QV: جمعٌ يلتفّ سالبًا فيُقرأ «لا أعداد» ويُسقط المجال بصمت.
        val extreme = HealthModel.Counters(
            confirmed = Long.MAX_VALUE,
            rolledBack = 0,
            unknown = 1_000_000_000,
        )
        assertEquals(Long.MAX_VALUE, extreme.total)
        assertTrue(extreme.total > 0)
        assertEquals("{\"cpu\":{\"confirmed\":${HealthModel.MAX_COUNT}," +
            "\"rolled_back\":0,\"unknown\":${HealthModel.MAX_COUNT}}}",
            HealthModel.encode(mapOf("cpu" to extreme)))
    }

    @Test
    fun `adding one outcome never touches the other two`() {
        val base = HealthModel.Counters(confirmed = 1, rolledBack = 2, unknown = 3)
        assertEquals(HealthModel.Counters(2, 2, 3), base.plus(HealthModel.Outcome.CONFIRMED))
        assertEquals(HealthModel.Counters(1, 3, 3), base.plus(HealthModel.Outcome.ROLLED_BACK))
        assertEquals(HealthModel.Counters(1, 2, 4), base.plus(HealthModel.Outcome.UNKNOWN))
        assertEquals(6L, base.total)
        assertEquals(HealthModel.Counters(2, 4, 6), base.merge(base))
    }
}
