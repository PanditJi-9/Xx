/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * الاختيارات التي تحتاجها **الواجهة والإقلاع معًا**، في ملف `settings_prefs` واحد.
 *
 * الملف صغير بقصد: كل ما يُوضع هنا يجب أن يقرأه طرفان مختلفان (شاشة + هيكل التطبيق أو
 * إقلاع العملية)، وإلا فمكانه الـViewModel أو ملف الخصائص الذي يملكه.
 *
 * - **اللغة**: تكتبها شاشة الإعدادات، ويقرأها [AppLanguage] عند كل إقلاع — فلا يجوز أن تعيش
 *   في حالة شاشة تموت بموت العملية.
 * - **الخيارات المتقدّمة**: علم مستخدم يُخفي/يُظهر ما لا يُنصح به عامة.
 * - **التمرير الأفقي**: يقرأه هيكل التطبيق (`MainActivity`) لتفعيل الإيماءة، وتكتبه الإعدادات.
 *
 * ولون الواجهة والسمة لا يعيشان هنا: لهما ملفهما (`settings` / `color_mode`) في `ui/theme`.
 */
class SettingsPreference private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    /** نسخة حيّة في الذاكرة لكل قيمة: تُقرأ مرة عند الإنشاء، وتُحدَّث عند كل كتابة. */
    private val language = MutableStateFlow(readLanguage())
    private val advanced = MutableStateFlow(prefs.getBoolean(KEY_ADVANCED_MODE, false))
    private val swipeNavigation = MutableStateFlow(prefs.getBoolean(KEY_SWIPE_NAVIGATION, true))

    /** وسم اللغة المحفوظ؛ [`AppLanguage.AUTO`] تعني «اتبع لغة النظام». */
    val currentLanguageCode: StateFlow<String> = language.asStateFlow()

    /** هل فعّل المستخدم الخيارات المتقدّمة؟ (مطفأة افتراضيًّا) */
    val isAdvancedMode: StateFlow<Boolean> = advanced.asStateFlow()

    /** هل التمرير الأفقي بين الأقسام والتبويبات مفعّل؟ (مفعّل افتراضيًّا) */
    val isSwipeNavigationEnabled: StateFlow<Boolean> = swipeNavigation.asStateFlow()

    fun setLanguageCode(code: String) {
        prefs.edit { putString(KEY_LANGUAGE, code) }
        language.value = code
    }

    fun setAdvancedModeEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ADVANCED_MODE, enabled) }
        advanced.value = enabled
    }

    fun setSwipeNavigationEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SWIPE_NAVIGATION, enabled) }
        swipeNavigation.value = enabled
    }

    private fun readLanguage(): String =
        prefs.getString(KEY_LANGUAGE, AppLanguage.AUTO)?.takeIf { it.isNotEmpty() } ?: AppLanguage.AUTO

    companion object {
        const val PREFS_FILE = "settings_prefs"

        /** المفتاح نفسه الذي يقرأه [AppLanguage.savedTag] — لا يُغيَّر إلا بتغييرهما معًا. */
        private const val KEY_LANGUAGE = "app_language_code"
        private const val KEY_ADVANCED_MODE = "is_advanced_mode_enabled"

        /**
         * الغياب يعني `true`: الميزة تُسلَّم مفعّلة لمن ثبّت التطبيق قبل وجودها، بلا
         * ترحيل يكتب مفتاحًا لكل مستخدم. والإطفاء وحده يُكتب.
         */
        private const val KEY_SWIPE_NAVIGATION = "is_swipe_navigation_enabled"

        @Volatile
        private var instance: SettingsPreference? = null

        /** نسخة واحدة لكل عملية: الملف مخزون مشترك، وقراءتان متوازيتان لا تُنتجان شيئًا مختلفًا. */
        fun getInstance(context: Context): SettingsPreference =
            instance ?: synchronized(this) {
                instance ?: SettingsPreference(context).also { instance = it }
            }
    }
}
