/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root.
 *
 * محتوى تبويبات شاشة الجزيرة الديناميكية: الأساس — التصميم — المتقدم.
 *
 * فُصل من `DynamicIslandScreen.kt` عند تقسيمه: الشاشة تحتفظ بالهيكل والحالة، وهذا الملف
 * يحتفظ بالأقسام وحدها — وكل تبويب يستقبل قيمه وسائط لا يقرؤها من حالة عامة.
 */
package nd.max.ui.subscreens

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.ColorScheme
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
import nd.max.ui.design.MaxConditionNotice
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
import nd.max.ui.util.IslandPrefs
/** إجراء اختبار واحد: عنوانه وأيقونته وفعله. */
private class TestAction(
    @StringRes val label: Int,
    val icon: ImageVector,
    val run: () -> Unit
)

/**
 * مجموعة اختبار: عنوان صغير ثم صفوف من زرّين متساويين.
 *
 * **ولماذا الصفوف لا تراصّ الرقائق بحرّيتها:** كان الاستوديو يترك كل رقيقة تأخذ عرضها، فتبرز
 * صفوف غير منظّمة تختلف باختلاف طول النصّ — وبعد الدمج تُكتب عناوين جديدة بلغات قد تطول.
 * فالصفّان المتساويان يجعلان الشكل ثابتًا مهما طال النصّ، والفراغ الأخير يُكمَل بشبح شفّاف.
 */
@Composable
private fun TestGroup(@StringRes title: Int, actions: List<TestAction>) {
    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        actions.chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
            ) {
                pair.forEach { a ->
                    BouncyChip(stringResource(a.label), a.icon, Modifier.weight(1f), a.run)
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * محتوى تبويب «الأساس»: حالة التشغيل، الاختبارات، الأنشطة.
 */
@Composable
internal fun CoreTabContent(
    running: Boolean,
    setEnabled: (Boolean) -> Unit,
    a11yOk: Boolean,
    listenerOk: Boolean,
    postOk: Boolean,
    btOk: Boolean,
    batteryOk: Boolean,
    ctx: Context,
    postLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    btLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    triggers: Set<String>,
    blocked: Set<String>,
    appsOpen: Boolean,
    appsOpenSetter: (Boolean) -> Unit,
    demoMsg: String,
    demoMsg2: String,
    demoMsg3: String,
    demoNav: String,
    demoEta: String,
    demoOrder: String,
    demoArrive: String,
    demoDownload: String,
    scheme: ColorScheme
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.section)
    ) {
        /* ── ١. الحالة والمفتاح الرئيس + ما ينقص من صلاحيات ─────────────── */
        MaxSection(title = stringResource(R.string.hud_section_state)) {
            MaxGroup {
                MaxSwitchRow(
                    title = stringResource(if (running) R.string.ify_home_island_on else R.string.ify_home_island_off),
                    subtitle = stringResource(if (running) R.string.ify_home_island_on_sub else R.string.ify_home_island_off_sub),
                    checked = running,
                    onCheckedChange = { setEnabled(it) },
                    icon = Icons.Rounded.PowerSettingsNew,
                    iconTone = if (running) MaxTone.Accent else MaxTone.Neutral
                )
            }
            if (!a11yOk) {
                MaxConditionNotice(
                    MaxCondition(
                        kind = MaxConditionKind.PermissionRequired,
                        title = stringResource(R.string.ify_home_taps_title),
                        detail = stringResource(R.string.ify_home_taps_body),
                        technicalDetail = stringResource(R.string.ify_home_taps_hint),
                        primaryActionLabel = stringResource(R.string.ify_home_open_settings),
                        onPrimaryAction = { Perms.openAccessibilitySettings(ctx) },
                        secondaryActionLabel = stringResource(R.string.ify_common_app_info),
                        onSecondaryAction = { Perms.openAppInfo(ctx) }
                    )
                )
            }
            if (!listenerOk) {
                MaxConditionNotice(
                    MaxCondition(
                        kind = MaxConditionKind.PermissionRequired,
                        title = stringResource(R.string.ify_perm_notification_access),
                        detail = stringResource(R.string.ify_perm_notification_body),
                        primaryActionLabel = stringResource(R.string.ify_home_open_settings),
                        onPrimaryAction = { Perms.openListenerSettings(ctx) }
                    )
                )
            }
            if (!postOk) {
                MaxConditionNotice(
                    MaxCondition(
                        kind = MaxConditionKind.PermissionRequired,
                        title = stringResource(R.string.ify_perm_show_notifications),
                        detail = stringResource(R.string.ify_perm_show_notifications_body),
                        primaryActionLabel = stringResource(R.string.ify_perm_allow),
                        onPrimaryAction = { postLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    )
                )
            }
        }

        /* ── ٢. لأفضل نتيجة: ما ليس حاجزًا لكن الجزيرة أعذب به ──────────── */
        if (!btOk || !batteryOk) {
            MaxSection(title = stringResource(R.string.ify_home_best_title)) {
                MaxGroup {
                    if (!btOk) {
                        MaxNavigationRow(
                            title = stringResource(R.string.ify_perm_bluetooth),
                            icon = Icons.Rounded.Bluetooth,
                            onClick = { btLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) }
                        )
                    }
                    if (!btOk && !batteryOk) MaxGroupDivider()
                    if (!batteryOk) {
                        MaxNavigationRow(
                            title = stringResource(R.string.ify_perm_battery),
                            icon = Icons.Rounded.BatteryChargingFull,
                            onClick = { Perms.openBatterySettings(ctx) }
                        )
                    }
                }
            }
        }

        /* ── ٣. جرّب الآن: نفس إجراءات الاستوديو على المحرّك الحقيقي ────── */
        MaxSection(
            title = stringResource(R.string.ify_home_test_title),
            description = stringResource(R.string.ify_home_test_hint)
        ) {
            TestGroup(
                R.string.ify_test_group_basic,
                listOf(
                    TestAction(R.string.ify_demo_message, Icons.AutoMirrored.Rounded.Chat) {
                        IslandController.flash(IslandMode.Notification("WhatsApp", "Rahul", demoMsg))
                    },
                    TestAction(R.string.ify_demo_charging, Icons.Rounded.Bolt) {
                        IslandController.flash(IslandMode.Charging(82))
                    },
                    TestAction(R.string.ify_test_timer_60, Icons.Rounded.Timer) {
                        IslandController.startTimer(60)
                    },
                    TestAction(R.string.ify_demo_music, Icons.Rounded.MusicNote) {
                        IslandController.setMedia(IslandMode.Media("Blinding Lights", "The Weeknd", true))
                    },
                    TestAction(R.string.ify_demo_call, Icons.Rounded.Call) {
                        IslandController.setCall("Mummy", true, null, null, canAnswer = true)
                    },
                    TestAction(R.string.ify_test_end_call, Icons.Rounded.CallEnd) {
                        IslandController.clearCall()
                    },
                    TestAction(R.string.ify_demo_earbuds, Icons.Rounded.Headphones) {
                        IslandController.flash(IslandMode.Device("AirPods Pro", true), 3500)
                    },
                    TestAction(R.string.ify_trigger_battery, Icons.Rounded.BatteryAlert) {
                        IslandController.flash(IslandMode.LowBattery(15), 5000)
                    },
                    TestAction(R.string.ify_test_silent, Icons.AutoMirrored.Rounded.VolumeOff) {
                        IslandController.flash(IslandMode.Toggle(ToggleKind.Silent, true), 2500)
                    },
                    TestAction(R.string.ify_test_flashlight, Icons.Rounded.FlashOn) {
                        IslandController.flash(IslandMode.Toggle(ToggleKind.Flashlight, true), 2500)
                    },
                )
            )
            TestGroup(
                R.string.ify_test_group_live,
                listOf(
                    TestAction(R.string.ify_trigger_navigation, Icons.Rounded.Navigation) {
                        IslandController.setLive(
                            IslandMode.Live(LiveKind.Navigation, "Maps", demoNav, demoEta),
                            "demo", null, null
                        )
                    },
                    TestAction(R.string.ify_test_food_order, Icons.Rounded.DeliveryDining) {
                        IslandController.setLive(
                            IslandMode.Live(LiveKind.Tracking, "Zomato", demoOrder, demoArrive),
                            "demo", null, null
                        )
                    },
                    TestAction(R.string.ify_trigger_downloads, Icons.Rounded.Download) {
                        IslandController.setDownload(
                            IslandMode.Live(LiveKind.Download, "Chrome", "movie.mp4", demoDownload, 38),
                            "demo-dl", null, null
                        )
                    },
                    TestAction(R.string.ify_test_clear_live, Icons.Rounded.Close) {
                        IslandController.clearLive()
                        IslandController.clearDownload()
                    },
                )
            )
            TestGroup(
                R.string.ify_test_group_combo,
                listOf(
                    TestAction(R.string.ify_test_three_messages, Icons.AutoMirrored.Rounded.Chat) {
                        IslandController.flash(IslandMode.Notification("WhatsApp", "Rahul", demoMsg))
                        IslandController.flash(IslandMode.Notification("WhatsApp", "Priya", demoMsg2))
                        IslandController.flash(IslandMode.Notification("Messages", "Mummy", demoMsg3))
                    },
                    TestAction(R.string.ify_test_nav_music, Icons.Rounded.Navigation) {
                        IslandController.setMedia(IslandMode.Media("Blinding Lights", "The Weeknd", true))
                        IslandController.setLive(
                            IslandMode.Live(LiveKind.Navigation, "Maps", demoNav, demoEta),
                            "demo", null, null
                        )
                    },
                    TestAction(R.string.ify_test_clear_music, Icons.Rounded.Close) {
                        IslandController.setMedia(null)
                    },
                )
            )
        }

        /* ── ٤. ما تعرضه الجزيرة: ١٢ نشاطًا ثم التطبيقات في الصفّ نفسه ──── */
        MaxSection(title = stringResource(R.string.ify_act_title)) {
            MaxGroup {
                Trigger.entries.forEachIndexed { i, t ->
                    if (i > 0) MaxGroupDivider()
                    MaxSwitchRow(
                        title = stringResource(t.label),
                        subtitle = stringResource(t.hint()),
                        checked = t.key in triggers,
                        onCheckedChange = { IslandSettings.setTrigger(t, it) },
                        icon = t.icon()
                    )
                }
                MaxGroupDivider()
                MaxNavigationRow(
                    title = stringResource(R.string.ify_tab_apps),
                    valueText = stringResource(R.string.ify_apps_blocked, blocked.size),
                    icon = Icons.Rounded.Android,
                    onClick = { appsOpenSetter(true) }
                )
            }
            Text(
                text = stringResource(R.string.ify_act_footer),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
            )
        }
    }
}

/**
 * محتوى تبويب «التصميم»: الموضع، الشكل، الحركة، السلوك.
 */
@Composable
internal fun DesignTabContent(
    x: Float,
    y: Float,
    scale: Float,
    w: Float,
    h: Float,
    corner: Float,
    speed: Float,
    glow: Boolean,
    haptics: Boolean,
    collapse: Int,
    idlePill: Boolean,
    stickyNotif: Boolean,
    appAccentOn: Boolean,
    dynamicOk: Boolean,
    source: Int,
    seed: Int,
    ctx: Context,
    nudge: (Float, Float) -> Unit,
    scheme: ColorScheme
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.section)
    ) {
        /* ── ٥. الموضع: منزلقان ثم تحريك دقيق بلمسات ───────────────────── */
        MaxSection(title = stringResource(R.string.ify_cust_position)) {
            MaxGroup {
                MaxSliderRow(
                    title = stringResource(R.string.ify_cust_left_right),
                    value = x,
                    onValueChange = { IslandSettings.offsetX.value = it },
                    valueText = "${x.roundToInt()} dp",
                    valueRange = -180f..180f,
                    steps = 35,
                    onValueChangeFinished = { IslandSettings.save() },
                    mirrorsInRtl = false
                )
                MaxGroupDivider()
                MaxSliderRow(
                    title = stringResource(R.string.ify_cust_up_down),
                    value = y,
                    onValueChange = { IslandSettings.offsetY.value = it },
                    valueText = "${y.roundToInt()} dp",
                    valueRange = 0f..300f,
                    steps = 29,
                    onValueChangeFinished = { IslandSettings.save() }
                )
                MaxGroupDivider()
                Column(
                    Modifier.padding(
                        horizontal = MaxSpace.rowPaddingHorizontal,
                        vertical = MaxSpace.sm
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
                ) {
                    Text(
                        text = stringResource(R.string.ify_cust_nudge_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(onClick = { nudge(-1f, 0f) }) {
                            Icon(Icons.Rounded.ChevronLeft, stringResource(R.string.ify_dir_left))
                        }
                        FilledTonalIconButton(onClick = { nudge(0f, -1f) }) {
                            Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.ify_dir_up))
                        }
                        FilledTonalIconButton(onClick = { nudge(0f, 1f) }) {
                            Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.ify_dir_down))
                        }
                        FilledTonalIconButton(onClick = { nudge(1f, 0f) }) {
                            Icon(Icons.Rounded.ChevronRight, stringResource(R.string.ify_dir_right))
                        }
                        FilledTonalButton(onClick = {
                            IslandSettings.offsetX.value = 0f
                            IslandSettings.save()
                        }) { Text(stringResource(R.string.ify_cust_center)) }
                    }
                }
            }
        }

        /* ── ٦. الشكل: قوالب جاهزة ثم القياسات الدقيقة ─────────────────── */
        MaxSection(title = stringResource(R.string.ify_cust_shape)) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
            ) {
                BouncyChip(stringResource(R.string.ify_preset_small)) { IslandSettings.preset(0.8f, 0.85f, 1f) }
                BouncyChip(stringResource(R.string.ify_preset_normal)) { IslandSettings.resetShape() }
                BouncyChip(stringResource(R.string.ify_preset_large)) { IslandSettings.preset(1.2f, 1.1f, 1.1f) }
                BouncyChip(stringResource(R.string.ify_preset_wide)) { IslandSettings.preset(1f, 1.45f, 1f) }
                BouncyChip(stringResource(R.string.ify_preset_tall)) { IslandSettings.preset(1f, 1f, 1.5f) }
            }
            MaxGroup {
                MaxSliderRow(
                    title = stringResource(R.string.ify_cust_size),
                    value = scale,
                    onValueChange = { IslandSettings.scale.value = it },
                    valueText = "${(scale * 100).roundToInt()}%",
                    valueRange = 0.5f..1.8f,
                    steps = 13,
                    onValueChangeFinished = { IslandSettings.save() }
                )
                MaxGroupDivider()
                MaxSliderRow(
                    title = stringResource(R.string.ify_cust_width),
                    value = w,
                    onValueChange = { IslandSettings.widthScale.value = it },
                    valueText = "${(w * 100).roundToInt()}%",
                    valueRange = 0.6f..1.6f,
                    steps = 10,
                    onValueChangeFinished = { IslandSettings.save() }
                )
                MaxGroupDivider()
                MaxSliderRow(
                    title = stringResource(R.string.ify_cust_height),
                    value = h,
                    onValueChange = { IslandSettings.heightScale.value = it },
                    valueText = "${(h * 100).roundToInt()}%",
                    valueRange = 0.7f..1.6f,
                    steps = 9,
                    onValueChangeFinished = { IslandSettings.save() }
                )
                MaxGroupDivider()
                MaxSliderRow(
                    title = stringResource(R.string.ify_cust_roundness),
                    value = corner,
                    onValueChange = { IslandSettings.corner.value = it },
                    valueText = "${(corner * 100).roundToInt()}%",
                    valueRange = 0.2f..1f,
                    steps = 8,
                    onValueChangeFinished = { IslandSettings.save() }
                )
            }
        }

        /* ── ٧. الحركة والإحساس ────────────────────────────────────────── */
        MaxSection(title = stringResource(R.string.ify_cust_anim_title)) {
            MaxGroup {
                MaxSliderRow(
                    title = stringResource(R.string.ify_cust_anim_speed),
                    value = speed,
                    onValueChange = { IslandSettings.animSpeed.value = it },
                    valueText = "×" + "%.1f".format(speed),
                    valueRange = 0.5f..2f,
                    steps = 15,
                    onValueChangeFinished = { IslandSettings.save() }
                )
                MaxGroupDivider()
                MaxSwitchRow(
                    title = stringResource(R.string.ify_cust_glow),
                    subtitle = stringResource(R.string.ify_cust_glow_sub),
                    checked = glow,
                    onCheckedChange = { IslandSettings.glow.value = it; IslandSettings.save() },
                    icon = Icons.Rounded.Lightbulb
                )
                MaxGroupDivider()
                MaxSwitchRow(
                    title = stringResource(R.string.ify_cust_haptics),
                    subtitle = stringResource(R.string.ify_cust_haptics_sub),
                    checked = haptics,
                    onCheckedChange = { IslandSettings.haptics.value = it; IslandSettings.save() },
                    icon = Icons.Rounded.Vibration
                )
            }
        }

        /* ── ٨. السلوك: متى تبقى، ومتى تسكن، وبأي لون تتوهّج ──────────── */
        MaxSection(title = stringResource(R.string.ify_cust_behavior)) {
            MaxGroup {
                MaxSliderRow(
                    title = stringResource(R.string.ify_cust_auto_collapse),
                    value = collapse.toFloat(),
                    onValueChange = { IslandSettings.autoCollapseSec.value = it.roundToInt() },
                    valueText = if (collapse == 0) {
                        stringResource(R.string.ify_common_off)
                    } else {
                        stringResource(R.string.ify_cust_seconds, collapse)
                    },
                    valueRange = 0f..15f,
                    steps = 15,
                    onValueChangeFinished = { IslandSettings.save() }
                )
                MaxGroupDivider()
                MaxSwitchRow(
                    title = stringResource(R.string.ify_cust_idle_pill),
                    subtitle = stringResource(R.string.ify_cust_idle_pill_sub),
                    checked = idlePill,
                    onCheckedChange = { IslandSettings.idlePill.value = it; IslandSettings.save() },
                    icon = Icons.Rounded.AccessTime
                )
                MaxGroupDivider()
                MaxSwitchRow(
                    title = stringResource(R.string.ify_cust_sticky_notif),
                    subtitle = stringResource(R.string.ify_cust_sticky_notif_sub),
                    checked = stickyNotif,
                    onCheckedChange = { IslandSettings.stickyNotif.value = it; IslandSettings.save() },
                    icon = Icons.Rounded.PushPin
                )
                MaxGroupDivider()
                MaxSwitchRow(
                    title = stringResource(R.string.ify_cust_app_accent),
                    subtitle = stringResource(R.string.ify_cust_app_accent_sub),
                    checked = appAccentOn,
                    onCheckedChange = { IslandSettings.islandAppAccent.value = it; IslandSettings.save() },
                    icon = Icons.Rounded.Palette
                )
            }
        }

        /* ── ١٠. اللون المميّز: من الخلفية تلقائيًّا أو اختيارًا ───────── */
        MaxSection(title = stringResource(R.string.ify_cust_accent_color)) {
            MaxSegmented(
                options = if (dynamicOk) {
                    listOf(
                        stringResource(R.string.ify_cust_wallpaper),
                        stringResource(R.string.ify_cust_custom)
                    )
                } else {
                    listOf(stringResource(R.string.ify_cust_custom))
                },
                selectedIndex = if (dynamicOk) source else 0,
                onSelect = { i ->
                    IslandSettings.accentSource.value = if (dynamicOk) i else 1
                    IslandSettings.save()
                }
            )
            if (!dynamicOk || source == 1) {
                AccentSwatches(
                    selectedHex = accentHexOf(seed),
                    onSelect = { hex ->
                        IslandSettings.accentSeed.value = parseAccentHex(hex).toArgb()
                        IslandSettings.save()
                    },
                    colors = ISLAND_ACCENTS
                )
            }
        }
    }
}

/**
 * محتوى تبويب «المتقدم»: إيماءات السحب، النوافذ، إعادة الضبط.
 */
@Composable
internal fun AdvancedTabContent(
    swUp: SwipeAction,
    swDown: SwipeAction,
    swLeft: SwipeAction,
    swRight: SwipeAction,
    swipeDir: SwipeDir?,
    swipeDirSetter: (SwipeDir?) -> Unit,
    styles: Map<String, KindStyle>,
    kindSheetKind: IslandKind?,
    kindSheetKindSetter: (IslandKind?) -> Unit,
    kindSheetAll: Boolean,
    kindSheetAllSetter: (Boolean) -> Unit,
    resetConfirm: Boolean,
    resetConfirmSetter: (Boolean) -> Unit,
    scheme: ColorScheme
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.section)
    ) {
        /* ── ٩. إيماءات السحب — كل صفّ يفتح مُنتقي إجرائه ─────────────── */
        MaxSection(title = stringResource(R.string.ify_cust_swipe_title)) {
            MaxGroup {
                MaxNavigationRow(
                    title = stringResource(R.string.ify_cust_swipe_up),
                    valueText = stringResource(swUp.label),
                    icon = Icons.Rounded.KeyboardArrowUp,
                    onClick = { swipeDirSetter(SwipeDir.Up) }
                )
                MaxGroupDivider()
                MaxNavigationRow(
                    title = stringResource(R.string.ify_cust_swipe_down),
                    valueText = stringResource(swDown.label),
                    icon = Icons.Rounded.KeyboardArrowDown,
                    onClick = { swipeDirSetter(SwipeDir.Down) }
                )
                MaxGroupDivider()
                MaxNavigationRow(
                    title = stringResource(R.string.ify_cust_swipe_left),
                    valueText = stringResource(swLeft.label),
                    icon = Icons.Rounded.SwapHoriz,
                    onClick = { swipeDirSetter(SwipeDir.Left) }
                )
                MaxGroupDivider()
                MaxNavigationRow(
                    title = stringResource(R.string.ify_cust_swipe_right),
                    valueText = stringResource(swRight.label),
                    icon = Icons.Rounded.SwapHoriz,
                    onClick = { swipeDirSetter(SwipeDir.Right) }
                )
            }
            Text(
                text = stringResource(R.string.ify_cust_swipe_hint),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
            )
        }

        /* ── ١١. النوافذ والأنشطة: كل صفّ يفتح ورقة تعديله الحيّة ───────── */
        MaxSection(
            title = stringResource(R.string.ify_cust_banner),
            description = stringResource(R.string.ify_cust_banner_sub)
        ) {
            MaxGroup {
                MaxNavigationRow(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.ify_banner_all),
                    subtitle = stringResource(R.string.ify_banner_all_sub),
                    onClick = { kindSheetAllSetter(true) }
                )
            }
            ISLAND_KIND_GROUPS.forEach { (groupTitle, kinds) ->
                Text(
                    text = stringResource(groupTitle),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                    modifier = Modifier.padding(start = MaxSpace.groupPadding, top = MaxSpace.xs)
                )
                MaxGroup {
                    kinds.forEachIndexed { i, k ->
                        if (i > 0) MaxGroupDivider()
                        MaxNavigationRow(
                            icon = k.icon(),
                            title = stringResource(k.label),
                            valueText = kindStyleSummary(styles[k.key] ?: KindStyle()),
                            onClick = { kindSheetKindSetter(k) }
                        )
                    }
                }
            }
        }

        /* ── ١٢. إعادة الضبط — بلا مساس بالأنشطة والتطبيقات ───────────── */
        MaxGroup {
            MaxNavigationRow(
                title = stringResource(R.string.ify_cust_reset),
                subtitle = stringResource(R.string.ify_cust_reset_sub),
                icon = Icons.Rounded.RestartAlt,
                iconTone = MaxTone.Critical,
                onClick = { resetConfirmSetter(true) }
            )
        }
    }
}
