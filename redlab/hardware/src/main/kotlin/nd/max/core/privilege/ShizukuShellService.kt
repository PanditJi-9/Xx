/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.privilege

import android.util.Log
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * خدمة المستخدم (`AR-20`) — الكائن الذي يُنشئه **خادم Shizuku** في عملية منفصلة يديرها،
 * فتُنفَّذ أوامرنا بصلاحية تلك العملية (مستوى ADB عادة، أو جذرًا إن كان Shizuku نفسه يعمل بجذر).
 *
 * **لماذا صنف لا `Service`:** خادم Shizuku يُنشئ الصنف مباشرةً في عملية `app_process` يملكها
 * ([المرجع: نموذج Shizuku الرسمي]) — فلا يُسجَّل في المانيفست ولا يحتاج `onBind`، والشرط الوحيد
 * وجود مُنشئ بلا وسائط. و`destroy()` رقم معاملته في العقد محفوظ هناك، ويُسقط العملية عند فضّ الربط.
 *
 * **حدود الطبقة — معلنة لا مُخفيّة:**
 *   • لا جذر مضمون: العملية بصلاحية Shizuku نفسها؛ إن كان الشيزوكو بجذر صار الأمر بجذر، وإن كان
 *     بمستوى ADB بقي بمستوى ADB — ولا ندّعي أعلى من ذلك ([`PrivilegedShell`] يعلن المسار المستعمل).
 *   • لا كتابة sysfs من هنا: القيود النواة/SELinux لا تُلتفّ، وما يحتاج جذرًا يبقى في مساره الجذريّ.
 *   • أمر واحد في كل نداء، بلا صدفة تفاعلية:لا حالة تُبنى على نداء سابق ولا `cd` يبقى أثره.
 */
class ShizukuShellService : IShizukuShell.Stub() {

    override fun exec(command: String): Array<String> {
        val rejection = ShizukuExecRules.rejectionFor(command)
        if (rejection != null) {
            return arrayOf(ShizukuExecRules.CODE_LAUNCH_FAILED.toString(), "rejected: $rejection")
        }
        return runCatching { runShell(command) }.getOrElse { failure ->
            // الفشل يُعاد بنصّه لا يُبتلع: رسالة الاستثناء أصدق من صمت.
            arrayOf(
                ShizukuExecRules.CODE_LAUNCH_FAILED.toString(),
                failure.message ?: failure.javaClass.simpleName,
            )
        }
    }

    override fun destroy() {
        Log.i(TAG, "destroy requested; exiting user service")
        System.exit(0)
    }

    /**
     * تشغيل واحد: `sh -c` مع دمج الخطأ في الخَرْج، وقراءة على خيط منفصل (فالأنبوب الممتلئ يعلّق
     * العملية إن لم يُقرأ)، ومهلة صريحة تُتبَع بقتل إجباريّ وحصاد — فلا يبقى أمر معلّق يعيش وحده.
     */
    private fun runShell(command: String): Array<String> {
        val process = ProcessBuilder(SH, "-c", command)
            .redirectErrorStream(true)
            .start()
        val sink = CappedSink(ShizukuExecRules.MAX_OUTPUT_CHARS)
        val drain = Thread { sink.drain(process.inputStream) }
            .apply { isDaemon = true }
        drain.start()

        val finished = process.waitFor(ShizukuExecRules.EXEC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!finished) process.destroyForcibly()
        process.waitFor()
        // القراءة تنتهي عند إغلاق الأنبوب؛ ومهلة الحصاد تحرس من وريث يحمل الأنبوب بعد قتل أبيه.
        drain.join(DRAIN_JOIN_MS)

        val code = if (finished) process.exitValue() else ShizukuExecRules.CODE_TIMEOUT
        return arrayOf(code.toString(), sink.text())
    }

    /**
     * مصرف مقيّد: يجمع حتى [limit] حرفًا ثم **يستمرّ في القراءة ويرمي** — فالأمر لا ينحبس على
     * أنبوب ممتلئ، والذاكرة لا تكبر بحجم خَرْجه، والاقتطاع يُعلَن بعلامته.
     */
    private class CappedSink(private val limit: Int) {

        private val builder = StringBuilder()
        private var truncated = false

        fun drain(stream: InputStream) {
            val reader = InputStreamReader(stream, Charsets.UTF_8)
            val chars = CharArray(CHUNK_CHARS)
            while (true) {
                val read = reader.read(chars)
                if (read <= 0) break
                append(chars, read)
            }
        }

        private fun append(chars: CharArray, count: Int) {
            val room = limit - builder.length
            if (room <= 0) {
                truncated = true
                return
            }
            if (count <= room) {
                builder.append(chars, 0, count)
            } else {
                builder.append(chars, 0, room)
                truncated = true
            }
        }

        fun text(): String =
            if (truncated) builder.toString() + ShizukuExecRules.TRUNCATION_MARK else builder.toString()
    }

    private companion object {
        /** وسم السجل — ثابت يُبحث به في `logcat` عند تشخيص الطبقة. */
        const val TAG = "MaxManagerShell"

        /** مسار الصدفة النظامية: ثابت، ولا تُستدعى صدفة المستخدم. */
        const val SH = "/system/bin/sh"

        /** مهلة ضمّ خيط القراءة بعد موت العملية بالمِلّي ثانية. */
        const val DRAIN_JOIN_MS = 2_000L

        /** حجم قطعة القراءة بالحروف — يوازن بين النداءات وحدود الذاكرة. */
        const val CHUNK_CHARS = 8_192
    }
}
