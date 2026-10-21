/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.license

import java.io.File
import nd.max.core.access.FeatureAccess
import nd.max.core.access.PromoState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد **شاشة «ما يفعله Max AI»**: صورةٌ واحدة هي شجرة القدرات ([MaxAiCapabilityTree])، بلا نصوصٍ
 * ولا جدولٍ ولا صفِّ ترخيص (طلب المالك ٢٠٢٦-١٠-٢٢).
 *
 * والمقيس على ثلاثة مستويات:
 *
 * 1. **المفردات**: [PromoState] تغطّي [FeatureAccess] تغطيةً متبادلة (يقرؤها غير هذه الشاشة).
 * 2. **الوصول والحياكة**: الشاشة **غير مقفلة** في الرسم الملاحيّ، ولها بابان (شاشة المحرّك ·
 *    لوح قفله)، وجسمها هو الشجرة وحدها.
 * 3. **النصوص**: عناوين عقد الشجرة موجودة بالإنجليزية والعربية، وكل عقدة مقروءة في الرسم.
 *
 * **والحدّ المُعلَن:** «هل الرسم جميلٌ ومقروء على الشاشات الصغيرة مع RTL؟» — UAT على جهاز.
 */
class MaxAiIntroTest {

    // ── ① المفردات: ترجمةٌ متبادلة بلا سقوط ──────────────────────────────────

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

    // ── ② الوصول والحياكة ────────────────────────────────────────────────────

    @Test
    fun `the intro stays reachable without a licence and its body is the tree alone`() {
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
            "وجسم الشاشة هو شجرة القدرات",
            screen.contains("MaxAiCapabilityTree("),
        )
        assertFalse(
            "ولا جدولَ صفوفٍ ولا قائمةَ نصوصٍ تعود إليها",
            screen.contains("MaxSection(") || screen.contains("MaxBullets(") || screen.contains("MaxNavigationRow("),
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

    // ── ③ النصوص: لغتان، ولا ثالثة ──────────────────────────────────────────

    @Test
    fun `every intro string exists in English and Arabic`() {
        val en = resFile("values/max_ai_strings.xml").readText()
        val ar = resFile("values-ar/max_ai_strings.xml").readText()
        val keys = listOf(
            "maxai_intro_title",
            "maxai_intro_gate_action",
            "maxai_tree_subtitle",
            "maxai_tree_teaser",
            "maxai_tree_root",
            "maxai_tree_mode",
            "maxai_tree_readings",
            "maxai_tree_gpu",
            "maxai_tree_thermal",
            "maxai_tree_cpu",
            "maxai_tree_learning",
            "maxai_tree_safety",
            "maxai_tree_decide",
            "maxai_tree_verify",
            "maxai_tree_perf",
            "maxai_tree_battery",
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

    @Test
    fun `the tree draws every node string it declares`() {
        val tree = sourceFile("ui/license/MaxAiCapabilityTree.kt").readText()
        val nodes = listOf(
            "root", "mode", "readings", "gpu", "thermal", "cpu",
            "learning", "safety", "decide", "verify", "perf", "battery",
        )
        for (node in nodes) {
            assertTrue("عقدةٌ بلا نصّ في الرسم: $node", tree.contains("R.string.maxai_tree_$node"))
            assertTrue("وعقدةٌ بلا رسمٍ: $node", tree.contains("nodeText(measurer, $node,"))
        }
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
