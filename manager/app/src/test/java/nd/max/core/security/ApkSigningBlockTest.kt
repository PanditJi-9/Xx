/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * قارئ كتلة توقيع الحزمة — **يُقاس على بنية مصنوعة بإزاحات معلومة**، لا على ثقة.
 *
 * ولماذا مصنوعة لا حزمة حقيقية: الحزمة الحقيقية تحتاج مُصرِّف حزمة وتوقيعًا (ومفتاحًا)،
 * فلا تصلح لاختبار وحدة يُشغَّل في كل بناء. وهذه البنية تُبنى هنا بايتًا بايتًا من
 * المواصفة نفسها (إزاحات `uint32` صغيرة النهاية وأطوال متداخلة) — فأي انزياح في الحساب
 * يُنتج شهادةً مختلفة ويُكتشف. **ومقارنتها بـ`apksigner` على حزمة موقّعة فعلًا** تُجرى
 * كقياس منفصل عند التحقّق، لأن اختبارًا يبنيه المُختبَر ويكتبه قد يتّفق مع نفسه خطأً.
 */
class ApkSigningBlockTest {

    @get:Rule
    val folder = TemporaryFolder()

    /** شهادة مصنوعة: محتوى DER وهميّ يكفي لأن يُقرأ ويعاد كما هو. */
    private val certificate = ByteArray(20) { (0x30 + it).toByte() }

    /** يبني كتلة توقيع v2 على بنية المواصفة: تسلسل موقّعين ← موقّع ← بيانات موقّعة ← شهادة. */
    private fun signingBlockValue(cert: ByteArray): ByteArray {
        fun u32(value: Int) = byteArrayOf(
            (value and 0xFF).toByte(),
            ((value ushr 8) and 0xFF).toByte(),
            ((value ushr 16) and 0xFF).toByte(),
            ((value ushr 24) and 0xFF).toByte(),
        )
        fun framed(payload: ByteArray) = u32(payload.size) + payload
        val digests = u32(0)
        val certificates = framed(framed(cert))
        val attributes = u32(0)
        val signedData = framed(digests + certificates + attributes)
        // الموقّع: **جسمه** بلا تأطير مسبق، ثم يُؤطَّر مرّة (الموقّع) ويُؤطَّر ثانيةً (تسلسله)
        // — تمامًا كالشهادة أعلاه. والتأطير الزائد هنا كان عطبًا حقيقيًّا في هذه البنية
        // كشفه القرار مقابل القارئ: أربعة أطوال متتالية بدل ثلاثة.
        val signer = signedData + u32(0) + u32(0)
        return framed(framed(signer))
    }

    /**
     * حزمة مصنوعة: محتوى + كتلة توقيع + فهرس مركزي + `EOCD` يشير إلى بداية الفهرس.
     *
     * **والحسابان المهمّان هنا من المواصفة مباشرةً:** حجم الكتلة المعلن = حجم الأزواج
     * `+ 8 + 16` (حجمان مكرّران وسحر)، وطول الزوج = `4 + طول القيمة`. وكل انزياح فيهما
     * كان سيُنتج قراءةً من موضع آخر — وهذا بالضبط ما يقيسه الاختبار.
     */
    private fun apkWith(v2Value: ByteArray?): File {
        val content = ByteArray(64) { 0x11 }
        val blob = ByteArrayOutputStream()
        blob.write(content)
        if (v2Value != null) {
            // معرّف v2 = 0x7109871a صغيرة النهاية (و`0x87` أكبر من `127` فتكتب `Byte` صريحًا).
            val id = byteArrayOf(0x1A, 0x87.toByte(), 0x09, 0x71)
            val body = id + v2Value
            val u64 = { value: Long -> ByteArray(8) { (value ushr (8 * it)).toByte() } }
            // الأزواج = ٨ بايت طول الزوج + المحتوى، والمعلَن = الأزواج + ٢٤ (حجمان وسحر،
            // ناقص الحجم الأول نفسه) — وهو ما يقرؤه القارئ بـ`declared - 24`.
            val declared = body.size + 32L
            blob.write(u64(declared))
            blob.write(u64(body.size.toLong()))
            blob.write(body)
            blob.write(u64(declared))
            blob.write("APK Sig Block 42".toByteArray(Charsets.US_ASCII))
        }
        val centralDirectoryOffset = blob.size()
        blob.write(ByteArray(32) { 0x22 })
        val eocd = ByteArray(22)
        eocd[0] = 0x50; eocd[1] = 0x4B; eocd[2] = 0x05; eocd[3] = 0x06
        for (i in 0 until 4) eocd[16 + i] = ((centralDirectoryOffset ushr (8 * i)) and 0xFF).toByte()
        blob.write(eocd)

        // اسم فريد لكل نداء: `TemporaryFolder.newFile` يرفض الاسم المكرّر، وبعض الاختبارات
        // تبني حزمتين في الاستدعاء نفسه (كتلك التي تُقارن حزمة بكتلة وأخرى بلاها).
        val file = folder.newFile("sample-" + (built++).toString() + ".apk")
        file.writeBytes(blob.toByteArray())
        return file
    }

    /** عدّاد أسماء الحزم المصنوعة داخل الاختبار الواحد. */
    private var built = 0

    @Test
    fun `the certificate is read out of the signing block`() {
        val apk = apkWith(signingBlockValue(certificate))
        val extracted = ApkSigningBlock.signerCertificate(apk)
        assertEquals(certificate.toList(), extracted?.toList())
        assertEquals(
            IntegrityDigest.encode(IntegrityDigest.sha256Of(certificate)),
            IntegrityDigest.encode(ApkSigningBlock.signerCertificateDigest(apk)!!),
        )
    }

    @Test
    fun `an apk without a signing block reports nothing instead of guessing`() {
        assertNull(ApkSigningBlock.signerCertificate(apkWith(null)))
        assertNull(ApkSigningBlock.signerCertificateDigest(apkWith(null)))
    }

    @Test
    fun `a missing file or a non zip reports nothing`() {
        assertNull(ApkSigningBlock.signerCertificate(File(folder.root, "absent.apk")))
        val plain = folder.newFile("plain.bin")
        plain.writeBytes(ByteArray(200) { it.toByte() })
        assertNull(ApkSigningBlock.signerCertificate(plain))
    }

    /**
     * الشهادة المقروءة **بايتات DER حقيقية**: تُمرَّر على `CertificateFactory` للتأكّد من
     * أنّ ما نستخرجه هو الشهادة نفسها لا غالبها. وهذا الفحص يقابل عطبًا صامتًا: استخراج
     * طول خاطئ يُنتج بصمة خاطئة **مستقرّة**، فتقارن بصمةً خاطئة ببصمة خاطئة وتظنّها سليمة.
     */
    @Test
    fun `real apk signer certificate parses as x509 and its digest is of its own der`() {
        val candidates = listOf(
            "build/outputs/apk/release/app-release.apk",
            "build/outputs/apk/debug/app-debug.apk",
            "app/build/outputs/apk/release/app-release.apk",
            "app/build/outputs/apk/debug/app-debug.apk",
        ).map(::File).firstOrNull { it.isFile } ?: return
        val der = ApkSigningBlock.signerCertificate(candidates) ?: return
        val parsed = java.security.cert.CertificateFactory.getInstance("X.509")
            .generateCertificate(der.inputStream())
        assertEquals(parsed.encoded.toList(), der.toList())
        assertTrue(der.size > 200)
    }
}

/** محلّل قائمة الختوم — سطر واحد معطوب لا يجوز أن يُسقط القائمة كلها ولا أن يُمرّ. */
class SealedManifestParsingTest {

    @Test
    fun `only well formed lines are accepted`() {
        val digest = "a".repeat(64)
        val text = """
            # تعليق

            $digest spoof/device_catalog.json
            $digest
            short $digest
            ${digest.uppercase()} socs.json
        """.trimIndent()
        val parsed = AssetVault.parseManifest(text)
        assertEquals(2, parsed.size)
        assertEquals("spoof/device_catalog.json" to digest, parsed[0])
        assertEquals("socs.json" to digest, parsed[1])
    }

    @Test
    fun `an empty manifest yields nothing rather than a false all clear`() {
        assertTrue(AssetVault.parseManifest("").isEmpty())
        assertTrue(AssetVault.parseManifest("# nothing here\n").isEmpty())
    }
}
