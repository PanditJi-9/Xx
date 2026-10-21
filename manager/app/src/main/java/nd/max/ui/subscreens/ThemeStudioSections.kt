@file:OptIn(ExperimentalMaterial3Api::class)
/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.ui.subscreens

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.RoundedCorner
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import nd.max.R
import nd.max.core.platform.EventLog
import nd.max.ui.component.ExpressiveSwitchItem
import nd.max.ui.component.StudioOutlinedButton
import nd.max.ui.design.MaxAlpha
import nd.max.ui.design.MaxCardStyle
import nd.max.ui.design.MaxChoiceRow
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxBleed
import nd.max.ui.design.maxEdgeFade
import nd.max.ui.design.maxRadius
import nd.max.ui.theme.ColorMode
import nd.max.ui.theme.NavBarStyle
import nd.max.ui.theme.NavLabelMode
import nd.max.ui.theme.ThemeContrast
import nd.max.ui.theme.ThemeDensity
import nd.max.ui.theme.ThemeFontScale
import nd.max.ui.theme.ThemeKeys
import nd.max.ui.theme.ThemeMotion
import nd.max.ui.theme.ThemePreset
import nd.max.ui.theme.ThemeRoundness
import nd.max.ui.theme.ThemeStudio
import nd.max.ui.theme.ThemeStudioPresets
import nd.max.ui.theme.TopBarStyle

/*
 * أقسام «استوديو السمة» الجديدة — الأنماط الجاهزة، والتخصيص المتقدّم، وواجهة الحركة.
 *
 * وُجد هذا الملف منفصلًا لقاعدة واحدة مقيسة: شاشة السمة (‏`CustomThemeScreen.kt`) تتجاوز
 * ١٣٠٠ سطر، وكل قسم جديد يُضاف داخلها يزيد كتلة يستحيل مراجعتها. فالقسم هنا مدخل واحد
 * (`ThemePresetsRow` · `ThemeAdvancedSection` · `ThemeInterfaceControls` · `ThemeResetRow`)
 * والكتابة في التفضيلات تمرّ من نقطة واحدة ([persistChoice]) فلا يتفرّق المفتاح عن حدثه.
 *
 * وكل الحشوات والزوايا من رموز `MaxSpace`/`MaxRadius` — بوابة رموز التصميم تحرس هذا الملف
 * كما تحرس غيره، ولا حرفيّ جديد يدخل.
 */

// ─── الأنماط الجاهزة ─────────────────────────────────────────────────────────

/**
 * صفّ الأنماط الستّة: بطاقة لكل نمط تُرسم **بألوانه هو** (بذرته ومظهره وتباينه الجارِيّين)
 * فتكون المقارنة بالنظر لا بالأسماء، ويُعلَّم المطبَّق بإطار وخلفية الاسم.
 */
@Composable
fun ThemePresetsRow(
    colorSpec: ColorSpec.SpecVersion,
    isSystemDark: Boolean,
    selectedKeyColor: Int,
    selectedMode: ColorMode,
    selectedContrast: ThemeContrast,
    selectedRoundness: ThemeRoundness,
    swatchStartBleed: Dp = MaxSpace.gutter,
    onApply: (ThemePreset) -> Unit,
) {
    LazyRow(
        // الامتداد إلى الحافة والتلاشي عند الطرفين كما في صفّ الشرائح أعلاه: العين تفهم
        // أنّ هناك المزيد من غير سهم ولا مؤشّر.
        modifier = Modifier
            .fillMaxWidth()
            .maxBleed(start = swatchStartBleed, end = MaxSpace.gutter)
            .maxEdgeFade(),
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
        contentPadding = PaddingValues(horizontal = MaxSpace.gutter),
    ) {
        items(items = ThemeStudioPresets, key = { it.id }) { preset ->
            ThemePresetCard(
                preset = preset,
                colorSpec = colorSpec,
                isSystemDark = isSystemDark,
                isSelected = preset.matches(
                    keyColor = selectedKeyColor,
                    colorMode = selectedMode,
                    contrast = selectedContrast,
                    roundness = selectedRoundness,
                ),
                onClick = { onApply(preset) },
            )
        }
    }
}

@Composable
private fun ThemePresetCard(
    preset: ThemePreset,
    colorSpec: ColorSpec.SpecVersion,
    isSystemDark: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    // ألوان النمط نفسه لا ألوان السمة الحالية: البطاقة تُري ما ستؤول إليه الشاشة عند
    // الضغط — فرق الظلّ بين «عنوان بلون» و«شاشة بلون» هو فرق بين وعد وقياس.
    val scheme = rememberDynamicColorScheme(
        seedColor = Color(preset.seed),
        isDark = preset.mode.getDarkThemeValue(isSystemDark),
        isAmoled = preset.mode == ColorMode.DARKAMOLED,
        specVersion = colorSpec,
        contrastLevel = preset.contrast.level,
    )
    // ودرجة الاستدارة في الرسم هي درجة النمط نفسها (لا الجارية): من لم يجرب «حادّة» في
    // التطبيق يراها في بطاقتها قبل أن يضغط.
    val cardShape = RoundedCornerShape(MaxRadius.control * preset.roundness.scale)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(80.dp),
            shape = cardShape,
            color = scheme.surfaceContainerHigh,
            border = BorderStroke(
                width = if (isSelected) MaxSpace.hairline else MaxSize.hairlineBorder,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = MaxAlpha.borderStrong)
                },
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MaxSpace.sm)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = MaxSpace.xxl, height = MaxSpace.sm)
                                .clip(cardShape)
                                .background(scheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .size(MaxSpace.md)
                                .clip(CircleShape)
                                .background(scheme.tertiary)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MaxSpace.xl)
                            .clip(cardShape)
                            .background(scheme.secondaryContainer)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MaxSpace.md)
                            .clip(cardShape)
                            .background(scheme.primaryContainer)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(cardShape)
                            .background(scheme.surfaceVariant.copy(alpha = MaxAlpha.borderStrong))
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(MaxSize.iconGlyphSmall)
                    )
                }
            }
        }
        Text(
            text = stringResource(preset.nameRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─── صفّ اختيار (تسمية + أزرار مقسّمة) ───────────────────────────────────────

/**
 * صفّ اختيار واحد: أيقونة وعنوان بمستوى ثانويّ (خانة التخصيص المتقدّم)، ثم أزرار مقسّمة
 * **نصّية** — التسمية نفسها هي الشرح في اختيارات من كلمتين، والأيقونة على كل زرّ كانت
 * ستزيد الضجيج لا البيان.
 */
@Composable
private fun ThemeChoiceRow(
    icon: ImageVector,
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    colors = SegmentedButtonDefaults.colors(
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
    }
}

// ─── التخصيص المتقدّم (مطويّ افتراضيًّا) ─────────────────────────────────────

/**
 * «تخصيص متقدّم» — قسم واحد قابل للفتح يحمل التفاصيل الدقيقة (مواصفة الألوان · التباين ·
 * الاستدارة · حجم النص) بعيدًا عن العين المبتدئة وبقريب من اليد إن طُلبت.
 *
 * **ولماذا مطويّ افتراضيًّا:** الصفحة كانت تعرض التفاصيل كلها دفعةً؛ وصارت تُعرض الآن:
 * المعاينة والأنماط واللون والمظهر مباشرةً، ثم هذه البوّابة. ومن فتحها وجد خيارات حقيقية
 * لا حشوًا — كل واحدة تُغيّر التطبيق فعلًا وتُحفَظ.
 */
@Composable
fun ThemeAdvancedSection(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    currentColorSpec: ColorSpec.SpecVersion,
    onColorSpecChange: (ColorSpec.SpecVersion) -> Unit,
    currentContrast: ThemeContrast,
    onContrastChange: (ThemeContrast) -> Unit,
    currentRoundness: ThemeRoundness,
    onRoundnessChange: (ThemeRoundness) -> Unit,
    currentFontScale: ThemeFontScale,
    onFontScaleChange: (ThemeFontScale) -> Unit,
    currentDensity: ThemeDensity,
    onDensityChange: (ThemeDensity) -> Unit,
    currentCardStyle: MaxCardStyle,
    onCardStyleChange: (MaxCardStyle) -> Unit,
    prefs: SharedPreferences,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(MaxDuration.standard),
        label = "advanced_chevron"
    )

    Column {
        Surface(
            onClick = { onExpandedChange(!expanded) },
            color = Color.Transparent,
            shape = RoundedCornerShape(maxRadius(MaxRadius.row)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaxSpace.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(MaxSize.iconGlyphSmall)
                )
                Text(
                    text = stringResource(R.string.theme_advanced_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.theme_advanced_hide else R.string.theme_advanced_show
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(MaxSize.iconGlyph)
                        .rotate(rotation)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(320)) + fadeIn(tween(320)),
            exit = shrinkVertically(tween(320)) + fadeOut(tween(320))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaxSpace.md),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.xl)
            ) {
                // مواصفة الألوان — الصفّ الموجود نفسه بترتيبه وأيقوناته (لم يُحذف شيء،
                // نُقل إلى هنا لأنه تفصيل تقنيّ لا خيار يوميّ).
                ThemeSpecChoice(
                    currentColorSpec = currentColorSpec,
                    onColorSpecChange = onColorSpecChange,
                    prefs = prefs
                )

                ThemeChoiceRow(
                    icon = Icons.Outlined.Contrast,
                    title = stringResource(R.string.theme_contrast_title),
                    options = listOf(
                        stringResource(R.string.theme_opt_standard),
                        stringResource(R.string.theme_contrast_medium),
                        stringResource(R.string.theme_contrast_high),
                    ),
                    selectedIndex = currentContrast.ordinal,
                    onSelect = { index ->
                        val next = ThemeContrast.entries[index]
                        persistChoice(prefs, ThemeKeys.CONTRAST, currentContrast.key, next.key)
                        onContrastChange(next)
                    }
                )

                ThemeChoiceRow(
                    icon = Icons.Outlined.RoundedCorner,
                    title = stringResource(R.string.theme_roundness_title),
                    options = listOf(
                        stringResource(R.string.theme_roundness_sharp),
                        stringResource(R.string.theme_roundness_standard),
                        stringResource(R.string.theme_roundness_round),
                    ),
                    selectedIndex = currentRoundness.ordinal,
                    onSelect = { index ->
                        val next = ThemeRoundness.entries[index]
                        persistChoice(prefs, ThemeKeys.ROUNDNESS, currentRoundness.key, next.key)
                        onRoundnessChange(next)
                    }
                )

                ThemeChoiceRow(
                    icon = Icons.Outlined.FormatSize,
                    title = stringResource(R.string.theme_font_title),
                    options = listOf(
                        stringResource(R.string.theme_font_small),
                        stringResource(R.string.theme_opt_standard),
                        stringResource(R.string.theme_font_large),
                    ),
                    selectedIndex = currentFontScale.ordinal,
                    onSelect = { index ->
                        val next = ThemeFontScale.entries[index]
                        persistChoice(prefs, ThemeKeys.FONT_SCALE, currentFontScale.key, next.key)
                        onFontScaleChange(next)
                    }
                )

                // كثافة المعلومات (§٦-ج): تضغط الحشو الرأسي أو توسّعه — ولا تمسّ حجم النصّ ولا
                // أرض اللمس. والافتراضيّ «قياسيّ» = سلوك اليوم بالضبط.
                ThemeChoiceRow(
                    icon = Icons.Outlined.ViewAgenda,
                    title = stringResource(R.string.theme_density_title),
                    options = listOf(
                        stringResource(R.string.theme_density_compact),
                        stringResource(R.string.theme_opt_standard),
                        stringResource(R.string.theme_density_spacious),
                    ),
                    selectedIndex = currentDensity.ordinal,
                    onSelect = { index ->
                        val next = ThemeDensity.entries[index]
                        persistChoice(prefs, ThemeKeys.DENSITY, currentDensity.key, next.key)
                        onDensityChange(next)
                    }
                )

                // شكل البطاقات (§٦-ج): القشرة وحدها (حدّ/خلفية) — والمحتوى لا يتغيّر في أيّ وضع.
                ThemeChoiceRow(
                    icon = Icons.Outlined.SpaceDashboard,
                    title = stringResource(R.string.theme_card_style_title),
                    options = listOf(
                        stringResource(R.string.theme_card_outlined),
                        stringResource(R.string.theme_card_filled),
                        stringResource(R.string.theme_card_flat),
                    ),
                    selectedIndex = currentCardStyle.ordinal,
                    onSelect = { index ->
                        val next = MaxCardStyle.entries[index]
                        persistChoice(prefs, ThemeKeys.CARD_STYLE, currentCardStyle.key, next.key)
                        onCardStyleChange(next)
                    }
                )
            }
        }
    }
}

/** صفّ مواصفة الألوان — أيقونة لكل مواصفة والتمييز بترتيبها لا باسمها (كما كان). */
@Composable
private fun ThemeSpecChoice(
    currentColorSpec: ColorSpec.SpecVersion,
    onColorSpecChange: (ColorSpec.SpecVersion) -> Unit,
    prefs: SharedPreferences,
) {
    val specOptions = ColorSpec.SpecVersion.entries

    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
        ) {
            Icon(
                imageVector = Icons.Outlined.Colorize,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
            Text(
                text = stringResource(R.string.str_color_specification),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            specOptions.forEachIndexed { index, spec ->
                SegmentedButton(
                    selected = currentColorSpec == spec,
                    onClick = {
                        onColorSpecChange(spec)
                        persistChoice(prefs, "color_spec", currentColorSpec.name, spec.name)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = specOptions.size),
                    colors = SegmentedButtonDefaults.colors(
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    icon = {
                        Icon(
                            imageVector = when (index) {
                                0 -> Icons.Outlined.Science
                                1 -> Icons.Outlined.Colorize
                                else -> Icons.Outlined.Tune
                            },
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = spec.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
    }
}

// ─── الواجهة: شكل الشريط والحركة ─────────────────────────────────────────────

/**
 * اسم نمط الشريط السفلي ووصفه — **من الموارد لا من محفور**، والتغطية (خمسة أنماط × لغتين)
 * يقيسها `NavBarStyleTest`: كل نمط له نصّان، ولا نصّان لعنصر واحد. والدالّتان هنا (لا في
 * `MaxNavBar`) لأنّ الاستوديو هو من يعرضهما، والقائمة نفسها تُبنى من `NavBarStyle.entries`.
 */
internal fun NavBarStyle.labelRes(): Int = when (this) {
    NavBarStyle.FLOATING -> R.string.theme_nav_floating
    NavBarStyle.DOCKED -> R.string.theme_nav_docked
    NavBarStyle.COMPACT -> R.string.theme_nav_compact
    NavBarStyle.LABELED -> R.string.theme_nav_labeled
    NavBarStyle.LARGE -> R.string.theme_nav_large
    NavBarStyle.ADAPTIVE -> R.string.theme_nav_adaptive
}

/** وصف سطرٍ واحد يشرح البنية — لأنّ الاسم وحده («ممتدّ») لا يقول ماذا يتمدّد. */
internal fun NavBarStyle.descriptionRes(): Int = when (this) {
    NavBarStyle.FLOATING -> R.string.theme_nav_floating_desc
    NavBarStyle.DOCKED -> R.string.theme_nav_docked_desc
    NavBarStyle.COMPACT -> R.string.theme_nav_compact_desc
    NavBarStyle.LABELED -> R.string.theme_nav_labeled_desc
    NavBarStyle.LARGE -> R.string.theme_nav_large_desc
    NavBarStyle.ADAPTIVE -> R.string.theme_nav_adaptive_desc
}

/**
 * اختيارات «الواجهة» الجديدة — بنية الشريط العلوي، ثمّ بنية شريط التنقل (خمسة أنماط مستقلّة)،
 * ثمّ مستوى الحركة. تُعرض مباشرةً (لا في القسم المطويّ) لأنها تُرى وتُلمس في كل تنقّل، لا تفصيلًا
 * نادرًا.
 */
@Composable
fun ThemeInterfaceControls(
    currentTopBar: TopBarStyle,
    onTopBarChange: (TopBarStyle) -> Unit,
    currentNavStyle: NavBarStyle,
    onNavStyleChange: (NavBarStyle) -> Unit,
    currentMotion: ThemeMotion,
    onMotionChange: (ThemeMotion) -> Unit,
    prefs: SharedPreferences,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.xl)) {
        // ── بنية الشريط العلوي (§٦-أ) ───────────────────────────────────────
        // **أنماطها تختلف في البنية لا في اللون:** أيقونة (صفّ ثابت وأيقونة الشاشة بدل الرجوع) ·
        // عنوان (اسم الشاشة وحده) · شفّاف (تدرّج فوق المحتوى) — والكلاسيكيّ هو القائم. والمفتاح هنا
        // يكتب النمط، والشريط في `ScreenChrome` يقرؤه، والمعاينة أعلاه ترسم به.
        ThemeChoiceRow(
            icon = Icons.Outlined.ViewAgenda,
            title = stringResource(R.string.theme_topbar_title),
            options = listOf(
                stringResource(R.string.theme_topbar_classic),
                stringResource(R.string.theme_topbar_icon),
                stringResource(R.string.theme_topbar_title_only),
                stringResource(R.string.theme_topbar_translucent),
            ),
            selectedIndex = currentTopBar.ordinal,
            onSelect = { index ->
                val next = TopBarStyle.entries[index]
                persistChoice(prefs, ThemeKeys.TOP_BAR, currentTopBar.key, next.key)
                onTopBarChange(next)
            }
        )

        // ── نمط الشريط السفلي (§٦-ب) — ستّة تصاميم مستقلة ────────────────────
        // كان الخياران السابقان صفًّا مقسومًا (`SegmentedButton`)، وبالستّة يضيق الصفّ على
        // الشاشات الصغيرة وترثّ الأسماء الطويلة في RTL — فصار قائمة اختيار عمودية: كل نمط باسمه
        // ووصفه، والراديو يقول أيّها الجاري. والقائمة من `NavBarStyle.entries` نفسها، فلا قائمة
        // موازية تُنسى إن زاد نمط. والكتابة عبر `persistChoice` فتبقى «اختيار واحد = مفتاح واحد».
        Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
            ) {
                Icon(
                    imageVector = Icons.Outlined.SpaceDashboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(MaxSize.iconGlyphSmall)
                )
                Text(
                    text = stringResource(R.string.theme_nav_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            MaxGroup {
                NavBarStyle.entries.forEach { entry ->
                    MaxChoiceRow(
                        title = stringResource(entry.labelRes()),
                        subtitle = stringResource(entry.descriptionRes()),
                        selected = entry == currentNavStyle,
                        onSelect = {
                            persistChoice(prefs, ThemeKeys.NAV_BAR_STYLE, currentNavStyle.key, entry.key)
                            onNavStyleChange(entry)
                        }
                    )
                }
            }
        }

        ThemeChoiceRow(
            icon = Icons.Outlined.Animation,
            title = stringResource(R.string.theme_motion_title),
            options = listOf(
                stringResource(R.string.theme_motion_full),
                stringResource(R.string.theme_motion_calm),
                stringResource(R.string.theme_motion_none),
            ),
            selectedIndex = currentMotion.ordinal,
            onSelect = { index ->
                val next = ThemeMotion.entries[index]
                persistChoice(prefs, ThemeKeys.MOTION, currentMotion.key, next.key)
                onMotionChange(next)
            }
        )

        // ── الظهور التدريجي ─────────────────────────────────────────────────
        // الاسم المعروض «الظهور التدريجي»، أمّا المفتاح `SCROLL_EFFECTS` فيبقى كما هو: تغييره
        // يُصفّر تفضيل من أطفأ التأثير، والاسم الداخلي لا يظهر لأحد.
        // **مفعّلة افتراضيًّا، ومعطّلة بمفتاح واحد** (معيار القبول ٨): المفتاح هنا، والباب الذي
        // تمرّ به كل المواضع المربوطة `rememberScrollEffectsEnabled` في `MaxMotion` — فمن
        // أطفأها أُوقف الكشف في كل مكان، لا في شاشة دون أخرى. والمحتوى يُعرض فورًا كاملًا،
        // وإيقاف الحركة في النظام (`ANIMATOR_DURATION_SCALE = 0`) يسبق هذا المفتاح ويُلغيه.
        var scrollEffects by remember { mutableStateOf(ThemeStudio.scrollEffects(prefs)) }
        ExpressiveSwitchItem(
            icon = Icons.Outlined.Animation,
            title = stringResource(R.string.str_scroll_effects),
            checked = scrollEffects,
            onCheckedChange = { next ->
                scrollEffects = next
                persistFlag(prefs, ThemeKeys.SCROLL_EFFECTS, next)
            }
        )
    }
}

// ─── إعادة الضبط ─────────────────────────────────────────────────────────────

/** زرّ «إعادة ضبط السمة» — في ذيل الصفحة، بلا صخب، وبخلف تأكيدٍ قبل أن يمسّ شيئًا. */
@Composable
fun ThemeResetRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StudioOutlinedButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Filled.Restore,
                contentDescription = null,
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
            Spacer(Modifier.width(MaxSpace.sm))
            Text(
                text = stringResource(R.string.theme_reset_action),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── كتابة الاختيار ──────────────────────────────────────────────────────────

/**
 * نقطة الكتابة الواحدة لاختيارات الاستوديو: حدثٌ ثم تفضيل — بنفس ترتيب أقسام الشاشة
 * القائمة (`EventLog` أولًا ثم `prefs.edit`) حتى لا يبقى اختيار بلا أثر في السجلّ.
 * والقيمة نفسها لا تُكتب مرّتين.
 */
private fun persistChoice(prefs: SharedPreferences, key: String, old: String, new: String) {
    if (old == new) return
    EventLog.userAction(screen = "CustomTheme", field = key, old = old, new = new.toString())
    prefs.edit { putString(key, new) }
}

/**
 * كتابة مفتاح بوليانيّ (الظهور التدريجي · وإخفاء الميزات غير المتاحة §٤-و من شاشة الإعدادات) —
 * بنفس ترتيب [persistChoice]: حدثٌ ثم تفضيل. والقيمة نفسها لا تُكتب مرّتين، ولا يُكتب مفتاحٌ لم يتغيّر.
 * و`default` هو افتراضيّ المفتاح نفسه: مفتاحٌ افتراضيّه «مفعّل» ومفتاحٌ افتراضيّه «مطفأ»
 * لا يُقرآن بالافتراضيّ نفسه، وإلا كُتب «تغيير» لم يقع. و`screen` اسم الشاشة في سجلّ الأحداث.
 */
internal fun persistFlag(
    prefs: SharedPreferences,
    key: String,
    new: Boolean,
    default: Boolean = true,
    screen: String = "CustomTheme",
) {
    val old = prefs.getBoolean(key, default)
    if (old == new) return
    EventLog.userAction(screen = screen, field = key, old = old.toString(), new = new.toString())
    prefs.edit { putBoolean(key, new) }
}
