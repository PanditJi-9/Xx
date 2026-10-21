/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavHostController
import kotlin.math.abs
import kotlinx.coroutines.flow.drop
import nd.max.ui.component.rememberMaxHaptics
import nd.max.ui.design.MaxAlpha
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.mainscreens.ApplistScreen
import nd.max.ui.mainscreens.ControlScreen
import nd.max.ui.mainscreens.HomeScreen
import nd.max.ui.mainscreens.HomeTourTargets
import nd.max.ui.mainscreens.LocalHomeTourTargets
import nd.max.ui.mainscreens.SettingsScreen

/**
 * سطح الأقسام الأربعة — **صفحات أفقية حيّة لا انتقالات مسارات**.
 *
 * **العطب الذي حلّ محلّه.** كان السحب بين الأقسام يغيّر مسار `NavHost`، فتُركَّب الشاشة الجديدة
 * من الصفر وتخرج القديمة، والحركة انزلاقُ شاشةٍ كاملة فوق أخرى. النتيجة أن الأقسام تُقرأ كأنها
 * تطبيقات منفصلة لا وجهًا واحدًا، وأن كل سحب يدفع ثمن تركيب شاشة كاملة قبل أن يظهر شيء.
 *
 * **والصفحة هنا مكوَّنة سلفًا:** [HorizontalPager] يملك المسافة والسرعة والإفلات، و`beyondViewportPageCount`
 * يجهّز الجارة قبل أن يلمسها المستخدم، فلا يبدأ السحب على فراغ. والصفحة في السكون تملأ الشاشة
 * بحوافها، ومع أوّل حركة تنكمش قليلًا وتستدير، فتظهر البطاقة الجارة فوق خلفية واحدة — «مجموعة
 * بطاقات» لا «شاشتان تتبادلان».
 *
 * **والحساب في مرحلة الرسم لا في التركيب:** كل تحويل الصفحة (مقياس، استدارة، قصّ، رفع) يُقرأ من
 * [PagerState] داخل `graphicsLayer`/`drawWithContent`، فلا إعادة تركيب ولا قياس في أي إطار من
 * إطارات السحب. وهذا هو الفرق بين حركة ناعمة وحركة «تتبع الإصبع متأخرةً».
 *
 * @param activeRoute المسار الرئيسيّ المعروض فعلًا؛ يحدّد الصفحة النشطة (لبثّها الحيّ وللجولة)
 *   ورمز الخلفية. لا يُقرأ من [PagerState] في التركيب لأنه يتغيّر عند الاستقرار لا مع كل بكسل.
 * @param gesturesEnabled مفتاح تفضيل السحب؛ إطفاؤه لا يمنع الانتقال بالنقر أو بالجولة.
 * @param tourTargets سجلّ مراسي الجولة، يُعلَّق للصفحة النشطة وحدها كي لا تعلّق صفحةٌ مجاورة
 *   مُمرِّر قائمتها مكان الصفحة التي يراها المستخدم.
 * @param saveableStateHolder حافظ حالة الصفحات في الجذر: يبقى حيًّا حين يخرج سطح الأقسام من
 *   التركيب عند فتح شاشة فرعية، فتعود كل قائمة إلى موضعها لا إلى أعلاها.
 * @param onSettledPage يُنادى عند استقرار صفحة **بعد سحب المستخدم** أو بعده؛ المُنادي يقارن
 *   بالمسار الحاليّ فلا يعيد التنقّل إلى ما هو عليه.
 */
@Composable
internal fun PrimaryDeck(
    pagerState: PagerState,
    navController: NavHostController,
    activeRoute: String?,
    gesturesEnabled: Boolean,
    saveableStateHolder: SaveableStateHolder,
    modifier: Modifier = Modifier,
    tourTargets: HomeTourTargets? = null,
    onSettledPage: (Int) -> Unit = {},
) {
    val haptics = rememberMaxHaptics()
    val latestOnSettledPage by rememberUpdatedState(onSettledPage)
    var userDragging by remember(pagerState) { mutableStateOf(false) }
    // لا يُقرأ `pagerState.currentPage` في التركيب هنا: قراءته كانت ستُعيد تركيب السطح مع
    // كل صفحة تعبر. البداية تُكتب داخل التأثير نفسه (غير مُراقَبة) ثم يُقرأ التغيّر من التدفّق.
    var lastTickedPage by remember(pagerState) { mutableIntStateOf(0) }

    // نقرة واحدة خفيفة عند عبور صفحة **بيد المستخدم**. ولا تُطلق مع الانتقال البرمجيّ
    // (شريط التنقّل أو الجولة): ذلك الفعل له اهتزازُه الخاص، ونبضتان لفعل واحد ضجيج.
    LaunchedEffect(pagerState) {
        pagerState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> userDragging = true
                // لمسة لم تحرّك الصفحة ليست سحبًا: تبقى النبضة للانتقال البرمجيّ وحده.
                is DragInteraction.Stop, is DragInteraction.Cancel ->
                    if (!pagerState.isScrollInProgress) userDragging = false
                else -> Unit
            }
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) userDragging = false
        }
    }
    LaunchedEffect(pagerState) {
        lastTickedPage = pagerState.currentPage
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page != lastTickedPage) {
                if (userDragging) haptics.step()
                lastTickedPage = page
            }
        }
    }
    // يُنادى مرة عند أول تركيب أيضًا؛ ويُسقطها `.drop(1)` لأن أول قيمة ليست «استقرارًا بعد
    // فعل» بل الوضع الذي وُجد عليه السطح. فلو نُقل بها لسبقت مزامنة المسار عند العودة من شاشة
    // فرعية غيّرت المسار، وأعادت المستخدم إلى صفحة قديمة.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .drop(1)
            .collect { page ->
                latestOnSettledPage(page)
            }
    }

    val fling = PagerDefaults.flingBehavior(
        state = pagerState,
        // قسم واحد لكل قفزة: أربعة أقسام لا عشرون، وتخطّي قسمين بقفزة واحدة يجعل المستخدم
        // يفقد أين هبط. والسرعة تتحكّم في «هل ينتقل أصلًا» لا في «كم قسمًا يتخطّى».
        pagerSnapDistance = PagerSnapDistance.atMost(1),
        snapAnimationSpec = PrimaryDeckSnapSpring,
    )

    Box(modifier = modifier.fillMaxSize()) {
        PrimaryDeckBackdrop(activeRoute = activeRoute)

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // تجهيز الجارة يمنع أول إطار فارغ في أول سحب؛ والحدّ واحد لا أكثر لأن كل صفحة
            // شاشة كاملة ببثّها الحيّ، وجيران أبعد لا يراهم أحد.
            beyondViewportPageCount = 1,
            userScrollEnabled = gesturesEnabled,
            flingBehavior = fling,
            key = { index -> MaxDestination.PrimaryDestinations[index].route },
        ) { page ->
            val destination = MaxDestination.PrimaryDestinations[page]
            val isActive = destination.route == activeRoute
            // يُقرأ في التركيب لأن `drawWithContent` ليس `@Composable`.
            val outlineColor = MaterialTheme.colorScheme.outline

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // كل القراءات هنا داخل مرحلة الرسم: لا شيء منها يُعيد التركيب.
                    .graphicsLayer {
                        val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        // **`steps` من الصفر لا من الواحد:** الصفحة الساكنة (`offset = 0`) تملأ
                        // الحواف بلا انكماشٍ ولا استدارة ولا قصّ، ومع أوّل حركة ينمو الرقم فيظهر
                        // أثر البطاقة. وكان مقلوبًا (`1 - |offset|`) فتُصغَّر **الصفحة المعروضة**
                        // وتُستدير في السكون: يقرؤها المستخدم «شاشة داخل شاشة» على خلفية سطح
                        // أفتح من محتواها (طلب المالك: الحواف الغريبة و«كأنها شاشة صغيرة»).
                        val steps = abs(offset).coerceIn(0f, 1f)
                        val scale = 1f - CARD_SCALE_DROP * steps
                        scaleX = scale
                        scaleY = scale
                        translationY = -CARD_LIFT.toPx() * steps
                        // الاستدارة تنمو من صفر: الصفحة الساكنة تملأ الحواف بلا قصّ،
                        // والمتحرّكة تصير بطاقة كاملة الاستدارة قبل أن تفارق نصفها.
                        shape = RoundedCornerShape(MaxRadius.group.toPx() * steps)
                        clip = steps > 0.001f
                    }
                    .drawWithContent {
                        drawContent()
                        val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        // نفس [steps] أعلاه: صفرٌ في السكون فلا يُرسم الحدّ الشعريّ حول صفحةٍ
                        // تملأ الشاشة (وكان مقلوبًا فيُرسم الإطار حول الشاشة الساكنة دائمًا).
                        val steps = abs(offset).coerceIn(0f, 1f)
                        if (steps <= 0.001f) return@drawWithContent
                        // حدّ شعريّ داخل القصّ: في السكون لا يُرسم شيء (الصفحة تملأ الحواف)،
                        // وعند الحركة يفصل البطاقة عن الخلفية بلا ظلّ — لغتنا تفصل بالحدّ لا بالارتفاع.
                        val stroke = MaxSize.hairlineBorder.toPx()
                        drawRoundRect(
                            color = outlineColor.copy(
                                alpha = MaxAlpha.borderStrong * steps,
                            ),
                            topLeft = Offset(stroke / 2f, stroke / 2f),
                            size = Size(size.width - stroke, size.height - stroke),
                            cornerRadius = CornerRadius(MaxRadius.group.toPx() * steps),
                            style = Stroke(width = stroke),
                        )
                    }
            ) {
                // الصفحة معتمة بنفسها: الشاشات الأربعة ترسم على `Scaffold` شفّاف (الخلفية كانت
                // تأتي من `NavHost`)، فلولا هذه الطبقة لظهر رمز الخلفية عبر محتواها.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    CompositionLocalProvider(
                        LocalHomeTourTargets provides if (isActive) tourTargets else null,
                    ) {
                        saveableStateHolder.SaveableStateProvider(destination.route) {
                            when (page) {
                                0 -> HomeScreen(navController = navController, isVisible = isActive)
                                1 -> ApplistScreen(navController = navController)
                                2 -> ControlScreen(navController = navController)
                                else -> SettingsScreen(navController = navController)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * خلفية السطح المشترك: لون السطح نفسه، ورمز القسم النشط أثرًا خافتًا في مركزه.
 *
 * تظهر في فواصل البطاقات أثناء السحب وحدها (الصفحة الساكنة تغطّيها كاملة)، ولذلك لا تكلّف
 * تركيبًا متكرّرًا: الرمز يتبع [activeRoute] الذي يتغيّر عند الاستقرار لا مع كل إطار.
 */
@Composable
private fun PrimaryDeckBackdrop(activeRoute: String?) {
    val surface = MaterialTheme.colorScheme.surface
    val active = MaxDestination.PrimaryDestinations.firstOrNull { it.route == activeRoute }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to surface,
                    0.62f to surface,
                    1f to MaterialTheme.colorScheme.surfaceVariant,
                )
            ),
    ) {
        if (active != null) {
            Icon(
                imageVector = active.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = MaxAlpha.toneContainer),
                // ٢٢٤dp = ٨ × رمز الفراغ الكبير؛ مكتوبة بالرمز لا برقم جديد.
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(MaxSpace.xxl * 8),
            )
        }
    }
}

/** كم تنكمش البطاقة الجارة عند ذروة خروجها: ٤٫٥٪ — تُقرأ «بطاقة» ولا تُقرأ «تصغير شاشة». */
private const val CARD_SCALE_DROP = 0.045f

/** رفع البطاقة أثناء الحركة: ٤dp تكفي لتفصلها عن الخلفية بلا أن تُرى قفزة. */
private val CARD_LIFT = MaxSpace.xs

/**
 * نابض الاستقرار الواحد — يملكه السحب والنقر معًا.
 *
 * فلو كان للنقر نابضٌ آخر لَرُئي الفرق في الحسّ نفسه: بطاقةٌ تصل بسرعة مع سحب، وأخرى تصل
 * بمنحنى مختلف مع نقرة. والقيمة مقيّدة الحركة (٠٫٩ بلا ارتداد يُرى) كي لا تتجاوز الحركة
 * [nd.max.ui.design.MaxDuration.deliberate] إحساسًا.
 */
internal val PrimaryDeckSnapSpring = spring<Float>(
    dampingRatio = 0.9f,
    stiffness = Spring.StiffnessMediumLow,
)
