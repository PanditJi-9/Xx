/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/*
 * مؤثّرات اللوبي المشتركة: غلاف الأجواء (سماء ليليّة بنفسجيّة + غبار متصاعد)، وساعة الحركة الواحدة،
 * وتقدّم الدخول، ولمعة الأزرار. كل ما هنا رسم بالكود — لا أصل منقول — ويُقرأ في مرحلة الرسم/الطبقة
 * فلا تُعاد تركيب الشاشة أثناء الحركة.
 *
 * **حلقة مستمرّة واحدة**: [LocalStageClock] تُنشأ مرّة في `GameLobbyScreen` وتغذّي الغبار وطفو البطل
 * ولمعة «Play Now» وأعمدة التقدّم معًا؛ فلا تتصارع حلقات. وهي ثابتة على `0` إن أُوقفت الأنيميشنات
 * من النظام، فيصير كل ما يقرؤها ساكنًا.
 */

private class StageConst(override val value: Float) : State<Float>

/** ساعة المسرح: دورة 0..1 كل [StageClockMs]. كل دالّة موجيّة تأخذ عددًا صحيحًا من الدورات فتلتحم الحلقة بلا قفزة. */
internal val LocalStageClock = staticCompositionLocalOf<State<Float>> { StageConst(0f) }

/** تقدّم الدخول 0..1 بعد غطاء الدخول السينمائيّ؛ يبقى 1 حيث لا دخول. */
internal val LocalStageIntro = staticCompositionLocalOf<State<Float>> { StageConst(1f) }

/** قيمة ثابتة تُستعمل افتراضًا حيث لا مصدر حيّ. */
internal val StageSettled: State<Float> = StageConst(1f)

internal const val StageClockMs = 30_000

private val TwoPi = (2.0 * PI).toFloat()

/** موجة 0..1 بعدد صحيح من الدورات على ساعة المسرح؛ [phase] إزاحة كسريّة. */
internal fun stageWave(t: Float, cycles: Int, phase: Float = 0f): Float =
    0.5f + 0.5f * sin(TwoPi * (t * cycles + phase))

private fun stageUnit(index: Int, salt: Int): Float {
    val v = sin(index * 12.9898f + salt * 78.233f) * 43758.547f
    return v - floor(v)
}

private class Speck(
    val x: Float,
    val y: Float,
    val radius: Float,
    val rise: Int,
    val sway: Float,
    val phase: Float,
    val warm: Boolean,
)

private const val SpeckCount = 34

private val Specks: List<Speck> = List(SpeckCount) { i ->
    Speck(
        x = stageUnit(i, 1),
        y = stageUnit(i, 2),
        radius = 0.0012f + stageUnit(i, 3) * 0.0022f,
        rise = 1 + (stageUnit(i, 4) * 2.99f).toInt(),
        sway = 0.010f + stageUnit(i, 5) * 0.030f,
        phase = stageUnit(i, 6),
        warm = stageUnit(i, 7) > 0.72f
    )
}

/**
 * غبار متصاعد خافت: ٣٤ ذرّة تطفو للأعلى وتتلألأ ببطء. موضعها دالّة في [LocalStageClock] وحدها، فلا حالة ولا
 * تخصيص في الإطار. على طبقة مستقلّة كي لا يُعيد تسجيل الخلفية الثابتة كل إطار.
 */
@Composable
internal fun StageDust(modifier: Modifier = Modifier) {
    val clock = LocalStageClock.current
    Canvas(modifier.graphicsLayer { }) {
        val t = clock.value
        val reach = size.minDimension
        for (s in Specks) {
            val rise = ((s.y - s.rise * t) % 1f + 1f) % 1f
            val x = (s.x + s.sway * sin(TwoPi * (t * 2f + s.phase))) * size.width
            val y = rise * size.height
            val twinkle = 0.45f + 0.55f * stageWave(t, 3, s.phase)
            val edge = sin(PI.toFloat() * rise)
            val tint = if (s.warm) GameSpacePalette.Orange else GameSpacePalette.Lavender
            drawCircle(
                color = tint.copy(alpha = 0.42f * twinkle * edge),
                radius = s.radius * reach,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * أجواء اللوبي كلّها: سماء ليليّة بنفسجيّة، توهّج بنفسجيّ خلف البطل، أفق دافئ، ركنان ورديّ وأزرق، وإطار
 * معتم. تُرسم مرّة خلف المُصفِّح فتتّحد الرئيسية والأقسام في مشهد واحد بدل أن تحمل الرئيسية وحدها الغلاف.
 */
@Composable
internal fun LobbyAtmosphere() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        listOf(
                            GameSpacePalette.Void.copy(alpha = 0.70f),
                            GameSpacePalette.Deep.copy(alpha = 0.90f)
                        )
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(GameSpacePalette.Violet.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.55f, size.height * 0.30f),
                        radius = size.minDimension * 1.0f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(GameSpacePalette.Orange.copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(size.width * 0.50f, size.height * 0.56f),
                        radius = size.minDimension * 0.85f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(GameSpacePalette.Magenta.copy(alpha = 0.09f), Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.98f),
                        radius = size.minDimension * 0.85f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(GameSpacePalette.Blue.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.98f, size.height * 0.02f),
                        radius = size.minDimension * 0.70f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        0.62f to Color.Transparent,
                        1f to GameSpacePalette.Void.copy(alpha = 0.55f),
                        center = center,
                        radius = size.maxDimension * 0.72f
                    )
                )
            }
    )
    StageDust(Modifier.fillMaxSize())
}

/**
 * لمعة قطريّة تعبر المحتوى مرّة كل بضع ثوانٍ ([cycles] مرّات في دورة الساعة). تُرسم فوق المحتوى وداخل القصّ
 * الذي سبقها في السلسلة، فضعها بعد `clip`. تُقرأ الساعة في مرحلة الرسم فقط.
 */
internal fun Modifier.stageSheen(
    clock: State<Float>,
    cycles: Int = 6,
    strength: Float = 0.22f,
): Modifier = drawWithContent {
    drawContent()
    val phase = (clock.value * cycles) % 1f
    val window = 0.34f
    if (phase < window) {
        val p = phase / window
        val band = size.height * 1.6f
        val x = -band + p * (size.width + band * 2f)
        rotate(degrees = 18f, pivot = center) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = strength), Color.Transparent),
                    startX = x,
                    endX = x + band
                ),
                topLeft = Offset(x, -size.height),
                size = Size(band, size.height * 3f)
            )
        }
    }
}

/** ذهبٌ دافئ لرأس الشهاب في إطار الطاقة: لمعة «نادرة» فوق البنفسجيّ، وهي الإشارة البصريّة المألوفة لعناصر الألعاب الأسطوريّة. */
internal val LegendGold = Color(0xFFFFD98A)

/** معجم «الجناح الملكي»: ذهب شامبانيا للحدود والتمييز، وعاج دافئ للنصّ الرئيس، وأبنوس للمفاتيح المضيئة. */
internal val LuxGold = Color(0xFFE6C77A)
internal val LuxGoldDeep = Color(0xFFB8924A)
internal val LuxIvory = Color(0xFFF6F0E3)
internal val LuxObsidian = Color(0xFF120B26)

/** دورات إطار الطاقة في دورة الساعة الواحدة (٣٠ ثانية)، وشدّة الحلقة الخافتة الثابتة تحت الشهابين. */
private const val EnergyRimCycles = 5
private const val EnergyRimBase = 0.22f

/** نافذة عبور اللمعة على الاسم، ونافذة وميض النجمة، كنسبة من دورة كل منهما. */
private const val ShineWindow = 0.22f
private const val GlintWindow = 0.12f

/**
 * إطار طاقة يدور حول لوحٍ بزوايا [corner]: حلقة بسمك [thickness] يدور عليها شهابان (رأس ساطع بذيل يخبو) بلون [tone]
 * ورأس بلون [head]، فوق حلقة خافتة ثابتة. يُقصّ التدرّج المخروطيّ على الحلقة وحدها (الفرق بين مستطيلين مستديرين) فيدور
 * الضوء لا الشكل. تُبنى الحلقة مرّة لكل مقاس، ويُقرأ اللون والساعة في الرسم فقط. يوضع بعد `clip` اللوح كي لا يخرج منه.
 */
internal fun Modifier.stageEnergyRim(
    clock: State<Float>,
    tone: () -> Color,
    corner: Dp,
    thickness: Dp,
    cycles: Int = EnergyRimCycles,
    head: Color = Color.White
): Modifier = drawWithCache {
    val round = corner.toPx()
    val width = thickness.toPx()
    val outer = Path().apply {
        addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(round)))
    }
    val inner = Path().apply {
        addRoundRect(
            RoundRect(
                width,
                width,
                size.width - width,
                size.height - width,
                CornerRadius((round - width).coerceAtLeast(0f))
            )
        )
    }
    val ring = Path.combine(PathOperation.Difference, outer, inner)
    onDrawWithContent {
        drawContent()
        val color = tone()
        val faint = color.copy(alpha = 0f)
        val angle = (clock.value * cycles * 360f) % 360f
        drawPath(ring, color.copy(alpha = EnergyRimBase))
        clipPath(ring) {
            rotate(degrees = angle, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        0.00f to faint,
                        0.22f to faint,
                        0.47f to color.copy(alpha = 0.95f),
                        0.50f to head.copy(alpha = 0.95f),
                        0.51f to faint,
                        0.72f to faint,
                        0.96f to color.copy(alpha = 0.85f),
                        0.98f to head.copy(alpha = 0.80f),
                        0.99f to faint,
                        1.00f to faint,
                        center = center
                    ),
                    radius = size.maxDimension,
                    center = center
                )
            }
        }
    }
}

/**
 * لمعة تعبر الحروف وحدها: طبقة معزولة ثم شريط ضوء بمزج `SrcAtop` فلا يلمع إلا ما رُسم فيها (حروف الاسم لا فراغها).
 * تمرّ [cycles] مرّات في دورة الساعة، ويستغرق العبور [ShineWindow] من دورتها الصغرى.
 */
internal fun Modifier.stageTextShine(clock: State<Float>, cycles: Int = 3, strength: Float = 0.85f): Modifier =
    this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val phase = (clock.value * cycles) % 1f
            if (phase < ShineWindow) {
                val p = phase / ShineWindow
                val band = size.width * 0.38f
                val x = -band + p * (size.width + band * 2f)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0f),
                            Color.White.copy(alpha = strength),
                            Color.White.copy(alpha = 0f)
                        ),
                        startX = x,
                        endX = x + band
                    ),
                    blendMode = BlendMode.SrcAtop
                )
            }
        }

/**
 * وميض نجمة على سطح اللوح (زاوية عند [atX]،[atY] كنسبة من مقاسه): خطّان متعامدان ونقطة تتّسع وتنطفئ في ثانية تقريبًا
 * مرّة كل بضع ثوانٍ، كلمعة على زجاج مصقول. يوضع بعد `clip` اللوح فيُقصّ ما خرج من ذراعيه عن الحواف.
 */
internal fun Modifier.stageGlint(
    clock: State<Float>,
    cycles: Int = 4,
    atX: Float = 0.84f,
    atY: Float = 0.15f
): Modifier = drawWithContent {
    drawContent()
    val phase = (clock.value * cycles + 0.37f) % 1f
    if (phase < GlintWindow) {
        val p = sin(PI.toFloat() * phase / GlintWindow)
        val c = Offset(size.width * atX, size.height * atY)
        val len = size.minDimension * 0.30f * p
        val stroke = size.minDimension * 0.014f
        val star = Color.White.copy(alpha = 0.90f * p)
        drawLine(star, Offset(c.x - len, c.y), Offset(c.x + len, c.y), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(star, Offset(c.x, c.y - len), Offset(c.x, c.y + len), strokeWidth = stroke, cap = StrokeCap.Round)
        drawCircle(Color.White.copy(alpha = 0.60f * p), radius = size.minDimension * 0.045f * p, center = c)
    }
}

private class EmberDot(
    val x: Float,
    val y: Float,
    val radius: Float,
    val rise: Int,
    val sway: Float,
    val phase: Float
)

private const val EmberCount = 26

private val EmberDots: List<EmberDot> = List(EmberCount) { i ->
    EmberDot(
        x = stageUnit(i, 11),
        y = stageUnit(i, 12),
        radius = 0.0035f + stageUnit(i, 13) * 0.0085f,
        rise = 1 + (stageUnit(i, 14) * 2.99f).toInt(),
        sway = 0.015f + stageUnit(i, 15) * 0.040f,
        phase = stageUnit(i, 16)
    )
}

/**
 * جمرات متصاعدة بلون اللعبة خلف البطل: ٢٦ شرارة تطفو للأعلى بثلاث سرعات صحيحة (فتتكرّر الدورة بلا قفزة) وتتلألأ،
 * لكلٍّ هالة من دائرتين مسطّحتين وقلب أبيض — بلا تدرّج مخصَّص في كل إطار فلا ضغط على الرسم. موضعها دالّة في الساعة
 * وحدها كالغبار، فلا حالة ولا تخصيص أثناء الحركة.
 */
@Composable
internal fun StageEmbers(tone: () -> Color, modifier: Modifier = Modifier) {
    val clock = LocalStageClock.current
    Canvas(modifier.graphicsLayer { }) {
        val t = clock.value
        val color = tone()
        val reach = size.minDimension
        for (e in EmberDots) {
            val rise = ((e.y - e.rise * t) % 1f + 1f) % 1f
            val x = (e.x + e.sway * sin(TwoPi * (t * 2f + e.phase))) * size.width
            val y = rise * size.height
            val a = (0.40f + 0.60f * stageWave(t, 3, e.phase)) * sin(PI.toFloat() * rise)
            val r = e.radius * reach
            val at = Offset(x, y)
            drawCircle(color = color.copy(alpha = 0.10f * a), radius = r * 3.4f, center = at)
            drawCircle(color = color.copy(alpha = 0.22f * a), radius = r * 1.9f, center = at)
            drawCircle(color = Color.White.copy(alpha = 0.80f * a), radius = r * 0.55f, center = at)
        }
    }
}
