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

package nd.max.widget.controls

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
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
import nd.max.widget.Footnote
import nd.max.widget.WidgetRoot
import nd.max.widget.colorAccent
import nd.max.widget.colorDim
import nd.max.widget.colorSurface
import nd.max.widget.colorText
import nd.max.widget.core.BoostAction
import nd.max.widget.core.ControlButton
import nd.max.widget.core.IslandToggleAction
import nd.max.widget.core.LastBoost
import nd.max.widget.core.RefreshWidgetsAction
import nd.max.widget.core.WidgetConfig
import nd.max.widget.core.WidgetConfigStore
import nd.max.widget.core.WidgetIntents
import nd.max.widget.core.WidgetKind
import nd.max.widget.core.WidgetPalette
import nd.max.widget.core.WidgetTheme
import nd.max.widget.openRouteAction
import nd.max.widget.openStudioAction

/**
 * ودجة «التحكّم السريع».
 *
 * فكرتها أن تُغني عن فتح التطبيق لفعلٍ واحد: تحرير الذاكرة، وتشغيل الجزيرة —
 * والباقي اختصارات وصول. والأزرار **يختارها المستخدم** (حتى أربعة، وهذا حدّ
 * القراءة المريحة في شبكة ٢×٢ لا حدّ تقنيّ)، وحالة الجزيرة تُرى باللون:
 * معبّأة بلون التمييز = تعمل الآن.
 */
class ControlsWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }
            .getOrDefault(WidgetConfigStore.INVALID_ID)
        val store = WidgetConfigStore(context)
        val config = if (appWidgetId == WidgetConfigStore.INVALID_ID) {
            WidgetConfig.defaults(WidgetKind.Controls)
        } else {
            store.load(appWidgetId, WidgetKind.Controls)
        }
        val palette = WidgetTheme.palette(config.accent, config.mode)
        val islandActive = runCatching { IslandService.isActive() }.getOrDefault(false)
        val lastBoost = if (appWidgetId == WidgetConfigStore.INVALID_ID) {
            null
        } else {
            store.lastBoost(appWidgetId)
        }

        provideContent {
            ControlsContent(
                config = config,
                palette = palette,
                islandActive = islandActive,
                lastBoost = lastBoost,
            )
        }
    }

    internal companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
    }
}

@Composable
private fun ControlsContent(
    config: WidgetConfig,
    palette: WidgetPalette,
    islandActive: Boolean,
    lastBoost: LastBoost?,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val refresh = actionRunCallback<RefreshWidgetsAction>()
    val buttons = config.buttons.ifEmpty { ControlButton.Default }
    val footnote = lastBoost?.let { boost ->
        when {
            !boost.executed -> context.getString(R.string.widget_controls_boost_failed)
            boost.freedMb != null -> context.getString(R.string.widget_controls_freed, boost.freedMb)
            else -> context.getString(R.string.widget_controls_boost_done)
        }
    }

    WidgetRoot(palette = palette, onClick = refresh) {
        if (size.width >= 200.dp) {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    buttons.forEachIndexed { index, button ->
                        if (index > 0) {
                            Spacer(GlanceModifier.width(6.dp))
                        }
                        ControlTile(
                            button = button,
                            islandActive = islandActive,
                            palette = palette,
                            modifier = GlanceModifier.defaultWeight().height(62.dp),
                        )
                    }
                }
                if (footnote != null) {
                    Spacer(GlanceModifier.height(6.dp))
                    Footnote(text = footnote, color = palette.colorDim())
                }
            }
        } else {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                buttons.chunked(2).forEachIndexed { rowIndex, rowButtons ->
                    if (rowIndex > 0) {
                        Spacer(GlanceModifier.height(6.dp))
                    }
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        rowButtons.forEachIndexed { index, button ->
                            if (index > 0) {
                                Spacer(GlanceModifier.width(6.dp))
                            }
                            ControlTile(
                                button = button,
                                islandActive = islandActive,
                                palette = palette,
                                modifier = GlanceModifier.defaultWeight().height(48.dp),
                            )
                        }
                        if (rowButtons.size == 1) {
                            Spacer(GlanceModifier.width(6.dp))
                            Box(
                                modifier = GlanceModifier.defaultWeight().height(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlTile(
    button: ControlButton,
    islandActive: Boolean,
    palette: WidgetPalette,
    modifier: GlanceModifier,
) {
    val context = LocalContext.current
    val label = context.getString(button.labelRes)
    val accent = palette.colorAccent()
    val surface = palette.colorSurface()
    val text = palette.colorText()
    when (button) {
        ControlButton.Boost -> ActionTile(
            label = label,
            iconRes = R.drawable.widget_ic_boost,
            filled = false,
            accent = accent,
            surface = surface,
            text = text,
            onClick = actionRunCallback<BoostAction>(),
            modifier = modifier,
        )

        ControlButton.Island -> ActionTile(
            label = label,
            iconRes = R.drawable.widget_ic_island,
            filled = islandActive,
            accent = accent,
            surface = surface,
            text = text,
            onClick = actionRunCallback<IslandToggleAction>(),
            modifier = modifier,
        )

        ControlButton.OpenStorage -> ActionTile(
            label = label,
            iconRes = R.drawable.widget_ic_storage,
            filled = false,
            accent = accent,
            surface = surface,
            text = text,
            onClick = openRouteAction(context, WidgetIntents.ROUTE_STORAGE),
            modifier = modifier,
        )

        ControlButton.OpenControl -> ActionTile(
            label = label,
            iconRes = R.drawable.widget_ic_control,
            filled = false,
            accent = accent,
            surface = surface,
            text = text,
            onClick = openRouteAction(context, WidgetIntents.ROUTE_CONTROL),
            modifier = modifier,
        )

        ControlButton.OpenStudio -> ActionTile(
            label = label,
            iconRes = R.drawable.widget_ic_studio,
            filled = false,
            accent = accent,
            surface = surface,
            text = text,
            onClick = openStudioAction(context),
            modifier = modifier,
        )

        ControlButton.Refresh -> ActionTile(
            label = label,
            iconRes = R.drawable.widget_ic_refresh,
            filled = false,
            accent = accent,
            surface = surface,
            text = text,
            onClick = actionRunCallback<RefreshWidgetsAction>(),
            modifier = modifier,
        )
    }
}
