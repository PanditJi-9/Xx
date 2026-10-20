/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.security

/**
 * تشفير/فكّ سداسي عشر ومقارنة ثابتة الزمن — **منطق خالص** يُقاس في JVM.
 *
 * ولماذا ملفٌّ لعشرين سطرًا: كل عطب في هذه الدوال لا يُرى عند التشغيل السليم، بل يُرى
 * في اللحظة الوحيدة التي يجب أن تعمل فيها — لحظة التلاعب. تشفيرٌ سداسي يقبل طولًا فرديًّا
 * يعود بنصف بصمة، ومقارنةٌ `==` على `ByteArray` تقارن **الهويّة** لا المحتوى فتُعلن
 * «مطابق» دائمًا أو «مختلف» دائمًا. كلاهما صامت تمامًا في التشغيل العادي.
 */
object IntegrityDigest {

    private const val HEX = "0123456789abcdef"

    /** SHA-256 لبايتات. موجودة هنا لأن ثلاث طبقات تحسبها، ولكل واحدة مُحرّكها المستقلّ. */
    fun sha256Of(bytes: ByteArray): ByteArray =
        java.security.MessageDigest.getInstance("SHA-256").digest(bytes)

    /** يرمّز بايتات إلى سداسي عشري صغير الأحرف. المصفوفة الفارغة ⇒ نصّ فارغ (لا `null`). */
    fun encode(bytes: ByteArray): String {
        val out = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            out.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
        }
        return out.toString()
    }

    /**
     * يفكّ سداسيًّا عشريًّا. و`null` عند أي عيب: طول فردي، أو محرف خارج `[0-9a-fA-F]`، أو فراغ.
     *
     * **والفراغ ليس صفرًا:** `null` تعني «لا بصمة مثبَّتة» فيسقط المتحقّق إلى وضع الرصد،
     * بينما مصفوفة أصفار تعني «بصمة موجودة ولا يمكن أن تطابق» فيرفض — والفرق بينهما هو
     * الفرق بين «لم نضبط الحماية» و«أعلنّا التلاعب». ولا يجوز أن يختلطا.
     */
    fun decode(hex: String?): ByteArray? {
        if (hex.isNullOrEmpty() || hex.length % 2 != 0) return null
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val hi = nibble(hex[i * 2])
            val lo = nibble(hex[i * 2 + 1])
            if (hi < 0 || lo < 0) return null
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    /** يفكّ سداسيًّا **مقنَّعًا**: كل بايت مُزاح بـ`mask[b % mask.size]`. */
    fun decodeMasked(hex: String?, mask: ByteArray): ByteArray? {
        if (mask.isEmpty()) return null
        val raw = decode(hex) ?: return null
        for (i in raw.indices) raw[i] = (raw[i].toInt() xor mask[i % mask.size].toInt()).toByte()
        return raw
    }

    /**
     * مقارنة ثابتة الزمن: لا تتوقّف عند أول اختلاف، ولا تكشف الطول بمجرّد الخروج المبكر.
     *
     * والعائد `false` إذا اختلف الطولان — وهو **إفشاء للطول وحده** لا للمحتوى، والطول هنا
     * ثابت (٣٢ بايت لـSHA-256) فلا يُفشي شيئًا.
     */
    fun constantTimeEquals(a: ByteArray?, b: ByteArray?): Boolean {
        if (a == null || b == null || a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    private fun nibble(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> -1
    }
}
