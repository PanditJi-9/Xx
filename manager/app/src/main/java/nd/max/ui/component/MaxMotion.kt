/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * MaxManager motion system.
 * Motion is purposeful: short entrance choreography, responsive state changes,
 * and restrained springs. Keep motion subtle enough that hardware data remains
 * the visual priority.
 *
 * الطبقة الراقية (Premium) فوق هذا الأساس، وكلها تحت سقف `MaxDuration.deliberate` (360ms)
 * ولا شيء فيها يدور إلى الأبد (DESIGN.md §Motion):
 *  • [MaxEase] — منحنيات «مُشدَّدة» (emphasized): دخولٌ سريع وهبوطٌ ناعم، وخروجٌ يتسارع.
 *  • [maxEnter] — كشفٌ متدرّج لكتل الشاشة عند أول ظهور، يُرتَّب بموضع الكتلة على الشاشة لا برقم.
 *  • [rememberMaxHaptics] — اهتزازٌ لمسيّ واحد لكل فعل (تبديل · خطوة · اختيار) بدل نداءات متفرّقة.
 */
package nd.max.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import nd.max.ui.design.MaxDuration
import nd.max.ui.design.MaxSpace
import nd.max.ui.theme.ThemeStudio

object MaxMotion {
    const val fast = 160
    const val standard = 260
    const val emphasis = 420

    /** Snappier spring for small controls (switches, pills, chevrons). */
    val controlSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val contentSpring = spring<IntSize>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Spring with a touch of overshoot for needle-like value changes. */
    val needleSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun enter(delayMillis: Int = 0): EnterTransition =
        fadeIn(
            animationSpec = tween(
                durationMillis = standard,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = standard,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            ),
            initialOffsetY = { it / 14 }
        )

    val exit: ExitTransition =
        fadeOut(animationSpec = tween(fast, easing = FastOutSlowInEasing)) +
            slideOutVertically(
                animationSpec = tween(fast, easing = FastOutSlowInEasing),
                targetOffsetY = { -it / 18 }
            )
}

/**
 * هل أوقف المستخدم حركات النظام؟ (`Settings.Global.ANIMATOR_DURATION_SCALE == 0`).
 *
 * يُقرأ مرّة عند التركيب لا في كل إطار: هو تفضيل نظام يُقلَّب في الإعدادات، ومن غيّره يعيد
 * فتح الشاشة فيُعاد التركيب. والقراءة مغلّفة بـ`runCatching` لأن مزوّد الإعدادات قد يحجب
 * المفتاح على بعض البنيات — وحجبُه ليس «الحركة مطلوبة»، بل «لا نعرف»، والمجهول هنا يُحلّ إلى
 * السلوك المعتاد لا إلى إسقاط حركة لم يطلبها أحد.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) != 0f
        }.getOrDefault(true)
    }
}

/**
 * هل تأثيرات التمرير مطلوبة الآن؟ = **تفضيل المستخدم** ∧ **حركة النظام مفتوحة**.
 *
 * وهي البوابة الواحدة التي تمرّ بها [MaxReveal] و[Modifier.maxEnter] و[Modifier.maxItemMotion]:
 * مفتاح واحد في شاشة التخصيص يوقفها في كل موضع مربوط، فلا يبقى مؤثّر شارد يعمل بعد «أوقفتُها».
 *
 * **وقراءتها واحدة عند التركيب** كقراءة تفضيل النظام نفسها: من قلّب المفتاح يعيد فتح الشاشة
 * فيُعاد التركيب — وهي نفس المعادلة القائمة في `rememberAnimationsEnabled`، فلا مساران
 * لقراءة تفضيل واحد.
 */
@Composable
fun rememberScrollEffectsEnabled(): Boolean {
    val context = LocalContext.current
    val systemAllows = rememberAnimationsEnabled()
    val userAllows = remember(context) {
        runCatching {
            ThemeStudio.scrollEffects(
                context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
            )
        }.getOrDefault(true)
    }
    return systemAllows && userAllows
}

/** اسم ملفّ التفضيلات الذي يكتب فيه التطبيق خيارات الإعدادات (والثيم) — نصًّا واحدًا. */
private const val SETTINGS_PREFS = "settings"

/**
 * A restrained reveal used for dashboard blocks and settings groups.
 *
 * **ويحترم إيقاف الحركة في النظام:** من أوقف الحركات (`ANIMATOR_DURATION_SCALE = 0`) يُعرض له
 * المحتوى كما هو بلا fade ولا إزاحة — لا «حركة أسرع». وهذا شرط خطة المستوى (`DESIGN.md`:
 * "احترم Reduce Motion")، وكان غير مطبَّق: الـAPI كان يُنادي `animateTo` بلا سؤال.
 */
@Composable
fun MaxReveal(
    visible: Boolean = true,
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!rememberScrollEffectsEnabled()) {
        if (visible) content()
        return
    }
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            entrance.animateTo(
                1f,
                tween(MaxMotion.standard, delayMillis.coerceIn(0, 160), FastOutSlowInEasing)
            )
        } else {
            entrance.snapTo(0f)
        }
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.graphicsLayer {
            alpha = entrance.value
            translationY = (1f - entrance.value) * 12.dp.toPx()
        },
        enter = EnterTransition.None,
        exit = MaxMotion.exit
    ) {
        content()
    }
}


/** Responsive press feedback shared by interactive MaxManager surfaces. */
@Composable
fun Modifier.maxPressMotion(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.98f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = MaxMotion.controlSpring,
        label = "pressScale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Tiny settle motion for values that change without changing layout. */
fun Modifier.maxValueMotion(scale: Float = 1f, alpha: Float = 1f): Modifier =
    graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }


// ───────────────────────────── الطبقة الراقية ─────────────────────────────

/**
 * منحنيات التسارع «المُشدَّدة» (Material 3 emphasized).
 *
 * `FastOutSlowIn` يبدأ وينتهي بالتوازن نفسه، فيبدو الدخول «ميكانيكيًّا». هذه المنحنيات تفرّق بين
 * **ما يظهر** (يندفع ثم يهبط ببطء ← يشعر المستخدم أن العنصر «وصل») و**ما يغادر** (يتردّد لحظة ثم
 * ينصرف بسرعة ← لا يُعطّل النظر). والفرق محسوس عند ٢٤٠ms رغم أنه لا يُرى كحركة مستقلّة.
 */
object MaxEase {
    /** لكل ما **يدخل**: انطلاقة سريعة وهبوط طويل ناعم. */
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** لكل ما **يغادر**: بداية متمهّلة ثم انصراف سريع. */
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** لما يتحرّك **داخل الشاشة نفسها** (مؤشّر منزلق، تغيّر حجم): يأخذ وقته في النهاية. */
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/**
 * أقصى تأخير يناله بلوك في أسفل الشاشة المرئية.
 *
 * ١١٠ + [MaxDuration.standard] (٢٤٠) = ٣٥٠ms، أي **تحت** سقف [MaxDuration.deliberate] (٣٦٠):
 * الحركة كلها تنتهي قبل أن تتحوّل إلى زخرفة.
 */
private const val MaxEnterStaggerMillis = 110

/**
 * كشف متدرّج للكتلة عند أول ظهورها: شفافية + صعود قصير، **مرّة واحدة** لكل كتلة.
 *
 * **لماذا بالموضع لا بالرقم.** الكتل هنا تُكتب في ٤٠ شاشة بأيدٍ مختلفة؛ ترقيمها يدويًّا (١، ٢، ٣…)
 * يعني أن كل من يضيف قسمًا يعدّل أرقام غيره، وأن قسمًا يُحذف يترك فجوة في الإيقاع. فالتأخير هنا
 * **يُشتقّ من ارتفاع الكتلة على الشاشة**: الأعلى أولًا والأسفل بعده، فيتولّد تتابعٌ طبيعي مهما
 * كان ترتيب الكتابة. وما كان **تحت حافة الشاشة** يظهر فورًا بلا حركة — فلا يُمثَّل عرضٌ لا يراه أحد،
 * وعند التمرير تجد الكتلة نفسها كاملة.
 *
 * **لا تُعاد.** حالة «دخلت» محفوظة بـ`rememberSaveable`: تدوير الجهاز، أو عودة صفّ إلى قائمة
 * كسولة بعد خروجه منها، لا يُعيدان الحركة. وتُقرأ **بلا تتبّع** ([Snapshot.withoutReadObservation])
 * لأن قراءتها في التركيب كانت ستُعيد تركيب القسم كله عند انتهاء الحركة — ثمن لا يُشترى بحركة.
 *
 * **ويحترم إيقاف الحركة** ([rememberAnimationsEnabled]): تُرسم الكتلة كاملة بلا أي معدِّل إضافي.
 * وكل ما فيها يجري في مرحلة الرسم (`graphicsLayer`)، فلا إعادة تركيب ولا إعادة قياس في أي إطار.
 *
 * @param travel المسافة التي تصعدها الكتلة. افتراضيًّا [MaxSpace.md]: كافية لتُفهَم، أقلّ من أن تُلاحَظ.
 */
@Composable
fun Modifier.maxEnter(travel: Dp = MaxSpace.md): Modifier {
    val animationsEnabled = rememberScrollEffectsEnabled()
    val entered = rememberSaveable { mutableStateOf(false) }
    val progress = remember {
        val alreadyEntered = Snapshot.withoutReadObservation { entered.value }
        Animatable(if (animationsEnabled && !alreadyEntered) 0f else 1f)
    }
    val placedAtY = remember { CompletableDeferred<Float>() }
    val screenHeightPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            val y = placedAtY.await()
            if (y >= screenHeightPx) {
                // تحت الحافة: لا مُشاهد لها الآن، فلا حركة.
                progress.snapTo(1f)
            } else {
                val staggerMillis = (y / screenHeightPx).coerceIn(0f, 1f) * MaxEnterStaggerMillis
                delay(staggerMillis.toLong())
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(MaxDuration.standard, easing = MaxEase.EmphasizedDecelerate)
                )
            }
        }
        entered.value = true
    }

    return if (!animationsEnabled) {
        this
    } else {
        this
            // قبل `graphicsLayer` عن قصد: الموضع المقيس هو موضع الاستقرار، لا موضعٌ أزاحته حركتنا نفسها.
            .onGloballyPositioned { coordinates ->
                if (!placedAtY.isCompleted) placedAtY.complete(coordinates.positionInWindow().y)
            }
            .graphicsLayer {
                val p = progress.value
                alpha = p
                translationY = (1f - p) * travel.toPx()
            }
    }
}

/**
 * اهتزاز لمسيّ **واحد** لكل فعل، بمعنًى واحد في كل التطبيق.
 *
 * كان الاهتزاز موزّعًا بنداءات خام (`TextHandleMove` في موضعين، `LongPress` في موضع) فلا يعرف من
 * يضيف مفتاحًا أيّ نوع يختار. هنا الفعل هو الاسم، والنوع التفصيلي يُقرَّر في مكان واحد:
 * لو احتاج نوعٌ إلى تبديل، يتغيّر هنا لا في كل شاشة.
 *
 * النظام نفسه هو من يحترم تفضيل المستخدم «ردّ الفعل اللمسي»؛ لا نتجاوزه.
 */
@Stable
class MaxHaptics internal constructor(private val feedback: HapticFeedback) {
    /** مفتاح تبديل: نبضة «تشغيل» مختلفة عن «إيقاف»، فتُعرف الحالة الجديدة باللمس وحده. */
    fun toggle(on: Boolean) {
        feedback.performHapticFeedback(
            if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
        )
    }

    /** تغيّر اختيار بين بدائل (تبويب · شريط تنقّل · خيار حصريّ). */
    fun select() {
        feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /** عبور خطوة منفصلة على شريط تمرير. */
    fun step() {
        feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }
}

@Composable
fun rememberMaxHaptics(): MaxHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { MaxHaptics(feedback) }
}

/**
 * حركة صفّ في قائمة كسولة: ينزلق إلى موضعه الجديد، ويظهر/يغيب بنعومة عند التصفية والبحث.
 *
 * بلا هذا يقفز كل صفّ تحت العنصر المحذوف إلى مكانه في إطار واحد، فيفقد المستخدم موضع عينه
 * وسط قائمة طويلة. يُستعمل فقط حيث للصفّ **مفتاح ثابت** وحيث الصفّ **عنصر جذر واحد**: صفٌّ يتبعه
 * فاصل خارج جذره ينزلق وحده ويترك الفاصل يقفز — وهذا أسوأ من بلا حركة.
 *
 * ولا يُوضَع على قوائم تتبدّل ترتيبًا كل ثانية (العمليات، السجلّ الحيّ): هناك الحركة المستمرّة
 * ضجيج يُخفي القراءة، وهو مناقض لقاعدة «الحركة تشرح تغيّرًا لا تُزيّن».
 */
@Composable
fun LazyItemScope.maxItemMotion(): Modifier =
    if (rememberScrollEffectsEnabled()) {
        Modifier.animateItem(
            fadeInSpec = tween(MaxDuration.standard, easing = MaxEase.EmphasizedDecelerate),
            placementSpec = spring<IntOffset>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            fadeOutSpec = tween(MaxDuration.quick, easing = MaxEase.EmphasizedAccelerate),
        )
    } else {
        Modifier
    }
