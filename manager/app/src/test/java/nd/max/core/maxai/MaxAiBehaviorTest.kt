/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * السلوك مفردات محفوظة: معرّفها يُكتب على القرص ويُقرأ بعد ترقية، فالثبات هنا
 * عقد لا تفصيل. والاختبار نقي (بلا Android) كي يُشغَّل في JDK العادي.
 */
class MaxAiBehaviorTest {

    @Test
    fun `balanced is the default when nothing was chosen`() {
        assertSame(MaxAiBehavior.BALANCED, MaxAiBehavior.fromId(null))
        assertSame(MaxAiBehavior.DEFAULT, MaxAiBehavior.fromId(""))
    }

    @Test
    fun `every behavior id round-trips`() {
        MaxAiBehavior.entries.forEach { behavior ->
            assertSame(behavior, MaxAiBehavior.fromId(behavior.id))
        }
    }

    @Test
    fun `an unknown persisted id falls back instead of throwing`() {
        assertSame(MaxAiBehavior.BALANCED, MaxAiBehavior.fromId("turbo"))
        assertSame(MaxAiBehavior.BALANCED, MaxAiBehavior.fromId("CALM"))
    }

    @Test
    fun `ids stay stable because they are persisted`() {
        assertEquals(
            listOf("calm", "balanced", "decisive"),
            MaxAiBehavior.entries.map { it.id },
        )
    }
}
