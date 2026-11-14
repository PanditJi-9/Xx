/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.input

import android.content.Context
import android.os.SystemClock
import nd.max.AppMonitorLogger
import nd.max.core.privilege.PrivilegeManager
import nd.max.core.privilege.PrivilegedShell

/**
 * الواجهة الوحيدة لحقن اللمس في التطبيق.
 *
 * ### ترتيب المسارين
 *
 * ١) **خدمة الوصول** إن كانت متّصلة: لا تحتاج جذرًا، وتُعطي نتيجة موثّقة من Android.
 * ٢) **`input tap/swipe` عبر [PrivilegedShell]** (جذر أو Shizuku) إن لم تكن الخدمة مفعّلة.
 * ٣) وإلّا: [InjectionRefusal.NO_LANE] صريحًا — لا صمت ولا «تمّ».
 *
 * ### الحارس
 *
 * قبل كل حقنة يُقرأ نشاط الإصبع ([FingerProbe])؛ فإن كان نازلًا تُرفض الحقنة بدل أن تتسابق
 * معه. وبلا جذر تكون القراءة `UNKNOWN`، والميزة تعمل مع إعلان أن الحارس غير متاح — وهو حدّ
 * مقيس في [FingerProbe]، لا نجاح مزعوم.
 *
 * **ويُستدعى من خيط خلفيّ** (`Dispatchers.IO`)؛ كل مسار داخله يعرف خيطه.
 */
object TouchInjector {

    @Volatile
    private var lastInjectedAtMs = 0L

    /** آخر قراءة إصبع — تعرضها الواجهة لتعرف أسوأ حالة بدل تخمينها. */
    @Volatile
    var lastFinger: FingerProbeResult = FingerProbeResult(FingerReading.UNKNOWN, "not-probed")
        private set

    /** هل قرأ الحارس إصبعًا حقيقيًّا يومًا؟ تُبنى عليه صياغة الواجهة («حارس متاح» / «غير متاح»). */
    @Volatile
    var guardEverActive: Boolean = false
        private set

    suspend fun tap(context: Context, x: Float, y: Float): InjectionOutcome =
        inject(context, x, y, x, y, TAP_DURATION_MS)

    suspend fun swipe(
        context: Context,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        durationMs: Int,
    ): InjectionOutcome = inject(context, x1, y1, x2, y2, TouchInjectionRules.swipeDuration(durationMs))

    /**
     * تمرير لمسة **مسجِّل الماكرو** إلى اللعبة — الحارس يُتجاوَز هنا عن قصد.
     *
     * السبب: الإصبع الذي يرسم الخطوة هو نفسه الذي يجب أن تصل لمسته إلى اللعبة؛ فقراءة الحارس
     * سترى `DOWN` وترفض كل خطوة، فيصير المسجِّل حاجزًا لا ناقلًا. وهذا **الموضع الوحيد** الذي
     * يُتجاوَز فيه الحارس، ويُعلن في واجهة الماكرو؛ ولا يعني أن الحقن صار بلا حارس في بقية المسارات.
     */
    suspend fun passThrough(
        context: Context,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        durationMs: Int,
    ): InjectionOutcome = inject(
        context, x1, y1, x2, y2,
        TouchInjectionRules.swipeDuration(durationMs),
        guard = false,
    )

    private suspend fun inject(
        context: Context,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        durationMs: Int,
        guard: Boolean = true,
    ): InjectionOutcome {
        val metrics = context.resources.displayMetrics
        val startX = TouchInjectionRules.clampX(x1, metrics.widthPixels)
        val startY = TouchInjectionRules.clampY(y1, metrics.heightPixels)
        val endX = TouchInjectionRules.clampX(x2, metrics.widthPixels)
        val endY = TouchInjectionRules.clampY(y2, metrics.heightPixels)
        if (!TouchInjectionRules.inBounds(x1, y1, metrics.widthPixels, metrics.heightPixels)) {
            return InjectionOutcome.Refused(InjectionRefusal.OUT_OF_BOUNDS)
        }

        val now = SystemClock.elapsedRealtime()
        // لا يُسأل الجذر من أجل الحارس وحده: صلاحية غير متاحة الآن = قراءة مجهولة، لا طلب إذن.
        val canProbe = PrivilegeManager.snapshot.value.let { it.rootGranted || it.shizuku.ready }
        val finger = if (guard && canProbe) {
            FingerProbe.read()
        } else {
            FingerProbeResult(FingerReading.UNKNOWN, if (guard) "no-privilege" else "guard-bypassed")
        }
        lastFinger = finger
        if (finger.reading != FingerReading.UNKNOWN) guardEverActive = true
        TouchInjectionRules.refusal(now, lastInjectedAtMs, finger.reading)?.let { reason ->
            record("EVENT=TOUCH_INJECTION_REFUSED reason=${reason.name.lowercase()} finger=${finger.reading.name.lowercase()}")
            return InjectionOutcome.Refused(reason)
        }

        lastInjectedAtMs = now
        val outcome = if (A11yGestureInjector.available) {
            A11yGestureInjector.swipe(startX, startY, endX, endY, durationMs)
        } else {
            shell(startX, startY, endX, endY, durationMs)
        }
        recordOutcome(outcome)
        return outcome
    }

    private suspend fun shell(
        x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Int,
    ): InjectionOutcome {
        val pickup = (x1 - x2).let { it * it } + (y1 - y2).let { it * it }
        val command = if (pickup < TAP_SLOP_PX_SQUARED) {
            "input tap ${x1.toInt()} ${y1.toInt()}"
        } else {
            "input swipe ${x1.toInt()} ${y1.toInt()} ${x2.toInt()} ${y2.toInt()} $durationMs"
        }
        return when (val outcome = PrivilegedShell.exec(command)) {
            is PrivilegedShell.Outcome.Ran ->
                if (outcome.ok) InjectionOutcome.Delivered(InjectionLane.PRIVILEGED_SHELL)
                else InjectionOutcome.Failed(InjectionLane.PRIVILEGED_SHELL, "exit-${outcome.code}")

            is PrivilegedShell.Outcome.Refused -> InjectionOutcome.Refused(InjectionRefusal.NO_LANE)
            is PrivilegedShell.Outcome.Failed -> InjectionOutcome.Failed(InjectionLane.PRIVILEGED_SHELL, outcome.reason)
        }
    }

    private fun recordOutcome(outcome: InjectionOutcome) {
        when (outcome) {
            is InjectionOutcome.Failed -> record("EVENT=TOUCH_INJECTION_FAILED lane=${outcome.lane.name.lowercase()} detail=${outcome.detail}")
            is InjectionOutcome.Refused -> record("EVENT=TOUCH_INJECTION_REFUSED reason=${outcome.reason.name.lowercase()}")
            is InjectionOutcome.Delivered -> Unit
        }
    }

    private fun record(line: String) {
        runCatching { AppMonitorLogger.persist(line) }
    }

    private const val TAP_DURATION_MS = 60
    private const val TAP_SLOP_PX_SQUARED = 4f
}
