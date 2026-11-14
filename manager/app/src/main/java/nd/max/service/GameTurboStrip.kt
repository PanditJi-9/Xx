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
import android.widget.LinearLayout
import android.widget.TextView
import nd.max.AppMonitorLogger

/**
 * شريط التحكّم العائم في وضع Game Turbo — **خمس أدوات حقيقيّة فوق اللعبة**.
 *
 * ### لماذا هو عائم لا شريط تنقّل
 *
 * الشريط المطلوب يعيش **فوق اللعبة أثناء اللعب** (وضع Turbo)، لا داخل شاشة Game Space؛
 * فكل زرّ فيه ينفّذ فعلًا حقيقيًّا بلا مغادرة اللعبة: عدّاد FPS، ولقطة، وتسجيل، وتكبير Scout،
 * وإخفاء الشريط. وهذا يختلف عن شريط تنقّل ثانٍ داخل الشاشة، وهو ما رُفض سابقًا لازدواج المصدر.
 *
 * ### حدوده
 *
 * - يحتاج `SYSTEM_ALERT_WINDOW`، ويظهر فوق لعبة متتبَّعة فقط.
 * - اللقطة والتسجيل يحتاجان مسار الامتياز (جذر/Shizuku) كما في لوحة اللعب؛ بلا مسار يُعلن
 *   الفشل ولا يُدّعى نجاح.
 * - موضعه يُحفظ كنسبة رأسيّة فيبقى حيث تركه المستخدم بعد تبدّل الوضع.
 */
internal class GameTurboStrip(
    context: Context,
    initialY: Float,
    private val onFps: () -> Unit,
    private val onScreenshot: () -> Unit,
    private val onRecord: () -> Unit,
    private val onScout: () -> Unit,
    private val onClose: () -> Unit,
    private val onMoved: (Float) -> Unit,
) {

    private val appContext: Context = context.applicationContext
    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = appContext.resources.displayMetrics.density

    private var params: WindowManager.LayoutParams? = null
    private var root: LinearLayout? = null
    private var grown = false
    private var yFraction = initialY.coerceIn(0.05f, 0.85f)
    private var recordButton: TextView? = null
    private var scoutButton: TextView? = null

    fun show() {
        if (grown) return
        val metrics = appContext.resources.displayMetrics
        val layout = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(6), dp(8), dp(6), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(0xD914181E.toInt())
                setStroke(dp(1), 0x66FFFFFF)
            }
            addView(handle())
            addView(button("FPS") { onFps() })
            addView(button("PNG") { onScreenshot() })
            recordButton = button("REC") { onRecord() }.also { addView(it) }
            scoutButton = button("ZOOM") { onScout() }.also { addView(it) }
            addView(button("×") { onClose() })
        }
        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(8)
            // الارتفاع يُقاس بعد الإضافة في `post`؛ هنا تقدير آمن يبقيه داخل الشاشة.
            y = (yFraction * (metrics.heightPixels - dp(STRIP_ESTIMATE_DP)).coerceAtLeast(0)).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        runCatching { windowManager.addView(layout, lp) }
            .onSuccess {
                root = layout
                params = lp
                grown = true
                layout.post { clampIntoView() }
            }
            .onFailure { AppMonitorLogger.w("EVENT=GAME_TURBO_ADD_FAILED", it) }
    }

    fun setRecording(recording: Boolean) {
        recordButton?.text = if (recording) "STOP" else "REC"
        recordButton?.setTextColor(if (recording) 0xFFF26D6D.toInt() else 0xF2FFFFFF.toInt())
    }

    fun setScout(active: Boolean) {
        scoutButton?.setTextColor(if (active) 0xFF6DE0A2.toInt() else 0xF2FFFFFF.toInt())
    }

    fun hide() {
        root?.let { view -> runCatching { windowManager.removeView(view) } }
        root = null
        params = null
        grown = false
    }

    fun destroy() = hide()

    private fun handle(): TextView = TextView(appContext).apply {
        text = "≡"
        setTextColor(0xCCFFFFFF.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        gravity = Gravity.CENTER
        setPadding(0, dp(2), 0, dp(4))
        setOnTouchListener(DragListener(this))
    }

    private fun button(label: String, action: () -> Unit): TextView = TextView(appContext).apply {
        text = label
        setTextColor(0xF2FFFFFF.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = false
        setPadding(dp(8), dp(8), dp(8), dp(8))
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0x33161A20)
            setStroke(dp(1), 0x44FFFFFF)
        }
        layoutParams = LinearLayout.LayoutParams(dp(48), dp(40)).apply {
            bottomMargin = dp(4)
        }
        setOnClickListener { action() }
    }

    private inner class DragListener(private val handleView: View) : View.OnTouchListener {
        private var downRawY = 0f
        private var baseY = 0
        private var downAt = 0L

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val lp = params ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawY = event.rawY
                    baseY = lp.y
                    downAt = SystemClock.elapsedRealtime()
                }

                MotionEvent.ACTION_MOVE -> {
                    val metrics = appContext.resources.displayMetrics
                    val height = root?.height?.takeIf { it > 0 } ?: dp(STRIP_ESTIMATE_DP)
                    lp.y = (baseY + (event.rawY - downRawY).toInt())
                        .coerceIn(0, (metrics.heightPixels - height).coerceAtLeast(0))
                    runCatching { windowManager.updateViewLayout(handleView.rootView, lp) }
                }

                MotionEvent.ACTION_UP -> {
                    val metrics = appContext.resources.displayMetrics
                    val height = root?.height?.takeIf { it > 0 } ?: dp(STRIP_ESTIMATE_DP)
                    val room = (metrics.heightPixels - height).coerceAtLeast(1)
                    yFraction = (lp.y.toFloat() / room).coerceIn(0.05f, 0.85f)
                    onMoved(yFraction)
                    if (SystemClock.elapsedRealtime() - downAt <= TAP_MAX_MS && lp.y == baseY) {
                        // لمسة سريعة على المقبض تطوي الشريط — نفس زرّ الإغلاق بلا ازدواج واجهة.
                        onClose()
                    }
                }

                MotionEvent.ACTION_CANCEL -> Unit
            }
            return true
        }
    }

    private fun clampIntoView() {
        val lp = params ?: return
        val metrics = appContext.resources.displayMetrics
        val height = root?.height?.takeIf { it > 0 } ?: dp(STRIP_ESTIMATE_DP)
        val room = (metrics.heightPixels - height).coerceAtLeast(1)
        lp.y = (yFraction * room).toInt()
        root?.let { runCatching { windowManager.updateViewLayout(it, lp) } }
    }

    private fun dp(value: Int): Int = (value * density).toInt()

    private companion object {
        /** تقدير ارتفاع الشريط قبل القياس: ٦ صفوف × ٤٤dp + الحشو. */
        const val STRIP_ESTIMATE_DP = 292
        const val TAP_MAX_MS = 400L
    }
}
