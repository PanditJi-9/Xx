/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/**
 * تنفيذ خطة وصفة **حكمها مقيس لا مُدَّعى**: قراءة قبل، كتابة، قراءة بعد، ومقارنة.
 *
 * **ولماذا لا تكتب الخطة نفسها:** [RecipePlan] تقرّر فقط. أمّا التنفيذ فيحتاج قدرة على القراءة
 * والكتابة في العتاد، وهي القدرة التي تُمرَّر من الخارج — لا تُفتح هنا. ولذلك كل شيء في هذا
 * الملف يُقاس بواجهة [NodeIo] مزيفة في الاختبار، بلا جهاز وبلا جذر.
 *
 * **وقاعدة التراجع:** لا تُترك كتابة بلا ملكية. إن اختلفت القراءة بعد الكتابة فالعقدة تُعاد إلى
 * قيمتها السابقة، وإن كانت السابقة **غير مقروءة** فالحالة تُعلن `RESTORE_FAILED` ولا يُقال
 * «تراجعنا» كذبًا. وأي خطوة تُفشل الوصفة تُوقف البقية وتعيد ما سبق كتابته بالعكس.
 *
 * **وحدّها الصادق:** الترجيح بين عمليتين متزامنتين على العقدة نفسها ليس هنا — هذا عمل
 * `HardwareControlArbiter` (ADR-11)، والمنفّذ لا يفتح ملفًا بنفسه ولا يعرف صلاحيةً.
 */
object RecipeRun {

    /** القدرة على لمس العتاد. تُنفَّذ خارج هذا الملف بقناة الجذر أو الصدفة. */
    interface NodeIo {
        fun read(path: String): String?
        fun write(path: String, value: String): Boolean
    }

    enum class Outcome {
        /** كُتبت القيمة وقُرئت كما هي. */
        APPLIED,

        /** كُتبت ثم اختلفت القراءة، فأُعيدت القيمة السابقة. */
        MISMATCH_RESTORED,

        /** كُتبت ثم اختلفت، ولا قيمة سابقة تُعاد (كانت غير مقروءة). */
        RESTORE_FAILED,

        /** رفضت الكتابة من الأساس. */
        WRITE_FAILED,

        /** خطوة سابقة أُعيدت بعد فشل خطوة لاحقة. */
        ROLLED_BACK,

        /** تعذّرت إعادة خطوة سابقة. */
        ROLLBACK_FAILED,

        /** خطة غير قابلة للتنفيذ — لم تُلمس العقدة. */
        BLOCKED,
    }

    data class StepResult(
        val step: RecipePlan.Step,
        val outcome: Outcome,
        val before: String? = null,
        val after: String? = null,
    )

    data class Report(val results: List<StepResult>) {
        /** نجحت الوصفة كاملةً: خطوة واحدة على الأقل، وكلّها `APPLIED`. */
        val ok: Boolean get() = results.isNotEmpty() && results.all { it.outcome == Outcome.APPLIED }
        val applied: Int get() = results.count { it.outcome == Outcome.APPLIED }
        val rolledBack: Int
            get() = results.count { it.outcome == Outcome.ROLLED_BACK || it.outcome == Outcome.MISMATCH_RESTORED }
        val failed: Int
            get() = results.count {
                it.outcome == Outcome.WRITE_FAILED ||
                    it.outcome == Outcome.RESTORE_FAILED ||
                    it.outcome == Outcome.ROLLBACK_FAILED
            }
    }

    fun run(plan: RecipePlan.Plan, io: NodeIo): Report {
        // مسار فارغ يعني خطوة لم تُحلّ إلى عقدة؛ ولا تُلمس.
        if (!plan.applicable || plan.steps.any { it.path == null }) {
            return Report(plan.steps.map { StepResult(it, Outcome.BLOCKED) })
        }
        val done = mutableListOf<StepResult>()
        for (step in plan.steps) {
            val path = step.path ?: continue
            val before = io.read(path)
            if (!io.write(path, step.value)) {
                done.add(StepResult(step, Outcome.WRITE_FAILED, before))
                return Report(done + rollback(done, io))
            }
            val after = io.read(path)
            if (!sameValue(after, step.value)) {
                val restored = before != null && io.write(path, before)
                done.add(
                    StepResult(
                        step = step,
                        outcome = if (restored) Outcome.MISMATCH_RESTORED else Outcome.RESTORE_FAILED,
                        before = before,
                        after = after,
                    )
                )
                return Report(done + rollback(done, io))
            }
            done.add(StepResult(step, Outcome.APPLIED, before, after))
        }
        return Report(done)
    }

    /**
     * يعيد الخطوات المطبَّقة بالعكس — والخطوة الفاشلة نفسها ليست هنا لأن حالتها كُتبت في موضعها،
     * وإعادتها ثانيةً تجعل التقرير يقول إننا تراجعنا عن كتابة واحدة مرّتين.
     *
     * والقيمة السابقة `null` تعني «لم نقرأها»، فلا تُعاد عقدة إلى قيمة لم نرها (`ROLLBACK_FAILED`).
     */
    private fun rollback(done: List<StepResult>, io: NodeIo): List<StepResult> =
        done.asReversed()
            .filter { it.outcome == Outcome.APPLIED }
            .map { applied ->
                val path = applied.step.path
                val before = applied.before
                val restored = path != null && before != null && io.write(path, before)
                applied.copy(outcome = if (restored) Outcome.ROLLED_BACK else Outcome.ROLLBACK_FAILED)
            }

    private fun sameValue(actual: String?, wanted: String): Boolean =
        actual != null && actual.trim() == wanted.trim()
}
