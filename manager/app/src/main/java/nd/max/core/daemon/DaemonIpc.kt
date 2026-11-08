/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.daemon

import android.net.LocalSocket
import android.net.LocalSocketAddress
import nd.max.core.privilege.ExecutionModeRules
import nd.max.core.privilege.ExecutionModeStore

/**
 * عميل قناة الخادم — **بلا جذر**.
 *
 * التطبيق العاديّ يفتح السوكيت المجرَّد `@maxmanager.sock` (نفس الاسم في `archdaemon/jni/include/IpcServer.h`)،
 * ويحكم SELinux بـ`connectto` على نطاق الخادم، ثم يتحقّق الخادم من uid التطبيق بنفسه. والتطبيق لا يرسل
 * أمرًا حرًّا: **قائمة بيضاء مغلقة** (`PING`، `STATUS`، `PROFILE 1|2|3 [AI]`) — فلو سُرق السوكيت لم يكن
 * مع السارق صدفة.
 *
 * والنتيجة ثلاثيّة لا ثنائيّة: [Result.Unreachable] (لا خادم/لا قناة/رفض SELinux) يفتح **طريق الجذر القديم**،
 * أمّا [Result.Refused] (الخادم ردّ برفض صريح، مثل منع الاختيار اليدوي أثناء AI) فلا يُعاد بالجذر —
 * فالمحاولة الثانية كانت ستنفّذ الأمر مرّتين أو تتجاوز البوّابة.
 */
class DaemonIpc(
    private val transport: IpcTransport = LocalSocketTransport(),
    private val nowNanos: () -> Long = System::nanoTime,
    private val rootAllowed: () -> Boolean = { ExecutionModeRules.mayUseRoot(ExecutionModeStore.current()) },
) {

    sealed interface Result {
        data class Ok(val payload: String) : Result
        data class Refused(val reason: String) : Result
        data object Unreachable : Result
    }

    fun ping(): Boolean = send(DaemonIpcProtocol.PING, privileged = false) is Result.Ok

    fun status(): Result = send(DaemonIpcProtocol.STATUS, privileged = false)

    fun profile(profileId: String, fromAi: Boolean): Result {
        val request = DaemonIpcProtocol.profileRequest(profileId, fromAi)
            ?: return Result.Refused("invalid-profile")
        return send(request)
    }

    /**
     * قراءة عقدة من القائمة البيضاء (`READ`). `null` = لم تُقرأ (لا قناة، أو خارج القائمة، أو الخادم رفض/لا يعرف
     * الفعل) فيُكمل المستدعي بطبقته التالية — لا يُفترض أنّ العقدة غائبة.
     */
    fun read(path: String): String? {
        if (!rootAllowed()) return null
        if (!DaemonIpcProtocol.readable(path) || backingOff()) return null
        val reply = transport.roundTrip("READ $path") ?: return fail()
        return DaemonIpcProtocol.parseBody(reply)?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** وجود عقدة: `true`/`false` جوابٌ من الخادم نفسه (بالجذر)، و`null` = لا جواب فيُسأل غيره. */
    fun exists(path: String): Boolean? {
        if (!rootAllowed()) return null
        if (!DaemonIpcProtocol.readable(path) || backingOff()) return null
        val reply = transport.roundTrip("EXISTS $path") ?: return fail()
        val parsed = DaemonIpcProtocol.parseReply(reply)
        return if (parsed is Result.Ok) parsed.payload == "1" else null
    }

    /**
     * كتابة عقدة (`WRITE`) — `true` فقط إن أكّد الخادم. وقائمة الكتابة **لا تُنسخ هنا**: الحكم لها وحدها في
     * الخادم، فمسار خارجها يردّ `ERR invalid` فيعود المستدعي إلى طريقه القديم (الجذر) كما كان.
     */
    fun write(path: String, value: String): Boolean {
        if (!rootAllowed()) return false
        if (!DaemonIpcProtocol.writeShape(path, value) || backingOff()) return false
        val reply = transport.roundTrip("WRITE $path $value")
        if (reply == null) {
            fail()
            return false
        }
        return DaemonIpcProtocol.parseReply(reply) is Result.Ok
    }

    /** هل العقدة في قائمة الكتابة وموجودة؟ `null` = لا جواب فيُسأل غيره. */
    fun canWrite(path: String): Boolean? {
        if (!rootAllowed()) return null
        if (!DaemonIpcProtocol.writeShape(path, "0") || backingOff()) return null
        val reply = transport.roundTrip("CANWRITE $path") ?: return fail()
        val parsed = DaemonIpcProtocol.parseReply(reply)
        return if (parsed is Result.Ok) parsed.payload == "1" else null
    }

    /** كتابة خاصية إعداد (`SETPROP`). `true` فقط إن أكّد الخادم؛ غير ذلك يُكمل المستدعي بطريقه القديم. */
    fun setProperty(key: String, value: String): Boolean {
        if (!rootAllowed()) return false
        if (!DaemonIpcProtocol.settable(key, value) || backingOff()) return false
        val reply = transport.roundTrip("SETPROP $key $value")
        if (reply == null) {
            fail()
            return false
        }
        return DaemonIpcProtocol.parseReply(reply) is Result.Ok
    }

    private fun send(request: String, privileged: Boolean = true): Result {
        // رفض السياسة ليس تعذّر قناة: لا يجوز أن يفتح مسارًا احتياطيًا للكتابة.
        // و`PING`/`STATUS` ليسا عملًا مميّزًا: حجبهما يجعل الواجهة تقول «الخادم متوقّف»
        // عن خادم يعمل، وهو تشخيص كاذب. الحجب على ما ينفّذ أو يقرأ بالجذر وحده.
        if (privileged && !rootAllowed()) return Result.Refused("mode-root-blocked")
        if (backingOff()) return Result.Unreachable
        val reply = transport.roundTrip(request)
        if (reply == null) {
            fail()
            return Result.Unreachable
        }
        unreachableUntil = 0L
        return DaemonIpcProtocol.parseReply(reply)
    }

    /*
     * ذاكرة سلبيّة: حين لا قناة (خادم قديم/متوقّف) تُجرّب القراءات بالعشرات في الثانية فلا يُدفع اتصال فاشل
     * لكل واحدة — يُعاد الاختبار بعد خمس ثوانٍ. ونجاح أيّ طلب يمسحها.
     */
    @Volatile private var unreachableUntil = 0L

    private fun fail(): Nothing? {
        unreachableUntil = nowNanos() + UNREACHABLE_BACKOFF_NANOS
        return null
    }

    private fun backingOff(): Boolean = unreachableUntil != 0L && nowNanos() < unreachableUntil

    companion object {
        private const val UNREACHABLE_BACKOFF_NANOS = 5_000_000_000L

        /** نسخة واحدة للتطبيق: بلا حالة، والسوكيت يُفتح لكل طلب. */
        val default: DaemonIpc by lazy { DaemonIpc() }
    }
}

/** النصّ على السلك — خالص، فيُقاس على المضيف بلا جهاز. */
object DaemonIpcProtocol {
    const val SOCKET_NAME = "maxmanager.sock"
    const val PING = "PING"
    const val STATUS = "STATUS"

    /** `PROFILE 1|2|3 [AI]`، أو `null` لأيّ رقم آخر — لا يُرسَل شيء غير مفهوم إلى الخادم. */
    fun profileRequest(profileId: String, fromAi: Boolean): String? {
        if (profileId != "1" && profileId != "2" && profileId != "3") return null
        return if (fromAi) "PROFILE $profileId AI" else "PROFILE $profileId"
    }

    /*
     * **مرآة** قوائم `archdaemon/jni/src/Ipc/IpcServer.c` (READ_PREFIXES / READ_FILES / IPC_PROP_PREFIX).
     * هنا مسار سريع يوفّر رحلة فاشلة فقط؛ والحكم الملزِم للخادم وحده — فلو اختلفت القائمتان لم ينكسر أمان.
     */
    private val READ_PREFIXES = listOf(
        "/sys/devices/system/", "/sys/class/power_supply/", "/sys/class/devfreq/", "/sys/class/kgsl/",
        "/sys/class/thermal/", "/sys/class/graphics/", "/sys/module/ged/", "/sys/kernel/ged/",
        "/sys/kernel/gpu/", "/sys/block/zram0/", "/proc/pressure/", "/proc/sys/vm/",
    )
    private val READ_FILES = setOf("/proc/stat", "/proc/meminfo", "/proc/cpuinfo", "/proc/uptime", "/proc/swaps", "/proc/loadavg")
    private const val PROP_PREFIX = "persist.sys.maxmanagerconf."
    private val PATH_CHARS = Regex("^[A-Za-z0-9/._:@+-]+$")
    private val KEY_CHARS = Regex("^[A-Za-z0-9._]+$")
    private val VALUE_CHARS = Regex("^[A-Za-z0-9._:+,-]+$")

    fun readable(path: String): Boolean {
        if (path.length >= 192 || !PATH_CHARS.matches(path) || !path.startsWith("/")) return false
        if (path.contains("..") || path.contains("//")) return false
        return path in READ_FILES || READ_PREFIXES.any { path.startsWith(it) && path.length > it.length }
    }

    private val WRITE_VALUE_CHARS = Regex("^[A-Za-z0-9_.:,+-]+( [A-Za-z0-9_.:,+-]+)*$")

    /** شكل الطلب فقط (لا قائمة): يوفّر رحلة لأمر مشوَّه. الحكم الملزِم للخادم. */
    fun writeShape(path: String, value: String): Boolean =
        path.length < 192 && PATH_CHARS.matches(path) && path.startsWith("/") &&
            !path.contains("..") && !path.contains("//") &&
            value.length in 1..64 && WRITE_VALUE_CHARS.matches(value)

    fun settable(key: String, value: String): Boolean =
        key.startsWith(PROP_PREFIX) && key.length > PROP_PREFIX.length && key.length < 96 &&
            KEY_CHARS.matches(key) && value.length < 92 && VALUE_CHARS.matches(value)

    /** جسم ردّ `READ`: `OK <len>\n<body>` — `null` لأيّ ردّ آخر. */
    fun parseBody(reply: String): String? {
        val newline = reply.indexOf('\n')
        if (newline < 0) return null
        val header = reply.substring(0, newline)
        if (!header.startsWith("OK ")) return null
        if ((header.removePrefix("OK ").trim().toIntOrNull() ?: -1) < 0) return null
        return reply.substring(newline + 1)
    }

    fun parseReply(line: String): DaemonIpc.Result {
        val text = line.substringBefore('\n').trim()
        return when {
            text == "OK" -> DaemonIpc.Result.Ok("")
            text.startsWith("OK ") -> DaemonIpc.Result.Ok(text.removePrefix("OK "))
            text.startsWith("ERR ") -> DaemonIpc.Result.Refused(text.removePrefix("ERR "))
            else -> DaemonIpc.Result.Unreachable
        }
    }
}

/** سطر طلب → سطر ردّ؛ `null` = تعذّر الوصول (وليس رفضًا). */
fun interface IpcTransport {
    fun roundTrip(request: String): String?
}

private const val MAX_REPLY_BYTES = 4096 + 64

class LocalSocketTransport(private val timeoutMs: Int = 20_000) : IpcTransport {
    override fun roundTrip(request: String): String? = runCatching {
        LocalSocket().use { socket ->
            socket.connect(LocalSocketAddress(DaemonIpcProtocol.SOCKET_NAME, LocalSocketAddress.Namespace.ABSTRACT))
            // تبديل الملفّ قد يستغرق ثواني (الخادم يشغّل العملية ويكتب العقد)، فالمهلة أوسع من PING.
            socket.soTimeout = timeoutMs
            socket.outputStream.write("$request\n".toByteArray(Charsets.US_ASCII))
            socket.outputStream.flush()
            // الخادم يغلق بعد الردّ، فالقراءة حتى النهاية تلتقط جسم `READ` (حدّه ٤ كيلوبايت + سطر العنوان).
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (out.size() < MAX_REPLY_BYTES) {
                val got = socket.inputStream.read(buffer)
                if (got <= 0) break
                out.write(buffer, 0, got)
            }
            if (out.size() == 0) null else String(out.toByteArray(), Charsets.UTF_8)
        }
    }.getOrNull()
}
