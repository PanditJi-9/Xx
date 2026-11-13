/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
package nd.max.service

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import nd.max.AppMonitorLogger
import nd.max.core.gamespace.GameMemo

/**
 * المذكّرة المثبّتة فوق اللعبة — **سطر واحد لا يلمس ولا يأخذ تركيزًا**.
 *
 * ### ما هي وما ليست
 *
 * [nd.max.core.gamespace.PanelReminder] ينبّه مرّة عند انتهاء مدّة ثم يصمت؛ وهذه تحمل سطرًا
 * **يبقى** ما دامت اللعبة أمام المستخدم («المرحلة ٤ — الباب الأيسر») ويُطفأ يدويًّا. وليست سطحًا
 * ثانيًا للمؤقّت: لا تعدّ وقتًا ولا تُنبّه، ولا تظهر إلّا بنصّ غير فارغ.
 *
 * ### ولماذا رسمٌ خام لا Compose
 *
 * كالستارة ([DisplayVeilOverlay]): نافذة صغيرة عابرة تُضاف وتُزال مع تبدّل اللعبة، فلا داعي
 * لدورة حياة Compose وسحب وViewModelStore. ولذلك أيضًا [OverlayWindow] (المصمَّم لنوافذ تُسحب
 * وتُطوى) لا يُستعمل هنا: نافذة واحدة عابرة، بلا لمس ولا سحب.
 *
 * **وحدّها المعلن:** تحتاج `SYSTEM_ALERT_WINDOW` كأيّ تراكب آخر. وفشل `addView` لا يُسقط خدمة
 * اللوحة: يُسجَّل باسمه ويبقى باقي التحكّم يعمل. ولا يُدّعى أنها تظهر فوق كل لعبة: لعبة تملك
 * نافذة ملء الشاشة بنمط خاصّ قد تحجب تراكبًا عاديًّا — وهذا يُقاس على الجهاز لا هنا.
 */
internal class GameMemoOverlay(context: Context) {

    private val appContext: Context = context.applicationContext
    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val view = MemoView(appContext)
    private var params: WindowManager.LayoutParams? = null
    private var added = false
    private var shownText = ""
    private var shownWidth = 0

    /** يعرض النصّ أو يحدّثه، ويُخفي حين يكون النصّ فارغًا (لا إطار بلا كلام). */
    fun show(memo: GameMemo) {
        val text = memo.shownText
        if (text.isEmpty()) {
            hide()
            return
        }
        fitWidth()
        if (shownText != text) {
            view.text = text
            shownText = text
        }
        if (added) return
        val lp = buildParams()
        runCatching { windowManager.addView(view, lp) }
            .onSuccess {
                params = lp
                added = true
            }
            .onFailure {
                AppMonitorLogger.w("EVENT=GAME_MEMO_OVERLAY_FAILED stage=addView", it)
            }
    }

    fun hide() {
        if (!added) return
        runCatching { windowManager.removeView(view) }
        params = null
        added = false
        shownText = ""
    }

    fun destroy() = hide()

    /**
     * أقصى عرض = ٩٢٪ من الشاشة: مذكّرة طويلة تُقصّ بـ`…` بدل أن تصير شريطًا يغطّي اللعبة.
     * ويُعاد الحساب عند تبدّل المقاس (دوران)، فلا يبقى الحدّ مقاسَ وضعٍ سابق.
     */
    private fun fitWidth() {
        val width = appContext.resources.displayMetrics.widthPixels
        if (width == shownWidth) return
        shownWidth = width
        view.maxWidth = (width * MAX_WIDTH_FRACTION).toInt()
    }

    private fun buildParams(): WindowManager.LayoutParams {
        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            // شريط الحالة يقاس لا يُفترض: النصّ يبدأ تحته لا فوقه.
            y = statusBarHeight() + dp(8)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun statusBarHeight(): Int {
        val id = appContext.resources.getIdentifier("status_bar_height", "dimen", "android")
        val measured = if (id > 0) appContext.resources.getDimensionPixelSize(id) else 0
        return if (measured > 0) measured else dp(24)
    }

    private fun dp(value: Int): Int =
        (value * appContext.resources.displayMetrics.density).toInt()

    /**
     * النصّ وحده: خلفية داكنة شبه شفّافة وحواف مستديرة، سطر واحد يُقصّ عند الحدّ.
     * والاتجاه يُؤخذ من أوّل حرف قويّ، فيُقرأ العربي يمينًا واللاتيني يسارًا في الجهاز نفسه.
     */
    private class MemoView(context: Context) : TextView(context) {

        private val density = resources.displayMetrics.density

        init {
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
            setPadding(
                (14 * density).toInt(), (9 * density).toInt(),
                (14 * density).toInt(), (9 * density).toInt()
            )
            background = GradientDrawable().apply {
                cornerRadius = 12 * density
                setColor(0xE612161B.toInt())
            }
            isClickable = false
            isFocusable = false
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        }
    }

    private companion object {
        /** أقصى عرض كنسبة من الشاشة — الباقي هامش يمنع الالتصاق بالحافّة. */
        const val MAX_WIDTH_FRACTION = 0.92f
    }
}
