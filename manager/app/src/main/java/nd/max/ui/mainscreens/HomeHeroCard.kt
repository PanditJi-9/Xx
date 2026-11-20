/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * البطاقة الأولى في الرئيسية — **الشكل المدمج** (طلب المالك بلقطة «الصورة الثانية»).
 *
 * ترتيب القراءة: الجهاز (أيقونته واسمه وشريحته)، ثم صفّ الوصول (الشارة، وبجانبها شرح الغياب حين لا
 * جذر ولا شيزوكو)، ثم الحرارة قراءةً رئيسية مع Max AI، ثم ثلاث بلاطات قراءة **على سطر واحد** لكل منها،
 * ثم باب إلى نظرة الجهاز بزرّ مدمج في طرف الصف.
 *
 * ما تغيّر عن الشكل الطويل، ولماذا:
 *  - البلاطات كانت تحجز سطرين لكل تسمية فتطول البطاقة بلا قراءة إضافية. الآن سطر واحد للتسمية وقيمة
 *    واحدة، فتُقرأ البلاطات الثلاث أفقيًّا وبجانب بعضها.
 *  - باب نظرة الجهاز كان زرًّا بعرض البطاقة كلها. الآن زرّ مدمج في طرف الصف، والصندوق اللمسي يبقى ٤٨dp
 *    (`MaxSize.minTouchTarget`) كما تفرضه سياسة اللمس، فالمرئي أصغر والمساحة اللمسية كما هي.
 *  - شارة الوصول انتقلت إلى صفّ الشرح تحت الجهاز، فالحالة تُقرأ بجانب سببها.
 *
 * ما بقي كما هو: الحرارة بكلمة حكمها بلونها، والغياب شرطة (ADR-07). ولا رقم حرفيًّا هنا: المسافات من
 * `MaxSpace`، والحجم من `MaxSize`، والنصف قطر من `MaxRadius`.
 */
package nd.max.ui.mainscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import nd.max.R
import nd.max.core.privilege.PrivilegeLevel
import nd.max.ui.component.AccessEntryButton
import nd.max.ui.component.MaxAiEntryButton
import nd.max.ui.component.MaxValueWithUnit
import nd.max.ui.component.MaxEase
import nd.max.ui.component.MaxMotion
import nd.max.ui.component.rememberScrollEffectsEnabled
import nd.max.ui.component.NeuralIconChip
import nd.max.ui.component.NeuralPanel
import nd.max.ui.component.NeuralTile
import nd.max.ui.component.NeuralValue
import nd.max.ui.component.neuralClickable
import nd.max.ui.component.neuralPalette
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.navigation.MaxDestination
import nd.max.ui.theme.HeroTitleStyle
import nd.max.ui.theme.MonoFontFamily
import nd.max.ui.theme.MonoValueStyleHero
import nd.max.ui.viewmodel.DashboardState
import nd.max.ui.design.maxRadius

@Composable
internal fun HomeHeroCard(
    deviceName: String,
    dashboard: DashboardState,
    /** طبقة الامتياز المكتشفة — من `PrivilegeManager` لا من قراءة ثانية. */
    accessLevel: PrivilegeLevel,
    /** حالة Max AI الحقيقية (`MaxAiState.aiEnabled`) — تُعلّم الزرّ بلونه. */
    aiEnabled: Boolean,
    /** الباب إلى نظرة الجهاز: المالك الوحيد لفكرة «نظرة على الجهاز» بأقسامها. */
    onOverview: () -> Unit,
    onMaxAi: () -> Unit,
    /** فتح نافذة وضع الوصول: الطبقة ومدير الجذر وSELinux والفلتر. */
    onAccess: () -> Unit,
) {
    val p = neuralPalette()
    val access = accessAccent(accessLevel, p)

    // الحرارة: الواحدة نفسها التي تقرؤها الشبكة (`deviceHeatC`)، وكلمة حكمها بلونها.
    val heat = deviceHeatC(dashboard)
    val heatAccent = temperatureAccent(heat)

    // البطارية والطاقة: الصفر غير مقروء، فيُكتب شرطة لا رقمًا كاذبًا.
    val battery = dashboard.batteryPercent.takeIf { it > 0 }
    val batteryAccent = when {
        dashboard.isCharging -> p.ok
        battery != null && battery <= 20 -> p.warn
        else -> p.accentAlt
    }
    val power = dashboard.powerWatt.takeIf { it > 0.05f }

    NeuralPanel(accent = access, verticalSpacing = MaxSpace.md) {
        // تقليل الحركة يُقرأ مرة واحدة هنا (قرار المستخدم/النظام)، ويُمرَّر للحركة الوحيدة في البطاقة.
        val swapMotion = rememberScrollEffectsEnabled()
        // 1 — الجهاز: الأيقونة ثم الاسم بنمط `HeroTitleStyle`، ثم الشريحة.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NeuralIconChip(Icons.Rounded.PhoneAndroid, p.accent, size = MaxSize.iconContainer)
            Spacer(Modifier.width(MaxSpace.md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
                Text(
                    deviceName,
                    color = p.text,
                    style = HeroTitleStyle,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    dashboard.chipsetName,
                    color = p.muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // 2 — الوصول: الشارة (جذر · شيزوكو · أساسي)، وبجانبها شرح الغياب حين لا جذر ولا شيزوكو.
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
        ) {
            // الشارة تحمل درعًا حين يكون الجذر فعلًا؛ فلا يُقرأ «جذر» بلا علامة سلطة،
            // ولا يُرسم الدرع على «أساسي» فيوهم بسلطة ليست هناك.
            AccessEntryButton(
                text = stringResource(accessLabelRes(accessLevel)),
                accent = access,
                icon = if (accessLevel == PrivilegeLevel.ROOT) Icons.Rounded.Shield else null,
                onClick = onAccess,
            )
            if (accessLevel == PrivilegeLevel.NONE) {
                Text(
                    stringResource(R.string.home_access_note_none),
                    color = p.muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 3 — الحرارة قراءةً رئيسية: كلمة الحكم بلونها فوق الرقم الكبير بالخط الأحادي، والوحدة بجانبه،
        // و`Max AI` في الطرف المقابل.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.home_temperature_short),
                        color = p.muted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                    if (heat != null) {
                        Spacer(Modifier.width(MaxSpace.sm))
                        // كلمة الحكم وحدها تتبدّل بحركة رأسية قصيرة — **لا** الرقم ولا لونه: الأرقام
                        // تتدحرج من `NeuralValue` أصلًا، واللون يفصل دلالة ويجب أن يقطع فورًا كي
                        // يبقى صادقًا مع اللحظة. و`sizeTransform = null` فلا يغيّر تبدّلُ كلمةٍ
                        // هندسة الصفّ. وتقليل الحركة يُعرض الحالة النهائية ساكنةً لا يخفيها.
                        val verdict = stringResource(heatWordRes(heat))
                        if (swapMotion) {
                            // تلاشٍ متبادَل لا انزلاق: `AnimatedContent` احتاج `tween<IntOffset>`
                            // صريحًا فسقط التصريف عليه، والنفع البصري واحد — الكلمة تتبدّل بحركة
                            // قصيرة والرقم لا يتحرّك معها ولا يقفز خطّ الأساس حين تطول الكلمة.
                            Crossfade(
                                targetState = verdict,
                                animationSpec = tween(MaxMotion.fast, easing = MaxEase.Emphasized),
                                label = "heatVerdict",
                            ) { word ->
                                Text(
                                    word,
                                    color = heatAccent,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                )
                            }
                        } else {
                            Text(
                                verdict,
                                color = heatAccent,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                    }
                }
                // الرقم ووحدته نصّ واحد (لا Row يحاذي الأسفل): الوحدة كانت تهبط تحت خطّ الأساس،
                // والرقم المتحرّك لا يملك خطّ أساس يُحاذى إليه من الأصل. التفصيل في [MaxValueWithUnit].
                MaxValueWithUnit(
                    value = heat?.toString() ?: "\u2014",
                    unit = if (heat != null) stringResource(R.string.home_unit_celsius) else null,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = p.text,
                    unitColor = p.muted,
                )
            }
            MaxAiEntryButton(
                text = stringResource(R.string.max_nav_max_ai),
                active = aiEnabled,
                onClick = onMaxAi,
            )
        }

        // 4 — ثلاث بلاطات قراءة على سطر واحد: مدة التشغيل · البطارية · استهلاك الطاقة.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
            HeroTile(
                label = stringResource(R.string.max_home_uptime),
                value = compactUptime(dashboard.uptimeMinutes),
                accent = p.accent,
                modifier = Modifier.weight(1f),
            )
            HeroTile(
                label = stringResource(R.string.max_home_battery),
                value = battery?.let { "$it%" } ?: "\u2014",
                accent = batteryAccent,
                modifier = Modifier.weight(1f),
            )
            HeroTile(
                label = stringResource(R.string.home_power_draw),
                value = power?.let { "${it.oneDecimal()} W" } ?: "\u2014",
                accent = p.accentAlt,
                modifier = Modifier.weight(1f),
            )
        }

        // 5 — باب نظرة الجهاز: زرّ مدمج في طرف الصف، لا بعرض البطاقة كلها.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            HeroDoorButton(
                text = stringResource(R.string.home_hero_open_overview),
                icon = MaxDestination.DeviceInfo.icon,
                accent = p.accent,
                onClick = onOverview,
            )
        }
    }
}

/**
 * بلاطة قراءة في البطاقة الأولى: تسمية بسطر واحد بلونها، وقيمة بالخط الأحادي على لون البلاطة نفسه.
 * الحشو مضغوط بالرموز، والتسمية الطويلة تُقصّ بنقاط عند الضيق بدل أن تحجز سطرًا ثانيًا لا يُقرأ.
 */
@Composable
private fun HeroTile(label: String, value: String, accent: Color, modifier: Modifier) {
    val p = neuralPalette()
    NeuralTile(
        modifier = modifier,
        accent = accent,
        contentPadding = PaddingValues(horizontal = MaxSpace.sm, vertical = MaxSpace.sm),
        verticalSpacing = MaxSpace.xs,
    ) {
        Text(
            label,
            color = accent,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        NeuralValue(value, style = MonoValueStyleHero, color = p.text, maxLines = 1)
    }
}

/**
 * باب نظرة الجهاز: زرّ مدمج بحواف المجموعة، في طرف الصف لا بعرض البطاقة كلها. الشكل المرئي أصغر من
 * صندوق اللمس، والصندوق نفسه `MaxSize.minTouchTarget` كما تفرضه سياسة اللمس.
 */
@Composable
private fun HeroDoorButton(
    text: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
) {
    val p = neuralPalette()
    val shape = RoundedCornerShape(maxRadius(MaxRadius.control))
    Box(
        Modifier
            .heightIn(min = MaxSize.minTouchTarget)
            .neuralClickable(onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .clip(shape)
                .background(p.tile)
                .border(MaxSize.hairlineBorder, accent.copy(alpha = .32f), shape)
                .padding(horizontal = MaxSpace.md, vertical = MaxSpace.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(MaxSize.iconGlyphSmall), tint = accent)
            Spacer(Modifier.width(MaxSpace.xs))
            Text(
                text,
                color = p.text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(MaxSpace.xs))
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                null,
                Modifier.size(MaxSize.iconGlyphSmall),
                tint = accent,
            )
        }
    }
}
