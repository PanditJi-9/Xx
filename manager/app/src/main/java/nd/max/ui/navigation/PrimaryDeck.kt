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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import nd.max.ui.component.rememberMaxHaptics
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
 * من الصفر وتخرج القديمة. وهنا [HorizontalPager] يملك المسافة والسرعة والإفلات، والصفحة
 * الجارة جاهزة قبل أن يلمسها المستخدم.
 *
 * **الحركة انزلاقٌ أفقيّ خالص (طلب المالك ٢٠٢٦-١٠-٢١: «سيّئة وغير سلسة»).** كان كل إطار سحب
 * يدفع ثمن تحويلٍ لكل صفحة (تصغير + استدارة + قصّ + حدّ شعريّ مرسوم) فوق خلفيةٍ إضافية مرسومة
 * خلف صفحاتٍ معتمة أصلًا — عملٌ يُنفَق على الرسم في أثقل شاشات التطبيق. وحذفُه يترك للـPager
 * الحركة الأصلية التي لا يوجد أخفّ منها.
 *
 * **والبار السفلي يتبع الـPager لا المسار.** المسار يُكتب عند **استقرار** الصفحة، فكان المؤشّر
 * يتأخّر عن السحب بطول النابض كلّه، وكانت نقرةٌ أثناء الاستقرار تُلغى لأن الاستقرار القديم يعيد
 * المسارَ إلى صفحته. المالك الآن هو [PagerState]: النقر يحرّكه فورًا، والبار يقرأ `targetPage`،
 * والمسار يلحق به عند الاستقرار ([onSettledPage]).
 *
 * @param activeRoute المسار الرئيسيّ المعروض فعلًا؛ يحدّد الصفحة النشطة (لبثّها الحيّ وللجولة).
 *   `null` أو مسارٌ فرعيّ يعني أن السطح **خلف** شاشة فرعية (لا صفحة نشطة، فيتوقّف البثّ).
 * @param gesturesEnabled مفتاح تفضيل السحب؛ إطفاؤه لا يمنع الانتقال بالنقر أو بالجولة.
 * @param tourTargets سجلّ مراسي الجولة، يُعلَّق للصفحة النشطة وحدها كي لا تعلّق صفحةٌ مجاورة
 *   مُمرِّر قائمتها مكان الصفحة التي يراها المستخدم.
 * @param saveableStateHolder حافظ حالة الصفحات في الجذر: يبقى حيًّا حين يخرج سطح الأقسام من
 *   التركيب، فتعود كل قائمة إلى موضعها لا إلى أعلاها.
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

    // **الإقلاع أخفّ، ثم السحب بلا تركيبٍ في منتصفه.** أول إطار يركّب الصفحة المعروضة وحدها (كان
    // `beyondViewportPageCount = 1` من اللحظة الأولى فيركّب معها الجارة كاملةً قبل أن يظهر شيء —
    // جزءٌ من الشاشة السوداء عند الفتح). ثم تُجهَّز بقية الصفحات **صفحةً صفحةً** في أوقات الفراغ.
    //
    // **لماذا لا تكفي الجارة وحدها:** كلما عبر السحب منتصف صفحةٍ صارت جارتها الجديدة بعيدةً عن
    // الصفحة الحالية، فيركّبها الـPager **أثناء حركة الاستقرار نفسها** — وهذا هو التقطيع في الانتقال
    // الثاني (قائمة التطبيقات والتحكّم أثقل ما في التطبيق تركيبًا). وبعد التجهيز لا تركيب أثناء
    // أي سحب. وكل خطوة تنتظر **سكون الـPager** أولًا فلا تُركَّب صفحةٌ تحت إصبع المستخدم.
    var warmedPages by remember(pagerState) { mutableIntStateOf(0) }
    LaunchedEffect(pagerState) {
        delay(PrimaryDeckWarmupMillis)
        val target = MaxDestination.PrimaryDestinations.size - 1
        while (warmedPages < target) {
            snapshotFlow { pagerState.isScrollInProgress }.first { scrolling -> !scrolling }
            warmedPages += 1
            delay(PrimaryDeckWarmStepMillis)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = warmedPages,
            userScrollEnabled = gesturesEnabled,
            flingBehavior = fling,
            key = { index -> MaxDestination.PrimaryDestinations[index].route },
        ) { page ->
            val destination = MaxDestination.PrimaryDestinations[page]
            val isActive = destination.route == activeRoute

            // الصفحة معتمة بنفسها: الشاشات الأربعة ترسم على `Scaffold` شفّاف (الخلفية كانت
            // تأتي من `NavHost`).
            // **وللصفحة طبقة رسمها الخاصّة (`graphicsLayer`):** الـPager يحرّك الصفحات بتغيير موضعها كل
            // إطار. بلا طبقةٍ لكل صفحة يُعاد تسجيل رسم ما فيها (بطاقات وخطوط ومنحنيات الرئيسية) مع كل
            // إطار سحب؛ ومعها تُنقَل الطبقة المسجَّلة كما هي، ولا يُعاد الرسم إلا حين يتغيّر محتواها فعلًا.
            // بلا تعتيمٍ ولا قصّ ولا ذاكرة رسمٍ إضافية: طبقةٌ فارغة الإعدادات.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { }
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

/** مهلة تجهيز الصفحة الجارة بعد أول إطار: تكفي لأن تُرسم الصفحة الأولى قبل أن تُركَّب الثانية. */
private const val PrimaryDeckWarmupMillis = 450L

/** الفاصل بين تجهيز صفحةٍ وأخرى بعد الأولى: يترك للرئيسية أن تُحمَّل بياناتها بلا منافسة. */
private const val PrimaryDeckWarmStepMillis = 900L

/**
 * نابض الاستقرار الواحد — يملكه السحب والنقر معًا.
 *
 * فلو كان للنقر نابضٌ آخر لَرُئي الفرق في الحسّ نفسه: صفحةٌ تصل بسرعة مع سحب، وأخرى تصل
 * بمنحنى مختلف مع نقرة. ومؤشّر البار السفلي يستعمله هو أيضًا فيصل مع الصفحة لا بعدها.
 * بلا ارتداد (`NoBouncy`) وبصلابةٍ متوسطة: تستقرّ في نحو ٢٥٠–٣٠٠ms بدل نابضٍ بطيءٍ كان
 * يُقرأ «تأخّرًا» — وهي دون [nd.max.ui.design.MaxDuration.deliberate].
 */
internal val PrimaryDeckSnapSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
)
