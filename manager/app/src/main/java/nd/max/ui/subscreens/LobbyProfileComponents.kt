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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nd.max.R
import nd.max.core.gamespace.LobbyCue
import nd.max.ui.component.LocalLobbySounds
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius
import nd.max.ui.theme.DisplayFontFamily

/**
 * صفوف «ملفّ اللعبة» — **مكوّنات الاختيار المشتركة** لمركز التحكم: رقاقة الاختيار نفسها، والحالات
 * الثلاث نفسها (`default|true|false`)، واللوحة اللونية نفسها. كانت خاصةً بنافذة الملفّ وصارت مشتركة.
 *
 * ولا يعرف صفّ الرقاقات **معنى** الاختيار: `selected` قيمة واحدة، ومن أراد تبديلًا يبنيه في موضع النداء.
 */

/** نصف قطر رقاقة الاختيار في صفوف اللوبي — قيمة واحدة للموضعين. */
internal val LobbyChipRadius = 12.dp

/** حافة البطاقة المذهّبة، ولمعتها العلويّة، وخطّها الفاصل — تدرّجات ثابتة تُبنى مرّة. */
private val SectionSheen = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.05f), 0.30f to Color.Transparent)
private val SectionRim = Brush.linearGradient(
    listOf(LuxGold.copy(alpha = 0.50f), Color.White.copy(alpha = 0.06f), LuxGold.copy(alpha = 0.22f))
)
private val SectionDivider = Brush.horizontalGradient(listOf(LuxGold.copy(alpha = 0.55f), Color.Transparent))

/** بطاقة قسم بترويسة — تُستعمل في أقسام مركز التحكم. */
@Composable
internal fun LobbySectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(maxRadius(MaxRadius.group)))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1B1840).copy(alpha = 0.86f), GameSpacePalette.GlassDeep.copy(alpha = 0.94f))
                )
            )
            .background(SectionSheen)
            .border(MaxSize.hairlineBorder, SectionRim, RoundedCornerShape(maxRadius(MaxRadius.group)))
            .padding(MaxSpace.lg),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(LuxGold, LuxGoldDeep)))
            )
            Text(
                text = title,
                color = LuxIvory,
                fontSize = 17.sp,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.SemiBold
            )
        }
        // خطّ ذهبيّ رفيع يخبو نحو الداخل: لافتة جناح تفصل الترويسة عن المحتوى.
        Box(Modifier.fillMaxWidth().height(MaxSize.hairlineBorder).background(SectionDivider))
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
    val sounds = LocalLobbySounds.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaxSpace.lg)) {
        Text(
            text = title,
            color = GameSpacePalette.Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            modifier = Modifier.weight(0.34f)
        )
        Row(
            modifier = Modifier
                .weight(0.66f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
        ) {
            options.forEach { (value, label) ->
                val isSelected = value.equals(selected, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .heightIn(min = MaxSize.minTouchTarget)
                        .clip(RoundedCornerShape(LobbyChipRadius))
                        .background(if (isSelected) LuxGold.copy(alpha = 0.16f) else GameSpacePalette.PanelRaised.copy(alpha = 0.55f))
                        .border(
                            MaxSize.hairlineBorder,
                            if (isSelected) LuxGold.copy(alpha = 0.80f) else Color.Transparent,
                            RoundedCornerShape(LobbyChipRadius)
                        )
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = {
                                if (!isSelected) sounds.play(LobbyCue.Tap)
                                onSelect(value)
                            }
                        )
                        .padding(horizontal = MaxSpace.lg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) LuxIvory else GameSpacePalette.Muted,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
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
        Text(text = title, color = GameSpacePalette.Ink, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(MaxSpace.md))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedTrackColor = LuxGold,
                checkedThumbColor = LuxObsidian,
                uncheckedTrackColor = GameSpacePalette.PanelRaised,
                uncheckedThumbColor = GameSpacePalette.Muted
            )
        )
    }
}
