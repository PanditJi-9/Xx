/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.ui.component.rememberLobbyIcon

/*
 * البطل والرفّ بإحداثيات اللقطة (١٦٧٢×٩٤١) — انظر `GameSpaceStage.kt`.
 *
 * لا فنّ لعبة: اللقطة تحمل فنّ PUBG وهو ليس ملكنا ولا يأتي مع `PackageManager`. فالبطل يعرض **أيقونة
 * التطبيق الحقيقيّة** على لوح بهالة، وبطاقات الرفّ تأخذ الأيقونة نفسها فوق خلفية مموّهة منها (أندرويد ١٢+؛
 * قبله تدرّج بنفسجيّ وحده). الأسماء أرقامٌ مسمّاة لا حرفيّة، لبوّابة `design_tokens`.
 */

private val RadiusPanel = 32.dp
private val RadiusCard = 18.dp
private val RadiusMore = 24.dp
private val StrokeFine = 1.dp
private val StrokeFirm = 1.5.dp
private val StrokeSelected = 3.dp
private val GapCard = 19.dp
private val PadCta = 26.dp
private val PadName = 10.dp
private val PadNameV = 12.dp
private val GapS = 10.dp
private val GapM = 16.dp
private val PlayWidth = 248.dp
private val PlayHeight = 57.dp
private val MoreSize = 58.dp
private val PlayGlyph = 30.dp
private val CardWidth = 184.dp
private val CardHeight = 214.dp
private val CardIcon = 112.dp
private val MoreWidth = 158.dp
private val MoreGlyph = 52.dp
private val ArrowSize = 38.dp
private val ArrowGlyph = 24.dp
private val NotchWidth = 3.dp
private val NotchHeight = 23.dp
private val DotSize = 11.dp
private val DotActiveWidth = 26.dp
private val DotGap = 15.dp
private val ArtEdge = 250.dp
private val GlowS = 14.dp
private val GlowL = 40.dp
private val SeeAllGlyph = 24.dp

private const val ShelfMoreKey = "stage.shelf.more"

private val PlayFill = Brush.horizontalGradient(
    listOf(GameSpacePalette.Plum.copy(alpha = 0.88f), GameSpacePalette.VioletDeep.copy(alpha = 0.55f))
)
private val PlayRim = Brush.horizontalGradient(
    listOf(GameSpacePalette.VioletSoft, GameSpacePalette.Violet.copy(alpha = 0.60f), GameSpacePalette.VioletSoft)
)
private val SelectedRim = Brush.linearGradient(
    listOf(GameSpacePalette.Lavender, GameSpacePalette.VioletSoft, GameSpacePalette.Violet)
)
private val CardFill = Brush.verticalGradient(listOf(Color(0xFF2B1660), Color(0xFF0C0626)))
private val IdleRim = Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.14f))
)

/**
 * البطل: لوح اللعبة في الوسط (مركزه ٨٥٠،٢٥٥)، واسمها الكبير وسطر «آخر لعب» وزرّ «Play Now» عند
 * (٢٣٠،١٥٠)، والنقاط عند (٨٠٣،٤٦٣). بلا ألعاب: بطاقة فراغ تقود إلى المكتبة، لا «٠٪» ولا زرّ معطّل.
 */
@Composable
internal fun StageHero(data: StageData, actions: StageActions) {
    Box(Modifier.at(620, 25, 460, 460), contentAlignment = Alignment.Center) {
        HeroArt(game = data.selected, edge = ArtEdge)
    }
    StageDots(
        count = data.games.size,
        index = if (data.selected == null) -1 else data.selectedIndex,
        modifier = Modifier.at(803, 463, 140, 12)
    )
    if (data.games.isEmpty()) {
        EmptyHero(onOpenLibrary = actions.onOpenLibrary, modifier = Modifier.at(230, 150, 340, 300))
        return
    }
    Box(Modifier.at(230, 150, 280, 170), contentAlignment = Alignment.Center) {
        Crossfade(
            targetState = data.selected?.label.orEmpty(),
            animationSpec = tween(durationMillis = 260)
        ) { label ->
            Text(
                text = label,
                color = GameSpacePalette.Ink,
                fontSize = 46.sp,
                lineHeight = 50.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    shadow = Shadow(
                        color = GameSpacePalette.VioletSoft.copy(alpha = 0.55f),
                        offset = Offset.Zero,
                        blurRadius = 28f
                    )
                )
            )
        }
    }
    data.tagline?.let { line ->
        Box(Modifier.at(230, 322, 280, 36), contentAlignment = Alignment.Center) {
            Text(
                text = line,
                color = GameSpacePalette.Ink.copy(alpha = 0.92f),
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    PlayRow(
        enabled = data.selected != null,
        onLaunch = actions.onLaunch,
        onOptions = actions.onOptions,
        modifier = Modifier.at(230, 391, 340, 58)
    )
}

/** «Play Now» بحدّ بنفسجيّ متدرّج على خلفية داكنة (٢٤٨×٥٧)، و«…» دائرة (٥٨) بجواره. */
@Composable
private fun PlayRow(enabled: Boolean, onLaunch: () -> Unit, onOptions: () -> Unit, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val shape = RoundedCornerShape(PlayHeight)
        Row(
            modifier = Modifier
                .width(PlayWidth)
                .height(PlayHeight)
                .alpha(if (enabled) 1f else 0.5f)
                .shadow(
                    elevation = GlowS,
                    shape = shape,
                    ambientColor = GameSpacePalette.Violet.copy(alpha = 0.45f),
                    spotColor = GameSpacePalette.VioletSoft.copy(alpha = 0.70f)
                )
                .clip(shape)
                .background(PlayFill)
                .border(StrokeFirm, PlayRim, shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onLaunch),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GapS, Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(PlayGlyph)
            )
            Text(
                text = stringResource(R.string.gs_launch),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(30.dp))
        Box(
            modifier = Modifier
                .size(MoreSize)
                .clip(CircleShape)
                .background(GameSpacePalette.Plum.copy(alpha = 0.90f))
                .border(StrokeFine, GameSpacePalette.VioletSoft.copy(alpha = 0.45f), CircleShape)
                .clickable(role = Role.Button, onClick = onOptions),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = stringResource(R.string.gs_more),
                tint = GameSpacePalette.Ink,
                modifier = Modifier.size(PlayGlyph)
            )
        }
    }
}

/** لوح اللعبة: أيقونة التطبيق الحقيقيّة بحدّ متدرّج، فوق هالة بنفسجيّة وحلقتين رفيعتين. */
@Composable
private fun HeroArt(game: GameApp?, edge: Dp) {
    Box(
        modifier = Modifier
            .size(edge * 1.84f)
            .drawBehind {
                val reach = size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GameSpacePalette.Violet.copy(alpha = 0.55f),
                            GameSpacePalette.Magenta.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = reach
                    ),
                    radius = reach,
                    center = center
                )
                drawCircle(
                    color = GameSpacePalette.VioletSoft.copy(alpha = 0.35f),
                    radius = reach * 0.82f,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = GameSpacePalette.Lavender.copy(alpha = 0.14f),
                    radius = reach * 0.97f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = game?.packageName,
            animationSpec = tween(durationMillis = 320)
        ) { packageName ->
            HeroIcon(packageName = packageName, label = game?.label.orEmpty(), edge = edge)
        }
    }
}

@Composable
private fun HeroIcon(packageName: String?, label: String, edge: Dp) {
    val icon by rememberLobbyIcon(packageName)
    val shape = RoundedCornerShape(edge * 0.24f)
    Box(
        modifier = Modifier
            .size(edge)
            .shadow(
                elevation = GlowL,
                shape = shape,
                ambientColor = GameSpacePalette.Violet.copy(alpha = 0.50f),
                spotColor = GameSpacePalette.VioletSoft.copy(alpha = 0.80f)
            )
            .clip(shape)
            .background(CardFill)
            .border(StrokeSelected, SelectedRim, shape),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = icon
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = label.take(1).uppercase(),
                color = GameSpacePalette.Ink,
                fontSize = (edge.value * 0.42f).sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun EmptyHero(onOpenLibrary: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(RadiusCard)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(GapM)) {
        Icon(
            imageVector = Icons.Rounded.VideogameAsset,
            contentDescription = null,
            tint = GameSpacePalette.VioletSoft,
            modifier = Modifier.size(MoreGlyph)
        )
        Text(
            text = stringResource(R.string.gs_empty_title),
            color = GameSpacePalette.Ink,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.gs_empty_body),
            color = GameSpacePalette.Ink.copy(alpha = 0.75f),
            fontSize = 19.sp
        )
        Row(
            modifier = Modifier
                .height(PlayHeight)
                .clip(shape)
                .background(PlayFill)
                .border(StrokeFirm, PlayRim, shape)
                .clickable(role = Role.Button, onClick = onOpenLibrary)
                .padding(horizontal = PadCta),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GapS)
        ) {
            Icon(
                imageVector = Icons.Rounded.VideogameAsset,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(PlayGlyph)
            )
            Text(
                text = stringResource(R.string.gs_empty_action),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/** نقاط التنقّل: النقطة الحالية قرص ممتدّ بنفسجيّ (٢٦×١١)، والبقيّة دوائر (١١) باهتة. */
@Composable
private fun StageDots(count: Int, index: Int, modifier: Modifier = Modifier) {
    if (count <= 1) return
    val window = 5
    val start = (index - window / 2).coerceIn(0, (count - window).coerceAtLeast(0))
    val end = (start + window).coerceAtMost(count)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DotGap)
    ) {
        for (i in start until end) {
            val active = i == index
            Box(
                modifier = Modifier
                    .height(DotSize)
                    .width(if (active) DotActiveWidth else DotSize)
                    .clip(CircleShape)
                    .background(if (active) GameSpacePalette.Violet else Color.White.copy(alpha = 0.35f))
            )
        }
    }
}

/**
 * رفّ الألعاب (١٩٥،٥٠٩، ١٤٥١×٣٠٦): لوح زجاجيّ بعنوان ذي شقّ بنفسجيّ و«See All»، صفّ بطاقات
 * (١٨٤×٢١٤ عند y=٥٧٣) ينتهي ببطاقة «More»، وسهمان دائريّان على حافتي اللوح (مركزاهما ١٩٩ و١٦٤٥، y=٦٨١).
 */
@Composable
internal fun StageShelf(data: StageData, actions: StageActions) {
    val listState = rememberLazyListState()
    // اختيارٌ بالأسهم أو النقاط يجب أن يُظهر بطاقته، لا أن يتركها خارج اللوح.
    LaunchedEffect(data.selectedIndex) {
        if (data.selectedIndex in data.games.indices) listState.animateScrollToItem(data.selectedIndex)
    }
    Box(Modifier.at(195, 509, 1451, 306).stageGlass(RoundedCornerShape(RadiusPanel)))
    Row(
        modifier = Modifier.at(217, 520, 1405, 36),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GapM)
    ) {
        Box(
            Modifier
                .width(NotchWidth)
                .height(NotchHeight)
                .background(GameSpacePalette.Violet)
        )
        Text(
            text = stringResource(R.string.gs_shelf),
            color = GameSpacePalette.Ink,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(RadiusCard))
                .clickable(role = Role.Button, onClick = actions.onOpenLibrary),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GapS)
        ) {
            Text(
                text = stringResource(R.string.gs_view_all),
                color = GameSpacePalette.Ink.copy(alpha = 0.88f),
                fontSize = 18.sp,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = GameSpacePalette.Ink.copy(alpha = 0.88f),
                modifier = Modifier.size(SeeAllGlyph)
            )
        }
    }
    LazyRow(
        modifier = Modifier.at(228, 573, 1384, 214),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(GapCard)
    ) {
        items(data.games, key = { it.packageName }) { game ->
            ShelfCard(
                game = game,
                selected = game.packageName == data.selected?.packageName,
                onClick = { actions.onSelect(game.packageName) }
            )
        }
        item(key = ShelfMoreKey) {
            ShelfMore(onClick = actions.onOpenLibrary)
        }
    }
    ShelfArrow(
        icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
        description = stringResource(R.string.gs_prev),
        enabled = data.selectedIndex > 0,
        onClick = { data.games.getOrNull(data.selectedIndex - 1)?.let { actions.onSelect(it.packageName) } },
        modifier = Modifier.at(180, 662, 38, 38)
    )
    ShelfArrow(
        icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        description = stringResource(R.string.gs_next),
        enabled = data.selectedIndex < data.games.lastIndex,
        onClick = { data.games.getOrNull(data.selectedIndex + 1)?.let { actions.onSelect(it.packageName) } },
        modifier = Modifier.at(1626, 662, 38, 38)
    )
}

@Composable
private fun ShelfArrow(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(GameSpacePalette.Glass.copy(alpha = 0.92f))
            .border(StrokeFine, Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = GameSpacePalette.Ink,
            modifier = Modifier.size(ArrowGlyph)
        )
    }
}

/** بطاقة «More» في آخر الرفّ (١٥٨×٢١٤): تفتح المكتبة كاملةً. */
@Composable
private fun ShelfMore(onClick: () -> Unit) {
    val shape = RoundedCornerShape(RadiusMore)
    Column(
        modifier = Modifier
            .size(width = MoreWidth, height = CardHeight)
            .stageGlass(shape)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Apps,
            contentDescription = null,
            tint = GameSpacePalette.Lavender.copy(alpha = 0.70f),
            modifier = Modifier.size(MoreGlyph)
        )
        Spacer(Modifier.height(GapM))
        Text(
            text = stringResource(R.string.gs_more_tile),
            color = GameSpacePalette.Ink,
            fontSize = 18.sp,
            maxLines = 1
        )
    }
}

/**
 * بطاقة لعبة (١٨٤×٢١٤): خلفية مموّهة من أيقونتها والأيقونة نفسها واضحة فوقها، واسم اللعبة على حجاب
 * سفليّ. المختارة وحدها تحمل حدًّا متدرّجًا (٣) وتوهّجًا.
 */
@Composable
private fun ShelfCard(game: GameApp, selected: Boolean, onClick: () -> Unit) {
    val icon by rememberLobbyIcon(game.packageName)
    val shape = RoundedCornerShape(RadiusCard)
    Box(
        modifier = Modifier
            .size(width = CardWidth, height = CardHeight)
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = GlowS,
                        shape = shape,
                        ambientColor = GameSpacePalette.Violet.copy(alpha = 0.50f),
                        spotColor = GameSpacePalette.VioletSoft.copy(alpha = 0.80f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(CardFill)
            .border(if (selected) StrokeSelected else StrokeFine, if (selected) SelectedRim else IdleRim, shape)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        val bitmap = icon
        if (bitmap != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.55f)
                        .blur(GapM + GapS)
                )
            }
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 26.dp)
                    .size(CardIcon)
                    .clip(RoundedCornerShape(CardIcon * 0.22f))
            )
        } else {
            Text(
                text = game.label.take(1).uppercase(),
                color = GameSpacePalette.Ink,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, GameSpacePalette.Void.copy(alpha = 0.88f))
                    )
                )
                .padding(horizontal = PadName, vertical = PadNameV)
        ) {
            Text(
                text = game.label,
                color = if (selected) GameSpacePalette.Ink else GameSpacePalette.Lavender,
                fontSize = 17.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
