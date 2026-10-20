/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/**
 * مالك **ترددات الذاكرة وUFS** — المالك القياسيّ الوحيد لعُقَد `devfreq` التي ليست GPU.
 *
 * لماذا هذا الملف
 * ---------------
 * مشروع مرجعيّ مغلق (`Rodin Essential 1.18.5`) يعرض تحكّم «Memory DVFS / UFS» بكتابة
 * `min_freq`/`max_freq` على مسارين **مكتوبين بالاسم** في سياسته:
 *
 * ```text
 * /devices/platform/soc/1c00f000.dvfsrc/mtk-dvfsrc-devfreq/devfreq/mtk-dvfsrc-devfreq/{min,max}_freq
 * /devices/platform/soc/112b0000.ufshci/devfreq/112b0000.ufshci/{min,max}_freq
 * ```
 *
 * ومساراتٌ كهذه تعمل على الجهاز الذي كُتبت عليه وحدها: رقم `1c00f000` و`112b0000` هما عنوانان
 * على ناقل ذلك الـSoC بعينه، وعلى كيرنل آخر هما عقدتان لا وجود لهما. والقاعدة في هذا المستودع
 * أن **الأسماء لا تُنقل، والقياس يُنقل**: فنحن نُعدّد `/sys/class/devfreq` ونسأل كل جهاز عن
 * اسمه، ثم **نُصنّفه** من رموز معروفة في اسمه — فيعمل التحكّم على MediaTek وQualcomm وExynos
 * وبأي كيرنل عامّ يُصدّر عقدة قابلة للكتابة، بلا جدول أجهزة.
 *
 * والقواعد التي لا تُخترق
 * ----------------------
 * 1. **مالك واحد للعقدة الواحدة.** كل جهاز يُصنَّف GPU أو محرّكًا رسوميًّا يُترك لـ
 *    [GpuHardwareBackend] ولا يُدخل هنا. عقدتان تتنازعان `min_freq` واحدًا تعني كتابتين
 *    تنجرف إحداهما عن الأخرى — وهي القاعدة نفسها التي فُرضت على المفاتيح القياسية.
 * 2. **ما لم تُثبت وحدته لا يُكتب.** قيمة `min_freq` بلغة العقدة: Hz على النواة العامة،
 *    kHz أو MHz على كيرنلات مصنّعة. والوحدة تُستنتج من **سلّم العقدة نفسها** ومن حدودها
 *    ([GpuHardwareBackend.inferFrequencyUnit] — قاعدة واحدة يعرّفها موضع واحد، فحساب قيمة
 *    خام لا يعرف إن كانت العقدة رسومية)، وإن لم يتّفق الجميع على وحدة واحدة فالجهاز
 *    **للقراءة فقط** — كتابة Hz في عقدة تتكلّم kHz هي بالضبط العطب الذي فسد به `min_freq`
 *    على كل جهاز لا يستعمل Hz (مقيس في [GpuHardwareBackend] نفسه).
 * 3. **لا قيمة مُخترعة.** كل رقم نكتبه جاء من سلّم أثبتته النواة. سلّم من قيمة واحدة ليس
 *    مدى، والطلب يُقصّ إلى درجات معلنة لا يُقرَّب إلى ما ليس منها.
 * 4. **الترتيب عند الكتابة ليس تفصيلًا.** رفع الأرضية فوق السقف الحيّ يرفضه السائق، فيُكتب
 *    السقف أوّلًا حين يلزم (`min > current max`) — وهي القاعدة نفسها في مالك GPU.
 * 5. **لا كتابة بلا استرجاع.** خط الأساس يُحفظ قبل كل كتابة، ويُعاد عند الانحراف أو عند
 *    فشل التحقق، ثم يُقاس الاسترجاع بقراءة (`rollbackVerified`) لا يُعلن.
 * 6. **السبب المُعلَن هو السبب المقيس.** ثلاثة أحكام لا تُخلط: «قُرئ فلم يكن» · «رُصد ولم
 *    يُقرأ» · «لم يكن الصنف أصلًا». والخلط بينها ليس تفصيل عرض: مشروع مرجعيّ خسر قسم بطارية
 *    ظهر فارغًا لأن **عبور مجلّد أب** كان محجوبًا، فقُرئ العطب «ميزة غائبة» وبحث صاحبه في غير
 *    موضعه حتى نُشرت `dir search` الصحيحة. فما لم يُقرأ يُعلَن نقصَ قياس ([Discovery.unreadable]
 *    و[UNREADABLE_GAP_CODE] و[REASON_ROOT_ABSENT]), وما لم يوجد بقياسٍ يُعلَن غيابًا، ولا
 *    يُنسب إلى أحدهما فعلُ الآخر.
 *
 * وما لا يفعله هذا الملف
 * ----------------------
 * لا يقرّر **متى** يُكتب: لا حلقة، ولا سياسة، ولا حرارة. الكتابة تمرّ من [SystemIo] وحدها،
 * والمستدعي الإنتاجي هو الـViewModel عبر الـarbiter ([HardwareControlArbiter])، وأطلس يختار
 * الطريقة والمسار. ولا يستورد هذا الملف شيئًا من `android.*` — فيُقاس كاملًا باختبار JVM.
 */
object MemoryUfsHardwareBackend {

    private const val ROOT = "/sys/class/devfreq"

    /** فاصل الصيغة المُرمَّزة: `<min>:<max>|<governor>` — يقرؤه `HardwareVerification` أيضًا. */
    private const val FIELD_SEPARATOR = "|"
    private const val UNTOUCHED = "-"

    /** نوع العقدة كما تحكمه الأدلّة: نطاق تردّد ذاكرة، أو نطاق تردّد تخزين. */
    enum class Kind { MEMORY, STORAGE }

    /** عائلة السائق — للتشخيص والعرض، **وليست شرطًا لأي كتابة**. */
    enum class Family { MEDIATEK, QUALCOMM, EXYNOS, GENERIC }

    /**
     * الوصول المقيس لجهاز واحد. والقيمة `READ_ONLY` ليست فشلًا: هي «أقرأ ولا أكتب بثقة» —
     * وحدة غير مُثبتة، أو سلّم غير معلن، أو عقدة مدى غير قابلة للكتابة.
     */
    enum class Level { READY, READ_ONLY }

    /** نصف القراءة — بلا `write`: مُستهلكُ الاكتشاف لا يستطيع حتى أن يطلب الكتابة. */
    interface ReadIo {
        fun exists(path: String): Boolean
        fun read(path: String): String?
        fun listDirectories(path: String): List<String>
    }

    interface Io : ReadIo {
        fun writable(path: String): Boolean
        fun write(path: String, value: String): Boolean
    }

    object SystemIo : Io {
        override fun exists(path: String) = RootFileAccess.exists(path)
        override fun read(path: String) = RootFileAccess.read(path)
        override fun listDirectories(path: String) = RootFileAccess.listDirectories(path)
        override fun writable(path: String) = RootFileAccess.writable(path)
        override fun write(path: String, value: String) = RootFileAccess.write(path, value)
    }

    /**
     * جهاز تردّد واحد كما قيس الآن.
     *
     * والأرقام بلغة العقدة نفسها (`minFreq`/`maxFreq`/`currentFreq`/`frequencies` كما قرأها
     * السائق) و`frequencyUnit` يسمّي تلك اللغة — والتحويل إلى MHz للعرض مرّة واحدة
     * ([megahertz])، فلا تحويلان يختلفان.
     */
    data class Device(
        val path: String,
        val name: String,
        val kind: Kind,
        val family: Family,
        val governor: String?,
        val governors: List<String>,
        val minFreq: Long?,
        val maxFreq: Long?,
        val currentFreq: Long?,
        val frequencies: List<Long>,
        val frequencyUnit: GpuHardwareBackend.FrequencyUnit,
        val minWritable: Boolean = false,
        val maxWritable: Boolean = false,
        val governorWritable: Boolean = false,
        /**
         * صنفه من **ملفّ المستخدم** لا من قواعدنا. ويُعرض في الواجهة: مصدرُ تصنيفٍ مُخفى يجعل
         * المستخدم ينسب للمحرّك معرفةً أتى بها هو — وهو عكس ما تُظهره هذه الميزة.
         */
        val userTaught: Boolean = false,
        val evidence: List<String> = emptyList(),
    ) {
        val unitTrusted: Boolean get() = frequencyUnit != GpuHardwareBackend.FrequencyUnit.AMBIGUOUS

        /** سلّم صالح للكتابة: درجتان معلنتان على الأقل، وكلّهن موجب. */
        val ladder: List<Long> get() = frequencies.filter { it > 0L }.distinct().sorted()

        /**
         * هل تقبل عقدتا المدى كتابة سقف/أرضية؟ الشروط كلها **مقيسة**: وجود العقدتين،
         * وقابلية كتابتهما، وسلّم معلن، وخط أساس مقروء، ووحدة مُثبتة.
         */
        val rangeWritable: Boolean
            get() = minWritable && maxWritable && ladder.size >= 2 &&
                minFreq != null && maxFreq != null && unitTrusted

        /**
         * حالة الكتابة كما تعنيها **هذه القدرة**: المدى (`min_freq`/`max_freq`).
         *
         * والحاكم قابل للكتابة لكنه **لا يرفع المستوى**: وعدُ «قابل للتحكم» هنا يعني «يُثبَّت مدى
         * تردّد ويُقرأ مرتجعة»، وحاكمٌ وحده لا يعطي ذلك. فجهاز بوحدة غير مُثبتة أو بسلّم لا يصلح
         * مدىً يبقى [READ_ONLY] وإن كان حاكِمه قابلًا للكتابة — وهو نصّ [Level] نفسه: «أقرأ ولا
         * أكتب بثقة». والفرق ليس تجميليًّا: `governorWritable` كان يجعل جهازًا **لا يقبل قيمة واحدة**
         * يُعرَض قابلاً للتحكم (قياس `MemoryUfsHardwareBackendTest`).
         */
        val level: Level get() = if (rangeWritable) Level.READY else Level.READ_ONLY

        val provenMinFreq: Long? get() = ladder.firstOrNull()
        val provenMaxFreq: Long? get() = ladder.lastOrNull()

        /**
         * أعلى قيمة يمكن **طلبها فعلًا** الآن: أعلى درجة معلنة، مقصوصةً بالسقف الحيّ الذي يفرضه
         * السائق (سياسة حرارة/طاقة قد تكون خفّضته). وأخذُ الكتالوج وحده كان يجعل البروفايلات
         * تطلب قيمة مستحيلة، فتُقرأ «فشلًا» وتُعاد الحالة السابقة (القياس في [GpuHardwareBackend]).
         */
        val configurableMaxFreq: Long?
            get() {
                val advertised = provenMaxFreq
                val live = maxFreq?.takeIf { it > 0L }
                return when {
                    advertised == null -> live
                    live == null -> advertised
                    else -> minOf(advertised, live)
                }
            }
    }

    /**
     * حالة كل جهاز مكتشف في تمريرة واحدة، مع رمز سبب ثابت (لا جملة).
     *
     * و[unclassified] و[graphicsOwned] ليستا ترفًا: بلا الأولى يبقى المستخدم لا يعلم أن جهازه
     * فيه عقدة لم نعرف اسمها (فيتعلّم «لا ميزة»)، وبلا الثانية قد يظن أن عقدة الرسوميات «ناقصة»
     * فنعلّمها ذاكرةً — وهي مملوكة أصلًا لاستوديو GPU.
     */
    data class Discovery(
        val devices: List<Device>,
        val scanned: Boolean,
        val reason: String,
        /** عُقَد تردّد رأيناها ولم يعرف المحرّك صنفها — تُعلَّم من الشاشة، ولا تُخمَّن. */
        val unclassified: List<Unclassified> = emptyList(),
        /** أسماء عُقَد رسومية في المجلد نفسه — مملوكة لاستوديو GPU، فلا يتعلّمها هذا المحرّك. */
        val graphicsOwned: List<String> = emptyList(),
        /**
         * عُقَد **رُصدت في التعديد ولم تُقرأ**: لا سطر منها أجاب.
         *
         * وهي ليست «لا ميزة» ولا «مجهولة الصنف»: هي **قياس ناقص**، وسببه الغالب حجبُ عبور
         * مجلّدٍ أب — وهو بعينه ما يجعل قسمًا يبدو غير مدعوم وهو محجوب فقط.
         */
        val unreadable: List<String> = emptyList(),
    ) {
        fun of(kind: Kind): List<Device> = devices.filter { it.kind == kind }
    }

    /** نيّة واحدة على جهاز واحد. الحقول الغائبة لا تُكتب. */
    data class Request(
        val minFreq: Long? = null,
        val maxFreq: Long? = null,
        val governor: String? = null,
    )

    /** خط الأساس المقروء قبل الكتابة — استرجاعٌ له معنى فقط إذا كان مقروءًا فعلًا. */
    data class Baseline(
        val devicePath: String,
        val minFreq: Long?,
        val maxFreq: Long?,
        val governor: String?,
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

    // ── التصنيف ────────────────────────────────────────────────────────────────────────────────
    //
    // والرموز هنا **مفردات مرصودة** من سائقين معلومين: `dvfsrc` و`emi` و`memlat` من كيرنلات
    // MediaTek، و`cpubw`/`llccbw`/`l3c`/`bwmon` من Qualcomm، و`mif`/`int` من Exynos، و`ufshci`
    // من طبقة UFS في AOSP. وليست قائمة أجهزة: الرمز يُطابق **اسم عقدة**، والاسم يُقرأ من
    // الجهاز، فلا يُسقط جهازٌ جديد ولا يُقبل جهاز لا يقول اسمه شيئًا.

    private data class Rule(val kind: Kind, val family: Family, val tokens: Set<String>)

    private val RULES: List<Rule> = listOf(
        // الذاكرة — MediaTek: وحدة DVFS للذاكرة، ومراقبة عنق الزجاجة، وEMI.
        Rule(Kind.MEMORY, Family.MEDIATEK, setOf("dvfsrc")),
        Rule(Kind.MEMORY, Family.MEDIATEK, setOf("emi", "dramc")),
        // الذاكرة — Qualcomm: عرض النطاق (cpubw)، وآخر مستوى مخبأ (llccbw/l3c)، ومراقب (bwmon/devbw).
        Rule(Kind.MEMORY, Family.QUALCOMM, setOf("cpubw", "llccbw", "l3c", "bwmon", "devbw")),
        // و`memlat` **مشترك**، ولا يُنسب إلى أحدهما: MediaTek وQualcomm كلاهما ينشر عقد مراقبة
        // عنق الذاكرة بهذا الاسم (`memlat-cpu0` على كوالكوم، و`mtk-memlat` على ميدياتك)، فنسبُه
        // إلى مصنّع بعينه **تخمينٌ يُعرض كمعرفة**. والتعريف الأصدق: الوظيفة معلومة (ذاكرة) والنسب
        // مجهول — والعائلة وسمٌ تشخيصيّ لا شرطَ كتابة، والذي يُقاس عليه مسارٌ هو السلّم والوحدة
        // والقابلية للكتابة. (وهذا الإصلاح خرج من تشغيل الاختبارات لا من مراجعة نظرية.)
        Rule(Kind.MEMORY, Family.GENERIC, setOf("memlat")),
        // الذاكرة — Exynos: واجهة الذاكرة (mif) والناقل الداخلي (int) وDSU.
        Rule(Kind.MEMORY, Family.EXYNOS, setOf("mif", "dsu")),
        Rule(Kind.MEMORY, Family.EXYNOS, setOf("int")),
        // الذاكرة — عامّ: أسماء المتحكّمات كما تسمّيها النواة نفسها.
        Rule(Kind.MEMORY, Family.GENERIC, setOf("dram", "ddr", "ddrc", "membw", "bus")),
        // التخزين — UFS/eMMC/NVMe وعناقيد SD.
        Rule(Kind.STORAGE, Family.GENERIC, setOf("ufs", "ufshci", "nvme", "mmc", "sdhci", "sdcc", "scsi")),
    )

    /**
     * رموز تُخرج العقدة من هذا المالك: رسوميات. الاسم هنا **مانع** لا مصنّف — فمالك GPU قائم
     * ويُقاس بالمفاتيح القياسية `gpu_frequency:<device>`، ووجود عقدة هنا ثانيةً يعني مقبضًا
     * له كاتبان.
     */
    private val GPU_TOKENS = setOf("mali", "kgsl", "gpufreq", "gpu", "adreno")

    private fun tokensOf(raw: String): Set<String> =
        raw.lowercase().split(*SEPARATORS).filter(String::isNotBlank).toSet()

    private val SEPARATORS = charArrayOf('.', '_', '-', ':', '/', ' ', '@', ',')

    /**
     * هل الاسم لرسوميات؟ — حكمٌ **مانع** يُستعمل في ثلاثة مواضع: التصنيف هنا، ورفض سطر التعليم
     * ([HardwareDeviceOverlay.parse])، ورفض الفحص ([probe]).
     *
     * وموضعه واحد لأن الحكم يجب أن يكون واحدًا: لو اختلف أحد الثلاثة لصار في التطبيق مقبضٌ له
     * مالكَان — وهذا هو العطب نفسه الذي يمنعه المستودع في كل عائلة مفاتيح.
     */
    fun graphicsOwned(raw: String): Boolean = tokensOf(raw).any { it in GPU_TOKENS }

    /**
     * المطابقة **على حدود الرمز** لا في أي موضع: `int` لا تُطابق `interconnect`، و`mif` لا
     * تُطابق `amifoo`. وبلا هذا الحدّ يصير التصنيف تخمينًا ينزلق إلى عقدة أخرى بنصّ عابر.
     */
    private fun classify(raw: String): Pair<Kind, Family>? {
        val tokens = tokensOf(raw)
        if (tokens.isEmpty() || tokens.any { it in GPU_TOKENS }) return null
        RULES.forEach { rule ->
            if (tokens.any { it in rule.tokens }) return rule.kind to rule.family
        }
        return null
    }

    // ── تصنيف المستخدم (ملفّ التعليم) ──────────────────────────────────────────────────────────

    /**
     * تصنيفُ المستخدم النشط في هذه العملية.
     *
     * ولماذا حالة عملية لا وسيطًا في كل نداء: التصنيف **واقعٌ واحد على الجهاز**، والمنادون هنا
     * كثيرون (خريطة القدرة وأطلس والشاشة والمُلاءِم والفحص) — ووسيطٌ يُمرَّر يدويًّا يعني أن
     * مناديا واحدًا يُنسى فيرى المستخدم «الجهاز مُعلَّم» في شاشة و«غير مُعلَّم» في أخرى. وهذا هو
     * التفريق نفسه الذي يمنعه المستودع في تعريف التصنيف («تعريفان للمقبض الواحد»).
     *
     * والافتراضي **فارغ**: بلا ملفّ تعليم لا يتغيّر شيء عن سلوك ما قبل هذه الميزة.
     */
    @Volatile
    private var taught: List<HardwareDeviceOverlay.Entry> = emptyList()

    /** يشغل تصنيف المستخدم (يُنادى عند قراءة الملفّ وبعد كل تعديل عليه). */
    fun installOverlay(entries: List<HardwareDeviceOverlay.Entry>) {
        taught = entries.distinctBy { it.device }
    }

    /** ما هو مُشغَّل الآن — تُعرض في الشاشة كي لا يبقى للمستخدم تصنيفٌ «في ذاكرتنا» بلا أثر مرئيّ. */
    fun currentOverlay(): List<HardwareDeviceOverlay.Entry> = taught

    /**
     * عقدة تردّد رآها المحرّك **ولم يعرف صنفها** — تُعرَض ليُعلَّم اسمها، ولا تُخمَّن ولا تُسقَط.
     *
     * وهي «القائمة الناقصة» في المشروع المرجعيّ (`--labels`) لكن مقلوبةً إلى داخل التطبيق: لا
     * ملفّ يُكتب بيد خبير، بل سطرٌ يعلّمه صاحب الجهاز من الشاشة — بعد **قياس** العقدة ([probe]).
     */
    data class Unclassified(
        val directory: String,
        val declared: String,
        val ladderSize: Int,
        val writable: Boolean,
    ) {
        val label: String get() = declared.ifBlank { directory }
    }

    /** نتيجة فحص جهاز لتعليمه: قياسٌ أُجيز، أو رفضٌ برمزه. */
    sealed interface Probe {
        data class Ok(val device: Device) : Probe
        data class Refused(val reason: String) : Probe
    }

    /** قراءة عقدة واحدة — أربع نتائج لا اثنتان، لأن «لا أعرف» ليست «لا يوجد». */
    /**
     * قراءة عقدة واحدة — **خمس** نتائج لا اثنتين، لأن «لا أعرف» ليست «لا يوجد».
     *
     * والفرق بين [NotAnInterface] و[Unreadable] هو الفرق الذي كلّف المشروعَ المرجعيّ قسمًا
     * ظهر فارغًا: الأول حكمٌ **قيس** (قرأنا داخل المجلّد فلم يحمل واجهة تردّد)، والثاني **نقص
     * قياس** (لم نقرأ شيئًا، فكلّ ما نقوله عن محتواه تخمين — ومنه «لا يوجد»).
     */
    private sealed interface Reading {
        data class Classified(val device: Device) : Reading
        data class Unknown(val entry: Unclassified) : Reading
        data class Owned(val directory: String) : Reading
        /** لم تُقرأ: لا سطر منها أجاب — وليست «ليست واجهة تردّد». */
        data class Unreadable(val directory: String) : Reading
        data object NotAnInterface : Reading
    }

    // ── الاكتشاف ───────────────────────────────────────────────────────────────────────────────

    /**
     * تمريرة واحدة على `/sys/class/devfreq`.
     *
     * `scanned = false` حين لم تُقرأ القائمة أصلًا: **غياب الأجهزة لا يُدّعى من فشل قراءة**،
     * فالفرق بين «عدّدتُ فلم أجد» و«لم أستطع أن أعدّ» هو الفرق بين قدرة غير موجودة وقياس ناقص.
     */
    fun discover(io: Io = SystemIo, taught: List<HardwareDeviceOverlay.Entry> = this.taught): Discovery {
        val names = io.listDirectories(ROOT)
            .sorted()
        if (names.isEmpty()) {
            // سؤالٌ واحد كان يُجاب بجوابٍ واحد، وهما سؤالان: **غياب صنف devfreq من النواة أصلًا**
            // قياسٌ نهائيّ (فلا يُقال للمستخدم «لم أستطع العدّ» ويُطلب منه جذرٌ لا يُغيّر شيئًا)،
            // و**جذرٌ موجود لا يُعدَّد** نقصُ قياس (فلا يُدَّعى غياب). وكانت الثانية وحدها تُقال
            // في الحالتين.
            return if (io.exists(ROOT)) {
                Discovery(emptyList(), scanned = false, reason = REASON_ROOT_UNREADABLE)
            } else {
                Discovery(emptyList(), scanned = true, reason = REASON_ROOT_ABSENT)
            }
        }
        val readings = names.map { read(it, io, taught) }
        val devices = readings.filterIsInstance<Reading.Classified>()
            .map { it.device }
            .sortedWith(compareBy({ it.kind.name }, { it.name }))
        return Discovery(
            devices = devices,
            scanned = true,
            reason = if (devices.isEmpty()) REASON_NO_DEVICE else "observed",
            unclassified = readings.filterIsInstance<Reading.Unknown>()
                .map { it.entry }
                .sortedBy { it.directory },
            graphicsOwned = readings.filterIsInstance<Reading.Owned>()
                .map { it.directory }
                .sorted(),
            unreadable = readings.filterIsInstance<Reading.Unreadable>()
                .map { it.directory }
                .sorted(),
        )
    }

    /** قراءة اكتشاف لا تكتب — والتوقيع نفسه يقبل [ReadIo] وحده. */
    fun observe(
        reads: ReadIo,
        taught: List<HardwareDeviceOverlay.Entry> = this.taught,
    ): Discovery = discover(ReadOnlyIo(reads), taught)

    /** يغلّف قارئًا كـ[Io] ويجيب شقّ الكتابة **محليًّا** بالرفض: لا محاولة كتابة تقع أصلًا. */
    private class ReadOnlyIo(private val reads: ReadIo) : Io {
        override fun exists(path: String) = reads.exists(path)
        override fun read(path: String) = reads.read(path)
        override fun listDirectories(path: String) = reads.listDirectories(path)
        override fun writable(path: String) = false
        override fun write(path: String, value: String) = false
    }

    /** يعيد قراءة جهاز واحد بعينه في المسار الذي اكتُشف (تحديث الحالة الحيّة). */
    fun refresh(
        path: String,
        io: Io = SystemIo,
        taught: List<HardwareDeviceOverlay.Entry> = this.taught,
    ): Device? {
        val name = path.substringAfterLast('/')
        if (name.isBlank()) return null
        val reading = read(name, io, taught)
        return (reading as? Reading.Classified)?.device?.takeIf { it.path == path }
    }

    /**
     * **فحص التعليم**: يُقرأ الجهاز بالصنف المقترح، ولا يُقبل التعليم إلا أن تُقرأ عقدة تردّد
     * حقيقية منه.
     *
     * ولماذا فحصٌ قبل الخزن: هو الفرق بين «المستخدم اختار من قائمة» و«المستخدم اختار صنفًا لعقدة
     * حقيقية لها سلّم» — والثاني وحده يُصلح الوعد «يعمل على أغلб الأجهزة». وما لم يُقَس لا يُخزَّن،
     * ورمز الرفض يُعرَض بجملته: التعليم ليس زرًّا ينجح دائمًا بلا أثر.
     *
     * ولا يكتب شيئًا: قارئ‑فقط (والنداء يأتي من واجهة، والكتابة كلها عبر المُحكِّم).
     */
    fun probe(
        directory: String,
        kind: Kind,
        io: Io = SystemIo,
        taught: List<HardwareDeviceOverlay.Entry> = this.taught,
    ): Probe {
        if (!HardwareDeviceOverlay.isCanonicalName(directory)) return Probe.Refused(PROBE_NAME_INVALID)
        if (graphicsOwned(directory)) return Probe.Refused(HardwareDeviceOverlay.REASON_GRAPHICS_OWNED)
        val candidate = runCatching { HardwareDeviceOverlay.Entry(directory, kind) }.getOrNull()
            ?: return Probe.Refused(PROBE_KIND_UNSUPPORTED)
        return when (val reading = read(directory, io, taught + candidate)) {
            is Reading.Classified -> Probe.Ok(reading.device)
            is Reading.Owned -> Probe.Refused(HardwareDeviceOverlay.REASON_GRAPHICS_OWNED)
            // و«لم تُقرأ» ليست «ليست واجهة تردّد»: الأولى تُعلن أن القياس لم يقع فيُوجَّه المستخدم
            // إلى ما يُصلحه (عبور مجلّد، أو اسم صحيح)، والثانية حكمٌ على العقدة بأنها ليست واجهة
            // تردّد. وكان الرمزان واحدًا، فكان اسمٌ مطبوعٌ خطأً يُقرأ «العقدة ليست واجهة» وهي لم
            // تُقرأ أصلًا — وهو الخلط نفسه الذي منعناه في [read].
            is Reading.Unreadable -> Probe.Refused(PROBE_UNREADABLE)
            is Reading.Unknown, Reading.NotAnInterface -> Probe.Refused(PROBE_NOT_INTERFACE)
        }
    }

    private fun read(directoryName: String, io: Io, taught: List<HardwareDeviceOverlay.Entry>): Reading {
        val path = "$ROOT/$directoryName"
        val declared = io.read("$path/device_name").orEmpty().trim()

        val rawFrequencies = parseLongList(io.read("$path/available_frequencies"))
        // تُقتطع هنا ولا يُفترض أن القارئ اقتطعها: قيمة sysfs تصل بفاصل سطر، وقارئ يردّ البايتات
        // كما هي يجعل كل تردّد يُحلَّل `null` — فيُقال عن جهاز *له* ساعة إنه بلا ساعة.
        val rawMin = io.read("$path/min_freq")?.trim()?.toLongOrNull()
        val rawMax = io.read("$path/max_freq")?.trim()?.toLongOrNull()
        val rawCurrent = io.read("$path/cur_freq")?.trim()?.toLongOrNull()
        val governor = io.read("$path/governor")?.trim()?.takeIf(String::isNotEmpty)
        val governors = io.read("$path/available_governors").orEmpty()
            .split(Regex("\\s+")).filter(String::isNotBlank).distinct()

        // وقبل الحكم على «أي عقدة هذه»: هل قرأنا منها **شيئًا**؟ سطرٌ لم يُقرأ منه حرف — ولا حتى
        // `device_name` الذي تنشره عُقَد devfreq كلّها — يعني أن القراءة مُنعت (عبورُ مجلّدٍ أب
        // محجوب، أو صلاحية غائبة)، لا أن العقدة بلا تردّد. والفرق ليس لغويًّا: «ليست واجهة تردّد»
        // دعوى تحتاج قراءةً تُسندها، وبلا قراءة يبقى الحكم الأمين **نقص قياس يُعلَن**.
        val answered = declared.isNotEmpty() || governor != null ||
            rawFrequencies.isNotEmpty() || rawMin != null || rawMax != null || rawCurrent != null
        if (!answered) return Reading.Unreadable(directoryName)

        // وما قُرئ ولم يُعلن سلّمًا ولا حدودًا ولا ساعة = ليست واجهة تردّد أصلًا (ولا تُعرَض
        // للتعليم: لا معنى لتعليم عقدة لا تردّد فيها شيء). وهذه دعوى **مقيسة**: عقدة أجابت بشيء
        // ولم تنشر تردّدًا.
        if (governor == null && rawFrequencies.isEmpty() && rawMin == null && rawMax == null && rawCurrent == null) {
            return Reading.NotAnInterface
        }

        val frequencies = rawFrequencies.filter { it > 0L }.distinct().sorted()
        val writableRange = io.writable("$path/min_freq") && io.writable("$path/max_freq")

        // الرسوميات تُخرَج أولًا: `gpu_frequency` له مالكه القائم، وتعليمُ عنقود رسوميّ ذاكرةً
        // كان يجعل للمقبض الواحد كاتبَين (وهو ما تمنعه [HardwareDeviceOverlay] في القراءة أيضًا).
        if (graphicsOwned("$directoryName $declared")) return Reading.Owned(directoryName)

        // القاعدة المبنية تفوز: تعليمٌ لجهاز تعرفه القواعد لا يغيّر صنفه أبدًا.
        val builtIn = classify("$directoryName $declared $path")
        val userKind = if (builtIn == null) taught.firstOrNull { it.device == directoryName }?.kind else null
        if (builtIn == null && userKind == null) {
            return Reading.Unknown(
                Unclassified(
                    directory = directoryName,
                    declared = declared,
                    ladderSize = frequencies.size,
                    writable = writableRange,
                ),
            )
        }

        val kind = builtIn?.first ?: requireNotNull(userKind)
        val family = builtIn?.second ?: Family.GENERIC
        val userTaught = builtIn == null
        val unit = GpuHardwareBackend.inferFrequencyUnit(
            rawFrequencies + listOfNotNull(rawMin, rawMax, rawCurrent),
        )
        val evidence = buildList {
            add("devfreq-device")
            add("kind-${kind.name.lowercase()}")
            add("family-${family.name.lowercase()}")
            if (userTaught) add("user-taught")
            if (frequencies.size >= 2) add("published-ladder-${frequencies.size}")
            if (unit != GpuHardwareBackend.FrequencyUnit.AMBIGUOUS) add("consistent-${unit.name.lowercase()}-unit")
            if (rawMin != null || rawMax != null) add("kernel-declared-bounds")
        }

        return Reading.Classified(
            Device(
                path = path,
                name = declared.ifBlank { directoryName },
                kind = kind,
                family = family,
                governor = governor,
                governors = governors,
                // الحدود تُحفظ **كما قرأها السائق**؛ وحدّ غير مقروء لا يُخترع من السلّم، فالاختراع
                // يجعل خط الأساس يبدو مقروءًا فتمرّ كتابة لا يمكن التراجع عنها إلى ما كان عليه فعلًا.
                minFreq = rawMin,
                maxFreq = rawMax,
                currentFreq = rawCurrent,
                frequencies = frequencies,
                frequencyUnit = unit,
                minWritable = io.writable("$path/min_freq"),
                maxWritable = io.writable("$path/max_freq"),
                governorWritable = io.writable("$path/governor"),
                userTaught = userTaught,
                evidence = evidence,
            ),
        )
    }

    private fun parseLongList(raw: String?): List<Long> = raw.orEmpty().split(Regex("\\s+"))
        .mapNotNull(String::toLongOrNull).filter { it > 0L }.distinct().sorted()

    /**
     * رمز فجوة واحد يُستعمل في موضعين: رفض المُلاءِم للملاءمة (`assess`) وسبب الفجوة في خريطة
     * أطلس. وتعريفٌ واحد عن قصد: رمزان لنقص واحد يجعل تقريرًا يقول «لا شيء» وآخر يقول «تعليم».
     */
    const val UNCLASSIFIED_GAP_CODE = "unclassified-devfreq-node-seen"

    /**
     * رمز فجوة ثانٍ: عُقَد **رُصدت ولم تُقرأ**. ولا يُدمج مع [UNCLASSIFIED_GAP_CODE]: ذاك يقول
     * «سمِّ الصنف» وفعلُه نقرةٌ واحدة، وهذا يقول «لم يُقرأ شيء» وفعلُه صلاحيةُ عبور أو مسارٌ
     * مصحَّح — ودمجُهما يجعل الخريطة تطلب التعليم حيث لا تعليم ينفع.
     */
    const val UNREADABLE_GAP_CODE = "devfreq-node-unreadable"

    /** سببا «لا قياس» على الجذر — رمزان لا واحد، كما في العُقَد. */
    const val REASON_ROOT_UNREADABLE = "devfreq-root-unreadable"
    const val REASON_ROOT_ABSENT = "devfreq-root-absent"

    /**
     * الجذر عُدَّ فعلًا وليس فيه جهاز ذاكرة/تخزين — **دليل غياب** لا نقص قياس.
     *
     * وكان نصًّا في موضع الكتابة وحده؛ وصار رمزًا مسمّى لأن الطبقة نفسها تُطبع في البصمة
     * (`backend=devfreq-backend:<reason>`) فيحتاج شرحها معرّفًا لا جملةً منسوخة.
     */
    const val REASON_NO_DEVICE = "no-memory-or-storage-devfreq-observed"

    /** رموز رفض الفحص — ثابتة وتُترجَم في الواجهة. */
    const val PROBE_NAME_INVALID = "probe-name-invalid"
    const val PROBE_KIND_UNSUPPORTED = "probe-kind-unsupported"
    const val PROBE_NOT_INTERFACE = "probe-not-a-frequency-interface"

    /** الفحص لم يقرأ شيئًا من العقدة المطلوبة — نقص قياس لا حكمٌ على العقدة. */
    const val PROBE_UNREADABLE = "probe-node-unreadable"

    // ── الطلب والحكم ───────────────────────────────────────────────────────────────────────────

    /** نسبة من أعلى درجة معلنة ← أقرب درجة **معلنة** لا تتجاوزها؛ ولا قيمة مُخترعة. */
    fun nodeForFraction(device: Device, fraction: Float): Long? {
        val ladder = device.ladder
        val ceiling = device.configurableMaxFreq ?: return null
        if (ladder.isEmpty() || ceiling <= 0L) return null
        val wanted = (ceiling * fraction.coerceIn(0f, 1f)).toLong()
        return ladder.lastOrNull { it <= wanted } ?: ladder.firstOrNull()
    }

    /**
     * الطلب من نسبة: سقفٌ بمقدار [fraction] من أعلى ما يمكن طلبه، وأرضيةٌ اختيارية بنسبة
     * [floorFraction] **لا تتجاوز السقف** (مدى بـ`min > max` ترفضه النواة).
     */
    fun requestForFraction(device: Device, fraction: Float, floorFraction: Float? = null): Request? {
        val max = nodeForFraction(device, fraction) ?: return null
        val min = floorFraction?.let { nodeForFraction(device, it) }
            ?.takeIf { it in device.ladder }
            // والأرضية **تُقصّ إلى السقف** ولا تُسقط الطلب: مدى بـ`min > max` ترفضه النواة أصلًا،
            // وإلغاءُ الطلب لأن أرضيته عالية يترك المستخدم بلا تغيير ولا سبب مفهوم — وهو النصّ
            // نفسه في `MemoryUfsModel.request`. (كان الإسقاط هنا خطأً كشفه اختبار لم يكن يُشغَّل.)
            ?.let { minOf(it, max) }
        return Request(minFreq = min, maxFreq = max, governor = null)
    }

    /** الطلب من قيمتين حرفيتين (شرائح الشاشة تعطي درجات معلنة أصلًا). */
    fun requestForRange(device: Device, minNode: String, maxNode: String): Request? {
        val request = Request(
            minFreq = minNode.trim().toLongOrNull(),
            maxFreq = maxNode.trim().toLongOrNull() ?: return null,
        )
        return if (validate(device, request) == null) request else null
    }

    /**
     * حكم الشكل قبل أي كتابة. يعيد **رمز السبب** أو `null` حين يصلح الطلب.
     *
     * ولا يقبل هذا الحكم قيمة ليست من السلّم المُعلن: الطلب يُبنى من السلّم أصلًا، وتغيير ذلك
     * يعني كتابة رقم لا وجود له في الجهاز.
     */
    fun validate(device: Device, request: Request): String? {
        if (!device.unitTrusted) return "frequency-unit-unproven"
        if (device.ladder.size < 2) return "no-published-ladder"
        if (!device.minWritable || !device.maxWritable) return "range-not-writable"
        val min = request.minFreq
        val max = request.maxFreq
        if (min == null && max == null && request.governor == null) return "empty-request"
        if (min != null && min !in device.ladder) return "min-not-advertised"
        if (max != null && max !in device.ladder) return "max-not-advertised"
        if (min != null && max != null && min > max) return "min-above-max"
        if (request.governor != null && device.governors.isNotEmpty() && request.governor !in device.governors) {
            return "governor-not-advertised"
        }
        return null
    }

    /** خط الأساس كما قُرئ الآن — لا قيمة محفوظة من اكتشاف سابق. */
    fun captureBaseline(device: Device, io: Io = SystemIo): Baseline {
        val live = refresh(device.path, io) ?: device
        return Baseline(live.path, live.minFreq, live.maxFreq, live.governor)
    }

    /**
     * الكتابة المتحقّقة: تُكتب الحدود ثم يُقرأ الجهاز ثانيةً، والنجاح **قراءةٌ أثبتت الطلب**
     * لا نجاحُ أمر. وعند الفشل يُعاد خط الأساس ويُقاس الاسترجاع.
     */
    fun applyValidated(device: Device, request: Request, io: Io = SystemIo): TransactionResult {
        val refusal = validate(device, request)
        if (refusal != null) {
            return TransactionResult(request, device, writeSucceeded = false, verified = false, error = refusal)
        }
        val baseline = captureBaseline(device, io)
        val live = refresh(device.path, io) ?: device

        val wrote = writeRange(live, request, io)
        val after = refresh(device.path, io)
        if (wrote && satisfied(request, after)) {
            return TransactionResult(request, after, writeSucceeded = true, verified = true)
        }

        val restored = restoreBaseline(baseline, io)
        return TransactionResult(
            requested = request,
            actual = refresh(device.path, io) ?: after,
            writeSucceeded = wrote,
            verified = false,
            rollbackAttempted = true,
            rollbackVerified = restored,
            error = when {
                !wrote -> "write-rejected-by-kernel"
                else -> "live-value-differs-from-request"
            },
        )
    }

    /** استرجاع خط الأساس: يُكتب ما قُرئ قبله، ويُقاس بالقراءة. */
    fun restoreBaseline(baseline: Baseline, io: Io = SystemIo): Boolean {
        val device = refresh(baseline.devicePath, io) ?: return false
        var ok = true
        // الترتيب عند الاسترجاع معاكسٌ عند الكتابة: الأرضية القديمة قد تكون فوق السقف الحاليّ،
        // فيُعاد السقف أوّلًا ثم الأرضية — وإلا رفض السائق كتابة الأرضية.
        if (baseline.maxFreq != null && baseline.maxFreq != device.maxFreq) {
            ok = io.write("${baseline.devicePath}/max_freq", baseline.maxFreq.toString()) && ok
        }
        if (baseline.minFreq != null && baseline.minFreq != device.minFreq) {
            ok = io.write("${baseline.devicePath}/min_freq", baseline.minFreq.toString()) && ok
        }
        if (baseline.governor != null && baseline.governor != device.governor) {
            io.write("${baseline.devicePath}/governor", baseline.governor)
        }
        val live = refresh(baseline.devicePath, io) ?: return false
        val restoredMax = baseline.maxFreq == null || live.maxFreq == baseline.maxFreq
        val restoredMin = baseline.minFreq == null || live.minFreq == baseline.minFreq
        return ok && restoredMax && restoredMin
    }

    /**
     * كتابة الحدود بترتيب آمن.
     *
     * والترتيب ليس تفصيلًا: رفع الأرضية فوق السقف الحيّ يرفضه السائق (المدى يصير مقلوبًا لحظةً)،
     * فيُكتب السقف أوّلًا في هذه الحالة — والقاعدة نفسها في [GpuHardwareBackend.rangeWriteOrder].
     */
    private fun writeRange(device: Device, request: Request, io: Io): Boolean {
        val currentMax = device.maxFreq
        val targetMin = request.minFreq
        val targetMax = request.maxFreq
        val order = if (targetMin != null && currentMax != null && targetMin > currentMax) {
            listOf("max", "min")
        } else {
            listOf("min", "max")
        }
        var ok = true
        order.forEach { field ->
            val value = if (field == "min") targetMin else targetMax
            if (value != null) ok = io.write("${device.path}/${field}_freq", value.toString()) && ok
        }
        if (request.governor != null) {
            ok = io.write("${device.path}/governor", request.governor) && ok
        }
        return ok
    }

    /**
     * هل القراءة الحيّة **دليل** على تنفيذ الطلب؟ — الحقول المطلوبة وحدها تُحكم، والحكم **احتواءً
     * لا تساويًا**.
     *
     * ولماذا الاحتواء: الطلب هنا «مدى» — لا تتجاوز كذا ولا تنزل تحت كذا — والسائق يضيّق المدى
     * لأسباب مشروعة (حرارة، طاقة، سقف مصنّع)، ومدى أضيق من المطلوب **تلبيةٌ** لا فشل. والتساوي
     * الحرفيّ كان يجعل ضبطًا ناجحًا يُقرأ فشلًا فيُعاد خط الأساس — أي يُلغى ما طلبه المستخدم
     * ويُقال له «فشل». (وهذا أيضًا خرج من تشغيل الاختبارات.)
     *
     * والحدّان اللذان لا يُتسامح فيهما: قيمةٌ صفرية أو غائبة ليست قياسًا (الصفر في عقدة تردّد إمّا
     * «لا قراءة» وإمّا «أُطفئ»، ولا يُدَّعى أيّهما)، وقيمةٌ **تجاوزت المطلوب** ليست تلبية.
     */
    fun satisfied(request: Request, live: Device?): Boolean {
        if (live == null) return false
        val min = live.minFreq
        val max = live.maxFreq
        if (request.maxFreq != null && (max == null || max <= 0L || max > request.maxFreq)) return false
        if (request.minFreq != null && (min == null || min <= 0L || min < request.minFreq)) return false
        if (request.governor != null && live.governor != request.governor) return false
        return true
    }

    // ── الصيغ المُرمَّزة (للـarbiter) ────────────────────────────────────────────────────────────

    /**
     * ترميز الطلب: `<min>:<max>|<governor>`.
     *
     * ولماذا هذا الشكل بعينه: `HardwareVerification.ceilingReached` تقرأ **آخر رقم في مدى**
     * (`min:max`) قبل `|`، فيقع حكم «هل بلغنا؟» على السقف لا على الأرضية — وهذا هو السؤال
     * الصحيح لمقبض سقف، وهو الذي يمنع تخطّي كتابة قيمة أعلى لأن قيمتنا السابقة احتُسبت «مُلبّاة».
     */
    fun encodeRequest(request: Request): String =
        "${request.minFreq ?: ""}:${request.maxFreq ?: ""}$FIELD_SEPARATOR${request.governor ?: UNTOUCHED}"

    /**
     * خط الأساس بنفس شكل الترميز: `min:max|governor` كما قُرئت لحظةَ القراءة.
     *
     * وشكلٌ واحد للطلب والقراءة **شرطٌ للاسترجاع**: خطُ أساس بصيغة ثانية يحتاج مُفكِّكًا ثانيًا،
     * وأوّل ما يسقط عندها الاسترجاعُ نفسه — أي أن الفشل يترك الجهاز على قيمة لم يطلبها أحد.
     */
    fun encodeBaseline(device: Device): String =
        "${device.minFreq ?: ""}:${device.maxFreq ?: ""}$FIELD_SEPARATOR${device.governor ?: UNTOUCHED}"

    /** القراءة الحيّة بنفس الشكل ولنفس الحقول: التساوي الحرفيّ يصير ممكنًا بلا تخمين. */
    fun encodeLive(device: Device, request: Request): String {
        val min = if (request.minFreq != null) device.minFreq?.toString().orEmpty() else UNTOUCHED
        val max = if (request.maxFreq != null) device.maxFreq?.toString().orEmpty() else UNTOUCHED
        val governor = if (request.governor != null) device.governor ?: UNTOUCHED else UNTOUCHED
        return "$min:$max$FIELD_SEPARATOR$governor"
    }

    /** أعلى ما يمكن طلبه الآن بوحدة العقدة؛ `null` = لا قياس. */
    fun ceilingNode(device: Device): Long? = device.configurableMaxFreq

    /** القيمة بوحدة العقدة ← ميغاهرتز للعرض. `null` بلا وحدة مُثبتة أو برقم غير معقول. */
    fun megahertz(device: Device, raw: Long?): Long? {
        if (raw == null || raw <= 0L || !device.unitTrusted) return null
        val hz = raw * device.frequencyUnit.hzMultiplier
        return (hz / 1_000_000L).takeIf { it in 1L..1_000_000L }
    }
}
