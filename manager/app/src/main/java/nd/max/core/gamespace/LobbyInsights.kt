/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

/**
 * مرشِّح عرض المكتبة في «استكشف»: الكل، المفضّلة، ما لُعب هذا الأسبوع، وما لُعب يومًا.
 *
 * المرشِّح **عرضٌ لا ملكية**: لا يكتب شيئًا ولا يخفي لعبة من الحلقة؛ الحلقة تبقى مرتّبة بحسب
 * `LobbyOrder`، وهذا يقصر نتائج البحث فقط.
 */
enum class LobbyFilter { All, Favorites, Recent, Played, Unplayed }

/** ملخّص مسيرة المكتبة، مشتقٌّ من سجلّ الجلسات القائم لا من مخزن ثانٍ. */
data class LibrarySummary(
    val totalMs: Long,
    val sessionCount: Int,
    val gamesPlayed: Int,
    val neverPlayed: Int,
    val top: List<GamePlayStats>,
)

/** بنود تهيئة اللعبة قبل الجولة. أربعة، وكلها من حالة قائمة فعلًا. */
enum class SetupKind { Profile, Panel, History, Recent }

/** بند تهيئة واحد: نوعه وهل تحقّق. */
data class SetupStep(val kind: SetupKind, val done: Boolean)

/**
 * مشتقّات «الرؤى» و«الاستكشاف» — **نقيّة بالكامل**: لا أندرويد ولا وقت حقيقي، فتُقاس على JVM.
 *
 * ### القواعد
 * - المسيرة تُحسب لألعاب المكتبة وحدها؛ جلسة لعبة أُزيلت من المكتبة لا تُخلط في الأرقام.
 * - «لُعبت» تعني `sessionCount > 0` فقط؛ ولا صفر يُعرض عن لعبة لم تُرصد.
 * - «هذا الأسبوع» نافذة سبعة أيام شاملة الطرفين، والمستقبل (ساعة منحرفة) لا يُقرأ حديثًا.
 * - البحث لا يغيّر الترتيب الوارد، ولا يفرّق بين حالة الأحرف، ويطابق الاسم والحزمة معًا.
 */
object LobbyInsights {
    const val RECENT_WINDOW_MS = 7L * 24L * 60L * 60L * 1_000L
    const val TOP_COUNT = 3
    const val SETUP_TOTAL = 4

    /** ملخّص المكتبة: مجموع الوقت، الجلسات، ما لُعب وما لم يُلعب، وأكثر ثلاث ألعاب. */
    fun summary(stats: Map<String, GamePlayStats>, library: Set<String>): LibrarySummary {
        val played = stats.values.filter { it.packageName in library && it.sessionCount > 0 && it.totalMs > 0L }
        return LibrarySummary(
            totalMs = played.sumOf { it.totalMs },
            sessionCount = played.sumOf { it.sessionCount },
            gamesPlayed = played.size,
            neverPlayed = (library.size - played.size).coerceAtLeast(0),
            top = played
                .sortedWith(compareByDescending<GamePlayStats> { it.totalMs }.thenBy { it.packageName })
                .take(TOP_COUNT),
        )
    }

    /** بنود التهيئة الأربعة بترتيب العرض: الملف، اللوحة، السجل، آخر أسبوع. */
    fun setup(
        stats: GamePlayStats?,
        tweaks: Int,
        panelArmed: Boolean,
        nowMs: Long,
    ): List<SetupStep> = listOf(
        SetupStep(SetupKind.Profile, tweaks > 0),
        SetupStep(SetupKind.Panel, panelArmed),
        SetupStep(SetupKind.History, stats != null && stats.sessionCount > 0),
        SetupStep(SetupKind.Recent, isRecent(stats, nowMs)),
    )

    /** كم بندًا تحقّق من أربعة؛ `null` يعني لا لعبة مختارة فلا تُعرض نسبة. */
    fun readyCount(steps: List<SetupStep>): Int = steps.count { it.done }

    /**
     * تصفية قائمة اللعب بالمرشِّح والنصّ معًا، مع الحفاظ على الترتيب الوارد.
     *
     * @param query نصّ البحث؛ الفراغ يعرض الكل. يطابق الاسم والحزمة بلا حساسية لحالة الأحرف.
     */
    fun filter(
        games: List<GameApp>,
        filter: LobbyFilter,
        query: String,
        favorites: Set<String>,
        stats: Map<String, GamePlayStats>,
        nowMs: Long,
    ): List<GameApp> {
        val needle = query.trim()
        return games.filter { game ->
            val matchesText = needle.isEmpty() ||
                game.label.contains(needle, ignoreCase = true) ||
                game.packageName.contains(needle, ignoreCase = true)
            if (!matchesText) return@filter false
            when (filter) {
                LobbyFilter.All -> true
                LobbyFilter.Favorites -> game.packageName in favorites
                LobbyFilter.Played -> (stats[game.packageName]?.sessionCount ?: 0) > 0
                LobbyFilter.Unplayed -> (stats[game.packageName]?.sessionCount ?: 0) <= 0
                LobbyFilter.Recent -> isRecent(stats[game.packageName], nowMs)
            }
        }
    }

    /** عدد كل مرشِّح على القائمة نفسها؛ يُعرض في الشرائح قبل التصفية النصّية. */
    fun counts(
        games: List<GameApp>,
        favorites: Set<String>,
        stats: Map<String, GamePlayStats>,
        nowMs: Long,
    ): Map<LobbyFilter, Int> = LobbyFilter.entries.associateWith { filter ->
        filter(games, filter, "", favorites, stats, nowMs).size
    }

    /** هل آخر جلسة داخل نافذة الأسبوع؟ المستقبل والمجهول ليسا «حديثًا». */
    fun isRecent(stats: GamePlayStats?, nowMs: Long): Boolean {
        if (stats == null || stats.sessionCount <= 0) return false
        val last = stats.lastPlayedAtMs
        if (last <= 0L || last > nowMs) return false
        return nowMs - last <= RECENT_WINDOW_MS
    }
}
