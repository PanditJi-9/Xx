/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import nd.max.AppMonitorLogger

/**
 * مسجِّل الماكرو: طبقة شفّافة تلتقط اللمس، تمرّره إلى اللعبة، وتسجّل الخطوة.
 *
 * ### لماذا طبقة فوق اللعبة
 *
 * التسجيل يحتاج أن يرى التطبيق اللمسة قبل اللعبة؛ فيُلتقط المسار كاملًا (نزول/حركة/رفع)
 * ويُمرَّر عبر [nd.max.core.input.TouchInjector.passThrough] بنفس اللحظة — فلا تصير اللعبة
 * صمّاء أثناء التسجيل. وهذا هو الموضع الوحيد الذي يُتجاوَز فيه حارس الإصبع، لأن الإصبع
 * الذي يرسم الخطوة هو مصدرها.
 *
 * ### حدوده
 *
 * - لا يظهر إلا بلعبة متتبَّعة ومسار حقن وإقرار.
 * - الحركة تُسجَّل بخطّ مستقيم بين النزول والرفع؛ اللعبة التي تعتمد على منحنى إصبع دقيق
 *   قد لا تراه كما رسمته — يُعلن في الواجهة.
 */
internal class MacroPadOverlay(
    context: Context,
    private val onStroke: (Float, Float, Float, Float, Int) -> Unit,
    private val onStop: () -> Unit,
) {

    private val appContext: Context = context.applicationContext
    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = appContext.resources.displayMetrics.density

    private var root: FrameLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var shown = false
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var downAt = 0L
    private var moved = false

    fun show() {
        if (shown) return
        val metrics = appContext.resources.displayMetrics
        val frame = FrameLayout(appContext).apply {
            setBackgroundColor(0x22000000)
        }
        val label = TextView(appContext).apply {
            text = "REC"
            setTextColor(0xFFF26D6D.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(0xCC161A20.toInt())
                setStroke(dp(1), 0x99F26D6D.toInt())
            }
        }
        val stop = TextView(appContext).apply {
            text = "STOP"
            setTextColor(0xF2FFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(0xCC1F1216.toInt())
                setStroke(dp(1), 0x99F26D6D.toInt())
            }
            setOnClickListener { onStop() }
        }
        frame.addView(
            label,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.START
            ).apply { topMargin = dp(48); leftMargin = dp(12) },
        )
        frame.addView(
            stop,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply { topMargin = dp(48); rightMargin = dp(12) },
        )
        frame.setOnTouchListener(CaptureListener())
        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val lp = WindowManager.LayoutParams(
            metrics.widthPixels,
            metrics.heightPixels,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        runCatching { windowManager.addView(frame, lp) }
            .onSuccess {
                root = frame
                params = lp
                shown = true
            }
            .onFailure { AppMonitorLogger.w("EVENT=MACRO_PAD_ADD_FAILED", it) }
    }

    fun hide() {
        root?.let { view -> runCatching { windowManager.removeView(view) } }
        root = null
        params = null
        shown = false
    }

    fun destroy() = hide()

    private inner class CaptureListener : View.OnTouchListener {
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val metrics = appContext.resources.displayMetrics
            val width = metrics.widthPixels.toFloat().coerceAtLeast(1f)
            val height = metrics.heightPixels.toFloat().coerceAtLeast(1f)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX.coerceIn(0f, width)
                    downY = event.rawY.coerceIn(0f, height)
                    lastX = downX
                    lastY = downY
                    downAt = SystemClock.elapsedRealtime()
                    moved = false
                }

                MotionEvent.ACTION_MOVE -> {
                    lastX = event.rawX.coerceIn(0f, width)
                    lastY = event.rawY.coerceIn(0f, height)
                    if (!moved && (kotlin.math.abs(lastX - downX) > dp(6) ||
                            kotlin.math.abs(lastY - downY) > dp(6))
                    ) {
                        moved = true
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    downAt = 0L
                    moved = false
                }

                MotionEvent.ACTION_UP -> {
                    val upX = event.rawX.coerceIn(0f, width)
                    val upY = event.rawY.coerceIn(0f, height)
                    if (!moved) {
                        lastX = upX
                        lastY = upY
                    }
                    val duration = (SystemClock.elapsedRealtime() - downAt).toInt()
                        .coerceIn(40, 1_500)
                    downAt = 0L
                    onStroke(downX / width, downY / height, lastX / width, lastY / height, duration)
                }
            }
            return true
        }
    }

    private fun dp(value: Int): Int = (value * density).toInt()
}
