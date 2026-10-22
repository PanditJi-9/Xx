/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
package nd.max.ui.util

import android.content.Context
import nd.max.core.gamespace.CrosshairState
import nd.max.core.gamespace.CrosshairStyle

/**
 * تفضيلات التصويب المخصّص — في مخزن `settings` نفسه الذي يقرأه `GamePanelService`،
 * فمستمع الخدمة يلتقط كل تعديل في لحظته بلا بثّ ولا إعادة تشغيل.
 *
 * **والقراءة تُطهِّر دائمًا:** ملفّ قديم أو معدَّل يدويًّا لا يُسقط الرسم؛ كل قيمة خارج
 * مداها تُقصّ في [CrosshairState.sanitized]. والمفاتيح كلها تبدأ بـ`game_crosshair_`
 * ليتمكّن مستمع الخدمة من تمييزها عن مفاتيح مقبض اللوحة.
 */
object CrosshairPrefs {

    private const val PREFS_NAME = "settings"

    private const val KEY_ENABLED = "game_crosshair_enabled"
    private const val KEY_STYLE = "game_crosshair_style"
    private const val KEY_COLOR = "game_crosshair_color"
    private const val KEY_SIZE = "game_crosshair_size"
    private const val KEY_THICKNESS = "game_crosshair_thickness"
    private const val KEY_GAP = "game_crosshair_gap"
    private const val KEY_OPACITY = "game_crosshair_opacity"
    private const val KEY_OFFSET_X = "game_crosshair_offset_x"
    private const val KEY_OFFSET_Y = "game_crosshair_offset_y"
    private const val KEY_OUTLINE = "game_crosshair_outline"

    fun load(context: Context): CrosshairState = runCatching {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        CrosshairState(
            enabled = p.getBoolean(KEY_ENABLED, false),
            style = CrosshairStyle.fromKey(p.getString(KEY_STYLE, null)),
            color = p.getInt(KEY_COLOR, CrosshairState.DEFAULT_COLOR),
            sizeDp = p.getInt(KEY_SIZE, CrosshairState.DEFAULT_SIZE_DP),
            thicknessDp = p.getInt(KEY_THICKNESS, CrosshairState.DEFAULT_THICKNESS_DP),
            gapDp = p.getInt(KEY_GAP, CrosshairState.DEFAULT_GAP_DP),
            opacity = p.getFloat(KEY_OPACITY, CrosshairState.DEFAULT_OPACITY),
            offsetX = p.getFloat(KEY_OFFSET_X, 0f),
            offsetY = p.getFloat(KEY_OFFSET_Y, 0f),
            outline = p.getBoolean(KEY_OUTLINE, true)
        ).sanitized()
    }.getOrDefault(CrosshairState())

    fun save(context: Context, state: CrosshairState) {
        val clean = state.sanitized()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, clean.enabled)
            .putString(KEY_STYLE, clean.style.key)
            .putInt(KEY_COLOR, clean.color)
            .putInt(KEY_SIZE, clean.sizeDp)
            .putInt(KEY_THICKNESS, clean.thicknessDp)
            .putInt(KEY_GAP, clean.gapDp)
            .putFloat(KEY_OPACITY, clean.opacity)
            .putFloat(KEY_OFFSET_X, clean.offsetX)
            .putFloat(KEY_OFFSET_Y, clean.offsetY)
            .putBoolean(KEY_OUTLINE, clean.outline)
            .apply()
    }

    /** تبديل سريع من داخل اللوحة: مفتاح واحد فقط، فلا نكتب مظهرًا لم يغيّره المستخدم. */
    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    /** إعادة الضبط: تُحذف المفاتيح كلها فيعود [load] إلى الافتراضيات (ومنها «مطفأ»). */
    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_ENABLED)
            .remove(KEY_STYLE)
            .remove(KEY_COLOR)
            .remove(KEY_SIZE)
            .remove(KEY_THICKNESS)
            .remove(KEY_GAP)
            .remove(KEY_OPACITY)
            .remove(KEY_OFFSET_X)
            .remove(KEY_OFFSET_Y)
            .remove(KEY_OUTLINE)
            .apply()
    }
}
