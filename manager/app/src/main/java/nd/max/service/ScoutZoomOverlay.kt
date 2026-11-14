/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import nd.max.AppMonitorLogger

/**
 * Scout: عدسة مكبِّرة لمركز الشاشة فوق اللعبة.
 *
 * ### من أين تأتي الصورة
 *
 * من `AccessibilityService.takeScreenshot` عبر [nd.max.core.input.A11yGestureInjector] — أي
 * **قراءة محتوى الشاشة**، وهذا هو الموضع الوحيد في التطبيق الذي يقرأه، ويُقال في وصف خدمة
 * الوصول وفي الإقرار. الصورة لا تُخزَّن ولا تُرسل؛ تُقصّ وتُعرض ثم تُترك للجامع.
 *
 * ### حدوده
 *
 * - يحتاج Android 11+ وخدمة وصول مفعّلة؛ وإلا تُعرض «غير متاح» بصدق.
 * - التحديث كل ٧٠٠ ملّي ثانية: العدسة للمساعدة على الرؤية لا لبثّ فيديو، ومعدّل أعلى يستهلك
 *   المعالج ويُبطئ اللعبة التي يُفترض أنها تُساعد عليها.
 */
internal class ScoutZoomOverlay(context: Context) {

    private val appContext: Context = context.applicationContext
    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = appContext.resources.displayMetrics.density

    private var root: FrameLayout? = null
    private var image: ImageView? = null
    private var label: TextView? = null
    private var shown = false

    fun show() {
        if (shown) return
        val frame = FrameLayout(appContext).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(0xE612161B.toInt())
                setStroke(dp(2), 0x99FFFFFF.toInt())
            }
            clipToOutline = true
        }
        val picture = ImageView(appContext).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        val caption = TextView(appContext).apply {
            text = "SCOUT"
            setTextColor(0xCCFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(3), dp(6), dp(3))
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(0x99000000.toInt())
            }
        }
        frame.addView(
            picture,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ),
        )
        frame.addView(
            caption,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.START
            ).apply { leftMargin = dp(6); topMargin = dp(6) },
        )
        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val lp = WindowManager.LayoutParams(
            dp(SIZE_DP),
            dp(SIZE_DP),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(112)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        runCatching { windowManager.addView(frame, lp) }
            .onSuccess {
                root = frame
                image = picture
                label = caption
                shown = true
            }
            .onFailure { AppMonitorLogger.w("EVENT=SCOUT_OVERLAY_ADD_FAILED", it) }
    }

    /** لقطة جديدة: تُقصّ مربّعًا حول المركز وتُعرض؛ و`null` تعني «غير متاح» بصدق. */
    fun update(bitmap: Bitmap?) {
        if (!shown) return
        if (bitmap == null) {
            label?.text = "SCOUT —"
            return
        }
        val side = minOf(bitmap.width, bitmap.height) / 2
        if (side <= 0) return
        val left = ((bitmap.width - side) / 2).coerceAtLeast(0)
        val top = ((bitmap.height - side) / 2).coerceAtLeast(0)
        val crop = runCatching {
            Bitmap.createBitmap(bitmap, left, top, side, side)
        }.getOrNull() ?: return
        image?.setImageBitmap(crop)
        label?.text = "SCOUT ${crop.width}px"
    }

    fun hide() {
        root?.let { view -> runCatching { windowManager.removeView(view) } }
        root = null
        image = null
        label = null
        shown = false
    }

    fun destroy() = hide()

    private fun dp(value: Int): Int = (value * density).toInt()

    private companion object {
        const val SIZE_DP = 168
    }
}
