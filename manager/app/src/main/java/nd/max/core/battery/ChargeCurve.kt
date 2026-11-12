/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.battery

import kotlin.math.abs

/**
 * منحنى الشحن — حكم على **نافذة** من العيّنات المسجّلة، لا على لحظة.
 *
 * **ولماذا نافذة:** «البطارية ٤٠٠٠ mV» أو «التيار ٢٠٠٠ mA» رقمان بلا معنى وحدهما: الشاحن البطيء
 * والكابل الرديء والسخونة كلها تظهر في **شكل** الشحن لا في نقطة واحدة. فالنافذة تقول: كم كانت
 * أعلى قدرة، وما متوسطها، وكم استغرق الصعود فعلًا، وبأي معدّل يصل الامتلاء لو استمر الحال.
 *
 * **وكلّه من عيّنات مسجّلة سلفًا** (`BatteryHistoryStore`): لا قياس جديد، ولا خدمة خلفية، ولا صلاحية.
 *
 * **وحدوده الصادقة:**
 * 1. القدرة = الجهد × التيار. والتيار من `BATTERY_PROPERTY_CURRENT_NOW`، **وإشارته تختلف بين
 *    النوى**: موجب على أجهزة وسالب على أخرى. فنأخذ المقدار ([abs]) لأنه ما يُقاس فعلًا، ويُقال ذلك
 *    هنا بدل ادّعاء اتفاق لا وجود له.
 * 2. «الامتلاء المتوقّع» **ليس وعدًا**: هو معدّل الصعود المقيس حتى آخر عيّنة، ممتدًّا خطيًّا — والشحن
 *    ينحدر قرب الامتلاء، فالرقم تحته لا فوقه.
 * 3. نافذة أقصر من [MIN_SPAN_MS] تُرفض (`null`) لأن معدّلًا من دقيقتين ضجيج.
 */
object ChargeCurve {

    /** أقصى نقاط تُرسم — نافذة العرض لا تزيد عن ذلك كي لا يصير الرسم خطًّا مسنونًا. */
    const val MAX_POINTS = 240

    /** أقلّ امتداد زمني تُقبل فيه نافذة (دقيقة). */
    const val MIN_SPAN_MS = 60_000L

    /** نقطة على المنحنى: النسبة، والقدرة إن عُرفت (لا صفر مكان المجهول). */
    data class Point(
        val atMillis: Long,
        val levelPercent: Int,
        val watts: Float?,
    )

    /**
     * نافذة شحن واحدة. `peakWatts`/`meanWatts` تبقى `null` إن لم تُقرأ قدرة واحدة — لا صفر.
     */
    data class Window(
        val points: List<Point>,
        val peakWatts: Float?,
        val meanWatts: Float?,
        val startLevel: Int,
        val endLevel: Int,
        val spanMinutes: Float,
        val minutesToFull: Float?,
        /** Energy moved into the battery across the measured window, in watt-hours. */
        val energyWh: Float?,
        /** Charge moved into the battery across the measured window, in milliamp-hours. */
        val chargeMah: Float?,
    )

    /**
     * قدرة العيّنة بالواط، أو `null` إن نقص جهد أو تيار.
     *
     * @return `voltageMv / 1000 × |currentUa| / 1_000_000`، أو `null` لقيمة غير موجبة.
     */
    fun watts(sample: BatterySample): Float? {
        val millivolts = sample.voltageMv?.takeIf { it > 0 } ?: return null
        val microAmps = sample.currentUa?.let { abs(it) }?.takeIf { it > 0L } ?: return null
        return (millivolts / 1000f) * (microAmps / 1_000_000f)
    }

    /**
     * تكامل شبه منحرف على العيّنات **كلها** لا على نقاط الرسم المقتطعة.
     *
     * **ولماذا لا نجمع `watts × الفاصل` بالكامل:** القدرة تتغيّر بين طرفَي العيّنة، فنصف
     * المجموع هو أفضل تقدير خطّي متاح. والفاصل يُحسب بالساعات لأن الوحدة المطلوبة Wh.
     *
     * **وحدوده:** فاصل بطرف مجهول يُتخطّى ولا يُحسب صفرًا — وهذا هو الفرق بين «لم يُقرأ»
     * و«لم يُستهلك». فإذا لم يبقَ فاصل صالح واحد كان الناتج `null` لا `0.0`.
     */
    private fun energyAndCharge(plugged: List<BatterySample>): Pair<Float?, Float?> {
        var energyWh = 0.0
        var energyIntervals = 0
        var chargeMah = 0.0
        var chargeIntervals = 0
        for (index in 0 until plugged.size - 1) {
            val first = plugged[index]
            val second = plugged[index + 1]
            val dtHours = (second.atMillis - first.atMillis).coerceAtLeast(0L) / 3_600_000.0
            if (dtHours <= 0.0) continue
            val firstWatts = watts(first)
            val secondWatts = watts(second)
            if (firstWatts != null && secondWatts != null) {
                energyWh += (firstWatts + secondWatts) / 2.0 * dtHours
                energyIntervals++
            }
            val firstMicroAmps = first.currentUa?.let { abs(it) }?.takeIf { it > 0L }
            val secondMicroAmps = second.currentUa?.let { abs(it) }?.takeIf { it > 0L }
            if (firstMicroAmps != null && secondMicroAmps != null) {
                chargeMah += (firstMicroAmps + secondMicroAmps) / 2.0 / 1000.0 * dtHours
                chargeIntervals++
            }
        }
        return (energyWh.takeIf { energyIntervals > 0 }?.toFloat()) to
            (chargeMah.takeIf { chargeIntervals > 0 }?.toFloat())
    }

    /**
     * نافذة الشحن من سلسلة عيّنات (غير مرتّبة مسموح).
     *
     * @return `null` حين لا تكفي الأدلّة: أقلّ من عيّنتين موصولتين، أو امتداد أقصر من [MIN_SPAN_MS].
     */
    fun window(samples: List<BatterySample>): Window? {
        val plugged = samples.filterNot { it.discharging }.sortedBy { it.atMillis }
        if (plugged.size < 2) return null
        val first = plugged.first()
        val last = plugged.last()
        val spanMs = last.atMillis - first.atMillis
        if (spanMs < MIN_SPAN_MS) return null

        val points = plugged.takeLast(MAX_POINTS).map {
            Point(atMillis = it.atMillis, levelPercent = it.levelPercent, watts = watts(it))
        }
        val (energyWh, chargeMah) = energyAndCharge(plugged)
        val known = points.mapNotNull { it.watts }
        val spanMinutes = spanMs / 60_000f
        val levelDelta = last.levelPercent - first.levelPercent
        val ratePerMinute = if (levelDelta > 0 && spanMinutes > 0f) levelDelta / spanMinutes else 0f
        val minutesToFull = if (ratePerMinute > 0f && last.levelPercent < 100) {
            (100 - last.levelPercent) / ratePerMinute
        } else {
            null
        }

        return Window(
            points = points,
            peakWatts = known.maxOrNull(),
            meanWatts = known.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
            startLevel = first.levelPercent,
            endLevel = last.levelPercent,
            spanMinutes = spanMinutes,
            minutesToFull = minutesToFull,
            energyWh = energyWh,
            chargeMah = chargeMah,
        )
    }
}
