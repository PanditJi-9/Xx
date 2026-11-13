/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import android.content.Context
import nd.max.core.input.ShoulderPadLayout
import nd.max.core.input.ShoulderPadMath
import nd.max.core.input.PadSide

/**
 * موضع زرّي الكتف وحالتهما — في متجر `settings` نفسه الذي تستمع إليه خدمة اللوحة.
 *
 * الاختيار مقصود (كـ[DisplayVeilPrefs]): مفتاح `game_shoulder_*` يصل إلى الخدمة بلا مستمع
 * ثانٍ وبلا إعادة تشغيل. والقيم تُنظَّف عند القراءة، فملفّ تفضيلات قديم أو معطوب لا يرسم
 * زرًّا خارج الشاشة.
 */
object ShoulderPadPrefs {

    private const val PREFS_NAME = "settings"
    private const val KEY_ENABLED = "game_shoulder_pads"
    private const val KEY_LEFT_X = "game_shoulder_left_x"
    private const val KEY_LEFT_Y = "game_shoulder_left_y"
    private const val KEY_RIGHT_X = "game_shoulder_right_x"
    private const val KEY_RIGHT_Y = "game_shoulder_right_y"

    data class State(val enabled: Boolean = false, val layout: ShoulderPadLayout = ShoulderPadLayout())

    fun load(context: Context): State = runCatching {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaults = ShoulderPadLayout()
        State(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            layout = ShoulderPadMath.normalized(
                ShoulderPadLayout(
                    leftX = prefs.getFloat(KEY_LEFT_X, defaults.leftX),
                    leftY = prefs.getFloat(KEY_LEFT_Y, defaults.leftY),
                    rightX = prefs.getFloat(KEY_RIGHT_X, defaults.rightX),
                    rightY = prefs.getFloat(KEY_RIGHT_Y, defaults.rightY),
                )
            ),
        )
    }.getOrDefault(State())

    fun setEnabled(context: Context, enabled: Boolean) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ENABLED, enabled)
                .apply()
        }
    }

    /** يحفظ موضع جهة واحدة بعد سحبها — الجهة الأخرى تبقى كما هي. */
    fun savePosition(context: Context, side: PadSide, xFraction: Float, yFraction: Float) {
        runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            when (side) {
                PadSide.LEFT -> prefs.putFloat(KEY_LEFT_X, ShoulderPadMath.clampFraction(xFraction))
                    .putFloat(KEY_LEFT_Y, ShoulderPadMath.clampFraction(yFraction))
                PadSide.RIGHT -> prefs.putFloat(KEY_RIGHT_X, ShoulderPadMath.clampFraction(xFraction))
                    .putFloat(KEY_RIGHT_Y, ShoulderPadMath.clampFraction(yFraction))
            }
            prefs.apply()
        }
    }
}
