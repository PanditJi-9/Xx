/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
package nd.max.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import nd.max.AppMonitorLogger
import nd.max.core.gamespace.CrosshairGeometry
import nd.max.core.gamespace.CrosshairState
import nd.max.core.gamespace.crosshairGeometry
import kotlin.math.roundToInt

/**
 * نافذة التصويب فوق اللعبة — **علامة لا تلمس**.
 *
 * نافذة مستقلّة عن لوحة Game Turbo: لوحة جانبية صغيرة على الحافة، وهذه ملء الشاشة شفافة
 * لا تستقبل لمسًا (`FLAG_NOT_TOUCHABLE`) ولا تركيزًا. ولذلك لا تحتاج `OverlayWindow` الذي
 * يدير السحب والسحب للإغلاق ودورة حياة Compose؛ رسمٌ خام بـ[Paint] أخفّ ولا يستهلك إطارًا
 * حين لا يتغيّر شيء.
 *
 * **وحدّها المعلن:** تحتاج `SYSTEM_ALERT_WINDOW` كأي تراكب آخر. وفشل `addView` لا يُسقط
 * الخدمة: يُسجَّل باسمه، ويبقى باقي Game Turbo يعمل.
 */
internal class CrosshairOverlay(context: Context) {

    private val appContext: Context = context.applicationContext
    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val view = CrosshairView(appContext)
    private var params: WindowManager.LayoutParams? = null
    private var added = false

    /** يعرض الحالة أو يحدّثها إن كانت النافذة مضافة. */
    fun show(state: CrosshairState) {
        view.state = state
        if (added) return
        val lp = buildParams()
        runCatching { windowManager.addView(view, lp) }
            .onSuccess {
                params = lp
                added = true
            }
            .onFailure {
                AppMonitorLogger.w("EVENT=CROSSHAIR_OVERLAY_FAILED stage=addView", it)
            }
    }

    fun hide() {
        if (!added) return
        runCatching { windowManager.removeView(view) }
        params = null
        added = false
    }

    /** بعد تبدّل الاتجاه أو المقاس: النافذة MATCH_PARENT، ويُعاد الرسم بالمقاس الجديد. */
    fun refresh() {
        if (!added) return
        view.invalidate()
        val lp = params ?: return
        runCatching { windowManager.updateViewLayout(view, lp) }
    }

    fun destroy() = hide()

    private fun buildParams(): WindowManager.LayoutParams {
        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    /**
     * الرسم الخام: خطوط ودوائر [CrosshairGeometry] وحدها. والحدود الداكنة تُرسم أولًا
     * بخطٍّ أعرض قليلًا، فتبقى العلامة مقروءة على الثلج والسماء والجدران الفاتحة.
     */
    private class CrosshairView(context: Context) : View(context) {

        private val density = resources.displayMetrics.density
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
        }

        var state: CrosshairState = CrosshairState()
            set(value) {
                field = value
                invalidate()
            }

        init {
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = false
            isFocusable = false
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val clean = state.sanitized()
            if (!clean.enabled || width <= 0 || height <= 0) return
            val geometry = crosshairGeometry(clean, width.toFloat(), height.toFloat(), density)
            val alpha = (Color.alpha(clean.color) * clean.opacity).roundToInt().coerceIn(0, 255)
            val ink = Color.argb(
                alpha,
                Color.red(clean.color),
                Color.green(clean.color),
                Color.blue(clean.color)
            )
            if (clean.outline) {
                val outlineAlpha = (alpha * 0.55f).roundToInt().coerceIn(0, 255)
                drawGeometry(canvas, geometry, Color.argb(outlineAlpha, 0, 0, 0), 2.5f * density)
            }
            drawGeometry(canvas, geometry, ink, 0f)
        }

        private fun drawGeometry(
            canvas: Canvas,
            geometry: CrosshairGeometry,
            color: Int,
            extra: Float
        ) {
            paint.color = color
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = geometry.strokeWidth + extra
            geometry.lines.forEach {
                canvas.drawLine(it.startX, it.startY, it.endX, it.endY, paint)
            }
            geometry.circles.forEach { circle ->
                if (circle.filled) {
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(
                        circle.centerX,
                        circle.centerY,
                        circle.radius + extra / 2f,
                        paint
                    )
                } else {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = geometry.strokeWidth + extra
                    canvas.drawCircle(circle.centerX, circle.centerY, circle.radius, paint)
                }
            }
        }
    }
}
