/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.privilege

import kotlinx.coroutines.runBlocking

/**
 * الجسر المتزامن لأوامر **المنصّة** فوق [PrivilegedShell] (`AR-20`).
 *
 * **المشكلة التي يحلّها:** ميزات الكثافة (`wm`) وتجميد الحزم (`pm`) و`AppOps` و`Doze`
 * وقراءة الإطارات تعمل بمستوى ADB أصلًا، لكنها كانت تستدعي `RootCommands` مباشرةً — فتبدو
 * معطّلة في وضع Shizuku رغم أن المنصّة تسمح بها. هذا الجسر يُمرّرها عبر الطبقة المتاحة
 * **دون** أن يفتح لها كتابة العتاد: [PlatformCommandRules] ترفض كل ما يمسّ `/sys` أو `/proc`
 * أو يستدعي `su`، فيبقى الحدّ بين «أمر منصّة» و«أمر مالك عتاد» صريحًا.
 *
 * **لماذا متزامنًا:** المستهلكون الحاليون دوالٌّ متزامنة تُنادى من `Dispatchers.IO` أصلًا
 * (ViewModels وشاشات)، وتحويلها كلها إلى `suspend` كان سيوسّع الـdiff بلا مكسب. و`runBlocking`
 * هنا لا يحجب الواجهة لأن كل مواضع النداء خارج الخيط الرئيسي — وهذا **شرط مستدعٍ** مكتوب،
 * لا افتراضًا صامتًا. ومن احتاج مسارًا معلّقًا فليستعمل [PrivilegedShell.exec] مباشرةً.
 *
 * **الحدّ المعلن:** في وضع Shizuku لا ترقية إلى جذر ولا العكس؛ والاختيار يُقرأ عند كل نداء،
 * فلا تُنفَّذ مهمة أُرسلت قبل تبديل الوضع بعد تبديله.
 */
object PlatformShell {

    /** حكم تنفيذ واحد: رمز الخروج والخَرْج المدمج (خَرْج + خطأ). */
    data class Outcome(val code: Int, val output: String) {
        val ok: Boolean get() = code == 0
        val lines: List<String> get() = output.lines().map(String::trim)
    }

    /**
     * ينفّذ أوامر متتابعة في نداء واحد، ويعود بـ`null` إن لم توجد طبقة تسمح بها
     * أو رفضتها القاعدة. والرفض **قبل** التنفيذ، لا ترجمةً لفشل لاحق.
     */
    fun exec(vararg commands: String): Outcome? {
        if (commands.isEmpty()) return null
        if (commands.any { !PlatformCommandRules.isPlatformCommand(it) }) return null

        val mode = ExecutionModeStore.current()
        val generation = ExecutionModeStore.generation()
        val snapshot = PrivilegeManager.refresh()
        val lane = ExecutionModeRules.lane(mode, snapshot.rootGranted, snapshot.shizuku.ready) ?: return null
        if (!ExecutionModeRules.mayDispatch(mode, generation, ExecutionModeStore.generation(), lane)) return null

        return when (lane) {
            PrivilegedShellRules.Lane.ROOT -> root(commands)
            PrivilegedShellRules.Lane.SHIZUKU -> shizuku(commands)
        }
    }

    /** مخرج مُشذَّب السطور عند النجاح فقط — نفس دلالة `RootShell.run` التي كان المستهلكون يقرأونها. */
    fun run(vararg commands: String): List<String>? = exec(*commands)?.takeIf { it.ok }?.lines

    private fun root(commands: Array<out String>): Outcome? {
        val result = runCatching { RootCommands.cmd(*commands).exec() }.getOrNull() ?: return null
        // الرفض المعلن من بوابة الجذر ليس فشل أمر: هو «لا طبقة الآن».
        if (result.code == -1 && result.err.firstOrNull() == "mode-root-blocked") return null
        val merged = (result.out + result.err).joinToString("\n")
        return Outcome(result.code, ShizukuExecRules.capOutput(merged))
    }

    /**
     * مسار Shizuku: الأوامر تُضمّ بأسطر كاملة كما كانت تُكتب في مدخل صدفة libsu، ثم تُنفَّذ
     * بـ`sh -c` واحد. والضمّ بأسطر لا بـ`;` مقصود: أمر فيه `;` داخليّ يبقى كما هو.
     */
    private fun shizuku(commands: Array<out String>): Outcome? {
        val joined = commands.joinToString("\n")
        return when (val verdict = runBlocking { ShizukuGateway.exec(joined) }) {
            is ShizukuExec.Ran -> Outcome(verdict.code, verdict.output)
            is ShizukuExec.Refused -> null
            is ShizukuExec.Failed -> Outcome(ShizukuExecRules.CODE_LAUNCH_FAILED, verdict.reason)
        }
    }
}
