/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.privilege

/**
 * قاعدة اختيار مسار التنفيذ (`AR-20`) — **نقيّة فتُاختبر بلا جهاز**، ومفصولة عن الملفّ الذي
 * يستدعي libsu/Shizuku حتى يبقى الحكم نفسه قابلًا للقياس وحده.
 */
object PrivilegedShellRules {

    /** الطبقة التي نُفِّذ بها الأمر فعلًا — تُعلَن للمستخدم؛ جذر أو Shizuku، ولا تُنسب إلى غيرهما. */
    enum class Lane { ROOT, SHIZUKU }

    /**
     * الجذر أولًا لأنه الأعلى والأوسع أثرًا، ثم Shizuku. و`null` = لا طبقة.
     *
     * **ولماذا `null` لا «تنفيذ بصلاحية التطبيق»:** صدفة libsu بلا جذر تعمل بصلاحية التطبيق
     * العادية، فيرجع الأمر «permission denied» أو يبدو كأنه نُفِّذ — وكلاهما أسوأ من رفض صريح.
     * هذه القاعدة هي التي تمنع ذلك بدل أن تتركه لعرفٍ في كل موضع استدعاء.
     */
    fun laneFor(rootGranted: Boolean, shizukuReady: Boolean): Lane? = when {
        rootGranted -> Lane.ROOT
        shizukuReady -> Lane.SHIZUKU
        else -> null
    }
}
