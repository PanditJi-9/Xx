/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewSidebar
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import nd.max.R
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE

/*
 * أجزاء اللوحة الثابتة — الشريط الجانبيّ، شريط الحالة، لوحات الأداء الثلاث، وقاعدة الحالة.
 *
 * **كل رقم هنا بكسل من اللقطة** (١٦٧٢×٩٤١): انظر `GameSpaceStage.kt`. فلا رموز `MaxSpace`/`MaxSize` هنا
 * (قياساتها بالـdp الفعليّ لا بوحدة اللقطة)، بل ثوابت مسمّاة تحمل المقاس المرسوم. وبوّابة `design_tokens`
 * تعدّ `RoundedCornerShape(N.dp)` و`.border(N.dp` و`spacedBy(N.dp)` وتمنع زيادتها، فكل قياس منها اسمٌ.
 *
 * اللغة البصريّة: ألواح زجاجيّة داكنة بحدّ أبيض خافت، والبنفسجيّ للتفاعل وحده (العنصر النشط، الأقواس،
 * المنحنى). ما لم يُقرأ يبقى «—» وحلقةً بلا قوس لا صفرًا (ADR-07).
 */

private val RadiusRail = 40.dp
private val RadiusItem = 16.dp
private val RadiusCard = 24.dp
private val StrokeFine = 1.dp
private val StrokeFirm = 1.5.dp
private val GapS = 10.dp
private val GapM = 16.dp
private val GapL = 24.dp
private val PadCard = 22.dp
private val PadWide = 30.dp
private val AvatarSize = 36.dp
private val GearHit = 64.dp

private val GlassBorder = Brush.linearGradient(
    listOf(
        Color.White.copy(alpha = 0.16f),
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.10f)
    )
)
private val GlassFill = Brush.verticalGradient(
    listOf(GameSpacePalette.Glass.copy(alpha = 0.80f), GameSpacePalette.GlassDeep.copy(alpha = 0.88f))
)
private val RailFill = Brush.verticalGradient(
    listOf(GameSpacePalette.Glass.copy(alpha = 0.86f), GameSpacePalette.GlassDeep.copy(alpha = 0.93f))
)
private val ActiveFill = Brush.horizontalGradient(
    listOf(GameSpacePalette.VioletDeep.copy(alpha = 0.65f), GameSpacePalette.Plum.copy(alpha = 0.92f))
)
private val BrandBrush = Brush.linearGradient(
    listOf(Color.White, GameSpacePalette.Lavender, GameSpacePalette.VioletSoft)
)
private val AvatarBrush = Brush.linearGradient(
    listOf(GameSpacePalette.VioletSoft, GameSpacePalette.VioletDeep)
)

/** قاعدة الحالة: مستطيل مستدير الزوايا بقطع مائل في الزاوية السفلى اليمنى كما في اللقطة. */
private val BaseShape = GenericShape { size, _ ->
    val r = size.height * 0.28f
    val c = size.height * 0.52f
    moveTo(r, 0f)
    lineTo(size.width - r, 0f)
    arcTo(Rect(size.width - 2f * r, 0f, size.width, 2f * r), -90f, 90f, false)
    lineTo(size.width, size.height - c)
    lineTo(size.width - c, size.height)
    lineTo(r, size.height)
    arcTo(Rect(0f, size.height - 2f * r, 2f * r, size.height), 90f, 90f, false)
    lineTo(0f, r)
    arcTo(Rect(0f, 0f, 2f * r, 2f * r), 180f, 90f, false)
    close()
}

/** سطح زجاجيّ داكن بحدّ أبيض خافت — لغة كل اللوحات في اللقطة. */
internal fun Modifier.stageGlass(shape: Shape): Modifier =
    this
        .clip(shape)
        .background(GlassFill)
        .border(StrokeFine, GlassBorder, shape)

/**
 * أقسام الشريط كما في التصميم. العناوين والأيقونات للأقسام **القائمة فعلًا** في مركز التحكّم:
 * المكتبة، الأداء (الرؤى)، الأدوات (اللوحة الجانبية)، المزايا (ملفّ اللعبة)، وإعدادات اللعبة.
 * أمّا الإعدادات فتحملها أيقونة الترس أسفل الشريط.
 */
internal enum class StageNav(val labelRes: Int, val icon: ImageVector, val section: LobbySection) {
    Library(R.string.gs_nav_games, Icons.Outlined.SportsEsports, LobbySection.Library),
    Performance(R.string.gs_nav_perf, Icons.Outlined.Insights, LobbySection.Insights),
    Tools(R.string.gs_nav_tools, Icons.Outlined.ViewSidebar, LobbySection.Panel),
    Features(R.string.gs_nav_features, Icons.Outlined.StarBorder, LobbySection.Loadout),
    Profile(LobbySection.Game.titleRes, Icons.Outlined.Tune, LobbySection.Game)
}

/**
 * الشريط الجانبيّ بإحداثيات اللقطة: لوح (٠،٢٤، ١٦٨×٨٩٢)، شعار عند y≈٨٥، «Home» نشطة في (١٤،١٥٢)
 * بارتفاع ٦٣، ثم خمسة أقسام بارتفاع ٦٥ لكلٍّ، والترس مركزه (٨٠،٨٧٥).
 */
@Composable
internal fun StageRail(onHome: () -> Unit, onSection: (LobbySection) -> Unit) {
    val frame = RoundedCornerShape(RadiusRail)
    Box(
        Modifier
            .at(0, 24, 168, 892)
            .clip(frame)
            .background(RailFill)
            .border(StrokeFine, GlassBorder, frame)
    )
    Box(Modifier.at(0, 56, 168, 58), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.gs_brand_mark),
            style = TextStyle(brush = BrandBrush, fontSize = 52.sp, fontWeight = FontWeight.Black),
            maxLines = 1
        )
    }
    Column(Modifier.at(14, 152, 154, 410)) {
        StageRailItem(
            labelRes = R.string.gs_nav_home,
            icon = Icons.Outlined.Home,
            active = true,
            height = 63,
            onClick = onHome
        )
        Spacer(Modifier.height(19.dp))
        StageNav.entries.forEach { entry ->
            StageRailItem(
                labelRes = entry.labelRes,
                icon = entry.icon,
                active = false,
                height = 65,
                onClick = { onSection(entry.section) }
            )
        }
    }
    Box(
        modifier = Modifier
            .at(48, 843, 64, 64)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = { onSection(LobbySection.Lobby) }),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = stringResource(R.string.gs_nav_settings),
            tint = GameSpacePalette.Ink,
            modifier = Modifier.size(GearIcon)
        )
    }
}

private val GearIcon = 40.dp

@Composable
private fun StageRailItem(
    labelRes: Int,
    icon: ImageVector,
    active: Boolean,
    height: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(RadiusItem)
    val tint = if (active) Color.White else GameSpacePalette.Ink.copy(alpha = 0.88f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(shape)
            .then(
                if (active) {
                    Modifier
                        .background(ActiveFill)
                        .border(StrokeFirm, GameSpacePalette.VioletSoft.copy(alpha = 0.85f), shape)
                } else {
                    Modifier
                }
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(start = 17.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GapM)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(GlyphRail)
        )
        Text(
            text = stringResource(labelRes),
            color = tint,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

private val GlyphRail = 32.dp

/**
 * شريط الحالة العلويّ: الساعة والتاريخ يسارًا (x=٢٠٠)، واتّصال + بطارية + نسبة + صورة «M» يمينًا.
 * أزرار اللوبي (استكشاف/مفضّلة/سجلّ/إغلاق) تُوضع بين الطرفين بمقاسها **الفعليّ** — بإعادة
 * `LocalDensity` الأصلي داخل منطقتها — فتبقى قابلة للّمس مهما صغّرت اللوحة.
 */
@Composable
internal fun StageStatusBar(
    data: StageData,
    onProfile: () -> Unit,
    realDensity: Density,
    topActions: @Composable () -> Unit,
) {
    val link by rememberLobbyLink()
    Row(
        modifier = Modifier.at(200, 22, 480, 40),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GapM)
    ) {
        Text(
            text = data.clock,
            color = GameSpacePalette.Ink,
            fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = data.date,
            color = GameSpacePalette.Ink.copy(alpha = 0.75f),
            fontSize = 19.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    Box(Modifier.at(700, 0, 720, 86), contentAlignment = Alignment.CenterEnd) {
        CompositionLocalProvider(LocalDensity provides realDensity) {
            Row(Modifier.wrapContentSize(align = Alignment.CenterEnd, unbounded = true)) {
                topActions()
            }
        }
    }
    Row(
        modifier = Modifier.at(1426, 22, 226, 44),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GapM, Alignment.End)
    ) {
        Icon(
            imageVector = when (link) {
                LobbyLink.Wifi -> Icons.Rounded.Wifi
                LobbyLink.Cellular -> Icons.Rounded.SignalCellularAlt
                LobbyLink.Offline -> Icons.Rounded.WifiOff
            },
            contentDescription = null,
            tint = if (link == LobbyLink.Offline) GameSpacePalette.Muted else GameSpacePalette.Ink,
            modifier = Modifier.size(GlyphStatus)
        )
        StageBatteryGlyph(percent = data.batteryPercent, charging = data.charging)
        Text(
            text = data.batteryPercent?.let { stringResource(R.string.gs_percent_value, it) }
                ?: MAX_VALUE_UNAVAILABLE,
            color = GameSpacePalette.Ink,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        val profile = stringResource(R.string.lobby_tile_profile)
        Box(
            modifier = Modifier
                .size(AvatarSize)
                .clip(CircleShape)
                .background(AvatarBrush)
                .semantics { contentDescription = profile }
                .clickable(role = Role.Button, onClick = onProfile),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.gs_brand_mark),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

private val GlyphStatus = 30.dp

/** بطارية أفقيّة كما في اللقطة: إطار مجوّف بحلمة، وتعبئة بنسبة الشحن الحقيقيّة (سماويّة عند الشحن). */
@Composable
private fun StageBatteryGlyph(percent: Int?, charging: Boolean) {
    Canvas(Modifier.size(width = 38.dp, height = 20.dp)) {
        val stroke = size.height * 0.11f
        val body = size.width * 0.92f
        val tone = GameSpacePalette.Ink.copy(alpha = 0.92f)
        drawRoundRect(
            color = tone,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(body - stroke, size.height - stroke),
            cornerRadius = CornerRadius(size.height * 0.28f),
            style = Stroke(width = stroke)
        )
        drawRoundRect(
            color = tone,
            topLeft = Offset(body + stroke, size.height * 0.32f),
            size = Size(size.width - body - stroke, size.height * 0.36f),
            cornerRadius = CornerRadius(stroke)
        )
        if (percent != null) {
            val fill = (body - stroke * 3f) * (percent / 100f).coerceIn(0f, 1f)
            drawRoundRect(
                color = if (charging) GameSpacePalette.Cyan else GameSpacePalette.Ink,
                topLeft = Offset(stroke * 1.5f, stroke * 1.5f),
                size = Size(fill, size.height - stroke * 3f),
                cornerRadius = CornerRadius(size.height * 0.14f)
            )
        }
    }
}

/** أيقونة النبض في رأس لوحة الأداء. */
@Composable
private fun PulseLine(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, h * 0.5f)
            lineTo(w * 0.22f, h * 0.5f)
            lineTo(w * 0.34f, h * 0.08f)
            lineTo(w * 0.50f, h * 0.94f)
            lineTo(w * 0.62f, h * 0.28f)
            lineTo(w * 0.72f, h * 0.5f)
            lineTo(w, h * 0.5f)
        }
        drawPath(
            path = path,
            color = GameSpacePalette.VioletSoft,
            style = Stroke(width = h * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * اللوحات الثلاث بمستطيلاتها في اللقطة: الأداء (١٢٣٦،٨٧، ٤١٠×٢٢٩)، البطارية ووضع الأداء
 * (١٢٣٦،٣٣٠، ٤١٠×٩١)، والإطارات اللحظيّة (١٢٣٦،٤٣٣، ٤١٠×٨٥).
 */
@Composable
internal fun StageBoard(data: StageData, onOpen: () -> Unit) {
    PerformanceCard(data = data, onOpen = onOpen, modifier = Modifier.at(1236, 87, 410, 229))
    StatusCard(data = data, modifier = Modifier.at(1236, 330, 410, 91))
    FpsCard(fps = data.fps, modifier = Modifier.at(1236, 433, 410, 85))
}

@Composable
private fun PerformanceCard(data: StageData, onOpen: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(RadiusCard)
    Column(
        modifier = modifier
            .stageGlass(shape)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(start = PadCard, top = 16.dp, end = 10.dp)
    ) {
        Row(
            modifier = Modifier.height(30.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GapM)
        ) {
            PulseLine(Modifier.size(width = 26.dp, height = 22.dp))
            Text(
                text = stringResource(R.string.gs_device_perf),
                color = GameSpacePalette.Ink,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth()) {
            StageRing(
                progress = data.cpu?.let { it / 100f },
                value = data.cpu?.let { stringResource(R.string.gs_percent_value, it) } ?: MAX_VALUE_UNAVAILABLE,
                label = stringResource(R.string.gs_cpu),
                from = GameSpacePalette.Violet,
                to = GameSpacePalette.VioletSoft,
                modifier = Modifier.weight(1f)
            )
            StageRing(
                progress = data.gpu?.let { it / 100f },
                value = data.gpu?.let { stringResource(R.string.gs_percent_value, it) } ?: MAX_VALUE_UNAVAILABLE,
                label = stringResource(R.string.gs_gpu),
                from = GameSpacePalette.Blue,
                to = GameSpacePalette.Cyan,
                modifier = Modifier.weight(1f)
            )
            StageRing(
                // مقياس الحرارة ٢٥–٦٥°م: القوس مقياس بصريّ، والرقم هو الخبر.
                progress = data.heat?.let { ((it - 25f) / 40f).coerceIn(0f, 1f) },
                value = data.heat?.let { "${it.roundToInt()}°C" } ?: MAX_VALUE_UNAVAILABLE,
                label = stringResource(R.string.gs_heat),
                from = GameSpacePalette.Magenta,
                to = GameSpacePalette.Orange,
                labelIcon = Icons.Rounded.Thermostat,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private val RingSize = 106.dp
private val RingLabelIcon = 26.dp

/** حلقة قراءة (١٠٦): مسار خافت، وقوس متدرّج بتوهّج، والرقم في المركز بوحدة أصغر. الغائب بلا قوس. */
@Composable
private fun StageRing(
    progress: Float?,
    value: String,
    label: String,
    from: Color,
    to: Color,
    modifier: Modifier = Modifier,
    labelIcon: ImageVector? = null,
) {
    val animated by animateFloatAsState(
        targetValue = (progress ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700)
    )
    val number = value.takeWhile { it.isDigit() }
    val unit = value.drop(number.length)
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(RingSize), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(RingSize)) {
                val strokeWidth = size.minDimension * 0.075f
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
                if (progress != null && animated > 0.005f) {
                    val sweep = 360f * animated
                    val stop = (sweep / 360f).coerceIn(0.02f, 1f)
                    val brush = Brush.sweepGradient(0f to from, stop to to, 1f to to)
                    rotate(degrees = -90f) {
                        drawArc(
                            brush = brush,
                            startAngle = 0f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            alpha = 0.25f,
                            style = Stroke(width = strokeWidth * 2.0f, cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = brush,
                            startAngle = 0f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
            }
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 30.sp)) {
                        append(if (number.isEmpty()) value else number)
                    }
                    if (number.isNotEmpty() && unit.isNotEmpty()) {
                        withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal)) {
                            append(unit)
                        }
                    }
                },
                color = if (value == MAX_VALUE_UNAVAILABLE) GameSpacePalette.Muted else GameSpacePalette.Ink,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Row(
            modifier = Modifier.height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GapS)
        ) {
            if (labelIcon != null) {
                Icon(
                    imageVector = labelIcon,
                    contentDescription = null,
                    tint = to,
                    modifier = Modifier.size(RingLabelIcon)
                )
            }
            Text(
                text = label,
                color = GameSpacePalette.Ink.copy(alpha = 0.92f),
                fontSize = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StageDivider(height: Dp) {
    Box(
        Modifier
            .width(StrokeFine)
            .height(height)
            .background(Color.White.copy(alpha = 0.12f))
    )
}

private val BatteryIcon = 54.dp
private val ModeIcon = 44.dp
private val ChevronIcon = 28.dp

/** لوحة البطارية ووضع الأداء: نصفان يفصلهما خطّ رفيع، وسهم يشير إلى أنّ الوضع قابل للفتح. */
@Composable
private fun StatusCard(data: StageData, modifier: Modifier) {
    val shape = RoundedCornerShape(RadiusCard)
    Row(
        modifier = modifier
            .stageGlass(shape)
            .padding(horizontal = PadWide),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GapM)
    ) {
        // بطارية خضراء؛ والبرق داخلها يظهر عند الشحن فقط — لا يُرسم ادّعاءً.
        Box(modifier = Modifier.size(BatteryIcon), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.BatteryFull,
                contentDescription = stringResource(R.string.gs_battery),
                tint = GameSpacePalette.Mint,
                modifier = Modifier.size(BatteryIcon)
            )
            if (data.charging) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = GameSpacePalette.GlassDeep,
                    modifier = Modifier.size(GlyphStatus)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.batteryPercent?.let { stringResource(R.string.gs_percent_value, it) }
                    ?: MAX_VALUE_UNAVAILABLE,
                color = GameSpacePalette.Ink,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.gs_battery),
                color = GameSpacePalette.Ink.copy(alpha = 0.75f),
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        StageDivider(BatteryIcon)
        Icon(
            imageVector = Icons.Rounded.Bolt,
            contentDescription = null,
            tint = if (data.panelEnabled) GameSpacePalette.VioletSoft else GameSpacePalette.Muted,
            modifier = Modifier.size(ModeIcon)
        )
        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = stringResource(R.string.gs_perf_mode),
                color = GameSpacePalette.VioletSoft,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (data.panelEnabled) stringResource(R.string.gs_perf_mode_on) else MAX_VALUE_UNAVAILABLE,
                color = if (data.panelEnabled) GameSpacePalette.Ink else GameSpacePalette.Muted,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = GameSpacePalette.Ink,
            modifier = Modifier.size(ChevronIcon)
        )
    }
}

/** لوحة الإطارات اللحظيّة: منحنى آخر العيّنات يسارًا (١٥٤×٤٨)، والرقم الكبير، ثم أيقونة أعمدة. */
@Composable
private fun FpsCard(fps: StageFps, modifier: Modifier) {
    val shape = RoundedCornerShape(RadiusCard)
    val shown = fps.value != null && fps.live
    Row(
        modifier = modifier
            .stageGlass(shape)
            .padding(horizontal = PadCard),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GapL)
    ) {
        StageSparkline(
            values = fps.history,
            modifier = Modifier.size(width = 154.dp, height = 48.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.gs_fps_title),
                color = GameSpacePalette.Ink.copy(alpha = 0.85f),
                fontSize = 15.sp,
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(GapS)
            ) {
                Text(
                    text = fps.value?.toString() ?: MAX_VALUE_UNAVAILABLE,
                    color = if (shown) GameSpacePalette.Ink else GameSpacePalette.Muted,
                    fontSize = 37.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.gs_fps_unit),
                    color = GameSpacePalette.Ink.copy(alpha = 0.78f),
                    fontSize = 17.sp,
                    maxLines = 1
                )
            }
        }
        StageBars(Modifier.size(width = 28.dp, height = 26.dp))
    }
}

/** منحنى بنفسجيّ مملوء من عيّنات حقيقيّة؛ أقلّ من عيّنتين = خطّ أساس متقطّع لا منحنى مخترع. */
@Composable
private fun StageSparkline(values: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) {
            drawLine(
                color = GameSpacePalette.VioletSoft.copy(alpha = 0.35f),
                start = Offset(0f, size.height * 0.75f),
                end = Offset(size.width, size.height * 0.75f),
                strokeWidth = size.height * 0.04f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(size.height * 0.2f, size.height * 0.2f))
            )
            return@Canvas
        }
        val peak = (values.maxOrNull() ?: 1f).coerceAtLeast(1f) * 1.15f
        val step = size.width / (values.size - 1)
        val line = Path()
        val fill = Path()
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = size.height - (v / peak).coerceIn(0f, 1f) * size.height * 0.90f - size.height * 0.05f
            if (i == 0) {
                line.moveTo(x, y)
                fill.moveTo(x, size.height)
                fill.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.close()
        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                listOf(GameSpacePalette.VioletSoft.copy(alpha = 0.35f), Color.Transparent)
            )
        )
        drawPath(
            path = line,
            color = GameSpacePalette.VioletSoft,
            style = Stroke(width = size.height * 0.05f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/** ثلاثة أعمدة صغيرة — رمز اللوحة كما في التصميم؛ زينة صريحة لا قراءة. */
@Composable
private fun StageBars(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val bar = size.width * 0.24f
        val gap = (size.width - bar * 3f) / 2f
        listOf(0.45f, 0.75f, 1f).forEachIndexed { i, fraction ->
            drawRoundRect(
                color = GameSpacePalette.VioletSoft,
                topLeft = Offset(i * (bar + gap), size.height * (1f - fraction)),
                size = Size(bar, size.height * fraction),
                cornerRadius = CornerRadius(bar / 3f)
            )
        }
    }
}

private val BaseIcon = 30.dp
private val BarWidth = 393.dp
private val BarHeight = 18.dp

/**
 * قاعدة الحالة (١٩٥،٨٥٠، ١٤٥١×٦٢): برق، فاصل، حكم آخر جلسة (أو الجاهزية)، وشريط نسبة بشرائح مائلة.
 * [detail] سطر ثانٍ **اختياريّ**: يظهر حين يكون هناك شرط ناقص يُقال، ويُسقط حين تكتمل التهيئة.
 * النسبة هي الجاهزية الحقيقيّة لا رقمًا مرسومًا.
 */
@Composable
internal fun StageBase(
    title: String,
    detail: String,
    percent: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val target = percent.coerceIn(0, 100)
    val animated by animateFloatAsState(
        targetValue = target.toFloat(),
        animationSpec = tween(durationMillis = 700)
    )
    Row(
        modifier = modifier
            .clip(BaseShape)
            .background(GlassFill)
            .border(StrokeFine, GlassBorder, BaseShape)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(start = 47.dp, end = 132.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Bolt,
            contentDescription = null,
            tint = GameSpacePalette.VioletSoft,
            modifier = Modifier.size(BaseIcon)
        )
        Spacer(Modifier.width(33.dp))
        StageDivider(BaseIcon)
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = GameSpacePalette.Ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    color = GameSpacePalette.VioletSoft,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Canvas(
            modifier = Modifier
                .size(width = BarWidth, height = BarHeight)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(target / 100f, 0f..1f)
                }
        ) {
            val count = 25
            val gap = size.width * 0.006f
            val skew = size.height * 0.55f
            val segment = (size.width - skew - gap * (count - 1)) / count
            val filled = (count * animated / 100f).roundToInt()
            val fill = Brush.horizontalGradient(
                colors = listOf(GameSpacePalette.VioletDeep, GameSpacePalette.Violet, GameSpacePalette.VioletSoft),
                startX = 0f,
                endX = size.width
            )
            for (i in 0 until count) {
                val x = i * (segment + gap)
                val cell = Path().apply {
                    moveTo(x + skew, 0f)
                    lineTo(x + skew + segment, 0f)
                    lineTo(x + segment, size.height)
                    lineTo(x, size.height)
                    close()
                }
                if (i < filled) {
                    drawPath(path = cell, brush = fill)
                } else {
                    drawPath(path = cell, color = Color.White.copy(alpha = 0.08f))
                }
            }
        }
        Spacer(Modifier.width(21.dp))
        Text(
            text = stringResource(R.string.gs_percent_value, target),
            color = GameSpacePalette.Ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
