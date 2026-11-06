/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.stats

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * تفصيل الاستخدام **يومًا بيوم** من فترات المقدّمة الخام.
 *
 * **ولماذا لا يُسأل `UsageStatsManager` عن كل يوم على حدة:** كل استعلام يعيد تجميعًا
 * داخليًّا قد يقطع يومًا عند حدٍّ مختلف عن منتصف الليل المحلّي، فيصير مجموع الأيام
 * لا يساوي الشهر. هنا **نقطّع نحن** فترةً مقيسةً واحدةً على حدود اليوم المحلّي، فيتحقّق
 * شرط واحد قابل للاختبار: مجموع الأيام = مجموع ما قُطع، لا أكثر ولا أقل.
 *
 * يختزل أحداث المقدّمة إلى فترات غير متداخلة ثم يقطّعها على الأيام. جلب أحداث أندرويد
 * في `DailyUsageReader` وحده؛ يبقى الاختزال والتقطيع قابلين للاختبار بلا جهاز.
 */
object DailyUsage {

    /** فترة مقدّمة واحدة. الفترات التي `endMs <= startMs` تُسقَط ولا تُفسَّر. */
    data class Span(val startMs: Long, val endMs: Long)

    /** مجموع الملّي ثانية في يوم محلي واحد. */
    data class Day(val epochDay: Long, val millis: Long)

    enum class Kind { RESUME, PAUSE, CLOSE_ALL }

    /** هوية النشاط تمنع إيقاف نشاطٍ قديم من إغلاق نشاطٍ جديد في الحزمة نفسها. */
    data class Event(
        val timeMs: Long,
        val kind: Kind,
        val packageName: String = "",
        val activityName: String = ""
    )

    private data class ActivityKey(val packageName: String, val activityName: String)

    /**
     * زمنٌ مرصود لا مجموعٌ لكل التطبيقات: نشاطان متزامنان في تقسيم الشاشة يُحسبان
     * مرة واحدة. التكرار لا يغيّر بداية الفترة، والإيقاف بلا بداية لا يخترع زمنًا.
     * إطفاء الشاشة أو قفلها أو إعادة الإقلاع يغلق الجميع حتى يصل حدث مقدّمة جديد.
     * وما لا تحتفظ به المنصّة من أحداث لا يمكن استعادته هنا.
     */
    fun foregroundSpans(events: List<Event>, nowMs: Long): List<Span> {
        val active = HashSet<ActivityKey>()
        val spans = ArrayList<Span>()
        var openedAt: Long? = null

        fun close(atMs: Long) {
            openedAt?.let { start -> if (atMs > start) spans += Span(start, atMs) }
            openedAt = null
        }

        events.sortedBy { it.timeMs }.forEach { event ->
            if (event.timeMs > nowMs) return@forEach
            val key = ActivityKey(event.packageName, event.activityName)
            when (event.kind) {
                Kind.RESUME -> if (event.packageName.isNotBlank() && active.add(key)) {
                    if (openedAt == null) openedAt = event.timeMs
                }
                Kind.PAUSE -> if (active.remove(key) && active.isEmpty()) close(event.timeMs)
                Kind.CLOSE_ALL -> {
                    close(event.timeMs)
                    active.clear()
                }
            }
        }
        close(nowMs)
        return spans
    }

    /**
     * يوزّع [spans] على أيام النافذة `[fromEpochDay, toEpochDay]` بحسب [zone].
     *
     * - اليوم يبدأ بمنتصف الليل **في [zone]** لا بتوقيت غرينتش؛ فمنطقة تغيّر إزاحتها
     *   (توقيت صيفي) تُقطَّع عند بداية يومها الحقيقية لا عند ظلّ ثابت.
     * - ما وقع قبل النافذة أو بعدها يُقصّ عند حدودها ولا يُحسب يومًا إضافيًّا.
     * - النافذة المعكوسة ([toEpochDay] < [fromEpochDay]) تعود فارغة، لا سالبة.
     * - [spans] غير متداخلة عند مصدرها [foregroundSpans]؛ لا يُجمع التطبيقان المتزامنان.
     */
    fun bucket(
        spans: List<Span>,
        fromEpochDay: Long,
        toEpochDay: Long,
        zone: ZoneId
    ): List<Day> {
        if (toEpochDay < fromEpochDay) return emptyList()
        // القارئ يحتاج أسبوعًا فقط؛ نافذة غير محدودة ليست مدخلًا صالحًا لهذا الرسم.
        require(toEpochDay - fromEpochDay in 0L..365L)
        val count = (toEpochDay - fromEpochDay + 1).toInt()
        val totals = LongArray(count)
        for (span in spans) {
            if (span.endMs <= span.startMs) continue
            val firstDay = dayOf(span.startMs, zone).coerceAtLeast(fromEpochDay)
            // الطرف الأخير حصريّ: فترة تنتهي تمامًا عند منتصف الليل لا تفتح يومًا جديدًا.
            val lastDay = dayOf(span.endMs - 1, zone).coerceAtMost(toEpochDay)
            var day = firstDay
            while (day <= lastDay) {
                val start = startOfDay(day, zone)
                val end = startOfDay(day + 1, zone)
                val overlap = minOf(span.endMs, end) - maxOf(span.startMs, start)
                if (overlap > 0) totals[(day - fromEpochDay).toInt()] += overlap
                day++
            }
        }
        return List(count) { i -> Day(fromEpochDay + i, totals[i]) }
    }

    /** اليوم المحلّي الذي تنتمي إليه اللحظة [ms] في [zone]. */
    internal fun dayOf(ms: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toEpochDay()

    /** أول ملّي ثانية في اليوم — تُقرأ من المنطقة نفسها لا من إزاحة ثابتة. */
    internal fun startOfDay(epochDay: Long, zone: ZoneId): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toInstant().toEpochMilli()

    /**
     * مقارنة السبعة الأخيرة بالسبعة التي قبلها — الرقم الذي كان الرسم لا يقوله.
     *
     * **ولماذا `percentChange` قد تكون `null`:** صفرٌ في الأسبوع السابق ليس أساسًا للمقارنة؛
     * «+∞٪» أو «+١٠٠٪» كلاهما كذب. فنُبلّغ بالمجموعين ونمتنع عن النسبة، والشاشة تقول ذلك بلفظه.
     */
    data class WeekOverWeek(val currentMillis: Long, val previousMillis: Long) {

        /** الفرق المطلق بإشارته؛ سالبٌ يعني أن هذا الأسبوع أقلّ. */
        val deltaMillis: Long get() = currentMillis - previousMillis

        /** النسبة المقرّبة إلى عدد صحيح، أو `null` حين لا أساس للمقارنة. */
        val percentChange: Int?
            get() = if (previousMillis <= 0L) {
                null
            } else {
                ((deltaMillis * 100.0) / previousMillis).roundToInt()
            }
    }

    /**
     * يقارن آخر سبعة أيام بالسبعة السابقة لها، أو `null` إن لم تُقرأ أربعة عشر يومًا.
     *
     * **والترتيب هو العقد:** القائمة مرتّبة تصاعديًّا باليوم، وآخر عنصر هو اليوم؛ فالمقارنة
     * تُقرأ من النهاية لا من البداية. وما قبل الأربعة عشر لا يدخل الحساب أصلًا حتى لا يتغيّر
     * المعنى إن قرأ القارئ ثلاثين يومًا لغرض آخر.
     */
    fun weekOverWeek(days: List<Day>): WeekOverWeek? {
        if (days.size < 14) return null
        val current = days.subList(days.size - 7, days.size).sumOf { it.millis }
        val previous = days.subList(days.size - 14, days.size - 7).sumOf { it.millis }
        return WeekOverWeek(current, previous)
    }

    /**
     * آخر [size] يومًا من القائمة — نافذة العرض في الشاشة (أسبوع أو شهر).
     *
     * **ولماذا دالة لا `takeLast` في الواجهة:** القاعدة التي تُختبر هي «لا يُعرض أكثر مما
     * قُرئ، ولا يُقصّ ما لا يوجد»؛ ووضعها هنا يجعلها مقيسة بـJVM بدل أن تُترك لتعبير في
     * Composable لا يُقاس إلا على جهاز. والقائمة القصيرة تُعاد كما هي، و[`size`] غير الموجب
     * يعطي قائمة فارغة — فشل آمن لا استثناء في شاشة.
     */
    fun tail(days: List<Day>, size: Int): List<Day> {
        if (size <= 0) return emptyList()
        return if (days.size <= size) days else days.subList(days.size - size, days.size)
    }
}
