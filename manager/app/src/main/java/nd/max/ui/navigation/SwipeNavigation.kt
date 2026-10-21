/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.navigation

import kotlin.math.abs

/**
 * اتجاه التمرير الأفقي بعد ترجمته إلى **معنى** لا إلى إشارة بكسل.
 *
 * [NEXT] يعني «الصفحة/التبويب التالي في ترتيب القراءة»، و[PREVIOUS] يعني السابق.
 * الفرق بين LTR وRTL يُحسم في [resolveSwipeDirection] مرة واحدة، فلا تكرّر كل شاشة
 * شرطًا على اتجاه التخطيط.
 */
enum class SwipeDirection {
    NEXT,
    PREVIOUS,
    NONE,
}

/**
 * حكم التمرير من قياس خام: المسافة والسرعة والاتجاه.
 *
 * **ولماذا دالّة نقيّة:** القرار الذي يقع في كل لمسة يجب أن يُقاس على JVM بلا جهاز
 * وبلا Compose. فالاختبار هنا يثبت أن السحب في العربية معكوس عن الإنجليزية، وأن
 * السحب الرأسي لا يُحوَّل إلى تنقّل، وأن النقرة القصيرة لا تكفي.
 *
 * @param dx إزاحة الإصبع الأفقية بالبكسل (موجبة = يمين).
 * @param dy إزاحة الإصبع الرأسية بالبكسل.
 * @param velocityX سرعة الإصبع الأفقية بالبكسل/الثانية.
 * @param minDistancePx أقل مسافة مقبولة للسحب البطيء.
 * @param minVelocityPxPerSecond أقل سرعة مقبولة للقفزة القصيرة.
 * @param isRtl هل التخطيط من اليمين إلى اليسار؟
 */
fun resolveSwipeDirection(
    dx: Float,
    dy: Float,
    velocityX: Float,
    minDistancePx: Float,
    minVelocityPxPerSecond: Float,
    isRtl: Boolean,
): SwipeDirection {
    val horizontal = abs(dx)
    // ولا يكفي أن تكون السرعة عالية: الإصبع الذي تحرّك رأسيًّا ثم انزلق أفقيًّا
    // قليلًا في نهايته ليس سحبًا أفقيًّا.
    if (horizontal <= abs(dy) * SWIPE_SLOPE_RATIO) return SwipeDirection.NONE
    val fastEnough = abs(velocityX) >= minVelocityPxPerSecond
    if (horizontal < minDistancePx && !fastEnough) return SwipeDirection.NONE
    if (dx == 0f) return SwipeDirection.NONE

    // في LTR: السحب لليسار يكشف التالي. وفي RTL: السحب لليمين يكشف التالي.
    val towardNext = if (isRtl) dx > 0f else dx < 0f
    return if (towardNext) SwipeDirection.NEXT else SwipeDirection.PREVIOUS
}

/**
 * أقل ميل مقبول: الأفقي يجب أن يتجاوز الرأسي بنسبة ١٫٤. أقل من ذلك تمريرٌ مائل
 * يُترك للقائمة الرأسية كما هو.
 */
private const val SWIPE_SLOPE_RATIO = 1.4f

/**
 * ما تسجّله شاشة ذات تبويبات عند الجذر: ماذا تفعل حين يُسحب المحتوى.
 *
 * الإرجاع `true` يعني «تولّيت الأمر»، و`false` يعني «لا تبويب في هذا الاتجاه»،
 * فيعود القرار إلى الجذر: قسم رئيسي أو رجوع.
 */
data class SwipeNavigationHandlers(
    val onNext: (() -> Boolean)? = null,
    val onPrevious: (() -> Boolean)? = null,
)

/**
 * سجلّ الشاشة التي تملك التمرير الآن.
 *
 * الشاشات تُسجّل عند `ON_RESUME` وتنزع عند `ON_PAUSE` (انظر `RegisterSwipeNavigation`)،
 * فلا تبقى شاشة خرجت من الواجهة تملك إيماءة الشاشة التي جاءت بعدها. والجذر يقرأ
 * `current` **لحظة الإيماءة** لا لحظة التركيب، فلا حاجة إلى إعادة تركيب عند كل تسجيل.
 *
 * كل هذا على خيط الواجهة وحده؛ لا أقفال ولا مشاركة بين خيوط.
 */
class SwipeNavigationController {
    private class Registration(val handlers: SwipeNavigationHandlers)

    private val registrations = mutableListOf<Registration>()

    val current: SwipeNavigationHandlers?
        get() = registrations.lastOrNull()?.handlers

    fun register(handlers: SwipeNavigationHandlers): Any {
        val registration = Registration(handlers)
        registrations.add(registration)
        return registration
    }

    fun unregister(token: Any) {
        registrations.removeAll { it === token }
    }
}
