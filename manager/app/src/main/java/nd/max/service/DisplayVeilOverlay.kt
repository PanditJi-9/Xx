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
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import nd.max.AppMonitorLogger
import nd.max.core.gamespace.DisplayVeilState

/**
 * ستارة العرض فوق اللعبة — **طبقة لون واحدة لا تلمس ولا تُلتقط**.
 *
 * ### ما هي وما ليست
 *
 * لون واحد شفاف يُرسم على كامل الشاشة: دافئ، أو أخضر ليليّ، أو تعتيم محايد. **وليست** معالجة
 * صورة: لا تزيد التباين ولا الحدّة ولا التشبّع، ولذلك لا يحمل اسمها ولا نصوصها وعدًا بذلك.
 *
 * ### ولماذا لا [nd.max.ui.overlay.OverlayWindow]
 *
 * كالتصويب ([CrosshairOverlay]): نافذة ملء الشاشة لا تُسحب ولا تستقبل لمسًا
 * (`FLAG_NOT_TOUCHABLE`) ولا تركيزًا، فدورة حياة Compose والسحب كلها وزن لا مقابل له. رسمٌ خام
 * بـ[Canvas.drawColor] أخفّ، ولا يستهلك إطارًا حين لا يتغيّر اللون.
 *
 * **وحدّها المعلن:** تحتاج `SYSTEM_ALERT_WINDOW` كأيّ تراكب آخر. وفشل `addView` لا يُسقط خدمة
 * اللوحة: يُسجَّل باسمه ويبقى باقي التحكّم يعمل.
 */
internal class DisplayVeilOverlay(context: Context) {

    private val appContext: Context = context.applicationContext
    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val view = VeilView(appContext)
    private var params: WindowManager.LayoutParams? = null
    private var added = false

    /** يعرض اللون أو يحدّثه إن كانت النافذة مضافة. */
    fun show(state: DisplayVeilState) {
        view.state = state
        if (added) return
        val lp = buildParams()
        runCatching { windowManager.addView(view, lp) }
            .onSuccess {
                params = lp
                added = true
            }
            .onFailure {
                AppMonitorLogger.w("EVENT=DISPLAY_VEIL_OVERLAY_FAILED stage=addView", it)
            }
    }

    fun hide() {
        if (!added) return
        runCatching { windowManager.removeView(view) }
        params = null
        added = false
    }

    /** بعد تبدّل الاتجاه أو المقاس: النافذة MATCH_PARENT ويُعاد الرسم بالمقاس الجديد. */
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

    /** الرسم: [Canvas.drawColor] باللون الواحد. لا Paint ولا هندسة. */
    private class VeilView(context: Context) : View(context) {

        var state: DisplayVeilState = DisplayVeilState()
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
            if (!state.enabled || width <= 0 || height <= 0) return
            canvas.drawColor(state.argb)
        }
    }
}
