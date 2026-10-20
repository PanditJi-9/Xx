/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package nd.max.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import nd.max.R
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace

/** ألوان الأرقام المُقترحة. والاسم مرافق للون لأنّ اللون وحده لا يُنطق. */
val HUD_ACCENTS: List<Pair<String, Int>> = listOf(
    "#00E676" to R.string.max_fps_color_green,
    "#00E5FF" to R.string.max_fps_color_cyan,
    "#FFEA00" to R.string.max_fps_color_yellow,
    "#FF6D00" to R.string.max_fps_color_orange,
    "#FF1744" to R.string.max_fps_color_red,
    "#FFFFFF" to R.string.max_fps_color_white
)

/**
 * ألوان الجزيرة الديناميكية: **الأسود أوّلًا** لأنه الافتراضيّ ولأنه ما تطلبه رومات المصانع،
 * ثم ألوان اللوحة الستّة نفسها.
 *
 * والقائمة تبدأ بالأسود ولا تحذف شيئًا من [HUD_ACCENTS]: من اعتاد كبسولة ملوّنة وجدها، ومن
 * جاء يريد أسود المصنع وجده أوّل الصفّ بلمسة واحدة — بدل أن يمرّ على ستّة ألوان ليعود إلى الأصل.
 */
val ISLAND_ACCENTS: List<Pair<String, Int>> = listOf(
    "#000000" to R.string.max_fps_color_black
) + HUD_ACCENTS

/** تحليل لون سِتّ عشريّ — نصّ لا يُقرأ يعود إلى الأصل، لا إلى لون عشوائيّ. */
fun parseAccentHex(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color(0xFF00E676))

/**
 * صفّ ألوان اللون المُميِّز — **مشترك بين لوحة الأداء والجزيرة الديناميكية**.
 *
 * ### ولماذا استُخرج من شاشة اللوحة (`DYNAMIC-ISLAND-01`)
 *
 * كان `HudAccentRow` خاصًّا بشاشة لوحة الأداء: الألوان الستّة وقائمة أسمائها ومنطق الاختيار
 * (نقطة ملوّنة + علامة صحّ + `selectable` بقارئ شاشة) سطرًا بسطر. والجزيرة الديناميكية تسأل
 * **السؤال نفسه**: «ما لون الكبسولة؟» — والقائمتان نفساهما، لأن اللونين ليسا لونَي شاشة بل
 * **لونَي قراءة**: ما يُقرأ فوق لعبة يجب أن يبقى مقروءًا. فنسخة ثانية كانت ستعني أن إصلاحًا
 * في تباين لون لا يصل نصف المستخدمين.
 *
 * والحدّ المعلن: المفتاح الموصوف (`fps_overlay_accent_color`) وصفٌ عامّ — «Accent color» —
 * يُعاد استعماله بدل تكرار نصّ جديد بنفس المعنى.
 *
 * @param selectedHex اللون المختار بصورته المخزّنة (`#RRGGBB`)، والمقارنة بلا حساسية لحالة الأحرف.
 * @param onSelect يُنادى بالصورة المخزّنة لا بمعرّف الصفّ — فما يُحفظ هو ما يُقرأ.
 * @param colors قائمة الألوان المعروضة. الافتراضيّ ألوان اللوحة، وشاشة الجزيرة تمرّر
 *   [ISLAND_ACCENTS] (الأسود أوّلًا). والقائمة معامل لا نسخة ثانية من المكوّن: منطق الاختيار
 *   وعلامة الصحّ وقارئ الشاشة واحد في الحالتين.
 */
@Composable
fun AccentSwatches(
    selectedHex: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Pair<String, Int>> = HUD_ACCENTS
) {
    val selectedLabel = stringResource(R.string.max_fps_color_selected)
    val unselectedLabel = stringResource(R.string.max_fps_color_not_selected)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Text(
            text = stringResource(R.string.fps_overlay_accent_color),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MaxSpace.groupPadding)
        )
        // صفّ يتلفّف لا صفّ يمتدّ: سبعة ألوان (بعد ضمّ الأسود) تتجاوز عرض هاتف ضيّق، وكان
        // الصفّ الواحد سيقصّ آخر لون منها على جهاز ٣٢٠dp بلا أن يظهر نقص.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            colors.forEach { (hex, nameRes) ->
                val name = stringResource(nameRes)
                val selected = hex.equals(selectedHex, ignoreCase = true)
                val swatch = remember(hex) { parseAccentHex(hex) }
                // علامة تُرى على الأبيض كما على الأسود.
                val markColor = if (swatch.luminance() > 0.5f) Color.Black else Color.White

                Box(
                    modifier = Modifier
                        .size(MaxSize.minTouchTarget)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(hex) }
                        )
                        .semantics {
                            contentDescription = name
                            stateDescription = if (selected) selectedLabel else unselectedLabel
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(MaxSize.rowIconContainer)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(
                                width = if (selected) MaxSpace.hairline else MaxSize.hairlineBorder,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = markColor,
                                modifier = Modifier.size(MaxSize.iconGlyphSmall)
                            )
                        }
                    }
                }
            }
        }
    }
}
