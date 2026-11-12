/*
 * Copyright (C) 2026-2027 Zexshia
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */
package nd.max.ui.subscreens

import nd.max.core.platform.HudTally
import nd.max.core.platform.SessionPoint
import nd.max.core.platform.StoredGameSession
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * **الأرقام التي يطبعها تقرير الجلسة — حيّة أو محفوظة، بشكل واحد.**
 *
 * التقرير في هذه الشاشة واحد: بطاقة تُقرأ فيها الجلسة كما يقرأها تقرير Scene (اسم اللعبة،
 * الحزمة، المدّة، ثمانية أرقام، ومنحنى الإطارات). وكان في الشاشة مسارَان — واحد للجلسة
 * الحيّة وآخر للمحفوظة — فكان الرقمان نفساهما يُنسَّقان مرّتين، وأحدهما يفقد حقولًا.
 * هذه الطبقة تُلغي الازدواج: كلاهما يتحوّل إلى [GameReportStats]، والرسم لا يعرف من أين جاء.
 *
 * وحدّها المعلن: النسب المئوية (١٪) تُحسب من نقاط المنحنى المصغّرة (١٨٠ نقطة كحدّ أقصى)،
 * لا من ملايين الإطارات الخامّ — فتُسمّى «أدنى ١٪ من العيّنات» ولا تُقدَّم على أنها مقياس
 * Scene نفسه. وما لم يُقَس يبقى `null` فلا يُطبع صفرًا.
 */
data class GameReportStats(
    val durationMs: Long = 0L,
    val samples: Int = 0,
    val droppedSamples: Int = 0,
    val averageFps: Float? = null,
    val minFps: Float? = null,
    val maxFps: Float? = null,
    /** أدنى ١٪ من عيّنات المنحنى — لا من الإطارات الخامّ. */
    val onePercentLowFps: Float? = null,
    val frameP99Ms: Float? = null,
    val jankPercent: Int? = null,
    val maxSocTempC: Float? = null,
    val batteryTempStartC: Float? = null,
    val batteryTempEndC: Float? = null,
    val averagePowerMw: Int? = null,
    val refreshHz: Int? = null,
    val renderer: String? = null,
    val cpuFreqAverageMhz: Int? = null,
    val gpuLoadAverage: Float? = null,
    val voltageMinMv: Int? = null,
    val energyWh: Float? = null,
    val viewSize: String? = null,
    val powerSave: Boolean = false,
    val stopReasonId: String? = null,
) {
    /** فرق حرارة البطارية داخل الجلسة — الحقل الثالث في خانة الحرارة. */
    val batteryTempDeltaC: Float?
        get() = if (batteryTempStartC != null && batteryTempEndC != null) {
            batteryTempEndC - batteryTempStartC
        } else {
            null
        }

    val isEmpty: Boolean get() = samples == 0 && averageFps == null
}

/** تقرير جلسة محفوظة — كل حقل من السجلّ نفسه، لا من حساب الشاشة. */
fun storedReportStats(session: StoredGameSession): GameReportStats {
    val temps = session.series.mapNotNull { it.heat?.takeIf { v -> v > 0f } }
    return GameReportStats(
        durationMs = session.spanMs,
        samples = session.samples,
        droppedSamples = session.droppedSamples,
        averageFps = session.framesAverage,
        minFps = session.framesLow,
        maxFps = session.framesHigh,
        onePercentLowFps = lowPercentileFps(session.series.mapNotNull { it.frames }),
        frameP99Ms = session.frameP99AverageMs,
        jankPercent = session.jankAverage,
        maxSocTempC = session.socTempPeak,
        batteryTempStartC = temps.firstOrNull(),
        batteryTempEndC = temps.lastOrNull(),
        averagePowerMw = wattToMw(session.powerAverage),
        refreshHz = session.refreshHz,
        renderer = session.renderer,
        cpuFreqAverageMhz = session.cpuFreqAverageMhz,
        gpuLoadAverage = session.gpuLoadAverage,
        voltageMinMv = session.batteryVoltageMinMv,
        energyWh = session.energyWh,
        viewSize = session.viewSize,
        powerSave = session.powerSave,
        stopReasonId = session.stopReason,
    )
}

/**
 * تقرير الجلسة الجارية — من الحائز والمنحنى الحيّين.
 *
 * @param elapsedMs مدّة الجلسة من ساعة بدئها، لا `spanMs` وحده: الرقم يكبر أمام العين.
 */
fun liveReportStats(
    tally: HudTally?,
    series: List<SessionPoint>,
    elapsedMs: Long,
    refreshHz: Int? = null,
): GameReportStats {
    val temps = series.mapNotNull { it.heat?.takeIf { v -> v > 0f } }
    return GameReportStats(
        durationMs = elapsedMs,
        samples = tally?.samples ?: series.size,
        droppedSamples = tally?.droppedSamples ?: 0,
        averageFps = tally?.framesAverage,
        minFps = tally?.framesLow,
        maxFps = tally?.framesHigh,
        onePercentLowFps = lowPercentileFps(series.mapNotNull { it.frames }),
        frameP99Ms = tally?.frameP99AverageMs,
        jankPercent = tally?.jankAverage,
        maxSocTempC = tally?.socTempPeak,
        batteryTempStartC = temps.firstOrNull(),
        batteryTempEndC = temps.lastOrNull(),
        averagePowerMw = wattToMw(tally?.powerAverage),
        refreshHz = refreshHz,
        renderer = tally?.renderer,
        cpuFreqAverageMhz = tally?.cpuFreqAverageMhz,
        gpuLoadAverage = tally?.gpuLoadAverage,
        voltageMinMv = tally?.batteryVoltageMinMv,
        energyWh = tally?.energyWh,
    )
}

/**
 * أدنى نسبة مئوية من قيم الإطارات.
 *
 * الترتيب تصاعديّ، والموضع `floor((n-1) * percent / 100)` — فـ`1%` من ١٨٠ نقطة هو ثاني
 * أبطأ نقطة تقريبًا، لا صفرًا ولا الأسرع. والقائمة الفارغة تُعيد `null` لا `0f`.
 */
fun lowPercentileFps(frames: List<Float>, percent: Double = 1.0): Float? {
    val values = frames.filter { it.isFinite() && it > 0f }.sorted()
    if (values.isEmpty()) return null
    val index = floor((values.size - 1) * (percent / 100.0)).toInt().coerceIn(0, values.lastIndex)
    return values[index]
}

/** الواط إلى ملي واط كما يُطبع في التقرير (7354 mW لا 7.35 W). */
fun wattToMw(watt: Float?): Int? {
    val value = watt?.takeIf { it > 0f } ?: return null
    return (value * 1000f).roundToInt()
}

/**
 * نطاق المحور الرأسي للمنحنى: من الأدنى المقيس إلى الأعلى، مع هامش صغير،
 * وتُضمّ دقّة العرض فيه حين تكون أعلى من كل القيم — فخطّها لا يخرج من الرسم.
 */
fun fpsAxisRange(
    points: List<SessionPoint>,
    minFps: Float?,
    maxFps: Float?,
    refreshHz: Int?,
): ClosedFloatingPointRange<Float> {
    val frames = points.mapNotNull { it.frames?.takeIf { v -> v > 0f } }
    val low = minOf(minFps ?: frames.minOrNull() ?: 0f, frames.minOrNull() ?: Float.MAX_VALUE)
    val high = maxOf(maxFps ?: frames.maxOrNull() ?: 1f, frames.maxOrNull() ?: -1f, (refreshHz ?: 0).toFloat())
    val safeLow = if (low == Float.MAX_VALUE) 0f else low
    val safeHigh = maxOf(high, safeLow + 1f)
    val pad = (safeHigh - safeLow) * 0.08f + 0.5f
    return (safeLow - pad).coerceAtLeast(0f)..(safeHigh + pad)
}
