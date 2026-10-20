/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.component

import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.text.NumberFormat
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.core.gamespace.LobbyModel
import nd.max.ui.design.MaxAlpha
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace

/*
 * لوبي بطاقات Carousel بعمق: البطاقة المحدَّدة كبيرة بإطار نيون متدرّج (وردي-أحمر ← أزرق) وتوهّج
 * خارجيّ، وجارتاها أضيق ومائلتان بمنظور قويّ وخافتتان. كل التحويل (مقياس · ميل · شفافية · انزلاق)
 * يُحسب في **مرحلة الطبقة** من إزاحة الصفحة، فلا إعادة تركيب أثناء السحب. وكل الرسم بالكود؛
 * الصور الوحيدة أيقونات التطبيقات.
 *
 * اتجاه الميل: الحافة الخارجية لكل جارة أقرب للمشاهد وأطول، والداخلية أبعد وأقصر («شاشة منحنية»)،
 * وهذا ما تُظهره اللقطة المرجعية. **غير مُتحقَّق على جهاز:** سلوك `blur` تحت API 31 (لا أثر له
 * هناك فتبقى الأيقونة المكبَّرة مموَّهة بالترشيح فقط وتحجبها القاعدة السوداء المتدرّجة).
 */

/** مقياس الجارتين مقابل المركزية (مع الميل تضيق الجارة إلى نحو ٧٣٪ من عرض المركزية). */
private const val SIDE_SCALE = 0.90f

/** شفافية الجارتين. */
private const val SIDE_ALPHA = 0.55f

/** ميل الجارتين بالدرجات حول المحور الرأسيّ: ٢٠° (كانت ٣٦°)، فتبقى عناوينهما مقروءة ولا تنحرف. */
private const val SIDE_TILT_DEG = 20f

/** كم تُسحب الجارتان نحو المركز (نسبة من عرض البطاقة): تُبقي بين الحافتين فجوة ضيقة كما في المرجع. */
private const val SIDE_TUCK = 0.08f

/** مسافة الكاميرا (× الكثافة): أصغر منها يشتدّ المنظور. */
private const val CAMERA_DISTANCE = 22f

/** ارتفاع البطاقة المركزية عن الجارتين (dp): تطفو فوق صفّها بدل أن تقف معه على خطّ واحد. */
private const val CARD_LIFT = 6f

/** أكبر عدد بطاقات تعبرها سحبة سريعة واحدة (كانت بطاقة واحدة): قوائم الألعاب الطويلة تُتصفّح بلا ملل. */
private const val MAX_FLING_PAGES = 3

/**
 * الأيقونة الرئيسية تتفاعل مع السحب: تكبر من [EMBLEM_REST_SCALE] إلى ١ كلما اقتربت من المركز، وتنزاح
 * عكس اتجاه البطاقة بنسبة [EMBLEM_PARALLAX] من عرضها (بارالاكس: تتأخّر عن إطارها فيظهر العمق)،
 * وترتفع [EMBLEM_LIFT] dp. كله في طبقة الرسم.
 */
private const val EMBLEM_REST_SCALE = 0.88f
private const val EMBLEM_PARALLAX = 0.18f
private const val EMBLEM_LIFT = 3f

/** حصّة المؤشّر المنزلق من عرض التبويب. */
private const val TAB_INDICATOR_SHARE = 0.70f

/** مقياس بداية نبضة نجمة المفضّلة قبل أن ترتدّ إلى ١. */
private const val POP_FROM = 0.6f

/** مسطرة الموضع: عرض الشرطة وفجوتها وأقصى عرض وارتفاع. */
private val RailTick = 14.dp
private val RailGap = 4.dp
private val RailMaxWidth = 220.dp
private val RailHeight = 3.dp

/** هامش رأسيّ حول البطاقة يتّسع للتوهّج، لأن الـPager يقصّ ما يخرج عن ارتفاعه. تحسبه الشاشة أيضًا لحجم البطاقة. */
internal val LobbyGlowPad = MaxSpace.xl

/** عرض هالة البطاقة المركزية؛ نصفه يخرج عن الحدّ فيملأ الفجوة بين الكروت. */
private val HaloWidth = 10.dp
private val UnderlineHeight = 2.dp
private val TabSlotMin = 112.dp
private val StripPad = MaxSpace.xxl + MaxSpace.sm

/** أجزاء من ارتفاع البطاقة تُشتقّ منها الشارة والنصوص، فتتناسب مع أي مقاس. */
private const val BADGE_SHARE = 0.17f
private const val TITLE_SHARE = 0.088f
private const val META_SHARE = 0.068f
private val BadgeMin = 26.dp
private val BadgeMax = 34.dp

/** الأيقونة الرئيسية في المساحة فوق العنوان: حصّة من ارتفاعها، وحصّة من عرض البطاقة كي لا تضيق الجارتان. */
private const val EMBLEM_SHARE = 0.90f
private const val EMBLEM_WIDTH_SHARE = 0.62f

/** لون اللعبة يبدأ به تدرّج الخلفية من الأعلى، ثم يذوب في السطح الداكن. */
private const val WASH_ALPHA = 0.40f

/** أيقونات بحجم ثابت: تكفي لاستخراج اللون، وهي الصورة الرئيسية أيضًا، فلا تُكبَّر فوق دقّتها. */
private val IconRaster = 128.dp

private const val NO_TONE = -1
private const val OPAQUE = 0xFF000000.toInt()
private val toneCache = LruCache<String, Int>(96)

// ───────────────────────────── أيقونة اللعبة ولونها ─────────────────────────────

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

// ───────────────────────────── بطاقة اللعبة ─────────────────────────────

/**
 * بطاقة لعبة واحدة. [emphasis] دالة (لا قيمة) كي تُقرأ في مرحلة الرسم فقط: 1 للمركزية و0 للبعيدة.
 *
 * التكوين من الخلف إلى الأمام: هالة خافتة خارج الحدّ تتراجع مع البعد، ثم خلفية بلون اللعبة يذوب إلى
 * الأسود، ثم لمعة علوية بالدرجة نفسها، ثم **الأيقونة في المنتصف** بوصفها الصورة الرئيسية. وفي الأسفل
 * العنوان وسطر البيانات، وفي الزاوية العليا شارة سداسية للمفضّلة. وإطار البطاقة بلون اللعبة نفسها.
 *
 * بلا تمويه ولا ظلّ نصّ. النبض الوحيد هو تنفّس الهالة مع زرّ «ابدأ»، وهو الحلقة المعتمدة في اللوبي،
 * والباقي عمق من التدرّج والخطّ الرفيع (`DESIGN.md`، والتمويه ممنوع بـADR-12).
 *
 * @param starEnabled زرّ المفضّلة يعمل للمركزية وحدها؛ في الجارتين تُرسم الشارة حالةً ولا تُلمس،
 *   فنقرة الجارة تنقلك إليها.
 * @param offset إزاحة الصفحة الموقَّعة عن المركز (−١..١)، تُقرأ في الرسم لبارالاكس الأيقونة.
 * @param positionDescription وصف الإتاحة، مثل «Race Master، ٣ من ٧».
 * @param breathe نبض الهالة الوحيد، يتزامن مع زرّ «ابدأ».
 */
@Composable
fun LobbyGameCard(
    packageName: String,
    title: String,
    meta: String,
    favorite: Boolean,
    favoriteDescription: String,
    positionDescription: String,
    emphasis: () -> Float,
    offset: () -> Float,
    breathe: State<Float>,
    starEnabled: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = remember { RoundedCornerShape(MaxRadius.group) }
    val icon by rememberLobbyIcon(packageName)
    val tone = rememberLobbyTone(packageName)
    // نبضة الشارة عند تغيّر المفضّلة بفعل المستخدم فقط: أول تركيب لا يحرّك شيئًا.
    val pop = remember { Animatable(1f) }
    val shownFavorite = remember { mutableStateOf(favorite) }
    LaunchedEffect(favorite) {
        if (favorite != shownFavorite.value) {
            shownFavorite.value = favorite
            pop.snapTo(POP_FROM)
            pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
        }
    }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .semantics { contentDescription = positionDescription }
            .drawWithCache {
                // المسار يُبنى مرّة لكل مقاس لا في كل إطار تمرير؛ المتحرّك (القرب والنبض) يُقرأ عند الرسم.
                val path = pathOf(shape, size, layoutDirection, this)
                onDrawBehind {
                    val e = emphasis()
                    if (e > 0.02f) drawHalo(path, tone, e * (0.55f + 0.45f * breathe.value))
                }
            }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to tone.copy(alpha = WASH_ALPHA),
                    0.5f to LobbyPalette.Surface,
                    1f to LobbyPalette.Black
                )
            )
            .drawWithCache {
                val path = pathOf(shape, size, layoutDirection, this)
                val sheen = Brush.linearGradient(
                    0f to tone.copy(alpha = MaxAlpha.edgeLight),
                    0.55f to Color.Transparent,
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
                onDrawWithContent {
                    drawRect(sheen)
                    drawContent()
                    drawRim(path, tone, emphasis())
                }
            }
    ) {
        val titleSize = (maxHeight.value * TITLE_SHARE).coerceIn(13f, 18f).sp
        val metaSize = (maxHeight.value * META_SHARE).coerceIn(11f, 14f).sp
        val badge = (maxHeight * BADGE_SHARE).coerceIn(BadgeMin, BadgeMax)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaxSpace.lg, vertical = MaxSpace.md)
        ) {
            // الأيقونة تأخذ المساحة فوق النص كلها، فلا تتداخل معه أبدًا.
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val art = icon
                if (art != null) {
                    val side = minOf(maxHeight * EMBLEM_SHARE, maxWidth * EMBLEM_WIDTH_SHARE, IconRaster)
                    Image(
                        bitmap = art,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(side)
                            .graphicsLayer {
                                val e = emphasis()
                                val s = EMBLEM_REST_SCALE + (1f - EMBLEM_REST_SCALE) * e
                                scaleX = s
                                scaleY = s
                                translationX = -offset().coerceIn(-1f, 1f) * size.width * EMBLEM_PARALLAX
                                translationY = -e * EMBLEM_LIFT * density
                            }
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.hairline)) {
                Text(
                    text = title,
                    color = LobbyPalette.Ink,
                    fontSize = titleSize,
                    lineHeight = titleSize * 1.2f,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        color = LobbyPalette.Muted,
                        fontSize = metaSize,
                        lineHeight = metaSize * 1.25f,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(MaxSpace.xs)
                .size(MaxSize.minTouchTarget)
                .then(
                    if (starEnabled) {
                        Modifier.clickable(role = Role.Button, onClickLabel = favoriteDescription, onClick = onToggleFavorite)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(badge)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    }
                    .clip(LobbyHexShape)
                    .background(LobbyPalette.Black.copy(alpha = 0.6f))
                    .border(MaxSize.hairlineBorder, LobbyPalette.Ink.copy(alpha = MaxAlpha.border), LobbyHexShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = null,
                    tint = if (favorite) LobbyPalette.RedBright else LobbyPalette.Ink,
                    modifier = Modifier.size(badge * 0.6f)
                )
            }
        }
    }
}

/**
 * هالة الإطار: خطّ واحد خافت خارج الحدّ، يقوى مع القرب من المركز. تُرسم قبل الـclip فتخرج عن البطاقة،
 * شدّتها تأتي من المستدعي (القرب والنبض معًا)، والدالّة نفسها بلا تمويه.
 */
private fun DrawScope.drawHalo(path: Path, tone: Color, emphasis: Float) {
    drawPath(
        path = path,
        color = tone,
        alpha = MaxAlpha.haloGlow * 2f * emphasis,
        style = Stroke(width = HaloWidth.toPx(), join = StrokeJoin.Round)
    )
}

/**
 * إطار البطاقة بلون اللعبة. الخطّ بضعف عرضه لأن نصفه الخارجيّ يقصّه الـclip، فيبقى المرئيّ بعرض
 * [MaxSize.activeRing]. شدّته تتبع القرب من المركز: حدٌّ خافت للجارتين، وإطار كامل للمركزية.
 */
private fun DrawScope.drawRim(path: Path, tone: Color, emphasis: Float) {
    drawPath(
        path = path,
        color = tone,
        alpha = MaxAlpha.border + (1f - MaxAlpha.border) * emphasis,
        style = Stroke(width = MaxSize.activeRing.toPx() * 2f, join = StrokeJoin.Round)
    )
}

// ───────────────────────────── الـPager بعمق ─────────────────────────────

private fun pageOffset(state: PagerState, index: Int): Float =
    (state.currentPage - index) + state.currentPageOffsetFraction

/**
 * Carousel عرضيّ: صفحة مركزية بعرض [cardWidth] وجارتان مائلتان. يراعي RTL: يُعكس اتجاه الميل
 * والسحب نحو المركز بإشارة [direction] بدل شيفرة شرطيّة في كل مكان.
 *
 * @param onSelect نقرة على جارة تنقلك إليها (المركزية لا تستجيب، فلا يُطلق شيء بنقرة عابرة).
 * @param page محتوى الصفحة؛ يستلم [emphasis] (القرب 0..1) و[offset] (الإزاحة الموقَّعة −١..١) ليرسم نفسه.
 */
@Composable
fun LobbyCardPager(
    state: PagerState,
    cardWidth: Dp,
    cardHeight: Dp,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    key: ((Int) -> Any)? = null,
    page: @Composable (index: Int, emphasis: () -> Float, offset: () -> Float) -> Unit
) {
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val inset = ((maxWidth - cardWidth) / 2).coerceAtLeast(0.dp)
        val fling = PagerDefaults.flingBehavior(
            state = state,
            pagerSnapDistance = PagerSnapDistance.atMost(MAX_FLING_PAGES),
            snapAnimationSpec = LobbySnapSpring
        )
        HorizontalPager(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight + LobbyGlowPad * 2),
            contentPadding = PaddingValues(horizontal = inset),
            pageSize = PageSize.Fixed(cardWidth),
            beyondViewportPageCount = 1,
            pageSpacing = MaxSpace.md,
            flingBehavior = fling,
            key = key
        ) { index ->
            val emphasis = { 1f - abs(pageOffset(state, index)).coerceIn(0f, 1f) }
            val offsetOf = { pageOffset(state, index) }
            Box(
                Modifier
                    .fillMaxSize()
                    .zIndex((100 - abs(index - state.currentPage)).toFloat())
                    .graphicsLayer {
                        val offset = pageOffset(state, index)
                        val side = offset.coerceIn(-1f, 1f)
                        val distance = abs(side)
                        val scale = 1f - (1f - SIDE_SCALE) * distance
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - (1f - SIDE_ALPHA) * distance
                        translationY = -(1f - distance) * CARD_LIFT * density
                        rotationY = side * SIDE_TILT_DEG * direction
                        translationX = side * size.width * SIDE_TUCK * direction
                        cameraDistance = CAMERA_DISTANCE * density
                    }
                    .then(
                        // المركزية بلا clickable أصلًا (لا «معطَّل» في شجرة الإتاحة)؛ الجارتان تنقلانك إليهما.
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
                    .padding(vertical = LobbyGlowPad)
            ) {
                page(index, emphasis, offsetOf)
            }
        }
    }
}

// ───────────────────────────── شريط التبويب العلويّ ─────────────────────────────

/**
 * شريط تبويب معلَّق: شبه منحرف أعرض عند الأعلى بكتفين منحنيين وحدّ فولاذيّ خافت على الحافة السفلى.
 * التبويب المحدَّد نصّه أحمر وتحته خطّ أحمر متوهّج. تبويباته **مرشِّحات حقيقية** للقائمة نفسها
 * (الكل · المفضّلة)، لا وجهات بلا شاشة خلفها — أي تبويب لا تقف خلفه ميزة جاهزة لا يُرسم.
 *
 * ارتفاع الشريط يضعه المستدعي في [modifier]؛ والتبويبات تملأ ارتفاعه فيقع الخطّ على حافته السفلى.
 * [descriptions] وصف الإتاحة لكل تبويب (أطول من النص المرئيّ حيث يلزم، مثل «المفضّلة فقط»).
 */
@Composable
fun LobbyTabStrip(
    labels: List<String>,
    descriptions: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    counts: List<Int> = emptyList()
) {
    // موضع وعرض كل تبويب داخل الشريط (بعد الحشو) يُقاسان عند التخطيط ويُقرآن عند الرسم فقط.
    val lefts = remember(labels.size) { mutableStateListOf<Float>().apply { repeat(labels.size) { add(0f) } } }
    val widths = remember(labels.size) { mutableStateListOf<Float>().apply { repeat(labels.size) { add(0f) } } }
    val slideX = remember { Animatable(0f) }
    val slideW = remember { Animatable(0f) }
    val targetX = lefts.getOrElse(selected) { 0f }
    val targetW = widths.getOrElse(selected) { 0f }
    // المؤشّر **ينزلق** بزنبرك من تبويب إلى آخر بدل أن يخبو في مكان ويظهر في آخر؛ وأول قياس يُثبَّت
    // بلا حركة كي لا ينطلق من الصفر عند فتح الشاشة.
    LaunchedEffect(selected, targetX, targetW) {
        if (targetW <= 0f) return@LaunchedEffect
        if (slideW.value <= 0f) {
            slideX.snapTo(targetX)
            slideW.snapTo(targetW)
        } else {
            val spec = spring<Float>(dampingRatio = 0.78f, stiffness = 520f)
            coroutineScope {
                launch { slideX.animateTo(targetX, spec) }
                launch { slideW.animateTo(targetW, spec) }
            }
        }
    }
    Row(
        modifier = modifier
            .semantics { contentDescription = description }
            .clip(LobbyStripShape)
            .background(
                Brush.verticalGradient(
                    listOf(LobbyPalette.PanelRaised.copy(alpha = 0.55f), LobbyPalette.Surface.copy(alpha = 0.90f))
                )
            )
            .border(
                MaxSize.hairlineBorder,
                Brush.verticalGradient(listOf(Color.Transparent, LobbyPalette.Steel.copy(alpha = 0.55f))),
                LobbyStripShape
            )
            .padding(horizontal = StripPad)
            .drawBehind {
                val full = slideW.value
                if (full > 1f) {
                    val w = full * TAB_INDICATOR_SHARE
                    val x = slideX.value + (full - w) / 2f
                    val line = UnderlineHeight.toPx()
                    drawRect(
                        color = LobbyPalette.Red.copy(alpha = 0.35f),
                        topLeft = Offset(x, size.height - line * 3f),
                        size = Size(w, line * 3f)
                    )
                    drawRect(
                        color = LobbyPalette.RedBright,
                        topLeft = Offset(x, size.height - line),
                        size = Size(w, line)
                    )
                }
            },
        horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            val ink by animateColorAsState(
                targetValue = if (isSelected) LobbyPalette.RedBright else LobbyPalette.Ink.copy(alpha = 0.85f),
                animationSpec = tween(MaxDuration.quick),
                label = "lobbyTabInk"
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = TabSlotMin)
                    .onPlaced { coordinates ->
                        val x = coordinates.positionInParent().x
                        val w = coordinates.size.width.toFloat()
                        if (lefts.getOrNull(index) != x) lefts[index] = x
                        if (widths.getOrNull(index) != w) widths[index] = w
                    }
                    .semantics { contentDescription = descriptions.getOrElse(index) { label } }
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = MaxSpace.md),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaxSpace.xs)
                ) {
                    Text(
                        text = label,
                        color = ink,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                    // عدد حقيقيّ من المكتبة نفسها (ADR-07) بأرقام لغة الجهاز؛ صفر لا يُكتب.
                    val count = counts.getOrNull(index) ?: 0
                    if (count > 0) {
                        Text(
                            text = remember(count) { NumberFormat.getInstance().format(count) },
                            color = ink.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────────── مسطرة الموضع ─────────────────────────────

/**
 * مسطرة موضع تحت الـCarousel: شرطة لكل لعبة، والأقرب إلى المركز يلتحم بلون اللعبة ويتدرّج مع السحب
 * (لا قفزة بين شرطة وأخرى). تُظهر «أين أنا من القائمة» بنظرة، وهو ما تُخفيه البطاقات المقصوصة على الأطراف.
 * [position] و[tone] دالّتان تُقرآن في الرسم فقط، فالمسطرة لا تُعاد تركيبها أثناء السحب. تنعكس في RTL
 * فتوافق اتجاه الـPager. أقل من لعبتين: لا تُرسم.
 */
@Composable
fun LobbyPositionRail(count: Int, position: () -> Float, tone: () -> Color, modifier: Modifier = Modifier) {
    if (count < 2) return
    val width = (RailTick * count + RailGap * (count - 1)).coerceAtMost(RailMaxWidth)
    Canvas(modifier.size(width = width, height = RailHeight)) {
        val pos = position()
        val accent = tone()
        val gap = RailGap.toPx()
        val slot = (size.width - gap * (count - 1)) / count
        val rtl = layoutDirection == LayoutDirection.Rtl
        val radius = CornerRadius(size.height / 2f)
        val rest = LobbyPalette.Steel.copy(alpha = 0.28f)
        for (i in 0 until count) {
            val near = 1f - abs(i - pos).coerceIn(0f, 1f)
            val x = i * (slot + gap)
            drawRoundRect(
                color = lerp(rest, accent, near),
                topLeft = Offset(if (rtl) size.width - x - slot else x, 0f),
                size = Size(slot, size.height),
                cornerRadius = radius
            )
        }
    }
}

// ───────────────────────────── هيكل التحميل ─────────────────────────────

/**
 * هيكل تحميل بثلاث بطاقات بمقاسات الـCarousel نفسها (مركزية وجارتان) تنبض بنبض اللوبي الوحيد [pulse]،
 * فتنتقل العين إلى البطاقات الحقيقية بلا قفزة تخطيط بدل نصّ «جارٍ التحميل» على فراغ. [description]
 * وصف الإتاحة (نفس نصّ التحميل القديم). الجارتان تخرجان عن الحافة كما في الحقيقية.
 */
@Composable
fun LobbySkeletonCards(
    cardWidth: Dp,
    cardHeight: Dp,
    pulse: State<Float>,
    description: String,
    modifier: Modifier = Modifier
) {
    val shape = remember { RoundedCornerShape(MaxRadius.group) }
    val pill = remember { RoundedCornerShape(MaxRadius.pill) }
    val step = cardWidth + MaxSpace.md
    Box(
        modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        SkeletonCard(shape, pill, cardWidth, cardHeight, center = false, pulse = pulse, modifier = Modifier.offset(x = -step))
        SkeletonCard(shape, pill, cardWidth, cardHeight, center = false, pulse = pulse, modifier = Modifier.offset(x = step))
        SkeletonCard(shape, pill, cardWidth, cardHeight, center = true, pulse = pulse, modifier = Modifier)
    }
}

@Composable
private fun SkeletonCard(
    shape: RoundedCornerShape,
    pill: RoundedCornerShape,
    width: Dp,
    height: Dp,
    center: Boolean,
    pulse: State<Float>,
    modifier: Modifier
) {
    Box(
        modifier
            .size(width = width, height = height)
            .graphicsLayer {
                val k = if (center) 1f else SIDE_SCALE
                scaleX = k
                scaleY = k
                alpha = (if (center) 0.95f else 0.55f) * (0.55f + 0.45f * pulse.value)
            }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(LobbyPalette.PanelRaised.copy(alpha = 0.55f), LobbyPalette.Surface)))
            .border(MaxSize.hairlineBorder, LobbyPalette.Hairline, shape)
            .padding(MaxSpace.lg)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxHeight(0.7f)
                        .aspectRatio(1f)
                        .clip(shape)
                        .background(LobbyPalette.Ink.copy(alpha = 0.08f))
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
                Box(Modifier.fillMaxWidth(0.7f).height(MaxSpace.sm).clip(pill).background(LobbyPalette.Ink.copy(alpha = 0.10f)))
                Box(Modifier.fillMaxWidth(0.4f).height(MaxSpace.sm).clip(pill).background(LobbyPalette.Ink.copy(alpha = 0.06f)))
            }
        }
    }
}
