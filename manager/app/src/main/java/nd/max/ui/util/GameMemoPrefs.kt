/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import android.content.Context
import nd.max.core.gamespace.GameMemo

/**
 * تخزين المذكّرة في متجر `settings` نفسه الذي تقرؤه `GamePanelService` وتستمع إليه.
 *
 * والاختيار مقصود: مفتاح `game_memo_*` يصل إلى الخدمة بلا مستمع ثانٍ وبلا إعادة تشغيل، كما
 * تفعل `DisplayVeilPrefs`. والقيمة الغائبة تعني **لا مذكّرة** لا مذكّرة فارغة تُعرض إطارًا
 * بلا نصّ (ADR-07).
 */
object GameMemoPrefs {
    private const val PREFS_NAME = "settings"
    private const val KEY_TEXT = "game_memo_text"
    private const val KEY_VISIBLE = "game_memo_visible"

    fun load(context: Context): GameMemo = runCatching {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        GameMemo.of(
            raw = prefs.getString(KEY_TEXT, null),
            visible = prefs.getBoolean(KEY_VISIBLE, false),
        )
    }.getOrDefault(GameMemo())

    fun save(context: Context, memo: GameMemo) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_TEXT, GameMemo.sanitize(memo.text))
                .putBoolean(KEY_VISIBLE, memo.visible && memo.text.isNotBlank())
                .apply()
        }
    }

    /** إظهار/إخفاء بلا لمس النصّ — البلاطة تفعل هذا بعد الحفظ. */
    fun setVisible(context: Context, visible: Boolean) {
        runCatching {
            val current = load(context)
            save(context, current.copy(visible = visible && current.text.isNotBlank()))
        }
    }

    fun reset(context: Context) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .remove(KEY_TEXT)
                .remove(KEY_VISIBLE)
                .apply()
        }
    }
}
