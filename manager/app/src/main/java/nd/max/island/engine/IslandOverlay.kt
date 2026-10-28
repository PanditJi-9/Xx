/*
 * Ported from Islandify (https://github.com/bughunter11/Islandify) — MIT License,
 * Copyright (c) 2026 bughunter11. Full license text: THIRD_PARTY_NOTICES.md.
 * Adapted for MaxManager (packages, resources, integration). Modifications (C) 2026 Nader Magdy.
 */
package nd.max.island.engine

import nd.max.R
import nd.max.island.core.IslandInsets
import nd.max.island.core.IslandLayout
import nd.max.island.core.IslandSettings
import nd.max.island.core.Trigger
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.NotificationManager
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.BatteryManager
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Owns the island window (a ComposeView) for one host service.
 *
 * [windowType] decides where the window sits:
 *  - TYPE_ACCESSIBILITY_OVERLAY: ABOVE the status bar, so taps reach the island everywhere.
 *    Used by [IslandA11yService].
 *  - TYPE_APPLICATION_OVERLAY: Android places this BELOW the status bar window, so any tap
 *    inside the status bar strip is swallowed by SystemUI and never reaches the island.
 *    Only used as a fallback by [IslandService].
 *
 * Also listens for charging / Bluetooth / headphone events while the island is shown.
 */
class IslandOverlay(
    private val host: Context,
    private val windowType: Int,
) {
    private val wm = host.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density get() = host.resources.displayMetrics.density

    private var owner: OverlayOwner? = null
    private var scope: CoroutineScope? = null
    private var view: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    private var receiversOn = false

    /** أقصى عرض آمن مقيس من القصّة والحواف — يُغذّي Compose بدل «عرض الشاشة ناقص ٨dp». */
    private val maxWidth = MutableStateFlow(0)

    /** المساحة تحت موضع النافذة — سقف ارتفاع يمنع بطاقةً تُدفع تحت القصّة في الوضع العرضيّ. */
    private val maxHeight = MutableStateFlow(0)

    val shown: Boolean get() = view != null

    /**
     * الحوافّ تُقرأ من النظام مرّة، وتُعاد القراءة عند تغيّر الإعداد فقط.
     * والسبب مقيس: تغيّر عرض الكبسولة يُعيد التوسيط في كل إطار من الحركة، وقراءة
     * `currentWindowMetrics` في كل إطار نداء IPC إلى مدير النوافذ — تُثقل الحركة بلا فائدة.
     */
    private var cachedInsets: IslandInsets? = null

    /** دوران الشاشة يغيّر الحواف الآمنة وقد لا يغيّر مقاس النافذة — فيُعاد الحساب هنا أيضًا. */
    private val configCallback = object : android.content.ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
            cachedInsets = null
            view?.post { applyPos() }
        }

        override fun onLowMemory() = Unit
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (!IslandSettings.isOn(Trigger.Charging)) return
            val connected = when (i.action) {
                Intent.ACTION_POWER_CONNECTED -> true
                Intent.ACTION_POWER_DISCONNECTED -> false
                else -> return
            }
            val bm = c.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            IslandController.flash(IslandMode.Charging(pct, connected), 3500)
        }
    }

    /**
     * Low battery: warns once at 20% and once at 10%, only while not charging.
     * If the phone is already low when the island starts, nothing pops up (that is not a change).
     */
    private var batteryPrimed = false
    private var lowWarned = 100   // lowest step already shown (20 or 10), 100 = none

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (level < 0 || scale <= 0) return
            val pct = level * 100 / scale
            val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            if (plugged || pct > 20) {
                lowWarned = 100
                batteryPrimed = true
                return
            }
            val step = if (pct <= 10) 10 else 20
            if (!batteryPrimed) {
                batteryPrimed = true
                lowWarned = step
                return
            }
            if (step < lowWarned) {
                lowWarned = step
                if (IslandSettings.isOn(Trigger.Battery)) {
                    IslandController.flash(IslandMode.LowBattery(pct), 5000)
                }
            }
        }
    }

    /** Silent / vibrate (ringer) and Do Not Disturb. Both are readable without any permission. */
    private var lastRinger = -1
    private var lastFilter = -1

    private val soundReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (!IslandSettings.isOn(Trigger.Toggles)) return
            when (i.action) {
                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                    val mode = c.getSystemService(AudioManager::class.java)?.ringerMode ?: return
                    if (mode == lastRinger) return
                    lastRinger = mode
                    val m = when (mode) {
                        AudioManager.RINGER_MODE_SILENT -> IslandMode.Toggle(ToggleKind.Silent, true)
                        AudioManager.RINGER_MODE_VIBRATE -> IslandMode.Toggle(ToggleKind.Vibrate, true)
                        else -> IslandMode.Toggle(ToggleKind.Silent, false)
                    }
                    IslandController.flash(m, 2500)
                }
                NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                    val f = c.getSystemService(NotificationManager::class.java)?.currentInterruptionFilter ?: return
                    if (f == lastFilter || f == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) return
                    lastFilter = f
                    val on = f != NotificationManager.INTERRUPTION_FILTER_ALL
                    IslandController.flash(IslandMode.Toggle(ToggleKind.Dnd, on), 2500)
                }
            }
        }
    }

    /** Flashlight. The first report per camera is the current state, not a change, so it is skipped. */
    private val torchStates = HashMap<String, Boolean>()
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            val prev = torchStates.put(cameraId, enabled)
            if (prev == null || prev == enabled) return
            if (IslandSettings.isOn(Trigger.Toggles)) {
                IslandController.flash(IslandMode.Toggle(ToggleKind.Flashlight, enabled), 2000)
            }
        }
    }

    /** Bluetooth connect + wired headphones. Reading the name needs BLUETOOTH_CONNECT (Android 12+). */
    private val deviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (!IslandSettings.isOn(Trigger.Devices)) return
            when (i.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val dev: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33)
                        i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    else @Suppress("DEPRECATION") i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val name = runCatching { dev?.name }.getOrNull() ?: host.getString(R.string.ify_fallback_bt_device)
                    val headphones = runCatching {
                        dev?.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO
                    }.getOrDefault(false)
                    IslandController.flash(IslandMode.Device(name, headphones), 3500)
                }
                Intent.ACTION_HEADSET_PLUG -> if (i.getIntExtra("state", 0) == 1) {
                    IslandController.flash(
                        IslandMode.Device(i.getStringExtra("name") ?: host.getString(R.string.ify_fallback_wired_headset), true), 3000
                    )
                }
            }
        }
    }

    fun show() {
        if (view != null) return

        val o = OverlayOwner().also { it.create() }
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Position is computed by us in applyPos() (not by Android's CENTER_HORIZONTAL),
            // so system transitions (app open, call screen, lock) can never leave it off-center.
            gravity = Gravity.TOP or Gravity.LEFT
            x = 0
            y = (IslandSettings.offsetY.value * density).toInt()
            // Let the window extend into the camera cutout area
            // The island sits at the top and the keyboard at the bottom, so never resize / pan for the IME
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            if (Build.VERSION.SDK_INT >= 30) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val v = ComposeView(host).apply {
            setViewTreeLifecycleOwner(o)
            setViewTreeSavedStateRegistryOwner(o)
            setViewTreeViewModelStoreOwner(o)
            setContent {
                IslandTheme {
                    val mode by IslandController.mode.collectAsState()
                    val level by IslandController.level.collectAsState()
                    val scale by IslandSettings.scale.collectAsState()
                    val widthScale by IslandSettings.widthScale.collectAsState()
                    val heightScale by IslandSettings.heightScale.collectAsState()
                    val corner by IslandSettings.corner.collectAsState()
                    val speed by IslandSettings.animSpeed.collectAsState()
                    val glow by IslandSettings.glow.collectAsState()
                    val haptics by IslandSettings.haptics.collectAsState()
                    val useAppAccent by IslandSettings.islandAppAccent.collectAsState()
                    val replying by IslandController.replying.collectAsState()
                    val queued by IslandController.queued.collectAsState()
                    val secondary by IslandController.secondary.collectAsState()
                    val idlePill by IslandSettings.idlePill.collectAsState()
                    val hidden by IslandController.hidden.collectAsState()
                    val gestureV by IslandController.gestureVelocity.collectAsState()
                    val maxW by maxWidth.collectAsState()
                    val maxH by maxHeight.collectAsState()
                    val rtl = host.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
                    val dir = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    // معاينة شاشة التخصيص: تبقى ظاهرة وهي Idle كي يرى المستخدم أثر كل شريط.
                    // وتُطفأ بمجرّد مغادرة الشاشة — فلا كبسولة دائمة بلا طلب.
                    val previewing by IslandController.previewing.collectAsState()
                    // Nothing happening -> the island is not on screen at all.
                    // It grows out of the top when an activity starts and shrinks away when it ends.
                    val last = remember { LastMode() }
                    if (mode !is IslandMode.Idle) last.value = mode
                    // A fast flick collapses / expands with a shorter, bolder transition than a slow drag.
                    val motion = remember(gestureV) { IslandLayout.transitionFor(gestureV) }

                    // PRE-WARM: draw the island once, invisible and tiny, right after the window is added.
                    // The first real composition (icons, theme, springs) is what made the first call /
                    // notification lag. After 2 frames everything is already cached.
                    var warm by remember { mutableStateOf(false) }
                    if (!warm) {
                        LaunchedEffect(Unit) {
                            withFrameNanos { }
                            withFrameNanos { }
                            warm = true
                        }
                        Box(Modifier.size(1.dp).alpha(0f)) {
                            DynamicIslandUi(
                                mode = IslandMode.Call("", true, 0L),
                                level = IslandLevel.Expanded,
                                onClick = {},
                            )
                        }
                    }

                    // الظهور في السكون له طريقان: خيار المستخدم الدائم (`idlePill`)، أو معاينة
                    // التخصيص العابرة (`previewing`) التي تُمكّن الضبط بالعين لا بالتخمين.
                    val showWhenIdle = (idlePill || previewing) && !hidden
                    // اثنان قد يظهران معًا: الجزيرة، ومقبض الاستعادة. بلا Box يصطفّان في زاوية
                    // النافذة بدل التوسيط، والمقبض يهبط بعيدًا عن موضعه المقصود.
                    Box(Modifier.wrapContentSize(), contentAlignment = Alignment.TopCenter) {
                        AnimatedVisibility(
                            // Opt-in only ("Always show idle pill"): by default an idle island is not on
                            // screen at all, so turning the service on does not leave a black pill behind.
                            // When enabled — or while the customization screen is open — it stays so
                            // there is something to see and tap (time / date / battery).
                            visible = (mode !is IslandMode.Idle || showWhenIdle) && !hidden,
                            // Fast + snappy: velocity decides the duration / starting scale.
                            enter = scaleIn(
                                tween(motion.durationMs), motion.startScale, TransformOrigin(0.5f, 0f)
                            ) + fadeIn(tween((motion.durationMs * 0.65f).toInt().coerceAtLeast(1))),
                            exit = scaleOut(
                                tween(motion.durationMs), motion.startScale, TransformOrigin(0.5f, 0f)
                            ) + fadeOut(tween((motion.durationMs * 0.75f).toInt().coerceAtLeast(1))),
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides dir) {
                                DynamicIslandUi(
                                    mode = if (mode is IslandMode.Idle && showWhenIdle) mode else last.value,
                                    level = level,
                                    scale = scale,
                                    widthScale = widthScale,
                                    heightScale = heightScale,
                                    cornerFactor = corner,
                                    animSpeed = speed,
                                    glow = glow,
                                    haptics = haptics,
                                    onClick = IslandController::toggle,
                                    onLongPress = IslandController::large,
                                    onDismiss = IslandController::dismiss,
                                    onPrev = IslandController::prev,
                                    onPlayPause = IslandController::playPause,
                                    onNext = IslandController::next,
                                    onSeek = IslandController::seekTo,
                                    onAnswer = IslandController::answerCall,
                                    onDecline = IslandController::declineCall,
                                    onMarkRead = IslandController::markRead,
                                    onReply = IslandController::startReply,
                                    onSendReply = IslandController::sendReply,
                                    onCancelReply = IslandController::cancelReply,
                                    replying = replying,
                                    onOpenMedia = IslandController::openMedia,
                                    secondary = secondary,
                                    queued = queued,
                                    onSwipeSide = IslandController::skip,
                                    onSwipe = IslandController::swipe,
                                    onPromoteSecondary = IslandController::promoteSecondary,
                                    maxWidthPx = maxW,
                                    maxHeightPx = maxH,
                                    appAccent = if (useAppAccent) MaterialTheme.colorScheme.primary else null,
                                )
                            }
                        }
                        // بعد الإخفاء يبقى مقبض صغير — بلا مقبض يصير سحب-الإخفاء طريقًا بلا رجوع.
                        AnimatedVisibility(
                            visible = hidden && !previewing,
                            enter = fadeIn(tween(160)) + scaleIn(tween(180), 0.7f, TransformOrigin(0.5f, 0f)),
                            exit = fadeOut(tween(120)),
                        ) {
                            IslandRestoreHandle(onRestore = IslandController::restore)
                        }
                    }
                }
            }
        }

        // Tap outside the island while typing = cancel the reply (needs FLAG_WATCH_OUTSIDE_TOUCH, set below)
        v.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_OUTSIDE) IslandController.cancelReply()
            false
        }

        // Whenever the window size changes (pill grows / shrinks), re-center it ourselves
        v.addOnLayoutChangeListener { _, l, _, r, _, ol, _, or, _ ->
            if (r - l != or - ol) v.post { applyPos() }
        }

        val added = runCatching { wm.addView(v, lp) }
        if (added.isFailure) {
            // e.g. overlay permission revoked. Leave everything clean so show() can be retried.
            s.cancel()
            o.destroy()
            return
        }

        owner = o
        scope = s
        params = lp
        view = v
        registerReceivers()
        runCatching { host.registerComponentCallbacks(configCallback) }

        // Replying needs the keyboard: make the window focusable only while the user is typing
        s.launch {
            IslandController.replying.collect { typing ->
                val p = params ?: return@collect
                val f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                val w = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                p.flags = if (typing) (p.flags and f.inv()) or w else (p.flags or f) and w.inv()
                view?.let { runCatching { wm.updateViewLayout(it, p) } }
            }
        }

        // Sliders in the app move the island live; the safe-area switch re-places it immediately.
        s.launch {
            combine(
                IslandSettings.offsetX,
                IslandSettings.offsetY,
                IslandSettings.avoidSystemUi,
                IslandSettings.cutoutAnchor
            ) { _, _, _, _ -> Unit }
                .collect { applyPos() }
        }
    }

    private fun screenW(): Int =
        if (Build.VERSION.SDK_INT >= 30) wm.currentWindowMetrics.bounds.width()
        else host.resources.displayMetrics.widthPixels

    private fun screenH(): Int =
        if (Build.VERSION.SDK_INT >= 30) wm.currentWindowMetrics.bounds.height()
        else host.resources.displayMetrics.heightPixels

    private fun statusBarPx(): Int = runCatching {
        val id = host.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (id > 0) host.resources.getDimensionPixelSize(id) else 0
    }.getOrDefault(0)

    /**
     * القراءة الفيزيائية الواحدة: القصّة، شريط الحالة، الحواف الآمنة، والاتجاه.
     *
     * ولا يُفترض صفرًا عند الفشل: `status_bar_height` يبقى احتياطًا حين لا تُعلن قصّة،
     * والأكبر بينهما هو ما يُتجنّب (فجهاز يعلن قصّة ويخفي شريط الحالة لا تهبط كبسولته تحت الكاميرا).
     */
    private fun readInsets(): IslandInsets {
        val dm = host.resources.displayMetrics
        val width = screenW()
        val height = screenH()
        var left = 0
        var top = statusBarPx()
        var right = 0
        var bottom = 0
        var cl = -1
        var ct = -1
        var cr = -1
        var cb = -1
        if (Build.VERSION.SDK_INT >= 30) {
            runCatching {
                val wi = wm.currentWindowMetrics.windowInsets
                val bars = wi.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                )
                left = bars.left
                top = maxOf(top, bars.top)
                right = bars.right
                bottom = bars.bottom
                wi.displayCutout?.let { cut ->
                    cutoutUnion(cut)?.let { r ->
                        cl = r.left
                        ct = r.top
                        cr = r.right
                        cb = r.bottom
                    }
                    left = maxOf(left, cut.safeInsetLeft)
                    top = maxOf(top, cut.safeInsetTop)
                    right = maxOf(right, cut.safeInsetRight)
                    bottom = maxOf(bottom, cut.safeInsetBottom)
                }
            }
        } else if (Build.VERSION.SDK_INT >= 29) {
            runCatching {
                val display = host.getSystemService(DisplayManager::class.java)
                    ?.getDisplay(android.view.Display.DEFAULT_DISPLAY)
                display?.cutout?.let { cut ->
                    cutoutUnion(cut)?.let { r ->
                        cl = r.left
                        ct = r.top
                        cr = r.right
                        cb = r.bottom
                    }
                    left = maxOf(left, cut.safeInsetLeft)
                    top = maxOf(top, cut.safeInsetTop)
                    right = maxOf(right, cut.safeInsetRight)
                    bottom = maxOf(bottom, cut.safeInsetBottom)
                }
            }
        }
        return IslandInsets(
            screenWidthPx = width,
            screenHeightPx = height,
            density = dm.density,
            safeLeftPx = left,
            safeTopPx = top,
            safeRightPx = right,
            safeBottomPx = bottom,
            statusBarPx = statusBarPx(),
            cutoutLeftPx = cl,
            cutoutTopPx = ct,
            cutoutRightPx = cr,
            cutoutBottomPx = cb,
            landscape = width > height,
            rtl = host.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL,
        )
    }

    /**
     * اتحاد مستطيلات القصّة: الجهاز قد يُعلن أكثر من مستطيل (ثقبان)، والمقصود «أين المحظور»
     * لا شكل الثقب. ولا تُستعمل `boundingRectLeft` ونظائرها لأنّ أنواعها تختلف بين إصدارات
     * الـSDK (فبعض الـstubs الحديثة يعيدها `Rect` لا `int`)، فيُبنى الاتحاد من القائمة
     * المشتركة `boundingRects` — وهي موجودة في الجميع وتغطّي تعدّد الثقوب.
     */
    private fun cutoutUnion(cut: android.view.DisplayCutout): android.graphics.Rect? {
        val rects = runCatching { cut.boundingRects }.getOrNull() ?: return null
        val first = rects.firstOrNull() ?: return null
        val u = android.graphics.Rect(first)
        for (i in 1 until rects.size) u.union(rects[i])
        return u
    }

    /** Place the window inside the safe area (+ user offset) and publish the safe width to Compose. */
    private fun applyPos() {
        val v = view ?: return
        val p = params ?: return
        if (v.width <= 0) return
        val insets = cachedInsets ?: readInsets().also { cachedInsets = it }
        val at = IslandLayout.place(
            insets = insets,
            offsetXf = IslandSettings.offsetX.value,
            offsetYf = IslandSettings.offsetY.value,
            islandWidthPx = v.width,
            islandHeightPx = v.height,
            avoidSystemUi = IslandSettings.avoidSystemUi.value,
            cutoutAnchor = IslandSettings.cutoutAnchor.value,
        )
        if (maxWidth.value != at.maxWidthPx) maxWidth.value = at.maxWidthPx
        // الارتفاع المتاح من **موضع الارتساء** (لا من y بعد الضغط) إلى أسفل المنطقة الآمنة.
        // لو حُسب من y بعد ضغطه لصار السقف مساويًا للارتفاع الحاليّ، فلا تكبر البطاقة أبدًا.
        val margin = (IslandLayout.EDGE_MARGIN_DP * insets.density).toInt().coerceAtLeast(0)
        val room = (insets.screenHeightPx - at.anchorTopPx - insets.safeBottomPx - margin).coerceAtLeast(0)
        if (maxHeight.value != room) maxHeight.value = room
        if (p.x != at.xPx || p.y != at.yPx) {
            p.x = at.xPx
            p.y = at.yPx
            runCatching { wm.updateViewLayout(v, p) }
        }
    }

    fun hide() {
        val v = view ?: return
        cachedInsets = null
        unregisterReceivers()
        runCatching { host.unregisterComponentCallbacks(configCallback) }
        scope?.cancel()
        scope = null
        runCatching { wm.removeView(v) }
        view = null
        params = null
        owner?.destroy()
        owner = null
    }

    private fun registerReceivers() {
        if (receiversOn) return
        receiversOn = true
        ContextCompat.registerReceiver(
            host, powerReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            host, deviceReceiver,
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(Intent.ACTION_HEADSET_PLUG)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Low battery
        batteryPrimed = false
        lowWarned = 100
        ContextCompat.registerReceiver(
            host, batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Silent / DND: remember the current state first, so only real changes show up
        lastRinger = runCatching { host.getSystemService(AudioManager::class.java).ringerMode }.getOrDefault(-1)
        lastFilter = runCatching {
            host.getSystemService(NotificationManager::class.java).currentInterruptionFilter
        }.getOrDefault(-1)
        ContextCompat.registerReceiver(
            host, soundReceiver,
            IntentFilter().apply {
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Flashlight
        torchStates.clear()
        runCatching {
            host.getSystemService(CameraManager::class.java)
                .registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
        }
    }

    private fun unregisterReceivers() {
        if (!receiversOn) return
        receiversOn = false
        runCatching { host.unregisterReceiver(powerReceiver) }
        runCatching { host.unregisterReceiver(deviceReceiver) }
        runCatching { host.unregisterReceiver(batteryReceiver) }
        runCatching { host.unregisterReceiver(soundReceiver) }
        runCatching { host.getSystemService(CameraManager::class.java).unregisterTorchCallback(torchCallback) }
    }
}

/** Gives Compose (running inside a service window) a lifecycle / saved-state / view-model owner. */
private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val ssc = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = ssc.savedStateRegistry

    fun create() {
        ssc.performAttach()
        ssc.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}

/** Remembers the last real activity so the island can still draw it while it animates away. */
private class LastMode {
    var value: IslandMode = IslandMode.Idle
}
