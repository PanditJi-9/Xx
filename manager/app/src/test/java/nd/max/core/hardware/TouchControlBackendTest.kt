/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * مالك اللمس: الاكتشاف، والقدرة، ومحرّك التسوية — **مقيسًا بلا جهاز**.
 *
 * ولماذا هذه المجموعة هي جوهر الإصلاح: كانت الحالات الثلاث التي رآها المستخدم على جهازه
 * (POCO X6 Pro · AxionOS · ٢٠٢٦-١٠-١٥) — «المفتاح لا يظهر»، «المفتاح يظهر ولا أثر»، «الروم يعلن
 * خدمة لمس ونحن لا نكتب فيها» — **غير قابلة للقياس في أي مكان**: القوائم كانت مكرّرة في ثلاث
 * طبقات، والكتابة بلا قراءة، والحكم بلا سطر. فصارت هنا دوالّ مُعلمة بمُوجّه وهمي وساعة مُستبدة،
 * فتُقاس الحالات كلها قبل أي جهاز.
 */
class TouchControlBackendTest {

    /**
     * عتاد وهمي: قيم العُقَد، وأيّها يقبل الكتابة، وسلوك الكتابة نفسه.
     *
     * و[forcedOutcome] يحاكي جهازًا **يستقبل الكتابة ثم لا تثبت القيمة** (`DIFFERS`) أو عقدة
     * تُكتب ولا تُقرأ (`READBACK_UNAVAILABLE`) — وهما حكمان مختلفان عن «فشل الأمر» ولا يجوز
     * أن يُطويا معه.
     */
    private class FakeAccess(
        private val values: MutableMap<String, String> = mutableMapOf(),
        private val writable: Set<String> = emptySet(),
        private val forcedOutcome: WriteVerification.Outcome? = null,
    ) : TouchControlBackend.Access {
        val writes = mutableListOf<Pair<String, String>>()

        override fun read(path: String): String? = values[path]
        override fun exists(path: String): Boolean = path in values
        override fun writable(path: String): Boolean = path in writable

        override fun writeVerified(path: String, value: String): WriteVerification.Outcome {
            writes += path to value
            if (path !in writable) return WriteVerification.Outcome.WRITE_FAILED
            if (forcedOutcome != null) return forcedOutcome
            values[path] = value
            return WriteVerification.Outcome.MATCHED
        }
    }

    private class FakeClock(var now: Long = 1_000_000L) : TouchControlBackend.Clock {
        override fun nowMs(): Long = now
    }

    private object DeclaredOnly : TouchControlBackend.VendorBridge {
        override fun transportPresent(): Boolean = true
        override val hasWritePath: Boolean = false
        override fun apply(boost: Boolean): Boolean? = null
    }

    private class WritableVendor(private val accept: Boolean = true) : TouchControlBackend.VendorBridge {
        var asks = 0
            private set

        override fun transportPresent(): Boolean = true
        override val hasWritePath: Boolean = true
        override fun apply(boost: Boolean): Boolean? {
            asks++
            return accept
        }
    }

    private val gameNode = "/proc/touchpanel/game_switch_enable"
    private val altNode = "/proc/touch_boost/enable"
    private val dt2wNode = "/proc/touchpanel/double_tap_enable"

    // ── الاكتشاف ─────────────────────────────────────────────────────────────────────────────

    @Test fun theGameModeNodeWinsByListOrderNotByExistence() {
        // لو قُرئت العقدتان، فالمكتوبة في المقدّمة هي التي تُستعمل — وإلا كُتب في واحدة وقُرئت
        // الأخرى فظهر «لا أثر» مع أن الكتابة نجحت في مكان آخر.
        val access = FakeAccess(mutableMapOf(gameNode to "0", altNode to "0"))
        val found = TouchControlBackend.discover(access, TouchControlBackend.NoVendorBridge)

        assertEquals(gameNode, found.gameMode?.path)
        assertTrue(found.hasBoostNode)
        assertEquals("touch-sysfs", found.reason)
        // وعقدة تعزيز الإدخال تُرصد معه لا تُطمس: عقدتان مختلفتان، والأولى تُكتب وحدها.
        assertEquals(altNode, found.inputBoost?.path)
    }

    @Test fun inputBoostKeepsItsOwnKindAndIsNotCalledAGameMode() {
        // الجهاز المقيس (Redmi rodin · MT6899): النواة تعرض `/proc/touch_boost/enable` وحده، وهو
        // مُعزّز تردّد CPU لا نمط لعب. وكان يُعرض باسم «وضع الألعاب» ويُوصف بمعدّل استطلاع لا
        // يملكه — فصار نوعه `INPUT_BOOST`، والدليل يسمّيه، والواجهة تشرح أثره الحقيقي.
        val access = FakeAccess(mutableMapOf(altNode to "0"), writable = setOf(altNode))
        val found = TouchControlBackend.discover(access, TouchControlBackend.NoVendorBridge)

        assertNull(found.gameMode)
        assertEquals(altNode, found.inputBoost?.path)
        assertEquals(TouchControlBackend.Kind.INPUT_BOOST, found.inputBoost?.kind)
        assertEquals(found.inputBoost, found.boostProvider)
        assertTrue(found.hasBoostNode)
        assertEquals("touch-sysfs", found.reason)
        assertTrue(found.evidence.contains("input-boost:$altNode"))

        val capability = TouchControlBackend.capability(access, TouchControlBackend.NoVendorBridge)
        assertEquals(AccessLevel.READ_WRITE, capability.access)
    }

    @Test fun theTouchBoostNodeSolelyDrivesBoostOnADeviceWithoutADigitizerSwitch() {
        // محرّك التسوية يقبل مزوّد تعزيز الإدخال: الكتابة فيه مقيسة (write + read-back) كأي عقدة.
        val access = FakeAccess(mutableMapOf(altNode to "0"), writable = setOf(altNode))
        val attempt = TouchControlBackend.Reconciler(
            access,
            vendor = { TouchControlBackend.NoVendorBridge },
        ).reconcile(true)

        assertEquals(TouchControlBackend.Result.APPLIED, attempt.result)
        assertEquals(altNode, attempt.path)
        assertEquals("write-verified", attempt.reason)
    }

    @Test fun aNodeThatCannotBeReadIsNotANodeOfControl() {
        // الوجود ليس قراءة: مسار يُعلن وجوده ولا يُقرأ لا يُنسب إليه تحكّم، ولا يُقال للمستخدم
        // إن مفتاحه بيد kernel لا يجيب.
        val found = TouchControlBackend.discover(FakeAccess(), TouchControlBackend.NoVendorBridge)

        assertNull(found.gameMode)
        assertFalse(found.hasBoostNode)
        assertFalse(found.hasAnyNode)
        assertEquals("not-detected", found.reason)
    }

    @Test fun sampleRateStaysEmptyUntilANodeIsProven() {
        // الفارغ **مقصود** ولا يُملأ بأسماء مخمّنة: معنى القيمة (هرتز؟ فهرس؟ مرمّز سائق؟) غير
        // مثبَت، فالمفتاح لا يُعرض بدل أن يُعرض ولا يفعل شيئًا معروفًا.
        assertTrue(TouchControlBackend.SAMPLE_RATE_NODES.isEmpty())
        assertEquals(
            TouchControlBackend.GAME_MODE_NODES + TouchControlBackend.INPUT_BOOST_NODES,
            TouchControlBackend.BOOST_NODES,
        )
    }

    @Test fun doubleTapIsDiscoveredIndependentlyOfBoost() {
        val access = FakeAccess(mutableMapOf(dt2wNode to "0"))
        val found = TouchControlBackend.discover(access, TouchControlBackend.NoVendorBridge)

        assertEquals(dt2wNode, found.doubleTap?.path)
        assertFalse("عقدة النقر المزدوج لا تُنسب إلى تعزيز اللمس", found.hasBoostNode)
        assertTrue(found.hasAnyNode)
        assertTrue(found.evidence.contains("double-tap-wake:$dt2wNode"))
    }

    @Test fun theDeclaredVendorTransportIsNamedButNotCountedAsAProvider() {
        // الحال المقيسة على جهاز المستخدم: الخدمة معلنة، ولا طريق كتابة مثبَت. فالدليل يذكرها
        // (لئلا يُظنّ أنها لم تُرصد) والحال تسمّي رفض العقد باسمه.
        val found = TouchControlBackend.discover(FakeAccess(), DeclaredOnly)

        assertTrue(found.vendorTransportPresent)
        assertFalse(found.vendorHasWritePath)
        assertTrue(found.evidence.contains(TouchControlBackend.VENDOR_TRANSPORT_EVIDENCE))
        assertEquals(TouchControlBackend.VENDOR_CONTRACT_REASON, found.reason)
    }

    // ── القدرة ───────────────────────────────────────────────────────────────────────────────

    @Test fun aWritableNodeIsReadWriteAndAnUnwritableOneIsReadOnly() {
        val writable = TouchControlBackend.capability(
            FakeAccess(mutableMapOf(gameNode to "0"), writable = setOf(gameNode)),
            TouchControlBackend.NoVendorBridge,
        )
        assertEquals(AccessLevel.READ_WRITE, writable.access)
        assertEquals(HardwareFeature.TOUCH_CONTROL, writable.feature)
        assertEquals("touch-backend:touch-sysfs", writable.backend)

        val readOnly = TouchControlBackend.capability(
            FakeAccess(mutableMapOf(gameNode to "0")),
            TouchControlBackend.NoVendorBridge,
        )
        assertEquals(AccessLevel.READ_ONLY, readOnly.access)
    }

    @Test fun aReadableNodeThatRefusesWritesIsNotASwitchTheUserCanKeepOn() {
        // العطب المُبلَّغ عنه: الشاشة تُفعّل المفتاح بمجرّد قراءة العقدة، فيُلمس ويرتدّ صامتًا
        // على جهاز بلا جذر. الحكم هنا هو نفس ما تُبنى عليه الواجهة: قابلية الكتابة لا الوجود.
        val readableOnly = FakeAccess(mutableMapOf(gameNode to "0"))
        assertFalse(TouchControlBackend.writableProvider(TouchControlBackend.discover(readableOnly), readableOnly))

        val writable = FakeAccess(mutableMapOf(gameNode to "0"), writable = setOf(gameNode))
        assertTrue(TouchControlBackend.writableProvider(TouchControlBackend.discover(writable), writable))
    }

    @Test fun doubleTapAloneStillMakesTheDeviceReadable() {
        // جهاز لا نمط فيه ولا معدّل، لكنه يُوقَظ بلمسة مزدوجة: القدرة `READ_ONLY` لا `NONE` —
        // والحكم `NONE` كان يقول «لا تحكّم» عن جهاز فيه تحكّم.
        val capability = TouchControlBackend.capability(
            FakeAccess(mutableMapOf(dt2wNode to "0")),
            TouchControlBackend.NoVendorBridge,
        )

        assertEquals(AccessLevel.READ_ONLY, capability.access)
    }

    @Test fun declaredTransportWithoutAWritePathIsNoneWithItsOwnReason() {
        val capability = TouchControlBackend.capability(FakeAccess(), DeclaredOnly)

        assertEquals(AccessLevel.NONE, capability.access)
        assertEquals("touch-backend:${TouchControlBackend.VENDOR_CONTRACT_REASON}", capability.backend)
        assertTrue(capability.evidence.contains(TouchControlBackend.VENDOR_TRANSPORT_EVIDENCE))
    }

    // ── محرّك التسوية ────────────────────────────────────────────────────────────────────────

    @Test fun aNodeAlreadyHoldingTheRequestIsLeftAlone() {
        val access = FakeAccess(mutableMapOf(gameNode to "1"))
        val reconciler = TouchControlBackend.Reconciler(access, { TouchControlBackend.NoVendorBridge }, FakeClock())

        val attempt = reconciler.reconcile(true)

        assertEquals(TouchControlBackend.Result.DEFERRED, attempt.result)
        assertEquals(TouchControlBackend.Channel.SYSFS, attempt.channel)
        assertEquals("node-holds-request", attempt.reason)
        assertEquals("1", attempt.live)
        assertTrue("لا كتابة على عقدة تحمل المطلوب", access.writes.isEmpty())
        assertTrue(attempt.persistable)
    }

    @Test fun driftIsWrittenAndVerifiedWithTheLiveValueInEvidence() {
        val access = FakeAccess(mutableMapOf(gameNode to "0"), writable = setOf(gameNode))
        val reconciler = TouchControlBackend.Reconciler(access, { TouchControlBackend.NoVendorBridge }, FakeClock())

        val attempt = reconciler.reconcile(true)

        assertEquals(TouchControlBackend.Result.APPLIED, attempt.result)
        assertEquals("write-verified", attempt.reason)
        assertEquals(gameNode, attempt.path)
        assertEquals("1", attempt.live)
        assertEquals(listOf(gameNode to "1"), access.writes)
    }

    @Test fun aRejectedWriteIsRefusedThenHeldOffBeforeItIsRetried() {
        // العطب المقيس: العقدة التي لا تُكتب كانت تُكتب كل ٥٠٠ م.ث (٣٠١٦ سطرًا في ٣٦ دقيقة، ٩٤%
        // من ملف السجل). فالرفض يُثبَّت معه **مهلة** كاملة، والقيمة الحيّة تُقرأ مرّة واحدة لتشرح
        // ما وجده النظام فعلًا حين رفض.
        val access = FakeAccess(mutableMapOf(gameNode to "0"))
        val clock = FakeClock()
        val reconciler = TouchControlBackend.Reconciler(access, { TouchControlBackend.NoVendorBridge }, clock)

        val refused = reconciler.reconcile(true)
        assertEquals(TouchControlBackend.Result.REFUSED, refused.result)
        assertEquals("write_failed", refused.reason)
        assertEquals("0", refused.live)
        assertFalse(refused.persistable)

        val heldOff = reconciler.reconcile(true)
        assertEquals(TouchControlBackend.Result.DEFERRED, heldOff.result)
        assertEquals("write-backoff", heldOff.reason)
        assertEquals(1, access.writes.size)

        // وبعد المهلة يُعاد السؤال مرّة واحدة — لا فورًا ولا أبدًا.
        clock.now += TouchControlBackend.RETRY_BACKOFF_MS
        val retried = reconciler.reconcile(true)
        assertEquals(TouchControlBackend.Result.REFUSED, retried.result)
        assertEquals(2, access.writes.size)
    }

    @Test fun aDeviceThatAcceptsThenKeepsAnotherValueIsRefusedWithItsVerdict() {
        val access = FakeAccess(
            mutableMapOf(gameNode to "0"),
            writable = setOf(gameNode),
            forcedOutcome = WriteVerification.Outcome.DIFFERS,
        )
        val reconciler = TouchControlBackend.Reconciler(access, { TouchControlBackend.NoVendorBridge }, FakeClock())

        val attempt = reconciler.reconcile(true)

        assertEquals(TouchControlBackend.Result.REFUSED, attempt.result)
        assertEquals("differs", attempt.reason)
        assertEquals("0", attempt.live)
    }

    @Test fun withoutAnyProviderTheAbsenceIsNamed() {
        val reconciler = TouchControlBackend.Reconciler(FakeAccess(), { TouchControlBackend.NoVendorBridge }, FakeClock())

        val attempt = reconciler.reconcile(true)

        assertEquals(TouchControlBackend.Result.UNAVAILABLE, attempt.result)
        assertEquals(TouchControlBackend.Channel.NONE, attempt.channel)
        assertEquals("no-touch-provider", attempt.reason)
        assertNull(attempt.path)
    }

    @Test fun aDeclaredButUnprovenVendorContractIsRefusedByContractNotByFailure() {
        // وهو سطر جهاز المستخدم نفسه: لا عقدة، والخدمة معلنة، والنمط غير مثبَت ⇒ `unavailable`
        // على قناة المصنّع برمز العقد — ولا تُنادى كتابة تخمينية إلى HAL اللمس.
        val reconciler = TouchControlBackend.Reconciler(FakeAccess(), { DeclaredOnly }, FakeClock())

        val attempt = reconciler.reconcile(true)

        assertEquals(TouchControlBackend.Result.UNAVAILABLE, attempt.result)
        assertEquals(TouchControlBackend.Channel.VENDOR, attempt.channel)
        assertEquals(TouchControlBackend.VENDOR_CONTRACT_REASON, attempt.reason)
        assertFalse(attempt.persistable)
    }

    @Test fun aVendorWithAProvenWritePathAppliesOnceThenHoldsTheRequest() {
        val vendor = WritableVendor()
        val reconciler = TouchControlBackend.Reconciler(FakeAccess(), { vendor }, FakeClock())

        val applied = reconciler.reconcile(true)
        assertEquals(TouchControlBackend.Result.APPLIED, applied.result)
        assertEquals("vendor-applied", applied.reason)
        assertEquals(1, vendor.asks)

        val again = reconciler.reconcile(true)
        assertEquals(TouchControlBackend.Result.DEFERRED, again.result)
        assertEquals("vendor-already-asked", again.reason)
        assertEquals("ولا يُسأل المحوّل بالطلب نفسه مرّتين", 1, vendor.asks)
    }

    @Test fun aVendorThatRejectsTheRequestIsRefusedNotSilentlySwallowed() {
        val vendor = WritableVendor(accept = false)
        val reconciler = TouchControlBackend.Reconciler(FakeAccess(), { vendor }, FakeClock())

        val attempt = reconciler.reconcile(true)

        assertEquals(TouchControlBackend.Result.REFUSED, attempt.result)
        assertEquals("vendor-refused", attempt.reason)
        assertFalse(attempt.persistable)
    }

    @Test fun doubleTapReconcilesOnItsOwnLineWithVerification() {
        val access = FakeAccess(mutableMapOf(dt2wNode to "0"), writable = setOf(dt2wNode))
        val reconciler = TouchControlBackend.Reconciler(access, { TouchControlBackend.NoVendorBridge }, FakeClock())

        assertTrue(reconciler.reconcileDoubleTap(true))
        assertEquals(listOf(dt2wNode to "1"), access.writes)

        // وعقدة تحمل المطلوب لا تُكتب ثانية، وعقدة غائبة تعود `false` بلا كتابة.
        assertTrue(reconciler.reconcileDoubleTap(true))
        assertEquals(1, access.writes.size)
        assertFalse(reconciler.reconcileDoubleTap(false, TouchControlBackend.discover(FakeAccess(), TouchControlBackend.NoVendorBridge)))
    }

    @Test fun theDefaultVendorBridgeClaimsNothing() {
        // ولا يُدَّعى نقلٌ لم يُقَس: الجسر الافتراضي يقول «لا شيء» حتى تُركَّب تركيبة تقيس.
        assertFalse(TouchControlBackend.NoVendorBridge.transportPresent())
        assertFalse(TouchControlBackend.NoVendorBridge.hasWritePath)
        assertNull(TouchControlBackend.NoVendorBridge.apply(true))
    }
}
