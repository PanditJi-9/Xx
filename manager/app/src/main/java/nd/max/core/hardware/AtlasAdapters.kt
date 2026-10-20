/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import nd.max.core.atlas.AtlasControlIntent
import nd.max.core.atlas.AtlasControlTarget
import nd.max.core.atlas.AtlasControlTransport
import nd.max.core.atlas.AtlasCpuPolicyFact
import nd.max.core.atlas.AtlasSafetyPolicy
import nd.max.core.atlas.AtlasSafetyVerdict

/**
 * طبقة **المواءمة** (Adapt) في دورة أطلس — «كيف أجعل هذه الميزة تعمل على هذا الجهاز تحديدًا؟».
 *
 * لماذا وُجد هذا الملف
 * --------------------
 * حتى اليوم كان لكل مقبض كتابة **مُنتِج واحد مكتوب بيده** (`AtlasDiscoveredControl` لسقف cpufreq،
 * و`ThermalCeilingRoutes` للحرارة…)، فدعم جهاز أو كيرنل جديد = تعديل القلب نفسه. وهذا بالضبط ما
 * يمنعه طلب المالك: «اجعل التصميم قائمًا على Capability Discovery + Adapters + Device Profiles +
 * Runtime Verification بحيث يمكن إضافة دعم لجهاز أو Kernel جديد دون إعادة كتابة قلب Max Atlas».
 *
 * فالعقد هنا ثلاثة أطراف:
 *
 * - **[AtlasControlAdapter]**: *طريقة* واحدة لتفعيل هدف تحكم على جهاز — يقيس ملاءمته
 *   ([assess]) ثم يبني مسارات المعاملة ([plan]) أو يبني مسارًا تمثيليًّا للخريطة ([probe]،
 *   يُبنى ولا يُنفَّذ أبدًا). مولّد جهاز/كيرنل جديد = **مُلاءِم جديد يُسجَّل هنا**، والقلب
 *   (`MaxAtlas` · المخطِّط · المُحكِّم) لا يتغيّر.
 * - **[AtlasAdapterRegistry]**: الاختيار الحتمي بين الملاءِمين — أول مُنطبق يفوز بترتيب النقل
 *   ثم المعرّف، وغياب الجميع **فجوة مُعلنة** (`Gap`) لا صمت.
 * - **[AtlasControlRequest]**: نيّة المستدعي بالمعنى الذي يقرره سياسِيّه (قيمة السقف مثلًا)،
 *   **لا مسارًا ولا أمر shell** — فصل «ماذا تريد» (MAX AI) عن «كيف على هذا الجهاز» (أطلس).
 *
 * والقواعد المُلزمة
 * ----------------
 * 1. **المُلاءِم لا يكتب.** `plan` يبني معاملات (`HardwareRepairRequest`) بعقود إغلاق تُنفَّذ
 *    لاحقًا وحدها — الكتابة تمرّ من `AtlasRepairPort` ← `HardwareRepairExecutor` ← المُحكِّم،
 *    نفس الحدّ المُتحقَّق الوحيد في المشروع. فلا كاتب ثانٍ ينجرف عن الأول.
 * 2. **قائمة عدم اللمس فوق الجميع.** كل مسار يُبنى يمرّ على [AtlasSafetyPolicy] (بالمسار هنا
 *    وبالمفتاح في `MaxAtlas`)، والممنوع يُسقَط برمز سبب ثابت `never-touch:<rule>` — لا يُجرَّب
 *    ثم يُرحَّع.
 * 3. **غياب الملاءِم فجوة لا نجاح ولا فشل.** الجهاز الذي يقرأ ولا يملك مسارًا مُثبَتًا يُعلن
 *    «يحتاج مُلاءِمًا أو طريقة بديلة» — لا يُختلق مسار ولا يُصمت عنه.
 * 4. **ما لم يُقاس لا يُدَّعى.** `assess` يجيب من قياسات مرّرة في [AtlasAdapterContext] (سياسات
 *    مكتشفة، وصول مُقاس) لا من جدول أسماء؛ والجهاز المجهول = غير مُنطبق لا «مدعوم».
 */
data class AtlasAdapterContext(
    /** هل المعاملة المُتحقَّقة قابلة للمحاولة الآن؟ (إشارة تُقاس، لا تُفترض) */
    val privileged: Boolean,
    /** سياسات cpufreq كما اكتشفتها `AtlasBackendProvider` — فارغة = لم تُكتشف، لا «غير موجودة». */
    val cpuFacts: List<AtlasCpuPolicyFact> = emptyList(),
    /** نفس السطح كما يقرأه `CpuHardwareBackend` مباشرة (التشخيص)؛ أيّهما وُجد كافٍ للملاءمة. */
    val cpuPolicies: List<CpuHardwareBackend.Policy> = emptyList(),
    /** ثقب المعاملة لكتّاب السقوف. `null` = لا كاتب في هذا السياق، فلا يُبنى مسار كتابة. */
    val ceilingAccess: AtlasCeilingAccess? = null,
    /**
     * جهاز GPU كما **قيس** الآن — وأهمّ ما فيه قابلية الكتابة المقيسة (`devfreqCeilingWritable`
     * ≠ مسار تثبيت OPP)، وهي ما يختار به الملاءِم الطريقة. ولا يُشتقّ من `GpuFact` عن قصد:
     * ذلك وحيٌ قراءةً لا يُعبّر عن الكتابة أصلًا (اختيارًا موثّقًا في `GpuHardwareBackend`).
     */
    val gpuDevice: GpuHardwareBackend.Device? = null,
    /** ثقب معاملة GPU ([AtlasGpuCeilingAccess])؛ `null` = لا كاتب ⇒ لا مسار كتابة GPU. */
    val gpuAccess: AtlasGpuCeilingAccess? = null,
    /**
     * هل أجابَت قراءة الحرارة (مناطق/إشارة منصّة) على هذا الجهاز؟ — **إشارة تُقاس ينقلها المستدعي**،
     * لا افتراض «كل جهاز له thermal zones». والقراءة وحدها لا تدّعي كتابة: الخريطة تقول حينها
     * «للقراءة فقط» لا «يحتاج مُلاءِمًا».
     */
    val thermalStatusReadable: Boolean = false,
    /**
     * أجهزة تردّد **الذاكرة** كما اكتُشفت (dvfsrc · cpubw · memlat · mif …).
     *
     * وقائمةٌ لا جهاز واحد: على نواة واحدة قد تُصدَّر ثلاثة أجهزة (واحد لكل عنقود)، واختيار
     * «أفضلها» باسم الشركة المصنّعة كان يُسقط عقدتين قابلتين للكتابة — وشرط المالك أن تعمل
     * الميزة على أكثر الأجهزة، وهو يشترط هنا **ألّا يُسقَط ما قيس**. كل جهاز يأخذ مفتاحه
     * القياسي ([HardwareControlKey.devfreqFrequency]) ومعاملته المستقلّة.
     */
    val memoryDevices: List<MemoryUfsHardwareBackend.Device> = emptyList(),
    /** أجهزة تردّد **التخزين** كما اكتُشفت (ufshci · nvme · mmc0 …). */
    val storageDevices: List<MemoryUfsHardwareBackend.Device> = emptyList(),
    /**
     * ثقب معاملة تردّد `devfreq`. `null` = لا كاتب في هذا السياق، فلا يُبنى مسار كتابة — ونفس
     * عقد `ceilingAccess`/`gpuAccess`: السياقُ يقول **ما قيس**، والملاءِم يقول كيف. وقرينُه
     * المُتخيَّل (كاتبٌ فوقه قيم لا يقيسها) هو ما يمنع الانزلاق إلى «كُتب شيء وقيس غيره».
     */
    val devfreqAccess: AtlasDevfreqCeilingAccess? = null,
    /**
     * عُقَد `devfreq` **قُرئت ولم يُعرف صنفها** ([MemoryUfsHardwareBackend.Unclassified]).
     *
     * ولماذا تُنقل إلى هنا: الخريطة تقول اليوم عن هدف `MEMORY` «لم يُرصَد» — وهذا **غير صحيح**
     * حين تكون العقدة موجودة وتُجيب بسلّم، وإنما الذي نقص هو **اسم صنفها**. فنقلُ القائمة يجعل
     * رمز الفجوة يقول ما ينقص بالضبط (`unclassified-devfreq-node-seen`) ويشير إلى الفعل الذي
     * يجبرها: تعليم الاسم من شاشة الذاكرة وUFS — لا انتظار تحديث يحمل اسم جهاز جديد.
     */
    val unclassifiedDevfreq: List<MemoryUfsHardwareBackend.Unclassified> = emptyList(),
    /**
     * عُقَد `devfreq` **رُصدت في التعديد ولم تُقرأ** ([MemoryUfsHardwareBackend.Discovery.unreadable]).
     *
     * ولماذا تُنقل منفصلةً عن [unclassifiedDevfreq]: تلك أجابت بسلّم ونقصها اسم صنفها (فعلُها
     * نقرة تعليم)، وهذه لم تُجب بشيء (فعلُها صلاحيةُ عبور أو مسارٌ مصحَّح). ورمزُ فجوةٍ واحد
     * للاثنين يطلب الفعل الخطأ — وهو ما يُصلحه [MemoryUfsHardwareBackend.UNREADABLE_GAP_CODE].
     */
    val unreadableDevfreq: List<String> = emptyList(),
    /**
     * عُقَد اللمس كما قُرئت (نمط لعب/تعزيز إدخال/نقر مزدوج) ووصولها المُقاس.
     *
     * تُمرَّر كقائمة لا كعقدة واحدة لأن الجهاز قد يعرض أكثر من مقبض، والملاءِم يختار المقروء
     * القابل للكتابة بقيمتَي `0/1` المثبَتتين. و`touchAccess = null` تعني «لم يُقس وصول» —
     * فلا يُبنى مسار كتابة، ولا يُدّعى أن اللمس غير مدعوم.
     */
    val touchNodes: List<TouchControlBackend.Node> = emptyList(),
    val touchAccess: TouchControlBackend.Access? = null,
)

/** حكم الملاءمة على هذا الجهاز الآن — ثلاث حالات لا تُختزل. */
sealed interface AtlasAdapterAssessment {

    data class Applicable(val adapterId: String, val evidence: String) : AtlasAdapterAssessment

    /** غير مُنطبق هنا. السبب رمز ثابت لا جملة. */
    data class NotApplicable(val reason: String) : AtlasAdapterAssessment

    /** قاعدة سلامة منعت هذا السطح أصلًا. */
    data class Denied(val ruleId: String) : AtlasAdapterAssessment
}

/**
 * نيّة تحكم مُصنَّفة. كل صيغة تحمل **القرار** (القيم المطلوبة كما احتسبها سياسِيّ المستدعي)،
 * ومسار التنفيذ يختاره أطلس.
 */
sealed interface AtlasControlRequest {
    val intent: AtlasControlIntent

    /** هل يجوز مساءلة الهدف المقاس؟ (منعصر المخطِّط، لا كتابة) */
    val measuredGoalAvailable: Boolean get() = true

    /**
     * سقف تردد لكل سياسة cpufreq بوحدة kHz.
     *
     * @param ceilingKHzByKnob اسم السياسة ← السقف المطلوب؛ `null` أو مفتاح غائب = لا نيّة ⇒ لا مسار.
     * @param reviewed هل راجع أحد شكل مسار هذا المقبض؟ (حارس الفشل المغلق: الافتراضي `false`).
     */
    data class CpuCeilings(
        override val intent: AtlasControlIntent,
        val owner: ControlOwnership.Owner,
        val token: String,
        val ceilingKHzByKnob: Map<String, Long?>,
        val reviewed: (String) -> Boolean = { false },
        override val measuredGoalAvailable: Boolean = true,
    ) : AtlasControlRequest

    /**
     * سقف GPU لكل جهاز رسوميّ — **بوحدة العقدة نفسها** كما تُقرأ من سلّم المقبض
     * ([GpuHardwareBackend.Device.frequencyUnit])، فلا تحويل عند المستدعي ولا خلط وحدتين.
     *
     * @param ceilingByKnob اسم الجهاز (`GpuHardwareBackend.Device.name`) ← السقف المطلوب؛
     *   `null` أو مفتاح غائب = لا نيّة ⇒ لا مسار (فلا كتابة بلا نيّة).
     * @param reviewed هل راجع أحد عقدة هذا الجهاز؟ الافتراضي `false` — الفشل مغلق كما في كل مقبض.
     */
    data class GpuCeilings(
        override val intent: AtlasControlIntent,
        val owner: ControlOwnership.Owner,
        val token: String,
        val ceilingByKnob: Map<String, Long?>,
        val reviewed: (String) -> Boolean = { false },
        override val measuredGoalAvailable: Boolean = true,
    ) : AtlasControlRequest

    /**
     * سقف تردّد الذاكرة لكل جهاز `devfreq` — **بوحدة العقدة نفسها** كما نشرها سلّم الجهاز
     * ([MemoryUfsHardwareBackend.Device.ladder])، فلا تحويل عند المستدعي ولا خلط وحدتين.
     *
     * @param ceilingByKnob اسم الجهاز (`Device.name`) ← السقف المطلوب؛ `null` أو مفتاح غائب =
     *   لا نيّة ⇒ لا مسار (فلا كتابة بلا نيّة).
     * @param floorByKnob أرضية اختيارية لكل جهاز. وهي **ليست زينةً**: أرضيةُ تردّد الذاكرة هي
     *   ما يقصر زمن التوقّف عند نقص عرض النطاق (memory stall) — وهي الحالة التي لا يعالجها سقفٌ
     *   صار أعلى، فيعطيها هذا العقدُ نيّةً مُصنَّفة لا قاعدةً مكتوبة بيدٍ في واجهة.
     * @param reviewed هل راجع أحد شكل مسار هذا المقبض؟ (حارس الفشل المغلق: الافتراضي `false`).
     */
    data class MemoryCeilings(
        override val intent: AtlasControlIntent,
        val owner: ControlOwnership.Owner,
        val token: String,
        val ceilingByKnob: Map<String, Long?>,
        val floorByKnob: Map<String, Long?> = emptyMap(),
        val reviewed: (String) -> Boolean = { false },
        override val measuredGoalAvailable: Boolean = true,
    ) : AtlasControlRequest

    /** سقف/أرضية تردّد التخزين (UFS · eMMC · NVMe) — نفس العقد لهدف `STORAGE`. */
    data class StorageCeilings(
        override val intent: AtlasControlIntent,
        val owner: ControlOwnership.Owner,
        val token: String,
        val ceilingByKnob: Map<String, Long?>,
        val floorByKnob: Map<String, Long?> = emptyMap(),
        val reviewed: (String) -> Boolean = { false },
        override val measuredGoalAvailable: Boolean = true,
    ) : AtlasControlRequest

    /**
     * تعزيز اللمس: قيمة `on/off` لعقدة **مثبَتة القيمتين** (0/1). والعقدة والوصول يُقرآن من
     * السياق، والطلب يحمل القرار فقط — فلا يمرّر المستدعي مسارًا ولا قيمة سائق مخمَّنة.
     */
    data class TouchBoost(
        override val intent: AtlasControlIntent,
        val owner: ControlOwnership.Owner,
        val token: String,
        val enabled: Boolean,
        override val measuredGoalAvailable: Boolean = true,
    ) : AtlasControlRequest
}

/** خطة مُلاءِم واحدة: مسارات بُنيت، ومفاتيح لم تُبنَ مع رمز سبب ثابت (لا جملة). */
data class AtlasAdapterPlan(
    val adapterId: String,
    val bindings: List<AtlasRouteBinding>,
    val skipped: List<Pair<String, String>>,
)

/** طريقة واحدة لتفعيل هدف تحكم على هذا الجهاز. */
interface AtlasControlAdapter {
    val id: String
    val target: AtlasControlTarget
    val transport: AtlasControlTransport

    /** هل تصلح هذه الطريقة لهذا الجهاز **الآن**؟ فشل مغلق: المجهول غير مُنطبق. */
    fun assess(context: AtlasAdapterContext, request: AtlasControlRequest? = null): AtlasAdapterAssessment

    /** مسارات معاملة لطلب واحد. لا يبني أبدًا مسارًا تمنعه قواعد عدم اللمس. */
    fun plan(context: AtlasAdapterContext, request: AtlasControlRequest): AtlasAdapterPlan

    /**
     * مسار تمثيلي لخريطة القدرة: يُبنى (تُقرأ خطوط الأساس قراءةً فقط) ويُحكم عليه بالمخطِّط،
     * **ولا يُنفَّذ أبدًا** — لا كتابة تخرج من هذه الدالة ولا من نتائجها.
     */
    fun probe(context: AtlasAdapterContext): AtlasAdapterPlan
}

/** نتيجة اختيار الملاءِم: مُنطبق، أو فجوة مُعلنة، أو رفض سلامة — لا حالة رابعة. */
sealed interface AtlasAdapterChoice {
    data class Chosen(val adapter: AtlasControlAdapter, val assessment: AtlasAdapterAssessment.Applicable) : AtlasAdapterChoice

    /** لا ملاءِم هنا: جهاز آخر أو بناء آخر يحتاج مُلاءِمًا أو طريقة بديلة. */
    data class Gap(val rejections: List<Pair<String, String>>) : AtlasAdapterChoice

    data class Denied(val ruleId: String) : AtlasAdapterChoice
}

/**
 * سجلّ الملاءِمين — نقطة التوسعة الوحيدة لدعم جهاز/كيرنل جديد.
 *
 * الاختيار حتميّ بالكامل: ترتيب ثابت (مرتبة النقل ثم المعرّف)، وبدونها لا يختلف اختياران على
 * جهاز واحد. وكل مُرشَّح مرفوض يخرج **بسببه** في `Gap.rejections`، فلا يُصمت عن «لماذا لا يعمل».
 */
class AtlasAdapterRegistry(private val adapters: List<AtlasControlAdapter>) {

    init {
        require(adapters.map { it.id }.distinct().size == adapters.size) {
            "adapter ids must be unique within one registry"
        }
    }

    fun adaptersFor(target: AtlasControlTarget): List<AtlasControlAdapter> = adapters
        .filter { it.target == target }
        .sortedWith(compareBy({ transportRank(it.transport) }, { it.id }))

    fun choose(
        target: AtlasControlTarget,
        context: AtlasAdapterContext,
        request: AtlasControlRequest? = null,
    ): AtlasAdapterChoice {
        val rejections = mutableListOf<Pair<String, String>>()
        adaptersFor(target).forEach { adapter ->
            when (val assessment = adapter.assess(context, request)) {
                is AtlasAdapterAssessment.Applicable -> return AtlasAdapterChoice.Chosen(adapter, assessment)
                is AtlasAdapterAssessment.Denied -> return AtlasAdapterChoice.Denied(assessment.ruleId)
                is AtlasAdapterAssessment.NotApplicable -> rejections += adapter.id to assessment.reason
            }
        }
        return AtlasAdapterChoice.Gap(rejections)
    }

    private fun transportRank(transport: AtlasControlTransport): Int = when (transport) {
        AtlasControlTransport.PLATFORM_HINT -> 0
        AtlasControlTransport.VENDOR_BRIDGE -> 1
        AtlasControlTransport.ROOT_DAEMON -> 2
        AtlasControlTransport.ARBITER_SYSFS -> 3
        AtlasControlTransport.READ_ONLY -> 4
    }

    companion object {
        /**
         * السجل الافتراضي لهذا البناء. مُلاءِموه بلا حالة: الأثراح تمرّ في السياق والطلب.
         *
         * **والسجل هو نقطة التوسعة الوحيدة:** إضافة طريقة الذاكرة وUFS هنا هي كل ما لزم لتدخل
         * قدرتان جديدتان (هدف `MEMORY` وهدف `STORAGE`) إلى خريطة القدرة والتنفيذ معًا — ولم
         * يتغيّر سطرٌ في `MaxAtlas` ولا في المخطِّط ولا في المُحكِّم، وهو شرط المالك الحرفيّ.
         */
        fun defaults(): AtlasAdapterRegistry = AtlasAdapterRegistry(
            listOf(
                CpuCeilingAdapter(),
                GpuDevfreqCeilingAdapter(),
                GpuOppPinCeilingAdapter(),
                MemoryDevfreqCeilingAdapter(),
                UfsDevfreqCeilingAdapter(),
                TouchSysfsAdapter(),
            ),
        )
    }
}

/**
 * مُلاءِم سقف cpufreq: يلفّ الجسر المُتحقَّق [AtlasDiscoveredControl] ويضعه تحت عقد السجل.
 *
 * هو نفسه المسار الإنتاجي المعروف (مفتاح `cpu_limits:<policy>`، معاملة المُحكِّم الكاملة)؛ الجديد
 * أن اختياره **طريقةً** صار عبر السجل، وأن العقدة الممنوعة تُسقَط قبل البناء لا بعده.
 */
class CpuCeilingAdapter : AtlasControlAdapter {
    override val id: String = "cpufreq-policy-ceiling"
    override val target: AtlasControlTarget = AtlasControlTarget.CPU_FREQUENCY
    override val transport: AtlasControlTransport = AtlasControlTransport.ARBITER_SYSFS

    override fun assess(context: AtlasAdapterContext, request: AtlasControlRequest?): AtlasAdapterAssessment {
        val visible = context.cpuFacts.isNotEmpty() || context.cpuPolicies.isNotEmpty()
        if (!visible) return AtlasAdapterAssessment.NotApplicable("no-cpufreq-policy-seen")
        return AtlasAdapterAssessment.Applicable(id, "cpufreq-policies=${context.cpuFacts.size + context.cpuPolicies.size}")
    }

    override fun plan(context: AtlasAdapterContext, request: AtlasControlRequest): AtlasAdapterPlan {
        val ceilings = request as? AtlasControlRequest.CpuCeilings
            ?: return AtlasAdapterPlan(id, emptyList(), listOf(KEY_ANY to SKIP_WRONG_REQUEST))
        val access = context.ceilingAccess
            ?: return AtlasAdapterPlan(id, emptyList(), listOf(KEY_ANY to SKIP_NO_SEAM))
        return planFor(context, ceilings, access) { fact -> ceilings.ceilingKHzByKnob[fact.name] }
    }

    override fun probe(context: AtlasAdapterContext): AtlasAdapterPlan {
        val access = context.ceilingAccess
            ?: return AtlasAdapterPlan(id, emptyList(), listOf(KEY_ANY to SKIP_NO_SEAM))
        // المسار التمثيلي: السقف المطلوب = أقصى قيمة أثبتتها النواة — يُبنى ويُحكم عليه ولا يُكتب.
        return planFor(context, null, access) { fact -> fact.provenMaxKHz ?: fact.maxKHz }
    }

    private fun planFor(
        context: AtlasAdapterContext,
        ceilings: AtlasControlRequest.CpuCeilings?,
        access: AtlasCeilingAccess,
        ceilingKHz: (AtlasCpuPolicyFact) -> Long?,
    ): AtlasAdapterPlan {
        // قواعد عدم اللمس أولًا: سياسة على عقدة ممنوعة تُسقَط برمز سبب، ولا تصل إلى المُخطِّط.
        val (safe, denied) = context.cpuFacts.partition {
            AtlasSafetyPolicy.verdictFor(name = it.name, path = it.path) == AtlasSafetyVerdict.Allowed
        }
        val deniedSkips = denied.map { fact ->
            val rule = AtlasSafetyPolicy.verdictFor(name = fact.name, path = fact.path) as? AtlasSafetyVerdict.Denied
            HardwareControlKey.cpuLimits(fact.name) to "never-touch:${rule?.ruleId ?: "unknown"}"
        }
        val control = AtlasDiscoveredControl(
            access = access,
            token = ceilings?.token ?: PROBE_TOKEN,
            owner = ceilings?.owner ?: ControlOwnership.Owner.MAX_AI,
        )
        val plan = control.cpuCeilingPlan(
            facts = safe,
            ceilingKHz = ceilingKHz,
            reviewed = { fact ->
                ceilings?.reviewed?.invoke(fact.name)
                    ?: AtlasDiscoveredControl.isReviewedControlRoute(HardwareControlKey.cpuLimits(fact.name))
            },
        )
        return AtlasAdapterPlan(id, plan.bindings, plan.skipped + deniedSkips)
    }

    private companion object {
        /** رمز يُستعمل حين لا مفتاح بعينه في السؤال (طلب بصيغة خاطئة، أو بلا ثقب معاملة). */
        const val KEY_ANY: String = "cpu-ceiling"
        const val SKIP_WRONG_REQUEST: String = "wrong-request-shape"
        const val SKIP_NO_SEAM: String = "no-write-seam-available"

        /** معرّف التمثيل — لا يحمل ملكية ولا يُنفَّذ، فيكفيه معرّف ثابت غير قابل للالتباس بمعاملة. */
        const val PROBE_TOKEN: String = "atlas-map-probe"
    }
}
