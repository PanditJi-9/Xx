/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

import android.content.Context
import nd.max.R

/**
 * ما يراه المستخدم في الإشعارات عن Max AI: **الهدف الفاعل الآن** لا آخر ملف عام طُبّق.
 *
 * العطب الذي وُلدت منه: بطاقة «Balanced Profile» ثابتة لأن الخادم لا يرسلها إلا عند تطبيق ملف
 * عام (١/٢/٣)، وMax AI لا يطبّق ملفات أصلًا — يحرّك مقابض بأوزان هدف (`Objective`). فكان المحرك
 * يعمل ويتغيّر هدفه، والبطاقة تقول «متوازن» أبدًا، فيُظنّ أنه معطّل. والإصلاح أن يُنشر الهدف
 * الفاعل من المحرك (الكاتب الوحيد) وتقرأه كل الإشعارات من المصدر نفسه.
 *
 * القسم النقيّ (الترميز والحلّ) بلا أندرويد حتى يُختبر على JVM؛ ونص السطر وحده يحتاج `Context`.
 */
data class ActiveObjectiveNotice(val label: String, val source: String) {

    companion object {
        private const val SEPARATOR = '|'

        /** `label|source` — ما يكتبه المحرك في [nd.max.MaxManagerProps.AiRuntime.ACTIVE_OBJECTIVE]. */
        fun encode(label: String, source: String): String = "$label$SEPARATOR$source"

        /** `null` للفارغ أو المشوَّه: «لا إعلان» لا «هدف متوازن مخمَّن». */
        fun decode(raw: String?): ActiveObjectiveNotice? {
            val text = raw?.trim().orEmpty()
            if (text.isEmpty()) return null
            val label = text.substringBefore(SEPARATOR).trim()
            if (label !in KNOWN_LABELS) return null
            val source = text.substringAfter(SEPARATOR, "").trim().ifEmpty { "user" }
            return ActiveObjectiveNotice(label, source)
        }

        /**
         * الحلّ بترتيب الصدق:
         *  1. Max AI مطفأ ⇒ `null` (لا سطر «يعمل»).
         *  2. هدف منشور من المحرك ⇒ هو الفاعل.
         *  3. وإلا (المحرك لم يدُر بعد) ⇒ تفضيل المستخدم المحفوظ، وإن غاب فمتوازن.
         */
        fun resolve(aiEnabledRaw: String?, activeRaw: String?, preferenceRaw: String?): ActiveObjectiveNotice? {
            if (aiEnabledRaw != "1") return null
            decode(activeRaw)?.let { return it }
            val preference = preferenceRaw?.trim().orEmpty().takeIf { it in KNOWN_LABELS } ?: "balanced"
            return ActiveObjectiveNotice(preference, "user")
        }

        private val KNOWN_LABELS = setOf("performance", "balanced", "battery")

        /** سطر الإشعار الجاهز، أو `null` إذا كان Max AI مطفأً. */
        fun line(context: Context, notice: ActiveObjectiveNotice?): String? {
            notice ?: return null
            val label = when (notice.label) {
                "performance" -> R.string.max_ai_objective_pick_performance
                "battery" -> R.string.max_ai_objective_pick_battery
                else -> R.string.max_ai_objective_pick_balanced
            }
            return context.getString(R.string.notif_maxai_running, context.getString(label))
        }
    }
}
