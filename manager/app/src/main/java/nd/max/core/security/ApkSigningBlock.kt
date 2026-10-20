/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.security

import java.io.File
import java.io.RandomAccessFile

/**
 * قارئ **كتلة توقيع الحزمة** (`APK Signing Block`) — شهادة الموقّع من الملفّ مباشرة،
 * بلا `PackageManager` وبلا `java.security.cert`.
 *
 * ## لماذا هذا الملفّ — العطب الذي يُصلحه بالقياس
 *
 * الطريق الأول لبصمة الموقّع يسأل النظام: `PackageManager.getPackageInfo` ←
 * `SigningInfo`. وهو طريق **قابل للهوك بسطر واحد** في بيئة معدّلة، وهو ما يجعل فحص
 * التوقيع — وهو **المرساة الوحيدة القوية** في حماية أي تطبيق — يسقط بأثر رجعيّ.
 *
 * والطريق الطبيعي الثاني `java.security.cert.CertificateFactory` يقرأ شهادة `v1/JAR`
 * من `META-INF/` (توقيع `.RSA`). وهذا كان كافيًا قبل `minSdk 24`، أمّا اليوم فـAGP **لا يُنتج
 * توقيع v1 أصلًا** متى كان `minSdk ≥ 24` (وهو ٢٩ عندنا) ⇒ فالمسار الثاني **لا يجد شيئًا**
 * ويُعيد `null`، فيبقى فحصٌ واحد يواجه الهوك وحده. وقد كان ذلك بالضبط ما يحدث في أول
 * نسخة من هذه الطبقة: مسار ثانٍ مكتوب، وموجود، و**لا يعمل**.
 *
 * فالقراءة هنا مباشرة من البنية التي تُنتجها أداة التوقيع نفسها (v2/v3): شهادة DER
 * تُستخرج بالبايت من `APK Signing Block`. ولا يعتمد الاستخراج على `PackageManager`، ولا
 * على مكتبة تشفير، ولا على `META-INF` — بل على هندسة ملفّات ZIP كما تُكتب على القرص.
 *
 * ## الصيغة المقروءة (من مواصفة APK Signature Scheme v2/v3)
 *
 * ```
 * [محتوى الحزمة] [كتلة التوقيع] [فهرس ZIP المركزي] [سجلّ نهاية الفهرس EOCD]
 * ```
 *
 * وكتلة التوقيع: `uint64 حجم` ← أزواج `(uint64 طول الزوج = 4 + طول القيمة, uint32 المعرّف, القيمة)`
 * ← `uint64 الحجم نفسه` ← `16 بايت السحر "APK Sig Block 42"`. والموقع يُستنبط من
 * `EOCD → إزاحة الفهرس المركزي`، فتُقرأ الكتلة **قبل** هذه الإزاحة بلا مسح كامل الملفّ.
 *
 * وما يُقرأ من قيمة معرّف v2/v3: `تسلسل الموقّعين` ← أوّل موقّع ← `البيانات الموقّعة` ←
 * `الشهادات` ← أوّل شهادة DER. ولا تُتحقَّق التواقيع هنا **عن قصد**: التحقّق من السلسلة
 * واجب النظام عند التثبيت، وهذه الطبقة تسأل سؤالًا آخر: **مَن وقّع هذه الحزمة التي
 * بين يديّ الآن؟** ولذلك تُقرأ الشهادة ولا تُبنى عليها ثقة؛ الثقة تُبنى بمقارنتها بالبصمة
 * المثبَّتة زمن البناء ([SecurityPin]).
 *
 * ## حدّها المعلن
 *
 * * تُقرأ **الشهادة الأولى** للموقّع الأول. حزمة موقّعة بأكثر من موقّع ⇒ يُعاد الأول
 *   (`PackageManager` هو من يُعلن تعدّد الموقّعين في [IntegrityProbe]: لا يُبنى دليل هناك).
 * * ولا تُتحقّق صحّة الكتلة تشفيريًّا — تُقرأ بنيتها. ولذلك أي عيب بنيوي يُعيد `null`
 *   (لا «مطابقة») ليجري الحكم في موضع واحد هو [SecurityGate].
 */
object ApkSigningBlock {

    private const val MAGIC = "APK Sig Block 42"

    /** معرّف كتلة توقيع v2. */
    private const val ID_V2 = 0x7109871aL

    /** معرّف كتلة توقيع v3. */
    private const val ID_V3 = 0xf05368c0L

    /** معرّف كتلة توقيع v3.1 (تدوير المفاتيح). */
    private const val ID_V31 = 0x1b93ad61L

    /**
     * المعرّفات بترتيب القراءة، **وكلّها `uint32` غير مُوقَّعة**: `0xf05368c0` أكبر من
     * `Int.MAX_VALUE`، فلو قُرئ `Int` لصار عددًا سالبًا لا يُطابق ما يُقرأ من الملفّ
     * إلا بعد قصّ ثنائيّ متعمّد. والمفتاح هنا `Long` لأن `readU32` تُعيد `Long` أصلًا —
     * فتُقرأ البِتّات كما هي بلا قصّ ولا تحويل.
     */
    private val SIGNING_BLOCK_IDS = longArrayOf(ID_V2, ID_V3, ID_V31)

    private const val EOCD_SIGNATURE = 0x06054b50L
    private const val EOCD_MIN_SIZE = 22

    /** أقصى تعليق في ZIP = 65535 بايت، فمنه يُشتقّ مدى البحث عن EOCD. */
    private const val MAX_COMMENT = 65535

    /**
     * شهادة الموقّع الأولى (DER)، أو `null` إن لم تكن الحزمة موقّعة بمخطّط v2/v3 أو كان
     * العثور عليها متعذّرًا. **ولا يعني `null` أن الحزمة معدَّلة.**
     */
    fun signerCertificate(file: File): ByteArray? = runCatching {
        if (!file.isFile) return null
        RandomAccessFile(file, "r").use { raf ->
            val centralDirectory = centralDirectoryOffset(raf) ?: return null
            val block = signingBlock(raf, centralDirectory) ?: return null
            for (id in SIGNING_BLOCK_IDS) {
                val value = block[id] ?: continue
                firstCertificate(value)?.let { return it }
            }
            null
        }
    }.getOrNull()

    /** بصمة SHA-256 لشهادة الموقّع — هي القيمة التي تُقارَن بـ[SecurityPin]. */
    fun signerCertificateDigest(file: File): ByteArray? =
        signerCertificate(file)?.let(IntegrityDigest::sha256Of)

    /**
     * إزاحة فهرس ZIP المركزي من سجلّ نهاية الفهرس (EOCD)، بالقراءة العكسيّة من آخر الملفّ
     * لأن التعليق (حتى ٦٥٥٣٥ بايت) يزيح السجلّ عن نهايته تمامًا.
     */
    private fun centralDirectoryOffset(raf: RandomAccessFile): Long? {
        val length = raf.length()
        if (length < EOCD_MIN_SIZE) return null
        val window = minOf(length, (EOCD_MIN_SIZE + MAX_COMMENT).toLong()).toInt()
        val tail = ByteArray(window)
        raf.seek(length - window)
        raf.readFully(tail)
        for (i in window - EOCD_MIN_SIZE downTo 0) {
            if (readU32(tail, i) != EOCD_SIGNATURE) continue
            val offset = readU32(tail, i + 16)
            return if (offset in 0 until length) offset else null
        }
        return null
    }

    /**
     * أزواج `(المعرّف ← القيمة)` في كتلة التوقيع، أو `null` إن لم توجد كتلة أصلًا
     * (حزمة موقّعة بـv1 وحدها، أو غير موقّعة).
     */
    private fun signingBlock(raf: RandomAccessFile, centralDirectory: Long): Map<Long, ByteArray>? {
        if (centralDirectory < 32) return null
        val footer = ByteArray(24)
        raf.seek(centralDirectory - footer.size)
        raf.readFully(footer)
        if (String(footer, 8, 16, Charsets.US_ASCII) != MAGIC) return null
        val declared = readU64(footer, 0)
        val blockStart = centralDirectory - (declared + 8)
        if (blockStart < 0) return null
        // الحجم مكتوب مرّتين (في الرأس وفي الذيل) وتطابقهما شرط سلامة البنية.
        val header = ByteArray(8)
        raf.seek(blockStart)
        raf.readFully(header)
        if (readU64(header, 0) != declared) return null

        // حصة الأزواج = الحجم المعلن − (٨ بايت حجمان + ١٦ بايت السحر).
        val pairsLength = (declared - 24).toInt().coerceAtLeast(0)
        val pairs = ByteArray(pairsLength)
        raf.seek(blockStart + 8)
        raf.readFully(pairs)

        val out = LinkedHashMap<Long, ByteArray>()
        var cursor = 0
        while (cursor + 12 <= pairs.size) {
            val pairLength = readU64(pairs, cursor)
            if (pairLength < 4 || pairLength > (pairs.size - cursor - 8).toLong()) break
            val id = readU32(pairs, cursor + 8)
            val valueStart = cursor + 12
            val valueLength = (pairLength - 4).toInt()
            out[id] = pairs.copyOfRange(valueStart, valueStart + valueLength)
            cursor = valueStart + valueLength
        }
        return out.ifEmpty { null }
    }

    /**
     * أوّل شهادة في قيمة كتلة توقيع، بتتبّع الأطوال المتداخلة:
     * `تسلسل الموقّعين ← موقّع ← البيانات الموقّعة ← تسلسل الشهادات ← شهادة`.
     */
    private fun firstCertificate(value: ByteArray): ByteArray? {
        val signers = lengthPrefixed(value, 0) ?: return null
        val signer = lengthPrefixed(value, signers.first) ?: return null
        val signedData = lengthPrefixed(value, signer.first) ?: return null
        // البيانات الموقّعة: تسلسل البصمات ← تسلسل الشهادات ← السمات الإضافية.
        val digests = lengthPrefixed(value, signedData.first) ?: return null
        val certificates = lengthPrefixed(value, digests.first + digests.second) ?: return null
        val certificate = lengthPrefixed(value, certificates.first) ?: return null
        return value.copyOfRange(certificate.first, certificate.first + certificate.second)
    }

    /**
     * يقرأ `uint32 طول` في الموضع ثم يعيد `(بداية المحتوى, الطول)` — والتحقّق من الحدود
     * جزء من القراءة لا خطوة لاحقة: قارئ يفترض أن الطول صالح يقرأ ذاكرة الملفّ كقيم.
     */
    private fun lengthPrefixed(bytes: ByteArray, offset: Int): Pair<Int, Int>? {
        if (offset < 0 || offset + 4 > bytes.size) return null
        val length = readU32(bytes, offset).toInt()
        val start = offset + 4
        if (length < 0 || start + length > bytes.size) return null
        return start to length
    }

    private fun readU32(bytes: ByteArray, offset: Int): Long {
        if (offset + 4 > bytes.size) return -1
        return (bytes[offset].toLong() and 0xFF) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFF) shl 24)
    }

    private fun readU64(bytes: ByteArray, offset: Int): Long {
        if (offset + 8 > bytes.size) return -1
        var value = 0L
        for (i in 7 downTo 0) value = (value shl 8) or (bytes[offset + i].toLong() and 0xFF)
        return value
    }
}
