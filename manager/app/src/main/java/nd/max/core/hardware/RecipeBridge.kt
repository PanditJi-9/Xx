/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/**
 * يصل [RecipeRun] بالكاتب الحقيقي وبالـarbiter — ولا يفتح ملفًا بنفسه.
 *
 * **ولماذا طبقةٌ بينهما أصلًا:** `RecipeRun` يعرف الترتيب (لقطة ← كتابة ← قراءة ← استرجاع) ولا يعرف
 * الصلاحيات ولا ملكية المقبض. و[HardwareControlArbiter] هو من يعرف: يرفض الكتابة إن كان المستخدم قد
 * قفل المقبض يدويًّا، ويسجّل الطلب في دفتره، ويمنع مالكين متعارضين من الكتابة على العقدة نفسها (ADR-11).
 * فلو كتب `RecipeRun` مباشرة لصار مالكًا رابعًا لا يعلم أحد عنه.
 *
 * **وحدوده الصادقة:**
 * 1. لا صلاحية هنا: `writeFireAndForget` تُرسل الأمر وتُسجّل «أُرسل» لا «استقرّ»، وما ترفضه النواة يبقى
 *    مرفوضًا. ومنح الجذر/Shizuku عمل مسار الصلاحيات القائم.
 * 2. `read` قد يعود `null` لعقدة لا تُقرأ؛ وحينها يُرفض الطلب في الـarbiter بـ`baseline-unreadable` بدل
 *    أن تُكتب قيمة بلا لقطة — وهذا هو الصواب: بلا لقطة لا استرجاع.
 * 3. الإفراج `restore = false` مقصود: `RecipeRun` هو من يقرّر الاسترجاع بعد قراءة التحقّق، فاسترجاع
 *    مزدوج من الطبقتين يعيد قيمة قديمة فوق استرجاع صحيح. و`release` تبقي الدفتر نظيفًا بعد الطلب.
 */
object RecipeBridge {

    /** مفتاح الملكية في الدفتر: العقدة نفسها، فلا يبقى طلبٌ معلّقًا بعد انتهاء الوصفة. */
    private fun key(path: String) = path

    /**
     * [io] جاهز للتمرير إلى `RecipeRun.run`، وكل كتابة فيه تمرّ ببوّابة الـarbiter.
     *
     * @param token معرّف الجلسة الذي يُسجَّل في الدفتر؛ استعمله ثابتًا لكل وصفة كي يُعرف من كتب.
     */
    fun nodeIo(
        arbiter: HardwareControlArbiter,
        token: String = "recipe",
    ): RecipeRun.NodeIo = object : RecipeRun.NodeIo {

        override fun read(path: String): String? = RootFileAccess.read(path)

        override fun write(path: String, value: String): Boolean {
            val desired = value.trim()
            if (desired.isEmpty()) return false
            // اللقطة تُقرأ هنا أيضًا لأن الـarbiter يرفض بلا أساس؛ وقراءتان لنفس العقدة ليستا كتابتين.
            val baseline = RootFileAccess.read(path) ?: return false
            val result = arbiter.submit(
                key = key(path),
                owner = ControlOwnership.Owner.GLOBAL_PROFILE,
                token = token,
                desired = desired,
                apply = { applied -> RootFileAccess.writeFireAndForget(path, applied) },
                read = { RootFileAccess.read(path) },
                baseline = baseline,
                restore = { previous -> RootFileAccess.writeFireAndForget(path, previous) },
            )
            return try {
                result.applied && !result.blocked
            } finally {
                // الاسترجاع مسؤولية RecipeRun بعد قراءة التحقّق؛ هنا يُغلق الطلب فقط.
                arbiter.release(key(path), token, restore = false)
            }
        }
    }
}
