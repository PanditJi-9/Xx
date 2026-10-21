/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius
import nd.max.ui.component.MaxMotion
import nd.max.ui.component.maxPressMotion
import nd.max.ui.component.rememberAnimationsEnabled
import nd.max.ui.component.rememberMaxHaptics
import nd.max.ui.theme.NavBarStyle
import nd.max.ui.theme.NavLabelMode
import kotlinx.coroutines.launch
import nd.max.ui.design.MaxAlpha

/**
 * Tint of the floating navigation surface when the backdrop is blurred by Haze.
 * Light on purpose: the blur already separates the bar from the page, so the bar only
 * needs enough tint to keep its labels and icons readable.
 */
private const val FloatingBarTintAlpha = 0.45f

/**
 * Tint used when the blur preference is off. Still translucent — the whole point of the
 * floating bar is that content is seen travelling behind it — but darker than the blurred
 * case, because a plain translucent pill has to carry contrast on its own.
 */
private const val FloatingBarTintAlphaNoBlur = 0.82f

/**
 * Bottom-bar / nav-rail item backed by a [MaxDestination] route.
 * Kept as a thin data holder so the pill composable stays destination-agnostic.
 */
data class NavItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val gradientColors: List<Color> = listOf(Color.Transparent, Color.Transparent),
)

/**
 * مواضع عناصر الشريط داخل حاويتها، مفتاحها المسار.
 *
 * **لماذا تُقاس ولا تُحسب من الفهرس.** الحساب (`index * (عرض + فاصل)`) يفترض عرضًا متساويًا واتجاه
 * LTR وارتفاعًا واحدًا — وكلها تنكسر هنا: الليبل العربي قد يلتفّ سطرين فيطول عنصر دون جاره، وRTL
 * يعكس الترتيب. القياس الفعلي لا يفترض شيئًا.
 */
private class NavItemBounds {
    val rects = mutableStateMapOf<String, Rect>()
}

/** يسجّل موضع هذا العنصر في [bounds]. يُوضَع **قبل** أي حركة ضغط حتى لا يتأرجح المؤشّر مع اللمس. */
private fun Modifier.navItemBounds(bounds: NavItemBounds, route: String): Modifier =
    onGloballyPositioned { coordinates ->
        val rect = Rect(coordinates.positionInParent(), coordinates.size.toSize())
        if (bounds.rects[route] != rect) bounds.rects[route] = rect
    }

/**
 * مؤشّر الاختيار **الواحد** الذي ينتقل بين عناصر الشريط.
 *
 * كان لكل عنصر خلفيته: واحدة تتلاشى وأخرى تظهر، فلا يرى العين «انتقالًا» بل وميضًا. هنا شكلٌ واحد
 * يسير من مكانه إلى المكان الجديد، فيفهم المستخدم أن *الاختيار انتقل* لا أن شيئًا انطفأ وآخر أُضيء.
 *
 * **والشكل يبقى صلبًا أثناء السير** (نابضٌ واحد للحدود الأربعة): كان للمؤشّر نابضان مختلفان
 * فيمتدّ أفقيًّا ثم يرتدّ، وقرأه المالك «غريبًا» فصار انزلاقًا واحدًا بلا تمطّط. ولا يزيد الحساب
 * على أربعة أرقام تتغيّر في مرحلة الرسم، فلا إعادة تركيب ولا قياس في أي إطار.
 *
 * بلا حركة (تفضيل النظام) يقفز المؤشّر إلى مكانه مباشرة، والحالة نفسها مقروءة بالنصّ والخط والأيقونة.
 */
@Composable
private fun Modifier.navSelectionIndicator(
    bounds: NavItemBounds,
    selectedRoute: String,
    color: Color,
    cornerRadius: Dp,
): Modifier {
    val animationsEnabled = rememberAnimationsEnabled()
    val left = remember { Animatable(0f) }
    val top = remember { Animatable(0f) }
    val right = remember { Animatable(0f) }
    val bottom = remember { Animatable(0f) }
    val placed = remember { booleanArrayOf(false) }
    val target = bounds.rects[selectedRoute]

    LaunchedEffect(target, animationsEnabled) {
        val t = target ?: return@LaunchedEffect
        if (!placed[0] || !animationsEnabled) {
            placed[0] = true
            left.snapTo(t.left)
            top.snapTo(t.top)
            right.snapTo(t.right)
            bottom.snapTo(t.bottom)
        } else {
            // **نابضٌ واحد للحدود الأربعة (أمر المالك ٢٠٢٦-١٠-٢١: «أنيميشن التنقّل يبدو
            // غريبًا»):** كان للحافتين القائدتين صلابة أعلى من التابعتين، فكان المؤشّر
            // **يتمطّط** أفقيًّا وهو يسير ثم يرتدّ إلى مستطيله. أثرٌ يُقرأ «مطّاطًا» لا
            // «مادّة»، خصوصًا مع صفحةٍ تنكمش في الوقت نفسه. صار شكلٌ صلبٌ واحد ينزلق إلى
            // موضعه: الحركة تُقرأ انتقالَ اختيار، لا تمدّد جسم.
            val slide = spring<Float>(dampingRatio = 0.90f, stiffness = Spring.StiffnessMediumLow)
            launch { left.animateTo(t.left, slide) }
            launch { right.animateTo(t.right, slide) }
            launch { top.animateTo(t.top, slide) }
            launch { bottom.animateTo(t.bottom, slide) }
        }
    }

    return drawBehind {
        val width = right.value - left.value
        val height = bottom.value - top.value
        if (target != null && width > 0f && height > 0f) {
            val topLeft = Offset(left.value, top.value)
            val indicatorSize = Size(width, height)
            val corner = CornerRadius(cornerRadius.toPx())
            drawRoundRect(color = color, topLeft = topLeft, size = indicatorSize, cornerRadius = corner)
            // لمسة الزجاج: لمعةٌ تنزل من أعلى المؤشّر وحافّةٌ رقيقة تلتقط الضوء، فيبدو عدسةً تنزلق
            // على الشريط لا لوحًا ملوّنًا. شفافيتان خافتتان ولا تأثير في اللون الأساسي ولا في التباين.
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                    startY = topLeft.y,
                    endY = topLeft.y + height
                ),
                topLeft = topLeft,
                size = indicatorSize,
                cornerRadius = corner
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = topLeft,
                size = indicatorSize,
                cornerRadius = corner,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

/**
 * Pill-style navigation rail for wide layouts (>= 840dp).
 * Mirrors [BottomNavBar]; the two must stay visually identical.
 */
@Composable
fun NavigationRailBar(
    items: List<NavItem>,
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isBlurEnabled: Boolean = false,
    hazeState: HazeState? = null,
    // النمط يصل إلى الرأس أيضًا (§٦-ب): الرأس والشريط «يجب أن يبقيا متطابقين» — فالنصّ وحجم
    // الأيقونة وأرض اللمس تُقرأ من النمط نفسه في الاثنين. والفرق البنيويّ الوحيد أنّ الرأس
    // عموديّ، فلا «تمدّد أفقيّ» للعنصر النشط فيه (ولا يُدَّعى نمطٌ لا يظهر هنا).
    style: NavBarStyle = NavBarStyle.FLOATING,
) {
    val navigationShape = RoundedCornerShape(maxRadius(MaxRadius.sheet))
    val isBlurActive = isBlurEnabled && hazeState != null
    val itemBounds = remember { NavItemBounds() }
    val indicatorColor = MaterialTheme.colorScheme.primaryContainer

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(104.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 12.dp, top = 16.dp, bottom = 16.dp)
            .clip(navigationShape)
            .then(
                if (isBlurEnabled && hazeState != null) {
                    Modifier.hazeEffect(state = hazeState) {
                        blurEffect { blurRadius = 24.dp }
                    }
                } else Modifier
            ),
        shape = navigationShape,
        // Same floating treatment as BottomNavBar: never fully opaque, so the page keeps
        // being perceptible underneath, plus a hairline edge to hold the shape when the
        // blur path is off (see the note there).
        color = if (isBlurActive) {
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = FloatingBarTintAlpha)
        } else {
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = FloatingBarTintAlphaNoBlur)
        },
        border = if (isBlurActive) null else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = MaxAlpha.border),
        ),
        shadowElevation = if (isBlurActive) 0.dp else 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
                .navSelectionIndicator(itemBounds, selectedRoute, indicatorColor, MaxRadius.tile),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            items.forEach { item ->
                val isSelected = selectedRoute == item.route
                NavPill(
                    item = item,
                    isSelected = isSelected,
                    isBlurEnabled = isBlurEnabled,
                    indicatorDrawn = true,
                    style = style,
                    onClick = { onItemSelected(item.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .navItemBounds(itemBounds, item.route),
                )
            }
        }
    }
}

/**
 * Bottom navigation bar for compact layouts.
 * Exactly the four primary destinations (ADR-03).
 *
 * **والأنماط الستّة (§٦-ب)** — كلّها تنقّلٌ كامل، وتختلف في البنية لا في اللون:
 * [NavBarStyle.FLOATING] كبسولة عائمة والنصّ تحت كل أيقونة (الافتراضيّ) · [NavBarStyle.DOCKED]
 * بعرض الشاشة على الحافّة السفلية بزوايا عليا وحدها · [NavBarStyle.COMPACT] أيقونات وحدها
 * بأهداف أوسع · [NavBarStyle.LABELED] العنصر النشط وحده يتمدّد ويعرض نصّه ·
 * [NavBarStyle.LARGE] مساحة أكبر للأيقونة والنصّ · [NavBarStyle.ADAPTIVE] سطحٌ في مستوى الصفحة
 * بلا شفافية ولا ظلّ وباستدارة البطاقة.
 *
 * والقواعد تُقرأ من النمط نفسه ([NavBarStyle.floating] · [NavBarStyle.labels] ·
 * [NavBarStyle.iconSize]…) فلا فرعَ مكتوبًا هنا يخالف ما تعرضه المعاينة. والزوايا من `maxRadius`
 * فتتبع مقياس «استدارة الحواف» في السمة كما تتبعه البطاقات.
 */
@Composable
fun BottomNavBar(
    items: List<NavItem>,
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isBlurEnabled: Boolean = false,
    hazeState: HazeState? = null,
    style: NavBarStyle = NavBarStyle.FLOATING,
) {
    val docked = !style.floating
    // «المتكيّف» يسكن مستوى الصفحة (§٦-ب): سطحٌ معتم بلا ضباب ولا ظلّ، وحوافه على هوامش
    // الصفحة نفسها، واستدارة البطاقة لا الكبسولة.
    val pagePlane = style.pagePlane
    val pillShape = if (docked) {
        // الأرض مستقيمة: الشريط ملتصق بها، والاستدارة في الزاويتين العليين وحدها.
        RoundedCornerShape(
            topStart = maxRadius(MaxRadius.sheet),
            topEnd = maxRadius(MaxRadius.sheet)
        )
    } else {
        RoundedCornerShape(maxRadius(if (pagePlane) MaxRadius.group else MaxRadius.sheet))
    }
    // ضبابٌ خلف سطحٍ معتم لا يرى شيئًا — فالبوابة تُطفأ مع «المتكيّف» ولو كانت مفعّلة في السمة.
    val isBlurActive = isBlurEnabled && hazeState != null && !pagePlane
    val itemBounds = remember { NavItemBounds() }
    val indicatorColor = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier
            .fillMaxWidth()
            // The bar and its clearance: page content scrolls behind the pill (the shell
            // publishes this wrapper's measured height as
            // LocalFloatingBottomBarHeight — see MainActivity), and these margins are the
            // gap that keeps the last row of a page from touching the pill.
            //
            // وفي «الملتصق» لا هوامش: الوجه يمتدّ بعرض الشاشة، وحشو شريط النظام ينتقل إلى
            // داخل الوجه (على الصفّ أدناه) فيصل الوجه إلى حافّة الشاشة فعلًا.
            .then(
                if (docked) {
                    Modifier
                } else {
                    // و«المتكيّف» بهوامش الصفحة لا بهوامش الكبسولة: الوجه يقف على الخطّ نفسه
                    // الذي يقف عليه المحتوى، فلا يبدو شيئًا ثالثًا بين المحتوى والحافّة.
                    val barInsetH = if (pagePlane) MaxSpace.gutter else 18.dp
                    val barInsetV = if (pagePlane) MaxSpace.sm else 12.dp
                    Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = barInsetH, vertical = barInsetV)
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                // أقصى عرضٍ للكبسولة على الأجهزة العريضة؛ والملتصق بعرض الشاشة كلها.
                .then(if (docked) Modifier else Modifier.widthIn(max = 440.dp))
                .fillMaxWidth()
                .clip(pillShape)
                .then(
                    if (isBlurEnabled && hazeState != null) {
                        Modifier.hazeEffect(state = hazeState) {
                            blurEffect {
                                blurRadius = 24.dp
                            }
                        }
                    } else Modifier
                ),
            shape = pillShape,
            // The pill owns the bottom of the screen, so it must never be a solid slab:
            // with Haze on, the backdrop is blurred and a light tint is enough; with Haze
            // off, the surface stays translucent so page content is still visibly moving
            // through it, and a hairline border plus elevation keep the pill readable and
            // clearly above the page instead of merged with it.
            color = when {
                pagePlane -> MaterialTheme.colorScheme.surfaceContainerHigh
                isBlurActive -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = FloatingBarTintAlpha)
                else -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = FloatingBarTintAlphaNoBlur)
            },
            border = if (isBlurActive) null else BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = MaxAlpha.border),
            ),
            // وبلا ظلٍّ في «المتكيّف»: الظلّ يقول «أنا فوق الصفحة»، وهو يقول «أنا منها».
            shadowElevation = if (isBlurActive || pagePlane) 0.dp else 10.dp,
        ) {
            // الحشو والفجوة من النمط — لا حرفيًّا في النداء: «الواسع» يتنفّس أكثر، والمدمج
            // أضيق رأسيًّا لأن لا صفّ نصّ تحته.
            val rowPadH = 8.dp
            val rowPadV = if (style == NavBarStyle.LARGE) 12.dp else 8.dp
            val rowGap = 4.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // في «الملتصق» يهبط شريط النظام إلى داخل الوجه هنا (لا على الصندوق).
                    .then(
                        if (docked) {
                            Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = rowPadH, vertical = rowPadV)
                    // بعد الحشو مباشرةً: أصل الرسم هنا هو أصل `positionInParent` للعناصر، فينطبقان.
                    .navSelectionIndicator(itemBounds, selectedRoute, indicatorColor, maxRadius(MaxRadius.tile)),
                horizontalArrangement = Arrangement.spacedBy(rowGap, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val isSelected = selectedRoute == item.route
                    // «الممتدّ»: العنصر النشط يأخذ من العرض ما يحتاجه نصّه، والانتقال بلا قفزة —
                    // والتمدّد هو الفرق البنيويّ في هذا النمط، لا لونٌ آخر.
                    val itemWeight by animateFloatAsState(
                        targetValue = if (style.expandsActive && isSelected) 1.9f else 1f,
                        animationSpec = tween(MaxMotion.standard),
                        label = "studioNavItemWeight",
                    )
                    NavPill(
                        item = item,
                        isSelected = isSelected,
                        isBlurEnabled = isBlurEnabled,
                        indicatorDrawn = true,
                        style = style,
                        onClick = { onItemSelected(item.route) },
                        modifier = Modifier.weight(itemWeight).navItemBounds(itemBounds, item.route),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavPill(
    item: NavItem,
    isSelected: Boolean,
    isBlurEnabled: Boolean = false,
    // الشريط يرسم المؤشّر المشترك خلف العناصر، فلا يرسم العنصر خلفيته بنفسه (وإلا ظهر مؤشّران).
    indicatorDrawn: Boolean = false,
    // قواعد النمط (§٦-ب): النصّ وحجم الأيقونة وأرض اللمس والحشو — تُقرأ ولا تُخمَّن.
    style: NavBarStyle = NavBarStyle.FLOATING,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = rememberMaxHaptics()
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        if (isSelected && !indicatorDrawn) colors.primaryContainer else Color.Transparent,
        tween(MaxMotion.standard), label = "studioNavBackground",
    )
    val foreground by animateColorAsState(
        if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        tween(MaxMotion.fast), label = "studioNavForeground",
    )
    val lift by animateFloatAsState(
        if (isSelected) 1.08f else 1f,
        MaxMotion.controlSpring, label = "studioNavIcon",
    )
    // متى يُكتب النصّ؟ (دائمًا · للنشط وحده · لا نصّ) — والقاعدة من النمط لا من فرع هنا.
    val showLabel = when (style.labels) {
        NavLabelMode.ALWAYS -> true
        NavLabelMode.ACTIVE_ONLY -> isSelected
        NavLabelMode.NONE -> false
    }
    val itemPadH = 3.dp
    Column(
        modifier = modifier
            .maxPressMotion(interactionSource)
            .clip(RoundedCornerShape(maxRadius(MaxRadius.tile)))
            .background(background)
            .selectable(
                selected = isSelected,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {
                    if (!isSelected) haptics.select()
                    onClick()
                },
            )
            .heightIn(min = style.itemMinHeight)
            .padding(horizontal = itemPadH, vertical = style.itemPadV),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(style.iconTextGap, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = item.icon,
            // وبلا نصٍّ ظاهر ينتقل وصف الوجهة إلى الأيقونة نفسها — وإلا صار العنصر زرًّا
            // صامتًا لقارئ الشاشة (وهذا حال [NavLabelMode.NONE] و[NavLabelMode.ACTIVE_ONLY]
            // في غير النشط). ومع النصّ الظاهر يبقى الوصف `null` فلا يُقرأ مرّتين.
            contentDescription = if (showLabel) null else stringResource(item.labelRes),
            tint = foreground,
            modifier = Modifier.size(style.iconSize).scale(lift),
        )
        if (showLabel) {
            Text(
                text = stringResource(item.labelRes),
                style = if (style.largeLabels) {
                    MaterialTheme.typography.labelMedium
                } else {
                    MaterialTheme.typography.labelSmall
                },
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = foreground,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
