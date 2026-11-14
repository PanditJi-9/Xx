/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import android.content.Context
import nd.max.core.input.ExtraPoint
import nd.max.core.input.GameExtrasMath
import nd.max.core.input.MacroMath
import nd.max.core.input.MacroStep

/**
 * إضافات اللعبة في متجر `settings` نفسه — لكل لعبة بمفتاحها.
 *
 * **ولماذا لكل لعبة:** نقطة فوق زرّ في لعبة ليست نقطة في أخرى؛ وخلطها في مفتاح واحد يعيد
 * المشكلة التي وُلدت منها الإضافات. والقراءة تنظّف المخزَّن، فملفّ تفضيلات قديم لا يرسم نقطة
 * خارج الشاشة.
 */
object GameExtrasPrefs {

    private const val PREFS_NAME = "settings"
    private const val KEY_TURBO = "game_turbo_strip"
    private const val KEY_TURBO_Y = "game_turbo_strip_y"
    private const val KEY_SCOUT = "game_extra_scout"
    private const val KEY_HAND = "game_extra_hand"

    private fun enabledKey(pkg: String) = "game_extra_enabled_$pkg"
    private fun pointsKey(pkg: String) = "game_extra_points_$pkg"
    private fun macroKey(pkg: String) = "game_extra_macro_$pkg"

    data class State(
        val enabled: Boolean = false,
        val points: List<ExtraPoint> = emptyList(),
        val macro: List<MacroStep> = emptyList(),
        val scout: Boolean = false,
        val hand: Boolean = false,
        val turbo: Boolean = false,
        val turboY: Float = 0.40f,
    )

    fun load(context: Context, pkg: String): State = runCatching {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        State(
            enabled = prefs.getBoolean(enabledKey(pkg), false),
            points = GameExtrasMath.decode(prefs.getString(pointsKey(pkg), null)),
            macro = MacroMath.decode(prefs.getString(macroKey(pkg), null)),
            scout = prefs.getBoolean(KEY_SCOUT, false),
            hand = prefs.getBoolean(KEY_HAND, false),
            turbo = prefs.getBoolean(KEY_TURBO, false),
            turboY = prefs.getFloat(KEY_TURBO_Y, 0.40f).coerceIn(0.05f, 0.85f),
        )
    }.getOrDefault(State())

    fun setEnabled(context: Context, pkg: String, enabled: Boolean) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(enabledKey(pkg), enabled).apply()
        }
    }

    fun savePoints(context: Context, pkg: String, points: List<ExtraPoint>) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(pointsKey(pkg), GameExtrasMath.encode(points)).apply()
        }
    }

    fun saveMacro(context: Context, pkg: String, steps: List<MacroStep>) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(macroKey(pkg), MacroMath.encode(steps)).apply()
        }
    }

    fun setScout(context: Context, enabled: Boolean) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_SCOUT, enabled).apply()
        }
    }

    fun setHand(context: Context, enabled: Boolean) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_HAND, enabled).apply()
        }
    }

    fun setTurbo(context: Context, enabled: Boolean) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_TURBO, enabled).apply()
        }
    }

    fun saveTurboY(context: Context, fraction: Float) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putFloat(KEY_TURBO_Y, fraction.coerceIn(0.05f, 0.85f)).apply()
        }
    }
}
