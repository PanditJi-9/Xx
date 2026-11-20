/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * مظهر لوحة «Game Turbo»: [Neon] هو التصميم الجديد (الافتراضي)، و[Classic] هو اللوحة القائمة.
 *
 * يُخزَّن **الاسم** لا الترتيب، وأي قيمة لا تُعرف تُسقط إلى الافتراضي — نفس مبدأ [GamePanelPrefs].
 */
enum class GameTurboStyle(val key: String) {
    Neon("neon"),
    Classic("classic");

    companion object {
        /** الجديد هو الافتراضي: من لم يختر شيئًا، أو خُزِّنت له قيمة مجهولة، يرى التصميم الجديد. */
        val DEFAULT: GameTurboStyle = Neon

        fun of(raw: String?): GameTurboStyle = entries.firstOrNull { it.key == raw } ?: DEFAULT
    }
}

/**
 * اختيار مظهر اللوحة، ومتى يُسأل عنه.
 *
 * ### العقد
 *
 * - [style] يُقرأ كلّ مرة تُفتح فيها اللوحة (التفضيل في ملفّ في الذاكرة): من يغيّره من إعدادات مساحة
 *   الألعاب يراه عند الفتح التالي بلا إعادة تشغيل الخدمة.
 * - [chosen] = المستخدم اختار فعلًا مرة واحدة. النافذة المنبثقة تظهر **حين يكون `false` فقط**، فلا تعود بعد
 *   الاختيار إلا بطلبٍ صريح ([requestChooser]).
 * - الحفظ بـ`commit()` لا `apply()`: «لا تظهر مجددًا» وعدٌ لا يصحّ أن يضيع إن قُتلت العملية بعد النقر مباشرة.
 */
object GameTurboStylePrefs {

    /** نفس مخزن [GamePanelPrefs]: تفضيلات اللوحة كلّها في موضع واحد. */
    private const val PREFS_NAME = "settings"

    private const val KEY_STYLE = "game_turbo_style"
    private const val KEY_CHOSEN = "game_turbo_style_chosen"

    fun style(context: Context): GameTurboStyle = runCatching {
        GameTurboStyle.of(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_STYLE, null)
        )
    }.getOrDefault(GameTurboStyle.DEFAULT)

    fun chosen(context: Context): Boolean = runCatching {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_CHOSEN, false)
    }.getOrDefault(false)

    /** يحفظ الاختيار ويعلّم أن السؤال انتهى. يعود `false` بلا ادّعاء إن رفض المخزن الكتابة. */
    fun save(context: Context, style: GameTurboStyle): Boolean = runCatching {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_STYLE, style.key)
            .putBoolean(KEY_CHOSEN, true)
            .commit()
    }.getOrDefault(false)

    private val _chooserRequests = MutableStateFlow(0)

    /** يزيد عند كل طلب إعادة عرض النافذة؛ القيمة `0` = لا طلب بعد. */
    val chooserRequests: StateFlow<Int> = _chooserRequests.asStateFlow()

    /** يفتح نافذة الاختيار مرة أخرى عند الطلب (لا مستدعي في الواجهة اليوم: صفّ المساحة يحفظ مباشرةً). */
    fun requestChooser() {
        _chooserRequests.value = _chooserRequests.value + 1
    }
}
