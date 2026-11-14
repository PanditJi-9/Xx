/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import android.content.Context

/**
 * إقرار التسجيل: يُقال **مرة واحدة** ما الذي يُسجَّل وأين يُحفظ، ثم لا يُسأل ثانيةً.
 *
 * والمخزن هو `settings` نفسه الذي يقرأه `GamePanelService` ويستمع إليه، فلا مستمع ثانٍ ولا
 * إعادة تشغيل خدمة. والقيمة الغائبة تعني «لم يُقرّ» لا «أقرّ» — فالحارس الذي يبدأ صامتًا
 * ليس حارسًا (ADR-07).
 */
object CaptureConsentPrefs {
    private const val PREFS_NAME = "settings"
    private const val KEY_ACK = "game_capture_consent"

    /** هل أقرّ المالك التسجيل سابقًا؟ القراءة الفاشلة تُعامل «لم يُقرّ» لا العكس. */
    fun acknowledged(context: Context): Boolean = runCatching {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ACK, false)
    }.getOrDefault(false)

    /** يُثبت الإقرار بعد أن يقرأه المالك ويوافق. */
    fun acknowledge(context: Context) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ACK, true).apply()
        }
    }
}
