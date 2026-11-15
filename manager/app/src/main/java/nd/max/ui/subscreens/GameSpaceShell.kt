/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nd.max.ui.design.MaxRadius
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.ui.component.AppIconImage

/*
 * غلاف اللوبي: ما يبقى ثابتًا فوق صفحات المُصفِّح الرأسيّ.
 *
 *  - أفقيًّا: الشريط الجانبيّ المرسوم بإحداثيات اللقطة نفسها وبالنسبة نفسها التي تُرسم بها «الرئيسية»،
 *    فيقع حيث تقع في اللقطة تمامًا ولا يتحرّك مع السحب. صفحات الأقسام تُزاح عن حافته.
 *  - عموديًّا (تخطيط بديل): شريط سفليّ بالأقسام نفسها، و«الرئيسية» عمود واحد من الكتل ذاتها.
 */

/**
 * صفحات اللوبي: «الرئيسية» أولًا، ثم صفحة لكل قسم من أقسام مركز التحكّم، والترس آخرًا.
 *
 * [section] هو القسم الذي تعرضه الصفحة (`null` للرئيسية وحدها). والترس يعرض قسم «اللوبي» لأن
 * إعدادات مركز التحكّم تعيش هناك، وله تسمية وأيقونة خاصّتان به فلا يُقرأ «لوبي» على بندٍ اسمه «إعدادات».
 */
internal enum class LobbyPage(val section: LobbySection?) {
    Home(null),
    Loadout(LobbySection.Loadout),
    Insights(LobbySection.Insights),
    Game(LobbySection.Game),
    Panel(LobbySection.Panel),
    Library(LobbySection.Library),
    Settings(LobbySection.Lobby);

    /** عنوان البند في الشريط الجانبيّ والشريط السفليّ — من الموارد، فلا نصّ صلب في الواجهة. */
    val labelRes: Int
        get() = when (this) {
            Home -> R.string.gs_nav_home
            Settings -> R.string.gs_nav_settings
            else -> requireNotNull(section).titleRes
        }

    val icon: ImageVector
        get() = when (this) {
            Home -> Icons.Rounded.Home
            Settings -> Icons.Rounded.Settings
            else -> requireNotNull(section).icon
        }

    companion object {
        /** الصفحة عند رقم مُصفِّحٍ قد يخرج عن المدى — يُقيَّد ولا يرمي. */
        fun at(index: Int): LobbyPage = entries[index.coerceIn(0, entries.lastIndex)]

        /** صفحة القسم المطلوب: «لوبي» تعني الترس، وكل قسم آخر يملك صفحته. */
        fun of(section: LobbySection): LobbyPage =
            entries.firstOrNull { it.section == section } ?: Home
    }
}

private const val RailRightEdge = 168
private const val ShellMaxFontScale = 1.15f

/** هندسة اللوحة الثابتة داخل مساحة معيّنة: النسبة، وحافة الشريط اليمنى لإزاحة صفحات الأقسام عنها. */
internal class StageFrame(val scale: Float, val railEnd: Dp)

internal fun stageFrame(width: Dp, height: Dp): StageFrame {
    val scale = minOf(width.value / StageDesignWidth, height.value / StageDesignHeight)
    val left = (width.value - StageDesignWidth * scale) / 2f
    return StageFrame(scale = scale, railEnd = (left + RailRightEdge * scale).dp)
}

/**
 * الشريط الجانبيّ فوق المُصفِّح. لا يعترض اللمس خارج بنوده، فالسحب الرأسيّ على الصفحة يصل إلى
 * المُصفِّح، ولمس بند ينقله إلى صفحته.
 */
@Composable
internal fun GameSpaceRailLayer(
    current: LobbyPage,
    onPage: (LobbyPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val real = LocalDensity.current
        val scale = minOf(maxWidth.value / StageDesignWidth, maxHeight.value / StageDesignHeight)
        val design = Density(
            density = real.density * scale,
            fontScale = minOf(real.fontScale, ShellMaxFontScale)
        )
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Ltr,
            LocalDensity provides design
        ) {
            Box(Modifier.requiredSize(StageDesignWidth.dp, StageDesignHeight.dp)) {
                StageRail(current = current, onPage = onPage)
            }
        }
    }
}

internal val PortraitNavHeight = 64.dp
private const val NavTintMs = 200
private val PortraitHeroHeight = 340.dp

/** الشريط السفليّ للتخطيط العموديّ: الأقسام نفسها بالترتيب نفسه، والصفحة الظاهرة مضاءة. */
@Composable
internal fun GameSpaceBottomNav(
    current: LobbyPage,
    onPage: (LobbyPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PortraitNavHeight)
            .background(
                Brush.verticalGradient(
                    listOf(GameSpacePalette.Glass.copy(alpha = 0.92f), GameSpacePalette.Void.copy(alpha = 0.97f))
                )
            )
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.10f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        LobbyPage.entries.forEach { page ->
            val active = page == current
            val tint by animateColorAsState(
                targetValue = if (active) Color.White else GameSpacePalette.Ink.copy(alpha = 0.66f),
                animationSpec = tween(durationMillis = NavTintMs),
                label = "navTint"
            )
            val bar by animateColorAsState(
                targetValue = if (active) GameSpacePalette.VioletSoft else Color.Transparent,
                animationSpec = tween(durationMillis = NavTintMs),
                label = "navBar"
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(selected = active, role = Role.Tab, onClick = { onPage(page) }),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier
                        .width(24.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(bar)
                )
                Spacer(Modifier.height(4.dp))
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(page.labelRes),
                    color = tint,
                    fontSize = 10.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** كتلة بأبعاد اللقطة تُكبَّر أو تُصغَّر لتملأ العرض المتاح: كروت اللوحة نفسها بلا نسخة عموديّة ثانية. */
@Composable
private fun StageScaled(
    designWidth: Int,
    designHeight: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val real = LocalDensity.current
        val scale = maxWidth.value / designWidth
        Box(Modifier.fillMaxWidth().height((designHeight * scale).dp)) {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Ltr,
                LocalDensity provides Density(
                    density = real.density * scale,
                    fontScale = minOf(real.fontScale, ShellMaxFontScale)
                )
            ) {
                Box(Modifier.requiredSize(designWidth.dp, designHeight.dp)) { content() }
            }
        }
    }
}

/**
 * «الرئيسية» في التخطيط العموديّ: عمود واحد — الساعة والأزرار، البطل، لوحات الأداء، رفّ الألعاب، قاعدة
 * الجاهزيّة — بالمعطيات نفسها التي يقرؤها التخطيط الأفقيّ. يتمرّر داخليًّا إن ضاق الارتفاع.
 */
@Composable
internal fun StagePortrait(
    data: StageData,
    actions: StageActions,
    topActions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = PortraitNavHeight + 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = data.clock,
                color = GameSpacePalette.Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = data.date,
                color = GameSpacePalette.Ink.copy(alpha = 0.75f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { topActions() }

        Box(Modifier.fillMaxWidth().height(PortraitHeroHeight)) {
            StageHeroBackdrop(
                game = data.selected,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(MaxRadius.sheet))
            )
            if (data.games.isEmpty()) {
                EmptyHero(
                    onOpenLibrary = actions.onOpenLibrary,
                    modifier = Modifier.align(Alignment.Center).width(300.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    HeroArt(game = data.selected, edge = 92.dp)
                    Text(
                        text = data.selected?.label.orEmpty(),
                        color = GameSpacePalette.Ink,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    data.tagline?.let { line ->
                        Text(
                            text = line,
                            color = GameSpacePalette.Ink.copy(alpha = 0.92f),
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    PlayRow(
                        enabled = data.selected != null,
                        onLaunch = actions.onLaunch,
                        onOptions = actions.onOptions,
                        modifier = Modifier
                    )
                }
            }
        }

        StageScaled(designWidth = 410, designHeight = 418) {
            PerformanceCard(data = data, onOpen = actions.onOptions, modifier = Modifier.at(0, 0, 410, 229))
            StatusCard(data = data, modifier = Modifier.at(0, 241, 410, 91))
            FpsCard(fps = data.fps, modifier = Modifier.at(0, 333, 410, 85))
        }

        if (data.games.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(data.games, key = { it.packageName }) { game ->
                    PortraitShelfItem(
                        game = game,
                        selected = game.packageName == data.selected?.packageName,
                        onClick = { actions.onSelect(game.packageName) }
                    )
                }
            }
        }

        PortraitBase(
            title = data.statusTitle,
            detail = data.statusDetail,
            percent = data.readinessPercent,
            onOpen = actions.onOpenSessions
        )
    }
}

@Composable
private fun PortraitShelfItem(game: GameApp, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(MaxRadius.deck)
    Column(
        modifier = Modifier
            .width(84.dp)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF2B1660), Color(0xFF0C0626))))
                .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) GameSpacePalette.VioletSoft else Color.White.copy(alpha = 0.14f),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            AppIconImage(packageName = game.packageName, size = 56.dp, contentDescription = null)
        }
        Text(
            text = game.label,
            color = if (selected) GameSpacePalette.Ink else GameSpacePalette.Lavender,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/** قاعدة الجاهزيّة العموديّة: برق، حكم آخر جلسة، نسبة، وشريط تقدّم رفيع. النسبة هي الجاهزيّة الحقيقيّة. */
@Composable
private fun PortraitBase(title: String, detail: String, percent: Int, onOpen: () -> Unit) {
    val target = percent.coerceIn(0, 100)
    val shape = RoundedCornerShape(MaxRadius.deck)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .stageGlass(shape)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = GameSpacePalette.VioletSoft,
                modifier = Modifier.size(26.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = GameSpacePalette.Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        color = GameSpacePalette.VioletSoft,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = stringResource(R.string.gs_percent_value, target),
                color = GameSpacePalette.Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(MaxRadius.tick))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(target / 100f)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(GameSpacePalette.VioletDeep, GameSpacePalette.Violet, GameSpacePalette.VioletSoft)
                        )
                    )
            )
        }
    }
}
