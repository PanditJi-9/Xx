/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.input

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * حقن اللمس عبر خدمة الوصول المفعّلة — **بلا جذر**، وبإذن المستخدم الصريح.
 *
 * ### لماذا خدمة الوصول
 *
 * `dispatchGesture` هي الطريق الرسميّ الذي يمنحه Android لتوليد لمسة من تطبيق وصول مفعّل.
 * لا تحتاج جذرًا ولا Shizuku، لكنها تحتاج أن يفعّل المستخدم الخدمة من إعدادات النظام — وهذا
 * شرط يُعرض في الواجهة، لا يُفترض.
 *
 * ### العقد
 *
 * الخدمة تُسجّل نفسها هنا عند الاتصال وتُزيل نفسها عند الانفصال، فلا يبقى مرجع ميّت. والحقن
 * يُنفَّذ على الخيط الرئيسيّ كما يشترط Android، وينتظر نداء النتيجة (اكتمال/إلغاء) بمهلة
 * صريحة — ولا يُقال «نجح» قبل `onCompleted`.
 */
object A11yGestureInjector {

    private const val GESTURE_TIMEOUT_MS = 1_500L
    private const val TAP_DURATION_MS = 60L
    private const val MAX_DURATION_MS = 1_500L

    @Volatile
    private var service: AccessibilityService? = null

    /** هل الخدمة متّصلة الآن؟ الواجهة تعرض «المسار متاح» بناءً عليه. */
    val available: Boolean get() = service != null

    fun attach(service: AccessibilityService) {
        this.service = service
    }

    fun detach(service: AccessibilityService) {
        if (this.service === service) this.service = null
    }

    suspend fun tap(x: Float, y: Float): InjectionOutcome = dispatch(
        fromX = x, fromY = y, toX = x, toY = y, durationMs = TAP_DURATION_MS
    )

    suspend fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Int): InjectionOutcome = dispatch(
        fromX = x1, fromY = y1, toX = x2, toY = y2,
        durationMs = durationMs.toLong()
            .coerceIn(TouchInjectionRules.MIN_SWIPE_MS.toLong(), MAX_DURATION_MS)
    )

    private suspend fun dispatch(
        fromX: Float, fromY: Float, toX: Float, toY: Float, durationMs: Long,
    ): InjectionOutcome = withContext(Dispatchers.Main) {
        val attached = service ?: return@withContext InjectionOutcome.Refused(InjectionRefusal.NO_LANE)
        val path = Path().apply {
            moveTo(fromX, fromY)
            lineTo(toX, toY)
        }
        val stroke = GestureDescription.StrokeDescription(
            path, 0L, durationMs.coerceIn(1L, MAX_DURATION_MS)
        )
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        val completed = withTimeoutOrNull(GESTURE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
                val accepted = runCatching {
                    attached.dispatchGesture(gesture, callback, null)
                }.getOrDefault(false)
                if (!accepted && continuation.isActive) continuation.resume(false)
            }
        }
        when (completed) {
            true -> InjectionOutcome.Delivered(InjectionLane.ACCESSIBILITY)
            false -> InjectionOutcome.Failed(InjectionLane.ACCESSIBILITY, "gesture-cancelled")
            null -> InjectionOutcome.Failed(InjectionLane.ACCESSIBILITY, "gesture-timeout")
        }
    }
}
