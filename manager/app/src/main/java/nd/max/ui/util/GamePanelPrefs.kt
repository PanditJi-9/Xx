/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.util

import android.content.Context
import nd.max.core.gamespace.HandleSize
import nd.max.core.gamespace.PanelSensitivity
import nd.max.core.gamespace.PanelSide
import nd.max.core.gamespace.PanelShelfModel
import nd.max.core.gamespace.validGamePackages

/**
 * تفضيلات اللوحة الجانبية للألعاب — **لكلّ لعبة، لا مفتاح عامّ واحد**.
 *
 * ### ولماذا لكلّ لعبة
 *
 * اللوحة تُفتح فوق لعبة تُقاس، فمفتاح عامّ واحد يعني أن من فعّلها لِلعبة FPS يجدها تظهر فوق
 * لعبة بلا فائدة — والتراكب الذي يظهر حيث لا يُطلب هو نفسه العطب الذي مُنع في `HudSession`
 * («قارئ واحد لا قارئان» بمعنى: لا سطح يظهر بلا طلب). فالمجموعة **أسماء حزم مُتحقَّق منها**
 * ([validGamePackages])، فلا يُكتب في المخزن اسم لم يُقرأ من مصدر حقيقي.
 *
 * ### والأسماء لا الأرقام
 *
 * كما في `FpsOverlayPrefs`/`ProcessOverlayPrefs`: [PanelSide] يُخزَّن **اسمًا**، وقراءته تُسقط
 * إلى الافتراضيّ عند أي اسم لا يُعرف — فلا يُحوَّل جهاز محفوظ من جانب إلى آخر بإعادة ترتيب.
 *
 * والحدّ المعلن: مجموعة فارغة تعني «لا لعبة مُفعَّلة»، وهي الحالة الافتراضية — ولا تُقرأ خطأً
 * كـ«مفعَّل للجميع».
 */
object GamePanelPrefs {

    /** نفس مخزن `GameSpaceRepository` (مكتبة الألعاب)، فالبيانات تُدار من موضع واحد. */
    private const val PREFS_NAME = "settings"

    private const val KEY_ENABLED = "game_panel_enabled"
    private const val KEY_SNAP = "game_panel_snap"
    private const val KEY_SIDE = "game_panel_side"
    private const val KEY_HANDLE_Y = "game_panel_handle_y"
    private const val KEY_SHELF_PICKS = "game_panel_shelf_picks"
    private const val KEY_HANDLE_SIZE = "game_panel_handle_size"
    private const val KEY_SENSITIVITY = "game_panel_sensitivity"
    private const val KEY_LOCKED = "game_panel_locked"
    private const val KEY_HAPTICS = "game_panel_haptics"
    private const val USE_PREFIX = "game_panel_use_"

    /** الحافة الافتراضية: يمين الشاشة — وهي الموضع الذي تعتاده ألواح الألعاب. */
    val DEFAULT_SIDE: PanelSide = PanelSide.End

    data class State(
        val enabledPackages: Set<String> = emptySet(),
        val snapEdges: Boolean = true,
        val side: PanelSide = DEFAULT_SIDE,
        /** ارتفاع المقبض كنسبة 0..1 من المدى المتاح؛ سالب = لم يُسحب بعد (الوضع الافتراضي). */
        val handleY: Float = -1f,
        /** حجم المقبض: يُضرب في أبعاد رسمه كلها. */
        val handleSize: HandleSize = HandleSize.Medium,
        /** حساسية السحب للداخل لفتح اللوحة. */
        val sensitivity: PanelSensitivity = PanelSensitivity.Balanced,
        /** مقفل: لا يُسحب المقبض (لا عموديًّا ولا بالضغط المطوّل)؛ النقر والسحب للداخل يفتحان اللوحة كالمعتاد. */
        val locked: Boolean = false,
        /** اهتزاز اللمس والتسليح على المقبض. */
        val haptics: Boolean = true
    )

    fun load(context: Context): State {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val packages = runCatching {
            validGamePackages(p.getStringSet(KEY_ENABLED, emptySet())?.toSet().orEmpty())
        }.getOrDefault(emptySet())
        return State(
            enabledPackages = packages,
            snapEdges = p.getBoolean(KEY_SNAP, true),
            side = enumOr(p.getString(KEY_SIDE, null), PanelSide.entries, DEFAULT_SIDE),
            handleY = p.getFloat(KEY_HANDLE_Y, -1f),
            handleSize = enumOr(p.getString(KEY_HANDLE_SIZE, null), HandleSize.entries, HandleSize.Medium),
            sensitivity = enumOr(p.getString(KEY_SENSITIVITY, null), PanelSensitivity.entries, PanelSensitivity.Balanced),
            locked = p.getBoolean(KEY_LOCKED, false),
            haptics = p.getBoolean(KEY_HAPTICS, true)
        )
    }

    /** يُفعِّل/يُطفئ لعبة. ويعود `false` بلا ادّعاء نجاح إن رفض المخزن أو كان الاسم غير صالح. */
    fun setEnabled(context: Context, pkg: String, enabled: Boolean): Boolean = runCatching {
        val current = load(context)
        val next = validGamePackages(if (enabled) current.enabledPackages + pkg else current.enabledPackages - pkg)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_ENABLED, next)
            .commit()
    }.getOrDefault(false)

    fun saveSide(context: Context, side: PanelSide) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_SIDE, side.name).apply()
    }

    fun saveHandleSize(context: Context, size: HandleSize) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_HANDLE_SIZE, size.name).apply()
    }

    fun saveSensitivity(context: Context, sensitivity: PanelSensitivity) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_SENSITIVITY, sensitivity.name).apply()
    }

    fun saveLocked(context: Context, locked: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_LOCKED, locked).apply()
    }

    fun saveHaptics(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_HAPTICS, enabled).apply()
    }

    /**
     * يعيد المقبض إلى أصله: الحافة والارتفاع والحجم والحساسية والقفل والاهتزاز. **لا يمسّ** الألعاب المُفعَّلة
     * ولا شريط التطبيقات السريعة: هذه بيانات المستخدم لا مظهر المقبض.
     */
    fun resetHandle(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_SIDE, DEFAULT_SIDE.name)
            .remove(KEY_HANDLE_Y)
            .remove(KEY_HANDLE_SIZE)
            .remove(KEY_SENSITIVITY)
            .remove(KEY_LOCKED)
            .remove(KEY_HAPTICS)
            .apply()
    }

    fun saveHandleY(context: Context, fraction: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_HANDLE_Y, fraction.coerceIn(0f, 1f)).apply()
    }

    /** التطبيقات التي ثبّتها المستخدم في الشريط بترتيبها؛ ما لا يبدو اسم حزمة يُسقَط. */
    fun shelfPicks(context: Context): List<String> = runCatching {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SHELF_PICKS, null).orEmpty()
        PanelShelfModel.picksSanitized(raw.split(','))
    }.getOrDefault(emptyList())

    fun savePicks(context: Context, picks: List<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_SHELF_PICKS, PanelShelfModel.picksSanitized(picks).joinToString(","))
            .apply()
    }

    /** كم مرّة فُتح كل تطبيق من هذه اللوحة نفسها: هو ما يتعلّمه الترتيب الذكيّ. */
    fun shelfUses(context: Context): Map<String, Int> = runCatching {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).all
            .mapNotNull { (key, value) ->
                if (key.startsWith(USE_PREFIX) && value is Int) key.removePrefix(USE_PREFIX) to value else null
            }.toMap()
    }.getOrDefault(emptyMap())

    fun recordUse(context: Context, pkg: String) {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        p.edit().putInt(USE_PREFIX + pkg, p.getInt(USE_PREFIX + pkg, 0) + 1).apply()
    }

    private fun <T> enumOr(raw: String?, values: List<T>, fallback: T): T where T : Enum<T> =
        values.firstOrNull { it.name == raw } ?: fallback
}
