/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import com.topjohnwu.superuser.Shell
import nd.max.core.jni.ProbeBridge

/**
 * Idempotent ZRAM backend with safety checks and live verification.
 *
 * ## العطب الذي أُصلح هنا (بلاغ المالك ٢٠٢٦-١٠-٢١: «شاشة Memory لا يعمل أيّ خيار فيها»)
 *
 * كانت السلسلة تُنفَّذ بمسار **sysfs** في أوامر المبادلة:
 * `swapoff /sys/block/zram0` ثم `mkswap /sys/block/zram0` ثم `swapon /sys/block/zram0`.
 * و`swapoff`/`mkswap`/`swapon` تأخذ **عقدة كتلة** (`/dev/block/zram0`)، لا مجلّدًا في
 * `sysfs`؛ فالنواة تردّها بـ`ENOTBLK` («ليس جهاز كتلة»). والنتيجة المقيسة على الجهاز:
 *
 * ١. `swapoff` يفشل ⇒ المبادلة تبقى مشغولةً بالقيم القديمة.
 * ٢. `echo 1 > reset` يفشل بـ`EBUSY` لأن الجهاز ما زال مبادلةً فعّالة.
 * ٣. كتابة `comp_algorithm` و`disksize` على جهازٍ مشغول تفشل صامتةً (`2>/dev/null`).
 * ٤. فيظهر للمستخدم «رفضت النواة التغيير» مع الفرق بين المطلوب والواقع — ولا سبب.
 *
 * والصواب ثلاثة أجزاء، كلّها هنا: **عقدة الكتلة** الحقيقية (تُصنع من `dev` إن غابت)،
 * و**توقّف عند أوّل خطوة فاشلة** بنصّ النواة نفسه، و**إعادة المبادلة** إلى حالتها السابقة
 * إن فشل أيّ شيء بعد فصلها — فلا يبقى المستخدم بلا مبادلة لأن محاولة تغييرٍ فشلت.
 */
object ZramHardwareBackend {
    private const val DEFAULT_ROOT = "/sys/block/zram0"

    data class State(
        val exists: Boolean,
        val sizeBytes: Long?,
        val algorithm: String?,
        val algorithms: List<String>,
        val activeSwap: Boolean = false,
        val devicePath: String = DEFAULT_ROOT,
    )

    /** خطوة واحدة كما رآها النواة: رمز الخروج ونصّها (بلا كتمان). */
    data class Step(val code: Int, val message: String) {
        val ok: Boolean get() = code == 0
    }

    /** المرحلة التي توقّفت عندها السلسلة — تُعرض باسمها فتكون diagnosis لا لومًا عامًّا. */
    enum class Stage { Detach, Reset, Algorithm, Size, Format, Attach }

    /**
     * نتيجة محاولة كتابة واحدة، بأدقّ ما يملكه التطبيق: هل الجذر ممنوح؟ وأيّ خطوة رُفضت؟
     * وبماذا؟ ولا «نجاح» إلا بعد قراءة القيمة من العقدة نفسها ومن `/proc/swaps`.
     */
    data class ApplyResult(
        val requestedSizeBytes: Long?,
        val requestedAlgorithm: String?,
        val liveSizeBytes: Long?,
        val liveAlgorithm: String?,
        val notRoot: Boolean = false,
        val node: String? = null,
        val failedStage: Stage? = null,
        val message: String? = null,
        /** `null` حين لم تُفصل المبادلة؛ وإلا: هل أُعيدت فعليًّا بعد فشلٍ ما بعد الفصل؟ */
        val swapRestored: Boolean? = null,
    ) {
        val verified: Boolean get() = !notRoot && failedStage == null

        /** رمز ثابت للمستهلكين القدامى (`VerificationResult.error`) — بلا كتمان للسبب. */
        fun errorCode(): String? = when {
            verified -> null
            notRoot -> "zram-no-root"
            else -> buildString {
                append("zram-").append(failedStage?.name?.lowercase() ?: "unverified")
                message?.takeIf { it.isNotBlank() }?.let { append(":").append(it) }
            }
        }
    }

    private fun deviceRoot(): String = RootFileAccess.globDirectories("/sys/block/zram*")
        .sortedWith(compareBy<String> { it.removePrefix("/sys/block/zram").toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it })
        .firstOrNull()
        ?: DEFAULT_ROOT

    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    /** تنفيذ أمر وإرجاع رمز الخروج ونصّ النواة — لا `2>/dev/null` في الخطوات الحاسمة. */
    private fun run(command: String): Step = runCatching {
        val result = Shell.cmd(command).exec()
        val message = (result.err + result.out)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" · ")
            .take(180)
        Step(result.code, message)
    }.getOrElse { Step(-1, it.message?.take(120) ?: "shell-error") }

    /** هل الجذر ممنوح فعلًا؟ أوّل سؤال، لأنّ كل ما بعده كتابة. */
    fun isRoot(): Boolean = runCatching {
        val result = Shell.cmd("id -u").exec()
        result.isSuccess && result.out.joinToString("").trim() == "0"
    }.getOrDefault(false)

    /**
     * كتابة `swappiness` كخطوة واحدة، بنصّ النواة نفسه — والقراءة بعدها هي الحكم لا رمز
     * الخروج: ملفٌّ للقراءة فقط مع الجذر يردّ `Permission denied`، وكان يُترجَم إلى
     * «رفضت النواة» بلا تمييز بين «لا جذر» و«قيمة غير مقبولة».
     */
    fun writeSwappiness(value: Int): Step = run("echo $value > /proc/sys/vm/swappiness")

    /**
     * عقدة الكتلة المقابلة للجهاز: `/dev/block/zramN`.
     *
     * بعض النوى لا تُنشئ العقدة (تُنشئها `init` عند الحاجة فقط، أو تختفي بعد `reset`)،
     * فتُصنع من `dev` (`major:minor`) — وهي نفس خطوة `mknod` التي تفعلها أدوات ZRAM.
     * وغياب العقدة يعني أنّ الفصل/الإعادة مستحيلان، فنقول ذلك بدل محاولةٍ صامتة.
     */
    fun deviceNode(root: String = deviceRoot()): String? {
        val index = root.removePrefix("/sys/block/zram").toIntOrNull() ?: return null
        val node = "/dev/block/zram$index"
        if (RootFileAccess.exists(node)) return node
        val pair = Regex("^(\\d+):(\\d+)$")
            .find(RootFileAccess.read("$root/dev")?.trim().orEmpty())
            ?: return null
        val (major, minor) = pair.destructured
        run("mkdir -p /dev/block")
        run("mknod $node b $major $minor")
        run("chmod 660 $node")
        return node.takeIf { RootFileAccess.exists(it) }
    }

    fun readState(): State {
        val root = deviceRoot()
        val exists = RootFileAccess.exists(root)
        if (!exists) return State(false, null, null, emptyList(), false, root)
        val raw = RootFileAccess.read("$root/comp_algorithm").orEmpty()
        val algorithms = raw.replace("[", " ").replace("]", " ")
            .split(Regex("\\s+")).filter(String::isNotBlank)
        val algorithm = Regex("\\[(.*?)]").find(raw)?.groupValues?.getOrNull(1)
            ?: algorithms.firstOrNull()
        return State(
            exists = true,
            sizeBytes = RootFileAccess.read("$root/disksize")?.toLongOrNull(),
            algorithm = algorithm,
            algorithms = algorithms.distinct(),
            activeSwap = isActiveSwap(root),
            devicePath = root,
        )
    }

    fun setAlgorithm(algorithm: String): VerificationResult<String> {
        val result = apply(algorithm = algorithm)
        return VerificationResult(
            requested = algorithm,
            actual = result.liveAlgorithm,
            writeSucceeded = !result.notRoot && result.failedStage == null,
            verified = result.verified && result.liveAlgorithm == algorithm,
            error = result.errorCode(),
        )
    }

    fun setSizeBytes(sizeBytes: Long, compressionAlgorithm: String? = null): VerificationResult<Long> {
        val result = apply(sizeBytes = sizeBytes, algorithm = compressionAlgorithm)
        return VerificationResult(
            requested = sizeBytes,
            actual = result.liveSizeBytes,
            writeSucceeded = !result.notRoot && result.failedStage == null,
            verified = result.verified && result.liveSizeBytes == sizeBytes,
            error = result.errorCode(),
        )
    }

    /**
     * سلسلة `swapoff → reset → comp_algorithm → disksize → mkswap → swapon` بعقدة الكتلة،
     * وبتحقّقٍ بعد كل خطوة، و**بإعادة المبادلة** إلى حجمها السابق إن فشل ما بعد الفصل.
     *
     * ولا يُنفَّذ شيء إن كان المطلوب هو الحال القائم (عقدةٌ متطابقة) — فتغييرٌ بلا فرق ليس
     * كتابة. و`sizeBytes = 0` تعني «أطفئ المبادلة» (فصلٌ بلا إعادة وصل).
     */
    fun apply(sizeBytes: Long? = null, algorithm: String? = null): ApplyResult {
        val before = readState()
        val root = before.devicePath
        if (!before.exists) {
            return ApplyResult(sizeBytes, algorithm, null, null, failedStage = Stage.Detach, message = "missing:$root")
        }
        if (!isRoot()) {
            return ApplyResult(sizeBytes, algorithm, before.sizeBytes, before.algorithm, notRoot = true)
        }
        val node = deviceNode(root)
            ?: return ApplyResult(
                sizeBytes, algorithm, before.sizeBytes, before.algorithm,
                failedStage = Stage.Detach, message = "no-block-node:$root",
            )

        if (algorithm != null && algorithm !in before.algorithms) {
            return ApplyResult(
                sizeBytes, algorithm, before.sizeBytes, before.algorithm,
                node = node, failedStage = Stage.Algorithm, message = "unsupported-algorithm",
            )
        }

        val sizeChanged = sizeBytes != null && sizeBytes != before.sizeBytes
        val algoChanged = algorithm != null && algorithm != before.algorithm
        if (!sizeChanged && !algoChanged) {
            return ApplyResult(sizeBytes, algorithm, before.sizeBytes, before.algorithm, node = node)
        }

        val wasActive = isActiveSwap(node)
        val targetSize = sizeBytes ?: before.sizeBytes ?: 0L
        // تغيير الكودك وحده لا يُطفئ المبادلة: الحالة المطلوبة له هي حالته الحالية.
        val wantSwap = if (sizeBytes == null) wasActive else targetSize > 0L

        if (wasActive) {
            val detach = run("swapoff ${quote(node)}")
            if (!detach.ok) {
                // لم تُفصل: لا شيء تغيّر، والمبادلة كما هي — نُبلّغ سبب النواة نفسه.
                return ApplyResult(
                    sizeBytes, algorithm, before.sizeBytes, before.algorithm,
                    node = node, failedStage = Stage.Detach, message = detach.message,
                )
            }
        }

        if (algoChanged) {
            val write = run("echo ${quote(algorithm!!)} > ${quote("$root/comp_algorithm")}")
            if (!write.ok) {
                return restoreThenFail(before, node, wasActive, Stage.Algorithm, write.message, sizeBytes, algorithm)
            }
        }

        if (sizeChanged) {
            val write = run("echo $sizeBytes > ${quote("$root/disksize")}")
            val live = readState().sizeBytes
            if (!write.ok || live != sizeBytes) {
                val why = write.message.ifBlank { "readback:$live" }
                return restoreThenFail(before, node, wasActive, Stage.Size, why, sizeBytes, algorithm)
            }
        }

        if (wantSwap) {
            val format = run("mkswap ${quote(node)}")
            if (!format.ok) {
                return restoreThenFail(before, node, wasActive, Stage.Format, format.message, sizeBytes, algorithm)
            }
            val attach = run("swapon ${quote(node)}")
            if (!attach.ok) {
                return restoreThenFail(before, node, wasActive, Stage.Attach, attach.message, sizeBytes, algorithm)
            }
        }

        val after = readState()
        val sizeOk = sizeBytes == null || after.sizeBytes == sizeBytes
        val algoOk = algorithm == null || after.algorithm == algorithm
        val swapOk = isActiveSwap(node) == wantSwap
        if (!sizeOk || !algoOk || !swapOk) {
            val why = buildString {
                if (!sizeOk) append("size=").append(after.sizeBytes).append(' ')
                if (!algoOk) append("algorithm=").append(after.algorithm).append(' ')
                if (!swapOk) append("swap=").append(isActiveSwap(node))
            }.trim()
            return restoreThenFail(before, node, wasActive, Stage.Attach, why, sizeBytes, algorithm)
        }

        return ApplyResult(sizeBytes, algorithm, after.sizeBytes, after.algorithm, node = node)
    }

    /**
     * إعادة المبادلة إلى حجمها وكودكها السابقين بعد فشلٍ وقع **بعد** فصلها.
     *
     * هذا ليس تلميعًا: بلا هذه الخطوة يصبح «تغيير swap» الذي فشل سببًا لبقاء الجهاز بلا
     * مبادلة حتى إعادة التشغيل — وهو أسوأ من رفضٍ صريح. ونصّ الإعادة يُرفَق بالرسالة.
     */
    private fun restoreThenFail(
        before: State,
        node: String,
        wasActive: Boolean,
        stage: Stage,
        message: String?,
        sizeRequested: Long?,
        algorithmRequested: String?,
    ): ApplyResult {
        var restored: Boolean? = null
        if (wasActive) {
            val root = before.devicePath
            var ok = true
            if (ok) ok = run("echo 1 > ${quote("$root/reset")}").ok
            val size = before.sizeBytes ?: 0L
            if (ok && size > 0L) ok = run("echo $size > ${quote("$root/disksize")}").ok
            if (ok && !before.algorithm.isNullOrBlank()) {
                run("echo ${quote(before.algorithm)} > ${quote("$root/comp_algorithm")}")
            }
            if (ok && size > 0L) ok = run("mkswap ${quote(node)}").ok
            if (ok && size > 0L) ok = run("swapon ${quote(node)}").ok
            restored = ok && isActiveSwap(node)
        }
        val suffix = when (restored) {
            true -> " · swap-restored"
            false -> " · SWAP-NOT-RESTORED"
            null -> ""
        }
        return ApplyResult(
            requestedSizeBytes = sizeRequested,
            requestedAlgorithm = algorithmRequested,
            liveSizeBytes = readState().sizeBytes,
            liveAlgorithm = readState().algorithm,
            node = node,
            failedStage = stage,
            message = (message ?: "step-failed") + suffix,
            swapRestored = restored,
        )
    }

    /**
     * هل هذا الجهاز مشارَك كمبادلة؟
     *
     * `/proc/swaps` **عالمية القراءة**، فالسؤال «هل مسار هذا الجهاز في العمود الأول؟» لا
     * يحتاج ولادة عملية (`awk`): تُقرأ داخل العملية في نداء واحد، ويُشطر السطر الأول
     * (العنوان). والاحتياطي هو `awk` نفسه إن غابت المكتبة الأصلية أو عاد الملف فارغًا.
     *
     * **والتطابق بأيّ من الاسمين:** `/proc/swaps` يكتب عقدة الكتلة (`/dev/block/zram0`)،
     * بينما بقية الشاشة تعمل بمسار sysfs. وكان التطابق على اسمٍ واحد فقط، فتقول الدالة
     * «غير فعّالة» وهي فعّالة — وهذا ما جعل `setAlgorithm` يكتب على جهازٍ مشغول.
     */
    private fun isActiveSwap(device: String): Boolean {
        val trimmed = device.trimEnd('/')
        val aliases = setOf(
            trimmed,
            trimmed.replace("/dev/block/", "/sys/block/"),
            trimmed.replace("/sys/block/", "/dev/block/"),
        )
        ProbeBridge.readMany(listOf("/proc/swaps"))
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { swaps ->
                return swaps.lineSequence()
                    .drop(1)
                    .mapNotNull { line -> line.trim().split(Regex("\\s+")).firstOrNull() }
                    .any { it in aliases }
            }
        return runCatching {
            val condition = aliases.joinToString(" || ") { "\$1==\"$it\"" }
            Shell.cmd("awk '$condition {found=1} END {exit found?0:1}' /proc/swaps 2>/dev/null")
                .exec().isSuccess
        }.getOrDefault(false)
    }
}
