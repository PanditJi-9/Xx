package nd.max.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حكم الجلسة: الوسيط يُقارن لا المتوسط، والسبب يُسمّى بترتيبه لا بأول ما يخطر.
 * كل الحالات هنا تُبنى بيد، فلا تعتمد على جهاز ولا على وقت.
 */
class GameSessionInsightTest {

    private fun session(
        id: String,
        pkg: String? = "com.example.game",
        frames: Float? = 60f,
        jank: Int? = 4,
        soc: Float? = 38f,
        energy: Float? = 1.0f,
        samples: Int = 300,
        refresh: Int? = 120,
        renderer: String? = "skiagl",
        powerSave: Boolean = false,
        endedAt: Long = 1_000L,
        series: List<SessionPoint> = emptyList()
    ): StoredGameSession = StoredGameSession(
        id = id,
        packageName = pkg,
        startedAtMs = endedAt - 60_000L,
        endedAtMs = endedAt,
        samples = samples,
        framesAverage = frames,
        jankAverage = jank,
        socTempPeak = soc,
        energyWh = energy,
        refreshHz = refresh,
        renderer = renderer,
        powerSave = powerSave,
        series = series
    )

    private fun history(vararg sessions: StoredGameSession) = sessions.toList()

    @Test
    fun baselineUsesMedianOfRecentSessionsOfTheSameGame() {
        val base = sessionBaseline(
            history = history(
                session("a", frames = 40f, endedAt = 10L),
                session("b", frames = 60f, endedAt = 20L),
                session("c", frames = 50f, endedAt = 30L)
            ),
            packageName = "com.example.game"
        )
        assertEquals(50f, base?.frames ?: 0f, 0.01f)
        assertEquals(3, base?.count ?: -1)
    }

    @Test
    fun baselineIgnoresOtherGamesShortSessionsAndItself() {
        val base = sessionBaseline(
            history = history(
                session("self", frames = 10f, endedAt = 99L),
                session("short", frames = 10f, samples = 5, endedAt = 98L),
                session("other", pkg = "com.example.other", frames = 10f, endedAt = 97L),
                session("real", frames = 55f, endedAt = 96L)
            ),
            packageName = "com.example.game",
            excludeId = "self"
        )
        assertEquals(55f, base?.frames ?: 0f, 0.01f)
        assertEquals(1, base?.count ?: -1)
    }

    @Test
    fun shortSessionIsUnusableEvenWithHistory() {
        val current = session("now", frames = 20f, samples = 12)
        val insight = sessionInsight(current, history(session("a"), session("b")))
        assertEquals(SessionVerdict.Unusable, insight.verdict)
        assertNull(insight.framesDeltaPercent)
    }

    @Test
    fun fewerThanTwoEarlierSessionsIsFirstRun() {
        val current = session("now", frames = 58f)
        val insight = sessionInsight(current, history(session("only", frames = 60f)))
        assertEquals(SessionVerdict.FirstRun, insight.verdict)
        assertEquals(1, insight.baselineCount)
    }

    @Test
    fun dropOfFivePercentOrMoreIsWorse() {
        val current = session("now", frames = 54f)
        val insight = sessionInsight(current, history(session("a", frames = 60f), session("b", frames = 60f)))
        assertEquals(SessionVerdict.Worse, insight.verdict)
        assertEquals(-10, insight.framesDeltaPercent)
    }

    @Test
    fun riseOfFivePercentOrMoreIsBetter() {
        val current = session("now", frames = 63f)
        val insight = sessionInsight(current, history(session("a", frames = 60f), session("b", frames = 60f)))
        assertEquals(SessionVerdict.Better, insight.verdict)
        assertEquals(5, insight.framesDeltaPercent)
    }

    @Test
    fun sameFramesButThreeJankPointsIsWorse() {
        val current = session("now", frames = 60f, jank = 9)
        val insight = sessionInsight(current, history(session("a", jank = 4), session("b", jank = 4)))
        assertEquals(SessionVerdict.Worse, insight.verdict)
        assertEquals(SessionCause.Jank, insight.cause)
        assertEquals(SessionAction.Display, insight.action)
    }

    @Test
    fun heatRiseIsNamedBeforeJank() {
        val current = session("now", frames = 52f, jank = 12, soc = 47f)
        val insight = sessionInsight(
            current,
            history(session("a", frames = 60f, jank = 4, soc = 40f), session("b", frames = 60f, jank = 4, soc = 40f))
        )
        assertEquals(SessionVerdict.Worse, insight.verdict)
        assertEquals(SessionCause.Thermal, insight.cause)
        assertEquals(SessionAction.Thermal, insight.action)
    }

    @Test
    fun energyRiseIsNamedWhenFramesAndJankAreClose() {
        val current = session("now", frames = 57f, jank = 4, energy = 1.5f)
        val insight = sessionInsight(
            current,
            history(session("a", frames = 60f, energy = 1.0f), session("b", frames = 60f, energy = 1.0f))
        )
        assertEquals(SessionVerdict.Worse, insight.verdict)
        assertEquals(SessionCause.Energy, insight.cause)
        assertEquals(SessionAction.Power, insight.action)
    }

    @Test
    fun powerSaverIsBlamedWhenNothingElseMoved() {
        val current = session("now", frames = 57f, jank = 4, powerSave = true)
        val insight = sessionInsight(
            current,
            history(session("a", frames = 60f), session("b", frames = 60f))
        )
        assertEquals(SessionVerdict.Worse, insight.verdict)
        assertEquals(SessionCause.PowerSave, insight.cause)
        assertEquals(SessionAction.Power, insight.action)
    }

    @Test
    fun betterRunHasNoCauseAndNoAction() {
        val current = session("now", frames = 66f)
        val insight = sessionInsight(current, history(session("a", frames = 60f), session("b", frames = 60f)))
        assertEquals(SessionVerdict.Better, insight.verdict)
        assertEquals(SessionCause.None, insight.cause)
        assertEquals(SessionAction.None, insight.action)
    }

    @Test
    fun worstPointIsTheLowestFrameNotTheFirst() {
        val points = listOf(
            SessionPoint(atMs = 0L, frames = 60f),
            SessionPoint(atMs = 1_000L, frames = 31f),
            SessionPoint(atMs = 2_000L, frames = 55f)
        )
        val worst = worstPointOf(points)
        assertEquals(1_000L, worst?.atMs ?: -1L)
        assertEquals(31f, worst?.frames ?: 0f, 0.01f)
        assertEquals(1_000L, worstMoment(session("now", series = points))?.atMs ?: -1L)
    }

    @Test
    fun zeroBaselineDoesNotDivideByZero() {
        val current = session("now", frames = 60f, energy = 2f)
        val insight = sessionInsight(
            current,
            history(session("a", energy = 0f), session("b", energy = 0f))
        )
        assertEquals(0, insight.energyDeltaPercent)
        assertTrue(insight.verdict == SessionVerdict.Similar || insight.verdict == SessionVerdict.Better)
    }
}
