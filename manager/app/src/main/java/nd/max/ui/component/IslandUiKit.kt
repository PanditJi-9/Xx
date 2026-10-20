/*
 * Ported from Islandify (https://github.com/bughunter11/Islandify) — MIT License,
 * Copyright (c) 2026 bughunter11. Full license text: THIRD_PARTY_NOTICES.md.
 * Adapted for MaxManager (packages, resources, integration). Modifications (C) 2026 Nader Magdy.
 */
/*
 * عدّة الجزيرة الديناميكية — الرسم المشترك بين شاشة الجزيرة وورقاتها.
 *
 * ### لماذا هذا الملف موجود (`ISLAND-UNIFY-01`)
 *
 * كان الاستوديو نشاطًا مستقلًّا له حِزمه الخاصة (`island/ui/components`, `island/ui/screens`)،
 * وفيه هذه القطع نفسها: أيقونات الأنواع، وعيّنات العرض الحيّة، والعيّنة «النطّاطة»، ومسرح
 * المعاينة. وعند دمج الاستوديو في شاشة الجزيرة داخل MaxManager **لا يجوز أن يعبر المكوّن
 * حدود ملفٍ يُحذف** — فوُضعت القطع المشتركة هنا مرّة واحدة: شاشة الجزيرة تسأل عنها، وورقة
 * النوافذ تسأل عنها، والمعاينة الحيّة تسأل عنها. نسخة ثانية من أيٍّ منها كانت ستعني أن إصلاحًا
 * واحدًا لا يصل بقية السطح.
 *
 * والحدّ المعلن: هذه مكوّنات **هوية الجزيرة** (كبسولتها ونوافذها)، لا مكوّنات عامة — والعام
 * يبقى في `ui/design` (`MaxSection`, `MaxGroup`, `MaxSliderRow`...). وما كان منها في حزمة
 * الاستوديو المحذوفة نُقل هنا حرفًا بحرف حيث أمكن.
 */
package nd.max.ui.component

import androidx.annotation.StringRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.DeliveryDining
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale
import nd.max.R
import nd.max.island.core.IslandKind
import nd.max.island.core.Trigger
import nd.max.island.engine.IslandMode
import nd.max.island.engine.LiveKind
import nd.max.island.engine.ToggleKind
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.maxRadius

/* ---------------------------------------------------------------------------------------------
 * العيّنات الحيّة: ما يُعرض في المعاينة وقائمة الاختبار
 * ------------------------------------------------------------------------------------------- */

/** الأنواع السبعة التي تُعرَض في معاينة الشاشة — من `Demo` في استوديو الجزيرة (نقل Islandify). */
internal enum class IslandDemo(@StringRes val label: Int, val icon: ImageVector) {
    Idle(R.string.ify_demo_idle, Icons.Rounded.RadioButtonUnchecked),
    Notification(R.string.ify_demo_message, Icons.AutoMirrored.Rounded.Chat),
    Media(R.string.ify_demo_music, Icons.Rounded.MusicNote),
    Timer(R.string.ify_demo_timer, Icons.Rounded.Timer),
    Charging(R.string.ify_demo_charging, Icons.Rounded.Bolt),
    Call(R.string.ify_demo_call, Icons.Rounded.Call),
    Device(R.string.ify_demo_earbuds, Icons.Rounded.Headphones),
}

/** محتوى العيّنة المقابل لكل نوع — نصوصها من الموارد (كانت حرفية في الأصل المنقول). */
@Composable
internal fun IslandDemo.toMode(): IslandMode = when (this) {
    IslandDemo.Idle -> IslandMode.Idle
    IslandDemo.Notification -> IslandMode.Notification("WhatsApp", "Rahul", stringResource(R.string.ify_demo_msg))
    IslandDemo.Media -> IslandMode.Media("Blinding Lights", "The Weeknd", true)
    IslandDemo.Timer -> IslandMode.Timer(42, 60)
    IslandDemo.Charging -> IslandMode.Charging(82)
    IslandDemo.Call -> IslandMode.Call("Mummy", true, 0L)
    IslandDemo.Device -> IslandMode.Device("AirPods Pro", true)
}

/* ---------------------------------------------------------------------------------------------
 * الأيقونات: لكل نوع نشاط ولكل نوع نافذة أيقونتها الواحدة
 * ------------------------------------------------------------------------------------------- */

internal fun Trigger.icon(): ImageVector = when (this) {
    Trigger.Notifications -> Icons.Rounded.Notifications
    Trigger.Media -> Icons.Rounded.MusicNote
    Trigger.Calls -> Icons.Rounded.Call
    Trigger.Charging -> Icons.Rounded.Bolt
    Trigger.Timer -> Icons.Rounded.Timer
    Trigger.Devices -> Icons.Rounded.Headphones
    Trigger.Battery -> Icons.Rounded.BatteryAlert
    Trigger.Toggles -> Icons.AutoMirrored.Rounded.VolumeOff
    Trigger.Navigation -> Icons.Rounded.Navigation
    Trigger.Tracking -> Icons.Rounded.DeliveryDining
    Trigger.Downloads -> Icons.Rounded.Download
    Trigger.Quiet -> Icons.Rounded.NotificationsNone
}

/** سطر الشرح تحت كل مفتاح نشاط — ما يعنيه تشغيله بالضبط. */
@StringRes
internal fun Trigger.hint(): Int = when (this) {
    Trigger.Notifications -> R.string.ify_hint_notifications
    Trigger.Media -> R.string.ify_hint_media
    Trigger.Calls -> R.string.ify_hint_calls
    Trigger.Charging -> R.string.ify_hint_charging
    Trigger.Timer -> R.string.ify_hint_timer
    Trigger.Devices -> R.string.ify_hint_devices
    Trigger.Battery -> R.string.ify_hint_battery
    Trigger.Toggles -> R.string.ify_hint_toggles
    Trigger.Navigation -> R.string.ify_hint_navigation
    Trigger.Tracking -> R.string.ify_hint_tracking
    Trigger.Downloads -> R.string.ify_hint_downloads
    Trigger.Quiet -> R.string.ify_hint_quiet
}

internal fun IslandKind.icon(): ImageVector = when (this) {
    IslandKind.Notification -> Icons.Rounded.Notifications
    IslandKind.Charging -> Icons.Rounded.Bolt
    IslandKind.Unplugged -> Icons.Rounded.BatteryStd
    IslandKind.LowBattery -> Icons.Rounded.BatteryAlert
    IslandKind.Device -> Icons.Rounded.Headphones
    IslandKind.Silent -> Icons.AutoMirrored.Rounded.VolumeOff
    IslandKind.Vibrate -> Icons.Rounded.Vibration
    IslandKind.Dnd -> Icons.Rounded.DoNotDisturbOn
    IslandKind.Flashlight -> Icons.Rounded.FlashOn
    IslandKind.Call -> Icons.Rounded.Call
    IslandKind.Media -> Icons.Rounded.MusicNote
    IslandKind.Timer -> Icons.Rounded.Timer
    IslandKind.Navigation -> Icons.Rounded.Navigation
    IslandKind.Tracking -> Icons.Rounded.DeliveryDining
    IslandKind.Download -> Icons.Rounded.Download
}

/** محتوى المعاينة الحيّة داخل ورقة النافذة — لكل نوع عيّنته الخاصة. */
@Composable
internal fun IslandKind.demo(): IslandMode = when (this) {
    IslandKind.Notification -> IslandMode.Notification("WhatsApp", "Rahul", stringResource(R.string.ify_demo_msg))
    IslandKind.Charging -> IslandMode.Charging(82, true)
    IslandKind.Unplugged -> IslandMode.Charging(82, false)
    IslandKind.LowBattery -> IslandMode.LowBattery(15)
    IslandKind.Device -> IslandMode.Device("AirPods Pro", true)
    IslandKind.Silent -> IslandMode.Toggle(ToggleKind.Silent, true)
    IslandKind.Vibrate -> IslandMode.Toggle(ToggleKind.Vibrate, true)
    IslandKind.Dnd -> IslandMode.Toggle(ToggleKind.Dnd, true)
    IslandKind.Flashlight -> IslandMode.Toggle(ToggleKind.Flashlight, true)
    IslandKind.Call -> IslandMode.Call("Mummy", true, 0L)
    IslandKind.Media -> IslandMode.Media("Blinding Lights", "The Weeknd", true)
    IslandKind.Timer -> IslandMode.Timer(42, 60)
    IslandKind.Navigation -> IslandMode.Live(
        LiveKind.Navigation, "Maps",
        stringResource(R.string.ify_demo_nav_instruction), stringResource(R.string.ify_demo_nav_eta)
    )
    IslandKind.Tracking -> IslandMode.Live(
        LiveKind.Tracking, "Zomato",
        stringResource(R.string.ify_demo_order_status), stringResource(R.string.ify_demo_order_eta)
    )
    IslandKind.Download -> IslandMode.Live(
        LiveKind.Download, "Chrome", "movie.mp4", stringResource(R.string.ify_demo_download), 38
    )
}

/**
 * قائمة النوافذ: (عنوان المجموعة، أنواعها) — القائمة نفسها في قسم النوافذ بالشاشة.
 * (نقلًا عن `GROUPS` في استوديو الجزيرة؛ المجموعات الثلاث هي نفسها في قائمة الاختبار.)
 */
internal val ISLAND_KIND_GROUPS: List<Pair<Int, List<IslandKind>>> = listOf(
    R.string.ify_test_group_basic to listOf(
        IslandKind.Notification, IslandKind.Charging, IslandKind.Unplugged, IslandKind.LowBattery,
        IslandKind.Device, IslandKind.Silent, IslandKind.Vibrate, IslandKind.Dnd, IslandKind.Flashlight,
    ),
    R.string.ify_kind_group_island to listOf(
        IslandKind.Call, IslandKind.Media, IslandKind.Timer,
    ),
    R.string.ify_test_group_live to listOf(
        IslandKind.Navigation, IslandKind.Tracking, IslandKind.Download,
    ),
)

/* ---------------------------------------------------------------------------------------------
 * المكوّنات المرسومة
 * ------------------------------------------------------------------------------------------- */

/** شريحة تُضغط فتنطّ قليلًا — زر الأوامر في الجزيرة (نقلًا عن `BouncyChip`). */
@Composable
internal fun BouncyChip(label: String, icon: ImageVector? = null, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "chipPress"
    )
    val leading: (@Composable () -> Unit)? =
        if (icon == null) null else ({ Icon(icon, null, Modifier.size(18.dp)) })
    ElevatedAssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = leading,
        interactionSource = src,
        modifier = modifier.graphicsLayer { scaleX = sc; scaleY = sc }
    )
}

/**
 * مسرح المعاينة: «شاشة الهاتف» الداكنة التي تُعرض فوقها الجزيرة.
 *
 * والتدرّج يأخذ اللون الأساسيّ من `MaterialTheme` المحيط — ولأن النداء يقع داخل `IslandTheme`
 * (نظام ألوان الجزيرة نفسها)، فالشاشة تُعرض بلون الجزيرة لا بلون التطبيق المضيف: المعاينة
 * تصدق لأنها ترسم ما يُرسم فعلًا فوق التطبيقات.
 */
@Composable
internal fun IslandDemoStage(
    height: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), Color(0xFF0B0B0C))
                ),
                RoundedCornerShape(maxRadius(MaxRadius.group))
            ),
        contentAlignment = Alignment.TopCenter,
        content = content
    )
}

/** صورة لون البذرة المخزَّنة بصيغة `#RRGGBB` — ما تفهمه صفوف الألوان المشتركة. */
internal fun accentHexOf(seed: Int): String =
    String.format(Locale.US, "#%06X", seed and 0xFFFFFF)
