/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.privilege

import android.content.Context

/**
 * مخزن [ExecutionMode] — `SharedPreferences` منفصلة باسم `execution_mode`، مع نسخة في الذاكرة.
 *
 * النسخة الذاكرية لأن [PrivilegedShell.exec] لا يملك `Context` ويُنادى من كل عملية (التطبيق ولوحة
 * الألعاب)؛ و`MaxManagerApplication.onCreate` يستدعي [load] قبل أي تسخين لصدفة الجذر.
 * الكتابة `apply()` غير متزامنة فلا تحجب الواجهة.
 */
object ExecutionModeStore {
    private const val PREFS_NAME = "execution_mode"
    private const val KEY_MODE = "mode"

    @Volatile
    private var cached: ExecutionMode = ExecutionMode.AUTO

    /** الوضع الحالي من الذاكرة (قبل [load] يكون [ExecutionMode.AUTO] = السلوك السابق). */
    fun current(): ExecutionMode = cached

    fun load(context: Context): ExecutionMode {
        // قبل فكّ تشفير التخزين (إقلاع مباشر) قد تتعذّر القراءة: الافتراضي AUTO لا يُسقط الإقلاع.
        val mode = runCatching { ExecutionMode.fromId(prefs(context).getString(KEY_MODE, null)) }
            .getOrDefault(ExecutionMode.AUTO)
        cached = mode
        return mode
    }

    fun save(context: Context, mode: ExecutionMode) {
        cached = mode
        prefs(context).edit().putString(KEY_MODE, mode.id).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
