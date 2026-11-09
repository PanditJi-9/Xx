/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.platform

import android.system.Os
import android.system.OsConstants
import nd.max.core.hardware.ControlOwnership
import nd.max.core.hardware.HardwareControlArbiter
import nd.max.core.hardware.HardwareControlKey
import nd.max.core.hardware.RootFileAccess
import nd.max.core.hardware.SharedHardwareOwnershipStore
import nd.max.core.hardware.WriteVerification
import java.util.UUID

/**
 * جدول `minfree` القديم، لا إعدادات `lmkd` الحديثة. لا تُكتب قيمة بلا لقطة قابلة للاسترجاع،
 * وكل طلب يمرّ بالمحكّم بمفتاح الكتالوج نفسه. الحساب مستقل عن صلاحيات الجهاز.
 */
object LmkTuning {

    const val MINFREE = "/sys/module/lowmemorykiller/parameters/minfree"

    const val MIN_PAGE = 1

    /** سقف إدخال محافظ بالصفحات؛ ليس تقديرًا للذاكرة ولا ضمانًا لملاءمة الرقم للجهاز. */
    const val MAX_PAGE = 500_000
    const val MAX_THRESHOLDS = 6
    const val MAX_TEXT_LENGTH = 128
    const val MIN_PERCENT = 50
    const val MAX_PERCENT = 200

    /** لا إسقاط لجزء فارغ، ولا إصلاح لجدول غير مرتب؛ كلاهما قد يغيّر معنى المتجه المكتوب. */
    fun parse(raw: String?): List<Int>? {
        if (raw == null || raw.length > MAX_TEXT_LENGTH) return null
        val text = raw.trim()
        if (text.isEmpty()) return null
        val parts = if (',' in text) text.split(',').map(String::trim) else text.split(Regex("\\s+"))
        if (parts.size !in 2..MAX_THRESHOLDS) return null
        val values = parts.map { part ->
            if (part.isEmpty() || part.any { it !in '0'..'9' }) return null
            part.toIntOrNull() ?: return null
        }
        return values.takeIf(::valid)
    }

    private fun valid(values: List<Int>): Boolean =
        values.size in 2..MAX_THRESHOLDS &&
            values.all { it in MIN_PAGE..MAX_PAGE } &&
            values.zipWithNext().all { (previous, next) -> next > previous }

    fun format(values: List<Int>): String = values.joinToString(",")

    /** لا قصّ صامت ولا رفع مصطنع للترتيب: النسبة التي تنتج سلّمًا غير صالح لا تُكتب. */
    fun scale(values: List<Int>, percent: Int): List<Int>? {
        if (percent !in MIN_PERCENT..MAX_PERCENT || !valid(values)) return null
        val scaled = values.map { value ->
            (value.toLong() * percent / 100L).toInt()
        }
        return scaled.takeIf(::valid)
    }

    fun pageToMegabytes(pages: Int, pageBytes: Long): Long? {
        if (pages !in MIN_PAGE..MAX_PAGE || pageBytes !in 4096L..65536L) return null
        if ((pageBytes and (pageBytes - 1L)) != 0L) return null
        return pages.toLong() * pageBytes / (1024L * 1024L)
    }

    enum class Outcome { APPLIED, NOT_PRESENT, INVALID, BLOCKED, NOT_WRITABLE, UNVERIFIED }

    /** الكتابة ليست افتراضية في المُنشئ: الاختبار والإنتاج يحدّدان بوابة الطلب صراحةً. */
    class Io(
        private val read: (String) -> String?,
        private val submit: (desired: String, expected: String) -> Outcome,
        private val pageBytes: () -> Long? = { null },
    ) {
        fun current(): List<Int>? = runCatching { parse(read(MINFREE)) }.getOrNull()

        fun pageSizeBytes(): Long? = runCatching { pageBytes() }.getOrNull()

        fun apply(values: List<Int>, expected: List<Int>): Outcome {
            if (!valid(values) || !valid(expected) || values.size != expected.size) return Outcome.INVALID
            val before = current() ?: return Outcome.NOT_PRESENT
            if (before != expected) return Outcome.BLOCKED
            val result = runCatching { submit(format(values), format(expected)) }
                .getOrDefault(Outcome.UNVERIFIED)
            if (result != Outcome.APPLIED) return result
            return if (current() == values) Outcome.APPLIED else Outcome.UNVERIFIED
        }

        companion object {
            fun system(arbiter: HardwareControlArbiter): Io = transactionIo(arbiter)

            /** نفس معاملة الإنتاج، بناقل مُمرَّر كي تُقاس الأسبقية والاسترجاع بالمحكّم الحقيقي. */
            internal fun transactionIo(
                arbiter: HardwareControlArbiter,
                read: (String) -> String? = RootFileAccess::read,
                write: (String, String) -> WriteVerification.Outcome = { path, value ->
                    RootFileAccess.writeVerifiedWith(path, value) { wrote, readBack ->
                        when {
                            readBack == null -> WriteVerification.Outcome.READBACK_UNAVAILABLE
                            parse(wrote) == parse(readBack) -> WriteVerification.Outcome.MATCHED
                            else -> WriteVerification.Outcome.DIFFERS
                        }
                    }
                },
                pageBytes: () -> Long? = { Os.sysconf(OsConstants._SC_PAGESIZE).takeIf { it > 0L } },
            ): Io = Io(
                read = read,
                submit = { desired, expected -> applySystem(arbiter, desired, expected, read, write) },
                pageBytes = pageBytes,
            )
        }
    }

    private fun applySystem(
        arbiter: HardwareControlArbiter,
        desired: String,
        expected: String,
        read: (String) -> String?,
        write: (String, String) -> WriteVerification.Outcome,
    ): Outcome {
        if (!SharedHardwareOwnershipStore.isConfigured()) return Outcome.BLOCKED
        val target = parse(desired) ?: return Outcome.INVALID
        val key = HardwareControlKey.MEMORY_LMK_MINFREE
        // طلب عابر، لا مُنتج يعيد التطبيق في الخلفية. يُزال من الدفتر بعد انتهاء المعاملة.
        val token = "manual-lmk-${UUID.randomUUID()}"
        var baselineChanged = false
        var writeOutcome: WriteVerification.Outcome? = null
        fun readCanonical(): String? = parse(read(MINFREE))?.let(::format)
        fun writeVerified(value: String, recordAttempt: Boolean = true): Boolean {
            val outcome = write(MINFREE, value)
            if (recordAttempt) writeOutcome = outcome
            return outcome == WriteVerification.Outcome.MATCHED
        }

        return try {
            val result = arbiter.submit(
                key = key,
                owner = ControlOwnership.Owner.GLOBAL_PROFILE,
                token = token,
                desired = desired,
                read = ::readCanonical,
                baseline = expected,
                apply = { value ->
                    // يعاد القياس داخل قفل المعاملة: لا يُطغى على تغيير أثناء حوار الإقرار.
                    if (readCanonical() != expected) {
                        baselineChanged = true
                        false
                    } else {
                        writeVerified(value)
                    }
                },
                restore = { previous ->
                    // لا تراجع يطغى على كاتب خارجي: تُسترجع فقط قيمة ما زالت أثر محاولتنا.
                    val live = readCanonical()
                    live == previous || (parse(live) == target && writeVerified(previous, recordAttempt = false))
                },
                // القراءة الأولى للمحكّم ما زالت تُنتج أساسًا؛ لا يتخطى الكتابة إن تغيّر بين القراءتين.
                realized = { _, _ -> readCanonical() == expected },
            )
            when {
                result.blocked || baselineChanged -> Outcome.BLOCKED
                result.verified && parse(result.actual) == target -> Outcome.APPLIED
                result.error?.endsWith("and-rollback-failed") == true -> Outcome.UNVERIFIED
                writeOutcome == WriteVerification.Outcome.DIFFERS ||
                    writeOutcome == WriteVerification.Outcome.READBACK_UNAVAILABLE -> Outcome.UNVERIFIED
                !result.applied -> Outcome.NOT_WRITABLE
                else -> Outcome.UNVERIFIED
            }
        } catch (_: Exception) {
            Outcome.UNVERIFIED
        } finally {
            runCatching { arbiter.release(key, token, restore = false) }
        }
    }
}
