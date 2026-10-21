/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.navigation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * الشاشة التي تملك التمرير الآن. يُوفّره جذر التطبيق، وتسجّل فيه الشاشات ذات
 * التبويبات عند دخولها. وغيابه (معاينة، اختبار) يعني «لا إيماءة» لا انهيار.
 */
val LocalSwipeNavigationController = compositionLocalOf<SwipeNavigationController?> { null }

/**
 * تسجيل تبويبات الشاشة عند الجذر.
 *
 * **ولماذا عند `ON_RESUME` لا عند التركيب:** أثناء انتقال الملاحة تُركَّب شاشتان معًا
 * لحظةً. لو سجّلتا معًا لكانت آخر واحدة تُركَّب هي التي تملك الإيماءة، وقد تكون
 * الشاشة الخارجة. ربط التسجيل بدورة حياة وجهة الملاحة يجعل المالك واحدًا: الشاشة
 * التي يراها المستخدم فعلًا.
 *
 * @param onNext يُرجع `true` إن انتقل إلى التبويب التالي، و`false` عند آخره.
 * @param onPrevious يُرجع `true` إن انتقل إلى التبويب السابق، و`false` عند أوله.
 */
@Composable
fun RegisterSwipeNavigation(
    onNext: (() -> Boolean)? = null,
    onPrevious: (() -> Boolean)? = null,
) {
    val controller = LocalSwipeNavigationController.current ?: return
    val latestNext by rememberUpdatedState(onNext)
    val latestPrevious by rememberUpdatedState(onPrevious)
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        var token: Any? = null

        fun register() {
            if (token != null) return
            token = controller.register(
                SwipeNavigationHandlers(
                    onNext = { latestNext?.invoke() == true },
                    onPrevious = { latestPrevious?.invoke() == true },
                )
            )
        }

        fun unregister() {
            token?.let(controller::unregister)
            token = null
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> register()
                Lifecycle.Event.ON_PAUSE -> unregister()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            register()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unregister()
        }
    }
}

/**
 * مستشعر التمرير الأفقي في **جذر** التطبيق.
 *
 * **لماذا في الجذر:** كل شاشة تمرّ من `NavHost` واحد، فالقياس في مكان واحد يغطّي
 * الشاشات كلها — بما فيها الشاشات القديمة التي لم تُبنَ على `MaxScreen` — بلا أن
 * نضيف معاملًا إلى كل توقيع.
 *
 * **وحدّه المقصود:** لا يستهلك الإيماءة. يقرأها في `Final` بعد أن مرّت على الأطفال،
 * فإن كان طفلٌ قد استهلكها (قائمة أفقية · شريط تمرير · Carousel · معادل صوتي)
 * تُركت له. فالسحب على عنصر أفقي لا يقلب الصفحة من تحت إصبع المستخدم.
 */
@Composable
fun Modifier.swipeNavigation(
    enabled: Boolean,
    onSwipe: (SwipeDirection) -> Unit,
): Modifier {
    val latestOnSwipe by rememberUpdatedState(onSwipe)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    return pointerInput(enabled, isRtl) {
        if (!enabled) return@pointerInput
        val minDistancePx = 64.dp.toPx()
        val minVelocityPxPerSecond = 320.dp.toPx()
        val touchSlop = viewConfiguration.touchSlop

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val velocityTracker = VelocityTracker()
            velocityTracker.addPosition(down.uptimeMillis, down.position)
            var lastPosition = down.position
            var consumedAfterSlop = false
            var multiTouch = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                if (event.changes.size > 1) multiTouch = true
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                lastPosition = change.position

                // لا نحسب استهلاك «الضغط» نفسه: كل زرّ يستهلكه ليعرض أثره، ومع ذلك
                // نريد أن يعمل السحب الذي يبدأ فوقه. ننتظر تجاوز حدّ اللمس أولًا.
                if (!consumedAfterSlop && change.pressed && change.isConsumed) {
                    val travelled = (change.position - down.position).getDistance()
                    if (travelled > touchSlop) consumedAfterSlop = true
                }

                velocityTracker.addPosition(change.uptimeMillis, change.position)
                if (!change.pressed) break
            }

            if (!multiTouch && !consumedAfterSlop) {
                val dx = lastPosition.x - down.position.x
                val dy = lastPosition.y - down.position.y
                val direction = resolveSwipeDirection(
                    dx = dx,
                    dy = dy,
                    velocityX = velocityTracker.calculateVelocity().x,
                    minDistancePx = minDistancePx,
                    minVelocityPxPerSecond = minVelocityPxPerSecond,
                    isRtl = isRtl,
                )
                if (direction != SwipeDirection.NONE) latestOnSwipe(direction)
            }
        }
    }
}
