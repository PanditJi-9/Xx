package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Test

class PanelResolutionTest {

    private val native = DisplayReading(
        nativeW = 1080, nativeH = 2400, nativeDpi = 440,
        activeW = 1080, activeH = 2400, activeDpi = 440,
    )

    @Test
    fun `presets are fractions of the native panel and keep an even width`() {
        val p = PanelResolution.presets(native).associateBy { it.id }
        assertEquals(ScalePreset("native", 1080, 2400, 440), p["native"])
        assertEquals(ScalePreset("balanced", 864, 1920, 352), p["balanced"])
        assertEquals(ScalePreset("performance", 648, 1440, 264), p["performance"])
        assertEquals(ScalePreset("battery_saver", 486, 1080, 198), p["battery_saver"])
    }

    @Test
    fun `an untouched display reports native and a matching override reports its preset`() {
        assertEquals("native", PanelResolution.activeId(native))
        val performance = native.copy(activeW = 648, activeH = 1440, activeDpi = 264)
        assertEquals("performance", PanelResolution.activeId(performance))
    }

    @Test
    fun `an override that matches no preset is reported as custom`() {
        val custom = native.copy(activeW = 700, activeH = 1500, activeDpi = 300)
        assertEquals("custom", PanelResolution.activeId(custom))
    }
}
