/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.ui.util

import android.content.Context
import nd.max.core.audio.AudioSoundPreset

/**
 * ما يُحفظ من محرّك الصوت: **مفتاحه ووصفته وشدّته** — لا جلسات النظام.
 *
 * ### ولماذا وُجد
 *
 * كان `AudioEffectService` يحمل حالته في ذاكرة العملية وحدها (`masterEnabled` ·
 * `activePreset` · `activeIntensity`)، فمن أطفأ الجهاز عاد وقد «نُسي» إعداد الصوت — بينما
 * القسم الذي يَعِد بحفظ كل خيار لا يستطيع أن يقول ذلك وفمُه مغلق. الحفظ هنا لا يُعيد الجلسات
 * (وهي ملك النظام وتُلتقط عند فتحها)؛ يُعيد **القرار** فيُطبَّق على أول جلسة تفتح.
 *
 * ### والأسماء لا الأرقام
 *
 * الوصفة تُخزَّن برمزها (`bass` · `movie`)؛ رمز لا يُعرف يعود إلى `OFF` — فلا تُحوَّل وصفة
 * محفوظة إلى أخرى عند إعادة ترتيب القائمة.
 */
object AudioEffectPrefs {

    private const val PREFS = "maxmanager_audio_effect"
    private const val KEY_ENABLED = "audio_effect_enabled"
    private const val KEY_PRESET = "audio_effect_preset"
    private const val KEY_INTENSITY = "audio_effect_intensity"

    data class State(
        val enabled: Boolean = false,
        val preset: AudioSoundPreset = AudioSoundPreset.OFF,
        val intensity: Int = 50,
    )

    fun load(context: Context): State {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val token = p.getString(KEY_PRESET, null)
        return State(
            enabled = p.getBoolean(KEY_ENABLED, false),
            preset = token?.let(AudioSoundPreset::ofToken) ?: AudioSoundPreset.OFF,
            intensity = p.getInt(KEY_INTENSITY, 50).coerceIn(0, 100)
        )
    }

    fun save(context: Context, enabled: Boolean, preset: AudioSoundPreset, intensity: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            putBoolean(KEY_ENABLED, enabled)
            putString(KEY_PRESET, preset.token)
            putInt(KEY_INTENSITY, intensity.coerceIn(0, 100))
            apply()
        }
    }
}
