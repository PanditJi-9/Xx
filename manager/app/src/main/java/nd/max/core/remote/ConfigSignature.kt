/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.remote

import nd.max.core.security.P256Signatures
import java.security.PublicKey

/**
 * التحقّق من توقيع الإعداد البعيد — **والمفتاح العامّ وحده هنا**، والمفتاح الخاصّ لا يخرج من سرّ
 * دالّة التوقيع في Supabase (لا في المستودع ولا في الحزمة).
 *
 * ## القاعدة صارت مشتركة (جولة المفاتيح)
 *
 * الرياضيات نفسها تُصدّق على قناتين: الإعداد البعيد **وشهادات الترخيص** — فانتقلت إلى
 * [P256Signatures] في `core/security` مصدرًا واحدًا، وبقيت هنا الواجهة التي تعرفها بقية ملفّات
 * هذه الحزمة واختباراتها (`tools/test_remote_jvm.py`). فلا نسختان تتباعدان يومًا.
 *
 * ## لماذا ECDSA **P-256/SHA-256** لا Ed25519 — وهذا قياسُ توفّرٍ لا تفضيل
 *
 * `minSdk 29` في MaxManager و`26` في Max Control، و`Ed25519` لم يعد جزءًا من واجهات أندرويد
 * المستقرّة عند هذه المستويات، بينما `SHA256withECDSA` موجود في كل أصل ثقة منذ الأزل: تشفيره
 * يُطلب بالاسم من `Signature` نفسه، ولا يُحتاج إلى `Provider` مخصّص ولا إلى مكتبة ثالثة تُشحن
 * مع التطبيق. (وهذا مُوثَّق في `MAX-CONTROL-01` §٥.٢، ويُنقل التعليل إلى جانب الثابت كما هو هنا.)
 *
 * ## ولماذا `java.util` للأساس 64 لا `android.util.Base64`
 *
 * `java.util.Base64` متاح في أندرويد منذ API 26، واستعماله يُخرج كل هذا الملفّ **خارج أندرويد**:
 * يُصرَّف ويُقاس على JVM عادية في `tools/test_remote_jvm.py` بمفتاحين حقيقيّين (صالح ومزوّر)،
 * وذلك هو ما يُثبت §٥.٤ حالة ٣. ولو دخل `android.util.Base64` لصار قياس المفتاح المزوّر **يحتاج
 * جهازًا** — أي أن قرار استيرادٍ واحد هو الفرق بين دعوى مُقاسة ودعوى مُدّعاة.
 */
object ConfigSignature {

    /** الخوارزمية بالاسم كما تُطلب من مزوّد الأمان: P-256 مع SHA-256. */
    const val ALGORITHM = P256Signatures.ALGORITHM

    /** المنحنى المقبول — التوقيع على منحنى آخر لا يُقبل ولو صحّ رياضيًّا. */
    const val CURVE = P256Signatures.CURVE

    /**
     * فكّ المفتاح العامّ من X.509 SubjectPublicKeyInfo مُرمَّزًا base64 — ما يُخرجه
     * `openssl ec -pubout` بالحرف. و`null` تعني: لا مفتاح، أو مفتاح لا يُقرأ، أو منحنى آخر.
     *
     * وفصلُ «لا مفتاح مضبوط» عن «التوقيع باطل» مقصود: الأولى إعداد بناء ناقص، والثانية محاولة
     * تلاعب. ولا يجوز أن تُقرأ الثانية كأنها الأولى — وهذا ما يقرؤه `RemoteRejection` بالاسم.
     */
    fun decodePublicKey(base64: String): PublicKey? = P256Signatures.decodePublicKey(base64)

    /** فكّ base64 قياسيّ (مع الحشو)؛ وأي نصّ آخر يعود `null` بدل استثناء. */
    fun decodeBase64(text: String): ByteArray? = P256Signatures.decodeBase64(text)

    /** ترميز base64 قياسيّ — يُستعمل في الأدوات والاختبارات، لا في مسار التشغيل. */
    fun encodeBase64(bytes: ByteArray): String = P256Signatures.encodeBase64(bytes)

    /**
     * التحقّق: `true` فقط إذا كان التوقيع صالحًا على **هذه البايتات بالضبط** بهذا المفتاح.
     *
     * وكل استثناء يُبتلع إلى `false`؛ لأن سؤال هذه الدالّة «هل أُثبت؟» لا «ما نوع العطب؟»، وسؤال
     * العطب يُجاب في السجلّ من `RemoteRejection.SIGNATURE` الذي يسمّي المحاولة.
     */
    fun verify(payloadBytes: ByteArray, signatureBase64: String, publicKey: PublicKey): Boolean =
        P256Signatures.verify(payloadBytes, signatureBase64, publicKey)

    /**
     * هل هذا البناء يملك مفتاحًا صالحًا أصلًا؟ — تُسأل مرّة عند الحكم، فلا يُخلط «لا مفتاح»
     * بـ«مفتاح رفض التوقيع»، وهذا تمييزٌ يُسجَّل باسمه.
     */
    fun configured(publicKeyBase64: String): Boolean = P256Signatures.configured(publicKeyBase64)
}
