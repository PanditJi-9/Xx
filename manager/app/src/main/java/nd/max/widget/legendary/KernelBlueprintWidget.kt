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
import android.os.Build
import android.os.SystemClock
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
import androidx.glance.layout.fillMaxSize
import nd.max.R
import nd.max.widget.StatCell
import nd.max.widget.VerticalHairline
import nd.max.widget.WidgetRoot
import nd.max.widget.colorAccent
import nd.max.widget.colorDim
import nd.max.widget.colorTrack
import nd.max.widget.core.WidgetAccent
import nd.max.widget.core.WidgetIntents
import nd.max.widget.core.WidgetPalette
import nd.max.widget.core.WidgetTheme
import nd.max.widget.core.WidgetThemeMode
import nd.max.widget.openRouteAction

/** ناقل ودجة مخطط النواة Kernel Blueprint */
class KernelBlueprintReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KernelBlueprintWidget()
}

/**
 * ودجة «مخطط النواة Kernel Blueprint» (٤×١):
 * شريط أنيق يعرض معمارية النظام وإصدار النواة ووقت تشغيل الجهاز.
 */
class KernelBlueprintWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val palette = WidgetTheme.palette(WidgetAccent.Brand, WidgetThemeMode.System)
        val uptimeHours = (SystemClock.elapsedRealtime() / (1000 * 60 * 60)).toInt()

        provideContent {
            KernelBlueprintContent(uptimeHours = uptimeHours, palette = palette)
        }
    }
}

@Composable
private fun KernelBlueprintContent(
    uptimeHours: Int,
    palette: WidgetPalette,
) {
    val context = LocalContext.current
    val openInfo = openRouteAction(context, WidgetIntents.ROUTE_DEVICE_INFO)
    val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64"

    WidgetRoot(palette = palette, onClick = openInfo) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatCell(
                iconRes = R.drawable.widget_ic_cpu,
                label = "Arch",
                value = abi.replace("_v8a", "").replace("-v8a", ""),
                tone = palette.colorAccent(),
                dim = palette.colorDim(),
                track = palette.colorTrack(),
                modifier = GlanceModifier.defaultWeight(),
                valueSize = 13.sp,
            )
            VerticalHairline(palette.colorTrack())
            StatCell(
                iconRes = R.drawable.widget_ic_control,
                label = "Android",
                value = Build.VERSION.RELEASE,
                tone = palette.colorAccent(),
                dim = palette.colorDim(),
                track = palette.colorTrack(),
                modifier = GlanceModifier.defaultWeight(),
                valueSize = 13.sp,
            )
            VerticalHairline(palette.colorTrack())
            StatCell(
                iconRes = R.drawable.widget_ic_speed,
                label = "Uptime",
                value = "${uptimeHours}h",
                tone = palette.colorAccent(),
                dim = palette.colorDim(),
                track = palette.colorTrack(),
                modifier = GlanceModifier.defaultWeight(),
                valueSize = 13.sp,
            )
        }
    }
}
