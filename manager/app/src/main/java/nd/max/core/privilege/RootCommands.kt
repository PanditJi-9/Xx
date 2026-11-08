/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential. See LICENSE at the repository root.
 */
package nd.max.core.privilege

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * بوابة الأوامر الجذرية، لا مسارًا بديلًا إلى Shizuku. إنشاء المهمة لا يفتح `su`.
 * يُفحص الاختيار عند التنفيذ وبعد الحصول على الصدفة؛ وما بدأ بالفعل لا يمكن التراجع عنه
 * بمجرّد تغيير الوضع. كتابة العتاد تظلّ عبر الـarbiter وRootFileAccess.
 */
object RootCommands {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // نمتلك الانتظار قبل صفّ libsu، حتى يُعاد فحص الوضع بعده لا قبله فقط.
    private val commandLock = Any()

    data class Result(val out: List<String>, val err: List<String>, val code: Int) {
        val isSuccess: Boolean get() = code == 0
    }

    fun cmd(vararg commands: String): Job = Job(commands.toList(), ExecutionModeStore.generation())

    class Job internal constructor(private val commands: List<String>, private val generation: Long) {
        private var output: MutableList<String>? = null
        private var error: MutableList<String>? = null
        private var redirected = false

        fun to(output: MutableList<String>?, error: MutableList<String>? = null): Job = apply {
            this.output = output
            this.error = error
            redirected = true
        }

        private fun permitted(): Boolean = ExecutionModeRules.mayDispatch(
            ExecutionModeStore.current(), generation, ExecutionModeStore.generation(),
            PrivilegedShellRules.Lane.ROOT,
        )

        fun exec(): Result = synchronized(commandLock) {
            if (!permitted()) return refused()
            return runCatching {
                val shell = Shell.getShell()
                // الحصول على su قد ينتظر نافذة إذن؛ لا تُرسل المهمة بعد تبديل الوضع أثناء الانتظار.
                if (!permitted() || !shell.isRoot) return refused()
                val job = shell.newJob().add(*commands.toTypedArray())
                if (redirected) job.to(output, error) else job.to(ArrayList(), ArrayList())
                val result = job.exec()
                Result(result.out.toList(), result.err.toList(), result.code)
            }.getOrElse { Result(emptyList(), listOf("root-execution-failed"), -1) }
        }

        fun submit(callback: ((Result) -> Unit)? = null) {
            scope.launch {
                val result = exec()
                if (callback != null) withContext(Dispatchers.Main) { callback(result) }
            }
        }

        private fun refused() = Result(emptyList(), listOf("mode-root-blocked"), -1)
    }
}
