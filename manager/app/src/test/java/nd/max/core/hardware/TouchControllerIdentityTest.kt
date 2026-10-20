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
 * هوية محكّم اللمس (‏§٧-أ): **تُقرأ من جهاز الإدخال، ولا تُستنتج من اسم الهاتف**.
 *
 * والحرّاس هنا ثلاثة: تصنيف الأسماء المعروفة، وبقاء المجهول مجهولًا، وبراءة توقيع المصنِّف من
 * طراز الهاتف/الشريحة — وهو الفرق بين «قرينة تعريف» و«تخمين قدرة».
 */
class TouchControllerIdentityTest {

    private fun device(name: String, vendor: Int = 0, product: Int = 0) =
        TouchInputDevice(name = name, vendorId = vendor, productId = product, descriptor = "d-$name")

    // ── ① الأسماء المعروفة تُصنَّف بعائلتها ────────────────────────────────────────────────

    @Test
    fun `known controller names classify to their family`() {
        val cases = mapOf(
            "fts_ts" to TouchControllerFamily.FOCALTECH,
            "FocalTech FT5x06" to TouchControllerFamily.FOCALTECH,
            "NVTCapacitiveTouchScreen" to TouchControllerFamily.NOVATEK,
            "nt36xxx" to TouchControllerFamily.NOVATEK,
            "goodix_ts" to TouchControllerFamily.GOODIX,
            "synaptics_dsx" to TouchControllerFamily.SYNAPTICS,
            "atmel_mxt_ts" to TouchControllerFamily.ATMEL,
            "elan-touchscreen" to TouchControllerFamily.ELAN,
            "himax-touchscreen" to TouchControllerFamily.HIMAX,
        )

        cases.forEach { (name, expected) ->
            assertEquals("اسم الوحدة: $name", expected, TouchControllerClassifier.classify(name))
        }
    }

    // ── ② المجهول يبقى مجهولًا ─────────────────────────────────────────────────────────────

    @Test
    fun `an unknown name stays unknown and is not guessed`() {
        listOf("touchscreen", "primary-touch", "input-0", "").forEach { name ->
            assertEquals(
                "«$name» ليست عائلة معروفة — ولا تُنسب إلى أشهر محكّم في الشريحة",
                TouchControllerFamily.UNKNOWN,
                TouchControllerClassifier.classify(name),
            )
        }
    }

    @Test
    fun `the phone model is not an input to classification`() {
        listOf("POCO X7 Pro", "rodin", "24069PC21G", "Dimensity 8400").forEach { model ->
            assertEquals(
                "اسم الهاتف/الشريحة لا يُنتج عائلة — مدخل المصنِّف اسم جهاز الإدخال وحده",
                TouchControllerFamily.UNKNOWN,
                TouchControllerClassifier.classify(model),
            )
        }
    }

    // ── ③ الهوية تُبنى من الجهاز المقروء، والمعرّفات تُعرض خامّة ───────────────────────────

    @Test
    fun `the first reported touch device becomes the identity`() {
        val identity = TouchControllerClassifier.identify(
            listOf(device("fts_ts", vendor = 0x1234, product = 0x5678), device("goodix_ts")),
        )

        assertTrue("الاسم طابق عائلة معروفة", identity.identified)
        assertEquals(TouchControllerFamily.FOCALTECH, identity.family)
        assertEquals("fts_ts", identity.device?.name)
        assertEquals(
            "المعرّفان كما يعلنهما الناقل — بلا تفسير",
            "0x1234 / 0x5678",
            identity.idsLabel,
        )
    }

    @Test
    fun `no device means no identity rather than a fabricated one`() {
        val identity = TouchControllerClassifier.identify(emptyList())

        assertNull(identity.device)
        assertNull(identity.idsLabel)
        assertFalse(identity.identified)
        assertEquals(TouchControllerFamily.UNKNOWN, identity.family)
    }

    @Test
    fun `a device with an unknown name keeps its raw data but no family`() {
        val identity = TouchControllerClassifier.identify(listOf(device("primary-touch", vendor = 0, product = 0)))

        assertEquals("primary-touch", identity.device?.name)
        assertEquals("0x0000 / 0x0000", identity.idsLabel)
        assertFalse("الاسم مقروء، والعائلة غير معروفة", identity.identified)
    }
}
