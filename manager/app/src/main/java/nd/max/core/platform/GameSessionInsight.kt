/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.core.platform

import kotlin.math.roundToInt

/**
 * حكم الجلسة: هل كانت أفضل أم أسوأ من المعتاد، وما السبب المرجَّح، وإلى أي شاشة يقود الفعل.
 *
 * **ولماذا هذا الملف صافٍ بلا أندرويد:** الحكم سياسة أرقام لا رسم، فيُختبر كاملًا بلا جهاز.
 * والأساس (baseline) وسيط جلسات **نفس اللعبة** لا متوسط الألعاب كلها: لعبة ثقيلة وأخرى خفيفة
 * لا تُقارنان، ومقارنتهما تُنتج حكمًا كاذبًا في الاتجاهين.
 */
enum class SessionVerdict { FirstRun, Better, Similar, Worse, Unusable }

/** السبب المرجَّح للتراجع — مرتّب بالأولوية لا بالعرض. */
enum class SessionCause { Thermal, Jank, Energy, PowerSave, Refresh, Renderer, None }

/** الوجهة التي يُصلحها هذا السبب، أو [None] حين لا فعل. */
enum class SessionAction { Thermal, Display, Power, Renderer, None }

/**
 * العتبات في مكان واحد: تُقرأ ولا تُزرع في الشروط. وتغييرها هنا يغيّر الحكم في الشاشة
 * والإشعار معًا لأن كليهما يستدعي [sessionInsight].
 */
data class InsightRules(
    val minSamples: Int = 20,
    val baselineSessions: Int = 5,
    val framesWorsePercent: Int = 5,
    val framesBetterPercent: Int = 5,
    val jankWorsePoints: Int = 3,
    val jankBetterPoints: Int = 3,
    val thermalRiseC: Float = 2.5f,
    val thermalCeilingC: Float = 42f,
    val energyRisePercent: Int = 15,
    val refreshDropHz: Float = 1f
)

/** وسيط جلسات سابقة لنفس الحزمة — كل حقل `null` حين لم تُقَس له قيمة في الأساس. */
data class SessionBaseline(
    val frames: Float? = null,
    val jank: Int? = null,
    val socTemp: Float? = null,
    val energyWh: Float? = null,
    val p99: Float? = null,
    val refreshHz: Float? = null,
    val renderer: String? = null,
    val count: Int = 0
)

data class SessionInsight(
    val verdict: SessionVerdict,
    val cause: SessionCause = SessionCause.None,
    val action: SessionAction = SessionAction.None,
    val framesDeltaPercent: Int? = null,
    val jankDelta: Int? = null,
    val socTempDelta: Float? = null,
    val energyDeltaPercent: Int? = null,
    val baselineCount: Int = 0,
    val baselineFrames: Float? = null,
    val worstPoint: SessionPoint? = null
)

/**
 * أساس المقارنة: آخر [max] جلسات لنفس الحزمة، تُقبل فقط إن بلغت [InsightRules.minSamples]
 * عيّنة وكان لها متوسط إطارات. والوسيط لا المتوسط: جلسة واحدة كارثية لا تسمّم الأساس.
 */
fun sessionBaseline(
    history: List<StoredGameSession>,
    packageName: String?,
    excludeId: String? = null,
    max: Int = 5,
    rules: InsightRules = InsightRules()
): SessionBaseline? {
    val pool = history.asSequence()
        .filter { it.id != excludeId }
        .filter { it.samples >= rules.minSamples && it.framesAverage != null }
        .filter { packageName == null || it.packageName == packageName }
        .sortedByDescending { it.endedAtMs }
        .take(max.coerceAtLeast(1))
        .toList()
    if (pool.isEmpty()) return null
    val renderer = pool.mapNotNull { it.renderer?.takeIf(String::isNotBlank) }
        .groupingBy { it.lowercase() }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
    return SessionBaseline(
        frames = median(pool.mapNotNull { it.framesAverage }),
        jank = median(pool.mapNotNull { it.jankAverage?.toFloat() })?.roundToInt(),
        socTemp = median(pool.mapNotNull { it.socTempPeak }),
        energyWh = median(pool.mapNotNull { it.energyWh }),
        p99 = median(pool.mapNotNull { it.frameP99AverageMs }),
        refreshHz = median(pool.mapNotNull { it.refreshHz?.toFloat() }),
        renderer = renderer,
        count = pool.size
    )
}

/**
 * الحكم. الترتيب مقصود:
 * 1. جلسة قصيرة أو بلا إطارات ⇒ [SessionVerdict.Unusable] — لا تُقارن بما لا يُقاس.
 * 2. أقلّ من جلستين في الأساس ⇒ [SessionVerdict.FirstRun] — أول مرة ليست «أسوأ».
 * 3. أسوأ/أفضل بعتبة مزدوجة (إطارات أو jank) ⇒ وإلا [SessionVerdict.Similar].
 * والسبب يُسمّى في حالة التراجع فقط؛ جلسة أفضل لا تحتاج تفسيرًا يُصلَح.
 */
fun sessionInsight(
    session: StoredGameSession,
    history: List<StoredGameSession>,
    rules: InsightRules = InsightRules()
): SessionInsight {
    val worst = worstMoment(session)
    val base = sessionBaseline(history, session.packageName, session.id, rules.baselineSessions, rules)
    val frames = session.framesAverage
    if (session.samples < rules.minSamples || frames == null) {
        return SessionInsight(
            verdict = SessionVerdict.Unusable,
            baselineCount = base?.count ?: 0,
            worstPoint = worst
        )
    }
    if (base == null || base.count < 2 || base.frames == null) {
        return SessionInsight(
            verdict = SessionVerdict.FirstRun,
            baselineCount = base?.count ?: 0,
            baselineFrames = base?.frames,
            worstPoint = worst
        )
    }
    val framesDelta = percentDelta(frames, base.frames)
    val jankDelta = session.jankAverage?.let { current -> base.jank?.let { current - it } }
    val socDelta = session.socTempPeak?.let { current -> base.socTemp?.let { current - it } }
    val energyDelta = session.energyWh?.let { current -> base.energyWh?.let { percentDelta(current, it) } }
    val worse = framesDelta <= -rules.framesWorsePercent ||
        (jankDelta ?: 0) >= rules.jankWorsePoints
    val better = framesDelta >= rules.framesBetterPercent ||
        (jankDelta ?: 0) <= -rules.jankBetterPoints
    val verdict = when {
        worse -> SessionVerdict.Worse
        better -> SessionVerdict.Better
        else -> SessionVerdict.Similar
    }
    val cause = if (verdict == SessionVerdict.Worse) {
        causeOf(session, base, socDelta, jankDelta, energyDelta, rules)
    } else {
        SessionCause.None
    }
    return SessionInsight(
        verdict = verdict,
        cause = cause,
        action = actionOf(cause),
        framesDeltaPercent = framesDelta,
        jankDelta = jankDelta,
        socTempDelta = socDelta,
        energyDeltaPercent = energyDelta,
        baselineCount = base.count,
        baselineFrames = base.frames,
        worstPoint = worst
    )
}

/** أسوأ لحظة في السلسلة: أدنى إطارات مسجَّلة. `null` إن لم تُسجَّل إطارات أصلًا. */
fun worstMoment(session: StoredGameSession): SessionPoint? = worstPointOf(session.series)

/** أدنى إطار في سلسلة نقاط — تُستعمل للسجلّ ولرسم العلامة على المنحنى. */
fun worstPointOf(points: List<SessionPoint>): SessionPoint? = points
    .asSequence()
    .filter { it.frames != null }
    .minByOrNull { it.frames ?: Float.MAX_VALUE }

private fun causeOf(
    session: StoredGameSession,
    base: SessionBaseline,
    socDelta: Float?,
    jankDelta: Int?,
    energyDelta: Int?,
    rules: InsightRules
): SessionCause {
    val socPeak = session.socTempPeak
    if (socDelta != null && socDelta >= rules.thermalRiseC && socPeak != null && socPeak >= rules.thermalCeilingC) {
        return SessionCause.Thermal
    }
    if ((jankDelta ?: 0) >= rules.jankWorsePoints) return SessionCause.Jank
    if ((energyDelta ?: 0) >= rules.energyRisePercent) return SessionCause.Energy
    if (session.powerSave) return SessionCause.PowerSave
    val currentHz = session.refreshHz
    val baseHz = base.refreshHz
    if (currentHz != null && baseHz != null && currentHz + rules.refreshDropHz <= baseHz) {
        return SessionCause.Refresh
    }
    val currentRenderer = session.renderer
    val baseRenderer = base.renderer
    if (currentRenderer != null && baseRenderer != null && !currentRenderer.equals(baseRenderer, true)) {
        return SessionCause.Renderer
    }
    return SessionCause.None
}

private fun actionOf(cause: SessionCause): SessionAction = when (cause) {
    SessionCause.Thermal -> SessionAction.Thermal
    SessionCause.Jank, SessionCause.Refresh -> SessionAction.Display
    SessionCause.Energy, SessionCause.PowerSave -> SessionAction.Power
    SessionCause.Renderer -> SessionAction.Renderer
    SessionCause.None -> SessionAction.None
}

private fun percentDelta(current: Float, base: Float): Int {
    if (base == 0f) return 0
    return ((current - base) / base * 100f).roundToInt()
}

private fun median(values: List<Float>): Float? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) {
        sorted[middle]
    } else {
        (sorted[middle - 1] + sorted[middle]) / 2f
    }
}
