/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * **ذاكرة عدّادات الصحّة على القرص** — ملفّ نصّيّ صغير داخل `filesDir` (نمط `StatsStore`:
 * كتابة ذرّيّة بمؤقّت وإعادة تسمية، وتحمّل ملفّ تالف بلا استثناء).
 *
 * **والعدّ يقع عند المصدر الوحيد** — [HardwareControlArbiter] ينادي [record] مرّةً لكل نتيجة
 * كتابة تحكّم، والمجال يُشتقّ من المفتاح ([HealthModel.domainOf]) والتصنيف من حقول النتيجة
 * ([HealthModel.classify]). فلا تُعدّ قراءةٌ ولا تشخيص، ولا تُعدّ نتيجةٌ مرّتين.
 *
 * **ولماذا لا تُصفَّر الأعداد بعد الإرسال:** صفّ الخادم واحد لكل تثبيت (§١٠.٥) ويُحدَّث
 * كلّ يوم، فأعداد الجهاز **تراكميّة** بإرسالها هي. ولا تُصفَّر إلا بالسحب (§١٠.٣ البند ٣:
 * إطفاء المفتاح يمحو الحساب المحلي كاملًا) أو بمسح بيانات التطبيق.
 *
 * **وحدّها المعلن:** الكتابة على القرص مؤجَّلة (٢ ثانية) ولو ماتت العملية قبلها يضيع ما لم
 * يُكتب — والأعداد التراكميّة تُعوّض الفاقد من الجلسة التالية، ولا يُبنى على رقمٍ واحد حكمٌ
 * مصيريّ. وفي المقابل لا يمسّ هذا الملفّ شبكة ولا حزمة ولا سجلًّا: أرقام محليّة صرفة.
 */
object HealthLedger {

    private const val FILE_NAME = "stats_health.state"
    private const val PERSIST_DEBOUNCE_MS = 2_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    private var counts: MutableMap<String, HealthModel.Counters> = mutableMapOf()
    private var file: File? = null
    private var persistJob: Job? = null

    /**
     * يُنادى مرّةً من [nd.max.core.access.RemoteAccess] عند الإقلاع (مع شقيقه `CommunityStats`).
     *
     * وإن عُدّت نتائج قبل هذا النداء (إقلاع مبكّر لكاتب) فالأعداد في الذاكرة — و[attach]
     * **يدمجها** مع ذاكرة القرص بدل أن يستبدل بها، فلا يضيع عدّ جرى ولا يُحسب مرّتين.
     */
    fun attach(context: Context) {
        if (file != null) return
        val target = File(context.applicationContext.filesDir, FILE_NAME)
        synchronized(lock) {
            file = target
            val merged = decode(target).toMutableMap()
            counts.forEach { (domain, counters) ->
                merged[domain] = (merged[domain] ?: HealthModel.Counters()).merge(counters)
            }
            counts = merged
        }
    }

    /**
     * عدّ نتيجة كتابة واحدة. `blocked` تُسقط النتيجة: حجبٌ بالأسبقية أو قفلٌ يدويّ **ليس
     * تنفيذًا** — لا مؤكَّدًا ولا متراجعًا ولا مجهولًا — فإدخاله في العدّ كان سيقلب معنى
     * «صحّة التحكّم» إلى «ضغط المستخدم على الأزرار».
     */
    fun record(
        key: String,
        applied: Boolean,
        verified: Boolean,
        blocked: Boolean,
        rollbackAttempted: Boolean,
    ) {
        if (blocked) return
        val domain = HealthModel.domainOf(key)
        val outcome = HealthModel.classify(applied, verified, rollbackAttempted)
        synchronized(lock) {
            counts[domain] = (counts[domain] ?: HealthModel.Counters()).plus(outcome)
        }
        schedulePersist()
    }

    /** الرسالة كما ستُرسل — نفس دالّة الترميز التي يقرؤها حقلُ الإشارة والشاشة معًا. */
    fun json(): String = synchronized(lock) { HealthModel.encode(counts) }

    /** السحب ومسح البيانات: يُمحى الحساب المحلي كاملًا — لا عدّ يبقى لجهازٍ سحب موافقته. */
    fun clear() {
        synchronized(lock) {
            persistJob?.cancel()
            persistJob = null
            counts = mutableMapOf()
            runCatching { file?.delete() }
        }
    }

    private fun schedulePersist() {
        if (file == null) return
        synchronized(lock) {
            persistJob?.cancel()
            persistJob = scope.launch {
                delay(PERSIST_DEBOUNCE_MS)
                persist()
            }
        }
    }

    private fun persist() {
        val target = file ?: return
        val snapshot = synchronized(lock) { counts.toMap() }
        val text = buildString {
            HealthModel.DOMAINS.forEach { domain ->
                val counters = snapshot[domain] ?: return@forEach
                if (counters.total <= 0) return@forEach
                append(domain).append('=')
                append(counters.confirmed).append(',')
                append(counters.rolledBack).append(',')
                append(counters.unknown).append('\n')
            }
        }
        runCatching {
            target.parentFile?.mkdirs()
            val temporary = File(target.parentFile, "${target.name}.tmp")
            temporary.writeText(text, Charsets.UTF_8)
            if (temporary.renameTo(target)) return@runCatching
            if (!target.delete()) return@runCatching
            temporary.renameTo(target)
        }
    }

    /** قراءة متسامحة: مجهولٌ أو تالفٌ = لا عدّ (لا عطب يوقف التشغيل). */
    private fun decode(target: File): Map<String, HealthModel.Counters> {
        val lines = runCatching { target.readLines(Charsets.UTF_8) }.getOrNull() ?: return emptyMap()
        val map = mutableMapOf<String, HealthModel.Counters>()
        lines.forEach { line ->
            val at = line.indexOf('=')
            if (at <= 0) return@forEach
            val domain = line.substring(0, at).trim()
            if (domain !in HealthModel.DOMAINS) return@forEach
            val parts = line.substring(at + 1).split(',')
            if (parts.size != 3) return@forEach
            val counters = HealthModel.Counters(
                confirmed = parts[0].trim().toLongOrNull()?.coerceAtLeast(0) ?: 0,
                rolledBack = parts[1].trim().toLongOrNull()?.coerceAtLeast(0) ?: 0,
                unknown = parts[2].trim().toLongOrNull()?.coerceAtLeast(0) ?: 0,
            )
            if (counters.total > 0) map[domain] = counters
        }
        return map
    }
}
