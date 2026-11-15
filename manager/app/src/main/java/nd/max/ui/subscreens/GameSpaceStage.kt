/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import nd.max.core.gamespace.GameApp
import nd.max.core.gamespace.PanelCpuLoad
import nd.max.core.platform.HudDeepReaders
import nd.max.core.platform.HudLive
import nd.max.core.platform.HudSnapshot
import nd.max.ui.component.lobbyIntro

/*
 * المسرح — «Home» لوحةٌ ثابتة الأبعاد داخل مساحة العرض المتاحة.
 *
 * اللقطة مرسومة على لوحة ١٦٧٢×٩٤١. فبدل أن تُعاد هندسة الشاشة على كل جهاز (فتخرج الأقسام واللوحات
 * بقياسات غير المرسومة)، نبني اللوحة **مرّة واحدة بوحدات اللقطة نفسها** ثم نكبّرها أو نصغّرها
 * بنسبةٍ واحدة تُلائم المساحة المتاحة، وتتوسّط وتترك هامشًا حيث لا تتّسع بالنسبة نفسها:
 *
 *  - كل `dp` و`sp` داخل اللوحة يساوي **بكسلًا واحدًا من اللقطة**. بلغةٍ أخرى: `Modifier.at(1236, 87, 410, 229)`
 *    هو مستطيل لوحة الأداء في الصورة بإحداثياتها حرفيًّا.
 *  - التكبير بإبدال `LocalDensity` (لا بـ`graphicsLayer.scale`)، فيبقى النص والخطوط حادّة بأي نسبة،
 *    وتبقى اللمسات تُقاس بالبكسل الفعلي فتقع على ما يُرى.
 *  - الاتّجاه مثبّت LTR لهذه اللوحة وحدها: الشريط يسارًا واللوحات يمينًا كما في اللقطة، وقلبه في
 *    العربية يعكس الهندسة كلها.
 *  - حجم الخط يتبع تكبير النظام حتى ١٫١٥ فقط؛ فوقه يتجاوز النص حدود الألواح الثابتة.
 *
 * ### الحياة فوق اللوحة (جولة ٢٠٢٦-١١-١٥)
 *
 * أجواء الشاشة (سماء ليليّة وغبار) صارت في `LobbyAtmosphere` خلف المُصفِّح كلّه، وساعة الحركة الواحدة
 * وتقدّم الدخول يصلان عبر `LocalStageClock`/`LocalStageIntro`: فكل جزء ينساب إلى مكانه بتأخّر نسبيّ بعد
 * غطاء الدخول، والبطل يطفو، واللمعة تعبر زرّ التشغيل، والسحب الأفقيّ على البطل يبدّل اللعبة.
 *
 * ### الثمن، صراحةً
 *
 * اللمسات تتبع نسبة اللوحة: على شاشة ٩١٥×٤١٢dp النسبة ≈٠٫٤٤ فيُرى عنصر الشريط (٦٥ وحدة) بنحو ٢٨dp.
 * وهذا ثمن «اللوحة الثابتة» الذي طُلب؛ ولذلك أزرار اللوبي العلويّة (الاستكشاف، المفضّلة، السجلّ،
 * الإغلاق) تُرسم بمقاسها الفعليّ بإعادة `LocalDensity` الأصلي داخلها، وتُوضَع فوق اللوحة عند إحداثياتها.
 *
 * ### ما لا يُدّعى (ADR-07)
 *
 * لا فنّ لعبة: اللقطة تحمل فنّ PUBG وهو ليس ملكنا ولا يأتي مع `PackageManager`. فالبطل يعرض **أيقونة
 * التطبيق الحقيقيّة**، وبطاقات الرفّ تأخذ الأيقونة فوق خلفية مموّهة منها. والأرقام تُقرأ فعلًا:
 * المعالج من `PanelCpuLoad`، والرسوميات من `HudDeepReaders.gpuLoadPercent`، والحرارة من `socTempC`
 * (أو حرارة البطارية عند غيابها)، والإطارات من `HudLive`. وما لم يُقرأ يبقى «—» وحلقةً بلا قوس.
 */

internal const val StageDesignWidth = 1672
internal const val StageDesignHeight = 941

/** أقصى تكبير للخط يتحمّله تخطيط اللوحة الثابتة. */
private const val MaxFontScale = 1.15f

/**
 * أدنى نسبة يُقرأ عندها نصّ اللوحة بلا عناء: تحتها (هاتف عرضيّ قصير الارتفاع) يُرفع حجم الخط وحده
 * دون الهندسة، فيبقى النصّ مقروءًا والمقاسات المرسومة كما هي. فوقها لا رفع.
 */
private const val LegibleScale = 0.60f
private const val MaxFontBoost = 1.28f
private const val MaxFontCombined = 1.35f

/** أقصى عرض إضافيّ (بوحدات اللقطة) تتمدّد به اللوحة على الشاشات الأعرض من ١٦:٩ بدل الهامش الفارغ. */
private const val MaxStageExtra = 760

/** أقصى ما تأخذه لوحات اليمين من العرض الإضافيّ لتتّسع للنصّ الأكبر. */
private const val MaxRightGrow = 130

/**
 * العرض الإضافيّ للوحة الحاليّة بوحدات اللقطة. الارتفاع ثابت ٩٤١، فإن كانت الشاشة أعرض من ١٦:٩
 * (أغلب الهواتف: ٢٠:٩) امتدّت اللوحة بدل أن يُترك على جانبيها شريطان فارغان: الشريط يبقى على اليسار،
 * ولوحات الأداء على اليمين، ويتّسع البطل والرفّ بينهما.
 */
internal val LocalStageExtra = compositionLocalOf { 0 }

internal class StageSpace(val scale: Float, val extra: Int)

internal fun stageSpace(width: Dp, height: Dp): StageSpace {
    val scale = minOf(width.value / StageDesignWidth, height.value / StageDesignHeight)
    val extra = (width.value / scale - StageDesignWidth).toInt().coerceIn(0, MaxStageExtra)
    return StageSpace(scale = scale, extra = extra)
}

/** حصّة اللوحات اليمنى من العرض الإضافيّ: تتّسع قليلًا ليبقى فيها النصّ الأكبر بلا اقتطاع. */
internal fun stageRightGrow(extra: Int): Int = (extra * 3 / 10).coerceAtMost(MaxRightGrow)

/** مقياس الخط داخل اللوحة: تكبير النظام (حتى ١٫١٥) مضروبًا برفع القراءة على الشاشات الصغيرة، بسقف واحد. */
internal fun stageFontScale(system: Float, scale: Float): Float {
    val boost = (LegibleScale / scale).coerceIn(1f, MaxFontBoost)
    return minOf(minOf(system, MaxFontScale) * boost, MaxFontCombined)
}
private const val FpsLiveMs = 5_000L

/** موضع عنصرٍ على اللوحة بإحداثيات اللقطة (بكسل اللقطة = ١dp داخل اللوحة). */
internal fun Modifier.at(x: Int, y: Int, w: Int, h: Int): Modifier =
    this.offset(x.dp, y.dp).size(w.dp, h.dp)

/** القراءات الحيّة في قراءة واحدة، فالمؤقّت واحد. */
private data class StageReadings(
    val cpu: Int? = null,
    val gpu: Int? = null,
    val tempC: Float? = null,
    /** وقت القراءة؛ يتغيّر كل دورة فيُعاد تقدير «حيّ/قديم» للإطارات حتى بلا عيّنة جديدة. */
    val atMs: Long = 0L,
)

/**
 * الإطارات اللحظيّة: الرقم الأخير ومنحنى آخر العيّنات، و[live] صحيح إن كانت العيّنة أحدث من خمس ثوانٍ.
 * بلا عيّنة للعبة المختارة: لا رقم ولا منحنى.
 */
@Immutable
internal data class StageFps(
    val value: Int?,
    val history: List<Float>,
    val live: Boolean,
)

/** كل ما تعرضه اللوحة، في كائن واحد بدل عشرين وسيطًا تتناقل بين الدوال. */
internal class StageData(
    val games: List<GameApp>,
    val selected: GameApp?,
    val selectedIndex: Int,
    val tagline: String?,
    val cpu: Int?,
    val gpu: Int?,
    val heat: Float?,
    val fps: StageFps,
    val batteryPercent: Int?,
    val charging: Boolean,
    val panelEnabled: Boolean,
    val readinessPercent: Int,
    val statusTitle: String,
    val statusDetail: String,
    val clock: String,
    val date: String,
    /** الإطلاق جارٍ: الزرّ يعرض مؤشّر تقدّم ويرفض نقرة ثانية. */
    val launching: Boolean = false,
)

internal class StageActions(
    val onSelect: (String) -> Unit,
    val onLaunch: () -> Unit,
    val onOptions: () -> Unit,
    val onHome: () -> Unit,
    val onOpenSection: (LobbySection) -> Unit,
    val onOpenLibrary: () -> Unit,
    val onOpenSessions: () -> Unit,
)

/**
 * الإطارات اللحظيّة: عيّنة تخصّ اللعبة المختارة وحدها، وإلا لا رقم ولا منحنى —
 * الشاشة قبل الإطلاق لا تدّعي قياسًا لم يقع.
 */
private fun stageFps(hud: HudSnapshot, packageName: String?, nowMs: Long): StageFps {
    val mine = hud.reading != null && hud.sampledAtMs > 0L &&
        (packageName == null || hud.owner == packageName)
    if (!mine) return StageFps(value = null, history = emptyList(), live = false)
    return StageFps(
        value = hud.reading?.frames?.roundToInt(),
        history = hud.framesHistory,
        live = nowMs - hud.sampledAtMs <= FpsLiveMs
    )
}

@Composable
internal fun GameSpaceStage(
    games: List<GameApp>,
    selected: GameApp?,
    readinessPercent: Int,
    statusTitle: String,
    statusDetail: String,
    panelEnabled: Boolean,
    batteryPercent: Int?,
    batteryTempC: Float?,
    charging: Boolean,
    onSelect: (String) -> Unit,
    onLaunch: () -> Unit,
    onOptions: () -> Unit,
    onHome: () -> Unit,
    onOpenSection: (LobbySection) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSessions: () -> Unit,
    modifier: Modifier = Modifier,
    /** سطر تحت اسم اللعبة (آخر لعب)؛ `null` يُسقطه، لا يُكتب مكانه شيء مخترع. */
    tagline: String? = null,
    clock: String = "",
    date: String = "",
    /** الإطلاق جارٍ الآن (يمنع نقرتين ويُظهر مؤشّر التقدّم في الزرّ). */
    launching: Boolean = false,
    /** أزرار اللوبي العلويّة (استكشاف/مفضّلة/سجلّ/إغلاق…) — تُوضع على اللوحة بمقاسها الفعليّ. */
    topActions: @Composable () -> Unit = {},
) {
    // القراءات الحيّة: نفس القارئين اللذين يغذّيان مراقب الجهاز، فلا مقياس ثانٍ.
    // مؤقّت واحد للثلاثة معًا: حلقة واحدة تُقاس مرّة كل ثانيتين، لا ثلاث حلقات تتصارع.
    val readings by produceState(initialValue = StageReadings()) {
        while (true) {
            value = StageReadings(
                cpu = PanelCpuLoad.sample(),
                gpu = HudDeepReaders.gpuLoadPercent(),
                tempC = HudDeepReaders.socTempC(),
                atMs = System.currentTimeMillis()
            )
            delay(2_000L)
        }
    }
    val hud by HudLive.snapshot.collectAsState()
    val selectedIndex = remember(games, selected) {
        games.indexOfFirst { it.packageName == selected?.packageName }.coerceAtLeast(0)
    }
    val data = StageData(
        games = games,
        selected = selected,
        selectedIndex = selectedIndex,
        tagline = tagline,
        cpu = readings.cpu,
        gpu = readings.gpu,
        heat = readings.tempC ?: batteryTempC,
        fps = stageFps(hud, selected?.packageName, readings.atMs),
        batteryPercent = batteryPercent,
        charging = charging,
        panelEnabled = panelEnabled,
        readinessPercent = readinessPercent,
        statusTitle = statusTitle,
        statusDetail = statusDetail,
        clock = clock,
        date = date,
        launching = launching
    )
    val actions = StageActions(
        onSelect = onSelect,
        onLaunch = onLaunch,
        onOptions = onOptions,
        onHome = onHome,
        onOpenSection = onOpenSection,
        onOpenLibrary = onOpenLibrary,
        onOpenSessions = onOpenSessions
    )

    Box(modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .displayCutoutPadding(),
            contentAlignment = Alignment.Center
        ) {
            // التخطيط العموديّ البديل: عمود واحد بالكتل نفسها بدل لوحة أفقيّة تُصغَّر حتى لا تُقرأ.
            if (maxHeight > maxWidth) {
                StagePortrait(data = data, actions = actions, topActions = topActions)
                return@BoxWithConstraints
            }
            val real = LocalDensity.current
            // نسبة واحدة للعرض والارتفاع: اللوحة لا تتمطّط أبدًا. والعرض الزائد عن ١٦:٩ تملؤه اللوحة نفسها.
            val space = stageSpace(maxWidth, maxHeight)
            val design = Density(
                density = real.density * space.scale,
                fontScale = stageFontScale(real.fontScale, space.scale)
            )
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Ltr,
                LocalDensity provides design,
                LocalStageExtra provides space.extra
            ) {
                StageCanvas(data = data, actions = actions, realDensity = real, topActions = topActions)
            }
        }
    }
}

/**
 * اللوحة الثابتة نفسها: صندوق ١٦٧٢×٩٤١ وحدةً، وكل منطقة عند إحداثيات اللقطة. الشريط الجانبيّ ليس هنا:
 * هو ثابت فوق كل الصفحات (`GameSpaceRailLayer`) فلا يتحرّك مع السحب بين الأقسام.
 */
@Composable
private fun StageCanvas(
    data: StageData,
    actions: StageActions,
    realDensity: Density,
    topActions: @Composable () -> Unit,
) {
    val intro = LocalStageIntro.current
    val extra = LocalStageExtra.current
    Box(Modifier.requiredSize((StageDesignWidth + extra).dp, StageDesignHeight.dp)) {
        StageHeroBackdrop(
            game = data.selected,
            modifier = Modifier
                .at(168, 0, 1504 + extra, 560)
                .lobbyIntro(intro, scaleFrom = 1.04f)
        )
        StageHero(data = data, actions = actions)
        StageStatusBar(
            data = data,
            onProfile = { actions.onOpenSection(LobbySection.Game) },
            realDensity = realDensity,
            topActions = topActions
        )
        StageBoard(
            data = data,
            onOpen = actions.onOptions
        )
        StageShelf(data = data, actions = actions)
        StageBase(
            title = data.statusTitle,
            detail = data.statusDetail,
            percent = data.readinessPercent,
            onOpen = actions.onOpenSessions,
            modifier = Modifier
                .at(195, 850, 1451 + extra, 62)
                .lobbyIntro(intro, fromBelow = 40.dp, start = 0.40f)
        )
    }
}
