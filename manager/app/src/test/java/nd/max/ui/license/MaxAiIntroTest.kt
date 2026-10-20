/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.license

import java.io.File
import nd.max.core.access.FeatureAccess
import nd.max.core.access.PromoState
import nd.max.core.remote.RemoteFlags
import nd.max.ui.design.MaxTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد **شاشة «ما يفعله Max AI»** (§٥): «أنشئ شاشة أو عرضًا بصريًّا احترافيًّا يعرّف المستخدم
 * بقدرات Max AI ويمنحه سببًا مقنعًا لاكتشاف الميزات المدفوعة» — بلا رقمٍ مكتوب بيد.
 *
 * والقياس على أربعة مستويات:
 *
 * 1. **الحلقة**: أربع مراحل، عناوينها وأسطرها مختلفة (لا نسخةٌ من سطر)، وبنبراتها المرسومة.
 * 2. **الجدول**: معرّفات الأسطح = [RemoteFlags.ALL] **في الاتجاهين** — فلا سطح يسقط ولا صفٌّ
 *    يُخترع، وكلها بلا تكرار.
 * 3. **المفردات**: [PromoState] تغطّي [FeatureAccess] تغطيةً متبادلة (٤ مقابل ٤، ولا حكمان
 *    يشتركان في حالة عرض) — فالشارة لا تقول «متاح» على حكمٍ لم يُتَّخذ.
 * 4. **الوصول والحياكة**: الشاشة **غير مقفلة** في الرسم الملاحيّ (وظيفتها أن تُشرح)، ولها بابان
 *    (شاشة المحرّك · لوح قفله)، ولا يُوجَّه إليها من قفل سطحٍ آخر، والحكم يُقرأ **لكل صفّ**
 *    من `RemoteAccess.access` لحظة العرض لا من نسخة محفوظة.
 *
 * **والحدّ المُعلَن:** «هل تقنع الشاشة فعلًا؟» و«هل تُقرأ بسلاسة مع RTL والشاشات الصغيرة؟» —
 * UAT على جهاز. والمقيس هنا: التطابق والربط، لا الذوق.
 */
class MaxAiIntroTest {

    // ── ① الحلقة ─────────────────────────────────────────────────────────────

    @Test
    fun `the loop is four distinct stages, and no two share a line`() {
        assertEquals(
            "الحلقة أربع مراحل كما في المحرّك: قراءة ← تشخيص ← تنفيذ ← تحقُّق",
            4,
            introStages.size,
        )
        assertEquals(
            "ولا سطران بعنوان واحد",
            introStages.size,
            introStages.map { it.titleRes }.toSet().size,
        )
        assertEquals(
            "ولا سطر شرحٍ مكرَّر",
            introStages.size,
            introStages.map { it.bodyRes }.toSet().size,
        )
        assertEquals(
            "ونبرات الرسم هي المرسومة: محايد ← لون الشاشة ← تنبيه ← مؤكَّد",
            listOf(MaxTone.Neutral, MaxTone.Accent, MaxTone.Caution, MaxTone.Positive),
            introStages.map { it.tone },
        )
    }

    // ── ② الجدول: تطابقٌ في الاتجاهين ────────────────────────────────────────

    @Test
    fun `the access table is exactly the flag registry — no missing row, no invented one`() {
        val rowIds = introSurfaces.map { it.flagId }
        assertEquals(
            "لا سطحٌ مسجَّل سقط من الجدول",
            RemoteFlags.ALL.map { it.id }.toSet(),
            rowIds.toSet(),
        )
        assertEquals(
            "ولا صفّان للمعرّف نفسه",
            rowIds.size,
            rowIds.toSet().size,
        )
        assertEquals(
            "وكل صفٍّ له اسمه من الموارد",
            introSurfaces.size,
            introSurfaces.map { it.nameRes }.toSet().size,
        )
    }

    // ── ③ المفردات: ترجمةٌ متبادلة بلا سقوط ──────────────────────────────────

    @Test
    fun `every verdict maps to one state and every state to one verdict`() {
        val mapped = FeatureAccess.entries.map { PromoState.of(it) }
        assertEquals(
            "لا حكمان يشتركان في شارةٍ واحدة",
            FeatureAccess.entries.size,
            mapped.toSet().size,
        )
        assertEquals(
            "ولا حالة عرضٍ بلا حكم: العددان متقابلان",
            PromoState.entries.size,
            FeatureAccess.entries.size,
        )
        assertEquals(
            "والترجمة بالمعنى لا بالموضع",
            mapOf(
                FeatureAccess.OPEN to PromoState.AVAILABLE,
                FeatureAccess.PROMO to PromoState.FREE_NOW,
                FeatureAccess.LOCKED to PromoState.NEEDS_KEY,
                FeatureAccess.OFF to PromoState.OFF,
            ),
            FeatureAccess.entries.associateWith { PromoState.of(it) },
        )
    }

    // ── ④ الوصول والحياكة ────────────────────────────────────────────────────

    @Test
    fun `the intro stays reachable without a licence, and reads a live verdict per row`() {
        val graph = sourceFile("ui/navigation/MaxNavGraph.kt").readText()
        assertTrue(
            "المسار مسجَّل في الرسم الملاحيّ",
            graph.contains("composable(MaxDestination.MaxAiIntro.route) { MaxAiIntroScreen(navController) }"),
        )
        assertFalse(
            "وغير ملفوفٍ ببوّابة ترخيص — من لا يملك مفتاحًا يقرأ ما وراءه قبل أن يقرّر",
            graph.contains("FeatureGate(RemoteFlags.MAXAI, navController) { MaxAiIntroScreen"),
        )

        val screen = sourceFile("ui/license/MaxAiIntroScreen.kt").readText()
        assertTrue(
            "وحكم كل صفٍّ يُقرأ لحظة العرض من السجلّ الحيّ لا من نسخة محفوظة",
            screen.contains("PromoState.of(RemoteAccess.access(surface.flagId))"),
        )
        assertTrue(
            "والإعلان (مجّانيّ حتى تاريخ) من نافذة اللوحة نفسها",
            screen.contains("snapshot.promo"),
        )

        val ai = sourceFile("ui/mainscreens/MaxAiScreen.kt").readText()
        assertTrue(
            "وللمحرّك بابٌ إلى الشاشة من داخله",
            ai.contains("MaxDestination.MaxAiIntro.launchRoute"),
        )

        val gate = sourceFile("ui/license/FeatureGate.kt").readText()
        assertTrue(
            "وللوح القفل بابٌ ثانٍ — **على سطح المحرّك وحده**",
            gate.contains("val intro = featureId == RemoteFlags.MAXAI") &&
                gate.contains("if (intro)") &&
                gate.contains("R.string.maxai_intro_gate_action"),
        )
    }

    // ── ⑤ النصوص: لغتان، ولا ثالثة ──────────────────────────────────────────

    @Test
    fun `every intro string exists in English and Arabic`() {
        val en = resFile("values/max_ai_strings.xml").readText()
        val ar = resFile("values-ar/max_ai_strings.xml").readText()
        val keys = listOf(
            "maxai_intro_title",
            "maxai_intro_subtitle",
            "maxai_intro_teaser",
            "maxai_intro_loop_title",
            "maxai_intro_loop_desc",
            "maxai_intro_stage1_title",
            "maxai_intro_stage1_body",
            "maxai_intro_stage2_title",
            "maxai_intro_stage2_body",
            "maxai_intro_stage3_title",
            "maxai_intro_stage3_body",
            "maxai_intro_stage4_title",
            "maxai_intro_stage4_body",
            "maxai_intro_access_title",
            "maxai_intro_access_desc",
            "maxai_intro_state_available",
            "maxai_intro_state_free",
            "maxai_intro_state_key",
            "maxai_intro_state_off",
            "maxai_intro_honesty_title",
            "maxai_intro_honest_measured",
            "maxai_intro_honest_readback",
            "maxai_intro_honest_unknown",
            "maxai_intro_honest_hardware",
            "maxai_intro_license_row",
            "maxai_intro_license_sub",
            "maxai_intro_gate_action",
            "maxai_intro_promo_title",
            "maxai_intro_promo_detail",
        )
        for (key in keys) {
            assertTrue("ناقص في الإنجليزية: $key", en.contains("name=\"$key\""))
            assertTrue("ناقص في العربية: $key", ar.contains("name=\"$key\""))
        }
        // قاعدة §0.2: لغتان لا ثالثة — ولا يُطالب هنا بمزامنة ٨٣ لغة أخرى (تُطلب بأمر «زامن»).
        val registry = sourceFile("ui/navigation/MaxDestinations.kt").readText()
        assertTrue(
            "واسم الوجهة في السجلّ من نصوص الشاشة نفسها لا نصًّا آخر",
            registry.contains("R.string.maxai_intro_title"),
        )
        assertTrue(
            "ودور الشاشة في سجلّ الأدوار له نصّه في اللغتين (لا الوصف العامّ لواجهة تحكّم)",
            resFile("values/strings.xml").readText().contains("name=\"max_role_maxai_intro\"") &&
                resFile("values-ar/strings.xml").readText().contains("name=\"max_role_maxai_intro\""),
        )
    }

    // ── أدوات القياس ─────────────────────────────────────────────────────────

    /** مسار ملفّ مصدر من جذر يعمل في الموضعين: CI من `manager/`، ومحلّيًّا من الجذر. */
    private fun sourceFile(relative: String): File {
        val candidates = listOf(
            File("src/main/java/nd/max/$relative"),
            File("app/src/main/java/nd/max/$relative"),
            File("../app/src/main/java/nd/max/$relative"),
            File("manager/app/src/main/java/nd/max/$relative"),
        )
        return candidates.firstOrNull { it.isFile } ?: error("لم يُوجد المصدر: $relative")
    }

    /** مسار ملفّ موارد — نفس المنطق على مجلّد `res`. */
    private fun resFile(relative: String): File {
        val candidates = listOf(
            File("src/main/res/$relative"),
            File("app/src/main/res/$relative"),
            File("../app/src/main/res/$relative"),
            File("manager/app/src/main/res/$relative"),
        )
        return candidates.firstOrNull { it.isFile } ?: error("لم يُوجد المورد: $relative")
    }
}
