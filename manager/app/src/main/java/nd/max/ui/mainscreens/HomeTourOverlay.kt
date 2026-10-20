/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * غطاء جولة أول فتح — **تعتيم الشاشة وثقبٌ مضيء حول العنصر المشروح وبطاقة شرح** (`HomeTourModel`).
 *
 * أربع قرارات هندسية، لأن هذا النوع من الأغطية يفشل بصمت:
 *
 * 1. **المرساة تُقاس بموضعها لا بحدودها المقصوصة.** `boundsInWindow()` تقصّ المستطيل بحدّ الشاشة،
 *    فيصير ما تحت الشاشة بلا حجم ولا يُعرف كم نمرّر إليه. فنقرأ `positionInWindow()` + `size`
 *    (موضع حقيقي حتى خارج الإطار)، والرئيسية عنصر واحد في `LazyColumn` فكل كتلتها مركّبة ومقيسة.
 * 2. **التمرير إلى العنصر قبل إضاءته.** كل خطوة تمرّر القائمة حتى يستقرّ أعلى العنصر عند ١٦٪ من
 *    الارتفاع (`ScrollAnchor`)، فتبقى تحته مساحةٌ لبطاقة الشرح. ويُنتظر قياس المرساة بـ`snapshotFlow`
 *    لا بتأخير ثابت — فلا تتوقف الجولة على جهاز بطيء ولا تسبق القياس على سريع.
 * 3. **الثقب بـ`BlendMode.Clear` داخل طبقة منفصلة** (`CompositingStrategy.Offscreen`): بلا الطبقة يمحو
 *    `Clear` الخلفية كلها إلى الشفاف فيظهر أسود. والثقب يتبع العنصر كل إطار بلا تحريك مرافق —
 *    كان التحريك سيتخلف عن التمرير ويُضيء مكانًا ليس فيه العنصر.
 * 4. **الغطاء يبتلع اللمس** (`pointerInput`)، فلا يضغط المستخدم زرًّا تحت التعتيم ولا يمرّر الشاشة
 *    خلف الجولة. و**زر الرجوع يتخطّى** (يُتمّ الجولة) بدل أن يغلق الشاشة من تحتها.
 *
 * **ما لا تفعله:** لا تُغيّر حالة التطبيق ولا تطلب صلاحية؛ إتمامها يحفظ علمًا واحدًا
 * (`HomeGuideStore.tourFinished`) و`?` في الرأس يعيدها. وبعد احترام إيقاف الحركة في النظام:
 * بلا حركة يقفز التمرير إلى موضعه.
 *
 * **وعبورها التبويبات** (طلب المالك، ٢٠٢٦-١٠-١٣) أضاف ثلاثة قرارات، وكلّها لئلّا تتجمّد الجولة:
 *
 * 5. **الغطاء يقود التنقّل لا المستخدم.** كل خطوة تحمل قسمها (`HomeTourSection`)، فإن كانت في
 *    تبويب غير المعروض الآن انتقلت إليه (`navigateToPrimary`) وتوقّفت — ثم تُعيد إعادةُ التركيب
 *    بعد التنقّل إكمالَ الخطوة. ولو انتظرت في مكان واحد لفعلت الشيء نفسه وهي لا تعرف أنها انتظرت.
 * 6. **الانتظار بمهلة لا إلى الأبد.** القياس قد لا يأتي أبدًا (كتلة مخفيّة بشرط حالة، أو شاشة
 *    رفضت العرض)، فبدون مهلة تبقى الجولة معلّقة بلا زرّ يعمل. بعد المهلة تُعرض البطاقة **بلا ثقب**
 *    — وتقول ما تقوله بلا كذّ: لا تُضيء مكانًا ليس فيه العنصر.
 * 7. **التمرير للرئيسية وحدها.** بقية الشاشات مراسيها في أعلى صفحتها، فلا تحتاج تمريرًا مدارًا
 *    (`listState = null`) — وتمريرٌ بلا حالة يظنّ أنه مرّر وهو لا يفعل.
 *
 * وتُرسم في **جذر النافذة فوق الشريط السفلي** لأن الجولة تعبر التبويبات: لو بقيت تحت الشريط لما
 * رأى المستخدم غطاءها على الشاشة التي انتقل إليها، ولامت المسة على الشريط الجولة عن خطوتها.
 */
package nd.max.ui.mainscreens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlin.math.abs
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import nd.max.R
import nd.max.ui.component.MaxMotion
import nd.max.ui.component.NeuralIconChip
import nd.max.ui.component.NeuralPanel
import nd.max.ui.component.NeuralPanelShape
import nd.max.ui.component.NeuralPill
import nd.max.ui.component.NeuralTrack
import nd.max.ui.component.maxAdaptiveContentWidth
import nd.max.ui.component.neuralPalette
import nd.max.ui.component.rememberAnimationsEnabled
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.floatingBottomBarPadding
import nd.max.ui.navigation.MaxDestination

/** أين يستقرّ أعلى العنصر المشروح من ارتفاع الشاشة بعد التمرير. */
private const val ScrollAnchor = 0.16f

/** أقصى انتظار لقياس مرساة الخطوة — بعده تُعرض البطاقة بلا ثقب بدل أن تبقى الجولة معلّقة. */
private const val AnchorTimeoutMs = 2_000L

/** مهلة قصيرة لقياس المرساة قبل أن نفترض أن صفحتها مُمرَّرة بعيدًا ونعيدها إلى الأعلى. */
private const val AnchorQuickMs = 400L

/** عتمة الغطاء: تُخفي ما حول العنصر وتُبقي شكل الشاشة مقروءًا. */
private const val ScrimAlpha = 0.78f

/** أقصى عدد محاولات تمرير لتقريب العنصر من موضعه (القياس بعد التمرير قد يختلف عن المتوقّع). */
private const val MaxScrollPasses = 3

/** ارتفاع تقديري للبطاقة قبل أن تُقاس أول مرة، حتى لا يُحسب موضعها على صفر. */
private const val CardHeightEstimateDp = 300

/** تمرير بطلب من الجولة: [delta] بالبكسل، و[animated] احترامًا لإيقاف الحركة في النظام. */
internal typealias HomeTourScrollAction = suspend (Float, Boolean) -> Unit

/**
 * ما تعرفه شاشةٌ عن قائمتها: تمرير بمقدار، والعودة إلى أعلاها.
 *
 * **والعودة إلى الأعلى مطلوبة:** مراسي الإعدادات والتطبيقات والتحكم في أعلى صفحتها، والصفحة تحفظ موضع
 * تمريرها. فمن مرّر الإعدادات إلى أسفلها ثم بدأ الجولة وجد الهدف خارج الشاشة — بل خارج التركيب
 * (عناصر `LazyColumn` البعيدة تُحذف) فلا تُقاس مرساته أصلًا ولا يُعرف كم نمرّر.
 */
@Stable
internal class HomeTourScroller(
    val by: HomeTourScrollAction,
    val toTop: suspend () -> Unit,
)

/**
 * سجلّ مواضع المراسي — تكتبه الشاشات أثناء الرسم، ويقرؤه الغطاء. حالة قابلة للمراقبة، فيتتبع
 * الثقبُ العنصرَ أثناء التمرير.
 */
@Stable
internal class HomeTourTargets {
    private val bounds = mutableStateMapOf<HomeTourTarget, Rect>()

    operator fun get(target: HomeTourTarget): Rect? = bounds[target]

    /** لا تُكتب حالة بقيمة لم تتغيّر: كل كتابة تُعيد تركيب الغطاء. */
    fun report(target: HomeTourTarget, rect: Rect) {
        if (bounds[target] != rect) bounds[target] = rect
    }

    /**
     * يُبقي مراسي قسمٍ واحد ويمحو ما سواه.
     *
     * المرساة تُقاس مرّة وتبقى في السجلّ بعد أن تخرج شاشتها من التركيب، فثقبٌ يُرسم بموضع قديم
     * يُضيء مكانًا فارغًا في شاشة أخرى. يُمحى مع كل تغيّر قسم، فيتبع الثقبُ عنصرًا معروضًا لا
     * موضعًا محفوظًا — والجولة تعبر أربعة تبويبات، فالمسألة ليست نظرية.
     */
    fun retain(section: HomeTourSection) {
        bounds.keys.filter { it.section != section }.forEach { bounds.remove(it) }
    }

    /**
     * تمرير الشاشة التي تملك القائمة — تُعلَّق مرّة واحدة من تلك الشاشة.
     *
     * **لماذا لا يُمرّر الغطاء بنفسه:** التمرير يحتاج `LazyListState`، وحالتها ملك الشاشة لا
     * الغطاء؛ فلو أخذها الغطاء في توقيعه لأخبر عن قائمةٍ يملكها غيره، ولاحتجنا حالة قائمةٍ لكل
     * شاشة يمرّ بها. البديل مرّة واحدة: من يملك القائمة يعرف كيف يمرّرها.
     */
    fun attachScroll(scroller: HomeTourScroller) {
        this.scroller = scroller
    }

    /**
     * يفكّ تعليق **هذا** المُمرِّر فقط. الشاشة القديمة تبقى مركّبة أثناء حركة الانتقال، فلو مسحت
     * `null` غير مشروط لمحَت مُمرِّر الشاشة الجديدة التي علّقت نفسها قبل أن تنتهي الحركة.
     */
    fun detachScroll(scroller: HomeTourScroller) {
        if (this.scroller === scroller) this.scroller = null
    }

    /** يُمرّر بالنيابة عن الشاشة التي علّقت التمرير، ولا يفعل شيئًا إن لم تُعلَّق (شاشات بلا قائمة). */
    suspend fun scrollBy(delta: Float, animated: Boolean) {
        scroller?.by?.invoke(delta, animated)
    }

    /** يُعيد الشاشة إلى أعلاها حتى تُركَّب مراسيها العليا؛ بلا فعل إن لم تُعلَّق قائمة. */
    suspend fun scrollToTop() {
        scroller?.toTop?.invoke()
    }

    private var scroller: HomeTourScroller? = null
}

/**
 * يربط قائمة الشاشة بالجولة: تمرير بمقدار، وعودة إلى الأعلى. يُستدعى مرّة في كل شاشة تحمل مراسي
 * (الرئيسية · التحكم · التطبيقات · الإعدادات)، وبلا أثر خارج الجولة (`LocalHomeTourTargets == null`).
 */
@Composable
internal fun HomeTourScrollBinding(listState: LazyListState) {
    val targets = LocalHomeTourTargets.current
    DisposableEffect(targets, listState) {
        val scroller = HomeTourScroller(
            by = { delta, animated ->
                if (animated) {
                    listState.animateScrollBy(delta, tween(MaxMotion.standard, easing = FastOutSlowInEasing))
                } else {
                    listState.scrollBy(delta)
                }
            },
            toTop = { listState.scrollToItem(0) },
        )
        targets?.attachScroll(scroller)
        onDispose { targets?.detachScroll(scroller) }
    }
}

/**
 * سجلّ المراسي المُعلَّق للشاشات كلّها، و`null` خارج الجولة (معاينات الاستوديو · اختبارات الواجهة)
 * فيصير `HomeTourAnchor` بلا فعل ولا قياس.
 *
 * **لماذا CompositionLocal لا وسيط في التوقيع:** المراسي صارت في أربع شاشات، وتمريرُها في توقيع
 * كل شاشة يجعل كل شاشة تعرف بجولة تعرضها شاشة أخرى؛ والتوقيع ينمو مع كل خطوة جديدة.
 */
internal val LocalHomeTourTargets = compositionLocalOf<HomeTourTargets?> { null }

/** إعادة الجولة من `?` في رأس الرئيسية — تُعلَّق من الجذر لأن الجولة تعبر التبويبات. */
internal val LocalHomeTourRestart = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * تعليم كتلة بأنها مرساة الجولة. تُقاس بـ`positionInWindow` + `size` لا بـ`boundsInWindow`
 * (الأخيرة تقصّ ما تحت الشاشة فيصير بلا حجم ولا يُعرف كم نمرّر إليه).
 */
@Composable
internal fun HomeTourAnchor(
    target: HomeTourTarget,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val targets = LocalHomeTourTargets.current
    Box(
        modifier.then(
            if (targets == null) {
                Modifier
            } else {
                Modifier.onGloballyPositioned { coordinates ->
                    targets.report(target, Rect(coordinates.positionInWindow(), coordinates.size.toSize()))
                }
            }
        )
    ) {
        content()
    }
}

/** الوجهة التي تنتمي إليها الخطوة — من الوجهة نفسها لا من نصّ مسار مكتوب بيد. */
private fun HomeTourSection.destination(): MaxDestination = when (this) {
    HomeTourSection.Home -> MaxDestination.Now
    HomeTourSection.Control -> MaxDestination.Control
    HomeTourSection.Apps -> MaxDestination.Apps
    HomeTourSection.Settings -> MaxDestination.Settings
}

@Composable
internal fun HomeTourOverlay(
    targets: HomeTourTargets,
    currentRoute: String?,
    onNavigate: (MaxDestination) -> Unit,
    onFinish: () -> Unit,
) {
    val p = neuralPalette()
    val scheme = MaterialTheme.colorScheme
    val animationsEnabled = rememberAnimationsEnabled()
    var index by remember { mutableIntStateOf(0) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val step = HomeTourModel.steps[index]
    val section = step.target.section
    val last = HomeTourModel.isLast(index)

    BackHandler(onBack = onFinish)

    // (١) القسم أوّلًا: مراسي غيره تُمحى فلا يُضيء ثقبٌ موضعًا من شاشة لم تعد معروضة، وإن كانت
    //     الخطوة في تبويب آخر انتقلنا وتوقّفنا هنا — والتنقّل يغيّر `currentRoute` فتُعاد هذه
    //     الكتلة لتُكمل الخطوة داخل شاشتها. (بلا `return` هنا كان الانتظار يقع على شاشة خاطئة.)
    //
    //     والمفتاح **الخطوة** لا القسم وحده: كان المفتاح `section` فلا تُعاد الكتلة حين تنتقل الجولة
    //     بين خطوات الرئيسية الست (كلها في قسم واحد)، فلا يُمرَّر شيء أبدًا — يبقى الثقب في أسفل
    //     الشاشة تحت الشريط السفلي أو خارجها، وتغطي البطاقة ما بقي ظاهرًا.
    LaunchedEffect(index, currentRoute) {
        targets.retain(section)
        if (currentRoute != section.route) {
            onNavigate(section.destination())
            return@LaunchedEffect
        }
        // (٢) القياس بمهلة: كتلة مخفيّة بشرط حالة — أو شاشة لم ترسم مرساها — لا تُجمّد الجولة.
        //     المرساة قد لا تكون مركّبة أصلًا (صفحة مُمرَّرة بعيدًا فحُذف عنصرها من `LazyColumn`)،
        //     فبعد مهلة قصيرة نعيد الصفحة إلى أعلاها ثم ننتظر مرّة أخرى بالمهلة الكاملة.
        val measured = withTimeoutOrNull(AnchorQuickMs) {
            snapshotFlow { targets[step.target] }.filterNotNull().first()
        } ?: run {
            targets.scrollToTop()
            withTimeoutOrNull(AnchorTimeoutMs) {
                snapshotFlow { targets[step.target] }.filterNotNull().first()
            }
        }
        if (measured == null) return@LaunchedEffect
        // قياس الغطاء نفسه يُنتظر أيضًا: كان `size == Zero` يُنهي الكتلة بلا تمرير ولا إعادة محاولة
        // لو سبق أوّلُ تركيبٍ أوّلَ قياس.
        val overlay = withTimeoutOrNull(AnchorTimeoutMs) {
            snapshotFlow { size }.first { it != IntSize.Zero }
        } ?: return@LaunchedEffect
        // (٣) التمرير: للشاشة التي علّقت قائمتها (الرئيسية)، وغيرها لا يمرّر شيئًا. وتُعاد القياسات
        //     بعد كل تمريرة حتى يستقرّ العنصر أو يتوقف التقدّم (نهاية القائمة).
        repeat(MaxScrollPasses) {
            val before = targets[step.target] ?: return@LaunchedEffect
            val delta = before.top - origin.y - overlay.height * ScrollAnchor
            if (abs(delta) < 1f) return@LaunchedEffect
            targets.scrollBy(delta, animationsEnabled)
            withFrameNanos { }
            withFrameNanos { }
            val after = targets[step.target] ?: return@LaunchedEffect
            if (abs(after.top - before.top) < 1f) return@LaunchedEffect
        }
    }

    // الإحداثيات من النافذة إلى الغطاء نفسه: إن لم يبدأ عند (٠،٠) (شريط نظام، نافذة منقسمة)
    // أزاح الثقبُ العنصرَ بمقدار الإزاحة.
    val rect = targets[step.target]?.translate(-origin)

    // ── هندسة البطاقة والثقب (بكسلات): البطاقة **لا تُغطّي** المُضاء أبدًا.
    val density = LocalDensity.current
    var cardHeight by remember { mutableIntStateOf(0) }
    val topEdge = with(density) {
        (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + MaxSpace.lg).toPx()
    }
    val bottomEdge = with(density) {
        floatingBottomBarPadding(
            MaxSpace.lg + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ).toPx()
    }
    val gap = with(density) { MaxSpace.md.toPx() }
    val holePad = with(density) { MaxSpace.sm.toPx() }
    val cardH = if (cardHeight > 0) cardHeight.toFloat() else with(density) { CardHeightEstimateDp.dp.toPx() }
    val heightPx = size.height.toFloat()
    val contentBottom = heightPx - bottomEdge          // أسفل ما يُرى من الصفحة فوق الشريط السفلي
    val topCardBottom = topEdge + cardH                // لو كانت البطاقة في الأعلى
    val bottomCardTop = heightPx - bottomEdge - cardH  // لو كانت البطاقة في الأسفل

    val wanted = rect?.let { Rect(it.left - holePad, it.top - holePad, it.right + holePad, it.bottom + holePad) }
    val preferBottom = wanted == null || wanted.center.y < heightPx / 2f
    val fitsBottom = wanted == null || wanted.bottom <= bottomCardTop - gap
    val fitsTop = wanted == null || wanted.top >= topCardBottom + gap
    val cardAtBottom = when {
        preferBottom && fitsBottom -> true
        !preferBottom && fitsTop -> false
        fitsBottom -> true
        fitsTop -> false
        else -> preferBottom // لا يتّسع الجانبان (عنصر طويل): تُقصّ حافة الثقب أدناه بدل أن تُغطّيه.
    }
    // الثقب: يُقصّ بحدّ البطاقة وبحدّ المحتوى المرئي فلا يمتدّ تحت البطاقة ولا تحت الشريط السفلي.
    val hole: Rect? = wanted?.let { w ->
        val top = if (cardAtBottom) w.top else maxOf(w.top, topCardBottom + gap)
        val bottom = if (cardAtBottom) minOf(w.bottom, bottomCardTop - gap, contentBottom) else minOf(w.bottom, contentBottom)
        Rect(w.left, maxOf(top, 0f), w.right, minOf(bottom, heightPx)).takeIf { it.height > holePad * 2f }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                origin = it.positionInWindow()
                size = it.size
            }
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
        ) {
            drawRect(scheme.scrim.copy(alpha = ScrimAlpha))
            if (hole != null) {
                val corner = CornerRadius(MaxRadius.group.toPx() + holePad / 2f)
                drawRoundRect(Color.Black, hole.topLeft, hole.size, corner, blendMode = BlendMode.Clear)
                drawRoundRect(
                    color = p.accent.copy(alpha = .85f),
                    topLeft = hole.topLeft,
                    size = hole.size,
                    cornerRadius = corner,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        val cardEdge = if (cardAtBottom) {
            Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = floatingBottomBarPadding(
                        MaxSpace.lg + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
                )
        } else {
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + MaxSpace.lg)
        }
        TourCard(
            step = step,
            index = index,
            total = HomeTourModel.count,
            last = last,
            onBack = { index = HomeTourModel.previous(index) },
            onNext = {
                if (last) {
                    onFinish()
                } else {
                    index = HomeTourModel.next(index)
                }
            },
            onSkip = onFinish,
            modifier = cardEdge
                .maxAdaptiveContentWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = MaxSpace.lg)
                .onSizeChanged { cardHeight = it.height },
        )
    }
}

private fun HomeTourStep.glyph(): ImageVector = when (this) {
    HomeTourStep.Identity -> Icons.Rounded.Shield
    HomeTourStep.Vitals -> Icons.Rounded.Speed
    HomeTourStep.Actions -> Icons.Rounded.Bolt
    HomeTourStep.Pulse -> Icons.Rounded.Insights
    HomeTourStep.Memory -> Icons.Rounded.Memory
    HomeTourStep.Deck -> Icons.Rounded.Tune
    HomeTourStep.ControlDomains -> Icons.Rounded.Tune
    HomeTourStep.AppsSearch -> Icons.Rounded.Search
    HomeTourStep.AppsFilters -> Icons.Rounded.Apps
    HomeTourStep.SettingsTop -> Icons.Rounded.Palette
}

@Composable
private fun TourCard(
    step: HomeTourStep,
    index: Int,
    total: Int,
    last: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier,
) {
    val p = neuralPalette()
    // خلفية **مصمتة** تحت لوحة النيورال شبه الشفافة: بدونها يظهر نصّ الشاشة وبطاقاتها من تحت البطاقة
    // (حين يمتدّ الثقب المُضاء خلفها) فيتداخل مع نصّ الشرح ويصعب قراءته.
    NeuralPanel(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow, NeuralPanelShape),
        accent = p.accent,
        verticalSpacing = MaxSpace.md,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeuralIconChip(step.glyph(), p.accent, size = 36.dp)
            Spacer(Modifier.width(MaxSpace.sm))
            Text(
                stringResource(R.string.home_tour_step, index + 1, total),
                color = p.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            NeuralPill(
                text = stringResource(R.string.home_banner_skip),
                accent = p.muted,
                compact = true,
                onClick = onSkip,
            )
        }
        Text(
            stringResource(step.titleRes),
            color = p.text,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            stringResource(step.bodyRes),
            color = p.muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
        )
        NeuralTrack((index + 1f) / total, p.accent)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
            HomeActionButton(
                text = stringResource(R.string.home_tour_back),
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                filled = false,
                accent = if (index == 0) p.muted else p.accent,
                onClick = onBack,
                modifier = Modifier.weight(1f),
            )
            HomeActionButton(
                text = stringResource(if (last) R.string.home_banner_done else R.string.home_tour_next),
                icon = if (last) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                filled = true,
                accent = p.accent,
                onClick = onNext,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
