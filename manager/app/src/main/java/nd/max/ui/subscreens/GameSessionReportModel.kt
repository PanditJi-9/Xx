/*
 * Copyright (C) 2026-2027 Zexshia
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */
package nd.max.ui.subscreens

import nd.max.core.platform.HudTally
import nd.max.core.platform.SessionPoint
import nd.max.core.platform.StoredGameSession
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

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

// ───────────────────────────────────────────────────────────────────────────────────────────
// ما يضيفه التقرير على الأرقام الثمانية: الثبات، توزيع زمن الإطارات، أسوأ اللحظات، والتراكب.
//
// كلها حسابات صافية على نقاط المنحنى المصغّرة (١٨٠ نقطة كحدّ أقصى)، فهي **نِسَب من العيّنات
// المحفوظة** لا من كل إطار رُسم — وهذا الحدّ نفسه معلن في ترويسة الملفّ، ولا يُقدَّم الرقم على
// أنه مقياس أداة أخرى. وما لم يُقَس يبقى `null` فلا يُطبع صفرًا (ADR-07).
// ───────────────────────────────────────────────────────────────────────────────────────────

/** عتبة «ثابت»: إطار لا ينزل عن ٩٠٪ من معدّل الجلسة المعتاد. */
const val BAND_STEADY_FRACTION = 0.9f

/** عتبة «متذبذب»: بين ٧٠٪ و٩٠٪ من المعدّل المعتاد؛ ما دونها «متقطّع». */
const val BAND_UNEVEN_FRACTION = 0.7f

/** حدّ «السلاسة» الذي تقيسه أدوات التقارير الأخرى (٤٥ إطارًا فأكثر) — يُعرض ليسهل المقارنة. */
const val SMOOTH_FPS_THRESHOLD = 45f

/** قيم الإطارات الموجبة المحدودة فقط، بالترتيب الزمني. */
fun positiveFrames(points: List<SessionPoint>): List<Float> =
    points.mapNotNull { it.frames?.takeIf { v -> v.isFinite() && v > 0f } }

/**
 * وسيط الإطارات: ما **تسعى إليه اللعبة** في هذه الجلسة، لا دقّة الشاشة.
 *
 * ولماذا لا دقّة الشاشة: لعبة مقفولة على ٦٠ إطارًا فوق شاشة ٩٠ هرتز ليست «متعثّرة»، فمقارنتها
 * بالـ٩٠ تُظهر ثباتًا منخفضًا كاذبًا. الوسيط يقيس **الانتظام مع نفسها**.
 */
fun typicalFps(frames: List<Float>): Float? {
    val values = frames.filter { it.isFinite() && it > 0f }.sorted()
    if (values.isEmpty()) return null
    val mid = values.size / 2
    return if (values.size % 2 == 1) values[mid] else (values[mid - 1] + values[mid]) / 2f
}

/** حصص زمن الجلسة في ثلاث فئات؛ مجموعها واحد (حتى خطأ التقريب). */
data class FpsBands(val steady: Float, val uneven: Float, val rough: Float)

/**
 * توزيع العيّنات على «ثابت / متذبذب / متقطّع» نسبةً إلى [typicalFps].
 * أقلّ من عيّنتين لا تُسمّى توزيعًا فتُعيد `null`.
 */
fun fpsBands(frames: List<Float>): FpsBands? {
    val values = frames.filter { it.isFinite() && it > 0f }
    if (values.size < 2) return null
    val target = typicalFps(values) ?: return null
    var steady = 0
    var uneven = 0
    var rough = 0
    values.forEach { fps ->
        when {
            fps >= target * BAND_STEADY_FRACTION -> steady++
            fps >= target * BAND_UNEVEN_FRACTION -> uneven++
            else -> rough++
        }
    }
    val total = values.size.toFloat()
    return FpsBands(steady / total, uneven / total, rough / total)
}

/** نسبة العيّنات عند [threshold] إطار فأكثر (٠..١)، أو `null` بلا عيّنات. */
fun shareAtOrAbove(frames: List<Float>, threshold: Float): Float? {
    val values = frames.filter { it.isFinite() && it > 0f }
    if (values.isEmpty()) return null
    return values.count { it >= threshold }.toFloat() / values.size
}

/** الانحراف المعياري للإطارات (تذبذب المعدّل) — أصغر = أنظم. أقلّ من عيّنتين: `null`. */
fun fpsSpread(frames: List<Float>): Float? {
    val values = frames.filter { it.isFinite() && it > 0f }
    if (values.size < 2) return null
    val mean = values.sum() / values.size
    val variance = values.sumOf { ((it - mean) * (it - mean)).toDouble() } / values.size
    return sqrt(variance).toFloat()
}

/** لحظة هبوط في الجلسة: موضعها في قائمة النقاط، وزمنها، وقيمتها. */
data class WorstMoment(val index: Int, val atMs: Long, val fps: Float)

/**
 * أدنى [count] لحظات في الجلسة، **مفصولة**: هبوط واحد طويل يمرّ بعدّة عيّنات سيئة لا يُعدّ
 * ثلاث لحظات. الفاصل أقلّه ثلاث نقاط (٥٪ من الجلسة في الجلسة الكاملة).
 *
 * @param below لا تُعدّ لحظةً إلا ما نزل تحت هذا الحدّ — فجلسة مسطّحة لا تُعرض فيها «أسوأ
 * لحظات» وهي لحظات عادية.
 */
fun worstMoments(
    points: List<SessionPoint>,
    count: Int = 3,
    below: Float = Float.MAX_VALUE,
): List<WorstMoment> {
    if (count <= 0) return emptyList()
    val candidates = points.mapIndexedNotNull { index, point ->
        point.frames?.takeIf { it.isFinite() && it > 0f && it < below }
            ?.let { WorstMoment(index, point.atMs, it) }
    }
    if (candidates.isEmpty()) return emptyList()
    val gap = maxOf(3, points.size / 20)
    val picked = ArrayList<WorstMoment>(count)
    for (moment in candidates.sortedBy { it.fps }) {
        if (picked.size >= count) break
        if (picked.none { abs(it.index - moment.index) < gap }) picked += moment
    }
    return picked
}

/**
 * مقياس يُرسم **فوق** منحنى الإطارات على محور ثانٍ، فيُرى سبب الهبوط (حرارة؟ حِمل؟) في اللحظة
 * نفسها بدل رقمين متباعدين.
 *
 * @param zeroIsReading الصفر قراءة حقيقية (حِمل ٠٪ = خامل)، لا غياب كما في الحرارة والطاقة.
 * @param fixedRange نطاق ثابت لا يتبع البيانات (الحِمل ٠..١٠٠)، فيُقارَن بين الجلسات.
 */
enum class ReportOverlay(
    val unit: String,
    val decimals: Int,
    val zeroIsReading: Boolean = false,
    val fixedRange: ClosedFloatingPointRange<Float>? = null,
) {
    SocTemp("°C", 0),
    BatteryTemp("°C", 0),
    GpuLoad("%", 0, zeroIsReading = true, fixedRange = 0f..100f),
    CpuLoad("%", 0, zeroIsReading = true, fixedRange = 0f..100f),
    Power("W", 1),
    FrameTime("ms", 1);

    /** قراءة هذه النقطة لهذا المقياس، أو `null` إن لم تُقَس. */
    fun read(point: SessionPoint): Float? {
        val raw = when (this) {
            SocTemp -> point.socTempC
            BatteryTemp -> point.heat
            GpuLoad -> point.gpuLoadPercent?.toFloat()
            CpuLoad -> point.cpu?.toFloat()
            Power -> point.watt
            FrameTime -> point.frameP99Ms
        } ?: return null
        if (!raw.isFinite()) return null
        return if (zeroIsReading) raw.takeIf { it >= 0f } else raw.takeIf { it > 0f }
    }

    /** القيمة كما تُطبع: بخانات هذا المقياس ثم وحدته (النسبة ملاصقة، والباقي بمسافة). */
    fun format(value: Float): String {
        val number = "%.${decimals}f".format(value)
        return if (unit == "%") "$number%" else "$number $unit"
    }
}

/** هل في الجلسة قراءتان على الأقل لهذا المقياس؟ أقلّ من ذلك لا يُرسم خطًّا. */
fun overlayAvailable(points: List<SessionPoint>, overlay: ReportOverlay): Boolean =
    points.count { overlay.read(it) != null } >= 2

/** نطاق محور التراكب: الثابت إن وُجد، وإلا من أدنى قراءة إلى أعلاها بهامش، ولا ينهار عند التساوي. */
fun overlayRange(points: List<SessionPoint>, overlay: ReportOverlay): ClosedFloatingPointRange<Float>? {
    overlay.fixedRange?.let { return it }
    val values = points.mapNotNull { overlay.read(it) }
    if (values.size < 2) return null
    val low = values.minOrNull() ?: return null
    val high = values.maxOrNull() ?: return null
    val pad = ((high - low) * 0.08f).coerceAtLeast(0.5f)
    return (low - pad).coerceAtLeast(0f)..(high + pad)
}

/** متوسّط قراءات المقياس عبر الجلسة، أو `null` بلا قراءات. */
fun overlayAverage(points: List<SessionPoint>, overlay: ReportOverlay): Float? {
    val values = points.mapNotNull { overlay.read(it) }
    return if (values.isEmpty()) null else values.sum() / values.size
}
