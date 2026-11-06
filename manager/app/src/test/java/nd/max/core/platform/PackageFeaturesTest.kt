/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * مقارنة ما تطلبه الحزمة بما يعلنه الجهاز. يُختبر هنا لأن الخطأ الصامت يعني **تحذيرًا كاذبًا**
 * («ميزة غير مدعومة» وهي مدعومة) أو **ثقة كاذبة** (تطبيق يُثبَّت ثم لا يعمل).
 */
class PackageFeaturesTest {

    @Test
    fun a_feature_the_device_advertises_is_supported() {
        val result = PackageFeatures.split(listOf("android.hardware.nfc"), setOf("android.hardware.nfc"))

        assertEquals(listOf("android.hardware.nfc"), result.required)
        assertTrue(result.unsupported.isEmpty())
        assertEquals(listOf("android.hardware.nfc"), result.supported)
    }

    @Test
    fun a_declared_feature_the_device_lacks_is_reported() {
        val result = PackageFeatures.split(
            listOf("android.hardware.nfc", "android.hardware.telephony"),
            setOf("android.hardware.telephony")
        )

        assertEquals(listOf("android.hardware.nfc"), result.unsupported)
        assertEquals(listOf("android.hardware.telephony"), result.supported)
    }

    @Test
    fun names_are_trimmed_deduplicated_and_sorted() {
        val result = PackageFeatures.split(
            listOf(" b ", "a", "b", "", "   "),
            setOf("a")
        )

        assertEquals(listOf("a", "b"), result.required)
        assertEquals(listOf("b"), result.unsupported)
    }

    @Test
    fun nothing_declared_means_nothing_to_warn_about() {
        val result = PackageFeatures.split(emptyList(), setOf("android.hardware.nfc"))

        assertTrue(result.required.isEmpty())
        assertTrue(result.unsupported.isEmpty())
    }
}
