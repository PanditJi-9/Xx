/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.platform

/**
 * إيقاع تسليم مستشعر واحد، **مقيسًا من الطوابع التي وصلت فعلًا** لا من التأخير المطلوب.
 *
 * `SENSOR_DELAY_UI` طلبٌ لا وعد: بعض الأجهزة تسلّم أبطأ، وبعضها يقطع التسليم عند إطفاء الشاشة.
 * فالرقم المعروض هنا مشتقّ من `SensorEvent.timestamp` (ساعة رتيبة بالنانونية)، ولا يُدَّعى تواتر
 * قبل وصول [MIN_SAMPLES] حدثًا — الفاصل الواحد ليس معدّلًا.
 *
 * والنافذة متحرّكة على آخر [capacity] حدثًا: الفاصل الطويل يظلّ ظاهرًا بعد أن يهدأ المستشعر،
 * ثم يخرج من النافذة كما تخرج العيّنات نفسها. والطابع غير المتزايد يُهمَل كاملًا، فلا يُخترع
 * فاصل سالب من حدث وصل بترتيب مختلف.
 */
class SensorCadence(private val capacity: Int = DEFAULT_CAPACITY) {

    /** ما يُعرض عن مستشعر: عدّ الأحداث، ومعدّلها، وأطول فاصل بين حدثين داخل النافذة. */
    data class Snapshot(
        val received: Long,
        val hz: Float? = null,
        val longestGapMs: Long? = null,
    )

    private val stamps = LongArray(capacity.coerceAtLeast(MIN_SAMPLES))
    private var received = 0L
    private var next = 0
    private var lastStampNs = Long.MIN_VALUE

    /** يُسجّل حدثًا ويُعيد اللقطة بعده؛ والحدث غير المتزايد لا يُسجَّل ولا يُغيّر شيئًا. */
    fun record(timestampNs: Long): Snapshot {
        if (received > 0L && timestampNs <= lastStampNs) return snapshot()
        stamps[next] = timestampNs
        next = (next + 1) % stamps.size
        lastStampNs = timestampNs
        received += 1
        return snapshot()
    }

    fun snapshot(): Snapshot {
        val window = minOf(received, stamps.size.toLong()).toInt()
        if (window < MIN_SAMPLES) return Snapshot(received)
        var first = Long.MAX_VALUE
        var previous = 0L
        var longest = 0L
        for (i in 0 until window) {
            val stamp = stamps[(next - window + i + stamps.size) % stamps.size]
            if (i == 0) {
                first = stamp
            } else {
                val gap = stamp - previous
                if (gap > longest) longest = gap
            }
            previous = stamp
        }
        val span = previous - first
        val hz = if (span > 0L) ((window - 1) * 1_000_000_000.0 / span).toFloat() else null
        return Snapshot(received = received, hz = hz, longestGapMs = longest / 1_000_000L)
    }

    companion object {
        /** نافذة صغيرة تكفي لأن يظهر الانقطاع، ولا تُبقي فاصلًا عمره دقائق كأنه الحاضر. */
        const val DEFAULT_CAPACITY = 64

        /** أقلّ من ثلاثة أحداث = فاصل واحد، والفاصل الواحد ليس معدّلًا. */
        const val MIN_SAMPLES = 3
    }
}
