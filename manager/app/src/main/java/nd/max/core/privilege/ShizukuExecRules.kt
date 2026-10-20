/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.privilege

/**
 * حدود تنفيذ الأوامر عبر خدمة المستخدم — **نقيّة فتُختبر بلا جهاز**، ويقرأها طرفا العقد معًا
 * (الخدمة في عملية Shizuku والتطبيق في عمليته) فلا تفترق نسختان من الحدّ نفسه.
 *
 * وكلُّ حدٍّ هنا له سبب يُقاس، لا ذوق:
 *   • [MAX_COMMAND_CHARS]: أمر أطول من ذلك خطأ برمجيّ في المستدعي، فيُرفض قبل أن يُدفع ثمن
 *     معاملة binder له.
 *   • [MAX_OUTPUT_CHARS]: خَرْج أطول يُقتطع **بعلامة ظاهرة** ([TRUNCATION_MARK])، فلا يوهم
 *     مستدعيًا بأنه قرأ الكلّ — والاقتطاع في الطرفين لا في الطرف البعيد وحده.
 *   • [BIND_TIMEOUT_MS]: أول ربط يُطلق عملية جديدة، فهو أبطأ من اللاحق؛ والمهلة تمنع تعليقًا أبديًّا.
 *   • [EXEC_TIMEOUT_SECONDS]: أمر معلّق (`top`، `logcat`) لا يجوز أن يعلّق الجسر إلى الأبد.
 */
object ShizukuExecRules {

    /** إصدار عقد [IShizukuShell] كما يعرفه خادم Shizuku؛ رفعُه يطلب إعادة تشغيل الخدمة. */
    const val SERVICE_VERSION = 1

    /** أقصى طول أمر مقبول — أبعدُه خطأ في المستدعي لا حالة مستخدم. */
    const val MAX_COMMAND_CHARS = 4096

    /** أطول خَرْج يُعاد عبر binder (حرفًا). الباقي يُقتطع بعلامة، والقراءة تستمرّ فلا ينحبس الأمر. */
    const val MAX_OUTPUT_CHARS = 65_536

    /** علامة اقتطاع **تقنية** (لا نصّ واجهة): تُقرأ بآلة، وتُعرَض على المستخدم من واجهة تُعرّبها. */
    const val TRUNCATION_MARK = "\n[output truncated]"

    /** مهلة انتظار ربط الخدمة بالمِلّي ثانية. */
    const val BIND_TIMEOUT_MS = 8_000L

    /** مهلة تنفيذ الأمر الواحد بالثواني — بعدها يُقتل الأمر ويُعلن [CODE_TIMEOUT]. */
    const val EXEC_TIMEOUT_SECONDS = 20L

    /** رمز خروج صناعيّ: انتهت المهلة ولم ينتهِ الأمر (ليس رمز نظام). */
    const val CODE_TIMEOUT = -2

    /** رمز خروج صناعيّ: تعذّر إطلاق الصدفة أصلًا. */
    const val CODE_LAUNCH_FAILED = -3

    /**
     * يرفض قبل السلك ما لا يجوز إرساله، أو يعود بـ`null` إذا كان الأمر صالحًا.
     * والمفاتيح ثابتة لأنها تُسجَّل وتُقارن، لا لأنها تُعرض كما هي.
     */
    fun rejectionFor(command: String): String? = when {
        command.isBlank() -> "empty-command"
        command.length > MAX_COMMAND_CHARS -> "command-too-long"
        else -> null
    }

    /**
     * يقتطع الخَرْج الزائد ويُلحق العلامة. والعملية **متكرّرة بأمان**: نصّ مُقتطع أصلًا لا يُقتطع
     * ثانيةً (وإلا لُحقت العلامة بنفسها وأوهمت بقراءة ناقصة مرّتين).
     */
    fun capOutput(raw: String): String = when {
        raw.length <= MAX_OUTPUT_CHARS -> raw
        raw.endsWith(TRUNCATION_MARK) -> raw
        else -> raw.take(MAX_OUTPUT_CHARS) + TRUNCATION_MARK
    }

    /**
     * يفكّ ردّ الخدمة `[رمز الخروج، الخَرْج]` إلى حكم معلن.
     * وردّ غير صالح = [ShizukuExec.Failed] — **لا نجاح مخمَّن**: غياب رمز الخروج ليس صفرًا.
     */
    fun parseReply(reply: Array<String>?): ShizukuExec {
        if (reply == null) return ShizukuExec.Failed("no-reply")
        val code = reply.getOrNull(0)?.trim()?.toIntOrNull() ?: return ShizukuExec.Failed("bad-code")
        return ShizukuExec.Ran(code, reply.getOrNull(1).orEmpty())
    }
}
