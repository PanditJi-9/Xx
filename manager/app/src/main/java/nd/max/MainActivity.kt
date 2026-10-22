/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max
import nd.max.core.security.SecurityEnforcement
import nd.max.core.update.UpdateGate
import nd.max.core.security.IntegrityCheckpoint
import nd.max.core.platform.EventLog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.core.ipc.RootNodeChannel
import nd.max.ui.component.*
import nd.max.ui.design.LocalFloatingBottomBarHeight
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxSpace
import nd.max.ui.mainscreens.HomeGuideStore
import nd.max.ui.mainscreens.HomeTourOverlay
import nd.max.ui.mainscreens.HomeTourTargets
import nd.max.ui.mainscreens.LocalHomeTourRestart
import nd.max.ui.mainscreens.LocalHomeTourTargets
import nd.max.ui.navigation.*
import nd.max.ui.settings.AppLanguage
import nd.max.ui.settings.SettingsPreference
import nd.max.ui.theme.MaxManagerTheme
import nd.max.ui.theme.NavBarStyle
import nd.max.ui.theme.ThemeMotion
import nd.max.ui.theme.ThemeStudio
import nd.max.ui.util.*
import nd.max.widget.core.WidgetIntents
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /**
     * نيّة الودجة (WidgetKit) تُحفظ حالةً لا متغيّرًا محليًّا: `onNewIntent` فوق مهمة قائمة
     * (`singleTask`) يعيش خارج التركيب الأول، ويحدّث هاتين الحالتين فيقرأ الملاح الجديد.
     * و`widgetRouteSeq` عدّاد تسلسليّ يفرّق بين «الطلب نفسه مرة أخرى» و«لا طلب جديد».
     */
    private val widgetRouteState by lazy { mutableStateOf<String?>(null) }
    private val widgetRouteSeq by lazy { mutableIntStateOf(0) }

    /**
     * لغة التطبيق تُربط هنا قبل أي مورد وقبل إنشاء الواجهة. النشاط `ComponentActivity` لا يمرّ على
     * `AppCompatDelegate` (وهو ما يعمل به تبديل اللغة على API 33+)، فاللفّ هنا هو ما يجعل الاختيار
     * المحفوظ ظاهرًا على أندرويد ١٠–١٢ بدل أن يبقى محفوظًا بلا أثر. التفصيل في `AppLanguage.wrap`.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // **بوّابة النشاط: قبل `enableEdgeToEdge` وقبل الاتصال بالرفيق الجذريّ.**
        // وترتيبها هنا مقصود: لا تُفتح واجهة، ولا يُطلب امتياز، ولا يُرسم شيء — على
        // نسخةٍ لم تجتز سلامة الحزمة.
        if (SecurityEnforcement.refuseActivity(this, IntegrityCheckpoint.ACTIVITY)) return
        enableEdgeToEdge()
        RootNodeChannel.connect(this)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val fromTileType = if (intent.action == "android.service.quicksettings.action.QS_TILE_PREFERENCES") {
            val component = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, android.content.ComponentName::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME)
            }
            when (component?.className) {
                "nd.max.TileService.BypassChgTileService" -> "bypass"
                "nd.max.TileService.ProfileTileService" -> "profile"
                else -> null
            }
        } else null
        // نيّة الودجة (WidgetKit): مسار شاشة مطلوب من الإجراء الخارجيّ — تُقرأ إلى الحالة
        // التي يقرؤها الملاح (والتي يحدّثها `onNewIntent` أيضًا فوق مهمة قائمة).
        // **إطلاق فعليّ فقط (`savedInstanceState == null`)**: النشاط بلا `configChanges` فيُعاد إنشاؤه عند
        // كل تغيّر في الاتجاه أو اللغة أو الوضع الداكن، والنيّة الأصلية تبقى في `getIntent()`. فلو قُرئ
        // المسار في كل `onCreate` لأُعيد التوجيه إلى الشاشة المطلوبة بعد كل إعادة إنشاء — واللوبي بالذات
        // يقلب الاتجاه عند الدخول ويعيده عند الخروج، فكان إغلاقه يعيدك إليه إلى الأبد. والإنشاء من حالة
        // محفوظة (تدوير أو إعادة بعد قتل العملية) لا يحمل طلبًا جديدًا: الملاح يستعيد موضعه بنفسه،
        // وأمّا الطلب الجديد على نشاط قائم فيصل من `onNewIntent` دائمًا.
        if (savedInstanceState == null) {
            widgetRouteState.value = intent.getStringExtra(WidgetIntents.EXTRA_OPEN_ROUTE)
        }
        setContent {
            MaxManagerTheme {
                MainScreen(
                    fromTileType = fromTileType,
                    widgetRoute = widgetRouteState.value,
                    widgetRouteSeq = widgetRouteSeq.intValue
                )
            }
        }
    }

    /**
     * `singleTask`: لمسة الودجة الثانية لا تمرّ بـ`onCreate` بل هنا — وكانت تُهمَل، فتفتح
     * الودجة التطبيق ولا تقود إلى الشاشة المطلوبة. والعدّاد التسلسليّ يجعل الطلب نفسه
     * يُنفَّذ من جديد بدل أن يُسكت: مسارٌ مطلوب مرّتين = تنقّل مرّتين.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val route = intent.getStringExtra(WidgetIntents.EXTRA_OPEN_ROUTE) ?: return
        widgetRouteState.value = route
        widgetRouteSeq.intValue++
    }

    /**
     * `AR-32` — مراقبة انسداد الخيط الرئيسي داخل عمليتنا (قياس لا استنتاج). تُشغَّل مع
     * ظهور الواجهة وتُوقف مع اختفائها، فما لا نستطيع رؤيته لا ندّعي قياسه.
     */
    private val stallDetector = MainThreadStallDetector { report ->
        EventLog.symptom(
            screen = "UiHealth",
            symptom = "main_thread_stall",
            valueMs = report.durationMs,
            worstMs = report.worstMs,
            count = report.count,
        )
    }

    override fun onStart() {
        super.onStart()
        stallDetector.start()
    }

    override fun onStop() {
        // أسوأ انسداد يُسجَّل مرّة عند الإخفاء: هذا هو الرقم الذي يُقارَن به لاحقًا لنفي
        // التحسّن أو إثباته، ولا معنى لسطر يقيس انسدادًا واحدًا بلا سياقه.
        if (stallDetector.worstStallMs > 0L) {
            EventLog.symptom(
                screen = "UiHealth",
                symptom = "main_thread_stall_session",
                valueMs = stallDetector.worstStallMs,
                count = stallDetector.stallCount,
            )
        }
        stallDetector.stop()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        RootNodeChannel.disconnect()
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(fromTileType: String? = null, widgetRoute: String? = null, widgetRouteSeq: Int = 0) {
    val navController = rememberNavController()
    val context = LocalContext.current
    // موارد من `LocalResources.current`: كل نصوص هذه الشاشة ودوالها تُقرأ داخل `launch`/`onClick`،
    // وهي سياقات لا تُبطل فيها قراءة `LocalContext.current.resources` عند تغيّر التكوين.
    val resources = LocalResources.current
    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val primaryRoutes = remember { MaxDestination.PrimaryRoutes }
    val navActions = remember(navController) { MaxNavActions(navController) }
    // سطح الأقسام الأربعة: حالة الصفحة وحافظ حالاتها يعيشان في الجذر لا في السطح نفسه،
    // فينجو موضع كل قائمة حين يخرج السطح من التركيب عند فتح شاشة فرعية.
    val deckPagerState = rememberPagerState(initialPage = 0) {
        MaxDestination.PrimaryDestinations.size
    }
    val deckStateHolder = rememberSaveableStateHolder()
    var deckSynced by remember { mutableStateOf(false) }

    // **والسجلّ الواحد يُكتب هنا — في المكان الوحيد الذي تمرّ به كل وجهة:** مستمعٌ واحدٌ على
    // الرّسّام يقيس **أيّ** شاشةٍ تُفتح من أيّ مكان، لا نقرات مكان بعينه؛ فمن يفتح الحرارة من
    // شاشة التحكّم تُحتسب له كما لو فتحها من بطاقة الرئيسية. والكتابة **محلية على الجهاز**
    // (`SharedPreferences` ← `ScreenUsageStore`) ولا تخرج منه ولا تُقرأ من الشبكة.
    //
    // **ومفتاحها المسار لا مفتاح بطاقة (الجولة ٢٠٣):** كان هذا المستمع يسجّل في المنصة وحدها
    // بمسارٍ يُترجَم إلى مفتاح بطاقة، فما ليس في بركة البطاقات لم يُسجَّل أصلًا — ولا يقرؤه
    // المُوجِّد. صار العدّ **لكل وجهة** بالمسار (هويّتها، ADR-02)، ومنه تقرأ المنصة ما يعنّيها
    // عبر `homeDeckUsage` — فسجلٌّ واحد، والفاتحان لا يفترقان يومًا.
    val screenUsageStore = remember(context) { ScreenUsageStore.of(context) }
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            screenUsageStore.record(destination.route)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
    LaunchedEffect(fromTileType) {
        when (fromTileType) {
            "bypass" -> navController.navigate(MaxDestination.BypassCharging.route) {
                popUpTo(MaxDestination.Now.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            "profile" -> navController.navigate(MaxDestination.Now.route) {
                popUpTo(MaxDestination.Now.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    // نيّة ودجة: تفتح الشاشة المطلوبة — و`launchSingleTop` يمنع تكديس نسخ. و`widgetRouteSeq`
    // يعيد تنفيذ الطلب نفسه إن وصل ثانيةً، بعد أن كانت المفتاحَ الواحد تُسكته لأن القيمة
    // لم تتغيّر (ودجة → نفس الشاشة بعد رجوع المستخدم).
    LaunchedEffect(widgetRoute, widgetRouteSeq) {
        val target = when (widgetRoute) {
            WidgetIntents.ROUTE_STORAGE -> MaxDestination.StorageCare.route
            WidgetIntents.ROUTE_CONTROL -> MaxDestination.Control.route
            WidgetIntents.ROUTE_ISLAND -> MaxDestination.DynamicIsland.route
            WidgetIntents.ROUTE_GAME_LOBBY -> MaxDestination.GameLobby.route
            else -> null
        }
        if (target != null) {
            navController.navigate(target) { launchSingleTop = true }
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // Primary destination currently selected, or null on hubs/feature screens.
    // Used for cheap local state refreshes; never for device probing.
    val currentPrimaryRoute = currentRoute?.takeIf { it in primaryRoutes }
    val coroutineScope = rememberCoroutineScope()
    // إيماءة واحدة في الجذر: الشاشة ذات التبويبات تسجّل معالجها، وغيرها يعود للقسم/الرجوع.
    val swipeController = remember { SwipeNavigationController() }
    val preferenceSettings = remember(context) { SettingsPreference.getInstance(context) }
    val swipePreference by preferenceSettings.isSwipeNavigationEnabled.collectAsState()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val configuration = LocalConfiguration.current
    val useNavigationRail = configuration.screenWidthDp >= 840
    val appPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val hasCompletedGetStarted = remember {
        appPrefs.getBoolean("has_completed_get_started", false)
    }
    val isPrimaryRoute = currentRoute != null && currentRoute in primaryRoutes
    // يظهر السطح على الأقسام الأربعة، وقبل استقرار أول مسار (`currentRoute == null`) كي لا
    // يبقى فراغُ سطحٍ بين الإقلاع وأول إطار.
    val showPrimaryDeck = isPrimaryRoute || (currentRoute == null && hasCompletedGetStarted)
    // **السطح يبقى مركَّبًا خلف أول شاشة فرعية (الرجوع التنبّئي).** كان يخرج من التركيب عند فتح
    // شاشة فرعية، فإذا سحب المستخدم من الحافّة للرجوع لم يجد الملاحُ شيئًا تحتها (المقعد فارغ
    // بقصد) وظهر فراغٌ أسود ثم قفزت الشاشة السابقة. وهو يبقى للمستوى الأول فقط: الشاشة التي
    // تحتها في المكدّس قسمٌ رئيسيّ. والشاشة الفرعية معتمة بنفسها وتحجب اللمس (`MaxNavGraph`).
    // `previousBackStackEntry` واجهة عامة مستقرّة (يتجاوز رسم الملاح نفسه)، ويُعاد قراءتها كلما
    // تغيّر المدخل الأعلى — وهو ما يرصده `navBackStackEntry` أعلاه.
    val belowTopRoute = remember(navBackStackEntry) {
        navController.previousBackStackEntry?.destination?.route
    }
    val deckBehindScreen = hasCompletedGetStarted && !isPrimaryRoute && currentRoute != null &&
        belowTopRoute != null && belowTopRoute in primaryRoutes
    val composeDeck = showPrimaryDeck || deckBehindScreen
    // **البار يتبع الصفحة لا المسار.** المسار يُكتب عند استقرار الصفحة، فكان مؤشّر البار يتأخّر
    // عن السحب كلّه، ونقرةٌ أثناء الاستقرار كانت تُلغى (الاستقرار القديم يعيد المسار). `targetPage`
    // يتحرّك مع بدء السحب أو النقر مباشرةً، فيبدأ المؤشّر والصفحة معًا.
    val barSelectedRoute by remember(deckPagerState, currentRoute, showPrimaryDeck, deckSynced) {
        derivedStateOf {
            if (showPrimaryDeck && deckSynced) {
                MaxDestination.PrimaryDestinations.getOrNull(deckPagerState.targetPage)?.route ?: currentRoute
            } else {
                currentRoute
            }
        }
    }
    // عند الخروج من الأقسام (شاشة فرعية) يُصفَّر العلم: العودة بعدها تقفز إلى مسارها الحقيقيّ
    // بلا انزلاقٍ عبر صفحات لم يرها المستخدم، ثم يعود السحب يزامن بالحركة كالمعتاد.
    LaunchedEffect(showPrimaryDeck) {
        if (!showPrimaryDeck) deckSynced = false
    }
    /**
     * مزامنة واحدة بين المسار والصفحة.
     *
     * - أول مزامنة **تقفز بلا حركة**: إقلاعٌ على قسم غير الرئيسية (ودجة تفتح التحكّم مثلًا) لا
     *   يجوز أن يُرى فيه انزلاقٌ لم يطلبه المستخدم.
     * - وما بعدها يتحرّك بفيزياء السحب نفسها، فالنقر على الشريط والقفز بالجولة يبدوان كحركة
     *   بطاقةٍ واحدة لا كقطعٍ إلى شاشة أخرى.
     * - وأثناء سحب المستخدم لا تُقاطَع الصفحة: استقرارها هو من يكتب المسار ([onDeckSettled]).
     */
    LaunchedEffect(currentRoute) {
        val target = MaxDestination.PrimaryDestinations.indexOfFirst { it.route == currentRoute }
        if (target < 0) return@LaunchedEffect
        if (!deckSynced) {
            deckPagerState.scrollToPage(target)
            deckSynced = true
        } else if (deckPagerState.currentPage != target && !deckPagerState.isScrollInProgress) {
            deckPagerState.animateScrollToPage(
                page = target,
                animationSpec = PrimaryDeckSnapSpring,
            )
        }
    }
    /**
     * ما استقرّت عليه الصفحة يصير هو المسار — لا العكس.
     *
     * والشرطان يمنعان دورتين: `deckSynced` يمنع أول نداء قبل معرفة مسار الإقلاع، ومقارنة المسار
     * تمنع إعادة التنقّل إلى الوجهة نفسها بعد انتقال برمجيّ (شريط التنقّل أو الجولة).
     */
    val onDeckSettled: (Int) -> Unit = { page ->
        val destination = MaxDestination.PrimaryDestinations.getOrNull(page)
        if (deckSynced && isPrimaryRoute && destination != null && currentRoute != destination.route) {
            navActions.navigateToPrimary(destination)
        }
    }
    LaunchedEffect(Unit) {
        WallpaperCache.init(context)
    }

    // **جولة أول فتح في الجذر لا في الرئيسية (طلب المالك، ٢٠٢٦-١٠-١٣):** الجولة أصبحت تعبر التبويبات
    // الأربعة — الرئيسية، فالتحكم، فالتطبيقات، فالإعدادات — ثم **تعود إلى الرئيسية عند الإتمام**.
    // ولو بقيت في شاشة الرئيسية لضاعت خطوتها لحظة الانتقال: الشاشة تخرج من التركيب ومعها `index`.
    // والملك واحد للحالة: سجلّ واحد (`HomeGuideStore`) وغطاء واحد، و`?` في الرأس يطلب الإعادة منه.
    val tourStore = remember(context) { HomeGuideStore.of(context) }
    var tourActive by remember { mutableStateOf(!tourStore.tourFinished) }
    val tourTargets = remember { HomeTourTargets() }
    val restartTour: () -> Unit = remember(tourStore) {
        {
            tourStore.restart()
            tourActive = true
        }
    }
    // وتُعرَض على التبويبات الأربعة وحدها: شاشة البداية مسارها مستقلّ، والشاشات الفرعية تُفتح من
    // داخل الجولة لا العكس — وغطاءٌ فوق شاشة تفاصيل يشرح عنصرًا في شاشة أخرى ويحجب ما فُتح لأجله.
    val tourVisible = tourActive && hasCompletedGetStarted && isPrimaryRoute
    // مطفأ على شاشة البداية والأقسام الأربعة وأثناء الجولة: الأقسام يملكها سطح البطاقات،
    // وشاشة البداية والجولة لكلتيهما أزرارها ومسارها.
    val swipeEnabled = swipePreference && currentRoute != null &&
        currentRoute != MaxDestination.GetStarted.route && !isPrimaryRoute && !tourVisible
    /**
     * قرار السحب الواحد على الشاشات الفرعية:
     * 1) تبويبات الشاشة إن سجّلت معالجًا وأجابت «تولّيت الأمر».
     * 2) رجوع عبر `OnBackPressedDispatcher` في بقية الشاشات — فيمرّ أولًا على أي
     *    `BackHandler` مفتوح (ورقة سفلية مثلًا) قبل أن يُغلق الشاشة.
     *
     * **ولا قسم رئيسي هنا بعد اليوم:** الانتقال بين الأقسام بطاقاتٌ في `PrimaryDeck` يملكها
     * أفقيًّا `HorizontalPager`، فلا يشاركه جذرُ الإيماءة قرارًا.
     */
    val handleSwipe: (SwipeDirection) -> Unit = { direction ->
        if (direction != SwipeDirection.NONE) {
            val screenHandlers = swipeController.current
            val screenHandled = when {
                screenHandlers == null -> false
                direction == SwipeDirection.NEXT -> screenHandlers.onNext?.invoke() == true
                direction == SwipeDirection.PREVIOUS -> screenHandlers.onPrevious?.invoke() == true
                else -> false
            }
            if (!screenHandled && direction == SwipeDirection.PREVIOUS) {
                val dispatcher = backDispatcher
                if (dispatcher != null) dispatcher.onBackPressed() else navController.popBackStack()
            }
        }
    }
    var isBlurEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("expressive_blur_ui", false)) }
    // نمط شريط التنقل (عائم/ملتصق) — يُقرأ كما يُقرأ البلور: عند الإقلاع، وعند تحديث الحالة،
    // وعند دخول أيّ وجهة رئيسّة (شاشة السمة فرعية، فيُعاد القراءة عند العودة منها).
    var navBarStyle by remember { mutableStateOf(ThemeStudio.navBarStyle(settingsPrefs)) }
    val hazeState = remember { HazeState() }
    // ADR-17 session state: probed ONCE at start and refreshed explicitly.
    // The route space is ~50 destinations, so keying this on every route meant
    // a root + module probe on every hub and feature entry (F-05).
    var rootStatus by remember { mutableStateOf(false) }
    val navItems = remember {
        MaxDestination.PrimaryDestinations.map { NavItem(it.route, it.titleRes, it.icon) }
    }
    val pendingReboot by RebootManager.pendingReboot.collectAsState()
    val refreshStatus: suspend () -> Unit = {
        // **سؤالٌ واحد هنا: هل الجذر متاح.** كان يُقاس معه «هل الوحدة مثبَّتة» ويُخزَّن حقلًا
        // لا يقرؤه أحد بعد فصل الملاحة عن الوحدة — وقياسٌ لا يُقرأ عملٌ بلا ثمرة، وحالة الوحدة
        // تُقرأ حيث تُعرض فعلًا: الرئيسية (`HomeViewmodel`) وشاشة البداية عند التثبيت.
        rootStatus = withContext(Dispatchers.IO) { RootUtils.requestRootAccess() }
        isBlurEnabled = settingsPrefs.getBoolean("expressive_blur_ui", false)
        navBarStyle = ThemeStudio.navBarStyle(settingsPrefs)
        RebootManager.refreshModuleFlag()
    }
    LaunchedEffect(Unit) {
        refreshStatus()
    }
    // Cheap local preference re-read when a primary destination is entered.
    // No device probe here: root/module state is owned by the session above.
    LaunchedEffect(currentPrimaryRoute) {
        isBlurEnabled = settingsPrefs.getBoolean("expressive_blur_ui", false)
        navBarStyle = ThemeStudio.navBarStyle(settingsPrefs)
    }
    val installingDialog = rememberInstallingDialog()
    val updateDialog = rememberConfirmDialog(
        onConfirm = {
            coroutineScope.launch {
                installingDialog.withInstalling {
                    val result = withContext(Dispatchers.IO) {
                        Shell.cmd(
                            "cp ${MaxManagerPaths.MODULE_APK} /data/local/tmp/MaxManager_tmp.apk",
                            "sleep 5 && pm install -r /data/local/tmp/MaxManager_tmp.apk",
                            "rm -f /data/local/tmp/MaxManager_tmp.apk",
                        ).exec()
                    }
                    if (result.isSuccess) {
                        Toast.makeText(context, resources.getString(R.string.toast_update_success), Toast.LENGTH_SHORT).show()
                    } else {
                        val errorLog = result.out.joinToString("\n").ifEmpty { resources.getString(R.string.status_unknown) }
                        Toast.makeText(context, resources.getString(R.string.toast_install_fail, errorLog), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )
    val rebootDialog = rememberConfirmDialog(
        onConfirm = {
            Shell.cmd("svc power reboot || reboot").submit()
        }
    )
    LaunchedEffect(rootStatus) {
        if (rootStatus) {
            val (moduleVC, apkAvailable, rebootPending) = withContext(Dispatchers.IO) {
                Triple(
                    RootUtils.getModuleVersionCode(),
                    RootUtils.isUpdateApkAvailable(),
                    RootUtils.isModuleUpdatePendingReboot(),
                )
            }
            // `AR-04`: إصدار التطبيق يُقرأ من **مصدر واحد** (`VersionIdentity`)، لا بتكرار
            // `getPackageInfo` هنا مع تفريع حسب إصدار النظام كما كان.
            val appVC = VersionIdentity.readApp(context).versionCode
            // مفتاح «تعطيل التحديثات» (شاشة التحكّم): هذا الحوار إعلانُ تحديثٍ أيضًا، فلولا هذا
            // الشقّ لعادت النافذة من الباب الثالث بعد أن أُغلقت الأولين.
            if (appVC >= 0L && appVC < moduleVC.toLong() && apkAvailable && !UpdateGate.isDisabled(context)) {
                updateDialog.showConfirm(
                    title = resources.getString(R.string.dialog_update_available_title),
                    content = resources.getString(R.string.dialog_update_available_content, appVC, moduleVC),
                    confirm = resources.getString(R.string.dialog_update_available_confirm),
                    dismiss = resources.getString(R.string.dialog_update_available_dismiss),
                )
            }
            if (rebootPending) {
                rebootDialog.showConfirm(
                    title = resources.getString(R.string.dialog_module_update_title),
                    content = resources.getString(R.string.dialog_module_update_content),
                    confirm = resources.getString(R.string.dialog_module_update_confirm),
                    dismiss = resources.getString(R.string.dialog_module_update_dismiss),
                )
            }
        }
    }
    val isFabVisible = remember { mutableStateOf(true) }
    // BottomNavBar floats *over* the NavHost (see the Box below) instead of living in a
    // Scaffold's bottomBar slot, and the NavHost is deliberately left full-bleed so page
    // content keeps scrolling underneath the pill. That pass-through is the whole point of
    // the design: a bar whose backdrop you can see moving through its blur reads as
    // floating, whereas insetting the NavHost by the bar's height (what this used to do)
    // cropped the scroll viewport short and left a dead strip of background behind the pill
    // — content visibly disappeared before it ever reached the bar.
    //
    // Since nothing reserves layout space for the bar anymore, it is published to the
    // screens instead: `LocalFloatingBottomBarHeight` carries the bar's real rendered height
    // (it is not a fixed constant — labels can wrap to two lines, e.g. longer Arabic
    // strings) so each scrollable page body can reserve exactly that much as bottom content
    // padding. Content scrolls behind the bar; the last row still stops above it.
    //
    // Seeding this at 0px left a window — most visible right after switching a primary
    // screen's own layout (e.g. Control's compact/expanded toggle) — where the bar was
    // already on screen but no space had been reserved for it yet, so the last row could
    // render behind it. Seed it with the safe MaxSpace.bottomBarReserve floor and only ever
    // grow it from the real measurement, so content always has at least that much clearance.
    val density = LocalDensity.current
    // The seed must already include the system navigation-bar inset: BottomNavBar applies
    // `.windowInsetsPadding(WindowInsets.navigationBars)` itself, so its *real* measured
    // height (captured below via onSizeChanged) always contains that inset. A seed of just
    // MaxSpace.bottomBarReserve was missing it, so on the very first frame(s) — before
    // onSizeChanged ever fires — page bodies were under-padded by the inset's worth of space
    // (~24–48dp on most devices), letting the last visible card peek out from behind the bar.
    val navInsetPx = WindowInsets.navigationBars.getBottom(density)
    var bottomBarHeightPx by remember(density, navInsetPx) {
        mutableIntStateOf(with(density) { MaxSpace.bottomBarReserve.roundToPx() } + navInsetPx)
    }
    // **الملاحة لا تُعلَّق على الجذر ولا على الوحدة.** كان الشرط `rootStatus && moduleInstalled`
    // يُخفي الشريط السفلي كلّه على جهاز بلا جذر أو بلا وحدة مثبَّتة — فيبقى المستخدم في شاشة
    // واحدة بلا تنقّل، ويظنّ التطبيق معطوبًا (وهو شرط قبول صريح: «لا يعتمد ظهور الشريط السفلي
    // على تشغيل daemon»). والصواب أن الشريط بنيةُ واجهة: يظهر على الوجهات الرئيسّة بحسب المسار
    // وحده، وما لا يعمل بلا جذر تُعلنه الشاشة نفسها بحالة قدرة (`MaxConditionKind.RootRequired`)
    // بدل أن يُسدّ الطريق كلّه. وحالة الجذر تبقى لما يخصّها فعلًا: إشعار تحديث الوحدة، وزرّ
    // إعادة التشغيل المعلّق.
    val isPrimaryBarVisible = isPrimaryRoute
    // Reserved bottom space for the pages that show the bar. Zero in navigation-rail
    // layouts (the rail lives on the start edge, so the bottom stays free) and on every
    // route without the bar. The value already includes the system navigation-bar inset,
    // because the bar applies `.windowInsetsPadding(navigationBars)` itself.
    val bottomBarInset = if (!useNavigationRail && isPrimaryBarVisible) {
        with(density) { bottomBarHeightPx.toDp() }
    } else 0.dp
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    if (available.y < -10f) isFabVisible.value = false
                    else if (available.y > 10f) isFabVisible.value = true
                }
                return Offset.Zero
            }
        }
    }
    CompositionLocalProvider(
        LocalAppHazeState provides hazeState,
        LocalFloatingBottomBarHeight provides bottomBarInset,
        // سجلّ المراسي يُعلَّق أثناء الجولة وحدها: خارجها تصير مراسي الشاشات الأربع بلا فعل ولا
        // قياس، فلا يدفع من لا جولة له ثمن `onGloballyPositioned` على كل كتلة معلَّمة.
        LocalHomeTourTargets provides if (tourVisible) tourTargets else null,
        LocalHomeTourRestart provides restartTour,
        LocalSwipeNavigationController provides swipeController,
    ) {
        RootDialogsProvider {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .swipeNavigation(enabled = swipeEnabled, onSwipe = handleSwipe)
                    // The app's real background, painted behind everything. It shows through
                    // wherever a screen's own body does not cover the window (the pill's
                    // rounded corners, the gesture-bar strip) and keeps those slivers from
                    // falling through to the raw window background (solid black) — which is
                    // what used to make the pill look bolted onto a black slab.
                    .background(MaterialTheme.colorScheme.background),
            ) {
                // **سطح الأقسام أوّلًا، والملاح فوقه للشاشات الفرعية وحدها.**
                // مسارات الأقسام الأربعة في `maxNavGraph` صارت مقاعد فارغة، فـ`NavHost` لا يرسم
                // شاشةً فوق السطح في السكون — لكنه يبقى هو من يملك الرجوع والتكديس، ولهذا لم
                // يُستبدل. وحين يكون السطح ظاهرًا يُترك خلفه شفّافًا، وحين تُفتح شاشة فرعية يخرج
                // السطح من التركيب (فلا لمسة شبح خلف شاشة معتمة) ويرتدي الملاح سطحَ التطبيق.
                if (composeDeck) {
                    // حشوة المحتوى السفليّة للسطح ثابتة (ارتفاع البار الحقيقي) حتى وهو خلف شاشة
                    // فرعية: القيمة العامة تصير صفرًا هناك (لا بار)، فكانت صفحاته ستُعاد تخطيطًا
                    // وتقفز أثناء إيماءة الرجوع.
                    val deckBottomInset = if (!useNavigationRail) {
                        with(density) { bottomBarHeightPx.toDp() }
                    } else 0.dp
                    CompositionLocalProvider(LocalFloatingBottomBarHeight provides deckBottomInset) {
                        PrimaryDeck(
                            pagerState = deckPagerState,
                            navController = navController,
                            activeRoute = currentRoute,
                            gesturesEnabled = swipePreference && !tourVisible && showPrimaryDeck,
                            saveableStateHolder = deckStateHolder,
                            tourTargets = if (tourVisible) tourTargets else null,
                            onSettledPage = onDeckSettled,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = if (useNavigationRail) 96.dp else 0.dp)
                                .nestedScroll(nestedScrollConnection)
                                .then(
                                    // مصدر الضباب هو ما يظهر **فوق**: السطح حين يعلو، والملاح
                                    // حين تعلو شاشة فرعية (فزجاجها يطمس محتواها لا السطح خلفها).
                                    if (isBlurEnabled && showPrimaryDeck) Modifier.hazeSource(state = hazeState) else Modifier
                                ),
                        )
                    }
                }
                NavHost(
                    navController = navController,
                    startDestination = if (hasCompletedGetStarted) {
                        MaxDestination.Now.route
                    } else MaxDestination.GetStarted.route,
                    modifier = Modifier
                        .fillMaxSize()
                        // Full-bleed to the bottom edge: content scrolls *behind* the
                        // floating bar (each page body reserves the bar's height as bottom
                        // content padding — see LocalFloatingBottomBarHeight). `start` still
                        // reserves space for the navigation rail on wide layouts, because
                        // the rail covers the content instead of the other way round.
                        .padding(start = if (useNavigationRail) 96.dp else 0.dp)
                        // شفّاف فوق سطح الأقسام (المقاعد فارغة)، ومعتم على الشاشات الفرعية
                        // وحدها — فهو خلفية الشاشات التي ترسم على `Scaffold` شفّاف.
                        // شفّاف ما دام السطح مركَّبًا تحته (فوقه أو خلف شاشة فرعية معتمة بنفسها).
                        .background(
                            if (composeDeck) Color.Transparent else MaterialTheme.colorScheme.surface
                        )
                        .nestedScroll(nestedScrollConnection)
                        .then(
                            if (!showPrimaryDeck && isBlurEnabled) {
                                Modifier.hazeSource(state = hazeState)
                            } else {
                                Modifier
                            }
                        ),
                    enterTransition = {
                        // مستوى الحركة يُقرأ **لحظة الانتقال** لا لحظة التركيب: لا حالة تُراقَب
                        // ولا إعادة رسم — الانتقال نفسه هو القارئ، فيسري الخيار فورًا.
                        when (ThemeStudio.motion(settingsPrefs)) {
                            // «بلا حركة»: التنقّل نفسه بلا مشهد.
                            ThemeMotion.NONE -> EnterTransition.None
                            // «هادئة»: تلاشٍ قصير وحده — بلا انزلاق ولا تحجيم.
                            ThemeMotion.CALM -> fadeIn(
                                animationSpec = tween(180, easing = LinearOutSlowInEasing)
                            )
                            ThemeMotion.FULL ->
                                if (initialState.destination.route == MaxDestination.GetStarted.route && targetState.destination.route in primaryRoutes) {
                                    // سقف الحركة (DESIGN.md): 360ms هو الأقصى؛ كان 700 = ضِعف السقف في أول
                                    // انتقال يراه المستخدم (§٩ · لا حركة زائدة).
                                    fadeIn(animationSpec = tween(MaxDuration.deliberate))
                                } else if (targetState.destination.route !in primaryRoutes) {
                                    fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                                        slideInVertically(
                                            initialOffsetY = { it / 10 },
                                            animationSpec = tween(260, easing = FastOutSlowInEasing)
                                        ) +
                                        scaleIn(initialScale = 0.98f, animationSpec = tween(260, easing = FastOutSlowInEasing))
                                } else {
                                    fadeIn(animationSpec = tween(180, easing = LinearOutSlowInEasing)) +
                                        scaleIn(
                                            initialScale = 0.985f,
                                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                                        )
                                }
                        }
                    },
                    exitTransition = {
                        when (ThemeStudio.motion(settingsPrefs)) {
                            ThemeMotion.NONE -> ExitTransition.None
                            ThemeMotion.CALM -> fadeOut(
                                animationSpec = tween(140, easing = FastOutSlowInEasing)
                            )
                            ThemeMotion.FULL ->
                                if (initialState.destination.route == MaxDestination.GetStarted.route && targetState.destination.route in primaryRoutes) {
                                    fadeOut(animationSpec = tween(MaxDuration.deliberate))
                                } else if (initialState.destination.route in primaryRoutes && targetState.destination.route !in primaryRoutes) {
                                    fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                        scaleOut(targetScale = 0.985f, animationSpec = tween(180, easing = FastOutSlowInEasing))
                                } else {
                                    fadeOut(animationSpec = tween(120))
                                }
                        }
                    },
                    popEnterTransition = {
                        when (ThemeStudio.motion(settingsPrefs)) {
                            ThemeMotion.NONE -> EnterTransition.None
                            ThemeMotion.CALM -> fadeIn(
                                animationSpec = tween(180, easing = LinearOutSlowInEasing)
                            )
                            ThemeMotion.FULL ->
                                if (initialState.destination.route !in primaryRoutes && targetState.destination.route in primaryRoutes) {
                                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                        scaleIn(initialScale = 0.985f, animationSpec = tween(220, easing = FastOutSlowInEasing))
                                } else {
                                    fadeIn(animationSpec = tween(180, easing = LinearOutSlowInEasing)) +
                                        scaleIn(
                                            initialScale = 0.985f,
                                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                                        )
                                }
                        }
                    },
                    popExitTransition = {
                        when (ThemeStudio.motion(settingsPrefs)) {
                            ThemeMotion.NONE -> ExitTransition.None
                            ThemeMotion.CALM -> fadeOut(
                                animationSpec = tween(140, easing = FastOutSlowInEasing)
                            )
                            ThemeMotion.FULL ->
                                if (initialState.destination.route !in primaryRoutes) {
                                    fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                                        slideOutVertically(
                                            targetOffsetY = { it / 12 },
                                            animationSpec = tween(200, easing = FastOutSlowInEasing)
                                        ) +
                                        scaleOut(targetScale = 0.98f, animationSpec = tween(200, easing = FastOutSlowInEasing))
                                } else {
                                    fadeOut(animationSpec = tween(120))
                                }
                        }
                    },
                ) {
                    maxNavGraph(navController)
                }
                // No bottom scrim is painted here on purpose. There used to be a
                // transparent→surface gradient sitting behind the pill (to blend the
                // gesture-bar strip), but now that page content runs underneath the bar it
                // turned into a haze band that faded content out just before it reached the
                // bottom edge — the exact "the bar is not floating" impression this change
                // removes. The bar's own translucent surface is the only thing between the
                // scrolling content and the bottom edge now.
                AnimatedVisibility(
                    visible = isPrimaryBarVisible,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(
                        if (useNavigationRail) Alignment.CenterStart else Alignment.BottomCenter,
                    ),
                ) {
                    val onItemSelected: (String) -> Unit = { route ->
                        // **النقر يحرّك الصفحة أولًا، والمسار يلحق بها عند الاستقرار.** كان النقر
                        // يغيّر المسار فقط وينتظر مزامنةً تتجاهل الحركة إن كانت صفحةٌ تستقرّ بعد
                        // سحب، ثم يعيد استقرارُ تلك الصفحة المسارَ إلى ما كان — فتضيع النقرة
                        // الأولى ويحتاج المستخدم إلى نقرتين. `animateScrollToPage` يقطع أي
                        // استقرارٍ جارٍ (الأولوية نفسها) فتُنفَّذ النقرة من أول مرة.
                        val index = MaxDestination.PrimaryDestinations.indexOfFirst { it.route == route }
                        if (index >= 0) {
                            if (showPrimaryDeck && deckSynced) {
                                coroutineScope.launch {
                                    deckPagerState.animateScrollToPage(
                                        page = index,
                                        animationSpec = PrimaryDeckSnapSpring,
                                    )
                                }
                            } else {
                                navActions.navigateToPrimary(MaxDestination.PrimaryDestinations[index])
                            }
                        }
                    }
                    if (useNavigationRail) {
                        NavigationRailBar(
                            items = navItems,
                            selectedRoute = barSelectedRoute ?: MaxDestination.Now.route,
                            isBlurEnabled = isBlurEnabled,
                            hazeState = hazeState,
                            style = navBarStyle,
                            modifier = Modifier.align(Alignment.CenterStart),
                            onItemSelected = onItemSelected,
                        )
                    } else {
                        BottomNavBar(
                            items = navItems,
                            selectedRoute = barSelectedRoute ?: MaxDestination.Now.route,
                            isBlurEnabled = isBlurEnabled,
                            hazeState = hazeState,
                            style = navBarStyle,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .onSizeChanged { size ->
                                    // Only ever grow past the safe reserve (e.g. a real
                                    // two-line label) — never shrink below it, so a
                                    // transient small measurement can't under-reserve space
                                    // for the content behind it.
                                    val minPx = with(density) { MaxSpace.bottomBarReserve.roundToPx() }
                                    bottomBarHeightPx = maxOf(size.height, minPx)
                                },
                            onItemSelected = onItemSelected,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = isPrimaryBarVisible && pendingReboot && isFabVisible.value,
                    enter = scaleIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(),
                    exit = scaleOut(animationSpec = tween(200, easing = FastOutLinearInEasing)) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 24.dp,
                            bottom = (if (useNavigationRail) 24.dp else 116.dp) +
                                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                        ),
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            rebootDialog.showConfirm(
                                title = resources.getString(R.string.dialog_reboot_required_title),
                                content = resources.getString(R.string.dialog_reboot_required_content),
                                confirm = resources.getString(R.string.reboot),
                                dismiss = resources.getString(R.string.dialog_update_available_dismiss),
                            )
                        },
                        icon = { Icon(Icons.Rounded.RestartAlt, contentDescription = stringResource(R.string.reboot)) },
                        text = { Text(stringResource(R.string.reboot), fontWeight = FontWeight.Bold) },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    )
                }
            }
            // **وغطاء الجولة آخر الجذر وفوق الشريط السفلي:** الجولة تعبر التبويبات، فلو رُسم تحت
            // الشريط لرأى المستخدم في التبويب الجديد شريطًا بلا غطاء ولمست لمسةُ الشريط الجولةَ
            // خارج خطوتها. وهو هنا _مانعُ لمسٍ كامل_ — والمخرج واحد: «تخطّي» أو إتمامها.
            if (tourVisible) {
                HomeTourOverlay(
                    targets = tourTargets,
                    currentRoute = currentRoute,
                    // التنقّل عبر بوّابة التبويبات نفسها (`navigateToPrimary`): بها يُحفظ موضع
                    // كل تبويب ويُستعاد، وبها لا يتكدّس تبويبٌ على نفسه في الرجوع.
                    onNavigate = { destination -> navActions.navigateToPrimary(destination) },
                    onFinish = {
                        tourActive = false
                        tourStore.tourFinished = true
                        // **والرجوع إلى الرئيسية عند الإتمام** (طلب المالك): بعد الإعدادات ينتهي
                        // القارئ في شاشة يبدأ منها، لا في آخر تبويب مرّ به.
                        navActions.navigateToPrimary(MaxDestination.Now)
                    },
                )
            }
            ConfirmDialogHost(handle = updateDialog)
            ConfirmDialogHost(handle = rebootDialog)
            // سطح التحديث عن بُعد: نافذة «يتوفّر تحديث» وشاشة حجب النسخة القديمة — في الجذر
            // لأن الحكم على التطبيق كلّه لا على وجهة بعينها، ولأن الحجب يجب أن يسبق أي شاشة.
            UpdateHost()
            // نافذة «التحديث المنشور من اللوحة»: الإصدار ورابط التنزيل كما كتبهما المالك في
            // Max Control، والزرّ يفتح الرابط في المتصفّح. قناة ثانية مستقلة عن قناة المانيفست.
            ConfigUpdateHost()
        }
    }
}
