/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.license

import nd.max.core.remote.TestKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حكم الحالة: سارٍ/منتهٍ — **مع مانع رجوع الساعة** الذي هو أهمّ ما يُقاس هنا.
 *
 * والزمن مُدخل دائمًا (`nowMillis`) فلا يُقرأ من ساعة الجهاز في اختبار — وهذا ما يجعل «ساعة
 * رُجعت للوراء» حالةً قابلة للقياس بدل أن تكون قصة.
 */
class LicenseStateTest {

    private val server = TestKeys.generate()

    @Test
    fun `no record means no licence at all`() {
        val state = LicenseStates.evaluate(null, LicenseTestSupport.NOW)
        assertEquals(LicenseStatus.NONE, state.status)
        assertFalse(state.active)
        assertNull(state.expiresAtMillis)
        assertNull(state.remainingMillis)
        assertNull(LicenseStates.remainingDays(state))
    }

    @Test
    fun `an accepted certificate reads as active with a human remaining count`() {
        val record = LicenseTestSupport.record(LicenseTestSupport.payload(), server)
        val state = LicenseStates.evaluate(record, LicenseTestSupport.NOW)
        assertEquals(LicenseStatus.ACTIVE, state.status)
        assertEquals("pro", state.plan)
        assertEquals(LicenseTestSupport.LICENSE_ID, state.licenseId)
        assertEquals(30L * LicenseTestSupport.DAY, state.remainingMillis)
        assertEquals(30L, LicenseStates.remainingDays(state))
        assertEquals(1, state.deviceLimit)
        assertEquals(1, state.devicesUsed)
    }

    @Test
    fun `the remaining count rounds up so that a day left reads as a day`() {
        val almost = LicenseState(remainingMillis = 1L)
        assertEquals(1L, LicenseStates.remainingDays(almost))
        val exact = LicenseState(remainingMillis = LicenseTestSupport.DAY)
        assertEquals(1L, LicenseStates.remainingDays(exact))
        val over = LicenseState(remainingMillis = LicenseTestSupport.DAY + 1)
        assertEquals(2L, LicenseStates.remainingDays(over))
    }

    @Test
    fun `the trusted clock never moves backwards`() {
        val record = LicenseTestSupport.record(LicenseTestSupport.payload(), server)
        val rewound = LicenseTestSupport.NOW - 10 * LicenseTestSupport.DAY
        assertEquals(
            "ساعةٌ رُجعت لا تُنزل الزمن الموثوق خطوةً واحدة",
            record.serverMillis,
            LicenseStates.trustedNow(record, rewound),
        )
        assertEquals(
            "والساعة المتقدّمة تُحترم (ضررٌ على صاحبها لا مكسب)",
            LicenseTestSupport.NOW + LicenseTestSupport.DAY,
            LicenseStates.trustedNow(record, LicenseTestSupport.NOW + LicenseTestSupport.DAY),
        )
    }

    @Test
    fun `a key that expired stays expired after the clock is rewound`() {
        val record = LicenseTestSupport.record(
            LicenseTestSupport.payload(expiresAt = LicenseTestSupport.iso(LicenseTestSupport.NOW + 60_000)),
            server,
        )
        // وصل الخادم بعد الانتهاء (رفضٌ يحمل زمنه) فثُبّتت المرساة بعده — ثم أرجع المستخدم ساعته.
        val anchored = LicenseStates.anchored(
            record,
            serverMillis = LicenseTestSupport.NOW + 2 * 60_000,
            nowMillis = LicenseTestSupport.NOW + 2 * 60_000,
        )
        val state = LicenseStates.evaluate(anchored, LicenseTestSupport.NOW - 5 * LicenseTestSupport.DAY)
        assertEquals(LicenseStatus.EXPIRED, state.status)
        assertEquals(0L, state.remainingMillis)
    }

    @Test
    fun `the anchor only ever moves forward`() {
        val record = LicenseTestSupport.record(LicenseTestSupport.payload(), server)
        val backwards = LicenseStates.anchored(record, serverMillis = 0L, nowMillis = LicenseTestSupport.NOW)
        assertEquals(record.serverMillis, backwards.serverMillis)
    }

    @Test
    fun `an expired certificate is reported expired with nothing left`() {
        val record = LicenseTestSupport.record(
            LicenseTestSupport.payload(expiresAt = LicenseTestSupport.iso(LicenseTestSupport.NOW + 1000)),
            server,
        )
        val state = LicenseStates.evaluate(record, LicenseTestSupport.NOW + 2000)
        assertEquals(LicenseStatus.EXPIRED, state.status)
        assertEquals(0L, state.remainingMillis)
        assertFalse(state.active)
    }

    @Test
    fun `the record is rebuilt from the signed payload alone`() {
        val payload = LicenseTestSupport.payload(
            plan = "plus",
            pausedMs = 3 * LicenseTestSupport.DAY,
            deviceLimit = 3,
            devicesUsed = 2,
        )
        val envelope = LicenseTestSupport.envelope(payload, server)
        val accepted = LicenseCertificates.admit(
            envelope,
            server.publicBase64,
            LicenseTestSupport.DEVICE,
            LicenseTestSupport.NOW,
        ) as LicenseAdmission.Accepted
        val record = LicenseStates.recordOf(accepted.payload, envelope, LicenseTestSupport.NOW)
        assertEquals("plus", record.plan)
        assertEquals(3 * LicenseTestSupport.DAY, record.pausedMillis)
        assertEquals(3, record.deviceLimit)
        assertEquals(2, record.devicesUsed)
        assertEquals(LicenseTestSupport.NOW + 30L * LicenseTestSupport.DAY, record.expiresAtMillis)
        assertEquals(LicenseRecord.FORMAT, record.format)
        assertTrue("الغلاف الموقّع محفوظ كما هو", record.certificate.payload == envelope.payload)
    }
}
