/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nd.max.R
import nd.max.ui.util.GamePanelPrefs

/**
 * مُنتقي تصميم اللوحة: يظهر **مرة واحدة أول فتح** ثم لا يعود إلا من الإعدادات (أمر المالك).
 *
 * بطاقتان جنباً إلى جنب، وكل بطاقة تحمل رسمها المصغّر المرسوم برمجيًّا — لا صور من أي تطبيق
 * آخر (§0.4)، ولا افتراض صامت: الاختيار يُكتب في متجر `GamePanelPrefs` نفسه ويُوئم «اخُتير».
 */
@Composable
internal fun GamePanelSkinChooser(onPick: (GamePanelPrefs.PanelSkin) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF205070D))
            .padding(TurboPadMd),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TurboPadMd)
        ) {
            Text(
                text = stringResource(R.string.game_panel_skin_choose),
                color = TurboPalette.Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.game_panel_skin_choose_desc),
                color = TurboPalette.Muted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TurboPadMd)) {
                SkinCard(
                    title = stringResource(R.string.game_panel_skin_turbo),
                    desc = stringResource(R.string.game_panel_skin_turbo_desc),
                    tint = TurboPalette.Red,
                    preview = { TurboSkinPreview() },
                    onPick = { onPick(GamePanelPrefs.PanelSkin.Turbo) }
                )
                SkinCard(
                    title = stringResource(R.string.game_panel_skin_classic),
                    desc = stringResource(R.string.game_panel_skin_classic_desc),
                    tint = TurboPalette.Cyan,
                    preview = { ClassicSkinPreview() },
                    onPick = { onPick(GamePanelPrefs.PanelSkin.Classic) }
                )
            }
        }
    }
}

@Composable
private fun SkinCard(
    title: String,
    desc: String,
    tint: Color,
    preview: @Composable () -> Unit,
    onPick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .clip(TurboChamfer)
            .background(TurboPalette.Plate)
            .border(PanelEdge, tint.copy(alpha = 0.55f), TurboChamfer)
            .clickable(role = Role.RadioButton, onClick = onPick)
            .padding(TurboPadSm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        preview()
        Text(text = title, color = TurboPalette.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(
            text = desc,
            color = TurboPalette.Muted,
            fontSize = 8.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/**
 * رسم مصغّر لسطح «Game Turbo»: ترويسة مشطوفة، ثلاثة عدّادات، بلاطات — بيانات شكلية تُمثّل
 * تصميمنا نفسه لا أي منتج آخر.
 */
@Composable
private fun TurboSkinPreview() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(TurboChamferSmall)
            .background(TurboPalette.Back)
            .border(PanelEdge, TurboPalette.Track, TurboChamferSmall)
    ) {
        val w = size.width
        val h = size.height
        // الترويسة: شريط علوي بخط أزرق وعلامة حمراء.
        drawRect(TurboPalette.Plate, size = Size(w, h * 0.16f))
        drawRect(TurboPalette.Red, size = Size(w * 0.07f, h * 0.09f), topLeft = Offset(w * 0.03f, h * 0.035f))
        drawRect(TurboPalette.Blue.copy(alpha = 0.5f), topLeft = Offset(0f, h * 0.16f), size = Size(w, 1.5f))
        // ثلاثة عدّادات: أقواس.
        repeat(3) { index ->
            val cx = w * (0.2f + 0.3f * index)
            val cy = h * 0.40f
            val r = h * 0.11f
            drawArc(
                TurboPalette.Track,
                startAngle = 135f, sweepAngle = 270f, useCenter = false,
                topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2),
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                if (index == 1) TurboPalette.Red else TurboPalette.Blue,
                startAngle = 135f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2),
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        // صفّا البلاطات.
        repeat(2) { row ->
            repeat(5) { col ->
                val tileW = w * 0.155f
                val tileH = h * 0.12f
                drawRoundRect(
                    TurboPalette.Plate,
                    topLeft = Offset(w * 0.035f + col * (tileW + w * 0.022f), h * (0.60f + 0.17f * row)),
                    size = Size(tileW, tileH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
            }
        }
        // الشريط السفلي.
        drawRect(TurboPalette.Plate, topLeft = Offset(0f, h * 0.92f), size = Size(w, h * 0.08f))
        drawRect(TurboPalette.Red, topLeft = Offset(w * 0.03f, h * 0.925f), size = Size(h * 0.05f, h * 0.05f))
    }
}

/** رسم مصغّر للكلاسيكي: لوحة نحيفة، رقائق قراءات، منزلقان. */
@Composable
private fun ClassicSkinPreview() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(TurboChamferSmall)
            .background(Color(0xFF101418))
            .border(PanelEdge, TurboPalette.Track, TurboChamferSmall)
    ) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            TurboPalette.Plate,
            topLeft = Offset(w * 0.05f, h * 0.05f),
            size = Size(w * 0.9f, h * 0.9f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        // سطر العنوان ورقائق القراءات.
        drawRoundRect(
            TurboPalette.Track,
            topLeft = Offset(w * 0.12f, h * 0.14f),
            size = Size(w * 0.5f, h * 0.06f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        )
        repeat(3) { index ->
            drawRoundRect(
                TurboPalette.Track,
                topLeft = Offset(w * 0.12f + index * w * 0.27f, h * 0.28f),
                size = Size(w * 0.22f, h * 0.10f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )
        }
        // منزلقان: مسار ونقطة.
        repeat(2) { index ->
            val y = h * (0.52f + 0.16f * index)
            drawLine(
                TurboPalette.Track,
                start = Offset(w * 0.12f, y),
                end = Offset(w * 0.88f, y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                TurboPalette.Cyan,
                radius = 3.dp.toPx(),
                center = Offset(w * (0.32f + 0.26f * index), y)
            )
        }
        // صفّ بلاطات سفلي.
        repeat(4) { index ->
            drawRoundRect(
                TurboPalette.Track,
                topLeft = Offset(w * 0.12f + index * w * 0.20f, h * 0.78f),
                size = Size(w * 0.16f, h * 0.10f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
        }
    }
}
