/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** القائمة التي يقرؤها الرفيق الجذري من الملف، بلا أندرويد. */
class SidePanelArmedTest {

    @Test fun encodeWritesOnePackagePerLineSortedAndUnique() {
        assertEquals("com.a.one\ncom.b.two\n", SidePanelArmed.encode(setOf("com.b.two", "com.a.one", "com.a.one")))
    }

    @Test fun anEmptySetWritesAnEmptyFile() {
        assertEquals("", SidePanelArmed.encode(emptySet()))
    }

    @Test fun invalidNamesNeverReachTheFile() {
        assertEquals("com.a.one\n", SidePanelArmed.encode(setOf("com.a.one", "bad;pkg", "com", "")))
    }

    @Test fun decodeIgnoresBlankAndInvalidLines() {
        assertEquals(setOf("com.a.one", "com.b.two"), SidePanelArmed.decode("com.a.one\n\n  com.b.two  \nbad;x\n"))
    }

    @Test fun aMissingFileArmsNothing() {
        assertEquals(emptySet<String>(), SidePanelArmed.decode(null))
        assertFalse(SidePanelArmed.contains(null, "com.a.one"))
    }

    @Test fun containsMatchesWholeLinesOnly() {
        val text = "com.a.one\ncom.a.onex\n"
        assertTrue(SidePanelArmed.contains(text, "com.a.one"))
        // بادئة الحزمة ليست الحزمة: "com.a.on" لا تُفعَّل لأن "com.a.one" مكتوبة.
        assertFalse(SidePanelArmed.contains(text, "com.a.on"))
        assertFalse(SidePanelArmed.contains(text, "bad;pkg"))
    }

    @Test fun encodeThenDecodeRoundTrips() {
        val packages = setOf("com.a.one", "com.b.two")
        assertEquals(packages, SidePanelArmed.decode(SidePanelArmed.encode(packages)))
    }

    @Test fun theServiceComponentNamesThePanelService() {
        assertEquals("nd.max/nd.max.service.GamePanelService", SidePanelArmed.SERVICE_COMPONENT)
    }
}
