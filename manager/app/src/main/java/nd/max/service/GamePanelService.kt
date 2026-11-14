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
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import nd.max.AppMonitorLogger
import nd.max.core.gamespace.ForegroundSource
import nd.max.core.gamespace.GameForeground
import nd.max.core.gamespace.GameLibraryAccess
import nd.max.core.gamespace.InAppProfileApplier
import nd.max.core.gamespace.InAppRunLog
import nd.max.core.gamespace.UsageStatsForeground
import nd.max.core.gamespace.GameProfileRepository
import nd.max.core.gamespace.GameThermalProfiles
import nd.max.core.gamespace.PlaySessionStore
import nd.max.core.gamespace.PlaySessionTracker
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
import nd.max.core.gamespace.PanelScreenshot
import nd.max.core.gamespace.PanelScreenRecorder
import nd.max.core.gamespace.PanelLifetime
import nd.max.core.gamespace.CrosshairState
import nd.max.core.gamespace.DisplayVeilState
import nd.max.core.gamespace.GameMemo
import nd.max.core.input.A11yGestureInjector
import nd.max.core.input.ExtraPoint
import nd.max.core.input.GameExtrasMath
import nd.max.core.input.GyroAimController
import nd.max.core.input.HandTouchController
import nd.max.core.input.InjectionOutcome
import nd.max.core.input.InjectionRefusal
import nd.max.core.input.MacroMath
import nd.max.core.input.MacroRunner
import nd.max.core.input.MacroStep
import nd.max.core.input.PadSide
import nd.max.core.input.TouchInjector
import nd.max.core.gamespace.gameLibrary
import nd.max.core.gamespace.nextRefreshRate
import nd.max.core.gamespace.panelPlacement
import nd.max.core.gamespace.panelServiceLifetime
import nd.max.core.gamespace.dismissGamePanel
import nd.max.core.gamespace.panelForegroundProbeMs
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
import nd.max.ui.subscreens.InAppRunRow
import nd.max.ui.subscreens.inAppReceipt
import nd.max.ui.subscreens.knobLabelRes
import nd.max.ui.subscreens.reasonTextRes
import nd.max.ui.design.MaxDuration
import nd.max.ui.overlay.OverlayWindow
import nd.max.ui.settings.AppLanguage
import nd.max.ui.overlay.SilkDragController
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import nd.max.ui.util.CaptureConsentPrefs
import nd.max.ui.util.CrosshairPrefs
import nd.max.ui.util.DisplayVeilPrefs
import nd.max.ui.util.FpsOverlayPrefs
import nd.max.ui.util.GamePanelPrefs
import nd.max.ui.util.GameExtrasPrefs
import nd.max.ui.util.GameMemoPrefs
import nd.max.ui.util.GyroAimPrefs
import nd.max.ui.util.InjectionConsentPrefs
import nd.max.ui.util.ShoulderPadPrefs

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

        /** أوامر الماكرو من لوحة الألعاب: بدء التسجيل، إيقافه وحفظه، تشغيله. */
        const val ACTION_MACRO_RECORD = "nd.max.service.GamePanelService.MACRO_RECORD"
        const val ACTION_MACRO_STOP = "nd.max.service.GamePanelService.MACRO_STOP"
        const val ACTION_MACRO_PLAY = "nd.max.service.GamePanelService.MACRO_PLAY"

        /** يُرسل أمرًا للخدمة إن كانت معروضة؛ لا يبدأ خدمة من العدم لأمر ماكرو. */
        fun sendAction(context: Context, action: String) {
            if (!isRunning) return
            runCatching {
                context.startService(Intent(context, GamePanelService::class.java).setAction(action))
            }
        }

        /** أقصى ما يُحتفظ فيه بوضع النظام المختار قبل أن يُعاد الحكم للقراءة الحقيقية. */
        private const val PROFILE_HOLD_MS = 6_000L

        /** عمر مخبأ وضع توفير البطارية — عشر ثوانٍ: قرار عرض لا قياس لحظيّ. */
        private const val POWER_SAVE_CACHE_MS = 10_000L

        /** فاصل قراءة الترددات الحيّة واللوحة مفتوحة: يتحرّك الشريط والرقم بسلاسة دون إرهاق العقد. */
        private const val CLOCK_TICK_MS = 700L

        /**
         * يُشغّل اللوحة متى كانت هناك **لعبة مُفعَّلة واحدة على الأقل**، وتبقى ما بقيت تلك اللعبة مُفعَّلة.
         *
         * **وهذا شرط الطلب حرفيًّا («يعمل دائمًا»، تكملة ٢٧٢):** تظهر اللوحة عند فتح اللعبة من داخل التطبيق
         * أو من مشغّل خارجي أو بعد إعادة تشغيل الجهاز. ولا يمكن أن تنتظر الخدمة فتح اللعبة إن لم تكن حيّة أصلًا،
         * فتُشغَّل هنا، وعند إقلاع التطبيق أو تحديثه عبر `ModuleWatchReceiver`.
         *
         * وإن لم تكن هناك لعبة مُفعَّلة **ولا تصويب مخصّص** فلا يُشغَّل شيء: لا خدمة أمامية بلا سبب،
         * ولا إشعار دائم لم يُطلب.
         */
        fun ensureRunning(context: Context) {
            val panel = GamePanelPrefs.load(context).enabledPackages
            // التصويب سطحٌ مستقلّ داخل Game Turbo: يُشغّل الخدمة ولو لم تُفعَّل لوحة لعبة،
            // لأن العلامة تعمل فوق أي لعبة في المكتبة. والشرط يبقى: لا خدمة أمامية بلا سبب.
            val crosshair = CrosshairPrefs.load(context).enabled
            // وستارة العرض سطحٌ مستقلّ كذلك: تُشغّل الخدمة ولو لم تُفعَّل لوحة لعبة.
            val veil = DisplayVeilPrefs.load(context).enabled
            if ((panel.isEmpty() && !crosshair && !veil) || isRunning) return
            // تطبيق المستخدم لا يرسم تراكبًا بلا صلاحية التراكب: بدء الخدمة كان يُنشر إشعارًا
            // أماميًّا ثم يُزال بعد فشل `addView`، ويُترك المستخدم يظنّ أن اللوحة لا تعمل. البند
            // الإرشادي في Game Space هو الإشارة الصادقة، و`service.sh` يمنح العملية عند الإقلاع.
            if (!Settings.canDrawOverlays(context)) {
                AppMonitorLogger.w("EVENT=GAME_PANEL_OVERLAY_MISSING source=ensureRunning")
                return
            }
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
         * **ولا يُوقفها تعطيلُ لعبة واحدة ما دامت أخرى مُفعَّلة.** وتبقى إن كان التصويب المخصّص
         * مفعّلًا ولو لم تبقَ لعبة مُفعَّلة. وتُوقف إذا سُحبت صلاحية التراكب، فلا خدمة أمامية بلا
         * نافذة ممكنة، أو إذا لم يبقَ سببٌ للعمل.
         */
        fun reconcile(context: Context) {
            val panel = GamePanelPrefs.load(context).enabledPackages
            val crosshair = CrosshairPrefs.load(context).enabled
            val veil = DisplayVeilPrefs.load(context).enabled
            if ((panel.isEmpty() && !crosshair && !veil) || !Settings.canDrawOverlays(context)) {
                if (isRunning) {
                    context.startService(Intent(context, GamePanelService::class.java).setAction(ACTION_STOP))
                }
                return
            }
            ensureRunning(context)
        }
    }

    private lateinit var window: OverlayWindow
    /** لغة التطبيق التي بُني بها التراكب الآن: إن تغيّرت من الإعدادات أُعيد بناؤه بالجديدة. */
    private var overlayLanguage = ""
    private var state by mutableStateOf(GamePanelState())
    /** حالة التصويب المقروءة الآن — يستهلكها الرسم والتراكب من المخزن الواحد. */
    private var crosshair by mutableStateOf(CrosshairState())
    private var crosshairOverlay: CrosshairOverlay? = null
    /** ستارة العرض المقروءة الآن — لون واحد شفّاف فوق اللعبة، لا معالجة صورة. */
    private var veil by mutableStateOf(DisplayVeilState())
    private var veilOverlay: DisplayVeilOverlay? = null
    /** المذكّرة المثبّتة: سطر واحد يبقى فوق اللعبة المتتبَّعة، لا ينبّه ولا يعدّ وقتًا. */
    private var memoOverlay: GameMemoOverlay? = null
    /** زرّا الكتف: نافذتان صغيرتان تحقنان اللمسة في مركزهما على اللعبة تحتهما. */
    private var padsOverlay: ShoulderPadOverlay? = null
    /** مساعد التصويب الجيروسكوبيّ — يعمل فقط واللعبة متتبَّعة والميزة مفعّلة ومُقرّة. */
    private var gyroController: GyroAimController? = null
    private var gyroRunning = false
    /** نقاط الإضافات (تكبير/نقر مزدوج/سحب…) — نوافذ صغيرة تُحقن عند لمسها، فوق اللعبة وحدها. */
    private var extrasOverlay: GameExtrasOverlay? = null
    /** شريط التحكم العائم بنمط Game Turbo: FPS · لقطة · تسجيل · تكبير · إغلاق. */
    private var turboStrip: GameTurboStrip? = null
    /** عدسة التكبير: تقرأ صورة الشاشة عبر خدمة الوصول (الميزة الوحيدة التي تقرأ الشاشة). */
    private var scoutOverlay: ScoutZoomOverlay? = null
    private var scoutJob: Job? = null
    /** مسطرة تسجيل الماكرو: تلتقط اللمسات وتمرّرها للعبة في الوقت نفسه. */
    private var macroPad: MacroPadOverlay? = null
    private var macroRecording = false
    private val macroSteps = mutableListOf<MacroStep>()
    private var lastMacroAtMs = 0L
    /** يد ← لمس: عبور اليد فوق الحسّاس القريب ينقر مركز الشاشة. */
    private var handController: HandTouchController? = null
    private var handRunning = false
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var library: Set<String> = emptySet()

    /** كاتب سجلّ اللعب الوحيد: جلسة تُفتح مع متابعة لعبة وتُغلق بانتقالها. */
    private val playSessions = PlaySessionTracker()

    /** المجموعة المُفعَّلة التي قُرئت معها المكتبة، ومتى قُرئت — تُعاد القراءة عند تغيّرها أو بعد [nd.max.core.gamespace.PANEL_LIBRARY_TTL_MS]. */
    private var libraryFor: Set<String> = emptySet()
    private var libraryLoadedAt = 0L

    /** آخر مصدر استُعمل لكشف اللعبة الأمامية — تبديله يُصفّر حالة أحداث الاستخدام. */
    private var lastForegroundSource: ForegroundSource? = null

    /**
     * آخر جواب عن اللعبة الأمامية ووقت سؤاله — **السؤال لا يقع في كل دورة**.
     *
     * وكل سؤال في مسار الجذر يفتح صدفةً تُنفّذ `dumpsys activity activities` (أثقل تفريغ في
     * النظام). وكان السؤال مع كل دورة: ١٨٠٠ صدفة في الساعة والخدمة خاملة. والفاصل الآن من
     * [nd.max.core.gamespace.panelForegroundProbeMs] — ويبقى ١٫٥ ث واللعبة أمامية.
     */
    private var lastForegroundProbeAtMs = 0L
    private var lastForeground: String? = null

    /** مخبأ وضع توفير البطارية: `isPowerSaveMode` رحلة Binder، وقيمته تتغيّر بدقائق. */
    private var powerSaveCachedAtMs = 0L
    private var powerSaveCached = false

    private fun powerSaveNow(): Boolean {
        val now = android.os.SystemClock.uptimeMillis()
        if (powerSaveCachedAtMs != 0L && now - powerSaveCachedAtMs < POWER_SAVE_CACHE_MS) return powerSaveCached
        powerSaveCached = powerManager?.isPowerSaveMode == true
        powerSaveCachedAtMs = now
        return powerSaveCached
    }

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
        when {
            key == null -> Unit
            key.startsWith("game_crosshair_") -> {
                crosshair = CrosshairPrefs.load(applicationContext)
                syncCrosshair(crosshair)
            }
            key.startsWith("game_veil_") -> {
                veil = DisplayVeilPrefs.load(applicationContext)
                syncVeil(veil)
            }
            key.startsWith("game_panel_") && key != "game_panel_handle_y" -> applyPrefsNow()
        }
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
        crosshair = CrosshairPrefs.load(this)
        veil = DisplayVeilPrefs.load(this)
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
            // لا نُسقط جلسة مفتوحة عند الإيقاف اليدويّ: تُغلق وتُحفظ ثم تُوقف الخدمة.
            lifecycleScope.launch {
                finishPlaySession()
                stopSelf()
            }
            return START_NOT_STICKY
        }
        // كل بدء عبر `startForegroundService` يُلزم الخدمة باستدعاء `startForeground` خلال خمس ثوانٍ، حتى لو
        // كانت تعمل. والرفيق الجذري يبدأها عند كل دخول لعبة مُفعَّلة، فيُستدعى الإشعار هنا في كل مرة.
        ensureForeground()
        // الرفيق الجذري يبدأ الخدمة بلا مرور من `ensureRunning`، فالفحص هنا واجب: بلا صلاحية
        // التراكب يرفض `WindowManager.addView` النافذة، وكانت الخدمة تُغلق بلا أثر يشرح السبب.
        if (!Settings.canDrawOverlays(this)) {
            AppMonitorLogger.w("EVENT=GAME_PANEL_OVERLAY_MISSING source=onStartCommand")
            isRunning = false
            stopSelf()
            return START_NOT_STICKY
        }
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
        // أوامر الماكرو تصل بعد تركيب النافذة: التسجيل يفتح مسطرة فوق اللعبة، والتشغيل يعيد
        // الخطوات المخزَّنة لنفس الحزمة. وكلها تمرّ بالحقن المحروس نفسه.
        when (intent?.action) {
            ACTION_MACRO_RECORD -> startMacroRecording()
            ACTION_MACRO_STOP -> stopMacroRecording(save = true)
            ACTION_MACRO_PLAY -> playMacro()
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
        // نافذة التصويب MATCH_PARENT: تُعاد قراءة المقاس الجديد في الرسم التالي.
        crosshairOverlay?.refresh()
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
        // إيقاف الخدمة يُعيد إعدادات اللعبة فورًا؛ وإن تعذّر بقيت اللقطة محفوظة ويُكمل `recover` عند الإقلاع.
        val appContext = applicationContext
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            runCatching { InAppProfileApplier.release(appContext) }
        }
        clockJob?.cancel()
        RootNodeChannel.disconnect(RootNodeChannel.OWNER_GAME_PANEL)
        HudSampler.stop(HUD_OWNER_PANEL)
        silk.release()
        window.unmount()
        crosshairOverlay?.destroy()
        crosshairOverlay = null
        veilOverlay?.destroy()
        veilOverlay = null
        memoOverlay?.destroy()
        memoOverlay = null
        padsOverlay?.destroy()
        padsOverlay = null
        gyroController?.destroy()
        gyroController = null
        extrasOverlay?.destroy()
        extrasOverlay = null
        turboStrip?.destroy()
        turboStrip = null
        scoutJob?.cancel()
        scoutJob = null
        scoutOverlay?.destroy()
        scoutOverlay = null
        macroPad?.destroy()
        macroPad = null
        macroRecording = false
        handController?.destroy()
        handController = null
        handRunning = false
        recordOpenSessionOnDestroy()
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
        // ما بقي من جلسة سابقة ماتت قبل أن تُعيد إعدادات اللعبة (عدم الإزعاج، معدّل التحديث…) يُعاد أولًا.
        runCatching { withContext(Dispatchers.IO) { InAppProfileApplier.recover(applicationContext) } }
        while (lifecycleScope.isActive) {
            val prefs = GamePanelPrefs.load(applicationContext)
            val crosshairNow = CrosshairPrefs.load(applicationContext)
            val veilNow = DisplayVeilPrefs.load(applicationContext)
            // التصويب سببٌ مستقلّ لبقاء الخدمة: من أطفأ لوحات الألعاب كلها وأبقى العلامة
            // لا يُوقف خدمته فجأة. وإن لم يبقَ أيّ سبب — لا لوحة ولا تصويب — تُوقف.
            // أمّا الستارة فلا تُرسم إلّا فوق لعبة متتبَّعة، فبقاء الخدمة من أجلها بلا لعبة
            // مُفعَّلة استقصاءٌ لشيء لا يمكن أن يُرسم. والتصويب يبقى فوق هذا كلّه لأنه يظهر
            // فوق أي تطبيق لا داخل لعبة وحدها.
            val veilKeepsService = veilNow.enabled && prefs.enabledPackages.isNotEmpty()
            if (panelServiceLifetime(prefs.enabledPackages) == PanelLifetime.Stop &&
                !crosshairNow.enabled &&
                !veilKeepsService
            ) {
                finishPlaySession()
                stopSelf()
                return
            }
            crosshair = crosshairNow
            veil = veilNow
            panelPrefs = prefs
            handleScale = prefs.handleSize.scale
            refreshLibraryIfNeeded(prefs.enabledPackages)

            // لغة التطبيق تغيّرت من الإعدادات منذ بناء التراكب: يُعاد بناؤه بها.
            AppLanguage.savedTag(applicationContext).let { language ->
                if (language != overlayLanguage) rebuildOverlay(language)
            }

            // الشاشة المطفأة لا تُستطلع: لا لعبة تُلعب والشاشة مطفأة، فتُطوى اللوحة ([PanelSubject.Unknown]).
            val interactive = powerManager?.isInteractive ?: true
            val powerSave = powerSaveNow()
            // **السؤال عن المقدّمة لا يقع في كل دورة.** إيقاع الحالة (‏١٫٥/٢/١٠ ث) هو إيقاع
            // الرسم والقرار؛ وسؤال النظام أثقل منه وأبطأ حاجةً (لا يتغيّر إلا بتبديل تطبيق).
            // فبين سؤاليْن تُعاد قيمة صحيحة معروفة، والمصدر لا يُعاد اختياره كل دورة أيضًا
            // (‏`GameForeground.source` يقرأ AppOps و`cachedRootGranted`).
            val probeMs = panelForegroundProbeMs(
                interactive = interactive,
                gameInFront = state.subject is PanelSubject.Tracked,
                powerSave = powerSave,
            )
            val probeAt = android.os.SystemClock.uptimeMillis()
            val foreground = if (!interactive) {
                null
            } else if (probeMs == Long.MAX_VALUE || probeAt - lastForegroundProbeAtMs < probeMs) {
                lastForeground
            } else {
                lastForegroundProbeAtMs = probeAt
                withContext(Dispatchers.IO) {
                    // المصدر بحسب الوضع والإذن: جذر ⇒ `dumpsys` كما كان، وغيره ⇒ أحداث الاستخدام (بلا جذر).
                    val source = GameForeground.source(applicationContext)
                    if (source != lastForegroundSource) {
                        UsageStatsForeground.reset()
                        lastForegroundSource = source
                    }
                    val resumed = runCatching { GameForeground.current(applicationContext, source) }.getOrNull()
                    // مراجعة نافذة التركيز **حين تبدو لعبةً فقط** (فلا يُدفع ثمنها في الخمول): إن كان المقيم
                    // لعبة من المكتبة فالتركيز هو الحَكَم، لأن اللعبة قد تبقى «مقيمة» بعد مغادرتها فيبقى
                    // المقبض ظاهرًا فوق الشاشة الرئيسية وفوق التطبيقات. (مسار الصدفة وحده: التركيز يُقرأ بها.)
                    if (source == ForegroundSource.SHELL && resumed != null && resumed in library) {
                        preferFocusedPackage(resumed, runCatching { FpsMonitorUtil.getFocusedWindowPackage() }.getOrNull())
                    } else {
                        resumed
                    }
                }.also { lastForeground = it }
            }
            val step = panelStep(
                previous = state,
                enabledPackages = prefs.enabledPackages,
                side = prefs.side,
                interactive = interactive,
                foreground = foreground,
                library = library,
                powerSave = powerSave,
            )
            // مغادرة اللعبة والوحة مفتوحة: تتلاشى اللوحة أولًا ثم تُطوى، بدل أن تبقى مجمّدة على الشاشة حتى الاستطلاع التالي.
            if (state.mode == GamePanelMode.Open && step.state.mode != GamePanelMode.Open) {
                syncClockLoop(false)
                fadeOutCockpit()
            }
            state = step.state
            // سجلّ اللعب: انتقال اللعبة يُغلق جلسة ويفتح أخرى؛ وقراءة مجهولة لا تُغلق جلسة قائمة.
            trackPlaySession(step.state.subject, interactive)
            // العلامة تتبع اللعبة الأمامية: تظهر مع `Tracked` وتختفي مع غيرها، وتُخفى
            // واللوحة مفتوحة حتى لا تُرسم فوق أزرارها.
            syncCrosshair(crosshairNow)
            syncVeil(veilNow)
            syncMemo(GameMemoPrefs.load(applicationContext))
            // حقن اللمس لا يُبنى إلا بلعبة متتبَّعة: زرّا الكتف والجيروسكوب بلا لعبة أمامية
            // يكتبان على شاشة الإعدادات نفسها — وهذا خطر لا ميزة.
            syncInjection(state.subject is PanelSubject.Tracked)
            // إضافات اللعبة (نقاط، شريط Turbo، تكبير، يد→لمس) على الطبقة نفسها وبالشروط نفسها.
            syncExtras(state.subject)
            // ساعة الجلسة تُلمس لكل لعبة ترى الخدمة في المقدّمة، مُفعَّلةً كانت أم لا، كما كانت قبل التقسيم.
            (state.subject as? PanelSubject.Tracked)?.let { PanelSessionClock.touch(it.packageName) }
            // بروفايل اللعبة بلا رفيق الجذر: يدخل مع اللعبة المتتبَّعة ويخرج بخروجها. المطبِّق نفسه يقرّر هل هو
            // المسؤول (مسار الجذر يبقى لـ`AppMonitor`)، وعند عدم المسؤولية لا يفعل شيئًا.
            val trackedPkg = (state.subject as? PanelSubject.Tracked)?.packageName
            withContext(Dispatchers.IO) { runCatching { InAppProfileApplier.sync(applicationContext, trackedPkg) } }
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
     * سجلّ اللعب — **كاتب واحد**: هذه الخدمة. تُغلق الجلسة عند تبدّل اللعبة أو مغادرتها أو إطفاء
     * الشاشة، ولا تُغلقها عند قراءة مجهولة (فشل قراءة ليس دليل خروج).
     *
     * والحدّ المعلن: الجلسة تبدأ من أوّل استطلاع ترى فيه الخدمة اللعبة، فلا تُحسب الفترة السابقة
     * لبدء الخدمة. والكتابة على IO لا على خيط الرسم.
     */
    private suspend fun trackPlaySession(subject: PanelSubject, interactive: Boolean) {
        val now = System.currentTimeMillis()
        val finished = when {
            !interactive -> playSessions.close(now)
            subject is PanelSubject.Unknown -> null
            else -> playSessions.onForeground((subject as? PanelSubject.Tracked)?.packageName, now)
        }
        if (finished != null) {
            withContext(Dispatchers.IO) { PlaySessionStore.record(applicationContext, finished) }
        }
    }

    private suspend fun finishPlaySession() {
        val finished = playSessions.close(System.currentTimeMillis()) ?: return
        withContext(Dispatchers.IO) { PlaySessionStore.record(applicationContext, finished) }
    }

    /** عند تدمير الخدمة بلا مرور من مسار الإيقاف: محاولة أخيرة لا تُعطّل الخيط الرئيسيّ. */
    private fun recordOpenSessionOnDestroy() {
        val finished = playSessions.close(System.currentTimeMillis()) ?: return
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            PlaySessionStore.record(applicationContext, finished)
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
     * يطابق نافذة التصويب مع الحالة: مفعّلة + لعبة في المكتبة أمامية + اللوحة غير مفتوحة.
     *
     * **ولا تُعرض فوق التطبيقات الأخرى:** العلامة جزء من Game Turbo فوق لعبة، لا تراكب عامّ
     * يبقى على الشاشة الرئيسية. والإضافة والإزالة على الخيط الرئيسيّ دائمًا (شرط `WindowManager`)،
     * حتى لو وصل التغيير من مستمع تفضيلات على خيط آخر.
     */
    private fun syncCrosshair(prefs: CrosshairState = crosshair) {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            mainHandler.post { syncCrosshair(prefs) }
            return
        }
        val wanted = prefs.enabled &&
            state.subject is PanelSubject.Tracked &&
            state.mode != GamePanelMode.Open &&
            Settings.canDrawOverlays(this)
        if (!wanted) {
            crosshairOverlay?.hide()
            return
        }
        val overlay = crosshairOverlay ?: CrosshairOverlay(this).also { crosshairOverlay = it }
        overlay.show(prefs)
    }

    /** تبديل سريع من بلاطة اللوحة: يكتب المفتاح الواحد ويطبّقه فورًا. */
    private fun toggleCrosshair() {
        val next = crosshair.copy(enabled = !crosshair.enabled)
        crosshair = next
        CrosshairPrefs.save(applicationContext, next)
        syncCrosshair(next)
    }

    /**
     * ستارة العرض — لون واحد شفّاف فوق اللعبة، **ولا تُحجب اللمس**.
     *
     * ### ولماذا لا شرط `state.mode != GamePanelMode.Open` كما في التصويب
     *
     * العلامة تُخفى واللوحة مفتوحة لأنها تُرسم فوق أزرارها. أمّا الستارة فطبقة محيطة: لو أُخفيت
     * عند كل فتح لوحة لارتجف لون الشاشة في كل تعديل، وهو أسوأ من عدم وجودها. فتبقى ما دامت لعبة
     * متتبَّعة، وتنزاح معها.
     *
     * ### والحدّ المعلن
     *
     * لون لا معالجة: لا تباين ولا حدّة ولا تشبّع. ومن أراد تغيير الصورة نفسها فذاك مسار آخر لا
     * يُدّعى هنا.
     */
    private fun syncVeil(prefs: DisplayVeilState = veil) {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            mainHandler.post { syncVeil(prefs) }
            return
        }
        val wanted = prefs.enabled &&
            state.subject is PanelSubject.Tracked &&
            Settings.canDrawOverlays(this)
        if (!wanted) {
            veilOverlay?.hide()
            return
        }
        val overlay = veilOverlay ?: DisplayVeilOverlay(this).also { veilOverlay = it }
        overlay.show(prefs)
    }

    /**
     * المذكّرة المثبّتة: تُرسم فوق **لعبة متتبَّعة** وحدها، وبلا نصّ لا نافذة إطلاقًا.
     *
     * والشرط ليس تشدّدًا: مذكّرة «المرحلة ٤» بلا لعبة أمامها كلامٌ في الفراغ، وإظهارها فوق
     * الشاشة الرئيسية يجعلها إشعارًا مزعجًا لا مذكّرة. وقراءة الإذن هنا كالستارة: بلا
     * `SYSTEM_ALERT_WINDOW` لا نحاول أصلًا، فلا سجلّ فشل في كل دورة.
     */
    /**
     * إضافات اللعبة على طبقة الحقن نفسها — **لا تعمل إلا للعبة متتبَّعة، وبإقرار، وبصلاحية تراكب**.
     *
     * وهذه هي الميزة الوحيدة التي تقرأ الشاشة (`ScoutZoomOverlay` عبر لقطة خدمة الوصول)؛ تُعلن
     * في وصف الخدمة وفي الإقرار، ولا تُخزَّن ولا تُرسَل. والماكرو يمرّر لمساته عبر
     * `TouchInjector.passThrough` لأن الإصبع الذي يسجّل هو نفسه الذي يولّد، فالحارس العاديّ
     * كان سيرفض كل خطوة — وهذا الاستثناء الوحيد، ومكتوب في مكانه.
     */
    private fun syncExtras(subject: PanelSubject) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { syncExtras(subject) }
            return
        }
        val pkg = (subject as? PanelSubject.Tracked)?.packageName
        val canDraw = Settings.canDrawOverlays(this)
        val consented = InjectionConsentPrefs.acknowledged(applicationContext)
        val active = pkg != null && consented && canDraw
        val extras = if (pkg != null) GameExtrasPrefs.load(applicationContext, pkg) else GameExtrasPrefs.State()

        // نقاط الإضافات: كل نقطة نافذة صغيرة؛ لمسها يحقن إيماءتها في موضعها على اللعبة.
        if (active && pkg != null && extras.enabled && extras.points.isNotEmpty()) {
            val overlay = extrasOverlay ?: GameExtrasOverlay(
                context = this,
                initial = extras.points,
                onPress = { point -> injectExtraPoint(point) },
                onMoved = { point -> persistExtraPoint(pkg, point) },
            ).also { extrasOverlay = it }
            overlay.show(extras.points)
        } else {
            extrasOverlay?.hide()
        }

        // شريط Game Turbo: خمس أدوات حقيقية فوق اللعبة، وموضعه الرأسي يُحفظ كنسبة.
        if (active && extras.turbo) {
            val strip = turboStrip ?: GameTurboStrip(
                context = this,
                initialY = extras.turboY,
                onFps = { toggleFpsOverlay() },
                onScreenshot = { captureScreenshot() },
                onRecord = { toggleTurboRecording() },
                onScout = { GameExtrasPrefs.setScout(applicationContext, !GameExtrasPrefs.load(applicationContext, pkg.orEmpty()).scout) },
                onClose = { GameExtrasPrefs.setTurbo(applicationContext, false) },
                onMoved = { y -> GameExtrasPrefs.saveTurboY(applicationContext, y) },
            ).also { turboStrip = it }
            strip.setScout(extras.scout)
            strip.show()
        } else {
            turboStrip?.hide()
        }

        // عدسة التكبير: لقطة من خدمة الوصول كل ٧٠٠ ملّي ثانية؛ بلا خدمة أو دون Android 11 تُقال
        // الحال صريحةً (`screenshotAvailable=false`) ولا تُفتح نافذة فارغة.
        val scoutWanted = active && extras.scout && A11yGestureInjector.screenshotAvailable
        if (scoutWanted && scoutJob == null) {
            val overlay = scoutOverlay ?: ScoutZoomOverlay(this).also { scoutOverlay = it }
            overlay.show()
            scoutJob = lifecycleScope.launch(Dispatchers.Default) {
                while (isActive) {
                    val bitmap = A11yGestureInjector.screenshot()
                    withContext(Dispatchers.Main) { overlay.update(bitmap) }
                    delay(if (bitmap == null) 1_500L else 700L)
                }
            }
        } else if (!scoutWanted) {
            scoutJob?.cancel()
            scoutJob = null
            scoutOverlay?.hide()
        }

        // يد ← لمس: عبور اليد أمام الحسّاس القريب ينقر مركز الشاشة، بتهدئة ٧٠٠ ملّي ثانية.
        val handWanted = active && extras.hand
        if (handWanted) {
            val controller = handController ?: HandTouchController(this).also { handController = it }
            if (controller.available && !handRunning) {
                controller.start()
                handRunning = true
            }
        } else if (handRunning) {
            handController?.stop()
            handRunning = false
        }
    }

    /** حفظ موضع نقطة بعد سحبها — الإيماءة تبقى كما اختارها المستخدم. */
    private fun persistExtraPoint(pkg: String, point: ExtraPoint) {
        val now = GameExtrasPrefs.load(applicationContext, pkg)
        GameExtrasPrefs.savePoints(
            applicationContext,
            pkg,
            now.points.map { if (it.id == point.id) it.copy(x = point.x, y = point.y) else it },
        )
    }

    /** لمسة نقطة إضافية: تحسب إيماءتها النقيّة ثم تنفّذها، والنقر المزدوج خطوتان بفاصل. */
    private fun injectExtraPoint(point: ExtraPoint) {
        lifecycleScope.launch(Dispatchers.IO) {
            val metrics = resources.displayMetrics
            val action = GameExtrasMath.actionFor(point, metrics.widthPixels, metrics.heightPixels)
            repeat(action.repeatCount.coerceAtLeast(1)) { index ->
                if (index > 0 && action.repeatGapMs > 0L) delay(action.repeatGapMs)
                val outcome = runCatching {
                    TouchInjector.swipe(
                        applicationContext,
                        action.startX, action.startY,
                        action.endX, action.endY,
                        action.durationMs,
                    )
                }.getOrElse {
                    InjectionOutcome.Failed(nd.max.core.input.InjectionLane.ACCESSIBILITY, "exception")
                }
                if (outcome !is InjectionOutcome.Delivered) {
                    reportInjection("EVENT=GAME_EXTRA_OUTCOME id=${point.id}", outcome)
                    return@launch
                }
            }
        }
    }

    /** تبديل تراكب FPS من الشريط — الخدمة القائمة هي المالك، فلا نسخة ثانية من الحالة. */
    private fun toggleFpsOverlay() {
        if (FpsOverlayService.isRunning) {
            FpsOverlayService.stop(applicationContext)
        } else {
            FpsOverlayService.start(applicationContext)
        }
    }

    /** لقطة من الشريط: تُحفظ في `Pictures/MaxManager` وتُعلن النتيجة، أو الفشل بصدق. */
    private fun captureScreenshot() {
        lifecycleScope.launch(Dispatchers.IO) {
            val path = runCatching { PanelScreenshot.capture() }.getOrNull()
            withContext(Dispatchers.Main) {
                val message = if (path != null) {
                    getString(R.string.game_turbo_screenshot_saved)
                } else {
                    getString(R.string.game_turbo_screenshot_failed)
                }
                runCatching { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    /** تسجيل الشاشة من الشريط — بإقرار التسجيل نفسه، لا بإقرار الحقن وحده. */
    private fun toggleTurboRecording() {
        if (!CaptureConsentPrefs.acknowledged(applicationContext)) {
            Toast.makeText(applicationContext, R.string.game_turbo_record_needs_consent, Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val wasRecording = runCatching { PanelScreenRecorder.isRecording() }.getOrDefault(false)
            val text = if (wasRecording) {
                runCatching { PanelScreenRecorder.stop() }
                R.string.game_turbo_record_stopped
            } else {
                runCatching { PanelScreenRecorder.start() }
                R.string.game_turbo_record_started
            }
            withContext(Dispatchers.Main) {
                turboStrip?.setRecording(!wasRecording)
                runCatching { Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    /** بدء تسجيل الماكرو: تُفتح مسطرة شفافة تلتقط كل سحبة وتمرّرها للعبة في الحين نفسه. */
    private fun startMacroRecording() {
        if (macroRecording) return
        val pkg = (state.subject as? PanelSubject.Tracked)?.packageName ?: return
        if (!InjectionConsentPrefs.acknowledged(applicationContext) || !Settings.canDrawOverlays(this)) return
        macroSteps.clear()
        lastMacroAtMs = 0L
        macroRecording = true
        val pad = macroPad ?: MacroPadOverlay(
            context = this,
            onStroke = { x1, y1, x2, y2, duration -> recordMacroStroke(x1, y1, x2, y2, duration) },
            onStop = { stopMacroRecording(save = true) },
        ).also { macroPad = it }
        pad.show()
        Toast.makeText(applicationContext, R.string.game_extra_macro_started, Toast.LENGTH_SHORT).show()
        AppMonitorLogger.persist("EVENT=MACRO_RECORD_START pkg=$pkg")
    }

    /** خطوة مسجَّلة: تُخزَّن بفاصلها الحقيقي وتُمرَّر إلى اللعبة بالحقن المباشر (بلا حارس). */
    private fun recordMacroStroke(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Int) {
        if (!macroRecording) return
        if (macroSteps.size >= MacroMath.MAX_STEPS) return
        val now = android.os.SystemClock.elapsedRealtime()
        val gap = if (lastMacroAtMs == 0L) MacroMath.DEFAULT_DELAY_MS else now - lastMacroAtMs
        lastMacroAtMs = now
        macroSteps += MacroMath.recorded(x1, y1, x2, y2, gap)
        lifecycleScope.launch(Dispatchers.IO) {
            val metrics = resources.displayMetrics
            runCatching {
                TouchInjector.passThrough(
                    applicationContext,
                    x1 * metrics.widthPixels, y1 * metrics.heightPixels,
                    x2 * metrics.widthPixels, y2 * metrics.heightPixels,
                    durationMs,
                )
            }
        }
    }

    /** إيقاف التسجيل: يُحفظ للعبة المتتبَّعة وحدها، والعدد يُقال لا يُقدَّر. */
    private fun stopMacroRecording(save: Boolean) {
        if (!macroRecording) return
        macroRecording = false
        macroPad?.hide()
        macroPad = null
        val pkg = (state.subject as? PanelSubject.Tracked)?.packageName
        val count = macroSteps.size
        if (save && pkg != null && count > 0) {
            GameExtrasPrefs.saveMacro(applicationContext, pkg, macroSteps.toList())
        }
        val message = if (save && count > 0) {
            getString(R.string.game_extra_macro_saved, count)
        } else {
            getString(R.string.game_extra_macro_stopped, count)
        }
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    /** تشغيل الماكرو المخزَّن للعبة المتتبَّعة — النتيجة عدد الخطوات التي وصلت فعلًا. */
    private fun playMacro() {
        val pkg = (state.subject as? PanelSubject.Tracked)?.packageName ?: return
        val steps = GameExtrasPrefs.load(applicationContext, pkg).macro
        if (steps.isEmpty()) {
            Toast.makeText(applicationContext, R.string.game_extra_macro_empty, Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val result = MacroRunner.play(applicationContext, steps)
            withContext(Dispatchers.Main) {
                val message = if (result.detail == null) {
                    getString(R.string.game_extra_macro_played, result.stepsRun)
                } else {
                    getString(R.string.game_extra_macro_blocked, result.stepsRun, result.detail)
                }
                runCatching { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    /** إعلان نتيجة حقنة فاشلة/مرفوضة — والتقييد بالزمن صامت لأنه ليس عطبًا. */
    private suspend fun reportInjection(event: String, outcome: InjectionOutcome) {
        if (outcome is InjectionOutcome.Delivered) return
        val message = when (outcome) {
            is InjectionOutcome.Refused -> when (outcome.reason) {
                InjectionRefusal.RATE_LIMITED -> return
                InjectionRefusal.FINGER_DOWN -> getString(R.string.game_panel_inject_finger)
                InjectionRefusal.NO_LANE -> getString(R.string.game_panel_inject_no_lane)
                InjectionRefusal.OUT_OF_BOUNDS -> getString(R.string.game_panel_inject_bounds)
            }

            is InjectionOutcome.Failed -> getString(R.string.game_panel_inject_failed, outcome.detail)
            is InjectionOutcome.Delivered -> return
        }
        withContext(Dispatchers.Main) {
            runCatching { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
        }
        AppMonitorLogger.persist("$event outcome=${outcome::class.simpleName}")
    }

    private fun syncMemo(memo: GameMemo) {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            mainHandler.post { syncMemo(memo) }
            return
        }
        val wanted = memo.visible &&
            memo.shownText.isNotEmpty() &&
            state.subject is PanelSubject.Tracked &&
            Settings.canDrawOverlays(this)
        if (!wanted) {
            memoOverlay?.hide()
            return
        }
        val overlay = memoOverlay ?: GameMemoOverlay(this).also { memoOverlay = it }
        overlay.show(memo)
    }

    /**
     * حقن اللمس: زرّا الكتف والجيروسكوب — **لا يعملان بلا لعبة متتبَّعة ولا بلا إقرار**.
     *
     * والخدمة هي المالك الوحيد لهذين السطحين (كالستارة والمذكّرة): تُنشئهما وتُخفيهما
     * وتُدمّرهما. واختيار «متتبَّعة فقط» ليس تحفّظًا زائدًا: الحقن بلا لعبة يكتب على شاشة
     * الإعدادات أو متجر التطبيقات، وهو خطر بلا مقابل.
     */
    private fun syncInjection(tracked: Boolean) {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            mainHandler.post { syncInjection(tracked) }
            return
        }
        val context = applicationContext
        val consented = InjectionConsentPrefs.acknowledged(context)

        val pads = ShoulderPadPrefs.load(context)
        val padsWanted = tracked && pads.enabled && consented && Settings.canDrawOverlays(this)
        if (padsWanted) {
            val overlay = padsOverlay ?: ShoulderPadOverlay(
                context = this,
                initial = pads.layout,
                onTap = { side, x, y -> injectPadTap(side, x, y) },
                onMoved = { side, xFraction, yFraction ->
                    ShoulderPadPrefs.savePosition(applicationContext, side, xFraction, yFraction)
                },
            ).also { padsOverlay = it }
            // يُعاد الحساب كل دورة: الدوران يغيّر أبعاد الشاشة والنسبة تبقى، فلا يبقى الزرّ
            // في موضع وضعٍ سابق.
            overlay.updateLayout(pads.layout)
            overlay.show()
        } else {
            padsOverlay?.hide()
        }

        val gyro = GyroAimPrefs.load(context)
        val gyroWanted = tracked && gyro.enabled && consented
        val controller = gyroController ?: if (gyroWanted) {
            GyroAimController(this).also { gyroController = it }
        } else {
            null
        }
        if (gyroWanted && controller != null && controller.available) {
            if (!gyroRunning) {
                controller.start(gyro)
                gyroRunning = true
            } else {
                controller.update(gyro)
            }
        } else if (gyroRunning) {
            controller?.stop()
            gyroRunning = false
        }
    }

    /** نقرة الزرّ: الحقن في مركزه — والعطب يُقال، أما التقييد بالزمن فصامت عن قصد. */
    private fun injectPadTap(side: PadSide, x: Float, y: Float) {
        lifecycleScope.launch(Dispatchers.IO) {
            val outcome = runCatching { TouchInjector.tap(applicationContext, x, y) }
                .getOrElse { InjectionOutcome.Failed(nd.max.core.input.InjectionLane.PRIVILEGED_SHELL, "exception") }
            if (outcome is InjectionOutcome.Delivered) return@launch
            val message = when (outcome) {
                is InjectionOutcome.Refused -> when (outcome.reason) {
                    InjectionRefusal.RATE_LIMITED -> return@launch
                    InjectionRefusal.FINGER_DOWN -> getString(R.string.game_panel_inject_finger)
                    InjectionRefusal.NO_LANE -> getString(R.string.game_panel_inject_no_lane)
                    InjectionRefusal.OUT_OF_BOUNDS -> getString(R.string.game_panel_inject_bounds)
                }

                is InjectionOutcome.Failed -> getString(R.string.game_panel_inject_failed, outcome.detail)
                is InjectionOutcome.Delivered -> return@launch
            }
            withContext(Dispatchers.Main) {
                runCatching { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
            }
            AppMonitorLogger.persist("EVENT=SHOULDER_PAD_TAP side=${side.name.lowercase()} outcome=${outcome::class.simpleName}")
        }
    }

    /** دورة اللمس من البلاطة: دافئة ← ليلية ← تعتيم ← إطفاء. */
    private fun cycleVeil() {
        val next = veil.cycle()
        DisplayVeilPrefs.save(applicationContext, next)
        syncVeil(next)
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
        syncCrosshair()
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
        if (HudRecorder.isLive) HudRecorder.stop(this) else HudRecorder.start(this)
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
        syncCrosshair()
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
        // إيصال الإطلاق: نتيجة آخر تطبيق لملفّ اللعبة، مقروءة من السجلّ الذي كتبه
        // `InAppProfileApplier` نفسه — لا من نيّة الإطلاق، ولا من صفر مخترع.
        val trackedPkg = (state.subject as? PanelSubject.Tracked)?.packageName
        val receiptRow = remember(trackedPkg) {
            val run = trackedPkg?.let { pkg ->
                InAppRunLog.decode(
                    applicationContext.getSharedPreferences(InAppRunLog.PREFS, MODE_PRIVATE)
                        .getString(InAppRunLog.KEY, null)
                )?.takeIf { it.pkg == pkg }
            }
            inAppReceipt(run)
        }
        val launchReceipt = when (receiptRow) {
            is InAppRunRow.Blocked -> stringResource(
                R.string.game_panel_receipt_blocked,
                stringResource(reasonTextRes(receiptRow.reason))
            )
            is InAppRunRow.Applied -> stringResource(
                R.string.game_panel_receipt_applied,
                receiptRow.written,
                receiptRow.total,
                receiptRow.verified
            )
            is InAppRunRow.Skipped -> stringResource(
                R.string.game_panel_receipt_skipped,
                stringResource(knobLabelRes(receiptRow.knob)),
                stringResource(reasonTextRes(receiptRow.reason))
            )
            else -> null
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
                syncCrosshair()
                syncClockLoop(false)
                window.setFullScreen(false, dockAt?.first, dockAt?.second)
                window.setTouchHandler(silk)
                window.setFocusable(false)
                // بعد قياس المقبض الجديد لا القديم، فيُوضع على الحافة فورًا لا بعد الاستطلاع التالي.
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ placeWindow() }, 80L)
            },
            onClose = { window.setFocusable(false); dismissPanel() },
            onCycleRefresh = { cycleRefreshRate() },
            refreshChoices = refreshChoices,
            onSelectRefresh = { hz -> selectRefreshRate(hz) },
            onToggleRecording = { toggleRecording() },
            onOpenControls = { openAppSettings() },
            onSelectProfile = { id -> selectProfile(id) },
            onToggleBypass = { toggleBypass() },
            crosshairEnabled = crosshair.enabled,
            onToggleCrosshair = { toggleCrosshair() },
            veilEnabled = veil.enabled,
            veilTone = veil.tone,
            onCycleVeil = { cycleVeil() },
            // المذكّرة تحتاج لوحة مفاتيح: النافذة تُرفع للحظة الكتابة وتُعاد عند الإغلاق،
            // فلا تبقى سارقةً للمس بعد أن ينتهي الحقل.
            onTyping = { typing -> window.setFocusable(typing) },
            trackedPackage = (state.subject as? PanelSubject.Tracked)?.packageName,
            thermal = thermal,
            onSelectThermal = { id -> selectThermal(id) },
            handleFx = silk.fx,
            handleScale = handleScale,
            origin = openOrigin,
            launchReceipt = launchReceipt
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
