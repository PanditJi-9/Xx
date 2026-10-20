/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حكم تتابع العيّنات — العطب الذي أبلغ عنه المالك (٢٠٢٦-١٠-٢١): «يظهر التنبيه الأحمر قبل ثوانٍ
 * من تجميع التطبيقات، فيظنّ المستخدم للحظات أنّ هناك مشكلة».
 *
 * والسبب كان في **ترتيب** ما يصل لا في القراءة: `ProcessWatch.snapshot` يبثّ قيمته المحفوظة فور
 * الاشتراك، وكانت الشاشة تعدّها عيّنة جديدة فتعرض `noAnswer` الحمراء قبل أن تصل القراءة الأولى.
 * فالبوابة هنا تقيس التتابع نفسه: محفوظٌ ليس جديدًا · فشلٌ عابر ليس عطبًا · وفشلٌ بعد نجاح لا
 * يمحو جدولًا معروضًا.
 *
 * **وحدّها المعلن:** هذه حالة العرض لا قراءة `top`؛ فما يُقاس هنا هو الحكم على ما وصل، لا الجهاز.
 */
class ProcessReadGuardTest {

    @Test
    fun `the held sample replayed on subscribe is not a new reading`() {
        val guard = ProcessReadGuard()
        guard.begin(tick = 7)

        assertFalse("بثّ القيمة المحفوظة لا يُعرض كعيّنة جديدة", guard.offer(tick = 7, failed = true))
        assertEquals(ProcessReadState.Waiting, guard.state)
    }

    @Test
    fun `one transient failure stays waiting and does not turn red`() {
        val guard = ProcessReadGuard()
        guard.begin(tick = 0)

        guard.offer(tick = 1, failed = true)
        assertEquals(
            "محاولة فاشلة واحدة قبل أيّ جدول تبقى انتظارًا، لا «تعذّرت القراءة»",
            ProcessReadState.Waiting,
            guard.state,
        )
    }

    @Test
    fun `two failures before any data are the first honest error`() {
        val guard = ProcessReadGuard()
        guard.begin(tick = 0)

        guard.offer(tick = 1, failed = true)
        guard.offer(tick = 2, failed = true)

        assertEquals(ProcessReadState.Failed, guard.state)
    }

    @Test
    fun `a success after a miss clears the count`() {
        val guard = ProcessReadGuard()
        guard.begin(tick = 0)
        guard.offer(tick = 1, failed = true)

        guard.offer(tick = 2, failed = false)
        assertEquals(ProcessReadState.Live, guard.state)

        guard.offer(tick = 3, failed = true)
        assertEquals("العدّاد بدأ من جديد بعد النجاح", ProcessReadState.Live, guard.state)
    }

    @Test
    fun `a failure after success keeps the table on screen and says stale`() {
        val guard = ProcessReadGuard()
        guard.begin(tick = 0)
        guard.offer(tick = 1, failed = false)

        assertFalse("لا تُعرض عيّنة فاشلة بدل جدول معروض", guard.offer(tick = 2, failed = true))
        assertEquals(ProcessReadState.Live, guard.state)

        assertFalse(guard.offer(tick = 3, failed = true))
        assertEquals(ProcessReadState.Stale, guard.state)
    }

    @Test
    fun `recovery returns to live`() {
        val guard = ProcessReadGuard()
        guard.begin(tick = 0)
        guard.offer(tick = 1, failed = false)
        guard.offer(tick = 2, failed = true)
        guard.offer(tick = 3, failed = true)
        assertEquals(ProcessReadState.Stale, guard.state)

        assertTrue(guard.offer(tick = 4, failed = false))
        assertEquals(ProcessReadState.Live, guard.state)
    }
}
