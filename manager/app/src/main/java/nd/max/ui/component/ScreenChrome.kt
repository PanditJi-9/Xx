/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.ui.component

/**
 * Shared "chrome" for every screen that isn't Home: one top bar, one section
 * label, used everywhere instead of the twenty-odd near-identical copies that
 * used to live one per screen (same gradient scrim, same LargeFlexibleTopAppBar,
 * same back arrow — only the title string ever changed). Consolidating them
 * here means a single place to evolve the look, and a consistent per-category
 * accent color instead of every icon in the app reading as the same undifferentiated
 * blue chip.
 */

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSectionSpec
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.R
import nd.max.ui.design.maxRadius
import nd.max.ui.theme.ThemeStudio
import nd.max.ui.theme.TopBarStyle

/**
 * An opaque toolbar surface keeps scrolling content from reducing title contrast.
 */
@Composable
fun MaxManagerTopBarScrim(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.surface)
            .drawWithContent {
                drawContent()
                drawLine(
                    color = colorScheme.outlineVariant.copy(alpha = 0.45f),
                    start = androidx.compose.ui.geometry.Offset(0f, size.height),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        content = content
    )
}

/**
 * The screen's category color (tertiary/secondary/primary), provided once at
 * the top of each screen and read by every body-level icon — [LeadingIcon],
 * [SmallLeadingIcon], etc. — so a screen's whole icon set matches its top bar
 * glyph instead of every list icon defaulting to the same flat primary tint.
 * `null` (the default outside any screen) means "no override, use primary".
 */
val LocalScreenAccent = compositionLocalOf<Color?> { null }

/**
 * Wraps [content] so every themed icon inside it picks up [accent] as its
 * default color via [LocalScreenAccent]. Call this once per screen, around
 * the same Scaffold whose top bar receives the identical accent value.
 */
@Composable
fun ScreenAccentProvider(accent: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalScreenAccent provides accent, content = content)
}

/**
 * A small glowing icon chip that sits next to a screen's title, giving each
 * destination a color identity (tertiary for hardware/performance, secondary
 * for display/apps, primary for appearance/meta) instead of the single flat
 * primary tint every leading icon in the app used to share.
 *
 * v2 adds a hairline border and a soft outer halo so the glyph reads as a
 * back-lit indicator lamp on the screen's instrument panel.
 */
@Composable
fun ScreenAccentGlyph(
    icon: ImageVector,
    accent: Color,
    size: androidx.compose.ui.unit.Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.inset))
    Box(
        modifier = modifier
            .size(size + 10.dp)
            .clip(shape)
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.20f), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(size * 0.65f)
        )
    }
}

/**
 * حشو البار الأفقيّ: زرّ الرجوع وأيقونات الإجراء يقعان على خطّ `gutter` نفسه الذي تقع عليه البطاقات،
 * فلا يلتصق شيءٌ منها بحافة الشاشة. مقدار الإزاحة مُشتقّ من القياس لا من التخمين:
 * - `TopAppBar` يضيف 4dp داخليًّا من جهة البداية، فزرّ الرجوع عند 16dp يصير على 20dp = gutter.
 * - زرّ الأيقونة 48dp والرمز 24dp، فالرمز يبعد 12dp عن حدّ الزرّ؛ لذلك إجراء النهاية عند 4dp + 4dp + 12dp = gutter.
 * بلا زرّ رجوع يبدأ العنوان من داخل البار بـ16dp، فتصير الإزاحة 4dp ليقع العنوان على gutter أيضًا.
 */
@Composable
internal fun maxTopBarInsets(hasBack: Boolean): WindowInsets {
    val start = if (hasBack) MaxSpace.gutter - 4.dp else MaxSpace.gutter - 16.dp
    val end = MaxSpace.gutter - 16.dp
    return WindowInsets.statusBars.add(WindowInsets(left = start, right = end))
}

/**
 * The one top bar every sub-screen (everything except Home) renders through — **وهو الباب العامّ
 * لأنماط الشريط العلوي (§٦-أ)**: يقرأ النمط المختار من تفضيلات الاستوديو ويفوّض إلى
 * [MaxStyledTopBar]. فشاشةٌ بُنيت على هيكلها الخاصّ (لا `MaxScreenScaffold`) تكتسب النمط بلا أيّ
 * تعديلٍ في ملفّها — الباب واحد والتفريعات خلفه، ولا شاشة تنفرد بشريطها.
 *
 * **والوصف (`subtitle`) لا يُرسم في الشريط في أيّ نمط**: الهياكل تفتحه من زرّ علامة الاستفهام في طرف الشريط (`withScreenHelp`)، فلا يُكرَّر سطرٌ ولا يُقتطع في شريط ضيّق. والمعامل باقٍ لتوافق المنادين.
 */
@Composable
fun MaxManagerSubScreenTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    title: String,
    onBack: () -> Unit,
    accentIcon: ImageVector? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    actions: @Composable RowScope.() -> Unit = {},
    subtitle: String? = null
) {
    MaxStyledTopBar(
        style = rememberTopBarStyle(),
        scrollBehavior = scrollBehavior,
        title = title,
        onBack = onBack,
        accentIcon = accentIcon,
        accent = accent,
        subtitle = subtitle,
        actions = actions
    )
}

/**
 * التنفيذ الكلاسيكيّ ([TopBarStyle.CLASSIC]) — شريط متوسط بعنوان كبير، ورمز مجال يتقلّص عند
 * الطيّ، والعودة في زرّ مُعبَّأ. كان هذا الشريط الوحيد قبل الأنماط، وهو ما يبقى لمن لم يختر:
 * فغياب المفتاح أو مفتاحٌ لا يُعرف يعودان إليه، فلا يتغيّر شكل شاشةٍ على صاحبه.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassicMediumTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    title: String,
    onBack: () -> Unit,
    accentIcon: ImageVector? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    actions: @Composable RowScope.() -> Unit = {}
) {
    var titleVisible by remember { mutableStateOf(false) }
    LaunchedEffect(title) {
        titleVisible = false
        titleVisible = true
    }

    // Tied 1:1 to the same collapsedFraction Material3 reads internally to shrink/reposition
    // the title row. A separate tween() here would chase a constantly-moving target and drift
    // out of sync with M3's own (un-eased) transform, leaving the icon visibly stranded beside
    // the back arrow for a moment before it finally caught up and popped away.
    val collapsedFraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
    val accentIconAlpha = 1f - collapsedFraction
    val accentIconSlotWidth = 50.dp * accentIconAlpha
    val accentIconSpacerWidth = 12.dp * accentIconAlpha

    // The bar sits in one opaque column together with its scrim, so the header
    // surface stays solid while the content scrolls underneath it.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        MaxManagerTopBarScrim {
            MediumTopAppBar(
                title = {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = titleVisible,
                        enter = fadeIn(tween(260)) + scaleIn(initialScale = 0.96f, animationSpec = tween(260)),
                        label = "subScreenTitleEnter"
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (accentIcon != null) {
                                Box(
                                    modifier = Modifier
                                        .width(accentIconSlotWidth)
                                        .clipToBounds()
                                ) {
                                    ScreenAccentGlyph(
                                        icon = accentIcon,
                                        accent = accent,
                                        modifier = Modifier.graphicsLayer {
                                            alpha = accentIconAlpha
                                            scaleX = 0.94f + (0.06f * accentIconAlpha)
                                            scaleY = 0.94f + (0.06f * accentIconAlpha)
                                        }
                                    )
                                }
                                Spacer(Modifier.width(accentIconSpacerWidth))
                            }
                            Text(
                                text = title,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(maxRadius(MaxRadius.inset))
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                scrollBehavior = scrollBehavior,
                // Consume the real system status-bar inset at the app-bar level.
                // This prevents large titles/actions from sliding underneath the phone
                // status icons while keeping the scrim itself edge-to-edge.
                windowInsets = maxTopBarInsets(hasBack = true)
            )
        }
    }
}

/**
 * نمط الشريط العلوي المختار (§٦-أ) — يُقرأ عند التركيب كقراءة تفضيلات الاستوديو نفسها، ومن قلّب
 * النمط عاد فوجد شاشاته بالنمط الجديد. والقيمة الغائبة (أو مفتاحٌ مجهول) تُقرأ
 * [TopBarStyle.COMPACT_ICON] — أي «أيقونة»، وهو الافتراضي بأمر المالك (٢٠٢٦-١٠-٢٢).
 */
@Composable
fun rememberTopBarStyle(): TopBarStyle {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            ThemeStudio.topBarStyle(context.getSharedPreferences("settings", Context.MODE_PRIVATE))
        }.getOrDefault(TopBarStyle.COMPACT_ICON)
    }
}

/**
 * الشريط العلوي بأنماطه الأربعة — **نقطة الرسم الواحدة**. وبابان يوصلان إليها ولا يكرّرانها:
 * الهياكل الثلاثة في `MaxScreenScaffold` تناديها بالنمط الذي قرأته للتوّ (قراءة واحدة للصفحة)،
 * والباب العامّ [MaxManagerSubScreenTopBar] يقرأ النمط ويفوّض إليها — فتكتسب الأنماطَ شاشاتٌ
 * بُنيت على هياكلها الخاصّة بلا تعديلٍ في ملفّاتها.
 *
 * **وقواعد النمط بياناتٌ على [TopBarStyle]** (صفّ ثابت؟ زرّ رجوع؟ أيقونة مجال؟ شفّاف فوق المحتوى؟)
 * — تُقاس على JVM، والرسم هنا يقرؤها. وكلّها تُبقي الإجراءات (`actions`) فلا نمط يُخفي قدرة.
 *
 * - [TopBarStyle.CLASSIC] كما كان: عنوان كبير يطوي، وأيقونة تتقلّص، وزرّ رجوع مُعبَّأ.
 * - الثلاثة الباقية **صفّ ثابت لا يتمدّد ولا يطوي**، فلا ترسم بـ`scrollBehavior` — والوصف (`subtitle`)
 *   يُفتح من زرّ علامة الاستفهام في طرف الشريط في كل الأنماط، فلا يُرسم في الشريط ولا يُكرَّر.
 *
 * **وتثبيت حدّ الطيّ عند الصفر لازم لا زينة:** الهياكل تربط `exitUntilCollapsedScrollBehavior`
 * بالـ`Scaffold`، وحدّه الافتراضيّ `-Float.MAX_VALUE` لا يغيّره إلا شريطٌ يرسم به. فبلا هذا التثبيت
 * يظلّ الاتصال «يطوي» شريطًا لا وجود له ويبتلع كل دفعة تمرير صاعدة — فلا تتحرّك الصفحة. مع
 * الصفر لا يتغيّر `heightOffset` أبدًا فلا يُستهلك شيء (والكلاسيكيّ يضبط حدّه بنفسه).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaxStyledTopBar(
    style: TopBarStyle,
    scrollBehavior: TopAppBarScrollBehavior,
    title: String,
    onBack: () -> Unit,
    accentIcon: ImageVector? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    if (style.compactBar) {
        SideEffect { scrollBehavior.state.heightOffsetLimit = 0f }
    }
    when (style) {
        TopBarStyle.CLASSIC -> ClassicMediumTopBar(
            scrollBehavior = scrollBehavior,
            title = title,
            onBack = onBack,
            accentIcon = accentIcon,
            accent = accent,
            actions = actions
        )

        TopBarStyle.COMPACT_ICON -> CompactIconTopBar(
            title = title,
            onBack = onBack,
            accentIcon = accentIcon,
            accent = accent,
            actions = actions
        )

        TopBarStyle.TITLE_ONLY -> TitleOnlyTopBar(
            title = title,
            actions = actions
        )

        TopBarStyle.TRANSLUCENT -> TranslucentTopBar(
            title = title,
            onBack = onBack,
            actions = actions
        )
    }
}

/**
 * أرقام التدرّج في [TopBarStyle.TRANSLUCENT] — في موضع واحد مُعلَن لا مبثوثة في الرسم، فمن أراد
 * شريطًا أخفّ أو أثقل غيّر هنا وحده.
 *
 * التدرّج من لون **الصفحة نفسه** (`surface`) لا من لون ثابت، فيصحّ في الفاتح والداكن وAMOLED
 * وكل سمة ديناميكية. وهو عمودي من الأعلى: كثيف خلف شريط الحالة حتى لا تتداخل أيقوناته مع نصّ
 * ينزلق، ثم يخفّ عند صفّ العنوان فيُرى المحتوى خلفه خافتًا، ثم يذوب إلى الشفّاف كليًّا.
 */
private object TranslucentBarSpec {
    /** كثافة اللون عند الحافّة العلوية (خلف شريط الحالة). */
    const val TOP_ALPHA = 0.96f

    /** كثافته عند صفّ العنوان — فوق نصف الكثافة ليبقى العنوان مقروءًا فوق نصٍّ ينزلق تحته. */
    const val TITLE_ALPHA = 0.80f

    /** كثافته عند الحافّة السفلية للشريط، قبل أن يذوب في منطقة التلاشي. */
    const val EDGE_ALPHA = 0.42f

    /** أين يقع صفّ العنوان من ارتفاع الشريط (نسبة) — النقطة الوسطى في التدرّج. */
    const val TITLE_STOP = 0.6f
}

/** عنوان الشريط في الأنماط الثابتة: سطر واحد بخطّ الكلاسيكيّ نفسه، ومعلَّم عنوانًا لقارئ الشاشة. */
@Composable
private fun BarTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** زرّ العودة العاري — النمط الشفّاف لا يُعبّئ الزرّ: يطفو فوق التدرّج بلا صندوق. */
@Composable
private fun PlainBackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
    }
}

/**
 * **مربّع الأيقونة الذي يحلّ محلّ زرّ الرجوع**: بحجم أرض اللمس نفسها (٤٨dp) وبتعبئة الزرّ
 * الكلاسيكيّ وبزواياه، فيقع على خطّ الـgutter حيث كان الزرّ يقع، وتبقى الأيقونة بلون المجال.
 * وهو **علامة لا زرّ**: لا نقر عليه — الرجوع بإيماءة النظام وزرّه.
 */
@Composable
private fun BarGlyphBox(icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier
            .size(MaxSize.minTouchTarget)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(maxRadius(MaxRadius.inset))
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent)
    }
}

/**
 * مدمج بأيقونة ([TopBarStyle.COMPACT_ICON]): الشريط الكلاسيكيّ **وهو مطويّ** — صفّ بارتفاع ثابت
 * وعنوان بجوار مربّع — لكن المربّع يحمل **أيقونة الشاشة** بدل سهم الرجوع. لا يتمدّد ولا يطوي
 * (لا `scrollBehavior`): ما يُرى عند فتح الشاشة هو ما يبقى عند التمرير.
 *
 * وشاشةٌ لم تُمرّر أيقونة مجال تحتفظ بزرّ الرجوع في الخانة نفسها بدل خانة فارغة، فلا يبقى
 * العنوان بلا مرساه ولا تفقد الشاشة طريق العودة داخل التطبيق.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactIconTopBar(
    title: String,
    onBack: () -> Unit,
    accentIcon: ImageVector?,
    accent: Color,
    actions: @Composable RowScope.() -> Unit
) {
    MaxManagerTopBarScrim {
        TopAppBar(
            title = { BarTitle(title) },
            navigationIcon = {
                if (accentIcon != null) {
                    BarGlyphBox(icon = accentIcon, accent = accent)
                } else {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(maxRadius(MaxRadius.inset))
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            windowInsets = maxTopBarInsets(hasBack = true)
        )
    }
}

/**
 * العنوان فقط ([TopBarStyle.TITLE_ONLY]): **اسم الشاشة وحده** — بلا أيقونة ولا زرّ رجوع. صفّ ثابت
 * يبدأ عنوانه على خطّ الـgutter كما تبدأ البطاقات (`hasBack = false`)، والإجراءات إن وُجدت تبقى
 * في النهاية: النمط يسقط الزخرفة لا القدرة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TitleOnlyTopBar(
    title: String,
    actions: @Composable RowScope.() -> Unit
) {
    MaxManagerTopBarScrim {
        TopAppBar(
            title = { BarTitle(title) },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            windowInsets = maxTopBarInsets(hasBack = false)
        )
    }
}

/**
 * شفّاف ومظلَّل ([TopBarStyle.TRANSLUCENT]): **لا سطح معتم** — تدرّج عمودي من لون الصفحة يغطّي
 * الشريط وشريط الحالة ثم يمتدّ تحت حافّته بمقدار `MaxSpace.lg` (فراغ جسم الصفحة العلويّ نفسه)
 * ويذوب إلى الشفّاف. والمحتوى في الهياكل ينزلق **تحت** هذا التدرّج فيخفت عند العنوان بدل أن
 * يُقصّ بحافّة صلبة؛ وعند الراحة لا يغطّي التدرّج شيئًا لأن منطقته الممتدّة هي الفراغ بين الشريط
 * وأوّل عنصر.
 *
 * والتدرّج **رسمٌ لا قياس** (`drawBehind`): لا يزيد ارتفاع الشريط ولا يعترض لمسًا، فما تحت
 * منطقة التلاشي يبقى قابلًا للنقر. وزرّ الرجوع عارٍ فوقه، والعنوان بخطّ الأنماط الثابتة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslucentTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val fadeBelow = MaxSpace.lg
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val total = size.height + fadeBelow.toPx()
                val barEnd = size.height / total
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to surface.copy(alpha = TranslucentBarSpec.TOP_ALPHA),
                        barEnd * TranslucentBarSpec.TITLE_STOP to surface.copy(alpha = TranslucentBarSpec.TITLE_ALPHA),
                        barEnd to surface.copy(alpha = TranslucentBarSpec.EDGE_ALPHA),
                        1f to Color.Transparent,
                        startY = 0f,
                        endY = total
                    ),
                    size = Size(size.width, total)
                )
            }
    ) {
        TopAppBar(
            title = { BarTitle(title) },
            navigationIcon = { PlainBackButton(onBack) },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            windowInsets = maxTopBarInsets(hasBack = true)
        )
    }
}

/**
 * The screen's description: one paragraph with its accent mark. Pages place it as the first
 * item of the body, so it scrolls with the content. It is never part of the top bar.
 *
 * `TextAlign.Start` follows the reading direction (right in RTL, left in LTR), so the mark
 * and the first line of text always sit at the same edge.
 *
 * **والعلامة شرطةُ القسم لا نجمة الذكاء.** كانت `AutoAwesome` — رمز Max AI نفسه — تُرسم أمام
 * وصف **كل** شاشة تقريبًا (`MaxScreen(subtitle=…)` في ١٨ شاشة)، فصارت الواجهة تبدو موسومة
 * بالذكاء حيث لا ذكاء: وهي ملاحظة التقييم الأولى «AI seems to be overused» التي عالجتها §٩.
 * والآن العلامة موتيف المشروع: الشرطة الرأسية بأرقام `MaxSectionSpec.accent*` — نفس أرقام
 * `NeuralSectionHeader`، فصار وصف الشاشة ورأس القسم بالعلامة نفسها.
 */
@Composable
fun MaxScreenDescription(
    text: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(MaxSectionSpec.accentGap)
    ) {
        // الفراغ العلوي من سلّم المسافات (`MaxSpace.xs`) لا رقمًا محلّيًّا: الشرطة تنزل قليلًا
        // لتقف على سطر النصّ الأول لا على حرفه الأعلى.
        Box(
            Modifier
                .padding(top = MaxSpace.xs)
                .width(MaxSectionSpec.accentWidth)
                .height(MaxSectionSpec.accentHeight)
                .clip(RoundedCornerShape(MaxSectionSpec.accentRadius))
                .background(accent)
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )
    }
}

/**
 * The app's one recurring aside mark — **the section rule itself**: a short vertical accent
 * bar in the screen's own colour, shared by every screen instead of a generic glyph that
 * repeated the Max AI sparkle wherever a sentence needed emphasis. `accent` lets each screen
 * tint its own rule so the note visually belongs to the screen it's on.
 */
@Composable
fun MaxManagerInsight(
    text: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(MaxSectionSpec.accentGap)
    ) {
        Box(
            Modifier
                .padding(top = MaxSpace.xs)
                .width(MaxSectionSpec.accentWidth)
                .height(MaxSectionSpec.accentHeight)
                .clip(RoundedCornerShape(MaxSectionSpec.accentRadius))
                .background(accent)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MaxManagerSectionTitle(text: String, accent: Color = MaterialTheme.colorScheme.primary) {
    var visible by remember(text) { mutableStateOf(false) }
    LaunchedEffect(text) {
        visible = false
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(240)) + scaleIn(initialScale = 0.98f, animationSpec = tween(240)),
        label = "sectionTitleEnter"
    ) {
        MaxSectionHeader(
            title = text,
            accent = accent,
            // الفراغان من عقد القسم لا أرقامًا محلّية: هما ما يجعل تباعد قسم-لقسم واحدًا
            // في كل شاشة، والتغيير يجري من مكان واحد.
            modifier = Modifier.padding(
                start = MaxSpace.xs,
                end = MaxSpace.xs,
                top = MaxSectionSpec.spaceBefore,
                bottom = MaxSectionSpec.spaceAfter,
            )
        )
    }
}
