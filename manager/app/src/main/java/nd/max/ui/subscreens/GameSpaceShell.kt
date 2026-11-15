/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.ui.component.AppIconImage
import nd.max.ui.component.lobbyPress

/*
 * غلاف اللوبي: ما يبقى ثابتًا فوق صفحات المُصفِّح الرأسيّ.
 *
 *  - أفقيًّا: الشريط الجانبيّ المرسوم بإحداثيات اللقطة نفسها وبالنسبة نفسها التي تُرسم بها «الرئيسية»،
 *    فيقع حيث تقع في اللقطة تمامًا ولا يتحرّك مع السحب. صفحات الأقسام تُزاح عن حافته.
 *  - عموديًّا (تخطيط بديل): شريط سفليّ بالأقسام نفسها، و«الرئيسية» عمود واحد من الكتل ذاتها.
 */

private const val RailRightEdge = 168
private const val ShellMaxFontScale = 1.15f

private val NavHairline = 1.dp
private val PortraitHeroRadius = 28.dp
private val PortraitCardRadius = 20.dp
private val PortraitBarRadius = 3.dp

/** هندسة اللوحة الثابتة داخل مساحة معيّنة: النسبة، وحافة الشريط اليمنى لإزاحة صفحات الأقسام عنها. */
internal class StageFrame(val scale: Float, val railEnd: Dp)

internal fun stageFrame(width: Dp, height: Dp): StageFrame {
    val space = stageSpace(width, height)
    val left = (width.value - (StageDesignWidth + space.extra) * space.scale) / 2f
    return StageFrame(scale = space.scale, railEnd = (left + RailRightEdge * space.scale).dp)
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
        val space = stageSpace(maxWidth, maxHeight)
        val design = Density(
            density = real.density * space.scale,
            fontScale = stageFontScale(real.fontScale, space.scale)
        )
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Ltr,
            LocalDensity provides design
        ) {
            Box(Modifier.requiredSize((StageDesignWidth + space.extra).dp, StageDesignHeight.dp)) {
                StageRail(current = current, onPage = onPage)
            }
        }
    }
}

internal val PortraitNavHeight = 64.dp
private val PortraitHeroHeight = 340.dp

/** الشريط السفليّ للتخطيط العموديّ: الأقسام نفسها بالترتيب نفسه، والصفحة الظاهرة مضاءة بانتقال. */
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
                    listOf(GameSpacePalette.Glass.copy(alpha = 0.94f), GameSpacePalette.Void.copy(alpha = 0.98f))
                )
            )
            .drawBehind {
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, GameSpacePalette.VioletSoft.copy(alpha = 0.55f), Color.Transparent)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = NavHairline.toPx()
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        LobbyPage.entries.forEach { page ->
            BottomNavItem(page = page, active = page == current, onClick = { onPage(page) })
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(page: LobbyPage, active: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val selection by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "bottomNavSelection"
    )
    val tint = lerp(GameSpacePalette.Ink.copy(alpha = 0.66f), Color.White, selection)
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .lobbyPress(interaction, 0.92f)
            .selectable(
                selected = active,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .width(24.dp)
                .height(3.dp)
                .graphicsLayer {
                    scaleX = selection
                    alpha = selection
                }
                .clip(CircleShape)
                .background(GameSpacePalette.VioletSoft)
        )
        Spacer(Modifier.height(4.dp))
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    val k = 1f + 0.10f * selection
                    scaleX = k
                    scaleY = k
                }
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
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(PortraitHeroRadius))
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
                        modifier = Modifier,
                        launching = data.launching
                    )
                }
            }
        }

        StageScaled(designWidth = 410, designHeight = 418) {
            PerformanceCard(data = data, onOpen = actions.onOptions, modifier = Modifier.at(0, 0, 410, 229))
            StatusCard(data = data, modifier = Modifier.at(0, 241, 410, 91), onOpen = actions.onOptions)
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
    val shape = RoundedCornerShape(PortraitCardRadius)
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
    val shape = RoundedCornerShape(PortraitCardRadius)
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
                .clip(RoundedCornerShape(PortraitBarRadius))
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
