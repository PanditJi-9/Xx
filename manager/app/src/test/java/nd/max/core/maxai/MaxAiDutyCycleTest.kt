/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.maxai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * إيقاع الحلقة السريعة — العقد الذي يجعل «إطفاء Max AI يوقف عمله الخلفي» حقيقة مقيسة.
 *
 * والعطب الذي يحرسه: الحلقة كانت تجمع حالة الجهاز كل ثانية وتستدعي JNI وتقيّم الأمان
 * وتُسجّل حلقة، **والمحرك مطفأ كما وهو يعمل**. فمن يُطفئ الميزة لا يرى فرقًا في الحرارة
 * أو اللاغ — وهو نصّ الشكوى.
 */
class MaxAiDutyCycleTest {

    @Test
    fun `an off engine never takes a full sample`() {
        val duty = MaxAiDutyCycle.fast(aiEnabled = false, screenOn = true)
        assertFalse("المطفأ لا يجمع ولا يستدعي JNI ولا يكتب دفترًا", duty.fullSample)
        assertEquals(MaxAiDutyCycle.Reason.IDLE_GUARD, duty.reason)
        assertEquals(MaxAiDutyCycle.IDLE_GUARD_MS, duty.delayMs)
    }

    @Test
    fun `an off engine still ticks its emergency guard far slower than one second`() {
        val duty = MaxAiDutyCycle.fast(aiEnabled = false, screenOn = false)
        assertTrue(
            "حاجز الطوارئ يبقى، لكن بلا استطلاع ثانيةً بثانية",
            duty.delayMs >= 10_000L,
        )
    }

    @Test
    fun `an active engine samples fully once per second with the screen on`() {
        val duty = MaxAiDutyCycle.fast(aiEnabled = true, screenOn = true)
        assertTrue(duty.fullSample)
        assertEquals(MaxAiDutyCycle.ACTIVE_MS, duty.delayMs)
    }

    @Test
    fun `an active engine slows down with the screen off without stopping`() {
        val duty = MaxAiDutyCycle.fast(aiEnabled = true, screenOn = false)
        assertTrue("الحماية لا تتوقّف والشاشة مطفأة", duty.fullSample)
        assertEquals(MaxAiDutyCycle.SCREEN_OFF_MS, duty.delayMs)
        assertTrue(duty.delayMs > MaxAiDutyCycle.ACTIVE_MS)
    }
}
