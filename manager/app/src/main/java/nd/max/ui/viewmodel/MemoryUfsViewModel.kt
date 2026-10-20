/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.viewmodel

import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nd.max.core.hardware.ControlOwnership
import nd.max.core.hardware.DevfreqCeilingContracts
import nd.max.core.hardware.HardwareControlArbiter
import nd.max.core.hardware.HardwareControlKey
import nd.max.core.hardware.HardwareDeviceOverlay
import nd.max.core.hardware.HardwareOverlayStore
import nd.max.core.hardware.MemoryPressureReader
import nd.max.core.hardware.MemoryUfsHardwareBackend
import nd.max.core.hardware.MemoryUfsTuningStore
import nd.max.core.hardware.SystemDevfreqCeilingAccess
import nd.max.core.hardware.ThermalGuard
import nd.max.core.maxai.MemoryStall
import nd.max.ui.util.MemoryUfsModel

/**
 * ما نتج عن آخر كتابة — **قراءةً لا ادّعاءً**.
 *
 * والفرق بين «طُلب» و«قُبل» هو الفرق الذي يجعل شاشة تحكّم عتاد صادقة: قيمةٌ يرفضها السائق
 * (سياسة حرارة، سقف مصنّع، عقدة للقراءة فقط) تظهر هنا بنصّها المطلوب وبما قُرئ بعده.
 */
data class MemoryUfsOutcome(
    val key: String,
    val deviceLabel: String,
    val requested: String,
    val live: String?,
    val verified: Boolean,
    val reason: String?,
)

sealed interface MemoryUfsOperation {

    data object Applying : MemoryUfsOperation

    /** انتهت الكتابة: جهازًا جهازًا، ولكلّ نتيجةٌ مقيسة. */
    data class Finished(val outcomes: List<MemoryUfsOutcome>) : MemoryUfsOperation {
        val allVerified: Boolean get() = outcomes.isNotEmpty() && outcomes.all { it.verified }
        val anyVerified: Boolean get() = outcomes.any { it.verified }
    }

    /** رُفض الطلب قبل أي كتابة، برمز سبب ثابت. */
    data class Refused(val code: String, val deviceLabel: String?) : MemoryUfsOperation

    /**
     * نتيجة **تعليم جهاز** أو نسيانه — مفصولة عن نتائج الكتابة عن قصد: التعليم يغيّر تصنيفًا في
     * ملفّ التطبيق، ولا يكتب عقدة. وخلطُه بنتائج الكتابة يجعل شاشةً واحدة تُظهر «نجاح عتاد»
     * لِما لم يلمس العتاد. وما رُفض يُعرض برمزه كما رفض الفحص.
     */
    data class Overlay(
        val action: Action,
        val device: String,
        val code: String? = null,
    ) : MemoryUfsOperation {
        enum class Action { TAUGHT, FORGOTTEN, REFUSED }
    }
}

/**
 * جلسة «الذاكرة وUFS»: تقرأ الأجهزة، وتبني الطلب، وتمرّر الكتابة من المُحكِّم.
 *
 * ولماذا `@HiltViewModel`: هذا الـViewModel له **مُنشئ بوسائط** (المُحكِّم والسياق)، فلا مُنشئ
 * له بلا وسائط — فطلبه بـ`viewModel()` يرمي عند فتح الشاشة (العطب الموثَّق في
 * `ViewModelInstantiationTest`). فالشاشة تطلبه بـ`hiltViewModel()`.
 *
 * والكتابة **لا تمرّ من هنا مباشرة إلى العقدة**: كل نداء يمرّ بـ[HardwareControlArbiter]
 * بمفتاحه القياسي وبصمته وخط أساسه واسترجاعه — وهي الحدّ الواحد الذي يمنع كاتبين يتنازعان
 * عقدةً واحدة، ويرفض الكتابة إن كان المستخدم قد قفل المقبض يدويًّا.
 */
@HiltViewModel
class MemoryUfsViewModel @Inject constructor(
    private val arbiter: HardwareControlArbiter,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    var state by mutableStateOf(MemoryUfsUiState())
        private set

    /** ملفّ التعليم في مجلّد التطبيق — لا صلاحية إضافية ولا مسار نظام. */
    private val overlayStore: HardwareOverlayStore = HardwareOverlayStore.inDirectory(context.filesDir)

    private var pollJob: Job? = null

    /** بصمة كل معاملة ناجحة: بها يُسترجع **ما كتبناه نحن** لا ما كتبه غيرنا. */
    private val tokens = linkedMapOf<String, String>()

    /** ما أثبتته القراءة بعد آخر كتابة ناجحة: يُقارَن بكل عيّنة لكشف الانحراف. */
    private val verified = linkedMapOf<String, Long?>()

    val memoryDevices: List<MemoryUfsHardwareBackend.Device>
        get() = state.devices.filter { it.kind == MemoryUfsHardwareBackend.Kind.MEMORY }

    val storageDevices: List<MemoryUfsHardwareBackend.Device>
        get() = state.devices.filter { it.kind == MemoryUfsHardwareBackend.Kind.STORAGE }

    val selectedMemory: MemoryUfsHardwareBackend.Device?
        get() = memoryDevices.firstOrNull { it.path == state.selectedMemoryPath } ?: memoryDevices.firstOrNull()

    val selectedStorage: MemoryUfsHardwareBackend.Device?
        get() = storageDevices.firstOrNull { it.path == state.selectedStoragePath } ?: storageDevices.firstOrNull()

    fun load() {
        if (pollJob != null) return
        pollJob = viewModelScope.launch(Dispatchers.IO) {
            // التصنيف يُشغَّل **قبل** أول قراءة، وإلا عرضت الشاشة لحظةً أجهزةً غير مُعلَّمة ثم
            // قفزت — والقياس يجب أن يكون على الحالة التي ستبقى.
            installOverlayFromStore()
            while (isActive) {
                refreshOnce()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun pausePolling() {
        pollJob?.cancel()
        pollJob = null
    }

    /**
     * قراءة واحدة: تكتشف، وتقرأ الخنق والحرارة، وتحديث الحالة — يستعملها الدور والتحديث الفوري.
     * وهي مفصولة لسبب مقيس: تعليم جهاز يجب أن يُظهر أثره في اللحظة، لا بعد دورة كاملة (ثانيتان)
     * — والانتظار هنا يُقرأ «لم ينجح التعليم».
     */
    private suspend fun refreshOnce() {
        val discovery = runCatching { MemoryUfsHardwareBackend.discover() }
            .getOrElse { MemoryUfsHardwareBackend.Discovery(emptyList(), false, "discovery-failed") }
        val stall = runCatching { MemoryPressureReader.read() }.getOrDefault(MemoryStall.UNSUPPORTED)
        val pressure = runCatching {
            ThermalGuard.readPressure(context.getSystemService(Context.POWER_SERVICE) as? PowerManager)
        }.getOrDefault(ThermalGuard.Pressure.UNKNOWN)

        // الانحراف يُقاس على العقدة نفسها: قيمةٌ كتبناها وتحقّقت ثم تغيّرت بيد كاتب آخر
        // (سياسة مصنّع، أو عملية أخرى) — وهي الحالة التي تشرح «لماذا عاد الرقم وحده».
        val drifted = verified.any { (key, maxNode) ->
            val device = discovery.devices.firstOrNull { HardwareControlKey.devfreqFrequency(it.kind, it.name) == key }
            device != null && maxNode != null && device.maxFreq != maxNode
        }

        state = state.copy(
            loading = false,
            devices = discovery.devices,
            scanned = discovery.scanned,
            reason = discovery.reason,
            unclassified = discovery.unclassified,
            unreadable = discovery.unreadable,
            graphicsOwned = discovery.graphicsOwned,
            stall = stall,
            thermal = pressure,
            notice = when {
                drifted -> NOTICE_DRIFTED
                state.notice == NOTICE_DRIFTED -> null
                else -> state.notice
            },
            // وما انحرف يُنسى: بصمته تُسقَط فلا يُعاد وصف رقمٍ لم يبقَ لنا.
            selectedMemoryPath = state.selectedMemoryPath
                ?: discovery.devices.firstOrNull { it.kind == MemoryUfsHardwareBackend.Kind.MEMORY }?.path,
            selectedStoragePath = state.selectedStoragePath
                ?: discovery.devices.firstOrNull { it.kind == MemoryUfsHardwareBackend.Kind.STORAGE }?.path,
        )
        if (drifted) verified.keys.toList().forEach { key ->
            val device = discovery.devices.firstOrNull {
                HardwareControlKey.devfreqFrequency(it.kind, it.name) == key
            }
            if (device != null && verified[key] != null && device.maxFreq != verified[key]) {
                tokens.remove(key)
                verified.remove(key)
            }
        }
    }

    /** يقرأ ملفّ التعليم ويُشغّله في المحرّك، ويُظهر ما رُفض منه كما هو. */
    private fun installOverlayFromStore() {
        val parsed = runCatching { overlayStore.load() }
            .getOrElse { HardwareDeviceOverlay.Parsed() }
        MemoryUfsHardwareBackend.installOverlay(parsed.entries)
        state = state.copy(
            taughtDevices = parsed.entries.map { it.device },
            overlayRejections = parsed.rejections,
        )
    }

    /**
     * **تعلّم جهازًا**: يُفحص أوّلًا ([MemoryUfsHardwareBackend.probe])، ولا يُخزَّن إلا ما أُجيز
     * بقياس. وترتيب الخطوات مقصود: فحص ← خزن ← إعادة تحميل ← قراءة فورية؛ ولا يُعلن التعليم
     * نجاحًا إن لم يُكتب الملفّ فعلًا.
     */
    fun teachDevice(directory: String, kind: MemoryUfsHardwareBackend.Kind) {
        // والكلّ على خيط الإدخال/الإخراج: الفحص يقرأ عقدة sysfs (قراءات صغيرة ولكنها IO)، ونداؤه
        // من خيط الواجهة يجعل نقرةً تُنتج إطارًا متقطّعًا — والقاعدة نفسها في كل مسار قراءة هنا.
        viewModelScope.launch(Dispatchers.IO) {
            val probe = runCatching { MemoryUfsHardwareBackend.probe(directory, kind) }
                .getOrElse { MemoryUfsHardwareBackend.Probe.Refused(PROBE_FAILED) }
            if (probe is MemoryUfsHardwareBackend.Probe.Refused) {
                state = state.copy(
                    operation = MemoryUfsOperation.Overlay(MemoryUfsOperation.Overlay.Action.REFUSED, directory, probe.reason),
                )
                return@launch
            }
            val stored = runCatching {
                overlayStore.teach(HardwareDeviceOverlay.Entry(directory, kind))
            }.getOrDefault(false)
            if (!stored) {
                state = state.copy(
                    operation = MemoryUfsOperation.Overlay(MemoryUfsOperation.Overlay.Action.REFUSED, directory, OVERLAY_NOT_SAVED),
                )
                return@launch
            }
            installOverlayFromStore()
            refreshOnce()
            state = state.copy(operation = MemoryUfsOperation.Overlay(MemoryUfsOperation.Overlay.Action.TAUGHT, directory))
        }
    }

    /** ينسى تعليم جهاز — والجهاز يعود إلى ما تقوله القواعد المبنية (أو إلى قائمة المجهول). */
    fun forgetDevice(device: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val stored = runCatching { overlayStore.forget(device) }.getOrDefault(false)
            if (!stored) {
                state = state.copy(
                    operation = MemoryUfsOperation.Overlay(MemoryUfsOperation.Overlay.Action.REFUSED, device, OVERLAY_NOT_SAVED),
                )
                return@launch
            }
            installOverlayFromStore()
            refreshOnce()
            state = state.copy(operation = MemoryUfsOperation.Overlay(MemoryUfsOperation.Overlay.Action.FORGOTTEN, device))
        }
    }

    fun selectMemory(path: String) {
        state = state.copy(selectedMemoryPath = path, operation = null)
    }

    fun selectStorage(path: String) {
        state = state.copy(selectedStoragePath = path, operation = null)
    }

    fun setMode(mode: MemoryUfsModel.Mode) {
        state = state.copy(mode = mode, operation = null)
    }

    fun setApplyToAll(enabled: Boolean) {
        state = state.copy(applyToAll = enabled, operation = null)
    }

    /** أرضية صريحة بوحدة العقدة (من شريط التمرير أو من اقتراح الخنق). */
    fun setFloor(node: Long?) {
        state = state.copy(floorNode = node, operation = null)
    }

    /** اقتراح الخنق لجهاز بعينه — يُحسب في الطبقة الصافية، وهذا مجرّد تمرير. */
    fun floorAdviceFor(device: MemoryUfsHardwareBackend.Device): Long? = MemoryUfsModel.floorAdvice(
        device = device,
        fullFraction = state.stall.fullFraction,
        ceilingNode = MemoryUfsHardwareBackend.nodeForFraction(device, state.mode.fraction),
    )

    /**
     * كتابة البروفايل المختار على الجهاز المختار — أو على **كل أجهزة النوع** حين يكون
     * «طبّق على الكلّ» مفعّلًا.
     *
     * ولماذا «الكلّ» ليس ترفًا: على نواة واحدة تُصدَّر ثلاثة أجهزة ذاكرة (واحد لكل عنقود)،
     * وتغيير واحدها يترك البقية تُسحب إلى أدنى حالاتها — فيُقرأ الجهاز «لم يتغيّر».
     */
    fun apply(kind: MemoryUfsHardwareBackend.Kind) {
        val candidates = when {
            state.applyToAll -> state.devices.filter { it.kind == kind }
            else -> listOfNotNull(if (kind == MemoryUfsHardwareBackend.Kind.MEMORY) selectedMemory else selectedStorage)
        }
        if (candidates.isEmpty()) {
            state = state.copy(operation = MemoryUfsOperation.Refused(NO_DEVICE_CODE, null))
            return
        }
        state = state.copy(applying = true, operation = MemoryUfsOperation.Applying)
        viewModelScope.launch(Dispatchers.IO) {
            val outcomes = candidates.map { device -> writeOne(device) }
            state = state.copy(applying = false, operation = MemoryUfsOperation.Finished(outcomes))
        }
    }

    private fun writeOne(device: MemoryUfsHardwareBackend.Device): MemoryUfsOutcome {
        val key = HardwareControlKey.devfreqFrequency(device.kind, device.name)
        val label = MemoryUfsModel.deviceLabel(device)
        val request = MemoryUfsModel.request(device, state.mode, state.floorNode)
            ?: return MemoryUfsOutcome(key, label, "-", null, verified = false, reason = NO_PROVEN_RANGE)

        val desired = MemoryUfsHardwareBackend.encodeRequest(request)
        val live = MemoryUfsHardwareBackend.refresh(device.path) ?: device
        val baseline = MemoryUfsHardwareBackend.encodeBaseline(live)
        val token = "memufs-${System.currentTimeMillis()}-${device.name}"

        val result = runCatching {
            arbiter.submit(
                key = key,
                owner = ControlOwnership.Owner.GLOBAL_PROFILE,
                token = token,
                desired = desired,
                apply = { value -> SystemDevfreqCeilingAccess.writeRange(device.path, value) },
                read = { SystemDevfreqCeilingAccess.readBaseline(device.path) },
                baseline = baseline,
                restore = { value -> SystemDevfreqCeilingAccess.restoreBaseline(device.path, value) },
                verify = DevfreqCeilingContracts::contained,
                realized = DevfreqCeilingContracts::reached,
            )
        }.getOrElse { error ->
            return MemoryUfsOutcome(key, label, desired, null, verified = false, reason = error.message ?: WRITE_FAILED)
        }

        val after = MemoryUfsHardwareBackend.refresh(device.path)
        if (result.verified) {
            tokens[key] = token
            verified[key] = after?.maxFreq
            // **يُحفظ ما أُثبت بقراءة فقط:** الطلب نفسه لا قيمة القراءة، فيُعاد التحقّق منه
            // على سلّم الجهاز عند الإقلاع قبل أي كتابة (`MemoryUfsTuningStore`).
            MemoryUfsTuningStore.save(context, device.path, request)
        } else {
            tokens.remove(key)
            verified.remove(key)
        }
        return MemoryUfsOutcome(
            key = key,
            deviceLabel = label,
            requested = desired,
            live = after?.let(MemoryUfsHardwareBackend::encodeBaseline),
            verified = result.verified,
            reason = result.error,
        )
    }

    /**
     * يعيد كل مقبض كتبناه في هذه الجلسة إلى خط أساسه — بالبصمة نفسها، فلا يُلمس مقبض غيره.
     */
    fun revert() {
        val keys = tokens.keys.toList()
        if (keys.isEmpty()) {
            state = state.copy(operation = MemoryUfsOperation.Refused(NOTHING_TO_REVERT, null))
            return
        }
        state = state.copy(applying = true, operation = MemoryUfsOperation.Applying)
        viewModelScope.launch(Dispatchers.IO) {
            val outcomes = keys.map { key ->
                val token = tokens.remove(key) ?: return@map null
                val device = state.devices.firstOrNull {
                    HardwareControlKey.devfreqFrequency(it.kind, it.name) == key
                }
                val label = device?.let(MemoryUfsModel::deviceLabel)
                val released = runCatching { arbiter.release(key, token, restore = true) }.getOrNull()
                // التراجع يعني «لا تُعِد ما تركته» — فالطلب المحفوظ يُنسى مع البصمة.
                device?.path?.let { MemoryUfsTuningStore.forget(context, it) }
                verified.remove(key)
                MemoryUfsOutcome(
                    key = key,
                    deviceLabel = label ?: key,
                    requested = device?.let { MemoryUfsHardwareBackend.encodeBaseline(it) } ?: "-",
                    live = device?.path?.let { MemoryUfsHardwareBackend.refresh(it) }
                        ?.let(MemoryUfsHardwareBackend::encodeBaseline),
                    verified = released?.rollbackVerified == true || released == null,
                    reason = if (released == null) NOTHING_TO_REVERT else released.error,
                )
            }.filterNotNull()
            state = state.copy(applying = false, operation = MemoryUfsOperation.Finished(outcomes))
        }
    }

    fun dismissOperation() {
        state = state.copy(operation = null)
    }

    fun dismissNotice() {
        state = state.copy(notice = null)
    }

    companion object {
        const val POLL_INTERVAL_MS = 2_000L

        /** رموز ثابتة تُترجَم في الشاشة — لا جملة تُبنى في الـViewModel. */
        const val NO_PROVEN_RANGE = "no-proven-range"
        const val NO_DEVICE_CODE = "no-devfreq-device-seen"
        const val NOTHING_TO_REVERT = "nothing-to-revert"
        const val WRITE_FAILED = "write-failed"
        const val NOTICE_DRIFTED = "drifted"

        /** فشل الفحص نفسه (استثناء) — لا يُترجم إلى «الجهاز غير صالح». */
        const val PROBE_FAILED = "probe-failed"

        /** التعليم أُجيز قياسًا ولكن الملفّ لم يُكتب (أو لم يُقرأ بعد الكتابة). */
        const val OVERLAY_NOT_SAVED = "overlay-not-saved"
    }
}

/** حالة الشاشة كاملة — كائن واحد يُقرأ ويُنسخ، فلا تتفرّق الحقيقة على حقول متضاربة. */    data class MemoryUfsUiState(
        val loading: Boolean = true,
        val devices: List<MemoryUfsHardwareBackend.Device> = emptyList(),
        /** عُقَد تردّد لم يُعرف صنفها — تُعرَض ليُعلَّم اسمها (وغيابها يعني «لا مجهول هنا»). */
        val unclassified: List<MemoryUfsHardwareBackend.Unclassified> = emptyList(),
        /**
         * عُقَد رُصدت **ولم تُقرأ**: نقصُ قياس يُعلَن، ولا يُقرأ «لا ميزة» ولا يُعرَض للتعليم —
         * فلا شيء منها أُجيب عنه لُيعلَّم.
         */
        val unreadable: List<String> = emptyList(),
        /** عُقَد رسومية في المجلد نفسه: مملوكة لاستوديو GPU ولا تُعلَّم هنا. */
        val graphicsOwned: List<String> = emptyList(),
        /** ما علّمه المستخدم فعلًا (أسماء المجلّدات) — يُعرض مع إمكانية النسيان. */
        val taughtDevices: List<String> = emptyList(),
        /** أسطر الملفّ التي لم تُقبل، بأسبابها — لا تُصمَت، ولا تُسقط الملفّ. */
        val overlayRejections: List<HardwareDeviceOverlay.Rejection> = emptyList(),
        val scanned: Boolean = false,
        val reason: String = "not-scanned",
    val selectedMemoryPath: String? = null,
    val selectedStoragePath: String? = null,
    val mode: MemoryUfsModel.Mode = MemoryUfsModel.Mode.SUSTAINED,
    val floorNode: Long? = null,
    val applyToAll: Boolean = false,
    val applying: Boolean = false,
    val operation: MemoryUfsOperation? = null,
    val stall: MemoryStall.Sample = MemoryStall.UNSUPPORTED,
    val thermal: ThermalGuard.Pressure = ThermalGuard.Pressure.UNKNOWN,
    val notice: String? = null,
)
