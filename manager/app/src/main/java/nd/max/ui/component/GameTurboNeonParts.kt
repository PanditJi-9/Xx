/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.component

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nd.max.R
import nd.max.core.gamespace.GameThermalProfiles
import nd.max.ui.component.NeonColors as C
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** عرض فضاء التصميم وارتفاعه (إحداثيات الصورة المرجعية). */
internal const val NEON_W = 1342f
internal const val NEON_H = 1135f

/** حالة شريط الحالة السفلي: ما يُقال فيه حقيقيّ دائمًا، و`Idle` لا يدّعي إنجازًا. */
internal enum class NeonBarKind { Idle, Working, Done, Failed }

/**
 * جهة محاذاة النصّ الفعلية: في اللوحة المنعكسة ([mirror]) تتبدّل الجهتان فالنصّ الذي كان يلاصق يسار صندوقه
 * يلاصق يمينه، أمّا الوسط فلا يتغيّر. دالّة نقيّة ليُقاس تبديلها في JVM عادي.
 */
internal fun neonEdge(align: TextAlign, mirror: Boolean): TextAlign = when {
    !mirror || align == TextAlign.Center -> align
    align == TextAlign.End -> TextAlign.Start
    else -> TextAlign.End
}

/** نصّ بإحداثي مطلق يصغّر نفسه تلقائيًا إن ضاق عليه عرضه (ترجمة أطول من الأصل). */
@Composable
internal fun NeonText(
    text: AnnotatedString,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    size: Float,
    color: Color = C.Ink,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start,
    italic: Boolean = false,
    glow: Color? = null,
    alpha: Float = 1f
) {
    val measurer = rememberTextMeasurer()
    // العرض الطبيعيّ يُقاس مرّة لكل نصّ: ما زاد على صندوقه يُصغَّر بنسبة الضيق (حتى ٥٥٪) فلا يُقصّ بـ«…».
    // وليس بـ`didOverflowWidth`: القصّ بالإبهام يُخفي الفيضان عنه فيبقى الخطّ كبيرًا.
    val fit = remember(text, w, size, weight, italic) {
        val natural = measurer.measure(
            text = text,
            style = TextStyle(
                fontSize = size.sp,
                fontWeight = weight,
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                letterSpacing = 0.sp
            ),
            overflow = TextOverflow.Clip,
            softWrap = false,
            maxLines = 1,
            constraints = Constraints()
        ).size.width
        if (natural > w && natural > 0) max(0.55f, w / natural) else 1f
    }
    // في اللوحة المنعكسة (ملتصقة باليمين) يقلب النصّ نفسه حول مركز صندوقه فيبقى مقروءًا، وتتبدّل جهتا
    // المحاذاة: النصّ الذي كان يلاصق يسار صندوقه (بجانب أيقونة على يساره) يلاصق الآن يمينه بجانب أيقونته.
    val mirror = LocalNeonMirror.current
    val edge = neonEdge(align, mirror)
    Box(
        Modifier.at(x, y, w, h).upright(mirror),
        contentAlignment = when (edge) {
            TextAlign.Center -> Alignment.Center
            TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        Text(
            text = text,
            color = color.copy(alpha = alpha),
            fontSize = (size * fit).sp,
            fontWeight = weight,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            // محاذاة **فيزيائية**: لوحة التصميم إحداثياتها بكسلات الصورة، فالنص العربي يبدأ من يسار صندوقه
            // كما في الأصل. `Start` كان سيقلبه إلى اليمين لأن فقرة عربية تبدأ من يمينها.
            textAlign = when (edge) {
                TextAlign.Center -> TextAlign.Center
                TextAlign.End -> TextAlign.Right
                else -> TextAlign.Left
            },
            // بلا تباعد حروف ولا ارتفاع سطر موروثَين من الثيم: كانا يرفعان الأرقام الكبيرة عن موضعها.
            letterSpacing = 0.sp,
            lineHeight = (size * fit * 1.25f).sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = glow?.let { Shadow(it, Offset.Zero, size * 0.5f) }),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun NeonText(
    text: String,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    size: Float,
    color: Color = C.Ink,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start,
    italic: Boolean = false,
    glow: Color? = null,
    alpha: Float = 1f
) = NeonText(AnnotatedString(text), x, y, w, h, size, color, weight, align, italic, glow, alpha)

/**
 * منطقة نقر شفّافة بلا تموّج: التفاعل يُقرأ من تغيّر الرسم نفسه.
 *
 * [label] و[selected] يمنحان المنطقة اسمًا وحالةً لقارئ الشاشة؛ فبدونهما تبقى المساحة الشفّافة زرًّا بلا اسم
 * (النص المرسوم تحتها عُقدة أخرى غير مرتبطة بها).
 */
@Composable
internal fun Modifier.neonClick(
    label: String? = null,
    selected: Boolean? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
): Modifier = semantics(mergeDescendants = true) {
    if (label != null) contentDescription = label
    if (selected != null) this.selected = selected
}.clickable(
    interactionSource = interactionSource ?: remember { MutableInteractionSource() },
    indication = null,
    enabled = enabled,
    role = if (selected != null) Role.RadioButton else Role.Button,
    onClick = onClick
)

// ───────────────────────────── الإطار الثابت ─────────────────────────────

private fun hexagon(cx: Float, cy: Float, r: Float): Path {
    val p = FloatArray(12)
    for (i in 0 until 6) {
        val a = (PI / 3.0 * i).toFloat()
        p[i * 2] = cx + r * cos(a)
        p[i * 2 + 1] = cy + r * sin(a)
    }
    return poly(*p)
}

/** كل ما لا يتغيّر: صفائح الرأس والجسم والأقسام بحوافّ النيون الحمراء ← الزرقاء. يُرسم مرة واحدة. */
@Composable
internal fun NeonFrame() {
    Box(
        Modifier
            .size(NEON_W.dp, NEON_H.dp)
            .drawBehind {
                val edge = Brush.horizontalGradient(
                    0f to C.Red, 0.30f to C.Red, 0.52f to C.Purple, 0.74f to C.Blue, 1f to C.Blue,
                    startX = 20f, endX = 1090f
                )
                // تسرّب الضوء خارج الإطار (الأحمر يسارًا والأزرق يمينًا)
                halo(Offset(8f, 96f), 200f, C.Red, 0.42f)
                halo(Offset(10f, 1096f), 170f, C.Red, 0.32f)
                halo(Offset(1100f, 300f), 280f, C.Blue, 0.30f)
                halo(Offset(1096f, 1010f), 210f, C.Blue, 0.24f)

                // الجسم
                val body = poly(20f, 152f, 1062f, 152f, 1090f, 184f, 1090f, 1082f, 1052f, 1120f, 58f, 1120f, 20f, 1082f)
                drawPath(
                    body,
                    Brush.verticalGradient(0f to Color(0xFF080C1A), 1f to Color(0xFF02040A)),
                    alpha = 0.97f
                )
                var ly = 160f
                while (ly < 1112f) {
                    drawLine(Color.White.copy(alpha = 0.016f), Offset(24f, ly), Offset(1086f, ly), 1f)
                    ly += 7f
                }
                glowStroke(body, edge, 3f, glow = 1.15f)

                // منطقة المحتوى
                val content = poly(214f, 192f, 240f, 166f, 1062f, 166f, 1086f, 190f, 1086f, 1100f, 214f, 1100f)
                drawPath(content, C.NavyHi, alpha = 0.32f)
                glowStroke(content, edge, 2f, glow = 0.6f, alpha = 0.85f)
                drawLine(
                    Brush.verticalGradient(0f to C.Red, 1f to C.Blue, startY = 170f, endY = 1100f),
                    Offset(209f, 172f), Offset(209f, 1098f), 2f, alpha = 0.55f
                )

                // الرأس
                val header = poly(25f, 72f, 77f, 18f, 695f, 18f, 750f, 138f, 62f, 141f, 25f, 127f)
                drawPath(
                    header,
                    Brush.horizontalGradient(0f to Color(0xFF14040A), 0.45f to Color(0xFF04060E), 1f to Color(0xFF030824), startX = 25f, endX = 750f)
                )
                halo(Offset(127f, 84f), 130f, C.Red, 0.28f)
                val headEdge = Brush.horizontalGradient(
                    0f to C.Red, 0.35f to C.Red, 0.6f to C.Purple, 1f to C.Blue, startX = 25f, endX = 750f
                )
                glowStroke(header, headEdge, 3f, glow = 1.25f)
                glowStroke(Path().apply { moveTo(330f, 19f); lineTo(388f, 19f) }, C.RedSoft, 4f, glow = 1.4f, spread = 14f)
                glowStroke(Path().apply { moveTo(560f, 19f); lineTo(642f, 19f) }, C.RedSoft, 4f, glow = 1.4f, spread = 14f)
                drawLine(headEdge, Offset(62f, 147f), Offset(1062f, 147f), 1.5f, alpha = 0.6f)

                // لوحة BOOST
                val boost = poly(770f, 97f, 800f, 59f, 997f, 59f, 1059f, 131f, 800f, 131f)
                drawPath(
                    boost,
                    Brush.horizontalGradient(0f to Color(0xFF14040C), 1f to Color(0xFF040C32), startX = 770f, endX = 1059f)
                )
                val boostEdge = Brush.horizontalGradient(
                    0f to C.Red, 0.45f to C.Red, 0.62f to C.Purple, 1f to C.Blue, startX = 770f, endX = 1059f
                )
                glowStroke(boost, boostEdge, 2.6f, glow = 1.2f)
                val hex = hexagon(827f, 95f, 34f)
                drawPath(hex, Color(0xFF16060E))
                glowStroke(hex, C.Red, 2.6f, glow = 1.1f, spread = 12f)
                val plate = poly(866f, 70f, 1004f, 70f, 1032f, 121f, 866f, 121f)
                drawPath(plate, Color(0xFF071238), alpha = 0.85f)
                clipPath(plate) {
                    var hx = 840f
                    while (hx < 1060f) {
                        drawLine(C.Blue.copy(alpha = 0.14f), Offset(hx, 121f), Offset(hx + 51f, 70f), 1.2f)
                        hx += 9f
                    }
                }

                // شارة الحرارة
                // الأصل مقصوص عند حافة الصورة؛ هنا تُغلق الشارة بحافّة مشطوفة كي تظهر القراءة كاملة («38°C» لا «38°»).
                val temp = poly(1062f, 60f, 1135f, 34f, 1318f, 34f, 1336f, 52f, 1336f, 98f, 1318f, 116f, 1140f, 116f)
                drawPath(temp, Color(0xFF0A0408))
                translate(-9f, 4f) { glowStroke(temp, C.Red, 2f, glow = 0.5f, spread = 8f, alpha = 0.5f) }
                glowStroke(temp, C.Red, 2.6f, glow = 1.1f)

                // قسم «معلومات الأداء»
                val s1 = poly(236f, 198f, 256f, 178f, 1050f, 178f, 1068f, 198f, 1068f, 470f, 236f, 470f)
                val secEdge = Brush.horizontalGradient(0f to C.Red, 0.4f to C.Purple, 1f to C.Blue, startX = 236f, endX = 1068f)
                drawPath(s1, C.Navy, alpha = 0.55f)
                glowStroke(s1, secEdge, 1.8f, glow = 0.5f, alpha = 0.8f)

                // قسم «أدوات اللعبة»
                val s2 = poly(240f, 712f, 262f, 694f, 1050f, 694f, 1068f, 712f, 1068f, 1000f, 240f, 1000f)
                drawPath(s2, C.Navy, alpha = 0.55f)
                glowStroke(s2, secEdge, 1.8f, glow = 0.5f, alpha = 0.8f)
            }
    )
}

// ───────────────────────────── الرأس ─────────────────────────────

@Composable
internal fun NeonHeader(boostOn: Boolean, tempText: String, tempC: Float?, subtitle: String) {
    GlowShape(127f, 84f, 96f, C.Red, haloAlpha = 0.7f) {
        iconRocket(
            body = Brush.linearGradient(listOf(Color.White, Color(0xFFFFB3BC)), Offset.Zero, Offset(100f, 100f)),
            hole = Color(0xFFB0001A),
            flame = C.Red
        )
    }
    NeonText(
        buildAnnotatedString {
            withStyle(SpanStyle(color = C.Ink)) { append("Game ") }
            withStyle(SpanStyle(color = C.Red)) { append("Turbo") }
        },
        250f, 20f, 360f, 72f, 56f, weight = FontWeight.Black, italic = true, glow = C.RedHot.copy(alpha = 0.55f)
    )
    NeonText(subtitle, 250f, 92f, 440f, 40f, 30f, color = C.InkDim)

    // BOOST: مضاءة فقط حين يكون ملفّ الأداء الأعلى فعليًا هو العامل
    val dim = if (boostOn) 1f else 0.4f
    GlowShape(827f, 95f, 44f, C.Red, haloAlpha = 0.6f, dim = dim) {
        iconBolt(Brush.linearGradient(listOf(Color(0xFFFFB0B8), C.Red), Offset.Zero, Offset(100f, 100f)))
    }
    NeonText(
        "BOOST", 868f, 70f, 150f, 52f, 30f,
        color = if (boostOn) C.Red else Color(0xFF7A2A32),
        weight = FontWeight.Black, italic = true, align = TextAlign.Center,
        glow = if (boostOn) C.RedHot else null
    )

    // لون الحرارة يقول الحالة: سماويّ بارد ← كهرمانيّ دافئ ← أحمر ساخن (عتبات عرض لا حدود عتاد)
    val tempTone = when {
        tempC == null -> C.InkDim
        tempC >= 45f -> C.Red
        tempC >= 40f -> C.Amber
        else -> C.Cyan
    }
    val hot = tempC != null && tempC >= 45f
    GlowShape(1184f, 76f, 50f, tempTone, haloAlpha = if (hot) 0.5f else 0.12f) { iconThermometer(tempTone) }
    NeonText(tempText, 1214f, 40f, 112f, 72f, 34f, color = tempTone, weight = FontWeight.Medium)
}

// ───────────────────────────── الشريط الجانبي ─────────────────────────────

private val TabIconY = floatArrayOf(217f, 355f, 495f, 630f, 772f, 917f)
private val TabLabelY = floatArrayOf(266f, 405f, 545f, 684f, 827f, 974f)

/** [available] تقول أي تبويب يعمل فعلًا؛ غير المتاح يُعتَّم ويشرح سبب عدم عمله عند النقر. */
@Composable
internal fun NeonSidebar(
    labels: List<String>,
    available: List<Boolean>,
    recordingLive: Boolean,
    onTab: (Int) -> Unit
) {
    Box(
        Modifier
            .size(NEON_W.dp, NEON_H.dp)
            .drawBehind {
                val plate = poly(25f, 190f, 52f, 163f, 203f, 163f, 203f, 282f, 180f, 305f, 25f, 305f)
                drawPath(
                    plate,
                    Brush.verticalGradient(0f to Color(0xFFB80012), 0.55f to Color(0xFF5A0008), 1f to Color(0xFF260004), startY = 163f, endY = 305f)
                )
                glowStroke(plate, C.Red, 3f, glow = 1.5f, spread = 22f)
            }
    )
    for (i in 0 until 6) {
        val on = i == 0
        val dim = if (available[i]) 1f else 0.55f
        val top = Color.White
        val bottom = if (on) Color(0xFFFFB3BC) else Color(0xFF6FAEFF)
        val glow = if (on) C.Red else C.Blue
        val haloA = if (on) 0.55f else 0.1f
        val cx = 114f
        val cy = TabIconY[i]
        when (i) {
            0 -> GlowIcon(Icons.Rounded.SportsEsports, cx, cy, 62f, top, bottom, glow, haloA)
            1 -> GlowIcon(Icons.Rounded.Speed, cx, cy, 58f, top, bottom, glow, haloA, dim)
            2 -> GlowShape(cx, cy, 58f, glow, haloA, dim) {
                iconCube(Brush.linearGradient(listOf(Color.White, Color(0xFF4F9CFF)), Offset.Zero, Offset(100f, 100f)))
            }
            3 -> if (recordingLive) {
                GlowIcon(Icons.Rounded.Videocam, cx, cy, 58f, Color(0xFFFFD0D4), C.Red, C.Red, 0.6f)
            } else {
                GlowIcon(Icons.Rounded.Videocam, cx, cy, 58f, top, bottom, glow, haloA)
            }
            4 -> GlowShape(cx, cy, 56f, glow, haloA, dim) {
                iconBag(Brush.linearGradient(listOf(Color.White, Color(0xFF4F9CFF)), Offset.Zero, Offset(100f, 100f)), Color(0xFF0A1228))
            }
            else -> GlowIcon(Icons.Rounded.Settings, cx, cy, 58f, top, bottom, glow, haloA)
        }
        NeonText(
            labels[i], 28f, TabLabelY[i] - 21f, 176f, 42f, 23f,
            weight = if (on) FontWeight.Bold else FontWeight.Medium,
            align = TextAlign.Center, alpha = dim
        )
        val top0 = if (i == 0) 163f else TabIconY[i] - 60f
        val bottom0 = if (i == 0) 305f else TabLabelY[i] + 30f
        Box(Modifier.at(25f, top0, 178f, bottom0 - top0).neonClick(label = labels[i], selected = on) { onTab(i) })
    }
}

// ───────────────────────────── المقاييس ─────────────────────────────

private fun ratio(value: Float?, ceiling: Float): Float =
    if (value == null || ceiling <= 0f) 0f else (value / ceiling).coerceIn(0f, 1f)

@Composable
private fun NeonGauge(
    cx: Float,
    cy: Float,
    label: String,
    value: String,
    unit: String?,
    sub: String?,
    fraction: Float,
    red: Boolean,
    series: List<Float> = emptyList(),
    seriesCeiling: Float = 1f
) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "neonGauge")
    val main = if (red) C.Red else C.Blue
    val light = if (red) C.RedSoft else C.BlueHi
    val track = if (red) Color(0xFF2A0A12) else Color(0xFF0E2144)
    val seam = Color(0xFF02040A)
    Box(
        Modifier
            .at(cx - 125f, cy - 125f, 250f, 250f)
            // العدّاد أداة قياس لا تخطيط: اتّجاه ملئه (مع عقارب الساعة) ومحور زمن منحنى الإطارات لا ينعكسان.
            .upright(LocalNeonMirror.current)
            .drawBehind {
                val c = center
                // الحلقة الخارجية الرقيقة وعلاماتها الأربع
                drawCircle(main.copy(alpha = 0.07f), radius = 114f, center = c, style = Stroke(9f))
                drawCircle(main.copy(alpha = 0.55f), radius = 114f, center = c, style = Stroke(1.6f))
                for (k in 0 until 4) {
                    rotate(k * 90f, pivot = c) {
                        drawLine(main.copy(alpha = 0.7f), Offset(c.x + 106f, c.y), Offset(c.x + 122f, c.y), 2f)
                    }
                }
                // المسار: نصف قطر ٩٠ وسُمك ٢٤ ⇒ الشريط من ٧٨ إلى ١٠٢ كما في الأصل
                drawCircle(track, radius = 90f, center = c, style = Stroke(24f))
                if (f > 0.004f) {
                    val a = f.coerceIn(0.02f, 0.99f)
                    val sweep = Brush.sweepGradient(0f to main, a to light, 1f to light, center = c)
                    rotate(120f, pivot = c) {
                        for (k in 3 downTo 1) {
                            drawArc(
                                brush = sweep, startAngle = 0f, sweepAngle = 360f * f, useCenter = false,
                                topLeft = Offset(c.x - 90f, c.y - 90f), size = Size(180f, 180f),
                                alpha = 0.06f * (4 - k), style = Stroke(24f + k * 10f)
                            )
                        }
                        drawArc(
                            brush = sweep, startAngle = 0f, sweepAngle = 360f * f, useCenter = false,
                            topLeft = Offset(c.x - 90f, c.y - 90f), size = Size(180f, 180f),
                            style = Stroke(24f)
                        )
                        // خطّ لامع على حافّة القوس الخارجية
                        drawArc(
                            brush = sweep, startAngle = 0f, sweepAngle = 360f * f, useCenter = false,
                            topLeft = Offset(c.x - 101f, c.y - 101f), size = Size(202f, 202f),
                            alpha = 0.85f, style = Stroke(1.8f)
                        )
                    }
                }
                // فواصل الكتل: خطّ كل ١٠° عبر الشريط، من نقطة البدء (الساعة ٧)
                for (i in 0 until 36) {
                    rotate(120f + i * 10f, pivot = c) {
                        drawLine(seam.copy(alpha = 0.5f), Offset(c.x + 78f, c.y), Offset(c.x + 102f, c.y), 1.5f)
                    }
                }
                // فجوة سوداء ثم القرص الداخلي المتوهّج عند حافّته وحلقته الرقيقة
                drawCircle(seam, radius = 78f, center = c)
                drawCircle(
                    Brush.radialGradient(
                        0f to Color(0xFF02040A), 0.7f to Color(0xFF03060F), 1f to main.copy(alpha = 0.32f),
                        center = c, radius = 69f
                    ),
                    radius = 69f, center = c
                )
                drawCircle(light.copy(alpha = 0.6f), radius = 70.5f, center = c, style = Stroke(1.6f))
                if (series.size >= 2) {
                    val disc = Path().apply {
                        addOval(Rect(c.x - 100f, c.y - 100f, c.x + 100f, c.y + 100f))
                    }
                    clipPath(disc) {
                        val n = series.size
                        val x0 = c.x - 92f
                        val span = 184f
                        val base = c.y + 74f
                        val line = Path()
                        val fill = Path().apply { moveTo(x0, base) }
                        for (i in 0 until n) {
                            val px = x0 + span * i / (n - 1)
                            val py = base - 30f * ratio(series[i], seriesCeiling)
                            if (i == 0) line.moveTo(px, py) else line.lineTo(px, py)
                            fill.lineTo(px, py)
                        }
                        fill.lineTo(x0 + span, base)
                        fill.close()
                        drawPath(fill, Brush.verticalGradient(0f to C.Red.copy(alpha = 0.75f), 1f to C.Red.copy(alpha = 0.04f), startY = base - 30f, endY = base))
                        glowStroke(line, C.RedHot, 2.2f, glow = 1f, spread = 8f)
                    }
                }
            }
    )
    // مراكز النصوص نسبةً إلى مركز الحلقة كما في الأصل: العنوان −٤٠، الرقم +١٢، الوسم +٦٨
    NeonText(label, cx - 60f, cy - 57f, 120f, 34f, 24f, align = TextAlign.Center, alpha = 0.92f)
    NeonText(
        buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 58.sp, fontWeight = FontWeight.Bold)) { append(value) }
            if (unit != null) withStyle(SpanStyle(fontSize = 32.sp, fontWeight = FontWeight.Normal)) { append(unit) }
        },
        cx - 100f, cy - 26f, 200f, 76f, 58f, align = TextAlign.Center, glow = main.copy(alpha = 0.35f)
    )
    // ظلّ أسود حول الوسم: قد يمرّ فوق القوس المضاء عند الحِمل العالي فيبقى مقروءًا
    if (sub != null) NeonText(sub, cx - 80f, cy + 51f, 160f, 34f, 23f, align = TextAlign.Center, alpha = 0.95f, glow = Color.Black.copy(alpha = 0.9f))
}

@Composable
internal fun NeonGauges(
    title: String,
    cpuPct: Int?,
    cpuTag: String?,
    gpuPct: Int?,
    gpuTag: String?,
    fps: Int?,
    frames: List<Float>,
    refreshHz: Int?
) {
    val none = nd.max.ui.design.MAX_VALUE_UNAVAILABLE
    GlowShape(279f, 210f, 44f, C.Red, haloAlpha = 0.4f) { iconPulse(C.Red) }
    NeonText(title, 323f, 192f, 300f, 40f, 23f, weight = FontWeight.Medium)
    NeonGauge(397f, 355f, "CPU", cpuPct?.toString() ?: none, if (cpuPct != null) "%" else null, cpuTag, ratio(cpuPct?.toFloat(), 100f), red = false)
    NeonGauge(668f, 355f, "GPU", gpuPct?.toString() ?: none, if (gpuPct != null) "%" else null, gpuTag, ratio(gpuPct?.toFloat(), 100f), red = true)
    val ceiling = max(refreshHz?.toFloat() ?: 0f, max(frames.maxOrNull() ?: 0f, 60f))
    NeonGauge(
        928f, 355f, "FPS", fps?.toString() ?: none, null, null,
        ratio(fps?.toFloat(), ceiling), red = false,
        series = frames.takeLast(48), seriesCeiling = ceiling
    )
}

// ───────────────────────────── أوضاع الأداء ─────────────────────────────

/**
 * الأوضاع الخمسة ↔ ملفّات سقف الـGPU الخمسة الحقيقية (`GameThermalProfiles`).
 *
 * الترتيب تدرّج من الأقوى إلى الأوفر ثم «افتراضي» (بلا سقف): أحمر ساخن ← عنبريّ ← سماويّ ← أخضر بارد،
 * فيقول اللون رتبة الوضع قبل أن تُقرأ كلمته. ولأن خمسة ألوان معًا ضجيج، **لا يُرى لون الوضع إلا حين يُختار**؛
 * وما عداه رماديّ هادئ بلا توهّج، فيُعرف المختار من نظرة واحدة.
 */
internal enum class NeonMode(
    val id: String,
    val color: Color,
    @StringRes val title: Int,
    @StringRes val sub: Int
) {
    Performance("performance", C.Red, R.string.neon_mode_performance, R.string.neon_mode_performance_sub),
    Gaming("gaming", C.Amber, R.string.neon_mode_gaming, R.string.neon_mode_gaming_sub),
    Balanced("balanced", C.Cyan, R.string.neon_mode_balanced, R.string.neon_mode_balanced_sub),
    Saver("power", C.Green, R.string.neon_mode_saver, R.string.neon_mode_saver_sub),
    Default(GameThermalProfiles.DEFAULT, Color(0xFF9DB2D8), R.string.neon_mode_default, R.string.neon_mode_default_sub)
}

private val IdleIconTop = Color(0xFFD2DAEA)
private val IdleIconBottom = Color(0xFF7F8AA6)

/**
 * [targets] سقف كل وضع بالميغاهرتز على هذا الجهاز (حقيقيّ من `ThermalPanelState.targetsMhz`) فيُرى قبل اللمس؛
 * وما لا قياس له يعرض وصفه النصّيّ. والمختار يعرض حالته الفعلية (جارٍ التطبيق · طُبِّق · لم يُطبَّق) في حبّة ملوّنة.
 */
@Composable
internal fun NeonModes(
    selected: String?,
    targets: Map<String, Int>,
    applying: Boolean,
    statusFor: (String) -> Pair<String, Boolean>?,
    onPick: (String) -> Unit
) {
    // أعلى سقف بين الأوضاع الأربعة المقيسة: أساس طول شريط كل بطاقة
    val maxCap = NeonMode.entries.mapNotNull { targets[it.id] }.maxOrNull()?.takeIf { it > 0 }
    // نبض الحبّة أثناء التطبيق فقط: لا حركة دائمة بلا سبب
    val pulse = if (applying) {
        rememberInfiniteTransition(label = "neonModePulse").animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
            label = "neonModePulseA"
        )
    } else null
    NeonMode.entries.forEachIndexed { i, mode ->
        val title = stringResource(mode.title)
        val x = 245f + 164f * i
        val y = 483f
        val on = mode.id == selected
        val t by animateFloatAsState(if (on) 1f else 0f, tween(180), label = "neonModeSel")
        val status = if (on) statusFor(mode.id) else null
        val failed = status?.second == true
        val cap = targets[mode.id]
        val line = when {
            status != null -> status.first
            on -> stringResource(R.string.neon_mode_selected)
            cap != null -> stringResource(R.string.neon_mode_upto, cap)
            else -> stringResource(mode.sub)
        }
        val accent = if (failed) C.RedHot else mode.color
        val frac = if (cap != null && maxCap != null) (cap.toFloat() / maxCap).coerceIn(0f, 1f) else null
        val src = remember { MutableInteractionSource() }
        val pressed by src.collectIsPressedAsState()
        Box(
            Modifier
                .at(x, y, 152f, 179f)
                .drawBehind {
                    val shape = chamfer(0f, 0f, 152f, 179f, 10f)
                    drawPath(shape, Color(0xFF070A14))
                    // الخامل: إطار رفيع رماديّ، بلا لون ولا توهّج
                    drawPath(shape, Color(0xFF2B3550), alpha = 1f - t, style = Stroke(1.6f))
                    if (t > 0.01f) {
                        drawPath(
                            shape,
                            Brush.verticalGradient(
                                0f to accent.copy(alpha = 0.55f * t),
                                1f to accent.copy(alpha = 0.12f * t)
                            )
                        )
                        halo(Offset(76f, 40f), 120f, accent, 0.28f * t)
                        glowStroke(shape, accent, 3.4f, glow = 1.8f, alpha = t)
                        // حبّة الحالة السفلى (تنبض أثناء التطبيق)
                        val k = if (on) pulse?.value ?: 1f else 1f
                        val pill = chamfer(10f, 130f, 132f, 36f, 8f)
                        drawPath(pill, accent.copy(alpha = 0.32f * t * k))
                        drawPath(pill, accent.copy(alpha = t * k), style = Stroke(1.5f))
                    }
                    // مقياس السقف: طول الشريط = سقف الوضع ÷ أعلى سقف، فتُقرأ الأوضاع الأربعة تدرّجًا بصريًّا
                    if (frac != null) {
                        drawRect(Color(0xFF141B2E), Offset(16f, 170f), Size(120f, 4f))
                        drawRect(if (on) accent else Color(0xFF55627F), Offset(16f, 170f), Size(120f * frac, 4f))
                    }
                    // وميض لحظة الضغط: يُعرف الزرّ الملموس قبل أن يستجيب النظام
                    if (pressed) drawPath(shape, Color.White, alpha = 0.14f)
                }
                .neonClick(label = "$title, $line", selected = on, interactionSource = src) { onPick(mode.id) }
        )
        val cx = x + 76f
        val cy = y + 52f
        val iconHalo = if (on) 0.5f else 0f
        val top = if (on) Color.White else IdleIconTop
        val bottom = if (on) lerp(mode.color, Color.White, 0.45f) else IdleIconBottom
        when (mode) {
            NeonMode.Performance -> GlowShape(cx, cy, 62f, mode.color, iconHalo) {
                if (on) iconRocket(Brush.linearGradient(listOf(Color.White, Color(0xFFFFB3BC)), Offset.Zero, Offset(100f, 100f)), Color(0xFFB0001A), C.Red)
                else iconRocket(Brush.linearGradient(listOf(IdleIconTop, IdleIconBottom), Offset.Zero, Offset(100f, 100f)), Color(0xFF2A3042), IdleIconBottom)
            }
            NeonMode.Saver -> GlowShape(cx, cy, 66f, mode.color, iconHalo) {
                if (on) iconBattery(C.Green, Brush.verticalGradient(listOf(Color(0xFFB6FF70), Color(0xFF2EC20F)), startY = 34f, endY = 66f), Color(0xFF0A1228))
                else iconBattery(IdleIconBottom, Brush.verticalGradient(listOf(IdleIconTop, IdleIconBottom), startY = 34f, endY = 66f), Color(0xFF0A1228))
            }
            NeonMode.Gaming -> GlowIcon(Icons.Rounded.SportsEsports, cx, cy, 72f, top, bottom, mode.color, iconHalo)
            NeonMode.Balanced -> GlowIcon(Icons.Rounded.Balance, cx, cy, 64f, top, bottom, mode.color, iconHalo)
            NeonMode.Default -> GlowIcon(Icons.Rounded.PhoneAndroid, cx, cy, 66f, top, bottom, mode.color, iconHalo)
        }
        NeonText(
            title, x + 4f, y + 80f, 144f, 44f, 23f,
            color = if (on) Color.White else C.InkDim,
            weight = if (on) FontWeight.Bold else FontWeight.Medium,
            align = TextAlign.Center
        )
        NeonText(
            line, x + 12f, y + 130f, 128f, 36f, 17f,
            color = if (on) Color.White else C.InkDim,
            weight = if (on) FontWeight.Medium else FontWeight.Normal,
            align = TextAlign.Center, alpha = if (on) 1f else 0.85f
        )
        if (on) {
            // علامة الاختيار: ✓ على لون الوضع، أو «!» حمراء إن لم يُطبَّق
            val onAccent = if (failed || mode == NeonMode.Performance) Color.White else Color(0xFF05060E)
            GlowShape(x + 128f, y + 24f, 30f, accent, 0.5f) { drawCircle(accent, radius = 50f, center = Offset(50f, 50f)) }
            if (failed) {
                NeonText("!", x + 112f, y + 8f, 32f, 32f, 24f, color = onAccent, weight = FontWeight.Black, align = TextAlign.Center)
            } else {
                GlowIcon(Icons.Rounded.Check, x + 128f, y + 24f, 22f, onAccent, onAccent, accent, 0f)
            }
        }
    }
}

// ───────────────────────────── الأدوات ─────────────────────────────

/** عائلة اللون: الأحمر لأدوات المقاطعة، والأزرق لباقي الأدوات (كما في المرجع). */
internal enum class NeonFamily { Red, Blue }

internal enum class NeonTool(val family: NeonFamily) {
    Notifications(NeonFamily.Red),
    Calls(NeonFamily.Red),
    ScreenRecord(NeonFamily.Blue),
    GameMode(NeonFamily.Blue),
    Brightness(NeonFamily.Blue),
    Colors(NeonFamily.Blue),
    Sound(NeonFamily.Blue),
    TouchLock(NeonFamily.Blue),
    Clean(NeonFamily.Blue),
    Network(NeonFamily.Blue)
}

/** حالة أداة واحدة: [active] مضاءة، [enabled] لها خلفية حقيقية، [busy] قيد التنفيذ. */
internal class NeonToolState(val active: Boolean, val enabled: Boolean, val busy: Boolean = false)

@Composable
internal fun NeonTools(
    title: String,
    state: (NeonTool) -> NeonToolState,
    onTool: (NeonTool) -> Unit
) {
    GlowIcon(Icons.Rounded.SportsEsports, 283f, 720f, 36f, Color(0xFFFFB3BC), C.Red, C.Red, 0.45f)
    NeonText(title, 322f, 700f, 400f, 40f, 23f, weight = FontWeight.Medium)

    val labels = listOf(
        stringResource(R.string.neon_tool_notifications),
        stringResource(R.string.neon_tool_calls),
        stringResource(R.string.neon_tool_record),
        stringResource(R.string.neon_tool_gamemode),
        stringResource(R.string.neon_tool_brightness),
        stringResource(R.string.neon_tool_colors),
        stringResource(R.string.neon_tool_sound),
        stringResource(R.string.neon_tool_touchlock),
        stringResource(R.string.neon_tool_clean),
        stringResource(R.string.neon_tool_network)
    )
    NeonTool.entries.forEachIndexed { i, tool ->
        val row = i / 5
        val col = i % 5
        val x = 255f + 161.5f * col
        val y = if (row == 0) 757f else 880f
        val h = if (row == 0) 104f else 108f
        val st = state(tool)
        val red = tool.family == NeonFamily.Red
        val line = if (red) C.Red else C.BlueHi
        val alpha = if (st.enabled) 1f else 0.58f
        val src = remember { MutableInteractionSource() }
        val pressed by src.collectIsPressedAsState()
        Box(
            Modifier
                .at(x, y, 151f, h)
                .drawBehind {
                    val shape = chamfer(0f, 0f, 151f, h, 9f)
                    if (st.active) {
                        drawPath(
                            shape,
                            Brush.verticalGradient(
                                0f to line.copy(alpha = 0.40f),
                                1f to (if (red) Color(0xFF0D0409) else Color(0xFF050A1C))
                            ),
                            alpha = alpha
                        )
                        glowStroke(shape, line, 2.8f, glow = 1.5f, alpha = alpha)
                    } else {
                        // الخامل مسطّح بلا توهّج: الإضاءة تعني «يعمل» فقط
                        drawPath(shape, Color(0xFF080C18), alpha = alpha)
                        drawPath(shape, if (red) Color(0xFF6A2230) else Color(0xFF2B3F6E), alpha = alpha, style = Stroke(1.5f))
                    }
                    if (pressed) drawPath(shape, Color.White, alpha = 0.14f)
                    // مؤشّر الحالة: مضيء = يعمل · كهرمانيّ = قيد التنفيذ · معتم = متوقّف
                    val led = Offset(151f - 19f, 19f)
                    when {
                        st.busy -> drawCircle(C.Amber, radius = 6f, center = led, alpha = alpha)
                        st.active -> {
                            halo(led, 16f, line, 0.7f * alpha)
                            drawCircle(line, radius = 6f, center = led, alpha = alpha)
                        }
                        else -> drawCircle(Color(0xFF39425A), radius = 5f, center = led, alpha = alpha)
                    }
                }
                .neonClick(label = labels[i], selected = st.active, interactionSource = src) { onTool(tool) }
        )
        val cx = x + 75.5f
        val cy = y + (if (row == 0) 35f else 39f)
        val g = if (red) C.Red else C.Blue
        val top = if (!st.active) IdleIconTop else if (red) Color(0xFFFFC2C8) else Color.White
        val bottom = if (!st.active) IdleIconBottom else if (red) C.Red else Color(0xFF58A6FF)
        val a = if (st.active) 0.5f else 0f
        when (tool) {
            NeonTool.Notifications -> GlowIcon(Icons.Rounded.NotificationsOff, cx, cy, 54f, top, bottom, g, a, alpha)
            NeonTool.Calls -> GlowIcon(Icons.Rounded.PhoneDisabled, cx, cy, 54f, top, bottom, g, a, alpha)
            NeonTool.ScreenRecord -> GlowIcon(Icons.Rounded.Videocam, cx, cy, 58f, top, bottom, g, a, alpha, framed = true)
            NeonTool.GameMode -> GlowIcon(Icons.Rounded.SportsEsports, cx, cy, 56f, top, bottom, g, a, alpha)
            NeonTool.Brightness -> GlowIcon(Icons.Rounded.LightMode, cx, cy, 56f, top, bottom, g, a, alpha)
            NeonTool.Colors -> GlowIcon(Icons.Rounded.Palette, cx, cy, 54f, top, bottom, g, a, alpha)
            NeonTool.Sound -> GlowIcon(Icons.Rounded.GraphicEq, cx, cy, 56f, top, bottom, g, a, alpha)
            NeonTool.TouchLock -> GlowIcon(Icons.Rounded.Lock, cx, cy, 52f, top, bottom, g, a, alpha)
            NeonTool.Clean -> GlowIcon(Icons.Rounded.CleaningServices, cx, cy, 54f, top, bottom, g, a, alpha)
            NeonTool.Network -> GlowIcon(Icons.Rounded.Wifi, cx, cy, 54f, top, bottom, g, a, alpha)
        }
        NeonText(
            labels[i], x + 2f, y + h - 40f, 147f, 32f, 18f, align = TextAlign.Center,
            color = if (st.active) Color.White else C.InkDim, alpha = alpha
        )
    }
}

// ───────────────────────────── شريط الحالة السفلي ─────────────────────────────

@Composable
internal fun NeonStatusBar(kind: NeonBarKind, title: String, sub: String, percent: Int?) {
    val sweep by rememberInfiniteTransition(label = "neonBar").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "neonBarSweep"
    )
    val failed = kind == NeonBarKind.Failed
    val lit = when (kind) {
        NeonBarKind.Done -> (percent ?: 100) / 100f * 9f
        NeonBarKind.Working -> sweep * 9f
        else -> if (percent != null) percent / 100f * 9f else 0f
    }
    Box(
        Modifier
            .size(NEON_W.dp, NEON_H.dp)
            .drawBehind {
                val plate = poly(220f, 1068f, 262f, 1016f, 1000f, 1016f, 1038f, 1068f, 1000f, 1118f, 262f, 1118f)
                drawPath(plate, Color(0xFF05060E), alpha = 0.97f)
                val edge = Brush.horizontalGradient(
                    0f to C.Red, 0.4f to C.Red, 0.62f to C.Purple, 1f to C.Blue, startX = 220f, endX = 1038f
                )
                glowStroke(plate, edge, 2.6f, glow = 1.2f)
                val badge = Offset(292f, 1068f)
                drawCircle(Color(0xFF16060C), radius = 38f, center = badge)
                glowStroke(Path().apply { addOval(Rect(badge, 38f)) }, if (failed) Color(0xFF86101C) else C.Red, 3f, glow = 1.2f, spread = 14f)
                drawCircle(
                    C.Red.copy(alpha = 0.6f), radius = 30f, center = badge,
                    style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
                )
                for (i in 0 until 9) {
                    val seg = slanted(655f + 36.5f * i, 1077f, 30f, 15f, 9f)
                    val t = (lit - i).coerceIn(0f, 1f)
                    val onColor = if (i < 4) lerp(Color(0xFF0066FF), Color(0xFF3A7BFF), i / 3f)
                    else lerp(Color(0xFFFF2D42), Color(0xFFFFE0E2), (i - 4) / 4f)
                    drawPath(seg, Color(0xFF141B33), alpha = 0.85f)
                    if (t > 0f) {
                        glowStroke(seg, onColor, 1f, glow = 0.8f, spread = 7f, alpha = t)
                        drawPath(seg, onColor, alpha = t)
                    }
                }
            }
    )
    GlowShape(292f, 1068f, 40f, C.Red, haloAlpha = 0.5f, dim = if (failed) 0.5f else 1f) {
        iconBolt(Brush.linearGradient(listOf(Color(0xFFFFC0C6), C.Red), Offset.Zero, Offset(100f, 100f)))
    }
    NeonText(title, 365f, 1030f, 285f, 38f, 24f, weight = FontWeight.Medium)
    NeonText(sub, 365f, 1068f, 285f, 32f, 18f, color = C.InkDim)
    NeonText(
        if (percent != null) "$percent%" else nd.max.ui.design.MAX_VALUE_UNAVAILABLE,
        880f, 1028f, 110f, 44f, 30f, weight = FontWeight.Medium, align = TextAlign.End
    )
}
