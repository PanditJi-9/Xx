/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.security

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * القياس الذي يحسم الجدل: **حزمة موقّعة حقيقية تُقبل، ونسخة عُدّل فيها بايت واحد تُرفض**.
 *
 * والفكرة التي يقوم عليها: التعديل هنا **بايت واحد في مكانه** لا إعادة بناء للحزمة.
 * فالبايتات كلّها تبقى كما هي — الفهرس المركزي، وسجلّ نهاية الفهرس، وكتلة التوقيع نفسها —
 * ويُغيَّر بايت واحد داخل محتوى مدخل. فيصير العطب منسوبًا إلى ذلك البايت وحده بلا لبس:
 * لو رُفضت النسخة لسبب آخر (إعادة كتابة zip، فشل قراءة) لما دلّ شيء على أن كاشف تعديل
 * المحتوى يعمل.
 *
 * **ولذلك يُقاس وجهان لا وجه واحد:**
 *
 * * الحزمة الموقّعة الأصلية ⇒ [ApkContentVerification.Outcome.VERIFIED]. ولو سقط هذا
 *   الوجه لصار الفحص يرفض النسخ السليمة — وهو أسوأ من غيابه (تطبيق لا يعمل على جهاز
 *   نظيف بلا سبب).
 * * والنسخة المعدَّلة ⇒ [ApkContentVerification.Outcome.REJECTED].
 *
 * **وحدّه المعلن:** يحتاج حزمة مبنيّة في الشجرة، وهي ليست موجودة دائمًا (اختبارات الوحدة
 * تُشغَّل قبل التغليف في CI). فيُعلن التخطّي بـ`assumeTrue` — لا يُقال «مرّ» لقياسٍ لم
 * يُجرَ. وحين تُبنى الحزمة (محليًّا أو في خطوة لاحقة) يُقاس فعلًا بلا تعديل.
 */
class ApkContentVerificationTest {

    /** أوّل حزمة مبنيّة في الشجرة، أو `null` — والمسارات بترتيب الأولوية: الإصدار ثم التصحيح. */
    private fun builtApk(): File? = listOf(
        "build/outputs/apk/release/app-release.apk",
        "build/outputs/apk/debug/app-debug.apk",
        "app/build/outputs/apk/release/app-release.apk",
        "app/build/outputs/apk/debug/app-debug.apk",
        "manager/app/build/outputs/apk/release/app-release.apk",
        "manager/app/build/outputs/apk/debug/app-debug.apk",
    ).map(::File).firstOrNull { it.isFile }

    @Test
    fun `the signed apk verifies`() {
        val apk = builtApk()
        assumeTrue("لا حزمة مبنيّة في الشجرة ⇒ القياس غير مُتحقَّق في هذه البيئة", apk != null)
        assertEquals(
            "حزمة موقّعة سليمة يجب أن تُقبل — ورفضها عطب بالغ",
            ApkContentVerification.Outcome.VERIFIED,
            ApkContentVerification.verifyFile(apk!!),
        )
    }

    @Test
    fun `a copy with one flipped entry byte is rejected`() {
        val apk = builtApk()
        assumeTrue("لا حزمة مبنيّة في الشجرة ⇒ القياس غير مُتحقَّق في هذه البيئة", apk != null)
        val original = apk!!.readBytes()
        val tampered = File.createTempFile("tampered", ".apk")
        try {
            val patched = flipOneByteInsideEntry(original, tampered, ENTRY)
            assumeTrue("المدخل '$ENTRY' غير موجود في هذه الحزمة ⇒ القياس غير مُتحقَّق", patched >= 0)

            // الإثبات الأول: الفرق بايت واحد — فما يُقاس بعده منسوب إليه وحده.
            val tamperedBytes = tampered.readBytes()
            assertEquals("النسخة يجب أن تكون بنفس حجم الأصل", original.size, tamperedBytes.size)
            val differing = original.indices.count { original[it] != tamperedBytes[it] }
            assertEquals("يجب أن يختلف البايتان في موضع واحد لا أكثر", 1, differing)

            assertEquals(
                "نسخة عُدّل فيها بايت واحد يجب أن تُرفض",
                ApkContentVerification.Outcome.REJECTED,
                ApkContentVerification.verifyFile(tampered),
            )
        } finally {
            tampered.delete()
        }
    }

    /**
     * ينسخ البايتات كما هي ويقلب بايتًا واحدًا **داخل محتوى** مدخل معيّن، بمشي على سجلات
     * ZIP المحلية (`0x04034b50`) وقراءة `compressedSize` وطولَي الاسم والامتداد.
     * ويُعيد إزاحة البايت المقلوب، أو `-1` إن لم يوجد المدخل.
     *
     * **ولماذا المشي على السجلات لا بحث نصّيّ عن الاسم:** الاسم يوجد أيضًا في الفهرس
     * المركزي، وقلب بايت هناك يعدّل بنية الفهرس نفسها فيصير الرفض محتملًا لأي سبب. هنا
     * يُقلَب بايت في **المحتوى**، ومكانه محسوب من الرأس لا مُخمَّن.
     */
    private fun flipOneByteInsideEntry(source: ByteArray, target: File, entry: String): Int {
        var cursor = 0
        while (cursor + LOCAL_HEADER_SIZE <= source.size) {
            if (readU32(source, cursor) != LOCAL_HEADER_SIGNATURE) break
            val compressedSize = readU32(source, cursor + 18)
            val nameLength = readU16(source, cursor + 26)
            val extraLength = readU16(source, cursor + 28)
            val dataStart = cursor + LOCAL_HEADER_SIZE + nameLength + extraLength
            if (compressedSize < 8L || dataStart + compressedSize > source.size) break
            val name = String(source, cursor + LOCAL_HEADER_SIZE, nameLength, Charsets.UTF_8)
            if (name == entry) {
                val at = dataStart + (compressedSize / 2).toInt()
                val copy = source.copyOf()
                copy[at] = (copy[at].toInt() xor 0x01).toByte()
                target.writeBytes(copy)
                return at
            }
            cursor = dataStart + compressedSize.toInt()
        }
        return -1
    }

    private fun readU32(bytes: ByteArray, offset: Int): Long =
        (bytes[offset].toLong() and 0xFF) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFF) shl 24)

    private fun readU16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private companion object {
        /** مدخل مخزَّن بلا ضغط وموجود في كل حزمة أندرويد. */
        const val ENTRY = "resources.arsc"
        const val LOCAL_HEADER_SIGNATURE = 0x04034b50L
        const val LOCAL_HEADER_SIZE = 30
    }
}
