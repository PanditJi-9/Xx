/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * يقيس حساب عتبات LMK ومعنى كل حكم كتابة — بلا جهاز وبلا جذر.
 *
 * **ولماذا فصلُ القراءة والكتابة في `Io`:** هذا الاختبار يمرّر قراءةً وكتابةً مصنوعتين، فيصير
 * كل حكم (`APPLIED` / `NOT_WRITABLE` / `UNVERIFIED`) قابلًا لإعادة الإنتاج بدل أن يُجرَّب على
 * نواةٍ حيّة. والخطأ الذي يُمسك هنا هو أخطر ما في الملف: كتابة متجه أقصر من متجه النواة.
 */
class LmkTuningTest {

    @Test
    fun `parse accepts a comma separated table`() {
        assertEquals(listOf(10240, 20480, 30720), LmkTuning.parse("10240,20480,30720"))
    }

    @Test
    fun `parse accepts whitespace and newlines`() {
        assertEquals(listOf(10240, 20480, 30720), LmkTuning.parse(" 10240 20480\n30720 \n"))
    }

    @Test
    fun `parse rejects a partial table instead of keeping the valid part`() {
        assertNull(LmkTuning.parse("10240,20480,abc"))
        assertNull(LmkTuning.parse("10240,,"))
    }

    @Test
    fun `parse rejects a single value and an empty node`() {
        assertNull(LmkTuning.parse("10240"))
        assertNull(LmkTuning.parse(null))
        assertNull(LmkTuning.parse(""))
    }

    @Test
    fun `parse rejects zero and values above the ceiling`() {
        assertNull(LmkTuning.parse("0,20480"))
        assertNull(LmkTuning.parse("10240,${LmkTuning.MAX_PAGE + 1}"))
    }

    @Test
    fun `format writes the kernel shape without spaces`() {
        assertEquals("10240,20480", LmkTuning.format(listOf(10240, 20480)))
    }

    @Test
    fun `scale softens and hardens by the requested percent`() {
        val base = listOf(10000, 20000, 40000)
        assertEquals(listOf(7500, 15000, 30000), LmkTuning.scale(base, 75))
        assertEquals(listOf(12500, 25000, 50000), LmkTuning.scale(base, 125))
    }

    @Test
    fun `scale refuses percents outside the shipped range`() {
        assertNull(LmkTuning.scale(listOf(10000, 20000), 49))
        assertNull(LmkTuning.scale(listOf(10000, 20000), 201))
    }

    @Test
    fun `scale keeps the ladder strictly increasing after rounding`() {
        // ١ و٢ بعد ٧٥٪ يصيران ٠ و١ بلا إصلاح — سلّم مكسور ترفضه النواة.
        assertEquals(listOf(1, 2), LmkTuning.scale(listOf(1, 2), 75))
        val scaled = LmkTuning.scale(listOf(3, 3, 3), 75)!!
        assertEquals(3, scaled.size)
        assertTrue(scaled[1] > scaled[0])
        assertTrue(scaled[2] > scaled[1])
    }

    @Test
    fun `orderAdjusted reports that the exact percent was not kept`() {
        assertTrue(LmkTuning.orderAdjusted(listOf(1, 2), LmkTuning.scale(listOf(1, 2), 75)!!, 75))
        assertFalse(LmkTuning.orderAdjusted(listOf(10000, 20000), LmkTuning.scale(listOf(10000, 20000), 75)!!, 75))
    }

    @Test
    fun `pageToMegabytes uses the page size it is given`() {
        assertEquals(40, LmkTuning.pageToMegabytes(10240))
        assertEquals(4, LmkTuning.pageToMegabytes(1024))
        assertEquals(5, LmkTuning.pageToMegabytes(1024, pageKb = 5))
    }

    @Test
    fun `apply reports applied only when the read-back matches`() {
        val node = "10240,20480,30720"
        val io = LmkTuning.Io(read = { node }, write = { _, _ -> true })
        assertEquals(LmkTuning.Outcome.APPLIED, io.apply(listOf(10240, 20480, 30720)))
    }

    @Test
    fun `apply reports not present when the kernel has no table`() {
        val io = LmkTuning.Io(read = { null }, write = { _, _ -> true })
        assertEquals(LmkTuning.Outcome.NOT_PRESENT, io.apply(listOf(10240, 20480)))
    }

    @Test
    fun `apply reports not writable when the write is refused`() {
        val io = LmkTuning.Io(read = { "10240,20480" }, write = { _, _ -> false })
        assertEquals(LmkTuning.Outcome.NOT_WRITABLE, io.apply(listOf(10240, 20480)))
    }

    @Test
    fun `apply reports unverified when the node keeps the old value`() {
        val io = LmkTuning.Io(read = { "10240,20480" }, write = { _, _ -> true })
        assertEquals(LmkTuning.Outcome.UNVERIFIED, io.apply(listOf(7680, 15360)))
    }

    @Test
    fun `apply refuses a vector of a different length than the kernel vector`() {
        var wrote = false
        val io = LmkTuning.Io(read = { "10240,20480,30720" }, write = { _, _ -> wrote = true; true })
        assertEquals(LmkTuning.Outcome.UNVERIFIED, io.apply(listOf(10240, 20480)))
        assertFalse("لا تُكتب عقدة أقصر من عقدة النواة", wrote)
    }

    @Test
    fun `apply writes the formatted ladder and verifies against the parsed read-back`() {
        val written = StringBuilder()
        var node = "10240,20480"
        val io = LmkTuning.Io(
            read = { node },
            write = { _, value -> written.append(value); node = value; true },
        )
        assertEquals(LmkTuning.Outcome.APPLIED, io.apply(listOf(7680, 15360)))
        assertEquals("7680,15360", written.toString())
    }
}
