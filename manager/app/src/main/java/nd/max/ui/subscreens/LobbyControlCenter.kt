/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.subscreens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material.icons.rounded.ViewSidebar
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.core.gamespace.HandleSize
import nd.max.core.gamespace.LobbyCue
import nd.max.core.gamespace.LobbyDepth
import nd.max.core.gamespace.LobbyPrefsState
import nd.max.core.gamespace.LobbySort
import nd.max.core.gamespace.PanelSensitivity
import nd.max.core.gamespace.PanelSide
import nd.max.core.gamespace.lobbyEnumOr
import nd.max.ui.component.AppIconImage
import nd.max.ui.component.LobbyNotice
import nd.max.ui.component.LobbyPalette
import nd.max.ui.component.LocalLobbySounds
import nd.max.ui.component.canvasScaleLabel
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius
import nd.max.ui.util.AppConfig
import nd.max.ui.util.GamePanelPrefs
import nd.max.ui.util.GameSpaceShortcutPinner
import nd.max.ui.util.PinOutcome
import nd.max.ui.util.canvasScaleOptions
import nd.max.ui.util.getSupportedRefreshRates

/*
 * مركز التحكم: **سطح واحد** يضمّ ما كان موزّعًا على أربع نوافذ (ملف اللعبة، ومقبض اللوحة الجانبية،
 * وإدارة الألعاب، والمجموعات المحذوفة) مضافًا إليه خيارات اللوبي الجديدة. شريط جانبيّ بأربعة أقسام
 * ومحتوى يتبدّل بتلاشٍ ناعم؛ لا نوافذ منبثقة ولا تنقّل داخليّ.
 *
 * المسؤوليّات كما كانت (لم تُغيَّر):
 *  - قيم ملف اللعبة تكتب عبر `onUpdate` إلى `AppSettingsViewModel` وحده (ADR-11: لا عتاد من هنا).
 *  - إعدادات المقبض تُحفظ في `GamePanelPrefs` لحظة التغيير وتلتقطها الخدمة بمستمع التفضيلات.
 *  - إدراج/استبعاد لعبة يمرّ بـ`GameSpaceRepository.membership` وحدها عبر `onToggleGame`.
 *  - خيارات اللوبي تفضيلات عرض محلّية في `LobbyPrefs` ولا تلمس الجهاز.
 */

/** أقسام مركز التحكم. الترتيب هو ترتيب الشريط الجانبي. */
internal enum class LobbySection(val titleRes: Int, val icon: ImageVector) {
    Game(R.string.lobby_tile_profile, Icons.Rounded.Tune),
    Panel(R.string.lobby_tile_panel, Icons.Rounded.ViewSidebar),
    Lobby(R.string.lobby_cc_lobby, Icons.Rounded.Layers),
    Library(R.string.lobby_cc_library, Icons.Rounded.VideogameAsset)
}

/** هل اللعبة في اللوبي الآن؟ — الاستبعاد الصريح يغلب الكشف الآلي (نفس قاعدة `gameLibrary`). */
internal fun isInLobby(app: GameApp, manual: Set<String>, excluded: Set<String>): Boolean =
    app.packageName !in excluded && (app.detectedGame || app.packageName in manual)

private val SidebarWidth = 208.dp
private val SidebarItemHeight = 52.dp
private val SidebarIcon = 40.dp
private val ChipHeight = 48.dp
private val LibraryIcon = 40.dp

/**
 * @param section القسم الظاهر؛ يتحكّم به الأب ليفتح اللوبي على قسم بعينه (نقر بلاطة ملف اللعبة مثلًا).
 * @param app اللعبة المحدَّدة في الحلقة (`null` حين لا ألعاب).
 * @param onPrefs يستلم دالّة تحوّل الخيارات الحاليّة إلى الجديدة فلا تضيع تغييرات متتابعة.
 * @param accent لون اللعبة (أو العلامة) لتمييز القسم المختار.
 */
@Composable
internal fun LobbyControlCenter(
    section: LobbySection,
    onSection: (LobbySection) -> Unit,
    app: GameApp?,
    config: AppConfig,
    panelEnabled: Boolean,
    onPanelEnabled: (Boolean) -> Unit,
    onUpdate: (String, String) -> Unit,
    onOpenFull: () -> Unit,
    prefs: LobbyPrefsState,
    onPrefs: ((LobbyPrefsState) -> LobbyPrefsState) -> Unit,
    apps: List<GameApp>,
    manual: Set<String>,
    excluded: Set<String>,
    onToggleGame: (String, Boolean) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        ControlSidebar(
            selected = section,
            onSelect = onSection,
            app = app,
            accent = accent,
            modifier = Modifier
                .width(SidebarWidth)
                .fillMaxHeight()
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Crossfade(
                targetState = section,
                animationSpec = tween(MaxDuration.quick),
                label = "lobbySection",
                modifier = Modifier
                    .widthIn(max = MaxSize.readingMaxWidth)
                    .fillMaxSize()
            ) { current ->
                when (current) {
                    LobbySection.Game -> GameBody(app, config, onUpdate, onOpenFull)
                    LobbySection.Panel -> PanelBody(app, panelEnabled, onPanelEnabled)
                    LobbySection.Lobby -> LobbyOptionsBody(prefs, onPrefs)
                    LobbySection.Library -> LibraryBody(apps, manual, excluded, onToggleGame, accent)
                }
            }
        }
    }
}

// ─────────────────────────────── الشريط الجانبي ───────────────────────────────

@Composable
private fun ControlSidebar(
    selected: LobbySection,
    onSelect: (LobbySection) -> Unit,
    app: GameApp?,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.control))
    Column(
        modifier = modifier
            .background(LobbyPalette.Black.copy(alpha = 0.40f))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaxSpace.md, vertical = MaxSpace.md),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        if (app != null) {
            Row(
                modifier = Modifier.padding(bottom = MaxSpace.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
            ) {
                AppIconImage(packageName = app.packageName, size = SidebarIcon, contentDescription = null)
                Text(
                    text = app.label,
                    color = LobbyPalette.Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        val sounds = LocalLobbySounds.current
        LobbySection.entries.forEach { item ->
            val isSelected = item == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SidebarItemHeight)
                    .clip(shape)
                    .background(if (isSelected) accent.copy(alpha = 0.26f) else Color.Transparent)
                    .border(
                        MaxSize.hairlineBorder,
                        if (isSelected) accent.copy(alpha = 0.60f) else Color.Transparent,
                        shape
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = {
                            if (!isSelected) sounds.play(LobbyCue.Tap)
                            onSelect(item)
                        }
                    )
                    .padding(horizontal = MaxSpace.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (isSelected) LobbyPalette.Ink else LobbyPalette.Muted
                )
                Text(
                    text = stringResource(item.titleRes),
                    color = if (isSelected) LobbyPalette.Ink else LobbyPalette.Muted,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─────────────────────────────── قسم ملف اللعبة ───────────────────────────────

@Composable
private fun GameBody(
    app: GameApp?,
    config: AppConfig,
    onUpdate: (String, String) -> Unit,
    onOpenFull: () -> Unit
) {
    if (app == null) {
        EmptyBody()
    } else {
        GameProfileColumn(app, config, onUpdate, onOpenFull)
    }
}

@Composable
private fun GameProfileColumn(
    app: GameApp,
    config: AppConfig,
    onUpdate: (String, String) -> Unit,
    onOpenFull: () -> Unit
) {
    val context = LocalContext.current
    val defaultLabel = stringResource(R.string.default_label)
    val refreshModes = remember { getSupportedRefreshRates(context) }
    // §9.5: أسماء دقة مفهومة محسوبة من ضلع شاشة الجهاز — لا معاملات خام («0.50»).
    val nativeShortSide = remember {
        val m = context.resources.displayMetrics
        minOf(m.widthPixels, m.heightPixels)
    }
    val downscale = remember(nativeShortSide, config.resolution_downscale) {
        canvasScaleOptions(nativeShortSide, config.resolution_downscale)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaxSpace.xl, vertical = MaxSpace.lg),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.lg)
    ) {
        Text(
            text = stringResource(R.string.lobby_settings_title, app.label),
            color = LobbyPalette.Ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        LobbySectionCard(stringResource(R.string.lobby_head_touch)) {
            LobbyOptionRow(
                title = stringResource(R.string.lobby_row_touch),
                options = listOf(
                    "default" to stringResource(R.string.lobby_touch_follow),
                    "true" to stringResource(R.string.lobby_touch_responsive),
                    "false" to stringResource(R.string.lobby_touch_battery)
                ),
                selected = config.touch_boost,
                onSelect = { onUpdate("touch_boost", it) }
            )
            LobbyTriStateRow(stringResource(R.string.lobby_row_haptics), config.haptic_feedback) {
                onUpdate("haptic_feedback", it)
            }
        }
        LobbySectionCard(stringResource(R.string.lobby_head_perf)) {
            LobbyTriStateRow(stringResource(R.string.lobby_row_lite), config.perf_lite_mode) {
                onUpdate("perf_lite_mode", it)
            }
            LobbyTriStateRow(stringResource(R.string.lobby_row_cpuboost), config.cpu_boost) {
                onUpdate("cpu_boost", it)
            }
            LobbyTriStateRow(stringResource(R.string.lobby_row_killbg), config.kill_bg_apps) {
                onUpdate("kill_bg_apps", it)
            }
            LobbyTriStateRow(stringResource(R.string.lobby_row_preload), config.game_preload) {
                onUpdate("game_preload", it)
            }
        }
        LobbySectionCard(stringResource(R.string.lobby_head_gpu)) {
            LobbyOptionRow(
                title = stringResource(R.string.lobby_row_gpu),
                options = listOf(
                    "default" to defaultLabel,
                    "power" to stringResource(R.string.lobby_gpu_power),
                    "balanced" to stringResource(R.string.lobby_gpu_balanced),
                    "gaming" to stringResource(R.string.lobby_gpu_gaming),
                    "performance" to stringResource(R.string.lobby_gpu_performance)
                ),
                selected = config.gpu_profile,
                onSelect = { onUpdate("gpu_profile", it) }
            )
            LobbyTriStateRow(stringResource(R.string.lobby_row_hwui), config.force_hw_ui) {
                onUpdate("force_hw_ui", it)
            }
        }
        LobbySectionCard(stringResource(R.string.lobby_head_show)) {
            LobbyOptionRow(
                title = stringResource(R.string.lobby_row_refresh),
                options = refreshModes.map {
                    it to if (it.equals("default", ignoreCase = true)) defaultLabel else "${it}Hz"
                },
                selected = config.refresh_rate,
                onSelect = { onUpdate("refresh_rate", it) }
            )
            LobbyOptionRow(
                title = stringResource(R.string.lobby_row_downscale),
                options = downscale.filter { it.supported }.map { it.stored to canvasScaleLabel(it) },
                selected = config.resolution_downscale,
                onSelect = { onUpdate("resolution_downscale", it) }
            )
        }
        LobbySectionCard(stringResource(R.string.lobby_head_net)) {
            LobbyTriStateRow(stringResource(R.string.lobby_row_wifi), config.wifi_no_sleep) {
                onUpdate("wifi_no_sleep", it)
            }
        }
        LobbySectionCard(stringResource(R.string.lobby_head_function)) {
            LobbyTriStateRow(stringResource(R.string.lobby_row_dnd), config.dnd_on_gaming) {
                onUpdate("dnd_on_gaming", it)
            }
            LobbyTriStateRow(stringResource(R.string.lobby_row_notifs), config.disable_notifs) {
                onUpdate("disable_notifs", it)
            }
            LobbyTriStateRow(stringResource(R.string.lobby_row_bypass), config.bypass_charging) {
                onUpdate("bypass_charging", it)
            }
            OutlinedButton(
                onClick = onOpenFull,
                border = BorderStroke(MaxSize.hairlineBorder, LobbyPalette.Hairline),
                modifier = Modifier.heightIn(min = MaxSize.minTouchTarget)
            ) {
                Text(text = stringResource(R.string.lobby_open_full), color = LobbyPalette.Ink)
            }
        }
        Text(
            text = stringResource(R.string.gaming_runtime_boundary),
            color = LobbyPalette.Steel,
            fontSize = 12.sp
        )
    }
}

// ─────────────────────────────── قسم اللوحة الجانبية ───────────────────────────────

/**
 * مفتاح اللوحة لهذه اللعبة + إعدادات المقبض (عامّة لكل الألعاب). كل تغيير يُحفظ لحظته والخدمة تلتقطه
 * بمستمع التفضيلات، فلا زرّ «حفظ». سحب المقبض نفسه بالضغط المطوّل يغيّر الحافة والارتفاع بلا مرور من هنا.
 */
@Composable
private fun PanelBody(
    app: GameApp?,
    panelEnabled: Boolean,
    onPanelEnabled: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var handle by remember { mutableStateOf(GamePanelPrefs.load(context)) }
    val reload = { handle = GamePanelPrefs.load(context) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaxSpace.xl, vertical = MaxSpace.lg),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.lg)
    ) {
        if (app != null) {
            LobbySectionCard(stringResource(R.string.lobby_tile_panel)) {
                LobbySwitchRow(stringResource(R.string.lobby_row_panel), panelEnabled, onPanelEnabled)
            }
        }
        LobbySectionCard(stringResource(R.string.panel_handle_title)) {
            // يمين/يسار **فيزيائيّان**: الصفّ مثبَّت LTR كي لا ينعكس ترتيبه في RTL فيخالف الحافة المرسومة.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                LobbyChoiceRow(
                    title = stringResource(R.string.panel_handle_edge),
                    options = listOf(
                        PanelSide.Start to stringResource(R.string.panel_handle_left),
                        PanelSide.End to stringResource(R.string.panel_handle_right)
                    ),
                    selected = handle.side,
                    onSelect = {
                        GamePanelPrefs.saveSide(context, it)
                        reload()
                    }
                )
            }
            LobbyChoiceRow(
                title = stringResource(R.string.panel_handle_size),
                options = listOf(
                    HandleSize.Small to stringResource(R.string.panel_handle_size_small),
                    HandleSize.Medium to stringResource(R.string.panel_handle_size_medium),
                    HandleSize.Large to stringResource(R.string.panel_handle_size_large)
                ),
                selected = handle.handleSize,
                onSelect = {
                    GamePanelPrefs.saveHandleSize(context, it)
                    reload()
                }
            )
            LobbyChoiceRow(
                title = stringResource(R.string.panel_handle_sensitivity),
                options = listOf(
                    PanelSensitivity.Low to stringResource(R.string.panel_handle_sens_low),
                    PanelSensitivity.Balanced to stringResource(R.string.panel_handle_sens_balanced),
                    PanelSensitivity.High to stringResource(R.string.panel_handle_sens_high)
                ),
                selected = handle.sensitivity,
                onSelect = {
                    GamePanelPrefs.saveSensitivity(context, it)
                    reload()
                }
            )
            LobbyToggleRow(
                title = stringResource(R.string.panel_handle_lock),
                detail = stringResource(R.string.panel_handle_lock_desc),
                checked = handle.locked,
                onChange = {
                    GamePanelPrefs.saveLocked(context, it)
                    reload()
                }
            )
            LobbyToggleRow(
                title = stringResource(R.string.panel_handle_haptics),
                detail = null,
                checked = handle.haptics,
                onChange = {
                    GamePanelPrefs.saveHaptics(context, it)
                    reload()
                }
            )
            Text(
                text = stringResource(R.string.panel_handle_hint),
                color = LobbyPalette.Steel,
                fontSize = 12.sp
            )
            Text(
                text = stringResource(R.string.lobby_panel_handle_scope),
                color = LobbyPalette.Steel,
                fontSize = 12.sp
            )
            OutlinedButton(
                onClick = {
                    GamePanelPrefs.resetHandle(context)
                    reload()
                },
                modifier = Modifier.heightIn(min = MaxSize.minTouchTarget)
            ) {
                Text(text = stringResource(R.string.panel_handle_reset), color = LobbyPalette.Ink)
            }
        }
        CrosshairSettingsCard()
    }
}

// ─────────────────────────────── قسم اللوبي ───────────────────────────────

/** خيارات مظهر اللوبي وسلوكه — تفضيلات عرض محلّية، لا تكتب في العتاد ولا في ملف لعبة. */
@Composable
private fun LobbyOptionsBody(
    prefs: LobbyPrefsState,
    onPrefs: ((LobbyPrefsState) -> LobbyPrefsState) -> Unit
) {
    val context = LocalContext.current
    val sounds = LocalLobbySounds.current
    val shortcutLabel = stringResource(R.string.game_space_title)
    var shortcutOutcome by remember { mutableStateOf<PinOutcome?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaxSpace.xl, vertical = MaxSpace.lg),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.lg)
    ) {
        LobbySectionCard(stringResource(R.string.lobby_head_look)) {
            LobbyOptionRow(
                title = stringResource(R.string.lobby_row_depth),
                options = listOf(
                    LobbyDepth.Flat.name to stringResource(R.string.lobby_depth_flat),
                    LobbyDepth.Soft.name to stringResource(R.string.lobby_depth_soft),
                    LobbyDepth.Deep.name to stringResource(R.string.lobby_depth_deep)
                ),
                selected = prefs.depth.name,
                onSelect = { raw ->
                    onPrefs { it.copy(depth = lobbyEnumOr(raw, LobbyDepth.entries, it.depth)) }
                }
            )
            LobbyToggleRow(
                title = stringResource(R.string.lobby_row_colors),
                detail = stringResource(R.string.lobby_row_colors_desc),
                checked = prefs.gameColors,
                onChange = { value -> onPrefs { it.copy(gameColors = value) } }
            )
            LobbyToggleRow(
                title = stringResource(R.string.lobby_row_ambient),
                detail = stringResource(R.string.lobby_row_ambient_desc),
                checked = prefs.ambient,
                onChange = { value -> onPrefs { it.copy(ambient = value) } }
            )
            LobbyToggleRow(
                title = stringResource(R.string.lobby_row_status),
                detail = stringResource(R.string.lobby_row_status_desc),
                checked = prefs.showStatus,
                onChange = { value -> onPrefs { it.copy(showStatus = value) } }
            )
        }
        LobbySectionCard(stringResource(R.string.lobby_head_behavior)) {
            LobbyOptionRow(
                title = stringResource(R.string.lobby_row_sort),
                options = listOf(
                    LobbySort.Favorites.name to stringResource(R.string.lobby_sort_favorites),
                    LobbySort.Name.name to stringResource(R.string.lobby_sort_name),
                    LobbySort.Newest.name to stringResource(R.string.lobby_sort_newest)
                ),
                selected = prefs.sort.name,
                onSelect = { raw ->
                    onPrefs { it.copy(sort = lobbyEnumOr(raw, LobbySort.entries, it.sort)) }
                }
            )
            LobbyToggleRow(
                title = stringResource(R.string.lobby_row_flash),
                detail = stringResource(R.string.lobby_row_flash_desc),
                checked = prefs.launchFlash,
                onChange = { value -> onPrefs { it.copy(launchFlash = value) } }
            )
            LobbyToggleRow(
                title = stringResource(R.string.lobby_row_lobby_haptics),
                detail = null,
                checked = prefs.haptics,
                onChange = { value -> onPrefs { it.copy(haptics = value) } }
            )
            LobbyToggleRow(
                title = stringResource(R.string.lobby_row_sounds),
                detail = stringResource(R.string.lobby_row_sounds_desc),
                checked = prefs.sounds,
                onChange = { value -> onPrefs { it.copy(sounds = value) } }
            )
            LobbyToggleRow(
                title = stringResource(R.string.lobby_row_entrance),
                detail = stringResource(R.string.lobby_row_entrance_desc),
                checked = prefs.entrance,
                onChange = { value -> onPrefs { it.copy(entrance = value) } }
            )
            LobbyToggleRow(
                title = stringResource(R.string.lobby_row_awake),
                detail = stringResource(R.string.lobby_row_awake_desc),
                checked = prefs.keepScreenOn,
                onChange = { value -> onPrefs { it.copy(keepScreenOn = value) } }
            )
            LobbyToggleRow(
                title = stringResource(R.string.lobby_row_resume),
                detail = null,
                checked = prefs.resumeLast,
                onChange = { value -> onPrefs { it.copy(resumeLast = value) } }
            )
        }
        LobbySectionCard(stringResource(R.string.lobby_head_shortcut)) {
            Text(
                text = stringResource(R.string.lobby_shortcut_desc),
                color = LobbyPalette.Steel,
                fontSize = 12.sp
            )
            OutlinedButton(
                onClick = {
                    sounds.play(LobbyCue.Tap)
                    shortcutOutcome = GameSpaceShortcutPinner.requestPin(context, shortcutLabel)
                },
                border = BorderStroke(MaxSize.hairlineBorder, LobbyPalette.Hairline),
                modifier = Modifier.heightIn(min = MaxSize.minTouchTarget)
            ) {
                Icon(imageVector = Icons.Rounded.AddToHomeScreen, contentDescription = null, tint = LobbyPalette.Ink)
                Text(
                    text = stringResource(R.string.lobby_shortcut_add),
                    color = LobbyPalette.Ink,
                    modifier = Modifier.padding(start = MaxSpace.sm)
                )
            }
            shortcutOutcome?.let { outcome ->
                LobbyNotice(
                    message = stringResource(outcome.messageRes()),
                    caution = outcome == PinOutcome.Unsupported || outcome == PinOutcome.Failed
                )
            }
        }
        Text(
            text = stringResource(R.string.lobby_scope_note),
            color = LobbyPalette.Steel,
            fontSize = 12.sp
        )
        OutlinedButton(
            onClick = { onPrefs { LobbyPrefsState(lastPackage = it.lastPackage) } },
            modifier = Modifier.heightIn(min = MaxSize.minTouchTarget)
        ) {
            Icon(imageVector = Icons.Rounded.RestartAlt, contentDescription = null, tint = LobbyPalette.Ink)
            Text(
                text = stringResource(R.string.lobby_reset),
                color = LobbyPalette.Ink,
                modifier = Modifier.padding(start = MaxSpace.sm)
            )
        }
    }
}

/** نصّ نتيجة طلب الاختصار؛ كل نتيجة تُقال صراحةً (لا «تمّ» عامّة) لأن قرار التثبيت للمشغّل لا لنا. */
private fun PinOutcome.messageRes(): Int = when (this) {
    PinOutcome.Requested -> R.string.lobby_shortcut_requested
    PinOutcome.AlreadyPinned -> R.string.lobby_shortcut_exists
    PinOutcome.Unsupported -> R.string.lobby_shortcut_unsupported
    PinOutcome.Failed -> R.string.lobby_shortcut_failed
}

// ─────────────────────────────── قسم المكتبة ───────────────────────────────

/**
 * إدارة الألعاب: كل تطبيق قابل للتشغيل مع مفتاح إدراج/إزالة، وبحث. هذا ما كان «إدارة الألعاب» نافذةً؛
 * يقيم الآن داخل المركز. الكتابة تمرّ بـ`GameSpaceRepository.membership` وحدها — لا مخزن ثانٍ.
 */
@Composable
private fun LibraryBody(
    apps: List<GameApp>,
    manual: Set<String>,
    excluded: Set<String>,
    onToggle: (pkg: String, include: Boolean) -> Unit,
    accent: Color
) {
    var query by rememberSaveable { mutableStateOf("") }
    val rows = remember(apps, manual, excluded, query) {
        apps.filter {
            query.isBlank() ||
                it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }.sortedWith(
            compareByDescending<GameApp> { isInLobby(it, manual, excluded) }
                .thenBy { it.label.lowercase() }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaxSpace.xl, vertical = MaxSpace.lg),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        Text(
            text = stringResource(R.string.lobby_manage_title),
            color = LobbyPalette.Ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MaxSize.minTouchTarget)
                .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
                .background(LobbyPalette.Panel)
                .padding(horizontal = MaxSpace.md),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text(text = stringResource(R.string.spoof_search), color = LobbyPalette.Muted, fontSize = 15.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = LobbyPalette.Ink, fontSize = 15.sp),
                cursorBrush = SolidColor(accent),
                modifier = Modifier.fillMaxWidth()
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            items(rows, key = { it.packageName }) { app ->
                val included = isInLobby(app, manual, excluded)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MaxSize.minTouchTarget)
                        .padding(vertical = MaxSpace.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
                ) {
                    AppIconImage(packageName = app.packageName, size = LibraryIcon, contentDescription = null)
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            color = LobbyPalette.Ink,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = app.packageName,
                            color = LobbyPalette.Muted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Switch(
                        checked = included,
                        onCheckedChange = { onToggle(app.packageName, it) },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = LobbyPalette.Red,
                            checkedThumbColor = LobbyPalette.Ink,
                            uncheckedTrackColor = LobbyPalette.PanelRaised,
                            uncheckedThumbColor = LobbyPalette.Muted
                        )
                    )
                }
            }
        }
    }
}

// ─────────────────────────────── مكوّنات مشتركة ───────────────────────────────

@Composable
private fun EmptyBody() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaxSpace.xl),
        contentAlignment = Alignment.TopCenter
    ) {
        LobbyNotice(message = stringResource(R.string.lobby_empty))
    }
}

/** صفّ خيارات متساوية العرض؛ المحدَّد بلون اللوبي الأحمر. القيمة المعروضة تتبع [selected] لا حالة محلية. */
@Composable
private fun <T> LobbyChoiceRow(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    val shape = remember { RoundedCornerShape(MaxRadius.control) }
    val sounds = LocalLobbySounds.current
    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
        Text(text = title, color = LobbyPalette.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
            options.forEach { (value, label) ->
                val on = value == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(ChipHeight)
                        .clip(shape)
                        .background(
                            if (on) LobbyPalette.Red.copy(alpha = 0.28f)
                            else LobbyPalette.PanelRaised.copy(alpha = 0.45f)
                        )
                        .border(
                            MaxSize.hairlineBorder,
                            if (on) LobbyPalette.RedBright else LobbyPalette.Hairline,
                            shape
                        )
                        .selectable(
                            selected = on,
                            role = Role.RadioButton,
                            onClick = {
                                if (!on) sounds.play(LobbyCue.Tap)
                                onSelect(value)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (on) LobbyPalette.RedBright else LobbyPalette.Ink,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** صفّ تبديل بعنوان وشرح اختياريّ؛ الصفّ كلّه هدف اللمس. */
@Composable
private fun LobbyToggleRow(title: String, detail: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val sounds = LocalLobbySounds.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaxSize.minTouchTarget)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = { value ->
                    sounds.play(if (value) LobbyCue.On else LobbyCue.Off)
                    onChange(value)
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
            Text(text = title, color = LobbyPalette.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (detail != null) Text(text = detail, color = LobbyPalette.Steel, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LobbyPalette.Red,
                uncheckedTrackColor = LobbyPalette.PanelRaised,
                uncheckedThumbColor = LobbyPalette.Muted
            )
        )
    }
}
