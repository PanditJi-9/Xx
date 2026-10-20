/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * حكم تجاوز الشحن — معيار القبول ٣: **لا نتيجة نجاح كاذبة في عمليات التحكم**.
 *
 * والعطب الذي يحرسه هذا الملف مقيس لا متخيَّل: البلاطة كانت تكتب ثم تُعلن «مفعّل» قبل أي قراءة،
 * فتبقى تقول مفعّلًا على جهاز بلا جذر. فالحكم هنا يُقاس على JVM، ويُقاس معه أن **البلاطة نفسها**
 * لم تعد تعلن الحالة قبل القراءة (قياس نصّي على المصدر — وهو ما يمكن قياسه بلا جهاز).
 */
class BypassVerdictTest {

    @Test
    fun `only the value that was asked for counts as applied`() {
        assertTrue(BypassVerdict.applied(requestedOn = true, readBack = "1"))
        assertTrue(BypassVerdict.applied(requestedOn = false, readBack = "0"))
        assertTrue(
            "وحرف زائد لا يُسقط حكمًا صحيحًا — القراءة تُشذَّب",
            BypassVerdict.applied(requestedOn = true, readBack = " 1 \n"),
        )
    }

    @Test
    fun `a value other than the requested one is a failure, never a success`() {
        assertFalse(BypassVerdict.applied(requestedOn = true, readBack = "0"))
        assertFalse(BypassVerdict.applied(requestedOn = false, readBack = "1"))
        assertFalse(
            "وقيمة غريبة (كتبتها وحدة أو روم) ليست نجاحًا",
            BypassVerdict.applied(requestedOn = true, readBack = "on"),
        )
    }

    @Test
    fun `a missing or empty property is not success`() {
        assertFalse("خصيصة غير موجودة", BypassVerdict.applied(requestedOn = true, readBack = null))
        assertFalse(BypassVerdict.applied(requestedOn = true, readBack = ""))
        assertFalse(BypassVerdict.applied(requestedOn = false, readBack = "   "))
        assertFalse(
            "ولا يُقرأ الغائب «off» حين طُلب الإطفاء: الغائب ليس تنفيذًا",
            BypassVerdict.applied(requestedOn = false, readBack = null),
        )
    }

    @Test
    fun `the tile reads the truth after the write instead of claiming it before`() {
        val tile = sourceFile("TileService/BypassChgTileService.kt").readText()
        assertTrue(
            "الحكم يُقرأ من القراءة بعد الكتابة",
            tile.contains("BypassVerdict.applied("),
        )
        assertFalse(
            "ولا يعود نمط «اكتب ثم أعلن مفعّلًا» قبل أي قراءة",
            tile.contains("setBypassChg(\"1\")\n            tile.state = Tile.STATE_ACTIVE"),
        )
        assertTrue(
            "وفشل التطبيق يُعلن بلفظه لا بصمت",
            tile.contains("R.string.status_not_applied"),
        )
        assertTrue(
            "والكتابة مُتحقَّقة بثلاث طرق لا نداء واحد",
            tile.contains("PropertyUtils.setAndConfirm("),
        )
    }

    /** مسار ملفّ مصدر من جذر يعمل في الموضعين: CI من `manager/`، ومحلّيًّا من الجذر. */
    private fun sourceFile(relative: String): File {
        val candidates = listOf(
            File("src/main/java/nd/max/$relative"),
            File("app/src/main/java/nd/max/$relative"),
            File("../app/src/main/java/nd/max/$relative"),
            File("manager/app/src/main/java/nd/max/$relative"),
        )
        return candidates.firstOrNull { it.isFile } ?: error("لم يُوجد المصدر: $relative")
    }
}
