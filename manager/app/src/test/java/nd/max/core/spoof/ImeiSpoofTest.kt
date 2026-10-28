/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.spoof

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ImeiSpoofTest {
    // مثال Luhn المنشور لـIMEI: 49-015420-323751-8.
    private val known = "490154203237518"

    @Test fun luhnAcceptsAKnownImeiAndRejectsAnyOtherCheckDigit() {
        assertTrue(ImeiSpoof.isValid(known))
        for (wrong in "012345679") assertFalse(ImeiSpoof.isValid(known.dropLast(1) + wrong))
        assertEquals(8, ImeiSpoof.checkDigit(known.dropLast(1)))
    }

    @Test fun malformedValuesAreNeverValid() {
        assertFalse(ImeiSpoof.isValid(""))
        assertFalse(ImeiSpoof.isValid("49015420323751"))
        assertFalse(ImeiSpoof.isValid("4901542032375181"))
        assertFalse(ImeiSpoof.isValid("49015420323751a"))
    }

    @Test fun generatedValuesAreFifteenDigitsAndPassLuhn() {
        val random = Random(2026)
        repeat(500) {
            val imei = ImeiSpoof.generate(random)
            assertEquals(15, imei.length)
            assertTrue(imei, imei.all { it in '0'..'9' })
            assertTrue(imei, ImeiSpoof.isValid(imei))
        }
    }

    @Test fun generationIsDeterministicForAGivenRandomAndVariesAcrossDraws() {
        assertEquals(ImeiSpoof.generate(Random(7)), ImeiSpoof.generate(Random(7)))
        val random = Random(7)
        assertNotEquals(ImeiSpoof.generate(random), ImeiSpoof.generate(random))
    }

    @Test fun slotZeroIsTheValueItselfAndOtherSlotsAreValidAndDistinct() {
        assertEquals(known, ImeiSpoof.forSlot(known, 0))
        assertEquals(known, ImeiSpoof.forSlot(known, -1))
        val second = ImeiSpoof.forSlot(known, 1)
        assertNotEquals(known, second)
        assertTrue(ImeiSpoof.isValid(second))
        assertEquals(known.take(8), second.take(8))
        assertEquals(second, ImeiSpoof.forSlot(known, 1))
    }

    @Test fun theSerialWrapsInsteadOfOverflowingIntoTheTac() {
        val body = "49015499999999"
        val value = body + ImeiSpoof.checkDigit(body)
        val next = ImeiSpoof.forSlot(value, 1)
        assertTrue(ImeiSpoof.isValid(next))
        assertEquals("49015499", next.take(8))
        assertEquals("000000", next.substring(8, 14))
    }

    @Test fun anInvalidValueIsReturnedUntouched() {
        assertEquals("123", ImeiSpoof.forSlot("123", 1))
    }

    @Test fun keysAreScopedByPackageName() {
        assertEquals("imei.com.a.game", ImeiSpoof.key("com.a.game"))
        assertNotEquals(ImeiSpoof.key("com.a"), ImeiSpoof.key("com.a.b"))
    }
}
