/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * قياس منطق السجلّ والسياسة — **بإثباتين متعاكسين لا بإثبات واحد**.
 *
 * العطب الذي تحرسه هذه الاختبارات ليس «ألا يكتشف التعديل»، بل الأدهى منه: **أن يرفض
 * نسخة سليمة**. حارسٌ يعترض على السليم يُنزع من الشجرة في يومين، ثم لا يبقى منها شيء.
 * ولذلك كل قاعدة هنا تُقاس من الوجهين: تُطابق المعدَّل، ولا تُطابق السليم.
 */
class IntegrityLedgerTest {

    private fun finding(
        signal: IntegritySignal,
        checkpoint: IntegrityCheckpoint = IntegrityCheckpoint.PROCESS,
    ) = IntegrityFinding(signal, checkpoint)

    @Test
    fun `a clean ledger is clean and suspects nothing`() {
        val ledger = IntegrityLedger()
        assertEquals(IntegrityVerdict.CLEAN, ledger.verdict())
        assertEquals(0, ledger.all().size)
    }

    @Test
    fun `one hard finding is enough to confirm tampering`() {
        val ledger = IntegrityLedger()
        val verdict = ledger.record(finding(IntegritySignal.SIGNER_MISMATCH))
        assertEquals(IntegrityVerdict.TAMPERED, verdict)
    }

    @Test
    fun `one soft finding only suspects and never refuses`() {
        val ledger = IntegrityLedger()
        assertEquals(IntegrityVerdict.SUSPECTED, ledger.record(finding(IntegritySignal.TRACER_ATTACHED)))
        assertFalse(ledger.verdict() == IntegrityVerdict.TAMPERED)
    }

    @Test
    fun `two distinct soft findings confirm tampering`() {
        val ledger = IntegrityLedger()
        ledger.record(finding(IntegritySignal.TRACER_ATTACHED))
        val verdict = ledger.record(finding(IntegritySignal.HOOK_FRAMEWORK_MAPPED))
        assertEquals(IntegrityVerdict.TAMPERED, verdict)
    }

    /**
     * القاعدة التي تحمي المستخدم السليم: سطح هوك مثبَّت لتطبيق آخر يُرى في أربعة مواضع
     * تحقّق — ولو عُدّت المواضع أدلّة لصار «مخترقًا» بلا ذنب.
     */
    @Test
    fun `the same signal at four checkpoints stays a single piece of evidence`() {
        val ledger = IntegrityLedger()
        IntegrityCheckpoint.entries.forEach { checkpoint ->
            ledger.record(finding(IntegritySignal.HOOK_FRAMEWORK_MAPPED, checkpoint))
        }
        assertEquals(IntegrityVerdict.SUSPECTED, ledger.verdict())
        assertEquals(IntegrityCheckpoint.entries.size, ledger.all().size)
    }

    @Test
    fun `the verdict is sticky and never falls back`() {
        val ledger = IntegrityLedger(softThreshold = 2)
        ledger.record(finding(IntegritySignal.TRACER_ATTACHED))
        ledger.record(finding(IntegritySignal.HOOK_FRAMEWORK_MAPPED))
        assertEquals(IntegrityVerdict.TAMPERED, ledger.verdict())
        // إشارة جديدة لا تُنقص شيئًا، والسجلّ لا يُصفَّر بالاستمرار.
        ledger.record(finding(IntegritySignal.DEBUGGER_ATTACHED))
        assertEquals(IntegrityVerdict.TAMPERED, ledger.verdict())
    }

    @Test
    fun `a repeated identical finding is stored once`() {
        val ledger = IntegrityLedger()
        repeat(5) { ledger.record(finding(IntegritySignal.DEBUGGER_ATTACHED)) }
        assertEquals(1, ledger.all().size)
    }

    /**
     * القائمة الحمراء معكوسة: الحالات المشروعة **لا تُنتج دليلًا**. وهذه هي المادة التي
     * يطلبها المالك صراحةً (لا يُمنع التطبيق لجذور أو KernelSU أو روم مخصّص)، فتُقاس
     * بدل أن تُوصف — وإلّا صارت وعدًا في تعليق.
     */
    @Test
    fun `the never-evidence list maps to no signal at all`() {
        assertTrue(IntegrityLedger.NEVER_EVIDENCE.isNotEmpty())
        val signals = IntegritySignal.entries.map { it.name.lowercase() }
        IntegrityLedger.NEVER_EVIDENCE.forEach { tolerable ->
            // لا إشارة واحدة في العدّاد تحمل اسمًا من هذه القائمة: التشابه يُكشف،
            // وعليه لو أُضيفت إشارة «جذور» يومًا لسقط هذا الاختبار بالاسم.
            val clash = signals.filter { it.contains(tolerable.substringBefore('-')) }
            assertTrue("state '$tolerable' must not map to $clash", clash.isEmpty())
        }
    }

    @Test
    fun `hard signals are exactly the ones a clean run cannot produce`() {
        // كل تعديل على الحزمة ⇒ إشارة قاطعة، وكل قياس للحظة التشغيل ⇒ إشارة تُصدَّق.
        assertTrue(IntegritySignal.SIGNER_MISMATCH.hard)
        assertTrue(IntegritySignal.APK_CONTENT_MISMATCH.hard)
        assertTrue(IntegritySignal.SEALED_ASSET_MISMATCH.hard)
        assertTrue(IntegritySignal.NATIVE_IMAGE_MISMATCH.hard)
        assertFalse(IntegritySignal.TRACER_ATTACHED.hard)
        assertFalse(IntegritySignal.HOOK_FRAMEWORK_MAPPED.hard)
    }
}

/**
 * السياسة: أيّ إشارة تُحتسب ومتى يُرفض. وكل تأكيد هنا يُقابله تأكيد معاكس، لأن هذا
 * الملفّ هو الذي يمنع «تعطيل التطبيق على أجهزة سليمة».
 */
class IntegrityPolicyTest {

    @Test
    fun `a debug build never refuses`() {
        val ledger = IntegrityLedger()
        ledger.record(IntegrityFinding(IntegritySignal.SIGNER_MISMATCH, IntegrityCheckpoint.PROCESS))
        val debug = IntegrityPolicy(enforce = false, debuggable = true)
        assertEquals(IntegrityVerdict.TAMPERED, ledger.verdict())
        assertFalse("debug must observe, not refuse", debug.refuses(ledger))
    }

    @Test
    fun `a release build refuses on confirmed tampering only`() {
        val release = IntegrityPolicy(enforce = true, debuggable = false)
        val clean = IntegrityLedger()
        assertFalse(release.refuses(clean))
        val suspected = IntegrityLedger()
        suspected.record(IntegrityFinding(IntegritySignal.TRACER_ATTACHED, IntegrityCheckpoint.PROCESS))
        assertFalse("suspicion alone never refuses", release.refuses(suspected))
        val confirmed = IntegrityLedger()
        confirmed.record(IntegrityFinding(IntegritySignal.SIGNER_MISMATCH, IntegrityCheckpoint.PROCESS))
        assertTrue(release.refuses(confirmed))
    }

    /**
     * العطب الذي وقع فعلًا في تصميم هذه الطبقة قبل أن يُصلَح: مصحّح مرتبط بعملية هو
     * الوضع **الطبيعي** في بناء debug، فأربع نقاط تحقّق كانت تجعل المطوّر «مخترقًا» بنفسه.
     */
    @Test
    fun `an attached debugger is expected in a debuggable build and counted in a release build`() {
        val debuggable = IntegrityPolicy(enforce = false, debuggable = true)
        val release = IntegrityPolicy(enforce = true, debuggable = false)
        assertFalse(debuggable.counts(IntegritySignal.DEBUGGER_ATTACHED))
        assertFalse(debuggable.counts(IntegritySignal.TRACER_ATTACHED))
        assertTrue(release.counts(IntegritySignal.DEBUGGER_ATTACHED))
        assertTrue(release.counts(IntegritySignal.TRACER_ATTACHED))
    }

    @Test
    fun `package integrity signals are counted in both build kinds`() {
        val debuggable = IntegrityPolicy(enforce = false, debuggable = true)
        assertTrue(debuggable.counts(IntegritySignal.SIGNER_MISMATCH))
        assertTrue(debuggable.counts(IntegritySignal.APK_CONTENT_MISMATCH))
        assertTrue(debuggable.counts(IntegritySignal.SEALED_ASSET_MISMATCH))
        assertTrue(debuggable.counts(IntegritySignal.NATIVE_IMAGE_MISMATCH))
    }
}

/** تشفير/فكّ سداسي ومقارنة ثابتة الزمن: أخطاؤها صامتة تمامًا، فتُقاس. */
class IntegrityDigestTest {

    @Test
    fun `hex round-trips and rejects anything malformed`() {
        val bytes = ByteArray(32) { (it * 7).toByte() }
        assertEquals(bytes.toList(), IntegrityDigest.decode(IntegrityDigest.encode(bytes))?.toList())
        assertNull(IntegrityDigest.decode(""))
        assertNull(IntegrityDigest.decode(null))
        assertNull(IntegrityDigest.decode("abc"))
        assertNull(IntegrityDigest.decode("zz"))
    }

    @Test
    fun `masking is reversible and depends on the mask`() {
        val digest = ByteArray(32) { (it + 1).toByte() }
        val mask = byteArrayOf(0x9e.toByte(), 0x7c, 0x1a, 0x34, 0xc0.toByte(), 0x5f, 0x2b, 0x68)
        val masked = ByteArray(32) { (digest[it].toInt() xor mask[it % mask.size].toInt()).toByte() }
        assertFalse(IntegrityDigest.encode(digest) == IntegrityDigest.encode(masked))
        assertEquals(
            digest.toList(),
            IntegrityDigest.decodeMasked(IntegrityDigest.encode(masked), mask)?.toList(),
        )
    }

    @Test
    fun `constant-time comparison is about content not identity`() {
        val a = ByteArray(32) { 3 }
        val b = ByteArray(32) { 3 }
        val c = ByteArray(32) { 4 }
        assertTrue(IntegrityDigest.constantTimeEquals(a, b))
        assertFalse(IntegrityDigest.constantTimeEquals(a, c))
        assertFalse(IntegrityDigest.constantTimeEquals(a, ByteArray(31) { 3 }))
        assertFalse(IntegrityDigest.constantTimeEquals(a, null))
        assertFalse(IntegrityDigest.constantTimeEquals(null, null))
    }

    @Test
    fun `sha256 matches the published vector`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            IntegrityDigest.encode(IntegrityDigest.sha256Of("abc".toByteArray())),
        )
    }
}
