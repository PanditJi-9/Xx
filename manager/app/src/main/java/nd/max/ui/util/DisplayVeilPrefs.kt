/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
package nd.max.ui.util

import android.content.Context
import nd.max.core.gamespace.DisplayVeilState
import nd.max.core.gamespace.DisplayVeilTone

/**
 * تفضيلات ستارة العرض — في مخزن `settings` نفسه الذي تقرأه خدمة اللوحة.
 *
 * **ولماذا هنا لا في مخزن خاصّ:** الخدمة تستمع إلى تغيّرات `settings` أصلًا ([GamePanelService]
 * تفرّق بين مفاتيح التصويب ومفاتيح المقبض)، فمفتاح جديد يبدأ بـ`game_veil_` يصل إلى موضعه بلا
 * مستمع ثانٍ ولا إعادة تشغيل خدمة.
 *
 * **والقراءة تُطهِّر:** مفتاح مجهول أو ملفّ قديم لا يُسقط الستارة؛ [DisplayVeilTone.fromKey]
 * تعيد الدافئ فلا يبقى لون محفوظ لا وجود له.
 */
object DisplayVeilPrefs {

    private const val PREFS_NAME = "settings"

    private const val KEY_ENABLED = "game_veil_enabled"
    private const val KEY_TONE = "game_veil_tone"

    fun load(context: Context): DisplayVeilState = runCatching {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        DisplayVeilState(
            enabled = p.getBoolean(KEY_ENABLED, false),
            tone = DisplayVeilTone.fromKey(p.getString(KEY_TONE, null))
        )
    }.getOrDefault(DisplayVeilState())

    fun save(context: Context, state: DisplayVeilState) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, state.enabled)
            .putString(KEY_TONE, state.tone.key)
            .apply()
    }

    /** تبديل سريع من داخل اللوحة: مفتاح واحد فقط، فلا يُكتب لون لم يختره المستخدم. */
    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    /** إعادة الضبط: حذف المفتاحين فيعود [load] إلى «مطفأة». */
    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_ENABLED)
            .remove(KEY_TONE)
            .apply()
    }
}
