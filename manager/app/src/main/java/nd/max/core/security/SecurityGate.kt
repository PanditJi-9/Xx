/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.security

import android.content.Context
import nd.max.BuildConfig
import nd.max.core.diagnostics.DiagnosticCenter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * البوابة: **نقطة واحدة يُسأل منها كل موضع، ولا تحكم بنفسها**.
 *
 * المنطق في [IntegrityLedger]، والسياسة في [IntegrityPolicy]، والقياس في [IntegrityProbe]
 * و[SecurityBridge]. وهذه الطبقة تربطها فقط: تُشغِّل الفحوص مرّة أو في كل موضع، وتُمرّر
 * الملاحظات إلى السجلّ، وتُعلن الحكم. ولا يوجد فيها شرط `if` واحد يحكم بالتلاعب — وهذا
 * مقصود: الحكم كلّه في ملفّ يُقاس في JVM، فلا يبقى في الكود الذي لا يُقاس منه شيء.
 *
 * ## التوزيع: أيّ فحص في أيّ موضع
 *
 * | الفحص | متى | لماذا هناك |
 * | --- | --- | --- |
 * | بصمة الموقّع (مساران) | مرّة في العمر | لا تتغيّر داخل العملية، وتكرارها كلفة بلا مكسب |
 * | مكتبة أصلية مستبدلة على القرص | مرّة في العمر | قراءة حزمة كاملة؛ تُقاس مرّة عند الإقلاع |
 * | أصول مختومة | مرّة في العمر | قراءة ملفّات، وتكرارها على كل شاشة إسراف معلن |
 * | مصحّح · متتبّع · خريطة هوك · جسر Java | **كل موضع** | قياسات حيّة: تُقاس لحظة الفحص لا تُحفظ |
 *
 * والقاعدة التي تحكم الاختيار: **يُوزَّع الحيّ، ويُخزَّن الثابت.** فتكرار قراءة الحزمة في كل
 * نقطة لا يزيد شيئًا، بينما تكرار قياس «هل هناك متتبّع الآن؟» في كل نقطة هو ما يجعل حذف
 * نقطة واحدة بلا فائدة للمهاجم — فيبقى الحكم قائمًا من النقاط التي لم تُحذف.
 *
 * ## ولا تُنفَّذ الفحوص الثقيلة على خيط الواجهة — أبدًا
 *
 * الفحص الثابت يقرأ الحزمة كلها ويحسب بصمات مدخلاتها، وقد يبلغ ثوانٍ على جهاز بطيء. وكان
 * `Application.onCreate` يطلقه على خيط خلفيّ، لكن `MainActivity.onCreate` قد يسبقه في
 * `SecurityEnforcement.refuseActivity` فيجد الراية غير مرفوعة ويشغّل الفحص **على الخيط
 * الرئيسيّ** — فتظلّ النافذة سوداء حتى ينتهي. صار الطلب هنا **جدولةً لا انتظارًا**: أول
 * مستدعٍ يرفع راية `staticChecksStarted` ويبدأ خيطًا خلفيًّا منخفض الأولوية، وكل مستدعٍ
 * بعدها يُكمل فحوص الموضع الحيّة ويعود فورًا. والحكم (`enforce = false`) لا يمنع شيئًا
 * أصلًا، فانتظار الواجهة له مقايضة مرفوضة.
 */
object SecurityGate {

    private val ledger = IntegrityLedger()

    /** انتهى الفحص الثابت فعلًا (لا «بدأ») — يُقرأ للتشخيص وحده. */
    @Volatile
    private var staticChecksCompleted = false

    /** حرس «خيط واحد فقط»: يُرفع قبل بدء الخيط، فلا يبدأ فحصان في سباق الإقلاع. */
    private val staticChecksStarted = AtomicBoolean(false)

    @Volatile
    private var diagnosticsWritten = false

    /**
     * السياسة السارية: رصد وتشخيص محلي فقط — `enforce = false` دائمًا.
     * والتطبيق لا يرفض نفسه ولا يقتل عمليته على أجهزة المستخدمين (جذور، LSPosed، أو توقيع محلي).
     */
    fun policy(): IntegrityPolicy = IntegrityPolicy(
        enforce = false,
        debuggable = BuildConfig.DEBUG,
    )

    /**
     * يُشغّل فحوص الموضع ويُعيد الحكم الجاري.
     *
     * ولا يرفع استثناءً ولا يقتل العملية: **الرفض قرار المتصل** ([shouldRefuse])، فتبقى
     * السياسة في موضع واحد قابل للقراءة.
     */
    fun evaluate(context: Context, checkpoint: IntegrityCheckpoint): IntegrityVerdict {
        val active = policy()
        // جدولة لا انتظار: الفحص الثابت ثقيل، ولا يجوز أن يدفع ثمنه أوّل إطار في النشاط.
        ensureStaticChecks(context, active)
        runLiveChecks(checkpoint, active)
        val verdict = ledger.verdict()
        if (verdict != IntegrityVerdict.CLEAN) writeDiagnosticsOnce(context, verdict)
        return verdict
    }

    /**
     * يُشغّل الفحص الثابت مرّة على خيط خلفيّ ويعود فورًا — للاستدعاء من `Application.onCreate`
     * بلا خيط إضافيّ حوله. ومناداة `evaluate` من أي موضع تفعل الشيء نفسه.
     */
    fun warmUp(context: Context) {
        ensureStaticChecks(context, policy())
    }

    /** هل يُرفض التشغيل الآن؟ يُشغّل الفحص للتشخيص ويُعيد false دائمًا لضمان استقرار التطبيق. */
    fun shouldRefuse(context: Context, checkpoint: IntegrityCheckpoint): Boolean {
        evaluate(context, checkpoint)
        return false
    }

    /** الحكم الجاري بلا فحص جديد. */
    fun verdict(): IntegrityVerdict = ledger.verdict()

    /** الأدلّة المرصودة — للتشخيص المحلي وحده. */
    fun evidence(): List<IntegrityFinding> = ledger.all()

    /** هل ضُبطت بصمة موقّع أصلًا؟ يُعرض في التشخيص لأن «الحماية غير مُعدّة» يجب أن تُرى. */
    fun pinConfigured(): Boolean = SecurityPin.configured

    /** يُعيد كل شيء إلى أوّله — للاختبارات وحدها. */
    internal fun resetForTest() {
        ledger.reset()
        staticChecksCompleted = false
        staticChecksStarted.set(false)
        diagnosticsWritten = false
    }

    private fun record(
        active: IntegrityPolicy,
        signal: IntegritySignal,
        checkpoint: IntegrityCheckpoint,
        detail: String = "",
    ) {
        if (!active.counts(signal)) return
        ledger.record(IntegrityFinding(signal, checkpoint, detail))
    }

    /**
     * جدولة الفحوص الثابتة **مرّة واحدة** على خيط خلفيّ منخفض الأولوية.
     *
     * والراية تُرفع بـ`compareAndSet` قبل بدء الخيط، فلا يبدأ فحصان في سباق الإقلاع/الواجهة.
     * ومناداة ثانية أثناء العمل تعود فورًا؛ ولا ينتظرها أحد. وأولوية الخيط دنيا كي لا تزاحم
     * أوّل إطار، والفحص تشخيصيّ لا يمنع تشغيلًا (`enforce = false`).
     */
    private fun ensureStaticChecks(context: Context, active: IntegrityPolicy) {
        if (staticChecksCompleted) return
        if (!staticChecksStarted.compareAndSet(false, true)) return
        Thread {
            try {
                runStaticChecks(context, active)
            } finally {
                // «اكتمل» تعني انتهى فعلًا؛ حتى لو رمى الفحص تبقى الراية مرفوعة فلا إعادة محاولة
                // في كل فتح شاشة — الفشل يُسجَّل في `DiagnosticCenter` داخل `runCatching`.
                staticChecksCompleted = true
            }
        }.apply {
            name = "max-integrity-static"
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }.start()
    }

    /** الفحوص الثابتة — يُستدعى من [ensureStaticChecks] وحده، على الخيط الخلفيّ. */
    private fun runStaticChecks(context: Context, active: IntegrityPolicy) {
        runCatching {
            val pinned = SecurityPin.expected
            if (pinned == null) {
                // «لا بصمة» ليست تلاعبًا: بناءٌ في بيئة بلا مخزن مفاتيح يجب أن يعمل.
                DiagnosticCenter.record(
                    component = COMPONENT,
                    message = "integrity: no signer pin was derived at build time — observing only",
                )
            } else {
                checkSignerFromPlatform(context, pinned)
                checkSignerFromPackageFile(context, pinned)
            }
            checkApkContent(context)
            checkNativeImage(context)
            checkSealedAssets(context)
        }.onFailure {
            DiagnosticCenter.record(
                component = COMPONENT,
                message = "integrity: static checks failed: ${it.message}",
                throwable = it,
            )
        }
    }

    /**
     * التحقّق الكامل من محتوى الحزمة مقابل بصماتها الموقّعة — **الأقوى في هذه الطبقة**.
     *
     * وهو الفحص الوحيد الذي يرى تعديلًا في الـDEX حين يبقى التوقيع الأصلي كما هو (حالة
     * إبطال تحقّق النظام على جهاز متجذّر): بصمة الموقّع لم تتغيّر، والأصول قد لا تُلمَس،
     * فيبقى هذا وحده هو الذي يرى تعديل المنطق.
     *
     * **وموضعه عند الإقلاع لا على كل شاشة:** يقرأ الحزمة كلها ويحسب بصمات مدخلاتها،
     * وتكراره في كل نقطة تحقّق كلفة بلا مكسب (المحتوى لا يتغيّر داخل العملية).
     * ونتيجته تُخزَّن في السجلّ الثابت، فتبقى قائمة في كل النقاط التي تليه.
     */
    private fun checkApkContent(context: Context) {
        val outcome = ApkContentVerification.verify(context)
        when (outcome) {
            ApkContentVerification.Outcome.REJECTED -> record(
                policy(), IntegritySignal.APK_CONTENT_MISMATCH, IntegrityCheckpoint.PROCESS,
                "apk content digest rejected",
            )
            // "لم يتسنّ الفحص" **لا يُحتسب دليلًا** — وهذا مقصود ومنصوص عليه في
            // `ApkContentVerification`: تحويل كل عطب بيئيّ إلى «نسخة معدّلة» عطبٌ أسوأ
            // من الثغرة التي يُغلقها الفحص.
            ApkContentVerification.Outcome.UNAVAILABLE -> DiagnosticCenter.record(
                component = COMPONENT,
                message = "integrity: apk content verification unavailable",
            )
            ApkContentVerification.Outcome.VERIFIED -> Unit
        }
    }

    /** المسار الأول: ما يقوله النظام عن موقّع الحزمة المُثبَّتة. */
    private fun checkSignerFromPlatform(context: Context, pinned: ByteArray) {
        val actual = IntegrityProbe.signerDigest(context) ?: return
        if (!IntegrityDigest.constantTimeEquals(pinned, actual)) {
            record(
                policy(), IntegritySignal.SIGNER_MISMATCH, IntegrityCheckpoint.PROCESS,
                "platform signer digest differs",
            )
        }
    }

    /**
     * المسار الثاني: ما يُقرأ من ملفّ الحزمة نفسه (`APK Signing Block`).
     *
     * وهذا هو المسار الذي **لا يمرّ على `PackageManager`**؛ فمن هوك `getPackageInfo` ليُخفي
     * إعادة توقيع، يبقى مكشوفًا هنا. و`null` (تعذّرت القراءة) لا يُحتسب دليلًا.
     */
    private fun checkSignerFromPackageFile(context: Context, pinned: ByteArray) {
        val apk = java.io.File(context.applicationInfo.sourceDir)
        val actual = ApkSigningBlock.signerCertificateDigest(apk)
        if (actual != null && !IntegrityDigest.constantTimeEquals(pinned, actual)) {
            record(
                policy(), IntegritySignal.SIGNER_MISMATCH, IntegrityCheckpoint.PROCESS,
                "apk signing block digest differs",
            )
        }
    }

    /**
     * المكتبة الأصلية: هل الملفّ المُحمَّل فعلًا هو الملفّ الذي وُقّع؟
     *
     * الدليل قاطع لأن استبدال الملفّ في `nativeLibraryDir` **لا يغيّر بايتًا في الحزمة
     * الموقّعة**، فلا يراه فحص التوقيع أبدًا. ويُقارَن بمُحرّكين مستقلّين: `MessageDigest`
     * في Kotlin وSHA في native — فمن أراد إخفاء الاستبدال يُبطل المحرّكين.
     */
    private fun checkNativeImage(context: Context) {
        val kotlinSaysMismatch = IntegrityProbe.nativeImageMismatch(context) ?: return
        if (!kotlinSaysMismatch) return
        // تأكيد ثانٍ من native قبل إعلان دليل قاطع: لا يُعلن التلاعب بمحرّك واحد.
        val apkDigest = IntegrityProbe.apkEntryDigest(context, nativeEntryName(context))
        val diskPath = java.io.File(context.applicationInfo.nativeLibraryDir, IntegrityProbe.NATIVE_LIBRARY_NAME).absolutePath
        val diskDigestHex = SecurityBridge.fileDigestHex(diskPath)
        val confirmed = when {
            apkDigest == null || diskDigestHex == null -> false
            else -> diskDigestHex != IntegrityDigest.encode(apkDigest)
        }
        record(
            policy(), IntegritySignal.NATIVE_IMAGE_MISMATCH, IntegrityCheckpoint.PROCESS,
            if (confirmed) "confirmed by native digest" else "kotlin digest only",
        )
    }

    private fun nativeEntryName(context: Context): String {
        val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull { it == "arm64-v8a" || it == "armeabi-v7a" }
            ?: android.os.Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
        return "lib/$abi/${IntegrityProbe.NATIVE_LIBRARY_NAME}"
    }

    /** الأصول المختومة: هل ما يُقرأ من الحزمة هو ما خُتم زمن البناء؟ */
    private fun checkSealedAssets(context: Context) {
        val intact = AssetVault.verifySealedAssets(context) ?: return
        if (!intact) {
            record(
                policy(), IntegritySignal.SEALED_ASSET_MISMATCH, IntegrityCheckpoint.PROCESS,
                "sealed asset digest differs",
            )
        }
    }

    /** الفحوص الحيّة — تُقاس في كل موضع لأنها تقيس لحظتها، لا حالةً محفوظة. */
    private fun runLiveChecks(checkpoint: IntegrityCheckpoint, active: IntegrityPolicy) {
        runCatching {
            if (IntegrityProbe.debuggerAttached()) {
                record(active, IntegritySignal.DEBUGGER_ATTACHED, checkpoint)
            }
            val tracer = IntegrityProbe.tracerPid()
            if (tracer != null && tracer > 0) {
                record(active, IntegritySignal.TRACER_ATTACHED, checkpoint)
            }
            IntegrityProbe.hookLibraryMapped()?.let {
                record(active, IntegritySignal.HOOK_FRAMEWORK_MAPPED, checkpoint, it)
            }
            IntegrityProbe.javaHookBridge()?.let {
                record(active, IntegritySignal.JAVA_HOOK_BRIDGE_PRESENT, checkpoint, it)
            }
            // المرساة الأصلية: نفس السؤال من كود مُصرَّف. وغياب المكتبة ليس دليلًا.
            SecurityBridge.runtimeProbe()?.let { probe ->
                if (probe.tracerAttached) record(active, IntegritySignal.TRACER_ATTACHED, checkpoint, "native")
                if (probe.hookLibraryMapped) record(active, IntegritySignal.HOOK_FRAMEWORK_MAPPED, checkpoint, "native")
                if (probe.debugPortOpen) record(active, IntegritySignal.DEBUGGER_ATTACHED, checkpoint, "native")
            }
        }
    }

    /**
     * يُسجَّل الحكم **مرّة واحدة** في مركز التشخيص المحلي.
     *
     * ولماذا مرّة: `DiagnosticCenter` يزيل التكرار بعدّاد، لكن تسجيل أربع نقاط تحقّق في
     * كل فتح للتطبيق يملأ المخزن الحلقي بضجيج يُخفي أعطالًا حقيقية. والسطر يبقى، لأن
     * المستخدم الذي يرى «تعذّر التشغيل» يجب أن يجد **سببًا** في التشخيص لا صمتًا.
     */
    private fun writeDiagnosticsOnce(context: Context, verdict: IntegrityVerdict) {
        if (diagnosticsWritten) return
        diagnosticsWritten = true
        runCatching {
            val summary = ledger.all().joinToString(", ") { it.signal.name + "/" + it.checkpoint.name }
            DiagnosticCenter.record(
                component = COMPONENT,
                message = "integrity verdict=" + verdict.name + " evidence=[" + summary + "]",
            )
        }
    }

    /** اسم المكوّن في التشخيص — ثابت واحد حتى لا تتفرّق الأسطر في بحث المستخدم. */
    const val COMPONENT = "security"
}
