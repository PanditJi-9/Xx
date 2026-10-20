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

package nd.max.widget.pulse

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.LinearProgressIndicator
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
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import nd.max.R
import nd.max.widget.StatCell
import nd.max.widget.VerticalHairline
import nd.max.widget.WidgetRoot
import nd.max.widget.colorDim
import nd.max.widget.colorTrack
import nd.max.widget.core.PulseStat
import nd.max.widget.core.RefreshWidgetsAction
import nd.max.widget.core.WidgetConfig
import nd.max.widget.core.WidgetConfigStore
import nd.max.widget.core.WidgetData
import nd.max.widget.core.WidgetFormat
import nd.max.widget.core.WidgetKind
import nd.max.widget.core.WidgetPalette
import nd.max.widget.core.WidgetTheme
import nd.max.widget.core.WidgetThemeMode
import nd.max.widget.core.WidgetTone
import nd.max.widget.toneColor

/**
 * ودجة «نبض الجهاز».
 *
 * التصميم مشتقّ من معنى القراءة لا من شكل جاهز: البطارية **خبر** (كم بقي؟) فهي
 * القيمة الكبرى في المقاس الصغير، والحرارة والمعالج والذاكرة **سياق** يحيط بها
 * كخلايا لها شرائطها. والألوان تقول الحالة قبل أن تُقرأ الأرقام — أحمر يعني
 * «انتبه» لا «جمال».
 *
 * والمقاسان المُعلَنان: شبكة ٢×٢ صغيرة، وشريط ٤×١ عريض — و`Responsive` يختار
 * بحسب ما أعطاه المشغّل، والاختيار هنا بـ [LocalSize] لا بتخمين.
 */
class PulseWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }
            .getOrDefault(WidgetConfigStore.INVALID_ID)
        val config = if (appWidgetId == WidgetConfigStore.INVALID_ID) {
            WidgetConfig.defaults(WidgetKind.Pulse)
        } else {
            WidgetConfigStore(context).load(appWidgetId, WidgetKind.Pulse)
        }
        val palette = WidgetTheme.palette(config.accent, config.mode)
        val battery = WidgetData.battery(context)
        val temperature = WidgetData.temperatureC(context)
        val cpu = WidgetData.cpuLoad()
        val ram = WidgetData.ramMb(context)

        provideContent {
            PulseContent(
                config = config,
                palette = palette,
                battery = battery,
                temperature = temperature,
                cpu = cpu,
                ram = ram,
            )
        }
    }

    internal companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
    }
}

/** قيمة قياس وُلدت جاهزة للرسم: لا حساب داخل الشجرة. */
internal data class StatVisual(
    val label: String,
    val iconRes: Int,
    val value: String,
    val bar: Float?,
    val tone: WidgetTone,
)

internal fun visualOf(
    stat: PulseStat,
    context: Context,
    battery: WidgetData.BatteryVitals,
    temperature: Float?,
    cpu: Int?,
    ram: Pair<Int, Int>?,
): StatVisual {
    val label = context.getString(stat.labelRes)
    return when (stat) {
        PulseStat.Battery -> StatVisual(
            label = label,
            iconRes = R.drawable.widget_ic_battery,
            value = WidgetFormat.percentOf(battery.percent),
            bar = battery.percent?.let { it / 100f },
            tone = WidgetTheme.batteryPercentTone(battery.percent),
        )

        PulseStat.Temp -> StatVisual(
            label = label,
            iconRes = R.drawable.widget_ic_temp,
            value = WidgetFormat.degreesOf(temperature),
            // خريطة العرض: ٣٠° تعني شريطًا فارغًا و٤٥° ممتلئًا — النطاق الذي يهمّ فعلًا.
            bar = temperature?.let { (it - 30f) / 15f },
            tone = WidgetTheme.batteryTone(temperature),
        )

        PulseStat.Cpu -> StatVisual(
            label = label,
            iconRes = R.drawable.widget_ic_cpu,
            value = WidgetFormat.percentOf(cpu),
            bar = cpu?.let { it / 100f },
            tone = WidgetTheme.loadTone(cpu),
        )

        PulseStat.Ram -> {
            val percent = ram?.let { WidgetFormat.ramPercent(it.first, it.second) }
            StatVisual(
                label = label,
                iconRes = R.drawable.widget_ic_ram,
                value = WidgetFormat.percentOf(percent),
                bar = percent?.let { it / 100f },
                tone = WidgetTheme.loadTone(percent),
            )
        }
    }
}

@Composable
private fun PulseContent(
    config: WidgetConfig,
    palette: WidgetPalette,
    battery: WidgetData.BatteryVitals,
    temperature: Float?,
    cpu: Int?,
    ram: Pair<Int, Int>?,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val refresh = actionRunCallback<RefreshWidgetsAction>()
    val ordered = PulseStat.Ordered
        .filter { it in config.stats }
        .ifEmpty { PulseStat.Ordered }
        .map { visualOf(it, context, battery, temperature, cpu, ram) }

    WidgetRoot(palette = palette, onClick = refresh) {
        if (size.width >= 200.dp) {
            WideRow(stats = ordered, palette = palette, mode = config.mode)
        } else {
            SmallColumn(stats = ordered, palette = palette, mode = config.mode)
        }
    }
}

@Composable
private fun SmallColumn(
    stats: List<StatVisual>,
    palette: WidgetPalette,
    mode: WidgetThemeMode,
) {
    val head = stats.first()
    val secondary = stats.drop(1).take(2)
    val headTone = toneColor(head.tone, mode)

    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(head.iconRes),
                contentDescription = head.label,
                modifier = GlanceModifier.size(20.dp),
                colorFilter = ColorFilter.tint(headTone),
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = head.value,
                style = TextStyle(color = headTone, fontSize = 26.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }
        if (head.bar != null) {
            Spacer(GlanceModifier.height(8.dp))
            LinearProgressIndicator(
                progress = head.bar.coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                color = headTone,
                backgroundColor = palette.colorTrack(),
            )
        }
        if (secondary.isNotEmpty()) {
            Spacer(GlanceModifier.height(10.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                secondary.forEachIndexed { index, stat ->
                    if (index > 0) {
                        VerticalHairline(palette.colorTrack())
                    }
                    StatCell(
                        iconRes = stat.iconRes,
                        label = stat.label,
                        value = stat.value,
                        tone = toneColor(stat.tone, mode),
                        dim = palette.colorDim(),
                        track = palette.colorTrack(),
                        modifier = GlanceModifier.defaultWeight(),
                        valueSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun WideRow(
    stats: List<StatVisual>,
    palette: WidgetPalette,
    mode: WidgetThemeMode,
) {
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        stats.forEachIndexed { index, stat ->
            if (index > 0) {
                VerticalHairline(palette.colorTrack())
            }
            StatCell(
                iconRes = stat.iconRes,
                label = stat.label,
                value = stat.value,
                tone = toneColor(stat.tone, mode),
                dim = palette.colorDim(),
                track = palette.colorTrack(),
                modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp),
                bar = stat.bar,
                valueSize = if (stats.size <= 2) 22.sp else 16.sp,
            )
        }
    }
}
