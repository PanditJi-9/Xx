/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * ذاكرة قياسات جلسة العناية بالتخزين — **عابرة في الذاكرة، لا تُكتب على القرص** (`STORAGE-CARE-01`).
 *
 * السبب مقيس: قياس فئات التنظيف يمرّ على شجرة التخزين كاملة (سرد + `du`)، وإعادته في كل
 * فتح للشاشة تجعل الفتح الثاني بطيئًا بلا سبب — لا شيء تغيّر بين الفتحين. فالنتيجة تُحفظ
 * هنا بعمر محدود، وتُسقط عند ثلاثة أحداث: مرور المدة، أو تغيّر طبقة الجذر (يغيّر الفئات
 * المرئية أصلًا)، أو تنظيفٍ فعليّ (المحرّك يعرف أنه غيّر الملفات).
 *
 * **ولا يُكتب شيء على القرص عمدًا:** القياسات أرقام لحظية تتقادم بسرعة (تطبيق يفرّغ كاشه،
 * ملفّ يُحمَّل…)، وملفّ يحمل «12GB» من جلسة الأمس يعرض على المستخدم رقمًا غير صحيح بثقة.
 * والعمر القصير يجعل الذاكرة **تحسينًا** لا مصدر حقيقة: ما يظهر على الشاشة يبقى ما قيس.
 */
package nd.max.ui.util

object StorageCareCache {

    /** عمر القياس المسموح: عشر دقائق — أطول منه احتمال التقادم يغلب مكسب السرعة. */
    const val TTL_MS = 10 * 60 * 1000L

    data class Entry(
        val atMs: Long,
        /** طبقة الجذر لحظة القياس — تغيّرها يغيّر المجموعة المرئية، فالقيم لم تعد لنفس السؤال. */
        val rootAvailable: Boolean,
        val measurements: CleanMeasurementSet,
    )

    @Volatile
    private var entry: Entry? = null

    fun load(): Entry? = entry

    fun store(fresh: Entry) {
        entry = fresh
    }

    fun invalidate() {
        entry = null
    }

    /**
     * هل القياس ما يزال صالحًا للعرض؟
     *
     * والعمر **في المدى `0..ttl`** لا `<=`: عمر سالب يعني ساعة الجهاز تراجعت، فالوثوق
     * برقم «من المستقبل» أسوأ من إعادة القياس. والحدّ الأعلى هو التقادم المعلَن.
     */
    fun isFresh(nowMs: Long, entry: Entry, ttlMs: Long = TTL_MS): Boolean {
        val age = nowMs - entry.atMs
        return age in 0..ttlMs
    }
}
