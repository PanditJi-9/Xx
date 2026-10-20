/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * الصوت — **زمن المسار**: النموذج النقيّ وحده، بلا `AudioManager` ولا `AudioTrack`.
 *
 * **وما يُقال هنا بالضبط:** الرقم المحسوب هو **زمن مخزن الخَرْج** كما تُعلنه المنصّة
 * (`PROPERTY_OUTPUT_FRAMES_PER_BUFFER` ÷ `PROPERTY_OUTPUT_SAMPLE_RATE`)، وليس قياس رحلة
 * ذهاب وعودة. من يعرضه يقول «محسوب من إعلان المنصّة» ولا يقول «قياس» — والفرق هو نفسه
 * الذي يمنع `ADR-07`: لا قدرة مُبالَغ فيها ولا رقم بلا مصدر.
 *
 * **ولماذا لا يوجد هنا ادّعاء «تحسين»:** لا واجهة نظام عامّة تكتب زمن المسار الصوتي كلّه
 * (المسار يختاره HAL والمنصّة لكلّ تدفّق). فالواجهة تعرض ما قُرئ وتعلن حدوده، ولا تعرض
 * زرًّا يكتب شيئًا لا يمكن التحقّق منه.
 */
package nd.max.core.audio

import kotlin.math.roundToInt

/**
 * حكم **إعلان** المسار منخفض التأخير لهذا المخرج — ثلاثة أحكام لا اثنان:
 * مدعوم · تراجع (المنصّة قبلت الطلب لكنها منحت المسار العاديّ) · غير مقروء (عجز القارئ).
 */
enum class AudioLowLatencySupport { SUPPORTED, FALLBACK, UNREADABLE }

/**
 * قراءة واحدة لزمن المسار. وكلُّ حقل قد يكون `null`/غير مقروء، ولا يُكمَّل برقم شائع.
 *
 * [bufferMillis] مشتقّ من [sampleRateHz] و[framesPerBuffer] معًا؛ فإن غاب أحدهما بقي `null`
 * ولو كان الآخر مقروءًا — فلا يُحسب زمن من نصف معادلة.
 */
data class AudioLatencyReading(
    val sampleRateHz: Int?,
    val framesPerBuffer: Int?,
    val bufferMillis: Double?,
    val lowLatency: AudioLowLatencySupport,
) {
    /** هل قُرئ ما يكفي لحساب زمن المخزن؟ (`false` تعني «غير مقروء» لا «صفر»). */
    val bufferMeasured: Boolean get() = bufferMillis != null
}

/**
 * حساب زمن المخزن: الإطارات ÷ المعدّل × ١٠٠٠ (مِلّي ثانية).
 * والذي لا يُقرأ `null` — والقيم غير الموجبة ليست قراءة، بل غياب قيمة (ADR-07).
 */
fun audioBufferMillis(sampleRateHz: Int?, framesPerBuffer: Int?): Double? {
    val rate = sampleRateHz?.takeIf { it > 0 } ?: return null
    val frames = framesPerBuffer?.takeIf { it > 0 } ?: return null
    return frames * 1000.0 / rate
}

/**
 * عرض الزمن بمنزلة عشرية واحدة دائمًا — فما يُحسب قبل العرض يطابق ما يُقرأ بعده، ولا تختلف
 * الصياغة بين شاشتين. والغياب `null` فلا يُكتب `0.0` مكانه.
 */
fun audioBufferMillisText(millis: Double?): String? {
    val value = millis ?: return null
    return ((value * 10.0).roundToInt() / 10.0).toString()
}

/** يبني القراءة من إعلان المنصّة وحكم المسار — دالّة صافية تُختبر بلا جهاز. */
fun audioLatencyReading(
    capabilities: AudioOutputCapabilities?,
    support: AudioLowLatencySupport,
): AudioLatencyReading = AudioLatencyReading(
    sampleRateHz = capabilities?.sampleRateHz,
    framesPerBuffer = capabilities?.framesPerBuffer,
    bufferMillis = audioBufferMillis(capabilities?.sampleRateHz, capabilities?.framesPerBuffer),
    lowLatency = support,
)
