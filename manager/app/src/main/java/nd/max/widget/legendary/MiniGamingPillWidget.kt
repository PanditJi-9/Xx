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
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.width
import nd.max.R
import nd.max.widget.ActionTile
import nd.max.widget.WidgetRoot
import nd.max.widget.colorAccent
import nd.max.widget.colorSurface
import nd.max.widget.colorText
import nd.max.widget.core.BoostAction
import nd.max.widget.core.ToggleFpsOverlayAction
import nd.max.widget.core.WidgetAccent
import nd.max.widget.core.WidgetData
import nd.max.widget.core.WidgetIntents
import nd.max.widget.core.WidgetPalette
import nd.max.widget.core.WidgetTheme
import nd.max.widget.core.WidgetThemeMode
import nd.max.widget.openRouteAction
import nd.max.widget.MaxGlanceWidget

/** ناقل ودجة شريط الألعاب المصغر Mini Gaming Pill */
class MiniGamingPillReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MiniGamingPillWidget()
}

/**
 * ودجة «شريط الألعاب المصغّر» (٢×١):
 * كبسولة أنيقة ومضغوطة تحتوي على تحرير الذاكرة وتفعيل عداد FPS وتوربو GPU.
 */
class MiniGamingPillWidget : MaxGlanceWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun renderGlance(context: Context, id: GlanceId) {
        val palette = WidgetTheme.palette(WidgetAccent.Brand, WidgetThemeMode.System)
        val isFpsRunning = WidgetData.isFpsServiceRunning()

        provideContent {
            MiniGamingPillContent(isFpsRunning = isFpsRunning, palette = palette)
        }
    }
}

@Composable
private fun MiniGamingPillContent(
    isFpsRunning: Boolean,
    palette: WidgetPalette,
) {
    val context = LocalContext.current
    val fpsAction = actionRunCallback<ToggleFpsOverlayAction>()
    val boostAction = actionRunCallback<BoostAction>()
    val openGpu = openRouteAction(context, WidgetIntents.ROUTE_GPU_STUDIO)

    WidgetRoot(palette = palette) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ActionTile(
                label = "FPS",
                iconRes = R.drawable.widget_ic_fps,
                filled = isFpsRunning,
                accent = palette.colorAccent(),
                surface = palette.colorSurface(),
                text = palette.colorText(),
                onClick = fpsAction,
                modifier = GlanceModifier.defaultWeight().height(48.dp),
            )
            Spacer(GlanceModifier.width(4.dp))
            ActionTile(
                label = "Boost",
                iconRes = R.drawable.widget_ic_boost,
                filled = false,
                accent = palette.colorAccent(),
                surface = palette.colorSurface(),
                text = palette.colorText(),
                onClick = boostAction,
                modifier = GlanceModifier.defaultWeight().height(48.dp),
            )
            Spacer(GlanceModifier.width(4.dp))
            ActionTile(
                label = "GPU",
                iconRes = R.drawable.widget_ic_gpu,
                filled = true,
                accent = palette.colorAccent(),
                surface = palette.colorSurface(),
                text = palette.colorText(),
                onClick = openGpu,
                modifier = GlanceModifier.defaultWeight().height(48.dp),
            )
        }
    }
}
