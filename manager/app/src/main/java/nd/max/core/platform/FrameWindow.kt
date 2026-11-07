/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.platform

import kotlin.math.ceil

/**
 * نافذة إطارات متدحرجة: تحوّل قراءةً لحظية إلى **حكم على مدى**.
 *
 * **ولماذا وُجدت:** `FpsMonitorUtil` يعطي إطارات اللحظة وحدها. ورقمٌ لحظيّ لا يجيب السؤال الذي
 * يُشترى من أجله تراكب الأداء: «هل اللعبة ثابتة؟» — لأن الثانية الواحدة قد تحمل ١٢٠ إطارًا وثلاث
 * هبّات في الآن نفسه، واللحظة التي تقرأ فيها قد تكون إحداهما أو بينهما.
 *
 * **وحدودها الصادقة:**
 * 1. ليست مقياس زمن إطار حقيقيًّا: المصدر معدّل إطارات مُعايَن، لا `FrameMetrics`. فالنسبة هنا
 *    «حصة العيّنات القريبة من هدف الشاشة»، لا «نسبة الإطارات المتقطّعة» بالمعنى الذي يقيسه
 *    `dumpsys gfxinfo`.
 * 2. لا تخترع قياسًا: عيّنة غائبة أو غير منتهية أو غير موجبة **لا تُسجَّل**، فنافذة فارغة تعطي
 *    `null` لا صفرًا.
 * 3. والهدف من الشاشة لا من التطبيق: يُقرأ `Display.refreshRate` مرّة عند البناء؛ وحين يبدّله
 *    النظام (‏60↔120) تتغيّر النسبة، وهذا انعكاس صادق لا عطب.
 */
class FrameWindow(private val capacity: Int = DEFAULT_CAPACITY) {

    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    private val values = ArrayDeque<Float>(capacity)

    /** عدد العيّنات الحيّة الآن (≤ السعة). */
    val samples: Int
        @Synchronized get() = values.size

    /** يُسجّل عيّنة. الغائب/غير المنتهي/غير الموجب يُهمَل بلا أثر. */
    @Synchronized
    fun record(fps: Float?) {
        if (fps == null || !fps.isFinite() || fps <= 0f) return
        if (values.size == capacity) values.removeFirst()
        values.addLast(fps)
    }

    @Synchronized
    fun reset() = values.clear()

    /** المتوسّط — يُقرأ مع الوسيط لا بدلًا منه: متوسط عالٍ مع وسيط منخفض يعني هبّات. */
    @Synchronized
    fun mean(): Float? = if (values.isEmpty()) null else values.average().toFloat()

    /**
     * المئين `p` (٠..١) بطريقة الرتبة الأقرب: `p=0.5` وسيط، `p=0.99` أسوأ ١٪ تقريبًا.
     *
     * والرتبة تُقرَّب لأعلى كي لا يعود المئين ٩٩ هو الأقصى حين تكون النافذة صغيرة — وهو الخطأ
     * الذي يجعل «٩٩٪ من الوقت» كذبًا في أول عشر عيّنات.
     */
    @Synchronized
    fun percentile(p: Double): Float? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val clamped = p.coerceIn(0.0, 1.0)
        val rank = ceil(clamped * sorted.size).toInt().coerceIn(1, sorted.size)
        return sorted[rank - 1]
    }

    /**
     * حصة العيّنات التي بلغت `floor × target` — رقم ٠..١، أو `null` بلا عيّنات.
     *
     * `floor = 0.9` مقصود: إطار على ٩٠٪ من هدف الشاشة يُعدّ ثابتًا عمليًّا، وأي عتبة أعلى تُظهر
     * كل جهاز «غير ثابت» لأن النظام نفسه يخفض الإطارات عند سكون المشهد.
     */
    @Synchronized
    fun stableShare(target: Float, floor: Float = STABLE_FLOOR): Float? {
        if (values.isEmpty() || !target.isFinite() || target <= 0f) return null
        val threshold = target * floor
        return values.count { it >= threshold }.toFloat() / values.size
    }

    companion object {
        /** ١٢٠ عيّنة: دقيقتان بنبضة ثانية، أو ثانيتان بنبضة ٦٠ — مدى يكفي للحكم بلا ذاكرة تُذكر. */
        const val DEFAULT_CAPACITY = 120

        /** عتبة «ثابت» كما تُقاس في تراكبات الأداء: ٩٠٪ من هدف الشاشة. */
        const val STABLE_FLOOR = 0.9f
    }
}
