/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.gamespace

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * خلفية MONOLITH: حقل بازلتي ساكن.
 *
 * ليست سماءً ولا سديمًا ولا توهّجًا بلون اللعبة. سطح واحد، فرق سطوع واحد،
 * وأفق شعرة واحد. اللون كلّه يعيش في الشيء الذي يهمّ: أيقونة اللعبة وحلقة جاهزيتها.
 * لا حركة هنا إطلاقًا — الحركة في الشاشة تُشرح بانتقال، لا بنَفَس لا ينتهي.
 */
@Composable
fun MonolithField(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            Brush.verticalGradient(
                colors = listOf(Monolith.Basalt, Monolith.Obsidian, Monolith.Basalt)
            )
        )
        val horizon = size.height * 0.74f
        drawLine(
            color = Color.White.copy(alpha = 0.035f),
            start = Offset(0f, horizon),
            end = Offset(size.width, horizon),
            strokeWidth = 1f
        )
        // غبار ثابت محسوب من مواضع نسبية: الشكل نفسه في كل إطار، وبلا كلفة حركة.
        for (index in 0 until 11) {
            val fx = ((index * 37) % 97) / 97f
            val fy = ((index * 61) % 83) / 83f
            drawCircle(
                color = Color.White.copy(alpha = if (index % 3 == 0) 0.035f else 0.022f),
                radius = if (index % 4 == 0) 1.2.dp.toPx() else 0.8.dp.toPx(),
                center = Offset(size.width * (0.05f + fx * 0.9f), size.height * (0.06f + fy * 0.6f))
            )
        }
    }
}
