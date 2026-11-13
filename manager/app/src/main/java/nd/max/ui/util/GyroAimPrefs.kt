/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import android.content.Context
import nd.max.core.input.GyroAimSettings

/**
 * إعدادات مساعد التصويب الجيروسكوبيّ في متجر `settings` نفسه.
 *
 * الغياب يعني «متوقّف» لا «مفعّل» (ADR-07)، والقيم تُقيَّد عند القراءة فلا تبقى حساسية
 * ٩٩ من نسخة قديمة.
 */
object GyroAimPrefs {

    private const val PREFS_NAME = "settings"
    private const val KEY_ENABLED = "game_gyro_aim"
    private const val KEY_SENSITIVITY = "game_gyro_sensitivity"
    private const val KEY_DEAD_ZONE = "game_gyro_dead_zone"

    fun load(context: Context): GyroAimSettings = runCatching {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaults = GyroAimSettings()
        GyroAimSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            sensitivity = prefs.getFloat(KEY_SENSITIVITY, defaults.sensitivity),
            deadZone = prefs.getFloat(KEY_DEAD_ZONE, defaults.deadZone),
            smoothing = defaults.smoothing,
        ).normalized()
    }.getOrDefault(GyroAimSettings())

    fun save(context: Context, settings: GyroAimSettings) {
        val safe = settings.normalized()
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ENABLED, safe.enabled)
                .putFloat(KEY_SENSITIVITY, safe.sensitivity)
                .putFloat(KEY_DEAD_ZONE, safe.deadZone)
                .apply()
        }
    }
}
