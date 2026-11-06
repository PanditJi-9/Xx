/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.policy

import androidx.annotation.StringRes
import nd.max.R

/**
 * سجلّ ما **لا** يقدّمه هذا التطبيق، وسببه — يُعرض في الشاشة ولا يُخفى.
 *
 * ### لماذا وُجد (أمر المالك · `IMPROVE-NOT-DUPLICATE-01`)
 *
 * «وقم باضفة الاشياء المستبعدة معا تحذير وتبيه لهم». والقاعدة التي بُني عليها
 * (`AGENTS.md` §0.5 · `ADR-91`): المستبعَد يُعلن بوسمه وسببه، ولا يُترك ليُظنّ أنه سهو أو
 * نقص، **ولا يُعرض مفتاحًا مطفأً** يوهم بأن إقرارًا أو جذرًا سيُشغّله (`ADR-07`).
 *
 * ### والوسم ثلاثة لا واحد، لأن الأسباب مختلفة والخلط يكذب
 *
 * - [Kind.REFUSED]: قرارٌ لنا — لا يُفتح بإقرار ولا بجذر.
 * - [Kind.UNSUPPORTED]: المنصّة/النواة لا تُظهر المسار، فلا شيء يُكتب أصلًا.
 * - [Kind.DANGEROUS]: **مشحون** فعلًا، لكن خلف إقرار خطر واحد يُسجَّل بزمنه (`ADR-90`).
 *   ووجوده هنا مقصود: من يقرأ السجلّ يجب أن يرى الخطير بوسمه لا أن يظنّ كل ما في القائمة ممنوعًا.
 *
 * ### وما لا يفعله هذا الملفّ
 *
 * لا يُنفّذ شيئًا ولا يقرأ جهازًا ولا يقرّر صلاحية. هو **نصّ سياسة مُهيكل** يُترجم إلى صفوف
 * في الواجهة، ولذلك يعيش في `core/policy` لا في `ui/`: الحكم لا يعتمد على وجود شاشة.
 */
enum class ExcludedCapability(
    /** معرّف ثابت لا يُترجم — يُستخدم في الاختبار ومنع التكرار. */
    val id: String,
    @StringRes val title: Int,
    @StringRes val reason: Int,
    val kind: Kind,
) {

    // ——— مرفوض بقرار ———
    VOLTAGE(
        id = "voltage",
        title = R.string.excluded_voltage_title,
        reason = R.string.excluded_voltage_reason,
        kind = Kind.REFUSED,
    ),
    SOC_TUNABLES(
        id = "soc_tunables",
        title = R.string.excluded_soc_tunables_title,
        reason = R.string.excluded_soc_tunables_reason,
        kind = Kind.REFUSED,
    ),
    SYS_CONFIG(
        id = "sys_config",
        title = R.string.excluded_sysconfig_title,
        reason = R.string.excluded_sysconfig_reason,
        kind = Kind.REFUSED,
    ),
    KERNEL_FLASH(
        id = "kernel_flash",
        title = R.string.excluded_kernel_flash_title,
        reason = R.string.excluded_kernel_flash_reason,
        kind = Kind.REFUSED,
    ),
    BENCHMARK_RANKING(
        id = "benchmark_ranking",
        title = R.string.excluded_benchmark_title,
        reason = R.string.excluded_benchmark_reason,
        kind = Kind.REFUSED,
    ),
    TELEMETRY_UNMEASURED(
        id = "telemetry_unmeasured",
        title = R.string.excluded_telemetry_title,
        reason = R.string.excluded_telemetry_reason,
        kind = Kind.REFUSED,
    ),

    // ——— المنصّة لا تقدّمه ———
    WAKELOCKS(
        id = "wakelocks",
        title = R.string.excluded_wakelocks_title,
        reason = R.string.excluded_wakelocks_reason,
        kind = Kind.UNSUPPORTED,
    ),

    // ——— مشحون، لكنه خطر ويحتاج إقرارًا ———
    CPU_LIMITS(
        id = "cpu_limits",
        title = R.string.excluded_cpu_limits_title,
        reason = R.string.excluded_reason_dangerous,
        kind = Kind.DANGEROUS,
    ),
    GPU_LIMITS(
        id = "gpu_limits",
        title = R.string.excluded_gpu_limits_title,
        reason = R.string.excluded_reason_dangerous,
        kind = Kind.DANGEROUS,
    ),
    PACKAGE_FREEZE(
        id = "package_freeze",
        title = R.string.excluded_freeze_title,
        reason = R.string.excluded_reason_dangerous,
        kind = Kind.DANGEROUS,
    ),
    BOOT_RESTORE(
        id = "boot_restore",
        title = R.string.excluded_restore_title,
        reason = R.string.excluded_reason_dangerous,
        kind = Kind.DANGEROUS,
    ),
    ;

    /**
     * الوسم. والترتيب هو ترتيب العرض: ما رُفض أولًا، ثم ما لا تدعمه المنصّة، ثم الخطير المشحون
     * آخرًا — فلا يُخلط «ممنوع» بـ«خطر لكنه متاح».
     */
    enum class Kind(@StringRes val label: Int) {
        REFUSED(R.string.excluded_kind_refused),
        UNSUPPORTED(R.string.excluded_kind_unsupported),
        DANGEROUS(R.string.excluded_kind_dangerous),
    }

    companion object {
        /** الصفوف مجمَّعة بوسمها بترتيب [Kind.entries] — تُستهلك في الواجهة كما هي. */
        fun grouped(): List<Pair<Kind, List<ExcludedCapability>>> =
            Kind.entries.map { kind -> kind to entries.filter { it.kind == kind } }
    }
}
