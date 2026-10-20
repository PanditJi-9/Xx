/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.remote

import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

/**
 * أدوات اختبار الإعداد البعيد — **بمفاتيح حقيقية لا بدائل**.
 *
 * ولماذا لا يُستعمل «توقيع وهميّ»: الدعوى الوحيدة التي تستحقّ القياس هنا هي أن يمرّ توقيعٌ حقيقيّ
 * **ويرسب** توقيعٌ مزوَّر. فتُولَّد أزواج P-256 حقيقية في اللحظة (مفتاحٌ للحقّ ومفتاحٌ للتزوير)،
 * ويُوقَّع بـ`SHA256withECDSA` نفسها التي تُشحن في التطبيق — فلا يوجد فرق بين ما يُقاس هنا وما
 * يعمل على الجهاز.
 */
object TestKeys {

    data class Key(val publicBase64: String, val private: PrivateKey)

    /** زوجٌ جديد في كل نداء: المفتاحان في اختبار التزوير مختلفان بالضرورة. */
    fun generate(): Key {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec(ConfigSignature.CURVE))
        val pair = generator.generateKeyPair()
        return Key(
            publicBase64 = Base64.getEncoder().encodeToString(pair.public.encoded),
            private = pair.private,
        )
    }

    fun sign(bytes: ByteArray, key: PrivateKey): String {
        val signature = Signature.getInstance(ConfigSignature.ALGORITHM)
        signature.initSign(key)
        signature.update(bytes)
        return Base64.getEncoder().encodeToString(signature.sign())
    }
}

/** بناء المخطّطات كما تُبنى على الخادم بالحرف — نصًّا واحدًا هو ما يُوقَّع. */
object TestConfigs {

    /** لحظةٌ ثابتة في كل الاختبارات: الزمن مُدخل لا يُقرأ من الساعة (`nowMillis`). */
    const val NOW = 1_769_000_000_000L

    /**
     * نصّ ISO-8601 بإزاحةٍ عن [NOW] — **ولا زمن مكتوب بيد في أي اختبار**.
     *
     * وهذا ليس ترفًا: جُرّب المكتوب بيد أوّلًا، فوقع اختباران في خطأ حسابيّ عندي (تاريخٌ ظننته
     * في المستقبل وكان الماضي، وآخر انتهى قبل لحظته) — **والعطب كان في الاختبار لا في الكود**.
     * فصار الزمن يُشتقّ من مرجعٍ واحد، والمقصود يُكتب بالإزاحة («ساعة إلى الأمام») لا بلوحة تقويم.
     */
    fun iso(offsetMillis: Long): String =
        java.time.Instant.ofEpochMilli(NOW + offsetMillis).toString()

    /** لحظةٌ معقولة في الماضي: ساعتان قبل الحكم. */
    val ISSUED_AT: String get() = iso(-2 * 3_600_000L)

    /** دقيقة وأخرى وساعة — تُستعمل في مواقيت الإصدار والإعلان والانتهاء. */
    const val MINUTE = 60_000L
    const val HOUR = 3_600_000L
    const val DAY = 24 * HOUR

    /** الحمولة بأبسط شكل صالح، وتُبنى أجزاؤها من الوسائط لا بقالبٍ واحد للجميع. */
    fun payloadJson(
        revision: Long,
        issuedAt: String = iso(-2 * HOUR),
        schema: Int = 1,
        expiresAt: String? = null,
        features: String = """{"gamespace":{"enabled":false,"min_version_code":0}}""",
        announcement: String? = null,
        stats: String? = null,
        subscription: String? = null,
        promos: String? = null,
    ): String = buildString {
        append("""{"schema":$schema,"revision":$revision,"issued_at":"$issuedAt"""")
        if (expiresAt != null) append(""","expires_at":"$expiresAt"""")
        append(""","features":$features""")
        if (announcement != null) append(""","announcement":$announcement""")
        if (stats != null) append(""","stats":$stats""")
        if (subscription != null) append(""","subscription":$subscription""")
        if (promos != null) append(""","promos":$promos""")
        append("}")
    }

    /** غلافٌ موقّع **بالفعل** — وما يُوقَّع هو بايتات الحمولة بالضبط (لا إعادة تسلسل). */
    fun envelope(
        payload: String,
        key: TestKeys.Key,
        revision: Long? = null,
        schema: Int = 1,
        signature: String? = null,
        payloadOverride: ByteArray? = null,
    ): SignedEnvelope {
        val bytes = payloadOverride ?: payload.toByteArray(Charsets.UTF_8)
        return SignedEnvelope(
            schema = schema,
            revision = revision ?: runCatching {
                Regex(""""revision":(\d+)""").find(payload)!!.groupValues[1].toLong()
            }.getOrElse { 0L },
            payload = Base64.getEncoder().encodeToString(bytes),
            signature = signature ?: TestKeys.sign(bytes, key.private),
        )
    }

    /** الحمولة المفكوكة للغلاف — تُغذّي `admit` كما يفعل العميل. */
    fun bytes(envelope: SignedEnvelope): ByteArray = Base64.getDecoder().decode(envelope.payload)
}
