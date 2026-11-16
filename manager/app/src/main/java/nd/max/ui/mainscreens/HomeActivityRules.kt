/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.mainscreens

import nd.max.core.privilege.PrivilegeLevel

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
