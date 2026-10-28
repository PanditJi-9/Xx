/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root.
 *
 * شريط تبويبات شاشة الجزيرة الديناميكية وورقة اختيار إجراء السحب.
 *
 * فُصل هذا من `DynamicIslandScreen.kt` حين تجاوز سقف الألف سطر (`code_health`): الملف
 * الضخم يُفقد التعديل الآمن، والتقسيم يُبقي كل ملف في نافذة قراءة واحدة. والمحتوى هنا
 * عناصر تُرسم فوق الشاشة في الجذر — شريط اختيار التبويب، وورقة إجراء السحب.
 */
package nd.max.ui.subscreens

import android.Manifest
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SettingsApplications
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import nd.max.island.engine.LiveKind
import nd.max.island.engine.ToggleKind
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
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxScreen
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSegmented
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxSliderRow
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxSwitchRow
import nd.max.ui.design.MaxTone
import nd.max.ui.design.maxRadius
import nd.max.ui.util.IslandPrefs
import androidx.compose.material.icons.rounded.Sensors

/**
 * التبويبات الأربعة للجزيرة الديناميكية: الأساس (الرئيسية) — الأنشطة — التصميم (التخصيص) — التطبيقات.
 * تصميم متكامل يطابق استوديو الجزيرة وميزات HyperOS 4 وColorOS 17.
 */
enum class IslandTab(
    @StringRes val label: Int,
    val icon: ImageVector
) {
    CORE(R.string.island_tab_core, Icons.Rounded.PictureInPictureAlt),
    ACTIVITIES(R.string.ify_tab_activities, Icons.Rounded.Sensors),
    DESIGN(R.string.island_tab_design, Icons.Rounded.Tune),
    APPS(R.string.ify_tab_apps, Icons.Rounded.Android);
}

/**
 * شريط التبويبات العائم في أسفل الشاشة — تصميم مطابق لـ AudioStudioTabBar:
 * حبة مستديرة خارج المحتوى، أزرار متساوية العرض، مؤشر متحرك، ظلال ورفع.
 */
@Composable
fun IslandTabBar(
    selectedTab: IslandTab,
    onTabSelect: (IslandTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    // البناء عبر `from(scheme)` لا عبر المنشئ: قيم المنشئ الافتراضية كانت تقرأ
    // `MaterialTheme.colorScheme` — وهي قراءة `@Composable` في توقيع غير `@Composable`،
    // فلا تُصرّف (رفضها المُصرّف في QV: «Functions which invoke @Composable functions…»).
    val colors = IslandTabBarColors.from(scheme)

    // عرض كل تبويب كنسبة متساوية من العرض المتاح
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            .navigationBarsPadding()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(maxRadius(MaxRadius.sheet)),
            color = colors.container,
            shadowElevation = 8.dp,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IslandTab.entries.forEachIndexed { index, tab ->
                    val isSelected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Tab
                            ) {
                                onTabSelect(tab)
                            }
                            .semantics {
                                this.selected = isSelected
                                this.role = Role.Tab
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = stringResource(tab.label),
                                tint = if (isSelected) colors.selectedText else colors.unselectedText,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(tab.label),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) colors.selectedText else colors.unselectedText,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

/** ألوان شريط التبويبات العائم — متوافقة مع Material3 Surface/Container. */
private data class IslandTabBarColors(
    val container: Color,
    val indicator: Color,
    val selectedText: Color,
    val unselectedText: Color
) {
    companion object {
        /** وكلّها من المخطّط مرة واحدة — فالمُنشئ لم يبقَ فيه ما يقرأ `MaterialTheme`. */
        fun from(scheme: ColorScheme): IslandTabBarColors = IslandTabBarColors(
            container = scheme.surfaceContainerHigh,
            indicator = scheme.primary,
            selectedText = scheme.primary,
            unselectedText = scheme.onSurfaceVariant
        )
    }
}

/**
 * مُنتقي إجراء السحب: ورقة صغيرة بصفوف اختيار واحد لكل اتجاه.
 * والحالة والإجراء يصلان من الشاشة لا يُقرآن هنا: الشاشة تملك تدفّقات
 * `swipeUp/Down/Left/Right` الأربعة أصلًا، فالورقة عرضٌ خالص لا مصدر حقيقة ثانٍ.
 */
@Composable
internal fun SwipeActionSheet(
    dir: SwipeDir?,
    current: SwipeAction,
    onSet: (SwipeAction) -> Unit,
    onClose: () -> Unit
) {
    CustomBottomSheet(visible = dir != null, onDismiss = onClose) {
        if (dir == null) return@CustomBottomSheet
        val title = when (dir) {
            SwipeDir.Up -> stringResource(R.string.ify_cust_swipe_up)
            SwipeDir.Down -> stringResource(R.string.ify_cust_swipe_down)
            SwipeDir.Left -> stringResource(R.string.ify_cust_swipe_left)
            SwipeDir.Right -> stringResource(R.string.ify_cust_swipe_right)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaxSpace.xl)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = MaxSpace.md, bottom = MaxSpace.sm)
            )
            SwipeAction.entries.forEach { a ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = a == current,
                            role = Role.RadioButton,
                            onClick = {
                                onSet(a)
                                onClose()
                            }
                        )
                        .padding(vertical = MaxSpace.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = a == current, onClick = null)
                    Spacer(Modifier.width(MaxSpace.md))
                    Text(text = stringResource(a.label), style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(MaxSpace.lg))
        }
    }
}
