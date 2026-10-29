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

package nd.max.widget.storage

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import nd.max.R
import nd.max.widget.ActionTile
import nd.max.widget.WidgetRoot
import nd.max.widget.colorAccent
import nd.max.widget.colorDim
import nd.max.widget.colorText
import nd.max.widget.colorTrack
import nd.max.widget.core.WidgetConfig
import nd.max.widget.core.WidgetConfigStore
import nd.max.widget.core.WidgetData
import nd.max.widget.core.WidgetFormat
import nd.max.widget.core.WidgetIntents
import nd.max.widget.core.WidgetKind
import nd.max.widget.core.WidgetPalette
import nd.max.widget.core.WidgetTheme
import nd.max.widget.openRouteAction
import nd.max.widget.toneColor
import nd.max.widget.MaxGlanceWidget

/**
 * ودجة «عناية التخزين».
 *
 * تقرأ ما يُقرأ فورًا (StatFs لا حمل ولا جذر)، وتعرض **المتاح** كبيرًا لأن
 * السؤال اليوميّ «هل بقي مكان؟» لا «كم استُهلك؟» — والنسبة والشريط سياق تحته.
 * والنقرة كلها تفتح شاشة العناية، فالحذف نفسه لا يقع من خلف ظهر المستخدم:
 * الودجة تُرشد، والقرار يبقى في الشاشة التي تُقيس وتُؤكِّد.
 */
class StorageWidget : MaxGlanceWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun renderGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }
            .getOrDefault(WidgetConfigStore.INVALID_ID)
        val config = if (appWidgetId == WidgetConfigStore.INVALID_ID) {
            WidgetConfig.defaults(WidgetKind.Storage)
        } else {
            WidgetConfigStore(context).load(appWidgetId, WidgetKind.Storage)
        }
        val palette = WidgetTheme.palette(config.accent, config.mode)
        val storage = WidgetData.storage()

        provideContent {
            StorageContent(config = config, palette = palette, storage = storage)
        }
    }

    internal companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
    }
}

@Composable
private fun StorageContent(
    config: WidgetConfig,
    palette: WidgetPalette,
    storage: WidgetData.StorageVitals?,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val openCare = openRouteAction(context, WidgetIntents.ROUTE_STORAGE)
    val unitGb = context.getString(R.string.widget_unit_gb)
    val freeValue = storage?.let { WidgetFormat.gbFromBytes(it.freeBytes) } ?: WidgetFormat.DASH
    val totalValue = storage?.let { "${WidgetFormat.gbFromBytes(it.totalBytes)} $unitGb" } ?: WidgetFormat.DASH
    val usedLine = storage?.let { context.getString(R.string.widget_storage_used, it.usedPercent) }
    val totalLine = context.getString(R.string.widget_storage_total, totalValue)

    WidgetRoot(palette = palette, onClick = openCare) {
        if (size.width >= 200.dp) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = GlanceModifier.defaultWeight().padding(end = 10.dp)) {
                    BigFreeValue(freeValue = freeValue, unit = unitGb, palette = palette)
                    Spacer(GlanceModifier.height(6.dp))
                    StorageDetails(
                        config = config,
                        palette = palette,
                        usedLine = usedLine,
                        totalLine = totalLine,
                        storage = storage,
                    )
                }
                ActionTile(
                    label = context.getString(R.string.widget_storage_open_care),
                    iconRes = R.drawable.widget_ic_storage,
                    filled = true,
                    accent = palette.colorAccent(),
                    surface = palette.colorText(),
                    text = palette.colorText(),
                    onClick = openCare,
                    modifier = GlanceModifier.defaultWeight().height(66.dp),
                )
            }
        } else {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BigFreeValue(
                    freeValue = freeValue,
                    unit = unitGb,
                    palette = palette,
                    horizontalAlignment = Alignment.CenterHorizontally,
                )
                Spacer(GlanceModifier.height(6.dp))
                StorageDetails(
                    config = config,
                    palette = palette,
                    usedLine = usedLine,
                    totalLine = totalLine,
                    storage = storage,
                )
            }
        }
    }
}

@Composable
private fun BigFreeValue(
    freeValue: String,
    unit: String,
    palette: WidgetPalette,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    val context = LocalContext.current
    Column(horizontalAlignment = horizontalAlignment) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = freeValue,
                style = TextStyle(
                    color = palette.colorAccent(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = unit,
                style = TextStyle(color = palette.colorDim(), fontSize = 11.sp),
                maxLines = 1,
            )
        }
        Text(
            text = context.getString(R.string.widget_storage_free),
            style = TextStyle(color = palette.colorDim(), fontSize = 10.sp),
            maxLines = 1,
        )
    }
}

@Composable
private fun StorageDetails(
    config: WidgetConfig,
    palette: WidgetPalette,
    usedLine: String?,
    totalLine: String,
    storage: WidgetData.StorageVitals?,
) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            text = listOfNotNull(usedLine, totalLine).joinToString(separator = " · "),
            style = TextStyle(color = palette.colorDim(), fontSize = 10.sp),
            maxLines = 1,
        )
        if (config.showStorageBar && storage != null) {
            Spacer(GlanceModifier.height(8.dp))
            LinearProgressIndicator(
                progress = storage.usedFraction.coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                color = toneColor(WidgetTheme.loadTone(storage.usedPercent), config.mode),
                backgroundColor = palette.colorTrack(),
            )
        }
    }
}
