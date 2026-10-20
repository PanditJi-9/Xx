/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.license

import kotlinx.serialization.json.Json
import java.io.File

/**
 * واجهة تشفير بايتات الترخيص عند السكون — مفصولة عن [LicenseStore] لسببين مقيسين:
 *
 * ① **لتُقاس الدورة كاملة على JVM** بلا أندرويد: `LicenseStoreTest` يختبر الكتابة الذرّية والتلف
 * والقطع وسجلًّا مُعدَّلًا بمُشفِّر بسيط، فلا يبقى منطق الملفّ بلا قياس.
 * ② **ولتُختار الطبقة في الأندرويد صراحةً** ([LicenseCiphers]): مخزن مفاتيح النظام أولًا، ثم
 * مفتاح مشتقّ من بصمة الموقّع، وأخيرًا لا تشفير — وكلٌّ منها **يُعلن اسمه** في السجلّ.
 *
 * وماذا يقاوم التشفير فعلًا؟ النسخَ الحرفيّ لبيانات التطبيق: نقل `files/license.bin` إلى جهاز
 * آخر بحساب جذر لا يكفي — بايتاته مشفّرة بمفتاح لا يخرج من الجهاز الأول، والشهادة داخله مربوطة
 * بهويّة الجهاز الثاني أصلًا. فالنسخ لا يُلغي المنع، بل يتبعه منعٌ ثانٍ.
 */
interface LicenseCipher {

    /** اسم الطبقة كما يُكتب في السجلّ (تشخيص المالك: هل الملفّ مشفّر أصلًا وبماذا). */
    val id: String

    /** يُعيد `null` بدل استثناء — فشل التشفير ليس سببًا لإسقاط العملية كلها. */
    fun seal(plain: ByteArray): ByteArray?

    fun open(sealed: ByteArray): ByteArray?
}

/**
 * ذاكرة الشهادة على ملفّ واحد — بنفس قواعد [nd.max.core.remote.ConfigStore] حرفيًّا:
 * **التلف = الغياب** · **الكتابة ذرّية** · **الفشل لا يُسقط شيئًا** · **لا حذف إلا بطلب**.
 *
 * ولا يُستعمل `SharedPreferences` لأن الشهادة تحتاج كتابة ذرّية موثوقة عند سقوط الجهد في
 * منتصفها، ولأن الملفّ يُقاس كاملًا على JVM بعقدة [`java.io.File`] وحدها.
 */
class LicenseStore(private val file: File, private val cipher: LicenseCipher) {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    /** السجلّ المخزَّن (والغلاف داخله يُعاد التحقّق منه بمفتاح التوقيع في كل إقلاع). */
    fun read(): LicenseRecord? {
        if (!file.isFile) return null
        val sealed = runCatching { file.readBytes() }.getOrNull() ?: return null
        val plain = cipher.open(sealed) ?: return null
        val record = runCatching {
            json.decodeFromString<LicenseRecord>(plain.toString(Charsets.UTF_8))
        }.getOrNull() ?: return null
        if (record.format != LicenseRecord.FORMAT) return null
        if (record.licenseId.isBlank()) return null
        if (record.certificate.payload.isBlank() || record.certificate.signature.isBlank()) return null
        return record
    }

    /** كتابة ذرّية. `false` = لم تُكتب، وللملفّ السابق أن يبقى. */
    fun write(record: LicenseRecord): Boolean {
        val plain = json.encodeToString(record).toByteArray(Charsets.UTF_8)
        val sealed = cipher.seal(plain) ?: return false
        val temporary = File(file.parentFile, "${file.name}.tmp")
        return runCatching {
            file.parentFile?.mkdirs()
            temporary.writeBytes(sealed)
            if (temporary.renameTo(file)) return@runCatching true
            if (!file.delete()) return@runCatching false
            temporary.renameTo(file)
        }.getOrDefault(false)
    }

    /**
     * يسجّل محاولةً لم تُقبل (شبكة · رفض · توقيع) **دون أن يمسح الشهادة القديمة**: الشهادة الأخيرة
     * الصالحة تبقى هي الحكم حتى ينقضي وقتها، ولذلك تُنفصل المحاولة عن الحفظ. وغياب ملفّ سابق ⇒
     * لا كتابة.
     *
     * و`serverMillis` اختياريّ: حين يحمل ردُّ الرفض زمنًا من الخادم تُثبَّت المرساة عليه (إلى
     * الأمام فقط — [LicenseStates.anchored])، فيصير رجوع الساعة لاحقًا بلا أثر.
     */
    fun noteAttempt(nowMillis: Long, code: String, serverMillis: Long = 0L): Boolean {
        val current = read() ?: return false
        return write(
            current.copy(
                lastAttemptMillis = nowMillis,
                lastOutcome = code,
                serverMillis = maxOf(current.serverMillis, serverMillis),
            ),
        )
    }

    /** حذف الذاكرة — عند تشخيص المالك أو تصفير يدويّ. وغياب الملفّ نجاحٌ لا عطب. */
    fun clear(): Boolean = !file.exists() || file.delete()

    companion object {
        /** اسم الملفّ داخل مجلّد الترخيص — وهو **مشفَّر دائمًا** فلا امتداد JSON. */
        const val FILE_NAME = "license.bin"
    }
}
