/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.privilege

/**
 * وضع التنفيذ الذي يختاره المستخدم لمساحة الألعاب: **فصل صريح** بين الجذر وShizuku بدل الاختيار الصامت.
 *
 * - [AUTO]: السلوك السابق — الجذر إن وُجد، وإلا Shizuku، وإلا رفض.
 * - [ROOT]: الجذر وحده؛ لا يُلجأ إلى Shizuku إن غاب الجذر (يُرفض الأمر ويُقال السبب).
 * - [SHIZUKU]: Shizuku وحده؛ **لا يُستدعى `su` أبدًا** (لا نافذة صلاحية، ولا صدفة جذر تُسخَّن عند الإقلاع).
 * - [BASIC]: بلا امتياز؛ كل ما يحتاج صدفة مميَّزة يُرفض بسبب معلَن، وتبقى ما تسمح به المنصّة للتطبيق العادي.
 *
 * والوضع **يحكم مسار الأوامر المميّزة** (`PrivilegedShell.exec`) وتسخين صدفة الجذر عند الإقلاع؛ وشاشات
 * الجذر الأخرى (الوحدات، الملفات، …) تبقى على صدفتها ولا يمسّها هذا الاختيار.
 */
enum class ExecutionMode(val id: String) {
    AUTO("auto"),
    ROOT("root"),
    SHIZUKU("shizuku"),
    BASIC("basic"),
    ;

    companion object {
        /** قيمة غير معروفة أو غائبة ⇒ [AUTO] (السلوك السابق): لا يُفاجأ مستخدم قديم بتغيّر. */
        fun fromId(id: String?): ExecutionMode = entries.firstOrNull { it.id == id } ?: AUTO
    }
}

/** قواعد الوضع — **نقيّة** فتُقاس على JVM بلا جهاز (نفس مبدأ [PrivilegedShellRules]). */
object ExecutionModeRules {
    /** مفاتيح رفض ثابتة تُسجَّل وتُقارن؛ والنصّ المعروض يُشتقّ منها في الواجهة لا هنا. */
    const val REASON_ROOT_UNAVAILABLE = "mode-root-unavailable"
    const val REASON_SHIZUKU_UNAVAILABLE = "mode-shizuku-unavailable"
    const val REASON_BASIC = "mode-basic"

    /** مفتاح «لا امتياز» في وضع [ExecutionMode.AUTO] — هو نفسه `PrivilegedShell.REASON_NO_PRIVILEGE`. */
    const val REASON_NO_PRIVILEGE = "no-privilege"

    /** المسار الذي ينفَّذ به الأمر في هذا الوضع، أو `null` = رفض. لا ترقية ولا تنازل صامت بين الأوضاع. */
    fun lane(mode: ExecutionMode, rootGranted: Boolean, shizukuReady: Boolean): PrivilegedShellRules.Lane? =
        when (mode) {
            ExecutionMode.AUTO -> PrivilegedShellRules.laneFor(rootGranted, shizukuReady)
            ExecutionMode.ROOT -> if (rootGranted) PrivilegedShellRules.Lane.ROOT else null
            ExecutionMode.SHIZUKU -> if (shizukuReady) PrivilegedShellRules.Lane.SHIZUKU else null
            ExecutionMode.BASIC -> null
        }

    /** الامتياز الفعلي في هذا الوضع: الأقل بين المختار والمتاح. */
    fun level(mode: ExecutionMode, rootGranted: Boolean, shizukuReady: Boolean): PrivilegeLevel =
        when (lane(mode, rootGranted, shizukuReady)) {
            PrivilegedShellRules.Lane.ROOT -> PrivilegeLevel.ROOT
            PrivilegedShellRules.Lane.SHIZUKU -> PrivilegeLevel.SHIZUKU
            null -> PrivilegeLevel.NONE
        }

    /** سبب الرفض حين لا يوجد مسار — يميّز «اخترتَ وضعًا غير متاح» من «لا امتياز أصلًا». */
    fun refusalReason(mode: ExecutionMode): String = when (mode) {
        ExecutionMode.AUTO -> REASON_NO_PRIVILEGE
        ExecutionMode.ROOT -> REASON_ROOT_UNAVAILABLE
        ExecutionMode.SHIZUKU -> REASON_SHIZUKU_UNAVAILABLE
        ExecutionMode.BASIC -> REASON_BASIC
    }

    /** هل يجوز لهذا الوضع أن يُسخِّن صدفة `su` عند الإقلاع؟ [ExecutionMode.SHIZUKU] و[ExecutionMode.BASIC] لا. */
    fun mayUseRoot(mode: ExecutionMode): Boolean = mode == ExecutionMode.AUTO || mode == ExecutionMode.ROOT
}
