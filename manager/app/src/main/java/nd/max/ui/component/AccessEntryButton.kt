/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nd.max.ui.design.MaxAlpha
import nd.max.ui.design.MaxSize

/**
 * زرّ وضع الوصول في الرئيسية (جذر · شيزوكو · أساسي) — بنيّ على قالب [MaxAiEntryButton] نفسه.
 *
 * أمر المالك: «تصميم زر جذر سيئ وغير متناسق». والعلّة قياسيّة: كان يمرّ من `NeuralPill`
 * فيُرسم **٤٨dp** (حدّ اللمس مطبّقًا على شكل الحبّة نفسه) بنقطة + درع + نصّ ١١sp + سهم، بينما
 * Max AI في الصفّ التالي ٤٠dp بشارة دائرية ونصّ `labelLarge`. فالزرّان اللذان يتجاوران في
 * البطاقة نفسها كانا بلغتين.
 *
 * الآن: الكبسولة المسطّحة نفسها — سطح واحد، حدّ شعرة، شارة دائرية ٢٨dp، الاسم، سهم متّجه مع
 * اتجاه اللغة. **الشارة تحمل الدرع حين يكون الجذر فعلًا** (علامة السلطة)، وفي غيره تحمل
 * نقطة محايدة فلا يُرسم الدرع على «أساسي» فيوهم بسلطة ليست هناك. اللون من [accent] (الحالة
 * تُحمل في اللون والحدّ والشارة معًا). الشكل المرئي ٤٠dp، ومنطقة اللمس ٤٨dp عبر
 * `minimumInteractiveComponentSize` فلا تتقلّص.
 */
@Composable
fun AccessEntryButton(
    text: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier
            .minimumInteractiveComponentSize()
            .height(ACCESS_BUTTON_HEIGHT)
            .clip(CircleShape)
            .background(accent.copy(alpha = MaxAlpha.toneContainer))
            .border(MaxSize.hairlineBorder, accent.copy(alpha = MaxAlpha.borderStrong), CircleShape)
            .semantics(mergeDescendants = true) {}
            .neuralClickable(onClick, role = Role.Button)
            .padding(start = 6.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(ACCESS_BADGE_SIZE)
                .clip(CircleShape)
                .background(accent.copy(alpha = MaxAlpha.toneContainerStrong)),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(MaxSize.iconGlyphSmall),
                    tint = accent,
                )
            } else {
                Box(Modifier.size(ACCESS_DOT_SIZE).clip(CircleShape).background(accent))
            }
        }
        Text(
            text,
            color = scheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = accent,
        )
    }
}

private val ACCESS_BUTTON_HEIGHT = 40.dp
private val ACCESS_BADGE_SIZE = 28.dp
private val ACCESS_DOT_SIZE = 8.dp
