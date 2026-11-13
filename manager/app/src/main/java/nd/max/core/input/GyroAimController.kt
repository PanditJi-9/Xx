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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * مساعد التصويب الجيروسكوبيّ: يقرأ حسّاس الدوران ويُحوّل حركته إلى سحبات صغيرة في مركز
 * الشاشة عبر [TouchInjector].
 *
 * ### شروطه
 *
 * لا يعمل إلا واللعبة متتبَّعة، والميزة مفعّلة، والإقرار مأخوذ، ومسار حقن متاح. وكل شرط
 * يُفحص قبل البدء في الخدمة ([nd.max.service.GamePanelService])، لا هنا.
 *
 * ### حدوده
 *
 * - يحتاج جهازًا فيه جيروسكوب؛ الغياب يُعلن (`available=false`) ولا يُعطّل شيئًا آخر.
 * - سحب واحد كل دورة الحقن (لا كل عيّنة): الحقن عبر الصدفة/خدمة الوصول ليس مسارًا بتردّد
 *   ١٢٠ هرتز، فالتنعيم هنا يمنع الطيران لا يضمن الدقّة.
 * - لا يُدّعى أن كل لعبة تقبل هذا النوع من التحكّم؛ وهذا يُقاس على جهاز المستخدم.
 */
class GyroAimController(context: Context) : SensorEventListener {

    private val appContext = context.applicationContext
    private val sensors = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyro: Sensor? = sensors?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var settings = GyroAimSettings()
    private var lastTimestampNs = 0L
    private var previousDx = 0f
    private var previousDy = 0f
    private var running = false

    @Volatile
    private var injecting = false

    /** هل يوجد جيروسكوب في هذا الجهاز؟ */
    val available: Boolean get() = gyro != null

    fun start(settings: GyroAimSettings) {
        val sensor = gyro ?: return
        this.settings = settings.normalized()
        if (running) return
        running = true
        lastTimestampNs = 0L
        previousDx = 0f
        previousDy = 0f
        runCatching {
            sensors?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME, handler)
        }
    }

    /** تحديث حيّ أثناء العمل — تغيير الحساسية لا يحتاج إعادة تسجيل. */
    fun update(settings: GyroAimSettings) {
        this.settings = settings.normalized()
    }

    fun stop() {
        if (!running) return
        running = false
        runCatching { sensors?.unregisterListener(this) }
        lastTimestampNs = 0L
    }

    fun destroy() {
        stop()
        scope.cancel()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val sample = event ?: return
        if (sample.sensor.type != Sensor.TYPE_GYROSCOPE) return
        if (!running || !settings.enabled) return
        val timestamp = sample.timestamp
        val dtMs = if (lastTimestampNs == 0L) 0L else (timestamp - lastTimestampNs) / 1_000_000L
        lastTimestampNs = timestamp
        if (dtMs < GyroAimMath.MIN_DT_MS) return
        val delta = GyroAimMath.step(
            yawRadPerSec = sample.values.getOrElse(2) { 0f },
            pitchRadPerSec = sample.values.getOrElse(0) { 0f },
            dtMs = dtMs,
            settings = settings,
            previousDx = previousDx,
            previousDy = previousDy,
        )
        previousDx = delta.dx
        previousDy = delta.dy
        if (delta.isStill || injecting) return
        injecting = true
        val metrics = appContext.resources.displayMetrics
        val centerX = metrics.widthPixels / 2f
        val centerY = metrics.heightPixels / 2f
        scope.launch {
            runCatching {
                TouchInjector.swipe(
                    appContext,
                    centerX, centerY,
                    centerX + delta.dx, centerY + delta.dy,
                    GYRO_SWIPE_MS,
                )
            }
            injecting = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        /** سحبة قصيرة تُقرأ كتحريك مستمرّ في معظم الألعاب، وأقصر من أن تُقرأ كسحب قائمة. */
        const val GYRO_SWIPE_MS = 50
    }
}
