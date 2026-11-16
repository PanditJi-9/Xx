/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import nd.max.R
import nd.max.core.gamespace.GameApp
import nd.max.ui.component.lobbyIntro
import nd.max.ui.component.lobbyPress
import nd.max.ui.component.rememberLobbyArt
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
private val CardIcon = 128.dp
private val MoreWidth = 158.dp
private val MoreGlyph = 52.dp
private val ArrowSize = 38.dp
private val ArrowGlyph = 28.dp
private val NotchWidth = 4.dp
private val NotchHeight = 24.dp
private val DotSize = 11.dp
private val DotActiveWidth = 28.dp
private val DotGap = 15.dp
private val GlowS = 14.dp
private val GlowL = 40.dp
private val SeeAllGlyph = 24.dp
private val ShelfInset = 8.dp
private val LiftDistance = 7.dp
private val FloatDistance = 9.dp
private val OrbitGlow = 9.dp
private val OrbitDot = 2.6.dp
private const val HeroStackGap = 22
private const val HeroDotsGap = 16
private const val HeroDotsH = 12
private val ChipHeight = 38.dp
private val ChipGlyph = 22.dp
private val ChipPadStart = 14.dp
private val ChipPadEnd = 18.dp
private val ChipGap = 8.dp

private const val ShelfMoreKey = "stage.shelf.more"
private const val SwipeShare = 0.10f

/** أصغر مقاس للوح الأيقونة (وحدات اللقطة) تُعرض معه تحيّة الاستقبال؛ أدناه لا يتّسع العمود لسطر إضافيّ. */
private const val GreetingMinTile = 150

/** وهج زرّ التشغيل، وشفافيّة البطاقة غير المختارة في الرفّ (المختارة وحدها كاملة فيتّضح التركيز). */
private val GlowPlay = 24.dp
private const val IdleAlpha = 0.80f

/** أرضيّة ضوئيّة تحت لوح الأيقونة (بعدها عن حافته السفلى بوحدات اللقطة، وانضغاطها الرأسيّ، ومداها بنسبة عرضه). */
private val FloorGap = 6.dp
private const val FloorSquash = 0.18f
private const val FloorReach = 0.62f

/** ميل لوح الأيقونة حول المحور الرأسيّ (درجات) وبُعد الكاميرا (× الكثافة): منظور خفيف يوحي بعمق لا بتدوير. */
private const val TiltDegrees = 6f
private const val TiltCamera = 14f

/** وهج اسم اللعبة: لون اللعبة نفسه خلف الحروف (شفافيّته، وإزاحته رأسيًّا، وتمويهه بالبكسل). */
private const val TitleGlow = 0.55f
private const val TitleGlowDrop = 6f
private const val TitleGlowBlur = 26f

/**
 * فنّ البطل (من أيقونة اللعبة بدقّة ٥١٢): ضلعه نسبةً إلى ارتفاع الخلفية، وشفافيّته، وانزياحه وتنفّسه البطيئان،
 * ومقدار ما يمتدّ تحت عمود اللوحات (نسبة من ضلعه)، ومركزه الرأسيّ. الهاتف العموديّ يتوسّط الفنّ بأصغر ضلع وأخفّ شفافيّة.
 */
private const val ArtSide = 1.22f
private const val ArtSidePortrait = 1.05f
private const val ArtAlpha = 0.94f
private const val ArtAlphaPortrait = 0.50f
private const val ArtDrift = 14f
private const val ArtBreath = 0.03f
private const val ArtUnderBoard = 0.10f
private const val ArtCenterY = 0.46f

/** أشعّة الضوء المائلة فوق الأورورا: ميلها، ومراكزها وعروضها (نسبة من العرض)، وشدّة كلٍّ منها. */
private const val BeamTilt = 22f
private val BeamCenters = floatArrayOf(0.50f, 0.66f, 0.84f)
private val BeamWidths = floatArrayOf(0.05f, 0.10f, 0.06f)
private val BeamAlphas = floatArrayOf(0.09f, 0.14f, 0.08f)

/** إزاحة صبغة الأورورا الثانية (درجات على عجلة الألوان) عن لون اللعبة. */
private const val HueShift = 42f

/** انبثاق الاختيار عند تبديل اللعبة: مدّته، وشفافيّة حلقته، ومداها (× مقاس اللوح)، وسمكها، ومقاس اللوح قبل أن «ينبثق». */
private const val BurstMs = 900
private const val BurstAlpha = 0.60f
private const val BurstReach = 1.0f
private val BurstStroke = 5.dp
private const val PopFrom = 0.90f

/** سمك إطار الطاقة الرفيع على زرّ التشغيل، ومدى انجراف الأورورا البطيء (وحدات اللقطة). */
private val RimThin = 2.dp
private const val AuroraDrift = 44f

private val PlayFill = Brush.horizontalGradient(
    listOf(GameSpacePalette.Violet.copy(alpha = 0.72f), GameSpacePalette.VioletDeep.copy(alpha = 0.58f))
)

/** لمعة علويّة على الزرّ: تُبرزه كزجاج مضيء فوق الخلفية الداكنة. */
private val PlayGloss = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.20f),
    0.55f to Color.Transparent
)
private val PlayRim = Brush.horizontalGradient(
    listOf(LuxGold, LuxGoldDeep.copy(alpha = 0.70f), LuxGold)
)
private val SelectedRim = Brush.linearGradient(
    listOf(LuxGold, GameSpacePalette.Lavender, LuxGoldDeep)
)
private val CardFill = Brush.verticalGradient(listOf(Color(0xFF2B1660), Color(0xFF0C0626)))
private val IdleRim = Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.06f))
)
private val TitleFill = Brush.verticalGradient(listOf(Color.White, LuxGold))
private val NotchFill = Brush.verticalGradient(listOf(LuxGold, LuxGoldDeep))
private val CardSheen = Brush.linearGradient(
    colors = listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
    start = Offset.Zero,
    end = Offset(260f, 300f)
)

/**
 * هويّة اللعبة في وسط الشاشة: بلاطة الأيقونة الواضحة بتوهّج بلون اللعبة، فاسمها الكبير، فشارتان حقيقيّتان
 * («آخر لعب …» و«لُعبت …» من السجلّ المحلّيّ — تسقطان إن لم يوجد سجلّ)، ثم «Play Now» ونقاط التنقّل. كلّها
 * في كتلة واحدة تتوسّط المنطقة بين الشريط العلويّ والرفّ، فلا شيء يعلو أزرار اللوبي ولا حلقات تدور حول فراغ.
 * الخلفيّة السينمائيّة ([StageHeroBackdrop]) هي التي تعطي الوسط حضوره. السحب الأفقيّ فوق المنطقة يبدّل اللعبة
 * (الطبقة تحت الأزرار فلا تسرق نقراتها). بلا ألعاب: بطاقة فراغ تقود إلى المكتبة، لا «٠٪» ولا زرّ معطّل.
 */
@Composable
internal fun StageHero(data: StageData, actions: StageActions) {
    val intro = LocalStageIntro.current
    val extra = LocalStageExtra.current
    val drop = LocalStageDrop.current
    val boost = stageTouchBoost()
    val room = extra - stageRightGrow(extra)
    // أسفل منطقة البطل (= أعلى الرفّ): ٥٠٩ في اللقطة المرجعيّة، ويرتفع في التخطيط المضغوط.
    val z = StageGeo.shelfTop(drop)
    if (data.games.size > 1) {
        StageSwipeLayer(
            onStep = { delta ->
                data.games.getOrNull(data.selectedIndex + delta)?.let { actions.onSelect(it.packageName) }
            },
            modifier = Modifier.at(168, 40, 1040 + room, z - 49)
        )
    }
    if (data.games.isEmpty()) {
        val emptyTop = maxOf(96, z - 359)
        EmptyHero(
            onOpenLibrary = actions.onOpenLibrary,
            modifier = Modifier
                .at(230, emptyTop, 340, minOf(300, z - 59 - emptyTop))
                .lobbyIntro(intro, fromStart = 70.dp, start = 0.10f)
        )
        return
    }
    // المنطقة: من تحت الشريط العلويّ (١٠٠) إلى فوق الرفّ بـ٢٤، ويمينها قبل عمود اللوحات بـ٤٠.
    val left = 230
    val right = stageBoardX(extra) - 40
    val topY = 100
    val zoneH = (z - 24) - topY
    val ctaH = (58 * boost).toInt()
    // البلاطة تتّسع مع ارتفاع المنطقة (١٢٠–١٩٠) والكتلة كلّها تتوسّطها رأسيًّا.
    val tile = (zoneH - HeroStackGap - ctaH - HeroDotsGap - HeroDotsH).coerceIn(120, 190)
    val blockH = tile + HeroStackGap + ctaH + HeroDotsGap + HeroDotsH
    val rowY = topY + ((zoneH - blockH) / 2).coerceAtLeast(0)
    val ctaY = rowY + tile + HeroStackGap
    val dotsY = ctaY + ctaH + HeroDotsGap
    val textX = left + tile + 28
    val textW = right - textX
    val played = lobbyPlayTimeText(data.stats?.totalMs ?: 0L)
    HeroTile(
        game = data.selected,
        edge = tile.dp,
        modifier = Modifier
            .at(left, rowY, tile, tile)
            .lobbyIntro(intro, scaleFrom = 0.88f, start = 0.05f)
    )
    Column(
        modifier = Modifier
            .at(textX, rowY, textW, tile)
            .lobbyIntro(intro, fromStart = 70.dp, start = 0.10f),
        verticalArrangement = Arrangement.Center
    ) {
        if (tile >= GreetingMinTile) HeroGreeting()
        HeroTitle(label = data.selected?.label.orEmpty(), packageName = data.selected?.packageName)
        val lastPlayed = data.tagline
        if (lastPlayed != null || played != null) {
            Spacer(Modifier.height(GapM))
            Row(horizontalArrangement = Arrangement.spacedBy(GapS)) {
                lastPlayed?.let { HeroChip(icon = Icons.Rounded.History, text = it) }
                played?.let { HeroChip(icon = Icons.Rounded.Timer, text = it) }
            }
        }
    }
    PlayRow(
        enabled = data.selected != null,
        onLaunch = actions.onLaunch,
        onOptions = actions.onOptions,
        launching = data.launching,
        boost = boost,
        modifier = Modifier
            .at(left, ctaY, if (boost > 1f) 430 else 340, ctaH)
            .lobbyIntro(intro, fromStart = 70.dp, start = 0.22f)
    )
    StageDots(
        count = data.games.size,
        index = if (data.selected == null) -1 else data.selectedIndex,
        modifier = Modifier
            .at(left + 6, dotsY, 140, HeroDotsH)
            .lobbyIntro(intro, start = 0.30f)
    )
}

/**
 * بلاطة الأيقونة: الأيقونة الحقيقيّة بحدّها المتدرّج ولمعتها الزجاجيّة، تطفو طفوًا خفيفًا فوق توهّج بلون اللعبة
 * (من أيقونتها) ينتقل بنعومة مع تبديلها. لا حلقات ولا أضواء تدور: الأيقونة هي الحدث.
 */
@Composable
private fun HeroTile(game: GameApp?, edge: Dp, modifier: Modifier) {
    val clock = LocalStageClock.current
    val tone by animateColorAsState(
        targetValue = rememberLobbyTone(game?.packageName),
        animationSpec = tween(durationMillis = 520),
        label = "heroTileTone"
    )
    // انبثاق عند كل تبديل (وعند أول ظهور): حلقة ضوء تتّسع وتنطفئ حول اللوح، واللوح ينبثق من مقاس أصغر.
    val burst = remember { Animatable(1f) }
    LaunchedEffect(game?.packageName) {
        burst.snapTo(0f)
        burst.animateTo(1f, tween(durationMillis = BurstMs, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = modifier
            .drawBehind {
                val reach = size.minDimension * 1.05f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tone.copy(alpha = 0.55f), Color.Transparent),
                        center = center,
                        radius = reach
                    ),
                    radius = reach,
                    center = center
                )
                // أرضيّة ضوئيّة تحت اللوح: قرص مضغوط رأسيًّا يوحي بأن اللوح يطفو فوق سطح لا أنه ملصق على الخلفية.
                val floor = Offset(center.x, size.height + FloorGap.toPx())
                scale(scaleX = 1f, scaleY = FloorSquash, pivot = floor) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(tone.copy(alpha = 0.50f), Color.Transparent),
                            center = floor,
                            radius = size.width * FloorReach
                        ),
                        radius = size.width * FloorReach,
                        center = floor
                    )
                }
                val b = burst.value
                if (b < 1f) {
                    drawCircle(
                        color = tone.copy(alpha = BurstAlpha * (1f - b)),
                        radius = size.minDimension * (0.55f + BurstReach * b),
                        center = center,
                        style = Stroke(width = BurstStroke.toPx() * (1f - b))
                    )
                }
            }
            .graphicsLayer {
                val t = clock.value
                translationY = (stageWave(t, 3, 0.25f) - 0.5f) * 2f * FloatDistance.toPx()
                rotationY = (stageWave(t, 2, 0.10f) - 0.5f) * 2f * TiltDegrees
                cameraDistance = TiltCamera * density
                val pop = PopFrom + (1f - PopFrom) * burst.value
                scaleX = pop
                scaleY = pop
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

/**
 * تحيّة الاستقبال: سطر ذهبيّ صغير فوق الاسم يتبدّل بساعة اليوم («مساء الخير · جناحك جاهز») فيضيف دفء مضيف الفندق.
 * الساعة تُقرأ مرّة عند التركيب: الشاشة تُفتح وتُغلق، فلا حاجة لمؤقّت يُحدّثها كل دقيقة.
 */
@Composable
private fun HeroGreeting() {
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = stringResource(
        when (hour) {
            in 5..11 -> R.string.gs_greet_morning
            in 12..16 -> R.string.gs_greet_afternoon
            in 17..21 -> R.string.gs_greet_evening
            else -> R.string.gs_greet_night
        }
    )
    Text(
        text = stringResource(R.string.gs_greet_format, greeting, stringResource(R.string.gs_greet_suite)),
        color = LuxGold,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * اسم اللعبة بخطّ العرض الأسود وتدرّج أبيض→لافندر، وخلفه وهج بلون اللعبة (من أيقونتها) ينتقل بنعومة مع تبديلها.
 * منفصل عن [StageHero] كي لا يُعيد تركيب كتلة البطل كلها مع كل إطار من انتقال اللون.
 */
@Composable
private fun HeroTitle(label: String, packageName: String?) {
    val clock = LocalStageClock.current
    val tone by animateColorAsState(
        targetValue = rememberLobbyTone(packageName),
        animationSpec = tween(durationMillis = 520),
        label = "heroTitleTone"
    )
    Crossfade(
        targetState = label,
        animationSpec = tween(durationMillis = 260),
        label = "stageHeroTitle"
    ) { text ->
        // الأسماء الطويلة تصغر خطوة أو خطوتين بدل أن تُقتطع، وتأخذ سطرين كحدّ أقصى.
        val size = when {
            text.length > 26 -> 40.sp
            text.length > 16 -> 48.sp
            else -> 58.sp
        }
        Text(
            text = text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.stageTextShine(clock),
            style = TextStyle(
                brush = TitleFill,
                fontSize = size,
                lineHeight = size * 1.05f,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Black,
                shadow = Shadow(
                    color = tone.copy(alpha = TitleGlow),
                    offset = Offset(0f, TitleGlowDrop),
                    blurRadius = TitleGlowBlur
                )
            )
        )
    }
}

/** شارة معلومة صغيرة تحت اسم اللعبة: أيقونة ونصّ على زجاج خفيف بحدّ رفيع. */
@Composable
private fun HeroChip(icon: ImageVector, text: String) {
    val shape = RoundedCornerShape(ChipHeight)
    Row(
        modifier = Modifier
            .height(ChipHeight)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(StrokeFine, Color.White.copy(alpha = 0.16f), shape)
            .padding(start = ChipPadStart, end = ChipPadEnd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChipGap)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LuxGold,
            modifier = Modifier.size(ChipGlyph)
        )
        Text(
            text = text,
            color = GameSpacePalette.Ink.copy(alpha = 0.92f),
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
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
 * خلفية البطل السينمائيّة — ثلاث طبقات كلّها من أيقونة اللعبة الحقيقيّة (لا فنّ منقول، ADR-07):
 *  ١. **لون**: الأيقونة مكبَّرة ومموَّهة تملأ العرض وتتنفّس ببطء، فوقها أورورا بلونين متناغمين (لون اللعبة ولونٌ
 *     مجاور له على عجلة الألوان) وثلاثة أشعّة ضوء مائلة تنبض؛
 *  ٢. **فنّ**: الأيقونة نفسها بدقّة ٥١٢ كبيرةً خلف عمود اللوحات، تذوب أطرافها (وزواياها الشفّافة) بقناع شعاعيّ
 *     وتتلاشى من جهة النصّ فلا تزاحم الاسم؛ وفي هذا الفنّ يمرّ الزجاج الخلفيّ للوحات فيأخذ عمقًا؛
 *  ٣. **قراءة**: حجاب معتم على الجانبين والأعلى والأسفل يحفظ وضوح النصّ ويُذيب الحافتين في الرفّ وشريط الحالة.
 * ينتقل اللون والفنّ بنعومة مع تبديل اللعبة. [boardEdge]: حافة عمود اللوحات اليمنى بإحداثيات هذه الطبقة
 * (وحدات اللقطة)؛ بلا قيمة (الهاتف العموديّ) يتوسّط الفنّ.
 */
@Composable
internal fun StageHeroBackdrop(game: GameApp?, modifier: Modifier = Modifier, boardEdge: Int? = null) {
    val icon by rememberLobbyIcon(game?.packageName)
    val art by rememberLobbyArt(game?.packageName)
    val tone by animateColorAsState(
        targetValue = rememberLobbyTone(game?.packageName),
        animationSpec = tween(durationMillis = 520),
        label = "stageHeroTone"
    )
    val clock = LocalStageClock.current
    val accent = shiftHue(tone, HueShift)
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
                        .graphicsLayer {
                            val t = clock.value
                            val k = 1.34f + 0.06f * stageWave(t, 1)
                            scaleX = k
                            scaleY = k
                            translationX = (stageWave(t, 1, 0.25f) - 0.5f) * 70f
                        }
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.blur(GlowL)
                            } else {
                                Modifier
                            }
                        )
                        .alpha(0.55f)
                )
            }
        }
        // أورورا: تُرسم مرّة واحدة ثم تتحرّك الطبقة نفسها ببطء (إزاحة وتكبير) فلا إعادة رسم كل إطار.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val t = clock.value
                    translationX = (stageWave(t, 1, 0.10f) - 0.5f) * AuroraDrift * 1.dp.toPx()
                    translationY = (stageWave(t, 2, 0.35f) - 0.5f) * AuroraDrift * 0.5f * 1.dp.toPx()
                    val k = 1.04f + 0.04f * stageWave(t, 1, 0.55f)
                    scaleX = k
                    scaleY = k
                }
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(
                                tone.copy(alpha = 0.50f),
                                tone.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.66f, size.height * 0.38f),
                            radius = size.height * 1.25f
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.32f), Color.Transparent),
                            center = Offset(size.width * 0.96f, size.height * 0.92f),
                            radius = size.height * 0.95f
                        )
                    )
                }
        )
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val pulse = 0.55f + 0.45f * stageWave(clock.value, 2, 0.20f)
                    rotate(degrees = BeamTilt, pivot = Offset(size.width * 0.72f, 0f)) {
                        for (i in BeamCenters.indices) {
                            val w = size.width * BeamWidths[i]
                            val x = size.width * BeamCenters[i] - w / 2f
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        tone.copy(alpha = BeamAlphas[i] * pulse),
                                        Color.Transparent
                                    ),
                                    startX = x,
                                    endX = x + w
                                ),
                                topLeft = Offset(x, -size.height * 0.30f),
                                size = Size(w, size.height * 1.90f)
                            )
                        }
                    }
                }
        )
        Crossfade(
            targetState = art,
            animationSpec = tween(durationMillis = 520),
            label = "stageHeroArt"
        ) { bitmap ->
            if (bitmap != null) {
                HeroKeyArt(bitmap = bitmap, clock = clock, boardEdge = boardEdge)
            }
        }
        StageEmbers(tone = { tone }, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.horizontalGradient(
                            0f to GameSpacePalette.Void.copy(alpha = 0.92f),
                            0.20f to GameSpacePalette.Void.copy(alpha = 0.66f),
                            0.38f to GameSpacePalette.Void.copy(alpha = 0.22f),
                            0.52f to Color.Transparent,
                            1f to GameSpacePalette.Void.copy(alpha = 0.42f)
                        )
                    )
                    drawRect(
                        Brush.verticalGradient(
                            0f to GameSpacePalette.Void.copy(alpha = 0.50f),
                            0.26f to Color.Transparent,
                            0.60f to Color.Transparent,
                            1f to GameSpacePalette.Deep
                        )
                    )
                }
        )
    }
}

/**
 * فنّ البطل: أيقونة اللعبة بدقّة عالية ترتكز على حافة عمود اللوحات (ويمتدّ عُشرها تحته)، أطرافها تذوب بقناع
 * شعاعيّ يُبقي قلبها، وتتلاشى من جهة النصّ. تُرسم **مرّة واحدة** على طبقة مستقلّة، والانزياح والتنفّس البطيئان
 * يجريان بتحويل الطبقة نفسها (لا إعادة رسم كل إطار).
 */
@Composable
private fun HeroKeyArt(bitmap: ImageBitmap, clock: State<Float>, boardEdge: Int?) {
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                val t = clock.value
                translationX = (stageWave(t, 1, 0.25f) - 0.5f) * ArtDrift * 1.dp.toPx()
                val k = 1f + ArtBreath * stageWave(t, 1)
                scaleX = k
                scaleY = k
            }
    ) {
        val unit = 1.dp.toPx()
        val centered = boardEdge == null
        val side = size.height * if (centered) ArtSidePortrait else ArtSide
        val cx = if (boardEdge == null) size.width * 0.5f else boardEdge * unit - side * ArtUnderBoard
        val cy = size.height * if (centered) 0.5f else ArtCenterY
        val left = cx - side / 2f
        val top = cy - side / 2f
        drawImage(
            image = bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(side.roundToInt(), side.roundToInt()),
            alpha = if (centered) ArtAlphaPortrait else ArtAlpha,
            filterQuality = FilterQuality.High
        )
        // ذوبان الحواف: قناع شعاعيّ يُبقي قلب الفنّ ويُذيب أطرافه وزوايا الأيقونة الشفّافة في الخلفية.
        drawRect(
            brush = Brush.radialGradient(
                0f to Color.Black,
                0.56f to Color.Black,
                0.84f to Color.Black.copy(alpha = 0.40f),
                1f to Color.Transparent,
                center = Offset(cx, cy),
                radius = side * 0.5f
            ),
            topLeft = Offset(left, top),
            size = Size(side, side),
            blendMode = BlendMode.DstIn
        )
        if (!centered) {
            // تلاشٍ من جهة النصّ: يحفظ قراءة الاسم والأزرار.
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.38f to Color.Black,
                    1f to Color.Black,
                    startX = left,
                    endX = left + side
                ),
                topLeft = Offset(left, top),
                size = Size(side, side),
                blendMode = BlendMode.DstIn
            )
        }
    }
}

/** لونٌ مجاور على عجلة الألوان (إزاحة [degrees] في الصبغة): يعطي الأورورا لونين متناغمين بدل لون واحد مسطّح. */
private fun shiftHue(color: Color, degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[0] = (hsv[0] + degrees) % 360f
    return Color(android.graphics.Color.HSVToColor(hsv))
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
                    elevation = GlowPlay,
                    shape = shape,
                    ambientColor = GameSpacePalette.Violet.copy(alpha = 0.55f),
                    spotColor = LuxGold.copy(alpha = 0.60f)
                )
                .clip(shape)
                .background(PlayFill)
                .background(PlayGloss)
                .border(StrokeFirm, PlayRim, shape)
                .stageSheen(clock, cycles = 6, strength = 0.20f)
                .stageEnergyRim(
                    clock = clock,
                    tone = { LuxGold },
                    corner = playH / 2f,
                    thickness = RimThin,
                    cycles = 6
                )
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
                .border(StrokeFine, LuxGold.copy(alpha = 0.45f), CircleShape)
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
    val tone = rememberLobbyTone(packageName)
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
            .stageSheen(clock, cycles = 4, strength = 0.16f)
            .stageEnergyRim(
                clock = clock,
                tone = { tone },
                corner = edge * 0.24f,
                thickness = StrokeSelected,
                head = LegendGold
            )
            .stageGlint(clock),
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
                        .background(if (active) LuxGold else Color.White.copy(alpha = 0.32f))
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
    val clock = LocalStageClock.current
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
                alpha = IdleAlpha + (1f - IdleAlpha) * lift
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
            .then(
                if (selected) {
                    Modifier
                        .stageSheen(clock, cycles = 5, strength = 0.14f)
                        .stageEnergyRim(
                            clock = clock,
                            tone = { LuxGold },
                            corner = RadiusCard,
                            thickness = StrokeSelected,
                            cycles = 4
                        )
                } else {
                    Modifier
                }
            )
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
