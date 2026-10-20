/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import nd.max.core.platform.ProcessFeed
import nd.max.core.platform.ProcessReadGuard
import nd.max.core.platform.ProcessReadState
import nd.max.core.platform.ProcessReading
import nd.max.core.platform.ProcessSample
import nd.max.core.platform.ProcessScope
import nd.max.core.platform.ProcessSort
import nd.max.core.platform.ProcessWatch
import nd.max.core.platform.ProcessWatchSettings

/** ما وقع بعد إجراء على عملية — نوعه صريح فلا تُخمَّن رسالة من نصّ. */
enum class ProcessActionKind { Killed, KillFailed, Stopped, StopFailed }

data class ProcessActionResult(val kind: ProcessActionKind, val name: String)

/**
 * حالة شاشة مراقب المهام — **عرض ما قُرئ** لا قراءة ثانية.
 *
 * ### ولماذا تغيّر هذا
 *
 * كانت الشاشة تقرأ `top` بنفسها كل دورتين بمعلمين (`limit` · `sort`) ثم تحسب التعداد من القائمة
 * المقصوصة: فتقول «٢٠ عملية» وجهازك فيه ٦٠٠، ويُقرأ الرقم كأنه حجم الجهاز. وهي الآن تشترك في
 * **العيّنة نفسها** مع التراكب ([ProcessWatch])، والتعداد يأتي محسوبًا على الشجرة كلها، والقَصّ
 * والتصفية **في العرض** ([visible]) لا في القراءة: تغيير النطاق أو الترتيب يعيد ترتيب ما وصل
 * بلا أمر جديد إلى الـ`su`.
 *
 * ### والحدّ المعلَن
 *
 * الترتيب بالذاكرة يرتّب **داخل نافذة القراءة** (أعلى [ProcessWatch.MAX_ROWS] بالمعالج) — وهذا
 * مكتوب حيث يراه قارئ الشيفرة وفي وصف الشاشة، لا مخفيًّا.
 */
class ProcessManagerViewModel : ViewModel() {

    private companion object {
        /** معرّف الشاشة عند القارئ المشترك. */
        const val WATCH_OWNER = "process_screen"

        /** فاصل الشاشة ثابت: القائمة معروضة أمام المستخدم، فلا معنى لإبطائها بخيار. */
        const val SCREEN_INTERVAL_SECONDS = 2

        /** أعداد الصفوف المعروضة — ثلاثة لا حقل حرّ، فقائمة بخمس مئة صفّ ليست قائمة تُقرأ. */
        val LIMIT_CHOICES: List<Int> = listOf(10, 20, 50)
    }

    /** آخر عيّنة وصلت — تبدأ «لا جواب» فلا يكذب العرض قبل أوّل قراءة. */
    var sample by mutableStateOf(ProcessSample.noAnswer)
        private set

    /** حكم التتابع — يُعلَن قبل `readState` لأن قيمةه الابتدائية تُقرأ عند الإنشاء. */
    private val reads = ProcessReadGuard()

    /**
     * هل وصلت قراءة جديدة؟ وهل هي حيّة أم محفوظة أم فاشلة؟
     *
     * **ولماذا يلزم حقلٌ ثالث:** `failed = true` في البداية كانت تعني شيئين مختلفين — «لم تُقرأ
     * بعد» و«قُرئت وفشلت» — فكانت الشاشة تُفتح على «لا جواب من الجهاز» قبل أن تُمنح القراءة
     * فرصتها، فيظنّ المستخدم أنّ عطلاً وقع. والحقل يفصل الانتظار عن العطب.
     *
     * **وصُحّح في ٢٠٢٦-١٠-٢١:** لم يكفِ الحقل وحده، لأن `ProcessWatch.snapshot` يبثّ قيمته
     * المحفوظة فور الاشتراك (وكانت `noAnswer`)، فكانت «القيمة المحفوظة» تُعدّ عيّنةً جديدة
     * وتُظهر البطاقة الحمراء ثوانيَ قبل أوّل جدول. الحكم الآن في [ProcessReadGuard] الذي يقيس
     * تتابع العدّاد، ولا يعلن الفشل إلا بعد محاولتين، ولا يمسح جدولًا معروضًا لفشل عابر.
     */
    var readState by mutableStateOf(reads.state)
        private set

    var scope by mutableStateOf(ProcessScope.All)
        private set

    var sort by mutableStateOf(ProcessSort.Cpu)
        private set

    var limit by mutableStateOf(20)
        private set

    var query by mutableStateOf("")
        private set

    var result by mutableStateOf<ProcessActionResult?>(null)
        private set

    /** ما سيُعرض: تصفية النطاق والبحث، ثم الترتيب، ثم القَصّ — كلّها على عيّنة واحدة. */
    val visible: List<ProcessReading>
        get() = ProcessFeed.arrange(sample.readings, scope, query, sort, limit)

    /** الأعداد من العيّنة نفسها: «يعمل» على الشجرة، والتطبيقات/النظام على ما قُرئ منها. */
    val tally get() = sample.tally

    // والأسماء `choose*` لا `set*`: الخصائص `var` تولّد `setScope` تلقائيًّا، ودالّة بالاسم نفسه
    // **تصادم على مستوى الـJVM** (أمسكه المُصرّف: Platform declaration clash) — والاسم هنا يوصف
    // ما يفعله المستخدم (يختار) لا ما يفعله الحقل (يُسنَد).
    fun chooseScope(value: ProcessScope) { scope = value }

    fun chooseSort(value: ProcessSort) { sort = value }

    fun chooseLimit(value: Int) { limit = value.takeIf { it in LIMIT_CHOICES } ?: limit }

    fun search(value: String) { query = value }

    fun nextLimit() {
        val index = LIMIT_CHOICES.indexOf(limit)
        limit = LIMIT_CHOICES[(index + 1) % LIMIT_CHOICES.size]
    }

    fun previousLimit() {
        val index = LIMIT_CHOICES.indexOf(limit)
        limit = LIMIT_CHOICES[(index - 1 + LIMIT_CHOICES.size) % LIMIT_CHOICES.size]
    }

    fun clearResult() { result = null }

    /** تسجيل الشاشة عند القارئ المشترك والاستماع للعيّنات. والتسجيل لا يتكرّر بنداء ثانٍ. */
    fun start(context: Context) {
        // العدّاد يُقرأ **قبل** التسجيل: القيمة المحفوظة في `snapshot` تُبثّ فور الاشتراك،
        // وهي ليست عيّنة جديدة — وهذا بعينه ما كان يُظهر البطاقة الحمراء قبل وصول أوّل جدول.
        reads.begin(ProcessWatch.samples.value)
        ProcessWatch.start(
            context = context,
            owner = WATCH_OWNER,
            scope = viewModelScope,
            settings = { ProcessWatchSettings(limit = limit, intervalSeconds = SCREEN_INTERVAL_SECONDS) }
        )
        // مستمع واحد فقط: `start` صار يُنادى عند كل ON_START، فلا يتراكم مستمع جديد في كل عودة.
        // ويُجمع العدّاد مع العيّنة لأن `_snapshot` يُحدَّث **قبل** `_samples` في القارئ: بمراقبة
        // العيّنة وحدها تُقرأ القيمة الجديدة بعدّادها القديم فتُحسب بثًّا محفوظًا وتُهمَل.
        if (collectJob?.isActive != true) {
            collectJob = viewModelScope.launch {
                combine(ProcessWatch.snapshot, ProcessWatch.samples) { incoming, tick -> incoming to tick }
                    .collect { (incoming, tick) -> accept(incoming, tick) }
            }
        }
    }

    /** يمرّر عيّنة إلى حكم التتابع، ويعرضها فقط إن حكم بأنها أحدث من المعروض وأصدق. */
    private fun accept(incoming: ProcessSample, tick: Int) {
        val show = reads.offer(tick, incoming.failed)
        readState = reads.state
        if (show) sample = incoming
    }

    private var collectJob: Job? = null

    /** إلغاء التسجيل عند مغادرة الشاشة أو إخفائها — فالقارئ لا يعمل لشاشة لا تُرى. */
    fun stop() {
        ProcessWatch.stop(WATCH_OWNER)
        collectJob?.cancel()
        collectJob = null
    }

    /**
     * إعادة قراءة فورية: يُلغى التسجيل ويُعاد، فتُطلب عيّنة جديدة بدل انتظار الحلقة.
     *
     * وهذا معنى زرّ «إعادة المحاولة» في حالة الفشل: القراءة الفاشلة لا تُصلحها `delay`، بل أمر
     * جديد للجهاز.
     */
    fun restart(context: Context) {
        ProcessWatch.stop(WATCH_OWNER)
        start(context)
    }

    fun kill(reading: ProcessReading) = applyResult(
        success = ProcessFeed.kill(reading.pid),
        ok = ProcessActionKind.Killed,
        failed = ProcessActionKind.KillFailed,
        name = reading.label
    )

    fun forceStop(reading: ProcessReading) = applyResult(
        success = ProcessFeed.forceStop(reading.packageName),
        ok = ProcessActionKind.Stopped,
        failed = ProcessActionKind.StopFailed,
        name = reading.label
    )

    private fun applyResult(
        success: Boolean,
        ok: ProcessActionKind,
        failed: ProcessActionKind,
        name: String
    ) {
        result = ProcessActionResult(kind = if (success) ok else failed, name = name)
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
