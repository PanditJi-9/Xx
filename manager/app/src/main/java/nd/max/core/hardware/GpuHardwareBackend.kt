/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/**
 * Canonical GPU owner. A provider is controllable only when its identity,
 * frequency unit, advertised values, writable controls and restoration baseline
 * can all be proved from the running driver.
 */
object GpuHardwareBackend {
    private const val ROOT = "/sys/class/devfreq"
    private const val THERMAL_ROOT = "/sys/class/thermal"

    /** تردد الGPU الجاري كما تبلّغ به عقدة GED — أداة قياس لا مرجع كتابة. */
    private const val GED_CURRENT_FREQUENCY = "/sys/kernel/ged/hal/current_freqency"

    // شكل صيغة قيمة مفتاح gpu_frequency — انظر explanation في encodeRequest.
    private const val SCHEMA_SEPARATOR = "|"
    private const val UNTOUCHED = "-"
    private const val UNREADABLE = "unreadable"
    private const val RELEASED = "released"
    private const val HELD = "held"

    enum class Family { QUALCOMM, MALI, UNKNOWN }
    enum class IntentMode { EFFICIENCY, ADAPTIVE, SUSTAINED }
    enum class SelectionState { READY, READ_ONLY, AMBIGUOUS, UNAVAILABLE }
    enum class FrequencyUnit(val hzMultiplier: Long) {
        HZ(1L), KHZ(1_000L), MHZ(1_000_000L), AMBIGUOUS(0L)
    }

    /**
     * دور العقدة كما يقوله **اسمها** — لا كما نظنّه.
     *
     * ولماذا لزم: `/sys/class/devfreq` يحمل على سنابدراجون أكثر من عقدة واحدة في عائلة الرسوميات،
     * وأشهرها زوجان: عقدة الساعة (`3d00000.qcom,kgsl-3d0`، وهي جهاز الـGPU نفسه في النواة) ومراقب
     * عرض النطاق (`kgsl-busmon`، يوافق على مستوى الذاكرة ولا يملك ساعة GPU). وكان الاثنان يحصلان على
     * **الدرجة نفسها** (كل اسم فيه `kgsl` كان ١٠٠)، فيُقرأ التعادل `multiple-equally-proven-gpu-providers`
     * ولا يُختار مزوّد — والقياس على جهاز حقيقي (POCO X6 Pro · SM8735 · 2026-10-15) يقول إن كل مقابض
     * الـGPU أُغلقت بهذا السبب وحده: `knob=gpu_profile outcome=unsupported
     * reason=no-gpu-provider:multiple-equally-proven-gpu-providers`.
     *
     * فالدور فصلٌ بين مالكَين مختلفَين لا درجةٌ في سلّم واحد: مراقب النطاق **لا يُرشَّح** ليكون مالك
     * ساعة GPU (فلا يُكتب فيه تردد GPU أبدًا)، ويبقى **مرئيًّا** في المرشّحات واللقطة والتشخيص.
     * و[GRAPHICS_UNKNOWN] لما هو رسوميّ بالعائلة بلا توقيع ساعة صريح — يُقاس ولا يُدّعى.
     */
    enum class NodeRole(val token: String) {
        GPU_CLOCK("gpu-clock"),
        BANDWIDTH_MONITOR("bandwidth-monitor"),
        GRAPHICS_UNKNOWN("graphics-unknown"),
    }

    /**
     * توقيعات **ساعة GPU** في اسم العقدة — اسم سائق الرسوميات كما تنشره النواة نفسها.
     *
     * `kgsl-3d` هو اسم جهاز Qualcomm (`3d00000.qcom,kgsl-3d0` مقيس في بصمة POCO X6 Pro)،
     * و`gpufreq`/`mali`/`adreno` أسماء سائقات معلومة. والفصل على `kgsl-3d` لا على `kgsl` وحده
     * مقصود: `kgsl-busmon` يحمل `kgsl` ولا يحمل `kgsl-3d`، وهو مراقب لا ساعة.
     */
    private val GPU_CLOCK_SIGNATURES = listOf("kgsl-3d", "gpufreq", "gpuclock", "mali", "adreno")

    /**
     * رموز **عرض النطاق/جودة الخدمة** — عُقَد توافق على مستوى الذاكرة ولا تُنتج ساعة GPU.
     *
     * والمفردات مرصودة من سائقات حقيقية: `busmon`/`bwmon`/`devbw`/`memlat` (مراقبات Qualcomm)،
     * و`gpubw`/`cpubw`/`llcc`/`l3c`/`llccbw` (عرض نطاق وحدة الرسوميات وآخر مستوى مخبأ)،
     * و`dvfsrc`/`emi`/`mif`/`dram`/`ddr`/`membw`/`dramc` (MediaTek وExynos وبوّابات الذاكرة)،
     * و`noc`/`vbif`/`icc`/`qos` (ناقل وربط بيني). والمطابقة **على حدود الرمز** لا في أي موضع —
     * كما في [MemoryUfsHardwareBackend] — فلا تُلتقط عقدة باسم عابر يحوي الرمز داخل كلمة أطول.
     */
    private val BANDWIDTH_ROLE_TOKENS = setOf(
        "busmon", "bwmon", "devbw", "memlat", "busbw",
        "gpubw", "cpubw", "llcc", "llccbw", "l3c",
        "dvfsrc", "emi", "mif", "dram", "dramc", "ddr", "membw",
        "noc", "vbif", "icc", "qos",
    )

    private val ROLE_SEPARATORS = charArrayOf('.', '_', '-', ':', '/', ' ', '@', ',')

    /**
     * سطح قراءة بديل لساعة GPU على سنابدراجون — حين تُرصد عقدة `devfreq` ولا تُقرأ أعمدتها.
     *
     * **العطب المقيس (حزمة ٢٠٢٦-١٠-٢١ · Xiaomi 17 Pro Max · SM8850 · Android 17):**
     * عدّ `/sys/class/devfreq` يرى `3d00000.qcom,kgsl-3d0`، لكن قراءة `device_name`
     * و`min_freq`/`max_freq`/`cur_freq`/`available_frequencies`/`governor` كلها تعود فارغة —
     * فتظهر شاشة GPU Studio «Aucune lecture» للتردد والمدى والحاكم، وتُصنَّف القدرة `NONE`
     * فتصير مقابض الرسوميات غير قابلة للتعديل. والدليل في اللقطة المنشورة:
     * `clocks-unreadable-at-discovery` مع `none` في الحقول الستّة كلها.
     *
     * ولا يعني ذلك أن العتاد لا يعلن ساعته: نواة كوالكوم تنشر العقدة نفسها مرّتين — كصنف
     * `devfreq`، وكعقدة `kgsl` تحت `/sys/class/kgsl/kgsl-3d0/devfreq` — وبعض الرومات تمنع
     * الأولى عن عملية الاكتشاف (سياق `untrusted_app` على `vendor_sysfs_devfreq`) وتترك
     * الثانية مقروءة (كما تُقرأ `gpu_busy_percentage` و`temp` منها في الشاشة نفسها).
     *
     * فالسطح البديل **قراءة لا تخمين**: نفس أسماء الأعمدة (`min_freq`/`max_freq`/…)،
     * ويُقبل فقط إذا أعطى سلّمًا أو قيمة مقروءة بوحدة مؤكَّدة. ولا يُضاف إلا حين لا يوجد
     * مزوّد ساعة مقروء أصلًا — فلا يصير للـGPU مالكَان (وهو ما يمنعه هذا المستودع).
     */
    private class ClockSurface(
        val base: String,
        val minKey: String,
        val maxKey: String,
        val currentKey: String,
        val ladderKey: String,
        val evidence: String,
    )

    private val QUALCOMM_CLOCK_SURFACES = listOf(
        ClockSurface(
            base = "/sys/class/kgsl/kgsl-3d0/devfreq",
            minKey = "min_freq",
            maxKey = "max_freq",
            currentKey = "cur_freq",
            ladderKey = "available_frequencies",
            evidence = "kgsl-devfreq-surface",
        ),
        // والسطح الثاني هو عقد `kgsl` نفسها بأسماء كوالكوم القديمة (`gpuclk`/`max_gpuclk`).
        // يُقبل لل**قراءة** فقط حين لا يوجد سطح `devfreq`: `min_freq`/`max_freq` غائبان عنه،
        // فالكتابة تبقى غير مثبتة (FailClosed) والتردد الحيّ يُقرأ من `gpuclk`.
        ClockSurface(
            base = "/sys/class/kgsl/kgsl-3d0",
            minKey = "min_gpuclk",
            maxKey = "max_gpuclk",
            currentKey = "gpuclk",
            ladderKey = "gpu_available_frequencies",
            evidence = "kgsl-class-surface",
        ),
    )

    /**
     * سبب الغياب حين **تُرصد** عُقَد رسومية ولا مزوّد بينها: كلها مراقبات عرض نطاق.
     *
     * ولماذا ثابت مُعلَن لا نصّ في موضع واحد: الرمز يُقرأ في ثلاثة أماكن (السجل، وشاشة GPU،
     * والقاموس)، ونصًّا منسوخًا في كل موضع كان سيُخالف أحدها عند أول تعديل — وهو ما يمنعه
     * المستودع في تعريف الرمز الواحد.
     */
    const val BANDWIDTH_MONITOR_REASON = "bandwidth-monitor-not-a-gpu-provider"

    /**
     * الجذر **موجود ولا يُعدَّد**: «لا اسم يُقرأ» ليست «لا مزوّد على الجهاز».
     *
     * وبرمز [MemoryUfsHardwareBackend.REASON_ROOT_UNREADABLE] نفسه عن قصد: نقص قياس واحد
     * (جذر `devfreq` محجوب عن كل قنوات القراءة) يُكتب بمعرّف واحد، وإلا تعلّمت الحزمة رمزين
     * لحالة واحدة وطلب القارئ علاجين لا علاجًا واحدًا.
     */
    const val ROOT_UNREADABLE_REASON = MemoryUfsHardwareBackend.REASON_ROOT_UNREADABLE

    /**
     * The read half of [Io] (plan `P3`/`T3.2`).
     *
     * A discovery caller — `Max Atlas` — gets this and nothing more. It has no `write` and no
     * `writable` member, so it cannot *ask* whether something could be written, which is stronger
     * than asking and being told `false`. The legacy [Io] extends it, so every existing implementation
     * and call site is unchanged.
     *
     * **Contract:** [read] returns the value as the interface reported it, `null` when it did not
     * answer. `SystemIo` is backed by [RootFileAccess], which trims; the scanner below does not rely on
     * that, because a sysfs value arrives with a trailing newline and a reader that handed back the
     * bytes unchanged used to make every frequency parse to `null` — a device that *has* a clock would
     * have been reported as having none. Trimming at the parse site is what makes both readers behave
     * the same.
     */
    interface ReadIo {
        fun exists(path: String): Boolean
        fun read(path: String): String?
        fun listDirectories(path: String): List<String>

        /**
         * قراءة عدة عقد في **نداء واحد** — بند §4: «اجمع البيانات عند الإمكان».
         *
         * الافتراضيّ يعود إلى [read] لكل عقدة، فيبقى كل منفّذ — والوهميّ في الاختبارات — على
         * سلوكه الحرفيّ بلا سطر واحد، و[SystemIo] وحده يمرّ بالدفعة الأصليّة في `RootFileAccess`
         * (قارئ داخل العملية أولًا، ثم IPC واحدة لما لم يُقرأ، ثم الفرديّ لما بقي).
         *
         * **ولماذا وُجدت:** مسار الاكتشاف يسأل عقدًا صغيرة كثيرة ومتقاربة — ستّ ساعات لكل
         * مرشّح بعد سؤال الهوية، وعقدتَي حِمل لكل مرشّح، وقراءة `type` لكل منطقة حرارية من
         * عشرات ثم درجة المطابقة، وثلاثة أسطح OPP — وهي بعينها الحالة التي صُمِّم لها القارئ
         * الدفعيّ. والدلالة لكل عقدة لا تتغيّر: المقصوص غير الفارغ أو `null`، والترتيب محفوظ
         * بالمحاذاة، وعقدة لم يُجَب عنها يكمّلها القارئ كما كان.
         */
        fun readMany(paths: List<String>): List<String?> = paths.map(::read)
    }

    interface Io : ReadIo {
        fun writable(path: String): Boolean
        fun write(path: String, value: String): Boolean

        /**
         * سلطة المنصّة: `release` = هل نرفع سقف المصنّع؟ و`lock` = هل نثبّت قيمة واحدة (فيُطفأ DVFS)؟
         *
         * ومعاملان لا معامل: [PlatformCeilingAuthority.permitGpu] يحمل السبب مفصّلًا. وخُلاصته أن
         * `releaseVendorCeiling` كان يُمرَّر كـ`lock`، فطلبُ سقفٍ عند قدرة الجهاز — وهو عكس التثبيت —
         * كان **يوقف DVFS** ويثبّت التردد الذي صادف وجوده. وهذا هو الفرق بين «كُتب السقف» و«تحرّك الجهاز».
         *
         * والمُهلة الافتراضية **بلا فعل** عن قصد: كاتب مُوجَّه باختبار لا يجب أن يلمس عقد سلطة حقيقية،
         * و[MockIo] وأخواته يُرثان هذه القيمة فلا يتغيّر سلوك أي اختبار قائم. أما [SystemIo] فينفّذها فعلًا.
         *
         * ولماذا هي في هذا الموصل لا نداء مباشر داخل [applyDevfreq]: لأن نقطة الكتابة نفسها
         * تُستبدل في الاختبارات، فالوصول إلى العتاد من داخل دالة تُختبر بمُوجّه وهمي يُبطل الوهم.
         */
        fun permitVendorCeiling(lock: Boolean, release: Boolean): Boolean = false
    }

    object SystemIo : Io {
        override fun exists(path: String) = RootFileAccess.exists(path)
        override fun writable(path: String) = RootFileAccess.writable(path)
        override fun read(path: String) = RootFileAccess.read(path)
        override fun readMany(paths: List<String>) = RootFileAccess.readMany(paths)
        override fun write(path: String, value: String) = RootFileAccess.write(path, value)
        override fun listDirectories(path: String) = RootFileAccess.listDirectories(path)
        override fun permitVendorCeiling(lock: Boolean, release: Boolean): Boolean =
            PlatformCeilingAuthority.permitGpu(lock = lock, release = release).anyChannel
    }

    data class Device(
        val path: String,
        val name: String,
        val governor: String?,
        val governors: List<String>,
        val minFreq: Long?,
        val maxFreq: Long?,
        val currentFreq: Long?,
        val frequencies: List<Long>,
        val family: Family = Family.UNKNOWN,
        val loadPercent: Int? = null,
        val thermalC: Int? = null,
        val frequencyUnit: FrequencyUnit = FrequencyUnit.AMBIGUOUS,
        val minWritable: Boolean = false,
        val maxWritable: Boolean = false,
        val governorWritable: Boolean = false,
        val mtkFixedIndexPath: String? = null,
        val mtkLockWritable: Boolean = false,
        val mtkOppIndexByFrequency: Map<Long, String> = emptyMap(),
        val evidence: List<String> = emptyList(),
    ) {
        /**
         * دور هذه العقدة — **مشتقّ من اسمها ومسارها** لا محفوظًا معها.
         *
         * والمشتقّ مقصود لثلاثة أسباب: (١) لا يمكن أن ينحرف عن الاسم فتُوصف عقدة بغير دورها،
         * (٢) واللقطة المنشورة من الرفيق (`GpuDiscoverySnapshot`) تنقل الاسم والمسار ولا تنقل
         * الدور — فيُعاد اشتقاقه عند الفكّ بلا تعديل صيغة ولا نسخة، (٣) ولقطة كُتبت بنسخة أقدم
         * (فيها المراقب بدرجة مزوّد) تُقرأ في هذه النسخة بدورها الصحيح.
         */
        val role: NodeRole get() = nodeRoleOf("$name $path")

        val unitTrusted: Boolean get() = frequencyUnit != FrequencyUnit.AMBIGUOUS

        /**
         * هل تقبل عقدتا `min_freq`/`max_freq` كتابة **سقف** (مدى)؟
         *
         * وهذا هو السؤال الصحيح لطلب السقف، ووجود مسار تثبيت OPP لا يُبطله — وخلطُ الاثنين هو
         * العطب المقيس على جهاز حقيقي (rodin · MT6899): `rangeWritable` كان يشترط
         * `mtkFixedIndexPath == null`، فصار كل بروفايل على هذا الجهاز **تثبيتًا** لدرجة واحدة
         * بدل أن يكون سقفًا (والجهاز `devfreq` أصلًا، و`max_freq` فيه هو ما يقرأه المتصل سقفًا
         * حيًّا في [configurableMaxFrequency]). والتثبيت يجمّد التردد: «أداء» يعطي ٦٥٠ عالقًا.
         */
        val devfreqCeilingWritable: Boolean get() =
            minWritable && maxWritable && frequencies.isNotEmpty() && minFreq != null && maxFreq != null && unitTrusted

        /**
         * مسار «مدى» بالتقييد القديم: يمنع المدى حين يوجد مسار تثبيت MTK.
         *
         * وبقي للـ**تثبيت وحده** (اختيار مسار الكتابة في [applyValidated] و[requestForMode]):
         * كتابةُ مدى على جهاز له قفل OPP معناها «سقف»، والتثبيت يبقى لطلب التثبيت الصريح.
         */
        val rangeWritable: Boolean get() = devfreqCeilingWritable && mtkFixedIndexPath == null
        val exactLockWritable: Boolean get() = rangeWritable ||
            (mtkFixedIndexPath != null && mtkLockWritable && mtkOppIndexByFrequency.isNotEmpty() && unitTrusted)
        val provenMinFreq: Long? get() = frequencies.firstOrNull()
        val provenMaxFreq: Long? get() = frequencies.lastOrNull()
    }

    data class Selection(
        val state: SelectionState,
        val device: Device? = null,
        val candidates: List<Device> = emptyList(),
        val reason: String,
        val observedAtMs: Long = System.currentTimeMillis(),
    ) {
        /**
         * المرشّحات بأسمائها **وأدوارها** — للمخاطبة في السجل والتشخيص.
         *
         * ولماذا هنا لا عند كل منادٍ: سببُ غياب المزوّد بلا أسماء عُقَده لا يُشخَّص. والجهاز المقيس
         * (POCO X6 Pro · SM8735 · 2026-10-15) أغلق كل مقابض الـGPU بسبب واحد
         * (`multiple-equally-proven-gpu-providers`) ولم تحمل حزمته اسم شريك التعادل — فصار
         * الاسم والدور يُقالان معًا، فيُقرأ السبب من الحزمة نفسها لا من التخمين.
         *
         * **ومجلّد العقدة (`@`) ثالثهما لعلّة مقيسة (CI · 2026-10-16):** عقدتان قد تعلنان
         * `device_name` واحدًا (`mali-gpu` في الاختبار) فيتعادلان، وبالاسم وحده لا يُعرف أيّ
         * عقدتين تعادلتا. والمجلّد هو ما تُعدّ به النواة عُقَدها في `/sys/class/devfreq` فلا
         * يتكرّر — ويبقى `name:role` متلاصقًا كما تقرؤه الاختبارات والسجل.
         */
        val candidateSummary: String
            get() = candidates.joinToString(",") {
                "${it.name}:${it.role.token}@${it.path.substringAfterLast('/')}"
            }.ifBlank { "none" }

        /** العُقَد التي استُبعدت **بالدور** (مراقبات عرض النطاق) — للتشخيص لا للحكم. */
        val roleExcludedSummary: String
            get() = candidates.filter { it.role == NodeRole.BANDWIDTH_MONITOR }
                .joinToString(",") { it.name }
                .ifBlank { "none" }
    }

    data class Request(
        val minFreq: Long? = null,
        val maxFreq: Long? = null,
        val governor: String? = null,
        /** Release a fixed-index lock so the governor scales the GPU dynamically again. */
        val releaseLock: Boolean = false,
        /**
         * هل يرفع هذا الطلب سقف المصنّع قبل الكتابة؟
         *
         * `true` (الافتراضي: سلوك كل كاتب قائم لم يُعلن غيره) لمن يطلب **قدرة الجهاز** — كبروفايل
         * أداء لكل تطبيق، أو «المدى الكامل» في GPU Studio. ولا يعني تثبيتًا: يُمرَّر مع [lock]
         * المستقلّ في [Io.permitVendorCeiling] (كان مخلوطًا به فصار الطلب يُطفئ DVFS).
         *
         * و`false` لمن يكتب **سقفًا أدنى من قدرة الجهاز** (بروفايل تبريد): طلب تبريد لا يجوز أن
         * يرفع حماية المصنّع ثم يكتب سقفه فوقها، وإلا صار تسخينًا.
         */
        val releaseVendorCeiling: Boolean = true,
    )

    data class Baseline(
        val devicePath: String,
        val minFreq: Long?,
        val maxFreq: Long?,
        val governor: String?,
        val fixedIndex: String? = null,
    )

    data class TransactionResult(
        val requested: Request,
        val actual: Device?,
        val writeSucceeded: Boolean,
        val verified: Boolean,
        val rollbackAttempted: Boolean = false,
        val rollbackVerified: Boolean? = null,
        val error: String? = null,
    )

    private data class Candidate(val device: Device, val score: Int)

    /**
     * A GPU **fact** as Atlas may hold it.
     *
     * Every writability flag of [Device] is absent, not `false`: a projection that cannot express
     * "this could be written" cannot leak a control decision, and `P3`/`T3.4` requires exactly that.
     * The frequency unit stays attached because it is the one thing that decides whether a number
     * means anything at all.
     */
    data class GpuFact(
        val path: String,
        val name: String,
        val family: Family,
        val governor: String?,
        val governors: List<String>,
        val minFreq: Long?,
        val maxFreq: Long?,
        val currentFreq: Long?,
        val frequencies: List<Long>,
        val frequencyUnit: FrequencyUnit,
        val loadPercent: Int?,
        val thermalC: Int?,
        val evidence: List<String>,
    ) {
        /** دور العقدة — كما في [Device.role]: مشتقّ من الاسم، فلا يتخلّف عن صاحبه. */
        val role: NodeRole get() = nodeRoleOf("$name $path")

        val unitTrusted: Boolean get() = frequencyUnit != FrequencyUnit.AMBIGUOUS
        val provenMinFreq: Long? get() = frequencies.firstOrNull()
        val provenMaxFreq: Long? get() = frequencies.lastOrNull()
    }

    enum class GpuObservationState { AVAILABLE, AMBIGUOUS, UNAVAILABLE }

    data class GpuObservation(
        val state: GpuObservationState,
        /** Every candidate, so an ambiguous device is visible rather than silently narrowed. */
        val devices: List<GpuFact>,
        /** The provider the scanner considers best; `null` when nothing qualified or several tied. */
        val bestPath: String?,
        val reason: String,
        val observedAtMs: Long,
    )

    /**
     * Adapts a reader to the legacy [Io] by answering the writer half **locally**.
     *
     * It never delegates `writable` and never performs `write`, so running discovery through it is
     * not "a read that happened to be refused" — the attempt does not exist. This is what lets Atlas
     * reuse the shipped candidate/selection code without a second copy of it.
     */
    class ReadOnlyIo(private val reads: ReadIo) : Io {
        override fun exists(path: String): Boolean = reads.exists(path)
        override fun read(path: String): String? = reads.read(path)
        // **والدفعة تمرّ كما هي** ولا تُفكَّك إلى فرديّات: لو تركنا الافتراضيّ هنا لصار
        // كل قارئ دفعيّ يمرّ عبر Atlas يُقرأ عقدةً عقدة — أي يُهدَر بالضبط ما يمنحه §4.
        override fun readMany(paths: List<String>): List<String?> = reads.readMany(paths)
        override fun listDirectories(path: String): List<String> = reads.listDirectories(path)
        override fun writable(path: String): Boolean = false
        override fun write(path: String, value: String): Boolean = false
    }

    fun selection(io: Io = SystemIo): Selection {
        val scan = scan(io)
        val local = scan.candidates
        val blinded = scan.listed == 0 && io.exists(ROOT)
        // والشرط **قابلية القراءة** لا مجرّد وجود مرشّح: عقدة تُرى ولا يُقرأ منها عمود
        // واحد ليست قياسًا، وعليها يُتبنّى قياس الرفيق (المقيس على SM8850 · ٢٠٢٦-١٠-٢١:
        // المسح المحلي عاد بقشرة بلا ساعة فظلت الشاشة «Aucune lecture»).
        val hasReadableLocal = local.any {
            it.device.role == NodeRole.GPU_CLOCK && it.device.unitTrusted && it.device.frequencies.isNotEmpty()
        }
        if (hasReadableLocal || io !== SystemIo) return select(local, blinded)
        // العمى المحلي وحده لا يعني الغياب: لقطة الرفيق (تُقاس بالجذر حيث تُرى العقد)
        // تُتبنّى بوسمها حين لا يجد العدّ المحلي قياسًا — والحارس `SystemIo` حتى لا تمسّ
        // اختبارات الموجّه الوهمي قرصًا حقيقيًا عبر `RootFileAccess`.
        val published = GpuDiscoverySnapshot.read()?.entries.orEmpty()
        if (published.isEmpty()) return select(local, blinded)
        // **والسلطة تُعاد قراءتها هنا لا تُنقل:** اللقطة تنقل ما رآه الرفيق من قيم، ولا
        // تنقل حقّ الكتابة (تحفظه `false` عن قصد). ولو تُرك كذلك لصار كل جهاز متبنّى
        // «للقراءة فقط» حتى لو كان الجذر يكتب عقده الآن — وهو عكس المطلوب. فتُقاس
        // القابلية في هذه العملية بموصلها الحقيقي، وتبقى الكتابة محروسة بحكمٍ بعدها.
        return select(published.map { Candidate(it.device.withWritabilityFrom(io), it.score) })
    }

    /**
     * يعيد بناء أعلام الكتابة **في العملية الحالية** لجهاز جاء من لقطة الرفيق.
     *
     * والقيم لا تُمسّ: ما قرأه الرفيق يبقى كما هو. والذي يُقاس هنا هو ما لا تستطيع
     * لقطة أن تحمله — قدرة هذا السياق على الكتابة (`IO.writable` يمرّ بالجذر أيضًا).
     */
    private fun Device.withWritabilityFrom(io: Io): Device = copy(
        minWritable = io.writable("$path/min_freq"),
        maxWritable = io.writable("$path/max_freq"),
        governorWritable = io.writable("$path/governor"),
        mtkLockWritable = mtkFixedIndexPath?.let(io::writable) == true,
    )

    /** آخر نشر للقطة (ذاكرة العملية) — النشر كل ٥ دقائق لا كل دورة. */
    @Volatile private var lastSnapshotPublishMs = 0L

    /**
     * نشر لقطة الاكتشاف للعمليات العمياء — من الرفيق الجذري وحده (يرى العقد كلها).
     * لا تُنشَر الفراغ (فلا تُخفي «لم يُقَس» عن «لا عتاد») ولا تُرمى الأخطاء: العائد
     * نجاح النشر وحده. والتكلفة في الدورة سطر زمني واحد؛ والنشر نفسه كل ٥ دقائق.
     */
    fun publishDiscoverySnapshot(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (nowMs - lastSnapshotPublishMs < GpuDiscoverySnapshot.PUBLISH_INTERVAL_MS) return false
        lastSnapshotPublishMs = nowMs
        val local = runCatching { discoverCandidates(SystemIo) }.getOrNull().orEmpty()
        if (local.isEmpty()) return false
        val entries = local.map { GpuDiscoverySnapshot.Entry(it.device, it.score) }
        return GpuDiscoverySnapshot.write(GpuDiscoverySnapshot.encode(nowMs, entries))
    }

    /**
     * Read-only discovery for Atlas: one pass over the same candidate code, reported as facts.
     *
     * Ambiguity is preserved exactly as [selection] computes it (no "first GPU wins"), and the unit
     * ambiguity survives too, so a caller cannot read a value whose meaning was never established.
     */
    fun observe(reads: ReadIo): GpuObservation {
        val io = ReadOnlyIo(reads)
        val scan = scan(io)
        val selection = select(scan.candidates, blinded = scan.listed == 0 && io.exists(ROOT))
        return GpuObservation(
            state = when (selection.state) {
                SelectionState.UNAVAILABLE -> GpuObservationState.UNAVAILABLE
                SelectionState.AMBIGUOUS -> GpuObservationState.AMBIGUOUS
                else -> GpuObservationState.AVAILABLE
            },
            devices = scan.candidates.map { fact(it.device) },
            bestPath = selection.device?.path,
            reason = selection.reason,
            observedAtMs = selection.observedAtMs,
        )
    }

    private fun fact(device: Device): GpuFact = GpuFact(
        path = device.path,
        name = device.name,
        family = device.family,
        governor = device.governor,
        governors = device.governors,
        minFreq = device.minFreq,
        maxFreq = device.maxFreq,
        currentFreq = device.currentFreq,
        frequencies = device.frequencies,
        frequencyUnit = device.frequencyUnit,
        loadPercent = device.loadPercent,
        thermalC = device.thermalC,
        evidence = device.evidence,
    )

    private fun select(candidates: List<Candidate>, blinded: Boolean = false): Selection {
        if (candidates.isEmpty()) {
            // وغيابُ العدّ يُفرَّق عن غياب المزوّد: صفرُ أسماء مع جذرٍ **موجود** تعني «حُجب عن
            // القراءة» لا «لا عقدة على هذا الجهاز» — وكلٌّ له إجراء مختلف عند من يقرأ الحزمة.
            return Selection(
                SelectionState.UNAVAILABLE,
                reason = if (blinded) ROOT_UNREADABLE_REASON else "no-gpu-provider",
            )
        }
        // والدور يُفصل **قبل** المقارنة بالدرجات: مراقب عرض النطاق ليس مزوّدًا أضعف، بل ليس مزوّدًا.
        // ولو تُرك في المقارنة لَما نفع خفضُ درجته: عقدة واحدة بدرجة عالية تكفي لخلق تعادل مُعلَن،
        // وهو ما أغلق كل مقابض GPU على جهاز مُقاس (انظر [NodeRole]).
        val monitors = candidates.filter { it.device.role == NodeRole.BANDWIDTH_MONITOR }
        val providers = candidates.filterNot { it.device.role == NodeRole.BANDWIDTH_MONITOR }
        if (providers.isEmpty()) {
            // ولا يُعَلَّم الغياب بغياب آخر: «عُقد رُصدت وليست مزوّدًا» تختلف عن «لا عقدة تُرصد»،
            // وكلٌّ له إجراء مختلف عند من يقرأ السجل. والعُقد المرصودة تُعاد في `candidates` بالاسم.
            return Selection(
                SelectionState.UNAVAILABLE,
                candidates = monitors.map(Candidate::device),
                reason = BANDWIDTH_MONITOR_REASON,
            )
        }
        val bestScore = providers.maxOf(Candidate::score)
        val best = providers.filter { it.score == bestScore }
        if (best.size != 1) {
            return Selection(
                SelectionState.AMBIGUOUS,
                candidates = best.map(Candidate::device),
                reason = "multiple-equally-proven-gpu-providers",
            )
        }
        val device = best.single().device
        val writable = device.unitTrusted && (device.rangeWritable || device.exactLockWritable || device.governorWritable)
        return Selection(
            state = if (writable) SelectionState.READY else SelectionState.READ_ONLY,
            device = device,
            // وكل المرشّحات تبقى معروضة (ومنها المراقبات المستبعدة): من ينشر تشخيصًا أو لقطة
            // يرى ما رُصد كاملًا، وليس ما اختير وحده.
            candidates = candidates.map(Candidate::device),
            reason = when {
                !device.unitTrusted -> "ambiguous-frequency-unit"
                writable -> "verified-gpu-provider"
                else -> "telemetry-only-provider"
            },
        )
    }

    fun refresh(path: String, io: Io = SystemIo): Device? {
        discoverCandidates(io).firstOrNull { it.device.path == path }?.let { return it.device }
        // والسطح البديل ليس تحت `devfreq`، فلا يجده المسح الأساسي — ويُعاد بناؤه بنفس قارئه
        // (لا يُعاد استعمال جهاز قديم: القيمة تُقاس في كل نداء كما في المسح العادي).
        return qualcommClockFallback(io, onlyBase = path)?.device
    }

    fun effectiveFrequency(device: Device, io: Io = SystemIo): Long? =
        currentExactLockFrequency(device, io) ?: device.maxFreq

    /**
     * Returns the highest frequency that is both advertised by the provider and
     * currently allowed by its live `max_freq` ceiling.  The advertised OPP
     * table is a capability catalogue; `max_freq` is a runtime constraint and
     * can be lowered by a vendor thermal/power daemon. Treating the catalogue
     * maximum as the live maximum made per-app profiles repeatedly request an
     * impossible value (for example 1.3 GHz while the driver was capped at
     * 754 MHz).
     */
    fun configurableMaxFrequency(device: Device): Long? {
        val advertisedMax = device.frequencies.filter { it > 0L }.maxOrNull()
        val liveMax = device.maxFreq?.takeIf { it > 0L }
        return when {
            advertisedMax == null -> liveMax
            liveMax == null -> advertisedMax
            else -> minOf(advertisedMax, liveMax)
        }
    }

    /**
     * يحرّر سقف المصنّع **بلا معاملة** — ويُنادى **قبل** أي حكم «مُلبّى».
     *
     * ولماذا خارج المعاملة: قياس من جهاز حقيقي (rodin · MTK6899) يقول إن `max_freq` يقرأ أعلى
     * درجة مُعلنة **أصلًا** (1300000000) بينما الجهاز يعمل على 260MHz. فيقرأ المُحكِّم قيمة تساوي
     * الطلب، فيحكم «مُلبّى» ويتخطّى `apply` — و**تحرير السلطة كان يسكن داخل `apply`**. فلم يُنفَّذ
     * ولا مرة واحدة في الحزمة كاملة (صفر كتابة على عقد GED والتبريد، مقابل ١٢ كتابة على نظيرها
     * في CPU). فالتحرير هنا: يُنفَّذ ولو كان الحكم «مُلبّى» — لأن الطلب عند قدرة الجهاز **هو**
     * التحرير نفسه، لا كتابة تردد.
     *
     * والكتابة تمرّ بعقدها **عند الحاجة فقط** ([PlatformCeilingAuthority] تقرأ قبل أن تكتب)، فنداؤه
     * في كل جلسة تطبيق لا يُنتج سطرًا على جهاز مُحرَّر أصلًا.
     */
    fun releaseVendorCeiling(io: Io = SystemIo): Boolean =
        io.permitVendorCeiling(lock = false, release = true)

    /**
     * سقف العقدة بوحدة Hz — و`null` تعني «لا قياس» لا «صفر».
     *
     * والوحدة تُحوَّل هنا لأن الكائن يحفظ أرقامه بلغة العقدة نفسها (انظر `frequencyUnit`)، والحكم
     * على السقف يُقارَن بطلب المستخدم بالهرتز. ووحدة غير مؤكَّدة ⇒ `null`: لا يُبنى حكم على وحدة مجهولة.
     */
    fun nodeCeilingHz(device: Device): Long? {
        if (device.frequencyUnit == FrequencyUnit.AMBIGUOUS) return null
        val raw = device.maxFreq?.takeIf { it > 0L }
            ?: device.frequencies.filter { it > 0L }.maxOrNull()
            ?: return null
        return raw * device.frequencyUnit.hzMultiplier
    }

    /**
     * قراءة السقف الفعلي: سقف العقدة + ما تقوله عقد سلطة المنصّة.
     *
     * وهذا هو المُقاس الذي يصحّ الحكم عليه في طلب «سقف»: `max_freq` وحده لا يكفي — على هذا
     * الجهاز يقرأ 1300 بينما التبريد وسقف GED هما من يقصّ. ولا يوجد وهم `Io` للنصف الثاني عن
     * قصد: `PlatformCeilingAuthority` تقرأ عقد العتاد مباشرة (لا موصل فيها)، فالاختبار يبني
     * [GpuCeilingPolicy.CeilingReading] بنفسه ويحكم بـ[GpuCeilingPolicy.ceilingSatisfied] — وهي خالصة.
     */
    fun ceilingReading(device: Device, io: Io = SystemIo): GpuCeilingPolicy.CeilingReading {
        val platform = PlatformCeilingAuthority.gpuPlatformCeiling()
        return GpuCeilingPolicy.CeilingReading(
            nodeCeilingHz = nodeCeilingHz(device),
            platformUpbound = platform.upboundRaw,
            platformCoolingHeld = platform.coolingHeld,
            // وقفل OPP الثابت يُقرأ **هنا** معثر لم يكن يُقرأ في أي حكم سقف: على MediaTek يقصّ
            // `fix_target_opp_index` التردد من خارج `devfreq`، فجهاز مُثبَّت يقرأ `max_freq` عند
            // القدرة بينما هو عالق على درجة واحدة — والقراءة وحدها كانت تُصدّقه «مُلبّى».
            // وعدم القراءة (`null`) لا يُدَّعى قفلًا ولا حرية: مسار القفل غير موجود أصلًا هو
            // الحالة الوحيدة التي تُعطي `null` (ووجود المسار يُكتشف قبل ذلك في [readCandidate]).
            lockActive = device.mtkFixedIndexPath?.let { path ->
                io.read(path)?.let { MtkGpuOppTable.parseIndex(it) != "-1" }
            },
        )
    }

    /**
     * التردد الذي **يجري عليه الجهاز فعلًا** — لا ما نظنّ أننا كتبناه.
     *
     * وله موضعان لسبيين: عقدة GED هي ما يبلّغ به MTK عن تردد الGPU الجاري، وعقدة `cur_freq` هي
     * البديل العام. ولا يُخترع تردد: قيمة لا تطابق أي درجة مُعلنة تُعاد `null` (قد تكون وحدة أخرى
     * لا نعرفها)، ولا يُبنى عليها حكم — بل يُقال «لم يُقس». وهذا ما يجعل فرقًا بين «ثبّتنا الفهرس»
     * و«الجهاز يعمل عليه» مرئيًّا بدل أن يبقى صدى النواة هو الدليل الوحيد.
     */
    fun currentFrequencyHz(device: Device, io: Io = SystemIo): Long? {
        val advertised = device.frequencies.filter { it > 0L }
        for (path in listOf(GED_CURRENT_FREQUENCY, "${device.path}/cur_freq", "${device.path}/gpuclk")) {
            val raw = io.read(path)?.trim()?.toLongOrNull() ?: continue
            if (raw <= 0L) continue
            normalizeToAdvertisedHz(raw, advertised)?.let { return it }
        }
        return null
    }

    /**
     * Maps a requested frequency to a real OPP that the provider can currently
     * carry.  It never returns an OPP above the live configurable ceiling.
     */
    fun snapToAvailableAtOrBelow(
        device: Device,
        requestedHz: Long,
        respectLiveCeiling: Boolean = true,
    ): Long? {
        val frequencies = device.frequencies.filter { it > 0L }.distinct().sorted()
        if (frequencies.isEmpty()) return null
        // Cooling profiles respect the live vendor ceiling. A full-capability request
        // must not: its contract is to ask for the highest advertised OPP and let the
        // platform authority report any lower ceiling it enforces after the write.
        val cap = if (respectLiveCeiling) configurableMaxFrequency(device) else null
        val usable = if (cap != null) frequencies.filter { it <= cap } else frequencies
        if (usable.isEmpty()) return null
        return usable.lastOrNull { it <= requestedHz } ?: usable.first()
    }

    /**
     * الصيغة القياسية لقيمة مفتاح `gpu_frequency:<device>`.
     *
     * المُحكِّم يُثبت المعاملة بـ**تساوي نصّين**: القيمة المطلوبة والقيمة
     * المقروءة من السائق بعد الكتابة. فصيغة تحمل المدى الرقمي وحده لا تستطيع
     * إثبات طلبٍ مضمونه في حقل آخر: اختيار مُحكِّم (governor)، أو تحرير قفل
     * OPP الثابت في MediaTek. في الحالتين تنجح الكتابة، ولا يساوي المقروء
     * المطلوبَ أبدًا، فيُصنَّف تغييرٌ ناجح فاشلًا غير مؤكَّد وتُعاد الحالة
     * السابقة — والمستخدم يرى المقبض يرتدّ بلا سبب مكتوب.
     *
     * فالصيغة هنا تحمل كل حقل يمكن للطلب أن يمسّه، و[encodeLive] تُسقِط الحالة
     * الحية على **نفس** الحقول التي يمسّها ذلك الطلب وحده؛ فيصير التساوي معناه
     * «هذا الطلب مُلبّى» لا شيئًا آخر.
     */
    fun encodeRequest(request: Request): String = listOf(
        if (request.minFreq != null && request.maxFreq != null) {
            "${request.minFreq}:${request.maxFreq}"
        } else UNTOUCHED,
        request.governor ?: UNTOUCHED,
        if (request.releaseLock) RELEASED else UNTOUCHED,
    ).joinToString(SCHEMA_SEPARATOR)

    /** الحالة الحية مُسقَطةً على حقول [request] وحدها — لا على كل الحالة. */
    fun encodeLive(device: Device, request: Request, io: Io = SystemIo): String = listOf(
        if (request.minFreq != null && request.maxFreq != null) {
            "${device.minFreq?.toString().orEmpty()}:${device.maxFreq?.toString().orEmpty()}"
        } else UNTOUCHED,
        if (request.governor != null) device.governor ?: UNREADABLE else UNTOUCHED,
        if (request.releaseLock) {
            if (fixedLockReleased(device, io)) RELEASED else HELD
        } else UNTOUCHED,
    ).joinToString(SCHEMA_SEPARATOR)

    /**
     * هل لا يوجد قفل OPP ثابت يقيّد التردد؟ صحيح أيضًا لجهاز لا يملك مسار قفل
     * أصلًا (لا شيئ لنحرّره)، وخاطئ إن كان المسار موجودًا وغير مقروء — لا يُدّعى
     * تحرير قفل لم يُقرأ.
     */
    /**
     * هل الحالة الحية تُلبّي الطلب المُرمَّز؟ — حكم [requestSatisfied] وحده يُمرَّر للمُحكِّم.
     *
     * ولماذا لا يكفي التساوي الحرفي: حقول المدى (`min:max`) تُدمج/تُقيَّد من السائق
     * وسياسة الـvendor. فطلبٌ `500000000:754000000` مقابل قراءة `500000000:600000000`
     * هو **تلبية** (السقف لم يُخرَق) لا فشل، والتساوي كان يسترجع خط الأساس ثم يُعاد
     * الطلب في كل دورة انحراف بلا نتيجة. وبقية الحقول تبقى تساويًا حرفيًّا لأن معناها
     * الوحيد كذلك: مُحكِّم اخترناه، أو تحرير قفل OPP ثابت. والصيغة تعيش هنا وحدها
     * لأن هذا الملف هو الذي يُنشئها ([encodeRequest]/[encodeLive])، فلا تُنسخ في مكان آخر.
     */
    fun requestSatisfied(desired: String, actual: String?): Boolean {
        if (actual == null) return false
        if (!desired.contains(SCHEMA_SEPARATOR) && !actual.contains(SCHEMA_SEPARATOR)) {
            return HardwareVerification.rangeContained(desired, actual)
        }
        val want = desired.split(SCHEMA_SEPARATOR)
        val live = actual.split(SCHEMA_SEPARATOR)
        if (want.size != live.size) return false
        return want.indices.all { index ->
            if (index != 0) return@all want[index] == live[index]
            if (want[index] == UNTOUCHED) live[index] == UNTOUCHED
            else HardwareVerification.rangeContained(want[index], live[index])
        }
    }

    fun fixedLockReleased(device: Device, io: Io = SystemIo): Boolean =
        MtkGpuOppTable.isReleased(device, io)

    fun encodeBaseline(baseline: Baseline): String = listOf(
        baseline.devicePath,
        baseline.minFreq?.toString().orEmpty(),
        baseline.maxFreq?.toString().orEmpty(),
        baseline.governor.orEmpty(),
        baseline.fixedIndex.orEmpty(),
    ).joinToString("\u001f")

    fun decodeBaseline(value: String): Baseline? {
        val parts = value.split('\u001f')
        if (parts.size != 5 || parts[0].isBlank()) return null
        return Baseline(
            devicePath = parts[0],
            minFreq = parts[1].toLongOrNull(),
            maxFreq = parts[2].toLongOrNull(),
            governor = parts[3].takeIf(String::isNotBlank),
            fixedIndex = parts[4].takeIf(String::isNotBlank),
        )
    }

    fun captureBaseline(device: Device, io: Io = SystemIo): Baseline = Baseline(
        devicePath = device.path,
        minFreq = device.minFreq,
        maxFreq = device.maxFreq,
        governor = device.governor,
        fixedIndex = device.mtkFixedIndexPath?.let(io::read)?.let(MtkGpuOppTable::parseIndex),
    )

    fun requestForMode(device: Device, mode: IntentMode): Request? {
        val advertised = device.frequencies.filter { it > 0L }.distinct().sorted()
        if (advertised.isEmpty()) return null
        // والسقف الحيّ **لا** يُقيَّد به التخطيط هنا، وهو تغيير مقصود لا إسقاط سهو.
        //
        // كان `frequencies` مُرشَّحًا بـ`configurableMaxFrequency` (= أدنى ما بين المُعلن والسقف
        // الحيّ)، والسقف الحيّ على `devfreq/max_freq` قيمةٌ **يكتبها هذا التطبيق نفسه** لسقف
        // بروفايل. فيصير خفضُنا نحن هو «قدرة الجهاز» في نظر الطلب التالي: اختيار «power» (٥٢٠)
        // ثم وضعُ القدرة الكاملة يعطي طلبًا أقصاه **٥٢٠**، فلا يزيد التردد عمّا نُقص أبدًا،
        // والتقرير يقول «نُفِّذ». وهذا العطب المقيس على rodin · MT6899 · 2026-09-22 في per-app
        // وشاشة GPU وMAX AI معًا.
        //
        // والكتابة فوق السقف الحيّ ليست مخاطرة كتمان: القيمة تُكتب فعلًا، ثم يُقاس ما تقبل
        // المنصّة أن تُبقيه ويُعلَن برقمه (`gpu-ceiling-open-below-request`)، بل إن النسبة تُحسب
        // من **القدرة** كما هو مُوثَّق لكل بروفايل.
        val frequencies = advertised
        if (frequencies.isEmpty()) return null
        val last = frequencies.lastIndex
        val min = frequencies.first()
        val max = frequencies.last()
        // وسقف المقصود يُنفَّذ بكتابة مدى كلّما قبل الجهاز المدى، ولو كان له مسار تثبيت OPP:
        // التثبيت يجمّد التردد، ولا يُستعمل إلا حين لا يُقبل المدى فعلًا.
        if (device.devfreqCeilingWritable) return when (mode) {
            // `efficiency` **طلب تبريد**: يسقّف دون القدرة، فلا يجوز أن يرفع حماية وضعها المصنّع
            // (وإلا صار طلبُ تبريد تسخينًا — وهو نفس السبب الذي فُصل من أجله المعاملان).
            // و`adaptive`/`sustained` طلبا قدرة (مدى كامل · وسقف عند القدرة) فيَرفعانها.
            IntentMode.EFFICIENCY -> Request(min, frequencies[(last * 0.40f).toInt().coerceIn(0, last)], releaseVendorCeiling = false)
            IntentMode.ADAPTIVE -> Request(min, max)
            IntentMode.SUSTAINED -> Request(frequencies[(last * 0.65f).toInt().coerceIn(0, last)], max)
        }
        if (device.exactLockWritable && device.mtkFixedIndexPath != null) return when (mode) {
            IntentMode.EFFICIENCY -> Request(min, min)
            IntentMode.ADAPTIVE -> Request(releaseLock = true)
            IntentMode.SUSTAINED -> {
                val sustained = frequencies[(last * 0.65f).toInt().coerceIn(0, last)]
                Request(sustained, sustained)
            }
        }
        return null
    }

    fun validate(device: Device, request: Request): String? {
        if (request.minFreq == null && request.maxFreq == null && request.governor == null && !request.releaseLock) return "empty-request"
        if (!device.unitTrusted) return "ambiguous-frequency-unit"
        if (request.releaseLock) {
            if (request.minFreq != null || request.maxFreq != null || request.governor != null) return "invalid-request"
            if (device.mtkFixedIndexPath == null || !device.mtkLockWritable) return "release-lock-unavailable"
            return null
        }
        if (request.governor != null && request.governor !in device.governors) return "unsupported-governor"
        if (request.governor != null && !device.governorWritable) return "governor-read-only"
        if (request.minFreq != null || request.maxFreq != null) {
            if (request.minFreq == null || request.maxFreq == null) return "incomplete-range"
            if (request.minFreq > request.maxFreq) return "invalid-range"
            if (request.minFreq !in device.frequencies || request.maxFreq !in device.frequencies) return "unsupported-frequency"
            val exact = request.minFreq == request.maxFreq
            if (exact && !device.exactLockWritable) return "exact-lock-read-only-or-unproven"
            if (!exact && !device.devfreqCeilingWritable) return "range-read-only-or-unproven"
            if (device.mtkFixedIndexPath == null && (device.minFreq == null || device.maxFreq == null)) return "baseline-unreadable"
        }
        return null
    }

    fun apply(device: Device, request: Request, io: Io = SystemIo): TransactionResult {
        val selected = selection(io)
        val live = selected.device?.takeIf { it.path == device.path }
            ?: return TransactionResult(
                request,
                null,
                false,
                false,
                error = if (selected.state == SelectionState.AMBIGUOUS) "provider-ambiguous" else "provider-disappeared",
            )
        val error = validate(live, request)
        if (error != null) return TransactionResult(request, live, false, false, error = error)
        return applyValidated(live, request, io)
    }

    /**
     * Applies a request to an already-selected and already-validated provider.
     *
     * The caller must own the control transaction (normally through
     * [HardwareControlArbiter]). This seam prevents a control arbiter callback
     * from selecting and transacting the same hardware a second time.
     */
    fun applyValidated(live: Device, request: Request, io: Io = SystemIo): TransactionResult {
        val error = validate(live, request)
        if (error != null) return TransactionResult(request, live, false, false, error = error)
        return when {
            // مسارا القفل الثابت يسكنان `MtkGpuFixedIndex` (فصل ملف بحجم الحدّ وبكائنه الأصلي):
            // تحرير الفهرس، أو تثبيت درجة واحدة بفهرسها.
            request.releaseLock -> MtkGpuFixedIndex.release(live, request, io)
            request.minFreq != null && request.minFreq == request.maxFreq && live.mtkFixedIndexPath != null -> MtkGpuFixedIndex.pin(live, request, io)
            else -> applyDevfreq(live, request, io)
        }
    }

    private fun applyDevfreq(live: Device, request: Request, io: Io): TransactionResult {
        val touchesRange = request.minFreq != null && request.maxFreq != null
        val touchesGovernor = request.governor != null
        if (touchesRange && (live.minFreq == null || live.maxFreq == null)) {
            return TransactionResult(request, live, false, false, error = "baseline-unreadable")
        }
        if (touchesGovernor && live.governor == null) {
            return TransactionResult(request, live, false, false, error = "baseline-unreadable")
        }
        val baseline = captureBaseline(live, io)
        // سلطة المنصّة **قبل** الكتابة، والترتيب مقصود: على MediaTek يقمع جهاز تبريد الحرارة وسقف
        // GED أي رفع فوقهما، فيُقرأ `differs` بلا سبب ظاهر في السجل (قيس على جهاز حقيقي:
        // 1300000000 ⇒ 754000000). ولو كتبنا أولًا لبقي أثر القمع مسجّلًا عطلًا انتهى.
        if (touchesRange) {
            io.permitVendorCeiling(
                // `lock` = تثبيت قيمة واحدة، لا «تحرير السقف». خلطهما كان يُطفئ DVFS في كل طلب
                // سقف (والسقف عكس التثبيت)، فيثبت التردد الذي صادف وجوده لحظة الطلب.
                lock = request.minFreq == request.maxFreq,
                release = request.releaseVendorCeiling,
            )
        }
        // وقفل OPP ثابت قائم يُرفع **مع** كتابة السقف: هو ليس السقف الذي طُلب، بل جمودٌ من جلسة
        // سابقة (أو من أداة أخرى) — وإبقاؤه يجعل كتابة المدى بلا أثر مرئي: الجهاز يبقى على درجته
        // المجمَّدة، ويُقرأ السقف عند القدرة فيُقال «نُفِّذ». والفهرس المُرفوع محفوظ في خط الأساس
        // ([Baseline.fixedIndex]) فيُعاده [restoreBaseline] عند الخروج أو [restoreTouchedBaseline]
        // عند فشل الكتابة.
        val lockPath = live.mtkFixedIndexPath
        val heldIndex = lockPath?.let { io.read(it)?.let(MtkGpuOppTable::parseIndex) }?.takeIf { it != "-1" }
        if (touchesRange && heldIndex != null && lockPath != null) {
            io.write(lockPath, "-1")
            MtkGpuFixedIndex.restoreDynamicScaling(io)
        }
        var wrote = true
        if (touchesRange) wrote = writeRange(live, request.minFreq, request.maxFreq, io)
        if (wrote && touchesGovernor) wrote = io.write("${live.path}/governor", request.governor)
        val actual = refresh(live.path, io)
        if (wrote && matches(actual, request)) return TransactionResult(request, actual, true, true)

        val rollbackVerified = restoreTouchedBaseline(baseline, request, io)
        // وبعد التراجع يُعاد الوضع الطبيعي للسلطة: طلب تثبيت فشل كان قد أوقف DVFS في GED، وإبقاؤه
        // موقوفًا يُثبّت التردد الذي صادف وجوده — أي يُنتج العطب الذي جاء التراجع لإلغائه.
        // و`release = false`: التراجع يُعيد DVFS ولا يلمس سقف المصنّع (رفعه هنا كان سيصير تهرّبًا
        // من قمع فشل طلبُناه نحن).
        io.permitVendorCeiling(lock = false, release = false)
        return TransactionResult(
            requested = request,
            actual = refresh(live.path, io),
            writeSucceeded = wrote,
            verified = false,
            rollbackAttempted = true,
            rollbackVerified = rollbackVerified,
            error = if (rollbackVerified) "apply-not-verified-baseline-restored" else "apply-and-rollback-failed",
        )
    }

    private fun restoreTouchedBaseline(baseline: Baseline, request: Request, io: Io): Boolean {
        val live = refresh(baseline.devicePath, io) ?: return false
        var restoredAll = true
        if (request.minFreq != null || request.maxFreq != null) {
            val min = baseline.minFreq
            val max = baseline.maxFreq
            restoredAll = if (min == null || max == null) false else writeRangeRaw(live, min, max, io) && restoredAll
        }
        if (request.governor != null) {
            val governor = baseline.governor
            restoredAll = if (governor == null) false else io.write("${live.path}/governor", governor) && restoredAll
        }
        // والفهرس الذي رفعناه مع السقف يُعاد معه: تراجعٌ يُبقي الجهاز مجمَّدًا على درجة كان قد
        // تحرّر منها **ليس** تراجعًا. ويُكتب فقط حين كان خط الأساس فهرسًا حقيقًّا.
        val lockPath = live.mtkFixedIndexPath
        val baselineIndex = baseline.fixedIndex
        if (request.minFreq != null && request.maxFreq != null && lockPath != null &&
            baselineIndex != null && baselineIndex != "-1"
        ) {
            val wroteIndex = io.write(lockPath, baselineIndex)
            restoredAll = wroteIndex && io.read(lockPath)?.let(MtkGpuOppTable::parseIndex) == baselineIndex && restoredAll
        }
        val restored = refresh(baseline.devicePath, io) ?: return false
        return restoredAll &&
            (request.minFreq == null || (restored.minFreq == baseline.minFreq && restored.maxFreq == baseline.maxFreq)) &&
            (request.governor == null || restored.governor == baseline.governor)
    }

    fun restoreBaseline(baseline: Baseline, io: Io = SystemIo): Boolean {
        val live = refresh(baseline.devicePath, io) ?: return false
        var touched = false
        var restoredAll = true
        if (baseline.fixedIndex != null && live.mtkFixedIndexPath != null) {
            touched = true
            val wrote = io.write(live.mtkFixedIndexPath, baseline.fixedIndex)
            restoredAll = wrote && io.read(live.mtkFixedIndexPath)?.let(MtkGpuOppTable::parseIndex) == baseline.fixedIndex && restoredAll
            // والقفل يُعاد إلى ما كان، فالتوسّع الديناميكي يُعاد معه: إبقاء DVFS مطفأً بعد أن زال
            // سبب إطفائه يُجمّد التردد المنخفض الذي صادف وجوده — وهو العطب الذي تُسمّيه هذه الطبقة
            // «أسوأ من عدم التثبيت» لأنه لا يظهر في أي شاشة.
            MtkGpuFixedIndex.restoreDynamicScaling(io)
        }
        // والمدى يُعاد حين **يخالف** ما كان — ولو وُجد مسار قفل OPP على هذا الجهاز.
        //
        // وكان الشرط `live.mtkFixedIndexPath == null`: أي أنه على MediaTek — وهو الجهاز المقيس —
        // **لا يُستعاد المدى أبدًا**. فتبقى `max_freq` على آخر قيمة كتبناها بعد خروج التطبيق أو
        // بعد التراجع، وهو ما يُقرأ من المستخدم حرفيًّا: «بعد إغلاق التطبيق لا يرجع للوضع
        // الافتراضي» — وفي السجل: `WRITE_CHECK path=/sys/class/devfreq/13000000.mali/min_freq
        // wrote=260000000 matched` ثم `max_freq wrote=520000000 matched` ولا استرجاع بعدهما.
        //
        // والشرط الجديد هو الفرق نفسه لا وجود المسار: لا كتابة إن كان المدى الحيّ هو خط الأساس
        // (فلا ضجيج على مقبض لم يُمَسّ)، وكتابة إن انحرف — ومعها يُحكم على النتيجة كما كان.
        val rangeDrifted = baseline.minFreq != null && baseline.maxFreq != null &&
            (live.minFreq != baseline.minFreq || live.maxFreq != baseline.maxFreq)
        if (rangeDrifted) {
            touched = true
            restoredAll = writeRangeRaw(live, baseline.minFreq!!, baseline.maxFreq!!, io) && restoredAll
        }
        if (baseline.governor != null && live.governorWritable) {
            touched = true
            restoredAll = io.write("${live.path}/governor", baseline.governor) && restoredAll
        }
        if (!touched) return false
        val restored = refresh(baseline.devicePath, io) ?: return false
        return restoredAll &&
            (baseline.minFreq == null || (restored.minFreq == baseline.minFreq && restored.maxFreq == baseline.maxFreq)) &&
            (baseline.governor == null || !live.governorWritable || restored.governor == baseline.governor)
    }

    fun setGovernor(device: Device, governor: String, io: Io = SystemIo): VerificationResult<String> {
        val result = apply(device, Request(governor = governor), io)
        return VerificationResult(governor, result.actual?.governor, result.writeSucceeded, result.verified, result.error)
    }

    fun clamp(device: Device, frequency: Long, io: Io = SystemIo): VerificationResult<Long> {
        val result = apply(device, Request(frequency, frequency), io)
        return VerificationResult(frequency, result.actual?.maxFreq, result.writeSucceeded, result.verified, result.error)
    }

    fun currentExactLockFrequency(device: Device, io: Io = SystemIo): Long? {
        val live = selection(io).device?.takeIf { it.path == device.path } ?: return null
        val path = live.mtkFixedIndexPath ?: return null
        val index = io.read(path)?.let(MtkGpuOppTable::parseIndex)?.takeIf { it != "-1" } ?: return null
        return live.mtkOppIndexByFrequency.entries.firstOrNull { MtkGpuOppTable.parseIndex(it.value) == index }?.key
    }

    fun releaseExactLock(io: Io = SystemIo): Boolean {
        val device = selection(io).device ?: return true
        val path = device.mtkFixedIndexPath ?: return true
        if (!device.mtkLockWritable) return false
        val baselineIndex = io.read(path)?.let(MtkGpuOppTable::parseIndex) ?: return false
        if (baselineIndex == "-1") return true
        val wrote = io.write(path, "-1")
        val verified = wrote && io.read(path)?.let(MtkGpuOppTable::parseIndex) == "-1"
        if (verified) return true
        io.write(path, baselineIndex)
        return false
    }

    fun frequencyMHz(device: Device, raw: Long?): Long? {
        if (raw == null || raw <= 0L || !device.unitTrusted) return null
        val hz = raw * device.frequencyUnit.hzMultiplier
        return (hz / 1_000_000L).takeIf { it in 1L..10_000L }
    }

    fun inferFrequencyUnit(values: List<Long>): FrequencyUnit {
        val positive = values.filter { it > 0L }
        if (positive.isEmpty()) return FrequencyUnit.AMBIGUOUS
        val units = positive.map { raw ->
            when {
                raw >= 10_000_000L -> FrequencyUnit.HZ
                raw >= 10_000L -> FrequencyUnit.KHZ
                else -> FrequencyUnit.MHZ
            }
        }.distinct()
        if (units.size != 1) return FrequencyUnit.AMBIGUOUS
        val unit = units.single()
        val mhz = positive.map { (it * unit.hzMultiplier) / 1_000_000L }
        return unit.takeIf { mhz.all { value -> value in 1L..10_000L } } ?: FrequencyUnit.AMBIGUOUS
    }

    fun rangeWriteOrder(currentMax: Long?, targetMin: Long, targetMax: Long): List<String> =
        if (currentMax != null && targetMin > currentMax) listOf("max", "min") else listOf("min", "max")

    private fun discoverCandidates(io: Io): List<Candidate> = scan(io).candidates

    /**
     * حصيلة عدّة واحدة: المرشّحون، و**كم اسمًا رصد العدّ** قبل التصنيف.
     *
     * ولماذا العدد جزء من الحصيلة: «لا اسم يُقرأ» و«أسماء تُقرأ وليس بينها مزوّد» حالتان
     * بإجراءين مختلفين — الأولى نقص قياس يفتحه الجذر، والثانية دليل غياب. وعدّ الذاكرة/التخزين
     * يفصلهما منذ اليوم ([MemoryUfsHardwareBackend.REASON_ROOT_UNREADABLE])، وهذا نظيره هنا.
     */
    private data class Scan(val candidates: List<Candidate>, val listed: Int)

    private fun scan(io: Io): Scan {
        val names = io.listDirectories(ROOT)
        val candidates = names.mapNotNull { readCandidate(it, io) }.toMutableList()
        // ولا يُضاف سطح بديل حين يوجد مزوّد ساعة مقروء: الهدف سدّ نقص القراءة لا مضاعفة المالك.
        val hasReadableClock = candidates.any {
            it.device.role == NodeRole.GPU_CLOCK && it.device.unitTrusted && it.device.frequencies.isNotEmpty()
        }
        if (!hasReadableClock) qualcommClockFallback(io)?.let(candidates::add)
        return Scan(
            candidates = candidates.sortedWith(
                compareByDescending<Candidate> { it.score }.thenBy { it.device.name },
            ),
            listed = names.size,
        )
    }

    /**
     * يقرأ ساعة GPU من سطح كوالكوم البديل ([QUALCOMM_CLOCK_SURFACES]) — أو `null` إن لم يُقرأ شيء.
     *
     * والأعمدة الستّة في **نداء واحد** كما في [readCandidate]؛ والوحدة تُستنتج من القيم نفسها
     * ([inferFrequencyUnit]) ولا تُفترض: سطح بوحدة غامضة لا يُقبل مزوّدًا.
     *
     * ودلالة العائد كما في `readCandidate`: قيمة مقصوصة غير فارغة أو `null` — لا صفر ولا قيمة مخترعة.
     */
    private fun qualcommClockFallback(io: Io, onlyBase: String? = null): Candidate? {
        for (surface in QUALCOMM_CLOCK_SURFACES) {
            if (onlyBase != null && surface.base != onlyBase) continue
            if (!io.exists(surface.base)) continue
            val paths = listOf(
                "${surface.base}/${surface.minKey}",
                "${surface.base}/${surface.maxKey}",
                "${surface.base}/${surface.currentKey}",
                "${surface.base}/${surface.ladderKey}",
                "${surface.base}/governor",
                "${surface.base}/available_governors",
            )
            val reads = io.readMany(paths)
            val frequencies = parseLongList(reads.getOrNull(3))
            val min = reads.getOrNull(0)?.trim()?.toLongOrNull()
            val max = reads.getOrNull(1)?.trim()?.toLongOrNull()
            val current = reads.getOrNull(2)?.trim()?.toLongOrNull()
            val governor = reads.getOrNull(4)?.trim()?.takeIf(String::isNotBlank)
            val governors = reads.getOrNull(5).orEmpty()
                .split(Regex("\\s+")).filter(String::isNotBlank).distinct()
            if (frequencies.isEmpty() && min == null && max == null && current == null) continue
            val unit = inferFrequencyUnit(frequencies + listOfNotNull(min, max, current))
            if (unit == FrequencyUnit.AMBIGUOUS) continue
            val evidence = buildList {
                add("qualcomm-gpu-identity")
                add("role-${NodeRole.GPU_CLOCK.token}")
                add(surface.evidence)
                add("clocks-read-from-kgsl-mirror")
                if (min != null && max != null) add("live-range")
                if (governor != null) add("live-governor")
                if (frequencies.isNotEmpty()) add("devfreq-opp-table")
                if (current != null) add("current-frequency")
                add("consistent-${unit.name.lowercase()}-unit")
            }
            val device = Device(
                path = surface.base,
                name = "kgsl-3d0",
                governor = governor,
                governors = governors,
                minFreq = min,
                maxFreq = max,
                currentFreq = current,
                frequencies = frequencies,
                family = Family.QUALCOMM,
                loadPercent = readLoad(surface.base, Family.QUALCOMM, io),
                thermalC = readGpuThermal(io),
                frequencyUnit = unit,
                minWritable = io.writable("${surface.base}/${surface.minKey}") &&
                    surface.minKey == "min_freq",
                maxWritable = io.writable("${surface.base}/${surface.maxKey}") &&
                    surface.maxKey == "max_freq",
                governorWritable = governor != null && io.writable("${surface.base}/governor"),
                evidence = evidence,
            )
            return Candidate(device, candidateScore(100, frequencies, current, null))
        }
        return null
    }

    /**
     * البوابة الأولى: هل العقدة من **عائلة الرسوميات** أصلًا؟ وما ليس منها لا يُرشَّح ولا يُسمّى
     * مزوّدًا في لقطة.
     *
     * والحدّ هو الحدّ المقيس سابقًا حرفيًّا، وإنما سُمّي: `kgsl`/`adreno`/`mali` يُقرآن في
     * التوقيع الكامل (اسم المجلّد + `device_name` + المسار، كما كانا في `identityScore`)،
     * و`gpu`/`3d` في اسم المجلّد وحده (كما كانا أيضًا). ولماذا لم يُوسَّع: توسيعُ الحدّ يُدخل
     * عقدًا لا ساعة فيها (فتُخدَّر التشخيص بقائمة مزوّدين لا مزوّد بينها)، والمقيسُ كافٍ.
     */
    private fun isGraphicsNode(name: String, descriptor: String): Boolean =
        descriptor.contains("kgsl") || descriptor.contains("adreno") || descriptor.contains("mali") ||
            name.contains("gpu", true) || name.contains("3d", true)

    /**
     * دور عقدة من نصّها الكامل (الاسم المعلن + اسم المجلّد + المسار).
     *
     * والترتيب مقصود: **توقيع الساعة أولًا** — فالعقدة التي تحمل `kgsl-3d` هي جهاز الـGPU نفسه
     * ولو حمل اسمها رمز عرض نطاق. ثم رمز العرض، ثم العائلة بلا توقيع (درجة أضعف، ولا تُسقط).
     */
    private fun nodeRoleOf(descriptor: String): NodeRole {
        val lower = descriptor.lowercase()
        if (GPU_CLOCK_SIGNATURES.any(lower::contains)) return NodeRole.GPU_CLOCK
        val tokens = lower.split(*ROLE_SEPARATORS)
        if (tokens.any { it in BANDWIDTH_ROLE_TOKENS }) return NodeRole.BANDWIDTH_MONITOR
        return NodeRole.GRAPHICS_UNKNOWN
    }

    private fun familyOf(name: String, deviceName: String, path: String): Family {
        val descriptor = "$name $deviceName $path".lowercase()
        return when {
            descriptor.contains("kgsl") || descriptor.contains("adreno") -> Family.QUALCOMM
            descriptor.contains("mali") -> Family.MALI
            else -> Family.UNKNOWN
        }
    }

    /**
     * درجة الدليل على أنّ هذه العقدة **مزوّد ساعة GPU** — ومعنى الدرجات عيّنةٌ واحدة:
     * توقيع الساعة (١٠٠) أقوى من رمز عائلة عامّ، والعامّ أقوى من رمز أضعف منه.
     *
     * والمقارنة بين مرشّحين لا تقع إلا **داخل** [NodeRole.GPU_CLOCK] أو داخل غير المراقبات:
     * المراقب لا يُرشَّح أصلًا ([select])، ودرجته هنا لا تُقرأ للاختيار — تُكتب في اللقطة
     * والتشخيص فقط، ولهذا كانت منخفضةً لا صفرية (الصفر يعني «ليست مرشّحة فلا تُرى»).
     */
    private fun graphicsIdentityScore(role: NodeRole, descriptor: String): Int = when {
        role == NodeRole.BANDWIDTH_MONITOR -> 40
        descriptor.contains("kgsl-3d") || descriptor.contains("adreno") ||
            descriptor.contains("mali") || descriptor.contains("gpufreq") -> 100
        descriptor.contains("gpu") -> 80
        descriptor.contains("3d") -> 70
        else -> 60
    }

    /** التوقيع الكامل للعقدة: لا يُحكم على دور قبل أن يُرى الاسم والمسار معًا. */
    private fun descriptorOf(name: String, deviceName: String, path: String): String =
        "$name $deviceName $path".lowercase()

    private fun candidateScore(identityScore: Int, frequencies: List<Long>, current: Long?, mtkLockPath: String?): Int =
        identityScore + (if (frequencies.isNotEmpty()) 20 else 0) + (if (current != null) 5 else 0) +
            (if (mtkLockPath != null) 10 else 0)

    private fun readCandidate(name: String, io: Io): Candidate? {
        val path = "$ROOT/$name"
        val deviceName = io.read("$path/device_name").orEmpty()
        val descriptor = descriptorOf(name, deviceName, path)
        // البوابة الأولى: **عائلة الرسوميات** — ما ليس منها لا يُرشَّح أصلًا (ولا يُعرض باسم GPU
        // في لقطةٍ تُقرأ كأنها قائمة مزوّدين). والدور يُحكم بعدها، فهو تفصيلٌ داخل العائلة.
        if (!isGraphicsNode(name, descriptor)) return null
        val role = nodeRoleOf(descriptor)
        val family = familyOf(name, deviceName, path)
        val identityScore = graphicsIdentityScore(role, descriptor)

        // الستّة الباقية **في نداء واحد** (§4: «اجمع البيانات عند الإمكان»): كانت ستّ رحلات
        // قراءة لكل مرشّح — وعشرة مرشّحين على جهاز مزدحم يعني ستّين رحلة في نبضة اكتشاف واحدة
        // تعمل واللعبة تعمل. والدلالة لكل عقدة هي هي: المقصوص غير الفارغ أو `null`، والترتيب
        // محفوظ بالمحاذاة، والعقدة التي لم تُجب يكمّلها القارئ الدفعيّ في طبقة `RootFileAccess`.
        val clocks = io.readMany(
            listOf(
                "$path/available_frequencies",
                "$path/min_freq",
                "$path/max_freq",
                "$path/cur_freq",
                "$path/governor",
                "$path/available_governors",
            )
        )
        val rawFrequencies = parseLongList(clocks.getOrNull(0))
        // Trimmed here, not assumed trimmed by the reader: see the [ReadIo] contract.
        val rawMin = clocks.getOrNull(1)?.trim()?.toLongOrNull()
        val rawMax = clocks.getOrNull(2)?.trim()?.toLongOrNull()
        val rawCurrent = clocks.getOrNull(3)?.trim()?.toLongOrNull()
        val governor = clocks.getOrNull(4)?.trim()
        val governors = clocks.getOrNull(5).orEmpty()
            .split(Regex("\\s+")).filter(String::isNotBlank).distinct()
        // عقدة GPU معروفة الاسم بلا ساعات مقروءة تُحفَظ هيكلًا لا تُسقَط.
        //
        // والدافع مقيس على سنابدراجون (SM8650/Adreno في حزمة ٢٠٢٦-١٠-١٤): قراءة `governor`
        // و`available_frequencies` و`cur_freq` قد تُحجب عن عملية الاكتشاف (SELinux يمنع عبور
        // `devfreq` على uid التطبيق — مرفوضات مقيسة في `dmesg`) بينما تُقرأ من الجذر لاحقًا
        // (`refresh` يعيد قراءة المسار نفسه)، والإسقاط كان يجعل الجهاز كله «not-detected»
        // فلا سقف ولا قياس ولا ملفّ. والكتابة تبقى محروسة: بلا سلّم ولا حدّين يبقى
        // `devfreqCeilingWritable` كاذبًا، والملاءِمات تتخطّى غير القابل، والمحكّم يتحقّق من
        // كل كتابة — فأسوأ ما يحدث محاولة مُتحقَّق منها وسببها مكتوب، لا صمت ولا تخمين.
        // والدليل يسمّي النقص (`clocks-unreadable-at-discovery`) فيُقرأ في البصمة والحزمة.
        val clocksUnreadable = governor == null && rawFrequencies.isEmpty() && rawCurrent == null

        val mtkDiscovery = if (family == Family.MALI) MtkGpuOppTable.discover(io) else MtkGpuOppTable.Discovery(emptySet(), emptyMap())
        val mtkMap = mtkDiscovery.indexed
        // An unindexed OPP proves capability but cannot safely be written through a
        // fixed-index node. Never invent an index for it.
        val mtkLockPath = if (
            mtkMap.isNotEmpty() &&
            // A fixed-index writer is safe only when every discovered capability
            // has a real kernel index. If another OPP surface exposes an unindexed
            // higher capability, prefer a generic devfreq range (when writable)
            // instead of silently pinning the GPU to the smaller indexed table.
            mtkDiscovery.frequencies.all { it in mtkMap }
        ) MtkGpuOppTable.LOCK_PATHS.firstOrNull(io::exists) else null
        val genericUnit = inferFrequencyUnit(rawFrequencies + listOfNotNull(rawMin, rawMax, rawCurrent))
        val frequencies: List<Long>
        val min: Long?
        val max: Long?
        val current: Long?
        val unit: FrequencyUnit
        val effectiveMtkFrequency = if (mtkLockPath != null) {
            val activeIndex = io.read(mtkLockPath)?.let(MtkGpuOppTable::parseIndex)?.takeIf { it != "-1" }
            activeIndex?.let { index -> mtkMap.entries.firstOrNull { MtkGpuOppTable.parseIndex(it.value) == index }?.key }
        } else null
        // ودمج سلّم `devfreq` يُشترط فيه اتفاق الوحدة: جدول MediaTek بالهرتز معلوم، فلو أضفنا
        // إليه سلّمًا بلغة أخرى لَخُلطت وحدتان في سلّم واحد.
        val mtkScaleIsHertz = genericUnit == FrequencyUnit.HZ || rawFrequencies.isEmpty()
        if (mtkLockPath != null) {
            // وأعلى درجات الجهاز = ما تُعلنه **العقدتان** معًا: كتالوج MediaTek يحمل الفهرس، وسلّم
            // `devfreq` هو ما تُكتب عليه الحدود — فرق يُسقط درجاتٍ قابلة للكتابة لو أُخذ أحدهما.
            frequencies = (mtkDiscovery.frequencies + mtkMap.keys +
                if (mtkScaleIsHertz) rawFrequencies else emptyList())
                .filter { it > 0L }.distinct().sorted()
            // وسقف المصنّع الحيّ (`min_freq`/`max_freq`) **لا يُسقَط** حين يوجد مسار تثبيت OPP.
            //
            // كان الفرع يأخذ الحدّين من القفل وحده (`effectiveMtkFrequency ?: أطراف الجدول`)، فسقف
            // الحرارة/الطاقة الحيّ — وهو ٧٥٤ على الجهاز المقيس بينما أعلى درجة معلنة ١٣٠٠ — لا يُقرأ
            // في التطبيق **أبدًا**: `live_max` في سطر الفحص كان يقول ١٣٠٠ والجهاز يعمل على ٧٥٤.
            // والإسقاط لم يكن حيادًا: هو ما جعل الحاكم يقرأ «القدرة» فيحكم «مُلبّى» بلا كتابة.
            //
            // والمقروء يُصدَّق على ذاته: إن كان القفل قائمًا فالتحديد هو ما يقصّ (فالحدّان = تردد القفل)،
            // وإن لم يكن قائمًا فالحدّ الحيّ هو ما تقوله العقدتان، وغيابهما وحده يُعيد أطراف الجدول.
            // وتُحوَّل قيمة العقدة بوحدة معلومة قبل أن تُقبل كحدّ (`normalizeToAdvertisedHz` كما في
            // `current`): قيمة ليست من درجات معلنة لا تُفسَّر Hz بالحدس.
            val devfreqMin = if (mtkScaleIsHertz) normalizeToAdvertisedHz(rawMin, frequencies) else null
            val devfreqMax = if (mtkScaleIsHertz) normalizeToAdvertisedHz(rawMax, frequencies) else null
            min = effectiveMtkFrequency ?: devfreqMin ?: frequencies.firstOrNull()
            max = effectiveMtkFrequency ?: devfreqMax ?: frequencies.lastOrNull()
            current = effectiveMtkFrequency ?: normalizeToAdvertisedHz(rawCurrent, frequencies)
            unit = FrequencyUnit.HZ
        } else {
            // لغة أرقام هذا الكائن هي **لغة العقدة نفسها**، لا Hz.
            //
            // `min_freq`/`max_freq`/`cur_freq` و`available_frequencies` تُحفظ كما قرأها السائق،
            // و`frequencyUnit` يسمّي تلك اللغة: `frequencyMHz()` هي التي تحوّل للعرض،
            // و`writeRange()` تكتب القيمة كما هي. وتحويلها هنا إلى Hz كان: (١) يضاعف التحويل في
            // كل عرض (MHZ × MHZ)، (٢) ويسجّل قيمة غير متطابقة لما يقرأه `encodeLive` فيُصنَّف
            // تطبيقٌ ناجح فاشلًا، (٣) ويكتب Hz في عقدة تتكلّم kHz — أي يفسد `min_freq` على كل جهاز
            // لا يستعمل Hz. وجدول MediaTek وحده معلوم الوحدة (Hz) فيُدمج **فقط** حين يكون الجدول
            // العام بنفس اللغة أو غير موجود أصلًا؛ وإلا لَخُلطت وحدتان في سلّم واحد.
            val mtkCapability = if (genericUnit == FrequencyUnit.HZ || rawFrequencies.isEmpty()) {
                mtkDiscovery.frequencies
            } else {
                emptySet()
            }
            frequencies = (rawFrequencies + mtkCapability).filter { it > 0L }.distinct().sorted()
            // حدّ أدنى/أعلى غير مقروء **لا يُخترع** من السلّم. الاختراع كان يجعل خط الأساس يبدو
            // مقروءًا، فيمرّ المدى من [validate] وتنجح كتابة لا يمكن التراجع عنها إلى ما كان عليه
            // فعلًا (وقد كُشف ذلك في `GpuControlModelTest.unreadableBaselineBlocksRangeMutation`).
            // والغياب هنا هو بالضبط ما يجعل `rangeWritable` كاذبًا و[validate] تُجيب
            // `range-read-only-or-unproven`: الفشل الصريح خير من نجاح لا خط أساس له.
            min = rawMin
            max = rawMax
            current = rawCurrent
            unit = if (mtkCapability.isNotEmpty()) FrequencyUnit.HZ else genericUnit
        }
        val evidence = buildList {
            add(if (family == Family.QUALCOMM) "qualcomm-gpu-identity" else "gpu-device-identity")
            // ودور العقدة دليل يُكتب لا يُستنتج من الاسم عند القارئ: كل من يقرأ البصمة أو السجل
            // (بما فيه الجلسة التالية) يرى أيّ عقدة وُصفت بأيّ دور، وأيّها استُبعد ولِمَ.
            add("role-${role.token}")
            if (role == NodeRole.BANDWIDTH_MONITOR) add("not-a-gpu-clock-owner")
            if (clocksUnreadable) add("clocks-unreadable-at-discovery")
            if (frequencies.isNotEmpty()) add(if (mtkLockPath != null) "mtk-signed-opp-table" else "devfreq-opp-table")
            if (current != null) add("current-frequency")
            if (unit != FrequencyUnit.AMBIGUOUS) add("consistent-${unit.name.lowercase()}-unit")
        }
        val device = Device(
            path = path,
            name = deviceName.ifBlank { name },
            governor = governor,
            governors = governors,
            minFreq = min,
            maxFreq = max,
            currentFreq = current,
            frequencies = frequencies,
            family = family,
            loadPercent = readLoad(path, family, io),
            thermalC = readGpuThermal(io),
            frequencyUnit = unit,
            minWritable = io.writable("$path/min_freq"),
            maxWritable = io.writable("$path/max_freq"),
            governorWritable = io.writable("$path/governor"),
            mtkFixedIndexPath = mtkLockPath,
            mtkLockWritable = mtkLockPath?.let(io::writable) == true,
            mtkOppIndexByFrequency = mtkMap,
            evidence = evidence,
        )
        val score = candidateScore(identityScore, frequencies, current, mtkLockPath)
        return Candidate(device, score)
    }

    private fun parseLongList(raw: String?): List<Long> = raw.orEmpty().split(Regex("\\s+"))
        .mapNotNull(String::toLongOrNull).filter { it > 0L }.distinct().sorted()

    private fun normalizeToAdvertisedHz(raw: Long?, advertisedHz: List<Long>): Long? {
        if (raw == null || raw <= 0L) return null
        return listOf(raw, raw * 1_000L, raw * 1_000_000L).distinct().singleOrNull { it in advertisedHz }
    }

    private fun readLoad(path: String, family: Family, io: Io): Int? {
        val paths = buildList {
            add("$path/load")
            add("$path/device/load")
            if (family == Family.QUALCOMM) add("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage")
            if (family == Family.MALI) add("/sys/module/ged/parameters/gpu_idle")
        }
        // الدفعة الواحدة تحلّ محلّ السلسلة: كانت العقدة تُسأل، فإن لم تُجب تُسأل التالية — والآن
        // تُقرأ كلّها في نداء واحد ثم **يُختار أوّل قيمة تُحلَّل** بالترتيب نفسه. والعقد التي لا
        // يقرؤها القارئ تبقى `null` فلا تُختلق قيمة، ومنطق `gpu_idle` المقلوب محفوظ حرفيًّا.
        val reads = io.readMany(paths)
        return paths.indices.asSequence().mapNotNull { index ->
            val raw = reads.getOrNull(index) ?: return@mapNotNull null
            if (paths[index].endsWith("gpu_idle")) parseLoad(raw)?.let { 100 - it } else parseLoad(raw)
        }.firstOrNull()
    }

    internal fun parseLoad(raw: String): Int? {
        val numbers = Regex("\\d+").findAll(raw).mapNotNull { it.value.toLongOrNull() }.toList()
        if (numbers.isEmpty()) return null
        val percent = if (numbers.size >= 2 && numbers[1] > 0L && numbers[0] > 100L) {
            ((numbers[0] * 100L) / numbers[1]).toInt()
        } else numbers.first().toInt()
        return percent.takeIf { it in 0..100 }
    }

    /**
     * حرارة GPU من مناطق `/sys/class/thermal` — **بنداءين دفعيّين لا نداءً لكل منطقة** (§4).
     *
     * كانت كل `thermal_zone*` تُسأل عن `type` ثم عن `temp` كلٌّ على حِدة: على جهاز بـ٦٦ منطقة
     * يعني مسحًا واحدًا **نحو ٧٠ رحلة قراءة** في نبضة اكتشاف تعمل واللعبة تعمل. والآن: سرد واحد،
     * فدفعة أنواع واحدة، فدفعة درجات للمطابقات وحدها (وغير المطابقة لا تُقرأ درجتها كما كان).
     * والنتيجة الرقميّة هي هي بالحرف: الترتيب محفوظ بالمحاذاة، والغائب يبقى غائبًا فلا تُختلق قيمة.
     */
    private fun readGpuThermal(io: Io): Int? {
        val zones = io.listDirectories(THERMAL_ROOT).filter { it.startsWith("thermal_zone") }
        if (zones.isEmpty()) return null
        val types = io.readMany(zones.map { "$THERMAL_ROOT/$it/type" })
        val matched = zones.indices.filter { index ->
            val type = types.getOrNull(index).orEmpty().lowercase()
            listOf("gpu", "gpuss", "mali", "tsgpu").any(type::contains)
        }
        if (matched.isEmpty()) return null
        val temps = io.readMany(matched.map { "$THERMAL_ROOT/${zones[it]}/temp" })
        var best: Int? = null
        matched.forEachIndexed { position, _ ->
            val raw = temps.getOrNull(position)?.toLongOrNull() ?: return@forEachIndexed
            val c = when {
                kotlin.math.abs(raw) >= 10_000L -> raw / 1_000L
                kotlin.math.abs(raw) > 200L -> raw / 10L
                else -> raw
            }.toInt()
            if (c in -40..200) best = maxOf(best ?: c, c)
        }
        return best
    }

    private fun writeRange(device: Device, min: Long, max: Long, io: Io): Boolean {
        if (min !in device.frequencies || max !in device.frequencies || min > max) return false
        return writeRangeRaw(device, min, max, io)
    }

    private fun writeRangeRaw(device: Device, min: Long, max: Long, io: Io): Boolean {
        for (field in rangeWriteOrder(device.maxFreq, min, max)) {
            val value = if (field == "min") min else max
            if (!io.write("${device.path}/${field}_freq", value.toString())) return false
        }
        return true
    }

    private fun matches(actual: Device?, request: Request): Boolean = actual != null &&
        (request.minFreq == null || actual.minFreq == request.minFreq) &&
        (request.maxFreq == null || actual.maxFreq == request.maxFreq) &&
        (request.governor == null || actual.governor.equals(request.governor, true))
}
