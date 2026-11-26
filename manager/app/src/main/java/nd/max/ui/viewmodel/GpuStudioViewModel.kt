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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.MaxManagerProps
import nd.max.core.hardware.ControlOwnership
import nd.max.core.hardware.GpuHardwareBackend
import nd.max.core.hardware.GpuTweakPersistence
import nd.max.core.hardware.HardwareControlArbiter
import nd.max.core.hardware.HardwareControlKey
import nd.max.core.hardware.ManualControlLocks
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
    /**
     * سطر تقني غير مترجَم يشرح **لماذا** رُفض التثبيت: المطلوب والساعة المقيسة ومصدرها
     * (مثل `req=520 MHz · clock=260 MHz @current_freqency`). بلا هذا السطر كان الرفض يصل
     * برمزٍ وحده فلا يُعرف أكان العتاد لم يقبل الكتابة أم أن الساعة المقيسة لا تتبعها.
     */
    val clockNote: String? = null,
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

    private var pollJob: Job? = null
    private var baselineProviderPath: String? = null

    fun load() {
        if (pollJob != null) return
        viewModelScope.launch(Dispatchers.IO) {
            val selection = GpuHardwareBackend.selection()
            val device = selection.device
            if (baselineProviderPath != device?.path) {
                baselineProviderPath = device?.path
                state = state.copy(baseline = device?.let(GpuHardwareBackend::captureBaseline))
            }
            val live = device?.let(::readLive)
            state = state.copy(
                loading = false,
                selection = selection,
                device = device,
                liveMhz = live?.first,
                liveSource = live?.second,
                // تُقرأ عند فتح الشاشة، ثم تُقرأ مرّة أخرى إن تغيّر المزوّد (في الاستقصاء أدناه):
                // المفاتيح لا تتغيّر إلا بفعل المستخدم في هذه الشاشة، فموضعا التغيّر معروفان.
                savedRequest = device?.let(GpuTweakPersistence::loadValidated),
            )
            startPolling()
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val selection = GpuHardwareBackend.selection()
                val device = selection.device
                // نفس رقم الشاشة: **التردد الجاري المقاس**، لا السقف ولا فهرس القفل.
                val live = device?.let(::readLive)
                val liveMhz = live?.first
                val verified = state.verifiedSnapshot
                val drifted = verified != null && (
                    device == null ||
                        device.path != verified.path ||
                        device.minFreq != verified.minFreq ||
                        device.maxFreq != verified.maxFreq ||
                        device.governor != verified.governor
                    )
                val states = device?.let { GpuHardwareBackend.timeInState(it) }
                val providerChanged = baselineProviderPath != device?.path
                if (providerChanged) baselineProviderPath = device?.path
                state = state.copy(
                    loading = false,
                    selection = selection,
                    device = device,
                    baseline = if (providerChanged) device?.let(GpuHardwareBackend::captureBaseline) else state.baseline,
                    pending = if (providerChanged) null else state.pending,
                    stateTimes = states,
                    liveMhz = liveMhz,
                    liveSource = live?.second,
                    historyMHz = if (providerChanged) listOfNotNull(liveMhz?.toFloat()) else if (liveMhz != null) (state.historyMHz + liveMhz.toFloat()).takeLast(60) else state.historyMHz,
                    verifiedSnapshot = if (drifted || providerChanged) null else verified,
                    // مع تغيّر المزوّد قد تقرأ المفاتيح حالةً لا تُصدَّق على عتادٍ آخر
                    // (`loadValidated` تُحكّم الطلب على الجهاز الجديد وترد `null` إن لم يصلح)
                    // فلا يبقى سطرٌ يقول «محفوظة» لعتادٍ ليس هذا.
                    savedRequest = if (providerChanged) device?.let(GpuTweakPersistence::loadValidated) else state.savedRequest,
                    notice = when {
                        providerChanged -> GpuNotice(GpuNoticeKind.PROVIDER_CHANGED)
                        drifted -> GpuNotice(GpuNoticeKind.DRIFTED)
                        else -> state.notice
                    },
                )
                delay(1_500)
            }
        }
    }

    fun stageMode(mode: GpuHardwareBackend.IntentMode) {
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
        state = state.copy(
            pending = null,
            lastResult = null,
            notice = GpuNotice(GpuNoticeKind.CANCELLED),
            verifiedSnapshot = null,
            mode = null,
            stagedIntent = null,
        )
    }

    fun applyPreview() {
        val device = state.device ?: return
        val pending = state.pending ?: return
        state = state.copy(applying = true)
        viewModelScope.launch(Dispatchers.IO) {
            val key = HardwareControlKey.gpuFrequency(device.name)
            val token = "gpu-studio-${System.currentTimeMillis()}"
            val baseline = GpuHardwareBackend.captureBaseline(device)
            // القيمة المطلوبة والصيغة المقروءة من **نفس** الشكل: `GpuHardwareBackend`
            // يحمل الحقول التي يمكن للطلب أن يمسّها (المدى · المُحكِّم · قفل OPP).
            // كان المدى وحده، فطلبٌ يمسّ المُحكِّم أو يحرّر القفل لا يساوي المقروء
            // أبدًا — فيُصنَّف نجاحه فشلًا وتُعاد الحالة السابقة (وهو ما يراه
            // المستخدم: «اخترت فلا يتغير شيء»).
            val desired = GpuHardwareBackend.encodeRequest(pending)
            val arbiterResult = arbiter.submit(
                key = key,
                owner = ControlOwnership.Owner.GLOBAL_PROFILE,
                token = token,
                desired = desired,
                // الطلب نفسه يُطبَّق لا نصّ يُفكّ من جديد: المُحكِّم لا يُطبِّق إلا
                // `desired` الخاص بهذا الطلب، فإعادة الترميز هنا كانت تُسقط
                // `releaseLock` لو أُضيف حقل للمخطط لاحقًا.
                // الحكم من قياس المسار نفسه لا من نجاح الكتابة: `pin` قد يكتب الفهرس
                // ثم يقيس ترددًا مخالفًا فيُرجع `verified=false` مع `writeSucceeded=true`.
                // وتمرير `writeSucceeded` كان يترك المُحكِّم يعلن نجاحًا لأن قراءة الفهرس
                // عادت إلى خط أساس مساوٍ للطلب (قفل قديم ٨٣٢) — وهو العطب المقيس.
                apply = { GpuHardwareBackend.applyValidated(device, pending).verified },
                read = {
                    GpuHardwareBackend.refresh(device.path)?.let { live ->
                        GpuHardwareBackend.encodeLive(live, pending)
                    }
                },
                baseline = GpuHardwareBackend.encodeLive(device, pending),
                restore = { GpuHardwareBackend.restoreBaseline(baseline) },
                // صدى الفهرس وحده ليس حكمًا: قفل قديم ٨٣٢ يطابق الطلب بينما الساعة المقيسة
                // ٧٥٤. فلا يُقبل «مطابق» — قبل الكتابة أو بعدها — إلا إذا وافقت الساعة
                // المقيسة الطلب. وغياب القياس يُبقي حكم الفهرس (BY_INDEX) لا يُبطله.
                verify = { _, actual ->
                    val echo = actual != null && actual == desired
                    val target = pending.minFreq?.takeIf { it == pending.maxFreq }
                    if (target == null) {
                        // **المدى بحكم التحرير لا بالمساواة الحرفية** — نفس الدالة التي يحكم بها
                        // المحرّك، فلا سياستان لسلوك واحد. وهذا هو الموضع الذي كان يُحوِّل نجاح
                        // تحرير السقف إلى «رفض واستعادة»: طلبُ 1300 تُقصّه المنصّة إلى 520 بحقّها،
                        // فيعود الصدى `260:520` فلا يساوي `260:1300000000` فيُقال فشل ويُمحى التحرير.
                        GpuHardwareBackend.rangeRequestSatisfied(
                            liveBefore = device,
                            actual = GpuHardwareBackend.refresh(device.path),
                            request = pending,
                        )
                    } else {
                        val measured = GpuHardwareBackend.measuredFrequency(device)
                        // **الساعة هي الحكم.** صدى العقدة وحده ليس شرطًا: المنصّة قد تقصّ
                        // `max_freq` فترجع القراءة مختلفة بينما التردد المثبَّت هو المطلوب فعلًا
                        // (وهو ما يقرأه المستخدم). والتسامح ٢٪ أو ٥ MHz لأن الساعة تُقاس مرة
                        // ثانية بعد الكتابة بقليل، والمقارنة التامة كانت تُرجع تثبيتًا صحيحًا.
                        if (measured == null) echo
                        else GpuHardwareBackend.clockAgrees(target, measured.hz)
                    }
                },
                // والتخطّي قبل الكتابة يبقى مشروطًا بصدى العقدة **لا بالساعة**: ترددٌ يصادف أنه
                // يساوي الطلب الآن (خامل 260 عند طلب تثبيت 260) يجعل حكم الساعة صحيحًا، فلو
                // تُخطّيت الكتابة لما ثُبِّت شيء وارتفع التردد بعد ثوانٍ. `realized` يُسأل في موضع
                // واحد فقط: قبل أول محاولة؛ وبعدها يحكم `verify` وحده، فلا يُصنَّف تثبيت نُفِّذ.
                realized = { _, actual -> actual != null && actual == desired },
            )
            val refreshed = GpuHardwareBackend.selection()
            val failureClockNote = if (arbiterResult.verified) null else clockNote(refreshed.device ?: device, pending)
            val result = GpuHardwareBackend.TransactionResult(
                requested = pending,
                actual = refreshed.device,
                writeSucceeded = arbiterResult.applied,
                verified = arbiterResult.verified,
                rollbackAttempted = arbiterResult.rollbackAttempted,
                rollbackVerified = arbiterResult.rollbackVerified,
                error = arbiterResult.error,
            )
            state = state.copy(
                applying = false,
                selection = refreshed,
                device = refreshed.device,
                pending = if (result.verified) null else pending,
                lastResult = result,
                verifiedSnapshot = refreshed.device.takeIf { result.verified },
                mode = state.mode.takeIf { result.verified },
                stagedIntent = state.stagedIntent.takeIf { result.verified },
                notice = when {
                    result.verified -> GpuNotice(GpuNoticeKind.VERIFIED, reason = result.error)
                    result.rollbackVerified == true ->
                        GpuNotice(GpuNoticeKind.ROLLED_BACK, reason = result.error, clockNote = failureClockNote)
                    result.rollbackAttempted ->
                        GpuNotice(GpuNoticeKind.ROLLBACK_FAILED, reason = result.error, clockNote = failureClockNote)
                    else -> GpuNotice(GpuNoticeKind.REFUSED, reason = result.error, clockNote = failureClockNote)
                },
            )
        }
    }

    /**
     * المطلوب والساعة المقيسة بعد رفض — للتشخيص لا للترجمة. يُقرأ على خيط الإدخال والإخراج
     * وحده (يُستدعى من داخل `Dispatchers.IO`).
     */
    private fun clockNote(device: GpuHardwareBackend.Device, request: GpuHardwareBackend.Request): String? {
        val target = request.minFreq?.takeIf { it == request.maxFreq }
            ?.let { GpuHardwareBackend.frequencyMHz(device, it) }
        val measured = GpuHardwareBackend.measuredFrequency(device)
        if (target == null && measured == null) return null
        val parts = buildList {
            target?.let { add("req=$it MHz") }
            add(
                measured?.let { "clock=${it.hz / 1_000_000L} MHz @${it.source.substringAfterLast('/')}" }
                    ?: "clock=unread",
            )
        }
        return parts.joinToString(" · ")
    }

    fun restoreSession() {
        val baseline = state.baseline ?: return
        state = state.copy(applying = true)
        viewModelScope.launch(Dispatchers.IO) {
            val restored = if (baseline.fixedIndex != null || (baseline.minFreq != null && baseline.maxFreq != null) || baseline.governor != null) {
                GpuHardwareBackend.restoreBaseline(baseline)
            } else false
            val selection = GpuHardwareBackend.selection()
            state = state.copy(
                applying = false,
                selection = selection,
                device = selection.device,
                pending = null,
                lastResult = null,
                verifiedSnapshot = null,
                mode = null,
                stagedIntent = null,
                notice = GpuNotice(
                    if (restored) GpuNoticeKind.CANCELLED else GpuNoticeKind.ROLLBACK_FAILED,
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
        state = state.copy(applying = true)
        viewModelScope.launch(Dispatchers.IO) {
            // نيّة سابقة رابحة على مفتاح GPU قد تعيد تطبيق قفلها بعد الاستعادة، فتُبطل
            // أثر الزر في الدورة التالية. تُلغى نوايا هذه العملية أولًا، **بلا استرجاع**:
            // الاستعادة هي فعل هذا الزر نفسه، لا خط أساس جلسة سابقة.
            state.device?.let { device ->
                val key = HardwareControlKey.gpuFrequency(device.name)
                arbiter.snapshot().filter { it.key == key }.forEach { intent ->
                    arbiter.release(key, intent.token, restore = false)
                }
            }
            val reset = GpuHardwareBackend.resetToPlatformDefaults()
            // المحو بعد نجاح العتاد فقط: إعدادات محفوظة بلا قفل حُرِّر تُعيد الحالة القديمة
            // عند الإقلاع، فلا معنى لمحوها قبل أن يُثبت العتاد أنه عاد.
            val cleared = reset && GpuTweakPersistence.clearSaved()
            val selection = GpuHardwareBackend.selection()
            val device = selection.device
            val live = device?.let(::readLive)
            state = if (reset) {
                state.copy(
                    applying = false,
                    selection = selection,
                    device = device,
                    liveMhz = live?.first,
                    liveSource = live?.second,
                    baseline = device?.let(GpuHardwareBackend::captureBaseline),
                    pending = null,
                    lastResult = null,
                    verifiedSnapshot = null,
                    mode = null,
                    stagedIntent = null,
                    historyMHz = emptyList(),
                    savedRequest = if (cleared) null else device?.let(GpuTweakPersistence::loadValidated),
                    notice = if (cleared) {
                        GpuNotice(GpuNoticeKind.RESET)
                    } else {
                        // العتاد عاد، والمفاتيح لم تُمحَ: يُقال ذلك بصراحة لأن الإقلاع
                        // التالي سيعيد تطبيقها — وهو عكس ما وعد به زر «استعادة».
                        GpuNotice(GpuNoticeKind.RESET, reason = "saved-settings-not-cleared")
                    },
                )
            } else {
                state.copy(
                    applying = false,
                    selection = selection,
                    device = device,
                    liveMhz = live?.first,
                    liveSource = live?.second,
                    notice = GpuNotice(GpuNoticeKind.ROLLBACK_FAILED),
                )
            }
        }
    }

    fun saveVerifiedToTweaks() {
        val device = state.device ?: return
        val verified = state.verifiedSnapshot ?: return
        if (verified.path != device.path) return
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set(MaxManagerProps.GpuStudio.MIN_FREQ, verified.minFreq?.toString().orEmpty())
            PropertyUtils.set(MaxManagerProps.GpuStudio.MAX_FREQ, verified.maxFreq?.toString().orEmpty())
            PropertyUtils.set(MaxManagerProps.GpuStudio.GOVERNOR, verified.governor.orEmpty())
            PropertyUtils.set(MaxManagerProps.GpuStudio.MODE, state.mode ?: "custom")
            withContext(Dispatchers.Main) {
                state = state.copy(verifiedSnapshot = null, notice = null)
            }
        }
    }

    /**
     * التردد الجاري **المقاس** ومصدره: GED ثم `cur_freq` ثم `gpuclk` في
     * [GpuHardwareBackend.measuredFrequency]، فلا يُخلط مزوّد بآخر ولا تُوصف عقدة بغير اسمها.
     * و`null` حين لا يُقرأ شيء — لا صفر ولا سقف.
     */
    private fun readLive(device: GpuHardwareBackend.Device): Pair<Int, String>? =
        GpuHardwareBackend.measuredFrequency(device)?.let { measured ->
            (measured.hz / 1_000_000L).toInt().takeIf { it > 0 }?.let { it to measured.source }
        }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }

}
