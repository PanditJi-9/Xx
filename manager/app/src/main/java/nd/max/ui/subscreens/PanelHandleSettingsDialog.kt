/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import nd.max.R
import nd.max.core.gamespace.HandleSize
import nd.max.core.gamespace.PanelSensitivity
import nd.max.core.gamespace.PanelSide
import nd.max.ui.component.LobbyPalette
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.util.GamePanelPrefs

private val SheetMaxWidth: Dp = 560.dp
private val ChipHeight: Dp = 48.dp

/**
 * إعدادات مقبض اللوحة الجانبية: الحافة (يسار/يمين) والحجم وحساسية السحب للداخل وقفل الموضع والاهتزاز،
 * مع إعادة الضبط. **كل تغيير يُحفظ في لحظته** والخدمة تلتقطه فورًا (مستمع التفضيلات)، فلا زرّ «حفظ» ولا
 * إعادة تشغيل. وسحب المقبض نفسه بالضغط المطوّل يغيّر الحافة والارتفاع بلا مرور من هنا (انظر الملاحظة أسفل الورقة).
 */
@Composable
internal fun PanelHandleSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var prefs by remember { mutableStateOf(GamePanelPrefs.load(context)) }
    val reload = { prefs = GamePanelPrefs.load(context) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = SheetMaxWidth)
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(topStart = MaxRadius.sheet, topEnd = MaxRadius.sheet))
                    .background(LobbyPalette.Surface)
                    // يبتلع اللمس حتى لا تُغلق الورقة من داخلها.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = MaxSpace.lg, vertical = MaxSpace.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.panel_handle_title),
                        color = LobbyPalette.Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(ChipHeight)
                            .clip(RoundedCornerShape(MaxRadius.pill))
                            .clickable(role = Role.Button, onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.panel_handle_done),
                            tint = LobbyPalette.Ink
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = MaxSpace.lg),
                    verticalArrangement = Arrangement.spacedBy(MaxSpace.lg)
                ) {
                    // يمين/يسار **فيزيائيّان**: الصفّ مثبَّت LTR كي لا ينعكس ترتيبه في RTL فيخالف الحافة المرسومة.
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        ChoiceRow(
                            title = stringResource(R.string.panel_handle_edge),
                            options = listOf(
                                PanelSide.Start to stringResource(R.string.panel_handle_left),
                                PanelSide.End to stringResource(R.string.panel_handle_right)
                            ),
                            selected = prefs.side,
                            onSelect = { GamePanelPrefs.saveSide(context, it); reload() }
                        )
                    }
                    ChoiceRow(
                        title = stringResource(R.string.panel_handle_size),
                        options = listOf(
                            HandleSize.Small to stringResource(R.string.panel_handle_size_small),
                            HandleSize.Medium to stringResource(R.string.panel_handle_size_medium),
                            HandleSize.Large to stringResource(R.string.panel_handle_size_large)
                        ),
                        selected = prefs.handleSize,
                        onSelect = { GamePanelPrefs.saveHandleSize(context, it); reload() }
                    )
                    ChoiceRow(
                        title = stringResource(R.string.panel_handle_sensitivity),
                        options = listOf(
                            PanelSensitivity.Low to stringResource(R.string.panel_handle_sens_low),
                            PanelSensitivity.Balanced to stringResource(R.string.panel_handle_sens_balanced),
                            PanelSensitivity.High to stringResource(R.string.panel_handle_sens_high)
                        ),
                        selected = prefs.sensitivity,
                        onSelect = { GamePanelPrefs.saveSensitivity(context, it); reload() }
                    )
                    ToggleRow(
                        title = stringResource(R.string.panel_handle_lock),
                        detail = stringResource(R.string.panel_handle_lock_desc),
                        checked = prefs.locked,
                        onChange = { GamePanelPrefs.saveLocked(context, it); reload() }
                    )
                    ToggleRow(
                        title = stringResource(R.string.panel_handle_haptics),
                        detail = null,
                        checked = prefs.haptics,
                        onChange = { GamePanelPrefs.saveHaptics(context, it); reload() }
                    )
                    Text(
                        text = stringResource(R.string.panel_handle_hint),
                        color = LobbyPalette.Steel,
                        fontSize = 12.sp
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(MaxSpace.lg),
                    horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { GamePanelPrefs.resetHandle(context); reload() },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.panel_handle_reset), color = LobbyPalette.Ink) }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = LobbyPalette.Red, contentColor = Color.White)
                    ) { Text(stringResource(R.string.panel_handle_done)) }
                }
            }
        }
    }
}

/** صفّ خيارات متساوية العرض؛ المحدَّد بلون اللوبي الأحمر. القيمة المعروضة تتبع [selected] لا حالة محلية. */
@Composable
private fun <T> ChoiceRow(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val shape = remember { RoundedCornerShape(MaxRadius.control) }
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
                        .background(if (on) LobbyPalette.Red.copy(alpha = 0.28f) else LobbyPalette.PanelRaised.copy(alpha = 0.45f))
                        .border(MaxSize.hairlineBorder, if (on) LobbyPalette.RedBright else LobbyPalette.Hairline, shape)
                        .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(value) }),
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

@Composable
private fun ToggleRow(title: String, detail: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
            Text(text = title, color = LobbyPalette.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (detail != null) Text(text = detail, color = LobbyPalette.Steel, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LobbyPalette.Red
            )
        )
    }
}
