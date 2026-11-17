/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.component

import android.content.Context
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
// `VolumeUp` من أيقونات Material الاتجاهية (السمّاعة تنعكس في RTL) ⇒ تُستورد من `automirrored`،
// وهو النمط القائم في المستودع نفسه (`IslandUi.kt`). بوّابة RTL أسقطت التشغيل على الصيغة القديمة.
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.gamespace.CockpitModel
import nd.max.core.gamespace.DisplayVeilTone
import nd.max.core.gamespace.HudField
import nd.max.core.gamespace.HudReading
import nd.max.core.gamespace.HudTally
import nd.max.core.gamespace.PanelClockMath
import nd.max.core.gamespace.PanelClocks
import nd.max.core.gamespace.PanelControlState
import nd.max.core.gamespace.PanelDevice
import nd.max.core.gamespace.PanelReminder
import nd.max.core.gamespace.PanelScreenshot
import nd.max.core.gamespace.PanelScreenRecorder
import nd.max.core.gamespace.PanelSide
import nd.max.core.gamespace.PanelToggles
import nd.max.core.gamespace.ThermalPanelState
import nd.max.service.FpsOverlayService
import nd.max.ui.util.CaptureConsentPrefs

/**
 * تصميم «Game Turbo» الثاني للوحة الألعاب — طبق الأصل من المرجع الذي أمر به المالك.
 *
 * ### والعقد نفسه لا يتبدّل
 * التوقيع مطابق لـ[GamePanelCockpit] حرفيًّا: كل ما يظهر هنا يقف على **مقابض حقيقية** قائمة —
 * الأوضاع «١/٢/٣» تُمرَّر إلى `onSelectProfile` كما ترفضها الخدمة في الوضع التلقائي، وأدوات اللعب
 * هي مفاتيح `PanelToggles`/`PanelScreenRecorder`/`PanelScreenshot` نفسها. لا مفتاح وهميّ واحد
 * (§0.5/ADR-07) — والشكل وحده هو ما تُقله هذه النسخة.
 *
 * ### والألعاب لا تُزاح
 * الجسم يملأ نافذة اللوحة نفسها (حجمها تحدّده الخدمة)، واللمس في الخلفية أو سحب عمودي كبير
 * يطويها، والخمول أطول من [CockpitModel.IDLE_COLLAPSE_MS] يطويها أيضًا — كما في الكلاسيكي.
 * وقراءة الحالة الحقيقية كل ٨٠٠ ملّي مع توقّف قصير بعد أي لمس متوقّع — النسخة نفسها في الكلاسيكي.
 */
@Composable
internal fun GamePanelTurbo(
    gameLabel: String,
    reading: HudReading?,
    tally: HudTally?,
    fields: List<HudField>,
    accent: Color,
    side: PanelSide,
    recording: Boolean,
    refreshRateHz: Int?,
    clocks: PanelClocks,
    controls: PanelControlState,
    frames: List<Float>,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    onCycleRefresh: () -> Unit,
    onToggleRecording: () -> Unit,
    onOpenControls: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onToggleBypass: () -> Unit,
    crosshairEnabled: Boolean = false,
    onToggleCrosshair: () -> Unit = {},
    /** ستارة العرض: لون شفّاف فوق اللعبة — لا معالجة صورة. */
    veilEnabled: Boolean = false,
    veilTone: DisplayVeilTone = DisplayVeilTone.WARM,
    onCycleVeil: () -> Unit = {},
    /** رفع/إعادة تركيز نافذة اللوحة: عقد الكلاسيكي نفسه؛ لا حقل نصّي في هذا التصميم بعد. */
    onTyping: (Boolean) -> Unit = {},
    /** الحزمة المتتبَّعة: إضافات اللعبة تُدار من الكلاسيكي حاليًّا. */
    trackedPackage: String? = null,
    thermal: ThermalPanelState = ThermalPanelState(),
    onSelectThermal: (String) -> Unit = {},
    refreshChoices: List<Int> = emptyList(),
    onSelectRefresh: (Int?) -> Unit = {},
    origin: Offset = Offset(1f, 0.5f),
    /** إيصال الإطلاق: ماذا فُعل بملفّ اللعبة فعلًا. */
    launchReceipt: String? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val statusTop = WindowInsets.statusBars.getTop(LocalDensity.current) / density
    val device = remember { PanelDevice(context) }
    val scope = rememberCoroutineScope()
    val enter = remember { Animatable(0f) }
    var leaving by remember { mutableStateOf(false) }
    var touchedAt by remember { mutableStateOf(SystemClock.uptimeMillis()) }
    var holdUntil by remember { mutableStateOf(0L) }
    var section by remember { mutableStateOf(TurboSection.Perf) }
    var status by remember { mutableStateOf(device.status()) }
    var brightness by remember { mutableStateOf(device.brightness()) }
    var volume by remember { mutableStateOf(device.volume()) }
    val brightnessAllowed = remember { device.canSetBrightness() }
    var rotationLocked by remember { mutableStateOf(device.rotationLocked()) }
    var wifi by remember { mutableStateOf(false) }
    var mobile by remember { mutableStateOf(true) }
    var dnd by remember { mutableStateOf(false) }
    // متاحية المفاتيح تُقرأ خارج الخيط الأوسط (قد تستعلم النظام) — كما في الكلاسيكي.
    val touchAvailable by produceState(false) {
        value = withContext(Dispatchers.IO) { PanelToggles.touchAvailable() }
    }
    val dndAvailable by produceState(false) {
        value = withContext(Dispatchers.IO) { PanelToggles.dndAvailable(context) }
    }
    var touch by remember { mutableStateOf(false) }
    var screenRec by remember { mutableStateOf(false) }
    var fpsOverlay by remember { mutableStateOf(FpsOverlayService.isRunning) }
    var reminderMin by remember { mutableStateOf(0) }
    var optimizing by remember { mutableStateOf(false) }
    val hasCellular = remember { PanelToggles.hasCellular(context) }

    val notes = remember { mutableStateListOf<String>() }

    // النصوص تُلتقط في التأليف: المُساعدات خارج التأليف لا تقرأ الموارد.
    val sBoosting = stringResource(R.string.game_panel_turbo_boosting)
    val sBoostDone = stringResource(R.string.game_panel_turbo_boost_done)
    val sNeedsRoot = stringResource(R.string.game_panel_msg_needs_root)
    val sFreedNone = stringResource(R.string.game_panel_msg_freed_none)
    val sRecFailed = stringResource(R.string.game_panel_msg_rec_failed)
    val sRecOn = stringResource(R.string.game_panel_tool_screenrec_on)
    val sShotSaved = stringResource(R.string.game_panel_msg_shot_saved)
    val sShotFailed = stringResource(R.string.game_panel_msg_shot_failed)
    val sReminderDone = stringResource(R.string.game_panel_reminder_done)
    val sPerf = stringResource(R.string.profile_performance)
    val sBalanced = stringResource(R.string.profile_balanced)
    val sPower = stringResource(R.string.profile_powersave)
    val sAuto = stringResource(R.string.str_auto_mode)

    fun say(text: String) {
        notes.add(text)
        scope.launch {
            delay(TurboNoteMs)
            notes.remove(text)
        }
    }

    fun hold() {
        holdUntil = SystemClock.uptimeMillis() + TurboHoldMs
    }

    fun leave(after: () -> Unit) {
        if (leaving) return
        leaving = true
        scope.launch {
            enter.animateTo(0f, tween(TurboLeaveMs))
            after()
        }
    }

    /** كتابة متوقّعة: الحالة تُقلَب فورًا والكتابة الحقيقية خارج الخيط الأوسط، والقارئ يُوقَف لحظة. */
    fun flip(target: Boolean, set: (Boolean) -> Unit, write: suspend () -> Unit) {
        set(target)
        hold()
        PanelToggles.async { write() }
    }

    fun boost() {
        if (optimizing) return
        optimizing = true
        say(sBoosting)
        scope.launch {
            val freed = withContext(Dispatchers.IO) { device.cleanMemory() }
            optimizing = false
            when {
                freed == null -> say(sNeedsRoot)
                freed > 0 -> say(sBoostDone)
                else -> say(sFreedNone)
            }
        }
    }

    fun toggleScreenRec() {
        if (screenRec) {
            screenRec = false
            hold()
            PanelToggles.async { PanelScreenRecorder.stop(context) }
            return
        }
        // أول تسجيل يمرّ من إعدادات اللوحة حيث يُقرأ الإقرار؛ لا تسجيل بلا إقرار.
        if (!CaptureConsentPrefs.acknowledged(context)) {
            leave(onOpenControls)
            return
        }
        screenRec = true
        say(sRecOn)
        PanelToggles.async { PanelScreenRecorder.start(context) }
        scope.launch {
            delay(TurboRecVerifyMs)
            val live = withContext(Dispatchers.IO) { PanelScreenRecorder.isRecording() }
            if (!live) {
                screenRec = false
                say(sRecFailed)
            } else {
                leave(onCollapse)
            }
        }
    }

    fun shoot() {
        hold()
        scope.launch {
            val path = withContext(Dispatchers.IO) { PanelScreenshot.capture() }
            if (path != null) say(sShotSaved) else say(sShotFailed)
        }
    }

    fun toggleRotation() {
        val target = !rotationLocked
        rotationLocked = target
        hold()
        PanelToggles.async { device.setRotationLock(target) }
        scope.launch {
            delay(TurboRotationVerifyMs)
            rotationLocked = device.rotationLocked()
        }
    }

    fun toggleFpsOverlay() {
        val target = !fpsOverlay
        fpsOverlay = target
        hold()
        PanelToggles.async {
            if (target) FpsOverlayService.start(context) else FpsOverlayService.stop(context)
        }
        scope.launch {
            delay(TurboFpsVerifyMs)
            fpsOverlay = FpsOverlayService.isRunning
        }
    }

    fun cycleReminder() {
        hold()
        reminderMin = PanelReminder.cycle(context, sReminderDone)
    }

    // الحلقة القارئة: الحالة الحقيقية كل ٨٠٠ ملّي، والتفصيل كل ثالث نبضة (كما في الكلاسيكي).
    LaunchedEffect(Unit) {
        var tick = 0
        while (true) {
            delay(TurboProbeMs)
            tick++
            status = device.status()
            rotationLocked = device.rotationLocked()
            if (tick % 3 == 0 && SystemClock.uptimeMillis() > holdUntil) {
                wifi = PanelToggles.wifiOn(context)
                mobile = PanelToggles.dataOn(context)
                dnd = PanelToggles.dndOn(context)
                if (touchAvailable) touch = PanelToggles.touchOn()
                screenRec = PanelScreenRecorder.isRecording()
                reminderMin = PanelReminder.remainingMin()
                fpsOverlay = FpsOverlayService.isRunning
            }
        }
    }
    LaunchedEffect(Unit) {
        enter.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 170f))
    }
    // الخمول يطوي: اللمس الأخير هو ما يعيد العدّ، لا شيء آخر.
    LaunchedEffect(Unit) {
        while (true) {
            delay(TurboIdleCheckMs)
            if (!leaving && CockpitModel.shouldCollapseIdle(SystemClock.uptimeMillis(), touchedAt)) {
                leave(onCollapse)
            }
        }
    }

    val probe = remember(wifi, mobile, dnd, touch, screenRec, fpsOverlay, reminderMin) {
        TurboProbe(wifi, mobile, dnd, touch, screenRec, fpsOverlay, reminderMin)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(TurboPalette.Back)
            .padding(top = statusTop.dp)
            .graphicsLayer { alpha = enter.value }
            // اللمس في الخلفية يطوي — وأي عنصر قابل للنقر يستهلك لمسته فلا يصل إلى هنا.
            .pointerInput(Unit) {
                detectTapGestures { leave(onCollapse) }
            }
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = {
                        touchedAt = SystemClock.uptimeMillis()
                        total = 0f
                    },
                    onDrag = { _, amount ->
                        total += abs(amount)
                        if (total > TurboCollapseDragPx) {
                            total = -1f
                            leave(onCollapse)
                        }
                    }
                )
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            TurboRail(
                selected = section,
                onSelect = {
                    section = it
                    touchedAt = SystemClock.uptimeMillis()
                },
                onSettings = { leave(onOpenControls) }
            )
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                TurboHeader(
                    gameLabel = gameLabel,
                    heatC = reading?.heat,
                    optimizing = optimizing,
                    onBoost = { boost() }
                )
                Box(modifier = Modifier.weight(1f)) {
                    when (section) {
                        TurboSection.Perf -> TurboPerfSection(
                            clocks = clocks,
                            reading = reading,
                            frames = frames,
                            refreshRateHz = refreshRateHz,
                            controls = controls,
                            onSelectProfile = { id ->
                                onSelectProfile(id)
                                hold()
                            }
                        )
                        TurboSection.Tools -> TurboToolsSection(
                            probe = probe,
                            dndAvailable = dndAvailable,
                            touchAvailable = touchAvailable,
                            rotationLocked = rotationLocked,
                            fpsOverlay = fpsOverlay,
                            veilEnabled = veilEnabled,
                            recording = recording,
                            screenRec = screenRec,
                            onDnd = { target -> flip(target, { dnd = target }) { PanelToggles.setDnd(context, target) } },
                            onTouch = { target -> flip(target, { touch = target }) { PanelToggles.setTouch(target) } },
                            onBypass = {
                                onToggleBypass()
                                hold()
                            },
                            onRotation = { toggleRotation() },
                            onClean = { boost() },
                            onReminder = { cycleReminder() },
                            onFps = { toggleFpsOverlay() },
                            onVeil = {
                                onCycleVeil()
                                hold()
                            },
                            onRecord = { toggleScreenRec() },
                            onShot = { shoot() }
                        )
                        TurboSection.Services -> TurboServicesSection(
                            probe = probe,
                            hasCellular = hasCellular,
                            brightnessAllowed = brightnessAllowed,
                            brightness = brightness,
                            volume = volume,
                            crosshairEnabled = crosshairEnabled,
                            onWifi = { target -> flip(target, { wifi = target }) { PanelToggles.setWifi(context, target) } },
                            onData = { target -> flip(target, { mobile = target }) { PanelToggles.setData(context, target) } },
                            onCrosshair = {
                                onToggleCrosshair()
                                hold()
                            },
                            onBrightness = { value -> brightness = value },
                            onBrightnessCommit = {
                                if (brightnessAllowed) {
                                    PanelToggles.async { device.setBrightness(brightness) }
                                }
                            },
                            onVolume = { value -> volume = value },
                            onVolumeCommit = {
                                PanelToggles.async { device.setVolume(volume) }
                            }
                        )
                        TurboSection.Record -> TurboRecordSection(
                            recording = recording,
                            screenRec = screenRec,
                            reminderMin = reminderMin,
                            onHudRecord = {
                                onToggleRecording()
                                hold()
                            },
                            onScreenRec = { toggleScreenRec() },
                            onShot = { shoot() },
                            onReminder = { cycleReminder() }
                        )
                    }
                }
                TurboBottomBar(
                    gameLabel = gameLabel,
                    profileLabel = profileLabel(controls, sPerf, sBalanced, sPower, sAuto),
                    note = notes.lastOrNull(),
                    batteryPct = status.batteryPct,
                    charging = status.charging,
                    frames = frames,
                    optimizing = optimizing,
                    onBoost = { boost() }
                )
            }
        }
    }
}

private fun profileLabel(
    controls: PanelControlState,
    perf: String,
    balanced: String,
    power: String,
    auto: String
): String? = when {
    controls.profile == "1" -> perf
    controls.profile == "2" -> balanced
    controls.profile == "3" -> power
    controls.auto -> auto
    else -> null
}

/** لقطة الحالة الحقيقية المارة إلى الشاشات — قيم مُقرؤة لا محسوبة. */
private data class TurboProbe(
    val wifi: Boolean,
    val mobile: Boolean,
    val dnd: Boolean,
    val touch: Boolean,
    val screenRec: Boolean,
    val fpsOverlay: Boolean,
    val reminderMin: Int
)

/** الشريط الجانبي: الأقسام الحقيقية الأربعة + الإعدادات (تفتح شاشة الإعدادات في التطبيق). */
@Composable
private fun TurboRail(
    selected: TurboSection,
    onSelect: (TurboSection) -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(TurboRailWidth)
            .background(TurboPalette.Rail)
            .padding(vertical = TurboPadMd),
        verticalArrangement = Arrangement.spacedBy(TurboPadXs)
    ) {
        railItems().forEach { item ->
            val label = stringResource(item.labelRes)
            val on = item.section == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TurboRailItemHeight)
                    .clip(TurboChamferSmall)
                    .background(if (on) TurboPalette.Red.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable(role = Role.Tab) {
                        if (item.section != null) onSelect(item.section) else onSettings()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = label,
                    tint = if (on) TurboPalette.Red else TurboPalette.Muted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = label,
                    color = if (on) TurboPalette.Ink else TurboPalette.Muted,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
        }
    }
}

private data class TurboRailItem(val section: TurboSection?, val icon: ImageVector, val labelRes: Int)

private fun railItems() = listOf(
    TurboRailItem(TurboSection.Perf, Icons.Rounded.Speed, R.string.game_panel_turbo_rail_perf),
    TurboRailItem(TurboSection.Tools, Icons.Rounded.Tune, R.string.game_panel_turbo_rail_tools),
    TurboRailItem(TurboSection.Services, Icons.Rounded.Wifi, R.string.game_panel_turbo_rail_services),
    TurboRailItem(TurboSection.Record, Icons.Rounded.Videocam, R.string.game_panel_turbo_rail_record),
    TurboRailItem(null, Icons.Rounded.Settings, R.string.game_panel_turbo_rail_settings)
)

/** الترويسة: العلامة الحمراء والعنوان وزر التسريع وشارة الحرارة — كلها بيانات حقيقية. */
@Composable
private fun TurboHeader(
    gameLabel: String,
    heatC: Float?,
    optimizing: Boolean,
    onBoost: () -> Unit
) {
    val brand = stringResource(R.string.game_panel_skin_turbo)
    val cut = brand.lastIndexOf(' ')
    val title = buildAnnotatedString {
        if (cut > 0) {
            append(brand.substring(0, cut + 1))
            withStyle(SpanStyle(color = TurboPalette.Red, fontStyle = FontStyle.Italic)) {
                append(brand.substring(cut + 1))
            }
        } else {
            withStyle(SpanStyle(color = TurboPalette.Red, fontStyle = FontStyle.Italic)) {
                append(brand)
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TurboPadMd, vertical = TurboPadSm)
            .clip(TurboChamfer)
            .background(TurboPalette.Plate)
            .border(PanelEdge, TurboPalette.Blue.copy(alpha = 0.35f), TurboChamfer)
            .padding(horizontal = TurboPadSm, vertical = TurboPadSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(TurboChamferSmall)
                .background(TurboPalette.Red),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(TurboPadSm))
        Column {
            Text(text = title, color = TurboPalette.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = gameLabel, color = TurboPalette.Muted, fontSize = 9.sp, maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        heatC?.let {
            Box(
                modifier = Modifier
                    .clip(TurboChamferSmall)
                    .border(PanelEdge, TurboPalette.Red, TurboChamferSmall)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$LtrMark${"%.0f".format(it)}°C$LtrMark",
                    color = TurboPalette.Red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(TurboPadSm))
        }
        Row(
            modifier = Modifier
                .clip(TurboChamferSmall)
                .background(TurboPalette.Red.copy(alpha = if (optimizing) 0.25f else 0.12f))
                .border(PanelEdge, TurboPalette.Red, TurboChamferSmall)
                .clickable(enabled = !optimizing, role = Role.Button, onClick = onBoost)
                .padding(horizontal = TurboPadSm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = TurboPalette.Red, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.game_panel_turbo_boost),
                color = TurboPalette.Red,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** قسم معلومات الأداء: ثلاثة عدّادات حقيقية ثم صفّ الأوضاع. */
@Composable
private fun TurboPerfSection(
    clocks: PanelClocks,
    reading: HudReading?,
    frames: List<Float>,
    refreshRateHz: Int?,
    controls: PanelControlState,
    onSelectProfile: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TurboPadMd, vertical = TurboPadSm),
        verticalArrangement = Arrangement.spacedBy(TurboPadSm)
    ) {
        TurboSectionHeader(stringResource(R.string.game_panel_turbo_perf_info), TurboPalette.Red)
        Row(horizontalArrangement = Arrangement.spacedBy(TurboPadSm)) {
            val cpuLoad = clocks.cpuLoadPct ?: reading?.cpu
            TurboGauge(
                label = stringResource(R.string.game_panel_turbo_gauge_cpu),
                value = cpuLoad?.let { "$LtrMark$it%$LtrMark" } ?: MaxValueUnavailable,
                caption = PanelClockMath.cpuTag(clocks.cpuShownMhz)?.let { "$LtrMark$it$LtrMark" },
                frac = (cpuLoad ?: 0) / 100f,
                tint = TurboPalette.Blue
            )
            TurboGauge(
                label = stringResource(R.string.game_panel_turbo_gauge_gpu),
                value = clocks.gpuLoadPct?.let { "$LtrMark$it%$LtrMark" } ?: MaxValueUnavailable,
                caption = PanelClockMath.gpuTag(clocks.gpuShownMhz)?.let { "$LtrMark$it$LtrMark" },
                frac = (clocks.gpuLoadPct ?: 0) / 100f,
                tint = TurboPalette.Red
            )
            val fpsNow = frames.lastOrNull() ?: refreshRateHz?.toFloat()
            TurboGauge(
                label = stringResource(R.string.game_panel_turbo_gauge_fps),
                value = fpsNow?.roundToInt()?.let { "$LtrMark$it$LtrMark" } ?: MaxValueUnavailable,
                caption = refreshRateHz?.let { "$LtrMark${it}Hz$LtrMark" },
                frac = (fpsNow ?: 0f) / TurboFpsReference,
                tint = TurboPalette.Blue,
                spark = frames
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(TurboPadSm)) {
            TurboModeTile(
                title = stringResource(R.string.profile_performance),
                tint = ModeRed,
                icon = Icons.Rounded.Bolt,
                selected = controls.profile == "1",
                enabled = !controls.auto,
                onClick = { onSelectProfile("1") }
            )
            TurboModeTile(
                title = stringResource(R.string.profile_balanced),
                tint = ModeAmber,
                icon = Icons.Rounded.SportsEsports,
                selected = controls.profile == "2",
                enabled = !controls.auto,
                onClick = { onSelectProfile("2") }
            )
            TurboModeTile(
                title = stringResource(R.string.profile_powersave),
                tint = ModeGreen,
                icon = Icons.Rounded.BatteryFull,
                selected = controls.profile == "3",
                enabled = !controls.auto,
                onClick = { onSelectProfile("3") }
            )
            // الوضع التلقائي حالة لا مفتاح: الذكاء يملك ملفّ الوضع، فلا نقر عليه ولا نجاح مُختلق.
            TurboModeTile(
                title = stringResource(R.string.str_auto_mode),
                tint = ModeAuto,
                icon = Icons.Rounded.Tune,
                selected = controls.auto,
                enabled = false,
                onClick = {}
            )
        }
    }
}

/** قسم أدوات اللعب: عشر بلاطات، كل واحدة مقبض حقيقي. */
@Composable
private fun TurboToolsSection(
    probe: TurboProbe,
    dndAvailable: Boolean,
    touchAvailable: Boolean,
    rotationLocked: Boolean,
    fpsOverlay: Boolean,
    veilEnabled: Boolean,
    recording: Boolean,
    screenRec: Boolean,
    onDnd: (Boolean) -> Unit,
    onTouch: (Boolean) -> Unit,
    onBypass: () -> Unit,
    onRotation: () -> Unit,
    onClean: () -> Unit,
    onReminder: () -> Unit,
    onFps: () -> Unit,
    onVeil: () -> Unit,
    onRecord: () -> Unit,
    onShot: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TurboPadMd, vertical = TurboPadSm),
        verticalArrangement = Arrangement.spacedBy(TurboPadSm)
    ) {
        TurboSectionHeader(stringResource(R.string.game_panel_turbo_tools_title), TurboPalette.Red)
        Row(horizontalArrangement = Arrangement.spacedBy(TurboPadSm)) {
            TurboToolTile(
                icon = Icons.Rounded.NotificationsOff,
                label = stringResource(R.string.game_panel_notify_dnd),
                active = probe.dnd,
                tint = TurboPalette.Red,
                enabled = dndAvailable,
                onClick = { onDnd(!probe.dnd) }
            )
            TurboToolTile(
                icon = Icons.Rounded.TouchApp,
                label = stringResource(R.string.game_panel_tool_touch),
                active = probe.touch,
                tint = TurboPalette.Blue,
                enabled = touchAvailable,
                onClick = { onTouch(!probe.touch) }
            )
            TurboToolTile(
                icon = Icons.Rounded.Tune,
                label = stringResource(R.string.game_panel_tool_bypass),
                active = false,
                tint = TurboPalette.Red,
                enabled = true,
                onClick = onBypass
            )
            TurboToolTile(
                icon = Icons.Rounded.ScreenRotation,
                label = stringResource(R.string.game_panel_tool_rotation),
                active = rotationLocked,
                tint = TurboPalette.Cyan,
                enabled = true,
                onClick = onRotation
            )
            TurboToolTile(
                icon = Icons.Rounded.CleaningServices,
                label = stringResource(R.string.game_panel_tool_optimize),
                active = false,
                tint = TurboPalette.Blue,
                enabled = true,
                onClick = onClean
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(TurboPadSm)) {
            TurboToolTile(
                icon = Icons.Rounded.Timer,
                label = reminderLabel(probe.reminderMin),
                active = probe.reminderMin > 0,
                tint = TurboPalette.Yellow,
                enabled = true,
                onClick = onReminder
            )
            TurboToolTile(
                icon = Icons.Rounded.Speed,
                label = stringResource(R.string.game_panel_tool_fps),
                active = fpsOverlay,
                tint = TurboPalette.Blue,
                enabled = true,
                onClick = onFps
            )
            TurboToolTile(
                icon = Icons.Rounded.Palette,
                label = stringResource(R.string.game_panel_veil),
                active = veilEnabled,
                tint = TurboPalette.Blue,
                enabled = true,
                onClick = onVeil
            )
            TurboToolTile(
                icon = Icons.Rounded.Videocam,
                label = stringResource(R.string.game_panel_tool_screenrec),
                active = screenRec || recording,
                tint = TurboPalette.Red,
                enabled = true,
                onClick = onRecord
            )
            TurboToolTile(
                icon = Icons.Rounded.PhotoCamera,
                label = stringResource(R.string.game_panel_tool_shot),
                active = false,
                tint = TurboPalette.Cyan,
                enabled = true,
                onClick = onShot
            )
        }
    }
}

@Composable
private fun reminderLabel(minutes: Int): String =
    if (minutes > 0) "$LtrMark${minutes}′$LtrMark" else stringResource(R.string.game_panel_tool_reminder)

/** قسم الخدمات: الشبكة والتصويب والصوت والسطوع — مقابض النظام نفسها. */
@Composable
private fun TurboServicesSection(
    probe: TurboProbe,
    hasCellular: Boolean,
    brightnessAllowed: Boolean,
    brightness: Float,
    volume: Float,
    crosshairEnabled: Boolean,
    onWifi: (Boolean) -> Unit,
    onData: (Boolean) -> Unit,
    onCrosshair: () -> Unit,
    onBrightness: (Float) -> Unit,
    onBrightnessCommit: () -> Unit,
    onVolume: (Float) -> Unit,
    onVolumeCommit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TurboPadMd, vertical = TurboPadSm),
        verticalArrangement = Arrangement.spacedBy(TurboPadSm)
    ) {
        TurboSectionHeader(stringResource(R.string.game_panel_turbo_rail_services), TurboPalette.Cyan)
        Row(horizontalArrangement = Arrangement.spacedBy(TurboPadSm)) {
            TurboToolTile(
                icon = Icons.Rounded.Wifi,
                label = stringResource(R.string.game_panel_tool_wifi),
                active = probe.wifi,
                tint = TurboPalette.Cyan,
                enabled = true,
                onClick = { onWifi(!probe.wifi) }
            )
            if (hasCellular) {
                TurboToolTile(
                    icon = Icons.Rounded.SignalCellularAlt,
                    label = stringResource(R.string.game_panel_tool_data),
                    active = probe.mobile,
                    tint = TurboPalette.Cyan,
                    enabled = true,
                    onClick = { onData(!probe.mobile) }
                )
            }
            TurboToolTile(
                icon = Icons.Rounded.TouchApp,
                label = stringResource(R.string.crosshair_title),
                active = crosshairEnabled,
                tint = TurboPalette.Blue,
                enabled = true,
                onClick = onCrosshair
            )
        }
        if (brightnessAllowed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TurboChamferSmall)
                    .background(TurboPalette.Plate)
                    .border(PanelEdge, TurboPalette.Track, TurboChamferSmall)
                    .padding(horizontal = TurboPadSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = TurboPalette.Yellow, modifier = Modifier.size(18.dp))
                Slider(
                    value = brightness.coerceIn(0f, 1f),
                    onValueChange = onBrightness,
                    onValueChangeFinished = onBrightnessCommit,
                    colors = SliderDefaults.colors(
                        thumbColor = TurboPalette.Yellow,
                        activeTrackColor = TurboPalette.Yellow.copy(alpha = 0.7f),
                        inactiveTrackColor = TurboPalette.Track
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TurboChamferSmall)
                .background(TurboPalette.Plate)
                .border(PanelEdge, TurboPalette.Track, TurboChamferSmall)
                .padding(horizontal = TurboPadSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = TurboPalette.Cyan, modifier = Modifier.size(18.dp))
            Slider(
                value = volume.coerceIn(0f, 1f),
                onValueChange = onVolume,
                onValueChangeFinished = onVolumeCommit,
                colors = SliderDefaults.colors(
                    thumbColor = TurboPalette.Cyan,
                    activeTrackColor = TurboPalette.Cyan.copy(alpha = 0.7f),
                    inactiveTrackColor = TurboPalette.Track
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** قسم التسجيل: تسجيل HUD، وتسجيل الشاشة بالإقرار، ولقطة، ومؤقّت. */
@Composable
private fun TurboRecordSection(
    recording: Boolean,
    screenRec: Boolean,
    reminderMin: Int,
    onHudRecord: () -> Unit,
    onScreenRec: () -> Unit,
    onShot: () -> Unit,
    onReminder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TurboPadMd, vertical = TurboPadSm),
        verticalArrangement = Arrangement.spacedBy(TurboPadSm)
    ) {
        TurboSectionHeader(stringResource(R.string.game_panel_turbo_rail_record), TurboPalette.Red)
        Row(horizontalArrangement = Arrangement.spacedBy(TurboPadSm)) {
            TurboToolTile(
                icon = Icons.Rounded.Videocam,
                label = stringResource(R.string.game_panel_tool_record),
                active = recording,
                tint = TurboPalette.Red,
                enabled = true,
                onClick = onHudRecord
            )
            TurboToolTile(
                icon = Icons.Rounded.PhotoCamera,
                label = stringResource(R.string.game_panel_tool_screenrec),
                active = screenRec,
                tint = TurboPalette.Red,
                enabled = true,
                onClick = onScreenRec
            )
            TurboToolTile(
                icon = Icons.Rounded.PhotoCamera,
                label = stringResource(R.string.game_panel_tool_shot),
                active = false,
                tint = TurboPalette.Cyan,
                enabled = true,
                onClick = onShot
            )
            TurboToolTile(
                icon = Icons.Rounded.Timer,
                label = reminderLabel(reminderMin),
                active = reminderMin > 0,
                tint = TurboPalette.Yellow,
                enabled = true,
                onClick = onReminder
            )
        }
    }
}

/** عنوان قسم: شرطة ملوّنة ثم العنوان — كما في المرجع. */
@Composable
private fun TurboSectionHeader(text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 3.dp)
                .background(tint)
        )
        Spacer(Modifier.width(6.dp))
        Text(text = text, color = TurboPalette.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** العدّاد الدائري: قوس القيمة فوق مسار خافت، والقيمة في المنتصف، والوصف تحته. */
@Composable
private fun TurboGauge(
    label: String,
    value: String,
    caption: String?,
    frac: Float,
    tint: Color,
    spark: List<Float>? = null
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(TurboGaugeSize)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = TurboGaugeStroke.toPx()
                val diameter = size.minDimension - strokePx * 2
                val topLeft = Offset(strokePx, strokePx)
                val arcSize = Size(diameter, diameter)
                drawArc(
                    color = TurboPalette.Track,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                drawArc(
                    color = tint,
                    startAngle = 135f,
                    sweepAngle = 270f * frac.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value,
                    color = TurboPalette.Ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                spark?.takeIf { it.size > 1 }?.let { history ->
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .height(12.dp)
                    ) {
                        val top = (history.maxOrNull() ?: TurboFpsReference).coerceAtLeast(60f)
                        val stepX = size.width / (history.size - 1)
                        val path = Path()
                        history.forEachIndexed { index, sample ->
                            val x = stepX * index
                            val y = size.height * (1f - (sample / top).coerceIn(0f, 1f))
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, TurboPalette.Red, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
                caption?.let {
                    Text(text = it, color = TurboPalette.Muted, fontSize = 8.sp, maxLines = 1)
                }
            }
        }
        Text(text = label, color = TurboPalette.Muted, fontSize = 10.sp, maxLines = 1)
    }
}

/** بلاطة وضع: بطاقة أصغر كما طلب المالك — أيقونة وعنوان، والاختيار يمرّ للخدمة. */
@Composable
private fun TurboModeTile(
    title: String,
    tint: Color,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .height(TurboModeTileHeight)
            .clip(TurboChamferSmall)
            .background(TurboPalette.Plate)
            .border(PanelEdge, if (selected) tint else TurboPalette.Track, TurboChamferSmall)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) tint else TurboPalette.Muted, modifier = Modifier.size(16.dp))
        Text(
            text = title,
            color = if (selected) TurboPalette.Ink else TurboPalette.Muted,
            fontSize = 8.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/** بلاطة أداة: أيقونة وعنوان؛ الفعّالة تُضيء بلونها، والمتعطّلة تخفت. */
@Composable
private fun TurboToolTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.38f
    Column(
        modifier = Modifier
            .weight(1f)
            .height(TurboToolTileHeight)
            .clip(TurboChamferSmall)
            .background(TurboPalette.Plate.copy(alpha = alpha))
            .border(PanelEdge, if (active) tint else TurboPalette.Track, TurboChamferSmall)
            .clickable(enabled = enabled, role = Role.Switch, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) tint else TurboPalette.Muted,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = if (active) TurboPalette.Ink else TurboPalette.Muted,
            fontSize = 8.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/** الشريط السفلي: زر التسريع، الخبر الأخير أو اسم اللعبة، البطارية، وشريط الإطارات. */
@Composable
private fun TurboBottomBar(
    gameLabel: String,
    profileLabel: String?,
    note: String?,
    batteryPct: Int?,
    charging: Boolean,
    frames: List<Float>,
    optimizing: Boolean,
    onBoost: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TurboPadMd, vertical = TurboPadSm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TurboChamfer)
                .background(TurboPalette.Plate)
                .border(PanelEdge, TurboPalette.Track, TurboChamfer)
                .padding(horizontal = TurboPadSm, vertical = TurboPadSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(TurboChamferSmall)
                    .background(if (optimizing) TurboPalette.Red.copy(alpha = 0.4f) else TurboPalette.Red)
                    .clickable(enabled = !optimizing, role = Role.Button, onClick = onBoost),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(TurboPadSm))
            Text(
                text = note ?: profileLabel ?: gameLabel,
                color = if (note != null) TurboPalette.Ink else TurboPalette.Muted,
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            batteryPct?.let {
                Text(
                    text = "$LtrMark$it%$LtrMark${if (charging) " ⚡" else ""}",
                    color = TurboPalette.Ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
        ) {
            drawRect(TurboPalette.Track)
            val top = (frames.maxOrNull() ?: TurboFpsReference).coerceAtLeast(60f)
            val frac = ((frames.lastOrNull() ?: 0f) / top).coerceIn(0f, 1f)
            if (frac > 0f) {
                drawRect(
                    brush = Brush.horizontalGradient(listOf(TurboPalette.Blue, TurboPalette.Red)),
                    size = Size(size.width * frac, size.height)
                )
            }
        }
    }
}

/** لوحة الألوان: قريبة من المرجع بأمر المالك، وبلا اعتماد على ألوان منتج آخر. */
private object TurboPalette {
    val Back = Color(0xFF05070D)
    val Plate = Color(0xFF0C1118)
    val Rail = Color(0xFF080A10)
    val Ink = Color(0xFFF1F3F5)
    val Muted = Color(0xFF93A2B4)
    val Red = Color(0xFFFF2D3A)
    val Blue = Color(0xFF2D9CFF)
    val Cyan = Color(0xFF38BDF8)
    val Yellow = Color(0xFFFACC15)
    val Track = Color(0x26FFFFFF)
}

/** زاوية مشطوفة: القطع الأربع كما في المرجع. */
private val TurboChamfer = TurboChamferShape(TurboChamferCut)
private val TurboChamferSmall = TurboChamferShape(TurboChamferCutSmall)

private val TurboRailWidth = 58.dp
private val TurboRailItemHeight = 44.dp
private val TurboGaugeSize = 92.dp
private val TurboGaugeStroke = 7.dp
private val TurboModeTileHeight = 56.dp
private val TurboToolTileHeight = 62.dp
private val TurboPadXs = 2.dp
private val TurboPadSm = 6.dp
private val TurboPadMd = 10.dp

private val TurboChamferCut = 12.dp
private val TurboChamferCutSmall = 9.dp
private const val TurboLeaveMs = 240
private const val TurboNoteMs = 4200L
private const val TurboHoldMs = 1600L
private const val TurboProbeMs = 800L
private const val TurboIdleCheckMs = 1000L
private const val TurboRecVerifyMs = 1400L
private const val TurboRotationVerifyMs = 700L
private const val TurboFpsVerifyMs = 900L
private const val TurboCollapseDragPx = 160f
private const val TurboFpsReference = 120f

/** علامة اتجاه الكتابة للأرقام: تُغلَّف القيم اللاتينية كي لا تنقلب في RTL. */
private const val LtrMark = "\u200E"

/** القيمة غير المتاحة: نفس لغة بقية اللوحة — لا صفر مُختلق. */
private val MaxValueUnavailable get() = "—"

/** شكل الزوايا المشطوفة (قطع مستقيم في كل زاوية) — يُصنع مرة ويُعاد استخدامه. */
private fun TurboChamferShape(cut: Dp) = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cutPx = with(density) { cut.toPx() }.coerceAtMost(minOf(size.width, size.height) / 2f)
        val path = Path().apply {
            moveTo(cutPx, 0f)
            lineTo(size.width - cutPx, 0f)
            lineTo(size.width, cutPx)
            lineTo(size.width, size.height - cutPx)
            lineTo(size.width - cutPx, size.height)
            lineTo(cutPx, size.height)
            lineTo(0f, size.height - cutPx)
            lineTo(0f, cutPx)
            close()
        }
        return Outline.Generic(path)
    }
}

/** أقسام الشريط الجانبي: كل قسم محتوى حقيقي — لا قسم زخرفيّ. */
private enum class TurboSection { Perf, Tools, Services, Record }
