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

/*
 * الشريطان الشيفرونيان خارج `CockpitDeck.kt`: الهندسة (منحنى الوسط · السواد خارج الشريط · المقاطع)
 * ورسمهما. فُصلا لأن الملفّ الأصلي تجاوز سقف الطول الذي تفرضه `tools/code_health.py`، وليس لأن
 * سلوكًا تغيّر — والقصّ نقل حرفي (قيس: لا سطر كود أو تعليق فُقد أو تكرّر).
 *
 * تفصيل الرسم غير مصفوف هنا ثلاث مرّات: انظر الرأس التوضيحي في `CockpitDeck.kt` لسطح اللعبة كلّه،
 * و`deckRail` أدناه لما يرسمه الشريط الواحد مقطعًا مقطعًا.
 *
 * الدوالّ الثلاث `railRightEdge`/`railBlack`/`DeckRails` وحدها `internal` لأن `CockpitDeck.kt`
 * يستدعيها؛ وما بقي (منحنى الوسط · ثوابت المقاطع) `private` كما كان فلا تتّسع الواجهة بلا داع.
 */

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** إطار الشريط: خطّ بعرض ٣dp من حافته + نصف وهج بعرض ٧dp = ٦٫٥dp. */
private const val RAIL_EDGE_PAD = 6.5f

internal fun railRightEdge(y: Float, h: Float, xTop: Float, xApex: Float, xBot: Float, railW: Float): Float =
    railCenterX((y / h).coerceIn(0f, 1f), xTop, xApex, xBot) + railW / 2f + RAIL_EDGE_PAD

private const val RAIL_N = 17
private const val RAIL_APEX = 5.5f / RAIL_N
private const val RAIL_SOFT = 0.035f
private const val RAIL_SAMPLES = 8
private const val RAIL_FRAME_STEPS = 40

/** خطّ وسط الشريط عند الارتفاع النسبي [t]: ساقان مستقيمتان تلتقيان عند القمّة بانحناءة ناعمة كالمرجع. */
private fun railCenterX(t: Float, xTop: Float, xApex: Float, xBot: Float): Float {
    val mTop = (xApex - xTop) / RAIL_APEX
    val mBot = (xBot - xApex) / (1f - RAIL_APEX)
    val u = t - RAIL_APEX
    return xApex + 0.5f * (mTop + mBot) * u + 0.5f * (mBot - mTop) * sqrt(u * u + RAIL_SOFT * RAIL_SOFT)
}

/** السواد خارج الشريط: من حافة الشاشة إلى حافة الشريط الخارجية بحدّ صلب كالمرجع. [reach] نصف العرض + هامش الإطار. */
internal fun DrawScope.railBlack(xTop: Float, xApex: Float, xBot: Float, reach: Float, left: Boolean) {
    val sign = if (left) -1f else 1f
    val startX = if (left) 0f else size.width
    val path = Path().apply {
        moveTo(startX, 0f)
        for (k in 0..RAIL_FRAME_STEPS) {
            val t = k / RAIL_FRAME_STEPS.toFloat()
            lineTo(railCenterX(t, xTop, xApex, xBot) + sign * reach, size.height * t)
        }
        lineTo(startX, size.height)
        close()
    }
    drawPath(path, DeckInk.copy(alpha = 0.96f))
}

/**
 * شريط شيفرون كالمرجع: ١٧ مقطعًا متوازي الأضلاع بفواصل رفيعة، قمّته نحو الوسط عند ٣٢٪ من الارتفاع، وإطار رفيع
 * بوهج خفيف من الجهتين. يمتلئ من الأسفل بقدر [level]؛ المقطع الأخير المضيء أسطع، وعلامة [peak] مخطّطة فوقه،
 * والإضاءة تتدرّج بحسب [sweep] عند الدخول.
 */
private fun DrawScope.deckRail(
    xTop: Float, xApex: Float, xBot: Float, rw: Float, top: Float, bottom: Float,
    level: Float, peak: Float, sweep: Float, tint: Color
) {
    fun cx(t: Float) = railCenterX(t, xTop, xApex, xBot)
    val half = rw / 2f
    val span = bottom - top
    fun yAt(t: Float) = top + span * t
    // مقطع بين tA وtB بحافتين أفقيّتين؛ ومن فيه القمّة يُرسم بنقاط أكثر فينعطف كالمرجع
    fun strip(tA: Float, tB: Float): Path {
        val n = if (RAIL_APEX in tA..tB) RAIL_SAMPLES else 1
        return Path().apply {
            for (k in 0..n) {
                val t = tA + (tB - tA) * k / n
                if (k == 0) moveTo(cx(t) - half, yAt(t)) else lineTo(cx(t) - half, yAt(t))
            }
            for (k in n downTo 0) {
                val t = tA + (tB - tA) * k / n
                lineTo(cx(t) + half, yAt(t))
            }
            close()
        }
    }

    // الإطار الرفيع من الجهتين، ووهج خفيف بلون الوضع
    val framePad = 3.dp.toPx()
    val frameColor = lerp(tint, Color.Black, 0.55f)
    val frameGlow = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val frameLine = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    for (sign in floatArrayOf(-1f, 1f)) {
        val edge = Path().apply {
            for (k in 0..RAIL_FRAME_STEPS) {
                val t = k / RAIL_FRAME_STEPS.toFloat()
                val x = cx(t) + sign * (half + framePad)
                if (k == 0) moveTo(x, yAt(t)) else lineTo(x, yAt(t))
            }
        }
        drawPath(edge, tint.copy(alpha = 0.10f), style = frameGlow)
        drawPath(edge, frameColor.copy(alpha = 0.90f), style = frameLine)
    }

    val step = 1f / RAIL_N
    val lit = if (level <= 0f) 0 else (level.coerceIn(0f, 1f) * RAIL_N).roundToInt().coerceIn(1, RAIL_N)
    val shownLit = (lit * sweep.coerceIn(0f, 1f)).toInt()
    val peakIdx = if (peak > 0.02f) (peak.coerceIn(0f, 1f) * RAIL_N).roundToInt().coerceIn(1, RAIL_N) - 1 else -1
    val glow = Stroke(width = 5.dp.toPx(), join = StrokeJoin.Round)
    val hair = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Round)
    val peakLine = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round)
    for (i in 0 until RAIL_N) {
        val tTop = 1f - (i + 1) * step + step * 0.08f
        val tBot = 1f - i * step - step * 0.08f
        val yTop = yAt(tTop)
        val yBot = yAt(tBot)
        val path = strip(tTop, tBot)
        if (i < shownLit) {
            var c = lerp(lerp(tint, Color.Black, 0.30f), lerp(tint, Color.White, 0.45f), i / (RAIL_N - 1f))
            val leading = i == shownLit - 1
            if (leading) c = lerp(c, Color.White, 0.35f)
            drawPath(path, c.copy(alpha = if (leading) 0.45f else 0.26f), style = glow)
            drawPath(
                path,
                Brush.verticalGradient(
                    listOf(lerp(c, Color.White, 0.14f), lerp(c, Color.Black, 0.10f)), startY = yTop, endY = yBot
                )
            )
        } else {
            drawPath(
                path,
                Brush.verticalGradient(listOf(Color(0xB84A4D54), Color(0xB82E3036)), startY = yTop, endY = yBot)
            )
            drawPath(path, Color.White.copy(alpha = 0.10f), style = hair)
            if (i == peakIdx && i >= lit) drawPath(path, lerp(tint, Color.White, 0.7f).copy(alpha = 0.9f), style = peakLine)
        }
    }
}

/** الشريطان في مكوّن مستقلّ: حركة الذروة تُعيد تركيبه وحده لا اللوحة كلها. */
@Composable
internal fun DeckRails(m: DeckMetrics, cpuLevel: Float?, gpuLevel: Float?, tint: Color, enter: () -> Float) {
    val cpuLvl by animateFloatAsState(cpuLevel ?: 0f, tween(450), label = "cpuRail")
    val gpuLvl by animateFloatAsState(gpuLevel ?: 0f, tween(450), label = "gpuRail")
    val cpuPeak = rememberPeak(cpuLevel)
    val gpuPeak = rememberPeak(gpuLevel)
    Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = enter().coerceIn(0f, 1f) }) {
        val sweep = enter()
        // من حافة الشاشة العلوية إلى السفلية
        val top = 0f
        val bottom = size.height
        deckRail(
            m.railTopL.dp.toPx(), m.railApexL.dp.toPx(), m.railBotL.dp.toPx(), m.railW.dp.toPx(),
            top, bottom, cpuLvl, cpuPeak, sweep, tint
        )
        deckRail(
            m.railTopR.dp.toPx(), m.railApexR.dp.toPx(), m.railBotR.dp.toPx(), m.railW.dp.toPx(),
            top, bottom, gpuLvl, gpuPeak, sweep, tint
        )
    }
}
