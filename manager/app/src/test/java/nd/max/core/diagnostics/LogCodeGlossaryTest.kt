/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.diagnostics

import nd.max.core.hardware.GpuHardwareBackend
import nd.max.core.hardware.MemoryUfsHardwareBackend
import nd.max.core.hardware.TouchControlBackend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * القاموس هو شرط «أرسل الملف وحده فيُشخَّص»: رمز بلا شرح يعني سؤالًا آخر لصاحب الجهاز.
 *
 * والفحص هنا لا يقول «الشرح جميل» بل يقول ثلاثة أشياء يسهل إسقاطها: أن **الرموز المبنيّة**
 * تُشرح (`preempted-by-SYSTEM`)، وأن **قرار Atlas المركّب** يُشرح (`blocked-privilege-unavailable`)،
 * وأن **المجهول يبقى مجهولًا** بلا شرح مخترع.
 */
class LogCodeGlossaryTest {

    @Test
    fun `every code emitter in this repository is explained`() {
        // قائمة الرموز التي نعرف أن المحرّك يكتبها فعلًا، مقيسة من مواضع الكتابة لا من الذاكرة.
        val emitted = listOf(
            // PerAppHardwareStatus.Outcome
            "applied", "skipped", "blocked", "unsupported", "not-writable", "not-verified",
            // AppMonitor (أسباب per-app)
            "verified", "profile-is-default", "governor-is-default", "governor-not-advertised",
            "no-lower-advertised-step", "power-profile-above-live",
            "power-profile-never-lowers", "cooling-already-at-or-below-percent",
            "policy-unavailable", "no-gpu-provider", "no-advertised-frequency-range",
            GpuHardwareBackend.BANDWIDTH_MONITOR_REASON,
            // أسباب العدّ على جذر devfreq — تُطبع في البصمة (`backend=devfreq-backend:<reason>`)
            // وتُقرأ في الحزمة، فشرحها شرط «أرسل الملف وحده»
            MemoryUfsHardwareBackend.REASON_ROOT_UNREADABLE,
            MemoryUfsHardwareBackend.REASON_ROOT_ABSENT,
            MemoryUfsHardwareBackend.REASON_NO_DEVICE,
            "provider-disappeared", "unsupported-frequency", "outside-proven-hardware-bounds",
            // GpuCeilingPolicy (شكل تنفيذ السقف وحكمه المقيس)
            "gpu-ceiling-released", "gpu-ceiling-held", "gpu-node-ceiling-unreadable", "gpu-opp-lock-held",
            "gpu-ceiling-open-below-request", "gpu-pinned-at-request",
            "pin-verified", "verified-by-index", "pin-clock-mismatch", "pin-unreadable",
            // AppMonitor (اختياران على مقبض واحد، من إعداد قديم)
            "explicit-frequency-wins-over-profile",
            "live-value-mismatch", "restored", "undecided", "unknown",
            // HardwareControlArbiter
            "manual-lock", "no-winner", "baseline-unreadable", "live-read-unavailable",
            "apply-not-verified-baseline-restored", "apply-not-verified-and-rollback-failed",
            "restore-not-verified", "handoff-awaiting-owner-process",
            // WriteVerification
            "matched", "differs", "write_failed", "unreadable", "write_only_sent",
            // ThermalCeilingRouter
            "route-verified", "ceiling-already-held", "user-ceiling-already-held",
            "ceiling-not-planable", "knob-not-owned", "no-route-transaction",
            "thermal-router-unavailable", "rollback-not-verified",
            // AtlasAdaptiveExecutor
            "route-quarantined-after-unverified-rollback",
            // TouchControlBackend: مسارات اللمس وحكمها
            "touch-sysfs", "vendor-hal", "not-detected",
            TouchControlBackend.VENDOR_CONTRACT_REASON,
            "node-holds-request", "write-backoff", "write-verified", "no-touch-provider",
            "vendor-applied", "vendor-refused", "vendor-already-asked",
            // عدم الإزعاج أثناء اللعب
            "zen-mode-set", "zen-mode-already-on", "zen-mode-write-not-honoured",
            // اللوحة الجانبية: حصيلة مزامنة ملف القائمة
            "written", "already-in-sync", "write-refused", "write-unverified", "no-armed-games",
            "prefs-write-refused", "arm-sync-threw",
        )

        val unexplained = emitted.filter { LogCodeGlossary.explain(it) == null }

        assertTrue("رموز بلا شرح في القاموس: $unexplained", unexplained.isEmpty())
    }

    @Test
    fun `a built code is explained through its template`() {
        assertNotNull(
            "`preempted-by-SYSTEM` رمز يُبنى من اسم المالك، لا رمز ثابت",
            LogCodeGlossary.explain("preempted-by-SYSTEM"),
        )
        assertNotNull(LogCodeGlossary.explain("guard-idle:severe"))
        assertNotNull(
            "رمز التحرير يُلحَق بسقف المنصّة المقيس؛ بلا شرح تسقط اللاحقة وحدها",
            LogCodeGlossary.explain("gpu-ceiling-open-below-request:754000000"),
        )
        assertTrue(
            "القالب يجب أن يذكر المالك وإلا لم يُفهم الرمز",
            LogCodeGlossary.explain("preempted-by-MAX_AI")!!.contains("OWNER"),
        )
    }

    @Test
    fun `an atlas decision composed of status and reason is explained`() {
        val blocked = LogCodeGlossary.explain("blocked-privilege-unavailable")
        val eligible = LogCodeGlossary.explain("eligible-selected")

        assertNotNull("قرار مركّب بلا شرح يترك القارئ يخمّن نصفه", blocked)
        assertTrue(blocked!!.contains("privilege"))
        assertNotNull(eligible)
    }

    @Test
    fun `an unknown code stays unexplained rather than guessed`() {
        assertNull(LogCodeGlossary.explain("something-nobody-wrote"))
        assertNull(LogCodeGlossary.explain(""))
    }

    @Test
    fun `the glossary names the unit of each knob`() {
        val guide = LogCodeGlossary.fieldGuideLines()

        assertTrue(
            "بلا وحدة، 1300000000 و1300000 قد يُقرآن الطلب نفسه",
            guide.any { it.contains("gpu_frequency:DEVICE = Hz") },
        )
        assertTrue(guide.any { it.contains("cpu_limits:POLICY = kHz") })
        assertTrue("وقيمة gpu_profile ليست ترددًا", guide.any { it.startsWith("unit gpu_profile = ") })
    }

    @Test
    fun `every failure code has something to do about it`() {
        // الرموز التي تُرفَق بفشل فعلًا — من `LogVerdict` ومواضع الكتابة. والإصلاح شرط الحزمة
        // التي تُرسَل وحدها: `explain` يقول ما وقع، وهذا يقول ما يُفعل به.
        val failures = listOf(
            "not-verified", "not-writable", "unsupported", "blocked", "restored",
            "differs", "write_failed", "unreadable", "unknown",
            "governor-not-advertised", "policy-unavailable", "no-gpu-provider",
            GpuHardwareBackend.BANDWIDTH_MONITOR_REASON, "gpu-provider-not-writable",
            MemoryUfsHardwareBackend.REASON_ROOT_UNREADABLE,
            MemoryUfsHardwareBackend.REASON_ROOT_ABSENT,
            MemoryUfsHardwareBackend.REASON_NO_DEVICE,
            "no-advertised-frequency-range", "provider-disappeared", "unsupported-frequency",
            "outside-proven-hardware-bounds", "live-value-mismatch", "undecided",
            "gpu-ceiling-held", "gpu-node-ceiling-unreadable", "gpu-opp-lock-held",
            "pin-clock-mismatch", "pin-unreadable",
            "manual-lock", "preempted-by-SYSTEM", "no-winner", "baseline-unreadable",
            "live-read-unavailable", "apply-not-verified-baseline-restored",
            "apply-not-verified-and-rollback-failed", "restore-not-verified",
            "handoff-awaiting-owner-process", "ceiling-not-planable", "knob-not-owned",
            "no-route-transaction", "thermal-router-unavailable", "rollback-not-verified",
            "route-quarantined-after-unverified-rollback", "privilege-unavailable",
            "rollback-unproven", "unit-ambiguous", "provider-ambiguous",
            "goal-unmeasurable", "route-not-reviewed",
            // اللمس وعدم الإزعاج واللوحة الجانبية
            "no-touch-provider", TouchControlBackend.VENDOR_CONTRACT_REASON, "write-backoff",
            "zen-mode-write-not-honoured", "write-refused", "write-unverified",
            "prefs-write-refused", "arm-sync-threw",
        )

        val missing = failures.filter { LogCodeGlossary.remedyOf(it) == null }

        assertTrue("أعطال بلا إصلاح مُرمَّز: $missing", missing.isEmpty())
    }

    @Test
    fun `a healthy code carries no remedy so it is never read as a failure`() {
        // وجود إصلاح لرمز سليم يعني أن التقرير يطالب بإصلاح ما لم يفسد — وهو أسوأ من نقص شرح.
        val healthy = listOf(
            "applied", "verified", "matched", "skipped", "profile-is-default", "governor-is-default",
            "curve-does-not-cap",
            "route-verified", "ceiling-already-held", "user-ceiling-already-held", "selected",
            "gpu-ceiling-released", "pin-verified", "verified-by-index",
            "gpu-ceiling-open-below-request", "gpu-pinned-at-request",
            // اللمس وعدم الإزعاج واللوحة الجانبية: رموز حال لا عطل
            "node-holds-request", "write-verified", "vendor-applied", "vendor-already-asked",
            "zen-mode-set", "zen-mode-already-on", "written", "already-in-sync", "no-armed-games",
        )

        val wrongly = healthy.filter { LogCodeGlossary.remedyOf(it) != null }

        assertTrue("رمز سليم بإصلاح يُوهم بوجود عطل: $wrongly", wrongly.isEmpty())
    }

    @Test
    fun `a composed refusal reaches its remedy through the suffix`() {
        // قرار Atlas يُكتب `STATUS-REASON`، فالإصلاح يُطلب بالرمز المركّب لا المجرّد.
        assertEquals(
            LogCodeGlossary.remedyOf("privilege-unavailable"),
            LogCodeGlossary.remedyOf("blocked-privilege-unavailable"),
        )
        assertEquals(LogCodeGlossary.explain("differs"), LogCodeGlossary.explain("blocked-differs"))
    }

    @Test
    fun `a prefixed detail reaches its remedy through the prefix`() {
        // وسبب مقبض GPU يُكتب `no-gpu-provider:DETAIL` — والتفصيل وحده (`…-equally-proven-…`)
        // ليس رمزًا في القاموس. وبلاحقةٍ لا تُقرأ إلا الأجزاء المعروفة مفصولةً بشرطة، فيُطلب
        // الشرح بالجزء الأول: بدونه بقي سطر «GPU لا يعمل» بلا معنى ولا إصلاح في الحزمة.
        assertEquals(
            LogCodeGlossary.remedyOf("no-gpu-provider"),
            LogCodeGlossary.remedyOf("no-gpu-provider:multiple-equally-proven-gpu-providers"),
        )
        assertEquals(
            LogCodeGlossary.explain("no-gpu-provider"),
            LogCodeGlossary.explain("no-gpu-provider:${GpuHardwareBackend.BANDWIDTH_MONITOR_REASON}"),
        )
        assertEquals(
            LogCodeGlossary.remedyOf("gpu-provider-not-writable"),
            LogCodeGlossary.remedyOf("gpu-provider-not-writable:3d00000.qcom,kgsl-3d0"),
        )
    }

    @Test
    fun `the reading order explains how to find a failure and what to do with it`() {
        assertTrue("خطوات القراءة بلا ذكر الفشل لا تنفع من يستلم الملف", LogCodeGlossary.howToRead.size >= 3)
        assertTrue(
            LogCodeGlossary.howToRead.any { it.contains("not applied") },
        )
        assertTrue(LogCodeGlossary.howToRead.any { it.contains("unit") })
    }

    @Test
    fun `meanings never contain an equals sign that would split a log field`() {
        // تُكتب في السجل بنفس صيغة الحقول، فـ`=` داخل نصّها يُنتج حقلًا زائدًا عند القراءة.
        // وكتلتا الإصلاح والقراءة تُكتبان في الترويسة كذلك، فهما داخل الشرط نفسه.
        val offenders = (LogCodeGlossary.codes + LogCodeGlossary.fieldGuide + LogCodeGlossary.unitGuide +
            LogCodeGlossary.remedies)
            .filter { (_, meaning) -> meaning.contains('=') } +
            LogCodeGlossary.howToRead.filter { it.contains('=') }.map { "howto" to it }

        assertTrue(
            "السطر يُفكَّك على بداية حقل، فمعنى فيه `=` يُنتج حقلًا زائدًا: $offenders",
            offenders.isEmpty(),
        )
    }
}
