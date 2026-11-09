/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** الحساب وبوابة الطلب يُقاسان هنا؛ قبول عقدة حقيقية للكتابة يحتاج جهازًا. */
class LmkTuningTest {
    private val original = listOf(10240, 20480, 30720)
    private val target = listOf(7680, 15360, 23040)

    @Test
    fun parseAcceptsKernelCommaSeparatedValues() {
        assertEquals(original, LmkTuning.parse("10240,20480,30720\n"))
    }

    @Test
    fun parseAcceptsWhitespaceWithoutDiscardingTokens() {
        assertEquals(original, LmkTuning.parse(" 10240\t20480\n30720 \r\n"))
        assertEquals(original, LmkTuning.parse("10240, 20480,\n30720"))
    }

    @Test
    fun parseRejectsEmptyCommaFields() {
        listOf("10240,,20480", ",10240,20480", "10240,20480,", "10240,20480,,").forEach { raw ->
            assertNull(raw, LmkTuning.parse(raw))
        }
    }

    @Test
    fun parseRejectsMixedTokensAndPartialNumbers() {
        listOf("10240,20480abc", "10240,20480 30720", "1,+2", "1,-2", "1,2.0", "1,٢").forEach { raw ->
            assertNull(raw, LmkTuning.parse(raw))
        }
    }

    @Test
    fun parseRejectsNullEmptyAndSingleValue() {
        assertNull(LmkTuning.parse(null))
        assertNull(LmkTuning.parse(""))
        assertNull(LmkTuning.parse(" \n"))
        assertNull(LmkTuning.parse("10240"))
    }

    @Test
    fun parseBoundsLengthCountAndMagnitude() {
        assertNull(LmkTuning.parse("1," + "2".repeat(LmkTuning.MAX_TEXT_LENGTH)))
        assertNull(LmkTuning.parse("1,2,3,4,5,6,7"))
        assertNull(LmkTuning.parse("0,20480"))
        assertNull(LmkTuning.parse("10240,${LmkTuning.MAX_PAGE + 1}"))
        assertNull(LmkTuning.parse("1,99999999999999999999"))
        assertEquals(listOf(1, 2, 3, 4, 5, 6), LmkTuning.parse("1,2,3,4,5,6"))
    }

    @Test
    fun parseRejectsDuplicateAndDescendingThresholds() {
        assertNull(LmkTuning.parse("20480,10240"))
        assertNull(LmkTuning.parse("10240,10240"))
    }

    @Test
    fun formatPreservesCountAndOrder() {
        assertEquals("10240,20480,30720", LmkTuning.format(original))
        assertEquals(original, LmkTuning.parse(LmkTuning.format(original)))
    }

    @Test
    fun scaleUsesTheRequestedPercentage() {
        assertEquals(target, LmkTuning.scale(original, 75))
        assertEquals(listOf(12800, 25600, 38400), LmkTuning.scale(original, 125))
        assertEquals(original, LmkTuning.scale(original, 100))
    }

    @Test
    fun scaleDoesNotMutateItsInput() {
        val input = original.toMutableList()
        LmkTuning.scale(input, 75)
        assertEquals(original, input)
    }

    @Test
    fun scaleRejectsOutOfRangePercentages() {
        listOf(Int.MIN_VALUE, 0, 49, 201, Int.MAX_VALUE).forEach { percent ->
            assertNull(LmkTuning.scale(original, percent))
        }
    }

    @Test
    fun scaleRejectsMalformedInputs() {
        listOf(emptyList(), listOf(1), listOf(-1, 2), listOf(3, 3), listOf(4, 2), listOf(1, Int.MAX_VALUE)).forEach { input ->
            assertNull(LmkTuning.scale(input, 75))
        }
    }

    @Test
    fun scaleRejectsRoundingCollisionsInsteadOfInventingValues() {
        assertNull(LmkTuning.scale(listOf(1, 2), 75))
        assertNull(LmkTuning.scale(listOf(4, 5), 50))
    }

    @Test
    fun scaleRejectsCeilingOverflowInsteadOfClamping() {
        assertNull(LmkTuning.scale(listOf(400000, 500000), 125))
        assertEquals(listOf(250000, 500000), LmkTuning.scale(listOf(125000, 250000), 200))
    }

    @Test
    fun pageConversionUsesMeasuredFourAndSixteenKilobytePages() {
        assertEquals(40L, LmkTuning.pageToMegabytes(10240, 4096L))
        assertEquals(160L, LmkTuning.pageToMegabytes(10240, 16384L))
    }

    @Test
    fun pageConversionRejectsUnknownOrInvalidPageSizes() {
        listOf(-1L, 0L, 2048L, 5000L, Long.MAX_VALUE).forEach { bytes ->
            assertNull(LmkTuning.pageToMegabytes(10240, bytes))
        }
        assertNull(LmkTuning.pageToMegabytes(0, 4096L))
    }

    @Test
    fun unreadablePageSizeRemainsUnknown() {
        val io = LmkTuning.Io(read = { null }, submit = { _, _ -> LmkTuning.Outcome.BLOCKED })
        assertNull(io.pageSizeBytes())
    }

    @Test
    fun pageSizeAndReadExceptionsDoNotEscape() {
        val io = LmkTuning.Io(
            read = { throw IllegalStateException("read") },
            submit = { _, _ -> LmkTuning.Outcome.BLOCKED },
            pageBytes = { throw IllegalStateException("page-size") },
        )
        assertNull(io.current())
        assertNull(io.pageSizeBytes())
    }

    @Test
    fun invalidTargetNeverReachesTheSubmitGate() {
        var submitted = false
        val io = LmkTuning.Io(read = { LmkTuning.format(original) }, submit = { _, _ -> submitted = true; LmkTuning.Outcome.APPLIED })
        assertEquals(LmkTuning.Outcome.INVALID, io.apply(listOf(0, 1, 2), original))
        assertFalse(submitted)
    }

    @Test
    fun differentVectorLengthNeverReachesTheSubmitGate() {
        var submitted = false
        val io = LmkTuning.Io(read = { LmkTuning.format(original) }, submit = { _, _ -> submitted = true; LmkTuning.Outcome.APPLIED })
        assertEquals(LmkTuning.Outcome.INVALID, io.apply(target.take(2), original))
        assertFalse(submitted)
    }

    @Test
    fun unavailableNodeNeverReachesTheSubmitGate() {
        var submitted = false
        val io = LmkTuning.Io(read = { null }, submit = { _, _ -> submitted = true; LmkTuning.Outcome.APPLIED })
        assertEquals(LmkTuning.Outcome.NOT_PRESENT, io.apply(target, original))
        assertFalse(submitted)
    }

    @Test
    fun staleSnapshotBlocksInsteadOfOverwritingAnotherWriter() {
        var submitted = false
        val io = LmkTuning.Io(read = { "12000,24000,48000" }, submit = { _, _ -> submitted = true; LmkTuning.Outcome.APPLIED })
        assertEquals(LmkTuning.Outcome.BLOCKED, io.apply(target, original))
        assertFalse(submitted)
    }

    @Test
    fun appliedRequiresBothGateApprovalAndMatchingReadback() {
        var node = LmkTuning.format(original)
        val io = LmkTuning.Io(read = { node }, submit = { desired, expected ->
            assertEquals(LmkTuning.format(original), expected)
            node = desired
            LmkTuning.Outcome.APPLIED
        })
        assertEquals(LmkTuning.Outcome.APPLIED, io.apply(target, original))
        assertEquals("7680,15360,23040", node)
    }

    @Test
    fun acceptedGateCannotFabricateSuccessfulReadback() {
        val io = LmkTuning.Io(read = { LmkTuning.format(original) }, submit = { _, _ -> LmkTuning.Outcome.APPLIED })
        assertEquals(LmkTuning.Outcome.UNVERIFIED, io.apply(target, original))
    }

    @Test
    fun readbackFormattingIsComparedByFullParsedVector() {
        var node = LmkTuning.format(original)
        val io = LmkTuning.Io(read = { node }, submit = { _, _ -> node = "7680, 15360, 23040\n"; LmkTuning.Outcome.APPLIED })
        assertEquals(LmkTuning.Outcome.APPLIED, io.apply(target, original))
    }

    @Test
    fun unavailableReadbackIsUnverified() {
        var node: String? = LmkTuning.format(original)
        val io = LmkTuning.Io(read = { node }, submit = { _, _ -> node = null; LmkTuning.Outcome.APPLIED })
        assertEquals(LmkTuning.Outcome.UNVERIFIED, io.apply(target, original))
    }

    @Test
    fun refusedAndBlockedGateOutcomesRemainDistinct() {
        listOf(LmkTuning.Outcome.NOT_WRITABLE, LmkTuning.Outcome.BLOCKED, LmkTuning.Outcome.UNVERIFIED).forEach { verdict ->
            val io = LmkTuning.Io(read = { LmkTuning.format(original) }, submit = { _, _ -> verdict })
            assertEquals(verdict, io.apply(target, original))
        }
    }

    @Test
    fun submitExceptionIsNotReportedAsSuccess() {
        val io = LmkTuning.Io(read = { LmkTuning.format(original) }, submit = { _, _ -> throw IllegalStateException("write") })
        assertEquals(LmkTuning.Outcome.UNVERIFIED, io.apply(target, original))
    }
}
