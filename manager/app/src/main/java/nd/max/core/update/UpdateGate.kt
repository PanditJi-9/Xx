/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.update

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * مفتاح «تعطيل التحديثات» — **مفتاح واحد يفتح ثلاثة أبواب**.
 *
 * **ولماذا ثلاثة:** النافذة التي يراها المستخدم قد تأتي من ثلاث قنوات لا قناة، وكل قناة لها
 * مُستضافها: قناة اللوحة المنشورة ([nd.max.ui.component.ConfigUpdateHost])، وقناة المانيفست
 * المُعلَنة في [nd.max.ui.component.UpdateSurface] (الحجب والتعليق والإعلان)، وحوار «تطبيق أقدم
 * من الوحدة» في `MainActivity`. ولو أُغلقت قناة واحدة لعادت النافذة من الأخرى، وهو عطبٌ يُشكو
 * منه المستخدم بقوله «أطفأتها ولم تختفِ».
 *
 * ## عقد الإخفاء — ما يُحجب وما لا يُحجب
 *
 * * **الظهور التلقائيّ يُحجب كلّه**: نافذة اللوحة الاختيارية، وإعلان المانيفست الاختياري/المُعلَن،
 *   وحوار «التطبيق أقدم من الوحدة». هذا هو المقصود بـ«لا تظهر نافذة التحديث المنبثقة».
 * * **الفحص اليدويّ لا يُحجب**: من ضغط «تحقّق من التحديثات» بنفسه يريد أن يرى ما وُجد (نفس قاعدة
 *   `UpdateManager.checkNow`). فالكتمان هنا كان يقول «لا جديد» وهو يعلم أن هناك جديدًا.
 * * **حجب النسخة (`UpdateVerdict.BLOCKED`) لا يُحجب**: طريق مسدود بلا نافذة = تطبيقٌ مقفل بلا سبب
 *   مرئي، والحدّ أمانٌ لا إزعاج. فالمفتاح يُسكِت الإعلان لا يُسقط الحارس.
 *
 * ## ولماذا حالة واحدة في الذاكرة لا قراءة مبعثرة
 *
 * القيمة تُقرأ مرة وتبقى [StateFlow]، فالمستضافون في الشاشات جمعوا الحالة ورأوا التبديل لحظة
 * ضغط المستخدم من شاشة التحكّم — بلا إعادة تركيب وبلا استعلام خارق. والكتابة إلى ملفّ تفضيلات
 * منفصل (`update_settings`) كي لا يدخل التحديث تفضيلات المستخدم الأخرى.
 *
 * والمنطق الخالص على الحدّين مقيس في `UpdateGateContractTest` (سلوك) و`UpdateGateSourceContractTest`
 * (أبواب الثلاثة في مصادرها).
 */
object UpdateGate {

    private const val PREFS = "update_settings"
    private const val KEY_DISABLED = "updates_disabled"

    private val _disabled = MutableStateFlow(false)
    val disabled: StateFlow<Boolean> = _disabled.asStateFlow()

    /**
     * القيمة المخزّنة — ويُسخَّن الحَمل بين العمليات: أول قراءة تملأ [disabled] كي لا يبدأ
     * المستضاف بمفتاحٍ افتراضيّ خاطئٍ ثم يتغيّر عند أول استعلام.
     */
    fun isDisabled(context: Context): Boolean {
        val stored = prefs(context).getBoolean(KEY_DISABLED, false)
        _disabled.value = stored
        return stored
    }

    fun setDisabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_DISABLED, value).apply()
        _disabled.value = value
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
