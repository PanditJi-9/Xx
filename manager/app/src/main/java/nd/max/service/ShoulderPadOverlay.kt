/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.service

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import nd.max.AppMonitorLogger
import nd.max.core.input.PadSide
import nd.max.core.input.ShoulderPadLayout
import nd.max.core.input.ShoulderPadMath

/**
 * زرّا الكتف (L/R) فوق اللعبة — **بلمس حقيقيّ، لا زخرفة**.
 *
 * ### كيف تعملان
 *
 * كل زرّ نافذة صغيرة قابلة للسحب. **النقر** عليها يُحقن لمسة في مركزها عبر
 * [nd.max.core.input.TouchInjector]، أي على عنصر التحكّم الذي وُضع الزرّ فوقه في اللعبة.
 * و**السحب** يغيّر موضعه ويحفظه كنسبة من الشاشة. ولأن الزرّ نفسه يستهلك اللمسة، فالحقنة هي
 * التي تصل إلى اللعبة تحته — وهذا هو الميكانيزم المقصود لا خطأ.
 *
 * ### حدوده
 *
 * - يحتاج `SYSTEM_ALERT_WINDOW` (كأيّ تراكب) ومسار حقن ([nd.max.core.input.InjectionLane]).
 * - لعبة ترفض اللمس المُولَّد لن تستجيب؛ يُعلن ذلك في الإقرار ولا يُخفى.
 * - لا يُدّعى أن الزرّين يظهران فوق كل لعبة: نوافذ ملء الشاشة الخاصة قد تحجب التراكب.
 */
internal class ShoulderPadOverlay(
    context: Context,
    initial: ShoulderPadLayout,
    private val onTap: (PadSide, Float, Float) -> Unit,
    private val onMoved: (PadSide, Float, Float) -> Unit,
) {

    private val appContext: Context = context.applicationContext
    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = appContext.resources.displayMetrics.density
    private val padPx = dp(PAD_DP)

    private var layout = ShoulderPadMath.normalized(initial)
    private val leftView = buildView("L")
    private val rightView = buildView("R")
    private var leftParams: WindowManager.LayoutParams? = null
    private var rightParams: WindowManager.LayoutParams? = null
    private var leftAdded = false
    private var rightAdded = false

    fun show() {
        if (leftAdded && rightAdded) return
        val metrics = appContext.resources.displayMetrics
        if (!leftAdded) {
            val lp = buildParams(PadSide.LEFT, metrics.widthPixels, metrics.heightPixels)
            runCatching { windowManager.addView(leftView, lp) }
                .onSuccess { leftParams = lp; leftAdded = true }
                .onFailure { AppMonitorLogger.w("EVENT=SHOULDER_PAD_ADD_FAILED side=left", it) }
        }
        if (!rightAdded) {
            val lp = buildParams(PadSide.RIGHT, metrics.widthPixels, metrics.heightPixels)
            runCatching { windowManager.addView(rightView, lp) }
                .onSuccess { rightParams = lp; rightAdded = true }
                .onFailure { AppMonitorLogger.w("EVENT=SHOULDER_PAD_ADD_FAILED side=right", it) }
        }
    }

    /** تحديث الموضع بعد تبدّل الوضع/الدقّة أو بعد حفظ جديد. */
    fun updateLayout(next: ShoulderPadLayout) {        layout = ShoulderPadMath.normalized(next)
        val metrics = appContext.resources.displayMetrics
        leftParams?.let { lp ->
            lp.x = ShoulderPadMath.centerPx(layout.leftX, metrics.widthPixels, padPx)
            lp.y = ShoulderPadMath.centerPx(layout.leftY, metrics.heightPixels, padPx)
            runCatching { windowManager.updateViewLayout(leftView, lp) }
        }
        rightParams?.let { lp ->
            lp.x = ShoulderPadMath.centerPx(layout.rightX, metrics.widthPixels, padPx)
            lp.y = ShoulderPadMath.centerPx(layout.rightY, metrics.heightPixels, padPx)
            runCatching { windowManager.updateViewLayout(rightView, lp) }
        }
    }

    fun hide() {
        if (leftAdded) {
            runCatching { windowManager.removeView(leftView) }
            leftAdded = false
            leftParams = null
        }
        if (rightAdded) {
            runCatching { windowManager.removeView(rightView) }
            rightAdded = false
            rightParams = null
        }
    }

    fun destroy() = hide()

    private fun buildParams(side: PadSide, screenW: Int, screenH: Int): WindowManager.LayoutParams {
        val x = when (side) {
            PadSide.LEFT -> ShoulderPadMath.centerPx(layout.leftX, screenW, padPx)
            PadSide.RIGHT -> ShoulderPadMath.centerPx(layout.rightX, screenW, padPx)
        }
        val y = when (side) {
            PadSide.LEFT -> ShoulderPadMath.centerPx(layout.leftY, screenH, padPx)
            PadSide.RIGHT -> ShoulderPadMath.centerPx(layout.rightY, screenH, padPx)
        }
        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            padPx,
            padPx,
            type,
            // **قابلة للمس عن قصد:** الزرّ يستهلك اللمسة ثم يحقنها في مركزه.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun buildView(label: String): TextView = TextView(appContext).apply {
        text = label
        setTextColor(0xF2FFFFFF.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        gravity = Gravity.CENTER
        alpha = 0.88f
        isClickable = true
        isFocusable = false
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xCC161A20.toInt())
            setStroke(dp(2), 0x99FFFFFF.toInt())
        }
        setOnTouchListener(PadTouchListener(this, label))
    }

    /** سحب وإفلات + نقرة — والفرق بينهما يُقاس بالحركة لا بالمدة. */
    private inner class PadTouchListener(
        private val view: View,
        private val label: String,
    ) : View.OnTouchListener {

        private var downRawX = 0f
        private var downRawY = 0f
        private var baseX = 0
        private var baseY = 0
        private var moved = false
        private var downAt = 0L

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val side = if (label == "L") PadSide.LEFT else PadSide.RIGHT
            val params = (if (side == PadSide.LEFT) leftParams else rightParams) ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    baseX = params.x
                    baseY = params.y
                    moved = false
                    downAt = SystemClock.elapsedRealtime()
                    view.alpha = 1f
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!moved && (kotlin.math.abs(dx) > dp(DRAG_SLOP_DP) ||
                            kotlin.math.abs(dy) > dp(DRAG_SLOP_DP))
                    ) {
                        moved = true
                    }
                    if (moved) {
                        val metrics = appContext.resources.displayMetrics
                        params.x = (baseX + dx.toInt())
                            .coerceIn(0, (metrics.widthPixels - padPx).coerceAtLeast(0))
                        params.y = (baseY + dy.toInt())
                            .coerceIn(0, (metrics.heightPixels - padPx).coerceAtLeast(0))
                        runCatching { windowManager.updateViewLayout(view, params) }
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    view.alpha = 0.88f
                    moved = false
                }

                MotionEvent.ACTION_UP -> {
                    view.alpha = 0.88f
                    val metrics = appContext.resources.displayMetrics
                    if (moved) {
                        val xFraction = ShoulderPadMath.fractionOf(params.x, metrics.widthPixels, padPx)
                        val yFraction = ShoulderPadMath.fractionOf(params.y, metrics.heightPixels, padPx)
                        when (side) {
                            PadSide.LEFT -> layout = layout.copy(leftX = xFraction, leftY = yFraction)
                            PadSide.RIGHT -> layout = layout.copy(rightX = xFraction, rightY = yFraction)
                        }
                        onMoved(side, xFraction, yFraction)
                    } else if (SystemClock.elapsedRealtime() - downAt <= TAP_MAX_MS) {
                        onTap(
                            side,
                            ShoulderPadMath.tapX(params.x, padPx),
                            ShoulderPadMath.tapY(params.y, padPx),
                        )
                    }
                }
            }
            return true
        }
    }

    private fun dp(value: Int): Int = (value * density).toInt()

    private companion object {
        /** ٦٤dp: فوق حدّ اللمس ٤٨dp بهامش، وتحت حجم يغطّي أزرار اللعبة. */
        const val PAD_DP = 64
        const val DRAG_SLOP_DP = 8
        const val TAP_MAX_MS = 700L
    }
}
