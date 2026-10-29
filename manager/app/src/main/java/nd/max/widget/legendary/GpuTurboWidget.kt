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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import nd.max.R
import nd.max.widget.ActionTile
import nd.max.widget.StatCell
import nd.max.widget.VerticalHairline
import nd.max.widget.WidgetHeader
import nd.max.widget.WidgetRoot
import nd.max.widget.colorAccent
import nd.max.widget.colorDim
import nd.max.widget.colorSurface
import nd.max.widget.colorText
import nd.max.widget.colorTrack
import nd.max.widget.core.RefreshWidgetsAction
import nd.max.widget.core.WidgetAccent
import nd.max.widget.core.WidgetData
import nd.max.widget.core.WidgetFormat
import nd.max.widget.core.WidgetIntents
import nd.max.widget.core.WidgetTheme
import nd.max.widget.core.WidgetThemeMode
import nd.max.widget.openRouteAction
import nd.max.widget.MaxGlanceWidget

/** ناقل ودجة مسرّع الرسّام GPU Turbo */
class GpuTurboReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GpuTurboWidget()
}

/**
 * ودجة «مسرّع الرسّام GPU Turbo»:
 * تعرض تردد الرسّام الحي ونسبة التحميل وزرّ القفز المباشر إلى استوديو GPU.
 */
class GpuTurboWidget : MaxGlanceWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun renderGlance(context: Context, id: GlanceId) {
        val palette = WidgetTheme.palette(WidgetAccent.Brand, WidgetThemeMode.System)
        val load = WidgetData.gpuLoad()
        val freq = WidgetData.gpuFreqMhz()

        provideContent {
            GpuTurboContent(load = load, freq = freq, palette = palette)
        }
    }

    internal companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
    }
}

@Composable
private fun GpuTurboContent(
    load: Int?,
    freq: Int?,
    palette: nd.max.widget.core.WidgetPalette,
) {
    val context = LocalContext.current
    val refresh = actionRunCallback<RefreshWidgetsAction>()
    val openStudio = openRouteAction(context, WidgetIntents.ROUTE_GPU_STUDIO)

    WidgetRoot(palette = palette, onClick = refresh) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WidgetHeader(
                title = context.getString(R.string.widget_gpu_name),
                subtitle = freq?.let { context.getString(R.string.widget_freq_mhz, it) } ?: WidgetFormat.DASH,
                iconRes = R.drawable.widget_ic_gpu,
                palette = palette,
            )
            Spacer(GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatCell(
                    iconRes = R.drawable.widget_ic_speed,
                    label = context.getString(R.string.widget_stat_cpu),
                    value = WidgetFormat.percentOf(load),
                    bar = load?.let { it / 100f },
                    tone = palette.colorAccent(),
                    dim = palette.colorDim(),
                    track = palette.colorTrack(),
                    modifier = GlanceModifier.defaultWeight(),
                    valueSize = 16.sp,
                )
                Spacer(GlanceModifier.width(6.dp))
                ActionTile(
                    label = context.getString(R.string.widget_action_gpu_studio),
                    iconRes = R.drawable.widget_ic_control,
                    filled = true,
                    accent = palette.colorAccent(),
                    surface = palette.colorSurface(),
                    text = palette.colorText(),
                    onClick = openStudio,
                    modifier = GlanceModifier.defaultWeight().height(52.dp),
                )
            }
        }
    }
}
