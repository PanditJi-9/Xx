/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

/**
 * حارس التدخل — يمنع المحرك من أن يصير هو نفسه مصدر الاهتزاز.
 *
 * **العطب الذي وُلد منه (مقيس في سجل جهاز rodin، ٢٠٢٦-١٠-١٣/١٤):**
 *
 * ```text
 * 00:19:42  cpu_limits:policy0  300000:1200000   (خفض)
 * 00:20:31  cpu_limits:policy0  300000:1300000   (رفع)
 * 00:21:20  cpu_limits:policy0  300000:1200000   (خفض)
 * 00:21:30  cpu_limits:policy0  300000:1100000   (خفض)
 * 00:22:14  cpu_limits:policy0  300000:1000000   (خفض)
 * 00:23:05  cpu_limits:policy0  300000:1200000   (رفع)
 * ```
 *
 * أي سقفٌ يتأرجح خطوةً كل ٤٠–٥٠ ثانية، **٣٠١ كتابة مُتحقَّقة في ١٥ ساعة**، وفي موضع آخر
 * نزل `policy0` إلى `300000:500000` (٥٠٠ ميجاهرتز) — جهاز بطيء بلا مكسب يُثبته القياس.
 * والسبب بنيوي: الاتجاه دالّة لحظية في `cpuLoad` (`Objective.preferredDirection`)، فحملٌ
 * يعبر ٧٠٪ صعودًا وهبوطًا يقلب الاتجاه، وكل قلب كتابةٌ على النواة. والقياس نفسه لا يحكم
 * على الخطوة السابقة إلا بعد عشر ثوانٍ — أقصر من أن يميّز أثرها من ضجيج الحمل.
 *
 * فالحارسان هنا **حدّان على معدّل الكتابة**، لا سلطة تحكم جديدة:
 *
 * 1. **ثبات الاتجاه:** بعد خطوةٍ في اتجاه، لا يُقبل عكسها قبل [DIRECTION_HOLD_MS]. فالخطوة
 *    تعيش أطول من نافذة القياس ومن دورة القرار، فلا تُلغى قبل أن تُقاس.
 * 2. **ميزانية الخطوات:** لا أكثر من [MAX_STEPS_PER_WINDOW] خطوات على المقبض نفسه في
 *    [STEP_WINDOW_MS]. وهذا سقفُ معدّل لا سقفُ قدرة: يسمح للمحرك أن يتعلّم ويصحّح، ويمنع
 *    المسير الأعمى إلى أرضية السلّم حين يكون الهدف غير قابل للبلوغ (وضعه الترشيد قبل
 *    `Objective.BATTERY_COMFORT_FRACTION`).
 *
 * والأمان لا يمرّ من هنا: `SafetyEngine` يكتب عبر `CpuCeilingKnobs` مباشرة، فحماية الحرارة
 * تبقى بلا حدّ ولا تهدئة.
 *
 * والخيوط: `check`/`record` تُستدعى من خيط دورة القرار الواحد، لكن الحالة محميّة بـ
 * `@Synchronized` لأن أي مستدعٍ ثانٍ (تشخيص/اختبار) لا يجوز أن يراها نصف مكتوبة.
 */
class InterventionGuard {

    /** خطوة سابقة على مقبض: اتجاهها وزمنها. */
    private data class Record(val direction: ControlRegistry.Direction, val atMs: Long)

    private val lastStep = HashMap<String, Record>()
    private val stepTimes = HashMap<String, ArrayDeque<Long>>()

    /** حكم الحارس على خطوة مخطَّطة — لا يكتب شيئًا ولا يستهلك ميزانية. */
    data class Verdict(
        val blocked: Boolean,
        val reason: String?,
        /** كم بقي قبل أن يُسمح بالمحاولة، أو 0 حين لا حجب. */
        val retryInMs: Long,
    ) {
        companion object {
            val ALLOWED = Verdict(blocked = false, reason = null, retryInMs = 0L)
        }
    }

    /** أسباب الحجب — ثوابت تُعرض في الواجهة والتشخيص بلا ترجمة تخمينية. */
    object Reason {
        /** عكس اتجاه خطوة سابقة قبل انقضاء مدّتها. */
        const val DIRECTION_HOLD = "direction_hold"

        /** استُهلكت ميزانية الخطوات في النافذة. */
        const val STEP_BUDGET = "step_budget"
    }

    /**
     * هل تُقبل هذه الخطوة الآن؟
     *
     * @param nowMs ساعة أحادية المصدر (زمن الحائط) — تُمرَّر كي تُقاس الدالة بلا انتظار.
     */
    @Synchronized
    fun check(
        key: String,
        direction: ControlRegistry.Direction,
        nowMs: Long,
    ): Verdict {
        val previous = lastStep[key]
        if (previous != null && previous.direction != direction) {
            val age = nowMs - previous.atMs
            if (age in 0 until DIRECTION_HOLD_MS) {
                return Verdict(
                    blocked = true,
                    reason = Reason.DIRECTION_HOLD,
                    retryInMs = DIRECTION_HOLD_MS - age,
                )
            }
        }
        val times = stepTimes[key] ?: return Verdict.ALLOWED
        prune(times, nowMs)
        if (times.size >= MAX_STEPS_PER_WINDOW) {
            val oldest = times.first()
            return Verdict(
                blocked = true,
                reason = Reason.STEP_BUDGET,
                retryInMs = (oldest + STEP_WINDOW_MS - nowMs).coerceAtLeast(0L),
            )
        }
        return Verdict.ALLOWED
    }

    /**
     * يُسجَّل بعد **تنفيذ** الخطوة (والتحقق منها)، لا عند تخطيطها: الميزانية تُحصي كتابات
     * حقيقية على العتاد، لا نوايا.
     */
    @Synchronized
    fun record(key: String, direction: ControlRegistry.Direction, nowMs: Long) {
        lastStep[key] = Record(direction, nowMs)
        val times = stepTimes.getOrPut(key) { ArrayDeque() }
        times.addLast(nowMs)
        prune(times, nowMs)
    }

    /** يُصفَّر عند إطفاء Max AI: الميزانية تخصّ جلسة تشغيل لا عمر العملية. */
    @Synchronized
    fun reset() {
        lastStep.clear()
        stepTimes.clear()
    }

    private fun prune(times: ArrayDeque<Long>, nowMs: Long) {
        while (times.isNotEmpty() && nowMs - times.first() > STEP_WINDOW_MS) {
            times.removeFirst()
        }
    }

    companion object {
        /**
         * خمس دقائق: أطول من نافذة الاستجابة (`RESPONSE_WINDOW_MS = 10s`) ومن دورة القرار
         * (٣٠ ثانية) ومن الحكم المؤجّل الأول، فتُقاس الخطوة قبل أن تُعكس.
         */
        const val DIRECTION_HOLD_MS = 300_000L

        /** نافذة الميزانية: نصف ساعة — أفق الحكم المؤجّل نفسه (`DEFERRED_WINDOW_MS`). */
        const val STEP_WINDOW_MS = 1_800_000L

        /**
         * ثلاث خطوات لكل مقبض في النافذة.
         *
         * ثلاثة لأن التعلّم يحتاج تكرارًا ليثبت الأثر، والرابعة في الاتجاه نفسه بلا بلوغ
         * الهدف ليست تعلّمًا بل مسيرًا. والمقيس كان ٢٥ كتابة في الساعة على المقبض الواحد.
         */
        const val MAX_STEPS_PER_WINDOW = 3
    }
}
