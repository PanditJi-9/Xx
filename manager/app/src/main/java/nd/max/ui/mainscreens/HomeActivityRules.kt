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

    /** هل يعمل مسار الامتياز المختار الآن بشرطيه؟ */
    fun active(level: PrivilegeLevel, moduleInstalled: Boolean): Boolean = when (level) {
        PrivilegeLevel.ROOT -> moduleInstalled
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
     * سطر الخادم في النافذة — أو `null` حين لم تُقَس حالته بعد.
     *
     * ولِمَ `null` لا «لا يجيب»؟ لأن `UNKNOWN` تعني **لم تُسأل بعد**؛ وكتابة «لا يجيب» قبل السؤال
     * هي نفس الكذب الذي جاء الأمر لإزالته، بمقياس أدقّ.
     */
    fun daemonRowRes(health: DaemonHealth): Int? = when (health) {
        DaemonHealth.UNKNOWN -> null
        DaemonHealth.RUNNING -> R.string.home_status_row_daemon_running
        DaemonHealth.DENIED -> R.string.home_status_row_daemon_denied
        DaemonHealth.UNREACHABLE -> R.string.home_status_row_daemon_unreachable
        DaemonHealth.STOPPED -> R.string.home_status_row_daemon_stopped
    }

    /**
     * علامة الصحّ للخادم شرطٌ واحد: **يردّ ويعمل**.
     * و«يردّ رافضًا هويّة التطبيق» عطبُ هويّة لا فشلُ خادم، ولا «لا يجيب» — ولكلٍّ سطره.
     */
    fun daemonSatisfied(health: DaemonHealth): Boolean = health == DaemonHealth.RUNNING

    /**
     * سببُ توقّف الخادم حين يكون التحديث المعلَّق هو السبب — أو `null` حين لا يُعرف سبب.
     *
     * الحارس (`DaemonSupervisor.blockReason`) يرفض تشغيل الخادم ما دامت علامة `update` موجودة،
     * ويُطبَّق التحديث بالإقلاع. فحينها فقط يُشرح للمستخدم: «متوقّف لأن تحديث الوحدة معلّق»، بدل
     * أن يقرأ «لا يجيب» بلا سبب. ولا تُضاف الجملة إن كان الخادم يعمل أصلًا (`RUNNING`)، لأنها
     * ستكون كذبًا جديدًا.
     */
    fun daemonUpdateNoticeRes(updatePending: Boolean, health: DaemonHealth): Int? =
        if (updatePending && health == DaemonHealth.UNREACHABLE) {
            R.string.home_status_row_daemon_update_pending
        } else {
            null
        }
}
