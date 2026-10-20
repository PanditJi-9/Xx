/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import android.content.Context
import android.content.SharedPreferences

/**
 * نصّ نافذة الكتابة التي تُفتح من كلمة MAX في الشاشة الرئيسية.
 *
 * يُحفظ في ملفّ التفضيلات المشترك الذي تستعمله [HomeDeckStore]، بمفتاحٍ خاصّ، فلا ملفّ ثانٍ.
 * والقيمة الغائبة تعني نصًّا فارغًا لا خطأً.
 */
class HomeNoteStore(private val prefs: SharedPreferences) {

    var text: String
        get() = prefs.getString(KEY_TEXT, null).orEmpty()
        set(value) {
            prefs.edit().putString(KEY_TEXT, value).apply()
        }

    companion object {
        private const val PREFS = "settings"
        private const val KEY_TEXT = "home_note_text"

        fun of(context: Context): HomeNoteStore =
            HomeNoteStore(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))
    }
}
