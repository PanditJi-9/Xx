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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
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
