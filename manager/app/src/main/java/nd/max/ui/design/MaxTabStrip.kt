/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * شريط تبويبات واحد لكل التطبيق — بلا حبّات منفصلة، ومؤشّر واحد يحمل الاختيار.
 *
 * **ما كان يعيبه.** كل تبويب حبّة كبسولة (`MaxRadius.pill`) بحدّها ولونها، فيصير الشريط خمسة أزرار
 * متجاورة لا تبويبات لمجموعة واحدة، والمختار يُعرف بلون الخلفية وحده. والكبسولة نفسها مخالفة لـ
 * DESIGN.md: الكبسولة **حالة** لا **فعل**، واختيار التبويب فعل.
 *
 * **الشكل الآن.** لا خلفية ولا حدّ لكل تبويب. عدسة واحدة بزاوية `MaxRadius.control` تنزلق من التبويب
 * القديم إلى الجديد، فيُفهم الاختيار بالحركة، ويبقى اللون والخط والأيقونة أدلّةً إضافية لا بديلًا.
 * الحركة هنا تشرح تغيّرًا حدث فعلًا، وتبقى تحت سقف `MaxDuration.deliberate` كما يقضي DESIGN.md.
 *
 * **تثبيت العدسة على تبويبها.** العدسة ترسم داخل محتوى الشريط القابل للتمرير، لا فوق نافذة العرض،
 * فتُقاس مواضع التبويبات في إحداثيات المحتوى، وهي لا تتغيّر بالتمرير. النتيجة أن العدسة تلتصق بتبويبها
 * بالضبط: لا تلحق به بنابض أثناء التمرير، ولا تبقى عالقة في موضع قديم حين يخرج تبويبها من الشاشة.
 * وتنزلق العدسة فقط حين يتغيّر الاختيار، أو يتغيّر عرض أحد التبويبات.
 *
 * **القيود المحسومة (كما كانت):**
 *  1. العرض للتبويب لا للصف: المقطع يأخذ عرضه الطبيعي (`softWrap = false`)، والشريط كله يتمرّر.
 *  2. لا تصغير خطّ: `labelLarge` هو حجم المقطع، والفرق كله في الشكل لا في الخط.
 *  3. صندوق لمس `MaxSize.minTouchTarget` (٤٨dp) لكل مقطع.
 *
 * **وRTL بلا كود شرطيّ:** مواضع التبويبات تُقاس فعليًّا (`positionInParent`)، فالعدسة تتبع ما رُسم
 * لا ما افترضناه، والصفّ الداخلي يرتّب تبويباته من الجهة الصحيحة تلقائيًّا، و`bringIntoView` يعرف اتجاه
 * التمرير من التخطيط نفسه.
 */
package nd.max.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch
import nd.max.ui.component.MaxMotion
import nd.max.ui.component.maxPressMotion
import nd.max.ui.component.rememberAnimationsEnabled
import nd.max.ui.component.rememberMaxHaptics

/** تبويب واحد: ليبل لا يُقصّ، وأيقونة اختيارية، وعدّاد اختياري. */
data class MaxTab(
    val label: String,
    val icon: ImageVector? = null,
    val badgeCount: Int = 0,
)

/**
 * شريط تبويبات قابل للتمرير الأفقي، بلا حدّ أعلى لعدد التبويبات.
 *
 * **الحواف:** يمتدّ إلى حافة الشاشة ويُظلّل طرفيه بالمكوّن المشترك [maxEdgeFade]، والمقطع الأول يرتاح
 * عند هامش الصفحة، فلا يُقصّ مقطع عند حدّ الهامش.
 *
 * ولا يُوصف على أنه `TabRow`: محتوى المقطع يُقاس بعرضه الطبيعي، فالشريط يتجاوز الشاشة ويمرّ بدل أن
 * يضغط عناصره.
 */
@Composable
fun MaxTabStrip(
    tabs: List<MaxTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    mutedIndices: Set<Int> = emptySet(),
) {
    if (tabs.isEmpty()) return
    val scroll = rememberScrollState()
    val haptics = rememberMaxHaptics()
    val bounds = remember { TabBounds() }
    val selectedRequester = remember { BringIntoViewRequester() }

    // المقطع المختار يُرى عند التغيير: بلا هذا يصير تبويب مُختار خارج الشاشة عنوانًا لحالة لا تُرى،
    // خصوصًا في العربية حيث يبدأ الشريط من اليمين.
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in tabs.indices) {
            runCatching { selectedRequester.bringIntoView() }
        }
    }

    Row(
        // يمتدّ إلى حافة الشاشة ويُظلّل طرفيه (`maxBleed` و`maxEdgeFade`). الحشو داخل المحتوى المتمرّر،
        // فيرتاح أوّل تبويب عند هامش الصفحة، وآخره كذلك.
        modifier = modifier
            .fillMaxWidth()
            .maxBleed()
            .maxEdgeFade()
            .horizontalScroll(scroll)
            .padding(PaddingValues(horizontal = MaxSpace.gutter, vertical = MaxSpace.xs)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // هذا الصفّ الداخلي هو ما يتمرّر مع التبويبات، والعدسة ترسم فيه، فتكون في إحداثيات المحتوى نفسه.
        Row(
            modifier = Modifier.tabSelectionLens(
                bounds = bounds,
                selectedIndex = selectedIndex,
                fill = accent.copy(alpha = MaxAlpha.toneContainerStrong),
                stroke = accent.copy(alpha = MaxAlpha.borderStrong),
                cornerRadius = MaxRadius.control,
            ),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                key(tab.label) {
                    MaxTabItem(
                        tab = tab,
                        selected = index == selectedIndex,
                        accent = accent,
                        muted = index in mutedIndices,
                        modifier = Modifier
                            .recordTabBounds(bounds, index)
                            .then(
                                if (index == selectedIndex) {
                                    Modifier.bringIntoViewRequester(selectedRequester)
                                } else {
                                    Modifier
                                },
                            ),
                        onSelect = {
                            if (index != selectedIndex) {
                                haptics.select()
                                onSelect(index)
                            }
                        },
                    )
                }
            }
        }
    }
}

/** مواضع التبويبات داخل الصفّ الداخلي، مفتاحها الفهرس. تُقاس ولا تُحسب: العرض يختلف بطول الليبل. */
private class TabBounds {
    val rects = mutableStateMapOf<Int, Rect>()
}

/** يسجّل موضع هذا التبويب داخل [bounds]. */
private fun Modifier.recordTabBounds(bounds: TabBounds, index: Int): Modifier =
    onGloballyPositioned { coordinates ->
        val rect = Rect(coordinates.positionInParent(), coordinates.size.toSize())
        if (bounds.rects[index] != rect) bounds.rects[index] = rect
    }

/** حالة العدسة: حدودها المتحرّكة، و`placed` تعرف هل وُضعت أول مرة، فلا تنزلق من مكان لم تكن فيه. */
private class TabLens {
    var placed = false
    val left = Animatable(0f)
    val top = Animatable(0f)
    val right = Animatable(0f)
    val bottom = Animatable(0f)
}

/** الحافة التابعة: تلحق بالقائدة بلا ارتداد، فيبدو المؤشّر مادّةً تنساب لا لوحًا يقفز. */
private val trailingEdgeSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/**
 * العدسة التي تنزلق بين التبويبات: مؤشّر واحد يحمل الاختيار، لا خلفية لكل تبويب.
 *
 * **تُرسم في إحداثيات المحتوى:** موضع كل تبويب ثابت مهما مرّ الشريط، فلا يتبدّل الهدف أثناء التمرير،
 * ولا تُعاد برمجة الحركة في كل إطار. تنزلق العدسة فقط حين يتغيّر الهدف نفسه: عند تغيّر الاختيار، أو
 * عرض تبويب.
 *
 * **الحافتان تتحركان بنابضين مختلفين:** الحافة القائدة تسرع نحو الوجهة، والتابعة تلحق بها. وهذا منطق
 * مؤشّر شريط التنقّل (`MaxNavBar.kt`) نفسه، لكنه محلّي هنا عمدًا: توحيدهما قرار منفصل يخصّ ذلك الملف،
 * ولا يُمسّ في هذه المهمة.
 *
 * بلا حركة (تفضيل النظام) تقفز العدسة إلى مكانها مباشرة، والحالة نفسها مقروءة بالنص واللون والأيقونة.
 */
@Composable
private fun Modifier.tabSelectionLens(
    bounds: TabBounds,
    selectedIndex: Int,
    fill: Color,
    stroke: Color,
    cornerRadius: Dp,
): Modifier {
    val animationsEnabled = rememberAnimationsEnabled()
    val lens = remember { TabLens() }
    val target = bounds.rects[selectedIndex]

    LaunchedEffect(target, animationsEnabled) {
        val t = target ?: return@LaunchedEffect
        if (!lens.placed || !animationsEnabled) {
            lens.placed = true
            lens.left.snapTo(t.left)
            lens.top.snapTo(t.top)
            lens.right.snapTo(t.right)
            lens.bottom.snapTo(t.bottom)
        } else {
            // الاتجاه بالإحداثيات المادّية لا بالبداية والنهاية، فيعمل في العربية كما في الإنجليزية.
            val movingRight = t.left > lens.left.value
            launch {
                lens.left.animateTo(t.left, if (movingRight) trailingEdgeSpring else MaxMotion.controlSpring)
            }
            launch {
                lens.right.animateTo(t.right, if (movingRight) MaxMotion.controlSpring else trailingEdgeSpring)
            }
            launch { lens.top.animateTo(t.top, MaxMotion.controlSpring) }
            launch { lens.bottom.animateTo(t.bottom, MaxMotion.controlSpring) }
        }
    }

    return drawBehind {
        val width = lens.right.value - lens.left.value
        val height = lens.bottom.value - lens.top.value
        if (target != null && width > 0f && height > 0f) {
            val topLeft = Offset(lens.left.value, lens.top.value)
            val lensSize = Size(width, height)
            val corner = CornerRadius(cornerRadius.toPx())
            drawRoundRect(color = fill, topLeft = topLeft, size = lensSize, cornerRadius = corner)
            drawRoundRect(
                color = stroke,
                topLeft = topLeft,
                size = lensSize,
                cornerRadius = corner,
                style = Stroke(width = MaxSize.hairlineBorder.toPx()),
            )
        }
    }
}

/**
 * مقطع واحد. يُنادى من [MaxTabStrip] فقط، لا يُنسخ في شاشة.
 *
 * لا خلفية له: العدسة تحمل الاختيار، فيبقى اللون لونَ الخطّ والأيقونة والعدّاد.
 */
@Composable
private fun MaxTabItem(
    tab: MaxTab,
    selected: Boolean,
    accent: Color,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val animationsEnabled = rememberAnimationsEnabled()

    val content by animateColorAsState(
        targetValue = if (selected) accent else scheme.onSurfaceVariant,
        animationSpec = tween(MaxDuration.quick),
        label = "maxTabContent",
    )
    val tabAlpha by animateFloatAsState(
        targetValue = if (muted) MaxAlpha.disabledContent else 1f,
        animationSpec = tween(MaxDuration.quick),
        label = "maxTabAlpha",
    )
    val lift by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = MaxMotion.controlSpring,
        label = "maxTabIconLift",
    )

    Row(
        modifier = modifier
            .heightIn(min = MaxSize.minTouchTarget)
            .graphicsLayer { alpha = tabAlpha }
            .then(if (animationsEnabled) Modifier.maxPressMotion(interaction, pressedScale = 0.97f) else Modifier)
            .clip(RoundedCornerShape(maxRadius(MaxRadius.control)))
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onSelect,
            )
            .padding(horizontal = MaxSpace.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tab.icon?.let { glyph ->
            Icon(
                imageVector = glyph,
                contentDescription = null,
                tint = content,
                modifier = Modifier
                    .size(MaxSize.iconGlyphSmall)
                    .graphicsLayer {
                        if (animationsEnabled) {
                            scaleX = lift
                            scaleY = lift
                        }
                    },
            )
            Spacer(Modifier.width(MaxSpace.sm))
        }
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelLarge.copy(lineBreak = LineBreak.Heading),
            color = content,
            maxLines = 1,
            // ولا يُقرأ `softWrap = false` مع `Ellipsis` كتنازل: لا يُقصّ هنا شيء أبدًا، لأن غياب
            // الالتفاف يجعل النصّ يقيس بعرضه الحقيقي، والتجاوز يحمله تمرير الشريط.
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
        if (tab.badgeCount > 0) {
            Spacer(Modifier.width(MaxSpace.sm))
            // العدّاد شارة صغيرة بزاوية `chip` لا كبسولة: الكبسولة في هذه الطبقة تعني حالة فقط.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(maxRadius(MaxRadius.chip)))
                    .background(content.copy(alpha = MaxAlpha.toneContainer))
                    .padding(horizontal = MaxSpace.xs),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.badgeCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = content,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
