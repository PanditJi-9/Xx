/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.input

import android.os.SystemClock
import nd.max.AppMonitorLogger
import nd.max.core.privilege.PrivilegedShell

/**
 * قراءة **نشاط** الإصبع الحقيقيّ من عقدة اللمس، لحماية الحقن من أن يتسابق مع إصبع المستخدم.
 *
 * ### ما يقرؤه فعلًا
 *
 * `getevent` عبر [PrivilegedShell] على عقدة اللمس: `BTN_TOUCH DOWN` أو `ABS_MT_TRACKING_ID`
 * غير `ffffffff` ⇒ إصبع نازل؛ `BTN_TOUCH UP` أو `TRACKING_ID = ffffffff` ⇒ رُفع. ويُقرأ
 * **آخر ١٢٠ ملّي ثانية فقط**؛ فالإصبع الساكن تمامًا قد لا يُولّد حدثًا فيُقرأ `UP`، وهذا حدّ
 * مقيس يُعلن ولا يُخفى. ومن هنا الاسم: *نشاط* الإصبع لا حالته المطلقة.
 *
 * ### ومتى يكون المجهول مجهولًا
 *
 * بلا جذر/Shizuku لا تُقرأ `/dev/input` ⇒ [FingerReading.UNKNOWN]، ولا تُخمَّن «لا إصبع».
 * والعقدة تُكتشف مرة واحدة وتُحفظ؛ وفشل الاكتشاف يُعاد بعد دقيقة بدل أن يُثقل كل حقنة.
 */
object FingerProbe {

    private const val SAMPLE_WINDOW = "0.12"
    private const val MAX_EVENTS = "12"
    private const val NODE_RETRY_MS = 60_000L

    private val nodeLock = Any()
    private var touchNode: String? = null
    private var nodeCheckedAt = 0L

    /** قراءة واحدة — تُنادى من مسار الحقن قبل كل لمسة. */
    suspend fun read(): FingerProbeResult {
        val node = resolveNode() ?: return FingerProbeResult(FingerReading.UNKNOWN, "no-touch-node")
        val output = when (val outcome = PrivilegedShell.exec(
            "timeout $SAMPLE_WINDOW getevent -q -c $MAX_EVENTS $node 2>/dev/null"
        )) {
            is PrivilegedShell.Outcome.Ran -> outcome.output
            is PrivilegedShell.Outcome.Refused -> return FingerProbeResult(FingerReading.UNKNOWN, "probe-refused")
            is PrivilegedShell.Outcome.Failed -> return FingerProbeResult(FingerReading.UNKNOWN, "probe-failed")
        }
        val reading = interpret(output)
        if (reading == FingerReading.UNKNOWN) {
            // قراءة فارغة تعني «لا حدث في النافذة»: لا إصبع **نشِط**، وهو قرار مقصود لا خطأ.
            return FingerProbeResult(FingerReading.UP, "quiet-window")
        }
        return FingerProbeResult(reading, null)
    }

    /** تفسير مخرَج `getevent` — نقيّ كي يُختبر بلا جهاز. */
    fun interpret(output: String): FingerReading {
        var down = false
        var up = false
        for (raw in output.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("add device") || line.startsWith("name:")) continue
            val tokens = line.split(Regex("\\s+"))
            if (tokens.size < 4) continue
            val type = tokens[tokens.size - 3].lowercase()
            val code = tokens[tokens.size - 2].lowercase()
            val value = tokens.last().lowercase()
            when {
                type == "0001" && code == "014a" -> if (value == "00000001") down = true else up = true
                type == "0003" && code == "0039" -> if (value == "ffffffff") up = true else down = true
            }
        }
        return when {
            down -> FingerReading.DOWN
            up -> FingerReading.UP
            else -> FingerReading.UNKNOWN
        }
    }

    private suspend fun resolveNode(): String? {
        synchronized(nodeLock) {
            val cached = touchNode
            if (cached != null) return cached
            if (SystemClock.elapsedRealtime() - nodeCheckedAt < NODE_RETRY_MS) return null
        }
        val outcome = PrivilegedShell.exec(
            "for f in /dev/input/event*; do " +
                "if timeout 0.25 getevent -i \"\$f\" 2>/dev/null | grep -m1 'name:' | grep -qi 'touch'; " +
                "then echo \"\$f\"; break; fi; done"
        )
        val found = (outcome as? PrivilegedShell.Outcome.Ran)
            ?.takeIf { it.ok }
            ?.output
            ?.lineSequence()
            ?.map { it.trim() }
            ?.firstOrNull { NODE_PATTERN.matches(it) }
        synchronized(nodeLock) {
            nodeCheckedAt = SystemClock.elapsedRealtime()
            touchNode = found
        }
        if (found == null) AppMonitorLogger.persist("EVENT=FINGER_PROBE_NO_NODE")
        return found
    }

    /** لا يُقبل من الصدفة إلا مسار عقدة نظاميّة — فلا يُبنى أمر من نصّ خارجيّ. */
    private val NODE_PATTERN = Regex("^/dev/input/event[0-9]+$")
}
