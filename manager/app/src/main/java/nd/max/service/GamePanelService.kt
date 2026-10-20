/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.service

import nd.max.core.security.SecurityEnforcement
import nd.max.core.security.IntegrityCheckpoint
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.R
import nd.max.core.gamespace.GameLibraryAccess
import nd.max.core.gamespace.GameProfileRepository
import nd.max.core.gamespace.GameThermalProfiles
import nd.max.core.gamespace.GamePanelMode
import nd.max.core.gamespace.BypassState
import nd.max.core.gamespace.PanelClockHold
import nd.max.core.gamespace.PanelClockReader
import nd.max.core.gamespace.PanelControlState
import nd.max.core.gamespace.PanelControls
import nd.max.core.gamespace.PanelClocks
import nd.max.core.ipc.RootNodeChannel
import nd.max.core.gamespace.GamePanelState
import nd.max.core.gamespace.PanelSide
import nd.max.core.gamespace.PanelSubject
import nd.max.core.gamespace.ThermalPanelState
import nd.max.core.gamespace.ThermalPhase
import nd.max.core.gamespace.ThermalSave
import nd.max.core.gamespace.ThermalVerdict
import nd.max.core.gamespace.PANEL_REFRESH_CHOICES
import nd.max.core.gamespace.PanelSessionClock
import nd.max.core.gamespace.PanelLifetime
import nd.max.core.gamespace.gameLibrary
import nd.max.core.gamespace.nextRefreshRate
import nd.max.core.gamespace.panelPlacement
import nd.max.core.gamespace.panelServiceLifetime
import nd.max.core.gamespace.dismissGamePanel
import nd.max.core.gamespace.panelLibraryStale
import nd.max.core.gamespace.panelStep
import nd.max.core.gamespace.preferFocusedPackage
import nd.max.core.platform.FpsMonitorUtil
import nd.max.core.platform.HUD_OWNER_PANEL
import nd.max.core.platform.HudField
import nd.max.core.platform.HudLive
import nd.max.core.platform.HudRecorder
import nd.max.core.platform.HudSampler
import nd.max.ui.component.GamePanelSurface
import nd.max.ui.design.MaxDuration
import nd.max.ui.overlay.OverlayWindow
import nd.max.ui.settings.AppLanguage
import nd.max.ui.overlay.SilkDragController
import androidx.compose.ui.geometry.Offset
import nd.max.ui.util.FpsOverlayPrefs
import nd.max.ui.util.GamePanelPrefs

/**
 * لوحة مساحة الألعاب الجانبية — **قيادة فقط، والقراءة والكتابة ليست هنا**.
 *
 * ### ما تفعله وما لا تفعله
 *
 * تفعل شيئًا واحدًا: تُبقي **مقبضًا على الحافة** حين تكون اللعبة الأمامية في المكتبة ومُفعَّلة
 * لها اللوحة، وتفتحها بلمسة، وتُخفيها حين تُغادر اللعبة. والخدمة نفسها تبقى حيّة ما دامت لعبة مُفعَّلة واحدة على الأقل، فتعمل اللوحة
 * عند فتح اللعبة من أي مكان (تكملة ٢٧٢). ولا تقرأ عتادًا ولا تكتب عليه: كل
 * رقم يُعرض يأتي من `HudSampler` (القارئ الواحد) عبر `HudLive`، وكل كتابة تبقى في `AppMonitor`
 * عبر المركّب (ADR-11). ولهذا لا يوجد هنا سطر `RootFileAccess` واحد.
 *
 * ### ولماذا خدمة رابعة لا توسيع `FpsOverlayService`
 *
 * الخدمة القائمة تعرض **لوحة أرقام مصقولة** في مكان يختاره المستخدم، وهذه تعرض **حالة لعبة**
 * (أيّ لعبة، مقبض/فتح، جلسة) في موضع تلتصق فيه بالحافة. جمعهما في خدمة واحدة يعني مفتاحًا
 * واحدًا يُشغّل سطحين مختلفَي السلوك، وإطفاء أحدهما يُطفئ الآخر. والحدّ المعلن: الخدمتان
 * **لا تعملان معًا** لأنّ `HudSampler` قارئ واحد يملكه من بدأ أخيرًا (`HUD_OWNER_OVERLAY`
 * مشترك) — وهو قيد قائم مُعلن في `HudSession`، لا عطبًا جديدًا.
 *
 * ### والحدّ الثاني
 *
 * تعرّف اللعبة الأمامية يأتي من `FpsMonitorUtil.getForegroundPackage()`، وهي **قراءة بالصدفة** وليست إشارة
 * لحظية؛ ولذلك الفاصل بين القراءتين مقيس في `panelPollDelayMs`: ثانية ونصف واللعبة أمامية، وثانيتان في غير ذلك،
 * وعشر ثوانٍ والشاشة مطفأة. وقراءة غير مقروءة تُطوى اللوحة ([PanelSubject.Unknown]) بدل أن تُقرأ «ليست لعبة».
 */
class GamePanelService : LifecycleService() {

    companion object {
        /** هل الخدمة معروضة الآن؟ تقرأه الشاشة قبل أن تَعِد المستخدم بشيء. */
        var isRunning = false
            private set

        const val CHANNEL_ID = "maxmanager_game_panel"
        const val NOTIF_ID = 9023
        const val ACTION_STOP = "nd.max.service.GamePanelService.STOP"

        /** أقصى ما يُحتفظ فيه بوضع النظام المختار قبل أن يُعاد الحكم للقراءة الحقيقية. */
        private const val PROFILE_HOLD_MS = 6_000L

        /** فاصل قراءة الترددات الحيّة واللوحة مفتوحة: يتحرّك الشريط والرقم بسلاسة دون إرهاق العقد. */
        private const val CLOCK_TICK_MS = 700L

        /**
         * يُشغّل اللوحة متى كانت هناك **لعبة مُفعَّلة واحدة على الأقل**، وتبقى ما بقيت تلك اللعبة مُفعَّلة.
         *
         * **وهذا شرط الطلب حرفيًّا («يعمل دائمًا»، تكملة ٢٧٢):** تظهر اللوحة عند فتح اللعبة من داخل التطبيق
         * أو من مشغّل خارجي أو بعد إعادة تشغيل الجهاز. ولا يمكن أن تنتظر الخدمة فتح اللعبة إن لم تكن حيّة أصلًا،
         * فتُشغَّل هنا، وعند إقلاع التطبيق أو تحديثه عبر `ModuleWatchReceiver`.
         *
         * وإن لم تكن هناك لعبة مُفعَّلة فلا يُشغَّل شيء: لا خدمة أمامية بلا سبب، ولا إشعار دائم لم يُطلب.
         */
        fun ensureRunning(context: Context) {
            val enabled = GamePanelPrefs.load(context).enabledPackages
            if (enabled.isEmpty() || isRunning) return
            val intent = Intent(context, GamePanelService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        /**
         * يُطابق الخدمة مع الألعاب المُفعَّلة عند كل تغيير من الردهة: تبقى إن بقيت لعبة مُفعَّلة، وتُوقف إن لم تبقَ.
         * **ولا يُوقفها تعطيلُ لعبة واحدة ما دامت أخرى مُفعَّلة.**
         */
        fun reconcile(context: Context) {
            if (GamePanelPrefs.load(context).enabledPackages.isEmpty()) {
                if (isRunning) {
                    context.startService(Intent(context, GamePanelService::class.java).setAction(ACTION_STOP))
                }
            } else {
                ensureRunning(context)
            }
        }
    }

    private lateinit var window: OverlayWindow
    /** لغة التطبيق التي بُني بها التراكب الآن: إن تغيّرت من الإعدادات أُعيد بناؤه بالجديدة. */
    private var overlayLanguage = ""
    private var state by mutableStateOf(GamePanelState())
    private var library: Set<String> = emptySet()

    /** المجموعة المُفعَّلة التي قُرئت معها المكتبة، ومتى قُرئت — تُعاد القراءة عند تغيّرها أو بعد [nd.max.core.gamespace.PANEL_LIBRARY_TTL_MS]. */
    private var libraryFor: Set<String> = emptySet()
    private var libraryLoadedAt = 0L

    /** هل القارئ الواحد (`HudSampler`) مملوك لهذه الخدمة الآن؟ يُشغَّل مع الحاجة ويُوقَف بدونها. */
    private var samplerRunning = false

    /** شاشة الجهاز: الاستطلاع لا يعمل والشاشة مطفأة. */
    private val powerManager: android.os.PowerManager?
        get() = getSystemService(android.os.PowerManager::class.java)

    /** آخر معدّل تحديث فرضناه من اللوحة — يُعرض كما هو، و`null` يعني «بلا فرض». */
    private var refreshRateHz by mutableStateOf<Int?>(null)
    /** معدّلات الشاشة المدعومة فعلًا، تُقرأ مرّة واحدة لقائمة الحبّة. */
    private val refreshChoices: List<Int> by lazy { supportedRefreshRates() }
    private var clocks by mutableStateOf(PanelClocks())

    /** حلقة الترددات الحيّة: تعمل واللوحة مفتوحة فقط، وتبدأ قراءتها فور الفتح لا بعد دورة الاستطلاع. */
    private var clockJob: Job? = null
    private var controls by mutableStateOf(PanelControlState())

    /** بروفايل الحرارة للّعبة الأمامية: الاختيار المعروض فورًا، وأين وصل تنفيذه فعلًا. */
    private var thermal by mutableStateOf(ThermalPanelState())
    private var thermalPkg = ""
    private var thermalJob: Job? = null

    /** حتى هذه اللحظة يُعرض وضع النظام المختار كما لُمس، ولا يُرجعه الاستطلاع إلى القديم قبل أن تنتهي الخدمة. */
    @Volatile private var profileHoldUntil = 0L
    private lateinit var silk: SilkDragController
    private var handleY = -1f

    /** آخر تفضيلات للمقبض قُرئت (حجم/حساسية/قفل/اهتزاز): يقرؤها السحب عند كل لمسة بلا إعادة بناء. */
    @Volatile private var panelPrefs = GamePanelPrefs.State()

    /** مقياس رسم المقبض للتركيب (يتبع حجمه المختار). */
    private var handleScale by mutableStateOf(1f)

    /**
     * يلتقط تغيير إعدادات المقبض من الشاشة فورًا بدل انتظار الاستطلاع التالي (ثانية ونصف): حافة جديدة أو حجم أو
     * قفل يظهر أثره في لحظته. ومفتاح الارتفاع مستثنى لأن الخدمة نفسها هي من يكتبه عند استقرار السحب.
     */
    private val prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null && key.startsWith("game_panel_") && key != "game_panel_handle_y") applyPrefsNow()
    }

    /**
     * تدوير الشاشة أو تبدّل عرضها (عرض اللعبة يتغيّر عند الإقلاع): المقبض مربوط بالحافة بجاذبية النافذة فلا
     * ينزاح أفقيًّا، لكن ارتفاعه ومقاسه يُعاد حسابهما بعد أن يستقرّ النظام على المقاس الجديد.
     */
    private val displayListener = object : android.hardware.display.DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = redockSoon()
    }
    private var dockAt: Pair<Int, Int>? = null
    private var openOrigin by mutableStateOf(Offset(1f, 0.5f))

    override fun onCreate() {
        super.onCreate()
        // بوّابة الخدمة: خدمة تعمل على نسخة معدَّلة تُنفّذ طلب المعدِّل بلا واجهة أصلًا.
        if (SecurityEnforcement.refuseService(this, IntegrityCheckpoint.SERVICE)) return
        val saved = GamePanelPrefs.load(this)
        val metrics = resources.displayMetrics
        // يُسخَّن اكتشاف الرسّام في خيط خلفي قبل أن تُفتح اللوحة، فيجد أول عدّاد جهازه جاهزًا.
        PanelClockReader.prewarm()
        overlayLanguage = AppLanguage.savedTag(this)
        panelPrefs = saved
        handleScale = saved.handleSize.scale
        // الإزاحة صفر نحو الداخل من الحافة المختارة: لا حساب بمقاس شاشة (قد يكون قديمًا ساعة الإقلاع).
        buildOverlay(
            startSide = saved.side,
            startX = 0,
            startY = ((if (saved.handleY >= 0f) saved.handleY else 0.3f) * metrics.heightPixels * 0.78f).toInt()
        )
        getSharedPreferences("settings", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(prefsListener)
        (getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager)
            .registerDisplayListener(displayListener, android.os.Handler(android.os.Looper.getMainLooper()))
        ensureForeground()
        // الخدمة تعيش خارج النشاط: بلا هذا الربط تبقى قناة الجذر null أثناء اللعب وتُقرأ عقد التردد بلا جذر.
        RootNodeChannel.connect(applicationContext, RootNodeChannel.OWNER_GAME_PANEL)
        // القارئ الواحد لا يُشغَّل هنا: يبدأ ويتوقف مع الحاجة (`syncSampler`)، لأن الخدمة تبقى حيّة طويلًا.
        lifecycleScope.launch { poll() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        // كل بدء عبر `startForegroundService` يُلزم الخدمة باستدعاء `startForeground` خلال خمس ثوانٍ، حتى لو
        // كانت تعمل. والرفيق الجذري يبدأها عند كل دخول لعبة مُفعَّلة، فيُستدعى الإشعار هنا في كل مرة.
        ensureForeground()
        if (!window.isShown) {
            // **بلا سحب مقصود:** اللوحة ملتصقة بالحافة بموضع محسوب ([panelPlacement])، والسحب
            // كان سيجعلها تُسحب ثم يُعيدها الاستطلاع كل ثانية ونصف إلى موضعها — أي صراعًا يقرأه
            // المستخدم «اللوحة ترتجف». وموضعها يُحسب، فلا حاجة لسحب.
            if (!mountOverlay()) {
                // لا ادّعاء تشغيل بلا نافذة: نفس عقد `FpsOverlayService` بالحرف.
                isRunning = false
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    /**
     * يُثبّت اللوحة في نافذة التراكب الحالية، ويربط اللمس وإعادة الوضع بعد أول رسم.
     * `false` إن رفض النظام النافذة، فلا ادّعاء تشغيل بلا نافذة.
     */
    private fun mountOverlay(): Boolean {
        val mounted = window.mount(dragEnabled = false) { Panel() }
        if (!mounted) return false
        isRunning = true
        window.setTouchHandler(silk)
        window.onceLaidOut { placeWindow() }
        return true
    }

    /** يبني النافذة وسحبها بسياق اللغة الحالي: `OverlayWindow` يُثبَّت بسياقه ولا يتبدّل بعد ذلك. */
    private fun buildOverlay(startSide: PanelSide, startX: Int, startY: Int) {
        window = OverlayWindow(
            this,
            startX = startX,
            startY = startY,
            // يسار/يمين مطلقان لا START/END: الأخيران ينعكسان في RTL والرسم مثبَّت على LTR.
            windowGravity = android.view.Gravity.TOP or
                (if (startSide == PanelSide.Start) android.view.Gravity.LEFT else android.view.Gravity.RIGHT)
        )
        silk = SilkDragController(
            context = applicationContext,
            window = window,
            dockedStart = { state.side == PanelSide.Start },
            savedFraction = { handleY },
            onOpen = { openPanel() },
            onSettled = { fraction ->
                handleY = fraction
                GamePanelPrefs.saveHandleY(applicationContext, fraction)
            },
            onSideChosen = { side -> chooseSide(side) },
            sensitivity = { panelPrefs.sensitivity.distanceFactor },
            locked = { panelPrefs.locked },
            haptics = { panelPrefs.haptics }
        )
    }

    /** حافة اختارها المستخدم بسحب المقبض إليها: تُحفظ وتُطبَّق على الحالة فورًا (فلا يردّها الاستطلاع إلى القديمة). */
    private fun chooseSide(side: PanelSide) {
        if (state.side == side) return
        state = state.copy(side = side)
        GamePanelPrefs.saveSide(applicationContext, side)
    }

    /** إعدادات المقبض تغيّرت من الشاشة: تُقرأ وتُطبَّق في لحظتها، ويُعاد الوضع بعد أن تقيس النافذة حجمها الجديد. */
    private fun applyPrefsNow() {
        val fresh = GamePanelPrefs.load(applicationContext)
        panelPrefs = fresh
        handleScale = fresh.handleSize.scale
        if (state.side != fresh.side && state.mode != GamePanelMode.Open) state = state.copy(side = fresh.side)
        if (fresh.handleY < 0f) handleY = -1f
        redockSoon()
    }

    /** يُعيد وضع المقبض الآن ثم بعد أن يستقرّ النظام (قراءة المقاس تتأخّر قليلًا عن حدث التبدّل). */
    private fun redockSoon() {
        if (!this::window.isInitialized) return
        val host = window.hostView() ?: return
        for (delayMs in longArrayOf(0L, 150L, 600L)) {
            host.postDelayed({ if (window.isShown) placeWindow() }, delayMs)
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        redockSoon()
    }

    /**
     * تغيّرت لغة التطبيق بعد بناء التراكب. نافذة `OverlayWindow` تُغلق نفسها عند الإزالة ولا تُعاد تثبيتها،
     * فتُبنى نافذة جديدة في الموضع نفسه بالسياق الحالي، ويُحدَّث الإشعار بلغته.
     */
    private fun rebuildOverlay(language: String) {
        val wasShown = window.isShown
        val (x, y) = window.position()
        silk.release()
        window.unmount()
        overlayLanguage = language
        buildOverlay(startSide = state.side, startX = x, startY = y)
        ensureForeground()
        if (wasShown && !mountOverlay()) {
            isRunning = false
            stopSelf()
        }
    }

    /** الإشعار الأمامي: يُعاد استدعاؤه عند كل بدء لأن كل بدء يُلزم بـ`startForeground` (انظر `onStartCommand`). */
    private fun ensureForeground() {
        startOverlayForeground(
            OverlayNotice(
                channelId = CHANNEL_ID,
                notificationId = NOTIF_ID,
                channelNameRes = R.string.game_panel_notif_channel,
                titleRes = R.string.game_panel_notif_title,
                stopLabelRes = R.string.game_panel_notif_stop,
                stopAction = ACTION_STOP,
                serviceClass = GamePanelService::class.java
            )
        )
    }

    override fun onDestroy() {
        runCatching {
            getSharedPreferences("settings", Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(prefsListener)
            (getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager)
                .unregisterDisplayListener(displayListener)
        }
        clockJob?.cancel()
        RootNodeChannel.disconnect(RootNodeChannel.OWNER_GAME_PANEL)
        HudSampler.stop(HUD_OWNER_PANEL)
        silk.release()
        window.unmount()
        isRunning = false
        super.onDestroy()
    }

    /**
     * استطلاع اللعبة الأمامية: يُسأل النظام عن المقدّمة، ثم تُطبَّق خطوة الاستطلاع النقيّة (`panelStep`)،
     * ويُجدَّد ما يلزم من المكتبة.
     *
     * **وكل قرار يُحسب في الملف النقي، والخدمة تطبّقه فقط.** والخدمة لا تنتهي عند مغادرة اللعبة (تكملة ٢٧٢):
     * تنتهي حين لا تبقى لعبة مُفعَّلة، أو بزرّ الإيقاف في الإشعار.
     */
    private suspend fun poll() {
        while (lifecycleScope.isActive) {
            val prefs = GamePanelPrefs.load(applicationContext)
            if (panelServiceLifetime(prefs.enabledPackages) == PanelLifetime.Stop) {
                stopSelf()
                return
            }
            panelPrefs = prefs
            handleScale = prefs.handleSize.scale
            refreshLibraryIfNeeded(prefs.enabledPackages)

            // لغة التطبيق تغيّرت من الإعدادات منذ بناء التراكب: يُعاد بناؤه بها.
            AppLanguage.savedTag(applicationContext).let { language ->
                if (language != overlayLanguage) rebuildOverlay(language)
            }

            // الشاشة المطفأة لا تُستطلع: لا لعبة تُلعب والشاشة مطفأة، فتُطوى اللوحة ([PanelSubject.Unknown]).
            val interactive = powerManager?.isInteractive ?: true
            val foreground = if (!interactive) null else withContext(Dispatchers.IO) {
                val resumed = runCatching { FpsMonitorUtil.getForegroundPackage() }.getOrNull()
                // مراجعة نافذة التركيز **حين تبدو لعبةً فقط** (فلا يُدفع ثمنها في الخمول): إن كان المقيم
                // لعبة من المكتبة فالتركيز هو الحَكَم، لأن اللعبة قد تبقى «مقيمة» بعد مغادرتها فيبقى
                // المقبض ظاهرًا فوق الشاشة الرئيسية وفوق التطبيقات.
                if (resumed != null && resumed in library) {
                    preferFocusedPackage(resumed, runCatching { FpsMonitorUtil.getFocusedWindowPackage() }.getOrNull())
                } else {
                    resumed
                }
            }
            val step = panelStep(
                previous = state,
                enabledPackages = prefs.enabledPackages,
                side = prefs.side,
                interactive = interactive,
                foreground = foreground,
                library = library
            )
            // مغادرة اللعبة والوحة مفتوحة: تتلاشى اللوحة أولًا ثم تُطوى، بدل أن تبقى مجمّدة على الشاشة حتى الاستطلاع التالي.
            if (state.mode == GamePanelMode.Open && step.state.mode != GamePanelMode.Open) {
                syncClockLoop(false)
                fadeOutCockpit()
            }
            state = step.state
            // ساعة الجلسة تُلمس لكل لعبة ترى الخدمة في المقدّمة، مُفعَّلةً كانت أم لا، كما كانت قبل التقسيم.
            (state.subject as? PanelSubject.Tracked)?.let { PanelSessionClock.touch(it.packageName) }
            if (!silk.busy) handleY = prefs.handleY
            if (state.mode == GamePanelMode.Open) {
                val fresh = withContext(Dispatchers.IO) { runCatching { PanelControls.read() }.getOrDefault(controls) }
                controls = if (android.os.SystemClock.uptimeMillis() < profileHoldUntil) {
                    fresh.copy(profile = controls.profile)
                } else {
                    fresh
                }
                (state.subject as? PanelSubject.Tracked)?.let { runCatching { syncThermal(it.packageName) } }
            }
            syncSampler(step.samplerWanted)
            syncClockLoop(state.mode == GamePanelMode.Open)
            // بعد التلاشي تبقى شفافية النافذة صفرًا، فتُعاد إلى 1 قبل أن يظهر المقبض أو اللوحة من جديد.
            if (state.visible) window.hostView()?.alpha = 1f
            placeWindow()
            delay(step.nextDelayMs)
        }
    }

    /**
     * المكتبة تُقرأ عند تغيّر الألعاب المُفعَّلة، وكل [nd.max.core.gamespace.PANEL_LIBRARY_TTL_MS] حتى تُرى التثبيتات الجديدة.
     * والقراءة الفاشلة تُبقي المكتبة السابقة بدل أن تُسقطها، فلا تُنسى الألعاب بسبب خطأ عابر.
     */
    private suspend fun refreshLibraryIfNeeded(enabled: Set<String>) {
        val now = android.os.SystemClock.uptimeMillis()
        if (!panelLibraryStale(loadedFor = libraryFor, enabled = enabled, ageMs = now - libraryLoadedAt)) return
        libraryFor = enabled
        libraryLoadedAt = now
        library = withContext(Dispatchers.IO) {
            runCatching {
                gameLibrary(
                    GameLibraryAccess.apps(applicationContext),
                    GameLibraryAccess.manual(applicationContext),
                    GameLibraryAccess.excluded(applicationContext)
                ).map { it.packageName }.toSet()
            }.getOrElse { library }
        }
    }

    /**
     * القارئ الواحد (`HudSampler`) يعمل **فقط** حين تكون اللوحة مرسومة لعبة مُفعَّلة، ويتوقف في غير ذلك.
     * كان يبدأ مع الخدمة وينتهي معها، وصار يتبع الحاجة، لأن الخدمة تبقى حيّة طويلًا الآن.
     */
    private fun syncSampler(wanted: Boolean) {
        if (wanted && !samplerRunning) {
            HudSampler.start(
                context = applicationContext,
                owner = HUD_OWNER_PANEL,
                fields = { HudField.entries.toSet() }
            )
            samplerRunning = true
        } else if (!wanted && samplerRunning) {
            HudSampler.stop(HUD_OWNER_PANEL)
            samplerRunning = false
        }
    }

    /**
     * الترددات الحيّة (`PanelClockReader`) كل [CLOCK_TICK_MS] ما دامت اللوحة مفتوحة. **كانت تُقرأ في دورة
     * الاستطلاع نفسها** (ثانية ونصف إلى ثانيتين) وبعد أن تُرسم اللوحة، فتُفتح اللوحة على «—» ثانيتين
     * وتبدو العدّادات والأشرطة جامدة. وقراءة فاشلة تُبقي آخر قيمة صحيحة بدل أن تُفرغ الشاشة.
     */
    /** الجزئي يُكمل القديم ولا يمحو ما فيه (الرسّام) — فلا «—» وامضة بين قراءتين. */
    private fun mergeClocks(old: PanelClocks, p: PanelClocks) = old.copy(
        cpuMhz = p.cpuMhz ?: old.cpuMhz, cpuCeilingMhz = p.cpuCeilingMhz ?: old.cpuCeilingMhz,
        cpuFloorMhz = p.cpuFloorMhz ?: old.cpuFloorMhz,
        cpuDynamicMaxMhz = p.cpuDynamicMaxMhz ?: old.cpuDynamicMaxMhz
    )

    private fun syncClockLoop(wanted: Boolean) {
        if (!wanted) {
            clockJob?.cancel()
            clockJob = null
            return
        }
        if (clockJob?.isActive == true) return
        clockJob = lifecycleScope.launch {
            // قراءة رسّام فاشلة واحدة لا تمحو آخر رقم صحيح فتومض «—» (انظر PanelClockHold).
            var gpuMisses = 0
            while (isActive) {
                val fresh = withContext(Dispatchers.IO) {
                    runCatching { PanelClockReader.read { partial -> clocks = mergeClocks(clocks, partial) } }.getOrNull()
                }
                if (fresh != null) {
                    val held = PanelClockHold.merge(clocks, fresh, gpuMisses)
                    clocks = held.clocks
                    gpuMisses = held.misses
                }
                delay(CLOCK_TICK_MS)
            }
        }
    }

    /**
     * تُطفأ اللوحة المفتوحة بتلاشٍ قصير ([MaxDuration.quick]) قبل أن تُطوى.
     *
     * **ولماذا لا تُطوى فورًا:** الطيّ يُزيل المحتوى في اللحظة نفسها، فتبدو اللوحة مجمّدة ثم تختفي بلا سبب مرئي.
     * والتلاشي يُظهر أنها تنصرف مع اللعبة. وزمنه دون سقف الحركة في نظام التصميم، فلا يصير زخرفة.
     */
    private suspend fun fadeOutCockpit() {
        val view = window.hostView() ?: return
        val duration = MaxDuration.quick.toLong()
        view.animate().alpha(0f).setDuration(duration).start()
        delay(duration)
    }

    /**
     * إغلاق اللوحة من داخلها: تُطوى لبقية **هذه** الجلسة فقط، والخدمة تبقى تراقب. كان الإغلاق يُوقف الخدمة،
     * فلا تعود اللوحة عند اللعبة التالية إلّا إن فُتح التطبيق من جديد (تكملة ٢٧٢ غيّرت ذلك).
     */
    private fun dismissPanel() {
        state = dismissGamePanel(state)
        syncClockLoop(false)
        window.setFullScreen(false, dockAt?.first, dockAt?.second)
        window.setTouchHandler(silk)
        syncSampler(wanted = state.mode != GamePanelMode.Hidden)
        placeWindow()
    }

    /**
     * يدوّر معدّل التحديث: 60 ⟶ 90 ⟶ 120 ⟶ **بلا فرض** — عبر `RefreshRateReceiver` وحده.
     *
     * **ولا كتابة من هنا:** المالك القائم يطبّق ذرّيًّا بقراءة نهائية وبـ`resetprop` مقيّد،
     * وتكرار الكتابة في موضع ثانٍ هو ما يمنعه ADR-11.
     */
    /**
     * معدّلات التحديث التي تدعمها الشاشة فعلًا (مثل 60/90/120/144/165)، وإلا الاحتياط القديم.
     * نفس طريقة `RefreshRateReceiver` في القراءة (`toInt`) فلا يُرفض معدّل عرضناه.
     */
    private fun supportedRefreshRates(): List<Int> = runCatching {
        val dm = getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
        dm.getDisplay(android.view.Display.DEFAULT_DISPLAY).supportedModes
            .map { it.refreshRate.toInt() }.distinct().sorted()
    }.getOrDefault(emptyList()).ifEmpty { PANEL_REFRESH_CHOICES }

    private fun cycleRefreshRate() = selectRefreshRate(nextRefreshRate(refreshRateHz, supportedRefreshRates()))

    /** يختار معدّلًا بعينه من قائمة الحبّة، أو يعيد «بلا فرض» (null)، عبر `RefreshRateReceiver` وحده كالتدوير. */
    private fun selectRefreshRate(hz: Int?) {
        val intent = Intent(applicationContext, nd.max.RefreshRateReceiver::class.java)
            .setAction("nd.max.SET_FPS")
        if (hz == null) intent.putExtra("reset", true) else intent.putExtra("fps", hz)
        applicationContext.sendBroadcast(intent)
        refreshRateHz = hz
    }

    /** تسجيل الجلسة: الحائز القائم `HudRecorder` — لا سجلّ ثانٍ ولا ملفّ جديد. */
    private fun selectProfile(id: String) {
        if (controls.auto) return
        // فوري: الوسم واللون يتبعان اللمسة قبل أن تنتهي الخدمة (كانت اللوحة تنتظر الخدمة كلها
        // ثم تقرأ فتتغيّر)، ثم تصحّح القراءة الحقيقية ما عُرض إن رُفض التطبيق.
        profileHoldUntil = android.os.SystemClock.uptimeMillis() + PROFILE_HOLD_MS
        controls = controls.copy(profile = id)
        lifecycleScope.launch(Dispatchers.IO) {
            PanelControls.applyProfile(id, autoKnown = false)
            profileHoldUntil = 0L
            controls = PanelControls.read()
        }
    }

    /**
     * مزامنة بروفايل الحرارة مع اللعبة الأمامية: لعبة جديدة تُحمَّل لها ترددات البروفايلات
     * (من قدرة الجهاز نفسها) ويُقرأ اختيارها المحفوظ؛ وما دام هناك تنفيذ جارٍ لا يُستبدل المعروض.
     */
    private suspend fun syncThermal(pkg: String) {
        if (pkg != thermalPkg) {
            thermalJob?.cancel()
            thermalPkg = pkg
            thermal = ThermalPanelState()
            runCatching { GameProfileRepository.load() }
            thermal = thermal.copy(targetsMhz = GameThermalProfiles.loadTargets(applicationContext))
        }
        if (thermal.phase == ThermalPhase.Applying) return
        val saved = GameThermalProfiles.savedSelection(pkg)
        if (saved != thermal.selected) thermal = thermal.copy(selected = saved)
    }

    /**
     * تبديل بروفايل الحرارة: **الواجهة أولًا** ثم الحفظ ثم التأكيد.
     *
     *  ١. لحظة اللمس: يُعرض الاختيار وحالة «جارٍ» (لا انتظار على شيء).
     *  ٢. يُحفظ في إعداد اللعبة بالمستودع نفسه الذي تكتب به شاشة الإعدادات.
     *  ٣. `AppMonitor` يلتقط التغيير خلال أجزاء من الثانية ويعيد مقبض الـGPU وحده، وتقرأ اللوحة
     *     سجلّ نتيجته حتى يردّ، فيُعرض «طُبِّق · ٦٥٠ MHz» أو السبب الحقيقي للرفض.
     *
     * والكاتب الوحيد لعقدة الـGPU يبقى `AppMonitor` (ADR-11): اللوحة لا تكتب فيها أبدًا.
     */
    private fun selectThermal(id: String) {
        val pkg = thermalPkg
        if (pkg.isBlank()) return
        val target = GameThermalProfiles.normalize(id)
        val requestedAt = System.currentTimeMillis()
        thermal = thermal.copy(
            selected = target, phase = ThermalPhase.Applying, reason = "", liveMhz = null, requestedAt = requestedAt
        )
        thermalJob?.cancel()
        thermalJob = lifecycleScope.launch {
            val verdict = try {
                when (GameThermalProfiles.persist(pkg, target)) {
                    ThermalSave.Failed -> ThermalVerdict(ThermalPhase.Failed, "save-failed")
                    ThermalSave.Unchanged -> ThermalVerdict(ThermalPhase.Applied)
                    ThermalSave.Changed -> GameThermalProfiles.awaitApplied(pkg, requestedAt)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ThermalVerdict(ThermalPhase.Failed, "save-failed")
            }
            if (pkg == thermalPkg) {
                thermal = thermal.copy(phase = verdict.phase, reason = verdict.reason, liveMhz = verdict.liveMhz)
            }
        }
    }

    private fun toggleBypass() {
        lifecycleScope.launch(Dispatchers.IO) {
            PanelControls.setBypass(controls.bypass != BypassState.On)
            controls = PanelControls.read()
        }
    }

    private fun toggleRecording() {
        if (HudRecorder.isLive) HudRecorder.stop() else HudRecorder.start()
    }

    /**
     * يضع النافذة على الحافة **بعد أن يُقاس مقاس المحتوى** (المقبض غير اللوحة).
     *
     * والحدّ المعلن: القياس يأتي بعد أوّل إطار، فتكون أوّل وضعية عند ٠ ثم تُصحَّح — وهو سطر
     * واحد مرئيّ عند الإقلاع لا انزياح دائم.
     */
    private fun placeWindow() {
        // الحالة المخفيّة تُخفي **النافذة نفسها** لا الرسم وحده: نافذة تراكب باقية على الحافة تبقى
        // ترى اللمس وقد تُرسم من حالة قديمة، وهذا هو «المقبض الباقي بعد مغادرة اللعبة». INVISIBLE لا GONE
        // كي يبقى مقاسها فيُوضع المقبض فورًا عند العودة إلى اللعبة لا بعد الاستطلاع التالي.
        if (!state.visible) {
            window.setFullScreen(false)
            window.setTouchHandler(null)
            window.hostView()?.visibility = android.view.View.INVISIBLE
            return
        }
        window.hostView()?.visibility = android.view.View.VISIBLE
        if (state.mode == GamePanelMode.Open) {
            window.setTouchHandler(null)
            window.setFullScreen(true)
            return
        }
        // لا تبقى النافذة ملء الشاشة تحجب اللمس حين تُطوى اللوحة، ولو كان المقبض مشغولًا بحركة الآن:
        // الحجب المؤقت كان يُرى شاشةً مجمّدة لا تستجيب حتى الاستطلاع التالي.
        window.setFullScreen(false)
        window.setTouchHandler(silk)
        if (silk.busy) return
        val (screenWidth, screenHeight) = window.screenBounds()
        val content = window.contentSize() ?: return
        if (handleY >= 0f) {
            silk.dock()
            return
        }
        val placement = panelPlacement(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            panelWidth = content.first,
            panelHeight = content.second,
            insetTop = 0,
            insetBottom = 0,
            side = state.side
        )
        // الحافة بجاذبية النافذة (إزاحة صفر)، والارتفاع وحده من الحساب: لا x مطلق يتقادم مع مقاس الشاشة.
        window.dockTo(state.side == PanelSide.Start, 0, placement.y)
    }

    /** يفتح اللوحة من موضع المقبض الحالي: تتّسع النافذة وتنطلق موجة الدخول من نقطة المقبض. */
    private fun openPanel() {
        val (_, screenHeight) = window.screenBounds()
        val at = window.position()
        val height = window.contentSize()?.second ?: 0
        dockAt = at
        openOrigin = Offset(
            if (state.side == PanelSide.Start) 0f else 1f,
            ((at.second + height / 2f) / screenHeight).coerceIn(0f, 1f)
        )
        window.setTouchHandler(null)
        window.hostView()?.alpha = 1f
        state = state.copy(mode = GamePanelMode.Open, openByUser = true)
        syncClockLoop(true)
        window.setFullScreen(true)
    }


    /** يفتح شاشة إعدادات التطبيق التي يعيش فيها مقبض عدم الإزعاج (أثره عالميّ ومالكه هناك). */
    private fun openAppSettings() {
        runCatching {
            startActivity(
                Intent(applicationContext, nd.max.MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** الرسم: من الحالة وحدها، ومن القاسم المشترك للأرقام — بلا قراءة ثانية. */
    @Composable
    private fun Panel() {
        val snapshot by HudLive.snapshot.collectAsState()            // تُقرأ كل إطار لأنّها رخيصة (ملفّ تفضيلات في الذاكرة) — فمن غيّر اللون أو الحقول
            // من الشاشة يراها في اللوحة بلا إعادة تشغيل الخدمة، وهو نفس عقد التراكبين القائمين.
            val prefs = FpsOverlayPrefs.load(applicationContext)
            val label = when (val subject = state.subject) {
            is PanelSubject.Tracked -> subject.packageName
            is PanelSubject.Other -> subject.packageName
            PanelSubject.Unknown -> ""
        }
        GamePanelSurface(
            mode = state.mode,
            gameLabel = label,
            reading = snapshot.reading,
            tally = HudRecorder.tally(),
            fields = prefs.orderedFields,
            accent = parseHudColor(prefs.colorHex),
            side = state.side,
            recording = HudRecorder.isLive,
            refreshRateHz = refreshRateHz,
            clocks = clocks,
            controls = controls,
            frames = snapshot.framesHistory,
            onOpen = { openPanel() },
            onCollapse = {
                state = state.copy(mode = GamePanelMode.Handle, openByUser = false)
                syncClockLoop(false)
                window.setFullScreen(false, dockAt?.first, dockAt?.second)
                window.setTouchHandler(silk)
                // بعد قياس المقبض الجديد لا القديم، فيُوضع على الحافة فورًا لا بعد الاستطلاع التالي.
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ placeWindow() }, 80L)
            },
            onClose = { dismissPanel() },
            onCycleRefresh = { cycleRefreshRate() },
            refreshChoices = refreshChoices,
            onSelectRefresh = { hz -> selectRefreshRate(hz) },
            onToggleRecording = { toggleRecording() },
            onOpenControls = { openAppSettings() },
            onSelectProfile = { id -> selectProfile(id) },
            onToggleBypass = { toggleBypass() },
            thermal = thermal,
            onSelectThermal = { id -> selectThermal(id) },
            handleFx = silk.fx,
            handleScale = handleScale,
            origin = openOrigin
        )
    }
}

/**
 * لون التمييز المخزَّن نصًّا (`#RRGGBB`) ⟶ `Color`، والقراءة المرفوضة تُسقط إلى الأصل.
 *
 * **ولماذا لا يُترك التحليل لمكتبة:** التفضيل يكتبه المستخدم من منتقي في الشاشة، وقيمة مُفسَدة
 * (نصّ فارغ، رمز ناقص) يجب أن تُعطي اللون الافتراضي لا أن تُسقط الرسم — وهو نفس مبدأ «أيّ اسم
 * نوع لا يُعرف يُسقط إلى الافتراضيّ» في `FpsOverlayPrefs`.
 */
internal fun parseHudColor(hex: String, fallback: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF00E676)): androidx.compose.ui.graphics.Color =
    runCatching {
        val clean = hex.removePrefix("#")
        if (clean.length != 6) fallback else androidx.compose.ui.graphics.Color(clean.toLong(16) or 0xFF000000L)
    }.getOrDefault(fallback)
