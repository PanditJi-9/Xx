/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.license

import java.io.File
import java.security.SecureRandom

/**
 * هويّة الجهاز للترخيص — **معرّفٌ عشوائيّ يُنشئه التطبيق**، لا معرّف عتاد.
 *
 * ## لماذا لا `ANDROID_ID` ولا رقم تسلسليّ — وهذا قرار سياسة لا صدفة
 *
 * المستودع يمنع جمع معرّفات الجهاز (`DEVICE-INFO` §"لا معرّفات": لا Android ID ولا أرقام تسلسلية
 * ولا IMEI)، وحدّ الأجهزة **لا يحتاجها**: يكفي رقمٌ عشوائي فريد لهذا التثبيت يُربط به المفتاح.
 * ونتيجتان مرغوبتان معًا: ① الخصوصية كما هي معلنة؛ ② ومقعد الجهاز يقاس على **تثبيتٍ** — فمسح
 * بيانات التطبيق يستهلك مقعدًا جديدًا، ولو كان المعرّف عتاديًّا لصار المسح المجّانيّ ثغرةَ مشاركة.
 *
 * ## ومعنى «الجهاز» بالتحديد (يُقال للمالك والمستخدم صراحةً)
 *
 * الجهاز هنا = **هذا التثبيت على هذه الحزمة**. إعادة تثبيت نظيفة = جهاز جديد يحتاج مقعدًا (ويُحقّ
 * للمالك تحرير المقاعد من اللوحة). وهذا سلوك مقصود: أي سياسة أسهل تُفتح للمشاركة بين الأصدقاء —
 * وهي بعينها ما طلب المالك منعه.
 */
object DeviceIdentities {

    /** اسم ملفّ الهويّة داخل مجلّد الترخيص. */
    const val FILE_NAME = "device.id"

    private val HEX32 = Regex("^[0-9a-f]{32}$")

    /** هل النصّ معرّف جهازٍ صالح؟ (النموذج يُشترط في الشهادة أيضًا على الخادم). */
    fun isValid(id: String): Boolean = HEX32.matches(id)

    /** معرّف عشوائيّ جديد: ١٦ بايتًا من `SecureRandom` — ١٢٨ بت، لا يُخمَّن ولا يُستنتج. */
    fun newId(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * يقرأ المعرّف من الملفّ أو يُنشئه ويكتبه ذرّيًّا.
     *
     * @return المعرّف و**هل استقرّ على قرص** — ففشل الكتابة لا يمنع الجلسة الحالية (المعرّف في
     *   الذاكرة) لكنه يُعلن نفسه ليُسجَّل: جلسةٌ تُفعّل بمفتاحٍ ثم تُعيد التفعيل في الجلسة القادمة
     *   استهلاكًا لمقعدٍ جديد تبعاتٌ يعرفها المالك قبل أن يفتح تذكرة.
     */
    fun loadOrCreate(file: File): DeviceIdentity {
        val existing = if (file.isFile) {
            runCatching { file.readText(Charsets.UTF_8) }.getOrNull()?.trim()?.lowercase()
        } else {
            null
        }
        if (existing != null && isValid(existing)) return DeviceIdentity(existing, persisted = true)

        val fresh = newId()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        val persisted = runCatching {
            file.parentFile?.mkdirs()
            temporary.writeText(fresh, Charsets.UTF_8)
            if (temporary.renameTo(file)) return@runCatching true
            if (!file.delete()) return@runCatching false
            temporary.renameTo(file)
        }.getOrDefault(false)
        return DeviceIdentity(fresh, persisted)
    }

    /** عرضٌ مختصر للمستخدم (للدعم: «جهازك …1234») — لا يكشف المعرّف كاملًا في لقطة شاشة. */
    fun preview(id: String): String =
        if (id.length >= 10) id.substring(0, 6) + "…" + id.substring(id.length - 4) else id
}

/** المعرّف ونتيجة استقراره على القرص — مُدخلٌ للجلسة وللسجلّ. */
data class DeviceIdentity(val id: String, val persisted: Boolean)
