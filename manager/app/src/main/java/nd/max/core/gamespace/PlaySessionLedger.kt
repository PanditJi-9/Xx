/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import nd.max.core.platform.ForegroundAppResolver

/**
 * جلسة لعب مكتملة — **وقت الجدار** لا وقت تشغيل الخدمة، فتُحفظ وتُقرأ بعد إعادة التشغيل.
 *
 * والحدّ المعلن: هذه جلسة **شوهدت** بواسطة خدمة اللوحة الجانبية. ليست سجلًّا لكل تطبيق على
 * الجهاز، ولا تُقاس بلا خدمة حيّة.
 */
data class PlaySession(
    val packageName: String,
    val startedAtMs: Long,
    val endedAtMs: Long,
) {
    val durationMs: Long get() = (endedAtMs - startedAtMs).coerceAtLeast(0L)
}

/** إجمالي وقت لعبة واحدة، مشتقًّا من الجلسات المحفوظة. */
data class GamePlayStats(
    val packageName: String,
    val lastPlayedAtMs: Long,
    val totalMs: Long,
    val sessionCount: Int,
)

/**
 * عمر آخر لعب كفئة نقيّة، والواجهة وحدها تترجمها إلى نصّ (فتُترجم إلى العربية بلا منطق مكرّر).
 * `None` تعني «لا جلسة مسجّلة»، لا «الآن».
 */
sealed interface PlayAge {
    object None : PlayAge
    data class Minutes(val minutes: Int) : PlayAge
    data class Hours(val hours: Int) : PlayAge
    object Yesterday : PlayAge
    data class Days(val days: Int) : PlayAge
}

/**
 * سجلّ جلسات اللعب — **نقيّ بالكامل**: لا أندرويد ولا ملفّات ولا وقت حقيقي، فيُقاس على JVM.
 *
 * **ليس `GameSessionStore`:** ذلك أرشيف تقارير FPS يبدأه المستخدم (٢٠ جلسة غنيّة بالقياسات)،
 * وهذا سجلّ وقت لعب تلقائي (٤٠٠ صفّ خفيف) يقرأه اللوبي. لا تُدمج السلسلتان، ولا يُكتب هنا أي
 * قياس أداء لم يُقَس. كلٌّ منهما يجيب سؤالًا مختلفًا، ولا تعرض شاشة واحدة الرقمين كأنهما واحد.
 *
 * ### القواعد
 * - الجلسة الصالحة: اسم حزمة صالح الصيغة، بداية موجبة، نهاية بعدها، ومدة بين ٥ ثوانٍ و١٢ ساعة.
 * - السقف [MAX_SESSIONS] يبقي الأحدث؛ فالسجلّ لا يكبر بلا حدّ.
 * - الملفّ التالف **لا يُسقط التطبيق**: السطور غير الصالحة تُتخطّى، وما لم يبقَ صالح يُقرأ فراغًا.
 * - المجهول لا يُختلق: غياب الجلسة يعني `None`، ولا تعرض الواجهة صفرًا.
 */
object PlaySessionLedger {
    const val MAX_SESSIONS = 400
    const val MIN_DURATION_MS = 5_000L
    const val MAX_DURATION_MS = 12L * 60L * 60L * 1_000L

    private const val HEADER = "v1"
    private const val SEP = "\t"

    fun valid(session: PlaySession): Boolean =
        ForegroundAppResolver.isPackageName(session.packageName) &&
            session.startedAtMs > 0L &&
            session.endedAtMs > session.startedAtMs &&
            session.durationMs in MIN_DURATION_MS..MAX_DURATION_MS

    /** يضيف جلسة صالحة ويقصّ الأقدم؛ والجلسة غير الصالحة لا تغيّر شيئًا. */
    fun append(
        existing: List<PlaySession>,
        session: PlaySession,
        limit: Int = MAX_SESSIONS,
    ): List<PlaySession> {
        if (!valid(session) || session in existing) return existing
        val safeLimit = limit.coerceIn(1, MAX_SESSIONS)
        return (existing + session).sortedBy { it.endedAtMs }.takeLast(safeLimit)
    }

    /** يقرأ عقد `pkg<TAB>start<TAB>end`؛ السطر التالف يُتخطّى ولا يُسقط الباقي. */
    fun parse(text: String?): List<PlaySession> {
        if (text.isNullOrBlank()) return emptyList()
        val parsed = ArrayList<PlaySession>()
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed == HEADER) continue
            val parts = trimmed.split(SEP)
            if (parts.size != 3) continue
            val started = parts[1].trim().toLongOrNull() ?: continue
            val ended = parts[2].trim().toLongOrNull() ?: continue
            val session = PlaySession(parts[0].trim(), started, ended)
            if (valid(session)) parsed += session
        }
        return parsed.sortedBy { it.endedAtMs }.takeLast(MAX_SESSIONS)
    }

    fun encode(sessions: List<PlaySession>): String = buildString {
        append(HEADER).append('\n')
        for (session in sessions.filter(::valid).sortedBy { it.endedAtMs }.takeLast(MAX_SESSIONS)) {
            append(session.packageName).append(SEP)
                .append(session.startedAtMs).append(SEP)
                .append(session.endedAtMs).append('\n')
        }
    }

    /** تجميع لكل لعبة: آخر لعب، مجموع المدة، وعدد الجلسات. */
    fun stats(sessions: List<PlaySession>): Map<String, GamePlayStats> =
        sessions.filter(::valid).groupBy { it.packageName }.mapValues { (pkg, list) ->
            GamePlayStats(
                packageName = pkg,
                lastPlayedAtMs = list.maxOf { it.endedAtMs },
                totalMs = list.sumOf { it.durationMs },
                sessionCount = list.size,
            )
        }

    /** فئة العمر؛ المستقبل (ساعة الجهاز انحرفت) يُقرأ مجهولًا لا «الآن». */
    fun playAge(nowMs: Long, lastPlayedAtMs: Long?): PlayAge {
        if (lastPlayedAtMs == null || lastPlayedAtMs <= 0L || lastPlayedAtMs > nowMs) return PlayAge.None
        val ageMs = nowMs - lastPlayedAtMs
        val minutes = ageMs / 60_000L
        val hours = ageMs / 3_600_000L
        return when {
            minutes < 60L -> PlayAge.Minutes(minutes.coerceAtLeast(1L).toInt())
            hours < 24L -> PlayAge.Hours(hours.toInt())
            hours < 48L -> PlayAge.Yesterday
            else -> PlayAge.Days((hours / 24L).toInt())
        }
    }

    /** دقائق اللعب للعرض؛ `null` لما دون ٥ ثوانٍ فلا يُعرض «0 دقيقة». */
    fun playMinutes(totalMs: Long): Int? =
        if (totalMs < MIN_DURATION_MS) null
        else (totalMs / 60_000L).coerceAtLeast(1L).toInt()
}

/**
 * يتتبّع انتقال اللعبة الأمامية إلى جلسة مكتملة — **نقيّ** ومُختبر.
 *
 * - لعبة جديدة تُغلق السابقة وتبدأ الجديدة.
 * - تطبيق آخر يُغلق الجلسة.
 * - قراءة غير مقروءة (`Unknown`) **لا تُغلق** الجلسة: قراءة فاشلة ليست دليل خروج.
 * - الشاشة المطفأة تُغلق الجلسة صراحةً من الخدمة (`interactive == false`).
 */
class PlaySessionTracker {
    private var packageName: String? = null
    private var startedAtMs = 0L

    @Synchronized
    fun onForeground(pkg: String?, nowMs: Long): PlaySession? {
        if (pkg == packageName) return null
        val finished = close(nowMs)
        if (pkg != null) {
            packageName = pkg
            startedAtMs = nowMs
        }
        return finished
    }

    @Synchronized
    fun close(nowMs: Long): PlaySession? {
        val pkg = packageName ?: return null
        val start = startedAtMs
        packageName = null
        startedAtMs = 0L
        if (pkg.isBlank() || start <= 0L || nowMs <= start) return null
        return PlaySession(pkg, start, nowMs)
    }

    @Synchronized
    fun currentPackage(): String? = packageName
}
