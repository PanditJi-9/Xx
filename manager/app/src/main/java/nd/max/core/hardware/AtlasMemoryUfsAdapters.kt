/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import nd.max.core.atlas.AtlasControlTarget
import nd.max.core.atlas.AtlasControlTransport
import nd.max.core.atlas.AtlasRouteCandidate
import nd.max.core.atlas.AtlasRouteEvidence
import nd.max.core.atlas.AtlasSafetyPolicy
import nd.max.core.atlas.AtlasSafetyVerdict

/**
 * مُلاءِمو **تردّد الذاكرة والتخزين** — كيف يعمل مقبض `devfreq` على *هذا* الجهاز؟
 *
 * لماذا ملفٌّ مستقلّ
 * ----------------
 * `AtlasAdapters.kt` يحمل السجلّ والعقد العامّ (Context · Request · Registry) ومُلاءِمي CPU/GPU،
 * وهذا الملفّ يحمل **طريقة الذاكرة وUFS** وحدها: ثقبَ المعاملة، وعقود الحكم، ومُلاءِمَين
 * (هدف `MEMORY` وهدف `STORAGE`). فإضافة جهاز أو كيرنل جديد لا تلمس القلب — وهذا هو شرط المالك
 * الحرفيّ («يمكن إضافة دعم لجهاز أو Kernel جديد دون إعادة كتابة قلب Max Atlas»).
 *
 * والأربع التي لا تُخترق
 * ---------------------
 * 1. **المُلاءِم لا يكتب.** يبني `HardwareRepairRequest` بعقود إغلاق تُنفَّذ لاحقًا عبر
 *    `AtlasRepairPort` ← `HardwareRepairExecutor` ← المُحكِّم — الحدّ المُتحقَّق الوحيد.
 * 2. **الممنوع يُسقَط قبل البناء** بقاعدة [AtlasSafetyPolicy] وبرمز `never-touch:<rule>`.
 * 3. **ما لم يُقس لا يُدَّعى:** `unitProven` و`baselineReadable` و`rollbackProven` تُشتقّ من
 *    قياس هذه اللحظة (سلّمٌ مُعلن · حدّان مقروءان · وصول مُتحقَّق)، لا تُكتب `true`.
 * 4. **الفشل مغلق:** `reviewed` تأتي من الكتالوج المُراجَع، وقيمتها الافتراضية `false`، فلا
 *    يُبنى تشغيلٌ لعقدة لم يراجعها أحد (المخطِّط يردّها `ROUTE_NOT_REVIEWED`).
 *
 * والحدّ المُعلن: الكتابة تعمل على `min_freq`/`max_freq`/`governor` — وهي الواجهة التي يعرضها
 * المالك المرجعيّ (Rodin) لـ«Memory DVFS / UFS». وما لا يُعلنه الكيرنل (ثابتات توقيت UFS،
 * وعتبات QoS، وضبط نوافذ العنق) **لا يُخترع** ولا يُكتب: لا عقدة اسمها لا نعرفه.
 */

/** ثقب معاملة تردّد `devfreq`: خط الأساس، والكتابة، والاسترجاع. قرين ثقبَي CPU/GPU. */
interface AtlasDevfreqCeilingAccess {

    /** هل المعاملة المتحقّقة قابلة للمحاولة الآن؟ إشارة **تُقاس**، لا تُفترض. */
    val privileged: Boolean

    /** خط الأساس المرمَّز (`min:max|governor`)؛ `null` = لا قياس. */
    fun readBaseline(devicePath: String): String?

    /** كتابة المدى المرمَّز عبر الكاتب المُتحقَّق القائم؛ `false` = لم تُثبَت الكتابة. */
    fun writeRange(devicePath: String, encoded: String): Boolean

    /** استرجاع خط أساس مقروء سابقًا؛ `false` = لم يُثبَت الاسترجاع. */
    fun restoreBaseline(devicePath: String, encoded: String): Boolean
}

/**
 * المنفِّذ الإنتاجي — **لا كاتب ثانٍ**: كل كتابة تفوّض إلى
 * [MemoryUfsHardwareBackend.applyValidated]، وهي التي تكتب وتقرأ وتُرجِع عند الفشل. والاختلاف
 * عن بقية المقابض في اختيار الطريقة والحكم، لا في مسار الكتابة.
 */
object SystemDevfreqCeilingAccess : AtlasDevfreqCeilingAccess {

    override val privileged: Boolean
        get() = MemoryUfsHardwareBackend.discover().devices.any { it.rangeWritable }

    override fun readBaseline(devicePath: String): String? =
        MemoryUfsHardwareBackend.refresh(devicePath)?.let(MemoryUfsHardwareBackend::encodeBaseline)

    override fun writeRange(devicePath: String, encoded: String): Boolean {
        val request = DevfreqEncoding.parseRequest(encoded) ?: return false
        val device = MemoryUfsHardwareBackend.refresh(devicePath) ?: return false
        return MemoryUfsHardwareBackend.applyValidated(device, request).verified
    }

    override fun restoreBaseline(devicePath: String, encoded: String): Boolean {
        val baseline = DevfreqEncoding.parseBaseline(devicePath, encoded) ?: return false
        return MemoryUfsHardwareBackend.restoreBaseline(baseline)
    }
}

/**
 * شكل الترميز الوحيد: `min:max|governor`.
 *
 * وموضعه واحد لأن **المعنى يجب أن يكون واحدًا**: المُلاءِم يبني الطلب، والمُنفِّذ يفكّه، والحكم
 * يقارنه — وثلاث قراءات لصيغة واحدة بلا مُفكِّك واحد هي الطريق المعتاد إلى «كُتب شيء وقيس غيره».
 */
object DevfreqEncoding {

    private const val FIELD_SEPARATOR = "|"
    private const val UNTOUCHED = "-"

    /**
     * صيغة اسم الحاكم: يبدأ بحرف أو رقم، ثم حروف/أرقام/`.`/`_`/`-` فقط.
     *
     * والتحقّق مقصود: ترميزٌ مشوّه (`--` مثلًا) لا يجوز أن يصير «حاكمًا مطلوبًا» فيمرّ إلى
     * مسار كتابة — الفشل مغلق، والترميز الخطأ يُرفض بدل أن يُترجَم إلى نيّة.
     */
    private val GOVERNOR_TOKEN = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")

    /** الرقم في أي خانة؛ `null` حين لا رقم — ولا «صفر» بديلًا عن غياب قياس. */
    private fun number(field: String): Long? = field.trim().takeIf(String::isNotEmpty)?.toLongOrNull()

    /** الحاكم إن كان الترميز صالحًا؛ و`-` (غير مطلوب) و`--` (مشوّه) كلاهما `null`. */
    private fun governorToken(field: String): String? =
        field.trim().takeIf { it != UNTOUCHED && GOVERNOR_TOKEN.matches(it) }

    private fun fields(encoded: String): Triple<String, String, String> {
        val head = encoded.substringBefore(FIELD_SEPARATOR)
        val governor = encoded.substringAfter(FIELD_SEPARATOR, UNTOUCHED)
        val pair = head.split(":", limit = 2)
        return Triple(pair.getOrNull(0) ?: "", pair.getOrNull(1) ?: "", governor)
    }

    /** `min:max|gov` ← طلب. صيغةٌ بلا أي رقم وبلا حاكم = `null` (لا طلب مُخترع من فراغ). */
    fun parseRequest(encoded: String): MemoryUfsHardwareBackend.Request? {
        val (minField, maxField, governorField) = fields(encoded)
        val min = number(minField)
        val max = number(maxField)
        if (min != null && max != null && min > max) return null
        val governor = governorToken(governorField)
        if (min == null && max == null && governor == null) return null
        return MemoryUfsHardwareBackend.Request(minFreq = min, maxFreq = max, governor = governor)
    }

    /** `min:max|gov` ← خط أساس. مسار غير صالح = `null` (لا استرجاع إلى مسار مُخترع). */
    fun parseBaseline(devicePath: String, encoded: String): MemoryUfsHardwareBackend.Baseline? {
        if (!devicePath.startsWith("/")) return null
        val (minField, maxField, governorField) = fields(encoded)
        return MemoryUfsHardwareBackend.Baseline(
            devicePath = devicePath,
            minFreq = number(minField),
            maxFreq = number(maxField),
            governor = governorToken(governorField),
        )
    }

    /** سقف القراءة (الحقل الثاني) — و`null` حين لا رقم. */
    fun ceiling(encoded: String?): Long? = encoded?.let { number(fields(it).second) }

    /** أرضية القراءة (الحقل الأوّل) — و`null` حين لا رقم. */
    fun floor(encoded: String?): Long? = encoded?.let { number(fields(it).first) }

    /** الحاكم في القراءة؛ `null` حين لم يُقرأ أو لم يُطلب أو كان الترميز مشوّهًا. */
    fun governor(encoded: String?): String? = encoded?.let { governorToken(fields(it).third) }
}

/**
 * عقود حكم `devfreq` — كل دالة **خالصة** ولا تقرأ عقدة ولا تكتب.
 *
 * والحكمان مفصولان كما في كل مقبض في هذا المشروع:
 *
 * | الحكم | سؤاله | يُستعمل في |
 * | --- | --- | --- |
 * | [contained] | «هل الحالة المقروءة **مقبولة** فلا يُسترجع خط الأساس؟» | تحقق المعاملة ونافذة التأكيد |
 * | [reached] | «هل القراءة الحيّة **دليل** على أن طلبنا نُفِّذ؟» | قرار **الكتابة** في المُحكِّم |
 */
object DevfreqCeilingContracts {

    /**
     * «لا تتجاوز السقف، ولا تنزل تحت الأرضية» — والحدود **كلها اختيارية**: خانة لم تُطلب لا
     * تُحكم. ومدى بلا أي حدّ مقروء لا يُثبت شيئًا (لا تلبية بلا قياس).
     */
    fun contained(desired: String?, actual: String?): Boolean {
        val wantCeiling = DevfreqEncoding.ceiling(desired) ?: return exactFallback(desired, actual)
        val liveCeiling = DevfreqEncoding.ceiling(actual) ?: return false
        if (liveCeiling > wantCeiling) return false
        val wantFloor = DevfreqEncoding.floor(desired)
        val liveFloor = DevfreqEncoding.floor(actual)
        if (wantFloor != null && liveFloor != null && liveFloor < wantFloor) return false
        val wantGovernor = DevfreqEncoding.governor(desired)
        if (wantGovernor != null && DevfreqEncoding.governor(actual) != wantGovernor) return false
        return true
    }

    /**
     * هل **بلغت** القراءةُ ما طلبناه؟ — سؤال الكتابة لا سؤال التلبية.
     *
     * والاستثناء المقصود: طلبٌ فيه حاكمٌ وتحقّق سقفُه لا يُعتبر «منفَّذًا»، لأن تخطّي الكتابة
     * حينها يترك الحاكم الذي طلبه المستخدم بلا كتابة أبدًا — والعطب مصرّحٌ به في `HardwareVerification`:
     * حكمُ السقف متسامح، فقيمةٌ كتبناها سابقًا تُقرأ «مُلبّاة» فلا تُكتب قيمة أعلى.
     */
    fun reached(desired: String?, actual: String?): Boolean {
        val wantGovernor = DevfreqEncoding.governor(desired)
        if (wantGovernor != null && DevfreqEncoding.governor(actual) != wantGovernor) return false
        val want = DevfreqEncoding.ceiling(desired) ?: return true
        val live = DevfreqEncoding.ceiling(actual) ?: return true
        return live >= want
    }

    private fun exactFallback(desired: String?, actual: String?): Boolean {
        val wantGovernor = DevfreqEncoding.governor(desired) ?: return false
        return DevfreqEncoding.governor(actual) == wantGovernor
    }
}

/**
 * المُلاءِم المشترك: طريقةٌ واحدة لهدفٍ واحد (`MEMORY` أو `STORAGE`) ولنفس العُقَد — والاختلاف
 * بين الاثنين في **الهدف والمفتاح والمسارات**، وليس في المنطق. وتكرار المنطق مرّتين كان يعني
 * قاعدة تُصلح في أحدهما وتُنسى في الآخر.
 *
 * **وهو عامّ (public) لا داخليًّا**: الفئة `open` حتى يمدّها مُلاءِم جهاز/كيرنل جديد بمُلاءِم
 * خاصّ به (نقطة التوسعة التي طلبها المالك)، و`internal` كانت تُظهر خطأ تجميع حقيقيًّا:
 * «public subclass exposes its internal supertype» — أي صنفٌ عامّ لا يجوز أن يرث صنفًا مخفيًّا،
 * لأن المستدعي قد يرى الابن ولا يرى أصله. والكشف هنا **مقصود** لا مصادفة.
 */
open class DevfreqCeilingAdapter(
    protected val kind: MemoryUfsHardwareBackend.Kind,
    override val id: String,
) : AtlasControlAdapter {

    override val target: AtlasControlTarget = when (kind) {
        MemoryUfsHardwareBackend.Kind.MEMORY -> AtlasControlTarget.MEMORY
        MemoryUfsHardwareBackend.Kind.STORAGE -> AtlasControlTarget.STORAGE
    }

    override val transport: AtlasControlTransport = AtlasControlTransport.ARBITER_SYSFS

    protected fun devices(context: AtlasAdapterContext): List<MemoryUfsHardwareBackend.Device> = when (kind) {
        MemoryUfsHardwareBackend.Kind.MEMORY -> context.memoryDevices
        MemoryUfsHardwareBackend.Kind.STORAGE -> context.storageDevices
    }

    override fun assess(context: AtlasAdapterContext, request: AtlasControlRequest?): AtlasAdapterAssessment {
        val devices = devices(context)
        if (devices.isEmpty()) {
            // ولا يُخلط الرمزان: «لم يُرصد جهاز» فراغٌ مقيس، و«رُصد ولم يُصنَّف» نقصٌ في التصنيف
            // يملك المستخدم إغلاقه بالتعليم — والفرق بينهما هو الفرق بين طلب تحديثٍ وطلب نقرة.
            // وثالثٌ يُضاف: «رُصد ولم يُقرأ» نقصُ قياس (عبورُ مجلّد أب محجوب مثلًا)، وفعلُه ليس
            // التعليم — فدمجُه مع الثاني يرسل المستخدم ليعلّم عقدةً لم يُقرأ منها حرف.
            val reason = when {
                context.unclassifiedDevfreq.isNotEmpty() -> SKIP_UNCLASSIFIED
                context.unreadableDevfreq.isNotEmpty() -> SKIP_UNREADABLE
                else -> SKIP_NO_DEVICE
            }
            return AtlasAdapterAssessment.NotApplicable(reason)
        }
        // والملاءمة تُقاس بما **يبنيه هذا المُلاءِم**: مسار مدى (`min_freq`/`max_freq`) وحده — فهو
        // لا يكتب حاكمًا أبدًا. وقابلية كتابة الحاكم كانت تُنتج «مُنطبق» لجهاز لا مسار له، أي
        // هدفًا مؤهّلًا في الخريطة لا يكتب شيئًا — وأسوأ من فجوة مُعلنة هي إعلان قدرة لا فعل لها.
        // (كشفه تشغيلُ الاختبارات: شهادة الملاءمة نفسها كانت تطبع `writable=0` وهي تُعلن Applicable.)
        val writable = devices.any { it.rangeWritable }
        if (!writable) return AtlasAdapterAssessment.NotApplicable(SKIP_NO_WRITABLE_DEVICE)
        return AtlasAdapterAssessment.Applicable(
            id,
            "devfreq=${devices.size}; kind=${kind.name.lowercase()}; writable=${devices.count { it.rangeWritable }}",
        )
    }

    override fun plan(context: AtlasAdapterContext, request: AtlasControlRequest): AtlasAdapterPlan {
        val ceilings = ceilingsOf(request)
            ?: return AtlasAdapterPlan(id, emptyList(), listOf(KEY_ANY to SKIP_WRONG_REQUEST))
        val access = context.devfreqAccess
            ?: return AtlasAdapterPlan(id, emptyList(), listOf(KEY_ANY to SKIP_NO_SEAM))
        return buildPlan(
            context = context,
            access = access,
            owner = ceilings.owner,
            token = ceilings.token,
            request = { device -> ceilings.ceilingByKnob[device.name] to ceilings.floorByKnob[device.name] },
            reviewed = { device -> ceilings.reviewed(device.name) },
        )
    }

    override fun probe(context: AtlasAdapterContext): AtlasAdapterPlan {
        val access = context.devfreqAccess
            ?: return AtlasAdapterPlan(id, emptyList(), listOf(KEY_ANY to SKIP_NO_SEAM))
        // المسار التمثيلي: السقف عند أقصى ما أثبتته النواة، ولا أرضية — يُبنى ويُحكم عليه ولا يُكتب.
        return buildPlan(
            context = context,
            access = access,
            owner = ControlOwnership.Owner.MAX_AI,
            token = PROBE_TOKEN,
            request = { device -> device.configurableMaxFreq to null },
            // ومراجعة الشكل تُقرأ من المفتاح القياسي كما في CPU: حكمٌ على **شكل** المعاملة
            // (خط أساس من قراءة حيّة · كتابة عبر الكاتب المُتحقَّق · قراءة مرتجعة · نافذة تأكيد ·
            // استرجاع عند الانحراف) لا على جهاز. ومسار الذاكرة هو هذا الشكل نفسه — ولذلك لا
            // يُدَّعى لجهاز بعينه شيءٌ لم يُقَس فيه.
            reviewed = { device ->
                AtlasDiscoveredControl.isReviewedControlRoute(
                    HardwareControlKey.devfreqFrequency(kind, device.name),
                )
            },
        )
    }

    private fun ceilingsOf(request: AtlasControlRequest): CeilingsView? = when {
        kind == MemoryUfsHardwareBackend.Kind.MEMORY && request is AtlasControlRequest.MemoryCeilings ->
            CeilingsView(request.ceilingByKnob, request.floorByKnob, request.owner, request.token, request.reviewed)

        kind == MemoryUfsHardwareBackend.Kind.STORAGE && request is AtlasControlRequest.StorageCeilings ->
            CeilingsView(request.ceilingByKnob, request.floorByKnob, request.owner, request.token, request.reviewed)

        else -> null
    }

    private data class CeilingsView(
        val ceilingByKnob: Map<String, Long?>,
        val floorByKnob: Map<String, Long?>,
        val owner: ControlOwnership.Owner,
        val token: String,
        val reviewed: (String) -> Boolean,
    )

    private fun buildPlan(
        context: AtlasAdapterContext,
        access: AtlasDevfreqCeilingAccess,
        owner: ControlOwnership.Owner,
        token: String,
        request: (MemoryUfsHardwareBackend.Device) -> Pair<Long?, Long?>,
        reviewed: (MemoryUfsHardwareBackend.Device) -> Boolean,
    ): AtlasAdapterPlan {
        val bindings = mutableListOf<AtlasRouteBinding>()
        val skipped = mutableListOf<Pair<String, String>>()
        // يُقاس **مرّة** لكل خطة: الثقب الإنتاجي يعدّد `/sys/class/devfreq` كاملًا في كل نداء،
        // وسؤالُه مرّةً لكل جهاز يضاعف القراءة بعدد العقد بلا معلومة جديدة.
        val privileged = access.privileged

        devices(context).forEach { device ->
            val key = HardwareControlKey.devfreqFrequency(kind, device.name)
            // قواعد عدم اللمس أوّلًا — باسم الجهاز ومساره معًا (فشل مغلق: أيّهما وقع منع).
            val safety = AtlasSafetyPolicy.verdictFor(name = device.name, path = device.path)
            if (safety is AtlasSafetyVerdict.Denied) {
                skipped += key to "never-touch:${safety.ruleId}"
                return@forEach
            }
            val (requested, requestedFloor) = request(device)
            if (requested == null || requested <= 0L) {
                skipped += key to SKIP_NO_REQUEST
                return@forEach
            }
            if (!device.unitTrusted) {
                skipped += key to SKIP_UNIT_AMBIGUOUS
                return@forEach
            }
            if (device.minWritable.not() || device.maxWritable.not()) {
                // عقدة تُقرأ ولا تُكتب: تُنسحب برمزها ولا يُبنى لها مسار كتابة أصلًا.
                skipped += key to SKIP_NOT_WRITABLE
                return@forEach
            }
            val ladder = device.ladder
            val snappedMax = ladder.lastOrNull { it <= requested }
                ?: run {
                    skipped += key to SKIP_NO_PROVEN_RANGE
                    return@forEach
                }
            // الأرضية تُقصّ إلى السقف المطلوب: مدى بـ`min > max` ترفضه النواة أصلًا، وإسقاطه هنا
            // يجعل الرفض رمزًا مُعلنًا لا فشلًا صامتًا في السائق.
            val snappedMin = requestedFloor
                ?.let { floor -> ladder.lastOrNull { it <= minOf(floor, snappedMax) } }
                ?.takeIf { it in ladder }

            val baseline = runCatching { access.readBaseline(device.path) }.getOrNull()
            val desired = MemoryUfsHardwareBackend.encodeRequest(
                MemoryUfsHardwareBackend.Request(minFreq = snappedMin, maxFreq = snappedMax),
            )
            val isReviewed = runCatching { reviewed(device) }.getOrDefault(false)
            val routeId = HardwareRepairExecutor.labelFor(key)
            val liveDevice = MemoryUfsHardwareBackend.refresh(device.path) ?: device

            bindings += AtlasRouteBinding(
                candidate = AtlasRouteCandidate(
                    id = routeId,
                    priority = 0,
                    evidence = evidenceFor(liveDevice, baseline, isReviewed, privileged),
                ),
                request = HardwareRepairRequest(
                    routeId = routeId,
                    key = key,
                    owner = owner,
                    token = token,
                    desired = desired,
                    // الطلب يُفكّ من **نصّه** لا من صورة محفوظة: لو كُتب حقلٌ لاحقًا في المخطط
                    // لَما انزلق الطلب المُنفَّذ عن الطلب المفحوص (وهو سبب مُعشّش في كل كاتب).
                    apply = { value -> access.writeRange(device.path, value) },
                    read = { access.readBaseline(device.path) },
                    restore = { value -> access.restoreBaseline(device.path, value) },
                    baseline = baseline,
                    verify = DevfreqCeilingContracts::contained,
                    realized = DevfreqCeilingContracts::reached,
                    stabilitySamples = 3,
                    stabilityIntervalMs = 40L,
                ),
            )
        }

        return AtlasAdapterPlan(id, bindings, skipped)
    }

    private fun evidenceFor(
        live: MemoryUfsHardwareBackend.Device,
        baseline: String?,
        reviewed: Boolean,
        privileged: Boolean,
    ): AtlasRouteEvidence = AtlasRouteEvidence(
        providerId = PROVIDER_ID,
        transport = AtlasControlTransport.ARBITER_SYSFS,
        target = target,
        // «مقروء» = عقدة الساعة أو عقدة السقف أجابت في القياس now.
        readable = live.currentFreq != null || live.maxFreq != null,
        privilegeAvailable = privileged,
        // الوحدة مُثبتة فقط حين نُشر سلّمٌ من درجتين واتّفق عليه الاستنتاج كلّه.
        unitProven = live.ladder.size >= 2 && live.unitTrusted,
        baselineReadable = DevfreqEncoding.ceiling(baseline) != null,
        rollbackProven = DevfreqEncoding.ceiling(baseline) != null && privileged,
        reviewed = reviewed,
        reason = buildString {
            append("devfreq; kind=").append(kind.name.lowercase())
            append("; family=").append(live.family.name.lowercase())
            append("; ladder=").append(live.ladder.size)
            append("; clock=").append(live.currentFreq ?: "unread")
            append("; baseline=").append(if (baseline != null) "read" else "unread")
            append(if (reviewed) "; reviewed" else "; not-reviewed")
        },
    )

    internal companion object {
        const val KEY_ANY: String = "devfreq-ceiling"
        const val SKIP_NO_DEVICE: String = "no-devfreq-device-seen"

        /**
         * عُقَد `devfreq` قُرئت ولم تُصنَّف — تُعلَّم من شاشة الذاكرة وUFS ([HardwareDeviceOverlay]).
         * والتعريف في [MemoryUfsHardwareBackend.UNCLASSIFIED_GAP_CODE] وحده: رمز واحد لنقص واحد،
         * يُقرأ في المُلاءِم وفي خريطة أطلس معًا.
         */
        const val SKIP_UNCLASSIFIED: String = MemoryUfsHardwareBackend.UNCLASSIFIED_GAP_CODE

        /** ورمزُ نقص القراءة من موضعه الواحد أيضًا — رمزٌ واحد لنقصٍ واحد. */
        const val SKIP_UNREADABLE: String = MemoryUfsHardwareBackend.UNREADABLE_GAP_CODE
        const val SKIP_NO_WRITABLE_DEVICE: String = "devfreq-range-not-writable"
        const val SKIP_WRONG_REQUEST: String = "wrong-request-shape"
        const val SKIP_NO_SEAM: String = "no-write-seam-available"
        const val SKIP_NO_REQUEST: String = "no-requested-value"
        const val SKIP_NO_PROVEN_RANGE: String = "no-proven-range"
        const val SKIP_UNIT_AMBIGUOUS: String = "frequency-unit-unproven"
        const val SKIP_NOT_WRITABLE: String = "devfreq-range-not-writable"
        const val PROVIDER_ID: String = "memory-ufs-devfreq"

        /** معرّف التمثيل — لا ملكية فيه ولا يُنفَّذ أبدًا. */
        const val PROBE_TOKEN: String = "atlas-map-probe"
    }

    init {
        require(id.isNotBlank()) { "an adapter needs an id" }
    }
}

/** مُلاءِم عقدة الذاكرة (DVFS للذاكرة/عرض النطاق). */
class MemoryDevfreqCeilingAdapter : DevfreqCeilingAdapter(
    kind = MemoryUfsHardwareBackend.Kind.MEMORY,
    id = "memory-devfreq-ceiling",
)

/** مُلاءِم عقدة التخزين (UFS/eMMC/NVMe). */
class UfsDevfreqCeilingAdapter : DevfreqCeilingAdapter(
    kind = MemoryUfsHardwareBackend.Kind.STORAGE,
    id = "ufs-devfreq-ceiling",
)
