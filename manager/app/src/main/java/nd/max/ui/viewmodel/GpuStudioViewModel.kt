/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.viewmodel

import javax.inject.Inject

import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import nd.max.MaxManagerProps
import nd.max.core.hardware.ControlOwnership
import nd.max.core.hardware.GpuCeilingPolicy
import nd.max.core.hardware.GpuHardwareBackend
import nd.max.core.hardware.GpuTweakPersistence
import nd.max.core.hardware.HardwareControlArbiter
import nd.max.core.hardware.HardwareControlKey
import nd.max.core.platform.PropertyUtils

/**
 * ناتج ما حدث، **برمز لا بجملة**.
 *
 * كان الحقل جملة عربية يكتبها الـViewModel، فتظهر في الواجهة الإنجليزية عربيةً — وهذا عطب
 * في الترجمة يتكرّر في كل شاشة تكتب نصّها في الطبقة الخلفية. والرمز هنا يُترجم في الشاشة،
 * و`reason` يبقى كما هو من العتاد (كود إنجليزي ثابت) لأنه حقيقة تقنية لا جملة للمستخدم.
 */
enum class GpuNoticeKind {
    STAGED_INTENT,
    STAGED_RANGE,
    STAGED_LOCK,
    STAGED_GOVERNOR,
    CANCELLED,
    /** The device exposes no trustworthy frequency table for this intent. */
    NO_TABLE,
    /** A staged request was refused before it reached the hardware. */
    REFUSED,
    VERIFIED,
    ROLLED_BACK,
    ROLLBACK_FAILED,
    PROVIDER_CHANGED,
    DRIFTED,
    /** استُعيد افتراضي المنصّة: حُرِّر القفل ومُحي المحفوظ، لا «طُبِّق طلب». */
    RESET,
}

@androidx.compose.runtime.Immutable
data class GpuNotice(
    val kind: GpuNoticeKind,
    /** Machine code straight from the backend (e.g. `unsupported-frequency`), or null. */
    val reason: String? = null,
    val intent: GpuHardwareBackend.IntentMode? = null,
)

data class GpuStudioUiState(
    val loading: Boolean = true,
    val selection: GpuHardwareBackend.Selection? = null,
    val device: GpuHardwareBackend.Device? = null,
    val pending: GpuHardwareBackend.Request? = null,
    val baseline: GpuHardwareBackend.Baseline? = null,
    /**
     * التردد الجاري **المقاس** — لا السقف ولا فهرس القفل.
     *
     * يُقرأ من GED ثم `cur_freq` بنفس سلّم القراءة الذي تستعمله الرئيسية، فيتطابق الرقمان.
     * و`null` تعني «لم يُقس» لا صفرًا، فلا يُعرض رقم مختلق.
     */
    val liveMhz: Int? = null,
    /** المصدر الفعلي لـ[liveMhz]، فتُعرض البطاقة عقدة القياس لا مسار المزوّد. `null` حين لا قياس. */
    val liveSource: String? = null,
    /** Timestamp of this clock sample, never the provider-discovery timestamp. */
    val liveSampleAtMs: Long? = null,
    /** Completion time of the device reading that supplies live limits/load/temperature. */
    val deviceSampleAtMs: Long? = null,
    val historyMHz: List<Float> = emptyList(),
    /**
     * جدول الوقت في الحالة كما نشرته النواة، أو `null` إن لم تنشره.
     *
     * و`null` ليست «صفرًا»: القسم لا يُعرض أصلًا لمزوّد لا ينشر جدولًا (§0.8)، بينما `null`
     * لجدول انقضى يبقى `null` ولا يُعرض له رقم. وهذا الفصل هو نفسه المطبَّق على المعالج.
     */
    val stateTimes: List<GpuHardwareBackend.StateTime>? = null,
    val applying: Boolean = false,
    val lastResult: GpuHardwareBackend.TransactionResult? = null,
    val notice: GpuNotice? = null,
    val verifiedSnapshot: GpuHardwareBackend.Device? = null,
    /** Smart-intent label behind the current staged/verified state, or null when custom. */
    val mode: String? = null,
    /** Which smart intent is currently staged, so the row can show real selection. */
    val stagedIntent: GpuHardwareBackend.IntentMode? = null,
    /**
     * الحالة **المحفوظة** التي يعيد تطبيقها كل إقلاع، مقروءةً من المفاتيح لا مُقدَّرة.
     *
     * ووجودها في الحالة لأن الشاشة تحتاج أن تقولها: `GpuTweakPersistence.applySaved()`
     * يعيد تطبيق المفاتيح في كل إقلاع وبعد كل تراجع per-app، فمن اختار «افتراضي» في هذه
     * الجلسة يجد حالته القديمة عائدة عند أول إقلاع. والقرار في محو المحفوظ قرار مالك؛ وما
     * هنا **قراءة خالصة** (`loadValidated` لا تكتب ولا تنادي `su`).
     */
    val savedRequest: GpuHardwareBackend.Request? = null,
)

/**
 * Session orchestrator. Hardware discovery and mutation stay in GpuHardwareBackend.
 *
 * و`@HiltViewModel` ليست زخرفة: الـViewModel له مُنشئ **بوسائط** (`arbiter`)، فلا مُنشئ له
 * بلا وسائط. وبلا هذا الوسم لا يستطيع Hilt أن يبنيه عند طلبه بـ`hiltViewModel()`، وبلا
 * `hiltViewModel()` ينادي `viewModel()` المصنعَ الافتراضي فيرمي عند فتح الشاشة — وهو العطل
 * الذي كان يخرج التطبيق. (ونفس قالب `CpuCoreControlViewModel`.)
 */
@HiltViewModel
class GpuStudioViewModel @Inject constructor(
    private val arbiter: HardwareControlArbiter,
) : ViewModel() {
    var state by mutableStateOf(GpuStudioUiState())
        private set

    companion object {
        const val LIVE_SAMPLE_MAX_AGE_MS = 5_000L
        private const val POLL_INTERVAL_MS = 1_500L
        private const val REQUEST_TOKEN = "gpu-studio"

        private fun readBaseline(device: GpuHardwareBackend.Device): String? =
            GpuHardwareBackend.refresh(device.path)?.takeIf {
                it.name == device.name && it.frequencyUnit == device.frequencyUnit
            }?.let {
                GpuHardwareBackend.encodeBaseline(GpuHardwareBackend.captureBaseline(it))
            }
    }

    private val hardwareMutex = Mutex()
    private var loadJob: Job? = null
    private var pollJob: Job? = null

    /** Presentation clock only; stops with the last lifecycle-aware subscriber. */
    val nowMs: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0L), System.currentTimeMillis())

    private data class LiveSample(val hz: Long, val mhz: Int, val source: String, val atMs: Long)

    private data class Observation(
        val selection: GpuHardwareBackend.Selection,
        val live: LiveSample?,
        val deviceAtMs: Long?,
        val stateTimes: List<GpuHardwareBackend.StateTime>?,
        val baseline: GpuHardwareBackend.Baseline?,
        val savedRequest: GpuHardwareBackend.Request?,
    )

    /** Called on IO while holding hardwareMutex; discovery cannot overwrite an in-flight write. */
    private fun readObservation(previous: GpuHardwareBackend.Device?, resetBaseline: Boolean = false): Observation {
        val selection = GpuHardwareBackend.selection()
        val device = selection.device
        val deviceAtMs = device?.let { System.currentTimeMillis() }
        val providerChanged = previous?.path != device?.path || previous?.name != device?.name ||
            previous?.frequencyUnit != device?.frequencyUnit
        val baseline = if (providerChanged || resetBaseline) device?.let(GpuHardwareBackend::captureBaseline) else null
        val saved = if (providerChanged || resetBaseline) device?.let(GpuTweakPersistence::loadValidated) else null
        val times = device?.let { GpuHardwareBackend.timeInState(it) }
        // Sample last, so a slow state-table read does not freshly label an older clock.
        val live = device?.let(::readLive)
        return Observation(selection, live, deviceAtMs, times, baseline, saved)
    }

    /** All state publication stays on Main and merges with the latest staged edits. */
    private fun withObservation(observation: Observation, resetBaseline: Boolean = false): GpuStudioUiState {
        val device = observation.selection.device
        val providerChanged = state.device?.path != device?.path || state.device?.name != device?.name ||
            state.device?.frequencyUnit != device?.frequencyUnit
        val verified = state.verifiedSnapshot
        val fixed = state.lastResult?.takeIf { it.verified }?.requested?.let {
            it.minFreq?.takeIf { min -> min == it.maxFreq }
        }
        val fixedHz = fixed?.times(verified?.frequencyUnit?.hzMultiplier ?: 0L)
        val drifted = verified != null && (
            device == null || device.path != verified.path || device.minFreq != verified.minFreq ||
                device.maxFreq != verified.maxFreq || device.governor != verified.governor ||
                (fixed != null && observation.live?.hz != fixedHz)
            )
        return state.copy(
            loading = false,
            selection = observation.selection,
            device = device,
            baseline = if (providerChanged || resetBaseline) observation.baseline else state.baseline,
            pending = if (providerChanged) null else state.pending,
            stateTimes = observation.stateTimes,
            liveMhz = observation.live?.mhz,
            liveSource = observation.live?.source,
            liveSampleAtMs = observation.live?.atMs,
            deviceSampleAtMs = observation.deviceAtMs,
            historyMHz = if (providerChanged) {
                listOfNotNull(observation.live?.mhz?.toFloat())
            } else if (observation.live != null) {
                (state.historyMHz + observation.live.mhz.toFloat()).takeLast(60)
            } else state.historyMHz,
            verifiedSnapshot = if (drifted || providerChanged) null else verified,
            lastResult = if (providerChanged) null else state.lastResult,
            mode = if (providerChanged) null else state.mode,
            stagedIntent = if (providerChanged) null else state.stagedIntent,
            savedRequest = if (providerChanged || resetBaseline) observation.savedRequest else state.savedRequest,
            notice = when {
                providerChanged && !state.loading -> GpuNotice(GpuNoticeKind.PROVIDER_CHANGED)
                drifted -> GpuNotice(GpuNoticeKind.DRIFTED)
                else -> state.notice
            },
        )
    }

    fun load() {
        if (pollJob?.isActive == true || loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            hardwareMutex.withLock {
                val previous = state.device
                val observation = withContext(Dispatchers.IO) { readObservation(previous) }
                state = withObservation(observation)
            }
            startPolling()
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                hardwareMutex.withLock {
                    if (state.applying) return@withLock
                    val previous = state.device
                    val observation = withContext(Dispatchers.IO) { readObservation(previous) }
                    // Apply may have been requested while this read was in flight.
                    if (!state.applying) state = withObservation(observation)
                }
            }
        }
    }

    /** Synchronous busy guard plus one IO transaction at a time; no staging can be lost on completion. */
    private fun mutate(operation: suspend () -> Unit) {
        if (state.applying || state.loading) return
        state = state.copy(applying = true)
        viewModelScope.launch {
            hardwareMutex.withLock {
                try {
                    operation()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    val previous = state.device
                    val observation = withContext(Dispatchers.IO) {
                        runCatching { readObservation(previous) }.getOrNull()
                    }
                    state = (observation?.let { withObservation(it) } ?: state.copy(
                        liveMhz = null,
                        liveSource = null,
                        liveSampleAtMs = null,
                        deviceSampleAtMs = null,
                    )).copy(
                        verifiedSnapshot = null,
                        lastResult = null,
                        // A thrown transaction is not evidence that no write happened or recovery succeeded.
                        notice = GpuNotice(GpuNoticeKind.ROLLBACK_FAILED, reason = "gpu-transaction-interrupted"),
                    )
                } finally {
                    state = state.copy(applying = false)
                }
            }
        }
    }

    fun stageMode(mode: GpuHardwareBackend.IntentMode) {
        if (state.applying) return
        val device = state.device ?: return
        val request = GpuHardwareBackend.requestForMode(device, mode)
        state = if (request == null) {
            state.copy(notice = GpuNotice(GpuNoticeKind.NO_TABLE, intent = mode))
        } else {
            state.copy(
                pending = request,
                lastResult = null,
                notice = GpuNotice(GpuNoticeKind.STAGED_INTENT, intent = mode),
                verifiedSnapshot = null,
                mode = mode.name.lowercase(),
                stagedIntent = mode,
            )
        }
    }

    fun stageRange(min: Long, max: Long) {
        if (state.applying) return
        val device = state.device ?: return
        val request = GpuHardwareBackend.Request(min, max, state.pending?.governor)
        val error = GpuHardwareBackend.validate(device, request)
        state = if (error == null) {
            state.copy(
                pending = request,
                lastResult = null,
                notice = GpuNotice(GpuNoticeKind.STAGED_RANGE),
                verifiedSnapshot = null,
                mode = null,
                stagedIntent = null,
            )
        } else {
            state.copy(notice = GpuNotice(GpuNoticeKind.REFUSED, reason = error))
        }
    }

    fun stageLock(frequency: Long) {
        if (state.applying) return
        val governor = state.pending?.governor
        val request = GpuHardwareBackend.Request(frequency, frequency, governor)
        val device = state.device ?: return
        val error = GpuHardwareBackend.validate(device, request)
        state = if (error == null) {
            state.copy(
                pending = request,
                lastResult = null,
                notice = GpuNotice(GpuNoticeKind.STAGED_LOCK),
                verifiedSnapshot = null,
                mode = null,
                stagedIntent = null,
            )
        } else {
            state.copy(notice = GpuNotice(GpuNoticeKind.REFUSED, reason = error))
        }
    }

    fun stageGovernor(governor: String) {
        if (state.applying) return
        val device = state.device ?: return
        val current = state.pending
        val request = GpuHardwareBackend.Request(
            minFreq = current?.minFreq,
            maxFreq = current?.maxFreq,
            governor = governor,
        )
        val error = GpuHardwareBackend.validate(device, request)
        state = if (error == null) {
            state.copy(
                pending = request,
                lastResult = null,
                notice = GpuNotice(GpuNoticeKind.STAGED_GOVERNOR),
                verifiedSnapshot = null,
                mode = null,
                stagedIntent = null,
            )
        } else {
            state.copy(notice = GpuNotice(GpuNoticeKind.REFUSED, reason = error))
        }
    }

    fun cancelPreview() {
        if (state.applying) return
        state = state.copy(
            pending = null,
            lastResult = null,
            notice = GpuNotice(GpuNoticeKind.CANCELLED),
            verifiedSnapshot = null,
            mode = null,
            stagedIntent = null,
        )
    }

    /** The mode switch stages the mutually exclusive request, not a second UI-only Boolean. */
    fun stageFrequencyMode(fixed: Boolean) {
        if (state.applying) return
        val device = state.device ?: return
        val frequencies = device.frequencies
        if (frequencies.isEmpty()) return
        if (fixed) {
            val target = state.pending?.maxFreq?.takeIf { it in frequencies }
                ?: device.maxFreq?.takeIf { it in frequencies } ?: frequencies.last()
            stageLock(target)
        } else {
            if (!device.devfreqCeilingWritable || frequencies.size < 2) {
                state = state.copy(notice = GpuNotice(GpuNoticeKind.REFUSED, reason = "range-read-only-or-unproven"))
                return
            }
            var min = state.pending?.minFreq ?: device.minFreq ?: frequencies.first()
            var max = state.pending?.maxFreq ?: device.maxFreq ?: frequencies.last()
            // Equal endpoints are an exact request. Switching off must actually stage a range.
            if (min >= max || min !in frequencies || max !in frequencies) {
                min = device.minFreq?.takeIf { it in frequencies } ?: frequencies.first()
                max = device.maxFreq?.takeIf { it in frequencies } ?: frequencies.last()
                if (min >= max) {
                    min = frequencies.first()
                    max = frequencies.last()
                }
            }
            stageRange(min, max)
        }
    }

    /** Read the other endpoint from the current pending request, not an older Compose callback. */
    fun stageRangeMinimum(frequency: Long) {
        if (state.applying) return
        val device = state.device ?: return
        val max = state.pending?.maxFreq ?: device.maxFreq ?: device.frequencies.lastOrNull() ?: return
        stageRange(minOf(frequency, max), max)
    }

    fun stageRangeMaximum(frequency: Long) {
        if (state.applying) return
        val device = state.device ?: return
        val min = state.pending?.minFreq ?: device.minFreq ?: device.frequencies.firstOrNull() ?: return
        stageRange(min, maxOf(frequency, min))
    }

    fun applyPreview() {
        if (state.applying) return
        val stagedDevice = state.device ?: return
        val pending = state.pending ?: return
        val mode = state.mode
        val intent = state.stagedIntent
        mutate {
            val (transaction, observation) = withContext(Dispatchers.IO) {
                // Retarget one bounded intent. Dropping only our token also makes the
                // rollback baseline the state immediately before this request, not an older apply.
                arbiter.releaseToken(REQUEST_TOKEN, restore = false)
                val selected = GpuHardwareBackend.selection()
                val device = selected.device?.takeIf {
                    it.path == stagedDevice.path && it.name == stagedDevice.name &&
                        it.frequencyUnit == stagedDevice.frequencyUnit
                }
                val result = if (device == null) {
                    GpuHardwareBackend.TransactionResult(
                        requested = pending,
                        actual = selected.device,
                        writeSucceeded = false,
                        verified = false,
                        error = if (selected.state == GpuHardwareBackend.SelectionState.AMBIGUOUS) {
                            "provider-ambiguous"
                        } else "provider-disappeared",
                    )
                } else {
                    var readDevice: GpuHardwareBackend.Device? = null
                    var backendResult: GpuHardwareBackend.TransactionResult? = null
                    var clockError: String? = null
                    val desired = GpuHardwareBackend.encodeRequest(pending)
                    val arbiterResult = arbiter.submit(
                        key = HardwareControlKey.gpuFrequency(device.name),
                        owner = ControlOwnership.Owner.GLOBAL_PROFILE,
                        token = REQUEST_TOKEN,
                        desired = desired,
                        apply = {
                            backendResult = GpuHardwareBackend.applyValidated(readDevice ?: device, pending)
                            backendResult?.verified == true
                        },
                        read = {
                            GpuHardwareBackend.refresh(device.path)?.takeIf {
                                it.name == device.name && it.frequencyUnit == device.frequencyUnit
                            }?.let { live ->
                                readDevice = live
                                GpuHardwareBackend.encodeBaseline(GpuHardwareBackend.captureBaseline(live))
                            }.also { if (it == null) readDevice = null }
                        },
                        // Baseline encoding is request-independent, including the vendor lock.
                        restore = { encoded ->
                            GpuHardwareBackend.decodeBaseline(encoded)?.let {
                                GpuHardwareBackend.restoreBaseline(it)
                            } == true
                        },
                        verify = { _, actual ->
                            val live = readDevice
                            val echo = actual != null && live != null &&
                                GpuHardwareBackend.encodeLive(live, pending) == desired
                            val target = pending.minFreq?.takeIf { it == pending.maxFreq }
                            if (target == null) {
                                echo
                            } else {
                                val multiplier = live?.frequencyUnit?.hzMultiplier ?: 0L
                                val targetHz = target.takeIf { multiplier > 0L && it <= Long.MAX_VALUE / multiplier }
                                    ?.times(multiplier)
                                val measured = live?.let { GpuHardwareBackend.currentFrequencyHz(it) }
                                val verdict = GpuCeilingPolicy.pinVerdict(targetHz, measured)
                                if (backendResult?.verified == true) {
                                    clockError = when (verdict) {
                                        GpuCeilingPolicy.PinVerdict.VERIFIED -> null
                                        GpuCeilingPolicy.PinVerdict.CLOCK_MISMATCH -> "pin-clock-mismatch"
                                        else -> "fixed-clock-unreadable"
                                    }
                                }
                                // An index/range echo with no measured clock is not fixed-clock success.
                                echo && verdict == GpuCeilingPolicy.PinVerdict.VERIFIED
                            }
                        },
                        // The first explicit apply must reach the backend even if configuration already echoes it.
                        realized = { _, _ -> backendResult?.verified == true },
                    )
                    GpuHardwareBackend.TransactionResult(
                        requested = pending,
                        actual = readDevice,
                        writeSucceeded = backendResult?.writeSucceeded ?: false,
                        verified = arbiterResult.verified && backendResult?.verified == true,
                        rollbackAttempted = arbiterResult.rollbackAttempted || backendResult?.rollbackAttempted == true,
                        rollbackVerified = if (arbiterResult.rollbackAttempted) {
                            arbiterResult.rollbackVerified
                        } else backendResult?.rollbackVerified,
                        // Preserve the backend's actual refusal, rather than replacing it with the arbiter wrapper.
                        error = backendResult?.error ?: clockError ?: arbiterResult.error,
                    )
                }
                result to readObservation(stagedDevice)
            }
            val providerMatches = observation.selection.device?.let {
                it.path == stagedDevice.path && it.name == stagedDevice.name &&
                    it.frequencyUnit == stagedDevice.frequencyUnit
            } == true
            val fixed = pending.minFreq?.takeIf { it == pending.maxFreq }
            val clockMatches = fixed == null || observation.live?.hz == fixed * stagedDevice.frequencyUnit.hzMultiplier
            val clockMoved = transaction.verified && providerMatches && !clockMatches
            val result = transaction.copy(
                actual = observation.selection.device,
                verified = transaction.verified && providerMatches && clockMatches,
                error = transaction.error ?: when {
                    !providerMatches -> "provider-disappeared"
                    clockMoved && observation.live == null -> "fixed-clock-unreadable"
                    clockMoved -> "pin-clock-mismatch"
                    else -> null
                },
            )
            state = withObservation(observation).copy(
                pending = pending.takeIf { !result.verified && providerMatches },
                lastResult = result,
                verifiedSnapshot = observation.selection.device.takeIf { result.verified },
                mode = mode.takeIf { result.verified },
                stagedIntent = intent.takeIf { result.verified },
                notice = when {
                    !providerMatches -> GpuNotice(GpuNoticeKind.PROVIDER_CHANGED, reason = result.error)
                    clockMoved -> GpuNotice(GpuNoticeKind.DRIFTED, reason = result.error)
                    result.verified -> GpuNotice(GpuNoticeKind.VERIFIED, reason = result.error)
                    result.rollbackVerified == true -> GpuNotice(GpuNoticeKind.ROLLED_BACK, reason = result.error)
                    result.rollbackAttempted -> GpuNotice(GpuNoticeKind.ROLLBACK_FAILED, reason = result.error)
                    else -> GpuNotice(GpuNoticeKind.REFUSED, reason = result.error)
                },
            )
        }
    }

    fun restoreSession() {
        if (state.applying) return
        val device = state.device ?: return
        val baseline = state.baseline ?: return
        if (baseline.devicePath != device.path) return
        mutate {
            val (result, observation) = withContext(Dispatchers.IO) {
                arbiter.releaseToken(REQUEST_TOKEN, restore = false)
                val result = arbiter.submit(
                    key = HardwareControlKey.gpuFrequency(device.name),
                    owner = ControlOwnership.Owner.GLOBAL_PROFILE,
                    token = REQUEST_TOKEN,
                    desired = GpuHardwareBackend.encodeBaseline(baseline),
                    apply = { GpuHardwareBackend.restoreBaseline(baseline) },
                    read = { readBaseline(device) },
                    restore = { encoded ->
                        GpuHardwareBackend.decodeBaseline(encoded)?.let { GpuHardwareBackend.restoreBaseline(it) } == true
                    },
                    verify = { desired, actual -> actual != null && actual == desired },
                )
                result to readObservation(device)
            }
            state = withObservation(observation).copy(
                pending = null,
                lastResult = null,
                verifiedSnapshot = null,
                mode = null,
                stagedIntent = null,
                notice = GpuNotice(
                    when {
                        result.verified -> GpuNoticeKind.CANCELLED
                        result.rollbackVerified == true -> GpuNoticeKind.ROLLED_BACK
                        result.rollbackAttempted -> GpuNoticeKind.ROLLBACK_FAILED
                        else -> GpuNoticeKind.REFUSED
                    },
                    reason = result.error,
                ),
            )
        }
    }

    /**
     * استعادة افتراضي المنصّة: تحرير قفل OPP الثابت، وإرجاع المدى المعلن، ومحو ما يعيد
     * التطبيقُ تطبيقه كل إقلاع.
     *
     * وليست «افتراضي (حالة البداية)»: تلك تستعيد لقطة فتح الشاشة، وهي نفسها قد تكون
     * ملوّثة بقفل جلسة أقدم. وهذه تذهب إلى العتاد نفسه، ثم تقرأه مرتجِعًا؛ ولا تمسّ
     * المحفوظ إلا إذا نجح المسار كله — ففشلٌ جزئيّ لا يُفقد المستخدمَ إعداداته القديمة
     * بلا بديل مقيس.
     */
    fun resetToDefaults() {
        if (state.applying) return
        val device = state.device ?: return
        mutate {
            val (result, cleared, observation) = withContext(Dispatchers.IO) {
                // Never evict another subsystem's intent to make a reset look successful.
                arbiter.releaseToken(REQUEST_TOKEN, restore = false)
                var resetVerified = false
                val result = arbiter.submit(
                    key = HardwareControlKey.gpuFrequency(device.name),
                    owner = ControlOwnership.Owner.GLOBAL_PROFILE,
                    token = REQUEST_TOKEN,
                    desired = "gpu-platform-defaults",
                    apply = {
                        resetVerified = GpuHardwareBackend.resetToPlatformDefaults()
                        resetVerified
                    },
                    read = { readBaseline(device) },
                    restore = { encoded ->
                        GpuHardwareBackend.decodeBaseline(encoded)?.let { GpuHardwareBackend.restoreBaseline(it) } == true
                    },
                    verify = { _, actual -> resetVerified && actual != null },
                    realized = { _, _ -> resetVerified },
                )
                val cleared = result.verified && GpuTweakPersistence.clearSaved()
                Triple(result, cleared, readObservation(device, resetBaseline = result.verified))
            }
            val refreshed = withObservation(observation, resetBaseline = result.verified)
            state = if (result.verified) {
                refreshed.copy(
                    pending = null,
                    lastResult = null,
                    verifiedSnapshot = null,
                    mode = null,
                    stagedIntent = null,
                    historyMHz = emptyList(),
                    savedRequest = if (cleared) null else observation.savedRequest,
                    notice = if (cleared) {
                        GpuNotice(GpuNoticeKind.RESET)
                    } else {
                        // العتاد عاد، والمفاتيح لم تُمحَ: يُقال ذلك بصراحة لأن الإقلاع
                        // التالي سيعيد تطبيقها — وهو عكس ما وعد به زر «استعادة».
                        GpuNotice(GpuNoticeKind.RESET, reason = "saved-settings-not-cleared")
                    },
                )
            } else {
                refreshed.copy(
                    lastResult = null,
                    verifiedSnapshot = null,
                    notice = GpuNotice(
                        when {
                            result.rollbackVerified == true -> GpuNoticeKind.ROLLED_BACK
                            result.rollbackAttempted -> GpuNoticeKind.ROLLBACK_FAILED
                            else -> GpuNoticeKind.REFUSED
                        },
                        reason = result.error,
                    ),
                )
            }
        }
    }

    fun saveVerifiedToTweaks() {
        if (state.applying) return
        val device = state.device ?: return
        val verified = state.verifiedSnapshot ?: return
        if (verified.path != device.path) return
        val mode = state.mode ?: "custom"
        mutate {
            val observation = withContext(Dispatchers.IO) { readObservation(device) }
            val refreshed = withObservation(observation)
            if (refreshed.verifiedSnapshot == null) {
                state = refreshed
                return@mutate
            }
            val saved = withContext(Dispatchers.IO) {
                PropertyUtils.set(MaxManagerProps.GpuStudio.MIN_FREQ, verified.minFreq?.toString().orEmpty())
                PropertyUtils.set(MaxManagerProps.GpuStudio.MAX_FREQ, verified.maxFreq?.toString().orEmpty())
                PropertyUtils.set(MaxManagerProps.GpuStudio.GOVERNOR, verified.governor.orEmpty())
                PropertyUtils.set(MaxManagerProps.GpuStudio.MODE, mode)
                observation.selection.device?.let(GpuTweakPersistence::loadValidated)
            }
            // Refresh the saved label without changing the session-start restoration baseline.
            state = refreshed.copy(
                verifiedSnapshot = null,
                notice = null,
                savedRequest = saved,
            )
        }
    }

    /**
     * التردد الجاري **المقاس** ومصدره: GED ثم `cur_freq` ثم `gpuclk` في
     * [GpuHardwareBackend.measuredFrequency]، فلا يُخلط مزوّد بآخر ولا تُوصف عقدة بغير اسمها.
     * و`null` حين لا يُقرأ شيء — لا صفر ولا سقف.
     */
    private fun readLive(device: GpuHardwareBackend.Device): LiveSample? =
        GpuHardwareBackend.measuredFrequency(device)?.let { measured ->
            (measured.hz / 1_000_000L).toInt().takeIf { it > 0 }?.let {
                LiveSample(measured.hz, it, measured.source, measured.observedAtMs)
            }
        }

    override fun onCleared() {
        loadJob?.cancel()
        pollJob?.cancel()
        super.onCleared()
    }

}
