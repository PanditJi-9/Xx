/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.license

import java.security.MessageDigest

/**
 * صيغة مفتاح الترخيص — **تُتحقّق على الجهاز قبل أي اتصال**، فلا يُزعج الخادم بمفتاح مبتور.
 *
 * ```
 * MAXM-XXXXX-XXXXX-XXXXX-XXXXX-XX
 *      └──────── 100 بت عشوائية ──────┘└ 10 بت تحقّق
 * ```
 *
 * ## أبجدية Crockford أساس 32 لا hex ولا base64
 *
 * ستّة عشر رمزًا (hex) تعطي ٤ بت للمحرف فتحتاج ٢٥ محرفًا لـ١٠٠ بت؛ وbase64 يستعمل حرفين
 * كبيرين وصغيرين فيصير المفتاح هشًّا في القراءة والنسخ اليدوي (هاتفٌ يقلّب I وl، وزبونٌ ينسخ
 * من محادثة). وCrockford أساس 32 يحلّ الثلاثة: ٣٢ رمزًا = ٥ بت للمحرف (٢٠ محرفًا = ١٠٠ بت)،
 * ولا حروف صغيرة أصلًا، ويحذف `I` و`L` (تُقرآن `1`) و`O` (تُقرأ `0`) و`U`.
 *
 * ## والتجزيء **يُوقف الأخطاء لا يُصادق**
 *
 * محرفا التحقّق يكشفان ~٩٩.٩٪ من أخطاء النسخ قبل الاتصال (وهو ما يُقاس في
 * `tools/test_license_jvm.py`). و**الخادم لا يعتمد عليهما إطلاقًا**: يبحث بالبصمة وحدها —
 * فمن زوّر محرفَي تجزئة صحيحين لا يكسب شيئًا، ومن أخطأ نسخًا لا يستهلك محاولة.
 *
 * ## والبصمة لا المفتاح
 *
 * ما يُرسل للخادم وما يُخزَّن في قاعدة البيانات هو **SHA-256** للصيغة القانونية — والمفتاح
 * الخامّ لا يخرج من الجهاز إلّا كنص يعرضه المستخدم لنفسه. فحتى تسريب قاعدة البيانات كاملة
 * لا يعطي مفتاحًا واحدًا قابلًا للاستعمال.
 */
object LicenseKey {

    /** أبجدية Crockford: بلا I · L · O · U. */
    const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    const val PREFIX = "MAXM"
    const val BODY_LEN = 20
    const val CHECK_LEN = 2
    private const val CANONICAL_LEN = PREFIX.length + BODY_LEN + CHECK_LEN

    /**
     * تطبيع ما لصقه المستخدم إلى الصيغة القانونية `MAXM` + الحمولة + التجزيء، أو `null`.
     *
     * ويقبل: الفواصل والمسافات والأسطر حيثما وقعت · الحروف الصغيرة · `O`↔`0` و`I`/`L`↔`1`
     * و`U`↔`V` (خلطُ القراءة الأشهر) · وجود `MAXM` أو غيابه.
     */
    fun normalize(raw: String): String? {
        val cleaned = StringBuilder(raw.length)
        for (ch in raw.uppercase()) {
            when (ch) {
                'O' -> cleaned.append('0')
                'I', 'L' -> cleaned.append('1')
                'U' -> cleaned.append('V')
                else -> if (ch in ALPHABET) cleaned.append(ch)
            }
        }
        var body = cleaned.toString()
        // الفصل بالطول لا بمجرد الرؤية: حمولةٌ تبدأ هي نفسها بـ`MAXM` (٣١⁻⁴ احتمالًا) يجب ألّا
        // يُقتطع منها شيء — والاقتطاع يُقبل فقط إذا بقي بعده طولٌ قانونيّ بالضبط.
        if (body.length == CANONICAL_LEN && body.startsWith(PREFIX)) body = body.substring(PREFIX.length)
        if (body.length != BODY_LEN + CHECK_LEN) return null
        if (body.any { it !in ALPHABET }) return null
        val candidate = PREFIX + body
        if (!checksumMatches(candidate)) return null
        return candidate
    }

    /** الصيغة المعروضة: `MAXM-XXXXX-XXXXX-XXXXX-XXXXX-XX`. */
    fun format(canonical: String): String {
        val body = canonical.removePrefix(PREFIX)
        if (body.length != BODY_LEN + CHECK_LEN) return canonical
        return buildString(canonical.length + 5) {
            append(PREFIX)
            for (i in 0 until BODY_LEN) {
                if (i % 5 == 0) append('-')
                append(body[i])
            }
            append('-')
            append(body.substring(BODY_LEN))
        }
    }

    /** ما يُرسل للخادم ويُخزَّن هناك: SHA-256 للصيغة القانونية، بأحرف صغيرة hex. */
    fun sha256Hex(canonical: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.US_ASCII))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    /** محرفا التحقّق كما يولّدهما الخادم بالحرف (نفس الدالة في `maxctl/index.ts`). */
    fun checksum(canonical: String): String {
        val body = canonical.removePrefix(PREFIX).take(BODY_LEN)
        val digest = MessageDigest.getInstance("SHA-256").digest((PREFIX + body).toByteArray(Charsets.US_ASCII))
        val first = (digest[0].toInt() and 0xFF) % 32
        val second = (digest[1].toInt() and 0xFF) % 32
        return "" + ALPHABET[first] + ALPHABET[second]
    }

    private fun checksumMatches(canonical: String): Boolean {
        val body = canonical.removePrefix(PREFIX)
        return checksum(canonical) == body.substring(BODY_LEN)
    }
}
