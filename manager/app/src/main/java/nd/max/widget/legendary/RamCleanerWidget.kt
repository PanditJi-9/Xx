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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.width
import nd.max.R
import nd.max.widget.ActionTile
import nd.max.widget.StatCell
import nd.max.widget.WidgetRoot
import nd.max.widget.colorAccent
import nd.max.widget.colorDim
import nd.max.widget.colorSurface
import nd.max.widget.colorText
import nd.max.widget.colorTrack
import nd.max.widget.core.BoostAction
import nd.max.widget.core.WidgetAccent
import nd.max.widget.core.WidgetData
import nd.max.widget.core.WidgetFormat
import nd.max.widget.core.WidgetPalette
import nd.max.widget.core.WidgetTheme
import nd.max.widget.core.WidgetThemeMode

/** ناقل ودجة منظف الذاكرة RAM Cleaner */
class RamCleanerReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RamCleanerWidget()
}

/**
 * ودجة «منظف الذاكرة RAM Cleaner» (٢×١):
 * زر سريع ومباشر لتنظيف وضغط الذاكرة وإظهار المساحة المتاحة فورًا.
 */
class RamCleanerWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val palette = WidgetTheme.palette(WidgetAccent.Brand, WidgetThemeMode.System)
        val ram = WidgetData.ramMb(context)

        provideContent {
            RamCleanerContent(ram = ram, palette = palette)
        }
    }
}

@Composable
private fun RamCleanerContent(
    ram: Pair<Int, Int>?,
    palette: WidgetPalette,
) {
    val context = LocalContext.current
    val boostAction = actionRunCallback<BoostAction>()
    val freeMb = ram?.let { it.second - it.first } ?: 0
    val freeStr = ram?.let { "${WidgetFormat.gbFromMb(freeMb)} GB" } ?: WidgetFormat.DASH

    WidgetRoot(palette = palette) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatCell(
                iconRes = R.drawable.widget_ic_ram,
                label = context.getString(R.string.widget_storage_free),
                value = freeStr,
                tone = palette.colorAccent(),
                dim = palette.colorDim(),
                track = palette.colorTrack(),
                modifier = GlanceModifier.defaultWeight(),
                valueSize = 14.sp,
            )
            Spacer(GlanceModifier.width(6.dp))
            ActionTile(
                label = context.getString(R.string.widget_action_clean_ram),
                iconRes = R.drawable.widget_ic_boost,
                filled = true,
                accent = palette.colorAccent(),
                surface = palette.colorSurface(),
                text = palette.colorText(),
                onClick = boostAction,
                modifier = GlanceModifier.defaultWeight().height(48.dp),
            )
        }
    }
}
