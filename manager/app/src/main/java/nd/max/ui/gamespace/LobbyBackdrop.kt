/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.gamespace

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/*
 * خلفية Game Space الحيّة.
 *
 * الفكرة التي تجعل الشاشة تبدو مشهدًا لا جدولًا: **فنّ اللعبة نفسها** يملأ الخلفية،
 * مموّهًا حتى يصير ضوءًا لا صورة. ثم لون اللعبة يضيء من زاويتين، وحجاب سفلي
 * يحفظ تباين النصّ فوق كل ذلك.
 *
 * وحدّها المعلن: التمويه يحتاج أندرويد ١٢ (واجهة ٣١) فأعلى؛ على ٢٩–٣٠ يُرسم
 * التدرّج والتوهّج وحدهما بدل أيقونة عملاقة حادّة — الشكل الناقص أصدق من شكل قبيح.
 * ولا شيء يتحرّك هنا: الحركة في الشاشة تُشرح بانتقال، لا بنَفَس لا ينتهي.
 */
@Composable
fun LobbyBackdrop(
    accent: Color,
    icon: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    val painter = remember(icon) { icon?.let { BitmapPainter(it) } }
    Box(modifier) {
        MonolithField(Modifier.fillMaxSize())
        if (painter != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // ١٤٠dp من التمويه تُذيب حدود الأيقونة تمامًا، فتبقى بقعتها اللونية وحدها.
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.22f)
                    .blur(140.dp)
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            val glow = accent.copy(alpha = 0.16f)
            val dim = accent.copy(alpha = 0.07f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glow, Color.Transparent),
                    center = Offset(size.width * 0.84f, size.height * 0.10f),
                    radius = size.height * 0.72f
                ),
                radius = size.height * 0.72f,
                center = Offset(size.width * 0.84f, size.height * 0.10f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(dim, Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.94f),
                    radius = size.height * 0.58f
                ),
                radius = size.height * 0.58f,
                center = Offset(size.width * 0.12f, size.height * 0.94f)
            )
            // حجاب سفلي: النصّ والصفوف تعيش في النصف السفلي، فلا تُنافسها الخلفية.
            drawRect(
                Brush.verticalGradient(
                    0.55f to Color.Transparent,
                    1f to Monolith.Obsidian.copy(alpha = 0.88f)
                )
            )
        }
    }
}
