/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import android.content.Context
import nd.max.core.input.A11yGestureInjector
import nd.max.core.privilege.PrivilegeManager

/**
 * إقرار **حقن اللمس**: قدرة شبه خطيرة تُبنى حقيقيّة، لكن لا تعمل قبل أن يقرأ المستخدم
 * ما تفعله ويوافق (ADR-93/§0.8).
 *
 * النصّ في الواجهة يقول بالحرف: التطبيق يولّد لمسات (نقرات/سحبات) في اللعبة، وأنها قد
 * تُرفض من بعض الألعاب، وأن الحارس لا يرى الإصبع بلا جذر. وهذا الإقرار **مرة واحدة**،
 * ويبقى قابلًا للإلغاء بإيقاف الميزة نفسها.
 */
object InjectionConsentPrefs {

    private const val PREFS_NAME = "settings"
    private const val KEY_ACK = "game_touch_injection_consent"

    fun acknowledged(context: Context): Boolean = runCatching {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ACK, false)
    }.getOrDefault(false)

    fun acknowledge(context: Context) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ACK, true).apply()
        }
    }

    /** هل يوجد مسار حقن الآن؟ خدمة الوصول أو صدفة مميّزة. */
    fun laneAvailable(context: Context): Boolean {
        if (A11yGestureInjector.available) return true
        return runCatching {
            val snapshot = PrivilegeManager.refresh()
            snapshot.rootGranted || snapshot.shizuku.ready
        }.getOrDefault(false)
    }
}
