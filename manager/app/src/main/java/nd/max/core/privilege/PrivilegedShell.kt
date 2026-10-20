/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.privilege

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * المنفّذ الواحد للأوامر المميّزة (`AR-20`): جذر ← Shizuku ← رفض معلن.
 *
 * **لماذا واجهة واحدة:** كل موضع كان يستدعي `Shell.cmd` مباشرةً يفترض أن الصدفة جذرية؛ وحين لا
 * تكون، تُنفَّذ الأوامر بصلاحية التطبيق العادية فتفشل بأثر جانبيّ صامت أو تبدو كأنها نُفِّذت. هنا
 * القرار صريح ومركزيّ: الجذر إن وُجد، وإلا Shizuku إن كان جاهزًا، وإلا رفض — والمستدعي يعرف بأي
 * طبقة نُفِّذ ([Outcome.Ran.lane])، فتقول الواجهة الحقيقة.
 *
 * **الحدود المعلنة:** لا ترقية امتياز داخل النداء (لا `su` من مسار Shizuku ولا العكس)، والكتابة
 * إلى sysfs لا تمرّ من هنا لأنها تحتاج مالك العتاد لا صدفة ([`nd.max.core.hardware.RootFileAccess`]).
 * وليست هذه بديلًا عن arbiter: هي طبقة **تنفيذ** لا سياسة، ومن يستعملها يبقى داخل عقده.
 */
object PrivilegedShell {

    sealed interface Outcome {
        /** نُفِّذ فعلًا في الطبقة [lane] برمز الخروج [code] (صفر = نجاح)، وبخَرْج [output]. */
        data class Ran(
            val lane: PrivilegedShellRules.Lane,
            val code: Int,
            val output: String,
        ) : Outcome {
            val ok: Boolean get() = code == 0
        }

        /** رُفض **قبل** التنفيذ: لا طبقة تسمح به. [reason] مفتاح تقنيّ، لا نصّ واجهة. */
        data class Refused(val reason: String) : Outcome

        /** حُوِّل إلى طبقة متاحة وفشل الطريق نفسه (IPC/استثناء) — والفرق عن [Refused] مقصود. */
        data class Failed(val reason: String) : Outcome
    }

    /** مفاتيح الرفض/الفشل — ثابتة لأنها تُسجَّل وتُقارن. */
    const val REASON_NO_PRIVILEGE = "no-privilege"
    const val REASON_ROOT_FAILED = "root-failed"

    /**
     * ينفّذ أمرًا واحدًا في أعلى طبقة متاحة، ويُعلن الطبقة والحكم.
     *
     * **التزام المستدعي:** يُنادى من سياق غير رئيسي (أو من `PanelToggles.async` الذي يضمن ذلك)؛
     * فالنداء ينتظر ربط الخدمة إلى مهلة صريحة، ولا يجوز أن ينتظر واجهة.
     */
    suspend fun exec(command: String): Outcome = withContext(Dispatchers.IO) {
        val snapshot = PrivilegeManager.refresh()
        when (PrivilegedShellRules.laneFor(snapshot.rootGranted, snapshot.shizuku.ready)) {
            PrivilegedShellRules.Lane.ROOT -> root(command)
            PrivilegedShellRules.Lane.SHIZUKU -> shizuku(command)
            null -> Outcome.Refused(REASON_NO_PRIVILEGE)
        }
    }

    /** مسار الجذر كما كان (libsu) — والفرق الوحيد أن رمز الخروج يُعلَن ولا يُهمَل. */
    private fun root(command: String): Outcome {
        val result = runCatching { Shell.cmd(command).exec() }.getOrNull()
            ?: return Outcome.Refused(REASON_ROOT_FAILED)
        val merged = (result.out + result.err).joinToString("\n")
        return Outcome.Ran(
            PrivilegedShellRules.Lane.ROOT,
            result.code,
            ShizukuExecRules.capOutput(merged),
        )
    }

    /** مسار Shizuku — والحكم يُترجم بنوعه لا يُسطَّح إلى نجاح/فشل. */
    private suspend fun shizuku(command: String): Outcome =
        when (val verdict = ShizukuGateway.exec(command)) {
            is ShizukuExec.Ran -> Outcome.Ran(
                PrivilegedShellRules.Lane.SHIZUKU,
                verdict.code,
                verdict.output,
            )

            is ShizukuExec.Refused -> Outcome.Refused(verdict.reason)
            is ShizukuExec.Failed -> Outcome.Failed(verdict.reason)
        }
}
