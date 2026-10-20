/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.ui.component

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import nd.max.ui.theme.ThemeStudio

/** اسم ملفّ التفضيلات الذي يكتب فيه التطبيق خيارات الإعدادات (والثيم) — نصًّا واحدًا. */
private const val SETTINGS_PREFS = "settings"

/**
 * **هل تُعرض الميزات غير المتاحة على الجهاز (رماديةً مع سببها)؟**
 *
 * عقد خطة المستوى (§٤-و): الميزات غير المتاحة على الجهاز تُعرض رماديةً مع توضيح السبب، وللمستخدم
 * مفتاح «إخفاء الميزات غير المتاحة على جهازي» يخفيها من الأماكن المناسبة — **دون حذف وظيفة
 * متاحة أو حجب معلومة أساسيّة**. فهذه البوابة الواحدة تجيب السؤال من التفضيل الواحد
 * ([ThemeStudio.hideUnavailableFeatures])، وتمرّ بها أسطح مصفوفات القدرات معًا — فلا يخفى سطحٌ
 * ويبقى آخر يعرض ما أخفاه المستخدم.
 *
 * **والافتراضيّ «تُعرض»:** القيمة الغائبة في التفضيلات = عرض. من لم يقلب المفتاح لا يتغيّر عنده
 * شيء بعد التحديث، وهي نفس قراءة [rememberScrollEffectsEnabled] في هذا المجلد (نمطٌ واحد
 * لقراءة تفضيل الحفظ نفسه، فلا مساران لتخزين واحد).
 *
 * **وحدّها:** لا تقرّر «ما هو غير متاح» — ذلك حكمُ الطبقة الصافية (`AccessLevel.NONE` في مصفوفة
 * العتاد، و`AudioSupport.UNAVAILABLE` المقيس في مصفوفة الصوت). وهنا عرضُ ذلك الحكم فقط.
 */
@Composable
fun rememberUnavailableVisible(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            !ThemeStudio.hideUnavailableFeatures(
                context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
            )
        }.getOrDefault(true)
    }
}
