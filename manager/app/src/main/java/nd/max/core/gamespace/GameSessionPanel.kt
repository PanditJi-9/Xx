/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.core.gamespace

import nd.max.core.platform.ForegroundAppResolver

/**
 * لوحة الألعاب الجانبية — **القواعد وحدها، بلا أندرويد وبلا Compose**.
 *
 * ### ما هذا الملفّ ولماذا ليس في `ui/`
 *
 * سؤال اللوحة الجانبية ليس سؤالًا بصريًّا: «أيّ لعبة تُعرض لها اللوحة الآن؟»، و«متى تكون
 * مطويّة ومتى تُفتح؟»، و«أين تُوضع على حافة شاشة عرضية فيها شرائح نظام؟» — ثلاثة قرارات
 * تُخطئ في ثلاث حالات حقيقية (لا قراءة بعد · اللعبة ليست في المكتبة · تدوير الجهاز)، وكلّها
 * قابلة للقياس بلا شاشة. فوُضعت هنا لتُختبر وحدها كما تُختبر `GameLibrary` و`compareVisibility`،
 * وليبقى ملفّ الخدمة قيادةً لا منطقًا.
 *
 * ### والحدّ المعلن
 *
 * لا كتابة عتاد هنا ولا قراءته: هذا الملفّ يحسب **أيضًا** هل اللوحة تغطّي اللعبة ([coversGame])
 * لتُقال في الواجهة صراحةً، لا ليُبنى محرّك ثانٍ.
 */

/** ما تعرفه اللوحة عن اللعبة التي فوقها. */
sealed interface PanelSubject {

    /** اللعبة الحالية **وهي في المكتبة** — الحالة الوحيدة التي تُعرض فيها اللوحة. */
    data class Tracked(val packageName: String) : PanelSubject

    /**
     * التطبيق الأمامي صالح لكنّه **ليس** لعبة في المكتبة.
     *
     * وفُصل عن [NotAGame] لأنّ الإجراء مختلف: هنا نقول اسم التطبيق، وهناك نقول «لا قراءة بعد».
     */
    data class Other(val packageName: String) : PanelSubject

    /** لا قراءة مقروءة بعد (الخدمة بدأت للتوّ أو المصدر لم يجب). */
    data object Unknown : PanelSubject
}

/**
 * قرار المطابقة: اسم حزمة صالح ⟶ لعبة في المكتبة أو غيره، وغائب/غير صالح ⟶ [PanelSubject.Unknown].
 *
 * **والغائب ليس «غير لعبة»:** `FpsMonitorUtil.getForegroundPackage()` تُعيد `""` حين لا يجيب
 * المصدر، وقراءة `""` كـ«التطبيق الأمامي ليس لعبة» تُطوي اللوحة عن لعبة قائمة — وهو نفس عطب
 * `status_unknown` في طبقة البيانات بلون آخر. فما لم يُقَس لا يُحكم عليه.
 */
fun panelSubject(foreground: String?, library: Set<String>): PanelSubject = when {
    !ForegroundAppResolver.isPackageName(foreground) -> PanelSubject.Unknown
    foreground!! in library -> PanelSubject.Tracked(foreground)
    else -> PanelSubject.Other(foreground)
}

/**
 * التطبيق الأمامي بعد مراجعة **نافذة التركيز**: قراءة `mResumedActivity` وحدها قد تُبقي اللعبة «مقيمة»
 * بعد مغادرتها (مهمّتها حيّة في الخلفية)، فيبقى مقبض اللوحة على الشاشة فوق الشاشة الرئيسية وفوق كل تطبيق.
 * ونافذة التركيز (`mCurrentFocus`/`mFocusedApp`) هي ما يُعرض فعلًا الآن.
 *
 * القاعدة: اسم صالح من نافذة التركيز **يُقدَّم** على المقيم؛ وغير مقروء (أو غير صالح الصيغة) لا يُبطل
 * شيئًا فيبقى المقيم — فما لم يُقَس لا يُحكم به.
 */
fun preferFocusedPackage(resumed: String?, focused: String?): String? =
    if (ForegroundAppResolver.isPackageName(focused)) focused else resumed

/** حالات اللوحة الثلاث. ولا رابعة: كل ما ليس هذين مخفيّ. */
enum class GamePanelMode {
    /** لا شيء على الشاشة — اللعبة كاملة الرؤية. */
    Hidden,

    /**
     * مقبض رقيق ملتصق بالحافة: **لا يغطّي اللعبة** (سِتّةَ عشرَ dp على الحافة) ويفتح اللوحة بلمسة.
     *
     * **ولماذا مقبض لا لوحة مطويّة:** المطلوب «قائمة جانبية مثل REDMAGIC» لا تراكب يغطّي اللعب.
     * والمقبض هو أصغر تمثيل ممكن يبقى **قابلًا للفتح** بلا لوحة نظام ولا اختصار مفاتيح.
     */
    Handle,

    /** اللوحة مفتوحة — **وهي الحالة الوحيدة التي تشغل من مساحة اللعبة**. */
    Open
}

/** حافة الالتصاق. والاتجاه يُقرأ من التخطيط لا من اللغة وحدها. */
enum class PanelSide { Start, End }

/**
 * حالة اللوحة كما تصل الواجهة.
 *
 * @param enabled هل اللوحة مُفعَّلة لهذه اللعبة؟ (تفضيل لكل لعبة، لا مفتاح عامّ واحد)
 * @param openByUser هل فتحها المستخدم في هذه الجلسة؟ (يُصفَّر عند مغادرة اللعبة)
 * @param side الحافة الحالية — تبقى عبر الطيّ فلا تقفز اللوحة من جانب إلى آخر.
 * @param dismissed أُغلقت اللوحة من داخلها في هذه الجلسة فقط. تعود مع اللعبة التالية، ولا تُوقف الخدمة.
 */
data class GamePanelState(
    val mode: GamePanelMode = GamePanelMode.Hidden,
    val subject: PanelSubject = PanelSubject.Unknown,
    val enabled: Boolean = false,
    val openByUser: Boolean = false,
    val side: PanelSide = PanelSide.End,
    val dismissed: Boolean = false
) {
    /** هل يُرسم شيء أصلًا؟ */
    val visible: Boolean get() = mode != GamePanelMode.Hidden

    /**
     * هل تشغل اللوحة من مساحة اللعبة الآن؟ — **يُقال في الواجهة صراحةً** («لا تغطّي اللعبة»
     * مقابل «اللوحة مفتوحة») بدل أن يُدَّعى أن التراكب لا يغطّي أبدًا.
     */
    val coversGame: Boolean get() = mode == GamePanelMode.Open

    /** تُفتح اللوحة فقط للعبة مُفعَّلة ومُتابَعة؛ وما عدا ذلك مقبض أو لا شيء. */
    val canOpen: Boolean get() = enabled && subject is PanelSubject.Tracked
}

/**
 * الدالّة الوحيدة التي تُنتج الحالة من الوقائع — كل تغيير يمرّ من هنا.
 *
 * **والقاعدة التي تحكمها:** مفعَّل + لعبة في المكتبة ⟹ **مقبض على الأقلّ**؛ فمن فعّل اللوحة
 * لا يُترك بلا أثر يفتحه (وهو العطب الذي يجعل «الميزة مفعَّلة ولا أراها»). وما عدا ذلك ⟹ مخفيّ.
 *
 * **والفتح لا يُحفظ:** [openByUser] حالة جلسة، فمغادرة اللعبة تُعيدها إلى المقبض — وإلّا فُتحت
 * اللوحة تلقائيًّا فوق أوّل لعبة تُشغَّل بعد ساعة.
 *
 * **والإغلاق لا يُعطّل اللعبة:** `dismissed` يُخفي اللوحة لبقية الجلسة فقط، و`enabled` يبقى كما هو فتعود عند اللعبة التالية.
 * وإعادته إلى `false` عند مغادرة اللعبة قرار الخدمة لا هذه الدالّة.
 */
fun reconcileGamePanel(
    state: GamePanelState,
    subject: PanelSubject,
    enabled: Boolean,
    openByUser: Boolean,
    dismissed: Boolean = false
): GamePanelState {
    val tracked = subject is PanelSubject.Tracked
    val mode = when {
        !enabled || !tracked || dismissed -> GamePanelMode.Hidden
        openByUser -> GamePanelMode.Open
        else -> GamePanelMode.Handle
    }
    return state.copy(mode = mode, subject = subject, enabled = enabled, openByUser = openByUser, dismissed = dismissed)
}

/** موضع اللوحة بالبكسل بعد تطبيق الشرائح وحدود الشاشة. */
data class PanelPlacement(val x: Int, val y: Int)

/**
 * موضع اللوحة على الحافة: **ملتصقة تمامًا** بها، ومحترمةً شرائح النظام، وباقيةً داخل الشاشة.
 *
 * **والعطب الذي تمنعه:** على شاشة عرضية فيها شريحة حالة وشريحة تنقّل، الالتصاق عند `y = 0`
 * يضع رأس اللوحة **تحت شريحة الحالة** فيصير أوّل زرّ غير قابل للمس. وهذا يُقاس بلا جهاز.
 *
 * والحدّ الأدنى محفوظ: لو ضاقت الشاشة عن اللوحة تُقصّ إلى ما تبقّى بدل أن تُخرج خارجها.
 *
 * **والشرائح تُطبَّق بعد القياس لا قبله:** لو حُسب الارتفاع المتاح أوّلًا ثم طُلب من المحتوى أن
 * يسكن فيه، لبقي ارتفاع اللوحة محسوبًا قبل أن يُعرف. فتُطبَّق `insetTop` على `y` الناتج، وتُحصر
 * النتيجة بين `insetTop` و`screenHeight - insetBottom` — وهذا هو مسار الشاشة الضيّقة الذي كانت
 * `coerceAtLeast(insetTop)` وحدها تُفلته (شاشة ٢٠٠px وشريحة سفلية ٤٨ ⟹ `y = 52`، أي أسفل
 * الشاشة).
 */
fun panelPlacement(
    screenWidth: Int,
    screenHeight: Int,
    panelWidth: Int,
    panelHeight: Int,
    insetTop: Int,
    insetBottom: Int,
    side: PanelSide
): PanelPlacement {
    val width = panelWidth.coerceIn(0, screenWidth.coerceAtLeast(0))
    val x = if (side == PanelSide.End) (screenWidth - width).coerceAtLeast(0) else 0
    val maxY = (screenHeight - insetBottom - panelHeight).coerceAtLeast(insetTop)
    val y = (insetTop + (screenHeight - panelHeight) / 2).coerceIn(insetTop, maxY)
    return PanelPlacement(x = x, y = y)
}

/**
 * هل **لعبة مُفعَّلة أمامية الآن؟** سؤال عن اللحظة لا عن العمر.
 *
 * منذ تكملة ٢٧٢ **لا تُوقف الخدمة نفسها** ما دامت هناك لعبة مُفعَّلة ([panelServiceLifetime])، فهذا
 * السؤال يحدّد شيئين فقط: فاصل الاستطلاع ([panelPollDelayMs])، وهل تعمل قراءة الأداء للّوحة. وقبل
 * اللعبة لا قراءة أداء ولا رسم، فلا يُكلَّف الجهاز شيئًا لا يُرى.
 *
 * **والمقارنة على اسم صالح فقط:** قراءة غير مقروءة ليست دليلًا على أن اللعبة غادرت.
 */
fun panelServiceNeeded(enabledPackages: Set<String>, foreground: String?): Boolean =
    ForegroundAppResolver.isPackageName(foreground) && foreground in enabledPackages

/** فاصل الاستطلاع الحيّ: أسرع ما تكون لعبة مُفعَّلة أمامية، وأبطأ حين لا تكون. */
const val PANEL_ACTIVE_POLL_MS: Long = 1_500L
const val PANEL_IDLE_POLL_MS: Long = 2_000L
const val PANEL_SCREEN_OFF_POLL_MS: Long = 10_000L

/**
 * فاصل الاستطلاع والوحة **مفتوحة**: اللوحة تغطّي اللعبة، فخروجها منها يُطوى خلال ثانية لا بعد ثانيتين.
 * يُدفع ثمنه ما دامت اللوحة مفتوحة فقط، ويعود إلى [PANEL_IDLE_POLL_MS] فور المغادرة.
 */
const val PANEL_OPEN_POLL_MS: Long = 1_000L

/**
 * الإيقاع الخامل **وتوفير البطارية مُفعَّل** — ثماني ثوانٍ.
 *
 * **المقيس الذي أوجبه:** الاستطلاع ليس `stat`؛ كل دورة تسأل النظام عن المقدّمة بصدفة
 * (`FpsMonitorUtil`: `dumpsys activity activities` وهو أثقل تفريغ في النظام). فثانيتان خاملة
 * معناها **١٨٠٠ صدفة في الساعة** بلا لعبة في المقدّمة. وتوفير البطارية يعني: كشف فتح اللعبة
 * يتأخّر ثوانيَ معدودة، وهو ثمن مقبول مقابل إسكات المسار الأثقل.
 */
const val PANEL_IDLE_POWER_SAVE_POLL_MS: Long = 8_000L

/** والوحة مفتوحة في توفير البطارية: ثانيتان — لا تُجمَّد أمام المستخدم. */
const val PANEL_OPEN_POWER_SAVE_POLL_MS: Long = 2_000L

/**
 * كم ينتظر الاستطلاع التالي.
 *
 * **ولماذا يبطؤ حين لا لعبة:** الخدمة تبقى على الجهاز ما دامت هناك لعبة مُفعَّلة، وكل استطلاع يسأل
 * النظام عن المقدّمة بصدفة. فالخمول كل ثانيتين (وهو السقف نفسه الذي يلتزم به `AppMonitor` في قراءته
 * بالصدفة)، والشاشة المطفأة لا تُستطلع أصلًا، لأنه لا لعبة تُلعب والشاشة مطفأة.
 *
 * @param powerSave وضع توفير البطارية في النظام — يُبطئ الخمول وحده، **لا** لحظة اللعب.
 */
fun panelPollDelayMs(
    interactive: Boolean,
    gameInFront: Boolean,
    cockpitOpen: Boolean = false,
    powerSave: Boolean = false,
): Long = when {
    !interactive -> PANEL_SCREEN_OFF_POLL_MS
    cockpitOpen -> if (powerSave) PANEL_OPEN_POWER_SAVE_POLL_MS else PANEL_OPEN_POLL_MS
    gameInFront -> PANEL_ACTIVE_POLL_MS
    powerSave -> PANEL_IDLE_POWER_SAVE_POLL_MS
    else -> PANEL_IDLE_POLL_MS
}

/**
 * كم يمرّ بين سؤاليْن عن التطبيق المقدّمة — **منفصل عن إيقاع الحالة**.
 *
 * **العطب المقيس:** `poll()` كان يسأل النظام عن المقدّمة **في كل دورة**، فصار الإيقاع الخامل
 * (٢ ث) هو إيقاع السؤال نفسه. والجواب لا يلزم إلا لمعرفة «هل فُتحت لعبة؟»؛ وقيمة أخيرة معروفة
 * تكفي بين سؤاليْن لأن كل ما يُبنى عليها (`panelStep`) لا يتغيّر بغير تبديل المقدّمة.
 *
 * والقيم: لحظة اللعب تُبقى على إيقاع الاستطلاع نفسه (‏١٫٥ ث)، والخمول ثماني ثوانٍ، وتوفير
 * البطارية عشرين، والشاشة المطفأة لا سؤال أصلًا (`Long.MAX_VALUE`).
 */
fun panelForegroundProbeMs(
    interactive: Boolean,
    gameInFront: Boolean,
    powerSave: Boolean = false,
): Long = when {
    !interactive -> Long.MAX_VALUE
    gameInFront -> PANEL_ACTIVE_POLL_MS
    powerSave -> 20_000L
    else -> 8_000L
}

/** خيارات معدّل التحديث المعروضة في اللوحة، بالهرتز. */
val PANEL_REFRESH_CHOICES: List<Int> = listOf(60, 90, 120)

/**
 * الحلقة التالية في معدّل التحديث: 60 ⟶ 90 ⟶ 120 ⟶ **الافتراضي** ⟶ 60…
 *
 * **و`null` تعني «بلا فرض»:** الوضع الأخير في الحلقة ليس رقمًا بل **إزالة الفرض** ليختار
 * النظام من جديد. وهذا الفرق هو الذي يمنع تسمية الرجوع إلى الافتراضيّ «٦٠Hz» — وهو ما كان
 * يُثبّت الجهاز على ٦٠ ظنًّا أنّه أرجعه.
 *
 * وما لا يُعرف يُبدأ من أوّله: قيمة حالية غائبة أو غير مدرجة ⟹ أوّل خيار، لا رقم مخترع.
 */
fun nextRefreshRate(current: Int?, choices: List<Int> = PANEL_REFRESH_CHOICES): Int? {
    if (choices.isEmpty()) return null
    val index = choices.indexOf(current)
    if (index < 0) return choices.first()
    return if (index == choices.lastIndex) null else choices[index + 1]
}

/** ما تعرضه اللوحة من أدوات. ولا يُعرض زرّ لا يُعرف ما يفعله. */
enum class GamePanelTile {
    /** معدّل التحديث — يُرسَل إلى مالكه القائم (`RefreshRateReceiver`)، ولا يُكتب من هنا. */
    RefreshRate,

    /** عدم الإزعاج — يُقرأ حاله ويُحرَّر في `App Settings` لأنّ أثره عالميّ ومالكه هناك. */
    DoNotDisturb,

    /** تسجيل الجلسة — الحائز القائم `HudRecorder`. */
    RecordSession,

    /** التقاط الشاشة — **غير مُنفَّذ بعد**، ويُعرض بهذه الحالة لا كزرّ يعمل. */
    Capture
}

/**
 * حالة أداة: تُعرض، أو تُعرض مع سبب، أو تُعرض بلا تنفيذ.
 *
 * **وهذا هو نصّ ADR-08:** المقابض المطفأة تُعلن سببها بدل أن تُخفى أو تدّعي عملًا. وأداة
 * التقاط الشاشة تُعلن «غير متاحة بعد» لأنّ التزام الصمت عنها يعني زرًّا يبدو معطوبًا.
 */
enum class GamePanelTileState { Ready, ControlledElsewhere, NotAvailableYet }

fun gamePanelTileState(tile: GamePanelTile): GamePanelTileState = when (tile) {
    GamePanelTile.RefreshRate -> GamePanelTileState.Ready
    GamePanelTile.RecordSession -> GamePanelTileState.Ready
    GamePanelTile.DoNotDisturb -> GamePanelTileState.ControlledElsewhere
    GamePanelTile.Capture -> GamePanelTileState.NotAvailableYet
}

/** قرار عمر الخدمة بعد كل استطلاع. */
enum class PanelLifetime { Continue, Stop }

/**
 * متى تُوقف الخدمة نفسها. **وهذا القرار تغيّر بطلب المالك صراحةً: «يعمل دائمًا»** (تكملة ٢٧٢).
 *
 * كانت القاعدة السابقة تُوقفها حين تغادر اللعبة، فلا يعود شيء يُشغّلها: فتح اللعبة من خارج التطبيق
 * بعد أول خروج لا يُظهر اللوحة. فالآن:
 *
 * 1. لعبة مُفعَّلة واحدة على الأقل ⟹ **استمرّ**، أمامية كانت أم لا.
 * 2. لا لعبة مُفعَّلة ⟹ **توقّف**: انتهى سبب الخدمة، فلا إشعار دائم بلا سبب.
 *
 * والمستخدم يملك النهاية دائمًا: تعطيل آخر لعبة في الردهة، أو زرّ الإيقاف في الإشعار.
 */
fun panelServiceLifetime(enabledPackages: Set<String>): PanelLifetime =
    if (enabledPackages.isEmpty()) PanelLifetime.Stop else PanelLifetime.Continue

/** ما تقرّره خطوة استطلاع واحدة، مُحسوبًا هنا بلا أندرويد: الحالة، وبقاء الخدمة، وتشغيل القارئ، والفاصل. */
data class PanelStep(
    val state: GamePanelState,
    val lifetime: PanelLifetime,
    val samplerWanted: Boolean,
    val nextDelayMs: Long
)

/**
 * خطوة استطلاع واحدة من الحالة السابقة ومعطيات هذه اللحظة.
 *
 * ### لماذا في هذا الملف
 *
 * الخدمة تقرأ النظام والعتاد، وهذه الدالّة تقرّر **ماذا يحدث بعد القراءة**: متى تبقى الخدمة، ومتى تُطوى اللوحة،
 * ومتى يعمل القارئ، وكم ينتظر الاستطلاع التالي. فصار كل ما يراه المستخدم من اللوحة قابلًا للاختبار بالتشغيل،
 * لا بالقراءة وحدها، وهذا هو سبب وجود هذا الملف أصلًا («القواعد وحدها، بلا أندرويد»).
 *
 * `sameGame` هنا هو السطر نفسه الذي كانت الخدمة تحسبه: الفتح والإغلاق لا ينتقلان إلى لعبة أخرى. وقراءة مفقودة
 * تُصفّر الفتح، وهذا يُخالف ما يصفه اختبار `anUnreadableSample…` (فرق قائم قبل هذه الخطوة، لم يُغيَّر هنا).
 */
fun panelStep(
    previous: GamePanelState,
    enabledPackages: Set<String>,
    side: PanelSide,
    interactive: Boolean,
    foreground: String?,
    library: Set<String>,
    powerSave: Boolean = false,
): PanelStep {
    val subject = if (interactive) panelSubject(foreground, library) else PanelSubject.Unknown
    val sameGame = subject is PanelSubject.Tracked && previous.subject == subject
    val enabled = subject is PanelSubject.Tracked && subject.packageName in enabledPackages
    val next = reconcileGamePanel(
        state = previous,
        subject = subject,
        enabled = enabled,
        openByUser = sameGame && previous.openByUser,
        dismissed = sameGame && previous.dismissed
    ).copy(side = side)
    return PanelStep(
        state = next,
        lifetime = panelServiceLifetime(enabledPackages),
        samplerWanted = next.mode != GamePanelMode.Hidden,
        nextDelayMs = panelPollDelayMs(
            interactive = interactive,
            gameInFront = panelServiceNeeded(enabledPackages, foreground),
            cockpitOpen = next.mode == GamePanelMode.Open,
            powerSave = powerSave,
        )
    )
}

/** الإغلاق من داخل اللوحة: تُطوى لبقية **هذه** الجلسة، واللعبة تبقى مُفعَّلة. */
fun dismissGamePanel(state: GamePanelState): GamePanelState =
    state.copy(mode = GamePanelMode.Hidden, openByUser = false, dismissed = true)

/** عمر المكتبة المقروءة قبل إعادة القراءة. عشر دقائق تكفي، فالتثبيت الجديد يُرى في وقت معقول. */
const val PANEL_LIBRARY_TTL_MS: Long = 10 * 60_000L

/**
 * هل تُعاد قراءة المكتبة؟ نعم حين تتغيّر الألعاب المُفعَّلة، أو حين تتقادم القراءة، لأن الخدمة تعيش طويلًا الآن،
 * وتثبيت لعبة جديدة بعد بدء الخدمة لا يُرى إلا بقراءة جديدة.
 */
fun panelLibraryStale(loadedFor: Set<String>, enabled: Set<String>, ageMs: Long): Boolean =
    enabled != loadedFor || ageMs >= PANEL_LIBRARY_TTL_MS
