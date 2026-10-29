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
import nd.max.widget.core.WidgetAccent
import nd.max.widget.core.WidgetIntents
import nd.max.widget.core.WidgetPalette
import nd.max.widget.core.WidgetTheme
import nd.max.widget.core.WidgetThemeMode
import nd.max.widget.openRouteAction
import nd.max.widget.MaxGlanceWidget

/** ناقل ودجة ماكرو الألعاب Game Combo */
class GameComboReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GameComboWidget()
}

/**
 * ودجة «ماكرو الألعاب Game Combos» (٢×١):
 * وصول فوري لاختصارات الكومبو والماكرو والتحكم اللمسي في الألعاب.
 */
class GameComboWidget : MaxGlanceWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun renderGlance(context: Context, id: GlanceId) {
        val palette = WidgetTheme.palette(WidgetAccent.Brand, WidgetThemeMode.System)

        provideContent {
            GameComboContent(palette = palette)
        }
    }
}

@Composable
private fun GameComboContent(
    palette: WidgetPalette,
) {
    val context = LocalContext.current
    val openLobby = openRouteAction(context, WidgetIntents.ROUTE_GAME_LOBBY)

    WidgetRoot(palette = palette) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ActionTile(
                label = context.getString(R.string.widget_game_combo_name),
                iconRes = R.drawable.widget_ic_touch,
                filled = true,
                accent = palette.colorAccent(),
                surface = palette.colorSurface(),
                text = palette.colorText(),
                onClick = openLobby,
                modifier = GlanceModifier.defaultWeight().height(48.dp),
            )
            Spacer(GlanceModifier.width(6.dp))
            ActionTile(
                label = context.getString(R.string.widget_game_launcher_name),
                iconRes = R.drawable.widget_ic_game,
                filled = false,
                accent = palette.colorAccent(),
                surface = palette.colorSurface(),
                text = palette.colorText(),
                onClick = openLobby,
                modifier = GlanceModifier.defaultWeight().height(48.dp),
            )
        }
    }
}
