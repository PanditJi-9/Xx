/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelShelfModelTest {

    @Test
    fun `pinned apps come first in their order and only when they can launch`() {
        val out = PanelShelfModel.compose(
            picks = listOf("com.b.app", "com.gone.app", "com.a.app"),
            uses = emptyMap(),
            launchable = setOf("com.a.app", "com.b.app"),
        )
        assertEquals(listOf("com.b.app", "com.a.app"), out)
    }

    @Test
    fun `most opened apps follow the pins and come before the defaults`() {
        val out = PanelShelfModel.compose(
            picks = emptyList(),
            uses = mapOf("com.x.app" to 3, "com.y.app" to 5, "com.zero.app" to 0),
            launchable = setOf("com.x.app", "com.y.app", "com.zero.app", "com.android.chrome"),
        )
        assertEquals(listOf("com.y.app", "com.x.app", "com.android.chrome"), out)
    }

    @Test
    fun `without usage the defaults keep their own order`() {
        val out = PanelShelfModel.compose(
            picks = emptyList(),
            uses = emptyMap(),
            launchable = setOf("com.whatsapp", "com.android.chrome"),
        )
        assertEquals(listOf("com.android.chrome", "com.whatsapp"), out)
    }

    @Test
    fun `the shelf never exceeds its limit`() {
        val out = PanelShelfModel.compose(
            picks = emptyList(),
            uses = emptyMap(),
            launchable = PanelShelfModel.DEFAULTS.toSet(),
            limit = 3,
        )
        assertEquals(PanelShelfModel.DEFAULTS.take(3), out)
    }

    @Test
    fun `picks are validated, de-duplicated and capped`() {
        val valid = (1..7).map { "com.p$it.app" }
        val picks = PanelShelfModel.picksSanitized(listOf("not a package", "com.a.app", "com.a.app") + valid)
        assertEquals(PanelShelfModel.MAX_PICKS, picks.size)
        assertFalse(picks.contains("not a package"))
        assertEquals(picks.distinct(), picks)
        assertEquals("com.a.app", picks.first())
    }

    @Test
    fun `the floating command waits for the launch, quotes the component and has no bounds option`() {
        // `-W` هو ما يجعل `Status: ok` يُطبع؛ و`--bounds` ليس خيارًا في `am start` فكان يُسقط الأمر كله.
        assertEquals(
            "am start -W --windowingMode 5 -n 'com.a.app/.Main'",
            PanelFloatingCommand.build("com.a.app/.Main"),
        )
        assertFalse(PanelFloatingCommand.build("com.a.app/.Main").contains("--bounds"))
        // صنف داخلي باسم فيه `$` لا يجوز أن يتوسّع في الصدفة.
        assertTrue(PanelFloatingCommand.build("com.a.app/.Main\$Alias").contains("'com.a.app/.Main\$Alias'"))
    }

    @Test
    fun `floating success needs the status line`() {
        assertTrue(PanelFloatingCommand.succeeded(listOf("Starting: Intent { }", "Status: ok", "LaunchState: COLD")))
        assertFalse(PanelFloatingCommand.succeeded(listOf("Starting: Intent { }")))
        assertFalse(PanelFloatingCommand.succeeded(listOf("Error: Activity class does not exist")))
        assertFalse(PanelFloatingCommand.succeeded(null))
    }

    @Test
    fun `enabling freeform writes both developer options`() {
        val commands = PanelFloatingCommand.enableFreeform()
        assertTrue(commands.any { it == "settings put global enable_freeform_support 1" })
        assertTrue(commands.any { it == "settings put global force_resizable_activities 1" })
    }
}
