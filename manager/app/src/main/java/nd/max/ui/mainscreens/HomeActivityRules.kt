/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.mainscreens

import nd.max.R
import nd.max.core.privilege.PrivilegeLevel
import nd.max.ui.viewmodel.DaemonHealth

/**
 * متى تكون شارة الرئيسية **مفعّلة** — قرار واحد يقرأه الشاشة والاختبار معًا.
 *
 * ## الأمر الذي بُنيت عليه (المالك، ٢٠٢٦-١١-١٦)
 *
 * «اعرض active إذا اختار الشخص بدون روت و Shizuku … وليس active إذا اختار روت ولا يوجد، أو module
 * غير مثبّت — يجب الشرطان لظهور active». أي أنّ «مفعّل» ليست زينةً بصرية، بل **إثبات أنّ المسار
 * الذي اختاره المستخدم يعمل الآن**:
 *
 * * **[PrivilegeLevel.ROOT]** ⇒ الجذر ممنوح **والوحدة مثبّتة** معًا. الجذر وحده يكفي لبعض الأوامر،
 *   لكن نصف المنظومة (السكربتات التي تعمل قبل الإقلاع) يأتي من الوحدة؛ فإن غابت فالتطبيق يعمل
 *   بنصف قدرته، وعرض «مفعّل» هنا كذبٌ صريح.
 * * **[PrivilegeLevel.SHIZUKU]** ⇒ `PrivilegeLevel.SHIZUKU` لا يُنتَج أصلًا إلا حين يكون Shizuku
 *   جاهزًا **وإذنه ممنوحًا** (`ExecutionModeRules.level` + `ShizukuGateway`)، فالقيمة نفسها هي
 *   الإثبات ولا يُشترط معها جذر.
 * * **[PrivilegeLevel.NONE]** ⇒ لا مسار يعمل، فهي «غير مفعّل» بحكم التعريف.
 *
 * ## ولماذا دالّة نقيّة هنا لا داخل `HomeViewmodel`
 *
 * لأن السؤال سياسةٌ لا حالة: يُقاس بمدخلين (المستوى، وحال الوحدة) ويُختبر بلا جهاز ولا Compose.
 * ووضعُه في الـViewModel كان سيخلط قراءة الحالة بقراري العرض.
 */
internal object HomeActivityRules {

    /**
     * هل يعمل مسار الامتياز المختار الآن بشرطيه؟
     *
     * **والوحدة المكتوبة بلا إقلاع ليست عاملة بعد:** التفعيل من داخل التطبيق يكتب ملفّات
     * الوحدة مباشرةً، فيراها مدير الجذر فورًا، لكن سكربتاتها لا تعمل إلا في الإقلاع التالي.
     * فاعتبارها «مفعّلة» قبل الإقلاع ادّعاءٌ كاذب — ولهذا يشترط ألّا يكون هناك تحديث معلّق.
     */
    fun active(
        level: PrivilegeLevel,
        moduleInstalled: Boolean,
        rebootPending: Boolean = false,
    ): Boolean = when (level) {
        PrivilegeLevel.ROOT -> moduleInstalled && !rebootPending
        PrivilegeLevel.SHIZUKU -> true
        PrivilegeLevel.NONE -> false
    }
}

/**
 * نصّ نافذة الحالة — **الحالة تحدّد العنوان والجسم**، لا اللون وحده.
 *
 * ## العطب الذي تمنعه هذه القاعدة (أمر المالك ٢٠٢٦-١١-٢٠)
 *
 * «الناس يفهمون هذه الشاشة بشكل خاطئ … يجب أن تظهر Why not active? عندما يكون مكتوبًا غير مفعّل،
 * ولكن عندما يكون مكتوبًا مفعّل تختلف الرسالة». وكانت النافذة **واحدة** تُفتح من الشارة في الحالتين،
 * فمن تطبيقه يعمل تمامًا قرأ «لماذا غير مفعّل؟» و«مسار واحد يعمل يكفي…» — أي أنّ الشاشة كانت
 * تنقض شارتها. فالشارة `ACTIVE` تفتح نافذة تقول «كل شيء يعمل»، و`غير مفعّل` تفتح نافذة السبب.
 *
 * ## ولماذا في دالّة نقيّة هنا
 *
 * لأن المتوقَّع من الاختبار أن يثبت **اختلاف المعرّفين** لا شكل النافذة؛ فيُقاس بلا Compose وبلا جهاز.
 * ولو غُيّر أحد المعرّفين غدًا إلى نصّ «لماذا غير مفعّل» سقط الاختبار بالاسم.
 */
internal object HomeStatusCopy {

    fun titleRes(active: Boolean): Int =
        if (active) R.string.home_status_active_title else R.string.home_status_reason_title

    fun bodyRes(active: Boolean): Int =
        if (active) R.string.home_status_active_body else R.string.home_status_reason_body

    /**
     * سطر الخادم في النافذة — أو `null` حين لا يُعرض في هذا الوضع.
     *
     * **ولا يُعرض الخادم إلا في وضع الروت** (أمر المالك ٢٠٢٦-١١-٢٢): في Shizuku/بلا مسار
     * يكون التطبيق عاملًا بمساره المباشر، وسطر «الخادم لا يجيب» يُوهم بعطبٍ ليس عطبًا.
     * وفي وضع الروت يصير سؤال «هل يعمل دون مشاكل؟» سؤالًا حقيقيًّا، فتُعرض كل حالاته:
     * يعمل، أو يعمل ويُدار بالجذر، أو يرفض هويّة التطبيق، أو يعمل ولا يصل إليه التطبيق،
     * أو لا يجيب، أو لم تُقس حالته بعد.
     *
     * و[UNKNOWN] تبقى `null` خارج وضع الروت ولا تُكتب «لا يجيب» قبل السؤال (ADR-07).
     * وفي وضع الروت تُكتب صراحةً «لم تُقس بعد» — لا نجاحًا ولا فشلًا.
     */
    fun daemonRowRes(health: DaemonHealth, rootMode: Boolean = false): Int? = when (health) {
        DaemonHealth.UNKNOWN ->
            if (rootMode) R.string.home_status_row_daemon_unknown else null
        DaemonHealth.RUNNING -> R.string.home_status_row_daemon_running
        // الجذر أثبت أن الخادم يعمل ويعلن نفسه running — سطر نجاح لا تحذير قناة.
        DaemonHealth.RUNNING_ROOT -> R.string.home_status_row_daemon_running_root
        DaemonHealth.DENIED -> R.string.home_status_row_daemon_denied
        // في غير وضع الروت لا يُعرض سطر «لا يجيب»: التطبيق يعمل بمساره المباشر، والسطر يُوهم
        // بعطلٍ غير موجود. وفي وضع الروت يُعرض لأنه عطب حقيقي (أمر المالك ٢٠٢٦-١١-٢٢).
        DaemonHealth.UNREACHABLE ->
            if (rootMode) R.string.home_status_row_daemon_unreachable else null
        // «حيّ لكن التطبيق لا يصل إليه» سطرٌ رابع لا يُطوى في «لا يجيب»: القناة لم تُسأل أصلًا
        // (SELinux في تثبيت الوحدة)، والجذر أثبت أن الخادم يعمل. ولا يُعرض خارج وضع الروت،
        // فالحالة نفسها لا تُنتَج إلا بتأكيد الجذر.
        DaemonHealth.CHANNEL_BLOCKED ->
            if (rootMode) R.string.home_status_row_daemon_channel_blocked else null
        DaemonHealth.STOPPED -> R.string.home_status_row_daemon_stopped
    }

    /**
     * علامة الصحّ للخادم شرطٌ واحد: **يردّ ويعمل**.
     * و«يردّ رافضًا هويّة التطبيق» عطبُ هويّة لا فشلُ خادم، ولا «لا يجيب» — ولكلٍّ سطره.
     */
    fun daemonVerdict(health: DaemonHealth): Boolean? = when (health) {
        DaemonHealth.RUNNING, DaemonHealth.RUNNING_ROOT -> true
        DaemonHealth.UNKNOWN -> null
        // حيّ ومقيس، لكن التطبيق لا يستطيع التحكّم فيه عبر القناة ⇒ ليس «دون مشاكل».
        DaemonHealth.CHANNEL_BLOCKED -> false
        else -> false
    }

    /** هل الخادم يعمل دون مشاكل؟ — `true` وحدها تعني نعم. */
    fun daemonSatisfied(health: DaemonHealth): Boolean = daemonVerdict(health) == true

    /**
     * سطر الخادم كما تعرضه النافذة: **وضع الروت وحده** يسأل عن الخادم.
     *
     * دالّة نقيّة لأنها سياسة عرض، لا شكلًا — فتُقاس على JVM بلا Compose ولا جهاز.
     */
    fun daemonRowFor(level: PrivilegeLevel, health: DaemonHealth): Int? =
        if (level == PrivilegeLevel.ROOT) daemonRowRes(health, rootMode = true) else null
}
