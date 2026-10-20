/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import android.content.Context

/**
 * ما كتبه المستخدم على الذاكرة/UFS بنجاح — **يُحفظ ليُعاد، لا ليُدَّعى**.
 *
 * ### الفجوة التي يغلقها
 *
 * شاشة الذاكرة وUFS كانت تكتب المدى وتقرأه مرتجعةً في الجلسة نفسها، ثم يُصفَّر عند الإقلاع:
 * لا خاصية `persist.*` تحمل الطلب، ولا المحرّك يعرف به. فالنتيجة أن «حفظتُ إعدادي» كان يخصّ
 * الجلسة وحدها — وهو ما لا يجوز أن يبقى بعد أن صار القسم يَعِد بكل شيء.
 *
 * ### وما يُحفظ: الطلب لا القيمة الحيّة
 *
 * يُخزَّن `min:max|governor` بصيغة [MemoryUfsHardwareBackend.encodeRequest] نفسها التي يمرّرها
 * المُحكِّم. وعند الإقلاع **يُعاد التحقّق** من سلّم الجهاز الحالي ([MemoryUfsHardwareBackend.validate])
 * قبل أي كتابة: جهاز تغيّر سائقه أو نواته لا يُكتب عليه رقم لم يعد من سلّمه. والحفظ يقع **بعد**
 * قراءة تحقّق ناجحة فقط — فلا يُحفظ وعدٌ لم يُقَس.
 *
 * ### والحدّ مُعلَن
 *
 * مفتاح واحد لكل مسار عقدة. ومسار اختفى بعد تحديث نواة يبقى محفوظًا بلا أثر: لا يُكتب إلى مسار
 * لم يُقرأ، ويُسجَّل ذلك رمزًا (`device-gone`) بدل حذفه صامتًا — فقد يعود المسار بعد إقلاع تالٍ.
 */
object MemoryUfsTuningStore {

    private const val PREFS = "maxmanager_memory_ufs_tuning"
    private const val PREFIX = "range:"

    /** مدى محفوظ لمسار عقدة واحد. */
    data class Saved(val devicePath: String, val request: MemoryUfsHardwareBackend.Request)

    fun save(context: Context, devicePath: String, request: MemoryUfsHardwareBackend.Request) {
        if (!devicePath.startsWith("/")) return
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(PREFIX + devicePath, MemoryUfsHardwareBackend.encodeRequest(request))
            .apply()
    }

    /** يُنسى المدى بعد تراجع المستخدم عنه — فلا يُعاد ما تركه. */
    fun forget(context: Context, devicePath: String) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(PREFIX + devicePath)
            .apply()
    }

    /**
     * كل المدى المحفوظ، مرتّبًا بمسار العقدة.
     *
     * والسطر التالف **يسقط وحده** ولا يُسقط البقية: بيانات قديمة من إصدار سابق لا تُبطل الحفظ.
     */
    fun load(context: Context): List<Saved> =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all
            .mapNotNull { (key, value) ->
                if (!key.startsWith(PREFIX)) return@mapNotNull null
                val path = key.removePrefix(PREFIX)
                val encoded = value as? String ?: return@mapNotNull null
                val request = DevfreqEncoding.parseRequest(encoded) ?: return@mapNotNull null
                Saved(path, request)
            }
            .sortedBy(Saved::devicePath)

    /** هل هناك حفظ أصلًا؟ — يُسأل قبل إعلان «لا شيء يُعاد» في السجلّ. */
    fun isEmpty(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all.keys
            .none { it.startsWith(PREFIX) }
}
