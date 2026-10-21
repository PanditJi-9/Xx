/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.component

import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nd.max.core.gamespace.LobbyDeckMath
import nd.max.core.gamespace.LobbyDepth
import nd.max.core.gamespace.LobbyModel
import nd.max.ui.design.MaxAlpha
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace

/*
 * مسرح اللوبي ثلاثيّ الأبعاد: حلقة شرائح (بطاقات) بعمق حقيقيّ، وخلفية بارالاكس من أيقونة اللعبة
 * نفسها مع جسيمات ضوء، ومسطرة موضع، وهيكل تحميل.
 *
 * ### كيف تُبنى الحلقة
 * `HorizontalPager` يعطي المنطق والفيزياء (سحب/قذف/إمساك). كل صفحة تُحوَّل في `graphicsLayer`
 * بوضعيّة من [LobbyDeckMath.pose] (مقياس وشفافيّة وميل وتقارب)، فتصير الصفوف المستقيمة قوسًا
 * يتراجع في العمق. الحساب كلّه في مرحلة الرسم فلا إعادة تركيب أثناء السحب.
 *
 * ### إشارات الاتجاه
 * إشارة الميل `rotationY = -pose.rotationY × direction` اختيار تصميميّ لم يُتحقَّق منه على جهاز؛ والتقارب نحو المركز يُقلب في RTL عبر `direction`.
 *
 * ### الأداء
 * حلقة مستمرّة واحدة لكل ما يتحرّك (`breathe` للطفو، `drift` للجسيمات) وتتوقّف كلّها عند إيقاف
 * «الحركة المحيطة» أو حين يطلب النظام تقليل الحركة. لا blur ولا RenderEffect (ADR-12: الزينة الثقيلة مُطفأة افتراضيًّا وتكلّف إطارات وبطارية): النعومة من
 * تدرّجات ولون وأيقونة مكبّرة بترشيح خطّي.
 */

/** مسافة الكاميرا (× الكثافة): أصغر منها يشتدّ المنظور. قيمة تقديرية لم تُقَس، وتُضبط على جهاز. */
private const val DECK_CAMERA_DISTANCE = 20f

/** رفع الشريحة المركزية عن صفّها وسعة طفوها بالتنفّس، بالـdp. */
private const val DECK_LIFT_DP = 8f
private const val DECK_BOB_DP = 3f

/** أقصى صفحات تقطعها قذفة واحدة، وعدد الصفحات المركّبة خارج الإطار (تكفي لـ±3 وصلت للحلقة). */
private const val DECK_MAX_FLING_PAGES = 3
private const val DECK_REACH_PAGES = 3

/** الأيقونة داخل الشريحة: مقياسها في الراحة، وانزياح البارالاكس، ورفعها عند التركيز. */
private const val EMBLEM_REST_SCALE = 0.86f
private const val EMBLEM_PARALLAX = 0.16f
private const val EMBLEM_LIFT_DP = 4f
private const val EMBLEM_WIDTH_SHARE = 0.60f

/** أكبر ضلع للأيقونة داخل الشريحة؛ تبقى أصغر من الشريحة بهامش واضح حولها. */
private val EMBLEM_MAX_SIDE = 104.dp

/** صبغة اللعبة في أعلى الشريحة، ولمعة الزجاج المنزلقة عبرها أثناء السحب. */
private const val SLAB_WASH_ALPHA = 0.46f
private const val SLAB_SHEEN_ALPHA = 0.13f
private const val SLAB_SHEEN_TRAVEL = 0.75f
private const val SLAB_TITLE_SIZE_MIN = 13f
private const val SLAB_TITLE_SIZE_MAX = 17f
private const val SLAB_TITLE_SHARE = 0.068f

/**
 * عرض تلاشي حافتَي الحلقة بنسبة عرضها. الـPager يقصّ ما يخرج عن حدوده قصًّا حادًّا (يظهر خطًّا رأسيًّا
 * يقطع الشريحة الجارة بجانب زرّ «ابدأ»)، فتُمحى الحافتان بقناع تدرّجيّ بدل القص.
 */
private const val DECK_EDGE_FADE_SHARE = 0.16f

/** الخلفية: حجم أيقونة اللعبة المكبّرة، وانزياحها، وشفافيّتها. */
private const val GHOST_SCALE = 1.25f
private const val GHOST_PARALLAX = 0.10f
private const val GHOST_ALPHA = 0.30f

/** توهّج الأرضيّة: ضغطه الرأسيّ ونصف قطره بنسبة العرض وموضعه الرأسيّ. */
private const val FLOOR_SQUASH = 0.22f
private const val FLOOR_RADIUS_SHARE = 0.26f
private const val FLOOR_Y_SHARE = 0.80f

/** جسيمات الضوء: العدد وبذرة التوليد الثابتة (نفس الشكل كل مرة) وشفافيّتها. */
private const val MOTE_COUNT = 28
private const val MOTE_SEED = 7
private const val MOTE_PARALLAX = 0.05f
private const val MOTE_ALPHA_MIN = 0.10f
private const val MOTE_ALPHA_SPAN = 0.24f

/** مسطرة الموضع. */
private val RailTick = 18.dp
private val RailTickActive = 30.dp
private val RailThickness = 4.dp
private val RailGap = 6.dp
private const val RAIL_MAX_TICKS = 9

/** هيكل التحميل: مقياس الشريحتين الجانبيتين وشفافيّتهما. */
private const val SKELETON_SIDE_SCALE = 0.82f
private const val SKELETON_SIDE_ALPHA = 0.55f
private const val SKELETON_STEP = 0.95f
private const val SKELETON_PULSE_MIN = 0.35f
private const val SKELETON_PULSE_SPAN = 0.40f

/** حجم رسم الأيقونة بالبكسل: أكبر من أيّ شريحة فلا تتكبّر عيّنة صغيرة. */
private val IconRaster = 128.dp

private const val NO_TONE = -1
private const val OPAQUE = 0xFF000000.toInt()
private val toneCache = LruCache<String, Int>(96)

/** هامش رأسيّ حول الشريحة يتّسع لطفوها وتضخّم حافتها بالمنظور؛ الـPager يقصّ ما يخرج عن ارتفاعه. */
internal val LobbyDeckPad = MaxSpace.xl

/**
 * نسبة عرض الشريحة إلى ارتفاعها.
 *
 * كانت `0.80` — بطاقة عموديّة طويلة تشغل أكثر من نصف ارتفاع الشاشة الأفقيّة، وقال المالك إنها
 * «تبدو طويلة» وطلبها **أقصر وأعرض قليلًا**. و`1.0` تعني مربّعًا: الارتفاع ينقص بنحو السُّبع
 * والعرض يزيد بنحو العُشر، فتبقى الأيقونة (١٠٤dp سقفًا) والعنوان بسطرين داخل البطاقة بهامش واضح
 * (الأيقونة ١٠٤ + كتلة العنوان ~٣٨ + حشو ~٣٢ = ١٧٤dp من أصل ٢١٧dp).
 */
internal const val LOBBY_SLAB_ASPECT = 1.0f

// ─────────────────────────────── أيقونات ولون ───────────────────────────────

/** أيقونة التطبيق من نفس `AppIconCache` الذي تستعمله بقية الشاشات؛ `null` إلى أن تُقرأ. */
@Composable
private fun rememberLobbyIcon(packageName: String?): State<ImageBitmap?> {
    val context = LocalContext.current
    val density = LocalDensity.current
    val px = remember(density) { with(density) { IconRaster.roundToPx() } }
    return produceState<ImageBitmap?>(packageName?.let { AppIconCache.get(it) }, packageName) {
        if (packageName == null) {
            value = null
        } else if (value == null) {
            value = runCatching { AppIconCache.loadIcon(context.packageManager, packageName, px) }.getOrNull()
        }
    }
}

/**
 * لون اللعبة المميَّز، أو أحمر العلامة إن لم يكن في أيقونتها لون ظاهر (رماديّة/سوداء/شفّافة).
 * يُحسب مرّة لكل حزمة على `Default` ويُخزَّن، فلا يتكرّر عند كل تمرير.
 */
@Composable
fun rememberLobbyTone(packageName: String?): Color {
    val icon by rememberLobbyIcon(packageName)
    val rgb by produceState<Int?>(packageName?.let { toneCache.get(it) }, packageName, icon) {
        val pkg = packageName ?: return@produceState
        toneCache.get(pkg)?.let {
            value = it
            return@produceState
        }
        val bitmap = icon ?: return@produceState
        val computed = withContext(Dispatchers.Default) {
            runCatching {
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.readPixels(pixels, 0, 0, bitmap.width, bitmap.height)
                LobbyModel.dominantRgb(pixels)?.let { LobbyModel.lift(it) }
            }.getOrNull()
        }
        val stored = computed ?: NO_TONE
        toneCache.put(pkg, stored)
        value = stored
    }
    val resolved = rgb
    return if (resolved == null || resolved == NO_TONE) LobbyPalette.Red else Color(OPAQUE or resolved)
}

// ─────────────────────────────── مسار الشكل ───────────────────────────────

/** مسار الشكل مرّة واحدة لحجم معلوم؛ يُبنى خارج مسار الرسم ويُعاد استعماله بين الإطارات. */
internal fun pathOf(shape: Shape, size: Size, direction: LayoutDirection, density: Density): Path =
    when (val outline = shape.createOutline(size, direction, density)) {
        is Outline.Generic -> outline.path
        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
    }

// ─────────────────────────────── الحلقة ───────────────────────────────

/**
 * حلقة الشرائح ثلاثيّة الأبعاد. الشريحة المركزية تقف في منتصف المساحة (حشو `contentPadding`)،
 * والجارات تنحني في العمق: تصغر وتخفت وتميل وتقترب من المركز حسب [depth].
 *
 * @param page يرسم محتوى الشريحة؛ يستلم المؤشّر و`slot` (كسريّ، 0 للمركزية) ليحرّك أجزاءه في مرحلة الرسم.
 * @param onSelect نقرة على جارة تُحرّك الحلقة إليها.
 */
@Composable
internal fun LobbyDeck(
    state: PagerState,
    depth: LobbyDepth,
    slabWidth: Dp,
    slabHeight: Dp,
    breathe: State<Float>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    key: ((Int) -> Any)? = null,
    page: @Composable (index: Int, slot: () -> Float) -> Unit
) {
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val inset = ((maxWidth - slabWidth) / 2).coerceAtLeast(0.dp)
        val fling = PagerDefaults.flingBehavior(
            state = state,
            pagerSnapDistance = PagerSnapDistance.atMost(DECK_MAX_FLING_PAGES),
            snapAnimationSpec = LobbySnapSpring
        )
        HorizontalPager(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .height(slabHeight + LobbyDeckPad * 2)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fade = size.width * DECK_EDGE_FADE_SHARE
                    // DstIn: يُبقي من المحتوى بقدر ألفا القناع؛ شفّاف عند الحدّ وكامل عند عمق [fade].
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black,
                            startX = 0f,
                            endX = fade
                        ),
                        size = Size(fade, size.height),
                        blendMode = BlendMode.DstIn
                    )
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Black,
                            1f to Color.Transparent,
                            startX = size.width - fade,
                            endX = size.width
                        ),
                        topLeft = Offset(size.width - fade, 0f),
                        size = Size(fade, size.height),
                        blendMode = BlendMode.DstIn
                    )
                },
            contentPadding = PaddingValues(horizontal = inset),
            pageSize = PageSize.Fixed(slabWidth),
            beyondViewportPageCount = DECK_REACH_PAGES,
            pageSpacing = MaxSpace.md,
            flingBehavior = fling,
            key = key
        ) { index ->
            val slot = { index - (state.currentPage + state.currentPageOffsetFraction) }
            Box(
                Modifier
                    .fillMaxSize()
                    .zIndex(LobbyDeckMath.zOrder(index - state.currentPage.toFloat()))
                    .graphicsLayer {
                        val s = slot()
                        val pose = LobbyDeckMath.pose(s, depth)
                        val focus = LobbyDeckMath.emphasis(s)
                        val bob = (breathe.value - 0.5f) * 2f * DECK_BOB_DP
                        scaleX = pose.scale
                        scaleY = pose.scale
                        alpha = pose.alpha
                        rotationY = -pose.rotationY * direction
                        translationX = -sign(s) * pose.tuck * size.width * direction
                        translationY = -(DECK_LIFT_DP + bob) * focus * density
                        cameraDistance = DECK_CAMERA_DISTANCE * density
                    }
                    .then(
                        if (index != state.currentPage) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelect(index) }
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(vertical = LobbyDeckPad)
            ) {
                page(index, slot)
            }
        }
    }
}

/**
 * شريحة لعبة: زجاج داكن بصبغة لون اللعبة من أعلى، ونصف أيقونة مرفوعة بتوهّج خلفها، واسم اللعبة،
 * ولمعة قطريّة تنزلق عبرها أثناء السحب، وإطار يشتدّ كلّما اقتربت من المركز.
 */
@Composable
internal fun LobbyDeckCard(
    packageName: String,
    title: String,
    description: String,
    gameColors: Boolean,
    slot: () -> Float,
    modifier: Modifier = Modifier
) {
    val shape = remember { RoundedCornerShape(MaxRadius.sheet) }
    val icon by rememberLobbyIcon(packageName)
    val gameTone = rememberLobbyTone(packageName)
    val tone = if (gameColors) gameTone else LobbyPalette.Red
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .semantics { contentDescription = description }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to tone.copy(alpha = SLAB_WASH_ALPHA),
                    0.55f to LobbyPalette.Surface,
                    1f to LobbyPalette.Black
                )
            )
            .drawWithCache {
                val path = pathOf(shape, size, layoutDirection, this)
                onDrawWithContent {
                    drawContent()
                    val s = slot()
                    drawSheen(s)
                    drawRim(path, tone, LobbyDeckMath.emphasis(s))
                }
            }
    ) {
        val titleSize = (maxHeight.value * SLAB_TITLE_SHARE)
            .coerceIn(SLAB_TITLE_SIZE_MIN, SLAB_TITLE_SIZE_MAX).sp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = MaxSpace.lg, end = MaxSpace.lg, top = MaxSpace.lg, bottom = MaxSpace.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .drawBehind {
                        val e = LobbyDeckMath.emphasis(slot())
                        if (e > 0.02f) {
                            val radius = size.minDimension * 0.62f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(tone.copy(alpha = 0.38f * e), Color.Transparent),
                                    center = center,
                                    radius = radius
                                ),
                                radius = radius,
                                center = center
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val art = icon
                if (art != null) {
                    val side = minOf(maxHeight, maxWidth * EMBLEM_WIDTH_SHARE, EMBLEM_MAX_SIDE)
                    Image(
                        bitmap = art,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(side)
                            .graphicsLayer {
                                val s = slot()
                                val e = LobbyDeckMath.emphasis(s)
                                val k = EMBLEM_REST_SCALE + (1f - EMBLEM_REST_SCALE) * e
                                scaleX = k
                                scaleY = k
                                translationX = s.coerceIn(-1f, 1f) * size.width * EMBLEM_PARALLAX * direction
                                translationY = -e * EMBLEM_LIFT_DP * density
                            }
                    )
                }
            }
            // كتلة العنوان بارتفاع سطرين ثابت: تبقى الأيقونات على خطّ واحد بين شرائح بعناوين قصيرة وطويلة.
            val titleBlock = with(LocalDensity.current) { (titleSize * 1.25f * 2f).toDp() }
            Box(
                modifier = Modifier
                    .padding(top = MaxSpace.sm)
                    .fillMaxWidth()
                    .height(titleBlock),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = LobbyPalette.Ink,
                    fontSize = titleSize,
                    lineHeight = titleSize * 1.25f,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** لمعة زجاج قطريّة يتحرّك مركزها عكس اتّجاه [slot] فتنزلق عبر الشريحة وهي تتحرّك. */
private fun DrawScope.drawSheen(slot: Float) {
    val center = size.width * (0.5f - slot.coerceIn(-1.5f, 1.5f) * SLAB_SHEEN_TRAVEL)
    val half = size.width * 0.45f
    drawRect(
        brush = Brush.linearGradient(
            0f to Color.Transparent,
            0.5f to Color.White.copy(alpha = SLAB_SHEEN_ALPHA),
            1f to Color.Transparent,
            start = Offset(center - half, 0f),
            end = Offset(center + half, size.height * 0.7f)
        )
    )
}

/** إطار الشريحة: يشتدّ نحو لون اللعبة كلّما قوي التركيز. */
private fun DrawScope.drawRim(path: Path, tone: Color, emphasis: Float) {
    drawPath(
        path = path,
        color = tone,
        alpha = MaxAlpha.border + (1f - MaxAlpha.border) * emphasis,
        style = Stroke(width = MaxSize.activeRing.toPx(), join = StrokeJoin.Round)
    )
}

// ─────────────────────────────── الخلفية ───────────────────────────────

private class StageMote(
    val x: Float,
    val y: Float,
    val cycles: Int,
    val twinkle: Int,
    val phase: Float,
    val radiusDp: Float,
    val depth: Float
)

/** مواضع الجسيمات ثابتة بالبذرة؛ `cycles` و`twinkle` صحيحان ليتّصل التفاف الحلقة بلا قفزة. */
private val StageMotes: List<StageMote> = run {
    val random = Random(MOTE_SEED)
    List(MOTE_COUNT) {
        StageMote(
            x = random.nextFloat(),
            y = random.nextFloat(),
            cycles = 1 + random.nextInt(3),
            twinkle = 3 + random.nextInt(7),
            phase = random.nextFloat(),
            radiusDp = 1f + random.nextFloat() * 2.2f,
            depth = 0.4f + random.nextFloat() * 0.6f
        )
    }
}

/**
 * خلفية المسرح: أيقونة اللعبة مكبّرة وخافتة (تتمازج بين الجارتين أثناء السحب) + توهّج لون اللعبة خلف
 * الحلقة + تعتيم للقراءة + أرضيّة مضيئة + جسيمات ضوء. كلّها في `drawBehind` واحد يقرأ الحالة في مرحلة
 * الرسم فقط، فلا إعادة تركيب أثناء السحب.
 *
 * @param position موضع الحلقة الكسريّ (مؤشّر الصفحة + الإزاحة).
 * @param packages حزم الألعاب بترتيب الحلقة (لقراءة أيقوناتها من الذاكرة المشتركة).
 * @param drift الحلقة البطيئة 0..1 لانجراف الجسيمات.
 * @param focusX موضع مركز الحلقة أفقيًّا بنسبة العرض.
 */
@Composable
internal fun LobbyStageBackdrop3D(
    tone: () -> Color,
    position: () -> Float,
    packages: List<String>,
    drift: State<Float>,
    motes: Boolean,
    focusX: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxSize()
            .background(LobbyPalette.Black)
            .drawBehind {
                val pos = position()
                val accent = tone()
                drawGhosts(packages, pos)
                drawStageLight(accent, focusX)
                if (motes) drawMotes(accent, pos, drift.value)
            }
    )
}

private fun DrawScope.drawGhosts(packages: List<String>, position: Float) {
    if (packages.isEmpty()) return
    val direction = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
    val first = floor(position).toInt()
    for (i in first..(first + 1)) {
        val pkg = packages.getOrNull(i) ?: continue
        val image = AppIconCache.get(pkg) ?: continue
        val closeness = 1f - abs(i - position).coerceIn(0f, 1f)
        val side = size.maxDimension * GHOST_SCALE
        val shift = (i - position) * size.width * GHOST_PARALLAX * direction
        val left = (size.width - side) / 2f + shift
        val top = (size.height - side) / 2f
        drawImage(
            image = image,
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(side.roundToInt(), side.roundToInt()),
            alpha = GHOST_ALPHA * closeness,
            filterQuality = FilterQuality.Low
        )
    }
}

private fun DrawScope.drawStageLight(tone: Color, focusX: Float) {
    val w = size.width
    val h = size.height
    val rtl = layoutDirection == LayoutDirection.Rtl
    // توهّج لون اللعبة خلف الحلقة.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(tone.copy(alpha = 0.34f), tone.copy(alpha = 0.10f), Color.Transparent),
            center = Offset(w * focusX, h * 0.46f),
            radius = w * 0.46f
        )
    )
    // تعتيم علويّ وسفليّ لقراءة الشريط العلويّ والأزرار.
    drawRect(
        brush = Brush.verticalGradient(
            0f to LobbyPalette.Black.copy(alpha = 0.62f),
            0.30f to Color.Transparent,
            0.62f to Color.Transparent,
            1f to LobbyPalette.Black.copy(alpha = 0.86f)
        )
    )
    // تعتيم جهة عمود المعلومات (بداية الاتّجاه).
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(LobbyPalette.Black.copy(alpha = 0.72f), Color.Transparent),
            startX = if (rtl) w else 0f,
            endX = if (rtl) w * 0.45f else w * 0.55f
        )
    )
    // أرضيّة مضيئة تحت الحلقة: دائرة مضغوطة رأسيًّا إلى بيضاويّ.
    val floorCenter = Offset(w * focusX, h * FLOOR_Y_SHARE)
    val floorRadius = w * FLOOR_RADIUS_SHARE
    withTransform({ scale(1f, FLOOR_SQUASH, pivot = floorCenter) }) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tone.copy(alpha = 0.55f), Color.Transparent),
                center = floorCenter,
                radius = floorRadius
            ),
            radius = floorRadius,
            center = floorCenter
        )
    }
}

private fun stageFract(v: Float): Float = v - floor(v)

private fun DrawScope.drawMotes(tone: Color, position: Float, drift: Float) {
    val twoPi = (2.0 * PI).toFloat()
    for (m in StageMotes) {
        val yy = stageFract(m.y - drift * m.cycles)
        val xx = stageFract(m.x - position * MOTE_PARALLAX * m.depth)
        val twinkle = 0.5f + 0.5f * sin(twoPi * (drift * m.twinkle + m.phase))
        drawCircle(
            color = tone.copy(alpha = (MOTE_ALPHA_MIN + MOTE_ALPHA_SPAN * twinkle) * m.depth),
            radius = m.radiusDp * density,
            center = Offset(xx * size.width, yy * size.height)
        )
    }
}

// ─────────────────────────────── مسطرة الموضع ───────────────────────────────

/**
 * مسطرة موضع: علامة لكل لعبة (أو نافذة منزلقة إن زادت عن [RAIL_MAX_TICKS])، والنشطة أطول ومضيئة بلون
 * اللعبة. تُرسم في مرحلة الرسم وتقرأ الموضع هناك.
 */
@Composable
internal fun LobbyPositionRail(
    count: Int,
    position: () -> Float,
    tone: () -> Color,
    modifier: Modifier = Modifier
) {
    if (count <= 1) return
    val shown = count.coerceAtMost(RAIL_MAX_TICKS)
    val width = RailTick * (shown - 1) + RailTickActive + RailGap * (shown - 1)
    Canvas(modifier.size(width = width, height = RailThickness)) {
        val pos = position().coerceIn(0f, (count - 1).toFloat())
        val windowStart = (pos - (shown - 1) / 2f)
            .coerceIn(0f, (count - shown).toFloat())
        val accent = tone()
        val tick = RailTick.toPx()
        val activeW = RailTickActive.toPx()
        val gap = RailGap.toPx()
        val radius = CornerRadius(size.height / 2f)
        var x = 0f
        for (i in 0 until shown) {
            val index = windowStart + i
            val nearness = 1f - abs(index - pos).coerceIn(0f, 1f)
            val w = tick + (activeW - tick) * nearness
            drawRoundRect(
                color = lerp(LobbyPalette.Hairline, accent, nearness),
                topLeft = Offset(x, 0f),
                size = Size(w, size.height),
                cornerRadius = radius
            )
            x += w + gap
        }
    }
}

// ─────────────────────────────── هيكل التحميل ───────────────────────────────

/**
 * ثلاث شرائح هيكليّة (مركزية وجارتان) تنبض بدل حلقة فارغة أثناء التحميل. تحمل وصفًا للقارئ الصوتي.
 */
@Composable
internal fun LobbyDeckSkeleton(
    slabWidth: Dp,
    slabHeight: Dp,
    pulse: State<Float>,
    description: String,
    modifier: Modifier = Modifier
) {
    val shape = remember { RoundedCornerShape(MaxRadius.sheet) }
    val pill = remember { RoundedCornerShape(MaxRadius.pill) }
    val step = slabWidth * SKELETON_STEP
    Box(
        modifier
            .fillMaxWidth()
            .height(slabHeight + LobbyDeckPad * 2)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        SkeletonSlab(shape, pill, slabWidth, slabHeight, true, pulse, Modifier.offset(x = -step))
        SkeletonSlab(shape, pill, slabWidth, slabHeight, true, pulse, Modifier.offset(x = step))
        SkeletonSlab(shape, pill, slabWidth, slabHeight, false, pulse, Modifier)
    }
}

@Composable
private fun SkeletonSlab(
    shape: Shape,
    pill: Shape,
    width: Dp,
    height: Dp,
    side: Boolean,
    pulse: State<Float>,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .size(width = width, height = height)
            .graphicsLayer {
                val k = if (side) SKELETON_SIDE_SCALE else 1f
                scaleX = k
                scaleY = k
                alpha = (SKELETON_PULSE_MIN + SKELETON_PULSE_SPAN * pulse.value) *
                    (if (side) SKELETON_SIDE_ALPHA else 1f)
            }
            .clip(shape)
            .background(LobbyPalette.Surface)
            .padding(MaxSpace.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(MaxSpace.md)
                .clip(shape)
                .background(LobbyPalette.Hairline)
        )
        Box(
            Modifier
                .padding(top = MaxSpace.md)
                .fillMaxWidth(0.6f)
                .height(MaxSpace.md)
                .clip(pill)
                .background(LobbyPalette.Hairline)
        )
    }
}
