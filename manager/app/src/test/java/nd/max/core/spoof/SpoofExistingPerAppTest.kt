/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 */
package nd.max.core.spoof

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpoofExistingPerAppTest {
    private val json = """
        {
          "com.mobile.legends": {
            "refresh_rate": "120",
            "renderer": "skiavk",
            "resolution_target": "default"
          },
          "com.other.app": { "refresh_rate": "default", "renderer": "default" }
        }
    """.trimIndent()

    @Test fun readsOneFieldOfOneAppOnly() {
        assertEquals("120", SpoofExistingPerApp.value(json, "com.mobile.legends", "refresh_rate"))
        assertEquals("skiavk", SpoofExistingPerApp.value(json, "com.mobile.legends", "renderer"))
        assertEquals("default", SpoofExistingPerApp.value(json, "com.other.app", "refresh_rate"))
        assertNull(SpoofExistingPerApp.value(json, "com.other.app", "resolution_target"))
    }
    @Test fun anAbsentSourceIsNullNotAnEmptyString() {
        assertNull(SpoofExistingPerApp.value(json, "com.absent.app", "refresh_rate"))
        assertNull(SpoofExistingPerApp.value("", "com.mobile.legends", "refresh_rate"))
        assertNull(SpoofExistingPerApp.value("{not json", "com.mobile.legends", "refresh_rate"))
        assertNull(SpoofExistingPerApp.value(json, "com.game:aid", "refresh_rate"))
    }
    @Test fun aPackageNameCannotBecomeAPattern() {
        val hostile = """{ "com.aXb": { "refresh_rate": "1" }, "com.ab": { "refresh_rate": "2" } }"""
        assertNull(SpoofExistingPerApp.value(hostile, "com.a.b", "refresh_rate"))
        assertEquals("2", SpoofExistingPerApp.value(hostile, "com.ab", "refresh_rate"))
    }
    @Test fun theDeclaredKeysMatchTheShippedSeedAndEachHasOneOwner() {
        assertEquals(listOf("refresh_rate", "renderer", "resolution_downscale"),
            SpoofExistingPerApp.DISPLAY_KEYS.map { it.key })
        assertEquals(SpoofExistingPerApp.DISPLAY_KEYS.size,
            SpoofExistingPerApp.DISPLAY_KEYS.map { it.key }.distinct().size)
        assertTrue(SpoofExistingPerApp.DISPLAY_KEYS.all { it.owner.isNotBlank() })
    }
    @Test fun everyDeclaredKeyIsOneTheConfigActuallyDecodes() {
        // كان في القائمة مفتاح لا يفكّه أي حقل (`resolution_target`) فقيمةٌ تحته تسقط صامتة.
        // والقاعدة الآن: ما يُعرض على هذا السطح يُفكّ فعلًا، ولذلك كلها `consumedByConfig = true`.
        assertTrue(
            "a key listed here that no AppConfig field decodes would be a silent drop, not a knob",
            SpoofExistingPerApp.DISPLAY_KEYS.all { it.consumedByConfig },
        )
    }
    @Test fun theSpoofSurfaceDeclaresNoWriterForTheseKeys() {
        // The boundary is the type: every outcome is blocked and no write is ever attempted.
        val result = SpoofApplyBackend.evaluate("com.mobile.legends",
            SpoofProfile("p1", "n", "b", "m", "d", "pr"), consent = true,
            inventory = nd.max.ui.util.ModuleInventory(true, emptyList()), barrierAcknowledged = true)
        assertFalse(result.writeAttempted)
        assertTrue(SpoofEngineAdapter.plan("COPG", SpoofProfile("p1", "n", "b", "m", "d", "pr")).records
            .none { it.substringBefore('=') in SpoofExistingPerApp.DISPLAY_KEYS.map { knob -> knob.key } })
    }
}
