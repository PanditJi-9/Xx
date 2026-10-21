/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import android.text.format.Formatter
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.AppMonitorLogger
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.core.gamespace.GameLibraryAccess
import nd.max.core.gamespace.GameLobbyMeta
import nd.max.core.gamespace.GameLobbyMetaReader
import nd.max.core.gamespace.LobbyModel
import nd.max.core.gamespace.LobbyOrder
import nd.max.core.gamespace.LobbyPrefs
import nd.max.core.gamespace.LobbyPrefsState
import nd.max.core.gamespace.LobbySort
import nd.max.core.gamespace.SidePanelArmedStore
import nd.max.core.gamespace.gameLibrary
import nd.max.core.platform.ForegroundAppResolver
import nd.max.service.GamePanelService
import nd.max.ui.component.LOBBY_SLAB_ASPECT
import nd.max.ui.component.LobbyDeck
import nd.max.ui.component.LobbyDeckCard
import nd.max.ui.component.LobbyDeckPad
import nd.max.ui.component.LobbyDeckSkeleton
import nd.max.ui.component.LobbyInfoPanel
import nd.max.ui.component.LobbyKeepScreenOn
import nd.max.ui.component.LobbyNotice
import nd.max.ui.component.LobbyPalette
import nd.max.ui.component.LobbyPositionRail
import nd.max.ui.component.LobbyPullHint
import nd.max.ui.component.LobbyRoundButton
import nd.max.ui.component.LobbySnapSpring
import nd.max.ui.component.LobbyStageBackdrop3D
import nd.max.ui.component.LobbyStatusCapsule
import nd.max.ui.component.LobbyTopTabs
import nd.max.ui.component.LobbyWindowEffect
import nd.max.ui.component.lobbyIntro
import nd.max.ui.component.lobbyLoop
import nd.max.ui.component.rememberLobbyAnimationsEnabled
import nd.max.ui.component.rememberLobbyIntro
import nd.max.ui.component.rememberLobbyTone
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxSpace
import nd.max.ui.navigation.openAppSettings
import nd.max.ui.util.AppConfig
import nd.max.ui.util.GamePanelPrefs
import nd.max.ui.util.customizedFieldCount
import nd.max.ui.viewmodel.AppSettingsViewModel
import nd.max.ui.viewmodel.GameSpaceViewModel

private val TopBarHeight = 56.dp
private val MinSlabHeight = 132.dp
private val MaxSlabHeight = 288.dp
private val RailReserve = MaxSpace.xl + MaxSpace.md
private val HintReserve = 32.dp

/** عمود المعلومات والحلقة يقتسمان العرض؛ مركز الحلقة (لتوجيه الإضاءة) عند منتصف حصّتها. */
private const val INFO_WEIGHT = 0.34f
private const val DECK_WEIGHT = 0.66f
private const val DECK_CENTER_SHARE = INFO_WEIGHT + DECK_WEIGHT / 2f

/**
 * أقصى عرض للشريحة بنسبة عرض مساحة الحلقة؛ يبقي مكانًا للجارتين على الجانبين.
 *
 * رُفع من `0.36` إلى `0.40` مع رفع النسبة في [LOBBY_SLAB_ASPECT] إلى ١: طلب المالك بطاقات
 * **أقصر وأعرض قليلًا**، فالارتفاع اليوم = العرض (مربّع) بدل 1.25×العرض، والعرض زاد عُشرًا.
 * والجارتان ما زالتا تُريان (نصف المساحة الباقية لكل جانب) كما كانتا: لم يُلمس ترتيب الحلقة.
 */
private const val SLAB_WIDTH_SHARE = 0.40f

/** وميض الإطلاق: شدّة اللون، وتكبير الشاشة وتلاشيها أثناء الغوص. */
private const val LAUNCH_ALPHA = 0.60f
private const val DIVE_ZOOM = 0.05f
private const val DIVE_FADE = 0.35f

/** انتقال المسرح ⇄ مركز التحكم: انكماش وتلاشٍ وميل رأسيّ للصفحة التي تغادر أو تدخل. */
private const val ROOT_SHRINK = 0.07f
private const val ROOT_FADE = 0.85f
private const val ROOT_TILT_DEGREES = 9f
private const val ROOT_CAMERA = 20f
private const val ROOT_BACKDROP_DIM = 0.55f

/** دورة انجراف الجسيمات بطيئة عمدًا (ms)؛ تنفّس البطاقة أسرع. */
private const val DRIFT_MS = 40_000
private const val BREATHE_MS = 2800

private val metaCache = LruCache<String, GameLobbyMeta>(64)

/**
 * لوبي الألعاب ثلاثيّ الأبعاد. صفحتان رأسيّتان تُسحبان كأنهما واحدة:
 *
 *  - **المسرح:** عمود معلومات اللعبة + «ابدأ» + بلاطتا الملف واللوحة (بداية الاتّجاه)، وحلقة شرائح
 *    بعمق حقيقيّ (نهاية الاتّجاه) فوق خلفيّة من أيقونة اللعبة نفسها وجسيمات ضوء.
 *  - **مركز التحكّم:** سطح واحد يضمّ ملف اللعبة ومقبض اللوحة الجانبية وخيارات اللوبي وإدارة الألعاب؛
 *    ما كان أربع نوافذ منفصلة. حُذفت «المجموعات» (Combos) بالكامل بطلب المالك.
 *
 * الانتقال بين الصفحتين يتبع الإصبع لحظةً بلحظة (VerticalPager): المسرح ينكمش ويميل ويخفت بينما
 * يرتفع المركز ويستقيم، والتبويبات العلويّة تنزلق مع الموضع نفسه. من الريموت: ↑ أو R1 يفتح المركز،
 * L1 أو الرجوع يعيد المسرح.
 *
 * ### حدود لم تتغيّر
 *
 * - لا كتابة عتاد من هذه الشاشة (ADR-11): التشغيل بـ`GameLibraryAccess.launch`، والإعدادات
 *   بـ`AppSettingsViewModel.updateSetting`، ومفتاح اللوحة بـ`GamePanelPrefs` وخدمتها.
 * - لا أرقام مختلقة (ADR-07): كبسولة الحالة تعرض قراءات النظام وما لم يُقرأ «—»، والبطاقة تعرض حجم
 *   الحزمة وعمر التثبيت المقروءين فعلًا.
 * - خيارات اللوبي الجديدة تفضيلات عرض محلّية (`LobbyPrefs`) لا تلمس الجهاز.
 * - لا أصول منقولة: كل رسم بالكود، والصور أيقونات التطبيقات المثبَّتة.
 *
 * **غير مُتحقَّق على جهاز:** القفل العرضي وإخفاء الأشرطة وقصّ الكاميرا، وإشارة ميل الشرائح ورفع
 * الصفحة في RTL، وسلاسة ٦٠fps أثناء السحب، وعتبة الانتقال الرأسيّ.
 */
@Composable
fun GameLobbyScreen(
    navController: NavController,
    viewModel: GameSpaceViewModel = hiltViewModel(),
    settingsViewModel: AppSettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var lobbyPrefs by remember { mutableStateOf(LobbyPrefs.load(context)) }
    // دالّة تحوّل الخيارات الحاليّة إلى الجديدة وتحفظها؛ تقرأ الحالة لحظة النداء فلا تضيع تغييرات متتابعة.
    val updatePrefs: ((LobbyPrefsState) -> LobbyPrefsState) -> Unit = { change ->
        val next = change(lobbyPrefs)
        lobbyPrefs = next
        LobbyPrefs.save(context, next)
    }
    LobbyWindowEffect()
    LobbyKeepScreenOn(lobbyPrefs.keepScreenOn)

    val library by viewModel.library.collectAsStateWithLifecycle()
    var selected by rememberSaveable {
        mutableStateOf<String?>(lobbyPrefs.lastPackage.takeIf { lobbyPrefs.resumeLast })
    }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var section by rememberSaveable { mutableStateOf(LobbySection.Game) }
    var launchFailed by remember { mutableStateOf(false) }
    /** فشل تثبيت قائمة اللوحة الجانبية (المخزن أو ملفّ الرفيق الجذري) — يُعرض ولا يُخفى. */
    var panelArmFailed by remember { mutableStateOf(false) }
    var panelRevision by remember { mutableIntStateOf(0) }
    val panelPrefs = remember(panelRevision) { GamePanelPrefs.load(context) }

    val allGames = remember(library) { gameLibrary(library.apps, library.manual, library.excluded) }
    val installedDays by rememberInstalledDays(allGames, lobbyPrefs.sort == LobbySort.Newest)
    val games = remember(library, favoritesOnly, lobbyPrefs.sort, installedDays) {
        LobbyOrder.sort(
            games = allGames.filter { !favoritesOnly || it.packageName in library.favorites },
            favorites = library.favorites,
            order = lobbyPrefs.sort,
            installedDays = installedDays
        )
    }
    val packages = remember(games) { games.map { it.packageName } }
    val gamesState = rememberUpdatedState(games)
    val initialPage = remember { LobbyModel.targetPage(games.map { it.packageName }, selected) }
    val deckState = rememberPagerState(initialPage = initialPage) { gamesState.value.size }
    val rootState = rememberPagerState(initialPage = 0) { 2 }
    val restore = remember { LobbyRestoreGate() }

    // القائمة تغيّرت (تحميل · مرشِّح · ترتيب): أعد الحلقة إلى اللعبة المحدَّدة.
    LaunchedEffect(games) {
        if (games.isNotEmpty()) {
            val target = LobbyModel.targetPage(games.map { it.packageName }, selected)
            if (deckState.currentPage != target) deckState.scrollToPage(target)
            restore.games = games
        }
    }
    // استقرّت صفحة بفعل المستخدم: احفظ الحزمة. تُتجاهل الإصدارات الناتجة عن تغيّر القائمة نفسها،
    // وإلا كتبت فوق التحديد الصحيح مؤشّرًا قديمًا قبل أن تُعيد الدالة أعلاه الموضع.
    LaunchedEffect(deckState) {
        snapshotFlow { deckState.settledPage }.collect { page ->
            val current = gamesState.value
            if (restore.games === current) {
                current.getOrNull(page)?.let { game ->
                    selected = game.packageName
                    if (lobbyPrefs.resumeLast && lobbyPrefs.lastPackage != game.packageName) {
                        updatePrefs { it.copy(lastPackage = game.packageName) }
                    }
                }
            }
        }
    }
    // نقرة اهتزاز خفيفة كلما عبرت شريحة جديدة المركز بسحبة حقيقية (لا بإعادة الموضع البرمجية).
    LaunchedEffect(deckState) {
        snapshotFlow { deckState.currentPage }.drop(1).collect {
            if (restore.games === gamesState.value && deckState.isScrollInProgress && lobbyPrefs.haptics) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    val active = games.getOrNull(deckState.currentPage)
    val activeConfig: AppConfig? = active?.let { settingsViewModel.fullConfig[it.packageName] }
    val panelEnabled = active?.packageName in panelPrefs.enabledPackages

    // لون المسرح يتبع اللعبة المحدَّدة (أو لون العلامة إن أُوقف «ألوان من اللعبة») وينتقل بنعومة؛
    // يُقرأ في مرحلة الرسم عبر `toneState.value` حيث أمكن.
    val gameTone = rememberLobbyTone(active?.packageName)
    val toneState = animateColorAsState(
        targetValue = if (lobbyPrefs.gameColors) gameTone else LobbyPalette.Red,
        animationSpec = tween(MaxDuration.deliberate),
        label = "lobbyStageTone"
    )
    // وميض الإطلاق: يتمدّد من أسفل الشاشة بلون اللعبة، ثم يبدأ التشغيل. `launching` يمنع نقرتين متتاليتين.
    val burst = remember { Animatable(0f) }
    var launching by remember { mutableStateOf(false) }

    val setPanel: (Boolean) -> Unit = { enabled ->
        active?.packageName?.let { pkg ->
            if (GamePanelPrefs.setEnabled(context, pkg, enabled)) {
                panelRevision += 1
                GamePanelService.reconcile(context)
                scope.launch(Dispatchers.IO) {
                    // والنتيجة **تُقرأ** ولا تُرمى: الملفّ الذي يقرؤه الرفيق الجذري هو شرط تشغيل اللوحة
                    // عند فتح اللعبة من مشغّل خارجي والتطبيق مقتول، وفشل كتابته كان يمرّ صامتًا فيرى
                    // المستخدم المفتاح «مُفعَّلًا» واللوحة لا تظهر أبدًا (حزمة المستخدم ٢٠٢٦-١٠-١٥:
                    // `side_panel_armed.txt` غائب، ولا سطر واحد في السجل كلّه يشرح ذلك).
                    val sync = SidePanelArmedStore.sync(GamePanelPrefs.load(context).enabledPackages)
                    if (!sync.ok) {
                        AppMonitorLogger.w("EVENT=SIDE_PANEL_ARM_SYNC_FAILED pkg=$pkg ${sync.logDetail()}")
                    }
                    withContext(Dispatchers.Main) { panelArmFailed = !sync.ok }
                }
            } else {
                AppMonitorLogger.w("EVENT=SIDE_PANEL_ARM_SYNC_FAILED pkg=$pkg reason=prefs-write-refused")
                panelArmFailed = true
            }
        }
    }

    val animations = rememberLobbyAnimationsEnabled()
    val ambient = animations && lobbyPrefs.ambient
    val intro = rememberLobbyIntro(animations)
    val breathe = lobbyLoop(ambient, BREATHE_MS, RepeatMode.Reverse, FastOutSlowInEasing, rest = 0.5f)
    val drift = lobbyLoop(ambient, DRIFT_MS, RepeatMode.Restart, LinearEasing, rest = 0f)

    val tick = {
        if (lobbyPrefs.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    val openControl: (LobbySection) -> Unit = { target ->
        section = target
        scope.launch {
            rootState.animateScrollToPage(1, animationSpec = tween(MaxDuration.deliberate, easing = FastOutSlowInEasing))
        }
    }
    val closeControl: () -> Unit = {
        scope.launch {
            rootState.animateScrollToPage(0, animationSpec = tween(MaxDuration.deliberate, easing = FastOutSlowInEasing))
        }
    }
    BackHandler(enabled = rootState.currentPage == 1) { closeControl() }

    // وميض الإطلاق ثم التشغيل؛ `launching` يمنع نقرتين، وتُستدعى من الزرّ ومن الريموت/الكيبورد.
    val start: () -> Unit = start@{
        val pkg = active?.packageName ?: return@start
        if (launching) return@start
        launching = true
        if (lobbyPrefs.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            if (animations && lobbyPrefs.launchFlash) {
                burst.animateTo(1f, tween(MaxDuration.standard, easing = FastOutSlowInEasing))
            }
            launchFailed = !GameLibraryAccess.launch(context, pkg)
            launching = false
            burst.animateTo(0f, tween(MaxDuration.deliberate))
        }
    }
    // تحكم بالريموت/الكيبورد (أجهزة الألعاب): ←/→ بين الألعاب (معكوسة في RTL)، ↑ أو R1 مركز التحكم،
    // L1 أو الرجوع للمسرح، Enter/A تشغيل، X ملف اللعبة. الأزرار المركَّزة تستهلك Enter بنفسها قبل هنا.
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val step: (Int) -> Unit = { delta ->
        val target = (deckState.currentPage + delta).coerceIn(0, (gamesState.value.size - 1).coerceAtLeast(0))
        if (target != deckState.currentPage) scope.launch { deckState.animateScrollToPage(target) }
    }
    val rootFling = PagerDefaults.flingBehavior(
        state = rootState,
        snapAnimationSpec = LobbySnapSpring
    )
    val depthStrength = lobbyPrefs.depth.strength
    val focusX = if (rtl) 1f - DECK_CENTER_SHARE else DECK_CENTER_SHARE
    val manageApps = remember(library) {
        library.apps.filter { ForegroundAppResolver.isPackageName(it.packageName) }
            .distinctBy { it.packageName }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else if (rootState.currentPage != 0) {
                    when (event.key) {
                        Key.ButtonL1 -> { closeControl(); true }
                        else -> false
                    }
                } else {
                    when (event.key) {
                        Key.DirectionLeft -> { step(if (rtl) 1 else -1); true }
                        Key.DirectionRight -> { step(if (rtl) -1 else 1); true }
                        Key.DirectionUp -> { openControl(LobbySection.Game); true }
                        Key.ButtonR1 -> { openControl(section); true }
                        Key.Enter, Key.NumPadEnter, Key.ButtonA -> { start(); true }
                        Key.ButtonX -> { if (active != null) openControl(LobbySection.Game); true }
                        else -> false
                    }
                }
            }
            .focusRequester(focus)
            .focusable()
    ) {
        LobbyStageBackdrop3D(
            tone = { toneState.value },
            position = { deckState.currentPage + deckState.currentPageOffsetFraction },
            packages = packages,
            drift = drift,
            motes = ambient,
            focusX = focusX,
            modifier = Modifier.graphicsLayer {
                val progress = (rootState.currentPage + rootState.currentPageOffsetFraction).coerceIn(0f, 1f)
                alpha = 1f - ROOT_BACKDROP_DIM * progress
            }
        )

        VerticalPager(
            state = rootState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = burst.value
                    alpha = 1f - DIVE_FADE * p
                    scaleX = 1f + DIVE_ZOOM * p
                    scaleY = scaleX
                },
            beyondViewportPageCount = 1,
            flingBehavior = rootFling,
            key = { it }
        ) { page ->
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val slot = page - (rootState.currentPage + rootState.currentPageOffsetFraction)
                        val a = abs(slot).coerceIn(0f, 1f)
                        val shrink = 1f - ROOT_SHRINK * a
                        scaleX = shrink
                        scaleY = shrink
                        alpha = 1f - ROOT_FADE * a
                        rotationX = ROOT_TILT_DEGREES * depthStrength * a
                        cameraDistance = ROOT_CAMERA * density
                    }
            ) {
                if (page == 0) {
                    // ─────────────── المسرح ───────────────
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .displayCutoutPadding()
                            .padding(top = TopBarHeight),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(INFO_WEIGHT)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (active != null) {
                                val tweaks = activeConfig?.customizedFieldCount() ?: 0
                                val favorite = active.packageName in library.favorites
                                LobbyInfoPanel(
                                    title = active.label,
                                    meta = rememberLobbyMetaText(active.packageName),
                                    favorite = favorite,
                                    onFavorite = {
                                        tick()
                                        viewModel.favorite(active.packageName, !favorite)
                                    },
                                    profileValue = if (tweaks > 0) {
                                        stringResource(R.string.lobby_tile_profile_value, tweaks)
                                    } else {
                                        stringResource(R.string.lobby_tile_profile_none)
                                    },
                                    onProfile = { openControl(LobbySection.Game) },
                                    panelValue = stringResource(
                                        if (panelEnabled) R.string.lobby_state_on else R.string.lobby_state_off
                                    ),
                                    panelOn = panelEnabled,
                                    onPanel = {
                                        tick()
                                        setPanel(!panelEnabled)
                                    },
                                    startLabel = stringResource(R.string.lobby_start),
                                    onStart = start,
                                    tone = { toneState.value },
                                    breathe = breathe,
                                    modifier = Modifier
                                        .padding(start = MaxSpace.xxl, end = MaxSpace.xl)
                                        .lobbyIntro(intro, fromBelow = 24.dp, start = 0.1f)
                                )
                            }
                        }
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(DECK_WEIGHT)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            val fitHeight = (maxHeight - LobbyDeckPad * 2 - RailReserve - HintReserve)
                                .coerceIn(MinSlabHeight, MaxSlabHeight)
                            val slabWidth = minOf(fitHeight * LOBBY_SLAB_ASPECT, maxWidth * SLAB_WIDTH_SHARE)
                            val slabHeight = slabWidth / LOBBY_SLAB_ASPECT
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                when {
                                    games.isNotEmpty() -> {
                                        LobbyDeck(
                                            state = deckState,
                                            depth = lobbyPrefs.depth,
                                            slabWidth = slabWidth,
                                            slabHeight = slabHeight,
                                            breathe = breathe,
                                            onSelect = { target ->
                                                scope.launch { deckState.animateScrollToPage(target) }
                                            },
                                            modifier = Modifier.lobbyIntro(
                                                intro,
                                                fromBelow = 24.dp,
                                                start = 0.1f,
                                                scaleFrom = 0.94f
                                            ),
                                            key = { index -> games.getOrNull(index)?.packageName ?: index }
                                        ) { index, slot ->
                                            val app = games.getOrNull(index)
                                            if (app != null) {
                                                LobbyDeckCard(
                                                    packageName = app.packageName,
                                                    title = app.label,
                                                    description = stringResource(
                                                        R.string.lobby_card_position,
                                                        app.label,
                                                        index + 1,
                                                        games.size
                                                    ),
                                                    gameColors = lobbyPrefs.gameColors,
                                                    slot = slot
                                                )
                                            }
                                        }
                                        // مسطرة الموضع: «أين أنا من القائمة» بنظرة؛ لا تُرسم لأقل من لعبتين.
                                        LobbyPositionRail(
                                            count = games.size,
                                            position = { deckState.currentPage + deckState.currentPageOffsetFraction },
                                            tone = { toneState.value },
                                            modifier = Modifier
                                                .padding(top = MaxSpace.xs)
                                                .lobbyIntro(intro, fromBelow = MaxSpace.lg, start = 0.2f)
                                        )
                                        LobbyPullHint(
                                            text = stringResource(R.string.lobby_pull_hint),
                                            bob = breathe,
                                            modifier = Modifier.padding(top = MaxSpace.sm)
                                        )
                                    }
                                    library.loading -> LobbyDeckSkeleton(
                                        slabWidth = slabWidth,
                                        slabHeight = slabHeight,
                                        pulse = breathe,
                                        description = stringResource(R.string.lobby_loading)
                                    )
                                    else -> Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(MaxSpace.lg)
                                    ) {
                                        LobbyNotice(
                                            stringResource(
                                                if (favoritesOnly) R.string.lobby_empty_favorites else R.string.lobby_empty
                                            )
                                        )
                                        if (!favoritesOnly) {
                                            OutlinedButton(onClick = { openControl(LobbySection.Library) }) {
                                                Text(
                                                    text = stringResource(R.string.lobby_manage_games),
                                                    color = LobbyPalette.Ink
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ─────────────── مركز التحكم ───────────────
                    LobbyControlCenter(
                        section = section,
                        onSection = { section = it },
                        app = active,
                        config = activeConfig ?: AppConfig(),
                        panelEnabled = panelEnabled,
                        onPanelEnabled = setPanel,
                        onUpdate = { key, value ->
                            active?.let { settingsViewModel.updateSetting(it.packageName, key, value) }
                        },
                        onOpenFull = { active?.let { navController.openAppSettings(it.packageName) } },
                        prefs = lobbyPrefs,
                        onPrefs = updatePrefs,
                        apps = manageApps,
                        manual = library.manual,
                        excluded = library.excluded,
                        onToggleGame = viewModel::membership,
                        accent = toneState.value,
                        modifier = Modifier
                            .displayCutoutPadding()
                            .padding(top = TopBarHeight)
                    )
                }
            }
        }

        // الشريط العلويّ ثابت فوق الصفحتين: تبويبات تنزلق مع السحب + كبسولة الحالة + أزرار.
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .displayCutoutPadding()
                .height(TopBarHeight)
                .padding(horizontal = MaxSpace.xxl)
                .lobbyIntro(intro, fromBelow = (-24).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
        ) {
            LobbyTopTabs(
                labels = listOf(
                    stringResource(R.string.lobby_tab_games),
                    stringResource(R.string.lobby_tab_control)
                ),
                selected = rootState.currentPage,
                position = { rootState.currentPage + rootState.currentPageOffsetFraction },
                tone = { toneState.value },
                description = stringResource(R.string.lobby_tab_lobby),
                onSelect = { index -> scope.launch { rootState.animateScrollToPage(index) } }
            )
            Spacer(Modifier.weight(1f))
            if (lobbyPrefs.showStatus) LobbyStatusCapsule()
            LobbyRoundButton(
                icon = Icons.Rounded.Star,
                description = stringResource(R.string.lobby_favorites_filter),
                onClick = {
                    tick()
                    favoritesOnly = !favoritesOnly
                },
                selected = favoritesOnly,
                tone = toneState.value
            )
            LobbyRoundButton(
                icon = Icons.Rounded.Refresh,
                description = stringResource(R.string.gaming_refresh),
                onClick = { viewModel.refresh() }
            )
            LobbyRoundButton(
                icon = Icons.Rounded.Close,
                description = stringResource(R.string.lobby_close),
                onClick = { navController.navigateUp() }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .displayCutoutPadding()
                .padding(top = TopBarHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            if (library.failed) LobbyNotice(stringResource(R.string.gaming_config_failed), caution = true)
            if (launchFailed) LobbyNotice(stringResource(R.string.game_space_launch_failed), caution = true)
            if (panelArmFailed) LobbyNotice(stringResource(R.string.lobby_panel_arm_failed), caution = true)
        }

        // طبقة الإطلاق: رسم فقط (لا مدخلات)، تُقرأ فيها `burst` في مرحلة الرسم فلا إعادة تركيب أثناء الحركة.
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val p = burst.value
                    if (p > 0f) {
                        val tone = toneState.value
                        drawRect(
                            brush = Brush.radialGradient(
                                0f to tone.copy(alpha = LAUNCH_ALPHA * (1f - p * 0.4f)),
                                0.55f to tone.copy(alpha = LAUNCH_ALPHA * 0.35f),
                                1f to Color.Transparent,
                                center = Offset(size.width * 0.5f, size.height * 0.88f),
                                radius = size.maxDimension * (0.2f + p * 1.1f)
                            ),
                            alpha = p.coerceIn(0f, 1f)
                        )
                    }
                }
        )
    }
}

/** يحمل آخر قائمة أُعيد إليها موضع الحلقة؛ مرجعٌ لا حالة، فلا يسبّب إعادة تركيب. */
private class LobbyRestoreGate {
    var games: List<GameApp>? = null
}

/**
 * سطر بيانات اللعبة: حجم الحزمة وعمر التثبيت في سطر واحد تفصل بينهما نقطة وسطى (يُقتطع بنقاط إن ضاق
 * العرض). أرقام مقروءة فعلًا من النظام (ADR-07)، وما لم يُقرأ لا يُكتب. يقرأ على IO مرّة ويحفظ في [metaCache].
 */
@Composable
private fun rememberLobbyMetaText(packageName: String): String {
    val context = LocalContext.current
    val meta by produceState<GameLobbyMeta?>(metaCache.get(packageName), packageName) {
        if (value == null) {
            val read = withContext(Dispatchers.IO) { GameLobbyMetaReader.read(context, packageName) }
            metaCache.put(packageName, read)
            value = read
        }
    }
    val today = stringResource(R.string.lobby_meta_age_today)
    val ageText = meta?.installedDays?.let { days ->
        if (days < 1) today else stringResource(R.string.lobby_meta_age_days, days)
    }
    val sizeText = meta?.sizeBytes?.let { Formatter.formatShortFileSize(context, it) }
    return listOfNotNull(sizeText, ageText).joinToString(" · ")
}

/**
 * عمر تثبيت كل لعبة بالأيام (لترتيب «الأحدث»). يُقرأ على IO مرّة لكل حزمة من نفس [metaCache]، ولا يُقرأ
 * شيء ما لم يكن الترتيب «الأحدث». ما لم يُعرف عمره لا يدخل الخريطة فيُرتَّب في الآخر.
 */
@Composable
private fun rememberInstalledDays(games: List<GameApp>, enabled: Boolean): State<Map<String, Int>> {
    val context = LocalContext.current
    return produceState<Map<String, Int>>(emptyMap(), games, enabled) {
        value = if (!enabled) {
            emptyMap()
        } else {
            withContext(Dispatchers.IO) {
                buildMap<String, Int> {
                    for (app in games) {
                        val meta = metaCache.get(app.packageName)
                            ?: GameLobbyMetaReader.read(context, app.packageName)
                                .also { metaCache.put(app.packageName, it) }
                        meta.installedDays?.let { put(app.packageName, it) }
                    }
                }
            }
        }
    }
}
