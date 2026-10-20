/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/**
 * **عدّادات صحّة التحكّم** — نموذج نقيّ يُختبر على JVM (`MAX-CONTROL-01` §١٠.١ حقل `health`).
 *
 * ليست قيمًا ولا قراءات ولا تقارير — بل **أعداد مصنَّفة** لكل مجال: كم مرة انتهت كتابةٌ
 * بحالة «نُفِّذ وأُثبت» (confirmed) · «أُعيد خط الأساس» (rolled_back) · «بقي الحكم مجهولًا»
 * (unknown). وهي وحدها ما يغادر الجهاز في الإشارة اليومية — بعد موافقة صريحة — فتُجمَّع في
 * لوحة الصحّة (§٦.٣: «أعداد لا قيم»).
 *
 * **ومن أين تأتي الأعداد:** من [HardwareControlArbiter] وحده — الممرّ الذي تمرّ منه كل كتابة
 * تحكّم ([StatsRules]-style: مصدر واحد، لا عدّ في الشاشات). وما لا يمرّ منه (قراءة عقدة أو
 * تشخيص) لا يُعدّ أصلًا، فلا يختلط «قرأنا» بـ«كتبنا».
 *
 * **ولماذا التصنيف ثلاثيّ لا خماسيّ:** [WriteVerification.Outcome] خمس حالات للحكم التقنيّ
 * (`MATCHED` · `DIFFERS` · `WRITE_FAILED` · `READBACK_UNAVAILABLE` · `WRITE_ONLY_SENT`)،
 * وواجهة المجتمع تحتاج ثلاثة أسئلة فقط: أثبتَ الوصول · ارتدّ · ما نعرف. فالخمس تُطوى إلى
 * الثلاث في [classify] بقاعدة واحدة معلنة، ويبقى الخمس كاملًا في سجلّ الجهاز نفسه.
 */
object HealthModel {

    /** تصنيف النتيجة — ثلاثٌ لا أكثر، وهي عقد §١٠.١ حرفيًّا. */
    enum class Outcome {
        /** كُتبت وثبت الوصول إلى القيمة المطلوبة. */
        CONFIRMED,

        /** كُتبت ثم أُعيد خط الأساس (تقصير/انحراف/تحرير بملكية). */
        ROLLED_BACK,

        /** جرى التنفيذ وبقي الحكم مجهولًا: قراءة متعذّرة أو قيمة اختلفت أو فشل كتابة. */
        UNKNOWN,
    }

    /** أعداد مجال واحد — والافتراضيّ صفر، فمجالٌ بلا صفّ ليس مجهولًا. */
    data class Counters(
        val confirmed: Long = 0,
        val rolledBack: Long = 0,
        val unknown: Long = 0,
    ) {
        /**
         * مجموع الأعداد — **مُشبَع عند `Long.MAX_VALUE`** لا ملتفًّا.
         *
         * ولماذا: هذا المجموع جواب سؤال «هل لهذا المجال عددٌ فعلًا؟» في [encode] والتخزين.
         * وجمعٌ يلتفّ سالبًا على أعدادٍ شاذّة يجعل الجواب «لا» فيُسقط المجال بصمت — وهذا
         * ما كشفه الاختبار (‏`Long.MAX_VALUE` + عدد موجب ⇒ سالب). والأعداد الحقيقية لا تبلغ
         * السقف أصلًا، لكن الحرس يجب أن يصحّ عند الحدّ لا أن يعتمد على استحالة بلوغه.
         */
        val total: Long
            get() {
                val a = confirmed.coerceAtLeast(0)
                val b = rolledBack.coerceAtLeast(0)
                val c = unknown.coerceAtLeast(0)
                val ab = if (a > Long.MAX_VALUE - b) Long.MAX_VALUE else a + b
                return if (c > Long.MAX_VALUE - ab) Long.MAX_VALUE else ab + c
            }

        /** عدّ نتيجةً واحدة في هذا المجال. */
        fun plus(outcome: Outcome): Counters = when (outcome) {
            Outcome.CONFIRMED -> copy(confirmed = confirmed + 1)
            Outcome.ROLLED_BACK -> copy(rolledBack = rolledBack + 1)
            Outcome.UNKNOWN -> copy(unknown = unknown + 1)
        }

        /** جمع عدّاداتين — يُستعمل عند دمج ذاكرة القرص مع ما عُدّ قبل [HealthLedger.attach]. */
        fun merge(other: Counters): Counters = Counters(
            confirmed = confirmed + other.confirmed,
            rolledBack = rolledBack + other.rolledBack,
            unknown = unknown + other.unknown,
        )
    }

    /**
     * المجالات المسموح بها في الرسالة — مرتّبة أبجديًّا (والترميز [encode] يضمن الترتيب نفسه).
     *
     * والقائمة نفسها مكتوبة في قيد الخادم (`installs_health_shape` والمُنقّي داخل
     * `track_install`): ما لا يُذكر هنا لا يعبر السلك، حتى لو أضاف كاتبٌ جديد مفتاحًا.
     */
    val DOMAINS: List<String> = listOf(
        "audio", "charging", "cpu", "display", "gpu", "memory", "other", "spoof", "storage", "thermal", "touch",
    )

    /**
     * سقف العدّاد — يمنع أي رقم شاذّ من مغادرة الجهاز. والخادم يشترط عددًا صحيحًا من ٩ خانات
     * أو أقل، فالسقفان متطابقان عن قصد لا صدفة.
     */
    const val MAX_COUNT: Long = 999_999_999L

    /**
     * المجال من مفتاح مقبض — والمفاتيح تأتي من `HardwareControlKey` وحدها
     * (`cpu_limits:` · `gpu_frequency:` · `mem_frequency:` · `ufs_frequency:` · `audio_*:` ·
     * `spoof_engine:` …)، وما لا يُعرف يُنسب إلى `other` بدل أن يُسقط أو يُخترع له مجال.
     *
     * والفحص بالبادئة المطبَّعة (حروف صغيرة) كي لا يصنع مفتاحٌ بأحرف كبيرة مجالًا ثانيًا.
     */
    fun domainOf(key: String): String {
        val lower = key.trim().lowercase()
        return when {
            lower.startsWith("cpu") -> "cpu"
            lower.startsWith("gpu") -> "gpu"
            lower.startsWith("mem") -> "memory"
            lower.startsWith("ufs") || lower.startsWith("storage") -> "storage"
            lower.startsWith("audio") -> "audio"
            lower.startsWith("thermal") -> "thermal"
            lower.startsWith("display") -> "display"
            lower.startsWith("touch") -> "touch"
            lower.startsWith("charg") -> "charging"
            lower.startsWith("spoof") -> "spoof"
            else -> "other"
        }
    }

    /**
     * التصنيف من حقول نتيجة الـarbiter — **ترتيب الفحص مقصود**: التراجع حكم قاطع يسبق
     * كل شيء (وإلا صُنّفت استعادةٌ ناجحة «تنفيذًا مؤكَّدًا» وهي إلغاء لا تنفيذ).
     *
     * وإجابةٌ واحدة عن `applied && verified`: هي شرط «التنفيذ المؤكَّد» — القراءة البعدية
     * أثبتت أن القيمة المطلوبة وصلت. وما عداها — ولو نجح أمر الكتابة نفسه — يبقى «مجهولًا»
     * بصدق ADR-07: «لا نقول نجحنا ولا نقول فشلنا».
     */
    fun classify(applied: Boolean, verified: Boolean, rollbackAttempted: Boolean): Outcome = when {
        rollbackAttempted -> Outcome.ROLLED_BACK
        applied && verified -> Outcome.CONFIRMED
        else -> Outcome.UNKNOWN
    }

    /**
     * ترميز الرسالة **حتميًّا**: مفاتيح مرتّبة أبجديًّا، والمجالات الفارغة تُسقط، وكل عدّاد
     * يُقصّ عند [MAX_COUNT]، ولا مفاتيح خارج [DOMAINS].
     *
     * والحتمية ليست ترفًا: نفس الأعداد ⇒ نفس البايتات بالضبط، وهي شرط مقارنة «هل تغيّر شيء»
     * في `StatsRules.due` (وقارئ الشاشة يعرض هذه الدالّة نفسها لا وصفًا لها).
     */
    fun encode(counts: Map<String, HealthModel.Counters>): String {
        val rows = sendableRows(counts)
        if (rows.isEmpty()) return "{}"
        return buildString {
            append('{')
            rows.forEachIndexed { index, (domain, counters) ->
                if (index > 0) append(',')
                append('"').append(domain).append("\":{")
                append("\"confirmed\":").append(counters.confirmed.coerceIn(0, MAX_COUNT)).append(',')
                append("\"rolled_back\":").append(counters.rolledBack.coerceIn(0, MAX_COUNT)).append(',')
                append("\"unknown\":").append(counters.unknown.coerceIn(0, MAX_COUNT))
                append('}')
            }
            append('}')
        }
    }

    /** الصفوف التي لها أعداد فعلية، مرتّبة — ولا يُرسل مجالٌ أصفاره كلها. */
    private fun sendableRows(counts: Map<String, Counters>): List<Pair<String, Counters>> =
        counts.entries
            .filter { it.key in DOMAINS && it.value.total > 0 }
            .sortedBy { it.key }
            .map { it.key to it.value }
}
