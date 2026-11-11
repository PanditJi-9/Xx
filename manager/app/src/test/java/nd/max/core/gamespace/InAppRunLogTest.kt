/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **السجلّ الذي يُعرض على المستخدم لا يُقرأ بالحدس.**
 *
 * ملخّص آخر تطبيق يُكتب في تفضيلات مشتركة، وقد يكون ما فيها نصًّا من نسخة أقدم أو ملفًّا
 * عُدّل أو ترميزًا ناقصًا. فكل قراءة مشكوكة تُعيد `null` — «لا سجلّ» — ولا تُعيد تشغيلًا
 * مخترعًا يُعرض كأنه حقيقة. وهذا ما يُقاس هنا.
 */
class InAppRunLogTest {

    private val full = InAppRun(
        pkg = "com.example.game",
        writes = 6,
        written = 5,
        verified = 4,
        skipped = mapOf(InAppKnob.DND to "zen-mode-already-on", InAppKnob.HAPTICS to "already-off"),
    )

    @Test
    fun `a full run survives encode and decode`() {
        assertEquals(full, InAppRunLog.decode(InAppRunLog.encode(full)))
    }

    @Test
    fun `a run with nothing skipped survives too`() {
        val clean = InAppRun(pkg = "a.b", writes = 1, written = 1, verified = 1)
        val back = InAppRunLog.decode(InAppRunLog.encode(clean))
        assertEquals(clean, back)
        assertTrue("تشغيل نظيف عاد مرفوضًا", back != null)
    }

    @Test
    fun `a blocked run keeps its reason`() {
        val blocked = InAppRun(pkg = "a.b", writes = 0, written = 0, verified = 0, blocked = "lane-unavailable")
        assertEquals("lane-unavailable", InAppRunLog.decode(InAppRunLog.encode(blocked))?.blocked)
    }

    @Test
    fun `nothing stored means no record, never an empty success`() {
        assertNull(InAppRunLog.decode(null))
        assertNull(InAppRunLog.decode(""))
        assertNull(InAppRunLog.decode("   \n "))
    }

    @Test
    fun `another version is not guessed at`() {
        assertNull(InAppRunLog.decode("run\tv2\ta.b\nn\t1\t1\t1"))
    }

    @Test
    fun `an unsafe package name is refused`() {
        assertNull(InAppRunLog.decode("run\tv1\ta b<script>\nn\t1\t1\t1"))
    }

    @Test
    fun `counts that cannot be true are refused`() {
        assertNull("كُتب أكثر من المطلوب", InAppRunLog.decode("run\tv1\ta.b\nn\t2\t3\t2"))
        assertNull("تأكّد أكثر من المطلوب", InAppRunLog.decode("run\tv1\ta.b\nn\t2\t2\t3"))
        assertNull("عدد سالب", InAppRunLog.decode("run\tv1\ta.b\nn\t-1\t0\t0"))
    }

    @Test
    fun `an unknown knob is refused rather than dropped`() {
        assertNull(InAppRunLog.decode("run\tv1\ta.b\nn\t1\t1\t1\nk\tCPU\talready-off"))
    }

    @Test
    fun `a knob written twice is refused rather than picked by guess`() {
        assertNull(
            InAppRunLog.decode(
                "run\tv1\ta.b\nn\t1\t1\t1\nk\tDND\talready-off\nk\tDND\tsnapshot-unsafe",
            ),
        )
    }

    @Test
    fun `a reason with unsafe characters is refused`() {
        assertNull(InAppRunLog.decode("run\tv1\ta.b\nn\t1\t1\t1\nk\tDND\tsome reason"))
    }

    @Test
    fun `every known token maps to its reason and anything else is unknown`() {
        assertEquals(InAppRunReason.ZEN_UNREADABLE, InAppRunReason.of("zen-unreadable"))
        assertEquals(InAppRunReason.ZEN_ALREADY_ON, InAppRunReason.of("zen-mode-already-on"))
        assertEquals(InAppRunReason.SNAPSHOT_UNSAFE, InAppRunReason.of("snapshot-unsafe"))
        assertEquals(InAppRunReason.SNAPSHOT_UNREADABLE, InAppRunReason.of("snapshot-unreadable"))
        assertEquals(InAppRunReason.ALREADY_OFF, InAppRunReason.of("already-off"))
        assertEquals(InAppRunReason.LANE_UNAVAILABLE, InAppRunReason.of("lane-unavailable"))
        assertEquals(InAppRunReason.UNKNOWN, InAppRunReason.of(null))
        assertEquals(InAppRunReason.UNKNOWN, InAppRunReason.of("xyz"))
    }
}
