/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

/*
 * الخيوط عبر الجلسة كاملة — **فرق** زمن المعالج لكل خيط بين حدّي التسجيل.
 *
 * شاشة العمليات تعطي لقطة حيّة: «هذا الخيط يستهلك الآن». وتبقى بعد إغلاق اللعبة بلا جواب:
 * أين ذهب المعالج طوال الجلسة؟ تقرير الجلسة يحفظ المتوسّطات (إطارات، معالج، حرارة) ولا
 * يحفظ **توزيعها على الخيوط**؛ والنقص هنا توزيع لا رقم آخر.
 *
 * **والقياس حدّان لا مسار لعب:** لقطة واحدة عند البدء ولقطة عند الإيقاف. الفرق بينهما
 * هو زمن الخيط في الجلسة؛ ولا عيّنة واحدة تُؤخذ أثناء اللعب، فلا يُضاف عمل إلى المسار الحرج
 * الذي يُقاس أصلًا.
 *
 * وحدوده المعلنة، وكلها تُعرض لا تُخترع (ADR-07):
 *  1. الخيط الذي وُلد في منتصف الجلسة **يُستبعد** — لا نعرف زمنه قبل وجوده، ونسبته إلى الجلسة كلها تُصغّره.
 *  2. الخيط الذي مات قبل الإيقاف **يُستبعد** للسبب نفسه: لا لقطة نهائية له.
 *  3. اختلاف المعرّف بين اللقطتين (لعبة أُعيد تشغيلها) يعني «مجهول» لا «صفر» — فتُترك القائمة غائبة.
 *  4. الوحدة نبضات خامّة كما يقرؤها `stat`؛ التحويل إلى ثوانٍ يحتاج `_SC_CLK_TCK` ويقع عند العرض.
 */
package nd.max.core.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import nd.max.core.privilege.RootCommands

/**
 * انشغال خيط خلال جلسة واحدة — فرق نبضات بين قراءتين، لا زمنًا تراكميًّا منذ إنشاء الخيط.
 *
 * @param ticks النبضات التي استهلكها الخيط بين بدء الجلسة وإيقافها.
 */
@Serializable
data class SessionThreadLoad(
    val tid: Int,
    val name: String,
    val ticks: Long
)

/**
 * لقطة خيوط عملية واحدة.
 *
 * وحفظ [pid] معها ليس تفصيلًا: الفرق لا معنى له بين عمليتين مختلفتين، واللعبة التي أُعيد
 * تشغيلها في منتصف الجلسة تعطي أرقامًا سليمة الشكل خاطئة المعنى. فالمعرّف هو ما يرفض الفرق.
 */
data class ThreadsSnapshot(
    val pid: Int,
    val threads: List<ThreadTime>
)

/**
 * قارئ حدّي: نداء جذر واحد يعطي معرّف العملية وخيوطها معًا.
 *
 * **ولماذا نداء واحد:** فصل `pidof` عن قراءة الخيوط يفتح ثغرة بين النداءين — عملية تموت
 * بينهما تعطي «لا شيء» بعد أن كان هناك شيء، بلا سبب ظاهر. والنداء الواحد يحمل معرّفه معه،
 * فما يُقرأ يُنسب إلى العملية التي قُرئ منها بالضبط.
 */
object SessionThreadProbe {

    /** كم خيطًا يُحفظ. الستة الأعلى تصف الجلسة؛ وما بعدها ذيلٌ لا يغيّر القرار. */
    const val MAX_THREADS = 6

    /**
     * أسماء الحزم أسماء، لا أوامر — والقشرة تُبنى بها نصًّا، فما خرج عن هذه المحارف يُرفض
     * قبل أن يصل إلى `sh`.
     */
    private val PackagePattern = Regex("[A-Za-z0-9._]+")

    private const val PID_PREFIX = "pid="

    /**
     * لقطة خيوط الحزمة الآن، أو `null` حين لا يُقرأ شيء.
     *
     * ولا يُرمى خطأ: غياب الجذر، أو لعبة أُغلقت، أو `pidof` غير موجود — كلها «لم تُقرأ»
     * بالمعنى نفسه الذي تعنيه في بقية الطبقة.
     */
    suspend fun read(packageName: String?): ThreadsSnapshot? {
        val pkg = packageName?.takeIf { PackagePattern.matches(it) } ?: return null
        val result = withContext(Dispatchers.IO) {
            runCatching { RootCommands.cmd(command(pkg)).exec() }.getOrNull()
        } ?: return null
        if (!result.isSuccess) return null
        return parseSnapshot(result.out)
    }

    /**
     * الأمر: معرّف العملية ثم كل ملفّ `stat` تحت `/proc/<pid>/task`، إن وُجدت العملية.
     *
     * و`echo` الفارغ بعد كل ملفّ يفصل السطور: `cat` لا يضمن سطرًا جديدًا في نهاية الملفّ،
     * فسطران يلتصقان يصيران سطرًا واحدًا لا يُقرأ ويُسقط خيطين بهدوء.
     */
    private fun command(pkg: String): String =
        "p=\$(pidof $pkg 2>/dev/null); p=\${p%% *}; " +
            "if [ -n \"\$p\" ]; then echo \"$PID_PREFIX\$p\"; " +
            "for f in /proc/\$p/task/*/stat; do cat \"\$f\" 2>/dev/null; echo; done; fi"

    /**
     * مخرَج النداء ← لقطة. دالّة خالصة تُقاس بلا جهاز.
     *
     * سطر المعرّف أوّلًا؛ وغيابه يعني أن العملية لم تُوجد، فالجواب `null` لا لقطة بمعرّف مخترع.
     */
    internal fun parseSnapshot(lines: List<String>): ThreadsSnapshot? {
        val head = lines.firstOrNull { it.startsWith(PID_PREFIX) } ?: return null
        val pid = head.removePrefix(PID_PREFIX).trim().toIntOrNull()?.takeIf { it > 0 } ?: return null
        val threads = ProcessFeed.parseThreadTimes(lines.filterNot { it.startsWith(PID_PREFIX) })
            ?: return null
        return ThreadsSnapshot(pid, threads)
    }

    /**
     * فرق اللقطتين: زمن كل خيط في الجلسة، الأعلى أوّلًا.
     *
     * @return `null` حين لا يُحسب فرق — لقطة غائبة، أو عملية مختلفة، أو لا خيط مشترك.
     *         والقائمة الفارغة لا تُعاد: جلسة لعبة لها خيط واحد على الأقلّ يستهلك.
     */
    fun loads(start: ThreadsSnapshot?, end: ThreadsSnapshot?): List<SessionThreadLoad>? {
        if (start == null || end == null || start.pid != end.pid) return null
        val before = start.threads.associateBy { it.tid }
        val deltas = buildList {
            for (thread in end.threads) {
                val from = before[thread.tid] ?: continue
                val delta = thread.ticks - from.ticks
                // الزمن لا يتراجع؛ وسالب هنا يعني قراءة تالفة أو خيطًا أُعيد استخدام معرّفه.
                if (delta > 0L) add(SessionThreadLoad(thread.tid, thread.name, delta))
            }
        }
        if (deltas.isEmpty()) return null
        return deltas
            .sortedWith(compareByDescending<SessionThreadLoad> { it.ticks }.thenBy { it.tid })
            .take(MAX_THREADS)
    }
}
