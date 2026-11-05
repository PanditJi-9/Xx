/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.core.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختيار طريق الكتابة (M2 · ADR-89) — دالّة نقيّة تُقاس بالحقن.
 *
 * **والحالة الثانية هي العطب الذي أُغلق:** استثناء الخادم كان يمنع binder والصدفة معًا،
 * فالكتابة تفشل على جهازٍ يعرف التطبيق طريقه.
 */
class WriteRoutingTest {

    @Test
    fun daemonWinsAndStopsTheChain() {
        var binderCalls = 0
        var shellCalls = 0

        val attempt = WriteRouting.run(
            daemon = { true },
            binder = { binderCalls++; true },
            shell = { shellCalls++; true },
        )

        assertEquals(WriteRoute.DAEMON, attempt.route)
        assertTrue(attempt.ok)
        assertEquals(emptyList<WriteRoute>(), attempt.failed)
        assertEquals("الطبقة التي نجحت لا تُجرَّب بعدها", 0, binderCalls)
        assertEquals("الطبقة التي نجحت لا تُجرَّب بعدها", 0, shellCalls)
    }

    @Test
    fun aThrowingDaemonDoesNotBlockTheBinder() {
        val attempt = WriteRouting.run(
            daemon = { error("daemon exploded") },
            binder = { true },
            shell = { error("must not be reached") },
        )

        assertEquals(WriteRoute.BINDER, attempt.route)
        assertTrue(attempt.ok)
        assertEquals(listOf(WriteRoute.DAEMON), attempt.failed)
    }

    @Test
    fun anAbsentBinderFallsBackToTheShell() {
        val attempt = WriteRouting.run(
            daemon = { false },
            binder = { null },
            shell = { true },
        )

        assertEquals(WriteRoute.ROOT_SHELL, attempt.route)
        assertTrue(attempt.ok)
        assertEquals(listOf(WriteRoute.DAEMON, WriteRoute.BINDER), attempt.failed)
    }

    @Test
    fun everyLayerFailingReportsEveryLayer() {
        val attempt = WriteRouting.run(
            daemon = { false },
            binder = { false },
            shell = { false },
        )

        assertNull(attempt.route)
        assertFalse(attempt.ok)
        assertEquals(listOf(WriteRoute.DAEMON, WriteRoute.BINDER, WriteRoute.ROOT_SHELL), attempt.failed)
    }

    @Test
    fun aThrowingShellIsAFailureNotACrash() {
        val attempt = WriteRouting.run(
            daemon = { false },
            binder = { false },
            shell = { error("shell exploded") },
        )

        assertNull(attempt.route)
        assertFalse(attempt.ok)
        assertEquals(3, attempt.failed.size)
    }

    @Test
    fun everyLayerThrowingIsStillASingleValue() {
        val attempt = WriteRouting.run(
            daemon = { error("a") },
            binder = { error("b") },
            shell = { error("c") },
        )

        assertNull(attempt.route)
        assertFalse(attempt.ok)
        assertEquals(listOf(WriteRoute.DAEMON, WriteRoute.BINDER, WriteRoute.ROOT_SHELL), attempt.failed)
    }
}
