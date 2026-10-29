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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import nd.max.R
import nd.max.island.engine.IslandService
import nd.max.widget.ActionTile
import nd.max.widget.WidgetHeader
import nd.max.widget.WidgetRoot
import nd.max.widget.colorAccent
import nd.max.widget.colorSurface
import nd.max.widget.colorText
import nd.max.widget.core.IslandToggleAction
import nd.max.widget.core.RefreshWidgetsAction
import nd.max.widget.core.WidgetAccent
import nd.max.widget.core.WidgetIntents
import nd.max.widget.core.WidgetPalette
import nd.max.widget.core.WidgetTheme
import nd.max.widget.core.WidgetThemeMode
import nd.max.widget.openRouteAction
import nd.max.widget.MaxGlanceWidget

/** ناقل ودجة ريموت الجزيرة Island Remote */
class IslandRemoteReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IslandRemoteWidget()
}

/**
 * ودجة «ريموت الجزيرة Island Remote» (٢×٢):
 * تحكّم فوري بتشغيل أو إيقاف الجزيرة التفاعلية مع مؤشر حي لحالتها الحالية واختصار للإعدادات.
 */
class IslandRemoteWidget : MaxGlanceWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun renderGlance(context: Context, id: GlanceId) {
        val palette = WidgetTheme.palette(WidgetAccent.Brand, WidgetThemeMode.System)
        val active = runCatching { IslandService.isActive() }.getOrDefault(false)

        provideContent {
            IslandRemoteContent(active = active, palette = palette)
        }
    }
}

@Composable
private fun IslandRemoteContent(
    active: Boolean,
    palette: WidgetPalette,
) {
    val context = LocalContext.current
    val toggleAction = actionRunCallback<IslandToggleAction>()
    val openIsland = openRouteAction(context, WidgetIntents.ROUTE_ISLAND)

    WidgetRoot(palette = palette) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WidgetHeader(
                title = context.getString(R.string.widget_island_remote_name),
                subtitle = if (active) context.getString(R.string.widget_status_on) else context.getString(R.string.widget_status_off),
                iconRes = R.drawable.widget_ic_island,
                palette = palette,
            )
            Spacer(GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionTile(
                    label = if (active) context.getString(R.string.widget_status_on) else context.getString(R.string.widget_status_off),
                    iconRes = R.drawable.widget_ic_island,
                    filled = active,
                    accent = palette.colorAccent(),
                    surface = palette.colorSurface(),
                    text = palette.colorText(),
                    onClick = toggleAction,
                    modifier = GlanceModifier.defaultWeight().height(52.dp),
                )
                Spacer(GlanceModifier.width(6.dp))
                ActionTile(
                    label = context.getString(R.string.dynamic_island_title),
                    iconRes = R.drawable.widget_ic_control,
                    filled = false,
                    accent = palette.colorAccent(),
                    surface = palette.colorSurface(),
                    text = palette.colorText(),
                    onClick = openIsland,
                    modifier = GlanceModifier.defaultWeight().height(52.dp),
                )
            }
        }
    }
}
