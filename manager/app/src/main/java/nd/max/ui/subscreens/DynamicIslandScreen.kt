/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned, but it hosts engine pieces ported from Islandify (MIT License,
 * Copyright (c) 2026 bughunter11) — see THIRD_PARTY_NOTICES.md.
 *
 * شاشة الجزيرة الديناميكية الموحّدة — دمج «استوديو الجزيرة» في شاشة واحدة بهوية MaxManager
 * ---------------------------------------------------------------------------------------------
 * كان للجزيرة نشاطٌ مستقلّ بخمس تبويبات (الرئيسية · النوافذ · التخصيص · التطبيقات · حول)
 * وعنوانه في هيكل التطبيق زرٌّ يفتحه. وقد أُمر بدمجها في شاشة واحدة «بهوية MaxManager»
 * بلا «حول» ولا اختيار لغة داخلها. فما يلي هو الدمج:
 *
 *   • كل ما كان يُضبط هناك يُضبط هنا — بالترتيب نفسه تقريبًا: المعاينة الحيّة، ثم الحالة
 *     والصلاحيات، ثم الاختبار، فالأنشطة والتطبيقات، فالموضع والشكل والحركة والسلوك
 *     والإيماءات واللون والنوافذ، وأخيرًا إعادة الضبط.
 *   • والرسم بهوية التطبيق: `MaxScreen` و`MaxSection`/`MaxGroup` والصفوف المشتركة
 *     (`MaxSwitchRow`/`MaxSliderRow`/`MaxNavigationRow`) والورقة `CustomBottomSheet`
 *     والحوار `MaxConfirmDialog` — لا أنماطًا جديدة.
 *   • وما بقي خاصًّا بالجزيرة (كبسولتها، عيّناتها، أيقونات أنواعها) نُقل إلى
 *     `nd.max.ui.component` (`IslandUiKit`/`IslandLiveCard`/`IslandKindSheet`/`IslandAppsSheet`)
 *     لأن الحزمة القديمة `island/ui` أُلغيت مع النشاط — وهذه هي العلّة التي جعلت الملف
 *     الأخير الذي يعبُر حدود الحذف هو هذه الشاشة وحدها.
 *
 * **وموضع المعاينة الحيّة هو المقصود لا المصادفة:** أول ما تراه العين هو الكبسولة نفسها
 * مرسومةً بالمحرّك الحقيقي (لا برسم يدويّ)، وقدامها رقائق تبديل العيّنة — تمامًا كما كانت
 * شاشة الاستوديو تفتح، لكن بإطار MaxManager.
 *
 * **والترحيل من الجزيرة القديمة مرة واحدة:** المحرّك السابق (`nd.max.service.IslandService`)
 * كان يُشغَّل من مسار قديم مستقلّ عن الاستوديو. فحين تُفتح هذه الشاشة أول مرّة: تُعلَّم
 * الجزيرة الجديدة بأنها «مُهيّأة» (فلا شاشة تعارف داخلية — بقرار المالك)، وإن وُجد المحرّك
 * القديم يعمل أو مفعّلًا في إعداداته يُوقف وتُنقل النيّة إلى المحرّك الجديد
 * (`nd.max.island.engine.IslandService` عبر `IslandControl`) — ثم تعمل الآلة الواحدة.
 */
package nd.max.ui.subscreens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeliveryDining
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import kotlin.math.roundToInt
import nd.max.R
import nd.max.island.core.IslandControl
import nd.max.island.core.IslandKind
import nd.max.island.core.IslandSettings
import nd.max.island.core.KindStyle
import nd.max.island.core.Perms
import nd.max.island.core.SwipeAction
import nd.max.island.core.SwipeDir
import nd.max.island.core.Trigger
import nd.max.island.engine.IslandController
import nd.max.island.engine.IslandMode
import nd.max.island.engine.IslandService as StudioIsland
import nd.max.island.engine.LiveKind
import nd.max.island.engine.ToggleKind
import nd.max.service.IslandService
import nd.max.ui.component.AccentSwatches
import nd.max.ui.component.BouncyChip
import nd.max.ui.component.CustomBottomSheet
import nd.max.ui.component.ISLAND_ACCENTS
import nd.max.ui.component.ISLAND_KIND_GROUPS
import nd.max.ui.component.IslandAppsSheet
import nd.max.ui.component.IslandKindSheet
import nd.max.ui.component.IslandLiveCard
import nd.max.ui.component.ScreenAccentProvider
import nd.max.ui.component.accentHexOf
import nd.max.ui.component.hint
import nd.max.ui.component.icon
import nd.max.ui.component.kindStyleSummary
import nd.max.ui.component.parseAccentHex
import nd.max.ui.design.MaxCondition
import nd.max.ui.design.MaxConditionKind
import nd.max.ui.design.MaxConfirmDialog
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxNavigationRow
import nd.max.ui.design.MaxScreen
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSegmented
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxSliderRow
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxSwitchRow
import nd.max.ui.design.MaxTone
import nd.max.ui.navigation.RegisterSwipeNavigation
import nd.max.ui.util.IslandPrefs

@Composable
fun DynamicIslandScreen(navController: NavController) {
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    /* ---------- حالة المحرّك والإعدادات الحيّة ---------- */
    val running by StudioIsland.active.collectAsState(initial = StudioIsland.isActive())
    val enabled by IslandSettings.enabled.collectAsState()
    val scale by IslandSettings.scale.collectAsState()
    val w by IslandSettings.widthScale.collectAsState()
    val h by IslandSettings.heightScale.collectAsState()
    val x by IslandSettings.offsetX.collectAsState()
    val y by IslandSettings.offsetY.collectAsState()
    val corner by IslandSettings.corner.collectAsState()
    val speed by IslandSettings.animSpeed.collectAsState()
    val glow by IslandSettings.glow.collectAsState()
    val haptics by IslandSettings.haptics.collectAsState()
    val source by IslandSettings.accentSource.collectAsState()
    val seed by IslandSettings.accentSeed.collectAsState()
    val appAccentOn by IslandSettings.islandAppAccent.collectAsState()
    val collapse by IslandSettings.autoCollapseSec.collectAsState()
    val stickyNotif by IslandSettings.stickyNotif.collectAsState()
    val idlePill by IslandSettings.idlePill.collectAsState()
    val swUp by IslandSettings.swipeUp.collectAsState()
    val swDown by IslandSettings.swipeDown.collectAsState()
    val swLeft by IslandSettings.swipeLeft.collectAsState()
    val swRight by IslandSettings.swipeRight.collectAsState()
    val triggers by IslandSettings.triggers.collectAsState()
    val styles by IslandSettings.kindStyles.collectAsState()
    val blocked by IslandSettings.blockedApps.collectAsState()
    val dynamicOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /* ---------- الصلاحيات: تُعاد قراءتها عند كل عودة إلى الشاشة ----------
     * العدّاد `permTick` هو الزناد: مراقب دورة الحياة يُحرّكه عند ON_RESUME، ومُنشِّئات
     * طلب الصلاحيات تُحرّكه عند النتيجة. ولا تُحسب الصلاحيات داخل التركيب مباشرة لأن
     * استدعاءاتها تقرأ حالة النظام — قراءة واحدة لكل «نبضة» تكفي وتُخزَّن بـ`remember`. */
    var permTick by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableStateOf(IslandTab.CORE) }
    // **تبويبات الجزيرة تتشارك حاوية تمرير واحدة:** كانت `MaxScreen` تحتفظ بموضع التمرير عند
    // تبديل التبويب، فينتقل المستخدم من آخر «الأساس» إلى «التصميم» فيجده في منتصفه أو في آخره
    // لا في أوّله (لقطتا المالك: الأساس متمرّرة، ثم التصميم عند نهايته). كل تبديل يبدأ القسم من
    // أعلاه — أي من أوّل صفّ في التبويب، لا من بطاقة المعاينة المشتركة فوقه.
    val pageScroll = rememberScrollState()
    var tabTopPx by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedTab) {
        if (tabTopPx > 0) pageScroll.scrollTo(tabTopPx) else pageScroll.scrollTo(0)
    }
    // السحب الأفقي يقلب تبويبات الجزيرة، وعند أوّلها يعود إلى الشاشة السابقة:
    // الإرجاع `false` عند الحدّ يُسلّم القرار إلى جذر التطبيق.
    RegisterSwipeNavigation(
        onNext = {
            val next = IslandTab.entries.getOrNull(selectedTab.ordinal + 1)
            if (next != null) {
                selectedTab = next
                true
            } else {
                false
            }
        },
        onPrevious = {
            val previous = IslandTab.entries.getOrNull(selectedTab.ordinal - 1)
            if (previous != null) {
                selectedTab = previous
                true
            } else {
                false
            }
        },
    )
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) permTick++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val a11yOk = remember(permTick) { Perms.accessibility(ctx) }
    val overlayOk = remember(permTick) { Perms.overlay(ctx) }
    val listenerOk = remember(permTick) { Perms.listener(ctx) }
    val postOk = remember(permTick) { Perms.post(ctx) }
    val btOk = remember(permTick) { Perms.bluetooth(ctx) }
    val batteryOk = remember(permTick) { Perms.battery(ctx) }

    val postLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permTick++
    }
    val btLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permTick++
    }

    /* ---------- أوراق الشاشة ---------- */
    var appsOpen by remember { mutableStateOf(false) }
    var kindSheetKind by remember { mutableStateOf<IslandKind?>(null) }
    var kindSheetAll by remember { mutableStateOf(false) }
    var swipeDir by remember { mutableStateOf<SwipeDir?>(null) }
    var resetConfirm by remember { mutableStateOf(false) }

    /* ---------- التشغيل/الإيقاف ---------- */
    fun setEnabled(on: Boolean) {
        if (on) {
            // مساران لا يعملان معًا: قبل تشغيل المحرّك الجديد يُوقف القديم وتُنزع «نيّته»
            val legacy = IslandPrefs.load(ctx)
            if (IslandService.isRunning) IslandService.stop(ctx)
            if (legacy.enabled) IslandPrefs.save(ctx, legacy.copy(enabled = false))
            IslandControl.start(ctx)
        } else {
            IslandControl.stop(ctx)
        }
        permTick++
    }

    fun nudge(dx: Float, dy: Float) {
        IslandSettings.offsetX.value = (IslandSettings.offsetX.value + dx).coerceIn(-180f, 180f)
        IslandSettings.offsetY.value = (IslandSettings.offsetY.value + dy).coerceIn(0f, 300f)
        IslandSettings.save()
    }

    /* ---------- الترحيل من الجزيرة القديمة (مرة واحدة عند أول فتح) ----------
     * المحرّك السابق كان له مسار تشغيل مستقلّ تمامًا عن الاستوديو، فمن كان على الجزيرة
     * القديمة لا يُترك بمحرّكين يعملان ولا بمحرّك لا يعمل: تُنقل النيّة للمحرّك الجديد. */
    LaunchedEffect(Unit) {
        IslandSettings.onboarded.value = true
        IslandSettings.save()
        val legacy = IslandPrefs.load(ctx)
        if (IslandService.isRunning || legacy.enabled) {
            if (IslandService.isRunning) IslandService.stop(ctx)
            IslandPrefs.save(ctx, legacy.copy(enabled = false))
            IslandSettings.enabled.value = true
            IslandSettings.save()
        }
    }

    // التشغيل التلقائيّ: من ترك الجزيرة مفعّلة (أو رُحّلت نيّته من القديمة) تعود وحدها
    // حين تكتمل شروطها — بلا أن يمرّ بإعدادات من جديد بعد كل إغلاق قسريّ.
    LaunchedEffect(running, permTick, enabled) {
        if (!running && enabled && (a11yOk || overlayOk) && listenerOk && postOk) {
            IslandControl.start(ctx)
        }
    }

    ScreenAccentProvider(scheme.tertiary) {
        Box(Modifier.fillMaxSize()) {
            MaxScreen(
                title = stringResource(R.string.dynamic_island_title),
                onBack = { navController.popBackStack() },
                subtitle = stringResource(R.string.dynamic_island_intro),
                accentIcon = Icons.Rounded.PictureInPictureAlt,
                accent = scheme.tertiary,
                banner = if (a11yOk || overlayOk) {
                    null
                } else {
                    // لا خدمة وصولية ولا رسم فوق التطبيقات: لا شيء يمكن أن يظهر أصلًا —
                    // فهذا تنبيه حاجز بشريحة الرسم فوق التطبيقات (أسرع مسار للصلاحيات).
                    MaxCondition(
                        kind = MaxConditionKind.PermissionRequired,
                        title = stringResource(R.string.max_fps_permission_title),
                        detail = stringResource(R.string.max_fps_permission_detail),
                        primaryActionLabel = stringResource(R.string.max_fps_permission_action),
                        onPrimaryAction = { Perms.openOverlaySettings(ctx) }
                    )
                },
                // التبويبات الثلاثة تشترك في هذه الحاوية؛ الشاشة تعيدها إلى الأعلى عند التبديل.
                scrollState = pageScroll
            ) {
                /* ── ٠. المعاينة الحيّة — الكبسولة نفسها بالمحرّك الحقيقي ────────── */
                IslandLiveCard()

                // نصوص العرض التجريبي تُقرأ هنا لا داخل التبويب: `stringResource` يُنادى في التركيب،
                // والتبويبات تستقبلها وسائط فلا تقرأ حالة عامة.
                val demoMsg = stringResource(R.string.ify_demo_msg)
                val demoMsg2 = stringResource(R.string.ify_demo_msg2)
                val demoMsg3 = stringResource(R.string.ify_demo_msg3)
                val demoNav = stringResource(R.string.ify_demo_nav_instruction)
                val demoEta = stringResource(R.string.ify_demo_nav_eta)
                val demoOrder = stringResource(R.string.ify_demo_order_status)
                val demoArrive = stringResource(R.string.ify_demo_order_eta)
                val demoDownload = stringResource(R.string.ify_demo_download)
                /* ── محتوى التبويبات ── */
                AnimatedContent(
                    targetState = selectedTab,
                    // موضع أوّل صفّ في التبويب داخل حاوية التمرير (بالبكسل) — يبدأ التمرير منه.
                    modifier = Modifier.onGloballyPositioned {
                        tabTopPx = it.positionInParent().y.roundToInt()
                    },
                    transitionSpec = {
                        fadeIn(animationSpec = tween(MaxDuration.standard, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(
                                animationSpec = tween(MaxDuration.standard, easing = FastOutSlowInEasing)
                            ) togetherWith
                            fadeOut(animationSpec = tween(MaxDuration.quick, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(
                                animationSpec = tween(MaxDuration.standard, easing = FastOutSlowInEasing)
                            )
                    },
                    label = "islandTabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        IslandTab.CORE -> CoreTabContent(
                            running = running,
                            setEnabled = ::setEnabled,
                            a11yOk = a11yOk,
                            listenerOk = listenerOk,
                            postOk = postOk,
                            btOk = btOk,
                            batteryOk = batteryOk,
                            ctx = ctx,
                            postLauncher = postLauncher,
                            btLauncher = btLauncher,
                            triggers = triggers,
                            blocked = blocked,
                            appsOpen = appsOpen,
                            appsOpenSetter = { appsOpen = it },
                            demoMsg = demoMsg,
                            demoMsg2 = demoMsg2,
                            demoMsg3 = demoMsg3,
                            demoNav = demoNav,
                            demoEta = demoEta,
                            demoOrder = demoOrder,
                            demoArrive = demoArrive,
                            demoDownload = demoDownload,
                            scheme = scheme
                        )
                        IslandTab.DESIGN -> DesignTabContent(
                            x = x,
                            y = y,
                            scale = scale,
                            w = w,
                            h = h,
                            corner = corner,
                            speed = speed,
                            glow = glow,
                            haptics = haptics,
                            collapse = collapse,
                            idlePill = idlePill,
                            stickyNotif = stickyNotif,
                            appAccentOn = appAccentOn,
                            dynamicOk = dynamicOk,
                            source = source,
                            seed = seed,
                            ctx = ctx,
                            nudge = ::nudge,
                            scheme = scheme
                        )
                        IslandTab.ADVANCED -> AdvancedTabContent(
                            swUp = swUp,
                            swDown = swDown,
                            swLeft = swLeft,
                            swRight = swRight,
                            swipeDir = swipeDir,
                            swipeDirSetter = { swipeDir = it },
                            styles = styles,
                            kindSheetKind = kindSheetKind,
                            kindSheetKindSetter = { kindSheetKind = it },
                            kindSheetAll = kindSheetAll,
                            kindSheetAllSetter = { kindSheetAll = it },
                            resetConfirm = resetConfirm,
                            resetConfirmSetter = { resetConfirm = it },
                            scheme = scheme
                        )
                    }
                }
                /* **ولا تُكرَّر بطاقات الصلاحيات هنا (٢٠٢٦-١٠-٢١):** القسم الأوّل في تبويب
                 * «الأساس» (`CoreTabContent`) يعرض بطاقات اللمسات والإشعارات ونشر الإشعارات
                 * نفسها؛ وكانت تُعاد هنا بعد محتوى التبويب، فيراها المستخدم مرّتين — واحدة فوق
                 * وسط الشاشة وأخرى في آخرها (بلقطتين أرسلهما المالك). موضعها واحد: حيث تُقرأ
                 * الحالة التي تنقصها، لا أسفل كل تبويب. */

                /* ختامُ المحتوى: حجزٌ لشريط التبويبات العائم فلا يقف آخر صفٍّ تحته */
                Spacer(Modifier.height(64.dp))
            }

        /* ---------- الأوراق فوق الشاشة في الجذر نفسه ----------
         * لا تُدرج داخل محتوى `MaxScreen` (عمود يتمرّر): ورقة تغطّي الشاشة يجب أن
         * تُرسم فوقها لا أن تشاركها تمريرها — فتُركَّب شقيقةً لها في `Box`. */
            IslandAppsSheet(visible = appsOpen, onClose = { appsOpen = false })
            IslandKindSheet(
                kind = kindSheetKind,
                all = kindSheetAll,
                onClose = {
                    kindSheetKind = null
                    kindSheetAll = false
                }
            )
            SwipeActionSheet(
                dir = swipeDir,
                current = when (swipeDir) {
                    SwipeDir.Up -> swUp
                    SwipeDir.Down -> swDown
                    SwipeDir.Left -> swLeft
                    SwipeDir.Right -> swRight
                    null -> SwipeAction.None
                },
                onSet = { a ->
                    when (swipeDir) {
                        SwipeDir.Up -> IslandSettings.swipeUp.value = a
                        SwipeDir.Down -> IslandSettings.swipeDown.value = a
                        SwipeDir.Left -> IslandSettings.swipeLeft.value = a
                        SwipeDir.Right -> IslandSettings.swipeRight.value = a
                        null -> Unit
                    }
                    IslandSettings.save()
                },
                onClose = { swipeDir = null }
            )
            IslandTabBar(
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = MaxSpace.gutter, vertical = MaxSpace.sm),
            )
            MaxConfirmDialog(
                visible = resetConfirm,
                title = stringResource(R.string.ify_cust_reset),
                message = stringResource(R.string.ify_cust_reset_sub),
                confirmLabel = stringResource(R.string.ify_cust_reset),
                onConfirm = {
                    IslandSettings.resetAll()
                    resetConfirm = false
                },
                onDismiss = { resetConfirm = false },
                icon = Icons.Rounded.RestartAlt,
                destructive = true
            )
        }
    }
}