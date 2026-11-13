/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** يوم واحد في إيقاع الأسبوع: تاريخه ووقت اللعب المسجّل فيه. */
data class CareerDay(val date: LocalDate, val totalMs: Long)

/** لعبة في ترتيب المسيرة؛ الوقت وحده، والاسم من المكتبة لا من السجلّ. */
data class CareerGame(val packageName: String, val totalMs: Long)

/** محطّة تُشتقّ من السجلّ وحده. لا مكافآت ولا نقاط ولا عملة. */
enum class CareerBadge { FirstSession, TenSessions, Marathon, WeekStreak, FiveGames }

/**
 * محطّة مع تقدّمها. `progress` لا يتجاوز `target`، فلا يُعرض «١٢ من ١٠».
 * والقفل ليس عيبًا: هو الفرق الصادق بين ما رُصد وما لم يُرصد بعد.
 */
data class CareerMilestone(
    val badge: CareerBadge,
    val progress: Int,
    val target: Int,
) {
    val unlocked: Boolean get() = progress >= target
}

/**
 * مسيرة المكتبة: إجمالياتها، إيقاع أسبوعها، أطول جلسة، جلسات ما بعد منتصف الليل،
 * ومحطّاتها. كل حقل مشتقّ من `PlaySessionLedger`، ولا رقم يُختلق عند غياب السجلّ.
 */
data class CareerStats(
    val totalMs: Long,
    val sessionCount: Int,
    val gamesPlayed: Int,
    val streakDays: Int,
    val week: List<CareerDay>,
    /** وقت اللعب في الأيام السبعة السابقة للأسبوع الحالي؛ مرجع المقارنة الصادق. */
    val lastWeekMs: Long,
    val bestSessionMs: Long,
    val bestSessionPackage: String?,
    val nightSessions: Int,
    /** أحدث ثلاث جلسات صالحة من المكتبة؛ تُعرض كما رُصدت بلا إعادة كتابة. */
    val recent: List<PlaySession>,
    val top: List<CareerGame>,
    val milestones: List<CareerMilestone>,
) {
    companion object {
        const val WEEK_DAYS = 7
        const val MARATHON_MINUTES = 120
        const val TEN_SESSIONS = 10
        const val FIVE_GAMES = 5
        const val WEEK_STREAK = 7

        /** نافذة الرؤى الطويلة: ثلاثة أشهر، لا أكثر. */
        const val NINETY_DAYS = 90
    }
}

/**
 * مشتقّات المسيرة — **نقيّة بالكامل**: لا أندرويد ولا وقت حقيقي، فتُقاس على JVM.
 *
 * ### القواعد
 * - الجلسة تُقرأ فقط إن كانت صالحة (`PlaySessionLedger.valid`)، وللعبة ما زالت في المكتبة،
 *   ولم تنتهِ في المستقبل (ساعة منحرفة لا تُصنع منها مسيرة).
 * - جلسة تعبر منتصف الليل تُنسب إلى يوم انتهائها؛ لا تُقسَّم على يومين.
 * - «بعد منتصف الليل» تعني بداية بين ٢٢:٠٠ و٠٥:٠٠ بالتوقيت المحلي.
 * - المحطّات تُقاس بحدود مثبّتة هنا، والواجهة تعرض التقدّم لا وعدًا بمكافأة.
 */
object CareerLedger {
    fun build(
        stats: Map<String, GamePlayStats>,
        sessions: List<PlaySession>,
        library: Set<String>,
        nowMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): CareerStats {
        val played = stats.values.filter {
            it.packageName in library && it.sessionCount > 0 && it.totalMs > 0L
        }
        val today = if (nowMs > 0L) {
            Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        } else {
            null
        }
        val weekStart = today?.minusDays((CareerStats.WEEK_DAYS - 1).toLong())
        val week = buildList {
            if (weekStart != null) {
                for (offset in 0 until CareerStats.WEEK_DAYS) {
                    add(CareerDay(weekStart.plusDays(offset.toLong()), 0L))
                }
            }
        }.toMutableList()
        val weekIndex = week.withIndex().associate { it.value.date to it.index }
        val todayEpoch = today?.toEpochDay()
        var lastWeekMs = 0L

        var bestMs = 0L
        var bestPackage: String? = null
        var night = 0
        for (session in sessions) {
            if (session.packageName !in library) continue
            if (!PlaySessionLedger.valid(session)) continue
            if (session.endedAtMs > nowMs) continue
            if (session.durationMs > bestMs) {
                bestMs = session.durationMs
                bestPackage = session.packageName
            }
            val start = Instant.ofEpochMilli(session.startedAtMs).atZone(zone)
            if (start.hour >= 22 || start.hour < 5) night += 1
            val day = Instant.ofEpochMilli(session.endedAtMs).atZone(zone).toLocalDate()
            if (todayEpoch != null) {
                val dayEpoch = day.toEpochDay()
                if (dayEpoch in (todayEpoch - 13L)..(todayEpoch - 7L)) {
                    lastWeekMs += session.durationMs
                }
            }
            val index = weekIndex[day] ?: continue
            week[index] = week[index].copy(totalMs = week[index].totalMs + session.durationMs)
        }

        val totalMs = played.sumOf { it.totalMs }
        val sessionCount = played.sumOf { it.sessionCount }
        val streak = streak(sessions, library, nowMs, zone)
        val gamesPlayed = played.size

        return CareerStats(
            totalMs = totalMs,
            sessionCount = sessionCount,
            gamesPlayed = gamesPlayed,
            streakDays = streak,
            week = week,
            lastWeekMs = lastWeekMs,
            bestSessionMs = bestMs,
            bestSessionPackage = bestPackage,
            nightSessions = night,
            recent = recent(sessions, library, nowMs),
            top = played
                .sortedWith(compareByDescending<GamePlayStats> { it.totalMs }.thenBy { it.packageName })
                .map { CareerGame(it.packageName, it.totalMs) },
            milestones = listOf(
                CareerMilestone(CareerBadge.FirstSession, sessionCount.coerceAtMost(1), 1),
                CareerMilestone(
                    CareerBadge.TenSessions,
                    sessionCount.coerceAtMost(CareerStats.TEN_SESSIONS),
                    CareerStats.TEN_SESSIONS,
                ),
                CareerMilestone(
                    CareerBadge.Marathon,
                    (bestMs / 60_000L).coerceAtMost(CareerStats.MARATHON_MINUTES.toLong()).toInt(),
                    CareerStats.MARATHON_MINUTES,
                ),
                CareerMilestone(
                    CareerBadge.WeekStreak,
                    streak.coerceAtMost(CareerStats.WEEK_STREAK),
                    CareerStats.WEEK_STREAK,
                ),
                CareerMilestone(
                    CareerBadge.FiveGames,
                    gamesPlayed.coerceAtMost(CareerStats.FIVE_GAMES),
                    CareerStats.FIVE_GAMES,
                ),
            ),
        )
    }

    /**
     * أحدث الجلسات الصالحة، بترتيب النهاية تنازليًا. نفس شروط المسيرة:
     * لعبة باقية في المكتبة، وجلسة صالحة، ولا نهاية في المستقبل.
     */
    fun recent(
        sessions: List<PlaySession>,
        library: Set<String>,
        nowMs: Long,
        limit: Int = 3,
    ): List<PlaySession> {
        if (limit <= 0) return emptyList()
        return sessions.asSequence()
            .filter {
                it.packageName in library &&
                    PlaySessionLedger.valid(it) &&
                    it.endedAtMs <= nowMs
            }
            .sortedByDescending { it.endedAtMs }
            .take(limit)
            .toList()
    }

    /**
     * أيام متتالية تنتهي اليوم أو أمس. اليوم الغائب لا يقطع السلسلة إن كان أمس حاضرًا،
     * لأن اليوم لم ينتهِ بعد؛ وأي فجوة أقدم تقطعها.
     */
    fun streak(
        sessions: List<PlaySession>,
        library: Set<String>,
        nowMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Int {
        if (sessions.isEmpty() || library.isEmpty() || nowMs <= 0L) return 0
        val days = HashSet<LocalDate>()
        for (session in sessions) {
            if (session.packageName !in library) continue
            if (session.endedAtMs <= 0L || session.endedAtMs > nowMs) continue
            days += Instant.ofEpochMilli(session.endedAtMs).atZone(zone).toLocalDate()
        }
        if (days.isEmpty()) return 0
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        var cursor = when {
            today in days -> today
            today.minusDays(1) in days -> today.minusDays(1)
            else -> return 0
        }
        var count = 0
        while (cursor in days) {
            count += 1
            cursor = cursor.minusDays(1)
        }
        return count
    }

    /**
     * نافذة أطول من خريطة الأسبوع: كم يومًا لعب فيها فعلًا خلال آخر [days] يومًا، ومتوسّط اليوم
     * النشط، وأكثر يوم ازدحامًا، والفترةُ السابقة للمقارنة.
     *
     * **وحدّها المُعلن:** تُبنى من السجلّ المحفوظ لا من عدّاد خارجي، فإن كان السجلّ قد بلغ سقفه
     * فالفترة ليست كاملة — ولذلك يعود [oldestAtMs] ليقول من أين تبدأ البيانات فعلًا، بدل أن
     * تُدَّعى تسعون يومًا كاملة.
     */
    fun span(
        sessions: List<PlaySession>,
        library: Set<String>,
        nowMs: Long,
        days: Int = NINETY_DAYS,
        zone: ZoneId = ZoneId.systemDefault()
    ): CareerSpan {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val start = today.minusDays((days - 1).toLong())
        val previousStart = start.minusDays(days.toLong())
        var totalMs = 0L
        var previousMs = 0L
        var oldest: Long? = null
        val perDay = HashMap<LocalDate, Long>()
        sessions.forEach { session ->
            if (session.packageName !in library) return@forEach
            if (!PlaySessionLedger.valid(session)) return@forEach
            if (session.endedAtMs > nowMs) return@forEach
            val day = Instant.ofEpochMilli(session.endedAtMs).atZone(zone).toLocalDate()
            val seen = oldest
            oldest = if (seen == null) session.endedAtMs else minOf(seen, session.endedAtMs)
            when {
                !day.isBefore(start) -> {
                    totalMs += session.durationMs
                    perDay[day] = (perDay[day] ?: 0L) + session.durationMs
                }
                !day.isBefore(previousStart) -> previousMs += session.durationMs
            }
        }
        return CareerSpan(
            days = days,
            totalMs = totalMs,
            activeDays = perDay.size,
            averageActiveDayMs = if (perDay.isEmpty()) 0L else totalMs / perDay.size,
            busiest = perDay.entries.maxByOrNull { it.value }?.let { CareerDay(it.key, it.value) },
            previousMs = previousMs,
            oldestAtMs = oldest
        )
    }
}

/** نتيجة [CareerLedger.span]: فترة طويلة بمقاييسها الصادقة وحدودها. */
data class CareerSpan(
    val days: Int,
    val totalMs: Long,
    val activeDays: Int,
    val averageActiveDayMs: Long,
    val busiest: CareerDay?,
    val previousMs: Long,
    val oldestAtMs: Long?
)
