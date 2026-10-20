/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.widget.core

/**
 * ألوان الودجات — دالات نقيّة تُختبر في JVM.
 *
 * القيم هنا **مقصودة لا مشتقّة** من `MaterialTheme`: الودجة تُرسم خارج سياق
 * التطبيق (بواسطة المشغّل)، فلا يصلها المخطّط الديناميكي ولا وضع السمة الجاري —
 * فتُحمل لوحة الودجة بنفسها، بأزواج (نهاريّ/ليليّ)، وقيم الحالات الثابتة
 * منسوخة من عقد `MaxTone` نفسه (`ui/design/MaxTokens.kt`): أخضر/كهرماني/أحمر
 * لا تتغيّر بتغيّر السمة — لأن تحذيرًا حراريًّا يبدو زخرفيًّا عطب صحّة، لا ذوق.
 *
 * [WidgetThemeMode.System] يُبقي الزوجين فيسلّمهما العميل إلى `ColorProvider`
 * فيختار النظام؛ و[WidgetThemeMode.Light]/[WidgetThemeMode.Dark] يفرضان أحدهما
 * على الطرفين معًا — فالفرض قرار المستخدم لا حالة النظام.
 */
data class WidgetAccentColors(
    val day: Long,
    val night: Long,
) {
    fun resolve(mode: WidgetThemeMode): Pair<Long, Long> = when (mode) {
        WidgetThemeMode.System -> day to night
        WidgetThemeMode.Light -> day to day
        WidgetThemeMode.Dark -> night to night
    }
}

/** لوحة الودجة الكاملة: خلفيات ونصوص وأشرطة ولون تمييز، كلها أزواج. */
data class WidgetPalette(
    val accentDay: Long,
    val accentNight: Long,
    val backgroundDay: Long,
    val backgroundNight: Long,
    val surfaceDay: Long,
    val surfaceNight: Long,
    val textDay: Long,
    val textNight: Long,
    val textDimDay: Long,
    val textDimNight: Long,
    val trackDay: Long,
    val trackNight: Long,
)

/** نبرة حالة — نسخة الودجة من `MaxTone` بحالاته الستّ الباقية لها معنى هنا. */
enum class WidgetTone {
    Neutral,
    Accent,
    Positive,
    Caution,
    Critical,
}

object WidgetTheme {

    // الأسطح — رماديّ مائل للأزرق يفصل الودجة عن أيّ خلفية مشغّل، بلا صراخ.
    private const val BG_DAY = 0xFFF1F4F7
    private const val BG_NIGHT = 0xFF0E1116
    private const val SURFACE_DAY = 0xFFFFFFFF
    private const val SURFACE_NIGHT = 0xFF181D24
    private const val TEXT_DAY = 0xFF10151B
    private const val TEXT_NIGHT = 0xFFF2F5F8
    private const val DIM_DAY = 0xFF5B6672
    private const val DIM_NIGHT = 0xFF98A3AF
    private const val TRACK_DAY = 0xFFDCE2E9
    private const val TRACK_NIGHT = 0xFF2A313B

    // الحالات — منسوخة بقيَمها من `MaxTone` كي لا تتباعد لغة الودجة عن لغة الشاشة.
    private const val POSITIVE_DAY = 0xFF0B6B4F
    private const val POSITIVE_NIGHT = 0xFF5FD9AC
    private const val CAUTION_DAY = 0xFF8A5200
    private const val CAUTION_NIGHT = 0xFFFFB86B
    private const val CRITICAL_DAY = 0xFF9A1B1B
    private const val CRITICAL_NIGHT = 0xFFFF9A90

    /** لون التمييز لكل خيار — نهاريّ داكن/ليليّ فاتح، كلها مقروءة على السطحين. */
    fun accent(accent: WidgetAccent): WidgetAccentColors = when (accent) {
        WidgetAccent.Brand -> WidgetAccentColors(0xFF52707F, 0xFF93B7C9)
        WidgetAccent.Teal -> WidgetAccentColors(0xFF00707C, 0xFF4DD0E1)
        WidgetAccent.Violet -> WidgetAccentColors(0xFF6C4DFF, 0xFFB9A6FF)
        WidgetAccent.Amber -> WidgetAccentColors(0xFFB26A00, 0xFFFFC069)
        WidgetAccent.Rose -> WidgetAccentColors(0xFFB6386B, 0xFFFF9BB8)
        WidgetAccent.Green -> WidgetAccentColors(0xFF0B6B4F, 0xFF5FD9AC)
    }

    fun palette(accent: WidgetAccent, mode: WidgetThemeMode): WidgetPalette {
        val (accentDay, accentNight) = accent(accent).resolve(mode)
        val (bgDay, bgNight) = pair(BG_DAY, BG_NIGHT, mode)
        val (surfaceDay, surfaceNight) = pair(SURFACE_DAY, SURFACE_NIGHT, mode)
        val (textDay, textNight) = pair(TEXT_DAY, TEXT_NIGHT, mode)
        val (dimDay, dimNight) = pair(DIM_DAY, DIM_NIGHT, mode)
        val (trackDay, trackNight) = pair(TRACK_DAY, TRACK_NIGHT, mode)
        return WidgetPalette(
            accentDay = accentDay,
            accentNight = accentNight,
            backgroundDay = bgDay,
            backgroundNight = bgNight,
            surfaceDay = surfaceDay,
            surfaceNight = surfaceNight,
            textDay = textDay,
            textNight = textNight,
            textDimDay = dimDay,
            textDimNight = dimNight,
            trackDay = trackDay,
            trackNight = trackNight,
        )
    }

    /** لونا نبرة الحالة — والفرض يعمل عليها كما يعمل على بقية اللوحة. */
    fun tone(tone: WidgetTone, mode: WidgetThemeMode): Pair<Long, Long> = when (tone) {
        WidgetTone.Positive -> pair(POSITIVE_DAY, POSITIVE_NIGHT, mode)
        WidgetTone.Caution -> pair(CAUTION_DAY, CAUTION_NIGHT, mode)
        WidgetTone.Critical -> pair(CRITICAL_DAY, CRITICAL_NIGHT, mode)
        WidgetTone.Neutral -> pair(DIM_DAY, DIM_NIGHT, mode)
        WidgetTone.Accent -> pair(0x00000000, 0x00000000, mode) // مكانه لوحة التمييز
    }

    private fun pair(day: Long, night: Long, mode: WidgetThemeMode): Pair<Long, Long> = when (mode) {
        WidgetThemeMode.System -> day to night
        WidgetThemeMode.Light -> day to day
        WidgetThemeMode.Dark -> night to night
    }

    /**
     * تصنيف حرارة البطارية إلى نبرة — الحدود **بمئويّات مئوية حقيقية**:
     * تحت ٣٨ طبيعيّ عمليًّا، وحتى ٤٣ يستحقّ انتباهًا، وفوقها حمراء.
     * والدالّة نقيّة لتُختبر: لا `Context` ولا جهاز.
     */
    fun batteryTone(tempC: Float?): WidgetTone = when {
        tempC == null -> WidgetTone.Neutral
        tempC >= 43f -> WidgetTone.Critical
        tempC >= 38f -> WidgetTone.Caution
        else -> WidgetTone.Positive
    }

    /** نبرة شريط امتلاء (ذاكرة/مساحة): ٧٥٪ انتباه و٩٠٪ حرج — نفس عتبات الواجهة. */
    fun loadTone(percent: Int?): WidgetTone = when {
        percent == null -> WidgetTone.Neutral
        percent >= 90 -> WidgetTone.Critical
        percent >= 75 -> WidgetTone.Caution
        else -> WidgetTone.Positive
    }

    /**
     * نبرة نسبة البطارية: ١٥٪ وأدنى حرجة، وحتى ٣٠٪ انتباه — عتبتان تختلفان
     * عن [loadTone] قصدًا: بطارية على ٢٠٪ خبرٌ أسوأ من ذاكرة على ٢٠٪.
     */
    fun batteryPercentTone(percent: Int?): WidgetTone = when {
        percent == null -> WidgetTone.Neutral
        percent <= 15 -> WidgetTone.Critical
        percent <= 30 -> WidgetTone.Caution
        else -> WidgetTone.Positive
    }
}
