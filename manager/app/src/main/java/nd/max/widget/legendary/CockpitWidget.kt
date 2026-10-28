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
import nd.max.widget.WidgetHeader
import nd.max.widget.WidgetRoot
import nd.max.widget.colorAccent
import nd.max.widget.colorDim
import nd.max.widget.colorSurface
import nd.max.widget.colorText
import nd.max.widget.colorTrack
import nd.max.widget.core.BoostAction
import nd.max.widget.core.RefreshWidgetsAction
import nd.max.widget.core.ToggleFpsOverlayAction
import nd.max.widget.core.WidgetAccent
import nd.max.widget.core.WidgetData
import nd.max.widget.core.WidgetFormat
import nd.max.widget.core.WidgetIntents
import nd.max.widget.core.WidgetPalette
import nd.max.widget.core.WidgetTheme
import nd.max.widget.core.WidgetThemeMode
import nd.max.widget.openRouteAction

/** ناقل ودجة قمرة القيادة للألعاب Cockpit */
class CockpitReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CockpitWidget()
}

/**
 * ودجة «لوحة قيادة الألعاب Cockpit» (٤×٢):
 * قمرة تحكم متكاملة تعرض أحمال المعالج والرسّام والذاكرة، مع أزرار تشغيل FPS
 * وتحرير الذاكرة والدخول إلى مساحة الألعاب.
 */
class CockpitWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val palette = WidgetTheme.palette(WidgetAccent.Brand, WidgetThemeMode.System)
        val cpu = WidgetData.cpuLoad()
        val gpu = WidgetData.gpuLoad()
        val ram = WidgetData.ramMb(context)
        val isFpsRunning = WidgetData.isFpsServiceRunning()

        provideContent {
            CockpitContent(
                cpu = cpu,
                gpu = gpu,
                ram = ram,
                isFpsRunning = isFpsRunning,
                palette = palette,
            )
        }
    }
}

@Composable
private fun CockpitContent(
    cpu: Int?,
    gpu: Int?,
    ram: Pair<Int, Int>?,
    isFpsRunning: Boolean,
    palette: WidgetPalette,
) {
    val context = LocalContext.current
    val refresh = actionRunCallback<RefreshWidgetsAction>()
    val boostAction = actionRunCallback<BoostAction>()
    val fpsAction = actionRunCallback<ToggleFpsOverlayAction>()
    val openLobby = openRouteAction(context, WidgetIntents.ROUTE_GAME_LOBBY)

    WidgetRoot(palette = palette, onClick = refresh) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetHeader(
                title = context.getString(R.string.widget_cockpit_name),
                subtitle = if (isFpsRunning) context.getString(R.string.widget_status_live) else null,
                iconRes = R.drawable.widget_ic_game,
                palette = palette,
            )
            Spacer(GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatCell(
                    iconRes = R.drawable.widget_ic_cpu,
                    label = context.getString(R.string.widget_stat_cpu),
                    value = WidgetFormat.percentOf(cpu),
                    bar = cpu?.let { it / 100f },
                    tone = palette.colorAccent(),
                    dim = palette.colorDim(),
                    track = palette.colorTrack(),
                    modifier = GlanceModifier.defaultWeight(),
                    valueSize = 14.sp,
                )
                Spacer(GlanceModifier.width(4.dp))
                StatCell(
                    iconRes = R.drawable.widget_ic_gpu,
                    label = "GPU",
                    value = WidgetFormat.percentOf(gpu),
                    bar = gpu?.let { it / 100f },
                    tone = palette.colorAccent(),
                    dim = palette.colorDim(),
                    track = palette.colorTrack(),
                    modifier = GlanceModifier.defaultWeight(),
                    valueSize = 14.sp,
                )
                Spacer(GlanceModifier.width(4.dp))
                StatCell(
                    iconRes = R.drawable.widget_ic_ram,
                    label = context.getString(R.string.widget_stat_ram),
                    value = ram?.let { "${it.first / 1024}G" } ?: WidgetFormat.DASH,
                    bar = ram?.let { it.first.toFloat() / it.second.toFloat() },
                    tone = palette.colorAccent(),
                    dim = palette.colorDim(),
                    track = palette.colorTrack(),
                    modifier = GlanceModifier.defaultWeight(),
                    valueSize = 14.sp,
                )
            }
            Spacer(GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionTile(
                    label = context.getString(R.string.widget_action_clean_ram),
                    iconRes = R.drawable.widget_ic_boost,
                    filled = false,
                    accent = palette.colorAccent(),
                    surface = palette.colorSurface(),
                    text = palette.colorText(),
                    onClick = boostAction,
                    modifier = GlanceModifier.defaultWeight().height(42.dp),
                )
                Spacer(GlanceModifier.width(6.dp))
                ActionTile(
                    label = context.getString(R.string.widget_action_fps_toggle),
                    iconRes = R.drawable.widget_ic_fps,
                    filled = isFpsRunning,
                    accent = palette.colorAccent(),
                    surface = palette.colorSurface(),
                    text = palette.colorText(),
                    onClick = fpsAction,
                    modifier = GlanceModifier.defaultWeight().height(42.dp),
                )
                Spacer(GlanceModifier.width(6.dp))
                ActionTile(
                    label = context.getString(R.string.widget_game_launcher_name),
                    iconRes = R.drawable.widget_ic_game,
                    filled = true,
                    accent = palette.colorAccent(),
                    surface = palette.colorSurface(),
                    text = palette.colorText(),
                    onClick = openLobby,
                    modifier = GlanceModifier.defaultWeight().height(42.dp),
                )
            }
        }
    }
}
