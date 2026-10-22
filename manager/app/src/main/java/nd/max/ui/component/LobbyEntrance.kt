/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.launch
import nd.max.core.gamespace.EntranceFrame
import nd.max.core.gamespace.LobbyCue
import nd.max.core.gamespace.LobbyEntranceTimeline
import nd.max.ui.design.MaxSpace

/*
 * الدخول السينمائيّ للوبي (طلب المالك ٢٠٢٦-١٠-٢١): اندفاع ضوئيّ عبر نفق حلقات ← وميض أبيض ← شعار
 * «M» السداسيّ والعنوان ← تلاشٍ إلى اللوبي. **كله رسم بالكود** (حلقات وخطوط ومسارات) بلون اللعبة
 * المحدَّدة؛ لا فيديو ولا صور ولا شعار مستعار (AGENTS.md §0.4 البند ٤).
 *
 * التوقيت كله من [LobbyEntranceTimeline] (نقيّ ومُختبَر) وصوته `lobby_intro.ogg` المبنيّ على الثوابت نفسها:
 * ذروة الباص تقع لحظة الوميض. المقدّمة تقرأ ساعة واحدة (`clock`) في مرحلتَي الرسم والطبقة فقط، فلا
 * إعادة تركيب أثناء الحركة.
 *
 * التخطّي: لمسة على الشاشة، أو الرجوع، أو أي زرّ ريموت (`skipSignal`) — يخفت الصوت والصورة معًا في
 * [LobbyEntranceTimeline.SKIP_FADE_MS]. وهو الوحيد الذي يستهلك اللمس فوق اللوبي ما دام يعمل.
 */

private val EmblemSize = 112.dp
private const val EMBLEM_SCALE_FROM = 1.22f
private const val RING_COUNT = 14
private const val RING_TRAVEL = 9f
private const val STREAK_COUNT = 56
private const val READY_TIMEOUT_MS = 700L
private val TwoPi = (2.0 * PI).toFloat()

/**
 * طبقة الدخول فوق اللوبي كله.
 *
 * @param title عنوان يظهر تحت الشعار.
 * @param skipLabel وصف إمكانية الوصول لزرّ التخطّي (الشاشة كلّها).
 * @param tone لون اللعبة المحدَّدة، يُقرأ في مرحلة الرسم.
 * @param skipSignal عدّاد يزيده المستدعي ليطلب التخطّي (ريموت/كيبورد)؛ 0 = لا طلب.
 * @param onFinished يُنادى مرّة عند الانتهاء أو بعد تلاشي التخطّي؛ المستدعي يزيل الطبقة.
 */
@Composable
internal fun LobbyEntrance(
    title: String,
    skipLabel: String,
    tone: () -> Color,
    skipSignal: Int,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sounds = LocalLobbySounds.current
    val clock = remember { Animatable(0f) }
    val skipFade = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val finished = rememberUpdatedState(onFinished)
    var skipped by remember { mutableStateOf(false) }
    var stream by remember { mutableIntStateOf(0) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val skip: () -> Unit = {
        if (!skipped) {
            skipped = true
            scope.launch {
                launch { sounds.fadeOutAndStop(stream, LobbyEntranceTimeline.SKIP_FADE_MS) }
                skipFade.animateTo(1f, tween(LobbyEntranceTimeline.SKIP_FADE_MS, easing = LinearEasing))
                finished.value()
            }
        }
    }

    LaunchedEffect(skipSignal) { if (skipSignal > 0) skip() }
    BackHandler(enabled = !skipped) { skip() }

    // الصوت والصورة يبدآن معًا: ننتظر فكّ ترميز العيّنة (≤٧٠٠ms) ثم نُطلق الساعة. إن تأخّرت العيّنة
    // أو كانت الأصوات مُطفأة يمضي الدخول صامتًا بلا انتظار إضافيّ.
    LaunchedEffect(Unit) {
        if (sounds.isEnabled) sounds.awaitReady(LobbyCue.Intro, READY_TIMEOUT_MS)
        if (!skipped) stream = sounds.play(LobbyCue.Intro)
        clock.animateTo(
            LobbyEntranceTimeline.TOTAL_MS.toFloat(),
            tween(LobbyEntranceTimeline.TOTAL_MS, easing = LinearEasing)
        )
        if (!skipped) finished.value()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = LobbyEntranceTimeline.veil(clock.value) * (1f - skipFade.value)
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = skipLabel,
                role = Role.Button,
                onClick = skip
            )
            .drawBehind { drawEntranceScene(clock.value, tone()) },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                val e = LobbyEntranceTimeline.emblem(clock.value)
                alpha = e
                val k = EMBLEM_SCALE_FROM + (1f - EMBLEM_SCALE_FROM) * e
                scaleX = k
                scaleY = k
            }
        ) {
            Canvas(Modifier.size(EmblemSize)) { drawLobbyEmblem(tone()) }
            Spacer(Modifier.height(MaxSpace.md))
            Text(
                text = title,
                color = LobbyPalette.Ink,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                // تباعد الحروف يقطع وصل الحروف العربية؛ يُستعمل في اللغات اللاتينية فقط.
                letterSpacing = if (rtl) 0.sp else 6.sp,
                maxLines = 1
            )
        }
    }
}

// ─────────────────────────────── مشهد الدخول ───────────────────────────────

private fun DrawScope.drawEntranceScene(elapsedMs: Float, accent: Color) {
    val f = LobbyEntranceTimeline.frame(elapsedMs)
    val center = Offset(size.width / 2f, size.height / 2f)
    val reach = max(size.width, size.height)
    drawRect(LobbyPalette.Black)
    if (f.tunnel > 0f) drawTunnel(f, accent, center, reach)
    if (f.emblem > 0f) drawEmblemStage(f.emblem, accent, center, reach)
    if (f.flash > 0f) drawFlash(f.flash, accent, center, reach)
}

/**
 * نفق الاندفاع: حلقات مستطيلة بحجم الشاشة تقترب من المركز نحو الكاميرا (حجمها `d³` فتتسارع كلما
 * قربت)، وخطوط سرعة شعاعيّة تطول بتقدّم الاندفاع. مواضع الخطوط من تجزئة ثابتة (لا عشوائيّة لحظيّة)
 * فيتكرّر المشهد نفسه كل مرّة.
 */
private fun DrawScope.drawTunnel(f: EntranceFrame, accent: Color, center: Offset, reach: Float) {
    val alpha = f.tunnel

    drawRect(
        brush = Brush.radialGradient(
            0f to accent.copy(alpha = 0.50f * f.warp * alpha),
            1f to Color.Transparent,
            center = center,
            radius = reach * 0.65f
        )
    )

    val travel = f.warp * RING_TRAVEL
    for (i in 0 until RING_COUNT) {
        val d = ((i + travel) / RING_COUNT) % 1f
        val s = d * d * d
        val ringW = size.width * (0.015f + 1.45f * s)
        val ringH = size.height * (0.015f + 1.45f * s)
        val fadeIn = (d * 5f).coerceAtMost(1f)
        val fadeOut = 1f - ((d - 0.82f) / 0.18f).coerceIn(0f, 1f)
        val a = fadeIn * fadeOut * alpha * (0.25f + 0.75f * f.warp)
        if (a <= 0.01f) continue
        drawRoundRect(
            color = lerp(accent, Color.White, 0.2f + 0.6f * s).copy(alpha = a.coerceIn(0f, 1f)),
            topLeft = Offset(center.x - ringW / 2f, center.y - ringH / 2f),
            size = Size(ringW, ringH),
            cornerRadius = CornerRadius(min(ringW, ringH) * 0.10f),
            style = Stroke(width = (1f + 5f * s) * density)
        )
    }

    for (j in 0 until STREAK_COUNT) {
        val angle = unit(j, 1) * TwoPi
        val speed = 1.2f + 2.8f * unit(j, 2)
        val base = (unit(j, 3) + f.warp * speed) % 1f
        val inner = base * base * reach * 0.75f
        val length = (0.03f + 0.32f * f.warp) * reach * (0.2f + base)
        val dx = cos(angle)
        val dy = sin(angle)
        val a = (0.12f + 0.70f * f.warp) * base * alpha
        drawLine(
            color = lerp(Color.White, accent, 0.45f * unit(j, 4)).copy(alpha = a.coerceIn(0f, 1f)),
            start = Offset(center.x + dx * inner, center.y + dy * inner),
            end = Offset(center.x + dx * (inner + length), center.y + dy * (inner + length)),
            strokeWidth = (0.8f + 1.8f * base) * density,
            cap = StrokeCap.Round
        )
    }

    drawRect(
        brush = Brush.radialGradient(
            0.55f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.55f * alpha),
            center = center,
            radius = reach * 0.75f
        )
    )
}

/** خلفية الشعار بعد الوميض: توهّج بلون اللعبة وخطّ أفق يمتدّ من المركز ثم يخبو. */
private fun DrawScope.drawEmblemStage(e: Float, accent: Color, center: Offset, reach: Float) {
    drawRect(
        brush = Brush.radialGradient(
            0f to accent.copy(alpha = 0.30f * e),
            1f to Color.Transparent,
            center = center,
            radius = reach * 0.55f
        )
    )
    val line = (e * 2.2f).coerceIn(0f, 1f)
    val lineAlpha = ((1f - e) * 0.9f).coerceIn(0f, 1f)
    if (line > 0f && lineAlpha > 0.01f) {
        val half = size.width * 0.5f * line
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.5f to Color.White.copy(alpha = lineAlpha),
                1f to Color.Transparent,
                startX = center.x - half,
                endX = center.x + half
            ),
            topLeft = Offset(center.x - half, center.y - density),
            size = Size(half * 2f, 2f * density)
        )
    }
}

/** الوميض: قلب أبيض بهالة بلون اللعبة، وشريط أفقيّ ضيّق (لمعة عدسة)، وغسل أبيض عند الذروة. */
private fun DrawScope.drawFlash(flash: Float, accent: Color, center: Offset, reach: Float) {
    drawRect(
        brush = Brush.radialGradient(
            0f to Color.White.copy(alpha = flash),
            0.45f to lerp(accent, Color.White, 0.55f).copy(alpha = 0.65f * flash),
            1f to Color.Transparent,
            center = center,
            radius = reach * (0.30f + 0.80f * flash)
        )
    )
    val band = size.height * 0.014f * (0.4f + flash)
    drawRect(
        brush = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.5f to Color.White.copy(alpha = flash),
            1f to Color.Transparent
        ),
        topLeft = Offset(0f, center.y - band / 2f),
        size = Size(size.width, band)
    )
    drawRect(Color.White.copy(alpha = flash * flash * 0.80f))
}

/**
 * الشعار: سداسيّ بحافة متدرّجة (أزرق ← لون اللعبة ← نيون) يحمل حرف «M» بخطّ واحد مكسور. هندسة بسيطة
 * مرسومة هنا، لا أيقونة التطبيق ولا شعار أي طرف ثالث.
 *
 * `internal` لأن **أيقونة اختصار الشاشة الرئيسية** (`GameSpaceShortcutPinner`) تُرسم بهذه الدالة نفسها
 * على لوحة خارج الشاشة: فيتطابق ما يراه المستخدم على سطح الهاتف مع ما يراه عند الدخول بالتعريف لا
 * بنسخةٍ ثانية تنحرف عنه يومًا.
 */
internal fun DrawScope.drawLobbyEmblem(accent: Color) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension / 2f

    fun hex(radius: Float): Path = Path().apply {
        for (i in 0 until 6) {
            val a = (PI / 3.0 * i - PI / 2.0).toFloat()
            val x = c.x + radius * cos(a)
            val y = c.y + radius * sin(a)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    drawPath(
        path = hex(r * 0.96f),
        brush = Brush.radialGradient(
            0f to accent.copy(alpha = 0.32f),
            1f to accent.copy(alpha = 0.06f),
            center = c,
            radius = r
        )
    )
    drawPath(
        path = hex(r * 0.96f),
        brush = Brush.linearGradient(
            colors = listOf(LobbyPalette.BlueBright, accent, LobbyPalette.Neon),
            start = Offset(c.x - r, c.y - r),
            end = Offset(c.x + r, c.y + r)
        ),
        style = Stroke(width = 3f * density, join = StrokeJoin.Round)
    )
    drawPath(
        path = hex(r * 0.74f),
        color = Color.White.copy(alpha = 0.22f),
        style = Stroke(width = 1.5f * density, join = StrokeJoin.Round)
    )

    val m = Path().apply {
        moveTo(c.x - r * 0.40f, c.y + r * 0.30f)
        lineTo(c.x - r * 0.40f, c.y - r * 0.30f)
        lineTo(c.x, c.y + r * 0.06f)
        lineTo(c.x + r * 0.40f, c.y - r * 0.30f)
        lineTo(c.x + r * 0.40f, c.y + r * 0.30f)
    }
    drawPath(
        path = m,
        color = Color.White,
        style = Stroke(width = 7f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/** تجزئة صحيحة ثابتة → عدد في 0..1؛ تعطي الخطوط مواضع «عشوائية» لكن متطابقة في كل تشغيل. */
private fun unit(index: Int, salt: Int): Float {
    var x = index * 374761393 + salt * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return (x and 0xFFFF) / 65535f
}
