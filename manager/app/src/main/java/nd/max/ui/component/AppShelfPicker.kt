/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nd.max.core.gamespace.PanelAppChoice
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.maxRadius

/**
 * قائمة التطبيقات السريعة: اختيار متعدّد بالأيقونات، يقف عند [maxPicks]. «الأكثر فتحًا» يمسح التثبيت
 * كلّه فيعود الترتيب إلى التعلّم. تُرسم تحت زرّ التحرير وتحاذي طرفه.
 */
@Composable
internal fun AppShelfPicker(
    title: String,
    hint: String,
    autoLabel: String,
    apps: List<PanelAppChoice>,
    picked: List<String>,
    maxPicks: Int,
    tint: Color,
    anchor: Rect,
    screenW: Dp,
    screenH: Dp,
    density: Float,
    onToggle: (String) -> Unit,
    onAuto: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.control))
    val dropW = 236f
    val listH = minOf(300f, screenH.value * 0.5f)
    val x = (anchor.right / density - dropW).coerceIn(8f, kotlin.math.max(8f, screenW.value - dropW - 8f))
    val below = anchor.bottom / density + 6f
    val totalH = listH + 100f
    val y = if (below + totalH <= screenH.value - 8f) below
    else kotlin.math.max(8f, anchor.top / density - totalH - 6f)
    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onDismiss() } }) {
        Column(
            Modifier.offset(x.dp, y.dp).width(dropW.dp)
                .clip(shape).background(CubeMenuBg).border(MaxSize.hairlineBorder, tint.copy(alpha = 0.55f), shape)
                .swallowTaps().padding(6.dp)
        ) {
            FitText(
                title, CubeDim, maxSp = 11f, minSp = 8f, weight = FontWeight.Bold, align = TextAlign.Left,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
            )
            FitText(
                hint, CubeDim, maxSp = 10f, minSp = 8f, align = TextAlign.Left,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Row(
                Modifier.fillMaxWidth().height(MaxSize.minTouchTarget).clip(shape)
                    .clickable(role = Role.Button) { onAuto() }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Autorenew, null, tint = tint, modifier = Modifier.size(18.dp))
                FitText(
                    autoLabel, CubeInk, maxSp = 12f, minSp = 9f, weight = FontWeight.Medium,
                    align = TextAlign.Left, modifier = Modifier.weight(1f)
                )
                if (picked.isEmpty()) Icon(Icons.Rounded.Check, null, tint = CubeInk, modifier = Modifier.size(16.dp))
            }
            LazyColumn(Modifier.fillMaxWidth().height(listH.dp)) {
                items(apps, key = { it.pkg }) { app ->
                    val on = app.pkg in picked
                    val blocked = !on && picked.size >= maxPicks
                    Row(
                        Modifier.fillMaxWidth().height(MaxSize.minTouchTarget)
                            .clickable(enabled = !blocked, role = Role.Checkbox) { onToggle(app.pkg) }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppIconImage(packageName = app.pkg, size = 26.dp, contentDescription = null)
                        FitText(
                            app.label, if (blocked) CubeDim else CubeInk, maxSp = 12f, minSp = 9f,
                            weight = FontWeight.Medium, align = TextAlign.Left, modifier = Modifier.weight(1f)
                        )
                        if (on) Icon(Icons.Rounded.Check, null, tint = tint, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
