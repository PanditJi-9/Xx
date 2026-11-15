/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.ui.component.lobbyIntro
import nd.max.ui.component.lobbyPress
import nd.max.ui.component.rememberLobbyIcon
import nd.max.ui.component.rememberLobbyTone
import nd.max.ui.theme.DisplayFontFamily

/*
 * البطل والرفّ بإحداثيات اللقطة (١٦٧٢×٩٤١) — انظر `GameSpaceStage.kt`.
 *
 * لا فنّ لعبة: اللقطة تحمل فنّ PUBG وهو ليس ملكنا ولا يأتي مع `PackageManager`. فالبطل يعرض **أيقونة
 * التطبيق الحقيقيّة** على لوح عائم بهالة بلون اللعبة نفسها (من أيقونتها) وحلقتين يدور عليهما ضوءان، وبطاقات
 * الرفّ تأخذ الأيقونة نفسها فوق خلفية مموّهة منها (أندرويد ١٢+؛ قبله تدرّج بنفسجيّ وحده). الأسماء أرقامٌ
 * مسمّاة لا حرفيّة، لبوّابة `design_tokens`.
 *
 * التفاعل: السحب الأفقيّ على البطل يبدّل اللعبة، والبطاقة المختارة تتوسّط الرفّ وترتفع، وكل زرّ ينكمش
 * بزنبرك عند اللمس، وزرّ «Play Now» تعبره لمعة وتتحوّل أيقونته إلى مؤشّر تقدّم أثناء الإطلاق.
 */

private val TwoPi = (2.0 * PI).toFloat()

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
private val ProgressStroke = 3.dp
private val CardWidth = 184.dp
private val CardHeight = 214.dp
private val CardIcon = 120.dp
private val MoreWidth = 158.dp
private val MoreGlyph = 52.dp
private val ArrowSize = 38.dp
private val ArrowGlyph = 28.dp
private val NotchWidth = 4.dp
private val NotchHeight = 24.dp
private val DotSize = 11.dp
private val DotActiveWidth = 28.dp
private val DotGap = 15.dp
private val ArtEdge = 292.dp
private val GlowS = 14.dp
private val GlowL = 40.dp
private val SeeAllGlyph = 24.dp
private val ShelfInset = 8.dp
private val LiftDistance = 7.dp
private val FloatDistance = 9.dp
private val OrbitGlow = 9.dp
private val OrbitDot = 2.6.dp

private const val ShelfMoreKey = "stage.shelf.more"
private const val SwipeShare = 0.10f

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
    listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.06f))
)
private val TitleFill = Brush.verticalGradient(listOf(Color.White, GameSpacePalette.Lavender))
private val NotchFill = Brush.verticalGradient(listOf(GameSpacePalette.VioletSoft, GameSpacePalette.Violet))
private val CardSheen = Brush.linearGradient(
    colors = listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
    start = Offset.Zero,
    end = Offset(260f, 300f)
)

/**
 * البطل: لوح اللعبة العائم في الوسط (مركزه ٨٥٠،٢٥٥)، واسمها الكبير بتدرّج لافندريّ وسطر «آخر لعب» وزرّ
 * «Play Now» عند (٢٣٠،١٥٠)، والنقاط عند (٨٠٣،٤٦٣). بلا ألعاب: بطاقة فراغ تقود إلى المكتبة، لا «٠٪» ولا زرّ
 * معطّل. السحب الأفقيّ فوقه يبدّل اللعبة (الطبقة تحت الأزرار فلا تسرق نقراتها).
 */
@Composable
internal fun StageHero(data: StageData, actions: StageActions) {
    val intro = LocalStageIntro.current
    val extra = LocalStageExtra.current
    val drop = LocalStageDrop.current
    val boost = stageTouchBoost()
    // العرض الإضافيّ بين نصّ اللعبة ولوحات اليمين: البطل والنقاط يتوسّطانه.
    val room = extra - stageRightGrow(extra)
    val shift = room / 2
    // أسفل منطقة البطل (= أعلى الرفّ): ٥٠٩ في اللقطة المرجعيّة، ويرتفع في التخطيط المضغوط فتقترب العناصر معه.
    val z = StageGeo.shelfTop(drop)
    val zone = z - 509
    // الأيقونة الكبيرة تصغر قليلًا حين تضيق المنطقة (٠٫٨ كحدّ أدنى) وتكبر قليلًا حين تتّسع.
    val artK = if (drop == 0) 1f else ((z - 86) / 423f).coerceIn(0.8f, 1.08f)
    val titleTop = maxOf(96, z - 359)
    if (data.games.size > 1) {
        StageSwipeLayer(
            onStep = { delta ->
                data.games.getOrNull(data.selectedIndex + delta)?.let { actions.onSelect(it.packageName) }
            },
            modifier = Modifier.at(168, 40, 1040 + room, z - 49)
        )
    }
    Box(
        modifier = Modifier
            .at(570 + shift, zone / 2 - 5, 560, 520)
            .lobbyIntro(intro, scaleFrom = 0.88f, start = 0.05f),
        contentAlignment = Alignment.Center
    ) {
        HeroArt(game = data.selected, edge = ArtEdge * artK)
    }
    StageDots(
        count = data.games.size,
        index = if (data.selected == null) -1 else data.selectedIndex,
        modifier = Modifier
            .at(803 + shift, z - 46, 140, 12)
            .lobbyIntro(intro, start = 0.30f)
    )
    if (data.games.isEmpty()) {
        EmptyHero(
            onOpenLibrary = actions.onOpenLibrary,
            modifier = Modifier
                .at(230, titleTop, 340, minOf(300, z - 59 - titleTop))
                .lobbyIntro(intro, fromStart = 70.dp, start = 0.10f)
        )
        return
    }
    Box(
        modifier = Modifier
            .at(230, titleTop, 320, z - 189 - titleTop)
            .lobbyIntro(intro, fromStart = 70.dp, start = 0.10f),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = data.selected?.label.orEmpty(),
            animationSpec = tween(durationMillis = 260),
            label = "stageHeroTitle"
        ) { label ->
            // الأسماء الطويلة تصغر خطوة أو خطوتين بدل أن تُقتطع بنقاط وهي في سطرين.
            val size = when {
                label.length > 26 -> 32.sp
                label.length > 16 -> 38.sp
                else -> 46.sp
            }
            Text(
                text = label,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    brush = TitleFill,
                    fontSize = size,
                    lineHeight = size * 1.08f,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = GameSpacePalette.VioletSoft.copy(alpha = 0.60f),
                        offset = Offset.Zero,
                        blurRadius = 30f
                    )
                )
            )
        }
    }
    data.tagline?.let { line ->
        Box(
            modifier = Modifier
                .at(230, z - 187, 320, 36)
                .lobbyIntro(intro, fromStart = 70.dp, start = 0.16f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = line,
                color = GameSpacePalette.Lavender.copy(alpha = 0.95f),
                fontSize = 22.sp,
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
        launching = data.launching,
        boost = boost,
        modifier = Modifier
            .at(230, z - 118, if (boost > 1f) 430 else 340, (58 * boost).toInt())
            .lobbyIntro(intro, fromStart = 70.dp, start = 0.22f)
    )
}

/**
 * طبقة سحب شفّافة فوق البطل: سحبة أفقيّة تتجاوز عُشر العرض تنقل اللعبة المختارة خطوة (يسارًا = التالية).
 * تُقرأ دالّة الخطوة بـ`rememberUpdatedState` فلا يُعاد تسجيل المستمع أثناء السحب كلما أُعيد التركيب
 * (القراءات الحيّة تُجدَّد كل ثانيتين). والسحب الرأسيّ لا يُستهلك هنا فيصل إلى المُصفِّح.
 */
@Composable
private fun StageSwipeLayer(onStep: (Int) -> Unit, modifier: Modifier) {
    val step by rememberUpdatedState(onStep)
    Box(
        modifier.pointerInput(Unit) {
            var dragged = 0f
            detectHorizontalDragGestures(
                onDragStart = { dragged = 0f },
                onDragEnd = {
                    val threshold = size.width * SwipeShare
                    if (dragged <= -threshold) {
                        step(1)
                    } else if (dragged >= threshold) {
                        step(-1)
                    }
                    dragged = 0f
                },
                onDragCancel = { dragged = 0f },
                onHorizontalDrag = { change, amount ->
                    change.consume()
                    dragged += amount
                }
            )
        }
    )
}

/**
 * خلفية البطل بعرض اللوحة كما في اللقطة المرجعيّة: فنّ اللعبة يملأ المنطقة خلف الاسم والبطل ويذوب في
 * العتمة من اليسار ومن الأسفل لتبقى النصوص والرفّ مقروءة. لا فنّ منقول (ADR-07): هي أيقونة اللعبة
 * الحقيقيّة مكبَّرة ومموَّهة، فوقها توهّج بلون اللعبة نفسه (من أيقونتها) ينتقل بنعومة مع تبديلها.
 */
@Composable
internal fun StageHeroBackdrop(game: GameApp?, modifier: Modifier = Modifier) {
    val icon by rememberLobbyIcon(game?.packageName)
    val tone by animateColorAsState(
        targetValue = rememberLobbyTone(game?.packageName),
        animationSpec = tween(durationMillis = 520),
        label = "stageHeroTone"
    )
    Box(modifier.clipToBounds()) {
        Crossfade(
            targetState = icon,
            animationSpec = tween(durationMillis = 520),
            label = "stageHeroBackdrop"
        ) { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.blur(GlowL)
                            } else {
                                Modifier
                            }
                        )
                        .alpha(0.62f)
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(
                                tone.copy(alpha = 0.34f),
                                tone.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.46f, size.height * 0.46f),
                            radius = size.height * 1.05f
                        )
                    )
                    drawRect(
                        Brush.horizontalGradient(
                            0f to GameSpacePalette.Void.copy(alpha = 0.94f),
                            0.34f to GameSpacePalette.Void.copy(alpha = 0.62f),
                            0.70f to Color.Transparent,
                            1f to GameSpacePalette.Void.copy(alpha = 0.55f)
                        )
                    )
                    drawRect(
                        Brush.verticalGradient(
                            0f to GameSpacePalette.Void.copy(alpha = 0.55f),
                            0.30f to Color.Transparent,
                            0.62f to Color.Transparent,
                            1f to GameSpacePalette.Deep
                        )
                    )
                }
        )
    }
}

/**
 * «Play Now» بحدّ بنفسجيّ متدرّج على خلفية داكنة (٢٤٨×٥٧) تعبرها لمعة، و«…» دائرة (٥٨) بجواره. كلاهما
 * ينكمش بزنبرك عند اللمس. أثناء [launching] تتحوّل أيقونة التشغيل إلى مؤشّر تقدّم ويُمنع النقر الثاني.
 */
@Composable
internal fun PlayRow(
    enabled: Boolean,
    onLaunch: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier,
    launching: Boolean = false,
    /** رفع مقاس الزرّين ليبقى لمسهما مريحًا حين تصغر اللوحة (١ = المقاس المرجعيّ). */
    boost: Float = 1f,
) {
    val clock = LocalStageClock.current
    val playInteraction = remember { MutableInteractionSource() }
    val moreInteraction = remember { MutableInteractionSource() }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val playH = PlayHeight * boost
        val shape = RoundedCornerShape(playH)
        Row(
            modifier = Modifier
                .width(PlayWidth * boost)
                .height(playH)
                .lobbyPress(playInteraction, 0.96f)
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
                .stageSheen(clock, cycles = 6, strength = 0.20f)
                .clickable(
                    interactionSource = playInteraction,
                    indication = null,
                    enabled = enabled && !launching,
                    role = Role.Button,
                    onClick = onLaunch
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GapS, Alignment.CenterHorizontally)
        ) {
            if (launching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(PlayGlyph * boost),
                    color = Color.White,
                    strokeWidth = ProgressStroke
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(PlayGlyph * boost)
                )
            }
            Text(
                text = stringResource(R.string.gs_launch),
                color = Color.White,
                fontSize = 22.sp,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(30.dp))
        Box(
            modifier = Modifier
                .size(MoreSize * boost)
                .lobbyPress(moreInteraction, 0.90f)
                .clip(CircleShape)
                .background(GameSpacePalette.Plum.copy(alpha = 0.90f))
                .border(StrokeFine, GameSpacePalette.VioletSoft.copy(alpha = 0.45f), CircleShape)
                .clickable(
                    interactionSource = moreInteraction,
                    indication = null,
                    role = Role.Button,
                    onClick = onOptions
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = stringResource(R.string.gs_more),
                tint = GameSpacePalette.Ink,
                modifier = Modifier.size(PlayGlyph * boost)
            )
        }
    }
}

/**
 * لوح اللعبة: أيقونة التطبيق الحقيقيّة بحدّ متدرّج ولمعة زجاجيّة، تطفو وتميل ميلًا طفيفًا فوق هالة بلون
 * اللعبة (من أيقونتها)، وحلقتين رفيعتين يدور عليهما ثلاثة أضواء، وبقعة ضوء على «أرضيّة» تحتها. الحركة كلّها
 * دالّة في ساعة المسرح الواحدة وتُقرأ في مرحلة الرسم/الطبقة.
 */
@Composable
internal fun HeroArt(game: GameApp?, edge: Dp) {
    val clock = LocalStageClock.current
    val tone by animateColorAsState(
        targetValue = rememberLobbyTone(game?.packageName),
        animationSpec = tween(durationMillis = 520),
        label = "heroArtTone"
    )
    Box(
        modifier = Modifier
            .size(edge * 1.84f)
            .drawBehind {
                val reach = size.minDimension / 2f
                val t = clock.value
                val e = edge.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tone.copy(alpha = 0.50f),
                            GameSpacePalette.Violet.copy(alpha = 0.22f),
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
                // أرضيّة ضوئيّة: دائرة مضغوطة رأسيًّا فتصير قطعًا ناقصًا بحافة ناعمة من كل جهة.
                val floorY = center.y + e * 0.60f
                scale(scaleX = 1f, scaleY = 0.2f, pivot = Offset(center.x, floorY)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(tone.copy(alpha = 0.55f), Color.Transparent),
                            center = Offset(center.x, floorY),
                            radius = e * 0.70f
                        ),
                        radius = e * 0.70f,
                        center = Offset(center.x, floorY)
                    )
                }
                // ثلاثة أضواء تدور على الحلقة الداخليّة (٣ دورات في دورة الساعة).
                val ring = reach * 0.82f
                for (k in 0 until 3) {
                    val a = TwoPi * (t * 3f + k / 3f)
                    val p = Offset(center.x + ring * cos(a), center.y + ring * sin(a))
                    drawCircle(
                        color = GameSpacePalette.Lavender.copy(alpha = 0.20f),
                        radius = OrbitGlow.toPx(),
                        center = p
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = OrbitDot.toPx(),
                        center = p
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                val t = clock.value
                translationY = (stageWave(t, 3, 0.25f) - 0.5f) * 2f * FloatDistance.toPx()
                rotationZ = (stageWave(t, 2, 0.60f) - 0.5f) * 2.4f
            },
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = game?.packageName,
                animationSpec = tween(durationMillis = 320),
                label = "stageHeroIcon"
            ) { packageName ->
                HeroIcon(packageName = packageName, label = game?.label.orEmpty(), edge = edge)
            }
        }
    }
}

@Composable
private fun HeroIcon(packageName: String?, label: String, edge: Dp) {
    val clock = LocalStageClock.current
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
            .border(StrokeSelected, SelectedRim, shape)
            .stageSheen(clock, cycles = 4, strength = 0.16f),
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
internal fun EmptyHero(onOpenLibrary: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(RadiusCard)
    val interaction = remember { MutableInteractionSource() }
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
            fontFamily = DisplayFontFamily,
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
                .lobbyPress(interaction, 0.96f)
                .clip(shape)
                .background(PlayFill)
                .border(StrokeFirm, PlayRim, shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onOpenLibrary
                )
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

/** نقاط التنقّل: النقطة الحالية قرص ممتدّ بنفسجيّ (٢٨×١١) يتمدّد بنعومة، والبقيّة دوائر (١١) باهتة. */
@Composable
internal fun StageDots(count: Int, index: Int, modifier: Modifier = Modifier) {
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
            key(i) {
                val active = i == index
                val width by animateDpAsState(
                    targetValue = if (active) DotActiveWidth else DotSize,
                    animationSpec = tween(durationMillis = 260),
                    label = "stageDot"
                )
                Box(
                    modifier = Modifier
                        .height(DotSize)
                        .width(width)
                        .clip(CircleShape)
                        .background(if (active) GameSpacePalette.VioletSoft else Color.White.copy(alpha = 0.32f))
                )
            }
        }
    }
}

/**
 * رفّ الألعاب (١٩٥،٥٠٩، ١٤٥١×٣٠٦): لوح زجاجيّ بعنوان ذي شقّ بنفسجيّ و«See All»، صفّ بطاقات
 * (١٨٤×٢١٤ عند y=٥٧٣) ينتهي ببطاقة «More»، وسهمان دائريّان على حافتي اللوح (مركزاهما ١٩٩ و١٦٤٥، y=٦٨١).
 * الاختيار بالأسهم أو النقاط أو السحب يُوسِّط بطاقته في الرفّ بتمرير ناعم بدل تركها عند الحافة.
 */
@Composable
internal fun StageShelf(data: StageData, actions: StageActions) {
    val intro = LocalStageIntro.current
    val extra = LocalStageExtra.current
    val drop = LocalStageDrop.current
    val boost = stageTouchBoost()
    // الرفّ: ٥٠٩ و١٤٥١ عرضًا في اللقطة؛ في التخطيط المضغوط يلتصق بأسفل الشاشة وينتهي قبل عمود اللوحات اليمنى.
    val top = StageGeo.shelfTop(drop)
    val shelfW = StageGeo.shelfWidth(drop, extra)
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    LaunchedEffect(data.selectedIndex) {
        if (data.selectedIndex in data.games.indices) {
            val viewport = listState.layoutInfo.viewportSize.width
            val card = with(density) { CardWidth.roundToPx() }
            listState.animateScrollToItem(
                index = data.selectedIndex,
                scrollOffset = -((viewport - card) / 2).coerceAtLeast(0)
            )
        }
    }
    Box(
        Modifier
            .at(195, top, shelfW, StageGeo.ShelfH)
            .lobbyIntro(intro, fromBelow = 60.dp, start = 0.25f)
            .stageGlass(RoundedCornerShape(RadiusPanel))
    )
    Row(
        modifier = Modifier
            .at(217, top + 11, shelfW - 46, 36)
            .lobbyIntro(intro, fromBelow = 60.dp, start = 0.28f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GapM)
    ) {
        Box(
            Modifier
                .width(NotchWidth)
                .height(NotchHeight)
                .clip(CircleShape)
                .background(NotchFill)
        )
        Text(
            text = stringResource(R.string.gs_shelf),
            color = GameSpacePalette.Ink,
            fontSize = 24.sp,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        val seeAllInteraction = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .lobbyPress(seeAllInteraction, 0.94f)
                .clip(CircleShape)
                .border(StrokeFine, Color.White.copy(alpha = 0.16f), CircleShape)
                .clickable(
                    interactionSource = seeAllInteraction,
                    indication = null,
                    role = Role.Button,
                    onClick = actions.onOpenLibrary
                )
                .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GapS)
        ) {
            Text(
                text = stringResource(R.string.gs_view_all),
                color = GameSpacePalette.Ink.copy(alpha = 0.90f),
                fontSize = 18.sp,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = GameSpacePalette.VioletSoft,
                modifier = Modifier.size(SeeAllGlyph)
            )
        }
    }
    LazyRow(
        modifier = Modifier
            .at(220, top + 64, shelfW - 51, 214)
            .lobbyIntro(intro, fromBelow = 60.dp, start = 0.32f),
        state = listState,
        contentPadding = PaddingValues(horizontal = ShelfInset),
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
        modifier = Modifier
            .at(199 - (50 * boost).toInt() / 2, top + 173 - (50 * boost).toInt() / 2, (50 * boost).toInt(), (50 * boost).toInt())
            .lobbyIntro(intro, start = 0.40f)
    )
    ShelfArrow(
        icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        description = stringResource(R.string.gs_next),
        enabled = data.selectedIndex < data.games.lastIndex,
        onClick = { data.games.getOrNull(data.selectedIndex + 1)?.let { actions.onSelect(it.packageName) } },
        modifier = Modifier
            .at(195 + shelfW - (50 * boost).toInt() / 2, top + 173 - (50 * boost).toInt() / 2, (50 * boost).toInt(), (50 * boost).toInt())
            .lobbyIntro(intro, start = 0.40f)
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
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .lobbyPress(interaction, 0.88f)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(GameSpacePalette.Glass.copy(alpha = 0.94f))
            .border(StrokeFine, Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
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
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .size(width = MoreWidth, height = CardHeight)
            .lobbyPress(interaction, 0.95f)
            .stageGlass(shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Apps,
            contentDescription = null,
            tint = GameSpacePalette.Lavender.copy(alpha = 0.75f),
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
 * بطاقة لعبة (١٨٤×٢١٤): خلفية مموّهة من أيقونتها والأيقونة نفسها واضحة فوقها، ولمعة زجاجيّة قطريّة، واسم
 * اللعبة على حجاب سفليّ. المختارة وحدها ترتفع وتكبر قليلًا وتحمل حدًّا متدرّجًا (٣) وتوهّجًا؛ والضغط
 * ينكمش بزنبرك.
 */
@Composable
private fun ShelfCard(game: GameApp, selected: Boolean, onClick: () -> Unit) {
    val icon by rememberLobbyIcon(game.packageName)
    val shape = RoundedCornerShape(RadiusCard)
    val interaction = remember { MutableInteractionSource() }
    val lift by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "shelfCardLift"
    )
    Box(
        modifier = Modifier
            .size(width = CardWidth, height = CardHeight)
            .lobbyPress(interaction, 0.95f)
            .graphicsLayer {
                val k = 1f + 0.05f * lift
                scaleX = k
                scaleY = k
                translationY = -LiftDistance.toPx() * lift
            }
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
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
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
                        .alpha(0.58f)
                        .blur(GapM + GapS)
                )
            }
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 22.dp)
                    .size(CardIcon)
                    .shadow(
                        elevation = GlowS,
                        shape = RoundedCornerShape(CardIcon * 0.22f),
                        ambientColor = Color.Black.copy(alpha = 0.50f),
                        spotColor = Color.Black.copy(alpha = 0.60f)
                    )
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
            Modifier
                .fillMaxSize()
                .background(CardSheen)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, GameSpacePalette.Void.copy(alpha = 0.92f))
                    )
                )
                .padding(horizontal = PadName, vertical = PadNameV)
        ) {
            Text(
                text = game.label,
                color = if (selected) GameSpacePalette.Ink else GameSpacePalette.Lavender,
                fontSize = 17.sp,
                fontFamily = DisplayFontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
