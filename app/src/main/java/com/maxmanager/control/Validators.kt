package com.maxmanager.control

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Pure input rules (no Android classes) so they can be unit-tested on the JVM. */
object Validators {
    private val VERSION_RE = Regex("""\d+(\.\d+){0,3}""")

    fun isVersion(v: String): Boolean = VERSION_RE.matches(v)

    /** Numeric, segment by segment: 2.10 > 2.9, and 2.1 == 2.1.0. */
    fun compareVersions(a: String, b: String): Int {
        val x = a.split('.').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val c = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }

    /** Strict yyyy-MM-dd and a real calendar day (rejects 2026-02-30). */
    fun isDate(s: String): Boolean = runCatching { LocalDate.parse(s) }.isSuccess

    fun isHttpsUrl(s: String): Boolean =
        s.startsWith("https://") && s.length > "https://".length && s.none { it.isWhitespace() }

    fun noteLines(text: String): List<String> =
        text.lines().map { it.trim() }.filter { it.isNotEmpty() }

    // ── حدود إنشاء المفاتيح ────────────────────────────────────────────────────────────────────
    // الأرقام هي أرقام الخادم نفسه (`maxctl/index.ts`: MAX_COUNT=200 · MAX_DURATION_DAYS=3650 ·
    // MAX_DEVICE_LIMIT=50)، و`intIn` هناك يقبل الطرفين ويستبدل الخارج بالافتراضي **صامتًا**
    // (30/1/1) — فيظنّ المالك أنه أنشأ ما كتب وهو لم يفعل. فاللوحة تتحقّق قبل النداء وتقول السبب.

    const val MAX_KEY_COUNT = 200
    const val MAX_KEY_DAYS = 3650
    const val MAX_KEY_DEVICES = 50

    /** مشكلة صياغة إنشاء المفاتيح، أو `null` إن كان النداء سيُنشئ ما كُتب تمامًا. */
    fun keyDraftProblem(days: Int, deviceLimit: Int, count: Int): String? = when {
        count !in 1..MAX_KEY_COUNT -> "العدد يجب أن يكون بين 1 و$MAX_KEY_COUNT"
        days !in 1..MAX_KEY_DAYS -> "المدّة بالأيام يجب أن تكون بين 1 و$MAX_KEY_DAYS"
        deviceLimit !in 1..MAX_KEY_DEVICES -> "حدّ الأجهزة يجب أن يكون بين 1 و$MAX_KEY_DEVICES"
        else -> null
    }

    // ── لحظات نوافذ المجّاني ────────────────────────────────────────────────────────────────────
    // العقد الذي يقرؤه التطبيق (`RemoteConfigs.instantMillis`) يقبل لحظة ISO-8601 كاملة،
    // والخادم (`Date.parse`) يقبل التاريخ وحده أيضًا. فاللوحة تكتب **لحظة كاملة دائمًا** ولا
    // تتّكل على تسامح أحدهما: صيغة واحدة على السلك = صفر انزياح بين الطرفين.

    /** هل النصّ تاريخ يوم أو لحظة ISO كاملة؟ */
    fun isMoment(s: String): Boolean {
        val t = s.trim()
        return isDate(t) || runCatching { Instant.parse(t) }.isSuccess
    }

    /** لحظة البداية: التاريخ ⇒ منتصف ليل UTC، واللحظة الكاملة ⇒ كما هي. */
    fun momentStart(s: String): String {
        val t = s.trim()
        return if (isDate(t)) "${t}T00:00:00Z" else t
    }

    /**
     * لحظة النهاية **غير المشمولة**: التاريخ ⇒ بداية اليوم التالي له.
     *
     * ونصف مفتوحة لأن التطبيق يقيس `[from, to)`: اليوم الأخير الذي يكتبه المالك يجب أن يقع
     * **داخل** النافذة، فتُزاح النهاية يومًا. ولو كُتبت كما هي لَكُسب يومٌ في الوجهين.
     */
    fun momentEndExclusive(s: String): String {
        val t = s.trim()
        return if (isDate(t)) LocalDate.parse(t).plusDays(1).toString() + "T00:00:00Z" else t
    }

    /** عرض لحظة للتعديل: منتصف ليل UTC يُعرض تاريخًا (ما كتبه المالك)، وغيره كما هو. */
    fun momentDisplay(s: String): String {
        val t = s.trim()
        val instant = runCatching { Instant.parse(t) }.getOrNull() ?: return t
        val utc = instant.atOffset(ZoneOffset.UTC)
        return if (utc.toLocalTime() == LocalTime.MIDNIGHT) utc.toLocalDate().toString() else t
    }

    /** عرض **نهاية** النافذة: منتصف الليل يعني «بداية اليوم التالي» ⇒ يُعرض يومًا سابقًا. */
    fun momentEndDisplay(s: String): String {
        val t = s.trim()
        val instant = runCatching { Instant.parse(t) }.getOrNull() ?: return t
        val utc = instant.atOffset(ZoneOffset.UTC)
        if (utc.toLocalTime() != LocalTime.MIDNIGHT) return t
        return utc.toLocalDate().minusDays(1).toString()
    }

    /** ترتيب النافذة: تُقارن اللحظتان لا النصّان (صيغة `+02:00` تُفسد المقارنة النصّية). */
    fun isWindowOrdered(from: String, to: String): Boolean {
        val a = runCatching { Instant.parse(momentStart(from)).toEpochMilli() }.getOrNull() ?: return true
        val b = runCatching { Instant.parse(momentEndExclusive(to)).toEpochMilli() }.getOrNull() ?: return true
        return a < b
    }
}
