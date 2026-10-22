/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حرّاس شرط بند منح صلاحية التراكب: لا بند بلا لوحة مفعّلة، ولا بند بعد المنح.
 * والشرط نفسه يُستعمل في الواجهة والخدمة، فالانحراف بينهما هو ما يُعيد العطب الصامت.
 */
class PanelOverlayGateTest {

    @Test
    fun `no notice when no panel is enabled`() {
        assertFalse(PanelOverlayGate.needsGrant(emptyList(), canDrawOverlays = false))
    }

    @Test
    fun `notice when an enabled panel cannot draw over other apps`() {
        assertTrue(PanelOverlayGate.needsGrant(listOf("com.example.game"), canDrawOverlays = false))
    }

    @Test
    fun `no notice when the permission is already granted`() {
        assertFalse(PanelOverlayGate.needsGrant(listOf("com.example.game"), canDrawOverlays = true))
    }

    @Test
    fun `one enabled package is enough even when others are off`() {
        assertTrue(
            PanelOverlayGate.needsGrant(listOf("com.example.game"), canDrawOverlays = false)
        )
    }
}
