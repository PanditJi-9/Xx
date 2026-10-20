/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **القنوات الأربع لمعدّل اللمس (§٧-ب) — مفصولة بالبناء، لا بالتعليق.**
 *
 * وهذه هي القاعدة التي تمنع أوضح كذبة في هذا الباب: «رفعنا أحداث Android إلى ١٠٠٠ ⇒ الحساس
 * يعمل عند ١٠٠٠ Hz». النموذج يمنعها بثلاثة أشياء: كل قناة سجلّ مستقلّ، و`value = null` تعني
 * مجهولًا لا صفرًا، والحدّ الموثّق لا يظهر بلا مصدر. والاختبارات تقيس كلًّا منها.
 */
class TouchRateChannelsTest {

    private val focaltech = TouchControllerFamily.FOCALTECH

    private fun channelReadings(
        sample: TouchRateSample? = null,
        registry: Map<TouchControllerFamily, TouchRateRegistryEntry> = emptyMap(),
        transport: Boolean = false,
        writePath: Boolean = false,
    ) = TouchRateChannels.snapshot(
        family = focaltech,
        sample = sample,
        vendorTransportPresent = transport,
        vendorWritePathProven = writePath,
        registry = registry,
    )

    private fun reading(
        channel: TouchChannel,
        sample: TouchRateSample? = null,
        registry: Map<TouchControllerFamily, TouchRateRegistryEntry> = emptyMap(),
        transport: Boolean = false,
        writePath: Boolean = false,
    ) = channelReadings(sample, registry, transport, writePath).first { it.channel == channel }

    // ── ① الأربع قنوات موجودة دائمًا، بترتيبها، ولا تُدمج ─────────────────────────────────

    @Test
    fun `the snapshot always carries all four channels in order`() {
        val channels = channelReadings().map(TouchChannelReading::channel)

        assertEquals(
            listOf(
                TouchChannel.HARDWARE_SAMPLING,
                TouchChannel.DOCUMENTED_LIMIT,
                TouchChannel.ANDROID_EVENTS,
                TouchChannel.EXPERIMENTAL_VENDOR,
            ),
            channels,
        )
    }

    // ── ② أحداث Android لا تملأ المعدّل الأصلي أبدًا ──────────────────────────────────────

    @Test
    fun `android events never fill the hardware sampling channel`() {
        val readings = channelReadings(sample = TouchRateSample(events = 500, durationMs = 1_000))
        val hardware = readings.first { it.channel == TouchChannel.HARDWARE_SAMPLING }
        val android = readings.first { it.channel == TouchChannel.ANDROID_EVENTS }

        assertEquals(500.0, android.value!!, 0.001)
        assertNull(
            "٥٠٠ حدث/ث إلى التطبيق لا تثبت معدّل عيّنات العتاد — تبقى مجهولة",
            hardware.value,
        )
        assertEquals(TouchChannelState.NOT_READABLE, hardware.state)
        assertEquals(TouchChannelState.MEASURED, android.state)
    }

    // ── ③ الحدّ الموثّق: رقم + مصدر، وإلا لا شيء ──────────────────────────────────────────

    @Test
    fun `a documented limit requires a value and a named source`() {
        val noEntry = reading(TouchChannel.DOCUMENTED_LIMIT)
        assertNull(noEntry.value)
        assertEquals(TouchChannelState.NOT_REGISTERED, noEntry.state)

        val noSource = reading(
            TouchChannel.DOCUMENTED_LIMIT,
            registry = mapOf(focaltech to TouchRateRegistryEntry(documentedLimitHz = 480)),
        )
        assertNull("رقم بلا مصدر لا يُعرض", noSource.value)
        assertEquals(TouchChannelState.NOT_REGISTERED, noSource.state)

        val sourced = reading(
            TouchChannel.DOCUMENTED_LIMIT,
            registry = mapOf(
                focaltech to TouchRateRegistryEntry(
                    documentedLimitHz = 480,
                    documentation = "vendor-spec: FocalTech FT5x06, 2024 edition, section 4",
                ),
            ),
        )
        assertEquals(480.0, sourced.value!!, 0.001)
        assertEquals(TouchChannelState.DOCUMENTED, sourced.state)
        assertEquals(TouchConfidence.DOCUMENTED, sourced.confidence)
    }

    @Test
    fun `a documented limit is not inferred from the controller family`() {
        // العائلة معروفة، ولا إدخال لها: هذا لا يكفي.
        assertNull(reading(TouchChannel.DOCUMENTED_LIMIT).value)
    }

    // ── ④ المعدّل الأصلي: من إدخال مقيس فقط ───────────────────────────────────────────────

    @Test
    fun `the hardware rate comes only from a verified entry`() {
        val absent = reading(TouchChannel.HARDWARE_SAMPLING)
        assertNull(absent.value)
        assertEquals(TouchConfidence.NONE, absent.confidence)
        assertEquals(TouchRateChannels.NOTE_NO_PUBLIC_READ_PATH, absent.note)

        val documentedOnly = reading(
            TouchChannel.HARDWARE_SAMPLING,
            registry = mapOf(
                focaltech to TouchRateRegistryEntry(
                    documentedLimitHz = 480,
                    documentation = "vendor-spec",
                ),
            ),
        )
        assertNull("الحدّ الموثّق ليس معدّل العتاد المقيس", documentedOnly.value)

        val measured = reading(
            TouchChannel.HARDWARE_SAMPLING,
            registry = mapOf(focaltech to TouchRateRegistryEntry(samplingHz = 240)),
        )
        assertEquals(240.0, measured.value!!, 0.001)
        assertEquals(TouchChannelState.MEASURED, measured.state)
        assertEquals(TouchConfidence.MEASURED, measured.confidence)
    }

    // ── ⑤ أحداث Android: المجهول مجهول، لا صفر ───────────────────────────────────────────

    @Test
    fun `an unmeasured android rate is unknown not zero`() {
        val unmeasured = reading(TouchChannel.ANDROID_EVENTS)
        assertNull(unmeasured.value)
        assertEquals(TouchChannelState.NOT_MEASURED, unmeasured.state)
        assertEquals(TouchRateChannels.NOTE_NOT_MEASURED, unmeasured.note)

        val tooShort = reading(
            TouchChannel.ANDROID_EVENTS,
            sample = TouchRateSample(events = 2, durationMs = 5),
        )
        assertNull("عيّنة ٢ على ٥ م.ث ليست معدّلًا", tooShort.value)
        assertEquals(TouchChannelState.NOT_MEASURED, tooShort.state)
        assertEquals(TouchRateChannels.NOTE_SAMPLE_TOO_SHORT, tooShort.note)

        val measured = reading(
            TouchChannel.ANDROID_EVENTS,
            sample = TouchRateSample(events = 120, durationMs = 1_000),
        )
        assertEquals(120.0, measured.value!!, 0.001)
        assertEquals(TouchRateChannels.NOTE_SINGLE_SAMPLE, measured.note)
    }

    // ── ⑥ الوضع التجريبي: رفض بالعقد بلا طريق مثبَت ──────────────────────────────────────

    @Test
    fun `the experimental mode is refused without a proven write path`() {
        val absent = reading(TouchChannel.EXPERIMENTAL_VENDOR)
        assertEquals(TouchChannelState.UNAVAILABLE, absent.state)
        assertEquals(TouchConfidence.NONE, absent.confidence)
        assertEquals(TouchRateChannels.NOTE_NO_VENDOR_TRANSPORT, absent.note)

        val declaredOnly = reading(TouchChannel.EXPERIMENTAL_VENDOR, transport = true)
        assertEquals(TouchChannelState.REFUSED_CONTRACT, declaredOnly.state)
        assertEquals(TouchConfidence.REFUSED, declaredOnly.confidence)
        assertEquals(TouchRateChannels.NOTE_VENDOR_CONTRACT_UNPROVEN, declaredOnly.note)
        assertEquals(TouchRequirement.VENDOR_SERVICE, declaredOnly.requirement)
        assertNull("لا رقم معدّل يُدَّعى لهذه القناة", declaredOnly.value)

        val proven = reading(TouchChannel.EXPERIMENTAL_VENDOR, transport = true, writePath = true)
        assertEquals(TouchChannelState.AVAILABLE, proven.state)
        assertEquals(TouchConfidence.MEASURED, proven.confidence)
        assertEquals(TouchRateChannels.NOTE_VENDOR_WRITE_PATH_PROVEN, proven.note)
        assertNull("القدرة تُعلَن، ولا يُخترع لها رقم", proven.value)
    }

    // ── ⑦ حارس السجلّ: لا إدخال بلا مصدر ─────────────────────────────────────────────────

    @Test
    fun `every documented entry names its source`() {
        TouchRateRegistry.entries.forEach { (family, entry) ->
            if (entry.documentedLimitHz != null) {
                assertTrue(
                    "الإدخال الموثّق لـ$family بلا مصدر — يُحذف أو يُوثَّق",
                    !entry.documentation.isNullOrBlank(),
                )
            }
            entry.samplingHz?.let {
                assertTrue("المعدّل المقيس لـ$family يجب أن يكون موجبًا", it > 0)
            }
            entry.documentedLimitHz?.let {
                assertTrue("الحدّ الموثّق لـ$family يجب أن يكون موجبًا", it > 0)
            }
        }
    }
}
