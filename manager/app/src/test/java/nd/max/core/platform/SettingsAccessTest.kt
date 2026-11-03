/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** قبل `init` لا سياق: لا كتابة مباشرة ولا قراءة ولا رمي — فيعود المستدعي إلى الصدفة كما كان. */
class SettingsAccessTest {
    @Test
    fun `without an application context nothing is written or read directly`() {
        assertFalse(SettingsAccess.putDirect("global", "low_power", "1"))
        assertNull(SettingsAccess.getDirect("global", "low_power"))
    }

    @Test
    fun `an unknown namespace is refused`() {
        assertFalse(SettingsAccess.putDirect("bogus", "k", "v"))
    }
}
