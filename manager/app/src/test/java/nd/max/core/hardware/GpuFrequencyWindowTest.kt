package nd.max.core.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GpuFrequencyWindowTest {

    private fun device(
        minHz: Long?,
        maxHz: Long?,
        ladderHz: List<Long>,
        unit: GpuHardwareBackend.FrequencyUnit = GpuHardwareBackend.FrequencyUnit.HZ,
    ) = GpuHardwareBackend.Device(
        path = "/sys/class/devfreq/13000000.mali",
        name = "13000000.mali",
        governor = "dummy",
        governors = listOf("dummy"),
        minFreq = minHz,
        maxFreq = maxHz,
        currentFreq = minHz,
        frequencies = ladderHz,
        frequencyUnit = unit,
    )

    @Test
    fun `dynamic bounds come from the live policy while capability comes from the ladder`() {
        val window = GpuFrequencyWindow.of(
            device(260_000_000L, 754_000_000L, listOf(260_000_000L, 650_000_000L, 1_300_000_000L)),
        )
        assertEquals(260, window.dynamicMinMhz)
        assertEquals(754, window.dynamicMaxMhz)
        assertEquals(260, window.capabilityMinMhz)
        assertEquals(1300, window.capabilityMaxMhz)
    }

    @Test
    fun `an unproven unit yields no numbers instead of invented ones`() {
        val window = GpuFrequencyWindow.of(
            device(260_000_000L, 754_000_000L, listOf(260_000_000L), GpuHardwareBackend.FrequencyUnit.AMBIGUOUS),
        )
        assertNull(window.dynamicMaxMhz)
        assertNull(window.capabilityMaxMhz)
    }
}
