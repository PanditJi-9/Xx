/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset

/**
 * عداد رقمي متدحرج حي (Rolling Ticker Counter) لأرقام الأداء الحية (تردد المعالج، الرام، الحرارة، الإطارات).
 *
 * بدلاً من القفز المفاجئ للقيم، تتدحرج الخانات المتغيرة فقط رأسياً بانسيابية تشبه عدادات أجهزة القياس
 * الدقيقة (Precision Instrumentation Odometer)، مع احترام تفضيل [rememberAnimationsEnabled].
 *
 * **ما أُصلح (عيب بصري ظاهر في لقطة الرئيسية: «28%» بأرقام شبحية متراكبة):**
 *  • كانت الخانة تُزاح **نصف** ارتفاعها بلا قصّ، فيتراكب الرقمان القديم والجديد ويطلعان خارج الخانة.
 *    الآن كل خانة **مقصوصة بحدودها** (`clipToBounds`) والإزاحة **ارتفاع الخانة كاملًا**: يخرج القديم
 *    كليًّا ويدخل الجديد من الجهة المقابلة، فلا يُرى رقمان في المكان نفسه.
 *  • الخانات كانت تُعرَّف بترتيب الفهرس من اليسار، فإذا تغيّر طول الرقم (٩٩ ← ١٠٠) انزاحت هويّات
 *    الخانات كلها وتدحرجت خانات لا علاقة لها. الآن المفتاح **موضع الخانة من اليمين**: الآحاد تبقى
 *    آحادًا، ولا يتدحرج إلا ما تغيّر فعلًا.
 */
@Composable
fun MaxRollingTicker(
    value: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null
) {
    if (!rememberAnimationsEnabled()) {
        Text(
            text = value,
            modifier = modifier,
            style = style,
            color = color,
            fontWeight = fontWeight
        )
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        value.forEachIndexed { index, char ->
            key(value.length - index) {
                if (char.isDigit()) {
                    TickerDigit(char = char, style = style, color = color, fontWeight = fontWeight)
                } else {
                    Text(
                        text = char.toString(),
                        style = style,
                        color = color,
                        fontWeight = fontWeight
                    )
                }
            }
        }
    }
}

/** خانة رقمية واحدة: مقصوصة بحدودها، والإزاحة ارتفاعها كاملًا، فلا يتراكب رقمان. */
@Composable
private fun TickerDigit(
    char: Char,
    style: TextStyle,
    color: Color,
    fontWeight: FontWeight?,
) {
    AnimatedContent(
        targetState = char,
        modifier = Modifier.clipToBounds(),
        transitionSpec = {
            // الارتفاع في القيمة ← الجديد يصعد من الأسفل؛ والهبوط بالعكس.
            val dir = if (targetState > initialState) 1 else -1
            val slide = tween<IntOffset>(MaxMotion.fast, easing = MaxEase.Emphasized)
            // القديم يخفت أسرع مما يظهر الجديد: لحظة التقاطع تكون أخفت ما يمكن.
            (slideInVertically(slide) { h -> h * dir } +
                fadeIn(tween(MaxMotion.fast, delayMillis = 40)))
                .togetherWith(
                    slideOutVertically(slide) { h -> -h * dir } +
                        fadeOut(tween(90))
                )
        },
        label = "tickerDigit"
    ) { target ->
        Text(
            text = target.toString(),
            style = style,
            color = color,
            fontWeight = fontWeight
        )
    }
}
