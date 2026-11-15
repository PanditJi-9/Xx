/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/**
 * توزيع وقت المعالج على تردداته — **حساب خالص** من جدول النواة إلى حصص.
 *
 * ولا يقرأ ملفًا ولا يعرف مسارًا: القراءة في [CpuHardwareBackend.timeInState]، والحساب هنا،
 * فيمكن قياسه على مصفوفة ثابتة بلا جهاز. والفصل مقصود لأن أصعب سؤال في هذا الشكل ليس الرسم:
 * هل «٣٠٪ عند ١٫٨ جيجاهرتز» حقيقة، أو رقم مبني على سقف مفترض؟ هنا يُبنى على مجموع العدادات
 * المُعلَن وحده.
 */
object CpuStateDistribution {

    /** حصة تردد واحد: كسور من ٠ إلى ١، لا نسبة مئوية مُقرَّبة. */
    data class Share(val frequencyKHz: Long, val share: Float)

    /**
     * الحالات الأدنى من هذه الحصة تُعدّ ضجيجًا **ولا تُرسم** — والعدد والباقي يُعلنان.
     *
     * والسبب من الشاشة لا من الحساب: جهاز بعشرين حالة يعرض صفوفًا بعشرين سطرًا، أغلبها ٠٫١٪،
     * فيصير الجدول أطول من الشاشة وأقلّ دلالة. والحدّ ١٪ معلن في الواجهة، فما أخفي يُقال عدده.
     */
    const val NOISE_SHARE: Float = 0.01f

    /** الحصص مرتّبة من أعلى تردد إلى أدناه؛ `null` حين لا عدّاد موجب أصلًا. */
    fun shares(states: List<CpuHardwareBackend.StateTime>): List<Share>? =
        sharesOf(states.map { it.frequencyKHz to it.ticks })

    /**
     * الحساب نفسه على أزواج (تردد، عدّاد) — لأن جدول الـGPU يُقرأ بوحدة تردد مختلفة عن kHz،
     * ولأن هذا الكائن هو **موضع واحد** لقاعدة الحصص: إعادة كتابتها لجهة ثانية تُنتج رقمين
     * مختلفين للشيء نفسه، وهو عطب ازدواج المصدر (ADR-11/ADR-18).
     */
    fun sharesOf(counts: List<Pair<Long, Long>>): List<Share>? {
        // صفٌّ بعدّاده صفر ليس حالة سكنها المعالج: النواة تنشر جدول OPP كاملًا وقد لا تُستخدم
        // بعض الترددات أبدًا. إدخاله يجعل «٠٪ مخفيّة» صفوفًا وهمية في العدّ، ويوهم بأن الخفاء
        // أكبر مما هو — فيُسقَط قبل الجمع لا بعد القسمة.
        val used = counts.filter { it.second > 0L }
        val total = used.sumOf { it.second }
        if (total <= 0L) return null
        // النواة قد تكرّر التردد (سطران لنفس القيمة على بعض الإصدارات): يُجمع قبل القسمة،
        // وإلا ظهرت حالتان متطابقتان بلا معنى.
        return used
            .groupBy { it.first }
            .map { (khz, rows) -> Share(khz, rows.sumOf { it.second }.toFloat() / total) }
            .sortedByDescending { it.frequencyKHz }
    }

    /** ما يُعرض وما يُخفى — مفصولين ليقول السطر الأخير الحقيقة كاملة بدل أن يخفيها. */
    data class Split(val visible: List<Share>, val hiddenCount: Int, val hiddenShare: Float)

    fun split(shares: List<Share>, threshold: Float = NOISE_SHARE): Split {
        val visible = shares.filter { it.share >= threshold }
        val hidden = shares.filter { it.share < threshold }
        return Split(
            visible = visible,
            hiddenCount = hidden.size,
            hiddenShare = hidden.sumOf { it.share.toDouble() }.toFloat(),
        )
    }

    /** أعلى حصة — سطر الملخّص يقوله بدل أن يترك المستخدم يقارن الأشرطة بعينه. */
    fun dominant(shares: List<Share>): Share? = shares.maxByOrNull { it.share }
}
