/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.component

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.audio.AudioSoundPreset
import nd.max.core.gamespace.CockpitModel
import nd.max.core.gamespace.DisplayVeilTone
import nd.max.core.gamespace.DndControl
import nd.max.core.gamespace.GameThermalProfiles
import nd.max.core.gamespace.PanelClockMath
import nd.max.core.gamespace.PanelClocks
import nd.max.core.gamespace.PanelControlState
import nd.max.core.gamespace.PanelDevice
import nd.max.core.gamespace.PanelScreenRecorder
import nd.max.core.gamespace.PanelSide
import nd.max.core.gamespace.PanelToggles
import nd.max.core.gamespace.ThermalPanelState
import nd.max.core.gamespace.ThermalPhase
import nd.max.core.platform.HudReading
import nd.max.service.AudioEffectService
import nd.max.ui.component.NeonColors as C
import nd.max.ui.util.CaptureConsentPrefs

/** عمر الرسالة العابرة في شريط الحالة. */
private const val NEON_NOTE_MS = 4_200L

/** عمر إيصال الإطلاق: أطول لأن قراءته عدد وسبب. */
private const val NEON_RECEIPT_MS = 9_000L

private const val NEON_LEAVE_MS = 240

/** أكبر تكبير للّوحة (أجهزة لوحية): فوقه تتضخّم بلا فائدة. */
private const val NEON_MAX_SCALE = 2f

private enum class NoteKind { Ok, Fail, Info }

/** ما يقوله الشريط السفلي في لحظة واحدة؛ `percent = null` يعني لا نسبة تُدّعى. */
private class NeonBar(val kind: NeonBarKind, val title: String, val sub: String, val percent: Int?)

/**
 * لوحة Game Turbo — التصميم النيوني (الافتراضي).
 *
 * طبق الأصل عن الصورة المرجعية، لكنّ **كل رقم فيها وكل مفتاح حقيقيّ**:
 *
 * - المقاييس من [HudReading] و[PanelClocks]، والفراغ شرطة لا رقم مخترع.
 * - الأوضاع الخمسة هي ملفّات المعالج الرسومي الحقيقية (`GameThermalProfiles`) بالمسار نفسه واللوحة الكلاسيكية.
 * - الأدوات تكتب بالمسارات القائمة وحدها ([PanelToggles] و[PanelDevice] و[PanelScreenRecorder]
 *   و[AudioEffectService])، وما لا خلفية له (قفل اللمس، شبكة الألعاب) **مُعتَّم ويقول ذلك** عند النقر —
 *   لا مفتاح وهميّ (ADR-07).
 * - شريط الحالة السفلي يقول ما جرى فعلًا؛ `100%` لا تظهر إلا بعد عمل اكتمل، لا زينة.
 */
@Composable
fun GamePanelTurboNeon(
    reading: HudReading?,
    side: PanelSide,
    recording: Boolean,
    refreshRateHz: Int?,
    clocks: PanelClocks,
    controls: PanelControlState,
    frames: List<Float>,
    onCollapse: () -> Unit,
    onToggleRecording: () -> Unit,
    onOpenControls: () -> Unit,
    onSelectProfile: (String) -> Unit,
    veilEnabled: Boolean,
    veilTone: DisplayVeilTone,
    onCycleVeil: () -> Unit,
    thermal: ThermalPanelState,
    onSelectThermal: (String) -> Unit,
    origin: Offset,
    launchReceipt: String?
) {
    val context = LocalContext.current
    val realDensity = LocalDensity.current
    val statusTop = WindowInsets.statusBars.getTop(realDensity) / realDensity.density
    val device = remember { PanelDevice(context) }
    val scope = rememberCoroutineScope()
    val enter = remember { Animatable(0f) }
    var leaving by remember { mutableStateOf(false) }
    var activeAt by remember { mutableStateOf(SystemClock.uptimeMillis()) }
    var holdUntil by remember { mutableStateOf(0L) }

    var zen by remember { mutableStateOf(0) }
    var screenRec by remember { mutableStateOf(false) }
    var autoBright by remember { mutableStateOf<Boolean?>(null) }
    var soundOn by remember { mutableStateOf(false) }
    var cleaning by remember { mutableStateOf(false) }
    var consentOpen by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var noteKind by remember { mutableStateOf(NoteKind.Ok) }
    var noteSeq by remember { mutableStateOf(0) }
    val dragged = remember { floatArrayOf(0f) }
    val brightnessAllowed = remember { device.canSetBrightness() }
    val dndAvailable by produceState(false) { value = withContext(Dispatchers.IO) { PanelToggles.dndAvailable(context) } }
    val controlsNow by rememberUpdatedState(controls)
    val thermalNow by rememberUpdatedState(thermal)

    val lNotif = stringResource(R.string.neon_tool_notifications)
    val lCalls = stringResource(R.string.neon_tool_calls)
    val lRec = stringResource(R.string.neon_tool_record)
    val lGameMode = stringResource(R.string.neon_tool_gamemode)
    val lBright = stringResource(R.string.neon_tool_brightness)
    val lColors = stringResource(R.string.neon_tool_colors)
    val lSound = stringResource(R.string.neon_tool_sound)
    val lClean = stringResource(R.string.neon_tool_clean)
    val lVeil = stringResource(R.string.game_panel_veil)
    val veilWarm = stringResource(R.string.game_panel_veil_warm)
    val veilNight = stringResource(R.string.game_panel_veil_night)
    val veilDim = stringResource(R.string.game_panel_veil_dim)
    val lHudRec = stringResource(R.string.game_panel_tool_record)
    val fmtOn = stringResource(R.string.game_panel_msg_on)
    val fmtOff = stringResource(R.string.game_panel_msg_off)
    val fmtFreed = stringResource(R.string.game_panel_msg_freed)
    val msgFreedNone = stringResource(R.string.game_panel_msg_freed_none)
    val msgNeedsRoot = stringResource(R.string.game_panel_msg_needs_root)
    val fmtWriteRefused = stringResource(R.string.game_panel_msg_write_refused)
    val fmtWriteFailed = stringResource(R.string.game_panel_msg_write_failed)
    val msgRecFailed = stringResource(R.string.game_panel_msg_rec_failed)
    val msgSaved = stringResource(R.string.game_panel_msg_saved)
    val recConsentTitle = stringResource(R.string.game_panel_rec_consent_title)
    val recConsentBody = stringResource(R.string.game_panel_rec_consent_body)
    val recConsentGo = stringResource(R.string.game_panel_rec_consent_go)
    val lCancel = stringResource(R.string.cancel)
    val msgNotifOn = stringResource(R.string.neon_msg_notif_on)
    val msgCallsOn = stringResource(R.string.neon_msg_calls_on)
    val msgDndOff = stringResource(R.string.neon_msg_dnd_off)
    val msgNeedDnd = stringResource(R.string.neon_msg_need_dnd)
    val msgProfileAuto = stringResource(R.string.neon_msg_profile_auto)
    val msgBrightFixed = stringResource(R.string.neon_msg_brightness_fixed)
    val msgBrightAuto = stringResource(R.string.neon_msg_brightness_auto)
    val msgNeedWrite = stringResource(R.string.neon_msg_need_write_settings)
    val msgUnavailable = stringResource(R.string.neon_unavailable)
    val barReady = stringResource(R.string.neon_bar_ready)
    val barReadySub = stringResource(R.string.neon_bar_ready_sub)
    val barWorking = stringResource(R.string.neon_bar_working)
    val barFailed = stringResource(R.string.neon_bar_failed)
    val fmtThermalApplied = stringResource(R.string.game_panel_thermal_msg_applied)
    val msgThermalPending = stringResource(R.string.game_panel_thermal_msg_pending)
    val sApplying = stringResource(R.string.game_panel_thermal_applying)
    val sPending = stringResource(R.string.game_panel_thermal_pending)
    val sNotApplied = stringResource(R.string.game_panel_thermal_failed_sub)
    val rUnsupported = stringResource(R.string.game_panel_thermal_reason_unsupported)
    val rHeld = stringResource(R.string.game_panel_thermal_reason_held)
    val rSave = stringResource(R.string.game_panel_thermal_reason_save)
    val rGeneric = stringResource(R.string.game_panel_thermal_reason_generic)
    val modeTitles = mapOf(
        GameThermalProfiles.DEFAULT to stringResource(R.string.neon_mode_default),
        "power" to stringResource(R.string.neon_mode_saver),
        "balanced" to stringResource(R.string.neon_mode_balanced),
        "gaming" to stringResource(R.string.neon_mode_gaming),
        "performance" to stringResource(R.string.neon_mode_performance)
    )

    fun veilToneLabel(tone: DisplayVeilTone): String = when (tone) {
        DisplayVeilTone.WARM -> veilWarm
        DisplayVeilTone.NIGHT -> veilNight
        DisplayVeilTone.DIM -> veilDim
    }

    fun hold() { holdUntil = SystemClock.uptimeMillis() + 1600 }

    fun say(text: String, kind: NoteKind = NoteKind.Ok, ttlMs: Long = NEON_NOTE_MS) {
        note = text
        noteKind = kind
        noteSeq += 1
        val mine = noteSeq
        scope.launch {
            delay(ttlMs)
            if (noteSeq == mine) note = null
        }
    }

    fun leave(after: () -> Unit) {
        if (leaving) return
        leaving = true
        scope.launch {
            enter.animateTo(0f, tween(NEON_LEAVE_MS))
            after()
        }
    }

    fun reasonText(reason: String): String = when {
        reason == "save-failed" -> rSave
        reason.contains("no-gpu-provider") || reason.contains("not-writable") || reason.contains("unsupported") -> rUnsupported
        reason.contains("held") || reason.contains("suppress") || reason.contains("blocked") || reason.contains("lock") -> rHeld
        else -> rGeneric
    }

    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 170f)) }

    // الحالة الحيّة من الجهاز نفسه كل ثانية، وتتوقف أثناء «تثبيت» تغيير للتوّ أُجري (فلا ترتدّ البلاطة قبل أن يُكتب).
    LaunchedEffect(Unit) {
        while (true) {
            if (SystemClock.uptimeMillis() >= holdUntil) {
                val z = withContext(Dispatchers.IO) { DndControl.currentZen(context) }
                val rec = withContext(Dispatchers.IO) { PanelScreenRecorder.isRecording() }
                val ab = withContext(Dispatchers.IO) { device.autoBrightness() }
                zen = z
                screenRec = rec
                autoBright = ab
                soundOn = AudioEffectService.isRunning && AudioEffectService.activePreset == AudioSoundPreset.GAME
            }
            delay(1000)
        }
    }

    // الطيّ عند الخمول، إلا أثناء تنفيذ جارٍ أو نافذة إقرار مفتوحة.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (!consentOpen && thermalNow.phase != ThermalPhase.Applying &&
                CockpitModel.shouldCollapseIdle(SystemClock.uptimeMillis(), activeAt)
            ) {
                if (!leaving) {
                    leaving = true
                    enter.animateTo(0f, tween(NEON_LEAVE_MS))
                    onCollapse()
                }
                break
            }
        }
    }

    LaunchedEffect(launchReceipt) {
        if (launchReceipt != null) say(launchReceipt, NoteKind.Info, NEON_RECEIPT_MS)
    }

    // إخفاق كتابة مقبض يُقال باسمه: لا يعود المقبض صامتًا.
    LaunchedEffect(Unit) {
        PanelToggles.failures.collect { failure ->
            hold()
            val label = when (failure.key) {
                "dnd" -> lNotif
                else -> failure.key
            }
            say(String.format(if (failure.refused) fmtWriteRefused else fmtWriteFailed, label), NoteKind.Fail)
        }
    }

    fun beginCapture() {
        screenRec = true
        hold()
        say(String.format(fmtOn, lRec))
        PanelToggles.async { PanelScreenRecorder.start(context) }
        scope.launch {
            // الحالة تُقرأ من الخدمة نفسها: لم تبدأ ⇒ تُصحَّح البلاطة ويُعلن الفشل بدل «بدأ التسجيل».
            delay(1400)
            val live = withContext(Dispatchers.IO) { PanelScreenRecorder.isRecording() }
            if (!live) {
                screenRec = false
                hold()
                say(msgRecFailed, NoteKind.Fail)
            } else {
                leave(onCollapse)
            }
        }
    }

    fun onTool(tool: NeonTool) {
        activeAt = SystemClock.uptimeMillis()
        when (tool) {
            NeonTool.Notifications, NeonTool.Calls -> {
                if (!dndAvailable) {
                    say(msgNeedDnd, NoteKind.Fail)
                    return
                }
                val calls = tool == NeonTool.Calls
                val on = if (calls) zen == 2 else zen == 1 || zen == 3
                val want = if (on) 0 else if (calls) 2 else 1
                hold()
                zen = want
                say(if (want == 0) msgDndOff else if (calls) msgCallsOn else msgNotifOn)
                PanelToggles.setDndZen(context, want)
            }

            NeonTool.ScreenRecord -> {
                hold()
                if (screenRec) {
                    screenRec = false
                    say(msgSaved)
                    PanelToggles.async { PanelScreenRecorder.stop(context) }
                } else if (!CaptureConsentPrefs.acknowledged(context)) {
                    consentOpen = true
                } else {
                    beginCapture()
                }
            }

            NeonTool.GameMode -> {
                if (controls.auto) {
                    say(msgProfileAuto, NoteKind.Info)
                    return
                }
                val on = controls.profile == "1"
                val wanted = if (on) "2" else "1"
                hold()
                say(String.format(if (on) fmtOff else fmtOn, lGameMode))
                onSelectProfile(wanted)
                scope.launch {
                    delay(1800)
                    if (controlsNow.profile != wanted) say(String.format(fmtWriteFailed, lGameMode), NoteKind.Fail)
                }
            }

            NeonTool.Brightness -> {
                val current = autoBright
                if (current == null || !brightnessAllowed) {
                    say(msgNeedWrite, NoteKind.Fail)
                    return
                }
                val wantAuto = !current
                hold()
                autoBright = wantAuto
                say(if (wantAuto) msgBrightAuto else msgBrightFixed)
                scope.launch {
                    val after = withContext(Dispatchers.IO) {
                        device.setAutoBrightness(wantAuto)
                        delay(900)
                        device.autoBrightness()
                    }
                    autoBright = after
                    if (after != wantAuto) say(String.format(fmtWriteFailed, lBright), NoteKind.Fail)
                }
            }

            NeonTool.Colors -> {
                val next = if (!veilEnabled) DisplayVeilTone.WARM else veilTone.next()
                val off = veilEnabled && next == DisplayVeilTone.WARM
                hold()
                say(String.format(if (off) fmtOff else fmtOn, if (off) lVeil else "$lVeil · ${veilToneLabel(next)}"))
                onCycleVeil()
            }

            NeonTool.Sound -> {
                hold()
                val want = !soundOn
                soundOn = want
                say(String.format(if (want) fmtOn else fmtOff, lSound))
                if (want) AudioEffectService.setPreset(context, AudioSoundPreset.GAME) else AudioEffectService.stop(context)
                scope.launch {
                    delay(1200)
                    val live = AudioEffectService.isRunning && AudioEffectService.activePreset == AudioSoundPreset.GAME
                    soundOn = live
                    if (live != want) say(String.format(fmtWriteFailed, lSound), NoteKind.Fail)
                }
            }

            NeonTool.Clean -> {
                if (cleaning) return
                cleaning = true
                scope.launch {
                    val freed = withContext(Dispatchers.IO) { device.cleanMemory() }
                    cleaning = false
                    when {
                        freed == null -> say(msgNeedsRoot, NoteKind.Fail)
                        freed > 0 -> say(String.format(fmtFreed, freed))
                        else -> say(msgFreedNone, NoteKind.Info)
                    }
                }
            }

            NeonTool.TouchLock, NeonTool.Network -> say(msgUnavailable, NoteKind.Info)
        }
    }

    fun pickThermal(id: String) {
        activeAt = SystemClock.uptimeMillis()
        if (id == thermal.selected && thermal.phase != ThermalPhase.Failed && thermal.phase != ThermalPhase.Pending) return
        onSelectThermal(id)
    }

    val tabAvailable = listOf(true, false, false, true, false, true)
    fun onTab(index: Int) {
        activeAt = SystemClock.uptimeMillis()
        when (index) {
            0 -> Unit
            3 -> {
                hold()
                say(String.format(if (recording) fmtOff else fmtOn, lHudRec))
                onToggleRecording()
            }
            5 -> {
                onOpenControls()
                leave(onCollapse)
            }
            else -> say(msgUnavailable, NoteKind.Info)
        }
    }

    // ───── شريط الحالة: ما يُقال هنا حقيقيّ دائمًا ─────
    val selectedName = modeTitles[thermal.selected] ?: thermal.selected
    val liveNote = note
    val bar: NeonBar = when {
        cleaning -> NeonBar(NeonBarKind.Working, barWorking, lClean, null)
        thermal.phase == ThermalPhase.Applying -> NeonBar(NeonBarKind.Working, barWorking, selectedName, null)
        liveNote != null && noteKind == NoteKind.Ok -> NeonBar(NeonBarKind.Done, liveNote, selectedName, 100)
        liveNote != null && noteKind == NoteKind.Fail -> NeonBar(NeonBarKind.Failed, barFailed, liveNote, null)
        liveNote != null -> NeonBar(NeonBarKind.Idle, liveNote, selectedName, null)
        thermal.phase == ThermalPhase.Applied -> NeonBar(
            NeonBarKind.Done,
            String.format(fmtThermalApplied, selectedName),
            thermal.liveMhz?.let { "$it MHz" } ?: barReadySub,
            100
        )
        thermal.phase == ThermalPhase.Pending -> NeonBar(NeonBarKind.Idle, msgThermalPending, selectedName, null)
        thermal.phase == ThermalPhase.Failed -> NeonBar(NeonBarKind.Failed, barFailed, reasonText(thermal.reason), null)
        else -> NeonBar(NeonBarKind.Idle, barReady, barReadySub, null)
    }

    val cpuPct = clocks.cpuLoadPct ?: reading?.cpu
    val gpuPct = clocks.gpuLoadPct ?: reading?.gpuLoadPercent
    val tempC = reading?.socTempC ?: reading?.heat
    val boostOn = controls.profile == "1" ||
        (thermal.selected == "performance" && thermal.phase == ThermalPhase.Applied)
    val labels = listOf(
        stringResource(R.string.neon_tab_performance),
        stringResource(R.string.neon_tab_tools),
        stringResource(R.string.neon_tab_services),
        stringResource(R.string.neon_tab_record),
        stringResource(R.string.neon_tab_live),
        stringResource(R.string.neon_tab_settings)
    )
    val subtitle = stringResource(R.string.neon_subtitle)
    val infoTitle = stringResource(R.string.neon_section_info)
    val toolsTitle = stringResource(R.string.neon_section_tools)
    // اللوحة تلتصق بحافة الهاتف وقائمة تبويباتها تلاصق تلك الحافة: على اليسار كما في الأصل، وعلى اليمين
    // يُعكس التخطيط كلّه (والنصوص والأيقونات والعدّادات تبقى سويّة — انظر [LocalNeonMirror]).
    val mirror = side == PanelSide.End
    // اهتزاز خفيف عند اللمس: تأكيد ملموس لأيّ زرّ ضُغط، ولا يعتمد على رؤية الشاشة أثناء اللعب
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(top = statusTop.dp)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        activeAt = SystemClock.uptimeMillis()
                    }
                }
            }
            .pointerInput(Unit) { detectTapGestures { leave(onCollapse) } }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragged[0] = 0f },
                    onVerticalDrag = { _, dy -> dragged[0] += dy },
                    onDragEnd = { if (abs(dragged[0]) > 160f) leave(onCollapse) }
                )
            }
    ) {
        Layout(
            content = {
                // داخل فضاء التصميم: Density = 1 فتتّحد وحدات dp وsp والرسم مع إحداثيات الصورة.
                CompositionLocalProvider(
                    LocalDensity provides Density(1f, 1f),
                    LocalNeonMirror provides mirror
                ) {
                    // قلب أفقيّ واحد لفضاء التصميم كلّه عند الالتصاق باليمين (يشمل اللمس: الإحداثيات تُقلب معه).
                    Box(Modifier.size(NEON_W.dp, NEON_H.dp).upright(mirror)) {
                        NeonFrame()
                        NeonHeader(
                            boostOn = boostOn,
                            tempText = tempC?.let { "${it.roundToInt()}°C" } ?: nd.max.ui.design.MAX_VALUE_UNAVAILABLE,
                            tempC = tempC,
                            subtitle = subtitle
                        )
                        NeonSidebar(labels = labels, available = tabAvailable, recordingLive = recording, onTab = ::onTab)
                        NeonGauges(
                            title = infoTitle,
                            cpuPct = cpuPct,
                            cpuTag = PanelClockMath.cpuTag(clocks.cpuShownMhz),
                            gpuPct = gpuPct,
                            gpuTag = PanelClockMath.gpuTag(clocks.gpuShownMhz),
                            fps = reading?.frames?.roundToInt(),
                            frames = frames,
                            refreshHz = refreshRateHz
                        )
                        NeonModes(
                            selected = thermal.selected,
                            targets = thermal.targetsMhz,
                            applying = thermal.phase == ThermalPhase.Applying || thermal.phase == ThermalPhase.Pending,
                            statusFor = { id ->
                                if (id != thermal.selected) null else when {
                                    thermal.phase == ThermalPhase.Applying -> sApplying to false
                                    thermal.phase == ThermalPhase.Pending -> sPending to false
                                    thermal.phase == ThermalPhase.Failed -> sNotApplied to true
                                    thermal.phase == ThermalPhase.Applied && thermal.liveMhz != null -> "${thermal.liveMhz} MHz" to false
                                    else -> null
                                }
                            },
                            onPick = { id ->
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                pickThermal(id)
                            }
                        )
                        NeonTools(
                            title = toolsTitle,
                            state = { tool ->
                                when (tool) {
                                    NeonTool.Notifications -> NeonToolState(zen == 1 || zen == 3, dndAvailable)
                                    NeonTool.Calls -> NeonToolState(zen == 2, dndAvailable)
                                    NeonTool.ScreenRecord -> NeonToolState(screenRec, true)
                                    NeonTool.GameMode -> NeonToolState(controls.profile == "1", !controls.auto)
                                    NeonTool.Brightness -> NeonToolState(autoBright == false, brightnessAllowed && autoBright != null)
                                    NeonTool.Colors -> NeonToolState(veilEnabled, true)
                                    NeonTool.Sound -> NeonToolState(soundOn, true)
                                    NeonTool.Clean -> NeonToolState(false, true, busy = cleaning)
                                    NeonTool.TouchLock, NeonTool.Network -> NeonToolState(false, false)
                                }
                            },
                            onTool = { tool ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTool(tool)
                            }
                        )
                        NeonStatusBar(bar.kind, bar.title, bar.sub, bar.percent)
                        if (consentOpen) {
                            NeonConfirm(
                                title = recConsentTitle,
                                body = recConsentBody,
                                cancel = lCancel,
                                go = recConsentGo,
                                onCancel = { consentOpen = false },
                                onGo = {
                                    CaptureConsentPrefs.acknowledge(context)
                                    consentOpen = false
                                    beginCapture()
                                }
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .align(if (mirror) Alignment.CenterEnd else Alignment.CenterStart)
                .graphicsLayer {
                    val p = enter.value
                    alpha = p.coerceIn(0f, 1f)
                    scaleX = 0.92f + 0.08f * p
                    scaleY = 0.92f + 0.08f * p
                    transformOrigin = TransformOrigin(origin.x, origin.y)
                }
                // اللمس داخل اللوحة لا يطويها: الطيّ لمسٌ في الفراغ حولها فقط.
                .pointerInput(Unit) { detectTapGestures { } }
        ) { measurables, constraints ->
            val margin = 8.dp.roundToPx()
            val availW = (constraints.maxWidth - 2 * margin).coerceAtLeast(1)
            val availH = (constraints.maxHeight - 2 * margin).coerceAtLeast(1)
            val s = min(availW / NEON_W, availH / NEON_H).coerceAtMost(NEON_MAX_SCALE)
            val placeable = measurables.first().measure(
                Constraints.fixed(NEON_W.roundToInt(), NEON_H.roundToInt())
            )
            layout((NEON_W * s).roundToInt() + 2 * margin, (NEON_H * s).roundToInt() + 2 * margin) {
                placeable.placeWithLayer(margin, margin) {
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0f, 0f)
                }
            }
        }
    }
}

/** نافذة إقرار داخل الشجرة (لا `Dialog`: نافذة الخدمة بلا Activity token فيسقط بـ`BadTokenException`). */
@Composable
private fun NeonConfirm(
    title: String,
    body: String,
    cancel: String,
    go: String,
    onCancel: () -> Unit,
    onGo: () -> Unit
) {
    Box(
        Modifier
            .size(NEON_W.dp, NEON_H.dp)
            .background(Color(0xCC000000))
            .neonClick { onCancel() }
    )
    Box(
        Modifier
            .at(235f, 436f, 640f, 400f)
            .drawBehind {
                val shape = chamfer(0f, 0f, 640f, 400f, 18f)
                drawPath(shape, Color(0xFF05070F))
                drawPath(shape, Brush.verticalGradient(0f to C.Red.copy(alpha = 0.16f), 1f to Color.Transparent))
                glowStroke(shape, C.Red, 2.8f, glow = 1.4f)
            }
            .neonClick { }
    )
    NeonText(title, 267f, 456f, 576f, 56f, 34f, weight = FontWeight.Bold)
    Text(
        text = body,
        color = C.InkDim,
        fontSize = 24.sp,
        maxLines = 6,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.at(267f, 524f, 576f, 190f).upright(LocalNeonMirror.current)
    )
    for (i in 0..1) {
        val x = if (i == 0) 267f else 575f
        val hot = i == 1
        Box(
            Modifier
                .at(x, 744f, 268f, 72f)
                .drawBehind {
                    val shape = chamfer(0f, 0f, 268f, 72f, 10f)
                    val c = if (hot) C.Green else C.BlueLine
                    drawPath(shape, c.copy(alpha = if (hot) 0.28f else 0.14f))
                    glowStroke(shape, c, 2.2f, glow = 1f)
                }
                .neonClick(label = if (hot) go else cancel) { if (hot) onGo() else onCancel() }
        )
        NeonText(
            if (hot) go else cancel, x, 744f, 268f, 72f, 26f,
            weight = FontWeight.Medium, align = TextAlign.Center
        )
    }
}
