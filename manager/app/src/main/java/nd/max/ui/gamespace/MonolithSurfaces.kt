/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.gamespace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSpace

/*
 * أسطح MONOLITH — سطح مصمت، شعرة واحدة، ولا ظلّ ولا توهّج.
 * العمق يأتي من درجة السطح لا من الظلّ، واللون من الشيء المعروض لا من الإطار.
 *
 * «الشقّ» الجمرّي (MonolithNotch) هو التفصيل الوحيد المتكرّر: علامة آلة دقيقة
 * تسبق اسم المنتج وكل عنوان قسم. لا يتكرّر في الأزرار ولا في البطاقات.
 */

/** التفصيل المميّز: شقّ جمرّي رأسي رفيع، لا خطّ عريض ولا وسم ملوّن. */
@Composable
fun MonolithNotch(modifier: Modifier = Modifier, color: Color = Monolith.Ember) {
    Box(
        modifier
            .width(2.dp)
            .height(14.dp)
            .background(color)
    )
}

@Composable
fun MonolithPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(MaxRadius.group)
    Column(
        modifier = modifier
            .clip(shape)
            // شبه شفاف: خلفية اللعبة تتنفّس من تحت البطاقة بدل أن يغطّيها لون مصمت.
            .background(Monolith.Slate.copy(alpha = 0.72f))
            .border(MaxSpace.hairline, Color.White.copy(alpha = 0.06f), shape)
            .padding(MaxSpace.lg),
        content = content
    )
}

@Composable
fun MonolithEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    accent: Color? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonolithNotch(color = accent ?: Monolith.Ember)
        Spacer(Modifier.width(MaxSpace.sm))
        Text(
            text = text,
            style = Monolith.eyebrow,
            color = accent ?: Monolith.BoneDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (meta != null) {
            Text(
                text = meta,
                style = Monolith.label,
                color = Monolith.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MonolithDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(MaxSpace.hairline)
            .background(Monolith.Hairline)
    )
}

@Composable
fun MonolithChip(
    text: String,
    accent: Color,
    filled: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(MaxRadius.control)
    Text(
        text = text,
        style = Monolith.label,
        color = if (filled) Monolith.Bone else Monolith.BoneDim,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(shape)
            .background(if (filled) accent.copy(alpha = 0.14f) else Monolith.Slate)
            .border(MaxSpace.hairline, if (filled) accent.copy(alpha = 0.5f) else Monolith.Hairline, shape)
            .padding(horizontal = MaxSpace.md, vertical = MaxSpace.xs)
    )
}

/**
 * الشيء الوحيد الذي يحمل الضوء في الشاشة: أيقونة اللعبة الحقيقية، بلا إطار ملوّن سميك
 * ولا هالة. حين تغيب الأيقونة يُعرض الحرف الأول بهدوء، فلا يُخترع شكل.
 */
@Composable
fun MonolithGameSlab(
    icon: ImageBitmap?,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val shape = RoundedCornerShape(MaxRadius.group)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Monolith.Slate)
            .border(MaxSpace.hairline, accent.copy(alpha = 0.42f), shape)
            .padding(MaxSpace.sm),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = label.take(1).uppercase(),
                style = Monolith.display,
                color = Monolith.BoneDim
            )
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(MaxSpace.hairline)
                .background(Color.White.copy(alpha = 0.10f))
        )
    }
}

/** حلقة تركيز مرئية للريموت ولوحة المفاتيح: حدّ رفيع يظهر فقط حين يملك العنصر التركيز. */
@Composable
private fun Modifier.focusOutline(
    interaction: MutableInteractionSource,
    shape: Shape,
    color: Color
): Modifier {
    val focused by interaction.collectIsFocusedAsState()
    return border(
        MaxSpace.hairline,
        if (focused) color else Color.Transparent,
        shape
    )
}

@Composable
fun MonolithPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(MaxRadius.control)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(if (enabled) Monolith.Bone else Monolith.Glass)
            .focusOutline(interaction, shape, Monolith.Obsidian)
            .semantics(mergeDescendants = true) { }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = MaxSpace.xl),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = Monolith.heading,
            color = if (enabled) Monolith.Obsidian else Monolith.Faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MonolithGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(MaxRadius.control)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .border(MaxSpace.hairline, Monolith.HairlineStrong, shape)
            .focusOutline(interaction, shape, Monolith.Bone)
            .semantics(mergeDescendants = true) { }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = MaxSpace.lg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = Monolith.label,
            color = if (enabled) Monolith.BoneDim else Monolith.Faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * حلقة الجاهزية: تقدّم حقيقي 0..1. لا نقطة متوهّجة في المركز، ولا دوران بلا معنى.
 * الرقم يقول النسبة، والوصف يقول ما الذي اكتمل.
 */
@Composable
fun MonolithReadinessRing(
    progress: Float,
    accent: Color,
    valueText: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val safe = progress.coerceIn(0f, 1f)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(56.dp)) {
                val stroke = 3.dp.toPx()
                drawArc(
                    color = Color.White.copy(alpha = 0.10f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * safe,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            Text(text = valueText, style = Monolith.label, color = Monolith.Bone)
        }
        Text(
            text = label,
            style = Monolith.label,
            color = Monolith.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** عنوان قسم بسيط: كلمة واحدة وحشوة صغيرة، لا زخرفة. */
@Composable
fun MonolithSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = Monolith.heading,
        color = Monolith.Bone,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.semantics { heading() }
    )
}
