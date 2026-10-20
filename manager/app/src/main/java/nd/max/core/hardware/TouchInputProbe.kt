/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.view.InputDevice

/** سياق الجهاز الذي يُعرض مع هوية المحكّم — بيانات نظام لا استنتاج. */
data class TouchDeviceContext(
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val sdk: Int,
) {
    /** سطر واحد للعرض: الشركة والطراز وإصدار Android — بلا رمز بريديّ ولا معرّف فريد. */
    val label: String get() = "$manufacturer $model · Android $androidRelease (API $sdk)"
}

/**
 * **قراءة أجهزة اللمس من الواجهة العامّة لـAndroid — بلا جذر وبلا صلاحية.**
 *
 * `InputManager` + `InputDevice` هما المصدر الرسميّ لأجهزة الإدخال المعلنة للنظام. منها نقرأ
 * اسم جهاز اللمس ومعرّفَي البائع/المنتج والبصمة — **قراءة فقط**، ولا يُكتب في أيّ منها.
 *
 * **وحدّه المعلن:** هذه الواجهة تصف ما يعلنه Android، لا ما تحقّقه الوحدة. وحدة قد يخفيها الروم
 * أو يعيد تسميتها؛ والمجهول يبقى مجهولًا (انظر [TouchControllerClassifier]). ولا يوفّر Android
 * هنا معدّل أخذ العيّنات ولا الحدّ الموثّق — لذلك تبقى قناتهما `null` في [TouchRateChannels].
 */
object TouchInputProbe {

    /**
     * كل جهاز إدخال يعلن مصدر لمس (شاشة لمس).
     *
     * والفشل (خدمة غائبة/استثناء نظام) يعيد قائمة فارغة لا استثناءً: الشاشة تعرض «لا شاشة لمس
     * معلنة» بدل أن تنهار. والقائمة الفارغة ليست «صفر أجهزة» مُدَّعى — هي «لم يُقرأ شيء».
     */
    fun touchDevices(context: Context): List<TouchInputDevice> = runCatching {
        val manager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
            ?: return emptyList()
        // `.toList()` أولًا: `IntArray` لا تحمل `mapNotNull` (وهي على `Iterable`/`Array` وحدها).
        manager.inputDeviceIds.toList().mapNotNull { id ->
            runCatching { manager.getInputDevice(id) }.getOrNull()
        }.filter { device ->
            device.sources and InputDevice.SOURCE_TOUCHSCREEN != 0
        }.map { device ->
            TouchInputDevice(
                name = device.name.orEmpty(),
                vendorId = device.vendorId,
                productId = device.productId,
                descriptor = runCatching { device.descriptor }.getOrNull().orEmpty(),
            )
        }
    }.getOrDefault(emptyList())

    /** سياق الجهاز للعرض — يُقرأ من `Build` مرة واحدة عند فتح الشاشة. */
    fun deviceContext(): TouchDeviceContext = TouchDeviceContext(
        manufacturer = Build.MANUFACTURER.orEmpty(),
        model = Build.MODEL.orEmpty(),
        androidRelease = Build.VERSION.RELEASE.orEmpty(),
        sdk = Build.VERSION.SDK_INT,
    )
}
