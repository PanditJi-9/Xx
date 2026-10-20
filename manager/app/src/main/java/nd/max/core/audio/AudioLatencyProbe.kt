/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * الصوت — **سابر زمن المسار**: القراءة الوحيدة التي تلمس المنصّة.
 *
 * **ما يفعله بالضبط:** يقرأ معدّل العيّنة وحجم المخزن المعلنَين (`AudioManager.PROPERTY_OUTPUT_*`)،
 * ثم يبني `AudioTrack` بالوضع منخفض التأخير ويقرأ ما **منحته** المنصّة فعلًا
 * (`AudioTrack.getPerformanceMode()`) ويحرّره فورًا. **لا تشغيل ولا صوت ولا إذن.**
 *
 * **وما لا يفعله:** لا يكتب شيئًا، ولا يعدّل مسارًا، ولا يدّعي قياس رحلة ذهاب وعودة — فذاك
 * يحتاج تشغيل نغمة وتسجيلها، وهو ما لا يُقاس بلا جهاز. ولذلك كل عجز يعود
 * [AudioLowLatencySupport.UNREADABLE] ولا يُخمَّن.
 */
package nd.max.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

object AudioLatencyProbe {

    /**
     * قراءة كاملة: إعلان المخزن + حكم المسار. تُنادى من خيط IO (تبني وتحرّر `AudioTrack`
     * ولو للحظة)، ولا ترمي: كل عجز يُقال بحكمه.
     */
    fun measure(context: Context): AudioLatencyReading {
        val capabilities = AudioDeviceCatalog.capabilities(context)
        return audioLatencyReading(
            capabilities = capabilities,
            support = lowLatencySupport(capabilities?.sampleRateHz),
        )
    }

    /**
     * يبني تدفّقًا أحاديّ القناة بمعدّل المخرج المعلن، ويطلب الوضع منخفض التأخير، ثم يقرأ
     * الوضع الفعليّ. والتحرير في `finally` حتى لا يبقى مورد صوتيّ محجوزًا.
     */
    private fun lowLatencySupport(sampleRateHz: Int?): AudioLowLatencySupport {
        val rate = sampleRateHz?.takeIf { it > 0 } ?: return AudioLowLatencySupport.UNREADABLE
        return runCatching {
            val minBytes = AudioTrack.getMinBufferSize(
                rate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (minBytes <= 0) return@runCatching AudioLowLatencySupport.UNREADABLE

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(minBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
            try {
                if (track.performanceMode == AudioTrack.PERFORMANCE_MODE_LOW_LATENCY) {
                    AudioLowLatencySupport.SUPPORTED
                } else {
                    AudioLowLatencySupport.FALLBACK
                }
            } finally {
                track.release()
            }
        }.getOrDefault(AudioLowLatencySupport.UNREADABLE)
    }
}
