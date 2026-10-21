/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nd.max.R
import nd.max.ui.component.LobbyPalette
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius

/**
 * صفوف «ملفّ اللعبة» — **مكوّنات الاختيار المشتركة** لمركز التحكم: رقاقة الاختيار نفسها، والحالات
 * الثلاث نفسها (`default|true|false`)، واللوحة اللونية نفسها. كانت خاصةً بنافذة الملفّ وصارت مشتركة.
 *
 * ولا يعرف صفّ الرقاقات **معنى** الاختيار: `selected` قيمة واحدة، ومن أراد تبديلًا يبنيه في موضع النداء.
 */

/** نصف قطر رقاقة الاختيار في صفوف اللوبي — قيمة واحدة للموضعين. */
internal val LobbyChipRadius = 12.dp

/** بطاقة قسم بترويسة — تُستعمل في أقسام مركز التحكم. */
@Composable
internal fun LobbySectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(maxRadius(MaxRadius.group)))
            .background(LobbyPalette.Panel)
            .padding(MaxSpace.lg),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.lg)
    ) {
        Text(text = title, color = LobbyPalette.Ink, fontSize = 19.sp, fontWeight = FontWeight.Medium)
        content()
    }
}

/**
 * صفّ اختيار أفقي بالرقاقات: العنوان بوزن 0.38 والقيم بوزن 0.62 قابلة للتمرير — فلا يُقتطع
 * خيار على شاشة ضيّقة، والمختار وحده يرتدي حافةً وخلفيةً.
 */
@Composable
internal fun LobbyOptionRow(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaxSpace.lg)) {
        Text(
            text = title,
            color = LobbyPalette.Ink,
            fontSize = 16.sp,
            modifier = Modifier.weight(0.38f)
        )
        Row(
            modifier = Modifier
                .weight(0.62f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
        ) {
            options.forEach { (value, label) ->
                val isSelected = value.equals(selected, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .heightIn(min = MaxSize.minTouchTarget)
                        .clip(RoundedCornerShape(LobbyChipRadius))
                        .background(if (isSelected) LobbyPalette.PanelSelected else LobbyPalette.PanelRaised.copy(alpha = 0.55f))
                        .border(
                            MaxSize.hairlineBorder,
                            if (isSelected) LobbyPalette.Hairline else Color.Transparent,
                            RoundedCornerShape(LobbyChipRadius)
                        )
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(value) })
                        .padding(horizontal = MaxSpace.lg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) LobbyPalette.Ink else LobbyPalette.Muted,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** قيمة ثلاثية (الافتراضي/تشغيل/إيقاف) — نفس القيم النصّية `default|true|false` التي يقرؤها المراقب. */
@Composable
internal fun LobbyTriStateRow(title: String, current: String, onSelect: (String) -> Unit) {
    LobbyOptionRow(
        title = title,
        options = listOf(
            "default" to stringResource(R.string.default_label),
            "true" to stringResource(R.string.lobby_state_on),
            "false" to stringResource(R.string.lobby_state_off)
        ),
        selected = current,
        onSelect = onSelect
    )
}

/** مفتاح تشغيل/إيقاف حقيقي (لا قيمة `default`) — لحالة اللوحة الجانبية في الملفّ. */
@Composable
internal fun LobbySwitchRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, color = LobbyPalette.Ink, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(MaxSpace.md))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedTrackColor = LobbyPalette.Red,
                checkedThumbColor = LobbyPalette.Ink,
                uncheckedTrackColor = LobbyPalette.PanelRaised,
                uncheckedThumbColor = LobbyPalette.Muted
            )
        )
    }
}
