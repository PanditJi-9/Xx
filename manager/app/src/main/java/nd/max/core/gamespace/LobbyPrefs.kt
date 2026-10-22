/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import android.content.Context

/**
 * مخزن خيارات اللوبي (SharedPreferences منفصلة باسم `game_lobby`). النموذج النقيّ في
 * [LobbyPrefsState]؛ هنا القراءة والكتابة فقط. الكتابة `apply()` غير متزامنة فلا تحجب الواجهة.
 */
object LobbyPrefs {
    private const val PREFS_NAME = "game_lobby"
    private const val KEY_DEPTH = "depth"
    private const val KEY_GAME_COLORS = "game_colors"
    private const val KEY_AMBIENT = "ambient"
    private const val KEY_STATUS = "status"
    private const val KEY_SORT = "sort"
    private const val KEY_FLASH = "launch_flash"
    private const val KEY_HAPTICS = "haptics"
    private const val KEY_SOUNDS = "sounds"
    private const val KEY_ENTRANCE = "entrance"
    private const val KEY_AWAKE = "keep_screen_on"
    private const val KEY_RESUME = "resume_last"
    private const val KEY_LAST = "last_package"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(context: Context): LobbyPrefsState {
        val p = prefs(context)
        val d = LobbyPrefsState()
        return LobbyPrefsState(
            depth = lobbyEnumOr(p.getString(KEY_DEPTH, null), LobbyDepth.entries, d.depth),
            gameColors = p.getBoolean(KEY_GAME_COLORS, d.gameColors),
            ambient = p.getBoolean(KEY_AMBIENT, d.ambient),
            showStatus = p.getBoolean(KEY_STATUS, d.showStatus),
            sort = lobbyEnumOr(p.getString(KEY_SORT, null), LobbySort.entries, d.sort),
            launchFlash = p.getBoolean(KEY_FLASH, d.launchFlash),
            haptics = p.getBoolean(KEY_HAPTICS, d.haptics),
            sounds = p.getBoolean(KEY_SOUNDS, d.sounds),
            entrance = p.getBoolean(KEY_ENTRANCE, d.entrance),
            keepScreenOn = p.getBoolean(KEY_AWAKE, d.keepScreenOn),
            resumeLast = p.getBoolean(KEY_RESUME, d.resumeLast),
            lastPackage = p.getString(KEY_LAST, null)?.takeIf { it.isNotBlank() }
        )
    }

    fun save(context: Context, state: LobbyPrefsState) {
        val editor = prefs(context).edit()
        editor.putString(KEY_DEPTH, state.depth.name)
        editor.putBoolean(KEY_GAME_COLORS, state.gameColors)
        editor.putBoolean(KEY_AMBIENT, state.ambient)
        editor.putBoolean(KEY_STATUS, state.showStatus)
        editor.putString(KEY_SORT, state.sort.name)
        editor.putBoolean(KEY_FLASH, state.launchFlash)
        editor.putBoolean(KEY_HAPTICS, state.haptics)
        editor.putBoolean(KEY_SOUNDS, state.sounds)
        editor.putBoolean(KEY_ENTRANCE, state.entrance)
        editor.putBoolean(KEY_AWAKE, state.keepScreenOn)
        editor.putBoolean(KEY_RESUME, state.resumeLast)
        val last = state.lastPackage
        if (last.isNullOrBlank()) editor.remove(KEY_LAST) else editor.putString(KEY_LAST, last)
        editor.apply()
    }
}
