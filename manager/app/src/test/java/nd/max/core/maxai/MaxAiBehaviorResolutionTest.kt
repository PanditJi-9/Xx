/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.maxai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * تجاوز السلوك لكل تطبيق يُقرأ من `AppConfig.max_ai_behavior`:
 * `default` والمجهول يعنيان «ورّث العام»، لا «متوازن» صامتًا.
 */
class MaxAiBehaviorResolutionTest {

    @Test
    fun `default per-app value inherits the global behavior`() {
        assertEquals(
            MaxAiBehavior.DECISIVE,
            MaxAiBehavior.resolve(MaxAiBehavior.DECISIVE, "default"),
        )
    }

    @Test
    fun `unknown per-app value inherits instead of silently becoming balanced`() {
        assertEquals(
            MaxAiBehavior.CALM,
            MaxAiBehavior.resolve(MaxAiBehavior.CALM, "future-behavior"),
        )
    }

    @Test
    fun `a known per-app value wins over the global choice`() {
        assertEquals(
            MaxAiBehavior.DECISIVE,
            MaxAiBehavior.resolve(MaxAiBehavior.CALM, "decisive"),
        )
    }

    @Test
    fun `explicit lookup does not invent a default`() {
        assertNull(MaxAiBehavior.fromIdOrNull("nope"))
        assertEquals(MaxAiBehavior.CALM, MaxAiBehavior.fromIdOrNull("calm"))
        assertEquals(MaxAiBehavior.DEFAULT, MaxAiBehavior.fromId("nope"))
    }
}
