/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import nd.max.ui.util.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حارس «هل هذا الإدخال يطلب شيئًا؟».
 *
 * **ولماذا يستحق اختبارًا وهو سطران:** قراره يفتح جلسة عتاد كاملة أو يمنعها. والقياس من حزمة
 * المستخدم: `com.termux` أنتج ٢٩٠ جلسة و٢٩٠ تراجعًا، وكل مقابضه `profile-is-default` — أي
 * عشرات الأوامر وإعادة تطبيق الملفّ العام مقابل لا شيء. فهذه الدعاوى تُثبّت الحدّين:
 * `default` لا يفتح جلسة، وأي قيمة صريحة تفتحها (فلا يُسقط طلب حقيقي).
 */
class PerAppSessionRulesTest {

    /** يبني كتلة إعداد من أزواج (مفتاح، قيمة) كما تُكتب في الملفّ — بلا تعقيد اقتباس. */
    private fun entry(vararg pairs: Pair<String, String>): String =
        pairs.joinToString(",") { (key, value) -> "\"$key\":\"$value\"" }

    @Test
    fun `an entry with every field at default requests nothing`() {
        val allDefault = entry(
            *PerAppSessionRules.KNOB_KEYS.map { it to "default" }.toTypedArray(),
        )
        assertFalse(
            "an all-default entry must not open a hardware session",
            PerAppSessionRules.hasExplicitOverride(allDefault),
        )
        assertEquals(0, PerAppSessionRules.explicitOverrideCount(allDefault))
    }

    @Test
    fun `one explicit knob is enough to open the session`() {
        assertTrue(PerAppSessionRules.hasExplicitOverride(entry("gpu_profile" to "gaming")))
        assertTrue(PerAppSessionRules.hasExplicitOverride(entry("cpu_governor" to "performance")))
        assertTrue(PerAppSessionRules.hasExplicitOverride(entry("touch_boost" to "true")))
        assertTrue(PerAppSessionRules.hasExplicitOverride(entry("refresh_rate" to "120")))
        assertTrue(
            "an encoded CPU policy is a request",
            PerAppSessionRules.hasExplicitOverride(
                entry("cpu_policy_controls" to "policy0|range|300000|2000000"),
            ),
        )
    }

    @Test
    fun `booleans and empty strings are read by value, not by presence`() {
        assertFalse(PerAppSessionRules.hasExplicitOverride(entry("cpu_policy_controls" to "")))
        assertFalse(PerAppSessionRules.hasExplicitOverride(entry("gpu_max_freq" to "")))
        assertFalse(PerAppSessionRules.hasExplicitOverride(entry("kill_bg_apps" to "default")))
        assertTrue(PerAppSessionRules.hasExplicitOverride(entry("kill_bg_apps" to "true")))
        assertTrue(PerAppSessionRules.hasExplicitOverride("\"perf_lite_mode\":false"))
    }

    @Test
    fun `an empty or missing entry is not a session`() {
        assertFalse(PerAppSessionRules.hasExplicitOverride(null))
        assertFalse(PerAppSessionRules.hasExplicitOverride(""))
        assertFalse(PerAppSessionRules.hasExplicitOverride("   "))
        assertEquals(0, PerAppSessionRules.explicitOverrideCount(""))
    }

    @Test
    fun `an unknown key never opens a hardware session`() {
        assertFalse(
            "a field this build cannot apply must not be read as a request",
            PerAppSessionRules.hasExplicitOverride(entry("future_knob" to "aggressive", "another" to "1")),
        )
        assertFalse(
            "free text is not a knob",
            PerAppSessionRules.hasExplicitOverride(entry("note" to "gaming")),
        )
    }

    /**
     * **الربط الذي يمنع الصمت:** كل حقل في [AppConfig] له اسم معروف هنا. فلو أُضيف حقل جديد
     * هناك ونُسي هنا، صار إعداد المستخدم لا يفتح جلسة — وهو عطب لا يظهر إلا على الجهاز.
     */
    @Test
    fun `the knob list covers every AppConfig field`() {
        val descriptor = AppConfig.serializer().descriptor
        val declared = (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }.toSet()
        assertEquals(
            "PerAppSessionRules.KNOB_KEYS must track AppConfig: " +
                "missing=${declared - PerAppSessionRules.KNOB_KEYS} extra=${PerAppSessionRules.KNOB_KEYS - declared}",
            declared,
            PerAppSessionRules.KNOB_KEYS,
        )
    }

    @Test
    fun `the count is the same number the decision uses`() {
        val mixed = entry("gpu_profile" to "gaming", "cpu_governor" to "default", "refresh_rate" to "120")
        assertEquals(2, PerAppSessionRules.explicitOverrideCount(mixed))
        assertTrue(PerAppSessionRules.hasExplicitOverride(mixed))
    }
}
