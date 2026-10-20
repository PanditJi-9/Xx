/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * زمن المسار الصوتي — نموذج نقيّ، فيُقاس على JVM بلا جهاز.
 *
 * وكلُّ اختبار هنا يقابل ادّعاءً كان يمكن أن يمرّ خاطئًا: حساب زمن من نصف معادلة، أو ملء
 * غياب القراءة بأرقام شائعة (`48000`)، أو عرض `0.0 ms` مكان «غير مقروء».
 *
 * **وحدّه المعلن:** هذا يثبت الحساب والعرض، ولا يثبت أن `AudioTrack.getPerformanceMode()`
 * يعطي حكمًا صحيحًا على جهاز بعينه — ذلك قياس جهاز، ولم يُدَّع هنا.
 */
class AudioLatencyModelTest {

    @Test
    fun `declared buffer becomes milliseconds at the declared rate`() {
        assertEquals(4.0, audioBufferMillis(sampleRateHz = 48_000, framesPerBuffer = 192)!!, 1e-9)
        assertEquals(5.333, audioBufferMillis(48_000, 256)!!, 1e-3)
    }

    @Test
    fun `neither half of the equation invents a value`() {
        assertNull(audioBufferMillis(null, 192))
        assertNull(audioBufferMillis(48_000, null))
        assertNull(audioBufferMillis(0, 192))
        assertNull(audioBufferMillis(-48_000, 192))
        assertNull(audioBufferMillis(48_000, 0))
    }

    @Test
    fun `the displayed text has one decimal place and never zero for absence`() {
        assertEquals("4.0", audioBufferMillisText(4.0))
        assertEquals("5.3", audioBufferMillisText(5.333))
        assertNull(audioBufferMillisText(null))
    }

    @Test
    fun `the reading keeps the support verdict and the measured numbers together`() {
        val capabilities = audioOutputCapabilitiesOf("48000", "192", emptyList())
        val reading = audioLatencyReading(capabilities, AudioLowLatencySupport.SUPPORTED)

        assertEquals(48_000, reading.sampleRateHz)
        assertEquals(192, reading.framesPerBuffer)
        assertEquals(4.0, reading.bufferMillis!!, 1e-9)
        assertTrue(reading.bufferMeasured)
        assertEquals(AudioLowLatencySupport.SUPPORTED, reading.lowLatency)
    }

    @Test
    fun `a null inventory yields an unmeasured reading rather than invented numbers`() {
        val reading = audioLatencyReading(null, AudioLowLatencySupport.UNREADABLE)

        assertNull(reading.sampleRateHz)
        assertNull(reading.framesPerBuffer)
        assertNull(reading.bufferMillis)
        assertFalse(reading.bufferMeasured)
        assertEquals(AudioLowLatencySupport.UNREADABLE, reading.lowLatency)
    }
}
