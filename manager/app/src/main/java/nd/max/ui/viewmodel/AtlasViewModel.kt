/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.ui.viewmodel

import android.app.Application
import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nd.max.core.atlas.AtlasDeviceIdentity
import nd.max.core.atlas.AtlasDiscovery
import nd.max.core.atlas.AtlasDomain
import nd.max.core.atlas.AtlasFailure
import nd.max.core.atlas.AtlasFeatureOutcome
import nd.max.core.atlas.AtlasFeatureRequest
import nd.max.core.atlas.AtlasFixtureAnswer
import nd.max.core.atlas.AtlasFixtureCodec
import nd.max.core.atlas.AtlasFixtureOrigin
import nd.max.core.atlas.AtlasFixtureStore
import nd.max.core.atlas.AtlasJob
import nd.max.core.atlas.AtlasKernelRelease
import nd.max.core.atlas.AtlasRecordingTransport
import nd.max.core.atlas.AtlasRepository
import nd.max.core.atlas.AtlasScanState
import nd.max.core.atlas.AtlasScanStatus
import nd.max.core.atlas.AtlasStage
import nd.max.core.atlas.MaxAtlas
import nd.max.core.diagnostics.AtlasReportExporter
import nd.max.core.diagnostics.AtlasSupportReport
import nd.max.core.diagnostics.DiagnosticCenter
import nd.max.core.hardware.AccessLevel
import nd.max.core.hardware.AtlasAdapterContext
import nd.max.core.hardware.AtlasReadTransport
import nd.max.core.hardware.CpuHardwareBackend
import nd.max.core.hardware.GpuHardwareBackend
import nd.max.core.hardware.HardwareCapabilityResolver
import nd.max.core.hardware.HardwareFeature
import nd.max.core.hardware.MemoryUfsHardwareBackend
import nd.max.core.hardware.SystemCeilingAccess
import nd.max.core.hardware.SystemDevfreqCeilingAccess
import nd.max.core.hardware.SystemGpuCeilingAccess
import nd.max.core.threading.DispatcherProvider
import nd.max.ui.design.MaxDataTrust

/**
 * Where a scan stands, as a code the screen turns into words.
 *
 * It is not [AtlasScanStatus] repeated: `PARTIAL` and `DENIED` are scan facts, while `Incomplete` and
 * `Denied` are what a reader needs to be told, and a cancelled run is its own phase because the one
 * thing that must never happen is that leaving the screen looks like the device having nothing.
 */
enum class AtlasUiPhase { Idle, Running, Complete, Incomplete, Cancelled, Denied, Unavailable }

/** Why no report can be prepared. Each value is a different fact, and the screen names it. */
enum class AtlasReportRefusal {
    /** Nothing has been read yet, or a read is still in flight. */
    NotFinished,

    /** The user left the screen: the outcomes describe an interrupted pass. */
    WasCancelled,

    /** A bound (time, operations, entries, bytes) stopped the pass, so "did not answer" is not the device's answer. */
    BoundReached,

    /** More interfaces than a summary may carry. */
    TooManyInterfaces,

    /** The report builder refused for a reason of its own. */
    Refused,

    /** The report was built but could not be written to disk. */
    WriteFailed,
}

/**
 * What one captured device fixture contains, as counts.
 *
 * Counts and not a verdict: a capture states how many answers were recorded, and never that the device
 * is supported. `replayedBack` is the artifact checking itself — the encoded bytes were decoded again
 * and the run could still be replayed from them — which is a fact about the file, not about the phone.
 */
data class AtlasCapture(
    val origin: AtlasFixtureOrigin,
    val values: Int,
    val failures: Int,
    val listings: Int,
    val resolutions: Int,
    /** Paths the recorder refused by policy, named in the fixture rather than dropped. */
    val skipped: Int,
    /** The entry ceiling was reached: the capture is honest but covers less than the pass asked for. */
    val truncated: Boolean,
    /** Answers the format could not hold. Counted, because an omission must not be invisible. */
    val unrecordable: Int,
    val replayedBack: Boolean,
) {
    val total: Int get() = values + failures + listings + resolutions

    companion object {
        fun of(result: AtlasRecordingTransport.Result, replayedBack: Boolean): AtlasCapture = AtlasCapture(
            origin = result.fixture.origin,
            values = result.fixture.counted { it is AtlasFixtureAnswer.Text },
            failures = result.fixture.counted { it is AtlasFixtureAnswer.Missing },
            listings = result.fixture.counted { it is AtlasFixtureAnswer.Listing },
            resolutions = result.fixture.counted {
                it is AtlasFixtureAnswer.Canonical || it is AtlasFixtureAnswer.Unresolvable
            },
            skipped = result.skipped.size,
            truncated = result.truncated,
            unrecordable = result.unrecordable,
            replayedBack = replayedBack,
        )
    }
}

/**
 * Everything the Atlas section of the diagnostics screen renders.
 *
 * The scan is a repository snapshot, never assembled here: a screen cannot start work, and a
 * recomposition cannot invent progress. `previewText` is the **frozen artifact** — what the user
 * previewed is byte-for-byte what is shared, which is only true because serializing reads nothing.
 *
 * `capture` and `evidenceFile` are the device half of the evidence loop: the summary of a recorded
 * pass and the file a share may hand over. They are separate from the report's fields because they are
 * separate artifacts — a report describes a pass, a fixture *is* the pass, and a screen that conflated
 * them would offer one where the user asked for the other.
 */
data class AtlasUiState(
    val scan: AtlasScanState = AtlasScanState(),
    val previewText: String? = null,
    val refusal: AtlasReportRefusal? = null,
    val shareFile: File? = null,
    val capture: AtlasCapture? = null,
    /** Why a capture could not be taken, or why it was dropped. Never a silent absence. */
    val captureRefusal: AtlasReportRefusal? = null,
    /** The captured fixture on disk, or `null` when it could not be written. */
    val evidenceFile: File? = null,
    /** One-shot chooser request. Distinct from [shareFile] because it hands over a different artifact. */
    val shareEvidenceRequest: Boolean = false,
    /** True once a pass has been asked for, so the empty state can differ from "never started". */
    val everStarted: Boolean = false,
    /**
     * Whether a bound (time, operations, entries, bytes) actually stopped the pass.
     *
     * It comes from the job's own counters rather than from the outcome count, because "we stopped" and
     * "the device stopped answering" are different facts and only one of them is worth a report.
     */
    val limitsReached: Boolean = false,
)

/**
 * The Atlas surface's only bridge to the reading machinery (`P7`, slice `G`).
 *
 * Three rules are structural here, not stylistic:
 *
 * 1. **No hardware I/O, no shell, no policy.** This class never opens a file, never builds a path and
 *    never decides what may be written; it hands the repository a feature list and a discovery
 *    function, and renders what comes back.
 * 2. **Nothing scans on construction.** Opening the diagnostics screen builds this object and reads
 *    the app's own version — no device is touched until [start] is called.
 * 3. **The candidate pass is the repository's, not the screen's.** The second bank is expanded inside
 *    the same scan, on the same budget and the same cancellation, so a screen cannot start a second
 *    body of reads behind the first one's back.
 */
@HiltViewModel
class AtlasViewModel @Inject constructor(
    private val application: Application,
    private val repository: AtlasRepository,
    private val discovery: AtlasDiscovery,
    private val identity: AtlasDeviceIdentity,
    private val dispatchers: DispatcherProvider,
    private val maxAtlas: MaxAtlas,
    /**
     * The same transport the discovery path reads through, injected here for one reason: recording a
     * device fixture means decorating it for one pass. Nothing else in this class reads a node.
     */
    private val readTransport: AtlasReadTransport,
) : ViewModel() {

    private val exporter = AtlasReportExporter(AtlasReportExporter.directoryFor(application.cacheDir))

    private val fixtureStore = AtlasFixtureStore(AtlasFixtureStore.directoryFor(application.cacheDir))

    /** The recording decorator for the pass in flight, or `null` when this pass is not being captured. */
    private var recording: AtlasRecordingTransport? = null

    private val _ui = MutableStateFlow(AtlasUiState())
    val ui: StateFlow<AtlasUiState> = _ui.asStateFlow()

    /** The job of the scan in flight, kept for its budgets and its "a limit stopped us" verdict. */
    private var job: AtlasJob? = null

    /** Every path this scan has already attempted, so no bank reads the same interface twice. */
    private var attemptedPaths: Set<String> = emptySet()

    init {
        viewModelScope.launch(dispatchers.io) {
            repository.state.collect { scan ->
                // Re-read on every publication: the terminal publication is what makes the final verdict
                // available, so the screen never shows "a bound stopped this" before it is true.
                _ui.value = _ui.value.copy(scan = scan, limitsReached = job?.limitsReached() == true)
                finishCapture(scan)
            }
        }
        viewModelScope.launch(dispatchers.io) {
            // Age-based cleanup of earlier reports. It never touches the file a share may still be
            // reading, because the newest export is passed as the protected one when a share happens.
            runCatching { exporter.cleanup() }
            runCatching { fixtureStore.cleanup() }
        }
    }

    /**
     * Starts a pass: the reviewed bank first, then the accumulated vocabulary for whatever it could not
     * answer.
     *
     * The enumeration happens here rather than in the repository because it needs the job, and the job
     * needs the same bounded access the scan will read through — so the listing and the reads share one
     * budget instead of two.
     */
    fun start() {
        if (_ui.value.scan.isRunning) return
        _ui.value = _ui.value.copy(everStarted = true, refusal = null, previewText = null, shareFile = null)
        viewModelScope.launch(dispatchers.io) {
            // The capture decision belongs to *this pass*: a decorated transport is used for the run the
            // user asked to record and never becomes the app's mode.
            val transport = recording ?: readTransport
            val reviewed = discovery.features(AtlasDiscovery.REVIEWED)
            val opened = discovery.newJob(reviewed, readTransport = transport)
            val reviewedChildren = discovery.enumeratedFeatures(
                job = opened,
                catalogs = listOf(AtlasDiscovery.REVIEWED),
                attemptedPaths = reviewed.map { it.request.path }.toSet(),
            )
            val plan = reviewed + reviewedChildren
            val planned = discovery.replan(opened, plan)
            job = planned
            attemptedPaths = plan.map { it.request.path }.toSet()
            repository.start(
                features = plan,
                discover = planned::discover,
                complete = { domains -> candidates(planned, domains) },
            )
        }
    }

    /** Retry is a new pass: held evidence is reused and only what is stale or unknown is read again. */
    fun retry() = start()

    /** Leaves the pass. The state says so from this moment, and a late answer cannot change it. */
    fun cancel() {
        repository.cancel()
    }

    /**
     * Builds the report and freezes it for preview.
     *
     * Refusals are decided **before** the artifact exists: a pass that was cancelled, or that a bound
     * stopped, has no report at all, because a report is a statement about what the device answered and
     * neither of those is the device answering.
     */
    fun previewReport() {
        val scan = _ui.value.scan
        val refusal = AtlasPresentation.refusalFor(scan, job?.limitsReached() == true)
        if (refusal != null) {
            _ui.value = _ui.value.copy(refusal = refusal, previewText = null)
            return
        }
        // والبناء **يقرأ العتاد** (خريطة القدرة: سياسات · جهاز GPU · وصول حرارة) فلا يقع على خيط
        // المستدعي — نفس مسلك `share()` وبقية مدخلات هذا الـViewModel. وما يُقرأ هنا قياسٌ للتوثيق،
        // والمسح المجمَّد في `scan` كافٍ للبناء فلا رقابة على المسح من هنا.
        viewModelScope.launch(dispatchers.io) {
            val built = AtlasSupportReport.from(
                state = scan,
                appVersion = appVersion(),
                catalogVersion = discovery.catalog.version,
                device = reportedDevice(),
                diagnostics = diagnostics(),
                capabilities = capabilityRows(),
                nowMs = SystemClock.elapsedRealtime(),
                limitsReached = job?.limitsReached() == true,
            )
            _ui.value = when (built) {
                is AtlasSupportReport.Result.Ready ->
                    _ui.value.copy(previewText = built.report.encode(), refusal = null)

                is AtlasSupportReport.Result.NotReportable ->
                    _ui.value.copy(previewText = null, refusal = AtlasReportRefusal.Refused)
            }
        }
    }

    /**
     * Writes the **previewed** bytes and asks for a share destination.
     *
     * The text is the one already in state: regenerating here would mean the user approved one artifact
     * and sent another. No upload path exists — the file goes to the system chooser and nowhere else.
     */
    fun share() {
        val text = _ui.value.previewText ?: return
        viewModelScope.launch(dispatchers.io) {
            val file = exporter.write(text)
            val protected = file ?: exporter.reports().firstOrNull()
            runCatching { exporter.cleanup(protected = protected) }
            _ui.value = _ui.value.copy(
                shareFile = file,
                refusal = if (file == null) AtlasReportRefusal.WriteFailed else null,
            )
        }
    }

    /** The screen has handed the file to the system chooser; the request is one-shot. */
    fun consumeShareRequest() {
        _ui.value = _ui.value.copy(shareFile = null)
    }

    /** Discards the frozen preview. The next preview is built from a fresh reading. */
    fun dismissPreview() {
        _ui.value = _ui.value.copy(previewText = null)
    }

    /**
     * Records the **next** pass as a device fixture.
     *
     * This is the device half of the evidence loop (`P13`): without it the replay machinery is real but
     * unreachable, which is why every compatibility claim in this repository still ends in "needs a
     * phone". One tap arms the recording, the pass runs exactly as it always does, and the artifact is
     * the run's own answers — not a summary written afterwards by whoever was debugging.
     *
     * A pass that is unfinished, cancelled or stopped by a bound is **not** capturable. That is the
     * doctor's first rule applied at the source: a fixture from an interrupted run would replay as a
     * device whose silence is really the interruption, and every later comparison would report a
     * difference the harness caused.
     */
    fun captureDeviceEvidence() {
        if (_ui.value.scan.isRunning) return
        val refusal = AtlasPresentation.captureRefusal(_ui.value.scan, _ui.value.limitsReached)
        if (refusal != null) {
            _ui.value = _ui.value.copy(capture = null, captureRefusal = refusal, evidenceFile = null, shareEvidenceRequest = false)
            return
        }
        _ui.value = _ui.value.copy(
            capture = null,
            captureRefusal = null,
            evidenceFile = null,
            shareEvidenceRequest = false,
        )
        recording = AtlasRecordingTransport(
            inner = readTransport,
            catalogVersion = discovery.catalog.version,
        )
        start()
    }

    /** The screen has handed the fixture to the system chooser; the request is one-shot. */
    fun consumeEvidenceRequest() {
        _ui.value = _ui.value.copy(shareEvidenceRequest = false)
    }

    /** Offers the captured fixture again, for a second destination. The file itself is not regenerated. */
    fun shareEvidence() {
        _ui.value = _ui.value.copy(shareEvidenceRequest = _ui.value.evidenceFile != null)
    }

    /**
     * Finishes a capture when its pass reaches a terminal state, or drops it with a reason.
     *
     * `recording` is cleared on entry, so a later publication of the same terminal state cannot write a
     * second file or overwrite the first capture's summary.
     */
    private fun finishCapture(scan: AtlasScanState) {
        val recorder = recording ?: return
        if (scan.status == AtlasScanStatus.IDLE || scan.isRunning) return
        recording = null
        val refusal = AtlasPresentation.captureRefusal(scan, job?.limitsReached() == true)
        if (refusal != null) {
            _ui.value = _ui.value.copy(
                capture = null,
                captureRefusal = refusal,
                evidenceFile = null,
                shareEvidenceRequest = false,
            )
            return
        }
        val result = recorder.result()
        val text = runCatching { AtlasFixtureCodec.encode(result.fixture) }.getOrNull()
        // The artifact checks itself: the bytes about to be written are decoded again, so "the file is
        // readable" is measured rather than assumed. It says nothing about the device, and is not
        // reported as if it did.
        val readable = text?.let { AtlasFixtureCodec.decode(it).isReadable } == true
        val file = text?.takeIf { readable }?.let { fixtureStore.write(it) }
        _ui.value = _ui.value.copy(
            capture = AtlasCapture.of(result, replayedBack = readable),
            captureRefusal = if (file == null) AtlasReportRefusal.WriteFailed else null,
            evidenceFile = file,
            // The chooser opens once, right after a capture: the user asked for a file, so handing them
            // the destination instead of a second button to press is the whole flow. A missing file
            // opens nothing and says why.
            shareEvidenceRequest = file != null,
        )
    }

    /**
     * The candidate pass, run inside the scan.
     *
     * It is given the still-unresolved domains, and it never re-asks an interface this scan already
     * addressed: the second bank is coverage, not a second opinion about the same file.
     */
    private fun candidates(current: AtlasJob, domains: Set<AtlasDomain>): List<AtlasFeatureRequest> {
        if (domains.isEmpty()) return emptyList()
        val rootCandidates = discovery.features(AtlasDiscovery.COMMUNITY, domains)
        val attempted = attemptedPaths + rootCandidates.map { it.request.path }.toSet()
        val childCandidates = discovery.enumeratedFeatures(
            job = current,
            catalogs = listOf(AtlasDiscovery.COMMUNITY),
            attemptedPaths = attempted,
            domains = domains,
            limit = CANDIDATE_LIMIT,
        )
        val extra = rootCandidates + childCandidates
        attemptedPaths = attempted + extra.map { it.request.path }.toSet()
        return extra
    }

    private fun appVersion(): String = runCatching {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.packageManager.getPackageInfo(
                application.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            application.packageManager.getPackageInfo(application.packageName, 0)
        }
        info.versionName ?: UNKNOWN_VERSION
    }.getOrDefault(UNKNOWN_VERSION)

    /**
     * The coarse device description the report may carry.
     *
     * Nothing here identifies the owner: a SoC family, an API level, one ABI and a kernel major.minor
     * are properties of a model, and the kernel's build suffix is dropped rather than reported because
     * it encodes a vendor build, not a version anybody can act on.
     */
    /**
     * خريطة القدرة كما هي الآن — Discover ← Map في سطر واحد: مسارات كل هدف تُبنى ([probe]، ولا
     * يُنفَّذ شيء) وتُحكم بالمخطِّط، وتُدمج مع ما أثبته الجهاز في هذا الجيل. والصفوف **رموزٌ
     * لا جمل**: لا مسار ملف ولا رقم ولا سرّ — مطابقةً لطرح التقرير نفسه بالطرح لا بالمراقبة.
     */
    private fun capabilityRows(): List<AtlasSupportReport.ReportedCapability> =
        maxAtlas.profile(mapContext()).capabilities.entries.map { capability ->
            AtlasSupportReport.ReportedCapability(
                target = capability.target.name.lowercase(),
                code = capability.code,
                adapter = capability.adapterId,
                verified = capability.verified,
            )
        }

    /**
     * سياق القياس لخريطة القدرة — كل حقل فيه **مُقاس الآن** لا محفوظ: السياسات المكتشفة، وجهاز GPU
     * كما اختاره الاكتشاف، وقابلية كتابة السقوف من مُنفِّذ العقد المشترك، ووصول الحرارة من
     * مُحلِّل القدرات. فلا يقول تقريرٌ عن جهازٍ شيئًا لم يُقَس فيه في هذه الجلسة.
     */
    private fun mapContext(): AtlasAdapterContext {
        val snapshot = HardwareCapabilityResolver.resolve(application)
        val thermalReadable = snapshot.capability(HardwareFeature.THERMAL_ZONES)?.access
            ?.let { it != AccessLevel.NONE } == true
        // وعدّاد `devfreq` واحد لهذه الجلسة: الاكتشاف يُقرأ **مرّة** ثم تُقسم قائمته على النوعين.
        // ولو نادى كل هدف عدّه بنفسه لصار للتصنيف قياسان قد يختلفان داخل تقرير واحد — وهو
        // العطب الذي يمنعه «ملك واحد لكل عقدة» في هذا المستودع.
        val discovery = MemoryUfsHardwareBackend.discover()
        val devfreq = discovery.devices
        return AtlasAdapterContext(
            privileged = SystemCeilingAccess.privileged,
            cpuPolicies = CpuHardwareBackend.policies(),
            ceilingAccess = SystemCeilingAccess,
            gpuDevice = GpuHardwareBackend.selection().device,
            gpuAccess = SystemGpuCeilingAccess,
            thermalStatusReadable = thermalReadable,
            memoryDevices = devfreq.filter { it.kind == MemoryUfsHardwareBackend.Kind.MEMORY },
            storageDevices = devfreq.filter { it.kind == MemoryUfsHardwareBackend.Kind.STORAGE },
            devfreqAccess = SystemDevfreqCeilingAccess,
            // وما لم يُصنَّف يُنقل إلى الخريطة: بها يقول رمز الفجوة «عُقَد تردّد بلا اسم» بدل
            // «لم يُرصَد شيء» — وللفرق أثر: الأول يُغلق بنقرة تعليم، والثاني لا شيء فيه يُعلَم.
            unclassifiedDevfreq = discovery.unclassified,
            // وللسبب نفسه يُنقل نقصُ القراءة أيضًا: رمز فجوة يقول «لم يُقرأ» لا «لم يُوجد».
            unreadableDevfreq = discovery.unreadable,
        )
    }

    private fun reportedDevice(): AtlasSupportReport.ReportedDevice = AtlasSupportReport.ReportedDevice(
        socModel = identity.socModel,
        socManufacturer = identity.socManufacturer,
        apiLevel = identity.apiLevel,
        abi = identity.supportedAbis.firstOrNull(),
        kernelMajorMinor = AtlasKernelRelease.parse(identity.kernelRelease).numericRelease,
        lowRamDevice = identity.isLowRamDevice,
    )

    /**
     * The diagnostic projection: component names and counts.
     *
     * It comes from [DiagnosticCenter.structured], which has no message field at all — a diagnostic
     * report is the last place that should become a channel for free text a call site interpolated.
     */
    private fun diagnostics(): AtlasSupportReport.ReportedDiagnostics {
        val summary = DiagnosticCenter.structured()
        return AtlasSupportReport.ReportedDiagnostics(
            warnCount = summary.warnCount,
            errorCount = summary.errorCount,
            distinctComponents = summary.distinctComponents,
            components = summary.entries
                .take(AtlasSupportReport.MAX_COMPONENTS)
                .map { entry ->
                    AtlasSupportReport.ReportedDiagnostics.ComponentSummary(
                        component = entry.component,
                        level = entry.level.name,
                        count = entry.count,
                    )
                },
        )
    }

    private companion object {
        const val UNKNOWN_VERSION = "unknown"

        /**
         * How many candidate interfaces one pass may add, on top of the reviewed features.
         *
         * A design value, not a measurement: the transport's own op/byte/time budgets still apply, and
         * this cap exists so the second bank cannot turn one screen open into a device sweep. Thirty-two
         * covers a whole vendor GPU class plus a thermal zone set plus a battery on a normal device.
         */
        const val CANDIDATE_LIMIT = 32
    }
}

/**
 * The presentation rules of the Atlas surface, as **pure functions** (`P7`).
 *
 * They live in the ViewModel's file rather than in a composable for one reason: a Compose function can
 * only be tested with a host, and a rule that cannot fail a test is a rule that gets broken silently.
 * Every question here is one the screen would otherwise answer inline — which is exactly how "progress"
 * becomes a timer and "cancelled" becomes "unsupported".
 */
internal object AtlasPresentation {

    /** The lifecycle code a reader sees. */
    fun phase(scan: AtlasScanState): AtlasUiPhase = when (scan.status) {
        AtlasScanStatus.IDLE -> AtlasUiPhase.Idle
        AtlasScanStatus.RUNNING -> AtlasUiPhase.Running
        AtlasScanStatus.CANCELLED -> AtlasUiPhase.Cancelled
        AtlasScanStatus.DENIED -> AtlasUiPhase.Denied
        AtlasScanStatus.UNAVAILABLE -> AtlasUiPhase.Unavailable
        AtlasScanStatus.PARTIAL -> AtlasUiPhase.Incomplete
        AtlasScanStatus.COMPLETED ->
            if (scan.unresolved.isEmpty()) AtlasUiPhase.Complete else AtlasUiPhase.Incomplete
    }

    /**
     * Progress over interfaces, or `null` when the total is not known.
     *
     * `null` is the whole point: a bar at zero and a bar that does not know its length are different
     * facts, and a screen that prints `0%` for the second one has invented a number. There is no timer
     * anywhere in this function — elapsed time is not progress.
     */
    fun percent(scan: AtlasScanState): Int? = scan.percent

    /** How many interfaces the reviewed bank could not answer. Candidate misses are not counted. */
    fun reviewedGaps(scan: AtlasScanState): Int = scan.reviewedUnresolved.size

    /** How many readings came from the accumulated vocabulary rather than from reviewed knowledge. */
    fun candidateReadings(scan: AtlasScanState): Int = scan.outcomes.count {
        it is AtlasFeatureOutcome.Observed && it.stage == AtlasStage.CANDIDATE_INTERFACE
    }

    /** Readings the first bank answered, so the two tiers can be shown as two numbers, not one. */
    fun reviewedReadings(scan: AtlasScanState): Int = scan.outcomes.count {
        it is AtlasFeatureOutcome.Observed &&
            (it.stage == AtlasStage.REVIEWED_KNOWLEDGE || it.stage == AtlasStage.BOUNDED_DISCOVERY)
    }

    /**
     * The condition banner for a state that is not simply "here are your readings", or `null`.
     *
     * `Applied` is never returned: it means "a write completed and was verified", and no read-only pass
     * ever applied anything. A completed scan returns `null`, which the section renders as content
     * rather than as a notice — a success banner above real data is noise that trains people to ignore
     * the one banner that matters.
     */
    fun condition(scan: AtlasScanState, limitsReached: Boolean): AtlasConditionCode? = when (phase(scan)) {
        AtlasUiPhase.Idle -> null
        AtlasUiPhase.Running -> AtlasConditionCode.Reading
        AtlasUiPhase.Complete -> if (limitsReached) AtlasConditionCode.LimitReached else null
        AtlasUiPhase.Incomplete -> if (limitsReached) AtlasConditionCode.LimitReached else AtlasConditionCode.Partial
        AtlasUiPhase.Cancelled -> AtlasConditionCode.Cancelled
        AtlasUiPhase.Denied -> AtlasConditionCode.Denied
        AtlasUiPhase.Unavailable -> AtlasConditionCode.NoBackend
    }

    /**
     * Why no report may be prepared, or `null` when one may.
     *
     * Order matters and is fixed here so a caller cannot pick the reason it prefers: a cancelled pass is
     * cancelled whatever else is also true, and a bound being reached is reported as the bound rather
     * than as the device's silence.
     */
    fun refusalFor(scan: AtlasScanState, limitsReached: Boolean): AtlasReportRefusal? = when {
        scan.status == AtlasScanStatus.IDLE || scan.isRunning -> AtlasReportRefusal.NotFinished
        scan.status == AtlasScanStatus.CANCELLED -> AtlasReportRefusal.WasCancelled
        limitsReached -> AtlasReportRefusal.BoundReached
        scan.outcomes.size > AtlasSupportReport.MAX_FEATURES -> AtlasReportRefusal.TooManyInterfaces
        scan.reviewedUnresolved.isEmpty() -> AtlasReportRefusal.Refused
        else -> null
    }

    /** Whether the report suggestion is offered at all (`T7.6`). */
    fun offersReport(scan: AtlasScanState, limitsReached: Boolean): Boolean =
        refusalFor(scan, limitsReached) == null

    /**
     * Why this pass cannot be recorded as a device fixture, or `null` when it can.
     *
     * It is [refusalFor] without the report's last clause — a capture is worth taking even when every
     * reviewed interface answered, because the fixture records what the device *said*, and a device
     * that answered everything is exactly the baseline a regression should be compared against. What it
     * keeps is the part that decides honesty: an unfinished, cancelled or bound-stopped pass describes
     * the interruption, not the device, and must never become a replayable artifact.
     */
    fun captureRefusal(scan: AtlasScanState, limitsReached: Boolean): AtlasReportRefusal? = when {
        scan.status == AtlasScanStatus.IDLE || scan.isRunning -> AtlasReportRefusal.NotFinished
        scan.status == AtlasScanStatus.CANCELLED -> AtlasReportRefusal.WasCancelled
        limitsReached -> AtlasReportRefusal.BoundReached
        scan.outcomes.isEmpty() -> AtlasReportRefusal.Refused
        else -> null
    }

    /** Whether the device-evidence row is offered for this pass. */
    fun canCapture(scan: AtlasScanState, limitsReached: Boolean): Boolean =
        captureRefusal(scan, limitsReached) == null

    /**
     * How a reading must be trusted.
     *
     * A one-shot Atlas read is never `Live`, and that is deliberate: `Live` means "sampled within the
     * expected interval", and Atlas has no interval — it reads once, on demand. Marking a single read as
     * live is the exact falsehood the trust axis exists to prevent, so a successful reading is
     * `Snapshot`, a missing interface on an enumerated parent is `Unsupported`, and a failure is
     * `Unreadable`.
     */
    fun trust(outcome: AtlasFeatureOutcome): MaxDataTrust = when (outcome) {
        is AtlasFeatureOutcome.Observed -> MaxDataTrust.Snapshot
        is AtlasFeatureOutcome.Suppressed -> MaxDataTrust.Unreadable
        is AtlasFeatureOutcome.Cancelled -> MaxDataTrust.Unreadable
        is AtlasFeatureOutcome.Unresolved -> when (outcome.failure) {
            AtlasFailure.ABSENT, AtlasFailure.BACKEND_UNAVAILABLE -> MaxDataTrust.Unsupported
            else -> MaxDataTrust.Unreadable
        }
    }
}

/** A condition the section renders. Codes, not sentences: the screen owns the wording. */
enum class AtlasConditionCode { Reading, Partial, LimitReached, Cancelled, Denied, NoBackend }
