/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * حالة شاشة العناية بالتخزين: **قياس، تأشير، حذف، وإعادة قياس — ثم تحليل وإدارة**.
 *
 * وثلاث قواعد تحكم قسم التنظيف، وكلها من عقد §6:
 *
 * 1. **القياس لا يُقدَّر:** كل صفّ يحمل `Long?` — والـ`null` تعني «فشل القياس» وتُعرض
 *    «لم يُقس». ولا صفر يُكتب مكانها.
 * 2. **التأشير حالة جلسة:** يُبنى من `StorageCareModel.safeSelection()` في كل فتح للشاشة،
 *    ولا يُخزَّن على الجهاز — فلا تعود فئة أطفأها المستخدم مرّةً معلَّمةً صامتة بعد يوم.
 * 3. **بعد التنظيف يُعاد القياس، ولا يُصدَّق الرقم المفترض:** المساحة المُزالة يجمعها المحرّك
 *    من فرق «قبل/بعد» لكل فئة، وهذا الرقم هو ما يُعرض — لا حجم ما طُلب حذفه.
 *
 * **وثلاثة عقود جديدة تحكم سرعة الفتح (إصلاح «تستمر في البحث»):**
 *
 * 1. **الفتح لا ينتظر القياس.** لا وجود لحالة حجب للشاشة: الصفوف تُرسم فورًا، كل فئة
 *    حالةً «يُقاس…» حتى تصل نتيجتها وحدها. القياس الكامل خلف الكواليس، والتصفّح حرّ.
 * 2. **النشر تدريجي لا دفعة واحدة.** كل فئة تُقاس في مهمّة مستقلة، وتُنشر في **لحظة
 *    انتهائها** لا عند انتهاء أخواتها. ورمز جولة (`roundToken`) يمنع نتيجة جولة قديمة من
 *    الوصول إلى الحالة بعد إعادة القياس.
 * 3. **ذاكرة جلسة بعمر عشر دقائق** (`StorageCareCache`): الفتح الثاني يعرض أرقام الجولة
 *    الأولى فورًا بلا إعادة مسح، والقياسات تُبطل بعد أي تنظيف فعلي أو تغيّر طبقة الجذر.
 *    و«إعادة القياس» تفرض جولة جديدة دائمًا.
 *
 * **والقسمان الآخران يملكان حالتهما هنا لا في الشاشة:**
 * - **التحليل** (`scan`): نتيجة `StorageUtil.scan` تُحفظ في الحالة فتبقى عبر التنقّل داخل
 *   الشاشة، والتقدّم يُنشر أثناء المسح — لا شاشة تجمّد رقمها صفرًا حتى ينتهي. وللمسح
 *   **إيقاف صريح** (`cancelScan`): نتيجة نصفية لا تُعرض على أنها كاملة، والنتيجة السابقة
 *   تبقى معروضة حتى تصل الجديدة.
 * - **التطبيقات** (`loadApps`): القراءة الثقيلة على IO، والصلاحية تُفحص عند كل عودة إلى
 *   الشاشة (`refreshApps`): من منح الصلاحية من الإعدادات ثم رجع يجب أن يرى الأرقام
 *   الدقيقة في الجولة نفسها — لا بعد إعادة فتح التطبيق.
 *
 * **والصلاحية تُقرأ سلبيًّا:** `cachedRootGranted()` لا تستدعي `su`، فلا تظهر نافذة صلاحية
 * لمجرد فتح شاشة تنظيف. ومن لا جذر له تُخفى عنه فئات الجذر الأربع وتُعرض له جملة النطاق —
 * لا زرّ يفشل صامتًا ولا قائمة فئات فارغة.
 *
 * **والأعمال الثقيلة لا تتزاحم:** الحذف لا يجري أثناء قياس أو مسح (فرق «قبل/بعد» بلا معنى
 * وسط تغيير آخر)، والمسح وإعادة القياس قراءتان تجريان معًا عند حاجة المستخدم — والقرار
 * في `canClean`/`canScan`/`canMeasureAgain` هنا لا في الشاشة.
 */
package nd.max.ui.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.core.privilege.PrivilegeManager
import nd.max.ui.util.AppStorageEntry
import nd.max.ui.util.AppStorageReader
import nd.max.ui.util.CareTargetList
import nd.max.ui.util.CleanMeasurement
import nd.max.ui.util.CleanMeasurementSet
import nd.max.ui.util.CleanOutcome
import nd.max.ui.util.FolderScan
import nd.max.ui.util.MountInfo
import nd.max.ui.util.StorageCareCache
import nd.max.ui.util.StorageCareCategory
import nd.max.ui.util.StorageCareEngine
import nd.max.ui.util.StorageCareModel
import nd.max.ui.util.StorageScanResult
import nd.max.ui.util.StorageUtil
import javax.inject.Inject

/**
 * ورقة فئة: تُفتح فورًا، ويُقرأ سردها في الخلفية («يُقرأ…» في الورقة لا تجميد للشاشة).
 *
 * @param targets سرد «ما سيُحذف بالضبط» — أو `null` حتى يصل، و`failed` داخله تعني «تعذّرت
 *        القراءة» لا «لا شيء».
 * @param outcome نتيجة «نظّف الآن» لهذه الفئة وحدها (لا يشاركها فيها مجموع الدفعة).
 */
data class CategorySheetState(
    val category: StorageCareCategory,
    val loading: Boolean = true,
    val targets: CareTargetList? = null,
    val cleaning: Boolean = false,
    val outcome: CleanOutcome? = null,
)

/** ورقة تطبيق: تفصيل الرقم (حزمة/بيانات/كاش) وأفعاله — والكاش وحده فعل مستقل عن البيانات. */
data class AppSheetState(
    val app: AppStorageEntry,
    val rootAvailable: Boolean,
    val busy: Boolean = false,
    /** الفعل الجاري الآن: مسح بيانات أم مسح كاش؟ يحدّد نصّ الانتظار وقرار الصحّة. */
    val clearingData: Boolean = false,
    val outcome: CleanOutcome? = null,
)

data class StorageCareUiState(
    /** نقطة التحميل الأم (`/data`) — تُقرأ على IO، لا في تركيب الشاشة على الخيط الرئيسي. */
    val mount: MountInfo? = null,
    /** القياس الأول أو إعادة قياس جارية. */
    val measuring: Boolean = false,
    /** صحيح بعد وصول أول نتيجة (جولة الآن أو ذاكرة صالحة). */
    val measuredOnce: Boolean = false,
    /** الفئات التي يقاس نصفها الآن — صفّها يعرض «يُقاس…» بدقّة، فئةً فئة. */
    val pendingCategories: Set<StorageCareCategory> = emptySet(),
    val measurements: CleanMeasurementSet = CleanMeasurementSet(),
    val selection: Set<StorageCareCategory> = StorageCareModel.safeSelection(),
    val cleaning: Boolean = false,
    /** مجاميع آخر عملية تنظيف: ما نُفِّذ، وما تحقّق، وما حُرِّر فعلًا. */
    val lastOutcomes: List<CleanOutcome> = emptyList(),
    /** مسح التحليل جارٍ، وعدد العناصر المقروءة حتى اللحظة. */
    val scanning: Boolean = false,
    val scanProgress: Int = 0,
    /** آخر نتيجة مسح — تبقى ما بقي الإطار في الذاكرة، فالعودة إلى الشاشة لا تُعيد دقائق. */
    val scanResult: StorageScanResult? = null,
    /** قراءة التطبيقات جارية. */
    val appsLoading: Boolean = false,
    /** هل صلاحية «الوصول إلى بيانات الاستخدام» ممنوحة؟ */
    val usageAccess: Boolean = false,
    /** أكبر التطبيقات — تُبنى مرّة عند الفتح، وتُعاد عند تغيّر الصلاحية. */
    val apps: List<AppStorageEntry> = emptyList(),
    /** وقت جولة القياس إن كانت الأرقام المعروضة من الذاكرة لا من جولة الآن. */
    val measurementsFromCacheAtMs: Long? = null,
    /** ورقة الفئة المفتوحة («ما سيُحذف بالضبط») — أو `null` إن كانت مغلقة. */
    val categorySheet: CategorySheetState? = null,
    /** ورقة التطبيق المفتوحة (تفصيل الرقم وأفعال الأجزاء) — أو `null`. */
    val appSheet: AppSheetState? = null,
    /** أكبر المجلدات من آخر مسح — الجواب على «أين ذهبت المساحة». */
    val folderScan: FolderScan? = null,
    /** قياس المجلدات فشل (لا `du` · لا قراءة) — يفرّق عن «لا مجلدات». */
    val foldersFailed: Boolean = false,
) {
    val totalBytes: Long get() = StorageCareModel.total(selection, measurements).bytes

    val totalIsPartial: Boolean get() = StorageCareModel.total(selection, measurements).hasUnmeasured

    /** ما حُرِّر في آخر عملية: مجموع فروق «قبل/بعد» المقيسة. */
    val freedBytes: Long? get() = lastOutcomes.takeIf { it.isNotEmpty() }?.sumOf { it.freedBytes }

    /** الحذف لا يجري أثناء قياس (الفرق «قبل/بعد» بلا معنى) ولا أثناء مسح أو حذف آخر. */
    val canClean: Boolean get() = totalBytes > 0L && !measuring && !cleaning && !scanning

    /** إعادة القياس قراءة فقط — تُسمح أثناء المسح، وتُمنع أثناء قياس أو حذف جارٍ. */
    val canMeasureAgain: Boolean get() = !measuring && !cleaning

    /** المسح قراءة فقط — يُسمح به أثناء القياس، ويُمنع أثناء حذف جارٍ أو مسح آخر. */
    val canScan: Boolean get() = !scanning && !cleaning
}

@HiltViewModel
class StorageCareViewModel @Inject constructor(
    application: Application,
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    /** قفل تحديثات الحالة: جولات القياس تنشر من عدة خيوط IO في آنٍ واحد. */
    private val lock = Any()

    /** رقم جولة القياس — نتيجة جولة سبقت إعادة القياس لا تصل إلى الحالة. */
    private var roundToken = 0

    /**
     * رمز فتح الورقة — نتيجة سرد فئة سبقت إغلاقًا أو تبديل فرقة لا تصل إلى الحالة.
     *
     * منفصل عن [roundToken] عمدًا: دوران مستقلّان (القياس في الخلفية، والورقة عند الطلب)،
     * وخلطهما كان يجعل إغلاق ورقة يُبطل جولة قياس جارية.
     */
    private var sheetToken = 0

    /** مقبض المسح الجاري — الإيقاف يمرّ من هنا. */
    private var scanJob: Job? = null

    private val _state = MutableStateFlow(StorageCareUiState())
    val state: StateFlow<StorageCareUiState> = _state.asStateFlow()

    init {
        // سعة القرص تحتاج صدفة (`stat -f` و`df -i`) — تُقرأ على IO. وكانت تُقرأ في تركيب
        // الشاشة على الخيط الرئيسي فيتجمّد أول إطار بمقدار عمر الصدفة، وهذا جزء من «البطء».
        viewModelScope.launch {
            val mount = withContext(Dispatchers.IO) {
                StorageUtil.readMounts().firstOrNull { it.path == "/data" }
            }
            mutate { it.copy(mount = mount) }
        }
    }

    /**
     * الجذر متاح؟
     *
     * ويُقرأ عند كل نداء لا مرّة عند التركيب: من رجع من شاشة الصلاحيات ونجح طلبه يجب أن يرى
     * الفئات كلها في الجولة التالية — لا بعد إعادة فتح التطبيق.
     */
    private fun rootAvailable(): Boolean = PrivilegeManager.cachedRootGranted()

    /** قراءة-ثم-كتابة للحالة تحت قفل واحد — تنشر جولات القياس من خيوط متعدّدة. */
    private fun mutate(transform: (StorageCareUiState) -> StorageCareUiState) {
        synchronized(lock) { _state.value = transform(_state.value) }
    }

    /**
     * يقيس كل فئة **مرئية** عند الطبقة الحالية — من الذاكرة إن كانت جولة سابقة ما تزال
     * صالحة، وإلا فجولة جديدة تنشر كل فئة في لحظة انتهائها.
     *
     * @param force يتجاهل الذاكرة ويفرض قياسًا جديدًا («إعادة القياس» وبعد التنظيف).
     */
    fun measureAll(force: Boolean = false) {
        val current = _state.value
        if (current.cleaning) return
        val root = rootAvailable()
        if (!force) {
            val cached = StorageCareCache.load()
            if (cached != null &&
                cached.rootAvailable == root &&
                StorageCareCache.isFresh(System.currentTimeMillis(), cached)
            ) {
                mutate { state ->
                    state.copy(
                        measuring = false,
                        measuredOnce = true,
                        pendingCategories = emptySet(),
                        measurements = cached.measurements,
                        measurementsFromCacheAtMs = cached.atMs,
                        selection = visibleSelection(state.selection, root),
                    )
                }
                return
            }
        }
        startMeasurementRound(root)
    }

    /** يفتح جولة قياس: مهمّة لكل فئة، وكل نتيجة تُنشر وحدها عند وصولها. */
    private fun startMeasurementRound(root: Boolean) {
        val token = synchronized(lock) {
            roundToken += 1
            roundToken
        }
        val visible = StorageCareCategory.entries.filter { StorageCareModel.visibleAt(root, it) }
        mutate { state ->
            state.copy(
                measuring = true,
                pendingCategories = visible.toSet(),
                measurements = CleanMeasurementSet(),
                measurementsFromCacheAtMs = null,
                // والمختار يُصفّى إلى المرئي: فئة اختفت بحذف الجذر لا تظلّ في المجموع.
                selection = visibleSelection(state.selection, root),
            )
        }
        viewModelScope.launch {
            val rounds = visible.map { category ->
                launch(Dispatchers.IO) {
                    val row = StorageCareEngine.measure(context, category, root)
                    // نتيجة جولة أُعيدت لا تُنشر: الرمز يُفحص داخل القفل نفسه.
                    mutate { state ->
                        if (token != roundToken) {
                            state
                        } else {
                            state.copy(
                                measurements = CleanMeasurementSet(
                                    state.measurements.rows
                                        .filterNot { it.category == category } + row
                                ),
                                pendingCategories = state.pendingCategories - category,
                                measuredOnce = true,
                            )
                        }
                    }
                }
            }
            rounds.joinAll()
            // والذاكرة تُكتب مرّة واحدة بعد اكتمال الجولة — لا نصف قياس.
            var completed: CleanMeasurementSet? = null
            mutate { state ->
                if (token != roundToken) {
                    state
                } else {
                    completed = state.measurements
                    state.copy(measuring = false, pendingCategories = emptySet(), measuredOnce = true)
                }
            }
            completed?.let { measurements ->
                StorageCareCache.store(
                    StorageCareCache.Entry(
                        atMs = System.currentTimeMillis(),
                        rootAvailable = root,
                        measurements = measurements,
                    )
                )
            }
        }
    }

    /** التأشير يُصفّى إلى الفئات المرئية عند الطبقة الحالية، ويسقط إلى الآمن إن خلا. */
    private fun visibleSelection(
        current: Set<StorageCareCategory>,
        root: Boolean,
    ): Set<StorageCareCategory> {
        val visible = StorageCareCategory.entries.filter { StorageCareModel.visibleAt(root, it) }
        return current.filter { it in visible }.toSet()
            .ifEmpty { StorageCareModel.safeSelection().filter { it in visible }.toSet() }
    }

    /** تأشير/إلغاء تأشير فئة. */
    fun toggle(category: StorageCareCategory) {
        mutate { state ->
            state.copy(
                selection = if (category in state.selection) {
                    state.selection - category
                } else {
                    state.selection + category
                }
            )
        }
    }

    /** إعادة الاختيار إلى المجموعة الآمنة (الكاش وبقايا المُزالة والصور المصغّرة والسجلات). */
    fun selectSafe() {
        mutate { it.copy(selection = StorageCareModel.safeSelection()) }
    }

    /**
     * ينظّف المختار فئةً فئة، ثم **يعيد القياس**.
     *
     * والتسلسل لا التوازي: كل حذف يقرأ بعده، وأمران متوازيان على الشجرة نفسها يتنازعان على
     * المجلدات التي يتغيّر محتواها — والقراءة بعد الحذف تفقد معناها إن كان حذف آخر لم ينتهِ.
     */
    fun clean() {
        val current = _state.value
        if (!current.canClean) return
        mutate { it.copy(cleaning = true, lastOutcomes = emptyList()) }
        viewModelScope.launch {
            val root = rootAvailable()
            val outcomes = withContext(Dispatchers.IO) {
                current.selection.map { category ->
                    val before: CleanMeasurement? = current.measurements.of(category)
                    StorageCareEngine.clean(context, category, before, root)
                }
            }
            // الذاكرة تُبطَل قبل إعادة القياس: ما فيها يصف شجرة لم تعد قائمة.
            StorageCareCache.invalidate()
            mutate { it.copy(cleaning = false, lastOutcomes = outcomes) }
            // وإعادة القياس بعد الحذف: الصفوف تعرض ما بقي، لا ما كان.
            measureAll(force = true)
        }
    }

    /**
     * يفتح ورقة فئة: الشاشة تعرض «ما سيُحذف بالضبط» قبل أي زرّ حذف — لا عنوانًا يلخّص.
     *
     * والسرد يُقرأ في الخلفية، والورقة تُفتح فورًا بحالة «يُقرأ…» — نفس عقد الفتح الذي
     * لا ينتظر القياس. ورمزٌ لكل فتح: نتيجة فئة أُغلقت ورقتها (أو استُبدلت بأخرى) لا تصل.
     */
    fun openCategorySheet(category: StorageCareCategory) {
        val token = synchronized(lock) {
            sheetToken += 1
            sheetToken
        }
        mutate { it.copy(categorySheet = CategorySheetState(category = category)) }
        viewModelScope.launch {
            val root = rootAvailable()
            val targets = withContext(Dispatchers.IO) {
                StorageCareEngine.targets(context, category, root)
            }
            mutate { state ->
                if (token != sheetToken) {
                    state
                } else {
                    state.copy(
                        categorySheet = state.categorySheet
                            ?.takeIf { it.category == category }
                            ?.copy(loading = false, targets = targets),
                    )
                }
            }
        }
    }

    /** يغلق ورقة الفئة — والسرد الجاري سفره يُهمل برمزه. */
    fun closeCategorySheet() {
        synchronized(lock) { sheetToken += 1 }
        mutate { it.copy(categorySheet = null) }
    }

    /**
     * ينظّف **الفئة المفتوحة وحدها** («نظّف الآن» في ورقتها) — لا الدفعة المختارة.
     *
     * ثم يعيد قياس الفئة وحدها وسردها: الورقة تعرض ما بقي لا ما كان. وإعادة القياس
     * الجزئية أسرع من جولة كاملة، ولا تُبطل أرقام الفئات الأخرى التي لم تُمسّ.
     */
    fun cleanCategorySheetNow() {
        val sheet = _state.value.categorySheet ?: return
        if (!_state.value.canMeasureAgain) return
        val category = sheet.category
        mutate { state ->
            state.copy(
                cleaning = true,
                categorySheet = state.categorySheet?.copy(cleaning = true, outcome = null),
            )
        }
        viewModelScope.launch {
            val root = rootAvailable()
            val before = _state.value.measurements.of(category)
            val outcome = withContext(Dispatchers.IO) {
                StorageCareEngine.clean(context, category, before, root)
            }
            StorageCareCache.invalidate()
            val after = withContext(Dispatchers.IO) {
                StorageCareEngine.measure(context, category, root)
            }
            mutate { state ->
                state.copy(
                    cleaning = false,
                    measurements = CleanMeasurementSet(
                        state.measurements.rows.filterNot { it.category == category } + after,
                    ),
                    categorySheet = state.categorySheet
                        ?.takeIf { it.category == category }
                        ?.copy(cleaning = false, outcome = outcome),
                )
            }
            // والسرد يُعاد قراءته ليصف ما بقي: قائمة كاشٍ مُسحت لا تبقى كما كانت.
            reloadCategorySheet(category)
        }
    }

    /** إعادة قراءة سرد الورقة المفتوحة (بعد التنظيف) تحت رمزها — لا تُزاحم ورقة أحدث. */
    private suspend fun reloadCategorySheet(category: StorageCareCategory) {
        val token = synchronized(lock) {
            sheetToken += 1
            sheetToken
        }
        val root = rootAvailable()
        val targets = withContext(Dispatchers.IO) {
            StorageCareEngine.targets(context, category, root)
        }
        mutate { state ->
            if (token != sheetToken) {
                state
            } else {
                state.copy(
                    categorySheet = state.categorySheet
                        ?.takeIf { it.category == category }
                        ?.copy(targets = targets),
                )
            }
        }
    }

    /** يفتح ورقة تطبيق: التفصيل (حزمة/بيانات/كاش) وأفعال الأجزاء — بلا استعلام ثانٍ. */
    fun openAppSheet(app: AppStorageEntry) {
        mutate { it.copy(appSheet = AppSheetState(app = app, rootAvailable = rootAvailable())) }
    }

    fun closeAppSheet() {
        mutate { it.copy(appSheet = null) }
    }

    /** «مسح الكاش» وحدها — البيانات لا تُمسّ، وهذا الفرق الذي طلبه المالك بنصّه. */
    fun clearAppCacheNow() = clearAppPart(clearData = false)

    /** «مسح البيانات» — يشمل الكاش، ويقف خلف تأكيد صريح في الشاشة. */
    fun clearAppDataNow() = clearAppPart(clearData = true)

    /**
     * ينفّذ فعل الجزء (كاش أو بيانات) ثم **يعيد قراءة الصفّ**، فالشاشة تقول ما بقي لا ما كان.
     *
     * و`cleaning` المشترك يُرفع هنا أيضًا: الحذف لا يتوازى مع قياس ولا مع حذف آخر — نفس
     * حرس الدفعة، فلا مساران يكتبان الشجرة في آنٍ واحد.
     */
    private fun clearAppPart(clearData: Boolean) {
        val sheet = _state.value.appSheet ?: return
        if (!_state.value.canMeasureAgain) return
        val packageName = sheet.app.packageName
        mutate { state ->
            state.copy(
                cleaning = true,
                appSheet = state.appSheet?.copy(busy = true, clearingData = clearData, outcome = null),
            )
        }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                if (clearData) {
                    StorageCareEngine.clearAppData(packageName)
                } else {
                    StorageCareEngine.clearAppCache(packageName)
                }
            }
            val fresh = withContext(Dispatchers.IO) {
                AppStorageReader.readOne(context, packageName)
            }
            mutate { state ->
                // الورقة تُحدَّث فقط إن كانت ما تزال ورقة **هذا** التطبيق: من أغلقها أو فتح
                // غيرها أثناء العمل لا تُفتح عليه نتيجة قديمة.
                val current = state.appSheet
                val sheet = if (current?.app?.packageName == packageName) {
                    current.copy(busy = false, outcome = outcome, app = fresh ?: current.app)
                } else {
                    current
                }
                state.copy(
                    cleaning = false,
                    apps = state.apps.map { entry ->
                        if (entry.packageName == packageName) fresh ?: entry else entry
                    },
                    appSheet = sheet,
                )
            }
        }
    }

    /**
     * يمسح التخزين المشترك لأجل قسم التحليل.
     *
     * **والنطاق هو جذر التخزين المشترك وحده** (`Environment.getExternalStorageDirectory`) —
     * نفس ما تمسحه شاشة تفصيل التخزين: سؤالان عن شجرة واحدة يجيبان من مسح واحد المنطق.
     * والتقدّم يُنشر أثناء الجريان (العدد المقروء حتى اللحظة) لا بعد النهاية.
     * والنتيجة السابقة تبقى معروضة حتى تصل الجديدة — الإيقاف لا يمسح ما قيس سابقًا.
     */
    fun scan() {
        val current = _state.value
        if (!current.canScan) return
        scanJob?.cancel()
        mutate { it.copy(scanning = true, scanProgress = 0, folderScan = null, foldersFailed = false) }
        scanJob = viewModelScope.launch {
            val roots = listOf(Environment.getExternalStorageDirectory())
            // ومجلدات الدرجة الأولى تُقاس بالتوازي مع المسح النمطي: كلاهما قراءة صرفة،
            // و`du` النظامية تمرّ على الشجرة مرّة أخرى — فالمتوازيتان أقصر من المتتاليتين.
            val foldersJob = async(Dispatchers.IO) { StorageUtil.topFolders(roots.first()) }
            val result = withContext(Dispatchers.IO) {
                StorageUtil.scan(roots) { scanned ->
                    mutate { state ->
                        if (state.scanning) state.copy(scanProgress = scanned) else state
                    }
                }
            }
            val folders = foldersJob.await()
            mutate {
                it.copy(
                    scanning = false,
                    scanResult = result,
                    folderScan = folders,
                    foldersFailed = folders == null,
                )
            }
        }
    }

    /** إيقاف المسح: الحالات تُطوى، والنتيجة السابقة تبقى — ولا نصف نتيجة يُعرض كاملة. */
    fun cancelScan() {
        if (!_state.value.scanning) return
        scanJob?.cancel()
        scanJob = null
        mutate { it.copy(scanning = false, scanProgress = 0) }
    }

    /**
     * يقرأ أكبر التطبيقات. **ثقيل** (استعلام لكل حزمة) ⇒ IO، ولا يُزاحم عملية أخرى.
     *
     * والصلاحية تُفحص داخل القراءة نفسها، فالنتيجة تحمل درجتها: دقيقة أو «حجم الحزمة فقط».
     */
    fun loadApps() {
        if (_state.value.appsLoading || _state.value.cleaning) return
        mutate { it.copy(appsLoading = true) }
        viewModelScope.launch {
            val granted = withContext(Dispatchers.IO) { AppStorageReader.hasUsageAccess(context) }
            val rows = withContext(Dispatchers.IO) { AppStorageReader.read(context) }
            mutate { it.copy(appsLoading = false, usageAccess = granted, apps = rows) }
        }
    }

    /**
     * يُنادى عند كل عودة إلى الشاشة: يقرأ الصلاحية، ويُعيد قراءة القائمة إن تغيّرت —
     * ومن رجع من نافذة المنح يرى الأرقام الدقيقة فورًا.
     */
    fun refreshApps() {
        val granted = AppStorageReader.hasUsageAccess(context)
        val current = _state.value
        if (granted != current.usageAccess || current.apps.isEmpty()) {
            mutate { it.copy(usageAccess = granted) }
            loadApps()
        }
    }
}
