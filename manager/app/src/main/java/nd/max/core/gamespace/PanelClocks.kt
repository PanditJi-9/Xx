package nd.max.core.gamespace

import nd.max.core.hardware.GpuFrequencyWindow
import nd.max.core.hardware.GpuHardwareBackend
import nd.max.core.hardware.RootFileAccess
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import nd.max.ui.util.CpuTopologyUtil
import nd.max.ui.util.MtkUtils

/** ترددات حيّة للوحة اللعبة: نفس عقد sysfs التي تقرؤها شاشتا CPU وGPU، بالجذر. */
data class PanelClocks(
    val cpuMhz: Int? = null,
    val cpuCeilingMhz: Int? = null,
    val gpuMhz: Int? = null,
    val gpuCeilingMhz: Int? = null,
    /** حمل المعالج الكلّي %، من دلتا `/proc/stat` لهذه اللوحة وحدها. `null` قبل قراءة ثانية. */
    val cpuLoadPct: Int? = null,
    /** حمل الرسّام %، من عقدة الانشغال. `null` حين لا تُجيب العقدة. */
    val gpuLoadPct: Int? = null,
    /** أدنى تردد يعلنه العتاد للمعالج (`cpuinfo_min_freq`)، أرضيّة شريط التردد. `null` إن لم يُعلن. */
    val cpuFloorMhz: Int? = null,
    /**
     * أدنى تردد **فيزيائي** للرسّام (أول درجة في سلّم قدرة العتاد). أرضيّة شريط الرسّام. `null` إن لم يُعلن.
     * (كان الحدّ الديناميكي الأدنى، فلا يتحرّك الشريط أبدًا حين يثبّت النظام الحدّين قرب بعضهما.)
     */
    val gpuFloorMhz: Int? = null,
    /** أعلى تردد **فيزيائي** للرسّام (آخر درجة في سلّم قدرة العتاد، مثل ١٣٠٠). سقف شريط الرسّام. */
    val gpuCapabilityMaxMhz: Int? = null,
    /**
     * السقف **الديناميكي** الحيّ للمعالج: أعلى `scaling_max_freq` بين العناقيد، أي ما يعمل به الهاتف
     * فعلًا الآن. هو الرقم المعروض وموضع الإضاءة على الشريط. `null` إن لم يُقرأ.
     */
    val cpuDynamicMaxMhz: Int? = null
) {
    /** رقم المعالج المعروض: سقفه الديناميكي الحيّ، وإن لم يُقرأ فالتردد الجاري. */
    val cpuShownMhz: Int? get() = cpuDynamicMaxMhz ?: cpuMhz

    /** رقم الرسّام المعروض: سقفه الديناميكي الحيّ (مثل ٧٥٤ لا ١٣٠٠). */
    val gpuShownMhz: Int? get() = gpuCeilingMhz ?: gpuMhz

    /**
     * مستوى شريط المعالج 0..1: السقف الديناميكي الحيّ بين أدنى وأعلى تردد **فيزيائيين**.
     * `null` ⇒ شريط فارغ و«—».
     */
    val cpuLevel: Float? get() = PanelClockMath.level(cpuShownMhz, cpuFloorMhz, cpuCeilingMhz)

    /**
     * مستوى شريط الرسّام 0..1: السقف الديناميكي الحيّ (مثل ٧٥٤) بين أدنى وأعلى تردد **فيزيائيين**
     * (مثل ٢٦٠ ← ١٣٠٠). `null` ⇒ شريط فارغ و«—».
     */
    val gpuLevel: Float? get() = PanelClockMath.level(gpuShownMhz, gpuFloorMhz, gpuCapabilityMaxMhz)
}

/**
 * قارئ الترددات الحيّة.
 *
 * **عطبان مقيسان في الإصدار السابق جعلا العدّادين «—» بلا أمل:**
 *  ١. قائمة سياسات المعالج كانت تُخزَّن **حتى لو فارغة** — فإن سبقت القراءة الأولى جاهزيّة قارئ الجذر
 *     (وهي تسبقه حين تُفتح اللوحة مع إقلاع اللعبة) بقيت فارغة إلى أن تموت العملية.
 *  ٢. `GpuHardwareBackend.selection()` (اكتشاف كامل للأجهزة) كانت تُعاد كل قراءة ما دام الجهاز غير موجود.
 * الآن لا يُخزَّن الفارغ، ويُعاد المحاولة كل [RETRY_MS] لا كل قراءة، وللمعالج والرسّام سلسلة بدائل
 * مباشرة حين يخفق المسار الأوّل. وأيّ حقل لا يجيبه العتاد يبقى `null` — لا قيمة مختلقة.
 */
object PanelClockReader {
    private const val RETRY_MS = 4_000L

    /** الجهاز المكتشف يُحفظ طويلًا: سلّمه ووحدته ثابتان، والحيّ منه ثلاث عقد تُقرأ كل نبضة. */
    private const val GPU_DEVICE_TTL_MS = 300_000L

    /** إعادة محاولة اكتشاف الرسّام الفاشل: أسرع من [RETRY_MS] لأن المحاولة في خيط خلفي ولا تحجب شيئًا. */
    private const val GPU_RETRY_MS = 2_000L

    /** عقدة الحمل الغائبة تُعاد تجربتها كل هذه المدة لا كل نبضة (كل غياب كان رحلة صدفة كاملة). */
    private const val LOAD_PROBE_MS = 10_000L

    private var policies: List<String> = emptyList()
    private var policiesAtMs = 0L
    private var cpuCeiling: Int? = null
    private var cpuFloor: Int? = null
    private var cpuBoundsAtMs = 0L
    private var gpuCapabilityMaxFallback: Int? = null
    private var gpuCapabilityMinFallback: Int? = null
    private var gpuCapabilityAtMs = 0L

    private var gpuFallbackCeiling: Int? = null
    private var gpuFallbackAtMs = 0L
    @Volatile private var gpuDevice: GpuHardwareBackend.Device? = null
    @Volatile private var gpuDeviceAtMs = 0L
    @Volatile private var gpuProbeAtMs = 0L
    @Volatile private var gpuProbeFailures = 0
    private val gpuProbing = AtomicBoolean(false)
    private var gpuLoadNode: String? = null
    private var gpuLoadProbeAtMs = 0L
    private val LEADING = Regex("\\d+")

    /** مرشّحات مباشرة حين لا يجد المستكشف جهازًا: عقد kgsl (كوالكوم) وMali. تُجرَّب بالترتيب، وغيابها ليس خطأ. */
    private val GPU_CUR_NODES = listOf(
        "/sys/class/kgsl/kgsl-3d0/gpuclk",
        "/sys/kernel/gpu/gpu_clock",
        "/sys/class/misc/mali0/device/clock"
    )
    private val GPU_MAX_NODES = listOf(
        "/sys/class/kgsl/kgsl-3d0/max_gpuclk",
        "/sys/kernel/gpu/gpu_max_clock"
    )

    /** يُستدعى من خيط IO. أي حقل لا يجيبه العتاد يبقى null — لا قيمة مختلقة. */
    @Synchronized
    fun read(onPartial: ((PanelClocks) -> Unit)? = null): PanelClocks {
        val now = android.os.SystemClock.elapsedRealtime()
        val paths = cpuPolicies(now)
        resolveCpuBounds(paths, now)
        val cpu = readCpuMhz(paths)
        val cpuDynamicMax = readCpuDynamicMaxMhz(paths)
        // المعالج يُنشر فور قراءته: اكتشاف الرسّام قد يطول (صدفة جذر)، فلا يُحبَس رقم المعالج خلفه.
        onPartial?.invoke(
            PanelClocks(
                cpuMhz = cpu, cpuCeilingMhz = cpuCeiling, cpuFloorMhz = cpuFloor,
                cpuDynamicMaxMhz = cpuDynamicMax, cpuLoadPct = null
            )
        )

        // الرسّام: الجهاز من الذاكرة (الاكتشاف الكامل يجري في خيط خلفي ولا يحجب هذه القراءة)،
        // والحيّ منه ثلاث عقد في نداء واحد. كانت كل نبضة تُعيد اكتشافه كاملًا مرتين (انظر [GpuLiveRead]).
        val dev = gpuDevice(now)
        val live = dev?.let { d -> runCatching { readGpuLive(d) }.getOrNull() }
        val window = dev?.let { GpuFrequencyWindow.of(it) }
        val gpu = readGpuMhz(live)
        val gpuLoad = readGpuLoad(dev, now)

        logIfBlind(now, cpu, cpuDynamicMax, gpu, live?.dynamicMaxMhz)
        return PanelClocks(
            cpuMhz = cpu, cpuCeilingMhz = cpuCeiling, gpuMhz = gpu,
            gpuCeilingMhz = live?.dynamicMaxMhz ?: gpuCeilingFallback(now),
            cpuLoadPct = PanelCpuLoad.sample(), gpuLoadPct = gpuLoad,
            // الأرضيّة والسقف الفيزيائيان (قدرة العتاد) لا الحدّان الديناميكيان: الشريط مسطرة ثابتة،
            // والإضاءة عليها تتبع السقف الديناميكي الحيّ.
            cpuFloorMhz = cpuFloor,
            gpuFloorMhz = window?.capabilityMinMhz ?: gpuCapabilityFallback(now).first,
            gpuCapabilityMaxMhz = window?.capabilityMaxMhz ?: gpuCapabilityFallback(now).second,
            cpuDynamicMaxMhz = cpuDynamicMax
        )
    }

    private var blindLoggedAtMs = 0L

    /** حين لا يُقرأ شيء إطلاقًا يُسجَّل السبب المرجّح (كل 30 ث) فيُرى في logcat بدل «—» صامتة. */
    private fun logIfBlind(now: Long, cpu: Int?, cpuMax: Int?, gpu: Int?, gpuMax: Int?) {
        if (cpu != null || cpuMax != null || gpu != null || gpuMax != null) return
        if (blindLoggedAtMs != 0L && now - blindLoggedAtMs < 30_000L) return
        blindLoggedAtMs = now
        android.util.Log.w(
            "MaxPanelClocks",
            "no clock readable: rootChannel=${nd.max.core.ipc.RootNodeChannel.isBound} " +
                "policies=${policies.size} gpuDevice=${gpuDevice?.path}"
        )
    }

    /** سياسات المعالج: الفارغ لا يُخزَّن، وتُعاد المحاولة كل [RETRY_MS]. */
    private fun cpuPolicies(now: Long): List<String> {
        if (policies.isNotEmpty()) return policies
        if (policiesAtMs != 0L && now - policiesAtMs < RETRY_MS) return emptyList()
        policiesAtMs = now
        val found = runCatching { CpuTopologyUtil.detectClusters().map { it.policyPath } }.getOrDefault(emptyList())
        if (found.isNotEmpty()) {
            policies = found
            cpuBoundsAtMs = 0L
        }
        return found
    }

    /** سقف المعالج وأرضيّته من عقد السياسات؛ وإن لم تُقرأ بعدُ تُعاد المحاولة كل [RETRY_MS]. */
    private fun resolveCpuBounds(paths: List<String>, now: Long) {
        if (cpuCeiling != null && cpuFloor != null) return
        if (cpuBoundsAtMs != 0L && now - cpuBoundsAtMs < RETRY_MS) return
        cpuBoundsAtMs = now
        if (paths.isNotEmpty()) {
            cpuCeiling = cpuCeiling ?: paths.maxOfOrNull { CpuTopologyUtil.clusterMaxFreqMhz(it) }?.takeIf { it > 0 }
            cpuFloor = cpuFloor ?: paths.map { CpuTopologyUtil.clusterMinFreqMhz(it) }.filter { it > 0 }.minOrNull()
            return
        }
        // بلا سياسات: عقد الأنوية نفسها (cpu0..cpuN).
        val cores = (0 until Runtime.getRuntime().availableProcessors().coerceIn(1, 16)).toList()
        val maxKhz = cores.mapNotNull { RootFileAccess.read("/sys/devices/system/cpu/cpu$it/cpufreq/cpuinfo_max_freq")?.trim()?.toLongOrNull() }
            .filter { it > 0L }
        val minKhz = cores.mapNotNull { RootFileAccess.read("/sys/devices/system/cpu/cpu$it/cpufreq/cpuinfo_min_freq")?.trim()?.toLongOrNull() }
            .filter { it > 0L }
        cpuCeiling = cpuCeiling ?: maxKhz.maxOrNull()?.div(1000L)?.toInt()
        cpuFloor = cpuFloor ?: minKhz.minOrNull()?.div(1000L)?.toInt()
    }

    /**
     * السقف الديناميكي الحيّ للمعالج: أعلى `scaling_max_freq` بين السياسات، في كل قراءة (لا يُخزَّن —
     * فهو يتغيّر بالحرارة والبروفايل). `null` حين لا تُقرأ عقدة، فلا يُختلق رقم.
     */
    private fun readCpuDynamicMaxMhz(paths: List<String>): Int? {
        if (paths.isEmpty()) return null
        return paths.map { CpuTopologyUtil.clusterDynamicMaxFreqMhz(it) }.filter { it > 0 }.maxOrNull()
    }

    /**
     * أدنى/أعلى تردد فيزيائي للرسّام حين لا يُحلّ الجهاز: جدول OPP ثم عقد kgsl. يُعاد حسابه كل
     * [RETRY_MS]. الأول = الأدنى، والثاني = الأعلى.
     */
    private fun gpuCapabilityFallback(now: Long): Pair<Int?, Int?> {
        if (gpuCapabilityAtMs != 0L && now - gpuCapabilityAtMs < RETRY_MS) {
            return gpuCapabilityMinFallback to gpuCapabilityMaxFallback
        }
        gpuCapabilityAtMs = now
        val opp = runCatching { MtkUtils.oppFrequenciesHz().filter { it > 0L } }.getOrDefault(emptyList())
        gpuCapabilityMinFallback = opp.minOrNull()?.div(1_000_000L)?.toInt()?.takeIf { it > 0 }
        gpuCapabilityMaxFallback = opp.maxOrNull()?.div(1_000_000L)?.toInt()?.takeIf { it > 0 }
            ?: GPU_MAX_NODES.firstNotNullOfOrNull { node ->
                rawToMhz(RootFileAccess.read(node)?.trim()?.toLongOrNull())
            }
        return gpuCapabilityMinFallback to gpuCapabilityMaxFallback
    }

    /** أعلى تردد بين العناقيد الآن (العنقود الأعلى هو ما يُعرض)، ثم بدائل عند الإخفاق. */
    private fun readCpuMhz(paths: List<String>): Int? {
        fun khz(node: String): Long? = RootFileAccess.read(node)?.trim()?.toLongOrNull()?.takeIf { it > 0L }
        val fromPolicies = paths.mapNotNull { khz("$it/scaling_cur_freq") ?: khz("$it/cpuinfo_cur_freq") }.maxOrNull()
        if (fromPolicies != null) return (fromPolicies / 1000L).toInt().takeIf { it > 0 }
        val cores = (0 until Runtime.getRuntime().availableProcessors().coerceIn(1, 16)).toList()
        return runCatching { CpuTopologyUtil.coreFreqMhz(cores).values.filterNotNull().filter { it > 0 }.maxOrNull() }.getOrNull()
    }

    /**
     * يُسخَّن اكتشاف الرسّام قبل أول نبضة (تُناديه الخدمة عند إنشائها)، فلا تبدأ اللوحة بانتظاره.
     * آمن من أي خيط: لا يقفل ولا يقرأ شيئًا بنفسه.
     */
    fun prewarm() {
        startGpuProbe(android.os.SystemClock.elapsedRealtime())
    }

    /**
     * جهاز الرسّام المحفوظ — **من الذاكرة دائمًا**. الاكتشاف الكامل كان يجري هنا في خيط القراءة نفسه
     * (فيتأخّر رقم الرسّام ثانيةً أو أكثر، وكل دقيقة مرّة)، وصار يجري في خيط خلفي ويُسلَّم للنبضة التالية.
     * وإلى أن يصل الجهاز يُعرض الرسّام من القراءات المباشرة الخفيفة ([readGpuMhz]) فلا يبقى «—».
     */
    private fun gpuDevice(now: Long): GpuHardwareBackend.Device? {
        val cached = gpuDevice
        if (cached == null || now - gpuDeviceAtMs >= GPU_DEVICE_TTL_MS) startGpuProbe(now)
        return cached
    }

    private fun startGpuProbe(now: Long) {
        // تراجع تدريجي بعد كل إخفاق (٢ ثم ٤ ثم ٨ … حتى ٣٠ ث): الاكتشاف الكامل ثقيل،
        // وجهازٌ لا يُكتشف لا يستحق إعادته كل ثانيتين طوال اللعب.
        val gap = minOf(GPU_RETRY_MS shl gpuProbeFailures.coerceIn(0, 4), 30_000L)
        if (gpuProbeAtMs != 0L && now - gpuProbeAtMs < gap) return
        if (!gpuProbing.compareAndSet(false, true)) return
        gpuProbeAtMs = now
        thread(name = "MaxPanelGpuProbe", isDaemon = true) {
            try {
                val found = runCatching { GpuHardwareBackend.selection().device }.getOrNull()
                if (found != null) {
                    gpuDevice = found
                    gpuDeviceAtMs = android.os.SystemClock.elapsedRealtime()
                    gpuProbeFailures = 0
                } else {
                    gpuProbeFailures = gpuProbeFailures + 1
                }
            } finally {
                gpuProbing.set(false)
            }
        }
    }

    /** العقد الحيّة الثلاث (`cur_freq` و`max_freq` وقفل OPP إن وُجد) في نداء واحد لا اكتشافًا كاملًا. */
    private fun readGpuLive(d: GpuHardwareBackend.Device): GpuLiveRead.Live {
        val lockPath = d.mtkFixedIndexPath
        val nodes = ArrayList<String>(3)
        nodes.add("${d.path}/cur_freq")
        nodes.add("${d.path}/max_freq")
        if (lockPath != null) nodes.add(lockPath)
        val raw = RootFileAccess.readMany(nodes)
        return GpuLiveRead.derive(
            d,
            GpuLiveRead.Raw(
                cur = raw.getOrNull(0)?.trim()?.toLongOrNull(),
                max = raw.getOrNull(1)?.trim()?.toLongOrNull(),
                lock = if (lockPath != null) raw.getOrNull(2) else null
            )
        )
    }

    /**
     * حمل الرسّام: عقدة واحدة مُثبَّتة بعد أول نجاح (قراءة واحدة كل نبضة). وحين لا تُحلّ عقدة يُعاد
     * التجريب كل [LOAD_PROBE_MS]، وبينهما يُستعان بمجسّات `MtkUtils` المحفوظة.
     */
    private fun readGpuLoad(dev: GpuHardwareBackend.Device?, now: Long): Int? {
        val node = gpuLoadNode
        if (node != null) {
            GpuLiveRead.loadPercent(node, RootFileAccess.read(node))?.let { return it }
            gpuLoadNode = null
        }
        if (dev != null && (gpuLoadProbeAtMs == 0L || now - gpuLoadProbeAtMs >= LOAD_PROBE_MS)) {
            gpuLoadProbeAtMs = now
            val candidates = GpuLiveRead.loadCandidates(dev)
            val raws = RootFileAccess.readMany(candidates)
            for (i in candidates.indices) {
                val percent = GpuLiveRead.loadPercent(candidates[i], raws.getOrNull(i))
                if (percent != null) {
                    gpuLoadNode = candidates[i]
                    return percent
                }
            }
        }
        return runCatching { LEADING.find(MtkUtils.getGpuLoad())?.value?.toIntOrNull() }
            .getOrNull()?.takeIf { it in 0..100 }
    }

    /** ميغاهرتز من قيمة خام بوحدة غير معلومة (Hz/kHz/MHz)؛ تُستنتج من حجمها، والملتبس `null`. */
    private fun rawToMhz(raw: Long?): Int? {
        if (raw == null || raw <= 0L) return null
        val unit = GpuHardwareBackend.inferFrequencyUnit(listOf(raw))
        if (unit == GpuHardwareBackend.FrequencyUnit.AMBIGUOUS) return null
        return ((raw * unit.hzMultiplier) / 1_000_000L).toInt().takeIf { it > 0 }
    }

    /**
     * سقف احتياطي حين لا يُحلّ جهاز الرسّام: عقدة `max_freq` ثم جدول OPP ثم عقد kgsl.
     * يُعاد حسابه كل [RETRY_MS] لا كل قراءة.
     */
    private fun gpuCeilingFallback(now: Long): Int? {
        if (gpuFallbackAtMs != 0L && now - gpuFallbackAtMs < RETRY_MS) return gpuFallbackCeiling
        gpuFallbackAtMs = now
        gpuFallbackCeiling = runCatching {
            MtkUtils.getGpuDevfreqNode()?.let { RootFileAccess.read("$it/max_freq")?.trim()?.toLongOrNull() }
                ?.div(1_000_000L)?.toInt()?.takeIf { it > 0 }
                ?: MtkUtils.oppFrequenciesHz().maxOrNull()?.div(1_000_000L)?.toInt()?.takeIf { it > 0 }
        }.getOrNull() ?: GPU_MAX_NODES.firstNotNullOfOrNull { node ->
            rawToMhz(RootFileAccess.read(node)?.trim()?.toLongOrNull())
        }
        return gpuFallbackCeiling
    }

    /** الجهاز المحفوظ أولًا (قراءة حيّة)، ثم مجسّ MediaTek، ثم عقد kgsl/Mali المباشرة. */
    private fun readGpuMhz(live: GpuLiveRead.Live?): Int? {
        live?.curMhz?.let { return it }
        val fromMtk = runCatching { LEADING.find(MtkUtils.getCurrentGpuFreq())?.value?.toIntOrNull() }
            .getOrNull()?.takeIf { it > 0 }
        if (fromMtk != null) return fromMtk
        return GPU_CUR_NODES.firstNotNullOfOrNull { node ->
            rawToMhz(RootFileAccess.read(node)?.trim()?.toLongOrNull())
        }
    }
}

/**
 * حمل المعالج الكلّي من `/proc/stat`: نسبة الانشغال بين قراءتين متتاليتين لهذه اللوحة وحدها.
 *
 * لا تُشارَك العدّادات مع قارئ الـHUD، فلكلٍّ خطّ أساسه ولا تُفسد إحدى القراءتين الأخرى.
 * القراءة الأولى، وكل قراءة بعد فجوة أطول من [FRESH_MS]، لا تُعطي قيمة لأن الدلتا لا تُحسب.
 */
object PanelCpuLoad {
    private const val FRESH_MS = 3_000L
    private val WS = Regex("\\s+")
    private var lastTotal = 0L
    private var lastIdle = 0L
    private var lastAt = 0L
    private var primed = false

    @Synchronized
    fun sample(): Int? {
        val now = android.os.SystemClock.elapsedRealtime()
        val line = runCatching { RootFileAccess.read("/proc/stat") }.getOrNull()?.lineSequence()?.firstOrNull()
        val fields = line?.trim()?.split(WS)?.drop(1)?.mapNotNull { it.toLongOrNull() }
        if (fields == null || fields.size < 5) {
            primed = false
            return null
        }
        val total = fields.sum()
        val idle = fields[3] + fields[4] // خمول + انتظار القرص (iowait)
        val fresh = primed && now - lastAt in 1..FRESH_MS
        val prevTotal = lastTotal
        val prevIdle = lastIdle
        lastTotal = total
        lastIdle = idle
        lastAt = now
        primed = true
        if (!fresh) return null
        val dt = total - prevTotal
        if (dt <= 0L) return null
        val busy = dt - (idle - prevIdle)
        return (100L * busy / dt).toInt().coerceIn(0, 100)
    }
}
