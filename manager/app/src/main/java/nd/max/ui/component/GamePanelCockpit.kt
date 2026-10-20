/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.gamespace.BypassState
import nd.max.core.gamespace.CockpitModel
import nd.max.core.gamespace.CubeLayoutMath
import nd.max.core.gamespace.PortraitLayoutMath
import nd.max.core.gamespace.GameThermalProfiles
import nd.max.core.gamespace.PanelClockMath
import nd.max.core.gamespace.PanelClocks
import nd.max.core.gamespace.PanelControlState
import nd.max.core.gamespace.PanelDevice
import nd.max.core.gamespace.PanelAppChoice
import nd.max.core.gamespace.PanelFloatingLauncher
import nd.max.core.gamespace.PanelShelf
import nd.max.core.gamespace.PanelShelfModel
import nd.max.core.gamespace.PanelReminder
import nd.max.core.gamespace.PanelScreenshot
import nd.max.core.gamespace.PanelScreenRecorder
import nd.max.core.gamespace.PanelSessionClock
import nd.max.core.gamespace.PanelSide
import nd.max.core.gamespace.PanelToggles
import nd.max.core.gamespace.QuickApp
import nd.max.core.gamespace.ThermalPanelState
import nd.max.core.gamespace.ThermalPhase
import nd.max.core.hardware.GpuHardwareBackend
import nd.max.core.platform.HudField
import nd.max.core.platform.HudReading
import nd.max.core.platform.HudTally
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.util.GamePanelPrefs
import nd.max.service.FpsOverlayService

/*
 * لوحة اللعبة المفتوحة بتصميم «Energy Cube»، على هيئة المرجع:
 *
 *   • صفّ دوائر في الأعلى (رئيسية · تحرير · طيّ) وتطبيقات سريعة، يفصلهما عن الجسم خطّ.
 *   • يسارًا: بروفايلات الحرارة الستة (افتراضي · موفّر · متوازن · ألعاب · أداء · مخصّص) كبطاقات
 *     قائمة/شبكة — لمسة واحدة تبدّل فورًا، ويُرى التنفيذ الفعلي (جارٍ ← طُبِّق ‏٦٥٠ MHz).
 *   • عمودا CPU / GPU بأرقام كبيرة وأقراص متوهجة وحبّتين: وضع النظام (CPU) والإشعارات (GPU)،
 *     وكلتاهما تفتح قائمة منسدلة بدل دورة عمياء.
 *   • شريطان طويلان من ٢٠ مقطعًا يلاصقان حافة اللوحين، يمتلئان بقدر **التردد الحيّ** (CPU يسارًا،
 *     GPU يمينًا) وعليهما شارة بقيمته؛ والنافذة على اللعبة بينهما حرّة. وحين يضيق الوسط يصير الشريط
 *     أفقيًّا داخل العمود. وفي **الطولي** تُرسم بطاقة سفليّة بدل هذا التخطيط (`GamePanelPortrait`).
 *   • يمينًا: شبكة بلاطات (كلها تكتب فعلًا) بتمرير متلاشٍ، وأسفل الوسط: الحالة وسطر الرسائل.
 *
 * اللون يتبع وضع النظام: أحمر=أداء، كهرماني=متوازن، أخضر=توفير، أزرق=تلقائي.
 * وكل مقاس من [CubeLayoutMath] فلا تتراكب الأعمدة ولا تُقطع النصوص (انظر `CubeLayoutTest`).
 */
private const val LEAVE_MS = 240

private typealias Enter = Animatable<Float, AnimationVector1D>

private enum class CubeMenu { None, Refresh, Notify, Thermal, Apps }

private fun modeAccent(controls: PanelControlState, fallback: Color): Color = when {
    controls.auto -> ModeAuto
    controls.profile == "1" -> ModeRed
    controls.profile == "2" -> ModeAmber
    controls.profile == "3" -> ModeGreen
    else -> fallback
}

private fun cockpitThermalIcon(id: String): ImageVector = when (id) {
    "power" -> Icons.Rounded.BatteryFull
    "balanced" -> Icons.Rounded.Waves
    "gaming" -> Icons.Rounded.SportsEsports
    "performance" -> Icons.Rounded.Bolt
    "custom" -> Icons.Rounded.Tune
    else -> Icons.Rounded.Restore
}

/** ألوان البروفايلات كما في شاشة الإعدادات (أخضر · أزرق · برتقالي · أحمر) ليتّفق ما يراه المستخدم. */
private fun thermalColor(id: String): Color = ThermalTint.colorOf(id)

private class CubeProbe(
    val wifi: Boolean, val mobile: Boolean, val dnd: Boolean,
    val touch: Boolean, val rec: Boolean, val minutes: Int
)

private fun batteryText(pct: Int?, charging: Boolean): String =
    (pct?.let { "$it%" } ?: "--") + if (charging) " ⚡" else ""

private fun Modifier.enterFrom(enter: Enter, dx: Float, dy: Float, delay: Float): Modifier = graphicsLayer {
    val p = ((enter.value - delay) / (1f - delay)).coerceAtLeast(0f)
    alpha = p.coerceIn(0f, 1f)
    translationX = (1f - p) * dx * density
    translationY = (1f - p) * dy * density
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun GamePanelCockpit(
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
    thermal: ThermalPanelState = ThermalPanelState(),
    onSelectThermal: (String) -> Unit = {},
    refreshChoices: List<Int> = emptyList(),
    onSelectRefresh: (Int?) -> Unit = {},
    origin: Offset = Offset(1f, 0.5f)
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    // شريط الحالة يقع فوق النافذة الكاملة، فنُزيح الجسم بارتفاعه الظاهر الآن فقط (لا بمقاس ثابت):
    // لعبة بملء الشاشة تُخفيه، والإزاحة الثابتة كانت تترك فراغًا علويًا لا تحجب شيئًا.
    val statusTop = WindowInsets.statusBars.getTop(LocalDensity.current) / density
    val device = remember { PanelDevice(context) }
    val scope = rememberCoroutineScope()
    val enter = remember { Animatable(0f) }
    var leaving by remember { mutableStateOf(false) }
    var touchedAt by remember { mutableStateOf(0L) }
    var holdUntil by remember { mutableStateOf(0L) }
    var activeAt by remember { mutableStateOf(android.os.SystemClock.uptimeMillis()) }
    var brightness by remember { mutableStateOf(device.brightness()) }
    var volume by remember { mutableStateOf(device.volume()) }
    val brightnessAllowed = remember { device.canSetBrightness() }
    var rotationLocked by remember { mutableStateOf(device.rotationLocked()) }
    var status by remember { mutableStateOf(device.status()) }
    var wifi by remember { mutableStateOf(false) }
    var mobile by remember { mutableStateOf(true) }
    var dnd by remember { mutableStateOf(false) }
    var touch by remember { mutableStateOf(false) }
    var screenRec by remember { mutableStateOf(false) }
    var fpsOverlay by remember { mutableStateOf(FpsOverlayService.isRunning) }
    var reminderMin by remember { mutableStateOf(0) }
    var optimizing by remember { mutableStateOf(false) }
    var listView by remember { mutableStateOf(true) }
    var details by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(CubeMenu.None) }
    var cpuPill by remember { mutableStateOf(Rect.Zero) }
    var gpuPill by remember { mutableStateOf(Rect.Zero) }
    var thermalTile by remember { mutableStateOf(Rect.Zero) }
    var shelfPicks by remember { mutableStateOf(GamePanelPrefs.shelfPicks(context)) }
    var shelfUses by remember { mutableStateOf(GamePanelPrefs.shelfUses(context)) }
    var appsAnchor by remember { mutableStateOf(Rect.Zero) }
    // التطبيق الذي يُفتح الآن عائمًا: يُرى مؤشّر انتظار على أيقونته (الفتح بالجذر يأخذ لحظات) ولا يُفتح غيره معه.
    var launching by remember { mutableStateOf<String?>(null) }
    // الحلقات الطويلة تقرأ آخر حالة لا لقطة لحظة إطلاقها.
    val thermalNow by rememberUpdatedState(thermal)
    val leftScroll = rememberScrollState()
    val gridScroll = rememberScrollState()
    val notes = remember { mutableStateListOf<String>() }
    val hasCellular = remember { PanelToggles.hasCellular(context) }
    val touchAvailable by produceState(false) { value = withContext(Dispatchers.IO) { PanelToggles.touchAvailable() } }
    val quickApps by produceState(emptyList<QuickApp>(), shelfPicks, shelfUses) {
        value = withContext(Dispatchers.IO) { PanelShelf.build(context, shelfPicks, shelfUses) }
    }
    val launchable by produceState(emptyList<PanelAppChoice>(), menu) {
        if (menu == CubeMenu.Apps) value = withContext(Dispatchers.IO) { PanelShelf.launchable(context) }
    }

    val lWifi = stringResource(R.string.game_panel_tool_wifi)
    val lData = stringResource(R.string.game_panel_tool_data)
    val lDefault = stringResource(R.string.game_panel_hz_default)
    val lInfo = stringResource(R.string.game_panel_tool_info)
    val lRecord = stringResource(R.string.game_panel_tool_record)
    val lRecording = stringResource(R.string.game_panel_tool_recording)
    val lScreen = stringResource(R.string.game_panel_tool_screenrec)
    val lScreenOn = stringResource(R.string.game_panel_tool_screenrec_on)
    val lShot = stringResource(R.string.game_panel_tool_shot)
    val lFps = stringResource(R.string.game_panel_tool_fps)
    val lFpsOn = stringResource(R.string.game_panel_tool_fps_on)
    val lRotation = stringResource(R.string.game_panel_tool_rotation)
    val lOptimize = stringResource(R.string.game_panel_tool_optimize)
    val lTouch = stringResource(R.string.game_panel_tool_touch)
    val lBypass = stringResource(R.string.game_panel_tool_bypass)
    val lReminder = stringResource(R.string.game_panel_tool_reminder)
    val lApp = stringResource(R.string.game_panel_tool_app)
    val lClose = stringResource(R.string.game_panel_tool_close)
    val lNormal = stringResource(R.string.game_panel_notify_normal)
    val lDnd = stringResource(R.string.game_panel_notify_dnd)
    val fmtOn = stringResource(R.string.game_panel_msg_on)
    val fmtOff = stringResource(R.string.game_panel_msg_off)
    val fmtFreed = stringResource(R.string.game_panel_msg_freed)
    val fmtReminder = stringResource(R.string.game_panel_msg_reminder)
    val msgDone = stringResource(R.string.game_panel_reminder_done)
    val msgSaved = stringResource(R.string.game_panel_msg_saved)
    val msgRecFailed = stringResource(R.string.game_panel_msg_rec_failed)
    val msgShotSaved = stringResource(R.string.game_panel_msg_shot_saved)
    val msgShotFailed = stringResource(R.string.game_panel_msg_shot_failed)
    val msgFreedNone = stringResource(R.string.game_panel_msg_freed_none)
    val msgNeedsRoot = stringResource(R.string.game_panel_msg_needs_root)
    val msgFpsFailed = stringResource(R.string.game_panel_msg_fps_failed)
    val cdList = stringResource(R.string.game_panel_view_list)
    val cdGrid = stringResource(R.string.game_panel_view_grid)
    val cdCollapse = stringResource(R.string.game_panel_collapse_cd)
    val cdSide = stringResource(R.string.game_panel_drawer_cd)
    val lHint = stringResource(R.string.game_panel_dismiss_hint)
    val lShelfEdit = stringResource(R.string.game_panel_shelf_edit)
    val lShelfTitle = stringResource(R.string.game_panel_shelf_title)
    val lShelfHint = stringResource(R.string.game_panel_shelf_hint)
    val lShelfAuto = stringResource(R.string.game_panel_shelf_auto)
    val lShelfFull = stringResource(R.string.game_panel_shelf_full)
    val msgFloatingUnsupported = stringResource(R.string.game_panel_floating_unsupported)
    val msgFloatingFailed = stringResource(R.string.game_panel_floating_failed)
    val msgFloatingNoRoot = stringResource(R.string.game_panel_floating_noroot)
    val msgFloatingNotLaunchable = stringResource(R.string.game_panel_floating_not_launchable)
    val lBrightness = stringResource(R.string.game_panel_brightness)
    val lVolume = stringResource(R.string.game_panel_volume)
    val fmtCpuFreq = stringResource(R.string.game_panel_cpu_freq_cd)
    val fmtGpuFreq = stringResource(R.string.game_panel_gpu_freq_cd)
    val fmtLoad = stringResource(R.string.game_panel_load_fmt)
    val lThermalTitle = stringResource(R.string.game_panel_thermal_title)
    val lThermalTile = stringResource(R.string.game_panel_thermal_tile)
    val lRefreshTitle = stringResource(R.string.game_panel_tool_refresh)
    val lNotifyTitle = stringResource(R.string.game_panel_notify_title)
    val sNoOverride = stringResource(R.string.game_panel_thermal_default_sub)
    val sApplying = stringResource(R.string.game_panel_thermal_applying)
    val sPending = stringResource(R.string.game_panel_thermal_pending)
    val sNotApplied = stringResource(R.string.game_panel_thermal_failed_sub)
    val fmtThermalApplied = stringResource(R.string.game_panel_thermal_msg_applied)
    val msgThermalPending = stringResource(R.string.game_panel_thermal_msg_pending)
    val fmtThermalFailed = stringResource(R.string.game_panel_thermal_msg_failed)
    val rUnsupported = stringResource(R.string.game_panel_thermal_reason_unsupported)
    val rHeld = stringResource(R.string.game_panel_thermal_reason_held)
    val rSave = stringResource(R.string.game_panel_thermal_reason_save)
    val rGeneric = stringResource(R.string.game_panel_thermal_reason_generic)
    val thermalLabels = mapOf(
        "default" to stringResource(R.string.default_label),
        "power" to stringResource(R.string.profile_label_power),
        "balanced" to stringResource(R.string.Profile_Balanced),
        "gaming" to stringResource(R.string.profile_label_gaming),
        "performance" to stringResource(R.string.Profile_Performance),
        "custom" to stringResource(R.string.profile_label_custom)
    )

    val wave = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { wave.animateTo(1f, tween(1150, easing = LinearOutSlowInEasing)) }
        enter.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 170f))
    }

    LaunchedEffect(Unit) {
        var tick = 0
        while (true) {
            val now = android.os.SystemClock.uptimeMillis()
            if (now - touchedAt > 1200) {
                brightness = device.brightness()
                volume = device.volume()
            }
            rotationLocked = device.rotationLocked()
            status = device.status()
            if (tick++ % 3 == 0 && now > holdUntil) {
                val probe = withContext(Dispatchers.IO) {
                    CubeProbe(
                        wifi = PanelToggles.wifiOn(context), mobile = PanelToggles.dataOn(context),
                        dnd = PanelToggles.dndOn(context), touch = PanelToggles.touchOn(),
                        rec = PanelScreenRecorder.isRecording(), minutes = PanelReminder.remainingMin()
                    )
                }
                wifi = probe.wifi; mobile = probe.mobile; dnd = probe.dnd
                touch = probe.touch; screenRec = probe.rec; reminderMin = probe.minutes
                // الحقيقة من الخدمة نفسها لا من ذاكرة البلاطة: قد أُوقفت اللوحة من إشعارها
                // أو من شاشتها، والبلاطة يجب أن تصدق — لا أن تبقى «تعمل» وهذا وهم.
                fpsOverlay = FpsOverlayService.isRunning
            }
            delay(800)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            // قائمة مفتوحة أو تنفيذ جارٍ = المستخدم يعمل؛ لا طيّ تحت يده.
            if (menu == CubeMenu.None && thermalNow.phase != ThermalPhase.Applying &&
                CockpitModel.shouldCollapseIdle(android.os.SystemClock.uptimeMillis(), activeAt)
            ) {
                if (!leaving) {
                    leaving = true
                    enter.animateTo(0f, tween(LEAVE_MS))
                    onCollapse()
                }
                break
            }
        }
    }

    fun leave(after: () -> Unit) {
        if (leaving) return
        leaving = true
        scope.launch {
            enter.animateTo(0f, tween(LEAVE_MS))
            after()
        }
    }

    fun say(text: String) {
        notes.add(text)
        while (notes.size > 3) notes.removeAt(0)
        scope.launch { delay(4200); notes.remove(text) }
    }

    fun hold() { holdUntil = android.os.SystemClock.uptimeMillis() + 1600 }

    /**
     * تطبيق من الشريط: نافذة عائمة فوق اللعبة، لا شاشة كاملة. تنطوي اللوحة بعد الفتح فقط (وإلا بقيت
     * ملء الشاشة فوق النافذة فلا تُرى). كل إخفاق يُقال **بسببه** لا بـ«تعذّر» واحدة، ويُرى مؤشّر انتظار أثناء الفتح.
     */
    fun launchFloating(app: QuickApp) {
        if (launching != null) return
        launching = app.pkg
        scope.launch {
            val outcome = withContext(Dispatchers.IO) { PanelFloatingLauncher.launch(context, app.pkg) }
            launching = null
            when (outcome) {
                PanelFloatingLauncher.Outcome.Opened -> {
                    withContext(Dispatchers.IO) { GamePanelPrefs.recordUse(context, app.pkg) }
                    shelfUses = GamePanelPrefs.shelfUses(context)
                    leave(onCollapse)
                }
                PanelFloatingLauncher.Outcome.Unsupported -> say(msgFloatingUnsupported)
                PanelFloatingLauncher.Outcome.NoRoot -> say(msgFloatingNoRoot)
                PanelFloatingLauncher.Outcome.NotLaunchable -> say(msgFloatingNotLaunchable)
                PanelFloatingLauncher.Outcome.Failed -> say(msgFloatingFailed)
            }
        }
    }

    /** لمسة في قائمة التحرير: تثبيت حتى [PanelShelfModel.MAX_PICKS]، أو إزالة. الحدّ يُقال لا يُصمت. */
    fun toggleShelfPick(pkg: String) {
        val current = shelfPicks
        if (pkg in current) {
            shelfPicks = current - pkg
        } else if (current.size >= PanelShelfModel.MAX_PICKS) {
            say(lShelfFull)
            return
        } else {
            shelfPicks = current + pkg
        }
        GamePanelPrefs.savePicks(context, shelfPicks)
    }

    /** قلب مقبض: يعرض الحالة المطلوبة فورًا، ويكتب بالروت، ثم تصحّحه القراءة الحقيقية. */
    fun flip(label: String, now: Boolean, set: (Boolean) -> Unit, write: (Boolean) -> Unit) {
        val want = !now
        hold()
        set(want)
        say(String.format(if (want) fmtOn else fmtOff, label))
        PanelToggles.async { write(want) }
    }

    fun slide(old: Float, new: Float, write: (Float) -> Unit) {
        touchedAt = android.os.SystemClock.uptimeMillis()
        if (CockpitModel.segmentOf(new, METER_BARS) != CockpitModel.segmentOf(old, METER_BARS)) write(new)
    }

    fun reasonText(reason: String): String = when {
        reason == "save-failed" -> rSave
        // وسبب «مراقبات فقط» يُقرأ صراحةً: نصّه لا يحوي `no-gpu-provider` (هو `…not-a-gpu-provider`)،
        // فلو تُرك للافتراضي لقالت اللوحة «رفضه المحرّك» على جهاز لا مزوّد فيه أصلًا — وهو فرق
        // يراه المستخدم: عتادٌ لا يتيح التحكم مقابل طلب رُفض.
        reason == GpuHardwareBackend.BANDWIDTH_MONITOR_REASON -> rUnsupported
        reason.contains("no-gpu-provider") || reason.contains("not-writable") || reason.contains("unsupported") -> rUnsupported
        reason.contains("held") || reason.contains("suppress") || reason.contains("blocked") || reason.contains("lock") -> rHeld
        else -> rGeneric
    }

    // نتيجة تبديل البروفايل تُقال مرّة عند وصولها (لا كل إعادة تركيب).
    LaunchedEffect(thermal.phase, thermal.requestedAt) {
        val name = thermalLabels[thermal.selected] ?: thermal.selected
        when (thermal.phase) {
            ThermalPhase.Applied -> say(
                String.format(fmtThermalApplied, name) + (thermal.liveMhz?.let { " · $it MHz" } ?: "")
            )
            ThermalPhase.Pending -> say(msgThermalPending)
            ThermalPhase.Failed -> say(String.format(fmtThermalFailed, reasonText(thermal.reason)))
            else -> Unit
        }
    }

    // لون اللوحة كلها (الأشرطة والأقراص والأزرار والموجة) يتبع **بروفايل الحرارة المختار** وحده، ثم المظهر المعتاد
    // حين يكون الافتراضي. لا يُغيَّر بحمل المعالج ولا بالحرارة: كان تجاوز «التحذير» يُحمِّر اللوحة كلما بلغ
    // حمل المعالج ٩٠٪ (وهو الحال دائمًا داخل اللعبة)، ويتذبذب عند العتبة فيبدو تغيّرًا عشوائيًا في الألوان،
    // ولا يظهر لون البروفايل إلا في مقبض السحب. وينتقل بين الألوان بانسياب، ويحترم تعطيل الحركة في النظام.
    val tintTarget = ThermalTint.of(thermal) ?: modeAccent(controls, accent)
    val tintMotion = if (rememberAnimationsEnabled()) tween<Color>(MaxDuration.standard, easing = LinearOutSlowInEasing) else tween<Color>(0)
    val tint = animateColorAsState(targetValue = tintTarget, animationSpec = tintMotion, label = "panelTint").value
    val title = rememberGameTitle(gameLabel)
    val refreshLabel = refreshRateHz?.let { "$it Hz" } ?: lDefault
    var dragged = 0f

    // بطاقات/خيارات بروفايل الحرارة: الاسم، وتحته التردد المتوقّع أو حالة التنفيذ للمختار.
    val thermalOptions = GameThermalProfiles.ids.map { id ->
        val chosen = id == thermal.selected
        val sub = when {
            chosen && thermal.phase == ThermalPhase.Applying -> sApplying
            chosen && thermal.phase == ThermalPhase.Pending -> sPending
            chosen && thermal.phase == ThermalPhase.Failed -> sNotApplied
            chosen && thermal.phase == ThermalPhase.Applied && thermal.liveMhz != null -> "${thermal.liveMhz} MHz"
            id == GameThermalProfiles.DEFAULT -> sNoOverride
            else -> thermal.targetsMhz[id]?.let { "$it MHz" } ?: ""
        }
        CubeOption(
            id = id, label = thermalLabels[id] ?: id, sub = sub,
            icon = cockpitThermalIcon(id), color = thermalColor(id),
            subColor = if (chosen && thermal.phase == ThermalPhase.Failed) ModeRed else CubeDim
        )
    }
    val thermalBusy = if (thermal.phase == ThermalPhase.Applying) thermal.selected else null
    fun pickThermal(id: String) {
        if (id == thermal.selected && thermal.phase != ThermalPhase.Failed && thermal.phase != ThermalPhase.Pending) return
        onSelectThermal(id)
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().padding(top = statusTop.dp).graphicsLayer { alpha = enter.value.coerceIn(0f, 1f) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    activeAt = android.os.SystemClock.uptimeMillis()
                }
            }
            .pointerInput(Unit) { detectTapGestures { leave(onCollapse) } }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onVerticalDrag = { _, delta -> dragged += delta },
                    onDragEnd = { if (kotlin.math.abs(dragged) > 160f) leave(onCollapse) }
                )
            }
    ) {
        val w = maxWidth
        val h = maxHeight
        val portrait = CockpitModel.isPortrait(w.value, h.value)
        val compact = CockpitModel.isCompact(w.value)
        var leftOpen by remember(compact) { mutableStateOf(!compact) }
        val deck = remember(w.value, h.value, leftOpen) { DeckMath.of(w.value, h.value, leftOpen) }
        val portraitLayout = remember(w.value, h.value) { PortraitLayoutMath.of(w.value, h.value) }
        // القوائم المنسدلة تُرسم داخل شجرة مبطَّنة بشريط الحالة، وحدود المراسي بإحداثيّ الجذر: يُطرح الفرق.
        val rootShift = statusTop * density
        val menuTop = if (portrait) MaxSpace.sm else deck.bodyTop.dp
        val cpuTag = PanelClockMath.cpuTag(clocks.cpuShownMhz)
        // الرقمان الكبيران للمعالج والرسّام = سقفهما الديناميكي الحيّ (ما يعمل به الهاتف الآن)، والشريط
        // مسطرة من أدنى تردد فيزيائي إلى أعلاه، والإضاءة عليها تتبع ذلك السقف.
        val gpuMaxTag = PanelClockMath.gpuTag(clocks.gpuShownMhz)
        val onBrightness: (Float) -> Unit = { value ->
            if (brightnessAllowed) {
                slide(brightness, value) { device.setBrightness(it) }
                brightness = value
            } else if (android.os.SystemClock.uptimeMillis() - touchedAt > 1500) {
                device.openWriteSettings()
                touchedAt = android.os.SystemClock.uptimeMillis()
            }
        }
        val onVolume: (Float) -> Unit = { value -> slide(volume, value) { device.setVolume(it) }; volume = value }

        // ── بلاطات التحكّم (كلها تكتب فعلًا): قائمة واحدة للتخطيطين العرضي والطولي
        val bypassOn = controls.bypass == BypassState.On
        val tiles = buildList {
            add(CubeTileSpec(Icons.Rounded.Wifi, null, lWifi, wifi, onClick = { flip(lWifi, wifi, { wifi = it }) { PanelToggles.setWifi(it) } }))
            if (hasCellular) add(CubeTileSpec(Icons.Rounded.Language, null, lData, mobile, onClick = { flip(lData, mobile, { mobile = it }) { PanelToggles.setData(it) } }))
            add(CubeTileSpec(null, refreshRateHz?.toString() ?: "Hz", refreshRateHz?.let { "$it Hz" } ?: lDefault, refreshRateHz != null, onClick = onCycleRefresh))
            add(CubeTileSpec(Icons.Rounded.DoNotDisturbOn, null, lDnd, dnd, onClick = { flip(lDnd, dnd, { dnd = it }) { PanelToggles.setDnd(it) } }))
            // البروفايلات ظاهرة دائمًا في الطولي وفي العرضي حين يُفتح العمود؛ وإلا تُختصر في بلاطة.
            if (!portrait && !leftOpen) {
                add(CubeTileSpec(
                    cockpitThermalIcon(thermal.selected), null, thermalLabels[thermal.selected] ?: lThermalTile,
                    thermal.selected != GameThermalProfiles.DEFAULT, onBounds = { thermalTile = it },
                    onClick = { menu = if (menu == CubeMenu.Thermal) CubeMenu.None else CubeMenu.Thermal }
                ))
            }
            add(CubeTileSpec(Icons.Rounded.Info, null, lInfo, details, onClick = { details = !details; if (details) leftOpen = true }))
            add(CubeTileSpec(if (recording) Icons.Rounded.Stop else Icons.Rounded.FiberManualRecord, null, if (recording) lRecording else lRecord, recording, onClick = onToggleRecording))
            // §1.2 — اختصار لوحة FPS من داخل تراكب اللعبة نفسه: يُشغّل خدمة اللوحة المستقلة
            // ويشاركها القارئ الواحد (تعدّد المالكين)، فيعملان معًا: هذه اللوحة تُقاس وتلك تُعرض.
            add(CubeTileSpec(Icons.Rounded.Speed, null, if (fpsOverlay) lFpsOn else lFps, fpsOverlay, onClick = {
                val want = !fpsOverlay
                hold()
                fpsOverlay = want
                if (want) FpsOverlayService.start(context) else FpsOverlayService.stop(context)
                scope.launch {
                    // لا إعلان بلا تحقّق: الخدمة الفعلية تقول بعد مهلة الإقلاع إنها تعمل — أو لا.
                    // ومهما أخطأ هذا التحقّق القصير، فالاستطلاع الدوري يعيد البلاطة إلى الحقيقة.
                    delay(900)
                    val live = FpsOverlayService.isRunning
                    fpsOverlay = live
                    say(
                        when {
                            live != want -> msgFpsFailed
                            want -> String.format(fmtOn, lFps)
                            else -> String.format(fmtOff, lFps)
                        }
                    )
                }
            }))
            add(CubeTileSpec(Icons.Rounded.Videocam, null, if (screenRec) lScreenOn else lScreen, screenRec, onClick = {
                hold()
                if (screenRec) {
                    screenRec = false
                    say(msgSaved)
                    PanelToggles.async { PanelScreenRecorder.stop() }
                } else {
                    screenRec = true
                    say(String.format(fmtOn, lScreen))
                    PanelToggles.async { PanelScreenRecorder.start() }
                    scope.launch {
                        // ملفّ العلم لم يظهر بعد المهلة ⇒ `screenrecord` لم يبدأ (لا جذر أو رفض):
                        // تُصحّح البلاطة ويُعلن الفشل — بدل «بدأ التسجيل» وهو لم يبدأ (§9.2).
                        delay(1400)
                        val live = withContext(Dispatchers.IO) { PanelScreenRecorder.isRecording() }
                        if (!live) {
                            screenRec = false
                            hold()
                            say(msgRecFailed)
                        } else {
                            leave(onCollapse)
                        }
                    }
                }
            }))
            add(CubeTileSpec(Icons.Rounded.PhotoCamera, null, lShot, false, onClick = {
                hold()
                scope.launch {
                    // الالتقاط يُعلن بنتيجته المقيسة: مسار صورة كتبت فعليًا، أو فشل صريح (§9.3).
                    val path = withContext(Dispatchers.IO) {
                        runCatching { PanelScreenshot.capture() }.getOrNull()
                    }
                    say(if (path != null) msgShotSaved else msgShotFailed)
                }
            }))
            add(CubeTileSpec(if (rotationLocked) Icons.Rounded.ScreenLockRotation else Icons.Rounded.ScreenRotation, null, lRotation, rotationLocked, onClick = {
                rotationLocked = !rotationLocked
                device.setRotationLock(rotationLocked)
            }))
            // **المكنسة لا الصاروخ (§٩):** البلاطة تُنادي `cleanMemory` وتُعلن الميجابايت المحرَّرة،
            // فرسمُ إطلاقٍ فوقها يقول فعلًا لم يقع. والرمز الآن هو نفس رمز «قاتل الذاكرة» في المحور
            // (`MaxDomainHubScreen`) لأن العملية واحدة — والاسم صار «تحرير الذاكرة» لا «تحسين».
            add(CubeTileSpec(Icons.Rounded.CleaningServices, null, if (optimizing) "…" else lOptimize, optimizing, onClick = {
                if (!optimizing) {
                    optimizing = true
                    scope.launch {
                        val freed = withContext(Dispatchers.IO) {
                            runCatching { device.cleanMemory() }.getOrNull()
                        }
                        optimizing = false
                        // الفرق بين الثلاث يعرض الحقيقة كما هي: لم تُنفَّذ (لا جذر)، نُفِّذت ولم
                        // يتحرّر شيء، نُفِّذت وتحرّر رقم **مقيس** (§9.4 — لا نجاح وهمي).
                        say(
                            when {
                                freed == null -> msgNeedsRoot
                                freed > 0 -> String.format(fmtFreed, freed)
                                else -> msgFreedNone
                            }
                        )
                    }
                }
            }))
            if (touchAvailable) add(CubeTileSpec(Icons.Rounded.TouchApp, null, lTouch, touch, onClick = { flip(lTouch, touch, { touch = it }) { PanelToggles.setTouch(it) } }))
            if (controls.bypass != BypassState.Unsupported) add(CubeTileSpec(Icons.Rounded.BatteryChargingFull, null, lBypass, bypassOn, onClick = onToggleBypass))
            add(CubeTileSpec(Icons.Rounded.Timer, null, if (reminderMin > 0) "${reminderMin}m" else lReminder, reminderMin > 0, onClick = {
                hold()
                val minutes = PanelReminder.cycle(context, msgDone)
                reminderMin = minutes
                say(if (minutes > 0) String.format(fmtReminder, minutes) else String.format(fmtOff, lReminder))
            }))
            add(CubeTileSpec(Icons.Rounded.PowerSettingsNew, null, lClose, false, onClick = { leave(onClose) }))
        }

        if (!portrait) {
            Canvas(Modifier.fillMaxSize()) { cubeWave(origin, wave.value, tint) }
            CockpitDeck(
                m = deck, w = w.value, h = h.value, enter = { enter.value }, tint = tint, leftOpen = leftOpen,
                title = title, fpsText = cockpitFpsText(frames, stringResource(R.string.game_panel_fps_unit)),
                batteryText = batteryText(status.batteryPct, status.charging),
                charging = status.charging,
                batteryLow = !status.charging && (status.batteryPct ?: 100) <= 15,
                note = notes.lastOrNull(), hint = lHint,
                apps = quickApps, launching = launching, onApp = { launchFloating(it) },
                appsOpen = menu == CubeMenu.Apps, appsEditLabel = lShelfEdit,
                onAppsEdit = { menu = if (menu == CubeMenu.Apps) CubeMenu.None else CubeMenu.Apps },
                onAppsBounds = { appsAnchor = it },
                homeLabel = lApp, onHome = { onOpenControls(); leave(onCollapse) },
                sideLabel = cdSide, onToggleLeft = { leftOpen = !leftOpen },
                collapseLabel = cdCollapse, onCollapse = { leave(onCollapse) },
                cpu = DeckGaugeSpec(
                    label = "CPU", value = PanelClockMath.ghz(clocks.cpuShownMhz) ?: MAX_VALUE_UNAVAILABLE, unit = "GHz",
                    level = clocks.cpuLevel, loadPct = clocks.cpuLoadPct, pill = refreshLabel,
                    pillOpen = menu == CubeMenu.Refresh,
                    onPill = { menu = if (menu == CubeMenu.Refresh) CubeMenu.None else CubeMenu.Refresh },
                    onPillBounds = { cpuPill = it },
                    description = String.format(fmtCpuFreq, cpuTag ?: MAX_VALUE_UNAVAILABLE)
                ),
                gpu = DeckGaugeSpec(
                    label = "GPU", value = PanelClockMath.mhz(clocks.gpuShownMhz) ?: MAX_VALUE_UNAVAILABLE, unit = "MHz",
                    level = clocks.gpuLevel, loadPct = clocks.gpuLoadPct, pill = if (dnd) lDnd else lNormal,
                    pillOpen = menu == CubeMenu.Notify,
                    onPill = { menu = if (menu == CubeMenu.Notify) CubeMenu.None else CubeMenu.Notify },
                    onPillBounds = { gpuPill = it },
                    description = String.format(fmtGpuFreq, gpuMaxTag ?: MAX_VALUE_UNAVAILABLE)
                ),
                brightness = brightness, brightnessAllowed = brightnessAllowed,
                brightnessIcon = Icons.Rounded.WbSunny, brightnessLabel = lBrightness, onBrightness = onBrightness,
                volume = volume, volumeIcon = Icons.AutoMirrored.Rounded.VolumeUp, volumeLabel = lVolume, onVolume = onVolume,
                listView = listView, onListView = { listView = it }, cdList = cdList, cdGrid = cdGrid,
                thermalTitle = lThermalTitle, thermalOptions = thermalOptions,
                thermalSelected = thermal.selected, thermalBusy = thermalBusy,
                onPickThermal = { pickThermal(it) }, leftScroll = leftScroll, gridScroll = gridScroll,
                tiles = tiles,
                extra = {
                    DeckCard { FrameGraph(history = frames, accent = tint) }
                    if (details) {
                        DeckCard { PanelReadings(reading = reading, fields = fields) }
                        DeckCard { GamePanelSession(tally = tally) }
                    }
                }
            )
        }

        // ── الطولي: بطاقة سفليّة بدل تخطيط العرضي (لا أعمدة تتقاطع على ٣٦٠dp).
        if (portrait) {
            PortraitPanel(
                layout = portraitLayout, enter = enter, tint = tint, title = title,
                statusLine = "${status.time} · ${batteryText(status.batteryPct, status.charging)} · ${CockpitModel.speedText(status.netBytes)}",
                homeLabel = lApp, onHome = { onOpenControls(); leave(onCollapse) },
                collapseLabel = cdCollapse, onCollapse = { leave(onCollapse) },
                apps = quickApps, launching = launching, onApp = { launchFloating(it) },
                appsEditLabel = lShelfEdit, appsOpen = menu == CubeMenu.Apps,
                onAppsEdit = { menu = if (menu == CubeMenu.Apps) CubeMenu.None else CubeMenu.Apps },
                onAppsBounds = { appsAnchor = it },
                cpu = PortraitGauge(
                    label = "CPU", value = PanelClockMath.ghz(clocks.cpuShownMhz) ?: MAX_VALUE_UNAVAILABLE, unit = "GHz",
                    level = clocks.cpuLevel,
                    loadText = clocks.cpuLoadPct?.let { String.format(fmtLoad, it) },
                    pill = refreshLabel, onPill = { menu = if (menu == CubeMenu.Refresh) CubeMenu.None else CubeMenu.Refresh },
                    onPillBounds = { cpuPill = it },
                    description = String.format(fmtCpuFreq, cpuTag ?: MAX_VALUE_UNAVAILABLE)
                ),
                gpu = PortraitGauge(
                    label = "GPU", value = PanelClockMath.mhz(clocks.gpuShownMhz) ?: MAX_VALUE_UNAVAILABLE, unit = "MHz",
                    level = clocks.gpuLevel,
                    loadText = clocks.gpuLoadPct?.let { String.format(fmtLoad, it) },
                    pill = if (dnd) lDnd else lNormal,
                    onPill = { menu = if (menu == CubeMenu.Notify) CubeMenu.None else CubeMenu.Notify },
                    onPillBounds = { gpuPill = it },
                    description = String.format(fmtGpuFreq, gpuMaxTag ?: MAX_VALUE_UNAVAILABLE)
                ),
                brightness = brightness, brightnessAllowed = brightnessAllowed, brightnessLabel = lBrightness,
                onBrightness = onBrightness,
                volume = volume, volumeLabel = lVolume, onVolume = onVolume,
                thermalTitle = lThermalTitle, thermalOptions = thermalOptions,
                thermalSelected = thermal.selected, thermalBusy = thermalBusy, onPickThermal = { pickThermal(it) },
                tiles = tiles,
                note = notes.lastOrNull() ?: lHint, noteIsMessage = notes.lastOrNull() != null,
                extra = {
                    InfoCard { FrameGraph(history = frames, accent = tint) }
                    if (details) {
                        InfoCard { PanelReadings(reading = reading, fields = fields) }
                        InfoCard { GamePanelSession(tally = tally) }
                    }
                }
            )
        }

        // ── القوائم المنسدلة: آخر ما يُرسم، فتعلو كل شيء ولمسة خارجها تغلقها وحدها
        when (menu) {
            CubeMenu.Refresh -> CubeDropdown(
                title = lRefreshTitle,
                options = buildList {
                    add(CubeOption("default", lDefault, "", Icons.Rounded.Tune, ModeAuto))
                    refreshChoices.forEach { hz -> add(CubeOption("$hz", "$hz Hz", "", Icons.Rounded.Speed, ModeAuto)) }
                },
                selectedId = refreshRateHz?.toString() ?: "default",
                busyId = null, tint = tint, anchor = cpuPill.translate(0f, -rootShift), screenW = w, screenH = h,
                topLimit = menuTop, density = density,
                onSelect = { id ->
                    menu = CubeMenu.None
                    // «افتراضي» يرفع الفرض (null)، واختيار المعدّل القائم نفسه لا يعيد التطبيق.
                    val hz = id.toIntOrNull()
                    if (hz != refreshRateHz) onSelectRefresh(hz)
                },
                onDismiss = { menu = CubeMenu.None }
            )
            CubeMenu.Notify -> CubeDropdown(
                title = lNotifyTitle,
                options = listOf(
                    CubeOption("normal", lNormal, "", Icons.Rounded.Visibility, ModeGreen),
                    CubeOption("dnd", lDnd, "", Icons.Rounded.VisibilityOff, ModeRed)
                ),
                selectedId = if (dnd) "dnd" else "normal", busyId = null, tint = tint,
                anchor = gpuPill.translate(0f, -rootShift), screenW = w, screenH = h, topLimit = menuTop, density = density,
                onSelect = { id ->
                    menu = CubeMenu.None
                    val want = id == "dnd"
                    if (want != dnd) flip(lDnd, dnd, { dnd = it }) { PanelToggles.setDnd(it) }
                },
                onDismiss = { menu = CubeMenu.None }
            )
            CubeMenu.Thermal -> CubeDropdown(
                title = lThermalTitle, options = thermalOptions, selectedId = thermal.selected, busyId = thermalBusy,
                tint = tint, anchor = thermalTile.translate(0f, -rootShift), screenW = w, screenH = h, topLimit = menuTop, density = density,
                onSelect = { id -> menu = CubeMenu.None; pickThermal(id) },
                onDismiss = { menu = CubeMenu.None }
            )
            CubeMenu.Apps -> AppShelfPicker(
                title = lShelfTitle, hint = lShelfHint, autoLabel = lShelfAuto,
                apps = launchable, picked = shelfPicks, maxPicks = PanelShelfModel.MAX_PICKS, tint = tint,
                anchor = appsAnchor.translate(0f, -rootShift), screenW = w, screenH = h, density = density,
                onToggle = { pkg -> toggleShelfPick(pkg) },
                onAuto = {
                    shelfPicks = emptyList()
                    GamePanelPrefs.savePicks(context, emptyList())
                    menu = CubeMenu.None
                },
                onDismiss = { menu = CubeMenu.None }
            )
            CubeMenu.None -> Unit
        }
    }
}
