/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import nd.max.ui.util.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * العدّ للإعدادات المحفوظة فقط، لا للمعدلات المطبّقة. القراءة المجهولة ليست صفرًا،
 * والخريطة المتبقية بعد الفشل لا تُستعمل لتخمين عدد حالي.
 */
class FasRatesTest {

    @Test
    fun `the default token and blanks are not a rate`() {
        assertNull(customRefreshHz(null))
        assertNull(customRefreshHz(""))
        assertNull(customRefreshHz("   "))
        assertNull(customRefreshHz("default"))
    }

    @Test
    fun `a whole number is read as written`() {
        assertEquals(90, customRefreshHz("90"))
        assertEquals(120, customRefreshHz(" 120 "))
    }

    @Test
    fun `a decimal is read the way the applier reads it`() {
        // `toFloatOrNull()?.toInt()` — نفس ما تفعله `InAppProfileRules.wants`.
        assertEquals(90, customRefreshHz("90.0"))
        assertEquals(120, customRefreshHz("120.7"))
    }

    @Test
    fun `zero and negative rates are not rates`() {
        // كتابة صفر أو سالب تعني «لا شيء» لا «معدّل صفر هرتز».
        assertNull(customRefreshHz("0"))
        assertNull(customRefreshHz("0.0"))
        assertNull(customRefreshHz("-60"))
    }

    @Test
    fun `text and non-finite numbers are refused rather than guessed`() {
        assertNull(customRefreshHz("90fps"))
        assertNull(customRefreshHz("sixty"))
        // NaN ⇒ 0 بالتحويل، وInfinity ⇒ Int.MAX_VALUE — وكلاهما ليس معدّلًا.
        assertNull(customRefreshHz("NaN"))
        assertNull(customRefreshHz("Infinity"))
        assertNull(customRefreshHz("-Infinity"))
        assertNull(customRefreshHz("1e100"))
    }

    @Test
    fun `only apps with a real rate are counted, and the list is ordered`() {
        val profiles = mapOf(
            "com.zeta.game" to AppConfig(refresh_rate = "90"),
            "com.alpha.game" to AppConfig(refresh_rate = "default"),
            "com.mid.game" to AppConfig(refresh_rate = "120.0"),
            "com.blank.game" to AppConfig(refresh_rate = ""),
            "com.bad.game" to AppConfig(refresh_rate = "-1"),
        )
        assertEquals(listOf("com.mid.game", "com.zeta.game"), customRefreshPackages(profiles))
    }

    @Test
    fun `an empty library counts zero, not an invented default`() {
        assertTrue(customRefreshPackages(emptyMap()).isEmpty())
        assertTrue(
            customRefreshPackages(mapOf("com.a" to AppConfig(), "com.b" to AppConfig())).isEmpty(),
        )
    }

    @Test
    fun `loading keeps the count unknown even with cached profiles`() {
        val profiles = mapOf("com.a" to AppConfig(refresh_rate = "90"))
        assertNull(customRefreshCount(profiles, loading = true, failed = false))
        assertNull(customRefreshCount(profiles, loading = true, failed = true))
    }

    @Test
    fun `failed reading reports neither zero nor a stale count`() {
        assertNull(customRefreshCount(emptyMap(), loading = false, failed = true))
        val profiles = mapOf("com.a" to AppConfig(refresh_rate = "90"))
        assertNull(customRefreshCount(profiles, loading = false, failed = true))
    }

    @Test
    fun `only a successfully read library has a known count`() {
        assertEquals(0, customRefreshCount(emptyMap(), loading = false, failed = false))
        val profiles = mapOf(
            "com.a" to AppConfig(refresh_rate = "90"),
            "com.b" to AppConfig(),
        )
        assertEquals(1, customRefreshCount(profiles, loading = false, failed = false))
    }
}
