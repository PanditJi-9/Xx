/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.island.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * الجزيرة الأصلية — **الحدّ المُعلن** لا التحكّم المتخيَّل.
 *
 * هذه الاختبارات تمنع عودةً خطرة: أيّ طريق يضيف `controllable = true` بلا واجهة نظام موثّقة
 * يُسقط هذه الاختبارات. ولا يُلمس SystemUI في أيّ فرع.
 */
class NativeIslandTest {

    private fun facts(
        maker: String,
        brand: String = maker,
        sdk: Int = 34,
        packages: Set<String> = emptySet(),
    ) = NativeIslandFacts(maker, brand, sdk, packages)

    @Test
    fun `a xiaomi device is detected but never reported as controllable`() {
        val v = NativeIslandRules.classify(facts("Xiaomi"))
        assertTrue(v.nativeLikely)
        assertFalse(v.controllable)
        assertEquals(NativeIslandReason.NO_DOCUMENTED_SWITCH, v.reason)
        assertTrue(v.vendor!!.contains("Xiaomi"))
    }

    @Test
    fun `oppo oneplus and realme share the same verdict`() {
        listOf("OPPO", "OnePlus", "realme").forEach { maker ->
            val v = NativeIslandRules.classify(facts(maker))
            assertTrue("$maker should be detected", v.nativeLikely)
            assertFalse(v.controllable)
        }
    }

    @Test
    fun `a system package can identify a vendor even with an unknown manufacturer`() {
        val v = NativeIslandRules.classify(facts("unknown", packages = setOf("com.oplus.systemui")))
        assertTrue(v.nativeLikely)
        assertEquals("OPPO / OnePlus / realme", v.vendor)
        assertFalse(v.controllable)
    }

    @Test
    fun `stock android has no native island and therefore nothing to disable`() {
        val v = NativeIslandRules.classify(facts("Google", packages = setOf("com.android.systemui")))
        assertFalse(v.nativeLikely)
        assertFalse(v.controllable)
        assertEquals(NativeIslandReason.NO_NATIVE_ISLAND, v.reason)
        assertNull(v.vendor)
    }

    @Test
    fun `old android versions are never assumed to ship an island`() {
        val v = NativeIslandRules.classify(facts("Xiaomi", sdk = 29))
        assertFalse(v.nativeLikely)
        assertFalse(v.controllable)
    }

    @Test
    fun `whitespace and case do not change detection`() {
        val v = NativeIslandRules.classify(facts("  xIaOmI ", brand = " Redmi "))
        assertTrue(v.nativeLikely)
        assertTrue(v.controllable.not())
    }
}
