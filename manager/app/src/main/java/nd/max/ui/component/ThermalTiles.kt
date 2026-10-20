package nd.max.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.maxRadius

/*
 * بروفايل الحرارة في لوحة اللعبة: الشكل نفسه لبلاطات «Thermal & GPU Governor» في الإعدادات
 * (الأيقونات والوسوم والألوان وحدّ المختار)، مُصغَّرًا إلى العمود الأيسر للوحة.
 */

/** أيقونة بروفايل الحرارة: هي أيقونات بطاقات «Thermal & GPU Governor» في الإعدادات نفسها. */
internal fun thermalIcon(id: String): ImageVector = when (id) {
    "power" -> Icons.Filled.BatteryFull
    "balanced" -> Icons.Filled.Balance
    "gaming" -> Icons.Filled.SportsEsports
    "performance" -> Icons.Filled.Speed
    "custom" -> Icons.Filled.Tune
    else -> Icons.Filled.DeviceUnknown
}

/**
 * بلاطة بروفايل الحرارة بشكل بلاطات الإعدادات: حدّ وخلفية بلون الشدّة للمختار، وأيقونة في مربّع
 * ملوّن، ثم الوسم. العرض هنا أضيق فيُصغَّر الوسم حتى يتّسع بدل أن يُقصّ.
 */
@Composable
private fun ThermalTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.inset))
    val fg = if (selected) accent else CubeDim
    Column(
        modifier
            .clip(shape)
            .background(Color(0xFF1F2226))
            .then(if (selected) Modifier.background(accent.copy(alpha = 0.16f)) else Modifier)
            .border(MaxSize.emphasisBorder, if (selected) accent.copy(alpha = 0.5f) else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (selected) {
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(maxRadius(MaxRadius.chip))).background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
            }
        } else {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(3.dp))
        FitText(
            label, fg, maxSp = 10f, minSp = 7f,
            weight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** بروفايل الحرارة: ثلاث بلاطات في الصفّ كما في الإعدادات، وصفّان للستّ بلاطات. */
@Composable
internal fun ThermalGrid(
    options: List<CubeOption>,
    selected: String,
    tileH: Dp,
    onPick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { option ->
                    ThermalTile(
                        icon = option.icon,
                        label = option.label,
                        accent = option.color,
                        selected = option.id == selected,
                        modifier = Modifier.weight(1f).height(tileH),
                        onClick = { onPick(option.id) }
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

