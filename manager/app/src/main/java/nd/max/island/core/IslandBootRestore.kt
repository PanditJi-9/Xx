/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.island.core

/**
 * قرار استعادة الجزيرة بعد الإقلاع — **دالّة نقيّة تُقاس على JVM**، والتنفيذ في
 * `PersistRestorer`.
 *
 * ### العطب الذي وُلدت منه
 *
 * كان `PersistRestorer` يعيد **المحرّك القديم** (`nd.max.service.IslandService`) من مخزنه
 * القديم (`IslandPrefs`)، والمحرّك الحاليّ لا يُقلعه أحد بعد الإقلاع. فنتيجتان مختلفتان بلا
 * أن يعرف المستخدم لماذا:
 *
 * 1. من لم يفتح شاشة الجزيرة بعد الترقية كانت تعود له **الجزيرة القديمة** وحدها.
 * 2. ومن فتحها ومنح «الرسم فوق التطبيقات» وحده — بلا خدمة وصولية — **لا تعود له الجزيرة
 *    أصلًا**؛ لأن خدمة الوصولية وحدها هي ما يعيد النظام ربطه بعد الإقلاع.
 *
 * ### والحدّ
 *
 * القرار هنا لا ينفّذ ولا يقرأ جهازًا: من يقيس (الوصولية، الصلاحية، الخدمة) يمرّر ما قاسه،
 * والدالّة ترتّب الأولويّة. والترتيب مقصود: الوصولية أولًا لأنها تستضيف النافذة فعلًا بعد
 * الإقلاع، ثم الخدمة الحيّة، ثم مسار «الرسم فوق التطبيقات» وحده.
 */
object IslandBootRestore {

    /** ما يفعله باب الاستعادة بالجزيرة. */
    enum class Step {
        /** لا نيّة للمستخدم — لا يُقلع شيء ولا تُفتح إعدادات من بثّ إقلاع. */
        SKIP_DISABLED,

        /** خدمة الوصولية تستضيف النافذة الآن: لا خدمة أمامية ثانية تُضاف. */
        ACCESSIBILITY_HOSTED,

        /** خدمة المقدّمة تعمل أصلًا. */
        ALREADY_RUNNING,

        /** تُقلع خدمة المقدّمة — مسار «الرسم فوق التطبيقات» هو المتاح. */
        START_SERVICE,

        /**
         * الوصولية ممنوحة لكن النظام لم يربط خدمتها بعد، ولا صلاحية رسم — فلا يُدّعى أنها
         * عادت: `Pending` بسبب مقيس، لا `Applied` بلا دليل.
         */
        WAIT_FOR_ACCESSIBILITY,

        /** نيّة موجودة ولا صلاحية رسم ولا وصولية — تُعلن الحاجة بدل ادّعاء النجاح. */
        SKIP_NO_PERMISSION,
    }

    fun decide(
        enabled: Boolean,
        accessibilityConnected: Boolean,
        accessibilityEnabled: Boolean,
        overlayGranted: Boolean,
        serviceRunning: Boolean,
    ): Step = when {
        // نيّة المستخدم تسبق كل شيء: خدمةٌ باقية أو وصولية مستضافة لا تُبقي جزيرةً أطفأها.
        !enabled -> Step.SKIP_DISABLED
        accessibilityConnected -> Step.ACCESSIBILITY_HOSTED
        serviceRunning -> Step.ALREADY_RUNNING
        overlayGranted -> Step.START_SERVICE
        accessibilityEnabled -> Step.WAIT_FOR_ACCESSIBILITY
        else -> Step.SKIP_NO_PERMISSION
    }

    /**
     * نقل نيّة المحرّك القديم إلى الحاليّ — **مرّة واحدة، في الإقلاع كما في الشاشة**.
     *
     * والقاعدة: نيّة قديمة قائمة **تُشعل الجديد** (ولو كان مطفأً: المستخدم أراد جزيرة)،
     * ونيّة غير قائمة **لا تمسّ الجديد** — فمن أطفأ الجديد لا يُشعله غيابُ نيّة قديمة.
     */
    data class IntentHandover(
        /** تُكتب `IslandPrefs.enabled = false` بعد النقل، فلا يعود المحرّك القديم أبدًا. */
        val clearLegacy: Boolean,
        /** تُكتب `IslandSettings.enabled = true` — النيّة المنقولة نفسها. */
        val enableCurrent: Boolean,
    )

    fun handover(legacyActive: Boolean, currentEnabled: Boolean): IntentHandover =
        if (legacyActive) {
            IntentHandover(clearLegacy = true, enableCurrent = true)
        } else {
            IntentHandover(clearLegacy = false, enableCurrent = currentEnabled)
        }
}
