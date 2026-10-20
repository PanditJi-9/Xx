/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.privilege

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * قاعدة اختيار المسار — نقيّة، فتُقاس بلا جهاز.
 *
 * وأهمّ سطر هنا هو الثالث: «لا طبقة» تُعيد `null` ولا تُنفِّذ شيئًا. لو أُعيدت طبقة
 * افتراضية لصار كل أمر بلا امتياز يُنفَّذ بصلاحية التطبيق ويرجع «permission denied» —
 * وهو العطب الصامت الذي وُجدت هذه القاعدة لمنعه (ADR-07: لا قدرة مُبالَغ فيها).
 */
class PrivilegedShellRulesTest {

    @Test
    fun `root wins when both lanes are available`() {
        assertEquals(
            PrivilegedShellRules.Lane.ROOT,
            PrivilegedShellRules.laneFor(rootGranted = true, shizukuReady = true),
        )
    }

    @Test
    fun `shizuku is used when only those privileges are granted`() {
        assertEquals(
            PrivilegedShellRules.Lane.SHIZUKU,
            PrivilegedShellRules.laneFor(rootGranted = false, shizukuReady = true),
        )
    }

    @Test
    fun `no privilege means refusal, never an unprivileged execution`() {
        assertNull(PrivilegedShellRules.laneFor(rootGranted = false, shizukuReady = false))
    }
}
