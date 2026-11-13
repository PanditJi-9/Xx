/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.gamespace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSpace

/*
 * أسطح MONOLITH — زجاج من فرق سطوع، لا ظلّ ولا تدرّج زخرفي.
 * كل سطح هنا يستعمل رموز التصميم (`MaxRadius`/`MaxSpace`) فلا يضيف حرفيًّا جديدًا.
 */

@Composable
fun MonolithPanel(
    modifier: Modifier = Modifier,
    accent: Color = Monolith.Hairline,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(MaxRadius.group)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.055f), Color.White.copy(alpha = 0.015f))
                )
            )
            .border(MaxSpace.hairline, accent, shape)
            .padding(MaxSpace.lg),
        content = content
    )
}

@Composable
fun MonolithEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    meta: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(MaxSpace.xxl)
                .height(MaxSpace.hairline)
                .background(Monolith.Ember)
        )
        Spacer(Modifier.width(MaxSpace.md))
        Text(
            text = text,
            style = Monolith.eyebrow,
            color = Monolith.BoneDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
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
    val shape = RoundedCornerShape(MaxRadius.chip)
    Text(
        text = text,
        style = Monolith.label,
        color = if (filled) Monolith.Bone else Monolith.BoneDim,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(shape)
            .background(if (filled) accent.copy(alpha = 0.16f) else Color.Transparent)
            .border(MaxSpace.hairline, if (filled) accent.copy(alpha = 0.55f) else Monolith.Hairline, shape)
            .padding(horizontal = MaxSpace.md, vertical = MaxSpace.xs)
    )
}

@Composable
fun MonolithPrimaryButton(
    text: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(MaxRadius.control)
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(if (enabled) accent else Monolith.Glass)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
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
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .border(MaxSpace.hairline, Monolith.HairlineStrong, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
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
 * حلقة الجاهزية: تقدّم حقيقي 0..1، لا يدور بلا معنى. الرقم داخل الحلقة هو النسبة
 * المكتملة، والوصف تحتها يقول ما الذي اكتمل — لا «AI» ولا «مثالي» بلا قياس.
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
            Canvas(Modifier.size(76.dp)) {
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
                drawCircle(
                    color = accent.copy(alpha = 0.20f),
                    radius = 2.4.dp.toPx(),
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }
            Text(text = valueText, style = Monolith.heading, color = Monolith.Bone)
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
