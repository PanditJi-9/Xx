/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

import nd.max.core.hardware.ProfileApplier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد الوضع الواحد: الهدف والمِلفّ ليسا مفتاحين منفصلين.
 *
 * هذه الطبقة تحمي من عودة «مبدّلين»: لو غيّر أحدهم خريطة `battery` إلى ملفّ
 * غير `ECO`، أو أضاف وضعًا رابعًا، يسقط الاختبار قبل أن يصل إلى الجهاز.
 */
class MaxAiModeTest {

    @Test
    fun `the three user modes are the three system profiles`() {
        assertEquals(ProfileApplier.PROFILE_PERFORMANCE, MaxAiMode.PERFORMANCE)
        assertEquals(ProfileApplier.PROFILE_BALANCED, MaxAiMode.BALANCED)
        assertEquals(ProfileApplier.PROFILE_ECO, MaxAiMode.BATTERY)
        assertTrue(MaxAiMode.isValid(MaxAiMode.PERFORMANCE))
        assertTrue(MaxAiMode.isValid(MaxAiMode.BALANCED))
        assertTrue(MaxAiMode.isValid(MaxAiMode.BATTERY))
        assertFalse(MaxAiMode.isValid("0"))
        assertFalse(MaxAiMode.isValid("4"))
        assertFalse(MaxAiMode.isValid(null))
    }

    @Test
    fun `preference to profile mapping is total and stable`() {
        assertEquals(MaxAiMode.PERFORMANCE, MaxAiMode.profileIdForPreference("performance"))
        assertEquals(MaxAiMode.BALANCED, MaxAiMode.profileIdForPreference("balanced"))
        assertEquals(MaxAiMode.BATTERY, MaxAiMode.profileIdForPreference("battery"))
        assertEquals(MaxAiMode.BALANCED, MaxAiMode.profileIdForPreference(null))
        assertEquals(MaxAiMode.BALANCED, MaxAiMode.profileIdForPreference("nonsense"))
    }

    @Test
    fun `profile to preference mapping is the inverse on the three modes`() {
        assertEquals("performance", MaxAiMode.preferenceForProfile(MaxAiMode.PERFORMANCE))
        assertEquals("balanced", MaxAiMode.preferenceForProfile(MaxAiMode.BALANCED))
        assertEquals("battery", MaxAiMode.preferenceForProfile(MaxAiMode.BATTERY))
        assertEquals("balanced", MaxAiMode.preferenceForProfile("0"))
        assertEquals("balanced", MaxAiMode.preferenceForProfile(null))
    }

    @Test
    fun `round trip keeps the user's mode`() {
        listOf("performance", "balanced", "battery").forEach { preference ->
            val profile = MaxAiMode.profileIdForPreference(preference)
            assertEquals(preference, MaxAiMode.preferenceForProfile(profile))
        }
    }
}
