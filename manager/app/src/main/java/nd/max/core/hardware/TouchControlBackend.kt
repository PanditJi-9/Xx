/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/**
 * مالك التحكم باللمس: **مفردة العُقَد، والاكتشاف، والقدرة، ومحرّك التسوية** — في موضع واحد.
 *
 * ولماذا وُجد هذا الملف
 * --------------------
 * كان للتحكم باللمس **ثلاثة تعريفات** لا تعريف واحد: قائمة المرشّحين في
 * `TouchBoostViewModel` (نمط اللعب ومعدّل العيّنات)، ونسخة ثانية في `PanelToggles.TOUCH_NODES`،
 * ومحوّل المصنّع في `XiaomiVendorFeatures`. والقوائم الثلاث كانت **متطابقة اليوم ومستقلّة في
 * الغد**: إضافة مسار في أحدها لا تصل الآخرين، فيرى المستخدم المفتاح في اللوحة ولا يراه في
 * الشاشة — وهو نفس علة «مقبض له كاتبان» التي يمنعها المستودع، منسوخةً على القراءة.
 *
 * **وحدّان لا يُخترقان هنا:**
 * 1. **لا كتابة إلى محوّل الروم للّمس.** النقل يُكتشف (خدمة معلنة في مدير الخدمات)، لكن معنى
 *    أرقام الأوضاع (`mode id`) في واجهة المصنّع ليس مُمثَّلًا في المستودع ولا قابلًا للإثبات بلا
 *    جهاز — فالكتابة بالتخمين إلى HAL اللمس تُنتج حالة لا يعرف أحد ما هي. فيُعلَن **الرفض**:
 *    قدرةٌ بلا طريق كتابة مثبَت، لا محاولة أعمى.
 * 2. **لا كتابة بلا قراءة.** كل كتابة تمرّ بـ[WriteVerification] وترجع بحكمها كما هو، والقراءة
 *    قبل الكتابة تجعل الدالة **مُسوّية** لا كاتبة عمياء. والدافع مقيس: `AppMonitor` ينادي هذا
 *    المسار كل ٥٠٠ م.ث، فسُجّلت على جهاز حقيقي ٣٠١٦ كتابة إلى `/proc/touch_boost/enable` في
 *    ٣٦ دقيقة بنفس القيمة (٩٤% من ملف السجل، والعطل الحقيقي مدفون تحتها).
 */
object TouchControlBackend {

    /**
     * نوع المقبض الذي تخدمه العقدة — لأن معنى `1`/`0` يختلف باختلاف المقبض:
     * نمط اللعب يرفع حساسية/معدّل الاستطلاع، والنقر المزدوج يوقظ الشاشة.
     */
    enum class Kind(val token: String) {
        GAME_MODE("game-mode"),
        SAMPLE_RATE("sample-rate"),

        /**
         * مُعزّز إدخال المعالج — وليس نمط لعب.
         *
         * و`/proc/touch_boost/enable` في نوى MediaTek يرفع حدود تردّد CPU لحظة اللمس
         * (`[touch_boost]cpu_policy_init` و`opp cnt` في dmesg الجهاز المقيس)، ولا يغيّر معدّل
         * أخذ عيّنات وحدة اللمس. فُصل عن [GAME_MODE] بعد أن كان يُعرض باسمه: الخلط بينهما يجعل
         * الشاشة تعد بمعدّل استطلاع لا يملكه المسار، ويقرأ المستخدم «لا أثر» عن حق.
         */
        INPUT_BOOST("input-boost"),

        DOUBLE_TAP_WAKE("double-tap-wake"),
    }

    /** عقدة لمحكّم اللمس: مسارها وقيمتاها. و`on`/`off` كما تنشرهما السائقات المعروفة. */
    data class Node(val path: String, val onValue: String, val offValue: String, val kind: Kind)

    /**
     * نمط اللعب — المفردات المعروفة في `proc`/`sys` (شاشات Qualcomm/Novatek وFocaltech).
     *
     * والترتيب يعني الأفضلية: أول عقدة **مقروءة** هي التي تُستعمل، فلا يُكتب في عقدة أعلى
     * في القائمة ثم تُقرأ الثانية فيُرى «لا أثر».
     */
    val GAME_MODE_NODES: List<Node> = listOf(
        Node("/proc/touchpanel/game_switch_enable", "1", "0", Kind.GAME_MODE),
        Node("/sys/touchpanel/game_switch_enable", "1", "0", Kind.GAME_MODE),
    )

    /**
     * معدّل العيّنات: **فارغ عن قصد**، ولا يُملأ بأسماء مخمَّنة.
     *
     * وأوامر معدّل الاستطلاع ليست معيارية: القيمة قد تكون هرتزًا (`120`/`240`) أو فهرسًا
     * (`1`/`2`/`3`) أو مرمّزًا خاصًّا بسائق. ووضع قيمة في جدول بلا دليل يعني الكتابة برقم قد
     * لا يعني شيئًا عند السائق — أو يعني شيئًا آخر. فتُترك فارغة حتى يُثبت العقد **والوحدة**
     * معًا على جهاز مُقاس، ويُعلَن النقص في الشاشة والقدرة بدل ملء الفراغ بتخمين.
     */
    val SAMPLE_RATE_NODES: List<Node> = emptyList()

    /**
     * مُعزّز إدخال المعالج — مسار MediaTek وحده، وبمعناه المقيس لا بادّعاء «نمط لعب».
     *
     * والدليل من جهاز المستخدم نفسه (Redmi rodin · MT6899 · Android 16): نواة `touch_boost`
     * تطبع عند الإقلاع `touch_boost_init, policy[0..2]` و`cpu_policy_init` و`opp cnt` — أي أنها
     * سياسات CPU وتردّدات OPP، لا سجلّ وحدة لمس. فالكتابة فيها تعزّز المعالج لحظة اللمس، وهذا
     * كل ما يُقال عنها في الواجهة؛ ولا تُوضع في [GAME_MODE_NODES] ولا تُسمّى معدّل استطلاع.
     */
    val INPUT_BOOST_NODES: List<Node> = listOf(
        Node("/proc/touch_boost/enable", "1", "0", Kind.INPUT_BOOST),
    )

    /** النقر المزدوج للإيقاظ — عقدتان معروفتان، الأولى هي المعتادة على شاشات OEM. */
    val DOUBLE_TAP_WAKE_NODES: List<Node> = listOf(
        Node("/proc/touchpanel/double_tap_enable", "1", "0", Kind.DOUBLE_TAP_WAKE),
        Node("/sys/android_touch/doubletap2wake", "1", "0", Kind.DOUBLE_TAP_WAKE),
    )

    /**
     * مرشّحو **تعزيز** اللمس بترتيب الأفضلية: نمط اللعب، ثم معدّل العيّنات، ثم تعزيز إدخال CPU.
     *
     * والترتيب مقصود: تعزيز المعالج آخر ما يُعرض، لأن أثره غير المباشر (تردّد CPU) لا يُقاس
     * بمعدّل أحداث اللمس — فلا يتقدّم على عقدة تُغيّر وحدة اللمس نفسها.
     */
    val BOOST_NODES: List<Node> = GAME_MODE_NODES + SAMPLE_RATE_NODES + INPUT_BOOST_NODES

    /** كل ما يُتصفّح من عُقَد اللمس (تعزيز ونقر مزدوج) — لقدرة الجهاز ولشاشة اللمس معًا. */
    val ALL_NODES: List<Node> = BOOST_NODES + DOUBLE_TAP_WAKE_NODES

    /**
     * نصف القراءة من عتاد اللمس — يمرّ منه كل ما هو **قراءة**.
     *
     * ووُجد ليُقاس بلا جهاز: كل حكم في هذا الملف (أيّ عقدة وُجدت، وهل تحمل الطلب، وهل تُكتب)
     * يمكن أن يُجرَّب بمُوجّه وهمي، فلا يبقى «يُرجَّح أن المسار يعمل» بل يُقاس.
     */
    interface Reads {
        fun read(path: String): String?
        fun exists(path: String): Boolean

        /**
         * هل تقبل العقدة كتابة؟ سؤال **قراءة** (`test -w` عبر الجذر) لا كتابة.
         *
         * ووُضع في موصّل القراءة عن قصد: القدرة تُقاس ولا تُتجرب، وفحص الكتابة الفعلي يجري في
         * موضعه من [Reconciler] بخطّه وحكمه.
         */
        fun writable(path: String): Boolean

        /**
         * نداء واحد لقائمة مسارات — القراءة المجمَّعة أرخص من رحلتين لكل مرشّح.
         *
         * والافتراضي يجعل كل مُوجّه وهمي يعمل بلا تعديل: من يعرف `read` يعرف القائمة.
         */
        fun readMany(paths: List<String>): List<String?> = paths.map(::read)
    }

    /**
     * نصف الكتابة: الكتابة **المتحقَّقة** بالبدائية المشتركة، فلا تُنسخ مقارنة نصّية هنا.
     */
    interface Access : Reads {
        fun writeVerified(path: String, value: String): WriteVerification.Outcome
    }

    /** الوصول على الجهاز — كلّه عبر [RootFileAccess]، ولا قراءة ملفّ مباشرة من هنا. */
    object SystemAccess : Access {
        override fun read(path: String): String? = RootFileAccess.read(path)
        override fun exists(path: String): Boolean = RootFileAccess.exists(path)
        override fun writable(path: String): Boolean = RootFileAccess.writable(path)
        override fun readMany(paths: List<String>): List<String?> = RootFileAccess.readMany(paths)
        override fun writeVerified(path: String, value: String): WriteVerification.Outcome =
            RootFileAccess.writeVerified(path, value)
    }

    /**
     * جسر المصنّع: ما **يُقاس** عن محوّل الروم، وما **يُرفض** فيه — لا ما يُتمنّى.
     *
     * والفصل مقصود بين ثلاثة أسئلة لأن خلطها هو العطب: «النقل معلن» لا يعني «نملك طريق كتابة»،
     * و«طريق الكتابة موجود» لا يعني «الكبسة نجحت». ويوم تُثبت النسخةُ أوضاعًا حقيقية على جهاز
     * مُقاس، تُركَّب جسرًا يجاوب الثلاثة — بلا تعديل هذا الملف.
     */
    interface VendorBridge {
        /** هل الروم يعلن خدمة اللمس الخاصة به؟ سؤال وجود، يُقاس ولا يُخمَّن. */
        fun transportPresent(): Boolean

        /**
         * هل نملك طريق **كتابة** مثبَتًا إلى المحوّل؟ (معنى الأوضاع معروف، والتحقق ممكن.)
         *
         * و`false` هنا ليست «فشل» بل **رفض بالعقد**: كتابة تخمينية إلى HAL اللمس تترك حالةً لا
         * يعرفها أحد. ومن يقرأ الشاشة يرى «النقل موجود ولا نكتب فيه» لا «جهازك لا يدعم اللمس».
         */
        val hasWritePath: Boolean

        /** يطبّق الطلب إن كان الطريق مثبَتًا. `null` = لا طريق (رفض بالعقد)، لا فشل. */
        fun apply(boost: Boolean): Boolean?
    }

    /** الجسر الافتراضي: لا نقل، ولا طريق، ولا كتابة — فما لم يُقَس لا يُدَّعى. */
    object NoVendorBridge : VendorBridge {
        override fun transportPresent(): Boolean = false
        override val hasWritePath: Boolean = false
        override fun apply(boost: Boolean): Boolean? = null
    }

    /**
     * الجسر المركَّب من طبقة التطبيق (`MaxManagerApplication.onCreate`).
     *
     * والافتراضي [NoVendorBridge] **مقصود**: نقيض العطب الذي وُجد هذا الحقل لإصلاحه — نصّ في
     * الشاشة يقول «يُستعمل محوّل الروم» بينما `applyTouchBoost` تُعيد `false` دائمًا، فيقرأ
     * المستخدم وعدًا لا وجود له.
     */
    @Volatile
    var vendorBridge: VendorBridge = NoVendorBridge

    /**
     * نتيجة اكتشاف عتاد اللمس — تُقرأ في الشاشة والقدرة والسجل، فلا يُخترع لكل قارئ حكمه.
     */
    data class Discovery(
        val gameMode: Node?,
        val sampleRate: Node?,
        /** تعزيز إدخال CPU (MediaTek `touch_boost`) — ليس معدّل استطلاع ولا نمط لعب. */
        val inputBoost: Node?,
        val doubleTap: Node?,
        val vendorTransportPresent: Boolean,
        val vendorHasWritePath: Boolean,
    ) {
        /**
         * مزوّد التعزيز: نمط اللعب أولًا، ثم معدّل العيّنات، ثم تعزيز إدخال CPU.
         *
         * والترتيب هو الأثر: عقدة تُغيّر وحدة اللمس تُقدَّم على عقدة ترفع تردّد المعالج فقط.
         */
        val boostProvider: Node? get() = gameMode ?: sampleRate ?: inputBoost

        /** هل توجد عقدة تحكّم فعلية (تعزيز)؟ */
        val hasBoostNode: Boolean get() = boostProvider != null

        /** هل وُجد أي شيء يُقرأ: تعزيز أو نقر مزدوج (تعمل به شاشة اللمس). */
        val hasAnyNode: Boolean get() = hasBoostNode || doubleTap != null

        /** مسارات العُقَد المرصودة ووحداتها — دليلٌ يُكتب في البصمة والحزمة. */
        val evidence: List<String>
            get() = ALL_NODES.filter { node ->
                node == gameMode || node == sampleRate || node == inputBoost || node == doubleTap
            }.map { "${it.kind.token}:${it.path}" } +
                if (vendorTransportPresent) listOf(VENDOR_TRANSPORT_EVIDENCE) else emptyList()

        /**
         * رمز الحال — معرّف إنجليزي ثابت (بلا ترجمة) يُقرأ في سطر القدرة:
         * `touch-sysfs` · `vendor-mode-contract-unproven` · `not-detected`.
         */
        val reason: String
            get() = when {
                hasBoostNode -> "touch-sysfs"
                vendorTransportPresent && !vendorHasWritePath -> VENDOR_CONTRACT_REASON
                vendorTransportPresent -> "vendor-hal"
                else -> "not-detected"
            }
    }

    /**
     * اكتشاف **قراءة فقط**: عقدة واحدة تُقرأ مرّة واحدة لكل مجموعة (نداء مجمَّع).
     *
     * والاختيار على «أول عقدة تُقرأ» لا على «أول عقدة توجد»: عقدة موجودة وغير مقروءة ليست عقدة
     * تحكّم — لا تُحكم عليها بالوجود، ولا تُنسب إليها كتابة.
     */
    fun discover(reads: Reads = SystemAccess, vendor: VendorBridge = vendorBridge): Discovery {
        val boostReads = reads.readMany(BOOST_NODES.map(Node::path))
        val gameMode = GAME_MODE_NODES.indices
            .firstOrNull { boostReads.getOrNull(it) != null }
            ?.let(GAME_MODE_NODES::get)
        val sampleBase = GAME_MODE_NODES.size
        val sampleRate = if (gameMode == null) {
            SAMPLE_RATE_NODES.indices
                .firstOrNull { boostReads.getOrNull(sampleBase + it) != null }
                ?.let(SAMPLE_RATE_NODES::get)
        } else null
        // تعزيز الإدخال يُقرأ دائمًا (لا فقط عند غياب نمط اللعب): الشاشة تسمّيه في «المزوّدات»،
        // والقدرة تذكر دليله، ولو كان نمط اللعب موجودًا فهما عقدتان مختلفتان لا بديلان.
        // والفهرس المحلي يُستعمل على القائمة المحلية — والمطلق على `boostReads` وحده.
        val inputBase = sampleBase + SAMPLE_RATE_NODES.size
        val inputBoost = INPUT_BOOST_NODES.indices
            .firstOrNull { boostReads.getOrNull(inputBase + it) != null }
            ?.let(INPUT_BOOST_NODES::get)
        val doubleTapReads = reads.readMany(DOUBLE_TAP_WAKE_NODES.map(Node::path))
        val doubleTap = DOUBLE_TAP_WAKE_NODES.indices
            .firstOrNull { doubleTapReads.getOrNull(it) != null }
            ?.let(DOUBLE_TAP_WAKE_NODES::get)
        return Discovery(
            gameMode = gameMode,
            sampleRate = sampleRate,
            inputBoost = inputBoost,
            doubleTap = doubleTap,
            vendorTransportPresent = runCatching { vendor.transportPresent() }.getOrDefault(false),
            vendorHasWritePath = vendor.hasWritePath,
        )
    }

    /**
     * إسقاط الاكتشاف على قدرة العتاد (`HardwareFeature.TOUCH_CONTROL`) — **قياسٌ واحد** لا ثانٍ.
     *
     * وكانت `TOUCH_CONTROL` لا تُملأ أبدًا فتظهر `not-detected` على كل جهاز، ولو كانت عقدة اللمس
     * تُكتب فعلًا. والقدرة الآن من المصدر نفسه الذي يكتب: عقدة مقروءة قابلة للكتابة ⇒ `READ_WRITE`،
     * ومقروءة فقط ⇒ `READ_ONLY`، ولا عقدة ⇒ `NONE` مع **سببها** (نقص عقد مقابل رفض عقد).
     */
    fun capability(reads: Reads = SystemAccess, vendor: VendorBridge = vendorBridge): FeatureCapability {
        val discovery = discover(reads, vendor)
        val access = when {
            discovery.boostProvider?.let { reads.writable(it.path) } == true -> AccessLevel.READ_WRITE
            // وعقدة النقر المزدوج تعمل وحدها: من يقرأ القدرة لا يقول «لا تحكّم» وجهازُه يُوقَظ
            // بلمسة مزدوجة — الحقيقة أن النمط/المعدّل هما الغائبان.
            discovery.doubleTap?.let { reads.writable(it.path) } == true -> AccessLevel.READ_ONLY
            discovery.hasAnyNode -> AccessLevel.READ_ONLY
            else -> AccessLevel.NONE
        }
        return FeatureCapability(
            feature = HardwareFeature.TOUCH_CONTROL,
            access = access,
            backend = "touch-backend:${discovery.reason}",
            evidence = discovery.evidence,
        )
    }

    // ── محرّك التسوية: القرار ← القراءة ← الكتابة ← الحكم ────────────────────────────────────

    /** القناة التي نُفِّذ بها القرار — تُقرأ في السجل، فلا يُخمَّن أيّها عمل. */
    enum class Channel(val token: String) {
        SYSFS("sysfs"),
        VENDOR("vendor-hal"),
        NONE("none"),
    }

    /**
     * نتيجة محاولة التسوية — أربع حالات، لا اثنتان.
     *
     * والفرق بينها هو ما يجعل سطر السجل كافيًا بلا شرح إضافي: [DEFERRED] «لا شيء يُفعَل الآن»
     * (العقدة تحمل المطلوب، أو مهلة إعادة محاولة سارية)، و[REFUSED] «حاولنا فلم تُقبل»،
     * و[UNAVAILABLE] «لا مزوّد على هذا الجهاز» — والثالث لا يُصلحه إعداد، بخلاف الثاني.
     */
    enum class Result(val token: String) {
        APPLIED("applied"),
        DEFERRED("deferred"),
        REFUSED("refused"),
        UNAVAILABLE("unavailable"),
    }

    /** محاولة واحدة: ماذا جرى، على أيّ قناة، وبأيّ رمز سبب، **وما كانت عليه العقدة**. */
    data class Attempt(
        val result: Result,
        val channel: Channel,
        val path: String?,
        val reason: String,
        /**
         * القيمة الحيّة للعقدة كما قُرئت — تُكتب في سطر السجل بجانب المطلوب.
         *
         * وليست تجميلًا: حكمٌ بلا قيمة مقروءة يترك من يقرأ السجل يسأل «وما القيمة الحقيقية؟»
         * فيُعيد القراءة يدويًّا على جهاز لا يملكه. و`null` تعني «لم تُقرأ» — لا «صفر».
         */
        val live: String? = null,
    ) {
        /** هل حفظ التفضيل مشروع؟ — «لم تُحاول بعد» و«نجحت» نعم، و«رُفضت» و«لا مزوّد» لا. */
        val persistable: Boolean get() = result == Result.APPLIED || result == Result.DEFERRED
    }

    /**
     * مصدر الزمن للمهل — يُستیبد في الاختبار فلا ينتظر أحدٌ دقيقةً حقيقية.
     */
    fun interface Clock {
        fun nowMs(): Long
    }

    /**
     * محرّك التسوية بحالته: مهلة إعادة المحاولة، وآخر قرار أُرسل إلى محوّل المصنّع.
     *
     * ووُجد كصنف لا كتجمّع حالات ساكن في `TouchBoostViewModel` لسببين: (١) الحالة صارت **قابلة
     * للاختبار** بزمن مُستبد ومُوجّه وهمي، (٢) وصار للمقبض **مالك واحد** يكتب: شاشة اللمس، ولوحة
     * الألعاب، و`AppMonitor` ينادون هذا الكائن نفسه — لا ثلاث نسخ من المنطق نفسه.
     *
     * والحالة `@Volatile` بلا قفل عن قصد: المنادون من خيوط مختلفة (دورة المراقبة، وشاشة، ولوحة)،
     * وأسوأ ما يُنتج التسابق كتابةٌ مكرّرة بنفس القيمة — وهي بالضبط ما تخفّفه القراءة قبل الكتابة.
     * وقفلٌ ينتظر نداء جذر لثوانٍ كان سيوقف الخيط الآخر على بلا شيء.
     */
    class Reconciler(
        private val access: Access = SystemAccess,
        private val vendor: () -> VendorBridge = { vendorBridge },
        private val clock: Clock = Clock { System.currentTimeMillis() },
    ) {
        @Volatile private var retryNotBeforeMs = 0L
        @Volatile private var lastVendorRequest: Boolean? = null

        fun reconcile(enabled: Boolean): Attempt {
            val discovery = discover(access, vendor())
            val provider = discovery.boostProvider
            if (provider == null) return reconcileWithoutNode(discovery, enabled)

            val wanted = value(provider, enabled)
            val live = access.read(provider.path)
            if (live != null && WriteVerification.compare(wanted, live) == WriteVerification.Outcome.MATCHED) {
                retryNotBeforeMs = 0L
                return Attempt(Result.DEFERRED, Channel.SYSFS, provider.path, "node-holds-request", live)
            }
            val now = clock.nowMs()
            if (now < retryNotBeforeMs) {
                return Attempt(Result.DEFERRED, Channel.SYSFS, provider.path, "write-backoff", live)
            }
            val outcome = runCatching { access.writeVerified(provider.path, wanted) }
                .getOrDefault(WriteVerification.Outcome.WRITE_FAILED)
            // النجاح يُصفّر المهلة، والفشل يُثبّتها دقيقة كاملة: العقدة التي لا تُكتب كانت تكتب
            // سطرًا كل دورة (٥٠٠ م.ث) فتدفن العطل الذي جاء الملف ليشرحه.
            if (outcome == WriteVerification.Outcome.MATCHED) {
                retryNotBeforeMs = 0L
                // والقيمة الحيّة معلومة من الحكم نفسه: `matched` تعني أنها تساوي المطلوب
                // (عدديًّا على الأقل)، فلا تُقرأ العقدة مرّة ثانية لمجرد سطر سجل.
                return Attempt(Result.APPLIED, Channel.SYSFS, provider.path, "write-verified", wanted)
            }
            retryNotBeforeMs = now + RETRY_BACKOFF_MS
            // أمّا الفشل فتُقرأ العقدة بعده مرّة واحدة: هنا **تلزم** القيمة الحيّة، فهي الفرق بين
            // «العقدة صفرت نفسها» و«العقدة غير قابلة للكتابة أصلًا» — وهو أول سؤال يطرحه المُصلح.
            return Attempt(
                result = Result.REFUSED,
                channel = Channel.SYSFS,
                path = provider.path,
                reason = WriteVerification.verdictWord(outcome),
                live = access.read(provider.path),
            )
        }

        /**
         * ولا مزوّد: يُقال أيّ القنوات رُفضت ولِمَ — ولا تُنادى كتابةٌ لا يُعرف معناها.
         */
        private fun reconcileWithoutNode(discovery: Discovery, enabled: Boolean): Attempt {
            if (!discovery.vendorTransportPresent) {
                return Attempt(Result.UNAVAILABLE, Channel.NONE, null, "no-touch-provider")
            }
            if (!discovery.vendorHasWritePath) {
                // النقل معلن والنمط غير مثبَت: هذا **رفض بالعقد** لا فشل، ولا تُنادى الواجهة أصلًا
                // (انظر [VendorBridge.hasWritePath]).
                return Attempt(Result.UNAVAILABLE, Channel.VENDOR, null, VENDOR_CONTRACT_REASON)
            }
            if (lastVendorRequest == enabled) {
                return Attempt(Result.DEFERRED, Channel.VENDOR, null, "vendor-already-asked")
            }
            return when (runCatching { vendor().apply(enabled) }.getOrNull()) {
                null -> Attempt(Result.UNAVAILABLE, Channel.VENDOR, null, VENDOR_CONTRACT_REASON)
                true -> {
                    lastVendorRequest = enabled
                    Attempt(Result.APPLIED, Channel.VENDOR, null, "vendor-applied")
                }
                false -> Attempt(Result.REFUSED, Channel.VENDOR, null, "vendor-refused")
            }
        }

        /** تفضيل النقر المزدوج — مقبض مستقل بخطّه: لا محوّل مصنّع، وعقدة واحدة معلومة القيمتين. */
        fun reconcileDoubleTap(enabled: Boolean, discovery: Discovery = discover(access, vendor())): Boolean {
            val node = discovery.doubleTap ?: return false
            if (nodeHolds(node, enabled)) return true
            val outcome = runCatching { access.writeVerified(node.path, value(node, enabled)) }
                .getOrDefault(WriteVerification.Outcome.WRITE_FAILED)
            return outcome == WriteVerification.Outcome.MATCHED
        }

        /**
         * هل تحمل العقدة المطلوب **الآن**؟ — سؤال قراءة لا كتابة.
         *
         * والمقارنة تمرّ بـ[WriteVerification.compare] لا بمقارنة نصّية: تُقبل التكافؤ العددي
         * (`1` و`01`) وتُوحَّد الفراغات، وإلا قرأنا «مختلفة» في عقدة سليمة فكتبنا بلا داعٍ.
         */
        private fun nodeHolds(node: Node, enabled: Boolean): Boolean {
            val live = access.read(node.path) ?: return false
            return WriteVerification.compare(value(node, enabled), live) == WriteVerification.Outcome.MATCHED
        }

        private fun value(node: Node, enabled: Boolean): String =
            if (enabled) node.onValue else node.offValue
    }

    /** محرّك التطبيق: على العتاد الحقيقي، وبالجسر المركَّب. */
    val reconciler: Reconciler = Reconciler()

    /**
     * يسوّي مقبض تعزيز اللمس ويعيد **وصف** ما جرى.
     *
     * والنتيجة تُعاد ولا تُسجَّل هنا: من ينادي كل ٥٠٠ م.ث هو من يعرف متى يتغيّر القرار، فالسجل
     * يُكتب عند التغيّر وحده (انظر `AppMonitor`)، ولا يتحوّل ملف السجل إلى صدى.
     */
    fun reconcile(enabled: Boolean): Attempt = reconciler.reconcile(enabled)

    /**
     * زمن الانتظار بعد محاولة فاشلة: العقدة التي لا تُكتب لا تُعاد محاولتها كل ٥٠٠ م.ث.
     * والحدّ الأعلى معلَن، ويُصفَّر عند أول نجاح — فلا رجوع تدريجيًّا.
     */
    const val RETRY_BACKOFF_MS = 60_000L

    /** رمز رفض محوّل المصنّع: النقل معلن، ومعنى أرقام الأوضاع غير مثبَت. */
    const val VENDOR_CONTRACT_REASON = "vendor-mode-contract-unproven"

    /** وسم دليل في البصمة: خدمة اللمس معلنة على هذا الروم. */
    const val VENDOR_TRANSPORT_EVIDENCE = "vendor-touch-transport-declared"
}
