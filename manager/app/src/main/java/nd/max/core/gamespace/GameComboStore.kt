/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.gamespace

import android.content.Context

/**
 * مخزن المجموعات (§1.1) — **ملفّ تفضيلات واحد**، والترميز في [GameComboCodec] النقيّ.
 *
 * والمجموعات **عامّة لا لكل لعبة**: هذا معنى «قابلة لإعادة الاستخدام» في الطلب — تُبنى مرّة
 * من إعدادات لعبة، وتُطبَّق على أيّ لعبة أخرى، فمخزنها مفرد ولا يُكرَّر في كل ملفّ لعبة.
 *
 * و[save] تعيد `Boolean` من `commit()` (لا `apply()`): الواجهة تقول «حُفظت» **بعد** أن تُقرَّر
 * الكتابة، وإلا فشل الكتابة يمرّ صامتًا ويرى المستخدم مجموعةً وُعدت ولم توجد (سابقة
 * `GamePanelPrefs.setEnabled` نفسها).
 */
object GameComboStore {

    private const val PREFS = "game_combos"
    private const val KEY = "combos"

    fun load(context: Context): List<GameCombo> =
        GameComboCodec.decode(prefs(context).getString(KEY, null))

    fun save(context: Context, combos: List<GameCombo>): Boolean {
        val bounded = combos.take(GameComboCodec.MAX_COMBOS).map { combo ->
            combo.copy(name = combo.name.trim().take(GameComboCodec.MAX_NAME_LENGTH))
        }
        return prefs(context).edit()
            .putString(KEY, GameComboCodec.encode(bounded))
            .commit()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
