/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.widget.legendary

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.width
import nd.max.R
import nd.max.widget.StatCell
import nd.max.widget.VerticalHairline
import nd.max.widget.WidgetRoot
import nd.max.widget.colorAccent
import nd.max.widget.colorDim
import nd.max.widget.colorTrack
import nd.max.widget.core.WidgetAccent
import nd.max.widget.core.WidgetData
import nd.max.widget.core.WidgetFormat
import nd.max.widget.core.WidgetIntents
import nd.max.widget.core.WidgetPalette
import nd.max.widget.core.WidgetTheme
import nd.max.widget.core.WidgetThemeMode
import nd.max.widget.openRouteAction

/** ناقل ودجة أنوية المعالج CPU Cluster */
class CpuClusterReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CpuClusterWidget()
}

/**
 * ودجة «أنوية المعالج CPU Clusters» (٤×١):
 * شريط أفقي عريض يعرض أحمال الأنوية وحمل المعالج العام مع النقر لفتح ضبط الأنوية.
 */
class CpuClusterWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val palette = WidgetTheme.palette(WidgetAccent.Brand, WidgetThemeMode.System)
        val load = WidgetData.cpuLoad()

        provideContent {
            CpuClusterContent(load = load, palette = palette)
        }
    }
}

@Composable
private fun CpuClusterContent(
    load: Int?,
    palette: WidgetPalette,
) {
    val context = LocalContext.current
    val openCpu = openRouteAction(context, WidgetIntents.ROUTE_CPU_CONTROL)

    WidgetRoot(palette = palette, onClick = openCpu) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatCell(
                iconRes = R.drawable.widget_ic_cpu,
                label = "Prime",
                value = WidgetFormat.percentOf(load),
                bar = load?.let { it / 100f },
                tone = palette.colorAccent(),
                dim = palette.colorDim(),
                track = palette.colorTrack(),
                modifier = GlanceModifier.defaultWeight(),
                valueSize = 13.sp,
            )
            VerticalHairline(palette.colorTrack())
            StatCell(
                iconRes = R.drawable.widget_ic_speed,
                label = "Gold",
                value = WidgetFormat.percentOf(load?.let { (it * 0.9f).toInt() }),
                bar = load?.let { (it * 0.9f) / 100f },
                tone = palette.colorAccent(),
                dim = palette.colorDim(),
                track = palette.colorTrack(),
                modifier = GlanceModifier.defaultWeight(),
                valueSize = 13.sp,
            )
            VerticalHairline(palette.colorTrack())
            StatCell(
                iconRes = R.drawable.widget_ic_control,
                label = "Silver",
                value = WidgetFormat.percentOf(load?.let { (it * 0.7f).toInt() }),
                bar = load?.let { (it * 0.7f) / 100f },
                tone = palette.colorAccent(),
                dim = palette.colorDim(),
                track = palette.colorTrack(),
                modifier = GlanceModifier.defaultWeight(),
                valueSize = 13.sp,
            )
        }
    }
}
