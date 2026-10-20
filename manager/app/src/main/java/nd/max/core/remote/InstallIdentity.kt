/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.remote

import java.io.File
import java.util.UUID

/**
 * **معرّف التثبيت** — الرقم الوحيد الذي يعبر من الجهاز في قناة الإحصاءات (`MAX-CONTROL-01` §١٠).
 *
 * ## ما هو، وما ليس
 *
 * عشوائيّ من [`UUID.randomUUID`] يُنشأ على الجهاز ولا يُستنتج من أيّ معرّف نظام فيه: لا المعرّف
 * الإعلانيّ ولا الرقم التسلسليّ ولا معرّف أندرويد — كلّها ممنوعة بنصّ §١٠.٢، ويحرس المنعَ
 * اختبارُ العقد نصيًّا (فلا رمز منها يُكتب هنا أصلًا). ومعرّف الترخيص
 * ([DeviceIdentities]) **ملفٌّ آخر بقصد**: لو كان واحدًا لربطت الإحصاءاتُ الترخيصَ في صفٍّ
 * واحد، والمطلوب فصلُ القناتين.
 *
 * ## والنسْح يعيد الدورة
 *
 * حين يسحب المستخدم موافقته تُحذف هذه الوثيقة — فإعادةُ التشغيل تبدأ بهويّةٍ جديدة لا تربط
 * ما قبل السحب بما بعده (`CommunityStats.withdraw`).
 */
data class InstallIdentity(val id: String, val persisted: Boolean)

object InstallIdentities {

    /** اسم ملفّ المعرّف داخل `filesDir`. */
    const val FILE_NAME = "install.id"

    private val UUID4 =
        Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

    /** هل النصّ معرّف تثبيتٍ صالح؟ (النموذج يُشترط في `p_install uuid` على الخادم أيضًا). */
    fun isValid(id: String): Boolean = UUID4.matches(id)

    /** معرّف جديد — ١٢٢ بتًا عشوائيَّة من `SecureRandom` داخل `java.util.UUID`. */
    fun newId(): String = UUID.randomUUID().toString()

    /**
     * يقرأ المعرّف من الملفّ أو يُنشئه ويكتبه **ذرّيًّا** (نمط [`nd.max.core.license.DeviceIdentities`]
     * حرفيًّا: كتابة مؤقّتة ثم إعادة تسمية).
     *
     * @return المعرّف و**هل استقرّ على قرص** — ففشل الكتابة لا يمنع الجلسة الحالية، لكنه يُعلن
     *   نفسه ليُسجَّل: معرّفٌ لا يستقرّ يعني صفًّا جديدًا في `installs` كلّ تشغيل.
     */
    fun loadOrCreate(file: File): InstallIdentity {
        val existing = if (file.isFile) {
            runCatching { file.readText(Charsets.UTF_8) }.getOrNull()?.trim()?.lowercase()
        } else {
            null
        }
        if (existing != null && isValid(existing)) return InstallIdentity(existing, persisted = true)

        val fresh = newId()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        val persisted = runCatching {
            file.parentFile?.mkdirs()
            temporary.writeText(fresh, Charsets.UTF_8)
            if (temporary.renameTo(file)) return@runCatching true
            if (!file.delete()) return@runCatching false
            temporary.renameTo(file)
        }.getOrDefault(false)
        return InstallIdentity(fresh, persisted)
    }

    /**
     * يحذف المعرّف المحلّيّ (سحب الموافقة). يُنادى **بعد** نجاح إرسال طلب الحذف — فلا يبقى
     * لدينا مفتاحُ صفٍّ لا نستطيع حذفه بعد اليوم.
     */
    fun remove(file: File): Boolean = runCatching { !file.exists() || file.delete() }.getOrDefault(false)
}
