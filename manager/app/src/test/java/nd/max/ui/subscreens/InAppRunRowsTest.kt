/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import nd.max.R
import nd.max.core.gamespace.InAppKnob
import nd.max.core.gamespace.InAppRun
import nd.max.core.gamespace.InAppRunReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **ما يُقال عن آخر تطبيق يُقرَّر قبل أن يُرسم.**
 *
 * الصفوف هنا هي الجواب عن سؤال المستخدم: «لماذا لم يتغيّر شيء؟». فتُقاس الترتيب والحالات
 * كلها في JVM، ويبقى الـ`Composable` رسمًا لا قرارًا.
 */
class InAppRunRowsTest {

    @Test
    fun `no record is said as no record`() {
        assertEquals(listOf<InAppRunRow>(InAppRunRow.Never), inAppRunRows(null))
    }

    @Test
    fun `a blocked run says the path was not ready, before anything else`() {
        val rows = inAppRunRows(
            InAppRun(pkg = "a.b", writes = 0, written = 0, verified = 0, blocked = "lane-unavailable"),
        )
        assertEquals(InAppRunRow.Blocked(InAppRunReason.LANE_UNAVAILABLE), rows.first())
    }

    @Test
    fun `a partial write is not reported as complete`() {
        val rows = inAppRunRows(InAppRun(pkg = "a.b", writes = 4, written = 3, verified = 2))
        val applied = rows.single() as InAppRunRow.Applied
        assertEquals(3, applied.written)
        assertEquals(4, applied.total)
        assertEquals(2, applied.verified)
        assertFalse("تشغيل ناقص عُرض مكتملًا", applied.complete)
    }

    @Test
    fun `a fully verified write is reported as complete`() {
        val applied = inAppRunRows(InAppRun(pkg = "a.b", writes = 2, written = 2, verified = 2))
            .single() as InAppRunRow.Applied
        assertTrue(applied.complete)
    }

    @Test
    fun `a skipped knob is named with its reason, in enum order`() {
        val rows = inAppRunRows(
            InAppRun(
                pkg = "a.b",
                writes = 0,
                written = 0,
                verified = 0,
                skipped = mapOf(
                    InAppKnob.HAPTICS to "already-off",
                    InAppKnob.DND to "zen-mode-already-on",
                ),
            ),
        )
        // لا «لا شيء مضبوط» حين يوجد مفتاح مطلوب ورُفض: الرسالتان تتناقضان.
        assertFalse(rows.contains(InAppRunRow.NothingAsked))
        val skipped = rows.filterIsInstance<InAppRunRow.Skipped>()
        assertEquals(listOf(InAppKnob.DND, InAppKnob.HAPTICS), skipped.map { it.knob })
        assertEquals(InAppRunReason.ALREADY_OFF, skipped.last().reason)
    }

    @Test
    fun `nothing asked is its own answer, not a failure`() {
        val rows = inAppRunRows(InAppRun(pkg = "a.b", writes = 0, written = 0, verified = 0))
        assertEquals(listOf<InAppRunRow>(InAppRunRow.NothingAsked), rows)
    }

    @Test
    fun `a skipped reason from another version is shown as unknown, never hidden`() {
        val rows = inAppRunRows(
            InAppRun(
                pkg = "a.b",
                writes = 0,
                written = 0,
                verified = 0,
                skipped = mapOf(InAppKnob.DND to "something-new"),
            ),
        )
        val skipped = rows.single() as InAppRunRow.Skipped
        assertEquals(InAppRunReason.UNKNOWN, skipped.reason)
    }

    @Test
    fun `a launch with no record and nothing asked says nothing`() {
        assertNull(inAppReceipt(null))
        assertNull(inAppReceipt(InAppRun(pkg = "a.b", writes = 0, written = 0, verified = 0)))
    }

    @Test
    fun `a blocked launch is the receipt, not a silent panel`() {
        val receipt = inAppReceipt(
            InAppRun(pkg = "a.b", writes = 0, written = 0, verified = 0, blocked = "lane-unavailable"),
        )
        assertEquals(InAppRunRow.Blocked(InAppRunReason.LANE_UNAVAILABLE), receipt)
    }

    @Test
    fun `an applied launch carries the real counters`() {
        val receipt = inAppReceipt(InAppRun(pkg = "a.b", writes = 4, written = 3, verified = 2))
        val applied = receipt as InAppRunRow.Applied
        assertEquals(3, applied.written)
        assertEquals(4, applied.total)
        assertEquals(2, applied.verified)
        assertFalse("إيصال ناقص عُرض مكتملًا", applied.complete)
    }

    @Test
    fun `a refused knob is news even when nothing was written`() {
        val receipt = inAppReceipt(
            InAppRun(
                pkg = "a.b",
                writes = 0,
                written = 0,
                verified = 0,
                skipped = mapOf(InAppKnob.DND to "zen-mode-already-on"),
            ),
        )
        assertEquals(InAppRunRow.Skipped(InAppKnob.DND, InAppRunReason.ZEN_ALREADY_ON), receipt)
    }

    @Test
    fun `every knob and every reason has its own string`() {
        assertEquals(R.string.profile_knob_dnd, knobLabelRes(InAppKnob.DND))
        assertEquals(R.string.profile_knob_refresh, knobLabelRes(InAppKnob.REFRESH_RATE))
        assertEquals(R.string.profile_knob_haptics, knobLabelRes(InAppKnob.HAPTICS))
        assertEquals(
            R.string.profile_run_reason_zen_already_on,
            reasonTextRes(InAppRunReason.ZEN_ALREADY_ON),
        )
        assertEquals(
            R.string.profile_run_reason_snapshot_unsafe,
            reasonTextRes(InAppRunReason.SNAPSHOT_UNSAFE),
        )
        assertEquals(R.string.profile_run_reason_unknown, reasonTextRes(InAppRunReason.UNKNOWN))
    }
}
