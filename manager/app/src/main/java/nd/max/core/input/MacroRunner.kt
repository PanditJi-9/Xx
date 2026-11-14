/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.input

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * إعادة تشغيل الماكرو — بيد المستخدم، لا في الخلفية.
 *
 * كل خطوة تُحوَّل إلى لمسة عبر [TouchInjector]، وتُحترم فواصلها المخزَّنة. وإن رُفضت خطوة
 * (إصبع على الشاشة أو لا مسار) **يتوقّف التشغيل ويُعلن السبب** بدل أن يكمل بتسلسل مكسور
 * يُفسد ما تفعله اللعبة.
 */
object MacroRunner {

    data class Result(
        val stepsRun: Int,
        val stopped: Boolean,
        val detail: String? = null,
    )

    suspend fun play(context: Context, steps: List<MacroStep>): Result {
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()
        var run = 0
        for (step in MacroMath.sanitize(steps)) {
            delay(step.delayMs)
            val duration = if (step.kind == MacroStepKind.TAP) {
                GameExtrasMath.TAP_MS
            } else {
                GameExtrasMath.SWIPE_MS
            }
            val outcome = TouchInjector.swipe(
                context,
                step.x1 * width, step.y1 * height,
                step.x2 * width, step.y2 * height,
                duration,
            )
            when (outcome) {
                is InjectionOutcome.Delivered -> run += 1
                is InjectionOutcome.Refused -> return Result(run, true, outcome.reason.name)
                is InjectionOutcome.Failed -> return Result(run, true, outcome.detail)
            }
        }
        return Result(run, false, null)
    }
}

/**
 * يد ← لمس: اقتراب اليد من الشاشة يُرسل لمسة واحدة في مركزها.
 *
 * *شرطه:* لعبة متتبَّعة، وإقرار الحقن، وحسّاس قرب يعلنه الجهاز. والغياب يُعلن (`available=false`)
 * ولا يُعطّل شيئًا آخر. واللمسة في المركز لأن الحسّاس لا يعرف **أين** اليد، بل **أنها اقتربت**؛
 * فادعاء تتبّعها كذبٌ لا ميزة.
 */
class HandTouchController(context: Context) : SensorEventListener {

    private val appContext = context.applicationContext
    private val sensors = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val proximity: Sensor? = sensors?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var running = false
    private var wasNear = false
    private var lastTapAtMs = 0L

    val available: Boolean get() = proximity != null

    fun start() {
        val sensor = proximity ?: return
        if (running) return
        running = true
        wasNear = false
        lastTapAtMs = 0L
        runCatching { sensors?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL, handler) }
    }

    fun stop() {
        if (!running) return
        running = false
        runCatching { sensors?.unregisterListener(this) }
    }

    fun destroy() {
        stop()
        scope.cancel()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val sample = event ?: return
        if (sample.sensor.type != Sensor.TYPE_PROXIMITY) return
        if (!running) return
        val value = sample.values.firstOrNull() ?: return
        val near = HandTouchMath.isNear(value, sample.sensor.maximumRange)
        val now = SystemClock.elapsedRealtime()
        val fire = HandTouchMath.shouldTap(wasNear, near, if (lastTapAtMs == 0L) Long.MAX_VALUE else now - lastTapAtMs)
        wasNear = near
        if (!fire) return
        lastTapAtMs = now
        val metrics = appContext.resources.displayMetrics
        val x = metrics.widthPixels / 2f
        val y = metrics.heightPixels / 2f
        scope.launch { runCatching { TouchInjector.tap(appContext, x, y) } }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
