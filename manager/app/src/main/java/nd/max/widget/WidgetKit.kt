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

package nd.max.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import nd.max.MainActivity
import nd.max.widget.core.WidgetIntents
import nd.max.widget.core.WidgetPalette
import nd.max.widget.core.WidgetTheme
import nd.max.widget.core.WidgetThemeMode
import nd.max.widget.core.WidgetTone

/**
 * طبقة الرسم المشتركة للودجات الثلاث — قطع Glance واحدة تُبنى منها التصاميم،
 * فلا يتفرّع «كيف يُرسم زرّ» إلى ثلاثة رسمات تتباعد أحكامها.
 *
 * وكل الألوان تُمرَّر `ColorProvider` بزوج (نهاريّ/ليليّ): النظام يختار بحسب
 * وضعه، والفرض من المستخدم يُطبَّق قبل الوصول إلى هنا (انظر `WidgetTheme`).
 */

/** لونا زوج — البوّابة الوحيدة التي يُبنى منها لون في هذه الحزمة. */
internal fun widgetColor(day: Long, night: Long): ColorProvider =
    ColorProvider(Color(day), Color(night))

internal fun WidgetPalette.colorAccent(): ColorProvider = widgetColor(accentDay, accentNight)

internal fun WidgetPalette.colorBackground(): ColorProvider = widgetColor(backgroundDay, backgroundNight)

internal fun WidgetPalette.colorSurface(): ColorProvider = widgetColor(surfaceDay, surfaceNight)

internal fun WidgetPalette.colorText(): ColorProvider = widgetColor(textDay, textNight)

internal fun WidgetPalette.colorDim(): ColorProvider = widgetColor(textDimDay, textDimNight)

internal fun WidgetPalette.colorTrack(): ColorProvider = widgetColor(trackDay, trackNight)

/** نصّ فوق لون التمييز: التمييز نهاريًّا داكن فنصّه أبيض، وليليًّا فاتح فنصّه داكن. */
internal fun onAccentColor(): ColorProvider = widgetColor(0xFFFFFFFF, 0xFF0E1116)

internal fun toneColor(tone: WidgetTone, mode: WidgetThemeMode): ColorProvider {
    val (day, night) = WidgetTheme.tone(tone, mode)
    return widgetColor(day, night)
}

/**
 * الجذر: خلفية الودجة، زواياها، ونقرتها. والنقرة على الجذر هي «حدّث» في ودجتَي
 * النبض والتحكّم، و«افتح العناية» في ودجة التخزين — كلٌّ يمرّر فعلها.
 */
@Composable
internal fun WidgetRoot(
    palette: WidgetPalette,
    onClick: Action? = null,
    content: @Composable () -> Unit,
) {
    var root = GlanceModifier
        .fillMaxSize()
        .background(palette.colorBackground())
        .cornerRadius(24.dp)
    if (onClick != null) {
        root = root.clickable(onClick)
    }
    Box(modifier = root.padding(12.dp), contentAlignment = Alignment.Center) { content() }
}

/** خليّة قياس: أيقونة ونبرة، قيمة كبيرة، وسم صغير، وشريط اختياري. */
@Composable
internal fun StatCell(
    iconRes: Int,
    label: String,
    value: String,
    tone: ColorProvider,
    dim: ColorProvider,
    track: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    bar: Float? = null,
    valueSize: TextUnit = 17.sp,
) {
    Column(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = null,
                modifier = GlanceModifier.size(16.dp),
                colorFilter = ColorFilter.tint(tone),
            )
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = value,
                style = TextStyle(color = tone, fontSize = valueSize, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = label,
            style = TextStyle(color = dim, fontSize = 10.sp, textAlign = TextAlign.Center),
            maxLines = 1,
        )
        if (bar != null) {
            Spacer(GlanceModifier.height(6.dp))
            LinearProgressIndicator(
                progress = bar.coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                color = tone,
                backgroundColor = track,
            )
        }
    }
}

/**
 * بلاطة فعل: أيقونة ووسم فوق سطح، أو معبّأة بلون التمييز حين يكون الفعل
 * «مشغَّلًا الآن» (جزيرة تعمل) — والحالة تُقرأ باللون لا بنصّ إضافي.
 */
@Composable
internal fun ActionTile(
    label: String,
    iconRes: Int,
    filled: Boolean,
    accent: ColorProvider,
    surface: ColorProvider,
    text: ColorProvider,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
) {
    val bg = if (filled) accent else surface
    val fg = if (filled) onAccentColor() else text
    Column(
        modifier = modifier
            .background(bg)
            .cornerRadius(16.dp)
            .clickable(onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier.size(20.dp),
            colorFilter = ColorFilter.tint(fg),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = label,
            style = TextStyle(color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

/** سطر نتيجة صغير أسفل الودجة (نتيجة آخر تحرير ذاكرة مثلًا). */
@Composable
internal fun Footnote(text: String, color: ColorProvider) {
    Text(
        text = text,
        style = TextStyle(color = color, fontSize = 10.sp, textAlign = TextAlign.Center),
        maxLines = 1,
    )
}

/** فاصل رأسيّ رفيع بين خلايا الصفّ العريض. */
@Composable
internal fun VerticalHairline(track: ColorProvider) {
    Box(
        modifier = GlanceModifier
            .width(1.dp)
            .height(30.dp)
            .background(track),
    ) {}
}

/** نيّة «افتح شاشةً داخل التطبيق» — تُقرأ في `MainActivity` وتُنقل إلى الملاح. */
internal fun openRouteAction(context: Context, route: String): Action =
    actionStartActivity(
        Intent(context, MainActivity::class.java)
            .putExtra(WidgetIntents.EXTRA_OPEN_ROUTE, route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

/**
 * نيّة «افتح الجزيرة الديناميكية».
 *
 * **ولماذا بقي الاسم:** الودجات تنادي هذه الدالة، والاستوديو المستقلّ دُمج في شاشة داخل
 * التطبيق (`MaxDestination.DynamicIsland` عبر `ROUTE_ISLAND`). فواجهة النداء لم تتغيّر،
 * وجسمها صار يمرّ بالملاح: لمسة الودجة تفتح نسخة التطبيق القائمة (`singleTask`) وتُنقل
 * إلى الشاشة بدل أن تفتح نشاطًا ثانيًا لا وجود له.
 */
internal fun openStudioAction(context: Context): Action =
    openRouteAction(context, WidgetIntents.ROUTE_ISLAND)
