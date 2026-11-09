/*
 * Copyright (C) 2026-2027 Zexshia
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */

package nd.max.core.platform

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.roundToInt

/**
 * سجلّ جلسات اللعب: ما قِيس يُحفظ، وما لم يُقَس يبقى `null` لا صفرًا.
 *
 * الحلقة في الذاكرة ([HudJournal]) تسقط الأقدم عند السقف، فهي للمدى الأخير لا للأرشيف.
 * وهذه الطبقة تحفظ **ملخّصًا** لكل جلسة مع نقاط مصغّرة، فتبقى الجلسة مقروءة بعد موت
 * العملية وبعد أسبوع — وهو الفارق الذي يجعل السجلّ سجلًا لا عدّادًا حيًّا.
 */
@Serializable
data class SessionPoint(
    val atMs: Long,
    val frames: Float? = null,
    val cpu: Int? = null,
    val ramMb: Int? = null,
    val watt: Float? = null,
    val battery: Int? = null,
    val heat: Float? = null,
    val jankPercent: Int? = null,
    val frameP99Ms: Float? = null
)

/**
 * جلسة محفوظة. `id` مبنيّ على وقت البداية والحزمة، فإعادة الحفظ لا تُنشئ صفًّا ثانيًا.
 *
 * @param spanMs المدى المقيس من أوّل عيّنة إلى آخرها، لا من ساعة الحائط.
 * @param series نقاط مصغّرة ([MAX_SERIES_POINTS]) تكفي لرسم المنحنى ولا تُكبّر الملف.
 */
@Serializable
data class StoredGameSession(
    val id: String,
    val packageName: String? = null,
    val appLabel: String? = null,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val spanMs: Long = 0L,
    val samples: Int = 0,
    val droppedSamples: Int = 0,
    val framesAverage: Float? = null,
    val framesLow: Float? = null,
    val framesHigh: Float? = null,
    val heatPeak: Float? = null,
    val powerAverage: Float? = null,
    val energyWh: Float? = null,
    val jankAverage: Int? = null,
    val frameP99AverageMs: Float? = null,
    val batteryStart: Int? = null,
    val batteryEnd: Int? = null,
    val renderer: String? = null,
    val series: List<SessionPoint> = emptyList()
) {
    /** ما يُعرض في السطر: اسم اللعبة، ثم الحزمة، ثم «جلسة بلا اسم» — لا سطر فارغ. */
    val displayName: String
        get() = appLabel?.takeIf { it.isNotBlank() }
            ?: packageName?.takeIf { it.isNotBlank() }
            ?: "—"
}

/** سقف النقاط المحفوظة لكل جلسة: ١٨٠ نقطة تغطّي ساعة بفاصل ٢٠ ثانية. */
const val MAX_SERIES_POINTS = 180

/** أعمدة CSV الواحدة للجلسة الحيّة والمحفوظة — مصدر واحد، فلا ينفصل التصديران. */
const val SESSION_CSV_HEADER =
    "elapsed_ms,frames,cpu_percent,ram_mb,watt,battery_c,jank_percent,frame_p99_ms,renderer"

/**
 * تخزين جلسات اللعب في `filesDir/game-sessions.json`.
 *
 * الكتابة ذرّية (مؤقّت ثم إعادة تسمية) لأن الملف يُقرأ عند فتح الشاشة بينما قد تُوقَف
 * جلسة في الخلفية؛ فملفّ نصف مكتوب يُقرأ فشلًا لا سجلًّا ناقصًا.
 */
object GameSessionStore {

    const val FILE_NAME = "game-sessions.json"
    const val MAX_SESSIONS = 20

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val lock = Any()

    private fun file(context: Context): File =
        File(context.applicationContext.filesDir, FILE_NAME)

    fun load(context: Context): List<StoredGameSession> = synchronized(lock) {
        read(file(context))
    }

    /** يضيف جلسة ويُبقي الأحدث فقط؛ يعيد `true` إن استقرّت على القرص. */
    fun append(context: Context, session: StoredGameSession): Boolean = synchronized(lock) {
        val target = file(context)
        val merged = merge(read(target), session, MAX_SESSIONS)
        write(target, merged)
    }

    fun delete(context: Context, id: String): Boolean = synchronized(lock) {
        val target = file(context)
        val next = read(target).filterNot { it.id == id }
        write(target, next)
    }

    fun clear(context: Context): Boolean = synchronized(lock) {
        val target = file(context)
        runCatching { target.delete() }.getOrDefault(false) || !target.exists()
    }

    private fun read(target: File): List<StoredGameSession> {
        if (!target.isFile) return emptyList()
        return runCatching { json.decodeFromString<List<StoredGameSession>>(target.readText()) }
            .getOrDefault(emptyList())
    }

    private fun write(target: File, sessions: List<StoredGameSession>): Boolean = runCatching {
        val temp = File(target.parentFile, target.name + ".tmp")
        temp.writeText(json.encodeToString(sessions))
        if (!temp.renameTo(target)) {
            target.writeText(temp.readText())
            temp.delete()
        }
        true
    }.getOrDefault(false)
}

/**
 * الدمج: الأحدث أوّلًا، والمعرّف المكرّر يُستبدل لا يُضاعف، والسقف يُطبَّق بعد الفرز.
 * دالّة صافية لأن هذا هو القرار الذي يفسد السجلّ بصمت لو أُخطئ فيه.
 */
fun merge(
    existing: List<StoredGameSession>,
    incoming: StoredGameSession,
    max: Int = GameSessionStore.MAX_SESSIONS
): List<StoredGameSession> {
    if (max <= 0) return emptyList()
    return (existing.filterNot { it.id == incoming.id } + incoming)
        .sortedByDescending { it.startedAtMs }
        .take(max)
}

/**
 * طاقة الجلسة بالواط-ساعة: تكامل شبه منحرف على عيّنات الواط.
 *
 * الفجوة الأطول من [maxGapMs] لا تُملأ بالاستقراء — انقطاع التسجيل ليس استهلاكًا،
 * ومدّ خطّ بين طرفي فجوة يخترع رقمًا. وغياب زوج صالح يعني `null` لا صفرًا.
 */
fun sessionEnergyWh(samples: List<SessionPoint>, maxGapMs: Long = 10_000L): Float? {
    var wattHours = 0.0
    var counted = false
    var previous: SessionPoint? = null
    for (point in samples) {
        val before = previous
        val beforeWatt = before?.watt?.takeIf { it > 0f }
        val watt = point.watt?.takeIf { it > 0f }
        if (before != null && beforeWatt != null && watt != null) {
            val delta = point.atMs - before.atMs
            if (delta in 1..maxGapMs) {
                wattHours += (beforeWatt + watt) / 2.0 * delta / 3_600_000.0
                counted = true
            }
        }
        previous = point
    }
    return if (counted) wattHours.toFloat() else null
}

/** متوسط jank% — عدد صحيح كما يُقرأ في اللوحة، و`null` إن لم تُقَس ولا مرّة. */
fun sessionJankAverage(samples: List<SessionPoint>): Int? {
    val values = samples.mapNotNull { it.jankPercent }
    if (values.isEmpty()) return null
    return values.average().roundToInt()
}

/** متوسط p99 لزمن الإطار بالمللي ثانية، و`null` إن لم تُقَس. */
fun sessionP99Average(samples: List<SessionPoint>): Float? {
    val values = samples.mapNotNull { it.frameP99Ms?.takeIf { ms -> ms > 0f } }
    if (values.isEmpty()) return null
    return values.average().toFloat()
}

/**
 * تصغير متساوٍ يحتفظ بالطرفين: أوّل نقطة وآخر نقطة موجودتان دائمًا، والفاصل متساوٍ.
 * لا متوسطات متحرّكة — القيمة المعروضة يجب أن تكون قراءة وقعت فعلًا.
 */
fun downsampleSamples(samples: List<SessionPoint>, maxPoints: Int = MAX_SERIES_POINTS): List<SessionPoint> {
    if (maxPoints <= 0) return emptyList()
    if (samples.size <= maxPoints) return samples
    if (maxPoints == 1) return listOf(samples.last())
    val step = (samples.size - 1).toDouble() / (maxPoints - 1)
    return (0 until maxPoints).map { index -> samples[(index * step).roundToInt()] }
}

/** CSV الجلسة المحفوظة — نفس أعمدة الحيّة، والغائب خليّة فارغة. */
fun storedSessionCsv(session: StoredGameSession): String {
    val out = StringBuilder(SESSION_CSV_HEADER.length + 16 + session.series.size * 48)
    out.append(SESSION_CSV_HEADER).append('\n')
    for (point in session.series) {
        out.append(point.atMs - session.startedAtMs).append(',')
        out.append(point.frames.orEmpty()).append(',')
        out.append(point.cpu.orEmpty()).append(',')
        out.append(point.ramMb.orEmpty()).append(',')
        out.append(point.watt.orEmpty()).append(',')
        out.append(point.battery.orEmpty()).append(',')
        out.append(point.jankPercent.orEmpty()).append(',')
        out.append(point.frameP99Ms.orEmpty()).append(',')
        out.append(session.renderer.orEmpty())
        out.append('\n')
    }
    return out.toString()
}

private fun Float?.orEmpty(): String = this?.toString() ?: ""
private fun Int?.orEmpty(): String = this?.toString() ?: ""
