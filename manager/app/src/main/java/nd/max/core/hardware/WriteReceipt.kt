/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.hardware

/**
 * الطريق الذي نجحت عليه الكتابة فعليًّا.
 *
 * **ولماذا يُعلن:** `RootFileAccess` يجرّب ثلاث طبقات (خادم ← binder ← صدفة جذر)، وكان يعيد
 * `true/false` فقط. فسؤال «بأيّ امتياز كُتب هذا المقبض؟» — وهو جوهر المرحلة الثانية
 * (`PrivilegeBroker` · `ARCHITECTURE-EVOLUTION-NOV26` §3) — لم يكن له جواب. الآن الجواب يُعاد
 * مع الكتابة، بلا تغيير في سلوك أي طبقة.
 *
 * والترتيب هو ترتيب المحاولة نفسه، لا ترتيب أهمّية: الخادم يكتب بنطاقه مع رقصة `chmod` لعقد
 * HyperOS 0444، ثم binder، ثم صدفة الجذر.
 */
enum class WriteRoute { DAEMON, BINDER, ROOT_SHELL }

/**
 * نتيجة محاولة كتابة واحدة على مستوى المسار (بلا قراءة تحقّق — تلك طبقة أعلى).
 *
 * @property route الطريق الفائز؛ `null` حين فشلت كلها.
 * @property ok نجاح أمر الكتابة.
 * @property failed كل طريق جُرِّب وفشل أو استثنى نفسه — بترتيب المحاولة، فلا يُخفي السجلّ طبقة.
 */
internal data class WriteAttempt(
    val route: WriteRoute?,
    val ok: Boolean,
    val failed: List<WriteRoute> = emptyList(),
)

/**
 * اختيار الطريق — دالّة نقيّة تُقاس بالحقن، لا تُلمس بها أندرويد.
 *
 * **والعطب الذي أُغلق هنا (مقيس):** كان الاستثناء من طبقة الخادم يُسقط الكتلة كلها، فلا تُجرَّب
 * طبقة binder ولا الصدفة — أي أن أعطال `DaemonIpc` وحدها كانت تمنع الكتابة التي يعرف التطبيق
 * طريقها. الآن كل طبقة في `runCatching` وحدها: فشلها يُسجَّل ويُكمل، لا يُسقط ما بعدها.
 */
internal object WriteRouting {

    fun run(
        daemon: () -> Boolean,
        binder: () -> Boolean?,
        shell: () -> Boolean,
    ): WriteAttempt {
        val failed = mutableListOf<WriteRoute>()

        if (runCatching(daemon).getOrNull() == true) {
            return WriteAttempt(WriteRoute.DAEMON, ok = true, failed = failed.toList())
        }
        failed += WriteRoute.DAEMON

        if (runCatching(binder).getOrNull() == true) {
            return WriteAttempt(WriteRoute.BINDER, ok = true, failed = failed.toList())
        }
        failed += WriteRoute.BINDER

        val shellOk = runCatching(shell).getOrDefault(false)
        if (shellOk) return WriteAttempt(WriteRoute.ROOT_SHELL, ok = true, failed = failed.toList())
        failed += WriteRoute.ROOT_SHELL
        return WriteAttempt(route = null, ok = false, failed = failed.toList())
    }
}

/**
 * إيصال كتابة كامل: الطريق، وحكم القيمة، وما قُرئ بعد الكتابة.
 *
 * **وهو الشكل الذي تطلبه المرحلة الثانية:** كل كتابة تحمل `tier` و`before/after` — و`before`
 * يقرأه المتصل حين يهمّه، لأن قراءته على كل كتابة تُضاعف كلفة المسار بلا فائدة.
 */
data class WriteReceipt(
    val path: String,
    val wrote: String,
    val readBack: String?,
    val outcome: WriteVerification.Outcome,
    val route: WriteRoute?,
) {
    val ok: Boolean get() = outcome != WriteVerification.Outcome.WRITE_FAILED
}
