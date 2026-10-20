/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
/*
 * الجزيرة الديناميكية — **الكبسولة**: سطحٌ واحد يُرسم فوق التطبيقات، ويُعرض نفسه في المعاينة
 * وفي معرض البطاقات.
 *
 * ### ما تغيّر فيها (تكملة `DYNAMIC-ISLAND-01`)
 *
 * 1. **سوداء افتراضيًّا وحبرٌ مشتقّ** — كانت التعبئة ثابتة (`0xF0101013`) والحدّ بلون الاختيار
 *    بشفافية ٠٫٣٥، فنشأت الشكوى المقيسة: «كبسولة زرقاء بحدّ سماوي». الآن التعبئة هي اللون
 *    المختار نفسه (والافتراضيّ `#000000`)، و**الحبر يُشتقّ منها لا يُختار**: أبيض على الداكن
 *    وأسود على الفاتح. وعلى الأسود الخالص **لا حدّ إطلاقًا** — حافّة المصنع نظيفة، والحدّ لا
 *    يقول شيئًا على سطح يطابق لون النتوء الذي يجلس فيه.
 * 2. **بطاقة لا سطر** — شعار التطبيق (`IslandAppBadge`)، ونبضة صوتية، وبطارية بنسبة حقيقية:
 *    قطعها في `IslandParts`. وهذه هي التي تجعل الكبسولة تُقرأ بطاقةً من رومات المصانع كما
 *    طلب المالك، لا نصًّا في إطار.
 * 3. **عرض بأرضية وسقف** — الكبسولة كانت تتمدّد دائمًا إلى ٧٢٪ لأن `weight(1f)` بـ`fill=true`
 *    يفرض على الصفّ ملء أقصى عرض. اليوم تنكمش مع محتواها بين **أرضية** (كبسولة هادئة بحجم
 *    النتوء ≈ ثلث الشاشة) و**سقف**، والرقمان من `IslandGeometry` وحدهما — ومعهما حشو رأسي
 *    مضاعف يوفّر «أطول/أقصر» بلا خطوة مقاس ثانية تنافس مقاس المستخدم (`Size`).
 *
 * ### ولماذا `IslandCapsule` عامّة
 *
 * نفس قاعدة لوحة الأداء (`HudSurface`): ما يُرسم فوق لعبة يجب أن يكون هو ما تراه في الإعداد.
 * فالكبسولة دالّة واحدة تُستدعى من الخدمة (بالحالة الحيّة)، ومن المعاينة (بمفاتيح الشاشة ودقّة
 * السحب)، ومن معرض البطاقات (بعيّنات ثابتة) — فلا تعرف المعاينة شكلًا يجهله الرسم، ولا يفترق
 * الرسمان عند أوّل تعديل.
 *
 * ### وأساس العرض ليس دائمًا الشاشة
 *
 * نسب العرض تُحسب من `widthBasis`: عرض الشاشة افتراضيًّا (وهو الصحيح فوق التطبيقات)، وعرض
 * مسرح المعاينة حين يُمرَّر — لأن المسرح أضيق من الشاشة بحشوة الصفحة، ولو حُسبت النسبة من
 * الشاشة لظهرت الكبسولة فيه أعرض مما هي عليه حقًّا.
 *
 * ### والحركة خيار لا عادة
 *
 * الدخول والخروج بحركة قصيرة، ويمكن إسكاتها من الإعدادات — من يقرأ على شاشة يلعب عليها لا
 * يجب أن يُفرض عليه انزلاق. وعند الإسكات لا حركة إطلاقًا: قطع لا «تقريب سريع».
 */
package nd.max.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.util.IslandContent
import nd.max.ui.util.IslandGeometry
import nd.max.ui.util.IslandLive
import nd.max.ui.util.IslandPrefs

/** الحبر: أبيض على تعبئة داكنة، وأسود على فاتحة — يُشتقّ من التعبئة نفسها فلا لون نصّ يُختار. */
private val InkOnDark = Color.White
private val InkOnLight = Color.Black

/** ثانويّ على الكبسولة (الفنان، تفصيل الإشعار) — نسبة واحدة تحفظ التسلسل على الحبرين معًا. */
private const val MUTED_ALPHA = 0.62f

/** حدّ التعبئة الملوّنة: شعرة بحبر خفيض — حافة تعريف لا إطار. وعلى الأسود لا حدّ أصلًا. */
private const val EDGE_ALPHA = 0.22f

/**
 * أخضر الشحن — **حالة لا زخرفة**: يُعرض في بطاقة الشحن وحدها، ولا يتبع لون الكبسولة المختار،
 * لأن من اختار كبسولة حمراء ما زال يريد أن يقرأ «يشحن» بالأخضر.
 */
internal val ChargingGreen = Color(0xFF00E676)

/** أسود عمليًّا: ما دون هذا الحدّ لا يُرسم له حدّ — سطح النتوء. */
private const val INK_BLACK_LUMINANCE = 0.05f

/** نسبة الشحن: `%` رمز دوليّ لا يُترجم، والعدد هو الرسالة. */
private fun chargingText(percent: Int): String = "$percent%"

/** فاصل التفصيل: شرطة طويلة تفصل العنوان عمّا يليه بلا تسمية ثانية. */
private const val SEPARATOR_DASH = " — "

/**
 * الكبسولة الحيّة — تُستدعى من `IslandService` (فوق التطبيقات).
 *
 * وتقرأ **حالتها ومفاتيحها** بنفسها: لا وسائط إلزامية، فالموضعان لا يمرّران شيئًا. والحالة
 * تُجمع من `IslandLive` (تُستبدل ولا تتراكم)، والمفاتيح من `IslandLive.config` — وهذا هو
 * الفرق عن قراءة الملفّ داخل الرسم: تغيير اللون أو المقاس يظهر خلال النبضة التالية (٥٠٠ms)
 * بلا إعادة تشغيل، ولو قُرئ الملفّ هنا لتجمّد اللون حتى أوّل تغيّر محتوى.
 */
@Composable
fun IslandSurface(modifier: Modifier = Modifier) {
    val state by IslandLive.state.collectAsState()
    val config by IslandLive.config.collectAsState()
    val prefs = config ?: return
    IslandCapsule(content = state.content, prefs = prefs, modifier = modifier)
}

/**
 * الكبسولة كما يراها المستخدم — بأي محتوى وبأي مفاتيح.
 *
 * @param widthBasis عرض الإطار الذي تُقاس منه النسب (عرض الشاشة افتراضيًّا).
 */
@Composable
fun IslandCapsule(
    content: IslandContent,
    prefs: IslandPrefs.State,
    modifier: Modifier = Modifier,
    widthBasis: Dp? = null
) {
    // النقطة الهادئة: حين لا شيء يُعرض، كانت الكبسولة تختفي تمامًا — وهذا ما جعل المالك يقول
    // «لا يظهر فيها أي شيء»: الجزيرة كانت تعمل، لكن اللحظة كانت هادئة. فصار الهدوء نفسه محتوى.
    val visible = content !is IslandContent.Hidden || prefs.showIdleDot
    val density = LocalDensity.current
    val enter = if (prefs.animate) {
        fadeIn(tween(180)) + slideInVertically(tween(180)) { -it / 2 }
    } else {
        EnterTransition.None
    }
    val exit = if (prefs.animate) {
        fadeOut(tween(140)) + slideOutVertically(tween(140)) { -it / 2 }
    } else {
        ExitTransition.None
    }
    CompositionLocalProvider(
        LocalDensity provides Density(density.density * prefs.size.scale, density.fontScale)
    ) {
        AnimatedVisibility(visible = visible, enter = enter, exit = exit) {
            CapsuleRow(content = content, prefs = prefs, widthBasis = widthBasis, modifier = modifier)
        }
    }
}

/** شكل الكبسولة من المفتاح: كل الأشكال من سلّم المشروع نفسه، فلا رقم جديد يُخترع هنا. */
private fun shapeFor(shape: IslandPrefs.Shape): Shape = when (shape) {
    IslandPrefs.Shape.Pill -> RoundedCornerShape(MaxRadius.pill)
    IslandPrefs.Shape.Soft -> RoundedCornerShape(MaxRadius.group)
    IslandPrefs.Shape.Square -> RoundedCornerShape(MaxRadius.control)
}

/** الحبر المقروء فوق تعبئة: أسودُ على الفاتح وأبيضُ على الداكن. */
private fun inkFor(fill: Color): Color =
    if (fill.luminance() > 0.5f) InkOnLight else InkOnDark

/** أسود خالص (سطوح النتوء) — يُقرأ من الإضاءة لا من مقارنة نصّية بلون مخزَّن. */
private fun isInkBlack(fill: Color): Boolean = fill.luminance() < INK_BLACK_LUMINANCE

@Composable
private fun CapsuleRow(
    content: IslandContent,
    prefs: IslandPrefs.State,
    widthBasis: Dp?,
    modifier: Modifier
) {
    val fill = remember(prefs.colorHex) { parseAccentHex(prefs.colorHex) }
    val ink = remember(fill) { inkFor(fill) }
    val muted = remember(ink) { ink.copy(alpha = MUTED_ALPHA) }
    val shape = shapeFor(prefs.shape)
    val animationsEnabled = rememberAnimationsEnabled()
    val screenWidth = widthBasis ?: LocalConfiguration.current.screenWidthDp.dp
    // مقاس المستخدم مضاعفُ الكثافة داخل الكبسولة كلها (خطًّا وأيقونةً وحشوًا)، فهو يضرب النسب
    // أيضًا — فنُزيحه عن النسب ثم يُعاد ضربه بالكثافة، وإلا جمع المستخدم ضربتين على العرض وحده.
    val sizeScale = prefs.size.scale
    val minWidth = screenWidth * (IslandGeometry.minWidthFraction(prefs.widthScale) / sizeScale)
    val maxWidth = screenWidth * (IslandGeometry.maxWidthFraction(prefs.widthScale) / sizeScale)
    val verticalPadding = MaxSpace.sm * IslandGeometry.verticalPaddingScale(prefs.heightScale)

    Row(
        modifier = modifier
            .animateContentSize()
            .widthIn(min = minWidth, max = maxWidth)
            .clip(shape)
            .background(fill.copy(alpha = prefs.opacity.alpha))
            .then(
                if (isInkBlack(fill)) {
                    Modifier
                } else {
                    Modifier.border(
                        width = MaxSize.hairlineBorder,
                        color = ink.copy(alpha = EDGE_ALPHA),
                        shape = shape
                    )
                }
            )
            .padding(horizontal = MaxSpace.md, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        // المركز لا البداية: كبسولة هادئة بنقطة واحدة كان محتواها يقف على الحافة اليسرى،
        // والمصنع يجعل ما في الكبسولة في وسطها. والفضلة تُوزَّع حول العناصر لا بعدها.
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm, Alignment.CenterHorizontally)
    ) {
        when (content) {
            is IslandContent.Transient -> {
                if (content.packageName.isNullOrBlank() && content.appLabel.isNullOrBlank()) {
                    Dot(ink)
                } else {
                    IslandAppBadge(
                        packageName = content.packageName,
                        label = content.appLabel,
                        tint = ink
                    )
                }
                Text(
                    text = transientLine(content, muted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = ink,
                    style = MaterialTheme.typography.labelMedium,
                    // `fill = false`: النصّ يأخذ ما يحتاجه ويقصّ عند الحدّ، ولا يفرض على الصفّ
                    // أقصى عرض — وهو الفرق المقيس بين «كبسولة تتبع محتواها» و«كبسولة ٧٢٪ دائمًا».
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            is IslandContent.Media -> {
                IslandEqualizer(tint = ink, animated = prefs.animate && animationsEnabled)
                Text(
                    text = mediaLine(content, muted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = ink,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            is IslandContent.Charging -> {
                IslandBattery(
                    percent = content.percent,
                    tint = ChargingGreen,
                    animated = prefs.animate && animationsEnabled
                )
                Text(
                    text = chargingText(content.percent),
                    maxLines = 1,
                    color = ChargingGreen,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace
                )
            }

            is IslandContent.Indicators -> {
                Dot(ink)
                Text(
                    text = indicatorText(content),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = ink,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // النقطة الهادئة — وجود الجزيرة بلا خبر: نقطة واحدة بلون حبرها وبلا نصّ.
            IslandContent.Hidden -> Dot(ink)
        }
    }
}

@Composable
private fun Dot(ink: Color) {
    Box(
        modifier = Modifier
            .size(MaxSpace.sm)
            .clip(CircleShape)
            .background(ink)
    )
}

/**
 * سطر الإشعار: العنوان أوّلًا (وهو ما يُقرأ في عجلة)، ثم تفصيله بلون خفيض.
 *
 * واسم التطبيق **ليس هنا**: يحمله الشعار (`IslandAppBadge`) — أيقونته إن قُرئت وحرفُه الأول
 * إن مُنع، وهو ما تفعله رومات المصانع. وذكره هنا أيضًا كان يقرأ الاسم مرّتين في كبسولة واحدة.
 */
private fun transientLine(content: IslandContent.Transient, muted: Color): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(content.title) }
        content.text?.let { detail ->
            withStyle(SpanStyle(color = muted, fontWeight = FontWeight.Normal)) {
                append(SEPARATOR_DASH)
                append(detail)
            }
        }
    }

/** سطر الوسائط: العنوان عريض والفنان خفيض — بلا أيقونة اسم بديل. */
private fun mediaLine(content: IslandContent.Media, muted: Color): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(content.title) }
        content.artist?.let { artist ->
            withStyle(SpanStyle(color = muted, fontWeight = FontWeight.Normal)) {
                append(SEPARATOR_DASH)
                append(artist)
            }
        }
    }

/**
 * سطر المؤشرات: ما قيس وحده، وما لم يُقس يسقط بلا شرطة.
 *
 * والوحدات رمزية عالمية (`fps` · `%` · `°C`) كما في لوحة الأداء بالحرف — وهي التي تجعل
 * الرقم يُقرأ بلا تسمية بجانبه في كبسولة لا تتّسع لتسميات.
 */
private fun indicatorText(content: IslandContent.Indicators): String = listOfNotNull(
    content.frames?.let { "$it fps" },
    content.cpuPercent?.let { "$it%" },
    content.heatC?.let { "%.0f°C".format(it) }
).joinToString(" · ")
