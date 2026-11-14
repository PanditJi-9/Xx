/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import nd.max.R
import nd.max.core.platform.SessionVerdict
import nd.max.ui.component.LobbyBattery
import nd.max.ui.component.LobbyMemory
import nd.max.ui.design.MAX_VALUE_UNAVAILABLE
import nd.max.ui.design.MaxAlpha
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius

/*
 * إطار «Game Space»: أربعة أجزاء تحيط بالمسرح —
 *
 *  (١) **شعار اللعب** في مطلع الشريط العلوي (‏Game بيضاء + Space بنفسجيّة + سطر PLAY · BOOST · ENJOY).
 *  (٢) **ساعة الجهاز وتاريخه وبطاريته** قبل أزرار الشريط (كما في التصميم).
 *  (٣) **شريط الأقسام** الرأسي على الحافة، بهدف لمس ≥ ٤٨dp لكل صفّ.
 *  (٤) **لوحة قراءات** أعلى اليمين بثلاث حلقات مجزّأة، و**قاعدة حالة** أسفل الشاشة.
 *
 * ### الهويّة من تصميم المالك نفسه
 *
 * صرّح المالك (٢٠٢٦-١١-١٤) أنّ اللقطة **من تصميمه بالكامل**، فلا تنطبق قاعدة `§0.4/4` (التي
 * تمنع استنساخ شكل تطبيق آخر) على شاشته. فالعبارة «ما لم يُنقل» في §7.7 من السجلّ **لُغيت في هذا
 * الباب**: أُخذت اللوحة البنفسجيّة كما هي، ومعها الحلقات المجزّأة والشعار ذو اللونين وسطر الشعار.
 *
 * **وما بقي مستبعدًا ليس شكلًا بل ادّعاء:** حمل المعالج وحمل الرسم («CPU 38% / GPU 52%») لا
 * يُقرآن في اللوبي، فيُعرض ما يُقرأ فعلًا (البطارية والذاكرة وحرارتها) و«—» لما لا يُقرأ — ADR-07.
 * وشريط «100%» ليس قراءة لشيء، فمكانه حكمُ آخر جلسة وعددُها.
 *
 * ### مرجع الأبعاد
 *
 * تُضبط الأبعاد في المسرح بحسب مساحة العرض المتاحة، مع إبقاء أحجام اللمس مقروءة على الأجهزة المختلفة.
 */

/**
 * لوحة Game Space فحمية ومحايدة؛ البنفسجي لمسات تفاعلية محدودة، والسياني للقراءات الحيّة.
 *
 * **محصورة في هذه الشاشة** ولا تحلّ محلّ رموز التطبيق العامّة: `MaxTone` و`LobbyPalette` تبقى
 * مرجع بقيّة الشاشات، فلا يتفرّع للتطبيق لوحتان.
 */
internal object GameSpacePalette {
    // لوحة فحمية محايدة؛ البنفسجي لمسات تفاعلية لا لونٌ يملأ الواجهة.
    val Void = Color(0xFF070A0F)
    val Deep = Color(0xFF0B1017)
    val Panel = Color(0xFF121923)
    val PanelRaised = Color(0xFF1A2430)
    val Violet = Color(0xFF62558B)
    val VioletDeep = Color(0xFF39364D)
    val VioletSoft = Color(0xFF9A8BC4)
    val Magenta = Color(0xFFB86D83)
    val Cyan = Color(0xFF35B9E8)
    val Mint = Color(0xFF4ADE80)
    val Track = Color(0xFF28323D)
    val Ink = Color(0xFFF1F3F6)
    val Muted = Color(0xFF929BA7)
    val Hairline = Color(0x334F5968)
    val Edge = Color(0x665F6875)
}

/** عرض شريط الأقسام — ٩٦dp، محصور بين الهدف اللمسي ونصف عرض اللقطة. */
internal val GameSpaceRailWidth = MaxSize.minTouchTarget + MaxSize.minTouchTarget

/** ارتفاع قاعدة الحالة المحفوظ أسفل المسرح (٥٦dp ≈ ٧١px). */
internal val GameSpaceStatusHeight = MaxSize.minTouchTarget + MaxSpace.sm

/** أقصى عرض للوحة القراءات (٣٠٠dp ≈ ٢٣٪ من ١٢٨٠dp). */
private val GameSpaceBoardMaxWidth = 300.dp

/** قطر الحلقة: ٧٢dp ≈ ٩٠px في اللقطة. */
private val GaugeDiameter = 72.dp

/**
 * شعار اللعب: «Game» بيضاء و«Space» بنفسجيّة، تحته سطر الشعار المتباعد.
 *
 * [compact] يُسقط السطر وحده على الشاشات الضيّقة، ولا يُسقط الاسم: الاسم هوية الشاشة.
 */
@Composable
internal fun GameSpaceBrand(
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    // الكلمة وحدها: العلامة «M» تسكن شريط الأقسام، فلا تتكرّر هنا.
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
            Text(
                text = stringResource(R.string.gs_brand_game),
                color = GameSpacePalette.Ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.gs_brand_space),
                color = GameSpacePalette.VioletSoft,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
        if (!compact) {
            Text(
                text = stringResource(R.string.gs_brand_tagline),
                color = GameSpacePalette.Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

/**
 * ساعة الجهاز وتاريخه وبطاريته — قراءات حقيقيّة من النظام ([rememberLobbyClock] و[rememberLobbyDate]
 * و`rememberLobbyBattery`)، وما لم يُقرأ يُعلن «—».
 *
 * لا أيقونة Wi-Fi: قراءة حالة الاتّصال تحتاج `ACCESS_WIFI_STATE` من طبقة الواجهة، وأيقونة تعرض حالة
 * غير مقروءة زينةٌ لا خبر — فتُركت، ويُقال ذلك بدل رسمها.
 */
@Composable
internal fun GameSpaceClockCluster(
    clock: String,
    date: String,
    batteryPercent: Int?,
    charging: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
            Text(
                text = clock,
                color = GameSpacePalette.Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = date,
                color = GameSpacePalette.Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
            Icon(
                imageVector = Icons.Rounded.BatteryFull,
                contentDescription = null,
                tint = when {
                    batteryPercent == null -> GameSpacePalette.Muted
                    charging -> GameSpacePalette.Cyan
                    else -> GameSpacePalette.Mint
                },
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
            Text(
                text = batteryPercent?.let { stringResource(R.string.gs_percent_value, it) }
                    ?: MAX_VALUE_UNAVAILABLE,
                color = GameSpacePalette.Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/** تاريخ اليوم بتنسيق النظام — يُقرأ كل دقيقة، ولا يُكتب في أي تفضيل. */
@Composable
internal fun rememberLobbyDate(): State<String> {
    val context = LocalContext.current
    return produceState("", context) {
        while (true) {
            val now = System.currentTimeMillis()
            val locale = Locale.getDefault()
            val pattern = DateFormat.getBestDateTimePattern(locale, "EEEMMMd")
            value = SimpleDateFormat(pattern, locale).format(Date(now))
            delay(60_000L - now % 60_000L + 50L)
        }
    }
}

/**
 * شريط الأقسام على حافة الشاشة. كل صفّ هدف لمس كامل ([MaxSize.minTouchTarget])، ويعلن نفسه
 * تبويبًا ([Role.Tab]) ليدور عليه قارئ الشاشة بمفهوم التنقّل لا الزرّ.
 */
@Composable
internal fun GameSpaceRail(
    selected: LobbySection,
    onSection: (LobbySection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemShape = RoundedCornerShape(maxRadius(MaxRadius.control))
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(GameSpaceRailWidth)
            .background(GameSpacePalette.Deep.copy(alpha = 0.86f))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.md),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        LobbySection.entries.forEach { item ->
            val isSelected = item == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MaxSize.minTouchTarget)
                    .clip(itemShape)
                    .background(
                        if (isSelected) GameSpacePalette.Violet.copy(alpha = 0.34f) else Color.Transparent
                    )
                    .border(
                        MaxSize.hairlineBorder,
                        if (isSelected) GameSpacePalette.VioletSoft else Color.Transparent,
                        itemShape
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = { onSection(item) }
                    )
                    .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
            ) {
                Icon(
                    imageVector = item.icon,
                    // الاسم يقوله النصّ المجاور؛ الأيقونة زخرفة لا تُقرأ وحدها.
                    contentDescription = null,
                    tint = if (isSelected) GameSpacePalette.Ink else GameSpacePalette.Muted,
                    modifier = Modifier.size(MaxSize.iconGlyph)
                )
                Text(
                    text = stringResource(item.titleRes),
                    color = if (isSelected) GameSpacePalette.Ink else GameSpacePalette.Muted,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * لوحة القراءات: ثلاث حلقات مجزّأة (بطارية · ذاكرة · حرارة البطارية) وصفّان أسفلها.
 * لا حمل معالج ولا رسم لأن اللوبي لا يقرأهما، والفراغ يُعلن «—» لا صفرًا.
 */
@Composable
internal fun GameSpaceBoard(
    battery: LobbyBattery?,
    memory: LobbyMemory?,
    tweaks: Int,
    panelEnabled: Boolean,
    onProfile: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.group))
    val usedPercent = memory?.usedPercent()
    val batteryPercent = battery?.percent
    val batteryHeat = battery?.tempTenthsC?.let { it / 10.0 }
    Column(
        modifier = modifier
            .widthIn(max = GameSpaceBoardMaxWidth)
            .clip(shape)
            .background(GameSpacePalette.Panel.copy(alpha = 0.88f))
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
            .padding(MaxSpace.md),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = GameSpacePalette.Magenta,
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
            Text(
                text = stringResource(R.string.gs_board_title),
                color = GameSpacePalette.Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = GameSpacePalette.Muted,
                modifier = Modifier.size(MaxSize.iconGlyphSmall)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
            GaugeCell(
                progress = batteryPercent?.let { it / 100f },
                value = batteryPercent?.let { stringResource(R.string.gs_percent_value, it) }
                    ?: MAX_VALUE_UNAVAILABLE,
                label = stringResource(R.string.gaming_mode_battery),
                tone = GameSpacePalette.Cyan,
                modifier = Modifier.weight(1f)
            )
            GaugeCell(
                progress = usedPercent?.let { it / 100f },
                value = usedPercent?.let { stringResource(R.string.gs_percent_value, it) }
                    ?: MAX_VALUE_UNAVAILABLE,
                label = stringResource(R.string.max_hub_memory),
                tone = GameSpacePalette.VioletSoft,
                modifier = Modifier.weight(1f)
            )
            GaugeCell(
                // لا مقياس ٠–١٠٠ لحرارة البطارية، فلا تُرسم نسبة كاذبة: الحلقة إطار والرقم هو الخبر.
                progress = null,
                value = batteryHeat?.let { stringResource(R.string.gs_heat_value, it) }
                    ?: MAX_VALUE_UNAVAILABLE,
                label = stringResource(R.string.gs_battery_heat),
                tone = GameSpacePalette.Magenta,
                modifier = Modifier.weight(1f)
            )
        }
        // على الشاشات القصيرة (هاتف عرضيّ) يبقى الرقم والقراءة، ويسقط الهامش التوضيحيّ وصفّ
        // الملف: الأوّل تكرار لشريط الأقسام، والثاني يُفتح من الشريط نفسه.
        if (!compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MaxSpace.xxl)
                    .clip(RoundedCornerShape(maxRadius(MaxRadius.chip)))
                    .background(GameSpacePalette.PanelRaised.copy(alpha = 0.55f))
                    .clickable(role = Role.Button, onClick = onProfile)
                    .padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = GameSpacePalette.VioletSoft,
                    modifier = Modifier.size(MaxSize.iconGlyphSmall)
                )
                Text(
                    text = stringResource(R.string.lobby_tile_profile_value, tweaks),
                    color = GameSpacePalette.Ink,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        if (panelEnabled) R.string.lobby_arena_panel_armed else R.string.lobby_arena_panel_off
                    ),
                    color = if (panelEnabled) GameSpacePalette.Cyan else GameSpacePalette.Muted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/** حلقة قراءة واحدة: مسار مجزّأ خافت، وقوس بالقيمة، والرقم في المركز. */
@Composable
private fun GaugeCell(
    progress: Float?,
    value: String,
    label: String,
    tone: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)
    ) {
        Box(modifier = Modifier.size(GaugeDiameter), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(GaugeDiameter)) {
                val dash = PathEffect.dashPathEffect(
                    floatArrayOf(size.minDimension * 0.085f, size.minDimension * 0.055f)
                )
                val stroke = Stroke(width = size.minDimension * 0.09f, cap = StrokeCap.Round, pathEffect = dash)
                drawArc(
                    color = GameSpacePalette.Track,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke
                )
                if (progress != null) {
                    drawArc(
                        color = tone,
                        startAngle = -90f,
                        sweepAngle = 360f * progress.coerceIn(0f, 1f),
                        useCenter = false,
                        style = stroke
                    )
                }
            }
            Text(
                text = value,
                color = if (value == MAX_VALUE_UNAVAILABLE) GameSpacePalette.Muted else GameSpacePalette.Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Text(
            text = label,
            color = GameSpacePalette.Muted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * قاعدة الحالة أسفل الشاشة: حكم آخر جلسة (أو «جاهز» إن لم تُسجَّل) وعدد الجلسات، وتفتح السجلّ.
 * لا شريط نسبة مئويّة: «١٠٠٪» في التصميم ليست قراءة لشيء، والادّعاء يُخالف ADR-07.
 */
@Composable
internal fun GameSpaceStatusBar(
    label: String,
    detail: String,
    tone: Color,
    onSessions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(maxRadius(MaxRadius.control))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = GameSpaceStatusHeight)
            .clip(shape)
            .background(GameSpacePalette.Deep.copy(alpha = 0.90f))
            .border(MaxSize.hairlineBorder, GameSpacePalette.Hairline, shape)
            .clickable(role = Role.Button, onClick = onSessions)
            .padding(horizontal = MaxSpace.xl, vertical = MaxSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)
    ) {
        Box(
            modifier = Modifier
                .size(MaxSize.iconGlyphSmall)
                .clip(RoundedCornerShape(maxRadius(MaxRadius.pill)))
                .background(if (tone == GameSpacePalette.Muted) GameSpacePalette.VioletSoft else tone)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)
        ) {
            Text(
                text = label,
                color = GameSpacePalette.Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                color = GameSpacePalette.Muted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = GameSpacePalette.Muted,
            modifier = Modifier.size(MaxSize.iconGlyphSmall)
        )
    }
}

/** نصّ حكم الجلسة من مفاتيح السجلّ نفسها (`GameSessionsScreen`) — لا مفردات ثانية للحكم نفسه. */
@Composable
internal fun gameSpaceVerdictLabel(verdict: SessionVerdict): String = stringResource(
    when (verdict) {
        SessionVerdict.Better -> R.string.game_sessions_verdict_better
        SessionVerdict.Similar -> R.string.game_sessions_verdict_similar
        SessionVerdict.Worse -> R.string.game_sessions_verdict_worse
        SessionVerdict.FirstRun -> R.string.game_sessions_verdict_first
        SessionVerdict.Unusable -> R.string.game_sessions_verdict_unusable
    }
)

/** لون الحكم: لون اللوحة نفسه (سماء/ورديّ) لا لون اللعبة — الحكم خبر لا هوية. */
internal fun gameSpaceVerdictTone(verdict: SessionVerdict): Color = when (verdict) {
    SessionVerdict.Better -> GameSpacePalette.Cyan
    SessionVerdict.Similar -> GameSpacePalette.Muted
    SessionVerdict.Worse, SessionVerdict.Unusable -> GameSpacePalette.Magenta
    SessionVerdict.FirstRun -> GameSpacePalette.VioletSoft
}

/** نسبة الذاكرة المشغولة من قراءة `ActivityManager` — `null` حتى تُقرأ، لا صفرًا. */
private fun LobbyMemory.usedPercent(): Int? {
    if (totalBytes <= 0L) return null
    val used = (totalBytes - availBytes).toDouble() / totalBytes * 100.0
    return used.roundToInt().coerceIn(0, 100)
}
