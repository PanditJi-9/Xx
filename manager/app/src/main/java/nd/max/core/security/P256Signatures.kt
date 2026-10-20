/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.security

import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * التحقّق بتوقيع **ECDSA P-256/SHA-256** — مشتركٌ بين قناتين:
 * الإعداد البعيد (`core/remote/ConfigSignature`) وشهادات الترخيص (`core/license`).
 *
 * ## لماذا ECDSA P-256 لا Ed25519 — قياسُ توفّرٍ لا تفضيل
 *
 * `minSdk 29` في MaxManager و`26` في Max Control، و`Ed25519` لم يكن جزءًا من واجهات أندرويد
 * المستقرّة عند هذه المستويات، بينما `SHA256withECDSA` موجود في كل أصل ثقة منذ الأزل: تشفيره
 * يُطلب بالاسم من `Signature` نفسه بلا `Provider` مخصّص ولا مكتبة ثالثة تُشحن مع التطبيق.
 * والخادم يوقّع بـWebCrypto (يُعيد التوقيع خامًّا r‖s) ويُحوّله إلى DER قبل الإرسال — فالطرفان
 * يتّفقان على **DER** لأن جافا لا تقبل غيره، والتحويل مكتوب ومُقاس في `supabase/functions/maxctl/`.
 *
 * ## ولماذا `java.util` للأساس 64 لا `android.util.Base64`
 *
 * `java.util.Base64` متاح منذ API 26، واستعماله يُخرج هذا الملفّ **خارج أندرويد**: يُصرَّف ويُقاس
 * على JVM عادية في `tools/test_remote_jvm.py` و`tools/test_license_jvm.py` بمفاتيح حقيقية
 * (صالح ومزوّر). ولو دخل `android.util.Base64` لصار قياس المفتاح المزوّر **يحتاج جهازًا**.
 *
 * ## يفصل دائمًا بين «لا مفتاح» و«توقيع باطل»
 *
 * الأولى إعداد بناء ناقص، والثانية محاولة تلاعب. ولا يجوز أن تُقرأ الثانية كأنها الأولى —
 * وهذا ما يُسمّى بالاسم في `RemoteRejection` وفي `LicenseRejection`.
 */
object P256Signatures {

    /** الخوارزمية بالاسم كما تُطلب من مزوّد الأمان: P-256 مع SHA-256. */
    const val ALGORITHM = "SHA256withECDSA"

    /** المنحنى المقبول — التوقيع على منحنى آخر لا يُقبل ولو صحّ رياضيًّا. */
    const val CURVE = "secp256r1"

    /**
     * فكّ المفتاح العامّ من X.509 SubjectPublicKeyInfo مُرمَّزًا base64 — ما يُخرجه
     * `openssl ec -pubout` بالحرف. و`null` تعني: لا مفتاح، أو مفتاح لا يُقرأ، أو منحنى آخر.
     */
    fun decodePublicKey(base64: String): PublicKey? {
        val cleaned = base64.trim()
        if (cleaned.isEmpty()) return null
        val der = runCatching { Base64.getDecoder().decode(cleaned) }.getOrNull() ?: return null
        val key: PublicKey = runCatching {
            KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(der))
        }.getOrNull() ?: return null
        // حرس المنحنى: مفتاح EC من منحنى آخر يُرفض هنا لا عند أول توقيع.
        val ecKey = key as? java.security.interfaces.ECPublicKey ?: return null
        val params = ecKey.params
        val fieldSize = params.curve.field.fieldSize
        val order = params.order.bitLength()
        return key.takeIf { fieldSize == 256 && order == 256 }
    }

    /** فكّ base64 قياسيّ (مع الحشو)؛ وأي نصّ آخر يعود `null` بدل استثناء. */
    fun decodeBase64(text: String): ByteArray? =
        runCatching { Base64.getDecoder().decode(text.trim()) }.getOrNull()

    /** ترميز base64 قياسيّ — يُستعمل في الأدوات والاختبارات، لا في مسار التشغيل. */
    fun encodeBase64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    /**
     * التحقّق: `true` فقط إذا كان التوقيع صالحًا على **هذه البايتات بالضبط** بهذا المفتاح.
     * وكل استثناء يُبتلع إلى `false`؛ لأن سؤال هذه الدالّة «هل أُثبت؟» لا «ما نوع العطب؟».
     */
    fun verify(payloadBytes: ByteArray, signatureBase64: String, publicKey: PublicKey): Boolean {
        val signatureBytes = decodeBase64(signatureBase64) ?: return false
        if (signatureBytes.isEmpty()) return false
        return runCatching {
            Signature.getInstance(ALGORITHM).apply {
                initVerify(publicKey)
                update(payloadBytes)
            }.verify(signatureBytes)
        }.getOrDefault(false)
    }

    /**
     * هل هذا البناء يملك مفتاحًا صالحًا أصلًا؟ — تُسأل مرّة عند الحكم، فلا يُخلط «لا مفتاح»
     * بـ«مفتاح رفض التوقيع»، وهذا تمييزٌ يُسجَّل باسمه.
     */
    fun configured(publicKeyBase64: String): Boolean = decodePublicKey(publicKeyBase64) != null
}
