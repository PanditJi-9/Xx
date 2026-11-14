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
import nd.max.core.input.ExtraGesture
import nd.max.core.input.ExtraPoint
import nd.max.core.input.GameExtrasMath

/**
 * نقاط الإيماءات فوق اللعبة — كل نقطة زرّ صغير قابل للسحب، ونقرته تُنفّذ إيماءته.
 *
 * ### الميكانيزم
 *
 * النقطة نافذة صغيرة فوق زرّ اللعبة الذي اختاره المستخدم. النقر يستهلكه التراكب ثم يحقن
 * الإيماءة في مركز النقطة نفسها — أي على زرّ اللعبة تحته. والسحب يحرّك النقطة ويحفظها كنسبة.
 *
 * ### حدوده
 *
 * يحتاج `SYSTEM_ALERT_WINDOW` ومسار حقن وإقرارًا، ولا يظهر إلا فوق لعبة متتبَّعة. واللعبة التي
 * ترفض الإدخال المُولَّد ترفض هذه أيضًا — وهذا مُعلن في الإقرار لا مخفيّ.
 */
internal class GameExtrasOverlay(
    context: Context,
    initial: List<ExtraPoint>,
    private val onPress: (ExtraPoint) -> Unit,
    private val onMoved: (ExtraPoint) -> Unit,
) {

    private val appContext: Context = context.applicationContext
    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = appContext.resources.displayMetrics.density
    private val dotPx = dp(DOT_DP)

    private var points: List<ExtraPoint> = GameExtrasMath.sanitize(initial)
    private val views = LinkedHashMap<Int, TextView>()
    private val params = LinkedHashMap<Int, WindowManager.LayoutParams>()

    fun show(next: List<ExtraPoint>) {
        points = GameExtrasMath.sanitize(next)
        val metrics = appContext.resources.displayMetrics
        // إزالة ما لم يبقَ في القائمة، ثم إضافة الجديد أو تحديث موضعه.
        val stale = views.keys.filter { id -> points.none { it.id == id } }
        for (id in stale) {
            views.remove(id)?.let { view -> runCatching { windowManager.removeView(view) } }
            params.remove(id)
        }
        for (point in points) {
            val existing = views[point.id]
            if (existing == null) {
                val view = buildView(point)
                val lp = buildParams(point, metrics.widthPixels, metrics.heightPixels)
                runCatching { windowManager.addView(view, lp) }
                    .onSuccess {
                        views[point.id] = view
                        params[point.id] = lp
                    }
                    .onFailure { AppMonitorLogger.w("EVENT=GAME_EXTRA_ADD_FAILED id=${point.id}", it) }
            } else {
                existing.text = glyph(point.gesture)
                params[point.id]?.let { lp ->
                    lp.x = GameExtrasMath.px(point.x, metrics.widthPixels).toInt()
                    lp.y = GameExtrasMath.px(point.y, metrics.heightPixels).toInt()
                    runCatching { windowManager.updateViewLayout(existing, lp) }
                }
            }
        }
    }

    fun hide() {
        for (view in views.values) runCatching { windowManager.removeView(view) }
        views.clear()
        params.clear()
    }

    fun destroy() = hide()

    private fun buildParams(point: ExtraPoint, screenW: Int, screenH: Int): WindowManager.LayoutParams {
        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            dotPx,
            dotPx,
            type,
            // قابلة للمس عن قصد: النقطة تستهلك اللمسة ثم تحقن إيماءتها في مركزها.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = GameExtrasMath.px(point.x, screenW).toInt()
            y = GameExtrasMath.px(point.y, screenH).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun buildView(point: ExtraPoint): TextView = TextView(appContext).apply {
        text = glyph(point.gesture)
        setTextColor(0xF2FFFFFF.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        gravity = Gravity.CENTER
        alpha = 0.82f
        isClickable = true
        isFocusable = false
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xB3161A20.toInt())
            setStroke(dp(2), 0x99FFFFFF.toInt())
        }
        setOnTouchListener(DotTouchListener(this, point.id))
    }

    private fun glyph(gesture: ExtraGesture): String = when (gesture) {
        ExtraGesture.TAP -> "•"
        ExtraGesture.DOUBLE_TAP -> "2"
        ExtraGesture.LONG_PRESS -> "L"
        ExtraGesture.SWIPE_UP -> "↑"
        ExtraGesture.SWIPE_DOWN -> "↓"
        ExtraGesture.SWIPE_LEFT -> "←"
        ExtraGesture.SWIPE_RIGHT -> "→"
    }

    /** سحب وإفلات + نقرة، بنفس عقد أزرار الكتف. */
    private inner class DotTouchListener(
        private val view: View,
        private val id: Int,
    ) : View.OnTouchListener {

        private var downRawX = 0f
        private var downRawY = 0f
        private var baseX = 0
        private var baseY = 0
        private var moved = false
        private var downAt = 0L

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val lp = params[id] ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    baseX = lp.x
                    baseY = lp.y
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
                        lp.x = (baseX + dx.toInt())
                            .coerceIn(0, (metrics.widthPixels - dotPx).coerceAtLeast(0))
                        lp.y = (baseY + dy.toInt())
                            .coerceIn(0, (metrics.heightPixels - dotPx).coerceAtLeast(0))
                        runCatching { windowManager.updateViewLayout(view, lp) }
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    view.alpha = 0.82f
                    moved = false
                }

                MotionEvent.ACTION_UP -> {
                    view.alpha = 0.82f
                    val metrics = appContext.resources.displayMetrics
                    if (moved) {
                        val xFraction = GameExtrasMath.fractionOf(lp.x.toFloat(), metrics.widthPixels)
                        val yFraction = GameExtrasMath.fractionOf(lp.y.toFloat(), metrics.heightPixels)
                        points = points.map { point ->
                            if (point.id == id) point.copy(x = xFraction, y = yFraction) else point
                        }
                        points.firstOrNull { it.id == id }?.let(onMoved)
                    } else if (SystemClock.elapsedRealtime() - downAt <= TAP_MAX_MS) {
                        points.firstOrNull { it.id == id }?.let(onPress)
                    }
                }
            }
            return true
        }
    }

    private fun dp(value: Int): Int = (value * density).toInt()

    private companion object {
        /** ٥٢dp: فوق حدّ اللمس ٤٨dp، وأصغر من أن تغطّي زرًّا كاملًا في اللعبة. */
        const val DOT_DP = 52
        const val DRAG_SLOP_DP = 8
        const val TAP_MAX_MS = 700L
    }
}
