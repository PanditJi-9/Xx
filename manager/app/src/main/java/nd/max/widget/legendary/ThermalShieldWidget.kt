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
import nd.max.widget.core.WidgetPalette
import nd.max.widget.core.WidgetTheme
import nd.max.widget.core.WidgetThemeMode
import nd.max.widget.openRouteAction

/** ناقل ودجة درع الحرارة Thermal Shield */
class ThermalShieldReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ThermalShieldWidget()
}

/**
 * ودجة «درع الحرارة Thermal Shield» (٢×٢):
 * مراقبة حرارة الجهاز وحساسات البطارية مع مؤشر ملون لدرجات الحرارة وزر فتح تفاصيل الحرارة.
 */
class ThermalShieldWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val palette = WidgetTheme.palette(WidgetAccent.Brand, WidgetThemeMode.System)
        val temp = WidgetData.temperatureC(context)

        provideContent {
            ThermalShieldContent(temp = temp, palette = palette)
        }
    }
}

@Composable
private fun ThermalShieldContent(
    temp: Float?,
    palette: WidgetPalette,
) {
    val context = LocalContext.current
    val refresh = actionRunCallback<RefreshWidgetsAction>()
    val openThermal = openRouteAction(context, WidgetIntents.ROUTE_THERMAL)

    WidgetRoot(palette = palette, onClick = refresh) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WidgetHeader(
                title = context.getString(R.string.widget_thermal_name),
                subtitle = WidgetFormat.degreesOf(temp),
                iconRes = R.drawable.widget_ic_temp,
                palette = palette,
            )
            Spacer(GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatCell(
                    iconRes = R.drawable.widget_ic_temp,
                    label = context.getString(R.string.widget_stat_temp),
                    value = WidgetFormat.degreesOf(temp),
                    bar = temp?.let { ((it - 30f) / 15f).coerceIn(0f, 1f) },
                    tone = palette.colorAccent(),
                    dim = palette.colorDim(),
                    track = palette.colorTrack(),
                    modifier = GlanceModifier.defaultWeight(),
                    valueSize = 16.sp,
                )
                Spacer(GlanceModifier.width(6.dp))
                ActionTile(
                    label = context.getString(R.string.widget_thermal_name),
                    iconRes = R.drawable.widget_ic_shield,
                    filled = true,
                    accent = palette.colorAccent(),
                    surface = palette.colorSurface(),
                    text = palette.colorText(),
                    onClick = openThermal,
                    modifier = GlanceModifier.defaultWeight().height(52.dp),
                )
            }
        }
    }
}
