/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.input

/** جهة الزرّ. */
enum class PadSide { LEFT, RIGHT }

/**
 * موضع الزرّين كنسبة من الشاشة (٠–١) — لا بكسل مطلق، فلا يضيع الموضع عند تبدّل الدقّة
 * أو الدوران. القيمة الافتراضيّة على الحافتين في الثلث السفليّ حيث تعيش أزرار الكتف عادةً.
 */
data class ShoulderPadLayout(
    val leftX: Float = 0.06f,
    val leftY: Float = 0.70f,
    val rightX: Float = 0.94f,
    val rightY: Float = 0.70f,
)

/**
 * حساب موضع الزرّ وحدوده — نقيّ ليُقاس بلا جهاز.
 *
 * النسبة تُقاس على **المساحة المتاحة** (`screen - pad`) لا على الشاشة كاملة، فالزرّ عند
 * النسبة ١ يبقى داخل الشاشة كاملًا ولا يُقصّ نصفه.
 */
object ShoulderPadMath {

    // النسبة تقع على المساحة المتاحة (`screen - pad`) لا على الشاشة كاملة؛ فالصفر يعني
    // ملاصقة الحافة مع بقاء الزرّ كاملًا مرئيًّا، والواحد يعني الحافة الأخرى. وكانت ٠٫٠٢/٠٫٩٨
    // تمنع الوصول إلى الحافتين بلا سبب، وقد أسقطها `ShoulderPadMathTest` فعلًا.
    const val MIN_FRACTION = 0f
    const val MAX_FRACTION = 1f

    fun clampFraction(value: Float): Float =
        if (!value.isFinite()) MIN_FRACTION else value.coerceIn(MIN_FRACTION, MAX_FRACTION)

    fun centerPx(fraction: Float, screenPx: Int, padPx: Int): Int {
        val room = (screenPx - padPx).coerceAtLeast(0)
        return (clampFraction(fraction) * room).toInt().coerceIn(0, room)
    }

    fun fractionOf(positionPx: Int, screenPx: Int, padPx: Int): Float {
        val room = (screenPx - padPx).coerceAtLeast(1)
        return clampFraction(positionPx.toFloat() / room)
    }

    fun normalized(layout: ShoulderPadLayout): ShoulderPadLayout = ShoulderPadLayout(
        leftX = clampFraction(layout.leftX),
        leftY = clampFraction(layout.leftY),
        rightX = clampFraction(layout.rightX),
        rightY = clampFraction(layout.rightY),
    )

    /** مركز الزرّ داخل نافذته = موضع النافذة + نصف الحجم؛ وهذا ما تُحقن عنده اللمسة. */
    fun tapX(windowX: Int, padPx: Int): Float = windowX + padPx / 2f

    fun tapY(windowY: Int, padPx: Int): Float = windowY + padPx / 2f
}
